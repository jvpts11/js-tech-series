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
import dev.jstech.computers.block.RedstoneInterfaceBlock;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.blockentity.RedstoneInterfaceBlockEntity;
import dev.jstech.computers.blockentity.SpeakerBlockEntity;
import dev.jstech.computers.os.media.MediaItem;
import dev.jstech.computers.os.media.MediaKind;
import dev.jstech.computers.os.media.MediaReaderBlockEntity;
import dev.jstech.core.audio.AudioOutput;
import dev.jstech.core.peripheral.PortKind;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;

/**
 * A device its computer disabled: it stays linked and keeps its port, but the computer neither reads nor writes it. A
 * disabled speaker plays nothing, a disabled Redstone Interface neither reads nor emits and no program finds it, a
 * disabled drive's disc is not seen by the system, and a disabled monitor goes dark. Enabled again, each is back; and
 * the computer remembers what it disabled across a save.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class DisabledDeviceGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    /** East of the computer, its screen turned east. */
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos NORTH = new BlockPos(5, 2, 1);
    private static final BlockPos SOUTH = new BlockPos(5, 2, 3);
    private static final int LINKED = 5;
    /** Long enough for a running computer's monitor to light. */
    private static final int LIT = 40;

    private DisabledDeviceGameTests() {
    }

    @GameTest(template = ARENA)
    public static void disabledSpeaker_playsNothingAndKeepsItsPort(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = computerWithMonitor(helper);
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        world.setBlock(NORTH, ComputingModule.SPEAKER.get());
        final SpeakerBlockEntity speaker = world.blockEntity(NORTH, SpeakerBlockEntity.class);
        final long at = helper.absolutePos(NORTH).asLong();
        helper.startSequence()
                .thenExecuteAfter(LINKED, () -> {
                    helper.assertTrue(pc.speakerCount() == 1, "the speaker links; got " + pc.speakerCount());
                    helper.assertTrue(pc.setDisabled(at, true), "the computer disables it");
                    final List<AudioOutput> outputs = pc.audioHost().outputs();
                    helper.assertTrue(outputs.equals(List.of(AudioOutput.at(centre(helper, MONITOR)))),
                            "only the monitor plays now; got " + outputs);
                    helper.assertTrue(pc.speakerCount() == 0, "the computer no longer counts it among its speakers");
                    helper.assertTrue(pc.portsInUse(PortKind.AUDIO) == 1, "but it keeps its audio port");
                    helper.assertTrue(speaker.linkedOwner().isPresent(), "and stays linked");
                    helper.assertTrue(speaker.channel() == SpeakerBlockEntity.CHANNEL_NONE,
                            "its screen says it plays no channel; got " + speaker.channel());
                    helper.assertTrue(pc.setDisabled(at, false), "enabled again");
                    helper.assertTrue(pc.speakerCount() == 1, "it plays again");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void disabledRedstoneInterface_neitherReadsNorEmitsAndIsNotFound(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = vintagePc(helper);
        final BlockPos sensorAt = COMPUTER.east();
        final BlockPos lamp = sensorAt.east();
        helper.setBlock(sensorAt, ComputingModule.VINTAGE_REDSTONE_INTERFACE.get().defaultBlockState()
                .setValue(RedstoneInterfaceBlock.FACING, Direction.EAST));
        helper.setBlock(lamp, Blocks.REDSTONE_LAMP);
        final long at = helper.absolutePos(sensorAt).asLong();
        helper.startSequence()
                .thenExecuteAfter(LINKED, () -> {
                    final RedstoneInterfaceBlockEntity sensor = sensor(helper, sensorAt);
                    helper.assertTrue(sensor.rename("Door"), "the interface takes its name");
                    sensor.emit(15, "");
                })
                .thenExecuteAfter(LINKED, () -> helper.assertTrue(
                        helper.getBlockState(lamp).getValue(RedstoneLampBlock.LIT), "the lamp at its lens lights"))
                .thenExecute(() -> helper.assertTrue(pc.setDisabled(at, true), "the computer disables it"))
                .thenExecuteAfter(LINKED, () -> {
                    final RedstoneInterfaceBlockEntity sensor = sensor(helper, sensorAt);
                    helper.assertTrue(!sensor.live() && sensor.output() == 0, "it emits nothing, unpowered");
                    helper.assertTrue(!helper.getBlockState(lamp).getValue(RedstoneLampBlock.LIT),
                            "and the lamp goes dark");
                    helper.assertTrue(sensor.linkedOwner().isPresent(), "still linked");
                    helper.assertTrue(RedstoneInterfaceBlockEntity.linkedTo(helper.getLevel(), pc, "Door") == null,
                            "and no program of its computer finds it by its name");
                    helper.assertTrue(sensor.emits() && sensor.strength() == 15, "keeping its setting");
                })
                .thenExecute(() -> pc.setDisabled(at, false))
                .thenExecuteAfter(LINKED, () -> helper.assertTrue(
                        helper.getBlockState(lamp).getValue(RedstoneLampBlock.LIT), "enabled, it lights the lamp again"))
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void disabledDrive_holdsADiscTheSystemDoesNotSee(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = computerWithMonitor(helper);
        helper.setBlock(SOUTH, ComputingModule.CD_DRIVE.get());
        if (!(helper.getBlockEntity(SOUTH) instanceof MediaReaderBlockEntity reader)) {
            throw new IllegalStateException("no drive at " + SOUTH);
        }
        final ItemStack disc = new ItemStack(ComputingModule.CD_ROM.get());
        MediaItem.setKind(disc, MediaKind.OS_INSTALL);
        MediaItem.setPayload(disc, ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "freebsd"));
        reader.mediaSlot().setStackInSlot(0, disc);
        final long at = helper.absolutePos(SOUTH).asLong();
        helper.startSequence()
                .thenExecuteAfter(LINKED, () -> {
                    helper.assertTrue(pc.hasBootableMedium(), "the computer sees the installer in its drive");
                    pc.setDisabled(at, true);
                    helper.assertTrue(!pc.hasBootableMedium(), "disabled, the drive's disc is not seen");
                    helper.assertTrue(pc.portsInUse(PortKind.DEVICE) == 1, "while the drive keeps its port");
                    pc.setDisabled(at, false);
                    helper.assertTrue(pc.hasBootableMedium(), "enabled, it is seen again");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void disabledMonitor_goesDark(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = computerWithMonitor(helper);
        final long at = helper.absolutePos(MONITOR).asLong();
        helper.startSequence()
                .thenExecuteAfter(LIT, () -> helper.assertTrue(
                        helper.getBlockState(MONITOR).getValue(MonitorBlock.LIT), "the running computer lights it"))
                .thenExecute(() -> pc.setDisabled(at, true))
                .thenExecuteAfter(LINKED, () -> helper.assertTrue(
                        !helper.getBlockState(MONITOR).getValue(MonitorBlock.LIT), "disabled, it goes dark"))
                .thenExecute(() -> pc.setDisabled(at, false))
                .thenExecuteAfter(LIT, () -> helper.assertTrue(
                        helper.getBlockState(MONITOR).getValue(MonitorBlock.LIT), "enabled, it lights again"))
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void disabled_isRememberedAcrossASave(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = computerWithMonitor(helper);
        TestWorldBuilder.forGameTest(helper).setBlock(NORTH, ComputingModule.SPEAKER.get());
        final long at = helper.absolutePos(NORTH).asLong();
        helper.startSequence()
                .thenExecuteAfter(LINKED, () -> {
                    pc.setDisabled(at, true);
                    final HolderLookup.Provider registries = helper.getLevel().registryAccess();
                    final CompoundTag saved = pc.saveCustomOnly(registries);
                    pc.setDisabled(at, false);
                    pc.loadCustomOnly(saved, registries);
                    helper.assertTrue(pc.isDisabled(at), "the computer reads back that it disabled the speaker");
                    helper.assertTrue(pc.speakerCount() == 0, "and still does not play it");
                })
                .thenSucceed();
    }

    private static PersonalComputerBlockEntity computerWithMonitor(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final PersonalComputerBlockEntity pc = world.placeRunningPersonalComputer(COMPUTER);
        world.placeMonitor(MONITOR, Direction.EAST);
        return pc;
    }

    /* A Vintage personal computer at COMPUTER, its board's two device ports free. */
    private static PersonalComputerBlockEntity vintagePc(final GameTestHelper helper) {
        helper.setBlock(COMPUTER, ComputingModule.VINTAGE_PERSONAL_COMPUTER.get());
        if (!(helper.getBlockEntity(COMPUTER) instanceof PersonalComputerBlockEntity computer)) {
            throw new IllegalStateException("no computer at " + COMPUTER);
        }
        final ItemStackHandler hardware = computer.getHardware();
        hardware.setStackInSlot(PersonalComputerBlockEntity.MOTHERBOARD_SLOT,
                new ItemStack(HardwareItems.MOTHERBOARD_BABYAT_VINTAGE.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.CPU_SLOT,
                new ItemStack(HardwareItems.CPU_INTEGRA_486SX.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.RAM_SLOTS_START,
                new ItemStack(HardwareItems.RAM_SIMM_4.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.PSU_SLOT, new ItemStack(HardwareItems.PSU_300.get()));
        return computer;
    }

    private static RedstoneInterfaceBlockEntity sensor(final GameTestHelper helper, final BlockPos at) {
        if (!(helper.getBlockEntity(at) instanceof RedstoneInterfaceBlockEntity sensor)) {
            throw new IllegalStateException("no Redstone Interface at " + at);
        }
        return sensor;
    }

    private static Vec3 centre(final GameTestHelper helper, final BlockPos pos) {
        return Vec3.atCenterOf(helper.absolutePos(pos));
    }
}
