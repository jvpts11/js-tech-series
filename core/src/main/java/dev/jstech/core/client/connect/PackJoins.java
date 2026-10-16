/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.connect;

import dev.jstech.core.connect.ConnectedQuadrants;
import dev.jstech.core.connect.texture.CtmTexture;
import dev.jstech.core.connect.texture.OptifineRule;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Which neighbours a face joins, by each format's rule: for OptiFine's and Continuity's, the same block, the same state
 * or the same texture on the neighbour's face, or the blocks and textures a rule names; for CTM's, the same state, or
 * every state of the block, or what its conditions name, a face's own conditions over the rest. A neighbour whose face
 * is covered by another that joins is left out where the rule says, so a seam shows on the inside of a corner. The
 * neighbours come as a mask of eight bits clockwise from the texture's up, as {@link ConnectedQuadrants} numbers
 * them.
 */
final class PackJoins {

    /* The textures on each face of each state, read once from its model. */
    private static final Map<BlockState, Map<Direction, Set<String>>> FACES = new ConcurrentHashMap<>();
    /* The tags rules name, made once each. */
    private static final Map<String, TagKey<Block>> TAGS = new ConcurrentHashMap<>();
    private static final long SEED = 42L;

    private PackJoins() {
    }

    /** Forgets the textures it read, as the models are baked again. */
    static void forget() {
        FACES.clear();
    }

    /**
     * The neighbours the face {@code pieces} of the block in the middle of {@code around} joins, by {@code joins};
     * a neighbour covered by another that joins is left out where {@code seams}.
     */
    static int mask(final PackSurroundings around, final QuadPieces pieces, final Direction face,
                    final IJoin joins, final boolean seams) {
        final Direction up = pieces.up();
        final Direction right = pieces.right();
        final Direction down = up.getOpposite();
        final Direction left = right.getOpposite();
        final Direction[][] ways = {{up}, {up, right}, {right}, {down, right}, {down}, {down, left}, {left},
            {up, left}};
        int mask = 0;
        for (int i = 0; i < ways.length; i++) {
            if (joined(around, ways[i], face, joins, seams)) {
                mask |= 1 << i;
            }
        }
        return mask;
    }

    /** Whether the block one step along {@code ways} from the middle of {@code around} joins, as {@link #mask}. */
    static boolean joined(final PackSurroundings around, final Direction[] ways, final Direction face,
                          final IJoin joins, final boolean seams) {
        if (!joins.joins(around.at(ways), around.extraAt(ways), face)) {
            return false;
        }
        if (!seams) {
            return true;
        }
        final Direction[] front = new Direction[ways.length + 1];
        System.arraycopy(ways, 0, front, 0, ways.length);
        front[ways.length] = face;
        return !joins.joins(around.at(front), around.extraAt(front), face);
    }

    /** OptiFine's and Continuity's rule {@code rule} for a face of {@code self} drawn with the texture {@code tile}. */
    static IJoin optifine(final OptifineRule rule, final BlockState self, final String tile) {
        if (!rule.connectTo().isEmpty() || !rule.connectTiles().isEmpty()) {
            // A rule that names what it joins joins that, as an overlay's names what it is laid for.
            return (other, extra, face) -> rule.connectTo().stream().anyMatch(filter -> matches(filter, other))
                    || rule.connectTiles().stream().anyMatch(name -> faceTextures(other, face).contains(name));
        }
        return switch (rule.connect()) {
            case STATE -> (other, extra, face) -> other == self;
            case TILE -> (other, extra, face) -> faceTextures(other, face).contains(tile);
            case BLOCK -> (other, extra, face) -> other.getBlock() == self.getBlock();
        };
    }

    /** The blocks an overlay of {@code rule} is laid over a face of {@code self} for. */
    static IJoin overlaySource(final OptifineRule rule, final BlockState self, final String tile) {
        if (!rule.connectTo().isEmpty() || !rule.connectTiles().isEmpty()) {
            return optifine(rule, self, tile);
        }
        return (other, extra, face) -> !other.isAir() && other.getBlock() != self.getBlock();
    }

