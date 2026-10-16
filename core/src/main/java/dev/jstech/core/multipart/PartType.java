/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multipart;

import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;

/**
 * A kind of part that mounts on a face of a block: what makes a new one, what a player calls it, and the model it is
 * drawn with. Registered in {@link CoreParts#REGISTRY} like any other registry entry, by a mod's own deferred
 * register, so a save names a part by its registry id and the players' games agree on which kind each id is.
 *
 * <p>A kind may have a second model for while a part of it is at work ({@link IFacePart#busy}), the same part with
 * its lamps lit, say. Its lamps can blink through an animated texture, which the game runs by itself, so the block's
 * mesh is built again only when the part starts or stops working, never at each blink.
 *
 * @param <P> the parts it makes
 */
public final class PartType<P extends IFacePart> {

    private final Supplier<P> factory;
    private final TextKey name;
    private final ResourceLocation model;
    private final ResourceLocation busyModel;
    private boolean moves;
    private boolean redstone;
    private boolean turnable;
    /* The room a part of the kind takes facing north, as made, and on each face, in the game's order of directions. */
    private List<PartBox> facingNorth = List.of(SpaceFit.deviceBox(NORTH));
    private List<List<PartBox>> onFaces = turned(this.facingNorth, 0);

    private static final int NORTH = 2;
    private static final int FACES = 6;
    private static final int QUARTERS = 4;

    /**
     * A kind drawn the same whether its parts are at work or not.
     *
     * @param factory what makes a new part of this kind, with nothing set yet
     * @param name    what a player calls it
     * @param model   the standalone block model it is drawn with, facing north
     */
    public PartType(final Supplier<P> factory, final TextKey name, final ResourceLocation model) {
        this(factory, name, model, model);
    }

    /**
     * @param factory   what makes a new part of this kind, with nothing set yet
     * @param name      what a player calls it
     * @param model     the standalone block model it is drawn with while idle, facing north
     * @param busyModel the standalone block model it is drawn with while at work, facing north
     */
    public PartType(final Supplier<P> factory, final TextKey name, final ResourceLocation model,
                    final ResourceLocation busyModel) {
        this.factory = Objects.requireNonNull(factory, "factory");
        this.name = Objects.requireNonNull(name, "name");
        this.model = Objects.requireNonNull(model, "model");
        this.busyModel = Objects.requireNonNull(busyModel, "busyModel");
    }

    /** A new part of this kind. */
    public P create() {
        return this.factory.get();
    }

    /** What a player calls this kind of part. */
    public Text text() {
        return this.name.text();
    }

    /** The standalone block model the part is drawn with while idle, facing north; its item wears it too. */
    public ResourceLocation model() {
        return this.model;
    }

    /** The standalone block model the part is drawn with, facing north, while it is at work or while idle. */
    public ResourceLocation model(final boolean busy) {
        return busy ? this.busyModel : this.model;
    }

    /** Whether the kind has a model of its own for while it is at work. */
    public boolean hasBusyModel() {
        return !this.busyModel.equals(this.model);
    }

    /**
     * Marks the kind as having something that moves, a fan's blades or a gauge's needle: besides its model, built
     * into the world's mesh, it is drawn on its own each frame by what its mod hands the Core's moving parts on the
     * player's game. Only spaces holding such a part are drawn so.
     */
    public PartType<P> moving() {
        this.moves = true;
        return this;
    }

    /** Whether the kind has something that moves, drawn each frame. */
    public boolean moves() {
        return this.moves;
    }

    /**
     * Gives the kind its own room, made facing north in pixels, in place of a plate twelve pixels across and five deep:
     * what its parts collide with, are aimed at and outlined by, and what keeps other parts out of their way.
     */
    public PartType<P> shaped(final List<PartBox> facingNorth) {
        if (facingNorth.isEmpty()) {
            throw new IllegalArgumentException("a part takes some room; give it at least one box");
        }
        this.facingNorth = List.copyOf(facingNorth);
        this.onFaces = turned(this.facingNorth, 0);
        return this;
    }

    /** The room a part of the kind takes on the face numbered {@code face}, in the order of the game's directions. */
    public List<PartBox> boxesOn(final int face) {
        return this.onFaces.get(face);
    }

    /** The room a part of the kind takes on the face numbered {@code face}, turned {@code turns} quarter turns. */
    public List<PartBox> boxesOn(final int face, final int turns) {
        return Math.floorMod(turns, QUARTERS) == 0 ? boxesOn(face) : turned(this.facingNorth, turns).get(face);
    }

    /** Marks the kind as one a wrench turns about its face, a quarter turn at a time, model and room together. */
    public PartType<P> turns() {
        this.turnable = true;
        return this;
    }

    /** Whether a wrench turns parts of the kind about their face. */
    public boolean turnable() {
        return this.turnable;
    }

    /** Marks the kind as one that deals in redstone: redstone wire beside its face joins it. */
    public PartType<P> redstone() {
        this.redstone = true;
        return this;
    }

    /** Whether redstone wire beside the face its parts stand on joins them. */
    public boolean joinsRedstone() {
        return this.redstone;
    }

    /** The id this kind is registered under, or null when it is not registered. */
    public ResourceLocation id() {
        return CoreParts.REGISTRY.getKey(this);
    }

    private static List<List<PartBox>> turned(final List<PartBox> facingNorth, final int turns) {
        final List<List<PartBox>> out = new ArrayList<>(FACES);
        for (int face = 0; face < FACES; face++) {
            final List<PartBox> on = new ArrayList<>(facingNorth.size());
            for (final PartBox box : facingNorth) {
                on.add(box.turnedAbout(turns).turnedTo(face));
            }
            out.add(List.copyOf(on));
        }
        return List.copyOf(out);
    }
}
