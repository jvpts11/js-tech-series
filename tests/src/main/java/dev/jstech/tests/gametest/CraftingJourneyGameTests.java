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
import dev.jstech.computers.menu.CraftingComputerMenu;
import dev.jstech.computers.menu.PatternEncoderMenu;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.fs.CraftFile;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.os.media.MediaReaderBlockEntity;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.CraftingFixtures.Network;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static dev.jstech.tests.testkit.CraftingFixtures.buildCraftingNetwork;
import static dev.jstech.tests.testkit.CraftingFixtures.storageKey;

/**
 * The autocrafting journey end to end: encode a recipe, load it into a Crafting Computer and craft it across
 * the full chain.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class CraftingJourneyGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;

    private CraftingJourneyGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void journey_encodeLoadAndCraftAcrossTheFullChain(final GameTestHelper helper) {
        /*
         * The player's whole path, server-side: encode a recipe onto a disc at the Pattern Encoder, carry the disc
         * to a drive linked to the Crafting Computer, read and load it into a Crafting Card's ROM (the exact steps
         * the Crafting Manager's Load button runs), then request the craft through the same entry point the
         * terminal's request popup uses, and watch the result land in network storage.
         */
        final Network net = buildCraftingNetwork(helper);
        final BlockPos encoderPos = new BlockPos(5, 2, 1);
        final BlockPos drivePos = new BlockPos(5, 2, 3);
        helper.setBlock(encoderPos, ComputingModule.PATTERN_ENCODER.get());
        helper.setBlock(drivePos, ComputingModule.DVD_DRIVE.get());
        if (!(helper.getBlockEntity(encoderPos) instanceof PatternEncoderBlockEntity encoder)) {
            throw new IllegalStateException("no pattern encoder at " + encoderPos);
        }
        if (!(helper.getBlockEntity(drivePos) instanceof MediaReaderBlockEntity drive)) {
            throw new IllegalStateException("no media reader at " + drivePos);
        }
        encoder.media().setStackInSlot(0, new ItemStack(ComputingModule.DVD_RW.get()));

        helper.startSequence()
                .thenExecuteAfter(SETTLE + 2, () -> {
                    // 1) Author the pattern on the computer's workbench and send it to the linked encoder.
                    helper.assertTrue(encoder.ownerPos() != null && encoder.ownerPos().equals(net.cc().getBlockPos()),
                            "the encoder links to the adjacent Crafting Computer; got " + encoder.ownerPos());
                    final var studio = net.cc().studio();
                    studio.setGhost(0, new ItemStack(Items.OAK_LOG));
                    studio.refreshPreview(helper.getLevel());
                    final var content = studio.serialize(PatternWorkbench.Kind.BENCH,
                            helper.getLevel().registryAccess());
                    helper.assertTrue(content.isPresent() && encoder.queueBurn("oak_planks", content.get()),
                            "the bench draft is sent to the encoder");
                    final var player = helper.makeMockPlayer(GameType.CREATIVE);
                    final BlockPos absolute = helper.absolutePos(encoderPos);
                    player.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
                    final var encoderMenu = new PatternEncoderMenu(1, player.getInventory(), encoder);
                    helper.assertTrue(encoderMenu.stillValid(player), "the Pattern Encoder menu stays open");
                })
                .thenExecuteAfter(120, () -> {
                    helper.assertTrue(encoder.completed() == 1, "the encoder burned the file; completed="
                            + encoder.completed());
                    helper.assertFalse(encoder.locked(), "the bay is free once the job is over");
                    // 2) Carry the disc over: out of the encoder, into the drive next to the computer.
                    final ItemStack disc = encoder.ejectMedia();
                    helper.assertFalse(disc.isEmpty(), "the disc comes out of the encoder");
                    drive.mediaSlot().setStackInSlot(0, disc);
                })
                .thenExecuteAfter(SETTLE + 2, () -> {
                    // 3) The drive links to the adjacent Crafting Computer, which lists the medium as a volume.
                    helper.assertTrue(drive.ownerPos() != null
                                    && drive.ownerPos().equals(helper.absolutePos(new BlockPos(5, 2, 2))),
                            "the DVD drive links to the Crafting Computer; got " + drive.ownerPos());
                    helper.assertTrue(net.cc().linkedEndpoints().contains(helper.absolutePos(drivePos).asLong()),
                            "the Crafting Computer lists the drive as a linked endpoint");
                    // 4) Load the .craft from the linked medium into a card's ROM (the Load button's steps).
                    final ItemStack media = drive.mediaSlot().getStackInSlot(0);
                    String craftPath = null;
                    for (final DiskFilesystem.FileEntry e
                            : DiskFilesystem.list(media, "", FilesystemKind.HIERARCHICAL)) {
                        if (e.type() == FileType.CRAFT) {
                            craftPath = e.path();
                        }
                    }
                    helper.assertTrue(craftPath != null, "the carried disc still holds the .craft file");
                    final var content = DiskFilesystem.read(media, craftPath);
                    final var parsed = CraftFile.parse(content.orElse(""), helper.getLevel().registryAccess());
                    helper.assertTrue(parsed.isPresent(), "the .craft parses back into a pattern");
                    helper.assertTrue(net.cc().loadPattern(parsed.get()), "the pattern loads into the card's ROM");
                    final var player = helper.makeMockPlayer(GameType.CREATIVE);
                    final BlockPos absolute = helper.absolutePos(new BlockPos(5, 2, 2));
                    player.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
                    final var ccMenu = new CraftingComputerMenu(1, player.getInventory(), net.cc());
                    helper.assertTrue(ccMenu.stillValid(player), "the Crafting Computer menu stays open");
                    /*
                     * Stock the ingredients now: the craft planner reads the incremental network index,
                     * which needs a tick to absorb a direct store write before the request is planned.
                     */
                    net.seed(Items.OAK_LOG, 8);
                })
                .thenExecuteAfter(SETTLE, () -> helper.assertTrue(net.mainframe().submitNetworkCraft(
                                storageKey(Items.OAK_PLANKS), 4, true, "test (Interactor)") != null,
                        "the craft request is accepted"))
                .thenExecuteAfter(20, () -> {
                    final long planks = net.storage(helper).count(storageKey(Items.OAK_PLANKS));
                    helper.assertTrue(planks >= 4, "the crafted planks land in network storage; got " + planks);
                })
                .thenSucceed();
    }
}
