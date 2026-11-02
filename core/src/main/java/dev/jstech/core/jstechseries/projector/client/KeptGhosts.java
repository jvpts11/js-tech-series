/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.projector.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;
import dev.jstech.core.client.multiblock.FadedConsumer;
import dev.jstech.core.jstechseries.projector.ProjectorSettings;
import dev.jstech.core.jstechseries.projector.ProjectorView;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The ghosts and marks of one multiblock shown, kept on the graphics card: built once, off the game's thread, from what
 * is shown, and drawn every frame from there, built again only when what is shown changes; the meshes drawn till then
 * stay until the new ones are ready. The ghosts, see-through, are drawn back to front from where the eye stood when
 * they were last put in order, and put in order again, off the game's thread too, once the eye has moved a few blocks.
 * Drawn afresh every frame, the tens of thousands of ghosts of a building as big as the Rocket Hangar took the game
 * down to a few frames a second.
 */
final class KeptGhosts implements AutoCloseable {

    private final RenderType ghostType;
    private final RenderType markType;
    /* The view and settings what is shown was last read from. */
    @Nullable
    private ProjectorView readView;
    @Nullable
    private ProjectorSettings readSettings;
    /* What the meshes being built or drawn hold, told apart by its signature. */
    private long wanted = NONE;
    /* The meshes drawn, about the block they were built round. */
    private BlockPos origin = BlockPos.ZERO;
    @Nullable
    private VertexBuffer ghosts;
    @Nullable
    private VertexBuffer marks;
    /* The ghosts' quads by where each stands, to put them in order again, and the eye they were put in order for. */
    @Nullable
    private MeshData.SortState order;
    private Vec3 orderedFor = Vec3.ZERO;
    /* Which build what is drawn comes from: a build or an ordering finished for an older one is let go. */
    private int generation;
    @Nullable
    private CompletableFuture<Built> building;
    @Nullable
    private CompletableFuture<Ordered> ordering;

    private static final long NONE = Long.MIN_VALUE;
    /* How far the eye moves, in blocks, before the see-through ghosts are put in order again from where it stands. */
    private static final double REORDER = 4.0;
    private static final float GHOST_ALPHA = 0.45F;
    private static final float GHOST_SCALE = 0.98F;
    private static final double GROW = 0.004;
    private static final float CHANNEL = 255F;
    private static final int HASH = 31;
    /* How many indices a quad takes, drawn as two triangles. */
    private static final int INDICES_PER_QUAD = 6;

    KeptGhosts(final RenderType ghostType, final RenderType markType) {
        this.ghostType = ghostType;
        this.markType = markType;
    }

    /**
     * What {@code view}, shown with {@code settings}, draws: read by {@code read} when the view or the settings are
     * new, and built anew off the game's thread when that is not what is drawn or being built already.
     */
    void want(final ProjectorView view, final ProjectorSettings settings, final Supplier<Content> read,
              final Vec3 eye) {
        if (view == this.readView && settings.equals(this.readSettings)) {
            return;
        }
        this.readView = view;
        this.readSettings = settings;
        final Content content = read.get();
        if (content.signature() == this.wanted) {
            return;
        }
        this.wanted = content.signature();
        final int made = ++this.generation;
        final BlockPos round = view.terminal().immutable();
        final Vector3f from = relative(eye, round);
        letGo(this.building);
        this.building = CompletableFuture.supplyAsync(() -> Built.of(content, round, from, made, this.ghostType,
                this.markType), Util.backgroundExecutor());
    }

    /**
     * Draws the meshes kept, taking up first any built or put in order since the last frame: the marks first, seen
     * through everything, then the ghosts over them, so a mark behind a ghost shows softened through it.
     */
    void draw(final Vec3 eye) {
        this.takeBuilt(eye);
        this.takeOrdered();
        if (this.marks != null) {
            this.drawMesh(this.markType, this.marks, eye);
        }
        if (this.ghosts != null) {
            this.reorder(eye);
            this.drawMesh(this.ghostType, this.ghosts, eye);
        }
    }

    /** Lets every mesh go, and whatever is still being built or put in order once it is done. */
    @Override
    public void close() {
        letGo(this.building);
        letGo(this.ordering);
        this.building = null;
        this.ordering = null;
        this.drop();
    }

