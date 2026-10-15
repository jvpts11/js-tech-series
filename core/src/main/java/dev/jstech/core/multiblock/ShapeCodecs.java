/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.id.StableCodecs;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * The parts of a multiblock's shape as data files hold them: a slot's blocks and tags, a place with what it counts
 * and gives, a limit, a repeated slice, an inside with its parts and their neighbour rules. A matcher made in code
 * cannot be written; one declared with {@link BlockMatch} can.
 */
public final class ShapeCodecs {

    /* The names of a pattern's three axes in a file. */
    private static final List<String> AXES = List.of("x", "y", "z");

    /** A slot: any of these blocks, any block of these tags. */
    public static final Codec<BlockMatch> MATCH = RecordCodecBuilder.<RawMatch>create(instance -> instance.group(
                    Codec.STRING.listOf().optionalFieldOf("blocks", List.of()).forGetter(RawMatch::blocks),
                    Codec.STRING.listOf().optionalFieldOf("tags", List.of()).forGetter(RawMatch::tags))
            .apply(instance, RawMatch::new))
            .flatXmap(ShapeCodecs::match, match -> DataResult.success(new RawMatch(match.blocks(), match.tags())));

    /** A matcher, which a file holds only when it is a {@link BlockMatch}. */
    public static final Codec<IBlockMatcher> MATCHER = MATCH.<IBlockMatcher>flatComapMap(match -> match,
            matcher -> matcher instanceof BlockMatch match ? DataResult.success(match)
                    : DataResult.error(() -> "a slot fitted with code cannot be written to a file; declare it with"
                            + " BlockMatch"));

    /** One letter of a pattern. */
    public static final Codec<Character> LETTER = Codec.STRING.flatXmap(
            text -> text.length() == 1 ? DataResult.success(text.charAt(0))
                    : DataResult.error(() -> "a key is one letter, not \"" + text + "\""),
            letter -> DataResult.success(String.valueOf(letter)));

    /** The least and the most, the most left out for no most. */
    public static final Codec<Limit> LIMIT = RecordCodecBuilder.<RawLimit>create(instance -> instance.group(
                    Codec.INT.fieldOf("least").forGetter(RawLimit::least),
                    Codec.INT.optionalFieldOf("most").forGetter(RawLimit::most))
            .apply(instance, RawLimit::new))
            .flatXmap(raw -> attempt(() -> new Limit(raw.least(), raw.most().orElse(Integer.MAX_VALUE))),
                    limit -> DataResult.success(new RawLimit(limit.least(), limit.most() == Integer.MAX_VALUE
                            ? Optional.empty() : Optional.of(limit.most()))));

