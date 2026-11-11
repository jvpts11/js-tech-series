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
import dev.jstech.core.persistence.SavedValue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import org.jetbrains.annotations.Nullable;

/**
 * What one portable device keeps on the server (computers/docs/design/wireless.md): the apps installed on it, the
 * alerts it got, the newest first, which kinds of alert it takes and the items it watches, with the level each is
 * watched at; the items requested into its own storage and the pictures its camera took, which share that storage;
 * and when it was last in an inventory, for the lost devices the server lets go. It changes only through
 * {@link PortableDevices}, which saves it; each change its system shows counts up its version, which tells its
 * player's game when to be told again.
 */
public final class PortableState {

    private final PortableModel model;
    private final Set<MobileApp> installed;
    private final List<PortableAlert> alerts = new ArrayList<>();
    private final Set<AlertKind> alertsOn = EnumSet.allOf(AlertKind.class);
    private final Map<StorageKey, Long> watched = new LinkedHashMap<>();
    /* The watched items already below their level, told once each until they come back up to it. */
    private final Set<StorageKey> below = new HashSet<>();
    private final Map<StorageKey, Long> stored = new LinkedHashMap<>();
    private final List<PortablePicture> pictures = new ArrayList<>();
    private int version;
    private int nextPicture = 1;
    private long seenAt;
    /* The Crypto Wallet's one wallet: the address and the key that spends from it, kept on the device itself. */
    private String walletAddress = "";
    private String walletKey = "";

    /** The alerts a device keeps; an older one goes when a new one comes past them. */
    public static final int MOST_ALERTS = 20;
    /** The items a device watches at most. */
    public static final int MOST_WATCHED = 12;
    /** The pictures a gallery keeps at most; a full one takes no more until some are deleted. */
    public static final int MOST_PICTURES = 60;

    PortableState(final PortableModel model) {
        this.model = model;
        this.installed = EnumSet.copyOf(MobileApp.factory(model));
    }

    public PortableModel model() {
        return model;
    }

    /** The address of the device's wallet, or empty before its Crypto Wallet made one. */
    public String walletAddress() {
        return walletAddress;
    }

