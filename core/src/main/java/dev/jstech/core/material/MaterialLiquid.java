/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.material;

import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import java.util.Objects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;

/**
 * A material as a liquid, registered because a mod uses it: gasoline as itself, iron molten, oxygen liquefied. Named
 * from its material ("Molten Iron"), so a material renamed names its liquid too; held in tanks and pipes as any of the
 * game's fluids, poured into the world and carried in a bucket.
 *
 * @param material    the material
 * @param id          the fluid's id: {@code jscore:molten_iron}
 * @param temperature how hot it is, in kelvin: what pipes have to stand
 */
public record MaterialLiquid(Material material, ResourceLocation id, int temperature) {

    public MaterialLiquid {
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(id, "id");
    }

    /** The id of {@code material}'s liquid: itself for a liquid, molten for a solid, liquefied for a gas. */
    public static ResourceLocation idOf(final Material material) {
        final String prefix = switch (material.roomState()) {
            case LIQUID -> "";
            case GAS -> "liquid_";
            default -> "molten_";
        };
        return ResourceLocation.fromNamespaceAndPath(material.id().getNamespace(),
                prefix + material.id().getPath());
    }

    /** What it is called: "Gasoline", "Molten Iron", "Liquid Oxygen". */
    public Text name() {
        return switch (this.material.roomState()) {
            case LIQUID -> this.material.name();
            case GAS -> MaterialTexts.LIQUEFIED.with(this.material.name());
            default -> MaterialTexts.MOLTEN.with(this.material.name());
        };
    }

    /** The fluid, as the game registered it. */
    public Fluid fluid() {
        return BuiltInRegistries.FLUID.get(this.id);
    }

    /** The id of its bucket. */
    public ResourceLocation bucketId() {
        return this.id.withSuffix("_bucket");
    }

    /** Whether it is a molten metal or another solid melted, which glows. */
    public boolean molten() {
        return this.material.roomState() != MatterState.LIQUID && this.material.roomState() != MatterState.GAS;
    }

    /** The type of a material's liquid: named from its material, for every stack and screen. */
    public static final class Type extends FluidType {

        private final MaterialLiquid liquid;

        public Type(final Properties properties, final MaterialLiquid liquid) {
            super(properties);
            this.liquid = liquid;
        }

        /** The liquid it is the type of. */
        public MaterialLiquid liquid() {
            return this.liquid;
        }

        @Override
        public Component getDescription() {
            return GameText.component(this.liquid.name());
        }

        @Override
        public Component getDescription(final FluidStack stack) {
            return getDescription();
        }
    }

    /** A material's liquid poured into the world. */
    public static final class Block extends LiquidBlock {

        private final MaterialLiquid liquid;

        public Block(final FlowingFluid fluid, final Properties properties, final MaterialLiquid liquid) {
            super(fluid, properties);
            this.liquid = liquid;
        }

        @Override
        public MutableComponent getName() {
            return GameText.component(this.liquid.name());
        }
    }

    /** A bucket of a material's liquid: "Molten Iron Bucket". */
    public static final class Bucket extends BucketItem {

        private final MaterialLiquid liquid;

        public Bucket(final Fluid fluid, final Properties properties, final MaterialLiquid liquid) {
            super(fluid, properties);
            this.liquid = liquid;
        }

        /** The liquid it holds. */
        public MaterialLiquid liquid() {
            return this.liquid;
        }

        @Override
        public Component getName(final ItemStack stack) {
            return GameText.component(MaterialTexts.BUCKET.with(this.liquid.name()));
        }
    }
}
