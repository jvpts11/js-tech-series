/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.room;

import it.unimi.dsi.fastutil.longs.LongArrays;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * A sealed room on the server: its blocks of air, the gas it holds, and what bounds it, as its shape was last worked
 * out. Its air fills its blocks but those under water. A room whose walls were opened stays a room, leaking through
 * its holes, until it is closed again, or until its air has long been the place's, when it is forgotten.
 */
public final class Room {

    private final long id;
    private final LongOpenHashSet cells = new LongOpenHashSet();
    private final Object2DoubleOpenHashMap<ResourceLocation> moles = new Object2DoubleOpenHashMap<>();
    /* The blocks the room filled with water through a breach, pumped out again once it is closed. */
    private final LongOpenHashSet flooded = new LongOpenHashSet();
    private final LongOpenHashSet fires = new LongOpenHashSet();
    private List<RoomFace> faces = List.of();
    private int water;
    private int lava;
    private int plants;
    /* Its air as last read, kept until its gas or its shape changes; read often, by everyone who breathes it. */
    private @Nullable RoomAir air;
    /* Its blocks lowest first, for water rushing in to fill from the floor up; worked out when first needed. */
    private long @Nullable [] lowestFirst;
    /* The water that has rushed in this second and more, short of a whole block, in cubic metres. */
    private double floodDebt;
    /* Its outline as drawn, worked out when first shown after its shape changed. */
    private @Nullable List<RoomOutline.Segment> outline;
    private boolean breached;
    private boolean underWater;
    private AABB bounds = new AABB(BlockPos.ZERO);
    /* The pressure a second ago, and how fast it changed since, in kilopascals a second. */
    private double lastPressure = -1;
    private double change;
    /* How hard the air rushes out through its holes this second, 0 for not at all. */
    private double jet;
    /* How many seconds it has stood open with its air the place's, to be forgotten after a while. */
    private int settledOpen;
    /* Whether its air could be breathed a second ago, to tell when it fills. */
    private boolean breathableBefore = true;

    private static final String ID = "Id";
    private static final String CELLS = "Cells";
    private static final String MOLES = "Moles";
    private static final String FLOODED = "Flooded";
    private static final String BREACHED = "Breached";

    public Room(final long id) {
        this.id = id;
    }

    /** Its number, its own in its dimension. */
    public long id() {
        return this.id;
    }

    /** Its blocks of air, as positions packed into longs. */
    public LongSet cells() {
        return this.cells;
    }

    /** How many blocks it holds, in cubic metres. */
    public int volume() {
        return this.cells.size();
    }

    /** How many of its blocks hold air rather than water, in cubic metres. */
    public double gasVolume() {
        return Math.max(0, this.cells.size() - this.water) * GasMath.BLOCK;
    }

    /** What bounds it. */
    public List<RoomFace> faces() {
        return this.faces;
    }

    /** Its faces gas crosses: its cracks, doors and holes. */
    public List<RoomFace> leaks() {
        final List<RoomFace> leaking = new ArrayList<>();
        for (final RoomFace face : this.faces) {
            if (face.leaks()) {
                leaking.add(face);
            }
        }
        return leaking;
    }

    /** How many of its blocks hold water. */
    public int water() {
        return this.water;
    }

    /** How many of its blocks hold lava. */
    public int lava() {
        return this.lava;
    }

    /** How many plants grow in it, breathing its carbon dioxide out as oxygen. */
    public int plants() {
        return this.plants;
    }

    /** The fires burning in it. */
    public LongSet fires() {
        return this.fires;
    }

    /** The blocks it filled with water through a breach. */
    public LongSet flooded() {
        return this.flooded;
    }

    /** Whether its walls were opened: it leaks through holes until it is closed again. */
    public boolean breached() {
        return this.breached;
    }

    /** Whether water lies beyond any of its walls: a room under water. */
    public boolean underWater() {
        return this.underWater;
    }

    /** The box round its blocks. */
    public AABB bounds() {
        return this.bounds;
    }

