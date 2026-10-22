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
import dev.jstech.core.id.StableCodecs;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
 * {"type": "picture", "drawing": "jsc:firmware", "data": {"look": "uefi"}, "height": 92, "caption": "..."}
 * {"type": "table", "caption": "...", "rows": [[{"words": "..."}, {"amount": 3072, "unit": "MB"}]]}
 * {"type": "table", "caption": "...", "heads": ["...", "..."], "rows": [[{"literal": "dir"}, {"words": "..."}]]}
 * {"type": "table", "caption": "...", "heads": ["...", "..."], "source": "jsc:cable_reach"}
 * {"type": "listing", "name": "Hello.sgs", "lines": ["using System.IO.*;", "", "namespace Hello;"]}
 * {"type": "session", "look": "jsc:guide/terminal/dos", "title": "MC-DOS 6.22",
 *  "lines": [{"prompt": "C:\\>", "text": "dir", "typed": true}, {"text": " Directory of C:\\"}]}
 * {"type": "recipes", "recipe_type": "jsindustrial:compressing"}
 * {"type": "command", "command": "jsc:command/mc_dos/dir"}
 * {"type": "scene", "caption": "...", "height": 120, "layers": false,
 *  "blocks": [{"x": 0, "y": 0, "z": 0, "state": "jsc:transition_case[facing=south]", "data": "{...}"}],
 *  "view": {"yaw": -35, "pitch": 25, "zoom": 1},
 *  "balloons": [{"number": 1, "part": {"x": 0, "y": 0, "z": 0, "box": [5, 2, 15, 7, 4, 16]}, "text": "..."}]}
 * {"type": "multiblock", "multiblock": "mymod:coke_oven", "caption": "...", "height": 120}
 * }</pre>
 */
public final class GuideCodecs {

    public static final Codec<GuideBlock.GuideValue> VALUE = RecordCodecBuilder.<RawValue>create(instance ->
            instance.group(
                    Codec.STRING.optionalFieldOf("words").forGetter(RawValue::words),
                    Codec.LONG.optionalFieldOf("amount").forGetter(RawValue::amount),
                    Codec.STRING.optionalFieldOf("unit", "").forGetter(RawValue::unit),
                    Codec.STRING.optionalFieldOf("literal").forGetter(RawValue::literal)
            ).apply(instance, RawValue::new)).flatXmap(GuideCodecs::value, GuideCodecs::raw);

    /** A row of a table: its cells, left to right, as a list. */
    public static final Codec<GuideBlock.TableRow> TABLE_ROW = VALUE.listOf().xmap(GuideBlock.TableRow::new,
            GuideBlock.TableRow::cells);

