/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.Eras;
import dev.jstech.core.progression.IAxisStep;
import dev.jstech.core.progression.Knowledge;
import dev.jstech.core.progression.Knowledges;
import dev.jstech.core.progression.Milestone;
import dev.jstech.core.progression.Milestones;
import dev.jstech.core.progression.ProgressionAxes;
import dev.jstech.core.progression.ProgressionAxis;
import dev.jstech.core.progression.ProgressionTriggers;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * Progression the way a mod adds its own, to prove the Core's: an era of hardware between Advanced and Exa, an axis of
 * three stages moved by making and placing things, two milestones in order, a piece of knowledge that comes whole and
 * one that comes a share at a time. The test press has a recipe that asks for the whole one.
 */
@TextHolder
public final class TestProgression {

    public static final TextKey QUANTUM_NAME = TextKey.of("jstests.era.quantum", "Test Quantum");
    public static final TextKey AXIS_NAME = TextKey.of("jstests.axis.stages", "Test stages");
    public static final TextKey FUSION_NAME = TextKey.of("jstests.knowledge.fusion", "Test Fusion");
    public static final TextKey COMPREHENSION_NAME = TextKey.of("jstests.knowledge.comprehension",
            "Test Comprehension");
    public static final TextKey SUBORBITAL_NAME = TextKey.of("jstests.milestone.suborbital", "Test Suborbital");
    public static final TextKey ORBIT_NAME = TextKey.of("jstests.milestone.orbit", "Test Orbit");
    public static final TextKey UNREACHED_NAME = TextKey.of("jstests.knowledge.unreached", "Test Unreached");

    /** An add-on's era, right after Advanced. */
    public static final Era QUANTUM = Eras.declareAfter(new Era(id("test_quantum"), QUANTUM_NAME.text(), 0xFF00FF),
            Eras.idOf(HardwareEra.ADVANCED));
    /** An axis of three stages of the test mod's own. */
    public static final ProgressionAxis<Stage> STAGES = ProgressionAxis.of(id("test_stages"), AXIS_NAME,
            List.of(Stage.values()));
    /** Known whole once given; the locked test press recipe asks for it. */
    public static final Knowledge FUSION = Knowledges.declare(Knowledge.of(id("test_fusion"), FUSION_NAME));
    /** Known a share at a time. */
    public static final Knowledge COMPREHENSION = Knowledges.declare(new Knowledge(id("test_comprehension"),
            COMPREHENSION_NAME, true));
    /** Knowledge no test ever gives, so what needs it is never open: the manual's locked idea needs it. */
    public static final Knowledge UNREACHED = Knowledges.declare(Knowledge.of(id("test_unreached"), UNREACHED_NAME));
    /** Two firsts, in the order a team reaches them. */
    public static final List<Milestone> FIRSTS = Milestones.declare(id("test_firsts"), List.of(
            new Milestone(id("test_suborbital"), SUBORBITAL_NAME), new Milestone(id("test_orbit"), ORBIT_NAME)));

    private TestProgression() {
    }

    /** Declares it all, before the game has loaded. */
    public static void declare() {
        ProgressionAxes.register(STAGES);
        ProgressionTriggers.whenMade(Items.GOLD_BLOCK, STAGES, Stage.MIDDLE);
        ProgressionTriggers.whenPlaced(Blocks.LODESTONE, STAGES, Stage.LAST);
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsTests.MODID, path);
    }

    /** The test axis's stages. */
    public enum Stage implements IAxisStep {
        FIRST(0, "first"),
        MIDDLE(1, "middle"),
        LAST(2, "last");

        private final int level;
        private final String serializedName;

        Stage(final int level, final String serializedName) {
            this.level = level;
            this.serializedName = serializedName;
        }

        @Override
        public int level() {
            return this.level;
        }

        @Override
        public String serializedName() {
            return this.serializedName;
        }

        @Override
        public Text text() {
            return Text.literal(this.serializedName);
        }
    }
}
