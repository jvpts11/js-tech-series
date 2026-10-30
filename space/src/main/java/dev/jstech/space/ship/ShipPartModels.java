/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import dev.jstech.core.content.DeclaredModel;
import dev.jstech.core.content.ModelBuilder;
import dev.jstech.space.JsSpace;
import dev.jstech.space.SpaceContent;
import dev.jstech.space.api.ShipKit;
import dev.jstech.space.api.ShipKits;
import dev.jstech.space.api.ShipPart;
import dev.jstech.space.api.ShipPartKind;
import dev.jstech.space.api.ShipParts;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntPredicate;
import net.minecraft.resources.ResourceLocation;

/**
 * The formed models of the small starship's parts, as the approved catalogue drew them, with the joints drawn on the
 * hull where a section meets the next, and inside a part one goes into its maker's interior: each block of a part's
 * kit is a bone of its own, named {@code c_<x>_<y>_<z>} by its place in the kit from the piece's anchor, so a block
 * missing is a bone hidden. A part is a model a piece of its kit, set from the piece's anchor on the ship; and a model
 * in each finish, the same geometry painted by the finish's texture. A cockpit's and a hull module's glass lets the
 * world be seen through it, so their models are drawn translucent.
 */
public final class ShipPartModels {

    /** The finishes the models are painted in, by their paths. */
    public static final List<String> FINISHES = List.of("orbiter", "bare_metal", "enamel");
    /* Every model, by its id. */
    private static final Map<ResourceLocation, DeclaredModel> MODELS = new LinkedHashMap<>();
    /* The cluster's four bells have the most triangles. */
    private static final int MOST_TRIANGLES = 9_000;

    private ShipPartModels() {
    }

    /** Declares the models of the parts' kits, once, from the mod's constructor, after the kits. */
    public static void declare() {
        ShipKits.declared().forEach((part, kit) -> {
            if (!JsSpace.MODID.equals(part.getNamespace())) {
                return;
            }
            final ShipPart declared = ShipParts.declared().get(part);
            final boolean glazed = declared != null && (declared.kind() == ShipPartKind.COCKPIT
                    || declared.kind() == ShipPartKind.HULL || declared.kind() == ShipPartKind.MODULE);
            for (final ShipKit.Piece piece : kit.pieces()) {
                final String name = name(part, kit, piece);
                for (final String finish : FINISHES) {
                    final ModelBuilder model = SpaceContent.CONTENT.model("ship/" + finish + "/" + name)
                            .file("jsmodels/ship/" + name + ".gltf")
                            .texture("textures/models/ship/" + name + "_" + finish + ".png")
                            .triangles(MOST_TRIANGLES);
                    if (glazed) {
                        model.translucent();
                    }
                    MODELS.put(id(finish, name), model.register());
                }
            }
        });
    }

    /** The model of {@code id}, if it is one of the parts' models. */
    public static Optional<DeclaredModel> get(final ResourceLocation id) {
        return Optional.ofNullable(MODELS.get(id));
    }

    /** The bone of the kit's block at {@code x, y, z} from its piece's anchor. */
    public static String bone(final int x, final int y, final int z) {
        return "c_" + x + "_" + y + "_" + z;
    }

    /**
     * The models a ship of the spine {@code spine} and the hung parts {@code hung} is drawn with, painted in the
     * finish {@code finish}, each where its piece's anchor stands along the spine from the nose; a part or a model not
     * known is left out, a section's length kept.
     */
    public static List<Placed> of(final Map<ResourceLocation, ShipPart> parts,
                                  final Map<ResourceLocation, ShipKit> kits, final List<ResourceLocation> spine,
                                  final List<ResourceLocation> hung, final ResourceLocation finish) {
        return of(parts, kits, new ShipDesign("", spine, hung, List.of(), finish));
    }

    /** The models the ship of {@code design} is drawn with, its flipped sections drawn flipped. */
    public static List<Placed> of(final Map<ResourceLocation, ShipPart> parts,
                                  final Map<ResourceLocation, ShipKit> kits, final ShipDesign design) {
        return of(parts, kits, design, part -> true);
    }

    /**
     * The models of the parts of the ship of {@code design} that {@code drawn} says, by their numbers, the spine's
     * from the nose first, then the hung ones: those a shipyard has formed.
     */
    public static List<Placed> of(final Map<ResourceLocation, ShipPart> parts,
                                  final Map<ResourceLocation, ShipKit> kits, final ShipDesign design,
                                  final IntPredicate drawn) {
        final List<Integer> lengths = new ArrayList<>();
        for (final ResourceLocation id : design.spine()) {
            final ShipPart part = parts.get(id);
            lengths.add(part == null ? 0 : (int) part.length());
        }
        final int[] fronts = ShipLayout.fronts(lengths);
        final List<Placed> placed = new ArrayList<>();
        for (int k = 0; k < design.spine().size(); k++) {
            if (drawn.test(k)) {
                add(design.spine().get(k), kits.get(design.spine().get(k)), fronts, k, design.flips(k),
                        design.finish(), placed);
            }
        }
        for (int k = 0; k < design.hung().size(); k++) {
            if (drawn.test(design.spine().size() + k)) {
                add(design.hung().get(k), kits.get(design.hung().get(k)), fronts, 0, false, design.finish(), placed);
            }
        }
        return List.copyOf(placed);
    }

    /** The model of {@code piece} of the kit {@code kit} of the part {@code part}, in the finish {@code finish}. */
    public static ResourceLocation model(final ResourceLocation finish, final ResourceLocation part,
                                         final ShipKit kit, final ShipKit.Piece piece) {
        return id(finish.getPath(), name(part, kit, piece));
    }

    /**
     * The model the blocks of {@code piece} take their shapes from once formed: its finishes share one geometry, so
     * its blocks are shaped by the first finish's model whatever the ship is painted in.
     */
    public static ResourceLocation shape(final ResourceLocation part, final ShipKit kit, final ShipKit.Piece piece) {
        return id(FINISHES.getFirst(), name(part, kit, piece));
    }

    private static void add(final ResourceLocation part, final ShipKit kit, final int[] fronts, final int section,
                            final boolean flipped, final ResourceLocation finish, final List<Placed> placed) {
        if (kit == null) {
            return;
        }
        for (final ShipKit.Piece piece : kit.pieces()) {
            final ResourceLocation model = id(finish.getPath(), name(part, kit, piece));
            if (MODELS.containsKey(model)) {
                placed.add(new Placed(model, ShipLayout.anchor(piece.anchor(), fronts, section), flipped));
            }
        }
    }

    /* A piece's model's name: its part's, and its anchor's for a part of more than one piece. */
    private static String name(final ResourceLocation part, final ShipKit kit, final ShipKit.Piece piece) {
        return kit.pieces().size() == 1 ? part.getPath() : part.getPath() + "_" + piece.anchor().serializedName();
    }

    private static ResourceLocation id(final String finish, final String name) {
        return ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "ship/" + finish + "/" + name);
    }

    /**
     * A model of a ship's part, where it stands on the ship.
     *
     * @param model   the model's id
     * @param z       where its anchor stands along the spine, in blocks from the nose
     * @param flipped whether it is drawn flipped left for right, about the ship's centre line
     */
    public record Placed(ResourceLocation model, int z, boolean flipped) {

        /** A model drawn as it is made. */
        public Placed(final ResourceLocation model, final int z) {
            this(model, z, false);
        }
    }
}
