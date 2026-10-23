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
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.blockentity.PrinterBlockEntity;
import dev.jstech.computers.blockentity.RedstoneInterfaceBlockEntity;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.ShellApp;
import dev.jstech.computers.client.os.TaskManagerApp;
import dev.jstech.computers.client.os.VirtualStudioApp;
import dev.jstech.computers.client.os.VirtualStudioCodeApp;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.operation.payload.WireLine;
import dev.jstech.computers.os.edit.project.ProjectTemplate;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.CliStyle;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.sigma.LanguageLevel;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.computers.CraftFiles;
import dev.jstech.tests.testkit.computers.EraKit;
import dev.jstech.tests.testkit.computers.EraNetwork;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import org.lwjgl.glfw.GLFW;

/**
 * Each era's programming in a player's hands, on the era's network with a Redstone Interface named Gate and a lamp
 * before it beside the computer, and its printer. A program is written with a mistake in it and compiled with the
 * era's compiler, which names the mistake; it is corrected and compiled again, and run, and it lights the lamp. On the
 * Vintage this is Σ, written at MC-DOS's prompt and compiled with scc; from the Legacy it is Σ#, written in Virtual
 * Studio Code and compiled with sgsc. From the Legacy too, a program writes what the network holds into a file, which
 * the printer prints; a program asks the Crafting Computer for planks when the network has too few; a script waits
 * for stone to pass a mark in the network and lights the lamp when it does; one opens a window of its own; one plays
 * a tune; and Virtual Studio makes a project of its own, builds it and starts it. The waiting script, which stays up
 * in the background by itself, is listed by sigma ps and by the Task Manager, and sigma stop stops it by its number,
 * as it does the program with the window. On the Vintage the script is one called every tick, run with start, listed
 * by tasklist, and stopped with sigma stop by the number tasklist gives it.
 */
public final class EraProgrammingClientTests {

    private static final int SETTLE = EraJourney.SETTLE;
    private static final int WAIT = 200;
    private static final int TIMEOUT = 12_000;
    private static final BlockPos INTERFACE = new BlockPos(9, 2, 3);
    private static final BlockPos LAMP = new BlockPos(9, 2, 4);
    private static final BlockPos PRINTER = new BlockPos(9, 2, 1);
    private static final String GATE = "Gate";
    private static final String CODE = "Virtual Studio Code";
    private static final String STUDIO = "Virtual Studio";
    private static final String TASK_MANAGER = "jsc:task_manager";
    /** The template of Virtual Studio's wizard a console program starts from, in Σ#. */
    private static final ProjectTemplate.Offer CONSOLE =
            new ProjectTemplate.Offer(ProjectTemplate.CONSOLE_APP, LanguageLevel.SIGMA_SHARP);
    private static final int LOGS = 16;
    private static final int STONE = 32;

    /* The programs, each on one line, the way they are typed. */
    private static final String SIGMA_BROKEN = "using Standard.*; namespace Lamp; class Lamp { static void Main() {"
            + " Redstone.Named(\"Gate\").Out(15) } }";
    private static final String SIGMA_LAMP = "using Standard.*; namespace Lamp; class Lamp { static void Main() {"
            + " Redstone.Named(\"Gate\").Out(15); printf(\"lit\\n\"); } }";
    private static final String SIGMA_BEAT = "using Standard.*; namespace Beat; class Beat : Script {"
            + " public override void OnTick() { } }";
    private static final String SHARP_BROKEN = "using System.Machine.*; namespace Lamp; class Lamp {"
            + " static void Main() { Redstone.Named(\"Gate\").Out(15) } }";
    private static final String SHARP_LAMP = "using System.Machine.*; using System.IO.*; namespace Lamp; class Lamp {"
            + " static void Main() { Redstone.Named(\"Gate\").Out(15); Console.PrintLine(\"lit\"); } }";
    private static final String SHARP_REPORT = "using System.IO.*; using System.Network.*; namespace Report;"
            + " class Report { static void Main() {"
            + " File.Write(\"report.txt\", \"Logs: \" + Network.Total(\"oak_log\"));"
            + " Console.PrintLine(\"written\"); } }";
    private static final String SHARP_KEEP = "using System.IO.*; using System.Network.*; using System.Operations.*;"
            + " namespace Keep; class Keep { static void Main() { if (Network.Total(\"oak_planks\") < 8) {"
            + " Operations.Craft(\"oak_planks\", 8); } Console.PrintLine(\"asked\"); } }";
    private static final String SHARP_WATCH = "using System.*; using System.Machine.*; using System.Network.*;"
            + " using System.Execution.*; namespace Watch; class Watch : IScript { public void OnInit() {"
            + " Program.SetName(\"watch\");"
            + " Network.WatchAbove(\"cobblestone\", 10, e => Redstone.Named(\"Gate\").Out(15)); }"
            + " public void OnTick() { } public void OnDestroy() { } }";
    private static final String SHARP_WINDOW = "using System.*; using System.UI.*; namespace Panel; class Panel :"
            + " IScript { Window made; public void OnInit() { made = new Window(\"Plant Panel\", 200, 100);"
            + " made.Content = new Label(\"holding\"); made.Show(); } public void OnTick() { }"
            + " public void OnDestroy() { made.Close(); } }";
    private static final String SHARP_TUNE = "using System.IO.*; using System.Sound.*; namespace Tune; class Tune {"
            + " static void Main() { Sound.Tones(\"C4:250 E4 G4\"); Console.PrintLine(\"sung\"); } }";

