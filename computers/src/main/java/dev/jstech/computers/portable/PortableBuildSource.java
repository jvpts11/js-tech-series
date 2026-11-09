/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.portable;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.text.Text;
import dev.jstech.core.tool.IBuildSource;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.List;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The network's storage as a place to build from (computers/docs/design/wireless.md): J's Core's building tools and
 * its Multiblock Builder take blocks from the network of the access point that reaches the player, while the player
 * carries a portable that is on and joined to it. A block taken comes out of the network's storage, and one given
 * back goes into it, or to the player when the network has no room.
 */
public final class PortableBuildSource implements IBuildSource {

    /** Its id among the places to build from. */
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "network");

    @Override
    public Text name() {
        return PortableTexts.BUILD_SOURCE.text();
    }

    @Override
    public boolean usable(final Player player) {
        if (player instanceof ServerPlayer server) {
            return networkOf(server) != null;
        }
        // The player's game knows a device is joined by the link its server gives it.
        for (final List<ItemStack> part : carried(player.getInventory())) {
            for (final ItemStack stack : part) {
                if (stack.getItem() instanceof PortableItem && Portables.isOn(stack)
                        && stack.has(ComputingComponents.PORTABLE_LINK.get())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public long count(final Player player, final Item item) {
        // Only the server counts it: J's Core tells the builder's display what it holds.
        if (!(player instanceof ServerPlayer server)) {
            return 0L;
        }
        final NetworkUuid network = networkOf(server);
        return network == null ? 0L : NetworkStorage.of(server.serverLevel(), network).count(StorageKey.of(item));
    }

    @Override
    public ItemStack take(final ServerPlayer player, final Item item) {
        final NetworkUuid network = networkOf(player);
        if (network == null) {
            return ItemStack.EMPTY;
        }
        // The network hands over a single block, which the player then places.
        final long taken = NetworkStorage.of(player.serverLevel(), network).select(StorageKey.of(item), 1L,
                (key, amount, simulate) -> Math.min(amount, 1L));
        return taken > 0L ? new ItemStack(item) : ItemStack.EMPTY;
    }

    @Override
    public void giveBack(final ServerPlayer player, final ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        final NetworkUuid network = networkOf(player);
        final long stored = network == null ? 0L
                : NetworkStorage.of(player.serverLevel(), network).insert(StorageKey.of(stack), stack.getCount());
        final ItemStack left = stack.copyWithCount(stack.getCount() - (int) stored);
        if (!left.isEmpty() && !player.getInventory().add(left)) {
            player.drop(left, false);
        }
    }

    /* The network a portable {@code player} carries is joined to now, the first found; null when none is. */
    @Nullable
    private static NetworkUuid networkOf(final ServerPlayer player) {
        for (final List<ItemStack> part : carried(player.getInventory())) {
            for (final ItemStack stack : part) {
                final UUID device = stack.getItem() instanceof PortableItem && Portables.isOn(stack)
                        ? Portables.id(stack) : null;
                final PortableLinks.Joined joined = device == null ? null
                        : PortableLinks.of(player.serverLevel(), device);
                if (joined != null) {
                    return joined.network();
                }
            }
        }
        return null;
    }

    /* Where a carried device can be: the inventory's rows, the hotbar's among them, and the off hand. */
    private static List<List<ItemStack>> carried(final Inventory inventory) {
        return List.of(inventory.items, inventory.offhand);
    }
}
