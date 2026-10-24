/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import dev.jstech.core.material.Forms;
import dev.jstech.core.material.MaterialUses;
import dev.jstech.core.material.Materials;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The test mod's content pack on a player's game: its blocks are drawn with the models the Core made for them rather
 * than the missing model, a machine's terminal as it faces, and every thing has its English name, a material's
 * ingot named for its material.
 */
public final class ContentPackClientTests {

    private static final String PACK = "jstestpack";

    private ContentPackClientTests() {
    }

    @ClientTest(timeoutTicks = 100)
    public static void contentPack_isDrawnAndNamedOnAPlayersGame(final ClientTestContext ctx) {
        ctx.then(0, () -> {
            final ModelManager models = ctx.mc().getModelManager();
            final BakedModel missing = models.getMissingModel();
            final BlockState lamp = BuiltInRegistries.BLOCK.get(id("test_lamp")).defaultBlockState();
            final BlockState press = BuiltInRegistries.BLOCK.get(id("test_pack_press")).defaultBlockState();
            ctx.assertTrue(models.getBlockModelShaper().getBlockModel(lamp) != missing,
                    "the lamp has the model made for it");
            ctx.assertTrue(models.getBlockModelShaper().getBlockModel(press) != missing,
                    "and so has the press, as it faces");
            ctx.assertTrue(name("test_lamp").equals("Test Pack Lamp") && name("test_token").equals("Test Pack Token")
                    && name("test_pack_press").equals("Test Pack Press"), "the pack's things have their English names");
            final ItemStack ingot = new ItemStack(MaterialUses.item(Materials.byId(id("test_mythril")).orElseThrow(),
                    Forms.INGOT).get());
            ctx.assertTrue(ingot.getHoverName().getString().equals("Test Mythril Ingot"),
                    "and the material's ingot is named for it: " + ingot.getHoverName().getString());
        });
    }

    private static String name(final String path) {
        return new ItemStack(BuiltInRegistries.ITEM.get(id(path))).getHoverName().getString();
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(PACK, path);
    }
}
