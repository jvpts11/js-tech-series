/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.storage;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.chemical.ChemicalBridges;
import dev.jstech.core.chemical.IChemicalBridge;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.Materials;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.matter.MatterBridges;
import dev.jstech.core.text.GameText;
import dev.jstech.core.util.Sizes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * The identity of one stored DATA type preserving an item's or fluid's data components: an item, a liquid (a fluid),
 * a material of J's Core in one of its other states of matter (a gas, a plasma, a supercritical fluid, a slurry, exotic
 * matter), or a chemical of a chemical mod that has no state in J's Core (computers/docs/design/storage.md). The
 * network makes no distinction between them: they are stored, indexed, moved and displayed as data, and everything
 * but an item weighs as a fluid does. A material is known by its id in J's Core; a chemical only by its registry id,
 * whatever mod owns it being reached through a {@link IChemicalBridge}. A chemical J's Core links to one of its
 * materials is that material, in the state it takes.
 */
public final class StorageKey {

    /** Which kind of substance a key names: the liquid kind keeps its old name, a fluid. */
    public enum Kind {
        ITEM(null), FLUID(null), GAS(MatterState.GAS), PLASMA(MatterState.PLASMA),
        SUPERCRITICAL(MatterState.SUPERCRITICAL), SLURRY(MatterState.SLURRY), EXOTIC(MatterState.EXOTIC),
        CHEMICAL(null);

        @Nullable
        private final MatterState matter;

        Kind(@Nullable final MatterState matter) {
            this.matter = matter;
        }

        /** The state of matter a key of this kind holds a material of J's Core in, or none. */
        public Optional<MatterState> matter() {
            return Optional.ofNullable(this.matter);
        }

        /** The kind that holds a material in {@code state}, or none for a state no key of its own holds. */
        public static Optional<Kind> of(final MatterState state) {
            for (final Kind kind : values()) {
                if (kind.matter == state) {
                    return Optional.of(kind);
                }
            }
            return Optional.empty();
        }

        /** The kind's name as ids and listings write it: {@code item}, {@code fluid}, {@code gas}. */
        public String serializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        /** The kind written as {@code name}, or none. */
        public static Optional<Kind> byName(final String name) {
            for (final Kind kind : values()) {
                if (kind.serializedName().equals(name)) {
                    return Optional.of(kind);
                }
            }
            return Optional.empty();
        }
    }

    public static final long MB_EQ_PER_ITEM = 1000L;

    private final Kind kind;
    private final ItemStack itemPrototype;   // count 1, components intact; non-empty iff this is an item key
    private final FluidStack fluidPrototype; // amount 1, components intact; non-empty iff this is a fluid key
    @Nullable
    private final ResourceLocation substance; // a chemical's registry id or a material's id; null for items, fluids
    private final int hash;

    private StorageKey(final Kind kind, final ItemStack itemPrototype, final FluidStack fluidPrototype,
                       @Nullable final ResourceLocation substance) {
        this.kind = kind;
        this.itemPrototype = itemPrototype;
        this.fluidPrototype = fluidPrototype;
        this.substance = substance;
        this.hash = switch (kind) {
            case FLUID -> Objects.hash(Kind.FLUID, fluidPrototype.getFluid(), fluidPrototype.getComponents());
            case ITEM -> Objects.hash(Kind.ITEM, itemPrototype.getItem(), itemPrototype.getComponents());
            default -> Objects.hash(kind, substance);
        };
    }

    public static StorageKey of(final ItemStack stack) {
        return new StorageKey(Kind.ITEM, stack.copyWithCount(1), FluidStack.EMPTY, null);
    }

    public static StorageKey of(final Item item) {
        return new StorageKey(Kind.ITEM, new ItemStack(item), FluidStack.EMPTY, null);
    }

    public static StorageKey of(final FluidStack fluid) {
        return new StorageKey(Kind.FLUID, ItemStack.EMPTY, fluid.copyWithAmount(1), null);
    }

    /**
     * A chemical key by registry id (e.g. {@code mekanism:oxygen}); a chemical J's Core links to one of its materials
     * is that material instead, in the state it takes, so a chemical mod's hydrogen and J's Core's are one.
     */
    public static StorageKey chemical(final ResourceLocation chemical) {
        final Optional<Material> material = MatterBridges.materialOfChemical(chemical);
        final Optional<Kind> kind = material.flatMap(linked -> Kind.of(linked.roomState()));
        if (material.isPresent() && kind.isPresent()) {
            return new StorageKey(kind.get(), ItemStack.EMPTY, FluidStack.EMPTY, material.get().id());
        }
        return new StorageKey(Kind.CHEMICAL, ItemStack.EMPTY, FluidStack.EMPTY, chemical);
    }

    /** A key for {@code material} in {@code state}: a gas, a plasma, a supercritical fluid, a slurry, exotic matter. */
    public static StorageKey matter(final MatterState state, final Material material) {
        return matter(Kind.of(state).orElseThrow(() -> new IllegalArgumentException("no key keeps " + state)),
                material.id());
    }

