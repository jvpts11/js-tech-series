/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.hazard;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.JsCore;
import dev.jstech.core.dimension.Climate;
import dev.jstech.core.dimension.DimensionHazards;
import dev.jstech.core.explosion.Explosions;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialProperties;
import dev.jstech.core.material.MaterialValues;
import dev.jstech.core.material.Materials;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.room.SealedRooms;
import dev.jstech.core.state.CoreState;
import dev.jstech.core.state.CoreStates;
import dev.jstech.core.state.DimensionState;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.util.Loaded;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.joml.Vector3f;

/**
 * Gas let out into the open (a leak, a vent, a burst tank): a cloud where it came out, which spreads and thins until it
 * is gone, doing to whoever stands in it what its material's hazards say ({@link GasHazard}), by how much of the air it
 * is. A toxic or corrosive gas poisons from its share of what is immediately dangerous ({@link GasLimits#idlh}), which
 * a gas mask stops; an asphyxiant takes the place of the air until there is too little oxygen to breathe; a flammable
 * gas between its explosive limits explodes from a flame, lava or a fire in it. In a sealed room the gas goes into the
 * room's air instead, which poisons in the same way.
 *
 * <pre>{@code
 * GasClouds.release(level, pos, chlorine, 40);   // forty moles of chlorine let out where a pipe broke
 * double ppm = GasClouds.shareAt(level, pos, chlorine) * 1_000_000;
 * }</pre>
 */
@TextHolder
public final class GasClouds {

