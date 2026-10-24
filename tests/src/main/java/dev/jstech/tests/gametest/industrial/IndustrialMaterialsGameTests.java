/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.industrial;

import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.ElementNaming;
import dev.jstech.core.jstechseries.Elements;
import dev.jstech.core.jstechseries.Isotopes;
import dev.jstech.core.jstechseries.SeriesMaterials;
import dev.jstech.core.jstechseries.SeriesMinerals;
import dev.jstech.core.jstechseries.SharedCatalogue;
import dev.jstech.core.jstechseries.Superheavies;
import dev.jstech.core.machine.ProcessingRecipe;
import dev.jstech.core.material.Forms;
import dev.jstech.core.material.HostRocks;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialUses;
import dev.jstech.core.material.Mineral;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.core.progression.Knowledge;
import dev.jstech.core.progression.TeamProgress;
import dev.jstech.core.team.CoreTeams;
import dev.jstech.industrial.IndustrialModule;
import dev.jstech.industrial.JsIndustrial;
import dev.jstech.industrial.blockentity.MaceratorBlockEntity;
import dev.jstech.industrial.chemistry.ReactionRecipes;
import dev.jstech.industrial.chemistry.Reactions;
import dev.jstech.industrial.discovery.ElementDiscovery;
import dev.jstech.industrial.material.IndustrialMaterials;
import dev.jstech.industrial.metallurgy.OreLevel;
import dev.jstech.industrial.tool.HandScrap;
import dev.jstech.industrial.tool.HandTools;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestKitMachine;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * J's Industrial's materials as a contract, from the catalogue's data: its minerals' ores named and tagged by the
 * Core's rule and dropping their raw minerals; the Macerator grinding any of them into two dusts and a counted share of
 * their byproducts; the furnace smelting the simple minerals raw and never a sulfide; the hand tools yielding less and
 * wearing in the grid; the Draw Plate's scraps, counted; a scrap melting into a nugget; an element discovered at its
 * first synthesis by a team; and a reaction become a chemical machine's recipe, in its coefficients' amounts.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class IndustrialMaterialsGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos MACERATOR = new BlockPos(2, 2, 2);
    private static final int GROUND = 4;
    private static final int WIRES = 8;

    private IndustrialMaterialsGameTests() {
    }

    @GameTest(template = ARENA)
    public static void mineralOres_areNamedAndTaggedByTheCoresRule(final GameTestHelper helper) {
        final Block ore = MaterialUses.oreBlock(SeriesMinerals.CHALCOPYRITE, HostRocks.DEEPSLATE).get();
        final BlockState state = ore.defaultBlockState();
        helper.assertTrue(ore.getName().getString().equals("Deepslate Chalcopyrite Ore"),
                "the ore is named for its rock and its mineral: " + ore.getName().getString());
        helper.assertTrue(state.is(Forms.ORE.blockTag(SeriesMinerals.CHALCOPYRITE)),
                "and filed under c:ores/chalcopyrite");
        final List<ItemStack> drops = Block.getDrops(state, helper.getLevel(), helper.absolutePos(MACERATOR), null);
        final Item raw = MaterialUses.item(SeriesMinerals.CHALCOPYRITE, Forms.RAW).get();
        helper.assertTrue(drops.size() == 1 && drops.getFirst().is(raw), "it drops raw chalcopyrite: " + drops);
        helper.assertTrue(new ItemStack(raw).getHoverName().getString().equals("Raw Chalcopyrite")
                && new ItemStack(raw).is(Forms.RAW.itemTag(SeriesMinerals.CHALCOPYRITE)),
                "named and tagged as raw ores are");
        final Block tin = MaterialUses.oreBlock(SeriesMaterials.TIN, HostRocks.STONE).get();
        helper.assertTrue(tin.defaultBlockState().is(Forms.ORE.blockTag(SeriesMaterials.TIN))
                && tin.getName().getString().equals("Tin Ore"), "tin's X ore is the Core's, c:ores/tin");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 1_200)
    public static void macerator_grindsAMineralIntoTwoDustsAndItsCountedByproducts(final GameTestHelper helper) {
        helper.setBlock(MACERATOR, IndustrialModule.MACERATOR.get());
        final MaceratorBlockEntity macerator = helper.getBlockEntity(MACERATOR);
        macerator.getInventory().setStackInSlot(0, new ItemStack(MaterialUses.item(SeriesMinerals.CHALCOPYRITE,
                Forms.RAW).get(), GROUND));
        helper.onEachTick(() -> macerator.power().generate(macerator.power().capacity()));
        final Mineral mineral = SeriesMinerals.CHALCOPYRITE.mineral().orElseThrow();
        final double sulfur = OreLevel.MACERATOR.byproduct(mineral.main().share(), share(mineral, Element.SULFUR),
                OreLevel.MACERATOR.mainOut());
        final Item copper = MaterialUses.item(SeriesMaterials.COPPER, Forms.DUST).get();
        final Item sulfurDust = MaterialUses.item(Elements.of(Element.SULFUR), Forms.DUST).get();
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(made(macerator, copper) == GROUND * 2,
                        "two copper dusts each: " + made(macerator, copper)))
                .thenExecute(() -> helper.assertTrue(made(macerator, sulfurDust) == (int) Math.floor(GROUND * sulfur
                        + 1e-9), "and the sulfur's counted share, never by chance: " + made(macerator, sulfurDust)
                        + " of " + GROUND * sulfur))
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void furnace_smeltsTheSimpleMineralsRawButNoSulfide(final GameTestHelper helper) {
        helper.assertTrue(smelted(helper, SeriesMinerals.HEMATITE).filter(made -> made.is(Items.IRON_INGOT))
                .isPresent(), "an oxide smelts raw into its metal");
        helper.assertTrue(smelted(helper, SeriesMinerals.ELECTRUM).isPresent()
                && smelted(helper, SeriesMinerals.MALACHITE).isPresent(), "a native metal and a carbonate too");
        helper.assertTrue(smelted(helper, SeriesMinerals.CHALCOPYRITE).isEmpty()
                && smelted(helper, SeriesMinerals.PYRITE).isEmpty(), "a sulfide must first be roasted");
        helper.assertTrue(smelted(helper, SeriesMaterials.TIN).filter(made -> made.is(MaterialUses.item(
                SeriesMaterials.TIN, Forms.INGOT).get())).isPresent(), "raw tin smelts into tin");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void handTools_yieldLessThanAMachineAndWearInTheGrid(final GameTestHelper helper) {
        final Recipe<?> plate = recipe(helper, "hand/iron_ingot_to_plate");
        helper.assertTrue(plate.getIngredients().size() == 4 && plate.getResultItem(helper.getLevel().registryAccess())
                .getCount() == 2, "the Forge Hammer beats three ingots into two plates");
        final Recipe<?> wire = recipe(helper, "hand/copper_ingot_to_wire");
        helper.assertTrue(wire.getResultItem(helper.getLevel().registryAccess()).getCount() == 1,
                "the Draw Plate pulls one wire of an ingot, where the Wire Drawer pulls two");
        final ItemStack hammer = new ItemStack(HandTools.FORGE_HAMMER.get());
        final ItemStack left = hammer.getCraftingRemainingItem();
        helper.assertTrue(left.is(HandTools.FORGE_HAMMER.get()) && left.getDamageValue() == 1,
                "the hammer stays in the grid, a use worn");
        hammer.setDamageValue(hammer.getMaxDamage() - 1);
        helper.assertTrue(hammer.getCraftingRemainingItem().isEmpty(), "until it is worn out");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void drawPlate_leavesAScrapEveryEighthWire(final GameTestHelper helper) {
        final FakePlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.getInventory().clearContent();
        player.setData(HandScrap.COUNTED, Map.of());
        final ItemStack wire = new ItemStack(MaterialUses.item(SeriesMaterials.COPPER, Forms.WIRE).get());
        final Item scrap = MaterialUses.item(SeriesMaterials.COPPER, IndustrialMaterials.SCRAP).get();
        try {
            int given = 0;
            for (int pulled = 0; pulled < WIRES; pulled++) {
                given += HandScrap.count(player, HandTools.DRAW_PLATE.get(), wire);
            }
            helper.assertTrue(given == 1 && player.getInventory().countItem(scrap) == 1,
                    "eight wires pulled by hand leave one scrap: " + given);
            helper.assertTrue(HandScrap.count(player, HandTools.FORGE_HAMMER.get(), wire) == 0,
                    "and work that is no drawing leaves none");
        } finally {
            player.getInventory().clearContent();
            player.setData(HandScrap.COUNTED, Map.of());
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void furnace_meltsAScrapIntoANuggetAndADustIntoItsIngot(final GameTestHelper helper) {
        helper.assertTrue(cooked(helper, MaterialUses.item(SeriesMaterials.COPPER, IndustrialMaterials.SCRAP).get())
                .filter(made -> made.is(MaterialUses.item(SeriesMaterials.COPPER, Forms.NUGGET).get())).isPresent(),
                "a scrap melts into a nugget, a loss before the arc furnace");
        helper.assertTrue(cooked(helper, MaterialUses.item(Elements.of(Element.SILVER), Forms.DUST).get())
                .filter(made -> made.is(MaterialUses.item(Elements.of(Element.SILVER), Forms.INGOT).get()))
                .isPresent(), "silver dust melts back into silver");
        helper.assertTrue(cooked(helper, MaterialUses.item(Elements.of(Element.TITANIUM), Forms.DUST).get())
                .isEmpty(), "titanium melts hotter than a furnace reaches");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void elementDiscovery_isATeamsFirstSynthesis(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final UUID owner = UUID.randomUUID();
        final String team = CoreTeams.teamOf(server, owner);
        final Material technetium = Elements.of(Element.TECHNETIUM);
        final Material beyond = Superheavies.of(170);
        try {
            helper.assertTrue(ElementDiscovery.discover(server, owner, technetium), "first made, discovered");
            final Knowledge known = ElementDiscovery.knowledge(technetium).orElseThrow();
            helper.assertTrue(TeamProgress.knows(server, team, known.id()), "the team knows it");
            helper.assertTrue(!ElementDiscovery.discover(server, owner, technetium), "and discovers it once");
            helper.assertTrue(!ElementDiscovery.discover(server, owner, SeriesMaterials.IRON),
                    "an element found in nature is no discovery");
            helper.assertTrue(ElementDiscovery.discover(server, owner, Isotopes.PLUTONIUM_239),
                    "making an isotope of one discovers its element");
            helper.assertTrue(ElementDiscovery.discover(server, owner, beyond)
                    && ElementNaming.DISCOVERED.get(server).get(beyond.id()).by().equals(owner),
                    "one beyond the known table is its first maker's to name, away as they are");
        } finally {
            for (final Knowledge knowledge : ElementDiscovery.bySynthesis().values()) {
                TeamProgress.forget(server, team, knowledge.id());
            }
            ElementNaming.DISCOVERED.update(server, all -> {
                final Map<ResourceLocation, ElementNaming.Discovery> out = new HashMap<>(all);
                out.remove(beyond.id());
                return Map.copyOf(out);
            });
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void reaction_becomesAMachinesRecipeInItsCoefficientsAmounts(final GameTestHelper helper) {
        final ProcessingRecipe ammonia = ReactionRecipes.of(TestKitMachine.REFINING, Reactions.AMMONIA, 2,
                Reactions.HABER_BOSCH, Ingredient.of(TestKitMachine.CATALYST.get())).ticks(10).build();
        helper.assertTrue(ammonia.matterInputs().equals(List.of(new MatterStack(Elements.of(Element.NITROGEN), 200L),
                new MatterStack(Elements.of(Element.HYDROGEN), 600L))), "two batches of N2 + 3 H2: "
                + ammonia.matterInputs());
        helper.assertTrue(ammonia.matterOutputs().equals(List.of(new MatterStack(SharedCatalogue.AMMONIA, 400L))),
                "make 400 mB of ammonia");
        helper.assertTrue(ammonia.catalysts().size() == 1 && ammonia.conditions().leastKelvin() == 673.15
                && ammonia.conditions().mostKpa() == 25_000.0, "over its catalyst, at its heat and its pressure");
        helper.succeed();
    }

    private static double share(final Mineral mineral, final Element element) {
        for (final Mineral.Content content : mineral.contents()) {
            if (content.material() == Elements.of(element)) {
                return content.share();
            }
        }
        return 0.0;
    }

    /* How many of {@code item} the Macerator's outputs hold. */
    private static int made(final MaceratorBlockEntity macerator, final Item item) {
        int count = 0;
        for (int slot = 1; slot <= MaceratorBlockEntity.OUTPUTS; slot++) {
            final ItemStack held = macerator.getInventory().getStackInSlot(slot);
            if (held.is(item)) {
                count += held.getCount();
            }
        }
        return count;
    }

    private static Optional<ItemStack> smelted(final GameTestHelper helper, final Material material) {
        return cooked(helper, MaterialUses.item(material, Forms.RAW).get());
    }

    private static Optional<ItemStack> cooked(final GameTestHelper helper, final Item item) {
        return helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(
                new ItemStack(item)), helper.getLevel()).map(found -> found.value().getResultItem(
                helper.getLevel().registryAccess()));
    }

    private static Recipe<?> recipe(final GameTestHelper helper, final String path) {
        final Optional<RecipeHolder<?>> found = helper.getLevel().getRecipeManager().byKey(
                ResourceLocation.fromNamespaceAndPath(JsIndustrial.MODID, path));
        helper.assertTrue(found.isPresent(), "the recipe " + path + " is there");
        return found.orElseThrow().value();
    }
}
