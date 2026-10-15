/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import dev.jstech.core.client.hud.HudElements;
import dev.jstech.core.client.notification.NotificationHistoryScreen;
import dev.jstech.core.client.notification.NotificationPlaceScreen;
import dev.jstech.core.client.notification.NotificationsClient;
import dev.jstech.core.config.ConfigDraft;
import dev.jstech.core.diagnostic.DiagnosticNotifications;
import dev.jstech.core.gui.HudStack;
import dev.jstech.core.notification.Notification;
import dev.jstech.core.notification.NotificationAction;
import dev.jstech.core.notification.NotificationLevel;
import dev.jstech.core.notification.NotificationSettings;
import dev.jstech.core.text.Text;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.Optional;
import net.minecraft.Util;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * The notifications on a player's game: on the HUD in the shape of the game's toasts, alike ones gathered with a
 * count, the history opened over the game with its filters, silencing and "do not disturb", and a notification's
 * place shown on a map.
 */
public final class NotificationsClientTests {

    /** The notifications' place on the HUD, and how tall a slot of the game's toasts is. */
    private static final ResourceLocation HUD = ResourceLocation.fromNamespaceAndPath("jscore", "notifications");
    private static final int TOAST_SLOT = 32;

    private NotificationsClientTests() {
    }

    @ClientTest(timeoutTicks = 300)
    public static void hud_showsNotificationsInTheShapeOfTheGamesToasts(final ClientTestContext ctx) {
        ctx.afterTest(() -> reset(ctx));
        ctx.then(0, () -> {
            reset(ctx);
            ctx.mc().getToasts().clear();
            SystemToast.addOrUpdate(ctx.mc().getToasts(), SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                    Component.literal("A toast of the game's own"), null);
            showThree(ctx);
        })
                .thenAssert(2, () -> NotificationsClient.board().showing(Util.getMillis()).size() == 3,
                        "three notifications on the screen")
                .thenAssert(0, () -> NotificationsClient.board().history().stream()
                        .anyMatch(entry -> entry.count() == 3), "the three alike warnings are one, counted")
                .thenAssert(0, () -> HudElements.placedIn(HudStack.Corner.TOP_RIGHT).get(HUD).y() >= TOAST_SLOT,
                        "and stand under the game's own toast in the corner, not over it")
                .thenScreenshot(2, "notifications_hud");
    }

    @ClientTest(timeoutTicks = 400)
    public static void history_filtersSilencesAndHoldsEverything(final ClientTestContext ctx) {
        ctx.afterTest(() -> reset(ctx));
        ctx.then(0, () -> {
            reset(ctx);
            showThree(ctx);
            NotificationsClient.openHistory();
        })
                .thenAwaitScreen(NotificationHistoryScreen.class, 40)
                .thenAssert(1, () -> history(ctx).entries().size() == 3, "the history lists the three, newest first")
                .thenScreenshot(2, "notifications_history")
                .then(0, () -> {
                    history(ctx).setLevel(NotificationLevel.ALERT);
                    ctx.assertTrue(history(ctx).entries().stream()
                            .allMatch(entry -> entry.level() == NotificationLevel.ALERT), "the filter shows alerts only");
                    history(ctx).setLevel(null);
                    history(ctx).nextPlace();
                    ctx.assertEquals(NotificationSettings.TOP_LEFT, NotificationSettings.place(),
                            "the place moves to the next corner and is kept");
                    NotificationsClient.toggleKind(DiagnosticNotifications.SLOWED.id().toString());
                    ctx.assertTrue(NotificationsClient.board().silenced(DiagnosticNotifications.SLOWED.id().toString(),
                            "jscore"), "a kind silenced is silenced on the board");
                    NotificationsClient.setDoNotDisturb(true);
                    // A title of its own, so it is no alike of the one already in the history.
                    NotificationsClient.show(DiagnosticNotifications.RECOVERED.of(Text.literal("Held"),
                            Text.literal("held while not to be disturbed")));
                    ctx.assertTrue(NotificationsClient.board().showing(Util.getMillis()).isEmpty(),
                            "nothing comes on the screen while not to be disturbed");
                    ctx.assertEquals(4, history(ctx).entries().size(), "but the history keeps it");
                })
                .thenScreenshot(2, "notifications_history_held");
    }

    @ClientTest(timeoutTicks = 300)
    public static void place_showsTheNotificationsPlaceOnAMap(final ClientTestContext ctx) {
        ctx.afterTest(() -> reset(ctx));
        ctx.then(0, () -> {
            reset(ctx);
            final BlockPos near = ctx.player().blockPosition().offset(12, 0, -8);
            final Notification notification = DiagnosticNotifications.STALLED.of(
                    DiagnosticNotifications.STALLED_NAME.text(), Text.literal(near.toShortString()))
                    .withAction(new NotificationAction.ShowPlace(Level.OVERWORLD, near));
            ctx.assertTrue(NotificationsClient.act(notification, null), "a notification with a place acts");
        })
                .thenAwaitScreen(NotificationPlaceScreen.class, 40)
                .thenAssert(2, () -> ctx.screen(NotificationPlaceScreen.class).where().contains("blocks"),
                        "under the map, where the place is and how far")
                .thenScreenshot(20, "notifications_place");
    }

    /* An alert with a place, three alike warnings and a piece of information with a page. */
    private static void showThree(final ClientTestContext ctx) {
        final BlockPos at = ctx.player().blockPosition().offset(20, 0, 5);
        NotificationsClient.show(DiagnosticNotifications.STALLED.of(DiagnosticNotifications.STALLED_NAME.text(),
                Text.literal("jsc:mainframe has run for 1204 ms")).withAction(new NotificationAction.ShowPlace(
                Level.OVERWORLD, at)));
        for (int i = 0; i < 3; i++) {
            NotificationsClient.show(DiagnosticNotifications.SLOWED.of(DiagnosticNotifications.SLOWED_NAME.text(),
                    Text.literal("jscore:fluid_grid runs one tick in " + (2 << i))));
        }
        NotificationsClient.show(new Notification(DiagnosticNotifications.RECOVERED.id(), NotificationLevel.INFO,
                DiagnosticNotifications.RECOVERED_NAME.text(), Text.literal("jscore:energy_grid is at pace again"),
                Optional.of(new NotificationAction.OpenManual("jscore:technical_reference", ""))));
    }

    /* Every notification forgotten and every setting of the notifications back to its default. */
    private static void reset(final ClientTestContext ctx) {
        ctx.mc().setScreen(null);
        ctx.mc().getToasts().clear();
        NotificationsClient.board().clear();
        final ConfigDraft defaults = new ConfigDraft(NotificationSettings.FILE);
        defaults.defaults();
        defaults.apply();
    }

    private static NotificationHistoryScreen history(final ClientTestContext ctx) {
        return ctx.screen(NotificationHistoryScreen.class);
    }
}
