/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.effect;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.jstech.core.client.instancing.Instancing;
import dev.jstech.core.client.light.LightRenderTypes;
import dev.jstech.core.effect.ArcPath;
import dev.jstech.core.effect.EffectCurves;
import dev.jstech.core.effect.EffectDefinition;
import dev.jstech.core.effect.EffectPhysics;
import dev.jstech.core.effect.EffectSettings;
import dev.jstech.core.effect.Emitter;
import dev.jstech.core.effect.EmitterShape;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.SplittableRandom;
import java.util.function.Supplier;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * Plays the Core's effects on the player's game: each tick every effect emits as its emitters say (at their rate, in
 * their bursts, from their shapes, turned the way the effect faces), and every particle moves by the physics of where
 * it is (the dimension's pull, the air's density, the wind, the water it is in), dies in what it may not live in,
 * stops at blocks if it collides, leaves its ribbon and emits what it carries. Each frame they are drawn camera-facing
 * from the particle sheet, see-through ones back to front, every one of a layer in one batch (many at once on the
 * graphics card, while {@link Instancing#on} says so), with the beams, arcs and ribbons. The game's particle setting
 * and the player's budget thin what is emitted; an effect too far to be seen emits nothing.
 */
public final class EffectEngine {

    /** A tick, in seconds. */
    static final double TICK = 0.05;
    private static final ParticleStore STORE = new ParticleStore();
    /* The particles laid over what is behind them and those added to it, when drawn many at once. */
    private static final InstancedParticles LAID = new InstancedParticles();
    private static final InstancedParticles ADDED = new InstancedParticles();
    private static final List<RunningEffect> RUNNING = new ArrayList<>();
    private static final SplittableRandom RANDOM = new SplittableRandom();
    /* How many particles the game's setting lets through, and how much fewer the farthest emit near the budget. */
    private static final double DECREASED = 0.5;
    private static final double MINIMAL = 0.2;
    private static final double CROWDED = 0.75;
    private static final double FAR = 16.0;
    private static final double SIZE_SPREAD = 0.15;
    private static final double FULL_TURN = 2 * Math.PI;
    private static final double SMOOTHING = 0.1;
    private static final double NANOS_A_MILLI = 1_000_000.0;
    private static final float HALO = 3.0F;
    private static final float HALO_SHARE = 0.35F;
    /* An arc's glow is narrower, so its bends do not show as boxes. */
    private static final float ARC_HALO = 2.0F;
    private static final float ARC_HALO_SHARE = 0.2F;
    private static final float BRANCH_SHARE = 0.6F;
    private static final int CHANNEL = 255;
    private static final int RGB = 0xFFFFFF;
    private static final int ALPHA_SHIFT = 24;
    private static final double LEAST_SPEED = 1e-4;
    private static final int ARC_TICKS = 2;
    private static final int BRIGHTEST = 15;
    /* An effect that went further than this in a tick, in blocks, jumped: what it emits is not laid along the way. */
    private static final double JUMPED = 64.0;

    private static double cost;
    private static long tickNanos;
    private static int instancedLast;

    private EffectEngine() {
    }

    /** How many particles live now. */
    public static int particles() {
        return STORE.count;
    }

    /** How many particles were drawn many at once on the graphics card in the last frame. */
    public static int instancedLastFrame() {
        return instancedLast;
    }

    /** What the effects cost the player's game a frame, in milliseconds, smoothed. */
    public static double costMillis() {
        return cost / NANOS_A_MILLI;
    }

    /** How many effects play now. */
    public static int playing() {
        return RUNNING.size();
    }

    /**
     * Starts playing {@code definition}, the file {@code id}'s if it is one; held until {@link #stop} with
     * {@code holder}, or once for none.
     */
    static RunningEffect start(final ClientLevel level, final @Nullable ResourceLocation id,
                               final EffectDefinition definition, final @Nullable Object holder,
                               final Supplier<Vec3> at, final Supplier<Vec3> way, final Supplier<Optional<Vec3>> to,
                               final int colour, final float scale) {
        final RunningEffect effect = new RunningEffect(level, id, definition, holder, at, way, to, colour, scale,
                RANDOM.nextLong());
        RUNNING.add(effect);
        return effect;
    }

    /** Lets the effects {@code holder} holds go: they emit no more, and go when their particles have. */
    static void stop(final Object holder) {
        for (final RunningEffect effect : RUNNING) {
            if (effect.holder == holder) {
                effect.stopped = true;
            }
        }
    }

    /** The effect {@code holder} holds, if one plays. */
    static Optional<RunningEffect> held(final Object holder) {
        return RUNNING.stream().filter(effect -> effect.holder == holder && !effect.stopped).findFirst();
    }

    /** Takes every effect and particle away, as the world closes. */
    static void clear() {
        STORE.clear();
        RUNNING.clear();
    }

    /** The files were read again: their pictures are found again, and what plays from a file plays it as it is. */
    static void filesChanged() {
        EffectSprites.forget();
        MeshPoints.forget();
        for (final RunningEffect effect : RUNNING) {
            if (effect.id != null) {
                EffectFiles.get(effect.id).ifPresent(effect::replace);
            }
        }
    }

    /** Each tick on the player's game. */
    static void tick() {
        final long started = System.nanoTime();
        final Minecraft minecraft = Minecraft.getInstance();
        final ClientLevel level = minecraft.level;
        if (level == null) {
            clear();
            return;
        }
        final Vec3 eye = minecraft.gameRenderer.getMainCamera().getPosition();
        final double setting = switch (minecraft.options.particles().get()) {
            case ALL -> 1.0;
            case DECREASED -> DECREASED;
            case MINIMAL -> MINIMAL;
        };
        final int budget = EffectSettings.particles();
        final Iterator<RunningEffect> running = RUNNING.iterator();
        while (running.hasNext()) {
            final RunningEffect effect = running.next();
            if (effect.level != level) {
                running.remove();
                continue;
            }
            final Vec3 at = effect.at.get();
            effect.place = EffectPlace.of(level, at);
            if (!effect.started) {
                effect.started = true;
                once(effect, at, eye);
            }
            final double away = at.distanceTo(eye);
            final boolean seen = away <= effect.definition.reach();
            final double crowd = STORE.count > budget * CROWDED && away > FAR ? DECREASED : 1.0;
            if (seen) {
                emit(effect, at, setting * crowd, budget);
            }
            effect.lastAt = at;
            effect.elapsed += TICK;
            if (!effect.held() && effect.elapsed > effect.end() + TICK && effect.alive() == 0) {
                running.remove();
            }
        }
        step(level, setting, budget);
        tickNanos = System.nanoTime() - started;
    }

    /** Draws the particles, ribbons, beams and arcs of the frame. */
    static void render(final Camera camera, final float partialTick, final MultiBufferSource.BufferSource buffers) {
        final long started = System.nanoTime();
        final ClientLevel level = Minecraft.getInstance().level;
        if (level != null && (STORE.count > 0 || !RUNNING.isEmpty())) {
            final Vec3 eye = camera.getPosition();
            drawParticles(level, camera, eye, partialTick, buffers);
            drawRibbons(eye, partialTick, buffers);
            drawBeams(level, eye, buffers);
            buffers.endBatch(EffectRenderTypes.PARTICLES);
            buffers.endBatch(EffectRenderTypes.GLOWING);
            buffers.endBatch(EffectRenderTypes.RIBBONS);
            buffers.endBatch(LightRenderTypes.GLOW);
        }
        cost += (tickNanos + System.nanoTime() - started - cost) * SMOOTHING;
    }

    /* What an effect does once as it starts: its marks are laid, its screen effects reach whoever is near. */
    private static void once(final RunningEffect effect, final Vec3 at, final Vec3 eye) {
        for (final Emitter emitter : effect.definition.emitters()) {
            if (!emitter.enabled()) {
                continue;
            }
            final Emitter.Extra extra = emitter.extra();
            final double reach = extra.radius() * effect.scale;
            if (emitter.kind() == Emitter.Kind.MARK) {
                WorldMarks.add(effect.level, at, reach, colourOf(effect, emitter.look().colour().getFirst()),
                        extra.fade());
            } else if (emitter.kind() == Emitter.Kind.SCREEN && at.distanceTo(eye) < reach) {
                final float near = (float) (1 - at.distanceTo(eye) / reach);
                ScreenEffects.fire(at, (float) extra.flash() * near, (float) extra.shake() * near,
                        (float) extra.shock() * near, (float) extra.fade());
            }
        }
    }

    /*
     * Emits what the effect's emitters emit this tick, from where it is; one that moved emits along the way it went
     * since the tick before, so a fast engine leaves a line, not beads a tick apart.
     */
    private static void emit(final RunningEffect effect, final Vec3 at, final double thinned, final int budget) {
        final Vec3 way = effect.way.get();
        final double[] frame = frameOf(way);
        final Vec3 from = effect.lastAt == null || effect.lastAt.distanceToSqr(at) > JUMPED * JUMPED ? at
                : effect.lastAt;
        for (final EmitterRun run : effect.runs) {
            final Emitter emitter = run.emitter;
            if (!emitter.enabled() || emitter.kind() != Emitter.Kind.PARTICLES) {
                continue;
            }
            final Emitter.Emission emission = emitter.emission();
            int count = 0;
            while (run.nextBurst < emission.bursts().size()
                    && emission.bursts().get(run.nextBurst).at() <= effect.elapsed) {
                count += (int) Math.ceil(emission.bursts().get(run.nextBurst).count() * thinned);
                run.nextBurst++;
            }
            final boolean flowing = effect.held() || effect.elapsed < emission.duration();
            if (flowing) {
                run.carry += emission.rate() * TICK * thinned;
                count += (int) run.carry;
                run.carry -= (int) run.carry;
            }
            for (int i = 0; i < count; i++) {
                spawn(run, effect, count > 1 ? from.lerp(at, (i + 1.0) / count) : at, frame, budget);
            }
        }
    }

    /* A particle of {@code run} from its shape about {@code at}, turned by {@code frame}. */
    private static void spawn(final EmitterRun run, final RunningEffect effect, final Vec3 at, final double[] frame,
                              final int budget) {
        final Emitter emitter = run.emitter;
        final Emitter.Emission emission = emitter.emission();
        final double[] local = new double[3];
        final double[] way = new double[3];
        emission.shape().sample(RANDOM, local, way);
        if (emission.shape().type() == EmitterShape.Type.MESH) {
            MeshPoints.sample(emission.shape().model(), RANDOM, local);
        }
        final double scale = effect.scale;
        final double px = at.x + (frame[0] * local[0] + frame[3] * local[1] + frame[6] * local[2]) * scale;
        final double py = at.y + (frame[1] * local[0] + frame[4] * local[1] + frame[7] * local[2]) * scale;
        final double pz = at.z + (frame[2] * local[0] + frame[5] * local[1] + frame[8] * local[2]) * scale;
        final double speed = emission.speed().at(RANDOM.nextDouble());
        final double[] v = {
            (frame[0] * way[0] + frame[3] * way[1] + frame[6] * way[2]) * speed,
            (frame[1] * way[0] + frame[4] * way[1] + frame[7] * way[2]) * speed,
            (frame[2] * way[0] + frame[5] * way[1] + frame[8] * way[2]) * speed};
        add(run, effect, px, py, pz, v, budget);
    }

    private static void add(final EmitterRun run, final RunningEffect effect, final double px, final double py,
                            final double pz, final double[] v, final int budget) {
        final Emitter.Look look = run.emitter.look();
        final float lives = (float) Math.max(TICK, run.emitter.emission().life().at(RANDOM.nextDouble()));
        final float spins = (float) (Math.toRadians(look.spin()) * (RANDOM.nextBoolean() ? 1 : -1));
        final float sized = (float) (1 + SIZE_SPREAD * (RANDOM.nextDouble() * 2 - 1));
        STORE.add(run, px, py, pz, v, lives, (float) (RANDOM.nextDouble() * FULL_TURN), spins, sized,
                effect.colour, budget);
    }

    /* Every particle on by a tick, by the physics of where it is. */
    private static void step(final ClientLevel level, final double thinned, final int budget) {
        final double[] v = new double[3];
        final BlockPos.MutableBlockPos block = new BlockPos.MutableBlockPos();
        for (int i = STORE.count - 1; i >= 0; i--) {
            final EmitterRun run = STORE.owner[i];
            final Emitter emitter = run.emitter;
            STORE.age[i] += (float) TICK;
            if (STORE.age[i] >= STORE.life[i]) {
                STORE.remove(i);
                continue;
            }
            block.set(STORE.x[i], STORE.y[i], STORE.z[i]);
            final FluidState fluid = level.getFluidState(block);
            final Emitter.Medium medium = emitter.motion().medium();
            if (medium == Emitter.Medium.AIR && !fluid.isEmpty() || medium == Emitter.Medium.WATER
                    && fluid.isEmpty()) {
                STORE.remove(i);
                continue;
            }
            final EffectPlace place = run.effect.place;
            final Emitter.Motion motion = emitter.motion();
            v[0] = STORE.vx[i];
            v[1] = STORE.vy[i];
            v[2] = STORE.vz[i];
            EffectPhysics.step(v, fluid.isEmpty() ? place.physics() : place.underwater(), motion.moves(),
                    motion.gravity(), motion.drag(), motion.buoyancy(), TICK);
            STORE.lastX[i] = STORE.x[i];
            STORE.lastY[i] = STORE.y[i];
            STORE.lastZ[i] = STORE.z[i];
            final double nx = STORE.x[i] + v[0] * TICK;
            final double ny = STORE.y[i] + v[1] * TICK;
            final double nz = STORE.z[i] + v[2] * TICK;
            if (motion.collide() && solid(level, block.set(nx, ny, nz))) {
                v[0] = 0;
                v[1] = 0;
                v[2] = 0;
            } else {
                STORE.x[i] = nx;
                STORE.y[i] = ny;
                STORE.z[i] = nz;
            }
            STORE.vx[i] = (float) v[0];
            STORE.vy[i] = (float) v[1];
            STORE.vz[i] = (float) v[2];
            STORE.angle[i] += (float) (STORE.spin[i] * TICK);
            STORE.mark(i);
            carried(run, i, v, thinned, budget);
        }
    }

    /* What a particle carries emits from it, as many a tick as its rate says on average. */
    private static void carried(final EmitterRun run, final int i, final double[] v, final double thinned,
                                final int budget) {
        if (run.children.isEmpty()) {
            return;
        }
        final double speed = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        final Vec3 way = speed < LEAST_SPEED ? new Vec3(0, 1, 0) : new Vec3(v[0] / speed, v[1] / speed,
                v[2] / speed);
        final double[] frame = frameOf(way);
        final Vec3 at = new Vec3(STORE.x[i], STORE.y[i], STORE.z[i]);
        for (final EmitterRun child : run.children) {
            if (!child.emitter.enabled()) {
                continue;
            }
            final double expected = child.emitter.emission().rate() * TICK * thinned;
            int count = (int) expected;
            if (RANDOM.nextDouble() < expected - count) {
                count++;
            }
            for (int k = 0; k < count; k++) {
                spawn(child, run.effect, at, frame, budget);
            }
        }
    }

    private static boolean solid(final ClientLevel level, final BlockPos pos) {
        return level.getBlockState(pos).isCollisionShapeFullBlock(level, pos);
    }

    /*
     * The frame an effect's shapes are turned by: three columns, the way across, the way out (the effect's own) and
     * the way along, so a shape's Y is the way the effect faces.
     */
    private static double[] frameOf(final Vec3 way) {
        final Vec3 out = way.lengthSqr() < LEAST_SPEED ? new Vec3(0, 1, 0) : way.normalize();
        final Vec3 other = Math.abs(out.y) < 0.99 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        final Vec3 across = other.cross(out).normalize();
        final Vec3 along = across.cross(out).normalize();
        return new double[] {across.x, across.y, across.z, out.x, out.y, out.z, along.x, along.y, along.z};
    }

    private static int colourOf(final RunningEffect effect, final int own) {
        return effect.colour == 0 ? own : own & ~RGB | effect.colour & RGB;
    }

    /*
     * The particles, see-through ones back to front: many at once on the graphics card while it may be, each layer in
     * one go, or one by one into the batches of the layers.
     */
    private static void drawParticles(final ClientLevel level, final Camera camera, final Vec3 eye,
                                      final float partialTick, final MultiBufferSource.BufferSource buffers) {
        final Vector3f left = camera.getLeftVector();
        final Vector3f up = camera.getUpVector();
        final boolean together = Instancing.on();
        final long[] order = new long[STORE.count];
        int laid = 0;
        for (int i = 0; i < STORE.count; i++) {
            if (STORE.owner[i].emitter.look().blend() == Emitter.Blend.ADDITIVE) {
                drawParticle(level, i, eye, partialTick, left, up, together ? null
                        : buffers.getBuffer(EffectRenderTypes.GLOWING), ADDED);
            } else {
                final double dx = lerp(STORE.lastX[i], STORE.x[i], partialTick) - eye.x;
                final double dy = lerp(STORE.lastY[i], STORE.y[i], partialTick) - eye.y;
                final double dz = lerp(STORE.lastZ[i], STORE.z[i], partialTick) - eye.z;
                final float away = (float) (dx * dx + dy * dy + dz * dz);
                order[laid++] = (long) Float.floatToIntBits(away) << Integer.SIZE | i;
            }
        }
        // Back to front, so each see-through one lies over those behind it.
        Arrays.sort(order, 0, laid);
        final VertexConsumer consumer = laid > 0 && !together ? buffers.getBuffer(EffectRenderTypes.PARTICLES)
                : null;
        for (int k = laid - 1; k >= 0; k--) {
            drawParticle(level, (int) order[k], eye, partialTick, left, up, consumer, LAID);
        }
        instancedLast = together ? LAID.draw(EffectRenderTypes.PARTICLES, camera)
                + ADDED.draw(EffectRenderTypes.GLOWING, camera) : 0;
    }

    /* A particle, into {@code consumer}, or written to {@code together} to be drawn many at once for none. */
    private static void drawParticle(final ClientLevel level, final int i, final Vec3 eye, final float partialTick,
                                     final Vector3f left, final Vector3f up, @Nullable final VertexConsumer consumer,
                                     final InstancedParticles together) {
        final EmitterRun run = STORE.owner[i];
        final Emitter.Look look = run.emitter.look();
        final double share = STORE.age[i] / STORE.life[i];
        double size = EffectCurves.at(look.size(), share) * STORE.size[i] * run.effect.scale / 2;
        if (run.emitter.motion().expands()) {
            size *= run.effect.place.widening();
        }
        final int argb = colourOf(run.effect, EffectCurves.colourAt(look.colour(), share));
        final List<TextureAtlasSprite> frames = EffectSprites.of(look.sprite(), look.frames());
        if (frames.isEmpty()) {
            return;
        }
        final TextureAtlasSprite sprite = frames.get(Math.min(frames.size() - 1, (int) (share * frames.size())));
        final double x = lerp(STORE.lastX[i], STORE.x[i], partialTick);
        final double y = lerp(STORE.lastY[i], STORE.y[i], partialTick);
        final double z = lerp(STORE.lastZ[i], STORE.z[i], partialTick);
        final int light = lightAt(level, x, y, z, look.light());
        final float turn = STORE.angle[i] + STORE.spin[i] * partialTick * (float) TICK;
        if (consumer == null) {
            together.add((float) (x - eye.x), (float) (y - eye.y), (float) (z - eye.z), (float) size,
                    sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1(), argb, turn, light);
            return;
        }
        final float cos = (float) (Math.cos(turn) * size);
        final float sin = (float) (Math.sin(turn) * size);
        // The two half-diagonals of the square, turned by the particle's angle on the camera's plane.
        final float ax = left.x() * cos + up.x() * sin;
        final float ay = left.y() * cos + up.y() * sin;
        final float az = left.z() * cos + up.z() * sin;
        final float bx = up.x() * cos - left.x() * sin;
        final float by = up.y() * cos - left.y() * sin;
        final float bz = up.z() * cos - left.z() * sin;
        final float cx = (float) (x - eye.x);
        final float cy = (float) (y - eye.y);
        final float cz = (float) (z - eye.z);
        final int r = argb >> 16 & CHANNEL;
        final int g = argb >> 8 & CHANNEL;
        final int b = argb & CHANNEL;
        final int a = argb >>> ALPHA_SHIFT;
        consumer.addVertex(cx - ax - bx, cy - ay - by, cz - az - bz).setUv(sprite.getU1(), sprite.getV1())
                .setColor(r, g, b, a).setLight(light);
        consumer.addVertex(cx - ax + bx, cy - ay + by, cz - az + bz).setUv(sprite.getU1(), sprite.getV0())
                .setColor(r, g, b, a).setLight(light);
        consumer.addVertex(cx + ax + bx, cy + ay + by, cz + az + bz).setUv(sprite.getU0(), sprite.getV0())
                .setColor(r, g, b, a).setLight(light);
        consumer.addVertex(cx + ax - bx, cy + ay - by, cz + az - bz).setUv(sprite.getU0(), sprite.getV1())
                .setColor(r, g, b, a).setLight(light);
    }

    /* The particles' ribbons, from the oldest place each remembers to where it is now, fading to its tail. */
    private static void drawRibbons(final Vec3 eye, final float partialTick,
                                    final MultiBufferSource.BufferSource buffers) {
        for (int i = 0; i < STORE.count; i++) {
            final int kept = STORE.trailCount[i];
            final float[] ribbon = STORE.trail[i];
            if (ribbon == null || kept < 2) {
                continue;
            }
            final EmitterRun run = STORE.owner[i];
            final Emitter.Look look = run.emitter.look();
            final List<double[]> points = new ArrayList<>(kept + 1);
            final int keep = Math.min(kept, (int) Math.ceil(look.trail() / TICK));
            for (int k = keep; k >= 1; k--) {
                final int at = Math.floorMod(STORE.trailHead[i] - k, ParticleStore.TRAIL) * 3;
                points.add(new double[] {ribbon[at], ribbon[at + 1], ribbon[at + 2]});
            }
            points.add(new double[] {lerp(STORE.lastX[i], STORE.x[i], partialTick),
                lerp(STORE.lastY[i], STORE.y[i], partialTick), lerp(STORE.lastZ[i], STORE.z[i], partialTick)});
            final int argb = colourOf(run.effect, EffectCurves.colourAt(look.colour(), STORE.age[i] / STORE.life[i]));
            Ribbons.strip(buffers.getBuffer(look.blend() == Emitter.Blend.ADDITIVE ? LightRenderTypes.GLOW
                    : EffectRenderTypes.RIBBONS), eye, points, (float) (look.width() * run.effect.scale), argb,
                    true);
        }
    }

    /* The beams and arcs of the effects playing: from where each is to where it is aimed, or along its way. */
    private static void drawBeams(final ClientLevel level, final Vec3 eye,
                                  final MultiBufferSource.BufferSource buffers) {
        for (final RunningEffect effect : RUNNING) {
            for (final Emitter emitter : effect.definition.emitters()) {
                if (!emitter.enabled() || emitter.kind() != Emitter.Kind.BEAM && emitter.kind() != Emitter.Kind.ARC) {
                    continue;
                }
                final double left = effect.held() ? 1 : 1 - effect.elapsed / Math.max(TICK, emitter.extra().fade());
                if (left <= 0) {
                    continue;
                }
                final Vec3 from = effect.at.get();
                final Vec3 to = effect.to.get().orElseGet(() -> from.add(effect.way.get().normalize().scale(
                        emitter.extra().length() * effect.scale)));
                final int own = colourOf(effect, emitter.look().colour().getFirst());
                final int argb = (int) Math.round((own >>> ALPHA_SHIFT) * left) << ALPHA_SHIFT | own & RGB;
                final float width = (float) (emitter.look().width() * effect.scale);
                final VertexConsumer consumer = buffers.getBuffer(emitter.look().blend() == Emitter.Blend.ADDITIVE
                        ? LightRenderTypes.GLOW : EffectRenderTypes.RIBBONS);
                final double[] a = {from.x, from.y, from.z};
                final double[] b = {to.x, to.y, to.z};
                if (emitter.kind() == Emitter.Kind.BEAM) {
                    final List<double[]> line = List.of(a, b);
                    Ribbons.strip(consumer, eye, line, width, argb, false);
                    Ribbons.strip(consumer, eye, line, width * HALO, faded(argb, HALO_SHARE), false);
                } else {
                    final long seed = effect.seed + level.getGameTime() / ARC_TICKS;
                    final List<double[]> arc = ArcPath.of(a, b, emitter.extra().jagged(), seed);
                    Ribbons.strip(consumer, eye, arc, width, argb, false);
                    Ribbons.strip(consumer, eye, arc, width * ARC_HALO, faded(argb, ARC_HALO_SHARE), false);
                    for (final List<double[]> branch : ArcPath.branches(arc, emitter.extra().branches(),
                            emitter.extra().jagged(), seed)) {
                        Ribbons.strip(consumer, eye, branch, width * BRANCH_SHARE, faded(argb, BRANCH_SHARE), true);
                    }
                }
            }
        }
    }

    private static int faded(final int argb, final float share) {
        return Math.round((argb >>> ALPHA_SHIFT) * share) << ALPHA_SHIFT | argb & RGB;
    }

    /* The light at a place, as packed for drawing, brightened to the particle's own light. */
    private static int lightAt(final ClientLevel level, final double x, final double y, final double z,
                               final int own) {
        if (own >= BRIGHTEST) {
            return LightTexture.FULL_BRIGHT;
        }
        final int packed = LevelRenderer.getLightColor(level, BlockPos.containing(x, y, z));
        return own <= 0 ? packed : LightTexture.pack(Math.max(LightTexture.block(packed), own),
                LightTexture.sky(packed));
    }

    private static double lerp(final double from, final double to, final float share) {
        return from + (to - from) * share;
    }
}
