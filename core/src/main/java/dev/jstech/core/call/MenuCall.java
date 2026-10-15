/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.call;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.ApiStatus;

/**
 * A call a screen makes to the server, typed, with no payload written by hand: declared once with
 * {@link MenuCalls#declare}, made from the screen with {@link #send}, and handled on the server only while the player
 * still has a menu of its kind open and in reach. The client is never trusted to say which block it means: the menu
 * the server opened is the proof.
 *
 * @param <M> the kind of menu the call is made from
 * @param <A> what the screen sends with it
 */
@ApiStatus.Experimental
public final class MenuCall<M extends AbstractContainerMenu, A> {

    private final ResourceLocation id;
    private final Class<M> menu;
    private final StreamCodec<? super RegistryFriendlyByteBuf, A> codec;
    private final IMenuCallHandler<M, A> handler;

    MenuCall(final ResourceLocation id, final Class<M> menu,
             final StreamCodec<? super RegistryFriendlyByteBuf, A> codec, final IMenuCallHandler<M, A> handler) {
        this.id = id;
        this.menu = menu;
        this.codec = codec;
        this.handler = handler;
    }

    /** The call's id, the same on both sides. */
    public ResourceLocation id() {
        return id;
    }

    /** The kind of menu the call is made from. */
    public Class<M> menu() {
        return menu;
    }

    /** Makes the call, from the player's own game: the server handles it if the player's menu still allows it. */
    public void send(final A argument) {
        PacketDistributor.sendToServer(new MenuCallPayload(this, argument));
    }

    /**
     * What the server does when {@code player} makes the call: handles it if the player has a menu of its kind open
     * and still in reach, and refuses it otherwise.
     *
     * @return whether it was handled
     */
    public boolean receive(final ServerPlayer player, final A argument) {
        if (!admits(player)) {
            return false;
        }
        handler.handle(menu.cast(player.containerMenu), player, argument);
        return true;
    }

    /** Whether {@code player} may make the call now. */
    public boolean admits(final ServerPlayer player) {
        return menu.isInstance(player.containerMenu) && player.containerMenu.stillValid(player);
    }

    @Override
    public String toString() {
        return id.toString();
    }

    void encode(final RegistryFriendlyByteBuf buffer, final Object argument) {
        codec.encode(buffer, cast(argument));
    }

    A decode(final RegistryFriendlyByteBuf buffer) {
        return codec.decode(buffer);
    }

    /* What travels with the call was made by this call's own codec, on one side or the other. */
    @SuppressWarnings("unchecked")
    A cast(final Object argument) {
        return (A) argument;
    }
}
