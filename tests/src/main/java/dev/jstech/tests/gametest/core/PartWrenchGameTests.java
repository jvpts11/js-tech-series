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
import dev.jstech.core.jstechseries.tools.SharedTools;
import dev.jstech.core.multipart.MicroShape;
import dev.jstech.core.multipart.Microblock;
import dev.jstech.core.multipart.PlacedModel;
import dev.jstech.core.multipart.SpaceBlockEntity;
import dev.jstech.core.multipart.SpaceParts;
import dev.jstech.core.multipart.SpacePlacement;
import dev.jstech.core.multipart.SpaceSlots;
import dev.jstech.core.text.Text;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestToolkit;
import dev.jstech.tests.testkit.TestCables;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The wrench on parts: used on a lever on a floor it turns it, sneaking it takes a part off into the player's hands, a
 * device of a kind that turns goes round a quarter turn at a time, model and room together, a post through the middle
 * changes its axis, and a part looked at tells the look-at tooltip what it is doing.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class PartWrenchGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos GROUND = new BlockPos(2, 2, 2);
    private static final BlockPos SPACE = GROUND.above();
    private static final BlockPos CABLE = new BlockPos(5, 2, 5);
    private static final int FLOOR = SpaceSlots.face(Direction.DOWN.get3DDataValue());

    private PartWrenchGameTests() {
    }

    @GameTest(template = ARENA)
    public static void wrench_turnsALeverAndTakesItOffSneaking(final GameTestHelper helper) {
        final SpaceParts parts = space(helper);
        final BlockState lever = Blocks.LEVER.defaultBlockState()
                .setValue(FaceAttachedHorizontalDirectionalBlock.FACE, AttachFace.FLOOR)
                .setValue(FaceAttachedHorizontalDirectionalBlock.FACING, Direction.NORTH);
        helper.assertTrue(parts.addSmall(lever), "a lever on the floor");
        final FakePlayer player = lookingDown(helper);

        useWrench(helper, player);
        same(helper, Direction.EAST, parts.small(Direction.DOWN)
                .getValue(FaceAttachedHorizontalDirectionalBlock.FACING), "the wrench turned the lever a quarter");

        player.setShiftKeyDown(true);
        useWrench(helper, player);
        player.setShiftKeyDown(false);
        helper.assertTrue(parts.small(Direction.DOWN) == null, "sneaking, the wrench took the lever off");
        helper.assertTrue(player.getInventory().countItem(Items.LEVER) == 1, "into the player's hands");
        player.getInventory().clearContent();
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void devicesOfAKindThatTurns_goRoundAQuarterAtATime(final GameTestHelper helper) {
        final CableBlockEntity cable = TestCables.lay(helper, CABLE, SharedEnergy.INSULATED_COPPER_CABLE);
        cable.addPart(Direction.UP, TestToolkit.LAMP_PART.get().create());
        cable.addPart(Direction.NORTH, TestToolkit.PROBE_PART.get().create());
        final int top = SpaceSlots.face(Direction.UP.get3DDataValue());
        helper.assertTrue(cable.parts().turn(top), "the lamp turns");
        same(helper, 1, cable.parts().turns(Direction.UP), "a quarter turn");
        helper.assertTrue(cable.parts().layout().models().stream().filter(placed -> placed.facing() == Direction.UP)
                .mapToInt(PlacedModel::turns).allMatch(turns -> turns == 1), "its model turned with it");
        helper.assertTrue(!cable.parts().canTurn(SpaceSlots.face(Direction.NORTH.get3DDataValue())),
                "a probe, of a kind that does not turn, is never turned");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void postsThroughTheMiddle_changeTheirAxis(final GameTestHelper helper) {
        final BlockPos pos = helper.absolutePos(SPACE);
        helper.assertTrue(SpacePlacement.place(helper.getLevel(), new SpacePlacement.Placement(pos, new Microblock(
                new Microblock.MicroblockPiece(MicroShape.POST, MicroShape.PANEL, Blocks.OAK_PLANKS),
                SpaceSlots.CENTRE, SpaceSlots.Y))), "an upright post through the middle");
        final SpaceParts parts = entity(helper).parts();
        helper.assertTrue(parts.turn(SpaceSlots.CENTRE), "the post turns");
        same(helper, SpaceSlots.Z, parts.microblock(SpaceSlots.CENTRE).axis(), "it runs north to south now");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void partsLookedAt_sayWhatTheyAreDoing(final GameTestHelper helper) {
        final SpaceParts parts = space(helper);
        final BlockState lever = Blocks.LEVER.defaultBlockState()
                .setValue(FaceAttachedHorizontalDirectionalBlock.FACE, AttachFace.FLOOR)
                .setValue(FaceAttachedHorizontalDirectionalBlock.FACING, Direction.NORTH);
        parts.addSmall(lever.setValue(LeverBlock.POWERED, true));
        final List<Text> lines = new ArrayList<>();
        entity(helper).describe(lines, false, lookingDown(helper));
        same(helper, 1, lines.size(), "the lever looked at says its redstone: " + lines);
        helper.succeed();
    }

    /* A space of parts above stone, made by a cover on its north wall, open above for a look down. */
    private static SpaceParts space(final GameTestHelper helper) {
        helper.setBlock(GROUND, Blocks.STONE);
        final BlockPos pos = helper.absolutePos(SPACE);
        helper.assertTrue(SpacePlacement.place(helper.getLevel(), new SpacePlacement.Placement(pos, new Microblock(
                new Microblock.MicroblockPiece(MicroShape.FACE, MicroShape.COVER, Blocks.STONE),
                SpaceSlots.face(Direction.NORTH.get3DDataValue()), SpaceSlots.Y))), "a space made by a cover");
        return entity(helper).parts();
    }

    private static SpaceBlockEntity entity(final GameTestHelper helper) {
        if (!(helper.getBlockEntity(SPACE) instanceof SpaceBlockEntity space)) {
            throw new GameTestAssertException("no space of parts at " + SPACE.toShortString());
        }
        return space;
    }

    /* A player standing in the space, its eyes in the block above, looking straight down at the floor's middle. */
    private static FakePlayer lookingDown(final GameTestHelper helper) {
        final FakePlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        final BlockPos pos = helper.absolutePos(SPACE);
        player.moveTo(pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5, 0F, 90F);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(SharedTools.WRENCH.get()));
        return player;
    }

    private static void useWrench(final GameTestHelper helper, final FakePlayer player) {
        final ServerLevel level = helper.getLevel();
        final BlockPos pos = helper.absolutePos(SPACE);
        player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
    }

    private static void same(final GameTestHelper helper, final Object expected, final Object actual,
                             final String what) {
        helper.assertTrue(Objects.equals(expected, actual), what + ": expected " + expected + ", got " + actual);
    }
}