    /** The key that spends from the device's wallet, or empty. */
    public String walletKey() {
        return walletKey;
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

    /** The kinds of alert it takes; all five from the factory. */
    public Set<AlertKind> alertsOn() {
        return Collections.unmodifiableSet(alertsOn);
    }

    /** The items it watches, each with the level below which it is told, in the order they were added. */
    public Map<StorageKey, Long> watched() {
        return Collections.unmodifiableMap(watched);
    }

    /** What its own storage holds, by kind, in the order it came. */
    public Map<StorageKey, Long> stored() {
        return Collections.unmodifiableMap(stored);
    }

    /** The pictures in its gallery, the oldest first. */
    public List<PortablePicture> pictures() {
        return Collections.unmodifiableList(pictures);
    }

    /** How much of its room is taken, in thousandths of an item: its items and its pictures. */
    public long used() {
        long weight = 0L;
        for (final long count : stored.values()) {
            weight += count * PortableStorage.ITEM_WEIGHT;
        }
        for (final PortablePicture picture : pictures) {
            weight += PortableStorage.pictureWeight(model, picture.media().bytes());
        }
        return weight;
    }

    /** How much of its room is free, in thousandths of an item. */
    public long free() {
        return Math.max(0L, PortableStorage.capacity(model) - used());
    }

    /** How many whole items more its storage takes. */
    public long freeItems() {
        return free() / PortableStorage.ITEM_WEIGHT;
    }

    /** The game time it was last in an inventory or in its dock. */
    public long seenAt() {
        return seenAt;
    }

    /** Counts up with every change its system shows. */
    public int version() {
        return version;
    }

    /** The device's state read back from what {@link #write} wrote; null for a device this mod no longer makes. */
    @Nullable
    static PortableState read(final CompoundTag tag, final HolderLookup.Provider registries) {
        final PortableModel model = PortableModel.byName(tag.getString("Model"));
        if (model == null) {
            return null;
        }
        final RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
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
        if (tag.contains("AlertsOn", Tag.TAG_LIST)) {
            state.alertsOn.clear();
            for (final Tag kind : tag.getList("AlertsOn", Tag.TAG_STRING)) {
                state.alertsOn.add(AlertKind.byName(kind.getAsString()));
            }
        }
        for (final Tag one : tag.getList("Watched", Tag.TAG_COMPOUND)) {
            final CompoundTag row = (CompoundTag) one;
            final StorageKey key = key(ops, row);
            if (key != null && state.watched.size() < MOST_WATCHED) {
                state.watched.put(key, Math.max(1L, row.getLong("Level")));
                if (row.getBoolean("Below")) {
                    state.below.add(key);
                }
            }
        }
        for (final Tag one : tag.getList("Stored", Tag.TAG_COMPOUND)) {
            final CompoundTag row = (CompoundTag) one;
            final StorageKey key = key(ops, row);
            if (key != null && row.getLong("Count") > 0L) {
                state.stored.merge(key, row.getLong("Count"), Long::sum);
            }
        }
        for (final Tag one : tag.getList("Pictures", Tag.TAG_COMPOUND)) {
            final PortablePicture picture = PortablePicture.read((CompoundTag) one);
            if (picture != null && state.pictures.size() < MOST_PICTURES) {
                state.pictures.add(picture);
            }
        }
        state.nextPicture = Math.max(1, tag.getInt("NextPicture"));
        state.seenAt = tag.getLong("SeenAt");
        state.walletAddress = tag.getString("WalletAddress");
        state.walletKey = tag.getString("WalletKey");
        return state;
    }

    /** Keeps the wallet {@code address}, spent with {@code key}, as the device's own; false when it has one. */
    boolean keepWallet(final String address, final String key) {
        if (!walletAddress.isEmpty()) {
            return false;
        }
        walletAddress = address;
        walletKey = key;
        version++;
        return true;
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

    /** Takes the kind {@code kind} of alert, or stops taking it. */
    void toggle(final AlertKind kind) {
        if (!alertsOn.remove(kind)) {
            alertsOn.add(kind);
        }
        version++;
    }

    /** Watches {@code key} at {@code level}, or moves its level; false when it watches as many as it can already. */
    boolean watch(final StorageKey key, final long level) {
        if (!watched.containsKey(key) && watched.size() >= MOST_WATCHED) {
            return false;
        }
        watched.put(key, Math.max(1L, level));
        below.remove(key);
        version++;
        return true;
    }

    /** Stops watching {@code key}; false when it did not. */
    boolean unwatch(final StorageKey key) {
        if (watched.remove(key) == null) {
            return false;
        }
        below.remove(key);
        version++;
        return true;
    }

    /**
     * Whether the network's {@code count} of the watched {@code key} has just gone below its level: true once, when
     * it goes down past it, and again only after it has come back up to it.
     */
    boolean wentBelow(final StorageKey key, final long count) {
        final Long level = watched.get(key);
        if (level == null) {
            return false;
        }
        if (count >= level) {
            below.remove(key);
            return false;
        }
        return below.add(key);
    }

    /** Puts up to {@code amount} of {@code key} into its storage, as much as its room takes; answers how much. */
    long insert(final StorageKey key, final long amount) {
        if (amount <= 0L || !key.isItem()) {
            return 0L;
        }
        final long taken = Math.min(amount, freeItems());
        if (taken > 0L) {
            stored.merge(key, taken, Long::sum);
        }
        return taken;
    }

    /** Takes up to {@code amount} of {@code key} out of its storage; answers how much. */
    long extract(final StorageKey key, final long amount) {
        final long held = stored.getOrDefault(key, 0L);
        final long taken = Math.max(0L, Math.min(amount, held));
        if (taken == held) {
            stored.remove(key);
        } else if (taken > 0L) {
            stored.put(key, held - taken);
        }
        return taken;
    }

    /**
     * Keeps a picture its camera took, which {@code media} is, numbered after the last; null when the gallery is full
     * or its storage has no room for it.
     */
    @Nullable
    PortablePicture addPicture(final MediaId media, final int width, final int height, final long day,
                               final byte[] thumbnail) {
        if (pictures.size() >= MOST_PICTURES || PortableStorage.pictureWeight(model, media.bytes()) > free()) {
            return null;
        }
        final PortablePicture picture = new PortablePicture(nextPicture++, media, width, height, day, thumbnail);
        pictures.add(picture);
        return picture;
    }

    /** The picture numbered {@code number}, or null when the gallery has none. */
    @Nullable
    PortablePicture picture(final int number) {
        for (final PortablePicture picture : pictures) {
            if (picture.number() == number) {
                return picture;
            }
        }
        return null;
    }

    /** Deletes the picture numbered {@code number}; false when the gallery has none. */
    boolean deletePicture(final int number) {
        return pictures.removeIf(picture -> picture.number() == number);
    }

    /** Marks it in an inventory or its dock at {@code now}. */
    void seen(final long now) {
        seenAt = now;
    }

    /** The device's state as it is saved. */
    CompoundTag write(final HolderLookup.Provider registries) {
        final RegistryOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
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
        final ListTag kinds = new ListTag();
        for (final AlertKind kind : alertsOn) {
            kinds.add(StringTag.valueOf(kind.serializedName()));
        }
        tag.put("AlertsOn", kinds);
        final ListTag watchedRows = new ListTag();
        for (final Map.Entry<StorageKey, Long> entry : watched.entrySet()) {
            final CompoundTag row = keyed(ops, entry.getKey());
            row.putLong("Level", entry.getValue());
            row.putBoolean("Below", below.contains(entry.getKey()));
            watchedRows.add(row);
        }
        tag.put("Watched", watchedRows);
        final ListTag storedRows = new ListTag();
        for (final Map.Entry<StorageKey, Long> entry : stored.entrySet()) {
            final CompoundTag row = keyed(ops, entry.getKey());
            row.putLong("Count", entry.getValue());
            storedRows.add(row);
        }
        tag.put("Stored", storedRows);
        final ListTag gallery = new ListTag();
        for (final PortablePicture picture : pictures) {
            gallery.add(picture.write());
        }
        tag.put("Pictures", gallery);
        tag.putInt("NextPicture", nextPicture);
        tag.putLong("SeenAt", seenAt);
        if (!walletAddress.isEmpty()) {
            tag.putString("WalletAddress", walletAddress);
            tag.putString("WalletKey", walletKey);
        }
        return tag;
    }

    /* A row of a saved list, with the key it is about. */
    private static CompoundTag keyed(final RegistryOps<Tag> ops, final StorageKey key) {
        final CompoundTag row = new CompoundTag();
        SavedValue.written(StorageKey.CODEC.encodeStart(ops, key), JsComputers.LOGGER, "a portable's item")
                .ifPresent(written -> row.put("Key", written));
        return row;
    }

    /* The key a saved row is about, or null for one that no longer reads. */
    @Nullable
    private static StorageKey key(final RegistryOps<Tag> ops, final CompoundTag row) {
        return row.contains("Key") ? SavedValue.readOr(StorageKey.CODEC.parse(ops, row.get("Key")),
                JsComputers.LOGGER, "a portable's item", null) : null;
    }
}
