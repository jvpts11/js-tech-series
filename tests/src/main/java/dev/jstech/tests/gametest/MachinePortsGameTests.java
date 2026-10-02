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
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.MonitorBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.blockentity.SpeakerBlockEntity;
import dev.jstech.computers.os.media.MediaReaderBlockEntity;
import dev.jstech.core.peripheral.PortKind;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * The ports a peripheral takes, by kind: a monitor a video output of a graphics card, a speaker half of an audio
 * output, a drive a device port of the board. A peripheral with no free port of its kind waits, unlinked, and links
 * the moment one frees or a card brings one; taking the card out unlinks the screens it fed.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MachinePortsGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos PC = new BlockPos(2, 2, 2);
    /* Long enough for a peripheral's next tick to try its link, and a little more. */
    private static final int LINK_TICKS = 10;

    private MachinePortsGameTests() {
    }

    @GameTest(template = ARENA)
    public static void screen_waitsForAVideoOutputAndTakesOneWhenACardGoesIn(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = standardPc(helper);
        final BlockPos monitorAt = PC.east();
        helper.setBlock(monitorAt, ComputingModule.MONITOR.get());
        final MonitorBlockEntity monitor = entity(helper, monitorAt, MonitorBlockEntity.class);
        helper.startSequence()
                .thenExecuteAfter(LINK_TICKS, () -> {
                    helper.assertTrue(computer.ports(PortKind.VIDEO) == 0, "a machine with no card has no output");
                    helper.assertTrue(computer.ports(PortKind.DEVICE) == 8, "a Standard board has eight device ports");
                    helper.assertTrue(monitor.ownerPos() == null, "the monitor waits for a video output");
                })
                .thenExecute(() -> computer.getHardware().setStackInSlot(PersonalComputerBlockEntity.GPU_SLOTS_START,
                        new ItemStack(ComputingModule.GPU_HD_7970.get())))
                .thenExecuteAfter(LINK_TICKS, () -> {
                    helper.assertTrue(computer.ports(PortKind.VIDEO) == 4, "a Standard card has four outputs");
                    helper.assertTrue(helper.absolutePos(PC).equals(monitor.ownerPos()),
                            "the monitor links once the card gives it an output");
                    helper.assertTrue(computer.portsInUse(PortKind.VIDEO) == 1, "it takes one output");
                    helper.assertTrue(computer.portsInUse(PortKind.DEVICE) == 0, "and no device port");
                })
                .thenSucceed();
    }

    /** Once linked, a monitor, a drive and a speaker stay linked on every tick after, none of them flickering. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void links_holdTickAfterTick(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = standardPc(helper);
        computer.getHardware().setStackInSlot(PersonalComputerBlockEntity.GPU_SLOTS_START,
                new ItemStack(ComputingModule.GPU_HD_7970.get()));
        helper.setBlock(PC.east(), ComputingModule.MONITOR.get());
        helper.setBlock(PC.west(), ComputingModule.DVD_DRIVE.get());
        helper.setBlock(PC.south(), ComputingModule.SPEAKER.get());
        final MonitorBlockEntity monitor = entity(helper, PC.east(), MonitorBlockEntity.class);
        final MediaReaderBlockEntity drive = entity(helper, PC.west(), MediaReaderBlockEntity.class);
        final SpeakerBlockEntity speaker = entity(helper, PC.south(), SpeakerBlockEntity.class);
        final BlockPos owner = helper.absolutePos(PC);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(owner.equals(monitor.ownerPos())
                        && owner.equals(drive.ownerPos()) && owner.equals(speaker.ownerPos()), "all three link"))
                .thenExecuteFor(100, () -> helper.assertTrue(owner.equals(monitor.ownerPos())
                        && owner.equals(drive.ownerPos()) && owner.equals(speaker.ownerPos())
                        && computer.linkedEndpoints().size() == 3, "none of them ever unlinks"))
                .thenSucceed();
    }

    /** The same for a monitor on a running Mainframe with a graphics card, the machine a desktop is driven from. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void mainframeScreen_holdsItsLinkTickAfterTick(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = TestWorldBuilder.forGameTest(helper).placeRunningMainframe(PC);
        mainframe.getInventory().setStackInSlot(MainframeBlockEntity.GPU_SLOTS_START,
                new ItemStack(ComputingModule.GPU_HD_7970.get()));
        helper.setBlock(PC.east(), ComputingModule.MONITOR.get());
        final MonitorBlockEntity monitor = entity(helper, PC.east(), MonitorBlockEntity.class);
        final BlockPos owner = helper.absolutePos(PC);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(owner.equals(monitor.ownerPos()), "the monitor links"))
                .thenExecuteFor(100, () -> helper.assertTrue(owner.equals(monitor.ownerPos()),
                        "the monitor never unlinks"))
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void screens_unlinkWhenTheirCardIsTakenOut(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer = standardPc(helper);
        computer.getHardware().setStackInSlot(PersonalComputerBlockEntity.GPU_SLOTS_START,
                new ItemStack(ComputingModule.GPU_HD_7970.get()));
        helper.setBlock(PC.east(), ComputingModule.MONITOR.get());
        helper.setBlock(PC.west(), ComputingModule.MONITOR.get());
        final MonitorBlockEntity east = entity(helper, PC.east(), MonitorBlockEntity.class);
        final MonitorBlockEntity west = entity(helper, PC.west(), MonitorBlockEntity.class);
        helper.startSequence()
                .thenExecuteAfter(LINK_TICKS, () -> helper.assertTrue(
                        computer.portsInUse(PortKind.VIDEO) == 2, "both monitors linked to the card's outputs"))
                .thenExecute(() -> computer.getHardware().setStackInSlot(PersonalComputerBlockEntity.GPU_SLOTS_START,
                        ItemStack.EMPTY))
                .thenExecuteAfter(LINK_TICKS, () -> {
                    helper.assertTrue(east.ownerPos() == null && west.ownerPos() == null,
                            "without the card both monitors are unlinked");
                    helper.assertTrue(computer.linkedEndpoints().isEmpty(), "and the machine lists neither");
                })
                .thenExecute(() -> computer.getHardware().setStackInSlot(PersonalComputerBlockEntity.GPU_SLOTS_START,
                        new ItemStack(ComputingModule.GPU_HD_7970.get())))
                .thenExecuteAfter(LINK_TICKS, () -> helper.assertTrue(
                        computer.portsInUse(PortKind.VIDEO) == 2, "with the card back both link again"))
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void drives_takeTheVintageBoardsTwoPortsAndTheThirdWaits(final GameTestHelper helper) {
        helper.setBlock(PC, ComputingModule.VINTAGE_PERSONAL_COMPUTER.get());
        final PersonalComputerBlockEntity computer = entity(helper, PC, PersonalComputerBlockEntity.class);
        final ItemStackHandler hardware = computer.getHardware();
        hardware.setStackInSlot(PersonalComputerBlockEntity.MOTHERBOARD_SLOT,
                new ItemStack(HardwareItems.MOTHERBOARD_BABYAT_VINTAGE.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.CPU_SLOT,
                new ItemStack(HardwareItems.CPU_INTEGRA_486SX.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.RAM_SLOTS_START,
                new ItemStack(HardwareItems.RAM_SIMM_4.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.PSU_SLOT, new ItemStack(HardwareItems.PSU_300.get()));
        final BlockPos[] drives = {PC.east(), PC.west(), PC.south()};
        for (final BlockPos drive : drives) {
            helper.setBlock(drive, ComputingModule.FLOPPY_DRIVE.get());
        }
        helper.startSequence()
                .thenExecuteAfter(LINK_TICKS, () -> {
                    helper.assertTrue(computer.ports(PortKind.DEVICE) == 2, "a Vintage board has two ports");
                    int linked = 0;
                    for (final BlockPos drive : drives) {
                        if (entity(helper, drive, MediaReaderBlockEntity.class).linkedOwner().isPresent()) {
                            linked++;
                        }
                    }
                    helper.assertTrue(linked == 2, "two drives take the two ports and the third waits; linked "
                            + linked);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void speakers_takeThePairOfTheSoundCardsOutput(final GameTestHelper helper) {
        helper.setBlock(PC, ComputingModule.LEGACY_PERSONAL_COMPUTER.get());
        final PersonalComputerBlockEntity computer = entity(helper, PC, PersonalComputerBlockEntity.class);
        final ItemStackHandler hardware = computer.getHardware();
        hardware.setStackInSlot(PersonalComputerBlockEntity.MOTHERBOARD_SLOT,
                new ItemStack(HardwareItems.MOTHERBOARD_ATX_LEGACY_LGA775.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.CPU_SLOT,
                new ItemStack(HardwareItems.CPU_INTEGRA_PENTIX_4_560.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.RAM_SLOTS_START,
                new ItemStack(HardwareItems.RAM_DDR_1024.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.PSU_SLOT, new ItemStack(HardwareItems.PSU_500B.get()));
        final BlockPos[] speakers = {PC.east(), PC.west(), PC.south()};
        for (final BlockPos speaker : speakers) {
            helper.setBlock(speaker, ComputingModule.LEGACY_SPEAKER.get());
        }
        helper.startSequence()
                .thenExecuteAfter(LINK_TICKS, () -> helper.assertTrue(linkedSpeakers(helper, speakers) == 0,
                        "a Legacy board without a sound card has no audio output"))
                .thenExecute(() -> hardware.setStackInSlot(PersonalComputerBlockEntity.GPU_SLOTS_START,
                        new ItemStack(HardwareItems.SOUND_CARD_TONE_BLASTER_AUDIGY.get())))
                .thenExecuteAfter(LINK_TICKS, () -> {
                    helper.assertTrue(computer.ports(PortKind.AUDIO) == 2, "the card's output drives a pair");
                    helper.assertTrue(linkedSpeakers(helper, speakers) == 2,
                            "two speakers take the pair and the third waits");
                })
                .thenSucceed();
    }

    /* A Standard personal computer at PC with its board, processor, memory and supply, and no card. */
    private static PersonalComputerBlockEntity standardPc(final GameTestHelper helper) {
        helper.setBlock(PC, ComputingModule.PERSONAL_COMPUTER.get());
        final PersonalComputerBlockEntity computer = entity(helper, PC, PersonalComputerBlockEntity.class);
        final ItemStackHandler hardware = computer.getHardware();
        hardware.setStackInSlot(PersonalComputerBlockEntity.MOTHERBOARD_SLOT,
                new ItemStack(HardwareItems.MOTHERBOARD_ATX_STANDARD_LGA1150.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.CPU_SLOT,
                new ItemStack(HardwareItems.CPU_INTEGRA_CENTRO_C7_4790K.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.RAM_SLOTS_START,
                new ItemStack(ComputingModule.RAM_DDR3_8192.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.PSU_SLOT, new ItemStack(ComputingModule.PSU_650G.get()));
        return computer;
    }

    private static int linkedSpeakers(final GameTestHelper helper, final BlockPos[] speakers) {
        int linked = 0;
        for (final BlockPos speaker : speakers) {
            if (entity(helper, speaker, SpeakerBlockEntity.class).linkedOwner().isPresent()) {
                linked++;
            }
        }
        return linked;
    }

    private static <T> T entity(final GameTestHelper helper, final BlockPos at, final Class<T> type) {
        final Object entity = helper.getBlockEntity(at);
        if (!type.isInstance(entity)) {
            throw new IllegalStateException("no " + type.getSimpleName() + " at " + at);
        }
        return type.cast(entity);
    }
}
