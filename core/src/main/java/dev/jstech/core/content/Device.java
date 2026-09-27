/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.content;

import dev.jstech.core.util.BlockEntityTickers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * What a block that makes a block entity does, said once: which block entity it makes, what that block entity does
 * each server tick, and which menu it opens when a player uses it. A {@link DeviceBlock} made from it does all of that,
 * faces whoever placed it and spills the block entity's inventories when it is broken, so none of it is written again
 * in a block class.
 *
 * @param <E> the block entity the block makes
 */
public final class Device<E extends BlockEntity> {

    private final Supplier<? extends BlockEntityType<E>> type;
    private @Nullable BlockEntityTicker<E> serverTicker;
    private @Nullable IMenuFactory<E> menu;

    private Device(final Supplier<? extends BlockEntityType<E>> type) {
        this.type = type;
    }

    /** A device making block entities of that type. */
    public static <E extends BlockEntity> Device<E> of(final Supplier<? extends BlockEntityType<E>> type) {
        return new Device<>(type);
    }

    /** Runs {@code ticker} on the block entity every server tick. */
    public Device<E> ticks(final BlockEntityTicker<E> ticker) {
        this.serverTicker = ticker;
        return this;
    }

    /** Opens the menu {@code factory} makes when a player uses the block with an empty hand. */
    public Device<E> opensMenu(final IMenuFactory<E> factory) {
        this.menu = factory;
        return this;
    }

    /** The block, made with these properties. */
    public DeviceBlock block(final BlockBehaviour.Properties properties) {
        return new DeviceBlock(properties, this);
    }

    BlockEntity create(final BlockPos pos, final BlockState state) {
        return type.get().create(pos, state);
    }

    @Nullable
    <T extends BlockEntity> BlockEntityTicker<T> serverTicker(final BlockEntityType<T> given) {
        return serverTicker == null ? null : BlockEntityTickers.create(given, type.get(), serverTicker);
    }

    boolean opensMenu() {
        return menu != null;
    }

    /** Opens the menu on the block entity at {@code pos}, telling the client where it is. */
    void openMenu(final ServerPlayer player, @Nullable final BlockEntity at, final Component title,
                  final BlockPos pos) {
        final IMenuFactory<E> factory = menu;
        if (factory == null || at == null || at.getType() != type.get()) {
            return;
        }
        @SuppressWarnings("unchecked") final E blockEntity = (E) at;
        player.openMenu(new SimpleMenuProvider((id, inventory, opener) -> factory.create(id, inventory, blockEntity),
                title), buf -> buf.writeBlockPos(pos));
    }

    /**
     * Makes the menu a device opens, on the server, for the block entity it was opened on.
     *
     * @param <E> the block entity
     */
    @FunctionalInterface
    public interface IMenuFactory<E extends BlockEntity> {
        AbstractContainerMenu create(int containerId, Inventory inventory, E blockEntity);
    }
}
