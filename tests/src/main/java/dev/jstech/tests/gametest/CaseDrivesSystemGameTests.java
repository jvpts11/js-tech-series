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
import dev.jstech.computers.blockentity.CaseFront;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.hardware.ComputerBuild;
import dev.jstech.computers.item.MotherboardItem;
import dev.jstech.computers.machine.DriveTable;
import dev.jstech.computers.operation.payload.FirmwareStatePayload;
import dev.jstech.computers.operation.payload.PatternStudioPayloads;
import dev.jstech.computers.operation.payload.PatternStudioStatePayload;
import dev.jstech.computers.operation.payload.crafting.CraftManagerPayloads;
import dev.jstech.computers.operation.payload.files.FileAccess;
import dev.jstech.computers.operation.payload.firmware.FirmwarePayloads;
import dev.jstech.computers.os.OsDisks;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.os.install.OsInstallJob;
import dev.jstech.computers.os.media.IMediaDrive;
import dev.jstech.computers.os.media.LiveMedium;
import dev.jstech.computers.os.media.MachineMedia;
import dev.jstech.computers.os.media.MediaDriveType;
import dev.jstech.computers.os.media.MediaItem;
import dev.jstech.computers.os.media.MediaKind;
import dev.jstech.computers.os.media.MediaVolume;
import dev.jstech.computers.program.install.LiveInstallState;
import dev.jstech.computers.storage.LocalStore;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.text.Text;
import dev.jstech.tests.JsTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;

