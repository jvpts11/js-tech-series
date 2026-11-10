/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.player;

import dev.jstech.computers.os.DesktopEnvironmentDef;
import dev.jstech.computers.os.PanelStyle;
import dev.jstech.computers.os.SoftwareHouse;
import dev.jstech.computers.sigma.DesktopFacts;
import dev.jstech.computers.sigma.pack.Manifest;
import dev.jstech.computers.sigma.pack.Packed;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * A desktop a player wrote and built (computers/docs/design/player-systems.md): its look, the folder it was built from,
 * and its files, which its package puts on a disk in a folder of its own, as any package of a player's.
 *
 * <p>Its files are its programs' listings ({@code notes.asm}), each program's icon beside it ({@code notes.pix}), the
 * picture its menu wears ({@code menu.pix}) when it has one, and its wallpaper ({@code wallpaper.pix}): the build
 * gathers them from wherever the desktop named them, so on a disk they always stand under these names.
 *
 * @param id     the desktop, as a system starts it and a package installs it
 * @param facts  what it declared
 * @param folder the folder it was built from
 * @param files  its files, by their name in its package
 */
@TextHolder
public record PlayerDesktop(ResourceLocation id, DesktopFacts facts, String folder, Map<String, String> files) {

    /** The name of its wallpaper in its package. */
    public static final String WALLPAPER = "wallpaper.pix";
    /** The name of the picture its menu wears in its package. */
    public static final String MENU_ICON = "menu.pix";
    /** What a program's listing is called in its package, after the program's name. */
    public static final String LISTING = ".asm";
    /** What a program's icon is called in its package, after the program's name. */
    public static final String ICON = ".pix";

    /* The two programs of the mod's that every desktop offers: the terminal, and the computer's settings. */
    private static final ResourceLocation TERMINAL_ID = ResourceLocation.fromNamespaceAndPath("jsc", "command_prompt");
    private static final ResourceLocation SETTINGS_ID = ResourceLocation.fromNamespaceAndPath("jsc", "settings");
    private static final TextKey TERMINAL = TextKey.of("jsc.desktop.player.terminal", "Terminal");
    private static final TextKey SETTINGS = TextKey.of("jsc.desktop.player.settings", "Settings");

    public PlayerDesktop {
        files = Map.copyOf(files);
    }

    /** The id a desktop of that package name is given: the name a system starts it by, "lantern-desktop". */
    public static ResourceLocation idOf(final String packageName) {
        final String slug = packageName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]+", "-");
        return ResourceLocation.fromNamespaceAndPath("jsc", "desktop/" + (slug.isEmpty() ? "unnamed" : slug));
    }

    /**
     * The desktop as the machines draw it: in a style of its own, offering the mod's terminal and settings beside its
     * programs, which its menu lists alone, so nothing of the mod's stands on its wallpaper.
     */
    public DesktopEnvironmentDef def() {
        return new DesktopEnvironmentDef(this.id, this.facts.name(), PanelStyle.PLAYER,
                List.of(TERMINAL_ID, SETTINGS_ID), Map.of(TERMINAL_ID, TERMINAL, SETTINGS_ID, SETTINGS),
                new SoftwareHouse(this.facts.author(), this.facts.author()), Set.of(TERMINAL_ID, SETTINGS_ID));
    }

    /** Its package's manifest, as the Mirror shelves it and a machine installs it: its author is its house. */
    public Manifest manifest() {
        return new Manifest(this.facts.name(), this.facts.version(), this.facts.author(), this.id.toString(),
                Manifest.DEFAULT_ICON, 1, List.copyOf(this.files.keySet()), this.facts.label(), Manifest.DESKTOP);
    }

    /** The folder its package is installed into on a machine. */
    public String installedFolder() {
        return Packed.folderOf(this.facts.name());
    }

    /** Where its wallpaper stands on a machine it is installed on. */
    public String wallpaperPath() {
        return this.installedFolder() + "/" + WALLPAPER;
    }

    /** Where the picture its menu wears stands on a machine it is installed on. */
    public String menuIconPath() {
        return this.installedFolder() + "/" + MENU_ICON;
    }

    /** Where that program's listing stands on a machine it is installed on, from the disk's root, to run it by. */
    public String programPath(final String program) {
        return "/" + this.installedFolder() + "/" + program + LISTING;
    }

    /** Where that program's icon stands on a machine it is installed on. */
    public String iconPath(final String program) {
        return this.installedFolder() + "/" + program + ICON;
    }
}
