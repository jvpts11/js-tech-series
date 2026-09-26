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
import dev.jstech.computers.audio.ComputingSounds;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.os.install.SetupJob;
import dev.jstech.computers.os.media.MediaItem;
import dev.jstech.computers.os.media.MediaKind;
import dev.jstech.computers.os.media.MediaReaderBlockEntity;
import dev.jstech.core.audio.SoundKey;
import dev.jstech.core.text.Text;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The sounds the machines make as machines, heard where the server plays them: a computer's power button, an old
 * machine's start-up, a hard drive spinning up, turning and winding down while a solid-state disk stays quiet, the
 * self-test's beep on the two ages that had one, a floppy disk going into a drive and coming out, and the drive
 * heard reading while a program on its disk installs.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MachineSoundGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos WHERE = new BlockPos(2, 2, 2);
    private static final int SETTLE = 3;
    /** Past the end of a hard drive's spin-up, when it is heard turning. */
    private static final int SPUN_UP = 180;
    /** On a free side of the crafting network's computer, where a drive links to it. */
    private static final BlockPos DRIVE_BESIDE_COMPUTER = new BlockPos(5, 2, 3);
    /** A program that installs from a floppy disk. */
    private static final ResourceLocation FLOPPY_PROGRAM =
            ResourceLocation.fromNamespaceAndPath("jsc", "minesweeper");

    private MachineSoundGameTests() {
    }

    @GameTest(template = ARENA)
    public static void vintagePc_poweringOn_clicksAndStartsUp(final GameTestHelper helper) {
        final Heard heard = Heard.at(helper, WHERE);
        final PersonalComputerBlockEntity pc = computer(helper, ComputingModule.VINTAGE_PERSONAL_COMPUTER.get(),
                HardwareItems.MOTHERBOARD_BABYAT_VINTAGE.get(), HardwareItems.CPU_INTEGRA_486SX.get(),
                HardwareItems.RAM_SIMM_4.get(), HardwareItems.PSU_300B.get(), StorageTier.HDD);
        pc.togglePower();
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    heard.stop();
                    heard.assertPlayed(helper, ComputingSounds.POWER_BUTTON);
                    heard.assertPlayed(helper, ComputingSounds.VINTAGE_STARTUP);
                    heard.assertNotPlayed(helper, ComputingSounds.HARD_DRIVE_SPIN_UP);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void legacyPc_withHardDrive_spinsUpTurnsAndWindsDown(final GameTestHelper helper) {
        final Heard heard = Heard.at(helper, WHERE);
        final PersonalComputerBlockEntity pc = legacy(helper, StorageTier.HDD);
        pc.togglePower();
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    heard.assertPlayed(helper, ComputingSounds.HARD_DRIVE_SPIN_UP);
                    helper.assertFalse(turning(helper, pc), "the drive is not heard turning while it spins up");
                })
                .thenExecuteAfter(SPUN_UP, () -> {
                    helper.assertTrue(turning(helper, pc), "a spun-up drive is heard turning");
                    pc.togglePower();
                })
                .thenExecuteAfter(SETTLE, () -> {
                    heard.stop();
                    heard.assertPlayed(helper, ComputingSounds.HARD_DRIVE_SPIN_DOWN);
                    helper.assertFalse(turning(helper, pc), "a machine switched off stops its drive");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void standardPc_withSolidStateDisk_makesNoDriveSound(final GameTestHelper helper) {
        final Heard heard = Heard.at(helper, WHERE);
        final PersonalComputerBlockEntity pc = computer(helper, ComputingModule.PERSONAL_COMPUTER.get(),
                ComputingModule.MOTHERBOARD_ATX_P.get(), ComputingModule.CPU_ASCENT_965.get(),
                ComputingModule.RAM_DDR3_8192.get(), ComputingModule.PSU_650G.get(), StorageTier.SSD);
        pc.togglePower();
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertFalse(pc.needsPost(), "waiting for the self-test to pass"))
                .thenExecuteAfter(SETTLE, () -> {
                    heard.stop();
                    heard.assertPlayed(helper, ComputingSounds.POWER_BUTTON);
                    heard.assertNotPlayed(helper, ComputingSounds.HARD_DRIVE_SPIN_UP);
                    heard.assertNotPlayed(helper, ComputingSounds.POST_BEEP);
                    helper.assertFalse(turning(helper, pc), "a solid-state disk is never heard turning");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void legacyPc_passingItsSelfTest_beeps(final GameTestHelper helper) {
        final Heard heard = Heard.at(helper, WHERE);
        legacy(helper, StorageTier.HDD).togglePower();
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(heard.played(ComputingSounds.POST_BEEP),
                        "waiting for the self-test's beep"))
                .thenExecute(heard::stop)
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void floppyDrive_diskInAndOut_slidesInAndEjects(final GameTestHelper helper) {
        final Heard heard = Heard.at(helper, WHERE);
        helper.setBlock(WHERE, ComputingModule.FLOPPY_DRIVE.get());
        if (!(helper.getBlockEntity(WHERE) instanceof MediaReaderBlockEntity drive)) {
            helper.fail("no floppy drive at " + WHERE);
            return;
        }
        drive.insertMedia(new ItemStack(ComputingModule.FLOPPY_DISK.get()));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    heard.assertPlayed(helper, ComputingSounds.FLOPPY_INSERT);
                    drive.ejectMedia();
                })
                .thenExecuteAfter(SETTLE, () -> {
                    heard.assertPlayed(helper, ComputingSounds.FLOPPY_EJECT);
                    drive.insertMedia(new ItemStack(ComputingModule.FLOPPY_DISK.get()));
                })
                .thenExecuteAfter(SETTLE, () -> {
                    // A drive being broken lets its disk fall out without the sound of ejecting it.
                    drive.dropContents(helper.getLevel(), helper.absolutePos(WHERE));
                })
                .thenExecuteAfter(SETTLE, () -> {
                    heard.stop();
                    helper.assertTrue(heard.count(ComputingSounds.FLOPPY_EJECT) == 1,
                            "only the ejected disk is heard coming out, not the one a broken drive drops");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void floppyDrive_programInstallingFromItsDisk_isHeardReading(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final TestWorldBuilder.CraftingNetwork net = world.buildCraftingNetwork();
        world.setBlock(DRIVE_BESIDE_COMPUTER, ComputingModule.FLOPPY_DRIVE.get());
        final MediaReaderBlockEntity drive = world.blockEntity(DRIVE_BESIDE_COMPUTER, MediaReaderBlockEntity.class);
        final ItemStack disk = new ItemStack(ComputingModule.FLOPPY_DISK.get());
        MediaItem.setKind(disk, MediaKind.PROGRAM_INSTALL);
        MediaItem.setPayload(disk, FLOPPY_PROGRAM);
        drive.mediaSlot().setStackInSlot(0, disk);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 3, () -> {
                    helper.assertTrue(net.cc().getBlockPos().equals(drive.ownerPos()),
                            "the drive links to the computer beside it; got " + drive.ownerPos());
                    helper.assertFalse(reading(helper, drive), "a drive nobody installs from is not heard reading");
                    net.cc().console().beginSetup(new SetupJob(FLOPPY_PROGRAM.toString(), "Minesweeper", "Midsoft",
                            1, Text.literal("Floppy"), false, 400));
                })
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(reading(helper, drive),
                            "the drive is heard reading while the program on its disk installs");
                    net.cc().console().clearSetup();
                })
                .thenExecuteAfter(SETTLE, () -> helper.assertFalse(reading(helper, drive),
                        "and falls quiet when the setup is over"))
                .thenSucceed();
    }

    private static PersonalComputerBlockEntity legacy(final GameTestHelper helper, final StorageTier disk) {
        return computer(helper, ComputingModule.LEGACY_PERSONAL_COMPUTER.get(),
                HardwareItems.MOTHERBOARD_ATX_LEGACY_LGA775.get(), HardwareItems.CPU_INTEGRA_DUO_E4300.get(),
                HardwareItems.RAM_DDR2_2048.get(), HardwareItems.PSU_500B.get(), disk);
    }

    private static PersonalComputerBlockEntity computer(final GameTestHelper helper, final Block block,
                                                        final ItemLike board, final ItemLike cpu, final ItemLike ram,
                                                        final ItemLike psu, final StorageTier disk) {
        helper.setBlock(WHERE, block);
        if (!(helper.getBlockEntity(WHERE) instanceof PersonalComputerBlockEntity pc)) {
            throw new IllegalStateException("no personal computer at " + WHERE);
        }
        final ItemStackHandler hardware = pc.getHardware();
        hardware.setStackInSlot(PersonalComputerBlockEntity.MOTHERBOARD_SLOT, new ItemStack(board));
        hardware.setStackInSlot(PersonalComputerBlockEntity.CPU_SLOT, new ItemStack(cpu));
        hardware.setStackInSlot(PersonalComputerBlockEntity.RAM_SLOTS_START, new ItemStack(ram));
        hardware.setStackInSlot(PersonalComputerBlockEntity.PSU_SLOT, new ItemStack(psu));
        hardware.setStackInSlot(PersonalComputerBlockEntity.DISK_SLOTS_START,
                new ItemStack(ComputingModule.disk(disk, DiskSize.GB_500)));
        return pc;
    }

    /** Whether the client is told the drive is reading, read from what it is sent. */
    private static boolean reading(final GameTestHelper helper, final MediaReaderBlockEntity drive) {
        return drive.getUpdateTag(helper.getLevel().registryAccess()).getBoolean("Reading");
    }

    /** Whether the client is told the machine's hard drive is turning, read from what it is sent. */
    private static boolean turning(final GameTestHelper helper, final PersonalComputerBlockEntity pc) {
        return pc.getUpdateTag(helper.getLevel().registryAccess()).getBoolean("DiskTurning");
    }

    /** The sounds the server plays at one block, heard from the moment it is made until it is stopped. */
    private static final class Heard implements Consumer<PlayLevelSoundEvent.AtPosition> {

        private final Vec3 at;
        private final List<ResourceLocation> sounds = new ArrayList<>();

        private Heard(final Vec3 at) {
            this.at = at;
        }

        static Heard at(final GameTestHelper helper, final BlockPos local) {
            final Heard heard = new Heard(Vec3.atCenterOf(helper.absolutePos(local)));
            NeoForge.EVENT_BUS.addListener(heard);
            return heard;
        }

        @Override
        public void accept(final PlayLevelSoundEvent.AtPosition event) {
            if (event.getSound() != null && event.getPosition().distanceToSqr(at) < 0.01) {
                sounds.add(event.getSound().value().getLocation());
            }
        }

        void stop() {
            NeoForge.EVENT_BUS.unregister(this);
        }

        boolean played(final SoundKey sound) {
            return sounds.contains(sound.id());
        }

        int count(final SoundKey sound) {
            return (int) sounds.stream().filter(sound.id()::equals).count();
        }

        void assertPlayed(final GameTestHelper helper, final SoundKey sound) {
            helper.assertTrue(played(sound), sound.id() + " was heard; heard instead: " + sounds);
        }

        void assertNotPlayed(final GameTestHelper helper, final SoundKey sound) {
            helper.assertFalse(played(sound), sound.id() + " was not heard; heard: " + sounds);
        }
    }
}
