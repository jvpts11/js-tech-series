/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.crc;

import dev.jstech.core.call.MenuCall;
import dev.jstech.core.call.MenuCalls;
import dev.jstech.core.machine.MachineMenu;
import dev.jstech.core.research.Discoveries;
import dev.jstech.industrial.JsIndustrial;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

/**
 * What a Computational Research Centre's Research tab asks of the server: to research a discovery now, for the whole
 * group, or to put it in the group's queue. Each is made only on a CRC's menu, by a player who may set it up, and only
 * for a discovery that is there to research.
 */
public final class CrcCalls {

    /** Researches the discovery now, for the whole group. */
    public static final MenuCall<MachineMenu, ResourceLocation> CHOOSE = call("crc_choose", CrcBlockEntity::choose);
    /** Puts the discovery at the end of the group's queue. */
    public static final MenuCall<MachineMenu, ResourceLocation> ENQUEUE = call("crc_enqueue", CrcBlockEntity::enqueue);

    private CrcCalls() {
    }

    /** Declares them, from the mod's constructor. */
    public static void declare() {
        // Loading the class declares them.
    }

    private static MenuCall<MachineMenu, ResourceLocation> call(final String id, final Change change) {
        return MenuCalls.declare(ResourceLocation.fromNamespaceAndPath(JsIndustrial.MODID, id), MachineMenu.class,
                ResourceLocation.STREAM_CODEC, (menu, player, discovery) -> {
                    if (menu.machine() instanceof CrcBlockEntity crc && crc.maySetUp(player)
                            && crc.getLevel() instanceof ServerLevel level
                            && Discoveries.byId(discovery).filter(found -> Discoveries.exists(level, found))
                            .isPresent()) {
                        change.apply(crc, level, discovery);
                    }
                });
    }

    /* What a call changes of the CRC. */
    @FunctionalInterface
    private interface Change {

        void apply(CrcBlockEntity crc, ServerLevel level, ResourceLocation discovery);
    }
}