    /* A finished build of this generation replaces the meshes drawn. */
    private void takeBuilt(final Vec3 eye) {
        final CompletableFuture<Built> pending = this.building;
        if (pending == null || !pending.isDone()) {
            return;
        }
        this.building = null;
        final Built built = pending.getNow(null);
        if (built == null) {
            return;
        }
        if (built.generation() != this.generation) {
            built.close();
            return;
        }
        letGo(this.ordering);
        this.ordering = null;
        this.drop();
        this.origin = built.origin();
        this.ghosts = upload(built.ghosts());
        this.marks = upload(built.marks());
        this.order = built.order();
        this.orderedFor = new Vec3(built.from().x() + this.origin.getX(), built.from().y() + this.origin.getY(),
                built.from().z() + this.origin.getZ());
        built.close();
        if (eye.distanceToSqr(this.orderedFor) > REORDER * REORDER) {
            this.reorder(eye);
        }
    }

    /* A finished ordering of the meshes drawn sets the order their ghosts are drawn in. */
    private void takeOrdered() {
        final CompletableFuture<Ordered> pending = this.ordering;
        if (pending == null || !pending.isDone()) {
            return;
        }
        this.ordering = null;
        final Ordered ordered = pending.getNow(null);
        if (ordered == null) {
            return;
        }
        if (ordered.generation() != this.generation || this.ghosts == null || ordered.indices() == null) {
            ordered.close();
            return;
        }
        this.ghosts.bind();
        this.ghosts.uploadIndexBuffer(ordered.indices());
        VertexBuffer.unbind();
        ordered.memory().close();
    }

    /* Puts the ghosts in order again off the game's thread once the eye has moved far enough from where it was. */
    private void reorder(final Vec3 eye) {
        final MeshData.SortState sorted = this.order;
        if (sorted == null || this.ordering != null || eye.distanceToSqr(this.orderedFor) <= REORDER * REORDER) {
            return;
        }
        this.orderedFor = eye;
        final Vector3f from = relative(eye, this.origin);
        final int made = this.generation;
        this.ordering = CompletableFuture.supplyAsync(() -> {
            final ByteBufferBuilder memory = new ByteBufferBuilder(sorted.centroids().length * INDICES_PER_QUAD
                    * sorted.indexType().bytes);
            return new Ordered(memory, sorted.buildSortedIndexBuffer(memory, VertexSorting.byDistance(from)), made);
        }, Util.backgroundExecutor());
    }

