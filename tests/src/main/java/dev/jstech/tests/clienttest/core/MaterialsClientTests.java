/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import com.mojang.blaze3d.platform.NativeImage;
import dev.jstech.core.client.material.MaterialTooltips;
import dev.jstech.core.jstechseries.SeriesMaterials;
import dev.jstech.core.material.FormArt;
import dev.jstech.core.material.Forms;
import dev.jstech.core.material.CatalogueItems;
import dev.jstech.core.material.HostRocks;
import dev.jstech.core.material.MaterialLiquids;
import dev.jstech.core.material.MaterialThing;
import dev.jstech.core.material.MaterialUses;
import dev.jstech.core.material.RegisteredThing;
import dev.jstech.tests.TestMaterials;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * The things of the materials the installed mods use, on a player's game: every one drawn in its material's colour
 * from the art made as the resources load, none of them the missing model; ores in each rock with the mineral's specks
 * on the rock's own texture; and a material's tooltip with its lines, those that wait for Shift behind a hint.
 */
public final class MaterialsClientTests {

    private static final int SETTLE = 4;
    private static final BlockPos WALL = new BlockPos(2, 3, 4);
    /* In the ground before the wall, as the players stand at y 2. */
    private static final BlockPos POOL = new BlockPos(3, 1, 6);
    private static final BlockPos VIEW = new BlockPos(5, 2, 9);
    private static final int MOST_TEXELS = FormArt.SIZE * FormArt.SIZE;

    private MaterialsClientTests() {
    }

