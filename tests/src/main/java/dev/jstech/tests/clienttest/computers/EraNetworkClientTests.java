/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.computers;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.block.RedstoneInterfaceBlock;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.blockentity.RedstoneInterfaceBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.client.ComputerTerminalScreen;
import dev.jstech.computers.client.RedstoneInterfaceScreen;
import dev.jstech.computers.client.ServerRackScreen;
import dev.jstech.computers.client.os.AutomationManagerApp;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.engine.INetworkEngine;
import dev.jstech.computers.engine.NetworkEngines;
import dev.jstech.computers.gui.layout.ServerRackLayout;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.operation.index.ItemLocation;
import dev.jstech.computers.program.Programs;
import dev.jstech.computers.storage.DriveVolumes;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.cable.CableEntry;
import dev.jstech.core.network.DataLine;
import dev.jstech.core.network.DataLink;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.core.uuid.NodeUuid;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.computers.EraKit;
import dev.jstech.tests.testkit.computers.EraNetwork;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * Each era's network worked by a player, on the era's network as the kit lays it. A run of the access line twice its
 * reach, with a cable in its middle, leaves the computer at its far end off the network until the player takes that
 * cable out and puts the era's repeater in its place. From the Legacy, a cable of the era before, laid by hand from the
 * access router, is taken by that newer router and brings a computer at its end onto the network. A Redstone
 * Interface beside the computer, named in its window, lights a lamp and puts it out again at a statement typed at the
 * prompt. Each engine the era has answers the network's statements while it runs. From the Legacy, a job typed at the
 * prompt runs on the Automation Engine and empties the stone it names, and the Automation Manager sees the engine; the
 * rack's server with the Load Balancer spreads two writes over its drives, and from the Standard the Predictive Cache
 * cuts what the index records for its reads, and the Integrity Monitor keeps the index sure of a bay a disk is pulled
 * from.
 */
public final class EraNetworkClientTests {

    private static final int SETTLE = EraJourney.SETTLE;
    private static final int WAIT = 160;
    /** A service's setup on the rack's server, which runs its course before the server has it. */
    private static final int INSTALL_WAIT = 2_400;
    private static final int TIMEOUT = 12_000;
    /** How long the cables are given to settle into lines before the network on them is read. */
    private static final int LINES_SETTLE = 40;
    /** How many cells a row of a winding run has, and how far apart its rows lie. */
    private static final int ROW = 10;
    private static final int ROW_GAP = 3;
    /** How many cables of the era before run from the router to the computer at their end. */
    private static final int OLDER_RUN = 3;
    private static final BlockPos ROUTER = EraNetwork.ROUTER;
    /* The Redstone Interface south of the computer, its back on it, and the lamp before its lens. */
    private static final BlockPos INTERFACE = new BlockPos(9, 2, 3);
    private static final BlockPos LAMP = new BlockPos(9, 2, 4);
    private static final BlockPos BY_THE_INTERFACE = new BlockPos(11, 2, 4);
    private static final String GATE = "Gate";
    /* At the rack, to work its screen. */
    private static final BlockPos AT_THE_RACK = new BlockPos(5, 2, 9);
    /* The monitor in front of the Mainframe, whose console is where the index is looked after. */
    private static final BlockPos MAINFRAME_MONITOR = new BlockPos(3, 2, 1);
    private static final BlockPos AT_THE_MAINFRAME = new BlockPos(5, 2, 1);
    private static final int STONE = 64;
    /** How many in a hundred the Predictive Cache takes off a server's reads. */
    private static final int PREDICTIVE_CUT = 15;

