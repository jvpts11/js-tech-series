/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

/**
 * What one mod writes in the manuals: its chapter, its sections and entries, and the manuals and styles it brings.
 *
 * <p>Everything is declared once, in English, and the data generation writes it out: the files a manual is read from
 * (under {@code assets/<mod>/guide/}) and the sentences, under keys made from the entry and the part, to the mod's
 * English language file. Other languages translate those keys like any other. Items are named by suppliers and read
 * when the files are written, since the registries only answer once the game has loaded.
 *
 * <pre>{@code
 * public static final ModGuide.SectionRef MACHINES = CONTENT.guide().section("machines").titled("Machines")
 *         .icon(() -> MACERATOR).register();
 *
 * CONTENT.guide().page("compressor", MACHINES).titled("Compressor").icon(() -> COMPRESSOR).covers(() -> COMPRESSOR)
 *         .whatItIs("A machine that presses ingots into plates.")
 *         .whatItIsFor("Making plates, the parts later machines are built from.")
 *         .howToGetIt("From the creative menu, J's Industrial tab.")
 *         .howToUseIt("Give it energy.", "Put ingots in its left slot.", "Take the plates from the right one.")
 *         .whatCanGoWrong("It stops halfway.", "It has run out of energy: give it a generator.")
 *         .register();
 * }</pre>
 */
public final class ModGuide {

    private final String namespace;
    private final List<Supplier<GuideSection>> sections = new ArrayList<>();
    private final List<Supplier<GuideEntry>> entries = new ArrayList<>();
    private final List<GuideManual> manuals = new ArrayList<>();
    private final Map<String, GuideStyle> styles = new LinkedHashMap<>();
    private final Map<String, String> english = new LinkedHashMap<>();
    /** The ids of the sections and entries, which share one name space: a link names either by its id. */
    private final Set<String> ids = new LinkedHashSet<>();
    private GuideChapter chapter;

    public ModGuide(final String namespace) {
        this.namespace = Objects.requireNonNull(namespace, "namespace");
    }

    /** The mod's chapter: its title, its place among the chapters, and its tab's colour. */
    public ChapterBuilder chapter() {
        return new ChapterBuilder();
    }

    /** A section of the mod's chapter. */
    public SectionBuilder section(final String path) {
        return new SectionBuilder(path);
    }

    /** An entry: a page, or several, about one thing. */
    public EntryBuilder page(final String path, final SectionRef section) {
        return new EntryBuilder(path, section);
    }

    /** A manual of the mod's own. */
    public ManualBuilder manual(final String path) {
        return new ManualBuilder(path);
    }

    /** A style the mod brings, under {@code <mod>:<path>}. */
    public void style(final String path, final GuideStyle style) {
        if (this.styles.putIfAbsent(path, style) != null) {
            throw new IllegalStateException("the style " + this.id(path) + " is declared twice");
        }
    }

    /** The mod's namespace. */
    public String namespace() {
        return this.namespace;
    }

    /** Whether the mod declared anything for the manuals. */
    public boolean isEmpty() {
        return this.chapter == null && this.entries.isEmpty() && this.manuals.isEmpty() && this.styles.isEmpty();
    }

    /** The chapter, or null when the mod has none. */
    public GuideChapter declaredChapter() {
        return this.chapter;
    }

    /** The sections, in the order declared, their icons read now. */
    public List<GuideSection> declaredSections() {
        return this.sections.stream().map(Supplier::get).toList();
    }

    /** The entries, made now: their items are read from the registries, which only answer once the game has loaded. */
    public List<GuideEntry> declaredEntries() {
        return this.entries.stream().map(Supplier::get).toList();
    }

    /** The manuals the mod brings. */
    public List<GuideManual> declaredManuals() {
        return List.copyOf(this.manuals);
    }

    /** The styles the mod brings, by path. */
    public Map<String, GuideStyle> declaredStyles() {
        return Map.copyOf(this.styles);
    }

