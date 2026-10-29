/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.Elements;
import dev.jstech.core.jstechseries.SharedCatalogue;
import dev.jstech.core.material.MaterialLiquids;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructureResult;
import dev.jstech.core.moving.StructureYard;
import dev.jstech.core.moving.YardLayout;
import dev.jstech.space.api.Propellant;
import dev.jstech.space.api.Propellants;
import dev.jstech.space.api.RocketPart;
import dev.jstech.space.api.RocketParts;
import dev.jstech.space.rocket.RocketBoilOff;
import dev.jstech.space.rocket.RocketStack;
import dev.jstech.space.rocket.RocketStages;
import dev.jstech.space.rocket.Rockets;
import dev.jstech.space.rocket.SkeletonPropellants;
import dev.jstech.space.rocket.SkeletonRocketParts;
import dev.jstech.space.rocket.StageMath;
import dev.jstech.space.rocket.TankLoad;
import dev.jstech.tests.JsTests;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * What rockets burn: the skeleton's methalox, liquid methane and oxygen in Raptor's mixture, both liquids there to be
 * held; the burns read from what the tanks really hold, an engine lighting only on its own propellant; and the cold
 * escaping the tanks, liquid hydrogen fastest, oxygen next, methane least, a liquid that keeps at the place's warmth
 * nothing, a rocket's tanks losing some as it stands.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class PropellantGameTests {

    private static final String ARENA = "empty";
    private static final String BATCH = "jsspace_propellants";
    private static final ResourceLocation METHANE = SharedCatalogue.METHANE.id();
    private static final ResourceLocation OXYGEN = Elements.of(Element.OXYGEN).id();
    private static final ResourceLocation HYDROGEN = Elements.of(Element.HYDROGEN).id();
    private static final ResourceLocation WATER = SharedCatalogue.WATER.id();
    /* The Overworld's warmth, a place colder than any of the liquids boil, and an hour, in kelvin and seconds. */
    private static final double WARM = 288;
    private static final double COLD = 60;
    private static final double HOUR = 3_600;
    /* The first stage's tank: eighteen layers of the small tank. */
    private static final int LAYERS = 18;

    private PropellantGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void methalox_isLiquidMethaneAndOxygenInRaptorsMixture(final GameTestHelper helper) {
        final Propellant methalox = Propellants.get(helper.getLevel(), SkeletonPropellants.METHALOX).orElseThrow();
        helper.assertTrue(Math.abs(methalox.share(OXYGEN) / methalox.share(METHANE) - 3.6) < 1e-9,
                "3.6 of oxygen to 1 of methane");
        helper.assertTrue(methalox.components().stream().allMatch(c -> c.state() == MatterState.LIQUID),
                "both carried liquid");
        helper.assertTrue(!methalox.solid() && !methalox.hypergolic(), "neither a solid grain nor hypergolic");
        helper.assertTrue(MaterialLiquids.of(SharedCatalogue.METHANE).isPresent()
                && MaterialLiquids.of(Elements.of(Element.OXYGEN)).isPresent(), "both liquids exist");
        helper.assertTrue(RocketParts.declared().values().stream().flatMap(part -> part.engine().stream())
                .allMatch(engine -> engine.propellant().equals(SkeletonPropellants.METHALOX)),
                "every engine of the skeleton burns it");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void burns_comeFromWhatTheTanksHoldAndTheEngineLightsOnlyOnItsOwn(final GameTestHelper helper) {
        final Map<ResourceLocation, RocketPart> parts = RocketParts.all(helper.getLevel());
        final Map<ResourceLocation, Propellant> propellants = Propellants.all(helper.getLevel());
        final Propellant methalox = propellants.get(SkeletonPropellants.METHALOX);
        final RocketStack stack = RocketStack.of(part("sea_level_engine_3", 1), part("tank_3", LAYERS),
                part("separation_ring_3", 1), part("vacuum_engine_3", 1), part("tank_3", 4),
                part("capsule_3", 1));
        final double full = 5_800.0 * LAYERS;
        final double design = StageMath.deltaV(RocketStages.burns(parts, stack).orElseThrow(), 0);
        final Map<Integer, TankLoad> filled = Map.of(1, TankLoad.filled(SkeletonPropellants.METHALOX, methalox,
                full), 4, TankLoad.filled(SkeletonPropellants.METHALOX, methalox, 5_800.0 * 4));
        final double loaded = StageMath.deltaV(RocketStages.burns(parts, stack, propellants, filled).orElseThrow(),
                0);
        helper.assertTrue(Math.abs(loaded - design) < 1e-6, "full tanks give the design's delta-v: " + loaded);
        final Map<Integer, TankLoad> half = Map.of(1, TankLoad.filled(SkeletonPropellants.METHALOX, methalox,
                full / 2), 4, filled.get(4));
        helper.assertTrue(StageMath.deltaV(RocketStages.burns(parts, stack, propellants, half).orElseThrow(), 0)
                < design, "half a tank gives less");
        // A first stage filled with water: its engine does not light, and the water is carried.
        final TankLoad water = new TankLoad(ResourceLocation.fromNamespaceAndPath("jstests", "water"),
                Map.of(WATER, full));
        final List<StageMath.Burn> wrong = RocketStages.burns(parts, stack, propellants, Map.of(1, water, 4,
                filled.get(4))).orElseThrow();
        helper.assertTrue(wrong.getFirst().thrust() == 0 && wrong.getFirst().vacuumDeltaV() == 0,
                "an engine does not light on the wrong propellant");
        helper.assertTrue(wrong.getFirst().startMass() > RocketStages.burns(parts, stack, propellants,
                Map.of(4, filled.get(4))).orElseThrow().getFirst().startMass(), "and what it holds is carried");
        helper.assertTrue(water.usable(SkeletonPropellants.METHALOX, methalox) == 0, "none of it burns");
        // Oxygen left over once the methane is short is carried and never burnt.
        final TankLoad lopsided = filled.get(1).with(METHANE, 1_000);
        helper.assertTrue(Math.abs(lopsided.usable(SkeletonPropellants.METHALOX, methalox)
                - 1_000 / methalox.share(METHANE)) < 1e-6, "the scarcer material sets what burns");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void boilOff_hydrogenFastestOxygenNextMethaneLeastAndWarmLiquidsNothing(
            final GameTestHelper helper) {
        final double surface = RocketBoilOff.surface(3, LAYERS);
        // Each tank full: a cubic metre a block, its liquid as dense as it is.
        final double volume = Math.PI * 1.5 * 1.5 * LAYERS * 1_000;
        final double hydrogen = lostShare(HYDROGEN, volume * 0.070_85, surface);
        final double oxygen = lostShare(OXYGEN, volume * 1.141, surface);
        final double methane = lostShare(METHANE, volume * 0.422_6, surface);
        helper.assertTrue(hydrogen > oxygen && oxygen > methane && methane > 0,
                "hydrogen " + hydrogen + ", oxygen " + oxygen + ", methane " + methane);
        helper.assertTrue(lostShare(WATER, volume, surface) == 0, "water keeps at the place's warmth");
        final TankLoad cold = RocketBoilOff.boil(new TankLoad(SkeletonPropellants.METHALOX, Map.of(OXYGEN, 1_000.0)),
                surface, COLD, HOUR);
        helper.assertTrue(cold.kilograms().get(OXYGEN) == 1_000, "where it is colder than it boils, nothing goes");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void rocket_standingLosesSomeOfItsTanksOxygen(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final Propellant methalox = Propellants.get(level, SkeletonPropellants.METHALOX).orElseThrow();
        final BlockPos at = new BlockPos(2, 2, 2);
        helper.setBlock(at, Blocks.IRON_BLOCK);
        warm(level);
        final StructureResult made = MovingStructures.assembleFor(level, helper.absolutePos(at),
                helper.absolutePos(at), UUID.randomUUID());
        helper.assertTrue(made.ok(), "the rocket's blocks are a moving structure");
        final RocketStack stack = RocketStack.of(part("sea_level_engine_3", 1), part("tank_3", LAYERS));
        Rockets.register(level.getServer(), made.structure().id(), stack, UUID.randomUUID());
        final TankLoad full = TankLoad.filled(SkeletonPropellants.METHALOX, methalox, 5_800.0 * LAYERS);
        helper.assertTrue(Rockets.load(level.getServer(), made.structure().id(), 1, full), "its tank fills");
        helper.assertTrue(!Rockets.load(level.getServer(), made.structure().id(), 9, full),
                "a place it does not have takes nothing");
        helper.runAfterDelay(60, () -> {
            try {
                final TankLoad left = Rockets.settle(level.getServer(), made.structure().id()).orElseThrow().tanks()
                        .get(1);
                helper.assertTrue(left.kilograms().get(OXYGEN) < full.kilograms().get(OXYGEN),
                        "standing three seconds, some oxygen boiled off: " + left.kilograms().get(OXYGEN));
                helper.assertTrue(full.kilograms().get(OXYGEN) - left.kilograms().get(OXYGEN) < 1,
                        "a little, through its foam");
            } finally {
                Rockets.forget(level.getServer(), made.structure().id());
                MovingStructures.takeAway(made.structure());
            }
            helper.succeed();
        });
    }

    /* The share of a full tank of {@code material} lost in an hour at the Overworld's warmth. */
    private static double lostShare(final ResourceLocation material, final double kilograms, final double surface) {
        final TankLoad load = new TankLoad(SkeletonPropellants.METHALOX, Map.of(material, kilograms));
        return 1 - RocketBoilOff.boil(load, surface, WARM, HOUR).kilograms().get(material) / kilograms;
    }

    private static RocketStack.Part part(final String path, final int layers) {
        return new RocketStack.Part(SkeletonRocketParts.id(path), layers);
    }

    /* Loads the chunks of the yard's next plot, so a structure made comes into it at once. */
    private static void warm(final ServerLevel level) {
        final int plot = StructureYard.nextPlot(level);
        final int cx = YardLayout.centreX(plot) >> 4;
        final int cz = YardLayout.centreZ(plot) >> 4;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                level.getChunk(cx + dx, cz + dz);
            }
        }
    }
}
