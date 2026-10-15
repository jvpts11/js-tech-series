/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.notification;

import dev.jstech.core.JsCore;
import dev.jstech.core.client.audio.AudioEngine;
import dev.jstech.core.client.guide.GuideClient;
import dev.jstech.core.client.hud.HudElements;
import dev.jstech.core.client.input.KeyActionsClient;
import dev.jstech.core.gui.HudStack;
import dev.jstech.core.input.CoreKeys;
import dev.jstech.core.notification.Notification;
import dev.jstech.core.notification.NotificationAction;
import dev.jstech.core.notification.NotificationBoard;
import dev.jstech.core.notification.NotificationKind;
import dev.jstech.core.notification.NotificationSettings;
import dev.jstech.core.notification.NotificationSounds;
import dev.jstech.core.notification.Notifications;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.ToastAddEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

/**
 * The notifications on a player's game: those the server sends and those the game finds out itself
 * ({@link #show}), drawn on the Core's HUD in the corner the player chose, with their sound; the history, opened by its
 * key (N unless the player changes it); and a click on one, while the chat is open, does what it offers.
 */
public final class NotificationsClient {

    private static final ResourceLocation HUD = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "notifications");
    private static final NotificationBoard<Notification> BOARD = new NotificationBoard<>();
    private static final NotificationToasts TOASTS = new NotificationToasts(BOARD);

    private NotificationsClient() {
    }

    /** Hands the buses what this answers, once, from the Core's constructor on a player's game. */
    public static void listen(final IEventBus modBus) {
        Notifications.showOnClient(NotificationsClient::show);
        HudElements.declare(HUD, NotificationsClient::corner, TOASTS);
        KeyActionsClient.onPress(CoreKeys.NOTIFICATIONS, NotificationsClient::openHistory);
        NeoForge.EVENT_BUS.addListener(ToastAddEvent.class, event -> {
            if (BOARD.doNotDisturb() && NotificationSettings.holdGameToasts()) {
                event.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener(ScreenEvent.MouseButtonPressed.Pre.class, NotificationsClient::onClick);
        NotificationSettings.FILE.onAnyChange(key -> applySettings());
        applySettings();
    }

    /** Shows {@code notification} on this game, as if the server had sent it; on the game's thread. */
    public static void show(final Notification notification) {
        final NotificationKind kind = Notifications.byId(notification.kind()).orElse(null);
        final long lasts = kind != null ? kind.lasts() : notification.level().defaultMillis();
        final NotificationBoard.Posted<Notification> posted = BOARD.post(notification, groupOf(notification),
                notification.kind().toString(), notification.kind().getNamespace(), notification.level(), lasts,
                Util.getMillis());
        if (posted.audible()) {
            AudioEngine.playOnScreen(NotificationSounds.of(notification.level()));
        }
    }

    /** The notifications of this game: on the screen and in the history. */
    public static NotificationBoard<Notification> board() {
        return BOARD;
    }

    /** Where they appear, as the player chose. */
    public static HudStack.Corner corner() {
        return switch (NotificationSettings.place()) {
            case NotificationSettings.TOP_LEFT -> HudStack.Corner.TOP_LEFT;
            case NotificationSettings.BOTTOM_LEFT -> HudStack.Corner.BOTTOM_LEFT;
            case NotificationSettings.BOTTOM_RIGHT -> HudStack.Corner.BOTTOM_RIGHT;
            default -> HudStack.Corner.TOP_RIGHT;
        };
    }

    /** Opens the history over whatever is open, or over the game. */
    public static void openHistory() {
        final Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new NotificationHistoryScreen(minecraft.screen));
    }

    /** Does what {@code notification} offers to do when clicked, from {@code parent}; whether it offered anything. */
    public static boolean act(final Notification notification, @Nullable final Screen parent) {
        if (notification.action().isEmpty()) {
            return false;
        }
        switch (notification.action().get()) {
            case NotificationAction.OpenManual open -> GuideClient.open(open.manual(), open.entry(), parent);
            case NotificationAction.ShowPlace place -> Minecraft.getInstance().setScreen(
                    new NotificationPlaceScreen(notification, place, parent));
        }
        return true;
    }

    /** The name of the mod {@code modid}, as its list of mods calls it. */
    public static String modName(final String modid) {
        return ModList.get().getModContainerById(modid).map(mod -> mod.getModInfo().getDisplayName()).orElse(modid);
    }

    /** What the player calls the kind {@code kind}: its declared name, or its id when this game does not know it. */
    public static Text kindName(final String kind) {
        final ResourceLocation id = ResourceLocation.tryParse(kind);
        return id == null ? Text.literal(kind) : Notifications.byId(id).map(found -> found.name().text())
                .orElse(Text.literal(kind));
    }

    /** Silences the kind {@code kind}, or lets it be heard again; kept in the player's settings. */
    public static void toggleKind(final String kind) {
        NotificationSettings.FILE.set(NotificationSettings.SILENCED_KINDS, toggled(
                NotificationSettings.silencedKinds(), kind));
    }

    /** Silences everything of the mod {@code modid}, or lets it be heard again. */
    public static void toggleMod(final String modid) {
        NotificationSettings.FILE.set(NotificationSettings.SILENCED_MODS, toggled(
                NotificationSettings.silencedMods(), modid));
    }

    /** Whether the player is not to be disturbed, kept in their settings. */
    public static void setDoNotDisturb(final boolean value) {
        NotificationSettings.FILE.set(NotificationSettings.DO_NOT_DISTURB, value);
    }

    /** Where they appear, kept in the player's settings. */
    public static void setPlace(final String place) {
        NotificationSettings.FILE.set(NotificationSettings.PLACE, place);
    }

    /* While the chat is open the pointer is free over the HUD: a click on a notification does what it offers. */
    private static void onClick(final ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof ChatScreen) || event.getButton() != 0) {
            return;
        }
        TOASTS.at(event.getMouseX(), event.getMouseY()).ifPresent(entry -> {
            BOARD.dismiss(entry.id());
            if (act(entry.item(), null)) {
                event.setCanceled(true);
            }
        });
    }

    private static void applySettings() {
        BOARD.setDoNotDisturb(NotificationSettings.doNotDisturb());
        BOARD.silence(new HashSet<>(NotificationSettings.silencedKinds()),
                new HashSet<>(NotificationSettings.silencedMods()));
        BOARD.setMostKept(NotificationSettings.history());
    }

    /* Alike notifications: of one kind, under one title. */
    private static String groupOf(final Notification notification) {
        return notification.kind() + "\n" + GameText.resolve(notification.title());
    }

    private static List<String> toggled(final List<String> list, final String one) {
        final Set<String> out = new HashSet<>(list);
        if (!out.remove(one)) {
            out.add(one);
        }
        final List<String> sorted = new ArrayList<>(out);
        sorted.sort(String::compareTo);
        return sorted;
    }
}
