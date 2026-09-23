/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.computers.HardwareItems;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.block.MonitorBlock;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.operation.payload.firmware.FirmwarePayloads;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.install.OsInstallJob;
import dev.jstech.computers.os.install.OsInstallRunner;
import dev.jstech.computers.os.media.MediaItem;
import dev.jstech.computers.os.media.MediaKind;
import dev.jstech.computers.os.media.MediaReaderBlockEntity;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * A guided installer writes the system and then waits for a reboot. Until that reboot happens the
 * machine is still running the installer, whatever the disk now holds: leaving the monitor and coming
 * back must return to the reboot prompt, never to a system that was never booted. Only a restart or
 * a power change ends the installer.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class OsInstallGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;
    private static final ResourceLocation DEBIAN = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "debian");
    private static final ResourceLocation FRAMES_95 = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "frames_95");

    private OsInstallGameTests() {
    }

    /** A running Mainframe with an empty disk and, beside it, a CD drive holding a guided installer. */
    private static MainframeBlockEntity placeMainframeWithInstaller(final GameTestHelper helper, final BlockPos pos) {
        helper.setBlock(pos, ComputingModule.MAINFRAME.get());
        if (!(helper.getBlockEntity(pos) instanceof MainframeBlockEntity mainframe)) {
            throw new IllegalStateException("no Mainframe at " + pos);
        }
        final ItemStackHandler inv = mainframe.getInventory();
        inv.setStackInSlot(MainframeBlockEntity.MOTHERBOARD_SLOT, new ItemStack(ComputingModule.MOTHERBOARD_MTX_P.get()));
        inv.setStackInSlot(MainframeBlockEntity.CPU_SLOTS_START, new ItemStack(ComputingModule.CPU_SERVO_2620.get()));
        inv.setStackInSlot(MainframeBlockEntity.RAM_SLOTS_START, new ItemStack(ComputingModule.RAM_DDR3_8192.get()));
        inv.setStackInSlot(MainframeBlockEntity.PSU_SLOT, new ItemStack(ComputingModule.PSU_650G.get()));
        // A GPU gives the Mainframe peripheral ports so the adjacent reader can link to it.
        inv.setStackInSlot(MainframeBlockEntity.GPU_SLOTS_START, new ItemStack(ComputingModule.GPU_HD_7970.get()));
        inv.setStackInSlot(MainframeBlockEntity.DISK_SLOTS_START,
                new ItemStack(ComputingModule.disk(StorageTier.HDD, DiskSize.GB_500)));
        mainframe.togglePower();

        final BlockPos readerPos = pos.east();
        helper.setBlock(readerPos, ComputingModule.CD_DRIVE.get());
        if (!(helper.getBlockEntity(readerPos) instanceof MediaReaderBlockEntity reader)) {
            throw new IllegalStateException("no reader at " + readerPos);
        }
        final ItemStack disc = new ItemStack(ComputingModule.CD_ROM.get());
        MediaItem.setKind(disc, MediaKind.OS_INSTALL);
        MediaItem.setPayload(disc, DEBIAN);
        reader.mediaSlot().setStackInSlot(0, disc);
        return mainframe;
    }

    /**
     * Starts the guided install and carries the machine's copy to its end.
     *
     * <p>The copy takes a minute and a half of world ticks, which is not something a test sits through: the
     * runner is asked for every one of its ticks here instead, which is the same path the machine walks.
     */
    private static void finishInstaller(final GameTestHelper helper, final MainframeBlockEntity mainframe) {
        helper.assertTrue(!mainframe.linkedEndpoints().isEmpty(), "the reader links to the Mainframe");
        helper.assertTrue(FirmwarePayloads.installOsFromReader(helper.getLevel(), mainframe, -1L, -1),
                "the guided installer opens");
        answerEveryQuestion(helper, mainframe);
        final dev.jstech.computers.os.install.OsInstallJob job = mainframe.installing();
        helper.assertTrue(job != null, "the machine took the copy on");
        for (int i = 0; i <= job.ticksTotal(); i++) {
            dev.jstech.computers.os.install.OsInstallRunner.tick(mainframe, helper.getLevel(),
                    mainframe.getBlockPos());
        }
        helper.assertTrue(DEBIAN.equals(mainframe.installedOsId()), "the system is on the disk");
    }

    /**
     * Takes the installer's own suggestion on every page it asks, which is what a player pressing the one key
     * does: the disk it found, the name the machine already goes by, and no desktop where no Mirror answers.
     */
    private static void answerEveryQuestion(final GameTestHelper helper, final MainframeBlockEntity mainframe) {
        final dev.jstech.computers.os.install.InstallerFlow flow = mainframe.installer();
        helper.assertTrue(flow != null, "the machine opened an installer");
        for (int page = 0; page < flow.style().stages().size() && !flow.started(); page++) {
            flow.next();
        }
        helper.assertTrue(flow.started(), "every question was answered and the work began");
    }

    /**
     * The copy is the machine's: it goes on with nobody watching, and nothing is on the disk until it ends.
     */
    @GameTest(template = ARENA)
    public static void guidedInstall_writesNothingUntilTheCopyEnds(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(2, 2, 2);
        final MainframeBlockEntity mainframe = placeMainframeWithInstaller(helper, pos);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 4, () -> {
                    mainframe.setNeedsPost(false);
                    helper.assertTrue(FirmwarePayloads.beginInstall(helper.getLevel(), mainframe, -1L, -1) == null,
                            "the installer opens");
                    final dev.jstech.computers.os.install.InstallerFlow flow = mainframe.installer();
                    helper.assertTrue(flow != null && !flow.started(), "and waits on its first question");
                    helper.assertTrue(!mainframe.hasOs(), "and nothing on the disk yet");
                    helper.assertTrue(MonitorBlock.entryFor(mainframe) == MonitorBlock.Entry.INSTALLING,
                            "a monitor opened now joins the installer where it is");
                    for (int i = 0; i < 20; i++) {
                        dev.jstech.computers.os.install.OsInstallRunner.tick(mainframe, helper.getLevel(), pos);
                    }
                    final dev.jstech.computers.os.install.OsInstallJob waiting = mainframe.installing();
                    helper.assertTrue(waiting != null && waiting.ticksLeft() == waiting.ticksTotal(),
                            "and copies nothing at all while a question has no answer");

                    answerEveryQuestion(helper, mainframe);
                    final dev.jstech.computers.os.install.OsInstallJob job = mainframe.installing();
                    helper.assertTrue(job != null && job.ticksLeft() == job.ticksTotal(), "with all of it to go");
                    for (int i = 0; i < 20; i++) {
                        dev.jstech.computers.os.install.OsInstallRunner.tick(mainframe, helper.getLevel(), pos);
                    }
                    helper.assertTrue(job.ticksLeft() < job.ticksTotal(), "it moves whether or not anybody watches");
                    helper.assertTrue(!mainframe.hasOs(), "and still nothing is written part way through");
                })
                .thenSucceed();
    }

    /**
     * The name the installer asks for is the name the machine ends up with, which is why it asks at all.
     *
     * <p>It also pins where the installer stands when the work is over: on its last page, waiting to be
     * restarted, rather than gone the moment the files landed.
     */
    @GameTest(template = ARENA)
    public static void guidedInstall_namesTheComputerWhatItWasTold(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(2, 2, 2);
        final MainframeBlockEntity mainframe = placeMainframeWithInstaller(helper, pos);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 4, () -> {
                    mainframe.setNeedsPost(false);
                    helper.assertTrue(FirmwarePayloads.beginInstall(helper.getLevel(), mainframe, -1L, -1) == null,
                            "the installer opens");
                    final dev.jstech.computers.os.install.InstallerFlow flow = mainframe.installer();
                    helper.assertTrue(flow != null, "the machine opened an installer");
                    flow.setComputerName("LIBRARY");
                    answerEveryQuestion(helper, mainframe);
                    final dev.jstech.computers.os.install.OsInstallJob job = mainframe.installing();
                    helper.assertTrue(job != null, "the machine took the copy on");
                    for (int i = 0; i <= job.ticksTotal(); i++) {
                        dev.jstech.computers.os.install.OsInstallRunner.tick(mainframe, helper.getLevel(), pos);
                    }
                    helper.assertTrue("LIBRARY".equals(mainframe.console().computerName()),
                            "the machine took the name: " + mainframe.console().computerName());
                    final dev.jstech.computers.os.install.InstallerFlow after = mainframe.installer();
                    helper.assertTrue(after != null
                                    && after.page() == dev.jstech.computers.os.install.InstallerPage.DONE,
                            "and the installer waits on its last page for the restart");
                })
                .thenSucceed();
    }

    /** What the machine was reading leaves the drive: the copy stops and the disk is untouched. */
    @GameTest(template = ARENA)
    public static void guidedInstall_stopsWhenTheMediumIsTakenOut(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(2, 2, 2);
        final MainframeBlockEntity mainframe = placeMainframeWithInstaller(helper, pos);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 4, () -> {
                    mainframe.setNeedsPost(false);
                    helper.assertTrue(FirmwarePayloads.beginInstall(helper.getLevel(), mainframe, -1L, -1) == null,
                            "the copy starts");
                    if (!(helper.getBlockEntity(pos.east()) instanceof MediaReaderBlockEntity reader)) {
                        helper.fail("no reader beside the Mainframe");
                        return;
                    }
                    reader.mediaSlot().setStackInSlot(0, ItemStack.EMPTY);
                    dev.jstech.computers.os.install.OsInstallRunner.tick(mainframe, helper.getLevel(), pos);
                    helper.assertTrue(mainframe.installing() == null, "the copy is over");
                    helper.assertTrue(!mainframe.hasOs(), "and the disk never saw the system");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void guidedInstall_keepsTheInstallerUntilTheReboot(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(2, 2, 2);
        final MainframeBlockEntity mainframe = placeMainframeWithInstaller(helper, pos);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 4, () -> {
                    mainframe.setNeedsPost(false); // the player already sat through POST to reach the firmware
                    helper.assertTrue(MonitorBlock.entryFor(mainframe) == MonitorBlock.Entry.BOOT,
                            "before the install a running machine boots its target");
                    finishInstaller(helper, mainframe);
                    helper.assertTrue(mainframe.pendingInstallSlot() != IOsHost.NO_PENDING_INSTALL,
                            "the machine remembers it is still in the installer");
                    helper.assertTrue(MonitorBlock.entryFor(mainframe) == MonitorBlock.Entry.INSTALLING,
                            "reopening the monitor returns to the installer's last page, not the system");
                    // The reboot the prompt asks for: a restart, as the firmware's boot-disk action performs.
                    mainframe.setNeedsPost(true);
                    helper.assertTrue(mainframe.pendingInstallSlot() == IOsHost.NO_PENDING_INSTALL,
                            "the restart ends the installer");
                    helper.assertTrue(MonitorBlock.entryFor(mainframe) == MonitorBlock.Entry.POST,
                            "the restart runs POST");
                    mainframe.setNeedsPost(false);
                    helper.assertTrue(MonitorBlock.entryFor(mainframe) == MonitorBlock.Entry.BOOT,
                            "after POST the machine boots the system it installed");
                })
                .thenSucceed();
    }

    /**
     * A guided installer for a system newer than the machine's era is refused with a reason the player
     * can act on, and the disk stays as it was. The refusal used to be silent: the installer reported
     * every step done, and the next boot landed back in the firmware with nothing on the disk.
     */
    @GameTest(template = ARENA)
    public static void guidedInstall_refusesASystemNewerThanTheMachine(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, ComputingModule.VINTAGE_PERSONAL_COMPUTER.get());
        if (!(helper.getBlockEntity(pos) instanceof PersonalComputerBlockEntity pc)) {
            throw new IllegalStateException("no vintage personal computer at " + pos);
        }
        // The weakest machine the mod builds, with the smallest disk that still holds the system.
        final ItemStackHandler hw = pc.getHardware();
        hw.setStackInSlot(PersonalComputerBlockEntity.MOTHERBOARD_SLOT,
                new ItemStack(HardwareItems.MOTHERBOARD_BABYAT_VINTAGE.get()));
        hw.setStackInSlot(PersonalComputerBlockEntity.CPU_SLOT, new ItemStack(HardwareItems.CPU_INTEGRA_486SX.get()));
        hw.setStackInSlot(PersonalComputerBlockEntity.RAM_SLOTS_START, new ItemStack(HardwareItems.RAM_SIMM_4.get()));
        hw.setStackInSlot(PersonalComputerBlockEntity.PSU_SLOT, new ItemStack(HardwareItems.PSU_300B.get()));
        hw.setStackInSlot(PersonalComputerBlockEntity.DISK_SLOTS_START,
                new ItemStack(HardwareItems.DISK_TRENCH_20M.get()));
        pc.togglePower();

        final BlockPos readerPos = pos.east();
        helper.setBlock(readerPos, ComputingModule.CD_DRIVE.get());
        if (!(helper.getBlockEntity(readerPos) instanceof MediaReaderBlockEntity reader)) {
            throw new IllegalStateException("no reader at " + readerPos);
        }
        final ItemStack disc = new ItemStack(ComputingModule.CD_ROM.get());
        MediaItem.setKind(disc, MediaKind.OS_INSTALL);
        MediaItem.setPayload(disc, FRAMES_95);
        reader.mediaSlot().setStackInSlot(0, disc);

        helper.startSequence()
                .thenExecuteAfter(SETTLE + 4, () -> {
                    helper.assertTrue(pc.isRunning(), "the vintage machine powers on");
                    helper.assertTrue(!pc.linkedEndpoints().isEmpty(), "the reader links to the computer");
                    final String failure = FirmwarePayloads.beginInstall(helper.getLevel(), pc, -1L, -1);
                    helper.assertTrue(failure != null && failure.contains("Legacy") && failure.contains("Vintage"),
                            "the refusal names the era the system needs and the machine's own, got: " + failure);
                    helper.assertTrue(!pc.hasOs(), "nothing was written to the disk");
                    helper.assertTrue(pc.pendingInstallSlot() == IOsHost.NO_PENDING_INSTALL,
                            "no reboot is pending after a refused install");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void guidedInstall_powerCycleEndsTheInstaller(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(2, 2, 2);
        final MainframeBlockEntity mainframe = placeMainframeWithInstaller(helper, pos);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 4, () -> {
                    mainframe.setNeedsPost(false);
                    finishInstaller(helper, mainframe);
                    helper.assertTrue(MonitorBlock.entryFor(mainframe) == MonitorBlock.Entry.INSTALLING,
                            "the installer waits for its reboot");
                    mainframe.togglePower(); // off
                    helper.assertTrue(mainframe.pendingInstallSlot() == IOsHost.NO_PENDING_INSTALL,
                            "switching the machine off drops the installer session");
                    helper.assertTrue(MonitorBlock.entryFor(mainframe) == MonitorBlock.Entry.NO_POWER,
                            "an off machine has no signal");
                    mainframe.togglePower(); // on again
                    helper.assertTrue(MonitorBlock.entryFor(mainframe) == MonitorBlock.Entry.POST,
                            "a cold start runs POST and then boots the installed system");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void guidedInstall_pendingRebootIsSavedWithTheMachine(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(2, 2, 2);
        final MainframeBlockEntity mainframe = placeMainframeWithInstaller(helper, pos);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 4, () -> {
                    mainframe.setNeedsPost(false);
                    finishInstaller(helper, mainframe);
                    final CompoundTag saved = mainframe.saveWithoutMetadata(helper.getLevel().registryAccess());
                    /*
                     * The disk is the one the installer settled on, not the word "the default": the installer
                     * chose it on its own page, so the reboot that follows boots that disk and no other.
                     */
                    helper.assertTrue(saved.contains("PendingInstall") && saved.getInt("PendingInstall") == 0,
                            "the pending reboot is written with the machine and names the disk it wrote to, got: "
                                    + (saved.contains("PendingInstall") ? saved.getInt("PendingInstall") : "none"));
                    // A formatted or pulled disk leaves nothing to reboot into: the pending install is dropped.
                    mainframe.getInventory().setStackInSlot(MainframeBlockEntity.DISK_SLOTS_START, ItemStack.EMPTY);
                    helper.assertTrue(MonitorBlock.entryFor(mainframe) == MonitorBlock.Entry.BOOT
                                    && mainframe.pendingInstallSlot() == IOsHost.NO_PENDING_INSTALL,
                            "with the installed disk gone the machine falls back to its boot target");
                })
                .thenSucceed();
    }

    /**
     * A machine in a rack holds its own copy, like any other machine.
     *
     * <p>It used to be written to there and then, because a bay had nowhere to keep the work: the copy took no
     * time, survived nothing and could not be watched. It keeps one now, on the Server item with the rest of
     * its session, so it goes on with nobody looking and is still going after a save.
     */
    @GameTest(template = ARENA)
    public static void rackUnit_keepsItsOwnCopyRatherThanBeingWrittenToAtOnce(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, ComputingModule.SERVER_RACK.get());
        if (!(helper.getBlockEntity(pos) instanceof ServerRackBlockEntity rack)) {
            helper.fail("no rack at " + pos);
            return;
        }
        TestWorldBuilder.mountDefaultServer(rack, 0);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final IOsHost unit = rack.unitHost(0);
                    helper.assertTrue(unit.keepsInstalls(), "a bay can hold a copy of its own now");

                    final OsInstallJob job = new OsInstallJob(DEBIAN.toString(), 0,
                            OsInstallJob.NO_READER, 40, 40);
                    unit.setInstalling(job);
                    helper.assertTrue(unit.installing() != null, "the bay took the copy on");
                    helper.assertTrue(!unit.hasOs(), "and nothing is on its drive yet");

                    final CompoundTag onItem = rack.getServers().getStackInSlot(0)
                            .get(ComputingComponents.SERVER_CONSOLE.get());
                    helper.assertTrue(onItem != null && onItem.contains("Installing"),
                            "the copy is flushed onto the Server item, so a save keeps it");

                    for (int i = 0; i <= job.ticksTotal(); i++) {
                        OsInstallRunner.tick(unit, helper.getLevel(), pos);
                    }
                    helper.assertTrue(DEBIAN.equals(unit.installedOsId()),
                            "and when it ends the system is on the bay's drive; got " + unit.installedOsId());
                    helper.assertTrue(unit.installing() == null, "with the copy over");
                })
                .thenSucceed();
    }

    /**
     * Two machines in one rack, one monitor: only the bay the switch is on is being looked at.
     *
     * <p>The bays all share the rack's position, so the watcher lookup cannot tell them apart. That makes this
     * the one place a page for one machine could land in front of somebody watching another, and the reason
     * every installer screen asks the machine whether it is the one on screen before it sends anything.
     */
    @GameTest(template = ARENA)
    public static void rackUnits_onlyTheBayTheSwitchIsOnIsOnScreen(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, ComputingModule.SERVER_RACK.get());
        if (!(helper.getBlockEntity(pos) instanceof ServerRackBlockEntity rack)) {
            helper.fail("no rack at " + pos);
            return;
        }
        TestWorldBuilder.mountDefaultServer(rack, 0);
        TestWorldBuilder.mountDefaultServer(rack, 1);
        rack.getServers().setStackInSlot(2, new ItemStack(ComputingModule.KVM_SWITCH.get()));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(rack.hasKvmSwitch(), "the switch is mounted");
                    rack.setActiveChannel(0);
                    helper.assertTrue(rack.unitHost(0).onScreen(), "the bay the switch is on is on screen");
                    helper.assertTrue(!rack.unitHost(1).onScreen(), "and the other bay is not");
                    rack.setActiveChannel(1);
                    helper.assertTrue(!rack.unitHost(0).onScreen(), "switching channels moves it over");
                    helper.assertTrue(rack.unitHost(1).onScreen(), "to the bay now being shown");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void rackUnit_pendingRebootRidesOnTheServerItem(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, ComputingModule.SERVER_RACK.get());
        if (!(helper.getBlockEntity(pos) instanceof ServerRackBlockEntity rack)) {
            helper.fail("no rack at " + pos);
            return;
        }
        TestWorldBuilder.mountDefaultServer(rack, 0);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final IOsHost unit = rack.unitHost(0);
                    unit.setPendingInstallSlot(0);
                    helper.assertTrue(unit.pendingInstallSlot() == 0, "the unit remembers its pending reboot");
                    final CompoundTag onItem =
                            rack.getServers().getStackInSlot(0).get(ComputingComponents.SERVER_CONSOLE.get());
                    helper.assertTrue(onItem != null && onItem.getInt("PendingInstall") == 0,
                            "the pending reboot is flushed onto the Server item with the rest of its session");
                    rack.toggleBayPower(0); // the bay switch is this machine's power button
                    helper.assertTrue(unit.pendingInstallSlot() == IOsHost.NO_PENDING_INSTALL,
                            "switching the bay off drops the installer session");
                    final CompoundTag after =
                            rack.getServers().getStackInSlot(0).get(ComputingComponents.SERVER_CONSOLE.get());
                    helper.assertTrue(after == null || !after.contains("PendingInstall"),
                            "and the item no longer carries it");
                })
                .thenSucceed();
    }
}
