/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.material;

import dev.jstech.core.content.ClaimedFlow;
import dev.jstech.core.util.DeclarationError;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.wrappers.FluidBucketWrapper;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.jetbrains.annotations.ApiStatus;

/**
 * The materials' liquids the installed mods use: a liquid exists in the game only when some mod says it uses it, and
 * the Core registers the union, each once, its fluid, the block it pours as and its bucket, named from its material
 * and drawn in its colour as the game loads. A mod says how hot the liquid it uses is, since the pipes that carry it
 * have to stand that; where mods differ, the hottest is kept.
 *
 * <pre>{@code
 * public static final Supplier<Fluid> MOLTEN_TIN = MaterialLiquids.use(MODID, SeriesMaterials.TIN, 505);
 * }</pre>
 */
public final class MaterialLiquids {

    private static final List<Use> USES = new CopyOnWriteArrayList<>();
    private static final Map<ResourceLocation, Material> LINKS = new ConcurrentHashMap<>();

    /** The temperature of a liquid at room temperature, in kelvin, as the game's water. */
    public static final int ROOM = 300;

    /* How bright a melted solid glows. */
    private static final int MOLTEN_LIGHT = 12;
    private static final int WATER_DENSITY = 1000;
    private static final int WATER_VISCOSITY = 1000;

    private static volatile List<MaterialLiquid> registered = List.of();
    private static volatile boolean closed;

    private MaterialLiquids() {
    }

    /**
     * Declares that the mod {@code modid} uses {@code material} as a liquid, at {@code kelvin}, and gives its fluid.
     *
     * @throws DeclarationError when the material cannot be a liquid, the temperature is below nothing, or the game
     *                          has already registered its things
     */
    public static Supplier<Fluid> use(final String modid, final Material material, final int kelvin) {
        if (closed) {
            throw DeclarationError.of(modid, "the use of " + material + " as a liquid",
                    "comes after the game registered its things", "declare uses from the mod's constructor");
        }
        if (!material.takes(MatterState.LIQUID)) {
            throw DeclarationError.of(modid, "the use of " + material + " as a liquid",
                    "is of a material that cannot be one", "declare it with also(MatterState.LIQUID)");
        }
        if (kelvin < 0) {
            throw DeclarationError.of(modid, "the use of " + material + " as a liquid",
                    "is at " + kelvin + " K, colder than nothing can be", "give its temperature in kelvin");
        }
        if (LINKS.containsValue(material)) {
            throw DeclarationError.of(modid, "the use of " + material + " as a liquid",
                    "is of a material whose liquid the game or another mod has already", "use that liquid");
        }
        USES.add(new Use(modid, material, kelvin));
        return fluid(material);
    }

    /**
     * Declares that the liquid {@code fluid}, the game's own or another mod's, is {@code material}: the game's water is
     * the series' water. The Core registers no liquid of its own for the material, and what heat does to the liquid
     * is the material's. The fluid is named by id alone, so a link to an absent mod's liquid sleeps.
     *
     * @throws DeclarationError when the fluid is linked to a material already
     */
    public static void link(final Material material, final ResourceLocation fluid) {
        Objects.requireNonNull(material, "material");
        if (LINKS.putIfAbsent(Objects.requireNonNull(fluid, "fluid"), material) != null) {
            throw DeclarationError.of(fluid.getNamespace(), "the liquid " + fluid, "is linked to a material twice",
                    "link it once");
        }
    }

    /** The material the liquid {@code fluid} is: a material's liquid's own, or the one linked to it; none otherwise. */
    public static Optional<Material> materialOf(final Fluid fluid) {
        if (fluid.getFluidType() instanceof MaterialLiquid.Type type) {
            return Optional.of(type.liquid().material());
        }
        final Fluid source = fluid instanceof FlowingFluid flowing ? flowing.getSource() : fluid;
        return Optional.ofNullable(LINKS.get(BuiltInRegistries.FLUID.getKey(source)));
    }

    /** {@code material}'s liquid, once registered. */
    public static Supplier<Fluid> fluid(final Material material) {
        return DeferredHolder.create(Registries.FLUID, MaterialLiquid.idOf(material));
    }

    /** The liquids the Core registered, in the order the materials were declared. */
    public static List<MaterialLiquid> registered() {
        return registered;
    }

    /** The registered liquid of {@code material}, or none. */
    public static Optional<MaterialLiquid> of(final Material material) {
        for (final MaterialLiquid liquid : registered) {
            if (liquid.material() == material) {
                return Optional.of(liquid);
            }
        }
        return Optional.empty();
    }

    /** The mods that use a liquid. */
    public static List<String> users() {
        final List<String> out = new ArrayList<>();
        USES.forEach(use -> {
            if (!out.contains(use.modid())) {
                out.add(use.modid());
            }
        });
        return out;
    }

    /** The buckets of the liquids the Core registered. */
    public static List<Item> buckets() {
        final List<Item> out = new ArrayList<>();
        for (final MaterialLiquid liquid : registered) {
            out.add(BuiltInRegistries.ITEM.get(liquid.bucketId()));
        }
        return out;
    }