    /** Every sentence declared, by key, in English, as the language file is written from them. */
    public Map<String, String> translations() {
        return Map.copyOf(this.english);
    }

    private String id(final String path) {
        return GuideIds.of(this.namespace, path);
    }

    /** Takes an id for a section or an entry, refusing one already taken by either. */
    private String claim(final String path) {
        final String id = this.id(path);
        if (!this.ids.add(id)) {
            throw new IllegalStateException("the id " + id + " names two sections or entries; a link could not"
                    + " tell them apart");
        }
        return id;
    }

    private String key(final String what, final String english) {
        Objects.requireNonNull(english, "the English of " + what);
        final String key = this.namespace + ".guide." + what;
        final String before = this.english.putIfAbsent(key, english);
        if (before != null && !before.equals(english)) {
            throw new IllegalStateException("the key " + key + " is declared with two English sentences");
        }
        return key;
    }

    /** The registered id of an item, refusing a block with no item, which would read as air and draw nothing. */
    private static String itemId(final Supplier<? extends ItemLike> item) {
        if (item == null) {
            return "";
        }
        final Item found = item.get().asItem();
        if (found == Items.AIR) {
            throw new IllegalStateException("a manual names " + item.get() + ", which has no item to draw");
        }
        return BuiltInRegistries.ITEM.getKey(found).toString();
    }

    /** A declared section, which entries are placed in. */
    public record SectionRef(String id) {
    }

    /** The mod's chapter, declared. */
    public final class ChapterBuilder {

        private String title;
        private int order;
        private String tab = "";
        private String about;

        private ChapterBuilder() {
        }

        /** The paragraph on the chapter's opening page, in English: what the mod is about. */
        public ChapterBuilder about(final String english) {
            this.about = english;
            return this;
        }

        /** The chapter's title, in English: the mod's name, as a manual's contents lists it. */
        public ChapterBuilder titled(final String english) {
            this.title = english;
            return this;
        }

        /** Where it stands among the chapters of a manual holding several. */
        public ChapterBuilder order(final int order) {
            this.order = order;
            return this;
        }

        /** Its tab's colour: a declared palette's id, whose role {@code tab} it is, or a colour {@code #AARRGGBB}. */
        public ChapterBuilder tab(final String tab) {
            this.tab = tab;
            return this;
        }

        public GuideChapter register() {
            if (ModGuide.this.chapter != null) {
                throw new IllegalStateException(ModGuide.this.namespace + " declares its chapter twice");
            }
            if (this.title == null) {
                throw new IllegalStateException(ModGuide.this.namespace + "'s chapter needs a title");
            }
            ModGuide.this.chapter = new GuideChapter(ModGuide.this.namespace, this.order,
                    ModGuide.this.key("chapter", this.title), this.tab,
                    this.about == null ? "" : ModGuide.this.key("chapter.about", this.about));
            return ModGuide.this.chapter;
        }
    }

    /** A section, declared. */
    public final class SectionBuilder {

        private final String path;
        private String title;
        private Supplier<? extends ItemLike> icon;

        private SectionBuilder(final String path) {
            this.path = path;
        }

        /** The section's title, in English. */
        public SectionBuilder titled(final String english) {
            this.title = english;
            return this;
        }

        /** The item drawn beside it on its chapter's opening page. */
        public SectionBuilder icon(final Supplier<? extends ItemLike> icon) {
            this.icon = icon;
            return this;
        }

        public SectionRef register() {
            if (this.title == null) {
                throw new IllegalStateException("the section " + ModGuide.this.id(this.path) + " needs a title");
            }
            final String id = ModGuide.this.claim(this.path);
            final String titleKey = ModGuide.this.key("section." + this.path, this.title);
            final int order = ModGuide.this.sections.size();
            final Supplier<? extends ItemLike> sectionIcon = this.icon;
            ModGuide.this.sections.add(() -> new GuideSection(id, order, titleKey, itemId(sectionIcon)));
            return new SectionRef(id);
        }
    }

