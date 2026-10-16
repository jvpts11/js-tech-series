/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.connect;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dev.jstech.core.connect.texture.CtmTexture;
import dev.jstech.core.connect.texture.OptifineRule;
import dev.jstech.core.connect.texture.OptifineRules;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/**
 * Which of the packs' connected textures may draw a face, in the order they are tried, their tiles found in the
 * blocks' atlas: CTM's override of the face's tint index in its model, else CTM's own texture of the face's, else the
 * OptiFine and Continuity rules of the face's texture, then those of its block. Worked out once for each face of each
 * model, and once for each block, and forgotten as the models are baked again.
 */
final class PackCandidates {

    /* Each face's plan, kept while the face is; each block's rules; each rule's tiles. */
    private static final Cache<BakedQuad, Plan> PLANS = CacheBuilder.newBuilder().weakKeys().build();
    private static final Map<Block, List<Rule>> BY_BLOCK = new ConcurrentHashMap<>();
    private static final Map<OptifineRule, Rule> RULES = new ConcurrentHashMap<>();
    private static final Map<String, Optional<Ctm>> CTM = new ConcurrentHashMap<>();

    private PackCandidates() {
    }

    /** Forgets every plan, as the models are baked again. */
    static void forget() {
        PLANS.invalidateAll();
        BY_BLOCK.clear();
        RULES.clear();
        CTM.clear();
    }

    /** The plan of the face {@code quad}, or none where its texture does not lie flat on it. */
    static @Nullable Plan of(final BakedQuad quad) {
        try {
            final Plan plan = PLANS.get(quad, () -> plan(quad));
            return plan.pieces() == null ? null : plan;
        } catch (final ExecutionException e) {
            throw new IllegalStateException("a face's connected textures could not be worked out", e.getCause());
        }
    }

    /** The OptiFine and Continuity rules by block for {@code block}, by id and by tag, in order. */
    static List<Rule> ofBlock(final Block block) {
        return BY_BLOCK.computeIfAbsent(block, key -> {
            final PackRules rules = PackRules.current();
            final List<Rule> out = new ArrayList<>();
            final String id = BuiltInRegistries.BLOCK.getKey(key).toString();
            for (final OptifineRule rule : rules.byBlock().getOrDefault(id, List.of())) {
                out.add(rule(rule));
            }
            for (final OptifineRule rule : rules.byTag()) {
                out.add(rule(rule));
            }
            return List.copyOf(out);
        });
    }

    /** CTM's texture {@code texture} as the face's own would be drawn, its proxy followed. */
    static Optional<Ctm> ctm(final String texture, final Optional<CtmTexture> override) {
        if (override.isPresent()) {
            return Optional.of(ctmOf(override.get().texture().map(own -> namespaced(own, texture)).orElse(texture),
                    override.get()));
        }
        return CTM.computeIfAbsent(texture, key -> {
            final CtmTexture found = PackRules.current().ctm().get(key);
            if (found == null) {
                return Optional.empty();
            }
            if (found.proxy().isPresent()) {
                final String proxy = namespaced(found.proxy().get(), key);
                final CtmTexture through = PackRules.current().ctm().get(proxy);
                return Optional.of(ctmOf(proxy, through == null ? found : through));
            }
            return Optional.of(ctmOf(key, found));
        });
    }

    private static Plan plan(final BakedQuad quad) {
        final QuadPieces pieces = QuadPieces.of(quad);
        final String sprite = quad.getSprite().contents().name().toString();
        final List<Rule> byTile = new ArrayList<>();
        for (final OptifineRule rule : PackRules.current().byTile().getOrDefault(sprite, List.of())) {
            byTile.add(rule(rule));
        }
        return new Plan(pieces, sprite, List.copyOf(byTile));
    }

    private static Rule rule(final OptifineRule rule) {
        return RULES.computeIfAbsent(rule, key -> {
            final TextureAtlasSprite[] tiles = new TextureAtlasSprite[key.tiles().size()];
            final boolean[] skips = new boolean[tiles.length];
            for (int i = 0; i < tiles.length; i++) {
                final OptifineRule.Tile tile = key.tiles().get(i);
                switch (tile.kind()) {
                    case SKIP -> skips[i] = true;
                    case DEFAULT -> tiles[i] = null;
                    default -> tiles[i] = sprite(OptifineRules.spriteOf(OptifineRules.tileFile(key.file(), tile)));
                }
            }
            return new Rule(key, tiles, skips);
        });
    }

    private static Ctm ctmOf(final String texture, final CtmTexture found) {
        final String namespace = texture.substring(0, texture.indexOf(':'));
        final TextureAtlasSprite[] companions = new TextureAtlasSprite[found.textures().size()];
        for (int i = 0; i < companions.length; i++) {
            companions[i] = sprite(PackRules.withNamespace(found.textures().get(i), namespace));
        }
        return new Ctm(found, sprite(texture), companions);
    }

    private static String namespaced(final String texture, final String like) {
        return PackRules.withNamespace(texture, like.substring(0, like.indexOf(':')));
    }

    private static TextureAtlasSprite sprite(final String id) {
        final TextureAtlas atlas = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS);
        return atlas.getSprite(ResourceLocation.parse(id));
    }

    /**
     * A face's plan: the face as its texture lies on it, its texture, and the rules of that texture.
     *
     * @param pieces the face, or none where its texture does not lie flat on it
     * @param sprite its texture's id
     * @param byTile the rules of its texture, in order
     */
    record Plan(@Nullable QuadPieces pieces, String sprite, List<Rule> byTile) {

        Plan {
            byTile = List.copyOf(byTile);
        }
    }

    /**
     * An OptiFine or Continuity rule with its tiles found: a tile the face's own texture where the rule says
     * {@code <default>}, none where it says {@code <skip>}.
     *
     * @param rule  the rule
     * @param tiles its tiles, none for the face's own texture
     * @param skips which tiles are nothing
     */
    record Rule(OptifineRule rule, TextureAtlasSprite[] tiles, boolean[] skips) {

        Rule {
            tiles = tiles.clone();
            skips = skips.clone();
        }

        /** Tile {@code index}: its sprite, the face's own for none, or none for a tile that is nothing. */
        @Nullable TextureAtlasSprite tile(final int index, final TextureAtlasSprite own) {
            if (index < 0 || index >= this.tiles.length || this.skips[index]) {
                return null;
            }
            return this.tiles[index] == null ? own : this.tiles[index];
        }
    }

    /**
     * CTM's texture with its sprites found.
     *
     * @param texture    what it says
     * @param own        the texture itself, or its proxy
     * @param companions its companions, in order
     */
    record Ctm(CtmTexture texture, TextureAtlasSprite own, TextureAtlasSprite[] companions) {

        Ctm {
            companions = companions.clone();
        }

        /** Companion {@code index}, or the texture itself where it has too few. */
        TextureAtlasSprite companion(final int index) {
            return index < this.companions.length ? this.companions[index] : this.own;
        }
    }
}
