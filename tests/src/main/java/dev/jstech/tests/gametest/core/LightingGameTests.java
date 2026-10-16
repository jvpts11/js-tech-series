/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.light.BeamLights;
import dev.jstech.core.light.Beams;
import dev.jstech.core.light.CoreLights;
import dev.jstech.core.light.LampBlockEntity;
import dev.jstech.core.light.LightBlock;
import dev.jstech.core.light.LightColours;
import dev.jstech.core.light.LightKind;
import dev.jstech.core.light.LightKinds;
import dev.jstech.core.light.LightMath;
import dev.jstech.core.light.LightReadings;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestLights;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The lighting kit in a world: a lamp shines as its control says, by redstone, by power or always, pulsing as its
 * kind says, and goes dark broken until mended; a floodlight's cone and a narrow beam light places far past the
 * game's fifteen blocks with light that counts for what spawns there, and that light goes when the lamp does; light
 * left standing in the air goes by itself; the colours of light are data, and a sensor reads light's level and hue.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class LightingGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos LAMP = new BlockPos(2, 2, 2);
    private static final BlockPos FAR_CORNER = new BlockPos(7, 2, 7);
    /* Long enough for a lamp to hear and turn on; for a beam to be laid; for light left to go by itself. */
    private static final int SETTLE = 5;
    private static final int LAID = 25;
    private static final int GONE = 50;
    private static final double HUE_CLOSE = 3.0;

    private LightingGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void alarmLamp_shinesWhileRedstonePowersItAndPulses(final GameTestHelper helper) {
        helper.setBlock(LAMP, TestLights.ALARM_LAMP.get());
        final Set<Integer> seen = new HashSet<>();
        helper.runAfterDelay(SETTLE, () -> {
            final LampBlockEntity lamp = helper.getBlockEntity(LAMP);
            helper.assertFalse(lamp.lit(), "dark with no redstone");
            helper.assertTrue(emission(helper, LAMP) == 0, "and giving no light");
            helper.setBlock(LAMP.east(), Blocks.REDSTONE_BLOCK);
        });
        helper.onEachTick(() -> {
            if (helper.getBlockEntity(LAMP) instanceof LampBlockEntity lamp && lamp.lit()) {
                seen.add(emission(helper, LAMP));
            }
        });
        helper.runAfterDelay(SETTLE + 60, () -> {
            final LampBlockEntity lamp = helper.getBlockEntity(LAMP);
            helper.assertTrue(lamp.lit(), "on while the redstone block powers it");
            helper.assertTrue(seen.size() >= 3, "its light pulses: " + seen);
            helper.assertTrue(seen.stream().mapToInt(Integer::intValue).max().orElse(0) >= 12,
                    "up to bright: " + seen);
            helper.setBlock(LAMP.east(), Blocks.AIR);
        });
        helper.runAfterDelay(SETTLE + 70, () -> {
            helper.assertFalse(((LampBlockEntity) helper.getBlockEntity(LAMP)).lit(), "dark again without it");
            helper.assertTrue(emission(helper, LAMP) == 0, "and its light gone");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void poweredLamp_shinesOnlyWhileItGetsItsWatts(final GameTestHelper helper) {
        helper.setBlock(LAMP, TestLights.POWERED_LAMP.get());
        helper.runAfterDelay(SETTLE, () -> {
            final LampBlockEntity lamp = helper.getBlockEntity(LAMP);
            helper.assertFalse(lamp.lit(), "dark with no energy");
            lamp.energy().receiveEnergy(lamp.energy().getMaxEnergyStored(), false);
        });
        helper.runAfterDelay(SETTLE * 2, () -> {
            final LampBlockEntity lamp = helper.getBlockEntity(LAMP);
            helper.assertTrue(lamp.lit(), "on with energy");
            helper.assertTrue(emission(helper, LAMP) == 14, "at its kind's level: " + emission(helper, LAMP));
            helper.assertTrue(lamp.energy().getEnergyStored() < lamp.energy().getMaxEnergyStored(),
                    "drawing its watts");
            lamp.energy().setEnergyStored(0);
        });
        helper.runAfterDelay(SETTLE * 3, () -> {
            helper.assertFalse(((LampBlockEntity) helper.getBlockEntity(LAMP)).lit(), "dark once its energy is gone");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 40)
    public static void fragileLamp_goesDarkBrokenUntilMended(final GameTestHelper helper) {
        helper.setBlock(LAMP, TestLights.FRAGILE_LAMP.get());
        helper.runAfterDelay(SETTLE, () -> {
            final LampBlockEntity lamp = helper.getBlockEntity(LAMP);
            helper.assertTrue(lamp.lit() && emission(helper, LAMP) == 12, "on, at its level");
            lamp.breakLamp();
            helper.assertTrue(lamp.broken(), "broken");
        });
        helper.runAfterDelay(SETTLE * 2, () -> {
            final LampBlockEntity lamp = helper.getBlockEntity(LAMP);
            helper.assertFalse(lamp.lit(), "dark while broken");
            helper.assertTrue(emission(helper, LAMP) == 0, "giving no light");
            lamp.mend();
        });
        helper.runAfterDelay(SETTLE * 3, () -> {
            helper.assertTrue(((LampBlockEntity) helper.getBlockEntity(LAMP)).lit(), "on again mended");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 140)
    public static void floodlight_lightsFarPastFifteenAndItsLightGoesWithIt(final GameTestHelper helper) {
        final int up = 22;
        far(helper, TestLights.FLOODLIGHT.get().defaultBlockState(), up);
    }

    @GameTest(template = ARENA, timeoutTicks = 140)
    public static void beamLamp_lightsWhereItsNarrowBeamMeetsSomething(final GameTestHelper helper) {
        final int up = 34;
        far(helper, TestLights.BEAM_LAMP.get().defaultBlockState(), up);
    }

    @GameTest(template = ARENA, timeoutTicks = 80)
    public static void lightBlock_goesByItselfWhereNoLightHoldsIt(final GameTestHelper helper) {
        final BlockPos dry = new BlockPos(1, 3, 1);
        final BlockPos wet = new BlockPos(3, 3, 1);
        final LightBlock light = CoreLights.LIGHT.get();
        helper.setBlock(dry, light.at(10, false));
        helper.setBlock(wet, light.at(10, true));
        helper.runAfterDelay(SETTLE, () -> helper.assertTrue(helper.getLevel().getBrightness(LightLayer.BLOCK,
                helper.absolutePos(dry)) == 10, "it lights the world at its level"));
        helper.runAfterDelay(GONE, () -> {
            helper.assertBlockPresent(Blocks.AIR, dry);
            helper.assertBlockPresent(Blocks.WATER, wet);
            helper.setBlock(wet, Blocks.AIR);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void colours_areDataForBlocksItemsAndMobs(final GameTestHelper helper) {
        helper.assertTrue(LightColours.of(Blocks.SOUL_LANTERN.defaultBlockState()) == 0x50C8FF,
                "a soul lantern's light is blue");
        helper.assertTrue(LightColours.of(Blocks.TORCH.defaultBlockState()) == LightMath.WHITE,
                "a torch's warm white tints nothing");
        final LightColours.Glow torch = LightColours.held(new ItemStack(Items.TORCH));
        helper.assertTrue(torch != null && torch.level() == 14, "a torch held gives its own light: " + torch);
        final LightColours.Glow lava = LightColours.held(new ItemStack(Items.LAVA_BUCKET));
        helper.assertTrue(lava != null && lava.level() == 15 && lava.colour() == 0xFF8030,
                "a bucket of lava, by the map: " + lava);
        helper.assertTrue(LightColours.held(new ItemStack(Items.STICK)) == null, "a stick gives none");
        final Blaze blaze = helper.spawn(EntityType.BLAZE, new BlockPos(2, 2, 2));
        final LightColours.Glow glow = LightColours.of(blaze);
        blaze.discard();
        helper.assertTrue(glow != null && glow.level() == 10, "a blaze glows: " + glow);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void kinds_areReadFromTheModsFiles(final GameTestHelper helper) {
        final LightKind file = LightKinds.FILES.entries(helper.getLevel()).get(TestLights.FLOOD);
        helper.assertTrue(file != null, "the floodlight's kind is read from the test mod's data");
        helper.assertTrue(file.equals(LightKinds.declared().get(TestLights.FLOOD)), "as declared: " + file);
        helper.assertTrue(LightKinds.get(helper.getLevel(), TestLights.FLOOD).reach() == TestLights.FLOOD_REACH,
                "reaching as far as declared");
        helper.succeed();
    }

    /*
     * In a batch of its own, so no lamp of another test is near enough to mix its colour in; at the far corner of the
     * arena from the beacon the test framework stands under it, whose cyan light would mix in too.
     */
    @GameTest(template = ARENA, timeoutTicks = 40, batch = "lighting_readings")
    public static void readings_giveTheLightsLevelAndHue(final GameTestHelper helper) {
        helper.setBlock(FAR_CORNER, TestLights.FRAGILE_LAMP.get());
        helper.runAfterDelay(SETTLE, () -> {
            final BlockPos near = helper.absolutePos(FAR_CORNER.above().above());
            final ServerLevel level = helper.getLevel();
            // The brighter of block and sky light: the lamp's ten two blocks off, or the open sky's at this hour.
            final double intensity = LightReadings.intensity(level, near).value();
            final int sky = level.getBrightness(LightLayer.SKY, near) - level.getSkyDarken();
            helper.assertTrue(intensity == Math.max(10, sky),
                    "two blocks from a lamp of 12, under a sky of " + sky + ": " + intensity);
            helper.assertTrue(level.getBrightness(LightLayer.BLOCK, near) == 10, "its own light there");
            final double hue = LightReadings.hue(level, near).value();
            final double green = LightMath.hue(TestLights.FRAGILE_GREEN);
            helper.assertTrue(Math.abs(hue - green) < HUE_CLOSE, "the lamp's green: " + hue + " against " + green);
            helper.succeed();
        });
    }

    /*
     * A lamp at the floor facing up, a stone {@code up} blocks above it: the place below the stone is lit, by light
     * that counts for what spawns there, while the lamp shines; and with the lamp gone the light goes by itself.
     */
    private static void far(final GameTestHelper helper, final BlockState lamp, final int up) {
        final BlockPos ceiling = LAMP.above(up);
        final BlockPos lit = ceiling.below();
        helper.setBlock(ceiling, Blocks.STONE);
        helper.setBlock(LAMP, lamp.setValue(DirectionalBlock.FACING, Direction.UP));
        helper.runAfterDelay(LAID, () -> {
            final ServerLevel level = helper.getLevel();
            final BlockPos at = helper.absolutePos(lit);
            final LampBlockEntity on = helper.getBlockEntity(LAMP);
            helper.assertTrue(level.getBlockState(at).getBlock() instanceof LightBlock,
                    "light stands under the stone, " + up + " blocks up: " + level.getBlockState(at) + ", the lamp "
                            + (on.lit() ? "on" : "off") + " facing " + on.facing() + ", lighting "
                            + Beams.lamp(level, helper.absolutePos(LAMP), on.facing(), on.kind(), 15).keySet()
                            + " for " + at);
            helper.assertTrue(BeamLights.holds(level, at), "held by the lamp's light");
            final int brightness = level.getBrightness(LightLayer.BLOCK, at);
            helper.assertTrue(brightness > 0, "lit, past fifteen blocks: " + brightness);
            helper.assertTrue(brightness > level.dimensionType().monsterSpawnBlockLightLimit(),
                    "by light too bright for a monster to spawn in");
            helper.setBlock(LAMP, Blocks.AIR);
        });
        helper.runAfterDelay(LAID + GONE, () -> {
            helper.assertFalse(helper.getLevel().getBlockState(helper.absolutePos(lit)).getBlock()
                    instanceof LightBlock, "the light went with the lamp");
            helper.setBlock(ceiling, Blocks.AIR);
            helper.succeed();
        });
    }

    /* The light the block at {@code pos} gives. */
    private static int emission(final GameTestHelper helper, final BlockPos pos) {
        final BlockPos at = helper.absolutePos(pos);
        return helper.getLevel().getBlockState(at).getLightEmission(helper.getLevel(), at);
    }
}
