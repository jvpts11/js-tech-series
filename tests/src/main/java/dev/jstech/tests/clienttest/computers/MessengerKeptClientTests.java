/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.computers;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.client.CommandPromptScreen;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.DesktopWindow;
import dev.jstech.computers.client.os.MessengerApp;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.program.MessengerLog;
import dev.jstech.computers.program.Programs;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.computers.EraKit;
import dev.jstech.tests.testkit.computers.EraNetwork;
import dev.jstech.tests.testkit.computers.ServerStacks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * The conversations the Messenger Service keeps, at the server it runs on (computers/docs/design/programs.md): msgsvc
 * at a Debian server's console, listing them, deleting one and saying what it does; and the Messenger on a Frames 10
 * server's own desktop, its Kept conversations window listing what the server keeps.
 */
public final class MessengerKeptClientTests {

    private static final int SETTLE = 4;
    private static final BlockPos RACK_MONITOR = new BlockPos(6, 2, 6);
    private static final BlockPos AT_THE_RACK_MONITOR = new BlockPos(6, 2, 8);
    private static final int SERVER = 0;
    private static final String MESSENGER = "jsc:messenger";

    private MessengerKeptClientTests() {
    }

    @ClientTest(timeoutTicks = 6000)
    public static void msgsvc_atTheServersConsole_listsDeletesAndSaysWhatItDoes(final ClientTestContext ctx) {
        kept(ctx, "debian", "")
                .thenTeleport(SETTLE, AT_THE_RACK_MONITOR, Direction.NORTH)
                .thenRightClick(SETTLE, RACK_MONITOR)
                .thenAwaitScreen(CommandPromptScreen.class, EraJourney.BOOT_WAIT);
        for (final String line : new String[] {"msgsvc list", "msgsvc delete 2", "msgsvc list", "msgsvc help"}) {
            ctx.then(SETTLE * 2, () -> ctx.type(line))
                    .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER));
        }
        ctx.thenWaitUntil(() -> said(ctx, "msgsvc status"), EraJourney.SCREEN_WAIT,
                        "msgsvc to say what each of its commands does")
                .then(SETTLE, () -> ctx.mc().getToasts().clear())
                .thenScreenshot(SETTLE * 3, "msgsvc-at-the-console");
    }

    @ClientTest(timeoutTicks = 6000)
    public static void keptConversations_onTheServersDesktop_listWhatItKeeps(final ClientTestContext ctx) {
        kept(ctx, "frames_10", MESSENGER)
                .thenTeleport(SETTLE, AT_THE_RACK_MONITOR, Direction.NORTH)
                .thenRightClick(SETTLE, RACK_MONITOR)
                .thenAwaitScreen(DesktopScreen.class, EraJourney.BOOT_WAIT)
                .thenWaitUntil(() -> !ctx.screen(DesktopScreen.class).launcherLabels().isEmpty(),
                        EraJourney.SCREEN_WAIT, "the server's desktop to list its programs")
                .then(SETTLE, () -> DesktopScreen.requestOpen(MESSENGER))
                .thenWaitUntil(() -> messenger(ctx) != null && messenger(ctx).online(), EraJourney.SCREEN_WAIT,
                        "the Messenger to reach the service on its own server")
                .then(SETTLE * 5, () -> ctx.clickDesktop(messenger(ctx).keptPoint()))
                .thenWaitUntil(() -> !ctx.screen(DesktopScreen.class).dialogTitles().isEmpty(),
                        EraJourney.SCREEN_WAIT, "the Kept conversations window to open")
                .then(SETTLE, () -> ctx.mc().getToasts().clear())
                .thenScreenshot(SETTLE * 5, "kept-conversations");
    }

    /*
     * The Standard era network, a server of {@code system} in its rack running the Messenger Service, with
     * {@code program} too when one is named, four conversations kept, and a monitor beside the rack.
     */
    private static ClientTestContext kept(final ClientTestContext ctx, final String system, final String program) {
        return EraJourney.lay(ctx, EraKit.STANDARD)
                .thenBuild(0, world -> {
                    world.placeMonitor(RACK_MONITOR, Direction.SOUTH, EraKit.STANDARD.monitor());
                    world.blockEntity(EraNetwork.RACK, ServerRackBlockEntity.class).getServers()
                            .setStackInSlot(SERVER, ServerStacks.defaultServer());
                })
                .thenServer(EraJourney.SETTLE, level -> {
                    final ServerRackBlockEntity rack = rack(ctx, level);
                    rack.unitHost(SERVER).installOs(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, system));
                    rack.consoleOf(SERVER).install(Programs.MESSENGER_SERVICE.toString());
                    if (!program.isEmpty()) {
                        rack.consoleOf(SERVER).install(program);
                    }
                })
                .thenWaitUntilServer(level -> up(rack(ctx, level).unitHost(SERVER)), EraJourney.BOOT_WAIT,
                        "the rack's server to come up", level -> "running=" + rack(ctx, level).unitHost(SERVER)
                                .isRunning())
                .thenServer(SETTLE, level -> {
                    final MessengerLog log = rack(ctx, level).messengerAt(SERVER);
                    final long now = level.getGameTime();
                    log.say(MessengerLog.LOBBY, "Dev", "the smelter stalled again", now, false);
                    log.say(MessengerLog.LOBBY, "Alex", "on it, the coal ran out", now, false);
                    log.say(MessengerLog.privateRoom("Dev", "Alex"), "Alex", "iron for the new rack?", now, false);
                    log.say(MessengerLog.privateRoom("Dev", "Sam"), "Sam", "back in ten", now, false);
                    log.say(MessengerLog.privateRoom("Alex", "Sam"), "Alex", "the pool paid out", now, false);
                    rack(ctx, level).setChanged();
                });
    }

    private static ServerRackBlockEntity rack(final ClientTestContext ctx, final ServerLevel level) {
        return (ServerRackBlockEntity) level.getBlockEntity(ctx.abs(EraNetwork.RACK));
    }

    private static boolean up(final IOsHost server) {
        return server.isRunning() && !server.needsPost() && !server.booting() && !server.atBootMenu()
                && server.crashPage() == null;
    }

    private static boolean said(final ClientTestContext ctx, final String what) {
        return ctx.mc().screen instanceof CommandPromptScreen<?> screen
                && screen.scrollbackText().stream().anyMatch(line -> line.contains(what));
    }

    @Nullable
    private static MessengerApp messenger(final ClientTestContext ctx) {
        final DesktopScreen desktop = ctx.openScreen(DesktopScreen.class);
        final DesktopWindow window = desktop == null ? null : desktop.windowFor(MESSENGER);
        return window != null && window.app() instanceof MessengerApp app ? app : null;
    }
}
