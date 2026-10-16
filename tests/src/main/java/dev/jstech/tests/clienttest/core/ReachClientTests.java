/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import dev.jstech.tests.TestBlocks;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Optional;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.advancements.critereon.ImpossibleTrigger;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Core's reaches into the player's game, each checked by what it does, so a new version of the game that moves
 * what one reaches fails here: the aim lands on a part standing out of a block; an advancement's title runs past the
 * game's 163 pixels; and the toasts say which of their slots they hold.
 */
public final class ReachClientTests {

    private static final BlockPos STAND = new BlockPos(0, 2, -2);
    private static final BlockPos POST = new BlockPos(0, 2, 1);
    private static final int WAIT = 200;
    private static final int GAME_TITLE_WIDTH = 163;
    private static final String LONG_TITLE = "Have You Tried Turning It Off and On Again, and Then Off Once More?";
    private static final String OCCUPIED_SLOTS = "occupiedSlots";

    private ReachClientTests() {
    }

    @ClientTest(timeoutTicks = 400)
    public static void standingParts_catchTheAim(final ClientTestContext ctx) {
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> level.setBlockAndUpdate(ctx.abs(POST),
                        TestBlocks.STANDING_POST.get().defaultBlockState()))
                .thenWaitUntil(() -> ctx.mc().level != null && ctx.mc().level.getBlockState(ctx.abs(POST))
                        .is(TestBlocks.STANDING_POST.get()), WAIT, "the post to reach the player")
                // Through the post standing out of the block's top, where the game sees only air.
                .then(2, () -> ctx.lookAt(Vec3.atLowerCornerOf(ctx.abs(POST)).add(0.5, 1.25, 0.5)))
                .then(3, () -> {
                    final HitResult hit = ctx.mc().hitResult;
                    ctx.assertTrue(hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK
                            && block.getBlockPos().equals(ctx.abs(POST)), "the aim lands on the post's block; it is "
                            + hit);
                })
                .thenServer(0, level -> level.setBlockAndUpdate(ctx.abs(POST), Blocks.AIR.defaultBlockState()));
    }

    @ClientTest(timeoutTicks = 100)
    public static void advancementTitles_runAsWideAsTheyAreWritten(final ClientTestContext ctx) {
        ctx.then(0, () -> {
            final DisplayInfo display = new DisplayInfo(new ItemStack(Items.REDSTONE), Component.literal(LONG_TITLE),
                    Component.literal("x"), Optional.empty(), AdvancementType.TASK, false, false, false);
            final AdvancementHolder holder = Advancement.Builder.advancement().display(display)
                    .addCriterion("never", new ImpossibleTrigger().createCriterion(
                            new ImpossibleTrigger.TriggerInstance()))
                    .build(ResourceLocation.fromNamespaceAndPath("jstests", "long_title"));
            final AdvancementWidget widget = new AdvancementWidget(null, ctx.mc(), new AdvancementNode(holder, null),
                    display);
            ctx.assertTrue(ctx.mc().font.width(LONG_TITLE) > GAME_TITLE_WIDTH,
                    "the title is longer than the game cuts");
            ctx.assertTrue(widget.getWidth() > GAME_TITLE_WIDTH + 29 + 8, "the box grows past the game's cut; it is "
                    + widget.getWidth());
        });
    }

    /* The test mod is built without the Core's access transformer: it finds the field by its name, as the game runs. */
    @ClientTest(timeoutTicks = 100)
    public static void toasts_sayWhichSlotsTheyHold(final ClientTestContext ctx) {
        ctx.then(0, () -> {
            try {
                final Field slots = ToastComponent.class.getDeclaredField(OCCUPIED_SLOTS);
                ctx.assertTrue(Modifier.isPublic(slots.getModifiers()), "the slots the toasts hold are open to read");
                ctx.assertTrue(slots.get(ctx.mc().getToasts()) != null, "and the toasts keep them");
            } catch (final NoSuchFieldException | IllegalAccessException moved) {
                ctx.assertTrue(false, "the toasts' slots moved: " + moved);
            }
        });
    }
}
