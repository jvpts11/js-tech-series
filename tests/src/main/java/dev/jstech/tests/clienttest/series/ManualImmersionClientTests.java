/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.series;

import dev.jstech.core.client.guide.GuideClient;
import dev.jstech.core.client.guide.ManualScreen;
import dev.jstech.core.client.guide.ManualTips;
import dev.jstech.core.client.guide.TornSheetClient;
import dev.jstech.core.client.notification.NotificationsClient;
import dev.jstech.core.guide.GuideTexts;
import dev.jstech.core.guide.GuideTips;
import dev.jstech.core.guide.TornSheets;
import dev.jstech.core.notification.Notification;
import dev.jstech.core.notification.NotificationAction;
import dev.jstech.core.text.GameText;
import dev.jstech.tests.TestGuide;
import dev.jstech.tests.TestPress;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The manuals in the world: a page torn out as a sheet, a copy the manual keeps, which a frame on a wall shows as a map
 * is shown; a manual laid on a lectern and read from it, with the strip that says where and Take; and the tip the
 * first time an item a manual explains is picked up, whose click opens its page.
 */
public final class ManualImmersionClientTests {

    private static final int SETTLE = 4;
    private static final int OPEN = 40;
    private static final int WAIT = 80;
    private static final BlockPos STAND = new BlockPos(0, 2, -2);
    /* The wall two blocks south of the player and the frame hanging on its north face; the lectern in its place. */
    private static final BlockPos WALL = new BlockPos(0, 3, 1);
    private static final BlockPos FRAME = new BlockPos(0, 3, 0);
    private static final BlockPos LECTERN = new BlockPos(0, 2, 0);

    private ManualImmersionClientTests() {
    }

