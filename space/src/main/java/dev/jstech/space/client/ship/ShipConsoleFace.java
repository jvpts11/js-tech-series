/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.ship;

import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import dev.jstech.space.JsSpace;
import dev.jstech.space.client.rocket.FlightComputerFace;
import dev.jstech.space.crew.CrewRole;
import dev.jstech.space.crew.CrewTexts;
import dev.jstech.space.crew.ShipCrew;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The face of a small ship's console as the approved crew page draws it, in the flight computer's display: the ship
 * and its seats and bunks along the top; a row a left key, the owner with a padlock, the owner's team, everyone else,
 * then the players named, five rows a page; the roles on the right keys for the owner, the row chosen boxed; what each
 * role does; and the scratchpad a name is typed into, or, for everyone else, their own role.
 */
@PaletteHolder
public final class ShipConsoleFace {

    /** The rows a page shows, one a left key. */
    public static final int ROWS = 5;
    /** The right keys' roles, from the top; the fifth names a player. */
    public static final List<CrewRole> ROLES = List.of(CrewRole.PILOT, CrewRole.CREW, CrewRole.PASSENGER,
            CrewRole.NONE);
    /** The keys along the top that turn the pages back and on. */
    public static final int PREVIOUS_KEY = 3;
    public static final int NEXT_KEY = 4;
    /** Where the rows end, on the right, in the face's pixels: clicked left of it, a row is chosen. */
    public static final int ROWS_RIGHT = FlightComputerFace.SX1 - 66;
    private static final int WHO_X = FlightComputerFace.SX0 + 8;
    private static final int NAME_X = FlightComputerFace.SX0 + 52;
    private static final int ROLE_RIGHT = FlightComputerFace.SX1 - 72;
    /* Where a chosen row's box ends, on the right. */
    private static final int ROLES_EDGE = ROLE_RIGHT + 4;
    /* The header's line under the page's name, the rule under it clear of the first row. */
    private static final int HEADER = 13;
    private static final int PAD_TOP = FlightComputerFace.SY1 - 16;
    private static final int KEY_LINE = FlightComputerFace.SY1 - 29;
    private static final int PAGE_LINE = FlightComputerFace.SY1 - 41;
    /** The page's own colours, beside the flight computer's. */
    public static final Palette<Colours> PALETTE = Palettes.declare(JsSpace.MODID, "ship_console", new Colours(
            0xFF0E2A1A, 0xFF08140E, 0xFF12301E));

    private ShipConsoleFace() {
    }

    /** Draws the face across {@code w} by {@code h} as {@code view} says, the face's pixels stretched to fit. */
    public static void paint(final GuiGraphics g, final int w, final int h, final View view) {
        final Font font = Minecraft.getInstance().font;
        final FlightComputerFace.Colours c = FlightComputerFace.PALETTE.get();
        final Colours own = PALETTE.get();
        g.pose().pushPose();
        g.pose().scale(w / (float) FlightComputerFace.WIDTH, h / (float) FlightComputerFace.HEIGHT, 1);
        FlightComputerFace.bezel(g, font, c, CrewTexts.MAKER, false);
        final int sx0 = FlightComputerFace.SX0;
        final int sy0 = FlightComputerFace.SY0;
        final int sx1 = FlightComputerFace.SX1;
        g.fill(sx0, sy0, sx1, FlightComputerFace.SY1, c.screen());
        top(g, font, view, c);
        // The ship, and its seats and bunks, the real bound of its crew.
        final String ship = GameText.resolve(CrewTexts.SHIP);
        FlightComputerFace.text(g, font, ship, WHO_X, sy0 + HEADER, c.cyan(), false);
        FlightComputerFace.text(g, font, view.shipName().toUpperCase(Locale.ROOT), WHO_X + font.width(ship) + 6,
                sy0 + HEADER, c.white(), false);
        final String seats = GameText.resolve(CrewTexts.SEATS.with(view.seats(), view.bunks()));
        FlightComputerFace.text(g, font, seats, sx1 - 8 - font.width(seats), sy0 + HEADER, c.windowNext(), false);
        FlightComputerFace.hline(g, sx0 + 4, sx1 - 4, sy0 + HEADER + 10, own.rule());
        final List<Row> rows = view.page();
        for (int i = 0; i < rows.size(); i++) {
            row(g, font, rows.get(i), i, view.owner() && view.chosen() == i, view.owner(), c, own);
        }
        FlightComputerFace.text(g, font, GameText.resolve(CrewTexts.PAGE.with(view.pageIndex() + 1, view.pages())),
                WHO_X, PAGE_LINE, c.windowNext(), false);
        keyLine(g, font, c);
        pad(g, font, view, c, own);
        if (view.owner()) {
            for (int i = 0; i < ROWS; i++) {
                FlightComputerFace.legend(g, font, rightLegends(view), i, false, c);
            }
        }
        g.pose().popPose();
    }

