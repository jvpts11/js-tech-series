/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.content;

import dev.jstech.core.multipart.IFacePart;
import dev.jstech.core.multipart.PartBox;
import dev.jstech.core.multipart.PartItem;
import dev.jstech.core.multipart.PartType;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.util.DeclarationError;
import java.util.List;
import java.util.function.Supplier;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

/**
 * Declares a kind of part a mod mounts on the faces of cables and spaces of parts, and the item that mounts it, under
 * one id: named once, for the kind and its item, shown in a tab, drawn as its look says, and with what it does on its
 * face. The data generation writes the item's model, its name and, for a plate, the part's model.
 *
 * <pre>{@code
 * public static final PartEntry<SensorPart> SENSOR = CONTENT.part("sensor", SensorPart::new).named("Sensor")
 *         .look(PartLook.plate("block/part/sensor_front", "block/part/sensor_side")).redstone()
 *         .tab(MyTab.TOOLS).register();
 * }</pre>
 *
 * @param <P> the parts it makes
 */
public final class PartBuilder<P extends IFacePart> {

    private final ModContent content;
    private final String id;
    private final Supplier<P> factory;
    private @Nullable String name;
    private @Nullable PartLook look;
    private ContentTab.@Nullable Section section;
    private @Nullable List<PartBox> room;
    private boolean redstone;
    private boolean turns;
    private boolean moves;

    /* The first word of an item's key, as the game writes it. */
    private static final String ITEM = "item";

    PartBuilder(final ModContent content, final String id, final Supplier<P> factory) {
        this.content = content;
        this.id = id;
        this.factory = factory;
    }

    /** What the part and its item are called in English; the other languages translate it. */
    public PartBuilder<P> named(final String english) {
        this.name = english;
        return this;
    }

    /** How the part looks. */
    public PartBuilder<P> look(final PartLook partLook) {
        this.look = partLook;
        return this;
    }

    /** The room it takes, made facing north, in pixels, in place of a device's plate ({@link PartType#shaped}). */
    public PartBuilder<P> shaped(final List<PartBox> facingNorth) {
        this.room = List.copyOf(facingNorth);
        return this;
    }

    /** It deals in redstone: wire beside its face joins it ({@link PartType#redstone()}). */
    public PartBuilder<P> redstone() {
        this.redstone = true;
        return this;
    }

    /** A wrench turns it about its face ({@link PartType#turns()}). */
    public PartBuilder<P> turns() {
        this.turns = true;
        return this;
    }

    /** It has something that moves, drawn each frame ({@link PartType#moving()}). */
    public PartBuilder<P> moving() {
        this.moves = true;
        return this;
    }

    /** The section of a creative tab its item is shown in. */
    public PartBuilder<P> tab(final ContentTab.Section inSection) {
        this.section = inSection;
        return this;
    }

    /** Registers the kind of part and its item. */
    public PartEntry<P> register() {
        if (this.name == null) {
            throw DeclarationError.of(this.content.modid(), "the part " + this.content.modid() + ":" + this.id,
                    "has no name", "give it its English name with named(...) before register()");
        }
        if (this.look == null) {
            throw DeclarationError.of(this.content.modid(), "the part " + this.content.modid() + ":" + this.id,
                    "has no look", "give it a look with look(PartLook.plate(...)) or look(PartLook.handmade(...))");
        }
        final PartLook drawn = this.look;
        // The kind is called what its item is, under the item's own key, which the item's name fills.
        final String itemKey = String.join(".", ITEM, this.content.modid(), this.id);
        final TextKey text = TextKey.of(itemKey, this.name);
        final DeferredHolder<PartType<?>, PartType<P>> type = this.content.partRegister().register(this.id,
                () -> made(text, drawn));
        ItemBuilder<PartItem> item = this.content.item(this.id, properties -> new PartItem(properties, type))
                .named(this.name).look(IItemLook.parent(drawn.model(this.content.modid(), this.id).toString()));
        if (this.section != null) {
            item = item.tab(this.section);
        }
        final PartEntry<P> entry = new PartEntry<>(type, item.register(), drawn);
        this.content.declare(entry);
        return entry;
    }

    private PartType<P> made(final TextKey text, final PartLook drawn) {
        final PartType<P> type = new PartType<>(this.factory, text, drawn.model(this.content.modid(), this.id),
                drawn.busyModel(this.content.modid(), this.id));
        if (this.room != null) {
            type.shaped(this.room);
        }
        if (this.redstone) {
            type.redstone();
        }
        if (this.turns) {
            type.turns();
        }
        if (this.moves) {
            type.moving();
        }
        return type;
    }
}
