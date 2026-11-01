/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.series;

import com.mojang.blaze3d.platform.NativeImage;
import dev.jstech.core.jstechseries.energy.ClampMeter;
import dev.jstech.core.jstechseries.fire.FireSafety;
import dev.jstech.core.jstechseries.hazard.HazardDevices;
import dev.jstech.core.jstechseries.probe.NetworkProbe;
import dev.jstech.core.jstechseries.probe.SignalProbe;
import dev.jstech.core.jstechseries.tools.PhysicsTool;
import dev.jstech.core.jstechseries.tools.SharedTools;
import dev.jstech.industrial.tool.HandTools;
import dev.jstech.testkit.client.ClientTestHelper;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * The tools and instruments drawn in three dimensions in the hand: each of the fourteen held, read off the screen
 * against the empty hand, and seen from the player's eye and from in front of them beside the game's own iron axe and
 * clock, held the two ways they are, for the eye to judge its grip; all of them laid on the ground; and their icons in
 * a slot as bright as their textures.
 */
public final class HeldModelsClientTests {

    private static final BlockPos STAND = new BlockPos(0, 2, 0);
    private static final int WAIT = 200;
    /*
     * Ticks for a held item to be raised into view once it changes: a slow tool's swing is kept lowered until it has
     * recovered, a second and a quarter for the Forge Hammer.
     */
    private static final int RAISED = 30;
    /* The items laid on the ground ahead of the player: so many in a row, the rows so far apart, in blocks. */
    private static final int A_ROW = 7;
    private static final int ROWS_APART = 2;
    /*
     * Where the held item is drawn in first person, as shares of the screen, read a pixel in so many; how different a
     * pixel must be from the empty hand's, in the sum of its channels, to count; and how many must, at least.
     */
    private static final double FROM_X = 0.5;
    private static final double FROM_Y = 0.4;
    private static final int STEP = 4;
    private static final int DIFFERENT = 60;
    private static final int DRAWN = 40;
    private static final int CHANNEL = 0xFF;
    /*
     * An icon read off the screen texel by texel against its flat texture: the texels a side, how far apart in the sum
     * of the channels a pixel may be from its texel and still be alike, and the share of a texture's opaque texels
     * that must be. A flat icon lit as a model's side comes out a fifth darker or more, far past it.
     */
    private static final int TEXELS = 16;
    private static final int LIKE = 30;
    private static final double ALIKE_SHARE = 0.9;
    private static final int SETTLE = 5;
    private static final List<Supplier<? extends Item>> HELD = List.of(HandTools.FORGE_HAMMER, HandTools.DRAW_PLATE,
            HandTools.MORTAR, SharedTools.SAW, SharedTools.WRENCH, FireSafety.WATER_EXTINGUISHER,
            FireSafety.FOAM_EXTINGUISHER, FireSafety.CARBON_DIOXIDE_EXTINGUISHER, FireSafety.DRY_POWDER_EXTINGUISHER,
            NetworkProbe.ITEM, SignalProbe.ITEM, ClampMeter.ITEM, HazardDevices.GEIGER_COUNTER,
            HazardDevices.POCKET_DOSIMETER);
    /* The game's own items held the two ways the tools are: a tool on the axe line, a plain item as a clock is. */
    private static final List<Item> REFERENCES = List.of(Items.IRON_AXE, Items.CLOCK);

    private HeldModelsClientTests() {
    }

