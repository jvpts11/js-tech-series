/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock.damage;

import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructureEvent;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.multiblock.FormedPartBlock;
import dev.jstech.core.multiblock.FormedPartBlockEntity;
import dev.jstech.core.multiblock.FormedParts;
import dev.jstech.core.multiblock.shape.CellSpace;
import dev.jstech.core.multiblock.shape.FormedPlace;
import dev.jstech.core.multiblock.shape.FormedShape;
import dev.jstech.core.multiblock.shape.FormedShapes;
import dev.jstech.core.multiblock.shape.MeshCells;
import dev.jstech.core.multiblock.shape.MeshOutline;
import dev.jstech.core.multiblock.shape.MeshVoxels;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * Damage to formed models, on the ground and aboard a moving structure alike. A strike is laid where it struck
 * ({@link #strike}): the point and the way the surface faced there are taken into the model's own space, and every
 * block of the model the stamp reaches keeps the record of it ({@link CellMark}), saved with the block, carried with
 * it aboard a moving structure and sent to the players who see it. From those records the server and every player's
 * game work out each reached block's faces as the strike left them, from the model's own file, and the block collides,
 * is aimed at and is outlined by them; the player's game draws the whole model so. Taking the records away
 * ({@link #erase}) undoes all of it.
 *
 * <pre>{@code
 * // A meteoroid holes the hull where it hit: a stamp of the Core's, a tenth of a block across.
 * FormedDamage.strike(level, hit.getBlockPos(), DamageStamps.MICROMETEOROID, hit.getLocation(),
 *         Vec3.atLowerCornerOf(hit.getDirection().getNormal()), 0.1, level.random.nextLong());
 * }</pre>
 */
public final class FormedDamage {

    /* Counts each change of a block's damage a player's game has been told of, so what is drawn from it is redone. */
    private static final AtomicInteger CHANGES = new AtomicInteger();
    /* How many sets of marks are kept worked out, the most recently used. */
    private static final int KEPT = 256;
    private static final Map<Key, Result> DAMAGED = Collections.synchronizedMap(
            new LinkedHashMap<>(KEPT, 0.75F, true) {
                @Override
                protected boolean removeEldestEntry(final Map.Entry<Key, Result> eldest) {
                    return size() > KEPT;
                }
            });
    /* The marks of a model in one order however they were gathered, so the same marks make the same damage. */
    private static final Comparator<DamageMark> ORDER = Comparator.comparingLong(DamageMark::seed)
            .thenComparing(mark -> mark.stamp().toString()).thenComparingDouble(DamageMark::x)
            .thenComparingDouble(DamageMark::y).thenComparingDouble(DamageMark::z)
            .thenComparingDouble(DamageMark::size);
    private static final int AXES = 3;
    private static final double HALF = 0.5;
    /* The most strikes a model keeps that lose no block, so a long rough ride does not pile them up. */
    private static final int MOST_MARKS = 32;
    /*
     * A lost block, as nothing else is blamed: struck from outside, the block's own faces gone with it and its stamp
     * tearing the edges round it; and one an explosion breaks, torn wider.
     */
    private static final double LOST_SIZE = 0.8;
    private static final double BLOWN_SIZE = 1.0;
    /* How many ticks a blame waits for the block it names to be lost, and how many a world keeps before sweeping. */
    private static final int BLAMED_FOR = 2;
    private static final int STALE_AFTER = 256;
    /*
     * A moving structure's blow: from how fast it dents, and folds as a crash does, in metres a second; how big the
     * dent is at the least and the most, in blocks, and how much it grows with each metre a second over the least.
     */
    private static final double DENTING_SPEED = 4;
    private static final double FOLDING_SPEED = 10;
    private static final double DENT_LEAST = 0.3;
    private static final double DENT_MOST = 1.2;
    private static final double DENT_PER_SPEED = 0.04;
    /* What each world was told of the blocks about to be lost, by place. */
    private static final Map<Level, Map<Long, Blame>> BLAMED = Collections.synchronizedMap(new WeakHashMap<>());

    private FormedDamage() {
    }

    /**
     * Lays the stamp {@code stamp} on the formed model the block at {@code pos} is drawn with, as it struck there: at
     * {@code at} in the world, the surface facing {@code facing}, {@code size} blocks across, by {@code seed}. On the
     * server; nothing on a player's game, nor for a block of no formed model or a stamp not declared.
     *
     * @return how many of the model's blocks the stamp reached, each now keeping it
     */
    public static int strike(final Level level, final BlockPos pos, final ResourceLocation stamp, final Vec3 at,
                             final Vec3 facing, final double size, final long seed) {
        final Struck struck = struck(level, pos);
        if (struck == null || DamageStamps.get(stamp).isEmpty() || !(size > 0) || facing.lengthSqr() == 0
                || marks(level, pos, struck.place()).size() >= MOST_MARKS) {
            return 0;
        }
        final FormedPlace place = struck.place();
        final double[] local = new double[AXES];
        CellSpace.toModel(new double[] {at.x - pos.getX(), at.y - pos.getY(), at.z - pos.getZ()}, place.turn(),
                local);
        final double[] way = new double[AXES];
        CellSpace.toModel(new double[] {HALF + facing.x, HALF + facing.y, HALF + facing.z}, place.turn(), way);
        final double[] origin = struck.shape().origin();
        final DamageMark mark = new DamageMark(stamp, origin[0] + place.x() + local[0],
                origin[1] + place.y() + local[1], origin[2] + place.z() + local[2], (float) (way[0] - HALF),
                (float) (way[1] - HALF), (float) (way[2] - HALF), (float) size, seed, Optional.empty());
        return lay(level, pos, struck, mark);
    }

    /**
     * Tells what is about to break the merged block at {@code pos}, so its loss tears the hole of {@code stamp},
     * {@code size} blocks across, rather than a plain impact's: as an explosion tells the blocks it is breaking. Heard
     * only by a loss within the next two ticks; on the server.
     */
    public static void blame(final Level level, final BlockPos pos, final ResourceLocation stamp, final double size) {
        if (level.isClientSide()) {
            return;
        }
        final Map<Long, Blame> told = BLAMED.computeIfAbsent(level, any -> new HashMap<>());
        if (told.size() > STALE_AFTER) {
            // Blames of blocks that were not lost after all go, now and then.
            told.values().removeIf(old -> level.getGameTime() - old.at() > BLAMED_FOR);
        }
        told.put(pos.asLong(), new Blame(stamp, size, level.getGameTime()));
    }

    /**
     * The merged block at {@code pos}, {@code part}, is lost, broken or blown away: its place leaves its model and
     * its shape, and the hole is torn round it, in the blocks beside it, by what was blamed for it, a plain impact
     * otherwise, a little wider than the block. Told by the block as it goes, on the server; a block carried with its
     * structure, or given back as its structure comes apart, is not lost.
     */
    @ApiStatus.Internal
    public static void lose(final Level level, final BlockPos pos, final FormedPartBlockEntity part) {
        final Struck struck = struck(level, pos);
        if (struck == null) {
            return;
        }
        final FormedPlace place = struck.place();
        final double[] origin = struck.shape().origin();
        final double[] middle = middle(struck.shape());
        final double x = origin[0] + place.x() + HALF;
        final double y = origin[1] + place.y() + HALF;
        final double z = origin[2] + place.z() + HALF;
        // Out of the model, from its middle through the lost block: the way its hull faced there.
        double[] out = {x - middle[0], y - middle[1], z - middle[2]};
        if (out[0] * out[0] + out[1] * out[1] + out[2] * out[2] < 1.0e-6) {
            out = new double[] {0, 1, 0};
        }
        final Blame blame = blamed(level, pos);
        final DamageMark mark = new DamageMark(blame.stamp(), x, y, z, (float) out[0], (float) out[1], (float) out[2],
                (float) blame.size(), pos.asLong() ^ level.getGameTime(), Optional.of(part.original()));
        lay(level, pos, struck, mark);
    }

    /**
     * Puts the block at {@code pos} back into the formed model it was lost from, when it is a block of the kind lost
     * there: it merges again, as the block of the model it was, and the loss is taken away from every block of the
     * model, which stands mended there. On the server; whether it did.
     */
    public static boolean mend(final Level level, final BlockPos pos) {
        final BlockState here = level.getBlockState(pos);
        if (!(level instanceof ServerLevel server) || here.getBlock() instanceof FormedPartBlock) {
            return false;
        }
        for (final Direction way : Direction.values()) {
            final BlockPos beside = pos.relative(way);
            if (!(level.getBlockEntity(beside) instanceof FormedPartBlockEntity part)) {
                continue;
            }
            for (final CellMark kept : part.damage()) {
                final Optional<BlockState> was = kept.mark().lost();
                final FormedPlace from = placeOf(part, kept.model());
                if (was.isEmpty() || was.get().getBlock() != here.getBlock() || from == null) {
                    continue;
                }
                final Optional<FormedShape> shape = FormedShapes.get(level, kept.model());
                if (shape.isEmpty()) {
                    continue;
                }
                final double[] origin = shape.get().origin();
                final DamageMark mark = kept.mark();
                final long cell = MeshCells.key((int) Math.floor(mark.x() - origin[0]),
                        (int) Math.floor(mark.y() - origin[1]), (int) Math.floor(mark.z() - origin[2]));
                if (!blockOf(beside, from, cell).equals(pos)) {
                    continue;
                }
                final FormedPlace back = new FormedPlace(kept.model(), from.orientation(), MeshCells.x(cell),
                        MeshCells.y(cell), MeshCells.z(cell));
                final BlockPos owner = part.terminal() == null ? beside : part.terminal();
                if (!FormedParts.lay(server, owner, pos, back, List.of())) {
                    return false;
                }
                for (final long key : shape.get().cells().keySet()) {
                    if (level.getBlockEntity(blockOf(pos, back, key)) instanceof FormedPartBlockEntity mended) {
                        mended.forget(mark);
                    }
                }
                return true;
            }
        }
        return false;
    }

    /** How many of its blocks the formed model the block at {@code pos} is drawn as {@code place} of has lost. */
    public static int lost(final Level level, final BlockPos pos, final FormedPlace place) {
        return (int) marks(level, pos, place).stream().filter(mark -> mark.lost().isPresent()).count();
    }

    /** Listens for what breaks and dents formed models: explosions, and blows of moving structures. */
    @ApiStatus.Internal
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(ExplosionEvent.Detonate.class, FormedDamage::onExplosion);
        NeoForge.EVENT_BUS.addListener(MovingStructureEvent.Collision.class, FormedDamage::onCollision);
        // Last, so a placing that something else refuses is never merged.
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, BlockEvent.EntityPlaceEvent.class, event -> {
            if (event.getLevel() instanceof Level level) {
                mend(level, event.getPos());
            }
        });
    }

    /**
     * Takes every strike away from the formed model the block at {@code pos} is drawn with, on the server: its blocks
     * stand as the model has them again.
     *
     * @return how many of its blocks had damage
     */
    public static int erase(final Level level, final BlockPos pos) {
        final Struck struck = struck(level, pos);
        if (struck == null) {
            return 0;
        }
        int erased = 0;
        for (final long key : struck.shape().cells().keySet()) {
            if (level.getBlockEntity(blockOf(pos, struck.place(), key)) instanceof FormedPartBlockEntity reached
                    && reached.unmark(struck.place().model())) {
                erased++;
            }
        }
        return erased;
    }

    /**
     * Every strike laid on the formed model the block at {@code pos} is drawn as {@code place} of, gathered from all
     * its blocks, in one order however they are gathered.
     */
    public static List<DamageMark> marks(final Level level, final BlockPos pos, final FormedPlace place) {
        final Optional<FormedShape> shape = FormedShapes.get(level, place.model());
        if (shape.isEmpty()) {
            return List.of();
        }
        final Set<DamageMark> found = new LinkedHashSet<>();
        for (final long key : shape.get().cells().keySet()) {
            if (level.getBlockEntity(blockOf(pos, place, key)) instanceof FormedPartBlockEntity part) {
                for (final CellMark kept : part.damage()) {
                    if (kept.model().equals(place.model()) && MeshCells.key(kept.x(), kept.y(), kept.z()) == key) {
                        found.add(kept.mark());
                    }
                }
            }
        }
        return ordered(found);
    }

    /** The strikes of {@link #marks}, those whose stamps are declared. */
    public static List<Strike> strikes(final Level level, final BlockPos pos, final FormedPlace place) {
        return marks(level, pos, place).stream().map(DamageMark::strike).flatMap(Optional::stream).toList();
    }

    /**
     * The boxes of the block {@code place} as the strikes it keeps, {@code kept}, left its faces, in sixteenths as the
     * model stands facing north; none while it keeps none, or its model does not read.
     */
    public static Optional<int[]> boxes(final Level level, final FormedPlace place, final List<CellMark> kept) {
        final Result result = result(level, place, kept);
        if (result == null) {
            return Optional.empty();
        }
        final float[] faces = result.faces.get(place.cell());
        return faces == null ? Optional.empty()
                : Optional.of(result.boxes.computeIfAbsent(place.cell(), cell -> MeshVoxels.boxes(faces)).clone());
    }

    /**
     * The faces of the merged block at {@code pos}, as the block {@code place}, as the strikes it keeps left them, in
     * its own space, nine numbers to a triangle; none while it keeps none.
     */
    public static Optional<float[]> triangles(final Level level, final BlockPos pos, final FormedPlace place) {
        if (!(level.getBlockEntity(pos) instanceof FormedPartBlockEntity part)) {
            return Optional.empty();
        }
        final Result result = result(level, place, part.damage());
        return result == null ? Optional.empty() : Optional.ofNullable(result.faces.get(place.cell()));
    }

    /**
     * The lines of the outline of the merged block at {@code pos}, as the block {@code place}, as the strikes it keeps
     * left its faces, in its own space, six numbers to a line; none while it keeps none.
     */
    public static Optional<float[]> lines(final Level level, final BlockPos pos, final FormedPlace place) {
        if (!(level.getBlockEntity(pos) instanceof FormedPartBlockEntity part)) {
            return Optional.empty();
        }
        final Result result = result(level, place, part.damage());
        if (result == null) {
            return Optional.empty();
        }
        final float[] faces = result.faces.get(place.cell());
        return faces == null ? Optional.empty()
                : Optional.of(result.lines.computeIfAbsent(place.cell(), cell -> MeshOutline.lines(faces)));
    }

    /** How many times a player's game has been told of a block's damage changing: what is drawn from it is redone. */
    public static int changes() {
        return CHANGES.get();
    }

    /** Tells that a block's damage changed on a player's game. */
    @ApiStatus.Internal
    public static void changed() {
        CHANGES.incrementAndGet();
    }

    /* Gives the record {@code mark} to every block of the struck model it reaches but a lost one; how many. */
    private static int lay(final Level level, final BlockPos pos, final Struck struck, final DamageMark mark) {
        final Optional<Strike> strike = mark.strike();
        if (strike.isEmpty()) {
            return 0;
        }
        final FormedPlace place = struck.place();
        final double[] origin = struck.shape().origin();
        int marked = 0;
        for (final Map.Entry<Long, float[]> cell : struck.rest().entrySet()) {
            final long key = cell.getKey();
            // A lost block keeps nothing: it is gone, and its blocks beside it keep its loss.
            if (mark.lost().isPresent() && key == place.cell()) {
                continue;
            }
            if (reaches(strike.get(), cell.getValue(), corner(origin, key))
                    && level.getBlockEntity(blockOf(pos, place, key)) instanceof FormedPartBlockEntity reached
                    && holds(reached, place.model(), key)) {
                reached.mark(new CellMark(place.model(), MeshCells.x(key), MeshCells.y(key), MeshCells.z(key),
                        mark));
                marked++;
            }
        }
        return marked;
    }

    /* An explosion breaking merged blocks blames it: from inside their model, or from outside it. */
    private static void onExplosion(final ExplosionEvent.Detonate event) {
        final Level level = event.getLevel();
        if (level.isClientSide()) {
            return;
        }
        final Vec3 blast = event.getExplosion().center();
        for (final BlockPos pos : event.getAffectedBlocks()) {
            final Struck struck = level.getBlockEntity(pos) instanceof FormedPartBlockEntity ? struck(level, pos)
                    : null;
            if (struck == null) {
                continue;
            }
            // From inside when the blast is nearer the model's middle than the block is.
            final Vec3 middle = middleInWorld(pos, struck);
            final boolean inside = blast.distanceToSqr(middle) < Vec3.atCenterOf(pos).distanceToSqr(middle);
            blame(level, pos, inside ? DamageStamps.EXPLOSION : DamageStamps.IMPACT, BLOWN_SIZE);
        }
    }

    /*
     * A moving structure struck something hard: the merged block of its own nearest where they met is dented there,
     * with folds past a crash's speed; a structure settling or touching down gently is not.
     */
    private static void onCollision(final MovingStructureEvent.Collision event) {
        final MovingStructure structure = event.structure();
        if (!(structure.level() instanceof ServerLevel level) || event.speed() < DENTING_SPEED) {
            return;
        }
        final Vec3 yard = MovingStructures.toYard(structure, event.at());
        final BlockPos centre = BlockPos.containing(yard);
        BlockPos nearest = null;
        double best = Double.MAX_VALUE;
        for (final BlockPos at : BlockPos.betweenClosed(centre.offset(-1, -1, -1), centre.offset(1, 1, 1))) {
            final double away = Vec3.atCenterOf(at).distanceToSqr(yard);
            if (away < best && level.getBlockEntity(at) instanceof FormedPartBlockEntity part
                    && !part.places().isEmpty()) {
                best = away;
                nearest = at.immutable();
            }
        }
        if (nearest == null) {
            return;
        }
        final Vec3 out = yard.subtract(Vec3.atCenterOf(nearest));
        final double size = Math.clamp(DENT_LEAST + (event.speed() - DENTING_SPEED) * DENT_PER_SPEED, DENT_LEAST,
                DENT_MOST);
        strike(level, nearest, event.speed() >= FOLDING_SPEED ? DamageStamps.COLLISION : DamageStamps.DENT, yard,
                out.lengthSqr() < 1.0e-6 ? new Vec3(0, 1, 0) : out, size, level.getGameTime() ^ nearest.asLong());
    }

    /* What was blamed for losing the block at {@code pos} just now, taken; a plain impact for nothing. */
    private static Blame blamed(final Level level, final BlockPos pos) {
        final Map<Long, Blame> told = BLAMED.get(level);
        final Blame blame = told == null ? null : told.remove(pos.asLong());
        if (blame != null && level.getGameTime() - blame.at() <= BLAMED_FOR
                && DamageStamps.get(blame.stamp()).isPresent()) {
            return blame;
        }
        return new Blame(DamageStamps.IMPACT, LOST_SIZE, level.getGameTime());
    }

    /* The block of the model {@code model} {@code part} is; none for one it is not a block of. */
    private static @Nullable FormedPlace placeOf(final FormedPartBlockEntity part, final ResourceLocation model) {
        for (final FormedPlace place : part.places()) {
            if (place.model().equals(model)) {
                return place;
            }
        }
        return null;
    }

    /* The middle of a model's blocks, in its space. */
    private static double[] middle(final FormedShape shape) {
        final double[] origin = shape.origin();
        final double[] sum = new double[AXES];
        int count = 0;
        final List<Long> keys = new ArrayList<>(shape.cells().keySet());
        keys.addAll(shape.apart());
        for (final long key : keys) {
            sum[0] += MeshCells.x(key);
            sum[1] += MeshCells.y(key);
            sum[2] += MeshCells.z(key);
            count++;
        }
        if (count == 0) {
            return origin.clone();
        }
        return new double[] {origin[0] + sum[0] / count + HALF, origin[1] + sum[1] / count + HALF,
                origin[2] + sum[2] / count + HALF};
    }

    /* Where the middle of the struck block's model is in the world. */
    private static Vec3 middleInWorld(final BlockPos pos, final Struck struck) {
        final double[] middle = middle(struck.shape());
        final double[] origin = struck.shape().origin();
        final FormedPlace place = struck.place();
        final double[] local = {middle[0] - origin[0] - place.x(), middle[1] - origin[1] - place.y(),
                middle[2] - origin[2] - place.z()};
        final double[] world = new double[AXES];
        CellSpace.toWorld(local, place.turn(), world);
        return new Vec3(pos.getX() + world[0], pos.getY() + world[1], pos.getZ() + world[2]);
    }

    /* The strikes {@code kept} by a block laid on the block {@code place}, worked out; none for none. */
    private static @Nullable Result result(final Level level, final FormedPlace place, final List<CellMark> kept) {
        final List<DamageMark> marks = new ArrayList<>();
        for (final CellMark mark : kept) {
            if (mark.on(place)) {
                marks.add(mark.mark());
            }
        }
        if (marks.isEmpty()) {
            return null;
        }
        final Optional<FormedShape> shape = FormedShapes.get(level, place.model());
        if (shape.isEmpty()) {
            return null;
        }
        final Optional<Map<Long, float[]>> rest = FormedFaces.of(place.model(), shape.get());
        if (rest.isEmpty()) {
            return null;
        }
        final Key key = new Key(place.model(), shape.get(), ordered(marks));
        final Result known = DAMAGED.get(key);
        if (known != null) {
            return known;
        }
        final Result made = deform(rest.get(), shape.get().origin(), key.marks());
        DAMAGED.put(key, made);
        return made;
    }

    /* The faces of each block {@code marks} reach, as they left them, each in its block's own space. */
    private static Result deform(final Map<Long, float[]> rest, final double[] origin, final List<DamageMark> marks) {
        final List<Strike> strikes = marks.stream().map(DamageMark::strike).flatMap(Optional::stream).toList();
        final List<Long> reached = new ArrayList<>();
        int numbers = 0;
        for (final Map.Entry<Long, float[]> cell : rest.entrySet()) {
            for (final Strike strike : strikes) {
                if (reaches(strike, cell.getValue(), corner(origin, cell.getKey()))) {
                    reached.add(cell.getKey());
                    numbers += cell.getValue().length;
                    break;
                }
            }
        }
        // The reached blocks' faces in the model's space, each triangle tagged with its block, struck together.
        final float[] corners = new float[numbers];
        final int[] tags = new int[numbers / (AXES * AXES)];
        int at = 0;
        for (int i = 0; i < reached.size(); i++) {
            final float[] pieces = rest.get(reached.get(i));
            final double[] corner = corner(origin, reached.get(i));
            for (int n = 0; n < pieces.length; n++) {
                corners[at + n] = (float) (pieces[n] + corner[n % AXES]);
            }
            Arrays.fill(tags, at / (AXES * AXES), (at + pieces.length) / (AXES * AXES), i);
            at += pieces.length;
        }
        final MeshDamage.Damaged damaged = MeshDamage.apply(corners, AXES, tags, strikes);
        final int[] counts = new int[reached.size()];
        for (final int tag : damaged.tags()) {
            counts[tag]++;
        }
        final float[][] per = new float[reached.size()][];
        final int[] filled = new int[reached.size()];
        for (int i = 0; i < reached.size(); i++) {
            per[i] = new float[counts[i] * AXES * AXES];
        }
        final float[] all = damaged.corners();
        for (int t = 0; t < damaged.triangles(); t++) {
            final int tag = damaged.tags()[t];
            final double[] corner = corner(origin, reached.get(tag));
            for (int n = 0; n < AXES * AXES; n++) {
                per[tag][filled[tag]++] = (float) (all[t * AXES * AXES + n] - corner[n % AXES]);
            }
        }
        final Map<Long, float[]> faces = new HashMap<>();
        for (int i = 0; i < reached.size(); i++) {
            faces.put(reached.get(i), per[i]);
        }
        return new Result(faces);
    }

    /* Whether {@code strike} reaches any face {@code pieces} of a block whose lowest corner is at {@code corner}. */
    private static boolean reaches(final Strike strike, final float[] pieces, final double[] corner) {
        // The strike taken into the block's own space, where its faces are.
        final Strike local = new Strike(strike.stamp(), strike.x() - corner[0], strike.y() - corner[1],
                strike.z() - corner[2], strike.nx(), strike.ny(), strike.nz(), strike.size(), strike.seed(),
                strike.lost());
        for (int first = 0; first + AXES * AXES <= pieces.length; first += AXES * AXES) {
            if (MeshDamage.reaches(local, pieces, AXES, first)) {
                return true;
            }
        }
        return false;
    }

    /* Where the lowest corner of the block {@code key} is in the model's space. */
    private static double[] corner(final double[] origin, final long key) {
        return new double[] {origin[0] + MeshCells.x(key), origin[1] + MeshCells.y(key), origin[2] + MeshCells.z(key)};
    }

    /* Where in the world the block {@code key} of the model stands, the block at {@code pos} being {@code place}. */
    private static BlockPos blockOf(final BlockPos pos, final FormedPlace place, final long key) {
        final double[] local = {MeshCells.x(key) - place.x() + HALF, MeshCells.y(key) - place.y() + HALF,
                MeshCells.z(key) - place.z() + HALF};
        final double[] world = new double[AXES];
        CellSpace.toWorld(local, place.turn(), world);
        return pos.offset((int) Math.round(world[0] - HALF), (int) Math.round(world[1] - HALF),
                (int) Math.round(world[2] - HALF));
    }

    /* Whether {@code part} is the block {@code key} of the model {@code model}. */
    private static boolean holds(final FormedPartBlockEntity part, final ResourceLocation model, final long key) {
        for (final FormedPlace place : part.places()) {
            if (place.model().equals(model) && place.cell() == key) {
                return true;
            }
        }
        return false;
    }

    /* The struck block's place in its model, the model's shape file and its faces at rest; none for a plain block. */
    private static @Nullable Struck struck(final Level level, final BlockPos pos) {
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof FormedPartBlockEntity part)
                || part.places().isEmpty()) {
            return null;
        }
        final FormedPlace place = part.places().getFirst();
        final Optional<FormedShape> shape = FormedShapes.get(level, place.model());
        if (shape.isEmpty()) {
            return null;
        }
        final Optional<Map<Long, float[]>> rest = FormedFaces.of(place.model(), shape.get());
        return rest.map(faces -> new Struck(place, shape.get(), faces)).orElse(null);
    }

    private static List<DamageMark> ordered(final Collection<DamageMark> marks) {
        return marks.stream().distinct().sorted(ORDER).toList();
    }

    /* A struck block's place, its model's shape file, and the model's faces at rest. */
    private record Struck(FormedPlace place, FormedShape shape, Map<Long, float[]> rest) {

        Struck {
            rest = Map.copyOf(rest);
        }
    }

    /* What was blamed for a block about to be lost, and when. */
    private record Blame(ResourceLocation stamp, double size, long at) {
    }

    /* A model, its shape file and a set of marks laid on it, in order. */
    private record Key(ResourceLocation model, FormedShape shape, List<DamageMark> marks) {

        Key {
            marks = List.copyOf(marks);
        }
    }

    /* The faces of each block a set of marks reached, as they left them, and the boxes and outlines worked from them. */
    private static final class Result {

        private final Map<Long, float[]> faces;
        private final Map<Long, int[]> boxes = new ConcurrentHashMap<>();
        private final Map<Long, float[]> lines = new ConcurrentHashMap<>();

        private Result(final Map<Long, float[]> faces) {
            this.faces = Map.copyOf(faces);
        }
    }
}
