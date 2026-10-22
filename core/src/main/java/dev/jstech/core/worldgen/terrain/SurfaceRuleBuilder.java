/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.terrain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.placement.CaveSurface;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

/**
 * The surface of a terrain as rules read in order, the first that holds for a block laying it: bedrock at the bottom,
 * a cap on the peaks, bare rock on the cliffs, a floor under the seas, the layers of each biome and of the rest. What
 * it builds is the game's own surface rule, written into the terrain's noise settings by the data generation; the
 * blocks are looked up only then.
 *
 * <pre>{@code
 * Terrains.surfaceRules().bedrock(id("my_land_bedrock"), 5)
 *         .above(140, () -> Blocks.SNOW_BLOCK)
 *         .steep(() -> Blocks.STONE)
 *         .in(MY_DESERT, Terrains.surfaceRules().layers(() -> Blocks.SAND, () -> Blocks.SANDSTONE, 4))
 *         .layers(() -> Blocks.GRASS_BLOCK, () -> Blocks.DIRT, 3);
 * }</pre>
 */
public final class SurfaceRuleBuilder {

    private final List<Supplier<SurfaceRules.RuleSource>> rules = new ArrayList<>();

    SurfaceRuleBuilder() {
    }

    /**
     * Bedrock {@code blocks} deep at the very bottom, fading upwards; {@code name} is what the game seeds its fading
     * by, the terrain's own so two terrains fade apart.
     */
    public SurfaceRuleBuilder bedrock(final String name, final int blocks) {
        Objects.requireNonNull(name, "name");
        this.rules.add(() -> SurfaceRules.ifTrue(SurfaceRules.verticalGradient(name, VerticalAnchor.bottom(),
                VerticalAnchor.aboveBottom(blocks)), state(() -> Blocks.BEDROCK)));
        return this;
    }

    /** At {@code y} and above, the floor open to the air is {@code block}: snow on the peaks. */
    public SurfaceRuleBuilder above(final int y, final Supplier<? extends Block> block) {
        this.rules.add(() -> SurfaceRules.ifTrue(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(y), 0),
                SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.ifTrue(SurfaceRules.waterBlockCheck(-1, 0),
                        state(block)))));
        return this;
    }

    /** Where the ground is too steep for its top, its floor is {@code block}: bare rock on a cliff. */
    public SurfaceRuleBuilder steep(final Supplier<? extends Block> block) {
        this.rules.add(() -> SurfaceRules.ifTrue(SurfaceRules.steep(), SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR,
                state(block))));
        return this;
    }

    /** Under a liquid, the floor is {@code block}: the bed of a sea or a lake. */
    public SurfaceRuleBuilder underLiquid(final Supplier<? extends Block> block) {
        this.rules.add(() -> SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.ifTrue(
                SurfaceRules.not(SurfaceRules.waterBlockCheck(-1, 0)), state(block))));
        return this;
    }

    /**
     * The layers of the ground: {@code top} where the floor meets the air, and {@code under} below it, {@code depth}
     * blocks deep, and on a floor under a liquid.
     */
    public SurfaceRuleBuilder layers(final Supplier<? extends Block> top, final Supplier<? extends Block> under,
                                     final int depth) {
        this.rules.add(() -> SurfaceRules.sequence(
                SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.sequence(
                        SurfaceRules.ifTrue(SurfaceRules.waterBlockCheck(-1, 0), state(top)), state(under))),
                SurfaceRules.ifTrue(SurfaceRules.stoneDepthCheck(depth, false, CaveSurface.FLOOR), state(under))));
        return this;
    }

    /** In {@code biome}, the rules of {@code inside}. */
    public SurfaceRuleBuilder in(final ResourceKey<Biome> biome, final SurfaceRuleBuilder inside) {
        Objects.requireNonNull(biome, "biome");
        this.rules.add(() -> SurfaceRules.ifTrue(SurfaceRules.isBiome(biome), inside.build()));
        return this;
    }

    /** Where {@code noise} reads from {@code from} to {@code to}, the rules of {@code inside}: patches of a surface. */
    public SurfaceRuleBuilder where(final ResourceKey<NormalNoise.NoiseParameters> noise, final double from,
                                    final double to, final SurfaceRuleBuilder inside) {
        Objects.requireNonNull(noise, "noise");
        this.rules.add(() -> SurfaceRules.ifTrue(SurfaceRules.noiseCondition(noise, from, to), inside.build()));
        return this;
    }

    /** Any rule of the game's own, in its turn among the rest. */
    public SurfaceRuleBuilder rule(final Supplier<SurfaceRules.RuleSource> rule) {
        this.rules.add(Objects.requireNonNull(rule, "rule"));
        return this;
    }

    /** The rules as the game's surface rule, read in the order they were given. */
    public SurfaceRules.RuleSource build() {
        return SurfaceRules.sequence(this.rules.stream().map(Supplier::get).toArray(SurfaceRules.RuleSource[]::new));
    }

    private static SurfaceRules.RuleSource state(final Supplier<? extends Block> block) {
        return SurfaceRules.state(block.get().defaultBlockState());
    }
}
