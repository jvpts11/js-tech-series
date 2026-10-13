/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program;

import dev.jstech.computers.os.ProgramVersions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

/**
 * The programs installed on a machine: which, at what version, which of them the machine built from source, and the
 * programs players wrote and published that it installed from the Mirror.
 *
 * <p>All of it lives on the system disk, so it goes when that disk is formatted.
 */
final class InstalledPrograms {

    private final Set<String> installed = new LinkedHashSet<>();
    /*
     * The mod version each installed package was built against. A mod update leaves packages behind their new
     * build, which is what `pckmgr update` exists to reconcile, the same way a real package manager reconciles a
     * repository that moved on without you.
     */
    private final Map<String, String> versions = new LinkedHashMap<>();
    /*
     * The installed programs this machine built from source rather than installed as a built package, which is
     * what lets each of them ask a little less of it (SourceAdvantage). Forgotten with the program, so the same
     * program installed again as a package asks what a package asks.
     */
    private final Set<String> builtFromSource = new LinkedHashSet<>();
    private final Map<String, ComputerConsoleState.Community> community = new LinkedHashMap<>();

    Set<String> installed() {
        return Set.copyOf(this.installed);
    }

    boolean isInstalled(final String programId) {
        return this.installed.contains(programId);
    }

    /**
     * Installs a program by id; false if it was already installed.
     *
     * <p>A program that was not installed arrives as a package, whatever a mark left from an earlier copy says: the
     * mark that it was built here is set after this, by the build that built it, and never outlives the copy it was
     * set for.
     */
    boolean install(final String programId) {
        final boolean added = this.installed.add(programId);
        if (added) {
            this.builtFromSource.remove(programId);
        }
        return added;
    }

    boolean uninstall(final String programId) {
        this.builtFromSource.remove(programId);
        return this.installed.remove(programId);
    }

    String version(final String programId) {
        return this.versions.getOrDefault(programId, "");
    }

    void setVersion(final String programId, final String version) {
        if (version == null || version.isBlank()) {
            this.versions.remove(programId);
        } else {
            this.versions.put(programId, version);
        }
    }

    /** Every installed package behind the version this build ships for it, each against its own version. */
    List<String> outdated() {
        final List<String> out = new ArrayList<>();
        for (final String id : this.installed) {
            if (!ProgramVersions.of(id).equals(this.versions.get(id))) {
                out.add(id);
            }
        }
        return out;
    }

    boolean builtFromSource(final String programId) {
        return this.builtFromSource.contains(programId);
    }

    Set<String> builtFromSource() {
        return Set.copyOf(this.builtFromSource);
    }

    /** Records that an installed program was built here from source; a program not installed is not recorded. */
    void markBuiltFromSource(final String programId) {
        if (this.installed.contains(programId)) {
            this.builtFromSource.add(programId);
        }
    }

    Collection<ComputerConsoleState.Community> community() {
        return List.copyOf(this.community.values());
    }

    @Nullable
    ComputerConsoleState.Community communityProgram(final String name) {
        return this.community.get(name);
    }

    void addCommunity(final ComputerConsoleState.Community program) {
        this.community.put(program.name(), program);
    }

    boolean removeCommunity(final String name) {
        return this.community.remove(name) != null;
    }

    /** Everything, as the disk it lived on is formatted. */
    void clear() {
        this.installed.clear();
        this.versions.clear();
        this.builtFromSource.clear();
        this.community.clear();
    }

    void save(final CompoundTag tag) {
        tag.put("Installed", strings(this.installed));
        if (!this.versions.isEmpty()) {
            final CompoundTag written = new CompoundTag();
            this.versions.forEach(written::putString);
            tag.put("InstalledVersions", written);
        }
        if (!this.builtFromSource.isEmpty()) {
            tag.put("BuiltFromSource", strings(this.builtFromSource));
        }
        if (!this.community.isEmpty()) {
            final ListTag written = new ListTag();
            for (final ComputerConsoleState.Community one : this.community.values()) {
                final CompoundTag each = new CompoundTag();
                each.putString("Name", one.name());
                each.putString("Version", one.version());
                each.putString("House", one.house());
                each.putString("Icon", one.icon());
                each.putString("Entry", one.entry());
                written.add(each);
            }
            tag.put("Community", written);
        }
    }

    void load(final CompoundTag tag) {
        this.clear();
        for (final Tag entry : tag.getList("Installed", Tag.TAG_STRING)) {
            this.installed.add(entry.getAsString());
        }
        final CompoundTag written = tag.getCompound("InstalledVersions");
        for (final String id : written.getAllKeys()) {
            this.versions.put(id, written.getString(id));
        }
        for (final Tag entry : tag.getList("BuiltFromSource", Tag.TAG_STRING)) {
            this.builtFromSource.add(entry.getAsString());
        }
        for (final Tag entry : tag.getList("Community", Tag.TAG_COMPOUND)) {
            final CompoundTag each = (CompoundTag) entry;
            this.community.put(each.getString("Name"), new ComputerConsoleState.Community(each.getString("Name"),
                    each.getString("Version"), each.getString("House"), each.getString("Icon"),
                    each.getString("Entry")));
        }
    }

    private static ListTag strings(final Collection<String> values) {
        final ListTag list = new ListTag();
        for (final String value : values) {
            list.add(StringTag.valueOf(value));
        }
        return list;
    }
}
