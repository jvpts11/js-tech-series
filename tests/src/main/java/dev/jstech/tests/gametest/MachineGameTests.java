/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dev.jstech.core.look.LookTexts;
import dev.jstech.core.machine.ProcessingRecipe;
import dev.jstech.core.machine.ProcessingRecipeBuilder;
import dev.jstech.core.text.Text;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestPress;
import dev.jstech.tests.TestPressBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Core's processing machines and recipes: a recipe of several inputs by the count and a fluid by the millibucket,
 * read from its file, worked into two outputs and a fluid; an upgrade that makes the work faster and dearer; a full
 * output that stops the work; and the builder's recipes read back as written.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MachineGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos PRESS = new BlockPos(1, 2, 1);
    /** The recipe's own time, in ticks, as its file gives it. */
    private static final int RECIPE_TICKS = 10;

    private MachineGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void press_worksItsInputsIntoItsOutputs(final GameTestHelper helper) {
        final TestPressBlockEntity press = placeLoaded(helper);
        helper.succeedWhen(() -> {
            helper.assertTrue(press.getInventory().getStackInSlot(2).is(Items.IRON_BLOCK),
                    "the first output holds the iron block");
            helper.assertTrue(press.getInventory().getStackInSlot(3).is(Items.CHARCOAL),
                    "the second the charcoal");
            helper.assertTrue(press.getInventory().getStackInSlot(0).getCount() == 2,
                    "two of the four ingots were taken");
            helper.assertTrue(press.inputTank(0).getFluidAmount() == 750, "250 mB of the water was taken");
            helper.assertTrue(press.outputTank(0).getFluid().is(Fluids.LAVA)
                    && press.outputTank(0).getFluidAmount() == 100, "and 100 mB of lava put out");
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void upgrade_makesTheWorkFasterAndDearer(final GameTestHelper helper) {
        final TestPressBlockEntity press = placeLoaded(helper);
        press.getInventory().setStackInSlot(TestPress.LAYOUT.firstUpgrade(), new ItemStack(TestPress.UPGRADE.get()));
        final int energy = press.getEnergy().getEnergyStored();
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(press.getMaxProgress() == RECIPE_TICKS / 2,
                    "one speed upgrade halves the time; got " + press.getMaxProgress());
            final int spent = energy - press.getEnergy().getEnergyStored();
            // Six energy a tick, the machine's four times one and a half, over the ticks so far.
            helper.assertTrue(spent > 0 && spent % 6 == 0, "each tick spends six; spent " + spent);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void press_stopsWhenItsOutputHasNoRoom(final GameTestHelper helper) {
        final TestPressBlockEntity press = placeLoaded(helper);
        press.getInventory().setStackInSlot(2, new ItemStack(Items.DIRT, 64));
        press.getInventory().setStackInSlot(3, new ItemStack(Items.DIRT, 64));
        helper.runAfterDelay(RECIPE_TICKS * 2, () -> {
            helper.assertTrue(press.getProgress() == 0 && press.getInventory().getStackInSlot(0).getCount() == 4,
                    "with nowhere to put its work it takes nothing");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 60)
    public static void press_saysWhatItIsDoingToWhoeverLooks(final GameTestHelper helper) {
        helper.setBlock(PRESS, TestPress.BLOCK.get());
        final TestPressBlockEntity idle = helper.getBlockEntity(PRESS);
        helper.assertTrue(said(idle).equals(LookTexts.IDLE.text()), "an empty press stands idle");
        final TestPressBlockEntity press = placeLoaded(helper);
        helper.runAfterDelay(RECIPE_TICKS / 2, () -> {
            final Text line = said(press);
            helper.assertTrue(line instanceof Text.Translated working && working.key().equals(LookTexts.WORKING),
                    "a press at work says how far along it is; it says " + line);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void builder_writesWhatTheFileReadsBack(final GameTestHelper helper) {
        final ProcessingRecipe built = ProcessingRecipeBuilder.of(TestPress.PRESSING).input(Items.GOLD_INGOT, 3)
                .fluidInput(Fluids.WATER, 100).output(Items.GOLD_BLOCK, 1).ticks(40).energyPerTick(12).build();
        final Codec<ProcessingRecipe> codec = TestPress.PRESSING.serializer().codec().codec();
        final JsonElement file = codec.encodeStart(JsonOps.INSTANCE, built).getOrThrow();
        final ProcessingRecipe back = codec.parse(JsonOps.INSTANCE, file).getOrThrow();
        helper.assertTrue(back.inputs().getFirst().count() == 3 && back.ticks() == 40 && back.energyPerTick() == 12
                        && back.fluidInputs().getFirst().amount() == 100
                        && back.outputs().getFirst().is(Items.GOLD_BLOCK),
                "the recipe comes back as written; got " + file);
        helper.assertTrue(codec.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"inputs\": []}")).isError(),
                "a recipe that takes nothing and makes nothing is refused");
        helper.succeed();
    }

    /* The one line the press says to a player who looks at it. */
    private static Text said(final TestPressBlockEntity press) {
        final List<Text> lines = new ArrayList<>();
        press.describe(lines, false);
        return lines.getFirst();
    }

    /* The press placed with what its recipe takes, twice over, and energy enough. */
    private static TestPressBlockEntity placeLoaded(final GameTestHelper helper) {
        helper.setBlock(PRESS, TestPress.BLOCK.get());
        final TestPressBlockEntity press = helper.getBlockEntity(PRESS);
        press.getInventory().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 4));
        press.getInventory().setStackInSlot(1, new ItemStack(Items.COAL, 2));
        press.inputTank(0).fill(new FluidStack(Fluids.WATER, 1_000), IFluidHandler.FluidAction.EXECUTE);
        press.getEnergy().setEnergyStored(50_000);
        return press;
    }
}
