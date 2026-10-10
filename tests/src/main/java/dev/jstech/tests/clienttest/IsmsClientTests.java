/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.AbstractSmallComputerBlockEntity;
import dev.jstech.computers.blockentity.ComputerHardwareLayout;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.DesktopWindow;
import dev.jstech.computers.client.os.IsmsApp;
import dev.jstech.computers.client.os.IsmsProfilerApp;
import dev.jstech.computers.machine.IqlTables;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.tests.testkit.CraftFiles;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * The IQL Server Management Studio as a player uses it: every menu of its bar opened, a script run a statement at a
 * time into tables with their real columns, Parse underlining a word, the question before a statement that cannot
 * be undone, a craft's estimated plan, the Object Explorer Details and the Activity Monitor, its Profiler recording
 * what the studio runs, a script saved to the computer's disk and opened again, and the look of each age.
 */
public final class IsmsClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 80;
    private static final int WORK_WAIT = 400;
    private static final int BOOT_WAIT = 1_200;
    private static final BlockPos COMPUTER = new BlockPos(5, 2, 3);
    private static final BlockPos COMPUTER_CABLE = new BlockPos(4, 2, 3);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 3);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 3);
    private static final String STUDIO = "jsc:isms";
    private static final String PROFILER = "jsc:isms/profiler";
    private static final List<String> MENUS = List.of("File", "Edit", "View", "Query", "Tools", "Window", "Help");
    /** One item of each menu, which the menu has to hold. */
    private static final List<String> ONE_OF_EACH = List.of("Open File...", "Find...", "Object Explorer Details",
            "Display Estimated Plan", "IQL Server Profiler", "Reset Window Layout", "IQL Reference");
    private static final ResourceLocation FRAMES_XP = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
            "frames_xp");
    private static final ResourceLocation FRAMES_11 = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
            "frames_11");
    private static final ResourceLocation FRAMES_7 = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
            "frames_7");

    private IsmsClientTests() {
    }

    /** The studio opens on the network with its explorer filled in, and every menu of its bar drops. */
    @ClientTest(timeoutTicks = 3000)
    public static void isms_opensWithItsExplorerAndEveryMenu(final ClientTestContext ctx) {
        ClientTestContext steps = openStudio(ctx, null)
                .then(0, () -> expect(ctx, studio(ctx).lookName().equals("STUDIO_2012"),
                        "a Standard computer's studio is the 2012 one; got " + studio(ctx).lookName()))
                .then(0, () -> expect(ctx, studio(ctx).explorerLabels().contains("Tables")
                                && studio(ctx).explorerLabels().stream().anyMatch(label -> label.startsWith("Storage")),
                        "the explorer lists the tables and the storage; got " + studio(ctx).explorerLabels()))
                .thenScreenshot(2, "isms-standard");
        for (int i = 0; i < MENUS.size(); i++) {
            final int menu = i;
            steps = steps.then(2, () -> openMenu(ctx, menu))
                    .then(2, () -> expect(ctx, studio(ctx).openMenuLabels().contains(ONE_OF_EACH.get(menu)),
                            "the " + MENUS.get(menu) + " menu drops; got " + studio(ctx).openMenuLabels()))
                    .thenScreenshot(1, "isms-menu-" + MENUS.get(menu).toLowerCase(Locale.ROOT))
                    .then(1, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE));
        }
        steps
                // A node's own menu: the items table offers its first rows.
                .then(2, () -> studio(ctx).revealNode("table/items"))
                .then(2, () -> ctx.rightClickDesktop(point(ctx, studio(ctx).nodeCenter("table/items"))))
                .then(2, () -> expect(ctx, studio(ctx).openMenuLabels().contains("Query Top 100 Rows"),
                        "the items table's menu; got " + studio(ctx).openMenuLabels()))
                .thenScreenshot(1, "isms-node-menu")
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .then(SETTLE, () -> leave(ctx))
                .thenAwaitNoScreen(SCREEN_WAIT);
    }

    /**
     * A script runs a statement at a time, each table with its real columns; Parse underlines what it cannot read;
     * a statement that cannot be undone is asked about first; results go to text on Ctrl+T.
     */
    @ClientTest(timeoutTicks = 3000)
    public static void isms_runsAScriptAStatementAtATime(final ClientTestContext ctx) {
        openStudio(ctx, null)
                .then(2, () -> {
                    studio(ctx).typeScript("-- what the network holds\nQUERY items;\nQUERY servers;\n");
                    ctx.key(GLFW.GLFW_KEY_F5);
                })
                .thenWaitUntil(() -> !studio(ctx).running() && studio(ctx).resultColumns().size() == 2, WORK_WAIT,
                        "both statements to come back, each with its table",
                        () -> "messages=" + studio(ctx).messageLines())
                .then(0, () -> expect(ctx, studio(ctx).resultColumns().get(0).equals(IqlTables.ITEMS)
                                && studio(ctx).resultColumns().get(1).equals(IqlTables.SERVERS),
                        "each table has its own columns; got " + studio(ctx).resultColumns()))
                .then(0, () -> expect(ctx, studio(ctx).statusText().equals("Query executed successfully"),
                        "the status bar says it went; got " + studio(ctx).statusText()))
                .thenScreenshot(2, "isms-results-grid")
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_T, GLFW.GLFW_MOD_CONTROL))
                .then(1, () -> expect(ctx, studio(ctx).resultsMode().equals("TEXT"),
                        "Ctrl+T sends results to text; got " + studio(ctx).resultsMode()))
                .thenScreenshot(1, "isms-results-text")
                .then(1, () -> {
                    studio(ctx).typeScript("QUERY items WHER qty < 5;");
                    ctx.key(GLFW.GLFW_KEY_F5, GLFW.GLFW_MOD_CONTROL);
                })
                .then(2, () -> expect(ctx, studio(ctx).markCount() == 1 && studio(ctx).resultsTab().equals("MESSAGES")
                                && studio(ctx).messageLines().stream().anyMatch(line -> line.startsWith("Line 1")),
                        "Parse underlines the word and says the line; got " + studio(ctx).messageLines()))
                .thenScreenshot(1, "isms-parse-error")
                .then(1, () -> {
                    studio(ctx).typeScript("DROP 1 cobblestone;");
                    ctx.key(GLFW.GLFW_KEY_F5);
                })
                .thenWaitUntil(() -> studio(ctx).dialogOpen(), SCREEN_WAIT,
                        "the question before a statement that cannot be undone")
                .then(0, () -> expect(ctx, String.join(" ", studio(ctx).dialogText()).contains("DROP 1 cobblestone"),
                        "it names the statement; got " + studio(ctx).dialogText()))
                .thenScreenshot(1, "isms-confirm")
                .then(1, () -> studio(ctx).pressDialog())
                .thenWaitUntilServer(level -> held(ctx, level, Items.COBBLESTONE) == 63L, WORK_WAIT,
                        "Yes to drop the one cobblestone", level -> "held=" + held(ctx, level, Items.COBBLESTONE))
                .then(SETTLE, () -> leave(ctx))
                .thenAwaitNoScreen(SCREEN_WAIT);
    }

    /** A craft's estimated plan, the Object Explorer Details of the tables and the Activity Monitor. */
    @ClientTest(timeoutTicks = 3000)
    public static void isms_showsAPlanTheDetailsAndTheActivity(final ClientTestContext ctx) {
        openStudio(ctx, net -> net.cc().loadPattern(CraftFiles.oakPlanks()))
                .then(2, () -> {
                    studio(ctx).typeScript("CRAFT 8 oak_planks;");
                    ctx.key(GLFW.GLFW_KEY_L, GLFW.GLFW_MOD_CONTROL);
                })
                .thenWaitUntil(() -> !studio(ctx).planLines().isEmpty(), WORK_WAIT, "the estimated plan")
                .then(0, () -> expect(ctx, studio(ctx).resultsTab().equals("PLAN")
                                && studio(ctx).planLines().get(0).startsWith("CRAFT 8"),
                        "the plan pane shows the craft first; got " + studio(ctx).planLines()))
                .thenScreenshot(2, "isms-plan")
                .then(1, () -> {
                    studio(ctx).selectNode("tables");
                    ctx.key(GLFW.GLFW_KEY_F7);
                })
                .then(2, () -> expect(ctx, studio(ctx).documentKind().equals("DETAILS")
                                && studio(ctx).detailRows().size() == IqlTables.TABLES.size()
                                && studio(ctx).detailColumns().contains("Last change"),
                        "F7 lists the tables with their rows and columns; got " + studio(ctx).detailRows()))
                .thenScreenshot(2, "isms-details")
                .then(1, () -> {
                    openMenu(ctx, 4);
                    ctx.clickDesktop(point(ctx, studio(ctx).menuItemCenter(1)));
                })
                .then(2, () -> expect(ctx, studio(ctx).documentKind().equals("ACTIVITY"),
                        "Tools opens the Activity Monitor; got " + studio(ctx).documentKind()))
                .thenScreenshot(SETTLE, "isms-activity")
                .then(SETTLE, () -> leave(ctx))
                .thenAwaitNoScreen(SCREEN_WAIT);
    }

    /** The Profiler, opened from Tools, records the statements the studio runs. */
    @ClientTest(timeoutTicks = 3000)
    public static void isms_profilerRecordsTheStudiosStatements(final ClientTestContext ctx) {
        openStudio(ctx, null)
                .then(2, () -> {
                    openMenu(ctx, 4);
                    ctx.clickDesktop(point(ctx, studio(ctx).menuItemCenter(0)));
                })
                .thenWaitUntil(() -> profiler(ctx) != null, SCREEN_WAIT, "the Profiler window")
                .then(2, () -> ctx.clickDesktop(profilerPoint(ctx, profiler(ctx).toolbarCenter("Start"))))
                .then(2, () -> expect(ctx, profiler(ctx).running(), "Start begins the trace"))
                .then(SETTLE, () -> {
                    studio(ctx).typeScript("QUERY items;");
                    studio(ctx).execute();
                })
                .thenWaitUntil(() -> profiler(ctx).rowTexts().stream().anyMatch(row -> row.startsWith(
                        "StatementCompleted")), WORK_WAIT, "the Profiler to record the statement",
                        () -> "rows=" + profiler(ctx).rowTexts())
                .then(1, () -> profiler(ctx).select(0))
                .then(1, () -> expect(ctx, profiler(ctx).detailLines().stream().anyMatch(line -> line.startsWith(
                        "asked by")), "the row's detail says who asked; got " + profiler(ctx).detailLines()))
                .thenScreenshot(2, "isms-profiler")
                .then(1, () -> profiler(ctx).stop())
                .then(SETTLE, () -> leave(ctx))
                .thenAwaitNoScreen(SCREEN_WAIT);
    }

    /** A script saved through the system's file window lands on the computer's disk, and opens again from there. */
    @ClientTest(timeoutTicks = 3000)
    public static void isms_savesAScriptToTheComputersDiskAndOpensIt(final ClientTestContext ctx) {
        final String[] path = {""};
        openStudio(ctx, null)
                .then(2, () -> {
                    studio(ctx).typeScript("QUERY items WHERE qty > 10;");
                    openMenu(ctx, 0);
                    ctx.clickDesktop(point(ctx, studio(ctx).menuItemCenter(6)));
                })
                .thenWaitUntil(() -> studio(ctx).fileDialog().isOpen(), SCREEN_WAIT, "the Save As window")
                .thenScreenshot(2, "isms-save-as")
                .then(2, () -> ctx.clickDesktop(studio(ctx).fileDialog().primaryPoint()))
                .thenWaitUntil(() -> !studio(ctx).documentPath().isEmpty(), WORK_WAIT,
                        "the script to be saved and the tab to know its file")
                // Read on the client thread here, so the server probe below touches only the server's own state.
                .then(0, () -> path[0] = studio(ctx).documentPath())
                .thenWaitUntilServer(level -> savedScript(ctx, level, path[0]).contains("qty > 10"), SCREEN_WAIT,
                        "the script to be on the computer's disk", level -> "path=" + path[0])
                .then(2, () -> {
                    openMenu(ctx, 0);
                    ctx.clickDesktop(point(ctx, studio(ctx).menuItemCenter(1)));
                })
                .thenWaitUntil(() -> studio(ctx).fileDialog().isOpen() && studio(ctx).fileDialog().rowNames()
                        .stream().anyMatch(name -> name.endsWith(".iql")), SCREEN_WAIT, "the Open window with it")
                .then(2, () -> ctx.clickDesktop(studio(ctx).fileDialog().rowPoint(studio(ctx).fileDialog().rowNames()
                        .stream().filter(name -> name.endsWith(".iql")).findFirst().orElseThrow())))
                .then(2, () -> ctx.clickDesktop(studio(ctx).fileDialog().primaryPoint()))
                .thenWaitUntil(() -> studio(ctx).documentCount() == 2
                                && studio(ctx).scriptText().contains("qty > 10"), WORK_WAIT,
                        "the script to open in a tab of its own")
                .then(SETTLE, () -> leave(ctx))
                .thenAwaitNoScreen(SCREEN_WAIT);
    }

    /** On a Legacy computer the studio is the Query Analyzer of 2000, with its Object Browser. */
    @ClientTest(timeoutTicks = 4000)
    public static void isms_isTheQueryAnalyzerOnALegacyComputer(final ClientTestContext ctx) {
        openOnAge(ctx, Age.LEGACY, "QUERY_ANALYZER", "isms-legacy");
    }

    /** On a Transition computer the studio is the one of 2008. */
    @ClientTest(timeoutTicks = 4000)
    public static void isms_isThe2008StudioOnATransitionComputer(final ClientTestContext ctx) {
        openOnAge(ctx, Age.TRANSITION, "STUDIO_2008", "isms-transition");
    }

    /** On an Advanced computer the studio is the one of 2022. */
    @ClientTest(timeoutTicks = 4000)
    public static void isms_isThe2022StudioOnAnAdvancedComputer(final ClientTestContext ctx) {
        openOnAge(ctx, Age.ADVANCED, "STUDIO_2022", "isms-advanced");
    }

    private static void openOnAge(final ClientTestContext ctx, final Age age, final String look,
                                  final String screenshot) {
        ctx.thenBuild(0, world -> {
                    final TestWorldBuilder.CraftingNetwork net = world.buildCraftingNetwork();
                    world.setBlock(COMPUTER_CABLE, ComputingModule.ETHERNET_CABLE);
                    world.setBlock(COMPUTER, age.machine().get());
                    world.faceRearTowardCable(COMPUTER);
                    final AbstractSmallComputerBlockEntity pc =
                            (AbstractSmallComputerBlockEntity) world.getBlockEntity(COMPUTER);
                    age.install(pc);
                    pc.installOs(age.system());
                    pc.console().install(STUDIO);
                    pc.togglePower();
                    world.placeMonitor(MONITOR, Direction.EAST);
                    net.seed(Items.COBBLESTONE, 64);
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> !ctx.screen(DesktopScreen.class).launcherLabels().isEmpty(), SCREEN_WAIT,
                        "the desktop to list its programs")
                .then(SETTLE, () -> DesktopScreen.requestOpen(STUDIO))
                .thenWaitUntil(() -> studio(ctx) != null && studio(ctx).schema() != null, SCREEN_WAIT,
                        "the studio with the network")
                .then(2, () -> expect(ctx, studio(ctx).lookName().equals(look),
                        "the studio wears its age's look; got " + studio(ctx).lookName()))
                .then(0, () -> expect(ctx, !age.analyzer() || studio(ctx).explorerLabels().contains("Maintenance"),
                        "the Query Analyzer calls its management Maintenance; got " + studio(ctx).explorerLabels()))
                .then(1, () -> {
                    studio(ctx).typeScript("QUERY items;");
                    ctx.key(GLFW.GLFW_KEY_F5);
                })
                .thenWaitUntil(() -> !studio(ctx).running() && studio(ctx).resultColumns().size() == 1, WORK_WAIT,
                        "the query to come back in this age too")
                .thenScreenshot(2, screenshot)
                .then(SETTLE, () -> leave(ctx))
                .thenAwaitNoScreen(SCREEN_WAIT);
    }

    /*
     * The standard network with a running Standard computer on it, the studio installed and a monitor; the desktop
     * up and the studio open with what the network said of itself.
     */
    private static ClientTestContext openStudio(final ClientTestContext ctx,
                                                @Nullable final Consumer<TestWorldBuilder.CraftingNetwork> extra) {
        return ctx.thenBuild(0, world -> {
                    final TestWorldBuilder.CraftingNetwork net = world.buildCraftingNetwork();
                    world.setBlock(COMPUTER_CABLE, ComputingModule.ETHERNET_CABLE);
                    final PersonalComputerBlockEntity pc = world.placeRunningPersonalComputer(COMPUTER, FRAMES_11);
                    pc.console().install(STUDIO);
                    world.placeMonitor(MONITOR, Direction.EAST);
                    net.seed(Items.COBBLESTONE, 64);
                    net.seed(Items.OAK_LOG, 8);
                    if (extra != null) {
                        extra.accept(net);
                    }
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> !ctx.screen(DesktopScreen.class).launcherLabels().isEmpty(), SCREEN_WAIT,
                        "the desktop to list its programs")
                .then(SETTLE, () -> DesktopScreen.requestOpen(STUDIO))
                .thenWaitUntil(() -> studio(ctx) != null && studio(ctx).schema() != null
                                && studio(ctx).schema().engine().compatible(), SCREEN_WAIT,
                        "the studio with the network and its Midsoft IQL Server");
    }

    /* An assertion read when its step runs, so its words may say what the window shows then. */
    private static void expect(final ClientTestContext ctx, final boolean condition, final String message) {
        ctx.assertTrue(condition, message);
    }

    private static void openMenu(final ClientTestContext ctx, final int index) {
        ctx.clickDesktop(point(ctx, studio(ctx).menuTitleCenter(index)));
    }

    private static void leave(final ClientTestContext ctx) {
        ctx.key(GLFW.GLFW_KEY_ESCAPE);
        if (ctx.mc().screen != null) {
            ctx.key(GLFW.GLFW_KEY_ESCAPE);
        }
        if (ctx.mc().screen != null) {
            ctx.key(GLFW.GLFW_KEY_ESCAPE);
        }
    }

    private static long held(final ClientTestContext ctx, final ServerLevel level, final Item item) {
        return level.getBlockEntity(ctx.abs(COMPUTER)) instanceof PersonalComputerBlockEntity pc
                && pc.networkUuid() != null
                ? NetworkStorage.of(level, pc.networkUuid()).query().getOrDefault(StorageKey.of(item), 0L) : 0L;
    }

    /* What the computer's disk holds at the path the studio saved its script to, or nothing. */
    private static String savedScript(final ClientTestContext ctx, final ServerLevel level, final String path) {
        if (path.isEmpty() || !(level.getBlockEntity(ctx.abs(COMPUTER)) instanceof PersonalComputerBlockEntity pc)) {
            return "";
        }
        return DiskFilesystem.read(pc.systemDisk(), path).orElse("");
    }

    @Nullable
    private static IsmsApp studio(final ClientTestContext ctx) {
        if (!(ctx.mc().screen instanceof DesktopScreen desktop)) {
            return null;
        }
        final DesktopWindow window = desktop.windowFor(STUDIO);
        return window != null && window.app() instanceof IsmsApp app ? app : null;
    }

    @Nullable
    private static IsmsProfilerApp profiler(final ClientTestContext ctx) {
        if (!(ctx.mc().screen instanceof DesktopScreen desktop)) {
            return null;
        }
        final DesktopWindow window = desktop.windowFor(PROFILER);
        return window != null && window.app() instanceof IsmsProfilerApp app ? app : null;
    }

    /** Converts a Profiler content-local point into desktop coordinates. */
    private static int[] profilerPoint(final ClientTestContext ctx, final int[] local) {
        final DesktopWindow window = ctx.screen(DesktopScreen.class).windowFor(PROFILER);
        if (window == null || local == null) {
            throw new ClientTestFailure("the Profiler window, or the point in it, is gone");
        }
        return DesktopSteps.contentPoint(window, local);
    }

    /** Converts a studio content-local point into desktop coordinates. */
    private static int[] point(final ClientTestContext ctx, final int[] local) {
        final DesktopWindow window = ctx.screen(DesktopScreen.class).windowFor(STUDIO);
        if (window == null || local == null) {
            throw new ClientTestFailure("the studio window, or the point in it, is gone");
        }
        return DesktopSteps.contentPoint(window, local);
    }

    /** A computer of an age and what is built in it, by item id, and the system it runs. */
    private enum Age {
        LEGACY(ComputingModule.LEGACY_PERSONAL_COMPUTER, "motherboard_atx_legacy_939", "cpu_velocion_sprint_64_fx_55",
                List.of("ram_ddr_1024", "ram_ddr_1024"), "gpu_radiance_x800_xt", "psu_500b",
                "disk_vaultis_link_ide_40g", FRAMES_XP),
        TRANSITION(ComputingModule.TRANSITION_PERSONAL_COMPUTER, "motherboard_atx_transition_775",
                "cpu_integra_centro_2_duo_e6600", List.of("ram_ddr2_2048", "ram_ddr2_2048"), "gpu_vertex_8800_gt",
                "psu_450b", "disk_hdd_500g", FRAMES_7),
        ADVANCED(ComputingModule.ADVANCED_PERSONAL_COMPUTER, "motherboard_atx_advanced_1151",
                "cpu_integra_centro_c9_9900k", List.of("ram_ddr4_16384", "ram_ddr4_16384"), "gpu_vertex_gtx_1080_ti",
                "psu_1000g", "disk_ssd_8t", FRAMES_11);

        private final BlockEntry<?> machine;
        private final String board;
        private final String cpu;
        private final List<String> rams;
        private final String gpu;
        private final String psu;
        private final String disk;
        private final ResourceLocation system;

        Age(final BlockEntry<?> machine, final String board, final String cpu, final List<String> rams,
            final String gpu, final String psu, final String disk, final ResourceLocation system) {
            this.machine = machine;
            this.board = board;
            this.cpu = cpu;
            this.rams = rams;
            this.gpu = gpu;
            this.psu = psu;
            this.disk = disk;
            this.system = system;
        }

        BlockEntry<?> machine() {
            return machine;
        }

        ResourceLocation system() {
            return system;
        }

        boolean analyzer() {
            return this == LEGACY;
        }

        void install(final AbstractSmallComputerBlockEntity computer) {
            final ComputerHardwareLayout layout = computer.hardwareLayout();
            final ItemStackHandler slots = computer.getHardware();
            slots.setStackInSlot(layout.motherboardSlot(), stack(board));
            slots.setStackInSlot(layout.cpuStart(), stack(cpu));
            for (int k = 0; k < rams.size(); k++) {
                slots.setStackInSlot(layout.ramStart() + k, stack(rams.get(k)));
            }
            slots.setStackInSlot(layout.pcieStart(), stack(gpu));
            slots.setStackInSlot(layout.psuSlot(), stack(psu));
            slots.setStackInSlot(layout.diskStart(), stack(disk));
        }

        private static ItemStack stack(final String id) {
            final Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, id));
            if (item == Items.AIR) {
                throw new IllegalStateException("no item " + id);
            }
            return new ItemStack(item);
        }
    }
}
