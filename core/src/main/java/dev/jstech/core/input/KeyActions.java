/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.input;

import dev.jstech.core.JsCore;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Every declared {@link KeyAction}, and the way a press reaches the server. Actions are declared while the mods load,
 * before the player's game makes its key bindings from them.
 */
@EventBusSubscriber(modid = JsCore.MODID)
public final class KeyActions {

    private static final String NETWORK_VERSION = "1";
    private static final List<KeyAction> ALL = new CopyOnWriteArrayList<>();
    private static final Map<ResourceLocation, KeyAction> BY_ID = new ConcurrentHashMap<>();

    private KeyActions() {
    }

    /**
     * Declares an action.
     *
     * @throws IllegalStateException when an action of that id was declared already
     */
    public static KeyAction declare(final KeyAction.Builder builder) {
        final KeyAction action = builder.build();
        if (BY_ID.putIfAbsent(action.id(), action) != null) {
            throw new IllegalStateException("the key action " + action.id() + " is declared twice");
        }
        ALL.add(action);
        return action;
    }

    /** Every declared action, in the order they were declared. */
    public static List<KeyAction> all() {
        return List.copyOf(ALL);
    }

    /** The action known by {@code id}. */
    public static Optional<KeyAction> byId(final ResourceLocation id) {
        return Optional.ofNullable(BY_ID.get(id));
    }

    /** What the server does with a press of {@code action} from {@code player}; nothing for an unknown action. */
    public static void press(final ServerPlayer player, final ResourceLocation action, final boolean shift) {
        byId(action).ifPresent(found -> found.pressedBy(player, shift));
    }

    @SubscribeEvent
    public static void onRegisterPayloads(final RegisterPayloadHandlersEvent event) {
        event.registrar(NETWORK_VERSION).playToServer(KeyActionPayload.TYPE, KeyActionPayload.STREAM_CODEC,
                KeyActions::onPress);
    }

    /* Runs on the server: the payload is only ever sent to it. */
    private static void onPress(final KeyActionPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                press(player, payload.action(), payload.shift());
            }
        });
    }
}
