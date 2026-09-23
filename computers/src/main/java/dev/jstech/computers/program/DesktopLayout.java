/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program;

import dev.jstech.computers.gui.CdeStyle;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/**
 * How a machine's desktop is laid out: its wallpaper, CDE's look, and where each icon was put.
 */
public final class DesktopLayout {

    private String wallpaper = "";
    /** CDE's palette and the backdrop of each workspace, as {@code CdeStyle} keeps them; empty until one is chosen. */
    private String cdeStyle = "";
    /**
     * Free-positioned desktop icon cells, keyed by the icon's stable id ({@code app:<label>} for a program
     * launcher, {@code file:<name>} for a desktop file or folder). Each value packs the grid column in the
     * high 16 bits and the row in the low 16 bits, so the slot stays put across monitor sizes (snap-to-grid).
     * Icons with no entry fall back to the auto-flow layout.
     */
    private final Map<String, Integer> iconCells = new LinkedHashMap<>();

    /** Packs a desktop grid column and row into a single value for {@link #iconCells}. */
    public static int packCell(final int column, final int row) {
        return (column << 16) | (row & 0xFFFF);
    }

    public static int cellColumn(final int packed) {
        return packed >> 16;
    }

    public static int cellRow(final int packed) {
        return packed & 0xFFFF;
    }

    /** The chosen desktop wallpaper id ({@code ""} means the system's default). */
    public String wallpaper() {
        return this.wallpaper;
    }

    public void setWallpaper(final String id) {
        this.wallpaper = id == null ? "" : id;
    }

    /** CDE's look on this machine, which is its wallpaper and its window colours at once. */
    public CdeStyle cdeStyle() {
        return CdeStyle.parse(this.cdeStyle);
    }

    public void setCdeStyle(final CdeStyle style) {
        this.cdeStyle = style == null || style.equals(CdeStyle.DEFAULT) ? "" : style.encoded();
    }

    /** A read-only view of every pinned desktop icon's cell, keyed by its stable id. */
    public Map<String, Integer> iconCells() {
        return Map.copyOf(this.iconCells);
    }

    /** Pins a desktop icon ({@code key}) to a packed grid cell, replacing any previous position for it. */
    public void setIconCell(final String key, final int packedCell) {
        if (key != null && !key.isEmpty()) {
            this.iconCells.put(key, packedCell);
        }
    }

    /** Forgets a pinned icon position (e.g. when its file is deleted or moved off the desktop). */
    public void clearIconCell(final String key) {
        this.iconCells.remove(key);
    }

    void save(final CompoundTag tag) {
        if (!this.wallpaper.isEmpty()) {
            tag.putString("Wallpaper", this.wallpaper);
        }
        if (!this.cdeStyle.isEmpty()) {
            tag.putString("CdeStyle", this.cdeStyle);
        }
        if (!this.iconCells.isEmpty()) {
            final ListTag cells = new ListTag();
            for (final Map.Entry<String, Integer> e : this.iconCells.entrySet()) {
                final CompoundTag c = new CompoundTag();
                c.putString("Key", e.getKey());
                c.putInt("Cell", e.getValue());
                cells.add(c);
            }
            tag.put("IconCells", cells);
        }
    }

    void load(final CompoundTag tag) {
        this.wallpaper = tag.getString("Wallpaper");
        this.cdeStyle = tag.getString("CdeStyle");
        this.iconCells.clear();
        for (final Tag entry : tag.getList("IconCells", Tag.TAG_COMPOUND)) {
            final CompoundTag c = (CompoundTag) entry;
            final String key = c.getString("Key");
            if (!key.isEmpty()) {
                this.iconCells.put(key, c.getInt("Cell"));
            }
        }
    }
}
