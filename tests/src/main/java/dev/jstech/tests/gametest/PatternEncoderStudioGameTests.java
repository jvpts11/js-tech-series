/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.PatternEncoderBlockEntity;
import dev.jstech.computers.crafting.PatternWorkbench;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.fs.CraftFile;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.CraftingFixtures.Network;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static dev.jstech.tests.testkit.CraftingFixtures.buildCraftingNetwork;

/**
 * GameTests for the Pattern Encoder: the studio draft burning onto writable media at the linked encoder,
 * and which media the encoder bay accepts for its era.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class PatternEncoderStudioGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;

    private PatternEncoderStudioGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void studio_benchDraftBurnsOntoMediaAtTheLinkedEncoder(final GameTestHelper helper) {
        /*
         * The workbench lives on the computer; the encoder beside it is its burner. One oak log resolves to
         * four planks through the recipe book, and the burned file reads back as that pattern.
         */
        final Network net = buildCraftingNetwork(helper);
        final BlockPos encoderPos = new BlockPos(5, 2, 3);
        helper.setBlock(encoderPos, ComputingModule.PATTERN_ENCODER.get());
        if (!(helper.getBlockEntity(encoderPos) instanceof PatternEncoderBlockEntity encoder)) {
            throw new IllegalStateException("no pattern encoder at " + encoderPos);
        }
        encoder.media().setStackInSlot(0, new ItemStack(ComputingModule.DVD_RW.get()));
        final var studio = net.cc().studio();
        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    helper.assertTrue(encoder.ownerPos() != null && encoder.ownerPos().equals(net.cc().getBlockPos()),
                            "the encoder links to the adjacent computer; got " + encoder.ownerPos());
                    studio.refreshPreview(helper.getLevel());
                    helper.assertTrue(studio.serialize(PatternWorkbench.Kind.BENCH,
                            helper.getLevel().registryAccess()).isEmpty(), "an empty bench serializes to nothing");
                    studio.setGhost(0, new ItemStack(Items.OAK_LOG));
                    studio.refreshPreview(helper.getLevel());
                    helper.assertTrue(studio.preview().is(Items.OAK_PLANKS) && studio.preview().getCount() == 4,
                            "one log previews four planks");
                    final var content = studio.serialize(PatternWorkbench.Kind.BENCH,
                            helper.getLevel().registryAccess());
                    helper.assertTrue(content.isPresent(), "a resolved bench draft serializes");
                    helper.assertTrue(encoder.queueBurn("oak_planks", content.get()), "the encoder queues the burn");
                })
                .thenExecuteAfter(120, () -> {
                    final ItemStack media = encoder.mediaStack();
                    final List<DiskFilesystem.FileEntry> files =
                            DiskFilesystem.list(media, "", FilesystemKind.HIERARCHICAL);
                    int craftCount = 0;
                    String craftPath = null;
                    for (final DiskFilesystem.FileEntry e : files) {
                        if (e.type() == FileType.CRAFT) {
                            craftCount++;
                            craftPath = e.path();
                        }
                    }
                    helper.assertTrue(craftCount == 1, "exactly one .craft file is burned");
                    final var content = DiskFilesystem.read(media, craftPath);
                    helper.assertTrue(content.isPresent(), "the .craft file is readable");
                    final var parsed = CraftFile.parse(content.get(), helper.getLevel().registryAccess());
                    helper.assertTrue(parsed.isPresent(), "the .craft round-trips back into a pattern");
                    helper.assertTrue(parsed.get().result().is(Items.OAK_PLANKS)
                            && parsed.get().result().getCount() == 4, "the burned pattern produces four planks");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void encoder_requiresWritableMediaOfItsEra(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, ComputingModule.PATTERN_ENCODER.get());
        if (!(helper.getBlockEntity(pos) instanceof PatternEncoderBlockEntity encoder)) {
            throw new IllegalStateException("no pattern encoder at " + pos);
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertFalse(encoder.hasMedia(), "nothing in the bay to write to");
                    helper.assertFalse(encoder.media().isItemValid(0, new ItemStack(ComputingModule.CD_ROM.get())),
                            "read-only media is rejected by the bay");
                    helper.assertFalse(encoder.media().isItemValid(0,
                            new ItemStack(ComputingModule.FLOPPY_DISK.get())), "a Standard encoder refuses a floppy");
                    helper.assertTrue(encoder.media().isItemValid(0, new ItemStack(ComputingModule.DVD_RW.get())),
                            "a Standard encoder takes a DVD-RW");
                })
                .thenSucceed();
    }
}