    @ClientTest(timeoutTicks = 3000)
    public static void held_eachToolAndInstrumentIsDrawnInTheHand(final ClientTestContext ctx) {
        final int[][] empty = new int[1][];
        final List<ItemEntity> dropped = new ArrayList<>();
        ctx.afterTest(() -> {
            ctx.mc().options.setCameraType(CameraType.FIRST_PERSON);
            ctx.server().execute(() -> dropped.forEach(Entity::discard));
        });
        ctx.thenTeleport(0, STAND, Direction.NORTH)
                .thenServer(0, level -> ctx.hold(ItemStack.EMPTY))
                .thenAwaitLandDrawn(WAIT)
                .then(0, () -> ctx.lookAt(ctx.player().getEyePosition().add(0, 0, -1)))
                .then(RAISED, () -> empty[0] = hand(ctx));
        // The game's own iron axe and clock first, held the game's way, for the eye to compare the grips with.
        for (final Item reference : REFERENCES) {
            final String name = BuiltInRegistries.ITEM.getKey(reference).getPath();
            ctx.thenServer(0, level -> ctx.hold(new ItemStack(reference)))
                    .thenWaitUntil(() -> ctx.player().getMainHandItem().is(reference), WAIT, name + " in the hand")
                    .thenScreenshot(RAISED, "held-" + name)
                    .then(0, () -> ctx.mc().options.setCameraType(CameraType.THIRD_PERSON_FRONT))
                    .thenScreenshot(3, "held-" + name + "-front")
                    .then(0, () -> ctx.mc().options.setCameraType(CameraType.FIRST_PERSON));
        }
        for (final Supplier<? extends Item> held : HELD) {
            final String name = BuiltInRegistries.ITEM.getKey(held.get()).getPath();
            ctx.thenServer(0, level -> ctx.hold(new ItemStack(held.get())))
                    .thenWaitUntil(() -> ctx.player().getMainHandItem().is(held.get()), WAIT, name + " in the hand")
                    .then(RAISED, () -> {
                        final int drawn = differing(empty[0], hand(ctx));
                        ctx.assertTrue(drawn >= DRAWN, name + " is drawn in the hand: " + drawn + " pixels");
                    })
                    .thenScreenshot(0, "held-" + name)
                    .then(0, () -> ctx.mc().options.setCameraType(CameraType.THIRD_PERSON_FRONT))
                    .thenScreenshot(3, "held-" + name + "-front")
                    .then(0, () -> ctx.mc().options.setCameraType(CameraType.FIRST_PERSON));
        }
        // On the ground they keep their models too, laid in two rows ahead of the player.
        ctx.thenServer(0, level -> {
            ctx.hold(ItemStack.EMPTY);
            for (int i = 0; i < HELD.size(); i++) {
                final BlockPos at = ctx.abs(new BlockPos(i % A_ROW - A_ROW / 2, STAND.getY(),
                        -3 - i / A_ROW * ROWS_APART));
                final ItemEntity item = new ItemEntity(level, at.getX() + 0.5, at.getY() + 0.2, at.getZ() + 0.5,
                        new ItemStack(HELD.get(i).get()), 0, 0, 0);
                item.setNeverPickUp();
                level.addFreshEntity(item);
                dropped.add(item);
            }
        })
                .then(20, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(new BlockPos(0, STAND.getY(), -4)))))
                .thenScreenshot(10, "held-models-on-the-ground")
                .thenServer(0, level -> dropped.forEach(Entity::discard));
    }

    @ClientTest(timeoutTicks = 400)
    public static void icons_inASlotAreDrawnAsTheirFlatTexture(final ClientTestContext ctx) {
        final List<ItemStack> stacks = new ArrayList<>();
        HELD.forEach(held -> stacks.add(new ItemStack(held.get())));
        // The physics tool too, whose model in the hand is drawn by its own renderer.
        stacks.add(new ItemStack(PhysicsTool.ITEM.get()));
        ctx.afterTest(() -> ctx.mc().setScreen(null));
        ctx.then(0, () -> ctx.mc().setScreen(new IconSheet(stacks)))
                .thenScreenshot(SETTLE, "held-icons")
                .then(0, () -> {
                    try (ClientTestHelper.FrameSampler frame = ctx.sampleFrame()) {
                        for (int i = 0; i < stacks.size(); i++) {
                            final ResourceLocation id = BuiltInRegistries.ITEM.getKey(stacks.get(i).getItem());
                            final int[] texels = texels(ctx, id.withPath(path -> "textures/item/" + path + ".png"));
                            int opaque = 0;
                            int alike = 0;
                            for (int t = 0; t < texels.length; t++) {
                                if (texels[t] >>> 24 != CHANNEL) {
                                    continue;
                                }
                                opaque++;
                                final double x = (IconSheet.LEFT + i % IconSheet.COLUMNS * IconSheet.CELL
                                        + t % TEXELS + 0.5) * IconSheet.SCALE;
                                final double y = (IconSheet.TOP + i / IconSheet.COLUMNS * IconSheet.CELL
                                        + t / TEXELS + 0.5) * IconSheet.SCALE;
                                if (apart(frame.pixel((int) x, (int) y), texels[t]) <= LIKE) {
                                    alike++;
                                }
                            }
                            ctx.assertTrue(opaque > 0 && alike >= opaque * ALIKE_SHARE, id + "'s icon is drawn as "
                                    + "its flat texture, not darker: " + alike + " of " + opaque + " texels alike");
                        }
                    }
                });
    }

    /* The brightness of the screen where the main hand's item is drawn, a pixel in so many. */
    private static int[] hand(final ClientTestContext ctx) {
        final int width = ctx.mc().getWindow().getGuiScaledWidth();
        final int height = ctx.mc().getWindow().getGuiScaledHeight();
        final int fromX = (int) (width * FROM_X);
        final int fromY = (int) (height * FROM_Y);
        final int[] out = new int[((width - fromX) / STEP) * ((height - fromY) / STEP)];
        try (ClientTestHelper.FrameSampler frame = ctx.sampleFrame()) {
            int i = 0;
            for (int y = fromY; y + STEP <= height; y += STEP) {
                for (int x = fromX; x + STEP <= width; x += STEP) {
                    if (i < out.length) {
                        final int argb = frame.pixel(x, y);
                        out[i++] = (argb >> 16 & CHANNEL) + (argb >> 8 & CHANNEL) + (argb & CHANNEL);
                    }
                }
            }
        }
        return out;
    }

    /* How many pixels differ clearly between two readings. */
    private static int differing(final int[] before, final int[] after) {
        int count = 0;
        for (int i = 0; i < Math.min(before.length, after.length); i++) {
            if (Math.abs(before[i] - after[i]) > DIFFERENT) {
                count++;
            }
        }
        return count;
    }

    /* How far a pixel on the screen (ARGB) is from a texel (ARGB), in the sum of their channels' differences. */
    private static int apart(final int pixel, final int texel) {
        return Math.abs((pixel >> 16 & CHANNEL) - (texel >> 16 & CHANNEL))
                + Math.abs((pixel >> 8 & CHANNEL) - (texel >> 8 & CHANNEL))
                + Math.abs((pixel & CHANNEL) - (texel & CHANNEL));
    }

    /* The texels of the sixteen-square texture {@code file} as the player's game reads it, ARGB, row by row. */
    private static int[] texels(final ClientTestContext ctx, final ResourceLocation file) {
        final Resource resource = ctx.mc().getResourceManager().getResource(file)
                .orElseThrow(() -> new IllegalStateException("no texture " + file));
        try (InputStream in = resource.open(); NativeImage image = NativeImage.read(NativeImage.Format.RGBA, in)) {
            final int[] out = new int[TEXELS * TEXELS];
            for (int y = 0; y < TEXELS; y++) {
                for (int x = 0; x < TEXELS; x++) {
                    out[y * TEXELS + x] = FastColor.ABGR32.fromArgb32(image.getPixelRGBA(x, y));
                }
            }
            return out;
        } catch (final IOException unreadable) {
            throw new UncheckedIOException(unreadable);
        }
    }

    /* The held items' icons as a slot draws them, in rows on a plain ground, each so many times its size. */
    private static final class IconSheet extends Screen {

        private final List<ItemStack> stacks;

        private static final int SCALE = 2;
        private static final int CELL = 18;
        private static final int COLUMNS = 7;
        private static final int LEFT = 4;
        private static final int TOP = 4;
        private static final int GROUND = 0xFF << 24 | 0x8B << 16 | 0x8B << 8 | 0x8B;

        private IconSheet(final List<ItemStack> stacks) {
            super(Component.empty());
            this.stacks = List.copyOf(stacks);
        }

        @Override
        public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
            g.fill(0, 0, this.width, this.height, GROUND);
            g.pose().pushPose();
            g.pose().scale(SCALE, SCALE, SCALE);
            for (int i = 0; i < this.stacks.size(); i++) {
                g.renderItem(this.stacks.get(i), LEFT + i % COLUMNS * CELL, TOP + i / COLUMNS * CELL);
            }
            g.pose().popPose();
        }
    }
}
