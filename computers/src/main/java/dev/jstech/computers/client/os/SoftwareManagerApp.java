/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.gui.layout.SoftwareManagerLayout;
import dev.jstech.computers.gui.layout.SoftwareManagerLayout.Rect;
import dev.jstech.computers.operation.payload.program.StorePayloads;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.ProgramShelf;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.os.ProgramVersions;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * Linux Mint's Software Manager (computers/docs/design/operating-systems.md): the programs the network's Mirror serves
 * this machine, filed by Mint's own categories with Network and Desktops beside them, with no ratings. Its front page
 * puts one program forward and shows the newest arrivals; a category or a search lists programs; a program's page
 * says what it takes and installs it with apt, as the Terminal would.
 */
@TextHolder
@PaletteHolder
public final class SoftwareManagerApp implements IDesktopApp {

    private final BlockPos host;
    private OsSkin skin;
    /** What the Mirror last said it serves, or null before it answered. */
    @Nullable
    private StorePayloads.Listing listing;
    private Page page = Page.FRONT;
    /** The list a program's page goes back to: the front page, a category or a search. */
    private Page backTo = Page.FRONT;
    private int category;
    /** The program a program's page shows, by the name it is got by. */
    private String program = "";
    private final StringBuilder search = new StringBuilder();
    private String searched = "";
    private boolean typing;
    private int scroll;
    /* Where the content was last drawn, which a click is measured against. */
    private int left;
    private int top;

    private static final TextKey CATEGORY_ACCESSORIES =
            TextKey.of("jsc.mint.software.category.accessories", "Accessories");
    private static final TextKey CATEGORY_GRAPHICS = TextKey.of("jsc.mint.software.category.graphics", "Graphics");
    private static final TextKey CATEGORY_INTERNET = TextKey.of("jsc.mint.software.category.internet", "Internet");
    private static final TextKey CATEGORY_OFFICE = TextKey.of("jsc.mint.software.category.office", "Office");
    private static final TextKey CATEGORY_PROGRAMMING =
            TextKey.of("jsc.mint.software.category.programming", "Programming");
    private static final TextKey CATEGORY_SOUND_AND_VIDEO =
            TextKey.of("jsc.mint.software.category.sound_and_video", "Sound and video");
    private static final TextKey CATEGORY_SYSTEM_TOOLS =
            TextKey.of("jsc.mint.software.category.system_tools", "System tools");
    private static final TextKey CATEGORY_NETWORK = TextKey.of("jsc.mint.software.category.network", "Network");
    private static final TextKey CATEGORY_GAMES = TextKey.of("jsc.mint.software.category.games", "Games");
    private static final TextKey CATEGORY_DESKTOPS = TextKey.of("jsc.mint.software.category.desktops", "Desktops");
    /** The categories, in the order the front page lays them out: Mint's own, and Network and Desktops. */
    private static final List<TextKey> CATEGORIES = List.of(CATEGORY_ACCESSORIES, CATEGORY_GRAPHICS,
            CATEGORY_INTERNET, CATEGORY_OFFICE, CATEGORY_PROGRAMMING, CATEGORY_SOUND_AND_VIDEO, CATEGORY_SYSTEM_TOOLS,
            CATEGORY_NETWORK, CATEGORY_GAMES, CATEGORY_DESKTOPS);
    /** The shelf each of those categories shows, in the same order. */
    private static final List<ProgramShelf> SHELVES = List.of(ProgramShelf.ACCESSORIES, ProgramShelf.GRAPHICS,
            ProgramShelf.INTERNET, ProgramShelf.OFFICE, ProgramShelf.PROGRAMMING, ProgramShelf.SOUND_AND_VIDEO,
            ProgramShelf.SYSTEM_TOOLS, ProgramShelf.NETWORK, ProgramShelf.GAMES, ProgramShelf.DESKTOPS);

