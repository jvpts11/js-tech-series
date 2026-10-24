/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.item;

import dev.jstech.core.content.GeoLook;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * The item of a machine drawn by its block entity (a rack, a Mainframe, a drive, a Pattern Encoder) which shows the
 * machine itself, as it comes: empty and switched off.
 *
 * <p>A cabinet's block model is a model drawn by the block entity, so the block itself renders
 * nothing, and its item, having no block model to fall back on, showed a flat icon that looked like a
 * different mod's placeholder next to the machine it places. This item hands the inventory the same
 * model the world uses, shrunk to fit the slot; the client setup gives every cabinet's item its renderer.
 */
public class CabinetBlockItem extends BlockItem {

    /**
     * How a cabinet is fitted into an item slot: its longest side in model units (16 to a block), which
     * decides how far it shrinks, and the offset in blocks that brings the cabinet's own middle onto the
     * item's middle. A cabinet is modelled around its origin block, which is rarely its centre.
     */
    public record Fit(float span, float offsetX, float offsetY, float offsetZ) {
    }

    /** The family the cabinet is drawn as in the world: its models, their atlases and the animation they share. */
    private final GeoLook<?> look;
    /** The model this cabinet is drawn with, e.g. {@code vintage_server_rack}. */
    private final String model;
    private final Fit fit;
    /**
     * How the names of the parts the world shows only when they are fitted or lit begin (a CPU, a server in a row, a
     * lamp). The slot shows the machine as it comes, empty and switched off, so those parts are hidden there and every
     * other part is shown.
     */
    private final List<String> fittedParts;

    public CabinetBlockItem(final Block block, final Properties properties, final GeoLook<?> look,
                            final String model, final Fit fit, final List<String> fittedParts) {
        super(block, properties);
        this.look = look;
        this.model = model;
        this.fit = fit;
        this.fittedParts = List.copyOf(fittedParts);
    }

    public ResourceLocation modelResource() {
        return look.modelFile(model);
    }

    public ResourceLocation textureResource() {
        return look.textureFile(model);
    }

    public ResourceLocation animationResource() {
        return look.animationFile();
    }

    public Fit fit() {
        return fit;
    }

    /** Whether the slot shows this part of the model: every part but the fitted and the lit ones. */
    public boolean shownInSlot(final String part) {
        for (final String fitted : fittedParts) {
            if (part.startsWith(fitted)) {
                return false;
            }
        }
        return true;
    }
}
