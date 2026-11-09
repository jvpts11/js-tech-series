/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.portable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

/**
 * What one portable device keeps on the server (computers/docs/design/wireless.md): the apps installed on it and the
 * alerts it got, the newest first. It changes only through {@link PortableDevices}, which saves it; each change counts
 * up its version, which tells its player's game when to be told again.
 */
public final class PortableState {

    private final PortableModel model;
    private final Set<MobileApp> installed;
    private final List<PortableAlert> alerts = new ArrayList<>();
    private int version;

    /** The alerts a device keeps; an older one goes when a new one comes past them. */
    public static final int MOST_ALERTS = 20;

    PortableState(final PortableModel model) {
        this.model = model;
        this.installed = EnumSet.copyOf(MobileApp.factory(model));
    }

    public PortableModel model() {
        return model;
    }

    /** The apps on it, in the order a home screen shows them. */
    public Set<MobileApp> installed() {
        return Collections.unmodifiableSet(installed);
    }

    /** Its alerts, the newest first. */
    public List<PortableAlert> alerts() {
        return Collections.unmodifiableList(alerts);
    }

    /** How many of its alerts its player has not seen: the badge on the alerts' icon. */
    public int unseen() {
        int count = 0;
        for (final PortableAlert alert : alerts) {
            if (!alert.seen()) {
                count++;
            }
        }
        return count;
    }

    /** Counts up with every change. */
    public int version() {
        return version;
    }

    /** The device's state read back from what {@link #write} wrote; null for a device this mod no longer makes. */
    @Nullable
    static PortableState read(final CompoundTag tag) {
        final PortableModel model = PortableModel.byName(tag.getString("Model"));
        if (model == null) {
            return null;
        }
        final PortableState state = new PortableState(model);
        for (final Tag app : tag.getList("Apps", Tag.TAG_STRING)) {
            final MobileApp found = MobileApp.byName(app.getAsString());
            if (found != null && found.runsOn(model)) {
                state.installed.add(found);
            }
        }
        for (final Tag alert : tag.getList("Alerts", Tag.TAG_COMPOUND)) {
            if (state.alerts.size() < MOST_ALERTS) {
                state.alerts.add(PortableAlert.read((CompoundTag) alert));
            }
        }
        return state;
    }

    /** Puts {@code app} on it; false when it already has it or cannot have it. */
    boolean install(final MobileApp app) {
        if (!app.runsOn(model) || !installed.add(app)) {
            return false;
        }
        version++;
        return true;
    }

    /** Takes {@code alert} in, the newest, the oldest going past {@link #MOST_ALERTS}. */
    void deliver(final PortableAlert alert) {
        alerts.addFirst(alert);
        while (alerts.size() > MOST_ALERTS) {
            alerts.removeLast();
        }
        version++;
    }

    /** Marks every alert seen; false when none was new. */
    boolean seeAll() {
        boolean changed = false;
        for (int i = 0; i < alerts.size(); i++) {
            final PortableAlert alert = alerts.get(i);
            if (!alert.seen()) {
                alerts.set(i, alert.asSeen());
                changed = true;
            }
        }
        if (changed) {
            version++;
        }
        return changed;
    }

    /** Lets every alert go; false when there were none. */
    boolean clearAlerts() {
        if (alerts.isEmpty()) {
            return false;
        }
        alerts.clear();
        version++;
        return true;
    }

    /** The device's state as it is saved. */
    CompoundTag write() {
        final CompoundTag tag = new CompoundTag();
        tag.putString("Model", model.serializedName());
        final ListTag apps = new ListTag();
        for (final MobileApp app : installed) {
            apps.add(StringTag.valueOf(app.serializedName()));
        }
        tag.put("Apps", apps);
        final ListTag saved = new ListTag();
        for (final PortableAlert alert : alerts) {
            saved.add(alert.write());
        }
        tag.put("Alerts", saved);
        return tag;
    }
}
