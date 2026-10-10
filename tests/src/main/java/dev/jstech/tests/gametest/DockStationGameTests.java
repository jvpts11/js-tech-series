/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.audio.ComputingSounds;
import dev.jstech.computers.block.NetworkGatewayBlock;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.machine.DriveTable;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.os.media.DockStationBlock;
import dev.jstech.computers.os.media.DockStationBlockEntity;
import dev.jstech.computers.os.media.MediaVolume;
import dev.jstech.computers.operation.payload.files.FileAccess;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.core.audio.SoundKey;
import dev.jstech.core.peripheral.PeripheralLine;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Dock Station: each disk goes into the tray of its size; the computer it is linked to sees each docked disk as an
 * external drive after the stick, lettered and opened and written as a drive; a disk going in or out sounds like a
 * server on its rails. And the Network Gateway's back takes the Vintage serial line, so every era links it.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class DockStationGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos COMPUTER = new BlockPos(4, 2, 2);
    private static final BlockPos DOCK = COMPUTER.south();
    private static final int SETTLE = 10;
    /** Past the dock's second, when it reads its letters off the computer again. */
    private static final int LETTERS = 30;

    private DockStationGameTests() {
    }

    @GameTest(template = ARENA)
    public static void trays_takeEachDiskBySize(final GameTestHelper helper) {
        helper.setBlock(DOCK, ComputingModule.DOCK_STATION.get());
        final DockStationBlockEntity dock = (DockStationBlockEntity) helper.getBlockEntity(DOCK);
        helper.assertTrue(dock.insertDisk(disk(StorageTier.HDD)).isEmpty(), "a hard disk goes in");
        helper.assertTrue(dock.insertDisk(disk(StorageTier.SSD)).isEmpty(), "an SSD goes in");
        helper.assertTrue(dock.insertDisk(disk(StorageTier.NVME)).isEmpty(), "an NVMe goes in");
        helper.assertTrue(!dock.disk(DockStationBlockEntity.BAY_HDD).isEmpty()
                        && !dock.disk(DockStationBlockEntity.BAY_SSD).isEmpty()
                        && !dock.disk(DockStationBlockEntity.BAY_NVME).isEmpty(),
                "each in the tray cut for its size");
        helper.assertTrue(!dock.insertDisk(disk(StorageTier.HDD)).isEmpty(), "a taken tray takes no second disk");
        final ItemStack stick = new ItemStack(ComputingModule.USB_FLASH_DRIVE.get());
        helper.assertTrue(dock.insertDisk(stick) == stick, "a stick is no disk for a tray");
        helper.assertTrue(!dock.ejectDisk(DockStationBlockEntity.BAY_SSD).isEmpty()
                        && dock.disk(DockStationBlockEntity.BAY_SSD).isEmpty(),
                "Eject gives the disk back and frees its tray");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void dockedDisks_areDrivesAfterTheStick(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = computer(helper);
        helper.setBlock(DOCK, ComputingModule.DOCK_STATION.get());
        final DockStationBlockEntity dock = (DockStationBlockEntity) helper.getBlockEntity(DOCK);
        dock.insertMedia(new ItemStack(ComputingModule.USB_FLASH_DRIVE.get()));
        dock.insertDisk(disk(StorageTier.HDD));
        dock.insertDisk(disk(StorageTier.NVME));
        helper.startSequence()
                .thenExecuteAfter(LETTERS, () -> {
                    final List<Character> letters = DriveTable.of(pc, helper.getLevel()).all().stream()
                            .map(DriveTable.Drive::drive).toList();
                    helper.assertTrue(letters.equals(List.of('C', 'D', 'E', 'F')),
                            "the system disk, the stick, then the two docked disks; got " + letters);
                    helper.assertTrue(dock.letter(DockStationBlockEntity.USB) == 'D'
                                    && dock.letter(DockStationBlockEntity.BAY_HDD) == 'E'
                                    && dock.letter(DockStationBlockEntity.BAY_NVME) == 'F'
                                    && dock.letter(DockStationBlockEntity.BAY_SSD) == ' ',
                            "the dock knows each one's letter; got " + dock.letter(0) + dock.letter(1)
                                    + dock.letter(2) + dock.letter(3));
                    helper.assertTrue(!dock.hostName().isEmpty(), "and the computer it is docked to");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void dockedDisk_takesFilesAsADrive(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = computer(helper);
        helper.setBlock(DOCK, ComputingModule.DOCK_STATION.get());
        final DockStationBlockEntity dock = (DockStationBlockEntity) helper.getBlockEntity(DOCK);
        final ItemStack disk = disk(StorageTier.SSD);
        DiskFilesystem.write(disk, "readme.txt", FileType.TXT, "on the docked disk", Long.MAX_VALUE,
                FilesystemKind.HIERARCHICAL);
        dock.insertDisk(disk);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ServerCliComputer cli = new ServerCliComputer(pc, helper.getLevel());
                    // The empty stick port keeps its letter, D:, as an empty drive does; the docked disk is next.
                    helper.assertTrue(cli.readFile("E:\\readme.txt").ok(),
                            "the computer reads the disk's file as E:, after the dock's stick port");
                    helper.assertTrue(cli.writeFile("E:\\copy.txt", "written from the computer").ok(),
                            "and writes to it");
                    helper.assertTrue(DiskFilesystem.read(dock.disk(DockStationBlockEntity.BAY_SSD), "copy.txt")
                                    .isPresent(), "the file is on the disk in the tray");
                    final String key = MediaVolume.key(helper.absolutePos(DOCK).asLong(),
                            DockStationBlockEntity.BAY_SSD);
                    helper.assertTrue(FileAccess.mediaStackFor(helper.getLevel(), pc, key)
                                    == dock.disk(DockStationBlockEntity.BAY_SSD),
                            "and the explorers open the tray by its key");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void trays_soundLikeAServerOnItsRails(final GameTestHelper helper) {
        helper.setBlock(DOCK, ComputingModule.DOCK_STATION.get());
        final DockStationBlockEntity dock = (DockStationBlockEntity) helper.getBlockEntity(DOCK);
        final Heard heard = Heard.at(helper, DOCK);
        helper.startSequence()
                .thenExecuteAfter(2, () -> dock.insertDisk(disk(StorageTier.HDD)))
                .thenExecuteAfter(2, () -> dock.ejectDisk(DockStationBlockEntity.BAY_HDD))
                .thenExecuteAfter(2, () -> {
                    heard.stop();
                    heard.assertPlayed(helper, ComputingSounds.RACK_SLIDE_IN);
                    heard.assertPlayed(helper, ComputingSounds.RACK_SLIDE_OUT);
                })
                .thenSucceed();
    }

    /**
     * The Dock serves the disks of every era, so its back takes every era's peripheral cable: an Advanced computer
     * reaches it with its own cable. It used to take the Standard's and older only, and the install of an Advanced
     * system from a stick in it failed with nothing said.
     */
    @GameTest(template = ARENA)
    public static void dock_backTakesEveryErasCable(final GameTestHelper helper) {
        helper.setBlock(DOCK, ComputingModule.DOCK_STATION.get());
        final BlockState state = helper.getBlockState(DOCK);
        final DockStationBlock dock = (DockStationBlock) state.getBlock();
        final Direction back = state.getValue(HorizontalDirectionalBlock.FACING).getOpposite();
        for (final HardwareEra era : List.of(HardwareEra.VINTAGE, HardwareEra.LEGACY, HardwareEra.TRANSITION,
                HardwareEra.STANDARD, HardwareEra.ADVANCED)) {
            helper.assertTrue(dock.accepts(state, back, PeripheralLine.of(era)),
                    "the Dock's back takes the " + era.serializedName() + " cable");
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void gateway_backTakesTheVintageSerialLine(final GameTestHelper helper) {
        helper.setBlock(DOCK, ComputingModule.NETWORK_GATEWAY.get());
        final BlockState state = helper.getBlockState(DOCK);
        final NetworkGatewayBlock gateway = (NetworkGatewayBlock) state.getBlock();
        final Direction back = state.getValue(HorizontalDirectionalBlock.FACING).getOpposite();
        helper.assertTrue(gateway.accepts(state, back, PeripheralLine.of(HardwareEra.VINTAGE)),
                "the serial DE-9 in the middle of its back takes the Vintage line");
        helper.assertTrue(!gateway.accepts(state, back.getOpposite(), PeripheralLine.of(HardwareEra.VINTAGE)),
                "its front takes no cable; the front is the other side's");
        helper.succeed();
    }

    /* A running Personal Computer with the default desktop system. */
    private static PersonalComputerBlockEntity computer(final GameTestHelper helper) {
        return TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(COMPUTER);
    }

    private static ItemStack disk(final StorageTier tier) {
        return new ItemStack(ComputingModule.disk(tier, DiskSize.GB_500));
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

        void assertPlayed(final GameTestHelper helper, final SoundKey sound) {
            helper.assertTrue(sounds.contains(sound.id()), sound.id() + " was heard; heard instead: " + sounds);
        }
    }
}
