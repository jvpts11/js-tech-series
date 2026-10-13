/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block.part;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.crafting.CraftingEras;
import dev.jstech.computers.crafting.CraftingLog;
import dev.jstech.computers.crafting.ICraftIo;
import dev.jstech.computers.crafting.MultiStagePattern;
import dev.jstech.computers.crafting.NetworkRecipe;
import dev.jstech.computers.crafting.ProcessingPattern;
import dev.jstech.computers.menu.CraftingInterfaceMenu;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.multipart.IFacePart;
import dev.jstech.core.multipart.IPartHost;
import dev.jstech.core.multipart.PartType;
import dev.jstech.core.persistence.SavedValue;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.core.util.Utf8Text;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * A Crafting Interface: the part on the crafting cable that holds {@code .craft} files and feeds a machine with them.
 * It either sits against a machine with one input face and feeds it there, or a crafting cable of its own leaves its
 * face, with Crafting Input Routers on it that feed each of the machine's input faces; it sees only the routers on
 * that cable. Its era says how many patterns it holds. It is named, so software and a Receiving Bus tied by hand find
 * it; it runs one recipe at a time (exclusive, the default when it feeds through routers, where two recipes' inputs
 * would mix) or several jobs at once as far as its machine takes them; it can be paused and given a most jobs at once.
 *
 * <p>Each job it runs is an Operation of the network; the interface keeps how each ended and, after one settles, what
 * that job is still owed, so an output that comes out late is never credited to the next job.
 */
public final class CraftingInterfacePart implements IFacePart {

    private CableBlockEntity host;
    private Direction face = Direction.NORTH;
    private final HardwareEra era;
    private UUID id = UUID.randomUUID();
    private String name = "";
    /* Exclusive or not as set by hand or by a program; null while it follows what it feeds. */
    @Nullable
    private Boolean exclusive;
    private boolean paused;
    private int maxJobs;
    private final List<HeldPattern> patterns = new ArrayList<>();
    /* Which setting a program set last, by the setting's word, and the program's name; a hand on it clears it. */
    private final Map<String, String> setBy = new LinkedHashMap<>();
    private final CraftingLog log = new CraftingLog();
    private final List<Owed> owed = new ArrayList<>();
    /* Whether the lamps blink, and when the interface last fed or was credited; neither is saved. */
    private boolean busy;
    private long workedAt = Long.MIN_VALUE;

    /** The longest name an interface may carry. */
    public static final int MAX_NAME_LENGTH = 32;
    /** How long, in ticks, a settled job's outputs may keep coming before what is still owed is let go. */
    public static final int QUIET_TICKS = 100;
    /** The setting words a program's mark is kept under. */
    public static final String MODE = "MODE";
    public static final String STATE = "STATE";
    public static final String JOBS = "JOBS";
    public static final String ROUTES = "ROUTES";
    private static final long BUSY_FOR = 60L;

    public CraftingInterfacePart(final HardwareEra era) {
        this.era = era;
    }

    @Override
    public PartType<?> type() {
        return ComputingParts.craftingInterface(era);
    }

    @Override
    public void attach(final IPartHost host, final Direction face) {
        if (!(host instanceof CableBlockEntity cable)) {
            throw new IllegalArgumentException("an interface mounts on a cable block, not on " + host);
        }
        this.host = cable;
        this.face = face;
    }

    @Override
    public boolean use(final ServerPlayer player) {
        if (host == null) {
            return false;
        }
        final CableBlockEntity cable = host;
        final Direction mounted = face;
        player.openMenu(new SimpleMenuProvider((containerId, inventory, opener) ->
                        new CraftingInterfaceMenu(containerId, inventory, cable, mounted, this),
                partItem().getHoverName()), buf -> CraftingInterfaceMenu.Opening.of(cable, mounted, this).write(buf));
        return true;
    }

    @Override
    public ItemStack partItem() {
        return ComputingParts.craftingInterfaceItem(era);
    }

    @Override
    public boolean busy() {
        return busy;
    }

    @Override
    public void serverTick() {
        final ServerLevel level = host == null ? null : host.partServerLevel();
        if (level == null) {
            return;
        }
        final long now = level.getGameTime();
        if (busy && now - workedAt >= BUSY_FOR) {
            busy = false;
            host.partLooksChanged();
        }
    }

