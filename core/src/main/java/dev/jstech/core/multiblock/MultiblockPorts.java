/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

import dev.jstech.core.JsCore;
import dev.jstech.core.energy.EnergyPorts;
import dev.jstech.core.heat.HeatPorts;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.matter.MatterCapabilities;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;

/**
 * The ports of a multiblock, made reachable: the items, fluid, energy, other matter and heat capabilities of a part
 * block entity pass to its terminal when the part is a port of the matching kind, and are absent otherwise. A mod
 * registers its part block entity once, and stamps the ports a match found with {@link #stamp}.
 */
public final class MultiblockPorts {

    /**
     * The blocks that are ports of their own, by the id a {@link BlockMatch} names it by: a shape whose shell lets
     * ports stand anywhere puts {@code BlockMatch.tag(PORTS_TAG)} among what its shell's places take.
     */
    public static final String PORTS_TAG = "jscore:multiblock_ports";
    /** The same tag, for a mod's data generation to put its port blocks in. */
    public static final TagKey<Block> PORTS = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "multiblock_ports"));

    private MultiblockPorts() {
    }

    /**
     * Registers the capabilities of {@code partType}, items, fluid, energy and each other state of matter, each passed
     * to the terminal through its ports.
     */
    public static void register(final RegisterCapabilitiesEvent event,
                                final BlockEntityType<? extends MultiblockPartBlockEntity> partType) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, partType, (part, side) -> {
            final IMultiblockPortHost host = host(part, PortKind.Resource.ITEMS, null);
            return host == null ? null : host.itemPort(part.port(), part.getBlockPos(), side);
        });
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, partType, (part, side) -> {
            final IMultiblockPortHost host = host(part, PortKind.Resource.FLUID, null);
            return host == null ? null : host.fluidPort(part.port(), part.getBlockPos(), side);
        });
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, partType, (part, side) -> {
            final IMultiblockPortHost host = host(part, PortKind.Resource.ENERGY, null);
            return host == null ? null : host.energyPort(part.port(), part.getBlockPos(), side);
        });
        for (final MatterState state : MatterState.values()) {
            MatterCapabilities.block(state).ifPresent(capability ->
                    event.registerBlockEntity(capability, partType, (part, side) -> {
                        final IMultiblockPortHost host = host(part, PortKind.Resource.MATTER, state);
                        return host == null ? null : host.matterPort(part.port(), part.getBlockPos(), side);
                    }));
        }
        event.registerBlockEntity(EnergyPorts.PORT, partType, (part, side) -> {
            final IMultiblockPortHost host = host(part, PortKind.Resource.ENERGY, null);
            return host == null ? null : host.energyClassPort(part.port(), part.getBlockPos(), side);
        });
        event.registerBlockEntity(HeatPorts.HEAT.block(), partType, (part, side) -> {
            final IMultiblockPortHost host = host(part, PortKind.Resource.HEAT, null);
            return host == null ? null : host.heatPort(part.getBlockPos(), side);
        });
    }

    /**
     * Registers {@code capability} on {@code partType}, a port of a mod's own that is bound to a terminal passing it
     * to the terminal's {@link IMultiblockPortHost#otherPort}: a line of its own reaching the machine, as compressed
     * air does.
     */
    public static <T> void registerOther(final RegisterCapabilitiesEvent event,
                                         final BlockCapability<T, @Nullable Direction> capability,
                                         final BlockEntityType<? extends MultiblockPartBlockEntity> partType) {
        event.registerBlockEntity(capability, partType, (part, side) -> {
            final BlockPos terminal = part.terminalPos();
            if (terminal == null || part.getLevel() == null || !part.getLevel().isLoaded(terminal)) {
                return null;
            }
            return part.getLevel().getBlockEntity(terminal) instanceof IMultiblockPortHost host
                    ? host.otherPort(capability, part.getBlockPos(), side) : null;
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

    /**
     * Binds each part of a formed structure that is a part block entity to the terminal at {@code terminal}, with
     * the port its pattern gives the part, or as a plain part.
     */
    public static void stamp(final Level level, final BlockPos terminal, final ShapeMatch.Formed formed) {
        for (final long encoded : formed.parts()) {
            final BlockPos at = MultiblockPatterns.decode(encoded);
            if (level.getBlockEntity(at) instanceof MultiblockPartBlockEntity part) {
                part.setTerminal(terminal);
                part.setPort(formed.ports().get(encoded));
            }
            level.invalidateCapabilities(at);
        }
    }

    /** Lets every part block entity among {@code places} go of its terminal and its port. */
    public static void release(final Level level, final Iterable<Long> places) {
        for (final long encoded : places) {
            final BlockPos at = MultiblockPatterns.decode(encoded);
            if (level.isLoaded(at) && level.getBlockEntity(at) instanceof MultiblockPartBlockEntity part) {
                part.release();
                level.invalidateCapabilities(at);
            }
        }
    }

    /*
     * The terminal a port of that resource (and, for matter, that state) passes to, or null for a plain part, a port
     * of another resource or state, or a part whose terminal is not loaded.
     */
    @Nullable
    private static IMultiblockPortHost host(final MultiblockPartBlockEntity part, final PortKind.Resource resource,
                                            @Nullable final MatterState state) {
        final PortKind kind = part.port();
        final BlockPos terminal = part.terminalPos();
        if (kind == null || kind.resource() != resource || kind.state().orElse(null) != state || terminal == null
                || part.getLevel() == null || !part.getLevel().isLoaded(terminal)) {
            return null;
        }
        return part.getLevel().getBlockEntity(terminal) instanceof IMultiblockPortHost host ? host : null;
    }
}
