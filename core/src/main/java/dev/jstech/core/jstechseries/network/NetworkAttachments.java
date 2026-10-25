/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.network;

import dev.jstech.core.JsCore;
import dev.jstech.core.jstechseries.peripheral.ControlLinks;
import java.util.function.Supplier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.ApiStatus;

/**
 * What the data network keeps with each dimension: its {@link NetworkSystem}, made the first time the dimension is
 * asked for it and never saved, since the cables and the members put the networks back as they load.
 */
public final class NetworkAttachments {

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, JsCore.MODID);
    /** A dimension's data networks. */
    public static final Supplier<AttachmentType<NetworkSystem>> NETWORK_SYSTEM =
            ATTACHMENT_TYPES.register("network_system", () -> AttachmentType.builder(NetworkSystem::new).build());
    /** Which controller each machine of a dimension is linked to on the industrial control cable. */
    @ApiStatus.Internal
    public static final Supplier<AttachmentType<ControlLinks.Claims>> CONTROL_CLAIMS =
            ATTACHMENT_TYPES.register("control_claims", () -> AttachmentType.builder(ControlLinks.Claims::new)
                    .build());

    private NetworkAttachments() {
    }

    /** Hands the attachment to the game, once, from the Core's constructor. */
    @ApiStatus.Internal
    public static void register(final IEventBus modBus) {
        ATTACHMENT_TYPES.register(modBus);
    }
}
