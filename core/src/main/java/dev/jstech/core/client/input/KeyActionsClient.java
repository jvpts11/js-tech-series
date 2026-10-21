/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.input;

import com.mojang.blaze3d.platform.InputConstants;
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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
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
        if (worth == null || worth.getAsBoolean()) {
            send(action, Screen.hasShiftDown());
        }
    }

    /**
     * Sends a press of {@code action} to the server as though shift were held or not, as {@code shift} says, for
     * input that is not the action's key: the mouse wheel turning an item's mode one way or the other. Nothing for an
     * action the server has no part in.
     */
    public static void send(final KeyAction action, final boolean shift) {
        if (action.reachesServer()) {
            PacketDistributor.sendToServer(new KeyActionPayload(action.id(), shift));
        }
    }

    /** Hands the mod's bus and the game's what this answers, once, from the mod's constructor on a player's game. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterKeyMappingsEvent.class, KeyActionsClient::onRegisterKeys);
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, KeyActionsClient::onClientTick);
    }

    public static void onRegisterKeys(final RegisterKeyMappingsEvent event) {
        KeyActions.freeze();
        for (final KeyAction action : KeyActions.all()) {
            final int key = action.defaultKey() == KeyAction.NO_KEY ? InputConstants.UNKNOWN.getValue()
                    : action.defaultKey();
            final KeyMapping mapping = new KeyMapping(action.name().key(), key, action.category().key());
            MAPPINGS.put(action, mapping);
            event.register(mapping);
        }
    }

    public static void onClientTick(final ClientTickEvent.Post event) {
        for (final Map.Entry<KeyAction, KeyMapping> entry : MAPPINGS.entrySet()) {
            while (entry.getValue().consumeClick()) {
                press(entry.getKey());
            }
        }
    }
}
