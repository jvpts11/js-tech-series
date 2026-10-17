/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.hazard;

import com.mojang.serialization.Codec;
import dev.jstech.core.JsCore;
import dev.jstech.core.hazard.DamageKinds;
import dev.jstech.core.hazard.HazardBalance;
import dev.jstech.core.hazard.HazardEffects;
import dev.jstech.core.hazard.HazardKind;
import dev.jstech.core.hazard.Hazards;
import dev.jstech.core.hazard.Pollution;
import dev.jstech.core.hazard.PollutionKind;
import dev.jstech.core.registry.CoreItems;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * The series' kinds of pollution, each with what it really does. Soot and particulates hang as a haze over the chunk,
 * and over a threshold whoever breathes them without a mask coughs, as in London's smog of 1952; rain and plants take
 * them down. Sulfur and nitrogen oxides fall as acid rain where it rains: leaves die slowly, crops grow slower, exposed
 * copper and calcite wear away. Heavy metals lie in the soil and the water: crops grown and fish caught there carry
 * them, and eating them poisons slowly, as in Minamata and Itai-itai; the soil is cleaned by digging it out, or slowly
 * by plants. Radioactive fallout spreads and decays, raises the dose rate where it lies and settles on whoever stands
 * in it. Carbon dioxide is one number for the whole world ({@link CarbonDioxide}). The whole of it is turned off with
 * the world's pollution setting, and fallout with its radiation.
 */
@TextHolder
public final class SeriesPollution {

    public static final TextKey MICROGRAMS = TextKey.of("jscore.pollution.unit.micrograms", "µg/m³");
    public static final TextKey MILLIGRAMS_PER_KILOGRAM = TextKey.of("jscore.pollution.unit.mg_per_kg", "mg/kg");
    public static final TextKey MILLIGRAMS = TextKey.of("jscore.pollution.unit.mg", "mg");
    public static final TextKey KILOBECQUERELS_PER_SQUARE_METRE = TextKey.of("jscore.pollution.unit.kbq_per_m2",
            "kBq/m²");
    public static final TextKey SOOT_NAME = TextKey.of("jscore.pollution.soot", "Soot and particulates");
    public static final TextKey SMOG_NAME = TextKey.of("jscore.hazard.smog", "Smog");
    public static final TextKey SMOG_SHORT = TextKey.of("jscore.hazard.smog.short", "SMOG");
    public static final TextKey COUGHING = TextKey.of("jscore.hazard.smog.coughing", "Coughing");
    public static final TextKey CHOKING = TextKey.of("jscore.hazard.smog.choking", "Choking on the smog");
    public static final TextKey SMOG_TREATMENT = TextKey.of("jscore.hazard.smog.treatment",
            "Get out of the haze, or wear a mask");
    public static final TextKey ACID_NAME = TextKey.of("jscore.pollution.acid", "Sulfur and nitrogen oxides");
    public static final TextKey ACID_AIR_NAME = TextKey.of("jscore.hazard.acid_air", "Acid in the air");
    public static final TextKey ACID_SHORT = TextKey.of("jscore.hazard.acid_air.short", "ACID");
    public static final TextKey METALS_NAME = TextKey.of("jscore.pollution.heavy_metals", "Heavy metals");
    public static final TextKey METALS_GROUND_NAME = TextKey.of("jscore.hazard.heavy_metals_ground",
            "Heavy metals in the ground");
    public static final TextKey METALS_SHORT = TextKey.of("jscore.hazard.heavy_metals_ground.short", "METAL");
    public static final TextKey POISONING_NAME = TextKey.of("jscore.hazard.heavy_metal_poisoning",
            "Heavy metal poisoning");
    public static final TextKey TIRED = TextKey.of("jscore.hazard.heavy_metal_poisoning.tired", "Tired and weak");
    public static final TextKey ACHING = TextKey.of("jscore.hazard.heavy_metal_poisoning.aching", "Aching bones");
    public static final TextKey NERVES = TextKey.of("jscore.hazard.heavy_metal_poisoning.nerves", "Nerves failing");
    public static final TextKey POISONING_TREATMENT = TextKey.of("jscore.hazard.heavy_metal_poisoning.treatment",
            "Chelation draws the metals out; stop eating what grew on fouled ground");
    public static final TextKey CONTAMINATED_FOOD = TextKey.of("jscore.pollution.contaminated_food",
            "Heavy metals: %s mg");
    public static final TextKey FALLOUT_NAME = TextKey.of("jscore.pollution.fallout", "Radioactive fallout");
    public static final TextKey DIED = TextKey.of("death.attack.jscore.heavy_metal_poisoning",
            "%1$s was poisoned by heavy metals");
    public static final TextKey DIED_FIGHTING = TextKey.of("death.attack.jscore.heavy_metal_poisoning.player",
            "%1$s was poisoned by heavy metals while fighting %2$s");

