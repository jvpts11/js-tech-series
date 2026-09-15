/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.machine.MachinePrograms;
import dev.jstech.computers.machine.SigmaLanguage;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.core.JsCore;
import dev.jstech.core.language.ILanguageProcess;
import dev.jstech.core.language.IProgrammingLanguage;
import dev.jstech.core.language.LanguageRegistry;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import dev.jstech.tests.testkit.ToyLanguage;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Core's language API, exercised by a language that is not ours.
 *
 * <p>The machines resolve a program through the registry and never name a language, which is what lets
 * an addon add one. With one language shipped, nothing would prove that any more, so the game-test server
 * registers a toy language while the game loads, and it is put through the whole of it here: compiled on the
 * way in, given a share of the tick, listed by what it calls itself, written down and read back. The
 * registry's own rules are checked on registries of their own, because the game's is closed by the time a test
 * runs.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class LanguageApiGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;
    private static final ResourceLocation OTHER = ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "other");

    private LanguageApiGameTests() {
    }

    /** A program in the toy language: one line of output a step, for as many steps as it says. */
    private static final String SOURCE = "count 3";

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void languageRegistry_runsALanguageTheMachinesKnowNothingAbout(final GameTestHelper helper) {
        final BlockPos at = new BlockPos(2, 2, 2);
        final PersonalComputerBlockEntity computer =
                TestWorldBuilder.at(helper.getLevel(), helper.absolutePos(BlockPos.ZERO))
                        .placeRunningPersonalComputer(at);
        final MachinePrograms.Started started =
                computer.programs().start("count.toy", SOURCE, 1, computer);
        helper.assertTrue(started.ok(), "the machine runs it without knowing the language: " + started.message());
        final var one = computer.programs().byId(started.id());
        helper.assertTrue("counter".equals(one.name()),
                "and lists it by the name the program gave itself; got " + one.name());

        computer.programs().tick(4096);
        helper.assertTrue(one.process().console().equals(List.of("1", "2", "3")),
                "it got its share of the tick and printed; console " + one.process().console());
        helper.assertTrue(one.process().state() == ILanguageProcess.State.FINISHED,
                "and finished; state " + one.process().state());

        /*
         * Written down and read back: the machine hands the language its own words and gets a
         * program that carries on, which is the half an addon cannot do without.
         */
        final CompoundTag saved = new CompoundTag();
        one.process().save(saved);
        final ILanguageProcess again = new ToyLanguage().restore(SOURCE, saved, computer);
        helper.assertTrue(again != null && again.spent() == 3,
                "a program comes back where it was; got " + (again == null ? "nothing" : again.spent()));
        helper.succeed();
    }

    /**
     * A machine whose terminal was holding a program that does not come back after a load, because nothing installed
     * runs its kind of file any more, lets the terminal go. It used to keep pointing at the missing program, so every
     * line typed at the prompt went nowhere until Ctrl+C.
     */
    @GameTest(template = ARENA)
    public static void load_letsTheTerminalGoWhenItsProgramDoesNotComeBack(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer =
                TestWorldBuilder.at(helper.getLevel(), helper.absolutePos(BlockPos.ZERO))
                        .placeRunningPersonalComputer(new BlockPos(2, 2, 2));
        final MachinePrograms.Started started =
                computer.programs().start("count.toy", "count 100000", 1, computer);
        helper.assertTrue(started.ok(), "the program starts: " + started.message());
        computer.programs().hold(started.id());
        final var registries = helper.getLevel().registryAccess();
        final CompoundTag saved = computer.saveWithFullMetadata(registries);
        /*
         * The registry is closed while the game runs, so the language cannot be taken away; the save names a kind
         * of file nothing runs instead, which is what a machine finds when the language that ran a program is gone.
         */
        saved.getCompound("Σ#").getList("programs", Tag.TAG_COMPOUND).getCompound(0).putString("name", "count.gone");
        computer.loadWithComponents(saved, registries);
        helper.assertTrue(computer.programs().byId(started.id()) == null,
                "the program does not come back without its language");
        helper.assertTrue(computer.programs().held() == 0,
                "and the terminal holds nothing; got " + computer.programs().held());
        helper.assertTrue(new ServerCliComputer(computer, helper.getLevel()).foreground() == null,
                "so the prompt takes what is typed again");
        helper.succeed();
    }

    /** A Σ# program that starts a toy program from the disk and waits for it to end. */
    private static final String PATIENT = """
            using System.*;
            using System.IO.*;
            using System.Collections.*;
            using System.Execution.*;
            namespace Programs;
            class Patient {
                static void Main() {
                    Process p = Program.Start("C:\\\\count.toy", new List<string>());
                    Console.PrintLine("started " + p.Name);
                    p.Wait();
                    Console.PrintLine("code " + p.ExitCode);
                }
            }
            """;

    /**
     * A program waiting on one in a language that is not the machine's own is woken when that one ends, although
     * such a language has no way to say so itself.
     */
    @GameTest(template = ARENA)
    public static void wait_wakesWhenAProgramInAnotherLanguageEnds(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer =
                TestWorldBuilder.at(helper.getLevel(), helper.absolutePos(BlockPos.ZERO))
                        .placeRunningPersonalComputer(new BlockPos(2, 2, 2));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ServerCliComputer shell = new ServerCliComputer(computer, helper.getLevel());
                    helper.assertTrue(shell.writeFile("C:\\count.toy", SOURCE).ok(),
                            "the toy program is on the disk, whatever the machine makes of its kind");
                    final MachinePrograms.Started started =
                            computer.programs().start("patient.sgs", PATIENT, 1, computer);
                    helper.assertTrue(started.ok(), "the patient starts: " + started.message());
                    final ILanguageProcess patient = computer.programs().byId(started.id()).process();
                    for (int i = 0; i < 6 && patient.console().size() < 2; i++) {
                        computer.programs().tick(4096);
                    }
                    helper.assertTrue(patient.console().equals(List.of("started count.toy", "code 0")),
                            "the patient is woken when the toy program ends; got " + patient.console() + " ("
                                    + patient.message() + ")");
                })
                .thenSucceed();
    }

    /** A Σ# program that says one line. */
    private static final String HELLO = """
            using System.*;
            namespace Programs;
            class Hello {
                static void Main() {
                    Console.PrintLine("hello");
                }
            }
            """;

    /**
     * A listing belongs to the machine: no language claims {@code .asm} and Σ# runs nothing itself, yet the machine
     * runs a listing, and a Σ# source file it compiles on the way in.
     */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void machine_runsListingsNoLanguageClaims(final GameTestHelper helper) {
        helper.assertTrue(JsCore.languages().isReserved("asm") && JsCore.languages().byExtension("asm") == null,
                "no language claims .asm");
        helper.assertTrue(SigmaLanguage.INSTANCE.binaryExtensions().isEmpty()
                && JsCore.languages().runnerOf("sgs") == null, "and Σ# runs nothing itself");
        final PersonalComputerBlockEntity computer =
                TestWorldBuilder.at(helper.getLevel(), helper.absolutePos(BlockPos.ZERO))
                        .placeRunningPersonalComputer(new BlockPos(2, 2, 2));
        final IProgrammingLanguage.CompileResult built = SigmaLanguage.INSTANCE.compile(
                List.of(new IProgrammingLanguage.SourceText("hello.sgs", HELLO)));
        helper.assertTrue(built.ok(), "the program compiles: " + built.complaints());
        final MachinePrograms.Started listing = computer.programs().start("hello.asm", built.binary(), 1, computer);
        final MachinePrograms.Started source = computer.programs().start("hello.sgs", HELLO, 1, computer);
        helper.assertTrue(listing.ok() && source.ok(),
                "the machine runs the listing and the source: " + listing.message() + " / " + source.message());
        computer.programs().tick(4096);
        helper.assertTrue(computer.programs().byId(listing.id()).process().console().equals(List.of("hello"))
                        && computer.programs().byId(source.id()).process().console().equals(List.of("hello")),
                "and both say hello");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void languageRegistry_keepsAnExtensionBackForTheMachines(final GameTestHelper helper) {
        final LanguageRegistry registry = new LanguageRegistry();
        helper.assertTrue(registry.reserve("asm"), "the machines keep .asm back");
        helper.assertTrue(!registry.register(new ToyLanguage(OTHER, Set.of("other"), Set.of("asm"))),
                "a language claiming it is refused");
        helper.assertTrue(registry.get(OTHER) == null && registry.byExtension("other") == null, "and is not there");
        helper.assertTrue(registry.register(new ToyLanguage()), "a language that leaves it alone is taken");
        helper.assertTrue(!registry.reserve("toyb"), "an extension a language already has cannot be kept back");
        helper.assertTrue(registry.sourceOf("toy") != null && registry.sourceOf("toyb") == null,
                "and the files written in a language are told apart from the ones it only runs");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void languageRegistry_refusesAnExtensionAnotherLanguageHas(final GameTestHelper helper) {
        final LanguageRegistry registry = new LanguageRegistry();
        helper.assertTrue(registry.register(new ToyLanguage()), "the first language is taken");
        helper.assertTrue(!registry.register(new ToyLanguage(OTHER, Set.of("other"), Set.of("toyb"))),
                "a second one claiming .toyb is refused");
        helper.assertTrue(registry.get(OTHER) == null, "and is not there");
        helper.assertTrue(registry.byExtension("other") == null, "nor is any extension of its own");
        final var runner = registry.runnerOf("toyb");
        helper.assertTrue(runner != null && ToyLanguage.ID.equals(runner.id()), "the first one still runs .toyb");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void languageRegistry_replacesALanguageRegisteredAgainUnderItsId(final GameTestHelper helper) {
        final LanguageRegistry registry = new LanguageRegistry();
        registry.register(new ToyLanguage());
        final ToyLanguage better = new ToyLanguage(ToyLanguage.ID, Set.of("toy"), Set.of("toy", "toyc"));
        helper.assertTrue(registry.register(better), "the same id comes in again");
        helper.assertTrue(registry.all().size() == 1 && registry.get(ToyLanguage.ID) == better,
                "in place of the one before");
        helper.assertTrue(registry.runnerOf("toyb") == null && registry.runnerOf("toyc") == better,
                "with its own extensions and none of the old one's");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void languageRegistry_takesNoChangeOnceClosed(final GameTestHelper helper) {
        final LanguageRegistry registry = new LanguageRegistry();
        registry.register(new ToyLanguage());
        registry.freeze();
        helper.assertTrue(!registry.register(new ToyLanguage(OTHER, Set.of("other"), Set.of("other"))),
                "nothing is added once it is closed");
        helper.assertTrue(!registry.unregister(ToyLanguage.ID), "and nothing is taken away");
        helper.assertTrue(registry.get(ToyLanguage.ID) != null, "what was there stays");
        helper.assertTrue(JsCore.languages().isFrozen(), "the game's own registry is closed once every mod has loaded");
        helper.assertTrue(JsCore.languages().runnerOf("toy") != null,
                "and holds the toy language the game-test server gave it while loading");
        helper.succeed();
    }
}
