/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.HardwareItems;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.block.MonitorBlock;
import dev.jstech.computers.block.MonitorKind;
import dev.jstech.computers.block.MonitorPanel;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.MonitorBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.client.AbstractComputerScreen;
import dev.jstech.computers.client.BootSequenceScreen;
import dev.jstech.computers.client.CommandPromptScreen;
import dev.jstech.computers.client.InstallerScreen;
import dev.jstech.computers.client.SystemBootScreen;
import dev.jstech.computers.client.monitor.MonitorPainter;
import dev.jstech.computers.client.monitor.MonitorPictureCache;
import dev.jstech.computers.client.monitor.SessionFaces;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.DesktopWindow;
import dev.jstech.computers.client.os.IDesktopApp;
import dev.jstech.computers.client.os.OffscreenDesktop;
import dev.jstech.computers.client.theme.MonitorFrameStyle;
import dev.jstech.computers.gui.layout.PowerStripLayout;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.monitor.IMonitorPicture;
import dev.jstech.computers.operation.payload.firmware.FirmwarePayloads;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.media.MediaItem;
import dev.jstech.computers.os.media.MediaKind;
import dev.jstech.computers.os.media.MediaReaderBlockEntity;
import dev.jstech.computers.program.Programs;
import dev.jstech.core.client.live.LiveScreens;
import dev.jstech.core.gui.Tube;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.HashSet;
import java.util.Set;
import java.util.function.IntPredicate;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * The monitors as a player sees them: every era's face from its real counterpart, the machine's screen live on the
 * glass of one nearby and gone from a distance, the Vintage systems in the tube's own colours (amber on an amber
 * monitor, sixteen colours on a CGA), flat panels joined into one screen, the power strip on an opened screen
 * shutting the machine down, and the face showing what a player at the machine sees: its start by the very screens
 * that show it, its installer on the page the player is on, and a program open at it by that program.
 */
public final class MonitorClientTests {

