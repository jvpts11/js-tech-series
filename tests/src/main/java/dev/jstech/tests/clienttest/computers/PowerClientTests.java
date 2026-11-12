/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.blockentity.UpsBlockEntity;
import dev.jstech.computers.client.CommandPromptScreen;
import dev.jstech.computers.client.ServerRackScreen;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.DesktopWindow;
import dev.jstech.computers.client.os.IDesktopApp;
import dev.jstech.computers.client.os.TaskManagerApp;
import dev.jstech.computers.gui.layout.ServerRackLayout;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.item.RackUnitItem;
import dev.jstech.computers.power.UpsRating;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.computers.TestComputerEnergy;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * What a machine shows of its power (computers/docs/design/power.md), shot for the side-by-side with its mocks: the
 * notice Frames 10 raises from the battery in its tray as its UPS takes over from a line that went, Plasma's power
 * manager's, the two lines MC-DOS prints on its console, the Power pages of the Task Manager on XP and Frames 11 and
 * the System Monitor of Frames 10 on the line, and a rack's screen with its UPS on the battery, its PDU, and a drive
 * shelf under a storage server and then alone.
 *
 * <p>The computers of a test run draw nothing; these turn the draw on with the UPS's line fed from the start, as a
 * cable at full flow would, and let the line go once the screen is up. The draw goes off again as each test ends.
 */
