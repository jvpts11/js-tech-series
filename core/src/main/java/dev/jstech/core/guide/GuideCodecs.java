/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.id.StableNames;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.util.ExtraCodecs;

/**
 * How a manual's files are written: its entries, sections, chapter, manuals and styles, as JSON a resource pack can
 * replace or a pack maker can write by hand.
 *
 * <p>An entry's blocks are a list of objects, each saying its {@code type} first:
 *
 * <pre>{@code
 * {"type": "text", "text": "jsc.guide.graphics_cards.what"}
 * {"type": "figure", "item": "jsc:gtx_780_ti", "caption": "jsc.guide.graphics_cards.figure1"}
 * {"type": "table", "caption": "...", "rows": [{"label": "...", "amount": 3072, "unit": "MB"}]}
 * {"type": "recipes", "recipe_type": "jsindustrial:compressing"}
 * }</pre>
 */
public final class GuideCodecs {

    public static final Codec<GuideBlock.GuideValue> VALUE = RecordCodecBuilder.<RawValue>create(instance ->
            instance.group(
                    Codec.STRING.optionalFieldOf("words").forGetter(RawValue::words),
                    Codec.LONG.optionalFieldOf("amount").forGetter(RawValue::amount),
                    Codec.STRING.optionalFieldOf("unit", "").forGetter(RawValue::unit),
                    Codec.STRING.optionalFieldOf("literal").forGetter(RawValue::literal)
            ).apply(instance, RawValue::new)).comapFlatMap(GuideCodecs::value, GuideCodecs::raw);