    private EraNetworkClientTests() {
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void vintage_linesCarryTheNetworkFar(final ClientTestContext ctx) {
        lines(ctx, EraKit.VINTAGE);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void legacy_linesCarryTheNetworkFar(final ClientTestContext ctx) {
        lines(ctx, EraKit.LEGACY);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void transition_linesCarryTheNetworkFar(final ClientTestContext ctx) {
        lines(ctx, EraKit.TRANSITION);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void standard_linesCarryTheNetworkFar(final ClientTestContext ctx) {
        lines(ctx, EraKit.STANDARD);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void advanced_linesCarryTheNetworkFar(final ClientTestContext ctx) {
        lines(ctx, EraKit.ADVANCED);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void vintage_redstoneInterfaceIsSwitchedByStatement(final ClientTestContext ctx) {
        redstone(ctx, EraKit.VINTAGE);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void legacy_redstoneInterfaceIsSwitchedByStatement(final ClientTestContext ctx) {
        redstone(ctx, EraKit.LEGACY);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void transition_redstoneInterfaceIsSwitchedByStatement(final ClientTestContext ctx) {
        redstone(ctx, EraKit.TRANSITION);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void standard_redstoneInterfaceIsSwitchedByStatement(final ClientTestContext ctx) {
        redstone(ctx, EraKit.STANDARD);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void advanced_redstoneInterfaceIsSwitchedByStatement(final ClientTestContext ctx) {
        redstone(ctx, EraKit.ADVANCED);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void vintage_eachEngineAnswersTheNetwork(final ClientTestContext ctx) {
        engines(ctx, EraKit.VINTAGE);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void legacy_eachEngineAnswersTheNetwork(final ClientTestContext ctx) {
        engines(ctx, EraKit.LEGACY);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void transition_eachEngineAnswersTheNetwork(final ClientTestContext ctx) {
        engines(ctx, EraKit.TRANSITION);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void standard_eachEngineAnswersTheNetwork(final ClientTestContext ctx) {
        engines(ctx, EraKit.STANDARD);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void advanced_eachEngineAnswersTheNetwork(final ClientTestContext ctx) {
        engines(ctx, EraKit.ADVANCED);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void legacy_jobsRunOnTheAutomationEngine(final ClientTestContext ctx) {
        automation(ctx, EraKit.LEGACY);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void transition_jobsRunOnTheAutomationEngine(final ClientTestContext ctx) {
        automation(ctx, EraKit.TRANSITION);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void standard_jobsRunOnTheAutomationEngine(final ClientTestContext ctx) {
        automation(ctx, EraKit.STANDARD);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void advanced_jobsRunOnTheAutomationEngine(final ClientTestContext ctx) {
        automation(ctx, EraKit.ADVANCED);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void legacy_serverServicesDoTheirWork(final ClientTestContext ctx) {
        services(ctx, EraKit.LEGACY);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void transition_serverServicesDoTheirWork(final ClientTestContext ctx) {
        services(ctx, EraKit.TRANSITION);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void standard_serverServicesDoTheirWork(final ClientTestContext ctx) {
        services(ctx, EraKit.STANDARD);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void advanced_serverServicesDoTheirWork(final ClientTestContext ctx) {
        services(ctx, EraKit.ADVANCED);
    }

    /*
     * The repeater: a run north from the router, twice the access line's reach with a cable in its middle, is laid as
     * the kit lays a network, and the computer at its end stays off; the player takes the middle cable out and puts
     * the era's repeater there, and the computer joins. From the Legacy, a short run of the era before's cable laid
     * by hand from the router's south side: the router takes it, and the computer at its end joins too.
     */
    private static void lines(final ClientTestContext ctx, final EraKit kit) {
        final int run = new DataLink(DataLine.ACCESS, kit.era()).range() - 2;
        final Winding north = new Winding(ROUTER.north(), Direction.NORTH, leadFor(run), 2 * run + 2);
        final BlockPos middle = north.cell(run);
        final BlockPos farComputer = north.cell(2 * run + 1);
        final Winding south = new Winding(ROUTER.south(), Direction.SOUTH, OLDER_RUN + 1, OLDER_RUN + 1);
        EraJourney.lay(ctx, kit).thenBuild(0, world -> {
            for (int i = 0; i < 2 * run + 1; i++) {
                world.setBlock(north.cell(i), kit.access());
            }
            EraNetwork.runningComputer(world, kit, farComputer);
        });
        ctx.thenTeleport(SETTLE, north.stand(run), Direction.NORTH)
                .thenServer(LINES_SETTLE, level -> ctx.assertTrue(!onTheNetwork(ctx, level, farComputer),
                        "a run twice the access line's reach leaves the computer at its end off the network; it is on "
                                + computerAt(ctx, level, farComputer).networkUuid()))
                .then(SETTLE, () -> Objects.requireNonNull(ctx.mc().gameMode, "no game mode")
                        .startDestroyBlock(ctx.abs(middle), Direction.UP))
                .thenWaitUntilServer(level -> level.getBlockState(ctx.abs(middle)).isAir(), WAIT,
                        "the middle cable to come out by hand", level -> "found "
                                + level.getBlockState(ctx.abs(middle)))
                .thenGive(0, new ItemStack(kit.repeater()))
                .then(SETTLE, () -> ctx.selectHotbar(0))
                .thenPlace(1, middle)
                .thenWaitUntilServer(level -> level.getBlockState(ctx.abs(middle)).is(kit.repeater()), WAIT,
                        "the repeater to go down from the hand", level -> "found "
                                + level.getBlockState(ctx.abs(middle)))
                .thenTeleport(SETTLE, north.stand(2 * run), Direction.NORTH)
                .thenWaitUntilServer(level -> onTheNetwork(ctx, level, farComputer), WAIT,
                        "the repeater to renew the line, and the computer at its end to join the network",
                        level -> "the far computer is on " + computerAt(ctx, level, farComputer).networkUuid())
                .thenScreenshot(SETTLE, shot(kit, "repeater"));
        if (kit.previous() == null) {
            return;
        }
        // The cable of the era before, laid by hand from the router's south side to a computer at its end.
        final CableEntry older = kit.previous().access();
        ctx.thenGive(0, older.stack(OLDER_RUN));
        for (int i = 0; i < OLDER_RUN; i++) {
            hand(ctx, south, i, 0);
        }
        ctx.thenBuild(SETTLE, world -> EraNetwork.runningComputer(world, kit, south.cell(OLDER_RUN)))
                .thenTeleport(SETTLE, south.stand(OLDER_RUN - 1), Direction.SOUTH)
                .thenWaitUntilServer(level -> onTheNetwork(ctx, level, south.cell(OLDER_RUN)), WAIT,
                        "the router to take the cable of the era before and bring the computer on it onto the network",
                        level -> "the computer is on " + computerAt(ctx, level, south.cell(OLDER_RUN)).networkUuid()
                                + "; the cable on " + networkAt(ctx, level, south.cell(0), older))
                .thenScreenshot(SETTLE, shot(kit, "older-line"));
    }

    /*
     * The interface beside the computer, named in its window by the player; at the prompt, a statement sets it to emit
     * and the lamp before it lights, another sets it to nothing and the lamp goes out.
     */
    private static void redstone(final ClientTestContext ctx, final EraKit kit) {
        EraJourney.lay(ctx, kit).thenBuild(0, world -> {
            world.setBlock(INTERFACE, kit.redstoneInterface().defaultBlockState()
                    .setValue(RedstoneInterfaceBlock.FACING, Direction.SOUTH));
            world.setBlock(LAMP, Blocks.REDSTONE_LAMP);
        });
        ctx.thenWaitUntilServer(level -> sensor(ctx, level).live(), WAIT,
                        "the interface to link to the computer behind it", level -> "not linked")
                .thenTeleport(SETTLE, BY_THE_INTERFACE, Direction.WEST)
                .then(SETTLE, () -> ctx.selectHotbar(8))
                .thenRightClick(SETTLE, INTERFACE)
                .thenAwaitScreen(RedstoneInterfaceScreen.class, WAIT)
                .then(SETTLE, () -> {
                    final int[] name = ctx.screen(RedstoneInterfaceScreen.class).nameCenter();
                    ctx.click(name[0], name[1]);
                })
                .then(SETTLE, () -> ctx.type(GATE))
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenWaitUntil(EraJourney.away(ctx), WAIT, "the window to close")
                .thenWaitUntilServer(level -> GATE.equals(sensor(ctx, level).name()), WAIT,
                        "the name typed in the window to name the interface",
                        level -> "named '" + sensor(ctx, level).name() + "'");
        atThePrompt(ctx, kit);
        statement(ctx, "iql SET REDSTONE '" + GATE + "' OUT 15");
        ctx.thenWaitUntilServer(level -> level.getBlockState(ctx.abs(LAMP)).getValue(RedstoneLampBlock.LIT), WAIT,
                        "the statement to set the interface emitting and the lamp before it to light",
                        level -> "strength=" + sensor(ctx, level).strength())
                .thenScreenshot(SETTLE, shot(kit, "lamp-on"));
        statement(ctx, "iql SET REDSTONE '" + GATE + "' OUT 0");
        ctx.thenWaitUntilServer(level -> !level.getBlockState(ctx.abs(LAMP)).getValue(RedstoneLampBlock.LIT), WAIT,
                "the next statement to put the lamp out", level -> "strength=" + sensor(ctx, level).strength());
    }

    /*
     * Each engine of the era put on the Mainframe and run in turn; with each running, a SELECT typed at the prompt
     * brings stone to the computer.
     */
    private static void engines(final ClientTestContext ctx, final EraKit kit) {
        final List<ResourceLocation> engines = new ArrayList<>();
        for (final INetworkEngine engine : NetworkEngines.all()) {
            if (JsComputers.MODID.equals(engine.def().program().getNamespace())
                    && engine.def().versions().containsKey(kit.era())) {
                engines.add(engine.def().program());
            }
        }
        ctx.assertTrue(!engines.isEmpty(), "the " + kit + " era has an engine");
        EraJourney.lay(ctx, kit).thenServer(SETTLE, level -> NetworkStorage.of(level,
                mainframe(ctx, level).networkUuid()).insert(StorageKey.of(Items.COBBLESTONE), STONE));
        atThePrompt(ctx, kit);
        final AtomicInteger brought = new AtomicInteger();
        for (final ResourceLocation engine : engines) {
            ctx.thenServer(SETTLE, level -> {
                final MainframeBlockEntity mainframe = mainframe(ctx, level);
                ctx.assertTrue(mainframe.installEngine(engine) || mainframe.installedEngines().containsKey(engine),
                        engine + " goes on the Mainframe");
                ctx.assertTrue(mainframe.activateEngine(engine) || engine.equals(mainframe.activeEngine()),
                        engine + " runs the network's statements");
            });
            statement(ctx, "iql SELECT 2 Cobblestone");
            ctx.thenWaitUntilServer(level -> engine.equals(mainframe(ctx, level).activeEngine())
                            && local(ctx, level) == brought.get() + 2, WAIT,
                    "the SELECT to bring the stone through " + engine, level -> "engine="
                            + mainframe(ctx, level).activeEngine() + " local=" + local(ctx, level))
                    .then(0, () -> brought.addAndGet(2));
        }
        ctx.thenScreenshot(SETTLE, shot(kit, "engines"));
    }

    /*
     * The Automation Engine on the Mainframe; a job typed at the prompt drops the stone it names on its own, every few
     * ticks; the Automation Manager, opened from Start, finds the engine online.
     */
    private static void automation(final ClientTestContext ctx, final EraKit kit) {
        EraJourney.lay(ctx, kit).thenServer(SETTLE, level -> {
            NetworkStorage.of(level, mainframe(ctx, level).networkUuid())
                    .insert(StorageKey.of(Items.COBBLESTONE), STONE);
            ctx.assertTrue(mainframe(ctx, level).installAutomationEngine(), "the Automation Engine goes on");
            computerAt(ctx, level, EraNetwork.COMPUTER).console().install(Programs.AUTOMATION_MANAGER.toString());
        });
        atThePrompt(ctx, kit);
        statement(ctx, "iql CREATE JOB drainer AS DROP 10 cobblestone EVERY 5t");
        ctx.thenWaitUntilServer(level -> stored(ctx, level) <= STONE - 20, WAIT * 2,
                        "the job to run on its own and drop the stone, more than once",
                        level -> "stone=" + stored(ctx, level))
                .thenScreenshot(SETTLE, shot(kit, "job"));
        EraJourney.launch(ctx, kit, AutomationManagerApp.TITLE)
                .thenWaitUntil(() -> {
                    final AutomationManagerApp app = DesktopSteps.app(ctx, AutomationManagerApp.TITLE,
                            AutomationManagerApp.class);
                    return app != null && app.hasState() && app.engineOnline();
                }, WAIT, "the Automation Manager to find the engine online")
                .thenScreenshot(SETTLE, shot(kit, "automation-manager"));
    }

    /*
     * The rack's server running Debian with the era's services. Two equal writes typed at the prompt land one on each
     * of its drives with the Load Balancer. From the Standard, the Predictive Cache takes fifteen in a hundred off the
     * read latency the index records once the player reindexes at the Mainframe's console, where the index is looked
     * after; and a disk pulled from the server's bay through the
     * rack's screen leaves the index in doubt, until the Integrity Monitor runs and the next pull leaves it sure.
     */
    private static void services(final ClientTestContext ctx, final EraKit kit) {
        final boolean standard = kit.era().isAtLeast(EraKit.STANDARD.era());
        final int[] drive = new int[1];
        EraJourney.lay(ctx, kit).thenServer(SETTLE, level -> {
            final ServerRackBlockEntity rack = rack(ctx, level);
            ctx.assertTrue(rack.unitHost(0).installOs(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
                    "debian")), "Debian goes on the rack's server");
            rack.consoleOf(0).install(Programs.LOAD_BALANCER.toString());
            final PersonalComputerBlockEntity computer = computerAt(ctx, level, EraNetwork.COMPUTER);
            computer.localStore().insert(StorageKey.of(Items.COBBLESTONE), STONE);
            computer.localStore().insert(StorageKey.of(Items.DIRT), STONE);
        });
        serving(ctx, Programs.LOAD_BALANCER);
        atThePrompt(ctx, kit);
        statement(ctx, "iql INSERT " + STONE + " Cobblestone");
        statement(ctx, "iql INSERT " + STONE + " Dirt");
        ctx.thenWaitUntilServer(level -> balanced(rack(ctx, level)), WAIT,
                "the Load Balancer to put the two writes one on each drive", level -> usage(rack(ctx, level)))
                .thenScreenshot(SETTLE, shot(kit, "load-balancer"));
        if (!standard) {
            return;
        }
        // The index is looked after at the Mainframe's own console, the only place its upkeep runs.
        ctx.thenBuild(0, world -> world.placeMonitor(MAINFRAME_MONITOR, Direction.EAST, kit.monitor()));
        reindex(ctx);
        ctx.thenWaitUntilServer(level -> (drive[0] = latency(ctx, level)) > 0, WAIT,
                        "the reindex to record where the stone is and how long it takes to read",
                        level -> "latency=" + latency(ctx, level))
                .thenServer(0, level -> rack(ctx, level).consoleOf(0).install(Programs.PREDICTIVE_CACHE.toString()));
        serving(ctx, Programs.PREDICTIVE_CACHE);
        reindex(ctx);
        ctx.thenWaitUntilServer(level -> latency(ctx, level) == cached(ctx, level), WAIT,
                "the Predictive Cache to take fifteen in a hundred off the server's reads once reindexed",
                level -> drive[0] + " ticks became " + latency(ctx, level) + "; the index has "
                        + places(ctx, level));
        reindex(ctx);
        pull(ctx, kit, "without the Integrity Monitor");
        ctx.thenWaitUntilServer(level -> !mainframe(ctx, level).networkIndex().health().isClean(), WAIT,
                "a disk pulled from a running server's bay to leave the index in doubt", level -> "clean");
        putBack(ctx);
        ctx.thenServer(SETTLE, level -> rack(ctx, level).consoleOf(0).install(Programs.INTEGRITY_MONITOR.toString()));
        serving(ctx, Programs.INTEGRITY_MONITOR);
        reindex(ctx);
        ctx.thenWaitUntilServer(level -> mainframe(ctx, level).networkIndex().health().isClean(), WAIT,
                "the reindex to make the index sure again", level -> "in doubt");
        pull(ctx, kit, "with the Integrity Monitor");
        ctx.thenWaitUntilServer(level -> mainframe(ctx, level).networkIndex().health().isClean(), WAIT,
                "the Integrity Monitor to keep the index sure of the bay", level -> "in doubt")
                .thenScreenshot(SETTLE, shot(kit, "integrity"));
        putBack(ctx);
    }

    /* At the Mainframe's console, the index rebuilt from every disk. */
    private static void reindex(final ClientTestContext ctx) {
        ctx.thenWaitUntil(EraJourney.away(ctx), WAIT, "the screen to be left")
                .thenTeleport(SETTLE, AT_THE_MAINFRAME, Direction.WEST)
                .thenRightClick(SETTLE, MAINFRAME_MONITOR)
                .thenAwaitScreen(ComputerTerminalScreen.class, EraJourney.BOOT_WAIT)
                .then(SETTLE, () -> {
                    final int[] console = ctx.screen(ComputerTerminalScreen.class).headingPoint("Console");
                    ctx.clickGui(console[0], console[1]);
                });
        statement(ctx, "iql REINDEX");
        ctx.thenWaitUntil(() -> consoleOf(ctx).contains("REINDEX started"), WAIT,
                "the Mainframe's console to answer the reindex", () -> "the console shows " + consoleOf(ctx));
    }

    /* The last of what the Mainframe's console shows the player. */
    private static String consoleOf(final ClientTestContext ctx) {
        final ComputerTerminalScreen screen = ctx.screen(ComputerTerminalScreen.class);
        final String text = screen == null ? "no terminal" : screen.consoleText();
        return text.substring(Math.max(0, text.length() - 300));
    }

    /* Waits out the setup of a service put on the rack's server, until the server runs it. */
    private static void serving(final ClientTestContext ctx, final ResourceLocation service) {
        ctx.thenWaitUntilServer(level -> rack(ctx, level).hasService(0, service), INSTALL_WAIT,
                service.getPath() + " to finish its setup on the rack's server", level -> "still setting up");
    }

    /* At the rack's screen, the first disk of its server's bay taken into the hotbar. */
    private static void pull(final ClientTestContext ctx, final EraKit kit, final String when) {
        ctx.thenWaitUntil(EraJourney.away(ctx), WAIT, "the prompt to be left")
                .thenGive(0)
                .thenTeleport(SETTLE, AT_THE_RACK, Direction.NORTH)
                .then(SETTLE, () -> ctx.selectHotbar(8))
                .thenRightClick(SETTLE, EraNetwork.RACK)
                .thenAwaitScreen(ServerRackScreen.class, WAIT)
                .then(2, () -> ctx.clickGui(ServerRackLayout.frontItemX(0) + 8, ServerRackLayout.itemY(0) + 8))
                .then(2, () -> ctx.clickGui(ServerRackLayout.INV_X + 8, ServerRackLayout.HOTBAR_Y + 8))
                .thenWaitUntilServer(level -> rack(ctx, level).claimedDriveStacks(0).size() == 1, WAIT,
                        "the disk to come out of the bay " + when, level -> "disks="
                                + rack(ctx, level).claimedDriveStacks(0).size());
    }

    /* The disk back from the hotbar into its bay, through the rack's screen still open. */
    private static void putBack(final ClientTestContext ctx) {
        ctx.then(2, () -> ctx.clickGui(ServerRackLayout.INV_X + 8, ServerRackLayout.HOTBAR_Y + 8))
                .then(2, () -> ctx.clickGui(ServerRackLayout.frontItemX(0) + 8, ServerRackLayout.itemY(0) + 8))
                .thenWaitUntilServer(level -> rack(ctx, level).claimedDriveStacks(0).size() == 2, WAIT,
                        "the disk to go back in", level -> "disks=" + rack(ctx, level).claimedDriveStacks(0).size());
    }

    /* Puts down by hand what hotbar slot {@code slot} holds at cell {@code index} of the run. */
    private static void hand(final ClientTestContext ctx, final Winding run, final int index, final int slot) {
        if (index == 0 || !run.stand(index).equals(run.stand(index - 1))) {
            ctx.thenTeleport(1, run.stand(index), Direction.NORTH);
        }
        ctx.then(1, () -> ctx.selectHotbar(slot))
                .thenPlace(1, run.cell(index));
    }

    /* The player at the computer's prompt: MC-DOS's own, or the terminal opened from Start. */
    private static void atThePrompt(final ClientTestContext ctx, final EraKit kit) {
        ctx.thenWaitUntil(EraJourney.away(ctx), WAIT, "the screen to be left")
                .thenTeleport(SETTLE, EraNetwork.AT_THE_DESK, Direction.WEST)
                .thenRightClick(SETTLE, EraNetwork.MONITOR)
                .thenAwaitScreen(EraJourney.systemScreen(kit), EraJourney.BOOT_WAIT);
        if (kit != EraKit.VINTAGE) {
            final String terminal = EraJourney.terminal(kit);
            EraJourney.launch(ctx, kit, terminal)
                    .thenWaitUntil(() -> ctx.screen(DesktopScreen.class).windowFor(terminal) != null, WAIT,
                            "the " + terminal + " window to open from Start");
        }
    }

    /* Types the statement at the prompt and sends it. */
    private static void statement(final ClientTestContext ctx, final String line) {
        ctx.then(SETTLE, () -> {
            ctx.type(line);
            ctx.key(GLFW.GLFW_KEY_ENTER);
        });
    }

    /* How long one lead of a winding run is, so its middle cell falls in the middle of a row, clear of its turns. */
    private static int leadFor(final int middle) {
        return Math.floorMod(middle - ROW / 2 - 1, ROW + ROW_GAP - 1) + 1;
    }

    /* The network the cable of that kind at that cell is on, for a failure to say where the line stopped. */
    private static String networkAt(final ClientTestContext ctx, final ServerLevel level, final BlockPos at,
                                    final CableEntry cable) {
        return level.getBlockEntity(ctx.abs(at)) instanceof CableBlockEntity laid
                ? laid.holds(cable.get()) ? String.valueOf(laid.network(cable.get()).orElse(null)) : "no such cable"
                : "nothing at " + at;
    }

    private static boolean onTheNetwork(final ClientTestContext ctx, final ServerLevel level, final BlockPos at) {
        final NetworkUuid network = mainframe(ctx, level).networkUuid();
        return network != null && network.equals(computerAt(ctx, level, at).networkUuid());
    }

    /* Whether the server's two drives hold the same weight, neither of it none. */
    private static boolean balanced(final ServerRackBlockEntity rack) {
        final List<ItemStack> drives = rack.claimedDriveStacks(0);
        return drives.size() == 2 && DriveVolumes.usedWeight(drives.get(0)) > 0
                && DriveVolumes.usedWeight(drives.get(0)) == DriveVolumes.usedWeight(drives.get(1));
    }

    private static String usage(final ServerRackBlockEntity rack) {
        final StringBuilder out = new StringBuilder("drives:");
        for (final ItemStack drive : rack.claimedDriveStacks(0)) {
            out.append(' ').append(DriveVolumes.usedWeight(drive));
        }
        return out.toString();
    }

    /*
     * The read latency the index records for the stone on the rack's server, or -1 while it holds none there: the
     * computer's own disk is a place the index knows the stone in too, at a disk's latency of its own.
     */
    private static int latency(final ClientTestContext ctx, final ServerLevel level) {
        final NodeUuid server = rack(ctx, level).unitHost(0).nodeUuid();
        for (final ItemLocation row : mainframe(ctx, level).networkIndex().locations(
                StorageKey.of(Items.COBBLESTONE))) {
            if (row.server().equals(server)) {
                return row.latencyTicks();
            }
        }
        return -1;
    }

    /*
     * What the index should record for the server with the Predictive Cache: fifteen in a hundred more taken off its
     * drives' own latency, on top of what the server's read cache card takes.
     */
    private static int cached(final ClientTestContext ctx, final ServerLevel level) {
        final ServerRackBlockEntity rack = rack(ctx, level);
        final NodeUuid server = rack.unitHost(0).nodeUuid();
        for (final ItemLocation row : mainframe(ctx, level).networkIndex().locations(
                StorageKey.of(Items.COBBLESTONE))) {
            if (row.server().equals(server)) {
                final int cut = rack.cacheCutPercent(0) + PREDICTIVE_CUT;
                return Math.max(1, row.tier().latencyTicks() * Math.max(0, 100 - cut) / 100);
            }
        }
        return -1;
    }

    /* Every place the index knows the stone in, with the latency it records for each. */
    private static String places(final ClientTestContext ctx, final ServerLevel level) {
        final StringBuilder out = new StringBuilder();
        for (final ItemLocation row : mainframe(ctx, level).networkIndex().locations(
                StorageKey.of(Items.COBBLESTONE))) {
            out.append('[').append(row.tier()).append(' ').append(row.quantity()).append(" at ")
                    .append(row.latencyTicks()).append(']');
        }
        return out.toString();
    }

    private static long local(final ClientTestContext ctx, final ServerLevel level) {
        return computerAt(ctx, level, EraNetwork.COMPUTER).localStore().view()
                .getOrDefault(StorageKey.of(Items.COBBLESTONE), 0L);
    }

    private static long stored(final ClientTestContext ctx, final ServerLevel level) {
        return NetworkStorage.of(level, mainframe(ctx, level).networkUuid()).count(StorageKey.of(Items.COBBLESTONE));
    }

    private static RedstoneInterfaceBlockEntity sensor(final ClientTestContext ctx, final ServerLevel level) {
        return TestWorldBuilder.at(level, ctx.origin()).blockEntity(INTERFACE, RedstoneInterfaceBlockEntity.class);
    }

    private static MainframeBlockEntity mainframe(final ClientTestContext ctx, final ServerLevel level) {
        return TestWorldBuilder.at(level, ctx.origin()).blockEntity(EraNetwork.MAINFRAME, MainframeBlockEntity.class);
    }

    private static PersonalComputerBlockEntity computerAt(final ClientTestContext ctx, final ServerLevel level,
                                                          final BlockPos at) {
        return TestWorldBuilder.at(level, ctx.origin()).blockEntity(at, PersonalComputerBlockEntity.class);
    }

    private static ServerRackBlockEntity rack(final ClientTestContext ctx, final ServerLevel level) {
        return TestWorldBuilder.at(level, ctx.origin()).blockEntity(EraNetwork.RACK, ServerRackBlockEntity.class);
    }

    private static String shot(final EraKit kit, final String what) {
        return kit.name().toLowerCase(Locale.ROOT) + "-" + what;
    }

    /**
     * A run of cells that winds to stay near where it starts: a lead straight on from its first cell, then rows of
     * {@link #ROW} cells east and west, {@link #ROW_GAP} apart, each joined to the next at its end; and for each cell
     * the place a player stands to put it down, a step back from it where nothing is laid.
     */
    private static final class Winding {

        private final List<BlockPos> cells = new ArrayList<>();
        private final List<BlockPos> stands = new ArrayList<>();

        Winding(final BlockPos start, final Direction away, final int lead, final int count) {
            BlockPos at = start;
            for (int i = 0; i < lead && cells.size() < count; i++) {
                add(at, at.east());
                at = at.relative(away);
            }
            Direction along = Direction.EAST;
            final int west = start.getX();
            final int east = start.getX() + ROW - 1;
            while (cells.size() < count) {
                for (int i = 0; i < ROW && cells.size() < count; i++) {
                    final int inward = at.getX() == west ? 1 : at.getX() == east ? -1 : 0;
                    add(at, at.relative(away.getOpposite()).offset(inward, 0, 0));
                    at = at.relative(along);
                }
                at = at.relative(along.getOpposite());
                final Direction inward = along.getOpposite();
                for (int i = 0; i < ROW_GAP - 1 && cells.size() < count; i++) {
                    at = at.relative(away);
                    add(at, at.relative(inward));
                }
                at = at.relative(away);
                along = along.getOpposite();
            }
        }

        BlockPos cell(final int index) {
            return cells.get(index);
        }

        BlockPos stand(final int index) {
            return stands.get(index);
        }

        private void add(final BlockPos cell, final BlockPos stand) {
            cells.add(cell);
            stands.add(stand);
        }
    }
}
