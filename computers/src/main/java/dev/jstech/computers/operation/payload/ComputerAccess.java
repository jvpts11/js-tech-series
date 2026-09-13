/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import com.mojang.logging.LogUtils;
import dev.jstech.computers.menu.CommandPromptMenu;
import dev.jstech.computers.menu.ComputerTerminalMenu;
import dev.jstech.computers.menu.DesktopMenu;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Who may send a payload that acts on a computer.
 *
 * <p>A client is not trusted to say which machine it is using: a modified one can put any position in any
 * payload. So every payload a client sends is registered here with a gate, and the gate is asked on the
 * server thread, before the handler runs, whether this player really has a screen open on that machine.
 * Nothing is let through by default: a payload its gate refuses is dropped, and the server log says so.
 *
 * <p>The gates follow the screens. A desktop, a command prompt, a network terminal and the assembly
 * screens are container menus the server opened, so the menu the player holds is the proof, reach
 * included. The firmware setup, the power-on self-test, the installer's last prompt and the KVM channel
 * picker are plain screens with no menu behind them, so the server writes down that it opened one
 * ({@link ScreenSessions}) and the gate reads that instead.
 */
public final class ComputerAccess {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** How long a player's refused payloads stay out of the log after one is written, in milliseconds. */
    private static final long QUIET_MILLIS = 10_000L;

    /** When each player last had a refusal written down; the server thread is the only one that touches it. */
    private static final Map<UUID, Long> LAST_LOGGED = new HashMap<>();

    private ComputerAccess() {
    }

    /** Whether a player may send this payload. */
    @FunctionalInterface
    public interface IGate<P> {

        boolean admits(ServerPlayer player, P payload);
    }

    /** Registers a payload a client sends, handled only for a sender its gate admits. */
    public static <P extends CustomPacketPayload> void accept(final PayloadRegistrar registrar,
                                                              final CustomPacketPayload.Type<P> type,
                                                              final StreamCodec<? super RegistryFriendlyByteBuf, P> codec,
                                                              final IGate<P> gate,
                                                              final IPayloadHandler<P> handler) {
        registrar.playToServer(type, codec, guarded(type, gate, handler));
    }

    /** The same gate around a handler, for a payload registered some other way (one that travels both ways). */
    public static <P extends CustomPacketPayload> IPayloadHandler<P> guarded(final CustomPacketPayload.Type<P> type,
                                                                            final IGate<P> gate,
                                                                            final IPayloadHandler<P> handler) {
        return (payload, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && gate.admits(player, payload)) {
                handler.handle(payload, context);
            } else {
                refused(context.player(), type);
            }
        });
    }

    /**
     * At a screen of that machine: its desktop, its command prompt or its network terminal.
     *
     * <p>Which of the three does not matter to the machine, since each is the same player at the same
     * machine through a menu the server opened, and a program can be opened in more than one of them.
     */
    public static <P> IGate<P> machine(final Function<P, BlockPos> host) {
        return (player, payload) -> {
            final BlockPos shown = shownBy(player.containerMenu);
            return shown != null && shown.equals(host.apply(payload)) && player.containerMenu.stillValid(player);
        };
    }

    /**
     * At a screen of that machine, or closing its desktop a moment ago: a desktop tells the machine which
     * windows it leaves open as it closes, and that reaches the server after the menu it came from is gone.
     */
    public static <P> IGate<P> machineOrClosingDesktop(final Function<P, BlockPos> host) {
        return anyOf(machine(host), (player, payload) -> ScreenSessions.closedDesktopOf(player, host.apply(payload)));
    }

    /** At a plain screen the server opened on that machine: the firmware, the self-test, the installer or the KVM. */
    public static <P> IGate<P> screen(final Function<P, BlockPos> host) {
        return (player, payload) -> ScreenSessions.admits(player, host.apply(payload));
    }

    /** In a menu of that kind, open on that block. */
    public static <P, M extends AbstractContainerMenu> IGate<P> menu(final Class<M> kind,
                                                                   final Function<M, BlockPos> at,
                                                                   final Function<P, BlockPos> pos) {
        return (player, payload) -> kind.isInstance(player.containerMenu)
                && at.apply(kind.cast(player.containerMenu)).equals(pos.apply(payload))
                && player.containerMenu.stillValid(player);
    }

    /** In a menu of that kind, for a payload that names nothing beyond what the menu already holds. */
    public static <P> IGate<P> menu(final Class<? extends AbstractContainerMenu> kind) {
        return (player, payload) -> kind.isInstance(player.containerMenu) && player.containerMenu.stillValid(player);
    }

    /** Admitted by any one of these. */
    @SafeVarargs
    public static <P> IGate<P> anyOf(final IGate<P>... gates) {
        final List<IGate<P>> all = List.of(gates);
        return (player, payload) -> {
            for (final IGate<P> gate : all) {
                if (gate.admits(player, payload)) {
                    return true;
                }
            }
            return false;
        };
    }

    /** Forgets a player who has left, so nothing is kept for them. */
    static void forget(final UUID player) {
        LAST_LOGGED.remove(player);
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

    /*
     * A refusal is written down once in a while per player: often enough to notice a screen sending what its
     * gate does not expect, rarely enough that a client sending nothing else cannot fill the log.
     */
    private static void refused(@Nullable final Player player, final CustomPacketPayload.Type<?> type) {
        if (player == null) {
            return;
        }
        final long now = System.currentTimeMillis();
        final Long last = LAST_LOGGED.get(player.getUUID());
        if (last != null && now - last < QUIET_MILLIS) {
            return;
        }
        LAST_LOGGED.put(player.getUUID(), now);
        LOGGER.warn("Ignored {} from {}: it did not come from a screen open on that machine",
                type.id(), player.getName().getString());
    }
}