    /* A key of a kind of matter by its material's id, as a save or a packet names it. */
    private static StorageKey matter(final Kind kind, final ResourceLocation material) {
        if (kind.matter().isEmpty()) {
            throw new IllegalArgumentException(kind + " holds no matter");
        }
        return new StorageKey(kind, ItemStack.EMPTY, FluidStack.EMPTY, material);
    }

    /**
     * The key an item name stands for, as a player writes it: {@code cobblestone} or {@code minecraft:cobblestone}
     * alike, and {@code jsc:...} for this mod's own. Null for a name no item answers to.
     *
     * <p>A name with no namespace is taken as vanilla's, which is what someone typing at a prompt means by
     * {@code iron_ingot}.
     */
    @Nullable
    public static StorageKey byName(final String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        final String id = name.contains(":") ? name : "minecraft:" + name;
        final ResourceLocation location = ResourceLocation.tryParse(id.toLowerCase(Locale.ROOT));
        if (location == null) {
            return null;
        }
        return BuiltInRegistries.ITEM.getOptional(location).map(StorageKey::of).orElse(null);
    }

    /**
     * The key a text id of {@link #id()} names ({@code item|minecraft:iron_ingot}), or null when it names nothing
     * known: what a log line written with the id is shown with again.
     */
    @Nullable
    public static StorageKey byId(final String id) {
        final int bar = id == null ? -1 : id.indexOf('|');
        if (bar < 0) {
            return null;
        }
        final ResourceLocation location = ResourceLocation.tryParse(id.substring(bar + 1));
        if (location == null) {
            return null;
        }
        return switch (id.substring(0, bar)) {
            case "item" -> BuiltInRegistries.ITEM.getOptional(location).map(StorageKey::of).orElse(null);
            case "fluid" -> BuiltInRegistries.FLUID.getOptional(location).map(fluid -> new FluidStack(fluid, 1))
                    .filter(stack -> !stack.isEmpty()).map(StorageKey::of).orElse(null);
            case "chemical" -> StorageKey.chemical(location);
            default -> Kind.byName(id.substring(0, bar)).filter(kind -> kind.matter().isPresent())
                    .filter(kind -> Materials.byId(location).isPresent())
                    .map(kind -> matter(kind, location)).orElse(null);
        };
    }

    public Kind kind() {
        return kind;
    }

    public boolean isItem() {
        return kind == Kind.ITEM;
    }

    public boolean isFluid() {
        return kind == Kind.FLUID;
    }

    public boolean isChemical() {
        return kind == Kind.CHEMICAL;
    }

    /** Whether the key is a material of J's Core in one of its states of matter other than liquid. */
    public boolean isMatter() {
        return kind.matter().isPresent();
    }

    /** The material of J's Core a matter key holds, or none for any other kind or a material no longer declared. */
    public Optional<Material> material() {
        return isMatter() ? Materials.byId(Objects.requireNonNull(substance)) : Optional.empty();
    }

    /**
     * Fluids and chemicals are measured in millibuckets; one item weighs {@link #MB_EQ_PER_ITEM}.
     *
     * <p>A quantity can come from a packet, so it can be any number at all, and a thousand times a large
     * enough one comes back negative. A negative weight is not a number written badly, it is a different
     * answer: a request for it passes every check that it fits, and putting it away adds storage rather than
     * using it. Asking for more items than there could ever be gets the largest weight there is instead.
     */
    public long weight(final long quantity) {
        return kind == Kind.ITEM ? Sizes.times(quantity, MB_EQ_PER_ITEM) : quantity;
    }

    public ItemStack stack(final int count) {
        return kind == Kind.ITEM ? itemPrototype.copyWithCount(count) : ItemStack.EMPTY;
    }

    public FluidStack fluidStack(final int amount) {
        return kind == Kind.FLUID ? fluidPrototype.copyWithAmount(amount) : FluidStack.EMPTY;
    }

    /**
     * The id a chemical mod knows the key by: a chemical's own registry id, or the chemical J's Core links a matter
     * key's material to; null for items, fluids and matter no chemical mod keeps.
     */
    @Nullable
    public ResourceLocation chemicalId() {
        if (kind == Kind.CHEMICAL) {
            return substance;
        }
        return material().flatMap(MatterBridges::chemicalOf).orElse(null);
    }

    public Item item() {
        return kind == Kind.ITEM ? itemPrototype.getItem() : Items.AIR;
    }

    public ItemStack prototype() {
        return itemPrototype;
    }

    public FluidStack fluidPrototype() {
        return fluidPrototype;
    }

    public Component displayName() {
        return switch (kind) {
            case FLUID -> fluidPrototype.getHoverName();
            case CHEMICAL -> ChemicalBridges.displayName(Objects.requireNonNull(substance));
            case ITEM -> itemPrototype.getHoverName();
            default -> material().map(found -> GameText.component(found.name()))
                    .orElseGet(() -> Component.literal(Objects.requireNonNull(substance).toString()));
        };
    }

