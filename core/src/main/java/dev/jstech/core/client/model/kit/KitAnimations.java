/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.model.kit;

import dev.jstech.core.model.AnimationController;
import dev.jstech.core.model.ModelAnimation;
import dev.jstech.core.model.network.ModelAnimationPayload;
import dev.jstech.core.model.network.ModelAnimations;
import dev.jstech.core.model.network.ModelTarget;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The things drawn with the Core's models on the player's game, each with what moves it: block entities, entities and
 * item stacks, an item drawn in each of its places (in a hand, in a slot, on the ground) moving apart, so one does not
 * leak into another. What the server tells a model to play finds it here; told before the model was first drawn, it
 * waits for it.
 */
public final class KitAnimations {

    /** How many item stacks the server animates are kept at once; the longest unseen go first. */
    private static final int ITEMS_KEPT = 256;
    /** How many targets' waiting orders are kept at once. */
    private static final int WAITING_KEPT = 1024;

    private static final Map<BlockEntity, KitInstance> BLOCKS = new WeakHashMap<>();
    private static final Map<BlockEntity, Map<String, KitInstance>> PARTS = new WeakHashMap<>();
    private static final Map<Integer, KitInstance> ENTITIES = new HashMap<>();
    private static final Map<Long, KitInstance> ITEMS = new LinkedHashMap<>(16, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(final Map.Entry<Long, KitInstance> eldest) {
            return size() > ITEMS_KEPT;
        }
    };
    private static final Map<Item, Map<ItemDisplayContext, KitInstance>> ITEM_KINDS = new HashMap<>();
    private static final Map<ModelTarget, Map<String, ModelAnimationPayload>> WAITING = new LinkedHashMap<>(16,
            0.75F, true) {
        @Override
        protected boolean removeEldestEntry(final Map.Entry<ModelTarget, Map<String, ModelAnimationPayload>> eldest) {
            return size() > WAITING_KEPT;
        }
    };
    /* What each target was told of late, for a model it is drawn with anew a moment after. */
    private static final Map<ModelTarget, Map<String, Order>> RECENT = new LinkedHashMap<>(16, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(final Map.Entry<ModelTarget, Map<String, Order>> eldest) {
            return size() > WAITING_KEPT;
        }
    };
    /** How long after it is told an order is carried out again on a model drawn anew, in milliseconds. */
    private static final long RECENT_MILLIS = 2_000;

    private KitAnimations() {
    }

    /** Forgets the entities and the items of a world the player leaves, once, from the client setup. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> forgetAll());
    }

    /** What moves the model of a block entity, made the first time it is drawn with {@code model}. */
    public static KitInstance of(final BlockEntity entity, final KitModel model) {
        final KitInstance kept = BLOCKS.get(entity);
        if (kept != null && kept.model() == model) {
            return kept;
        }
        final KitInstance made = new KitInstance(model);
        BLOCKS.put(entity, made);
        final ModelTarget target = ModelTarget.block(entity.getBlockPos());
        waiting(target, made);
        if (kept != null) {
            // Drawn with another model now, as a structure that has just formed: what was just played goes on.
            recent(target, made);
        }
        return made;
    }

    /**
     * What moves one of the models a block entity draws besides its own, as a part a machine holds is drawn on the seat
     * its case marks for it: one for each part, by its name, made the first time it is drawn with {@code model}. The
     * server plays nothing on a part; its renderer moves it.
     */
    public static KitInstance part(final BlockEntity entity, final String part, final KitModel model) {
        final Map<String, KitInstance> parts = PARTS.computeIfAbsent(entity, holder -> new HashMap<>());
        final KitInstance kept = parts.get(part);
        if (kept != null && kept.model() == model) {
            return kept;
        }
        final KitInstance made = new KitInstance(model);
        parts.put(part, made);
        return made;
    }

    /** What moves the model of an entity. */
    public static KitInstance of(final Entity entity, final KitModel model) {
        final KitInstance kept = ENTITIES.get(entity.getId());
        if (kept != null && kept.model() == model) {
            return kept;
        }
        final KitInstance made = new KitInstance(model);
        ENTITIES.put(entity.getId(), made);
        waiting(ModelTarget.entity(entity.getId()), made);
        return made;
    }

