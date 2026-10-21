/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.studio;

import dev.jstech.core.text.GameText;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/**
 * The studio's tree: every mod, each mod's kinds of thing and each kind's things, a mod or a kind opened and closed by
 * a click, each row with how many things it holds and the worst of their problems. What is typed in the search shows
 * only the things whose name or id holds it, every mod and kind that holds one open.
 */
final class StudioTree {

    private final Map<String, Map<ThingKind, List<StudioThing>>> byMod = new LinkedHashMap<>();
    private final Map<StudioThing, String> names;
    private final Map<StudioThing, StudioSheet> sheets;
    private final Set<String> openMods = new HashSet<>();
    private final Set<String> openKinds = new HashSet<>();
    private String typed = "";
    private int scroll;
    private @Nullable StudioThing chosen;

    StudioTree(final List<StudioThing> things, final Map<StudioThing, String> names,
               final Map<StudioThing, StudioSheet> sheets) {
        this.names = names;
        this.sheets = sheets;
        for (final StudioThing thing : things) {
            this.byMod.computeIfAbsent(thing.modid(), any -> new LinkedHashMap<>())
                    .computeIfAbsent(thing.kind(), any -> new ArrayList<>()).add(thing);
        }
    }

    /** The rows shown now, from the top of the whole tree. */
    List<Row> rows() {
        final List<Row> rows = new ArrayList<>();
        final boolean searching = !this.typed.isBlank();
        this.byMod.forEach((modid, kinds) -> {
            final List<Row> under = new ArrayList<>();
            int count = 0;
            StudioSheet.Mark worst = StudioSheet.Mark.NONE;
            for (final Map.Entry<ThingKind, List<StudioThing>> kind : kinds.entrySet()) {
                final List<StudioThing> shown = kind.getValue().stream().filter(this::shown).toList();
                if (shown.isEmpty()) {
                    continue;
                }
                final StudioSheet.Mark kindWorst = worst(shown);
                final boolean open = searching || this.openKinds.contains(key(modid, kind.getKey()));
                under.add(new Row(Type.KIND, modid, kind.getKey(), null, GameText.resolve(kind.getKey().title()),
                        shown.size(), kindWorst, 0, open));
                if (open) {
                    for (final StudioThing thing : shown) {
                        final StudioSheet sheet = this.sheets.get(thing);
                        under.add(new Row(Type.THING, modid, kind.getKey(), thing, this.names.get(thing), 0,
                                sheet.worst(), sheet.checks().size(), false));
                    }
                }
                count += shown.size();
                worst = worst.compareTo(kindWorst) >= 0 ? worst : kindWorst;
            }
            if (count == 0) {
                return;
            }
            final boolean open = searching || this.openMods.contains(modid);
            rows.add(new Row(Type.MOD, modid, null, null, title(modid), count, worst, 0, open));
            if (open) {
                rows.addAll(under);
            }
        });
        return rows;
    }

    /** Shows only the things whose name or id holds {@code word}; everything for a blank one. */
    void search(final String word) {
        this.typed = word.trim();
        this.scroll = 0;
    }

    /** The first row shown, as far as the tree is scrolled. */
    int scroll() {
        return this.scroll;
    }

    /** Scrolls by turns of the wheel, a row a turn, held to what {@code visible} rows show. */
    void scroll(final double turns, final int visible) {
        final int most = Math.max(0, this.rows().size() - visible);
        this.scroll = Math.max(0, Math.min(most, this.scroll - (int) Math.signum(turns)));
    }

    /** The thing chosen, if any. */
    Optional<StudioThing> chosen() {
        return Optional.ofNullable(this.chosen);
    }

    /**
     * Chooses {@code thing}, opening its mod and its kind and scrolling so its row shows among the {@code visible}
     * rows.
     */
    void choose(final StudioThing thing, final int visible) {
        this.chosen = thing;
        this.openMods.add(thing.modid());
        this.openKinds.add(key(thing.modid(), thing.kind()));
        final List<Row> rows = this.rows();
        for (int row = 0; row < rows.size(); row++) {
            if (rows.get(row).thing() == thing) {
                if (row < this.scroll) {
                    this.scroll = row;
                } else if (row >= this.scroll + visible) {
                    this.scroll = row - visible + 1;
                }
                return;
            }
        }
    }

    /**
     * Clicks the row {@code index} of {@link #rows()}: a mod or a kind opens or closes, a thing is chosen and given
     * back.
     */
    Optional<StudioThing> click(final int index) {
        final List<Row> rows = this.rows();
        if (index < 0 || index >= rows.size()) {
            return Optional.empty();
        }
        final Row row = rows.get(index);
        switch (row.type()) {
            case MOD -> toggle(this.openMods, row.modid());
            case KIND -> toggle(this.openKinds, key(row.modid(), row.kind()));
            case THING -> {
                this.chosen = row.thing();
                return Optional.ofNullable(row.thing());
            }
        }
        return Optional.empty();
    }

    /** The next thing after the one chosen, round the end, that has a problem; none when none has one. */
    Optional<StudioThing> nextWithProblems() {
        final List<StudioThing> all = new ArrayList<>();
        this.byMod.values().forEach(kinds -> kinds.values().forEach(all::addAll));
        final int from = this.chosen == null ? -1 : all.indexOf(this.chosen);
        for (int step = 1; step <= all.size(); step++) {
            final StudioThing next = all.get((from + step + all.size()) % all.size());
            if (!this.sheets.get(next).checks().isEmpty()) {
                return Optional.of(next);
            }
        }
        return Optional.empty();
    }

    private boolean shown(final StudioThing thing) {
        return this.typed.isBlank() || thing.matches(this.typed, this.names.get(thing));
    }

    private StudioSheet.Mark worst(final List<StudioThing> things) {
        return things.stream().map(thing -> this.sheets.get(thing).worst()).max(StudioSheet.Mark::compareTo)
                .orElse(StudioSheet.Mark.NONE);
    }

    private static String title(final String modid) {
        final String name = StudioThings.modName(modid);
        return name.isEmpty() ? GameText.resolve(StudioTexts.PACK.with(modid)) : name;
    }

    private static void toggle(final Set<String> open, final String key) {
        if (!open.remove(key)) {
            open.add(key);
        }
    }

    private static String key(final String modid, final ThingKind kind) {
        return modid + "/" + kind.name();
    }

    /** What a row of the tree is. */
    enum Type {
        MOD,
        KIND,
        THING
    }

    /**
     * A row of the tree.
     *
     * @param type     what it is
     * @param modid    the mod it is or is under
     * @param kind     the kind it is or is under; none for a mod
     * @param thing    the thing, for a thing
     * @param label    what it says
     * @param count    how many things it holds, for a mod or a kind
     * @param mark     the worst problem of what it holds or is
     * @param problems how many problems a thing has
     * @param open     whether a mod or a kind shows what it holds
     */
    record Row(Type type, String modid, @Nullable ThingKind kind, @Nullable StudioThing thing, String label,
               int count, StudioSheet.Mark mark, int problems, boolean open) {
    }
}
