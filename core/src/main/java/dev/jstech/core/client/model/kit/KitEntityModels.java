/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.model.kit;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.jstech.core.log.CoreLogs;
import dev.jstech.core.model.IAnimationListener;
import dev.jstech.core.model.ModelDefinition;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * The game's own creatures, and any mod's, drawn with a model of the Core's in place of their own, as a resource pack
 * says: a file in {@code assets/<namespace>/jstech/entity_models/} names the entity, the model, and the animation it
 * plays on and on, whose keyframes read the creature's queries ({@link EntityQueries}) to walk, swim or turn its head.
 * Only creatures, those the game draws as living things, can be drawn so; the pack that is removed takes the model
 * with it.
 *
 * <pre>{@code
 * {
 *   "entity": "minecraft:pig",
 *   "model": "mypack:pig",
 *   "plays": "animation.pig.move"
 * }
 * }</pre>
 */
public final class KitEntityModels extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = CoreLogs.MODELS.logger();
    private static final String FOLDER = "jstech/entity_models";
    private static final String CONTROLLER = "kit_entity_model";
    private static final KitEntityModels INSTANCE = new KitEntityModels();
    private static volatile Map<ResourceLocation, Entry> entries = Map.of();

    private KitEntityModels() {
        super(new Gson(), FOLDER);
    }

    /** Reads the packs' files as they load, and draws the creatures they name, from the mod's constructor. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterClientReloadListenersEvent.class, event -> event.registerReloadListener(INSTANCE));
        NeoForge.EVENT_BUS.addListener(RenderLivingEvent.Pre.class, KitEntityModels::draw);
    }

    /** What the packs say {@code type} is drawn with, if anything. */
    public static Optional<Entry> of(final EntityType<?> type) {
        return Optional.ofNullable(entries.get(BuiltInRegistries.ENTITY_TYPE.getKey(type)));
    }

    @Override
    protected void apply(final Map<ResourceLocation, JsonElement> files, final ResourceManager resources,
                         final ProfilerFiller profiler) {
        final Map<ResourceLocation, Entry> read = new HashMap<>();
        files.keySet().stream().sorted().forEach(file -> {
            try {
                final JsonObject json = GsonHelper.convertToJsonObject(files.get(file), file.toString());
                final Entry entry = new Entry(ResourceLocation.parse(GsonHelper.getAsString(json, "model")),
                        GsonHelper.getAsString(json, "plays", ""));
                read.put(ResourceLocation.parse(GsonHelper.getAsString(json, "entity")), entry);
            } catch (final RuntimeException wrong) {
                LOGGER.warn("The entity model {} could not be read and is left out: {}", file, wrong.getMessage());
            }
        });
        entries = Map.copyOf(read);
    }

    private static void draw(final RenderLivingEvent.Pre<?, ?> event) {
        final LivingEntity entity = event.getEntity();
        final Entry entry = entries.get(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
        final Optional<KitModel> model = entry == null ? Optional.empty() : KitModels.get(entry.model());
        if (model.isEmpty()) {
            // A file naming a model no pack holds leaves the creature as the game draws it.
            return;
        }
        event.setCanceled(true);
        final float partialTick = event.getPartialTick();
        final KitInstance instance = KitAnimations.of(entity, model.get());
        final double seconds = KitClock.seconds(partialTick);
        EntityQueries.hand(instance.animator().context(), entity, partialTick);
        if (!entry.plays().isEmpty() && instance.animator().controller(CONTROLLER).idle(seconds)) {
            instance.animator().play(CONTROLLER, entry.plays(), seconds);
        }
        instance.animate(KitClock.frame(), seconds, false, IAnimationListener.NONE);
        final PoseStack poses = event.getPoseStack();
        poses.pushPose();
        // A model faces north as it is drawn; a creature whose body faces yaw 0 faces south.
        poses.mulPose(Axis.YP.rotationDegrees(180.0F - Mth.rotLerp(partialTick, entity.yBodyRotO,
                entity.yBodyRot)));
        KitRenderer.draw(instance, ModelDefinition.Variant.NONE, entity, poses, event.getMultiBufferSource(),
                event.getPackedLight(), LivingEntityRenderer.getOverlayCoords(entity, 0.0F), null, entity.level(),
                true);
        poses.popPose();
    }

    /**
     * What a pack draws a creature with.
     *
     * @param model the model of the Core's it is drawn with
     * @param plays the animation it plays on and on, or none
     */
    public record Entry(ResourceLocation model, String plays) {

        /** The animation, if it plays one. */
        public @Nullable String playing() {
            return this.plays.isEmpty() ? null : this.plays;
        }
    }
}