    /** The right keys' legends: the roles, the chosen row's boxed, and the key that names a player; for the owner. */
    public static List<FlightComputerFace.Legend> rightLegends(final View view) {
        if (!view.owner()) {
            return List.of();
        }
        final List<FlightComputerFace.Legend> out = new ArrayList<>();
        final CrewRole chosen = view.chosen() >= 0 && view.chosen() < view.page().size()
                ? view.page().get(view.chosen()).role() : null;
        for (final CrewRole role : ROLES) {
            out.add(new FlightComputerFace.Legend(GameText.resolve(role.shortName()), role == chosen
                    ? FlightComputerFace.Legend.State.CHOSEN : FlightComputerFace.Legend.State.LIVE, false));
        }
        out.add(new FlightComputerFace.Legend(GameText.resolve(CrewTexts.ADD), FlightComputerFace.Legend.State.LIVE,
                false));
        return out;
    }

    /**
     * The rows of {@code crew}, all its pages': the owner, the owner's team by {@code teamName} (or as the owner's,
     * for an owner on a team of their own), everyone else, the players named.
     */
    public static List<Row> rows(final ShipCrew crew, final String teamName) {
        final List<Row> out = new ArrayList<>();
        out.add(new Row(Row.Kind.OWNER, crew.ownerName(), CrewRole.PILOT, crew.owner()));
        out.add(new Row(Row.Kind.TEAM, teamName.isEmpty() ? GameText.resolve(CrewTexts.OWNERS_TEAM) : teamName,
                crew.team(), new UUID(0, 0)));
        out.add(new Row(Row.Kind.OTHERS, GameText.resolve(CrewTexts.ANYONE_ELSE), crew.others(), new UUID(0, 0)));
        for (final ShipCrew.Member member : crew.named()) {
            out.add(new Row(Row.Kind.NAMED, member.name(), member.role(), member.id()));
        }
        return List.copyOf(out);
    }

    /** The colour of {@code role} on the page. */
    public static int colourOf(final CrewRole role, final FlightComputerFace.Colours c) {
        return switch (role) {
            case PILOT -> c.magenta();
            case CREW -> c.green();
            case PASSENGER -> c.cyan();
            case NONE -> c.legendOff();
        };
    }

    /* The page's name boxed on the first key along the top, and the pages turned on the last two. */
    private static void top(final GuiGraphics g, final Font font, final View view, final FlightComputerFace.Colours c) {
        final String page = GameText.resolve(CrewTexts.PAGE_CREW);
        final int x = FlightComputerFace.topKeyX(0) + 12;
        final int y = FlightComputerFace.SY0 + 2;
        FlightComputerFace.frame(g, x - font.width(page) / 2 - 2, y - 1, x + (font.width(page) + 1) / 2 + 2, y + 9,
                c.white());
        FlightComputerFace.text(g, font, page, x, y, c.white(), true);
        FlightComputerFace.text(g, font, GameText.resolve(CrewTexts.PREVIOUS), FlightComputerFace.topKeyX(
                PREVIOUS_KEY) + 12, y, view.pageIndex() > 0 ? c.cyan() : c.legendOff(), true);
        FlightComputerFace.text(g, font, GameText.resolve(CrewTexts.NEXT), FlightComputerFace.topKeyX(NEXT_KEY) + 12,
                y, view.pageIndex() + 1 < view.pages() ? c.cyan() : c.legendOff(), true);
    }

    /* Row {@code i} beside its left key: who, the name, the role in its colour; the owner's padlocked. */
    private static void row(final GuiGraphics g, final Font font, final Row row, final int i, final boolean chosen,
                            final boolean keyed, final FlightComputerFace.Colours c, final Colours own) {
        final int y = FlightComputerFace.sideKeyY(i) + 2;
        if (chosen) {
            g.fill(WHO_X - 5, y - 3, ROLES_EDGE, y + 11, own.chosenGround());
            FlightComputerFace.frame(g, WHO_X - 5, y - 3, ROLES_EDGE, y + 11, c.white());
        }
        if (keyed && row.kind() != Row.Kind.OWNER) {
            FlightComputerFace.hline(g, FlightComputerFace.SX0, FlightComputerFace.SX0 + 4, y + 3, c.cyan());
        }
        FlightComputerFace.text(g, font, GameText.resolve(row.kind().who()), WHO_X, y, chosen ? c.white()
                : c.engrave(), false);
        // The display shows capitals only, a player's name and a team's too.
        FlightComputerFace.text(g, font, row.name().toUpperCase(Locale.ROOT), NAME_X, y, c.white(), false);
        final String role = GameText.resolve(row.role().shortName());
        final int roleX = ROLE_RIGHT - font.width(role);
        FlightComputerFace.text(g, font, role, roleX, y, colourOf(row.role(), c), false);
        if (row.kind() == Row.Kind.OWNER) {
            // A small padlock: its shackle and its body.
            final int lx = roleX - 12;
            FlightComputerFace.frame(g, lx + 1, y, lx + 6, y + 5, c.engraveDim());
            g.fill(lx, y + 3, lx + 7, y + 8, c.engraveDim());
        }
    }

