/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.rocket;

import dev.jstech.core.client.GameLocale;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.space.JsSpace;
import dev.jstech.space.api.RocketPart;
import dev.jstech.space.api.RocketParts;
import dev.jstech.space.flight.FlightComputerBlockEntity;
import dev.jstech.space.flight.StageChoice;
import dev.jstech.space.hangar.RocketDesign;
import dev.jstech.space.rocket.RocketStack;
import dev.jstech.space.rocket.RocketStages;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * The Flight Computer's stages page, as the approved one draws it: the rocket's stack as a schematic on the left, each
 * stage bracketed and numbered, the one chosen in white and the top one, which holds the computer, marked so; the
 * chosen stage's figures on the right, as the transfer page's; and along the bottom where every stage goes. Its left
 * keys choose a stage, its right keys set where it goes: anywhere, a fall zone round the pad and its radius, or a
 * platform to land on, stepped through the world's platforms.
 */
@TextHolder
@PaletteHolder
public final class FlightComputerStages {

    public static final TextKey STAGE = TextKey.of("jsspace.flight_computer.stage", "STAGE");
    public static final TextKey OF = TextKey.of("jsspace.flight_computer.stage_of", "%s OF %s");
    public static final TextKey DRY = TextKey.of("jsspace.flight_computer.dry", "DRY");
    public static final TextKey TONNES = TextKey.of("jsspace.flight_computer.tonnes", "%s T");
    public static final TextKey LEGS = TextKey.of("jsspace.flight_computer.legs", "LEGS");
    public static final TextKey ENGINE = TextKey.of("jsspace.flight_computer.engine", "ENGINE");
    public static final TextKey RETURN = TextKey.of("jsspace.flight_computer.return", "RETURN");
    public static final TextKey FATE = TextKey.of("jsspace.flight_computer.fate", "FATE");
    public static final TextKey PLATFORM_ROW = TextKey.of("jsspace.flight_computer.platform_row", "PLAT");
    public static final TextKey DISTANCE = TextKey.of("jsspace.flight_computer.distance", "DIST");
    public static final TextKey STATE = TextKey.of("jsspace.flight_computer.state", "STATE");
    public static final TextKey CENTRE = TextKey.of("jsspace.flight_computer.centre", "CENTRE");
    public static final TextKey RADIUS = TextKey.of("jsspace.flight_computer.radius", "RADIUS");
    public static final TextKey YES = TextKey.of("jsspace.flight_computer.yes", "YES");
    public static final TextKey NO = TextKey.of("jsspace.flight_computer.no", "NO");
    public static final TextKey CAN_LAND = TextKey.of("jsspace.flight_computer.can_land", "CAN LAND");
    public static final TextKey CANNOT = TextKey.of("jsspace.flight_computer.cannot", "CANNOT");
    public static final TextKey ANYWHERE = TextKey.of("jsspace.flight_computer.anywhere", "ANYWHERE");
    public static final TextKey FALL_ZONE = TextKey.of("jsspace.flight_computer.fall_zone", "FALL ZONE");
    public static final TextKey PLATFORM = TextKey.of("jsspace.flight_computer.platform", "PLATFORM");
    public static final TextKey PLATFORM_GONE = TextKey.of("jsspace.flight_computer.platform_gone", "GONE");
    public static final TextKey PLATFORM_SIDE = TextKey.of("jsspace.flight_computer.platform_side", "LP %s  %s BLK");
    public static final TextKey FREE = TextKey.of("jsspace.flight_computer.free", "FREE");
    public static final TextKey HELD = TextKey.of("jsspace.flight_computer.held", "HELD");
    public static final TextKey PAD = TextKey.of("jsspace.flight_computer.pad", "PAD");
    public static final TextKey COMPUTER = TextKey.of("jsspace.flight_computer.computer", "FC");
    public static final TextKey STAGE_NAME = TextKey.of("jsspace.flight_computer.stage_name", "S%s");
    public static final TextKey SUMMARY_ANY = TextKey.of("jsspace.flight_computer.summary_any", "ANY");
    public static final TextKey SUMMARY_ZONE = TextKey.of("jsspace.flight_computer.summary_zone", "ZONE R%s");
    public static final TextKey SUMMARY_PLATFORM = TextKey.of("jsspace.flight_computer.summary_platform",
            "PLAT LP %s");
    public static final TextKey STAYS = TextKey.of("jsspace.flight_computer.stays", "STAYS");
    public static final TextKey SET_BEFORE = TextKey.of("jsspace.flight_computer.set_before", "SET BEFORE LAUNCH");
    public static final TextKey NO_ROCKET = TextKey.of("jsspace.flight_computer.no_rocket", "NO ROCKET PLAN");
    public static final TextKey KEY_ANY = TextKey.of("jsspace.flight_computer.key_any", "ANY");
    public static final TextKey KEY_ZONE = TextKey.of("jsspace.flight_computer.key_zone", "ZONE");
    public static final TextKey KEY_PLATFORM = TextKey.of("jsspace.flight_computer.key_platform", "PLAT");
    public static final TextKey KEY_NEXT_PLATFORM = TextKey.of("jsspace.flight_computer.key_next_platform",
            "PLAT +");
    public static final TextKey KEY_RADIUS = TextKey.of("jsspace.flight_computer.key_radius", "RAD +");
    /** The colours of the stack's schematic. */
    public static final Palette<Colours> PALETTE = Palettes.declare(JsSpace.MODID, "flight_computer_stages",
            new Colours(0xFF9AA6B4, 0xFF55606E, 0xFFD8B040));
    private static final int SX0 = FlightComputerFace.SX0;
    private static final int SY0 = FlightComputerFace.SY0;
    private static final int SX1 = FlightComputerFace.SX1;
    private static final int SY1 = FlightComputerFace.SY1;
    /* The schematic: its column, its floor, how tall a block of the stack is drawn. */
    private static final int COLUMN_X0 = SX0 + 34;
    private static final int COLUMN_X1 = SX0 + 50;
    private static final int FLOOR = SY1 - 34;
    private static final int TOP_ROOM = SY0 + 16;
    private static final int MOST_SCALE = 4;
    private static final int BELL_INSET = 3;
    private static final int NOSE_INSET = 6;
    /* The figures' box, as the transfer page's, and the row of fates under everything. */
    private static final int DATA_X0 = SX0 + 110;
    private static final int DATA_X1 = SX1 - 46;
    private static final int ROW = 12;
    private static final float SMALL = 0.8F;
    private static final int KILOGRAMS_A_TONNE = 1000;
    private static final int SUMMARY_GAP = 14;

