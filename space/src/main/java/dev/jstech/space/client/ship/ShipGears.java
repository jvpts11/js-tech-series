/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.ship;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.client.model.kit.IKitDriver;
import dev.jstech.core.client.model.kit.KitClock;
import dev.jstech.core.client.model.kit.KitInstance;
import dev.jstech.core.client.model.kit.KitModels;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.model.IAnimationListener;
import dev.jstech.core.model.ModelAnimator;
import dev.jstech.core.model.ModelPose;
import dev.jstech.space.ship.AirReserveEntity;
import dev.jstech.space.ship.GearMotion;
import dev.jstech.space.ship.ShipReadings;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.ExtraCodecs;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;

/**
 * A small ship's gear, moving: the struts folding into their bays and the bays' doors shutting after them as it goes
 * up, the other way as it comes down, and the wheels turning as it rolls. How each model's gear moves is read from the
 * file beside it, {@code jsmodels/ship/<model>.gear.json}: each bone that moves, how it moves (a strut that folds, a
 * door that hangs open, wheels that roll), about which axis and how far. Each ship drawn keeps its own
 * {@link GearMotion}, moved on once a frame by the time since the last.
 */
public final class ShipGears {

    private static final Logger LOG = LogUtils.getLogger();
    private static final Map<ResourceLocation, Optional<Rig>> READ = new HashMap<>();
    private static final Map<AirReserveEntity, Moving> MOVING = new WeakHashMap<>();
    private static final Driver DRIVER = new Driver();
    /* The most time one frame moves the gear on, in seconds: a ship seen again after a while is not jumped. */
    private static final double LONGEST_FRAME = 0.25;
    private static long readAt = -1;

    private ShipGears() {
    }

    /**
     * The gear of the ship {@code console} flies, moved on to this frame as its readings say: toward down while its
     * gear is down, toward up while not, its wheels rolling while it stands on the land.
     */
    public static GearMotion of(final AirReserveEntity console) {
        final ShipReadings readings = console.flight();
        final Moving moving = MOVING.computeIfAbsent(console, seen -> new Moving(GearMotion.standing(
                readings.gearDown())));
        final long frame = KitClock.frame();
        if (moving.frame != frame) {
            final double now = KitClock.now();
            final double seconds = moving.frame < 0 ? 0 : Math.clamp(now - moving.at, 0, LONGEST_FRAME);
            moving.motion.advance(seconds, readings.gearDown());
            if (readings.grounded() && readings.gearDown()) {
                moving.motion.roll(readings.speed() * seconds);
            }
            moving.frame = frame;
            moving.at = now;
        }
        return moving.motion;
    }

    /** How the gear of the model {@code model} moves, if it has gear that moves. */
    public static Optional<Rig> rigOf(final ResourceLocation model) {
        if (readAt != KitModels.reloadedAt()) {
            READ.clear();
            readAt = KitModels.reloadedAt();
        }
        return READ.computeIfAbsent(model, ShipGears::read);
    }

    /**
     * Stands the bones of {@code rig} in {@code instance} as {@code gear} has them, worked out for this frame; the
     * struts that fold go in {@code hidden} once the gear is all the way up, out of sight behind its doors.
     */
    public static void pose(final KitInstance instance, final Rig rig, final GearMotion gear,
                            final Set<String> hidden) {
        instance.animate(KitClock.frame(), KitClock.now(), DRIVER, new Posed(rig, gear), IAnimationListener.NONE);
        if (gear.stowed()) {
            for (final Bone bone : rig.bones()) {
                if (bone.kind() != Bone.Kind.DOOR) {
                    hidden.add(bone.bone());
                }
            }
        }
    }

