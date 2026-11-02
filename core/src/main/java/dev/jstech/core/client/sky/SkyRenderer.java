/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.sky;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import dev.jstech.core.client.weather.WeatherClient;
import dev.jstech.core.sky.SkyMath;
import dev.jstech.core.sky.SkySpec;
import dev.jstech.core.weather.WeatherKind;
import java.util.Objects;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Draws a sky as its {@link SkySpec} says, where the game would draw its own: the dome in the sky's colour, dimmed by
 * the distance of the sun and by an eclipse; the sunrise and sunset in their colour; the stars, square as the game's,
 * the band of the galaxy ({@link SkyGalaxy}) and the nebulae, turning with the sky; the zodiacal light round the sun at
 * night ({@link SkyZodiacal}); each sun in its kind's colour and size; the planets and moons ({@link SkyBodies}); the
 * aurora over the north and the meteors ({@link SkyMeteors}) at night. The stars are built once for each sky and kept.
 */
final class SkyRenderer {

    private static final ResourceLocation SUN = ResourceLocation.withDefaultNamespace("textures/environment/sun.png");
    private static final float DISTANCE = 100F;
    private static final float SUN_HALF = 30F;
    private static final float DOME = 16F;
    private static final float DOME_REACH = 512F;
    private static final int DOME_STEP = 45;
    private static final long STAR_SEED = 10_842L;
    /*
     * The galaxy's faint stars: how many beside the sky's own; how wide their band is from its middle, in degrees, at
     * its ends and the more at its heart, and how far the heart's widening reaches; how bright most are, a few more.
     */
    private static final float GALAXY_SHARE = 1.2F;
    private static final double BAND_RIM = 4.5;
    private static final double BAND_HEART = 6.0;
    private static final double BAND_HEART_REACH = 40.0;
    private static final float BRIGHT_SHARE = 0.05F;
    private static final float FAINT = 0.25F;
    private static final float FAINT_BRIGHT = 0.6F;
    private static final float FAINT_SPAN = 0.3F;
    private static final int NEBULA_LOBES = 5;
    private static final int FAN_STEPS = 20;
    private static final int AURORA_STRIPS = 48;
    private static final float AURORA_FROM = -70F;
    private static final float AURORA_TO = 70F;
    private static final float AURORA_FOOT = 8F;
    private static final float AURORA_HEAD = 42F;
    private static final float SUNRISE_REACH = 120F;
    private static final float SUNRISE_DEPTH = 40F;
    private static final int SUNRISE_STEPS = 16;
    private static final float HORIZON_DROP = 12F;
    /* How bright the game's stars shine at midnight. */
    private static final float MIDNIGHT_STARS = 0.5F;
    /* How much of the suns', moons' and planets' light a whole veil of weather hides: a dimmed disc still shows. */
    private static final float LIGHTS_VEILED = 0.65F;

    private static @Nullable VertexBuffer dome;
    private static @Nullable VertexBuffer dark;
    private static @Nullable VertexBuffer stars;
    private static @Nullable Object starsFor;

    private SkyRenderer() {
    }

