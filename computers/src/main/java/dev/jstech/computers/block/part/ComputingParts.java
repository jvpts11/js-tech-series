/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block.part;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.JsComputers;
import dev.jstech.core.multipart.CoreParts;
import dev.jstech.core.multipart.PartType;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The parts J's Computers mounts on the faces of the Core's cable blocks: the storage buses where a data cable runs,
 * an Import and an Export Bus for each era, and the crafting buses where a crafting cable runs, one design for every
 * era, registered with the Core's parts. The Standard's buses keep the ids the buses had before they had eras.
 */
@TextHolder
public final class ComputingParts {

    public static final DeferredRegister<PartType<?>> PARTS = DeferredRegister.create(CoreParts.KEY,
            JsComputers.MODID);

    private static final TextKey IMPORT_NAME = TextKey.of("jsc.bus.kind.import", "Import");
    private static final TextKey EXPORT_NAME = TextKey.of("jsc.bus.kind.export", "Export");
    private static final TextKey INPUT_NAME = TextKey.of("jsc.bus.kind.input", "Input");
    private static final TextKey RECEIVING_NAME = TextKey.of("jsc.bus.kind.receiving", "Receiving");
    private static final ResourceLocation IMPORT_MODEL = model("import_bus_part");
    private static final ResourceLocation EXPORT_MODEL = model("export_bus_part");

    public static final DeferredHolder<PartType<?>, PartType<ImportBusPart>> VINTAGE_IMPORT =
            importBus("vintage_import_bus", HardwareEra.VINTAGE);
    public static final DeferredHolder<PartType<?>, PartType<ImportBusPart>> LEGACY_IMPORT =
            importBus("legacy_import_bus", HardwareEra.LEGACY);
    public static final DeferredHolder<PartType<?>, PartType<ImportBusPart>> TRANSITION_IMPORT =
            importBus("transition_import_bus", HardwareEra.TRANSITION);
    public static final DeferredHolder<PartType<?>, PartType<ImportBusPart>> IMPORT =
            importBus("import_bus", HardwareEra.STANDARD);
    public static final DeferredHolder<PartType<?>, PartType<ImportBusPart>> ADVANCED_IMPORT =
            importBus("advanced_import_bus", HardwareEra.ADVANCED);
    public static final DeferredHolder<PartType<?>, PartType<ExportBusPart>> VINTAGE_EXPORT =
            exportBus("vintage_export_bus", HardwareEra.VINTAGE);
    public static final DeferredHolder<PartType<?>, PartType<ExportBusPart>> LEGACY_EXPORT =
            exportBus("legacy_export_bus", HardwareEra.LEGACY);
    public static final DeferredHolder<PartType<?>, PartType<ExportBusPart>> TRANSITION_EXPORT =
            exportBus("transition_export_bus", HardwareEra.TRANSITION);
    public static final DeferredHolder<PartType<?>, PartType<ExportBusPart>> EXPORT =
            exportBus("export_bus", HardwareEra.STANDARD);
    public static final DeferredHolder<PartType<?>, PartType<ExportBusPart>> ADVANCED_EXPORT =
            exportBus("advanced_export_bus", HardwareEra.ADVANCED);
    /* Input feeds like an Export and Receiving pulls like an Import, so they are drawn with those buses' models. */
    public static final DeferredHolder<PartType<?>, PartType<InputBusPart>> INPUT = PARTS.register("input_bus",
            () -> new PartType<>(InputBusPart::new, INPUT_NAME, EXPORT_MODEL));
    public static final DeferredHolder<PartType<?>, PartType<ReceivingBusPart>> RECEIVING = PARTS.register(
            "receiving_bus", () -> new PartType<>(ReceivingBusPart::new, RECEIVING_NAME, IMPORT_MODEL));

    private ComputingParts() {
    }

    public static void register(final IEventBus modEventBus) {
        PARTS.register(modEventBus);
    }