    public static final TextKey POISONING_NAME = TextKey.of("jscore.hazard.gas_poisoning", "Poisonous gas");
    public static final TextKey POISONING_UNIT = TextKey.of("jscore.hazard.gas_poisoning.unit",
            "seconds of danger");
    public static final TextKey IRRITATED = TextKey.of("jscore.hazard.gas_poisoning.irritated", "Coughing");
    public static final TextKey POISONED = TextKey.of("jscore.hazard.gas_poisoning.poisoned", "Poisoned");
    /** How often clouds spread and act, in ticks. */
    public static final int EVERY = 20;
    /** The moles of air in a cubic metre, a block, at the open air's pressure and 20 °C: n / V = P / RT. */
    public static final double AIR_MOLES = 41.6;
    /** How many blocks a cloud's radius grows a second, and the widest it grows. */
    public static final double GROWS = 0.5;
    public static final double WIDEST = 12;
    /** The share of a cloud at its widest the wind takes away each second. */
    public static final double FADES = 0.2;
    /** Below a part per million of the air, a cloud is gone. */
    public static final double GONE = 0.000_001;
    /** The heat of burning a mole of a fuel gas, in joules: hydrogen's 242 kJ, carbon monoxide's 283. */
    public static final double COMBUSTION = 250_000;
    /** The oxygen in the air, as a share, and the least pressure of it a body breathes, in kilopascals. */
    public static final double AIR_OXYGEN = 0.2095;
    public static final double BREATHES_FROM = 16;
    /** The pressure of the open air, in kilopascals. */
    public static final double OPEN_AIR = 101.325;
    /** Every cloud of each dimension, kept with the world. */
    public static final DimensionState<Book> STATE = CoreState.builder(ResourceLocation.fromNamespaceAndPath(
            JsCore.MODID, "gas_clouds"), Book.CODEC, Book.EMPTY).dimension();
    /**
     * A toxic or corrosive gas breathed, counted in seconds of breathing what is immediately dangerous: each second
     * adds the air's share of the gas over its dangerous share, which a gas mask stops. It coughs from a second of it;
     * from thirty it poisons, half a minute to get out of air as bad as the danger mark, a few seconds of air ten times
     * worse.
     */
    public static final HazardKind POISONING = Hazards.declare(HazardKind.builder(
                    ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "gas_poisoning"), POISONING_NAME,
                    POISONING_UNIT)
            .band(1, IRRITATED, HazardEffects.effect(MobEffects.CONFUSION, 0))
            .band(30, POISONED, HazardEffects.effect(MobEffects.POISON, 1), HazardEffects.damage(1))
            .recoversShare(1).protectedBy(DamageKinds.INHALED).affectsMobs().build());
    /* How far into a cloud a flame is looked for, in blocks. */
    private static final int FLAMES_WITHIN = 6;
    private static final double SPHERE = 4.0 / 3.0 * Math.PI;
    private static final int PUFFS_A_BLOCK = 2;
    private static final int MOST_PUFFS = 60;
    private static final float PUFF_SIZE = 1.5F;
    private static final int HEX = 16;
    private static final int RED = 16;
    private static final int GREEN = 8;
    private static final int CHANNEL = 255;
    private static final double PARTS = 1_000_000;

    private GasClouds() {
    }

    /** Declares the state and gives the hazards their sources, from the Core's constructor. */
    public static void declare() {
        CoreStates.register(STATE);
        POISONING.addSource(GasClouds::poisoning);
        DimensionHazards.AIR.addSource(GasClouds::smothering);
    }

    /** Hands the game's bus what this answers, once, from the Core's constructor. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(LevelTickEvent.Post.class, event -> {
            if (event.getLevel() instanceof ServerLevel level && level.getGameTime() % EVERY == 0) {
                tick(level);
            }
        });
    }

    /**
     * Lets {@code moles} of {@code gas} out at {@code at}: into the sealed room there, or as a cloud, joined to a
     * cloud of it already there. Nothing of a material that is never a gas.
     */
    public static void release(final ServerLevel level, final BlockPos at, final Material gas, final double moles) {
        if (moles <= 0 || !gas.takes(MatterState.GAS)) {
            return;
        }
        if (SealedRooms.give(level, at, gas.id(), moles) > 0) {
            return;
        }
        final Vec3 centre = Vec3.atCenterOf(at);
        STATE.update(level, book -> {
            final List<Cloud> clouds = new ArrayList<>(book.clouds());
            for (int i = 0; i < clouds.size(); i++) {
                final Cloud cloud = clouds.get(i);
                if (cloud.gas().equals(gas.id()) && cloud.centre().distanceTo(centre) <= cloud.radius()) {
                    clouds.set(i, cloud.withMoles(cloud.moles() + moles));
                    return new Book(clouds);
                }
            }
            clouds.add(new Cloud(gas.id(), centre.x, centre.y, centre.z, moles, 1));
            return new Book(clouds);
        });
    }

    /** The share of the air at {@code at} that {@code gas} is, in its clouds or in the sealed room there. */
    public static double shareAt(final ServerLevel level, final BlockPos at, final Material gas) {
        final Vec3 where = Vec3.atCenterOf(at);
        double share = 0;
        for (final Cloud cloud : STATE.get(level).clouds()) {
            if (cloud.gas().equals(gas.id()) && cloud.centre().distanceTo(where) <= cloud.radius()) {
                share += cloud.share();
            }
        }
        final Climate climate = SealedRooms.climateAt(level, at);
        return share + climate.air().map(air -> air.shares().getOrDefault(gas.id(), 0.0)).orElse(0.0);
    }

    /** The clouds of {@code level}. */
    public static List<Cloud> of(final ServerLevel level) {
        return STATE.get(level).clouds();
    }

    /* A second of every cloud: it spreads, the wind thins the widest, a flame sets off the explosive, and it shows. */
    private static void tick(final ServerLevel level) {
        final List<Cloud> before = STATE.get(level).clouds();
        if (before.isEmpty()) {
            return;
        }
        final List<Cloud> after = new ArrayList<>(before.size());
        for (final Cloud cloud : before) {
            final Optional<Material> gas = Materials.byId(cloud.gas());
            if (gas.isEmpty()) {
                continue;
            }
            // A flame sets it off as it stands, before the second's spreading thins it.
            if (explodes(level, cloud, gas.get())) {
                Explosions.blast(level, cloud.centre(), gas.get().id(), cloud.moles() * COMBUSTION).detonate();
                continue;
            }
            final Cloud spread = cloud.radius() >= WIDEST ? cloud.withMoles(cloud.moles() * (1 - FADES))
                    : cloud.withRadius(Math.min(WIDEST, cloud.radius() + GROWS));
            if (spread.share() < GONE) {
                continue;
            }
            show(level, spread, gas.get());
            after.add(spread);
        }
        STATE.set(level, new Book(after));
    }

    /* Whether {@code cloud} of a flammable {@code gas} lies between its explosive limits with a flame in it. */
    private static boolean explodes(final ServerLevel level, final Cloud cloud, final Material gas) {
        if (!MaterialHazards.gasHazards(gas).contains(GasHazard.FLAMMABLE)) {
            return false;
        }
        final OptionalDouble lower = GasLimits.lowerExplosive(gas);
        final OptionalDouble upper = GasLimits.upperExplosive(gas);
        if (lower.isEmpty() || upper.isEmpty() || cloud.share() < lower.getAsDouble()
                || cloud.share() > upper.getAsDouble()) {
            return false;
        }
        final BlockPos middle = BlockPos.containing(cloud.centre());
        final int reach = (int) Math.min(FLAMES_WITHIN, Math.ceil(cloud.radius()));
        for (final BlockPos at : BlockPos.betweenClosed(middle.offset(-reach, -reach, -reach),
                middle.offset(reach, reach, reach))) {
            if (at.distSqr(middle) <= (double) reach * reach && Loaded.at(level, at)
                    && flame(level.getBlockState(at))) {
                return true;
            }
        }
        return !level.getEntitiesOfClass(LivingEntity.class, cloud.box(), body -> body.isOnFire()
                && body.position().distanceTo(cloud.centre()) <= cloud.radius()).isEmpty();
    }

    /* Whether {@code state} lights a gas: a fire, lava, a lit campfire. */
    private static boolean flame(final BlockState state) {
        return state.is(BlockTags.FIRE) || state.getFluidState().is(Fluids.LAVA) || state.getFluidState()
                .is(Fluids.FLOWING_LAVA) || state.getBlock() instanceof CampfireBlock && state.getValue(
                CampfireBlock.LIT);
    }

    /* The cloud drawn as puffs of its gas's colour, or as plain cloud for a gas with none, more the wider it is. */
    private static void show(final ServerLevel level, final Cloud cloud, final Material gas) {
        final ParticleOptions puff = MaterialValues.value(gas, MaterialProperties.COLOUR, false)
                .<ParticleOptions>map(colour -> {
                    final int rgb = Integer.parseInt(colour.substring(1), HEX);
                    return new DustParticleOptions(new Vector3f(channel(rgb, RED), channel(rgb, GREEN),
                            channel(rgb, 0)), PUFF_SIZE);
                }).orElse(ParticleTypes.CLOUD);
        final int puffs = (int) Math.min(MOST_PUFFS, Math.ceil(cloud.radius() * PUFFS_A_BLOCK));
        final double spread = cloud.radius() / 2;
        level.sendParticles(puff, cloud.x(), cloud.y(), cloud.z(), puffs, spread, spread, spread, 0);
    }

    /* A channel of {@code rgb}, shifted down by {@code shift} bits, from 0 to 1. */
    private static float channel(final int rgb, final int shift) {
        return ((rgb >> shift) & CHANNEL) / (float) CHANNEL;
    }

    /* How poisonous the air {@code body} breathes is: each toxic or corrosive gas's share over what is dangerous. */
    private static double poisoning(final LivingEntity body, final ServerLevel level) {
        double rate = 0;
        final Vec3 at = body.getEyePosition();
        for (final Cloud cloud : STATE.get(level).clouds()) {
            if (cloud.centre().distanceTo(at) <= cloud.radius()) {
                rate += Materials.byId(cloud.gas()).map(gas -> poisons(gas, cloud.share())).orElse(0.0);
            }
        }
        final Climate climate = SealedRooms.climateAt(level, BlockPos.containing(at));
        if (climate.air().isPresent()) {
            for (final Map.Entry<ResourceLocation, Double> share : climate.air().get().shares().entrySet()) {
                rate += Materials.byId(share.getKey()).map(gas -> poisons(gas, share.getValue())).orElse(0.0);
            }
        }
        return rate;
    }

    /* How many times what is dangerous {@code share} of {@code gas} is; none for a gas that does not poison. */
    private static double poisons(final Material gas, final double share) {
        final Set<GasHazard> hazards = MaterialHazards.gasHazards(gas);
        if (!hazards.contains(GasHazard.TOXIC) && !hazards.contains(GasHazard.CORROSIVE)) {
            return 0;
        }
        return share * PARTS / GasLimits.idlh(gas);
    }

    /* Whether an asphyxiant's cloud has left {@code body} too little oxygen to breathe: 1 then, 0 otherwise. */
    private static double smothering(final LivingEntity body, final ServerLevel level) {
        final Vec3 at = body.getEyePosition();
        double displaced = 0;
        for (final Cloud cloud : STATE.get(level).clouds()) {
            if (cloud.centre().distanceTo(at) <= cloud.radius() && Materials.byId(cloud.gas())
                    .map(gas -> MaterialHazards.gasHazards(gas).contains(GasHazard.ASPHYXIANT)).orElse(false)) {
                displaced += cloud.share();
            }
        }
        final double oxygen = AIR_OXYGEN * Math.max(0, 1 - displaced) * OPEN_AIR;
        return displaced > 0 && oxygen < BREATHES_FROM ? 1 : 0;
    }

    /**
     * A cloud of gas.
     *
     * @param gas    its material's id
     * @param x      its middle
     * @param y      its middle
     * @param z      its middle
     * @param moles  the gas in it
     * @param radius how far from its middle it reaches, in blocks
     */
    public record Cloud(ResourceLocation gas, double x, double y, double z, double moles, double radius) {

        public static final Codec<Cloud> CODEC = RecordCodecBuilder.create(cloud -> cloud.group(
                ResourceLocation.CODEC.fieldOf("gas").forGetter(Cloud::gas),
                Codec.DOUBLE.fieldOf("x").forGetter(Cloud::x),
                Codec.DOUBLE.fieldOf("y").forGetter(Cloud::y),
                Codec.DOUBLE.fieldOf("z").forGetter(Cloud::z),
                Codec.DOUBLE.fieldOf("moles").forGetter(Cloud::moles),
                Codec.DOUBLE.fieldOf("radius").forGetter(Cloud::radius)
        ).apply(cloud, Cloud::new));

        /** Its middle. */
        public Vec3 centre() {
            return new Vec3(this.x, this.y, this.z);
        }

        /** The share of the air in it that is its gas, spread evenly through it. */
        public double share() {
            final double volume = SPHERE * this.radius * this.radius * this.radius;
            return Math.min(1, this.moles / (AIR_MOLES * volume));
        }

        /** The box it fits in. */
        public AABB box() {
            return new AABB(centre(), centre()).inflate(this.radius);
        }

        /** The same, holding {@code to} moles. */
        public Cloud withMoles(final double to) {
            return new Cloud(this.gas, this.x, this.y, this.z, to, this.radius);
        }

        /** The same, reaching {@code to} blocks. */
        public Cloud withRadius(final double to) {
            return new Cloud(this.gas, this.x, this.y, this.z, this.moles, to);
        }
    }

    /**
     * A dimension's clouds.
     *
     * @param clouds every cloud in it
     */
    public record Book(List<Cloud> clouds) {

        public static final Book EMPTY = new Book(List.of());
        public static final Codec<Book> CODEC = RecordCodecBuilder.create(book -> book.group(
                Cloud.CODEC.listOf().fieldOf("clouds").forGetter(Book::clouds)
        ).apply(book, Book::new));

        public Book {
            clouds = List.copyOf(clouds);
        }
    }
}