    /** The cable block it is on. */
    @Nullable
    public CableBlockEntity host() {
        return host;
    }

    /** The face it is on, which is the way it faces. */
    public Direction face() {
        return face;
    }

    public HardwareEra era() {
        return era;
    }

    /** The id a Receiving Bus tied by hand knows it by; it never changes. */
    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    /** How many patterns it holds at most, by its era. */
    public int capacity() {
        return CraftingEras.patternsPerInterface(era);
    }

    /** The patterns it holds, in the order they were placed. */
    public List<HeldPattern> patterns() {
        return List.copyOf(patterns);
    }

    /** Whether it holds {@code recipe} already. */
    public boolean holds(final NetworkRecipe recipe) {
        return indexOf(recipe) >= 0;
    }

    /** Where it holds {@code recipe}, or -1. */
    public int indexOf(final NetworkRecipe recipe) {
        for (int i = 0; i < patterns.size(); i++) {
            if (patterns.get(i).recipe().sameRecipe(recipe)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * The pattern it holds that carries {@code pattern}: the processing recipe itself, or a pipeline with it as one of
     * its stages. Null when it holds no such pattern.
     */
    @Nullable
    public HeldPattern holding(final ProcessingPattern pattern) {
        for (final HeldPattern held : patterns) {
            if (held.recipe().proc().isPresent() && held.recipe().proc().get().sameRecipe(pattern)) {
                return held;
            }
            if (held.recipe().multi().isPresent()) {
                for (final MultiStagePattern.Stage stage : held.recipe().multi().get().stages()) {
                    if (stage.proc().isPresent() && stage.proc().get().sameRecipe(pattern)) {
                        return held;
                    }
                }
            }
        }
        return null;
    }

    /** Places {@code recipe} in it: false when it is full, holds it already, or the recipe is a bench recipe. */
    public boolean place(final NetworkRecipe recipe) {
        if (recipe.bench().isPresent() || patterns.size() >= capacity() || holds(recipe)) {
            return false;
        }
        patterns.add(new HeldPattern(recipe, Map.of()));
        markChanged();
        return true;
    }

    /** Takes out the pattern at {@code index}, which it gives back; null when there is none. */
    @Nullable
    public NetworkRecipe take(final int index) {
        if (index < 0 || index >= patterns.size()) {
            return null;
        }
        final HeldPattern taken = patterns.remove(index);
        markChanged();
        return taken.recipe();
    }

    /**
     * Routes {@code input} of the pattern at {@code index} through the router {@code router}, or back to the router
     * whose filter takes it with null. {@code by} names the program that set it, empty for a hand.
     */
    public boolean route(final int index, final StorageKey input, @Nullable final UUID router, final String by) {
        if (index < 0 || index >= patterns.size()) {
            return false;
        }
        final HeldPattern held = patterns.get(index);
        final Map<String, UUID> routes = new HashMap<>(held.routes());
        if (router == null) {
            routes.remove(input.id());
        } else {
            routes.put(input.id(), router);
        }
        patterns.set(index, new HeldPattern(held.recipe(), routes));
        return changed(ROUTES, by);
    }

    /** Whether it runs one recipe at a time: as set, or, set by nobody, exactly when it feeds through routers. */
    public boolean exclusive(final boolean throughRouters) {
        return exclusive != null ? exclusive : throughRouters;
    }

    /** Whether its mode was set rather than following what it feeds. */
    public boolean exclusiveSet() {
        return exclusive != null;
    }

    public boolean paused() {
        return paused;
    }

    /** The most jobs it runs at once, 0 for as many as come. */
    public int maxJobs() {
        return maxJobs;
    }

    /** The program that set {@code setting} last, or empty when it was set by hand or never. */
    public String setBy(final String setting) {
        return setBy.getOrDefault(setting, "");
    }

    /** Every program mark, by setting word. */
    public Map<String, String> marks() {
        return Map.copyOf(setBy);
    }

    public CraftingLog log() {
        return log;
    }

    public void setName(final String newName) {
        name = Utf8Text.field(newName, MAX_NAME_LENGTH);
        markChanged();
    }

    public boolean setExclusive(final boolean oneAtATime, final String by) {
        exclusive = oneAtATime;
        return changed(MODE, by);
    }

    public boolean setPaused(final boolean held, final String by) {
        paused = held;
        return changed(STATE, by);
    }

    public boolean setMaxJobs(final int most, final String by) {
        maxJobs = Math.max(0, Math.min(64, most));
        return changed(JOBS, by);
    }

    /** It fed its machine or was credited at {@code now}: its lamps blink, and the jobs on it are moving. */
    public void worked(final long now) {
        workedAt = now;
        if (!busy) {
            busy = true;
            if (host != null) {
                host.partLooksChanged();
            }
        }
    }

    /** When it last fed its machine or was credited, so the jobs on it know the machine is moving. */
    public long workedAt() {
        return workedAt;
    }

    /** What settled jobs are still owed, oldest first: their late outputs are credited to these, never to a new job. */
    public List<Owed> owed() {
        return owed;
    }

    /** Lets go of everything still owed whose window has passed or that was paid in full; a tick of the drain. */
    public void drainTick() {
        if (owed.isEmpty()) {
            return;
        }
        owed.removeIf(o -> o.remaining <= 0 || --o.quietLeft <= 0);
        markChanged();
    }

    @Override
    public void save(final CompoundTag tag, final HolderLookup.Provider registries) {
        tag.putUUID("Id", id);
        if (!name.isEmpty()) {
            tag.putString("Name", name);
        }
        if (exclusive != null) {
            tag.putBoolean("Exclusive", exclusive);
        }
        tag.putBoolean("Paused", paused);
        tag.putInt("MaxJobs", maxJobs);
        final RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, registries);
        SavedValue.written(HeldPattern.CODEC.listOf().encodeStart(ops, patterns), JsComputers.LOGGER,
                "an interface's patterns").ifPresent(encoded -> tag.put("Patterns", encoded));
        final CompoundTag marks = new CompoundTag();
        setBy.forEach(marks::putString);
        tag.put("SetBy", marks);
        final ListTag lines = new ListTag();
        for (final CraftingLog.Entry entry : log.entries().reversed()) {
            lines.add(saveEntry(entry));
        }
        tag.put("Log", lines);
        final ListTag owing = new ListTag();
        for (final Owed o : owed) {
            StorageKey.CODEC.encodeStart(ops, o.key).result().ifPresent(keyTag -> {
                final CompoundTag row = new CompoundTag();
                row.put("Key", keyTag);
                row.putLong("Remaining", o.remaining);
                row.putInt("Quiet", o.quietLeft);
                row.putLong("Seq", o.seq);
                row.putString("Recipe", o.recipe);
                owing.add(row);
            });
        }
        tag.put("Owed", owing);
    }

    @Override
    public void load(final CompoundTag tag, final HolderLookup.Provider registries) {
        if (tag.hasUUID("Id")) {
            id = tag.getUUID("Id");
        }
        name = tag.getString("Name");
        exclusive = tag.contains("Exclusive") ? tag.getBoolean("Exclusive") : null;
        paused = tag.getBoolean("Paused");
        maxJobs = tag.getInt("MaxJobs");
        patterns.clear();
        final RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, registries);
        if (tag.contains("Patterns")) {
            SavedValue.read(HeldPattern.CODEC.listOf().parse(ops, tag.get("Patterns")), JsComputers.LOGGER,
                    "an interface's patterns").ifPresent(patterns::addAll);
        }
        setBy.clear();
        final CompoundTag marks = tag.getCompound("SetBy");
        for (final String setting : marks.getAllKeys()) {
            setBy.put(setting, marks.getString(setting));
        }
        final List<CraftingLog.Entry> lines = new ArrayList<>();
        for (final Tag one : tag.getList("Log", Tag.TAG_COMPOUND)) {
            lines.add(loadEntry((CompoundTag) one));
        }
        log.restore(lines);
        owed.clear();
        for (final Tag one : tag.getList("Owed", Tag.TAG_COMPOUND)) {
            final CompoundTag row = (CompoundTag) one;
            final StorageKey key = SavedValue.readOr(StorageKey.CODEC.parse(ops, row.get("Key")), JsComputers.LOGGER,
                    "what a settled job is still owed", null);
            if (key != null) {
                owed.add(new Owed(key, row.getLong("Remaining"), row.getInt("Quiet"), row.getLong("Seq"),
                        row.getString("Recipe"), null));
            }
        }
    }

    /** A line of a crafting part's activity, as it is saved. */
    public static CompoundTag saveEntry(final CraftingLog.Entry entry) {
        final CompoundTag line = new CompoundTag();
        line.putLong("Time", entry.time());
        line.putString("What", entry.what());
        line.putLong("Amount", entry.amount());
        line.putLong("Total", entry.total());
        line.putByte("Kind", entry.kind());
        line.putString("Note", entry.note());
        return line;
    }

    /** A line of a crafting part's activity read back from a save. */
    public static CraftingLog.Entry loadEntry(final CompoundTag line) {
        return new CraftingLog.Entry(line.getLong("Time"), line.getString("What"), line.getLong("Amount"),
                line.getLong("Total"), line.getByte("Kind"), line.getString("Note"));
    }

    private boolean changed(final String setting, final String by) {
        if (by == null || by.isEmpty()) {
            setBy.remove(setting);
        } else {
            setBy.put(setting, Utf8Text.field(by, MAX_NAME_LENGTH));
        }
        markChanged();
        return true;
    }

    private void markChanged() {
        if (host != null) {
            host.setChanged();
        }
    }

    /**
     * A pattern the interface holds, with the router each input goes through where one was chosen by hand: the key's
     * id to the router's id. An input chosen by nobody goes through the router whose filter takes it.
     */
    public record HeldPattern(NetworkRecipe recipe, Map<String, UUID> routes) {

        public static final Codec<HeldPattern> CODEC = RecordCodecBuilder.create(i -> i.group(
                NetworkRecipe.CODEC.fieldOf("recipe").forGetter(HeldPattern::recipe),
                Codec.unboundedMap(Codec.STRING, UUIDUtil.CODEC).optionalFieldOf("routes", Map.of())
                        .forGetter(HeldPattern::routes)
        ).apply(i, HeldPattern::new));

        public HeldPattern {
            routes = Map.copyOf(routes);
        }
    }

    /**
     * What a settled job is still owed of one output: the lots it fed that have not all come out. What comes out of
     * it within the quiet window goes where that job's outputs go, the craft it was part of or the network, never to
     * the next job.
     *
     * <p>The sink is not saved: after a reload the late outputs of an owed job go to the network, the same place a
     * restored step sends what it held, because resolving the craft again would need an id that outlives a reload.
     */
    public static final class Owed {

        private final StorageKey key;
        private long remaining;
        private int quietLeft;
        private final long seq;
        private final String recipe;
        @Nullable
        private final ICraftIo sink;

        /**
         * @param key       the output
         * @param remaining how much of it the job fed for and has not had back
         * @param quietLeft how long it may stay quiet before the rest is let go
         * @param seq       the order the job was fed in
         * @param recipe    the recipe the job ran, as {@link ProcessingPattern#identity()} writes it
         * @param sink      where the job's outputs go, or null for the network
         */
        public Owed(final StorageKey key, final long remaining, final int quietLeft, final long seq,
                    final String recipe, @Nullable final ICraftIo sink) {
            this.key = key;
            this.remaining = remaining;
            this.quietLeft = quietLeft;
            this.seq = seq;
            this.recipe = recipe;
            this.sink = sink;
        }

        public StorageKey key() {
            return key;
        }

        /** The recipe the job ran, as {@link ProcessingPattern#identity()} writes it. */
        public String recipe() {
            return recipe;
        }

        public long remaining() {
            return remaining;
        }

        /** The order the job was fed in, against the live jobs: the oldest is paid first. */
        public long seq() {
            return seq;
        }

        /** Where its outputs go: the craft it was part of, or null for the network. */
        @Nullable
        public ICraftIo sink() {
            return sink;
        }

        /** {@code amount} of it came out: less is owed, and the quiet window starts again. */
        public void paid(final long amount, final int window) {
            remaining = Math.max(0L, remaining - amount);
            quietLeft = window;
        }
    }
}
