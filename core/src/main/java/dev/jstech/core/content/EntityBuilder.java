/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.content;

import dev.jstech.core.util.DeclarationError;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import org.jetbrains.annotations.Nullable;

/**
 * Everything about one kind of entity, said once: what makes it, how big it is, what it is called, how far players
 * see it, and for a living one the attributes it starts with. {@link #register()} registers it and keeps the
 * declaration for the generator and the checks; the Core hands the game a living entity's attributes and, for one
 * that holds energy, the energy capability.
 *
 * <pre>{@code
 * CONTENT.entity("rover", Rover::new, MobCategory.MISC).size(1.4F, 1.0F).named("Rover").holdsEnergy().register();
 * }</pre>
 * @param <E> the entity's class
 */
public final class EntityBuilder<E extends Entity> {

    private final ModContent content;
    private final String id;
    private final EntityType.EntityFactory<E> factory;
    private final MobCategory category;
    private float width = 0.6F;
    private float height = 1.8F;
    private int trackingRange = 8;
    private int updateInterval = 3;
    private boolean fireImmune;
    private boolean holdsEnergy;
    private boolean noSummon;
    private @Nullable String english;
    private @Nullable Supplier<AttributeSupplier.Builder> attributes;

    EntityBuilder(final ModContent content, final String id, final EntityType.EntityFactory<E> factory,
                  final MobCategory category) {
        this.content = content;
        this.id = id;
        this.factory = factory;
        this.category = category;
    }

    /** What the entity is called in English. */
    public EntityBuilder<E> named(final String name) {
        this.english = name;
        return this;
    }

    /** How wide and how tall it is, in blocks. */
    public EntityBuilder<E> size(final float wide, final float tall) {
        this.width = wide;
        this.height = tall;
        return this;
    }

    /** How far away, in chunks, players see it, and every how many ticks they are told where it is. */
    public EntityBuilder<E> tracking(final int chunks, final int everyTicks) {
        this.trackingRange = chunks;
        this.updateInterval = everyTicks;
        return this;
    }

    public EntityBuilder<E> fireImmune() {
        this.fireImmune = true;
        return this;
    }

    /** The {@code /summon} command does not make it: an entity made only by its mod, as the mod's rules say. */
    public EntityBuilder<E> noSummon() {
        this.noSummon = true;
        return this;
    }

    /** A living entity's attributes: health, speed, and the rest it starts with. */
    public EntityBuilder<E> attributes(final Supplier<AttributeSupplier.Builder> start) {
        this.attributes = start;
        return this;
    }

    /** Its energy is reached by cables and chargers through the game's energy capability. */
    public EntityBuilder<E> holdsEnergy() {
        this.holdsEnergy = true;
        return this;
    }

    /**
     * Registers the entity type and keeps the declaration.
     *
     * @throws DeclarationError when it was not named
     */
    public EntityEntry<E> register() {
        if (english == null) {
            throw DeclarationError.of(content.modid(), "the entity " + content.modid() + ":" + id, "has no name",
                    "give it its English name with named(...) before register()");
        }
        final ResourceLocation key = ResourceLocation.fromNamespaceAndPath(content.modid(), id);
        final EntityEntry<E> entry = new EntityEntry<>(key, english, attributes, holdsEnergy);
        final float wide = width;
        final float tall = height;
        final int range = trackingRange;
        final int interval = updateInterval;
        final boolean immune = fireImmune;
        final boolean unsummoned = noSummon;
        content.entityRegister().register(id, () -> {
            EntityType.Builder<E> builder = EntityType.Builder.of(factory, category).sized(wide, tall)
                    .clientTrackingRange(range).updateInterval(interval);
            if (immune) {
                builder = builder.fireImmune();
            }
            if (unsummoned) {
                builder = builder.noSummon();
            }
            return builder.build(key.toString());
        });
        content.declare(entry);
        return entry;
    }
}
