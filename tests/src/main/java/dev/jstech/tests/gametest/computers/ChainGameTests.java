/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import com.mojang.authlib.GameProfile;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.block.WorkbenchBlock;
import dev.jstech.computers.blockentity.WorkbenchBlockEntity;
import dev.jstech.computers.chain.ChainBenches;
import dev.jstech.computers.chain.ChainParts;
import dev.jstech.computers.menu.WorkbenchMenu;
import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.Elements;
import dev.jstech.core.jstechseries.SeriesCompounds;
import dev.jstech.core.jstechseries.SeriesMinerals;
import dev.jstech.core.material.Form;
import dev.jstech.core.material.Forms;
import dev.jstech.core.material.HostRock;
import dev.jstech.core.material.HostRocks;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialLiquids;
import dev.jstech.core.material.MaterialUse;
import dev.jstech.core.material.MaterialUses;
import dev.jstech.core.worldgen.vein.DepositType;
import dev.jstech.core.worldgen.vein.Deposits;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.computers.TestComputerEnergy;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * J's Computers' own chain (computers/docs/design/survival.md): the ores of each era in the rocks of their veins and
 * the forms the chain asks for, its liquids, the veins of each era near the surface, deeper and in the deepslate, the
 * parts on the chain's shelf, and the Electronics Workbench of each era: it assembles from its nine trays a part of its
 * era or an older one, and nothing when the trays hold anything besides; it draws its energy behind it and nowhere
 * else, and nothing reaches its trays through its sides.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ChainGameTests {

    private static final String ARENA = "empty";
    private static final String ENERGY = TestComputerEnergy.BATCH;
    /* Where a bench stands, facing east: its back, where its energy comes in, to the west. */
    private static final BlockPos BENCH = new BlockPos(3, 2, 3);
    private static final int VINTAGE = 0;
    private static final int LEGACY = 1;

    private ChainGameTests() {
    }

    @GameTest(template = ARENA)
    public static void materials_theOresLieInTheirErasRocks(final GameTestHelper helper) {
        helper.assertTrue(ores(Elements.of(Element.TIN)).equals(List.of(HostRocks.STONE, HostRocks.DEEPSLATE)),
                "tin's ore in the stone and the deepslate");
        helper.assertTrue(ores(Elements.of(Element.MOLYBDENUM)).equals(List.of(HostRocks.DEEPSLATE)),
                "molybdenum's only in the deepslate");
        helper.assertTrue(ores(SeriesMinerals.FLUORSPAR).equals(List.of(HostRocks.STONE, HostRocks.DEEPSLATE,
                HostRocks.GRANITE, HostRocks.ANDESITE, HostRocks.TUFF)), "fluorspar's in its five rocks, in order");
        helper.assertTrue(used(Elements.of(Element.MERCURY), Forms.RAW) && used(Elements.of(Element.LITHIUM),
                Forms.CRUSHED), "mercury's ore drops its cinnabar, lithium's is crushed");
        helper.assertTrue(used(SeriesCompounds.COKE, Forms.LUMP) && used(SeriesCompounds.PVC, Forms.SHEET)
                && used(SeriesCompounds.PHENOLIC_RESIN, Forms.PLATE) && used(SeriesCompounds.POLYSILICON, Forms.ROD),
                "coke in lumps, PVC as sheets, the resin as plates, polysilicon as rods");
        helper.assertTrue(!used(Elements.of(Element.LITHIUM), Forms.INGOT),
                "no form the chain does not ask for: lithium has no ingot");
        helper.assertTrue(MaterialLiquids.registered().stream().anyMatch(liquid -> liquid.material()
                == Elements.of(Element.MERCURY)), "mercury is a liquid the chain uses");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void deposits_eachEraLiesDeeperAndRarer(final GameTestHelper helper) {
        final DepositType lead = deposit("lead");
        final DepositType cobalt = deposit("cobalt");
        final DepositType tungsten = deposit("tungsten");
        final DepositType fluorspar = deposit("fluorspar");
        helper.assertTrue(lead.minY() == 0 && lead.maxY() == 80, "the Vintage's near the surface");
        helper.assertTrue(cobalt.minY() == -16 && cobalt.maxY() == 40 && cobalt.frequency() < lead.frequency(),
                "the Legacy's deeper and rarer");
        helper.assertTrue(tungsten.maxY() == 0 && tungsten.frequency() < cobalt.frequency()
                && tungsten.rocks().equals(List.of(HostRocks.DEEPSLATE.id())), "the later eras' only in the deepslate");
        helper.assertTrue(fluorspar.minerals().getFirst().material().equals(SeriesMinerals.FLUORSPAR.id())
                && fluorspar.rocks().size() == 5, "fluorspar lies in veins of its own, through each of its rocks");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void parts_areOnTheChainsShelfByTheirNames(final GameTestHelper helper) {
        final TagKey<Item> chain = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(
                JsComputers.MODID, "chain"));
        final Item chip = ChainParts.MEMORY_CHIPS.get(VINTAGE).get();
        helper.assertTrue(chip.builtInRegistryHolder().is(chain) && ChainBenches.VINTAGE.get().asItem()
                .builtInRegistryHolder().is(chain), "the parts and the benches are on the chain's shelf");
        helper.assertValueEqual(new ItemStack(chip).getHoverName().getString(), "Memory Chip (Vintage)",
                "named as the mock names them");
        helper.assertValueEqual(ChainParts.MEMORY_MASKS.size() + ChainParts.CONTROLLER_MASKS.size(), 10,
                "a memory and a controller mask for each era");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 120)
    public static void bench_assemblesThePartItsTraysHold(final GameTestHelper helper) {
        final WorkbenchBlockEntity bench = bench(helper, ChainBenches.VINTAGE.get(), BENCH);
        bench.trays().insertItem(0, memoryChips(VINTAGE, 2), false);
        bench.trays().insertItem(7, new ItemStack(ChainParts.ETCHED_BOARDS.get(VINTAGE).get()), false);
        helper.succeedWhen(() -> {
            helper.assertTrue(bench.output().getStackInSlot(0).is(ChainParts.CERAMIC_PACKAGE.get()),
                    "the part waits in its tray");
            helper.assertTrue(bench.trays().getStackInSlot(0).isEmpty() && bench.trays().getStackInSlot(7).isEmpty(),
                    "its ingredients taken from their trays, wherever they lay");
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 120)
    public static void bench_assemblesNothingWhenATrayHoldsAnythingBesides(final GameTestHelper helper) {
        final WorkbenchBlockEntity bench = bench(helper, ChainBenches.VINTAGE.get(), BENCH);
        bench.trays().insertItem(0, memoryChips(VINTAGE, 2), false);
        bench.trays().insertItem(1, new ItemStack(ChainParts.ETCHED_BOARDS.get(VINTAGE).get()), false);
        bench.trays().insertItem(4, new ItemStack(ChainParts.FERRITE.get()), false);
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(bench.output().getStackInSlot(0).isEmpty() && bench.partUnderWay().isEmpty(),
                    "a ferrite left in a tray holds the part back");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 120)
    public static void bench_assemblesOlderErasPartsButNotNewer(final GameTestHelper helper) {
        final WorkbenchBlockEntity vintage = bench(helper, ChainBenches.VINTAGE.get(), BENCH);
        vintage.trays().insertItem(0, memoryChips(LEGACY, 1), false);
        final WorkbenchBlockEntity legacy = bench(helper, ChainBenches.LEGACY.get(), BENCH.south(2));
        legacy.trays().insertItem(3, memoryChips(VINTAGE, 2), false);
        legacy.trays().insertItem(5, new ItemStack(ChainParts.ETCHED_BOARDS.get(VINTAGE).get()), false);
        helper.succeedWhen(() -> {
            helper.assertTrue(legacy.output().getStackInSlot(0).is(ChainParts.CERAMIC_PACKAGE.get()),
                    "a Legacy bench assembles a Vintage part");
            helper.assertTrue(vintage.output().getStackInSlot(0).isEmpty() && vintage.partUnderWay().isEmpty(),
                    "a Vintage bench no Legacy one");
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 300, batch = ENERGY)
    public static void bench_drawsItsEnergyBehindIt(final GameTestHelper helper) {
        final WorkbenchBlockEntity bench = bench(helper, ChainBenches.VINTAGE.get(), BENCH);
        bench.trays().insertItem(0, memoryChips(VINTAGE, 2), false);
        bench.trays().insertItem(1, new ItemStack(ChainParts.ETCHED_BOARDS.get(VINTAGE).get()), false);
        helper.startSequence()
                .thenIdle(40)
                .thenExecute(() -> {
                    helper.assertTrue(bench.output().getStackInSlot(0).isEmpty() && !bench.powered(),
                            "no energy, no part");
                    helper.assertTrue(bench.energyPort(Direction.EAST) == null
                            && bench.energyPort(Direction.UP) == null, "nothing at its front or on top");
                })
                .thenWaitUntil(() -> {
                    // A cable behind it gives it a tick's worth every tick.
                    TestComputerEnergy.feed(bench.energyPort(Direction.WEST));
                    helper.assertTrue(bench.output().getStackInSlot(0).is(ChainParts.CERAMIC_PACKAGE.get()),
                            "fed from behind, it assembles");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void bench_takesNoItemsThroughItsSides(final GameTestHelper helper) {
        bench(helper, ChainBenches.ADVANCED.get(), BENCH);
        final BlockPos at = helper.absolutePos(BENCH);
        for (final Direction side : Direction.values()) {
            helper.assertTrue(helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, at, side) == null,
                    "nothing reaches its trays from the " + side);
        }
        helper.assertTrue(helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, at, null) == null,
                "nor from nowhere in particular");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void bench_windowStaysOpenAndTheBenchFallsWithItsTrays(final GameTestHelper helper) {
        final WorkbenchBlockEntity bench = bench(helper, ChainBenches.TRANSITION.get(), BENCH);
        final ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(),
                "bench-test"));
        player.setPos(Vec3.atCenterOf(helper.absolutePos(BENCH.west())));
        final WorkbenchMenu menu = new WorkbenchMenu(0, player.getInventory(), bench);
        helper.assertTrue(menu.stillValid(player), "the window stays open at a bench of any era");
        bench.trays().insertItem(2, new ItemStack(ChainParts.FERRITE.get(), 3), false);
        helper.getLevel().destroyBlock(helper.absolutePos(BENCH), true);
        helper.assertItemEntityPresent(ChainBenches.TRANSITION.get().asItem(), BENCH, 2.0);
        helper.assertItemEntityCountIs(ChainParts.FERRITE.get(), BENCH, 2.0, 3);
        helper.assertTrue(!menu.stillValid(player), "and closes once the bench is gone");
        helper.succeed();
    }

    /* A bench of {@code block} at {@code pos}, facing east, its back to the west. */
    private static WorkbenchBlockEntity bench(final GameTestHelper helper, final WorkbenchBlock block,
                                              final BlockPos pos) {
        helper.setBlock(pos, block.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
        return TestWorldBuilder.forGameTest(helper).blockEntity(pos, WorkbenchBlockEntity.class);
    }

    private static ItemStack memoryChips(final int era, final int count) {
        return new ItemStack(ChainParts.MEMORY_CHIPS.get(era).get(), count);
    }

    /* The rocks the chain lays {@code material}'s ore in, in the order the ore blocks were declared. */
    private static List<HostRock> ores(final Material material) {
        return MaterialUses.all().stream().filter(use -> use.modid().equals(JsComputers.MODID)
                && use.material() == material && use.form() == Forms.ORE).map(MaterialUse::rock).toList();
    }

    private static boolean used(final Material material, final Form form) {
        return MaterialUses.all().stream().anyMatch(use -> use.modid().equals(JsComputers.MODID)
                && use.material() == material && use.form() == form);
    }

    private static DepositType deposit(final String path) {
        return Deposits.declared().get(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, path));
    }
}
