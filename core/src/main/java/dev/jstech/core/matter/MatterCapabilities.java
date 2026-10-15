/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.matter;

import dev.jstech.core.JsCore;
import dev.jstech.core.material.MatterState;
import java.util.Optional;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.ItemCapability;
import org.jetbrains.annotations.Nullable;

/**
 * The capabilities a block or an item offers its store of each state of matter by, one for each, so a gas line never
 * finds a slurry and a tank of plasma is never asked for gas: gas, supercritical fluid, slurry, plasma and exotic
 * matter. Solids are items and liquids NeoForge's fluids, which have the game's own capabilities.
 */
public final class MatterCapabilities {

    public static final BlockCapability<IMatterHandler, @Nullable Direction> GAS = block("gas");
    public static final BlockCapability<IMatterHandler, @Nullable Direction> SUPERCRITICAL = block("supercritical");
    public static final BlockCapability<IMatterHandler, @Nullable Direction> SLURRY = block("slurry");
    public static final BlockCapability<IMatterHandler, @Nullable Direction> PLASMA = block("plasma");
    public static final BlockCapability<IMatterHandler, @Nullable Direction> EXOTIC = block("exotic");

    public static final ItemCapability<IMatterHandler, @Nullable Void> GAS_ITEM = item("gas");
    public static final ItemCapability<IMatterHandler, @Nullable Void> SUPERCRITICAL_ITEM = item("supercritical");
    public static final ItemCapability<IMatterHandler, @Nullable Void> SLURRY_ITEM = item("slurry");
    public static final ItemCapability<IMatterHandler, @Nullable Void> PLASMA_ITEM = item("plasma");
    public static final ItemCapability<IMatterHandler, @Nullable Void> EXOTIC_ITEM = item("exotic");

    private MatterCapabilities() {
    }

    /** The block capability of {@code state}, or none for a solid or a liquid, which have the game's own. */
    public static Optional<BlockCapability<IMatterHandler, @Nullable Direction>> block(final MatterState state) {
        return Optional.ofNullable(switch (state) {
            case GAS -> GAS;
            case SUPERCRITICAL -> SUPERCRITICAL;
            case SLURRY -> SLURRY;
            case PLASMA -> PLASMA;
            case EXOTIC -> EXOTIC;
            case SOLID, LIQUID -> null;
        });
    }

    /** The item capability of {@code state}, or none for a solid or a liquid. */
    public static Optional<ItemCapability<IMatterHandler, @Nullable Void>> item(final MatterState state) {
        return Optional.ofNullable(switch (state) {
            case GAS -> GAS_ITEM;
            case SUPERCRITICAL -> SUPERCRITICAL_ITEM;
            case SLURRY -> SLURRY_ITEM;
            case PLASMA -> PLASMA_ITEM;
            case EXOTIC -> EXOTIC_ITEM;
            case SOLID, LIQUID -> null;
        });
    }

    private static BlockCapability<IMatterHandler, @Nullable Direction> block(final String path) {
        return BlockCapability.createSided(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, path),
                IMatterHandler.class);
    }

    private static ItemCapability<IMatterHandler, @Nullable Void> item(final String path) {
        return ItemCapability.createVoid(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, path),
                IMatterHandler.class);
    }
}
