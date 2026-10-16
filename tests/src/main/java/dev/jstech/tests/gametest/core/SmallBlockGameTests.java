/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.jstechseries.energy.SharedEnergy;
import dev.jstech.core.multipart.MicroShape;
import dev.jstech.core.multipart.Microblock;
import dev.jstech.core.multipart.PartStates;
import dev.jstech.core.multipart.SmallBlocks;
import dev.jstech.core.multipart.SpaceBlockEntity;
import dev.jstech.core.multipart.SpaceBreaking;
import dev.jstech.core.multipart.SpaceParts;
import dev.jstech.core.multipart.SpacePlacement;
import dev.jstech.core.multipart.SpaceSlots;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestCables;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The game's small blocks in a block space: a torch hung beside a cable lights the cable's block and falls off with
 * the wall it hangs on, a lever powers what is beside the space, a button springs back after its time, a pressure
 * plate is pressed by what stands on it, each is placed from its item on the face of the block beside the space, and
 * each is kept in a save.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SmallBlockGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos GROUND = new BlockPos(2, 2, 2);
    private static final BlockPos SPACE = GROUND.above();
    private static final BlockPos CABLE = new BlockPos(5, 3, 5);
    private static final int TORCH_LIGHT = 14;

    private SmallBlockGameTests() {
    }

    @GameTest(template = ARENA)
    public static void torches_lightACableAndFallWithTheirWall(final GameTestHelper helper) {
        helper.setBlock(CABLE.west(), Blocks.STONE);
        final CableBlockEntity cable = TestCables.lay(helper, CABLE, SharedEnergy.INSULATED_COPPER_CABLE);
        final BlockState torch = Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.EAST);
        helper.assertTrue(cable.parts().addSmall(torch), "a torch hung on the wall west of the cable");
        same(helper, Direction.WEST, SmallBlocks.faceOf(torch), "it hangs on the cable's west face");
        same(helper, TORCH_LIGHT, state(helper, CABLE).getValue(PartStates.LIGHT), "the cable's block gives its light");

        helper.setBlock(CABLE.west(), Blocks.AIR);
        helper.assertTrue(cable.parts().small(Direction.WEST) == null, "with its wall gone, the torch falls");
        helper.assertItemEntityPresent(Items.TORCH, CABLE, 2.0);
        same(helper, 0, state(helper, CABLE).getValue(PartStates.LIGHT), "and its light goes");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void levers_powerWhatIsBesideTheSpace(final GameTestHelper helper) {
        final SpaceParts parts = space(helper);
        helper.setBlock(SPACE.east(), Blocks.REDSTONE_LAMP);
        final BlockState lever = Blocks.LEVER.defaultBlockState()
                .setValue(FaceAttachedHorizontalDirectionalBlock.FACE, AttachFace.FLOOR)
                .setValue(FaceAttachedHorizontalDirectionalBlock.FACING, Direction.NORTH);
        helper.assertTrue(parts.addSmall(lever), "a lever on the floor of the space");
        helper.assertTrue(SmallBlocks.use(helper.getLevel(), parts, Direction.DOWN, null), "the lever is pulled");
        helper.assertTrue(parts.small(Direction.DOWN).getValue(LeverBlock.POWERED), "and stands on");
        same(helper, 15, helper.getLevel().getSignal(helper.absolutePos(SPACE), Direction.EAST),
                "the space gives the lamp east of it a full signal");
        helper.succeedWhen(() -> helper.assertBlockProperty(SPACE.east(), RedstoneLampBlock.LIT, true));
    }

    @GameTest(template = ARENA)
    public static void buttons_springBackAfterTheirTime(final GameTestHelper helper) {
        final SpaceParts parts = space(helper);
        final BlockState button = Blocks.STONE_BUTTON.defaultBlockState()
                .setValue(FaceAttachedHorizontalDirectionalBlock.FACE, AttachFace.FLOOR)
                .setValue(FaceAttachedHorizontalDirectionalBlock.FACING, Direction.NORTH);
        helper.assertTrue(parts.addSmall(button), "a button on the floor of the space");
        SmallBlocks.use(helper.getLevel(), parts, Direction.DOWN, null);
        helper.assertTrue(parts.small(Direction.DOWN).getValue(ButtonBlock.POWERED), "the button is pressed");
        helper.assertTrue(state(helper, SPACE).getValue(PartStates.TICKING), "and the space ticks while it is");
        helper.succeedWhen(() -> {
            helper.assertTrue(!parts.small(Direction.DOWN).getValue(ButtonBlock.POWERED), "it springs back");
            helper.assertTrue(!state(helper, SPACE).getValue(PartStates.TICKING), "and the space rests again");
        });
    }

    @GameTest(template = ARENA)
    public static void pressurePlates_arePressedByWhatStandsOnThem(final GameTestHelper helper) {
        final SpaceParts parts = space(helper);
        helper.assertTrue(parts.addSmall(Blocks.OAK_PRESSURE_PLATE.defaultBlockState()), "a wooden plate on the floor");
        helper.spawnItem(Items.DIAMOND, new Vec3(SPACE.getX() + 0.5, SPACE.getY() + 0.05, SPACE.getZ() + 0.5));
        helper.succeedWhen(() -> helper.assertTrue(parts.small(Direction.DOWN).getValue(PressurePlateBlock.POWERED),
                "the diamond lying on it presses it"));
    }

    @GameTest(template = ARENA)
    public static void smallBlocks_arePlacedFromTheirItemIntoASpace(final GameTestHelper helper) {
        final SpaceParts parts = space(helper);
        final ServerLevel level = helper.getLevel();
        final FakePlayer player = FakePlayerFactory.getMinecraft(level);
        final ItemStack torches = new ItemStack(Items.TORCH, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, torches);
        final BlockPos ground = helper.absolutePos(GROUND);
        final BlockHitResult top = new BlockHitResult(Vec3.atCenterOf(ground).add(0, 0.5, 0), Direction.UP, ground,
                false);
        player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, top));
        helper.assertTrue(parts.small(Direction.DOWN) != null && parts.small(Direction.DOWN).is(Blocks.TORCH),
                "a torch stands on the space's floor");
        same(helper, 1, player.getMainHandItem().getCount(), "one torch used");
        same(helper, Items.TORCH, SpaceBreaking.takeOut(level, parts, SpaceSlots.face(Direction.DOWN
                .get3DDataValue())).getItem(), "taken out, it gives its torch back");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void smallBlocks_surviveAReload(final GameTestHelper helper) {
        final HolderLookup.Provider registries = helper.getLevel().registryAccess();
        space(helper);
        final SpaceBlockEntity space = entity(helper);
        final BlockState lever = Blocks.LEVER.defaultBlockState()
                .setValue(FaceAttachedHorizontalDirectionalBlock.FACE, AttachFace.FLOOR)
                .setValue(LeverBlock.POWERED, true);
        helper.assertTrue(space.parts().addSmall(lever), "a lever, on");
        final CompoundTag saved = space.saveWithoutMetadata(registries);
        final SpaceBlockEntity read = new SpaceBlockEntity(space.getBlockPos(), space.getBlockState());
        read.loadWithComponents(saved, registries);
        same(helper, lever, read.parts().small(Direction.DOWN), "the lever read back as it stood");
        helper.succeed();
    }

    /* A space of parts above stone, made by a cover on its ceiling, with its floor free for a small block. */
    private static SpaceParts space(final GameTestHelper helper) {
        helper.setBlock(GROUND, Blocks.STONE);
        final BlockPos pos = helper.absolutePos(SPACE);
        helper.assertTrue(SpacePlacement.place(helper.getLevel(), new SpacePlacement.Placement(pos, new Microblock(
                new Microblock.MicroblockPiece(MicroShape.FACE, MicroShape.COVER, Blocks.STONE),
                SpaceSlots.face(Direction.UP.get3DDataValue()), SpaceSlots.Y))), "a space made by a cover above");
        return entity(helper).parts();
    }

    private static SpaceBlockEntity entity(final GameTestHelper helper) {
        if (!(helper.getBlockEntity(SPACE) instanceof SpaceBlockEntity space)) {
            throw new GameTestAssertException("no space of parts at " + SPACE.toShortString());
        }
        return space;
    }

    private static BlockState state(final GameTestHelper helper, final BlockPos relative) {
        return helper.getLevel().getBlockState(helper.absolutePos(relative));
    }

    private static void same(final GameTestHelper helper, final Object expected, final Object actual,
                             final String what) {
        helper.assertTrue(Objects.equals(expected, actual), what + ": expected " + expected + ", got " + actual);
    }
}
