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
import dev.jstech.computers.QuinceItems;
import dev.jstech.computers.blockentity.ComputerHardwareLayout;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.client.BootSequenceScreen;
import dev.jstech.computers.client.CrashScreen;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.DesktopWindow;
import dev.jstech.computers.client.os.FilesApp;
import dev.jstech.computers.hardware.MotherboardSpec;
import dev.jstech.computers.operation.payload.firmware.FirmwarePayloads;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.install.InstallerPage;
import dev.jstech.computers.os.media.InstallerStacks;
import dev.jstech.computers.os.media.MediaReaderBlockEntity;
import dev.jstech.computers.os.crash.CrashCause;
import dev.jstech.computers.os.crash.CrashPages;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.computers.EraParts;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * Quince's systems on its own computers (computers/docs/design/quince.md), each as its mock draws it: the Quince System
 * on the Russet 128K, Quince OS 9 on the qRusset G3, Quince OS X in Pulp on the white qRusset and gone flat on the slim
 * one, and quinceOS on the coloured qRusset, each machine showing its desktop on its own glass with the startup disk's
 * window open.
 */
public final class QuinceClientTests {

    private static final int SETTLE = 4;
    private static final int BOOT_WAIT = 1_600;
    private static final BlockPos MACHINE = new BlockPos(3, 2, 3);
    private static final BlockPos PLAYER = MACHINE.north(2);
    /* A point every Quince all-in-one has glass at, in sixty-fourths of its front: the middle of the 128K's. */
    private static final int ON_THE_GLASS_X = 32;
    private static final int ON_THE_GLASS_Y = 22;
    private static final int WINDOW_WAIT = 200;
    private static final String FILES = "jsc:files";
    /* The startup disk's root, as its icon opens it. */
    private static final String ROOT = "";
    private static final String TEXTPAD = "jsc:editor";
    private static final String CALCULATOR = "jsc:calculator";
    private static final String QTUNES = "jsc:qtunes";
    private static final String EXPEDITION = "jsc:expedition";
    private static final String PREVIEW = "jsc:preview";
    private static final String MEDIA_PLAYER = "jsc:media_player";
    private static final String STORE = "jsc:quince_store";
    /* The network behind the machine: the Mainframe running the Mirror, a router, and the cables to its back. */
    private static final BlockPos STORE_ROUTER = MACHINE.south(2);
    private static final BlockPos STORE_MAINFRAME = MACHINE.south(4);
    /* The floppy drive behind the Russet, and how long the first floppy's copy may take. */
    private static final BlockPos FLOPPY_DRIVE = MACHINE.south();
    private static final int COPY_WAIT = 4800;
    /* The Quince Mainframe and the monitor beside it. */
    private static final BlockPos QUINCE_MAINFRAME = new BlockPos(3, 2, 2);
    private static final BlockPos SERVER_MONITOR = QUINCE_MAINFRAME.east();
    private static final String SERVER_ADMIN = "jsc:server_admin";
    private static final String FORCE_QUIT = "quince_memory";
    /* What was running when a system went down, and the part pulled from the Russet 128K. */
    private static final String EXPEDITION_NAME = "Expedition";
    private static final int EXPEDITION_PID = 12;
    private static final String PULLED_PART = "memory";
    private static final String THIS_COMPUTER = "jsc:this_pc";
    /* The bar's titles: the fruit's menu first, then the File menu on the classic systems. */
    private static final int FRUIT = 0;
    private static final int FILE_MENU = 1;
    private static final String ABOUT = "About This Computer";
    private static final String GET_INFO = "Get Info";
    /* How far down a window's title bar a click lands. */
    private static final int TITLE_DY = 5;
    /* The startup disk's name, a folder on it, and what the Seeker keeps out of sight at its root. */
    private static final String DISK_NAME = "Quince HD";
    private static final String APPLICATIONS = "Applications";
    private static final Set<String> OUT_OF_SIGHT = Set.of("bin", "etc", "tmp", "usr", "Volumes", "Desktop Folder");

