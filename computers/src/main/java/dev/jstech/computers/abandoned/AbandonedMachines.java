/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.abandoned;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.computers.block.PartEras;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.EraCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.Nullable;

/**
 * What an abandoned building's machines, containers and frames hold as it is placed (computers/docs/design/survival.md,
 * "Abandoned buildings"). Its template marks each thing with what it is: a machine is filled with its era's parts, as
 * {@link AbandonedFills} makes it; a chest draws from its building's table for its era, the main one from the table
 * that also holds a way to a graphical system, a hardware box from its era's and kind's; a frame on a wall shows one of
 * the building's boxed programs.
 */
public final class AbandonedMachines extends StructureProcessor {

    private final AbandonedBuilding building;
    private final Era era;
    private final boolean working;

    /** The key of a thing's mark in its template. */
    public static final String MARK = "jsc_abandoned";
    /** The key that marks a container of the building's loot, and which: main, chest or box. */
    public static final String LOOT = "jsc_loot";
    public static final MapCodec<AbandonedMachines> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StableCodecs.byName(AbandonedBuilding.class).fieldOf("building").forGetter(AbandonedMachines::building),
            EraCodecs.CODEC.fieldOf("era").forGetter(AbandonedMachines::era),
            Codec.BOOL.fieldOf("working").forGetter(AbandonedMachines::working))
            .apply(instance, AbandonedMachines::new));
    private static final String MAIN = "main";
    private static final String BOX = "box";
    private static final String ITEM = "Item";

    public AbandonedMachines(final AbandonedBuilding building, final Era era, final boolean working) {
        this.building = building;
        this.era = era;
        this.working = working;
    }

    public AbandonedBuilding building() {
        return building;
    }

    public Era era() {
        return era;
    }

    public boolean working() {
        return working;
    }

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(final LevelReader level, final BlockPos offset,
                                                             final BlockPos pos,
                                                             final StructureTemplate.StructureBlockInfo original,
                                                             final StructureTemplate.StructureBlockInfo placed,
                                                             final StructurePlaceSettings settings) {
        final CompoundTag data = placed.nbt();
        if (data == null) {
            return placed;
        }
        final RandomSource random = settings.getRandom(placed.pos());
        if (data.contains(LOOT)) {
            return with(placed, loot(placed, data.getString(LOOT), random));
        }
        if (!data.contains(MARK)) {
            return placed;
        }
        final CompoundTag mark = data.getCompound(MARK);
        if (mark.contains("cable")) {
            return with(placed, AbandonedFills.cable(level, placed, mark, era, settings));
        }
        final @Nullable CompoundTag filled = switch (mark.getString("machine")) {
            case "computer" -> AbandonedFills.computer(level, placed, mark, era, working, random);
            case "rack" -> AbandonedFills.rack(level, placed, mark, era, working, random);
            case "mainframe" -> AbandonedFills.mainframe(level, placed, era, working, random);
            case "rig" -> AbandonedFills.rig(level, placed, mark, era, working, random);
            case "battery" -> AbandonedFills.battery(level, placed);
            default -> unmarked(data);
        };
        return with(placed, filled);
    }

    @Override
    public StructureTemplate.StructureEntityInfo processEntity(final LevelReader level, final BlockPos seed,
                                                               final StructureTemplate.StructureEntityInfo raw,
                                                               final StructureTemplate.StructureEntityInfo entity,
                                                               final StructurePlaceSettings settings,
                                                               final StructureTemplate template) {
        final CompoundTag nbt = entity.nbt.copy();
        // A thing hung on a wall (a frame, a painting) knows the block it hangs in: where it is placed, not where it
        // stood in the template.
        nbt.putInt("TileX", entity.blockPos.getX());
        nbt.putInt("TileY", entity.blockPos.getY());
        nbt.putInt("TileZ", entity.blockPos.getZ());
        if (!nbt.contains(MARK)) {
            return new StructureTemplate.StructureEntityInfo(entity.pos, entity.blockPos, nbt);
        }
        nbt.remove(MARK);
        // A frame on a wall: one of the building's boxed programs, the same one wherever the frame is placed from.
        AbandonedSoftware.pick(building, era, settings.getRandom(entity.blockPos).nextInt())
                .map(AbandonedSoftware::boxed).filter(stack -> !stack.isEmpty())
                .ifPresent(stack -> nbt.put(ITEM, stack.save(level.registryAccess())));
        return new StructureTemplate.StructureEntityInfo(entity.pos, entity.blockPos, nbt);
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return AbandonedBuildings.MACHINES.get();
    }

    /* A container's table: the building's main or other chests', or a hardware box's by its era and kind. */
    private CompoundTag loot(final StructureTemplate.StructureBlockInfo placed, final String kind,
                             final RandomSource random) {
        final ResourceLocation table;
        if (BOX.equals(kind) && placed.state().hasProperty(HardwareBoxBlock.KIND)) {
            table = AbandonedLoot.box(PartEras.eraOf(placed.state().getValue(HardwareBoxBlock.ERA)),
                    placed.state().getValue(HardwareBoxBlock.KIND));
        } else {
            table = MAIN.equals(kind) ? AbandonedLoot.main(building, era) : AbandonedLoot.table(building, era);
        }
        final CompoundTag tag = new CompoundTag();
        tag.putString("LootTable", table.toString());
        tag.putLong("LootTableSeed", random.nextLong());
        return tag;
    }

    /* A mark no machine answers to (a monitor's): taken off, the rest kept. */
    private static @Nullable CompoundTag unmarked(final CompoundTag data) {
        final CompoundTag rest = data.copy();
        rest.remove(MARK);
        return rest.isEmpty() ? null : rest;
    }

    private static StructureTemplate.StructureBlockInfo with(final StructureTemplate.StructureBlockInfo placed,
                                                             final @Nullable CompoundTag nbt) {
        return new StructureTemplate.StructureBlockInfo(placed.pos(), placed.state(), nbt);
    }
}
