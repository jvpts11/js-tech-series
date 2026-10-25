/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving.physics;

import dev.jstech.core.moving.ForeignMover;
import dev.jstech.core.moving.ForeignMovers;
import dev.jstech.core.moving.IStructureBody;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructureEvent;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructureBalance;
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.moving.YardLayout;
import dev.jstech.core.protection.Actor;
import dev.jstech.core.protection.ProtectedAction;
import dev.jstech.core.protection.Protection;
import dev.jstech.core.region.Box;
import dev.jstech.core.util.Loaded;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

/**
 * Where a structure moving by its physics meets the world and the other structures near it, and what the blows do.
 * Each block of the world in its reach is seen from the structure, and met by each of the structure's blocks at its
 * surface that comes near it, box by box of their real shapes, a slab as a slab ({@link BoxContacts}); each block of
 * another structure the same, turned as that structure is. A structure moving by its physics too is pushed back as
 * hard as it pushes, each working the pair in its own step; one moved by a rule goes on as its rule says, carrying
 * what rests on it. The blows are summed over the tick, and a block whose blow was more than it holds breaks.
 */
final class StructureContacts {

    private final ContactSolver solver = new ContactSolver();
    private final BoxContacts boxes = new BoxContacts();
    private final OrientedBox mine = new OrientedBox();
    private final OrientedBox theirs = new OrientedBox();
    private final RigidBody ground = new RigidBody();
    private final Vector3d[] axes = {new Vector3d(), new Vector3d(), new Vector3d()};
    private final Vector3d[] otherAxes = {new Vector3d(), new Vector3d(), new Vector3d()};
    private final Vector3d scratch = new Vector3d();
    private final Vector3d middle = new Vector3d();
    private final Vector3d local = new Vector3d();
    private final Vector3d onFace = new Vector3d();
    private final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
    /* For each contact of the step, in the solver's order: the structure's block, and the block it met. */
    private final LongArrayList cells = new LongArrayList();
    private final LongArrayList met = new LongArrayList();
    /* The blows of the tick, summed: at the structure's blocks, and at the blocks they met. */
    private final Long2DoubleOpenHashMap cellBlows = new Long2DoubleOpenHashMap();
    private final Long2DoubleOpenHashMap metBlows = new Long2DoubleOpenHashMap();
    private final BoxContacts.IPointSink sink = this::take;
    /* Whether each contact of the step counts its blow, in the solver's order. */
    private final BooleanArrayList counted = new BooleanArrayList();
    /* Whether each contact's block met is another mod's moving structure's, which no blow of the kit's breaks. */
    private final BooleanArrayList foreign = new BooleanArrayList();
    /* The hardest blow of the tick, told to the game as it ends: where, how fast, and the block it met, packed. */
    private final Vector3d hardestAt = new Vector3d();
    private double hardest;
    private double hardestClosing;
    private long hardestMet;
    private boolean hardestForeign;
    private @Nullable MovingStructure struck;
    /*
     * The pair being worked: the bodies, the blocks, how well they grip, the frame its points are kept in, and what
     * the block met belongs to, the world (no frame) or a structure (its frame and, moving by its physics, its parts).
     */
    private RigidBody first;
    private RigidBody second;
    private StructureFrame frame;
    private StructureParts ownParts;
    private @Nullable StructureFrame theirFrame;
    private @Nullable StructureParts theirParts;
    /* What reads the blocks met, and the level whose loaded chunks they are in: the world's, or another mod's. */
    private BlockGetter theirReads;
    private @Nullable Level theirLoaded;
    private long cell;
    private long other;
    private long pair;
    /* The two boxes being met, counted, so the points where they meet bounce together. */
    private long manifold;
    private double grip;
    private boolean counts = true;
    private boolean meetingForeign;

