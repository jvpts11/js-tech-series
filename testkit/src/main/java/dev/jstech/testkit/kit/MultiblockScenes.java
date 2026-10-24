/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core Test Kit.
 */
package dev.jstech.testkit.kit;

import dev.jstech.core.multiblock.IMatchResult;
import dev.jstech.core.multiblock.IStructureTerminal;
import dev.jstech.core.multiblock.MultiblockPattern;
import dev.jstech.core.multiblock.MultiblockPatterns;
import dev.jstech.testkit.gametest.Expect;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;

/**
 * A scene for multiblocks: a pattern built in the arena, unturned, each of its letters the block a test names, the
 * terminal set last as a player would set it; the pattern matched against what stands there, a failure saying at
 * which of its places it wanted what and found what; and a terminal waited on until it says its structure formed.
 *
 * <pre>{@code
 * MultiblockScenes.build(helper, MyPatterns.OVEN, OVEN, Map.of('#', MyContent.OVEN.get(), 'B', Blocks.BRICKS));
 * MultiblockScenes.expectForms(helper, MyPatterns.OVEN, OVEN);
 * }</pre>
 */
public final class MultiblockScenes {

    private MultiblockScenes() {
    }

    /**
     * Builds {@code pattern} with its terminal at the arena's place {@code terminal}, unturned: every place of
     * a letter {@code blocks} names set to its block, a free place left as it is, the terminal's place last.
     *
     * @throws IllegalArgumentException when the pattern has a letter {@code blocks} does not name
     */
    public static void build(final GameTestHelper helper, final MultiblockPattern pattern, final BlockPos terminal,
                             final Map<Character, Block> blocks) {
        final BlockPos origin = terminal.offset(-pattern.terminalX(), -pattern.terminalY(),
                -pattern.terminalZ());
        for (int y = 0; y < pattern.sizeY(); y++) {
            for (int z = 0; z < pattern.sizeZ(); z++) {
                for (int x = 0; x < pattern.sizeX(); x++) {
                    final char letter = pattern.charAt(x, y, z);
                    if (letter == MultiblockPattern.IGNORE_CHAR || letter == MultiblockPattern.TERMINAL_CHAR) {
                        continue;
                    }
                    helper.setBlock(origin.offset(x, y, z), blockOf(blocks, letter, pattern));
                }
            }
        }
        helper.setBlock(terminal, blockOf(blocks, MultiblockPattern.TERMINAL_CHAR, pattern));
    }

    /** {@code pattern} matched against the world with its terminal at {@code terminal}, in any turn. */
    public static IMatchResult match(final GameTestHelper helper, final MultiblockPattern pattern,
                                     final BlockPos terminal) {
        return MultiblockPatterns.match(pattern, helper.getLevel(), helper.absolutePos(terminal));
    }

    /**
     * That {@code pattern} stands with its terminal at {@code terminal}; else a failure saying at which of its
     * places it wanted which letter and found which block.
     */
    public static IMatchResult.Success expectForms(final GameTestHelper helper, final MultiblockPattern pattern,
                                                  final BlockPos terminal) {
        final IMatchResult result = match(helper, pattern, terminal);
        if (result instanceof IMatchResult.Failure failure) {
            throw Expect.failure(helper, terminal, "the pattern " + pattern.name() + " does not stand: at "
                    + failure.relX() + ", " + failure.relY() + ", " + failure.relZ() + " from its terminal it wants '"
                    + failure.expectedChar() + "' and finds " + failure.actualBlockId());
        }
        return (IMatchResult.Success) result;
    }

    /** That the terminal at {@code terminal} says its structure is formed; else a failure saying it is not. */
    public static void expectFormed(final GameTestHelper helper, final BlockPos terminal) {
        final IStructureTerminal structure = Expect.instance(helper, terminal, "a multiblock's terminal",
                IStructureTerminal.class, helper.getLevel().getBlockEntity(helper.absolutePos(terminal)));
        Expect.that(helper, terminal, structure.formedOrientation().isPresent(),
                "the structure of the terminal is formed", "it is not");
    }

    private static Block blockOf(final Map<Character, Block> blocks, final char letter,
                                 final MultiblockPattern pattern) {
        final Block block = blocks.get(letter);
        if (block == null) {
            throw new IllegalArgumentException("the pattern " + pattern.name() + " has the letter '" + letter
                    + "', and no block was named for it");
        }
        return block;
    }
}
