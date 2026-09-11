/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.lua.lib;

import dev.jstech.computers.cannon.run.Values;
import java.util.Locale;

/**
 * The os and io libraries, on the machine's own clock.
 *
 * <p>There is no wall clock in here: time is the world's, in days and ticks of the day, so a program
 * that asks the hour gets the hour the sun says, and one that measures how long it ran gets ticks.
 * Reading a line is the runtime's, since it has to wait for one.
 */
final class LuaOs {

    private static final long TICKS_PER_DAY = 24_000L;
    private static final double TICKS_PER_HOUR = 1_000.0;
    private static final long MILLIS_PER_DAY = 86_400_000L;

    private LuaOs() {
    }

    static void register() {
        LuaLib.define("os.time", (context, target, arguments, line) -> {
            if (arguments.length > 0 && arguments[0] instanceof Values.Table given) {
                final long hour = LuaNumbers.toInteger(given.get("hour")) == null ? 12
                        : LuaNumbers.toInteger(given.get("hour"));
                final long minute = LuaNumbers.toInteger(given.get("min")) == null ? 0
                        : LuaNumbers.toInteger(given.get("min"));
                final long day = LuaNumbers.toInteger(given.get("day")) == null ? 1
                        : LuaNumbers.toInteger(given.get("day"));
                return (day - 1) * 24 + hour + minute / 60.0;
            }
            // The hour of the day, with the world's dawn at six, as ComputerCraft counts it.
            return hourOfDay(context.dayTime());
        });
        LuaLib.define("os.clock", (context, target, arguments, line) -> context.tick() / 20.0);
        LuaLib.define("os.day", (context, target, arguments, line) -> context.day());
        LuaLib.define("os.epoch", (context, target, arguments, line) ->
                context.day() * MILLIS_PER_DAY + (long) (hourOfDay(context.dayTime()) * 3_600_000.0));
        LuaLib.define("os.date", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "date", arguments, line);
            final String format = args.text(0, "%c");
            final double hours = args.has(1) ? args.real(1) : context.day() * 24 + hourOfDay(context.dayTime());
            final long day = (long) Math.floor(hours / 24);
            final double ofDay = hours - day * 24;
            final long hour = (long) Math.floor(ofDay);
            final long minute = (long) Math.floor((ofDay - hour) * 60);
            final long second = (long) Math.floor(((ofDay - hour) * 60 - minute) * 60);
            if (format.startsWith("*t") || format.startsWith("!*t")) {
                final Values.Table made = context.table(line);
                made.put(context.text("year", line), 1L + day / 365);
                made.put(context.text("month", line), 1L + (day % 365) / 31);
                made.put(context.text("day", line), 1L + (day % 365) % 31);
                made.put(context.text("hour", line), hour);
                made.put(context.text("min", line), minute);
                made.put(context.text("sec", line), second);
                made.put(context.text("wday", line), 1L + day % 7);
                made.put(context.text("yday", line), 1L + day % 365);
                made.put(context.text("isdst", line), false);
                context.resized(made, line);
                return made;
            }
            return context.text(strftime(format, day, hour, minute, second), line);
        });
        LuaLib.define("os.getenv", (context, target, arguments, line) -> null);
        LuaLib.define("io.write", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "write", arguments, line);
            final StringBuilder out = new StringBuilder();
            for (int i = 0; i < arguments.length; i++) {
                out.append(args.text(i));
            }
            context.write(out.toString());
            return null;
        });
        LuaLib.define("write", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "write", arguments, line);
            final String text = args.text(0);
            context.write(text);
            return (long) text.split("\n", -1).length - 1;
        });
    }

    private static double hourOfDay(final long dayTime) {
        return ((dayTime + 6_000L) % TICKS_PER_DAY) / TICKS_PER_HOUR;
    }

    private static String strftime(final String format, final long day, final long hour, final long minute,
                                   final long second) {
        final StringBuilder out = new StringBuilder();
        for (int i = 0; i < format.length(); i++) {
            final char c = format.charAt(i);
            if (c != '%' || i + 1 >= format.length()) {
                out.append(c);
                continue;
            }
            final char what = format.charAt(++i);
            switch (what) {
                case 'H' -> out.append(String.format(Locale.ROOT, "%02d", hour));
                case 'I' -> out.append(String.format(Locale.ROOT, "%02d", hour % 12 == 0 ? 12 : hour % 12));
                case 'M' -> out.append(String.format(Locale.ROOT, "%02d", minute));
                case 'S' -> out.append(String.format(Locale.ROOT, "%02d", second));
                case 'p' -> out.append(hour < 12 ? "AM" : "PM");
                case 'd' -> out.append(String.format(Locale.ROOT, "%02d", 1 + (day % 365) % 31));
                case 'm' -> out.append(String.format(Locale.ROOT, "%02d", 1 + (day % 365) / 31));
                case 'Y' -> out.append(1 + day / 365);
                case 'y' -> out.append(String.format(Locale.ROOT, "%02d", (1 + day / 365) % 100));
                case 'j' -> out.append(String.format(Locale.ROOT, "%03d", 1 + day % 365));
                case 'A', 'a' -> out.append("Day " + (1 + day % 7));
                case 'X' -> out.append(String.format(Locale.ROOT, "%02d:%02d:%02d", hour, minute, second));
                case 'x' -> out.append(String.format(Locale.ROOT, "%02d/%02d/%02d", 1 + (day % 365) / 31,
                        1 + (day % 365) % 31, (1 + day / 365) % 100));
                case 'c' -> out.append(String.format(Locale.ROOT, "day %d %02d:%02d:%02d", day, hour, minute,
                        second));
                case '%' -> out.append('%');
                default -> out.append('%').append(what);
            }
        }
        return out.toString();
    }
}