    private static final TextKey NAME = TextKey.of("jsc.mint.software.name", "Software Manager");
    private static final TextKey SEARCH_HINT = TextKey.of("jsc.mint.software.search", "Search");
    private static final TextKey CATEGORIES_LABEL = TextKey.of("jsc.mint.software.categories", "Categories");
    private static final TextKey NEWEST = TextKey.of("jsc.mint.software.newest", "New on the Mirror");
    private static final TextKey INSTALLED = TextKey.of("jsc.mint.software.installed", "Installed");
    private static final TextKey INSTALL = TextKey.of("jsc.mint.software.install", "Install");
    private static final TextKey FOOTER = TextKey.of("jsc.mint.software.footer",
            "%s programs installed, from the Mirror of your network");
    private static final TextKey NO_MIRROR = TextKey.of("jsc.mint.software.no_mirror",
            "No Mirror answers on this network, so there is nothing to show.");
    private static final TextKey RESULTS = TextKey.of("jsc.mint.software.results", "Results for %s");
    private static final TextKey INSTALLED_HEADING = TextKey.of("jsc.mint.software.installed_heading",
            "Installed applications");
    private static final TextKey NOTHING_FOUND = TextKey.of("jsc.mint.software.nothing_found",
            "Nothing on the Mirror matches %s.");
    private static final TextKey VERSION = TextKey.of("jsc.mint.software.version", "Version");
    private static final TextKey SIZE = TextKey.of("jsc.mint.software.size", "Size");
    private static final TextKey MEMORY = TextKey.of("jsc.mint.software.memory", "Memory");
    private static final TextKey PACKAGE = TextKey.of("jsc.mint.software.package", "Package");
    private static final TextKey FROM = TextKey.of("jsc.mint.software.from", "From");
    private static final TextKey SIZE_VALUE = TextKey.of("jsc.mint.software.size_value", "%s MB on the disk");
    private static final TextKey MEMORY_VALUE = TextKey.of("jsc.mint.software.memory_value", "%s MB while it runs");
    private static final TextKey FROM_VALUE = TextKey.of("jsc.mint.software.from_value", "the Mirror of your network");
    /** The look its icons are drawn in, which is Cinnamon's. */
    private static final String ICONS = "cinnamon";
    /** The scale of the category tiles' words, the game's small font. */
    private static final float TILE_TEXT = 0.75F;

