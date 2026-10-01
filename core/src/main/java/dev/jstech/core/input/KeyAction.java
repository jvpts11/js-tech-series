/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.input;

import dev.jstech.core.text.TextKey;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * A key a player presses to do something, declared once where both sides can see it: what the game's controls call
 * it, the key it starts on, and what the server does when it is pressed. The player's game makes the game's key
 * binding from it, and a press of a key that does something on the server reaches the server by itself; what the
 * press does on the player's own game is said by the client code, so this class names nothing of the client.
 *
 * <pre>{@code
 * KeyAction NEXT_MODE = KeyActions.declare(KeyAction.builder(id("next_mode"), NEXT_MODE_NAME)
 *         .onServer((player, shift) -> cycle(player, shift)));
 * }</pre>
 */
public final class KeyAction {

    private final ResourceLocation id;
    private final TextKey name;
    private final int defaultKey;
    private final @Nullable TextKey category;
    private final @Nullable IServerPress server;

    /** No key: the player gives it one in the game's controls. */
    public static final int NO_KEY = -1;

    private KeyAction(final Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.defaultKey = builder.defaultKey;
        this.category = builder.category;
        this.server = builder.server;
    }

    /** Starts declaring the action known by {@code id}, called {@code name} in the game's controls. */
    public static Builder builder(final ResourceLocation id, final TextKey name) {
        return new Builder(id, name);
    }

    public ResourceLocation id() {
        return this.id;
    }

    /** What the game's controls call it; its key is also the binding's name in a player's settings. */
    public TextKey name() {
        return this.name;
    }

    /** The key it starts on, as the game's input numbers keys, or {@link #NO_KEY}. */
    public int defaultKey() {
        return this.defaultKey;
    }

    /** The heading it is listed under in the game's controls: the series' own unless it was given another. */
    public TextKey category() {
        return this.category != null ? this.category : CoreKeys.CATEGORY;
    }

    /** Whether a press reaches the server. */
    public boolean reachesServer() {
        return this.server != null;
    }

    /* A press came from {@code player}; nothing for an action the server has no part in. */
    void pressedBy(final ServerPlayer player, final boolean shift) {
        if (this.server != null) {
            this.server.pressed(player, shift);
        }
    }

    /** What the server does when a player presses the key. */
    @FunctionalInterface
    public interface IServerPress {

        /**
         * @param player who pressed it
         * @param shift  whether shift was held
         */
        void pressed(ServerPlayer player, boolean shift);
    }

    /** Declares a key action one property at a time. */
    public static final class Builder {

        private final ResourceLocation id;
        private final TextKey name;
        private int defaultKey = NO_KEY;
        private @Nullable TextKey category;
        private @Nullable IServerPress server;

        private Builder(final ResourceLocation id, final TextKey name) {
            this.id = Objects.requireNonNull(id, "id");
            this.name = Objects.requireNonNull(name, "name");
        }

        /**
         * The key it starts on, as the game's input numbers keys; none unless said, since the game and the mods beside
         * it already take nearly every key.
         */
        public Builder key(final int key) {
            this.defaultKey = key;
            return this;
        }

        /** The heading it is listed under in the game's controls; the series' own unless said. */
        public Builder category(final TextKey heading) {
            this.category = Objects.requireNonNull(heading, "heading");
            return this;
        }

        /** What the server does when a player presses it. */
        public Builder onServer(final IServerPress press) {
            this.server = Objects.requireNonNull(press, "press");
            return this;
        }

        KeyAction build() {
            return new KeyAction(this);
        }
    }
}
