/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.Nullable;

/*
 * The blocks of a structure born as one, as the template they come from, to be laid into its plot once the plot's
 * chunks have loaded, the template's corner at {@code corner}: they never stand in the world. Nothing reads or writes a
 * chunk that is not loaded, so a structure born where the yard's chunks are not loaded yet waits for them a few ticks;
 * it is saved so, too.
 */
final class StructureBirth {

    private final StructureTemplate template;
    private final BlockPos corner;

    private static final String TEMPLATE = "template";
    private static final String CORNER = "corner";
    private static final String PALETTE = "palette";
    private static final String PALETTES = "palettes";
    private static final String BLOCKS = "blocks";
    private static final String STATE = "state";
    private static final String NAME = "Name";

    StructureBirth(final StructureTemplate template, final BlockPos corner) {
        this.template = template;
        this.corner = corner.immutable();
    }

    /* Lays its blocks into the plot quietly, as a structure's blocks come in; its template's creatures stay out. */
    void lay(final ServerLevel level) {
        final StructurePlaceSettings settings = new StructurePlaceSettings().setIgnoreEntities(true);
        this.template.placeInWorld(level, this.corner, this.corner, settings, level.getRandom(),
                Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    }

    /* It as its structure's save holds it. */
    CompoundTag save() {
        final CompoundTag tag = new CompoundTag();
        tag.put(TEMPLATE, this.template.save(new CompoundTag()));
        tag.putLong(CORNER, this.corner.asLong());
        return tag;
    }

    /* The birth a save holds, its blocks read by {@code blocks}. */
    static StructureBirth read(final CompoundTag tag, final HolderGetter<Block> blocks) {
        final StructureTemplate template = new StructureTemplate();
        template.load(blocks, tag.getCompound(TEMPLATE));
        return new StructureBirth(template, BlockPos.of(tag.getLong(CORNER)));
    }

    /* How many blocks of {@code template} are not air: what the structure born of it is made of. */
    static int blocksOf(final StructureTemplate template) {
        final CompoundTag saved = template.save(new CompoundTag());
        final ListTag palette = palette(saved);
        int count = 0;
        for (final Tag one : saved.getList(BLOCKS, Tag.TAG_COMPOUND)) {
            final BlockState state = stateOf(palette, ((CompoundTag) one).getInt(STATE));
            if (state != null && !state.isAir()) {
                count++;
            }
        }
        return count;
    }

    /* The first block of {@code template} that must stay put, with where it is in the template, if any. */
    static Optional<Placed> staysPut(final StructureTemplate template) {
        final CompoundTag saved = template.save(new CompoundTag());
        final ListTag palette = palette(saved);
        for (final Tag one : saved.getList(BLOCKS, Tag.TAG_COMPOUND)) {
            final CompoundTag block = (CompoundTag) one;
            final BlockState state = stateOf(palette, block.getInt(STATE));
            if (state != null && state.is(StructureAssembly.STAYS_PUT)) {
                final ListTag pos = block.getList("pos", Tag.TAG_INT);
                return Optional.of(new Placed(BuiltInRegistries.BLOCK.getKey(state.getBlock()),
                        new BlockPos(pos.getInt(0), pos.getInt(1), pos.getInt(2))));
            }
        }
        return Optional.empty();
    }

    /* The template's palette, the first when it has several. */
    private static ListTag palette(final CompoundTag saved) {
        if (saved.contains(PALETTES, Tag.TAG_LIST)) {
            final ListTag all = saved.getList(PALETTES, Tag.TAG_LIST);
            return all.isEmpty() ? new ListTag() : all.getList(0);
        }
        return saved.getList(PALETTE, Tag.TAG_COMPOUND);
    }

    /* The block of the palette's entry {@code index}, by its name; its properties do not say whether it is air. */
    private static @Nullable BlockState stateOf(final ListTag palette, final int index) {
        if (index < 0 || index >= palette.size()) {
            return null;
        }
        final ResourceLocation name = ResourceLocation.tryParse(palette.getCompound(index).getString(NAME));
        return name == null ? null : BuiltInRegistries.BLOCK.get(name).defaultBlockState();
    }

    /*
     * A block of a template.
     *
     * @param block what it is
     * @param at    where it is in the template
     */
    record Placed(ResourceLocation block, BlockPos at) {
    }
}
