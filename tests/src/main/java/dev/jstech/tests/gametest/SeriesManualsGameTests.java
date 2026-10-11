/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import com.google.gson.JsonElement;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dev.jstech.computers.guide.ComputersBooks;
import dev.jstech.computers.registry.ComputingContent;
import dev.jstech.core.client.guide.GuideLibrary;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.content.ModContent;
import dev.jstech.core.guide.CoreGuide;
import dev.jstech.core.guide.GuideBlock;
import dev.jstech.core.guide.GuideBook;
import dev.jstech.core.guide.GuideChapter;
import dev.jstech.core.guide.GuideCodecs;
import dev.jstech.core.guide.GuideContents;
import dev.jstech.core.guide.GuideEntry;
import dev.jstech.core.guide.GuideFacts;
import dev.jstech.core.guide.GuideGifts;
import dev.jstech.core.guide.GuideLayout;
import dev.jstech.core.guide.GuideLinks;
import dev.jstech.core.guide.GuideManual;
import dev.jstech.core.guide.GuidePiece;
import dev.jstech.core.guide.GuideProse;
import dev.jstech.core.guide.GuideSection;
import dev.jstech.core.guide.GuideStyle;
import dev.jstech.core.guide.GuideTexts;
import dev.jstech.core.guide.IGuideText;
import dev.jstech.core.guide.ManualItem;
import dev.jstech.core.guide.ModGuide;
import dev.jstech.core.guide.TextSize;
import dev.jstech.core.registry.CoreItems;
import dev.jstech.core.text.TextFormat;
import dev.jstech.industrial.IndustrialModule;
import dev.jstech.tests.JsTests;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The series' own manuals as their mods declare them: the Technical Reference handed to a player once, as they first
 * join a world; every item of the series the page of some entry; every entry following the form of its chapter, the
 * five parts in their order or running text; every link in a sentence leading to a page that exists; and every entry,
 * the drawings' views and plans included, read back from its file the same.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SeriesManualsGameTests {

    private static final String ARENA = "empty";
    /** Fewer entries than this, and a chapter of the series was lost on its way to the manuals. */
    private static final int FEWEST_ENTRIES = 50;
    /** The mods of the series whose every item a manual explains. */
    private static final List<String> SERIES = List.of("jscore", "jsc", "jsindustrial");
    private static final List<String> SPINE = List.of(GuideTexts.WHAT_IT_IS.key(), GuideTexts.WHAT_IT_IS_FOR.key(),
            GuideTexts.HOW_TO_GET_IT.key(), GuideTexts.HOW_TO_USE_IT.key(), GuideTexts.WHAT_CAN_GO_WRONG.key());
    /** The chapters written as a guide in running text rather than in the five parts. */
    private static final Set<String> PROSE = Set.of("jsc");
    /** How many faults a failure names. */
    private static final int FAULTS_NAMED = 12;
    /**
     * The books whose running text is held to the series' rules: each joins as its entries are written to them, the
     * Guide to Operations once it is written anew.
     */
    private static final List<String> PROSE_RULED = List.of();
    /** An item drawn at its own size, before the scale it is drawn at. */
    private static final int ITEM = 16;
    /** The width of a cell of the code font, and the scales small, big and huge text are drawn at. */
    private static final int CODE_CELL = 5;
    private static final double SMALL_SCALE = 0.75;
    private static final double BIG_SCALE = 2.0;
    private static final double HUGE_SCALE = 4.0;

    private SeriesManualsGameTests() {
    }

    @GameTest(template = ARENA)
    public static void technicalReference_isHandedToAPlayerOnceAsTheyFirstJoin(final GameTestHelper helper) {
        final UUID id = UUID.randomUUID();
        final FakePlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(id, "manual-reader"));
        player.getInventory().clearContent();
        helper.assertFalse(GuideGifts.wasGiven(player, CoreGuide.TECHNICAL_REFERENCE),
                "a player new to the world has not been given the manual");
        GuideGifts.welcome(player);
        helper.assertTrue(count(player) == 1, "the first join hands them the Technical Reference");
        helper.assertTrue(player.getInventory().getItem(0).isEmpty(), "and leaves their hotbar as it was");
        helper.assertTrue(GuideGifts.wasGiven(player, CoreGuide.TECHNICAL_REFERENCE), "the world keeps that it did");
        player.getInventory().clearContent();
        GuideGifts.welcome(player);
        helper.assertTrue(count(player) == 0, "a player who put theirs away is not handed another");
        helper.succeed();
    }

    /*
     * Each book of the systems and the languages is an item that opens it, bound as a book rather than a binder, its
     * chapters marked on its pages and its cover drawn from its parts: every line the cover prints is one of the
     * manual's cover lines, and every picture on it a texture the mod ships.
     */
    @GameTest(template = ARENA)
    public static void computersBooks_areBoundEachItsOwnWayWithItsCoverAndItsItem(final GameTestHelper helper) {
        final GuideLibrary library = library();
        final List<ItemEntry<ManualItem>> books = List.of(ComputersBooks.FRAMES_RESOURCE_KIT,
                ComputersBooks.LINUX_DESK_REFERENCE, ComputersBooks.LINUX_BY_HAND, ComputersBooks.DAEMON_HANDBOOK,
                ComputersBooks.LEARNING_IQL, ComputersBooks.BOOK_OF_SIGMA);
        final Set<String> looks = new HashSet<>();
        for (final ItemEntry<ManualItem> book : books) {
            final String id = book.get().manual();
            final GuideManual manual = library.manual(id).orElse(null);
            helper.assertTrue(manual != null, book.getId() + " opens a manual there is: " + id);
            final GuideStyle style = library.style(manual.style()).orElse(null);
            helper.assertTrue(style != null && style.cover().kind() == GuideStyle.CoverKind.BOOK
                    && !style.cover().parts().isEmpty(), id + " has a book's cover, drawn from its parts");
            helper.assertTrue(!style.rings() && style.make().marks() != GuideStyle.Marks.TABS,
                    id + " is bound as a book, its chapters marked on its pages");
            helper.assertTrue(!manual.chapters().isEmpty() && manual.chapters().stream()
                    .allMatch(chapter -> library.chapter(chapter).isPresent()), id + " names its declared chapters");
            for (final GuideStyle.CoverPart part : style.cover().parts()) {
                helper.assertTrue(part.words() <= manual.coverKeys().size(),
                        id + "'s cover prints line " + part.words() + " of " + manual.coverKeys().size());
                if (!part.image().isEmpty()) {
                    helper.assertTrue(ships(ResourceLocation.parse(part.image())),
                            id + "'s cover shows " + part.image() + ", a texture the mod does not ship");
                }
            }
            looks.add(manual.style());
        }
        helper.assertTrue(looks.size() == books.size(), "each book is dressed its own way: " + looks);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void seriesManuals_explainEveryItemOfTheSeries(final GameTestHelper helper) {
        final Set<String> covered = new HashSet<>();
        for (final ModGuide guide : guides()) {
            for (final GuideEntry entry : guide.declaredEntries()) {
                covered.addAll(entry.items());
            }
        }
        final List<String> missing = new ArrayList<>();
        for (final ResourceLocation item : BuiltInRegistries.ITEM.keySet()) {
            if (SERIES.contains(item.getNamespace()) && !covered.contains(item.toString())) {
                missing.add(item.toString());
            }
        }
        helper.assertTrue(missing.isEmpty(), "every item of the series is the page of an entry; none for " + missing);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void seriesEntries_followTheFormOfTheirChapter(final GameTestHelper helper) {
        final List<String> wrong = new ArrayList<>();
        for (final ModGuide guide : guides()) {
            // J's Computers' chapter is a guide in running text; the others' entries follow the five parts.
            final boolean prose = PROSE.contains(guide.namespace());
            for (final GuideEntry entry : guide.declaredEntries()) {
                final List<String> spine = entry.blocks().stream()
                        .filter(part -> part instanceof GuideBlock.Heading heading && SPINE.contains(heading.key()))
                        .map(part -> ((GuideBlock.Heading) part).key()).toList();
                final boolean written = entry.blocks().stream().anyMatch(part -> part instanceof GuideBlock.Paragraph);
                if (prose ? !spine.isEmpty() || !written : !spine.equals(SPINE)) {
                    wrong.add(entry.id() + " " + spine);
                }
            }
        }
        helper.assertTrue(wrong.isEmpty(), "every entry of a chapter in the five parts says what it is, what for, how"
                + " to get it, how to use it and what can go wrong, in that order, and every entry of a chapter in"
                + " running text has its paragraphs and none of those headings; not " + wrong);
        helper.succeed();
    }

    /*
     * Every link of every book leads to a page some book holds, from one book into another too: the links in its
     * sentences, and the entries its "See" lines name. The chapters of every mod count, so a link into another mod's
     * book is followed there.
     */
    @GameTest(template = ARENA)
    public static void seriesLinks_leadToPagesThatExist(final GameTestHelper helper) {
        final Set<String> pages = new HashSet<>();
        for (final ModGuide guide : allGuides()) {
            pages.add(guide.namespace());
            guide.declaredOtherChapters().forEach(chapter -> pages.add(chapter.id()));
            guide.declaredSections().forEach(section -> pages.add(section.id()));
            guide.declaredEntries().forEach(entry -> pages.add(entry.id()));
        }
        final List<String> broken = new ArrayList<>();
        int links = 0;
        for (final ModGuide guide : allGuides()) {
            for (final Map.Entry<String, String> sentence : guide.translations().entrySet()) {
                for (final String target : GuideLinks.targets(sentence.getValue())) {
                    links++;
                    if (!pages.contains(target)) {
                        broken.add(sentence.getKey() + " -> " + target);
                    }
                }
            }
            for (final GuideEntry entry : guide.declaredEntries()) {
                for (final GuideBlock block : entry.blocks()) {
                    if (block instanceof GuideBlock.SeeAlso see) {
                        for (final String target : see.entries()) {
                            links++;
                            if (!pages.contains(target)) {
                                broken.add(entry.id() + " sees " + target);
                            }
                        }
                    }
                }
            }
        }
        helper.assertTrue(links > 0, "the series' sentences lead to other pages");
        helper.assertTrue(broken.isEmpty(), "every link leads to a page some book holds; not " + broken);
        helper.succeed();
    }

    /*
     * Every book of every mod, laid out as a player's game lays it (its words measured as the game's font measures
     * them, near enough): no table cuts a word, nothing is drawn under a page's foot, and no page is left empty between
     * two that are not.
     */
    @GameTest(template = ARENA, timeoutTicks = 600)
    public static void everyBook_laysOutWithNoWordCutNothingUnderItsFootAndNoEmptyPage(final GameTestHelper helper) {
        final GuideLibrary library = library();
        final IGuideText text = new MeasuredText();
        final List<String> wrong = new ArrayList<>();
        int books = 0;
        for (final GuideManual manual : library.manuals()) {
            final GuideStyle style = library.style(manual.style()).orElse(null);
            if (style == null) {
                wrong.add(manual.id() + " names no style the game has: " + manual.style());
                continue;
            }
            books++;
            final GuideBook book = GuideLayout.lay(manual, style, library.contentsOf(manual), text);
            if (!book.cut().isEmpty()) {
                wrong.add(manual.id() + " cuts " + book.cut());
            }
            for (int index = 0; index < book.pages().size(); index++) {
                final GuideBook.Page page = book.pages().get(index);
                if (page.pieces().isEmpty() && index + 1 < book.pages().size()) {
                    wrong.add(manual.id() + " leaves page " + page.folio() + " empty");
                }
                for (final GuidePiece piece : page.pieces()) {
                    final int column = piece.x() >= GuideLayout.columnX(style, 1) && style.pages().columns() > 1
                            ? 1 : 0;
                    if (bottomOf(piece, style) > GuideLayout.bottom(style, column)) {
                        wrong.add(manual.id() + " draws under the foot of page " + page.folio() + ": " + piece);
                    }
                }
            }
        }
        helper.assertTrue(books > 1, "the books are laid out: " + books);
        helper.assertTrue(wrong.isEmpty(), wrong.size() + " faults: "
                + String.join("; ", wrong.subList(0, Math.min(FAULTS_NAMED, wrong.size()))));
        helper.succeed();
    }

    /*
     * The books written to the series' rules of running text: every entry speaks to its reader, every number says what
     * it measures, and every word is explained in the first entry that uses it. A book is held to them once its
     * entries are written to them.
     */
    @GameTest(template = ARENA)
    public static void seriesProse_speaksToTheReaderMeasuresItsNumbersAndExplainsFirst(final GameTestHelper helper) {
        final GuideLibrary library = library();
        final IGuideText text = new MeasuredText();
        final List<String> wrong = new ArrayList<>();
        for (final String id : PROSE_RULED) {
            final GuideManual manual = library.manual(id).orElse(null);
            if (manual == null) {
                wrong.add("no book " + id);
                continue;
            }
            final GuideContents contents = library.contentsOf(manual);
            for (final GuideContents.Chapter chapter : contents.chapters()) {
                for (final GuideContents.Section section : chapter.sections()) {
                    for (final GuideEntry entry : section.entries()) {
                        final String words = GuideProse.wordsOf(entry, text);
                        if (!words.isBlank() && !GuideProse.speaksToTheReader(words)) {
                            wrong.add(entry.id() + " never speaks to its reader");
                        }
                        final List<String> bare = GuideProse.bareNumbers(words);
                        if (!bare.isEmpty()) {
                            wrong.add(entry.id() + " says " + bare + " without what they measure");
                        }
                    }
                }
            }
            for (final GuideProse.Early early : GuideProse.usedBeforeExplained(contents, text)) {
                wrong.add(early.usedIn() + " uses " + early.term() + " before " + early.explainedIn()
                        + " explains it");
            }
        }
        helper.assertTrue(wrong.isEmpty(), wrong.size() + " faults: "
                + String.join("; ", wrong.subList(0, Math.min(FAULTS_NAMED, wrong.size()))));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void seriesDeclarations_readBackFromTheirFilesTheSame(final GameTestHelper helper) {
        int entries = 0;
        for (final ModGuide guide : guides()) {
            for (final GuideEntry entry : guide.declaredEntries()) {
                helper.assertTrue(roundTrips(GuideCodecs.ENTRY, entry), entry.id() + " reads back the same");
                entries++;
            }
            guide.declaredManuals().forEach(manual -> helper.assertTrue(roundTrips(GuideCodecs.MANUAL, manual),
                    manual.id() + " reads back the same"));
            guide.declaredStyles().forEach((path, style) -> helper.assertTrue(roundTrips(GuideCodecs.STYLE, style),
                    guide.namespace() + ":" + path + " reads back the same"));
        }
        helper.assertTrue(entries > FEWEST_ENTRIES, "the series' chapters hold their entries; found " + entries);
        helper.succeed();
    }

    /** Whether the mod of a texture's namespace ships it, as {@code textures/<path>.png}. */
    private static boolean ships(final ResourceLocation texture) {
        final String file = "assets/" + texture.getNamespace() + "/textures/" + texture.getPath() + ".png";
        return Files.exists(ModList.get().getModFileById(texture.getNamespace()).getFile()
                .findResource(file.split("/")));
    }

    /** What every mod writes in the manuals, the test mod's among them. */
    private static List<ModGuide> allGuides() {
        final List<ModGuide> guides = new ArrayList<>();
        for (final ModContent content : ModContent.all()) {
            guides.addAll(content.declaredGuides());
        }
        return guides;
    }

    /** Every book every mod declares, gathered as the game gathers them from their files. */
    private static GuideLibrary library() {
        final Map<String, GuideChapter> chapters = new HashMap<>();
        final Map<String, GuideSection> sections = new HashMap<>();
        final Map<String, GuideEntry> entries = new HashMap<>();
        final Map<String, GuideManual> manuals = new HashMap<>();
        final Map<String, GuideStyle> styles = new HashMap<>();
        for (final ModGuide guide : allGuides()) {
            final GuideChapter main = guide.declaredChapter();
            if (main != null) {
                chapters.put(main.id(), main);
            }
            guide.declaredOtherChapters().forEach(chapter -> chapters.put(chapter.id(), chapter));
            guide.declaredSections().forEach(section -> sections.put(section.id(), section));
            guide.declaredEntries().forEach(entry -> entries.put(entry.id(), entry));
            guide.declaredManuals().forEach(manual -> manuals.put(manual.id(), manual));
            guide.declaredStyles().forEach((path, style) -> styles.put(guide.namespace() + ":" + path, style));
        }
        return new GuideLibrary(chapters, sections, entries, manuals, styles);
    }

    /** How far down the page a piece reaches. */
    private static int bottomOf(final GuidePiece piece, final GuideStyle style) {
        return switch (piece) {
            case GuidePiece.Text text -> text.y() + text.size().lineHeight();
            case GuidePiece.Leader leader -> leader.y() + leader.size().lineHeight();
            case GuidePiece.Rule rule -> rule.y() + 1;
            case GuidePiece.Dots dots -> dots.y() + 1;
            case GuidePiece.Key key -> key.y() + TextSize.BODY.lineHeight();
            case GuidePiece.Box box -> box.y() + box.height();
            case GuidePiece.Item item -> item.y() + ITEM * item.scale();
            case GuidePiece.Recipes rows -> rows.y() + rows.count() * GuideLayout.recipeRow(style);
            case GuidePiece.Custom custom -> custom.y() + custom.height();
            case GuidePiece.Listing listing -> listing.y() + listing.height();
            case GuidePiece.Session session -> session.y() + session.height();
            case GuidePiece.Plate plate -> plate.y() + GuideLayout.PLATE_HEIGHT;
            case GuidePiece.Picture picture -> picture.y() + picture.height();
            case GuidePiece.Views views -> views.y() + GuideLayout.VIEWS_HEIGHT;
            case GuidePiece.Plan plan -> plan.y() + GuideLayout.PLAN_HEIGHT;
            case GuidePiece.Scene scene -> scene.y() + scene.height();
            case GuidePiece.SceneKey key -> key.y() + key.height();
        };
    }

    /** What the series writes in the manuals: the Core's and the series' own, the Computers' and the Industrial's. */
    private static List<ModGuide> guides() {
        final List<ModGuide> guides = new ArrayList<>();
        for (final ModContent content : List.of(CoreItems.CONTENT, ComputingContent.CONTENT,
                IndustrialModule.CONTENT)) {
            guides.addAll(content.declaredGuides());
        }
        return guides;
    }

    private static int count(final FakePlayer player) {
        int found = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            final ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(CoreGuide.MANUAL.get())) {
                found += stack.getCount();
            }
        }
        return found;
    }

    private static <T> boolean roundTrips(final Codec<T> codec, final T value) {
        final JsonElement written = codec.encodeStart(JsonOps.INSTANCE, value).getOrThrow();
        return value.equals(codec.parse(JsonOps.INSTANCE, written).getOrThrow());
    }

    /**
     * The manuals' words in the language the server has loaded, which is English, measured near enough as the game's
     * font measures them: each letter its own width, small text three quarters of that, big text twice, bold a pixel a
     * letter wider, and code a cell of five pixels a character.
     */
    private record MeasuredText() implements IGuideText {

        @Override
        public String text(final String key, final Object... args) {
            final String pattern = Language.getInstance().getOrDefault(key, key);
            if (args.length == 0) {
                return GuideFacts.fill(pattern, this);
            }
            final List<String> written = new ArrayList<>();
            for (final Object arg : args) {
                written.add(String.valueOf(arg));
            }
            return GuideFacts.fill(TextFormat.apply(pattern, written), this);
        }

        @Override
        public int width(final String text, final TextSize size) {
            if (size == TextSize.CODE) {
                return CODE_CELL * text.length();
            }
            int width = 0;
            for (int i = 0; i < text.length(); i++) {
                width += advance(text.charAt(i));
            }
            final boolean bold = size == TextSize.BOLD || size == TextSize.SMALL_BOLD || size == TextSize.HEADING;
            if (bold) {
                width += text.length();
            }
            return (int) Math.ceil(width * switch (size) {
                case SMALL, SMALL_BOLD -> SMALL_SCALE;
                case TITLE, LETTER -> BIG_SCALE;
                case CHAPTER -> HUGE_SCALE;
                default -> 1.0;
            });
        }

        @Override
        public int recipeCount(final String type, final String output) {
            return 0;
        }

        @Override
        public String amount(final long value, final String unit) {
            return unit.isEmpty() ? String.valueOf(value) : value + " " + unit;
        }

        /** How far the game's font moves on for a letter, its gap after it included. */
        private static int advance(final char letter) {
            return switch (letter) {
                case '!', ',', '.', ':', ';', '|', 'i' -> 2;
                case '\'', '`', 'l' -> 3;
                case ' ', 'I', 't', '[', ']' -> 4;
                case '"', '(', ')', '*', '<', '>', '{', '}', 'f', 'k' -> 5;
                case '@', '~' -> 7;
                default -> 6;
            };
        }
    }
}
