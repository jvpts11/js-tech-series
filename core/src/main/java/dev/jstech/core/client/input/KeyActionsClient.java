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
    private static final Map<KeyAction, Hold> HOLDS = new ConcurrentHashMap<>();

    private KeyActionsClient() {
    }

    /**
     * What holding {@code action}'s key for {@code ticks} or more does instead of a press: let go sooner, it is a press
     * as any other; held that long, {@code held} runs once and the press never comes.
     */
    public static void onHold(final KeyAction action, final int ticks, final Runnable held) {
        if (ticks < 1) {
            throw new IllegalArgumentException("a key is held one tick or more, not " + ticks);
        }
        HOLDS.put(Objects.requireNonNull(action, "action"), new Hold(ticks, Objects.requireNonNull(held, "held")));
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
            final Hold hold = HOLDS.get(entry.getKey());
            if (hold == null) {
                while (entry.getValue().consumeClick()) {
                    press(entry.getKey());
                }
                continue;
            }
            int clicks = 0;
            while (entry.getValue().consumeClick()) {
                clicks++;
            }
            if (hold.tick(entry.getValue().isDown(), clicks)) {
                press(entry.getKey());
            }
        }
    }

    /* How long a key with something to do when held has been down, and whether that has been done. */
    private static final class Hold {

        private final int ticks;
        private final Runnable held;
        private int down;
        private boolean done;

        private Hold(final int ticks, final Runnable held) {
            this.ticks = ticks;
            this.held = held;
        }

        /*
         * A tick of the key, down or not, pressed {@code clicks} times since the last: runs what holding does once it
         * has been down long enough, and says whether a press came instead (let go sooner, or pressed and let go
         * between two ticks).
         */
        boolean tick(final boolean isDown, final int clicks) {
            if (isDown) {
                this.down++;
                if (!this.done && this.down >= this.ticks) {
                    this.done = true;
                    this.held.run();
                }
                return false;
            }
            final boolean pressed = !this.done && (this.down > 0 || clicks > 0);
            this.down = 0;
            this.done = false;
            return pressed;
        }
    }
}