    /**
     * An entry, declared: its title, what it covers, and its blocks in order. The five parts every entry of the series
     * follows each have their own method, which puts the part's heading before its words.
     */
    public final class EntryBuilder {

        private final String path;
        private final SectionRef section;
        private final List<Supplier<GuideBlock>> blocks = new ArrayList<>();
        private final List<Supplier<? extends ItemLike>> covers = new ArrayList<>();
        private final List<Supplier<? extends Collection<? extends ItemLike>>> families = new ArrayList<>();
        private final List<Supplier<? extends ItemLike>> shown = new ArrayList<>();
        private Supplier<? extends ItemLike> icon;
        private String title;
        private int paragraphs;
        private int headings;
        private int figures;
        private int tables;
        private int steps;
        private int warnings;
        private int problems;
        private int terms;
        private int notes;
        private int plans;
        private List<GuideBlock.TableRow> rows;
        private List<GuideBlock.Callout> callouts;
        private List<Supplier<GuideBlock.PlanPart>> planParts;

        private EntryBuilder(final String path, final SectionRef section) {
            this.path = path;
            this.section = section;
        }

        /** The entry's title, in English. */
        public EntryBuilder titled(final String english) {
            this.title = english;
            return this;
        }

        /** The item drawn beside it in lists and in the index. */
        public EntryBuilder icon(final Supplier<? extends ItemLike> icon) {
            this.icon = icon;
            return this;
        }

        /** An item this entry is the page of: the manual key on it opens here. */
        public EntryBuilder covers(final Supplier<? extends ItemLike> item) {
            this.covers.add(item);
            return this;
        }

        /**
         * Every item of a family this entry is the page of, such as every processor: the family is read when the
         * files are written, so it holds whatever the mod registered by then.
         */
        public EntryBuilder coversAll(final Supplier<? extends Collection<? extends ItemLike>> items) {
            this.families.add(items);
            return this;
        }

        /**
         * Items shown on the plate under the entry's title in place of those it is the page of: the parts an entry
         * about an idea talks about, such as everything a first computer is built from.
         */
        @SafeVarargs
        public final EntryBuilder shows(final Supplier<? extends ItemLike>... items) {
            this.shown.addAll(List.of(items));
            return this;
        }

        /** The first part of the spine: what the thing is, from zero. */
        public EntryBuilder whatItIs(final String english) {
            return this.spine(GuideTexts.WHAT_IT_IS.key(), "what", english);
        }

        /** The second part: what it is for. */
        public EntryBuilder whatItIsFor(final String english) {
            return this.spine(GuideTexts.WHAT_IT_IS_FOR.key(), "for", english);
        }

        /** The third part: how to get it. */
        public EntryBuilder howToGetIt(final String english) {
            return this.spine(GuideTexts.HOW_TO_GET_IT.key(), "get", english);
        }

        /**
         * The third part in a sentence many entries share, such as where every item of a mod comes from: declared
         * once as a {@link TextKey}, it is translated once.
         */
        public EntryBuilder howToGetIt(final TextKey shared) {
            final String key = shared.key();
            this.add(() -> new GuideBlock.Heading(GuideTexts.HOW_TO_GET_IT.key()));
            return this.add(() -> new GuideBlock.Paragraph(key));
        }

        /** The fourth part: how to use it, step by step. */
        public EntryBuilder howToUseIt(final String... english) {
            this.add(() -> new GuideBlock.Heading(GuideTexts.HOW_TO_USE_IT.key()));
            return this.stepList("use", english);
        }

        /** The fifth part: what can go wrong, each problem as a player sees it followed by its fix. */
        public EntryBuilder whatCanGoWrong(final String... problemThenFix) {
            return this.troubles(GuideTexts.WHAT_CAN_GO_WRONG.key(), problemThenFix);
        }

