/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import dev.jstech.core.client.sky.SkyPlace;
import dev.jstech.core.dimension.RuntimeDimensions;
import dev.jstech.core.sky.Skies;
import dev.jstech.space.JsSpace;
import dev.jstech.space.client.life.SunGlare;
import dev.jstech.space.life.LifeSupport;
import dev.jstech.space.life.LunarSuit;
import dev.jstech.space.worldgen.MoonWorld;
import dev.jstech.testkit.client.ClientTestHelper;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * The lunar suit on a player's game: worn on a stand, each of its four pieces is drawn, the helmet, the suit with its
 * chest module, the trousers and the boots, where the stand alone shows none of them; the gold visor, fitted, is drawn
 * gold over the smoked one. On the Moon at noon, looking at the Sun whites the view out, and the gold visor lowered
 * cuts it to a gold tint.
 */
public final class LunarSuitClientTests {

    /* The player and the stand facing each other three blocks apart, and the heights on the stand looked at. */
    private static final BlockPos EYE = new BlockPos(0, 2, -1);
    private static final BlockPos STAND = new BlockPos(0, 2, 2);
    private static final double VISOR = 1.78;
    /* The suit's white cloth below its chest module, which stands out from the grass behind. */
    private static final double CHEST = 1.08;
    private static final double KNEE = 0.45;
    private static final double BOOT = 0.12;
    /* A stand faces north, at the player, turned half round. */
    private static final float FACING_NORTH = 180F;
    /*
     * How far a piece's colours stand from the bare stand's; how much redder than blue gold is, and how much brighter
     * than the smoked visor.
     */
    private static final int DRAWN = 40;
    private static final int GOLDEN = 40;
    private static final int BRIGHTER = 40;
    /* The patches read, in scaled pixels either way; the glare's read off the Sun's disc. */
    private static final int PATCH = 3;
    private static final int GLARE_PATCH = 6;
    private static final int OFF_THE_DISC = 40;
    /* How white the glare makes the view, and how dark it stays behind the gold visor. */
    private static final int WHITE = 190;
    private static final int DIMMED = 100;
    private static final int TINTED = 8;
    private static final long NOON = 6_000L;
    private static final int ABOVE = 3;
    private static final int ARRIVE = 600;
    private static final int SETTLE = 10;
    private static final int CHANNEL = 0xFF;
    private static final double LOOK_AHEAD = 16;

    private LunarSuitClientTests() {
    }

