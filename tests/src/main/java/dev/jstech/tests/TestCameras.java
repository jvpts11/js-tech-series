/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.camera.CameraFeed;
import dev.jstech.core.camera.CameraPath;
import dev.jstech.core.camera.CameraPaths;
import dev.jstech.core.camera.ZoomItem;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.Device;
import dev.jstech.core.content.DeviceBlock;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.content.IItemLook;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.model.Easing;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * The cameras of the test mod, declared the way a mod declares its own, to prove the Core's camera: binoculars of four
 * and eight times, looked through the spyglass's picture; a scene declared in code and one written as a resource
 * pack's file; and a screen that shows what a camera above it sees, looking north and down at whoever stands before
 * it. They wear the game's own pictures, so the test mod draws nothing.
 */
public final class TestCameras {

    /** The binoculars' powers, and how long the scene in code lasts, in ticks. */
    public static final List<Double> POWERS = List.of(4.0, 8.0);
    public static final int SCENE_TICKS = 40;

    public static final ItemEntry<ZoomItem> BINOCULARS = TestSounds.CONTENT.item("test_binoculars",
                    properties -> new ZoomItem(properties, POWERS,
                            Optional.of(ResourceLocation.withDefaultNamespace("textures/misc/spyglass_scope.png"))))
            .named("Test Binoculars").look(IItemLook.HANDMADE).tab(TestItems.ITEMS).register();

    /** A scene declared in code: from behind the origin, low, up over it to the right. */
    public static final ResourceLocation SCENE = CameraPaths.declare(id("test_scene"), new CameraPath(List.of(
            new CameraPath.Keyframe(0, 0, 3, -6, 0, 10, 0, 0, Easing.LINEAR),
            new CameraPath.Keyframe(SCENE_TICKS, -6, 6, 0, 60, 30, 0, 0, Easing.EASE_IN_OUT_SINE)), true));
    /** A scene of a resource pack's file only, {@code assets/jstests/jstech/camera_paths/test_file_scene.json}. */
    public static final ResourceLocation FILE_SCENE = id("test_file_scene");

    /* Its block state and model are written by hand, as the test mod's are. */
    public static final BlockEntry<DeviceBlock> SCREEN = TestSounds.CONTENT.block("test_camera_screen",
            Device.of(() -> TestCameras.SCREEN_ENTITY.get())::block)
            .named("Test camera screen").look(IBlockLook.cubeAll("test_camera_screen")).register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ScreenEntity>> SCREEN_ENTITY =
            TestSounds.CONTENT.blockEntity("test_camera_screen", ScreenEntity::new, SCREEN);

    /* The screen's camera sits this far above it, looking north and this far down. */
    private static final double CAMERA_ABOVE = 2.5;
    private static final float CAMERA_YAW = 180.0F;
    private static final float CAMERA_PITCH = 25.0F;
    private static final double CAMERA_FOV = 70.0;

    private TestCameras() {
    }

    /** Declares the cameras, before the test mod's content is registered. */
    public static void declare() {
        // Loading the class declares them.
    }

    /** What the camera of the screen at {@code screen} sees. */
    public static CameraFeed feedOf(final BlockPos screen) {
        return new CameraFeed(Vec3.atCenterOf(screen).add(0, CAMERA_ABOVE, 0), CAMERA_YAW, CAMERA_PITCH, CAMERA_FOV);
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsTests.MODID, path);
    }

    /** The test screen: nothing of its own, its renderer shows its camera. */
    public static final class ScreenEntity extends SyncedBlockEntity {

        public ScreenEntity(final BlockPos pos, final BlockState state) {
            super(SCREEN_ENTITY.get(), pos, state);
        }
    }
}
