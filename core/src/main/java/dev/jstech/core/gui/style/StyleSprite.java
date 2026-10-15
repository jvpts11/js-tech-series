/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.style;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * How a style draws one {@link StylePiece}. In a style file a piece is the id of a sprite the game cuts by its own
 * rules ({@code "button": "minecraft:widget/button"}), or an object saying how:
 *
 * <pre>{@code
 * "window": {"kind": "slice", "texture": "minecraft:textures/gui/demo_background.png",
 *            "region": [0, 0, 248, 166], "texture_size": [256, 256], "border": 4},
 * "separator": {"kind": "tile", "texture": "minecraft:textures/gui/header_separator.png", "size": [32, 2]},
 * "slot": {"kind": "flat", "fill": "slot_bg", "edge": "slot_edge"},
 * "header": {"kind": "none"}
 * }</pre>
 *
 * <p>The colours of a flat piece are a role's key or a colour of their own ({@code "#FF8B8B8B"}).
 */
public sealed interface StyleSprite {

    /** Draws nothing. */
    StyleSprite NONE = new Nothing();

    /** A piece as the file writes it. */
    Codec<StyleSprite> CODEC = Codec.either(ResourceLocation.CODEC, Kinds.CODEC).xmap(
            either -> either.map(Sprite::new, kind -> kind),
            sprite -> sprite instanceof Sprite plain ? Either.left(plain.id()) : Either.right(sprite));

    /**
     * A sprite of the game's or a resource pack's GUI atlas, cut as its file says (stretched, tiled or in nine
     * slices): drawn this way, a resource pack that repaints the game's widgets repaints the piece too.
     *
     * @param id the sprite, as {@code minecraft:widget/button}
     */
    record Sprite(ResourceLocation id) implements StyleSprite {
    }

    /**
     * A region of a texture cut in nine: its corners drawn as they are, its edges and middle stretched to the size
     * asked for.
     *
     * @param texture       the texture
     * @param u             the region's left edge in the texture
     * @param v             the region's top edge in the texture
     * @param width         the region's width
     * @param height        the region's height
     * @param textureWidth  the texture's width
     * @param textureHeight the texture's height
     * @param border        how far the corners and edges reach in from each side
     */
    record Slice(ResourceLocation texture, int u, int v, int width, int height, int textureWidth, int textureHeight,
                 int border) implements StyleSprite {
    }

    /**
     * A texture laid side by side over the size asked for, as the game lays its separators.
     *
     * @param texture the texture
     * @param width   its width
     * @param height  its height
     */
    record Tile(ResourceLocation texture, int width, int height) implements StyleSprite {
    }

    /**
     * Flat colours: a fill, a one-pixel edge and a bevel, each a role's key or a colour of its own, each left out to
     * draw none.
     *
     * @param fill   the ground
     * @param edge   a line all round
     * @param light  the lit edge, top and left
     * @param dark   the shaded edge, bottom and right
     * @param sunken whether the bevel is turned round, shaded at the top and left, as a well's
     */
    record Flat(Optional<String> fill, Optional<String> edge, Optional<String> light, Optional<String> dark,
                boolean sunken) implements StyleSprite {

        /** A fill of a role with an edge of another. */
        public static Flat of(final StyleRole fill, final StyleRole edge) {
            return new Flat(Optional.of(fill.key()), Optional.of(edge.key()), Optional.empty(), Optional.empty(),
                    false);
        }

        /** A fill of a role, with no edge. */
        public static Flat of(final StyleRole fill) {
            return new Flat(Optional.of(fill.key()), Optional.empty(), Optional.empty(), Optional.empty(), false);
        }
    }

    /** Nothing drawn. */
    record Nothing() implements StyleSprite {
    }

    /**
     * The game's own ground of a menu, which only the screen itself can draw: the title screen's panorama, or the
     * world blurred, under the game's menu texture. Drawn as a piece elsewhere, it is nothing.
     */
    record GameMenu() implements StyleSprite {
    }

    /** The kinds a piece written as an object may be. */
    final class Kinds {

        private static final List<String> NAMES = List.of("sprite", "slice", "tile", "flat", "none", "menu");

        private static final MapCodec<Sprite> SPRITE = ResourceLocation.CODEC.fieldOf("id").xmap(Sprite::new,
                Sprite::id);

        private static final MapCodec<Slice> SLICE = RecordCodecBuilder.mapCodec(i -> i.group(
                ResourceLocation.CODEC.fieldOf("texture").forGetter(Slice::texture),
                Codec.INT.listOf(4, 4).fieldOf("region").forGetter(s -> List.of(s.u(), s.v(), s.width(), s.height())),
                Codec.INT.listOf(2, 2).optionalFieldOf("texture_size", List.of(256, 256))
                        .forGetter(s -> List.of(s.textureWidth(), s.textureHeight())),
                Codec.intRange(0, 64).optionalFieldOf("border", 4).forGetter(Slice::border)
        ).apply(i, (texture, region, size, border) -> new Slice(texture, region.get(0), region.get(1), region.get(2),
                region.get(3), size.get(0), size.get(1), border)));

        private static final MapCodec<Tile> TILE = RecordCodecBuilder.mapCodec(i -> i.group(
                ResourceLocation.CODEC.fieldOf("texture").forGetter(Tile::texture),
                Codec.INT.listOf(2, 2).fieldOf("size").forGetter(t -> List.of(t.width(), t.height()))
        ).apply(i, (texture, size) -> new Tile(texture, size.get(0), size.get(1))));

        private static final MapCodec<Flat> FLAT = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.STRING.optionalFieldOf("fill").forGetter(Flat::fill),
                Codec.STRING.optionalFieldOf("edge").forGetter(Flat::edge),
                Codec.STRING.optionalFieldOf("light").forGetter(Flat::light),
                Codec.STRING.optionalFieldOf("dark").forGetter(Flat::dark),
                Codec.BOOL.optionalFieldOf("sunken", false).forGetter(Flat::sunken)
        ).apply(i, Flat::new));

        private static final MapCodec<Nothing> NOTHING = MapCodec.unit(new Nothing());

        private static final MapCodec<GameMenu> MENU = MapCodec.unit(new GameMenu());

        private static final Codec<String> KIND = Codec.STRING.validate(kind -> NAMES.contains(kind)
                ? DataResult.success(kind)
                : DataResult.error(() -> "a style piece of the kind \"" + kind + "\": write one of " + NAMES));

        static final Codec<StyleSprite> CODEC = KIND.dispatch("kind", Kinds::kindOf, Kinds::codecOf);

        private Kinds() {
        }

        private static String kindOf(final StyleSprite sprite) {
            return switch (sprite) {
                case Sprite ignored -> "sprite";
                case Slice ignored -> "slice";
                case Tile ignored -> "tile";
                case Flat ignored -> "flat";
                case Nothing ignored -> "none";
                case GameMenu ignored -> "menu";
            };
        }

        private static MapCodec<? extends StyleSprite> codecOf(final String kind) {
            return switch (kind) {
                case "sprite" -> SPRITE;
                case "slice" -> SLICE;
                case "tile" -> TILE;
                case "flat" -> FLAT;
                case "menu" -> MENU;
                default -> NOTHING;
            };
        }
    }
}
