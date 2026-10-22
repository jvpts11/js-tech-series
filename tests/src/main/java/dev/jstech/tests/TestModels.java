/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import com.mojang.serialization.MapCodec;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.DeclaredModel;
import dev.jstech.core.content.Device;
import dev.jstech.core.content.DeviceBlock;
import dev.jstech.core.content.EntityEntry;
import dev.jstech.core.content.GeoLook;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.content.IItemLook;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.machine.MachineBlockEntity;
import dev.jstech.core.machine.MachineEnergy;
import dev.jstech.core.machine.MachineStatus;
import dev.jstech.core.machine.MachineStructure;
import dev.jstech.core.multiblock.BlockMatch;
import dev.jstech.core.multiblock.MultiblockPattern;
import dev.jstech.core.multiblock.MultiblockPatterns;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * What the tests draw with the Core's models, from files of every kind the Core reads: a press from a Blockbench
 * project, its head striking and turning, a lamp that glows only while it is powered, an ingot held on its plate; a
 * crate from an OBJ file, as an item; a walker from an FBX file in its text form, its head turning, as an entity; a
 * helmet from a Bedrock geometry, as armour; the Dock Station of J's Computers as GeckoLib lays its files out,
 * drawn by the Core's models to set beside GeckoLib's; a turbine, a multiblock whose iron merges into its model once
 * it forms, its rotor turning while it works; a shaft, a gear and two pulleys, the parts of mechanical power drawn
 * turning; and, on a showcase, a pump from a 3MF assembly, a bracket from STL, a collar from STEP and a fan from a USD
 * scene, the parts of each file its bones. Their block states and item models are written by hand, as the test mod's
 * are.
 */
public final class TestModels {

    public static final DeclaredModel PRESS = TestSounds.CONTENT.model("test_kit_press")
            .file("jsmodels/test_kit_press.bbmodel")
            .texture("minecraft:textures/block/iron_block.png")
            .variant(off -> off.when("powered", "false").hide("lamp_glow"))
            .register();
    public static final DeclaredModel CRATE = TestSounds.CONTENT.model("test_kit_crate")
            .file("jsmodels/test_kit_crate.obj")
            .texture("minecraft:textures/block/barrel_side.png")
            .register();
    public static final DeclaredModel WALKER = TestSounds.CONTENT.model("test_kit_walker")
            .file("jsmodels/test_kit_walker.fbx")
            .texture("minecraft:textures/block/terracotta.png")
            .register();
    public static final DeclaredModel HELMET = TestSounds.CONTENT.model("test_kit_helmet")
            .file("jsmodels/test_kit_helmet.geo.json")
            .texture("minecraft:textures/block/gold_block.png")
            .register();
    /** A tool held by the player's own arm in first person, painted with a texture of four frames that glows. */
    public static final DeclaredModel ARMS = TestSounds.CONTENT.model("test_kit_arms")
            .file("jsmodels/test_kit_arms.geo.json")
            .texture("jstests:textures/kit/test_kit_flasher.png")
            .register();
    /** J's Computers' Dock Station as its GeckoLib family lays out its files. */
    public static final GeoLook<Object> DOCK = GeoLook.of("jsc", "dock", "dock_station", thing -> "dock_station");

