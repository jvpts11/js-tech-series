/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.machine.DiskTools;
import dev.jstech.computers.os.disk.DiskCommand;
import dev.jstech.computers.os.disk.DiskPartitions;
import dev.jstech.computers.os.disk.DiskReport;
import dev.jstech.computers.os.disk.DiskSnapshots;
import dev.jstech.computers.os.disk.PartitionScheme;
import dev.jstech.computers.os.disk.ZfsMember;
import dev.jstech.computers.os.disk.ZfsMirrors;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.cli.CliCommands;
import dev.jstech.computers.program.cli.CliShell;
import dev.jstech.computers.storage.DriveVolumes;
import dev.jstech.computers.storage.LocalStore;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.computers.storage.StorageVolume;
import dev.jstech.computers.storage.StorageVolumes;
import dev.jstech.computers.storage.VolumeSnapshot;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.computers.TestCli;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Snapshots and mirrors (computers/docs/design/storage-devices.md): a btrfs snapshot weighs nothing when taken and what
 * changed since, and set as the default it is what the partition holds from the next start; ZFS rolls back at once,
 * past later snapshots only when told to let them go; a ZFS mirror repairs a damaged part from its other member as it
 * reads it and when it scrubs, and with its other member gone repairs nothing. The snapshots outlive a save.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SnapshotGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos AT = new BlockPos(4, 2, 4);
    private static final ResourceLocation UBUNTU = ResourceLocation.fromNamespaceAndPath("jsc", "ubuntu");
    private static final ResourceLocation FREEBSD = ResourceLocation.fromNamespaceAndPath("jsc", "freebsd");
    private static final int SECOND = 1;

    private SnapshotGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void btrfs_snapshotWeighsWhatChangedAndTheNextStartMountsTheDefault(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = withSecondDisk(helper, UBUNTU);
        final StorageKey diamond = StorageKey.of(Items.DIAMOND);
        helper.startSequence()
                .thenExecute(() -> {
                    ask(pc, DiskCommand.create(SECOND, 0L, 1_024L, "btrfs"));
                    store(pc, 1, diamond, 40L);
                    final ServerCliComputer cli = new ServerCliComputer(pc, helper.getLevel());
                    final CliShell shell = CliCommands.shellFor(cli, 100);
                    helper.assertTrue(TestCli.contains(shell.run("btrfs subvolume snapshot /dev/sdb1"
                            + " /.snapshots/day4", cli), "Create a snapshot of '/dev/sdb1'"), "the snapshot is taken");
                    helper.assertTrue(volume(pc, 1).keptWeight() == 0L, "and weighs nothing while nothing changed");
                    take(pc, 1, diamond, 30L);
                    helper.assertTrue(volume(pc, 1).keptWeight() == diamond.weight(30L),
                            "it weighs what changed since: " + volume(pc, 1).keptWeight());
                    helper.assertTrue(volume(pc, 1).usedWeight() == diamond.weight(40L),
                            "which takes room on the disk as the items did");
                    helper.assertTrue(TestCli.contains(shell.run("btrfs subvolume list /dev/sdb1", cli),
                            "path .snapshots/day4"), "the list names it");
                    final CliShell.Response du = shell.run("btrfs filesystem du -s /.snapshots/day4", cli);
                    helper.assertTrue(TestCli.contains(du, "Exclusive") && TestCli.contains(du, "/.snapshots/day4"),
                            "du reads what it keeps");
                    shell.run("btrfs subvolume set-default 258 /dev/sdb1", cli);
                    helper.assertTrue(volume(pc, 1).count(diamond) == 10L, "set-default changes nothing yet");
                    pc.openBootPartitions();
                    helper.assertTrue(volume(pc, 1).count(diamond) == 40L,
                            "the next start mounts the snapshot: " + volume(pc, 1).count(diamond));
                    helper.assertTrue(volume(pc, 1).startsFrom() == null, "and the start after that does again");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void zfs_rollsBackAtOncePastLaterSnapshotsOnlyWithR(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = withSecondDisk(helper, FREEBSD);
        final StorageKey diamond = StorageKey.of(Items.DIAMOND);
        helper.startSequence()
                .thenExecute(() -> {
                    ask(pc, DiskCommand.create(SECOND, 0L, 1_024L, "zfs"));
                    store(pc, 1, diamond, 20L);
                    final ServerCliComputer cli = new ServerCliComputer(pc, helper.getLevel());
                    final CliShell shell = CliCommands.shellFor(cli, 100);
                    shell.run("zfs snapshot ada1p1@one", cli);
                    take(pc, 1, diamond, 20L);
                    shell.run("zfs snapshot ada1p1@two", cli);
                    final CliShell.Response refused = shell.run("zfs rollback ada1p1@one", cli);
                    helper.assertTrue(TestCli.contains(refused, "more recent snapshots")
                            && TestCli.contains(refused, "ada1p1@two"), "a later snapshot stops the rollback");
                    helper.assertTrue(volume(pc, 1).count(diamond) == 0L, "and nothing came back");
                    shell.run("zfs rollback -r ada1p1@one", cli);
                    helper.assertTrue(volume(pc, 1).count(diamond) == 20L, "-r rolls back at once");
                    final CliShell.Response listed = shell.run("zfs list -t snapshot", cli);
                    helper.assertTrue(TestCli.contains(listed, "ada1p1@one") && !TestCli.contains(listed, "@two"),
                            "letting the later one go");
                })
                .thenSucceed();
    }

    /** A partition keeps no more snapshots than its most, and zfs says why the one past it is not taken. */
    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void zfs_keepsNoMoreSnapshotsThanItsMost(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = withSecondDisk(helper, FREEBSD);
        helper.startSequence()
                .thenExecute(() -> {
                    ask(pc, DiskCommand.create(SECOND, 0L, 1_024L, "zfs"));
                    for (int i = 0; i < DiskSnapshots.MOST_KEPT; i++) {
                        helper.assertTrue(report(pc, DiskCommand.snapshot(SECOND, 1, "s" + i)).refusal() == null,
                                "snapshot " + i + " is taken");
                    }
                    helper.assertTrue(report(pc, DiskCommand.snapshot(SECOND, 1, "one_more")).refusal()
                                    == DiskReport.Refusal.TOO_MANY_SNAPSHOTS,
                            "the one past the most is refused");
                    helper.assertTrue(volume(pc, 1).snapshots().size() == DiskSnapshots.MOST_KEPT,
                            "and is not kept");
                    final ServerCliComputer cli = new ServerCliComputer(pc, helper.getLevel());
                    final CliShell.Response said = CliCommands.shellFor(cli, 100).run("zfs snapshot ada1p1@again", cli);
                    helper.assertTrue(TestCli.contains(said, "the most a partition keeps"),
                            "zfs says why; it said " + said);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void mirror_repairsOnReadAndOnScrubAndNotWithoutItsOtherMember(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = withSecondDisk(helper, FREEBSD);
        final StorageKey diamond = StorageKey.of(Items.DIAMOND);
        final StorageKey emerald = StorageKey.of(Items.EMERALD);
        helper.startSequence()
                .thenExecute(() -> {
                    ask(pc, DiskCommand.create(SECOND, 0L, 1_024L, "zfs"));
                    ask(pc, DiskCommand.create(SECOND, 1_024L, 1_024L, "zfs"));
                    ZfsMirrors.pair(pc.diskInSlot(SECOND), 1, pc.diskInSlot(SECOND), 2);
                    store(pc, 1, diamond, 50L);
                    store(pc, 1, emerald, 50L);
                    corrupt(pc, 1);
                    helper.assertTrue(volume(pc, 1).corrupted().containsKey(diamond), "corruption hit the pool");
                    final LocalStore local = new LocalStore(disks(pc), () -> { });
                    local.extract(diamond, 1L);
                    helper.assertTrue(!volume(pc, 1).corrupted().containsKey(diamond)
                            && volume(pc, 1).corrupted().containsKey(emerald),
                            "reading the damaged part repairs it, and only it");
                    final ServerCliComputer cli = new ServerCliComputer(pc, helper.getLevel());
                    final CliShell shell = CliCommands.shellFor(cli, 100);
                    shell.run("zpool scrub ada1p1", cli);
                    helper.assertTrue(volume(pc, 1).corrupted().isEmpty(), "the scrub repairs the rest");
                    final CliShell.Response status = shell.run("zpool status", cli);
                    helper.assertTrue(TestCli.contains(status, "mirror-0") && TestCli.contains(status, "ada1p2")
                            && TestCli.contains(status, "scrub repaired") && TestCli.contains(status, "(repaired)")
                            && TestCli.contains(status, "No known data errors"), "and the status counts it");
                    final ZfsMember member = ZfsMirrors.memberOf(DiskPartitions.view(pc.diskInSlot(SECOND), 1));
                    helper.assertTrue(member != null && member.repairs() == 2, "two repairs: " + member);
                    DiskPartitions.wipe(pc.diskInSlot(SECOND), 2);
                    corrupt(pc, 1);
                    shell.run("zpool scrub ada1p1", cli);
                    helper.assertTrue(!volume(pc, 1).corrupted().isEmpty(),
                            "without the other member nothing is repaired");
                    final CliShell.Response degraded = shell.run("zpool status ada1p1", cli);
                    helper.assertTrue(TestCli.contains(degraded, "DEGRADED") && TestCli.contains(degraded, "REMOVED")
                            && TestCli.contains(degraded, "data errors"), "and the pool says so");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void snapshots_outliveASave(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = withSecondDisk(helper, UBUNTU);
        final StorageKey diamond = StorageKey.of(Items.DIAMOND);
        helper.startSequence()
                .thenExecute(() -> {
                    ask(pc, DiskCommand.create(SECOND, 0L, 1_024L, "btrfs"));
                    store(pc, 1, diamond, 12L);
                    ask(pc, DiskCommand.snapshot(SECOND, 1, "kept"));
                    take(pc, 1, diamond, 12L);
                    final StorageVolumes store = StorageVolumes.get(helper.getLevel().getServer());
                    final CompoundTag saved = store.save(new CompoundTag(), helper.getLevel().registryAccess());
                    final StorageVolumes loaded = StorageVolumes.factory().deserializer().apply(saved,
                            helper.getLevel().registryAccess());
                    final StorageVolume again = loaded.find(volume(pc, 1).id());
                    helper.assertTrue(again != null && again.snapshots().size() == 1, "the snapshot is read back");
                    final VolumeSnapshot kept = again.snapshots().get(0);
                    helper.assertTrue("kept".equals(kept.name()) && kept.items().get(diamond) == 12L
                            && again.keptWeight() == diamond.weight(12L), "with what it kept and what it weighs");
                })
                .thenSucceed();
    }

    /* A running PC of that system, and a second disk beside its own with an empty table to make partitions in. */
    private static PersonalComputerBlockEntity withSecondDisk(final GameTestHelper helper,
                                                             final ResourceLocation system) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper)
                .placeRunningPersonalComputer(AT, system);
        pc.getHardware().setStackInSlot(PersonalComputerBlockEntity.DISK_SLOTS_START + SECOND,
                new ItemStack(ComputingModule.disk(StorageTier.SSD, DiskSize.GB_500)));
        ask(pc, DiskCommand.table(SECOND, PartitionScheme.GPT));
        return pc;
    }

    private static void ask(final PersonalComputerBlockEntity pc, final String path) {
        DiskTools.reply(pc, pc.getLevel(), path);
    }

    private static DiskReport report(final PersonalComputerBlockEntity pc, final String path) {
        return DiskReport.read(DiskTools.reply(pc, pc.getLevel(), path));
    }

    private static StorageVolume volume(final PersonalComputerBlockEntity pc, final int number) {
        return DriveVolumes.peek(DiskPartitions.view(pc.diskInSlot(SECOND), number));
    }

    private static void store(final PersonalComputerBlockEntity pc, final int number, final StorageKey key,
                              final long amount) {
        final ItemStack view = DiskPartitions.view(pc.diskInSlot(SECOND), number);
        final StorageVolume volume = DriveVolumes.of(view);
        volume.add(key, amount);
        DriveVolumes.refreshUsage(view, volume);
        DiskPartitions.commit(pc.diskInSlot(SECOND), view);
    }

    private static void take(final PersonalComputerBlockEntity pc, final int number, final StorageKey key,
                             final long amount) {
        final ItemStack view = DiskPartitions.view(pc.diskInSlot(SECOND), number);
        final StorageVolume volume = DriveVolumes.peek(view);
        volume.take(key, amount);
        DriveVolumes.refreshUsage(view, volume);
        DiskPartitions.commit(pc.diskInSlot(SECOND), view);
    }

    /* Corrupts half of everything the partition holds, the dice always falling on it. */
    private static void corrupt(final PersonalComputerBlockEntity pc, final int number) {
        final ItemStack view = DiskPartitions.view(pc.diskInSlot(SECOND), number);
        final StorageVolume volume = DriveVolumes.peek(view);
        volume.corrupt(Map.copyOf(volume.items()), 0.5, () -> 0.0);
        DriveVolumes.refreshUsage(view, volume);
        DiskPartitions.commit(pc.diskInSlot(SECOND), view);
    }

    private static List<ItemStack> disks(final PersonalComputerBlockEntity pc) {
        final List<ItemStack> out = new ArrayList<>();
        for (int slot = 0; slot < pc.diskSlots(); slot++) {
            out.add(pc.diskInSlot(slot));
        }
        return out;
    }
}