    private EraProgrammingClientTests() {
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void vintage_aProgramIsWrittenCorrectedRunAndStopped(final ClientTestContext ctx) {
        final EraKit kit = EraKit.VINTAGE;
        bench(ctx, kit, false);
        prompt(ctx, "write progs\\lamp.sg " + SIGMA_BROKEN);
        prompt(ctx, "scc progs\\lamp.sg");
        ctx.thenWaitUntilServer(level -> errorOnGlass(computer(ctx, level)), WAIT,
                        "scc to name the mistake", level -> lastLines(computer(ctx, level)))
                .thenScreenshot(SETTLE, shot(kit, "compile-error"));
        prompt(ctx, "cls");
        prompt(ctx, "write progs\\lamp.sg " + SIGMA_LAMP);
        prompt(ctx, "scc progs\\lamp.sg");
        prompt(ctx, "sigma run progs\\lamp.asm");
        lampLit(ctx, kit);
        prompt(ctx, "write progs\\beat.sg " + SIGMA_BEAT);
        prompt(ctx, "scc progs\\beat.sg");
        prompt(ctx, "start sigma run progs\\beat.asm");
        ctx.thenWaitUntilServer(level -> !computer(ctx, level).programs().view().isEmpty(), WAIT,
                        "start to leave the script running", level -> lastLines(computer(ctx, level)));
        prompt(ctx, "tasklist");
        ctx.thenWaitUntilServer(level -> glass(computer(ctx, level)).contains("beat")
                                && !glass(computer(ctx, level)).contains("halted"), WAIT,
                        "tasklist to list the script, running", level -> lastLines(computer(ctx, level)))
                .thenScreenshot(SETTLE, shot(kit, "tasklist"));
        // The runtime stops a program by the number tasklist gives it.
        final AtomicInteger task = new AtomicInteger();
        ctx.thenServer(0, level -> task.set(new ServerCliComputer(computer(ctx, level), level).sigmaProcesses()
                        .getFirst().id()))
                .then(SETTLE, () -> {
                    ctx.type("sigma stop " + task.get());
                    ctx.key(GLFW.GLFW_KEY_ENTER);
                })
                .thenWaitUntilServer(level -> computer(ctx, level).programs().view().isEmpty(), WAIT,
                        "sigma stop to stop the script", level -> lastLines(computer(ctx, level)));
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void legacy_programsAreWrittenInEachEditorAndRun(final ClientTestContext ctx) {
        sharp(ctx, EraKit.LEGACY);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void transition_programsAreWrittenInEachEditorAndRun(final ClientTestContext ctx) {
        sharp(ctx, EraKit.TRANSITION);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void standard_programsAreWrittenInEachEditorAndRun(final ClientTestContext ctx) {
        sharp(ctx, EraKit.STANDARD);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void advanced_programsAreWrittenInEachEditorAndRun(final ClientTestContext ctx) {
        sharp(ctx, EraKit.ADVANCED);
    }

    /* The Σ# journey of a desktop era. */
    private static void sharp(final ClientTestContext ctx, final EraKit kit) {
        bench(ctx, kit, true);
        EraJourney.launch(ctx, kit, CODE)
                .thenWaitUntil(() -> code(ctx) != null, WAIT, "Virtual Studio Code to open from Start");
        written(ctx, "progs/lamp.sgs", SHARP_BROKEN);
        ctx.then(SETTLE, () -> code(ctx).runInTerminal("sgsc progs/lamp.sgs"))
                .thenWaitUntil(() -> code(ctx).terminalText().toLowerCase(Locale.ROOT).contains("error"), WAIT,
                        "sgsc to name the mistake")
                .thenScreenshot(SETTLE, shot(kit, "compile-error"))
                // The whole file taken and typed again, put right.
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_A, GLFW.GLFW_MOD_CONTROL))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_BACKSPACE))
                .then(1, () -> ctx.type(SHARP_LAMP))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_S, GLFW.GLFW_MOD_CONTROL))
                .thenWaitUntilServer(level -> onDisk(ctx, level, "progs/lamp.sgs").contains("PrintLine"), WAIT,
                        "the corrected program to be saved", level -> onDisk(ctx, level, "progs/lamp.sgs"));
        built(ctx, "lamp");
        lampLit(ctx, kit);
        ctx.thenServer(SETTLE, level -> sensor(ctx, level).emit(0, ""));
        // What the network holds, written into a file and printed.
        written(ctx, "progs/report.sgs", SHARP_REPORT);
        built(ctx, "report");
        ctx.thenWaitUntil(() -> code(ctx).terminalText().contains("written"), WAIT, "the report to be written")
                // Frames' prompt prints with print, as MS-DOS's did.
                .then(SETTLE, () -> code(ctx).runInTerminal("print report.txt"))
                .thenWaitUntilServer(level -> !printer(ctx, level).output().getStackInSlot(0).isEmpty(), WAIT * 3,
                        "the report to come out of the printer", level -> "jobs=" + printer(ctx, level).jobs().size()
                                + "; the prompt said " + lastLines(computer(ctx, level)));
        // The stock kept by asking the Crafting Computer for planks.
        written(ctx, "progs/keep.sgs", SHARP_KEEP);
        built(ctx, "keep");
        ctx.thenWaitUntilServer(level -> stored(ctx, level, Items.OAK_PLANKS) >= 8, WAIT * 3,
                "the planks the program asked for to be crafted into the network",
                level -> "planks=" + stored(ctx, level, Items.OAK_PLANKS));
        // A tune through the machine's sound.
        written(ctx, "progs/tune.sgs", SHARP_TUNE);
        built(ctx, "tune");
        ctx.thenWaitUntil(() -> code(ctx).terminalText().contains("sung"), WAIT, "the tune to be played");
        // A window of its own.
        written(ctx, "progs/panel.sgs", SHARP_WINDOW);
        // A script stays up in the background by itself, and the prompt says which number it started as.
        ctx.then(SETTLE, () -> code(ctx).runInTerminal("sgsc progs/panel.sgs"))
                .then(WAIT / 4, () -> code(ctx).runInTerminal("sigma run progs/panel.asm"))
                .thenWaitUntil(() -> ctx.screen(DesktopScreen.class).windowFor("Plant Panel") != null, WAIT,
                        "the program's own window to open on the desktop")
                .thenScreenshot(SETTLE, shot(kit, "own-window"));
        // The program stopped by its number, and its window goes with it, leaving the editor in front again.
        stopped(ctx, "panel");
        ctx.thenWaitUntilServer(level -> computer(ctx, level).programs().view().stream()
                        .noneMatch(one -> one.file().toLowerCase(Locale.ROOT).contains("panel")), WAIT,
                        "sigma stop to end the program", level -> "running: " + programs(computer(ctx, level)))
                .thenWaitUntil(() -> ctx.screen(DesktopScreen.class).windowFor("Plant Panel") == null, WAIT,
                        "the program's window to go with it", () -> "the terminal shows "
                                + tail(code(ctx).terminalText()));
        // A script that waits for the stone, listed by sigma ps and in the Task Manager, then stopped.
        written(ctx, "progs/watch.sgs", SHARP_WATCH);
        ctx.then(SETTLE, () -> code(ctx).runInTerminal("sgsc progs/watch.sgs"))
                .thenWaitUntil(() -> code(ctx).terminalText().contains("wrote progs/watch.asm"), WAIT,
                        "sgsc to build the script", () -> "the terminal shows " + tail(code(ctx).terminalText()))
                .then(SETTLE, () -> code(ctx).runInTerminal("sigma run progs/watch.asm"))
                .thenWaitUntilServer(level -> processes(ctx, level).contains("watch"), WAIT,
                        "the script to stay up", level -> "processes " + processes(ctx, level))
                .then(SETTLE, () -> code(ctx).runInTerminal("sigma ps"))
                .thenServer(SETTLE, level -> NetworkStorage.of(level, mainframe(ctx, level).networkUuid())
                        .insert(StorageKey.of(Items.COBBLESTONE), STONE))
                .thenWaitUntilServer(level -> level.getBlockState(ctx.abs(LAMP)).getValue(RedstoneLampBlock.LIT),
                        WAIT, "the script to hear the stone pass its mark and light the lamp",
                        level -> "strength=" + sensor(ctx, level).emitStrength() + " stone=" + stored(ctx, level,
                                Items.COBBLESTONE) + "; processes " + processes(ctx, level));
        ctx.then(SETTLE, () -> {
                    final int[] at = ctx.screen(DesktopScreen.class).emptyPanelPoint();
                    ctx.rightClick(at[0] + 0.5, at[1] + 0.5);
                })
                .thenWaitUntil(() -> ctx.screen(DesktopScreen.class).isPanelMenuOpen(), WAIT,
                        "the right button on the panel's empty stretch to open its menu")
                .then(SETTLE, () -> {
                    final int[] at = ctx.screen(DesktopScreen.class).panelMenuPoint("Task Manager");
                    ctx.click(at[0] + 0.5, at[1] + 0.5);
                })
                .thenWaitUntil(() -> DesktopSteps.app(ctx, TASK_MANAGER, TaskManagerApp.class) != null, WAIT,
                        "the Task Manager to open")
                // The older Task Manager opens on its applications, the windows; the processes are its second tab.
                .then(SETTLE, () -> {
                    final int[] processes = DesktopSteps.app(ctx, TASK_MANAGER, TaskManagerApp.class).pageCentre(1);
                    if (tabbed(kit) && processes != null) {
                        ctx.clickDesktop(processes);
                    }
                })
                .thenWaitUntil(() -> {
                    final TaskManagerApp tasks = DesktopSteps.app(ctx, TASK_MANAGER, TaskManagerApp.class);
                    return tasks != null && String.join(" ", tasks.rowLabels()).toLowerCase(Locale.ROOT)
                            .contains("watch");
                }, WAIT, "the Task Manager to list the script")
                .thenScreenshot(SETTLE, shot(kit, "task-manager"));
        stopped(ctx, "watch");
        ctx.thenWaitUntilServer(level -> computer(ctx, level).programs().view().stream()
                        .noneMatch(one -> (one.name() + one.file()).toLowerCase(Locale.ROOT).contains("watch")), WAIT,
                "sigma stop to stop the script", level -> "the terminal shows " + lastLines(computer(ctx, level)));
        // Virtual Studio: a project of its own, built and started.
        EraJourney.launch(ctx, kit, STUDIO)
                .thenWaitUntil(() -> studio(ctx) != null && studio(ctx).onStartWindow(), WAIT,
                        "Virtual Studio to open on its Start Window")
                .then(SETTLE, () -> ctx.clickDesktop(studio(ctx).startLinkCenter("Create a new project")))
                .thenWaitUntil(() -> studio(ctx).wizardOpen(), WAIT, "the New Project wizard to open")
                .then(SETTLE, () -> studio(ctx).chooseTemplate(CONSOLE))
                .then(SETTLE, () -> studio(ctx).createProject(CONSOLE, "Plant"))
                .thenWaitUntil(() -> "Plant".equals(studio(ctx).solutionName()), WAIT,
                        "the studio to open the solution it made")
                .then(SETTLE, () -> studio(ctx).buildSolution())
                .thenWaitUntil(() -> studio(ctx).outputLines().stream().anyMatch(l -> l.startsWith("Build succeeded")),
                        WAIT, "the build to succeed")
                .then(SETTLE, () -> studio(ctx).startProgram())
                .thenWaitUntil(() -> studio(ctx).terminalText().contains("Hello from Plant"), WAIT * 2,
                        "the started program to print its line")
                .thenScreenshot(SETTLE, shot(kit, "virtual-studio"));
    }

    /*
     * The era's network, with the lamp's interface, named Gate, and the printer beside the computer; the compilers and
     * the runtime on it, and from the Legacy the editors, and the Crafting Computer with the planks' pattern and logs
     * in the network. The player then sits at the computer, at MC-DOS's prompt or at the desktop.
     */
    private static void bench(final ClientTestContext ctx, final EraKit kit, final boolean sharp) {
        EraJourney.lay(ctx, kit).thenBuild(0, world -> {
            world.setBlock(INTERFACE, kit.redstoneInterface().defaultBlockState()
                    .setValue(RedstoneInterfaceBlock.FACING, Direction.SOUTH));
            world.setBlock(LAMP, Blocks.REDSTONE_LAMP);
            world.setBlock(PRINTER, kit.printer());
            if (sharp) {
                final CraftingComputerBlockEntity crafter = EraNetwork.craftingComputer(world, kit);
                crafter.loadPattern(CraftFiles.oakPlanks());
            }
        });
        ctx.thenWaitUntilServer(level -> sensor(ctx, level).live(), WAIT,
                        "the interface to link to the computer behind it", level -> "not linked")
                .thenServer(SETTLE, level -> {
                    ctx.assertTrue(sensor(ctx, level).rename(GATE), "the interface takes the name " + GATE);
                    printer(ctx, level).paper().setStackInSlot(0, new ItemStack(Items.PAPER, 16));
                    final PersonalComputerBlockEntity computer = computer(ctx, level);
                    for (final String program : sharp ? List.of("sgsc", "sigma", "virtual_studio_code",
                            "virtual_studio") : List.of("scc", "sigma")) {
                        computer.console().install(JsComputers.MODID + ":" + program);
                    }
                    if (sharp) {
                        NetworkStorage.of(level, mainframe(ctx, level).networkUuid())
                                .insert(StorageKey.of(Items.OAK_LOG), LOGS);
                    }
                })
                .thenTeleport(SETTLE, EraNetwork.AT_THE_DESK, Direction.WEST)
                .thenRightClick(SETTLE, EraNetwork.MONITOR)
                .thenAwaitScreen(EraJourney.systemScreen(kit), EraJourney.BOOT_WAIT);
    }

    /* Opens a file in Virtual Studio Code, types the program into it and saves it. */
    private static void written(final ClientTestContext ctx, final String path, final String source) {
        ctx.then(SETTLE, () -> code(ctx).openFile(path))
                .thenWaitUntil(() -> path.equals(code(ctx).openFile()), WAIT, "the editor to open " + path)
                .then(SETTLE, () -> ctx.type(source))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_S, GLFW.GLFW_MOD_CONTROL))
                .thenWaitUntilServer(level -> !onDisk(ctx, level, path).isEmpty(), WAIT, path + " to be saved",
                        level -> "");
    }

    /* Compiles the program at the editor's terminal, then runs it there. */
    private static void built(final ClientTestContext ctx, final String name) {
        ctx.then(SETTLE, () -> code(ctx).runInTerminal("sgsc progs/" + name + ".sgs"))
                .thenWaitUntil(() -> code(ctx).terminalText().contains(name + ".asm"), WAIT,
                        "sgsc to build " + name)
                .then(SETTLE, () -> code(ctx).runInTerminal("sigma run progs/" + name + ".asm"));
    }

    private static void lampLit(final ClientTestContext ctx, final EraKit kit) {
        ctx.thenWaitUntilServer(level -> level.getBlockState(ctx.abs(LAMP)).getValue(RedstoneLampBlock.LIT), WAIT,
                        "the program to light the lamp through the interface",
                        level -> "strength=" + sensor(ctx, level).emitStrength() + "; the prompt said "
                                + lastLines(computer(ctx, level)))
                .thenScreenshot(SETTLE, shot(kit, "lamp-lit"));
    }

    /* The program started from that file stopped at the editor's terminal by its number, as sigma ps lists it. */
    private static void stopped(final ClientTestContext ctx, final String file) {
        final AtomicInteger number = new AtomicInteger();
        ctx.thenServer(SETTLE, level -> {
                    for (final ICliComputer.SigmaProcess one
                            : new ServerCliComputer(computer(ctx, level), level).sigmaProcesses()) {
                        if (one.file().contains(file)) {
                            number.set(one.id());
                        }
                    }
                })
                .then(1, () -> code(ctx).runInTerminal("sigma stop " + number.get()));
    }

    /* Whether the era's Task Manager is the older one, with its tabs: Frames XP's and Frames 7's. */
    private static boolean tabbed(final EraKit kit) {
        return kit == EraKit.LEGACY || kit == EraKit.TRANSITION;
    }

    /* The machine's processes, each by its number, its file and what it is doing. */
    private static String processes(final ClientTestContext ctx, final ServerLevel level) {
        final StringBuilder out = new StringBuilder();
        for (final ICliComputer.SigmaProcess one
                : new ServerCliComputer(computer(ctx, level), level).sigmaProcesses()) {
            out.append('[').append(one.id()).append(' ').append(one.file()).append(' ').append(one.state())
                    .append(']');
        }
        return out.toString();
    }

    /* The programs the machine runs, each by its name, its file and its state. */
    private static String programs(final PersonalComputerBlockEntity computer) {
        final StringBuilder out = new StringBuilder();
        computer.programs().view().forEach(one -> out.append('[').append(one.name()).append(' ').append(one.file())
                .append(']'));
        return out.toString();
    }

    /* The last of a terminal's text, enough to read what it last said. */
    private static String tail(final String text) {
        return text.substring(Math.max(0, text.length() - 400));
    }

    /* Types a line at MC-DOS's prompt and sends it. */
    private static void prompt(final ClientTestContext ctx, final String line) {
        ctx.then(SETTLE, () -> {
            ctx.type(line);
            ctx.key(GLFW.GLFW_KEY_ENTER);
        });
    }

    private static boolean errorOnGlass(final PersonalComputerBlockEntity computer) {
        for (final WireLine line : computer.console().glass()) {
            for (final WireLine.Span span : line.spans()) {
                if (span.style() == CliStyle.ERROR.id()) {
                    return true;
                }
            }
        }
        return glass(computer).toLowerCase(Locale.ROOT).contains("error");
    }

    private static String glass(final PersonalComputerBlockEntity computer) {
        final StringBuilder out = new StringBuilder();
        for (final WireLine line : computer.console().glass()) {
            line.spans().forEach(span -> out.append(span.text().english()));
            out.append('\n');
        }
        return out.toString();
    }

    private static String lastLines(final PersonalComputerBlockEntity computer) {
        final String all = glass(computer);
        return all.substring(Math.max(0, all.length() - 300));
    }

    private static String onDisk(final ClientTestContext ctx, final ServerLevel level, final String path) {
        return DiskFilesystem.read(computer(ctx, level).systemDisk(), path).orElse("");
    }

    private static long stored(final ClientTestContext ctx, final ServerLevel level, final Item item) {
        return NetworkStorage.of(level, mainframe(ctx, level).networkUuid()).count(StorageKey.of(item));
    }

    private static VirtualStudioCodeApp code(final ClientTestContext ctx) {
        return DesktopSteps.app(ctx, CODE, VirtualStudioCodeApp.class);
    }

    private static VirtualStudioApp studio(final ClientTestContext ctx) {
        return DesktopSteps.app(ctx, STUDIO, VirtualStudioApp.class);
    }

    private static RedstoneInterfaceBlockEntity sensor(final ClientTestContext ctx, final ServerLevel level) {
        return TestWorldBuilder.at(level, ctx.origin()).blockEntity(INTERFACE, RedstoneInterfaceBlockEntity.class);
    }

    private static PrinterBlockEntity printer(final ClientTestContext ctx, final ServerLevel level) {
        return TestWorldBuilder.at(level, ctx.origin()).blockEntity(PRINTER, PrinterBlockEntity.class);
    }

    private static PersonalComputerBlockEntity computer(final ClientTestContext ctx, final ServerLevel level) {
        return TestWorldBuilder.at(level, ctx.origin()).blockEntity(EraNetwork.COMPUTER,
                PersonalComputerBlockEntity.class);
    }

    private static MainframeBlockEntity mainframe(final ClientTestContext ctx, final ServerLevel level) {
        return TestWorldBuilder.at(level, ctx.origin()).blockEntity(EraNetwork.MAINFRAME, MainframeBlockEntity.class);
    }

    private static String shot(final EraKit kit, final String what) {
        return kit.name().toLowerCase(Locale.ROOT) + "-" + what;
    }
}
