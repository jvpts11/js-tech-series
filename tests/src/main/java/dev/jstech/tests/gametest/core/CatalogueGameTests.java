/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import com.mojang.authlib.GameProfile;
import dev.jstech.core.JsCore;
import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.ElementNaming;
import dev.jstech.core.jstechseries.Elements;
import dev.jstech.core.jstechseries.ExoticMatter;
import dev.jstech.core.hazard.HazardBalance;
import dev.jstech.core.jstechseries.Isotopes;
import dev.jstech.core.jstechseries.NaturalRadioactivity;
import dev.jstech.core.jstechseries.SeriesMaterials;
import dev.jstech.core.jstechseries.SharedCatalogue;
import dev.jstech.core.jstechseries.SimplePath;
import dev.jstech.core.jstechseries.Superheavies;
import dev.jstech.core.material.Forms;
import dev.jstech.core.material.HostRocks;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialKind;
import dev.jstech.core.material.MaterialLiquid;
import dev.jstech.core.material.MaterialLiquids;
import dev.jstech.core.material.MaterialMarks;
import dev.jstech.core.material.MaterialNames;
import dev.jstech.core.material.MaterialProperties;
import dev.jstech.core.material.MaterialUses;
import dev.jstech.core.material.Materials;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.material.Mineral;
import dev.jstech.core.material.MineralFamilies;
import dev.jstech.core.material.NamingBalance;
import dev.jstech.core.util.DeclarationError;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestMaterials;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The series' catalogue: the 118 known elements as data, isotopes tied to their elements, the elements beyond the known
 * table under their systematic names until a discoverer names them, the rules a name and a symbol follow, an operator
 * taking a name back, the series' names where discoverers do not name elements, and the exotic forms of matter.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class CatalogueGameTests {

    private static final String ARENA = "empty";

    private CatalogueGameTests() {
    }

    @GameTest(template = ARENA)
    public static void elements_areTheKnownTableAsData(final GameTestHelper helper) {
        helper.assertTrue(Elements.all().size() == 118, "118 elements");
        final Set<Integer> numbers = new HashSet<>();
        for (final Element element : Element.values()) {
            numbers.add(element.number());
            final Material material = Elements.of(element);
            helper.assertTrue(material.id().equals(ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
                    element.path())) && material.value(MaterialProperties.ATOMIC_NUMBER).orElseThrow()
                    == element.number(), element + " is jscore:" + element.path() + " with its atomic number");
        }
        helper.assertTrue(numbers.size() == 118 && numbers.contains(1) && numbers.contains(118), "1 to 118, each once");
        final Material iron = Elements.of(Element.IRON);
        helper.assertTrue(iron.value(MaterialProperties.SYMBOL).orElseThrow().equals("Fe")
                && iron.name().english().equals("Iron"), "iron is Fe");
        helper.assertTrue(Elements.of(Element.MERCURY).roomState() == MatterState.LIQUID
                && Elements.of(Element.BROMINE).roomState() == MatterState.LIQUID, "mercury and bromine are liquids");
        helper.assertTrue(Elements.of(Element.SILICON).kind() == MaterialKind.CRYSTAL
                && Elements.of(Element.HELIUM).kind() == MaterialKind.GAS, "silicon a crystal, helium a gas");
        helper.assertTrue(Elements.of(Element.HYDROGEN).takes(MatterState.PLASMA)
                && Elements.of(Element.HYDROGEN).takes(MatterState.LIQUID), "hydrogen liquefied and as plasma");
        helper.assertTrue(Element.byNumber(79).orElseThrow() == Element.GOLD && Element.byNumber(119).isEmpty(),
                "found by atomic number");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void isotopes_areTiedToTheirElementByTheirMassNumber(final GameTestHelper helper) {
        final Material uranium = Elements.of(Element.URANIUM);
        helper.assertTrue(Isotopes.URANIUM_235.isotope().orElseThrow().element() == uranium
                && Isotopes.URANIUM_235.isotope().orElseThrow().massNumber() == 235, "uranium-235 is uranium's");
        helper.assertTrue(Isotopes.URANIUM_235.name().english().equals("Uranium-235"), "named after it");
        helper.assertTrue(Isotopes.URANIUM_235.value(MaterialProperties.SYMBOL).orElseThrow().equals("²³⁵U"),
                "and written as chemists write it");
        helper.assertTrue(Isotopes.DEUTERIUM.name().english().equals("Deuterium")
                && Isotopes.DEUTERIUM.value(MaterialProperties.SYMBOL).orElseThrow().equals("D"),
                "deuterium has a name and a symbol of its own");
        helper.assertTrue(Isotopes.of(uranium).containsAll(List.of(Isotopes.URANIUM_235,
                Isotopes.URANIUM_238)), "an element's isotopes are found from it");
        final Material flerovium = Isotopes.stable(Element.FLEROVIUM).orElseThrow();
        helper.assertTrue(flerovium.isotope().orElseThrow().massNumber() == 114 + Isotopes.ISLAND_NEUTRONS
                && Isotopes.stable().size() == 15, "the island of stability's isotopes, rutherfordium to oganesson");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void superheavies_showTheirSystematicNameUntilDiscovered(final GameTestHelper helper) {
        helper.assertTrue(Superheavies.all().size() == Superheavies.LAST - Superheavies.FIRST + 1, "119 to 250");
        final Material first = Superheavies.of(119);
        helper.assertTrue(first.id().equals(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "ununennium"))
                && first.name().english().equals("Ununennium")
                && first.value(MaterialProperties.SYMBOL).orElseThrow().equals("Uue"),
                "119 is ununennium, Uue, until it is discovered");
        helper.assertTrue(Superheavies.seriesName(250).orElseThrow().english().equals("Aeternium")
                && Superheavies.seriesSymbol(119).orElseThrow().equals("Gv"), "and has a series' name to keep");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void naming_followsTheRulesAndCanBeTakenBack(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final ServerPlayer discoverer = FakePlayerFactory.get(helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "Discoverer"));
        final ServerPlayer other = FakePlayerFactory.get(helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "Bystander"));
        final Material element = Superheavies.of(121);
        try {
            helper.assertTrue(ElementNaming.refusal(server, discoverer.getUUID(), element, "Joaonium", "Jo")
                    .isPresent(), "an element not yet discovered is named by nobody");
            helper.assertTrue(ElementNaming.discover(discoverer, element), "discovered");
            helper.assertTrue(!ElementNaming.discover(other, element), "only once");
            helper.assertTrue(refused(server, other, element, "Joaonium", "Jo"), "only its discoverer names it");
            helper.assertTrue(refused(server, discoverer, element, "Joao2", "Jo"), "letters only");
            helper.assertTrue(refused(server, discoverer, element, "A".repeat(NamingBalance.longest() + 1), "Jo"),
                    "no longer than the world allows");
            helper.assertTrue(refused(server, discoverer, element, "Iron", "Jo"), "no other element's name");
            helper.assertTrue(refused(server, discoverer, element, "Genevium", "Jo"), "nor a series' name");
            helper.assertTrue(refused(server, discoverer, element, "Joaonium", "Fe"), "no other element's symbol");
            helper.assertTrue(refused(server, discoverer, element, "Joaonium", "Joa"), "a symbol of two letters");
            NamingBalance.setRequireIum(true);
            helper.assertTrue(refused(server, discoverer, element, "Joaon", "Jo"), "-ium, where the world asks");
            helper.assertTrue(ElementNaming.name(discoverer, element, "Joaonium", "jo").isEmpty(), "named");
            helper.assertTrue(element.name().english().equals("Joaonium")
                    && element.value(MaterialProperties.SYMBOL).orElseThrow().equals("Jo"),
                    "the element takes its name and its symbol, written as a symbol is");
            helper.assertTrue(refused(server, discoverer, element, "Otherium", "Ot"), "once given, not changed");
            helper.assertTrue(ElementNaming.unname(server, element) && element.name().english().equals("Unbiunium"),
                    "an operator takes it back, and it shows its systematic name again");
            helper.assertTrue(ElementNaming.name(discoverer, element, "Secondium", "Sd").isEmpty(),
                    "and its discoverer names it again");
        } finally {
            NamingBalance.setRequireIum(false);
            reset(server);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void naming_keepsTheSeriesNamesWhereDiscoverersDoNotName(final GameTestHelper helper) {
        final MinecraftServer server = helper.getLevel().getServer();
        final ServerPlayer discoverer = FakePlayerFactory.get(helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "Discoverer"));
        final Material teslium = Superheavies.of(120);
        try {
            NamingBalance.setDiscoverersName(false);
            helper.assertTrue(ElementNaming.discover(discoverer, teslium), "discovered");
            helper.assertTrue(teslium.name().english().equals("Teslium")
                    && teslium.value(MaterialProperties.SYMBOL).orElseThrow().equals("Tu"),
                    "it takes the series' name and symbol");
            helper.assertTrue(refused(server, discoverer, teslium, "Joaonium", "Jo"), "and nobody names it");
            final Material plain = Superheavies.of(124);
            ElementNaming.discover(discoverer, plain);
            helper.assertTrue(plain.name().english().equals("Unbiquadium"),
                    "an element with no series' name keeps its systematic one");
        } finally {
            NamingBalance.setDiscoverersName(true);
            reset(server);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void exoticMatter_isAKindOfItsOwnInTheStateEachCallsFor(final GameTestHelper helper) {
        helper.assertTrue(ExoticMatter.all().size() == 11 && ExoticMatter.all().stream()
                .allMatch(material -> material.kind() == MaterialKind.EXOTIC), "eleven exotic forms of matter");
        helper.assertTrue(ExoticMatter.QUARK_GLUON_PLASMA.roomState() == MatterState.PLASMA
                && ExoticMatter.TIME_CRYSTAL.roomState() == MatterState.SOLID
                && ExoticMatter.NEUTRONIUM.roomState() == MatterState.EXOTIC
                && ExoticMatter.NEUTRONIUM.value(MaterialProperties.ROOM_STATE).orElseThrow() == MatterState.EXOTIC,
                "each in its own state");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void minerals_containTheirElementsAndBearTheirMarks(final GameTestHelper helper) {
        final Mineral testite = TestMaterials.TESTITE.mineral().orElseThrow();
        helper.assertTrue(testite.family() == MineralFamilies.OXIDE
                && testite.main().material() == Elements.of(Element.URANIUM)
                && testite.byproducts().size() == 1 && testite.contains(Elements.of(Element.THORIUM))
                && testite.rocks().equals(Set.of(HostRocks.STONE, HostRocks.GRANITE)),
                "a mineral knows its main element, its byproducts, its family and its rocks");
        helper.assertTrue(NaturalRadioactivity.radioactive(TestMaterials.TESTITE)
                && MaterialMarks.of(TestMaterials.TESTITE).contains(NaturalRadioactivity.RADIOACTIVE),
                "one with uranium in it is naturally radioactive, and its things say so");
        helper.assertTrue(!NaturalRadioactivity.radioactive(Elements.of(Element.URANIUM)),
                "uranium itself is no mineral");
        try {
            HazardBalance.setNaturalRadioactivity(false);
            helper.assertTrue(!NaturalRadioactivity.radioactive(TestMaterials.TESTITE), "unless the world says not");
        } finally {
            HazardBalance.setNaturalRadioactivity(true);
        }
        helper.assertTrue(refusedDeclaration(() -> Materials.material(ResourceLocation.fromNamespaceAndPath(
                        JsTests.MODID, "not_a_mineral"), MaterialKind.METAL, Element.TIN.nameKey())
                        .contains(Elements.of(Element.TIN), 1.0).declare()), "only a mineral contains elements");
        helper.assertTrue(refusedDeclaration(() -> Materials.material(ResourceLocation.fromNamespaceAndPath(
                        JsTests.MODID, "too_much"), MaterialKind.MINERAL, Element.TIN.nameKey())
                        .mineral(MineralFamilies.OXIDE).contains(Elements.of(Element.TIN), 0.6)
                        .contains(Elements.of(Element.IRON), 0.5).declare()), "nor more than the whole");
        final Block ore = MaterialUses.oreBlock(TestMaterials.TESTITE, HostRocks.GRANITE).get();
        final List<ItemStack> drops = Block.getDrops(ore.defaultBlockState(), helper.getLevel(),
                helper.absolutePos(new BlockPos(1, 2, 1)), null);
        helper.assertTrue(drops.size() == 1
                && drops.get(0).getItem() == MaterialUses.item(TestMaterials.TESTITE, Forms.RAW).get(),
                "its ore in granite drops raw testite: " + drops);
        helper.assertTrue(Elements.hasGenericOre(Element.TIN) && Elements.hasGenericOre(Element.MERCURY)
                && !Elements.hasGenericOre(Element.NEODYMIUM) && !Elements.hasGenericOre(Element.CALCIUM),
                "tin has a generic ore, the rare earths and the salts' elements none");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void liquids_existOnlyWhereUsedNamedFromTheirMaterial(final GameTestHelper helper) {
        final MaterialLiquid tin = MaterialLiquids.of(SeriesMaterials.TIN).orElseThrow();
        helper.assertTrue(tin.id().equals(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "molten_tin"))
                && TestMaterials.MOLTEN_TIN.get() == tin.fluid() && tin.temperature() == 505,
                "tin molten, at the temperature it was used at");
        helper.assertTrue(english(tin.fluid().getFluidType().getDescription()).equals("Molten Tin")
                && english(new ItemStack(BuiltInRegistries.ITEM.get(tin.bucketId())).getHoverName())
                .equals("Molten Tin Bucket"), "named from its material");
        helper.assertTrue(tin.fluid().defaultFluidState().is(TagKey.create(Registries.FLUID,
                ResourceLocation.fromNamespaceAndPath("c", "molten_tin"))), "in its common tag");
        final MaterialLiquid gasoline = MaterialLiquids.of(SharedCatalogue.GASOLINE).orElseThrow();
        helper.assertTrue(gasoline.id().equals(SharedCatalogue.GASOLINE.id())
                && english(gasoline.fluid().getFluidType().getDescription()).equals("Gasoline"),
                "gasoline is itself, at room temperature");
        helper.assertTrue(MaterialLiquids.of(SharedCatalogue.DIESEL).isEmpty()
                && !BuiltInRegistries.FLUID.containsKey(MaterialLiquid.idOf(SharedCatalogue.DIESEL)),
                "diesel, which nobody uses, is not in the game");
        helper.assertTrue(refusedDeclaration(() -> MaterialLiquids.use(JsTests.MODID, SeriesMaterials.TIN, 505)),
                "a use after the game registered its things is refused");
        final BlockPos at = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.getLevel().setBlock(at, tin.fluid().defaultFluidState().createLegacyBlock(), Block.UPDATE_ALL);
        helper.assertTrue(helper.getLevel().getFluidState(at).getType() == tin.fluid(), "and pours into the world");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void catalogueItems_existOnlyWhereUsed(final GameTestHelper helper) {
        helper.assertTrue(BuiltInRegistries.ITEM.get(TestMaterials.GADGET.id()) == TestMaterials.GADGET_ITEM.get()
                && english(new ItemStack(TestMaterials.GADGET_ITEM.get()).getHoverName()).equals("Test Gadget"),
                "an entry a mod uses, named as declared");
        helper.assertTrue(SharedCatalogue.components().stream().noneMatch(entry ->
                BuiltInRegistries.ITEM.containsKey(entry.id())), "the series' components, which nobody uses, are not");
        helper.assertTrue(SharedCatalogue.fuels().size() == 3 && SharedCatalogue.components().size() == 7,
                "three fuels and seven components in the catalogue");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void simplePath_closesOnlyWhereTheModsRuleAndIndustrialSay(final GameTestHelper helper) {
        try {
            helper.assertTrue(SimplePath.open(JsTests.MODID) && SimplePath.condition(JsTests.MODID).test(null),
                    "open while the mod's rule is off");
            TestMaterials.INDUSTRIAL_CHAIN_ONLY[0] = true;
            helper.assertTrue(SimplePath.open(JsTests.MODID) != ModList.get().isLoaded("jsindustrial"),
                    "closed by the rule only where J's Industrial is installed");
            helper.assertTrue(SimplePath.open("jstests_nobody"), "a mod with no rule keeps its path open");
        } finally {
            TestMaterials.INDUSTRIAL_CHAIN_ONLY[0] = false;
        }
        helper.succeed();
    }

    private static String english(final Component component) {
        return component.getString();
    }

    private static boolean refusedDeclaration(final Runnable declaration) {
        try {
            declaration.run();
            return false;
        } catch (final DeclarationError refusal) {
            return true;
        }
    }

    private static boolean refused(final MinecraftServer server, final ServerPlayer by, final Material element,
                                   final String name, final String symbol) {
        final Optional<?> refusal = ElementNaming.refusal(server, by.getUUID(), element, name, symbol);
        return refusal.isPresent();
    }

    private static void reset(final MinecraftServer server) {
        ElementNaming.DISCOVERED.set(server, Map.of());
        MaterialNames.NAMES.set(server, Map.of());
    }
}
