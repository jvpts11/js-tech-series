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
import java.util.function.Supplier;
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
    private static final TextKey EXTERNAL_NAME = TextKey.of("jsc.bus.kind.external", "External Storage");
    /** Where the buses' models are, by the bus's id; the item of a bus wears its idle model. */
    public static final String MODELS = "block/bus/";
    private static final String BUSY = "_busy";

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
    public static final DeferredHolder<PartType<?>, PartType<ExternalStorageBusPart>> VINTAGE_EXTERNAL =
            externalBus("vintage_external_storage_bus", HardwareEra.VINTAGE);
    public static final DeferredHolder<PartType<?>, PartType<ExternalStorageBusPart>> LEGACY_EXTERNAL =
            externalBus("legacy_external_storage_bus", HardwareEra.LEGACY);
    public static final DeferredHolder<PartType<?>, PartType<ExternalStorageBusPart>> TRANSITION_EXTERNAL =
            externalBus("transition_external_storage_bus", HardwareEra.TRANSITION);
    public static final DeferredHolder<PartType<?>, PartType<ExternalStorageBusPart>> EXTERNAL =
            externalBus("external_storage_bus", HardwareEra.STANDARD);
    public static final DeferredHolder<PartType<?>, PartType<ExternalStorageBusPart>> ADVANCED_EXTERNAL =
            externalBus("advanced_external_storage_bus", HardwareEra.ADVANCED);
    /* Input feeds like an Export and Receiving pulls like an Import, so they share those shapes, in amber. */
    public static final DeferredHolder<PartType<?>, PartType<InputBusPart>> INPUT = PARTS.register("input_bus",
            () -> type(InputBusPart::new, INPUT_NAME, "input_bus"));
    public static final DeferredHolder<PartType<?>, PartType<ReceivingBusPart>> RECEIVING = PARTS.register(
            "receiving_bus", () -> type(ReceivingBusPart::new, RECEIVING_NAME, "receiving_bus"));

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

    /** Whether {@code type} is an External Storage Bus of any era. */
    public static boolean isExternal(final PartType<?> type) {
        return type == VINTAGE_EXTERNAL.get() || type == LEGACY_EXTERNAL.get() || type == TRANSITION_EXTERNAL.get()
                || type == EXTERNAL.get() || type == ADVANCED_EXTERNAL.get();
    }

    /** The External Storage Bus of {@code era}; an era after the Advanced has the Advanced's. */
    public static PartType<ExternalStorageBusPart> externalBus(final HardwareEra era) {
        return switch (era) {
            case VINTAGE -> VINTAGE_EXTERNAL.get();
            case LEGACY -> LEGACY_EXTERNAL.get();
            case TRANSITION -> TRANSITION_EXTERNAL.get();
            case STANDARD -> EXTERNAL.get();
            case ADVANCED, EXA, SINGULARITY -> ADVANCED_EXTERNAL.get();
        };
    }

    /** The item of the External Storage Bus of {@code era}, which a broken bus gives back. */
    public static ItemStack externalBusItem(final HardwareEra era) {
        return new ItemStack(switch (era) {
            case VINTAGE -> ComputingModule.VINTAGE_EXTERNAL_STORAGE_BUS_ITEM.get();
            case LEGACY -> ComputingModule.LEGACY_EXTERNAL_STORAGE_BUS_ITEM.get();
            case TRANSITION -> ComputingModule.TRANSITION_EXTERNAL_STORAGE_BUS_ITEM.get();
            case STANDARD -> ComputingModule.EXTERNAL_STORAGE_BUS_ITEM.get();
            case ADVANCED, EXA, SINGULARITY -> ComputingModule.ADVANCED_EXTERNAL_STORAGE_BUS_ITEM.get();
        });
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
        return PARTS.register(id, () -> type(() -> new ImportBusPart(era), IMPORT_NAME, id));
    }

    private static DeferredHolder<PartType<?>, PartType<ExportBusPart>> exportBus(final String id,
                                                                                    final HardwareEra era) {
        return PARTS.register(id, () -> type(() -> new ExportBusPart(era), EXPORT_NAME, id));
    }

    private static DeferredHolder<PartType<?>, PartType<ExternalStorageBusPart>> externalBus(final String id,
                                                                                             final HardwareEra era) {
        return PARTS.register(id, () -> type(() -> new ExternalStorageBusPart(era), EXTERNAL_NAME, id));
    }

    /*
     * A bus kind, drawn with the model of its own id: its era's casing, its kind's shape and colour, its lamps dark;
     * while it moves, the same model with the lamps blinking.
     */
    private static <P extends AbstractBusPart> PartType<P> type(final Supplier<P> factory, final TextKey name,
                                                                final String id) {
        return new PartType<>(factory, name, model(id), model(id + BUSY));
    }

    private static ResourceLocation model(final String name) {
        return ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, MODELS + name);
    }
}
