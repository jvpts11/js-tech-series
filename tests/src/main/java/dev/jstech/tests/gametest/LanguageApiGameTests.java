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
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.core.JsCore;
import dev.jstech.core.language.ILanguageProcess;
import dev.jstech.core.language.IProgrammingLanguage;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Core's language API, exercised by a language that is not ours.
 *
 * <p>The machines resolve a program through the registry and never name a language, which is what lets
 * an addon add one. With one language shipped, nothing would prove that any more, so a toy language is
 * registered here and put through the whole of it: compiled on the way in, given a share of the tick,
 * listed by what it calls itself, written down and read back. What it does is deliberately unlike
 * Σ#, so an assumption about Σ# leaking into the machines shows up as a failure here.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class LanguageApiGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;
    private static final ResourceLocation TOY = ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "toy");

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
        JsCore.languages().register(new ToyLanguage());
        try {
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
        } finally {
            JsCore.languages().unregister(TOY);
        }
    }

    /**
     * A machine whose terminal was holding a program that does not come back after a load, because its language is
     * gone, lets the terminal go. It used to keep pointing at the missing program, so every line typed at the prompt
     * went nowhere until Ctrl+C.
     */
    @GameTest(template = ARENA)
    public static void load_letsTheTerminalGoWhenItsProgramDoesNotComeBack(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer =
                TestWorldBuilder.at(helper.getLevel(), helper.absolutePos(BlockPos.ZERO))
                        .placeRunningPersonalComputer(new BlockPos(2, 2, 2));
        JsCore.languages().register(new ToyLanguage());
        try {
            final MachinePrograms.Started started =
                    computer.programs().start("count.toy", "count 100000", 1, computer);
            helper.assertTrue(started.ok(), "the program starts: " + started.message());
            computer.programs().hold(started.id());
            final var registries = helper.getLevel().registryAccess();
            final CompoundTag saved = computer.saveWithFullMetadata(registries);
            JsCore.languages().unregister(TOY);
            computer.loadWithComponents(saved, registries);
            helper.assertTrue(computer.programs().byId(started.id()) == null,
                    "the program does not come back without its language");
            helper.assertTrue(computer.programs().held() == 0,
                    "and the terminal holds nothing; got " + computer.programs().held());
            helper.assertTrue(new ServerCliComputer(computer, helper.getLevel()).foreground() == null,
                    "so the prompt takes what is typed again");
            helper.succeed();
        } finally {
            JsCore.languages().unregister(TOY);
        }
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
                    JsCore.languages().register(new ToyLanguage());
                    try {
                        /*
                         * The shell writes only the kinds of file the machines know, so the toy program goes on
                         * the disk directly; starting a program reads it back under any name.
                         */
                        DiskFilesystem.write(computer.systemDisk(), "count.toy", FileType.TXT, SOURCE,
                                Long.MAX_VALUE, FilesystemKind.HIERARCHICAL);
                        final ServerCliComputer shell = new ServerCliComputer(computer, helper.getLevel());
                        helper.assertTrue(shell.readFile("C:\\count.toy").ok(), "the toy program is on the disk");
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
                    } finally {
                        JsCore.languages().unregister(TOY);
                    }
                })
                .thenSucceed();
    }

    /** A language of two files and no ceremony: source counts, compiled text counts louder. */
    private static final class ToyLanguage implements IProgrammingLanguage {

        @Override
        public ResourceLocation id() {
            return TOY;
        }

        @Override
        public String displayName() {
            return "Toy";
        }

        @Override
        public Set<String> sourceExtensions() {
            return Set.of("toy");
        }

        @Override
        public Set<String> binaryExtensions() {
            return Set.of("toy", "toyb");
        }

        @Override
        public CompileResult compile(final List<SourceText> sources) {
            if (sources.isEmpty() || !sources.getFirst().text().startsWith("count ")) {
                return CompileResult.failed(List.of(new Complaint(
                        sources.isEmpty() ? "" : sources.getFirst().name(), 1, 1, "T001", "a toy program counts")));
            }
            return CompileResult.of(sources.getFirst().text());
        }

        @Override
        public List<Token> tokenize(final String text) {
            return List.of(new Token(1, 1, text.length(), Kind.NUMBER));
        }

        @Override
        public ILanguageProcess start(final String binary, final long heapBytes, final BlockEntity machine) {
            return new ToyProcess(howMany(binary), 0);
        }

        @Override
        public ILanguageProcess restore(final String binary, final CompoundTag saved, final BlockEntity machine) {
            return new ToyProcess(howMany(binary), saved.getInt("Counted"));
        }

        private static int howMany(final String binary) {
            try {
                return Integer.parseInt(binary.substring("count ".length()).trim());
            } catch (final NumberFormatException | IndexOutOfBoundsException notANumber) {
                return 0;
            }
        }
    }

    private static final class ToyProcess implements ILanguageProcess {

        private final int wanted;
        private final List<String> console = new ArrayList<>();
        private int counted;

        ToyProcess(final int wanted, final int counted) {
            this.wanted = wanted;
            this.counted = counted;
        }

        @Override
        public int step(final int budget) {
            int used = 0;
            while (used < budget && this.counted < this.wanted) {
                this.counted++;
                this.console.add(Integer.toString(this.counted));
                used++;
            }
            return used;
        }

        @Override
        public State state() {
            return this.counted < this.wanted ? State.RUNNING : State.FINISHED;
        }

        @Override
        public String message() {
            return "";
        }

        @Override
        public List<String> console() {
            return List.copyOf(this.console);
        }

        @Override
        public long written() {
            return this.console.size();
        }

        @Override
        public long spent() {
            return this.counted;
        }

        @Override
        public long heldBytes() {
            return 0L;
        }

        @Override
        public long heapBytes() {
            return 0L;
        }

        @Override
        public boolean isService() {
            return false;
        }

        @Override
        public void onTick() {
        }

        @Override
        public void onStop(final int budget) {
        }

        @Override
        public String name() {
            return "counter";
        }

        @Override
        public void save(final CompoundTag tag) {
            tag.putInt("Counted", this.counted);
        }
    }
}
