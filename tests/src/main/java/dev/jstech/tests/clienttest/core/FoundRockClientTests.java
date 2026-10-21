/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import com.mojang.blaze3d.platform.NativeImage;
import dev.jstech.core.JsCore;
import dev.jstech.core.resource.CachedRuntimeFiles;
import dev.jstech.core.resource.RuntimePacks;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import net.neoforged.fml.loading.FMLPaths;

/**
 * The art of an ore in another mod's rock on a player's game: tin's ore in the test mod's marble is drawn on the
 * texture the marble's own model draws it with, as no texture is at the marble's name, and the drawing is kept on disk
 * for the next load.
 */
public final class FoundRockClientTests {

    private static final String ORE = "jstests_test_marble_tin_ore";
    private static final int WIDTH = 16;

    private FoundRockClientTests() {
    }

    @ClientTest(timeoutTicks = 100)
    public static void foundRocksOre_isDrawnFromItsModelAndKept(final ClientTestContext ctx) {
        ctx.then(0, () -> {
            final ResourceLocation pack = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "material_art");
            final PackResources art = RuntimePacks.open(pack, PackType.CLIENT_RESOURCES).orElseThrow();
            final IoSupplier<InputStream> texture = art.getResource(PackType.CLIENT_RESOURCES,
                    ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "textures/block/" + ORE + ".png"));
            ctx.assertTrue(texture != null, "the ore in the marble has a texture");
            try (InputStream in = texture.get(); NativeImage image = NativeImage.read(in)) {
                ctx.assertTrue(image.getWidth() == WIDTH && image.getHeight() == WIDTH, "a block's square texture");
                ctx.assertTrue((image.getPixelRGBA(0, 0) >>> 24) != 0, "drawn on the marble, whole, not left clear");
            } catch (final IOException e) {
                ctx.fail("the ore's texture could not be read: " + e.getMessage());
            }
            art.close();
            final Path kept = FMLPaths.GAMEDIR.get().resolve(CachedRuntimeFiles.FOLDER).resolve(JsCore.MODID)
                    .resolve("material_art").resolve("assets/" + JsCore.MODID + "/textures/block/" + ORE + ".png");
            ctx.assertTrue(Files.isRegularFile(kept), "and the drawing is kept on disk for the next load: " + kept);
        });
    }
}