    /**
     * A place: its blocks and tags; a count, whose blocks are the place's own when it names none; a number by block,
     * a tag written {@code #namespace:path}, and how the numbers are gathered.
     */
    public static final Codec<Place> PLACE = RecordCodecBuilder.<RawPlace>create(instance -> instance.group(
                    Codec.STRING.listOf().optionalFieldOf("blocks", List.of()).forGetter(RawPlace::blocks),
                    Codec.STRING.listOf().optionalFieldOf("tags", List.of()).forGetter(RawPlace::tags),
                    RecordCodecBuilder.<RawCount>create(count -> count.group(
                                    Codec.STRING.fieldOf("name").forGetter(RawCount::name),
                                    MATCH.optionalFieldOf("which").forGetter(RawCount::which))
                            .apply(count, RawCount::new)).optionalFieldOf("count").forGetter(RawPlace::count),
                    RecordCodecBuilder.<RawValue>create(value -> value.group(
                                    Codec.STRING.fieldOf("key").forGetter(RawValue::key),
                                    Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).fieldOf("by")
                                            .forGetter(RawValue::by),
                                    StableCodecs.byName(Place.Gather.class).fieldOf("gather")
                                            .forGetter(RawValue::gather))
                            .apply(value, RawValue::new)).optionalFieldOf("value").forGetter(RawPlace::value))
            .apply(instance, RawPlace::new))
            .flatXmap(ShapeCodecs::place, ShapeCodecs::rawPlace);

    /** A pattern's repeated slice: across which axis, at which index, the least and the most times. */
    public static final Codec<MultiblockPattern.Repeat> REPEAT = RecordCodecBuilder.<RawRepeat>create(instance ->
                    instance.group(
                            Codec.STRING.fieldOf("axis").forGetter(RawRepeat::axis),
                            Codec.INT.fieldOf("index").forGetter(RawRepeat::index),
                            Codec.INT.fieldOf("least").forGetter(RawRepeat::least),
                            Codec.INT.fieldOf("most").forGetter(RawRepeat::most))
                            .apply(instance, RawRepeat::new))
            .flatXmap(ShapeCodecs::repeat, repeat -> DataResult.success(new RawRepeat(
                    AXES.get(repeat.axis()), repeat.index(), repeat.times().least(), repeat.times().most())));

    /** A neighbour: a working part, by name, or any of some blocks and tags. */
    public static final Codec<InsidePart.Neighbour> NEIGHBOUR = RecordCodecBuilder.<RawNeighbour>create(instance ->
                    instance.group(
                            Codec.STRING.optionalFieldOf("part").forGetter(RawNeighbour::part),
                            Codec.STRING.listOf().optionalFieldOf("blocks", List.of()).forGetter(RawNeighbour::blocks),
                            Codec.STRING.listOf().optionalFieldOf("tags", List.of()).forGetter(RawNeighbour::tags))
                            .apply(instance, RawNeighbour::new))
            .flatXmap(ShapeCodecs::neighbour, ShapeCodecs::rawNeighbour);

    /** A part inside: its name, its blocks, what it gives, its bonuses and its needs. */
    public static final Codec<InsidePart> PART = RecordCodecBuilder.<RawPart>create(instance -> instance.group(
                    Codec.STRING.fieldOf("name").forGetter(RawPart::name),
                    MATCHER.fieldOf("which").forGetter(RawPart::which),
                    Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).optionalFieldOf("gives", Map.of())
                            .forGetter(RawPart::gives),
                    RecordCodecBuilder.<InsidePart.Bonus>create(bonus -> bonus.group(
                                    Codec.STRING.fieldOf("key").forGetter(InsidePart.Bonus::key),
                                    NEIGHBOUR.fieldOf("of").forGetter(InsidePart.Bonus::of),
                                    Codec.DOUBLE.fieldOf("each").forGetter(InsidePart.Bonus::each))
                            .apply(bonus, InsidePart.Bonus::new)).listOf().optionalFieldOf("bonuses", List.of())
                            .forGetter(RawPart::bonuses),
                    RecordCodecBuilder.<RawNeed>create(need -> need.group(
                                    NEIGHBOUR.fieldOf("of").forGetter(RawNeed::of),
                                    Codec.INT.fieldOf("least").forGetter(RawNeed::least))
                            .apply(need, RawNeed::new)).listOf().optionalFieldOf("needs", List.of())
                            .forGetter(RawPart::needs))
            .apply(instance, RawPart::new))
            .flatXmap(raw -> attempt(() -> new InsidePart(raw.name(), raw.which(), raw.gives(), raw.bonuses(),
                            raw.needs().stream().map(n -> new InsidePart.Need(n.of(), n.least())).toList())),
                    part -> DataResult.success(new RawPart(part.name(), part.which(), part.gives(), part.bonuses(),
                            part.needs().stream().map(n -> new RawNeed(n.of(), n.least())).toList())));

    /** An inside: what may stand there that gives nothing, and its parts. */
    public static final Codec<InsideRule> INSIDE = RecordCodecBuilder.<RawInside>create(instance -> instance.group(
                    MATCHER.fieldOf("allows").forGetter(RawInside::allows),
                    PART.listOf().optionalFieldOf("parts", List.of()).forGetter(RawInside::parts))
            .apply(instance, RawInside::new))
            .flatXmap(raw -> attempt(() -> new InsideRule(raw.allows(), raw.parts()).requireKnownParts()),
                    rule -> DataResult.success(new RawInside(rule.allows(), rule.parts())));

    private ShapeCodecs() {
    }

    /** The first block or tag id the match names that cannot be read as an id, or none when all of them can. */
    @Nullable
    public static String unreadableId(final BlockMatch match) {
        for (final String id : match.blocks()) {
            if (ResourceLocation.tryParse(id) == null) {
                return id;
            }
        }
        for (final String id : match.tags()) {
            if (ResourceLocation.tryParse(id) == null) {
                return id;
            }
        }
        return null;
    }

    /** Every {@link BlockMatch} a pattern names: its places, what they count, its inside and its parts. */
    public static List<BlockMatch> matchesOf(final MultiblockPattern pattern) {
        final List<IBlockMatcher> all = new ArrayList<>();
        for (final Place place : pattern.places().values()) {
            all.add(place.fits());
            if (place.counted() != null) {
                all.add(place.counted().which());
            }
        }
        pattern.inside().ifPresent(inside -> {
            all.add(inside.rule().allows());
            for (final InsidePart part : inside.rule().parts()) {
                all.add(part.which());
                part.needs().forEach(need -> addNeighbour(all, need.of()));
                part.bonuses().forEach(bonus -> addNeighbour(all, bonus.of()));
            }
        });
        final List<BlockMatch> out = new ArrayList<>();
        for (final IBlockMatcher matcher : all) {
            if (matcher instanceof BlockMatch match) {
                out.add(match);
            }
        }
        return out;
    }

    private static void addNeighbour(final List<IBlockMatcher> all, final InsidePart.Neighbour of) {
        if (of instanceof InsidePart.Neighbour.Block block) {
            all.add(block.which());
        }
    }

    private static DataResult<BlockMatch> match(final RawMatch raw) {
        if (raw.blocks().isEmpty() && raw.tags().isEmpty()) {
            return DataResult.error(() -> "a slot names at least one block or tag");
        }
        final BlockMatch match = new BlockMatch(raw.blocks(), raw.tags());
        final String bad = unreadableId(match);
        return bad == null ? DataResult.success(match)
                : DataResult.error(() -> "a slot names \"" + bad + "\", which is not an id");
    }

    private static DataResult<Place> place(final RawPlace raw) {
        return match(new RawMatch(raw.blocks(), raw.tags())).flatMap(fits -> attempt(() -> {
            Place place = Place.of(fits);
            if (raw.count().isPresent()) {
                place = place.counting(raw.count().get().name(), raw.count().get().which().orElse(fits));
            }
            if (raw.value().isPresent()) {
                final RawValue value = raw.value().get();
                place = place.valued(value.key(), value.by(), value.gather());
            }
            return place;
        }));
    }

    private static DataResult<RawPlace> rawPlace(final Place place) {
        if (!(place.fits() instanceof BlockMatch fits)) {
            return DataResult.error(() -> "a place fitted with code cannot be written to a file; declare it with"
                    + " BlockMatch");
        }
        Optional<RawCount> count = Optional.empty();
        if (place.counted() != null) {
            final IBlockMatcher which = place.counted().which();
            if (!(which instanceof BlockMatch counted)) {
                return DataResult.error(() -> "the count " + place.counted().name() + " is made in code and cannot"
                        + " be written to a file; declare it with BlockMatch");
            }
            count = Optional.of(new RawCount(place.counted().name(),
                    counted.equals(fits) ? Optional.empty() : Optional.of(counted)));
        }
        final Place.Valued valued = place.valued();
        return DataResult.success(new RawPlace(fits.blocks(), fits.tags(), count, valued == null ? Optional.empty()
                : Optional.of(new RawValue(valued.key(), valued.byBlock(), valued.how()))));
    }

    private static DataResult<MultiblockPattern.Repeat> repeat(final RawRepeat raw) {
        final int axis = AXES.indexOf(raw.axis());
        if (axis < 0) {
            return DataResult.error(() -> "a slice repeats across x, y or z, not \"" + raw.axis() + "\"");
        }
        return attempt(() -> new MultiblockPattern.Repeat(axis, raw.index(), new Limit(raw.least(), raw.most())));
    }

    private static DataResult<InsidePart.Neighbour> neighbour(final RawNeighbour raw) {
        if (raw.part().isPresent()) {
            return raw.blocks().isEmpty() && raw.tags().isEmpty()
                    ? DataResult.success(InsidePart.Neighbour.part(raw.part().get()))
                    : DataResult.error(() -> "a neighbour is a part or some blocks, not both");
        }
        return match(new RawMatch(raw.blocks(), raw.tags())).map(InsidePart.Neighbour::block);
    }

    private static DataResult<RawNeighbour> rawNeighbour(final InsidePart.Neighbour neighbour) {
        return switch (neighbour) {
            case InsidePart.Neighbour.Part part -> DataResult.success(new RawNeighbour(Optional.of(part.name()),
                    List.of(), List.of()));
            case InsidePart.Neighbour.Block block -> block.which() instanceof BlockMatch match
                    ? DataResult.success(new RawNeighbour(Optional.empty(), match.blocks(), match.tags()))
                    : DataResult.error(() -> "a neighbour made in code cannot be written to a file; declare it with"
                            + " BlockMatch");
        };
    }

    /* The value made, or the reason it cannot be, as a decoding error. */
    private static <T> DataResult<T> attempt(final IMaker<T> maker) {
        try {
            return DataResult.success(maker.make());
        } catch (final IllegalArgumentException | IllegalStateException wrong) {
            return DataResult.error(wrong::getMessage);
        }
    }

    @FunctionalInterface
    private interface IMaker<T> {
        T make();
    }

    private record RawMatch(List<String> blocks, List<String> tags) {
    }

    private record RawLimit(int least, Optional<Integer> most) {
    }

    private record RawCount(String name, Optional<BlockMatch> which) {
    }

    private record RawValue(String key, Map<String, Double> by, Place.Gather gather) {
    }

    private record RawPlace(List<String> blocks, List<String> tags, Optional<RawCount> count,
                            Optional<RawValue> value) {
    }

    private record RawPart(String name, IBlockMatcher which, Map<String, Double> gives, List<InsidePart.Bonus> bonuses,
                           List<RawNeed> needs) {
    }

    private record RawInside(IBlockMatcher allows, List<InsidePart> parts) {
    }

    private record RawRepeat(String axis, int index, int least, int most) {
    }

    private record RawNeighbour(Optional<String> part, List<String> blocks, List<String> tags) {
    }

    private record RawNeed(InsidePart.Neighbour of, int least) {
    }
}
