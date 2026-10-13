/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.operation.payload.AutomationPayload;
import dev.jstech.computers.operation.payload.ClusterManagerStatePayload;
import dev.jstech.computers.operation.payload.ProcessListPayload;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.os.fs.FilesystemContents;
import dev.jstech.computers.os.fs.StoredFile;
import dev.jstech.computers.program.iql.IqlDefinition;
import dev.jstech.core.text.LongText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextBounds;
import dev.jstech.core.text.TextTags;
import dev.jstech.tests.JsTests;
import io.netty.buffer.Unpooled;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Text against the fixed limits of the forms it is kept and sent in: a string tag holds 65,535 bytes and a save
 * writes a longer one as nothing, and a wire field throws past its cap. Each test makes text that would once have
 * been lost or dropped a player, and checks it is kept whole or cut to fit.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class TextLimitsGameTests {

    private static final String ARENA = "empty";

    /* Three bytes of modified UTF-8 each, so a file of these passes a tag's bytes well before its own cap. */
    private static final String WIDE = String.valueOf((char) 0x4E2D);

    private TextLimitsGameTests() {
    }

    @GameTest(template = ARENA)
    public static void longText_keepsAShortTextAsAString(final GameTestHelper helper) {
        final CompoundTag tag = new CompoundTag();
        LongText.put(tag, "text", "hello");
        helper.assertTrue(tag.get("text") instanceof StringTag, "a text that fits stays a string, as saves had it");
        helper.assertTrue("hello".equals(LongText.get(tag, "text")), "and reads back as it was");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void longText_survivesASavePastWhatAStringHolds(final GameTestHelper helper) {
        final String text = WIDE.repeat(30_000);
        final CompoundTag tag = new CompoundTag();
        LongText.put(tag, "text", text);
        helper.assertTrue(tag.get("text") instanceof ByteArrayTag, "a text past a string's bytes is kept as bytes");
        final CompoundTag read = throughASave(helper, tag);
        helper.assertTrue(text.equals(LongText.get(read, "text")), "and comes back whole from the save");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void longText_codecRoundTripsThroughNbt(final GameTestHelper helper) {
        for (final String text : List.of("", "short", "a".repeat(70_000), WIDE.repeat(25_000))) {
            final Tag written = LongText.CODEC.encodeStart(NbtOps.INSTANCE, text).getOrThrow();
            final String read = LongText.CODEC.parse(NbtOps.INSTANCE, written).getOrThrow();
            helper.assertTrue(text.equals(read), "a text of " + text.length() + " letters comes back as it went");
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void longText_fitsFindsATooLongStringDeepInATag(final GameTestHelper helper) {
        final CompoundTag inner = new CompoundTag();
        inner.putString("note", "a".repeat(70_000));
        final ListTag list = new ListTag();
        list.add(inner);
        final CompoundTag outer = new CompoundTag();
        outer.put("entries", list);
        helper.assertFalse(LongText.fits(outer), "a string past a tag's bytes, three levels down, is found");
        inner.putString("note", "short");
        helper.assertTrue(LongText.fits(outer), "and a tag of short strings fits");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void textTags_cutALiteralToWhatTheWireCarries(final GameTestHelper helper) {
        final CompoundTag tag = TextTags.write(Text.literal("x".repeat(100_000)));
        helper.assertTrue(LongText.fits(tag), "a literal of any length saves as a string a tag holds");
        helper.assertTrue(TextTags.read(tag).english().length() == 8_192, "cut to what the wire carries");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void disk_refusesAFileLongerThanAFileHolds(final GameTestHelper helper) {
        final ItemStack disk = new ItemStack(ComputingModule.disk(StorageTier.NVME, DiskSize.TB_1));
        helper.assertTrue(write(disk, "big.txt", "x".repeat(StoredFile.MOST_CHARS + 1))
                == DiskFilesystem.WriteResult.TOO_LARGE, "one letter past the cap is refused");
        helper.assertTrue(write(disk, "big.txt", "x".repeat(StoredFile.MOST_CHARS))
                == DiskFilesystem.WriteResult.OK, "and the cap itself is written");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void disk_refusesAnAdditionPastWhatAFileHolds(final GameTestHelper helper) {
        final ItemStack disk = new ItemStack(ComputingModule.disk(StorageTier.NVME, DiskSize.TB_1));
        helper.assertTrue(write(disk, "log.txt", "x".repeat(StoredFile.MOST_CHARS - 1))
                == DiskFilesystem.WriteResult.OK, "a file just short of the cap is written");
        helper.assertTrue(append(disk, "log.txt", "xx") == DiskFilesystem.WriteResult.TOO_LARGE,
                "adding past the cap is refused");
        helper.assertTrue(append(disk, "log.txt", "x") == DiskFilesystem.WriteResult.OK, "adding up to it is not");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void disk_keepsAFileOfWideTextThroughASave(final GameTestHelper helper) {
        final String text = WIDE.repeat(StoredFile.MOST_CHARS);
        final FilesystemContents contents =
                new FilesystemContents(Map.of("wide.txt", new StoredFile("wide.txt", FileType.TXT, text)));
        final Tag written = FilesystemContents.CODEC.encodeStart(NbtOps.INSTANCE, contents).getOrThrow();
        final CompoundTag holder = new CompoundTag();
        holder.put("fs", written);
        final CompoundTag read = throughASave(helper, holder);
        final FilesystemContents back = FilesystemContents.CODEC.parse(NbtOps.INSTANCE, read.get("fs")).getOrThrow();
        helper.assertTrue(text.equals(back.files().get("wide.txt").content()),
                "a file of three-byte letters at the cap comes back whole from the save");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void processList_cutsANameThatSlippedByInsteadOfThrowing(final GameTestHelper helper) {
        final ProcessListPayload payload = new ProcessListPayload(List.of(new ProcessListPayload.ProcessLine(0,
                "j".repeat(200), ProcessListPayload.ProcessState.RUNNING, Text.EMPTY)));
        final ProcessListPayload read = roundTrip(helper, ProcessListPayload.STREAM_CODEC, payload);
        helper.assertTrue(read.processes().get(0).name().length() == ProcessListPayload.ProcessLine.MAX_NAME,
                "the name is cut to what the line carries");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void processList_keepsAJobNameAtTheCapWhole(final GameTestHelper helper) {
        final String name = "j".repeat(IqlDefinition.MAX_NAME);
        final ProcessListPayload payload = new ProcessListPayload(List.of(new ProcessListPayload.ProcessLine(0,
                name, ProcessListPayload.ProcessState.RUNNING, Text.EMPTY)));
        final ProcessListPayload read = roundTrip(helper, ProcessListPayload.STREAM_CODEC, payload);
        helper.assertTrue(name.equals(read.processes().get(0).name()),
                "an action on a job names it by what the list shows, so a job's longest name arrives whole");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void automation_cutsItsListsToWhatTheMessageCarries(final GameTestHelper helper) {
        final List<AutomationPayload.JobRow> jobs = new ArrayList<>();
        for (int i = 0; i < AutomationPayload.MAX_JOBS + 10; i++) {
            jobs.add(new AutomationPayload.JobRow("job-" + i + "-" + "n".repeat(100), Text.EMPTY, Text.EMPTY, false));
        }
        final AutomationPayload payload = new AutomationPayload(true, Text.EMPTY, jobs, List.of("a".repeat(500)));
        final AutomationPayload read = roundTrip(helper, AutomationPayload.STREAM_CODEC, payload);
        helper.assertTrue(read.jobs().size() == AutomationPayload.MAX_JOBS, "the list stops at its cap");
        helper.assertTrue(read.jobs().get(0).name().length() == IqlDefinition.MAX_NAME, "a long name is cut");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void clusterNode_cutsALongProgramList(final GameTestHelper helper) {
        final ClusterManagerStatePayload.WireNode node = new ClusterManagerStatePayload.WireNode(1L, 0, 0,
                "n".repeat(100), "o".repeat(100), "IQL Server Management Studio, ".repeat(10), 0, true, 0, 0,
                0L, 0L, ClusterManagerStatePayload.STATE_ONLINE);
        final ClusterManagerStatePayload.WireNode read =
                roundTrip(helper, ClusterManagerStatePayload.WireNode.STREAM_CODEC, node);
        helper.assertTrue(read.programs().length() == ClusterManagerStatePayload.WireNode.MAX_PROGRAMS,
                "the program list is cut to its cap rather than throwing");
        helper.assertTrue(read.name().length() == ClusterManagerStatePayload.WireNode.MAX_NAME, "and so is the name");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void clip_neverLeavesHalfACharacterOnTheWire(final GameTestHelper helper) {
        final String emoji = new String(Character.toChars(0x1F600));
        final String clipped = TextBounds.clip("a".repeat(63) + emoji, 64);
        helper.assertTrue(clipped.length() == 63, "the cut steps back before a character it would split");
        helper.succeed();
    }

    private static DiskFilesystem.WriteResult write(final ItemStack disk, final String path, final String content) {
        return DiskFilesystem.write(disk, path, FileType.TXT, content, Long.MAX_VALUE, FilesystemKind.HIERARCHICAL,
                0L);
    }

    private static DiskFilesystem.WriteResult append(final ItemStack disk, final String path, final String content) {
        return DiskFilesystem.append(disk, path, FileType.TXT, content, Long.MAX_VALUE, FilesystemKind.HIERARCHICAL,
                0L);
    }

    /* Writes the tag the way a region file does, and reads it back. */
    private static CompoundTag throughASave(final GameTestHelper helper, final CompoundTag tag) {
        try {
            final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream out = new DataOutputStream(bytes)) {
                NbtIo.write(tag, out);
            }
            try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
                return NbtIo.read(in);
            }
        } catch (final IOException failed) {
            helper.fail("the save could not be written or read: " + failed);
            return new CompoundTag();
        }
    }

    /* Writes the message and reads it back, the way it crosses the wire. */
    private static <T> T roundTrip(final GameTestHelper helper,
                                   final StreamCodec<? super RegistryFriendlyByteBuf, T> codec, final T payload) {
        final RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(),
                helper.getLevel().registryAccess());
        try {
            codec.encode(buf, payload);
            return codec.decode(buf);
        } finally {
            buf.release();
        }
    }
}
