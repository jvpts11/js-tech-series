/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.lua.lib;

import dev.jstech.computers.cannon.run.Values;
import java.util.List;

/**
 * ComputerCraft's {@code term}, {@code read}, {@code printError} and {@code paintutils}, on the
 * program's screen.
 *
 * <p>{@code read} is ComputerCraft's line editor: it waits for key and character events, draws what
 * has been typed where the cursor was when it began, and scrolls it sideways when it runs past the
 * edge. It waits through {@code os.pullEvent}, so inside {@code parallel} it hears the events the way
 * any other routine does.
 */
final class LuaTerm {

    private static final String BUFFER = "Buffer";
    private static final String CURSOR = "Cursor";
    private static final String START_X = "StartX";
    private static final String START_Y = "StartY";
    private static final String REPLACE = "Replace";
    private static final String HISTORY = "History";
    private static final String HISTORY_AT = "HistoryAt";

    private static final long KEY_ENTER = 257;
    private static final long KEY_BACKSPACE = 259;
    private static final long KEY_DELETE = 261;
    private static final long KEY_RIGHT = 262;
    private static final long KEY_LEFT = 263;
    private static final long KEY_DOWN = 264;
    private static final long KEY_UP = 265;
    private static final long KEY_HOME = 268;
    private static final long KEY_END = 269;
    private static final long KEY_PAD_ENTER = 335;

    private LuaTerm() {
    }