    private FlightComputerStages() {
    }

    /** What the stages page shows of {@code computer}'s rocket; none while it keeps no rocket's plan. */
    public static Shown shown(final FlightComputerBlockEntity computer) {
        final Level level = computer.getLevel();
        final Optional<RocketDesign> design = computer.rocket();
        if (level == null || design.isEmpty()) {
            return Shown.NONE;
        }
        final Map<ResourceLocation, RocketPart> parts = RocketParts.all(level);
        final RocketStack stack = design.get().stack();
        final List<List<Integer>> stages = RocketStages.stages(parts, stack);
        final List<Stage> out = new ArrayList<>();
        for (int k = 0; k < stages.size(); k++) {
            final int number = k + 1;
            final List<Piece> pieces = new ArrayList<>();
            boolean legs = false;
            boolean engine = false;
            for (final int place : stages.get(k)) {
                final RocketStack.Part placed = stack.sections().get(place).part();
                final RocketPart part = parts.get(placed.id());
                if (part != null) {
                    pieces.add(new Piece(part.kind(), part.height() * (part.stretches() ? placed.layers() : 1)));
                    legs |= part.kind() == RocketPart.Kind.LANDING_LEGS;
                    engine |= part.engine().isPresent();
                }
            }
            out.add(new Stage(number, pieces, RocketStages.dryMass(parts, stack, number), legs, engine,
                    computer.choices().get(number), computer.shownPlatforms().get(number)));
        }
        return new Shown(List.copyOf(out), Math.min(computer.stage(), Math.max(1, out.size())));
    }

    /** The left keys' legends: a stage to choose each, all but the top one. */
    public static List<FlightComputerFace.Legend> left(final Shown shown) {
        final List<FlightComputerFace.Legend> out = new ArrayList<>();
        for (int number = 1; number < shown.stages().size(); number++) {
            out.add(new FlightComputerFace.Legend(GameText.resolve(STAGE_NAME.with(number)), number == shown.chosen()
                    ? FlightComputerFace.Legend.State.CHOSEN : FlightComputerFace.Legend.State.LIVE, false));
        }
        return out;
    }

