/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.developer.studio.StudioKeys;
import dev.jstech.core.developer.studio.StudioServices;
import dev.jstech.core.jstechseries.SeriesMaterials;
import dev.jstech.core.material.Forms;
import dev.jstech.core.material.MaterialUses;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestClaims;
import dev.jstech.tests.TestItems;
import dev.jstech.tests.TestKitMachine;
import dev.jstech.testkit.gametest.GameTestPlayers;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The server's side of the studio's tools, asked as an operator asks: a block's fields, faces and what crossed them; a
 * block saved and read back; a loot table rolled; a structure caught; a multiblock's pattern recorded and written;
 * where a tag's entries came from; the packets sent; and every machine timed. Anyone else is refused.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class StudioServicesGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos AT = new BlockPos(2, 2, 2);
    /* The level a dedicated server gives its operators. */
    private static final int OPERATOR = 4;
    private static final int PUT = 4;
    private static final String TIN_TAG = "c:ingots/tin";

    private StudioServicesGameTests() {
    }

    @GameTest(template = ARENA)
    public static void block_saysItsFieldsFacesAndWhatCrossedThem(final GameTestHelper helper) {
        helper.setBlock(AT, TestKitMachine.BLOCK.get());
        final BlockPos pos = helper.absolutePos(AT);
        final ItemStack cobblestone = new ItemStack(Items.COBBLESTONE, 1);
        // Before the inspector first watches it, nothing that crosses its faces is counted, for nothing is counting.
        input(helper, pos).insertItem(0, cobblestone.copy(), false);
        asked(helper, StudioServices.BLOCK, at(pos), first -> helper.assertTrue(
                first.getCompound(StudioKeys.FLOW).getLong(StudioKeys.ITEMS_IN) == 0L,
                "unwatched, what was put in is not counted: " + first.get(StudioKeys.FLOW)));
        final IItemHandler input = input(helper, pos);
        final int taken = PUT - input.insertItem(0, cobblestone.copyWithCount(PUT), false).getCount();
        input.insertItem(0, cobblestone.copyWithCount(PUT), true);
        asked(helper, StudioServices.BLOCK, at(pos), answer -> {
            helper.assertTrue(answer.getString(StudioKeys.BLOCK).equals(TestKitMachine.BLOCK.getId().toString()),
                    "it names the block: " + answer);
            helper.assertTrue(!answer.getList(StudioKeys.FIELDS, Tag.TAG_COMPOUND).isEmpty(),
                    "it lists the fields the block entity saves");
            helper.assertTrue(answer.getList(StudioKeys.FACES, Tag.TAG_COMPOUND).size() == Direction.values().length,
                    "it reads each face");
            helper.assertTrue(answer.getCompound(StudioKeys.FLOW).getLong(StudioKeys.ITEMS_IN) == taken,
                    "watched, what was put in counts, and what was only tried does not: "
                            + answer.get(StudioKeys.FLOW));
        });
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void save_bringsAMachineBackAsItWent(final GameTestHelper helper) {
        helper.setBlock(AT, TestKitMachine.BLOCK.get());
        asked(helper, StudioServices.SAVE, at(helper.absolutePos(AT)), answer -> {
            final ListTag changes = answer.getList(StudioKeys.CHANGES, Tag.TAG_COMPOUND);
            helper.assertTrue(!changes.isEmpty(), "every field is listed");
            final List<String> changed = new ArrayList<>();
            for (int i = 0; i < changes.size(); i++) {
                if (changes.getCompound(i).getBoolean(StudioKeys.CHANGED)) {
                    changed.add(changes.getCompound(i).getString(StudioKeys.FIELD));
                }
            }
            helper.assertTrue(changed.isEmpty(), "the kit machine reads back what it saves: " + changed);
        });
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void loot_spreadsTheDropsOfManyRolls(final GameTestHelper helper) {
        final CompoundTag args = new CompoundTag();
        args.putString(StudioKeys.TABLE, "minecraft:blocks/gravel");
        args.putInt(StudioKeys.ROLLS, 400);
        args.putString(StudioKeys.TOOL, "minecraft:iron_shovel");
        asked(helper, StudioServices.LOOT, args, answer -> {
            helper.assertTrue(answer.getInt(StudioKeys.ROLLS) == 400, "it rolled as many times as asked");
            final ListTag rows = answer.getList(StudioKeys.ROWS, Tag.TAG_COMPOUND);
            final List<String> items = rows.stream().map(row -> ((CompoundTag) row).getString(StudioKeys.ID))
                    .toList();
            helper.assertTrue(items.contains("minecraft:gravel") && items.contains("minecraft:flint"),
                    "gravel gives gravel and, now and then, flint: " + items);
        });
        final CompoundTag none = new CompoundTag();
        none.putString(StudioKeys.TABLE, "jstests:blocks/no_such_table");
        asked(helper, StudioServices.LOOT, none, answer -> helper.assertTrue(answer.contains(StudioKeys.PROBLEM),
                "a table that is not there is said so"));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void structure_countsTheBlocksBetweenTheCorners(final GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 2, 1), Blocks.STONE);
        helper.setBlock(new BlockPos(2, 2, 1), Blocks.CHEST);
        helper.setBlock(new BlockPos(3, 3, 2), Blocks.STONE);
        final CompoundTag args = new CompoundTag();
        args.putLong(StudioKeys.FROM, helper.absolutePos(new BlockPos(1, 2, 1)).asLong());
        args.putLong(StudioKeys.TO, helper.absolutePos(new BlockPos(3, 3, 2)).asLong());
        asked(helper, StudioServices.STRUCTURE, args, answer -> {
            final int[] size = answer.getIntArray(StudioKeys.SIZE);
            helper.assertTrue(size.length == 3 && size[0] == 3 && size[1] == 2 && size[2] == 2,
                    "it spans the corners, both kept");
            helper.assertTrue(answer.getInt(StudioKeys.BLOCKS) == 3, "air is left out: " + answer);
            helper.assertTrue(answer.getInt(StudioKeys.ENTITIES) == 1, "the chest's block entity is kept");
            helper.assertTrue(!answer.contains(StudioKeys.PATH), "nothing is written unless asked");
        });
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void multiblock_recordsThePatternAndWritesItsFile(final GameTestHelper helper) {
        for (final BlockPos at : BlockPos.betweenClosed(new BlockPos(1, 2, 1), new BlockPos(3, 2, 2))) {
            helper.setBlock(at, Blocks.BRICKS);
        }
        helper.setBlock(new BlockPos(2, 2, 1), Blocks.BLAST_FURNACE);
        final CompoundTag args = new CompoundTag();
        args.putLong(StudioKeys.FROM, helper.absolutePos(new BlockPos(1, 2, 1)).asLong());
        args.putLong(StudioKeys.TO, helper.absolutePos(new BlockPos(3, 2, 2)).asLong());
        args.putLong(StudioKeys.CONTROLLER, helper.absolutePos(new BlockPos(2, 2, 1)).asLong());
        // A namespace no mod of this run has a source of, so the file goes to the world's own pack, not into a mod.
        args.putString(StudioKeys.NAME, "jstests_recorded:furnace_" + Math.abs(helper.absolutePos(AT).asLong()));
        args.putBoolean(StudioKeys.SAVE, true);
        asked(helper, StudioServices.MULTIBLOCK, args, answer -> {
            final ListTag letters = answer.getList(StudioKeys.LETTERS, Tag.TAG_COMPOUND);
            helper.assertTrue(letters.size() == 1 && letters.getCompound(0).getString(StudioKeys.LETTER).equals("B")
                    && letters.getCompound(0).getInt(StudioKeys.COUNT) == 5, "its bricks are B, five: " + letters);
            helper.assertTrue(answer.getString(StudioKeys.PATTERN).contains("B#B"), "the pattern is sent: "
                    + answer.getString(StudioKeys.PATTERN));
            final Path file = Path.of(answer.getString(StudioKeys.PATH));
            helper.assertTrue(Files.exists(file) && answer.getBoolean(StudioKeys.RELOAD),
                    "its file is written to the world's pack, in force once its data is reloaded: " + file);
            try {
                Files.deleteIfExists(file);
            } catch (final IOException kept) {
                throw new GameTestAssertException("the file written could not be taken away again: " + kept);
            }
        });
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void sources_sayWhichPackPutEachEntryInATag(final GameTestHelper helper) {
        final CompoundTag args = new CompoundTag();
        args.putString(StudioKeys.REGISTRY, "item");
        args.putString(StudioKeys.TAG, "minecraft:planks");
        asked(helper, StudioServices.SOURCES, args, answer -> {
            final ListTag packs = answer.getList(StudioKeys.PACKS, Tag.TAG_COMPOUND);
            helper.assertTrue(!packs.isEmpty(), "the game's own file of the tag is found");
            final CompoundTag first = packs.getCompound(0);
            helper.assertTrue(first.getString(StudioKeys.PACK).equals("vanilla"), "the game's own pack: " + first);
            helper.assertTrue(first.getList(StudioKeys.ENTRIES, Tag.TAG_STRING).contains(StringTag.valueOf(
                    "minecraft:oak_planks")), "with what it put in");
        });
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void payloads_listTheTicksSent(final GameTestHelper helper) {
        final CompoundTag args = new CompoundTag();
        args.putBoolean(StudioKeys.EVERYONE, true);
        asked(helper, StudioServices.PAYLOADS, args, answer -> helper.assertTrue(answer.contains(StudioKeys.TICKS),
                "it answers with the ticks kept, none or more"));
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void cost_answersWhenItsMeasureEnds(final GameTestHelper helper) {
        final ServerPlayer player = operator(helper, "studio_timer");
        final CompoundTag args = new CompoundTag();
        args.putInt(StudioKeys.SECONDS, 1);
        final CompoundTag[] answered = new CompoundTag[1];
        StudioServices.ask(player, StudioServices.COST, args, answer -> answered[0] = answer);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(answered[0] != null, "the measure ends after its second"))
                .thenExecute(() -> {
                    leave(player);
                    helper.assertTrue(answered[0].contains(StudioKeys.TICKS) && answered[0].getLong(
                            StudioKeys.TICKS) > 0, "it says how many ticks it measured: " + answered[0]);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void access_weighsEachActionForThePlayerAndWhoTheyNameWithTheClaim(final GameTestHelper helper) {
        final ServerPlayer alex = GameTestPlayers.connected(helper, "studio_alex");
        final AABB claim = new AABB(helper.absolutePos(AT));
        TestClaims.claim(claim);
        try {
            final CompoundTag args = at(helper.absolutePos(AT));
            args.putString(StudioKeys.WHO, "studio_alex");
            asked(helper, StudioServices.ACCESS, args, answer -> {
                final ListTag columns = answer.getList(StudioKeys.COLUMNS, Tag.TAG_COMPOUND);
                helper.assertTrue(columns.size() == 3 && columns.getCompound(0).getString(StudioKeys.KIND)
                        .equals(StudioKeys.YOU) && columns.getCompound(1).getString(StudioKeys.NAME)
                        .equals("studio_alex") && columns.getCompound(2).getString(StudioKeys.KIND)
                        .equals(StudioKeys.MACHINES), "the operator, the player named and their machines: " + columns);
                helper.assertTrue(columns.getCompound(1).getList(StudioKeys.ENTRIES, Tag.TAG_COMPOUND).size() == 8,
                        "each weighed for the eight actions every mod asks");
                helper.assertTrue(answer.getString(StudioKeys.CLAIM).equals(TestClaims.CLAIMANT.toString()),
                        "the place's claim, by its owner");
                helper.assertTrue(answer.getList(StudioKeys.ANSWERS, Tag.TAG_COMPOUND).stream().anyMatch(one ->
                        ((CompoundTag) one).getString(StudioKeys.NAME).equals(TestClaims.NAME)),
                        "and what the claim's protection answered");
                helper.assertTrue(answer.contains(StudioKeys.MAY_HURT), "and whether the player named may hurt");
            });
        } finally {
            TestClaims.unclaim(claim);
            GameTestPlayers.leave(alex);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void unification_tellsEachTagTheItemKeptAndTheRecipesItChanged(final GameTestHelper helper) {
        final String copy = BuiltInRegistries.ITEM.getKey(TestItems.FOREIGN_TIN.get()).toString();
        final String coreTin = BuiltInRegistries.ITEM.getKey(MaterialUses.item(SeriesMaterials.TIN, Forms.INGOT)
                .get()).toString();
        asked(helper, StudioServices.UNIFICATION, new CompoundTag(), answer -> {
            helper.assertTrue(answer.getBoolean(StudioKeys.ON), "unification is on in the test world");
            final CompoundTag tin = answer.getList(StudioKeys.ENTRIES, Tag.TAG_COMPOUND).stream()
                    .map(CompoundTag.class::cast).filter(one -> one.getString(StudioKeys.TAG).equals(TIN_TAG))
                    .findFirst().orElseThrow(() -> new GameTestAssertException("no " + TIN_TAG + " in " + answer));
            helper.assertTrue(tin.getString(StudioKeys.KEPT).equals(coreTin), "the Core's ingot is kept: " + tin);
            helper.assertTrue(tin.getList(StudioKeys.REPLACED, Tag.TAG_STRING).contains(StringTag.valueOf(copy)),
                    "and the test mod's copy replaced: " + tin);
            helper.assertTrue(tin.getInt(StudioKeys.COUNT) >= 2 && tin.getString(StudioKeys.ID).startsWith(
                    JsTests.MODID + ":unify/") && tin.getString(StudioKeys.BEFORE).equals(copy),
                    "the recipes that named the copy, one of them shown with the copy before: " + tin);
            helper.assertTrue(answer.getList(StudioKeys.PREFER, Tag.TAG_STRING).size() >= 2,
                    "with the mods kept first, in order");
        });
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void services_refuseAPlayerWhoIsNoOperator(final GameTestHelper helper) {
        final ServerPlayer player = GameTestPlayers.connected(helper, "studio_visitor");
        final CompoundTag[] answered = new CompoundTag[1];
        try {
            StudioServices.ask(player, StudioServices.BLOCK, at(helper.absolutePos(AT)),
                    answer -> answered[0] = answer);
        } finally {
            GameTestPlayers.leave(player);
        }
        helper.assertTrue(answered[0] != null && answered[0].contains(StudioServices.REFUSED),
                "a player who may not use the world's tools is told so");
        helper.succeed();
    }

    /* Asks {@code service} as an operator and checks what it answers, at once. */
    private static void asked(final GameTestHelper helper, final String service, final CompoundTag args,
                              final Consumer<CompoundTag> check) {
        final ServerPlayer player = operator(helper, "studio_operator");
        final CompoundTag[] answered = new CompoundTag[1];
        try {
            StudioServices.ask(player, service, args, answer -> answered[0] = answer);
        } finally {
            leave(player);
        }
        helper.assertTrue(answered[0] != null, service + " answers at once");
        helper.assertTrue(!answered[0].contains(StudioServices.REFUSED), service + " answers an operator: "
                + answered[0]);
        check.accept(answered[0]);
    }

    private static ServerPlayer operator(final GameTestHelper helper, final String name) {
        final ServerPlayer player = GameTestPlayers.connected(helper, name);
        player.server.getPlayerList().getOps().add(new ServerOpListEntry(player.getGameProfile(), OPERATOR, false));
        return player;
    }

    private static void leave(final ServerPlayer player) {
        player.server.getPlayerList().getOps().remove(player.getGameProfile());
        GameTestPlayers.leave(player);
    }

    /* Whichever face the kit machine's sides take its input at: cobblestone, which it makes stone of. */
    private static IItemHandler input(final GameTestHelper helper, final BlockPos pos) {
        final ItemStack cobblestone = new ItemStack(Items.COBBLESTONE, PUT);
        for (final Direction side : Direction.values()) {
            final IItemHandler offered = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
            if (offered != null && offered.insertItem(0, cobblestone, true).getCount() < PUT) {
                return offered;
            }
        }
        throw new GameTestAssertException("the kit machine takes cobblestone at no face");
    }

    private static CompoundTag at(final BlockPos pos) {
        final CompoundTag args = new CompoundTag();
        args.putLong(StudioKeys.POS, pos.asLong());
        return args;
    }
}
