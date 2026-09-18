/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.computers.os.boot.SystemWelcome;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * The systems a disk carries, and which of them it boots.
 *
 * <p>A disk used to carry one system, written straight onto it, so installing a second wrote over the first
 * with no word about what had been lost. A real disk holds as many as there is room for and a boot manager
 * picks between them, which is the whole reason a boot manager exists.
 *
 * <p>What each system remembers about being greeted rides in here too, per system rather than per disk. It has
 * to: a disk with two systems on it meets each of them once, and a mark on the disk itself would have the
 * second one arrive already met because the first had been.
 *
 * @param installed every system on the disk, in the order they were installed
 * @param bootIndex which of them the disk boots when nothing says otherwise
 */
public record DiskSystems(List<Entry> installed, int bootIndex) {

    /** A disk carrying nothing, which is what a disk with no mark on it is. */
    public static final DiskSystems NONE = new DiskSystems(List.of(), 0);

    /** More systems than any disk in the game has room for, so a list that crosses the wire has an end to it. */
    public static final int MOST_SYSTEMS = 16;

    public static final Codec<DiskSystems> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Entry.CODEC.listOf().optionalFieldOf("installed", List.of()).forGetter(DiskSystems::installed),
            Codec.INT.optionalFieldOf("boot", 0).forGetter(DiskSystems::bootIndex)
    ).apply(inst, DiskSystems::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, DiskSystems> STREAM_CODEC = StreamCodec.composite(
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list(MOST_SYSTEMS)), DiskSystems::installed,
            ByteBufCodecs.VAR_INT, DiskSystems::bootIndex,
            DiskSystems::new);

    public DiskSystems {
        installed = installed == null ? List.of() : List.copyOf(installed.size() > MOST_SYSTEMS
                ? installed.subList(0, MOST_SYSTEMS) : installed);
        bootIndex = installed.isEmpty() ? 0 : Math.max(0, Math.min(bootIndex, installed.size() - 1));
    }

    /** One system on a disk: which it is, and what it remembers about having been met. */
    public record Entry(ResourceLocation osId, SystemWelcome welcome) {

        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                ResourceLocation.CODEC.fieldOf("os").forGetter(Entry::osId),
                SystemWelcome.CODEC.optionalFieldOf("welcome", SystemWelcome.UNSEEN).forGetter(Entry::welcome)
        ).apply(inst, Entry::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, Entry::osId,
                SystemWelcome.STREAM_CODEC, Entry::welcome,
                Entry::new);

        public Entry {
            welcome = welcome == null ? SystemWelcome.UNSEEN : welcome;
        }
    }

    /** A disk carrying that one system and nothing else, freshly installed and never met. */
    public static DiskSystems of(final ResourceLocation osId) {
        return new DiskSystems(List.of(new Entry(osId, SystemWelcome.UNSEEN)), 0);
    }

    /** Whether the disk carries anything at all. */
    public boolean isEmpty() {
        return this.installed.isEmpty();
    }

    /** How many systems are on it. */
    public int count() {
        return this.installed.size();
    }

    /** The system this disk boots, or nothing when it carries none. */
    @Nullable
    public ResourceLocation boots() {
        return this.installed.isEmpty() ? null : this.installed.get(this.bootIndex).osId();
    }

    /** Whether that system is one of the ones on this disk. */
    public boolean has(final ResourceLocation osId) {
        return this.indexOf(osId) >= 0;
    }

    /** Where that system sits in the list, or {@code -1} when the disk does not carry it. */
    public int indexOf(@Nullable final ResourceLocation osId) {
        for (int at = 0; at < this.installed.size(); at++) {
            if (this.installed.get(at).osId().equals(osId)) {
                return at;
            }
        }
        return -1;
    }

    /** Every system on the disk, by id, in the order they were installed. */
    public List<ResourceLocation> ids() {
        final List<ResourceLocation> out = new ArrayList<>(this.installed.size());
        for (final Entry entry : this.installed) {
            out.add(entry.osId());
        }
        return List.copyOf(out);
    }

    /** What that system remembers about being greeted; one the disk does not carry has met nobody. */
    public SystemWelcome welcomeOf(@Nullable final ResourceLocation osId) {
        final int at = this.indexOf(osId);
        return at < 0 ? SystemWelcome.UNSEEN : this.installed.get(at).welcome();
    }

    /**
     * The same disk with that system on it as well, booting it.
     *
     * <p>A system already there is left exactly as it was, mark and all, and simply becomes the one that boots:
     * installing what is already installed is not a first meeting all over again.
     */
    public DiskSystems with(final ResourceLocation osId) {
        final int already = this.indexOf(osId);
        if (already >= 0) {
            return new DiskSystems(this.installed, already);
        }
        final List<Entry> grown = new ArrayList<>(this.installed);
        grown.add(new Entry(osId, SystemWelcome.UNSEEN));
        return new DiskSystems(grown, grown.size() - 1);
    }

    /** The same disk without that system, booting whatever is left. */
    public DiskSystems without(final ResourceLocation osId) {
        final int at = this.indexOf(osId);
        if (at < 0) {
            return this;
        }
        final List<Entry> left = new ArrayList<>(this.installed);
        left.remove(at);
        /*
         * Booting whatever stood before the one that went, so removing a system a player was not booting leaves
         * the one they were booting still chosen rather than sliding onto its neighbour.
         */
        final int boots = this.bootIndex > at ? this.bootIndex - 1 : Math.min(this.bootIndex, left.size() - 1);
        return new DiskSystems(left, Math.max(0, boots));
    }

    /** The same disk, booting that system instead; one it does not carry changes nothing. */
    public DiskSystems booting(final ResourceLocation osId) {
        final int at = this.indexOf(osId);
        return at < 0 ? this : new DiskSystems(this.installed, at);
    }

    /** The same disk, with that system's mark set to whatever it is now. */
    public DiskSystems remembering(final ResourceLocation osId, final SystemWelcome welcome) {
        final int at = this.indexOf(osId);
        if (at < 0) {
            return this;
        }
        final List<Entry> marked = new ArrayList<>(this.installed);
        marked.set(at, new Entry(osId, welcome));
        return new DiskSystems(marked, this.bootIndex);
    }
}
