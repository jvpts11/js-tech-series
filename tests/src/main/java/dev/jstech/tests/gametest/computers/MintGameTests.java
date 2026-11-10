/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.operation.payload.firmware.FirmwarePayloads;
import dev.jstech.computers.operation.payload.program.UpdatePayloads;
import dev.jstech.computers.os.OsDisks;
import dev.jstech.computers.os.ProgramVersions;
import dev.jstech.computers.os.boot.BootSplash;
import dev.jstech.computers.os.install.InstallerFlow;
import dev.jstech.computers.os.install.InstallerStyle;
import dev.jstech.computers.os.media.MediaItem;
import dev.jstech.computers.os.media.MediaKind;
import dev.jstech.computers.os.media.MediaReaderBlockEntity;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Linux Mint (computers/docs/design/operating-systems.md): it comes with Cinnamon and its two programs of its own, its
 * medium boots a live desktop that writes nothing to the disk, its installer is its own, and its Update Manager
 * brings what it ticks up to the Mirror's versions.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MintGameTests {

    private static final String ARENA = "empty";

    private static final BlockPos WHERE = new BlockPos(2, 2, 2);

    /** Ticks for a machine's attachment to find what is beside it. */
    private static final int SETTLE = 4;

    private static final ResourceLocation MINT = rl("linux_mint");
    private static final ResourceLocation UBUNTU = rl("ubuntu");
    private static final String CINNAMON = rl("cinnamon").toString();

    private MintGameTests() {
    }

    /** The installed system comes with Cinnamon, the Software Manager and the Update Manager, behind its own mark. */
    @GameTest(template = ARENA)
    public static void mint_comesWithCinnamonAndItsTwoPrograms(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer =
                TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(WHERE, MINT);
        helper.assertTrue(MINT.equals(computer.installedOsId()), "Linux Mint is installed");
        helper.assertTrue(computer.console().isInstalled(CINNAMON), "with Cinnamon");
        helper.assertTrue(computer.console().isInstalled(rl("software_manager").toString())
                && computer.console().isInstalled(rl("update_manager").toString()), "and its two programs");
        helper.assertTrue(rl("cinnamon").equals(computer.installedDesktopId()), "it comes up at Cinnamon");
        helper.assertTrue(computer.installedOs().bootSplash() == BootSplash.MINT, "behind its own mark");
        helper.succeed();
    }

    /** A live session runs Linux Mint off a disk in memory, and the machine's own disk keeps what it had. */
    @GameTest(template = ARENA)
    public static void liveSession_writesNothingToTheDisk(final GameTestHelper helper) {
        final PersonalComputerBlockEntity computer =
                TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(WHERE, UBUNTU);
        helper.assertTrue(computer.startLiveSession(MINT), "the machine starts the live medium");
        computer.settleLiveSession();
        helper.assertTrue(computer.liveSession(), "and is in a live session");
        helper.assertTrue(MINT.equals(computer.installedOsId()), "running Linux Mint");
        helper.assertTrue(UBUNTU.equals(OsDisks.systemOn(computer.diskInSlot(0))),
                "while its own disk still carries only what it had");
        computer.togglePower();
        helper.assertFalse(computer.liveSession(), "and the session is gone with the power");
        helper.succeed();
    }

    /** Its medium opens its own installer, which shows the divider only beside another system. */
    @GameTest(template = ARENA)
    public static void installer_isMintsOwn(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = mainframeWithInstallMedia(helper, WHERE);
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 4, () -> {
                    mainframe.setNeedsPost(false);
                    helper.assertTrue(FirmwarePayloads.beginInstall(helper.getLevel(), mainframe, -1L, -1) == null,
                            "the installer opens");
                    final InstallerFlow flow = mainframe.installer();
                    helper.assertTrue(flow != null && flow.style() == InstallerStyle.MINT, "Linux Mint's own");
                    helper.assertFalse(flow.alongside(), "an empty disk has nothing to go beside");
                })
                .thenSucceed();
    }

    /** The Update Manager installs what is ticked at the version the Mirror serves, and only with a Mirror. */
    @GameTest(template = ARENA)
    public static void updates_bringWhatIsTickedUpToTheMirror(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = bareMainframe(helper, WHERE);
        if (!mainframe.installOs(MINT)) {
            throw new IllegalStateException("failed to install Linux Mint on the test Mainframe");
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    mainframe.console().setInstalledVersion(CINNAMON, "0.1");
                    helper.assertTrue(mainframe.console().outdatedPackages().contains(CINNAMON),
                            "an older Cinnamon is an update");
                    helper.assertTrue(UpdatePayloads.apply(mainframe, helper.getLevel(), List.of(CINNAMON)) == 0,
                            "with no Mirror nothing is installed");
                    mainframe.installMirror();
                    helper.assertTrue(UpdatePayloads.apply(mainframe, helper.getLevel(), List.of(CINNAMON)) == 1,
                            "with one it is");
                    final String now = mainframe.console().installedVersion(CINNAMON);
                    helper.assertTrue(ProgramVersions.of(CINNAMON).equals(now), "at the Mirror's version; got " + now);
                    helper.assertFalse(mainframe.console().outdatedPackages().contains(CINNAMON), "and is no update");
                })
                .thenSucceed();
    }

    private static ResourceLocation rl(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, path);
    }

    /** A running Mainframe with an empty disk and, beside it, a CD drive holding Linux Mint's medium. */
    private static MainframeBlockEntity mainframeWithInstallMedia(final GameTestHelper helper, final BlockPos pos) {
        final MainframeBlockEntity mainframe = bareMainframe(helper, pos);
        final BlockPos readerPos = pos.east();
        helper.setBlock(readerPos, ComputingModule.CD_DRIVE.get());
        if (!(helper.getBlockEntity(readerPos) instanceof MediaReaderBlockEntity reader)) {
            throw new IllegalStateException("no reader at " + readerPos);
        }
        final ItemStack disc = new ItemStack(ComputingModule.CD_ROM.get());
        MediaItem.setKind(disc, MediaKind.OS_INSTALL);
        MediaItem.setPayload(disc, MINT);
        reader.mediaSlot().setStackInSlot(0, disc);
        return mainframe;
    }

    private static MainframeBlockEntity bareMainframe(final GameTestHelper helper, final BlockPos pos) {
        helper.setBlock(pos, ComputingModule.MAINFRAME.get());
        if (!(helper.getBlockEntity(pos) instanceof MainframeBlockEntity mainframe)) {
            throw new IllegalStateException("no Mainframe at " + pos);
        }
        final ItemStackHandler inv = mainframe.getInventory();
        inv.setStackInSlot(MainframeBlockEntity.MOTHERBOARD_SLOT,
                new ItemStack(ComputingModule.MOTHERBOARD_MTX_S_2011.get()));
        inv.setStackInSlot(MainframeBlockEntity.CPU_SLOTS_START, new ItemStack(ComputingModule.CPU_SERVO_2620.get()));
        inv.setStackInSlot(MainframeBlockEntity.RAM_SLOTS_START, new ItemStack(ComputingModule.RAM_DDR3_8192.get()));
        inv.setStackInSlot(MainframeBlockEntity.PSU_SLOT, new ItemStack(ComputingModule.PSU_650G.get()));
        inv.setStackInSlot(MainframeBlockEntity.DISK_SLOTS_START,
                new ItemStack(ComputingModule.disk(StorageTier.HDD, DiskSize.GB_500)));
        mainframe.togglePower();
        return mainframe;
    }
}