/**
 * The drives of a small computer's own front, as its system sees them: a drive in a bay and a stick in a port are
 * drives of the machine like the external ones, with a letter, reached by the explorers by their volume, listed and
 * started from by the firmware, offering their programs, held as the live medium, written to by the Crafting Manager
 * and listed by the Pattern Studio; a screen's eject opens a bay's tray and pulls a port's stick; and a disk in the
 * hot-swap bay is one of the machine's own disks, with its letter, a system put on it, booted, and gone with its
 * system when it is taken out.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class CaseDrivesSystemGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos WHERE = new BlockPos(2, 2, 2);
    private static final ResourceLocation FRAMES_XP = id("frames_xp");
    private static final ResourceLocation MINESWEEPER = id("minesweeper");
    private static final ResourceLocation ARCH = id("arch");

    private CaseDrivesSystemGameTests() {
    }

    @GameTest(template = ARENA)
    public static void bayDrive_isADriveOfTheMachineReadWithItsTrayIn(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final PersonalComputerBlockEntity pc = legacy(helper, HardwareItems.INTERNAL_CD_DRIVE.get());
        final CaseFront front = pc.front();
        final MediaVolume volume = front.volume(0);
        helper.assertValueEqual(volume.key(), "media:" + pc.getBlockPos().asLong() + ".f0", "the bay's volume");
        final IMediaDrive drive = MachineMedia.of(pc, level).find(volume);
        helper.assertTrue(drive != null && drive.type() == MediaDriveType.CD_DRIVE,
                "the drive in the bay is one of the machine's drives, a CD drive");
        front.tray(0).press(level, pc.getBlockPos());
        front.insert(0, new ItemStack(ComputingModule.CD_ROM.get()));
        helper.assertTrue(MachineMedia.of(pc, level).find(volume).medium().isEmpty(),
                "a disc lying on the open tray is not read");
        front.tray(0).press(level, pc.getBlockPos());
        helper.assertTrue(MachineMedia.of(pc, level).find(volume).medium().is(ComputingModule.CD_ROM.get()),
                "with the tray in it is");
        helper.assertTrue(DriveTable.of(pc, level).all().stream()
                        .anyMatch(letter -> letter.disk().is(ComputingModule.CD_ROM.get())),
                "and the disc has a drive letter");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void port_isADriveOnlyWhileAStickIsInAndTheExplorersReachIt(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final PersonalComputerBlockEntity pc = place(helper, ComputingModule.TRANSITION_PERSONAL_COMPUTER.get());
        helper.assertTrue(MachineMedia.of(pc, level).drives().isEmpty(), "an empty port is no drive");
        final CaseFront front = pc.front();
        final int slot = CaseFront.portSlot(1);
        front.insert(slot, new ItemStack(ComputingModule.USB_FLASH_DRIVE.get()));
        final MediaVolume volume = front.volume(slot);
        final IMediaDrive drive = MachineMedia.of(pc, level).find(volume);
        helper.assertTrue(drive != null && drive.type() == MediaDriveType.USB_PORT,
                "a stick in a port is a drive of the machine, its USB port");
        helper.assertTrue(FileAccess.mediaStackFor(level, pc, volume.key() + "/notes.txt")
                .is(ComputingModule.USB_FLASH_DRIVE.get()), "which the explorers reach by its volume");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void screenEject_opensABaysTrayAndPullsAPortsStick(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final PersonalComputerBlockEntity pc = place(helper, ComputingModule.TRANSITION_PERSONAL_COMPUTER.get(),
                HardwareItems.INTERNAL_DVD_DRIVE.get());
        final CaseFront front = pc.front();
        front.tray(0).press(level, pc.getBlockPos());
        front.insert(0, new ItemStack(ComputingModule.DVD_RW.get()));
        front.tray(0).press(level, pc.getBlockPos());
        front.insert(CaseFront.portSlot(0), new ItemStack(ComputingModule.USB_FLASH_DRIVE.get()));
        final FakePlayer player = player(helper);
        MachineMedia.of(pc, level).find(front.volume(0)).eject(level, player);
        helper.assertTrue(front.tray(0).isOpen() && front.held(0).is(ComputingModule.DVD_RW.get()),
                "This PC's eject opens the tray, the disc waiting on it");
        MachineMedia.of(pc, level).find(front.volume(CaseFront.portSlot(0))).eject(level, player);
        helper.assertTrue(front.stick(0).isEmpty()
                        && player.getInventory().contains(new ItemStack(ComputingModule.USB_FLASH_DRIVE.get())),
                "and pulls the stick into the player's hands");
        player.getInventory().clearContent();
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void firmware_listsTheBayDriveAndInstallsFromItWithItsTrayHeldIn(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final PersonalComputerBlockEntity pc = legacy(helper, HardwareItems.INTERNAL_CD_DRIVE.get());
        pc.getHardware().setStackInSlot(PersonalComputerBlockEntity.DISK_SLOTS_START,
                new ItemStack(HardwareItems.DISK_LINK_IDE_40G.get()));
        final CaseFront front = pc.front();
        front.tray(0).press(level, pc.getBlockPos());
        front.insert(0, installer(new ItemStack(ComputingModule.CD_ROM.get()), MediaKind.OS_INSTALL, FRAMES_XP));
        front.tray(0).press(level, pc.getBlockPos());
        final MediaVolume volume = front.volume(0);
        final FirmwareStatePayload state = FirmwarePayloads.buildFirmwareState(level, pc, pc.getBlockPos());
        helper.assertTrue(state.entries().stream().anyMatch(entry -> entry.kind() == FirmwareStatePayload.KIND_MEDIA
                        && entry.ref() == volume.readerPos() && entry.bay() == volume.bay()
                        && FRAMES_XP.toString().equals(entry.osId())),
                "the firmware lists the installer in the bay by its drive: " + state.entries());
        helper.assertTrue(pc.hasBootableMedium(), "and the machine can start from it");
        helper.assertTrue(FirmwarePayloads.beginInstall(level, pc, volume.readerPos(), volume.bay(), -1) == null,
                "the install starts from the bay's disc");
        final OsInstallJob job = pc.installing();
        helper.assertTrue(job != null && job.reads(volume), "reading that very drive");
        helper.assertTrue(front.reading(0), "whose busy lamp is lit");
        MachineMedia.of(pc, level).find(volume).eject(level, player(helper));
        helper.assertFalse(front.tray(0).isOpen(), "and whose tray stays in while it reads");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void programOnABayDisc_isOfferedForInstall(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = legacy(helper, HardwareItems.INTERNAL_CD_DRIVE.get());
        final CaseFront front = pc.front();
        front.tray(0).press(helper.getLevel(), pc.getBlockPos());
        front.insert(0, installer(new ItemStack(ComputingModule.CD_ROM.get()), MediaKind.PROGRAM_INSTALL,
                MINESWEEPER));
        front.tray(0).press(helper.getLevel(), pc.getBlockPos());
        final ProgramSpec minesweeper = OsRegistry.getProgram(MINESWEEPER);
        helper.assertTrue(minesweeper != null && pc.services().installs().onMedia().contains(minesweeper),
                "the program on the bay's disc is offered for install");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void liveMedium_inABayIsHeldUntilItsTrayOpens(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        helper.assertTrue(LiveMedium.distroOf(ARCH) == LiveInstallState.Distro.ARCH, "arch is the Arch medium");
        final PersonalComputerBlockEntity pc = legacy(helper, HardwareItems.INTERNAL_CD_DRIVE.get());
        final CaseFront front = pc.front();
        front.tray(0).press(level, pc.getBlockPos());
        front.insert(0, installer(new ItemStack(ComputingModule.CD_ROM.get()), MediaKind.OS_INSTALL, ARCH));
        front.tray(0).press(level, pc.getBlockPos());
        helper.assertTrue(LiveMedium.holding(level, pc, LiveInstallState.Distro.ARCH) == LiveMedium.Answer.PRESENT,
                "the live medium in the bay is held");
        front.tray(0).press(level, pc.getBlockPos());
        helper.assertTrue(LiveMedium.holding(level, pc, LiveInstallState.Distro.ARCH) == LiveMedium.Answer.GONE,
                "and gone once its tray is out");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void craftManager_writesToAStickInAPort(final GameTestHelper helper) {
        helper.setBlock(WHERE, ComputingModule.TRANSITION_CRAFTING_COMPUTER.get());
        final CraftingComputerBlockEntity cc = helper.getBlockEntity(WHERE);
        final int slot = CaseFront.portSlot(0);
        cc.front().insert(slot, new ItemStack(ComputingModule.USB_FLASH_DRIVE.get()));
        helper.assertValueEqual(CraftManagerPayloads.buildCraftManagerState(cc, helper.getLevel()).mediaVolumeKey(),
                cc.front().volume(slot).key(), "the Crafting Manager writes to the stick in the port");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void patternStudio_listsAStickInAPort(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = place(helper, ComputingModule.TRANSITION_PERSONAL_COMPUTER.get());
        final int slot = CaseFront.portSlot(1);
        pc.front().insert(slot, new ItemStack(ComputingModule.USB_FLASH_DRIVE.get()));
        final String key = pc.front().volume(slot).key();
        helper.assertTrue(PatternStudioPayloads.buildState(helper.getLevel(), pc, Text.EMPTY, 0).drives().stream()
                        .map(PatternStudioStatePayload.Drive::key).anyMatch(key::equals),
                "the Pattern Studio lists the stick in the port");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void hotSwapDisk_isOneOfTheMachinesDisksWithASystemAndABoot(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final PersonalComputerBlockEntity pc = legacy(helper, HardwareItems.HOT_SWAP_DRIVE_BAY.get());
        final CaseFront front = pc.front();
        helper.assertTrue(front.insert(0, new ItemStack(HardwareItems.DISK_LINK_IDE_40G.get())).isEmpty(),
                "a disk of the machine's era slides into the bay");
        final int slot = PersonalComputerBlockEntity.DISK_SLOTS;
        helper.assertTrue(pc.diskSlots() > slot && pc.diskInSlot(slot).is(HardwareItems.DISK_LINK_IDE_40G.get()),
                "it is a disk slot of the system's, after the case's own");
        helper.assertTrue(pc.wiredDiskSlots() == PersonalComputerBlockEntity.DISK_SLOTS,
                "while the disk slots the board wires stay what they were");
        helper.assertTrue(DriveTable.of(pc, level).all().stream()
                        .anyMatch(letter -> letter.disk().is(HardwareItems.DISK_LINK_IDE_40G.get())),
                "with a letter of its own");
        helper.assertTrue(pc.installOs(FRAMES_XP, slot), "a system goes on it");
        helper.assertValueEqual(OsDisks.systemOn(front.held(0)), FRAMES_XP, "the system written on the bay's disk");
        helper.assertTrue(pc.systemDisk() == front.held(0), "which the machine boots");
        helper.assertTrue(FirmwarePayloads.buildFirmwareState(level, pc, pc.getBlockPos()).entries().stream()
                        .anyMatch(entry -> entry.kind() == FirmwareStatePayload.KIND_DISK && entry.ref() == slot
                                && FRAMES_XP.toString().equals(entry.osId())),
                "and the firmware lists among its disks");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void hotSwapDisk_takenOutWhileRunningTakesItsSystemWithIt(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = legacy(helper, HardwareItems.HOT_SWAP_DRIVE_BAY.get());
        final CaseFront front = pc.front();
        front.insert(0, new ItemStack(HardwareItems.DISK_LINK_IDE_40G.get()));
        pc.installOs(FRAMES_XP, PersonalComputerBlockEntity.DISK_SLOTS);
        helper.assertTrue(pc.isRunning() && pc.hasOs(), "the machine runs the system on the bay's disk");
        final ItemStack out = front.take(0);
        helper.assertTrue(FRAMES_XP.equals(OsDisks.systemOn(out)), "the disk leaves with its system on it");
        helper.assertFalse(pc.hasOs(), "and the machine has none left, as when the disk comes out on its screen");
        helper.assertFalse(pc.validateOsSession(), "so a session on it ends");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void hotSwapDisk_holdsItemsAndCountsInTheBuildAsTheOthersDo(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = legacy(helper, HardwareItems.HOT_SWAP_DRIVE_BAY.get());
        final ComputerBuild empty = pc.currentBuild();
        helper.assertTrue(empty != null && empty.totalStorageItems() == 0L, "the machine starts with no disk at all");
        final CaseFront front = pc.front();
        front.insert(0, new ItemStack(HardwareItems.DISK_LINK_IDE_20G.get()));
        final ComputerBuild build = pc.currentBuild();
        helper.assertTrue(build != null && build.frontDisks().size() == 1 && build.totalStorageItems() > 0L,
                "the bay's disk counts in the build's capacity");
        helper.assertTrue(build.isPowered() && build.disks().isEmpty(), "and takes no disk slot of the board");
        helper.assertTrue(pc.diskPrivacyDiskCount() == PersonalComputerBlockEntity.DISK_SLOTS + 1,
                "with a privacy slider of its own, and none for the bay after it, which holds no disk");
        final StorageKey cobblestone = StorageKey.of(Items.COBBLESTONE);
        helper.assertTrue(pc.localStore().insert(cobblestone, 10) == 10 && pc.localStore().count(cobblestone) == 10,
                "the machine keeps its items on the bay's disk, its only disk");
        final ItemStack out = front.take(0);
        helper.assertTrue(pc.localStore().count(cobblestone) == 0, "taken out, the disk takes its items with it");
        helper.assertTrue(new LocalStore(List.of(out), () -> { }).count(cobblestone) == 10, "held on the disk itself");
        helper.assertTrue(pc.currentBuild().frontDisks().isEmpty(), "and leaves the build");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void hotSwapLamp_blinksForTheDiskTheSystemRunsFromAndNoOther(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = legacy(helper, HardwareItems.HOT_SWAP_DRIVE_BAY.get());
        final CaseFront front = pc.front();
        front.insert(0, new ItemStack(HardwareItems.DISK_LINK_IDE_40G.get()));
        pc.installOs(FRAMES_XP, PersonalComputerBlockEntity.DISK_SLOTS);
        pc.diskWorked(helper.getLevel());
        helper.assertTrue(front.diskWorked(0), "the bay's lamp blinks while the machine works the disk it runs from");
        helper.assertFalse(front.reading(0), "which is no drive reading a disc");
        // The system on a disk of the case's own, the bay's disk holding data only.
        pc.getHardware().setStackInSlot(PersonalComputerBlockEntity.DISK_SLOTS_START,
                new ItemStack(HardwareItems.DISK_LINK_IDE_40G.get()));
        front.take(0);
        front.insert(0, new ItemStack(HardwareItems.DISK_LINK_IDE_20G.get()));
        pc.installOs(FRAMES_XP, 0);
        pc.diskWorked(helper.getLevel());
        helper.assertTrue(pc.systemDisk() != front.held(0), "the machine runs from its own disk");
        helper.assertFalse(front.diskWorked(0), "and the bay's lamp stays dark for a disk it does not work");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void diskSlots_agreeWithTheDisksTheSystemReadsOnEveryBoard(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = place(helper, ComputingModule.LEGACY_PERSONAL_COMPUTER.get());
        int boards = 0;
        for (final Item item : BuiltInRegistries.ITEM) {
            if (!(item instanceof MotherboardItem)) {
                continue;
            }
            boards++;
            pc.getHardware().setStackInSlot(PersonalComputerBlockEntity.MOTHERBOARD_SLOT, new ItemStack(item));
            for (final Item bay : new Item[] {Items.AIR, HardwareItems.HOT_SWAP_DRIVE_BAY.get()}) {
                pc.getHardware().setStackInSlot(PersonalComputerBlockEntity.BAY_SLOTS_START, new ItemStack(bay));
                helper.assertValueEqual(pc.diskSlots(), pc.diskStacks().size(),
                        "the disk slots the system loops over and the ones it boots from, with "
                                + BuiltInRegistries.ITEM.getKey(item) + " and " + BuiltInRegistries.ITEM.getKey(bay));
            }
        }
        helper.assertTrue(boards > 0, "the game has boards to try");
        helper.succeed();
    }

    /* A Legacy Personal Computer, built and switched on, with these parts in its bays from the first. */
    private static PersonalComputerBlockEntity legacy(final GameTestHelper helper, final Item... bays) {
        final PersonalComputerBlockEntity pc = place(helper, ComputingModule.LEGACY_PERSONAL_COMPUTER.get(), bays);
        final ItemStackHandler hardware = pc.getHardware();
        hardware.setStackInSlot(PersonalComputerBlockEntity.MOTHERBOARD_SLOT,
                new ItemStack(HardwareItems.MOTHERBOARD_ATX_LEGACY_LGA775.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.CPU_SLOT,
                new ItemStack(HardwareItems.CPU_INTEGRA_PENTIX_4_560.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.RAM_SLOTS_START,
                new ItemStack(HardwareItems.RAM_DDR_1024.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.PSU_SLOT, new ItemStack(HardwareItems.PSU_500B.get()));
        pc.togglePower();
        return pc;
    }

    /* A Personal Computer in that case, with these parts in its bays from the first. */
    private static PersonalComputerBlockEntity place(final GameTestHelper helper, final Block block,
                                                     final Item... bays) {
        helper.setBlock(WHERE, block);
        final PersonalComputerBlockEntity pc = helper.getBlockEntity(WHERE);
        for (int bay = 0; bay < bays.length; bay++) {
            pc.getHardware().setStackInSlot(PersonalComputerBlockEntity.BAY_SLOTS_START + bay,
                    new ItemStack(bays[bay]));
        }
        return pc;
    }

    private static ItemStack installer(final ItemStack medium, final MediaKind kind, final ResourceLocation payload) {
        MediaItem.setKind(medium, kind);
        MediaItem.setPayload(medium, payload);
        return medium;
    }

    private static FakePlayer player(final GameTestHelper helper) {
        final FakePlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.getInventory().clearContent();
        return player;
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, path);
    }
}
