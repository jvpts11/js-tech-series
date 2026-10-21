/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.guide;

import com.google.gson.JsonParser;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import dev.jstech.core.api.client.GuideBlockRenderers;
import dev.jstech.core.api.client.IGuideBlockRenderer;
import dev.jstech.core.client.audio.AudioEngine;
import dev.jstech.core.client.gui.PointerShape;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.client.motion.MotionClock;
import dev.jstech.core.client.recipeview.ProcessingRecipeViews;
import dev.jstech.core.client.recipeview.RecipeViewers;
import dev.jstech.core.gui.ManualButtonLook;
import dev.jstech.core.guide.GuideBlock;
import dev.jstech.core.guide.GuideBook;
import dev.jstech.core.guide.GuideChapter;
import dev.jstech.core.guide.GuideCode;
import dev.jstech.core.guide.GuideContents;
import dev.jstech.core.guide.GuideEntry;
import dev.jstech.core.guide.GuideHighlighters;
import dev.jstech.core.guide.GuideHistory;
import dev.jstech.core.guide.GuideLayout;
import dev.jstech.core.guide.GuideManual;
import dev.jstech.core.guide.GuidePiece;
import dev.jstech.core.guide.GuideSearch;
import dev.jstech.core.guide.GuideSounds;
import dev.jstech.core.guide.GuideStyle;
import dev.jstech.core.guide.GuideTexts;
import dev.jstech.core.guide.LecternTakePayload;
import dev.jstech.core.guide.ReaderMarks;
import dev.jstech.core.guide.TearPayload;
import dev.jstech.core.guide.TextSize;
import dev.jstech.core.guide.TornSheets;
import dev.jstech.core.progression.KnowledgeGate;
import dev.jstech.core.gui.layout.ManualScreenLayout;
import dev.jstech.core.palette.PaletteRoles;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * An open manual: the binder or the folder in its style, one page or two at a time, with the chapter tabs at a
 * binder's edge, the contents, back, forward and search buttons on its top edge and the arrows on its sides.
 *
 * <p>It opens at its cover, at the page of an entry, or where the player left it, the game closed in between.
 * Clicking a line of the contents, a link, a line of the search or a chapter's tab goes there, and Back, the mouse's
 * back button or Backspace returns, Forward comes back again; the arrows, the arrow keys, Page Up and Page Down and
 * the mouse wheel turn the pages; the search button turns the left page into a search of the whole manual that finds
 * as the player types. Every control lights under the pointer, which turns into a hand over what takes a click, and
 * the game's tooltip says where a button leads. Pointing at an item shows its name and what the game says of it.
 *
 * <p>A style of drawings draws its sheets as drawings are drawn: the grid, the frame with its zones, and the title
 * block in the corner in place of a page's head and foot.
 */
public final class ManualScreen extends Screen {

    /* The manual and its style, read again from the resource packs each time the screen is laid out. */
    private GuideManual manual;
    private GuideStyle style;
    private final String openAt;
    /* The screen it was opened from, which closing it goes back to; none for the game itself. */
    @Nullable
    private final Screen parent;
    /* The lectern it is read from, or none; and whether it is laid out only to draw a page apart, as a torn sheet. */
    @Nullable
    private final BlockPos lectern;
    private final boolean sheet;
    private final GuideRecipes recipes = new GuideRecipes();
    private final GuideFaces faces = new GuideFaces();
    private final Map<String, Integer> colours = new HashMap<>();
    private final Map<String, Integer> tabColours = new HashMap<>();
    private final Map<String, CompoundTag> customData = new HashMap<>();
    /** The colours of each terminal a session names, read once when the manual's colours are. */
    private final Map<String, Map<String, Integer>> terminals = new HashMap<>();
    private final List<String> chapters = new ArrayList<>();
    /** Where the reader jumped from in this manual: the book's own way back. */
    private final GuideHistory history;
    private GuideBook book;
    private ManualScreenLayout.Geometry geometry;
    private int left;
    private int top;
    /** The spread shown: -1 is the closed cover, then 0 for the first page or pair of pages. */
    private int spread = -1;
    /** Whether the manual has been laid out once and put at its first page. */
    private boolean placed;
    /** The spread last drawn, so a page is heard turning whatever turned it; unset until the first is drawn. */
    private int heard = UNSET;
    private boolean searching;
    private EditBox searchField;
    /** What the search found the last time it was asked, for the book and the words it was asked with. */
    private List<GuideSearch.Hit> foundCache = List.of();
    private GuideBook foundBook;
    private String foundWords;
    private ItemStack hoveredItem = ItemStack.EMPTY;
    private List<Component> hoveredWords = List.of();
    /* What a click opens a recipe viewer on, the pointer over a recipe of a page: what it makes, or its slot's item. */
    private ItemStack recipeUnder = ItemStack.EMPTY;
    /** The scenes of the manual, as the reader has turned them; made with the screen's first layout. */
    private ManualScenes scenes;
    /* The reader's bookmarks and notes, the contents the marks are read from, and what the reader has seen. */
    private ManualMarksView marks;
    private GuideContents contents;
    private Set<String> seenNow = Set.of();
    private int seenSpread = UNSET;
    /** The spread a ribbon keeps: the one the book was put down at last time, or none. */
    private int kept = UNSET;
    /** The control held down under the pointer, drawn pressed until the button is let go; null when none is. */
    @Nullable
    private ManualScreenLayout.Rect pressed;
    /** The listing opened over the whole book, its lines' colours, and how many lines down it is scrolled. */
    @Nullable
    private GuidePiece.Listing opened;
    private List<List<GuideCode.Run>> openedRuns = List.of();
    private int openedScroll;
    /** How many characters across it is scrolled, for a line longer than the book is wide. */
    private int openedShift;
    /** The lines of the listing last copied, and when, so its button says so for a moment. */
    private List<String> copied = List.of();
    private long copiedAt;

    /** How long a tick of the game is, by which an animated picture's frames are timed. */
    private static final long MILLIS_PER_TICK = 50L;
    /** The dot on a chapter's tab that holds something new. */
    private static final int TAB_DOT = 3;
    /** Where a page of the front matter, the contents, stands: by its page, before every entry. */
    private static final String FRONT = "#front";
    /** The size a page apart is laid out at, to be drawn as a torn sheet: any size the binder fits. */
    private static final int SHEET_ROOM_WIDTH = 640;
    private static final int SHEET_ROOM_HEIGHT = 360;
    /** How far below the lines of the index its dots sit, at the size they are drawn. */
    private static final int DOT_DROP = 6;
    private static final int DOT_PITCH = 3;
    /**
     * The row a link's underline is drawn on, the one under the letters' foot, as the game underlines text: a row
     * lower would touch the tall letters of the line below and read as a line struck through them.
     */
    private static final int LINK_DROP = 8;
    /** How far the rings reach into each page: past the gutter's shade, short of the text. */
    private static final int RING_SPAN = 8;
    private static final int[] RINGS = {28, 96, 164};
    /** The rivets down a closed binder's spine, and how wide the spine is. */
    private static final int[] RIVETS = {38, 106, 174};
    private static final int SPINE_WIDTH = 14;
    private static final int SLOT = 18;
    private static final int SLOT_GAP = 2;
    private static final int NAME_ROOM = 52;
    private static final int ICON_SCALE_PERCENT = 75;
    private static final int LABEL_PAD = 10;
    private static final int LABEL_WIDTH = 136;
    private static final int FOLDER_LABEL_WIDTH = 128;
    private static final int MARK = 32;
    private static final int CYCLE_MILLIS = 1000;
    /** How far apart the items of a plate stand, and how long it shows a row before turning to the next. */
    private static final int PLATE_PITCH = 18;
    private static final int PLATE_TURN_MILLIS = 2000;
    /** A drawing's grid, and its frame's zones: six across, four down. */
    private static final int GRID = 10;
    private static final int ZONES_ACROSS = 6;
    private static final int ZONES_DOWN = 4;
    /** The title block's rows and the widths of its bottom row's cells. */
    private static final int BLOCK_ROW = 10;
    private static final int BLOCK_LAST_ROW = 22;
    private static final int[] BLOCK_CELLS = {54, 62, 34};
    /** Every set of drawings is at its first revision. */
    private static final String REVISION = "A";
    private static final String SPACED_DASH = "  -  ";
    /** The balloons of a block's views: their radius, and where the first stands beside the top view. */
    private static final int BALLOON = 5;
    private static final int BALLOON_X = 80;
    private static final int BALLOON_STEP = 48;
    private static final int BALLOON_ROOM = 48;
    /** The highlight behind a link under the pointer: its colour most of the way to white. */
    private static final double LINK_LIGHT = 0.8;
    /** How far a line the search found is set in under its entry's title. */
    private static final int LINE_INDENT = 10;
    /** No spread drawn yet. */
    private static final int UNSET = Integer.MIN_VALUE;
    /** The least room between a page's folio and the manual's title at its foot. */
    private static final int FOOT_GAP = 10;
    /** A listing's Copy button, its Close button over the whole book, and how long Copy says it has copied. */
    private static final int COPY_WIDTH = GuideLayout.LISTING_COPY;
    private static final int CLOSE_WIDTH = 26;
    private static final int LISTING_BUTTON = 8;
    private static final long COPIED_MILLIS = 2000L;
    /** How far a listing's button face goes from its strip towards white. */
    private static final double BUTTON_LIGHT = 0.35;
    /** A listing opened over the whole book: where it stands, and the room kept under it. */
    private static final int OPENED_X = 14;
    private static final int OPENED_Y = 26;
    private static final int OPENED_FOOT = 20;
    /** How many characters a turn of the wheel scrolls a listing across. */
    private static final int SHIFT_STEP = 4;
    /** A terminal's colours by their roles, and how far its strip's ground goes from the console's towards white. */
    private static final String TERMINAL_GROUND = "ground";
    private static final String TERMINAL_TEXT = "text";
    private static final String TERMINAL_PROMPT = "prompt";
    private static final String TERMINAL_TYPED = "typed";
    private static final double STRIP_LIGHT = 0.18;
    /** A key in a sentence: how tall it stands, and how far its face goes from the paper towards white. */
    private static final int KEY_HEIGHT = 9;
    private static final double KEY_LIGHT = 0.5;
    /** The mouse's side buttons, back and forward, as the game numbers them. */
    private static final int MOUSE_BACK = GLFW.GLFW_MOUSE_BUTTON_4;
    private static final int MOUSE_FORWARD = GLFW.GLFW_MOUSE_BUTTON_5;
    /* The way back of each manual, by its id, for as long as the game runs: the history is the book's own. */
    private static final Map<String, GuideHistory> HISTORIES = new HashMap<>();

    public ManualScreen(final GuideManual manual, final GuideStyle style, final String openAt) {
        this(manual, style, openAt, null);
    }

    /** The manual opened from {@code parent}, a settings screen's link, which closing it goes back to. */
    public ManualScreen(final GuideManual manual, final GuideStyle style, final String openAt,
                        @Nullable final Screen parent) {
        this(manual, style, openAt, parent, null, false);
    }

    private ManualScreen(final GuideManual manual, final GuideStyle style, final String openAt,
                         @Nullable final Screen parent, @Nullable final BlockPos lectern, final boolean sheet) {
        super(GameText.component(GuideTexts.CONTENTS));
        this.manual = manual;
        this.style = style;
        this.openAt = openAt == null ? "" : openAt;
        this.parent = parent;
        this.lectern = lectern;
        this.sheet = sheet;
        this.history = HISTORIES.computeIfAbsent(manual.id(), id -> new GuideHistory());
    }

    /**
     * The manual read from the lectern at {@code pos}, where it was last put down: a strip under it says where the
     * lectern is, with the buttons that take the manual off it and that close it.
     */
    public static ManualScreen onLectern(final GuideManual manual, final GuideStyle style, final BlockPos pos) {
        return new ManualScreen(manual, style, "", null, pos, false);
    }