    @ClientTest(timeoutTicks = 400)
    public static void things_areDrawnFromTheArtMadeAsTheGameLoads(final ClientTestContext ctx) {
        final List<ItemStack> stacks = new ArrayList<>();
        for (final RegisteredThing thing : MaterialUses.registered()) {
            stacks.add(new ItemStack(BuiltInRegistries.ITEM.get(thing.id())));
        }
        MaterialLiquids.buckets().forEach(bucket -> stacks.add(new ItemStack(bucket)));
        CatalogueItems.items().forEach(item -> stacks.add(new ItemStack(item)));
        ctx.then(0, () -> {
                    final BakedModel missing = ctx.mc().getModelManager().getMissingModel();
                    final List<String> broken = new ArrayList<>();
                    for (final ItemStack stack : stacks) {
                        if (ctx.mc().getItemRenderer().getModel(stack, null, null, 0) == missing) {
                            broken.add(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
                        }
                    }
                    ctx.assertTrue(broken.isEmpty(), "every thing of a material has its model: " + broken);
                })
                .then(0, () -> ctx.mc().setScreen(new Showcase(stacks)))
                .thenScreenshot(SETTLE, "every-thing-three-times-its-size")
                .then(0, () -> ctx.mc().setScreen(null));
    }

    @ClientTest(timeoutTicks = 400)
    public static void ores_wearTheirRockWithTheMineralsSpecks(final ClientTestContext ctx) {
        final List<Block> wall = List.of(MaterialUses.oreBlock(SeriesMaterials.TIN, HostRocks.STONE).get(),
                MaterialUses.oreBlock(SeriesMaterials.TIN, HostRocks.DEEPSLATE).get(),
                MaterialUses.oreBlock(SeriesMaterials.TIN, HostRocks.GRANITE).get(),
                MaterialUses.oreBlock(SeriesMaterials.IRON, HostRocks.TUFF).get(), Blocks.IRON_ORE,
                MaterialUses.block(SeriesMaterials.TIN, Forms.BLOCK).get(),
                MaterialUses.oreBlock(TestMaterials.TESTITE, HostRocks.GRANITE).get());
        ctx.thenBuild(0, world -> {
                    for (int i = 0; i < wall.size(); i++) {
                        world.setBlock(WALL.east(i), wall.get(i));
                    }
                    // A pool of molten tin before the wall, sunk into the ground, which holds it in.
                    for (int x = 0; x < 3; x++) {
                        world.setBlock(POOL.east(x), MaterialLiquids.of(SeriesMaterials.TIN).orElseThrow()
                                .fluid().defaultFluidState().createLegacyBlock());
                    }
                })
                .thenTeleport(SETTLE, VIEW, Direction.NORTH)
                .thenScreenshot(SETTLE * 2, "tin-in-stone-deepslate-granite-iron-in-tuff-the-games-own-a-block"
                        + "-a-radioactive-mineral-and-molten-tin");
    }

    @ClientTest(timeoutTicks = 100)
    public static void ores_ofVeinsCarryTheVeinsOfTheGamesQuartz(final ClientTestContext ctx) {
        ctx.then(0, () -> {
            final ResourceLocation ore = BuiltInRegistries.BLOCK.getKey(
                    MaterialUses.oreBlock(TestMaterials.TESTITE, HostRocks.STONE).get());
            final int[] drawn = texels(ctx, ResourceLocation.fromNamespaceAndPath(ore.getNamespace(),
                    "textures/block/" + ore.getPath() + ".png"));
            final int[] stone = texels(ctx, ResourceLocation.withDefaultNamespace("textures/block/stone.png"));
            final int[] veins = FormArt.specks(
                    texels(ctx, ResourceLocation.withDefaultNamespace("textures/block/nether_quartz_ore.png")),
                    FormArt.SIZE,
                    texels(ctx, ResourceLocation.withDefaultNamespace("textures/block/netherrack.png")),
                    FormArt.SIZE);
            int veined = 0;
            for (int i = 0; i < drawn.length; i++) {
                if (veins[i] != 0) {
                    veined++;
                    ctx.assertTrue(drawn[i] != stone[i], "a texel of the quartz's veins is the mineral at " + i);
                } else {
                    ctx.assertTrue(drawn[i] == stone[i], "a texel off the veins is the stone's own at " + i);
                }
            }
            ctx.assertTrue(veined > MOST_TEXELS / 8, "the quartz's veins run through it: " + veined + " texels");
        });
    }

    @ClientTest(timeoutTicks = 200)
    public static void tooltip_showsTheMaterialsLinesAndKeepsSomeForShift(final ClientTestContext ctx) {
        ctx.then(0, () -> {
            final MaterialThing ingot = new MaterialThing(SeriesMaterials.IRON, Forms.INGOT);
            final List<Component> lines = MaterialTooltips.lines(ingot, false);
            final List<String> plain = strings(lines);
            final List<String> shifted = strings(MaterialTooltips.lines(ingot, true));
            ctx.assertTrue(plain.getFirst().equals("Fe") && lines.getFirst().getStyle().isBold()
                    && plain.contains("Hold Shift for more") && plain.stream().noneMatch(line -> line.startsWith(
                    "Symbol") || line.startsWith("Atomic number")),
                    "without Shift, the symbol alone and set apart first, and a hint: " + plain);
            ctx.assertTrue(shifted.contains("Atomic number: 26") && shifted.contains("At room temperature: solid")
                    && !shifted.contains("Hold Shift for more"), "with Shift, the rest: " + shifted);
            final List<Component> onTheGame = new ItemStack(Items.IRON_INGOT).getTooltipLines(
                    Item.TooltipContext.EMPTY, ctx.mc().player, TooltipFlag.NORMAL);
            ctx.assertTrue(strings(onTheGame).contains("Fe"), "the game's own iron ingot shows them too");
            final List<String> testium = strings(MaterialTooltips.lines(
                    new MaterialThing(TestMaterials.TESTIUM, Forms.DUST), true));
            ctx.assertTrue(testium.equals(List.of("At room temperature: solid")),
                    "a material that gives nothing but its kind says only its state: " + testium);
        });
    }

    private static List<String> strings(final List<Component> lines) {
        final List<String> out = new ArrayList<>();
        lines.forEach(line -> out.add(line.getString()));
        return out;
    }

    /* The texels of the texture {@code file} as the player's game reads it, ARGB, row by row, sixteen to a row. */
    private static int[] texels(final ClientTestContext ctx, final ResourceLocation file) {
        final Resource resource = ctx.mc().getResourceManager().getResource(file)
                .orElseThrow(() -> new IllegalStateException("no texture " + file));
        try (InputStream in = resource.open(); NativeImage image = NativeImage.read(NativeImage.Format.RGBA, in)) {
            final int[] out = new int[FormArt.SIZE * FormArt.SIZE];
            for (int y = 0; y < FormArt.SIZE; y++) {
                for (int x = 0; x < FormArt.SIZE; x++) {
                    out[y * FormArt.SIZE + x] = FastColor.ABGR32.fromArgb32(image.getPixelRGBA(x, y));
                }
            }
            return out;
        } catch (final IOException unreadable) {
            throw new UncheckedIOException(unreadable);
        }
    }

    /* The things side by side, each three times its size on a plain ground, for the art to be judged by eye. */
    private static final class Showcase extends Screen {

        private final List<ItemStack> stacks;

        private static final int SCALE = 3;
        private static final int CELL = 18;
        private static final int COLUMNS = 8;
        private static final int GROUND = 0xFF << 24 | 0x8B << 16 | 0x8B << 8 | 0x8B;

        private Showcase(final List<ItemStack> stacks) {
            super(Component.empty());
            this.stacks = List.copyOf(stacks);
        }

        @Override
        public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
            g.fill(0, 0, this.width, this.height, GROUND);
            g.pose().pushPose();
            // Scaled the same along every axis, so a block's faces keep the light the game gives them.
            g.pose().scale(SCALE, SCALE, SCALE);
            for (int i = 0; i < this.stacks.size(); i++) {
                g.renderItem(this.stacks.get(i), 4 + i % COLUMNS * CELL, 4 + i / COLUMNS * CELL);
            }
            g.pose().popPose();
        }
    }
}
