/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.projectile.Projectiles;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestClaims;
import dev.jstech.tests.TestEntities;
import dev.jstech.tests.TestFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * What claimed land keeps out of the Core's own work, by the claims of the test mod: a fluid of the Core poured
 * beside a claim flows everywhere but into it, and something shot by nobody into a claim is spent there, harming
 * nothing. A structure crashing into one is the physics tests'.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ClaimedLandGameTests {

    private static final String ARENA = "empty";

    private ClaimedLandGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void claim_keepsOutAFluidOfTheCoreFlowingFromBesideIt(final GameTestHelper helper) {
        // A channel along x, closed at its west end and walled on both sides, so the oil goes nowhere else.
        for (int x = 0; x <= 7; x++) {
            helper.setBlock(new BlockPos(x, 1, 2), Blocks.STONE);
            helper.setBlock(new BlockPos(x, 2, 1), Blocks.STONE);
            helper.setBlock(new BlockPos(x, 2, 3), Blocks.STONE);
        }
        helper.setBlock(new BlockPos(0, 2, 2), Blocks.STONE);
        // The claim is the channel from x 4 on; the oil is poured at x 1, three blocks short of it.
        final AABB claim = new AABB(helper.absoluteVec(new Vec3(4, 2, 2)), helper.absoluteVec(new Vec3(8, 3, 3)));
        TestClaims.claim(claim);
        final BlockState oil = TestFluids.COOL_OIL.source().defaultFluidState().createLegacyBlock();
        helper.assertTrue(!oil.isAir(), "the oil has a block to pour");
        helper.setBlock(new BlockPos(1, 2, 2), oil);
        helper.runAfterDelay(190, () -> TestClaims.unclaim(claim));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(!helper.getBlockState(new BlockPos(3, 2, 2)).getFluidState()
                        .isEmpty(), "the oil flows up to the claim"))
                .thenIdle(30)
                .thenExecute(() -> {
                    helper.assertTrue(helper.getBlockState(new BlockPos(4, 2, 2)).getFluidState().isEmpty(),
                            "and never into it");
                    for (int x = 1; x <= 3; x++) {
                        helper.setBlock(new BlockPos(x, 2, 2), Blocks.AIR);
                    }
                    TestClaims.unclaim(claim);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void claim_spendsWhatNobodyShootsIntoIt(final GameTestHelper helper) {
        final Pig inside = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(2, 2, 6));
        final Pig outside = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(6, 2, 6));
        final float health = inside.getHealth();
        final AABB claim = new AABB(helper.absoluteVec(new Vec3(1, 1, 5)), helper.absoluteVec(new Vec3(4, 5, 8)));
        TestClaims.claim(claim);
        final TestEntities.Slug atInside = Projectiles.fire(helper.getLevel(), null, TestEntities.SLUG.get(),
                helper.absoluteVec(new Vec3(2.5, 2.6, 1.5)), new Vec3(0, 0, 1), 1.5F);
        final TestEntities.Slug atOutside = Projectiles.fire(helper.getLevel(), null, TestEntities.SLUG.get(),
                helper.absoluteVec(new Vec3(6.5, 2.6, 1.5)), new Vec3(0, 0, 1), 1.5F);
        helper.runAfterDelay(90, () -> TestClaims.unclaim(claim));
        helper.succeedWhen(() -> {
            helper.assertTrue(outside.getHealth() < health, "a shot by nobody hits outside the claim");
            helper.assertTrue(atInside.isRemoved() && inside.getHealth() >= health,
                    "and is spent at the claim, the pig inside unharmed");
            TestClaims.unclaim(claim);
            inside.discard();
            outside.discard();
            atOutside.discard();
        });
    }
}