    /** How fast its pressure changed over the last second, in kilopascals a second. */
    public double change() {
        return this.change;
    }

    /** How hard its air rushes out through its holes, 0 for not at all. */
    public double jet() {
        return this.jet;
    }

    /** Its pressure, in kilopascals. */
    public double pressure() {
        double total = 0;
        for (final Object2DoubleMap.Entry<ResourceLocation> entry : this.moles.object2DoubleEntrySet()) {
            total += Math.max(0, entry.getDoubleValue());
        }
        return GasMath.pressure(total, gasVolume(), GasMath.TEMPERED);
    }

    /** How many moles of {@code gas} it holds. */
    public double moles(final ResourceLocation gas) {
        return Math.max(0, this.moles.getDouble(gas));
    }

    /** Every gas it holds, with its moles. */
    public Map<ResourceLocation, Double> gases() {
        final Map<ResourceLocation, Double> held = new LinkedHashMap<>();
        for (final Object2DoubleMap.Entry<ResourceLocation> entry : this.moles.object2DoubleEntrySet()) {
            if (entry.getDoubleValue() > 0) {
                held.put(entry.getKey(), entry.getDoubleValue());
            }
        }
        return held;
    }

    /** Its air now. */
    public RoomAir air() {
        RoomAir now = this.air;
        if (now == null) {
            now = new RoomAir(gases(), gasVolume(), GasMath.TEMPERED);
            this.air = now;
        }
        return now;
    }

    /** Puts {@code amount} moles of {@code gas} into it; a negative amount takes, never below none. */
    public void add(final ResourceLocation gas, final double amount) {
        final double now = Math.max(0, this.moles.getDouble(gas) + amount);
        if (now <= 0) {
            this.moles.removeDouble(gas);
        } else {
            this.moles.put(gas, now);
        }
        this.air = null;
    }

    /** Takes up to {@code amount} moles of {@code gas} out of it; how many it took. */
    public double take(final ResourceLocation gas, final double amount) {
        final double taken = Math.min(moles(gas), Math.max(0, amount));
        add(gas, -taken);
        return taken;
    }

    /** Empties it of every gas. */
    public void empty() {
        this.moles.clear();
        this.air = null;
    }

    /** Its blocks of air, lowest first. */
    long[] lowestFirst() {
        long[] sorted = this.lowestFirst;
        if (sorted == null) {
            sorted = this.cells.toLongArray();
            LongArrays.quickSort(sorted, (a, b) -> Integer.compare(BlockPos.getY(a), BlockPos.getY(b)));
            this.lowestFirst = sorted;
        }
        return sorted;
    }

    /** Its outline as drawn. */
    public List<RoomOutline.Segment> outline() {
        List<RoomOutline.Segment> drawn = this.outline;
        if (drawn == null) {
            drawn = RoomOutline.of(this.cells, this.faces);
            this.outline = drawn;
        }
        return drawn;
    }

    /** The water rushed in short of a whole block, in cubic metres. */
    double floodDebt() {
        return this.floodDebt;
    }

    void setFloodDebt(final double debt) {
        this.floodDebt = debt;
    }

    /** Sets its blocks and what bounds them, as worked out again. */
    void shape(final LongSet cells, final List<RoomFace> faces, final int water, final int lava, final int plants,
               final LongSet fires, final boolean breached) {
        this.cells.clear();
        this.cells.addAll(cells);
        this.faces = List.copyOf(faces);
        this.water = water;
        this.lava = lava;
        this.plants = plants;
        this.air = null;
        this.lowestFirst = null;
        this.outline = null;
        this.fires.clear();
        this.fires.addAll(fires);
        this.breached = breached;
        this.underWater = faces.stream().anyMatch(RoomFace::water);
        this.flooded.retainAll(this.cells);
        this.bounds = boundsOf(this.cells);
    }

    /** Sets what lies beyond its faces again, its shape the same: the air round a room that moved. */
    void outsideNow(final List<RoomFace> moved) {
        this.faces = List.copyOf(moved);
    }

