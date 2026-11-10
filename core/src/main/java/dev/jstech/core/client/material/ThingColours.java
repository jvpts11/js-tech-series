/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.material;

import com.mojang.blaze3d.platform.NativeImage;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialLiquids;
import dev.jstech.core.material.Materials;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

/**
 * The colour a thing reads as at a glance, as {@code 0xRRGGBB}, for a machine that paints what it works in its own
 * colour: a material's colour when the thing is a material's (an ingot, a dust, an ore), otherwise the average of the
 * opaque texels of its picture (glass, a log, bone meal). A liquid's is its material's, or its tint, or its picture's.
 * Each is worked out once and kept; on the player's game only, where the pictures are.
 */
public final class ThingColours {

    /** What a thing with no picture to read reads as: a mid grey. */
    public static final int GREY = 0x808080;
    private static final Map<Item, Integer> ITEMS = new ConcurrentHashMap<>();
    private static final Map<Fluid, Integer> FLUIDS = new ConcurrentHashMap<>();
    private static final int WHITE = 0xFFFFFF;
    private static final int OPAQUE = 128;
    private static final int CHANNEL = 0xFF;

    private ThingColours() {
    }

    /** The colour {@code stack} reads as; {@link #GREY} for an empty one. */
    public static int of(final ItemStack stack) {
        if (stack.isEmpty()) {
            return GREY;
        }
        return ITEMS.computeIfAbsent(stack.getItem(), item -> Materials.of(item)
                .map(thing -> thing.material().colour())
                .orElseGet(() -> average(Minecraft.getInstance().getItemRenderer().getModel(new ItemStack(item),
                        null, null, 0).getParticleIcon())));
    }

    /** The colour {@code fluid} reads as. */
    public static int of(final Fluid fluid) {
        return FLUIDS.computeIfAbsent(fluid, f -> MaterialLiquids.materialOf(f).map(Material::colour).orElseGet(() -> {
            final IClientFluidTypeExtensions look = IClientFluidTypeExtensions.of(f);
            final int tint = look.getTintColor() & WHITE;
            if (tint != WHITE) {
                return tint;
            }
            return average(Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                    .apply(look.getStillTexture()));
        }));
    }

    /** Hands the mod's bus what forgets the colours worked out whenever the resource packs change. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterClientReloadListenersEvent.class,
                event -> event.registerReloadListener((ResourceManagerReloadListener) manager -> forget()));
    }

    /** Forgets every colour worked out. */
    public static void forget() {
        ITEMS.clear();
        FLUIDS.clear();
    }

    /* The average of a sprite's opaque texels, read from its picture in the resource packs. */
    private static int average(final TextureAtlasSprite sprite) {
        final ResourceLocation name = sprite.contents().name();
        final ResourceLocation file = ResourceLocation.fromNamespaceAndPath(name.getNamespace(),
                "textures/" + name.getPath() + ".png");
        final Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(file);
        if (resource.isEmpty()) {
            return GREY;
        }
        try (InputStream in = resource.get().open(); NativeImage image = NativeImage.read(in)) {
            long red = 0;
            long green = 0;
            long blue = 0;
            long count = 0;
            // The first frame only: an animated picture's frames lie one under another, each as wide as it is.
            final int side = Math.min(image.getWidth(), image.getHeight());
            for (int y = 0; y < side; y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    final int abgr = image.getPixelRGBA(x, y);
                    if ((abgr >>> 24 & CHANNEL) < OPAQUE) {
                        continue;
                    }
                    red += abgr & CHANNEL;
                    green += abgr >> 8 & CHANNEL;
                    blue += abgr >> 16 & CHANNEL;
                    count++;
                }
            }
            return count == 0 ? GREY : (int) (red / count) << 16 | (int) (green / count) << 8 | (int) (blue / count);
        } catch (final IOException unreadable) {
            return GREY;
        }
    }
}
