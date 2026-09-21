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
import dev.jstech.core.language.IProgrammingLanguage;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * How a machine writes its programs down and reads them back as a whole: a listing kept once however many programs run
 * it, and a save the machine cannot read bringing nothing back rather than something half guessed.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ProgramSaveGameTests {

    private static final String ARENA = "empty";
    /** Where a machine keeps its programs in its block entity's tag. */
    private static final String PROGRAMS = "Σ#";

    private ProgramSaveGameTests() {
    }

    /** A program that says one line. */
    private static final String HELLO = """
            using System.*;
            namespace Programs;
            class Hello {
                static void Main() {
                    Console.PrintLine("hello");
                }
            }
            """;

    /** Another one, so its listing is not the first one's. */
    private static final String BYE = """
            using System.*;
            namespace Programs;
            class Bye {
                static void Main() {
                    Console.PrintLine("bye");
                }
            }
            """;

    private static String listing(final String source) {
        final IProgrammingLanguage.CompileResult built = SigmaLanguage.SIGMA_SHARP.compile(
                List.of(new IProgrammingLanguage.SourceText("program.sgs", source)));
        if (!built.ok()) {
            throw new IllegalStateException("the test program does not compile: " + built.complaints());
        }
        return built.binary();
    }

    private static PersonalComputerBlockEntity computer(final GameTestHelper helper) {
        return TestWorldBuilder.at(helper.getLevel(), helper.absolutePos(BlockPos.ZERO))
                .placeRunningPersonalComputer(new BlockPos(2, 2, 2));
    }

    @GameTest(template = ARENA)
    public static void save_keepsAListingSeveralProgramsRunOnce(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = computer(helper);
        final MachinePrograms programs = computer.programs();
        final String hello = listing(HELLO);
        helper.assertTrue(programs.start("one.asm", hello, 1, computer).ok()
                        && programs.start("two.asm", hello, 1, computer).ok()
                        && programs.start("three.asm", listing(BYE), 1, computer).ok(),
                "three programs start from two listings");
        final var registries = helper.getLevel().registryAccess();
        final CompoundTag saved = computer.saveWithFullMetadata(registries);
        final int kept = saved.getCompound(PROGRAMS).getList("listings", Tag.TAG_COMPOUND).size();
        helper.assertTrue(kept == 2, "the save keeps each listing once; it kept " + kept);
        computer.loadWithComponents(saved, registries);
        helper.assertTrue(computer.programs().view().size() == 3,
                "and all three programs come back; got " + computer.programs().view().size());
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void load_bringsNothingBackFromASaveInAnotherForm(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = computer(helper);
        final MachinePrograms.Started started = computer.programs().start("hello.asm", listing(HELLO), 1, computer);
        helper.assertTrue(started.ok(), "the program starts: " + started.message());
        computer.programs().hold(started.id());
        final var registries = helper.getLevel().registryAccess();
        final CompoundTag saved = computer.saveWithFullMetadata(registries);
        saved.getCompound(PROGRAMS).putInt("format", 0);
        computer.loadWithComponents(saved, registries);
        helper.assertTrue(computer.programs().isEmpty(),
                "no program comes back from a save in a form the machine does not read");
        helper.assertTrue(computer.programs().held() == 0, "and the terminal holds nothing");
        final List<String> told = new ServerCliComputer(computer, helper.getLevel()).drainNotices();
        helper.assertTrue(told.stream().anyMatch(line -> line.contains("could not be brought back")),
                "the terminal says so the next time it is used; got " + told);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void load_bringsNothingBackWhenTheSaveLacksAListingItNames(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = computer(helper);
        final MachinePrograms programs = computer.programs();
        helper.assertTrue(programs.start("hello.asm", listing(HELLO), 1, computer).ok()
                && programs.start("bye.asm", listing(BYE), 1, computer).ok(), "two programs start");
        final var registries = helper.getLevel().registryAccess();
        final CompoundTag saved = computer.saveWithFullMetadata(registries);
        saved.getCompound(PROGRAMS).getList("listings", Tag.TAG_COMPOUND).remove(0);
        computer.loadWithComponents(saved, registries);
        helper.assertTrue(computer.programs().isEmpty(),
                "a save naming a listing it does not hold brings nothing back, not even the program whose listing is "
                        + "there");
        helper.succeed();
    }
}