    private QuinceClientTests() {
    }

    @ClientTest(timeoutTicks = 2400)
    public static void quinceSystem_showsItsDesktop(final ClientTestContext ctx) {
        atDesktop(ctx, ComputingModule.RUSSET_128K.get(), QuinceItems.BOARD_RUSSET_128K.get(), "quince_system")
                .thenWaitUntil(() -> seekerOnTheDisk(ctx), WINDOW_WAIT, "the Seeker on Quince HD")
                .thenScreenshot(SETTLE, "qs-desktop");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void quinceOs9_showsItsDesktop(final ClientTestContext ctx) {
        atDesktop(ctx, ComputingModule.QRUSSET_G3.getFirst().get(), QuinceItems.BOARD_QRUSSET_G3.get(), "quince_os_9")
                .thenWaitUntil(() -> seekerOnTheDisk(ctx), WINDOW_WAIT, "the Seeker on Quince HD")
                // Its figures count every icon it shows, the stored items' folder beside the disk's own.
                .thenAssert(SETTLE, () -> seeker(ctx).counted() == seeker(ctx).names().size() + 1,
                        "the Seeker's figures count the disk's folders and the stored items' folder")
                .thenScreenshot(SETTLE, "os9-desktop");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void quinceOsX_showsItsDesktopInPulp(final ClientTestContext ctx) {
        atDesktop(ctx, ComputingModule.QRUSSET_WHITE.get(), QuinceItems.BOARD_QRUSSET_WHITE.get(), "quince_os_x")
                .thenWaitUntil(() -> seekerOnTheDisk(ctx), WINDOW_WAIT, "the Seeker on Quince HD")
                .thenScreenshot(SETTLE, "osx-desktop");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void quinceOsX_showsItsDesktopGoneFlat(final ClientTestContext ctx) {
        atDesktop(ctx, ComputingModule.QRUSSET_SLIM.get(), QuinceItems.BOARD_QRUSSET_THIN.get(), "quince_os_x")
                .thenWaitUntil(() -> seekerOnTheDisk(ctx), WINDOW_WAIT, "the Seeker on Quince HD")
                .thenScreenshot(SETTLE, "flat-desktop");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void quinceOs_showsItsDesktop(final ClientTestContext ctx) {
        atDesktop(ctx, ComputingModule.QRUSSET_COLOURS.getFirst().get(), QuinceItems.BOARD_QRUSSET_COLOUR.get(),
                "quinceos")
                .thenWaitUntil(() -> seekerOnTheDisk(ctx), WINDOW_WAIT, "the Seeker on Quince HD")
                .thenScreenshot(SETTLE, "qos-desktop");
    }

    /* The Seeker opens a folder in its own window, titles it with the folder's name and lights it in its sidebar. */
    @ClientTest(timeoutTicks = 2400)
    public static void quinceOsX_seekerTitlesItsWindowWithTheFolder(final ClientTestContext ctx) {
        atDesktop(ctx, ComputingModule.QRUSSET_WHITE.get(), QuinceItems.BOARD_QRUSSET_WHITE.get(), "quince_os_x")
                .thenWaitUntil(() -> seekerOnTheDisk(ctx), WINDOW_WAIT, "the Seeker on Quince HD")
                .thenAssert(SETTLE, () -> seeker(ctx).openNamed(APPLICATIONS), "Applications is on the disk")
                .thenWaitUntil(() -> APPLICATIONS.equals(seeker(ctx).title()) && !seeker(ctx).names().isEmpty(),
                        WINDOW_WAIT, "the Seeker on Applications")
                .thenScreenshot(SETTLE, "osx-seeker-applications");
    }

    /*
     * About This Computer with TextPad and the Calculator open, each in its block of memory, then TextPad's Get Info
     * with its memory size.
     */
    @ClientTest(timeoutTicks = 3000)
    public static void quinceSystem_showsItsMemoryInAboutAndGetInfo(final ClientTestContext ctx) {
        memory(atDesktop(ctx, ComputingModule.RUSSET_128K.get(), QuinceItems.BOARD_RUSSET_128K.get(),
                "quince_system"), ctx, CALCULATOR, TEXTPAD, "qs");
    }

    /* The same on Quince OS 9, with qTunes and Expedition open, then Expedition's Get Info. */
    @ClientTest(timeoutTicks = 3000)
    public static void quinceOs9_showsItsMemoryInAboutAndGetInfo(final ClientTestContext ctx) {
        memory(atDesktop(ctx, ComputingModule.QRUSSET_G3.getFirst().get(), QuinceItems.BOARD_QRUSSET_G3.get(),
                "quince_os_9"), ctx, QTUNES, EXPEDITION, "os9");
    }

    /* The Quince System's bomb, a part pulled from the running machine, waiting for Restart. */
    @ClientTest(timeoutTicks = 3000)
    public static void quinceSystem_bombsAndWaitsForRestart(final ClientTestContext ctx) {
        crashed(atDesktop(ctx, ComputingModule.RUSSET_128K.get(), QuinceItems.BOARD_RUSSET_128K.get(),
                "quince_system"), ctx, CrashCause.HARDWARE, CrashPages.Moment.hardware(PULLED_PART), "qs-bomb");
    }

    /* Quince OS 9's bomb, naming Expedition, which was running when it went down. */
    @ClientTest(timeoutTicks = 3000)
    public static void quinceOs9_bombsNamingTheProgram(final ClientTestContext ctx) {
        final ClientTestContext steps = atDesktop(ctx, ComputingModule.QRUSSET_G3.getFirst().get(),
                QuinceItems.BOARD_QRUSSET_G3.get(), "quince_os_9");
        opened(steps, ctx, EXPEDITION);
        crashed(steps, ctx, CrashCause.MEMORY, CrashPages.Moment.memory(EXPEDITION_NAME, EXPEDITION_PID, List.of()),
                "os9-bomb");
    }

    /* Quince OS X falls to the grey page in its four languages. */
    @ClientTest(timeoutTicks = 3000)
    public static void quinceOsX_fallsToTheGreyPage(final ClientTestContext ctx) {
        crashed(atDesktop(ctx, ComputingModule.QRUSSET_WHITE.get(), QuinceItems.BOARD_QRUSSET_WHITE.get(),
                "quince_os_x"), ctx, CrashCause.MEMORY, CrashPages.Moment.memory(EXPEDITION_NAME, EXPEDITION_PID,
                List.of()), "osx-restart");
    }

    /*
     * Quince OS X out of application memory with Expedition, qTunes, Preview and TextPad open: it ends nothing and asks
     * in Force Quit Applications what to quit, the programs paused until the player chooses.
     */
    @ClientTest(timeoutTicks = 3000)
    public static void quinceOsX_asksWhatToForceQuit(final ClientTestContext ctx) {
        final ClientTestContext steps = atDesktop(ctx, ComputingModule.QRUSSET_WHITE.get(),
                QuinceItems.BOARD_QRUSSET_WHITE.get(), "quince_os_x");
        for (final String program : List.of(TEXTPAD, PREVIEW, QTUNES, EXPEDITION)) {
            opened(steps, ctx, program);
        }
        steps.thenServer(SETTLE, level -> {
                    final PersonalComputerBlockEntity machine = machine(ctx, level);
                    ctx.assertTrue(!machine.memoryShort(EXPEDITION_NAME, machine.ramLedger().freeMb() + 1),
                            "a program asking for more than is free gets no room");
                })
                .thenWaitUntil(() -> desktop(ctx).windowFor(FORCE_QUIT) != null, WINDOW_WAIT,
                        "Force Quit Applications")
                .thenScreenshot(SETTLE * 4, "osx-memory");
    }

    /* qTunes on Quince OS X, with the songs the machine has. */
    @ClientTest(timeoutTicks = 3000)
    public static void quinceOsX_showsQtunes(final ClientTestContext ctx) {
        final ClientTestContext steps = atDesktop(ctx, ComputingModule.QRUSSET_WHITE.get(),
                QuinceItems.BOARD_QRUSSET_WHITE.get(), "quince_os_x");
        opened(steps, ctx, QTUNES);
        steps.thenScreenshot(SETTLE * 10, "osx-qtunes");
    }

    /* Preview and the QuinceTime Player on Quince OS X, side by side. */
    @ClientTest(timeoutTicks = 3000)
    public static void quinceOsX_showsPreviewAndTheQuinceTimePlayer(final ClientTestContext ctx) {
        final ClientTestContext steps = atDesktop(ctx, ComputingModule.QRUSSET_WHITE.get(),
                QuinceItems.BOARD_QRUSSET_WHITE.get(), "quince_os_x");
        opened(steps, ctx, PREVIEW);
        opened(steps, ctx, MEDIA_PLAYER);
        steps.thenScreenshot(SETTLE * 10, "osx-media");
    }

    /*
     * The Quince Store on Quince OS X gone flat, on a network whose Mainframe runs the Mirror: the programs the mod
     * has, each to GET, the ones installed marked so.
     */
    @ClientTest(timeoutTicks = 3000)
    public static void flat_showsTheQuinceStoreFromTheMirror(final ClientTestContext ctx) {
        final ClientTestContext steps = atDesktop(ctx, ComputingModule.QRUSSET_SLIM.get(),
                QuinceItems.BOARD_QRUSSET_THIN.get(), "quince_os_x", world -> {
                    world.placeRunningMainframe(STORE_MAINFRAME).installMirror();
                    world.setBlock(STORE_MAINFRAME.north(), ComputingModule.HBW_CABLE);
                    world.setBlock(STORE_ROUTER, ComputingModule.PERSONAL_ROUTER.get());
                    world.setBlock(STORE_ROUTER.north(), ComputingModule.ETHERNET_CABLE);
                });
        opened(steps, ctx, STORE);
        steps.thenScreenshot(SETTLE * 20, "flat-store");
    }

    /*
     * The Quince System installs from its three floppies: once the first is copied the installer asks for the second,
     * naming it, and says the first has come out of the slot.
     */
    @ClientTest(timeoutTicks = 6000)
    public static void quinceSystem_asksForItsSecondFloppy(final ClientTestContext ctx) {
        final List<ItemStack> floppies = InstallerStacks.disksOf(OsRegistry.getOs(
                ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "quince_system")));
        ctx.thenBuild(0, world -> {
                    world.setBlock(MACHINE, ComputingModule.RUSSET_128K.get().defaultBlockState()
                            .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
                    build(world.blockEntity(MACHINE, PersonalComputerBlockEntity.class),
                            QuinceItems.BOARD_RUSSET_128K.get());
                    world.setBlock(FLOPPY_DRIVE, ComputingModule.FLOPPY_DRIVE.get().defaultBlockState()
                            .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
                    world.blockEntity(FLOPPY_DRIVE, MediaReaderBlockEntity.class).mediaSlot()
                            .setStackInSlot(0, floppies.getFirst().copy());
                    final PersonalComputerBlockEntity machine = world.blockEntity(MACHINE,
                            PersonalComputerBlockEntity.class);
                    machine.togglePower();
                })
                .thenWaitUntilServer(level -> ctx.abs(MACHINE).equals(((MediaReaderBlockEntity) level
                                .getBlockEntity(ctx.abs(FLOPPY_DRIVE))).ownerPos()), WINDOW_WAIT,
                        "the drive to link to the Russet", level -> "unlinked")
                .thenServer(SETTLE, level -> {
                    final PersonalComputerBlockEntity machine = machine(ctx, level);
                    ctx.assertTrue(FirmwarePayloads.installOsFromReader(level, machine, -1L, -1),
                            "the installer opens");
                    machine.installer().next();
                })
                .thenTeleport(SETTLE, PLAYER, Direction.SOUTH)
                .thenRightClickFront(SETTLE, MACHINE, ON_THE_GLASS_X, ON_THE_GLASS_Y)
                .thenWaitUntilServer(level -> machine(ctx, level).installer() != null
                                && machine(ctx, level).installer().page() == InstallerPage.INSERT, COPY_WAIT,
                        "the installer to ask for the second floppy", level -> "on " + (machine(ctx, level)
                                .installer() == null ? "nothing" : machine(ctx, level).installer().page()))
                .then(SETTLE * 4, () -> ctx.mc().getToasts().clear())
                .thenScreenshot(SETTLE * 2, "qs-insert");
    }

    /*
     * quinceOS's recovery: switched on sneaking, the machine stands at its start-up options, its last choice the Quince
     * chip's Options gear, which opens the recovery's utilities.
     */
    @ClientTest(timeoutTicks = 3600)
    public static void quinceOs_opensItsRecoveryFromTheStartupOptions(final ClientTestContext ctx) {
        ctx.thenBuild(0, world -> {
                    world.setBlock(MACHINE, ComputingModule.QRUSSET_COLOURS.getFirst().get().defaultBlockState()
                            .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
                    final PersonalComputerBlockEntity machine = world.blockEntity(MACHINE,
                            PersonalComputerBlockEntity.class);
                    build(machine, QuinceItems.BOARD_QRUSSET_COLOUR.get());
                    machine.installOs(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "quinceos"));
                    // Its power button pressed while sneaking, which asks for the start-up options.
                    machine.askStartupChoices(true);
                    machine.togglePower();
                })
                .thenTeleport(SETTLE, PLAYER, Direction.SOUTH)
                .thenRightClickFront(SETTLE, MACHINE, ON_THE_GLASS_X, ON_THE_GLASS_Y)
                .thenAwaitScreen(BootSequenceScreen.class, BOOT_WAIT)
                // The disk the machine starts from, then the gear after it.
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_RIGHT))
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .then(SETTLE * 4, () -> ctx.mc().getToasts().clear())
                .thenScreenshot(SETTLE * 2, "qos-recovery");
    }

    /*
     * Server Admin on Quince OS X Server, on the Quince Mainframe through its monitor: the machine's services, the
     * Mirror running among them.
     */
    @ClientTest(timeoutTicks = 3600)
    public static void server_showsServerAdmin(final ClientTestContext ctx) {
        final ClientTestContext steps = ctx.thenBuild(0, world -> {
                    world.setBlock(QUINCE_MAINFRAME, ComputingModule.QUINCE_MAINFRAME_STANDARD.get());
                    final MainframeBlockEntity mainframe = world.blockEntity(QUINCE_MAINFRAME,
                            MainframeBlockEntity.class);
                    final Item board = QuinceItems.BOARD_MAINFRAME_S.get();
                    final MotherboardSpec spec = EraParts.board(board);
                    final ItemStackHandler parts = mainframe.getInventory();
                    parts.setStackInSlot(MainframeBlockEntity.MOTHERBOARD_SLOT, new ItemStack(board));
                    parts.setStackInSlot(MainframeBlockEntity.CPU_SLOTS_START, new ItemStack(EraParts.cpuFor(board)));
                    parts.setStackInSlot(MainframeBlockEntity.RAM_SLOTS_START, new ItemStack(EraParts.ramFor(board)));
                    parts.setStackInSlot(MainframeBlockEntity.PSU_SLOT, new ItemStack(EraParts.psuFor(spec.era())));
                    parts.setStackInSlot(MainframeBlockEntity.DISK_SLOTS_START,
                            new ItemStack(EraParts.diskFor(spec.era())));
                    // A graphics card gives the machine the peripheral ports its monitor links to.
                    parts.setStackInSlot(MainframeBlockEntity.GPU_SLOTS_START,
                            new ItemStack(ComputingModule.GPU_HD_7970.get()));
                    if (!mainframe.installOs(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
                            "quince_os_x_server"))) {
                        throw new IllegalStateException("Quince OS X Server does not install on the Quince Mainframe");
                    }
                    mainframe.installMirror();
                    mainframe.togglePower();
                    world.placeMonitor(SERVER_MONITOR, Direction.EAST);
                })
                .thenTeleport(SETTLE, SERVER_MONITOR.east(2), Direction.WEST)
                .thenRightClick(SETTLE, SERVER_MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .then(SETTLE * 10, () -> ctx.mc().getToasts().clear());
        opened(steps, ctx, SERVER_ADMIN);
        steps.thenScreenshot(SETTLE * 10, "server-admin");
    }

    /*
     * Opens the two programs, the second in front, then About This Computer from the fruit's menu, and the Get Info of
     * the program in front from the File menu, each shot for its mock.
     */
    private static void memory(final ClientTestContext steps, final ClientTestContext ctx, final String first,
                               final String front, final String label) {
        steps.then(SETTLE, () -> DesktopScreen.requestOpen(first))
                .thenWaitUntil(() -> desktop(ctx).windowFor(first) != null, WINDOW_WAIT, first + " to open")
                .then(SETTLE, () -> DesktopScreen.requestOpen(front))
                .thenWaitUntil(() -> desktop(ctx).windowFor(front) != null, WINDOW_WAIT, front + " to open")
                .then(SETTLE, () -> ctx.clickDesktop(desktop(ctx).quinceTitlePoint(FRUIT)))
                .thenWaitUntil(() -> desktop(ctx).quinceItemPoint(ABOUT) != null, WINDOW_WAIT, "the fruit's menu")
                .then(SETTLE, () -> ctx.clickDesktop(desktop(ctx).quinceItemPoint(ABOUT)))
                .thenWaitUntil(() -> desktop(ctx).windowFor(THIS_COMPUTER) != null, WINDOW_WAIT,
                        "About This Computer")
                .thenScreenshot(SETTLE * 4, label + "-about")
                .then(SETTLE, () -> ctx.clickDesktop(desktop(ctx).windowFor(THIS_COMPUTER)
                        .buttonCentre(DesktopWindow.BUTTON_CLOSE)))
                .then(SETTLE, () -> ctx.clickDesktop(titleOf(desktop(ctx).windowFor(front))))
                .then(SETTLE, () -> ctx.clickDesktop(desktop(ctx).quinceTitlePoint(FILE_MENU)))
                .thenWaitUntil(() -> desktop(ctx).quinceItemPoint(GET_INFO) != null, WINDOW_WAIT, "the File menu")
                .then(SETTLE, () -> ctx.clickDesktop(desktop(ctx).quinceItemPoint(GET_INFO)))
                .thenScreenshot(SETTLE * 4, label + "-getinfo");
    }

    /* Opens {@code program} on the desktop and waits for its window. */
    private static void opened(final ClientTestContext steps, final ClientTestContext ctx, final String program) {
        steps.then(SETTLE, () -> DesktopScreen.requestOpen(program))
                .thenWaitUntil(() -> desktop(ctx).windowFor(program) != null, WINDOW_WAIT, program + " to open");
    }

    /* Brings the running machine down for that cause and shoots the page it falls to. */
    private static void crashed(final ClientTestContext steps, final ClientTestContext ctx, final CrashCause cause,
                                final CrashPages.Moment moment, final String label) {
        steps.thenServer(SETTLE, level -> ctx.assertTrue(machine(ctx, level).crash(cause, moment),
                        "the running system goes down"))
                .thenAwaitScreen(CrashScreen.class, WINDOW_WAIT)
                .thenAssert(SETTLE * 10, () -> ctx.mc().screen instanceof CrashScreen, "the page stands on the glass")
                .thenScreenshot(0, label);
    }

    private static PersonalComputerBlockEntity machine(final ClientTestContext ctx, final ServerLevel level) {
        return (PersonalComputerBlockEntity) level.getBlockEntity(ctx.abs(MACHINE));
    }

    private static DesktopScreen desktop(final ClientTestContext ctx) {
        return ctx.screen(DesktopScreen.class);
    }

    /* A point on a window's title bar, where a click brings it to the front. */
    private static int[] titleOf(final DesktopWindow window) {
        return new int[] {window.x() + window.width() / 2, window.y() + TITLE_DY};
    }

    /* The machine built on its board with that system, running, and its own glass open on the desktop. */
    private static ClientTestContext atDesktop(final ClientTestContext ctx, final Block block, final Item board,
                                               final String system) {
        return atDesktop(ctx, block, board, system, world -> { });
    }

    /* The same, with {@code around} built beside the machine in the same build. */
    private static ClientTestContext atDesktop(final ClientTestContext ctx, final Block block, final Item board,
                                               final String system, final Consumer<TestWorldBuilder> around) {
        return ctx.thenBuild(0, world -> {
                    around.accept(world);
                    world.setBlock(MACHINE, block.defaultBlockState()
                            .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
                    final PersonalComputerBlockEntity machine = world.blockEntity(MACHINE,
                            PersonalComputerBlockEntity.class);
                    build(machine, board);
                    if (!machine.installOs(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, system))) {
                        throw new IllegalStateException(system + " does not install on " + block);
                    }
                    machine.togglePower();
                })
                .thenTeleport(SETTLE, PLAYER, Direction.SOUTH)
                // On the glass, where a click opens the desktop; on the case it opens the machine's parts.
                .thenRightClickFront(SETTLE, MACHINE, ON_THE_GLASS_X, ON_THE_GLASS_Y)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .then(SETTLE * 4, () -> DesktopScreen.requestOpenFiles(ROOT))
                .thenWaitUntil(() -> ctx.screen(DesktopScreen.class).windowFor(FILES) != null, WINDOW_WAIT,
                        "the startup disk's window")
                .then(SETTLE * 10, () -> ctx.mc().getToasts().clear());
    }

    /* The startup disk's window as Quince names it, listing the disk with none of what its Seeker kept out of sight. */
    private static boolean seekerOnTheDisk(final ClientTestContext ctx) {
        final FilesApp seeker = seeker(ctx);
        return seeker != null && DISK_NAME.equals(seeker.title()) && !seeker.names().isEmpty()
                && seeker.names().stream().noneMatch(OUT_OF_SIGHT::contains);
    }

    /* The explorer in the startup disk's window, or null while there is none. */
    @Nullable
    private static FilesApp seeker(final ClientTestContext ctx) {
        final DesktopWindow window = ctx.screen(DesktopScreen.class).windowFor(FILES);
        return window != null && window.app() instanceof FilesApp files ? files : null;
    }

    /*
     * The parts in the slots the machine's own map has for them: the board, the strongest processor of its era, a
     * module of its memory where it takes one, its era's supply and disk.
     */
    private static void build(final PersonalComputerBlockEntity machine, final Item board) {
        final MotherboardSpec spec = EraParts.board(board);
        final ItemStackHandler hardware = machine.getHardware();
        final ComputerHardwareLayout map = machine.hardwareLayout();
        hardware.setStackInSlot(map.motherboardSlot(), new ItemStack(board));
        hardware.setStackInSlot(map.cpuSlot(0), new ItemStack(EraParts.cpuFor(board)));
        if (spec.ramSlots() > 0) {
            hardware.setStackInSlot(map.ramSlot(0), new ItemStack(EraParts.ramFor(board)));
        }
        hardware.setStackInSlot(map.psuSlot(), new ItemStack(EraParts.psuFor(spec.era())));
        hardware.setStackInSlot(map.diskStart(), new ItemStack(EraParts.diskFor(spec.era())));
    }
}