    @ClientTest(timeoutTicks = 500)
    public static void manual_tearsACopyOfAPageThatAFrameShows(final ClientTestContext ctx) {
        final int[] drawn = new int[1];
        ctx.afterTest(() -> clearFrame(ctx));
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenGive(0)
                .then(SETTLE, () -> GuideClient.open(TestGuide.MANUAL, TestGuide.PRESS))
                .thenAwaitScreen(ManualScreen.class, OPEN)
                .then(SETTLE, () -> {
                    final int[] tear = manual().tearButtonPoint();
                    ctx.click(tear[0] + 0.5, tear[1] + 0.5);
                })
                .thenWaitUntil(() -> sheet(ctx).isPresent(), OPEN, "a torn sheet in the player's inventory")
                .thenAssert(0, () -> manual().book().pageOf(TestGuide.PRESS).isPresent(),
                        "and the manual keeps its page")
                .thenAssert(0, () -> tooltipSays(ctx, sheet(ctx).orElseThrow(), "From the Test Manual, page"),
                        "the sheet's tooltip says which manual and which page it was torn from")
                .then(0, ManualImmersionClientTests::close)
                .thenAwaitNoScreen(OPEN)
                .then(0, () -> drawn[0] = TornSheetClient.framesDrawn())
                .thenServer(0, level -> {
                    level.setBlockAndUpdate(ctx.abs(WALL), Blocks.STONE.defaultBlockState());
                    final ItemFrame frame = new ItemFrame(level, ctx.abs(FRAME), Direction.NORTH);
                    frame.setItem(sheetOf(ctx.serverPlayer().getInventory().items), false);
                    level.addFreshEntity(frame);
                })
                .then(SETTLE, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(FRAME)).add(0, 0, 0.45)))
                .thenWaitUntil(() -> TornSheetClient.framesDrawn() > drawn[0], WAIT, "the frame to show its page")
                .thenScreenshot(SETTLE * 2, "torn_sheet_framed");
    }

    @ClientTest(timeoutTicks = 500)
    public static void lectern_holdsAManualReadWithItsStripAndTakenBack(final ClientTestContext ctx) {
        ctx.afterTest(() -> clearLectern(ctx));
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenGive(0, new ItemStack(TestGuide.MANUAL_ITEM.get()))
                .then(2, () -> ctx.selectHotbar(0))
                .thenServer(0, level -> level.setBlockAndUpdate(ctx.abs(LECTERN), Blocks.LECTERN.defaultBlockState()
                        .setValue(LecternBlock.FACING, Direction.NORTH)))
                .thenWaitUntil(() -> ctx.mc().level.getBlockState(ctx.abs(LECTERN)).is(Blocks.LECTERN), OPEN,
                        "the lectern in the world")
                .then(0, () -> ctx.rightClick(LECTERN, Direction.NORTH))
                .thenWaitUntil(() -> ctx.mc().level.getBlockState(ctx.abs(LECTERN)).getValue(LecternBlock.HAS_BOOK),
                        OPEN, "the manual laid on the lectern")
                .thenAssert(SETTLE, () -> !(ctx.mc().screen instanceof ManualScreen),
                        "laying it there opens nothing")
                .thenGive(0)
                .then(2, () -> ctx.rightClick(LECTERN, Direction.NORTH))
                .thenAwaitScreen(ManualScreen.class, OPEN)
                .thenAssert(SETTLE, () -> manual().lectern().filter(ctx.abs(LECTERN)::equals).isPresent(),
                        "using the lectern reads the manual from it")
                .thenScreenshot(2, "manual_on_lectern")
                .then(0, () -> button(GameText.component(GuideTexts.LECTERN_TAKE).getString()).onPress())
                .thenAwaitNoScreen(OPEN)
                .thenWaitUntil(() -> !ctx.mc().level.getBlockState(ctx.abs(LECTERN)).getValue(LecternBlock.HAS_BOOK),
                        OPEN, "Take takes the manual off the lectern")
                .thenWaitUntil(() -> ctx.player().getInventory().items.stream()
                        .anyMatch(stack -> stack.is(TestGuide.MANUAL_ITEM.get())), OPEN,
                        "into the player's inventory");
    }

    @ClientTest(timeoutTicks = 500)
    public static void firstPickup_bringsATipThatOpensItsPage(final ClientTestContext ctx) {
        final String upgrade = BuiltInRegistries.ITEM.getKey(TestPress.UPGRADE.get()).toString();
        ctx.afterTest(() -> ctx.player().connection.sendCommand("gamemode creative"));
        ctx.thenGive(0)
                .then(0, () -> {
                    ManualTips.forget(upgrade);
                    ctx.player().connection.sendCommand("gamemode survival");
                })
                .thenWaitUntil(() -> !ctx.player().isCreative(), OPEN, "the player in survival")
                .thenGive(0, new ItemStack(TestPress.UPGRADE.get()))
                .thenWaitUntil(() -> tip().isPresent(), WAIT, "a tip of the upgrade's page")
                .thenAssert(0, () -> ManualTips.told(upgrade), "the upgrade is told of once")
                .thenScreenshot(2, "manual_first_pickup_tip")
                .then(0, () -> NotificationsClient.act(tip().orElseThrow(), null))
                .thenAwaitScreen(ManualScreen.class, OPEN)
                .thenAssert(SETTLE, () -> manual().showing(manual().book().pageOf(TestGuide.PRESS).orElse(-1)),
                        "and its click opens the upgrade's page")
                .then(0, ManualImmersionClientTests::close)
                .thenAwaitNoScreen(OPEN);
    }

    /* In creative the tip waits for the player's word: none while the setting is off, and one once it is on. */
    @ClientTest(timeoutTicks = 500)
    public static void firstPickup_inCreativeOnlyWhenThePlayerAsks(final ClientTestContext ctx) {
        final String upgrade = BuiltInRegistries.ITEM.getKey(TestPress.UPGRADE.get()).toString();
        ctx.afterTest(() -> GuideTips.FILE.set(GuideTips.IN_CREATIVE, false));
        ctx.thenGive(0)
                .then(0, () -> {
                    GuideTips.FILE.set(GuideTips.IN_CREATIVE, false);
                    ManualTips.forget(upgrade);
                    NotificationsClient.board().clear();
                })
                .thenAssert(0, () -> ctx.player().isCreative(), "the player in creative")
                .thenGive(0, new ItemStack(TestPress.UPGRADE.get()))
                .thenAssert(WAIT / 2, () -> tip().isEmpty() && !ManualTips.told(upgrade),
                        "no tip in creative while the setting is off")
                .then(0, () -> GuideTips.FILE.set(GuideTips.IN_CREATIVE, true))
                .thenWaitUntil(() -> tip().isPresent(), WAIT, "a tip once the player asks for it in creative")
                .then(0, () -> GuideTips.FILE.set(GuideTips.IN_CREATIVE, false))
                .thenGive(0);
    }

    private static ManualScreen manual() {
        return (ManualScreen) Minecraft.getInstance().screen;
    }

    private static void close() {
        Minecraft.getInstance().setScreen(null);
    }

    /* The first torn sheet in the player's inventory, on their own game. */
    private static Optional<ItemStack> sheet(final ClientTestContext ctx) {
        return ctx.player().getInventory().items.stream().filter(stack -> stack.is(TornSheets.ITEM.get()))
                .findFirst();
    }

    /* A copy of the first torn sheet among the stacks, for the frame. */
    private static ItemStack sheetOf(final Iterable<ItemStack> stacks) {
        for (final ItemStack stack : stacks) {
            if (stack.is(TornSheets.ITEM.get())) {
                return stack.copyWithCount(1);
            }
        }
        return ItemStack.EMPTY;
    }

    private static boolean tooltipSays(final ClientTestContext ctx, final ItemStack stack, final String words) {
        return stack.getTooltipLines(Item.TooltipContext.of(ctx.mc().level), ctx.player(), TooltipFlag.NORMAL)
                .stream().map(Component::getString).anyMatch(line -> line.startsWith(words));
    }

    /* The button on the screen saying those words. */
    private static Button button(final String words) {
        return manual().children().stream().filter(child -> child instanceof Button button
                        && button.getMessage().getString().equals(words)).map(Button.class::cast).findFirst()
                .orElseThrow(() -> new IllegalStateException("no button " + words));
    }

    /* The tip of the upgrade's page among the notifications kept, the one that opens the press's entry. */
    private static Optional<Notification> tip() {
        return NotificationsClient.board().history().stream().map(entry -> entry.item())
                .filter(notification -> notification.kind().equals(GuideTips.TIP.id())
                        && notification.action().filter(action -> action instanceof NotificationAction.OpenManual open
                        && open.entry().equals(TestGuide.PRESS)).isPresent()).findFirst();
    }

    /* Takes the frame and its wall away on the server, without waiting for it: a cleanup runs on the client. */
    private static void clearFrame(final ClientTestContext ctx) {
        final MinecraftServer server = ctx.server();
        server.execute(() -> {
            final ServerLevel level = server.overworld();
            level.getEntitiesOfClass(ItemFrame.class, new AABB(ctx.abs(FRAME)).inflate(1)).forEach(Entity::discard);
            level.setBlockAndUpdate(ctx.abs(WALL), Blocks.AIR.defaultBlockState());
        });
    }

    private static void clearLectern(final ClientTestContext ctx) {
        final MinecraftServer server = ctx.server();
        server.execute(() -> server.overworld().setBlockAndUpdate(ctx.abs(LECTERN), Blocks.AIR.defaultBlockState()));
    }
}
