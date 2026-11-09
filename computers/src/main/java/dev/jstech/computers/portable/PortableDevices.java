/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.portable;

import dev.jstech.computers.JsComputers;
import dev.jstech.core.persistence.SaveLayout;
import java.util.HashMap;
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
 * apps and its alerts. A device's state is made the first time it is carried, with the apps it comes with.
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

    @Override
    public CompoundTag save(final CompoundTag tag, final HolderLookup.Provider registries) {
        final ListTag list = new ListTag();
        for (final Map.Entry<UUID, PortableState> entry : devices.entrySet()) {
            final CompoundTag one = entry.getValue().write();
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
            final PortableState state = one.hasUUID("Id") ? PortableState.read(one) : null;
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
