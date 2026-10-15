/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.cable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.jstech.core.JsCore;
import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.cable.CableItem;
import dev.jstech.core.cable.CableRunPayload;
import dev.jstech.core.cable.CableRuns;
import dev.jstech.core.cable.CableType;
import dev.jstech.core.cable.RunPath;
import dev.jstech.core.cable.Wire;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * Dragging a run of cable on the player's game: a click with a cable lays it as ever, and holding the click while
 * looking further along outlines the run from that block to the one looked at, white where it can be laid and red
 * where something is in the way; letting go has the server lay it all ({@link CableRuns}). While the click is held the
 * game's own repeated clicks are held back, so the cable is not laid twice.
 */
@PaletteHolder
public final class CableDragClient {

    /** The colours of the run's outline: a block it can be laid in, and one in the way. */
    public static final Palette<Colours> PALETTE = Palettes.declare(JsCore.MODID, "cable_drag", new Colours(
            0xFF << 24 | 0xFFFFFF, 0xFF << 24 | 0xFF5A50));
    private static final double PIXEL = 1.0 / 16.0;
    /* How far a run is dragged to, in blocks: half the longest run. */
    private static final double DRAG_REACH = RunPath.MOST / 2.0;

    private static boolean dragging;
    private static @Nullable BlockPos anchor;
    private static InteractionHand hand = InteractionHand.MAIN_HAND;

    private CableDragClient() {
    }

    /** Hands the game's bus what this answers, once, from the Core's client. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(InputEvent.InteractionKeyMappingTriggered.class, CableDragClient::onUse);
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> tick());
        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.class, CableDragClient::onRender);
    }

    /** A click with a cable starts a drag where it lays it; the game's repeated clicks while dragging are held back. */
    public static void onUse(final InputEvent.InteractionKeyMappingTriggered event) {
        final Player player = Minecraft.getInstance().player;
        if (!event.isUseItem() || player == null
                || !(player.getItemInHand(event.getHand()).getItem() instanceof CableItem item)) {
            return;
        }
        if (dragging) {
            event.setCanceled(true);
            event.setSwingHand(false);
            return;
        }
        if (Minecraft.getInstance().hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
            begin(event.getHand(), target(item, hit));
        }
    }

    /** Starts dragging a run from {@code at} with the cable in {@code inHand}, as a click with it does. */
    public static void begin(final InteractionHand inHand, final BlockPos at) {
        dragging = true;
        anchor = at.immutable();
        hand = inHand;
    }

    /** The blocks the run being dragged would go through now; none while nothing is dragged. */
    public static List<BlockPos> preview() {
        final Optional<CableItem> item = held();
        if (!dragging || anchor == null || item.isEmpty()) {
            return List.of();
        }
        final BlockPos from = anchor;
        return target(item.get()).map(to -> CableRuns.path(from, to, item.get().type().runsStraight()))
                .orElse(List.of(from));
    }

    /* Each tick of a drag: let go, the run is sent to be laid; held, the run's length shows above the hotbar. */
    private static void tick() {
        if (!dragging) {
            return;
        }
        final Minecraft minecraft = Minecraft.getInstance();
        final Optional<CableItem> item = held();
        if (minecraft.player == null || item.isEmpty() || anchor == null) {
            dragging = false;
            return;
        }
        if (minecraft.options.keyUse.isDown()) {
            minecraft.gui.setOverlayMessage(GameText.component(CableRuns.PREVIEW.with(preview().size())), false);
            return;
        }
        dragging = false;
        final BlockPos from = anchor;
        target(item.get()).filter(to -> !to.equals(from)).ifPresent(to -> PacketDistributor.sendToServer(
                new CableRunPayload(from, to, hand == InteractionHand.MAIN_HAND)));
    }

    /* The run being dragged, outlined block by block at the cable's thickness, through nothing. */
    private static void onRender(final RenderLevelStageEvent event) {
        if (!dragging || event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        final Minecraft minecraft = Minecraft.getInstance();
        final ClientLevel level = minecraft.level;
        final Optional<CableItem> item = held();
        if (level == null || item.isEmpty()) {
            return;
        }
        final CableType type = item.get().type();
        final double inset = (16 - type.thickness()) / 2.0 * PIXEL;
        final Vec3 camera = event.getCamera().getPosition();
        final PoseStack pose = event.getPoseStack();
        final MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        final VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        final Colours c = PALETTE.get();
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        for (final BlockPos pos : preview()) {
            final int colour = free(level, pos, type) ? c.free() : c.blocked();
            LevelRenderer.renderLineBox(pose, lines, new AABB(pos).deflate(inset), (colour >> 16 & 0xFF) / 255F,
                    (colour >> 8 & 0xFF) / 255F, (colour & 0xFF) / 255F, 1F);
        }
        pose.popPose();
        buffers.endBatch(RenderType.lines());
    }

    /* Whether a wire of {@code type} can be laid at {@code pos}: an empty place, or a cable block that takes it. */
    private static boolean free(final ClientLevel level, final BlockPos pos, final CableType type) {
        if (level.getBlockEntity(pos) instanceof CableBlockEntity cable) {
            return cable.holds(type) || cable.takes(Wire.of(type));
        }
        return level.getBlockState(pos).canBeReplaced();
    }

    /* The cable in the hand the drag is made with. */
    private static Optional<CableItem> held() {
        final Player player = Minecraft.getInstance().player;
        if (player == null) {
            return Optional.empty();
        }
        final ItemStack stack = player.getItemInHand(hand);
        return stack.getItem() instanceof CableItem item ? Optional.of(item) : Optional.empty();
    }

    /*
     * The block the run is dragged to: where a click with {@code item} would lay its wire on the block looked at, as far
     * as a run reaches rather than the player's own reach. None when nothing is looked at that near.
     */
    private static Optional<BlockPos> target(final CableItem item) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return Optional.empty();
        }
        final HitResult looked = minecraft.player.pick(DRAG_REACH, minecraft.getTimer().getGameTimeDeltaPartialTick(
                true), false);
        return looked instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK
                ? Optional.of(target(item, hit)) : Optional.empty();
    }

    /*
     * Where a click with {@code item} on {@code hit} lays its wire: in the cable block looked at, when it takes it and
     * the player is not sneaking; against the face looked at otherwise.
     */
    private static BlockPos target(final CableItem item, final BlockHitResult hit) {
        final Minecraft minecraft = Minecraft.getInstance();
        final BlockPos clicked = hit.getBlockPos();
        if (minecraft.player != null && minecraft.level != null && !minecraft.player.isSecondaryUseActive()
                && minecraft.level.getBlockEntity(clicked) instanceof CableBlockEntity cable
                && (cable.holds(item.type()) || cable.takes(Wire.of(item.type())))) {
            return clicked;
        }
        return clicked.relative(hit.getDirection());
    }

    /**
     * The colours of a run's outline.
     *
     * @param free    a block it can be laid in
     * @param blocked a block something is in the way of
     */
    public record Colours(int free, int blocked) {
    }
}