        /**
         * What can go wrong, at the end of an entry written as running text: under "If something goes wrong", each
         * problem as a player sees it followed by its fix.
         */
        public EntryBuilder ifSomethingGoesWrong(final String... problemThenFix) {
            return this.troubles(GuideTexts.IF_SOMETHING_GOES_WRONG.key(), problemThenFix);
        }

        /**
         * A paragraph. Its words may lead to another page: {@code [the words](namespace:path)} draws the words and
         * the target's number as a link, {@code [](namespace:path)} the number alone.
         */
        public EntryBuilder paragraph(final String english) {
            this.paragraphs++;
            final String key = this.key("text" + this.paragraphs, english);
            return this.add(() -> new GuideBlock.Paragraph(key));
        }

        /** A heading of the entry's own. */
        public EntryBuilder subheading(final String english) {
            this.headings++;
            final String key = this.key("heading" + this.headings, english);
            return this.add(() -> new GuideBlock.Heading(key));
        }

        /** A figure: an item drawn large, with its caption. */
        public EntryBuilder figure(final Supplier<? extends ItemLike> item, final String caption) {
            this.figures++;
            final String key = this.key("figure" + this.figures, caption);
            return this.add(() -> new GuideBlock.Figure(itemId(item), key));
        }

        /**
         * A picture the mod ships, numbered with the figures and captioned: a texture {@code namespace:path} under
         * {@code textures/}, drawn {@code width} by {@code height} (a width of 0 takes the column's).
         */
        public EntryBuilder picture(final String texture, final int width, final int height, final String caption) {
            this.figures++;
            final String key = this.key("figure" + this.figures, caption);
            return this.add(() -> new GuideBlock.Picture(texture, "", "{}", width, height, key));
        }

        /**
         * A picture a renderer draws as the page is shown, numbered with the figures and captioned: the kind
         * registered with {@code GuideBlockRenderers}, handed {@code data} (JSON), as wide as the column and
         * {@code height} tall.
         */
        public EntryBuilder drawing(final String kind, final int height, final String data, final String caption) {
            this.figures++;
            final String key = this.key("figure" + this.figures, caption);
            return this.add(() -> new GuideBlock.Picture("", kind, data, 0, height, key));
        }

        /** A table; its rows follow with {@link #property}, {@link #amount} and {@link #fixed}. */
        public EntryBuilder table(final String caption) {
            this.tables++;
            final String key = this.key("table" + this.tables, caption);
            final List<GuideBlock.TableRow> list = new ArrayList<>();
            this.rows = list;
            return this.add(() -> new GuideBlock.Table(key, list));
        }

        /** A row of the last table whose value is words, translated like any sentence. */
        public EntryBuilder property(final String label, final String value) {
            final String number = this.tables + "_" + (this.rowList().size() + 1);
            this.rowList().add(new GuideBlock.TableRow(this.key("table" + number, label),
                    new GuideBlock.GuideValue.Words(this.key("value" + number, value))));
            return this;
        }

        /** A row of the last table whose value is an amount with its unit, read from the code: "12,000 FE". */
        public EntryBuilder amount(final String label, final long value, final String unit) {
            final String number = this.tables + "_" + (this.rowList().size() + 1);
            this.rowList().add(new GuideBlock.TableRow(this.key("table" + number, label),
                    new GuideBlock.GuideValue.Amount(value, unit)));
            return this;
        }

        /** A row of the last table whose value is data, the same in every language: a model number, a bus. */
        public EntryBuilder fixed(final String label, final String value) {
            final String number = this.tables + "_" + (this.rowList().size() + 1);
            this.rowList().add(new GuideBlock.TableRow(this.key("table" + number, label),
                    new GuideBlock.GuideValue.Literal(value)));
            return this;
        }

        /** Every recipe of a type the game holds, or those making the item given. */
        public EntryBuilder recipes(final String type, final Supplier<? extends ItemLike> making) {
            return this.add(() -> new GuideBlock.Recipes(type, itemId(making)));
        }

