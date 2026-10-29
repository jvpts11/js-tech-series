/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.moving;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.jstech.core.JsCore;
import dev.jstech.core.client.effect.ClientEffects;
import dev.jstech.core.heat.Blackbody;
import dev.jstech.core.heat.ThermalSight;
import dev.jstech.core.moving.StructureEffectsPayload;
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.moving.physics.BlockPhysics;
import dev.jstech.core.multiblock.FormedPartBlock;
import dev.jstech.core.region.Box;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.jetbrains.annotations.Nullable;

/**
 * What the moving structures a player's game sees show of themselves going, as their server tells: each engine
 * burning, a rocket's flame and long column of exhaust or a jet's short blue flame, as big as it pushes; each face
 * heading into the air hot enough to glow glowing on its outermost blocks in the colour of a black body at its heat,
 * wrapped in the plasma of a re-entry once hotter still; and where the air is cold, white trails behind its engines
 * and its wingtips. What a structure was told to show fades a few seconds after it was last told, so one gone quiet
 * without a word stops showing.
 */
public final class StructureEffectsClient {

    /* What each structure shows, by its entity. */
    private static final Int2ObjectMap<Shown> SHOWN = new Int2ObjectOpenHashMap<>();
    /** The effects a structure shows: a rocket's plume, a jet's, a trail, a re-entry's plasma. */
    public static final ResourceLocation ROCKET = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "rocket_plume");
    public static final ResourceLocation JET = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "jet_exhaust");
    public static final ResourceLocation CONTRAIL = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "contrail");
    public static final ResourceLocation PLASMA = ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
            "reentry_plasma");
    /* How long what a structure was told lasts on the player's game past the server's word, in ticks. */
    private static final int FADES = 60;
    /* A face hotter than this, in kelvin, is wrapped in the plasma of a re-entry. */
    private static final double PLASMA_FROM = 1_600;
    /* How often the outermost blocks of a glowing face are looked for again, in ticks, and the most columns looked. */
    private static final int LOOK_AGAIN = 40;
    private static final int MOST_COLUMNS = 16_384;
    /* How much of its block's face a glow at its whole covers, and how far out from the face it stands, in blocks. */
    private static final float MOST = 0.75F;
    private static final double GROW = 0.01;
    /* An engine's plume at its least push, as a share of its whole size; how finely its size follows its push. */
    private static final float LEAST_PLUME = 0.4F;
    private static final int PLUME_STEPS = 4;
    /* A re-entry's plasma, as a share of the width of the face it wraps, at most and at least. */
    private static final float PLASMA_WIDTH = 0.5F;
    private static final float PLASMA_LEAST = 1.0F;
    private static final float PLASMA_MOST = 6.0F;
    private static final double HALF = 0.5;
    private static final int RGB = 0xFFFFFF;
    private static final int ALPHA_SHIFT = 24;
    private static final int CHANNEL = 255;

    private StructureEffectsClient() {
    }

    /** Hands the game's bus what this does, once, from the Core's client. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, StructureEffectsClient::onTick);
        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.class, StructureEffectsClient::onRender);
        NeoForge.EVENT_BUS.addListener(LevelEvent.Unload.class, event -> {
            if (event.getLevel().isClientSide()) {
                clear();
            }
        });
    }

    /** What the structure of entity {@code entity} shows now, as the server last told, or null for nothing. */
    public static @Nullable StructureEffectsPayload shown(final int entity) {
        final Shown shown = SHOWN.get(entity);
        return shown == null ? null : shown.payload;
    }

    /** How many effects the structure of entity {@code entity} holds now: its engines, trails and plasma. */
    public static int held(final int entity) {
        final Shown shown = SHOWN.get(entity);
        return shown == null ? 0 : shown.engines.size() + shown.trails.size() + shown.plasma.size();
    }

    /** The server told what a structure shows of itself going. */
    static void told(final StructureEffectsPayload payload) {
        final ClientLevel level = Minecraft.getInstance().level;
        final ClientStructure structure = ClientMovingStructures.byEntity(payload.entity());
        if (level == null || structure == null) {
            return;
        }
        if (!payload.any()) {
            final Shown gone = SHOWN.remove(payload.entity());
            if (gone != null) {
                gone.release();
            }
            return;
        }
        final Shown shown = SHOWN.computeIfAbsent(payload.entity(), any -> new Shown(payload.entity()));
        shown.payload = payload;
        shown.toldAt = level.getGameTime();
        shown.engines(level);
        shown.trails();
        shown.plasma(structure);
    }

    /* Each tick, what was told too long ago, or of a structure gone, fades. */
    private static void onTick(final ClientTickEvent.Post event) {
        final ClientLevel level = Minecraft.getInstance().level;
        if (level == null || SHOWN.isEmpty()) {
            return;
        }
        final Iterator<Shown> each = SHOWN.values().iterator();
        while (each.hasNext()) {
            final Shown shown = each.next();
            if (ClientMovingStructures.byEntity(shown.entity) == null
                    || level.getGameTime() - shown.toldAt > FADES) {
                shown.release();
                each.remove();
            }
        }
    }

    private static void clear() {
        for (final Shown shown : SHOWN.values()) {
            shown.release();
        }
        SHOWN.clear();
    }

    /* The faces of the structures hot enough to glow, on their outermost blocks, where each is drawn this frame. */
    private static void onRender(final RenderLevelStageEvent event) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || SHOWN.isEmpty()
                || minecraft.player == null || minecraft.level == null || ThermalSight.sees(minecraft.player)) {
            return;
        }
        final float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        final Vec3 camera = event.getCamera().getPosition();
        final PoseStack poses = event.getPoseStack();
        final MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = null;
        poses.pushPose();
        poses.translate(-camera.x, -camera.y, -camera.z);
        final PoseStack.Pose pose = poses.last();
        for (final Shown shown : SHOWN.values()) {
            final ClientStructure structure = ClientMovingStructures.byEntity(shown.entity);
            if (structure == null || !structure.ready() || StructureHandOffs.hidden(structure)) {
                continue;
            }
            final StructureFrame frame = new StructureFrame(structure.drawnAt(partialTick), structure.pivotX(),
                    structure.pivotY(), structure.pivotZ());
            for (final Direction face : Direction.values()) {
                final double kelvin = shown.kelvin(face);
                if (!Blackbody.glows(kelvin)) {
                    continue;
                }
                if (consumer == null) {
                    consumer = buffers.getBuffer(RenderType.debugQuads());
                }
                final int alpha = Math.round((float) Blackbody.glow(kelvin) * MOST * CHANNEL);
                final int colour = alpha << ALPHA_SHIFT | Blackbody.colour(kelvin) & RGB;
                for (final long block : shown.outermost(minecraft.level, structure, face)) {
                    glow(consumer, pose, frame, block, face, colour);
                }
            }
        }
        poses.popPose();
        if (consumer != null) {
            buffers.endBatch(RenderType.debugQuads());
        }
    }

    /* The glow over the face looking {@code face} of the block at the yard's packed place {@code block}. */
    private static void glow(final VertexConsumer consumer, final PoseStack.Pose pose, final StructureFrame frame,
                             final long block, final Direction face, final int colour) {
        final double x = BlockPos.getX(block);
        final double y = BlockPos.getY(block);
        final double z = BlockPos.getZ(block);
        // The face's middle, pushed out a little, and the two ways across it.
        final double mx = x + HALF + (HALF + GROW) * face.getStepX();
        final double my = y + HALF + (HALF + GROW) * face.getStepY();
        final double mz = z + HALF + (HALF + GROW) * face.getStepZ();
        final Direction.Axis axis = face.getAxis();
        final double[] u = axis == Direction.Axis.X ? new double[] {0, 0, HALF} : new double[] {HALF, 0, 0};
        final double[] v = axis == Direction.Axis.Y ? new double[] {0, 0, HALF} : new double[] {0, HALF, 0};
        if (face.getAxisDirection() == Direction.AxisDirection.NEGATIVE) {
            u[0] = -u[0];
            u[1] = -u[1];
            u[2] = -u[2];
        }
        corner(consumer, pose, frame, mx - u[0] - v[0], my - u[1] - v[1], mz - u[2] - v[2], colour);
        corner(consumer, pose, frame, mx + u[0] - v[0], my + u[1] - v[1], mz + u[2] - v[2], colour);
        corner(consumer, pose, frame, mx + u[0] + v[0], my + u[1] + v[1], mz + u[2] + v[2], colour);
        corner(consumer, pose, frame, mx - u[0] + v[0], my - u[1] + v[1], mz - u[2] + v[2], colour);
    }

    private static void corner(final VertexConsumer consumer, final PoseStack.Pose pose, final StructureFrame frame,
                               final double x, final double y, final double z, final int colour) {
        consumer.addVertex(pose, (float) frame.worldX(x, y, z), (float) frame.worldY(x, y, z),
                (float) frame.worldZ(x, y, z)).setColor(colour);
    }

    /* Where the yard's place {@code (x, y, z)} of the structure of entity {@code entity} is in the world now. */
    private static @Nullable Vec3 worldOf(final int entity, final double x, final double y, final double z) {
        final ClientStructure structure = ClientMovingStructures.byEntity(entity);
        if (structure == null) {
            return null;
        }
        final StructureFrame frame = structure.frame();
        return new Vec3(frame.worldX(x, y, z), frame.worldY(x, y, z), frame.worldZ(x, y, z));
    }

    /* The world's way of the structure's way {@code (x, y, z)}, as the structure of {@code entity} stands now. */
    private static Vec3 turned(final int entity, final double x, final double y, final double z) {
        final ClientStructure structure = ClientMovingStructures.byEntity(entity);
        if (structure == null) {
            return new Vec3(x, y, z);
        }
        final StructureFrame frame = structure.frame();
        return new Vec3(frame.turnX(x, y, z), frame.turnY(x, y, z), frame.turnZ(x, y, z));
    }

    /* What a structure shows, and the effects it holds for it. */
    private static final class Shown {

        private final int entity;
        private final Long2ObjectMap<Burning> engines = new Long2ObjectOpenHashMap<>();
        private final Long2ObjectMap<Object> trails = new Long2ObjectOpenHashMap<>();
        private final Map<Direction, Object> plasma = new EnumMap<>(Direction.class);
        /* The outermost blocks of each glowing face, by its way, and when they were last looked for. */
        private final Map<Direction, LongList> outermost = new EnumMap<>(Direction.class);
        private final Map<Direction, Long> lookedAt = new EnumMap<>(Direction.class);
        private StructureEffectsPayload payload;
        private long toldAt;
        /* Where it was going, the world's way back from it, as last worked out. */
        private Vec3 back = new Vec3(0, -1, 0);
        private @Nullable Vec3 lastPivot;

        private Shown(final int entity) {
            this.entity = entity;
        }

        /* How hot the face looking {@code face} is, in kelvin; nought for one not heated. */
        private double kelvin(final Direction face) {
            return payload.kelvins().get(face.get3DDataValue());
        }

        /* Holds a plume for each engine that burns, as big as it pushes, and lets go of those gone out. */
        private void engines(final ClientLevel level) {
            final LongSet burning = new LongOpenHashSet();
            for (final StructureEffectsPayload.Engine engine : payload.engines()) {
                if (engine.look() == StructureEffectsPayload.Engine.PROPELLER) {
                    continue;
                }
                burning.add(engine.pos());
                final int size = (engine.strength() * PLUME_STEPS + StructureEffectsPayload.STEPS - 1)
                        / StructureEffectsPayload.STEPS;
                final Burning was = engines.get(engine.pos());
                if (was != null && was.size == size && was.look == engine.look()) {
                    continue;
                }
                final BlockPos pos = BlockPos.of(engine.pos());
                // An engine merged into a formed structure's model burns the way the engine it was faces.
                final Direction back = BlockPhysics.wayOf(FormedPartBlock.originalAt(level, pos,
                        level.getBlockState(pos))).getOpposite();
                final Burning now = new Burning(size, engine.look());
                if (was != null) {
                    ClientEffects.release(was);
                }
                engines.put(engine.pos(), now);
                final double x = pos.getX() + HALF + HALF * back.getStepX();
                final double y = pos.getY() + HALF + HALF * back.getStepY();
                final double z = pos.getZ() + HALF + HALF * back.getStepZ();
                final float scale = LEAST_PLUME + (1 - LEAST_PLUME) * size / PLUME_STEPS;
                ClientEffects.holdScaled(now, engine.look() == StructureEffectsPayload.Engine.ROCKET ? ROCKET : JET,
                        () -> orHere(worldOf(entity, x, y, z)),
                        () -> turned(entity, back.getStepX(), back.getStepY(), back.getStepZ()), scale);
            }
            releaseAllBut(engines, burning);
        }

        /* Holds a trail behind each of its sources while it leaves trails, and lets go of the rest. */
        private void trails() {
            final LongSet sources = new LongOpenHashSet();
            if (payload.contrail()) {
                for (final long source : payload.trails()) {
                    sources.add(source);
                    if (trails.containsKey(source)) {
                        continue;
                    }
                    final Object holder = new Object();
                    trails.put(source, holder);
                    final double x = BlockPos.getX(source) + HALF;
                    final double y = BlockPos.getY(source) + HALF;
                    final double z = BlockPos.getZ(source) + HALF;
                    ClientEffects.hold(holder, CONTRAIL, () -> orHere(worldOf(entity, x, y, z)), this::backWay);
                }
            }
            releaseAllBut(trails, sources);
        }

        /* Wraps each face hot enough in a re-entry's plasma, as wide as the face, and lets go of the rest. */
        private void plasma(final ClientStructure structure) {
            final Box box = structure.bounds();
            for (final Direction face : Direction.values()) {
                final boolean hot = kelvin(face) >= PLASMA_FROM;
                final Object was = plasma.get(face);
                if (hot && was == null) {
                    final Object holder = new Object();
                    plasma.put(face, holder);
                    final double x = (box.minX() + box.maxX() + 1) * HALF + (box.maxX() + 1 - box.minX()) * HALF
                            * face.getStepX();
                    final double y = (box.minY() + box.maxY() + 1) * HALF + (box.maxY() + 1 - box.minY()) * HALF
                            * face.getStepY();
                    final double z = (box.minZ() + box.maxZ() + 1) * HALF + (box.maxZ() + 1 - box.minZ()) * HALF
                            * face.getStepZ();
                    final double across = face.getAxis() == Direction.Axis.Y ? Math.max(box.maxX() - box.minX(),
                            box.maxZ() - box.minZ()) : face.getAxis() == Direction.Axis.X
                            ? Math.max(box.maxY() - box.minY(), box.maxZ() - box.minZ())
                            : Math.max(box.maxX() - box.minX(), box.maxY() - box.minY());
                    final float scale = Math.max(PLASMA_LEAST, Math.min(PLASMA_MOST,
                            (float) (across + 1) * PLASMA_WIDTH));
                    ClientEffects.holdScaled(holder, PLASMA, () -> orHere(worldOf(entity, x, y, z)),
                            () -> turned(entity, face.getStepX(), face.getStepY(), face.getStepZ()), scale);
                } else if (!hot && was != null) {
                    ClientEffects.release(was);
                    plasma.remove(face);
                }
            }
        }

        /* The outermost blocks of the face looking {@code face}: the first one met in each column from that side. */
        private LongList outermost(final ClientLevel level, final ClientStructure structure, final Direction face) {
            final Long looked = lookedAt.get(face);
            final LongList known = outermost.get(face);
            if (known != null && looked != null && level.getGameTime() - looked < LOOK_AGAIN) {
                return known;
            }
            final LongList found = new LongArrayList();
            final Box box = structure.bounds();
            final Direction.Axis axis = face.getAxis();
            final int[] min = {box.minX(), box.minY(), box.minZ()};
            final int[] max = {box.maxX(), box.maxY(), box.maxZ()};
            final int along = switch (axis) {
                case X -> 0;
                case Y -> 1;
                case Z -> 2;
            };
            final int first = along == 0 ? 1 : 0;
            final int second = along == 2 ? 1 : 2;
            final long columns = (long) (max[first] - min[first] + 1) * (max[second] - min[second] + 1);
            if (columns <= MOST_COLUMNS) {
                final boolean positive = face.getAxisDirection() == Direction.AxisDirection.POSITIVE;
                final int[] at = new int[3];
                final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
                for (int a = min[first]; a <= max[first]; a++) {
                    for (int b = min[second]; b <= max[second]; b++) {
                        at[first] = a;
                        at[second] = b;
                        for (int c = positive ? max[along] : min[along]; positive ? c >= min[along]
                                : c <= max[along]; c += positive ? -1 : 1) {
                            at[along] = c;
                            pos.set(at[0], at[1], at[2]);
                            if (!level.getBlockState(pos).isAir()) {
                                found.add(pos.asLong());
                                break;
                            }
                        }
                    }
                }
            }
            outermost.put(face, found);
            lookedAt.put(face, level.getGameTime());
            return found;
        }

        /* The world's way back from where it goes, as it goes now; straight down while it is still. */
        private Vec3 backWay() {
            final ClientStructure structure = ClientMovingStructures.byEntity(entity);
            if (structure == null) {
                return back;
            }
            final StructureFrame frame = structure.frame();
            final Vec3 pivot = new Vec3(frame.worldX(structure.pivotX(), structure.pivotY(), structure.pivotZ()),
                    frame.worldY(structure.pivotX(), structure.pivotY(), structure.pivotZ()),
                    frame.worldZ(structure.pivotX(), structure.pivotY(), structure.pivotZ()));
            if (lastPivot != null && pivot.distanceToSqr(lastPivot) > 0) {
                back = lastPivot.subtract(pivot).normalize();
            }
            lastPivot = pivot;
            return back;
        }

        /* Lets go of every effect it holds. */
        private void release() {
            engines.values().forEach(ClientEffects::release);
            trails.values().forEach(ClientEffects::release);
            plasma.values().forEach(ClientEffects::release);
            engines.clear();
            trails.clear();
            plasma.clear();
        }

        private static <T> void releaseAllBut(final Long2ObjectMap<T> held, final LongSet kept) {
            final Iterator<Long2ObjectMap.Entry<T>> each = held.long2ObjectEntrySet().iterator();
            while (each.hasNext()) {
                final Long2ObjectMap.Entry<T> entry = each.next();
                if (!kept.contains(entry.getLongKey())) {
                    ClientEffects.release(entry.getValue());
                    each.remove();
                }
            }
        }

        private Vec3 orHere(final @Nullable Vec3 at) {
            if (at != null) {
                return at;
            }
            return lastPivot == null ? Vec3.ZERO : lastPivot;
        }
    }

    /* An engine's plume held: how big, in steps of its push, and what it looks like; each is its own holder. */
    private static final class Burning {

        private final int size;
        private final byte look;

        private Burning(final int size, final byte look) {
            this.size = size;
            this.look = look;
        }
    }
}