    public static final Codec<GuideBlock.TableRow> TABLE_ROW = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("label").forGetter(GuideBlock.TableRow::labelKey),
            VALUE.fieldOf("value").forGetter(GuideBlock.TableRow::value)
    ).apply(instance, GuideBlock.TableRow::new));

    public static final Codec<GuideBlock.Problem> PROBLEM = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("problem").forGetter(GuideBlock.Problem::problemKey),
            Codec.STRING.fieldOf("fix").forGetter(GuideBlock.Problem::fixKey)
    ).apply(instance, GuideBlock.Problem::new));

    public static final Codec<GuideBlock> BLOCK = Codec.STRING.dispatch("type", GuideCodecs::typeOf,
            GuideCodecs::codecOf);

    public static final Codec<GuideEntry> ENTRY = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(GuideEntry::id),
            Codec.STRING.fieldOf("section").forGetter(GuideEntry::section),
            Codec.INT.optionalFieldOf("order", 0).forGetter(GuideEntry::order),
            Codec.STRING.fieldOf("title").forGetter(GuideEntry::titleKey),
            Codec.STRING.optionalFieldOf("icon", "").forGetter(GuideEntry::icon),
            Codec.STRING.listOf().optionalFieldOf("items", List.of()).forGetter(GuideEntry::items),
            BLOCK.listOf().fieldOf("blocks").forGetter(GuideEntry::blocks)
    ).apply(instance, GuideEntry::new));

    public static final Codec<GuideSection> SECTION = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(GuideSection::id),
            Codec.INT.optionalFieldOf("order", 0).forGetter(GuideSection::order),
            Codec.STRING.fieldOf("title").forGetter(GuideSection::titleKey),
            Codec.STRING.optionalFieldOf("icon", "").forGetter(GuideSection::icon)
    ).apply(instance, GuideSection::new));

    public static final Codec<GuideChapter> CHAPTER = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("namespace").forGetter(GuideChapter::namespace),
            Codec.INT.optionalFieldOf("order", 0).forGetter(GuideChapter::order),
            Codec.STRING.fieldOf("title").forGetter(GuideChapter::titleKey),
            Codec.STRING.optionalFieldOf("tab", "").forGetter(GuideChapter::tab)
    ).apply(instance, GuideChapter::new));

    public static final Codec<GuideManual> MANUAL = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(GuideManual::id),
            Codec.STRING.fieldOf("title").forGetter(GuideManual::titleKey),
            Codec.STRING.listOf().optionalFieldOf("cover", List.of()).forGetter(GuideManual::coverKeys),
            Codec.STRING.optionalFieldOf("edition", "").forGetter(GuideManual::edition),
            Codec.STRING.optionalFieldOf("part_number", "").forGetter(GuideManual::partNumber),
            Codec.STRING.fieldOf("style").forGetter(GuideManual::style),
            Codec.STRING.listOf().fieldOf("chapters").forGetter(GuideManual::chapters),
            Codec.STRING.listOf().optionalFieldOf("about", List.of()).forGetter(GuideManual::aboutKeys),
            Codec.INT.optionalFieldOf("priority", 0).forGetter(GuideManual::priority)
    ).apply(instance, GuideManual::new));

    public static final Codec<GuideStyle.Folios> FOLIOS = Codec.STRING.comapFlatMap(GuideCodecs::folios,
            GuideStyle.Folios::serializedName);

    public static final Codec<GuideStyle> STYLE = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("palette", "").forGetter(GuideStyle::palette),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("colours", Map.of())
                    .forGetter(GuideStyle::colours),
            Codec.BOOL.optionalFieldOf("spread", true).forGetter(GuideStyle::spread),
            Codec.INT.fieldOf("page_width").forGetter(GuideStyle::pageWidth),
            Codec.INT.fieldOf("page_height").forGetter(GuideStyle::pageHeight),
            Codec.INT.optionalFieldOf("margin", 9).forGetter(GuideStyle::margin),
            Codec.BOOL.optionalFieldOf("rings", false).forGetter(GuideStyle::rings),
            Codec.STRING.optionalFieldOf("body_font", "").forGetter(GuideStyle::bodyFont),
            Codec.STRING.optionalFieldOf("table_font", "").forGetter(GuideStyle::tableFont),
            FOLIOS.optionalFieldOf("folios", GuideStyle.Folios.CHAPTER_PAGE).forGetter(GuideStyle::folios)
    ).apply(instance, GuideStyle::new));

    private static final StableNames<GuideStyle.Folios> FOLIO_NAMES = StableNames.of(GuideStyle.Folios.class);

    private static final MapCodec<GuideBlock.Paragraph> PARAGRAPH = RecordCodecBuilder.mapCodec(instance ->
            instance.group(Codec.STRING.fieldOf("text").forGetter(GuideBlock.Paragraph::key))
                    .apply(instance, GuideBlock.Paragraph::new));
    private static final MapCodec<GuideBlock.Heading> HEADING = RecordCodecBuilder.mapCodec(instance ->
            instance.group(Codec.STRING.fieldOf("text").forGetter(GuideBlock.Heading::key))
                    .apply(instance, GuideBlock.Heading::new));
    private static final MapCodec<GuideBlock.Figure> FIGURE = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("item").forGetter(GuideBlock.Figure::item),
            Codec.STRING.fieldOf("caption").forGetter(GuideBlock.Figure::captionKey)
    ).apply(instance, GuideBlock.Figure::new));
    private static final MapCodec<GuideBlock.Table> TABLE = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("caption").forGetter(GuideBlock.Table::captionKey),
            TABLE_ROW.listOf().fieldOf("rows").forGetter(GuideBlock.Table::rows)
    ).apply(instance, GuideBlock.Table::new));
    private static final MapCodec<GuideBlock.Recipes> RECIPES = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Codec.STRING.fieldOf("recipe_type").forGetter(GuideBlock.Recipes::type),
                    Codec.STRING.optionalFieldOf("output", "").forGetter(GuideBlock.Recipes::output)
            ).apply(instance, GuideBlock.Recipes::new));
    private static final MapCodec<GuideBlock.Steps> STEPS = RecordCodecBuilder.mapCodec(instance ->
            instance.group(Codec.STRING.listOf().fieldOf("steps").forGetter(GuideBlock.Steps::keys))
                    .apply(instance, GuideBlock.Steps::new));
    private static final MapCodec<GuideBlock.Warning> WARNING = RecordCodecBuilder.mapCodec(instance ->
            instance.group(Codec.STRING.fieldOf("text").forGetter(GuideBlock.Warning::key))
                    .apply(instance, GuideBlock.Warning::new));
    private static final MapCodec<GuideBlock.Problems> PROBLEMS = RecordCodecBuilder.mapCodec(instance ->
            instance.group(PROBLEM.listOf().fieldOf("problems").forGetter(GuideBlock.Problems::problems))
                    .apply(instance, GuideBlock.Problems::new));
    private static final MapCodec<GuideBlock.Define> DEFINE = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("term").forGetter(GuideBlock.Define::termKey),
            Codec.STRING.fieldOf("definition").forGetter(GuideBlock.Define::definitionKey)
    ).apply(instance, GuideBlock.Define::new));
    private static final MapCodec<GuideBlock.SeeAlso> SEE = RecordCodecBuilder.mapCodec(instance ->
            instance.group(Codec.STRING.listOf().fieldOf("entries").forGetter(GuideBlock.SeeAlso::entries))
                    .apply(instance, GuideBlock.SeeAlso::new));
    private static final MapCodec<GuideBlock.Custom> CUSTOM = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("kind").forGetter(GuideBlock.Custom::type),
            Codec.INT.fieldOf("height").forGetter(GuideBlock.Custom::height),
            ExtraCodecs.JSON.optionalFieldOf("data").forGetter(custom -> Optional.of(JsonParser.parseString(
                    custom.data())))
    ).apply(instance, (type, height, data) -> new GuideBlock.Custom(type, height,
            data.map(JsonElement::toString).orElse("{}"))));

    private GuideCodecs() {
    }

    private static String typeOf(final GuideBlock block) {
        return switch (block) {
            case GuideBlock.Paragraph paragraph -> "text";
            case GuideBlock.Heading heading -> "heading";
            case GuideBlock.Figure figure -> "figure";
            case GuideBlock.Table table -> "table";
            case GuideBlock.Recipes recipes -> "recipes";
            case GuideBlock.Steps steps -> "steps";
            case GuideBlock.Warning warning -> "warning";
            case GuideBlock.Problems problems -> "problems";
            case GuideBlock.Define define -> "define";
            case GuideBlock.SeeAlso see -> "see";
            case GuideBlock.Custom custom -> "custom";
        };
    }

    private static MapCodec<? extends GuideBlock> codecOf(final String type) {
        return switch (type) {
            case "text" -> PARAGRAPH;
            case "heading" -> HEADING;
            case "figure" -> FIGURE;
            case "table" -> TABLE;
            case "recipes" -> RECIPES;
            case "steps" -> STEPS;
            case "warning" -> WARNING;
            case "problems" -> PROBLEMS;
            case "define" -> DEFINE;
            case "see" -> SEE;
            case "custom" -> CUSTOM;
            default -> throw new IllegalArgumentException("no block of the type " + type);
        };
    }

    private static DataResult<GuideBlock.GuideValue> value(final RawValue raw) {
        if (raw.words().isPresent()) {
            return DataResult.success(new GuideBlock.GuideValue.Words(raw.words().get()));
        }
        if (raw.amount().isPresent()) {
            return DataResult.success(new GuideBlock.GuideValue.Amount(raw.amount().get(), raw.unit()));
        }
        if (raw.literal().isPresent()) {
            return DataResult.success(new GuideBlock.GuideValue.Literal(raw.literal().get()));
        }
        return DataResult.error(() -> "a table's value says words, an amount or a literal");
    }

    private static RawValue raw(final GuideBlock.GuideValue value) {
        return switch (value) {
            case GuideBlock.GuideValue.Words words -> new RawValue(Optional.of(words.key()), Optional.empty(), "",
                    Optional.empty());
            case GuideBlock.GuideValue.Amount amount -> new RawValue(Optional.empty(), Optional.of(amount.value()),
                    amount.unit(), Optional.empty());
            case GuideBlock.GuideValue.Literal literal -> new RawValue(Optional.empty(), Optional.empty(), "",
                    Optional.of(literal.text()));
        };
    }

    private static DataResult<GuideStyle.Folios> folios(final String name) {
        final GuideStyle.Folios folios = FOLIO_NAMES.find(name);
        return folios == null ? DataResult.error(() -> "no way of numbering pages called " + name)
                : DataResult.success(folios);
    }

    /** A table value as its file writes it: one of its three forms, the others absent. */
    private record RawValue(Optional<String> words, Optional<Long> amount, String unit, Optional<String> literal) {
    }
}
