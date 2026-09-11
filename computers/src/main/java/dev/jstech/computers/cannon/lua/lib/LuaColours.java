/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.lua.lib;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ComputerCraft's colours and keys: {@code colors}, {@code colours} (the same, with the British
 * spellings too) and {@code keys}, whose numbers are the ones the keyboard events carry.
 */
final class LuaColours {

    private static final String[] NAMES = {
        "white", "orange", "magenta", "lightBlue", "yellow", "lime", "pink", "gray",
        "lightGray", "cyan", "purple", "blue", "brown", "green", "red", "black"
    };

    /** The keys a program can name, by their number on the keyboard. */
    static final Map<String, Long> KEYS = new LinkedHashMap<>();

    static {
        KEYS.put("space", 32L);
        KEYS.put("apostrophe", 39L);
        KEYS.put("comma", 44L);
        KEYS.put("minus", 45L);
        KEYS.put("period", 46L);
        KEYS.put("slash", 47L);
        final String[] digits = {"zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine"};
        for (int i = 0; i < digits.length; i++) {
            KEYS.put(digits[i], 48L + i);
        }
        KEYS.put("semicolon", 59L);
        KEYS.put("equals", 61L);
        for (char c = 'a'; c <= 'z'; c++) {
            KEYS.put(String.valueOf(c), (long) (c - 'a' + 65));
        }
        KEYS.put("leftBracket", 91L);
        KEYS.put("backslash", 92L);
        KEYS.put("rightBracket", 93L);
        KEYS.put("grave", 96L);
        KEYS.put("enter", 257L);
        KEYS.put("tab", 258L);
        KEYS.put("backspace", 259L);
        KEYS.put("insert", 260L);
        KEYS.put("delete", 261L);
        KEYS.put("right", 262L);
        KEYS.put("left", 263L);
        KEYS.put("down", 264L);
        KEYS.put("up", 265L);
        KEYS.put("pageUp", 266L);
        KEYS.put("pageDown", 267L);
        KEYS.put("home", 268L);
        KEYS.put("end", 269L);
        KEYS.put("capsLock", 280L);
        KEYS.put("scrollLock", 281L);
        KEYS.put("numLock", 282L);
        KEYS.put("printScreen", 283L);
        KEYS.put("pause", 284L);
        for (int i = 1; i <= 25; i++) {
            KEYS.put("f" + i, 289L + i);
        }
        for (int i = 0; i <= 9; i++) {
            KEYS.put("numPad" + i, 320L + i);
        }
        KEYS.put("numPadDecimal", 330L);
        KEYS.put("numPadDivide", 331L);
        KEYS.put("numPadMultiply", 332L);
        KEYS.put("numPadSubtract", 333L);
        KEYS.put("numPadAdd", 334L);
        KEYS.put("numPadEnter", 335L);
        KEYS.put("numPadEqual", 336L);
        KEYS.put("leftShift", 340L);
        KEYS.put("leftCtrl", 341L);
        KEYS.put("leftAlt", 342L);
        KEYS.put("leftSuper", 343L);
        KEYS.put("rightShift", 344L);
        KEYS.put("rightCtrl", 345L);
        KEYS.put("rightAlt", 346L);
        KEYS.put("menu", 348L);
    }

    private LuaColours() {
    }

    /** The bit a colour index is, as programs hold colours. */
    static long bitOf(final int index) {
        return 1L << (index & 15);
    }

    /** The index of the lowest colour bit set, or -1 for none. */
    static int indexOf(final long colour) {
        for (int i = 0; i < 16; i++) {
            if ((colour & (1L << i)) != 0) {
                return i;
            }
        }
        return -1;
    }

    static void register() {
        for (final String library : List.of("colors", "colours")) {
            for (int i = 0; i < NAMES.length; i++) {
                LuaLib.constant(library + "." + NAMES[i], bitOf(i));
            }
            LuaLib.define(library + ".combine", (context, target, arguments, line) -> {
                final LuaArgs args = new LuaArgs(context, "combine", arguments, line);
                long out = 0;
                for (int i = 0; i < arguments.length; i++) {
                    out |= args.integer(i);
                }
                return out;
            });
            LuaLib.define(library + ".subtract", (context, target, arguments, line) -> {
                final LuaArgs args = new LuaArgs(context, "subtract", arguments, line);
                long out = args.integer(0);
                for (int i = 1; i < arguments.length; i++) {
                    out &= ~args.integer(i);
                }
                return out;
            });
            LuaLib.define(library + ".test", (context, target, arguments, line) -> {
                final LuaArgs args = new LuaArgs(context, "test", arguments, line);
                final long wanted = args.integer(1);
                return (args.integer(0) & wanted) == wanted;
            });
            LuaLib.define(library + ".packRGB", (context, target, arguments, line) -> {
                final LuaArgs args = new LuaArgs(context, "packRGB", arguments, line);
                return (channel(args.real(0)) << 16) | (channel(args.real(1)) << 8) | channel(args.real(2));
            });
            LuaLib.define(library + ".unpackRGB", (context, target, arguments, line) -> {
                final long rgb = new LuaArgs(context, "unpackRGB", arguments, line).integer(0);
                return context.values(List.of(((rgb >> 16) & 0xFF) / 255.0, ((rgb >> 8) & 0xFF) / 255.0,
                        (rgb & 0xFF) / 255.0), line);
            });
            LuaLib.define(library + ".toBlit", (context, target, arguments, line) -> {
                final int index = indexOf(new LuaArgs(context, "toBlit", arguments, line).integer(0));
                return context.text(index < 0 ? "0" : Integer.toHexString(index), line);
            });
            LuaLib.define(library + ".fromBlit", (context, target, arguments, line) -> {
                final String hex = new LuaArgs(context, "fromBlit", arguments, line).text(0);
                final int index = hex.length() == 1 ? LuaTerminal.digit(hex.charAt(0)) : -1;
                return index < 0 ? null : (Object) bitOf(index);
            });
        }
        LuaLib.constant("colours.grey", bitOf(7));
        LuaLib.constant("colours.lightGrey", bitOf(8));
        for (final Map.Entry<String, Long> key : KEYS.entrySet()) {
            LuaLib.constant("keys." + key.getKey(), key.getValue());
        }
        LuaLib.define("keys.getName", (context, target, arguments, line) -> {
            final long code = new LuaArgs(context, "getName", arguments, line).integer(0);
            for (final Map.Entry<String, Long> key : KEYS.entrySet()) {
                if (key.getValue() == code) {
                    return context.text(key.getKey(), line);
                }
            }
            return null;
        });
    }

    private static long channel(final double value) {
        return Math.round(Math.max(0, Math.min(1, value)) * 255);
    }
}