        /** Every recipe of a type the game holds. */
        public EntryBuilder recipes(final String type) {
            return this.add(() -> new GuideBlock.Recipes(type, ""));
        }

        /** Numbered steps of the entry's own. */
        public EntryBuilder steps(final String... english) {
            return this.stepList("step", english);
        }

        /** A warning, in a box of its own. */
        public EntryBuilder warning(final String english) {
            this.warnings++;
            final String key = this.key("warning" + this.warnings, english);
            return this.add(() -> new GuideBlock.Warning(key));
        }

        /** A word explained where the entry first uses it; the index lists it. */
        public EntryBuilder define(final String term, final String definition) {
            this.terms++;
            final String termKey = this.key("term" + this.terms, term);
            final String definitionKey = this.key("definition" + this.terms, definition);
            return this.add(() -> new GuideBlock.Define(termKey, definitionKey));
        }

        /** Links to other entries, by their ids. */
        public EntryBuilder seeAlso(final String... entries) {
            final List<String> list = List.of(entries);
            return this.add(() -> new GuideBlock.SeeAlso(list));
        }

        /** A special block of a kind another mod draws, handed {@code data} (JSON). */
        public EntryBuilder custom(final String type, final int height, final String data) {
            return this.add(() -> new GuideBlock.Custom(type, height, data));
        }

        /** A note set apart in the style's accent: what to read next, a hint. */
        public EntryBuilder note(final String english) {
            this.notes++;
            final String key = this.key("note" + this.notes, english);
            return this.add(() -> new GuideBlock.Note(key));
        }

        /** A note many entries share, declared once as a {@link TextKey}. */
        public EntryBuilder note(final TextKey shared) {
            final String key = shared.key();
            return this.add(() -> new GuideBlock.Note(key));
        }

        /** What follows starts in the page's next column, or on the next page after its last column. */
        public EntryBuilder nextColumn() {
            return this.add(() -> new GuideBlock.Break(GuideBlock.BreakKind.COLUMN));
        }

        /** What follows starts on the next page: on a drawing, the next sheet. */
        public EntryBuilder nextPage() {
            return this.add(() -> new GuideBlock.Break(GuideBlock.BreakKind.PAGE));
        }

        /** The block seen from above, the front and the side; its balloons follow with {@link #callout}. */
        public EntryBuilder views(final Supplier<? extends ItemLike> block) {
            final List<GuideBlock.Callout> list = new ArrayList<>();
            this.callouts = list;
            return this.add(() -> new GuideBlock.Views(itemId(block), list));
        }

        /**
         * A numbered balloon of the last views, pointing at a place of a face, in its sixteen pixels, and what the
         * legend under the views says of it, in English.
         */
        public EntryBuilder callout(final int number, final GuideBlock.View view, final int u, final int v,
                                    final String english) {
            if (this.callouts == null) {
                throw new IllegalStateException("a balloon follows the views it points at");
            }
            final String key = this.key("callout" + (this.callouts.size() + 1), english);
            this.callouts.add(new GuideBlock.Callout(number, view, u, v, key));
            return this;
        }

        /** A numbered balloon whose line of the legend many entries share, declared once as a {@link TextKey}. */
        public EntryBuilder callout(final int number, final GuideBlock.View view, final int u, final int v,
                                    final TextKey shared) {
            if (this.callouts == null) {
                throw new IllegalStateException("a balloon follows the views it points at");
            }
            this.callouts.add(new GuideBlock.Callout(number, view, u, v, shared.key()));
            return this;
        }

        /** A plan of blocks seen from above, titled in English; its blocks follow with {@link #planPart}. */
        public EntryBuilder plan(final String caption) {
            this.plans++;
            return this.planUnder(this.key("plan" + this.plans, caption));
        }

        /** A plan titled in a sentence many entries share, declared once as a {@link TextKey}. */
        public EntryBuilder plan(final TextKey caption) {
            this.plans++;
            return this.planUnder(caption.key());
        }

