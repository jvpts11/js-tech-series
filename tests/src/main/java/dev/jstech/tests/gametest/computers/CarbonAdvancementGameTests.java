/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.abandoned.AbandonedSoftware;
import dev.jstech.computers.advancement.HardwareMilestones;
import dev.jstech.computers.advancement.JscEvents;
import dev.jstech.computers.chain.machine.ChainMachines;
import dev.jstech.computers.machine.DiscCopies;
import dev.jstech.computers.os.media.InstallerStacks;
import dev.jstech.computers.teracoin.FoundBlock;
import dev.jstech.computers.teracoin.TeracoinAwards;
import dev.jstech.computers.teracoin.TeracoinState;
import dev.jstech.computers.teracoin.Trc;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.tests.JsTests;
import dev.jstech.testkit.gametest.GameTestPlayers;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Carbon front's advancements (computers/docs/design/advancements.md): the two new tabs and the four of before at
 * their counts, the hidden ones shown only once earned; the chain's machines counted by their family, a station's layer
 * by its era; an installer found in a building marked, a copy of it not; and Teracoin's wallets, gifts and holding,
 * which the chain alone cannot tell.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class CarbonAdvancementGameTests {

    private static final String ARENA = "empty";
    private static final long DAY = 24_000L;

    private CarbonAdvancementGameTests() {
    }

    @GameTest(template = ARENA)
    public static void tabs_holdTheirCountsAndShowTheHiddenOnlyOnceEarned(final GameTestHelper helper) {
        final Map<String, Long> perTab = helper.getLevel().getServer().getAdvancements().getAllAdvancements()
                .stream().map(AdvancementHolder::id).filter(id -> JsComputers.MODID.equals(id.getNamespace())
                        && !id.getPath().startsWith("recipes/"))
                .collect(Collectors.groupingBy(id -> id.getPath().substring(0, id.getPath().indexOf('/')),
                        Collectors.counting()));
        helper.assertTrue(perTab.equals(Map.of("fab", 14L, "hardware", 24L, "systems", 36L, "networks", 22L,
                "sigma", 14L, "teracoin", 9L)), "119 in six tabs: " + perTab);
        for (final String hidden : List.of("hardware/magic_smoke", "hardware/cpu_over_temperature",
                "systems/not_properly_shut_down", "teracoin/the_halvening")) {
            helper.assertTrue(display(helper, hidden).isHidden(), hidden + " stays hidden until earned");
        }
        helper.assertTrue(display(helper, "fab/root").getBackground().map(ResourceLocation::getPath)
                .orElse("").endsWith("white_concrete.png"), "Fab on white concrete");
        helper.assertTrue(display(helper, "teracoin/root").getBackground().map(ResourceLocation::getPath)
                .orElse("").endsWith("gold_block.png"), "Teracoin on gold");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void chain_countsEachMachineByItsFamilyAndEachLayerByItsEra(final GameTestHelper helper) {
        helper.assertTrue(ChainMachines.LITHOGRAPHY.equals(family("lithography_arf"))
                && ChainMachines.ASSEMBLY.equals(family("assembly_standard"))
                && "siemens_reactor".equals(family("siemens_reactor")), "a station or a line counts as its kind");
        final ServerPlayer player = GameTestPlayers.join(helper, "fabber");
        try {
            JscEvents.award(player, JscEvents.CHAIN_FORMED, "siemens_reactor");
            helper.assertTrue(done(player, "fab/bare_metal") && !done(player, "fab/bunny_suit"),
                    "any machine formed is Bare Metal, and only a station is Bunny Suit");
            JscEvents.award(player, JscEvents.CHAIN_FORMED, ChainMachines.LITHOGRAPHY);
            helper.assertTrue(done(player, "fab/bunny_suit"), "a station formed");
            for (final Era era : new Era[] {HardwareEra.VINTAGE, HardwareEra.LEGACY, HardwareEra.TRANSITION,
                    HardwareEra.STANDARD}) {
                JscEvents.award(player, JscEvents.LAYER_EXPOSED, HardwareMilestones.eraDetail(era));
            }
            helper.assertTrue(!done(player, "fab/die_shrink") && !done(player, "fab/thirteen_point_five_nanometres"),
                    "four eras of five are not every era, and none is extreme ultraviolet");
            JscEvents.award(player, JscEvents.LAYER_EXPOSED, HardwareMilestones.eraDetail(HardwareEra.ADVANCED));
            helper.assertTrue(done(player, "fab/die_shrink") && done(player, "fab/thirteen_point_five_nanometres"),
                    "the Advanced layer completes both");
        } finally {
            GameTestPlayers.leave(player);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void found_aBuildingsInstallerIsMarkedAndACopyIsNot(final GameTestHelper helper) {
        final List<ItemStack> found = AbandonedSoftware.graphicalSystem(HardwareEra.STANDARD);
        helper.assertTrue(!found.isEmpty() && found.stream().allMatch(InstallerStacks::isFound),
                "a building's way to a graphical system is marked found");
        final ItemStack copy = new ItemStack(found.getFirst().getItem());
        DiscCopies.copyOnto(found.getFirst(), copy);
        helper.assertTrue(!InstallerStacks.isFound(copy), "a copy of it was never found in a building");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void teracoin_aWalletAGiftAndSevenDaysHeld(final GameTestHelper helper) {
        final ServerPlayer giver = GameTestPlayers.join(helper, "giver");
        final ServerPlayer taker = GameTestPlayers.join(helper, "taker");
        try {
            final String mine = "TRC" + UUID.randomUUID().toString().replace("-", "");
            final String theirs = "TRC" + UUID.randomUUID().toString().replace("-", "");
            TeracoinAwards.walletMade(giver, mine);
            TeracoinAwards.walletMade(taker, theirs);
            helper.assertTrue(done(giver, "teracoin/root"), "a wallet made");
            TeracoinAwards.sent(giver, mine, mine);
            helper.assertTrue(!done(giver, "teracoin/pizza_day"), "sending to one's own wallet is no gift");
            TeracoinAwards.sent(giver, mine, theirs);
            helper.assertTrue(done(giver, "teracoin/pizza_day"), "sending to another player's is");
            final TeracoinState state = TeracoinState.of(helper.getLevel().getServer());
            final long now = helper.getLevel().getGameTime();
            state.chain().creditAlone(new FoundBlock(state.chain().height(), now, 0L, Trc.of(1_000), 1L, "test",
                    "test", theirs, "", false));
            TeracoinAwards.lookAtHolders(helper.getLevel().getServer(), now);
            helper.assertTrue(!done(taker, "teracoin/hodl"), "holding is timed from now");
            TeracoinAwards.lookAtHolders(helper.getLevel().getServer(), now + 7 * DAY);
            helper.assertTrue(done(taker, "teracoin/hodl"), "seven days held with nothing sent");
        } finally {
            GameTestPlayers.leave(giver);
            GameTestPlayers.leave(taker);
        }
        helper.succeed();
    }

    private static String family(final String machine) {
        return ChainMachines.familyOf(ChainMachines.byId(machine)
                .orElseThrow(() -> new IllegalStateException("no chain machine " + machine)));
    }

    private static DisplayInfo display(final GameTestHelper helper, final String path) {
        final AdvancementHolder holder = helper.getLevel().getServer().getAdvancements()
                .get(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, path));
        if (holder == null || holder.value().display().isEmpty()) {
            throw new IllegalStateException("no advancement shown as " + path);
        }
        return holder.value().display().get();
    }

    private static boolean done(final ServerPlayer player, final String path) {
        final AdvancementHolder holder = player.server.getAdvancements()
                .get(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, path));
        if (holder == null) {
            throw new IllegalStateException("no advancement " + path);
        }
        return player.getAdvancements().getOrStartProgress(holder).isDone();
    }
}
