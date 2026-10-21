/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import com.mojang.authlib.GameProfile;
import dev.jstech.core.energy.ItemEnergyStorage;
import dev.jstech.core.input.KeyActions;
import dev.jstech.core.item.ItemActions;
import dev.jstech.core.item.ItemStates;
import dev.jstech.core.jstechseries.SeriesMaterials;
import dev.jstech.core.material.MaterialProperties;
import dev.jstech.core.matter.IMatterHandler;
import dev.jstech.core.matter.MatterCapabilities;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.core.module.Modules;
import dev.jstech.core.tool.AreaMining;
import dev.jstech.core.tool.BuildingTools;
import dev.jstech.core.tool.MiningShape;
import dev.jstech.core.tool.ToolBalance;
import dev.jstech.core.tool.ToolHeat;
import dev.jstech.core.tool.Tools;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestFluids;
import dev.jstech.tests.TestMaterials;
import dev.jstech.tests.TestTools;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

/**
 * The Core's tools, through the test mod's: a drill mines the area of the face it breaks into and the vein of an ore,
 * leaves what is far harder or what it is not made for, and spends its energy for each block; out of it, it breaks no
 * faster than a hand and gets no drops a hand would not. A pick overheats and cools; a chainsaw burns its fuel and a
 * shovel its gas; a railgun fires let go charged, then waits; a wand lays lines and planes from the inventory, copies
 * a region and pastes it turned; a multitool's modules give it a vein, silk touch and reach; a hammer lasts by its tin;
 * and a key turns a magnet on in the inventory.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ToolGameTests {

    private static final String ARENA = "empty";
    private static final String BATCH = "jscore_tools";
    /* The test that lowers the server's largest copy runs alone, so no copy of another test meets it. */
    private static final String BALANCE_BATCH = "jscore_tools_balance";
    /* Long enough for a player's held items to count among their attributes. */
    private static final int EQUIPMENT_TICKS = 3;

    private ToolGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void drill_minesTheAreaOfTheFaceItBreaksInto(final GameTestHelper helper) {
        final ServerPlayer player = player(helper, "drill-area");
        final ItemStack drill = charged(new ItemStack(TestTools.DRILL.get()));
        ItemStates.setMode(drill, TestTools.AREA);
        player.setItemInHand(InteractionHand.MAIN_HAND, drill);
        wall(helper, Blocks.STONE.defaultBlockState(), 6);
        helper.setBlock(4, 3, 7, Blocks.STONE);
        final BlockPos middle = helper.absolutePos(new BlockPos(4, 3, 6));
        player.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(middle));
        player.gameMode.destroyBlock(middle);
        for (int x = 3; x <= 5; x++) {
            for (int y = 2; y <= 4; y++) {
                helper.assertBlockPresent(Blocks.AIR, x, y, 6);
            }
        }
        helper.assertBlockPresent(Blocks.STONE, 4, 3, 7);
        helper.assertTrue(energy(drill) == TestTools.DRILL_ENERGY - 9 * TestTools.DRILL_PER_BLOCK,
                "the drill spent its energy for each of the nine blocks: " + energy(drill));
        leave(helper, player);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void drill_leavesWhatIsFarHarderOrWhatItIsNotMadeFor(final GameTestHelper helper) {
        final ServerPlayer player = player(helper, "drill-hard");
        final ItemStack drill = charged(new ItemStack(TestTools.DRILL.get()));
        ItemStates.setMode(drill, TestTools.AREA);
        player.setItemInHand(InteractionHand.MAIN_HAND, drill);
        wall(helper, Blocks.STONE.defaultBlockState(), 6);
        helper.setBlock(3, 2, 6, Blocks.OBSIDIAN);
        helper.setBlock(5, 4, 6, Blocks.DIRT);
        helper.setBlock(5, 2, 6, Blocks.IRON_ORE);
        final BlockPos middle = helper.absolutePos(new BlockPos(4, 3, 6));
        player.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(middle));
        helper.assertTrue(AreaMining.around(helper.getLevel(), player, drill, middle,
                        AreaMining.face(player, middle), MiningShape.area(3, 3, 1)).size() == 6,
                "the area takes the six stones and the ore around the block aimed at, not the obsidian or dirt");
        player.gameMode.destroyBlock(middle);
        helper.assertBlockPresent(Blocks.OBSIDIAN, 3, 2, 6);
        helper.assertBlockPresent(Blocks.DIRT, 5, 4, 6);
        helper.assertBlockPresent(Blocks.AIR, 5, 2, 6);
        leave(helper, player);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void drill_followsTheVeinOfAnOre(final GameTestHelper helper) {
        final ServerPlayer player = player(helper, "drill-vein");
        final ItemStack drill = charged(new ItemStack(TestTools.DRILL.get()));
        ItemStates.setMode(drill, TestTools.VEIN);
        player.setItemInHand(InteractionHand.MAIN_HAND, drill);
        helper.setBlock(3, 2, 6, Blocks.IRON_ORE);
        helper.setBlock(4, 2, 6, Blocks.IRON_ORE);
        helper.setBlock(5, 3, 7, Blocks.DEEPSLATE_IRON_ORE);
        helper.setBlock(8, 2, 6, Blocks.IRON_ORE);
        helper.setBlock(3, 3, 6, Blocks.COAL_ORE);
        final BlockPos first = helper.absolutePos(new BlockPos(3, 2, 6));
        player.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(first));
        player.gameMode.destroyBlock(first);
        helper.assertBlockPresent(Blocks.AIR, 4, 2, 6);
        helper.assertBlockPresent(Blocks.AIR, 5, 3, 7);
        helper.assertBlockPresent(Blocks.IRON_ORE, 8, 2, 6);
        helper.assertBlockPresent(Blocks.COAL_ORE, 3, 3, 6);
        helper.assertTrue(energy(drill) == TestTools.DRILL_ENERGY - 3 * TestTools.DRILL_PER_BLOCK,
                "three blocks of the vein, deepslate ore among them, and no more: " + energy(drill));
        leave(helper, player);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void drill_outOfEnergyDoesNoMoreThanAHand(final GameTestHelper helper) {
        final ServerPlayer player = player(helper, "drill-flat");
        final ItemStack drill = new ItemStack(TestTools.DRILL.get());
        ItemStates.setMode(drill, TestTools.AREA);
        player.setItemInHand(InteractionHand.MAIN_HAND, drill);
        final BlockState ore = Blocks.IRON_ORE.defaultBlockState();
        final BlockPos at = helper.absolutePos(new BlockPos(4, 3, 6));
        /* Standing, as a player digging does: in the air the game digs five times slower. */
        player.setOnGround(true);
        helper.assertTrue(player.getDigSpeed(ore, at) <= 1.0F, "an empty drill digs as a hand does: "
                + player.getDigSpeed(ore, at));
        helper.assertTrue(!player.hasCorrectToolForDrops(ore, helper.getLevel(), at), "and gets no ore");
        charged(drill);
        helper.assertTrue(player.getDigSpeed(ore, at) > 4.0F
                        && player.hasCorrectToolForDrops(ore, helper.getLevel(), at),
                "charged, it digs as the diamond pick it is and gets the ore: " + player.getDigSpeed(ore, at));
        wall(helper, Blocks.STONE.defaultBlockState(), 6);
        new ItemEnergyStorage(drill, TestTools.DRILL_ENERGY, 0, Integer.MAX_VALUE)
                .setStored(TestTools.DRILL_PER_BLOCK * 3L);
        player.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(at));
        player.gameMode.destroyBlock(at);
        int left = 0;
        for (int x = 3; x <= 5; x++) {
            for (int y = 2; y <= 4; y++) {
                if (!helper.getBlockState(new BlockPos(x, y, 6)).isAir()) {
                    left++;
                }
            }
        }
        helper.assertTrue(left == 6 && energy(drill) == 0,
                "with energy for three blocks it breaks three and stops: " + left + " left, " + energy(drill));
        leave(helper, player);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void hotPick_overheatsAndCoolsDown(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final ServerPlayer player = player(helper, "hot-pick");
        final ItemStack pick = new ItemStack(TestTools.HOT_PICK.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, pick);
        for (int x = 3; x <= 5; x++) {
            helper.setBlock(x, 2, 6, Blocks.STONE);
            player.gameMode.destroyBlock(helper.absolutePos(new BlockPos(x, 2, 6)));
        }
        final ToolHeat heat = TestTools.PICK_HEAT;
        helper.assertTrue(heat.overheated(level, pick) && !Tools.works(level, pick),
                "three blocks bring it to its most, and it overheats: " + heat.heat(level, pick));
        final BlockState stone = Blocks.STONE.defaultBlockState();
        helper.assertTrue(player.getDigSpeed(stone, null) <= 1.0F, "overheated, it digs as a hand does");
        helper.runAfterDelay(20, () -> {
            final double now = heat.heat(level, pick);
            helper.assertTrue(now < heat.most() && now > heat.most() - 3 && heat.overheated(level, pick),
                    "a second later it has cooled a little, and is still too hot: " + now);
            final ToolHeat.Heated held = pick.get(Tools.HEAT.get());
            pick.set(Tools.HEAT.get(), new ToolHeat.Heated(held.heat(), held.since() - 400, held.overheated()));
            helper.assertTrue(heat.heat(level, pick) == 0 && Tools.works(level, pick),
                    "and once it has cooled down to nothing it works again");
            leave(helper, player);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void chainsaw_burnsItsFuelForEachBlock(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final ServerPlayer player = player(helper, "chainsaw");
        final ItemStack saw = new ItemStack(TestTools.CHAINSAW.get());
        final IFluidHandler tank = saw.getCapability(Capabilities.FluidHandler.ITEM);
        helper.assertTrue(tank != null && tank.fill(TestFluids.COOL_OIL.stack(5), IFluidHandler.FluidAction.EXECUTE)
                == 5, "the chainsaw takes the test fuel");
        player.setItemInHand(InteractionHand.MAIN_HAND, saw);
        helper.setBlock(4, 2, 6, Blocks.OAK_LOG);
        player.gameMode.destroyBlock(helper.absolutePos(new BlockPos(4, 2, 6)));
        final IFluidHandler after = player.getMainHandItem().getCapability(Capabilities.FluidHandler.ITEM);
        helper.assertTrue(after != null && after.getFluidInTank(0).getAmount() == 3,
                "a log burns two millibuckets of it: " + (after == null ? -1 : after.getFluidInTank(0).getAmount()));
        helper.setBlock(4, 2, 6, Blocks.OAK_LOG);
        player.gameMode.destroyBlock(helper.absolutePos(new BlockPos(4, 2, 6)));
        helper.assertTrue(!Tools.works(level, player.getMainHandItem()),
                "with one millibucket left it no longer works");
        leave(helper, player);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void airShovel_spendsItsGasAndHoldsItInItsTank(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final ServerPlayer player = player(helper, "air-shovel");
        final ItemStack shovel = new ItemStack(TestTools.AIR_SHOVEL.get());
        final IMatterHandler tank = shovel.getCapability(MatterCapabilities.GAS_ITEM);
        helper.assertTrue(tank != null && tank.capacity(0) == TestTools.SHOVEL_TANK * TestTools.SHOVEL_PRESSURE,
                "the shovel's tank holds its volume times its pressure");
        helper.assertTrue(tank.insert(new MatterStack(TestMaterials.TESTOGEN, 1_000_000), false)
                == TestTools.SHOVEL_TANK * TestTools.SHOVEL_PRESSURE, "and takes no more");
        tank.extract(TestMaterials.TESTOGEN, tank.capacity(0) - 2 * TestTools.SHOVEL_PER_BLOCK, false);
        player.setItemInHand(InteractionHand.MAIN_HAND, shovel);
        for (int i = 0; i < 2; i++) {
            helper.setBlock(4, 2, 6, Blocks.DIRT);
            player.gameMode.destroyBlock(helper.absolutePos(new BlockPos(4, 2, 6)));
        }
        helper.assertTrue(shovel.get(ItemStates.GAS.get()) == null && !Tools.works(level, shovel),
                "two blocks spend its two uses of gas, and empty it no longer works");
        leave(helper, player);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void railgun_firesLetGoChargedThenWaits(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final ServerPlayer player = player(helper, "railgun");
        final ItemStack gun = charged(new ItemStack(TestTools.RAILGUN.get()));
        player.setItemInHand(InteractionHand.MAIN_HAND, gun);
        TestTools.Railgun.forget();
        final int held = gun.getUseDuration(player);
        gun.releaseUsing(level, player, held - 5);
        helper.assertTrue(TestTools.Railgun.lastShot() < 0 && energy(gun) == TestTools.RAILGUN_ENERGY,
                "let go a quarter charged, it does not fire and spends nothing");
        gun.releaseUsing(level, player, held - TestTools.RAILGUN_CHARGE - 5);
        helper.assertTrue(TestTools.Railgun.lastShot() == 1.0F
                        && energy(gun) == TestTools.RAILGUN_ENERGY - TestTools.RAILGUN_PER_SHOT,
                "let go fully charged, it fires and spends a shot: " + TestTools.Railgun.lastShot());
        helper.assertTrue(player.getCooldowns().isOnCooldown(gun.getItem()) && !Tools.ready(player, gun)
                        && !gun.use(level, player, InteractionHand.MAIN_HAND).getResult().consumesAction(),
                "then it waits before the next");
        helper.assertTrue(Tools.charged(gun, TestTools.RAILGUN_CHARGE / 2) == 0.5F, "half the time, half charged");
        leave(helper, player);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void wand_laysAPlaneAndALineFromTheInventory(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final ServerPlayer player = player(helper, "wand-plane");
        final ItemStack wand = new ItemStack(TestTools.WAND.get());
        ItemStates.setMode(wand, TestTools.PLANE);
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        player.getInventory().setItem(1, new ItemStack(Items.STONE, 2));
        for (int x = 2; x <= 4; x++) {
            helper.setBlock(x, 2, 2, Blocks.STONE);
        }
        use(helper, player, wand, new BlockPos(3, 2, 2), Direction.UP);
        helper.assertBlockPresent(Blocks.STONE, 3, 3, 2);
        helper.assertBlockPresent(Blocks.STONE, 2, 3, 2);
        helper.assertBlockPresent(Blocks.AIR, 4, 3, 2);
        helper.assertTrue(player.getInventory().getItem(1).isEmpty(), "two stones placed, the two it had");
        ItemStates.setMode(wand, TestTools.LINE);
        player.getInventory().setItem(1, new ItemStack(Items.STONE, 10));
        for (int x = 2; x <= 4; x++) {
            helper.setBlock(x, 2, 7, Blocks.STONE);
        }
        use(helper, player, wand, new BlockPos(3, 2, 7), Direction.NORTH);
        for (int x = 2; x <= 4; x++) {
            helper.assertBlockPresent(Blocks.STONE, x, 2, 6);
        }
        helper.assertTrue(player.getInventory().getItem(1).getCount() == 7, "a line of three out of the face");
        helper.assertTrue(level.getBlockState(helper.absolutePos(new BlockPos(3, 2, 8))).isAir(),
                "and nothing behind it");
        leave(helper, player);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void wand_copiesARegionAndPastesItTurned(final GameTestHelper helper) {
        final ServerPlayer player = player(helper, "wand-copy");
        final ItemStack wand = new ItemStack(TestTools.WAND.get());
        ItemStates.setMode(wand, TestTools.COPY);
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        player.getInventory().setItem(1, new ItemStack(Items.STONE, 4));
        player.getInventory().setItem(2, new ItemStack(Items.OAK_LOG, 4));
        helper.setBlock(2, 2, 2, Blocks.STONE);
        helper.setBlock(new BlockPos(3, 2, 2), Blocks.OAK_LOG.defaultBlockState()
                .setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        player.setYRot(0);
        use(helper, player, wand, new BlockPos(2, 2, 2), Direction.UP);
        use(helper, player, wand, new BlockPos(3, 2, 2), Direction.UP);
        helper.assertTrue(wand.get(BuildingTools.COPY.get()) != null
                        && wand.get(BuildingTools.COPY.get()).count() == 2, "the wand holds a copy of the two blocks");
        ItemStates.setMode(wand, TestTools.PASTE);
        helper.setBlock(6, 2, 6, Blocks.STONE);
        player.setYRot(90);
        use(helper, player, wand, new BlockPos(6, 2, 6), Direction.UP);
        helper.assertBlockPresent(Blocks.STONE, 6, 3, 6);
        helper.assertBlockProperty(new BlockPos(6, 3, 7), RotatedPillarBlock.AXIS, Direction.Axis.Z);
        helper.assertTrue(player.getInventory().getItem(1).getCount() == 3
                        && player.getInventory().getItem(2).getCount() == 3,
                "pasted turned a quarter as the player turned, from the inventory");
        leave(helper, player);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BALANCE_BATCH)
    public static void wand_refusesARegionLargerThanTheServerAllows(final GameTestHelper helper) {
        final ServerPlayer player = player(helper, "wand-big");
        final ItemStack wand = new ItemStack(TestTools.WAND.get());
        ItemStates.setMode(wand, TestTools.COPY);
        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        final int before = ToolBalance.mostCopied();
        ToolBalance.setMostCopied(4);
        try {
            helper.setBlock(2, 2, 2, Blocks.STONE);
            helper.setBlock(4, 2, 4, Blocks.STONE);
            use(helper, player, wand, new BlockPos(2, 2, 2), Direction.UP);
            use(helper, player, wand, new BlockPos(4, 2, 4), Direction.UP);
            helper.assertTrue(wand.get(BuildingTools.COPY.get()) == null,
                    "a region of nine places is not copied while the server allows four");
        } finally {
            ToolBalance.setMostCopied(before);
        }
        leave(helper, player);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void multitool_modulesGiveItAVeinSilkTouchAndReach(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final ServerPlayer player = player(helper, "multitool");
        final ItemStack tool = charged(new ItemStack(TestTools.MULTITOOL.get()));
        Modules.install(level, tool, 0, new ItemStack(TestTools.VEIN_MODULE.get()));
        Modules.install(level, tool, 1, new ItemStack(TestTools.SILK_MODULE.get()));
        Modules.install(level, tool, 2, new ItemStack(TestTools.REACH_MODULE.get()));
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        helper.assertTrue(Tools.mines(level, tool).equals(Optional.of(MiningShape.vein(TestTools.MODULE_VEIN))),
                "the vein module makes it mine a vein");
        helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(level.registryAccess()
                        .lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), tool) == 1,
                "the silk module makes it work as though it had silk touch");
        final double reach = player.getAttributeBaseValue(Attributes.BLOCK_INTERACTION_RANGE);
        helper.runAfterDelay(EQUIPMENT_TICKS, () -> {
            /* A player's own tick is where the game counts what is held among their attributes. */
            player.doTick();
            helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE) - reach
                            - TestTools.REACH_BOOST) < 1.0e-9,
                    "and held, the reach module lets its holder reach further: "
                            + player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE));
            helper.setBlock(4, 2, 6, Blocks.STONE);
            player.gameMode.destroyBlock(helper.absolutePos(new BlockPos(4, 2, 6)));
            helper.assertItemEntityPresent(Items.STONE, new BlockPos(4, 2, 6), 2.0);
            helper.assertTrue(energy(tool) == TestTools.MULTITOOL_ENERGY - 2 * TestTools.MODULE_ENERGY,
                    "the stone drops as itself, and the two modules that spend spend a use: " + energy(tool));
            leave(helper, player);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void hammer_lastsByTheTinItIsMadeOf(final GameTestHelper helper) {
        final int protons = SeriesMaterials.TIN.value(MaterialProperties.ATOMIC_NUMBER).orElse(-1);
        final ItemStack hammer = new ItemStack(TestTools.TIN_HAMMER.get());
        helper.assertTrue(protons > 0 && hammer.getMaxDamage() == protons * TestTools.USES_PER_PROTON
                        && hammer.getMaxStackSize() == 1,
                "the hammer lasts five uses for each of tin's protons, alone in its stack: " + hammer.getMaxDamage());
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void magnet_turnsOnWhereverItIsCarried(final GameTestHelper helper) {
        final ServerPlayer player = player(helper, "magnet");
        final ItemStack magnet = new ItemStack(TestTools.MAGNET.get());
        player.getInventory().setItem(20, magnet);
        helper.assertTrue(!ItemActions.isOn(magnet), "a magnet never turned is off");
        KeyActions.press(player, TestTools.MAGNET_KEY.id(), false);
        helper.assertTrue(ItemActions.isOn(player.getInventory().getItem(20)),
                "the key turns it on deep in the inventory");
        KeyActions.press(player, TestTools.MAGNET_KEY.id(), false);
        helper.assertTrue(!ItemActions.isOn(player.getInventory().getItem(20)), "and off again");
        leave(helper, player);
        helper.succeed();
    }

    /* A wall of {@code state} three by three, at x 3 to 5 and y 2 to 4, across {@code z}. */
    private static void wall(final GameTestHelper helper, final BlockState state, final int z) {
        for (int x = 3; x <= 5; x++) {
            for (int y = 2; y <= 4; y++) {
                helper.setBlock(new BlockPos(x, y, z), state);
            }
        }
    }

    /* Uses {@code stack} on {@code face} of the block at {@code relative}, as the player's game would ask. */
    private static void use(final GameTestHelper helper, final ServerPlayer player, final ItemStack stack,
                            final BlockPos relative, final Direction face) {
        final BlockPos at = helper.absolutePos(relative);
        final Vec3 hit = Vec3.atCenterOf(at).add(Vec3.atLowerCornerOf(face.getNormal()).scale(0.5));
        player.gameMode.useItemOn(player, helper.getLevel(), stack, InteractionHand.MAIN_HAND,
                new BlockHitResult(hit, face, at, false));
    }

    /* Fills {@code stack} with energy, a transfer at a time; gives it. */
    private static ItemStack charged(final ItemStack stack) {
        final IEnergyStorage energy = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        while (energy != null && energy.receiveEnergy(Integer.MAX_VALUE, false) > 0) {
            // Each transfer takes what the item takes at once.
        }
        return stack;
    }

    private static int energy(final ItemStack stack) {
        final IEnergyStorage energy = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        return energy == null ? -1 : energy.getEnergyStored();
    }

    /* A player of a game connected to the server, in survival, in the test's arena, facing along z. */
    private static ServerPlayer player(final GameTestHelper helper, final String name) {
        final ServerLevel level = helper.getLevel();
        final CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(),
                name), false);
        final ServerPlayer player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(),
                cookie.clientInformation()) {
            @Override
            public boolean isSpectator() {
                return false;
            }

            @Override
            public boolean isCreative() {
                return false;
            }
        };
        final Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        NetworkRegistry.configureMockConnection(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        for (int id = 0; id <= 8; id++) {
            player.connection.handleAcceptTeleportPacket(new ServerboundAcceptTeleportationPacket(id));
        }
        player.setGameMode(GameType.SURVIVAL);
        final Vec3 there = helper.absoluteVec(new Vec3(4.5, 2, 4.5));
        player.moveTo(there.x, there.y, there.z, 0, 0);
        return player;
    }

    private static void leave(final GameTestHelper helper, final ServerPlayer player) {
        helper.getLevel().getServer().getPlayerList().remove(player);
    }
}
