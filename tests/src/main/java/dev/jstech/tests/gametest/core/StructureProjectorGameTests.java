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
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Structure Projector: it lists every multiblock that may be shown and leaves out the ones their mods hide, takes
 * only settings it can show, turns what it shows, and sees, in the world in front of it, what is placed, missing and
 * wrong.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class StructureProjectorGameTests {

    private static final String ARENA = "empty";
    /* The projector faces north as placed: what it shows stands north of it, above the arena's floor. */
    private static final BlockPos PROJECTOR = new BlockPos(2, 2, 5);
    private static final BlockPos TERMINAL = new BlockPos(2, 2, 3);

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
    public static void projector_seesWhatIsPlacedMissingAndWrongInFrontOfIt(final GameTestHelper helper) {
        final StructureProjectorBlockEntity projector = projector(helper);
        projector.apply(ProjectorSettings.FRESH.choosing(TestMultiblocks.FURNACE_ID));
        final ShapePreview preview = projector.preview().orElseThrow();
        helper.assertTrue(projector.terminalOf(preview).equals(helper.absolutePos(TERMINAL)),
                "the furnace stands in front of the projector, its near side a block away; got "
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
        helper.setBlock(PROJECTOR, StructureProjector.BLOCK.get());
        if (helper.getBlockEntity(PROJECTOR) instanceof StructureProjectorBlockEntity projector) {
            return projector;
        }
        throw new IllegalStateException("no projector where the test put it");
    }

    private static ResourceLocation rl(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsTests.MODID, path);
    }
}
