/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import dev.jstech.core.content.EntityEntry;
import dev.jstech.core.moving.joint.StructureJoints;
import dev.jstech.core.moving.physics.PhysicsMotion;
import dev.jstech.core.moving.physics.StructurePhysics;
import dev.jstech.core.region.Box;
import dev.jstech.core.registry.CoreItems;
import dev.jstech.core.registry.CoreAttachments;
import dev.jstech.core.uuid.StructureUuid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

/**
 * Moving structures: structures of real blocks that move through the world, from a drawbridge to an airship. The
 * blocks are kept in a reserved region of the world, the yard ({@link StructureYard}), where they run as they would on
 * the ground: machines run, computers boot, networks work and crops grow. They are drawn and collided where the
 * structure is, carry what stands on them, and are hit by arrows and every other ray. A structure moves by a rule
 * ({@link IStructureMotion}), survives a restart in mid-move and goes to another dimension with everyone aboard.
 *
 * <pre>{@code
 * StructureResult made = MovingStructures.assemble(level, corner, otherCorner);
 * MovingStructures.move(made.structure(), new TowardMotion(StructurePose.at(x, y + 12, z), 2, 0));
 * MovingStructures.setDown(made.structure());
 * }</pre>
 */
public final class MovingStructures {

    /** The entity each structure is in the world as. */
    public static final EntityEntry<MovingStructureEntity> ENTITY = CoreItems.CONTENT.entity("moving_structure",
                    MovingStructureEntity::new, MobCategory.MISC).size(1, 1).tracking(32, 20).fireImmune().noSummon()
            .named("Moving structure").register();
    /** The entity someone sitting on a structure's seat sits in. */
    public static final EntityEntry<StructureSeatEntity> SEAT = CoreItems.CONTENT.entity("structure_seat",
                    StructureSeatEntity::new, MobCategory.MISC).size(0.4F, 0.2F).tracking(10, 1).fireImmune()
            .noSummon().named("Structure seat").register();
    /** The entity a player riding a rope down hangs from. */
    public static final EntityEntry<ZiplineEntity> ZIPLINE = CoreItems.CONTENT.entity("zipline",
                    ZiplineEntity::new, MobCategory.MISC).size(0.3F, 0.3F).tracking(10, 1).fireImmune()
            .noSummon().named("Zipline").register();
    /* From this many structures moving in a level, their next poses are worked out beside the server's thread. */
    private static final int ALONGSIDE = 8;
    /* Players who left the game standing on a structure, waiting for it to load so they stand on it again. */
    private static final Map<ServerPlayer, Footing> RETURNING = new WeakHashMap<>();
    private static volatile ForkJoinPool workers;

    private MovingStructures() {
    }

