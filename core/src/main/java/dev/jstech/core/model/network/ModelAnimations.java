/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model.network;

import com.mojang.serialization.Codec;
import dev.jstech.core.JsCore;
import dev.jstech.core.client.model.kit.KitAnimations;
import dev.jstech.core.client.model.kit.KitTools;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Animations the server plays on the models of block entities, entities and items, seen alike by every player who
 * sees them: a door that opens when a machine starts, a press that strikes as it crafts. The server says which
 * controller plays what; each client that draws the model blends to it as its own code would. A player who comes into
 * sight later sees what the code plays from then on.
 *
 * <pre>{@code
 * ModelAnimations.play(this, "door", "open");          // in a block entity, on the server
 * ModelAnimations.play(entity, "arms", "wave");
 * ModelAnimations.play(holder, stack, "main", "fire"); // an item, as the entity holding it shows it
 * }</pre>
 */
public final class ModelAnimations {

    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, JsCore.MODID);

    /** The number an item stack is given the first time the server animates it, so its players find its model. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> ANIMATED_ID =
            COMPONENTS.registerComponentType("animated_id", builder -> builder.persistent(Codec.LONG)
                    .networkSynchronized(ByteBufCodecs.VAR_LONG));

    private static final String VERSION = "1";
    private static final float DEFAULT_TRANSITION = 0.2F;

    private ModelAnimations() {
    }

    /** Registers the payload and the item's number, once, from the mod's constructor. */
    public static void listen(final IEventBus modBus) {
        COMPONENTS.register(modBus);
        modBus.addListener(RegisterPayloadHandlersEvent.class, ModelAnimations::onRegisterPayloads);
    }

    /** Plays {@code animation} on the controller {@code controller} of the block entity's model, blending to it. */
    public static void play(final BlockEntity entity, final String controller, final String animation) {
        send(entity, new ModelAnimationPayload(ModelTarget.block(entity.getBlockPos()), controller,
                ModelAnimationPayload.Action.PLAY, animation, DEFAULT_TRANSITION, 1));
    }

    /** Plays {@code animation} from its start, whether it plays already or not. */
    public static void replay(final BlockEntity entity, final String controller, final String animation) {
        send(entity, new ModelAnimationPayload(ModelTarget.block(entity.getBlockPos()), controller,
                ModelAnimationPayload.Action.REPLAY, animation, DEFAULT_TRANSITION, 1));
    }

    /** Stops the controller {@code controller}, blending back to rest. */
    public static void stop(final BlockEntity entity, final String controller) {
        send(entity, new ModelAnimationPayload(ModelTarget.block(entity.getBlockPos()), controller,
                ModelAnimationPayload.Action.STOP, "", DEFAULT_TRANSITION, 1));
    }

    /** Plays the controller {@code controller} at {@code speed}, 1 as its animations are written. */
    public static void speed(final BlockEntity entity, final String controller, final float speed) {
        send(entity, new ModelAnimationPayload(ModelTarget.block(entity.getBlockPos()), controller,
                ModelAnimationPayload.Action.SPEED, "", 0, speed));
    }

    /** Plays {@code animation} on an entity's model. */
    public static void play(final Entity entity, final String controller, final String animation) {
        send(entity, new ModelAnimationPayload(ModelTarget.entity(entity.getId()), controller,
                ModelAnimationPayload.Action.PLAY, animation, DEFAULT_TRANSITION, 1));
    }

    /** Stops a controller of an entity's model. */
    public static void stop(final Entity entity, final String controller) {
        send(entity, new ModelAnimationPayload(ModelTarget.entity(entity.getId()), controller,
                ModelAnimationPayload.Action.STOP, "", DEFAULT_TRANSITION, 1));
    }

    /**
     * Plays {@code animation} on the model of an item stack, as the players who see {@code holder} see it; the stack
     * is given its number the first time.
     */
    public static void play(final Entity holder, final ItemStack stack, final String controller,
                            final String animation) {
        send(holder, new ModelAnimationPayload(ModelTarget.item(idOf(stack)), controller,
                ModelAnimationPayload.Action.PLAY, animation, DEFAULT_TRANSITION, 1));
    }

    /** The number of an item stack the server animates, given it now if it has none. */
    public static long idOf(final ItemStack stack) {
        final Long given = stack.get(ANIMATED_ID.get());
        if (given != null) {
            return given;
        }
        final long id = ThreadLocalRandom.current().nextLong() & Long.MAX_VALUE;
        stack.set(ANIMATED_ID.get(), id);
        return id;
    }

    private static void send(final BlockEntity entity, final ModelAnimationPayload payload) {
        if (entity.getLevel() instanceof ServerLevel level) {
            PacketDistributor.sendToPlayersTrackingChunk(level, new ChunkPos(entity.getBlockPos()), payload);
        }
    }

    private static void send(final Entity entity, final ModelAnimationPayload payload) {
        if (entity.level() instanceof ServerLevel) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, payload);
        }
    }

    private static void onRegisterPayloads(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(VERSION);
        registrar.playToClient(ModelAnimationPayload.TYPE, ModelAnimationPayload.STREAM_CODEC,
                ModelAnimations::onAnimation);
        registrar.playToClient(ModelToolPayload.TYPE, ModelToolPayload.STREAM_CODEC, ModelAnimations::onTool);
    }

    /* Runs on a client only: the payload is only ever sent to one. */
    private static void onAnimation(final ModelAnimationPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> KitAnimations.receive(payload));
    }

    /* Runs on a client only, as the one above. */
    private static void onTool(final ModelToolPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> KitTools.receive(payload));
    }
}
