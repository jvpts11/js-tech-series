/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.gui.CdeBackdrop;
import dev.jstech.computers.gui.CdeStyle;
import dev.jstech.computers.gui.layout.CdeFrontPanelLayout;
import dev.jstech.computers.operation.payload.DesktopWindowsPayload;
import dev.jstech.computers.operation.payload.OpenSystemBootPayload;
import dev.jstech.computers.operation.payload.WorkstationInfoPayload;
import dev.jstech.computers.operation.payload.desktop.WorkstationInfoPayloads;
import dev.jstech.computers.os.DesktopEnvironmentDef;
import dev.jstech.computers.os.KernelNames;
import dev.jstech.computers.os.OpenWindow;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.PanelStyle;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.os.WorkspaceSet;
import dev.jstech.computers.os.WorkstationFacts;
import dev.jstech.computers.os.boot.BootController;
import dev.jstech.computers.os.boot.BootIdentity;
import dev.jstech.computers.os.boot.BootSequence;
import dev.jstech.computers.os.boot.BootSplash;
import dev.jstech.computers.os.install.Installers;
import dev.jstech.computers.os.media.MediaItem;
import dev.jstech.computers.os.media.MediaKind;
import dev.jstech.computers.os.media.MediaReaderBlockEntity;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.CliCommands;
import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.cli.CliShell;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import io.netty.buffer.Unpooled;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * CDE is a desktop like the others are: a package that turns a terminal system into a graphical one. What is
 * its own is where it runs, which is UNIX and FreeBSD and nowhere else, and how each of the two comes by it.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class CdeGameTests {

    private static final String ARENA = "empty";

    private static final BlockPos WHERE = new BlockPos(2, 2, 2);

    private static final int SETTLE = 4;

    private static final ResourceLocation CDE = jsc("cde");

    private CdeGameTests() {
    }

    /** The desktop is registered with its own panel, its maker and the plain names it gives what it bundles. */
    @GameTest(template = ARENA)
    public static void cde_isADesktopWithItsOwnPanelAndItsOwnNames(final GameTestHelper helper) {
        final DesktopEnvironmentDef desktop = OsRegistry.getDesktop(CDE);
        helper.assertTrue(desktop != null && desktop.panelStyle() == PanelStyle.CDE, "CDE draws its own panel");
        final ProgramSpec spec = OsRegistry.getProgram(CDE);
        helper.assertTrue(spec != null && spec.platforms().contains(Platform.UNIX)
                        && spec.platforms().contains(Platform.FREEBSD) && spec.platforms().contains(Platform.LINUX)
                        && !spec.platforms().contains(Platform.FRAMES),
                "it runs on every Unix family and on nothing else: " + (spec == null ? "none" : spec.platforms()));
        helper.assertTrue("File Manager".equals(desktop.nameOf(OsRegistry.getProgram(jsc("files"))))
                        && "Style Manager".equals(desktop.nameOf(OsRegistry.getProgram(jsc("settings")))),
                "and it calls its programs what it always called them");
        final ProgramSpec plasma = OsRegistry.getProgram(jsc("kde_plasma"));
        helper.assertFalse(plasma.platforms().contains(Platform.UNIX), "no later desktop runs on UNIX");
        helper.succeed();
    }

    /** UNIX takes it from a medium with installpkg, and then comes up at a desktop instead of a terminal. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void unix_takesCdeFromItsMediumAndComesUpAtADesktop(final GameTestHelper helper) {
        final MainframeBlockEntity machine = machine(helper, "unix");
        final MediaReaderBlockEntity reader = drive(helper);
        if (machine == null || reader == null) {
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 4, () -> {
                    helper.assertTrue(BootController.targetForComputer(machine)
                            == BootController.BootTarget.TERMINAL_ONLY, "with no desktop it is a terminal");
                    final ItemStack disc = new ItemStack(ComputingModule.CD_ROM.get());
                    MediaItem.setKind(disc, MediaKind.PROGRAM_INSTALL);
                    MediaItem.setPayload(disc, CDE);
                    reader.mediaSlot().setStackInSlot(0, disc);
                    final ServerCliComputer cli = new ServerCliComputer(machine, helper.getLevel());
                    final CliShell shell = CliCommands.shellFor(cli, 52);
                    final String said = text(shell.run("installpkg", cli));
                    helper.assertTrue(said.contains("Installing the cde package."), "installpkg takes it; got " + said);
                    TestWorldBuilder.finishSetup(machine, helper.getLevel(), helper.absolutePos(WHERE));
                    helper.assertTrue(CDE.equals(machine.installedDesktopId()),
                            "the desktop the machine comes up at is CDE; got " + machine.installedDesktopId());
                    // A machine that is up keeps the session it booted; the restart is what applies the desktop.
                    helper.assertTrue(BootController.targetForComputer(machine)
                            == BootController.BootTarget.TERMINAL_ONLY, "a running console does not grow a desktop");
                    machine.setBootedDesktopId(machine.installedDesktopId());
                    helper.assertTrue(BootController.targetForComputer(machine)
                            == BootController.BootTarget.FULL_DESKTOP, "and after a restart it comes up at CDE");
                })
                .thenSucceed();
    }

    /** FreeBSD takes it from its packages, beside the desktops it already could. */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void freebsd_takesCdeFromItsPackages(final GameTestHelper helper) {
        final MainframeBlockEntity machine = machine(helper, "freebsd");
        if (machine == null) {
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ServerCliComputer cli = new ServerCliComputer(machine, helper.getLevel());
                    final CliShell shell = CliCommands.shellFor(cli, 52);
                    machine.installMirror();
                    final String said = text(shell.run("pkg install cde", cli));
                    helper.assertTrue(said.contains("New packages to be INSTALLED:"), "pkg takes it; got " + said);
                    TestWorldBuilder.finishSetup(machine, helper.getLevel(), helper.absolutePos(WHERE));
                    helper.assertTrue(CDE.equals(machine.installedDesktopId()),
                            "the desktop the machine comes up at is CDE; got " + machine.installedDesktopId());
                })
                .thenSucceed();
    }

    /**
     * A Linux distribution takes it too, as a real one can: from the Mirror, by the distribution's own package
     * manager, beside the desktops it already could, and then it comes up at CDE.
     */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void linux_takesCdeFromItsPackageManager(final GameTestHelper helper) {
        final MainframeBlockEntity machine = machine(helper, "debian");
        if (machine == null) {
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ServerCliComputer cli = new ServerCliComputer(machine, helper.getLevel());
                    final CliShell shell = CliCommands.shellFor(cli, 52);
                    machine.installMirror();
                    final String said = text(shell.run("apt install cde", cli));
                    helper.assertTrue(!said.contains("Unable to locate") && !said.contains("not found"),
                            "apt finds the package; got " + said);
                    TestWorldBuilder.finishSetup(machine, helper.getLevel(), helper.absolutePos(WHERE));
                    helper.assertTrue(CDE.equals(machine.installedDesktopId()),
                            "the desktop the machine comes up at is CDE; got " + machine.installedDesktopId());
                    machine.setBootedDesktopId(machine.installedDesktopId());
                    helper.assertTrue(BootController.targetForComputer(machine)
                            == BootController.BootTarget.FULL_DESKTOP, "and after a restart it comes up at CDE");
                })
                .thenSucceed();
    }

    /**
     * Which workspace a window is on, and which one is up, are the machine's to keep: they come back from a
     * save, they cross the wire whole, and a machine that goes down comes up again on the first workspace.
     */
    @GameTest(template = ARENA)
    public static void workspaces_areMachineStateThatGoesWithThePower(final GameTestHelper helper) {
        final MainframeBlockEntity machine = machine(helper, "unix");
        if (machine == null) {
            return;
        }
        helper.assertTrue(WorkspaceSet.COUNT == CdeFrontPanelLayout.WORKSPACES,
                "the panel offers exactly the workspaces a window can be on");
        final int oneAndFour = WorkspaceSet.only(0) | WorkspaceSet.only(3);
        final List<OpenWindow> left = List.of(
                new OpenWindow("File Manager", 40, 30, 200, 140, false, false, "", WorkspaceSet.only(0)),
                new OpenWindow("Text Editor", 60, 50, 180, 120, false, false, "", WorkspaceSet.only(2)),
                new OpenWindow("Terminal", 20, 20, 220, 140, false, false, "", WorkspaceSet.EVERY),
                new OpenWindow("Calculator", 30, 30, 120, 140, true, false, "", oneAndFour));
        machine.setOpenWindows(left);
        machine.setDesktopWorkspace(2);

        final CompoundTag saved = machine.saveWithoutMetadata(helper.getLevel().registryAccess());
        machine.setOpenWindows(List.of());
        machine.setDesktopWorkspace(0);
        machine.loadWithComponents(saved, helper.getLevel().registryAccess());
        helper.assertTrue(machine.desktopWorkspace() == 2, "the workspace that was up comes back from a save");
        helper.assertTrue(machine.openWindows().equals(left), "with every window on the workspace it was left on: "
                + machine.openWindows());

        final RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(),
                helper.getLevel().registryAccess());
        DesktopWindowsPayload.STREAM_CODEC.encode(buf,
                DesktopWindowsPayload.of(machine.getBlockPos(), machine.openWindows(), machine.desktopWorkspace()));
        final DesktopWindowsPayload arrived = DesktopWindowsPayload.STREAM_CODEC.decode(buf);
        helper.assertTrue(arrived.workspace() == 2 && arrived.toOpenWindows().equals(left),
                "and the same crosses the wire whole, the windows on several workspaces included");

        helper.assertTrue(new OpenWindow("x", 0, 0, 10, 10, false, false, "", 1 << 9).on(0),
                "a window on no workspace a desktop has is put on the first, so it cannot be lost");
        helper.assertTrue(left.get(2).on(0) && left.get(2).on(3) && left.get(1).on(2) && !left.get(1).on(0),
                "a window shows on its own workspace, and one on all of them shows on each");
        helper.assertTrue(left.get(3).on(0) && left.get(3).on(3) && !left.get(3).on(1) && !left.get(3).on(2),
                "and one that occupies two of them shows on those two and on no other");

        machine.togglePower();
        helper.assertTrue(machine.openWindows().isEmpty() && machine.desktopWorkspace() == 0,
                "switched off, the machine keeps neither the windows nor the workspace they were sorted by");
        helper.succeed();
    }

    /**
     * The Style Manager's choice is the machine's to keep: set through the machine's settings, it comes back
     * from a save and is what the desktop is handed, and a style nobody could draw is kept as one that can be.
     */
    @GameTest(template = ARENA)
    public static void cdeStyle_isKeptWithTheMachine(final GameTestHelper helper) {
        final MainframeBlockEntity machine = machine(helper, "unix");
        if (machine == null) {
            return;
        }
        final CdeStyle chosen = CdeStyle.DEFAULT.withPalette("Desert").withBackdrop(1, CdeBackdrop.DOTS);
        final ServerCliComputer cli = new ServerCliComputer(machine, helper.getLevel());
        final String said = text(CliCommands.shellFor(cli, 52).run("config cdestyle " + chosen.encoded(), cli));
        helper.assertTrue(machine.console().desktop().cdeStyle().equals(chosen), "the setting takes the style; got "
                + machine.console().desktop().cdeStyle() + " after " + said);

        final CompoundTag saved = machine.saveWithoutMetadata(helper.getLevel().registryAccess());
        machine.console().desktop().setCdeStyle(CdeStyle.DEFAULT);
        machine.loadWithComponents(saved, helper.getLevel().registryAccess());
        helper.assertTrue(machine.console().desktop().cdeStyle().equals(chosen), "and it comes back from a save");

        CliCommands.shellFor(cli, 52).run("config cdestyle Nobody;no,such", cli);
        helper.assertTrue(machine.console().desktop().cdeStyle().equals(CdeStyle.DEFAULT),
                "a style nobody could draw is kept as the one a workstation starts with");
        helper.succeed();
    }

    /**
     * Workstation Info reads the machine as it stands: its host name, the network it is on by that network's id,
     * the system with its release and architecture word, CDE with its version, and its memory; and the facts
     * cross the wire whole.
     */
    @GameTest(template = ARENA)
    public static void workstationInfo_readsTheMachineAsItStands(final GameTestHelper helper) {
        final MainframeBlockEntity machine = machine(helper, "unix");
        if (machine == null) {
            return;
        }
        machine.console().install(CDE.toString());
        machine.setBootedDesktopId(CDE);
        final WorkstationFacts facts = WorkstationInfoPayloads.factsOf(machine);
        helper.assertTrue(facts.userName().equals("player") && facts.hostName().equals(Installers.hostName(machine)),
                "who and where; got " + facts.userName() + "@" + facts.hostName());
        final String network = machine.networkUuid() == null ? "" : machine.networkUuid().value().toString();
        helper.assertTrue(facts.network().equals(network), "the network by its id; got " + facts.network());
        helper.assertTrue(facts.system().equals("UNIX System V 3.2")
                        && facts.architecture().equals(KernelNames.architecture(Platform.UNIX, machine.processorBits()))
                        && facts.windowSystem().equals("CDE 2.5.2"),
                "the system, its architecture and CDE; got " + facts.system() + " / " + facts.architecture() + " / "
                        + facts.windowSystem());
        helper.assertTrue(facts.memoryMb() == machine.ramTotalMb() && facts.processorMhz() == machine.maxCpuMhz(),
                "and the hardware the machine has");

        final RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(),
                helper.getLevel().registryAccess());
        WorkstationInfoPayload.STREAM_CODEC.encode(buf, new WorkstationInfoPayload(machine.getBlockPos(), facts));
        helper.assertTrue(WorkstationInfoPayload.STREAM_CODEC.decode(buf).facts().equals(facts),
                "the facts cross the wire whole");
        helper.succeed();
    }

    /**
     * CDE's loading screen names the workstation it is starting on, so the system coming up has to tell the
     * monitor who it is: which desktop, which system, and the host name its prompt will say a moment later.
     */
    @GameTest(template = ARENA)
    public static void systemComingUp_tellsTheMonitorWhoItIs(final GameTestHelper helper) {
        final MainframeBlockEntity machine = machine(helper, "unix");
        if (machine == null) {
            return;
        }
        final BootIdentity who = new BootIdentity("cde", "UNIX System V", Installers.hostName(machine),
                CdeStyle.DEFAULT.withPalette("Desert").encoded());
        helper.assertTrue(!who.hostName().isEmpty(), "a machine with a system on it answers to a host name");

        final RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(),
                helper.getLevel().registryAccess());
        OpenSystemBootPayload.STREAM_CODEC.encode(buf, new OpenSystemBootPayload(machine.getBlockPos(),
                machine.getBlockPos(), 40, 100, BootSequence.NONE, false, BootSplash.PLAIN, who));
        final OpenSystemBootPayload arrived = OpenSystemBootPayload.STREAM_CODEC.decode(buf);
        helper.assertTrue(arrived.who().equals(who), "who is coming up crosses the wire whole; got " + arrived.who());
        helper.assertTrue(arrived.remainingTicks() == 40 && arrived.totalTicks() == 100 && !arrived.endsDark(),
                "and nothing beside it is disturbed by it");
        helper.succeed();
    }

    private static ResourceLocation jsc(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, path);
    }

    private static String text(final CliShell.Response response) {
        final StringBuilder out = new StringBuilder();
        for (final CliLine line : response.lines()) {
            out.append(line.text()).append('\n');
        }
        return out.toString();
    }

    /** A powered machine with a full build, a graphics card for its ports, and that system on its disk. */
    private static MainframeBlockEntity machine(final GameTestHelper helper, final String system) {
        helper.setBlock(WHERE, ComputingModule.MAINFRAME.get());
        if (!(helper.getBlockEntity(WHERE) instanceof MainframeBlockEntity machine)) {
            helper.fail("no machine at " + WHERE);
            return null;
        }
        TestWorldBuilder.installMainframeBuild(machine);
        machine.uninstallOs();
        machine.getInventory().setStackInSlot(MainframeBlockEntity.GPU_SLOTS_START,
                new ItemStack(ComputingModule.GPU_HD_7970.get()));
        if (!machine.installOs(jsc(system))) {
            helper.fail("could not install " + system);
            return null;
        }
        machine.togglePower();
        return machine;
    }

    private static MediaReaderBlockEntity drive(final GameTestHelper helper) {
        helper.setBlock(WHERE.east(), ComputingModule.CD_DRIVE.get());
        if (!(helper.getBlockEntity(WHERE.east()) instanceof MediaReaderBlockEntity reader)) {
            helper.fail("no drive beside the machine");
            return null;
        }
        return reader;
    }
}