    /** Hands the game's bus what the kit answers, and its payloads to the mod's, once, from the Core's constructor. */
    public static void listen(final IEventBus modBus) {
        StructureYard.listen();
        StructureJoints.listen();
        StructureSeating.listen();
        StructureSleeping.listen();
        StructureForceWatch.listen();
        StructureSync.listen(modBus);
        StructureCommands.declare();
        NeoForge.EVENT_BUS.addListener(LevelTickEvent.Pre.class, MovingStructures::beforeTick);
        NeoForge.EVENT_BUS.addListener(LevelTickEvent.Post.class, MovingStructures::afterTick);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, EntityJoinLevelEvent.class, MovingStructures::onJoin);
        NeoForge.EVENT_BUS.addListener(BlockEvent.EntityPlaceEvent.class, MovingStructures::onPlace);
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, MovingStructures::onLoggedIn);
    }

    /** Makes a structure of every block in the box from {@code from} to {@code to}, its pivot at their middle. */
    public static StructureResult assemble(final ServerLevel level, final BlockPos from, final BlockPos to) {
        return StructureAssembly.box(level, from, to, null);
    }

    /** Makes a structure of {@code seed} and every block joined to it face to face. */
    public static StructureResult assembleJoined(final ServerLevel level, final BlockPos seed) {
        return StructureAssembly.joined(level, seed, null);
    }

    /** Sets {@code structure} down on the world's grid where it is, and takes it away. */
    public static StructureResult setDown(final MovingStructure structure) {
        return StructureDisassembly.setDown(structure);
    }

    /** Takes {@code structure} away for good, its blocks gone with it and its plot of the yard given back. */
    public static void takeAway(final MovingStructure structure) {
        remove(structure, true);
    }

    /** Sets the rule that moves {@code structure} from the next tick, or null to bring it to rest. */
    public static void move(final MovingStructure structure, @Nullable final IStructureMotion motion) {
        structure.move(motion);
    }

    /**
     * Holds {@code child} by {@code parent} where it stands now, a turret on a ship, a shuttle docked to a station:
     * it goes wherever the parent goes, and its own rule turns it on the parent ({@link StructureMounts}).
     * {@code anchor} is the parent's block holding it, whose loss lets it go.
     */
    public static StructureResult mount(final MovingStructure child, final MovingStructure parent,
                                        final Optional<BlockPos> anchor) {
        return StructureMounts.mount(child, parent, anchor);
    }

    /** Lets {@code child} go from what holds it: by its physics at its place's speed when it {@code falls}. */
    public static StructureResult unmount(final MovingStructure child, final boolean falls) {
        return StructureMounts.unmount(child, falls);
    }

    /**
     * Docks {@code visitor} to {@code host} where it stands: one with it while docked, their momentum shared
     * ({@link StructureDocking}); {@code clamp} the host's block holding it.
     */
    public static StructureResult dock(final MovingStructure host, final MovingStructure visitor,
                                       final Optional<BlockPos> clamp) {
        return StructureDocking.dock(host, visitor, clamp);
    }

    /** Lets a docked {@code visitor} go, pushed gently away along {@code away}, a way of the world. */
    public static StructureResult undock(final MovingStructure visitor, final Vec3 away) {
        return StructureDocking.undock(visitor, away);
    }

    /**
     * Joins {@code visitor} into {@code host} for good, its blocks moved into the host where they stand on it now
     * ({@link StructureMerging}); refused when the two are not square to each other or their blocks would overlap.
     */
    public static StructureResult join(final MovingStructure host, final MovingStructure visitor) {
        return StructureMerging.join(host, visitor);
    }

    /**
     * Sends {@code structure} to {@code to} with everyone aboard, its pivot arriving at {@code at}: it goes a few
     * ticks later, once the chunks it goes to have loaded, and is found there by its id. Refused, saying why, when it
     * cannot go.
     */
    public static StructureResult travel(final MovingStructure structure, final ServerLevel to, final Vec3 at) {
        return StructureTravel.travel(structure, to, at);
    }

    /**
     * Sits {@code player} on the seat at the yard's block {@code seat} of a structure, as using it does: refused
     * while they ride something or someone sits there already; whether they sit.
     */
    public static boolean sit(final ServerPlayer player, final BlockPos seat) {
        return StructureSeating.sit(player, seat);
    }

    /**
     * {@code player}, sitting at a helm, holds the controls {@code keys}, each its {@link StructureHelmPayload#bit},
     * as their game tells them: the structure they sit on is steered by them while they hold any; whether they sit at
     * a helm.
     */
    public static boolean helm(final ServerPlayer player, final int keys) {
        return StructureSeating.helm(player, keys);
    }

    /**
     * {@code player} takes hold of the rope joint {@code joint} at {@code along}, from 0 at its first end to 1 at its
     * second, to ride it down: refused out of their reach or while they ride something; whether they hang from it.
     */
    public static boolean zipline(final ServerPlayer player, final UUID joint, final double along) {
        return StructureSeating.zipline(player, joint, along);
    }

    /**
     * Pushes {@code structure} by {@code impulse}, in newton seconds, at the world's place {@code at}: one moving by
     * its physics changes its speed and its spin at once, as at a blow; one at rest or moved by a rule is let go to
     * its physics first, as anything pushed hard is. One held by another pushes what holds it.
     */
    public static void push(final MovingStructure structure, final Vec3 impulse, final Vec3 at) {
        final MovingStructure root = StructureMounts.rootOf(structure);
        if (!(root.motion() instanceof PhysicsMotion)) {
            root.move(PhysicsMotion.AT_REST);
        }
        root.physicsMade().push(new Vector3d(impulse.x, impulse.y, impulse.z), new Vector3d(at.x, at.y, at.z));
    }

    /**
     * Freezes {@code structure} where it stands, in the air too, its physics stopped, or, {@code frozen} false, lets
     * it go to its physics again; one held by another freezes or frees what holds it.
     */
    public static void freeze(final MovingStructure structure, final boolean frozen) {
        final MovingStructure root = StructureMounts.rootOf(structure);
        if (frozen) {
            root.move(null);
        } else if (!(root.motion() instanceof PhysicsMotion)) {
            root.move(PhysicsMotion.AT_REST);
        }
    }

    /** Whether {@code structure} is frozen where it stands: what holds it, if anything does, moves by no rule. */
    public static boolean frozen(final MovingStructure structure) {
        return StructureMounts.rootOf(structure).motion() == null;
    }

    /**
     * Holds the place {@code place} of {@code structure}'s yard towards the world's place {@code target}, as a stiff
     * spring damped to stop it swinging, bearing its weight, with at most {@code most} newtons, until the game's tick
     * {@code until}: a grip from outside, as the physics tool's, renewed each tick it holds. One at rest or moved by a
     * rule is let go to its physics first; one held by another pulls what holds it.
     */
    public static void hold(final MovingStructure structure, final Vec3 place, final Vec3 target, final double most,
                            final long until) {
        final MovingStructure root = StructureMounts.rootOf(structure);
        if (!(root.motion() instanceof PhysicsMotion)) {
            root.move(PhysicsMotion.AT_REST);
        }
        final Vec3 held = toYard(root, toWorld(structure, place));
        root.physicsMade().hold(new Vector3d(held.x, held.y, held.z), new Vector3d(target.x, target.y, target.z),
                most, until);
    }

    /** Lets go of what held {@code structure} from outside. */
    public static void letGo(final MovingStructure structure) {
        final StructurePhysics physics = StructureMounts.rootOf(structure).physics();
        if (physics != null) {
            physics.letGo();
        }
    }

    /** The place of {@code structure}'s yard that is at the world's place {@code world} now. */
    public static Vec3 toYard(final IStructureBody structure, final Vec3 world) {
        final StructureFrame frame = structure.frame();
        return new Vec3(frame.yardX(world.x, world.y, world.z), frame.yardY(world.x, world.y, world.z),
                frame.yardZ(world.x, world.y, world.z));
    }

    /** Where the place {@code place} of {@code structure}'s yard is in the world now. */
    public static Vec3 toWorld(final IStructureBody structure, final Vec3 place) {
        final StructureFrame frame = structure.frame();
        return new Vec3(frame.worldX(place.x, place.y, place.z), frame.worldY(place.x, place.y, place.z),
                frame.worldZ(place.x, place.y, place.z));
    }

    /** The structure of that id loaded in {@code level}, or null. */
    public static @Nullable MovingStructure byId(final ServerLevel level, final StructureUuid id) {
        return LevelMovingStructures.of(level).byId(id) instanceof MovingStructure structure ? structure : null;
    }

    /** The structure loaded in {@code level} whose blocks hold the yard's block {@code pos}, or null. */
    public static @Nullable IStructureBody at(final Level level, final BlockPos pos) {
        return LevelMovingStructures.of(level).atYard(pos.getX(), pos.getZ());
    }

    /** The loaded structures of {@code level} whose blocks may fill some of the world's box {@code box}. */
    public static List<IStructureBody> near(final Level level, final AABB box) {
        return LevelMovingStructures.of(level).near(box);
    }

    /**
     * Where the place {@code place} is in the world: a place of the yard among a loaded structure's blocks is where
     * that structure has it now, and so is a place another mod keeps a moving structure's blocks in
     * ({@link ForeignMovers}); any other place is where it is, the same place.
     */
    public static Vec3 inWorld(final Level level, final Vec3 place) {
        if (!YardLayout.isYard(place.x)) {
            return ForeignMovers.inWorld(level, place);
        }
        final IStructureBody body = LevelMovingStructures.of(level).atYard((int) Math.floor(place.x),
                (int) Math.floor(place.z));
        if (body == null) {
            return place;
        }
        final StructureFrame frame = body.frame();
        return new Vec3(frame.worldX(place.x, place.y, place.z), frame.worldY(place.x, place.y, place.z),
                frame.worldZ(place.x, place.y, place.z));
    }

    /** The block of the world that holds the middle of the block {@code pos}, which may be of the yard. */
    public static BlockPos inWorld(final Level level, final BlockPos pos) {
        if (!YardLayout.isYardBlock(pos.getX())) {
            if (!ForeignMovers.any()) {
                return pos;
            }
            final Vec3 middle = Vec3.atCenterOf(pos);
            final Vec3 there = ForeignMovers.inWorld(level, middle);
            return there == middle ? pos : BlockPos.containing(there);
        }
        return BlockPos.containing(inWorld(level, Vec3.atCenterOf(pos)));
    }

    /** A block of the yard changed: the structure whose plot it is in hears of it. */
    public static void noteChange(final ServerLevel level, final BlockPos pos, final BlockState before,
                                  final BlockState after) {
        if (!YardLayout.isYardBlock(pos.getX())) {
            return;
        }
        if (LevelMovingStructures.of(level).atYard(pos.getX(), pos.getZ()) instanceof MovingStructure structure) {
            structure.blockChanged(level, pos, before, after);
        }
    }

    /*
     * Makes a structure's entity in {@code level} and puts it in the world; with {@code arrival}, its blocks still
     * stand where they were, to come into its plot once its chunks have loaded.
     */
    static MovingStructure make(final ServerLevel level, final StructureUuid id, final int plot, final Box bounds,
                                final int blocks, final double pivotX, final double pivotY, final double pivotZ,
                                final StructurePose pose, @Nullable final StructureArrival arrival) {
        final MovingStructureEntity entity = ENTITY.get().create(level);
        if (entity == null) {
            throw new IllegalStateException("the moving structure's entity could not be made");
        }
        final MovingStructure structure = new MovingStructure(entity, id, plot, bounds, blocks, pivotX, pivotY,
                pivotZ, pose, null);
        entity.hold(structure);
        level.addFreshEntity(entity);
        if (arrival != null) {
            structure.arriving(arrival);
        }
        return structure;
    }

    /* Takes {@code structure} away for good: its entity gone, its plot given back, its blocks cleared if asked. */
    static void remove(final MovingStructure structure, final boolean clear) {
        final MovingStructureEntity entity = structure.entity();
        if (entity.isRemoved() || !(structure.level() instanceof ServerLevel level)) {
            return;
        }
        // What it held, with nothing to hold it now, falls; what goes with it to another dimension goes on.
        for (final MovingStructure child : StructureMounts.children(structure)) {
            if (!child.departing()) {
                StructureMounts.unmount(child, true);
            }
        }
        if (clear && !structure.arriving()) {
            StructureYard.clearAndGiveBack(level, structure.plot(), structure.bounds());
        } else {
            StructureYard.giveBack(level, structure.plot());
        }
        StructureJoints.forget(level, structure.id());
        entity.discard();
    }

    /* Works out the next poses of the structures moving in a level, beside the server's thread when many move. */
    private static void beforeTick(final LevelTickEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        final LevelMovingStructures all = LevelMovingStructures.of(level);
        if (all.isEmpty()) {
            return;
        }
        final List<MovingStructure> moving = new ArrayList<>();
        for (final IStructureBody body : all.all()) {
            if (body instanceof MovingStructure structure && !structure.resting()
                    && level.isPositionEntityTicking(structure.entity().blockPosition())) {
                moving.add(structure);
            }
        }
        if (moving.size() < ALONGSIDE) {
            for (final MovingStructure structure : moving) {
                structure.prepare();
            }
            return;
        }
        workers().submit(() -> moving.parallelStream().forEach(MovingStructure::prepare)).join();
    }

    private static void afterTick(final LevelTickEvent.Post event) {
        StructureFooting.tick(event.getLevel());
    }

    /*
     * Whatever is born among a structure's blocks in the yard, an item a block drops, a mob a spawner makes, the
     * lightning that strikes it, is put where that place is in the world, moving and facing as the structure has it.
     */
    private static void onJoin(final EntityJoinLevelEvent event) {
        final Entity entity = event.getEntity();
        if (entity instanceof MovingStructureEntity || entity instanceof StructureSeatEntity) {
            return;
        }
        if (!YardLayout.isYard(entity.getX())) {
            if (!event.loadedFromDisk()) {
                shotFromAboard(entity);
            }
            return;
        }
        final IStructureBody body = LevelMovingStructures.of(event.getLevel()).atYard((int) Math.floor(entity.getX()),
                (int) Math.floor(entity.getZ()));
        if (body == null) {
            return;
        }
        final StructureFrame frame = body.frame();
        final double x = entity.getX();
        final double y = entity.getY();
        final double z = entity.getZ();
        final Vec3 speed = entity.getDeltaMovement();
        entity.setPos(frame.worldX(x, y, z), frame.worldY(x, y, z), frame.worldZ(x, y, z));
        // It goes as fast as it was thrown, turned as the structure is, and as fast as its place on it goes.
        entity.setDeltaMovement(new Vec3(frame.turnX(speed.x, speed.y, speed.z), frame.turnY(speed.x, speed.y,
                speed.z), frame.turnZ(speed.x, speed.y, speed.z)).add(StructureFooting.speedAt(body,
                entity.position())));
        entity.setYRot((float) (entity.getYRot() + frame.pose().yawDegrees()));
    }

    /*
     * A shot or a thing thrown by someone standing or sitting on a structure goes as fast as they were thrown, and as
     * fast as the structure carries the place they were thrown from: an arrow loosed from a ship's deck flies on
     * with the ship.
     */
    private static void shotFromAboard(final Entity entity) {
        if (!LevelMovingStructures.anyLoaded() || entity.level().isClientSide()) {
            return;
        }
        final Entity thrower = entity instanceof Projectile shot ? shot.getOwner()
                : entity instanceof ItemEntity item ? item.getOwner() : null;
        if (thrower == null) {
            return;
        }
        IStructureBody body = StructureFooting.standingOn(thrower);
        if (body == null && thrower.getVehicle() instanceof StructureSeatEntity seat) {
            body = seat.structure();
        }
        if (body != null && body.ready()) {
            entity.setDeltaMovement(entity.getDeltaMovement().add(StructureFooting.speedAt(body,
                    thrower.position())));
        }
    }

    /* A block set down in the yard past the plot of the structure it is set on would be another's: it is refused. */
    private static void onPlace(final BlockEvent.EntityPlaceEvent event) {
        final BlockPos pos = event.getPos();
        if (!YardLayout.isYardBlock(pos.getX()) || !(event.getLevel() instanceof Level level)) {
            return;
        }
        final IStructureBody body = LevelMovingStructures.of(level).atYard(pos.getX(), pos.getZ());
        if (body == null) {
            event.setCanceled(true);
        }
    }

    /* A player who left the game standing on a structure comes back standing where they were on it. */
    private static void onLoggedIn(final PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        final Footing footing = player.getData(CoreAttachments.FOOTING.get());
        if (footing.structure().isEmpty()) {
            return;
        }
        if (!standAgain(player, footing)) {
            RETURNING.put(player, footing);
        }
    }

    /* Puts a returning player on their structure, if it is loaded; whether it was. */
    static boolean standAgain(final ServerPlayer player, final Footing footing) {
        final MovingStructure structure = footing.structure().map(id -> byId(player.serverLevel(), id)).orElse(null);
        if (structure == null || !structure.ready()) {
            return false;
        }
        final StructureFrame frame = structure.frame();
        player.teleportTo(player.serverLevel(), frame.worldX(footing.x(), footing.y(), footing.z()),
                frame.worldY(footing.x(), footing.y(), footing.z()), frame.worldZ(footing.x(), footing.y(),
                        footing.z()), (float) (frame.pose().yawDegrees() + footing.yaw()), player.getXRot());
        return true;
    }

    /* A structure finished loading: the players waiting for it stand on it again. */
    static void loaded(final MovingStructure structure) {
        if (RETURNING.isEmpty()) {
            return;
        }
        RETURNING.entrySet().removeIf(each -> each.getValue().structure().map(structure.id()::equals).orElse(false)
                && each.getKey().level() == structure.level() && standAgain(each.getKey(), each.getValue()));
    }

    private static ForkJoinPool workers() {
        ForkJoinPool pool = workers;
        if (pool == null) {
            synchronized (MovingStructures.class) {
                pool = workers;
                if (pool == null) {
                    pool = new ForkJoinPool(Math.max(1, Runtime.getRuntime().availableProcessors() / 2),
                            fork -> {
                                final ForkJoinWorkerThread thread =
                                        ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(fork);
                                thread.setName("jscore-moving-structures-" + thread.getPoolIndex());
                                thread.setDaemon(true);
                                return thread;
                            }, null, false);
                    workers = pool;
                }
            }
        }
        return pool;
    }
}
