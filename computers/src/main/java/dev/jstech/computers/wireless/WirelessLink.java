/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.wireless;

import dev.jstech.core.uuid.NodeUuid;
import java.util.Objects;
import net.minecraft.core.BlockPos;

/**
 * How a computer with a Wi-Fi card is on its network (computers/docs/design/wireless.md): the access point it joined
 * through, its card's Wi-Fi and the access point's, and so the speed it goes at, the older one's.
 *
 * @param point       where the access point stands
 * @param pointNode   the node the access point is known by on the network
 * @param card        the Wi-Fi of the computer's card
 * @param accessPoint the Wi-Fi of the access point
 */
public record WirelessLink(BlockPos point, NodeUuid pointNode, WirelessStandard card, WirelessStandard accessPoint) {

    public WirelessLink {
        Objects.requireNonNull(point, "point");
        Objects.requireNonNull(pointNode, "pointNode");
        Objects.requireNonNull(card, "card");
        Objects.requireNonNull(accessPoint, "accessPoint");
    }

    /** The Wi-Fi the link goes at: the older of the card's and the access point's. */
    public WirelessStandard speed() {
        return WirelessStandard.older(card, accessPoint);
    }

    /** How many items a tick the link carries at most. */
    public int itemsPerTick() {
        return speed().itemsPerTick();
    }
}
