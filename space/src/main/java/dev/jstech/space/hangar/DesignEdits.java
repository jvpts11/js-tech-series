/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.hangar;

import dev.jstech.space.rocket.RocketStack;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * What a player does to a design on a Rocket Hangar's screen, in the way of Kerbal Space Program's editor, on a rocket
 * stacked from the bottom up. Its nodes are the places a part goes: under the bottom part, between two parts, and on
 * the top one. A part picked up takes with it the parts on its far side from the root, the part the rest hangs from,
 * as a branch comes off a tree; the root picked up takes the whole rocket. A part turns a quarter turn at a time; a
 * part built in layers is built taller or shorter; boosters hang at a part's side in a ring of as many as the mirror
 * says, and stack segment on segment.
 */
public final class DesignEdits {

    /** The most layers a part built in layers takes on the screen. */
    public static final int MOST_LAYERS = 64;
    /** The most boosters the mirror hangs at once round a part. */
    public static final int MOST_MIRROR = 4;

    private DesignEdits() {
    }

    /** How many nodes {@code stack} has: one under each part and one on its top. */
    public static int nodes(final RocketStack stack) {
        return stack.sections().size() + 1;
    }

    /** {@code stack} with {@code run}, parts as they stood, set at the node {@code node}. */
    public static RocketStack insert(final RocketStack stack, final int node, final List<RocketStack.Section> run) {
        final List<RocketStack.Section> sections = new ArrayList<>(stack.sections());
        sections.addAll(Math.clamp(node, 0, sections.size()), run);
        return new RocketStack(sections);
    }

    /** {@code stack} with {@code part}, not turned and a layer tall, set at the node {@code node}. */
    public static RocketStack insert(final RocketStack stack, final int node, final RocketStack.Part part) {
        return insert(stack, node, List.of(new RocketStack.Section(part, List.of())));
    }

    /**
     * The parts the part at {@code place} takes with it when picked up, with {@code root} the root: itself and those
     * above it when it stands over the root, itself and those under it when it stands under, the whole rocket when it
     * is the root.
     */
    public static Picked pick(final RocketStack stack, final int place, final int root) {
        final List<RocketStack.Section> sections = stack.sections();
        if (place < 0 || place >= sections.size()) {
            return new Picked(stack, List.of(), root);
        }
        if (place == root || sections.size() == 1) {
            return new Picked(new RocketStack(List.of()), sections, 0);
        }
        if (place > root) {
            return new Picked(new RocketStack(sections.subList(0, place)), sections.subList(place, sections.size()),
                    root);
        }
        return new Picked(new RocketStack(sections.subList(place + 1, sections.size())), sections.subList(0,
                place + 1), root - place - 1);
    }

    /** {@code stack} with the part at {@code place} turned a quarter turn more, clockwise seen from above. */
    public static RocketStack turn(final RocketStack stack, final int place) {
        return replace(stack, place, section -> new RocketStack.Section(new RocketStack.Part(section.part().id(),
                section.part().layers(), section.part().turns() + 1), section.boosters()));
    }

    /** {@code stack} with the part at {@code place} built {@code layers} tall, 1 to {@link #MOST_LAYERS}. */
    public static RocketStack stretch(final RocketStack stack, final int place, final int layers) {
        final int tall = Math.clamp(layers, 1, MOST_LAYERS);
        return replace(stack, place, section -> new RocketStack.Section(new RocketStack.Part(section.part().id(),
                tall, section.part().turns()), section.boosters()));
    }

    /**
     * {@code stack} with {@code count} boosters of {@code segment} hung round the part at {@code place}, in place of
     * those it had; or, where it has boosters already, {@code segment} stacked on each of them.
     */
    public static RocketStack hang(final RocketStack stack, final int place, final RocketStack.Part segment,
                                   final int count) {
        return replace(stack, place, section -> {
            final List<List<RocketStack.Part>> boosters = new ArrayList<>();
            if (section.boosters().isEmpty()) {
                boosters.addAll(Collections.nCopies(Math.clamp(count, 1, RocketDesign.MOST_BOOSTERS),
                        List.of(segment)));
            } else {
                for (final List<RocketStack.Part> booster : section.boosters()) {
                    final List<RocketStack.Part> taller = new ArrayList<>(booster);
                    if (taller.size() < RocketDesign.MOST_SEGMENTS) {
                        taller.add(segment);
                    }
                    boosters.add(taller);
                }
            }
            return new RocketStack.Section(section.part(), boosters);
        });
    }

    /** {@code stack} with the boosters round the part at {@code place} taken off. */
    public static RocketStack unhang(final RocketStack stack, final int place) {
        return replace(stack, place, section -> new RocketStack.Section(section.part(), List.of()));
    }

    /* The part at {@code place} of {@code stack} changed by {@code change}; the stack as it was for no such place. */
    private static RocketStack replace(final RocketStack stack, final int place,
                                       final UnaryOperator<RocketStack.Section> change) {
        if (place < 0 || place >= stack.sections().size()) {
            return stack;
        }
        final List<RocketStack.Section> sections = new ArrayList<>(stack.sections());
        sections.set(place, change.apply(sections.get(place)));
        return new RocketStack(sections);
    }

    /**
     * What a pick leaves and takes.
     *
     * @param left  the rocket left on the pad
     * @param taken the parts taken, from the bottom up
     * @param root  the root's place in what is left
     */
    public record Picked(RocketStack left, List<RocketStack.Section> taken, int root) {

        public Picked {
            taken = List.copyOf(taken);
        }
    }
}
