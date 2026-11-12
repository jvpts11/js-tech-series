/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.backup.BackupArchive;
import dev.jstech.computers.backup.BackupContents;
import dev.jstech.computers.backup.BackupHolders;
import dev.jstech.computers.backup.BackupJob;
import dev.jstech.computers.backup.BackupPart;
import dev.jstech.computers.backup.BackupSchedule;
import dev.jstech.computers.backup.BackupService;
import dev.jstech.computers.backup.LossBook;
import dev.jstech.computers.backup.LossCause;
import dev.jstech.computers.backup.NetworkLosses;
import dev.jstech.computers.backup.TapeContents;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PartRefusals;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.blockentity.ServerServices;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.item.TapeItem;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.defrag.DefragJob;
import dev.jstech.computers.os.defrag.DefragRunner;
import dev.jstech.computers.os.defrag.DefragState;
import dev.jstech.computers.os.fs.BackupFile;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.os.fs.FilesystemContents;
import dev.jstech.computers.os.fs.StoredFile;
import dev.jstech.computers.program.Programs;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.IqlEngine;
import dev.jstech.computers.program.iql.IqlDefinition;
import dev.jstech.computers.program.iql.IqlSavedObject;
import dev.jstech.computers.rack.RackLayout;
import dev.jstech.computers.rack.RaidMode;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.computers.teracoin.TeracoinChain;
import dev.jstech.computers.teracoin.TeracoinState;
import dev.jstech.computers.teracoin.WalletFiles;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.text.Text;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.computers.NetworkFixtures;
import dev.jstech.tests.testkit.computers.ServerStacks;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.random.RandomGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Backup Service's hardware and what it keeps: the Tape Library of every era in a rack, the tapes it takes, and the
 * backups written to them and read back (computers/docs/design/programs.md). The tapes' room and winding, and the
 * drive's steps, are plain JUnit; what only a world answers is here.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class BackupGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 6;
    private static final BlockPos RACK = new BlockPos(2, 2, 2);
    /* The library's rows: a 2U unit topped at row 4 takes rows 4 and 5, so front slots 20 to 29. */
    private static final int LIBRARY_ROW = 4;
    private static final int FIRST_SLOT = LIBRARY_ROW * RackLayout.SLOTS_PER_U;
    private static final String JOB = "Nightly";
    private static final long DIAMONDS = 64L;
    /* Long enough for an LTO-6 to wind across the 64 items the backup takes, and more. */
    private static final int WIND = 60;
    /* The world's clock at two in the morning, its day starting at six, and one minute of it. */
    private static final long CLOCK_AT_TWO = 20_000L;
    private static final long MINUTE = 17L;
    /* Longer than the Automation Engine waits between two looks at its jobs. */
    private static final int EVALUATION = 25;

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void tapeLibrary_takesTenTapesOfItsOwnEraAndNothingElse(final GameTestHelper helper) {
        final ServerRackBlockEntity rack = TestWorldBuilder.forGameTest(helper).placeSeededRack(RACK);
        helper.assertTrue(rack.getServers().insertItem(LIBRARY_ROW, new ItemStack(ComputingModule.TAPE_LIBRARY.get()),
                false).isEmpty(), "the Standard's Tape Library mounts in a Standard rack");
        for (int slot = FIRST_SLOT; slot < FIRST_SLOT + 10; slot++) {
            helper.assertTrue(rack.roleOfFrontSlot(slot) == RackLayout.SlotRole.TAPE,
                    "its ten slots take tapes; slot " + slot + " is " + rack.roleOfFrontSlot(slot));
        }
        final ItemStack lto6 = new ItemStack(ComputingModule.LTO6_TAPE.get());
        final ItemStack dlt = new ItemStack(ComputingModule.DLT_TAPE.get());
        final ItemStack disk = new ItemStack(ComputingModule.FLOPPY_DISK.get());
        helper.assertTrue(rack.getFrontSlots().isItemValid(FIRST_SLOT, lto6), "an LTO-6 tape goes in");
        helper.assertFalse(rack.getFrontSlots().isItemValid(FIRST_SLOT, dlt), "a DLT tape of the Legacy does not");
        helper.assertTrue(rack.bayRefusal(FIRST_SLOT, dlt) == PartRefusals.TAPE_OF_ITS_ERA,
                "and the slot says the library reads only its own era's tapes");
        helper.assertTrue(rack.bayRefusal(FIRST_SLOT, disk) == PartRefusals.TAPE_SLOT,
                "and that only a tape goes in it");
        for (int slot = FIRST_SLOT; slot < FIRST_SLOT + 3; slot++) {
            helper.assertTrue(rack.getFrontSlots().insertItem(slot, lto6.copy(), false).isEmpty(),
                    "a tape goes into slot " + slot);
        }
        helper.assertTrue(rack.unitCodeAt(LIBRARY_ROW) == ServerRackBlockEntity.UNIT_TAPE_LIBRARY_VINTAGE
                        + HardwareEra.partsIndex(HardwareEra.STANDARD),
                "the rack draws the Standard's library face; got " + rack.unitCodeAt(LIBRARY_ROW));
        helper.succeed();
    }

    /* The book of losses */

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void deadDisk_writesEverythingItHeldInTheNetworksBook(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = NetworkFixtures.storageNetwork(helper);
        final long[] before = new long[1];
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ServerRackBlockEntity rack = NetworkFixtures.seededRack(helper);
                    rack.getServerStorage(0).insert(Items.DIAMOND, 64);
                    before[0] = helper.getLevel().getGameTime() - 1L;
                    // All of it corrupted at once is past nine tenths: every drive of the bay dies.
                    rack.corruptBay(0, 1.0, 1.0, helper.getLevel().random);
                    final long lost = lostOf(helper, mainframe, before[0], LossCause.DEAD_DISK, Items.DIAMOND);
                    helper.assertTrue(lost == 64L, "the 64 diamonds the dead disk held are in the book; got " + lost);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void raid0_writesTheWholeStripeInTheBookWhenAMemberIsPulled(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = NetworkFixtures.storageNetwork(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ServerRackBlockEntity rack = NetworkFixtures.seededRack(helper);
                    for (int slot = 0; slot < RackLayout.SLOTS_PER_U * 2; slot++) {
                        rack.getFrontSlots().setStackInSlot(slot, ItemStack.EMPTY);
                    }
                    rack.getServers().setStackInSlot(0, ServerStacks.raidStorageServer());
                    for (int i = 0; i < 3; i++) {
                        rack.insertDrive(0, new ItemStack(ComputingModule.disk(StorageTier.NVME, DiskSize.TB_1)));
                    }
                })
                .thenExecuteAfter(SETTLE, () -> {
                    final ServerRackBlockEntity rack = NetworkFixtures.seededRack(helper);
                    helper.assertTrue(rack.setRaidMode(0, RaidMode.RAID0), "the stripe forms");
                    rack.getServerStorage(0).insert(StorageKey.of(Items.COBBLESTONE), 900L);
                    final long before = helper.getLevel().getGameTime() - 1L;
                    rack.getFrontSlots().extractItem(0, 1, false);
                    final long lost = lostOf(helper, mainframe, before, LossCause.RAID0, Items.COBBLESTONE);
                    helper.assertTrue(lost == 900L, "the whole stripe's 900 stone are in the book; got " + lost);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void defrag_writesWhatItGaveUpOnInTheBook(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = NetworkFixtures.storageNetwork(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ServerRackBlockEntity rack = NetworkFixtures.seededRack(helper);
                    final IOsHost server = rack.unitHost(0);
                    server.installOs(ResourceLocation.fromNamespaceAndPath("jsc", "debian"));
                    rack.getServerStorage(0).insert(Items.STONE, 500);
                    final long before = helper.getLevel().getGameTime() - 1L;
                    // Past seven tenths corrupted, a pass brings next to nothing back off a solid-state drive.
                    rack.corruptBay(0, 0.85, 1.0, helper.getLevel().random);
                    helper.assertTrue(DefragRunner.begin(server, helper.getLevel(), 0).isEmpty(), "a pass starts");
                    final DefragJob job = server.console().defrag().job();
                    for (int i = 0; job != null && i < job.ticksTotal() && server.console().defrag().job() != null;
                         i++) {
                        DefragRunner.tick(server, helper.getLevel());
                    }
                    final DefragState.Result last = server.console().defrag().last();
                    helper.assertTrue(last != null && last.lost() > 0L, "the pass gave some up; got " + last);
                    // What the pass gave up of the stone is what the server no longer holds; its system files aside.
                    final long gone = 500L - rack.getServerStorage(0).count(Items.STONE);
                    final long lost = lostOf(helper, mainframe, before, LossCause.DEFRAG, Items.STONE);
                    helper.assertTrue(gone > 0L && lost == gone, "and every stone it gave up is in the book: " + gone
                            + " gone, " + lost + " written");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void iqlDrop_writesWhatItDestroysInTheBookAsItGoes(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = NetworkFixtures.storageNetwork(helper);
        final long[] before = new long[1];
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> NetworkFixtures.seededRack(helper).getServerStorage(0)
                        .insert(Items.DIRT, 40))
                // The network reads its storage once a tick, so the DROP is asked for once it sees the dirt.
                .thenExecuteAfter(2, () -> {
                    before[0] = helper.getLevel().getGameTime() - 1L;
                    final IqlEngine.Outcome outcome = NetworkFixtures.runIql(mainframe,
                            new ServerCliComputer(mainframe, helper.getLevel()), "DROP 30 minecraft:dirt");
                    helper.assertTrue(outcome.ok(), "the DROP is queued; got " + outcome.said().english());
                })
                .thenExecuteAfter(200, () -> {
                    final long left = NetworkFixtures.seededRack(helper).getServerStorage(0).count(Items.DIRT);
                    final long lost = lostOf(helper, mainframe, before[0], LossCause.DROP, Items.DIRT);
                    helper.assertTrue(lost == 30L && left == 10L, "the 30 dirt the DROP destroys are in the book"
                            + " and ten are left; got " + lost + " in the book, " + left + " left, book "
                            + NetworkLosses.of(helper.getLevel()).book(mainframe.networkUuid()).all());
                })
                .thenSucceed();
    }

    /* The Backup Service */

    @GameTest(template = ARENA, timeoutTicks = 800)
    public static void backupToTape_isWrittenAndARestoreGivesBackWhatWasLostOnce(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = NetworkFixtures.storageNetwork(helper);
        final UUID[] backup = new UUID[1];
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> withLibraryAndDiamonds(helper))
                .thenExecuteAfter(2, () -> {
                    final ServerServices.Host host = startService(helper, mainframe);
                    host.rack().backupsAt(host.slot()).put(job(BackupJob.Target.library(
                            NetworkFixtures.seededRack(helper).getBlockPos().asLong(), LIBRARY_ROW),
                            BackupJob.Schedule.BY_HAND));
                    final BackupService.Outcome outcome = BackupService.run(helper.getLevel(),
                            mainframe.networkUuid(), host, JOB);
                    helper.assertTrue(outcome.result() == BackupJob.RunState.RUNNING,
                            "a backup onto a tape waits for the library's drive; got " + outcome.said().english());
                })
                .thenWaitUntil(() -> helper.assertTrue(tape(helper).spots().size() == 1,
                        "the drive writes the backup onto the blank tape"))
                .thenExecute(() -> {
                    final TapeContents.Spot spot = tape(helper).spots().getFirst();
                    backup[0] = spot.backup();
                    helper.assertTrue(spot.items() == DIAMONDS && spot.size() == DIAMONDS,
                            "it keeps the 64 diamonds and takes 64 items of the tape; got " + spot);
                    helper.assertTrue(BackupArchive.of(helper.getLevel()).get(spot.backup()) != null,
                            "and the world keeps what it holds");
                    helper.assertTrue(job(helper).result() == BackupJob.RunState.DONE, "the job is done");
                    helper.assertTrue(NetworkFixtures.runIql(mainframe, new ServerCliComputer(mainframe,
                            helper.getLevel()), "DROP 30 minecraft:diamond").ok(), "30 diamonds are dropped");
                })
                .thenWaitUntil(() -> helper.assertTrue(diamonds(helper) == DIAMONDS - 30L, "the DROP ends"))
                .thenExecute(() -> {
                    final Text said = BackupService.restore(helper.getLevel(), mainframe.networkUuid(), backup[0],
                            EnumSet.of(BackupPart.ITEMS), Set.of());
                    helper.assertTrue(said.english().startsWith("Winding tape 1"),
                            "the tape winds back to the backup; got " + said.english());
                })
                .thenWaitUntil(() -> helper.assertTrue(diamonds(helper) == DIAMONDS,
                        "the restore gives the 30 dropped diamonds back; have " + diamonds(helper)))
                .thenExecute(() -> BackupService.restore(helper.getLevel(), mainframe.networkUuid(), backup[0],
                        EnumSet.of(BackupPart.ITEMS), Set.of()))
                .thenExecuteAfter(WIND, () -> helper.assertTrue(diamonds(helper) == DIAMONDS,
                        "a second restore of the same backup gives nothing more; have " + diamonds(helper)))
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 600)
    public static void backupToDrive_isAFileWeighingTheBackupAndRestoresAtOnce(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = NetworkFixtures.storageNetwork(helper);
        final int[] drive = new int[1];
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> withLibraryAndDiamonds(helper))
                .thenExecuteAfter(2, () -> {
                    final ServerServices.Host host = startService(helper, mainframe);
                    final List<ItemStack> drives = host.machine().diskStacks();
                    drive[0] = drives.indexOf(drives.stream().filter(stack -> !stack.isEmpty()).findFirst()
                            .orElseThrow());
                    host.rack().backupsAt(host.slot()).put(job(BackupJob.Target.disk(
                            host.machine().nodeUuid().value(), drive[0]), BackupJob.Schedule.BY_HAND));
                    final BackupService.Outcome outcome = BackupService.run(helper.getLevel(),
                            mainframe.networkUuid(), host, JOB);
                    helper.assertTrue(outcome.result() == BackupJob.RunState.DONE,
                            "a backup onto a drive is written at once; got " + outcome.said().english());
                    final ItemStack disk = host.machine().diskStacks().get(drive[0]);
                    final FilesystemContents fs = disk.getOrDefault(ComputingComponents.FILESYSTEM.get(),
                            FilesystemContents.EMPTY);
                    final StoredFile file = fs.files().values().stream()
                            .filter(one -> one.type() == FileType.BAK).findFirst().orElse(null);
                    helper.assertTrue(file != null && file.path().startsWith(BackupService.FOLDER + "/"),
                            "the backup is a file in the drive's Backups folder; got " + fs.files().keySet());
                    final BackupArchive.Backup kept = BackupArchive.of(helper.getLevel())
                            .get(BackupFile.read(file.content()).backup());
                    helper.assertTrue(kept != null && file.weight(DiskFilesystem.eraOf(disk)) == kept.weight(),
                            "and the file weighs on the drive what the backup weighs");
                    helper.assertTrue(NetworkFixtures.runIql(mainframe, new ServerCliComputer(mainframe,
                            helper.getLevel()), "DROP 10 minecraft:diamond").ok(), "10 diamonds are dropped");
                })
                .thenWaitUntil(() -> helper.assertTrue(diamonds(helper) == DIAMONDS - 10L, "the DROP ends"))
                .thenExecute(() -> {
                    final UUID id = BackupHolders.of(helper.getLevel(), mainframe.networkUuid()).getFirst().id();
                    final Text said = BackupService.restore(helper.getLevel(), mainframe.networkUuid(), id,
                            EnumSet.of(BackupPart.ITEMS), Set.of());
                    helper.assertTrue(said.english().startsWith("Restored: 10 items"),
                            "a backup on a drive comes back at once; got " + said.english());
                })
                .thenExecuteAfter(2, () -> helper.assertTrue(diamonds(helper) == DIAMONDS,
                        "with the dropped diamonds; have " + diamonds(helper)))
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 600)
    public static void backupOfWallets_givesBackAWalletWhoseFileWasLost(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = NetworkFixtures.storageNetwork(helper);
        final String[] wallet = new String[2];
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> withLibraryAndDiamonds(helper))
                .thenExecuteAfter(2, () -> {
                    final ServerServices.Host host = startService(helper, mainframe);
                    final IOsHost server = host.machine();
                    final TeracoinChain.Keys keys = TeracoinState.of(helper.getLevel().getServer()).chain()
                            .createWallet(RandomGenerator.getDefault());
                    wallet[0] = WalletFiles.write(server.services().files(), WalletFiles.folder(server), "savings",
                            keys, (char) 0);
                    wallet[1] = keys.address();
                    helper.assertTrue(wallet[0] != null, "the server keeps a wallet");
                    helper.assertTrue(BackupContents.keepsWallets(server), "and the Back up tab can tell it does");
                    final List<ItemStack> drives = server.diskStacks();
                    final int drive = drives.indexOf(drives.stream().filter(stack -> !stack.isEmpty()).findFirst()
                            .orElseThrow());
                    host.rack().backupsAt(host.slot()).put(new BackupJob(JOB, EnumSet.of(BackupPart.WALLETS),
                            BackupJob.Target.disk(server.nodeUuid().value(), drive), BackupJob.Schedule.BY_HAND, -1L,
                            BackupJob.RunState.NEVER));
                    helper.assertTrue(BackupService.run(helper.getLevel(), mainframe.networkUuid(), host, JOB)
                            .result() == BackupJob.RunState.DONE, "the wallets are backed up onto a drive at once");
                    helper.assertTrue(server.services().files().delete(wallet[0]), "and the wallet's file is lost");
                })
                .thenExecuteAfter(2, () -> {
                    final UUID id = BackupHolders.of(helper.getLevel(), mainframe.networkUuid()).getFirst().id();
                    BackupService.restore(helper.getLevel(), mainframe.networkUuid(), id,
                            EnumSet.of(BackupPart.WALLETS), Set.of());
                    final IOsHost server = startService(helper, mainframe).machine();
                    helper.assertTrue(WalletFiles.read(server.services().files(), wallet[0])
                                    .map(one -> one.address().equals(wallet[1])).orElse(false),
                            "the restore puts the wallet back where it was, with its address");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 600)
    public static void timedJob_isAnAutomationEngineJobThatFiresAtItsHour(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = NetworkFixtures.storageNetwork(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> withLibraryAndDiamonds(helper))
                .thenExecuteAfter(2, () -> {
                    final ServerServices.Host host = startService(helper, mainframe);
                    mainframe.installAutomationEngine();
                    final BackupJob nightly = job(BackupJob.Target.library(
                            NetworkFixtures.seededRack(helper).getBlockPos().asLong(), LIBRARY_ROW),
                            new BackupJob.Schedule(BackupJob.Schedule.Kind.DAILY, 2, 0));
                    host.rack().backupsAt(host.slot()).put(nightly);
                    helper.assertTrue(BackupSchedule.sync(helper.getLevel(), mainframe.networkUuid(), null, nightly),
                            "the Automation Engine runs to fire it");
                    final IqlSavedObject engineJob = mainframe.iqlCatalog().get(IqlDefinition.ObjectType.JOB,
                            BackupSchedule.engineJob(JOB));
                    helper.assertTrue(engineJob != null && engineJob.body().equals("BACKUP " + JOB)
                                    && engineJob.triggerSpec().equals("clock = '02:00'"),
                            "the Engine keeps a job that backs up at two; got " + engineJob);
                    helper.assertTrue(BackupSchedule.trigger(new BackupJob.Schedule(BackupJob.Schedule.Kind.WEEKLY, 3,
                                    6)).equals("clock = '03:00' AND weekday = 'Sunday'"),
                            "and a weekly one names its day");
                })
                /*
                 * A minute to two by the world's clock, then two, each held every tick for longer than the Engine
                 * waits between looks: other tests share the world and its clock.
                 */
                .thenExecuteFor(EVALUATION, () -> helper.getLevel().setDayTime(CLOCK_AT_TWO - MINUTE))
                .thenExecuteFor(EVALUATION, () -> helper.getLevel().setDayTime(CLOCK_AT_TWO))
                .thenWaitUntil(() -> helper.assertTrue(job(helper).result() == BackupJob.RunState.DONE
                                && tape(helper).spots().size() == 1,
                        "at two the Engine runs the backup; job " + job(helper)))
                .thenSucceed();
    }

    /* A Tape Library with one blank LTO-6 tape under the seeded server, which holds 64 diamonds and runs Debian. */
    private static void withLibraryAndDiamonds(final GameTestHelper helper) {
        final ServerRackBlockEntity rack = NetworkFixtures.seededRack(helper);
        rack.unitHost(0).installOs(ResourceLocation.fromNamespaceAndPath("jsc", "debian"));
        rack.getServers().insertItem(LIBRARY_ROW, new ItemStack(ComputingModule.TAPE_LIBRARY.get()), false);
        rack.getFrontSlots().insertItem(FIRST_SLOT, new ItemStack(ComputingModule.LTO6_TAPE.get()), false);
        rack.getServerStorage(0).insert(Items.DIAMOND, (int) DIAMONDS);
    }

    private static ServerServices.Host startService(final GameTestHelper helper,
                                                    final MainframeBlockEntity mainframe) {
        NetworkFixtures.seededRack(helper).consoleOf(0).install(Programs.BACKUP_SERVICE.toString());
        final ServerServices.Host host = BackupService.serviceOf(helper.getLevel(), mainframe.networkUuid());
        if (host == null) {
            throw new IllegalStateException("the seeded server runs no Backup Service");
        }
        return host;
    }

    private static BackupJob job(final BackupJob.Target target, final BackupJob.Schedule when) {
        return new BackupJob(JOB, EnumSet.of(BackupPart.ITEMS), target, when, -1L, BackupJob.RunState.NEVER);
    }

    private static BackupJob job(final GameTestHelper helper) {
        return NetworkFixtures.seededRack(helper).backupsAt(0).named(JOB);
    }

    private static TapeContents tape(final GameTestHelper helper) {
        return TapeItem.contents(NetworkFixtures.seededRack(helper).getFrontSlots().getStackInSlot(FIRST_SLOT));
    }

    private static long diamonds(final GameTestHelper helper) {
        return NetworkFixtures.seededRack(helper).getServerStorage(0).count(Items.DIAMOND);
    }

    /* How many of {@code item} the network's book has lost by {@code cause} after the game time {@code after}. */
    private static long lostOf(final GameTestHelper helper, final MainframeBlockEntity mainframe, final long after,
                               final LossCause cause, final Item item) {
        long lost = 0L;
        for (final LossBook.Loss<StorageKey> loss : NetworkLosses.of(helper.getLevel())
                .book(mainframe.networkUuid()).since(after)) {
            if (loss.cause() == cause && loss.key().item() == item) {
                lost += loss.left();
            }
        }
        return lost;
    }
}
