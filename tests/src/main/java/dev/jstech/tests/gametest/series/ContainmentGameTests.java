/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.series;

import dev.jstech.core.jstechseries.Antimatter;
import dev.jstech.core.jstechseries.ExoticMatter;
import dev.jstech.core.jstechseries.containment.ContainmentBlockEntity;
import dev.jstech.core.jstechseries.containment.SharedContainment;
import dev.jstech.core.jstechseries.energy.BatteryBankBlockEntity;
import dev.jstech.core.jstechseries.energy.SharedEnergy;
import dev.jstech.core.matter.IMatterHandler;
import dev.jstech.core.matter.MatterCapabilities;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestCables;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The containment of exotic matter and the Antimatter Trap, J's Core's shared blocks: each holds its matter while its
 * line pays its upkeep, takes nothing and lets nothing out without energy, keeps what it holds in the item it drops and
 * holds it again when placed; the trap keeps antimatter alone, the containment every other exotic form.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ContainmentGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos BATTERY = new BlockPos(1, 2, 2);
    private static final BlockPos CABLE = new BlockPos(2, 2, 2);
    private static final BlockPos CONTAINMENT = new BlockPos(3, 2, 2);
    private static final BlockPos ELSEWHERE = new BlockPos(5, 2, 2);

    private ContainmentGameTests() {
    }

    @GameTest(template = ARENA)
    public static void containment_holdsItsMatterWhileItsLinePaysTheUpkeep(final GameTestHelper helper) {
        final ContainmentBlockEntity containment = place(helper, SharedContainment.EXOTIC.get(), CONTAINMENT);
        final BatteryBankBlockEntity battery = battery(helper);
        battery.store().setStored(SharedEnergy.BATTERY_CAPACITY);
        TestCables.lay(helper, CABLE, SharedEnergy.INSULATED_COPPER_CABLE);
        final IMatterHandler matter = helper.getLevel().getCapability(MatterCapabilities.EXOTIC,
                helper.absolutePos(CONTAINMENT), Direction.UP);
        helper.assertTrue(matter != null, "it offers its matter to the lines of exotic matter");
        helper.succeedWhen(() -> {
            helper.assertTrue(containment.containment().powered(), "its line powers it");
            helper.assertTrue(matter.insert(new MatterStack(ExoticMatter.NEUTRONIUM, 100L), false) > 0
                    || containment.containment().held().isPresent(), "so it takes neutronium");
            helper.assertTrue(battery.store().stored() < SharedEnergy.BATTERY_CAPACITY,
                    "its energy coming from the battery");
        });
    }

    @GameTest(template = ARENA)
    public static void withoutEnergy_itTakesNothingAndLetsNothingOut(final GameTestHelper helper) {
        final ContainmentBlockEntity containment = place(helper, SharedContainment.EXOTIC.get(), CONTAINMENT);
        containment.containment().set(new MatterStack(ExoticMatter.STRANGE_MATTER, 400L));
        helper.runAfterDelay(2, () -> {
            final IMatterHandler matter = helper.getLevel().getCapability(MatterCapabilities.EXOTIC,
                    helper.absolutePos(CONTAINMENT), Direction.NORTH);
            helper.assertTrue(matter != null && !containment.containment().powered(), "with no energy it is sealed");
            helper.assertTrue(matter.extract(ExoticMatter.STRANGE_MATTER, 400L, false) == 0L,
                    "it lets nothing out");
            helper.assertTrue(matter.insert(new MatterStack(ExoticMatter.STRANGE_MATTER, 1L), false) == 0L,
                    "and takes nothing in");
            helper.assertTrue(containment.containment().held().map(MatterStack::amount).orElse(0L) == 400L,
                    "and loses nothing");
            containment.store().setStored(SharedContainment.STORE);
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(matter.extract(ExoticMatter.STRANGE_MATTER, 100L, false) == 100L,
                        "with its energy back it gives again");
                helper.succeed();
            });
        });
    }

    @GameTest(template = ARENA)
    public static void trap_keepsAntimatterAloneAndTheContainmentEverythingElse(final GameTestHelper helper) {
        final ContainmentBlockEntity trap = place(helper, SharedContainment.ANTIMATTER_TRAP.get(), CONTAINMENT);
        final ContainmentBlockEntity containment = place(helper, SharedContainment.EXOTIC.get(), ELSEWHERE);
        trap.store().setStored(SharedContainment.STORE);
        containment.store().setStored(SharedContainment.STORE);
        helper.assertTrue(Antimatter.is(Antimatter.ANTIHYDROGEN) && !Antimatter.is(ExoticMatter.NEUTRONIUM),
                "antihydrogen is antimatter, neutronium is not");
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(trap.matter().insert(new MatterStack(ExoticMatter.NEUTRONIUM, 10L), false) == 0L,
                    "the trap refuses neutronium");
            helper.assertTrue(trap.matter().insert(new MatterStack(Antimatter.ANTIHYDROGEN, 10L), false) == 10L,
                    "and takes antihydrogen");
            helper.assertTrue(containment.matter().insert(new MatterStack(Antimatter.ANTIHYDROGEN, 10L), false)
                    == 0L, "the containment refuses antimatter");
            helper.assertTrue(containment.matter().insert(new MatterStack(ExoticMatter.NEUTRONIUM, 10L), false)
                    == 10L, "and takes neutronium");
            helper.assertTrue(trap.containment().upkeep() > containment.containment().upkeep(),
                    "the trap's colder vacuum spends more");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void broken_keepsItsMatterInItsItemAndHoldsItAgainWhenPlaced(final GameTestHelper helper) {
        final ContainmentBlockEntity trap = place(helper, SharedContainment.ANTIMATTER_TRAP.get(), CONTAINMENT);
        trap.containment().set(new MatterStack(Antimatter.ANTIHYDROGEN, 250L));
        final List<ItemStack> drops = Block.getDrops(helper.getBlockState(CONTAINMENT), helper.getLevel(),
                helper.absolutePos(CONTAINMENT), trap);
        helper.assertTrue(drops.size() == 1 && drops.get(0).is(SharedContainment.ANTIMATTER_TRAP.get().asItem()),
                "broken, it drops itself");
        final MatterStack kept = drops.get(0).get(SharedContainment.CONTAINED.get());
        helper.assertTrue(kept != null && kept.material() == Antimatter.ANTIHYDROGEN && kept.amount() == 250L,
                "with the antihydrogen it held");
        helper.setBlock(CONTAINMENT, Blocks.AIR);
        final ContainmentBlockEntity again = place(helper, SharedContainment.ANTIMATTER_TRAP.get(), ELSEWHERE);
        again.applyComponentsFromItemStack(drops.get(0));
        helper.assertTrue(again.containment().held().map(MatterStack::amount).orElse(0L) == 250L,
                "placed again, it holds it again");
        helper.succeed();
    }

    private static ContainmentBlockEntity place(final GameTestHelper helper, final Block block, final BlockPos at) {
        helper.setBlock(at, block);
        return helper.getBlockEntity(at);
    }

    private static BatteryBankBlockEntity battery(final GameTestHelper helper) {
        helper.setBlock(BATTERY, SharedEnergy.BATTERY.get());
        return helper.getBlockEntity(BATTERY);
    }
}
