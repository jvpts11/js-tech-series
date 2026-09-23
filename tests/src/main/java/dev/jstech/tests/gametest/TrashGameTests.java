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
import dev.jstech.computers.operation.payload.DesktopFilesPayload;
import dev.jstech.computers.operation.payload.TrashActionPayload;
import dev.jstech.computers.operation.payload.TrashListingPayload;
import dev.jstech.computers.operation.payload.files.TrashPayloads;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.DiskTrash;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.os.fs.TrashFolder;
import dev.jstech.computers.os.fs.TrashKind;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A file deleted on a desktop waits in that desktop's trash, taking room on the disk, until it is put back or the
 * trash is emptied. Each family keeps it its own way: the Recycle Bin at the root on Frames, a Trash with a note per
 * file in the home on the Linux and FreeBSD desktops, and CDE's own Trash Can on whichever system runs it.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class TrashGameTests {

    private static final String ARENA = "empty";

    private static final BlockPos WHERE = new BlockPos(2, 2, 2);

    private static final ResourceLocation CDE = jsc("cde");

    private static final String FRAMES_DESK = "Users/Public/Desktop";

    private static final String LINUX_DESK = "home/player/Desktop";

    private TrashGameTests() {
    }

    /** Each desktop keeps its trash where its family always did, CDE included on a Linux. */
    @GameTest(template = ARENA)
    public static void trashOf_placesEachDesktopsTrashWhereItsFamilyKeepsIt(final GameTestHelper helper) {
        final MainframeBlockEntity frames = machine(helper, "frames_xp");
        if (frames == null) {
            return;
        }
        helper.assertTrue(TrashPayloads.trashOf(frames).equals(TrashFolder.of(TrashKind.RECYCLER, "")),
                "Frames keeps RECYCLER at the root; got " + TrashPayloads.trashOf(frames));
        final MainframeBlockEntity debian = machine(helper, "debian");
        helper.assertTrue(TrashPayloads.trashOf(debian).path().equals("home/player/.local/share/Trash"),
                "a Linux keeps it in the home; got " + TrashPayloads.trashOf(debian));
        debian.console().install(CDE.toString());
        debian.setBootedDesktopId(CDE);
        helper.assertTrue(TrashPayloads.trashOf(debian).path().equals("home/player/.dt/Trash"),
                "CDE on a Linux keeps its own; got " + TrashPayloads.trashOf(debian));
        final MainframeBlockEntity unix = machine(helper, "unix");
        unix.console().install(CDE.toString());
        unix.setBootedDesktopId(CDE);
        helper.assertTrue(TrashPayloads.trashOf(unix).path().equals("usr/player/.dt/Trash"),
                "and on UNIX, whose people live under /usr; got " + TrashPayloads.trashOf(unix));
        helper.succeed();
    }

    /**
     * On Frames a deleted file goes into RECYCLER under a serial name, INFO2 says where it came from, and restoring
     * puts it back under its own name with nothing left behind.
     */
    @GameTest(template = ARENA)
    public static void recycleBin_keepsAFileUntilItIsRestored(final GameTestHelper helper) {
        final MainframeBlockEntity machine = machine(helper, "frames_xp");
        if (machine == null) {
            return;
        }
        final ItemStack disk = machine.systemDisk();
        final TrashFolder bin = TrashPayloads.trashOf(machine);
        final String notes = FRAMES_DESK + "/notes.txt";
        write(helper, machine, notes, "a line");
        helper.assertTrue(DiskTrash.put(disk, bin, notes, machine.systemDiskFreeWeight(), 0L)
                == DiskTrash.Outcome.DONE, "the file goes into the bin");
        helper.assertFalse(DiskFilesystem.exists(disk, notes), "it has left the desktop");
        helper.assertTrue(DiskFilesystem.read(disk, "RECYCLER/Dc1.txt").orElse("").equals("a line"),
                "and waits in RECYCLER under a serial name, content whole");
        helper.assertTrue(DiskFilesystem.read(disk, "RECYCLER/INFO2").orElse("").contains(notes),
                "INFO2 says where it came from");
        final List<DiskTrash.Entry> inBin = DiskTrash.list(disk, bin);
        helper.assertTrue(inBin.size() == 1 && inBin.getFirst().original().equals(notes)
                && inBin.getFirst().stored().equals("Dc1.txt"), "the bin lists it with its old place; got " + inBin);
        helper.assertTrue(DiskTrash.holdsAnything(disk, bin), "which is what makes the bin look full");

        helper.assertTrue(DiskTrash.restore(disk, bin, "Dc1.txt", 0L), "restore puts it back");
        helper.assertTrue(DiskFilesystem.read(disk, notes).orElse("").equals("a line"), "under its own name");
        helper.assertTrue(DiskTrash.list(disk, bin).isEmpty() && !DiskFilesystem.exists(disk, "RECYCLER/INFO2"),
                "and the bin is empty again, its index gone with the last line");
        helper.succeed();
    }

    /**
     * On a Linux a deleted folder goes into the home's Trash with a note of its own, and comes back whole even when
     * the folder it was in has gone since: that folder is made again.
     */
    @GameTest(template = ARENA)
    public static void linuxTrash_bringsAFolderBackAndMakesItsParentAgain(final GameTestHelper helper) {
        final MainframeBlockEntity machine = machine(helper, "debian");
        if (machine == null) {
            return;
        }
        final ItemStack disk = machine.systemDisk();
        final TrashFolder trash = TrashPayloads.trashOf(machine);
        final String folder = LINUX_DESK + "/work/old";
        write(helper, machine, folder + "/a.txt", "one");
        write(helper, machine, folder + "/deep/b.txt", "two");
        helper.assertTrue(DiskTrash.put(disk, trash, folder, machine.systemDiskFreeWeight(), 0L)
                == DiskTrash.Outcome.DONE, "the folder goes into the Trash");
        final String kept = "home/player/.local/share/Trash/files/old";
        helper.assertTrue(DiskFilesystem.read(disk, kept + "/deep/b.txt").orElse("").equals("two"),
                "whole, under files, with what is inside it");
        helper.assertTrue(DiskFilesystem.read(disk, "home/player/.local/share/Trash/info/old.trashinfo").orElse("")
                .contains("Path=/" + folder), "with a note saying where it was");
        final List<DiskTrash.Entry> listed = DiskTrash.list(disk, trash);
        helper.assertTrue(listed.size() == 1 && listed.getFirst().directory() && listed.getFirst().weight() > 0L,
                "it is listed as a folder with the room its files take; got " + listed);

        DiskFilesystem.rmdir(disk, LINUX_DESK + "/work", FilesystemKind.HIERARCHICAL);
        helper.assertTrue(DiskTrash.restore(disk, trash, "old", 0L), "restore puts it back");
        helper.assertTrue(DiskFilesystem.read(disk, folder + "/deep/b.txt").orElse("").equals("two"),
                "the folder it was in is made again and the folder comes back whole");
        helper.assertFalse(DiskFilesystem.exists(disk, "home/player/.local/share/Trash/info/old.trashinfo"),
                "and its note is gone");
        helper.succeed();
    }

    /** CDE writes one index beside its files, and a second file of the same name is kept under a numbered one. */
    @GameTest(template = ARENA)
    public static void cdeTrashCan_keepsTwoFilesOfOneName(final GameTestHelper helper) {
        final MainframeBlockEntity machine = machine(helper, "unix");
        if (machine == null) {
            return;
        }
        machine.console().install(CDE.toString());
        machine.setBootedDesktopId(CDE);
        final ItemStack disk = machine.systemDisk();
        final TrashFolder can = TrashPayloads.trashOf(machine);
        final String first = "usr/player/Desktop/notes.txt";
        final String second = "usr/player/Documents/notes.txt";
        write(helper, machine, first, "first");
        write(helper, machine, second, "second");
        DiskTrash.put(disk, can, first, machine.systemDiskFreeWeight(), 0L);
        DiskTrash.put(disk, can, second, machine.systemDiskFreeWeight(), 0L);
        helper.assertTrue(DiskFilesystem.read(disk, "usr/player/.dt/Trash/notes.txt").orElse("").equals("first")
                        && DiskFilesystem.read(disk, "usr/player/.dt/Trash/notes.2.txt").orElse("").equals("second"),
                "both are kept, the second under a numbered name");
        final Map<String, String> index = TrashFolder.readIndex(
                DiskFilesystem.read(disk, "usr/player/.dt/Trash/.trashinfo").orElse(""));
        helper.assertTrue(first.equals(index.get("notes.txt")) && second.equals(index.get("notes.2.txt")),
                "and the index tells them apart; got " + index);
        helper.succeed();
    }

    /**
     * A deleted file keeps taking room until the trash is emptied, shredding one thing leaves the rest, and emptying
     * gives the room back.
     */
    @GameTest(template = ARENA)
    public static void shredAndEmpty_giveTheRoomBack(final GameTestHelper helper) {
        final MainframeBlockEntity machine = machine(helper, "frames_xp");
        if (machine == null) {
            return;
        }
        final ItemStack disk = machine.systemDisk();
        final TrashFolder bin = TrashPayloads.trashOf(machine);
        final long bare = DiskFilesystem.filesWeight(disk);
        write(helper, machine, FRAMES_DESK + "/a.txt", "x".repeat(4000));
        write(helper, machine, FRAMES_DESK + "/b.txt", "y".repeat(4000));
        final long written = DiskFilesystem.filesWeight(disk);
        DiskTrash.put(disk, bin, FRAMES_DESK + "/a.txt", machine.systemDiskFreeWeight(), 0L);
        DiskTrash.put(disk, bin, FRAMES_DESK + "/b.txt", machine.systemDiskFreeWeight(), 0L);
        helper.assertTrue(DiskFilesystem.filesWeight(disk) >= written,
                "what is in the bin still takes its room");
        helper.assertTrue(DiskTrash.shred(disk, bin, "Dc1.txt", 0L), "one is shredded");
        final List<DiskTrash.Entry> left = DiskTrash.list(disk, bin);
        helper.assertTrue(left.size() == 1 && left.getFirst().stored().equals("Dc2.txt"),
                "and the other is still there; got " + left);
        helper.assertTrue(DiskTrash.empty(disk, bin), "the bin is emptied");
        helper.assertTrue(DiskTrash.list(disk, bin).isEmpty() && DiskFilesystem.filesWeight(disk) == bare,
                "nothing is left in it and the disk has its room back");
        helper.succeed();
    }

    /**
     * The trash cannot go into itself: the folder it is in is refused, and something already inside it is deleted
     * for good, with its record, when it is deleted again.
     */
    @GameTest(template = ARENA)
    public static void theTrash_cannotGoIntoItself(final GameTestHelper helper) {
        final MainframeBlockEntity machine = machine(helper, "debian");
        if (machine == null) {
            return;
        }
        final ItemStack disk = machine.systemDisk();
        final TrashFolder trash = TrashPayloads.trashOf(machine);
        write(helper, machine, LINUX_DESK + "/a.txt", "x");
        DiskTrash.put(disk, trash, LINUX_DESK + "/a.txt", machine.systemDiskFreeWeight(), 0L);
        helper.assertTrue(DiskTrash.put(disk, trash, "home/player", machine.systemDiskFreeWeight(), 0L)
                == DiskTrash.Outcome.HOLDS_TRASH, "the home holds the Trash and cannot go into it");
        helper.assertTrue(DiskTrash.put(disk, trash, trash.storedPath("a.txt"), machine.systemDiskFreeWeight(), 0L)
                == DiskTrash.Outcome.HOLDS_TRASH, "nor can what is already in it");
        helper.assertTrue(DiskTrash.destroyWithin(disk, trash, trash.storedPath("a.txt"), 0L),
                "deleting it again deletes it for good");
        helper.assertTrue(DiskTrash.list(disk, trash).isEmpty()
                        && !DiskFilesystem.exists(disk, trash.recordPath("a.txt")),
                "and takes its note with it");
        helper.succeed();
    }

    /** What the trash says and what is done to it cross the wire whole, and so does whether the trash is full. */
    @GameTest(template = ARENA)
    public static void trashPayloads_crossTheWireWhole(final GameTestHelper helper) {
        final BlockPos at = new BlockPos(1, 2, 3);
        final RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(),
                helper.getLevel().registryAccess());
        final String longPlace = "a/".repeat(100) + "notes.txt";
        final TrashListingPayload listing = new TrashListingPayload(at, List.of(
                new TrashListingPayload.WireEntry("Dc1.txt", FRAMES_DESK + "/notes.txt", false, 3L),
                new TrashListingPayload.WireEntry("Dc2", longPlace, true, 12L)));
        TrashListingPayload.STREAM_CODEC.encode(buf, listing);
        final TrashListingPayload read = TrashListingPayload.STREAM_CODEC.decode(buf);
        helper.assertTrue(read.equals(listing), "the listing arrives as it left");
        helper.assertTrue(read.entries().get(1).original().length() == TrashListingPayload.MAX_ORIGINAL,
                "an old place too long for the wire is cut, not refused");

        final TrashActionPayload act = new TrashActionPayload(at, TrashActionPayload.Action.SHRED,
                List.of("Dc1.txt", "Dc2"));
        TrashActionPayload.STREAM_CODEC.encode(buf, act);
        helper.assertTrue(TrashActionPayload.STREAM_CODEC.decode(buf).equals(act), "and so does an action");

        final DesktopFilesPayload desk = new DesktopFilesPayload(List.of(), "", "", "Desk", List.of(), List.of(),
                List.of(), new DesktopFilesPayload.Prefs(0, 100, false, true, false, 100), List.of(), List.of(),
                Map.of(), true);
        DesktopFilesPayload.STREAM_CODEC.encode(buf, desk);
        helper.assertTrue(DesktopFilesPayload.STREAM_CODEC.decode(buf).trashFull(),
                "the desktop hears that its trash is full");
        helper.succeed();
    }

    private static ResourceLocation jsc(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, path);
    }

    private static void write(final GameTestHelper helper, final MainframeBlockEntity machine, final String path,
                              final String content) {
        final ItemStack disk = machine.systemDisk();
        final String parent = path.substring(0, path.lastIndexOf('/'));
        String built = "";
        for (final String segment : parent.split("/")) {
            built = built.isEmpty() ? segment : built + "/" + segment;
            DiskFilesystem.mkdir(disk, built, FilesystemKind.HIERARCHICAL);
        }
        final DiskFilesystem.WriteResult written = DiskFilesystem.write(disk, path, FileType.TXT, content,
                machine.systemDiskFreeWeight(), FilesystemKind.HIERARCHICAL);
        helper.assertTrue(written == DiskFilesystem.WriteResult.OK, "could not write " + path + ": " + written);
    }

    /** A powered machine with a full build and that system on its disk. */
    private static MainframeBlockEntity machine(final GameTestHelper helper, final String system) {
        helper.setBlock(WHERE, ComputingModule.MAINFRAME.get());
        if (!(helper.getBlockEntity(WHERE) instanceof MainframeBlockEntity machine)) {
            helper.fail("no machine at " + WHERE);
            return null;
        }
        TestWorldBuilder.installMainframeBuild(machine);
        machine.uninstallOs();
        if (!machine.installOs(jsc(system))) {
            helper.fail("could not install " + system);
            return null;
        }
        return machine;
    }
}
