/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.portable;

import dev.jstech.computers.blockentity.ServerServices;
import dev.jstech.computers.operation.payload.social.SocialPayloads;
import dev.jstech.computers.program.Programs;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * A portable's Messenger on the server (computers/docs/design/wireless.md): the same service a computer's Messenger
 * speaks to, run by a server of the network the device is on, its rooms and its people the same.
 */
public final class PortableMessenger {

    private PortableMessenger() {
    }

    /**
     * What {@code player} did in the Messenger of the device {@code device}: looked at {@code room}, said
     * {@code text} in it, or left; answered by the service, or told there is none on its network.
     */
    public static void talk(final ServerPlayer player, final ServerLevel level, final UUID device, final int action,
                            final String room, final String text) {
        final PortableLinks.Joined joined = PortableLinks.of(level, device);
        SocialPayloads.talk(player, level, joined == null ? null
                : ServerServices.find(level, joined.network(), Programs.MESSENGER_SERVICE), action, room, text);
    }
}
