/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.mechanical;

import dev.jstech.core.mechanical.MechanicalPartBlockEntity;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * The parts of mechanical power drawn turning on the player's game, each by the model of the Core's its mod names:
 * the whole model turning about its axle, or only the bones named, the rest standing still. A model of a turning part
 * is made standing up, its axle along Y through the middle of its block; it is laid along the part's axis as it is
 * placed. Belts and chains between pulleys are drawn running, whatever draws the pulleys.
 *
 * <pre>{@code
 * TurningParts.draw(MyParts.SHAFT, rl("shaft"));                // from the client setup
 * TurningParts.draw(MyParts.GEARBOX, rl("gearbox"), "input", "output");
 * TurningParts.register(event, MyParts.PART_ENTITY.get());       // in EntityRenderersEvent.RegisterRenderers
 * }</pre>
 * The part's block is made by {@code MechanicalPartBlock.turning(...)}, so its own model is not drawn under the turning
 * one and it hides nothing beside it.
 */
public final class TurningParts {

    /** What a belt's runs are drawn with: a sprite of the block atlas. */
    public static final ResourceLocation BELT = ResourceLocation.withDefaultNamespace("block/black_wool");
    /** What a chain's runs are drawn with. */
    public static final ResourceLocation CHAIN = ResourceLocation.withDefaultNamespace("block/chain");

    /* As declared, before the game makes the blocks; then by block, once looked up. The render thread reads them. */
    private static final Map<Supplier<? extends Block>, Look> DECLARED = new LinkedHashMap<>();
    private static final Map<Block, Look> LOOKS = new HashMap<>();

    private TurningParts() {
    }

    /** Draws {@code block}'s part turning, the whole of the model {@code model} turning about its axle. */
    public static synchronized void draw(final Supplier<? extends Block> block, final ResourceLocation model) {
        DECLARED.put(Objects.requireNonNull(block, "block"), new Look(model, List.of()));
    }

    /** Draws {@code block}'s part with the model {@code model}, only its bones {@code bones} turning. */
    public static synchronized void draw(final Supplier<? extends Block> block, final ResourceLocation model,
                                         final String... bones) {
        DECLARED.put(Objects.requireNonNull(block, "block"), new Look(model, List.of(bones)));
    }

    /** Registers the renderer of the parts of a block entity type. */
    public static <T extends MechanicalPartBlockEntity> void register(
            final EntityRenderersEvent.RegisterRenderers event, final BlockEntityType<T> type) {
        final BlockEntityRendererProvider<T> provider = context -> new MechanicalPartRenderer<>();
        event.registerBlockEntityRenderer(type, provider);
    }

    /** How {@code block}'s part is drawn turning, if its mod said. */
    static synchronized Optional<Look> of(final Block block) {
        if (!DECLARED.isEmpty()) {
            // The blocks are only made once the game registers them: they are looked up the first time one is drawn.
            DECLARED.forEach((declared, look) -> LOOKS.put(declared.get(), look));
            DECLARED.clear();
        }
        return Optional.ofNullable(LOOKS.get(block));
    }

    /**
     * How a part is drawn turning.
     *
     * @param model the model of the Core's it is drawn with
     * @param bones the bones that turn; none for the whole model
     */
    record Look(ResourceLocation model, List<String> bones) {

        Look {
            Objects.requireNonNull(model, "model");
            bones = List.copyOf(bones);
        }
    }
}
