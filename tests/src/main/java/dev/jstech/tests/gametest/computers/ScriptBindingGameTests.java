/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.hardware.CpuSocketId;
import dev.jstech.computers.hardware.CpuSpec;
import dev.jstech.computers.hardware.HardwareCodecs;
import dev.jstech.computers.hardware.Isas;
import dev.jstech.computers.hardware.RamGeneration;
import dev.jstech.computers.hardware.RamSpec;
import dev.jstech.computers.os.media.InstallerStacks;
import dev.jstech.computers.registry.ComputingContent;
import dev.jstech.computers.registry.HardwareRegistry;
import dev.jstech.computers.scripting.JsComputersScripts;
import dev.jstech.computers.scripting.ScriptedHardware;
import dev.jstech.core.content.ModContent;
import dev.jstech.core.content.ScriptBinding;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestHardware;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * What a modpack's scripts reach as {@code JsComputers}, and the scripts themselves when KubeJS is in the run
 * ({@code -PwithKubeJs}): the test script in {@code tests/src/kubejs} writes a recipe for an installer and one that
 * takes anything of the Vintage era, the way a modpack would.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ScriptBindingGameTests {

    private static final String ARENA = "empty";
    private static final ResourceLocation PLAIN_RECIPE = ResourceLocation.fromNamespaceAndPath("jstests",
            "vintage_personal_computer");
    private static final ResourceLocation INSTALLER_RECIPE = ResourceLocation.fromNamespaceAndPath("jstests",
            "frames_95_installer");
    private static final ResourceLocation VINTAGE_RECIPE = ResourceLocation.fromNamespaceAndPath("jstests",
            "any_vintage_thing");

    private ScriptBindingGameTests() {
    }

    @GameTest(template = ARENA)
    public static void kubeJs_isLoadedExactlyWhenTheRunAskedForIt(final GameTestHelper helper) {
        helper.assertTrue(ModList.get().isLoaded("kubejs") == kubeJsAsked(),
                "KubeJS is loaded exactly when the run asked for it");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void computers_handTheScriptsJsComputers(final GameTestHelper helper) {
        final List<ScriptBinding> bindings = ModContent.of(JsComputers.MODID).declaredScriptBindings();
        helper.assertTrue(bindings.contains(ComputingContent.SCRIPTS) && "JsComputers".equals(ComputingContent.SCRIPTS
                        .name()) && ComputingContent.SCRIPTS.statics() == JsComputersScripts.class,
                "J's Computers declares JsComputers; got " + bindings);
        helper.succeed();
    }

    /** The binding's calls, which work with or without a scripting mod: they are plain calls into the mod. */
    @GameTest(template = ARENA)
    public static void binding_givesInstallersAndTheListsOfAnEra(final GameTestHelper helper) {
        final ResourceLocation frames95 = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "frames_95");
        helper.assertTrue(ItemStack.isSameItemSameComponents(JsComputersScripts.installer("frames_95"),
                InstallerStacks.forSystem(frames95)), "an id without a namespace is J's Computers' own");
        helper.assertTrue(ItemStack.isSameItemSameComponents(JsComputersScripts.installer("jsc:frames_95"),
                InstallerStacks.forSystem(frames95)), "and the same with it");
        helper.assertTrue(JsComputersScripts.installer("no_such_system").isEmpty(), "an unknown system gives nothing");
        helper.assertTrue(!JsComputersScripts.readyDisk("frames_95").isEmpty(), "Frames 95 comes on a ready disk");
        helper.assertFalse(JsComputersScripts.programInstaller("isms").isEmpty(),
                "the ISMS, the page's example, is a program a player installs");
        helper.assertTrue(JsComputersScripts.eras().equals(List.of("vintage", "legacy", "transition", "standard",
                "advanced")), "the eras with things in the game, oldest first; got " + JsComputersScripts.eras());
        helper.assertTrue(JsComputersScripts.systems("legacy").contains(frames95.toString()),
                "Frames 95 is a Legacy system; got " + JsComputersScripts.systems("legacy"));
        helper.assertFalse(JsComputersScripts.systems("vintage").contains(frames95.toString()),
                "and not a Vintage one");
        for (final String era : JsComputersScripts.eras()) {
            for (final String program : JsComputersScripts.programs(era)) {
                helper.assertFalse(JsComputersScripts.programInstaller(program).isEmpty(),
                        program + " is listed as installable and has an installer");
            }
        }
        boolean refused = false;
        try {
            JsComputersScripts.systems("bronze_age");
        } catch (final IllegalArgumentException expected) {
            refused = expected.getMessage().contains("vintage");
        }
        helper.assertTrue(refused, "an era that does not exist is refused, naming the ones that do");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void scriptRecipe_makesAPlainItemByItsId(final GameTestHelper helper) {
        final Optional<RecipeHolder<?>> recipe = helper.getLevel().getRecipeManager().byKey(PLAIN_RECIPE);
        if (!kubeJsAsked()) {
            helper.assertTrue(recipe.isEmpty(), "without KubeJS no script made a recipe");
            helper.succeed();
            return;
        }
        helper.assertTrue(recipe.isPresent(), "the test script's plain recipe exists");
        final ItemStack made = recipe.get().value().getResultItem(helper.getLevel().registryAccess());
        helper.assertTrue(made.is(ComputingModule.VINTAGE_PERSONAL_COMPUTER.get().asItem()),
                "it makes a Vintage Personal Computer; got " + made);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void scriptRecipe_makesTheInstallerItNames(final GameTestHelper helper) {
        final Optional<RecipeHolder<?>> recipe = helper.getLevel().getRecipeManager().byKey(INSTALLER_RECIPE);
        if (!kubeJsAsked()) {
            helper.assertTrue(recipe.isEmpty(), "without KubeJS no script made a recipe");
            helper.succeed();
            return;
        }
        helper.assertTrue(recipe.isPresent(), "the test script's installer recipe exists");
        final ItemStack made = recipe.get().value().getResultItem(helper.getLevel().registryAccess());
        helper.assertTrue(ItemStack.isSameItemSameComponents(made, InstallerStacks.forSystem(
                        ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "frames_95"))),
                "it makes Frames 95's installer, written on its CD; got " + made);
        helper.succeed();
    }

    /** The test script goes through the Vintage systems, as the page shows, giving each an installer recipe. */
    @GameTest(template = ARENA)
    public static void scriptLoop_givesEverySystemOfAnEraAnInstallerRecipe(final GameTestHelper helper) {
        if (!kubeJsAsked()) {
            helper.succeed();
            return;
        }
        final List<String> systems = JsComputersScripts.systems("vintage");
        helper.assertFalse(systems.isEmpty(), "the Vintage era has systems");
        for (final String system : systems) {
            final ItemStack installer = JsComputersScripts.installer(system);
            final boolean made = helper.getLevel().getRecipeManager().getRecipes().stream().anyMatch(recipe ->
                    ItemStack.isSameItemSameComponents(recipe.value().getResultItem(helper.getLevel()
                            .registryAccess()), installer));
            helper.assertTrue(made, system + " has an installer recipe from the script's loop");
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void scriptRecipe_takesAnythingOfAnEraByItsTag(final GameTestHelper helper) {
        final Optional<RecipeHolder<?>> recipe = helper.getLevel().getRecipeManager().byKey(VINTAGE_RECIPE);
        if (!kubeJsAsked()) {
            helper.assertTrue(recipe.isEmpty(), "without KubeJS no script made a recipe");
            helper.succeed();
            return;
        }
        helper.assertTrue(recipe.isPresent(), "the test script's Vintage recipe exists");
        final List<Ingredient> ingredients = recipe.get().value().getIngredients();
        helper.assertTrue(ingredients.size() == 1, "it takes one thing; got " + ingredients.size());
        helper.assertTrue(ingredients.getFirst().test(new ItemStack(ComputingModule.FLOPPY_DISK.get())),
                "a floppy disk, a Vintage medium, is taken");
        helper.assertFalse(ingredients.getFirst().test(new ItemStack(ComputingModule.CD_ROM.get())),
                "a CD, a Legacy one, is not");
        helper.succeed();
    }

    /**
     * A part's sheet read as a startup script writes it, an object of plain fields whose numbers come as doubles, with
     * the codec the script's declaration is read with; a field written wrong is refused, naming what it was of.
     */
    @GameTest(template = ARENA)
    public static void partSheet_readsFromAnObjectAsAScriptWritesIt(final GameTestHelper helper) {
        final CpuSpec cpu = ScriptedHardware.read(HardwareCodecs.PROCESSOR, Map.of("era", "legacy", "isa", "x86",
                "socket", "jsc:socket_a", "cores", 1.0, "freq_mhz", 1300.0, "tdp_watts", 60.0), "a test processor");
        helper.assertTrue(cpu.era() == HardwareEra.LEGACY && cpu.isa() == Isas.X86 && cpu.cores() == 1
                        && cpu.freqMhz() == 1300 && cpu.socket().equals(CpuSocketId.SOCKET_A),
                "read field by field; got " + cpu);
        boolean refused = false;
        try {
            ScriptedHardware.read(HardwareCodecs.MEMORY, Map.of("era", "legacy", "generation", "ddr9",
                    "buffer_items", 1.0, "tdp_watts", 1.0), "a test module");
        } catch (final IllegalArgumentException expected) {
            refused = expected.getMessage().contains("a test module") && expected.getMessage().contains("ddr9");
        }
        helper.assertTrue(refused, "a generation no one declared is refused, saying what and of what");
        helper.succeed();
    }

    /** The test startup script declares the test mod's memory module, in a run with KubeJS, and nothing without it. */
    @GameTest(template = ARENA)
    public static void startupScript_declaresAPartAsAnObject(final GameTestHelper helper) {
        final RamSpec ram = HardwareRegistry.ram(new ItemStack(TestHardware.SCRIPTED_RAM.get()));
        if (!kubeJsAsked()) {
            helper.assertTrue(ram == null, "without KubeJS no script declared the module");
            helper.succeed();
            return;
        }
        helper.assertTrue(ram != null && ram.era() == HardwareEra.ADVANCED && ram.generation() == RamGeneration.DDR5
                && ram.bufferItems() == 4096, "the script's module is an Advanced DDR5 module of 4096; got " + ram);
        helper.succeed();
    }

    private static boolean kubeJsAsked() {
        return "present".equals(System.getProperty("jsc.gametests.kubejs"));
    }
}
