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
 *
 * <p>The game reads the rule for every block of a column, deep rock included. The rules are read only as deep under the
 * floor as they lay anything, one test of the depth for the rock below; and a biome's or a noise's test, which costs
 * far more than a depth's, is made only as deep as the rules {@link #in} that biome or {@link #where} that noise lay
 * anything. The surface comes out the same, made many times faster.
 */
public final class SurfaceRuleBuilder {

    private final List<Supplier<SurfaceRules.RuleSource>> rules = new ArrayList<>();
    /* How deep under the floor the rules given lay anything at most, in blocks; with one laying anywhere, no bound. */
    private int deepest;
    private boolean anywhere;

    SurfaceRuleBuilder() {
    }

    /**
     * Bedrock {@code blocks} deep at the very bottom, fading upwards; {@code name} is what the game seeds its fading
     * by, the terrain's own so two terrains fade apart.
     */
    public SurfaceRuleBuilder bedrock(final String name, final int blocks) {
        Objects.requireNonNull(name, "name");
        this.anywhere = true;
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
        this.deepest = Math.max(this.deepest, depth);
        this.rules.add(() -> SurfaceRules.sequence(
                SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.sequence(
                        SurfaceRules.ifTrue(SurfaceRules.waterBlockCheck(-1, 0), state(top)), state(under))),
                SurfaceRules.ifTrue(SurfaceRules.stoneDepthCheck(depth, false, CaveSurface.FLOOR), state(under))));
        return this;
    }

    /** In {@code biome}, the rules of {@code inside}, given whole; the column's biome read once, near its floor. */
    public SurfaceRuleBuilder in(final ResourceKey<Biome> biome, final SurfaceRuleBuilder inside) {
        Objects.requireNonNull(biome, "biome");
        reach(inside);
        this.rules.add(() -> inside.underTheFloor(ColumnBiomeCondition.of(biome)));
        return this;
    }

    /**
     * Where {@code noise} reads from {@code from} to {@code to}, the rules of {@code inside}, given whole: patches of a
     * surface.
     */
    public SurfaceRuleBuilder where(final ResourceKey<NormalNoise.NoiseParameters> noise, final double from,
                                    final double to, final SurfaceRuleBuilder inside) {
        Objects.requireNonNull(noise, "noise");
        reach(inside);
        this.rules.add(() -> inside.underTheFloor(SurfaceRules.noiseCondition(noise, from, to)));
        return this;
    }

    /** Any rule of the game's own, in its turn among the rest; it may lay blocks anywhere. */
    public SurfaceRuleBuilder rule(final Supplier<SurfaceRules.RuleSource> rule) {
        this.anywhere = true;
        this.rules.add(Objects.requireNonNull(rule, "rule"));
        return this;
    }

    /**
     * The rules as the game's surface rule, read in the order they were given; read only as deep under the floor as
     * they lay anything.
     */
    public SurfaceRules.RuleSource build() {
        return this.anywhere ? sequence() : SurfaceRules.ifTrue(SurfaceRules.stoneDepthCheck(this.deepest, false,
                CaveSurface.FLOOR), sequence());
    }

    /* As deep as the rules of {@code inside} lay anything, so do these. */
    private void reach(final SurfaceRuleBuilder inside) {
        this.anywhere |= inside.anywhere;
        this.deepest = Math.max(this.deepest, inside.deepest);
    }

    /*
     * These rules where {@code condition} holds, it tested only as deep under the floor as they lay anything: no
     * deeper they lay nothing, so the surface is the same, with the costly test left out of the rock below.
     */
    private SurfaceRules.RuleSource underTheFloor(final SurfaceRules.ConditionSource condition) {
        final SurfaceRules.RuleSource tested = SurfaceRules.ifTrue(condition, sequence());
        return this.anywhere ? tested : SurfaceRules.ifTrue(SurfaceRules.stoneDepthCheck(this.deepest, false,
                CaveSurface.FLOOR), tested);
    }

    /* The rules read in the order they were given, at any depth. */
    private SurfaceRules.RuleSource sequence() {
        return SurfaceRules.sequence(this.rules.stream().map(Supplier::get).toArray(SurfaceRules.RuleSource[]::new));
    }

    private static SurfaceRules.RuleSource state(final Supplier<? extends Block> block) {
        return SurfaceRules.state(block.get().defaultBlockState());
    }
}
