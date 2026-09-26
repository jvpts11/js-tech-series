/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program;

import dev.jstech.computers.audio.SoundOutput;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

/**
 * A machine's settings as its save keeps them, under the one compound they are written to.
 */
final class SettingsStorage {

    private static final String KEY = "Settings";

    private SettingsStorage() {
    }

    static void save(final ComputerSettings settings, final CompoundTag tag) {
        final CompoundTag s = new CompoundTag();
        s.putInt("Accent", settings.accent());
        s.putBoolean("Clock12h", settings.clock12h());
        s.putInt("GuiScale", settings.guiScale());
        s.putInt("Brightness", settings.brightness());
        s.putInt("Volume", settings.volume());
        s.putBoolean("Muted", settings.muted());
        s.putString("SoundOutput", settings.soundOutput().id());
        s.putString("SaveDrive", String.valueOf(settings.defaultSaveDrive()));
        s.putBoolean("RemovableAutoOpen", settings.removableAutoOpen());
        s.putBoolean("RemoteAllowed", settings.remoteAllowed());
        s.putBoolean("TaskbarCentered", settings.taskbarCentered());
        s.putBoolean("DarkMode", settings.darkMode());
        // Always written, even empty: a machine whose player unpinned everything must not get the default back.
        s.put("Pinned", strings(settings.pinned()));
        if (!settings.favourites().isEmpty()) {
            s.put("Favourites", strings(settings.favourites()));
        }
        if (!settings.shares().isEmpty()) {
            final ListTag shares = new ListTag();
            for (final ComputerSettings.Share share : settings.shares()) {
                final CompoundTag each = new CompoundTag();
                each.putString("Name", share.name());
                each.putString("Path", share.path());
                each.putBoolean("Write", share.writable());
                shares.add(each);
            }
            s.put("Shares", shares);
        }
        if (!settings.recipeChoices().isEmpty()) {
            final CompoundTag choices = new CompoundTag();
            settings.recipeChoices().forEach(choices::putInt);
            s.put("RecipeChoices", choices);
        }
        if (!settings.themePreset().isEmpty()) {
            s.putString("Theme", settings.themePreset());
        }
        if (!settings.defaultApps().isEmpty()) {
            final CompoundTag apps = new CompoundTag();
            settings.defaultApps().forEach(apps::putString);
            s.put("DefaultApps", apps);
        }
        if (!settings.variables().isEmpty()) {
            final CompoundTag named = new CompoundTag();
            settings.variables().forEach(named::putString);
            s.put("Variables", named);
        }
        tag.put(KEY, s);
    }

    static void load(final ComputerSettings settings, final CompoundTag tag) {
        final CompoundTag s = tag.getCompound(KEY);
        settings.setAccent(s.getInt("Accent"));
        settings.setClock12h(s.getBoolean("Clock12h"));
        settings.setGuiScale(s.getInt("GuiScale"));
        settings.setBrightness(s.contains("Brightness") ? s.getInt("Brightness") : 100);
        settings.setVolume(s.contains("Volume") ? s.getInt("Volume") : 100);
        settings.setMuted(s.getBoolean("Muted"));
        settings.setSoundOutput(SoundOutput.byId(s.getString("SoundOutput")));
        final String saveDrive = s.getString("SaveDrive");
        if (!saveDrive.isEmpty()) {
            settings.setDefaultSaveDrive(saveDrive.charAt(0));
        }
        settings.setRemovableAutoOpen(!s.contains("RemovableAutoOpen") || s.getBoolean("RemovableAutoOpen"));
        settings.setRemoteAllowed(!s.contains("RemoteAllowed") || s.getBoolean("RemoteAllowed"));
        settings.setTaskbarCentered(!s.contains("TaskbarCentered") || s.getBoolean("TaskbarCentered"));
        settings.setDarkMode(s.getBoolean("DarkMode"));
        if (s.contains("Pinned")) {
            settings.setPinned(strings(s.getList("Pinned", Tag.TAG_STRING)));
        }
        settings.setFavourites(strings(s.getList("Favourites", Tag.TAG_STRING)));
        final List<ComputerSettings.Share> shares = new ArrayList<>();
        final ListTag sharesTag = s.getList("Shares", Tag.TAG_COMPOUND);
        for (int i = 0; i < sharesTag.size(); i++) {
            final CompoundTag each = sharesTag.getCompound(i);
            shares.add(new ComputerSettings.Share(each.getString("Name"), each.getString("Path"),
                    each.getBoolean("Write")));
        }
        settings.setShares(shares);
        final Map<String, Integer> choices = new LinkedHashMap<>();
        final CompoundTag choicesTag = s.getCompound("RecipeChoices");
        for (final String key : choicesTag.getAllKeys()) {
            choices.put(key, choicesTag.getInt(key));
        }
        settings.putRecipeChoices(choices);
        settings.setThemePreset(s.getString("Theme"));
        settings.putDefaultApps(texts(s.getCompound("DefaultApps")));
        settings.putVariables(texts(s.getCompound("Variables")));
    }

    private static ListTag strings(final List<String> values) {
        final ListTag list = new ListTag();
        for (final String value : values) {
            list.add(StringTag.valueOf(value));
        }
        return list;
    }

    private static List<String> strings(final ListTag list) {
        final List<String> out = new ArrayList<>();
        for (final Tag entry : list) {
            out.add(entry.getAsString());
        }
        return out;
    }

    private static Map<String, String> texts(final CompoundTag tag) {
        final Map<String, String> out = new LinkedHashMap<>();
        for (final String key : tag.getAllKeys()) {
            out.put(key, tag.getString(key));
        }
        return out;
    }
}
