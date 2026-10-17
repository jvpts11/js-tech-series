/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import com.mojang.serialization.JsonOps;
import dev.jstech.core.dimension.DimensionHazardEvent;
import dev.jstech.core.dimension.DimensionHazards;
import dev.jstech.core.gametest.GameTestPlayers;
import dev.jstech.core.hazard.DamageKinds;
import dev.jstech.core.hazard.HazardMap;
import dev.jstech.core.hazard.HazardMaps;
import dev.jstech.core.hazard.Hazards;
import dev.jstech.core.hazard.IHazardInstrument;
import dev.jstech.core.hazard.ItemHazards;
import dev.jstech.core.hazard.Lungs;
import dev.jstech.core.hazard.Pollution;
import dev.jstech.core.hazard.Resistances;
import dev.jstech.core.hazard.Withering;
import dev.jstech.core.jstechseries.hazard.DeviceState;
import dev.jstech.core.jstechseries.hazard.HazardDevices;
import dev.jstech.core.jstechseries.hazard.WitheredGrass;
import dev.jstech.core.registry.CoreAttachments;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestHazards;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The hazard kit on a server: worn things and creatures stopping their share of each kind of harm, a hazard building
 * up from its sources and going away from them, in bands, cut by what is worn, suffered by creatures; how it kills;
 * dust breathed from broken blocks; what carried things do; the dimensions' hazards as kinds of the kit; the land
 * withering where it is contaminated; and the maps of hazards keeping the readings instruments take.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class HazardGameTests {

    private static final String ARENA = "empty";
    private static final double CLOSE = 1.0E-4;
    private static final BlockPos GROUND = new BlockPos(1, 1, 1);
    private static final BlockPos PIG = new BlockPos(2, 2, 2);

    private HazardGameTests() {
    }

    @GameTest(template = ARENA)
    public static void resistances_ofWhatIsWornAddUpByKind(final GameTestHelper helper) {
        final ServerPlayer player = GameTestPlayers.join(helper, "hazard_worn");
        try {
            wearProtection(player);
            close(helper, 0.25, Resistances.of(new ItemStack(Items.TURTLE_HELMET), DamageKinds.HEAT), "the helmet");
            close(helper, 0.75, Resistances.worn(player, DamageKinds.HEAT), "the helmet's and the chestplate's");
            close(helper, 0.9, Resistances.worn(player, DamageKinds.INHALED), "the helmet's against dust");
            helper.assertTrue(Resistances.worn(player).keySet().containsAll(List.of(DamageKinds.HEAT,
                    DamageKinds.INHALED)), "both kinds are worn; " + Resistances.worn(player));
            close(helper, 0, Resistances.worn(player, DamageKinds.ACID), "nothing worn against acid");
        } finally {
            GameTestPlayers.leave(player);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void resistances_cutTheDamageOfTheirKind(final GameTestHelper helper) {
        final Pig pig = helper.spawn(EntityType.PIG, PIG);
        final float before = pig.getHealth();
        pig.hurt(helper.getLevel().damageSources().onFire(), 4);
        close(helper, 2, before - pig.getHealth(), "a pig stops half the heat");
        pig.invulnerableTime = 0;
        final float hurt = pig.getHealth();
        pig.hurt(helper.getLevel().damageSources().magic(), 2);
        close(helper, 2, hurt - pig.getHealth(), "and none of what is no kind of its");
        helper.succeed();
    }

    /* The game's mock player: a band's effect reaches it, where a test's server player has no connection to tell. */
    @GameTest(template = ARENA)
    public static void hazard_buildsUpFromItsSourcesInBandsAndRecovers(final GameTestHelper helper) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        try {
            TestHazards.expose(player, 2);
            for (int second = 0; second < 3; second++) {
                Hazards.second(player, helper.getLevel());
            }
            close(helper, 6, Hazards.level(player, TestHazards.KIND), "three seconds of two a second");
            close(helper, 2, Hazards.rate(player, TestHazards.KIND), "the rate");
            close(helper, 2, Hazards.exposure(player, TestHazards.KIND), "what came at the player");
            helper.assertTrue(Hazards.band(player, TestHazards.KIND).map(band -> band.name() == TestHazards.ITCHY)
                    .orElse(false), "the first band");
            helper.assertTrue(player.hasEffect(MobEffects.WEAKNESS), "whose effect weakens");
            TestHazards.expose(player, 0);
            Hazards.second(player, helper.getLevel());
            close(helper, 5, Hazards.level(player, TestHazards.KIND), "a unit recovered away from the source");
            close(helper, 0, Hazards.exposure(player, TestHazards.KIND), "nothing comes at the player now");
        } finally {
            TestHazards.expose(player, 0);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void hazard_recoversFasterWithWhatSpeedsIt(final GameTestHelper helper) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Hazards.set(player, TestHazards.KIND, 4);
        Hazards.second(player, helper.getLevel());
        close(helper, 3, Hazards.level(player, TestHazards.KIND), "a unit a second at its own pace");
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200));
        Hazards.second(player, helper.getLevel());
        close(helper, 1, Hazards.level(player, TestHazards.KIND), "twice as fast under the medicine");
        Hazards.second(player, helper.getLevel());
        close(helper, 0, Hazards.level(player, TestHazards.KIND), "never below nothing");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void hazard_isCutByWhatIsWornAgainstIt(final GameTestHelper helper) {
        final ServerPlayer player = GameTestPlayers.join(helper, "hazard_worn_cut");
        try {
            wearProtection(player);
            TestHazards.expose(player, 4);
            Hazards.second(player, helper.getLevel());
            close(helper, 1, Hazards.level(player, TestHazards.KIND), "three quarters stopped by the heat gear");
            close(helper, 4, Hazards.exposure(player, TestHazards.KIND), "all of it came at the player");
        } finally {
            TestHazards.expose(player, 0);
            GameTestPlayers.leave(player);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void hazard_isSufferedByCreaturesOnlyOfKindsThatHarmThem(final GameTestHelper helper) {
        final Cow cow = helper.spawn(EntityType.COW, PIG);
        try {
            TestHazards.expose(cow, 3);
            Lungs.breathe(cow, Lungs.BLACK_LUNG, 5);
            Hazards.second(cow, helper.getLevel());
            close(helper, 3, Hazards.level(cow, TestHazards.KIND), "the test hazard harms creatures");
            close(helper, 0, Hazards.level(cow, Lungs.BLACK_LUNG), "black lung is a player's");
        } finally {
            TestHazards.expose(cow, 0);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void hazard_killsAsItsOwnDamage(final GameTestHelper helper) {
        final Pig pig = helper.spawn(EntityType.PIG, PIG);
        final Component message = Lungs.BLACK_LUNG.damageSource(helper.getLevel()).getLocalizedDeathMessage(pig);
        helper.assertTrue(message.getContents() instanceof TranslatableContents said
                && "death.attack.jscore.lung_failure".equals(said.getKey()), "the lungs' own death; " + message);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void lungs_breatheTheDustOfBrokenBlocksLessThroughAMask(final GameTestHelper helper) {
        final ServerPlayer bare = GameTestPlayers.join(helper, "hazard_lungs_bare");
        final ServerPlayer masked = GameTestPlayers.join(helper, "hazard_lungs_masked");
        try {
            masked.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.TURTLE_HELMET));
            final BlockPos ore = helper.absolutePos(GROUND);
            helper.setBlock(GROUND, Blocks.COAL_ORE);
            final BlockState state = helper.getLevel().getBlockState(ore);
            NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(helper.getLevel(), ore, state, bare));
            NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(helper.getLevel(), ore, state, masked));
            Hazards.second(bare, helper.getLevel());
            Hazards.second(masked, helper.getLevel());
            close(helper, 0.05, Hazards.level(bare, Lungs.BLACK_LUNG), "a coal block's dust, bare");
            close(helper, 0.005, Hazards.level(masked, Lungs.BLACK_LUNG), "a tenth of it through the mask");
            close(helper, 0.05, Lungs.harmed(bare), "the lungs' harm in all");
        } finally {
            GameTestPlayers.leave(bare);
            GameTestPlayers.leave(masked);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void itemHazards_hotThingsBurnTheirCarrierLessThroughHeatGear(final GameTestHelper helper) {
        final Player bare = helper.makeMockPlayer(GameType.SURVIVAL);
        final Player geared = helper.makeMockPlayer(GameType.SURVIVAL);
        wearProtection(geared);
        for (final Player player : List.of(bare, geared)) {
            player.getInventory().add(new ItemStack(Items.MAGMA_CREAM));
        }
        helper.assertTrue(ItemHazards.of(new ItemStack(Items.MAGMA_CREAM)).containsKey(ItemHazards.HOT),
                "magma cream is hot");
        close(helper, 2, ItemHazards.carried(bare, ItemHazards.HOT), "how hot what is carried is");
        ItemHazards.second(bare);
        ItemHazards.second(geared);
        close(helper, 2, bare.getMaxHealth() - bare.getHealth(), "it burns the bare hand");
        close(helper, 0.5, geared.getMaxHealth() - geared.getHealth(), "a quarter of it through heat gear");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void dimensionHazards_areKindsOfTheKitThatPassAtOnce(final GameTestHelper helper) {
        for (final DimensionHazardEvent.Hazard hazard : DimensionHazardEvent.Hazard.values()) {
            helper.assertTrue(Hazards.find(DimensionHazards.of(hazard).id()).isPresent(),
                    "declared in the kit: " + hazard);
        }
        final ServerPlayer player = GameTestPlayers.join(helper, "hazard_dimension");
        try {
            Hazards.add(player, DimensionHazards.AIR, 3);
            Hazards.second(player, helper.getLevel());
            close(helper, 0, Hazards.level(player, DimensionHazards.AIR), "out of the bad air, it is gone at once");
        } finally {
            GameTestPlayers.leave(player);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void withering_killsGrassAndPlantsWhereContaminatedAndTheyComeBack(final GameTestHelper helper) {
        final BlockPos plant = GROUND.above();
        helper.setBlock(GROUND, Blocks.GRASS_BLOCK);
        helper.setBlock(plant, Blocks.POPPY);
        final BlockPos top = helper.absolutePos(plant);
        TestHazards.contaminate(top, true);
        try {
            helper.assertTrue(Withering.weigh(helper.getLevel(), top, helper.getLevel().getRandom()),
                    "the contaminated column withers");
            helper.assertBlockPresent(Blocks.DEAD_BUSH, plant);
            helper.assertBlockPresent(WitheredGrass.BLOCK.get(), GROUND);
        } finally {
            TestHazards.contaminate(top, false);
        }
        boolean back = false;
        for (int tries = 0; tries < 200 && !back; tries++) {
            Withering.weigh(helper.getLevel(), top, helper.getLevel().getRandom());
            back = helper.getBlockState(GROUND).is(Blocks.GRASS_BLOCK);
        }
        helper.assertTrue(back, "clean again, the grass came back");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void witheredGrass_dropsDirtOrItselfWithSilkTouch(final GameTestHelper helper) {
        final BlockState withered = WitheredGrass.BLOCK.get().defaultBlockState();
        final ItemStack silk = new ItemStack(Items.NETHERITE_SHOVEL);
        silk.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.SILK_TOUCH), 1);
        final List<ItemStack> plain = drops(helper, withered, new ItemStack(Items.NETHERITE_SHOVEL));
        final List<ItemStack> careful = drops(helper, withered, silk);
        helper.assertTrue(plain.size() == 1 && plain.getFirst().is(Items.DIRT), "dirt; " + plain);
        helper.assertTrue(careful.size() == 1 && careful.getFirst().is(WitheredGrass.BLOCK.item()),
                "itself with silk touch; " + careful);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void withering_stopsCropsWhereContaminated(final GameTestHelper helper) {
        helper.setBlock(GROUND, Blocks.FARMLAND);
        helper.setBlock(GROUND.above(), Blocks.WHEAT);
        final BlockPos crop = helper.absolutePos(GROUND.above());
        final BlockState wheat = helper.getLevel().getBlockState(crop);
        helper.assertTrue(CommonHooks.canCropGrow(helper.getLevel(), crop, wheat, true), "clean, it grows");
        TestHazards.contaminate(crop, true);
        try {
            helper.assertFalse(CommonHooks.canCropGrow(helper.getLevel(), crop, wheat, true),
                    "contaminated, it does not");
        } finally {
            TestHazards.contaminate(crop, false);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void hazardMaps_keepAReadingWhenItIsNewOrMoved(final GameTestHelper helper) {
        final ServerPlayer player = GameTestPlayers.join(helper, "hazard_map");
        try {
            final ChunkPos chunk = new ChunkPos(helper.absolutePos(GROUND));
            HazardMaps.record(player, helper.getLevel().dimension(), chunk, TestHazards.KIND, 2);
            close(helper, 2, reading(helper, player, chunk).map(HazardMap.Reading::value).orElse(-1F), "read");
            HazardMaps.record(player, helper.getLevel().dimension(), chunk, TestHazards.KIND, 2.1);
            close(helper, 2, reading(helper, player, chunk).map(HazardMap.Reading::value).orElse(-1F),
                    "a small change waits until the reading grows old");
            HazardMaps.record(player, helper.getLevel().dimension(), chunk, TestHazards.KIND, 3);
            close(helper, 3, reading(helper, player, chunk).map(HazardMap.Reading::value).orElse(-1F),
                    "a moved rate is read at once");
            helper.assertTrue(reading(helper, player, new ChunkPos(chunk.x + 5, chunk.z)).isEmpty(),
                    "a chunk never measured is unknown");
            HazardMaps.tellAll(player);
        } finally {
            GameTestPlayers.leave(player);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void hazardDevices_measureTheSeriesRadiationAndKeepTheirSettings(final GameTestHelper helper) {
        final ItemStack geiger = new ItemStack(HazardDevices.GEIGER_COUNTER.get());
        helper.assertTrue(geiger.getItem() instanceof IHazardInstrument instrument
                && instrument.reads(geiger).equals(List.of(HazardDevices.RADIATION)),
                "the counter reads radiation");
        helper.assertTrue(HazardMaps.readBy(geiger).equals(List.of(Hazards.find(HazardDevices.RADIATION)
                .orElseThrow())), "as the kit finds it");
        helper.assertTrue(HazardMaps.readBy(new ItemStack(HazardDevices.HAZARD_MAP.get())).equals(
                Pollution.kinds().stream().flatMap(kind -> kind.felt().stream()).toList()),
                "the map device reads every pollution that harms bodies");
        helper.assertTrue(HazardMaps.readBy(new ItemStack(HazardDevices.POCKET_DOSIMETER.get())).isEmpty(),
                "the dosimeter maps nothing");
        DeviceState state = DeviceState.DEFAULT;
        helper.assertTrue(state.multiplier() == 1 && state.sound() && !state.light(), "how a device comes");
        state = state.nextRange();
        helper.assertTrue(state.multiplier() == 10, "the second range");
        state = state.nextRange().nextRange();
        helper.assertTrue(state.range() == 0, "round to the first after the last");
        final DeviceState set = state.toggleSound().toggleLight().showing(TestHazards.KIND.id().toString());
        final DeviceState read = DeviceState.CODEC.parse(JsonOps.INSTANCE, DeviceState.CODEC.encodeStart(
                JsonOps.INSTANCE, set).getOrThrow()).getOrThrow();
        helper.assertTrue(read.equals(set), "kept as it was set; " + read);
        geiger.set(HazardDevices.STATE.get(), set);
        helper.assertTrue(HazardDevices.state(geiger).equals(set), "on the device");
        helper.succeed();
    }

    /* The helmet and the chestplate the test data says stop heat, a quarter and a half, and dust. */
    private static void wearProtection(final Player player) {
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.TURTLE_HELMET));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.GOLDEN_CHESTPLATE));
    }

    private static List<ItemStack> drops(final GameTestHelper helper, final BlockState state, final ItemStack tool) {
        return state.getDrops(new LootParams.Builder(helper.getLevel())
                .withParameter(LootContextParams.ORIGIN, Vec3.ZERO)
                .withParameter(LootContextParams.TOOL, tool));
    }

    private static Optional<HazardMap.Reading> reading(final GameTestHelper helper, final ServerPlayer player,
                                                       final ChunkPos chunk) {
        return player.getData(CoreAttachments.HAZARD_MAP).reading(helper.getLevel().dimension().location(),
                TestHazards.KIND.id(), chunk.toLong());
    }

    private static void close(final GameTestHelper helper, final double expected, final double actual,
                              final String what) {
        helper.assertTrue(Math.abs(expected - actual) < CLOSE + Math.abs(expected) * CLOSE,
                what + ": expected " + expected + ", got " + actual);
    }
}