    private static final int SETTLE = 4;
    private static final int BOOT_WAIT = 1_200;
    private static final int LIGHT_WAIT = 400;
    private static final ResourceLocation MC_DOS = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "mc_dos");
    private static final String MINESWEEPER = "Minesweeper";
    /* The eight monitors in a row along x, two blocks apart, their screens toward the player to the south. */
    private static final BlockPos ROW = new BlockPos(2, 2, 2);
    private static final BlockPos ROW_VIEW = new BlockPos(9, 2, 8);
    private static final int STANDARD_INDEX = 6;
    private static final BlockPos FAR = new BlockPos(9, 2, 50);
    /* A wall of flat monitors three wide and two tall, a computer behind its bottom-left corner. */
    private static final BlockPos WALL = new BlockPos(3, 2, 3);
    private static final BlockPos WALL_VIEW = new BlockPos(4, 2, 8);
    /* A monitor on a machine, with the player standing at it to open it. */
    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    private static final BlockPos SCREEN = new BlockPos(6, 2, 2);
    private static final BlockPos AT_SCREEN = new BlockPos(8, 2, 2);
    private static final BlockPos DRIVE = new BlockPos(4, 2, 2);
    private static final ResourceLocation FREEBSD =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "freebsd");
    /* How far the power button stands out of the face, a hair less than its model's half pixel. */
    private static final double BUTTON_PROUD = 0.02;
    /* Where the player stands to look down at that monitor's face, and how far down. */
    private static final BlockPos FACE_VIEW = new BlockPos(9, 2, 2);
    private static final float FACE_VIEW_PITCH = 22.0F;
    /* A folder made on a Frames desktop, and the program pinned to its panel. */
    private static final String FOLDER = "Users/Public/Desktop/Projects";
    private static final String CALCULATOR_KEY = "calculator";

    private MonitorClientTests() {
    }

    @ClientTest(timeoutTicks = 2400)
    public static void monitors_wearTheirFacesAndShowTheirMachineLiveNearby(final ClientTestContext ctx) {
        final Block[] kinds = kinds();
        final BlockPos standard = ROW.east(STANDARD_INDEX * 2);
        ctx.thenBuild(0, world -> {
                    for (int i = 0; i < kinds.length; i++) {
                        world.placeMonitor(ROW.east(i * 2), Direction.NORTH, kinds[i]);
                    }
                })
                .thenTeleport(SETTLE, ROW_VIEW, Direction.NORTH)
                .thenScreenshot(20, "all-faces")
                .thenBuild(0, world -> world.placeRunningPersonalComputer(standard.north()))
                .thenWaitUntil(() -> lit(ctx, standard), LIGHT_WAIT, "the Standard monitor to light")
                .thenWaitUntil(() -> MonitorPictureCache.of(ctx.abs(standard)) instanceof IMonitorPicture.Desktop,
                        BOOT_WAIT, "the server to say the face shows the machine's desktop")
                .thenWaitUntil(() -> LiveScreens.held() > 0, 100, "the face to be drawn")
                .thenScreenshot(30, "live-desktop")
                // From further than anybody could read it, only the light shows and the picture is let go.
                .thenTeleport(0, FAR, Direction.NORTH)
                .thenWaitUntil(() -> LiveScreens.held() == 0, 300, "the picture to be let go far off");
    }

    @ClientTest(timeoutTicks = 3600)
    public static void tube_showsMcDosInAmberOnAnAmberMonitor(final ClientTestContext ctx) {
        openVintagePrompt(ctx, ComputingModule.AMBER_MONITOR.get())
                .thenScreenshot(SETTLE, "mc-dos-amber")
                .thenAssert(0, () -> glassIs(ctx, MonitorClientTests::amber), "every pixel of the glass is amber");
    }

    @ClientTest(timeoutTicks = 3600)
    public static void tube_showsMcDosInSixteenColoursOnACga(final ClientTestContext ctx) {
        final Set<Integer> sixteen = new HashSet<>();
        for (final int colour : Tube.sixteenColours()) {
            sixteen.add(colour);
        }
        openVintagePrompt(ctx, ComputingModule.CGA_MONITOR.get())
                .thenScreenshot(SETTLE, "mc-dos-cga")
                .thenAssert(0, () -> glassIs(ctx, rgb -> sixteen.contains(rgb)),
                        "every pixel of the glass is one of the sixteen colours");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void panel_ofFlatMonitorsShowsOneScreenWithOneButton(final ClientTestContext ctx) {
        final BlockPos[] wall = new BlockPos[6];
        ctx.thenBuild(0, world -> {
                    for (int v = 0; v < 2; v++) {
                        for (int u = 0; u < 3; u++) {
                            wall[v * 3 + u] = WALL.east(u).above(v);
                            world.placeMonitor(wall[v * 3 + u], Direction.NORTH,
                                    ComputingModule.TRANSITION_MONITOR.get());
                        }
                    }
                    world.placeRunningPersonalComputer(WALL.north());
                })
                .thenTeleport(SETTLE, WALL_VIEW, Direction.NORTH)
                .thenWaitUntil(() -> {
                    for (final BlockPos member : wall) {
                        if (!lit(ctx, member)) {
                            return false;
                        }
                    }
                    return true;
                }, BOOT_WAIT, "the whole screen to light")
                .thenScreenshot(40, "panel-3x2")
                .thenAssert(0, () -> {
                    int buttons = 0;
                    for (final BlockPos member : wall) {
                        if (ctx.mc().level.getBlockState(ctx.abs(member)).getValue(MonitorBlock.BUTTON)) {
                            buttons++;
                        }
                    }
                    return buttons == 1;
                }, "only the corner monitor shows its power button")
                .thenAssert(0, () -> {
                    final MonitorPanel panel = clientMonitor(ctx, WALL).panel();
                    return panel != null && panel.width() == 3 && panel.height() == 2;
                }, "the player's game sees one screen three by two");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void powerStrip_shutsTheMachineDownFromItsOpenedScreen(final ClientTestContext ctx) {
        final PersonalComputerBlockEntity[] pc = new PersonalComputerBlockEntity[1];
        final int[] power = new int[2];
        ctx.thenBuild(0, world -> {
                    pc[0] = world.placeRunningPersonalComputer(COMPUTER);
                    world.placeMonitor(SCREEN, Direction.WEST);
                })
                .thenTeleport(SETTLE, AT_SCREEN, Direction.WEST)
                .thenWaitUntilServer(level -> !pc[0].needsPost() && !pc[0].booting(), BOOT_WAIT,
                        "the machine to come up", level -> "still starting")
                .thenRightClick(SETTLE, SCREEN)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .then(SETTLE, () -> {
                    final MonitorFrameStyle.Geometry frame = ctx.screen(DesktopScreen.class).frameBounds();
                    power[0] = PowerStripLayout.stripX(frame.x()) + PowerStripLayout.buttonX()
                            + PowerStripLayout.BUTTON / 2;
                    power[1] = PowerStripLayout.stripY(frame.y()) + PowerStripLayout.POWER_Y
                            + PowerStripLayout.BUTTON / 2;
                    ctx.pointAt(power[0], power[1]);
                })
                .thenScreenshot(SETTLE, "power-strip-tooltip")
                .then(0, () -> {
                    // The way the game hands a click to a screen: the strip is asked before the screen is.
                    final DesktopScreen screen = ctx.screen(DesktopScreen.class);
                    if (!ClientHooks.onScreenMouseClickedPre(screen, power[0], power[1], 0)) {
                        screen.mouseClicked(power[0], power[1], 0);
                    }
                })
                .thenWaitUntilServer(level -> pc[0].goingDown() || !pc[0].isRunning(), 200,
                        "the Power button to shut the machine down", level -> "the machine is still up");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void face_showsTheMachineStartingByTheScreensAPlayerAtItSees(final ClientTestContext ctx) {
        final Set<Class<?>> seen = new HashSet<>();
        ctx.thenBuild(0, world -> {
                    world.placeRunningPersonalComputer(COMPUTER);
                    world.placeMonitor(SCREEN, Direction.WEST);
                })
                .thenTeleport(0, AT_SCREEN, Direction.WEST)
                // Every tick, the screen the face is drawn by, until the machine is at its desktop.
                .thenWaitUntil(() -> {
                    final AbstractComputerScreen<?> face = SessionFaces.screenOf(ctx.abs(SCREEN));
                    if (face != null) {
                        seen.add(face.getClass());
                    }
                    return MonitorPictureCache.of(ctx.abs(SCREEN)) instanceof IMonitorPicture.Desktop;
                }, BOOT_WAIT, "the machine to reach its desktop")
                .then(0, () -> ctx.assertTrue(seen.contains(BootSequenceScreen.class),
                        "the face showed the self-test by the very screen a player at the machine sees; saw " + seen))
                .then(0, () -> ctx.assertTrue(seen.contains(SystemBootScreen.class),
                        "and the system starting the same way; saw " + seen));
    }

    @ClientTest(timeoutTicks = 3000)
    public static void face_drawsTheProgramOpenAtTheMachine(final ClientTestContext ctx) {
        final IDesktopApp[] opened = new IDesktopApp[1];
        ctx.thenBuild(0, world -> {
                    final PersonalComputerBlockEntity pc = world.placeRunningPersonalComputer(COMPUTER);
                    pc.console().install(Programs.MINESWEEPER.toString());
                    world.placeMonitor(SCREEN, Direction.WEST);
                })
                .thenTeleport(SETTLE, AT_SCREEN, Direction.WEST)
                .thenWaitUntil(() -> MonitorPictureCache.of(ctx.abs(SCREEN)) instanceof IMonitorPicture.Desktop,
                        BOOT_WAIT, "the machine to come up")
                .thenRightClick(SETTLE, SCREEN)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> ctx.screen(DesktopScreen.class).launcherLabels().contains(MINESWEEPER),
                        LIGHT_WAIT, "the desktop to list Minesweeper")
                .then(SETTLE, () -> DesktopScreen.requestOpen(MINESWEEPER))
                .thenWaitUntil(() -> {
                    opened[0] = openProgram(ctx, MINESWEEPER);
                    return opened[0] != null;
                }, LIGHT_WAIT, "Minesweeper to open")
                .thenWaitUntil(() -> faceDraws(ctx, opened[0]), LIGHT_WAIT,
                        "the face to draw the window by the program open at the machine")
                .then(0, () -> ctx.player().closeContainer())
                .thenAwaitNoScreen(LIGHT_WAIT)
                .thenWaitUntil(() -> faceDraws(ctx, opened[0]), LIGHT_WAIT,
                        "and by the same program once the player has stepped away from it")
                .thenScreenshot(SETTLE, "face-with-a-program");
    }

    /**
     * The face shows the desktop the machine keeps: a folder made on it and a program pinned to its panel, with nobody
     * at the machine; and a terminal left open there is drawn inside its own window, its last lines at the foot.
     */
    @ClientTest(timeoutTicks = 3600)
    public static void face_showsTheDesktopsFoldersPinsAndATerminalInItsWindow(final ClientTestContext ctx) {
        final IDesktopApp[] terminal = new IDesktopApp[1];
        final String[] terminalName = new String[1];
        ctx.thenBuild(0, world -> {
                    final PersonalComputerBlockEntity pc = world.placeRunningPersonalComputer(COMPUTER);
                    String built = "";
                    for (final String segment : FOLDER.split("/")) {
                        built = built.isEmpty() ? segment : built + "/" + segment;
                        DiskFilesystem.mkdir(pc.systemDisk(), built, FilesystemKind.HIERARCHICAL);
                    }
                    pc.console().settings().pin(CALCULATOR_KEY);
                    world.placeMonitor(SCREEN, Direction.WEST);
                })
                .thenTeleport(SETTLE, AT_SCREEN, Direction.WEST)
                .thenWaitUntil(() -> MonitorPictureCache.of(ctx.abs(SCREEN)) instanceof IMonitorPicture.Desktop,
                        BOOT_WAIT, "the machine to come up")
                .thenWaitUntil(() -> {
                    final OffscreenDesktop face = MonitorPainter.desktopOf(ctx.abs(SCREEN));
                    return face != null && face.desktopItemNames().contains("Projects")
                            && face.pinnedLabels().contains("Calculator");
                }, LIGHT_WAIT, "the face to show the folder on the desktop and the program pinned to the panel", () -> {
                    final OffscreenDesktop face = MonitorPainter.desktopOf(ctx.abs(SCREEN));
                    return face == null ? "no face"
                            : "items=" + face.desktopItemNames() + " pins=" + face.pinnedLabels();
                })
                .thenRightClick(SETTLE, SCREEN)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .then(SETTLE, () -> {
                    terminalName[0] = DesktopScreen.terminalName();
                    DesktopScreen.requestOpen(terminalName[0]);
                })
                .thenWaitUntil(() -> {
                    terminal[0] = openProgram(ctx, terminalName[0]);
                    return terminal[0] != null;
                }, LIGHT_WAIT, "the terminal to open")
                // More lines than its window shows, so the ones scrolled above it are there to be wrongly shown.
                .then(SETTLE, () -> {
                    for (int i = 0; i < 24; i++) {
                        ctx.type("echo line " + i);
                        ctx.key(GLFW.GLFW_KEY_ENTER);
                    }
                })
                .then(LIGHT_WAIT / 4, () -> ctx.player().closeContainer())
                .thenAwaitNoScreen(LIGHT_WAIT)
                .thenWaitUntil(() -> faceDraws(ctx, terminal[0]), LIGHT_WAIT,
                        "the face to draw the terminal by the program left open at the machine")
                // A step back, looking down at the glass, to see the window and what is drawn in it.
                .thenTeleport(SETTLE, FACE_VIEW, Direction.WEST)
                .then(2, () -> ctx.player().setXRot(FACE_VIEW_PITCH))
                .thenScreenshot(SETTLE * 10, "face-terminal-folder-pin");
    }

    /** Looked at, the power button is outlined on its own, where a click presses it, and not the whole monitor. */
    @ClientTest(timeoutTicks = 2400)
    public static void powerButton_isOutlinedOnItsOwnWhileLookedAt(final ClientTestContext ctx) {
        ctx.thenBuild(0, world -> {
                    world.placeRunningPersonalComputer(COMPUTER);
                    world.placeMonitor(SCREEN, Direction.WEST);
                })
                .thenTeleport(SETTLE, AT_SCREEN, Direction.WEST)
                .then(SETTLE, () -> lookAt(ctx, buttonMiddle(ctx)))
                .thenWaitUntil(() -> {
                    if (!(ctx.mc().hitResult instanceof BlockHitResult hit)
                            || !hit.getBlockPos().equals(ctx.abs(SCREEN))) {
                        return false;
                    }
                    final BlockState state = ctx.mc().level.getBlockState(hit.getBlockPos());
                    final double[] at = MonitorBlock.frontPoint(state, hit.getBlockPos(), hit);
                    return at != null && ((MonitorBlock) state.getBlock()).kind().onButton(at[0], at[1]);
                }, LIGHT_WAIT, "the crosshair to rest on the power button")
                .thenScreenshot(SETTLE, "power-button-outline");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void face_showsTheInstallerOnThePageThePlayerIsOn(final ClientTestContext ctx) {
        ctx.thenBuild(0, world -> {
                    world.setBlock(COMPUTER, ComputingModule.MAINFRAME.get());
                    final ItemStackHandler inventory = world.blockEntity(COMPUTER, MainframeBlockEntity.class)
                            .getInventory();
                    inventory.setStackInSlot(MainframeBlockEntity.MOTHERBOARD_SLOT,
                            new ItemStack(ComputingModule.MOTHERBOARD_MTX_S_2011.get()));
                    inventory.setStackInSlot(MainframeBlockEntity.CPU_SLOTS_START,
                            new ItemStack(ComputingModule.CPU_SERVO_2620.get()));
                    inventory.setStackInSlot(MainframeBlockEntity.RAM_SLOTS_START,
                            new ItemStack(ComputingModule.RAM_DDR3_8192.get()));
                    inventory.setStackInSlot(MainframeBlockEntity.PSU_SLOT,
                            new ItemStack(ComputingModule.PSU_650G.get()));
                    inventory.setStackInSlot(MainframeBlockEntity.GPU_SLOTS_START,
                            new ItemStack(ComputingModule.GPU_HD_7970.get()));
                    inventory.setStackInSlot(MainframeBlockEntity.DISK_SLOTS_START,
                            new ItemStack(ComputingModule.disk(StorageTier.HDD, DiskSize.GB_500)));
                    world.blockEntity(COMPUTER, MainframeBlockEntity.class).togglePower();
                    world.setBlock(DRIVE, ComputingModule.CD_DRIVE.get());
                    final ItemStack disc = new ItemStack(ComputingModule.CD_ROM.get());
                    MediaItem.setKind(disc, MediaKind.OS_INSTALL);
                    MediaItem.setPayload(disc, FREEBSD);
                    world.blockEntity(DRIVE, MediaReaderBlockEntity.class).mediaSlot().setStackInSlot(0, disc);
                    world.placeMonitor(SCREEN, Direction.WEST);
                })
                .thenServer(SETTLE * 3, level -> {
                    final MainframeBlockEntity mainframe = TestWorldBuilder.at(level, ctx.origin())
                            .blockEntity(COMPUTER, MainframeBlockEntity.class);
                    mainframe.setNeedsPost(false);
                    FirmwarePayloads.beginInstall(level, mainframe, -1L, -1);
                })
                .thenTeleport(SETTLE, AT_SCREEN, Direction.WEST)
                .thenWaitUntil(() -> facePage(ctx, "WELCOME"), LIGHT_WAIT,
                        "the face to show the installer's welcome by the installer's own screen")
                .thenScreenshot(SETTLE, "face-installer")
                .thenRightClick(SETTLE, SCREEN)
                .thenAwaitScreen(InstallerScreen.class, LIGHT_WAIT)
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> "NAME".equals(ctx.screen(InstallerScreen.class).pageName()), LIGHT_WAIT,
                        "Enter to carry the installer on to its next page")
                .thenWaitUntil(() -> facePage(ctx, "NAME"), LIGHT_WAIT, "and the face to follow it there");
    }

    /*
     * The middle of the power button of the monitor at SCREEN, in the world: its front looks east, so across its
     * front runs toward the north, and the button stands on the face's plane.
     */
    private static Vec3 buttonMiddle(final ClientTestContext ctx) {
        final BlockPos at = ctx.abs(SCREEN);
        final MonitorKind kind = ((MonitorBlock) ctx.mc().level.getBlockState(at).getBlock()).kind();
        final double across = (kind.button(0) + kind.button(2)) / 2.0 / MonitorKind.FRONT;
        final double fromTop = (kind.button(1) + kind.button(3)) / 2.0 / MonitorKind.FRONT;
        return new Vec3(at.getX() + 1.0 + BUTTON_PROUD, at.getY() + 1.0 - fromTop, at.getZ() + 1.0 - across);
    }

    /* Turns the player's head to look at a point. */
    private static void lookAt(final ClientTestContext ctx, final Vec3 point) {
        final Vec3 eye = ctx.player().getEyePosition();
        final double dx = point.x - eye.x;
        final double dy = point.y - eye.y;
        final double dz = point.z - eye.z;
        ctx.player().setYRot((float) Math.toDegrees(Math.atan2(-dx, dz)));
        ctx.player().setXRot((float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
    }

    /* Whether the face of the monitor is drawn by the installer's screen, on the page named {@code page}. */
    private static boolean facePage(final ClientTestContext ctx, final String page) {
        return SessionFaces.screenOf(ctx.abs(SCREEN)) instanceof InstallerScreen face && page.equals(face.pageName());
    }

    /* The program drawing the window titled {@code title} on the open desktop, or null. */
    @Nullable
    private static IDesktopApp openProgram(final ClientTestContext ctx, final String title) {
        final DesktopScreen desktop = ctx.screen(DesktopScreen.class);
        final DesktopWindow window = desktop == null ? null : desktop.windowFor(title);
        return window == null ? null : window.app();
    }

    /* Whether the face of the monitor draws a window by that very program. */
    private static boolean faceDraws(final ClientTestContext ctx, final IDesktopApp program) {
        final OffscreenDesktop face = MonitorPainter.desktopOf(ctx.abs(SCREEN));
        if (face == null) {
            return false;
        }
        for (final IDesktopApp drawn : face.windowPrograms()) {
            if (drawn == program) {
                return true;
            }
        }
        return false;
    }

    /* A Vintage machine at its MC-DOS prompt on that monitor, the prompt opened. */
    private static ClientTestContext openVintagePrompt(final ClientTestContext ctx, final Block monitor) {
        final PersonalComputerBlockEntity[] pc = new PersonalComputerBlockEntity[1];
        return ctx.thenBuild(0, world -> {
                    world.setBlock(COMPUTER, ComputingModule.VINTAGE_PERSONAL_COMPUTER.get());
                    pc[0] = world.blockEntity(COMPUTER, PersonalComputerBlockEntity.class);
                    buildVintage(pc[0]);
                    pc[0].installOs(MC_DOS);
                    pc[0].togglePower();
                    world.placeMonitor(SCREEN, Direction.WEST, monitor);
                })
                .thenTeleport(SETTLE, AT_SCREEN, Direction.WEST)
                .thenWaitUntilServer(level -> !pc[0].needsPost() && !pc[0].booting(), BOOT_WAIT,
                        "the machine to come up", level -> "still starting")
                .thenRightClick(SETTLE, SCREEN)
                .thenAwaitScreen(CommandPromptScreen.class, BOOT_WAIT);
    }

    /* Whether every pixel on a grid over the open screen's glass passes the test. */
    private static boolean glassIs(final ClientTestContext ctx, final IntPredicate test) {
        if (!(ctx.mc().screen instanceof AbstractContainerScreen<?> screen)) {
            return false;
        }
        final int columns = 12;
        final int rows = 8;
        for (int c = 1; c < columns; c++) {
            for (int r = 1; r < rows; r++) {
                final int x = screen.getGuiLeft() + screen.getXSize() * c / columns;
                final int y = screen.getGuiTop() + screen.getYSize() * r / rows;
                final int rgb = ctx.pixel(x, y) & 0xFFFFFF;
                if (!test.test(rgb)) {
                    return false;
                }
            }
        }
        return true;
    }

    /* Red over green over blue, a hair of rounding allowed: the amber phosphor at any brightness. */
    private static boolean amber(final int rgb) {
        final int r = (rgb >> 16) & 0xFF;
        final int g = (rgb >> 8) & 0xFF;
        final int b = rgb & 0xFF;
        return r + 2 >= g && g + 2 >= b;
    }

    private static boolean lit(final ClientTestContext ctx, final BlockPos at) {
        return ctx.mc().level != null && ctx.mc().level.getBlockState(ctx.abs(at)).getBlock() instanceof MonitorBlock
                && ctx.mc().level.getBlockState(ctx.abs(at)).getValue(MonitorBlock.LIT);
    }

    private static MonitorBlockEntity clientMonitor(final ClientTestContext ctx, final BlockPos at) {
        return (MonitorBlockEntity) ctx.mc().level.getBlockEntity(ctx.abs(at));
    }

    private static Block[] kinds() {
        return new Block[] {ComputingModule.MONO_I_MONITOR.get(), ComputingModule.VINTAGE_MONITOR.get(),
            ComputingModule.AMBER_MONITOR.get(), ComputingModule.CGA_MONITOR.get(),
            ComputingModule.LEGACY_MONITOR.get(), ComputingModule.TRANSITION_MONITOR.get(),
            ComputingModule.MONITOR.get(), ComputingModule.COLOR_MONITOR.get()};
    }

    private static void buildVintage(final PersonalComputerBlockEntity computer) {
        final ItemStackHandler hardware = computer.getHardware();
        hardware.setStackInSlot(PersonalComputerBlockEntity.MOTHERBOARD_SLOT,
                new ItemStack(HardwareItems.MOTHERBOARD_BABYAT_VINTAGE.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.CPU_SLOT,
                new ItemStack(HardwareItems.CPU_INTEGRA_486SX.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.RAM_SLOTS_START,
                new ItemStack(HardwareItems.RAM_SIMM_4.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.PSU_SLOT, new ItemStack(HardwareItems.PSU_300.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.GPU_SLOTS_START,
                new ItemStack(HardwareItems.GPU_VGA_256.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.DISK_SLOTS_START,
                new ItemStack(ComputingModule.disk(StorageTier.HDD, DiskSize.GB_500)));
    }
}
