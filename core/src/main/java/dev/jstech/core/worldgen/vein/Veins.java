/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.vein;

import com.mojang.serialization.MapCodec;
import dev.jstech.core.JsCore;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.SplittableRandom;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * The veins the world's ores lie in: the kinds of vein the server lays ({@link Deposits}), where each vein of them lies
 * ({@link VeinField}) as the server's settings make them ({@link VeinBalance}), and whether a vein is found where it
 * lies, by the biome at its middle. Read the same by the world's generation and by prospecting, so what an instrument
 * finds is what is there.
 */
public final class Veins {

    private static final DeferredRegister<PlacementModifierType<?>> PLACEMENTS = DeferredRegister.create(
            Registries.PLACEMENT_MODIFIER_TYPE, JsCore.MODID);
    private static final DeferredRegister<MapCodec<? extends BiomeModifier>> MODIFIERS = DeferredRegister.create(
            NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, JsCore.MODID);
    private static final long FOUND_SALT = 2_862_933_555_777_941_757L;
    private static final double TO_UNIT = 1.0 / (1L << 53);

    /** {@code jscore:scattered_ores}: see {@link ScatteredOres}. */
    public static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<ScatteredOres>> SCATTERED_ORES =
            PLACEMENTS.register("scattered_ores", () -> () -> ScatteredOres.CODEC);
    /** {@code jscore:veins}: see {@link VeinsBiomeModifier}. */
    public static final DeferredHolder<MapCodec<? extends BiomeModifier>, MapCodec<VeinsBiomeModifier>> BIOME_MODIFIER =
            MODIFIERS.register("veins", () -> VeinsBiomeModifier.CODEC);

    private Veins() {
    }

    /** Registers the placement and the biome modifier, and forgets the server's biomes as it stops, once. */
    public static void listen(final IEventBus modBus) {
        PLACEMENTS.register(modBus);
        MODIFIERS.register(modBus);
        NeoForge.EVENT_BUS.addListener(ServerStoppedEvent.class, event -> {
            MovedOres.forget();
            VeinsBiomeModifier.forget();
        });
    }

    /** Where the veins of {@code kinds} lie, as the server's settings make them. */
    public static VeinField field(final List<Deposits.Entry> kinds) {
        final List<VeinField.Kind> placed = new ArrayList<>(kinds.size());
        for (final Deposits.Entry kind : kinds) {
            placed.add(kind.type().kind(VeinBalance.rarity(kind.id()), VeinBalance.spread(kind.id())));
        }
        return new VeinField(placed);
    }

    /** How much of a vein of {@code kind} is ore at its middle, as the server's settings make it. */
    public static double density(final Deposits.Entry kind) {
        return Math.min(1, kind.type().density() * VeinBalance.density(kind.id()));
    }

    /**
     * Whether {@code vein} of {@code kind} is found where it lies: always in its biomes, or with none named; elsewhere
     * only as often as the kind says. Worked out from the world's biomes as they are laid out, chunk made or not.
     */
    public static boolean found(final ChunkGenerator generator, final RandomState random, final Deposits.Entry kind,
                                final VeinField.Vein vein) {
        return found(generator, random, kind.type().biomes(), kind.type().elsewhere(), vein);
    }

    /**
     * Whether a body of the ground lies where the field put it: always where it has no {@code biomes}, or where the
     * biome at its middle is one of them; elsewhere {@code elsewhere} as often. The same for any body the field
     * places (veins, reservoirs).
     */
    public static boolean found(final ChunkGenerator generator, final RandomState random,
                                final Optional<TagKey<Biome>> biomes, final double elsewhere,
                                final VeinField.Vein body) {
        if (biomes.isEmpty()) {
            return true;
        }
        final Holder<Biome> middle = generator.getBiomeSource().getNoiseBiome(QuartPos.fromBlock(body.x()),
                QuartPos.fromBlock(body.y()), QuartPos.fromBlock(body.z()), random.sampler());
        if (middle.is(biomes.get())) {
            return true;
        }
        return elsewhere > 0 && (new SplittableRandom(body.seed() ^ FOUND_SALT).nextLong() >>> 11) * TO_UNIT
                < elsewhere;
    }
}
