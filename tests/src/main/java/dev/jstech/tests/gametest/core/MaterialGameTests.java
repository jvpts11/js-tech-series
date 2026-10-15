/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import dev.jstech.core.JsCore;
import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.SeriesMaterials;
import dev.jstech.core.material.Form;
import dev.jstech.core.material.Forms;
import dev.jstech.core.material.HostRocks;
import dev.jstech.core.material.MaterialFormBlock;
import dev.jstech.core.material.MaterialFormItem;
import dev.jstech.core.material.MaterialForm;
import dev.jstech.core.material.MaterialItems;
import dev.jstech.core.material.MaterialKind;
import dev.jstech.core.material.MaterialProperties;
import dev.jstech.core.material.MaterialProperty;
import dev.jstech.core.material.MaterialThing;
import dev.jstech.core.material.MaterialUses;
import dev.jstech.core.material.MaterialValues;
import dev.jstech.core.material.Materials;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.material.MineralFamilies;
import dev.jstech.core.material.ModMaterial;
import dev.jstech.core.material.RegisteredThing;
import dev.jstech.core.resource.RuntimePacks;
import dev.jstech.core.util.DeclarationError;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestMaterials;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The open catalogue of materials: its registries filled with the Core's declarations, the things of a form existing
 * only where a mod uses it, named from the form and the material, each in its common tag with its drops, the game's own
 * things kept for the forms it has, and the values a data pack gives over the declared ones.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MaterialGameTests {

    private static final String ARENA = "empty";

    private MaterialGameTests() {
    }

    @GameTest(template = ARENA)
    public static void registries_holdTheCoresDeclarations(final GameTestHelper helper) {
        helper.assertTrue(Forms.all().size() >= 21 && Forms.all().subList(0, 21).stream()
                .allMatch(form -> form.id().getNamespace().equals(JsCore.MODID)), "the Core's 21 forms, first");
        helper.assertTrue(MaterialProperties.all().containsAll(List.of(MaterialProperties.SYMBOL,
                MaterialProperties.ATOMIC_NUMBER, MaterialProperties.FORMULA, MaterialProperties.COLOUR,
                MaterialProperties.ROOM_STATE)), "the Core's five properties");
        helper.assertTrue(MineralFamilies.all().size() >= 11, "the eleven mineral families");
        helper.assertTrue(HostRocks.all().size() >= 12 && HostRocks.all().get(1) == HostRocks.DEEPSLATE,
                "the game's twelve rocks");
        helper.assertTrue(Materials.byId(SeriesMaterials.TIN.id()).orElseThrow() == SeriesMaterials.TIN
                && SeriesMaterials.TIN.value(MaterialProperties.SYMBOL).orElseThrow().equals("Sn")
                && SeriesMaterials.TIN.value(MaterialProperties.ATOMIC_NUMBER).orElseThrow() == 50,
                "tin, with its symbol and atomic number");
        helper.assertTrue(SeriesMaterials.IRON.roomState() == MatterState.SOLID
                && SeriesMaterials.IRON.takes(MatterState.LIQUID),
                "iron is solid and may be molten");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void declarations_thatCannotBeAreRefused(final GameTestHelper helper) {
        helper.assertTrue(refused(() -> Materials.material(SeriesMaterials.TIN.id(), MaterialKind.METAL,
                Element.TIN.nameKey()).declare()), "a material declared twice");
        helper.assertTrue(refused(() -> MaterialUses.use(JsTests.MODID, SeriesMaterials.TIN, Forms.PLATE)),
                "a use declared after the game registered its things");
        helper.assertTrue(refused(() -> MaterialUses.use(JsTests.MODID, SeriesMaterials.TIN, Forms.ORE)),
                "an ore declared without its rocks");
        helper.assertTrue(refused(() -> Materials.material(ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
                        "wrong"), MaterialKind.GEM, Element.TIN.nameKey())
                .provided(Forms.INGOT, () -> Items.IRON_INGOT).declare()), "a gem given an ingot");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void uses_registerOnlyWhatSomeModUses(final GameTestHelper helper) {
        helper.assertTrue(BuiltInRegistries.ITEM.get(id("tin_plate")) == TestMaterials.TIN_PLATE.get()
                && TestMaterials.TIN_PLATE.get() instanceof MaterialFormItem, "tin's plate, which a mod uses");
        helper.assertTrue(!BuiltInRegistries.ITEM.containsKey(id("copper_wire")),
                "no copper wire, which no mod uses");
        helper.assertTrue(TestMaterials.IRON_INGOT.get() == Items.IRON_INGOT
                && !BuiltInRegistries.ITEM.containsKey(id("iron_ingot")), "the game's own iron ingot, not a second");
        helper.assertTrue(BuiltInRegistries.ITEM.containsKey(
                ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "testium_dust")),
                "another mod's material, in its own namespace");
        for (final RegisteredThing thing : MaterialUses.registered()) {
            helper.assertTrue(thing.thing().material().provided(thing.thing().form()).isEmpty(),
                    "nothing another mod has is registered again: " + thing.id());
        }
        helper.assertTrue(BuiltInRegistries.BLOCK.get(id("tuff_iron_ore")) instanceof MaterialFormBlock
                && !BuiltInRegistries.BLOCK.containsKey(id("iron_ore"))
                && MaterialUses.oreBlock(SeriesMaterials.IRON, HostRocks.STONE).get() == Blocks.IRON_ORE,
                "iron's ore in a rock the game has none in, and the game's own where it has one");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void things_areNamedFromTheirFormAndMaterial(final GameTestHelper helper) {
        helper.assertTrue(english(TestMaterials.TIN_PLATE.get()).equals("Tin Plate"), "Tin Plate");
        helper.assertTrue(english(MaterialUses.item(SeriesMaterials.TIN, Forms.BLOCK).get()).equals("Block of Tin"),
                "Block of Tin");
        helper.assertTrue(english(MaterialUses.item(SeriesMaterials.TIN, Forms.RAW).get()).equals("Raw Tin"),
                "Raw Tin");
        helper.assertTrue(english(MaterialUses.oreBlock(SeriesMaterials.TIN, HostRocks.DEEPSLATE).get().asItem())
                .equals("Deepslate Tin Ore"), "Deepslate Tin Ore");
        helper.assertTrue(english(MaterialUses.oreBlock(SeriesMaterials.TIN, HostRocks.STONE).get().asItem())
                .equals("Tin Ore"), "Tin Ore");
        helper.assertTrue(english(TestMaterials.TESTIUM_DUST.get()).equals("Testium Dust"), "Testium Dust");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void things_mapBackToTheirMaterialAndForm(final GameTestHelper helper) {
        helper.assertTrue(Materials.of(TestMaterials.TIN_PLATE.get()).orElseThrow()
                .equals(new MaterialThing(SeriesMaterials.TIN, Forms.PLATE)), "tin plate is tin as a plate");
        helper.assertTrue(Materials.of(Items.IRON_INGOT).orElseThrow()
                .equals(new MaterialThing(SeriesMaterials.IRON, Forms.INGOT)), "the game's iron ingot is iron's");
        helper.assertTrue(Materials.of(Items.STICK).isEmpty(), "a stick is no material's");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void things_comeWithTheirTagsAndDrops(final GameTestHelper helper) {
        helper.assertTrue(new ItemStack(TestMaterials.TIN_PLATE.get()).is(itemTag("c", "plates/tin"))
                && new ItemStack(TestMaterials.TIN_PLATE.get()).is(itemTag("c", "plates")), "c:plates/tin, c:plates");
        final Block ore = MaterialUses.oreBlock(SeriesMaterials.TIN, HostRocks.GRANITE).get();
        final BlockState state = ore.defaultBlockState();
        helper.assertTrue(state.is(BlockTags.MINEABLE_WITH_PICKAXE) && state.is(blockTag("c", "ores/tin"))
                && state.is(blockTag("c", "ores_in_ground/granite")), "the ore is mined with a pickaxe, in its tags");
        helper.assertTrue(state.requiresCorrectToolForDrops()
                && ore.defaultDestroyTime() == Blocks.GRANITE.defaultDestroyTime() + 1.5f,
                "and a little harder than its rock");
        final BlockPos at = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.getLevel().setBlock(at, state, Block.UPDATE_ALL);
        final List<ItemStack> drops = Block.getDrops(state, helper.getLevel(), at, null);
        helper.assertTrue(drops.size() == 1 && drops.get(0).getItem()
                        == MaterialUses.item(SeriesMaterials.TIN, Forms.RAW).get(),
                "it drops raw tin: " + drops);
        final Block storage = MaterialUses.block(SeriesMaterials.TIN, Forms.BLOCK).get();
        final List<ItemStack> blockDrops = Block.getDrops(storage.defaultBlockState(), helper.getLevel(), at, null);
        helper.assertTrue(blockDrops.size() == 1 && blockDrops.get(0).getItem() == storage.asItem(),
                "a block of tin drops itself: " + blockDrops);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void dataPack_isMadeFromTheUses(final GameTestHelper helper) {
        final PackResources pack = RuntimePacks.open(ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
                "material_data"), PackType.SERVER_DATA).orElseThrow();
        helper.assertTrue(pack.getResource(PackType.SERVER_DATA, ResourceLocation.fromNamespaceAndPath("c",
                "tags/item/plates/tin.json")) != null, "the tag of tin's plates");
        helper.assertTrue(pack.getResource(PackType.SERVER_DATA, ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
                "loot_table/blocks/deepslate_tin_ore.json")) != null, "the drops of tin's ore in deepslate");
        helper.assertTrue(pack.getResource(PackType.SERVER_DATA, ResourceLocation.fromNamespaceAndPath("c",
                "tags/item/wires/copper.json")) == null, "nothing for what nobody uses");
        helper.assertTrue(pack.getNamespaces(PackType.SERVER_DATA).containsAll(List.of("c", "minecraft",
                JsCore.MODID)), "and only data");
        pack.close();
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void values_ofADataPackComeOverTheDeclaredOnes(final GameTestHelper helper) {
        final Map<ResourceLocation, MaterialValues.File> before = MaterialValues.FILES.entries();
        try {
            helper.assertTrue(MaterialValues.value(SeriesMaterials.TIN, MaterialProperties.SYMBOL, false)
                    .orElseThrow().equals("Sn"), "tin's symbol as declared");
            MaterialValues.FILES.replace(Map.of(SeriesMaterials.TIN.id(), new MaterialValues.File(
                    Map.of(MaterialProperties.SYMBOL.id(), new Dynamic<>(JsonOps.INSTANCE, new JsonPrimitive("Tn"))),
                    Map.of(MaterialProperties.ATOMIC_NUMBER.id(), MaterialProperty.Shown.ALWAYS))));
            helper.assertTrue(MaterialValues.value(SeriesMaterials.TIN, MaterialProperties.SYMBOL, false)
                    .orElseThrow().equals("Tn"), "a data pack's symbol over it");
            helper.assertTrue(MaterialValues.shown(SeriesMaterials.TIN, MaterialProperties.ATOMIC_NUMBER, false)
                    == MaterialProperty.Shown.ALWAYS, "and its line shown always, as the file says");
            final List<MaterialValues.Line> lines = MaterialValues.lines(SeriesMaterials.IRON, false);
            helper.assertTrue(lines.stream().anyMatch(line -> line.property() == MaterialProperties.SYMBOL
                            && line.shown() == MaterialProperty.Shown.ALWAYS
                            && line.value().english().equals("Fe"))
                    && lines.stream().anyMatch(line -> line.property() == MaterialProperties.ATOMIC_NUMBER
                            && line.shown() == MaterialProperty.Shown.SHIFT)
                    && lines.stream().noneMatch(line -> line.property() == MaterialProperties.COLOUR),
                    "iron's lines: its symbol always, its atomic number with Shift, never its colour");
        } finally {
            MaterialValues.FILES.replace(before);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void oldCatalogue_keepsItsIdsUnshared(final GameTestHelper helper) {
        final Item dust = MaterialItems.get(ModMaterial.IRON, MaterialForm.DUST).get();
        helper.assertTrue(BuiltInRegistries.ITEM.getKey(dust).equals(id("iron_dust")), "iron dust keeps its id");
        helper.assertTrue(Materials.of(dust).orElseThrow().equals(new MaterialThing(SeriesMaterials.IRON,
                Forms.DUST)), "and is iron as a dust");
        helper.assertTrue(!MaterialUses.shared(dust), "but no mod has said it shares it yet");
        helper.assertTrue(english(dust).equals("Iron Dust"), "named as before");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void everyCoreForm_fitsTheKindsItSaysItDoes(final GameTestHelper helper) {
        for (final Form form : Forms.all()) {
            helper.assertTrue(!form.kinds().isEmpty(), form + " fits some kind");
        }
        helper.assertTrue(SeriesMaterials.TIN.fits(Forms.WIRE) && !SeriesMaterials.TIN.fits(Forms.GEM)
                && !SeriesMaterials.TIN.fits(Forms.WAFER), "a metal takes a wire, not a gem nor a wafer");
        helper.assertTrue(SeriesMaterials.TIN.oreDrop().orElseThrow() == Forms.RAW, "a metal's ore drops its raw form");
        helper.succeed();
    }

    private static boolean refused(final Runnable declaration) {
        try {
            declaration.run();
            return false;
        } catch (final DeclarationError refusal) {
            return true;
        }
    }

    private static String english(final Item item) {
        return new ItemStack(item).getHoverName().getString();
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsCore.MODID, path);
    }

    private static TagKey<Item> itemTag(final String namespace, final String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(namespace, path));
    }

    private static TagKey<Block> blockTag(final String namespace, final String path) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(namespace, path));
    }
}
