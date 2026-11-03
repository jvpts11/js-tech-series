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
import dev.jstech.core.model.IAnimationListener;
import dev.jstech.core.model.ModelAnimator;
import dev.jstech.core.model.ModelPose;
import dev.jstech.space.ship.DoorMotion;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.ExtraCodecs;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;

/**
 * A small ship's doors as they move: a pressure door's leaf sliding into its bulkhead's wall as it opens, the
 * airlock's outer door's along the hull, and the lamp over each, red while it is shut and green while it is open; and
 * the boarding stair the outer door lets down on the land, coming down and going up as the approved storyboard has it
 * ({@link DoorMotion}). How each model's doors move is read from the file beside it,
 * {@code jsmodels/ship/<model>.doors.json}: each door by its piece's name, the bone of its leaf and how far it slides
 * open, its lamps' bones, the block of the model it works by, which keeps whether it is shut, and its stair's bones.
 * Each part drawn keeps its doors' motion, moved on once a frame by the time since the last.
 */
public final class ShipDoors {

    private static final Logger LOG = LogUtils.getLogger();
    private static final Map<ResourceLocation, Optional<Rig>> READ = new HashMap<>();
    private static final Map<KitInstance, Moving> MOVING = new WeakHashMap<>();
    private static final Driver DRIVER = new Driver();
    /* A model's place along a bone's move is in sixteenths of a block. */
    private static final float PIXELS = 16;
    /* The most time one frame moves the doors on, in seconds: a ship seen again after a while is not jumped. */
    private static final double LONGEST_FRAME = 0.25;
    /* How far apart the folded flights lie on one another, in blocks, as the storyboard lays them. */
    private static final float FOLDED_APART = 0.012F;
    /* How far below where they stand the handrails start rising, in blocks. */
    private static final float RAILS_DROP = 0.6F;
    private static final double HALF_TURN = 180;
    private static long readAt = -1;

    private ShipDoors() {
    }

    /** How the doors of the model {@code model} move, if it has doors. */
    public static Optional<Rig> rigOf(final ResourceLocation model) {
        if (readAt != KitModels.reloadedAt()) {
            READ.clear();
            readAt = KitModels.reloadedAt();
        }
        return READ.computeIfAbsent(model, ShipDoors::read);
    }

    /**
     * Stands the doors of {@code rig} in {@code instance} for this frame, each moving toward how {@code shut} says it
     * stands, its stair toward how {@code down} says; and puts in {@code hidden} each door's lamp that is not lit, and
     * a stair folded in its bay, or its handrails before they rise.
     */
    public static void pose(final KitInstance instance, final Rig rig, final Predicate<Door> shut,
                            final Predicate<Door> down, final Set<String> hidden) {
        final int count = rig.doors().size();
        final boolean[] open = new boolean[count];
        final boolean[] letDown = new boolean[count];
        for (int k = 0; k < count; k++) {
            final Door door = rig.doors().get(k);
            open[k] = !shut.test(door);
            letDown[k] = open[k] && door.stair().isPresent() && down.test(door);
        }
        Moving moving = MOVING.get(instance);
        if (moving == null || moving.doors.length != count) {
            moving = new Moving(count);
            for (int k = 0; k < count; k++) {
                moving.doors[k] = DoorMotion.standing(open[k], letDown[k]);
            }
            MOVING.put(instance, moving);
        }
        final long frame = KitClock.frame();
        if (moving.frame != frame) {
            final double now = KitClock.now();
            final double seconds = moving.frame < 0 ? 0 : Math.clamp(now - moving.at, 0, LONGEST_FRAME);
            for (int k = 0; k < count; k++) {
                final Door door = rig.doors().get(k);
                moving.doors[k].advance(seconds, open[k], letDown[k], door.stair().isPresent()
                        ? DoorMotion.OUTER_DOOR : DoorMotion.PRESSURE_DOOR);
            }
            moving.frame = frame;
            moving.at = now;
        }
        for (int k = 0; k < count; k++) {
            final Door door = rig.doors().get(k);
            final DoorMotion motion = moving.doors[k];
            hidden.add(open[k] ? door.shutLamp() : door.openLamp());
            door.stair().ifPresent(stair -> {
                if (motion.stowed()) {
                    hidden.addAll(stair.flights());
                    hidden.addAll(stair.treads());
                }
                if (motion.rails() <= 0) {
                    hidden.add(stair.rails());
                }
            });
        }
        instance.animate(frame, KitClock.now(), DRIVER, new Posed(rig, moving), IAnimationListener.NONE);
    }

