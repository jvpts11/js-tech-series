/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.core.audio.acoustics.SoundMedium;
import dev.jstech.core.dimension.Climate;
import dev.jstech.core.dimension.DimensionRulesData;
import dev.jstech.core.jstechseries.persistor.SpacePersistor;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructureAssembly;
import dev.jstech.core.moving.StructureResult;
import dev.jstech.core.room.SealedRooms;
import dev.jstech.core.team.Access;
import dev.jstech.core.team.Ownership;
import dev.jstech.space.api.OrbitKind;
import dev.jstech.space.api.ProbeTypes;
import dev.jstech.space.api.SatelliteTypes;
import dev.jstech.space.api.SpacecraftSize;
import dev.jstech.space.orbit.OrbitObject;
import dev.jstech.space.orbit.OrbitObjects;
import dev.jstech.space.rocket.RocketStack;
import dev.jstech.space.rocket.Rockets;
import dev.jstech.space.rocket.SkeletonRocketParts;
import dev.jstech.space.spacecraft.SkeletonSpacecraft;
import dev.jstech.space.spacecraft.SpacecraftBuild;
import dev.jstech.space.universe.SolarBody;
import dev.jstech.space.universe.SolarSystemWorlds;
import dev.jstech.space.universe.StarSystems;
import dev.jstech.space.universe.SystemSpaces;
import dev.jstech.tests.JsTests;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * In orbit: the system's space is a vacuum with no gravity, where no sound carries and a loose thing floats where it
 * was let go. The things in a world's orbit are kept with their owners: a satellite let go by its owner works no more
 * and another team may take it, and destroyed it becomes debris; a rocket passing into orbit is told there, kept loaded
 * when a Space Persistor stands aboard. The kinds of satellite and of probe are declared, and a spacecraft is whole
 * once it has one of each component and its mission module.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class OrbitGameTests {

    private static final String ARENA = "empty";
    private static final String BATCH = "jsspace_orbit";
    /* Where a loose thing is let go in the system's space, far from every world, and how long it is watched. */
    private static final BlockPos FLOATING = new BlockPos(0, 200, -40_000);
    private static final int WATCHED = 40;
    private static final double STILL = 1.0e-3;

    private OrbitGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void space_isAVacuumWithNoGravityWhereLooseThingsFloat(final GameTestHelper helper) {
        final ServerLevel space = SystemSpaces.spaceOf(helper.getLevel().getServer(), StarSystems.SOLAR_SYSTEM);
        helper.assertTrue(DimensionRulesData.of(space).gravity() == 0 && DimensionRulesData.of(space).pressure() == 0,
                "the system's space has no gravity and no air");
        final Climate place = SealedRooms.placeClimate(space, Vec3.atCenterOf(FLOATING));
        helper.assertFalse(SoundMedium.of(place.pressure(), place.temperature(), false).carries(),
                "no sound carries there");
        space.setChunkForced(FLOATING.getX() >> 4, FLOATING.getZ() >> 4, true);
        final ItemEntity loose = new ItemEntity(space, FLOATING.getX() + 0.5, FLOATING.getY() + 0.5,
                FLOATING.getZ() + 0.5, new ItemStack(Items.IRON_INGOT));
        loose.setDeltaMovement(Vec3.ZERO);
        space.addFreshEntity(loose);
        final double from = loose.getY();
        final long until = space.getGameTime() + WATCHED;
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(space.getGameTime() >= until && loose.tickCount > 0,
                        "the loose thing watched a while"))
                .thenExecute(() -> {
                    final double moved = Math.abs(loose.getY() - from);
                    loose.discard();
                    space.setChunkForced(FLOATING.getX() >> 4, FLOATING.getZ() >> 4, false);
                    helper.assertTrue(moved < STILL, "it floats where it was let go: moved " + moved);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void satellite_isLetGoTakenByAnotherTeamAndBrokenIntoDebris(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final UUID id = UUID.randomUUID();
        final Ownership first = Ownership.of(UUID.randomUUID()).withAccess(Access.TEAM);
        final Ownership second = Ownership.of(UUID.randomUUID()).withAccess(Access.TEAM);
        final ResourceLocation overworld = SolarSystemWorlds.worldOf(SolarBody.OVERWORLD);
        OrbitObjects.put(level.getServer(), id, OrbitObject.satellite(overworld, OrbitKind.GEOSTATIONARY,
                SkeletonSpacecraft.id("weather"), first));
        helper.assertTrue(OrbitObjects.around(level.getServer(), overworld).containsKey(id),
                "it goes round the Overworld");
        helper.assertTrue(!OrbitObjects.take(level.getServer(), id, second), "one at work cannot be taken");
        helper.assertTrue(OrbitObjects.letGo(level.getServer(), id) && !OrbitObjects.get(level.getServer(), id)
                .orElseThrow().working(), "let go by its owner, it works no more");
        helper.assertTrue(OrbitObjects.take(level.getServer(), id, second), "another team takes it");
        final OrbitObject taken = OrbitObjects.get(level.getServer(), id).orElseThrow();
        helper.assertTrue(taken.working() && taken.ownership().equals(second), "it is theirs and works again");
        helper.assertTrue(OrbitObjects.destroy(level.getServer(), id, 3), "it is destroyed");
        final OrbitObject debris = OrbitObjects.get(level.getServer(), id).orElseThrow();
        helper.assertTrue(debris.kind() == OrbitObject.Kind.DEBRIS && debris.pieces() == 3 && !debris.working()
                && !OrbitObjects.letGo(level.getServer(), id), "it is debris in its orbit, in three pieces");
        final OrbitObject read = OrbitObject.CODEC.parse(NbtOps.INSTANCE, OrbitObject.CODEC.encodeStart(
                NbtOps.INSTANCE, debris).getOrThrow()).getOrThrow();
        helper.assertTrue(read.equals(debris), "kept with the world as it is");
        OrbitObjects.forget(level.getServer(), id);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void rocket_passingIntoOrbitIsToldThereKeptLoadedByItsPersistor(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos at = helper.absolutePos(new BlockPos(1, 2, 1));
        level.setBlockAndUpdate(at, Blocks.IRON_BLOCK.defaultBlockState());
        level.setBlockAndUpdate(at.above(), SpacePersistor.BLOCK.get().defaultBlockState());
        final StructureResult made = StructureAssembly.blocks(level, List.of(at, at.above()), Vec3.atBottomCenterOf(at),
                null);
        helper.assertTrue(made.ok() && made.structure() != null, "the rocket is one body");
        final MovingStructure body = made.structure();
        final UUID builder = UUID.randomUUID();
        Rockets.register(level.getServer(), body.id(), RocketStack.of(new RocketStack.Part(SkeletonRocketParts.id(
                "capsule_3"), 1)), builder);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(body.ready(), "it stands ready"))
                .thenExecute(() -> {
                    OrbitObjects.arrived(level, body, SolarBody.OVERWORLD);
                    final OrbitObject told = OrbitObjects.get(level.getServer(), body.id().value()).orElseThrow();
                    helper.assertTrue(told.kind() == OrbitObject.Kind.ROCKET && told.world().equals(
                            SolarSystemWorlds.worldOf(SolarBody.OVERWORLD)) && told.ownership().owner()
                            .equals(builder), "it is told in the Overworld's orbit, its builder's");
                    helper.assertTrue(told.kept(), "the Space Persistor aboard keeps it loaded");
                    OrbitObjects.forget(level.getServer(), body.id().value());
                    Rockets.forget(level.getServer(), body.id());
                    MovingStructures.takeAway(body);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void spacecraft_kindsAreDeclaredAndOneIsWholeWithEachComponent(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        helper.assertTrue(SatelliteTypes.all(level).size() >= 12 && ProbeTypes.all(level).size() >= 12,
                "the kinds of satellite and of probe are declared");
        helper.assertTrue(SatelliteTypes.get(level, SkeletonSpacecraft.id("mapping")).map(type -> type.orbits()
                .equals(List.of(OrbitKind.POLAR))).orElse(false), "a mapping satellite works best in a polar orbit");
        helper.assertTrue(ProbeTypes.get(level, SkeletonSpacecraft.id("lander")).map(type -> type.lands())
                .orElse(false) && !ProbeTypes.get(level, SkeletonSpacecraft.id("flyby")).orElseThrow().lands(),
                "a lander goes down, a flyby does not");
        SpacecraftBuild build = SpacecraftBuild.of(SpacecraftSize.CUBESAT);
        helper.assertTrue(build.missing().size() == SpacecraftBuild.Slot.values().length && !build.whole(),
                "nothing fitted, everything is missing");
        build = build.with(SpacecraftBuild.Component.BUS).with(SpacecraftBuild.Component.SOLAR_PANELS)
                .with(SpacecraftBuild.Component.BATTERY).with(SpacecraftBuild.Component.ANTENNA)
                .with(SpacecraftBuild.Component.COLD_GAS).with(SpacecraftBuild.Component.COMPUTER);
        helper.assertTrue(build.missing().isEmpty() && !build.whole(), "with no mission module it is not whole");
        build = build.with(SpacecraftBuild.Component.ION).with(new SpacecraftBuild.Mission(false,
                SkeletonSpacecraft.id("communication")));
        helper.assertTrue(build.whole() && build.components().contains(SpacecraftBuild.Component.ION)
                && !build.components().contains(SpacecraftBuild.Component.COLD_GAS),
                "whole, one propulsion in place of the other");
        final SpacecraftBuild read = SpacecraftBuild.CODEC.parse(NbtOps.INSTANCE, SpacecraftBuild.CODEC.encodeStart(
                NbtOps.INSTANCE, build).getOrThrow()).getOrThrow();
        helper.assertTrue(read.equals(build), "kept as it is");
        helper.succeed();
    }
}