    public static final Codec<GuideBlock.Problem> PROBLEM = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("problem").forGetter(GuideBlock.Problem::problemKey),
            Codec.STRING.fieldOf("fix").forGetter(GuideBlock.Problem::fixKey)
    ).apply(instance, GuideBlock.Problem::new));

    public static final Codec<GuideBlock> BLOCK = Codec.STRING.dispatch("type", GuideCodecs::typeOf,
            GuideCodecs::codecOf);
    /** The type every kind of block is written with in a file, as {@link #BLOCK} reads it. */
    public static final Set<String> BLOCK_TYPES = Set.of("text", "heading", "figure", "picture", "table", "listing",
            "session", "recipes", "steps", "warning", "problems", "define", "see", "custom", "note", "break", "views",
            "plan", "command", "scene", "multiblock", "structure");

    public static final Codec<GuideEntry> ENTRY = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(GuideEntry::id),
            Codec.STRING.fieldOf("section").forGetter(GuideEntry::section),
            Codec.INT.optionalFieldOf("order", 0).forGetter(GuideEntry::order),
            Codec.STRING.fieldOf("title").forGetter(GuideEntry::titleKey),
            Codec.STRING.optionalFieldOf("icon", "").forGetter(GuideEntry::icon),
            Codec.STRING.listOf().optionalFieldOf("items", List.of()).forGetter(GuideEntry::items),
            Codec.STRING.listOf().optionalFieldOf("shows", List.of()).forGetter(GuideEntry::shows),
            BLOCK.listOf().fieldOf("blocks").forGetter(GuideEntry::blocks),
            Codec.STRING.optionalFieldOf("needs", "").forGetter(GuideEntry::needs)
    ).apply(instance, GuideEntry::new));

    public static final Codec<GuideSection> SECTION = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(GuideSection::id),
            Codec.INT.optionalFieldOf("order", 0).forGetter(GuideSection::order),
            Codec.STRING.fieldOf("title").forGetter(GuideSection::titleKey),
            Codec.STRING.optionalFieldOf("icon", "").forGetter(GuideSection::icon),
            Codec.STRING.optionalFieldOf("chapter", "").forGetter(GuideSection::chapter)
    ).apply(instance, GuideSection::new));

    public static final Codec<GuideChapter> CHAPTER = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(GuideChapter::id),
            Codec.INT.optionalFieldOf("order", 0).forGetter(GuideChapter::order),
            Codec.STRING.fieldOf("title").forGetter(GuideChapter::titleKey),
            Codec.STRING.optionalFieldOf("tab", "").forGetter(GuideChapter::tab),
            Codec.STRING.optionalFieldOf("about", "").forGetter(GuideChapter::aboutKey),
            Codec.BOOL.optionalFieldOf("in_reference", true).forGetter(GuideChapter::inReference)
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
            Codec.INT.optionalFieldOf("priority", 0).forGetter(GuideManual::priority),
            Codec.STRING.optionalFieldOf("icon", "").forGetter(GuideManual::icon)
    ).apply(instance, GuideManual::new));

    public static final Codec<GuideStyle.Pages> PAGES = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("spread", true).forGetter(GuideStyle.Pages::spread),
            Codec.INT.fieldOf("width").forGetter(GuideStyle.Pages::width),
            Codec.INT.fieldOf("height").forGetter(GuideStyle.Pages::height),
            Codec.INT.optionalFieldOf("margin", 10).forGetter(GuideStyle.Pages::margin),
            Codec.INT.optionalFieldOf("columns", 1).forGetter(GuideStyle.Pages::columns)
    ).apply(instance, GuideStyle.Pages::new));

    public static final Codec<GuideStyle.Decor> DECOR = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("rings", false).forGetter(GuideStyle.Decor::rings),
            Codec.BOOL.optionalFieldOf("grid", false).forGetter(GuideStyle.Decor::grid),
            Codec.BOOL.optionalFieldOf("frame", false).forGetter(GuideStyle.Decor::frame),
            Codec.BOOL.optionalFieldOf("title_block", false).forGetter(GuideStyle.Decor::titleBlock),
            Codec.BOOL.optionalFieldOf("upper_headings", false).forGetter(GuideStyle.Decor::upperHeadings),
            Codec.BOOL.optionalFieldOf("traced", false).forGetter(GuideStyle.Decor::traced),
            Codec.BOOL.optionalFieldOf("plain_numbers", false).forGetter(GuideStyle.Decor::plainNumbers),
            Codec.BOOL.optionalFieldOf("small_text", false).forGetter(GuideStyle.Decor::smallText)
    ).apply(instance, GuideStyle.Decor::new));

    public static final Codec<GuideStyle.CoverPart> COVER_PART = RecordCodecBuilder.create(instance -> instance.group(
            StableCodecs.byName(GuideStyle.PartKind.class).fieldOf("kind").forGetter(GuideStyle.CoverPart::kind),
            Codec.INT.optionalFieldOf("x", 0).forGetter(GuideStyle.CoverPart::x),
            Codec.INT.optionalFieldOf("y", 0).forGetter(GuideStyle.CoverPart::y),
            Codec.INT.optionalFieldOf("width", 0).forGetter(GuideStyle.CoverPart::width),
            Codec.INT.optionalFieldOf("height", 0).forGetter(GuideStyle.CoverPart::height),
            Codec.STRING.optionalFieldOf("colour", "").forGetter(GuideStyle.CoverPart::colour),
            Codec.INT.optionalFieldOf("line", 0).forGetter(GuideStyle.CoverPart::words),
            Codec.STRING.optionalFieldOf("image", "").forGetter(GuideStyle.CoverPart::image),
            Codec.FLOAT.optionalFieldOf("scale", 1.0F).forGetter(GuideStyle.CoverPart::scale),
            Codec.INT.optionalFieldOf("turn", 0).forGetter(GuideStyle.CoverPart::turn)
    ).apply(instance, GuideStyle.CoverPart::new));

    public static final Codec<GuideStyle.Cover> COVER = RecordCodecBuilder.create(instance -> instance.group(
            StableCodecs.byName(GuideStyle.CoverKind.class).optionalFieldOf("kind", GuideStyle.CoverKind.BINDER)
                    .forGetter(GuideStyle.Cover::kind),
            Codec.BOOL.optionalFieldOf("label", true).forGetter(GuideStyle.Cover::label),
            Codec.BOOL.optionalFieldOf("band", false).forGetter(GuideStyle.Cover::band),
            COVER_PART.listOf().optionalFieldOf("parts", List.of()).forGetter(GuideStyle.Cover::parts)
    ).apply(instance, GuideStyle.Cover::new));

    public static final Codec<GuideStyle.Make> MAKE = RecordCodecBuilder.create(instance -> instance.group(
            StableCodecs.byName(GuideStyle.Binding.class).fieldOf("binding").forGetter(GuideStyle.Make::binding),
            StableCodecs.byName(GuideStyle.Board.class).optionalFieldOf("board", GuideStyle.Board.PLAIN)
                    .forGetter(GuideStyle.Make::board),
            StableCodecs.byName(GuideStyle.Paper.class).optionalFieldOf("paper", GuideStyle.Paper.PLAIN)
                    .forGetter(GuideStyle.Make::paper),
            StableCodecs.byName(GuideStyle.Marks.class).optionalFieldOf("marks", GuideStyle.Marks.TABS)
                    .forGetter(GuideStyle.Make::marks),
            Codec.STRING.listOf().optionalFieldOf("colours", List.of()).forGetter(GuideStyle.Make::colours),
            Codec.BOOL.optionalFieldOf("ribbon", false).forGetter(GuideStyle.Make::ribbon)
    ).apply(instance, GuideStyle.Make::new));

    public static final Codec<GuideStyle> STYLE = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("palette", "").forGetter(GuideStyle::palette),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("colours", Map.of())
                    .forGetter(GuideStyle::colours),
            PAGES.fieldOf("pages").forGetter(GuideStyle::pages),
            DECOR.optionalFieldOf("decor", GuideStyle.Decor.binder(false)).forGetter(GuideStyle::decor),
            Codec.STRING.optionalFieldOf("body_font", "").forGetter(GuideStyle::bodyFont),
            Codec.STRING.optionalFieldOf("code_font", "").forGetter(GuideStyle::codeFont),
            StableCodecs.byName(GuideStyle.Folios.class).optionalFieldOf("folios", GuideStyle.Folios.CHAPTER_PAGE)
                    .forGetter(GuideStyle::folios),
            Codec.STRING.optionalFieldOf("drawing_prefix", "").forGetter(GuideStyle::drawingPrefix),
            COVER.optionalFieldOf("cover", new GuideStyle.Cover(GuideStyle.CoverKind.BINDER, true, false))
                    .forGetter(GuideStyle::cover),
            StableCodecs.byName(GuideStyle.HoldBar.class).optionalFieldOf("hold_bar", GuideStyle.HoldBar.PLAIN)
                    .forGetter(GuideStyle::holdBar),
            // A style saying nothing of how it is made is made as its decor says, so older files read the same.
            MAKE.optionalFieldOf("make").forGetter(style -> style.make().equals(GuideStyle.Make.of(style.decor()))
                    ? Optional.empty() : Optional.of(style.make()))
    ).apply(instance, (palette, colours, pages, decor, body, code, folios, prefix, cover, bar, make) ->
            new GuideStyle(palette, colours, pages, decor, body, code, folios, prefix, cover, bar,
                    make.orElseGet(() -> GuideStyle.Make.of(decor)))));

    public static final Codec<GuideBlock.Callout> CALLOUT = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("number").forGetter(GuideBlock.Callout::number),
            StableCodecs.byName(GuideBlock.View.class).fieldOf("view").forGetter(GuideBlock.Callout::view),
            Codec.INT.fieldOf("u").forGetter(GuideBlock.Callout::u),
            Codec.INT.fieldOf("v").forGetter(GuideBlock.Callout::v),
            Codec.STRING.fieldOf("text").forGetter(GuideBlock.Callout::key)
    ).apply(instance, GuideBlock.Callout::new));

    public static final Codec<GuideBlock.PlanPart> PLAN_PART = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("item", "").forGetter(GuideBlock.PlanPart::item),
            Codec.STRING.fieldOf("label").forGetter(GuideBlock.PlanPart::labelKey),
            Codec.BOOL.optionalFieldOf("optional", false).forGetter(GuideBlock.PlanPart::optional)
    ).apply(instance, GuideBlock.PlanPart::new));

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
            Codec.STRING.listOf().optionalFieldOf("heads", List.of()).forGetter(GuideBlock.Table::headKeys),
            TABLE_ROW.listOf().optionalFieldOf("rows", List.of()).forGetter(GuideBlock.Table::rows),
            Codec.STRING.optionalFieldOf("source", "").forGetter(GuideBlock.Table::source)
    ).apply(instance, GuideBlock.Table::new));
    private static final MapCodec<GuideBlock.Listing> LISTING = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Codec.STRING.optionalFieldOf("name", "").forGetter(GuideBlock.Listing::name),
                    Codec.STRING.optionalFieldOf("language", "").forGetter(GuideBlock.Listing::language),
                    Codec.STRING.listOf().fieldOf("lines").forGetter(GuideBlock.Listing::lines)
            ).apply(instance, GuideBlock.Listing::new));
    private static final Codec<GuideBlock.SessionLine> SESSION_LINE = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.optionalFieldOf("prompt", "").forGetter(GuideBlock.SessionLine::prompt),
                    Codec.STRING.fieldOf("text").forGetter(GuideBlock.SessionLine::text),
                    Codec.BOOL.optionalFieldOf("typed", false).forGetter(GuideBlock.SessionLine::typed)
            ).apply(instance, GuideBlock.SessionLine::new));
    private static final MapCodec<GuideBlock.Session> SESSION = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Codec.STRING.fieldOf("look").forGetter(GuideBlock.Session::look),
                    Codec.STRING.optionalFieldOf("title", "").forGetter(GuideBlock.Session::title),
                    SESSION_LINE.listOf().fieldOf("lines").forGetter(GuideBlock.Session::lines)
            ).apply(instance, GuideBlock.Session::new));
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
    private static final MapCodec<GuideBlock.Picture> PICTURE = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Codec.STRING.optionalFieldOf("image", "").forGetter(GuideBlock.Picture::image),
                    Codec.STRING.optionalFieldOf("drawing", "").forGetter(GuideBlock.Picture::drawing),
                    ExtraCodecs.JSON.optionalFieldOf("data").forGetter(picture -> Optional.of(
                            JsonParser.parseString(picture.data()))),
                    Codec.INT.optionalFieldOf("width", 0).forGetter(GuideBlock.Picture::width),
                    Codec.INT.fieldOf("height").forGetter(GuideBlock.Picture::height),
                    Codec.STRING.fieldOf("caption").forGetter(GuideBlock.Picture::captionKey),
                    Codec.INT.optionalFieldOf("frames", 1).forGetter(GuideBlock.Picture::frames),
                    Codec.INT.optionalFieldOf("frame_ticks", 1).forGetter(GuideBlock.Picture::frameTicks)
            ).apply(instance, (image, drawing, data, width, height, caption, frames, frameTicks) ->
                    new GuideBlock.Picture(image, drawing, data.map(JsonElement::toString).orElse("{}"), width,
                            height, caption, frames, frameTicks)));
    private static final MapCodec<GuideBlock.Note> NOTE = RecordCodecBuilder.mapCodec(instance ->
            instance.group(Codec.STRING.fieldOf("text").forGetter(GuideBlock.Note::key))
                    .apply(instance, GuideBlock.Note::new));
    private static final MapCodec<GuideBlock.Break> BREAK = RecordCodecBuilder.mapCodec(instance ->
            instance.group(StableCodecs.byName(GuideBlock.BreakKind.class).fieldOf("to")
                    .forGetter(GuideBlock.Break::kind)).apply(instance, GuideBlock.Break::new));
    private static final MapCodec<GuideBlock.Views> VIEWS = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("item").forGetter(GuideBlock.Views::item),
            CALLOUT.listOf().optionalFieldOf("callouts", List.of()).forGetter(GuideBlock.Views::callouts)
    ).apply(instance, GuideBlock.Views::new));
    private static final MapCodec<GuideBlock.Plan> PLAN = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("caption").forGetter(GuideBlock.Plan::captionKey),
            PLAN_PART.listOf().fieldOf("parts").forGetter(GuideBlock.Plan::parts)
    ).apply(instance, GuideBlock.Plan::new));
    private static final MapCodec<GuideBlock.Command> COMMAND = RecordCodecBuilder.mapCodec(instance ->
            instance.group(Codec.STRING.fieldOf("command").forGetter(GuideBlock.Command::source))
                    .apply(instance, GuideBlock.Command::new));
    private static final Codec<GuideBlock.SceneBlock> SCENE_BLOCK = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("x").forGetter(GuideBlock.SceneBlock::x),
                    Codec.INT.fieldOf("y").forGetter(GuideBlock.SceneBlock::y),
                    Codec.INT.fieldOf("z").forGetter(GuideBlock.SceneBlock::z),
                    Codec.STRING.fieldOf("state").forGetter(GuideBlock.SceneBlock::state),
                    Codec.STRING.optionalFieldOf("data", "").forGetter(GuideBlock.SceneBlock::data)
            ).apply(instance, GuideBlock.SceneBlock::new));
    private static final Codec<GuideBlock.SceneView> SCENE_VIEW = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.FLOAT.optionalFieldOf("yaw", GuideBlock.SceneView.CORNER.yaw())
                            .forGetter(GuideBlock.SceneView::yaw),
                    Codec.FLOAT.optionalFieldOf("pitch", GuideBlock.SceneView.CORNER.pitch())
                            .forGetter(GuideBlock.SceneView::pitch),
                    Codec.FLOAT.optionalFieldOf("zoom", GuideBlock.SceneView.CORNER.zoom())
                            .forGetter(GuideBlock.SceneView::zoom)
            ).apply(instance, GuideBlock.SceneView::new));
    /** A part's box in its block, as six numbers: where it starts across, up and back, then where it ends. */
    private static final Codec<List<Integer>> SCENE_BOX = Codec.INT.listOf().comapFlatMap(box -> box.size() == 6
            ? DataResult.success(box) : DataResult.error(() -> "a part's box is six numbers; got " + box), box -> box);
    private static final List<Integer> WHOLE_BLOCK = List.of(0, 0, 0, GuideBlock.ScenePart.PIXELS,
            GuideBlock.ScenePart.PIXELS, GuideBlock.ScenePart.PIXELS);
    private static final Codec<GuideBlock.ScenePart> SCENE_PART = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("x").forGetter(GuideBlock.ScenePart::x),
                    Codec.INT.fieldOf("y").forGetter(GuideBlock.ScenePart::y),
                    Codec.INT.fieldOf("z").forGetter(GuideBlock.ScenePart::z),
                    SCENE_BOX.optionalFieldOf("box", WHOLE_BLOCK).forGetter(part -> List.of(part.fromX(),
                            part.fromY(), part.fromZ(), part.toX(), part.toY(), part.toZ()))
            ).apply(instance, (x, y, z, box) -> new GuideBlock.ScenePart(x, y, z, box.get(0), box.get(1), box.get(2),
                    box.get(3), box.get(4), box.get(5))));
    private static final Codec<GuideBlock.SceneBalloon> SCENE_BALLOON = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("number").forGetter(GuideBlock.SceneBalloon::number),
                    SCENE_PART.fieldOf("part").forGetter(GuideBlock.SceneBalloon::part),
                    Codec.STRING.fieldOf("text").forGetter(GuideBlock.SceneBalloon::key),
                    Codec.STRING.optionalFieldOf("detail", "").forGetter(GuideBlock.SceneBalloon::detailKey)
            ).apply(instance, GuideBlock.SceneBalloon::new));
    private static final Codec<GuideBlock.SceneStep> SCENE_STEP = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("text").forGetter(GuideBlock.SceneStep::captionKey),
                    SCENE_BLOCK.listOf().optionalFieldOf("put", List.of()).forGetter(GuideBlock.SceneStep::put),
                    SCENE_PART.listOf().optionalFieldOf("take", List.of()).forGetter(GuideBlock.SceneStep::take),
                    Codec.INT.optionalFieldOf("ticks", GuideBlock.SceneStep.TICKS)
                            .forGetter(GuideBlock.SceneStep::ticks)
            ).apply(instance, GuideBlock.SceneStep::new));
    private static final MapCodec<GuideBlock.Scene> SCENE = RecordCodecBuilder.mapCodec(instance -> instance.group(
            SCENE_BLOCK.listOf().optionalFieldOf("blocks", List.of()).forGetter(GuideBlock.Scene::blocks),
            SCENE_VIEW.optionalFieldOf("view", GuideBlock.SceneView.CORNER).forGetter(GuideBlock.Scene::view),
            SCENE_BALLOON.listOf().optionalFieldOf("balloons", List.of()).forGetter(GuideBlock.Scene::balloons),
            Codec.BOOL.optionalFieldOf("layers", false).forGetter(GuideBlock.Scene::layers),
            Codec.INT.fieldOf("height").forGetter(GuideBlock.Scene::height),
            Codec.STRING.fieldOf("caption").forGetter(GuideBlock.Scene::captionKey),
            SCENE_STEP.listOf().optionalFieldOf("steps", List.of()).forGetter(GuideBlock.Scene::steps)
    ).apply(instance, GuideBlock.Scene::new));
    private static final MapCodec<GuideBlock.Structure> STRUCTURE = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Codec.STRING.fieldOf("structure").forGetter(GuideBlock.Structure::structure),
                    SCENE_VIEW.optionalFieldOf("view", GuideBlock.SceneView.CORNER)
                            .forGetter(GuideBlock.Structure::view),
                    Codec.INT.fieldOf("height").forGetter(GuideBlock.Structure::height),
                    Codec.STRING.fieldOf("caption").forGetter(GuideBlock.Structure::captionKey)
            ).apply(instance, GuideBlock.Structure::new));
    private static final MapCodec<GuideBlock.Multiblock> MULTIBLOCK = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Codec.STRING.fieldOf("multiblock").forGetter(GuideBlock.Multiblock::multiblock),
                    SCENE_VIEW.optionalFieldOf("view", GuideBlock.SceneView.CORNER)
                            .forGetter(GuideBlock.Multiblock::view),
                    Codec.INT.fieldOf("height").forGetter(GuideBlock.Multiblock::height),
                    Codec.STRING.fieldOf("caption").forGetter(GuideBlock.Multiblock::captionKey)
            ).apply(instance, GuideBlock.Multiblock::new));

    private GuideCodecs() {
    }

    private static String typeOf(final GuideBlock block) {
        return switch (block) {
            case GuideBlock.Paragraph paragraph -> "text";
            case GuideBlock.Heading heading -> "heading";
            case GuideBlock.Figure figure -> "figure";
            case GuideBlock.Picture picture -> "picture";
            case GuideBlock.Table table -> "table";
            case GuideBlock.Listing listing -> "listing";
            case GuideBlock.Session session -> "session";
            case GuideBlock.Recipes recipes -> "recipes";
            case GuideBlock.Steps steps -> "steps";
            case GuideBlock.Warning warning -> "warning";
            case GuideBlock.Problems problems -> "problems";
            case GuideBlock.Define define -> "define";
            case GuideBlock.SeeAlso see -> "see";
            case GuideBlock.Custom custom -> "custom";
            case GuideBlock.Note note -> "note";
            case GuideBlock.Break cut -> "break";
            case GuideBlock.Views views -> "views";
            case GuideBlock.Plan plan -> "plan";
            case GuideBlock.Command command -> "command";
            case GuideBlock.Scene scene -> "scene";
            case GuideBlock.Multiblock multiblock -> "multiblock";
            case GuideBlock.Structure structure -> "structure";
        };
    }

    private static MapCodec<? extends GuideBlock> codecOf(final String type) {
        return switch (type) {
            case "text" -> PARAGRAPH;
            case "heading" -> HEADING;
            case "figure" -> FIGURE;
            case "picture" -> PICTURE;
            case "table" -> TABLE;
            case "listing" -> LISTING;
            case "session" -> SESSION;
            case "recipes" -> RECIPES;
            case "steps" -> STEPS;
            case "warning" -> WARNING;
            case "problems" -> PROBLEMS;
            case "define" -> DEFINE;
            case "see" -> SEE;
            case "custom" -> CUSTOM;
            case "note" -> NOTE;
            case "break" -> BREAK;
            case "views" -> VIEWS;
            case "plan" -> PLAN;
            case "command" -> COMMAND;
            case "scene" -> SCENE;
            case "multiblock" -> MULTIBLOCK;
            case "structure" -> STRUCTURE;
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

    private static DataResult<RawValue> raw(final GuideBlock.GuideValue value) {
        return switch (value) {
            case GuideBlock.GuideValue.Words words -> DataResult.success(new RawValue(Optional.of(words.key()),
                    Optional.empty(), "", Optional.empty()));
            case GuideBlock.GuideValue.Amount amount -> DataResult.success(new RawValue(Optional.empty(),
                    Optional.of(amount.value()), amount.unit(), Optional.empty()));
            case GuideBlock.GuideValue.Literal literal -> DataResult.success(new RawValue(Optional.empty(),
                    Optional.empty(), "", Optional.of(literal.text())));
            // What a mod says is answered as the manual is laid out, so a file has no place for it.
            case GuideBlock.GuideValue.Said said -> DataResult.error(() -> "words a mod says as the game runs are"
                    + " never written to a manual's files: " + said.text().english());
        };
    }

    /** A table value as its file writes it: one of its three forms, the others absent. */
    private record RawValue(Optional<String> words, Optional<Long> amount, String unit, Optional<String> literal) {
    }
}