        /** A block of the last plan, named in English under it. */
        public EntryBuilder planPart(final Supplier<? extends ItemLike> block, final String label) {
            return this.part(block, this.partKey(label), false);
        }

        /** A block of the last plan, named in words many entries share. */
        public EntryBuilder planPart(final Supplier<? extends ItemLike> block, final TextKey label) {
            return this.part(block, label.key(), false);
        }

        /** A place of the last plan that may be left empty, outlined in dots and named in English. */
        public EntryBuilder planOptional(final String label) {
            return this.part(null, this.partKey(label), true);
        }

        /** A place of the last plan that may be left empty, named in words many entries share. */
        public EntryBuilder planOptional(final TextKey label) {
            return this.part(null, label.key(), true);
        }

        public void register() {
            if (this.title == null) {
                throw new IllegalStateException("the entry " + ModGuide.this.id(this.path) + " needs a title");
            }
            final String id = ModGuide.this.claim(this.path);
            final String titleKey = this.key("title", this.title);
            final int order = ModGuide.this.entries.size();
            final List<Supplier<GuideBlock>> declared = List.copyOf(this.blocks);
            final List<Supplier<? extends ItemLike>> items = List.copyOf(this.covers);
            final List<Supplier<? extends Collection<? extends ItemLike>>> groups = List.copyOf(this.families);
            final List<Supplier<? extends ItemLike>> showing = List.copyOf(this.shown);
            final Supplier<? extends ItemLike> entryIcon = this.icon;
            final String sectionId = this.section.id();
            ModGuide.this.entries.add(() -> {
                final Set<String> covered = new LinkedHashSet<>();
                items.forEach(item -> covered.add(itemId(item)));
                groups.forEach(group -> group.get().forEach(item -> covered.add(itemId(() -> item))));
                final List<String> shows = showing.stream().map(ModGuide::itemId).toList();
                return new GuideEntry(id, sectionId, order, titleKey, itemId(entryIcon), List.copyOf(covered),
                        shows, declared.stream().map(Supplier::get).toList());
            });
        }

        private EntryBuilder troubles(final String heading, final String... problemThenFix) {
            if (problemThenFix.length == 0 || problemThenFix.length % 2 != 0) {
                throw new IllegalArgumentException("what can go wrong is pairs of a problem and its fix");
            }
            this.add(() -> new GuideBlock.Heading(heading));
            final List<GuideBlock.Problem> list = new ArrayList<>();
            for (int i = 0; i < problemThenFix.length; i += 2) {
                this.problems++;
                list.add(new GuideBlock.Problem(this.key("problem" + this.problems, problemThenFix[i]),
                        this.key("fix" + this.problems, problemThenFix[i + 1])));
            }
            final GuideBlock block = new GuideBlock.Problems(list);
            return this.add(() -> block);
        }

        private EntryBuilder spine(final String heading, final String part, final String english) {
            final String key = this.key(part, english);
            this.add(() -> new GuideBlock.Heading(heading));
            return this.add(() -> new GuideBlock.Paragraph(key));
        }

        private EntryBuilder stepList(final String part, final String... english) {
            final List<String> keys = new ArrayList<>();
            for (final String step : english) {
                this.steps++;
                keys.add(this.key(part + this.steps, step));
            }
            final GuideBlock block = new GuideBlock.Steps(keys);
            return this.add(() -> block);
        }

        private EntryBuilder planUnder(final String key) {
            final List<Supplier<GuideBlock.PlanPart>> list = new ArrayList<>();
            this.planParts = list;
            return this.add(() -> new GuideBlock.Plan(key, list.stream().map(Supplier::get).toList()));
        }

        /** The key of the next block of the last plan, its English given. */
        private String partKey(final String english) {
            if (this.planParts == null) {
                throw new IllegalStateException("a plan's blocks follow the plan");
            }
            return this.key("plan" + this.plans + "_" + (this.planParts.size() + 1), english);
        }

