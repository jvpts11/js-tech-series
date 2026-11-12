/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.storage;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.os.fs.FilesystemContents;
import dev.jstech.core.persistence.SaveFiles;
import dev.jstech.core.persistence.SaveLayout;
import dev.jstech.core.persistence.SavedValue;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Every drive's stored items, by volume id, in one save-wide store. A drive carries its id wherever it
 * goes (between machines, racks and dimensions) and finds its contents here; the contents never ride
 * on the item, so a drive full of thousands of types is still a tiny item to sync and compare.
 *
 * <p>Lives on the overworld's data storage so ids resolve from any dimension. A drive that is destroyed
 * leaves its volume behind; that leak is small and accepted.
 *
 * <p>A save encodes only the volumes written since the last one. Every key is an item stack run through
 * its codec, and the autosave of a base with thousands of drives spent whole seconds of one tick encoding
 * contents nobody had touched; an unchanged volume now hands its last encoding to the next save as it is.
 *
 * <p>The file carries the version of its layout. One saved before it did reads as version 0; one a newer version
 * of the mod saved is read as far as it can be, and kept aside once before it is saved over.
 */
public final class StorageVolumes extends SavedData {

    public static final String DATA_NAME = "jsc_storage_volumes";
    /** The layout the file is saved in. */
    public static final SaveLayout LAYOUT = SaveLayout.of(JsComputers.MODID + ":storage_volumes");
    private static final Logger LOGGER = LogUtils.getLogger();

    private final Map<UUID, StorageVolume> volumes = new HashMap<>();
    /** Each volume's contents as last encoded, dropped the moment the volume is written. */
    private final Map<UUID, Tag> encoded = new HashMap<>();
    /** Each snapshot as encoded, by its id: a snapshot never changes, so it is encoded once for every save after. */
    private final Map<UUID, CompoundTag> encodedSnapshots = new HashMap<>();
    /** The version a newer mod saved the file in, until its copy is kept; 0 when none did. */
    private int newer;

    public StorageVolumes() {
    }

    public static SavedData.Factory<StorageVolumes> factory() {
        return new SavedData.Factory<>(StorageVolumes::new, StorageVolumes::load);
    }

    public static StorageVolumes get(final MinecraftServer server) {
        final StorageVolumes store = server.overworld().getDataStorage().computeIfAbsent(factory(), DATA_NAME);
        if (store.newer > 0) {
            SaveFiles.keepNewerCopy(server.overworld(), DATA_NAME, store.newer);
            store.newer = 0;
        }
        return store;
    }

    /** The running server's store, or null off the server thread (a client, a render pass). */
    @Nullable
    public static StorageVolumes current() {
        final MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server != null && server.isSameThread() ? get(server) : null;
    }

    /** The volume with the given id, created blank when it does not exist yet. */
    public StorageVolume volume(final UUID id) {
        return volumes.computeIfAbsent(id, key -> {
            setDirty();
            return new StorageVolume(key, () -> touched(key), false);
        });
    }

    /** A volume was written: its last encoding no longer describes it, and the store has something to save. */
    private void touched(final UUID id) {
        encoded.remove(id);
        setDirty();
    }

    @Nullable
    public StorageVolume find(final UUID id) {
        return volumes.get(id);
    }

    /** Drops a volume for good; returns whether there was one. */
    public boolean remove(final UUID id) {
        final boolean existed = volumes.remove(id) != null;
        encoded.remove(id);
        if (existed) {
            setDirty();
        }
        return existed;
    }

    public int count() {
        return volumes.size();
    }

    /** How many volumes the next save has to encode: written since the last save, or never saved. For tests. */
    public int pendingEncodes() {
        int pending = 0;
        for (final Map.Entry<UUID, StorageVolume> entry : volumes.entrySet()) {
            if (!entry.getValue().blank() && !encoded.containsKey(entry.getKey())) {
                pending++;
            }
        }
        return pending;
    }