    /**
     * What moves the model of an item stack drawn in {@code context}: the stack's own, for a stack the server
     * animates, drawn alike wherever it is; else one for each kind of item in each place it is drawn.
     */
    public static KitInstance of(final ItemStack stack, final ItemDisplayContext context, final KitModel model) {
        final Long id = stack.get(ModelAnimations.ANIMATED_ID.get());
        if (id != null) {
            final KitInstance kept = ITEMS.get(id);
            if (kept != null && kept.model() == model) {
                return kept;
            }
            final KitInstance made = new KitInstance(model);
            ITEMS.put(id, made);
            waiting(ModelTarget.item(id), made);
            return made;
        }
        final Map<ItemDisplayContext, KitInstance> byPlace = ITEM_KINDS.computeIfAbsent(stack.getItem(),
                item -> new EnumMap<>(ItemDisplayContext.class));
        final KitInstance kept = byPlace.get(context);
        if (kept != null && kept.model() == model) {
            return kept;
        }
        final KitInstance made = new KitInstance(model);
        byPlace.put(context, made);
        return made;
    }

    /** What moves the model of a block entity, if it has been drawn: for the code that asks what it plays. */
    public static Optional<KitInstance> drawn(final BlockEntity entity) {
        return Optional.ofNullable(BLOCKS.get(entity));
    }

    /** What moves the model of an entity, if it has been drawn. */
    public static Optional<KitInstance> drawn(final Entity entity) {
        return Optional.ofNullable(ENTITIES.get(entity.getId()));
    }

    /** Carries out what the server told a model to do; on the render thread. */
    public static void receive(final ModelAnimationPayload payload) {
        final Optional<KitInstance> found = find(payload.target());
        if (found.isPresent()) {
            carryOut(payload, found.get());
        } else {
            WAITING.computeIfAbsent(payload.target(), target -> new LinkedHashMap<>()).put(payload.controller(),
                    payload);
        }
        RECENT.computeIfAbsent(payload.target(), target -> new LinkedHashMap<>()).put(payload.controller(),
                new Order(payload, Util.getMillis()));
    }

    /** Forgets every model's things, whose models have been read again. */
    static void forgetModels() {
        BLOCKS.clear();
        PARTS.clear();
        ENTITIES.clear();
        ITEMS.clear();
        ITEM_KINDS.clear();
    }

    private static void forgetAll() {
        forgetModels();
        WAITING.clear();
        RECENT.clear();
    }

    private static Optional<KitInstance> find(final ModelTarget target) {
        final Minecraft game = Minecraft.getInstance();
        return switch (target.kind()) {
            case BLOCK -> {
                final BlockEntity entity = game.level == null ? null : game.level.getBlockEntity(target.pos());
                yield Optional.ofNullable(entity == null ? null : BLOCKS.get(entity));
            }
            case ENTITY -> Optional.ofNullable(ENTITIES.get(target.entity()));
            case ITEM -> Optional.ofNullable(ITEMS.get(target.item()));
        };
    }

    /* Carries out again on a model newly drawn for a target what the server told it of late. */
    private static void recent(final ModelTarget target, final KitInstance instance) {
        final Map<String, Order> orders = RECENT.get(target);
        if (orders == null) {
            return;
        }
        final long now = Util.getMillis();
        for (final Order order : orders.values()) {
            if (now - order.receivedAt() <= RECENT_MILLIS) {
                carryOut(order.payload(), instance);
            }
        }
    }

    private static void waiting(final ModelTarget target, final KitInstance instance) {
        final Map<String, ModelAnimationPayload> orders = WAITING.remove(target);
        if (orders != null) {
            orders.values().forEach(order -> carryOut(order, instance));
        }
    }

    private static void carryOut(final ModelAnimationPayload payload, final KitInstance instance) {
        final double now = KitClock.now();
        final AnimationController controller = instance.animator().controller(payload.controller());
        switch (payload.action()) {
            case PLAY, REPLAY -> {
                final Optional<ModelAnimation> animation = instance.animator().animation(payload.animation());
                animation.ifPresent(chosen -> {
                    if (payload.action() == ModelAnimationPayload.Action.PLAY) {
                        controller.play(chosen, now, payload.transition());
                    } else {
                        controller.replay(chosen, now, payload.transition());
                    }
                });
            }
            case STOP -> controller.stop(now, payload.transition());
            case SPEED -> controller.speed(payload.speed(), now);
        }
    }

    /* An order from the server, and when it came. */
    private record Order(ModelAnimationPayload payload, long receivedAt) {
    }
}