    /** Draws {@code sky} for {@code level}; whether it drew, which it always does but under fog or blindness. */
    static boolean render(final ClientLevel level, final SkySpec sky, final int ticks, final float partialTick,
                          final Matrix4f modelView, final Camera camera, final Matrix4f projection,
                          final boolean foggy, final Runnable setupFog) {
        setupFog.run();
        if (foggy || hidden(camera)) {
            return true;
        }
        final PoseStack pose = new PoseStack();
        pose.mulPose(modelView);
        // The hour and the sky's lean where the camera is: the clock's, or on a round world the place's own.
        final SkyPlace place = SkyPlace.of(level, sky);
        final float celestial = (float) place.celestial();
        final float daylight = place.skyLight();
        final float light = (float) (Math.min(1.0, SkySight.sunlight(sky)) * SkySight.eclipseLight(level, sky,
                place.celestial()));
        // A weather that hides the sky, as a dust storm: its fog's colour over the dome, the lights behind it dimmed.
        final WeatherKind veiling = WeatherClient.veiling();
        final float veil = veiling == null ? 0F : (float) veiling.veil();
        final Vec3 open = sky.skyColour().map(c -> SkySight.shown(c, daylight))
                .orElseGet(() -> level.getSkyColor(camera.getPosition(), partialTick)).scale(light);
        final Vec3 colour = veiling == null ? open : open.lerp(dust(veiling, daylight * light), veil);
        FogRenderer.levelFogColor();
        RenderSystem.depthMask(false);
        RenderSystem.setShaderColor((float) colour.x, (float) colour.y, (float) colour.z, 1.0F);
        domes();
        draw(Objects.requireNonNull(dome), pose, projection);
        // The dome took the sky's colour from the shader; what is drawn over it brings its own.
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        sunrise(level, pose, celestial, partialTick);
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        final float clear = 1.0F - level.getRainLevel(partialTick);
        final float seen = clear * (1F - veil);
        final float shining = clear * (1F - veil * LIGHTS_VEILED);
        pose.pushPose();
        // The sky's pole stands as high over the north as the place's latitude.
        pose.mulPose(Axis.XP.rotationDegrees((float) place.latitude()));
        pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(celestial * 360.0F));
        final float night = Math.max(place.starlight(), (float) sky.stars().byDay() * MIDNIGHT_STARS) * seen;
        if (night > 0) {
            FogRenderer.setupNoFog();
            starField(sky, pose, projection, (float) (night * sky.stars().brightness()));
            // The galaxy and the zodiacal light shine whole at midnight, where the game's stars shine at half.
            sky.galaxy().ifPresent(galaxy -> SkyGalaxy.render(galaxy, pose, Math.min(1F, night / MIDNIGHT_STARS)));
            for (final SkySpec.Nebula nebula : sky.nebulae()) {
                nebula(nebula, pose, night);
            }
            // The zodiacal light lies round the first sun, along its path: only at night proper, not by day's stars.
            sky.zodiacalLight().ifPresent(zodiacal -> {
                pose.pushPose();
                pose.mulPose(Axis.XP.rotationDegrees((float) (sky.suns().isEmpty() ? 0.0
                        : sky.suns().get(0).offset() * 360.0)));
                SkyZodiacal.render(zodiacal, pose, Math.min(1F, place.starlight() * seen / MIDNIGHT_STARS));
                pose.popPose();
            });
            setupFog.run();
        }
        pose.popPose();
        for (final SkySpec.Sun sun : sky.suns()) {
            if (sun.drawn()) {
                sun(sun, pose, celestial, shining);
            }
        }
        SkyBodies.render(level, sky, pose, celestial, colour, shining, night);
        pose.popPose();
        sky.aurora().ifPresent(aurora -> aurora(aurora, pose, ticks + partialTick, night * 2F));
        // Meteors burn in the air between the eye and the haze, which never hides them as it hides the far ground.
        FogRenderer.setupNoFog();
        SkyMeteors.render(sky, pose, ticks + partialTick, place.starlight() * seen);
        setupFog.run();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        final Minecraft minecraft = Minecraft.getInstance();
        if (sky.ground() && minecraft.player != null && minecraft.player.getEyePosition(partialTick).y
                - level.getLevelData().getHorizonHeight(level) < 0.0) {
            RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, 1.0F);
            pose.pushPose();
            pose.translate(0.0F, HORIZON_DROP, 0.0F);
            draw(Objects.requireNonNull(dark), pose, projection);
            pose.popPose();
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.depthMask(true);
        return true;
    }

    /**
     * Draws the stars, the galaxy's band and the nebulae of {@code sky}, turned as they stand with the sun
     * {@code celestial} turns round the sky at {@code latitude}, as bright as the starlight {@code night}, over what
     * is drawn.
     */
    static void stars(final SkySpec sky, final Matrix4f modelView, final Matrix4f projection, final double celestial,
                      final double latitude, final float night) {
        if (night <= 0) {
            return;
        }
        final PoseStack pose = new PoseStack();
        pose.mulPose(modelView);
        pose.mulPose(Axis.XP.rotationDegrees((float) latitude));
        pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
        pose.mulPose(Axis.XP.rotationDegrees((float) (celestial * 360.0)));
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        FogRenderer.setupNoFog();
        starField(sky, pose, projection, (float) (night * sky.stars().brightness()));
        sky.galaxy().ifPresent(galaxy -> SkyGalaxy.render(galaxy, pose, Math.min(1F, night / MIDNIGHT_STARS)));
        for (final SkySpec.Nebula nebula : sky.nebulae()) {
            nebula(nebula, pose, night);
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
    }

    /* A veiling weather's colour over the sky, its fog's or else its own, as lit as the day {@code lit} is. */
    private static Vec3 dust(final WeatherKind veiling, final float lit) {
        final float[] colour = rgb(veiling.fogColour().orElse(veiling.colour()));
        return new Vec3(colour[0] * lit, colour[1] * lit, colour[2] * lit);
    }

    /* Whether the camera sees no sky: in powder snow or lava, or blinded. */
    private static boolean hidden(final Camera camera) {
        final FogType fog = camera.getFluidInCamera();
        if (fog == FogType.POWDER_SNOW || fog == FogType.LAVA) {
            return true;
        }
        return camera.getEntity() instanceof LivingEntity living
                && (living.hasEffect(MobEffects.BLINDNESS) || living.hasEffect(MobEffects.DARKNESS));
    }

    private static void domes() {
        if (dome == null) {
            dome = keep(disc(DOME));
            dark = keep(disc(-DOME));
        }
    }

    /* The sunrise or sunset's fan, in the colour the sky's kind of place gives. */
    private static void sunrise(final ClientLevel level, final PoseStack pose, final float celestial,
                                final float partialTick) {
        final float[] glow = level.effects().getSunriseColor(celestial, partialTick);
        if (glow == null) {
            return;
        }
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(90.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(Mth.sin(celestial * Mth.TWO_PI) < 0.0F ? 180.0F : 0.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(90.0F));
        final Matrix4f matrix = pose.last().pose();
        final BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN,
                DefaultVertexFormat.POSITION_COLOR);
        builder.addVertex(matrix, 0.0F, DISTANCE, 0.0F).setColor(glow[0], glow[1], glow[2], glow[3]);
        for (int j = 0; j <= SUNRISE_STEPS; j++) {
            final float angle = j * Mth.TWO_PI / SUNRISE_STEPS;
            builder.addVertex(matrix, Mth.sin(angle) * SUNRISE_REACH, Mth.cos(angle) * SUNRISE_REACH,
                    -Mth.cos(angle) * SUNRISE_DEPTH * glow[3]).setColor(glow[0], glow[1], glow[2], 0.0F);
        }
        drawNow(builder);
        pose.popPose();
    }

    /* The stars, built once for the sky's count and galaxy, drawn as bright as the night is. */
    private static void starField(final SkySpec sky, final PoseStack pose, final Matrix4f projection,
                                  final float brightness) {
        final Object wanted = sky.stars().count() + "/" + sky.galaxy().map(g -> g.tilt() + "/" + g.centre())
                .orElse("none");
        if (!wanted.equals(starsFor)) {
            if (stars != null) {
                stars.close();
            }
            stars = keep(starMesh(sky));
            starsFor = wanted;
        }
        final VertexBuffer kept = stars;
        if (kept == null) {
            return;
        }
        RenderSystem.setShaderColor(brightness, brightness, brightness, brightness);
        kept.bind();
        kept.drawWithShader(pose.last().pose(), projection, GameRenderer.getPositionColorShader());
        VertexBuffer.unbind();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /*
     * The stars, scattered as the game scatters its own, and more crowding along the galaxy's band, faint, the band
     * wider towards its heart.
     */
    private static @Nullable MeshData starMesh(final SkySpec sky) {
        final RandomSource random = RandomSource.create(STAR_SEED);
        final BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_COLOR);
        final int count = sky.stars().count();
        for (int i = 0; i < count; i++) {
            final Vector3f at = new Vector3f(random.nextFloat() * 2 - 1, random.nextFloat() * 2 - 1,
                    random.nextFloat() * 2 - 1);
            final float length = at.lengthSquared();
            if (length > 0.01F && length < 1.0F) {
                star(builder, at, random, 1F);
            }
        }
        sky.galaxy().ifPresent(galaxy -> {
            final Quaternionf lean = new Quaternionf().rotateX((float) Math.toRadians(galaxy.tilt()));
            final int crowd = (int) (count * GALAXY_SHARE);
            for (int i = 0; i < crowd; i++) {
                final double l = random.nextDouble() * 360 - 180;
                final double width = BAND_RIM + BAND_HEART * Math.exp(-(l / BAND_HEART_REACH) * (l / BAND_HEART_REACH));
                final double b = random.nextGaussian() * width;
                final float bright = random.nextFloat() < BRIGHT_SHARE ? FAINT_BRIGHT + random.nextFloat() * FAINT_SPAN
                        : FAINT + random.nextFloat() * FAINT_SPAN;
                star(builder, SkyGalaxy.direction(galaxy, lean, l, b), random, bright);
            }
        });
        return builder.build();
    }

    /* One square star towards {@code at}, a little turned, a little tinted, as bright as {@code bright} says. */
    private static void star(final BufferBuilder builder, final Vector3f at, final RandomSource random,
                             final float bright) {
        final float size = 0.15F + random.nextFloat() * 0.1F;
        final Vector3f centre = new Vector3f(at).normalize(DISTANCE);
        final Quaternionf turn = new Quaternionf().rotateTo(new Vector3f(0.0F, 0.0F, -1.0F), centre)
                .rotateZ((float) (random.nextDouble() * Mth.TWO_PI));
        final float warmth = random.nextFloat();
        final float red = (0.85F + 0.15F * warmth) * bright;
        final float green = 0.95F * bright;
        final float blue = (1.0F - 0.15F * warmth) * bright;
        builder.addVertex(new Vector3f(centre).add(new Vector3f(size, -size, 0.0F).rotate(turn)))
                .setColor(red, green, blue, 1.0F);
        builder.addVertex(new Vector3f(centre).add(new Vector3f(size, size, 0.0F).rotate(turn)))
                .setColor(red, green, blue, 1.0F);
        builder.addVertex(new Vector3f(centre).add(new Vector3f(-size, size, 0.0F).rotate(turn)))
                .setColor(red, green, blue, 1.0F);
        builder.addVertex(new Vector3f(centre).add(new Vector3f(-size, -size, 0.0F).rotate(turn)))
                .setColor(red, green, blue, 1.0F);
    }

    /* A nebula: a few soft lobes of its colour round its place, bright in the middle and gone at the rim. */
    private static void nebula(final SkySpec.Nebula nebula, final PoseStack pose, final float night) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        final float[] colour = rgb(nebula.colour());
        final float alpha = (float) (nebula.brightness() * night);
        final RandomSource random = RandomSource.create(Double.doubleToLongBits(nebula.yaw() * 31 + nebula.pitch()));
        for (int lobe = 0; lobe < NEBULA_LOBES; lobe++) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees((float) (nebula.yaw() + random.nextGaussian() * nebula.size() / 6)));
            pose.mulPose(Axis.XP.rotationDegrees((float) (90 - nebula.pitch() + random.nextGaussian()
                    * nebula.size() / 6)));
            final Matrix4f matrix = pose.last().pose();
            final float radius = (float) (Math.tan(Math.toRadians(nebula.size() / 2)) * DISTANCE
                    * (0.5 + random.nextFloat() * 0.5));
            final BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN,
                    DefaultVertexFormat.POSITION_COLOR);
            builder.addVertex(matrix, 0F, DISTANCE, 0F).setColor(colour[0], colour[1], colour[2], alpha / 2);
            for (int i = 0; i <= FAN_STEPS; i++) {
                final float angle = i * Mth.TWO_PI / FAN_STEPS;
                builder.addVertex(matrix, Mth.cos(angle) * radius, DISTANCE, Mth.sin(angle) * radius)
                        .setColor(colour[0], colour[1], colour[2], 0F);
            }
            drawNow(builder);
            pose.popPose();
        }
    }

    /* A sun where the clock puts it, offset round the sky as it says, in its kind's colour and size. */
    private static void sun(final SkySpec.Sun sun, final PoseStack pose, final float celestial, final float clear) {
        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees((float) ((celestial + sun.offset()) * 360.0)));
        final Matrix4f matrix = pose.last().pose();
        final float half = (float) (SUN_HALF * sun.apparentSize());
        final float[] colour = rgb(sun.kind().colour());
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, SUN);
        final BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_TEX_COLOR);
        builder.addVertex(matrix, -half, DISTANCE, -half).setUv(0, 0).setColor(colour[0], colour[1], colour[2], clear);
        builder.addVertex(matrix, half, DISTANCE, -half).setUv(1, 0).setColor(colour[0], colour[1], colour[2], clear);
        builder.addVertex(matrix, half, DISTANCE, half).setUv(1, 1).setColor(colour[0], colour[1], colour[2], clear);
        builder.addVertex(matrix, -half, DISTANCE, half).setUv(0, 1).setColor(colour[0], colour[1], colour[2], clear);
        drawNow(builder);
        pose.popPose();
    }

    /* The aurora: curtains over the north, their foot in its colour, fading into the top colour, waving. */
    private static void aurora(final SkySpec.Aurora aurora, final PoseStack pose, final float time,
                               final float night) {
        final float strength = (float) (aurora.strength() * Math.min(1F, night));
        if (strength <= 0) {
            return;
        }
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        final Matrix4f matrix = pose.last().pose();
        final float[] foot = rgb(aurora.colour());
        final float[] head = rgb(aurora.topColour());
        final BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < AURORA_STRIPS; i++) {
            final float from = curtain(i, time);
            final float to = curtain(i + 1, time);
            final float pulse = strength * (0.55F + 0.45F * Mth.sin(time * 0.03F + i * 0.7F));
            final float pulseNext = strength * (0.55F + 0.45F * Mth.sin(time * 0.03F + (i + 1) * 0.7F));
            auroraVertex(builder, matrix, from, AURORA_FOOT, foot, pulse);
            auroraVertex(builder, matrix, to, AURORA_FOOT, foot, pulseNext);
            auroraVertex(builder, matrix, to, AURORA_HEAD, head, 0F);
            auroraVertex(builder, matrix, from, AURORA_HEAD, head, 0F);
        }
        drawNow(builder);
    }

    /* Where strip {@code i} of the curtains stands round the north, waving with {@code time}, in degrees. */
    private static float curtain(final int i, final float time) {
        return AURORA_FROM + (AURORA_TO - AURORA_FROM) * i / AURORA_STRIPS + 3F * Mth.sin(time * 0.02F + i * 0.4F);
    }

    private static void auroraVertex(final BufferBuilder builder, final Matrix4f matrix, final float yaw,
                                     final float height, final float[] colour, final float alpha) {
        final float round = (float) Math.toRadians(yaw);
        final float up = (float) Math.toRadians(height);
        builder.addVertex(matrix, Mth.sin(round) * Mth.cos(up) * DISTANCE, Mth.sin(up) * DISTANCE,
                -Mth.cos(round) * Mth.cos(up) * DISTANCE).setColor(colour[0], colour[1], colour[2], alpha);
    }

    /* A flat disc above the camera, or below it for a negative height, as the game's sky is drawn on. */
    private static @Nullable MeshData disc(final float height) {
        final float reach = Math.signum(height) * DOME_REACH;
        final BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN,
                DefaultVertexFormat.POSITION);
        builder.addVertex(0.0F, height, 0.0F);
        for (int i = -180; i <= 180; i += DOME_STEP) {
            builder.addVertex(reach * Mth.cos(i * Mth.DEG_TO_RAD), height, DOME_REACH * Mth.sin(i * Mth.DEG_TO_RAD));
        }
        return builder.build();
    }

    /* A buffer kept on the card holding {@code mesh}; none for nothing to hold. */
    private static @Nullable VertexBuffer keep(final @Nullable MeshData mesh) {
        if (mesh == null) {
            return null;
        }
        final VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        buffer.bind();
        buffer.upload(mesh);
        VertexBuffer.unbind();
        return buffer;
    }

    private static void draw(final VertexBuffer buffer, final PoseStack pose, final Matrix4f projection) {
        buffer.bind();
        buffer.drawWithShader(pose.last().pose(), projection, GameRenderer.getPositionShader());
        VertexBuffer.unbind();
    }

    /** Draws what {@code builder} holds, if anything. */
    static void drawNow(final BufferBuilder builder) {
        final MeshData mesh = builder.build();
        if (mesh != null) {
            BufferUploader.drawWithShader(mesh);
        }
    }

    /** {@code 0xRRGGBB} as red, green and blue from 0 to 1. */
    static float[] rgb(final int colour) {
        return new float[] {(colour >> 16 & 0xFF) / 255F, (colour >> 8 & 0xFF) / 255F, (colour & 0xFF) / 255F};
    }
}
