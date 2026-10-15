/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.call;

import dev.jstech.core.network.payload.ClientPayloads;
import dev.jstech.core.util.DeclarationError;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.jetbrains.annotations.ApiStatus;

/**
 * Every call a screen can make to the server, declared once on both sides, as constants:
 *
 * <pre>{@code
 * public static final MenuCall<PressMenu, Integer> SET_SPEED = MenuCalls.declare(rl("set_speed"), PressMenu.class,
 *         ByteBufCodecs.VAR_INT, (menu, player, speed) -> menu.press().setSpeed(speed));
 * ...
 * SET_SPEED.send(3);   // in the screen
 * }</pre>
 *
 * <p>They all travel in one payload the Core registers, so a mod writes none of its own.
 */
@ApiStatus.Experimental
public final class MenuCalls {

    private static final String NETWORK_VERSION = "1";
    private static final Map<ResourceLocation, MenuCall<?, ?>> BY_ID = new ConcurrentHashMap<>();
    private static final List<MenuCall<?, ?>> ALL = new CopyOnWriteArrayList<>();

    private MenuCalls() {
    }

    /**
     * Declares the call {@code id}, made from a menu of the kind {@code menu}, sending what {@code codec} writes, and
     * handled on the server by {@code handler}.
     *
     * @throws DeclarationError when a call of that id was declared already
     */
    public static <M extends AbstractContainerMenu, A> MenuCall<M, A> declare(
            final ResourceLocation id, final Class<M> menu, final StreamCodec<? super RegistryFriendlyByteBuf, A> codec,
            final IMenuCallHandler<M, A> handler) {
        final MenuCall<M, A> call = new MenuCall<>(id, menu, codec, handler);
        if (BY_ID.putIfAbsent(id, call) != null) {
            throw DeclarationError.of(id.getNamespace(), "the screen call " + id, "is declared twice",
                    "declare it once, as a static final, on both sides, and use that field");
        }
        ALL.add(call);
        return call;
    }

    /** The call declared as {@code id}, or none. */
    public static Optional<MenuCall<?, ?>> byId(final ResourceLocation id) {
        return Optional.ofNullable(BY_ID.get(id));
    }

    /** Every call declared, in the order they were declared. */
    public static List<MenuCall<?, ?>> all() {
        return List.copyOf(ALL);
    }

    /** Hands the mod's bus the payload every call travels in, once, from the Core's constructor. */
    @ApiStatus.Internal
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterPayloadHandlersEvent.class, event -> ClientPayloads.accept(
                event.registrar(NETWORK_VERSION), MenuCallPayload.TYPE, MenuCallPayload.STREAM_CODEC,
                (player, payload) -> payload.call().admits(player),
                (payload, player, level) -> payload.receivedFrom(player)));
    }
}
