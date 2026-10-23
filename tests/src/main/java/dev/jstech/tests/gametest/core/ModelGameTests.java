/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import com.google.gson.JsonParser;
import dev.jstech.core.jstechseries.command.SeriesCommands;
import dev.jstech.core.content.DeclaredModel;
import dev.jstech.core.datagen.ModelDefinitionProvider;
import dev.jstech.core.model.CadLook;
import dev.jstech.core.model.ModelBone;
import dev.jstech.core.model.ModelData;
import dev.jstech.core.model.ModelDefinition;
import dev.jstech.core.model.ModelMesh;
import dev.jstech.core.model.ModelReaders;
import dev.jstech.core.model.ModelShaping;
import dev.jstech.core.model.network.ModelAnimationPayload;
import dev.jstech.core.model.network.ModelAnimations;
import dev.jstech.core.model.network.ModelTarget;
import dev.jstech.core.util.DeclarationError;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestModels;
import dev.jstech.tests.TestSounds;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Core's models as the server sees them: every model a mod declares has its definition written beside its file,
 * the same the declaration says, and its file reads, whatever its format; the files of CAD programs and scenes read as
 * their parts, in their colours, at their size, within the triangles they may have, and a file over them stops the data
 * run; what the server plays on a model
 * crosses the wire as it left, and a name too long for the wire is refused where it is made, not where it is sent; an
 * item the server animates is given its number once; and the models' commands are there.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ModelGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos PRESS = new BlockPos(1, 2, 1);
    private static final String OVER_BUDGET = JsTests.MODID + ":jsmodels/test_kit_over_budget.stl";
    private static final int TOO_MANY = ModelDefinition.Detail.MOST_UNSAID + 1;
    private static final int STL_HEADER = 80;
    private static final int STL_TRIANGLE = 50;
    private static final int SHEET_ROW = 200;

    private ModelGameTests() {
    }

    @GameTest(template = ARENA)
    public static void declaredModels_haveTheirDefinitionsBesideThem(final GameTestHelper helper) {
        final List<DeclaredModel> declared = TestSounds.CONTENT.declaredModels();
        // The press, the crate, the walker, the helmet, the arms, the turbine, the shaft, the gear, the two pulleys,
        // and the pump, the bracket, the collar and the fan of CAD and scene files.
        helper.assertTrue(declared.size() == 14, "the test mod declares its fourteen models; it has "
                + declared.size());
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
    public static void cadModels_readAsTheirPartsInTheirColours(final GameTestHelper helper) {
        final ModelData pump = read(TestModels.PUMP);
        helper.assertTrue(pump.bone("impeller").orElseThrow().parent().equals("housing")
                && pump.bone("rotor").orElseThrow().parent().equals("motor")
                && pump.bone("flange").orElseThrow().parent().equals("pump"),
                "the pump's parts hang from the parts they are components of: " + pump.bones());
        helper.assertTrue(pump.textures().stream().filter(CadLook::isColour).count() == 6,
                "each of its six colours is a texture in the game's look");
        helper.assertTrue(ModelShaping.triangles(pump) == 1_000, "it has the 1 000 triangles of its file");
        final ModelDefinition.Detail detail = TestModels.PUMP.definition().detail();
        final int near = ModelShaping.triangles(ModelShaping.shape(pump, detail, false));
        final int far = ModelShaping.triangles(ModelShaping.shape(pump, detail, true));
        helper.assertTrue(near <= 800 && near > far && far <= 300, "and is drawn with at most 800 near and 300 far:"
                + " " + near + " and " + far);
        final ModelData bracket = read(TestModels.BRACKET);
        helper.assertTrue(ModelShaping.triangles(bracket) == 20 && Math.abs(wide(bracket) - 4.8F) < 0.01F,
                "the bracket is its file's twenty triangles, 300 millimetres wide: " + wide(bracket) + " pixels");
        final ModelData collar = read(TestModels.COLLAR);
        helper.assertTrue(collar.textures().size() == 1 && CadLook.isColour(collar.textures().getFirst())
                && Math.abs(wide(collar) - 1.92F) < 0.05F, "the collar is of its one brass, 120 millimetres wide, "
                + "drawn four times as big: " + wide(collar) + " pixels");
        final ModelData fan = read(TestModels.FAN);
        helper.assertTrue(fan.animations().size() == 1 && fan.animations().getFirst().bones().containsKey("blades"),
                "the fan's blades turn by the scene's samples");
        for (final DeclaredModel model : TestModels.SHOWN) {
            helper.assertTrue(ModelShaping.overBudget(read(model), model.definition().detail()).isEmpty(),
                    model.id() + " is within the triangles it may be drawn with");
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void dataRun_stopsAtAModelOverTheTrianglesItMayHaveUnsaid(final GameTestHelper helper) {
        final Path pack = budgetPack();
        try {
            final ExistingFileHelper files = new ExistingFileHelper(List.of(pack), Set.of(), true, null, null);
            String refusal = "";
            try {
                ModelDefinitionProvider.check(overBudget("{\"model\": \"" + OVER_BUDGET + "\"}"), files);
            } catch (final DeclarationError stopped) {
                refusal = stopped.getMessage();
            }
            helper.assertTrue(refusal.contains(TOO_MANY + " triangles"), "a file of more triangles than a model may "
                    + "have without saying how many it is drawn with stops the run: " + refusal);
            // Said how many it is drawn with, the same file is drawn with fewer, and the run goes on.
            ModelDefinitionProvider.check(overBudget("{\"model\": \"" + OVER_BUDGET + "\", \"triangles\": 800}"),
                    files);
        } finally {
            delete(pack);
        }
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

    private static DeclaredModel overBudget(final String definition) {
        final ResourceLocation id = ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "test_kit_over_budget");
        return new DeclaredModel(id, ModelDefinition.fromJson(id.toString(), JsonParser.parseString(definition)
                .getAsJsonObject()));
    }

    /* A pack of one model's file: a binary STL sheet of one triangle more than a model may have unsaid. */
    private static Path budgetPack() {
        try {
            final Path pack = Files.createTempDirectory("jstests-model-budget");
            final Path file = pack.resolve("assets").resolve(JsTests.MODID).resolve(
                    ResourceLocation.parse(OVER_BUDGET).getPath());
            Files.createDirectories(file.getParent());
            final ByteBuffer stl = ByteBuffer.allocate(STL_HEADER + 4 + TOO_MANY * STL_TRIANGLE)
                    .order(ByteOrder.LITTLE_ENDIAN);
            stl.position(STL_HEADER);
            stl.putInt(TOO_MANY);
            for (int i = 0; i < TOO_MANY; i++) {
                final float x = i % SHEET_ROW;
                final float z = (float) (i / SHEET_ROW);
                stl.putFloat(0).putFloat(1).putFloat(0);
                stl.putFloat(x).putFloat(0).putFloat(z);
                stl.putFloat(x).putFloat(0).putFloat(z + 1);
                stl.putFloat(x + 1).putFloat(0).putFloat(z);
                stl.putShort((short) 0);
            }
            Files.write(file, stl.array());
            return pack;
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void delete(final Path folder) {
        try (Stream<Path> paths = Files.walk(folder)) {
            for (final Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static ModelData read(final DeclaredModel model) {
        final ResourceLocation file = ResourceLocation.parse(model.definition().model());
        return ModelReaders.read(file.getPath(), bytes(file), beside -> null).getFirst();
    }

    /* How wide a model's meshes are across X, in pixels. */
    private static float wide(final ModelData data) {
        float low = Float.MAX_VALUE;
        float high = -Float.MAX_VALUE;
        for (final ModelBone bone : data.bones()) {
            for (final ModelMesh mesh : bone.meshes()) {
                for (int at = 0; at < mesh.positions().length; at += 3) {
                    low = Math.min(low, mesh.positions()[at]);
                    high = Math.max(high, mesh.positions()[at]);
                }
            }
        }
        return high - low;
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
