/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.machine;

import dev.jstech.core.machine.MachineBlockEntity;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * The faces machines show on their screen's Main tab: the one a mod gave each kind of machine of its own, or the
 * standard face for every other.
 *
 * <pre>{@code
 * MachineFaces.register(MY_CRUSHER.get(), CrusherFace::new);   // from the mod's client setup
 * }</pre>
 */
public final class MachineFaces {

    private static final Map<BlockEntityType<?>, Function<MachineBlockEntity, IMachineFace>> FACES =
            new ConcurrentHashMap<>();

    private MachineFaces() {
    }

    /** Gives the machines of {@code type} the face {@code face} makes for each, in place of the standard one. */
    public static void register(final BlockEntityType<?> type, final Function<MachineBlockEntity, IMachineFace> face) {
        FACES.put(Objects.requireNonNull(type, "type"), Objects.requireNonNull(face, "face"));
    }

    /** The face {@code machine} shows on its Main tab. */
    public static IMachineFace of(final MachineBlockEntity machine) {
        final Function<MachineBlockEntity, IMachineFace> own = FACES.get(machine.getType());
        return own != null ? own.apply(machine) : new StandardFace();
    }
}
