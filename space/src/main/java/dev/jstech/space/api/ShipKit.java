/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableCodecs;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/**
 * The kit of blocks a starship part is built of, and where each goes on the ship: its pieces, each set from one place
 * on the ship, and the block each letter stands for. A ship lies along its spine from the nose to the tail: x runs
 * across it, the hull's four blocks from -2 to 1 with the centre line between -1 and 0, growing to the right of a pilot
 * facing the nose; y rises from the hull's deck at 0; z runs from the nose to the tail. A piece's layers go up from its
 * corner, each a list of rows from the front to the back, each row of letters from the left to the right, a space for a
 * place left empty; a mirrored piece is also set on the other side of the centre line, as a wing's pair is.
 *
 * <pre>{@code
 * {"pieces": [{"anchor": "section", "x": -2, "y": 0, "z": 0,
 *              "layers": [["TDDT", "TGGT"], ["APBA", "V  V"], ["FCYF", "HIIH"]]}],
 *  "key": {"T": "jsspace:thermal_plating", "D": "jsspace:deck_plate", ...}}
 * }</pre>
 *
 * @param pieces its pieces
 * @param key    the block each letter stands for
 */
public record ShipKit(List<Piece> pieces, Map<String, ResourceLocation> key) {

    private static final Codec<String> LETTER = Codec.STRING.validate(letter -> letter.length() == 1
            && !" ".equals(letter) ? DataResult.success(letter) : DataResult.error(() -> "a kit's key is one letter"));
    private static final Codec<Piece> PIECE = RecordCodecBuilder.create(instance -> instance.group(
            StableCodecs.byName(Anchor.class).fieldOf("anchor").forGetter(Piece::anchor),
            Codec.intRange(-64, 64).fieldOf("x").forGetter(Piece::x),
            Codec.intRange(-64, 64).fieldOf("y").forGetter(Piece::y),
            Codec.intRange(-64, 64).fieldOf("z").forGetter(Piece::z),
            Codec.STRING.listOf(1, 64).listOf(1, 64).fieldOf("layers").forGetter(Piece::layers),
            Codec.BOOL.optionalFieldOf("mirrored", false).forGetter(Piece::mirrored)
    ).apply(instance, Piece::new));
    /** As a data pack's file writes one. */
    public static final Codec<ShipKit> CODEC = RecordCodecBuilder.<ShipKit>create(instance -> instance.group(
            PIECE.listOf(1, 16).fieldOf("pieces").forGetter(ShipKit::pieces),
            Codec.unboundedMap(LETTER, ResourceLocation.CODEC).fieldOf("key").forGetter(ShipKit::key)
    ).apply(instance, ShipKit::new)).validate(ShipKit::checked);

    public ShipKit {
        pieces = List.copyOf(pieces);
        key = Map.copyOf(key);
    }

    /* Every letter of every piece in the key. */
    private static DataResult<ShipKit> checked(final ShipKit kit) {
        for (final Piece piece : kit.pieces()) {
            for (final List<String> layer : piece.layers()) {
                for (final String row : layer) {
                    for (int x = 0; x < row.length(); x++) {
                        final char letter = row.charAt(x);
                        if (letter != ' ' && !kit.key().containsKey(String.valueOf(letter))) {
                            return DataResult.error(() -> "the letter " + letter + " is in no kit's key");
                        }
                    }
                }
            }
        }
        return DataResult.success(kit);
    }

    /** The place on the ship a piece is set from. */
    public enum Anchor implements IStableName {

        /** The front of the part's own section of the spine, its nose for a cockpit. */
        SECTION("section"),
        /** The joint behind the cockpit. */
        COCKPIT_JOINT("cockpit_joint"),
        /** The wings' root: the front of the second section behind the cockpit, or of the first if it is alone. */
        WING_ROOT("wing_root"),
        /** The end of the spine. */
        SPINE_END("spine_end");

        private final String serializedName;

        Anchor(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return this.serializedName;
        }
    }

    /**
     * A piece of a kit, set from its anchor.
     *
     * @param anchor   the place it is set from
     * @param x        its left side, across the ship
     * @param y        its lowest layer, above the hull's deck
     * @param z        its front row, from the anchor toward the tail
     * @param layers   its layers from the bottom, each its rows from the front, each row its letters from the left
     * @param mirrored whether it is also set on the other side of the centre line
     */
    public record Piece(Anchor anchor, int x, int y, int z, List<List<String>> layers, boolean mirrored) {

        public Piece {
            Objects.requireNonNull(anchor, "anchor");
            layers = List.copyOf(layers.stream().map(List::copyOf).toList());
        }
    }
}
