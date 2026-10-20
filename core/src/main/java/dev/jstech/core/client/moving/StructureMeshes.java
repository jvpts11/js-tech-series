/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.moving;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.blaze3d.vertex.VertexSorting;
import dev.jstech.core.client.light.DynamicLightsClient;
import dev.jstech.core.client.light.IDynamicLightSource;
import dev.jstech.core.light.LightColours;
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.moving.StructurePose;
import dev.jstech.core.moving.YardLayout;
import dev.jstech.core.region.Box;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;
import org.jetbrains.annotations.Nullable;

/**
 * A moving structure's blocks drawn as cached meshes, a section of sixteen blocks a side at a time: each section is
 * built once by the game's own section builder, so every block and fluid looks as it does on the ground, and built
 * again only when a block of it changes. The mesh is kept on the graphics card to be drawn as it is, and in memory to
 * be lit again and handed to a shader pack's drawing. Each section takes the light of the world where it is, so a
 * structure flying into a cave goes dark, and keeps its own inside, so a lamp aboard lights its deck.
 */
public final class StructureMeshes {

    private final ClientStructure structure;
    private final Long2ObjectMap<Section> sections = new Long2ObjectOpenHashMap<>();
    private final LongSet dirty = new LongOpenHashSet();
    private final List<BlockEntity> blockEntities = new ArrayList<>();
    /* The turn the faces are shaded for; and the world's light where the structure last was, for a new section. */
    private final Quaternionf shadedFor = new Quaternionf();
    /* The way a face faces, worked on as each vertex is shaded, on the render thread. */
    private final Vector3f facing = new Vector3f();
    private boolean blockEntitiesStale;
    private int lastWorldSky = LIGHTEST;
    private int lastWorldBlock;
    /* Whether its blocks are drawn as they are from now on, the structure being set down. */
    private boolean frozen;

    /* The brightest light. */
    private static final int LIGHTEST = 15;
    /* How far the structure turns before its faces are shaded again: the cosine of half of a degree and a half. */
    private static final float SHADE_AGAIN = (float) Math.cos(Math.toRadians(0.75));
    /* Whether the section being built here is built with its faces unshaded, to be shaded as the structure turns. */
    private static final ThreadLocal<Boolean> SHADING_LATER = ThreadLocal.withInitial(() -> false);
    /* The size of a vertex of a block's mesh, and where its light is in it. */
    private static final int VERTEX = DefaultVertexFormat.BLOCK.getVertexSize();
    private static final int LIGHT = DefaultVertexFormat.BLOCK.getOffset(VertexFormatElement.UV2);
    private static final int COLOUR = DefaultVertexFormat.BLOCK.getOffset(VertexFormatElement.COLOR);
    private static final int TEXTURE = DefaultVertexFormat.BLOCK.getOffset(VertexFormatElement.UV0);
    private static final int NORMAL = DefaultVertexFormat.BLOCK.getOffset(VertexFormatElement.NORMAL);
    /* A vertex's normal is packed a byte a part, its length 127; one far shorter faces nowhere. */
    private static final float NORMAL_LENGTH = 127F;
    private static final float NO_FACING = 0.25F;
    /* Where each side's shade is in {@link #sides()}. */
    private static final int EAST = 0;
    private static final int NORTH = 1;
    private static final int UP = 2;
    private static final int DOWN = 3;
    private static final int SIDES = 4;
    /* How far the eye moves through a section before its see-through faces are sorted again, in blocks. */
    private static final double RESORT = 1;
    /* A section's width, in blocks. */
    private static final int SECTION = 16;
    private static @Nullable SectionCompiler compiler;
    private static @Nullable SectionBufferBuilderPack pack;
    private static @Nullable ByteBufferBuilder scratch;

    StructureMeshes(final ClientStructure structure) {
        this.structure = structure;
    }

    /** A section of the yard changed: the structure holding it builds it again. */
    public static void dirty(final int x, final int y, final int z) {
        final ClientStructure holder = ClientMovingStructures.byPlot(YardLayout.plotOfChunk(x, z));
        if (holder != null) {
            holder.meshes().mark(SectionPos.asLong(x, y, z));
        }
    }

