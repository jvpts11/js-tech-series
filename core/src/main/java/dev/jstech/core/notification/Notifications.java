/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.notification;

import dev.jstech.core.team.CoreTeams;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.util.DeclarationError;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.jetbrains.annotations.ApiStatus;

/**
 * The series' notifications: drawn on the Core's HUD, not as the game's toasts, whose way of being drawn changes with
 * every version of the game. Each has a level (information, a warning, an alert) with its icon, colour, sound and time
 * on the screen; alike ones become one with a count; a key opens the history of the recent ones; a click opens the
 * right place; and the player chooses where they appear, silences kinds and mods, and can ask not to be disturbed.
 *
 * <p>A mod declares its kinds once and sends from the server:
 *
 * <pre>{@code
 * public static final NotificationKind OVERVOLTAGE = Notifications.kind(rl("overvoltage"),
 *         NotificationLevel.ALERT, TEXTS.OVERVOLTAGE).declare();
 * ...
 * Notifications.sendToTeamOf(server, owner, OVERVOLTAGE.of(TEXTS.OVERVOLTAGE.text(), TEXTS.BURNED.with(where)));
 * }</pre>
 *
 * <p>Something the player's own game finds out is shown there with {@code NotificationsClient.show}.
 */
public final class Notifications {

    private static final String NETWORK_VERSION = "1";
    private static final Map<ResourceLocation, NotificationKind> BY_ID = new ConcurrentHashMap<>();
    private static final List<NotificationKind> ALL = new CopyOnWriteArrayList<>();

    /* What a notification that reaches a player's game is handed to: the HUD, once the game has one. */
    private static volatile Consumer<Notification> onClient = notification -> { };

    private Notifications() {
    }

    /** A kind of notification called {@code id}, of {@code level}, named {@code name}, to be declared once. */
    public static NotificationKind.Builder kind(final ResourceLocation id, final NotificationLevel level,
                                                final TextKey name) {
        return new NotificationKind.Builder(id, level, name);
    }

    /** Every kind declared, in the order they were. */
    public static List<NotificationKind> kinds() {
        return List.copyOf(ALL);
    }

    /** The kind declared as {@code id}, or none. */
    public static Optional<NotificationKind> byId(final ResourceLocation id) {
        return Optional.ofNullable(BY_ID.get(id));
    }

    /** Sends {@code notification} to {@code player}; a player whose game has no Core is not sent anything. */
    public static void send(final ServerPlayer player, final Notification notification) {
        Objects.requireNonNull(notification, "notification");
        if (player instanceof FakePlayer || !player.connection.hasChannel(NotificationPayload.TYPE)) {
            return;
        }
        PacketDistributor.sendToPlayer(player, new NotificationPayload(notification));
    }

    /** Sends it to the player {@code player} when they are on. */
    public static void sendTo(final MinecraftServer server, final UUID player, final Notification notification) {
        final ServerPlayer online = server.getPlayerList().getPlayer(player);
        if (online != null) {
            send(online, notification);
        }
    }

    /** Sends it to every player on who is on {@code player}'s team, {@code player} among them: an owner's team. */
    public static void sendToTeamOf(final MinecraftServer server, final UUID player, final Notification notification) {
        final String team = CoreTeams.teamOf(server, player);
        for (final ServerPlayer online : server.getPlayerList().getPlayers()) {
            if (online.getUUID().equals(player) || team.equals(CoreTeams.teamOf(server, online.getUUID()))) {
                send(online, notification);
            }
        }
    }

    /** Hands the notifications that reach this game to {@code sink}, the HUD's; from the client's set-up. */
    @ApiStatus.Internal
    public static void showOnClient(final Consumer<Notification> sink) {
        onClient = Objects.requireNonNull(sink, "sink");
    }

    /** Registers the payload, once, from the Core's constructor. */
    @ApiStatus.Internal
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterPayloadHandlersEvent.class, event -> event.registrar(NETWORK_VERSION)
                .playToClient(NotificationPayload.TYPE, NotificationPayload.STREAM_CODEC,
                        (payload, context) -> context.enqueueWork(() -> onClient.accept(payload.notification()))));
    }

    static NotificationKind declare(final NotificationKind kind) {
        if (BY_ID.putIfAbsent(kind.id(), kind) != null) {
            throw DeclarationError.of(kind.id().getNamespace(), "the notification kind " + kind.id(),
                    "is declared twice", "declare it once, as a static final, and make its notifications from it");
        }
        ALL.add(kind);
        return kind;
    }
}