    /* How far a block's corner is from its middle: a block's whole reach, however it is turned. */
    private static final double REACH = Math.sqrt(3) / 2;
    /* The most blocks a structure's blows break in a tick, so a crash does not stall the game. */
    private static final int MOST_BROKEN = 32;
    /* A block of no volume, a rail or a flower, still holds a blow as a sliver of a block does. */
    private static final double LEAST_VOLUME = 0.05;
    /*
     * Slower than this, in metres a second, a structure meets one asleep gently, as it would the ground: more than a
     * tick of the pull gives one resting on it, so a crate resting on another asleep rests still; only a blow wakes it.
     */
    private static final double GENTLE = 2.0;
    /* A way this near an axis goes through a face; one between, along an edge or through a corner. */
    private static final double FACE = 0.9;
    /* A blow closing slower than this, in metres a second, is a structure settling, not a collision to tell. */
    private static final double COLLISION_SPEED = 1.0;

    StructureContacts() {
        ground.fixed();
    }

    /**
     * Finds where the structure standing at {@code frame}, moving as {@code body}, meets the world and the structures
     * near it over a step of {@code seconds}, ahead of time by {@code margin}, the structures of {@code island} worked
     * with it standing where they stand mid-tick. False, and nothing found, while a chunk it reaches is not loaded: it
     * waits there, as it would fall through what is unread.
     */
    boolean find(final ServerLevel level, final MovingStructure structure, final StructureFrame standing,
                 final StructureParts parts, final RigidBody body, final double margin, final double seconds,
                 final PhysicsIsland island) {
        solver.clear();
        cells.clear();
        met.clear();
        counted.clear();
        foreign.clear();
        struck = structure;
        final AABB box = worldBox(standing, structure.bounds()).inflate(margin);
        if (!Loaded.area(level, floor(box.minX), floor(box.minZ), floor(box.maxX), floor(box.maxZ))) {
            return false;
        }
        this.frame = standing;
        this.ownParts = parts;
        FrameMath.axes(standing, axes);
        world(level, parts, body, box, margin);
        structures(level, structure, parts, body, box, margin, seconds, island);
        return true;
    }

    /**
     * Readies the contacts found for a step of {@code seconds}, none started yet from its push of the step before:
     * how many times they are to be worked.
     */
    int ready(final double seconds, final double settles) {
        return solver.ready(seconds, ContactSolver.ITERATIONS, settles);
    }

    /** Starts each contact readied from its push of the step before. */
    void warm() {
        solver.warm();
    }

    /** Works the contacts {@code times} times. */
    void iterate(final int times) {
        solver.iterate(times);
    }

    /** Works the contacts once, the {@code index}th time. */
    void round(final int index) {
        solver.round(index);
    }

    /** Keeps what the contacts gave, and sums the blows they took over the tick. */
    void end() {
        solver.end();
        final List<ContactSolver.Contact> contacts = solver.contacts();
        for (int i = 0; i < contacts.size(); i++) {
            final ContactSolver.Contact contact = contacts.get(i);
            final double blow = contact.blow();
            if (blow > 0 && counted.getBoolean(i)) {
                cellBlows.addTo(cells.getLong(i), blow);
                final boolean theirs = foreign.getBoolean(i);
                if (!theirs) {
                    metBlows.addTo(met.getLong(i), blow);
                }
                if (blow > hardest) {
                    hardest = blow;
                    hardestAt.set(contact.point());
                    hardestClosing = contact.closing();
                    hardestMet = met.getLong(i);
                    hardestForeign = theirs;
                }
            }
        }
    }

    /** The contacts of the last step. */
    List<ContactSolver.Contact> contacts() {
        return solver.contacts();
    }

    /**
     * Lends the contacts of its last step with the world to {@code into}, worked there for {@code body} against the
     * ground: so a structure meeting this one works the two together, this one held up by what holds it.
     */
    void lendWorld(final ContactSolver into, final RigidBody body, final RigidBody theGround) {
        for (final ContactSolver.Contact contact : solver.contacts()) {
            if (contact.a() == body && contact.b() == ground) {
                into.add(body, theGround, contact.point(), contact.normal(), contact.apart(), contact.grip(),
                        contact.key(), contact.group() | Long.MIN_VALUE);
            }
        }
    }