    /** What heavy metal poisoning kills as. */
    public static final ResourceKey<DamageType> POISONED = ResourceKey.create(Registries.DAMAGE_TYPE,
            id("heavy_metal_poisoning"));
    /** The heavy metals a thing grown or caught on fouled ground carries, in milligrams; eating it takes them in. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Float>> HEAVY_METALS =
            CoreItems.CONTENT.component("heavy_metals", Codec.floatRange(0, 100_000), ByteBufCodecs.FLOAT);
    /* Where acid rain starts eating, the share of crops it stops growing for each microgram, and the most. */
    private static final double ACID_RAIN_AT = 40;
    private static final double ACID_STUNTS = 1.0 / 400;
    private static final double MOST_STUNTED = 0.5;
    /* What share of the soot a column's rain washes down at each look. */
    private static final double RAIN_WASHES = 0.02;
    /* Over how many milligrams a kilogram a crop or a fish carries metals, and how much for each. */
    private static final double FOOD_FOULED_AT = 5;
    private static final double TAKEN_UP = 0.01;
    /* What share of a chunk's metals each block of soil dug out takes with it. */
    private static final double DUG_OUT = 1.0 / 64;
    /* The dose rate and the contamination fallout gives, for each kilobecquerel a square metre. */
    private static final double FALLOUT_RATE = 0.001;
    private static final double FALLOUT_SETTLES = 0.0002;

    /** Smog breathed: how much soot is in the air the body breathes, in micrograms a cubic metre. */
    public static final HazardKind SMOG = Hazards.declare(HazardKind.builder(id("smog"), SMOG_NAME, MICROGRAMS)
            .band(150, COUGHING, HazardEffects.effect(MobEffects.MOVEMENT_SLOWDOWN, 0),
                    HazardEffects.effect(MobEffects.DIG_SLOWDOWN, 0))
            .band(300, CHOKING, HazardEffects.effect(MobEffects.MOVEMENT_SLOWDOWN, 1),
                    HazardEffects.effect(MobEffects.WEAKNESS, 0))
            .recoversShare(1).background(35).protectedBy(DamageKinds.INHALED).treatedBy(SMOG_TREATMENT)
            .enabledWhen(HazardBalance::pollution).shortName(SMOG_SHORT).rateShownIn(MICROGRAMS, 1)
            .mapped(35, 75, 150, 300).build());
    /** The acid in the air where a body stands, which harms the land rather than the body; for the map. */
    public static final HazardKind ACID_AIR = Hazards.declare(HazardKind.builder(id("acid_air"), ACID_AIR_NAME,
                    MICROGRAMS)
            .recoversShare(1).enabledWhen(HazardBalance::pollution).shortName(ACID_SHORT)
            .rateShownIn(MICROGRAMS, 1).mapped(20, 40, 80, 160).build());
    /** The heavy metals in the ground a body stands on, which harm through what grows there; for the map. */
    public static final HazardKind METALS_GROUND = Hazards.declare(HazardKind.builder(id("heavy_metals_ground"),
                    METALS_GROUND_NAME, MILLIGRAMS_PER_KILOGRAM)
            .recoversShare(1).enabledWhen(HazardBalance::pollution).shortName(METALS_SHORT)
            .rateShownIn(MILLIGRAMS_PER_KILOGRAM, 1).mapped(5, 20, 80, 300).build());
    /** The heavy metals taken in by eating, in milligrams, which leave the body slowly. */
    public static final HazardKind POISONING = Hazards.declare(HazardKind.builder(id("heavy_metal_poisoning"),
                    POISONING_NAME, MILLIGRAMS)
            .band(5, TIRED, HazardEffects.effect(MobEffects.WEAKNESS, 0))
            .band(15, ACHING, HazardEffects.effect(MobEffects.WEAKNESS, 1),
                    HazardEffects.effect(MobEffects.MOVEMENT_SLOWDOWN, 0), HazardEffects.damage(0.25F))
            .band(30, NERVES, HazardEffects.effect(MobEffects.WEAKNESS, 1), HazardEffects.effect(MobEffects.CONFUSION,
                    0), HazardEffects.damage(1))
            .recovers(0.001).treatedBy(POISONING_TREATMENT).deadlyAs(POISONED).enabledWhen(HazardBalance::pollution)
            .upTo(100).build());

    public static final PollutionKind SOOT = Pollution.declare(PollutionKind.builder(id("soot"), SOOT_NAME,
                    MICROGRAMS)
            .spread(0.15).fades(0.02).plantsTakeUp(0.1).felt(SMOG).withersAt(2_000)
            .enabledWhen(HazardBalance::pollution).build());
    public static final PollutionKind ACID = Pollution.declare(PollutionKind.builder(id("acid"), ACID_NAME,
                    MICROGRAMS)
            .spread(0.2).fades(0.03).plantsTakeUp(0.02).felt(ACID_AIR).withersAt(600)
            .enabledWhen(HazardBalance::pollution).build());
    public static final PollutionKind HEAVY_METALS_IN_SOIL = Pollution.declare(PollutionKind.builder(
                    id("heavy_metals"), METALS_NAME, MILLIGRAMS_PER_KILOGRAM)
            .spread(0.02).fades(0).every(200).plantsTakeUp(0.01).felt(METALS_GROUND)
            .enabledWhen(HazardBalance::pollution).build());
    public static final PollutionKind FALLOUT = Pollution.declare(PollutionKind.builder(id("fallout"), FALLOUT_NAME,
                    KILOBECQUERELS_PER_SQUARE_METRE)
            .spread(0.05).fades(0.001).every(200).enabledWhen(HazardBalance::radiation).build());

