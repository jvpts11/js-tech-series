/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.capability;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;

/**
 * A capability of a mod's own, for blocks, items and entities at once, declared in one line as a constant:
 *
 * <pre>{@code
 * public static final CapabilityKind<IHeatHandler> HEAT = CapabilityKinds.declare(rl("heat"), IHeatHandler.class);
 * }</pre>
 *
 * <p>A block or an entity answers it by the face it is asked on, an item as a whole. Whatever offers it says so
 * while the capabilities are registered, and whatever looks for it asks here:
 *
 * <pre>{@code
 * HEAT.onBlockEntity(event, MY_FURNACE.get(), (furnace, face) -> furnace.heat(face));
 * IHeatHandler next = HEAT.at(level, pos.relative(face), face.getOpposite());
 * }</pre>
 *
 * <p>Something that asks the same block every tick keeps the answer in a {@link CapabilityCache} instead, which the
 * game tells when it goes stale.
 *
 * @param <T> what the capability hands over
 */
public final class CapabilityKind<T> {

    private final ResourceLocation id;
    private final Class<T> type;
    private final BlockCapability<T, @Nullable Direction> block;
    private final ItemCapability<T, @Nullable Void> item;
    private final EntityCapability<T, @Nullable Direction> entity;

    CapabilityKind(final ResourceLocation id, final Class<T> type) {
        this.id = id;
        this.type = type;
        this.block = BlockCapability.createSided(id, type);
        this.item = ItemCapability.createVoid(id, type);
        this.entity = EntityCapability.createSided(id, type);
    }

    /** The capability's id, the same for its block, item and entity forms. */
    public ResourceLocation id() {
        return id;
    }

    /** What the capability hands over. */
    public Class<T> type() {
        return type;
    }

    /** The game's capability of blocks, asked by face. */
    public BlockCapability<T, @Nullable Direction> block() {
        return block;
    }

    /** The game's capability of items. */
    public ItemCapability<T, @Nullable Void> item() {
        return item;
    }

    /** The game's capability of entities, asked by face. */
    public EntityCapability<T, @Nullable Direction> entity() {
        return entity;
    }

    /** Has every block entity of {@code type} answer it, by face, through {@code provider}. */
    public <B extends BlockEntity> void onBlockEntity(
            final RegisterCapabilitiesEvent event, final BlockEntityType<B> type,
            final ICapabilityProvider<? super B, @Nullable Direction, T> provider) {
        event.registerBlockEntity(block, type, provider);
    }

    /** Has {@code items} answer it through {@code provider}. */
    public void onItem(final RegisterCapabilitiesEvent event,
                       final ICapabilityProvider<ItemStack, @Nullable Void, T> provider, final ItemLike... items) {
        event.registerItem(item, provider, items);
    }

    /** Has every entity of {@code type} answer it, by face, through {@code provider}. */
    public <E extends Entity> void onEntity(final RegisterCapabilitiesEvent event, final EntityType<E> type,
                                            final ICapabilityProvider<? super E, @Nullable Direction, T> provider) {
        event.registerEntity(entity, type, provider);
    }

    /** What the block at {@code pos} hands over on {@code face}, or null when it offers nothing there. */
    public @Nullable T at(final Level level, final BlockPos pos, @Nullable final Direction face) {
        return level.getCapability(block, pos, face);
    }

    /** What {@code stack} hands over, or null when it offers nothing. */
    public @Nullable T of(final ItemStack stack) {
        return stack.getCapability(item);
    }

    /** What {@code target} hands over on {@code face}, or null when it offers nothing there. */
    public @Nullable T of(final Entity target, @Nullable final Direction face) {
        return target.getCapability(entity, face);
    }

    /**
     * A lookup of the block at {@code pos} on {@code face}, kept for {@code owner} for as long as it stands, and
     * worked out again only when the game says the answer there has changed.
     */
    public CapabilityCache<T> cache(final ServerLevel level, final BlockPos pos, @Nullable final Direction face,
                                    final BlockEntity owner) {
        return CapabilityCache.of(block, level, pos, face, owner);
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