    /** Whether {@code type} is one of the crafting buses, which mount on crafting cables. */
    public static boolean isCrafting(final PartType<?> type) {
        return type == INPUT.get() || type == RECEIVING.get();
    }

    /** Whether {@code type} is an Import Bus of any era. */
    public static boolean isImport(final PartType<?> type) {
        return type == VINTAGE_IMPORT.get() || type == LEGACY_IMPORT.get() || type == TRANSITION_IMPORT.get()
                || type == IMPORT.get() || type == ADVANCED_IMPORT.get();
    }

    /** Whether {@code type} is an Export Bus of any era. */
    public static boolean isExport(final PartType<?> type) {
        return type == VINTAGE_EXPORT.get() || type == LEGACY_EXPORT.get() || type == TRANSITION_EXPORT.get()
                || type == EXPORT.get() || type == ADVANCED_EXPORT.get();
    }

    /** The Import Bus of {@code era}; an era after the Advanced has the Advanced's. */
    public static PartType<ImportBusPart> importBus(final HardwareEra era) {
        return switch (era) {
            case VINTAGE -> VINTAGE_IMPORT.get();
            case LEGACY -> LEGACY_IMPORT.get();
            case TRANSITION -> TRANSITION_IMPORT.get();
            case STANDARD -> IMPORT.get();
            case ADVANCED, EXA, SINGULARITY -> ADVANCED_IMPORT.get();
        };
    }

    /** The Export Bus of {@code era}; an era after the Advanced has the Advanced's. */
    public static PartType<ExportBusPart> exportBus(final HardwareEra era) {
        return switch (era) {
            case VINTAGE -> VINTAGE_EXPORT.get();
            case LEGACY -> LEGACY_EXPORT.get();
            case TRANSITION -> TRANSITION_EXPORT.get();
            case STANDARD -> EXPORT.get();
            case ADVANCED, EXA, SINGULARITY -> ADVANCED_EXPORT.get();
        };
    }

    /** The item of the Import Bus of {@code era}, which a broken bus gives back. */
    public static ItemStack importBusItem(final HardwareEra era) {
        return new ItemStack(switch (era) {
            case VINTAGE -> ComputingModule.VINTAGE_IMPORT_BUS_ITEM.get();
            case LEGACY -> ComputingModule.LEGACY_IMPORT_BUS_ITEM.get();
            case TRANSITION -> ComputingModule.TRANSITION_IMPORT_BUS_ITEM.get();
            case STANDARD -> ComputingModule.IMPORT_BUS_ITEM.get();
            case ADVANCED, EXA, SINGULARITY -> ComputingModule.ADVANCED_IMPORT_BUS_ITEM.get();
        });
    }

    /** The item of the Export Bus of {@code era}, which a broken bus gives back. */
    public static ItemStack exportBusItem(final HardwareEra era) {
        return new ItemStack(switch (era) {
            case VINTAGE -> ComputingModule.VINTAGE_EXPORT_BUS_ITEM.get();
            case LEGACY -> ComputingModule.LEGACY_EXPORT_BUS_ITEM.get();
            case TRANSITION -> ComputingModule.TRANSITION_EXPORT_BUS_ITEM.get();
            case STANDARD -> ComputingModule.EXPORT_BUS_ITEM.get();
            case ADVANCED, EXA, SINGULARITY -> ComputingModule.ADVANCED_EXPORT_BUS_ITEM.get();
        });
    }

    private static DeferredHolder<PartType<?>, PartType<ImportBusPart>> importBus(final String id,
                                                                                    final HardwareEra era) {
        return PARTS.register(id, () -> new PartType<>(() -> new ImportBusPart(era), IMPORT_NAME, IMPORT_MODEL));
    }

    private static DeferredHolder<PartType<?>, PartType<ExportBusPart>> exportBus(final String id,
                                                                                    final HardwareEra era) {
        return PARTS.register(id, () -> new PartType<>(() -> new ExportBusPart(era), EXPORT_NAME, EXPORT_MODEL));
    }

    private static ResourceLocation model(final String name) {
        return ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "block/" + name);
    }
}
