/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.portable;

import dev.jstech.computers.config.ComputersClientConfig;
import dev.jstech.computers.operation.payload.portable.PortableViewPayload;
import dev.jstech.computers.portable.PortableAlert;
import dev.jstech.computers.portable.PortableAlerts;
import dev.jstech.computers.portable.PortableItem;
import dev.jstech.computers.portable.Portables;
import dev.jstech.core.client.notification.NotificationsClient;
import dev.jstech.core.text.Text;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The network's alerts on the HUD (computers/docs/design/wireless.md), for a player whose setting wants them there
 * too ({@code notifications_on_hud}): each alert that comes in to a device they carry shows as one of J's Core's
 * notifications at the attention level, naming the device it came from. The alerts already there when the player
 * joins do not show again.
 */
public final class PortableHud {

    private PortableHud() {
    }

    /** What a device keeps came, after {@code before}: the alerts new since then show, when the player wants. */
    static void cameIn(@Nullable final PortableViewPayload before, final PortableViewPayload now) {
        if (before == null || !ComputersClientConfig.notificationsOnHud()) {
            return;
        }
        final long known = before.alerts().isEmpty() ? Long.MIN_VALUE : before.alerts().getFirst().at();
        final Text from = AppTexts.FROM_YOUR.with(deviceName(now.device()));
        for (final PortableAlert alert : now.alerts().reversed()) {
            if (!alert.seen() && alert.at() > known) {
                NotificationsClient.show(PortableAlerts.NOTICE.of(alert.title(), alert.detail()).withOrigin(from));
            }
        }
    }

    /* The name of the device {@code device} the player carries, its item's. */
    private static String deviceName(final UUID device) {
        final Minecraft game = Minecraft.getInstance();
        if (game.player != null) {
            final Inventory inventory = game.player.getInventory();
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                final ItemStack stack = inventory.getItem(slot);
                if (stack.getItem() instanceof PortableItem item && device.equals(Portables.id(stack))) {
                    return MobileParts.deviceName(item.model());
                }
            }
        }
        return "";
    }
}
