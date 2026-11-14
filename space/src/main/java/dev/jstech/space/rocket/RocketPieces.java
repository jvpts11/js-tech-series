/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.rocket;

import dev.jstech.space.api.RocketPart;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * A rocket's parts as its plan numbers them, each a multiblock of its own: the parts of its core first, from the
 * bottom up, by their places in its stack, then each segment of each booster, the boosters of each place in turn and
 * each booster's segments from its foot up. A booster stands touching the part it hangs at, its foot level with that
 * part's: the first to the east, and round from it a quarter turn at a time, so two stand east and west and four on
 * every side, each on whole blocks, as both diameters are odd.
 */
public final class RocketPieces {

    /* The sides a booster stands on round its part, a quarter turn apart from the east, as x and z steps. */
    private static final int[][] SIDES = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};

    private RocketPieces() {
    }

    /** How many parts {@code stack} is built of: its core's and every booster segment. */
    public static int count(final RocketStack stack) {
        int count = stack.sections().size();
        for (final RocketStack.Section section : stack.sections()) {
            for (final List<RocketStack.Part> booster : section.boosters()) {
                count += booster.size();
            }
        }
        return count;
    }

    /** The numbers of the parts of {@code stack} that are booster segments, in their order. */
    public static List<Integer> boosterNumbers(final RocketStack stack) {
        final List<Integer> out = new ArrayList<>();
        for (int number = stack.sections().size(); number < count(stack); number++) {
            out.add(number);
        }
        return List.copyOf(out);
    }

    /**
     * The parts of {@code stack}, numbered as its plan numbers them; a booster's place round a part whose part, or
     * whose foot, is unknown is taken as a part of three blocks across.
     */
    public static List<Piece> of(final Map<ResourceLocation, RocketPart> parts, final RocketStack stack) {
        final List<Piece> out = new ArrayList<>();
        final List<RocketStack.Section> sections = stack.sections();
        for (int k = 0; k < sections.size(); k++) {
            out.add(new Piece(k, k, -1, sections.get(k).part(), k > 0 ? sections.get(k - 1).part() : null, 0, 0));
        }
        for (int k = 0; k < sections.size(); k++) {
            final RocketStack.Section section = sections.get(k);
            final int core = diameter(parts, section.part());
            for (int b = 0; b < section.boosters().size(); b++) {
                final List<RocketStack.Part> booster = section.boosters().get(b);
                final int[] at = booster.isEmpty() ? new int[] {0, 0}
                        : offset(b, section.boosters().size(), core, diameter(parts, booster.get(0)));
                for (int s = 0; s < booster.size(); s++) {
                    out.add(new Piece(out.size(), k, b, booster.get(s), s > 0 ? booster.get(s - 1) : null, at[0],
                            at[1]));
                }
            }
        }
        return List.copyOf(out);
    }

    /** Where the foot of each booster of {@code stack} stands from the middle of the rocket's foot, in blocks. */
    public static List<Vec3> boosterFeet(final Map<ResourceLocation, RocketPart> parts, final RocketStack stack) {
        final List<Vec3> feet = new ArrayList<>();
        for (final Piece piece : of(parts, stack)) {
            if (piece.inBooster() && piece.below() == null) {
                feet.add(new Vec3(piece.x(), 0, piece.z()));
            }
        }
        return List.copyOf(feet);
    }

    /**
     * Where the middle of the booster {@code index} of {@code count} stands from the middle of the part it hangs at,
     * as x and z steps in blocks: touching it, {@code core} blocks across, the booster {@code booster} across.
     */
    public static int[] offset(final int index, final int count, final int core, final int booster) {
        final int[] side = SIDES[Math.floorMod(index * SIDES.length / Math.max(1, count), SIDES.length)];
        final int reach = (core + booster) / 2;
        return new int[] {side[0] * reach, side[1] * reach};
    }

    private static int diameter(final Map<ResourceLocation, RocketPart> parts, final RocketStack.Part placed) {
        final RocketPart part = parts.get(placed.id());
        return part == null ? RocketPart.DIAMETERS.get(0) : part.diameter();
    }

    /**
     * A part of a rocket as its plan numbers it.
     *
     * @param number  its number in the plan
     * @param section the place in the stack of the core's part it is, or that it hangs at
     * @param booster which booster of that place it is a segment of, from nought; -1 for a part of the core
     * @param part    the part, as it was set
     * @param below   the part under it in its own column, the core's or its booster's; none at a column's foot
     * @param x       how far its middle stands east of the rocket's axis, in blocks
     * @param z       how far its middle stands south of it
     */
    public record Piece(int number, int section, int booster, RocketStack.Part part,
                        @Nullable RocketStack.Part below, int x, int z) {

        /** Whether it is a segment of a booster. */
        public boolean inBooster() {
            return this.booster >= 0;
        }
    }
}
