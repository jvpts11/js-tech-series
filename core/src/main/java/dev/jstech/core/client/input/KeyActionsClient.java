/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import dev.jstech.core.JsCore;
import dev.jstech.core.input.KeyAction;
import dev.jstech.core.input.KeyActionPayload;
import dev.jstech.core.input.KeyActions;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The declared key actions on a player's game: a key binding for each, what a press does here, and the press sent to
 * the server for an action the server has a part in. The client code of an action says what its press does on the
 * player's game, and when a press is worth sending:
 *
 * <pre>{@code
 * KeyActionsClient.onPress(CoreKeys.TURN_OFF_LAST_SOUND, AudioKeys::turnOffLastSound);
 * KeyActionsClient.sendsWhen(CoreKeys.CHANGE_ITEM_MODE, () -> holdsItemWithModes());
 * }</pre>
 */
@EventBusSubscriber(modid = JsCore.MODID, value = Dist.CLIENT)
public final class KeyActionsClient {

    private static final Map<KeyAction, KeyMapping> MAPPINGS = new LinkedHashMap<>();
    private static final Map<KeyAction, Runnable> PRESSES = new ConcurrentHashMap<>();
    private static final Map<KeyAction, BooleanSupplier> SENDS_WHEN = new ConcurrentHashMap<>();

    private KeyActionsClient() {
    }

    /** What a press of {@code action} does on the player's own game. */
    public static void onPress(final KeyAction action, final Runnable press) {
        PRESSES.put(Objects.requireNonNull(action, "action"), Objects.requireNonNull(press, "press"));
    }

    /** When a press of {@code action} is sent to the server; always, unless said. */
    public static void sendsWhen(final KeyAction action, final BooleanSupplier worthSending) {
        SENDS_WHEN.put(Objects.requireNonNull(action, "action"), Objects.requireNonNull(worthSending, "when"));
    }

    /**
     * The game's key binding of {@code action}, to name its key to the player.
     *
     * @throws IllegalStateException before the game has made its key bindings
     */
    public static KeyMapping mapping(final KeyAction action) {
        final KeyMapping mapping = MAPPINGS.get(action);
        if (mapping == null) {
            throw new IllegalStateException("no key binding was made for " + action.id());
        }
        return mapping;
    }

    /** Does what a press of {@code action} does: on this game, and on the server when it has a part in it. */
    public static void press(final KeyAction action) {
        final Runnable here = PRESSES.get(action);
        if (here != null) {
            here.run();
        }
        final BooleanSupplier worth = SENDS_WHEN.get(action);
        if (action.reachesServer() && (worth == null || worth.getAsBoolean())) {
            PacketDistributor.sendToServer(new KeyActionPayload(action.id(), Screen.hasShiftDown()));
        }
    }

    @SubscribeEvent
    public static void onRegisterKeys(final RegisterKeyMappingsEvent event) {
        for (final KeyAction action : KeyActions.all()) {
            final int key = action.defaultKey() == KeyAction.NO_KEY ? InputConstants.UNKNOWN.getValue()
                    : action.defaultKey();
            final KeyMapping mapping = new KeyMapping(action.name().key(), key, action.category().key());
            MAPPINGS.put(action, mapping);
            event.register(mapping);
        }
    }

    @SubscribeEvent
    public static void onClientTick(final ClientTickEvent.Post event) {
        for (final Map.Entry<KeyAction, KeyMapping> entry : MAPPINGS.entrySet()) {
            while (entry.getValue().consumeClick()) {
                press(entry.getKey());
            }
        }
    }
}