        private EntryBuilder part(final Supplier<? extends ItemLike> block, final String key,
                                  final boolean optional) {
            if (this.planParts == null) {
                throw new IllegalStateException("a plan's blocks follow the plan");
            }
            this.planParts.add(() -> new GuideBlock.PlanPart(itemId(block), key, optional));
            return this;
        }

        private List<GuideBlock.TableRow> rowList() {
            if (this.rows == null) {
                throw new IllegalStateException("a table's rows follow its table");
            }
            return this.rows;
        }

        private EntryBuilder add(final Supplier<GuideBlock> block) {
            this.blocks.add(block);
            return this;
        }

        private String key(final String part, final String english) {
            return ModGuide.this.key(this.path + "." + part, english);
        }
    }

    /** A manual, declared. */
    public final class ManualBuilder {

        private final String path;
        private final List<String> cover = new ArrayList<>();
        private final List<String> chapters = new ArrayList<>();
        private final List<String> about = new ArrayList<>();
        private String title;
        private String edition = "";
        private String partNumber = "";
        private String style;
        private int priority;
        private String icon = "";

        private ManualBuilder(final String path) {
            this.path = path;
        }

        /**
         * The mark printed on its cover: a texture of 32 by 32 pixels, named as a model names one ({@code
         * jsc:gui/guide/cover_mark} is {@code assets/jsc/textures/gui/guide/cover_mark.png}).
         */
        public ManualBuilder icon(final String texture) {
            this.icon = Objects.requireNonNull(texture, "texture");
            return this;
        }

        /** The manual's title, in English. */
        public ManualBuilder titled(final String english) {
            this.title = english;
            return this;
        }

        /** The lines of its cover's label above the title, in English. */
        public ManualBuilder cover(final String... english) {
            for (final String line : english) {
                this.cover.add(ModGuide.this.key(this.what("cover" + (this.cover.size() + 1)), line));
            }
            return this;
        }

        /** The line under the title on its cover, in English: "First Edition". */
        public ManualBuilder edition(final String english) {
            this.edition = ModGuide.this.key(this.what("edition"), english);
            return this;
        }

        /** The part number on its cover, which reads the same in every language. */
        public ManualBuilder partNumber(final String number) {
            this.partNumber = number;
            return this;
        }

        /** The style it is drawn in. */
        public ManualBuilder style(final String style) {
            this.style = style;
            return this;
        }

        /** The namespaces of the chapters it holds, in their own order; {@code *} for every chapter. */
        public ManualBuilder chapters(final String... namespaces) {
            this.chapters.addAll(List.of(namespaces));
            return this;
        }

        /** The paragraphs of its "About this manual" page, in English. */
        public ManualBuilder about(final String... english) {
            for (final String paragraph : english) {
                this.about.add(ModGuide.this.key(this.what("about" + (this.about.size() + 1)), paragraph));
            }
            return this;
        }

        /**
         * Which manual the manual key opens when several alike hold an item's entry: the highest. A manual that names
         * the entry's chapter comes before one that holds every chapter, whatever their priorities.
         */
        public ManualBuilder priority(final int priority) {
            this.priority = priority;
            return this;
        }

        /** Declares the manual, and gives back its id. */
        public String register() {
            if (this.title == null || this.style == null || this.chapters.isEmpty()) {
                throw new IllegalStateException("the manual " + ModGuide.this.id(this.path)
                        + " needs a title, a style and its chapters");
            }
            final String id = ModGuide.this.id(this.path);
            final String titleKey = ModGuide.this.key(this.what("title"), this.title);
            ModGuide.this.manuals.add(new GuideManual(id, titleKey, this.cover, this.edition, this.partNumber,
                    this.style, this.chapters, this.about, this.priority, this.icon));
            return id;
        }

        /** A key of this manual's, kept apart from the entries' keys. */
        private String what(final String part) {
            return "manual." + this.path + "." + part;
        }
    }
}
