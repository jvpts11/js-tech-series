/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.portable;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.audio.media.MediaId;
import dev.jstech.core.persistence.SaveLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

/**
 * What the save keeps of every portable device (computers/docs/design/wireless.md), by the id its item carries: its
 * apps and its alerts, what it watches, its own storage and its gallery. A device's state is made the first time it is
 * carried, with the apps it comes with, and let go when the device is lost: its item destroyed, or out of every
 * inventory too long.
 */
public final class PortableDevices extends SavedData {

    private final Map<UUID, PortableState> devices = new HashMap<>();

    public static final String DATA_NAME = "jsc_portables";
    /** The layout the file is saved in. */
    public static final SaveLayout LAYOUT = SaveLayout.of(JsComputers.MODID + ":portables");

    public PortableDevices() {
    }

    public static SavedData.Factory<PortableDevices> factory() {
        return new SavedData.Factory<>(PortableDevices::new, PortableDevices::load);
    }

    /** The server's store. */
    public static PortableDevices get(final MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(factory(), DATA_NAME);
    }

    /** The state of the device {@code id}, made new for one of {@code model} the first time it is asked for. */
    public PortableState of(final UUID id, final PortableModel model) {
        return devices.computeIfAbsent(id, key -> {
            setDirty();
            return new PortableState(model);
        });
    }

    /** The state of the device {@code id}, or null when it has none yet. */
    @Nullable
    public PortableState find(final UUID id) {
        return devices.get(id);
    }

    /** Installs {@code app} on the device {@code id}; false when it has no state, has the app or cannot have it. */
    public boolean install(final UUID id, final MobileApp app) {
        final PortableState state = devices.get(id);
        return state != null && changed(state.install(app));
    }

    /** Keeps the wallet {@code address}, spent with {@code key}, on the device {@code id}; false when it has one. */
    public boolean keepWallet(final UUID id, final String address, final String key) {
        final PortableState state = devices.get(id);
        return state != null && changed(state.keepWallet(address, key));
    }

    /** Gives the device {@code id} an alert; false when it has no state yet. */
    public boolean deliver(final UUID id, final PortableAlert alert) {
        final PortableState state = devices.get(id);
        if (state == null) {
            return false;
        }
        state.deliver(alert);
        return changed(true);
    }

    /** Marks every alert of the device {@code id} seen; false when none was new. */
    public boolean seeAll(final UUID id) {
        final PortableState state = devices.get(id);
        return state != null && changed(state.seeAll());
    }

    /** Lets every alert of the device {@code id} go; false when it had none. */
    public boolean clearAlerts(final UUID id) {
        final PortableState state = devices.get(id);
        return state != null && changed(state.clearAlerts());
    }

    /** Has the device {@code id} take the kind {@code kind} of alert, or stop taking it. */
    public void toggle(final UUID id, final AlertKind kind) {
        final PortableState state = devices.get(id);
        if (state != null) {
            state.toggle(kind);
            changed(true);
        }
    }

    /** Has the device {@code id} watch {@code key} at {@code level}; false when it cannot watch another. */
    public boolean watch(final UUID id, final StorageKey key, final long level) {
        final PortableState state = devices.get(id);
        return state != null && changed(state.watch(key, level));
    }

    /** Has the device {@code id} stop watching {@code key}. */
    public boolean unwatch(final UUID id, final StorageKey key) {
        final PortableState state = devices.get(id);
        return state != null && changed(state.unwatch(key));
    }

    /** Whether the network's {@code count} of {@code key} has just gone below the level the device {@code id} set. */
    public boolean wentBelow(final UUID id, final StorageKey key, final long count) {
        final PortableState state = devices.get(id);
        final boolean before = state != null && state.wentBelow(key, count);
        // Coming back up to the level changes what is kept as well, though nobody is told.
        setDirty();
        return before;
    }

    /** Puts up to {@code amount} of {@code key} into the device's storage; answers how much it took. */
    public long insert(final UUID id, final StorageKey key, final long amount) {
        final PortableState state = devices.get(id);
        final long taken = state == null ? 0L : state.insert(key, amount);
        changed(taken > 0L);
        return taken;
    }

    /** Takes up to {@code amount} of {@code key} out of the device's storage; answers how much. */
    public long extract(final UUID id, final StorageKey key, final long amount) {
        final PortableState state = devices.get(id);
        final long taken = state == null ? 0L : state.extract(key, amount);
        changed(taken > 0L);
        return taken;
    }

    /** Keeps a picture the device's camera took; null when its gallery or its storage is full. */
    @Nullable
    public PortablePicture addPicture(final UUID id, final MediaId media, final int width, final int height,
                                      final long day, final byte[] thumbnail) {
        final PortableState state = devices.get(id);
        final PortablePicture picture = state == null ? null
                : state.addPicture(media, width, height, day, thumbnail);
        changed(picture != null);
        return picture;
    }

    /** Deletes the device's picture numbered {@code number}; false when it has none. */
    public boolean deletePicture(final UUID id, final int number) {
        final PortableState state = devices.get(id);
        return state != null && changed(state.deletePicture(number));
    }

    /** Marks the device {@code id} in an inventory or its dock at {@code now}. */
    public void seen(final UUID id, final long now) {
        final PortableState state = devices.get(id);
        if (state != null && state.seenAt() != now) {
            state.seen(now);
            setDirty();
        }
    }

    /** Lets the device {@code id} go, with everything on it; false when there was nothing to let go. */
    public boolean erase(final UUID id) {
        return changed(devices.remove(id) != null);
    }

    /** Lets go every device not in an inventory nor a dock since {@code before}, and answers which they were. */
    public List<UUID> eraseUnseenSince(final long before) {
        final List<UUID> gone = new ArrayList<>();
        devices.entrySet().removeIf(entry -> {
            if (entry.getValue().seenAt() < before) {
                gone.add(entry.getKey());
                return true;
            }
            return false;
        });
        changed(!gone.isEmpty());
        return gone;
    }

    /** Every device kept, by id. */
    public Map<UUID, PortableState> all() {
        return Collections.unmodifiableMap(devices);
    }

    @Override
    public CompoundTag save(final CompoundTag tag, final HolderLookup.Provider registries) {
        final ListTag list = new ListTag();
        for (final Map.Entry<UUID, PortableState> entry : devices.entrySet()) {
            final CompoundTag one = entry.getValue().write(registries);
            one.putUUID("Id", entry.getKey());
            list.add(one);
        }
        tag.put("Devices", list);
        return LAYOUT.stamp(tag);
    }

    private static PortableDevices load(final CompoundTag saved, final HolderLookup.Provider registries) {
        final PortableDevices store = new PortableDevices();
        final CompoundTag tag = LAYOUT.read(saved);
        for (final Tag element : tag.getList("Devices", Tag.TAG_COMPOUND)) {
            final CompoundTag one = (CompoundTag) element;
            final PortableState state = one.hasUUID("Id") ? PortableState.read(one, registries) : null;
            if (state != null) {
                store.devices.put(one.getUUID("Id"), state);
            }
        }
        return store;
    }

    /* Marks the store to be saved when {@code changed}, and says so. */
    private boolean changed(final boolean changed) {
        if (changed) {
            setDirty();
        }
        return changed;
    }
}
