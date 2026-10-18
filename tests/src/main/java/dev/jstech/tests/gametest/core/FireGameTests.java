/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.air.AirMix;
import dev.jstech.core.dimension.Wind;
import dev.jstech.core.dimension.Winds;
import dev.jstech.core.energy.EnergyDamage;
import dev.jstech.core.explosion.Explosions;
import dev.jstech.core.fire.ExtinguishingAgent;
import dev.jstech.core.fire.Extinguishing;
import dev.jstech.core.fire.FireBalance;
import dev.jstech.core.fire.FireClass;
import dev.jstech.core.fire.FireClasses;
import dev.jstech.core.fire.Fires;
import dev.jstech.core.hazard.Pollution;
import dev.jstech.core.jstechseries.fire.CharredBlocks;
import dev.jstech.core.jstechseries.fire.ExtinguisherItem;
import dev.jstech.core.jstechseries.fire.FireDetectorBlock;
import dev.jstech.core.jstechseries.fire.FireSafety;
import dev.jstech.core.jstechseries.fire.SprinklerBlock;
import dev.jstech.core.jstechseries.fire.SprinklerBlockEntity;
import dev.jstech.core.jstechseries.hazard.SeriesPollution;
import dev.jstech.core.room.GasMath;
import dev.jstech.core.room.Room;
import dev.jstech.core.room.SealedRooms;
import dev.jstech.core.signal.Signals;
import dev.jstech.tests.JsTests;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Core's fire in a real world: fires known as they light and go out, what burnt leaving its remains, the oxygen
 * feeding a fire and its lack starving it, the wind carrying it ahead, the classes of fire and what each agent does
 * on them, an extinguisher spraying and filling again, a sprinkler showering a fire out with the water of its pipe,
 * the detectors calling and speaking redstone, and the soot a fire gives off. The arena's floor is its layer at y 1.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class FireGameTests {

    private static final String ARENA = "empty";
    private static final double ATMOSPHERE = 101.3;
    /* The places a strong east wind blows in, for the wind's test; absolute. */
    private static final Set<AABB> WINDY = ConcurrentHashMap.newKeySet();
    private static final Wind GALE = new Wind(40, 0);
    private static volatile boolean windDeclared;

    private FireGameTests() {
    }

    @GameTest(template = ARENA)
    public static void fires_areKnownAsTheyLightAndGoOut(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        helper.setBlock(new BlockPos(4, 2, 4), Blocks.NETHERRACK);
        final int before = Fires.burning(level);
        helper.setBlock(new BlockPos(4, 3, 4), Blocks.FIRE);
        helper.assertTrue(Fires.burning(level) == before + 1, "a fire lit is known at once");
        helper.setBlock(new BlockPos(4, 3, 4), Blocks.AIR);
        helper.assertTrue(Fires.burning(level) == before, "and forgotten as it goes out");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "jscore_fire_remains", timeoutTicks = 1200)
    public static void fire_spreadsByTheAirAndLeavesItsRemains(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        level.setWeatherParameters(6_000, 0, false, false);
        final boolean fireTicked = level.getGameRules().getBoolean(GameRules.RULE_DOFIRETICK);
        level.getGameRules().getRule(GameRules.RULE_DOFIRETICK).set(true, level.getServer());
        final BlockPos wool = new BlockPos(4, 2, 4);
        helper.setBlock(wool, Blocks.WHITE_WOOL);
        helper.setBlock(wool.above(), Blocks.FIRE);
        final double rate = Fires.burnRate(level, helper.absolutePos(wool.above()));
        helper.assertTrue(rate > 0.5 && rate < 1.5, "the overworld's air burns as the game always did: " + rate);
        final BlockState log = Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
        final BlockState charred = Fires.remainsOf(log);
        helper.assertTrue(charred != null && charred.is(CharredBlocks.CHARRED_LOG.get())
                && charred.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.X, "a log chars along its own axis");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertBlockPresent(CharredBlocks.ASH.get(), wool))
                // The game's tests run with fire still; the rule goes back as it was.
                .thenExecute(() -> level.getGameRules().getRule(GameRules.RULE_DOFIRETICK).set(fireTicked,
                        level.getServer()))
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void remains_areLeftWhereTheFireGoesOut(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos burnt = new BlockPos(3, 2, 3);
        final BlockPos alight = new BlockPos(5, 2, 5);
        final BlockState log = Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
        Fires.burned(level, helper.absolutePos(burnt), log, false);
        helper.setBlock(alight, Blocks.FIRE);
        Fires.burned(level, helper.absolutePos(alight), Blocks.OAK_PLANKS.defaultBlockState(), true);
        helper.startSequence()
                .thenExecuteAfter(1, () -> {
                    helper.assertBlockPresent(CharredBlocks.CHARRED_LOG.get(), burnt);
                    helper.assertTrue(helper.getBlockState(burnt).getValue(RotatedPillarBlock.AXIS)
                            == Direction.Axis.Z, "it keeps the log's axis");
                    helper.assertBlockPresent(Blocks.FIRE, alight);
                    helper.setBlock(alight, Blocks.AIR);
                })
                .thenExecuteAfter(1, () -> {
                    helper.assertBlockPresent(CharredBlocks.CHARRED_PLANKS.get(), alight);
                    FireBalance.setRemains(false);
                    helper.setBlock(burnt, Blocks.AIR);
                    Fires.burned(level, helper.absolutePos(burnt), log, false);
                })
                .thenExecuteAfter(1, () -> {
                    FireBalance.setRemains(true);
                    helper.assertBlockNotPresent(CharredBlocks.CHARRED_LOG.get(), burnt);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = "jscore_fire_oxygen", timeoutTicks = 200)
    public static void oxygen_feedsTheFireAndItsLackStarvesIt(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos inside = new BlockPos(3, 3, 3);
        hut(helper, new BlockPos(2, 2, 2), new BlockPos(5, 5, 5));
        SealedRooms.track(level, helper.absolutePos(inside));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(room(helper, inside).isPresent(), "the room is found"))
                .thenExecute(() -> {
                    fill(room(helper, inside).orElseThrow(), 34, Map.of(AirMix.OXYGEN, 1.0));
                    helper.assertTrue(Fires.burnRate(level, helper.absolutePos(inside)) > 3,
                            "pure oxygen makes an inferno of any fire");
                    fill(room(helper, inside).orElseThrow(), ATMOSPHERE, Map.of(AirMix.NITROGEN, 1.0));
                })
                // The air at a fire is read once a tick.
                .thenExecuteAfter(1, () -> {
                    helper.assertTrue(Fires.burnRate(level, helper.absolutePos(inside)) == 0,
                            "with no oxygen nothing burns");
                    SealedRooms.untrack(level, helper.absolutePos(inside));
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = "jscore_fire_wind", timeoutTicks = 300)
    public static void wind_carriesTheFireAhead(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        level.setWeatherParameters(6_000, 0, false, false);
        final boolean fireTicked = level.getGameRules().getBoolean(GameRules.RULE_DOFIRETICK);
        level.getGameRules().getRule(GameRules.RULE_DOFIRETICK).set(true, level.getServer());
        declareWind();
        final AABB arena = new AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(9, 9, 9);
        WINDY.add(arena);
        final BlockPos fire = new BlockPos(2, 3, 4);
        final BlockPos downwind = new BlockPos(5, 3, 4);
        final BlockPos aside = new BlockPos(2, 3, 7);
        helper.setBlock(fire.below(), Blocks.NETHERRACK);
        helper.setBlock(downwind.below(), Blocks.WHITE_WOOL);
        helper.setBlock(aside.below(), Blocks.WHITE_WOOL);
        helper.setBlock(fire, Blocks.FIRE);
        final BlockPos at = helper.absolutePos(fire);
        helper.assertTrue(Fires.spread(level, at, at.east()) > Fires.spread(level, at, at.west()),
                "the fire spreads faster downwind than upwind");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertBlockPresent(Blocks.FIRE, downwind))
                .thenExecute(() -> {
                    WINDY.remove(arena);
                    level.getGameRules().getRule(GameRules.RULE_DOFIRETICK).set(fireTicked, level.getServer());
                    helper.assertBlockNotPresent(Blocks.FIRE, aside);
                    for (final BlockPos pos : BlockPos.betweenClosed(0, 2, 0, 8, 6, 8)) {
                        if (helper.getBlockState(pos).is(Blocks.FIRE)) {
                            helper.setBlock(pos, Blocks.AIR);
                        }
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void classes_areByTheWorstOfWhatFeedsTheFire(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos air = helper.absolutePos(new BlockPos(1, 6, 1));
        helper.assertTrue(classOf(level, air, Blocks.STONE) == FireClass.SOLIDS, "stone and wood feed solids");
        helper.assertTrue(classOf(level, air, Blocks.SPONGE) == FireClass.LIQUIDS, "a mapped fuel is its class");
        helper.assertTrue(classOf(level, air, Blocks.WET_SPONGE) == FireClass.COOKING_OILS, "cooking oils");
        helper.assertTrue(classOf(level, air, Blocks.LIGHTNING_ROD) == FireClass.ELECTRICAL, "electrical");
        helper.assertTrue(classOf(level, air, Blocks.RAW_IRON_BLOCK) == FireClass.METALS, "metals");
        final BlockPos fire = new BlockPos(4, 3, 4);
        helper.setBlock(fire.below(), Blocks.NETHERRACK);
        helper.setBlock(fire, Blocks.FIRE);
        helper.setBlock(fire.east(), Blocks.SPONGE);
        helper.setBlock(fire.west(), Blocks.LIGHTNING_ROD);
        helper.assertTrue(FireClasses.at(level, helper.absolutePos(fire)) == FireClass.ELECTRICAL,
                "a fire fed by a liquid and live equipment is electrical");
        helper.setBlock(fire, Blocks.AIR);
        helper.assertTrue(ExtinguishingAgent.WATER.on(FireClass.SOLIDS) == ExtinguishingAgent.Outcome.PUTS_OUT
                && ExtinguishingAgent.WATER.on(FireClass.ELECTRICAL) == ExtinguishingAgent.Outcome.SHOCKS
                && ExtinguishingAgent.FOAM.on(FireClass.COOKING_OILS) == ExtinguishingAgent.Outcome.PUTS_OUT
                && ExtinguishingAgent.CARBON_DIOXIDE.on(FireClass.SOLIDS) == ExtinguishingAgent.Outcome.REKINDLES
                && ExtinguishingAgent.DRY_POWDER.on(FireClass.METALS) == ExtinguishingAgent.Outcome.PUTS_OUT,
                "each agent does on each class what the real one does");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "jscore_fire_agents")
    public static void agents_putOutShockAndFlare(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final Player sprayer = helper.makeMockPlayer(GameType.SURVIVAL);
        final BlockPos solids = fire(helper, new BlockPos(1, 3, 1), Blocks.NETHERRACK);
        helper.assertTrue(Extinguishing.extinguish(level, helper.absolutePos(solids), ExtinguishingAgent.WATER, sprayer)
                == ExtinguishingAgent.Outcome.PUTS_OUT, "water puts out burning solids");
        helper.assertBlockNotPresent(Blocks.FIRE, solids);
        final BlockPos live = fire(helper, new BlockPos(4, 3, 1), Blocks.NETHERRACK);
        helper.setBlock(live.east(), Blocks.LIGHTNING_ROD);
        helper.assertTrue(Extinguishing.extinguish(level, helper.absolutePos(live), ExtinguishingAgent.WATER, sprayer)
                == ExtinguishingAgent.Outcome.SHOCKS, "water on live equipment shocks");
        helper.assertBlockPresent(Blocks.FIRE, live);
        helper.assertTrue(sprayer.getLastDamageSource() != null
                && sprayer.getLastDamageSource().is(EnergyDamage.ELECTRIC), "the sprayer takes the current");
        helper.assertTrue(Extinguishing.extinguish(level, helper.absolutePos(live), ExtinguishingAgent.CARBON_DIOXIDE,
                sprayer) == ExtinguishingAgent.Outcome.PUTS_OUT, "carbon dioxide puts it out");
        final BlockPos oil = fire(helper, new BlockPos(1, 3, 5), Blocks.NETHERRACK);
        helper.setBlock(oil.east(), Blocks.WET_SPONGE);
        helper.assertTrue(Extinguishing.extinguish(level, helper.absolutePos(oil), ExtinguishingAgent.FOAM, sprayer)
                == ExtinguishingAgent.Outcome.PUTS_OUT, "foam smothers burning oil");
        final BlockPos liquid = fire(helper, new BlockPos(5, 3, 5), Blocks.NETHERRACK);
        helper.setBlock(liquid.below().east(), Blocks.NETHERRACK);
        helper.setBlock(liquid.below().west(), Blocks.NETHERRACK);
        helper.setBlock(liquid.north(), Blocks.SPONGE);
        helper.assertTrue(Extinguishing.extinguish(level, helper.absolutePos(liquid), ExtinguishingAgent.WATER, sprayer)
                == ExtinguishingAgent.Outcome.FLARES, "water spreads a burning liquid");
        helper.assertTrue(helper.getBlockState(liquid.east()).is(Blocks.FIRE)
                || helper.getBlockState(liquid.west()).is(Blocks.FIRE), "and the fire flares up round it");
        for (final BlockPos pos : BlockPos.betweenClosed(0, 2, 0, 8, 6, 8)) {
            if (helper.getBlockState(pos).is(Blocks.FIRE)) {
                helper.setBlock(pos, Blocks.AIR);
            }
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "jscore_fire_burst", timeoutTicks = 100)
    public static void agents_waterMakesABurningMetalBurst(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos metal = new BlockPos(4, 3, 4);
        helper.setBlock(metal.below(), Blocks.RAW_IRON_BLOCK);
        helper.setBlock(metal, Blocks.FIRE);
        helper.assertTrue(Extinguishing.extinguish(level, helper.absolutePos(metal), ExtinguishingAgent.DRY_POWDER,
                null) == ExtinguishingAgent.Outcome.PUTS_OUT, "dry powder smothers a burning metal");
        helper.setBlock(metal, Blocks.FIRE);
        helper.assertTrue(Extinguishing.extinguish(level, helper.absolutePos(metal), ExtinguishingAgent.WATER, null)
                == ExtinguishingAgent.Outcome.REACTS, "water on it makes it burst");
        helper.assertTrue(Explosions.running(level).stream().anyMatch(blast -> blast.explosive()
                .equals(Extinguishing.BURNING_METAL)), "a blast goes off");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(Explosions.running(level).stream().noneMatch(blast -> blast
                        .explosive().equals(Extinguishing.BURNING_METAL)), "the blast went all the way out"))
                .thenExecute(() -> {
                    for (final BlockPos pos : BlockPos.betweenClosed(0, 2, 0, 8, 6, 8)) {
                        if (helper.getBlockState(pos).is(Blocks.FIRE)) {
                            helper.setBlock(pos, Blocks.AIR);
                        }
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void extinguisher_spraysTheFireOutAndAWaterOneFillsAgain(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos fire = fire(helper, new BlockPos(4, 3, 6), Blocks.NETHERRACK);
        final Player sprayer = helper.makeMockPlayer(GameType.SURVIVAL);
        sprayer.moveTo(helper.absoluteVec(new Vec3(4.5, 2, 2.5)));
        sprayer.lookAt(EntityAnchorArgument.Anchor.EYES, helper.absoluteVec(new Vec3(4.5, 3.4, 6.5)));
        final ItemStack stack = new ItemStack(FireSafety.WATER_EXTINGUISHER.get());
        sprayer.setItemInHand(InteractionHand.MAIN_HAND, stack);
        final ExtinguisherItem item = FireSafety.WATER_EXTINGUISHER.get();
        item.onUseTick(level, sprayer, stack, item.getUseDuration(stack, sprayer) - 2);
        helper.assertBlockNotPresent(Blocks.FIRE, fire);
        helper.assertTrue(stack.getDamageValue() > 0, "spraying takes from what it holds");
        helper.setBlock(new BlockPos(2, 2, 2), Blocks.WATER);
        sprayer.moveTo(helper.absoluteVec(new Vec3(2.5, 3, 2.5)));
        sprayer.lookAt(EntityAnchorArgument.Anchor.EYES, helper.absoluteVec(new Vec3(2.5, 2.5, 2.5)));
        item.use(level, sprayer, InteractionHand.MAIN_HAND);
        helper.assertTrue(stack.getDamageValue() == 0, "a water extinguisher fills again from water");
        final ItemStack powder = new ItemStack(FireSafety.DRY_POWDER_EXTINGUISHER.get());
        helper.assertTrue(powder.getMaxDamage() == ExtinguisherItem.holds(ExtinguishingAgent.DRY_POWDER),
                "each agent holds its own time of spraying");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "jscore_fire_sprinkler", timeoutTicks = 200)
    public static void sprinkler_showersTheFireOutWithTheWaterOfItsPipe(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos head = new BlockPos(4, 5, 4);
        helper.setBlock(head.above(), Blocks.STONE);
        helper.setBlock(head, FireSafety.SPRINKLER.get());
        final IFluidHandler pipe = level.getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(head),
                Direction.NORTH);
        helper.assertTrue(pipe != null, "it offers its tank to a pipe");
        helper.assertTrue(pipe.fill(new FluidStack(Fluids.LAVA, 500), IFluidHandler.FluidAction.EXECUTE) == 0,
                "it takes no liquid but water");
        helper.assertTrue(pipe.fill(new FluidStack(Fluids.WATER, 2_000), IFluidHandler.FluidAction.EXECUTE)
                == SprinklerBlockEntity.HOLDS, "it fills with water to what it holds");
        helper.assertTrue(pipe.drain(500, IFluidHandler.FluidAction.EXECUTE).isEmpty(), "and gives none out");
        final BlockPos fire = fire(helper, new BlockPos(4, 3, 4), Blocks.NETHERRACK);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertBlockNotPresent(Blocks.FIRE, fire))
                .thenExecute(() -> {
                    helper.assertTrue(helper.getBlockState(head).getValue(SprinklerBlock.OPEN), "its bulb burst");
                    final SprinklerBlockEntity sprinkler = (SprinklerBlockEntity) level.getBlockEntity(
                            helper.absolutePos(head));
                    helper.assertTrue(sprinkler != null && sprinkler.water().getFluidAmount()
                            < SprinklerBlockEntity.HOLDS, "it showered its water");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = "jscore_fire_detectors", timeoutTicks = 200)
    public static void detectors_callAndSpeakRedstone(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos smoke = new BlockPos(4, 5, 4);
        final BlockPos heat = new BlockPos(5, 5, 4);
        helper.setBlock(smoke.above(), Blocks.STONE);
        helper.setBlock(heat.above(), Blocks.STONE);
        helper.setBlock(smoke, FireSafety.SMOKE_DETECTOR.get());
        helper.setBlock(heat, FireSafety.HEAT_DETECTOR.get());
        helper.startSequence()
                .thenExecuteAfter(15, () -> helper.assertTrue(!helper.getBlockState(smoke)
                        .getValue(FireDetectorBlock.ALARM), "no fire, no alarm"))
                .thenExecute(() -> fire(helper, new BlockPos(4, 3, 4), Blocks.NETHERRACK))
                .thenWaitUntil(() -> helper.assertTrue(helper.getBlockState(smoke).getValue(FireDetectorBlock.ALARM)
                        && helper.getBlockState(heat).getValue(FireDetectorBlock.ALARM), "both detectors call"))
                .thenExecute(() -> {
                    helper.assertTrue(Signals.strengthAt(level, helper.absolutePos(smoke), null) == 15,
                            "the alarm gives full redstone");
                    helper.setBlock(new BlockPos(4, 3, 4), Blocks.AIR);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = "jscore_fire_soot", timeoutTicks = 200)
    public static void soot_risesFromWhatBurns(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos fire = fire(helper, new BlockPos(4, 3, 4), Blocks.NETHERRACK);
        final ChunkPos chunk = new ChunkPos(helper.absolutePos(fire));
        final double before = Pollution.at(level, chunk, SeriesPollution.SOOT);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(Pollution.at(level, chunk, SeriesPollution.SOOT) > before,
                        "a fire gives off soot"))
                .thenExecute(() -> helper.setBlock(fire, Blocks.AIR))
                .thenSucceed();
    }

    /* Lights a fire at {@code at}, on a block of {@code under}. */
    private static BlockPos fire(final GameTestHelper helper, final BlockPos at, final Block under) {
        helper.setBlock(at.below(), under);
        helper.setBlock(at, Blocks.FIRE);
        return at;
    }

    private static FireClass classOf(final ServerLevel level, final BlockPos pos, final Block block) {
        return FireClasses.of(level, pos, block.defaultBlockState(), Direction.UP);
    }

    /* A closed box of stone, hollow inside. */
    private static void hut(final GameTestHelper helper, final BlockPos low, final BlockPos high) {
        for (final BlockPos at : BlockPos.betweenClosed(low, high)) {
            final boolean shell = at.getX() == low.getX() || at.getX() == high.getX() || at.getY() == low.getY()
                    || at.getY() == high.getY() || at.getZ() == low.getZ() || at.getZ() == high.getZ();
            helper.setBlock(at, shell ? Blocks.STONE : Blocks.AIR);
        }
    }

    private static void fill(final Room room, final double kilopascals, final Map<ResourceLocation, Double> shares) {
        room.empty();
        for (final Map.Entry<ResourceLocation, Double> gas : shares.entrySet()) {
            room.add(gas.getKey(), GasMath.moles(kilopascals * gas.getValue(), room.gasVolume(), GasMath.TEMPERED));
        }
    }

    private static Optional<Room> room(final GameTestHelper helper, final BlockPos at) {
        return SealedRooms.roomAt(helper.getLevel(), helper.absolutePos(at));
    }

    /* A strong east wind in the places of {@link #WINDY}, declared once. */
    private static synchronized void declareWind() {
        if (windDeclared) {
            return;
        }
        windDeclared = true;
        Winds.source((level, at) -> WINDY.stream().anyMatch(box -> box.contains(at)) ? Optional.of(GALE)
                : Optional.empty());
    }
}