    /* A mesh seen from the eye, about the block it was built round, in the render state of its kind. */
    private void drawMesh(final RenderType type, final VertexBuffer mesh, final Vec3 eye) {
        type.setupRenderState();
        final Matrix4f seen = new Matrix4f(RenderSystem.getModelViewMatrix()).translate(
                (float) (this.origin.getX() - eye.x), (float) (this.origin.getY() - eye.y),
                (float) (this.origin.getZ() - eye.z));
        mesh.bind();
        mesh.drawWithShader(seen, RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
        VertexBuffer.unbind();
        type.clearRenderState();
    }

    private void drop() {
        if (this.ghosts != null) {
            this.ghosts.close();
            this.ghosts = null;
        }
        if (this.marks != null) {
            this.marks.close();
            this.marks = null;
        }
        this.order = null;
    }

    @Nullable
    private static VertexBuffer upload(@Nullable final MeshData mesh) {
        if (mesh == null) {
            return null;
        }
        final VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        buffer.bind();
        buffer.upload(mesh);
        VertexBuffer.unbind();
        return buffer;
    }

    /* Whatever a future still to finish holds is let go once it does. */
    private static void letGo(@Nullable final CompletableFuture<? extends AutoCloseable> pending) {
        if (pending != null) {
            pending.thenAccept(done -> {
                try {
                    done.close();
                } catch (final Exception ignored) {
                    // Only memory is let go, which does not fail.
                }
            });
        }
    }

    private static Vector3f relative(final Vec3 eye, final BlockPos round) {
        return new Vector3f((float) (eye.x - round.getX()), (float) (eye.y - round.getY()),
                (float) (eye.z - round.getZ()));
    }

    /** A ghost to draw: where it stands, and the block it is drawn as. */
    record Ghost(BlockPos pos, BlockState state) {
    }

    /** An outline round a whole block: where, and its colour without its alpha. */
    record Mark(BlockPos pos, int colour) {
    }

    /**
     * What a multiblock shown draws, and a number that tells it from any other: the same ghosts and marks in the same
     * places give the same number.
     */
    record Content(List<Ghost> ghosts, List<Mark> marks, long signature) {

        static Content of(final List<Ghost> ghosts, final List<Mark> marks) {
            long signature = ghosts.size();
            for (final Ghost ghost : ghosts) {
                signature = signature * HASH + ghost.pos().asLong();
                signature = signature * HASH + Block.getId(ghost.state());
            }
            signature = signature * HASH + marks.size();
            for (final Mark mark : marks) {
                signature = signature * HASH + mark.pos().asLong();
                signature = signature * HASH + mark.colour();
            }
            return new Content(List.copyOf(ghosts), List.copyOf(marks), signature == NONE ? 0 : signature);
        }
    }

    /* The meshes built off the game's thread, the memory they stand in, and what they were built for. */
    private record Built(ByteBufferBuilder ghostMemory, @Nullable MeshData ghosts,
                         @Nullable MeshData.SortState order, ByteBufferBuilder markMemory, @Nullable MeshData marks,
                         BlockPos origin, Vector3f from, int generation) implements AutoCloseable {

        static Built of(final Content content, final BlockPos origin, final Vector3f from, final int generation,
                        final RenderType ghostType, final RenderType markType) {
            final BlockRenderDispatcher blocks = Minecraft.getInstance().getBlockRenderer();
            final ByteBufferBuilder ghostMemory = new ByteBufferBuilder(ghostType.bufferSize());
            final BufferBuilder ghostBuilder = new BufferBuilder(ghostMemory, ghostType.mode(), ghostType.format());
            final VertexConsumer faded = new FadedConsumer(ghostBuilder, GHOST_ALPHA);
            final PoseStack pose = new PoseStack();
            for (final Ghost ghost : content.ghosts()) {
                final BlockPos at = ghost.pos();
                pose.pushPose();
                pose.translate(at.getX() - origin.getX() + 0.5, at.getY() - origin.getY() + 0.5,
                        at.getZ() - origin.getZ() + 0.5);
                pose.scale(GHOST_SCALE, GHOST_SCALE, GHOST_SCALE);
                pose.translate(-0.5, -0.5, -0.5);
                blocks.getModelRenderer().renderModel(pose.last(), faded, ghost.state(),
                        blocks.getBlockModel(ghost.state()), 1F, 1F, 1F, LightTexture.FULL_BRIGHT,
                        OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
                pose.popPose();
            }
            final MeshData ghosts = ghostBuilder.build();
            final MeshData.SortState order = ghosts == null ? null
                    : ghosts.sortQuads(ghostMemory, VertexSorting.byDistance(from));
            final ByteBufferBuilder markMemory = new ByteBufferBuilder(markType.bufferSize());
            final BufferBuilder markBuilder = new BufferBuilder(markMemory, markType.mode(), markType.format());
            final PoseStack about = new PoseStack();
            about.translate(-origin.getX(), -origin.getY(), -origin.getZ());
            for (final Mark mark : content.marks()) {
                LevelRenderer.renderLineBox(about, markBuilder, new AABB(mark.pos()).inflate(GROW),
                        (mark.colour() >> 16 & 0xFF) / CHANNEL, (mark.colour() >> 8 & 0xFF) / CHANNEL,
                        (mark.colour() & 0xFF) / CHANNEL, 1F);
            }
            return new Built(ghostMemory, ghosts, order, markMemory, markBuilder.build(), origin, from, generation);
        }

        /* Lets the memory go: the meshes, uploaded or not, are done with. */
        @Override
        public void close() {
            if (this.ghosts != null) {
                this.ghosts.close();
            }
            if (this.marks != null) {
                this.marks.close();
            }
            this.ghostMemory.close();
            this.markMemory.close();
        }
    }

    /* The ghosts' indices put in order off the game's thread, the memory they stand in, and their build. */
    private record Ordered(ByteBufferBuilder memory, @Nullable ByteBufferBuilder.Result indices, int generation)
            implements AutoCloseable {

        @Override
        public void close() {
            if (this.indices != null) {
                this.indices.close();
            }
            this.memory.close();
        }
    }
}