    /**
     * The right keys' legends for the stage chosen: anywhere, a fall zone, a platform, the next platform and the next
     * radius, where it goes now chosen; the platform's only for a stage that can come back, the radius's only for a
     * zone; none for the top stage.
     */
    public static List<FlightComputerFace.Legend> right(final Shown shown) {
        final Stage stage = shown.chosenStage();
        if (stage == null || stage.number() >= shown.stages().size()) {
            return List.of();
        }
        final StageChoice choice = stage.choice();
        final boolean back = stage.legs() && stage.engine();
        return List.of(legend(KEY_ANY, true, choice == null),
                legend(KEY_ZONE, true, choice != null && !choice.lands()),
                legend(KEY_PLATFORM, back, choice != null && choice.lands()),
                legend(KEY_NEXT_PLATFORM, back, false),
                legend(KEY_RADIUS, choice != null && !choice.lands(), false));
    }

    /** The stages page. */
    static void stages(final GuiGraphics g, final Font font, final FlightComputerFace.View view,
                       final FlightComputerFace.Colours c) {
        g.fill(SX0, SY0 + 12, SX1, SY1, c.space());
        final Shown shown = view.stages();
        if (shown.stages().isEmpty()) {
            final String none = GameText.resolve(NO_ROCKET);
            FlightComputerFace.text(g, font, none, (SX0 + SX1) / 2, (SY0 + SY1) / 2 - 4, c.amber(), true);
            return;
        }
        schematic(g, font, shown, c);
        figures(g, font, shown, c);
        summary(g, font, shown, c);
    }

    /* The stack, bottom up, each stage bracketed on its right and numbered; scaled to fit the screen's height. */
    private static void schematic(final GuiGraphics g, final Font font, final Shown shown,
                                  final FlightComputerFace.Colours c) {
        final Colours s = PALETTE.get();
        int blocks = 0;
        for (final Stage stage : shown.stages()) {
            for (final Piece piece : stage.pieces()) {
                blocks += piece.height();
            }
        }
        final int scale = Math.max(1, Math.min(MOST_SCALE, (FLOOR - TOP_ROOM) / Math.max(1, blocks)));
        int y = FLOOR;
        for (final Stage stage : shown.stages()) {
            final boolean chosen = stage.number() == shown.chosen();
            final int body = chosen ? s.steel() : s.steelDim();
            int top = y;
            for (final Piece piece : stage.pieces()) {
                final int h = Math.max(1, piece.height() * scale);
                piece(g, piece.kind(), top, h, body, s, c);
                top -= h;
            }
            final int colour = chosen ? c.white() : c.cyan();
            final int bx = COLUMN_X1 + 6;
            FlightComputerFace.vline(g, bx, top + 1, y - 1, colour);
            FlightComputerFace.hline(g, bx - 2, bx, top + 1, colour);
            FlightComputerFace.hline(g, bx - 2, bx, y - 2, colour);
            final String name = GameText.resolve(STAGE_NAME.with(stage.number()));
            final int ly = (top + y) / 2 - 4;
            FlightComputerFace.text(g, font, name, bx + 4, ly, colour, false);
            if (chosen) {
                FlightComputerFace.frame(g, bx + 2, ly - 1, bx + 6 + font.width(name), ly + 9, c.white());
            }
            if (stage.number() == shown.stages().size()) {
                FlightComputerFace.text(g, font, GameText.resolve(COMPUTER), bx + 9 + font.width(name), ly, c.green(),
                        false);
            }
            y = top - 1;
        }
        FlightComputerFace.hline(g, SX0 + 4, SX0 + 64, FLOOR + 1, c.ring());
        FlightComputerFace.text(g, font, GameText.resolve(PAD), SX0 + 4, FLOOR - 9, c.cyan(), false);
    }

