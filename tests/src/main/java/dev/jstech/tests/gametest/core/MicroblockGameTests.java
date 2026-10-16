/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.cable.CableType;
import dev.jstech.core.jstechseries.energy.SharedEnergy;
import dev.jstech.core.jstechseries.tools.SharedTools;
import dev.jstech.core.multipart.CoreSpaces;
import dev.jstech.core.multipart.MicroShape;
import dev.jstech.core.multipart.Microblock;
import dev.jstech.core.multipart.MicroblockRecipes;
import dev.jstech.core.multipart.SpaceBlockEntity;
import dev.jstech.core.multipart.SpaceBreaking;
import dev.jstech.core.multipart.SpaceParts;
import dev.jstech.core.multipart.SpacePlacement;
import dev.jstech.core.multipart.SpaceSlots;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestCables;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Microblocks in block spaces: placed where a click puts them, a space made where nothing stands and gone when its
 * last piece is taken, what fits beside what, a cable's face closed by a cover and not by a hollow piece or a facade,
 * and the saw's cuts, joins and facades in the crafting grid.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MicroblockGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos GROUND = new BlockPos(2, 2, 2);
    private static final BlockPos ABOVE = GROUND.above();
    private static final BlockPos CABLE = new BlockPos(2, 2, 3);
    private static final BlockPos BESIDE = new BlockPos(4, 2, 4);
    private static final int DOWN = SpaceSlots.face(Direction.DOWN.get3DDataValue());
    private static final int UP = SpaceSlots.face(Direction.UP.get3DDataValue());
    private static final int NORTH = SpaceSlots.face(Direction.NORTH.get3DDataValue());
    private static final int EAST = SpaceSlots.face(Direction.EAST.get3DDataValue());

    private MicroblockGameTests() {
    }

    @GameTest(template = ARENA)
    public static void placing_makesASpaceAndPutsASecondPieceInIt(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        helper.setBlock(GROUND, Blocks.STONE);
        final BlockPos ground = helper.absolutePos(GROUND);
        final Microblock.MicroblockPiece slab = piece(MicroShape.FACE, MicroShape.SLAB, Blocks.OAK_PLANKS);

        final SpacePlacement.Placement first = target(level, ground, Vec3.atCenterOf(ground).add(0, 0.5, 0), slab);
        same(helper, ground.above(), first.pos(), "the space beyond the face clicked");
        same(helper, DOWN, first.block().slot(), "the slab against the space's floor");
        helper.assertTrue(SpacePlacement.place(level, first), "the first slab is placed");
        helper.assertBlockPresent(CoreSpaces.BLOCK.get(), ABOVE);

        final Vec3 onItsTop = Vec3.atBottomCenterOf(ground.above()).add(0, 0.5, 0);
        final SpacePlacement.Placement second = target(level, ground.above(), onItsTop, slab);
        same(helper, ground.above(), second.pos(), "a click on the slab's top stays in its space");
        same(helper, UP, second.block().slot(), "and makes the top slab");
        helper.assertTrue(SpacePlacement.place(level, second), "the second slab is placed");

        final SpaceParts parts = space(helper, ABOVE).parts();
        helper.assertTrue(parts.microblock(DOWN) != null && parts.microblock(UP) != null, "both slabs stand");
        helper.assertTrue(!parts.add(new Microblock(piece(MicroShape.POST, MicroShape.PANEL, Blocks.OAK_PLANKS),
                SpaceSlots.CENTRE, SpaceSlots.Y)), "no post through two slabs");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void breaking_takesOnePieceAndTheEmptySpaceGoes(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos pos = helper.absolutePos(ABOVE);
        final Microblock.MicroblockPiece cover = piece(MicroShape.FACE, MicroShape.COVER, Blocks.STONE);
        final Microblock.MicroblockPiece corner = piece(MicroShape.CORNER, MicroShape.PANEL, Blocks.BRICKS);
        helper.assertTrue(SpacePlacement.place(level, new SpacePlacement.Placement(pos,
                new Microblock(cover, DOWN, SpaceSlots.Y))), "the cover is placed");
        helper.assertTrue(SpacePlacement.place(level, new SpacePlacement.Placement(pos,
                new Microblock(corner, SpaceSlots.corner(true, true, true), SpaceSlots.Y))), "the corner is placed");
        final SpaceParts parts = space(helper, ABOVE).parts();

        same(helper, cover, MicroblockRecipes.pieceOf(SpaceBreaking.takeOut(level, parts, DOWN)), "the cover back");
        helper.assertBlockPresent(CoreSpaces.BLOCK.get(), ABOVE);
        helper.assertTrue(SpaceBreaking.takeOut(level, parts, DOWN).isEmpty(), "nothing where nothing stands");
        same(helper, corner, MicroblockRecipes.pieceOf(SpaceBreaking.takeOut(level, parts,
                SpaceSlots.corner(true, true, true))), "the corner back");
        helper.assertBlockPresent(Blocks.AIR, ABOVE);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void cableFaces_closeUnderACoverOnly(final GameTestHelper helper) {
        final CableType copper = SharedEnergy.INSULATED_COPPER_CABLE.get();
        TestCables.lay(helper, CABLE.north(), SharedEnergy.INSULATED_COPPER_CABLE);
        final CableBlockEntity cable = TestCables.lay(helper, CABLE, SharedEnergy.INSULATED_COPPER_CABLE);
        final SpaceParts parts = cable.parts();
        helper.assertTrue(cable.crosses(copper, Direction.NORTH), "the wire runs north to its neighbour");

        helper.assertTrue(parts.add(new Microblock(piece(MicroShape.FACE, MicroShape.COVER, Blocks.STONE), NORTH,
                SpaceSlots.Y)), "a cover on the cable's face");
        helper.assertTrue(!cable.crosses(copper, Direction.NORTH), "the cover closes the face");
        parts.removeMicroblock(NORTH);
        helper.assertTrue(cable.crosses(copper, Direction.NORTH), "taking it off opens it again");

        helper.assertTrue(parts.add(new Microblock(piece(MicroShape.HOLLOW, MicroShape.COVER, Blocks.STONE), NORTH,
                SpaceSlots.Y)), "a hollow cover on the face");
        helper.assertTrue(cable.crosses(copper, Direction.NORTH), "the hollow cover lets the wire through");
        parts.removeMicroblock(NORTH);

        helper.assertTrue(parts.add(new Microblock(piece(MicroShape.FACADE, MicroShape.COVER, Blocks.STONE), NORTH,
                SpaceSlots.Y)), "a facade on the face");
        helper.assertTrue(cable.crosses(copper, Direction.NORTH), "the facade hides the wire and leaves it linked");
        helper.assertTrue(!parts.add(new Microblock(piece(MicroShape.FACE, MicroShape.SLAB, Blocks.STONE), EAST,
                SpaceSlots.Y)), "a slab beside the cable would reach into it");

        final BlockPos alone = helper.absolutePos(BESIDE);
        helper.assertTrue(!SpacePlacement.place(helper.getLevel(), new SpacePlacement.Placement(alone,
                new Microblock(piece(MicroShape.FACADE, MicroShape.COVER, Blocks.STONE), NORTH, SpaceSlots.Y))),
                "no facade where no cable runs");
        helper.assertBlockPresent(Blocks.AIR, BESIDE);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void saw_cutsJoinsAndMakesFacadesInTheGrid(final GameTestHelper helper) {
        final ItemStack saw = new ItemStack(SharedTools.SAW.get());
        final ItemStack stone = new ItemStack(Items.STONE);
        final ItemStack slab = CoreSpaces.stack(piece(MicroShape.FACE, MicroShape.SLAB, Blocks.STONE));
        final ItemStack panel = CoreSpaces.stack(piece(MicroShape.FACE, MicroShape.PANEL, Blocks.STONE));
        final ItemStack cover = CoreSpaces.stack(piece(MicroShape.FACE, MicroShape.COVER, Blocks.STONE));

        made(helper, craft(helper, CraftingInput.of(1, 2, List.of(saw, stone))),
                piece(MicroShape.FACE, MicroShape.SLAB, Blocks.STONE), 2, "the saw above a block halves it");
        made(helper, craft(helper, CraftingInput.of(1, 2, List.of(saw, slab))),
                piece(MicroShape.FACE, MicroShape.PANEL, Blocks.STONE), 2, "and a slab");
        made(helper, craft(helper, CraftingInput.of(2, 1, List.of(saw, slab))),
                piece(MicroShape.POST, MicroShape.SLAB, Blocks.STONE), 2, "the saw beside a slab cuts it into posts");
        made(helper, craft(helper, CraftingInput.of(2, 2, List.of(saw, ItemStack.EMPTY, ItemStack.EMPTY, cover))),
                piece(MicroShape.HOLLOW, MicroShape.COVER, Blocks.STONE), 1, "at its corner it hollows a cover");
        made(helper, craft(helper, CraftingInput.of(2, 1, List.of(panel, panel))),
                piece(MicroShape.FACE, MicroShape.SLAB, Blocks.STONE), 1, "two panels join into a slab");
        made(helper, craft(helper, CraftingInput.of(2, 1, List.of(cover,
                new ItemStack(SharedEnergy.INSULATED_COPPER_CABLE)))),
                piece(MicroShape.FACADE, MicroShape.COVER, Blocks.STONE), 1, "a cover with a cable makes a facade");

        final ItemStack whole = craft(helper, CraftingInput.of(2, 1, List.of(slab, slab)));
        helper.assertTrue(whole.is(Items.STONE) && whole.getCount() == 1, "two slabs make the block, got " + whole);
        helper.assertTrue(craft(helper, CraftingInput.of(1, 2, List.of(saw, new ItemStack(Items.CHEST)))).isEmpty(),
                "no piece is cut from a block with an entity");
        helper.assertTrue(craft(helper, CraftingInput.of(2, 1, List.of(panel, CoreSpaces.stack(piece(MicroShape.FACE,
                MicroShape.PANEL, Blocks.BRICKS))))).isEmpty(), "pieces of two blocks do not join");

        final CraftingInput cut = CraftingInput.of(1, 2, List.of(saw, stone));
        final NonNullList<ItemStack> left = recipe(helper, cut).orElseThrow(() -> new GameTestAssertException(
                "no recipe cuts a block")).value().getRemainingItems(cut);
        helper.assertTrue(left.get(0).is(SharedTools.SAW.get()) && left.get(0).getDamageValue() == 1,
                "the saw stays in the grid, worn by one cut: " + left.get(0));
        helper.succeed();
    }

    private static Microblock.MicroblockPiece piece(final MicroShape shape, final int size, final Block material) {
        return new Microblock.MicroblockPiece(shape, size, material);
    }

    private static SpacePlacement.Placement target(final ServerLevel level, final BlockPos clicked,
                                                   final Vec3 location, final Microblock.MicroblockPiece piece) {
        return SpacePlacement.target(level, clicked, location, Direction.UP, piece).orElseThrow(() ->
                new GameTestAssertException("no place for " + piece + " clicked at " + location));
    }

    private static SpaceBlockEntity space(final GameTestHelper helper, final BlockPos relative) {
        if (!(helper.getBlockEntity(relative) instanceof SpaceBlockEntity space)) {
            throw new GameTestAssertException("no space of parts at " + relative.toShortString());
        }
        return space;
    }

    private static Optional<RecipeHolder<CraftingRecipe>> recipe(final GameTestHelper helper,
                                                                 final CraftingInput input) {
        final ServerLevel level = helper.getLevel();
        return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
    }

    /* What the grid makes, or nothing when no recipe takes it. */
    private static ItemStack craft(final GameTestHelper helper, final CraftingInput input) {
        return recipe(helper, input).map(holder -> holder.value().assemble(input,
                helper.getLevel().registryAccess())).orElse(ItemStack.EMPTY);
    }

    private static void made(final GameTestHelper helper, final ItemStack made,
                             final Microblock.MicroblockPiece expected, final int count, final String what) {
        helper.assertTrue(expected.equals(MicroblockRecipes.pieceOf(made)) && made.getCount() == count,
                what + ": expected " + count + " of " + expected + ", got " + made + " " + made.getComponents());
    }

    private static void same(final GameTestHelper helper, final Object expected, final Object actual,
                             final String what) {
        helper.assertTrue(Objects.equals(expected, actual), what + ": expected " + expected + ", got " + actual);
    }
}