    /** CTM's texture {@code texture} for a face of {@code self}. */
    static IJoin ctm(final CtmTexture texture, final BlockState self) {
        final Optional<CtmTexture.ConnectTo> to = texture.connectTo();
        if (to.isEmpty()) {
            return texture.ignoreStates() ? (other, extra, face) -> other.getBlock() == self.getBlock()
                    : (other, extra, face) -> other == self;
        }
        return (other, extra, face) -> meets(to.get(), other, face);
    }

    /** Whether {@code state} is a block {@code filter} names, with the values it asks. */
    static boolean matches(final OptifineRule.BlockFilter filter, final BlockState state) {
        if (filter.tag()) {
            return state.is(tag(filter.id()));
        }
        if (!BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().equals(filter.id())) {
            return false;
        }
        for (final Map.Entry<String, List<String>> wanted : filter.properties().entrySet()) {
            final Property<?> property = state.getBlock().getStateDefinition().getProperty(wanted.getKey());
            if (property == null || !wanted.getValue().contains(valueText(state, property))) {
                return false;
            }
        }
        return true;
    }

    /** Whether {@code state} meets CTM's condition {@code condition}. */
    static boolean meets(final CtmTexture.Condition condition, final BlockState state) {
        if (condition.tag()) {
            if (!state.is(tag(condition.block()))) {
                return false;
            }
        } else if (!BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().equals(condition.block())) {
            return false;
        }
        for (final CtmTexture.Predicate predicate : condition.predicates()) {
            final Property<?> property = state.getBlock().getStateDefinition().getProperty(predicate.property());
            if (property == null || !predicate.meets(valueText(state, property))) {
                return false;
            }
        }
        return true;
    }

    /** The textures on the face {@code face} of {@code state}'s model, by id. */
    static Set<String> faceTextures(final BlockState state, final Direction face) {
        return FACES.computeIfAbsent(state, PackJoins::readFaces).getOrDefault(face, Set.of());
    }

    /* Whether {@code other} meets CTM's conditions for the face {@code face}, its own over the rest as it defers. */
    private static boolean meets(final CtmTexture.ConnectTo to, final BlockState other, final Direction face) {
        final boolean all = to.conditions().stream().anyMatch(condition -> meets(condition, other));
        final CtmTexture.Side side = to.sides().get(face.getSerializedName());
        if (side == null) {
            return all;
        }
        final boolean own = side.conditions().stream().anyMatch(condition -> meets(condition, other));
        return switch (side.defer().orElse("")) {
            case "and" -> own && all;
            case "or" -> own || all;
            default -> own;
        };
    }

    private static Map<Direction, Set<String>> readFaces(final BlockState state) {
        final BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
        final RandomSource random = RandomSource.create(SEED);
        final Map<Direction, Set<String>> out = new EnumMap<>(Direction.class);
        for (final Direction face : Direction.values()) {
            final List<BakedQuad> quads = model.getQuads(state, face, random, ModelData.EMPTY, null);
            out.put(face, quads.stream().map(quad -> quad.getSprite().contents().name().toString())
                    .collect(Collectors.toUnmodifiableSet()));
        }
        return out;
    }

    private static <T extends Comparable<T>> String valueText(final BlockState state, final Property<T> property) {
        return property.getName(state.getValue(property));
    }

    private static TagKey<Block> tag(final String id) {
        return TAGS.computeIfAbsent(id, key -> TagKey.create(Registries.BLOCK, ResourceLocation.parse(key)));
    }

    /** Whether a neighbour joins a face. */
    @FunctionalInterface
    interface IJoin {

        /**
         * Whether the neighbour in {@code other}, with what its model added about it in {@code extra} (none for
         * nothing), joins the face {@code face}.
         */
        boolean joins(BlockState other, Object extra, Direction face);
    }
}