    /* One part of the stack, its top at {@code top} going down {@code h}: a bell, a nose, legs, a ring or a body. */
    private static void piece(final GuiGraphics g, final RocketPart.Kind kind, final int top, final int h,
                              final int body, final Colours s, final FlightComputerFace.Colours c) {
        switch (kind) {
            case ENGINE -> {
                for (int k = 0; k < h; k++) {
                    final int inset = (h - k) * BELL_INSET / h;
                    FlightComputerFace.hline(g, COLUMN_X0 + 1 + inset, COLUMN_X1 - 1 - inset, top - k - 1, body);
                }
            }
            case CAPSULE, LANDER_ASCENT -> {
                for (int k = 0; k < h; k++) {
                    final int inset = k * NOSE_INSET / h;
                    FlightComputerFace.hline(g, COLUMN_X0 + inset, COLUMN_X1 - inset, top - k - 1, body);
                }
            }
            case LANDING_LEGS -> {
                for (int k = 0; k < h; k++) {
                    g.fill(COLUMN_X0 - 3 + k / 2, top - k - 1, COLUMN_X0 - 2 + k / 2, top - k, s.legs());
                    g.fill(COLUMN_X1 + 2 - k / 2, top - k - 1, COLUMN_X1 + 3 - k / 2, top - k, s.legs());
                }
            }
            case SEPARATION_RING, LANDER_DESCENT -> g.fill(COLUMN_X0, top - h, COLUMN_X1, top, c.amber());
            default -> {
                g.fill(COLUMN_X0, top - h, COLUMN_X1, top, body);
                FlightComputerFace.hline(g, COLUMN_X0, COLUMN_X1, top - h, c.ring());
            }
        }
    }

    /* The chosen stage's figures, in a box as the transfer page's. */
    private static void figures(final GuiGraphics g, final Font font, final Shown shown,
                                final FlightComputerFace.Colours c) {
        final Stage stage = shown.chosenStage();
        if (stage == null) {
            return;
        }
        final boolean top = stage.number() == shown.stages().size();
        final boolean back = stage.legs() && stage.engine();
        final List<String[]> rows = new ArrayList<>();
        final List<Integer> colours = new ArrayList<>();
        row(rows, colours, STAGE, GameText.resolve(OF.with(stage.number(), shown.stages().size())), c.white());
        row(rows, colours, DRY, GameText.resolve(TONNES.with(String.format(GameLocale.locale(), "%.1f",
                stage.dry() / KILOGRAMS_A_TONNE))), c.white());
        row(rows, colours, LEGS, GameText.resolve(stage.legs() ? YES : NO), stage.legs() ? c.green() : c.mars());
        row(rows, colours, ENGINE, GameText.resolve(stage.engine() ? YES : NO), stage.engine() ? c.green() : c.mars());
        row(rows, colours, RETURN, GameText.resolve(back ? CAN_LAND : CANNOT), back ? c.green() : c.mars());
        final StageChoice choice = stage.choice();
        if (top) {
            row(rows, colours, FATE, GameText.resolve(STAYS), c.cyan());
        } else if (choice == null) {
            row(rows, colours, FATE, GameText.resolve(ANYWHERE), c.white());
        } else if (choice.lands()) {
            row(rows, colours, FATE, GameText.resolve(PLATFORM), c.white());
            final FlightComputerBlockEntity.ShownPlatform platform = stage.platform();
            if (platform == null || platform.number() == 0) {
                row(rows, colours, PLATFORM_ROW, GameText.resolve(PLATFORM_GONE), c.amber());
            } else {
                row(rows, colours, PLATFORM_ROW, GameText.resolve(PLATFORM_SIDE.with(platform.number(),
                        platform.side())), c.white());
                row(rows, colours, DISTANCE, GameText.resolve(FlightComputerPages.BLOCKS.with(platform.distance())),
                        c.white());
                row(rows, colours, STATE, GameText.resolve(platform.free() ? FREE : HELD),
                        platform.free() ? c.green() : c.amber());
            }
        } else {
            row(rows, colours, FATE, GameText.resolve(FALL_ZONE), c.white());
            row(rows, colours, CENTRE, GameText.resolve(PAD), c.white());
            row(rows, colours, RADIUS, GameText.resolve(FlightComputerPages.BLOCKS.with(choice.radius())), c.white());
        }
        final int bottom = SY0 + 21 + rows.size() * ROW;
        g.fill(DATA_X0 - 4, SY0 + 14, DATA_X1 + 3, bottom, c.space());
        FlightComputerFace.frame(g, DATA_X0 - 4, SY0 + 14, DATA_X1 + 3, bottom, c.ring());
        g.pose().pushPose();
        g.pose().translate(DATA_X0, SY0 + 18, 0);
        g.pose().scale(SMALL, SMALL, 1);
        final int across = Math.round((DATA_X1 - DATA_X0) / SMALL);
        for (int i = 0; i < rows.size(); i++) {
            final int y = Math.round(i * ROW / SMALL);
            FlightComputerFace.text(g, font, rows.get(i)[0], 0, y, c.cyan(), false);
            FlightComputerFace.text(g, font, rows.get(i)[1], across - font.width(rows.get(i)[1]), y, colours.get(i),
                    false);
        }
        g.pose().popPose();
    }

