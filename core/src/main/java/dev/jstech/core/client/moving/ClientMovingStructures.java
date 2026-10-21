/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.moving;

import dev.jstech.core.client.light.DynamicLightsClient;
import dev.jstech.core.client.light.IDynamicLightSource;
import dev.jstech.core.moving.IStructureBody;
import dev.jstech.core.moving.LevelMovingStructures;
import dev.jstech.core.moving.MovingStructureEntity;
import dev.jstech.core.moving.StructureClientHooks;
import dev.jstech.core.moving.StructureCollisions;
import dev.jstech.core.moving.StructureControls;
import dev.jstech.core.moving.StructureEffectsPayload;
import dev.jstech.core.moving.StructureFooting;
import dev.jstech.core.moving.StructureFootingPayload;
import dev.jstech.core.moving.StructureForcesPayload;
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.moving.StructureHandOffPayload;
import dev.jstech.core.moving.StructureHelmPayload;
import dev.jstech.core.moving.StructurePosePayload;
import dev.jstech.core.moving.StructurePose;
import dev.jstech.core.moving.StructureSeatEntity;
import dev.jstech.core.moving.YardLayout;
import dev.jstech.core.region.Box;
import dev.jstech.core.uuid.StructureUuid;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * The moving structures a player's game sees: each made as its entity comes into the player's world and let go as it
 * leaves, moved a tick at a time from the poses the server tells. Each tick, before the player moves, the structure
 * the player stands on carries them, so a deck feels steady underfoot; after the player moved, the server is told
 * where on the deck they stand.
 */
public final class ClientMovingStructures implements StructureClientHooks.IHooks {

    private static final Int2ObjectMap<ClientStructure> BY_ENTITY = new Int2ObjectOpenHashMap<>();
    private static final Int2ObjectMap<ClientStructure> BY_PLOT = new Int2ObjectOpenHashMap<>();
    /* How often a player at a helm tells the server what they hold, unchanged, in ticks. */
    private static final int HELM_AGAIN = 10;
    private static boolean footed;
    /* Whether the server was told the vehicle the player drives stands on a structure. */
    private static boolean vehicleFooted;
    private static int helmKeys;

    private ClientMovingStructures() {
    }