    /** The natural transfer batch: a stack of the item, or a bucket (1 000 mB) of a fluid or chemical. */
    public int batch() {
        return kind == Kind.ITEM ? Math.max(1, itemPrototype.getMaxStackSize()) : 1000;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof StorageKey key) || key.kind != kind) {
            return false;
        }
        return switch (kind) {
            case FLUID -> FluidStack.isSameFluidSameComponents(fluidPrototype, key.fluidPrototype);
            case ITEM -> ItemStack.isSameItemSameComponents(itemPrototype, key.itemPrototype);
            default -> Objects.equals(substance, key.substance);
        };
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public String toString() {
        return switch (kind) {
            case FLUID -> "fluid " + fluidPrototype.getFluid();
            case ITEM -> "item " + itemPrototype.getItem();
            default -> kind.serializedName() + " " + substance;
        };
    }

    /** The registry id of the data behind the key: the item's, the fluid's, the material's or the chemical's. */
    public ResourceLocation registryId() {
        return switch (kind) {
            case FLUID -> BuiltInRegistries.FLUID.getKey(fluidPrototype.getFluid());
            case ITEM -> BuiltInRegistries.ITEM.getKey(itemPrototype.getItem());
            default -> Objects.requireNonNull(substance);
        };
    }

    /**
     * A short text id for the data, kind and registry id ({@code item|minecraft:iron_ingot},
     * {@code gas|jscore:hydrogen}), the way a machine's settings name what is starred. Two keys that differ only in
     * their components share one id.
     */
    public String id() {
        return kind.serializedName() + "|" + registryId();
    }

    /** A chemical key on disk: {@code {"chemical": "<id>"}}. */
    private static final Codec<StorageKey> CHEMICAL_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("chemical").forGetter(key -> Objects.requireNonNull(key.substance))
    ).apply(instance, StorageKey::chemical));

    /** A key of matter on disk: {@code {"matter": "<material id>", "state": "gas"}}. */
    private static final Codec<StorageKey> MATTER_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("matter").forGetter(key -> Objects.requireNonNull(key.substance)),
            Codec.STRING.fieldOf("state").forGetter(key -> key.kind.serializedName())
    ).apply(instance, (material, state) -> matter(Kind.byName(state).filter(kind -> kind.matter().isPresent())
            .orElse(Kind.GAS), material)));

    /**
     * Items and fluids keep their original shape (the stack itself), so every existing save reads back
     * unchanged; a chemical and a key of matter are the alternatives after them.
     */
    public static final Codec<StorageKey> CODEC = Codec.either(
            Codec.either(ItemStack.CODEC, FluidStack.CODEC), Codec.either(CHEMICAL_CODEC, MATTER_CODEC)).xmap(
            either -> either.map(inner -> inner.map(StorageKey::of, StorageKey::of),
                    other -> other.map(key -> key, key -> key)),
            key -> switch (key.kind) {
                case ITEM -> Either.left(Either.left(key.stack(1)));
                case FLUID -> Either.left(Either.right(key.fluidStack(1)));
                case CHEMICAL -> Either.right(Either.left(key));
                default -> Either.right(Either.right(key));
            });

    private static final byte WIRE_ITEM = 0;
    private static final byte WIRE_FLUID = 1;
    private static final byte WIRE_CHEMICAL = 2;
    private static final byte WIRE_MATTER = 3;
    /* The longest name a kind is sent by. */
    private static final int KIND_NAME_LENGTH = 16;

    public static final StreamCodec<RegistryFriendlyByteBuf, StorageKey> STREAM_CODEC = StreamCodec.of(
            (buf, key) -> {
                switch (key.kind) {
                    case ITEM -> {
                        buf.writeByte(WIRE_ITEM);
                        ItemStack.STREAM_CODEC.encode(buf, key.stack(1));
                    }
                    case FLUID -> {
                        buf.writeByte(WIRE_FLUID);
                        FluidStack.STREAM_CODEC.encode(buf, key.fluidStack(1));
                    }
                    case CHEMICAL -> {
                        buf.writeByte(WIRE_CHEMICAL);
                        ResourceLocation.STREAM_CODEC.encode(buf, Objects.requireNonNull(key.substance));
                    }
                    default -> {
                        buf.writeByte(WIRE_MATTER);
                        buf.writeUtf(key.kind.serializedName(), KIND_NAME_LENGTH);
                        ResourceLocation.STREAM_CODEC.encode(buf, Objects.requireNonNull(key.substance));
                    }
                }
            },
            buf -> {
                final byte wire = buf.readByte();
                return switch (wire) {
                    case WIRE_FLUID -> StorageKey.of(FluidStack.STREAM_CODEC.decode(buf));
                    case WIRE_CHEMICAL -> StorageKey.chemical(ResourceLocation.STREAM_CODEC.decode(buf));
                    case WIRE_MATTER -> {
                        final Kind kind = Kind.byName(buf.readUtf(KIND_NAME_LENGTH))
                                .filter(read -> read.matter().isPresent()).orElse(Kind.GAS);
                        yield matter(kind, ResourceLocation.STREAM_CODEC.decode(buf));
                    }
                    default -> StorageKey.of(ItemStack.STREAM_CODEC.decode(buf));
                };
            });
}