    /**
     * Whether the section the game builds now, on this thread, is a structure's built with its faces unshaded: the
     * game's shade of a face is by the side it faces in the yard, and the structure's faces face elsewhere as it turns.
     */
    public static boolean shadingLater() {
        return SHADING_LATER.get();
    }

    /** The block entities of the structure's blocks with something drawn of their own. */
    public List<BlockEntity> blockEntities() {
        if (blockEntitiesStale) {
            blockEntities.clear();
            for (final Section section : sections.values()) {
                blockEntities.addAll(section.blockEntities);
            }
            blockEntitiesStale = false;
        }
        return blockEntities;
    }

    /** Whether any section waits to be built. */
    public boolean waiting() {
        return !dirty.isEmpty();
    }

    /** How many sections are built. */
    public int built() {
        return sections.size();
    }

    /**
     * Marks the section {@code key} of the yard to be built again: one of the structure's own, within the world's
     * height; the game marks the sections round a changed one too, past the world's floor and past the structure.
     */
    void mark(final long key) {
        final Box box = structure.bounds();
        final int x = SectionPos.x(key);
        final int y = SectionPos.y(key);
        final int z = SectionPos.z(key);
        final Level level = structure.level();
        if (frozen || !structure.known() || y < level.getMinSection() || y >= level.getMaxSection()
                || x < box.minX() >> 4 || x > box.maxX() >> 4 || y < box.minY() >> 4 || y > box.maxY() >> 4
                || z < box.minZ() >> 4 || z > box.maxZ() >> 4) {
            return;
        }
        dirty.add(key);
    }

    /** Marks the sections of the structure's box not yet built, after it grew. */
    void boundsChanged() {
        if (frozen || !structure.known()) {
            return;
        }
        final Box box = structure.bounds();
        for (int x = box.minX() >> 4; x <= box.maxX() >> 4; x++) {
            for (int z = box.minZ() >> 4; z <= box.maxZ() >> 4; z++) {
                for (int y = box.minY() >> 4; y <= box.maxY() >> 4; y++) {
                    final long key = SectionPos.asLong(x, y, z);
                    if (!sections.containsKey(key)) {
                        mark(key);
                    }
                }
            }
        }
    }

    /**
     * Each block of the structure that gives light, where it is in the world as the structure stands at {@code frame},
     * for the light it lays on the world round it as it goes.
     */
    public void lamps(final StructureFrame frame, final List<IDynamicLightSource.Light> into) {
        for (final Section section : sections.values()) {
            for (final Lamp lamp : section.lamps) {
                final double x = lamp.x() + 0.5;
                final double y = lamp.y() + 0.5;
                final double z = lamp.z() + 0.5;
                into.add(new IDynamicLightSource.Light(new Vec3(frame.worldX(x, y, z), frame.worldY(x, y, z),
                        frame.worldZ(x, y, z)), lamp.light(), lamp.colour()));
            }
        }
    }

    /** From now on its blocks are drawn as they are: the structure is being set down, its plot about to be cleared. */
    void freeze() {
        frozen = true;
        dirty.clear();
    }

    /** Builds the sections waiting, until {@code deadline} in the game's nanoseconds; on the render thread. */
    void build(final long deadline, final Vec3 eye) {
        if (dirty.isEmpty() || !structure.ready()) {
            return;
        }
        followTurn();
        final Level level = structure.level();
        final RenderRegionCache cache = new RenderRegionCache();
        final LongIterator each = dirty.iterator();
        while (each.hasNext() && System.nanoTime() < deadline) {
            final long key = each.nextLong();
            each.remove();
            compile(level, cache, key, eye);
        }
    }