    private static Optional<Rig> read(final ResourceLocation model) {
        final ResourceLocation file = ResourceLocation.fromNamespaceAndPath(model.getNamespace(),
                "jsmodels/" + model.getPath() + ".doors.json");
        final Optional<Resource> found = Minecraft.getInstance().getResourceManager().getResource(file);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        try (Reader reader = found.get().openAsReader()) {
            final JsonElement json = JsonParser.parseReader(reader);
            return Rig.CODEC.parse(JsonOps.INSTANCE, json).resultOrPartial(problem -> LOG.warn(
                    "The doors {} cannot be read: {}", file, problem));
        } catch (final IOException | RuntimeException wrong) {
            LOG.warn("The doors {} cannot be read", file, wrong);
            return Optional.empty();
        }
    }

    /**
     * How a model's doors move.
     *
     * @param doors its doors
     */
    public record Rig(List<Door> doors) {

        public static final Codec<Rig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Door.CODEC.listOf().fieldOf("doors").forGetter(Rig::doors)
        ).apply(instance, Rig::new));

        public Rig {
            doors = List.copyOf(doors);
        }
    }

    /**
     * A door of a model.
     *
     * @param piece      its piece's name, as the model's shape file has it
     * @param leaf       the bone of its leaf, drawn shut
     * @param travel     how far its leaf slides as it opens, in blocks, in the model's own space
     * @param shutLamp   the bone of its lamp lit while it is shut
     * @param openLamp   the bone of its lamp lit while it is open
     * @param cell       the block of the model it works by, which keeps whether it is shut
     * @param startsShut whether it stands shut as its part forms
     * @param stair      the stair its opening lets down on the land, if it has one
     */
    public record Door(String piece, String leaf, Vector3f travel, String shutLamp, String openLamp,
                       List<Integer> cell, boolean startsShut, Optional<Stair> stair) {

        public static final Codec<Door> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("piece").forGetter(Door::piece),
                Codec.STRING.fieldOf("leaf").forGetter(Door::leaf),
                ExtraCodecs.VECTOR3F.fieldOf("travel").forGetter(Door::travel),
                Codec.STRING.fieldOf("shut_lamp").forGetter(Door::shutLamp),
                Codec.STRING.fieldOf("open_lamp").forGetter(Door::openLamp),
                Codec.INT.listOf(3, 3).fieldOf("cell").forGetter(Door::cell),
                Codec.BOOL.optionalFieldOf("starts_shut", false).forGetter(Door::startsShut),
                Stair.CODEC.optionalFieldOf("stair").forGetter(Door::stair)
        ).apply(instance, Door::new));

        public Door {
            travel = new Vector3f(travel);
            cell = List.copyOf(cell);
        }
    }

    /**
     * The bones of a door's stair, drawn folded in its bay with its flights out straight.
     *
     * @param bay     the bay's two doors, the first opening by turning about the model's z axis backward, the
     *                second forward
     * @param flights the three flights, each hanging from the one before, the first turning about the hinge
     * @param treads  each flight's two treads, kept level, the first flight's first
     * @param rails   the handrails, drawn as they stand with the stair down
     * @param slide   how far the folded stair slides out under the sill, in blocks, in the model's own space
     */
    public record Stair(List<String> bay, List<String> flights, List<String> treads, String rails, Vector3f slide) {

        public static final Codec<Stair> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.listOf(2, 2).fieldOf("bay").forGetter(Stair::bay),
                Codec.STRING.listOf(3, 3).fieldOf("flights").forGetter(Stair::flights),
                Codec.STRING.listOf(6, 6).fieldOf("treads").forGetter(Stair::treads),
                Codec.STRING.fieldOf("rails").forGetter(Stair::rails),
                ExtraCodecs.VECTOR3F.fieldOf("slide").forGetter(Stair::slide)
        ).apply(instance, Stair::new));

        public Stair {
            bay = List.copyOf(bay);
            flights = List.copyOf(flights);
            treads = List.copyOf(treads);
            slide = new Vector3f(slide);
        }
    }

    /* A model's doors as a ship's stand this frame, each by its place among them. */
    private record Posed(Rig rig, Moving moving) {
    }

    /* A part's doors moving, and when they were last moved on. */
    private static final class Moving {

        private final DoorMotion[] doors;
        private long frame = -1;
        private double at;

        private Moving(final int count) {
            this.doors = new DoorMotion[count];
        }
    }

    /* Slides each leaf as far as it is open and turns its stair's bones as far as it has come down, every frame. */
    private static final class Driver implements IKitDriver<Posed> {

        private final Quaternionf turn = new Quaternionf();
        private final Vector3f lift = new Vector3f();

        @Override
        public void drive(final Posed posed, final ModelAnimator animator, final double seconds) {
            // Nothing plays on a door; its bones are moved below.
        }

        @Override
        public void move(final Posed posed, final ModelPose pose, final double seconds) {
            for (int k = 0; k < posed.rig().doors().size(); k++) {
                final Door door = posed.rig().doors().get(k);
                final DoorMotion motion = posed.moving().doors[k];
                final int leaf = pose.index(door.leaf());
                if (leaf >= 0 && motion.leaf() > 0) {
                    final float share = (float) motion.leaf() * PIXELS;
                    pose.addMove(leaf, door.travel().x() * share, door.travel().y() * share,
                            door.travel().z() * share);
                }
                door.stair().ifPresent(stair -> stair(stair, motion, pose));
            }
        }

        @Override
        public boolean movesBones() {
            return true;
        }

        /* The stair's bones where the storyboard has them now. */
        private void stair(final Stair stair, final DoorMotion motion, final ModelPose pose) {
            final double bay = Math.toRadians(motion.bay());
            turn(pose, stair.bay().get(0), -bay);
            turn(pose, stair.bay().get(1), bay);
            final double first = Math.toRadians(motion.firstFlight());
            final double folded = Math.toRadians(HALF_TURN * motion.fold());
            // Each flight's turn in the model, the second folded back on the first, the third out along it again.
            final double[] along = {first, first + folded, first};
            final int hinge = pose.index(stair.flights().get(0));
            if (hinge >= 0) {
                final float slid = (float) motion.slid() * PIXELS;
                pose.addMove(hinge, stair.slide().x() * slid, stair.slide().y() * slid, stair.slide().z() * slid);
            }
            turn(pose, stair.flights().get(0), first);
            turn(pose, stair.flights().get(1), folded);
            turn(pose, stair.flights().get(2), -folded);
            if (motion.fold() > 0) {
                // Folded, each lies on the one before, a hair above it as the model stands.
                apart(pose, stair.flights().get(1), along[0]);
                apart(pose, stair.flights().get(2), along[1]);
            }
            for (int t = 0; t < stair.treads().size(); t++) {
                turn(pose, stair.treads().get(t), -along[t / 2]);
            }
            final int rails = pose.index(stair.rails());
            if (rails >= 0 && motion.rails() < 1) {
                pose.addMove(rails, 0, (float) (motion.rails() - 1) * RAILS_DROP * PIXELS, 0);
            }
        }

        /* Turns {@code bone} by {@code angle} radians about the model's z axis, if the model has it. */
        private void turn(final ModelPose pose, final String bone, final double angle) {
            final int index = pose.index(bone);
            if (index >= 0) {
                pose.setTurn(index, this.turn.identity().rotationZ((float) angle));
            }
        }

        /* Lifts {@code bone} a hair up the model, in the space of the flight it hangs from, turned {@code parent}. */
        private void apart(final ModelPose pose, final String bone, final double parent) {
            final int index = pose.index(bone);
            if (index >= 0) {
                this.lift.set(0, FOLDED_APART * PIXELS, 0).rotateZ((float) -parent);
                pose.addMove(index, this.lift.x(), this.lift.y(), this.lift.z());
            }
        }
    }
}