    public static final BlockEntry<PressBlock> PRESS_BLOCK = TestSounds.CONTENT.block("test_kit_press",
            PressBlock::new).named("Test kit press").look(IBlockLook.cubeAll("test_kit_press")).register();
    public static final BlockEntry<DockBlock> DOCK_BLOCK = TestSounds.CONTENT.block("test_kit_dock",
            DockBlock::new).named("Test kit dock").look(IBlockLook.cubeAll("test_kit_dock")).register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PressEntity>> PRESS_ENTITY =
            TestSounds.CONTENT.blockEntity("test_kit_press", PressEntity::new, PRESS_BLOCK);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DockEntity>> DOCK_ENTITY =
            TestSounds.CONTENT.blockEntity("test_kit_dock", DockEntity::new, DOCK_BLOCK);
    public static final ItemEntry<Item> CRATE_ITEM = TestSounds.CONTENT.item("test_kit_crate", Item::new)
            .named("Test kit crate").look(IItemLook.HANDMADE).tab(TestItems.ITEMS).register();
    public static final ItemEntry<Item> ARMS_ITEM = TestSounds.CONTENT.item("test_kit_arms", Item::new)
            .named("Test kit arms").look(IItemLook.HANDMADE).tab(TestItems.ITEMS).register();
    public static final ItemEntry<ArmorItem> HELMET_ITEM = TestSounds.CONTENT.item("test_kit_helmet",
            properties -> new ArmorItem(ArmorMaterials.IRON, ArmorItem.Type.HELMET, properties))
            .named("Test kit helmet").look(IItemLook.HANDMADE).tab(TestItems.ITEMS).register();
    public static final EntityEntry<Walker> WALKER_ENTITY = TestSounds.CONTENT.entity("test_kit_walker",
            Walker::new, MobCategory.MISC).size(0.6F, 1.3F).named("Test kit walker").register();
    /** The turbine's model: its casing round its controller's front, and the rotor on its top. */
    public static final DeclaredModel TURBINE = TestSounds.CONTENT.model("test_kit_turbine")
            .file("jsmodels/test_kit_turbine.bbmodel")
            .texture("minecraft:textures/block/iron_block.png")
            .register();
    /** The turbine's pattern: two layers of iron three by three, its controller in the middle of its south side. */
    public static final ResourceLocation TURBINE_PATTERN_ID = ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
            "test_kit_turbine");
    public static final MultiblockPattern TURBINE_PATTERN = MultiblockPatterns.declare(TURBINE_PATTERN_ID,
            MultiblockPattern.builder("test_kit_turbine")
                    .layer("III", "III", "I#I")
                    .layer("III", "III", "III")
                    .where('I', BlockMatch.blocks("minecraft:iron_block"))
                    .controller("jstests:test_kit_turbine")
                    .build());
    public static final BlockEntry<DeviceBlock> TURBINE_BLOCK = TestSounds.CONTENT.block("test_kit_turbine",
            Device.of(() -> TestModels.TURBINE_ENTITY.get()).ticks(MachineBlockEntity::serverTick)::block)
            .named("Test kit turbine").look(IBlockLook.cubeAll("test_kit_turbine")).register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TurbineEntity>> TURBINE_ENTITY =
            TestSounds.CONTENT.blockEntity("test_kit_turbine", TurbineEntity::new, TURBINE_BLOCK);
    /* The models the parts of mechanical power are drawn turning with, each made standing up, its axle along Y. */
    public static final DeclaredModel SHAFT = TestSounds.CONTENT.model("test_kit_shaft")
            .file("jsmodels/test_kit_shaft.bbmodel")
            .texture("minecraft:textures/block/iron_block.png")
            .register();
    public static final DeclaredModel GEAR = TestSounds.CONTENT.model("test_kit_gear")
            .file("jsmodels/test_kit_gear.bbmodel")
            .texture("minecraft:textures/block/copper_block.png")
            .register();
    public static final DeclaredModel SMALL_PULLEY = TestSounds.CONTENT.model("test_kit_small_pulley")
            .file("jsmodels/test_kit_small_pulley.bbmodel")
            .texture("minecraft:textures/block/oak_planks.png")
            .register();
    public static final DeclaredModel LARGE_PULLEY = TestSounds.CONTENT.model("test_kit_large_pulley")
            .file("jsmodels/test_kit_large_pulley.bbmodel")
            .texture("minecraft:textures/block/oak_planks.png")
            .register();
    /*
     * The models of CAD and scene files, each part of the file a bone, drawn on the showcase. The pump, an assembly of
     * 1 000 triangles, is brought down to 800, and to 300 from 32 blocks away; its rotor and impeller turn by an
     * animation of their own.
     */
    public static final DeclaredModel PUMP = TestSounds.CONTENT.model("test_kit_pump")
            .file("jsmodels/test_kit_pump.3mf")
            .animations("jsmodels/test_kit_pump.animation.json")
            .triangles(800)
            .far(32, 300)
            .register();
    /** A bracket from a text STL file, the triangles alone a 3D printer takes. */
    public static final DeclaredModel BRACKET = TestSounds.CONTENT.model("test_kit_bracket")
            .file("jsmodels/test_kit_bracket.stl")
            .register();
    /** A collar from a STEP file, its faces of brass laid from their surfaces, drawn four times its size. */
    public static final DeclaredModel COLLAR = TestSounds.CONTENT.model("test_kit_collar")
            .file("jsmodels/test_kit_collar.step")
            .scale(4)
            .register();
    /** A fan from a USD scene in text, its blades turning by the scene's own samples, plainer from 24 blocks away. */
    public static final DeclaredModel FAN = TestSounds.CONTENT.model("test_kit_fan")
            .file("jsmodels/test_kit_fan.usda")
            .far(24, 40)
            .register();
    /** The models the showcase draws, by the number its block's state holds. */
    public static final List<DeclaredModel> SHOWN = List.of(PUMP, BRACKET, COLLAR, FAN);
    public static final BlockEntry<ShowcaseBlock> SHOWCASE_BLOCK = TestSounds.CONTENT.block("test_kit_showcase",
            ShowcaseBlock::new).named("Test kit showcase").look(IBlockLook.cubeAll("test_kit_showcase")).register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShowcaseEntity>> SHOWCASE_ENTITY =
            TestSounds.CONTENT.blockEntity("test_kit_showcase", ShowcaseEntity::new, SHOWCASE_BLOCK);

    private static final int TURBINE_ENERGY = 1_000;

    private TestModels() {
    }

    /** Declares the models and what is drawn with them, before the test mod's content is registered. */
    public static void declare() {
        // Loading the class declares them.
    }

    /** The press: it faces the way it is placed, and its lamp glows while it is powered. */
    public static final class PressBlock extends BaseEntityBlock {

        private static final MapCodec<PressBlock> CODEC = simpleCodec(PressBlock::new);

        public PressBlock(final Properties properties) {
            super(properties.noOcclusion());
            registerDefaultState(defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                    .setValue(BlockStateProperties.POWERED, false));
        }

        @Override
        public BlockState getStateForPlacement(final BlockPlaceContext context) {
            return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,
                    context.getHorizontalDirection().getOpposite());
        }

        @Override
        public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
            return new PressEntity(pos, state);
        }

        @Override
        protected RenderShape getRenderShape(final BlockState state) {
            return RenderShape.ENTITYBLOCK_ANIMATED;
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.POWERED);
        }
    }

    /** The press's block entity, which holds nothing: what it plays the server tells its players. */
    public static final class PressEntity extends BlockEntity {

        public PressEntity(final BlockPos pos, final BlockState state) {
            super(PRESS_ENTITY.get(), pos, state);
        }
    }

    /** The block drawing J's Computers' Dock Station through the Core's models. */
    public static final class DockBlock extends BaseEntityBlock {

        private static final MapCodec<DockBlock> CODEC = simpleCodec(DockBlock::new);

        public DockBlock(final Properties properties) {
            super(properties.noOcclusion());
        }

        @Override
        public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
            return new DockEntity(pos, state);
        }

        @Override
        protected RenderShape getRenderShape(final BlockState state) {
            return RenderShape.ENTITYBLOCK_ANIMATED;
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
            return CODEC;
        }
    }

    /** The dock's block entity, which holds nothing. */
    public static final class DockEntity extends BlockEntity {

        public DockEntity(final BlockPos pos, final BlockState state) {
            super(DOCK_ENTITY.get(), pos, state);
        }
    }

    /** The showcase: a block drawn with one of the models of CAD and scene files, which its state says. */
    public static final class ShowcaseBlock extends BaseEntityBlock {

        /** Which of {@link #SHOWN} it is drawn with. */
        public static final IntegerProperty MODEL = IntegerProperty.create("model", 0, 3);
        private static final MapCodec<ShowcaseBlock> CODEC = simpleCodec(ShowcaseBlock::new);

        public ShowcaseBlock(final Properties properties) {
            super(properties.noOcclusion());
            registerDefaultState(defaultBlockState().setValue(MODEL, 0));
        }

        @Override
        public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
            return new ShowcaseEntity(pos, state);
        }

        @Override
        protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(MODEL);
        }

        @Override
        protected RenderShape getRenderShape(final BlockState state) {
            return RenderShape.ENTITYBLOCK_ANIMATED;
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
            return CODEC;
        }
    }

    /** The showcase's block entity, which holds nothing; its block's state says what it is drawn with. */
    public static final class ShowcaseEntity extends BlockEntity {

        public ShowcaseEntity(final BlockPos pos, final BlockState state) {
            super(SHOWCASE_ENTITY.get(), pos, state);
        }

        /** The model it is drawn with. */
        public DeclaredModel shown() {
            return SHOWN.get(getBlockState().getValue(ShowcaseBlock.MODEL));
        }
    }

    /**
     * The turbine's controller: once its iron stands whole, the iron merges into its model, which grows into place as
     * it forms; it works whenever its structure stands, and its rotor turns while it works.
     */
    public static final class TurbineEntity extends MachineBlockEntity {

        public TurbineEntity(final BlockPos pos, final BlockState state) {
            super(TURBINE_ENTITY.get(), pos, state, 0, MachineEnergy.fe(TURBINE_ENERGY, TURBINE_ENERGY, 0));
            formsWith(MachineStructure.of(TURBINE_PATTERN).merges().formingAnimation("form"));
        }

        @Override
        protected MachineStatus work(final ServerLevel level) {
            return MachineStatus.WORKING;
        }
    }

    /** The walker: it stands where it is put and does nothing; its model's head turns. */
    public static final class Walker extends Entity {

        public Walker(final EntityType<? extends Walker> type, final Level level) {
            super(type, level);
        }

        @Override
        protected void defineSynchedData(final SynchedEntityData.Builder builder) {
            // It keeps nothing of its own.
        }

        @Override
        protected void readAdditionalSaveData(final CompoundTag tag) {
            // It keeps nothing of its own.
        }

        @Override
        protected void addAdditionalSaveData(final CompoundTag tag) {
            // It keeps nothing of its own.
        }

        @Override
        public boolean isPickable() {
            return true;
        }
    }
}
