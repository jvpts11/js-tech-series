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
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.EraTable;
import dev.jstech.core.multipart.CoreParts;
import dev.jstech.core.multipart.IFacePart;
import dev.jstech.core.multipart.PartType;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The parts J's Computers mounts on the faces of the Core's cable blocks: the storage buses where a data cable runs,
 * an Import, an Export and an External Storage Bus for each era, and the crafting parts where a crafting cable runs:
 * a Crafting Interface for each era, and the Crafting Input Router and the Crafting Receiving Bus, one design for
 * every era, registered with the Core's parts. The Standard's buses keep the ids the buses had before they had eras.
 */
@TextHolder
public final class ComputingParts {

    public static final DeferredRegister<PartType<?>> PARTS = DeferredRegister.create(CoreParts.KEY,
            JsComputers.MODID);

    private static final TextKey IMPORT_NAME = TextKey.of("jsc.bus.kind.import", "Import");
    private static final TextKey EXPORT_NAME = TextKey.of("jsc.bus.kind.export", "Export");
    private static final TextKey ROUTER_NAME = TextKey.of("jsc.bus.kind.router", "Input Router");
    private static final TextKey RECEIVING_NAME = TextKey.of("jsc.bus.kind.receiving", "Receiving");
    private static final TextKey INTERFACE_NAME = TextKey.of("jsc.bus.kind.interface", "Crafting Interface");
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
    /*
     * The router feeds like an Export Bus and the Receiving Bus pulls like an Import, so they share those shapes on the
     * crafting amber, with the ring and chevrons in the colour of each: blue for the router, yellow for the bus.
     */
    public static final DeferredHolder<PartType<?>, PartType<CraftingRouterPart>> ROUTER = PARTS.register(
            "crafting_input_router", () -> type(CraftingRouterPart::new, ROUTER_NAME, "crafting_input_router"));
    public static final DeferredHolder<PartType<?>, PartType<ReceivingBusPart>> RECEIVING = PARTS.register(
            "receiving_bus", () -> type(ReceivingBusPart::new, RECEIVING_NAME, "receiving_bus"));
    /* The Crafting Interface of each era, red, its plate showing a slot for each pattern it holds. */
    public static final DeferredHolder<PartType<?>, PartType<CraftingInterfacePart>> VINTAGE_INTERFACE =
            craftingInterface("vintage_crafting_interface", HardwareEra.VINTAGE);
    public static final DeferredHolder<PartType<?>, PartType<CraftingInterfacePart>> LEGACY_INTERFACE =
            craftingInterface("legacy_crafting_interface", HardwareEra.LEGACY);
    public static final DeferredHolder<PartType<?>, PartType<CraftingInterfacePart>> TRANSITION_INTERFACE =
            craftingInterface("transition_crafting_interface", HardwareEra.TRANSITION);
    public static final DeferredHolder<PartType<?>, PartType<CraftingInterfacePart>> INTERFACE =
            craftingInterface("crafting_interface", HardwareEra.STANDARD);
    public static final DeferredHolder<PartType<?>, PartType<CraftingInterfacePart>> ADVANCED_INTERFACE =
            craftingInterface("advanced_crafting_interface", HardwareEra.ADVANCED);

    /*
     * Each era's parts and the items a broken one gives back; an era after the Advanced has the Advanced's. The items
     * are asked for when a part breaks, never while this class loads, so the module's items load in their own time.
     */
    private static final EraTable<Supplier<PartType<CraftingInterfacePart>>> INTERFACES = EraTable.ofFive(
            VINTAGE_INTERFACE, LEGACY_INTERFACE, TRANSITION_INTERFACE, INTERFACE, ADVANCED_INTERFACE);
    private static final EraTable<Supplier<PartType<ExternalStorageBusPart>>> EXTERNALS = EraTable.ofFive(
            VINTAGE_EXTERNAL, LEGACY_EXTERNAL, TRANSITION_EXTERNAL, EXTERNAL, ADVANCED_EXTERNAL);
    private static final EraTable<Supplier<PartType<ImportBusPart>>> IMPORTS = EraTable.ofFive(
            VINTAGE_IMPORT, LEGACY_IMPORT, TRANSITION_IMPORT, IMPORT, ADVANCED_IMPORT);
    private static final EraTable<Supplier<PartType<ExportBusPart>>> EXPORTS = EraTable.ofFive(
            VINTAGE_EXPORT, LEGACY_EXPORT, TRANSITION_EXPORT, EXPORT, ADVANCED_EXPORT);
    private static final EraTable<Supplier<Item>> INTERFACE_ITEMS = EraTable.ofFive(
            () -> ComputingModule.VINTAGE_CRAFTING_INTERFACE_ITEM.get(),
            () -> ComputingModule.LEGACY_CRAFTING_INTERFACE_ITEM.get(),
            () -> ComputingModule.TRANSITION_CRAFTING_INTERFACE_ITEM.get(),
            () -> ComputingModule.CRAFTING_INTERFACE_ITEM.get(),
            () -> ComputingModule.ADVANCED_CRAFTING_INTERFACE_ITEM.get());
    private static final EraTable<Supplier<Item>> EXTERNAL_ITEMS = EraTable.ofFive(
            () -> ComputingModule.VINTAGE_EXTERNAL_STORAGE_BUS_ITEM.get(),
            () -> ComputingModule.LEGACY_EXTERNAL_STORAGE_BUS_ITEM.get(),
            () -> ComputingModule.TRANSITION_EXTERNAL_STORAGE_BUS_ITEM.get(),
            () -> ComputingModule.EXTERNAL_STORAGE_BUS_ITEM.get(),
            () -> ComputingModule.ADVANCED_EXTERNAL_STORAGE_BUS_ITEM.get());
    private static final EraTable<Supplier<Item>> IMPORT_ITEMS = EraTable.ofFive(
            () -> ComputingModule.VINTAGE_IMPORT_BUS_ITEM.get(),
            () -> ComputingModule.LEGACY_IMPORT_BUS_ITEM.get(),
            () -> ComputingModule.TRANSITION_IMPORT_BUS_ITEM.get(),
            () -> ComputingModule.IMPORT_BUS_ITEM.get(),
            () -> ComputingModule.ADVANCED_IMPORT_BUS_ITEM.get());
    private static final EraTable<Supplier<Item>> EXPORT_ITEMS = EraTable.ofFive(
            () -> ComputingModule.VINTAGE_EXPORT_BUS_ITEM.get(),
            () -> ComputingModule.LEGACY_EXPORT_BUS_ITEM.get(),
            () -> ComputingModule.TRANSITION_EXPORT_BUS_ITEM.get(),
            () -> ComputingModule.EXPORT_BUS_ITEM.get(),
            () -> ComputingModule.ADVANCED_EXPORT_BUS_ITEM.get());