    static {
        SOOT.addEffect((level, top, amount) -> {
            if (level.isRainingAt(top.above())) {
                SOOT.field().add(level, new ChunkPos(top), (float) (-amount * RAIN_WASHES));
            }
        });
        ACID.addEffect(SeriesPollution::acidRain);
        IonizingRadiation.RAYS.addAmbient((level, at) -> FALLOUT.field().get(level,
                new ChunkPos(BlockPos.containing(at))) * FALLOUT_RATE);
        IonizingRadiation.CONTAMINATION.addSource((body, level) -> FALLOUT.enabled()
                ? FALLOUT.field().get(level, body.chunkPosition()) * FALLOUT_SETTLES : 0);
    }

    private SeriesPollution() {
    }

    /** Declares the kinds and hands the game's bus what they do to crops, catches, meals and dug soil. */
    public static void declare() {
        CarbonDioxide.declare();
        NeoForge.EVENT_BUS.addListener(CropGrowEvent.Pre.class, event -> {
            if (event.getLevel() instanceof ServerLevel level && ACID.enabled()) {
                final double acid = ACID.field().get(level, new ChunkPos(event.getPos()));
                if (level.getRandom().nextDouble() < Math.min(MOST_STUNTED, acid * ACID_STUNTS)) {
                    event.setResult(CropGrowEvent.Pre.Result.DO_NOT_GROW);
                }
            }
        });
        NeoForge.EVENT_BUS.addListener(BlockDropsEvent.class, event -> {
            if (event.getState().is(BlockTags.CROPS)) {
                foul(event.getLevel(), event.getPos(), event.getDrops().stream().map(ItemEntity::getItem).toList());
            }
        });
        NeoForge.EVENT_BUS.addListener(ItemFishedEvent.class, event -> {
            if (event.getHookEntity().level() instanceof ServerLevel level) {
                foul(level, event.getHookEntity().blockPosition(), event.getDrops());
            }
        });
        NeoForge.EVENT_BUS.addListener(LivingEntityUseItemEvent.Finish.class, event -> {
            final Float metals = event.getItem().get(HEAVY_METALS.get());
            if (metals != null && !event.getEntity().level().isClientSide()) {
                Hazards.add(event.getEntity(), POISONING, metals);
            }
        });
        NeoForge.EVENT_BUS.addListener(BlockEvent.BreakEvent.class, event -> {
            if (event.getLevel() instanceof ServerLevel level && event.getState().is(BlockTags.DIRT)) {
                final ChunkPos chunk = new ChunkPos(event.getPos());
                final float metals = HEAVY_METALS_IN_SOIL.field().get(level, chunk);
                if (metals > 0) {
                    HEAVY_METALS_IN_SOIL.field().add(level, chunk, (float) (-metals * DUG_OUT));
                }
            }
        });
    }

    /** The heavy metals {@code stack} carries, in milligrams; nothing for clean food. */
    public static double metalsIn(final ItemStack stack) {
        final Float metals = stack.get(HEAVY_METALS.get());
        return metals == null ? 0 : metals;
    }

    /* What grew or was caught at {@code pos} carries the metals of its ground, past the least that fouls food. */
    private static void foul(final ServerLevel level, final BlockPos pos, final Iterable<ItemStack> things) {
        if (!HEAVY_METALS_IN_SOIL.enabled()) {
            return;
        }
        final double ground = HEAVY_METALS_IN_SOIL.field().get(level, new ChunkPos(pos));
        if (ground <= FOOD_FOULED_AT) {
            return;
        }
        for (final ItemStack thing : things) {
            if (thing.getFoodProperties(null) != null) {
                thing.set(HEAVY_METALS.get(), (float) (ground * TAKEN_UP));
            }
        }
    }

    /* Acid rain at a column where it rains: leaves die, copper weathers, calcite crumbles, by how much acid falls. */
    private static void acidRain(final ServerLevel level, final BlockPos top, final double amount) {
        if (amount < ACID_RAIN_AT || !level.isRainingAt(top.above())) {
            return;
        }
        if (level.getRandom().nextDouble() >= Math.min(MOST_STUNTED, amount * ACID_STUNTS)) {
            return;
        }
        final BlockState state = level.getBlockState(top);
        if (state.is(BlockTags.LEAVES)) {
            level.destroyBlock(top, false);
        } else if (state.is(Blocks.CALCITE)) {
            level.setBlockAndUpdate(top, Blocks.GRAVEL.defaultBlockState());
        } else {
            final Block next = WeatheringCopper.getNext(state.getBlock()).orElse(null);
            if (next != null) {
                level.setBlockAndUpdate(top, next.withPropertiesOf(state));
            }
        }
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsCore.MODID, path);
    }
}
