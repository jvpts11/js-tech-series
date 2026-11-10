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
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.operation.payload.program.UpdatePayloads;
import dev.jstech.computers.os.ProgramVersions;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.ICliPackages;
import dev.jstech.computers.program.cli.SigmaCommands;
import dev.jstech.computers.sigma.SigmaCompiler;
import dev.jstech.computers.sigma.SourceFile;
import dev.jstech.computers.sigma.pack.Manifest;
import dev.jstech.computers.sigma.pack.Packed;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.computers.TestShell;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * The "update available" mark (computers/docs/design/programs.md): what the Mirror has newer than what a machine has
 * installed, which each package manager lists with its own command and installs nothing while it does, and which an
 * Update installs as the manager's upgrade does, the packages players publish again among them.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class PackageUpdatesGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos WHERE = new BlockPos(2, 2, 2);
    /** Ticks for a machine's attachment to find what is beside it. */
    private static final int SETTLE = 4;

    /* The program made out of date for the managers to list, the version it is put back to, and its new one. */
    private static final String SCREENFETCH = rl("screenfetch").toString();
    private static final String OLD = "0.1";
    /* A player's package, published at one version and then again at the next. */
    private static final String THEIRS = "stockwatch";
    private static final String FIRST = "1.0.0";
    private static final String NEXT = "1.1.0";

    private PackageUpdatesGameTests() {
    }

    /** apt lists each update as its upgradable list does, with the machine's architecture. */
    @GameTest(template = ARENA)
    public static void apt_listsTheUpdatesAsUpgradable(final GameTestHelper helper) {
        listed(helper, "ubuntu", "apt list --upgradable",
                List.of("Listing... Done", "screenfetch/mirror " + now() + " x86_64 [upgradable from: " + OLD + "]"));
    }

    /** dnf says when it read the metadata, then a row per update with its release and the Mirror. */
    @GameTest(template = ARENA)
    public static void dnf_checkUpdateListsTheUpdates(final GameTestHelper helper) {
        listed(helper, "fedora", "dnf check-update",
                List.of("Last metadata expiration check: 0:02:11 ago.", "screenfetch.x86_64", now() + "-1"));
    }

    /** pacman writes each update from the version installed to the new one. */
    @GameTest(template = ARENA)
    public static void pacman_queryUpgradesListsTheUpdates(final GameTestHelper helper) {
        listed(helper, "arch", "pacman -Qu", List.of("screenfetch " + OLD + "-1 -> " + now() + "-1"));
    }

    /** emerge pretends the world update, each package under its category with both versions. */
    @GameTest(template = ARENA)
    public static void emerge_pretendsTheWorldUpdate(final GameTestHelper helper) {
        listed(helper, "gentoo", "emerge -pvuD @world",
                List.of("These are the packages that would be merged, in order:",
                        "[ebuild     U  ] app-misc/screenfetch-" + now() + "::mirror [" + OLD + "::mirror]"));
    }

    /** pkg compares each installed package with the remote one, listing only those behind it. */
    @GameTest(template = ARENA)
    public static void pkg_versionListsWhatNeedsUpdating(final GameTestHelper helper) {
        listed(helper, "freebsd", "pkg version -vRL=",
                List.of("screenfetch-" + OLD, "<  needs updating (remote has " + now() + ")"));
    }

    /** pkg_add's dry upgrade checks its quirks and says what it would bring up. */
    @GameTest(template = ARENA)
    public static void pkgAdd_dryUpgradeListsTheUpdates(final GameTestHelper helper) {
        listed(helper, "openbsd", "pkg_add -un",
                List.of("quirks-7.14 signed on", "screenfetch-" + OLD + "->" + now() + ": ok"));
    }

    /** Asking for the list installs nothing, and Update installs the one asked for, and only an update. */
    @GameTest(template = ARENA)
    public static void update_installsOnlyWhatIsAnUpdate(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = mainframe(helper, "ubuntu");
        mainframe.installMirror();
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    outdate(mainframe);
                    TestShell.shell(helper, mainframe, "apt list --upgradable");
                    helper.assertTrue(OLD.equals(mainframe.console().installedVersion(SCREENFETCH)),
                            "listing the updates installs nothing");
                    helper.assertTrue(UpdatePayloads.apply(mainframe, helper.getLevel(), List.of("jsc:no_such")) == 0,
                            "a name that is no update installs nothing");
                    helper.assertTrue(UpdatePayloads.apply(mainframe, helper.getLevel(), List.of(SCREENFETCH)) == 1,
                            "Update installs the update");
                    final String installed = mainframe.console().installedVersion(SCREENFETCH);
                    helper.assertTrue(now().equals(installed), "at the Mirror's version; got " + installed);
                    helper.assertTrue(updateOf(helper, mainframe, SCREENFETCH) == null, "and it is no update now");
                })
                .thenSucceed();
    }

    /** With no Mirror in reach there is nothing to list, and the manager says it cannot reach one. */
    @GameTest(template = ARENA)
    public static void list_withNoMirrorSaysSo(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = mainframe(helper, "ubuntu");
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    outdate(mainframe);
                    final ServerCliComputer cli = new ServerCliComputer(mainframe, helper.getLevel());
                    helper.assertTrue(cli.packageUpdates().isEmpty(), "no Mirror, no updates");
                    final List<String> said = TestShell.shell(helper, mainframe, "apt list --upgradable");
                    helper.assertTrue(TestShell.says(said, "could not resolve mirror://"),
                            "apt says it cannot reach the Mirror; got " + said);
                })
                .thenSucceed();
    }

    /**
     * A package a player publishes again under its name is an update: every manager lists it, and Update unpacks the
     * Mirror's copy over the one installed.
     */
    @GameTest(template = ARENA)
    public static void playerPackage_publishedAgainIsAnUpdate(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = mainframe(helper, "ubuntu");
        mainframe.installMirror();
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ServerCliComputer cli = new ServerCliComputer(mainframe, helper.getLevel());
                    mainframe.console().install(SigmaCommands.RUNTIME);
                    helper.assertTrue(mainframe.shelve(THEIRS, theirs(FIRST)), "the Mirror takes the package");
                    final ICliPackages.Installing installed = cli.packageInstall(THEIRS, false);
                    helper.assertTrue(installed.ok(), "it installs: " + installed.message().english());
                    helper.assertTrue(updateOf(helper, mainframe, THEIRS) == null, "at the Mirror's version");

                    helper.assertTrue(mainframe.shelve(THEIRS, theirs(NEXT)), "the player publishes it again");
                    final ICliPackages.PackageUpdate update = updateOf(helper, mainframe, THEIRS);
                    helper.assertTrue(update != null && FIRST.equals(update.installed())
                            && NEXT.equals(update.available()), "the next version is an update; got " + update);
                    final List<String> said = TestShell.shell(helper, mainframe, "apt list --upgradable");
                    helper.assertTrue(TestShell.says(said, THEIRS + "/mirror " + NEXT + " x86_64 [upgradable from: "
                            + FIRST + "]"), "apt lists it; got " + said);

                    helper.assertTrue(UpdatePayloads.apply(mainframe, helper.getLevel(), List.of(THEIRS)) == 1,
                            "Update installs it");
                    final String version = mainframe.console().communityProgram(THEIRS).version();
                    helper.assertTrue(NEXT.equals(version), "at the version published last; got " + version);
                    helper.assertTrue(updateOf(helper, mainframe, THEIRS) == null, "and it is no update now");
                })
                .thenSucceed();
    }

    /* Runs the command on a machine of that system with the Mirror and one program out of date, and reads it. */
    private static void listed(final GameTestHelper helper, final String system, final String command,
                               final List<String> expected) {
        final MainframeBlockEntity mainframe = mainframe(helper, system);
        mainframe.installMirror();
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    outdate(mainframe);
                    final List<String> said = TestShell.shell(helper, mainframe, command);
                    for (final String line : expected) {
                        helper.assertTrue(TestShell.says(said, line), command + " says " + line + "; got " + said);
                    }
                    helper.assertTrue(OLD.equals(mainframe.console().installedVersion(SCREENFETCH)),
                            command + " installs nothing");
                })
                .thenSucceed();
    }

    /* Puts screenfetch on the machine at a version older than the Mirror's. */
    private static void outdate(final MainframeBlockEntity mainframe) {
        mainframe.console().install(SCREENFETCH);
        mainframe.console().setInstalledVersion(SCREENFETCH, OLD);
    }

    /* The version the Mirror serves screenfetch at. */
    private static String now() {
        return ProgramVersions.of(SCREENFETCH);
    }

    /* The update the Mirror has for that package on the machine, or null when it has none. */
    private static ICliPackages.PackageUpdate updateOf(final GameTestHelper helper, final MainframeBlockEntity on,
                                                       final String id) {
        for (final ICliPackages.PackageUpdate update
                : new ServerCliComputer(on, helper.getLevel()).packageUpdates()) {
            if (update.id().equals(id)) {
                return update;
            }
        }
        return null;
    }

    /* A player's package at that version, as it comes off sgpack. */
    private static String theirs(final String version) {
        final SigmaCompiler.Result built = SigmaCompiler.compile(List.of(new SourceFile("Script.sgs",
                "using System.*; namespace Programs; class Watcher : IScript { public void OnInit() { }"
                        + " public void OnTick() { } public void OnDestroy() { } }")));
        if (!built.ok()) {
            throw new IllegalStateException(String.join("\n", built.lines()));
        }
        final Manifest manifest = new Manifest(THEIRS, version, "jvpts11", THEIRS + ".asm", "bell", 1,
                List.of(THEIRS + ".asm"), "Tells you when the iron runs low");
        return new Packed(manifest, Map.of(THEIRS + ".asm", built.assembly())).write();
    }

    /* A powered Mainframe with a full, 64-bit build and a disk, with that system installed on it. */
    private static MainframeBlockEntity mainframe(final GameTestHelper helper, final String system) {
        helper.setBlock(WHERE, ComputingModule.MAINFRAME.get());
        if (!(helper.getBlockEntity(WHERE) instanceof MainframeBlockEntity mainframe)) {
            throw new IllegalStateException("no Mainframe at " + WHERE);
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
        if (!mainframe.installOs(rl(system))) {
            throw new IllegalStateException("failed to install " + system + " on the test Mainframe");
        }
        return mainframe;
    }

    private static ResourceLocation rl(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, path);
    }
}
