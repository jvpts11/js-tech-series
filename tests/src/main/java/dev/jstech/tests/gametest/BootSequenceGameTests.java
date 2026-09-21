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
import dev.jstech.computers.block.MonitorBlock;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.os.boot.BootLines;
import dev.jstech.computers.os.boot.BootMenu;
import dev.jstech.computers.os.boot.BootSequence;
import dev.jstech.computers.os.boot.BootRunner;
import dev.jstech.computers.os.boot.BootTiming;
import dev.jstech.computers.os.install.SetupTiming;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * What a system says while it comes up is read off the machine it is coming up on.
 *
 * <p>A line that was written in advance would say the same thing on every computer, which is the one thing a
 * starting machine must never do: the drive letters are the drives that are in, and a step about the network
 * reports what answered, or says that nothing did.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class BootSequenceGameTests {

    private static final String ARENA = "empty";

    private static final BlockPos WHERE = new BlockPos(2, 2, 2);

    /** Ticks for a machine's attachment to find the cable beside it. */
    private static final int SETTLE = 4;

    /** Room for the longest closing-down any machine here takes, so the wait for one is never cut short. */
    private static final int PATIENT =
            (int) (BootTiming.SHUTDOWN_MAX_SECONDS * SetupTiming.TICKS_PER_SECOND) + 40;

    private static final ResourceLocation MC_DOS =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "mc_dos");

    private static final ResourceLocation DEBIAN =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "debian");

    private BootSequenceGameTests() {
    }

    @GameTest(template = ARENA)
    public static void dos_readsTheDrivesThatAreInTheMachine(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = vintageWithDos(helper);
        if (computer == null) {
            return;
        }
        final BootSequence sequence = BootLines.forMachine(computer, helper.getLevel());
        helper.assertTrue(sequence.title().startsWith("Starting MC-DOS"),
                "the system names itself as it starts: " + sequence.title());
        helper.assertTrue(marked(sequence, "Drive C:"),
                "the first drive gets the first letter: " + labels(sequence));
        helper.assertFalse(marked(sequence, "Drive D:"),
                "and a machine with one drive has no second letter");
        helper.assertTrue(any(sequence, "KB extended memory available"),
                "the memory above the line is counted: " + labels(sequence));
        helper.succeed();
    }

    /** No cable, no network step: a machine does not report a link it does not have. */
    @GameTest(template = ARENA)
    public static void dos_withNoCable_saysNothingOfANetwork(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = vintageWithDos(helper);
        if (computer == null) {
            return;
        }
        helper.assertFalse(any(BootLines.forMachine(computer, helper.getLevel()), "network link up"),
                "a machine with no cable reaching it has no network line to show");
        helper.succeed();
    }

    /**
     * A kernel names the architecture the processor really understands, so the same distribution reads
     * differently on a machine of another age.
     */
    @GameTest(template = ARENA)
    public static void linux_namesTheArchitectureTheProcessorUnderstands(final GameTestHelper helper) {
        final PersonalComputerBlockEntity older = legacy(helper);
        if (older == null) {
            return;
        }
        helper.assertTrue(older.installOs(DEBIAN), "Debian installs on the machine");
        final BootSequence sequence = BootLines.forMachine(older, helper.getLevel());
        helper.assertTrue(has(sequence, "Linux version 6.8-jsc (i686)"),
                "a 16-bit machine's kernel is not x86_64: " + labels(sequence));
        helper.assertTrue(has(sequence, "Memory: " + older.ramTotalMb() + " MB available"),
                "and the memory is the memory that is in it: " + labels(sequence));
        helper.succeed();
    }

    /**
     * A machine on a cable says its network is up, whatever is or is not running on it yet.
     *
     * <p>The line is a claim about the network, so it used to be wrong to put the question to the shell: the
     * machine is on a network the moment a cable reaches one, and it is saying so on its way up, which is
     * before anything on it is running to be asked.
     */
    @GameTest(template = ARENA)
    public static void linux_saysTheNetworkIsUpWhenACableReachesOne(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.at(helper.getLevel(), helper.absolutePos(BlockPos.ZERO));
        /*
         * The whole chain, because a computer never meets a Mainframe directly: the Mainframe sits on the HBW
         * backbone and a Personal Router is what bridges an Ethernet machine onto it.
         */
        world.placeRunningMainframe(new BlockPos(1, 2, 2));
        world.setBlock(new BlockPos(2, 2, 2), ComputingModule.HBW_CABLE.get());
        world.setBlock(new BlockPos(3, 2, 2), ComputingModule.PERSONAL_ROUTER.get());
        world.setBlock(new BlockPos(4, 2, 2), ComputingModule.ETHERNET_CABLE.get());
        final PersonalComputerBlockEntity onACable = legacy(helper, new BlockPos(5, 2, 2));
        if (onACable == null) {
            return;
        }
        world.faceRearTowardCable(new BlockPos(5, 2, 2));
        helper.assertTrue(onACable.installOs(DEBIAN), "Debian installs on the cabled machine");
        onACable.togglePower();

        final PersonalComputerBlockEntity onItsOwn = legacy(helper, new BlockPos(5, 2, 5));
        if (onItsOwn == null) {
            return;
        }
        helper.assertTrue(onItsOwn.installOs(DEBIAN), "and on the one standing on its own");
        onItsOwn.togglePower();
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final BootSequence cabled = BootLines.forMachine(onACable, helper.getLevel());
                    final BootSequence alone = BootLines.forMachine(onItsOwn, helper.getLevel());
                    helper.assertTrue(onACable.networkAttached(), "the cable reaches a network");
                    helper.assertTrue(has(cabled, "Reached target Network is Online."),
                            "so the machine says so coming up: " + labels(cabled));
                    helper.assertFalse(has(alone, "Reached target Network is Online."),
                            "and the one with no cable claims nothing of the sort");
                })
                .thenSucceed();
    }

    /**
     * The Frames family says nothing at all while it loads, which is its character and not a gap.
     *
     * <p>A machine that reports its steps and one that shows a picture are both true to what they are. This
     * family shows the picture: the maker and the edition are drawn into it, so the sequence carries no words
     * for the screen to write underneath them.
     */
    @GameTest(template = ARENA)
    public static void frames_comesUpBehindItsPictureAndSaysNothing(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer =
                TestWorldBuilder.at(helper.getLevel(), helper.absolutePos(BlockPos.ZERO))
                        .placeRunningPersonalComputer(WHERE);
        /*
         * An ordinary start, not the first one: the newest edition greets the machine by name the first time it
         * ever comes up, and this is the question about every start after that.
         */
        computer.setSystemWelcome(computer.systemWelcome().met());
        final BootSequence sequence = BootLines.forMachine(computer, helper.getLevel());
        helper.assertTrue(sequence.title().isEmpty(),
                "the picture carries the maker's name, so the sequence does not: " + sequence.title());
        helper.assertTrue(sequence.subtitle().isEmpty(),
                "nor the edition: " + sequence.subtitle());
        helper.assertTrue(sequence.lines().isEmpty(), "and not a word about what it is doing");
        helper.succeed();
    }

    /**
     * The one start of that family that does say something: the newest edition's first, which greets the
     * machine by name where its maker's mark would otherwise be.
     */
    @GameTest(template = ARENA)
    public static void frames_firstStart_greetsTheMachineByName(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer =
                TestWorldBuilder.at(helper.getLevel(), helper.absolutePos(BlockPos.ZERO))
                        .placeRunningPersonalComputer(WHERE);
        final BootSequence sequence = BootLines.forMachine(computer, helper.getLevel());
        helper.assertTrue(sequence.title().equals("Hi."),
                "a machine coming up for the first time is greeted: " + sequence.title());
        helper.assertFalse(sequence.subtitle().isEmpty(),
                "and told what is being got ready for whom");
        helper.succeed();
    }

    /**
     * The later systems say goodbye; the earliest ones simply stop.
     *
     * <p>A machine of the first age switched off where it stood, and giving it a farewell screen would be the
     * kind of politeness those machines never had.
     */
    @GameTest(template = ARENA)
    public static void shutdown_belongsToTheSystemsThatHadOne(final GameTestHelper helper) {
        final PersonalComputerBlockEntity frames =
                TestWorldBuilder.at(helper.getLevel(), helper.absolutePos(BlockPos.ZERO))
                        .placeRunningPersonalComputer(WHERE);
        helper.assertFalse(BootLines.shutdownFor(frames, false).isEmpty(),
                "a machine of this age closes its programs and says so");
        /*
         * And says a different thing when it is coming straight back up, which is the difference between
         * being switched off and being started over.
         */
        helper.assertFalse(BootLines.shutdownFor(frames, true).subtitle()
                        .equals(BootLines.shutdownFor(frames, false).subtitle()),
                "restarting is not shutting down, and the machine does not say it is");

        final PersonalComputerBlockEntity older = vintageWithDos(helper, new BlockPos(4, 2, 2));
        if (older == null) {
            return;
        }
        helper.assertTrue(BootLines.shutdownFor(older, false).isEmpty(),
                "and one of the first age goes dark where it stands");
        helper.succeed();
    }

    /**
     * The boot manager lists every system that is really installed, and stops the machine only where that
     * family's manager really stopped it: the Frames one went straight through with a single installation and
     * only ever appeared once there was a second system to choose between.
     */
    @GameTest(template = ARENA)
    public static void bootMenu_listsWhatTheMachineCouldBoot(final GameTestHelper helper) {
        final PersonalComputerBlockEntity linux = legacy(helper);
        if (linux == null) {
            return;
        }
        helper.assertTrue(linux.installOs(DEBIAN), "Debian installs on the machine");
        final BootMenu menu = BootLines.menuFor(linux, 100);
        helper.assertFalse(menu.isEmpty(), "a Linux machine stops at its boot manager");
        helper.assertTrue(menu.entries().size() == 1, "with one entry for its one disk: " + menu.entries());
        helper.assertTrue(menu.entries().get(0).label().equals("Debian"),
                "named after the system on it: " + menu.entries().get(0).label());

        final PersonalComputerBlockEntity frames =
                TestWorldBuilder.at(helper.getLevel(), helper.absolutePos(BlockPos.ZERO))
                        .placeRunningPersonalComputer(new BlockPos(4, 2, 2));
        helper.assertTrue(BootLines.menuFor(frames, 100).isEmpty(),
                "and a Frames machine with one system goes straight through, as that manager did");
        helper.succeed();
    }

    /**
     * A restart lets the system say goodbye before the self-test begins, and only then begins it.
     *
     * <p>The whole point of the phase: a machine that jumped straight to its self-test never showed the one
     * screen a system of any age put up on its way down, although restarting is how anybody reboots one here.
     * A machine still closing down is not owing a self-test yet, and a machine that has finished is.
     */
    @GameTest(template = ARENA, timeoutTicks = PATIENT)
    public static void restart_closesTheSystemDownBeforeTheSelfTestBegins(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer =
                TestWorldBuilder.at(helper.getLevel(), helper.absolutePos(BlockPos.ZERO))
                        .placeRunningPersonalComputer(WHERE);
        computer.setNeedsPost(false);
        /*
         * Measured off this machine rather than guessed at: how long it closes down for follows how long it
         * takes to come up, so the wait here is the machine's own answer and not a number that has to be kept
         * in step with one.
         */
        final int closing = BootTiming.shutdownTicks(BootRunner.bootLength(computer));
        computer.restart();
        helper.assertFalse(computer.needsPost(),
                "a machine still closing its programs has not begun testing itself");
        helper.startSequence()
                .thenExecuteAfter(closing + 2, () -> helper.assertTrue(computer.needsPost(),
                        "and once it has finished, the self-test is owed"))
                .thenSucceed();
    }

    /**
     * A monitor opened while a machine is closing down joins the goodbye, rather than the desktop under it.
     *
     * <p>The screen a player is put in front of is decided by what the machine is doing, and a machine on its
     * way down is doing exactly one thing. Without the closing-down among those answers, looking away during a
     * restart and looking back handed the player the desktop of a system that was being torn down.
     */
    @GameTest(template = ARENA)
    public static void restart_aMonitorOpenedMidway_joinsTheGoodbye(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer =
                TestWorldBuilder.at(helper.getLevel(), helper.absolutePos(BlockPos.ZERO))
                        .placeRunningPersonalComputer(WHERE);
        computer.setNeedsPost(false);
        computer.restart();
        helper.assertTrue(MonitorBlock.entryFor(computer) == MonitorBlock.Entry.GOING_DOWN,
                "a machine closing down puts its goodbye on the glass: " + MonitorBlock.entryFor(computer));
        helper.assertTrue(computer.downRemaining() > 0, "with what is left of it, so the screen joins it");
        helper.assertTrue(computer.downTotal() >= computer.downRemaining(),
                "and how long the whole of it takes");
        helper.succeed();
    }

    /**
     * A machine of the earliest age has nothing to show on its way down, so a restart begins at once.
     *
     * <p>Waiting for a goodbye that system never had would leave the machine dark for several seconds for no
     * reason a player could see.
     */
    @GameTest(template = ARENA)
    public static void restart_withNothingToSay_beginsTheSelfTestAtOnce(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = vintageWithDos(helper);
        if (computer == null) {
            return;
        }
        computer.setPowered(true);
        computer.setNeedsPost(false);
        computer.restart();
        helper.assertTrue(computer.needsPost(), "nothing to say, so the machine starts over where it stands");
        helper.succeed();
    }

    /** A machine with no system has nothing to come up, and so nothing to say. */
    @GameTest(template = ARENA)
    public static void aMachineWithNoSystem_hasNoSequence(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = vintage(helper);
        if (computer == null) {
            return;
        }
        helper.assertTrue(BootLines.forMachine(computer, helper.getLevel()).isEmpty(),
                "nothing installed, nothing to show");
        helper.succeed();
    }

    private static boolean has(final BootSequence sequence, final String label) {
        for (final BootSequence.Line line : sequence.lines()) {
            if (line.label().equals(label)) {
                return true;
            }
        }
        return false;
    }

    /** Whether any step carries that mark at the head of its line, which is where a drive letter goes. */
    private static boolean marked(final BootSequence sequence, final String mark) {
        for (final BootSequence.Line line : sequence.lines()) {
            if (line.mark().equals(mark)) {
                return true;
            }
        }
        return false;
    }

    /** Whether any step says that anywhere in its line, for the lines a machine writes out as sentences. */
    private static boolean any(final BootSequence sequence, final String text) {
        for (final BootSequence.Line line : sequence.lines()) {
            if (line.label().contains(text) || line.value().contains(text)) {
                return true;
            }
        }
        return false;
    }

    private static String labels(final BootSequence sequence) {
        final StringBuilder out = new StringBuilder();
        for (final BootSequence.Line line : sequence.lines()) {
            out.append(line.label()).append(' ');
        }
        return out.toString();
    }

    private static PersonalComputerBlockEntity vintageWithDos(final GameTestHelper helper) {
        return vintageWithDos(helper, WHERE);
    }

    private static PersonalComputerBlockEntity vintageWithDos(final GameTestHelper helper, final BlockPos at) {
        final PersonalComputerBlockEntity computer = vintage(helper, at);
        if (computer == null) {
            return null;
        }
        if (!computer.installOs(MC_DOS)) {
            helper.fail("MC-DOS would not install on the vintage machine");
            return null;
        }
        return computer;
    }

    /** A Legacy machine: the 32-bit generation, whose kernel calls itself i686. */
    private static PersonalComputerBlockEntity legacy(final GameTestHelper helper) {
        return legacy(helper, WHERE);
    }

    private static PersonalComputerBlockEntity legacy(final GameTestHelper helper, final BlockPos at) {
        helper.setBlock(at, ComputingModule.LEGACY_PERSONAL_COMPUTER.get());
        if (!(helper.getBlockEntity(at) instanceof PersonalComputerBlockEntity computer)) {
            helper.fail("no legacy personal computer at " + at);
            return null;
        }
        final ItemStackHandler hardware = computer.getHardware();
        hardware.setStackInSlot(PersonalComputerBlockEntity.MOTHERBOARD_SLOT,
                new ItemStack(HardwareItems.MOTHERBOARD_ATX_LEGACY_LGA775.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.CPU_SLOT,
                new ItemStack(HardwareItems.CPU_INTEGRA_DUO_E4300.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.RAM_SLOTS_START,
                new ItemStack(HardwareItems.RAM_DDR2_2048.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.PSU_SLOT,
                new ItemStack(HardwareItems.PSU_500B.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.DISK_SLOTS_START,
                new ItemStack(ComputingModule.disk(StorageTier.HDD, DiskSize.GB_500)));
        return computer;
    }

    private static PersonalComputerBlockEntity vintage(final GameTestHelper helper) {
        return vintage(helper, WHERE);
    }

    private static PersonalComputerBlockEntity vintage(final GameTestHelper helper, final BlockPos at) {
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
        hardware.setStackInSlot(PersonalComputerBlockEntity.PSU_SLOT,
                new ItemStack(HardwareItems.PSU_300B.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.DISK_SLOTS_START,
                new ItemStack(ComputingModule.disk(StorageTier.HDD, DiskSize.GB_500)));
        return computer;
    }
}