    /**
     * Each tick: shades the faces again when the structure has turned, lights each section again when the world's
     * light where it is changed, and sorts again the see-through faces of a section the eye has moved through.
     */
    void tick(final Vec3 eye) {
        followTurn();
        for (final Section section : sections.values()) {
            if (takeWorldLight(section)) {
                for (final Layer layer : section.layers.values()) {
                    layer.lit = false;
                }
            }
            final Layer seeThrough = section.layers.get(RenderType.translucent());
            if (seeThrough != null && seeThrough.sort != null
                    && eye.distanceToSqr(seeThrough.sortedFromX, seeThrough.sortedFromY, seeThrough.sortedFromZ)
                    > RESORT * RESORT) {
                seeThrough.resortWanted = true;
            }
        }
    }

    /**
     * Draws the sections' meshes of {@code type} from the graphics card, turned and placed as the structure is drawn:
     * the way with no shader pack.
     */
    void draw(final RenderType type, final Matrix4f view, final Matrix4f projection, final Vec3 camera,
              final float partialTick, final Vec3 eye) {
        if (sections.isEmpty()) {
            return;
        }
        final StructureFrame frame = new StructureFrame(structure.drawnAt(partialTick), structure.pivotX(),
                structure.pivotY(), structure.pivotZ());
        type.setupRenderState();
        final ShaderInstance shader = RenderSystem.getShader();
        if (shader == null) {
            type.clearRenderState();
            return;
        }
        if (shader.CHUNK_OFFSET != null) {
            shader.CHUNK_OFFSET.set(0F, 0F, 0F);
        }
        final Quaternionf turn = new Quaternionf((float) frame.pose().qx(), (float) frame.pose().qy(),
                (float) frame.pose().qz(), (float) frame.pose().qw());
        for (final Section section : sections.values()) {
            final Layer layer = section.layers.get(type);
            if (layer == null) {
                continue;
            }
            if (!layer.lit || layer.resortWanted) {
                relight(section, layer, eye);
            }
            if (layer.vertices == null) {
                continue;
            }
            final Matrix4f model = new Matrix4f(view)
                    .translate((float) (frame.pose().x() - camera.x), (float) (frame.pose().y() - camera.y),
                            (float) (frame.pose().z() - camera.z))
                    .rotate(turn)
                    .translate((float) (section.x - structure.pivotX()), (float) (section.y - structure.pivotY()),
                            (float) (section.z - structure.pivotZ()));
            layer.vertices.bind();
            layer.vertices.drawWithShader(model, projection, shader);
        }
        VertexBuffer.unbind();
        type.clearRenderState();
    }

    /**
     * Hands the sections' meshes to {@code buffers}, turned and placed by {@code pose}, which already stands at the
     * structure's pivot turned as it is drawn: the way under a shader pack, which draws them as its own.
     */
    void emit(final PoseStack pose, final MultiBufferSource buffers) {
        for (final Section section : sections.values()) {
            pose.pushPose();
            pose.translate((float) (section.x - structure.pivotX()), (float) (section.y - structure.pivotY()),
                    (float) (section.z - structure.pivotZ()));
            final PoseStack.Pose at = pose.last();
            final float[] sides = sides();
            for (final Map.Entry<RenderType, Layer> each : section.layers.entrySet()) {
                final Layer layer = each.getValue();
                if (layer.copy == null) {
                    continue;
                }
                final VertexConsumer out = buffers.getBuffer(each.getKey());
                final ByteBuffer from = layer.copy;
                for (int vertex = 0; vertex < layer.count; vertex++) {
                    final int base = vertex * VERTEX;
                    final float x = from.getFloat(base);
                    final float y = from.getFloat(base + 4);
                    final float z = from.getFloat(base + 8);
                    final int colour = colourOf(layer, vertex, from.getInt(base + COLOUR), from.get(base + NORMAL),
                            from.get(base + NORMAL + 1), from.get(base + NORMAL + 2), sides);
                    final float u = from.getFloat(base + TEXTURE);
                    final float v = from.getFloat(base + TEXTURE + 4);
                    final int light = lit(from.getShort(base + LIGHT), from.getShort(base + LIGHT + 2), section);
                    final float nx = from.get(base + NORMAL) / 127F;
                    final float ny = from.get(base + NORMAL + 1) / 127F;
                    final float nz = from.get(base + NORMAL + 2) / 127F;
                    out.addVertex(at, x, y, z).setColor(colour & 0xFF, (colour >> 8) & 0xFF, (colour >> 16) & 0xFF,
                            (colour >>> 24) & 0xFF).setUv(u, v).setLight(light).setNormal(at, nx, ny, nz);
                }
            }
            pose.popPose();
        }
    }

