/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import com.mojang.authlib.GameProfile;
import dev.jstech.core.jstechseries.builder.BuildJobs;
import dev.jstech.core.jstechseries.builder.BuilderPlan;
import dev.jstech.core.jstechseries.builder.BuilderState;
import dev.jstech.core.jstechseries.builder.MultiblockBuilder;
import dev.jstech.core.jstechseries.projector.ProjectorView;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestStructures;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Multiblock Builder: aimed at a terminal it sees what of the multiblock is missing and what the player has,
 * builds it block by block from the player's inventory, takes the block the player chose where a place takes several,
 * and stops when the player runs out, having placed what it could.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MultiblockBuilderGameTests {

    private static final String ARENA = "empty";
    /* The baker's terminal stands amid the north face of a box of three that runs south of it, off the floor. */
    private static final BlockPos TERMINAL = new BlockPos(3, 3, 2);
    private static final String IRON = "minecraft:iron_block";
    private static final String GLASS = "minecraft:glass";
    private static final int TIME = 400;

    private MultiblockBuilderGameTests() {
    }

    @GameTest(template = ARENA)
    public static void plan_seesWhatIsMissingAndWhatThePlayerHas(final GameTestHelper helper) {
        final FakePlayer player = builder(helper, "plan");
        player.getInventory().add(new ItemStack(Items.IRON_BLOCK, 64));
        final BuilderPlan plan = plan(helper, player, BuilderState.DEFAULT);
        helper.assertTrue(!plan.wants().isEmpty() && plan.wants().stream().allMatch(want -> want.fits()
                .contains(IRON)), "every place still missing takes iron; got " + plan.wants().size() + " places");
        helper.assertTrue(plan.already() == 0, "nothing is in place yet");
        helper.assertTrue(plan.groups().stream().anyMatch(group -> group.fits().contains(GLASS)),
                "the faces take iron or glass, a choice");
        helper.assertTrue(plan.order().ready(), "64 iron blocks build it all");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = TIME)
    public static void build_placesEveryMissingBlockFromTheInventory(final GameTestHelper helper) {
        final FakePlayer player = builder(helper, "build");
        player.getInventory().add(new ItemStack(Items.IRON_BLOCK, 64));
        final int wanted = plan(helper, player, BuilderState.DEFAULT).wants().size();
        BuildJobs.start(player, helper.absolutePos(TERMINAL), BuilderState.DEFAULT);
        helper.succeedWhen(() -> {
            helper.assertTrue(plan(helper, player, BuilderState.DEFAULT).wants().isEmpty(), "still building");
            helper.assertTrue(player.getInventory().countItem(Items.IRON_BLOCK) == 64 - wanted,
                    "each block placed was taken from the inventory, one each");
            helper.assertTrue(BuildJobs.building(player).isEmpty(), "it stops once it is built");
        });
    }

    @GameTest(template = ARENA, timeoutTicks = TIME)
    public static void build_takesTheBlockChosenWhereAPlaceTakesSeveral(final GameTestHelper helper) {
        final FakePlayer player = builder(helper, "choose");
        player.getInventory().add(new ItemStack(Items.IRON_BLOCK, 64));
        player.getInventory().add(new ItemStack(Items.GLASS, 64));
        final BuilderPlan before = plan(helper, player, BuilderState.DEFAULT);
        final String faces = before.groups().stream().filter(group -> group.fits().contains(GLASS)).findFirst()
                .orElseThrow().key();
        final BuilderState glassFaces = BuilderState.DEFAULT.choosing(faces, GLASS);
        BuildJobs.start(player, helper.absolutePos(TERMINAL), glassFaces);
        helper.succeedWhen(() -> {
            helper.assertTrue(plan(helper, player, glassFaces).wants().isEmpty(), "still building");
            final ProjectorView view = plan(helper, player, glassFaces).view();
            helper.assertTrue(view.places().stream().anyMatch(place -> helper.getLevel().getBlockState(place.pos())
                    .is(Blocks.GLASS)), "the faces are glass, as chosen");
        });
    }

    @GameTest(template = ARENA, timeoutTicks = TIME)
    public static void build_stopsWhenThePlayerRunsOut(final GameTestHelper helper) {
        final FakePlayer player = builder(helper, "short");
        player.getInventory().add(new ItemStack(Items.IRON_BLOCK, 3));
        final int wanted = plan(helper, player, BuilderState.DEFAULT).wants().size();
        BuildJobs.start(player, helper.absolutePos(TERMINAL), BuilderState.DEFAULT);
        helper.succeedWhen(() -> {
            helper.assertTrue(BuildJobs.building(player).isEmpty(), "it stops");
            helper.assertTrue(player.getInventory().countItem(Items.IRON_BLOCK) == 0, "having used all it had");
            helper.assertTrue(plan(helper, player, BuilderState.DEFAULT).wants().size() == wanted - 3,
                    "and placed the three blocks there were");
        });
    }

    @GameTest(template = ARENA)
    public static void item_isAimedAtATerminalItIsUsedOn(final GameTestHelper helper) {
        final FakePlayer player = builder(helper, "aim");
        final ItemStack stack = new ItemStack(MultiblockBuilder.ITEM.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        final BlockPos terminal = helper.absolutePos(TERMINAL);
        final InteractionResult used = stack.onItemUseFirst(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(terminal), Direction.NORTH, terminal, false)));
        helper.assertTrue(used.consumesAction(), "used on a terminal, it answers before the terminal does");
        helper.assertTrue(MultiblockBuilder.aim(stack, player).equals(Optional.of(terminal)),
                "and is aimed at it, in the player's world");
        final BlockPos floor = helper.absolutePos(new BlockPos(0, 1, 0));
        final ItemStack other = new ItemStack(MultiblockBuilder.ITEM.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, other);
        helper.assertTrue(other.onItemUseFirst(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(floor), Direction.UP, floor, false))) == InteractionResult.PASS,
                "used on another block, it lets the block answer");
        helper.assertTrue(MultiblockBuilder.aim(other, player).isEmpty() && MultiblockBuilder.state(other)
                .equals(BuilderState.DEFAULT), "and aims at nothing, set as it is made");
        helper.succeed();
    }

    /* The baker's terminal placed, and a player of the test's own standing north of it, out of the way. */
    private static FakePlayer builder(final GameTestHelper helper, final String name) {
        helper.setBlock(TERMINAL, TestStructures.BAKER.get());
        final FakePlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.nameUUIDFromBytes(
                ("builder-" + name).getBytes(StandardCharsets.UTF_8)), "builder-" + name));
        player.getInventory().clearContent();
        final BlockPos stand = helper.absolutePos(TERMINAL.north(2));
        player.setPos(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5);
        return player;
    }

    private static BuilderPlan plan(final GameTestHelper helper, final FakePlayer player, final BuilderState state) {
        return BuilderPlan.of(helper.getLevel(), helper.absolutePos(TERMINAL), player, state).orElseThrow();
    }
}
