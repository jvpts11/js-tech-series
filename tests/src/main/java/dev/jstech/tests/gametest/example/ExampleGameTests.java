/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.example;

import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.signal.Signals;
import dev.jstech.example.ExampleContent;
import dev.jstech.example.ExampleSettings;
import dev.jstech.example.ExampleState;
import dev.jstech.example.machine.BatteryBlockEntity;
import dev.jstech.example.machine.CrusherBlockEntity;
import dev.jstech.example.machine.GeneratorBlockEntity;
import dev.jstech.example.machine.TankBlockEntity;
import dev.jstech.example.part.LevelSensorPart;
import dev.jstech.example.screen.LogbookMenu;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestFluidEnds;
import dev.jstech.tests.testkit.TestCables;
import dev.jstech.testkit.gametest.GameTestPlayers;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * J's Example in a real world: the generator burns fuel into energy as its settings say and counts it for the
 * server, the energy cable carries it to the battery, the crusher works the recipe its data file declares, the pipe
 * fills the tank with oil and a bucket of oil fills it too, the level sensor on a cable reads the battery beside it,
 * and the logbook counts each player's openings and starts a count over from its screen's button.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ExampleGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos FIRST = new BlockPos(1, 2, 2);
    private static final BlockPos LAST = new BlockPos(4, 2, 2);
    private static final List<BlockPos> RUN = List.of(new BlockPos(2, 2, 2), new BlockPos(3, 2, 2));

    private ExampleGameTests() {
    }

    @GameTest(template = ARENA)
    public static void generator_burnsFuelIntoAsMuchEnergyAsItsSettingSays(final GameTestHelper helper) {
        final GeneratorBlockEntity generator = place(helper, FIRST, ExampleContent.GENERATOR.get(),
                GeneratorBlockEntity.class);
        generator.getInventory().setStackInSlot(GeneratorBlockEntity.FUEL, new ItemStack(Items.COAL));
        final int output = ExampleSettings.generatorOutput();
        helper.assertTrue(output == ExampleSettings.GENERATOR_OUTPUT.defaultValue(), "the setting is as shipped");

        helper.succeedWhen(() -> {
            final int stored = generator.getEnergy().getEnergyStored();
            helper.assertTrue(generator.burning() > 0, "the coal burns");
            helper.assertTrue(stored > 0 && stored % output == 0, "it makes " + output + " a tick: " + stored);
            helper.assertTrue(generator.getInventory().getStackInSlot(GeneratorBlockEntity.FUEL).isEmpty(),
                    "the piece burning left the slot");
        });
    }

    @GameTest(template = ARENA)
    public static void generator_countsWhatItMakesForTheWholeServer(final GameTestHelper helper) {
        final long before = ExampleState.ENERGY_MADE.get(helper.getLevel().getServer());
        final GeneratorBlockEntity generator = place(helper, FIRST, ExampleContent.GENERATOR.get(),
                GeneratorBlockEntity.class);
        generator.getInventory().setStackInSlot(GeneratorBlockEntity.FUEL, new ItemStack(Items.COAL));

        helper.succeedWhen(() -> helper.assertTrue(ExampleState.ENERGY_MADE.get(helper.getLevel().getServer())
                >= before + generator.getEnergy().getEnergyStored() && generator.getEnergy().getEnergyStored() > 0,
                "the server's count grows by what it made"));
    }

    @GameTest(template = ARENA)
    public static void energyCable_carriesTheGeneratorsEnergyToTheBattery(final GameTestHelper helper) {
        final GeneratorBlockEntity generator = place(helper, FIRST, ExampleContent.GENERATOR.get(),
                GeneratorBlockEntity.class);
        final BatteryBlockEntity battery = place(helper, LAST, ExampleContent.BATTERY.get(), BatteryBlockEntity.class);
        generator.getEnergy().setEnergyStored(GeneratorBlockEntity.CAPACITY);
        for (final BlockPos at : RUN) {
            TestCables.lay(helper, at, ExampleContent.ENERGY_CABLE);
        }

        helper.succeedWhen(() -> {
            final int got = battery.getEnergy().getEnergyStored();
            helper.assertTrue(got > 0, "energy reaches the battery");
            helper.assertTrue(GeneratorBlockEntity.CAPACITY - generator.getEnergy().getEnergyStored() == got,
                    "every FE the generator gave arrived");
        });
    }

    @GameTest(template = ARENA)
    public static void crusher_crushesCobblestoneByItsRecipeFile(final GameTestHelper helper) {
        final ResourceLocation recipe = ResourceLocation.fromNamespaceAndPath("jsexample",
                "crushing/cobblestone_to_gravel");
        helper.assertTrue(ExampleContent.CRUSHING.all(helper.getLevel()).stream()
                .anyMatch(holder -> holder.id().equals(recipe)), "the recipe is read from its file");
        final CrusherBlockEntity crusher = place(helper, FIRST, ExampleContent.CRUSHER.get(),
                CrusherBlockEntity.class);
        crusher.getEnergy().setEnergyStored(CrusherBlockEntity.CAPACITY);
        crusher.getInventory().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 2));

        helper.succeedWhen(() -> helper.assertTrue(crusher.getInventory().getStackInSlot(1).is(Items.GRAVEL),
                "the cobblestone comes out as gravel"));
    }

    @GameTest(template = ARENA)
    public static void fluidPipe_fillsTheTankWithOil(final GameTestHelper helper) {
        TestFluidEnds.clear(helper.getLevel(), helper.absolutePos(FIRST));
        final FluidTank output = TestFluidEnds.tankAt(helper.getLevel(), helper.absolutePos(FIRST));
        output.fill(ExampleContent.OIL.stack(2_000), IFluidHandler.FluidAction.EXECUTE);
        helper.setBlock(FIRST, Blocks.CHISELED_TUFF);
        final TankBlockEntity tank = place(helper, LAST, ExampleContent.TANK.get(), TankBlockEntity.class);
        for (final BlockPos at : RUN) {
            TestCables.lay(helper, at, ExampleContent.FLUID_PIPE);
        }

        helper.succeedWhen(() -> {
            final FluidStack held = tank.tank().getFluid();
            helper.assertTrue(held.is(ExampleContent.OIL.source()) && held.getAmount() > 0, "oil reaches the tank");
            helper.assertTrue(output.getFluidAmount() + held.getAmount() == 2_000, "none is lost");
        });
    }

    @GameTest(template = ARENA)
    public static void tank_isFilledByABucketOfOil(final GameTestHelper helper) {
        final TankBlockEntity tank = place(helper, FIRST, ExampleContent.TANK.get(), TankBlockEntity.class);
        final ServerPlayer player = GameTestPlayers.join(helper, "tanker");
        try {
            player.setItemInHand(InteractionHand.MAIN_HAND,
                    new ItemStack(ExampleContent.OIL.bucket().orElseThrow().get()));
            final BlockPos at = helper.absolutePos(FIRST);
            helper.getLevel().getBlockState(at).useItemOn(player.getMainHandItem(), helper.getLevel(), player,
                    InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(at), Direction.UP, at, false));
            helper.assertTrue(tank.tank().getFluid().is(ExampleContent.OIL.source())
                    && tank.tank().getFluidAmount() == 1_000, "the bucket's oil is in the tank");
            helper.assertTrue(player.getMainHandItem().is(Items.BUCKET), "and the bucket is empty");
        } finally {
            GameTestPlayers.leave(player);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void levelSensor_onACableReadsHowFullTheBatteryBesideItIs(final GameTestHelper helper) {
        final BatteryBlockEntity battery = place(helper, RUN.get(1), ExampleContent.BATTERY.get(),
                BatteryBlockEntity.class);
        battery.getEnergy().setEnergyStored(BatteryBlockEntity.CAPACITY / 2);
        final CableBlockEntity cable = TestCables.lay(helper, RUN.get(0), ExampleContent.ENERGY_CABLE);
        final LevelSensorPart sensor = new LevelSensorPart();
        helper.assertTrue(cable.addPart(Direction.EAST, sensor), "the sensor mounts on the cable's face");

        final BlockPos at = helper.absolutePos(RUN.get(0));
        helper.succeedWhen(() -> {
            helper.assertTrue(sensor.signal() == LevelSensorPart.FULL / 2,
                    "half full reads " + LevelSensorPart.FULL / 2 + ": " + sensor.signal());
            final int read = Signals.strengthAt(helper.getLevel(), at, Direction.EAST);
            helper.assertTrue(read == LevelSensorPart.FULL / 2,
                    "and a reader of sensors reads it on the cable's face: " + read);
        });
    }

    @GameTest(template = ARENA)
    public static void logbook_countsEachOpeningForItsPlayerAndStartsOverFromItsButton(final GameTestHelper helper) {
        // Logged in over a connection, since opening a screen sends it to the player's game.
        final ServerPlayer player = GameTestPlayers.connected(helper, "reader");
        try {
            final int before = ExampleState.LOGBOOK_OPENED.get(player.server, player.getUUID());
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ExampleContent.LOGBOOK.get()));
            ExampleContent.LOGBOOK.get().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(ExampleState.LOGBOOK_OPENED.get(player.server, player.getUUID()) == before + 1,
                    "opening it counts one more for its player");
            helper.assertTrue(player.containerMenu instanceof LogbookMenu, "and opens its screen's menu");
            player.containerMenu.clickMenuButton(player, LogbookMenu.START_OVER);
            helper.assertTrue(ExampleState.LOGBOOK_OPENED.get(player.server, player.getUUID()) == 0,
                    "the button starts the count over");
        } finally {
            GameTestPlayers.leave(player);
        }
        helper.succeed();
    }

    /* Sets the block and hands back its block entity, failing the test when it is not of the type. */
    private static <T extends BlockEntity> T place(final GameTestHelper helper, final BlockPos at,
                                                   final Block block,
                                                   final Class<T> type) {
        helper.setBlock(at, block);
        final BlockEntity found = helper.getBlockEntity(at);
        if (!type.isInstance(found)) {
            helper.fail("no " + type.getSimpleName() + " at " + at.toShortString());
        }
        return type.cast(found);
    }
}
