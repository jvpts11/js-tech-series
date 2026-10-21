/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * Models for any mod, read from the files artists already make, and the animations that move them: a Blockbench
 * project, a Bedrock geometry and its animation files, glTF, FBX and OBJ, each read by a reader of the Core's own into
 * one form, with bones in a tree, cubes and meshes on them, locators, skins and morph targets. All of it is pure: it
 * holds no game types, so a test runs it without the game.
 *
 * <p>{@link ModelReaders} reads a model file of any of those formats, by its name's ending, into the {@link ModelData}
 * of each model it holds, and an animation file into each {@link ModelAnimation} it holds; a file that cannot be read
 * is refused with a {@link ModelFormatException} that says what is wrong with it. How a declared model is drawn (its
 * file, its textures, the bones that glow, its variants by the block's state) is its {@link ModelDefinition}, which the
 * data run writes. A {@link ModelAnimator} moves one drawn model: each of its controllers ({@link AnimationController})
 * plays an animation and blends from one to the next, each {@link IBoneMover} moves bones by code over them, and the
 * queries and variables the keyframes' expressions read live in its {@link ExpressionContext}. Where every bone is at
 * one moment is a {@link ModelPose}, and the model's faces are laid out once by the {@link GeometryBaker}.
 *
 * <p>How to declare, draw and animate a model, from zero, is in {@code core/docs/MODELS.md}. Examples that compile are
 * the test mod's renderers ({@code tests/src/main/java/dev/jstech/tests/clienttest/TestModelRenderers.java}), which
 * play animations by the state of what they draw, and its model tests
 * ({@code tests/src/main/java/dev/jstech/tests/gametest/core/ModelGameTests.java}), which read model files.
 */
@ApiStatus.Experimental
package dev.jstech.core.model;

import org.jetbrains.annotations.ApiStatus;
