/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block.part;

import dev.jstech.computers.JsComputers;
import dev.jstech.core.multipart.CoreParts;
import dev.jstech.core.multipart.PartType;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The parts J's Computers mounts on its cables' faces: the storage buses on data cables and the crafting buses on
 * crafting cables, registered with the Core's parts.
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

    public static final DeferredHolder<PartType<?>, PartType<ImportBusPart>> IMPORT = PARTS.register("import_bus",
            () -> new PartType<>(ImportBusPart::new, IMPORT_NAME, IMPORT_MODEL));
    public static final DeferredHolder<PartType<?>, PartType<ExportBusPart>> EXPORT = PARTS.register("export_bus",
            () -> new PartType<>(ExportBusPart::new, EXPORT_NAME, EXPORT_MODEL));
    /* Input feeds like an Export and Receiving pulls like an Import, so they are drawn with those buses' models. */
    public static final DeferredHolder<PartType<?>, PartType<InputBusPart>> INPUT = PARTS.register("input_bus",
            () -> new PartType<>(InputBusPart::new, INPUT_NAME, EXPORT_MODEL));
    public static final DeferredHolder<PartType<?>, PartType<ReceivingBusPart>> RECEIVING = PARTS.register(
            "receiving_bus", () -> new PartType<>(ReceivingBusPart::new, RECEIVING_NAME, IMPORT_MODEL));

    /** The numbers the buses were saved under before parts were the Core's, by the id each is registered under. */
    public static final Map<Integer, String> FORMER_NUMBERS = Map.of(
            0, JsComputers.MODID + ":import_bus",
            1, JsComputers.MODID + ":export_bus",
            2, JsComputers.MODID + ":input_bus",
            3, JsComputers.MODID + ":receiving_bus");

    private ComputingParts() {
    }

    public static void register(final IEventBus modEventBus) {
        PARTS.register(modEventBus);
    }

    /** Whether {@code type} is one of the crafting buses, which mount on crafting cables. */
    public static boolean isCrafting(final PartType<?> type) {
        return type == INPUT.get() || type == RECEIVING.get();
    }

    private static ResourceLocation model(final String name) {
        return ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "block/" + name);
    }
}
