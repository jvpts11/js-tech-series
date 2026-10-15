/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.screen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * A screen as its file declares it, in a resource pack's {@code assets/<namespace>/jstech/screens/<name>.json}: how
 * wide it is, the title it shows, and the tree of components it holds. There is no style sheet: the file says what the
 * screen holds and where, and its look is the style the player picks. The screen's code keeps only what it does.
 *
 * <pre>{@code
 * {
 *   "width": 176,
 *   "root": {"type": "column", "children": [
 *     {"type": "title"},
 *     {"type": "row", "align": "center", "children": [
 *       {"type": "slot", "slot": 0},
 *       {"type": "progress", "bind": "progress", "max": 200, "grow": 1, "height": 8},
 *       {"type": "slot", "slot": 1}
 *     ]},
 *     {"type": "button", "text": "screen.mymod.press.start", "action": "start"},
 *     {"type": "inventory"}
 *   ]}
 * }
 * }</pre>
 *
 * @param width  how wide the screen is; its height is what its components need
 * @param title  the translation key of its title, over the one its menu was opened with
 * @param style  the style its mod wants it in when the player chose none, over the mod's default
 * @param root   what it holds
 */
public record ScreenFile(int width, Optional<String> title, Optional<ResourceLocation> style, ElementSpec root) {

    /** The folder of a resource pack's assets the screens are read from. */
    public static final String FOLDER = "jstech/screens";

    /** The width of the game's own container screens. */
    public static final int GAME_WIDTH = 176;

    public static final Codec<ScreenFile> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(32, 1024).optionalFieldOf("width", GAME_WIDTH).forGetter(ScreenFile::width),
            Codec.STRING.optionalFieldOf("title").forGetter(ScreenFile::title),
            ResourceLocation.CODEC.optionalFieldOf("style").forGetter(ScreenFile::style),
            ElementSpec.CODEC.fieldOf("root").forGetter(ScreenFile::root)
    ).apply(i, ScreenFile::new));
}
