/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.computers.advancement.Acting;
import dev.jstech.computers.menu.CommandPromptMenu;
import dev.jstech.computers.menu.ComputerTerminalMenu;
import dev.jstech.computers.menu.DesktopMenu;
import dev.jstech.computers.menu.IMonitorMenu;
import dev.jstech.computers.menu.MonitorSessionMenu;
import dev.jstech.core.network.payload.ClientPayloads;
import dev.jstech.core.network.payload.ClientPayloads.IMenuPayloadHandler;
import dev.jstech.core.network.payload.ClientPayloads.IPayloadRefusal;
import dev.jstech.core.network.payload.ClientPayloads.IServerPayloadHandler;
import dev.jstech.core.network.payload.IPayloadGate;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Function;

/**
 * Who may send a payload that acts on a computer, over J's Core's gated client payloads.
 *
 * <p>A client is not trusted to say which machine it is using: a modified one can put any position in any payload.
 * So every payload a client sends is registered here with a gate, asked on the server thread before the handler runs,
 * and a payload its gate refuses is dropped. The gates follow the screens: a desktop, a command prompt, a network
 * terminal, the assembly screens and the monitor's own sessions (the self-test, the firmware, the installers, the
 * boot menu, the KVM) are all menus the server opened, so the menu the player holds is the proof, reach included.
 *
 * <p>Whatever a handler sets going on the network is the sender's doing, however late it finishes, so every handler
 * registered here runs as the player who sent it.
 */
public final class ComputerAccess {

    private ComputerAccess() {
    }

    /** Registers a payload a client sends, handled only for a sender its gate admits. */
    public static <P extends CustomPacketPayload> void accept(final PayloadRegistrar registrar,
                                                              final CustomPacketPayload.Type<P> type,
                                                              final StreamCodec<? super RegistryFriendlyByteBuf, P> codec,
                                                              final IPayloadGate<P> gate,
                                                              final IServerPayloadHandler<P> handler) {
        ClientPayloads.accept(registrar, type, codec, gate, acting(handler));
    }

    /** The same, for a payload whose sender has to be told when it is refused. */
    public static <P extends CustomPacketPayload> void accept(
            final PayloadRegistrar registrar,
            final CustomPacketPayload.Type<P> type,
            final StreamCodec<? super RegistryFriendlyByteBuf, P> codec,
            final IPayloadGate<P> gate,
            final IServerPayloadHandler<P> handler,
            @Nullable final IPayloadRefusal<P> refusal) {
        ClientPayloads.accept(registrar, type, codec, gate, acting(handler), refusal);
    }

    /**
     * Registers a payload that acts on the menu the player has open, on the block the payload names: the handler is
     * handed that menu, already checked, and checks nothing again.
     */
    public static <P extends CustomPacketPayload, M extends AbstractContainerMenu> void onMenu(
            final PayloadRegistrar registrar,
            final CustomPacketPayload.Type<P> type,
            final StreamCodec<? super RegistryFriendlyByteBuf, P> codec,
            final Class<M> kind,
            final Function<M, BlockPos> at,
            final Function<P, BlockPos> pos,
            final IMenuPayloadHandler<P, M> handler) {
        ClientPayloads.onMenu(registrar, type, codec, kind, at, pos, acting(handler));
    }

    /** The same, for a payload whose sender has to be told when it is refused. */
    public static <P extends CustomPacketPayload, M extends AbstractContainerMenu> void onMenu(
            final PayloadRegistrar registrar,
            final CustomPacketPayload.Type<P> type,
            final StreamCodec<? super RegistryFriendlyByteBuf, P> codec,
            final Class<M> kind,
            final Function<M, BlockPos> at,
            final Function<P, BlockPos> pos,
            final IMenuPayloadHandler<P, M> handler,
            @Nullable final IPayloadRefusal<P> refusal) {
        ClientPayloads.onMenu(registrar, type, codec, kind, at, pos, acting(handler), refusal);
    }

    /** The same, for a payload that names nothing the menu does not already hold. */
    public static <P extends CustomPacketPayload, M extends AbstractContainerMenu> void onMenu(
            final PayloadRegistrar registrar,
            final CustomPacketPayload.Type<P> type,
            final StreamCodec<? super RegistryFriendlyByteBuf, P> codec,
            final Class<M> kind,
            final IMenuPayloadHandler<P, M> handler) {
        ClientPayloads.onMenu(registrar, type, codec, kind, acting(handler));
    }

    /** The gate around a handler, for a payload registered some other way (one that travels both ways). */
    public static <P extends CustomPacketPayload> IPayloadHandler<P> guarded(final CustomPacketPayload.Type<P> type,
                                                                            final IPayloadGate<P> gate,
                                                                            final IServerPayloadHandler<P> handler) {
        return ClientPayloads.guarded(type, gate, acting(handler), null);
    }

    /**
     * Near enough to see the place a payload names, {@code reach} blocks at most: a monitor's face in the world, which
     * anybody in sight of it may ask after, the way it is drawn for anybody in sight of it.
     */
    public static <P> IPayloadGate<P> inSightOf(final Function<P, BlockPos> place, final double reach) {
        return (player, payload) -> player.distanceToSqr(Vec3.atCenterOf(place.apply(payload))) <= reach * reach;
    }

