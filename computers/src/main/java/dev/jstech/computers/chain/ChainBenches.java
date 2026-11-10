/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.chain;

import static dev.jstech.computers.registry.ComputingContent.CONTENT;
import static dev.jstech.computers.registry.ComputingContent.shelf;

import dev.jstech.computers.ComputingLooks;
import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.WorkbenchBlock;
import dev.jstech.computers.blockentity.WorkbenchBlockEntity;
import dev.jstech.computers.item.CabinetBlockItem;
import dev.jstech.computers.registry.ComputingContent.Shelf;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.content.IBlockModel;
import dev.jstech.core.content.IItemLook;
import dev.jstech.core.jstechseries.energy.JsEnergy;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.machine.ProcessingKind;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * The Electronics Workbench of each era (computers/docs/design/survival.md), the player's own bench, used by hand and
 * never automated, and the recipes each assembles: each era's parts are a kind of recipe of their own, which its
 * bench and every later era's bench work, so a bench assembles the parts of its era and the eras before it, and the
 * bench of the next era among its own.
 */
public final class ChainBenches {

    public static final BlockEntry<WorkbenchBlock> VINTAGE = bench(HardwareEra.VINTAGE, MapColor.WOOD);
    public static final BlockEntry<WorkbenchBlock> LEGACY = bench(HardwareEra.LEGACY, MapColor.COLOR_GRAY);
    public static final BlockEntry<WorkbenchBlock> TRANSITION = bench(HardwareEra.TRANSITION, MapColor.COLOR_GRAY);
    public static final BlockEntry<WorkbenchBlock> STANDARD = bench(HardwareEra.STANDARD, MapColor.COLOR_BLACK);
    public static final BlockEntry<WorkbenchBlock> ADVANCED = bench(HardwareEra.ADVANCED, MapColor.SNOW);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WorkbenchBlockEntity>> BENCH_BE =
            CONTENT.blockEntity("electronics_workbench", WorkbenchBlockEntity::new, VINTAGE, LEGACY, TRANSITION,
                    STANDARD, ADVANCED);

    /** The parts of each era, assembled at its bench and every later one's. */
    public static final ProcessingKind VINTAGE_PARTS = parts(HardwareEra.VINTAGE, VINTAGE);
    public static final ProcessingKind LEGACY_PARTS = parts(HardwareEra.LEGACY, LEGACY);
    public static final ProcessingKind TRANSITION_PARTS = parts(HardwareEra.TRANSITION, TRANSITION);
    public static final ProcessingKind STANDARD_PARTS = parts(HardwareEra.STANDARD, STANDARD);
    public static final ProcessingKind ADVANCED_PARTS = parts(HardwareEra.ADVANCED, ADVANCED);
    /* Each era's parts, from the oldest era's, in the order of the chain's eras. */
    private static final List<ProcessingKind> KINDS = List.of(VINTAGE_PARTS, LEGACY_PARTS, TRANSITION_PARTS,
            STANDARD_PARTS, ADVANCED_PARTS);

    static {
        final List<BlockEntry<WorkbenchBlock>> benches = benches();
        for (int era = 0; era < KINDS.size(); era++) {
            for (int later = era + 1; later < benches.size(); later++) {
                KINDS.get(era).alsoWorkedBy(benches.get(later));
            }
        }
    }

    private ChainBenches() {
    }

    /** Declares the benches, from the mod's constructor. */
    public static void declare() {
        // Loading the class declares them.
    }

    /** The benches, from the oldest era's. */
    public static List<BlockEntry<WorkbenchBlock>> benches() {
        return List.of(VINTAGE, LEGACY, TRANSITION, STANDARD, ADVANCED);
    }

    /** The kinds of recipe a bench of {@code era} assembles: its era's and every earlier one's, the oldest first. */
    public static List<ProcessingKind> assembledAt(final Era era) {
        final List<ProcessingKind> out = new ArrayList<>();
        for (int k = 0; k < KINDS.size(); k++) {
            if (ChainParts.ERAS.get(k).isAtMost(era)) {
                out.add(KINDS.get(k));
            }
        }
        return out;
    }

    /* The bench of {@code era}: drawn by its block entity, its item the bench as it stands, no hookup. */
    private static BlockEntry<WorkbenchBlock> bench(final Era era, final MapColor colour) {
        final String id = era.path() + "_electronics_workbench";
        final String model = ComputingLooks.workbench(era);
        return CONTENT.block(id, properties -> new WorkbenchBlock(properties, era))
                .named(ChainParts.englishName(era) + " Electronics Workbench")
                .properties(properties -> properties.mapColor(colour).strength(1.5F).sound(SoundType.WOOD)
                        .noOcclusion())
                .look(IBlockLook.fixed(new IBlockModel.ParticleOnly(id + "_body",
                        "block/chain/" + model + "_particle")))
                .geo(ComputingLooks.WORKBENCH)
                .item((block, properties) -> new CabinetBlockItem(block, properties, ComputingLooks.WORKBENCH,
                        model, ComputingModule.DEVICE_FIT, ComputingLooks.workbenchAtRest()))
                .itemLook(IItemLook.DRAWN_BY_ENTITY).tab(shelf(Shelf.CHAIN, era)).register();
    }

    /* The kind of recipe of the parts of {@code era}, worked first by its own bench. */
    private static ProcessingKind parts(final Era era, final BlockEntry<WorkbenchBlock> bench) {
        return CONTENT.processing(era.path() + "_electronics", ChainParts.englishName(era) + " Electronics", bench)
                .runsOn(JsEnergy.ENERGY).exactInputs();
    }
}
