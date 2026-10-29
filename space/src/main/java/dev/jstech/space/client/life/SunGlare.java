/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.life;

import dev.jstech.core.client.sky.SkyPlace;
import dev.jstech.core.dimension.DimensionRulesData;
import dev.jstech.core.sky.Skies;
import dev.jstech.core.sky.SkySpec;
import dev.jstech.space.JsSpace;
import dev.jstech.space.api.SuitModule;
import dev.jstech.space.life.LifeSupport;
import dev.jstech.space.universe.SystemLayout;
import dev.jstech.space.universe.SystemSpaces;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * The Sun's glare where no air softens it, on the Moon and in space: looking near the Sun whites out the view, the
 * nearer the more, unless something stands between the eye and it. Only what is seen; it does no harm, as the dark the
 * suit's lamps light does none. A module that cuts glare, the gold visor lowered over the helmet's, cuts it by its
 * share, and the view through it is a little gold.
 */
public final class SunGlare {

    private static final ResourceLocation LAYER = ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "sun_glare");
    /* Air thinner than this, in kilopascals, softens nothing: a vacuum's, not even Mars' thin air. */
    private static final double NO_AIR = 0.1;
    /* The glare begins this far from the Sun and is whole this near it, as the cosines of the angles. */
    private static final double BEGINS = Math.cos(Math.toRadians(25));
    private static final double WHOLE = Math.cos(Math.toRadians(3));
    /* How white the view is at most, how far a block between is looked for, and below the horizon by how much. */
    private static final float MOST = 0.85F;
    private static final double REACH = 192;
    private static final float SET = -0.02F;
    /* The white of the glare, the gold of the visor, and how much of it is seen through. */
    private static final int GLARE_WHITE = 0xFFF8EC;
    private static final int VISOR_GOLD = 0xD4A23C;
    private static final float VISOR_TINT = 0.14F;
    private static final int OPAQUE = 255;
    private static final int ALPHA_SHIFT = 24;
    private static final int RGB = 0xFFFFFF;

    private SunGlare() {
    }

    /** Draws the glare over the world, under the HUD, once, from the mod's client entry point. */
    public static void listen(final IEventBus modEventBus) {
        modEventBus.addListener(RegisterGuiLayersEvent.class,
                event -> event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, LAYER, SunGlare::draw));
    }

    /**
     * How strongly the Sun glares into the eye of {@code camera} in {@code level}, before any visor: 0 where air
     * softens it, the Sun is down or out of sight, or the eye looks away from it, to 1 looking straight at it.
     */
    public static double glare(final ClientLevel level, final Camera camera) {
        final Vec3 eye = camera.getPosition();
        if (DimensionRulesData.of(level).at(eye.y).pressure() >= NO_AIR) {
            return 0;
        }
        final Vector3f sun = sunward(level, eye);
        if (sun == null) {
            return 0;
        }
        final double towards = camera.getLookVector().dot(sun);
        if (towards <= BEGINS) {
            return 0;
        }
        final Vec3 far = eye.add(sun.x * REACH, sun.y * REACH, sun.z * REACH);
        if (level.clip(new ClipContext(eye, far, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE,
                CollisionContext.empty())).getType() != HitResult.Type.MISS) {
            return 0;
        }
        final double t = Math.min(1, (towards - BEGINS) / (WHOLE - BEGINS));
        return t * t * (3 - 2 * t);
    }

    /** The share of the glare what {@code player} wears on the head cuts, all of it with the gold visor down. */
    public static double cut(final ClientLevel level, final Player player) {
        double cut = 0;
        for (final SuitModule module : LifeSupport.modules(level, player.getItemBySlot(EquipmentSlot.HEAD))) {
            if (module.kind() == SuitModule.Kind.GLARE) {
                cut += module.amount();
            }
        }
        return Math.min(1, cut);
    }

    private static void draw(final GuiGraphics graphics, final DeltaTracker delta) {
        final Minecraft minecraft = Minecraft.getInstance();
        final ClientLevel level = minecraft.level;
        final Player player = minecraft.player;
        if (level == null || player == null) {
            return;
        }
        final double cut = cut(level, player);
        final int width = graphics.guiWidth();
        final int height = graphics.guiHeight();
        if (cut > 0 && minecraft.options.getCameraType().isFirstPerson()) {
            graphics.fill(0, 0, width, height, argb(VISOR_TINT * (float) cut, VISOR_GOLD));
        }
        final double glare = glare(level, minecraft.gameRenderer.getMainCamera()) * (1 - cut);
        if (glare > 0) {
            graphics.fill(0, 0, width, height, argb(MOST * (float) glare, GLARE_WHITE));
        }
    }

    /* Where the Sun lies from {@code eye}, as a direction; none where the world has no sun, or it is down. */
    private static @Nullable Vector3f sunward(final ClientLevel level, final Vec3 eye) {
        if (SystemSpaces.systemOf(level.dimension()).isPresent()) {
            final double[] sun = SystemLayout.sun();
            return new Vector3f((float) (sun[0] - eye.x), (float) (sun[1] - eye.y), (float) (sun[2] - eye.z))
                    .normalize();
        }
        final SkySpec sky = Skies.of(level);
        if (sky.suns().isEmpty()) {
            return null;
        }
        final Vector3f sun = SkyPlace.at(level, sky, eye.x, eye.z).sun(sky.suns().getFirst().offset()).normalize();
        return sun.y < SET ? null : sun;
    }

    private static int argb(final float alpha, final int rgb) {
        return Math.round(Math.min(1, alpha) * OPAQUE) << ALPHA_SHIFT | rgb & RGB;
    }
}
