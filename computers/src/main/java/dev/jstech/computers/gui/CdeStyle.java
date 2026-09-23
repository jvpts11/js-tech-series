/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui;

import dev.jstech.computers.os.WorkspaceSet;
import java.util.ArrayList;
import java.util.List;

/**
 * What a workstation keeps of CDE's look: the palette the whole desktop is drawn from, and the backdrop each of
 * the four workspaces wears, so a player sees at a glance which one is up.
 *
 * <p>It is kept as one short line of text, {@code Desert;hatch,pinstripe,tiles,weave}, and read back forgivingly:
 * a palette nobody has is the default one, and a backdrop that cannot be read is the one that workspace starts
 * with. Nothing a machine was left with can stop its desktop from being drawn.
 *
 * @param palette   the scheme by the name the Style Manager lists it under
 * @param backdrops one backdrop for each workspace, the first workspace first
 */
public record CdeStyle(String palette, List<CdeBackdrop> backdrops) {

    /** The backdrop each workspace starts with: a different one on each, so they are told apart from the first. */
    private static final List<CdeBackdrop> STARTING =
            List.of(CdeBackdrop.HATCH, CdeBackdrop.PINSTRIPE, CdeBackdrop.TILES, CdeBackdrop.WEAVE);

    /** What a workstation starts with: the default palette, and the starting backdrops. */
    public static final CdeStyle DEFAULT = new CdeStyle(CdeScheme.DEFAULT.label(), STARTING);

    /** The longest a kept style can be written: the longest palette name and four of the longest backdrops. */
    public static final int MOST_LETTERS = 64;

    private static final String PARTS = ";";
    private static final String BACKDROPS = ",";

    public CdeStyle {
        palette = CdeScheme.named(palette == null ? "" : palette).label();
        final List<CdeBackdrop> each = new ArrayList<>(WorkspaceSet.COUNT);
        for (int i = 0; i < WorkspaceSet.COUNT; i++) {
            final CdeBackdrop given = backdrops != null && i < backdrops.size() ? backdrops.get(i) : null;
            each.add(given != null ? given : STARTING.get(i));
        }
        backdrops = List.copyOf(each);
    }

    /** A style read back from how it was kept; anything unreadable in it is what a workstation starts with. */
    public static CdeStyle parse(final String kept) {
        if (kept == null || kept.isBlank()) {
            return DEFAULT;
        }
        final String[] parts = kept.split(PARTS, 2);
        final List<CdeBackdrop> backdrops = new ArrayList<>(WorkspaceSet.COUNT);
        if (parts.length > 1) {
            for (final String key : parts[1].split(BACKDROPS, -1)) {
                backdrops.add(CdeBackdrop.keyed(key.trim()));
            }
        }
        return new CdeStyle(parts[0].trim(), backdrops);
    }

    /** The style as it is kept. */
    public String encoded() {
        final List<String> keys = new ArrayList<>(this.backdrops.size());
        for (final CdeBackdrop backdrop : this.backdrops) {
            keys.add(backdrop.key());
        }
        return this.palette + PARTS + String.join(BACKDROPS, keys);
    }

    /** The scheme the desktop is drawn from. */
    public CdeScheme scheme() {
        return CdeScheme.named(this.palette);
    }

    /** The colours the desktop is drawn in now. */
    public CdePalette colours() {
        return scheme().colours();
    }

    /** The backdrop that workspace wears, counted from nought. */
    public CdeBackdrop backdrop(final int workspace) {
        return this.backdrops.get(WorkspaceSet.clampIndex(workspace));
    }

    /** The same style with the whole desktop drawn from another palette. */
    public CdeStyle withPalette(final String name) {
        return new CdeStyle(name, this.backdrops);
    }

    /** The same style with that workspace wearing another backdrop, and the others as they were. */
    public CdeStyle withBackdrop(final int workspace, final CdeBackdrop backdrop) {
        final List<CdeBackdrop> changed = new ArrayList<>(this.backdrops);
        changed.set(WorkspaceSet.clampIndex(workspace), backdrop);
        return new CdeStyle(this.palette, changed);
    }
}
