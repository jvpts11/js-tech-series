/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multipart;

import dev.jstech.core.JsCore;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

/**
 * The registry of part kinds. A mod registers its parts into it with a deferred register of its own:
 *
 * <pre>{@code
 * DeferredRegister<PartType<?>> PARTS = DeferredRegister.create(CoreParts.KEY, MODID);
 * DeferredHolder<PartType<?>, PartType<ImportBusPart>> IMPORT = PARTS.register("import_bus",
 *         () -> new PartType<>(ImportBusPart::new, IMPORT_NAME, IMPORT_MODEL));
 * }</pre>
 *
 * <p>The registry is synced, so a player's game numbers each kind as the server does and a block's parts are sent
 * as small numbers rather than ids.
 */
@EventBusSubscriber(modid = JsCore.MODID)
public final class CoreParts {

    /** The registry's key. */
    public static final ResourceKey<Registry<PartType<?>>> KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "part_type"));
    /** Every kind of part. */
    public static final Registry<PartType<?>> REGISTRY = new RegistryBuilder<>(KEY).sync(true).create();

    private CoreParts() {
    }

    @SubscribeEvent
    public static void onNewRegistry(final NewRegistryEvent event) {
        event.register(REGISTRY);
    }
}
