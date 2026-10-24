/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.line;

import dev.jstech.core.cable.BreachRules;
import dev.jstech.core.cable.IBreachRule;
import dev.jstech.core.cable.LineBreach;
import dev.jstech.core.fluid.CoreFluidTags;
import dev.jstech.core.fluid.PipeMarks;
import dev.jstech.core.hazard.GasClouds;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.Materials;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.matter.MatterBridges;
import dev.jstech.core.matter.MatterRelease;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.core.matter.PressureBursts;
import dev.jstech.industrial.config.IndustrialServerConfig;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * What befalls a pipe of J's Industrial past what it is made for, and what leaks out of it
 * (industrial/docs/design/fluids-and-gases.md). Too hot, it softens and bursts; too cold, it turns brittle and cracks;
 * each after {@link #SOFTENS_IN}. A corrosive fluid or gas it is not made for eats through it in about a minute
 * ({@link #CORRODES_IN}), and it falls as its scrap. Past the pressure it holds it bursts at once, a blast of what it
 * held; worn through by what it carried (hydrogen in plain steel, a slurry), it breaks and falls as its scrap. Any
 * other mark it is not made for (ultra-pure water, a gas in a liquid pipe) is only refused. A broken pipe leaks
 * {@link #LEAKS_FOR} seconds of its flow: a liquid spills, a slurry splashes out, a gas comes out as a cloud of
 * {@link GasClouds}.
 *
 * <p>Each is a setting of the server's, on as it comes ({@link IndustrialServerConfig}): pipes that do not fail only
 * refuse, leaks that are not hazardous only lose what the pipe carried, and hydrogen that does not embrittle runs in
 * any pipe that holds it.
 */
public final class PipeFailures {

    /** How long a pipe stands a fluid too hot or too cold for it, in ticks: five seconds. */
    public static final int SOFTENS_IN = 100;
    /** How long a pipe stands a corrosive fluid or gas it is not made for, in ticks: about a minute. */
    public static final int CORRODES_IN = 1_200;
    /** How long a pipe not lined against it stands a slurry, in ticks: ten minutes, first numbers. */
    public static final int ABRADES_IN = 12_000;
    /** How long plain steel stands hydrogen, in ticks: an hour, first numbers. */
    public static final int EMBRITTLES_IN = 72_000;
    /** How many seconds of a broken pipe's flow leak out of it. */
    public static final int LEAKS_FOR = 5;
    /** The millibuckets of a gas at the open air's pressure a mole is. */
    public static final double MILLIBUCKETS_A_MOLE = 22.4;
    /* The marks that eat a pipe not made for them; the others are only refused. */
    private static final List<TagKey<Fluid>> CORRODING = List.of(CoreFluidTags.CORROSIVE, IndustrialPipeMarks.ACIDS,
            IndustrialPipeMarks.CORROSIVE_GASES);
    private static final int TICKS_A_SECOND = 20;

    private PipeFailures() {
    }

    /**
     * The rule of a pipe of J's Industrial ({@code CableType.Builder.beyond}), which falls as {@code scrap} when it is
     * corroded or worn through, and holds {@code innerVolume} millibuckets of what it carries in each block, which a
     * burst lets out as a blast.
     */
    public static IBreachRule rule(final Supplier<ItemStack> scrap, final long innerVolume) {
        Objects.requireNonNull(scrap, "scrap");
        final IBreachRule softens = BreachRules.breaksAfter(SOFTENS_IN, PipeFailures::leak);
        final IBreachRule corrodes = BreachRules.breaksAfter(CORRODES_IN, (level, breach) -> {
            Block.popResource(level, breach.at(), scrap.get());
            leak(level, breach);
        });
        final IBreachRule bursts = BreachRules.breaksAfter(1, (level, breach) -> {
            PressureBursts.pipe(innerVolume).broken(level, breach);
            leak(level, breach);
        });
        final IBreachRule wornThrough = BreachRules.breaksAfter(1, (level, breach) -> {
            Block.popResource(level, breach.at(), scrap.get());
            leak(level, breach);
        });
        return (level, breach, gameTime) -> {
            if (!IndustrialServerConfig.pipesFail()) {
                return;
            }
            switch (breach.reason()) {
                case TOO_HOT, TOO_COLD -> softens.exposed(level, breach, gameTime);
                case NOT_MADE_FOR -> {
                    if (corrodes(breach.what())) {
                        corrodes.exposed(level, breach, gameTime);
                    }
                }
                case OVERPRESSURE -> bursts.exposed(level, breach, gameTime);
                case WORN_OUT -> {
                    if (IndustrialServerConfig.hydrogenEmbrittles() || !has(IndustrialPipeMarks.HYDROGEN,
                            breach.what())) {
                        wornThrough.exposed(level, breach, gameTime);
                    }
                }
            }
        };
    }

    /**
     * What leaks out of the broken pipe {@code breach} reached, unless leaks are not hazardous: a liquid spills, a gas
     * comes out as a cloud of {@link #LEAKS_FOR} seconds of the pipe's flow.
     */
    public static void leak(final ServerLevel level, final LineBreach breach) {
        if (!IndustrialServerConfig.leaksHazardous()) {
            return;
        }
        final long millibuckets = breach.type().throughput() * TICKS_A_SECOND * LEAKS_FOR;
        final Optional<Material> gas = gasOf(breach.what());
        if (gas.isPresent()) {
            GasClouds.release(level, breach.at(), gas.get(), millibuckets / MILLIBUCKETS_A_MOLE);
            return;
        }
        final Optional<Material> slurry = materialOf(breach.what()).filter(material -> material.takes(
                MatterState.SLURRY));
        if (slurry.isPresent()) {
            MatterRelease.release(level, breach.at(), MatterState.SLURRY, new MatterStack(slurry.get(),
                    millibuckets));
            return;
        }
        BreachRules.spill(level, breach.at(), breach.what());
    }

    /* Whether {@code what} has a mark that eats a pipe not made for it. */
    private static boolean corrodes(final String what) {
        for (final TagKey<Fluid> mark : CORRODING) {
            if (has(mark, what)) {
                return true;
            }
        }
        return false;
    }

    /* Whether {@code what}, a fluid's or a material's id, has {@code mark}. */
    private static boolean has(final TagKey<Fluid> mark, final String what) {
        final Optional<PipeMarks.Mark> found = PipeMarks.of(mark);
        if (found.isEmpty()) {
            return false;
        }
        final Optional<Fluid> fluid = fluidOf(what);
        if (fluid.isPresent()) {
            return found.get().on(fluid.get());
        }
        return materialOf(what).map(material -> found.get().on(material)).orElse(false);
    }

    /* The gas {@code what} is: a material that is one, or a fluid marked as a gas whose material is known. */
    private static Optional<Material> gasOf(final String what) {
        final Optional<Fluid> fluid = fluidOf(what);
        if (fluid.isPresent()) {
            if (!fluid.get().defaultFluidState().is(CoreFluidTags.GASES)) {
                return Optional.empty();
            }
            return MatterBridges.materialOfFluid(ResourceLocation.parse(what));
        }
        return materialOf(what).filter(material -> material.takes(MatterState.GAS));
    }

    private static Optional<Fluid> fluidOf(final String what) {
        final ResourceLocation id = ResourceLocation.tryParse(what);
        return id == null ? Optional.empty() : BuiltInRegistries.FLUID.getOptional(id)
                .filter(fluid -> fluid != Fluids.EMPTY);
    }

    private static Optional<Material> materialOf(final String what) {
        final ResourceLocation id = ResourceLocation.tryParse(what);
        return id == null ? Optional.empty() : Materials.byId(id);
    }
}
