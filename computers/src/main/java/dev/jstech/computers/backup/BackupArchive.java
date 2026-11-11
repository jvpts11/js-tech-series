/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.backup;

import dev.jstech.core.id.StableNames;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

/**
 * Every backup a world's Backup Services made, kept with the world by its id. What holds a backup carries only its id:
 * a tape knows where on it each backup lies, a backup file on a server's drive names its backup, so a tape or a drive
 * stays a small item however much it keeps, as a drive's stored items are kept by the save (computers/docs/design/
 * programs.md). Erasing a tape lets the backups on it go.
 */
public final class BackupArchive extends SavedData {

    private final Map<UUID, Backup> backups = new HashMap<>();

    private static final String FILE = "jsc_backups";
    private static final String BACKUPS = "backups";
    private static final StableNames<BackupPart> PARTS = StableNames.of(BackupPart.class);

    /** The archive of the world {@code level} is in, kept with its overworld. */
    public static BackupArchive of(final ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(factory(), FILE);
    }

    /** How a world makes the archive afresh and reads it back from its save. */
    public static SavedData.Factory<BackupArchive> factory() {
        return new SavedData.Factory<>(BackupArchive::new, BackupArchive::load);
    }

    /** Keeps {@code backup}. */
    public void put(final Backup backup) {
        backups.put(backup.id(), backup);
        setDirty();
    }

    /** The backup with that id, or null when none is kept. */
    @Nullable
    public Backup get(final UUID id) {
        return backups.get(id);
    }

    /** Lets the backup with that id go, its last holder erased. */
    public void remove(final UUID id) {
        if (backups.remove(id) != null) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(final CompoundTag tag, final HolderLookup.Provider registries) {
        final ListTag list = new ListTag();
        for (final Backup backup : backups.values()) {
            list.add(backup.save());
        }
        tag.put(BACKUPS, list);
        return tag;
    }

    private static BackupArchive load(final CompoundTag tag, final HolderLookup.Provider registries) {
        final BackupArchive archive = new BackupArchive();
        for (final Tag one : tag.getList(BACKUPS, Tag.TAG_COMPOUND)) {
            final Backup backup = Backup.load((CompoundTag) one);
            archive.backups.put(backup.id(), backup);
        }
        return archive;
    }

    /**
     * One backup: what made it and when, what it keeps and what that weighs.
     *
     * @param id      its id, which its holders carry
     * @param job     the job that made it
     * @param madeAt  the game time it was made, which a restore of items counts losses from
     * @param clock   the world's clock when it was made, as the console shows it
     * @param parts   what it keeps
     * @param weight  what it weighs, in thousandths of an item of its holder's era, as a stored item does
     * @param items   how many of the network's items it keeps
     * @param content what it keeps, a compound for each part
     */
    public record Backup(UUID id, String job, long madeAt, long clock, Set<BackupPart> parts, long weight,
                         long items, CompoundTag content) {

        public Backup {
            parts = parts.isEmpty() ? EnumSet.noneOf(BackupPart.class) : EnumSet.copyOf(parts);
        }

        CompoundTag save() {
            final CompoundTag tag = new CompoundTag();
            tag.putUUID("id", id);
            tag.putString("job", job);
            tag.putLong("made", madeAt);
            tag.putLong("clock", clock);
            final ListTag names = new ListTag();
            for (final BackupPart part : parts) {
                names.add(StringTag.valueOf(part.serializedName()));
            }
            tag.put("parts", names);
            tag.putLong("weight", weight);
            tag.putLong("items", items);
            tag.put("content", content);
            return tag;
        }

        static Backup load(final CompoundTag tag) {
            final Set<BackupPart> parts = EnumSet.noneOf(BackupPart.class);
            final ListTag names = tag.getList("parts", Tag.TAG_STRING);
            for (int i = 0; i < names.size(); i++) {
                final BackupPart part = PARTS.find(names.getString(i));
                if (part != null) {
                    parts.add(part);
                }
            }
            return new Backup(tag.getUUID("id"), tag.getString("job"), tag.getLong("made"), tag.getLong("clock"),
                    parts, tag.getLong("weight"), tag.getLong("items"), tag.getCompound("content"));
        }
    }
}
