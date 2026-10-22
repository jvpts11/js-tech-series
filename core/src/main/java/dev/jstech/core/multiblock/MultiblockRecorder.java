/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A multiblock's pattern read off one built in the world: the blocks between two corners, each kind of block a letter,
 * the block chosen as the controller at {@code #}, the ports placed on its shell marked as ports of their kind, and air
 * left free. Each letter is the first of its block's name where that is free, {@code I} and {@code O} for a port in
 * and out, so the file reads as the structure does; what the studio's recorder writes ({@link MultiblockPatterns}).
 */
public final class MultiblockRecorder {

    /* The letters a pattern keeps for itself, never given to a block. */
    private static final Set<Character> KEPT = Set.of(MultiblockPattern.CONTROLLER_CHAR,
            MultiblockPattern.IGNORE_CHAR);
    /* The letters a block is given when its own is taken, in turn. */
    private static final String SPARE = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private MultiblockRecorder() {
    }

    /**
     * The pattern {@code name} of the blocks of {@code level} between {@code from} and {@code to}, both kept, the block
     * at {@code controller} its controller, which may turn round, be mirrored when {@code mirrors} and lie on its side
     * when {@code liesDown}.
     *
     * @throws IllegalArgumentException when {@code controller} is outside the corners, or is air
     */
    public static Recording record(final BlockGetter level, final BlockPos from, final BlockPos to,
                                   final BlockPos controller, final String name, final boolean mirrors,
                                   final boolean liesDown) {
        final BlockPos min = new BlockPos(Math.min(from.getX(), to.getX()), Math.min(from.getY(), to.getY()),
                Math.min(from.getZ(), to.getZ()));
        final BlockPos max = new BlockPos(Math.max(from.getX(), to.getX()), Math.max(from.getY(), to.getY()),
                Math.max(from.getZ(), to.getZ()));
        if (controller.getX() < min.getX() || controller.getY() < min.getY() || controller.getZ() < min.getZ()
                || controller.getX() > max.getX() || controller.getY() > max.getY()
                || controller.getZ() > max.getZ()) {
            throw new IllegalArgumentException("the controller " + controller + " is outside the structure");
        }
        final BlockState controlling = level.getBlockState(controller);
        if (controlling.isAir()) {
            throw new IllegalArgumentException("the controller " + controller + " is air");
        }
        final int sx = max.getX() - min.getX() + 1;
        final int sy = max.getY() - min.getY() + 1;
        final int sz = max.getZ() - min.getZ() + 1;
        final Map<Kind, Character> letters = new LinkedHashMap<>();
        final Map<Character, Integer> counts = new LinkedHashMap<>();
        final Set<Character> used = new TreeSet<>();
        final MultiblockPattern.Builder builder = MultiblockPattern.builder(name)
                .controller(BuiltInRegistries.BLOCK.getKey(controlling.getBlock()).toString());
        int blocks = 0;
        for (int y = 0; y < sy; y++) {
            final String[] rows = new String[sz];
            for (int z = 0; z < sz; z++) {
                final StringBuilder row = new StringBuilder(sx);
                for (int x = 0; x < sx; x++) {
                    final BlockPos at = min.offset(x, y, z);
                    final BlockState state = level.getBlockState(at);
                    if (at.equals(controller)) {
                        row.append(MultiblockPattern.CONTROLLER_CHAR);
                        blocks++;
                    } else if (state.isAir()) {
                        row.append(MultiblockPattern.IGNORE_CHAR);
                    } else {
                        final Kind kind = new Kind(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(),
                                level.getBlockEntity(at) instanceof MultiblockPortBlockEntity port
                                        ? Optional.of(port.port()) : Optional.empty());
                        final char letter = letters.computeIfAbsent(kind, each -> letterFor(each, used));
                        counts.merge(letter, 1, Integer::sum);
                        row.append(letter);
                        blocks++;
                    }
                }
                rows[z] = row.toString();
            }
            builder.layer(rows);
        }
        final List<Letter> found = new ArrayList<>();
        letters.forEach((kind, letter) -> {
            builder.where(letter, BlockMatch.blocks(kind.block()));
            kind.port().ifPresent(port -> builder.port(letter, port));
            found.add(new Letter(letter, kind.block(), kind.port(), counts.get(letter)));
        });
        if (mirrors) {
            builder.mirrors();
        }
        if (liesDown) {
            builder.liesDown();
        }
        return new Recording(builder.build(), found, blocks, sx, sy, sz);
    }

    /* The letter of a kind of block: its name's first, or for a port I or O, where free; else the next spare one. */
    private static char letterFor(final Kind kind, final Set<Character> used) {
        final char own = kind.port().map(port -> port.input() ? 'I' : 'O').orElseGet(() -> firstOf(kind.block()));
        final char letter = free(own, used) ? own : spare(used);
        used.add(letter);
        return letter;
    }

    private static char firstOf(final String block) {
        final String path = block.substring(block.indexOf(':') + 1);
        return path.isEmpty() ? 'A' : path.toUpperCase(Locale.ROOT).charAt(0);
    }

    private static boolean free(final char letter, final Set<Character> used) {
        return Character.isLetterOrDigit(letter) && letter < 128 && !KEPT.contains(letter) && !used.contains(letter);
    }

    private static char spare(final Set<Character> used) {
        for (int i = 0; i < SPARE.length(); i++) {
            if (free(SPARE.charAt(i), used)) {
                return SPARE.charAt(i);
            }
        }
        throw new IllegalArgumentException("the structure holds more kinds of block than a pattern has letters");
    }

    /**
     * What was read off the structure.
     *
     * @param pattern the pattern
     * @param letters each letter given to a kind of block, in the order they were met
     * @param blocks  how many blocks it is made of, its controller too
     * @param sizeX   how wide it is, west to east
     * @param sizeY   how tall
     * @param sizeZ   how deep, north to south
     */
    public record Recording(MultiblockPattern pattern, List<Letter> letters, int blocks, int sizeX, int sizeY,
                            int sizeZ) {

        public Recording {
            letters = List.copyOf(letters);
        }
    }

    /**
     * A letter of the pattern and the blocks it stands for.
     *
     * @param letter the letter
     * @param block  the block's id
     * @param port   the kind of port it is, when it is one
     * @param count  how many places hold it
     */
    public record Letter(char letter, String block, Optional<PortKind> port, int count) {
    }

    /* A kind of block a letter is given to: a block, and the port it is when it is one. */
    private record Kind(String block, Optional<PortKind> port) {
    }
}
