/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.lua.lib;

/** The bit32 library: every operation on unsigned numbers of thirty-two bits. */
final class LuaBit32 {

    private static final long MASK = 0xFFFFFFFFL;

    private LuaBit32() {
    }

    static void register() {
        LuaLib.define("bit32.band", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "band", arguments, line);
            long result = MASK;
            for (int i = 0; i < arguments.length; i++) {
                result &= args.integer(i);
            }
            return result & MASK;
        });
        LuaLib.define("bit32.bor", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "bor", arguments, line);
            long result = 0;
            for (int i = 0; i < arguments.length; i++) {
                result |= args.integer(i);
            }
            return result & MASK;
        });
        LuaLib.define("bit32.bxor", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "bxor", arguments, line);
            long result = 0;
            for (int i = 0; i < arguments.length; i++) {
                result ^= args.integer(i);
            }
            return result & MASK;
        });
        LuaLib.define("bit32.bnot", (context, target, arguments, line) ->
                ~new LuaArgs(context, "bnot", arguments, line).integer(0) & MASK);
        LuaLib.define("bit32.btest", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "btest", arguments, line);
            long result = MASK;
            for (int i = 0; i < arguments.length; i++) {
                result &= args.integer(i);
            }
            return (result & MASK) != 0;
        });
        LuaLib.define("bit32.lshift", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "lshift", arguments, line);
            return shift(args.integer(0) & MASK, args.integer(1));
        });
        LuaLib.define("bit32.rshift", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "rshift", arguments, line);
            return shift(args.integer(0) & MASK, -args.integer(1));
        });
        LuaLib.define("bit32.arshift", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "arshift", arguments, line);
            final long value = args.integer(0) & MASK;
            final long by = args.integer(1);
            if (by < 0 || (value & 0x80000000L) == 0) {
                return shift(value, -by);
            }
            if (by >= 32) {
                return MASK;
            }
            return ((value >> by) | ~(MASK >> by)) & MASK;
        });
        LuaLib.define("bit32.lrotate", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "lrotate", arguments, line);
            return rotate(args.integer(0) & MASK, args.integer(1));
        });
        LuaLib.define("bit32.rrotate", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "rrotate", arguments, line);
            return rotate(args.integer(0) & MASK, -args.integer(1));
        });
        LuaLib.define("bit32.extract", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "extract", arguments, line);
            final long value = args.integer(0) & MASK;
            final long field = args.integer(1);
            final long width = args.integer(2, 1L);
            if (field < 0 || width < 1 || field + width > 32) {
                throw args.bad(1, "trying to access non-existent bits");
            }
            return (value >> field) & ((1L << width) - 1);
        });
        LuaLib.define("bit32.replace", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "replace", arguments, line);
            final long value = args.integer(0) & MASK;
            final long replacement = args.integer(1);
            final long field = args.integer(2);
            final long width = args.integer(3, 1L);
            if (field < 0 || width < 1 || field + width > 32) {
                throw args.bad(2, "trying to access non-existent bits");
            }
            final long mask = ((1L << width) - 1) << field;
            return ((value & ~mask) | ((replacement << field) & mask)) & MASK;
        });
    }

    private static long shift(final long value, final long by) {
        if (by <= -32 || by >= 32) {
            return 0;
        }
        return (by >= 0 ? value << by : value >>> -by) & MASK;
    }

    private static long rotate(final long value, final long by) {
        final int turn = (int) Math.floorMod(by, 32L);
        if (turn == 0) {
            return value;
        }
        return ((value << turn) | (value >>> (32 - turn))) & MASK;
    }
}