    static void register() {
        LuaLib.define("term.write", (context, target, arguments, line) -> {
            context.terminal().write(LuaValues.plainString(arguments.length > 0 ? arguments[0] : null));
            return null;
        });
        LuaLib.define("term.blit", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "blit", arguments, line);
            final String text = args.text(0);
            final String fg = args.text(1);
            final String bg = args.text(2);
            if (fg.length() != text.length() || bg.length() != text.length()) {
                throw context.error("Arguments must be the same length", line);
            }
            context.terminal().blit(text, fg, bg);
            return null;
        });
        LuaLib.define("term.clear", (context, target, arguments, line) -> {
            context.terminal().clear();
            return null;
        });
        LuaLib.define("term.clearLine", (context, target, arguments, line) -> {
            context.terminal().clearLine();
            return null;
        });
        LuaLib.define("term.getCursorPos", (context, target, arguments, line) -> context.values(
                List.of((long) context.terminal().cursorX(), (long) context.terminal().cursorY()), line));
        LuaLib.define("term.setCursorPos", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "setCursorPos", arguments, line);
            context.terminal().setCursor((int) args.integer(0), (int) args.integer(1));
            return null;
        });
        LuaLib.define("term.getCursorBlink", (context, target, arguments, line) -> context.terminal().blink());
        LuaLib.define("term.setCursorBlink", (context, target, arguments, line) -> {
            context.terminal().setBlink(LuaValues.truth(arguments.length > 0 ? arguments[0] : null));
            return null;
        });
        LuaLib.define("term.getSize", (context, target, arguments, line) ->
                context.values(List.of((long) LuaTerminal.WIDTH, (long) LuaTerminal.HEIGHT), line));
        LuaLib.define("term.scroll", (context, target, arguments, line) -> {
            context.terminal().scroll((int) new LuaArgs(context, "scroll", arguments, line).integer(0));
            return null;
        });
        for (final String spelling : List.of("Colour", "Color")) {
            LuaLib.define("term.is" + spelling, (context, target, arguments, line) -> true);
            LuaLib.define("term.setText" + spelling, (context, target, arguments, line) -> {
                context.terminal().setTextColour(colour(context, arguments, "setText" + spelling, line));
                return null;
            });
            LuaLib.define("term.getText" + spelling, (context, target, arguments, line) ->
                    LuaColours.bitOf(context.terminal().textColour()));
            LuaLib.define("term.setBackground" + spelling, (context, target, arguments, line) -> {
                context.terminal().setGroundColour(colour(context, arguments, "setBackground" + spelling, line));
                return null;
            });
            LuaLib.define("term.getBackground" + spelling, (context, target, arguments, line) ->
                    LuaColours.bitOf(context.terminal().groundColour()));
            LuaLib.define("term.setPalette" + spelling, (context, target, arguments, line) -> {
                final LuaArgs args = new LuaArgs(context, "setPalette" + spelling, arguments, line);
                final int index = colour(context, arguments, "setPalette" + spelling, line);
                final int rgb = arguments.length == 2 ? (int) args.integer(1)
                        : (int) ((channel(args.real(1)) << 16) | (channel(args.real(2)) << 8) | channel(args.real(3)));
                context.terminal().setPaletteColour(index, rgb);
                return null;
            });
            LuaLib.define("term.getPalette" + spelling, (context, target, arguments, line) -> {
                final int rgb = context.terminal().paletteColour(colour(context, arguments, "getPalette" + spelling,
                        line));
                return rgbValues(context, rgb, line);
            });
            LuaLib.define("term.nativePalette" + spelling, (context, target, arguments, line) -> rgbValues(context,
                    LuaTerminal.DEFAULT_PALETTE[colour(context, arguments, "nativePalette" + spelling, line)], line));
        }
        LuaLib.define("term.getLine", (context, target, arguments, line) -> {
            final int y = (int) new LuaArgs(context, "getLine", arguments, line).integer(0) - 1;
            if (y < 0 || y >= LuaTerminal.HEIGHT) {
                throw context.error("Line is out of range.", line);
            }
            final LuaTerminal terminal = context.terminal();
            return context.values(List.of(context.text(terminal.row(y), line), context.text(terminal.rowText(y), line),
                    context.text(terminal.rowGround(y), line)), line);
        });
        // There is one screen, so the one being drawn on, the machine's own and any it is sent to are one table.
        for (final String name : List.of("term.current", "term.native", "term.redirect")) {
            LuaLib.define(name, (context, target, arguments, line) -> context.global("term"));
        }
        LuaLib.define("printError", (context, target, arguments, line) -> {
            final LuaTerminal terminal = context.terminal();
            final int before = terminal.textColour();
            terminal.setTextColour(14);
            final StringBuilder out = new StringBuilder();
            for (int i = 0; i < arguments.length; i++) {
                out.append(i > 0 ? "\t" : "").append(LuaValues.plainString(arguments[i]));
            }
            context.print(out.toString());
            terminal.setTextColour(before);
            return null;
        });
        LuaLib.define("read", LuaTerm::read);
        LuaLib.define("io.read", LuaTerm::read);
        LuaLib.continueWith("read.event", LuaTerm::readEvent);
        registerPaint();
    }

    private static int colour(final ILuaContext context, final Object[] arguments, final String name, final int line) {
        final long bit = new LuaArgs(context, name, arguments, line).integer(0);
        final int index = LuaColours.indexOf(bit);
        if (index < 0 || Long.bitCount(bit) != 1) {
            throw context.error("bad argument #1 to '" + name + "' (invalid color)", line);
        }
        return index;
    }

    private static long channel(final double value) {
        return Math.round(Math.max(0, Math.min(1, value)) * 255);
    }

    private static Object rgbValues(final ILuaContext context, final int rgb, final int line) {
        return context.values(List.of(((rgb >> 16) & 0xFF) / 255.0, ((rgb >> 8) & 0xFF) / 255.0, (rgb & 0xFF) / 255.0),
                line);
    }

    // read

    private static Object read(final ILuaContext context, final Object target, final Object[] arguments,
                               final int line) {
        final LuaTerminal terminal = context.terminal();
        final Values.Obj state = context.state("read.event", null, line);
        final Object replace = arguments.length > 0 ? arguments[0] : null;
        final String given = arguments.length > 3 && arguments[3] instanceof String text ? text : "";
        state.set(BUFFER, context.text(given, line));
        state.set(CURSOR, (long) given.length());
        state.set(START_X, (long) terminal.cursorX());
        state.set(START_Y, (long) terminal.cursorY());
        state.set(REPLACE, replace instanceof String text && !text.isEmpty() ? context.text(text.substring(0, 1), line)
                : null);
        if (arguments.length > 1 && arguments[1] instanceof Values.Table history) {
            state.set(HISTORY, history);
            state.set(HISTORY_AT, history.length() + 1);
        }
        terminal.setBlink(true);
        context.reading(true);
        draw(context, state);
        return next(context, state, line);
    }

    private static LuaCall next(final ILuaContext context, final Values.Obj state, final int line) {
        return new LuaCall(context.global("os") instanceof Values.Table os ? os.get("pullEvent") : null,
                new Object[0], state);
    }

    private static Object readEvent(final ILuaContext context, final Values.Obj state, final Object result,
                                    final int line) {
        final List<Object> event = context.expand(result);
        final String name = event.isEmpty() ? "" : LuaValues.plainString(event.getFirst());
        String buffer = String.valueOf(state.get(BUFFER));
        int cursor = (int) (long) (Long) state.get(CURSOR);
        final Object carried = event.size() > 1 ? event.get(1) : null;
        switch (name) {
            case "char", "paste" -> {
                final String typed = LuaValues.plainString(carried);
                buffer = buffer.substring(0, cursor) + typed + buffer.substring(cursor);
                cursor += typed.length();
            }
            case "key" -> {
                final long key = carried instanceof Long code ? code : -1;
                if (key == KEY_ENTER || key == KEY_PAD_ENTER) {
                    final LuaTerminal terminal = context.terminal();
                    terminal.setBlink(false);
                    terminal.print("\n");
                    context.reading(false);
                    return context.text(buffer, line);
                } else if (key == KEY_BACKSPACE && cursor > 0) {
                    buffer = buffer.substring(0, cursor - 1) + buffer.substring(cursor);
                    cursor--;
                } else if (key == KEY_DELETE && cursor < buffer.length()) {
                    buffer = buffer.substring(0, cursor) + buffer.substring(cursor + 1);
                } else if (key == KEY_LEFT && cursor > 0) {
                    cursor--;
                } else if (key == KEY_RIGHT && cursor < buffer.length()) {
                    cursor++;
                } else if (key == KEY_HOME) {
                    cursor = 0;
                } else if (key == KEY_END) {
                    cursor = buffer.length();
                } else if ((key == KEY_UP || key == KEY_DOWN) && state.get(HISTORY) instanceof Values.Table history) {
                    long at = (Long) state.get(HISTORY_AT) + (key == KEY_UP ? -1 : 1);
                    at = Math.max(1, Math.min(history.length() + 1, at));
                    state.set(HISTORY_AT, at);
                    buffer = at > history.length() ? "" : LuaValues.plainString(history.get(at));
                    cursor = buffer.length();
                }
            }
            default -> {
                return next(context, state, line);
            }
        }
        state.set(BUFFER, context.text(buffer, line));
        state.set(CURSOR, (long) cursor);
        draw(context, state);
        return next(context, state, line);
    }

    /* The line as typed so far, scrolled sideways to keep the cursor on the screen. */
    private static void draw(final ILuaContext context, final Values.Obj state) {
        final LuaTerminal terminal = context.terminal();
        final String buffer = String.valueOf(state.get(BUFFER));
        final int cursor = (int) (long) (Long) state.get(CURSOR);
        final int startX = (int) (long) (Long) state.get(START_X);
        final int startY = (int) (long) (Long) state.get(START_Y);
        final int room = Math.max(1, LuaTerminal.WIDTH - startX + 1);
        final int scroll = Math.max(0, cursor - room + 1);
        String shown = buffer.substring(Math.min(scroll, buffer.length()));
        if (state.get(REPLACE) instanceof String mask) {
            shown = mask.repeat(shown.length());
        }
        terminal.setCursor(startX, startY);
        terminal.write(shown + " ".repeat(Math.max(0, room - shown.length())));
        terminal.setCursor(startX + cursor - scroll, startY);
    }

    // paintutils

    private static void registerPaint() {
        LuaLib.define("paintutils.drawPixel", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "drawPixel", arguments, line);
            pixel(context, (int) args.integer(0), (int) args.integer(1), arguments.length > 2 ? args.at(2) : null, line);
            return null;
        });
        LuaLib.define("paintutils.drawLine", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "drawLine", arguments, line);
            int x0 = (int) args.integer(0);
            int y0 = (int) args.integer(1);
            final int x1 = (int) args.integer(2);
            final int y1 = (int) args.integer(3);
            final Object colour = arguments.length > 4 ? args.at(4) : null;
            final int dx = Math.abs(x1 - x0);
            final int dy = -Math.abs(y1 - y0);
            final int sx = x0 < x1 ? 1 : -1;
            final int sy = y0 < y1 ? 1 : -1;
            int error = dx + dy;
            while (true) {
                pixel(context, x0, y0, colour, line);
                if (x0 == x1 && y0 == y1) {
                    break;
                }
                final int twice = 2 * error;
                if (twice >= dy) {
                    error += dy;
                    x0 += sx;
                }
                if (twice <= dx) {
                    error += dx;
                    y0 += sy;
                }
            }
            return null;
        });
        LuaLib.define("paintutils.drawBox", (context, target, arguments, line) -> box(context, arguments, false, line));
        LuaLib.define("paintutils.drawFilledBox", (context, target, arguments, line) ->
                box(context, arguments, true, line));
        LuaLib.define("paintutils.parseImage", (context, target, arguments, line) ->
                parseImage(context, new LuaArgs(context, "parseImage", arguments, line).text(0), line));
        LuaLib.define("paintutils.loadImage", (context, target, arguments, line) -> {
            final String path = LuaFs.resolve(context, new LuaArgs(context, "loadImage", arguments, line).text(0));
            final String text = context.files().read(path);
            return text == null ? null : parseImage(context, text, line);
        });
        LuaLib.define("paintutils.drawImage", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "drawImage", arguments, line);
            final Values.Table image = args.table(0);
            final int x = (int) args.integer(1);
            final int y = (int) args.integer(2);
            for (long row = 1; row <= image.length(); row++) {
                if (image.get(row) instanceof Values.Table cells) {
                    for (long column = 1; column <= cells.length(); column++) {
                        final Object colour = cells.get(column);
                        if (LuaNumbers.toInteger(colour) != null && LuaNumbers.toInteger(colour) > 0) {
                            pixel(context, x + (int) column - 1, y + (int) row - 1, colour, line);
                        }
                    }
                }
            }
            return null;
        });
    }

    private static void pixel(final ILuaContext context, final int x, final int y, final Object colour, final int line) {
        final LuaTerminal terminal = context.terminal();
        final int before = terminal.groundColour();
        if (colour != null) {
            final Long bit = LuaNumbers.toInteger(colour);
            if (bit != null && LuaColours.indexOf(bit) >= 0) {
                terminal.setGroundColour(LuaColours.indexOf(bit));
            }
        }
        terminal.setCursor(x, y);
        terminal.write(" ");
        if (colour != null) {
            terminal.setGroundColour(before);
        }
    }

    private static Object box(final ILuaContext context, final Object[] arguments, final boolean filled,
                              final int line) {
        final LuaArgs args = new LuaArgs(context, filled ? "drawFilledBox" : "drawBox", arguments, line);
        final int x0 = (int) Math.min(args.integer(0), args.integer(2));
        final int x1 = (int) Math.max(args.integer(0), args.integer(2));
        final int y0 = (int) Math.min(args.integer(1), args.integer(3));
        final int y1 = (int) Math.max(args.integer(1), args.integer(3));
        final Object colour = arguments.length > 4 ? args.at(4) : null;
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                if (filled || y == y0 || y == y1 || x == x0 || x == x1) {
                    pixel(context, x, y, colour, line);
                }
            }
        }
        return null;
    }

    private static Values.Table parseImage(final ILuaContext context, final String text, final int line) {
        final Values.Table image = context.table(line);
        final String[] rows = text.split("\n", -1);
        for (int r = 0; r < rows.length; r++) {
            final Values.Table cells = context.table(line);
            for (int c = 0; c < rows[r].length(); c++) {
                final int index = LuaTerminal.digit(rows[r].charAt(c));
                cells.put((long) (c + 1), index < 0 ? 0L : LuaColours.bitOf(index));
            }
            context.resized(cells, line);
            image.put((long) (r + 1), cells);
        }
        context.resized(image, line);
        return image;
    }
}