    /**
     * The manual laid out to draw one page apart, as a torn sheet shows it: never shown as a screen, it makes no sound
     * and leaves no mark of its reader.
     */
    static ManualScreen sheet(final GuideManual manual, final GuideStyle style) {
        final ManualScreen laid = new ManualScreen(manual, style, "", null, null, true);
        laid.init(Minecraft.getInstance(), SHEET_ROOM_WIDTH, SHEET_ROOM_HEIGHT);
        return laid;
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    @Override
    protected void init() {
        final GuideLibrary library = GuideLibrary.loaded();
        // After the resource packs are read again, the manual is laid out as they hold it now, in the style they give.
        library.manual(this.manual.id()).ifPresent(now -> library.style(now.style()).ifPresent(look -> {
            if (!look.equals(this.style) && this.scenes != null && this.marks != null) {
                this.scenes.close();
                this.scenes = null;
                this.marks.keep();
                this.marks = null;
            }
            this.manual = now;
            this.style = look;
        }));
        this.resolveColours();
        final GuideContents contents = library.contentsOf(this.manual);
        this.chapters.clear();
        final Set<String> mods = new HashSet<>();
        for (final GuideContents.Chapter chapter : contents.chapters()) {
            final GuideChapter held = chapter.chapter();
            this.tabColours.put(held.id(), this.tabColour(held));
            // A book has a tab a chapter; a manual holding every chapter, a tab a mod, leading to its first chapter.
            if (!this.manual.holdsEvery() || mods.add(held.namespace())) {
                this.chapters.add(held.id());
            }
        }
        final int keep = this.spread;
        this.book = GuideLayout.lay(this.manual, this.style, contents,
                new ClientGuideText(this.font, this.style, this.recipes));
        // Made once, so how the reader turned a scene outlasts the window being resized.
        if (this.scenes == null) {
            this.scenes = new ManualScenes(this.font, this.style, this::colour,
                    new ClientGuideText(this.font, this.style, this.recipes));
        }
        if (this.marks == null) {
            this.marks = new ManualMarksView(this.manual.id(), this.font, this.style,
                    new ClientGuideText(this.font, this.style, this.recipes));
        }
        this.contents = contents;
        // The first time a reader opens a manual, what it holds then is what they find there; only what comes into
        // it after is new.
        if (!this.sheet && !ManualMarks.opened(this.manual.id())) {
            final List<String> every = new ArrayList<>();
            contents.chapters().forEach(chapter -> chapter.sections().forEach(section -> section.entries()
                    .forEach(entry -> every.add(entry.id()))));
            ManualMarks.see(this.manual.id(), every);
        }
        this.geometry = ManualScreenLayout.of(this.style.pageWidth(), this.style.pageHeight(), this.style.spread());
        this.left = (this.width - this.geometry.width()) / 2;
        // Read from a lectern, the binder and its strip under it stand in the middle together.
        this.top = (this.height - this.geometry.height() - (this.lectern == null ? 0 : ManualScreenLayout.LECTERN_ROOM))
                / 2;
        if (this.lectern != null) {
            this.addLecternButtons();
        }
        final ManualScreenLayout.Rect field = ManualScreenLayout.searchField(this.geometry, this.style.margin());
        final String typed = this.searchField == null ? "" : this.searchField.getValue();
        this.searchField = new SearchField(this.font, this.left + field.x() + 3, this.top + field.y() + 3,
                field.width() - 6, field.height() - 4, this.colour(GuideStyle.LABEL_INK, ""));
        this.searchField.setBordered(false);
        this.searchField.setValue(typed);
        this.searchField.setVisible(this.searching);
        this.addRenderableWidget(this.searchField);
        if (this.placed) {
            this.spread = Math.min(keep, this.lastSpread());
        } else if (!this.openAt.isEmpty()) {
            this.book.pageOf(this.openAt).ifPresent(this::goToPage);
        } else {
            ManualPlaces.of(this.manual.id()).filter(place -> !place.cover())
                    .ifPresent(place -> this.book.pageOf(place.target())
                            .ifPresent(page -> this.goToPage(page + place.pagesIn())));
        }
        if (!this.placed) {
            this.kept = this.keptSpread();
            if (!this.sheet) {
                AudioEngine.playOnScreen(GuideSounds.OPEN);
            }
        }
        this.placed = true;
    }

    /** The spread shown now: -1 for the cover. */
    public int spread() {
        return this.spread;
    }

    /** The manual laid out as it is shown. */
    public GuideBook book() {
        return this.book;
    }

    /** Whether the search is open on the left page. */
    public boolean searching() {
        return this.searching;
    }

    /** The manual it shows. */
    public GuideManual manual() {
        return this.manual;
    }

    /**
     * Lays the manual out again from what the resource packs hold now, its reader left on the page they were reading:
     * found by where it stands in its entry, as a manual put down is opened again.
     */
    public void reread() {
        final ManualPlaces.Place at = this.spread < 0 ? ManualPlaces.Place.COVER
                : this.placeOf(this.style.spread() ? this.spread * 2 : this.spread);
        this.rebuildWidgets();
        if (at.cover()) {
            this.spread = -1;
        } else {
            this.pageAt(at).ifPresent(this::goToPage);
        }
    }

    /** Whether the spread shown holds that page. */
    public boolean showing(final int page) {
        return page >= 0 && this.spread == (this.style.spread() ? page / 2 : page);
    }

    /** Shows the spread holding that page. */
    public void goToPage(final int page) {
        this.searching = false;
        if (this.searchField != null) {
            this.searchField.setVisible(false);
        }
        this.spread = Math.max(0, Math.min(this.lastSpread(), this.style.spread() ? page / 2 : page));
    }

    /**
     * Shows the spread holding where that entry, section or chapter starts, when the manual holds it; when another
     * book holds it instead, as a link to it says, opens that book there.
     */
    public boolean goTo(final String target) {
        final OptionalInt page = this.book.pageOf(target);
        if (page.isPresent()) {
            this.jump(page.getAsInt());
            return true;
        }
        return GuideLibrary.loaded().manualForTarget(target).filter(other -> !other.id().equals(this.manual.id()))
                .map(other -> GuideClient.open(other.id(), target)).orElse(false);
    }

    /** Goes back to where the reader jumped from, as the Back button does; false when there is nowhere to go. */
    public boolean back() {
        final OptionalInt place = this.history.back(this.spread);
        place.ifPresent(this::showSpread);
        return place.isPresent();
    }

    /** Goes forward again after going back, as the Forward button does; false when there is nowhere to go. */
    public boolean forward() {
        final OptionalInt place = this.history.forward(this.spread);
        place.ifPresent(this::showSpread);
        return place.isPresent();
    }

    /**
     * Where an item stands on a plate of the spread shown, in the screen's coordinates, while the plate shows it; null
     * when it does not (client tests click it).
     */
    @Nullable
    public int[] plateItemPoint(final String item) {
        final int first = this.style.spread() ? this.spread * 2 : this.spread;
        final int[] onLeft = this.plateItemOn(this.geometry.left(), first, item);
        if (onLeft != null || this.geometry.right() == null) {
            return onLeft;
        }
        return this.plateItemOn(this.geometry.right(), this.rightPage(first), item);
    }

    /**
     * Where a link leading to that target starts on the spread shown, in the screen's coordinates, a little inside its
     * first letter; null when the spread shows no such link (client tests click it).
     */
    @Nullable
    public int[] linkPoint(final String target) {
        final int first = this.style.spread() ? this.spread * 2 : this.spread;
        final int[] onLeft = this.linkPointOn(this.geometry.left(), first, target);
        if (onLeft != null || this.geometry.right() == null) {
            return onLeft;
        }
        return this.linkPointOn(this.geometry.right(), this.rightPage(first), target);
    }

    /** The middle of the Back button, in the screen's coordinates (client tests point at it). */
    public int[] backButtonPoint() {
        return this.middleOf(this.geometry.back());
    }

    /** The middle of the contents button, in the screen's coordinates (client tests click it). */
    public int[] contentsButtonPoint() {
        return this.middleOf(this.geometry.contents());
    }

    /**
     * Where to click a listing of that file on the spread shown, in the screen's coordinates: its Copy button, or the
     * middle of its code; null when the spread shows no such listing (client tests click it).
     */
    @Nullable
    public int[] listingPoint(final String name, final boolean copy) {
        final int first = this.style.spread() ? this.spread * 2 : this.spread;
        for (final int index : new int[] {first, this.rightPage(first)}) {
            final ManualScreenLayout.Rect page = index == first ? this.geometry.left() : this.geometry.right();
            if (page == null || index < 0 || index >= this.book.pages().size()) {
                continue;
            }
            for (final GuidePiece piece : this.book.pages().get(index).pieces()) {
                if (piece instanceof GuidePiece.Listing listing && listing.name().equals(name)) {
                    final int x = page.x() + listing.x();
                    final int y = page.y() + listing.y();
                    return copy ? this.middleOf(copyButton(x, y, listing.width()))
                            : new int[] {this.left + x + listing.width() / 2, this.top + y + listing.height() / 2};
                }
            }
        }
        return null;
    }

    /**
     * Where to point at the scene of that id on the spread shown, in the screen's coordinates: the middle of its
     * picture, or its Reset button; null when the spread shows no such scene (client tests drag and click it).
     */
    @Nullable
    public int[] scenePoint(final String id, final boolean reset) {
        return this.onScene(id, placed -> reset
                ? this.middleOf(this.scenes.resetButton(placed.scene(), placed.x(), placed.y()))
                : new int[] {this.left + placed.x() + placed.scene().width() / 2,
                        this.top + placed.y() + (placed.scene().height() - GuideLayout.SCENE_STRIP) / 2});
    }

    /**
     * The middle of the button that shows a layer more of the scene of that id, or of the one that hides its top
     * layer, in the screen's coordinates; null when the spread shows no such scene (client tests click them).
     */
    @Nullable
    public int[] sceneLayerPoint(final String id, final boolean more) {
        return this.onScene(id, placed -> this.middleOf(this.scenes.layerButton(placed.scene(), placed.x(), placed.y(),
                more)));
    }

    /** How the reader has turned the scene of that id, once it has been drawn (client tests ask). */
    public Optional<GuideBlock.SceneView> sceneView(final String id) {
        return this.scenes.viewOf(id);
    }

    /** How many layers of the scene of that id are shown, once it has been drawn (client tests ask). */
    public OptionalInt sceneLayers(final String id) {
        return this.scenes.layersOf(id);
    }

    /** The middle of the button that opens the list of bookmarks, in the screen's coordinates (client tests click). */
    public int[] bookmarksButtonPoint() {
        return this.middleOf(ManualScreenLayout.bookmarks(this.geometry));
    }

    /** The middle of the list's row that marks the page shown, in the screen's coordinates (client tests click). */
    public int[] markPagePoint() {
        final int rows = Math.max(1, new MarksHost().rows().size());
        return this.middleOf(ManualScreenLayout.bookmarkRow(ManualScreenLayout.bookmarkList(this.geometry, rows),
                rows, rows));
    }

    /** The middle of the left page, or the right, in the screen's coordinates (client tests right click it). */
    public int[] pagePoint(final boolean right) {
        final ManualScreenLayout.Rect page = right && this.geometry.right() != null ? this.geometry.right()
                : this.geometry.left();
        return this.middleOf(page);
    }

    /** Whether the list of bookmarks is open (client tests ask). */
    public boolean bookmarkListOpen() {
        return this.marks.listOpen();
    }

    /** Whether a note is being written in a margin (client tests ask). */
    public boolean writingNote() {
        return this.marks.writing();
    }

    /** The step the scene of that id is at, from 0, once it has been drawn (client tests ask). */
    public OptionalInt sceneStep(final String id) {
        return this.scenes.stepOf(id);
    }

    /** Whether the scene of that id plays its steps (client tests ask). */
    public boolean scenePlaying(final String id) {
        return this.scenes.playing(id);
    }

    /**
     * The middle of a button of the timeline of the scene of that id: 0 the step before, 1 play or pause, 2 the next
     * step, in the screen's coordinates; null when the spread shows no such scene (client tests click them).
     */
    @Nullable
    public int[] sceneTimelinePoint(final String id, final int button) {
        return this.onScene(id, placed -> this.middleOf(this.scenes.timelineButtons(placed.scene(), placed.x(),
                placed.y()).get(button)));
    }

    /**
     * The middle of the In world button of the scene of that id, in the screen's coordinates; null when the spread
     * shows no such scene, or nothing shows multiblocks in the world (client tests click it).
     */
    @Nullable
    public int[] sceneInWorldPoint(final String id) {
        return this.onScene(id, placed -> this.scenes.inWorldButton(placed.scene(), placed.x(), placed.y())
                .map(this::middleOf).orElse(null));
    }

    /**
     * Where to click the first recipe shown on the spread, in the screen's coordinates: on its arrow, clear of its
     * slots, where a click opens what it makes; null when the spread shows no recipe (client tests click it).
     */
    @Nullable
    public int[] recipePoint() {
        final int first = this.style.spread() ? this.spread * 2 : this.spread;
        for (final int index : new int[] {first, this.rightPage(first)}) {
            final ManualScreenLayout.Rect page = index == first ? this.geometry.left() : this.geometry.right();
            if (page == null || index < 0 || index >= this.book.pages().size()) {
                continue;
            }
            for (final GuidePiece piece : this.book.pages().get(index).pieces()) {
                if (piece instanceof GuidePiece.Recipes rows
                        && rows.first() < this.recipes.of(rows.type(), rows.output()).size()) {
                    final int inputs = this.recipes.of(rows.type(), rows.output()).get(rows.first()).inputs().size();
                    final int row = GuideLayout.recipeRow(this.style);
                    return new int[] {this.left + page.x() + rows.x() + inputs * (SLOT + SLOT_GAP) + 4,
                            this.top + page.y() + rows.y() + (row - SLOT) / 2 + SLOT / 2};
                }
            }
        }
        return null;
    }

    /** The style the manual is drawn in. */
    public GuideStyle style() {
        return this.style;
    }

    /**
     * The middle of the mark of the {@code index}th chapter at the edge, in the screen's coordinates: its tab, its
     * block of the thumb index or its paper flag; null when the book shows no mark for it (client tests click it).
     */
    @Nullable
    public int[] markPoint(final int index) {
        return index >= 0 && index < this.tabCount() ? this.middleOf(this.markOf(index)) : null;
    }

    /** The spread the ribbon keeps, the one the book was put down at last time; empty when it keeps none. */
    public OptionalInt ribbonSpread() {
        return this.kept == UNSET ? OptionalInt.empty() : OptionalInt.of(this.kept);
    }

    /** Whether a listing is open over the whole book. */
    public boolean listingOpen() {
        return this.opened != null;
    }

    /** Opens or closes the search on the left page. */
    public void toggleSearch() {
        this.searching = !this.searching;
        if (this.spread < 0) {
            this.spread = 0;
        }
        this.searchField.setVisible(this.searching);
        this.setFocused(this.searching ? this.searchField : null);
        this.searchField.setFocused(this.searching);
    }

    /** Types into the search, as a player would. */
    public void search(final String words) {
        if (!this.searching) {
            this.toggleSearch();
        }
        this.searchField.setValue(words);
    }

    /** What the search finds for what is typed: the index's lines first, then the entries found in their words. */
    public List<GuideSearch.Hit> found() {
        final String wanted = this.searchField == null ? "" : this.searchField.getValue();
        // Asked several times a frame while the search is open, yet it only changes when the player types.
        if (this.foundBook == this.book && wanted.equals(this.foundWords)) {
            return this.foundCache;
        }
        this.foundCache = GuideSearch.find(this.book, wanted, this.recipes.words());
        this.foundBook = this.book;
        this.foundWords = wanted;
        return this.foundCache;
    }

    /** Whether the pointer is a pointing hand now, over something that takes a click (client tests ask). */
    public boolean pointing() {
        return PointerShape.pointing();
    }

    /**
     * Draws the page at a place alone, its paper filling a page's size from the top left: a page apart, as a torn
     * sheet shows it, with no pointer over it. Nothing where the book no longer holds the place.
     */
    public void drawSheet(final GuiGraphics g, final String target, final int pagesIn) {
        final OptionalInt page = this.pageAt(new ManualPlaces.Place(target, pagesIn));
        if (page.isPresent()) {
            this.drawPage(g, new ManualScreenLayout.Rect(0, 0, this.style.pageWidth(), this.style.pageHeight()),
                    page.getAsInt(), true, Integer.MIN_VALUE, Integer.MIN_VALUE);
        }
    }

    /** The lectern the manual is read from, or none. */
    public Optional<BlockPos> lectern() {
        return Optional.ofNullable(this.lectern);
    }

    /** The middle of the button that tears a copy of the page out, in the screen's coordinates (client tests click). */
    public int[] tearButtonPoint() {
        return this.middleOf(ManualScreenLayout.tear(this.geometry));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /* The page the manual is closed at is where it opens next time from its item, the game closed in between. */
    @Override
    public void removed() {
        ManualPlaces.leave(this.manual.id(), this.place());
        if (this.marks != null) {
            this.marks.keep();
        }
        PointerShape.reset();
        if (this.scenes != null) {
            this.scenes.close();
        }
        AudioEngine.playOnScreen(GuideSounds.CLOSE);
        super.removed();
    }

    @Override
    public void renderBackground(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        this.hoveredItem = ItemStack.EMPTY;
        this.hoveredWords = List.of();
        this.recipeUnder = ItemStack.EMPTY;
        this.scenes.frame();
        g.pose().pushPose();
        g.pose().translate(this.left, this.top, 0.0F);
        final int mx = mouseX - this.left;
        final int my = mouseY - this.top;
        if (this.spread < 0) {
            this.drawCover(g);
        } else if (this.opened == null) {
            this.drawOpen(g, mx, my);
        } else {
            // Under a listing opened over it, the book is only seen: nothing on it lights for the pointer.
            this.drawOpen(g, Integer.MIN_VALUE, Integer.MIN_VALUE);
        }
        this.drawLecternStrip(g);
        g.pose().popPose();
        if (this.opened != null && this.spread >= 0) {
            this.renderTransparentBackground(g);
            g.pose().pushPose();
            g.pose().translate(this.left, this.top, 0.0F);
            this.drawOpened(g, this.opened, mx, my);
            g.pose().popPose();
        }
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        // A page is heard turning whatever turned it: an arrow, the wheel, a key, a link, a tab, Back or Forward.
        if (this.heard != UNSET && this.heard != this.spread) {
            AudioEngine.playOnScreen(GuideSounds.PAGE);
        }
        this.heard = this.spread;
        PointerShape.point(this.clickable(mouseX - this.left, mouseY - this.top));
        if (!this.hoveredItem.isEmpty()) {
            g.renderTooltip(this.font, this.hoveredItem, mouseX, mouseY);
        } else if (!this.hoveredWords.isEmpty()) {
            g.renderComponentTooltip(this.font, this.hoveredWords, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (button == MOUSE_BACK) {
            return this.back();
        }
        if (button == MOUSE_FORWARD) {
            return this.forward();
        }
        final double mx = mouseX - this.left;
        final double my = mouseY - this.top;
        if (this.opened != null) {
            // A listing over the book takes every click: its buttons, or a click outside it to put it away.
            final ManualScreenLayout.Rect box = this.openedBox(this.opened);
            if (openedCopyButton(box).contains(mx, my)) {
                this.copy(this.opened.lines());
            } else if (closeButton(box).contains(mx, my) || !box.contains(mx, my)) {
                this.opened = null;
                AudioEngine.playOnScreen(GuideSounds.CLICK);
            }
            return true;
        }
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (this.spread < 0) {
            this.spread = 0;
            return true;
        }
        if (ManualScreenLayout.bookmarks(this.geometry).contains(mx, my)) {
            this.marks.toggleList();
            AudioEngine.playOnScreen(GuideSounds.CLICK);
            return true;
        }
        if (ManualScreenLayout.tear(this.geometry).contains(mx, my)) {
            this.tear();
            return true;
        }
        // The list of bookmarks, a ribbon or a note takes a click first; a page right clicked takes a note.
        if (this.marks.click(new MarksHost(), mx, my, button)) {
            return true;
        }
        // A recipe on a page opens in the recipe viewer installed, which comes back to the manual when it is closed.
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && RecipeViewers.showRecipesOf(this.recipeUnder)) {
            AudioEngine.playOnScreen(GuideSounds.CLICK);
            return true;
        }
        final PlacedScene scene = this.sceneAt(mx, my);
        if (scene != null && this.scenes.press(scene.scene(), scene.x(), scene.y(), mx, my)) {
            return true;
        }
        final PlacedListing listing = this.listingAt(mx, my);
        if (listing != null) {
            if (copyButton(listing.x(), listing.y(), listing.listing().width()).contains(mx, my)) {
                this.copy(listing.listing().lines());
            } else {
                this.open(listing.listing());
            }
            return true;
        }
        for (final ManualScreenLayout.Rect control : List.of(this.geometry.contents(), this.geometry.back(),
                this.geometry.forward(), this.geometry.search(), this.geometry.previous(), this.geometry.next())) {
            if (control.contains(mx, my)) {
                this.pressed = control;
            }
        }
        // The buttons and the tabs click as the game's buttons do; an arrow is heard as the page it turns.
        final boolean onButton = this.geometry.contents().contains(mx, my) || this.geometry.search().contains(mx, my)
                || this.geometry.back().contains(mx, my) && this.history.behind().isPresent()
                || this.geometry.forward().contains(mx, my) && this.history.ahead().isPresent();
        boolean onTab = false;
        for (int i = 0; i < this.tabCount(); i++) {
            onTab |= this.markOf(i).contains(mx, my);
        }
        if (onButton || onTab) {
            AudioEngine.playOnScreen(GuideSounds.CLICK);
        }
        if (this.geometry.contents().contains(mx, my)) {
            this.jump(this.book.contents());
            return true;
        }
        if (this.geometry.back().contains(mx, my)) {
            return this.back();
        }
        if (this.geometry.forward().contains(mx, my)) {
            return this.forward();
        }
        if (this.geometry.search().contains(mx, my)) {
            this.toggleSearch();
            return true;
        }
        if (this.geometry.previous().contains(mx, my)) {
            return this.turn(-1);
        }
        if (this.geometry.next().contains(mx, my)) {
            return this.turn(1);
        }
        for (int i = 0; i < this.tabCount(); i++) {
            if (this.markOf(i).contains(mx, my)) {
                return this.goTo(this.chapters.get(i));
            }
        }
        if (this.searching && this.geometry.left().contains(mx, my)) {
            final GuideSearch.Hit hit = this.searchResultAt(my);
            if (hit != null && hit.page() >= 0) {
                this.jump(hit.page());
                return true;
            }
            return false;
        }
        final String link = this.linkAt(mx, my);
        return !link.isEmpty() && this.goTo(link);
    }

    @Override
    public boolean mouseReleased(final double mouseX, final double mouseY, final int button) {
        this.pressed = null;
        this.scenes.release();
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /* A scene held by the pointer turns as it moves: across turns it round, up and down tilts it. */
    @Override
    public boolean mouseDragged(final double mouseX, final double mouseY, final int button, final double dragX,
                                final double dragY) {
        return this.scenes.drag(mouseX - this.left, mouseY - this.top)
                || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX, final double scrollY) {
        if (this.opened != null) {
            // The wheel scrolls a listing opened over the book down when it is longer than the book is tall, and
            // across, with Shift or the wheel's own sideways turn, when a line is longer than the book is wide.
            final boolean across = scrollX != 0.0 || Screen.hasShiftDown();
            final double turned = scrollX != 0.0 ? scrollX : scrollY;
            if (across) {
                this.openedShift = Math.max(0, Math.min(this.openedShiftMost(this.opened),
                        this.openedShift + (turned < 0.0 ? SHIFT_STEP : -SHIFT_STEP)));
            } else {
                final int most = Math.max(0, this.opened.lines().size() - this.openedRows(this.opened));
                this.openedScroll = Math.max(0, Math.min(most, this.openedScroll + (scrollY < 0.0 ? 1 : -1)));
            }
            return true;
        }
        // Over a scene the wheel brings it nearer or sends it farther; anywhere else it turns the page.
        final PlacedScene scene = this.sceneAt(mouseX - this.left, mouseY - this.top);
        if (scene != null && this.scenes.scroll(scene.scene(), scene.x(), scene.y(), mouseX - this.left,
                mouseY - this.top, scrollY)) {
            return true;
        }
        return scrollY != 0.0 && this.turn(scrollY < 0.0 ? 1 : -1);
    }

    /* A note being written in a margin takes every letter typed. */
    @Override
    public boolean charTyped(final char codePoint, final int modifiers) {
        return this.marks != null && this.marks.typed(codePoint) || super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        if (this.marks != null && this.marks.key(key)) {
            // Writing a note, Escape keeps it rather than closing the book, and no key turns a page.
            return true;
        }
        if (this.opened != null && key == GLFW.GLFW_KEY_ESCAPE) {
            // Escape puts a listing opened over the book away, leaving the book open.
            this.opened = null;
            return true;
        }
        if (this.searching && this.searchField.isFocused() && key != GLFW.GLFW_KEY_ESCAPE) {
            return this.searchField.keyPressed(key, scanCode, modifiers);
        }
        return switch (key) {
            case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_PAGE_DOWN -> this.turn(1);
            case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_PAGE_UP -> this.turn(-1);
            case GLFW.GLFW_KEY_HOME -> {
                this.jump(this.book.contents());
                yield true;
            }
            case GLFW.GLFW_KEY_BACKSPACE -> this.back();
            default -> super.keyPressed(key, scanCode, modifiers);
        };
    }

    /** Turns that many spreads, forward or back, never past the cover or the last. */
    public boolean turn(final int by) {
        final int to = Math.max(-1, Math.min(this.lastSpread(), this.spread + by));
        if (to == this.spread) {
            return false;
        }
        this.spread = to;
        if (this.searching) {
            this.toggleSearch();
        }
        return true;
    }

    private int lastSpread() {
        final int pages = this.book.pages().size();
        return this.style.spread() ? (pages - 1) / 2 : pages - 1;
    }

    private int[] middleOf(final ManualScreenLayout.Rect control) {
        return new int[] {this.left + control.x() + control.width() / 2, this.top + control.y() + control.height() / 2};
    }

    /* Take and Close on the strip under a manual read from a lectern, as the game's lectern has under its book. */
    private void addLecternButtons() {
        final ManualScreenLayout.Rect strip = ManualScreenLayout.lecternStrip(this.geometry);
        final ManualScreenLayout.Rect take = ManualScreenLayout.lecternTake(strip);
        final ManualScreenLayout.Rect close = ManualScreenLayout.lecternClose(strip);
        this.addRenderableWidget(Button.builder(GameText.component(GuideTexts.LECTERN_TAKE), button -> {
            if (this.lectern != null && this.minecraft != null && this.minecraft.getConnection() != null) {
                PacketDistributor.sendToServer(new LecternTakePayload(this.lectern));
            }
            this.onClose();
        }).bounds(this.left + take.x(), this.top + take.y(), take.width(), take.height()).build());
        this.addRenderableWidget(Button.builder(GameText.component(GuideTexts.LECTERN_CLOSE), button -> this.onClose())
                .bounds(this.left + close.x(), this.top + close.y(), close.width(), close.height()).build());
    }

    /* The strip under a manual read from a lectern, in the binder's cover colour, saying where the lectern is. */
    private void drawLecternStrip(final GuiGraphics g) {
        if (this.lectern == null) {
            return;
        }
        final ManualScreenLayout.Rect strip = ManualScreenLayout.lecternStrip(this.geometry);
        g.fill(strip.x(), strip.y(), strip.x() + strip.width(), strip.y() + strip.height(),
                this.colour(GuideStyle.COVER));
        Draw.outline(g, strip.x(), strip.y(), strip.width(), strip.height(), this.colour(GuideStyle.COVER_EDGE));
        final String where = this.text(GuideTexts.LECTERN_AT.key(), this.lectern.getX(), this.lectern.getY(),
                this.lectern.getZ());
        final int room = ManualScreenLayout.lecternTake(strip).x() - strip.x() - 2 * ManualScreenLayout.LECTERN_PAD;
        GuideFonts.draw(g, this.font, this.fit(where, room, TextSize.BODY), TextSize.BODY, this.style,
                strip.x() + ManualScreenLayout.LECTERN_PAD, strip.y() + (strip.height() - 7) / 2,
                this.colour(GuideStyle.LABEL));
    }

    /*
     * Tears a copy of the page on the left out of the book: the server hands the player the sheet, and the book keeps
     * its page. A page whose place is too long to send stays in the book.
     */
    private void tear() {
        if (this.spread < 0 || this.minecraft == null || this.minecraft.getConnection() == null) {
            return;
        }
        final int first = this.style.spread() ? this.spread * 2 : this.spread;
        final ManualPlaces.Place at = this.placeOf(first);
        final String folio = this.book.pages().get(first).folio();
        if (this.manual.id().length() > TornSheets.MOST_ID || at.target().length() > TornSheets.MOST_PLACE
                || folio.length() > TornSheets.MOST_FOLIO) {
            return;
        }
        PacketDistributor.sendToServer(new TearPayload(new TornSheets.Sheet(this.manual.id(), at.target(),
                at.pagesIn(), folio)));
        AudioEngine.playOnScreen(GuideSounds.PAGE);
    }

    /** Jumps to the spread holding that page, leaving a mark Back returns to when it is another spread. */
    private void jump(final int page) {
        final int from = this.spread;
        this.goToPage(page);
        if (this.spread != from) {
            this.history.left(from);
        }
    }

    /** Shows a spread Back or Forward leads to, or the cover. */
    private void showSpread(final int to) {
        if (this.searching) {
            this.toggleSearch();
        }
        this.spread = Math.max(-1, Math.min(this.lastSpread(), to));
    }

    /**
     * Where the manual stands now, to open there next time: the entry, section or chapter the first page shown belongs
     * to and how many pages into it, or the cover.
     */
    private ManualPlaces.Place place() {
        if (this.spread < 0) {
            return ManualPlaces.Place.COVER;
        }
        // A book put down in its front matter, its contents, opens at its cover again.
        final ManualPlaces.Place at = this.placeOf(this.style.spread() ? this.spread * 2 : this.spread);
        return FRONT.equals(at.target()) ? ManualPlaces.Place.COVER : at;
    }

    /**
     * Where a page stands: the entry, section or chapter it belongs to and how many pages into it; a page of the
     * front matter, before them all, by how far into the book it is.
     */
    private ManualPlaces.Place placeOf(final int page) {
        String target = "";
        int start = -1;
        for (final Map.Entry<String, Integer> at : this.book.targets().entrySet()) {
            if (at.getValue() <= page && at.getValue() > start) {
                target = at.getKey();
                start = at.getValue();
            }
        }
        return target.isEmpty() ? new ManualPlaces.Place(FRONT, page) : new ManualPlaces.Place(target, page - start);
    }

    /** The page at a place, when the book still holds where it stands. */
    private OptionalInt pageAt(final ManualPlaces.Place place) {
        if (FRONT.equals(place.target())) {
            return place.pagesIn() < this.book.pages().size() ? OptionalInt.of(place.pagesIn()) : OptionalInt.empty();
        }
        final OptionalInt start = this.book.pageOf(place.target());
        if (start.isEmpty()) {
            return OptionalInt.empty();
        }
        final int page = start.getAsInt() + place.pagesIn();
        return page < this.book.pages().size() ? OptionalInt.of(page) : OptionalInt.empty();
    }

    /** Whether something under the pointer takes a click: the closed cover, a control, a tab, a link, a result. */
    private boolean clickable(final double mx, final double my) {
        if (this.spread < 0) {
            return ManualScreenLayout.cover(this.geometry).contains(mx, my);
        }
        if (this.opened != null) {
            final ManualScreenLayout.Rect box = this.openedBox(this.opened);
            return openedCopyButton(box).contains(mx, my) || closeButton(box).contains(mx, my);
        }
        if (this.listingAt(mx, my) != null) {
            return true;
        }
        if (ManualScreenLayout.bookmarks(this.geometry).contains(mx, my) || this.marks.listOpen()
                || ManualScreenLayout.tear(this.geometry).contains(mx, my)) {
            return true;
        }
        final PlacedScene scene = this.sceneAt(mx, my);
        if (scene != null && this.scenes.clickable(scene.scene(), scene.x(), scene.y(), mx, my)) {
            return true;
        }
        if (this.geometry.contents().contains(mx, my) || this.geometry.search().contains(mx, my)
                || this.geometry.back().contains(mx, my) && this.history.behind().isPresent()
                || this.geometry.forward().contains(mx, my) && this.history.ahead().isPresent()
                || this.geometry.previous().contains(mx, my) || this.geometry.next().contains(mx, my)
                && this.spread < this.lastSpread()) {
            return true;
        }
        for (int i = 0; i < this.tabCount(); i++) {
            if (this.markOf(i).contains(mx, my)) {
                return true;
            }
        }
        if (this.searching && this.geometry.left().contains(mx, my)) {
            return this.searchResultAt(my) != null;
        }
        if (!this.recipeUnder.isEmpty() && RecipeViewers.any()) {
            return true;
        }
        return !this.linkAt(mx, my).isEmpty();
    }

    /** What a spread is, as a button leading there names it: its section, and the page it starts on. */
    private String describe(final int to) {
        final int first = this.style.spread() ? to * 2 : to;
        if (first < 0 || first >= this.book.pages().size()) {
            return this.text(this.manual.titleKey());
        }
        final GuideBook.Page page = this.book.pages().get(first);
        final GuideBook.Page named = this.style.spread() && first + 1 < this.book.pages().size()
                && !this.book.pages().get(first + 1).header().isEmpty() ? this.book.pages().get(first + 1) : page;
        return this.text(GuideTexts.LEADS_TO.key(), named.header(), page.folio());
    }

    /** How many chapter tabs stand at the edge: a binder's, as many as fit; a folder has none. */
    private int tabCount() {
        return this.style.cover().kind() != GuideStyle.CoverKind.FOLDER
                ? Math.min(this.chapters.size(), this.geometry.tabs()) : 0;
    }

    /** Where the mark of the chapter of that tab stands: a binder's tab, a printed thumb block or a paper flag. */
    private ManualScreenLayout.Rect markOf(final int index) {
        return switch (this.style.make().marks()) {
            case TABS -> ManualScreenLayout.tab(this.geometry, index);
            case THUMB -> ManualScreenLayout.thumb(this.geometry, index, this.tabCount(), this.isOpen(index));
            case FLAGS -> ManualScreenLayout.flag(this.geometry, index, this.isOpen(index));
        };
    }

    /** Whether the chapter of that tab is the one shown. */
    private boolean isOpen(final int index) {
        final String chapter = this.chapters.get(index);
        final String current = this.currentChapter();
        return this.manual.holdsEvery()
                ? GuideChapter.namespaceOf(chapter).equals(GuideChapter.namespaceOf(current))
                : chapter.equals(current);
    }

    /** The spread a ribbon keeps: the one the book was put down at last time, or none. */
    private int keptSpread() {
        final Optional<ManualPlaces.Place> place = ManualPlaces.of(this.manual.id()).filter(found -> !found.cover());
        if (place.isEmpty()) {
            return UNSET;
        }
        final OptionalInt page = this.book.pageOf(place.get().target());
        if (page.isEmpty()) {
            return UNSET;
        }
        final int at = page.getAsInt() + place.get().pagesIn();
        return this.style.spread() ? at / 2 : at;
    }

    private boolean drawing() {
        return this.style.decor().titleBlock();
    }

    private void resolveColours() {
        this.colours.clear();
        this.terminals.clear();
        // The binder's colours are the ground every role falls back on, read once here and not on each frame.
        this.colours.putAll(PaletteRoles.read(GuidePalettes.BINDER.get()));
        if (!this.style.palette().isEmpty()) {
            this.colours.putAll(PaletteLookup.roles(this.style.palette()));
        }
        this.style.colours().forEach((role, written) -> {
            final Integer colour = PaletteLookup.parse(written);
            if (colour != null) {
                this.colours.put(role, colour);
            }
        });
    }

    private int tabColour(final GuideChapter chapter) {
        final Integer own = PaletteLookup.colourOf(chapter.tab(), GuideStyle.TAB);
        return own == null ? this.colours.getOrDefault(GuideStyle.TAB, GuidePalettes.BINDER.get().tab()) : own;
    }

    /** The colour of the {@code index}th chapter's mark: the book's own colours in turn, or the chapter's. */
    private int markColour(final int index, final String chapter) {
        final List<String> colours = this.style.make().colours();
        if (colours.isEmpty()) {
            return this.tabColours.get(chapter);
        }
        final String written = colours.get(index % colours.size());
        final Integer parsed = PaletteLookup.parse(written);
        return parsed != null ? parsed : this.colour(written);
    }

    /** A role's colour, a chapter's tab standing for the tab role on that chapter's pages. */
    private int colour(final String role, final String chapter) {
        if (GuideStyle.TAB.equals(role) && this.tabColours.containsKey(chapter)) {
            return this.tabColours.get(chapter);
        }
        final Integer colour = this.colours.get(role);
        if (colour != null) {
            return colour;
        }
        // A role not even the binder names is drawn in ink, so nothing is ever left unpainted.
        return GuidePalettes.BINDER.get().ink();
    }

    private int colour(final String role) {
        return this.colour(role, "");
    }

    // ---------------------------------------------------------------- the closed manual

    private void drawCover(final GuiGraphics g) {
        final ManualScreenLayout.Rect cover = ManualScreenLayout.cover(this.geometry);
        switch (this.style.cover().kind()) {
            case FOLDER -> this.drawFolder(g, cover);
            case BOOK -> ManualCovers.draw(g, this.font, this.style, this.manual, cover, this::colour,
                    new ClientGuideText(this.font, this.style, this.recipes));
            case BINDER -> this.drawClosedBinder(g, cover);
        }
    }

    /** A binder closed: its vinyl, the spine with its rivets, its tabs, and its words on a label or on itself. */
    private void drawClosedBinder(final GuiGraphics g, final ManualScreenLayout.Rect cover) {
        for (int i = 0; i < Math.min(this.chapters.size(), this.geometry.tabs()); i++) {
            final ManualScreenLayout.Rect tab = ManualScreenLayout.coverTab(this.geometry, i);
            final int colour = this.tabColours.get(this.chapters.get(i));
            this.bevel(g, tab.x() - 1, tab.y(), tab.width() + 1, tab.height(), colour, this.lighter(colour),
                    this.shadowOf(colour));
            GuideFonts.draw(g, this.font, String.valueOf(i + 1), TextSize.BODY, this.style, tab.x() + 2,
                    tab.y() + (tab.height() - 8) / 2, this.colour(GuideStyle.TAB_INK));
        }
        final int fill = this.colour(GuideStyle.COVER);
        this.bevel(g, cover.x(), 0, cover.width(), cover.height(), fill, this.colour(GuideStyle.COVER_EDGE),
                this.shadowOf(fill));
        final int grain = this.mix(fill, this.colour(GuideStyle.COVER_EDGE), 0.25);
        for (int x = cover.x() + 2; x < cover.x() + cover.width() - 2; x += 3) {
            g.fill(x, 2, x + 1, cover.height() - 2, grain);
        }
        final int spine = this.colour(GuideStyle.SPINE);
        this.bevel(g, cover.x(), 0, SPINE_WIDTH, cover.height(), spine, fill, this.shadowOf(spine));
        final int ring = this.colour(GuideStyle.RING);
        for (final int rivet : RIVETS) {
            this.bevel(g, cover.x() + 2, rivet - 2, 10, 7, this.shadowOf(ring), ring,
                    this.shadowOf(this.shadowOf(ring)));
        }
        final int faceX = cover.x() + SPINE_WIDTH;
        final int faceWidth = cover.width() - SPINE_WIDTH;
        if (this.style.cover().label()) {
            this.drawLabel(g, faceX + (faceWidth - LABEL_WIDTH) / 2, 34, LABEL_WIDTH);
        } else {
            this.drawWords(g, faceX + faceWidth / 2, 36, this.colour(GuideStyle.COVER), LABEL_WIDTH - 40,
                    faceWidth - 2 * LABEL_PAD);
        }
        if (this.style.cover().band()) {
            g.fill(cover.x(), cover.height() - 18, cover.x() + cover.width(), cover.height() - 15,
                    this.colour(GuideStyle.BAND));
        }
        if (!this.manual.partNumber().isEmpty()) {
            final String part = this.text(GuideTexts.PART_NUMBER.key(), this.manual.partNumber());
            this.centred(g, part, TextSize.SMALL, faceX, faceWidth, cover.height() - 22,
                    this.mix(fill, this.colour(GuideStyle.LABEL), 0.6));
        }
    }

    /** A folder closed: the edges of its sheets at its side, its board, its label and the elastic round it. */
    private void drawFolder(final GuiGraphics g, final ManualScreenLayout.Rect cover) {
        final int width = cover.width() - 2;
        g.fill(cover.x() + width, 12, cover.x() + width + 8, cover.height() - 18, this.colour(GuideStyle.PAPER));
        g.fill(cover.x() + width - 3, 16, cover.x() + width + 5, cover.height() - 14, this.colour(GuideStyle.GRID));
        final int fill = this.colour(GuideStyle.COVER);
        this.bevel(g, cover.x(), 0, width, cover.height(), fill, this.colour(GuideStyle.COVER_EDGE),
                this.shadowOf(fill));
        final int grain = this.mix(fill, this.colour(GuideStyle.COVER_EDGE), 0.25);
        for (int y = 3; y < cover.height() - 3; y += 4) {
            g.fill(cover.x() + 2, y, cover.x() + width - 2, y + 1, grain);
        }
        this.drawLabel(g, cover.x() + (width - FOLDER_LABEL_WIDTH) / 2, 30, FOLDER_LABEL_WIDTH);
        if (this.style.cover().band()) {
            g.fill(cover.x(), cover.height() - 48, cover.x() + width, cover.height() - 45,
                    this.colour(GuideStyle.BAND));
        }
    }

    /** The cover's label: a card with the manual's mark, its lines, its title and its edition. */
    private void drawLabel(final GuiGraphics g, final int x, final int y, final int width) {
        final int label = this.colour(GuideStyle.LABEL);
        final int titleRoom = width - LABEL_PAD;
        final int height = this.wordsHeight(titleRoom) + (this.manual.icon().isEmpty() ? 20 : 14);
        this.bevel(g, x, y, width, height, label, this.lighter(label), this.shadowOf(this.lighter(label)));
        g.fill(x, y, x + width, y + 2, this.lighter(label));
        this.drawWords(g, x + width / 2, y + (this.manual.icon().isEmpty() ? 10 : 8), label, width - 2 * LABEL_PAD,
                titleRoom);
    }

    /**
     * The words of a cover from {@code y} down, centred on {@code centre}: the mark, the first line bold in its own
     * colour and the others small, a rule, the title large, a rule and the edition. Their ground is {@code ground},
     * which the rules and the small lines are drawn a shade of; the rules are {@code room} wide, and the title wraps
     * in {@code titleRoom}.
     */
    private void drawWords(final GuiGraphics g, final int centre, final int top, final int ground, final int room,
                           final int titleRoom) {
        int y = top;
        final ResourceLocation icon = this.manual.icon().isEmpty() ? null
                : ResourceLocation.tryParse(this.manual.icon());
        if (icon != null) {
            final ResourceLocation mark = icon.withPrefix("textures/").withSuffix(".png");
            g.blit(mark, centre - MARK / 2, y, 0, 0, MARK, MARK, MARK, MARK);
            y += MARK + 10;
        }
        final int ink = this.colour(GuideStyle.LABEL_INK);
        final int rule = this.mix(ground, ink, 0.4);
        final int faint = this.mix(ground, ink, 0.6);
        final int x = centre - room / 2;
        boolean first = true;
        for (final String key : this.manual.coverKeys()) {
            this.centred(g, this.text(key), first ? TextSize.BOLD : TextSize.SMALL, x, room, y,
                    first ? this.colour(GuideStyle.COVER_LINE) : faint);
            y += first ? 12 : 11;
            first = false;
        }
        g.fill(x + 4, y, x + room - 4, y + 1, rule);
        y += 7;
        final float scale = this.titleScale();
        for (final String line : GuideLayout.wrap(this.text(this.manual.titleKey()), (int) (titleRoom / scale),
                TextSize.BOLD, new ClientGuideText(this.font, this.style, this.recipes))) {
            final int lineWidth = (int) (GuideFonts.width(this.font, line, TextSize.BOLD, this.style) * scale);
            g.pose().pushPose();
            g.pose().translate(centre - lineWidth / 2.0F, y, 0.0F);
            g.pose().scale(scale, scale, 1.0F);
            GuideFonts.draw(g, this.font, line, TextSize.BOLD, this.style, 0, 0, ink);
            g.pose().popPose();
            y += Math.round(10 * scale) + 1;
        }
        y += 1;
        g.fill(x + 4, y, x + room - 4, y + 1, rule);
        y += 5;
        if (!this.manual.edition().isEmpty()) {
            this.centred(g, this.text(this.manual.edition(), this.sheetCount()), TextSize.SMALL, x, room, y, faint);
        }
    }

    /** How tall the cover's words stand, the title wrapping in {@code titleRoom}. */
    private int wordsHeight(final int titleRoom) {
        final int lines = GuideLayout.wrap(this.text(this.manual.titleKey()), (int) (titleRoom / this.titleScale()),
                TextSize.BOLD, new ClientGuideText(this.font, this.style, this.recipes)).size();
        final int mark = this.manual.icon().isEmpty() ? 0 : MARK + 10;
        final int coverLines = this.manual.coverKeys().isEmpty() ? 0 : 12 + 11 * (this.manual.coverKeys().size() - 1);
        return mark + coverLines + 7 + lines * (Math.round(10 * this.titleScale()) + 1) + 1 + 5
                + TextSize.SMALL.lineHeight();
    }

    /** A binder's title is set larger than a folder's label can take. */
    private float titleScale() {
        return this.style.cover().kind() == GuideStyle.CoverKind.FOLDER ? 1.25F : 1.5F;
    }

    /** How many sheets the manual has: its pages past the front matter. */
    private int sheetCount() {
        int sheets = 0;
        for (final GuideBook.Page page : this.book.pages()) {
            if (!page.chapter().isEmpty()) {
                sheets++;
            }
        }
        return sheets;
    }

    // ---------------------------------------------------------------- the open manual

    private void drawOpen(final GuiGraphics g, final int mx, final int my) {
        final MarksHost host = new MarksHost();
        this.see(host);
        this.seenNow = ManualMarks.seen(this.manual.id());
        final int fill = this.colour(GuideStyle.COVER);
        ManualControls.bevel(g, 0, 0, this.geometry.coverRight(), this.geometry.height(), fill,
                this.colour(GuideStyle.COVER_EDGE), ManualButtonLook.shaded(fill));
        ManualMake.board(g, this.geometry, this.style, this::colour);
        // Paper flags are stuck under the pages' edge, so they are drawn first and the pages over them.
        final boolean flags = this.style.make().marks() == GuideStyle.Marks.FLAGS;
        if (flags) {
            this.drawTabs(g, mx, my);
        }
        final int first = this.style.spread() ? this.spread * 2 : this.spread;
        this.drawPage(g, this.geometry.left(), first, true, mx, my);
        if (this.geometry.right() != null) {
            this.drawPage(g, this.geometry.right(), this.rightPage(first), false, mx, my);
            if (this.style.rings()) {
                this.drawRings(g);
            } else {
                ManualMake.binding(g, this.geometry, this.style, this::colour);
            }
        }
        // The tabs come after the pages, so the one shown and the one pointed at reach over the page's edge.
        if (!flags) {
            this.drawTabs(g, mx, my);
        }
        ManualMake.ribbon(g, this.geometry, this.style, this::colour, this.spread == this.kept);
        // The reader's own marks: the ribbons of their bookmarks and the notes in the margins of the pages shown.
        this.marks.drawRibbons(g, host);
        this.marks.drawNotes(g, host);
        this.drawButton(g, ManualScreenLayout.bookmarks(this.geometry), ManualControls.Kind.BOOKMARKS, true,
                List.of(GameText.component(GuideTexts.BOOKMARKS), GameText.component(GuideTexts.BOOKMARKS_TIP)
                        .withStyle(ChatFormatting.GRAY)), mx, my);
        this.drawButton(g, ManualScreenLayout.tear(this.geometry), ManualControls.Kind.TEAR, true,
                List.of(GameText.component(GuideTexts.TEAR), GameText.component(GuideTexts.TEAR_TIP)
                        .withStyle(ChatFormatting.GRAY)), mx, my);
        this.drawButton(g, this.geometry.contents(), ManualControls.Kind.CONTENTS, true,
                List.of(GameText.component(GuideTexts.CONTENTS)), mx, my);
        this.drawButton(g, this.geometry.back(), ManualControls.Kind.BACK, this.history.behind().isPresent(),
                this.leadsTo(GuideTexts.BACK, this.history.behind()), mx, my);
        this.drawButton(g, this.geometry.forward(), ManualControls.Kind.FORWARD, this.history.ahead().isPresent(),
                this.leadsTo(GuideTexts.FORWARD, this.history.ahead()), mx, my);
        this.drawButton(g, this.geometry.search(), ManualControls.Kind.SEARCH, true,
                List.of(GameText.component(GuideTexts.TO_SEARCH)), mx, my);
        if (this.spread > -1) {
            ManualControls.arrow(g, this.geometry.previous(), true, this.lookOf(this.geometry.previous(), true, mx,
                    my));
        }
        if (this.spread < this.lastSpread()) {
            ManualControls.arrow(g, this.geometry.next(), false, this.lookOf(this.geometry.next(), true, mx, my));
        }
        // The list of bookmarks lies over everything else on the binder while it is open.
        this.marks.drawList(g, host, mx, my);
    }

    /**
     * Notes the entries starting on the pages shown as seen, once a spread: what the contents no longer call new.
     */
    private void see(final MarksHost host) {
        if (this.spread == this.seenSpread) {
            return;
        }
        this.seenSpread = this.spread;
        final List<String> shown = new ArrayList<>();
        for (final ManualMarksView.Shown page : host.shown()) {
            for (final GuideContents.Chapter chapter : this.contents.chapters()) {
                for (final GuideContents.Section section : chapter.sections()) {
                    for (final GuideEntry entry : section.entries()) {
                        if (this.book.pageOf(entry.id()).orElse(UNSET) == page.page()) {
                            shown.add(entry.id());
                        }
                    }
                }
            }
        }
        if (!shown.isEmpty()) {
            ManualMarks.see(this.manual.id(), shown);
        }
    }

    /** The mark beside a section or a chapter of the contents: new, not yet, or none. */
    private ReaderMarks.Mark readerMark(final String target) {
        final List<GuideEntry> entries = new ArrayList<>();
        for (final GuideContents.Chapter chapter : this.contents.chapters()) {
            final boolean whole = chapter.chapter().id().equals(target);
            for (final GuideContents.Section section : chapter.sections()) {
                if (whole || section.section().id().equals(target)) {
                    entries.addAll(section.entries());
                }
            }
        }
        return ReaderMarks.of(entries, this.seenNow, knowledge -> KnowledgeGate.openHere(
                Optional.ofNullable(ResourceLocation.tryParse(knowledge))));
    }

    /** Whether a tab's chapter, or its mod's chapters for a manual of every mod, holds what the reader has not seen. */
    private boolean tabHoldsNew(final String chapter) {
        final String mod = GuideChapter.namespaceOf(chapter);
        final List<GuideEntry> entries = new ArrayList<>();
        for (final GuideContents.Chapter held : this.contents.chapters()) {
            final boolean under = this.manual.holdsEvery() ? GuideChapter.namespaceOf(held.chapter().id()).equals(mod)
                    : held.chapter().id().equals(chapter);
            if (under) {
                held.sections().forEach(section -> entries.addAll(section.entries()));
            }
        }
        return ReaderMarks.anyNew(entries, this.seenNow);
    }

    private void drawTabs(final GuiGraphics g, final int mx, final int my) {
        for (int i = 0; i < this.tabCount(); i++) {
            final ManualScreenLayout.Rect tab = this.markOf(i);
            final String chapter = this.chapters.get(i);
            final boolean on = this.isOpen(i);
            final boolean pointed = tab.contains(mx, my);
            if (this.style.make().marks() == GuideStyle.Marks.TABS) {
                ManualControls.tab(g, this.font, this.style, tab, i + 1, on, pointed, this.tabColours.get(chapter),
                        this.colour(GuideStyle.TAB_INK));
            } else {
                ManualMake.mark(g, this.font, this.style, tab, i + 1, on, pointed, this.markColour(i, chapter),
                        this::colour);
            }
            if (this.tabHoldsNew(chapter)) {
                // A gold dot at the tab's corner: its chapter holds something the reader has not seen.
                final GuidePalettes.Marks marks = GuidePalettes.MARKS.get();
                final int dotX = tab.x() + tab.width() - TAB_DOT - 1;
                g.fill(dotX - 1, tab.y(), dotX + TAB_DOT + 1, tab.y() + TAB_DOT + 2, marks.dotEdge());
                g.fill(dotX, tab.y() + 1, dotX + TAB_DOT, tab.y() + TAB_DOT + 1, marks.dot());
            }
            if (pointed) {
                // A tab of a manual holding every chapter stands for the mod, named by its main chapter.
                final String named = this.manual.holdsEvery() ? GuideChapter.namespaceOf(chapter) : chapter;
                this.hoveredWords = List.of(Component.literal(this.text(GuideLibrary.loaded().chapter(named)
                        .or(() -> GuideLibrary.loaded().chapter(chapter)).map(GuideChapter::titleKey)
                        .orElse(chapter))));
            }
        }
    }

    /** A button on the top edge in the look of what it is doing, and what it says under the pointer. */
    private void drawButton(final GuiGraphics g, final ManualScreenLayout.Rect button, final ManualControls.Kind kind,
                            final boolean leads, final List<Component> says, final int mx, final int my) {
        ManualControls.button(g, button, kind, this.lookOf(button, leads, mx, my));
        if (button.contains(mx, my)) {
            this.hoveredWords = says;
        }
    }

    /** What Back or Forward says under the pointer: its name, and where it leads or that it leads nowhere yet. */
    private List<Component> leadsTo(final TextKey name, final OptionalInt place) {
        return List.of(GameText.component(name), Component.literal(place.isPresent()
                ? this.describe(place.getAsInt()) : this.text(GuideTexts.NOWHERE_BACK.key())));
    }

    /** The look of a control in the state it is in: leading nowhere, held down, under the pointer, or at rest. */
    private ManualButtonLook lookOf(final ManualScreenLayout.Rect control, final boolean leads, final int mx,
                                    final int my) {
        final ManualButtonLook.State state = !leads ? ManualButtonLook.State.DISABLED
                : control.equals(this.pressed) && control.contains(mx, my) ? ManualButtonLook.State.PRESSED
                : control.contains(mx, my) ? ManualButtonLook.State.HOVER : ManualButtonLook.State.NORMAL;
        return ManualButtonLook.of(this.colour(GuideStyle.COVER_EDGE), this.colour(GuideStyle.INK),
                this.colour(GuideStyle.LABEL), state);
    }

    private String currentChapter() {
        final int first = this.style.spread() ? this.spread * 2 : this.spread;
        if (first >= 0 && first < this.book.pages().size()) {
            final String chapter = this.book.pages().get(first).chapter();
            if (!chapter.isEmpty()) {
                return chapter;
            }
        }
        if (this.style.spread() && first + 1 < this.book.pages().size()) {
            return this.book.pages().get(first + 1).chapter();
        }
        return "";
    }

    private void drawPage(final GuiGraphics g, final ManualScreenLayout.Rect page, final int index,
                          final boolean leftPage, final int mx, final int my) {
        g.fill(page.x(), page.y(), page.x() + page.width(), page.y() + page.height(),
                this.colour(GuideStyle.PAPER));
        ManualMake.paper(g, page, this.style, this::colour, leftPage);
        // A binder's pages lie flat against its rings; a bound book shades into its spine, drawn after both pages.
        if (this.geometry.right() != null && this.style.rings()) {
            final int gutterX = leftPage ? page.x() + page.width() - 6 : page.x();
            g.fill(gutterX, page.y(), gutterX + (leftPage ? 6 : 5), page.y() + page.height(),
                    this.colour(GuideStyle.GUTTER));
        }
        if (this.style.decor().grid()) {
            this.drawGrid(g, page);
        }
        if (this.style.decor().frame()) {
            this.drawFrame(g, page);
        }
        if (this.searching && leftPage) {
            this.drawSearch(g, page, mx, my);
            return;
        }
        if (index < 0 || index >= this.book.pages().size()) {
            return;
        }
        final GuideBook.Page shown = this.book.pages().get(index);
        if (this.drawing()) {
            this.drawTitleBlock(g, page, shown);
        } else {
            this.drawRunning(g, page, shown, leftPage);
        }
        for (final GuidePiece piece : shown.pieces()) {
            this.drawPiece(g, page, piece, shown.chapter(), mx, my);
        }
    }

    /** The head and the foot of a page: the chapter or the section, the folio and the manual's title. */
    private void drawRunning(final GuiGraphics g, final ManualScreenLayout.Rect page, final GuideBook.Page shown,
                             final boolean leftPage) {
        final int margin = this.style.margin();
        final int contentX = page.x() + margin;
        final int contentWidth = page.width() - 2 * margin;
        final String head = shown.header().toUpperCase(Locale.ROOT);
        final int headWidth = GuideFonts.width(this.font, head, TextSize.SMALL, this.style);
        GuideFonts.draw(g, this.font, head, TextSize.SMALL, this.style, leftPage ? contentX
                : contentX + contentWidth - headWidth, page.y() + GuideLayout.HEADER_Y, this.colour(GuideStyle.FAINT));
        final int rule = this.colour(GuideStyle.RULE);
        g.fill(contentX, page.y() + GuideLayout.HEADER_RULE_Y, contentX + contentWidth,
                page.y() + GuideLayout.HEADER_RULE_Y + 1, rule);
        final int footY = page.y() + page.height() - 16;
        g.fill(contentX, footY, contentX + contentWidth, footY + 1, rule);
        final int folioWidth = GuideFonts.width(this.font, shown.folio(), TextSize.SMALL, this.style);
        GuideFonts.draw(g, this.font, shown.folio(), TextSize.SMALL, this.style, leftPage ? contentX
                : contentX + contentWidth - folioWidth, footY + 5, this.colour(GuideStyle.INK));
        final String title = this.text(this.manual.titleKey());
        final int titleWidth = GuideFonts.width(this.font, title, TextSize.SMALL, this.style);
        // A long title stands in the middle only as far as it keeps clear of the folio; then it moves aside.
        final int middle = contentX + (contentWidth - titleWidth) / 2;
        final int titleX = leftPage ? Math.max(middle, contentX + folioWidth + FOOT_GAP)
                : Math.min(middle, contentX + contentWidth - folioWidth - FOOT_GAP - titleWidth);
        GuideFonts.draw(g, this.font, title, TextSize.SMALL, this.style, titleX, footY + 5,
                this.colour(GuideStyle.FAINT));
    }

    /** A drawing's faint grid over the whole sheet. */
    private void drawGrid(final GuiGraphics g, final ManualScreenLayout.Rect page) {
        final int grid = this.colour(GuideStyle.GRID);
        for (int x = page.x() + GRID; x < page.x() + page.width(); x += GRID) {
            g.fill(x, page.y(), x + 1, page.y() + page.height(), grid);
        }
        for (int y = page.y() + GRID; y < page.y() + page.height(); y += GRID) {
            g.fill(page.x(), y, page.x() + page.width(), y + 1, grid);
        }
    }

    /** A drawing's frame, its zones numbered along the top and lettered down the side, as a reader finds a part. */
    private void drawFrame(final GuiGraphics g, final ManualScreenLayout.Rect page) {
        final int x0 = page.x() + GuideLayout.FRAME;
        final int y0 = page.y() + GuideLayout.FRAME;
        final int width = page.width() - 2 * GuideLayout.FRAME;
        final int height = page.height() - 2 * GuideLayout.FRAME;
        Draw.outline(g, x0, y0, width, height, this.colour(GuideStyle.RULE));
        final int faint = this.colour(GuideStyle.FAINT);
        for (int i = 0; i < ZONES_ACROSS; i++) {
            final int zoneX = x0 + width * i / ZONES_ACROSS;
            if (i > 0) {
                g.fill(zoneX, y0 - 3, zoneX + 1, y0, faint);
            }
            final String number = String.valueOf(i + 1);
            final int numberWidth = GuideFonts.width(this.font, number, TextSize.SMALL, this.style);
            GuideFonts.draw(g, this.font, number, TextSize.SMALL, this.style,
                    x0 + width * (2 * i + 1) / (2 * ZONES_ACROSS) - numberWidth / 2, page.y() + 1, faint);
        }
        for (int i = 0; i < ZONES_DOWN; i++) {
            final String letter = String.valueOf((char) ('A' + i));
            GuideFonts.draw(g, this.font, letter, TextSize.SMALL, this.style, page.x() + 1,
                    y0 + height * (2 * i + 1) / (2 * ZONES_DOWN) - 3, faint);
        }
    }

    /**
     * A drawing's title block in the corner of its frame: the set it belongs to, what the sheet shows, the drawing's
     * number, which sheet of how many, and its revision.
     */
    private void drawTitleBlock(final GuiGraphics g, final ManualScreenLayout.Rect page, final GuideBook.Page shown) {
        final int x = page.x() + page.width() - GuideLayout.FRAME - GuideLayout.TITLE_BLOCK_WIDTH;
        final int y = page.y() + page.height() - GuideLayout.FRAME - GuideLayout.TITLE_BLOCK_HEIGHT;
        final int width = GuideLayout.TITLE_BLOCK_WIDTH;
        final int line = this.colour(GuideStyle.RULE);
        final int faint = this.colour(GuideStyle.FAINT);
        final int ink = this.colour(GuideStyle.INK);
        g.fill(x, y, x + width, y + GuideLayout.TITLE_BLOCK_HEIGHT, this.colour(GuideStyle.PAPER));
        Draw.outline(g, x, y, width, GuideLayout.TITLE_BLOCK_HEIGHT + 1, line);
        g.fill(x, y + BLOCK_ROW, x + width, y + BLOCK_ROW + 1, line);
        g.fill(x, y + BLOCK_LAST_ROW, x + width, y + BLOCK_LAST_ROW + 1, line);
        final String set = (this.manual.coverKeys().isEmpty() ? "" : this.text(this.manual.coverKeys().getFirst())
                + SPACED_DASH) + this.text(this.manual.titleKey());
        GuideFonts.draw(g, this.font, this.fit(set.toUpperCase(Locale.ROOT), width - 6, TextSize.SMALL),
                TextSize.SMALL, this.style, x + 3, y + 2, faint);
        GuideFonts.draw(g, this.font, this.fit(shown.title().toUpperCase(Locale.ROOT), width - 6, TextSize.BOLD),
                TextSize.BOLD, this.style, x + 3, y + BLOCK_ROW + 2, this.colour(GuideStyle.HEADING));
        final String[] labels = {this.text(GuideTexts.DRAWING.key()), this.text(GuideTexts.SHEET.key()),
                this.text(GuideTexts.REVISION.key())};
        final String[] values = {shown.folio(), this.text(GuideTexts.SHEET_OF.key(), shown.sheet(), shown.sheets()),
                REVISION};
        int cellX = x;
        for (int i = 0; i < BLOCK_CELLS.length; i++) {
            if (i > 0) {
                g.fill(cellX, y + BLOCK_LAST_ROW, cellX + 1, y + GuideLayout.TITLE_BLOCK_HEIGHT, line);
            }
            final String label = labels[i].toUpperCase(Locale.ROOT);
            GuideFonts.draw(g, this.font, label, TextSize.SMALL, this.style, cellX + 3, y + BLOCK_LAST_ROW + 2, faint);
            final int valueX = cellX + 3 + GuideFonts.width(this.font, label, TextSize.SMALL, this.style) + 3;
            final int room = (i == BLOCK_CELLS.length - 1 ? x + width : cellX + BLOCK_CELLS[i]) - valueX - 2;
            GuideFonts.draw(g, this.font, this.fit(values[i].toUpperCase(Locale.ROOT), room, TextSize.SMALL),
                    TextSize.SMALL, this.style, valueX, y + BLOCK_LAST_ROW + 2, ink);
            cellX += BLOCK_CELLS[i];
        }
    }

    private void drawPiece(final GuiGraphics g, final ManualScreenLayout.Rect page, final GuidePiece piece,
                           final String chapter, final int mx, final int my) {
        final int x = page.x() + piece.x();
        final int y = page.y() + piece.y();
        switch (piece) {
            case GuidePiece.Text text -> this.drawText(g, x, y, text, chapter, mx, my);
            case GuidePiece.Leader leader -> this.drawLeader(g, x, y, leader, chapter, mx, my);
            case GuidePiece.Rule rule -> g.fill(x, y, x + rule.width(), y + 1, this.colour(rule.colour(), chapter));
            case GuidePiece.Dots dots -> {
                final int colour = this.colour(dots.colour(), chapter);
                for (int dot = x; dot < x + dots.width(); dot += DOT_PITCH) {
                    g.fill(dot, y, dot + 1, y + 1, colour);
                }
            }
            case GuidePiece.Box box -> {
                g.fill(x, y, x + box.width(), y + box.height(), this.colour(box.fill(), chapter));
                Draw.outline(g, x, y, box.width(), box.height(), this.colour(box.edge(), chapter));
            }
            case GuidePiece.Item item -> this.drawItem(g, this.stackOf(item.item()), x, y, item.scale() * 100, mx,
                    my);
            case GuidePiece.Recipes rows -> this.drawRecipes(g, x, y, rows, chapter, mx, my);
            case GuidePiece.Listing listing -> this.drawListing(g, x, y, listing, chapter, mx, my);
            case GuidePiece.Session session -> this.drawSession(g, x, y, session);
            case GuidePiece.Key key -> this.drawKey(g, x, y, key.name(), chapter);
            case GuidePiece.Custom custom -> {
                final IGuideBlockRenderer renderer = rendererOf(custom.type());
                if (renderer != null) {
                    renderer.draw(g, this.font, x, y, custom.width(), custom.height(), this.dataOf(custom.data()),
                            mx + this.left, my + this.top);
                }
            }
            case GuidePiece.Views views -> this.drawViews(g, x, y, views, mx, my);
            case GuidePiece.Plan plan -> this.drawPlan(g, x, y, plan, mx, my);
            case GuidePiece.Plate plate -> this.drawPlate(g, x, y, plate, chapter, mx, my);
            case GuidePiece.Picture picture -> this.drawPicture(g, x, y, picture, mx, my);
            case GuidePiece.Scene scene -> {
                // A page apart is drawn into a picture of its own, which a scene's picture cannot be drawn inside.
                if (!this.sheet) {
                    final List<Component> words = this.scenes.draw(g, x, y, scene, mx, my);
                    if (!words.isEmpty()) {
                        this.hoveredWords = words;
                    }
                }
            }
            case GuidePiece.SceneKey key -> {
                if (!this.sheet) {
                    this.scenes.drawKey(g, x, y, key, mx, my);
                }
            }
        }
    }

    /**
     * The items an entry shows, on a plate under its title: a row of them at their own size, turning over to the next
     * row every little while when there are more, the one under the pointer lit as a slot is and named in its tooltip.
     */
    private void drawPlate(final GuiGraphics g, final int x, final int y, final GuidePiece.Plate plate,
                           final String chapter, final int mx, final int my) {
        g.fill(x, y, x + plate.width(), y + GuideLayout.PLATE_HEIGHT, this.colour(GuideStyle.SHADE, chapter));
        Draw.outline(g, x, y, plate.width(), GuideLayout.PLATE_HEIGHT, this.colour(GuideStyle.RULE, chapter));
        final List<String> row = plateRow(plate);
        final int first = x + plateStart(plate, row.size());
        for (int i = 0; i < row.size(); i++) {
            final int itemX = first + i * PLATE_PITCH;
            final ItemStack stack = this.stackOf(row.get(i));
            this.drawItem(g, stack, itemX, y + 2, 100, mx, my);
            if (!stack.isEmpty() && mx >= itemX && my >= y + 2 && mx < itemX + 16 && my < y + 18) {
                AbstractContainerScreen.renderSlotHighlight(g, itemX, y + 2, 0);
            }
        }
    }

    /** A picture: an image drawn to its size, or a drawing its mod's renderer makes in the room given. */
    private void drawPicture(final GuiGraphics g, final int x, final int y, final GuidePiece.Picture picture,
                             final int mx, final int my) {
        if (!picture.image().isEmpty()) {
            final ResourceLocation named = ResourceLocation.tryParse(picture.image());
            if (named == null) {
                return;
            }
            final ResourceLocation image = named.withPrefix("textures/").withSuffix(".png");
            // A strip of frames, one under the other, shows them in turn; with less movement, its first stands still.
            final int frame = picture.frames() < 2 || MotionClock.reduced() ? 0
                    : (int) Math.floorMod(Util.getMillis() / MILLIS_PER_TICK / picture.frameTicks(),
                    (long) picture.frames());
            Draw.blended(() -> g.blit(image, x, y, picture.width(), picture.height(), 0.0F,
                    (float) frame * picture.height(), picture.width(), picture.height(), picture.width(),
                    picture.height() * picture.frames()));
            return;
        }
        final IGuideBlockRenderer renderer = rendererOf(picture.drawing());
        if (renderer != null) {
            renderer.draw(g, this.font, x, y, picture.width(), picture.height(), this.dataOf(picture.data()),
                    mx + this.left, my + this.top);
        }
    }

    /**
     * A listing's frame: the strip with its label, its file's name and its Copy button, the paper its code is printed
     * on, and the rule beside the numbers of its lines. The code itself is the text laid over it.
     */
    private void drawListing(final GuiGraphics g, final int x, final int y, final GuidePiece.Listing listing,
                             final String chapter, final int mx, final int my) {
        this.drawCodeFrame(g, x, y, listing.width(), listing.height(), listing.gutter(), chapter);
        final ManualScreenLayout.Rect copy = copyButton(x, y, listing.width());
        this.drawStrip(g, x, y, copy.x() - x - 4, listing.label(), listing.name(), chapter);
        this.drawListingButton(g, copy, GuideTexts.COPY, this.justCopied(listing.lines()), chapter, mx, my);
    }

    /**
     * A session at a terminal in that terminal's own colours: its name on a strip of its ground lightened, then its
     * rows, the prompt, what was typed and what the system said each in its colour.
     */
    private void drawSession(final GuiGraphics g, final int x, final int y, final GuidePiece.Session session) {
        final Map<String, Integer> look = this.terminals.computeIfAbsent(session.look(), id -> {
            final Map<String, Integer> named = PaletteLookup.roles(id);
            return named.isEmpty() ? PaletteRoles.read(GuidePalettes.TERMINAL.get()) : named;
        });
        final int ground = look.getOrDefault(TERMINAL_GROUND, this.colour(GuideStyle.INK));
        final int typed = look.getOrDefault(TERMINAL_TYPED, this.colour(GuideStyle.PAPER));
        g.fill(x, y, x + session.width(), y + GuideLayout.SESSION_HEAD, ManualButtonLook.lighten(ground, STRIP_LIGHT));
        g.fill(x, y + GuideLayout.SESSION_HEAD, x + session.width(), y + session.height(), ground);
        GuideFonts.draw(g, this.font, this.fit(session.title(), session.width() - 6, TextSize.SMALL), TextSize.SMALL,
                this.style, x + GuideLayout.SESSION_PAD, y + 1, typed);
        int rowY = y + GuideLayout.SESSION_HEAD + GuideLayout.CODE_PAD;
        for (final List<GuidePiece.SessionRun> row : session.rows()) {
            int at = x + GuideLayout.SESSION_PAD;
            for (final GuidePiece.SessionRun run : row) {
                final int colour = switch (run.ink()) {
                    case PROMPT -> look.getOrDefault(TERMINAL_PROMPT, typed);
                    case TYPED -> typed;
                    case ANSWER -> look.getOrDefault(TERMINAL_TEXT, typed);
                };
                GuideFonts.draw(g, this.font, run.text(), TextSize.CODE, this.style, at, rowY, colour);
                at += GuideFonts.width(this.font, run.text(), TextSize.CODE, this.style);
            }
            rowY += GuideLayout.CODE_PITCH;
        }
    }

    /**
     * A key of the keyboard in a sentence: a pale keycap lit along its top and left, its edge shaded and a darker
     * line along its foot where it stands up from the board, its name on it in small letters.
     */
    private void drawKey(final GuiGraphics g, final int x, final int y, final String name, final String chapter) {
        final int width = GuideFonts.width(this.font, name, TextSize.SMALL, this.style) + GuideLayout.KEY_ROOM;
        final int face = ManualButtonLook.lighten(this.colour(GuideStyle.PAPER, chapter), KEY_LIGHT);
        ManualControls.bevel(g, x, y - 1, width, KEY_HEIGHT, face, ManualButtonLook.lighten(face, KEY_LIGHT),
                this.colour(GuideStyle.RULE, chapter));
        g.fill(x, y + KEY_HEIGHT - 2, x + width, y + KEY_HEIGHT - 1, this.colour(GuideStyle.FAINT, chapter));
        // The name in whichever of the page's ink and the label's reads on the key, as a drawing's pale ink would not.
        GuideFonts.draw(g, this.font, name, TextSize.SMALL, this.style, x + 2, y + 1, ManualButtonLook.markFor(face,
                this.colour(GuideStyle.INK, chapter), this.colour(GuideStyle.LABEL_INK, chapter)));
    }

    /** The paper, the strip and the frame of a listing, and the rule beside its numbers. */
    private void drawCodeFrame(final GuiGraphics g, final int x, final int y, final int width, final int height,
                               final int gutter, final String chapter) {
        final int rule = this.colour(GuideStyle.RULE, chapter);
        g.fill(x, y, x + width, y + GuideLayout.LISTING_HEAD, this.colour(GuideStyle.CODE_HEAD, chapter));
        g.fill(x, y + GuideLayout.LISTING_HEAD, x + width, y + height, this.colour(GuideStyle.CODE_PAPER, chapter));
        Draw.outline(g, x, y, width, height, rule);
        if (gutter > 0) {
            g.fill(x + gutter - 2, y + GuideLayout.LISTING_HEAD, x + gutter - 1, y + height, rule);
        }
    }

    /** The words on a listing's strip: its label in bold, then its file's name, cut short before its buttons. */
    private void drawStrip(final GuiGraphics g, final int x, final int y, final int room, final String label,
                           final String name, final String chapter) {
        final int ink = this.colour(GuideStyle.INK, chapter);
        GuideFonts.draw(g, this.font, label, TextSize.SMALL_BOLD, this.style, x + 3, y + 2, ink);
        final int nameX = x + 3 + GuideFonts.width(this.font, label, TextSize.SMALL_BOLD, this.style) + 4;
        GuideFonts.draw(g, this.font, this.fit(name, x + room - nameX, TextSize.SMALL), TextSize.SMALL, this.style,
                nameX, y + 2, ink);
    }

    /** A button on a listing's strip, lit under the pointer; Copy turns to Copied for a moment once it has copied. */
    private void drawListingButton(final GuiGraphics g, final ManualScreenLayout.Rect button, final TextKey word,
                                   final boolean done, final String chapter, final int mx, final int my) {
        final int face = done ? this.colour(GuideStyle.CODE_COPIED, chapter)
                : ManualButtonLook.lighten(this.colour(GuideStyle.CODE_HEAD, chapter), BUTTON_LIGHT);
        final ManualButtonLook look = ManualButtonLook.of(face, this.colour(GuideStyle.INK, chapter),
                this.colour(GuideStyle.LABEL, chapter), button.contains(mx, my) ? ManualButtonLook.State.HOVER
                        : ManualButtonLook.State.NORMAL);
        ManualControls.bevel(g, button.x(), button.y(), button.width(), button.height(), look.fill(), look.light(),
                look.dark());
        if (look.framed()) {
            Draw.outline(g, button.x(), button.y(), button.width(), button.height(), look.mark());
        }
        final String shown = this.text((done ? GuideTexts.COPIED : word).key());
        final int width = GuideFonts.width(this.font, shown, TextSize.SMALL, this.style);
        GuideFonts.draw(g, this.font, shown, TextSize.SMALL, this.style, button.x() + (button.width() - width) / 2,
                button.y() + 1, look.mark());
    }

    /**
     * A listing opened over the whole book: as wide as the binder, every line whole and never broken, scrolled by the
     * mouse wheel when it is longer than the book is tall, with Copy and Close on its strip.
     */
    private void drawOpened(final GuiGraphics g, final GuidePiece.Listing listing, final int mx, final int my) {
        final ManualScreenLayout.Rect box = this.openedBox(listing);
        this.drawCodeFrame(g, box.x(), box.y(), box.width(), box.height(), listing.gutter(), "");
        final ManualScreenLayout.Rect close = closeButton(box);
        final ManualScreenLayout.Rect copy = openedCopyButton(box);
        this.drawStrip(g, box.x(), box.y(), copy.x() - box.x() - 4, listing.label(), listing.name(), "");
        this.drawListingButton(g, copy, GuideTexts.COPY, this.justCopied(listing.lines()), "", mx, my);
        this.drawListingButton(g, close, GuideTexts.CLOSE, false, "", mx, my);
        final int top = box.y() + GuideLayout.LISTING_HEAD + GuideLayout.CODE_PAD;
        final int character = Math.max(1, GuideFonts.width(this.font, "0", TextSize.CODE, this.style));
        for (int i = 0; i < this.openedRows(listing) && this.openedScroll + i < listing.lines().size(); i++) {
            final String number = String.valueOf(this.openedScroll + i + 1);
            GuideFonts.draw(g, this.font, number, TextSize.CODE, this.style, box.x() + listing.gutter() - 3
                    - GuideFonts.width(this.font, number, TextSize.CODE, this.style),
                    top + i * GuideLayout.CODE_PITCH, this.colour(GuideStyle.FAINT));
        }
        // The code is cut at the frame and the rule beside its numbers, so a line scrolled across slides under them.
        g.enableScissor(this.left + box.x() + listing.gutter(), this.top + top, this.left + box.x() + box.width() - 1,
                this.top + box.y() + box.height() - 1);
        for (int i = 0; i < this.openedRows(listing) && this.openedScroll + i < listing.lines().size(); i++) {
            final int index = this.openedScroll + i;
            final int rowY = top + i * GuideLayout.CODE_PITCH;
            final String line = listing.lines().get(index);
            int at = box.x() + listing.gutter() + 1 - this.openedShift * character;
            for (final GuideCode.Run run : index < this.openedRuns.size() ? this.openedRuns.get(index)
                    : GuideCode.plain(List.of(line)).getFirst()) {
                final String piece = line.substring(run.start(), Math.min(line.length(), run.start() + run.length()));
                GuideFonts.draw(g, this.font, piece, TextSize.CODE, this.style, at, rowY,
                        this.colour(GuideCode.role(run.ink())));
                at += GuideFonts.width(this.font, piece, TextSize.CODE, this.style);
            }
        }
        g.disableScissor();
    }

    /** Where a listing opened over the whole book stands: as wide as the binder, as tall as it needs or the book. */
    private ManualScreenLayout.Rect openedBox(final GuidePiece.Listing listing) {
        final int width = this.geometry.coverRight() - OPENED_X - 4;
        final int whole = GuideLayout.LISTING_HEAD + 2 * GuideLayout.CODE_PAD
                + listing.lines().size() * GuideLayout.CODE_PITCH;
        return new ManualScreenLayout.Rect(OPENED_X, OPENED_Y, width, Math.min(whole,
                this.geometry.height() - OPENED_Y - OPENED_FOOT));
    }

    /** How far across a listing opened over the book can scroll: its longest line past what the book shows. */
    private int openedShiftMost(final GuidePiece.Listing listing) {
        final ManualScreenLayout.Rect box = this.openedBox(listing);
        final int character = Math.max(1, GuideFonts.width(this.font, "0", TextSize.CODE, this.style));
        final int shown = (box.width() - listing.gutter() - 4) / character;
        int longest = 0;
        for (final String line : listing.lines()) {
            longest = Math.max(longest, line.length());
        }
        return Math.max(0, longest - shown);
    }

    /** How many lines a listing opened over the book shows at once. */
    private int openedRows(final GuidePiece.Listing listing) {
        return (this.openedBox(listing).height() - GuideLayout.LISTING_HEAD - 2 * GuideLayout.CODE_PAD)
                / GuideLayout.CODE_PITCH;
    }

    /** The Copy button on a listing's strip, at its right end. */
    private static ManualScreenLayout.Rect copyButton(final int x, final int y, final int width) {
        return new ManualScreenLayout.Rect(x + width - COPY_WIDTH - 2, y + 1, COPY_WIDTH, LISTING_BUTTON);
    }

    /** The Close button of a listing opened over the book, at its strip's right end, Copy just before it. */
    private static ManualScreenLayout.Rect closeButton(final ManualScreenLayout.Rect box) {
        return new ManualScreenLayout.Rect(box.x() + box.width() - CLOSE_WIDTH - 2, box.y() + 1, CLOSE_WIDTH,
                LISTING_BUTTON);
    }

    /** The Copy button of a listing opened over the book, just before its Close. */
    private static ManualScreenLayout.Rect openedCopyButton(final ManualScreenLayout.Rect box) {
        return new ManualScreenLayout.Rect(closeButton(box).x() - 2 - COPY_WIDTH, box.y() + 1, COPY_WIDTH,
                LISTING_BUTTON);
    }

    /** Whether those lines were the last copied, a moment ago. */
    private boolean justCopied(final List<String> lines) {
        return lines.equals(this.copied) && Util.getMillis() - this.copiedAt < COPIED_MILLIS;
    }

    /** Copies a listing's lines whole, as they are typed, so the game's editors paste them. */
    private void copy(final List<String> lines) {
        this.minecraft.keyboardHandler.setClipboard(String.join("\n", lines));
        this.copied = lines;
        this.copiedAt = Util.getMillis();
        AudioEngine.playOnScreen(GuideSounds.CLICK);
    }

    /** Opens a listing over the whole book, coloured as its language colours it. */
    private void open(final GuidePiece.Listing listing) {
        this.opened = listing;
        this.openedRuns = GuideHighlighters.colour(listing.language(), listing.lines());
        this.openedScroll = 0;
        this.openedShift = 0;
        AudioEngine.playOnScreen(GuideSounds.CLICK);
    }

    /** The listing on a page shown under a point, with where it stands, or null. */
    @Nullable
    private PlacedListing listingAt(final double mx, final double my) {
        final int first = this.style.spread() ? this.spread * 2 : this.spread;
        final PlacedListing onLeft = this.listingOn(this.geometry.left(), first, mx, my);
        if (onLeft != null || this.geometry.right() == null || this.searching) {
            return onLeft;
        }
        return this.listingOn(this.geometry.right(), this.rightPage(first), mx, my);
    }

    @Nullable
    private PlacedListing listingOn(final ManualScreenLayout.Rect page, final int index, final double mx,
                                    final double my) {
        if (this.searching && page == this.geometry.left() || index < 0 || index >= this.book.pages().size()) {
            return null;
        }
        for (final GuidePiece piece : this.book.pages().get(index).pieces()) {
            if (piece instanceof GuidePiece.Listing listing) {
                final ManualScreenLayout.Rect rect = new ManualScreenLayout.Rect(page.x() + listing.x(),
                        page.y() + listing.y(), listing.width(), listing.height());
                if (rect.contains(mx, my)) {
                    return new PlacedListing(listing, rect.x(), rect.y());
                }
            }
        }
        return null;
    }

    /** The scene under the pointer on the pages shown, with where it stands on the screen; or null. */
    @Nullable
    private PlacedScene sceneAt(final double mx, final double my) {
        if (this.spread < 0 || this.opened != null) {
            return null;
        }
        final int first = this.style.spread() ? this.spread * 2 : this.spread;
        final PlacedScene onLeft = this.sceneOn(this.geometry.left(), first, mx, my);
        if (onLeft != null || this.geometry.right() == null || this.searching) {
            return onLeft;
        }
        return this.sceneOn(this.geometry.right(), this.rightPage(first), mx, my);
    }

    /** What {@code at} gives for the scene of that id where it stands on the spread shown; null when none shows it. */
    @Nullable
    private int[] onScene(final String id, final Function<PlacedScene, int[]> at) {
        final int first = this.style.spread() ? this.spread * 2 : this.spread;
        for (final int index : new int[] {first, this.rightPage(first)}) {
            final ManualScreenLayout.Rect page = index == first ? this.geometry.left() : this.geometry.right();
            if (page == null || index < 0 || index >= this.book.pages().size()) {
                continue;
            }
            for (final GuidePiece piece : this.book.pages().get(index).pieces()) {
                if (piece instanceof GuidePiece.Scene scene && scene.id().equals(id)) {
                    return at.apply(new PlacedScene(scene, page.x() + scene.x(), page.y() + scene.y()));
                }
            }
        }
        return null;
    }

    @Nullable
    private PlacedScene sceneOn(final ManualScreenLayout.Rect page, final int index, final double mx,
                                final double my) {
        if (this.searching && page == this.geometry.left() || index < 0 || index >= this.book.pages().size()) {
            return null;
        }
        for (final GuidePiece piece : this.book.pages().get(index).pieces()) {
            if (piece instanceof GuidePiece.Scene scene && new ManualScreenLayout.Rect(page.x() + scene.x(),
                    page.y() + scene.y(), scene.width(), scene.height()).contains(mx, my)) {
                return new PlacedScene(scene, page.x() + scene.x(), page.y() + scene.y());
            }
        }
        return null;
    }

    /** The items of a plate shown now: the row the plate has turned to. */
    private static List<String> plateRow(final GuidePiece.Plate plate) {
        final int perRow = platePerRow(plate);
        final int rows = (plate.items().size() + perRow - 1) / perRow;
        final int shown = rows <= 1 ? 0 : (int) (Util.getMillis() / PLATE_TURN_MILLIS % rows);
        return plate.items().subList(shown * perRow, Math.min(plate.items().size(), (shown + 1) * perRow));
    }

    private static int platePerRow(final GuidePiece.Plate plate) {
        return Math.max(1, (plate.width() - 2) / PLATE_PITCH);
    }

    /** How far into the plate its row starts, so the row stands in the middle of it. */
    private static int plateStart(final GuidePiece.Plate plate, final int count) {
        return (plate.width() - (count * PLATE_PITCH - 2)) / 2;
    }

    @Nullable
    private int[] plateItemOn(final ManualScreenLayout.Rect page, final int index, final String item) {
        if (index < 0 || index >= this.book.pages().size()) {
            return null;
        }
        for (final GuidePiece piece : this.book.pages().get(index).pieces()) {
            if (piece instanceof GuidePiece.Plate plate) {
                final List<String> row = plateRow(plate);
                final int at = row.indexOf(item);
                if (at >= 0) {
                    return new int[]{this.left + page.x() + plate.x() + plateStart(plate, row.size())
                            + at * PLATE_PITCH + 8, this.top + page.y() + plate.y() + 10};
                }
            }
        }
        return null;
    }

    private int[] linkPointOn(final ManualScreenLayout.Rect page, final int index, final String target) {
        if (index < 0 || index >= this.book.pages().size()) {
            return null;
        }
        for (final GuidePiece piece : this.book.pages().get(index).pieces()) {
            if (piece instanceof GuidePiece.Text text && text.link().equals(target)) {
                return new int[]{this.left + page.x() + text.x() + 2, this.top + page.y() + text.y() + 3};
            }
        }
        return null;
    }

    /** The item of a plate under a point of its page, or empty. */
    private static String plateItemAt(final GuidePiece.Plate plate, final double px, final double py) {
        if (py < plate.y() + 2 || py >= plate.y() + 18) {
            return "";
        }
        final List<String> row = plateRow(plate);
        final double across = px - plate.x() - plateStart(plate, row.size());
        final int at = (int) Math.floor(across / PLATE_PITCH);
        return across >= 0 && at < row.size() && across - at * PLATE_PITCH < 16 ? row.get(at) : "";
    }

    /**
     * A run of text. A link is underlined on a page and turns the accent's colour when pointed at on a drawing, where
     * an underline would read as line work.
     */
    private void drawText(final GuiGraphics g, final int x, final int y, final GuidePiece.Text text,
                          final String chapter, final int mx, final int my) {
        final int width = GuideFonts.width(this.font, text.text(), text.size(), this.style);
        final boolean pointed = !text.link().isEmpty() && mx >= x && mx < x + width && my >= y - 1
                && my < y + text.size().lineHeight();
        if (pointed && !this.drawing()) {
            // A link under the pointer lights the paper behind it with a pale wash of its own colour.
            g.fill(x - 1, y - 1, x + width + 1, y + LINK_DROP + 1, ManualButtonLook.lighten(
                    this.colour(GuideStyle.LINK, chapter), LINK_LIGHT));
        }
        final String role = pointed && this.drawing() ? GuideStyle.ACCENT : text.colour();
        GuideFonts.draw(g, this.font, text.text(), text.size(), this.style, x, y, this.colour(role, chapter));
        if (!text.link().isEmpty() && !this.drawing()) {
            g.fill(x, y + LINK_DROP, x + width, y + LINK_DROP + 1, this.colour(GuideStyle.LINK, chapter));
        }
    }

    private void drawLeader(final GuiGraphics g, final int x, final int y, final GuidePiece.Leader leader,
                            final String chapter, final int mx, final int my) {
        int textX = x;
        if (!leader.icon().isEmpty()) {
            this.drawItem(g, this.stackOf(leader.icon()), x, y - 2, ICON_SCALE_PERCENT, mx, my);
            textX += GuideLayout.ICON - 4;
        }
        if (!leader.lead().isEmpty()) {
            GuideFonts.draw(g, this.font, leader.lead(), TextSize.BOLD, this.style, textX, y,
                    this.colour(GuideStyle.HEADING, chapter));
            textX += GuideFonts.width(this.font, leader.lead(), TextSize.BOLD, this.style);
        }
        final int colour = this.colour(leader.link().isEmpty() ? leader.colour() : GuideStyle.INK, chapter);
        final int rightWidth = GuideFonts.width(this.font, leader.right(), leader.size(), this.style);
        final int rightX = x + leader.width() - rightWidth;
        // What is new since the reader last read, and what they cannot do yet, is said where the dots begin; the title
        // gives way for it, cut shorter, as it would for its page.
        final ReaderMarks.Mark mark = leader.link().isEmpty() ? ReaderMarks.Mark.NONE : this.readerMark(leader.link());
        final String said = mark == ReaderMarks.Mark.NONE ? "" : this.text(mark == ReaderMarks.Mark.NEW
                ? GuideTexts.MARK_NEW.key() : GuideTexts.MARK_NOT_YET.key());
        final int saidWidth = said.isEmpty() ? 0 : GuideFonts.width(this.font, said, TextSize.SMALL, this.style) + 3;
        // A title too long for its line is cut short before the page it leads to, never written over it.
        final String left = this.fit(leader.left(), rightX - textX - 4 - saidWidth, leader.size());
        GuideFonts.draw(g, this.font, left, leader.size(), this.style, textX, y, colour);
        int leftEnd = textX + GuideFonts.width(this.font, left, leader.size(), this.style) + 3;
        GuideFonts.draw(g, this.font, leader.right(), leader.size(), this.style, rightX, y, colour);
        if (!said.isEmpty()) {
            final GuidePalettes.Marks marks = GuidePalettes.MARKS.get();
            GuideFonts.draw(g, this.font, said, TextSize.SMALL, this.style, leftEnd, y + 1,
                    mark == ReaderMarks.Mark.NEW ? marks.fresh() : marks.notYet());
            leftEnd += saidWidth;
        }
        final int dots = this.colour(GuideStyle.FAINT, chapter);
        for (int dot = leftEnd; dot < rightX - 2; dot += DOT_PITCH) {
            g.fill(dot, y + DOT_DROP, dot + 1, y + DOT_DROP + 1, dots);
        }
        if (!leader.link().isEmpty() && mx >= x && mx < x + leader.width() && my >= y - 1 && my < y + 9) {
            g.fill(x, y + LINK_DROP, x + leader.width(), y + LINK_DROP + 1, this.colour(GuideStyle.LINK, chapter));
        }
    }

    private void drawRecipes(final GuiGraphics g, final int x, final int y, final GuidePiece.Recipes rows,
                             final String chapter, final int mx, final int my) {
        final List<GuideRecipes.View> views = this.recipes.of(rows.type(), rows.output());
        final TextSize nameSize = TextSize.SMALL;
        final int row = GuideLayout.recipeRow(this.style);
        for (int i = 0; i < rows.count() && rows.first() + i < views.size(); i++) {
            final GuideRecipes.View view = views.get(rows.first() + i);
            final int rowY = y + i * row + (row - SLOT) / 2;
            if (mx >= x && mx < x + rows.width() && my >= rowY && my < rowY + SLOT && !view.outputs().isEmpty()) {
                // A slot's item takes the place of what the row makes as soon as the pointer is found over it.
                this.recipeUnder = view.outputs().getFirst();
            }
            int slotX = x;
            for (final List<ItemStack> choices : view.inputs()) {
                this.drawSlot(g, slotX, rowY, this.cycle(choices), chapter, mx, my);
                slotX += SLOT + SLOT_GAP;
            }
            // One output is named beside its slot, as long as the arrow keeps room enough to read.
            final boolean named = view.outputs().size() == 1 && rows.width() - slotX + x > 2 * NAME_ROOM;
            final int nameRoom = named ? NAME_ROOM : 0;
            final int outputsWidth = view.outputs().size() * (SLOT + SLOT_GAP);
            final int arrowStart = slotX + 2;
            final int arrowEnd = x + rows.width() - nameRoom - outputsWidth - 4;
            final int ink = this.colour(GuideStyle.INK, chapter);
            final int arrowY = rowY + SLOT / 2 + 2;
            g.fill(arrowStart, arrowY, arrowEnd, arrowY + 1, ink);
            for (int head = 0; head < 4; head++) {
                g.fill(arrowEnd - head, arrowY - head, arrowEnd - head + 1, arrowY + head + 1, ink);
            }
            // A recipe locked behind knowledge the reader's team lacks wears a padlock on its arrow.
            if (view.locked()) {
                g.blitSprite(ProcessingRecipeViews.LOCK, arrowStart, arrowY - ProcessingRecipeViews.LOCK_HEIGHT / 2,
                        ProcessingRecipeViews.LOCK_WIDTH, ProcessingRecipeViews.LOCK_HEIGHT);
            }
            // The time stands over the arrow and the energy under it, as a recipe card is read.
            final int faint = this.colour(GuideStyle.FAINT, chapter);
            final String time = this.fit(view.time().getString(), arrowEnd - arrowStart, TextSize.SMALL);
            final int timeWidth = GuideFonts.width(this.font, time, TextSize.SMALL, this.style);
            GuideFonts.draw(g, this.font, time, TextSize.SMALL, this.style,
                    arrowStart + (arrowEnd - arrowStart - timeWidth) / 2, arrowY - 7, faint);
            final String energy = this.fit(view.energy().getString(), arrowEnd - arrowStart, TextSize.SMALL);
            final int energyWidth = GuideFonts.width(this.font, energy, TextSize.SMALL, this.style);
            GuideFonts.draw(g, this.font, energy, TextSize.SMALL, this.style,
                    arrowStart + (arrowEnd - arrowStart - energyWidth) / 2, arrowY + 3, faint);
            int outX = x + rows.width() - nameRoom - outputsWidth;
            for (final ItemStack output : view.outputs()) {
                this.drawSlot(g, outX, rowY, output, chapter, mx, my);
                outX += SLOT + SLOT_GAP;
            }
            if (named) {
                final String name = this.fit(view.outputs().getFirst().getHoverName().getString(), nameRoom - 2,
                        nameSize);
                GuideFonts.draw(g, this.font, name, nameSize, this.style, outX + 2, rowY + (SLOT - 6) / 2, ink);
            }
        }
    }

    private void drawSlot(final GuiGraphics g, final int x, final int y, final ItemStack stack, final String chapter,
                          final int mx, final int my) {
        g.fill(x, y, x + SLOT, y + SLOT, this.colour(GuideStyle.SHADE, chapter));
        Draw.outline(g, x, y, SLOT, SLOT, this.colour(GuideStyle.RULE, chapter));
        if (!stack.isEmpty() && mx >= x && mx < x + SLOT && my >= y && my < y + SLOT) {
            this.recipeUnder = stack;
        }
        this.drawItem(g, stack, x + 1, y + 1, 100, mx, my);
        if (!stack.isEmpty()) {
            g.renderItemDecorations(this.font, stack, x + 1, y + 1);
        }
    }

    /**
     * A block from above, the front and the side, each face in its outline with its name under it, the width of one
     * block under the front, and the balloons beside the top view, each with its dotted leader to what it names.
     */
    private void drawViews(final GuiGraphics g, final int x, final int y, final GuidePiece.Views views,
                           final int mx, final int my) {
        final ItemStack stack = this.stackOf(views.item());
        final int pitch = GuideLayout.VIEW_PITCH;
        this.drawFace(g, stack, GuideBlock.View.TOP, x, y, views.labels().get(0), mx, my);
        this.drawFace(g, stack, GuideBlock.View.FRONT, x, y + pitch, views.labels().get(1), mx, my);
        this.drawFace(g, stack, GuideBlock.View.SIDE, x + pitch, y + pitch, views.labels().get(2), mx, my);
        this.drawDimension(g, x, x + GuideLayout.VIEW - 1, y + 2 * pitch, views.labels().get(3));
        final int count = views.callouts().size();
        for (int i = 0; i < count; i++) {
            final GuideBlock.Callout callout = views.callouts().get(i);
            final int step = count < 2 ? 0 : Math.min(BALLOON_STEP, (views.width() - BALLOON_X - 2 * BALLOON)
                    / (count - 1));
            final int bx = x + BALLOON_X + i * step + (count < 2 ? BALLOON_STEP / 3 : 0);
            final int by = y + 10 + (i + 1) * BALLOON_ROOM / (count + 1);
            // The view a balloon points at stands where its face was drawn: above, or in the row under it.
            final int originX = callout.view() == GuideBlock.View.SIDE ? x + pitch : x;
            final int originY = callout.view() == GuideBlock.View.TOP ? y : y + pitch;
            final int scale = GuideLayout.VIEW_SCALE;
            this.drawBalloon(g, bx, by, callout.number(), originX + callout.u() * scale + 1,
                    originY + callout.v() * scale + 1);
        }
    }

    /** One face of a block in its outline, traced or as it looks, its name centred under it. */
    private void drawFace(final GuiGraphics g, final ItemStack stack, final GuideBlock.View view, final int x,
                          final int y, final String label, final int mx, final int my) {
        final int scale = GuideLayout.VIEW_SCALE;
        final TextureAtlasSprite sprite = this.faces.face(stack, view);
        if (sprite != null) {
            this.faces.draw(g, sprite, x, y, scale, this.style.decor().traced(), this.colour(GuideStyle.INK),
                    this.colour(GuideStyle.RULE));
        } else {
            Draw.outline(g, x, y, GuideLayout.VIEW, GuideLayout.VIEW, this.colour(GuideStyle.RULE));
            this.drawItem(g, stack, x + 8, y + 8, 200, mx, my);
        }
        if (mx >= x && my >= y && mx < x + GuideLayout.VIEW && my < y + GuideLayout.VIEW) {
            this.hoveredItem = stack;
        }
        // A name wider than its face is cut short rather than run into the next face's.
        final String name = this.fit(label, GuideLayout.VIEW - 1, TextSize.SMALL);
        final int labelWidth = GuideFonts.width(this.font, name, TextSize.SMALL, this.style);
        GuideFonts.draw(g, this.font, name, TextSize.SMALL, this.style, x + 8 * scale - labelWidth / 2,
                y + GuideLayout.VIEW + 3, this.colour(GuideStyle.FAINT));
    }

    /** A dimension line with its arrowheads and extension lines, the size written over its middle. */
    private void drawDimension(final GuiGraphics g, final int x0, final int x1, final int y, final String size) {
        final int faint = this.colour(GuideStyle.FAINT);
        g.fill(x0, y - 6, x0 + 1, y + 2, faint);
        g.fill(x1, y - 6, x1 + 1, y + 2, faint);
        g.fill(x0, y, x1, y + 1, faint);
        for (int i = 0; i < 3; i++) {
            g.fill(x0 + 1 + i, y - i, x0 + 2 + i, y + 1 + i, faint);
            g.fill(x1 - 1 - i, y - i, x1 - i, y + 1 + i, faint);
        }
        final int width = GuideFonts.width(this.font, size, TextSize.SMALL, this.style);
        final int mid = (x0 + x1) / 2 - width / 2;
        g.fill(mid - 2, y - 3, mid + width + 2, y + 4, this.colour(GuideStyle.PAPER));
        GuideFonts.draw(g, this.font, size, TextSize.SMALL, this.style, mid, y - 3, faint);
    }

    /** A numbered balloon, and its dotted leader to the place it names. */
    private void drawBalloon(final GuiGraphics g, final int x, final int y, final int number, final int toX,
                             final int toY) {
        final int faint = this.colour(GuideStyle.FAINT);
        final int steps = Math.max(Math.abs(toX - x), Math.abs(toY - y));
        for (int i = BALLOON + 1; i < steps; i += 2) {
            final int px = x + (toX - x) * i / steps;
            final int py = y + (toY - y) * i / steps;
            g.fill(px, py, px + 1, py + 1, faint);
        }
        final int ink = this.colour(GuideStyle.INK);
        g.fill(x - BALLOON + 1, y - BALLOON + 1, x + BALLOON, y + BALLOON, this.colour(GuideStyle.PAPER));
        for (int angle = 0; angle < 360; angle += 12) {
            final double radians = Math.toRadians(angle);
            final int px = (int) Math.round(x + BALLOON * Math.cos(radians));
            final int py = (int) Math.round(y + BALLOON * Math.sin(radians));
            g.fill(px, py, px + 1, py + 1, ink);
        }
        final String written = String.valueOf(number);
        final int width = GuideFonts.width(this.font, written, TextSize.SMALL, this.style);
        GuideFonts.draw(g, this.font, written, TextSize.SMALL, this.style, x - width / 2 + 1, y - 2, ink);
    }

    /** Blocks seen from above side by side, each named under it, an optional place outlined in dots. */
    private void drawPlan(final GuiGraphics g, final int x, final int y, final GuidePiece.Plan plan, final int mx,
                          final int my) {
        final int cell = GuideLayout.VIEW;
        int at = x + 2;
        for (final GuidePiece.PlanPart part : plan.parts()) {
            if (part.optional()) {
                final int faint = this.colour(GuideStyle.FAINT);
                for (int i = 0; i < cell; i += 3) {
                    g.fill(at + i, y, at + i + 1, y + 1, faint);
                    g.fill(at + i, y + cell - 1, at + i + 1, y + cell, faint);
                    g.fill(at + cell - 1, y + i, at + cell, y + i + 1, faint);
                }
                int lineY = y + 14;
                for (final String line : GuideLayout.wrap(part.label(), cell - 6, TextSize.SMALL,
                        new ClientGuideText(this.font, this.style, this.recipes))) {
                    GuideFonts.draw(g, this.font, line, TextSize.SMALL, this.style, at + 4, lineY, faint);
                    lineY += TextSize.SMALL.lineHeight();
                }
            } else {
                this.drawFace(g, this.stackOf(part.item()), GuideBlock.View.TOP, at, y, part.label(), mx, my);
            }
            at += cell;
        }
    }

    private ItemStack cycle(final List<ItemStack> choices) {
        if (choices.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return choices.get((int) (Util.getMillis() / CYCLE_MILLIS % choices.size()));
    }

    private void drawItem(final GuiGraphics g, final ItemStack stack, final int x, final int y, final int percent,
                          final int mx, final int my) {
        if (stack.isEmpty()) {
            return;
        }
        final float scale = percent / 100.0F;
        g.pose().pushPose();
        g.pose().translate(x, y, 0.0F);
        g.pose().scale(scale, scale, 1.0F);
        g.renderItem(stack, 0, 0);
        g.pose().popPose();
        final int size = Math.round(16 * scale);
        if (mx >= x && my >= y && mx < x + size && my < y + size) {
            this.hoveredItem = stack;
        }
    }

    private ItemStack stackOf(final String item) {
        if (item.isEmpty()) {
            return ItemStack.EMPTY;
        }
        final ResourceLocation id = ResourceLocation.tryParse(item);
        return id == null ? ItemStack.EMPTY : new ItemStack(BuiltInRegistries.ITEM.get(id));
    }

    /* The renderer a pack names, or none when the name is not a valid id. */
    private static IGuideBlockRenderer rendererOf(final String type) {
        final ResourceLocation id = ResourceLocation.tryParse(type);
        return id == null ? null : GuideBlockRenderers.get(id);
    }

    private CompoundTag dataOf(final String json) {
        return this.customData.computeIfAbsent(json, written -> {
            final Tag tag = new Dynamic<>(JsonOps.INSTANCE, JsonParser.parseString(written))
                    .convert(NbtOps.INSTANCE).getValue();
            return tag instanceof CompoundTag compound ? compound : new CompoundTag();
        });
    }

    private void drawSearch(final GuiGraphics g, final ManualScreenLayout.Rect page, final int mx, final int my) {
        final ManualScreenLayout.Rect field = ManualScreenLayout.searchField(this.geometry, this.style.margin());
        g.fill(field.x(), field.y(), field.x() + field.width(), field.y() + field.height(),
                this.colour(GuideStyle.LABEL));
        Draw.outline(g, field.x(), field.y(), field.width(), field.height(), this.colour(GuideStyle.RULE));
        final String head = this.text(GuideTexts.SEARCH.key()).toUpperCase(Locale.ROOT);
        final int margin = this.style.margin();
        GuideFonts.draw(g, this.font, head, TextSize.SMALL, this.style, page.x() + margin,
                page.y() + GuideLayout.HEADER_Y, this.colour(GuideStyle.FAINT));
        final List<GuideSearch.Hit> found = this.found();
        final int made = GuideSearch.count(found, GuideSearch.Found.RECIPE);
        final int used = GuideSearch.count(found, GuideSearch.Found.USE);
        final int width = page.width() - 2 * margin;
        // With recipes among them, the count says how many are pages, recipes and uses.
        final String count = found.isEmpty() ? this.text(GuideTexts.FOUND_NONE.key())
                : made + used == 0 ? this.text(GuideTexts.FOUND.key(), found.size())
                : this.text(GuideTexts.FOUND_SPLIT.key(), found.size(), found.size() - made - used, made, used);
        GuideFonts.draw(g, this.font, this.fit(count, width, TextSize.SMALL), TextSize.SMALL, this.style,
                page.x() + margin, field.y() + field.height() + 4, this.colour(GuideStyle.FAINT));
        for (final ResultRow row : this.resultRows()) {
            final GuideSearch.Hit hit = row.hit();
            // A recipe found stands under what it is, the recipe or the use, and says what it makes under that.
            final String title = switch (hit.found()) {
                case PAGE -> hit.text();
                case RECIPE -> this.text(GuideTexts.FOUND_RECIPE.key());
                case USE -> this.text(GuideTexts.FOUND_USE.key());
            };
            this.drawLeader(g, page.x() + margin, row.y(), new GuidePiece.Leader(0, 0, width, "", title,
                    hit.folio(), hit.term() ? TextSize.BODY : TextSize.BOLD, GuideStyle.INK, hit.target(),
                    hit.icon()), "", mx, my);
            if (!hit.line().isEmpty()) {
                // Where the words were found, set in under the entry's title: the line of its page that holds them.
                GuideFonts.draw(g, this.font, this.fit(hit.line(), width - LINE_INDENT, TextSize.SMALL),
                        TextSize.SMALL, this.style, page.x() + margin + LINE_INDENT,
                        row.y() + TextSize.BODY.lineHeight() + 1, this.colour(GuideStyle.FAINT));
            }
        }
    }

    /** Where each thing the search found stands on the left page, as many as fit above its foot. */
    private List<ResultRow> resultRows() {
        final ManualScreenLayout.Rect page = this.geometry.left();
        final ManualScreenLayout.Rect field = ManualScreenLayout.searchField(this.geometry, this.style.margin());
        final int bottom = page.y() + GuideLayout.bottom(this.style, this.style.pages().columns() - 1);
        final List<ResultRow> rows = new ArrayList<>();
        int y = field.y() + field.height() + 4 + TextSize.SMALL.lineHeight() + 4;
        for (final GuideSearch.Hit hit : this.found()) {
            final int height = TextSize.BODY.lineHeight() + 4 + (hit.line().isEmpty() ? 0
                    : TextSize.SMALL.lineHeight());
            if (y + height > bottom) {
                break;
            }
            rows.add(new ResultRow(hit, y, height));
            y += height;
        }
        return rows;
    }

    /** The thing the search found under a height of the left page, or null. */
    @Nullable
    private GuideSearch.Hit searchResultAt(final double my) {
        for (final ResultRow row : this.resultRows()) {
            if (my >= row.y() - 1 && my < row.y() + row.height() - 1) {
                return row.hit();
            }
        }
        return null;
    }

    /** The target of the link under the pointer, on either page, or empty. */
    private String linkAt(final double mx, final double my) {
        final int first = this.style.spread() ? this.spread * 2 : this.spread;
        final String onLeft = this.linkOn(this.geometry.left(), first, mx, my);
        if (!onLeft.isEmpty() || this.geometry.right() == null) {
            return onLeft;
        }
        return this.linkOn(this.geometry.right(), this.rightPage(first), mx, my);
    }

    /**
     * The page on the right: the one after the left, or, while the search is open, where the first thing found
     * stands, so the reader sees it among its neighbours: the page its words were found on, or its line of the index.
     */
    private int rightPage(final int first) {
        if (!this.searching) {
            return first + 1;
        }
        final List<GuideSearch.Hit> found = this.found();
        if (!found.isEmpty() && !found.getFirst().line().isEmpty()) {
            return found.getFirst().page();
        }
        if (!found.isEmpty()) {
            final String wanted = found.getFirst().text();
            final String target = found.getFirst().target();
            for (int page = this.book.indexAt(); page < this.book.pages().size(); page++) {
                for (final GuidePiece piece : this.book.pages().get(page).pieces()) {
                    // A line too long for the index runs on, its page beside its last words.
                    if (piece instanceof GuidePiece.Leader leader && leader.link().equals(target)
                            && wanted.endsWith(leader.left())) {
                        return page;
                    }
                }
            }
        }
        return this.book.indexAt();
    }

    private String linkOn(final ManualScreenLayout.Rect page, final int index, final double mx, final double my) {
        if (!page.contains(mx, my) || index < 0 || index >= this.book.pages().size()) {
            return "";
        }
        final double px = mx - page.x();
        final double py = my - page.y();
        for (final GuidePiece piece : this.book.pages().get(index).pieces()) {
            if (piece instanceof GuidePiece.Text text && !text.link().isEmpty() && py >= text.y() - 1
                    && py < text.y() + text.size().lineHeight() && px >= text.x()
                    && px < text.x() + GuideFonts.width(this.font, text.text(), text.size(), this.style)) {
                return text.link();
            }
            if (piece instanceof GuidePiece.Leader leader && !leader.link().isEmpty() && py >= leader.y() - 1
                    && py < leader.y() + leader.size().lineHeight() && px >= leader.x()
                    && px < leader.x() + leader.width()) {
                return leader.link();
            }
            // An item on a plate whose page is another entry leads to it, as a link would.
            if (piece instanceof GuidePiece.Plate plate) {
                final String item = plateItemAt(plate, px, py);
                if (!item.isEmpty()) {
                    return GuideLibrary.loaded().pageOf(item).filter(entry -> !entry.equals(plate.entry()))
                            .orElse("");
                }
            }
        }
        return "";
    }

    private void bevel(final GuiGraphics g, final int x, final int y, final int width, final int height,
                       final int fill, final int light, final int dark) {
        ManualControls.bevel(g, x, y, width, height, fill, light, dark);
    }

    private void drawRings(final GuiGraphics g) {
        final ManualScreenLayout.Rect left = this.geometry.left();
        final int from = left.x() + left.width() - RING_SPAN;
        final int to = this.geometry.right().x() + RING_SPAN;
        final int ring = this.colour(GuideStyle.RING);
        final int hole = this.colour(GuideStyle.FAINT);
        for (final int ringY : RINGS) {
            final int y = left.y() + ringY;
            g.fill(from + 2, y - 1, from + 5, y + 4, hole);
            g.fill(to - 5, y - 1, to - 2, y + 4, hole);
            g.fill(from, y, to, y + 3, ring);
            g.fill(from, y + 3, to, y + 4, this.shadowOf(ring));
        }
    }

    private void centred(final GuiGraphics g, final String text, final TextSize size, final int x, final int width,
                         final int y, final int colour) {
        final int textWidth = GuideFonts.width(this.font, text, size, this.style);
        GuideFonts.draw(g, this.font, text, size, this.style, x + (width - textWidth) / 2, y, colour);
    }

    /** The words as they fit the room, cut short with an ellipsis when they do not. */
    private String fit(final String words, final int room, final TextSize size) {
        if (GuideFonts.width(this.font, words, size, this.style) <= room) {
            return words;
        }
        String cut = words;
        while (!cut.isEmpty() && GuideFonts.width(this.font, cut + "...", size, this.style) > room) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut.stripTrailing() + "...";
    }

    private String text(final String key, final Object... args) {
        return new ClientGuideText(this.font, this.style, this.recipes).text(key, args);
    }

    /** The colour darkened, for the shaded edge of a bevel. */
    private int shadowOf(final int colour) {
        return ManualButtonLook.shaded(colour);
    }

    /** The colour lightened, for the lit edge of a bevel. */
    private int lighter(final int colour) {
        return ManualButtonLook.lit(colour);
    }

    /** The colour {@code share} of the way from {@code from} to {@code to}. */
    private int mix(final int from, final int to, final double share) {
        return ManualButtonLook.mix(from, to, share);
    }

    /**
     * The search's field: the game's own field for what is typed, drawn plain in the page's ink, with no shadow under
     * its letters, as every text of the series is drawn.
     */
    private static final class SearchField extends EditBox {

        private final Font font;
        private final int ink;

        private static final long BLINK_MILLIS = 300L;

        SearchField(final Font font, final int x, final int y, final int width, final int height, final int ink) {
            super(font, x, y, width, height, GameText.component(GuideTexts.SEARCH));
            this.font = font;
            this.ink = ink;
        }

        @Override
        public void renderWidget(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
            if (!this.isVisible()) {
                return;
            }
            final String shown = Texts.tail(this.font, this.getValue(), this.getWidth() - 2);
            Draw.text(g, this.font, shown, this.getX(), this.getY(), this.ink);
            if (this.isFocused() && Util.getMillis() / BLINK_MILLIS % 2 == 0) {
                final int cursorX = this.getX() + this.font.width(shown);
                g.fill(cursorX, this.getY() - 1, cursorX + 1, this.getY() + 9, this.ink);
            }
        }
    }

    /** A thing the search found, where its row stands on the left page and how tall the row is. */
    private record ResultRow(GuideSearch.Hit hit, int y, int height) {
    }

    /** A listing on a page shown, and where its frame stands on the binder. */
    private record PlacedListing(GuidePiece.Listing listing, int x, int y) {
    }

    /** A scene on a page shown, and where its frame stands on the screen. */
    private record PlacedScene(GuidePiece.Scene scene, int x, int y) {
    }

    /* What the reader's marks are told of the book as it is shown now. */
    private final class MarksHost implements ManualMarksView.Host {

        @Override
        public ManualScreenLayout.Geometry geometry() {
            return ManualScreen.this.geometry;
        }

        @Override
        public List<ManualMarksView.Shown> shown() {
            final ManualScreen screen = ManualScreen.this;
            if (screen.spread < 0 || screen.searching) {
                return List.of();
            }
            final List<ManualMarksView.Shown> shown = new ArrayList<>();
            final int first = screen.style.spread() ? screen.spread * 2 : screen.spread;
            if (first < screen.book.pages().size()) {
                shown.add(new ManualMarksView.Shown(first, screen.geometry.left(), screen.placeOf(first), false));
            }
            final int second = screen.rightPage(first);
            if (screen.geometry.right() != null && second >= 0 && second < screen.book.pages().size()) {
                shown.add(new ManualMarksView.Shown(second, screen.geometry.right(), screen.placeOf(second), true));
            }
            return shown;
        }

        @Override
        public List<ManualMarksView.Row> rows() {
            final ManualScreen screen = ManualScreen.this;
            final List<ManualMarksView.Row> rows = new ArrayList<>();
            for (final ManualMarks.Bookmark bookmark : ManualMarks.bookmarks(screen.manual.id())) {
                final OptionalInt page = screen.pageAt(bookmark.place());
                if (page.isPresent()) {
                    final String title = GuideLibrary.loaded().titleKeyOf(bookmark.place().target())
                            .map(screen::text).orElse(bookmark.place().target());
                    rows.add(new ManualMarksView.Row(bookmark.place(), bookmark.colour(), title,
                            screen.book.pages().get(page.getAsInt()).folio(), page.getAsInt()));
                }
            }
            return rows;
        }

        @Override
        public void goToPage(final int page) {
            ManualScreen.this.jump(page);
        }

        @Override
        public int colour(final String role) {
            return ManualScreen.this.colour(role);
        }
    }
}