    /** Forgets every contact, for a structure set moving anew. */
    void reset() {
        solver.reset();
        cellBlows.clear();
        metBlows.clear();
    }

    /** Forgets what its contacts pushed against other structures, as the structure falls asleep. */
    void forget() {
        solver.forgetBut(ground);
    }

    /**
     * Breaks each block whose blows this tick were more than it holds: of the world, of another structure, and of
     * this one, as many as a tick allows; what breaks drops as broken.
     */
    void breakWhatBroke(final ServerLevel level) {
        final MovingStructure mine = struck;
        tellHardest(level);
        if (StructureBalance.impacts() && mine != null) {
            final Actor actor = actorOf(mine);
            breakEach(level, actor, cellBlows, breakEach(level, actor, metBlows, 0));
        }
        metBlows.clear();
        cellBlows.clear();
    }

    /** The box of the world a structure's blocks fill, turned as its frame stands. */
    static AABB worldBox(final StructureFrame frame, final Box bounds) {
        final double[] box = frame.worldBox(bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX() + 1,
                bounds.maxY() + 1, bounds.maxZ() + 1);
        return new AABB(box[0], box[1], box[2], box[3], box[4], box[5]);
    }

    /**
     * A body for a structure moved by a rule: it does not give, and goes as its rule moved it last tick, so what
     * rests on it is carried and what it runs into is pushed.
     */
    static RigidBody held(final MovingStructure structure) {
        final RigidBody body = new RigidBody();
        body.fixed();
        final StructureFrame standing = structure.frame();
        body.position().set(standing.pose().x(), standing.pose().y(), standing.pose().z());
        final double[] moved = structure.velocity();
        final double[] turned = structure.spin();
        body.velocity().set(moved[0], moved[1], moved[2]).mul(StructurePhysics.TICKS);
        body.spin().set(turned[0], turned[1], turned[2]).mul(StructurePhysics.TICKS);
        return body;
    }