    private static Optional<Rig> read(final ResourceLocation model) {
        final ResourceLocation file = ResourceLocation.fromNamespaceAndPath(model.getNamespace(),
                "jsmodels/" + model.getPath() + ".gear.json");
        final Optional<Resource> found = Minecraft.getInstance().getResourceManager().getResource(file);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        try (Reader reader = found.get().openAsReader()) {
            final JsonElement json = JsonParser.parseReader(reader);
            return Rig.CODEC.parse(JsonOps.INSTANCE, json).resultOrPartial(problem -> LOG.warn(
                    "The gear {} cannot be read: {}", file, problem));
        } catch (final IOException | RuntimeException wrong) {
            LOG.warn("The gear {} cannot be read", file, wrong);
            return Optional.empty();
        }
    }

    /**
     * How a model's gear moves.
     *
     * @param bones the bones that move
     */
    public record Rig(List<Bone> bones) {

        public static final Codec<Rig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Bone.CODEC.listOf().fieldOf("bones").forGetter(Rig::bones)
        ).apply(instance, Rig::new));

        public Rig {
            bones = List.copyOf(bones);
        }
    }

    /**
     * A bone of a model's gear that moves.
     *
     * @param bone    the bone, by its name
     * @param kind    how it moves
     * @param axis    the axis it turns about, in the model's own space: turning it by a positive angle folds a
     *                strut, opens a door or rolls wheels forward
     * @param degrees how far it turns, folded or hanging open, in degrees; nothing for wheels
     * @param radius  the wheels' radius, in blocks; nothing for a strut or a door
     */
    public record Bone(String bone, Kind kind, Vector3f axis, float degrees, float radius) {

        public static final Codec<Bone> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("bone").forGetter(Bone::bone),
                StableCodecs.byName(Kind.class).fieldOf("kind").forGetter(Bone::kind),
                ExtraCodecs.VECTOR3F.fieldOf("axis").forGetter(Bone::axis),
                Codec.FLOAT.optionalFieldOf("degrees", 0.0F).forGetter(Bone::degrees),
                Codec.FLOAT.optionalFieldOf("radius", 0.0F).forGetter(Bone::radius)
        ).apply(instance, Bone::new));

        public Bone {
            axis = new Vector3f(axis).normalize();
        }

        /** How a bone of a model's gear moves. */
        public enum Kind implements IStableName {
            /** A strut, folding into its bay as the gear goes up. */
            FOLD("fold"),
            /** A bay's door, hanging open while the gear is out and shut once it is in. */
            DOOR("door"),
            /** Wheels, turning as the ship rolls. */
            SPIN("spin");

            private final String serializedName;

            Kind(final String serializedName) {
                this.serializedName = serializedName;
            }

            @Override
            public String serializedName() {
                return serializedName;
            }
        }
    }

    /* A model's gear as a ship's stands it this frame. */
    private record Posed(Rig rig, GearMotion gear) {
    }

    /* Turns a gear's bones, after its model's animations, every frame: a strut folded, a door hanging, wheels rolled. */
    private static final class Driver implements IKitDriver<Posed> {

        private final Quaternionf turn = new Quaternionf();

        @Override
        public void drive(final Posed posed, final ModelAnimator animator, final double seconds) {
            // Nothing plays on a gear; its bones are turned below.
        }

        @Override
        public void move(final Posed posed, final ModelPose pose, final double seconds) {
            for (final Bone bone : posed.rig().bones()) {
                final int index = pose.index(bone.bone());
                if (index < 0) {
                    continue;
                }
                final double angle = switch (bone.kind()) {
                    case FOLD -> Math.toRadians(bone.degrees()) * posed.gear().folded();
                    case DOOR -> Math.toRadians(bone.degrees()) * (1 - posed.gear().shut());
                    case SPIN -> posed.gear().turned(bone.radius());
                };
                pose.setTurn(index, this.turn.identity().rotateAxis((float) angle, bone.axis()));
            }
        }

        @Override
        public boolean movesBones() {
            return true;
        }
    }

    /* A ship's gear and when it was last moved on. */
    private static final class Moving {

        private final GearMotion motion;
        private long frame = -1;
        private double at;

        private Moving(final GearMotion motion) {
            this.motion = motion;
        }
    }
}