public final class PowerClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 120;
    private static final int BOOT_WAIT = 1_200;
    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    /* The UPS west of the computer, facing north: the computer stands against its east side. */
    private static final BlockPos UPS = new BlockPos(4, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);
    private static final BlockPos RACK = new BlockPos(3, 2, 3);
    /* The rack's units from the top, as the mock has them: its UPS, two batteries, the PDU, a server and a shelf. */
    private static final int UPS_ROW = 0;
    private static final int PDU_ROW = 3;
    private static final int SERVER_ROW = 4;
    private static final int SHELF_ROW = 6;
    /* The server's ten bays and seven of its shelf's. */
    private static final int DRIVES = 17;
    private static final int PERCENT = 64;
    private static final String TASK_MANAGER = "jsc:task_manager";
    private static final String SYSTEM_MONITOR = "jsc:system_monitor";
    /* The Power tab of XP's Task Manager, and the Power page of Frames 11's, counted from the first. */
    private static final int XP_POWER_PAGE = 4;
    private static final int FRAMES_11_POWER_PAGE = 7;
    /* Long enough for a page's histories to draw a few seconds of the line. */
    private static final int FIGURES_WAIT = 100;

    private PowerClientTests() {
    }

    @ClientTest(timeoutTicks = 2400)
    public static void upsNotice_frames10RaisesItFromTheBatteryInItsTray(final ClientTestContext ctx) {
        desktopOnBattery(ctx, "frames_10", null, "frames-10-on-battery");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void upsNotice_plasmaRaisesItFromItsPowerManager(final ClientTestContext ctx) {
        desktopOnBattery(ctx, "ubuntu", "jsc:kde_plasma", "plasma-on-battery");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void upsNotice_mcDosPrintsItsTwoLinesOnTheConsole(final ClientTestContext ctx) {
        drawOffAfter(ctx);
        computerOnUps(ctx, "mc_dos", null)
                .thenAwaitScreen(CommandPromptScreen.class, BOOT_WAIT)
                .thenServer(SETTLE, level -> TestComputerEnergy.letGo())
                .thenWaitUntil(() -> String.join("\n", ctx.screen(CommandPromptScreen.class).scrollbackText())
                        .contains("UPS: save your work"), SCREEN_WAIT, "the console to print the UPS's two lines")
                .thenScreenshot(SETTLE, "mc-dos-on-battery");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void taskManager_framesXpShowsItsPowerTab(final ClientTestContext ctx) {
        powerPage(ctx, "frames_xp", TASK_MANAGER, XP_POWER_PAGE, "frames-xp-task-manager-power");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void taskManager_frames11ShowsItsPowerPage(final ClientTestContext ctx) {
        powerPage(ctx, "frames_11", TASK_MANAGER, FRAMES_11_POWER_PAGE, "frames-11-task-manager-power");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void systemMonitor_frames10ShowsItsPowerAndTemperature(final ClientTestContext ctx) {
        powerPage(ctx, "frames_10", SYSTEM_MONITOR, -1, "frames-10-system-monitor");
    }

    @ClientTest(timeoutTicks = 1200)
    public static void rackScreen_showsItsUpsOnTheBatteryItsPduAndItsDriveShelf(final ClientTestContext ctx) {
        drawOffAfter(ctx);
        // An empty hand: a rack clicked with a server in it would mount the server instead of opening.
        ctx.thenGive(0).thenBuild(0, world -> {
            world.setBlock(RACK, ComputingModule.SERVER_RACK.get().defaultBlockState()
                    .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
            final ServerRackBlockEntity rack = world.blockEntity(RACK, ServerRackBlockEntity.class);
            rack.getServers().setStackInSlot(UPS_ROW, new ItemStack(ComputingModule.RACK_UPS.get()));
            rack.getServers().setStackInSlot(UPS_ROW + 1, new ItemStack(ComputingModule.UPS_BATTERY_EXPANSION.get()));
            rack.getServers().setStackInSlot(UPS_ROW + 2, new ItemStack(ComputingModule.UPS_BATTERY_EXPANSION.get()));
            rack.getServers().setStackInSlot(PDU_ROW, new ItemStack(ComputingModule.PDU.get()));
            rack.getServers().setStackInSlot(SERVER_ROW, new ItemStack(ComputingModule.STORAGE_SERVER.get()));
            rack.getServers().setStackInSlot(SHELF_ROW, new ItemStack(ComputingModule.DRIVE_SHELF.get()));
            for (int drive = 0; drive < DRIVES; drive++) {
                rack.insertDrive(SERVER_ROW, new ItemStack(ComputingModule.disk(StorageTier.NVME, DiskSize.TB_1)));
            }
            final UpsRating rating = RackUnitItem.rackUps(ServerRackBlockEntity.CAPACITY_U,
                    rack.getServers()::getStackInSlot);
            if (rating != null) {
                rack.setUpsCharge(rating.batteryJoules() * PERCENT / 100);
            }
        });
        ctx.thenServer(SETTLE, level -> TestComputerEnergy.drawing(true))
                .thenTeleport(SETTLE, RACK.south(2), Direction.NORTH)
                .thenRightClick(SETTLE, RACK)
                .thenAwaitScreen(ServerRackScreen.class, SCREEN_WAIT)
                .thenWaitUntilServer(level -> rack(ctx, level).upsReading().onBattery(), SCREEN_WAIT,
                        "the rack's UPS to carry it on its battery", level -> "ups=" + rack(ctx, level).upsReading())
                .then(SETTLE, () -> pick(ctx, UPS_ROW))
                .thenScreenshot(SETTLE * 3, "rack-ups-on-battery")
                .then(0, () -> pick(ctx, PDU_ROW))
                .thenScreenshot(SETTLE, "rack-pdu")
                .then(0, () -> pick(ctx, SHELF_ROW))
                .thenScreenshot(SETTLE, "rack-shelf")
                .thenServer(0, level -> rack(ctx, level).getServers().setStackInSlot(SERVER_ROW, ItemStack.EMPTY))
                .then(SETTLE, () -> pick(ctx, SHELF_ROW))
                .thenScreenshot(SETTLE, "rack-shelf-alone")
                .then(0, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAwaitNoScreen(SCREEN_WAIT);
    }

    /*
     * A running computer of {@code system} drawing from the line through its UPS, {@code program} opened on its desktop
     * and turned to its page {@code page} when one is given, its figures given a moment to gather, and shot.
     */
    private static void powerPage(final ClientTestContext ctx, final String system, final String program,
                                  final int page, final String label) {
        drawOffAfter(ctx);
        computerOnUps(ctx, system, null)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> !ctx.screen(DesktopScreen.class).launcherLabels().isEmpty(), SCREEN_WAIT,
                        "the desktop to list its programs")
                .then(SETTLE, () -> DesktopScreen.requestOpen(program))
                .thenWaitUntil(() -> app(ctx, program) != null, SCREEN_WAIT, program + " to open");
        if (page >= 0) {
            ctx.thenWaitUntil(() -> app(ctx, program) instanceof TaskManagerApp manager
                            && manager.pageCentre(page) != null, SCREEN_WAIT, "the page picker to be drawn")
                    .then(SETTLE, () -> ctx.clickDesktop(((TaskManagerApp) app(ctx, program)).pageCentre(page)));
        }
        ctx.thenScreenshot(FIGURES_WAIT, label);
    }

    @Nullable
    private static IDesktopApp app(final ClientTestContext ctx, final String program) {
        final DesktopScreen desktop = ctx.openScreen(DesktopScreen.class);
        final DesktopWindow window = desktop == null ? null : desktop.windowFor(program);
        return window == null ? null : window.app();
    }

    /* A running computer of {@code system} on a UPS whose line goes with its desktop up, and the notice it raises. */
    private static void desktopOnBattery(final ClientTestContext ctx, final String system,
                                         @Nullable final String desktop, final String label) {
        drawOffAfter(ctx);
        computerOnUps(ctx, system, desktop)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> !ctx.screen(DesktopScreen.class).launcherLabels().isEmpty(), SCREEN_WAIT,
                        "the desktop to list its programs")
                .thenServer(SETTLE, level -> TestComputerEnergy.letGo())
                .thenWaitUntil(() -> !ctx.screen(DesktopScreen.class).balloonTitle().isEmpty(), SCREEN_WAIT,
                        "the notice that the UPS carries the computer")
                .thenScreenshot(SETTLE, label);
    }

    /*
     * A running computer of {@code system}, with {@code desktop} over it when one is named, against a Standard UPS
     * charged to {@link #PERCENT}, and its monitor opened.
     */
    private static ClientTestContext computerOnUps(final ClientTestContext ctx, final String system,
                                                   @Nullable final String desktop) {
        return ctx.thenBuild(0, world -> {
                    final PersonalComputerBlockEntity pc = world.placeRunningPersonalComputer(COMPUTER,
                            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, system));
                    if (desktop != null) {
                        pc.console().install(desktop);
                    }
                    world.setBlock(UPS, ComputingModule.STANDARD_UPS.get());
                    final UpsBlockEntity ups = world.blockEntity(UPS, UpsBlockEntity.class);
                    ups.setCharge(UpsRating.POWERGUARD_800.batteryJoules() * PERCENT / 100);
                    world.placeMonitor(MONITOR, Direction.EAST);
                    // The line feeds the UPS at its back, and through it the computer, before anything draws.
                    TestComputerEnergy.keepFed(Objects.requireNonNull(ups.energyPort(Direction.SOUTH), "no inlet"));
                })
                .thenServer(SETTLE * 2, level -> TestComputerEnergy.drawing(true))
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR);
    }

    /* The line and the draw go off again however the test ends, so no other test sees them. */
    private static void drawOffAfter(final ClientTestContext ctx) {
        ctx.afterTest(() -> ctx.server().execute(() -> {
            TestComputerEnergy.letGo();
            TestComputerEnergy.drawing(false);
        }));
    }

    /* A click on a unit's faceplate, clear of its buttons, which puts it on the screen's card. */
    private static void pick(final ClientTestContext ctx, final int row) {
        ctx.clickGui(ServerRackLayout.PWR_X - 8, ServerRackLayout.rowY(row) + 10);
    }

    private static ServerRackBlockEntity rack(final ClientTestContext ctx, final ServerLevel level) {
        return TestWorldBuilder.at(level, ctx.origin()).blockEntity(RACK, ServerRackBlockEntity.class);
    }
}
