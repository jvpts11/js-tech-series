/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * The structure processor {@code jscore:weathering}: a structure's blocks worn by rule as it is placed, rather than a
 * worn copy drawn by hand: each rule turns blocks its test matches into its worn block as often as its chance says,
 * by {@code age}, the same each time for the same place. The worn block keeps the matched block's facing and its other
 * properties where it has them.
 *
 * <pre>{@code
 * { "processor_type": "jscore:weathering", "age": 1.0, "rules": [
 *     {"test": {"predicate_type": "minecraft:block_match", "block": "minecraft:iron_block"},
 *      "worn": {"Name": "mymod:rusted_iron_block"}, "chance": 0.4},
 *     {"test": {"predicate_type": "minecraft:block_match", "block": "minecraft:glass"},
 *      "worn": {"Name": "mymod:cracked_glass"}, "chance": 0.25} ] }
 * }</pre>
 *
 * @param rules what wears into what, and how often
 * @param age   how worn the structure is: each chance by it, 0 for new, 1 as the rules say
 */
public record Weathering(List<Rule> rules, double age) {

    private static final double OLDEST = 10;

    public Weathering {
        rules = List.copyOf(rules);
    }

    /** The processor of these rules. */
    public Processor processor() {
        return new Processor(this);
    }

    /**
     * A rule of wear.
     *
     * @param test   which blocks it wears
     * @param worn   what they wear into
     * @param chance how often, 0 to 1
     */
    public record Rule(RuleTest test, BlockState worn, double chance) {

        public static final Codec<Rule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                RuleTest.CODEC.fieldOf("test").forGetter(Rule::test),
                BlockState.CODEC.fieldOf("worn").forGetter(Rule::worn),
                Codec.doubleRange(0, 1).fieldOf("chance").forGetter(Rule::chance))
                .apply(instance, Rule::new));
    }

    /** The processor, as the game's structure templates take it. */
    public static final class Processor extends StructureProcessor {

        private final Weathering weathering;

        public static final MapCodec<Processor> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Rule.CODEC.listOf().fieldOf("rules").forGetter(processor -> processor.weathering.rules()),
                Codec.doubleRange(0, OLDEST).optionalFieldOf("age", 1.0).forGetter(processor -> processor.weathering
                        .age()))
                .apply(instance, (rules, age) -> new Processor(new Weathering(rules, age))));

        private Processor(final Weathering weathering) {
            this.weathering = weathering;
        }

        /** The rules it wears by. */
        public Weathering weathering() {
            return this.weathering;
        }

        @Override
        public StructureTemplate.StructureBlockInfo processBlock(final LevelReader level, final BlockPos offset,
                                                                 final BlockPos pos,
                                                                 final StructureTemplate.StructureBlockInfo original,
                                                                 final StructureTemplate.StructureBlockInfo placed,
                                                                 final StructurePlaceSettings settings) {
            final RandomSource random = settings.getRandom(placed.pos());
            for (final Rule rule : this.weathering.rules()) {
                if (rule.test().test(placed.state(), random)
                        && random.nextDouble() < rule.chance() * this.weathering.age()) {
                    return new StructureTemplate.StructureBlockInfo(placed.pos(), wear(placed.state(), rule.worn()),
                            placed.nbt());
                }
            }
            return placed;
        }

        @Override
        protected StructureProcessorType<?> getType() {
            return CoreStructures.WEATHERING.get();
        }

        /* The worn block, with the matched block's properties where it has the same ones. */
        private static BlockState wear(final BlockState from, final BlockState worn) {
            BlockState out = worn;
            for (final Property<?> property : from.getProperties()) {
                if (out.hasProperty(property)) {
                    out = copy(from, out, property);
                }
            }
            return out;
        }

        private static <T extends Comparable<T>> BlockState copy(final BlockState from, final BlockState to,
                                                                 final Property<T> property) {
            return to.setValue(property, from.getValue(property));
        }
    }
}