    @Override
    public CompoundTag save(final CompoundTag tag, final HolderLookup.Provider registries) {
        final RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, registries);
        final ListTag list = new ListTag();
        final Set<UUID> snapshotsSaved = new HashSet<>();
        for (final Map.Entry<UUID, StorageVolume> entry : volumes.entrySet()) {
            /*
             * A blank volume is recreated blank on demand; no need to write it. One that reads blank only because
             * none of its lines could be read (their mod is gone for now) keeps those lines, so it is written as it
             * was read.
             */
            if (entry.getValue().blank() && !encoded.containsKey(entry.getKey())) {
                continue;
            }
            final UUID id = entry.getKey();
            final CompoundTag one = new CompoundTag();
            one.putUUID("Id", id);
            saveCorruption(entry.getValue(), one, ops);
            saveSnapshots(entry.getValue(), one, ops, snapshotsSaved);
            Tag items = encoded.get(id);
            if (items == null) {
                final DataResult<Tag> fresh = ServerStorageContents.CODEC.encodeStart(ops, entry.getValue().snapshot());
                items = SavedValue.written(fresh, LOGGER, "what a server was storing (" + id + ")").orElse(null);
                if (items != null && fresh.result().isPresent()) {
                    encoded.put(id, items); // a whole encoding stands until the volume is written again
                }
            }
            if (items != null) {
                one.put("Items", items);
            }
            list.add(one);
        }
        // A snapshot let go, or on a volume dropped, needs its encoding no more.
        encodedSnapshots.keySet().retainAll(snapshotsSaved);
        tag.put("Volumes", list);
        return LAYOUT.stamp(tag);
    }

    private static StorageVolumes load(final CompoundTag saved, final HolderLookup.Provider registries) {
        final StorageVolumes store = new StorageVolumes();
        final int found = SaveLayout.versionOf(saved);
        if (LAYOUT.isNewer(found)) {
            store.newer = found;
        }
        final CompoundTag tag = LAYOUT.read(saved);
        final RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, registries);
        for (final Tag element : tag.getList("Volumes", Tag.TAG_COMPOUND)) {
            final CompoundTag one = (CompoundTag) element;
            if (!one.hasUUID("Id")) {
                continue;
            }
            final UUID id = one.getUUID("Id");
            final StorageVolume volume = new StorageVolume(id, () -> store.touched(id), false);
            final Tag items = one.get("Items");
            if (items != null) {
                final DataResult<ServerStorageContents> parsed = ServerStorageContents.CODEC.parse(ops, items);
                SavedValue.read(parsed, LOGGER, "what a server was storing (" + id + ")")
                        .ifPresent(contents -> volume.replaceAll(contents.items()));
                if (parsed.resultOrPartial(message -> { }).isPresent()) {
                    /*
                     * What was read is what would be written, so no encoding is owed. This also holds for a partly
                     * unreadable list: its raw lines (from a mod that is gone for now) are kept as they are until
                     * the volume is written again, instead of being dropped by the next save.
                     */
                    store.encoded.put(id, items);
                }
            }
            loadCorruption(volume, one, ops, id);
            store.loadSnapshots(volume, one, ops);
            store.volumes.put(id, volume);
        }
        if (found < LAYOUT.version()) {
            store.setDirty(); // written again at the next save, in today's layout
        }
        return store;
    }

    /*
     * What corruption did to a volume, written beside its contents only when there is any: a volume nothing ever
     * happened to saves as it always did. Small, so it is encoded afresh on every save.
     */
    private static void saveCorruption(final StorageVolume volume, final CompoundTag one,
                                       final RegistryOps<Tag> ops) {
        if (!volume.corrupted().isEmpty()) {
            SavedValue.written(ServerStorageContents.CODEC.encodeStart(ops,
                            new ServerStorageContents(volume.corrupted())), LOGGER, "what a disk had corrupted")
                    .ifPresent(tag -> one.put("Corrupted", tag));
        }
        if (!volume.corruptedFiles().files().isEmpty()) {
            SavedValue.written(FilesystemContents.CODEC.encodeStart(ops, volume.corruptedFiles()), LOGGER,
                    "the system files a disk had corrupted").ifPresent(tag -> one.put("CorruptedFiles", tag));
        }
        if (volume.badSectors() > 0) {
            one.putInt("BadSectors", volume.badSectors());
        }
        if (volume.dead()) {
            one.putBoolean("Dead", true);
        }
    }

    private static void loadCorruption(final StorageVolume volume, final CompoundTag one, final RegistryOps<Tag> ops,
                                       final UUID id) {
        if (!one.contains("Corrupted") && !one.contains("CorruptedFiles") && !one.contains("BadSectors")
                && !one.contains("Dead")) {
            return;
        }
        final Map<StorageKey, Long> corrupted = one.contains("Corrupted")
                ? SavedValue.read(ServerStorageContents.CODEC.parse(ops, one.get("Corrupted")), LOGGER,
                        "what a disk had corrupted (" + id + ")").map(ServerStorageContents::items).orElse(Map.of())
                : Map.of();
        final FilesystemContents files = one.contains("CorruptedFiles")
                ? SavedValue.read(FilesystemContents.CODEC.parse(ops, one.get("CorruptedFiles")), LOGGER,
                        "the system files a disk had corrupted (" + id + ")").orElse(FilesystemContents.EMPTY)
                : FilesystemContents.EMPTY;
        volume.restore(corrupted, files, one.getInt("BadSectors"), one.getBoolean("Dead"));
    }

    /*
     * The snapshots of a volume, written beside its contents only when it has any, each from its one encoding, and the
     * one btrfs is to start from. {@code saved} gathers the ids of every snapshot written.
     */
    private void saveSnapshots(final StorageVolume volume, final CompoundTag one, final RegistryOps<Tag> ops,
                               final Set<UUID> saved) {
        if (volume.snapshots().isEmpty()) {
            return;
        }
        final ListTag list = new ListTag();
        for (final VolumeSnapshot snapshot : volume.snapshots()) {
            CompoundTag kept = encodedSnapshots.get(snapshot.id());
            if (kept == null) {
                kept = encodeSnapshot(snapshot, ops);
                encodedSnapshots.put(snapshot.id(), kept);
            }
            saved.add(snapshot.id());
            list.add(kept);
        }
        one.put("Snapshots", list);
        if (volume.startsFrom() != null) {
            one.putUUID("StartsFrom", volume.startsFrom());
        }
    }

    private static CompoundTag encodeSnapshot(final VolumeSnapshot snapshot, final RegistryOps<Tag> ops) {
        final CompoundTag kept = new CompoundTag();
        kept.putUUID("Id", snapshot.id());
        kept.putInt("Number", snapshot.number());
        kept.putString("Name", snapshot.name());
        kept.putLong("Taken", snapshot.takenAt());
        SavedValue.written(ServerStorageContents.CODEC.encodeStart(ops, new ServerStorageContents(snapshot.items())),
                LOGGER, "what a snapshot kept").ifPresent(tag -> kept.put("Items", tag));
        if (!snapshot.files().files().isEmpty()) {
            SavedValue.written(FilesystemContents.CODEC.encodeStart(ops, snapshot.files()), LOGGER,
                    "the files a snapshot kept").ifPresent(tag -> kept.put("Files", tag));
        }
        return kept;
    }

    private void loadSnapshots(final StorageVolume volume, final CompoundTag one, final RegistryOps<Tag> ops) {
        if (!one.contains("Snapshots")) {
            return;
        }
        final List<VolumeSnapshot> snapshots = new ArrayList<>();
        for (final Tag element : one.getList("Snapshots", Tag.TAG_COMPOUND)) {
            final CompoundTag kept = (CompoundTag) element;
            if (!kept.hasUUID("Id")) {
                continue;
            }
            final UUID id = kept.getUUID("Id");
            final Map<StorageKey, Long> items = kept.contains("Items")
                    ? SavedValue.read(ServerStorageContents.CODEC.parse(ops, kept.get("Items")), LOGGER,
                            "what a snapshot kept (" + id + ")").map(ServerStorageContents::items).orElse(Map.of())
                    : Map.of();
            final FilesystemContents files = kept.contains("Files")
                    ? SavedValue.read(FilesystemContents.CODEC.parse(ops, kept.get("Files")), LOGGER,
                            "the files a snapshot kept (" + id + ")").orElse(FilesystemContents.EMPTY)
                    : FilesystemContents.EMPTY;
            snapshots.add(new VolumeSnapshot(id, kept.getInt("Number"), kept.getString("Name"), kept.getLong("Taken"),
                    items, files));
            encodedSnapshots.put(id, kept);
        }
        volume.restoreSnapshots(snapshots, one.hasUUID("StartsFrom") ? one.getUUID("StartsFrom") : null);
    }
}
