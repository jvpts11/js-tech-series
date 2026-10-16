/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import dev.jstech.core.blockentity.IFieldPart;
import dev.jstech.core.multiblock.BlockMatch;
import dev.jstech.core.multiblock.IBlockMatcher;
import dev.jstech.core.multiblock.IStructureShape;
import dev.jstech.core.multiblock.IStructureWatcher;
import dev.jstech.core.multiblock.LevelStructures;
import dev.jstech.core.multiblock.Limit;
import dev.jstech.core.multiblock.MultiblockPatterns;
import dev.jstech.core.multiblock.MultiblockPorts;
import dev.jstech.core.multiblock.PatternMatcher;
import dev.jstech.core.multiblock.ShapeMatch;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.ToIntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * A machine that is a multiblock's controller: its shape, drawn or by rule, and whether the world holds it whole.
 * Until it does the machine stands {@link MachineStatus#NOT_FORMED}, and once it does its parts are bound to it, its
 * ports pass to it, and it knows its size, its counts, its numbers and how many recipes it runs at once.
 *
 * <p>It is checked again only when a block changes in the box it watches (the farthest its shape reaches, or, once
 * formed, its structure and the places beside it), a few ticks after, and once a minute besides; never every tick.
 * It is never checked while a chunk it reaches is not loaded, and a structure is never undone because a chunk went:
 * it stays as it was until it can be read again.
 *
 * <pre>{@code
 * formsWith(MachineStructure.of(MyContent.TANK).parallel(formed -> formed.count("unit")));   // in the constructor
 * }</pre>
 */
@TextHolder
public final class MachineStructure implements IFieldPart, IStructureWatcher {

    private final IStructureShape shape;
    private ToIntFunction<ShapeMatch.Formed> parallelBy = formed -> 1;
    private @Nullable MachineBlockEntity machine;
    private @Nullable Runnable synced;
    private boolean formed;
    private List<Long> places = List.of();
    private Set<Long> held = Set.of();
    private int[] size = {0, 0, 0};
    private int repeats;
    private int parallel;
    private @Nullable ShapeMatch.Formed last;
    private @Nullable ShapeMatch.Failure failure;
    private boolean dirty = true;
    private boolean filed;
    private long notBefore;
    private long recheckAt;

    public static final TextKey FORMED = TextKey.of("jscore.multiblock.formed", "Formed");
    public static final TextKey FORMED_SIZE = TextKey.of("jscore.multiblock.formed_size", "Formed: %s by %s by %s");
    public static final TextKey PARALLEL = TextKey.of("jscore.multiblock.parallel", "Runs %s at once");
    public static final TextKey NEEDS_BLOCK = TextKey.of("jscore.multiblock.needs_block",
            "Needs %s at %s, %s, %s from the controller; found %s");
    public static final TextKey NEEDS_COUNT = TextKey.of("jscore.multiblock.needs_count",
            "Needs from %s to %s of %s; has %s");
    public static final TextKey NEEDS_AT_LEAST = TextKey.of("jscore.multiblock.needs_at_least",
            "Needs at least %s of %s; has %s");
    public static final TextKey WRONG_SIZE = TextKey.of("jscore.multiblock.wrong_size", "Not a size it may be: %s %s");
    public static final TextKey OR_ANOTHER = TextKey.of("jscore.multiblock.or_another", "%s or another");
    public static final TextKey ALONG_X = TextKey.of("jscore.multiblock.along_x", "blocks along x");
    public static final TextKey HIGH = TextKey.of("jscore.multiblock.high", "blocks high");
    public static final TextKey ALONG_Z = TextKey.of("jscore.multiblock.along_z", "blocks along z");
    public static final TextKey OF_FLOOR = TextKey.of("jscore.multiblock.of_floor", "places of floor");
    public static final TextKey PART_FRAME = TextKey.of("jscore.multiblock.part.frame", "a frame block");
    public static final TextKey PART_FACE = TextKey.of("jscore.multiblock.part.face", "a face block");
    public static final TextKey PART_INSIDE = TextKey.of("jscore.multiblock.part.inside", "what may stand inside");
    public static final TextKey PART_FLOOR = TextKey.of("jscore.multiblock.part.floor", "a floor block");
    public static final TextKey PART_WALL = TextKey.of("jscore.multiblock.part.wall", "a wall block");
    public static final TextKey PART_UNIT = TextKey.of("jscore.multiblock.part.unit", "a unit");
    /** How often a formed structure is looked at again besides, in ticks, should a change have gone unheard. */
    public static final int RECHECK_EVERY = 1_200;
    /* How long after a change it is checked, so a player building it is not checked each block they place. */
    private static final int SETTLE = 5;
    /* How long it waits for the chunks it reaches to load before it looks again. */
    private static final int LOADED_EVERY = 20;
    private static final String KEY = "Structure";
    private static final String FORMED_KEY = "Formed";
    private static final String PLACES_KEY = "Places";
    private static final String SIZE_KEY = "Size";
    private static final String REPEATS_KEY = "Repeats";
    private static final String PARALLEL_KEY = "Parallel";
    private static final int AXES = 3;

    private MachineStructure(final IStructureShape shape) {
        this.shape = Objects.requireNonNull(shape, "shape");
    }

    /** A controller of {@code shape}, running one recipe at a time when formed. */
    public static MachineStructure of(final IStructureShape shape) {
        return new MachineStructure(shape);
    }

    /**
     * The same, running as many recipes at once as {@code by} says of its formed structure: by its size
     * ({@code formed.repeats()}), by its parts ({@code formed.count("unit")}), by what is inside it.
     */
    public MachineStructure parallel(final ToIntFunction<ShapeMatch.Formed> by) {
        this.parallelBy = Objects.requireNonNull(by, "by");
        return this;
    }

    /** Its shape. */
    public IStructureShape shape() {
        return this.shape;
    }

    /** Whether its structure stands whole: as last checked, or as saved until it is checked after loading. */
    public boolean formed() {
        return this.formed;
    }

    /** All its last check found, once it has been checked since it loaded and found formed. */
    public Optional<ShapeMatch.Formed> structure() {
        return Optional.ofNullable(this.last);
    }

    /** What its last check found wrong, when it is not formed. */
    public Optional<ShapeMatch.Failure> failure() {
        return Optional.ofNullable(this.failure);
    }

    /** How many recipes it runs at once: none while it is not formed. */
    public int parallel() {
        return this.formed ? this.parallel : 0;
    }

    /** Its size along the world's x, y and z, none while it is not formed. */
    public int[] size() {
        return this.size.clone();
    }

    /** The times its pattern's repeated slice stands; none while it is not formed. */
    public int repeats() {
        return this.repeats;
    }

    @Override
    public void changed() {
        this.dirty = true;
        if (this.machine != null) {
            this.machine.wake();
        }
    }

    @Override
    public boolean holds(final long pos) {
        return this.formed && this.held.contains(pos);
    }

    @Override
    public void save(final CompoundTag tag, final HolderLookup.Provider registries) {
        final CompoundTag own = new CompoundTag();
        own.putBoolean(FORMED_KEY, this.formed);
        own.putLongArray(PLACES_KEY, this.places.stream().mapToLong(Long::longValue).toArray());
        own.putIntArray(SIZE_KEY, this.size);
        own.putInt(REPEATS_KEY, this.repeats);
        own.putInt(PARALLEL_KEY, this.parallel);
        tag.put(KEY, own);
    }

    @Override
    public void load(final CompoundTag tag, final HolderLookup.Provider registries) {
        final CompoundTag own = tag.getCompound(KEY);
        this.formed = own.getBoolean(FORMED_KEY);
        final List<Long> loaded = new ArrayList<>();
        for (final long place : own.getLongArray(PLACES_KEY)) {
            loaded.add(place);
        }
        this.places = List.copyOf(loaded);
        this.held = new HashSet<>(loaded);
        final int[] saved = own.getIntArray(SIZE_KEY);
        this.size = saved.length == AXES ? saved : new int[] {0, 0, 0};
        this.repeats = own.getInt(REPEATS_KEY);
        this.parallel = own.getInt(PARALLEL_KEY);
        this.dirty = true;
    }

    /* The machine it is declared on and how it tells its field it changed, from the machine's constructor. */
    void on(final MachineBlockEntity owner, final Runnable changed) {
        this.machine = owner;
        this.synced = changed;
    }

    /*
     * One server tick: files it with the level's structures the first time, and checks it when a change it watches
     * has settled, or once a minute, with every chunk it reaches loaded.
     */
    void tick(final ServerLevel level, final BlockPos pos) {
        final long now = level.getGameTime();
        if (!this.filed) {
            file(level, pos);
            this.dirty = true;
        }
        if (now < this.notBefore || !this.dirty && now < this.recheckAt) {
            return;
        }
        final int reach = this.shape.reach();
        if (!level.hasChunksAt(pos.offset(-reach, 0, -reach), pos.offset(reach, 0, reach))) {
            this.notBefore = now + LOADED_EVERY;
            return;
        }
        this.dirty = false;
        this.notBefore = now + SETTLE;
        this.recheckAt = now + RECHECK_EVERY;
        apply(level, pos, this.shape.check(MultiblockPatterns.provider(level),
                PatternMatcher.encodePosition(pos.getX(), pos.getY(), pos.getZ())));
    }

    /* The machine left the level, broken or its chunk unloaded: it watches nothing more. */
    void removed(final ServerLevel level, final BlockPos pos) {
        LevelStructures.of(level).forget(pos);
        this.filed = false;
    }

    /* Its lines for whoever looks at it: formed, with its size when details are asked for, or what is wrong. */
    void describe(final List<Text> lines, final boolean details) {
        if (this.formed) {
            lines.add(details ? FORMED_SIZE.with(this.size[0], this.size[1], this.size[2]) : FORMED.text());
            if (this.parallel > 1) {
                lines.add(PARALLEL.with(this.parallel));
            }
            return;
        }
        final ShapeMatch.Failure wrong = this.failure;
        if (wrong == null) {
            return;
        }
        lines.add(switch (wrong.wrong()) {
            case BLOCK -> NEEDS_BLOCK.with(wanted(wrong.wanted()), wrong.relX(), wrong.relY(), wrong.relZ(),
                    blockName(wrong.found()));
            case COUNT -> counted(wrong);
            case SIZE -> WRONG_SIZE.with(wrong.found(), measure(wrong.wanted()));
        });
    }

    private void apply(final ServerLevel level, final BlockPos pos, final ShapeMatch result) {
        switch (result) {
            case ShapeMatch.Formed found -> {
                final List<Long> now = new ArrayList<>(found.parts());
                now.addAll(found.inside());
                final Set<Long> nowHeld = new HashSet<>(now);
                final List<Long> gone = new ArrayList<>();
                for (final long place : this.places) {
                    if (!nowHeld.contains(place)) {
                        gone.add(place);
                    }
                }
                MultiblockPorts.release(level, gone);
                MultiblockPorts.stamp(level, pos, found);
                this.formed = true;
                this.last = found;
                this.failure = null;
                this.places = List.copyOf(now);
                this.held = nowHeld;
                this.size = new int[] {found.sizeX(), found.sizeY(), found.sizeZ()};
                this.repeats = found.repeats();
                this.parallel = Math.max(0, this.parallelBy.applyAsInt(found));
            }
            case ShapeMatch.Failure wrong -> {
                if (this.formed) {
                    MultiblockPorts.release(level, this.places);
                }
                this.formed = false;
                this.last = null;
                this.failure = wrong;
                this.places = List.of();
                this.held = Set.of();
                this.size = new int[] {0, 0, 0};
                this.repeats = 0;
                this.parallel = 0;
            }
        }
        file(level, pos);
        if (this.synced != null) {
            this.synced.run();
        }
        if (this.machine != null) {
            this.machine.wake();
        }
    }

    /* Files it with the level's structures: its formed structure and the places beside it, or all it reaches. */
    private void file(final ServerLevel level, final BlockPos pos) {
        final BlockPos min;
        final BlockPos max;
        if (this.formed && !this.places.isEmpty()) {
            final int[] lo = {pos.getX(), pos.getY(), pos.getZ()};
            final int[] hi = lo.clone();
            for (final long place : this.places) {
                final int[] xyz = PatternMatcher.decodePosition(place);
                for (int axis = 0; axis < AXES; axis++) {
                    lo[axis] = Math.min(lo[axis], xyz[axis]);
                    hi[axis] = Math.max(hi[axis], xyz[axis]);
                }
            }
            min = new BlockPos(lo[0] - 1, lo[1] - 1, lo[2] - 1);
            max = new BlockPos(hi[0] + 1, hi[1] + 1, hi[2] + 1);
        } else {
            final int reach = this.shape.reach();
            min = pos.offset(-reach, -reach, -reach);
            max = pos.offset(reach, reach, reach);
        }
        LevelStructures.of(level).watch(pos, min, max, this);
        this.filed = true;
    }

    /* What goes where a failure says, as the player is told it: a block's name, a tag, or the part of the shape. */
    private Text wanted(final String wanted) {
        final IBlockMatcher fits = this.shape.wanted(wanted);
        if (fits instanceof BlockMatch match) {
            final Text first = match.blocks().isEmpty() ? Text.literal("#" + match.tags().get(0))
                    : blockName(match.blocks().get(0));
            return match.blocks().size() + match.tags().size() > 1 ? OR_ANOTHER.with(first) : first;
        }
        return switch (wanted) {
            case ShapeMatch.FRAME -> PART_FRAME.text();
            case ShapeMatch.FACE -> PART_FACE.text();
            case ShapeMatch.INSIDE -> PART_INSIDE.text();
            case ShapeMatch.FLOOR -> PART_FLOOR.text();
            case ShapeMatch.WALL -> PART_WALL.text();
            case ShapeMatch.UNIT -> PART_UNIT.text();
            default -> Text.literal(wanted);
        };
    }

    private Text counted(final ShapeMatch.Failure wrong) {
        final Limit limit = this.shape.limits().get(wrong.wanted());
        if (limit == null || limit.most() == Integer.MAX_VALUE) {
            return NEEDS_AT_LEAST.with(limit == null ? 0 : limit.least(), wrong.wanted(), wrong.found());
        }
        return NEEDS_COUNT.with(limit.least(), limit.most(), wrong.wanted(), wrong.found());
    }

    private static Text measure(final String which) {
        return switch (which) {
            case ShapeMatch.SIZE_X -> ALONG_X.text();
            case ShapeMatch.SIZE_Y -> HIGH.text();
            case ShapeMatch.SIZE_Z -> ALONG_Z.text();
            case ShapeMatch.AREA -> OF_FLOOR.text();
            default -> Text.literal(which);
        };
    }

    /* A block's name in the player's language, by its id; the id itself for one the game does not know. */
    private static Text blockName(final String id) {
        final ResourceLocation key = ResourceLocation.tryParse(id);
        if (key == null || !BuiltInRegistries.BLOCK.containsKey(key)) {
            return Text.literal(id);
        }
        return GameText.of(BuiltInRegistries.BLOCK.get(key).getName());
    }
}
