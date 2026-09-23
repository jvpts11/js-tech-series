/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.advancement;

import com.mojang.serialization.Codec;
import dev.jstech.computers.JsComputers;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Where each of a player's programs was first run, so running one on a second computer can be told apart from
 * running it again on the first. Kept on the player, by program name, for as many programs as a player plausibly
 * carries between machines; past that the oldest are simply not remembered.
 */
public final class ProgramTravels {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, JsComputers.MODID);

    private static final int REMEMBERED = 64;

    public static final Supplier<AttachmentType<Map<String, Long>>> FIRST_RUN = ATTACHMENT_TYPES.register(
            "program_first_run", () -> AttachmentType.<Map<String, Long>>builder(() -> new HashMap<>())
                    .serialize(Codec.unboundedMap(Codec.STRING, Codec.LONG).xmap(HashMap::new, map -> map),
                            map -> !map.isEmpty())
                    .copyOnDeath()
                    .build());

    private ProgramTravels() {
    }

    public static void register(final IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }

    /** Notes that {@code player} ran the program called {@code name} on the machine at {@code machine}. */
    public static void ran(final ServerPlayer player, final String name, final long machine) {
        final Map<String, Long> first = player.getData(FIRST_RUN);
        final Long where = first.get(name);
        if (where == null) {
            if (first.size() < REMEMBERED) {
                first.put(name, machine);
                player.setData(FIRST_RUN, first);
            }
        } else if (where != machine) {
            JscEvents.award(player, JscEvents.SIGMA_TWO_MACHINES);
        }
    }
}
