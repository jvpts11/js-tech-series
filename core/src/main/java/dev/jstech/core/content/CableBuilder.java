/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.content;

import dev.jstech.core.cable.CableEntry;
import dev.jstech.core.cable.CableItem;
import dev.jstech.core.cable.CableType;
import dev.jstech.core.cable.CoreCables;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

/**
 * Declares a cable a mod lays in the Core's cable block: the cable and the item that lays it, under one id, named and
 * shown in a tab. The item looks like a length of the cable's own jacket.
 */
public final class CableBuilder {

    private final ModContent content;
    private final String id;
    private final CableType.Builder type;
    private @Nullable String name;
    private ContentTab.@Nullable Section section;

    CableBuilder(final ModContent content, final String id, final CableType.Builder type) {
        this.content = content;
        this.id = id;
        this.type = Objects.requireNonNull(type, "type");
    }

    /** What the item is called in English; the other languages translate it. */
    public CableBuilder named(final String english) {
        this.name = english;
        return this;
    }

    /** The section of a creative tab the item is shown in. */
    public CableBuilder tab(final ContentTab.Section inSection) {
        this.section = inSection;
        return this;
    }

    /** Registers the cable and its item. */
    public CableEntry register() {
        if (this.name == null) {
            throw new IllegalStateException("the cable " + this.id + " has no name");
        }
        final ResourceLocation key = ResourceLocation.fromNamespaceAndPath(this.content.modid(), this.id);
        final DeferredHolder<CableType, CableType> cable = DeferredHolder.create(CoreCables.KEY, key);
        ItemBuilder<CableItem> item = this.content.item(this.id, properties -> new CableItem(properties, cable))
                .named(this.name).look(IItemLook.CABLE);
        if (this.section != null) {
            item = item.tab(this.section);
        }
        final ItemEntry<CableItem> laid = item.register();
        this.content.cableRegister().register(this.id, () -> this.type.build(laid));
        final CableEntry entry = new CableEntry(cable, laid);
        this.content.declare(entry);
        return entry;
    }
}