    /** Hands the kit this game's answers and the game's bus what it answers, once, as the player's game starts. */
    public static void listen() {
        StructureClientHooks.set(new ClientMovingStructures());
        DynamicLightsClient.provide(ClientMovingStructures::lamps);
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Pre.class, ClientMovingStructures::beforeTick);
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, ClientMovingStructures::afterTick);
        NeoForge.EVENT_BUS.addListener(PlayerTickEvent.Post.class, ClientMovingStructures::afterPlayer);
        NeoForge.EVENT_BUS.addListener(LevelEvent.Unload.class, ClientMovingStructures::onUnload);
    }

    /** The structure whose blocks are in plot {@code plot} of the yard, or null. */
    public static @Nullable ClientStructure byPlot(final int plot) {
        return plot < 0 ? null : BY_PLOT.get(plot);
    }

    /** The structure of {@code entity}, or null when the game does not hold it. */
    public static @Nullable ClientStructure of(final MovingStructureEntity entity) {
        return BY_ENTITY.get(entity.getId());
    }

    /** The structure whose entity's id is {@code entity}, or null when the game does not hold it. */
    public static @Nullable ClientStructure byEntity(final int entity) {
        return BY_ENTITY.get(entity);
    }

    /** Every structure the player's game holds. */
    public static Collection<ClientStructure> all() {
        return BY_ENTITY.values();
    }

    /**
     * Where the place {@code place} is drawn in the world this frame, {@code partialTick} of the way through the tick:
     * a place of the yard among a structure's blocks is where that structure is drawn with them; any other place is
     * where it is.
     */
    public static Vec3 drawnInWorld(final Vec3 place, final float partialTick) {
        if (!YardLayout.isYard(place.x)) {
            return place;
        }
        final ClientStructure structure = byPlot(YardLayout.plotOfBlock((int) Math.floor(place.x),
                (int) Math.floor(place.z)));
        if (structure == null || StructureHandOffs.hidden(structure)) {
            return place;
        }
        final StructureFrame frame = new StructureFrame(structure.drawnAt(partialTick), structure.pivotX(),
                structure.pivotY(), structure.pivotZ());
        return new Vec3(frame.worldX(place.x, place.y, place.z), frame.worldY(place.x, place.y, place.z),
                frame.worldZ(place.x, place.y, place.z));
    }

    /** The game begins to build the section {@code key} of the world, from its blocks as they are now. */
    public static void buildBegun(final long key) {
        StructureHandOffs.buildBegun(key);
    }

    /** A build of the section {@code key} of the world is drawn; on any thread. */
    public static void built(final long key) {
        StructureHandOffs.built(key);
    }

    /** Whether a structure's blocks are changing hands with the world's on the player's game now. */
    public static boolean handingOver() {
        return StructureHandOffs.busy();
    }

    /**
     * The game is to build the section of the world at {@code (x, y, z)} again: whether it waits, as a structure's
     * blocks change hands with the world's there ({@link StructureHandOffs}).
     */
    public static boolean holdsBack(final int x, final int y, final int z) {
        return StructureHandOffs.holdsBack(x, y, z);
    }

    /** The blocks giving light of the structures set down and still drawn where they land, where they are. */
    public static List<IDynamicLightSource.Light> landedLamps() {
        final List<IDynamicLightSource.Light> out = new ArrayList<>();
        for (final ClientStructure structure : StructureHandOffs.landed()) {
            structure.meshes().lamps(structure.frame(), out);
        }
        return out;
    }

    /** Whether the entity of a structure the player's game holds stands in the block {@code pos} of the world. */
    public static boolean pivotAt(final BlockPos pos) {
        for (final ClientStructure structure : BY_ENTITY.values()) {
            if (structure.entity().blockPosition().equals(pos)) {
                return true;
            }
        }
        return false;
    }

    /** The sections of the yard holding the blocks of each structure reaching within {@code reach} of {@code at}. */
    public static List<Long> yardSectionsNear(final Vec3 at, final double reach) {
        final List<Long> out = new ArrayList<>();
        for (final ClientStructure structure : BY_ENTITY.values()) {
            if (!structure.ready() || structure.worldBox().distanceToSqr(at) > reach * reach) {
                continue;
            }
            final Box box = structure.bounds();
            for (int x = box.minX() >> 4; x <= box.maxX() >> 4; x++) {
                for (int y = box.minY() >> 4; y <= box.maxY() >> 4; y++) {
                    for (int z = box.minZ() >> 4; z <= box.maxZ() >> 4; z++) {
                        out.add(SectionPos.asLong(x, y, z));
                    }
                }
            }
        }
        return out;
    }

    @Override
    public void added(final MovingStructureEntity entity) {
        final ClientStructure structure = new ClientStructure(entity);
        final ClientStructure before = BY_ENTITY.put(entity.getId(), structure);
        if (before != null) {
            forget(before);
        }
        BY_PLOT.put(structure.plot(), structure);
        LevelMovingStructures.of(entity.level()).add(structure);
        structure.meshes().boundsChanged();
    }

    @Override
    public void removed(final MovingStructureEntity entity) {
        final ClientStructure structure = BY_ENTITY.remove(entity.getId());
        if (structure != null) {
            forget(structure);
        }
    }

    @Override
    public void changed(final MovingStructureEntity entity) {
        final ClientStructure structure = BY_ENTITY.get(entity.getId());
        if (structure == null) {
            return;
        }
        final int plot = structure.plot();
        final StructureUuid id = structure.id();
        structure.changed();
        if (plot != structure.plot() || !id.equals(structure.id())) {
            if (BY_PLOT.get(plot) == structure) {
                BY_PLOT.remove(plot);
            }
            BY_PLOT.put(structure.plot(), structure);
            LevelMovingStructures.of(entity.level()).remove(id);
            LevelMovingStructures.of(entity.level()).add(structure);
        }
        LevelMovingStructures.of(entity.level()).moved(structure);
    }

    @Override
    public @Nullable AABB box(final MovingStructureEntity entity) {
        final ClientStructure structure = BY_ENTITY.get(entity.getId());
        return structure == null ? null : structure.worldBox();
    }

    @Override
    public void pose(final StructurePosePayload payload) {
        final ClientStructure structure = BY_ENTITY.get(payload.entity());
        if (structure != null) {
            structure.told(payload);
        }
    }

    @Override
    public void handOff(final StructureHandOffPayload payload) {
        final ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            StructureHandOffs.told(payload, level, level.getGameTime());
        }
    }

    @Override
    public void effects(final StructureEffectsPayload payload) {
        StructureEffectsClient.told(payload);
    }

    @Override
    public void forces(final StructureForcesPayload payload) {
        StructureForcesClient.told(payload);
    }

    /*
     * The structure's entity has left: it no longer collides, carries or answers for its plot; its drawing is let go
     * too, unless it is kept drawn where it landed until the land is drawn with its blocks.
     */
    private static void forget(final ClientStructure structure) {
        if (BY_PLOT.get(structure.plot()) == structure) {
            BY_PLOT.remove(structure.plot());
        }
        LevelMovingStructures.of(structure.level()).remove(structure.id());
        if (!StructureHandOffs.keeps(structure)) {
            structure.meshes().close();
        }
    }

    /* The lights the blocks of the structures the player's game holds lay on the world round them as they go. */
    private static List<IDynamicLightSource.Light> lamps(final ClientLevel level) {
        final List<IDynamicLightSource.Light> out = new ArrayList<>();
        for (final ClientStructure structure : BY_ENTITY.values()) {
            if (structure.level() == level && structure.ready()) {
                structure.meshes().lamps(structure.frame(), out);
            }
        }
        for (final ClientStructure structure : StructureHandOffs.landed()) {
            if (structure.level() == level) {
                structure.meshes().lamps(structure.frame(), out);
            }
        }
        return out;
    }

    /* Each tick: every structure takes its pose, the player is carried by the one they stand on and pushed out. */
    private static void beforeTick(final ClientTickEvent.Pre event) {
        final Minecraft game = Minecraft.getInstance();
        if (game.level == null || BY_ENTITY.isEmpty()) {
            return;
        }
        // A structure held by another takes its place on its parent once the parent has taken its own.
        final List<ClientStructure> ordered = new ArrayList<>(BY_ENTITY.values());
        ordered.sort(Comparator.comparingInt(ClientStructure::depth));
        for (final ClientStructure structure : ordered) {
            structure.tick();
            final MovingStructureEntity entity = structure.entity();
            final StructurePose was = structure.previousFrame().pose();
            final StructurePose now = structure.frame().pose();
            entity.setPos(now.x(), now.y(), now.z());
            entity.xOld = was.x();
            entity.yOld = was.y();
            entity.zOld = was.z();
            entity.xo = was.x();
            entity.yo = was.y();
            entity.zo = was.z();
            LevelMovingStructures.of(structure.level()).moved(structure);
        }
        final LocalPlayer player = game.player;
        if (player == null) {
            return;
        }
        // A player riding something is where it puts them, a seat aboard sunk into its block: neither carried by a
        // deck nor pushed out of the block they sit in, which would set them off their seat between its updates.
        if (!player.isPassenger()) {
            final IStructureBody standing = StructureFooting.standingOn(player);
            if (standing instanceof ClientStructure structure && structure.ready()) {
                StructureFooting.carry(player, structure);
            }
            final List<IStructureBody> near = new ArrayList<>(LevelMovingStructures.of(player.level())
                    .near(player.getBoundingBox().inflate(0.5)));
            for (final IStructureBody body : near) {
                if (body.ready() && !player.isSpectator() && !player.noPhysics) {
                    StructureCollisions.pushOut(player, body);
                }
            }
        } else if (drivenHere(player) instanceof Entity driven) {
            // What the player drives on this game, a car, a boat, a horse, is carried by the deck under it as they
            // are on foot: their game moves it, and the server only hears where it went.
            final IStructureBody standing = StructureFooting.standingOn(driven);
            if (standing instanceof ClientStructure structure && structure.ready()) {
                StructureFooting.carry(driven, structure);
            }
            for (final IStructureBody body : new ArrayList<>(LevelMovingStructures.of(player.level())
                    .near(driven.getBoundingBox().inflate(0.5)))) {
                if (body.ready()) {
                    StructureCollisions.pushOut(driven, body);
                }
            }
        }
        final Vec3 eye = game.gameRenderer.getMainCamera().getPosition();
        for (final ClientStructure structure : BY_ENTITY.values()) {
            final StructureFrame frame = structure.frame();
            structure.meshes().tick(new Vec3(frame.yardX(eye.x, eye.y, eye.z), frame.yardY(eye.x, eye.y, eye.z),
                    frame.yardZ(eye.x, eye.y, eye.z)));
        }
        StructureAmbience.tick(game);
    }

    /*
     * After the player moved, and before the game sends where to: the server is told where on the deck they stand;
     * at a helm, what controls they hold.
     */
    private static void afterPlayer(final PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof LocalPlayer player)) {
            return;
        }
        if (player.isPassenger()) {
            // Sitting, they stand on no deck: the server is told so, or it would put them where they last stood.
            if (footed) {
                PacketDistributor.sendToServer(StructureFootingPayload.none());
                footed = false;
            }
            if (player.getVehicle() instanceof StructureSeatEntity seat && seat.helm()) {
                helm(player, seat);
            }
            reportDriven(player);
            return;
        }
        if (vehicleFooted) {
            PacketDistributor.sendToServer(StructureFootingPayload.noVehicle());
            vehicleFooted = false;
        }
        final IStructureBody standing = StructureFooting.standingOn(player);
        if (standing instanceof ClientStructure structure && structure.ready()) {
            final StructureFrame frame = structure.frame();
            final double[] place = {frame.yardX(player.getX(), player.getY(), player.getZ()),
                    frame.yardY(player.getX(), player.getY(), player.getZ()),
                    frame.yardZ(player.getX(), player.getY(), player.getZ())};
            PacketDistributor.sendToServer(new StructureFootingPayload(structure.entity().getId(), place, false));
            footed = true;
        } else if (footed) {
            PacketDistributor.sendToServer(StructureFootingPayload.none());
            footed = false;
        }
    }

    /*
     * After the entities moved: what stands on each structure going is put where the server had it on it, at the
     * structure's tick drawn now, so it is drawn on the deck as the deck is drawn; the player's own game moves the
     * player, and is let be.
     */
    private static void afterTick(final ClientTickEvent.Post event) {
        final ClientLevel level = Minecraft.getInstance().level;
        if (level == null || BY_ENTITY.isEmpty()) {
            return;
        }
        for (final ClientStructure structure : BY_ENTITY.values()) {
            if (structure.riders().isEmpty() || !structure.ready()) {
                continue;
            }
            final StructureFrame now = structure.frame();
            final StructureFrame before = structure.previousFrame();
            final double yaw = now.pose().yawDegrees();
            final double yawBefore = before.pose().yawDegrees();
            for (final Int2ObjectMap.Entry<double[]> each : structure.riders().int2ObjectEntrySet()) {
                final Entity entity = level.getEntity(each.getIntKey());
                if (entity == null || entity.isControlledByLocalInstance() || entity.isPassenger()) {
                    continue;
                }
                final double[] at = each.getValue();
                final double[] was = structure.previousRiders().getOrDefault(each.getIntKey(), at);
                entity.setPos(now.worldX(at[0], at[1], at[2]), now.worldY(at[0], at[1], at[2]),
                        now.worldZ(at[0], at[1], at[2]));
                entity.xo = before.worldX(was[0], was[1], was[2]);
                entity.yo = before.worldY(was[0], was[1], was[2]);
                entity.zo = before.worldZ(was[0], was[1], was[2]);
                entity.xOld = entity.xo;
                entity.yOld = entity.yo;
                entity.zOld = entity.zo;
                entity.setYRot((float) (yaw + at[3]));
                entity.yRotO = (float) (yawBefore + was[3]);
                if (entity instanceof LivingEntity living) {
                    living.yBodyRot = entity.getYRot();
                    living.yBodyRotO = entity.yRotO;
                }
            }
        }
    }

    /* What the player drives that their game moves, at the bottom of what they ride; null for none. */
    private static @Nullable Entity drivenHere(final LocalPlayer player) {
        final Entity root = player.getRootVehicle();
        return root != player && !(root instanceof StructureSeatEntity) && root.isControlledByLocalInstance()
                ? root : null;
    }

    /* Where on a deck what the player drives stands, told the server before the game sends where it went. */
    private static void reportDriven(final LocalPlayer player) {
        final Entity driven = drivenHere(player);
        final IStructureBody standing = driven == null ? null : StructureFooting.standingOn(driven);
        if (driven != null && standing instanceof ClientStructure structure && structure.ready()) {
            final StructureFrame frame = structure.frame();
            final double[] place = {frame.yardX(driven.getX(), driven.getY(), driven.getZ()),
                    frame.yardY(driven.getX(), driven.getY(), driven.getZ()),
                    frame.yardZ(driven.getX(), driven.getY(), driven.getZ())};
            PacketDistributor.sendToServer(new StructureFootingPayload(structure.entity().getId(), place, true));
            vehicleFooted = true;
        } else if (vehicleFooted) {
            PacketDistributor.sendToServer(StructureFootingPayload.noVehicle());
            vehicleFooted = false;
        }
    }

    /* The controls a player at a helm holds, told the server as they change and once a second besides. */
    private static void helm(final LocalPlayer player, final StructureSeatEntity seat) {
        final Minecraft game = Minecraft.getInstance();
        int keys = 0;
        if (player.input.forwardImpulse > 0) {
            keys |= StructureHelmPayload.bit(StructureControls.ControlAxis.FORWARD);
        }
        if (player.input.forwardImpulse < 0) {
            keys |= StructureHelmPayload.bit(StructureControls.ControlAxis.BACK);
        }
        if (player.input.leftImpulse > 0) {
            keys |= StructureHelmPayload.bit(StructureControls.ControlAxis.LEFT);
        }
        if (player.input.leftImpulse < 0) {
            keys |= StructureHelmPayload.bit(StructureControls.ControlAxis.RIGHT);
        }
        if (game.options.keyJump.isDown()) {
            keys |= StructureHelmPayload.bit(StructureControls.ControlAxis.UP);
        }
        if (game.options.keySprint.isDown()) {
            keys |= StructureHelmPayload.bit(StructureControls.ControlAxis.DOWN);
        }
        if (keys != helmKeys || player.tickCount % HELM_AGAIN == 0) {
            helmKeys = keys;
            PacketDistributor.sendToServer(new StructureHelmPayload(seat.getId(), keys));
        }
    }

    private static void onUnload(final LevelEvent.Unload event) {
        if (!event.getLevel().isClientSide()) {
            return;
        }
        for (final ClientStructure structure : List.copyOf(BY_ENTITY.values())) {
            if (structure.level() == event.getLevel()) {
                BY_ENTITY.remove(structure.entity().getId());
                forget(structure);
            }
        }
        StructureHandOffs.clear();
        footed = false;
    }
}