    /** Lets go of every mesh, on the graphics card and in memory, as the structure leaves the player's world. */
    void close() {
        for (final Section section : sections.values()) {
            section.close();
        }
        sections.clear();
        dirty.clear();
        blockEntities.clear();
    }

    private void compile(final Level level, final RenderRegionCache cache, final long key, final Vec3 eye) {
        final SectionPos at = SectionPos.of(key);
        final Section old = sections.remove(key);
        if (old != null) {
            old.close();
            blockEntitiesStale = true;
        }
        final RenderChunkRegion region = cache.createRegion(level, at, true);
        if (region == null) {
            return;
        }
        final BlockPos origin = at.origin();
        final VertexSorting sorting = VertexSorting.byDistance((float) (eye.x - origin.getX()),
                (float) (eye.y - origin.getY()), (float) (eye.z - origin.getZ()));
        final SectionCompiler.Results results = build(at, region, sorting, false);
        final Section section = new Section(origin.getX(), origin.getY(), origin.getZ());
        // A section built again keeps the world's light it had; a new one takes it now, not a tick later.
        if (old != null) {
            section.worldSky = old.worldSky;
            section.worldBlock = old.worldBlock;
        } else if (!takeWorldLight(section)) {
            section.worldSky = lastWorldSky;
            section.worldBlock = lastWorldBlock;
        }
        for (final Map.Entry<RenderType, MeshData> each : results.renderedLayers.entrySet()) {
            final MeshData mesh = each.getValue();
            final Layer layer = new Layer(mesh.drawState());
            final ByteBuffer vertices = mesh.vertexBuffer();
            layer.count = mesh.drawState().vertexCount();
            layer.copy = MemoryUtil.memAlloc(vertices.remaining());
            MemoryUtil.memCopy(vertices, layer.copy);
            if (each.getKey() == RenderType.translucent()) {
                layer.sort = results.transparencyState;
                layer.sortedFromX = eye.x;
                layer.sortedFromY = eye.y;
                layer.sortedFromZ = eye.z;
            }
            mesh.close();
            section.layers.put(each.getKey(), layer);
        }
        section.blockEntities.addAll(results.blockEntities);
        section.blockEntities.addAll(results.globalBlockEntities);
        findLamps(region, section);
        // The same blocks built again with their faces unshaded tell which faces the game shaded: those are shaded
        // here as they face once turned, and a face the game leaves unshaded, a plant's, stays as it is.
        final SectionCompiler.Results unshaded = build(at, region, sorting, true);
        for (final Map.Entry<RenderType, MeshData> each : unshaded.renderedLayers.entrySet()) {
            final Layer layer = section.layers.get(each.getKey());
            final MeshData mesh = each.getValue();
            if (layer != null && layer.copy != null && mesh.drawState().vertexCount() == layer.count) {
                unshade(layer, mesh.vertexBuffer());
            }
            mesh.close();
        }
        if (!section.layers.isEmpty() || !section.blockEntities.isEmpty() || !section.lamps.isEmpty()) {
            sections.put(key, section);
            blockEntitiesStale = true;
        }
    }

