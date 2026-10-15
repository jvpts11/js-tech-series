/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.protection;

import dev.jstech.core.team.CoreTeams;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

/**
 * Who used, changed or broke what another player owns, written down when the server's settings say so, the newest
 * last; the oldest entries make way once the log holds as many as the settings allow. Kept with the world, read with
 * {@code /jstech audit}.
 */
public final class AuditLog extends SavedData {

    private final Deque<Entry> entries = new ArrayDeque<>();

    /** The name of its file in the world's saved data. */
    private static final String FILE = "jstech_audit";
    private static final String ENTRIES = "entries";
    private static final String TIME = "time";
    private static final String ACTOR = "actor";
    private static final String ACTOR_NAME = "actorName";
    private static final String OWNER = "owner";
    private static final String KIND = "kind";
    private static final String THING = "thing";
    private static final String DIMENSION = "dimension";
    private static final String POS = "pos";

    /** What was done. */
    public enum Kind {
        /** Opened or used. */
        USE,
        /** Set up: settings, sides, access, trust. */
        CHANGE,
        /** Broken. */
        BREAK;

        /** Its name in the log and in commands: {@code use}, {@code change}, {@code break}. */
        public String serializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        /** The kind named {@code name}, or none. */
        public static Optional<Kind> byName(final String name) {
            for (final Kind kind : values()) {
                if (kind.serializedName().equals(name)) {
                    return Optional.of(kind);
                }
            }
            return Optional.empty();
        }
    }

    /**
     * One thing done.
     *
     * @param time      when, in milliseconds since the epoch
     * @param actor     who did it: the player, or the owner of the machine that did
     * @param actorName the name the server knew them by then
     * @param owner     whose the thing is
     * @param kind      what was done
     * @param thing     what it was: the block's or the entity's id
     * @param dimension where
     * @param pos       and at which place
     */
    public record Entry(long time, UUID actor, String actorName, UUID owner, Kind kind, ResourceLocation thing,
                        ResourceLocation dimension, BlockPos pos) {

        public Entry {
            Objects.requireNonNull(actor, "actor");
            Objects.requireNonNull(actorName, "actorName");
            Objects.requireNonNull(owner, "owner");
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(thing, "thing");
            Objects.requireNonNull(dimension, "dimension");
            pos = pos.immutable();
        }

        CompoundTag save() {
            final CompoundTag tag = new CompoundTag();
            tag.putLong(TIME, time);
            tag.putUUID(ACTOR, actor);
            tag.putString(ACTOR_NAME, actorName);
            tag.putUUID(OWNER, owner);
            tag.putString(KIND, kind.serializedName());
            tag.putString(THING, thing.toString());
            tag.putString(DIMENSION, dimension.toString());
            tag.putLong(POS, pos.asLong());
            return tag;
        }

        @Nullable
        static Entry load(final CompoundTag tag) {
            final ResourceLocation thing = ResourceLocation.tryParse(tag.getString(THING));
            final ResourceLocation dimension = ResourceLocation.tryParse(tag.getString(DIMENSION));
            final Optional<Kind> kind = Kind.byName(tag.getString(KIND));
            if (thing == null || dimension == null || kind.isEmpty() || !tag.hasUUID(ACTOR) || !tag.hasUUID(OWNER)) {
                return null;
            }
            return new Entry(tag.getLong(TIME), tag.getUUID(ACTOR), tag.getString(ACTOR_NAME), tag.getUUID(OWNER),
                    kind.get(), thing, dimension, BlockPos.of(tag.getLong(POS)));
        }
    }

    /** The log of {@code server}, kept in its overworld. */
    public static AuditLog of(final MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(AuditLog::new,
                AuditLog::load), FILE);
    }

    /**
     * Writes down that {@code actor} did {@code kind} to {@code thing}, which {@code owner} owns, at {@code pos} in
     * {@code level}, when the server's settings keep the log. What owners do to their own things is not written.
     */
    public static void record(final ServerLevel level, final BlockPos pos, final UUID actor, final Kind kind,
                              final ResourceLocation thing, final UUID owner) {
        if (!ProtectionBalance.audit() || actor.equals(owner)) {
            return;
        }
        final MinecraftServer server = level.getServer();
        of(server).add(new Entry(System.currentTimeMillis(), actor, CoreTeams.nameOf(server, actor)
                .orElse(actor.toString()), owner, kind, thing, level.dimension().location(), pos),
                ProtectionBalance.auditEntries());
    }

    /** Writes {@code entry} down, the oldest entries making way past {@code most}. */
    public void add(final Entry entry, final int most) {
        entries.addLast(entry);
        while (entries.size() > Math.max(1, most)) {
            entries.removeFirst();
        }
        setDirty();
    }

    /** How many entries it holds. */
    public int size() {
        return entries.size();
    }

    /**
     * The entries {@code keep} keeps, newest first, skipping the first {@code skip} of them and giving at most
     * {@code count}.
     */
    public List<Entry> newest(final Predicate<Entry> keep, final int skip, final int count) {
        final List<Entry> out = new ArrayList<>();
        int skipped = 0;
        final Iterator<Entry> newestFirst = entries.descendingIterator();
        while (newestFirst.hasNext() && out.size() < count) {
            final Entry entry = newestFirst.next();
            if (!keep.test(entry)) {
                continue;
            }
            if (skipped < skip) {
                skipped++;
            } else {
                out.add(entry);
            }
        }
        return out;
    }

    @Override
    public CompoundTag save(final CompoundTag tag, final HolderLookup.Provider registries) {
        final ListTag list = new ListTag();
        for (final Entry entry : entries) {
            list.add(entry.save());
        }
        tag.put(ENTRIES, list);
        return tag;
    }

    private static AuditLog load(final CompoundTag tag, final HolderLookup.Provider registries) {
        final AuditLog log = new AuditLog();
        for (final Tag saved : tag.getList(ENTRIES, Tag.TAG_COMPOUND)) {
            final Entry entry = Entry.load((CompoundTag) saved);
            if (entry != null) {
                log.entries.addLast(entry);
            }
        }
        return log;
    }
}