    /* Every block of the world in {@code box} the structure's blocks may meet. */
    private void world(final ServerLevel level, final StructureParts parts, final RigidBody body, final AABB box,
                       final double margin) {
        this.theirFrame = null;
        this.theirParts = null;
        this.theirReads = level;
        this.theirLoaded = level;
        final int minY = Math.max(level.getMinBuildHeight(), floor(box.minY));
        final int maxY = Math.min(level.getMaxBuildHeight() - 1, floor(box.maxY));
        if (minY > maxY || YardLayout.isYard(box.minX) || YardLayout.isYard(box.maxX)) {
            return;
        }
        final int minX = floor(box.minX);
        final int maxX = floor(box.maxX);
        final int minZ = floor(box.minZ);
        final int maxZ = floor(box.maxZ);
        for (int chunkX = minX >> 4; chunkX <= maxX >> 4; chunkX++) {
            for (int chunkZ = minZ >> 4; chunkZ <= maxZ >> 4; chunkZ++) {
                final LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }
                for (int sectionY = minY >> 4; sectionY <= maxY >> 4; sectionY++) {
                    final LevelChunkSection section = chunk.getSection(chunk.getSectionIndexFromSectionY(sectionY));
                    if (section.hasOnlyAir()) {
                        continue;
                    }
                    final int fromX = Math.max(minX, chunkX << 4);
                    final int toX = Math.min(maxX, (chunkX << 4) + 15);
                    final int fromY = Math.max(minY, sectionY << 4);
                    final int toY = Math.min(maxY, (sectionY << 4) + 15);
                    final int fromZ = Math.max(minZ, chunkZ << 4);
                    final int toZ = Math.min(maxZ, (chunkZ << 4) + 15);
                    for (int x = fromX; x <= toX; x++) {
                        for (int y = fromY; y <= toY; y++) {
                            for (int z = fromZ; z <= toZ; z++) {
                                final BlockState state = section.getBlockState(x & 15, y & 15, z & 15);
                                if (!state.isAir()) {
                                    meetWorld(level, parts, body, x, y, z, state, margin);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /* The block of the world at {@code (x, y, z)}, of {@code state}, met by the structure's blocks near it. */
    private void meetWorld(final ServerLevel level, final StructureParts parts, final RigidBody body, final int x,
                           final int y, final int z, final BlockState state, final double margin) {
        at.set(x, y, z);
        final VoxelShape shape = state.getCollisionShape(level, at);
        if (shape.isEmpty()) {
            return;
        }
        final double worldGrip = ContactMath.grip(state.getFriction(level, at, null));
        final long worldKey = at.asLong();
        for (final AABB part : StructureParts.boxesOf(shape)) {
            final double minX = x + part.minX;
            final double minY = y + part.minY;
            final double minZ = z + part.minZ;
            final double maxX = x + part.maxX;
            final double maxY = y + part.maxY;
            final double maxZ = z + part.maxZ;
            final double[] yard = frame.yardBox(minX - margin, minY - margin, minZ - margin, maxX + margin,
                    maxY + margin, maxZ + margin);
            for (int cx = floor(yard[0]); cx <= floor(yard[3]); cx++) {
                for (int cy = floor(yard[1]); cy <= floor(yard[4]); cy++) {
                    for (int cz = floor(yard[2]); cz <= floor(yard[5]); cz++) {
                        final long key = BlockPos.asLong(cx, cy, cz);
                        if (!parts.surface().contains(key)) {
                            continue;
                        }
                        // Set again for each block: a pair of structure boxes reuses the world's box below.
                        theirs.square(minX, minY, minZ, maxX, maxY, maxZ);
                        for (final AABB own : parts.shape(key)) {
                            FrameMath.box(frame, cx, cy, cz, own, axes, scratch, mine);
                            pairOf(body, ground, key, worldKey, ContactMath.grip(worldGrip, parts.grip(key)));
                            boxes.collide(mine, theirs, margin, sink);
                        }
                    }
                }
            }
        }
    }

    /*
     * The other structures near: each block of theirs the structure's blocks may meet over a step of {@code seconds}.
     * One worked in the same steps is looked for as far ahead as both may go, as only one of the two works the pair.
     */
    private void structures(final ServerLevel level, final MovingStructure structure, final StructureParts parts,
                            final RigidBody body, final AABB box, final double margin, final double seconds,
                            final PhysicsIsland island) {
        final StructurePhysics mine = island.member(structure);
        /*
         * The structures near are found where they stood as the tick began, and those worked with it have gone on
         * since: looked for as much further, one closing on it a step at a time is met however far it came, not missed
         * until it is already inside.
         */
        final AABB looked = box.inflate(island.widest(seconds) + island.goneMost());
        for (final IStructureBody near : MovingStructures.near(level, looked)) {
            if (near == structure || !near.ready() || !(near instanceof MovingStructure them)) {
                continue;
            }
            // What it holds and what holds it are of its own body: they move as one, and never meet.
            final StructurePhysics physics = island.owner(them);
            if (mine != null && physics == mine) {
                continue;
            }
            final boolean together = island.member(them) != null;
            // Two worked in the same steps meet once: the one of the lower id works the pair, both bodies pushed.
            if (together && structure.id().value().compareTo(them.id().value()) > 0) {
                continue;
            }
            final RigidBody theirBody;
            final StructureParts made;
            if (physics != null && physics.active()) {
                /*
                 * Each works the pair in its own step, both pushed: so each meets the other as its own pull acts. One
                 * asleep, met gently, holds as the ground does, so what rests on it rests still; met hard, it wakes.
                 * One that took its turn this tick already, waiting on the budget or worked in an island before this
                 * one, is held as it goes, standing still if it waits: pushed, it would take the weight of what rests
                 * on it with nothing of what holds it up worked here. One worked with it in the same steps moves with
                 * it, asleep or not.
                 */
                if (!together && physics.tookTurn(island.now())) {
                    theirBody = held(them);
                    if (physics.waitsAt(island.now())) {
                        theirBody.velocity().zero();
                        theirBody.spin().zero();
                    }
                } else if (!together && physics.asleep()
                        && body.velocity().length() + body.spin().length() < GENTLE) {
                    theirBody = held(them);
                } else {
                    physics.prepareBody();
                    theirBody = physics.body();
                }
                made = physics.partsOf(them);
            } else {
                theirBody = held(them);
                made = null;
            }
            final StructureFrame theirs = island.standing(them);
            final double theirGoing = together && physics != null ? physics.margin(seconds) : 0;
            final double meeting = margin + theirGoing;
            final AABB overlap = box.inflate(theirGoing).intersect(worldBox(theirs, them.bounds()).inflate(meeting));
            if (overlap.getXsize() < 0 || overlap.getYsize() < 0 || overlap.getZsize() < 0) {
                continue;
            }
            // Two structures moving by their physics both work their blows: only the one of the lower id counts them.
            counts = made == null || structure.id().value().compareTo(them.id().value()) < 0;
            final int before = solver.contacts().size();
            meetStructure(level, level, theirs, parts, made, body, theirBody, overlap.inflate(REACH), meeting);
            counts = true;
            // One worked in the same steps has its own contacts with the ground worked with these already.
            if (!together && physics != null && theirBody == physics.body() && solver.contacts().size() > before) {
                // They meet: worked as one, the other held up by the ground it stands on.
                final int lent = solver.contacts().size();
                physics.lendWorld(solver, theirBody, ground);
                for (int i = lent; i < solver.contacts().size(); i++) {
                    cells.add(0);
                    met.add(0);
                    counted.add(false);
                    foreign.add(false);
                }
            }
        }
        foreigners(parts, body, box, margin, level);
    }

    /*
     * The moving structures of other mods near ({@link ForeignMovers}): obstacles that move, met as the kit's own held
     * by a rule are, never moved by what meets them and none of their blocks broken.
     */
    private void foreigners(final StructureParts parts, final RigidBody body, final AABB box, final double margin,
                            final ServerLevel level) {
        if (!ForeignMovers.any()) {
            return;
        }
        for (final ForeignMover theirs : ForeignMovers.near(level, box)) {
            final AABB overlap = box.intersect(worldBox(theirs.frame(), theirs.blocks()).inflate(margin));
            if (overlap.getXsize() < 0 || overlap.getYsize() < 0 || overlap.getZsize() < 0) {
                continue;
            }
            final RigidBody moving = new RigidBody();
            moving.fixed();
            moving.position().set(theirs.frame().pose().x(), theirs.frame().pose().y(), theirs.frame().pose().z());
            moving.velocity().set(theirs.speed().x, theirs.speed().y, theirs.speed().z);
            moving.spin().set(theirs.spin().x, theirs.spin().y, theirs.spin().z);
            meetingForeign = true;
            meetStructure(theirs.reads(), theirs.loaded(), theirs.frame(), parts, null, body, moving,
                    overlap.inflate(REACH), margin);
            meetingForeign = false;
        }
    }

    /*
     * The structure's blocks within {@code overlap} met by the blocks of another, standing at {@code standing}: its
     * {@code others} parts, when it moves by its physics, else its blocks as {@code reads} has them, read only where
     * {@code loaded} has them loaded when they are a level's.
     */
    private void meetStructure(final BlockGetter reads, @Nullable final Level loaded, final StructureFrame standing,
                               final StructureParts parts, @Nullable final StructureParts others,
                               final RigidBody body, final RigidBody theirBody, final AABB overlap,
                               final double margin) {
        this.theirFrame = standing;
        this.theirParts = others;
        this.theirReads = reads;
        this.theirLoaded = loaded;
        FrameMath.axes(standing, otherAxes);
        final double reach = REACH + margin;
        for (final long key : parts.surface()) {
            final int x = BlockPos.getX(key);
            final int y = BlockPos.getY(key);
            final int z = BlockPos.getZ(key);
            FrameMath.middle(frame, x, y, z, middle);
            if (!overlap.contains(middle.x, middle.y, middle.z)) {
                continue;
            }
            final double[] yard = standing.yardBox(middle.x - reach, middle.y - reach, middle.z - reach,
                    middle.x + reach, middle.y + reach, middle.z + reach);
            for (int ox = floor(yard[0]); ox <= floor(yard[3]); ox++) {
                for (int oy = floor(yard[1]); oy <= floor(yard[4]); oy++) {
                    for (int oz = floor(yard[2]); oz <= floor(yard[5]); oz++) {
                        final long theirKey = BlockPos.asLong(ox, oy, oz);
                        final AABB[] theirShape;
                        final double theirGrip;
                        if (others != null) {
                            if (!others.surface().contains(theirKey)) {
                                continue;
                            }
                            theirShape = others.shape(theirKey);
                            theirGrip = others.grip(theirKey);
                        } else {
                            at.set(ox, oy, oz);
                            if (loaded != null && !Loaded.at(loaded, at)) {
                                continue;
                            }
                            final BlockState state = reads.getBlockState(at);
                            if (state.isAir()) {
                                continue;
                            }
                            final VoxelShape shape = state.getCollisionShape(reads, at);
                            if (shape.isEmpty()) {
                                continue;
                            }
                            theirShape = StructureParts.boxesOf(shape);
                            theirGrip = ContactMath.grip(loaded != null ? state.getFriction(loaded, at, null)
                                    : state.getBlock().getFriction());
                        }
                        for (final AABB own : parts.shape(key)) {
                            for (final AABB box : theirShape) {
                                FrameMath.box(frame, x, y, z, own, axes, scratch, mine);
                                FrameMath.box(standing, ox, oy, oz, box, otherAxes, scratch, theirs);
                                pairOf(body, theirBody, key, theirKey, ContactMath.grip(parts.grip(key), theirGrip));
                                boxes.collide(mine, theirs, margin, sink);
                            }
                        }
                    }
                }
            }
        }
    }

    /* The pair worked next: its bodies, its blocks and how well they grip. */
    private void pairOf(final RigidBody a, final RigidBody b, final long ownKey, final long otherKey,
                        final double holding) {
        this.manifold++;
        this.first = a;
        this.second = b;
        this.cell = ownKey;
        this.other = otherKey;
        this.pair = ownKey * 31 + otherKey;
        this.grip = holding;
    }

    /*
     * A point the pair meets at: a contact, known from step to step by where it is on the structure; none through a
     * face another block covers.
     */
    private void take(final Vector3d point, final Vector3d way, final double apart, final int feature) {
        if (throughCoveredFace(way)) {
            return;
        }
        // Known by where it is on the structure's own face, half the gap from the point between the two: the same
        // place whether still apart or touching, so what it pushed carries over as they close.
        FrameMath.yard(frame, onFace.set(way).mul(apart / 2).add(point), local);
        solver.add(first, second, point, way, apart, grip, ContactSolver.key(pair, local), manifold);
        cells.add(cell);
        met.add(other);
        counted.add(counts);
        foreign.add(meetingForeign);
    }

    /*
     * Whether the way the pair meets along, from the block met to the structure's, goes through a face of either that
     * a whole block beside it covers. Such a face is inside a surface, as between two blocks of one floor: a block
     * landing across two meets the one beside it there, and a contact through that face would push it sideways off a
     * flat floor, or bump it as it slides over one.
     */
    private boolean throughCoveredFace(final Vector3d way) {
        // The structure's block is met on its face looking against the way.
        local.set(frame.unturnX(way.x, way.y, way.z), frame.unturnY(way.x, way.y, way.z),
                frame.unturnZ(way.x, way.y, way.z));
        final Direction ownFace = faceOf(local, -1);
        if (ownFace != null && ownParts.whole(BlockPos.offset(cell, ownFace))) {
            return true;
        }
        // The block met, on its face looking along the way: the world's, or another structure's.
        final Direction face;
        if (theirFrame == null) {
            face = faceOf(way, 1);
        } else {
            local.set(theirFrame.unturnX(way.x, way.y, way.z), theirFrame.unturnY(way.x, way.y, way.z),
                    theirFrame.unturnZ(way.x, way.y, way.z));
            face = faceOf(local, 1);
        }
        if (face == null) {
            return false;
        }
        final long beside = BlockPos.offset(other, face);
        if (theirParts != null) {
            return theirParts.whole(beside);
        }
        at.set(beside);
        if (theirLoaded != null && !Loaded.at(theirLoaded, at)) {
            return false;
        }
        return Block.isShapeFullBlock(theirReads.getBlockState(at).getCollisionShape(theirReads, at));
    }

    /* The face a way goes through, {@code sign} along it, or none when it goes along an edge or through a corner. */
    private static @Nullable Direction faceOf(final Vector3d way, final int sign) {
        final double x = way.x * sign;
        final double y = way.y * sign;
        final double z = way.z * sign;
        if (Math.abs(x) >= FACE) {
            return x > 0 ? Direction.EAST : Direction.WEST;
        }
        if (Math.abs(y) >= FACE) {
            return y > 0 ? Direction.UP : Direction.DOWN;
        }
        if (Math.abs(z) >= FACE) {
            return z > 0 ? Direction.SOUTH : Direction.NORTH;
        }
        return null;
    }

    /*
     * Tells the game of the hardest blow of the tick, when it closed fast enough to be a blow and not a structure
     * settling ({@link MovingStructureEvent.Collision}), and forgets it.
     */
    private void tellHardest(final ServerLevel level) {
        final MovingStructure mine = struck;
        if (mine != null && hardest > 0 && hardestClosing >= COLLISION_SPEED) {
            // A blow at another mod's structure is told at the world's block it struck at, as it is no structure here.
            final BlockPos where = hardestForeign ? BlockPos.containing(hardestAt.x, hardestAt.y, hardestAt.z)
                    : BlockPos.of(hardestMet);
            final IStructureBody other = !hardestForeign && YardLayout.isYardBlock(where.getX())
                    ? MovingStructures.at(level, where) : null;
            NeoForge.EVENT_BUS.post(new MovingStructureEvent.Collision(mine, other, where,
                    new Vec3(hardestAt.x, hardestAt.y, hardestAt.z), hardestClosing, hardest));
        }
        hardest = 0;
        hardestClosing = 0;
    }

    /*
     * Who breaks what a structure's blows break: the player who made it, weighed as them wherever they are; for one
     * made by nobody, the world, coming from where the structure stands. A claim keeps out a crash as it does a hand.
     */
    private static Actor actorOf(final MovingStructure structure) {
        return structure.maker().map(Actor::forOwner)
                .orElseGet(() -> Actor.world(BlockPos.containing(structure.worldBox().getCenter())));
    }

    /*
     * Breaks each block of {@code blows} whose blow was more than it holds and that {@code actor} may break; how many
     * are broken this tick, all told.
     */
    private static int breakEach(final ServerLevel level, final Actor actor, final Long2DoubleMap blows,
                                 final int before) {
        int broken = before;
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (final Long2DoubleMap.Entry blow : blows.long2DoubleEntrySet()) {
            if (broken >= MOST_BROKEN) {
                break;
            }
            pos.set(blow.getLongKey());
            if (!Loaded.at(level, pos)) {
                continue;
            }
            final BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.getDestroySpeed(level, pos) < 0) {
                continue;
            }
            if (ContactMath.breaks(blow.getDoubleValue(), BlockPhysics.strengthOf(state),
                    Math.max(BlockPhysics.volumeOf(level, pos, state), LEAST_VOLUME))
                    && Protection.may(actor, ProtectedAction.BREAK, level, pos)) {
                level.destroyBlock(pos.immutable(), true);
                broken++;
            }
        }
        return broken;
    }

    private static int floor(final double value) {
        return (int) Math.floor(value);
    }
}
