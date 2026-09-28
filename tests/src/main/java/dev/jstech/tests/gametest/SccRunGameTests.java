/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.HardwareItems;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.machine.ProgramView;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.CliCommands;
import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.cli.CliShell;
import dev.jstech.core.text.Text;
import dev.jstech.tests.JsTests;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * A Vintage machine runs what {@code scc} compiled directly, by its own name at the prompt, exactly as a program
 * of its own day was run: no runtime is ever installed beside it, because the Sigma Runtime stays Legacy and up
 * and a Vintage board can never take it.
 *
 * <p>MC-DOS finds the listing by its name alone, current directory first and then the PATH; the family met at a
 * Unix prompt asks for the current directory by name ({@code ./hello}), and a bare name only from the PATH, as
 * the real shell always has. Either way, a listing built for another architecture is refused in the same words
 * the installed runtime already refuses one in.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SccRunGameTests {

    private static final String ARENA = "empty";

    private static final BlockPos WHERE = new BlockPos(2, 2, 2);

    private static final int SETTLE = 4;

    /** Past the end of a hard drive's spin-up on a machine just switched on, when it is heard turning. */
    private static final int SPUN_UP = 200;

    private static final ResourceLocation MC_DOS = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "mc_dos");
    private static final ResourceLocation UNIX = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "unix");
    private static final ResourceLocation SCC = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "scc");

    /** A program the smaller language holds a source for, in Σ's own library. */
    private static final String WATCH_SOURCE = """
            using Standard.*;
            namespace Programs;
            class Watch : Script {
                public override void OnTick() { Console.PrintLine("hello"); }
            }
            """;

    /** A plain console program: no service, no loop, just what it prints before it returns. */
    private static final String HELLO_SOURCE = """
            using Standard.*;
            namespace Programs;
            class Hello {
                static void Main() {
                    Console.PrintLine("Hello from the base.");
                }
            }
            """;

    private SccRunGameTests() {
    }

    /** MC-DOS finds the listing by its own name, current directory first, and starts it with no runtime at all. */
    @GameTest(template = ARENA)
    public static void mcDos_runsAListingByItsBareNameFromTheCurrentDirectory(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = vintage(helper, WHERE, MC_DOS);
        if (computer == null) {
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ServerCliComputer cli = new ServerCliComputer(computer, helper.getLevel());
                    /*
                     * The name a file is stored under is the name it was written under, on every system alike; a
                     * player who typed "scc hello.sg" gets "hello.asm" back, in the same case, and runs it the
                     * same way. This is exactly that case, spelled by hand instead of through a real compile.
                     */
                    helper.assertTrue(cli.writeFile("hello.asm", listing("jsc:x86_16", "Hello")).ok(),
                            "the listing is on the disk");
                    final CliShell shell = CliCommands.shellFor(cli, 52);
                    final CliShell.Response response = shell.run("hello", cli);
                    final String said = text(response);
                    helper.assertFalse(said.contains("not found"), "the bare name finds it; got " + said);
                    helper.assertTrue(computer.programs().held() > 0, "the terminal holds what it started");
                    /*
                     * A program that takes the terminal has nothing of its own to say, so it prints no line at
                     * all, not an empty one that would sit under the prompt for no reason.
                     */
                    helper.assertTrue(response.lines().isEmpty(), "no blank line under the prompt; got " + said);
                })
                .thenSucceed();
    }

    /** A name no command and no file answers to is an unknown command. */
    @GameTest(template = ARENA)
    public static void mcDos_aNameNothingAnswersTo_isAnUnknownCommand(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = vintage(helper, WHERE, MC_DOS);
        if (computer == null) {
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ServerCliComputer cli = new ServerCliComputer(computer, helper.getLevel());
                    final CliShell shell = CliCommands.shellFor(cli, 52);
                    final String said = text(shell.run("nosuchthing", cli));
                    helper.assertTrue(said.contains("command not found: nosuchthing"), "got " + said);
                })
                .thenSucceed();
    }

    /**
     * The family met at a Unix prompt runs {@code ./hello} exactly where it is written, and a bare {@code hello}
     * beside it in the same folder is not found, since the current directory is never searched by itself.
     */
    @GameTest(template = ARENA)
    public static void unix_runsAnExplicitPathButNeverABareNameBesideIt(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = vintage(helper, WHERE, UNIX);
        if (computer == null) {
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ServerCliComputer cli = new ServerCliComputer(computer, helper.getLevel());
                    helper.assertTrue(cli.writeFile("hello.asm", listing("jsc:x86_16", "Hello")).ok(),
                            "the listing is on the disk");
                    final CliShell shell = CliCommands.shellFor(cli, 52);
                    final String explicit = text(shell.run("./hello", cli));
                    helper.assertFalse(explicit.contains("not found"), "./hello finds it; got " + explicit);
                    helper.assertTrue(computer.programs().held() > 0, "the terminal holds what it started");

                    final ServerCliComputer bare = new ServerCliComputer(computer, helper.getLevel());
                    final CliShell bareShell = CliCommands.shellFor(bare, 52);
                    final String said = text(bareShell.run("hello", bare));
                    helper.assertTrue(said.contains("hello: not found"),
                            "a bare name beside the prompt is not found; got " + said);
                })
                .thenSucceed();
    }

    /** {@code scc} writes the listing, and the bare name it teaches runs it. */
    @GameTest(template = ARENA)
    public static void mcDos_sccCompilesThenTheBareNameRunsWhatItWrote(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = vintage(helper, WHERE, MC_DOS);
        if (computer == null) {
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    computer.console().install(SCC.toString());
                    final ServerCliComputer cli = new ServerCliComputer(computer, helper.getLevel());
                    helper.assertTrue(cli.writeFile("WATCH.SG", WATCH_SOURCE).ok(), "the source is on the disk");
                    final CliShell shell = CliCommands.shellFor(cli, 52);
                    final String compiled = text(shell.run("scc WATCH.SG", cli));
                    helper.assertTrue(compiled.contains("run it with: WATCH"), "it teaches the bare name; got "
                            + compiled);
                    final String ran = text(shell.run("WATCH", cli));
                    helper.assertFalse(ran.contains("not found"), "the bare name finds what scc wrote; got " + ran);
                    /*
                     * A script such as this one stays up on its own (it is a service, not something that takes the
                     * terminal), so a real start is confirmed among the machine's running programs, which a failed
                     * one (no room, cannot run) would never join, rather than under held().
                     */
                    final List<ProgramView> running = computer.programs().view();
                    helper.assertTrue(running.size() == 1, "the machine runs what it started; got " + running);
                })
                .thenSucceed();
    }

    /** The central flow end to end: scc compiles a console program, and its bare name runs it and prints. */
    @GameTest(template = ARENA)
    public static void mcDos_sccCompilesAConsoleProgram_theBareNamePrintsWhatItWrote(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = vintage(helper, WHERE, MC_DOS);
        if (computer == null) {
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    computer.console().install(SCC.toString());
                    final ServerCliComputer cli = new ServerCliComputer(computer, helper.getLevel());
                    helper.assertTrue(cli.writeFile("HELLO.SG", HELLO_SOURCE).ok(), "the source is on the disk");
                    final CliShell shell = CliCommands.shellFor(cli, 52);
                    final String compiled = text(shell.run("scc HELLO.SG", cli));
                    helper.assertTrue(compiled.contains("run it with: HELLO"), "it teaches the bare name; got "
                            + compiled);
                    final String ran = text(shell.run("hello", cli));
                    helper.assertFalse(ran.contains("not found"), "the bare name finds what scc wrote; got " + ran);
                    computer.programs().tick(512);
                    final List<String> unseen = computer.programs().unseen().stream().map(Text::english).toList();
                    helper.assertTrue(unseen.contains("Hello from the base."),
                            "the console holds what the program printed; got " + unseen);
                })
                .thenSucceed();
    }

    /** A listing put in a really-installed program's own directory is found there through the real PATH. */
    @GameTest(template = ARENA)
    public static void mcDos_findsAListingOnThePathOfAReallyInstalledProgram(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = vintage(helper, WHERE, MC_DOS);
        if (computer == null) {
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    // Installing scc puts a real C:\SCC directory on the disk and on the machine's own PATH.
                    computer.console().install(SCC.toString());
                    final ServerCliComputer cli = new ServerCliComputer(computer, helper.getLevel());
                    helper.assertTrue(cli.writeFile("SCC\\HELLO.ASM", listing("jsc:x86_16", "Hello")).ok(),
                            "the listing is in the installed program's own directory");
                    final CliShell shell = CliCommands.shellFor(cli, 52);
                    final String said = text(shell.run("hello", cli));
                    helper.assertFalse(said.contains("not found"), "the real PATH finds it; got " + said);
                })
                .thenSucceed();
    }

    /** A listing built for another architecture is refused in the runtime's own existing words. */
    @GameTest(template = ARENA)
    public static void mcDos_aListingOfAnotherArchitecture_getsTheRuntimesExistingError(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = vintage(helper, WHERE, MC_DOS);
        if (computer == null) {
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ServerCliComputer cli = new ServerCliComputer(computer, helper.getLevel());
                    helper.assertTrue(cli.writeFile("clock.asm", listing("jsc:x86", "Clock")).ok(),
                            "the listing is on the disk");
                    final CliShell shell = CliCommands.shellFor(cli, 52);
                    final String said = text(shell.run("clock", cli));
                    helper.assertTrue(said.contains("A4015"), "the refusal is named; got " + said);
                    helper.assertTrue(said.contains("built for x86; this machine is x86-16"),
                            "it names both architectures; got " + said);
                })
                .thenSucceed();
    }

    /** scc teaches the way a Vintage machine really runs what it wrote, never the runtime it can never hold. */
    @GameTest(template = ARENA)
    public static void scc_onAVintageMcDosMachine_teachesRunningItByName(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = vintage(helper, WHERE, MC_DOS);
        if (computer == null) {
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    computer.console().install(SCC.toString());
                    final ServerCliComputer cli = new ServerCliComputer(computer, helper.getLevel());
                    helper.assertTrue(cli.writeFile("WATCH.SG", WATCH_SOURCE).ok(), "the source is on the disk");
                    final CliShell shell = CliCommands.shellFor(cli, 52);
                    final String said = text(shell.run("scc WATCH.SG", cli));
                    helper.assertTrue(said.contains("run it with: WATCH"), "it teaches the bare name; got " + said);
                    helper.assertFalse(said.contains("sigma run"),
                            "never the runtime it can never hold; got " + said);
                })
                .thenSucceed();
    }

    /** The same machine met at a Unix prompt is taught its own way instead: the current directory spelled out. */
    @GameTest(template = ARENA)
    public static void scc_onAVintageUnixMachine_teachesRunningItFromTheCurrentDirectory(
            final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = vintage(helper, WHERE, UNIX);
        if (computer == null) {
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    computer.console().install(SCC.toString());
                    final ServerCliComputer cli = new ServerCliComputer(computer, helper.getLevel());
                    helper.assertTrue(cli.writeFile("watch.sg", WATCH_SOURCE).ok(), "the source is on the disk");
                    final CliShell shell = CliCommands.shellFor(cli, 52);
                    final String said = text(shell.run("scc watch.sg", cli));
                    helper.assertTrue(said.contains("run it with: ./watch"), "it teaches ./watch; got " + said);
                    helper.assertFalse(said.contains("sigma run"),
                            "never the runtime it can never hold; got " + said);
                })
                .thenSucceed();
    }

    /** A bare name with no path in front of it is found on the well-known directory the default PATH searches. */
    @GameTest(template = ARENA)
    public static void unix_findsABareNameOnItsDefaultPath(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = vintage(helper, WHERE, UNIX);
        if (computer == null) {
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ServerCliComputer cli = new ServerCliComputer(computer, helper.getLevel());
                    helper.assertTrue(cli.writeFile("/usr/bin/hello.asm", listing("jsc:x86_16", "Hello")).ok(),
                            "the listing is in a directory the default PATH searches");
                    final CliShell shell = CliCommands.shellFor(cli, 52);
                    final String said = text(shell.run("hello", cli));
                    helper.assertFalse(said.contains("not found"), "the default PATH finds it; got " + said);
                })
                .thenSucceed();
    }

    /**
     * With a listing of the same name both beside the prompt and in a really-installed program's own directory,
     * the one beside the prompt is the one a real machine runs, exactly as the current directory always wins.
     */
    @GameTest(template = ARENA)
    public static void mcDos_currentDirectoryWinsOverAReallyInstalledProgramsPath(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = vintage(helper, WHERE, MC_DOS);
        if (computer == null) {
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    // Installing scc puts a real C:\SCC directory on the disk and on the machine's own PATH.
                    computer.console().install(SCC.toString());
                    final ServerCliComputer cli = new ServerCliComputer(computer, helper.getLevel());
                    helper.assertTrue(cli.writeFile("hello.asm", listing("jsc:x86_16", "Hello")).ok(),
                            "the current directory's own copy is built for this machine");
                    helper.assertTrue(cli.writeFile("SCC\\HELLO.ASM", listing("jsc:x86", "Hello")).ok(),
                            "the PATH's copy is built for a different architecture on purpose");
                    final CliShell shell = CliCommands.shellFor(cli, 52);
                    final String said = text(shell.run("hello", cli));
                    helper.assertFalse(said.contains("A4015") || said.contains("not found"),
                            "the current directory's copy ran, not the PATH's wrong-architecture one; got " + said);
                })
                .thenSucceed();
    }

    /** What {@code %PATH%} expands to is exactly the PATH line {@code AUTOEXEC.BAT} itself carries. */
    @GameTest(template = ARENA)
    public static void mcDos_pathVariable_matchesAutoexecsOwnPathLine(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = vintage(helper, WHERE, MC_DOS);
        if (computer == null) {
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    computer.console().install(SCC.toString());
                    final ServerCliComputer cli = new ServerCliComputer(computer, helper.getLevel());
                    final CliShell shell = CliCommands.shellFor(cli, 52);
                    final String path = text(shell.run("echo %PATH%", cli)).strip();
                    final String autoexec = text(shell.run("type AUTOEXEC.BAT", cli));
                    String fromFile = null;
                    for (final String line : autoexec.split("\n")) {
                        if (line.regionMatches(true, 0, "PATH ", 0, "PATH ".length())) {
                            fromFile = line.substring("PATH ".length()).trim();
                        }
                    }
                    helper.assertTrue(fromFile != null, "AUTOEXEC.BAT carries a PATH line; got " + autoexec);
                    helper.assertTrue(path.equals(fromFile),
                            "echo %PATH% agrees with AUTOEXEC.BAT's own PATH line; got " + path + " vs " + fromFile);
                })
                .thenSucceed();
    }

    /** A line with nothing in it that ever stands for {@code %PATH%} never seeks a hard drive to build one. */
    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void mcDos_aPlainCommand_neverSeeksTheDiskForAPathNothingAsksFor(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = vintage(helper, WHERE, MC_DOS);
        if (computer == null) {
            return;
        }
        computer.togglePower();
        helper.startSequence()
                .thenExecuteAfter(SPUN_UP, () -> {
                    helper.assertTrue(turning(helper, computer), "the drive has spun up");
                    helper.assertFalse(seeking(helper, computer),
                            "the drive settled after coming on, before the command is typed");
                })
                .thenExecute(() -> {
                    final ServerCliComputer cli = new ServerCliComputer(computer, helper.getLevel());
                    final CliShell shell = CliCommands.shellFor(cli, 52);
                    shell.run("ver", cli);
                })
                .thenExecuteAfter(2, () -> helper.assertFalse(seeking(helper, computer),
                        "a line with no %PATH% on it never touches the disk"))
                .thenExecute(() -> {
                    /*
                     * A positive control: a real disk read on this same machine is heard seeking, so a
                     * regression that made every line silent could never pass the assertion above by accident.
                     */
                    final ServerCliComputer cli = new ServerCliComputer(computer, helper.getLevel());
                    final CliShell shell = CliCommands.shellFor(cli, 52);
                    shell.run("type AUTOEXEC.BAT", cli);
                })
                .thenExecuteAfter(1, () -> helper.assertTrue(seeking(helper, computer),
                        "a real disk read is heard seeking"))
                .thenSucceed();
    }

    /** A listing that does nothing, built for that architecture. */
    private static String listing(final String architecture, final String className) {
        return ".asm 3\n.arch " + architecture + "\n.start Programs." + className + " console\n\n.class Programs."
                + className + "\n\n.method static void Main() slots 0\n    ret\n";
    }

    private static String text(final CliShell.Response response) {
        final StringBuilder out = new StringBuilder();
        for (final CliLine line : response.lines()) {
            out.append(line.text()).append('\n');
        }
        return out.toString();
    }

    /** Whether the client is told the machine's hard drive is seeking, read from what it is sent. */
    private static boolean seeking(final GameTestHelper helper, final PersonalComputerBlockEntity computer) {
        return computer.getUpdateTag(helper.getLevel().registryAccess()).getBoolean("DiskSeeking");
    }

    /** Whether the client is told the machine's hard drive is turning at all, read from what it is sent. */
    private static boolean turning(final GameTestHelper helper, final PersonalComputerBlockEntity computer) {
        return computer.getUpdateTag(helper.getLevel().registryAccess()).getBoolean("DiskTurning");
    }

    private static PersonalComputerBlockEntity vintage(final GameTestHelper helper, final BlockPos at,
                                                        final ResourceLocation os) {
        helper.setBlock(at, ComputingModule.VINTAGE_PERSONAL_COMPUTER.get());
        if (!(helper.getBlockEntity(at) instanceof PersonalComputerBlockEntity computer)) {
            helper.fail("no vintage personal computer at " + at);
            return null;
        }
        final ItemStackHandler hardware = computer.getHardware();
        hardware.setStackInSlot(PersonalComputerBlockEntity.MOTHERBOARD_SLOT,
                new ItemStack(HardwareItems.MOTHERBOARD_BABYAT_VINTAGE.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.CPU_SLOT,
                new ItemStack(HardwareItems.CPU_INTEGRA_486SX.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.RAM_SLOTS_START,
                new ItemStack(HardwareItems.RAM_SIMM_4.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.PSU_SLOT, new ItemStack(HardwareItems.PSU_300B.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.DISK_SLOTS_START,
                new ItemStack(ComputingModule.disk(StorageTier.HDD, DiskSize.GB_500)));
        if (!computer.installOs(os)) {
            helper.fail("the system did not install on " + at);
            return null;
        }
        return computer;
    }
}