    /** Counts one more of its blocks under water, or one fewer. */
    void flood(final long cell, final boolean filled) {
        if (filled) {
            this.water++;
            this.flooded.add(cell);
        } else {
            this.water = Math.max(0, this.water - 1);
            this.flooded.remove(cell);
        }
        this.air = null;
    }

    /** Counts one more of its blocks holding water, or one fewer, as the world changed it. */
    void wet(final boolean filled) {
        this.water = Math.max(0, this.water + (filled ? 1 : -1));
        this.air = null;
    }

    /** Counts a fire lit in it, or put out. */
    void fire(final long cell, final boolean lit) {
        if (lit) {
            this.fires.add(cell);
        } else {
            this.fires.remove(cell);
        }
    }

    /** Counts a plant grown in it, or gone. */
    void plant(final boolean grown) {
        this.plants = Math.max(0, this.plants + (grown ? 1 : -1));
    }

    /**
     * Counts a second it stood open with its air the place's, or starts counting again; how many seconds it has.
     */
    int settleOpen(final boolean settled) {
        this.settledOpen = this.breached && settled ? this.settledOpen + 1 : 0;
        return this.settledOpen;
    }

    /** Notes whether its air can be breathed this second; whether it could not the second before. */
    boolean filledSince(final boolean breathable) {
        final boolean filled = breathable && !this.breathableBefore;
        this.breathableBefore = breathable;
        return filled;
    }

    /** Notes its pressure after a second, and how hard its air rushes out through its holes. */
    void tickPressure(final double jet) {
        final double now = pressure();
        this.change = this.lastPressure < 0 ? 0 : now - this.lastPressure;
        this.lastPressure = now;
        this.jet = jet;
    }

    /** Takes every gas of {@code other} into it. */
    void absorb(final Room other) {
        for (final Map.Entry<ResourceLocation, Double> gas : other.gases().entrySet()) {
            add(gas.getKey(), gas.getValue());
        }
        this.flooded.addAll(other.flooded);
    }

    /** Keeps it with the world. */
    public CompoundTag save() {
        final CompoundTag tag = new CompoundTag();
        tag.putLong(ID, this.id);
        tag.put(CELLS, new LongArrayTag(this.cells.toLongArray()));
        final CompoundTag gases = new CompoundTag();
        for (final Map.Entry<ResourceLocation, Double> gas : gases().entrySet()) {
            gases.putDouble(gas.getKey().toString(), gas.getValue());
        }
        tag.put(MOLES, gases);
        tag.put(FLOODED, new LongArrayTag(this.flooded.toLongArray()));
        tag.putBoolean(BREACHED, this.breached);
        return tag;
    }

    /** A room as it was kept; its bounds are worked out again once it is loaded. */
    public static Room load(final CompoundTag tag) {
        final Room room = new Room(tag.getLong(ID));
        for (final long cell : tag.getLongArray(CELLS)) {
            room.cells.add(cell);
        }
        final CompoundTag gases = tag.getCompound(MOLES);
        for (final String key : gases.getAllKeys()) {
            final ResourceLocation gas = ResourceLocation.tryParse(key);
            if (gas != null && gases.contains(key, Tag.TAG_DOUBLE)) {
                room.add(gas, gases.getDouble(key));
            }
        }
        for (final long cell : tag.getLongArray(FLOODED)) {
            room.flooded.add(cell);
        }
        room.breached = tag.getBoolean(BREACHED);
        room.bounds = boundsOf(room.cells);
        return room;
    }

    private static AABB boundsOf(final LongSet cells) {
        if (cells.isEmpty()) {
            return new AABB(BlockPos.ZERO);
        }
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        final LongIterator each = cells.iterator();
        while (each.hasNext()) {
            final long cell = each.nextLong();
            final int x = BlockPos.getX(cell);
            final int y = BlockPos.getY(cell);
            final int z = BlockPos.getZ(cell);
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);
        }
        return new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
    }
}
