/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import dev.jstech.core.multiblock.MultiblockPattern;
import dev.jstech.core.multiblock.MultiblockPatterns;
import dev.jstech.core.multiblock.Projectables;
import dev.jstech.core.multiblock.ShapePreview;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestMultiblocks;
import java.util.Optional;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Who a multiblock's controller is, for the views that show it before it is built: the block its pattern names, kept
 * in its file, else the controller block of a machine declared as data, of its own shape or of a pattern by id.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ProjectablesGameTests {

    private static final String ARENA = "empty";
    private static final String FURNACE_CONTROLLER = "jstests:test_furnace_controller";

    private ProjectablesGameTests() {
    }

    @GameTest(template = ARENA)
    public static void controllerOf_takesThePatternsOwnFirst(final GameTestHelper helper) {
        helper.assertTrue(Projectables.controllerOf(TestMultiblocks.FURNACE_ID).equals(Optional.of(FURNACE_CONTROLLER)),
                "the furnace's pattern names its controller, which a data machine on the same pattern does not"
                        + " replace; got " + Projectables.controllerOf(TestMultiblocks.FURNACE_ID));
        final ShapePreview preview = Projectables.preview(TestMultiblocks.FURNACE_ID, 0).orElseThrow();
        helper.assertTrue(preview.controllerBlock().equals(Optional.of(FURNACE_CONTROLLER)),
                "its preview shows the controller at its place");
        helper.assertTrue(preview.materials().keySet().stream().noneMatch(FURNACE_CONTROLLER::equals),
                "the controller is not among the materials, which are counted apart from it");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void controllerOf_findsTheBlockOfAMachineDeclaredAsData(final GameTestHelper helper) {
        final ResourceLocation baker = ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "baker");
        helper.assertTrue(Projectables.controllerOf(baker).equals(Optional.of("jstests:test_baker")),
                "a machine of its own shape has the block built on its file as its controller; got "
                        + Projectables.controllerOf(baker));
        helper.assertTrue(Projectables.preview(baker, 0).orElseThrow().controllerBlock()
                .equals(Optional.of("jstests:test_baker")), "its preview shows that block at the controller's place");
        helper.assertTrue(Projectables.controllerOf(ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "nowhere"))
                .isEmpty(), "a block naming a file there is not controls nothing");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void patternFile_keepsItsController(final GameTestHelper helper) {
        final JsonElement file = MultiblockPatterns.CODEC.encodeStart(JsonOps.INSTANCE, TestMultiblocks.FURNACE)
                .getOrThrow();
        helper.assertTrue(file.getAsJsonObject().get("controller").getAsString().equals(FURNACE_CONTROLLER),
                "the file names the controller; got " + file);
        final MultiblockPattern read = MultiblockPatterns.CODEC.parse(JsonOps.INSTANCE, file).getOrThrow();
        helper.assertTrue(read.controllerBlock().equals(Optional.of(FURNACE_CONTROLLER)),
                "and reads it back");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void declare_refusesAControllerThatIsNoId(final GameTestHelper helper) {
        final MultiblockPattern pattern = MultiblockPattern.builder("bad_controller").layer("#")
                .controller("Not An Id").build();
        try {
            MultiblockPatterns.declare(ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "bad_controller"),
                    pattern);
            helper.fail("a controller named by something that is no id is refused");
        } catch (final IllegalArgumentException refused) {
            helper.assertTrue(MultiblockPatterns.get(ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
                    "bad_controller")).isEmpty(), "and the pattern is not declared");
            helper.succeed();
        }
    }
}