    /* Notes the section's blocks that give light. */
    private void findLamps(final RenderChunkRegion region, final Section section) {
        final Box bounds = structure.bounds();
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int x = Math.max(section.x, bounds.minX()); x <= Math.min(section.x + SECTION - 1, bounds.maxX()); x++) {
            for (int y = Math.max(section.y, bounds.minY()); y <= Math.min(section.y + SECTION - 1, bounds.maxY());
                 y++) {
                for (int z = Math.max(section.z, bounds.minZ()); z <= Math.min(section.z + SECTION - 1,
                        bounds.maxZ()); z++) {
                    final BlockState state = region.getBlockState(at.set(x, y, z));
                    final int light = state.getLightEmission(region, at);
                    if (light > 0) {
                        section.lamps.add(new Lamp(x, y, z, light, LightColours.of(state)));
                    }
                }
            }
        }
    }

    /* Builds the section at {@code at} into the shared buffers, its faces shaded by the game or left to be. */
    private static SectionCompiler.Results build(final SectionPos at, final RenderChunkRegion region,
                                                 final VertexSorting sorting, final boolean shadeLater) {
        final SectionBufferBuilderPack buffers = pack();
        buffers.clearAll();
        SHADING_LATER.set(shadeLater);
        try {
            return compiler().compile(at, region, sorting, buffers);
        } finally {
            SHADING_LATER.set(false);
        }
    }

    /*
     * Marks which of the layer's faces the game shaded, from its vertices built unshaded beside it, and keeps those
     * faces' colours unshaded, to be shaded as they face.
     */
    private static void unshade(final Layer layer, final ByteBuffer unshaded) {
        final ByteBuffer shaded = layer.copy;
        final int from = unshaded.position();
        layer.shaded = new boolean[layer.count];
        for (int vertex = 0; vertex < layer.count; vertex++) {
            final int base = vertex * VERTEX;
            final int colour = unshaded.getInt(from + base + COLOUR);
            if (FaceShade.shaded(FaceShade.brightest(shaded.getInt(base + COLOUR)), FaceShade.brightest(colour),
                    shaded.get(base + NORMAL + 1) / NORMAL_LENGTH)) {
                layer.shaded[vertex] = true;
                shaded.putInt(base + COLOUR, colour);
            }
        }
    }

    /*
     * Shades the faces again as the structure has turned since they were last shaded: by a degree and a half, so a
     * structure turning as it goes is shaded every tick or two, and one at rest never again.
     */
    private void followTurn() {
        final StructurePose now = structure.frame().pose();
        final Quaternionf turn = new Quaternionf((float) now.qx(), (float) now.qy(), (float) now.qz(),
                (float) now.qw());
        if (Math.abs(turn.dot(shadedFor)) >= SHADE_AGAIN) {
            return;
        }
        shadedFor.set(turn);
        for (final Section section : sections.values()) {
            for (final Layer layer : section.layers.values()) {
                if (layer.shaded != null) {
                    layer.lit = false;
                }
            }
        }
    }

    /*
     * Takes the world's light where the section's blocks are: the middle of the part of the section they fill, and
     * just over its top, the brighter of the two, so a place that falls in a wall of the world darkens nothing; the
     * places held within the world's height. Where the player's game has not the world there, the light stays as it
     * was. Whether the light changed.
     */
    private boolean takeWorldLight(final Section section) {
        final Level level = structure.level();
        final StructureFrame frame = structure.frame();
        final Box bounds = structure.bounds();
        final double minX = Math.max(section.x, bounds.minX());
        final double minY = Math.max(section.y, bounds.minY());
        final double minZ = Math.max(section.z, bounds.minZ());
        final double maxX = Math.min(section.x + SECTION, bounds.maxX() + 1);
        final double maxY = Math.min(section.y + SECTION, bounds.maxY() + 1);
        final double maxZ = Math.min(section.z + SECTION, bounds.maxZ() + 1);
        final double cx = (minX + maxX) / 2;
        final double cy = (minY + maxY) / 2;
        final double cz = (minZ + maxZ) / 2;
        final BlockPos middle = within(level, frame.worldX(cx, cy, cz), frame.worldY(cx, cy, cz),
                frame.worldZ(cx, cy, cz));
        final BlockPos over = within(level, frame.worldX(cx, maxY + 0.5, cz), frame.worldY(cx, maxY + 0.5, cz),
                frame.worldZ(cx, maxY + 0.5, cz));
        if (!level.hasChunk(middle.getX() >> 4, middle.getZ() >> 4)
                || !level.hasChunk(over.getX() >> 4, over.getZ() >> 4)) {
            return false;
        }
        final int sky = Math.max(level.getBrightness(LightLayer.SKY, middle),
                level.getBrightness(LightLayer.SKY, over));
        final int block = Math.max(othersLight(level, middle, frame), othersLight(level, over, frame));
        lastWorldSky = sky;
        lastWorldBlock = block;
        if (sky == section.worldSky && block == section.worldBlock) {
            return false;
        }
        section.worldSky = sky;
        section.worldBlock = block;
        return true;
    }

    /*
     * The world's light of blocks at {@code at}, but none where it could be only the structure's own: its lamps lay
     * their light on the world round it as it goes, and it keeps its own light inside already.
     */
    private int othersLight(final Level level, final BlockPos at, final StructureFrame frame) {
        final int light = level.getBrightness(LightLayer.BLOCK, at);
        if (light == 0 || !DynamicLightsClient.on()) {
            return light;
        }
        for (final Section section : sections.values()) {
            for (final Lamp lamp : section.lamps) {
                final BlockPos there = BlockPos.containing(frame.worldX(lamp.x() + 0.5, lamp.y() + 0.5, lamp.z() + 0.5),
                        frame.worldY(lamp.x() + 0.5, lamp.y() + 0.5, lamp.z() + 0.5),
                        frame.worldZ(lamp.x() + 0.5, lamp.y() + 0.5, lamp.z() + 0.5));
                if (lamp.light() - there.distManhattan(at) >= light) {
                    return 0;
                }
            }
        }
        return light;
    }

    /* The block holding {@code (x, y, z)}, held within the world's height. */
    private static BlockPos within(final Level level, final double x, final double y, final double z) {
        return BlockPos.containing(x, Mth.clamp(y, level.getMinBuildHeight(), level.getMaxBuildHeight() - 1), z);
    }

    /*
     * Puts the section's mesh on the graphics card again, lit by the world's light where it is now, its faces shaded
     * as they face now, sorted again.
     */
    private void relight(final Section section, final Layer layer, final Vec3 eye) {
        if (layer.copy == null) {
            return;
        }
        final int bytes = layer.count * VERTEX;
        final ByteBufferBuilder builder = scratch();
        final long to = builder.reserve(bytes);
        MemoryUtil.memCopy(MemoryUtil.memAddress(layer.copy), to, bytes);
        final float[] sides = sides();
        for (int vertex = 0; vertex < layer.count; vertex++) {
            final long at = to + (long) vertex * VERTEX;
            final int packed = lit(MemoryUtil.memGetShort(at + LIGHT), MemoryUtil.memGetShort(at + LIGHT + 2),
                    section);
            MemoryUtil.memPutShort(at + LIGHT, (short) (packed & 0xFFFF));
            MemoryUtil.memPutShort(at + LIGHT + 2, (short) (packed >>> 16));
            if (layer.shaded != null && layer.shaded[vertex]) {
                MemoryUtil.memPutInt(at + COLOUR, colourOf(layer, vertex, MemoryUtil.memGetInt(at + COLOUR),
                        MemoryUtil.memGetByte(at + NORMAL), MemoryUtil.memGetByte(at + NORMAL + 1),
                        MemoryUtil.memGetByte(at + NORMAL + 2), sides));
            }
        }
        final ByteBufferBuilder.Result made = builder.build();
        if (made == null) {
            return;
        }
        if (layer.vertices == null) {
            layer.vertices = new VertexBuffer(VertexBuffer.Usage.STATIC);
        }
        layer.vertices.bind();
        layer.vertices.upload(new MeshData(made, layer.state));
        if (layer.sort != null) {
            final BlockPos origin = new BlockPos(section.x, section.y, section.z);
            final ByteBufferBuilder.Result order = layer.sort.buildSortedIndexBuffer(scratch(),
                    VertexSorting.byDistance((float) (eye.x - origin.getX()), (float) (eye.y - origin.getY()),
                            (float) (eye.z - origin.getZ())));
            if (order != null) {
                layer.vertices.uploadIndexBuffer(order);
            }
            layer.sortedFromX = eye.x;
            layer.sortedFromY = eye.y;
            layer.sortedFromZ = eye.z;
        }
        layer.resortWanted = false;
        layer.lit = true;
    }

    /* A vertex's colour: shaded as its face faces with the structure turned as it is, if the game shaded the face. */
    private int colourOf(final Layer layer, final int vertex, final int colour, final byte x, final byte y,
                         final byte z, final float[] sides) {
        if (layer.shaded == null || !layer.shaded[vertex]) {
            return colour;
        }
        facing.set(x / NORMAL_LENGTH, y / NORMAL_LENGTH, z / NORMAL_LENGTH);
        if (facing.lengthSquared() < NO_FACING) {
            return colour;
        }
        shadedFor.transform(facing.normalize());
        return FaceShade.shade(colour, FaceShade.of(facing.x, facing.y, facing.z, sides[EAST], sides[NORTH],
                sides[UP], sides[DOWN]));
    }

    /* The game's shade of a face facing each side, in the structure's world: east, north, up and down. */
    private float[] sides() {
        final Level level = structure.level();
        final float[] sides = new float[SIDES];
        sides[EAST] = level.getShade(Direction.EAST, true);
        sides[NORTH] = level.getShade(Direction.NORTH, true);
        sides[UP] = level.getShade(Direction.UP, true);
        sides[DOWN] = level.getShade(Direction.DOWN, true);
        return sides;
    }

    /* A vertex's own light, its sky no brighter than the world's sky where it is, its blocks' at least the world's. */
    private static int lit(final short block, final short sky, final Section section) {
        final int ownBlock = block & 0xFFFF;
        final int ownSky = sky & 0xFFFF;
        final int blockLight = Math.max(ownBlock, section.worldBlock << 4);
        final int skyLight = Math.min(ownSky, section.worldSky << 4);
        return blockLight | skyLight << 16;
    }

    private static SectionCompiler compiler() {
        if (compiler == null) {
            final Minecraft game = Minecraft.getInstance();
            compiler = new SectionCompiler(game.getBlockRenderer(), game.getBlockEntityRenderDispatcher());
        }
        return compiler;
    }

    private static SectionBufferBuilderPack pack() {
        if (pack == null) {
            pack = new SectionBufferBuilderPack();
        }
        return pack;
    }

    private static ByteBufferBuilder scratch() {
        if (scratch == null) {
            scratch = new ByteBufferBuilder(1 << 16);
        }
        return scratch;
    }

    /* One section of sixteen blocks a side: its first block in the yard, its meshes by kind, its block entities. */
    private static final class Section {

        private final int x;
        private final int y;
        private final int z;
        private final Map<RenderType, Layer> layers = new IdentityHashMap<>();
        private final List<BlockEntity> blockEntities = new ArrayList<>();
        private final List<Lamp> lamps = new ArrayList<>();
        private int worldSky = 15;
        private int worldBlock;

        private Section(final int x, final int y, final int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        void close() {
            for (final Layer layer : layers.values()) {
                layer.close();
            }
            layers.clear();
        }
    }

    /* A block of the structure that gives light: where it is in the yard, how much light, and of what colour. */
    private record Lamp(int x, int y, int z, int light, int colour) {
    }

    /* One kind of a section's mesh: on the graphics card, in memory, and the order its see-through faces go in. */
    private static final class Layer {

        private final MeshData.DrawState state;
        private @Nullable VertexBuffer vertices;
        private @Nullable ByteBuffer copy;
        /* For each vertex, whether the game shaded its face, kept unshaded in the copy; null when not known. */
        private boolean @Nullable [] shaded;
        private MeshData.@Nullable SortState sort;
        private int count;
        private double sortedFromX;
        private double sortedFromY;
        private double sortedFromZ;
        private boolean resortWanted;
        private boolean lit;

        private Layer(final MeshData.DrawState state) {
            this.state = state;
        }

        void close() {
            if (vertices != null) {
                vertices.close();
                vertices = null;
            }
            if (copy != null) {
                MemoryUtil.memFree(copy);
                copy = null;
            }
        }
    }
}
