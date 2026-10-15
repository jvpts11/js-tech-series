/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import com.mojang.authlib.GameProfile;
import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.cable.CableEnd;
import dev.jstech.core.cable.CableEndMenu;
import dev.jstech.core.cable.CableType;
import dev.jstech.core.cable.EndFilters;
import dev.jstech.core.cable.Wire;
import dev.jstech.core.grid.EndMode;
import dev.jstech.core.grid.GridKind;
import dev.jstech.core.inventory.ItemFilter;
import dev.jstech.core.jstechseries.tools.SharedTools;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestCableTypes;
import dev.jstech.tests.testkit.TestCables;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.Nullable;

/**
 * The screen of a line's end, through its menu on the server: each key sets its part of the end, a number typed is
 * held to its range, a bucket clicked onto a filter slot names its water, a right-click goes round the water, its tags
 * and its mod, a scroll sets how much to keep, a stack a pipe does not carry is refused, and the Basic Wrench opens it
 * when its player sneaks.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class EndSettingsGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos AT = new BlockPos(2, 2, 2);

    private EndSettingsGameTests() {
    }

    @GameTest(template = ARENA)
    public static void keys_setEveryPartOfTheEnd(final GameTestHelper helper) {
        final CableEndMenu menu = menu(helper, TestCableTypes.PLAIN_PIPE, "end_keys");
        final FakePlayer player = player(helper, "end_keys");
        menu.clickMenuButton(player, CableEndMenu.FIRST_MODE + 1);
        menu.clickMenuButton(player, CableEndMenu.PRIORITY_UP_TEN);
        menu.clickMenuButton(player, CableEndMenu.PRIORITY_DOWN);
        menu.clickMenuButton(player, CableEndMenu.RATE_UP);
        menu.clickMenuButton(player, CableEndMenu.FIRST_CHANNEL + 5);
        menu.clickMenuButton(player, CableEndMenu.FILTER_MODE);
        final CableEnd end = end(helper, TestCableTypes.PLAIN_PIPE);
        helper.assertTrue(end.mode() == EndMode.PUSH, "the second key sets push");
        helper.assertTrue(end.priority() == 9, "ten up and one down: " + end.priority());
        helper.assertTrue(end.rate() == CableEndMenu.FLUID_STEP, "a pipe's rate goes up a step: " + end.rate());
        helper.assertTrue(end.channel() == 5, "the sixth key is channel 5");
        helper.assertTrue(end.filter().mode() == ItemFilter.Mode.ONLY, "the filter's key turns all-but to only");
        menu.clickMenuButton(player, CableEndMenu.RATE_DOWN_TEN);
        helper.assertTrue(end(helper, TestCableTypes.PLAIN_PIPE).rate() == 0, "and the rate stops at nothing");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void typed_isHeldToItsRange(final GameTestHelper helper) {
        final CableEndMenu menu = menu(helper, TestCableTypes.PLAIN_PIPE, "end_typed");
        menu.typed(true, 250L);
        menu.typed(false, 500L);
        CableEnd end = end(helper, TestCableTypes.PLAIN_PIPE);
        helper.assertTrue(end.rate() == 250L, "the rate typed is set");
        helper.assertTrue(end.priority() == CableEnd.HIGHEST, "a priority past the highest is the highest");
        menu.typed(true, -5L);
        menu.typed(false, -500L);
        end = end(helper, TestCableTypes.PLAIN_PIPE);
        helper.assertTrue(end.rate() == 0L && end.priority() == CableEnd.LOWEST, "and below, the lowest");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void ghost_namesTheWaterOfABucketAndGoesRoundItsKinds(final GameTestHelper helper) {
        final CableEndMenu menu = menu(helper, TestCableTypes.PLAIN_PIPE, "end_ghost");
        final FakePlayer player = player(helper, "end_ghost");
        menu.setCarried(new ItemStack(Items.WATER_BUCKET));
        menu.clickMenuButton(player, CableEndMenu.FIRST_GHOST + 4);
        List<ItemFilter.Rule> rules = rules(helper);
        helper.assertTrue(rules.equals(List.of(new ItemFilter.Fuzzy("minecraft:water", 0L))),
                "a bucket names its water, in the first free slot: " + rules);
        helper.assertTrue(end(helper, TestCableTypes.PLAIN_PIPE).filter().mode() == ItemFilter.Mode.ONLY,
                "the first rule of an empty filter names what passes");
        menu.clickMenuButton(player, CableEndMenu.FIRST_MORE);
        menu.clickMenuButton(player, CableEndMenu.FIRST_MUCH_MORE);
        helper.assertTrue(rules(helper).get(0).amount() == CableEndMenu.FLUID_KEPT_STEP
                + CableEndMenu.FLUID_KEPT_MANY, "a scroll and a long scroll keep more");
        final int kinds = EndFilters.kinds(GridKind.FLUID, "minecraft:water").size();
        helper.assertTrue(kinds >= 3, "water has its own kind, a tag at least and its mod");
        menu.clickMenuButton(player, CableEndMenu.FIRST_CYCLE);
        rules = rules(helper);
        helper.assertTrue(rules.get(0) instanceof ItemFilter.Tag tag && tag.amount() == CableEndMenu.FLUID_KEPT_STEP
                + CableEndMenu.FLUID_KEPT_MANY, "a right-click turns it into a tag, keeping the amount: " + rules);
        for (int k = 1; k < kinds - 1; k++) {
            menu.clickMenuButton(player, CableEndMenu.FIRST_CYCLE);
        }
        helper.assertTrue(rules(helper).get(0) instanceof ItemFilter.Mod mod && "minecraft".equals(mod.mod()),
                "then its mod");
        menu.clickMenuButton(player, CableEndMenu.FIRST_CYCLE);
        helper.assertTrue(rules(helper).get(0) instanceof ItemFilter.Fuzzy fuzzy
                && "minecraft:water".equals(fuzzy.item()), "and back to the water");
        menu.setCarried(new ItemStack(Items.STICK));
        menu.clickMenuButton(player, CableEndMenu.FIRST_GHOST + 1);
        helper.assertTrue(rules(helper).size() == 1, "a stick holds nothing a pipe carries");
        menu.setCarried(ItemStack.EMPTY);
        menu.clickMenuButton(player, CableEndMenu.FIRST_GHOST);
        helper.assertTrue(rules(helper).isEmpty(), "clicked with nothing, the slot is cleared");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void ghost_namesTheItemOnALineOfItems(final GameTestHelper helper) {
        final CableEndMenu menu = menu(helper, TestCableTypes.ITEM_LINE, "end_items");
        final FakePlayer player = player(helper, "end_items");
        menu.setCarried(new ItemStack(Items.IRON_INGOT, 12));
        menu.clickMenuButton(player, CableEndMenu.FIRST_GHOST);
        menu.clickMenuButton(player, CableEndMenu.FIRST_MUCH_MORE);
        menu.clickMenuButton(player, CableEndMenu.FIRST_LESS);
        final CableEnd end = end(helper, TestCableTypes.ITEM_LINE);
        helper.assertTrue(end.filter().rules().equals(List.of(new ItemFilter.Fuzzy("minecraft:iron_ingot",
                CableEndMenu.ITEMS_KEPT_MANY - CableEndMenu.ITEMS_KEPT_STEP))), "the ingot, keeping fifteen: "
                + end.filter().rules());
        helper.assertTrue(menu.getCarried().getCount() == 12, "and the stack stays in the player's hand");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void wrench_opensTheScreenWhenSneaking(final GameTestHelper helper) {
        TestCables.lay(helper, AT, TestCableTypes.PLAIN_PIPE);
        final BlockPos cable = helper.absolutePos(AT);
        final ScreenWatcher player = new ScreenWatcher(helper.getLevel());
        player.moveTo(cable.getX() - 1.5, cable.getY() + 0.5 - player.getEyeHeight(), cable.getZ() + 0.5, -90.0F,
                0.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(SharedTools.WRENCH.get()));
        player.setShiftKeyDown(true);
        final Vec3 eastArm = new Vec3(cable.getX() + 0.85, cable.getY() + 0.5, cable.getZ() + 0.5);
        SharedTools.WRENCH.get().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(eastArm, Direction.WEST, cable, false)));
        helper.assertTrue(end(helper, TestCableTypes.PLAIN_PIPE).mode() == EndMode.NORMAL,
                "sneaking, the wrench turns no mode");
        helper.assertTrue(player.opened != null && player.opened.createMenu(0, player.getInventory(), player)
                instanceof CableEndMenu opened && opened.face() == Direction.EAST, "it opens the east end's screen");
        helper.succeed();
    }

    /* A pipe, or a line, at {@code AT}, and the menu of its east end, as the server makes it for a player. */
    private static CableEndMenu menu(final GameTestHelper helper, final Supplier<CableType> type, final String who) {
        TestCables.lay(helper, AT, type);
        final CableBlockEntity cable = TestCables.cable(helper, AT);
        final Wire wire = cable.wire(type.get());
        helper.assertTrue(wire != null, "the wire is laid");
        return new CableEndMenu(0, player(helper, who).getInventory(), cable, wire.slot(), Direction.EAST);
    }

    private static FakePlayer player(final GameTestHelper helper, final String name) {
        return FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(
                StandardCharsets.UTF_8)), name));
    }

    private static CableEnd end(final GameTestHelper helper, final Supplier<CableType> type) {
        return TestCables.cable(helper, AT).end(type.get(), Direction.EAST);
    }

    private static List<ItemFilter.Rule> rules(final GameTestHelper helper) {
        return end(helper, TestCableTypes.PLAIN_PIPE).filter().rules();
    }

    /* A fake player who keeps what the wrench would open, which a fake player otherwise never opens. */
    private static final class ScreenWatcher extends FakePlayer {

        @Nullable
        private MenuProvider opened;

        ScreenWatcher(final ServerLevel level) {
            super(level, new GameProfile(UUID.randomUUID(), "end_sneak"));
        }

        @Override
        public OptionalInt openMenu(@Nullable final MenuProvider provider,
                                    final Consumer<RegistryFriendlyByteBuf> extraData) {
            this.opened = provider;
            return OptionalInt.empty();
        }
    }
}