    /* What each role does, in its colour. */
    private static void keyLine(final GuiGraphics g, final Font font, final FlightComputerFace.Colours c) {
        int x = WHO_X;
        for (final CrewRole role : List.of(CrewRole.PILOT, CrewRole.CREW, CrewRole.PASSENGER)) {
            final String name = GameText.resolve(role.shortName());
            FlightComputerFace.text(g, font, name, x, KEY_LINE, colourOf(role, c), false);
            x += font.width(name) + 4;
            final String does = GameText.resolve(role == CrewRole.PILOT ? CrewTexts.FLIES : role == CrewRole.CREW
                    ? CrewTexts.WORKS : CrewTexts.RIDES);
            FlightComputerFace.text(g, font, does, x, KEY_LINE, c.engrave(), false);
            x += font.width(does) + 12;
        }
    }

    /* The scratchpad a name is typed into, for the owner; for everyone else, their role and whose roles these are. */
    private static void pad(final GuiGraphics g, final Font font, final View view, final FlightComputerFace.Colours c,
                            final Colours own) {
        final int x0 = FlightComputerFace.SX0 + 4;
        final int x1 = FlightComputerFace.SX1 - 4;
        g.fill(x0, PAD_TOP, x1, PAD_TOP + 12, own.padGround());
        FlightComputerFace.frame(g, x0, PAD_TOP, x1, PAD_TOP + 12, c.windowPast());
        final int y = PAD_TOP + 2;
        if (view.owner()) {
            FlightComputerFace.text(g, font, "[ " + view.scratch().toUpperCase(Locale.ROOT) + (view.caret() ? "_"
                    : " ") + " ]", x0 + 4, y, c.white(), false);
            final String hint = GameText.resolve(CrewTexts.TYPE_NAME);
            FlightComputerFace.text(g, font, hint, x1 - 4 - font.width(hint), y, c.windowNext(), false);
        } else {
            FlightComputerFace.text(g, font, GameText.resolve(CrewTexts.YOU.with(GameText.resolve(
                    view.viewer().shortName()))), x0 + 4, y, colourOf(view.viewer(), c), false);
            final String owner = GameText.resolve(CrewTexts.OWNER_SETS);
            FlightComputerFace.text(g, font, owner, x1 - 4 - font.width(owner), y, c.amber(), false);
        }
    }

    /**
     * A row of the crew page.
     *
     * @param kind whose row it is
     * @param name the name it shows
     * @param role the role it shows
     * @param id   the player, for a row of a player
     */
    public record Row(Kind kind, String name, CrewRole role, UUID id) {

        /** Whose a row is, and what its first column says. */
        public enum Kind {
            OWNER(CrewTexts.OWNER), TEAM(CrewTexts.TEAM), OTHERS(CrewTexts.OTHERS), NAMED(CrewTexts.NAMED);

            private final TextKey who;

            Kind(final TextKey who) {
                this.who = who;
            }

            /** What its first column says. */
            public TextKey who() {
                return who;
            }
        }
    }

    /**
     * What the face shows.
     *
     * @param shipName  the ship's name
     * @param seats     its seats
     * @param bunks     its bunks
     * @param page      the rows of the page open
     * @param pageIndex which page is open, from nought
     * @param pages     how many pages there are
     * @param owner     whether the player looking is its owner, who sets the roles
     * @param chosen    the row chosen on the page open, or -1
     * @param viewer    the role of the player looking
     * @param scratch   the name typed so far
     * @param caret     whether the caret blinks on
     */
    public record View(String shipName, int seats, int bunks, List<Row> page, int pageIndex, int pages, boolean owner,
                       int chosen, CrewRole viewer, String scratch, boolean caret) {

        public View {
            page = List.copyOf(page);
        }
    }

    /**
     * The crew page's own colours.
     *
     * @param chosenGround the dark green under the row chosen
     * @param padGround    the scratchpad's ground
     * @param rule         the rule under the header
     */
    public record Colours(int chosenGround, int padGround, int rule) {
    }
}
