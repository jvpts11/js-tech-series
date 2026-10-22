/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.hazard;

import com.mojang.serialization.Codec;
import dev.jstech.core.JsCore;
import dev.jstech.core.text.TextKey;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.ToDoubleFunction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;
import org.jetbrains.annotations.Nullable;

/**
 * What an item does to whoever carries it, as the data map {@code jscore:item_hazards} gives each item its hazards and
 * how strong: radioactive, hot, reacting with water, pyrophoric, unstable, toxic to touch, corrosive, blinding, and
 * the kinds a mod declares. Each says what protects from it, by the kind of harm worn things stop; the tooltip shows
 * each hazard and what protects. Weighed once a second for every player; what is unstable goes off when its carrier
 * burns, is caught in a blast or falls hard.
 */
public final class ItemHazards {

    private static final Map<ResourceLocation, Kind> DECLARED = new LinkedHashMap<>();
    /** Each item's hazards, by the hazard's id, and how strong each is. */
    public static final DataMapType<Item, Map<ResourceLocation, Float>> ITEMS = DataMapType.builder(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "item_hazards"), Registries.ITEM,
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.floatRange(0F, 1000F)))
            .synced(Codec.unboundedMap(ResourceLocation.CODEC, Codec.floatRange(0F, 1000F)), false).build();
    /** The damage of what burns the hand, of the heat kind. */
    public static final ResourceKey<DamageType> HOT_ITEM = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "hot_item"));
    /** The damage of what eats into the hand, of the acid kind. */
    public static final ResourceKey<DamageType> CORROSIVE_ITEM = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "corrosive_item"));
    private static final int TICKS_A_SECOND = 20;
    /* How long an effect a carried thing gives lasts, in ticks: a little over the second until it is given again. */
    private static final int HELD_TICKS = 70;
    /* The least fall damage that sets off what is unstable: a hard knock. */
    private static final float HARD_KNOCK = 4F;

    public static final Kind RADIOACTIVE = declare(new Kind(id("radioactive"), HazardTexts.RADIOACTIVE,
            HazardTexts.RADIOACTIVE_GUARD, null, (carrier, stack, strength, stopped) -> {
                // Its dose is the radiation kit's, which counts what is carried among its sources.
            }));
    public static final Kind HOT = declare(new Kind(id("hot"), HazardTexts.HOT, HazardTexts.HOT_GUARD,
            DamageKinds.HEAT, (carrier, stack, strength, stopped) -> {
                // Its damage is of the heat kind, so what is worn takes its share off as the damage lands.
                if (stopped < 1) {
                    carrier.hurt(source(carrier.level(), HOT_ITEM), (float) strength);
                }
            }));
    public static final Kind WATER_REACTIVE = declare(new Kind(id("water_reactive"), HazardTexts.WATER_REACTIVE,
            HazardTexts.WATER_REACTIVE_GUARD, null, (carrier, stack, strength, stopped) -> {
                if (carrier.isInWaterOrRain()) {
                    goOff(carrier, stack, strength);
                }
            }));
    public static final Kind PYROPHORIC = declare(new Kind(id("pyrophoric"), HazardTexts.PYROPHORIC,
            HazardTexts.PYROPHORIC_GUARD, null,
            (carrier, stack, strength, stopped) -> carrier.igniteForSeconds((float) Math.max(1, strength))));
    public static final Kind UNSTABLE = declare(new Kind(id("unstable"), HazardTexts.UNSTABLE,
            HazardTexts.UNSTABLE_GUARD, null, (carrier, stack, strength, stopped) -> {
                // It goes off when its carrier burns, is caught in a blast or falls hard.
            }));
    public static final Kind TOXIC = declare(new Kind(id("toxic"), HazardTexts.TOXIC_TOUCH, HazardTexts.TOXIC_GUARD,
            DamageKinds.TOXIC, (carrier, stack, strength, stopped) -> {
                if (stopped < 1) {
                    carrier.addEffect(new MobEffectInstance(MobEffects.POISON, HELD_TICKS,
                            strength * (1 - stopped) >= 2 ? 1 : 0));
                }
            }));
    public static final Kind CORROSIVE = declare(new Kind(id("corrosive"), HazardTexts.CORROSIVE,
            HazardTexts.CORROSIVE_GUARD, DamageKinds.ACID, (carrier, stack, strength, stopped) -> {
                // Its damage is of the acid kind, so what is worn takes its share off as the damage lands.
                if (stopped < 1) {
                    carrier.hurt(source(carrier.level(), CORROSIVE_ITEM), (float) strength);
                }
            }));
    public static final Kind BLINDING = declare(new Kind(id("blinding"), HazardTexts.BLINDING_LIGHT,
            HazardTexts.BLINDING_GUARD, DamageKinds.BLINDING, (carrier, stack, strength, stopped) -> {
                if (stopped < 1 && (carrier.getMainHandItem() == stack || carrier.getOffhandItem() == stack)) {
                    carrier.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, HELD_TICKS, 0));
                }
            }));

    private ItemHazards() {
    }

    /** Declares {@code kind}, from a mod's constructor; its texts are said beside it, in a class of texts. */
    public static Kind declare(final Kind kind) {
        synchronized (DECLARED) {
            if (DECLARED.putIfAbsent(kind.id(), kind) != null) {
                throw new IllegalStateException("the item hazard " + kind.id() + " is declared twice");
            }
        }
        return kind;
    }

    /** The kind {@code id}, if declared. */
    public static Optional<Kind> find(final ResourceLocation id) {
        synchronized (DECLARED) {
            return Optional.ofNullable(DECLARED.get(id));
        }
    }

    /** Every kind declared. */
    public static Collection<Kind> all() {
        synchronized (DECLARED) {
            return List.copyOf(DECLARED.values());
        }
    }

    /** Hands the buses the data map and what weighs what is carried, once, from the Core's constructor. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterDataMapTypesEvent.class, event -> event.register(ITEMS));
        NeoForge.EVENT_BUS.addListener(PlayerTickEvent.Post.class, ItemHazards::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(LivingDamageEvent.Post.class, ItemHazards::onHurt);
    }

    /** The hazards of the item of {@code stack}, each with how strong, in the order kinds were declared. */
    public static Map<Kind, Double> of(final ItemStack stack) {
        final Map<Kind, Double> out = new LinkedHashMap<>();
        if (stack.isEmpty()) {
            return out;
        }
        final Map<ResourceLocation, Float> map = stack.getItemHolder().getData(ITEMS);
        if (map == null) {
            return out;
        }
        for (final Kind kind : all()) {
            final Float strength = map.get(kind.id());
            if (strength != null && strength > 0) {
                out.put(kind, (double) strength);
            }
        }
        return out;
    }

    /** How strong {@code kind} is in what {@code carrier} carries, all stacks added up by how many of each. */
    public static double carried(final Player carrier, final Kind kind) {
        double total = 0;
        for (final ItemStack stack : carrier.getInventory().items) {
            total += of(stack).getOrDefault(kind, 0.0) * stack.getCount();
        }
        for (final ItemStack stack : carrier.getInventory().offhand) {
            total += of(stack).getOrDefault(kind, 0.0) * stack.getCount();
        }
        return total;
    }

    /**
     * How strong {@code kind} is in what {@code carrier} carries, what containers hold too, as a shulker box or a
     * bundle holds it: what a container holds counts by the share {@code passes} lets through its walls, and all of it
     * through a container that does not shield ({@link Shielding}). What reaches a body through walls, as radiation.
     */
    public static double carriedWithin(final Player carrier, final Kind kind,
                                       final ToDoubleFunction<Shielding.Wall> passes) {
        double total = 0;
        for (final List<ItemStack> stacks : List.of(carrier.getInventory().items, carrier.getInventory().offhand)) {
            for (final ItemStack stack : stacks) {
                total += of(stack).getOrDefault(kind, 0.0) * stack.getCount();
                final List<ItemStack> held = Shielding.contents(stack);
                if (held.isEmpty()) {
                    continue;
                }
                final double share = Shielding.of(stack).map(passes::applyAsDouble).orElse(1.0);
                for (final ItemStack inside : held) {
                    total += of(inside).getOrDefault(kind, 0.0) * inside.getCount() * share * stack.getCount();
                }
            }
        }
        return total;
    }

    /** A second of everything {@code carrier} carries. */
    public static void second(final Player carrier) {
        for (final ItemStack stack : List.copyOf(carrier.getInventory().items)) {
            secondOf(carrier, stack);
        }
        for (final ItemStack stack : List.copyOf(carrier.getInventory().offhand)) {
            secondOf(carrier, stack);
        }
    }

    private static void secondOf(final Player carrier, final ItemStack stack) {
        for (final Map.Entry<Kind, Double> hazard : of(stack).entrySet()) {
            final Kind kind = hazard.getKey();
            final double stopped = kind.protection() == null ? 0 : Resistances.total(carrier, kind.protection());
            kind.effect().second(carrier, stack, hazard.getValue(), stopped);
        }
    }

    private static void onPlayerTick(final PlayerTickEvent.Post event) {
        if (event.getEntity().level() instanceof ServerLevel && HazardBalance.itemHazards()
                && !event.getEntity().isCreative() && !event.getEntity().isSpectator()
                && event.getEntity().tickCount % TICKS_A_SECOND == 0) {
            second(event.getEntity());
        }
    }

    /* What is unstable goes off when its carrier burns, is caught in a blast or falls hard. */
    private static void onHurt(final LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof Player carrier) || !(carrier.level() instanceof ServerLevel)
                || !HazardBalance.itemHazards()) {
            return;
        }
        final DamageSource source = event.getSource();
        final boolean jolted = source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.IS_EXPLOSION)
                || source.is(DamageTypeTags.IS_FALL) && event.getNewDamage() >= HARD_KNOCK;
        if (!jolted) {
            return;
        }
        for (final ItemStack stack : List.copyOf(carrier.getInventory().items)) {
            final Double strength = of(stack).get(UNSTABLE);
            if (strength != null) {
                goOff(carrier, stack, strength);
            }
        }
    }

    /* The stack goes off where its carrier stands, and is gone: the more of it, the bigger the blast. */
    private static void goOff(final Player carrier, final ItemStack stack, final double strength) {
        final float power = (float) Math.min(8, strength * Math.cbrt(stack.getCount()));
        stack.setCount(0);
        carrier.level().explode(null, carrier.getX(), carrier.getY(0.5), carrier.getZ(), power,
                Level.ExplosionInteraction.TNT);
    }

    private static DamageSource source(final Level level, final ResourceKey<DamageType> type) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type));
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsCore.MODID, path);
    }

    /** What a hazard of a carried thing does, once a second while it is carried. */
    @FunctionalInterface
    public interface IEffect {

        /**
         * A second of {@code stack} carried by {@code carrier}, its hazard {@code strength} strong, {@code stopped} of
         * it stopped by what the carrier wears.
         */
        void second(Player carrier, ItemStack stack, double strength, double stopped);
    }

    /**
     * A kind of hazard a carried thing has.
     *
     * @param id         its id
     * @param name       what the tooltip calls it
     * @param guard      what the tooltip says protects from it
     * @param protection the kind of harm worn things are protected from it by, if any
     * @param effect     what it does, once a second
     */
    public record Kind(ResourceLocation id, TextKey name, TextKey guard, @Nullable DamageKind protection,
                       IEffect effect) {
    }
}
