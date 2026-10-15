/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multipart;

import dev.jstech.core.capability.CapabilityKind;
import dev.jstech.core.capability.CapabilityKinds;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;

/**
 * A block that holds parts answering, on each face, with what the part there offers: the game's items, fluids and
 * energy, and every capability declared with the Core. The block registers it once, and its parts are asked from then
 * on, so a part never registers anything:
 *
 * <pre>{@code
 * PartCapabilities.register(event, MY_PART_HOST.get(), MyPartHost::parts);
 * }</pre>
 */
public final class PartCapabilities {

    private PartCapabilities() {
    }

    /** Has every block entity of {@code type} answer, on a face, with what its part there offers. */
    public static <B extends BlockEntity> void register(final RegisterCapabilitiesEvent event,
                                                        final BlockEntityType<B> type,
                                                        final Function<B, FaceParts> parts) {
        final List<BlockCapability<?, @Nullable Direction>> capabilities = new ArrayList<>(List.of(
                Capabilities.ItemHandler.BLOCK, Capabilities.FluidHandler.BLOCK, Capabilities.EnergyStorage.BLOCK));
        for (final CapabilityKind<?> kind : CapabilityKinds.all()) {
            capabilities.add(kind.block());
        }
        for (final BlockCapability<?, @Nullable Direction> capability : capabilities) {
            registerOne(event, type, parts, capability);
        }
    }

    private static <T, B extends BlockEntity> void registerOne(
            final RegisterCapabilitiesEvent event, final BlockEntityType<B> type, final Function<B, FaceParts> parts,
            final BlockCapability<T, @Nullable Direction> capability) {
        event.registerBlockEntity(capability, type, (host, face) -> {
            if (face == null) {
                return null;
            }
            final IFacePart part = parts.apply(host).get(face);
            return part == null ? null : part.capability(capability);
        });
    }
}