    @ClientTest(timeoutTicks = 600)
    public static void suit_isDrawnPieceByPieceAndTheGoldVisorWhenFitted(final ClientTestContext ctx) {
        final double[] heights = {VISOR, CHEST, KNEE, BOOT};
        final int[][] suited = new int[heights.length][];
        ctx.afterTest(() -> ctx.server().execute(() -> clearStands(ctx)));
        ctx.thenTeleport(0, EYE, Direction.SOUTH)
                .thenServer(0, level -> {
                    clearStands(ctx);
                    final ArmorStand stand = EntityType.ARMOR_STAND.create(level);
                    stand.moveTo(Vec3.atBottomCenterOf(ctx.abs(STAND)), FACING_NORTH, 0F);
                    stand.setItemSlot(EquipmentSlot.HEAD, new ItemStack(LunarSuit.HELMET.get()));
                    stand.setItemSlot(EquipmentSlot.CHEST, new ItemStack(LunarSuit.SUIT.get()));
                    stand.setItemSlot(EquipmentSlot.LEGS, new ItemStack(LunarSuit.TROUSERS.get()));
                    stand.setItemSlot(EquipmentSlot.FEET, new ItemStack(LunarSuit.BOOTS.get()));
                    level.addFreshEntity(stand);
                })
                .thenWaitUntil(() -> !stands(ctx).isEmpty(), ARRIVE, "the stand to reach the player's game")
                .thenScreenshot(SETTLE, "lunar-suit-on-a-stand");
        for (int i = 0; i < heights.length; i++) {
            final int k = i;
            ctx.then(2, () -> lookAtStand(ctx, heights[k])).then(4, () -> suited[k] = mean(ctx, PATCH, 0));
        }
        ctx.thenServer(0, level -> {
            for (final EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
                    EquipmentSlot.FEET)) {
                serverStand(ctx).setItemSlot(slot, ItemStack.EMPTY);
            }
        });
        for (int i = 0; i < heights.length; i++) {
            final int k = i;
            ctx.then(2, () -> lookAtStand(ctx, heights[k])).then(4, () -> {
                final int[] bare = mean(ctx, PATCH, 0);
                ctx.assertTrue(distance(suited[k], bare) > DRAWN, "the suit is drawn at " + heights[k]
                        + " blocks up the stand: " + str(suited[k]) + " where the bare stand shows " + str(bare));
            });
        }
        ctx.thenServer(0, level -> {
                    final ItemStack helmet = new ItemStack(LunarSuit.HELMET.get());
                    LifeSupport.fit(level, helmet, ResourceLocation.fromNamespaceAndPath(JsSpace.MODID,
                            "gold_visor"));
                    serverStand(ctx).setItemSlot(EquipmentSlot.HEAD, helmet);
                })
                .then(4, () -> lookAtStand(ctx, VISOR))
                .then(6, () -> {
                    final int[] visor = mean(ctx, PATCH, 0);
                    ctx.assertTrue(visor[0] - visor[2] > GOLDEN && visor[0] - suited[0][0] > BRIGHTER,
                            "the gold visor is drawn gold over the smoked one: " + str(visor) + ", without it "
                                    + str(suited[0]));
                })
                .thenScreenshot(2, "lunar-helmet-gold-visor-down");
    }

    @ClientTest(timeoutTicks = 2400)
    public static void sunGlare_whitesOutTheViewOnTheMoonAndTheGoldVisorCutsIt(final ClientTestContext ctx) {
        final boolean[] daylight = new boolean[1];
        ctx.afterTest(() -> ctx.server().execute(() -> {
            ctx.server().getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(daylight[0], ctx.server());
            final ServerPlayer player = ctx.serverPlayer();
            player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
            final BlockPos origin = ctx.origin().above(2);
            player.teleportTo(ctx.server().overworld(), origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5, 0F,
                    0F);
        }));
        ctx.thenServer(0, level -> {
                    final GameRules.BooleanValue rule = level.getServer().getGameRules()
                            .getRule(GameRules.RULE_DAYLIGHT);
                    daylight[0] = rule.get();
                    rule.set(false, level.getServer());
                    standOnTheMoonAtNoon(ctx);
                })
                .thenWaitUntil(() -> ctx.mc().level != null
                        && ctx.mc().level.dimension().equals(MoonWorld.world().dimension()), ARRIVE,
                        "the player's game to reach the Moon")
                .thenAwaitLandDrawn(ARRIVE)
                .then(SETTLE, () -> lookAtTheSun(ctx))
                .then(SETTLE, () -> {
                    final double glare = SunGlare.glare(ctx.mc().level, ctx.mc().gameRenderer.getMainCamera());
                    final int[] seen = mean(ctx, GLARE_PATCH, OFF_THE_DISC);
                    ctx.assertTrue(glare > 0.9, "looking at the Sun in the Moon's vacuum, it glares: " + glare);
                    ctx.assertTrue(seen[0] > WHITE && seen[1] > WHITE && seen[2] > WHITE,
                            "and whites out the view: " + str(seen));
                })
                .thenScreenshot(0, "moon-sun-glare")
                .thenServer(0, level -> {
                    final ItemStack helmet = new ItemStack(LunarSuit.HELMET.get());
                    LifeSupport.fit(level, helmet, ResourceLocation.fromNamespaceAndPath(JsSpace.MODID,
                            "gold_visor"));
                    ctx.serverPlayer().setItemSlot(EquipmentSlot.HEAD, helmet);
                })
                .then(SETTLE, () -> lookAtTheSun(ctx))
                .then(SETTLE, () -> {
                    final int[] seen = mean(ctx, GLARE_PATCH, OFF_THE_DISC);
                    ctx.assertTrue(seen[0] < DIMMED && seen[1] < DIMMED && seen[2] < DIMMED,
                            "the gold visor lowered cuts the glare: " + str(seen));
                    ctx.assertTrue(seen[0] - seen[2] > TINTED, "and the view through it is gold: " + str(seen));
                })
                .thenScreenshot(0, "moon-sun-through-the-gold-visor");
    }

    /* The player standing on the Moon where its longitude and latitude are nought, at the Moon's noon there. */
    private static void standOnTheMoonAtNoon(final ClientTestContext ctx) {
        final ResourceKey<Level> key = MoonWorld.world().dimension();
        final ServerLevel moon = RuntimeDimensions.getOrCreate(ctx.server(), key, key.location());
        moon.setDayTime(NOON);
        moon.getChunk(BlockPos.ZERO);
        final int ground = moon.getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0);
        final ServerPlayer player = ctx.serverPlayer();
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
        player.teleportTo(moon, 0.5, ground + ABOVE, 0.5, 0F, 0F);
    }

    /* Turns the player's eyes on the Sun, where the game draws it from here. */
    private static void lookAtTheSun(final ClientTestContext ctx) {
        final Vec3 eye = ctx.player().getEyePosition();
        final Vector3f sun = SkyPlace.at(ctx.mc().level, Skies.of(ctx.mc().level), eye.x, eye.z).sun(0).normalize();
        ctx.lookAt(eye.add(sun.x * LOOK_AHEAD, sun.y * LOOK_AHEAD, sun.z * LOOK_AHEAD));
    }

    private static void lookAtStand(final ClientTestContext ctx, final double height) {
        ctx.lookAt(Vec3.atBottomCenterOf(ctx.abs(STAND)).add(0, height, 0));
    }

    private static List<ArmorStand> stands(final ClientTestContext ctx) {
        return ctx.mc().level.getEntitiesOfClass(ArmorStand.class, ctx.player().getBoundingBox().inflate(8));
    }

    private static ArmorStand serverStand(final ClientTestContext ctx) {
        return ctx.serverPlayer().serverLevel().getEntitiesOfClass(ArmorStand.class,
                ctx.serverPlayer().getBoundingBox().inflate(8)).getFirst();
    }

    private static void clearStands(final ClientTestContext ctx) {
        ctx.serverPlayer().serverLevel().getEntitiesOfClass(ArmorStand.class,
                ctx.serverPlayer().getBoundingBox().inflate(16)).forEach(ArmorStand::discard);
    }

    /*
     * The mean colour, red, green and blue, of a patch {@code patch} scaled pixels either way about a point
     * {@code off} pixels right of the screen's middle.
     */
    private static int[] mean(final ClientTestContext ctx, final int patch, final int off) {
        final int x = ctx.mc().getWindow().getGuiScaledWidth() / 2 + off;
        final int y = ctx.mc().getWindow().getGuiScaledHeight() / 2;
        final long[] sum = new long[3];
        int count = 0;
        try (ClientTestHelper.FrameSampler frame = ctx.sampleFrame()) {
            for (int dx = -patch; dx <= patch; dx++) {
                for (int dy = -patch; dy <= patch; dy++) {
                    final int argb = frame.pixel(x + dx, y + dy);
                    sum[0] += argb >> 16 & CHANNEL;
                    sum[1] += argb >> 8 & CHANNEL;
                    sum[2] += argb & CHANNEL;
                    count++;
                }
            }
        }
        return new int[] {(int) (sum[0] / count), (int) (sum[1] / count), (int) (sum[2] / count)};
    }

    private static int distance(final int[] a, final int[] b) {
        return Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]) + Math.abs(a[2] - b[2]);
    }

    private static String str(final int[] colour) {
        return "(" + colour[0] + ", " + colour[1] + ", " + colour[2] + ")";
    }
}
