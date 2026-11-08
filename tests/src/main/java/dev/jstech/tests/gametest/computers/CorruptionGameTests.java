/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.config.ComputersServerConfig;
import dev.jstech.computers.eventlog.EventLevel;
import dev.jstech.computers.eventlog.NetworkEvent;
import dev.jstech.computers.eventlog.NetworkEventLog;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.operation.NetworkInsertOperation;
import dev.jstech.computers.os.boot.SystemIntegrity;
import dev.jstech.computers.os.defrag.DefragJob;
import dev.jstech.computers.os.defrag.DefragRunner;
import dev.jstech.computers.os.defrag.DefragState;
import dev.jstech.computers.storage.DiskCorruption;
import dev.jstech.computers.storage.DiskHealth;
import dev.jstech.computers.storage.DriveVolumes;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.computers.storage.StorageVolume;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestCables;
import dev.jstech.tests.testkit.computers.NetworkFixtures;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Storage corruption and the Defrag (computers/docs/design/storage.md): what corruption takes out of a disk and the
 * room it still takes, bad sectors past half and a dead disk past nine tenths, the settings that turn it off, a
 * system whose files it hit, the pass that gives back what it can and mends a quarter of the bad sectors, a pass
 * paused, the disk's doubled wait; and the network failing under Operations: a conflict that loses what they carried
 * and corrupts both networks' storage once, and a network split under a transfer.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class CorruptionGameTests {

    private static final String ARENA = "empty";
    private static final String NO_CORRUPTION = "jsc_corruption_off";
    private static final int SETTLE = 4;
    private static final int PATIENT = 1_200;
    private static final BlockPos PC = new BlockPos(2, 2, 2);
    private static final long STORED = 1_000L;
    /** An INSERT far beyond a tick of the server's writing, so it is in flight when the network fails. */
    private static final long DEMAND = 30_000L;
    private static final StorageKey STONE = StorageKey.of(Items.STONE);

    private CorruptionGameTests() {
    }

    @BeforeBatch(batch = NO_CORRUPTION)
    public static void corruptionOff(final ServerLevel level) {
        ComputersServerConfig.FILE.set(ComputersServerConfig.CORRUPTION, false);
    }

    @AfterBatch(batch = NO_CORRUPTION)
    public static void corruptionOn(final ServerLevel level) {
        ComputersServerConfig.FILE.set(ComputersServerConfig.CORRUPTION, true);
    }

    /** A tenth corrupted leaves the contents and waits, still taking its room; the drive's summary says so. */
    @GameTest(template = ARENA)
    public static void corrupt_movesAShareOutOfTheContentsAndKeepsItsRoom(final GameTestHelper helper) {
        final ItemStack disk = filledDisk(StorageTier.HDD);
        final StorageVolume volume = DriveVolumes.of(disk);
        final long before = volume.usedWeight();
        helper.assertTrue(DiskCorruption.corrupt(disk, 0.10, null, false, helper.getLevel().random),
                "the disk is corrupted");
        helper.assertValueEqual(volume.count(STONE), STORED - 100L, "a tenth leaves the contents");
        helper.assertValueEqual(volume.corrupted().get(STONE), 100L, "and waits among the corrupted");
        helper.assertValueEqual(volume.usedWeight(), before, "still taking its room until the Defrag");
        final DiskHealth health = DriveVolumes.health(disk);
        helper.assertTrue(health.corruptedItems() == 100L && health.damage() == 100 && health.badSectors() == 0,
                "the drive's summary carries it, with no bad sectors yet; got " + health);
        helper.succeed();
    }

    /** Past half, bad sectors take the capacity the damage goes over it by; past nine tenths the disk dies. */
    @GameTest(template = ARENA)
    public static void corrupt_pastHalfHurtsTheDiskAndPastNineTenthsKillsIt(final GameTestHelper helper) {
        final ItemStack disk = filledDisk(StorageTier.HDD);
        final StorageVolume volume = DriveVolumes.of(disk);
        DiskCorruption.corrupt(disk, 0.60, null, false, helper.getLevel().random);
        helper.assertValueEqual(volume.damage(), 600, "six tenths are corrupted");
        helper.assertValueEqual(volume.badSectors(), 100, "a tenth of the disk is lost to bad sectors");
        helper.assertValueEqual(DriveVolumes.usableCapacity(disk, 1_000L), 900L, "and it holds a tenth less");
        DiskCorruption.corrupt(disk, 1.0, null, false, helper.getLevel().random);
        helper.assertTrue(volume.dead() && DriveVolumes.health(disk).dead(), "all of it corrupted, the disk dies");
        helper.assertTrue(volume.items().isEmpty() && volume.corrupted().isEmpty(), "taking all it held");
        helper.assertValueEqual(DriveVolumes.usableCapacity(disk, 1_000L), 0L, "a dead disk holds nothing");
        DriveVolumes.erase(disk);
        helper.assertTrue(DriveVolumes.health(disk).dead(), "and formatting does not bring it back");
        helper.succeed();
    }

    /** With the setting off, nothing is ever corrupted. */
    @GameTest(template = ARENA, batch = NO_CORRUPTION)
    public static void corrupt_withTheSettingOffCorruptsNothing(final GameTestHelper helper) {
        final ItemStack disk = filledDisk(StorageTier.HDD);
        helper.assertFalse(DiskCorruption.corrupt(disk, 1.0, null, true, helper.getLevel().random),
                "nothing is corrupted");
        helper.assertValueEqual(DriveVolumes.of(disk).count(STONE), STORED, "the disk keeps everything");
        helper.succeed();
    }

    /** A Personal Computer's private part is out of reach: with nothing published, nothing is corrupted. */
    @GameTest(template = ARENA, timeoutTicks = PATIENT)
    public static void corrupt_neverReachesAPersonalComputersPrivatePart(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(PC);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ItemStack disk = pc.diskInSlot(0);
                    DriveVolumes.setPublicPermille(disk, 0);
                    DriveVolumes.of(disk).add(STONE, STORED);
                    helper.assertFalse(pc.corruptPublic(1.0, helper.getLevel().random),
                            "a conflict reaches nothing of a computer that published nothing");
                    helper.assertValueEqual(DriveVolumes.of(disk).count(STONE), STORED, "its items are untouched");
                    DriveVolumes.setPublicPermille(disk, 1000);
                    helper.assertTrue(pc.corruptPublic(0.10, helper.getLevel().random),
                            "what it publishes is within reach");
                    helper.assertValueEqual(DriveVolumes.of(disk).corrupted().get(STONE), 100L, "a tenth of it");
                })
                .thenSucceed();
    }

    /** Corruption that hits the file that starts the system leaves a system that will not start. */
    @GameTest(template = ARENA, timeoutTicks = PATIENT)
    public static void corrupt_aSystemMissingItsFilesWillNotStart(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(PC);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(SystemIntegrity.check(pc).whole(), "the system starts as it is");
                    DiskCorruption.corrupt(pc.systemDisk(), 1.0, null, true, helper.getLevel().random);
                    helper.assertFalse(SystemIntegrity.check(pc).whole(), "with its files corrupted, it does not");
                    helper.assertFalse(DriveVolumes.of(pc.systemDisk()).corruptedFiles().files().isEmpty(),
                            "they wait for the Defrag on the volume");
                })
                .thenSucceed();
    }

    /**
     * A pass over a hard disk tries every corrupted item once: what comes back is part of the disk again, the rest is
     * lost, nothing waits any more, a quarter of the bad sectors are mended, and the pass's end is kept.
     */
    @GameTest(template = ARENA, timeoutTicks = PATIENT)
    public static void defrag_givesBackOrLosesEveryCorruptedItemAndMendsAQuarter(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(PC);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(pc.isRunning() && pc.installedOs() != null, "it runs"))
                .thenExecute(() -> {
                    final ItemStack disk = pc.systemDisk();
                    final StorageVolume volume = DriveVolumes.of(disk);
                    volume.add(STONE, STORED);
                    DiskCorruption.corrupt(disk, 0.60, null, false, helper.getLevel().random);
                    final int badBefore = volume.badSectors();
                    helper.assertTrue(DefragRunner.begin(pc, helper.getLevel(), 0).isEmpty(), "a pass starts");
                    final DefragJob job = pc.console().defrag().job();
                    helper.assertTrue(job != null && job.found() == 600L, "on the disk, with what it has to try");
                    for (int i = 0; i < job.ticksTotal() && pc.console().defrag().job() != null; i++) {
                        DefragRunner.tick(pc, helper.getLevel());
                    }
                    helper.assertTrue(pc.console().defrag().job() == null, "the pass runs its time and ends");
                    final DefragState.Result last = pc.console().defrag().last();
                    helper.assertTrue(last != null && last.found() == 600L
                            && last.recovered() + last.lost() == last.found(), "every corrupted item is tried; got "
                            + last);
                    helper.assertTrue(volume.corrupted().isEmpty(), "nothing waits any more");
                    helper.assertValueEqual(volume.count(STONE), 400L + last.recovered(),
                            "what came back is part of the disk again");
                    helper.assertValueEqual(volume.badSectors(), badBefore - (badBefore + 3) / 4,
                            "a quarter of the bad sectors are mended");
                })
                .thenSucceed();
    }

    /** A paused pass stands where it is, and the disk answers the network twice as slowly while a pass runs. */
    @GameTest(template = ARENA, timeoutTicks = PATIENT)
    public static void defrag_pausedStandsStillAndARunningPassSlowsTheDisk(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(PC);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(pc.isRunning() && pc.installedOs() != null, "it runs"))
                .thenExecute(() -> {
                    DriveVolumes.of(pc.systemDisk()).add(STONE, STORED);
                    DiskCorruption.corrupt(pc.systemDisk(), 0.10, null, false, helper.getLevel().random);
                    DefragRunner.begin(pc, helper.getLevel(), 0);
                    DefragRunner.tick(pc, helper.getLevel());
                    helper.assertValueEqual(DefragRunner.waitFactor(pc.nodeUuid(), helper.getLevel().getGameTime()),
                            2, "the disk's wait doubles while the pass runs");
                    DefragRunner.togglePause(pc);
                    final int done = pc.console().defrag().job().ticksDone();
                    for (int i = 0; i < 20; i++) {
                        DefragRunner.tick(pc, helper.getLevel());
                    }
                    helper.assertValueEqual(pc.console().defrag().job().ticksDone(), done, "paused, it stands still");
                    DefragRunner.stop(pc);
                    helper.assertTrue(pc.console().defrag().job() == null, "and stops when told");
                })
                .thenSucceed();
    }

    /**
     * Two Mainframes on one network: the Operation in flight loses what it carried, the log says so, and the storage
     * of the network is corrupted a tenth, once for the whole conflict.
     */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void conflict_losesWhatWasCarriedAndCorruptsTheStorageOnce(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = NetworkFixtures.storageNetwork(helper);
        final ServerRackBlockEntity rack = NetworkFixtures.seededRack(helper);
        final NetworkInsertOperation[] op = new NetworkInsertOperation[1];
        final NetworkUuid[] network = new NetworkUuid[1];
        final long[] corrupted = new long[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(mainframe.isRunning() && mainframe.networkUuid() != null,
                        "the Mainframe runs its network"))
                .thenExecuteAfter(SETTLE, () -> {
                    network[0] = mainframe.networkUuid();
                    final ItemStack drive = rack.claimedDriveStacks(0).getFirst();
                    DriveVolumes.of(drive).add(STONE, STORED);
                    op[0] = mainframe.submitNetworkInsert(Items.COBBLESTONE, DEMAND, "test");
                    helper.assertTrue(op[0] != null, "the INSERT dispatches");
                })
                .thenExecuteAfter(1, () -> {
                    TestCables.lay(helper, new BlockPos(2, 2, 3), ComputingModule.HBW_CABLE);
                    NetworkFixtures.placeRunningMainframe(helper, new BlockPos(2, 2, 4));
                })
                .thenWaitUntil(() -> helper.assertTrue(mainframe.hasNetworkConflict(), "the networks conflict"))
                .thenExecuteAfter(2, () -> {
                    helper.assertTrue(op[0].isDone() && op[0].leftover() == 0L,
                            "the INSERT settles with nothing to give back; leftover " + op[0].leftover());
                    helper.assertFalse(events(helper, network[0], "Lost in transit in the network conflict: ")
                            .isEmpty(), "the log says what was lost");
                    corrupted[0] = corruptedOn(rack);
                    helper.assertTrue(corrupted[0] > 0L, "the server's storage is corrupted");
                    helper.assertFalse(events(helper, network[0], "The network conflict corrupted").isEmpty(),
                            "and the log says so");
                })
                .thenExecuteAfter(20, () -> helper.assertValueEqual(corruptedOn(rack), corrupted[0],
                        "once for the whole conflict, not every tick"))
                .thenSucceed();
    }

    /** A cable pulled between the Mainframe and the server it writes: the INSERT loses what it carried. */
    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void split_losesWhatTheTransferCarried(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = NetworkFixtures.storageNetwork(helper);
        final NetworkInsertOperation[] op = new NetworkInsertOperation[1];
        final NetworkUuid[] network = new NetworkUuid[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(mainframe.isRunning() && mainframe.networkUuid() != null,
                        "the Mainframe runs its network"))
                .thenExecuteAfter(SETTLE, () -> {
                    network[0] = mainframe.networkUuid();
                    op[0] = mainframe.submitNetworkInsert(Items.COBBLESTONE, DEMAND, "test");
                    helper.assertTrue(op[0] != null, "the INSERT dispatches");
                })
                .thenWaitUntil(() -> helper.assertTrue(op[0].writtenTotal() > 0L, "it starts writing"))
                .thenExecute(() -> helper.setBlock(new BlockPos(2, 2, 2), Blocks.AIR))
                .thenWaitUntil(() -> helper.assertTrue(op[0].isDone(), "the INSERT settles once the network splits"))
                .thenExecute(() -> {
                    helper.assertValueEqual(op[0].leftover(), 0L, "with nothing left to give back");
                    helper.assertFalse(events(helper, network[0], "Lost in transit when the network split: ")
                            .isEmpty(), "and the log says what was lost");
                })
                .thenSucceed();
    }

    /* A drive of {@code tier} holding a thousand stone, nothing corrupted. */
    private static ItemStack filledDisk(final StorageTier tier) {
        final ItemStack disk = new ItemStack(ComputingModule.disk(tier, DiskSize.GB_500));
        final StorageVolume volume = DriveVolumes.of(disk);
        volume.add(STONE, STORED);
        DriveVolumes.refreshUsage(disk, volume);
        return disk;
    }

    private static long corruptedOn(final ServerRackBlockEntity rack) {
        long total = 0L;
        for (final ItemStack drive : rack.claimedDriveStacks(0)) {
            total += DriveVolumes.health(drive).corruptedItems();
        }
        return total;
    }

    private static List<NetworkEvent> events(final GameTestHelper helper, final NetworkUuid network,
                                             final String startsWith) {
        return NetworkEventLog.of(helper.getLevel()).newest(network, helper.getLevel().getGameTime(), any -> true,
                        NetworkEventLog.MOST).stream()
                .filter(event -> event.level() == EventLevel.ERROR && event.message().english().startsWith(startsWith))
                .toList();
    }
}
