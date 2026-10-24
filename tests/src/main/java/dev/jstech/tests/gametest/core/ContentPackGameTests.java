/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.jstech.core.JsCore;
import dev.jstech.core.machine.DataMachineBlock;
import dev.jstech.core.material.Forms;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialUses;
import dev.jstech.core.material.Materials;
import dev.jstech.core.pack.ContentPacks;
import dev.jstech.core.resource.RuntimePacks;
import dev.jstech.tests.JsTests;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The test mod's content pack, {@code jstech_content/jstestpack.json}, declared with no Java: a lamp that needs a
 * stone pickaxe and gives light, a block with no item, a token stacking to sixteen, a material with its ingot, its
 * dust and its ore, a kind of processing machine with a recipe, and the terminal of a machine declared as data,
 * each with the names, models and drops the Core made for it as the game loaded.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ContentPackGameTests {

    private static final String ARENA = "empty";
    private static final String PACK = "jstestpack";
    private static final int LAMP_LIGHT = 12;
    private static final int TOKEN_STACK = 16;
    private static final BlockPos AT = new BlockPos(1, 1, 1);

    private ContentPackGameTests() {
    }

    @GameTest(template = ARENA)
    public static void contentPack_registersWhatItDeclares(final GameTestHelper helper) {
        helper.assertTrue(ContentPacks.isPack(PACK), "the pack's namespace is a content pack's");
        final Block lamp = block("test_lamp");
        final BlockState lit = lamp.defaultBlockState();
        helper.assertTrue(lit.getLightEmission() == LAMP_LIGHT, "the lamp gives the light it declares");
        helper.assertTrue(lit.requiresCorrectToolForDrops(), "and needs its tool to drop");
        helper.assertTrue(lamp.asItem() != Items.AIR, "and has an item");
        helper.assertTrue(block("test_slab_of_light").asItem() == Items.AIR, "a block declared with no item has none");
        helper.assertTrue(item("test_token").getDefaultMaxStackSize() == TOKEN_STACK, "the token stacks to sixteen");
        final Optional<Material> mythril = Materials.byId(id("test_mythril"));
        helper.assertTrue(mythril.isPresent(), "the material is in the catalogue");
        helper.assertTrue(MaterialUses.item(mythril.get(), Forms.INGOT).get() != Items.AIR
                && MaterialUses.item(mythril.get(), Forms.DUST).get() != Items.AIR, "with its ingot and its dust");
        helper.assertTrue(BuiltInRegistries.RECIPE_TYPE.containsKey(id("test_pack_pressing")),
                "the kind of processing machine is a recipe type");
        helper.assertTrue(helper.getLevel().getRecipeManager().getRecipes().stream()
                        .anyMatch(holder -> holder.id().equals(id("test_pack_pressing/token"))),
                "whose recipe a data pack gives");
        helper.assertTrue(block("test_pack_press") instanceof DataMachineBlock machine
                && machine.machine().equals(id("test_pack_press")), "the press reads the machine file of its id");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void contentPack_blocksDropThemselvesWithTheirTool(final GameTestHelper helper) {
        helper.setBlock(AT, block("test_lamp"));
        final BlockPos at = helper.absolutePos(AT);
        final BlockState state = helper.getLevel().getBlockState(at);
        final List<ItemStack> mined = Block.getDrops(state, helper.getLevel(), at, null, null,
                new ItemStack(Items.STONE_PICKAXE));
        helper.assertTrue(mined.size() == 1 && mined.getFirst().is(block("test_lamp").asItem()),
                "a stone pickaxe takes the lamp whole: " + mined);
        // A player's hand that is not the right tool takes nothing, as the game decides when they break it.
        helper.assertTrue(new ItemStack(Items.STONE_PICKAXE).isCorrectToolForDrops(state),
                "a stone pickaxe is the right tool for the lamp");
        helper.assertTrue(!new ItemStack(Items.WOODEN_PICKAXE).isCorrectToolForDrops(state)
                && !ItemStack.EMPTY.isCorrectToolForDrops(state), "a wooden one or a bare hand is not");
        helper.setBlock(AT, Blocks.AIR);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void contentPack_artAndNamesAreMadeAsTheGameLoads(final GameTestHelper helper) {
        final PackResources art = RuntimePacks.open(ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
                "content_art"), PackType.CLIENT_RESOURCES).orElseThrow();
        final JsonObject english = read(art, "lang/en_us.json");
        helper.assertTrue("Test Pack Lamp".equals(string(english, "block.jstestpack.test_lamp"))
                        && "Test Pack Token".equals(string(english, "item.jstestpack.test_token"))
                        && "Test Mythril".equals(string(english, "jstestpack.material.test_mythril"))
                        && "Test Pack".equals(string(english, "itemGroup.jstestpack.main")),
                "the names are in English: " + english);
        final JsonObject lampModel = read(art, "models/block/test_lamp.json");
        helper.assertTrue("jstestpack:block/test_lamp".equals(lampModel.getAsJsonObject("textures").get("all")
                .getAsString()), "the lamp is a cube drawn with its texture: " + lampModel);
        final JsonObject pressState = read(art, "blockstates/test_pack_press.json");
        helper.assertTrue(pressState.getAsJsonObject("variants").has("facing=west"),
                "the press turns the way it faces: " + pressState);
        helper.assertTrue(read(art, "models/item/test_token.json").has("textures"), "the token is drawn flat");
        helper.succeed();
    }

    private static Block block(final String path) {
        return BuiltInRegistries.BLOCK.get(id(path));
    }

    private static Item item(final String path) {
        return BuiltInRegistries.ITEM.get(id(path));
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(PACK, path);
    }

    private static JsonObject read(final PackResources pack, final String path) {
        final IoSupplier<InputStream> file = pack.getResource(PackType.CLIENT_RESOURCES, id(path));
        if (file == null) {
            throw new IllegalStateException("the content packs' art holds no " + path);
        }
        try (InputStream in = file.get()) {
            return JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String string(final JsonObject json, final String key) {
        return json.has(key) ? json.get(key).getAsString() : "";
    }
}
