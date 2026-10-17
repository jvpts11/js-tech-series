/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.explosion.BlastBalance;
import dev.jstech.core.explosion.BlastConsequence;
import dev.jstech.core.explosion.BlastMath;
import dev.jstech.core.explosion.BlastMedium;
import dev.jstech.core.explosion.Detonation;
import dev.jstech.core.explosion.Explosions;
import dev.jstech.core.jstechseries.SeriesMaterials;
import dev.jstech.core.protection.Actor;
import dev.jstech.core.protection.IProtectionAdapter;
import dev.jstech.core.protection.ProtectedAction;
import dev.jstech.core.protection.Protection;
import dev.jstech.tests.JsTests;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Core's blasts in a real world: blocks give by their strength and walls shield, the wave arrives in its own
 * time, bodies take the overpressure by distance, a vacuum carries no wave, explosives go off, claims and the
 * server's setting hold, the breaking spreads over ticks, fragments strike, and the crater is scorched, lit and its
 * debris thrown. Each runs in a batch of its own, so no blast reaches another test.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ExplosionGameTests {

    private static final String ARENA = "empty";
    private static final ResourceLocation CHARGE = ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
            "test_charge");
    /* The arena's floor is its layer at y 1; the middle is half a block above it. */
    private static final Vec3 MIDDLE = new Vec3(4.5, 2.5, 4.5);
    /* The places the fence below keeps from blasts, absolute. */
    private static final Set<BlockPos> FENCED = ConcurrentHashMap.newKeySet();
    private static final String FENCE = "jstests:blast_fence";

    private ExplosionGameTests() {
    }

    @GameTest(template = ARENA, batch = "jscore_explosion_strength")
    public static void strength_glassGivesAndConcreteHolds(final GameTestHelper helper) {
        final BlockPos glass = new BlockPos(7, 2, 4);
        final BlockPos concrete = new BlockPos(1, 2, 4);
        helper.setBlock(glass, Blocks.GLASS);
        helper.setBlock(concrete, Blocks.WHITE_CONCRETE);
        final Detonation blast = detonate(helper, MIDDLE, 0.1);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(blast.finished(), "the blast went all the way out"))
                .thenExecute(() -> {
                    helper.assertBlockNotPresent(Blocks.GLASS, glass);
                    helper.assertBlockPresent(Blocks.WHITE_CONCRETE, concrete);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = "jscore_explosion_wall")
    public static void wall_shieldsWhatIsBehindIt(final GameTestHelper helper) {
        wall(helper, 6);
        final BlockPos behind = new BlockPos(8, 2, 4);
        final BlockPos open = new BlockPos(1, 2, 4);
        helper.setBlock(behind, Blocks.GLASS);
        helper.setBlock(open, Blocks.GLASS);
        final Detonation blast = detonate(helper, MIDDLE, 0.1);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(blast.finished(), "the blast went all the way out"))
                .thenExecute(() -> {
                    helper.assertBlockPresent(Blocks.GLASS, behind);
                    helper.assertBlockNotPresent(Blocks.GLASS, open);
                    helper.assertBlockPresent(Blocks.OBSIDIAN, new BlockPos(6, 2, 4));
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = "jscore_explosion_pace")
    public static void wave_reachesEachPlaceInItsOwnTime(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlastMedium here = Explosions.mediumAt(level, helper.absoluteVec(MIDDLE));
        helper.assertTrue(here.speed() > 320 && here.speed() < 360, "the air carries it at the speed of sound");
        // The near glass begins 2.5 blocks away, the far one 4.3 blocks away, in the arena's corner.
        final BlockPos near = new BlockPos(7, 2, 4);
        final BlockPos far = new BlockPos(0, 2, 1);
        helper.setBlock(near, Blocks.GLASS);
        helper.setBlock(far, Blocks.GLASS);
        BlastBalance.setLookedAt(1_048_576);
        // Slowed to a block a tick, as laws of a place may slow it, for the arrival to be seen in an arena.
        final Detonation blast = Explosions.blast(level, helper.absoluteVec(MIDDLE), CHARGE, joules(0.1))
                .through(here.paced(20)).detonate().orElseThrow();
        helper.assertTrue(blast.front() == 0 && blast.brokenBlocks() == 0, "nothing is reached as it goes off");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertBlockNotPresent(Blocks.GLASS, near))
                .thenExecute(() -> {
                    helper.assertTrue(blast.front() < 4.3, "the wave is not yet at the far glass");
                    helper.assertBlockPresent(Blocks.GLASS, far);
                })
                .thenWaitUntil(() -> helper.assertBlockNotPresent(Blocks.GLASS, far))
                .thenExecute(() -> {
                    BlastBalance.setLookedAt(BlastBalance.DEFAULT_LOOKED_AT);
                    helper.assertTrue(blast.front() >= 4.3, "it broke when the wave got there");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = "jscore_explosion_bodies")
    public static void bodies_takeTheOverpressureByDistanceAndWallsShield(final GameTestHelper helper) {
        wallAcross(helper, 6);
        // At the height of the cows' middle, two and three blocks off, one of them behind the wall.
        final Vec3 centre = new Vec3(4.5, 2.7, 4.5);
        final Cow near = helper.spawnWithNoFreeWill(EntityType.COW, new Vec3(6.5, 2, 4.5));
        final Cow far = helper.spawnWithNoFreeWill(EntityType.COW, new Vec3(1.5, 2, 4.5));
        final Cow sheltered = helper.spawnWithNoFreeWill(EntityType.COW, new Vec3(4.5, 2, 7.5));
        final double nearStart = near.getX();
        final Detonation blast = detonate(helper, centre, 0.08);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(blast.finished(), "the blast went all the way out"))
                .thenExecuteAfter(5, () -> {
                    helper.assertTrue(near.getHealth() < far.getHealth(), "the nearer is hurt the more");
                    helper.assertTrue(far.getHealth() < far.getMaxHealth(), "the farther is hurt too");
                    helper.assertTrue(sheltered.getHealth() == sheltered.getMaxHealth(),
                            "the one behind the wall is not");
                    helper.assertTrue(near.getX() > nearStart, "the near one is thrown away from the blast");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = "jscore_explosion_vacuum")
    public static void vacuum_carriesNoWaveButTheFireballBurns(final GameTestHelper helper) {
        final BlockPos glass = new BlockPos(6, 2, 4);
        helper.setBlock(glass, Blocks.GLASS);
        final Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new Vec3(3.5, 2, 4.5));
        final Detonation blast = Explosions.blast(helper.getLevel(), helper.absoluteVec(MIDDLE), CHARGE, joules(1))
                .through(BlastMedium.VACUUM).detonate().orElseThrow();
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(blast.finished(), "with no wave it is done at once"))
                .thenExecute(() -> {
                    helper.assertBlockPresent(Blocks.GLASS, glass);
                    helper.assertTrue(blast.brokenBlocks() == 0 && blast.front() == 0, "no wave went out");
                    helper.assertTrue(cow.getRemainingFireTicks() > 0, "the fireball set the cow alight");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void medium_isWaterWhereItGoesOffInWater(final GameTestHelper helper) {
        final BlockPos pool = new BlockPos(4, 2, 4);
        helper.setBlock(pool, Blocks.WATER);
        final BlastMedium water = Explosions.mediumAt(helper.getLevel(), helper.absoluteVec(MIDDLE));
        helper.assertTrue(water == BlastMedium.WATER, "a blast in water goes out through the water");
        final BlastMedium air = Explosions.mediumAt(helper.getLevel(), helper.absoluteVec(new Vec3(4.5, 4.5, 4.5)));
        helper.assertTrue(water.reach(1, BlastMath.HARMLESS) > 2 * air.reach(1, BlastMath.HARMLESS),
                "and carries far farther than the air");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "jscore_explosion_chain")
    public static void chain_setsOffTheGamesTnt(final GameTestHelper helper) {
        final BlockPos tnt = new BlockPos(6, 2, 4);
        helper.setBlock(tnt, Blocks.TNT);
        detonate(helper, MIDDLE, 0.5);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertEntityPresent(EntityType.TNT))
                .thenExecute(() -> {
                    helper.killAllEntitiesOfClass(PrimedTnt.class);
                    helper.killAllEntitiesOfClass(FallingBlockEntity.class);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = "jscore_explosion_claims")
    public static void claims_keepWhatTheyProtect(final GameTestHelper helper) {
        if (!Protection.names().contains(FENCE)) {
            Protection.register(FENCE, new BlastFence());
        }
        final BlockPos fenced = new BlockPos(7, 2, 4);
        final BlockPos open = new BlockPos(1, 2, 4);
        helper.setBlock(fenced, Blocks.GLASS);
        helper.setBlock(open, Blocks.GLASS);
        FENCED.add(helper.absolutePos(fenced));
        final Detonation blast = detonate(helper, MIDDLE, 0.1);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(blast.finished(), "the blast went all the way out"))
                .thenExecute(() -> {
                    FENCED.remove(helper.absolutePos(fenced));
                    helper.assertBlockPresent(Blocks.GLASS, fenced);
                    helper.assertBlockNotPresent(Blocks.GLASS, open);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = "jscore_explosion_setting")
    public static void consequence_isTheServersSettingForTheMod(final GameTestHelper helper) {
        BlastBalance.setConsequences(Map.of(JsTests.MODID, BlastConsequence.SPARE_BLOCKS));
        final BlockPos glass = new BlockPos(7, 2, 4);
        helper.setBlock(glass, Blocks.GLASS);
        final Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new Vec3(2.5, 2, 4.5));
        final Detonation blast = detonate(helper, MIDDLE, 0.05);
        BlastBalance.setConsequences(Map.of());
        helper.assertTrue(blast.consequence() == BlastConsequence.SPARE_BLOCKS, "the mod's setting holds");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(blast.finished(), "the blast went all the way out"))
                .thenExecute(() -> {
                    helper.assertBlockPresent(Blocks.GLASS, glass);
                    helper.assertTrue(cow.getHealth() < cow.getMaxHealth(), "it still hurts what it reaches");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = "jscore_explosion_harmless")
    public static void consequence_aModsOwnCapHolds(final GameTestHelper helper) {
        final BlockPos glass = new BlockPos(7, 2, 4);
        helper.setBlock(glass, Blocks.GLASS);
        final Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new Vec3(2.5, 2, 4.5));
        final Detonation blast = Explosions.blast(helper.getLevel(), helper.absoluteVec(MIDDLE), CHARGE, joules(0.05))
                .atMost(BlastConsequence.HARMLESS).detonate().orElseThrow();
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(blast.finished(), "the blast went all the way out"))
                .thenExecute(() -> {
                    helper.assertBlockPresent(Blocks.GLASS, glass);
                    helper.assertTrue(cow.getHealth() == cow.getMaxHealth(), "a harmless blast hurts nothing");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = "jscore_explosion_budget")
    public static void budget_spreadsTheBreakingOverTicks(final GameTestHelper helper) {
        for (int y = 2; y <= 5; y++) {
            for (int z = 2; z <= 6; z++) {
                helper.setBlock(new BlockPos(7, y, z), Blocks.GLASS);
            }
        }
        // Every line reaches the glass in the first tick; only the breaking is held back.
        BlastBalance.setLookedAt(1_048_576);
        BlastBalance.setBroken(16);
        final Detonation blast = detonate(helper, MIDDLE, 0.1);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(blast.brokenBlocks() > 0, "it began breaking"))
                .thenExecute(() -> helper.assertTrue(blast.brokenBlocks() <= 16 && blast.waiting() > 0,
                        "a tick breaks no more than the budget, the rest waiting"))
                .thenWaitUntil(() -> helper.assertTrue(blast.finished(), "the rest broke over the next ticks"))
                .thenExecute(() -> {
                    BlastBalance.setLookedAt(BlastBalance.DEFAULT_LOOKED_AT);
                    BlastBalance.setBroken(BlastBalance.DEFAULT_BROKEN);
                    helper.assertTrue(blast.brokenBlocks() >= 20, "all the glass broke in the end");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = "jscore_explosion_fragments")
    public static void fragments_strikeWithShrapnel(final GameTestHelper helper) {
        final Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, new Vec3(5.5, 2, 4.5));
        final Detonation blast = Explosions.blast(helper.getLevel(), helper.absoluteVec(new Vec3(4.5, 2.7, 4.5)),
                CHARGE, joules(0.5)).casing(SeriesMaterials.IRON, 1).detonate().orElseThrow();
        helper.assertTrue(blast.struckByFragments() > 0, "fragments of the casing struck the cow");
        helper.assertTrue(cow.getLastDamageSource() != null && cow.getLastDamageSource().is(Explosions.SHRAPNEL),
                "as shrapnel, before the wave");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(blast.finished(), "the blast went all the way out"))
                .thenExecute(() -> helper.killAllEntitiesOfClass(FallingBlockEntity.class))
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = "jscore_explosion_crater", timeoutTicks = 200)
    public static void crater_isScorchedLitAndItsDebrisThrown(final GameTestHelper helper) {
        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 9; z++) {
                helper.setBlock(new BlockPos(x, 2, z), Blocks.STONE);
                helper.setBlock(new BlockPos(x, 3, z), Blocks.GRASS_BLOCK);
            }
        }
        final Detonation blast = Explosions.blast(helper.getLevel(), helper.absoluteVec(new Vec3(4.5, 5.0, 4.5)),
                CHARGE, joules(1)).incendiary().detonate().orElseThrow();
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(blast.finished(), "the blast went all the way out"))
                .thenExecute(() -> {
                    helper.assertTrue(blast.thrownDebris() > 0, "earth was thrown as debris");
                    helper.assertFalse(where(helper, state -> state.is(Blocks.COARSE_DIRT)).isEmpty(),
                            "the ground round the crater is scorched");
                    final List<BlockPos> burning = where(helper, ExplosionGameTests::fire);
                    helper.assertFalse(burning.isEmpty(), "the fireball lit the crater");
                    helper.killAllEntitiesOfClass(FallingBlockEntity.class);
                    burning.forEach(pos -> helper.setBlock(pos, Blocks.AIR));
                })
                .thenSucceed();
    }

    /* Sets off {@code tnt} kilograms of TNT at the arena's {@code at}. */
    private static Detonation detonate(final GameTestHelper helper, final Vec3 at, final double tnt) {
        return Explosions.blast(helper.getLevel(), helper.absoluteVec(at), CHARGE, joules(tnt)).detonate()
                .orElseThrow();
    }

    private static double joules(final double tnt) {
        return tnt * BlastMath.JOULES_PER_KILOGRAM_OF_TNT;
    }

    /* A wall of obsidian across x = {@code x}, three blocks wide and high round the middle, on the floor. */
    private static void wall(final GameTestHelper helper, final int x) {
        for (int y = 2; y <= 4; y++) {
            for (int z = 3; z <= 5; z++) {
                helper.setBlock(new BlockPos(x, y, z), Blocks.OBSIDIAN);
            }
        }
    }

    /* A wall of obsidian across z = {@code z}, three blocks wide and high round the middle, on the floor. */
    private static void wallAcross(final GameTestHelper helper, final int z) {
        for (int y = 2; y <= 4; y++) {
            for (int x = 3; x <= 5; x++) {
                helper.setBlock(new BlockPos(x, y, z), Blocks.OBSIDIAN);
            }
        }
    }

    /* The places of the arena above its floor holding what {@code holds} says. */
    private static List<BlockPos> where(final GameTestHelper helper, final Predicate<BlockState> holds) {
        final List<BlockPos> found = new ArrayList<>();
        for (final BlockPos pos : BlockPos.betweenClosed(0, 2, 0, 8, 6, 8)) {
            if (holds.test(helper.getBlockState(pos))) {
                found.add(pos.immutable());
            }
        }
        return found;
    }

    private static boolean fire(final BlockState state) {
        return state.getBlock() instanceof BaseFireBlock;
    }

    /* Keeps the places in {@link #FENCED} from blasts, as a claim would. */
    private static final class BlastFence implements IProtectionAdapter {

        @Override
        public Verdict ask(final Actor actor, final ProtectedAction action, final ServerLevel level,
                           final BlockPos pos) {
            return action == ProtectedAction.EXPLODE && FENCED.contains(pos.immutable()) ? Verdict.DENY
                    : Verdict.ABSTAIN;
        }
    }
}