    /* Where each stage goes, along the bottom, and that it is set before launch. */
    private static void summary(final GuiGraphics g, final Font font, final Shown shown,
                                final FlightComputerFace.Colours c) {
        final int y = SY1 - 24;
        int x = SX0 + 6;
        for (final Stage stage : shown.stages()) {
            final String name = GameText.resolve(STAGE_NAME.with(stage.number()));
            FlightComputerFace.text(g, font, name, x, y, c.cyan(), false);
            x += font.width(name) + 4;
            final String fate;
            final int colour;
            final StageChoice choice = stage.choice();
            if (stage.number() == shown.stages().size()) {
                fate = GameText.resolve(STAYS);
                colour = c.cyan();
            } else if (choice == null) {
                fate = GameText.resolve(SUMMARY_ANY);
                colour = c.white();
            } else if (choice.lands()) {
                final FlightComputerBlockEntity.ShownPlatform platform = stage.platform();
                fate = GameText.resolve(SUMMARY_PLATFORM.with(platform == null ? 0 : platform.number()));
                colour = c.green();
            } else {
                fate = GameText.resolve(SUMMARY_ZONE.with(choice.radius()));
                colour = c.amber();
            }
            FlightComputerFace.text(g, font, fate, x, y, colour, false);
            x += font.width(fate) + SUMMARY_GAP;
        }
        final String set = GameText.resolve(SET_BEFORE);
        FlightComputerFace.text(g, font, set, SX1 - 6 - font.width(set), y + 11, c.green(), false);
    }

    private static void row(final List<String[]> rows, final List<Integer> colours, final TextKey label,
                            final String value, final int colour) {
        rows.add(new String[] {GameText.resolve(label), value});
        colours.add(colour);
    }

    private static FlightComputerFace.Legend legend(final TextKey key, final boolean live, final boolean chosen) {
        return new FlightComputerFace.Legend(GameText.resolve(key), !live ? FlightComputerFace.Legend.State.OFF
                : chosen ? FlightComputerFace.Legend.State.CHOSEN : FlightComputerFace.Legend.State.LIVE, false);
    }

    /**
     * What the stages page shows.
     *
     * @param stages each stage of the rocket, from the bottom; none for no rocket
     * @param chosen the stage chosen, from one
     */
    public record Shown(List<Stage> stages, int chosen) {

        /** No rocket's plan kept. */
        public static final Shown NONE = new Shown(List.of(), 1);

        public Shown {
            stages = List.copyOf(stages);
        }

        /** The stage chosen, if the rocket has it. */
        public @Nullable Stage chosenStage() {
            return this.chosen >= 1 && this.chosen <= this.stages.size() ? this.stages.get(this.chosen - 1) : null;
        }
    }

    /**
     * A stage as the page shows it.
     *
     * @param number   its number, from one at the bottom
     * @param pieces   its parts, from the bottom
     * @param dry      its mass empty, in kilograms
     * @param legs     whether it has landing legs
     * @param engine   whether it has an engine
     * @param choice   where it is set to go; none for anywhere
     * @param platform the platform it is set to land on, as shown; none for one not set to land
     */
    public record Stage(int number, List<Piece> pieces, double dry, boolean legs, boolean engine,
                        @Nullable StageChoice choice, FlightComputerBlockEntity.@Nullable ShownPlatform platform) {

        public Stage {
            pieces = List.copyOf(pieces);
        }
    }

    /**
     * A part of the stack as the schematic draws it.
     *
     * @param kind   what kind of part it is
     * @param height how tall it is, in blocks
     */
    public record Piece(RocketPart.Kind kind, int height) {
    }

    /**
     * The colours of the stack's schematic.
     *
     * @param steel    the chosen stage's body
     * @param steelDim the other stages' bodies
     * @param legs     the landing legs
     */
    public record Colours(int steel, int steelDim, int legs) {
    }
}
