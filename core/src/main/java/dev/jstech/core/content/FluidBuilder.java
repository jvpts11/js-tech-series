/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.content;

import java.util.Objects;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

/**
 * Everything about one fluid, said once: what it is called, how it looks, how hot, heavy and thick it is, how bright
 * it glows, whether it is a gas and whether it is corrosive. {@link #register()} registers its type, its still and
 * flowing fluid and, for a liquid, the block it pours as and its bucket; the generator writes their files, names and
 * tags from the declaration:
 *
 * <pre>{@code
 * FluidEntry BRINE = CONTENT.fluid("brine").named("Brine")
 *         .look(texture("block/brine_still"), texture("block/brine_flow"), 0xFFFFFFFF)
 *         .temperature(290).corrosive().tab(FLUIDS).register();
 * }</pre>
 *
 * <p>A gas is lighter than air: it is never poured into the world nor held in a bucket, only kept in tanks and moved
 * through pipes made to hold it in.
 */
public final class FluidBuilder {

    private final ModContent content;
    private final String id;
    private @Nullable String english;
    private ResourceLocation still = WATER_STILL;
    private ResourceLocation flowing = WATER_FLOW;
    private int tint = WHITE;
    private int temperature = ROOM;
    private int density = WATER_DENSITY;
    private int viscosity = WATER_VISCOSITY;
    private int light;
    private boolean gas;
    private boolean corrosive;
    private ContentTab.@Nullable Section section;

    private static final ResourceLocation WATER_STILL = ResourceLocation.withDefaultNamespace("block/water_still");
    private static final ResourceLocation WATER_FLOW = ResourceLocation.withDefaultNamespace("block/water_flow");
    /* No tint: every channel full, so the textures are drawn as they are. A neutral value, not a colour of a look. */
    private static final int WHITE = -1;
    /** The temperature of a fluid that says none, in kelvin: the game's water. */
    private static final int ROOM = 300;
    private static final int WATER_DENSITY = 1000;
    private static final int WATER_VISCOSITY = 1000;
    private static final int BRIGHTEST = 15;

    FluidBuilder(final ModContent content, final String id) {
        this.content = content;
        this.id = id;
    }

    /** What it is called in English; its bucket is called the same with "Bucket" after it. */
    public FluidBuilder named(final String name) {
        this.english = Objects.requireNonNull(name, "name");
        return this;
    }

    /** Its still and flowing textures and the ARGB colour they are tinted with; the game's water unless said. */
    public FluidBuilder look(final ResourceLocation stillTexture, final ResourceLocation flowingTexture,
                             final int argb) {
        this.still = Objects.requireNonNull(stillTexture, "stillTexture");
        this.flowing = Objects.requireNonNull(flowingTexture, "flowingTexture");
        this.tint = argb;
        return this;
    }

    /** How hot it is, in kelvin; the game's water, 300, unless said. */
    public FluidBuilder temperature(final int kelvin) {
        if (kelvin < 0) {
            throw new IllegalArgumentException("nothing is colder than 0 K, not " + kelvin);
        }
        this.temperature = kelvin;
        return this;
    }

    /** How heavy it is: the game's water is 1000. A gas takes it as how much lighter than air it is. */
    public FluidBuilder density(final int weight) {
        this.density = weight;
        return this;
    }

    /** How thick it is, which slows how it flows: the game's water is 1000. */
    public FluidBuilder viscosity(final int thickness) {
        this.viscosity = thickness;
        return this;
    }

    /** How bright it glows, from 0 to 15. */
    public FluidBuilder light(final int level) {
        if (level < 0 || level > BRIGHTEST) {
            throw new IllegalArgumentException("a fluid glows between 0 and 15, not " + level);
        }
        this.light = level;
        return this;
    }

    /** It is a gas: lighter than air, never poured into the world nor held in a bucket. */
    public FluidBuilder gas() {
        this.gas = true;
        return this;
    }

    /** It eats through what is not made to stand it. */
    public FluidBuilder corrosive() {
        this.corrosive = true;
        return this;
    }

    /** The section of a tab its bucket is shown in; a gas has no bucket to show. */
    public FluidBuilder tab(final ContentTab.Section inSection) {
        this.section = inSection;
        return this;
    }

    /**
     * Registers the fluid and keeps what was declared.
     *
     * @throws IllegalStateException when the fluid was given no name
     */
    public FluidEntry register() {
        if (this.english == null) {
            throw new IllegalStateException(this.content.modid() + ":" + this.id + " needs a name");
        }
        final String name = this.english;
        final ResourceLocation key = ResourceLocation.fromNamespaceAndPath(this.content.modid(), this.id);
        final ResourceLocation flowingKey = ResourceLocation.fromNamespaceAndPath(this.content.modid(),
                "flowing_" + this.id);
        final boolean isGas = this.gas;
        final FluidType.Properties typeProperties = FluidType.Properties.create()
                .descriptionId("fluid_type." + this.content.modid() + "." + this.id)
                .temperature(this.temperature)
                .density(isGas ? -Math.abs(this.density) : this.density)
                .viscosity(this.viscosity)
                .lightLevel(this.light)
                .canSwim(!isGas)
                .canDrown(!isGas)
                .supportsBoating(false);
        final DeferredHolder<FluidType, FluidType> type =
                this.content.fluidTypeRegister().register(this.id, () -> new FluidType(typeProperties));
        final DeferredHolder<Fluid, BaseFlowingFluid.Source> source = DeferredHolder.create(Registries.FLUID, key);
        final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> flowingFluid =
                DeferredHolder.create(Registries.FLUID, flowingKey);
        final BaseFlowingFluid.Properties properties = new BaseFlowingFluid.Properties(type, source, flowingFluid);
        BlockEntry<LiquidBlock> block = null;
        ItemEntry<BucketItem> bucket = null;
        if (!isGas) {
            block = this.content.block(this.id, blockProperties -> new LiquidBlock(source.get(), blockProperties))
                    .properties(blockProperties -> blockProperties.mapColor(MapColor.WATER).replaceable()
                            .noCollission().strength(100.0F).pushReaction(PushReaction.DESTROY).liquid()
                            .sound(SoundType.EMPTY))
                    .named(name)
                    .look(IBlockLook.fixed(new IBlockModel.ParticleOnly(this.id, this.still.toString())))
                    .drops(Drops.NONE)
                    .register();
            final ItemBuilder<BucketItem> bucketBuilder = this.content.item(this.id + "_bucket",
                            itemProperties -> new BucketItem(source.get(),
                                    itemProperties.craftRemainder(Items.BUCKET).stacksTo(1)))
                    .named(name + " Bucket")
                    .look(IItemLook.BUCKET);
            if (this.section != null) {
                bucketBuilder.tab(this.section);
            }
            bucket = bucketBuilder.register();
            properties.block(block).bucket(bucket);
        }
        this.content.fluidRegister().register(this.id, () -> new BaseFlowingFluid.Source(properties));
        this.content.fluidRegister().register("flowing_" + this.id, () -> new BaseFlowingFluid.Flowing(properties));
        final FluidEntry entry = new FluidEntry(key, name, type, source, flowingFluid, block, bucket,
                new FluidEntry.Look(this.still, this.flowing, this.tint), isGas, this.corrosive);
        this.content.declare(entry);
        return entry;
    }
}
