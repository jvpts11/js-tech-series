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
import dev.jstech.core.client.model.kit.KitModels;
import dev.jstech.space.ship.SuitLockerEntity;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.ExtraCodecs;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.slf4j.Logger;

/**
 * A small ship's suit locker as it holds, as the approved locker draws it: on its shelf the bottles it holds, the
 * others' places empty; the oxygen panel's hose to the suit's pack while a suit hangs on the rack, coiled on its hook
 * while none does; and the suit itself on the rack, drawn by the locker ({@link SuitLockerRenderer}). How a model's
 * locker stands is read from the file beside it, {@code jsmodels/ship/<model>.locker.json}.
 */
public final class ShipLockers {

    private static final Logger LOG = LogUtils.getLogger();
    private static final Map<ResourceLocation, Optional<Rig>> READ = new HashMap<>();
    private static long readAt = -1;

    private ShipLockers() {
    }

    /** How the locker of the model {@code model} stands, if it has one; a finish's model reads its model's. */
    public static Optional<Rig> rigOf(final ResourceLocation model) {
        if (readAt != KitModels.reloadedAt()) {
            READ.clear();
            readAt = KitModels.reloadedAt();
        }
        return READ.computeIfAbsent(ShipPartsRenderer.cabinOf(model), ShipLockers::read);
    }

    /**
     * Puts in {@code hidden} what of {@code rig} {@code locker} does not show: the bottles past those it holds, and
     * the hose's lie it is not in; an empty locker's for none, a stand's.
     */
    public static void hide(final Rig rig, final @Nullable SuitLockerEntity locker, final Set<String> hidden) {
        final int bottles = locker == null ? 0 : locker.bottles();
        for (int k = bottles; k < rig.bottles().size(); k++) {
            hidden.add(rig.bottles().get(k));
        }
        final boolean suit = locker != null && locker.holds(SuitLockerEntity.Place.SUIT);
        hidden.add(suit ? rig.hoseCoiled() : rig.hoseSuit());
    }

    private static Optional<Rig> read(final ResourceLocation model) {
        final ResourceLocation file = ResourceLocation.fromNamespaceAndPath(model.getNamespace(),
                "jsmodels/" + model.getPath() + ".locker.json");
        final Optional<Resource> found = Minecraft.getInstance().getResourceManager().getResource(file);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        try (Reader reader = found.get().openAsReader()) {
            final JsonElement json = JsonParser.parseReader(reader);
            return Rig.CODEC.parse(JsonOps.INSTANCE, json).resultOrPartial(problem -> LOG.warn(
                    "The suit locker {} cannot be read: {}", file, problem));
        } catch (final IOException | RuntimeException wrong) {
            LOG.warn("The suit locker {} cannot be read", file, wrong);
            return Optional.empty();
        }
    }

    /**
     * How a model's suit locker stands.
     *
     * @param cell       the block of the model the locker is
     * @param rack       where the suit stands on its rack, the middle of its feet, in the model's own space
     * @param scale      how large the suit stands there, worn being 1
     * @param facing     which way the suit faces, in the model's own space
     * @param bottles    the bones of the bottles on its shelf, in the order they are filled
     * @param hoseSuit   the bone of the hose to the suit's pack
     * @param hoseCoiled the bone of the hose coiled on its hook
     */
    public record Rig(List<Integer> cell, Vector3f rack, float scale, Vector3f facing, List<String> bottles,
                      String hoseSuit, String hoseCoiled) {

        public static final Codec<Rig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.listOf(3, 3).fieldOf("cell").forGetter(Rig::cell),
                ExtraCodecs.VECTOR3F.fieldOf("rack").forGetter(Rig::rack),
                Codec.FLOAT.fieldOf("scale").forGetter(Rig::scale),
                ExtraCodecs.VECTOR3F.fieldOf("facing").forGetter(Rig::facing),
                Codec.STRING.listOf().fieldOf("bottles").forGetter(Rig::bottles),
                Codec.STRING.fieldOf("hose_suit").forGetter(Rig::hoseSuit),
                Codec.STRING.fieldOf("hose_coiled").forGetter(Rig::hoseCoiled)
        ).apply(instance, Rig::new));

        public Rig {
            cell = List.copyOf(cell);
            rack = new Vector3f(rack);
            facing = new Vector3f(facing);
            bottles = List.copyOf(bottles);
        }
    }
}
