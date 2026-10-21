/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.tools.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.jstech.core.JsCore;
import dev.jstech.core.client.effect.ScreenEffects;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.model.kit.KitItemRenderer;
import dev.jstech.core.client.moving.ClientStructure;
import dev.jstech.core.client.moving.StructureForcesClient;
import dev.jstech.core.format.UnitFormatter;
import dev.jstech.core.item.ItemMode;
import dev.jstech.core.jstechseries.tools.PhysicsTool;
import dev.jstech.core.jstechseries.tools.PhysicsToolLayout;
import dev.jstech.core.jstechseries.tools.PhysicsToolScrollPayload;
import dev.jstech.core.moving.ForceDiagramTexts;
import dev.jstech.core.moving.StructureForcesPayload;
import dev.jstech.core.text.GameText;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * The physics tool on a player's game: its model in the hand, its panel at the screen's left while it is held, with
 * its modes, the chosen one framed, and its strength; the wheel, which changes its mode and with shift its strength;
 * and while it holds a structure, the beam bent from its muzzle to the point it holds, the ring there, the structure's
 * box lit, and beside it the structure's name, its mass and how far it is held.
 */
public final class PhysicsToolClient {

    private static final ResourceLocation LAYER = ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
            "physics_tool");
    private static final ResourceLocation BUTTON = ResourceLocation.withDefaultNamespace("widget/button");
    private static final ResourceLocation SLIDER = ResourceLocation.withDefaultNamespace("widget/slider");
    private static final ResourceLocation KNOB = ResourceLocation.withDefaultNamespace("widget/slider_handle");
    /* The beam: how many pieces it is drawn in, its core's and its glow's half widths, its dots, and the ring. */
    private static final int PIECES = 24;
    private static final double CORE = 0.025;
    private static final double GLOW = 0.08;
    private static final int DOTS = 6;
    private static final double DOT = 0.05;
    private static final double RING = 0.35;
    private static final int RING_PIECES = 24;
    /* How far from the eye the beam is drawn as wide as it is, in blocks; nearer, narrower. */
    private static final double TAPER = 4;
    /* Where the muzzle is from the eyes, held in first person: ahead, to the right, and down, in blocks. */
    private static final double AHEAD = 0.9;
    private static final double RIGHT = 0.42;
    private static final double DOWN = 0.32;
    /* From the eyes of a player seen from outside: their hand, lower and to the right. */
    private static final double HAND_DOWN = 0.55;
    private static final double HAND_RIGHT = 0.3;
    /* How far the beam leaves straight from the muzzle before it bends to the point, as a share of its length. */
    private static final double STRAIGHT = 0.5;
    private static final double TONNE = 1_000;
    private static final int LABEL_GAP = 12;

    private static int looked = -1;

    private PhysicsToolClient() {
    }

    /** Hands the mod's bus and the game's what this answers, once, from the Core's client. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterClientExtensionsEvent.class, event -> event.registerItem(
                KitItemRenderer.of(PhysicsTool.MODEL.id()).extensions(), PhysicsTool.ITEM.get()));
        modBus.addListener(RegisterGuiLayersEvent.class, event -> event.registerAbove(VanillaGuiLayers.HOTBAR,
                LAYER, PhysicsToolClient::render));
        NeoForge.EVENT_BUS.addListener(InputEvent.MouseScrollingEvent.class, PhysicsToolClient::onScroll);
        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.class, PhysicsToolClient::renderBeam);
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> tick());
    }

    /* The wheel, with the tool in the main hand and no screen open: its mode, or with shift its strength. */
    private static void onScroll(final InputEvent.MouseScrollingEvent event) {
        final Minecraft game = Minecraft.getInstance();
        if (game.screen != null || game.player == null || !game.player.getMainHandItem().is(PhysicsTool.ITEM.get())) {
            return;
        }
        event.setCanceled(true);
        final int steps = (int) Math.signum(event.getScrollDeltaY());
        if (steps != 0) {
            PacketDistributor.sendToServer(new PhysicsToolScrollPayload(steps, Screen.hasShiftDown()));
        }
    }

    /* Looks at the forces on what the tool holds, for its mass beside it, while it holds it. */
    private static void tick() {
        final LocalPlayer player = Minecraft.getInstance().player;
        final PhysicsTool.Grab grab = player == null ? null : held(player);
        final int now = grab != null && grab.body() instanceof ClientStructure structure
                ? structure.entity().getId() : -1;
        if (now != looked) {
            if (looked >= 0) {
                StructureForcesClient.stopLooking(looked);
            }
            if (now >= 0) {
                StructureForcesClient.look(now);
            }
            looked = now;
        }
    }

    /* What the player holds with the tool now: only while they hold its button. */
    private static @Nullable PhysicsTool.Grab held(final LocalPlayer player) {
        return player.isUsingItem() && player.getUseItem().is(PhysicsTool.ITEM.get()) ? PhysicsTool.grabbed(player)
                : null;
    }

    /* The panel, while the tool is in the main hand, and the label by what it holds. */
    private static void render(final GuiGraphics g, final DeltaTracker delta) {
        final Minecraft game = Minecraft.getInstance();
        if (game.player == null || game.options.hideGui) {
            return;
        }
        final ItemStack stack = game.player.getMainHandItem();
        if (!stack.is(PhysicsTool.ITEM.get())) {
            return;
        }
        final PhysicsToolPalette.Colours colours = PhysicsToolPalette.get();
        final Font font = game.font;
        final int x = PhysicsToolLayout.LEFT;
        final int y = PhysicsToolLayout.top(g.guiHeight());
        g.fill(x, y, x + PhysicsToolLayout.WIDTH, y + PhysicsToolLayout.HEIGHT, colours.panel());
        Draw.text(g, font, GameText.resolve(PhysicsTool.HEAD), x + PhysicsToolLayout.TITLE_X,
                y + PhysicsToolLayout.TITLE_Y, colours.title());
        final ItemMode chosen = PhysicsTool.mode(stack);
        final ItemMode[] modes = {PhysicsTool.GRAB_MODE, PhysicsTool.PUSH_MODE, PhysicsTool.FREEZE_MODE};
        for (int mode = 0; mode < modes.length; mode++) {
            final int mx = x + PhysicsToolLayout.modeX(mode);
            final int my = y + PhysicsToolLayout.MODE_Y;
            g.blitSprite(BUTTON, mx, my, PhysicsToolLayout.MODE_WIDTH, PhysicsToolLayout.MODE_HEIGHT);
            Draw.textCentered(g, font, GameText.resolve(modes[mode].name()), mx + PhysicsToolLayout.MODE_WIDTH / 2,
                    my + (PhysicsToolLayout.MODE_HEIGHT - font.lineHeight) / 2 + 1, colours.text());
            if (modes[mode].equals(chosen)) {
                Draw.outline(g, mx - 1, my - 1, PhysicsToolLayout.MODE_WIDTH + 2, PhysicsToolLayout.MODE_HEIGHT + 2,
                        colours.chosen());
            }
        }
        final int sx = x + PhysicsToolLayout.SLIDER_X;
        final int sy = y + PhysicsToolLayout.SLIDER_Y;
        final int strength = PhysicsTool.strength(stack);
        g.blitSprite(SLIDER, sx, sy, PhysicsToolLayout.SLIDER_WIDTH, PhysicsToolLayout.SLIDER_HEIGHT);
        g.blitSprite(KNOB, x + PhysicsToolLayout.knobX(strength), sy, PhysicsToolLayout.KNOB_WIDTH,
                PhysicsToolLayout.SLIDER_HEIGHT);
        Draw.textCentered(g, font, GameText.resolve(PhysicsTool.STRENGTH_IS.with(strength)),
                sx + PhysicsToolLayout.SLIDER_WIDTH / 2, sy + (PhysicsToolLayout.SLIDER_HEIGHT - font.lineHeight) / 2
                        + 1, colours.text());
        final String[] hints = {GameText.resolve(PhysicsTool.SCROLL_MODE),
                GameText.resolve(PhysicsTool.SCROLL_STRENGTH), GameText.resolve(PhysicsTool.THE_STRENGTH)};
        for (int line = 0; line < hints.length; line++) {
            Draw.textScaled(g, font, hints[line], x + PhysicsToolLayout.HINT_X, y + PhysicsToolLayout.HINT_Y
                    + line * PhysicsToolLayout.HINT_PITCH, colours.hint(), PhysicsToolLayout.SMALL);
        }
        final PhysicsTool.Grab grab = held(game.player);
        if (grab != null && grab.body() instanceof ClientStructure structure) {
            label(g, font, colours, structure, grab);
        }
    }

    /* Beside the point held: the structure's name and mass, and how far it is held. */
    private static void label(final GuiGraphics g, final Font font, final PhysicsToolPalette.Colours colours,
                              final ClientStructure structure, final PhysicsTool.Grab grab) {
        final float[] on = ScreenEffects.toScreen(grab.heldAt());
        if (on == null) {
            return;
        }
        final UnitFormatter format = UnitFormatter.forCurrentLocale();
        final StructureForcesPayload told = StructureForcesClient.of(structure.entity().getId());
        final String name = structure.entity().getName().getString();
        final String first = told == null ? name : GameText.resolve(PhysicsTool.NAMED.with(name, GameText.resolve(
                told.mass() >= TONNE ? ForceDiagramTexts.TONNES.with(format.decimal(told.mass() / TONNE))
                        : ForceDiagramTexts.KILOGRAMS.with(format.decimal(told.mass())))));
        final String second = GameText.resolve(PhysicsTool.HELD_AWAY.with(format.decimal(grab.distance())));
        final int w = Math.max(font.width(first), font.width(second));
        final int px = Math.round(on[0] * g.guiWidth());
        final int py = Math.round(on[1] * g.guiHeight());
        final int lx = Math.max(2, px - LABEL_GAP - w);
        final int ly = py + 4;
        g.fill(lx - 3, ly - 2, lx + w + 3, ly + 2 * font.lineHeight + 1, colours.label());
        Draw.text(g, font, first, lx, ly, colours.labelName());
        Draw.text(g, font, second, lx, ly + font.lineHeight, colours.labelHeld());
    }

    /* The beam from the muzzle to the point held, bent, with its dots and its ring, and the held structure's box. */
    private static void renderBeam(final RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        final Minecraft game = Minecraft.getInstance();
        final LocalPlayer player = game.player;
        final PhysicsTool.Grab grab = player == null ? null : held(player);
        if (grab == null) {
            return;
        }
        final PhysicsToolPalette.Colours colours = PhysicsToolPalette.get();
        final Camera camera = event.getCamera();
        final Vec3 eye = camera.getPosition();
        final float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        final Vec3 look = player.getViewVector(partialTick);
        final Vec3 right = look.cross(new Vec3(0, 1, 0)).lengthSqr() < 1.0e-6 ? new Vec3(1, 0, 0)
                : look.cross(new Vec3(0, 1, 0)).normalize();
        final Vec3 muzzle = camera.isDetached()
                ? player.getEyePosition(partialTick).add(0, -HAND_DOWN, 0).add(right.scale(HAND_RIGHT))
                : eye.add(look.scale(AHEAD)).add(right.scale(RIGHT)).add(0, -DOWN, 0);
        final Vec3 target = grab.heldAt();
        final Vec3 bend = muzzle.add(look.scale(target.distanceTo(muzzle) * STRAIGHT));
        final PoseStack pose = event.getPoseStack();
        final MultiBufferSource.BufferSource buffers = game.renderBuffers().bufferSource();
        pose.pushPose();
        pose.translate(-eye.x, -eye.y, -eye.z);
        final Matrix4f matrix = pose.last().pose();
        final VertexConsumer glow = buffers.getBuffer(RenderType.lightning());
        Vec3 last = muzzle;
        for (int piece = 1; piece <= PIECES; piece++) {
            final Vec3 next = along(muzzle, bend, target, (double) piece / PIECES);
            ribbon(glow, matrix, eye, last, next, GLOW, colours.beamGlow());
            ribbon(glow, matrix, eye, last, next, CORE, colours.beamCore());
            last = next;
        }
        for (int dot = 1; dot <= DOTS; dot++) {
            final Vec3 at = along(muzzle, bend, target, (double) dot / (DOTS + 1));
            ribbon(glow, matrix, eye, at.add(0, -DOT, 0), at.add(0, DOT, 0), DOT, colours.dot());
        }
        final Vec3 facing = eye.subtract(target).normalize();
        Vec3 across = facing.cross(new Vec3(0, 1, 0));
        across = across.lengthSqr() < 1.0e-6 ? new Vec3(1, 0, 0) : across.normalize();
        final Vec3 up = across.cross(facing);
        Vec3 ringLast = target.add(across.scale(RING));
        for (int piece = 1; piece <= RING_PIECES; piece++) {
            final double turn = 2 * Math.PI * piece / RING_PIECES;
            final Vec3 ringNext = target.add(across.scale(RING * Math.cos(turn))).add(up.scale(RING * Math.sin(turn)));
            ribbon(glow, matrix, eye, ringLast, ringNext, CORE, colours.ring());
            ringLast = ringNext;
        }
        buffers.endBatch(RenderType.lightning());
        LevelRenderer.renderLineBox(pose, buffers.getBuffer(RenderType.lines()), grab.body().worldBox(),
                (colours.ring() >> 16 & 0xFF) / 255F, (colours.ring() >> 8 & 0xFF) / 255F,
                (colours.ring() & 0xFF) / 255F, 1F);
        buffers.endBatch(RenderType.lines());
        pose.popPose();
    }

    /* The point {@code t} of the way along the curve from {@code a} by {@code b} to {@code c}. */
    private static Vec3 along(final Vec3 a, final Vec3 b, final Vec3 c, final double t) {
        final double u = 1 - t;
        return a.scale(u * u).add(b.scale(2 * u * t)).add(c.scale(t * t));
    }

    /*
     * A flat strip from {@code a} to {@code b}, {@code half} each side, turned to face the eye: wound both ways, as
     * the light it is drawn with shows only the faces turned to the eye.
     */
    private static void ribbon(final VertexConsumer out, final Matrix4f matrix, final Vec3 eye, final Vec3 a,
                               final Vec3 b, final double half, final int colour) {
        Vec3 side = b.subtract(a).cross(a.subtract(eye));
        if (side.lengthSqr() < 1.0e-12) {
            return;
        }
        // Narrower near the eye, so the beam leaving the muzzle under the eye is a beam and not a fan.
        side = side.normalize().scale(half * Math.min(1, a.distanceTo(eye) / TAPER));
        vertex(out, matrix, a.subtract(side), colour);
        vertex(out, matrix, a.add(side), colour);
        vertex(out, matrix, b.add(side), colour);
        vertex(out, matrix, b.subtract(side), colour);
        vertex(out, matrix, b.subtract(side), colour);
        vertex(out, matrix, b.add(side), colour);
        vertex(out, matrix, a.add(side), colour);
        vertex(out, matrix, a.subtract(side), colour);
    }

    private static void vertex(final VertexConsumer out, final Matrix4f matrix, final Vec3 at, final int colour) {
        out.addVertex(matrix, (float) at.x, (float) at.y, (float) at.z).setColor(colour);
    }
}