    private ComputingParts() {
    }

    public static void register(final IEventBus modEventBus) {
        PARTS.register(modEventBus);
    }

    /** Whether {@code type} is one of the crafting parts, which mount on crafting cables. */
    public static boolean isCrafting(final PartType<?> type) {
        return type == ROUTER.get() || type == RECEIVING.get() || isInterface(type);
    }

    /** Whether {@code type} is a Crafting Interface of any era. */
    public static boolean isInterface(final PartType<?> type) {
        return type == VINTAGE_INTERFACE.get() || type == LEGACY_INTERFACE.get() || type == TRANSITION_INTERFACE.get()
                || type == INTERFACE.get() || type == ADVANCED_INTERFACE.get();
    }

    /** The Crafting Interface of {@code era}; an era after the Advanced has the Advanced's. */
    public static PartType<CraftingInterfacePart> craftingInterface(final Era era) {
        return INTERFACES.get(era).get();
    }

    /** The item of the Crafting Interface of {@code era}, which a broken interface gives back. */
    public static ItemStack craftingInterfaceItem(final Era era) {
        return new ItemStack(INTERFACE_ITEMS.get(era).get());
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
    public static PartType<ExternalStorageBusPart> externalBus(final Era era) {
        return EXTERNALS.get(era).get();
    }

    /** The item of the External Storage Bus of {@code era}, which a broken bus gives back. */
    public static ItemStack externalBusItem(final Era era) {
        return new ItemStack(EXTERNAL_ITEMS.get(era).get());
    }

    /** The Import Bus of {@code era}; an era after the Advanced has the Advanced's. */
    public static PartType<ImportBusPart> importBus(final Era era) {
        return IMPORTS.get(era).get();
    }

    /** The Export Bus of {@code era}; an era after the Advanced has the Advanced's. */
    public static PartType<ExportBusPart> exportBus(final Era era) {
        return EXPORTS.get(era).get();
    }

    /** The item of the Import Bus of {@code era}, which a broken bus gives back. */
    public static ItemStack importBusItem(final Era era) {
        return new ItemStack(IMPORT_ITEMS.get(era).get());
    }

    /** The item of the Export Bus of {@code era}, which a broken bus gives back. */
    public static ItemStack exportBusItem(final Era era) {
        return new ItemStack(EXPORT_ITEMS.get(era).get());
    }

    private static DeferredHolder<PartType<?>, PartType<ImportBusPart>> importBus(final String id,
                                                                                    final Era era) {
        return PARTS.register(id, () -> type(() -> new ImportBusPart(era), IMPORT_NAME, id));
    }

    private static DeferredHolder<PartType<?>, PartType<ExportBusPart>> exportBus(final String id,
                                                                                    final Era era) {
        return PARTS.register(id, () -> type(() -> new ExportBusPart(era), EXPORT_NAME, id));
    }

    private static DeferredHolder<PartType<?>, PartType<ExternalStorageBusPart>> externalBus(final String id,
                                                                                             final Era era) {
        return PARTS.register(id, () -> type(() -> new ExternalStorageBusPart(era), EXTERNAL_NAME, id));
    }

    private static DeferredHolder<PartType<?>, PartType<CraftingInterfacePart>> craftingInterface(
            final String id, final Era era) {
        return PARTS.register(id, () -> type(() -> new CraftingInterfacePart(era), INTERFACE_NAME, id));
    }

    /*
     * A part kind, drawn with the model of its own id: its era's casing, its kind's shape and colour, its lamps dark;
     * while it works, the same model with the lamps blinking.
     */
    private static <P extends IFacePart> PartType<P> type(final Supplier<P> factory, final TextKey name,
                                                          final String id) {
        return new PartType<>(factory, name, model(id), model(id + BUSY));
    }

    private static ResourceLocation model(final String name) {
        return ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, MODELS + name);
    }
}