    /**
     * At a screen of that machine: its desktop, its command prompt or its network terminal.
     *
     * <p>Which of the three does not matter to the machine, since each is the same player at the same machine through
     * a menu the server opened, and a program can be opened in more than one of them.
     */
    public static <P> IPayloadGate<P> machine(final Function<P, BlockPos> host) {
        return (player, payload) -> {
            final BlockPos shown = shownBy(player.containerMenu);
            return shown != null && shown.equals(host.apply(payload)) && player.containerMenu.stillValid(player);
        };
    }

    /**
     * At any screen of that machine on one of its monitors, its start, its firmware's setup or an installer as well:
     * for the power buttons beside the glass, which work the machine's own switch whatever the glass is showing. They
     * were drawn on every screen but refused on these, so a press there did nothing.
     */
    public static <P> IPayloadGate<P> machineOrSession(final Function<P, BlockPos> host) {
        final IPayloadGate<P> atMachine = machine(host);
        return (player, payload) -> atMachine.admits(player, payload)
                || player.containerMenu instanceof MonitorSessionMenu session
                && session.hostPos().equals(host.apply(payload)) && session.stillValid(player);
    }

    /**
     * At a screen of that machine shown on that monitor, for a payload that names the monitor it acts on (one that
     * reopens a screen there): the monitor a payload names is not trusted either.
     */
    public static <P> IPayloadGate<P> machineAt(final Function<P, BlockPos> host, final Function<P, BlockPos> monitor) {
        final IPayloadGate<P> atMachine = machine(host);
        return (player, payload) -> atMachine.admits(player, payload)
                && player.containerMenu instanceof IMonitorMenu shown
                && shown.monitorPos().equals(monitor.apply(payload));
    }

    /**
     * At a screen of that machine, or closing its desktop a moment ago: a desktop tells the machine which windows it
     * leaves open as it closes, and that reaches the server after the menu it came from is gone.
     */
    public static <P> IPayloadGate<P> machineOrClosingDesktop(final Function<P, BlockPos> host) {
        return IPayloadGate.anyOf(machine(host),
                (player, payload) -> ScreenSessions.closedDesktopOf(player, host.apply(payload)));
    }

    /**
     * At one of the monitor's own sessions on that machine, and in one of the phases that sends this payload: the
     * self-test, the boot menu, the firmware, an installer, the copy's progress, the KVM.
     */
    public static <P> IPayloadGate<P> screen(final Function<P, BlockPos> host, final MonitorSessionMenu.Phase first,
                                             final MonitorSessionMenu.Phase... more) {
        final Set<MonitorSessionMenu.Phase> phases = EnumSet.of(first, more);
        return (player, payload) -> player.containerMenu instanceof MonitorSessionMenu session
                && phases.contains(session.phase())
                && session.hostPos().equals(host.apply(payload))
                && session.stillValid(player);
    }

    /** The same, for a payload that names the monitor it acts on, which has to be the session's own. */
    public static <P> IPayloadGate<P> screenAt(final Function<P, BlockPos> host, final Function<P, BlockPos> monitor,
                                               final MonitorSessionMenu.Phase first,
                                               final MonitorSessionMenu.Phase... more) {
        final IPayloadGate<P> inSession = screen(host, first, more);
        return (player, payload) -> inSession.admits(player, payload)
                && ((MonitorSessionMenu) player.containerMenu).monitorPos().equals(monitor.apply(payload));
    }

    /** In a menu of that kind, open on that block. */
    public static <P, M extends AbstractContainerMenu> IPayloadGate<P> menu(final Class<M> kind,
                                                                           final Function<M, BlockPos> at,
                                                                           final Function<P, BlockPos> pos) {
        return IPayloadGate.menu(kind, at, pos);
    }

    /** In a menu of that kind, for a payload that names nothing beyond what the menu already holds. */
    public static <P> IPayloadGate<P> menu(final Class<? extends AbstractContainerMenu> kind) {
        return IPayloadGate.menu(kind);
    }

    /** Admitted by any one of these. */
    @SafeVarargs
    public static <P> IPayloadGate<P> anyOf(final IPayloadGate<P>... gates) {
        return IPayloadGate.anyOf(gates);
    }

    private static <P> IServerPayloadHandler<P> acting(final IServerPayloadHandler<P> handler) {
        return (payload, player, level) -> Acting.as(player, () -> handler.handle(payload, player, level));
    }

    private static <P, M extends AbstractContainerMenu> IMenuPayloadHandler<P, M> acting(
            final IMenuPayloadHandler<P, M> handler) {
        return (payload, menu, player, level) -> Acting.as(player, () -> handler.handle(payload, menu, player, level));
    }

    /** The machine a computer screen's menu is showing, or null for a menu that is not one. */
    @Nullable
    private static BlockPos shownBy(final AbstractContainerMenu menu) {
        return switch (menu) {
            case DesktopMenu desktop -> desktop.hostPos();
            case CommandPromptMenu prompt -> prompt.hostPos();
            case ComputerTerminalMenu terminal -> terminal.hostPos();
            default -> null;
        };
    }
}