    /** Hands the mod's bus the registering of the union, once, from the Core's constructor. */
    @ApiStatus.Internal
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterEvent.class, MaterialLiquids::onRegister);
        modBus.addListener(RegisterCapabilitiesEvent.class, MaterialLiquids::onCapabilities);
    }

    /*
     * The game gives the fluid handler of a bucket to its own bucket class alone; a material's bucket is a kind of its
     * own, so it is given the same here, which every tank, pipe and drawing of the fluid in it asks for.
     */
    private static void onCapabilities(final RegisterCapabilitiesEvent event) {
        final List<Item> buckets = buckets();
        if (!buckets.isEmpty()) {
            event.registerItem(Capabilities.FluidHandler.ITEM, (stack, context) -> new FluidBucketWrapper(stack),
                    buckets.toArray(Item[]::new));
        }
    }

    private static void onRegister(final RegisterEvent event) {
        event.register(NeoForgeRegistries.Keys.FLUID_TYPES, helper -> {
            settle();
            for (final MaterialLiquid liquid : registered) {
                helper.register(liquid.id(), new MaterialLiquid.Type(FluidType.Properties.create()
                        .descriptionId("fluid_type." + liquid.id().getNamespace() + "." + liquid.id().getPath())
                        .temperature(liquid.temperature()).density(WATER_DENSITY).viscosity(WATER_VISCOSITY)
                        .lightLevel(liquid.molten() ? MOLTEN_LIGHT : 0).canSwim(true).canDrown(true)
                        .supportsBoating(false), liquid));
            }
        });
        event.register(Registries.FLUID, helper -> {
            settle();
            for (final MaterialLiquid liquid : registered) {
                final BaseFlowingFluid.Properties properties = properties(liquid);
                helper.register(liquid.id(), new ClaimedFlow.Source(properties));
                helper.register(flowingId(liquid), new ClaimedFlow.Flowing(properties));
            }
        });
        event.register(Registries.BLOCK, helper -> {
            settle();
            for (final MaterialLiquid liquid : registered) {
                helper.register(liquid.id(), new MaterialLiquid.Block(source(liquid).get(), BlockBehaviour
                        .Properties.of().mapColor(MapColor.WATER).replaceable().noCollission().strength(100.0F)
                        .pushReaction(PushReaction.DESTROY).liquid().noLootTable().sound(SoundType.EMPTY)
                        .lightLevel(state -> liquid.molten() ? MOLTEN_LIGHT : 0), liquid));
            }
        });
        event.register(Registries.ITEM, helper -> {
            settle();
            for (final MaterialLiquid liquid : registered) {
                helper.register(liquid.bucketId(), new MaterialLiquid.Bucket(source(liquid).get(),
                        new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1), liquid));
            }
        });
    }

    /* The uses close, and their union is taken, at the first of the registries the liquids are registered in. */
    private static synchronized void settle() {
        if (!closed) {
            closed = true;
            registered = union();
        }
    }

    private static BaseFlowingFluid.Properties properties(final MaterialLiquid liquid) {
        final DeferredHolder<FluidType, FluidType> type = DeferredHolder.create(NeoForgeRegistries.Keys.FLUID_TYPES,
                liquid.id());
        final DeferredHolder<Fluid, BaseFlowingFluid.Source> source = source(liquid);
        final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> flowing = DeferredHolder.create(Registries.FLUID,
                flowingId(liquid));
        return new BaseFlowingFluid.Properties(type, source, flowing)
                .block(DeferredHolder.create(Registries.BLOCK, liquid.id()))
                .bucket(DeferredHolder.create(Registries.ITEM, liquid.bucketId()));
    }

    private static DeferredHolder<Fluid, BaseFlowingFluid.Source> source(final MaterialLiquid liquid) {
        return DeferredHolder.create(Registries.FLUID, liquid.id());
    }

    private static ResourceLocation flowingId(final MaterialLiquid liquid) {
        return liquid.id().withPrefix("flowing_");
    }

    /* Each liquid once, at the hottest any mod uses it at, in the order the materials were declared. */
    private static List<MaterialLiquid> union() {
        final Map<Material, Integer> hottest = new LinkedHashMap<>();
        for (final Use use : USES) {
            hottest.merge(use.material(), use.kelvin(), Math::max);
        }
        final List<Material> order = Materials.all();
        final List<Material> materials = new ArrayList<>(hottest.keySet());
        materials.sort((a, b) -> Integer.compare(order.indexOf(a), order.indexOf(b)));
        final List<MaterialLiquid> out = new ArrayList<>();
        for (final Material material : materials) {
            out.add(new MaterialLiquid(material, MaterialLiquid.idOf(material), hottest.get(material)));
        }
        return Collections.unmodifiableList(out);
    }

    /* One mod's use of one liquid. */
    private record Use(String modid, Material material, int kelvin) {
    }
}
