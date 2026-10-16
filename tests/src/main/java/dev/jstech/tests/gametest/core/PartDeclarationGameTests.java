/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.gametest.ScenarioBuilder;
import dev.jstech.core.jstechseries.energy.SharedEnergy;
import dev.jstech.core.multipart.CoreParts;
import dev.jstech.core.multipart.MicroShape;
import dev.jstech.core.multipart.Microblock;
import dev.jstech.core.multipart.PartItem;
import dev.jstech.core.multipart.SpaceParts;
import dev.jstech.core.multipart.SpaceSlots;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestParts;
import dev.jstech.tests.TestToolkit;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A kind of part declared the Core's way registers its kind and its item under one id, its item mounts it on a cable's
 * face or on the face of a space beside any block, and the GameTest kit builds spaces of parts as a test needs them.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class PartDeclarationGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos GROUND = new BlockPos(2, 2, 2);
    private static final BlockPos CABLE = new BlockPos(5, 2, 5);

    private PartDeclarationGameTests() {
    }

    @GameTest(template = ARENA)
    public static void declaredParts_registerTheirKindAndItemUnderOneId(final GameTestHelper helper) {
        final ResourceLocation id = ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "plate_part");
        same(helper, TestParts.PLATE.get(), CoreParts.REGISTRY.get(id), "the kind under its id");
        helper.assertTrue(TestParts.PLATE.item().get() instanceof PartItem item
                && item.partType() == TestParts.PLATE.get(), "its item mounts it");
        same(helper, ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "block/part/plate_part"),
                TestParts.PLATE.get().model(), "drawn by the plate the data generation writes");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void partItems_mountOnACableOrBesideABlock(final GameTestHelper helper) {
        helper.setBlock(GROUND, Blocks.STONE);
        final ScenarioBuilder build = ScenarioBuilder.forGameTest(helper);
        build.setBlock(CABLE, SharedEnergy.INSULATED_COPPER_CABLE);
        final FakePlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(TestParts.PLATE.item().get(), 2));

        use(helper, player, GROUND, Direction.UP);
        same(helper, TestParts.PLATE.get(), build.partsAt(GROUND.above()).type(Direction.DOWN),
                "used on the stone's top, it stands on the floor of a space made above it");
        use(helper, player, CABLE, Direction.NORTH);
        same(helper, TestParts.PLATE.get(), build.partsAt(CABLE).type(Direction.NORTH),
                "used on a cable, it stands on the face clicked");
        helper.assertTrue(player.getMainHandItem().isEmpty(), "both items used");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void scenarioKit_buildsSpacesOfParts(final GameTestHelper helper) {
        helper.setBlock(GROUND, Blocks.STONE);
        final ScenarioBuilder build = ScenarioBuilder.forGameTest(helper);
        final BlockPos space = GROUND.above();
        build.placeMicroblock(space, new Microblock(new Microblock.MicroblockPiece(MicroShape.FACE, MicroShape.COVER,
                Blocks.STONE), SpaceSlots.face(Direction.UP.get3DDataValue()), SpaceSlots.Y));
        final TestToolkit.LampPart lamp = build.mountPart(space, Direction.NORTH, TestToolkit.LAMP_PART.get());
        build.hangSmall(space, Blocks.LEVER.defaultBlockState()
                .setValue(FaceAttachedHorizontalDirectionalBlock.FACE, AttachFace.FLOOR));
        final SpaceParts parts = build.partsAt(space);
        same(helper, lamp, parts.get(Direction.NORTH), "the lamp mounted");
        helper.assertTrue(parts.small(Direction.DOWN) != null && parts.small(Direction.DOWN).is(Blocks.LEVER),
                "the lever hung");
        same(helper, 3, parts.count(), "a cover, a lamp and a lever");
        helper.assertTrue(build.wrote(helper.absolutePos(space)), "the space is the scenario's own");
        helper.succeed();
    }

    private static void use(final GameTestHelper helper, final FakePlayer player, final BlockPos relative,
                            final Direction face) {
        final BlockPos pos = helper.absolutePos(relative);
        final Vec3 at = Vec3.atCenterOf(pos).add(Vec3.atLowerCornerOf(face.getNormal()).scale(0.5));
        player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(at, face, pos, false)));
    }

    private static void same(final GameTestHelper helper, final Object expected, final Object actual,
                             final String what) {
        helper.assertTrue(Objects.equals(expected, actual), what + ": expected " + expected + ", got " + actual);
    }
}