    /** Its own colours, {@code jsc:app/software_manager}: the featured banner and the tiles. */
    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "app/software_manager",
            new Colours(0xFF3E6A8A, 0xFF46749A, 0xFFFFFFFF, 0xFFD6E4EE, 0xFFFFFFFF, 0xFFE4E4E4, 0xFF3C6E1E));

    /** The open Software Manager that takes the Mirror's answers, or null when none is up. */
    @Nullable
    private static SoftwareManagerApp active;

    SoftwareManagerApp(final BlockPos host) {
        this.host = host;
        active = this;
        PacketDistributor.sendToServer(new StorePayloads.Ask(host));
    }

    /** Hands the open Software Manager what the Mirror serves. */
    public static void accept(final StorePayloads.Listing payload) {
        if (active != null && active.host.equals(payload.host())) {
            active.listing = payload;
        }
    }

    @Override
    public String title() {
        return GameText.resolve(NAME);
    }

    @Override
    public int defaultWidth() {
        return SoftwareManagerLayout.WIDTH;
    }

    @Override
    public int defaultHeight() {
        return SoftwareManagerLayout.HEIGHT;
    }

    @Override
    public void applySkin(final OsSkin given) {
        this.skin = given;
    }

    @Override
    public void onClosed() {
        if (active == this) {
            active = null;
        }
    }

    @Override
    public void renderContent(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                              final int height, final int mouseX, final int mouseY, final float partialTick) {
        this.left = x;
        this.top = y;
        if (this.skin == null) {
            return;
        }
        g.fill(x, y, x + width, y + height, this.skin.windowBg());
        bar(g, font, mouseX - x, mouseY - y);
        final StorePayloads.Listing shown = this.listing;
        if (shown == null) {
            return;
        }
        if (!shown.reachable()) {
            Draw.text(g, font, GameText.resolve(NO_MIRROR), x + 7, y + SoftwareManagerLayout.LIST_HEADING_Y,
                    this.skin.dim());
            return;
        }
        switch (this.page) {
            case PAGE -> programPage(g, font, shown, mouseX - x, mouseY - y);
            case CATEGORY, SEARCH, INSTALLED -> list(g, font, shown, mouseX - x, mouseY - y);
            default -> front(g, font, shown, mouseX - x, mouseY - y);
        }
    }

    @Override
    public void mouseClicked(final DesktopWindow window, final double mouseX, final double mouseY, final int button) {
        final double px = mouseX - this.left;
        final double py = mouseY - this.top;
        this.typing = SoftwareManagerLayout.SEARCH.contains(px, py);
        if (this.typing || this.listing == null || !this.listing.reachable()) {
            return;
        }
        if (this.page != Page.FRONT && SoftwareManagerLayout.BACK.contains(px, py)) {
            this.page = this.page == Page.PAGE ? this.backTo : Page.FRONT;
            this.scroll = 0;
            return;
        }
        // The menu shows what is installed, the first thing Mint's own menu offers.
        if (SoftwareManagerLayout.MENU.contains(px, py)) {
            this.page = Page.INSTALLED;
            this.scroll = 0;
            return;
        }
        switch (this.page) {
            case PAGE -> {
                final StorePayloads.Row row = rowNamed(this.listing, this.program);
                if (row != null && !row.installed() && SoftwareManagerLayout.ACTION.contains(px, py)) {
                    PacketDistributor.sendToServer(new StorePayloads.Get(this.host, row.program()));
                }
            }
            case CATEGORY, SEARCH, INSTALLED -> {
                final List<StorePayloads.Row> rows = listed(this.listing);
                final int at = SoftwareManagerLayout.rowAt(px, py);
                if (at >= 0 && this.scroll + at < rows.size()) {
                    open(rows.get(this.scroll + at).program(), this.page);
                }
            }
            default -> frontClicked(px, py);
        }
    }

    @Override
    public boolean charTyped(final char c) {
        if (!this.typing || c < ' ') {
            return false;
        }
        this.search.append(c);
        return true;
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        if (!this.typing) {
            return false;
        }
        if (key == GLFW.GLFW_KEY_BACKSPACE && !this.search.isEmpty()) {
            this.search.setLength(this.search.length() - 1);
        } else if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            this.searched = this.search.toString().strip();
            this.page = this.searched.isEmpty() ? Page.FRONT : Page.SEARCH;
            this.scroll = 0;
            this.typing = false;
        } else if (key == GLFW.GLFW_KEY_ESCAPE) {
            this.typing = false;
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(final double delta) {
        this.scroll = Math.max(0, this.scroll - (int) Math.signum(delta));
        return true;
    }

    /** The bar across the top: back, the search field with what is typed in it, and the menu's three lines. */
    private void bar(final GuiGraphics g, final Font font, final double mx, final double my) {
        final int x = this.left;
        final int y = this.top;
        g.fill(x, y, x + SoftwareManagerLayout.WIDTH, y + SoftwareManagerLayout.BAR_H, this.skin.panelBg());
        g.fill(x, y + SoftwareManagerLayout.BAR_H, x + SoftwareManagerLayout.WIDTH,
                y + SoftwareManagerLayout.BAR_H + 1, this.skin.edge());
        final Rect back = SoftwareManagerLayout.BACK;
        if (this.page != Page.FRONT) {
            Draw.text(g, font, "<", x + back.x() + 3, y + back.y() + 2,
                    back.contains(mx, my) ? this.skin.text() : this.skin.dim());
        }
        final Rect field = SoftwareManagerLayout.SEARCH;
        this.skin.field(g, x + field.x(), y + field.y(), field.w(), field.h(), this.typing);
        final String typed = this.search.toString();
        Draw.text(g, font, typed.isEmpty() && !this.typing ? GameText.resolve(SEARCH_HINT)
                        : Texts.tail(font, typed + (this.typing ? "_" : ""), field.w() - 6),
                x + field.x() + 3, y + field.y() + 2, typed.isEmpty() ? this.skin.dim() : this.skin.text());
        final Rect menu = SoftwareManagerLayout.MENU;
        for (int i = 0; i < 3; i++) {
            g.fill(x + menu.x() + 1, y + menu.y() + 2 + i * 3, x + menu.right() - 1, y + menu.y() + 3 + i * 3,
                    this.skin.dim());
        }
    }

    /** The front page: the program put forward, the categories, and the newest arrivals. */
    private void front(final GuiGraphics g, final Font font, final StorePayloads.Listing shown, final double mx,
                       final double my) {
        final Colours c = PALETTE.get();
        final int x = this.left;
        final int y = this.top;
        final StorePayloads.Row featured = featured(shown);
        final Rect banner = SoftwareManagerLayout.FEATURED;
        g.fill(x + banner.x(), y + banner.y(), x + banner.right(), y + banner.bottom(), c.banner());
        for (int i = 0; i < banner.w(); i += 8) {
            g.fill(x + banner.x() + i, y + banner.y(), x + banner.x() + i + 2, y + banner.bottom(), c.stripe());
        }
        if (featured != null) {
            final ProgramSpec spec = specOf(featured.program());
            icon(g, x + banner.x() + SoftwareManagerLayout.FEATURED_ICON_X,
                    y + banner.y() + SoftwareManagerLayout.FEATURED_ICON_Y, featured.program(), 2);
            final int tx = x + banner.x() + SoftwareManagerLayout.FEATURED_TEXT_X;
            final int room = banner.right() - SoftwareManagerLayout.FEATURED_TEXT_X - 6;
            Draw.text(g, font, Texts.clip(font, nameOf(featured, spec), room), tx,
                    y + banner.y() + SoftwareManagerLayout.FEATURED_NAME_Y, c.bannerInk());
            Draw.text(g, font, Texts.clip(font, houseOf(spec), room), tx,
                    y + banner.y() + SoftwareManagerLayout.FEATURED_HOUSE_Y, c.bannerDim());
            Draw.text(g, font, Texts.clip(font, aboutOf(spec), room), tx,
                    y + banner.y() + SoftwareManagerLayout.FEATURED_LINE_Y, c.bannerInk());
        }
        Draw.text(g, font, GameText.resolve(CATEGORIES_LABEL), x + banner.x(),
                y + SoftwareManagerLayout.CATEGORIES_LABEL_Y, this.skin.text());
        // The tiles' words in the small font, as Mint sets them: "Sound and video" fits a fifth of the window so.
        for (int i = 0; i < CATEGORIES.size(); i++) {
            final Rect r = SoftwareManagerLayout.category(i);
            tile(g, x + r.x(), y + r.y(), r.w(), r.h(), r.contains(mx, my));
            final String label = Texts.clip(font, GameText.resolve(CATEGORIES.get(i)),
                    (int) ((r.w() - 4) / TILE_TEXT));
            final int w = Math.round(font.width(label) * TILE_TEXT);
            Texts.scaled(g, font, label, x + r.x() + (r.w() - w) / 2, y + r.y() + 4, TILE_TEXT, this.skin.text());
        }
        Draw.text(g, font, GameText.resolve(NEWEST), x + banner.x(), y + SoftwareManagerLayout.NEWEST_LABEL_Y,
                this.skin.text());
        final List<StorePayloads.Row> newest = newest(shown);
        for (int i = 0; i < newest.size(); i++) {
            final Rect r = SoftwareManagerLayout.newest(i);
            final StorePayloads.Row row = newest.get(i);
            final ProgramSpec spec = specOf(row.program());
            tile(g, x + r.x(), y + r.y(), r.w(), r.h(), r.contains(mx, my));
            icon(g, x + r.x() + 5, y + r.y() + 5, row.program(), 1);
            Draw.text(g, font, Texts.clip(font, nameOf(row, spec), r.w() - 30), x + r.x() + 25, y + r.y() + 9,
                    this.skin.text());
            Draw.text(g, font, Texts.clip(font, houseOf(spec), r.w() - 10), x + r.x() + 5, y + r.y() + 24,
                    this.skin.dim());
            Draw.text(g, font, Texts.clip(font, aboutOf(spec), r.w() - 10), x + r.x() + 5, y + r.y() + 34,
                    this.skin.text());
            if (row.installed()) {
                Draw.text(g, font, GameText.resolve(INSTALLED), x + r.x() + 5, y + r.y() + 44, c.installed());
            }
        }
        final long installed = shown.rows().stream().filter(StorePayloads.Row::installed).count();
        Draw.text(g, font, Texts.clip(font, GameText.resolve(FOOTER.with(installed)), banner.w()), x + banner.x(),
                y + SoftwareManagerLayout.FOOTER_Y, this.skin.dim());
    }

    /** A program's own page: what it is, what it takes, and the button that installs it. */
    private void programPage(final GuiGraphics g, final Font font, final StorePayloads.Listing shown,
                             final double mx, final double my) {
        final StorePayloads.Row row = rowNamed(shown, this.program);
        if (row == null) {
            return;
        }
        final ProgramSpec spec = specOf(row.program());
        final int x = this.left;
        final int y = this.top;
        final Rect at = SoftwareManagerLayout.PAGE_ICON_AT;
        icon(g, x + at.x(), y + at.y(), row.program(), 2);
        final Rect action = SoftwareManagerLayout.ACTION;
        final int room = action.x() - SoftwareManagerLayout.PAGE_NAME_X - 6;
        Draw.text(g, font, Texts.clip(font, nameOf(row, spec), room), x + SoftwareManagerLayout.PAGE_NAME_X,
                y + SoftwareManagerLayout.PAGE_NAME_Y, this.skin.text());
        Draw.text(g, font, Texts.clip(font, houseOf(spec), room), x + SoftwareManagerLayout.PAGE_NAME_X,
                y + SoftwareManagerLayout.PAGE_HOUSE_Y, this.skin.dim());
        if (row.installed()) {
            final String label = GameText.resolve(INSTALLED);
            Draw.text(g, font, label, x + action.right() - font.width(label), y + action.y() + 3,
                    PALETTE.get().installed());
        } else {
            this.skin.button(g, font, x + action.x(), y + action.y(), action.w(), action.h(),
                    GameText.resolve(INSTALL), action.contains(mx, my), false, true);
        }
        g.fill(x + SoftwareManagerLayout.FEATURED.x(), y + SoftwareManagerLayout.PAGE_RULE_Y,
                x + SoftwareManagerLayout.FEATURED.right(), y + SoftwareManagerLayout.PAGE_RULE_Y + 1,
                this.skin.edge());
        final List<String[]> facts = List.of(
                new String[] {GameText.resolve(VERSION), ProgramVersions.of(spec == null ? "" : spec.id().toString())},
                new String[] {GameText.resolve(SIZE), GameText.resolve(SIZE_VALUE.with(spec == null ? 0
                        : spec.minDiskMb()))},
                new String[] {GameText.resolve(MEMORY), GameText.resolve(MEMORY_VALUE.with(spec == null ? 0
                        : spec.ramMbOn(eraHere())))},
                new String[] {GameText.resolve(PACKAGE), row.program()},
                new String[] {GameText.resolve(FROM), GameText.resolve(FROM_VALUE)});
        for (int i = 0; i < facts.size(); i++) {
            final int fy = y + SoftwareManagerLayout.FACTS_TOP + i * SoftwareManagerLayout.FACT_PITCH;
            Draw.text(g, font, facts.get(i)[0], x + SoftwareManagerLayout.FACT_LABEL_X, fy, this.skin.dim());
            Draw.text(g, font, facts.get(i)[1], x + SoftwareManagerLayout.FACT_VALUE_X, fy, this.skin.text());
        }
        final int width = SoftwareManagerLayout.WIDTH - 2 * SoftwareManagerLayout.FACT_LABEL_X;
        int ly = y + SoftwareManagerLayout.ABOUT_TOP;
        for (final FormattedCharSequence line : font.split(Component.literal(aboutOf(spec)), width)) {
            Draw.text(g, font, line, x + SoftwareManagerLayout.FACT_LABEL_X, ly, this.skin.text());
            ly += SoftwareManagerLayout.LINE + 1;
        }
    }

    /** A category's programs, or what a search found, a row each with its icon, name, house and what it does. */
    private void list(final GuiGraphics g, final Font font, final StorePayloads.Listing shown, final double mx,
                      final double my) {
        final int x = this.left;
        final int y = this.top;
        final List<StorePayloads.Row> rows = listed(shown);
        final String heading = switch (this.page) {
            case SEARCH -> GameText.resolve(RESULTS.with(this.searched));
            case INSTALLED -> GameText.resolve(INSTALLED_HEADING);
            default -> GameText.resolve(CATEGORIES.get(this.category));
        };
        Draw.text(g, font, Texts.clip(font, heading, SoftwareManagerLayout.FEATURED.w()),
                x + SoftwareManagerLayout.FEATURED.x(), y + SoftwareManagerLayout.LIST_HEADING_Y, this.skin.text());
        if (rows.isEmpty() && this.page == Page.SEARCH) {
            Draw.text(g, font, GameText.resolve(NOTHING_FOUND.with(this.searched)),
                    x + SoftwareManagerLayout.FEATURED.x(), y + SoftwareManagerLayout.LIST_TOP, this.skin.dim());
        }
        this.scroll = Math.max(0, Math.min(this.scroll, rows.size() - SoftwareManagerLayout.ROWS));
        for (int i = 0; i < SoftwareManagerLayout.ROWS && this.scroll + i < rows.size(); i++) {
            final StorePayloads.Row row = rows.get(this.scroll + i);
            final ProgramSpec spec = specOf(row.program());
            final Rect r = SoftwareManagerLayout.row(i);
            this.skin.listRow(g, x + r.x(), y + r.y(), r.w(), r.h(), r.contains(mx, my), false);
            icon(g, x + r.x() + 2, y + r.y() + 1, row.program(), 1);
            final String state = row.installed() ? GameText.resolve(INSTALLED) : "";
            final int room = r.right() - SoftwareManagerLayout.ROW_TEXT_X - font.width(state) - 8;
            Draw.text(g, font, Texts.clip(font, nameOf(row, spec), room), x + SoftwareManagerLayout.ROW_TEXT_X,
                    y + r.y(), this.skin.text());
            Draw.text(g, font, Texts.clip(font, aboutOf(spec), room), x + SoftwareManagerLayout.ROW_TEXT_X,
                    y + r.y() + SoftwareManagerLayout.LINE, this.skin.dim());
            Draw.text(g, font, state, x + r.right() - font.width(state) - 3, y + r.y() + 4,
                    PALETTE.get().installed());
        }
    }

    /** A click on the front page: the program put forward, a category, or one of the newest. */
    private void frontClicked(final double px, final double py) {
        final StorePayloads.Listing shown = this.listing;
        final StorePayloads.Row featured = featured(shown);
        if (featured != null && SoftwareManagerLayout.FEATURED.contains(px, py)) {
            open(featured.program(), Page.FRONT);
            return;
        }
        for (int i = 0; i < CATEGORIES.size(); i++) {
            if (SoftwareManagerLayout.category(i).contains(px, py)) {
                this.category = i;
                this.page = Page.CATEGORY;
                this.scroll = 0;
                return;
            }
        }
        final List<StorePayloads.Row> newest = newest(shown);
        for (int i = 0; i < newest.size(); i++) {
            if (SoftwareManagerLayout.newest(i).contains(px, py)) {
                open(newest.get(i).program(), Page.FRONT);
                return;
            }
        }
    }

    private void open(final String name, final Page from) {
        this.program = name;
        this.backTo = from;
        this.page = Page.PAGE;
    }

    /* The programs the page that is up lists: a category's, or those whose name or package holds the search. */
    private List<StorePayloads.Row> listed(final StorePayloads.Listing shown) {
        final List<StorePayloads.Row> out = new ArrayList<>();
        final String needle = this.searched.toLowerCase(Locale.ROOT);
        for (final StorePayloads.Row row : shown.rows()) {
            final boolean wanted = switch (this.page) {
                case SEARCH -> row.program().toLowerCase(Locale.ROOT).contains(needle)
                        || row.name().toLowerCase(Locale.ROOT).contains(needle);
                case INSTALLED -> row.installed();
                default -> categoryOf(specOf(row.program())) == this.category;
            };
            if (wanted) {
                out.add(row);
            }
        }
        return out;
    }

    /** A tile of the front page: white with a light edge, lit while the pointer is on it. */
    private void tile(final GuiGraphics g, final int x, final int y, final int w, final int h, final boolean hot) {
        final Colours c = PALETTE.get();
        g.fill(x, y, x + w, y + h, c.tileEdge());
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, hot ? this.skin.listHover() : c.tile());
    }

    /**
     * The program put forward on the front page: one of those not installed yet, a different one each day of the
     * world, and of all of them once everything is installed.
     */
    @Nullable
    private static StorePayloads.Row featured(final StorePayloads.Listing shown) {
        final List<StorePayloads.Row> candidates = new ArrayList<>();
        for (final StorePayloads.Row row : shown.rows()) {
            if (!row.installed()) {
                candidates.add(row);
            }
        }
        final List<StorePayloads.Row> from = candidates.isEmpty() ? shown.rows() : candidates;
        if (from.isEmpty()) {
            return null;
        }
        final long day = Minecraft.getInstance().level == null ? 0L
                : Minecraft.getInstance().level.getDayTime() / 24_000L;
        return from.get((int) Math.floorMod(day, (long) from.size()));
    }

    /** The newest arrivals: the last programs the Mirror's catalogue took in, newest first. */
    private static List<StorePayloads.Row> newest(final StorePayloads.Listing shown) {
        final List<StorePayloads.Row> rows = shown.rows();
        final List<StorePayloads.Row> out = new ArrayList<>();
        for (int i = rows.size() - 1; i >= 0 && out.size() < SoftwareManagerLayout.NEWEST_COUNT; i--) {
            out.add(rows.get(i));
        }
        return out;
    }

    @Nullable
    private static StorePayloads.Row rowNamed(final StorePayloads.Listing shown, final String name) {
        for (final StorePayloads.Row row : shown.rows()) {
            if (row.program().equals(name)) {
                return row;
            }
        }
        return null;
    }

    /**
     * The category a program is filed under: its own, else the desktops for a desktop and the network for a service.
     */
    static int categoryOf(@Nullable final ProgramSpec spec) {
        return SHELVES.indexOf(ProgramShelf.of(spec));
    }

    /** The program a row gets, by the name it is got by. */
    @Nullable
    private static ProgramSpec specOf(final String command) {
        for (final ProgramSpec spec : OsRegistry.programs()) {
            if (spec.commandName().equals(command)) {
                return spec;
            }
        }
        return null;
    }

    private static String nameOf(final StorePayloads.Row row, @Nullable final ProgramSpec spec) {
        return spec == null ? row.name() : GameText.resolve(spec.name());
    }

    private static String houseOf(@Nullable final ProgramSpec spec) {
        return spec == null ? "" : spec.house().name();
    }

    private static String aboutOf(@Nullable final ProgramSpec spec) {
        return spec == null ? "" : GameText.resolve(TextKey.of(spec.descriptionKey(), spec.description()));
    }

    /* The icon of a program in Cinnamon's look, at its size or at twice it. */
    private static void icon(final GuiGraphics g, final int x, final int y, final String command, final int scale) {
        final ProgramSpec spec = specOf(command);
        final ResourceLocation id = spec == null
                ? ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "generic") : spec.id();
        g.pose().pushPose();
        g.pose().translate(x, y, 0.0F);
        g.pose().scale(scale, scale, 1.0F);
        ProgramIcons.draw(g, 0, 0, 16, 16, id, ICONS);
        g.pose().popPose();
    }

    /** The era of the machine the window is on, for what a program holds in memory there; none while no desktop is. */
    @Nullable
    private static Era eraHere() {
        final DesktopState desktop = DesktopScreen.current();
        return desktop == null ? null : desktop.prefs().era();
    }

    /** Which page is up: the front, a list (a category's, a search's, or what is installed), or one program's. */
    private enum Page {
        FRONT, CATEGORY, SEARCH, INSTALLED, PAGE
    }

    /** Its own colours: the banner, its stripes and words, a tile and its edge, and the green of Installed. */
    private record Colours(int banner, int stripe, int bannerInk, int bannerDim, int tile, int tileEdge,
                           int installed) {
    }
}
