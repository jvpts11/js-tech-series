/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import com.google.gson.JsonParser;
import dev.jstech.core.command.SeriesCommands;
import dev.jstech.core.content.DeclaredModel;
import dev.jstech.core.model.ModelData;
import dev.jstech.core.model.ModelDefinition;
import dev.jstech.core.model.ModelReaders;
import dev.jstech.core.model.network.ModelAnimationPayload;
import dev.jstech.core.model.network.ModelAnimations;
import dev.jstech.core.model.network.ModelTarget;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestModels;
import dev.jstech.tests.TestSounds;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Core's models as the server sees them: every model a mod declares has its definition written beside its file,
 * the same the declaration says, and its file reads, whatever its format; what the server plays on a model crosses
 * the wire as it left, and a name too long for the wire is refused where it is made, not where it is sent; an item
 * the server animates is given its number once; and the models' commands are there.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ModelGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos PRESS = new BlockPos(1, 2, 1);

    private ModelGameTests() {
    }

    @GameTest(template = ARENA)
    public static void declaredModels_haveTheirDefinitionsBesideThem(final GameTestHelper helper) {
        final List<DeclaredModel> declared = TestSounds.CONTENT.declaredModels();
        // The press, the crate, the walker, the helmet, the arms, the turbine, the shaft, the gear and the two pulleys.
        helper.assertTrue(declared.size() == 10, "the test mod declares its ten models; it has " + declared.size());
        for (final DeclaredModel model : declared) {
            final ResourceLocation file = DeclaredModel.definitionFile(model.id());
            final ModelDefinition written = ModelDefinition.fromJson(model.id().toString(), JsonParser.parseString(
                    new String(bytes(file), StandardCharsets.UTF_8)).getAsJsonObject());
            helper.assertTrue(written.equals(model.definition()), "the definition of " + model.id()
                    + " says what its declaration says: " + written + " against " + model.definition());
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void modelFiles_readWhateverTheirFormat(final GameTestHelper helper) {
        for (final DeclaredModel model : TestSounds.CONTENT.declaredModels()) {
            final ResourceLocation file = ResourceLocation.parse(model.definition().model());
            final List<ModelData> read = ModelReaders.read(file.getPath(), bytes(file), beside -> null);
            helper.assertTrue(!read.getFirst().bones().isEmpty(), model.id() + " reads with its bones");
        }
        final ModelData press = ModelReaders.read("test_kit_press.bbmodel", bytes(ResourceLocation.parse(
                TestModels.PRESS.definition().model())), beside -> null).getFirst();
        helper.assertTrue(press.bone("press_head").isPresent() && press.bone("lamp_glow").orElseThrow().glows(),
                "the press has its head and its glowing lamp");
        helper.assertTrue(press.locators().containsKey("plate"), "and the locator its ingot is held at");
        helper.assertTrue(press.animations().size() == 2 && press.animations().getFirst().events().size() == 3,
                "and its two animations, the strike with its sound, its smoke and its word to code");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void animationPayload_crossesTheWireAsItLeft(final GameTestHelper helper) {
        final ModelAnimationPayload sent = new ModelAnimationPayload(ModelTarget.block(new BlockPos(3, -60, 9)),
                "main", ModelAnimationPayload.Action.REPLAY, "animation.test_kit_press.strike", 0.25F, 1.5F);
        final ByteBuf wire = Unpooled.buffer();
        ModelAnimationPayload.STREAM_CODEC.encode(wire, sent);
        final ModelAnimationPayload received = ModelAnimationPayload.STREAM_CODEC.decode(wire);
        helper.assertTrue(received.equals(sent), "the payload arrives as it left: " + received);
        final ByteBuf entity = Unpooled.buffer();
        final ModelAnimationPayload toEntity = new ModelAnimationPayload(ModelTarget.entity(42), "arms",
                ModelAnimationPayload.Action.STOP, "", 0, 1);
        ModelAnimationPayload.STREAM_CODEC.encode(entity, toEntity);
        helper.assertTrue(ModelAnimationPayload.STREAM_CODEC.decode(entity).equals(toEntity),
                "and one for an entity too");
        boolean refused = false;
        try {
            new ModelAnimationPayload(ModelTarget.item(1), "x".repeat(ModelAnimationPayload.MAX_NAME + 1),
                    ModelAnimationPayload.Action.PLAY, "a", 0, 1);
        } catch (final IllegalArgumentException tooLong) {
            refused = true;
        }
        helper.assertTrue(refused, "a name too long for the wire is refused where it is made");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void play_onAModelNoPlayerSeesSendsToNoOne(final GameTestHelper helper) {
        helper.setBlock(PRESS, TestModels.PRESS_BLOCK.get());
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertBlockEntityData(PRESS, entity -> true, () -> "a press"))
                .thenExecute(() -> {
                    final BlockEntity press = helper.getBlockEntity(PRESS);
                    ModelAnimations.play(press, "main", "strike");
                    ModelAnimations.replay(press, "main", "spin");
                    ModelAnimations.speed(press, "main", 2);
                    ModelAnimations.stop(press, "main");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void idOf_givesAStackItsNumberOnce(final GameTestHelper helper) {
        final ItemStack crate = new ItemStack(TestModels.CRATE_ITEM.get());
        final long first = ModelAnimations.idOf(crate);
        helper.assertTrue(first >= 0 && ModelAnimations.idOf(crate) == first, "a stack keeps the number it is given");
        helper.assertTrue(ModelAnimations.idOf(new ItemStack(TestModels.CRATE_ITEM.get())) != first,
                "and another stack is given another");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void modelCommands_areDeclared(final GameTestHelper helper) {
        helper.assertTrue(SeriesCommands.names().contains("model"), "/jstech model is declared");
        helper.succeed();
    }

    private static byte[] bytes(final ResourceLocation file) {
        final Path path = ModList.get().getModFileById(file.getNamespace()).getFile().findResource("assets",
                file.getNamespace(), file.getPath());
        try {
            return Files.readAllBytes(path);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
