/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.room;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * How some blocks seal a room in one dimension, over what the data map of seals says they do everywhere, written in
 * the dimension's rules: the game's glass and doors stand the overworld's water well enough, but not a room's air held
 * against the vacuum of another world. Its blocks are named, by id or by tag, and looked up as a room is worked out,
 * since a dimension's rules are read before the game's tags are.
 *
 * <pre>{@code
 * "seals": [{"blocks": ["#c:glass_blocks", "#c:glass_panes"], "leak": 0.02, "rating": 10},
 *           {"blocks": ["#minecraft:doors", "minecraft:iron_trapdoor"], "door": true, "leak": 0.02, "rating": 10}]
 * }</pre>
 *
 * @param tags   the tags of the blocks it is for
 * @param blocks the blocks it is for, by id
 * @param seal   how they seal there
 */
public record DimensionSeal(List<TagKey<Block>> tags, List<ResourceLocation> blocks, Seal seal) {

    public static final Codec<DimensionSeal> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ExtraCodecs.TAG_OR_ELEMENT_ID.listOf().fieldOf("blocks").forGetter(DimensionSeal::named),
            Seal.MAP_CODEC.forGetter(DimensionSeal::seal))
            .apply(instance, DimensionSeal::of));

    public DimensionSeal {
        tags = List.copyOf(tags);
        blocks = List.copyOf(blocks);
    }

    /** The seal for the blocks {@code named}, each by id or, after a {@code #}, by tag. */
    public static DimensionSeal of(final List<ExtraCodecs.TagOrElementLocation> named, final Seal seal) {
        final List<TagKey<Block>> tags = new ArrayList<>();
        final List<ResourceLocation> blocks = new ArrayList<>();
        for (final ExtraCodecs.TagOrElementLocation name : named) {
            if (name.tag()) {
                tags.add(TagKey.create(Registries.BLOCK, name.id()));
            } else {
                blocks.add(name.id());
            }
        }
        return new DimensionSeal(tags, blocks, seal);
    }

    /** Whether it is for {@code state}'s block. */
    public boolean covers(final BlockState state) {
        for (final TagKey<Block> tag : this.tags) {
            if (state.is(tag)) {
                return true;
            }
        }
        for (final ResourceLocation block : this.blocks) {
            if (state.getBlockHolder().is(block)) {
                return true;
            }
        }
        return false;
    }

    /* Its blocks as named in its file: the tags first, then the blocks. */
    private List<ExtraCodecs.TagOrElementLocation> named() {
        final List<ExtraCodecs.TagOrElementLocation> named = new ArrayList<>();
        this.tags.forEach(tag -> named.add(new ExtraCodecs.TagOrElementLocation(tag.location(), true)));
        this.blocks.forEach(block -> named.add(new ExtraCodecs.TagOrElementLocation(block, false)));
        return named;
    }
}
