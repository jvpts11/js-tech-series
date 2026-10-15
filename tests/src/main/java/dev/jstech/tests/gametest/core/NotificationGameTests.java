/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import com.mojang.authlib.GameProfile;
import dev.jstech.core.diagnostic.DiagnosticNotifications;
import dev.jstech.core.notification.Notification;
import dev.jstech.core.notification.NotificationAction;
import dev.jstech.core.notification.NotificationKind;
import dev.jstech.core.notification.NotificationLevel;
import dev.jstech.core.notification.Notifications;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.util.DeclarationError;
import dev.jstech.tests.JsTests;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.time.Duration;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Notifications, as the server knows them: kinds declared once with their level and time on the screen, a
 * notification that travels whole with its action, and sending that never fails a player who cannot show one. What
 * the player's game does with them (the HUD, the history) is the client tests'.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class NotificationGameTests {

    private static final String ARENA = "empty";

    private NotificationGameTests() {
    }

    @GameTest(template = ARENA)
    public static void kinds_areDeclaredOnceWithTheirLevelAndTime(final GameTestHelper helper) {
        final NotificationKind kind = Probe.KIND;
        helper.assertTrue(Notifications.byId(kind.id()).orElseThrow() == kind, "a kind is found by its id");
        helper.assertTrue(kind.lasts() == Duration.ofSeconds(3).toMillis(), "and lasts as long as it says");
        helper.assertTrue(DiagnosticNotifications.STALLED.lasts() == NotificationLevel.WARNING.defaultMillis(),
                "a kind that says nothing lasts as long as its level does");
        boolean refused = false;
        try {
            Notifications.kind(kind.id(), NotificationLevel.INFO, Probe.NAME).declare();
        } catch (final DeclarationError twice) {
            refused = true;
        }
        helper.assertTrue(refused, "a kind declared twice stops the game loading");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void notification_travelsWholeWithItsAction(final GameTestHelper helper) {
        final Notification toManual = Probe.KIND.of(Probe.NAME.text(), Text.literal("Metallurgy I"))
                .withAction(new NotificationAction.OpenManual("jscore:technical_reference", "jscore:cable_block"));
        final Notification toPlace = Probe.KIND.of(Probe.NAME.text(), Text.literal("120, 64, -30"))
                .withAction(new NotificationAction.ShowPlace(Level.NETHER, new BlockPos(120, 64, -30)));
        final Notification bare = Probe.KIND.of(Probe.NAME.text(), Text.literal("nothing to click"));
        for (final Notification sent : new Notification[] {toManual, toPlace, bare}) {
            final ByteBuf buffer = Unpooled.buffer();
            try {
                Notification.STREAM_CODEC.encode(buffer, sent);
                final Notification read = Notification.STREAM_CODEC.decode(buffer);
                // A title travels as its key, its English staying with the sender's language file.
                helper.assertTrue(read.kind().equals(sent.kind()) && read.level() == sent.level()
                        && read.title() instanceof Text.Translated title && title.key().key().equals(Probe.NAME.key())
                        && read.body().equals(sent.body()) && read.action().equals(sent.action()),
                        "a notification reads back as it was sent: " + read);
            } finally {
                buffer.release();
            }
        }
        helper.assertTrue(toPlace.level() == NotificationLevel.ALERT, "a notification has its kind's level");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void send_skipsAPlayerWhoseGameCannotShowIt(final GameTestHelper helper) {
        final Notification notification = Probe.KIND.of(Probe.NAME.text(), Text.literal("to nobody"));
        final ServerPlayer fake = FakePlayerFactory.get(helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "jstests-notified"));
        Notifications.send(fake, notification);
        Notifications.sendTo(helper.getLevel().getServer(), UUID.randomUUID(), notification);
        Notifications.sendToTeamOf(helper.getLevel().getServer(), fake.getUUID(), notification);
        helper.succeed();
    }

    /* The kind these tests use, declared once for the whole run. */
    private static final class Probe {

        private static final TextKey NAME = TextKey.of("jstests.notification.probe", "Probe");
        private static final NotificationKind KIND = Notifications.kind(
                ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "probe"), NotificationLevel.ALERT, NAME)
                .lasts(Duration.ofSeconds(3)).declare();

        private Probe() {
        }
    }
}
