/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.machine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.jstech.core.JsCore;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.tooltip.ItemTooltip;
import dev.jstech.core.client.gui.tooltip.ItemTooltips;
import dev.jstech.core.connect.RelativeFace;
import dev.jstech.core.machine.MachineBlockEntity;
import dev.jstech.core.machine.MachineResource;
import dev.jstech.core.machine.MachineWrench;
import dev.jstech.core.machine.WrenchResourcePayload;
import dev.jstech.core.text.GameText;
import java.util.Optional;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;

/**
 * A wrench on the player's side: holding one, the machine aimed at shows each of its faces in the colour of what it
 * does with the resource the wrench shows, that resource named under the crosshair; sneaking, the mouse wheel shows
 * the next resource; and a machine's item taken away whole says so in its tooltip.
 */
public final class MachineWrenchClient {

    private static final ResourceLocation LAYER = ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
            "machine_wrench");
    private static final double OUT = 0.003;
    private static final float SHADE = 0.35F;
    private static final int BELOW_CROSSHAIR = 10;
    private static final int LABEL_ALPHA = 0xE0;

    private MachineWrenchClient() {
    }

    /** Hands the mod's bus and the game's what this answers, once, from the Core's client. */
    public static void listen(final IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.class, MachineWrenchClient::onRender);
        NeoForge.EVENT_BUS.addListener(InputEvent.MouseScrollingEvent.class, MachineWrenchClient::onScroll);
        ItemTooltips.add(JsCore.MODID, MachineWrenchClient::describe);
        modBus.addListener(RegisterGuiLayersEvent.class, event -> event.registerAbove(VanillaGuiLayers.CROSSHAIR,
                LAYER, MachineWrenchClient::drawLabel));
    }

    /* Each face of the machine aimed at, in the colour of what it does with the resource shown. */
    private static void onRender(final RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        final Optional<MachineBlockEntity> aimed = aimed();
        if (aimed.isEmpty()) {
            return;
        }
        final MachineBlockEntity machine = aimed.get();
        final MachineResource resource = MachineWrench.shown(held(), machine);
        final Direction facing = RelativeFace.facingOf(machine.getBlockState()).orElse(Direction.NORTH);
        final Minecraft minecraft = Minecraft.getInstance();
        final Vec3 camera = event.getCamera().getPosition();
        final PoseStack pose = event.getPoseStack();
        final MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        final VertexConsumer quads = buffers.getBuffer(RenderType.debugQuads());
        final BlockPos pos = machine.getBlockPos();
        pose.pushPose();
        pose.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
        final Matrix4f matrix = pose.last().pose();
        for (final Direction side : Direction.values()) {
            final int colour = MachineWords.colour(machine.sides().mode(resource, RelativeFace.of(facing, side)));
            face(quads, matrix, side, (colour >> 16 & 0xFF) / 255F, (colour >> 8 & 0xFF) / 255F,
                    (colour & 0xFF) / 255F);
        }
        pose.popPose();
        buffers.endBatch(RenderType.debugQuads());
    }

    /* Sneaking with a wrench at a machine, the wheel shows its next resource and leaves the hotbar alone. */
    private static void onScroll(final InputEvent.MouseScrollingEvent event) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !minecraft.player.isShiftKeyDown() || minecraft.screen != null
                || event.getScrollDeltaY() == 0) {
            return;
        }
        aimed().ifPresent(machine -> {
            PacketDistributor.sendToServer(new WrenchResourcePayload(machine.getBlockPos(),
                    event.getScrollDeltaY() > 0));
            event.setCanceled(true);
        });
    }

    private static void describe(final ItemStack stack, final ItemTooltip tooltip) {
        if (stack.has(MachineWrench.TAKEN_WHOLE.get())) {
            tooltip.line(GameText.component(MachineWrench.TAKEN));
        }
    }

    /* The resource the wrench shows, named under the crosshair. */
    private static void drawLabel(final GuiGraphics g, final DeltaTracker delta) {
        final Optional<MachineBlockEntity> aimed = aimed();
        if (aimed.isEmpty()) {
            return;
        }
        final Minecraft minecraft = Minecraft.getInstance();
        final MachineResource resource = MachineWrench.shown(held(), aimed.get());
        Draw.textCentered(g, minecraft.font, GameText.component(resource.text()), g.guiWidth() / 2,
                g.guiHeight() / 2 + BELOW_CROSSHAIR, MachineArt.alpha(MachineArt.PALETTE.get().white(),
                        LABEL_ALPHA));
    }

    /* The machine aimed at while the player holds a wrench in the main hand. */
    private static Optional<MachineBlockEntity> aimed() {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || !MachineWrench.isWrench(held())
                || !(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return Optional.empty();
        }
        return minecraft.level.getBlockEntity(hit.getBlockPos()) instanceof MachineBlockEntity machine
                ? Optional.of(machine) : Optional.empty();
    }

    private static ItemStack held() {
        final Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player == null ? ItemStack.EMPTY : minecraft.player.getMainHandItem();
    }

    /* One face of the unit block, just outside it, in a translucent colour. */
    private static void face(final VertexConsumer quads, final Matrix4f matrix, final Direction side, final float r,
                             final float g, final float b) {
        final float lo = (float) -OUT;
        final float hi = (float) (1.0 + OUT);
        final float[][] corners = switch (side) {
            case DOWN -> new float[][] {{lo, lo, lo}, {hi, lo, lo}, {hi, lo, hi}, {lo, lo, hi}};
            case UP -> new float[][] {{lo, hi, lo}, {lo, hi, hi}, {hi, hi, hi}, {hi, hi, lo}};
            case NORTH -> new float[][] {{lo, lo, lo}, {lo, hi, lo}, {hi, hi, lo}, {hi, lo, lo}};
            case SOUTH -> new float[][] {{lo, lo, hi}, {hi, lo, hi}, {hi, hi, hi}, {lo, hi, hi}};
            case WEST -> new float[][] {{lo, lo, lo}, {lo, lo, hi}, {lo, hi, hi}, {lo, hi, lo}};
            case EAST -> new float[][] {{hi, lo, lo}, {hi, hi, lo}, {hi, hi, hi}, {hi, lo, hi}};
        };
        for (final float[] corner : corners) {
            quads.addVertex(matrix, corner[0], corner[1], corner[2]).setColor(r, g, b, SHADE);
        }
    }
}
