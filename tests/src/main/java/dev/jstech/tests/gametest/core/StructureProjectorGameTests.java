/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.jstechseries.projector.ProjectorSettings;
import dev.jstech.core.jstechseries.projector.ProjectorView;
import dev.jstech.core.jstechseries.projector.StructureProjector;
import dev.jstech.core.jstechseries.projector.StructureProjectorBlockEntity;
import dev.jstech.core.multiblock.Orientation;
import dev.jstech.core.multiblock.Projectables;
import dev.jstech.core.multiblock.ShapePreview;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestMultiblocks;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Structure Projector: it lists every multiblock that may be shown and leaves out the ones their mods hide, takes
 * only settings it can show, turns what it shows, shows it in front of whoever placed it, and sees there, in the
 * world, what is placed, missing and wrong.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class StructureProjectorGameTests {

    private static final String ARENA = "empty";
    /*
     * The projector as a player looking north places it, turned to face them, south: what it shows stands north of it,
     * in front of them, above the arena's floor.
     */
    private static final BlockPos PROJECTOR = new BlockPos(2, 2, 5);
    private static final BlockPos TERMINAL = new BlockPos(2, 2, 3);
    /* A player's turn looking north, in degrees. */
    private static final float LOOKING_NORTH = 180F;

    private StructureProjectorGameTests() {
    }

    @GameTest(template = ARENA)
    public static void projector_listsWhatMayBeShownAndNothingElse(final GameTestHelper helper) {
        final Set<ResourceLocation> listed = Projectables.all().keySet();
        helper.assertTrue(listed.contains(TestMultiblocks.FURNACE_ID), "a declared pattern is listed");
        helper.assertTrue(listed.contains(rl("baker")), "a machine declared as data with a shape of its own is too");
        helper.assertTrue(!listed.contains(rl("data_furnace")), "one whose shape names a pattern is listed as that"
                + " pattern alone");
        helper.assertTrue(!listed.contains(TestMultiblocks.HIDDEN_ID), "a pattern its mod hides is not listed");
        helper.assertTrue(Projectables.name(TestMultiblocks.FURNACE_ID).english().equals("Test Furnace"),
                "a multiblock with no name of its own is called after its id");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void projector_takesOnlySettingsItCanShow(final GameTestHelper helper) {
        final StructureProjectorBlockEntity projector = projector(helper);
        projector.apply(ProjectorSettings.FRESH.choosing(rl("nowhere")));
        helper.assertTrue(projector.settings().chosen().isEmpty(), "a multiblock that is nowhere is not chosen");
        projector.apply(new ProjectorSettings(Optional.of(TestMultiblocks.FURNACE_ID), 5, true, 1, true, false));
        final ProjectorSettings taken = projector.settings();
        helper.assertTrue(taken.chosen().equals(Optional.of(TestMultiblocks.FURNACE_ID)) && taken.turn() == 1
                && !taken.mirrored() && taken.layer() == 1 && taken.missingOnly() && !taken.on(),
                "it takes the furnace turned once, not mirrored since the furnace does not mirror; got " + taken);
        helper.assertTrue(projector.orientation().equals(Orientation.EAST), "turned once is east");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void projector_seesWhatIsPlacedMissingAndWrongWhereItShows(final GameTestHelper helper) {
        final StructureProjectorBlockEntity projector = projector(helper);
        projector.apply(ProjectorSettings.FRESH.choosing(TestMultiblocks.FURNACE_ID));
        final ShapePreview preview = projector.preview().orElseThrow();
        helper.assertTrue(projector.terminalOf(preview).equals(helper.absolutePos(TERMINAL)),
                "the furnace stands beyond the projector, its near side a block away; got "
                        + projector.terminalOf(preview));
        helper.setBlock(TERMINAL.west(), TestMultiblocks.PART.get());
        helper.setBlock(TERMINAL.east(), TestMultiblocks.PART.get());
        helper.setBlock(TERMINAL.north(), TestMultiblocks.PART.get());
        helper.setBlock(TERMINAL.south(), Blocks.DIRT);
        final ProjectorView view = ProjectorView.of(helper.getLevel(), projector).orElseThrow();
        helper.assertTrue(view.places().size() == 9, "its nine places, the terminal's counted; got "
                + view.places().size());
        helper.assertTrue(view.placed() == 3 && view.wrong() == 1 && view.missing() == 5,
                "three placed, one wrong, five missing; got " + view.placed() + ", " + view.wrong() + ", "
                        + view.missing());
        helper.assertTrue(view.missingByBlock().getOrDefault("jstests:test_furnace_part", 0) == 4,
                "four furnace parts still to place; got " + view.missingByBlock());
        helper.assertTrue(view.upTo(1).size() == 9, "its one layer is all of it");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void projector_showsWhatItShowsInFrontOfWhoPlacedIt(final GameTestHelper helper) {
        final Player player = helper.makeMockPlayer(GameType.CREATIVE);
        player.setYRot(LOOKING_NORTH);
        final ItemStack item = new ItemStack(StructureProjector.BLOCK.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, item);
        helper.setBlock(PROJECTOR.below(), Blocks.STONE);
        final BlockPos floor = helper.absolutePos(PROJECTOR.below());
        item.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(floor).add(0, 0.5, 0), Direction.UP, floor, false)));
        if (!(helper.getBlockEntity(PROJECTOR) instanceof StructureProjectorBlockEntity projector)) {
            throw new IllegalStateException("the player did not place the projector");
        }
        projector.apply(ProjectorSettings.FRESH.choosing(TestMultiblocks.FURNACE_ID));
        final BlockPos terminal = projector.terminalOf(projector.preview().orElseThrow());
        helper.assertTrue(terminal.equals(helper.absolutePos(TERMINAL)),
                "a player looking north places it: the furnace stands north of it, in front of them, not behind them;"
                        + " got " + helper.relativePos(terminal));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void projector_marksWhatStandsWhereItMustBeEmpty(final GameTestHelper helper) {
        final ShapePreview preview = ShapePreview.of(
                List.of(new ShapePreview.Ghost(0, 0, 0, 0, List.of(), List.of(), true)),
                List.of(new ShapePreview.Ghost(0, -1, 0, 0, List.of("minecraft:air"), List.of(), false)), 0);
        final BlockPos terminal = helper.absolutePos(TERMINAL);
        helper.setBlock(TERMINAL.below(), Blocks.DIRT);
        final ProjectorView dirt = ProjectorView.of(helper.getLevel(), preview, terminal);
        helper.assertTrue(dirt.toClear() == 1 && dirt.clears().getFirst().state() == ProjectorView.State.WRONG,
                "dirt where it must be empty is to be cleared; got " + dirt.toClear());
        helper.setBlock(TERMINAL.below(), Blocks.AIR);
        final ProjectorView dug = ProjectorView.of(helper.getLevel(), preview, terminal);
        helper.assertTrue(dug.toClear() == 0 && dug.clears().getFirst().state() == ProjectorView.State.PLACED,
                "once dug out, nothing is");
        helper.assertTrue(dug.places().size() == 1, "and the place left empty is no place to build");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void projector_holdsAKnownTerminalToItsBlock(final GameTestHelper helper) {
        final StructureProjectorBlockEntity projector = projector(helper);
        projector.apply(ProjectorSettings.FRESH.choosing(TestMultiblocks.FURNACE_ID));
        helper.setBlock(TERMINAL, Blocks.DIRT);
        final ProjectorView wrong = ProjectorView.of(helper.getLevel(), projector).orElseThrow();
        helper.assertTrue(stateAt(wrong, helper.absolutePos(TERMINAL)) == ProjectorView.State.WRONG,
                "dirt where the furnace's named terminal goes is wrong");
        helper.setBlock(TERMINAL, TestMultiblocks.TERMINAL.get());
        final ProjectorView right = ProjectorView.of(helper.getLevel(), projector).orElseThrow();
        helper.assertTrue(stateAt(right, helper.absolutePos(TERMINAL)) == ProjectorView.State.PLACED,
                "the terminal itself is placed");
        helper.succeed();
    }

    private static ProjectorView.State stateAt(final ProjectorView view, final BlockPos at) {
        return view.places().stream().filter(place -> place.pos().equals(at)).findFirst().orElseThrow().state();
    }

    private static StructureProjectorBlockEntity projector(final GameTestHelper helper) {
        helper.setBlock(PROJECTOR, StructureProjector.BLOCK.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        if (helper.getBlockEntity(PROJECTOR) instanceof StructureProjectorBlockEntity projector) {
            return projector;
        }
        throw new IllegalStateException("no projector where the test put it");
    }

    private static ResourceLocation rl(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsTests.MODID, path);
    }
}
