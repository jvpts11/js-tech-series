/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;

/**
 * The ports of a multiblock, made reachable: the items, fluid and energy capabilities of a part block entity pass to
 * its controller when the part is a port of the matching kind, and are absent otherwise. A mod registers its part
 * block entity once, and stamps the ports a match found with {@link #stamp}.
 */
public final class MultiblockPorts {

    private MultiblockPorts() {
    }

    /** Registers the three capabilities of {@code partType}, each passed to the controller through its ports. */
    public static void register(final RegisterCapabilitiesEvent event,
                                final BlockEntityType<? extends MultiblockPartBlockEntity> partType) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, partType, (part, side) -> {
            final IMultiblockPortHost host = host(part, PortKind.Resource.ITEMS);
            return host == null ? null : host.itemPort(part.port(), part.getBlockPos(), side);
        });
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, partType, (part, side) -> {
            final IMultiblockPortHost host = host(part, PortKind.Resource.FLUID);
            return host == null ? null : host.fluidPort(part.port(), part.getBlockPos(), side);
        });
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, partType, (part, side) -> {
            final IMultiblockPortHost host = host(part, PortKind.Resource.ENERGY);
            return host == null ? null : host.energyPort(part.port(), part.getBlockPos(), side);
        });
    }

    /**
     * Marks each part of a formed structure with the port its match found there, or as a plain part, so the parts'
     * capabilities follow the pattern.
     */
    public static void stamp(final Level level, final IMatchResult.Success match) {
        final Map<Long, PortKind> ports = match.ports();
        for (final long encoded : match.slavePositions()) {
            final BlockPos at = MultiblockPatterns.decode(encoded);
            if (level.getBlockEntity(at) instanceof MultiblockPartBlockEntity part) {
                part.setPort(ports.get(encoded));
            }
        }
        // A pipe that asked a part before keeps what it got until told the part changed.
        for (final long encoded : match.slavePositions()) {
            level.invalidateCapabilities(MultiblockPatterns.decode(encoded));
        }
    }

    /*
     * The controller a port of that resource passes to, or null for a plain part, a port of another resource, or a
     * part whose controller is not loaded.
     */
    @Nullable
    private static IMultiblockPortHost host(final MultiblockPartBlockEntity part, final PortKind.Resource resource) {
        final PortKind kind = part.port();
        final BlockPos controller = part.controllerPos();
        if (kind == null || kind.resource() != resource || controller == null || part.getLevel() == null
                || !part.getLevel().isLoaded(controller)) {
            return null;
        }
        return part.getLevel().getBlockEntity(controller) instanceof IMultiblockPortHost host ? host : null;
    }
}
