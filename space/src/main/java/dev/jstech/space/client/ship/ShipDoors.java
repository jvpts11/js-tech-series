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
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.ExtraCodecs;
import org.joml.Vector3f;
import org.slf4j.Logger;

/**
 * A small ship's doors as they stand: a pressure door's leaf in its bulkhead's doorway, slid into the wall while it is
 * open, the airlock's outer door's along the hull, and the lamp over each, red while it is shut and green while it is
 * open. How each model's doors move is read from the file beside it, {@code jsmodels/ship/<model>.doors.json}: each
 * door by its piece's name, the bone of its leaf and how far it slides open, its lamps' bones, and the block of the
 * model it works by, which keeps whether it is shut.
 */
public final class ShipDoors {

    private static final Logger LOG = LogUtils.getLogger();
    private static final Map<ResourceLocation, Optional<Rig>> READ = new HashMap<>();
    private static final Driver DRIVER = new Driver();
    /* A model's place along a bone's move is in sixteenths of a block. */
    private static final float PIXELS = 16;
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
     * Stands the doors of {@code rig} in {@code instance} as {@code shut} says each stands, for this frame: an open
     * door's leaf slid away; and puts in {@code hidden} each door's lamp that is not lit, and what it lets down, a
     * stair, or its bay's doors, as {@code down} says it stands.
     */
    public static void pose(final KitInstance instance, final Rig rig, final Predicate<Door> shut,
                            final Predicate<Door> down, final Set<String> hidden) {
        final boolean[] shutNow = new boolean[rig.doors().size()];
        for (int k = 0; k < shutNow.length; k++) {
            final Door door = rig.doors().get(k);
            shutNow[k] = shut.test(door);
            hidden.add(shutNow[k] ? door.openLamp() : door.shutLamp());
            if (!door.letDown().isEmpty()) {
                hidden.addAll(down.test(door) ? door.stowed() : door.letDown());
            }
        }
        instance.animate(KitClock.frame(), KitClock.now(), DRIVER, new Posed(rig, shutNow), IAnimationListener.NONE);
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
     * @param letDown    the bones shown while what its opening lets down is down, a stair; hidden else
     * @param stowed     the bones shown while that is stowed, its bay's doors; hidden else
     */
    public record Door(String piece, String leaf, Vector3f travel, String shutLamp, String openLamp,
                       List<Integer> cell, boolean startsShut, List<String> letDown, List<String> stowed) {

        public static final Codec<Door> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("piece").forGetter(Door::piece),
                Codec.STRING.fieldOf("leaf").forGetter(Door::leaf),
                ExtraCodecs.VECTOR3F.fieldOf("travel").forGetter(Door::travel),
                Codec.STRING.fieldOf("shut_lamp").forGetter(Door::shutLamp),
                Codec.STRING.fieldOf("open_lamp").forGetter(Door::openLamp),
                Codec.INT.listOf(3, 3).fieldOf("cell").forGetter(Door::cell),
                Codec.BOOL.optionalFieldOf("starts_shut", false).forGetter(Door::startsShut),
                Codec.STRING.listOf().optionalFieldOf("let_down", List.of()).forGetter(Door::letDown),
                Codec.STRING.listOf().optionalFieldOf("stowed", List.of()).forGetter(Door::stowed)
        ).apply(instance, Door::new));

        public Door {
            travel = new Vector3f(travel);
            cell = List.copyOf(cell);
            letDown = List.copyOf(letDown);
            stowed = List.copyOf(stowed);
        }
    }

    /* A model's doors as a ship's stand this frame, shut or not, each by its place among them. */
    private record Posed(Rig rig, boolean[] shut) {
    }

    /* Slides an open door's leaf away, after its model's animations, every frame. */
    private static final class Driver implements IKitDriver<Posed> {

        @Override
        public void drive(final Posed posed, final ModelAnimator animator, final double seconds) {
            // Nothing plays on a door; its leaf is moved below.
        }

        @Override
        public void move(final Posed posed, final ModelPose pose, final double seconds) {
            for (int k = 0; k < posed.shut().length; k++) {
                if (posed.shut()[k]) {
                    continue;
                }
                final Door door = posed.rig().doors().get(k);
                final int bone = pose.index(door.leaf());
                if (bone >= 0) {
                    pose.addMove(bone, door.travel().x() * PIXELS, door.travel().y() * PIXELS,
                            door.travel().z() * PIXELS);
                }
            }
        }

        @Override
        public boolean movesBones() {
            return true;
        }
    }
}
