/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration.mekanism;

import dev.jstech.core.chemical.IChemicalBridge;
import dev.jstech.core.chemical.IChemicalPort;
import dev.jstech.core.material.MatterState;
import mekanism.api.Action;
import mekanism.api.MekanismAPI;
import mekanism.api.MekanismAPITags;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.api.datamaps.IMekanismDataMapTypes;
import mekanism.api.recipes.ItemStackChemicalToObjectRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.ItemCapability;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Mekanism's chemicals (gases, infusions, pigments, slurries, one unified registry since 10.7), moved and stored like
 * fluids. The block capability is recreated by its registered name, so the bridge depends on the Mekanism API jar
 * alone and on nothing from the mod's internals.
 */
final class MekanismChemicalBridge implements IChemicalBridge {

    /** Mekanism's sided block capability for chemical handlers; capabilities are interned by name. */
    // The item form of the same capability: what a filled tank item or a hohlraum holds.
    private static final ItemCapability<IChemicalHandler, Void> ITEM_CHEMICAL_HANDLER =
            ItemCapability.createVoid(ResourceLocation.fromNamespaceAndPath("mekanism", "chemical_handler"),
                    IChemicalHandler.class);
    private static final BlockCapability<IChemicalHandler, @Nullable Direction> CHEMICAL_HANDLER =
            BlockCapability.createSided(ResourceLocation.fromNamespaceAndPath("mekanism", "chemical_handler"),
                    IChemicalHandler.class);
    /* Mekanism's tags of the chemicals each of its infusions stands for, whatever mod made one. */
    private static final List<TagKey<Chemical>> INFUSIONS = List.of(MekanismAPITags.Chemicals.CARBON,
            MekanismAPITags.Chemicals.REDSTONE, MekanismAPITags.Chemicals.DIAMOND,
            MekanismAPITags.Chemicals.REFINED_OBSIDIAN, MekanismAPITags.Chemicals.BIO, MekanismAPITags.Chemicals.FUNGI,
            MekanismAPITags.Chemicals.GOLD, MekanismAPITags.Chemicals.TIN);
    /* The folders of the pictures Mekanism draws its infusions and its pigments with. */
    private static final List<String> NO_MATTER_PICTURES = List.of("infuse_type/", "pigment/");

    @Override
    public Optional<IChemicalPort> portFor(final Level level, final BlockPos pos, @Nullable final Direction side) {
        final IChemicalHandler handler = level.getCapability(CHEMICAL_HANDLER, pos, side);
        return handler == null ? Optional.empty() : Optional.of(new HandlerPort(handler));
    }

    @Override
    public Supplier<Optional<IChemicalPort>> portCache(final ServerLevel level, final BlockPos pos,
                                                       @Nullable final Direction side) {
        final BlockCapabilityCache<IChemicalHandler, @Nullable Direction> cache =
                BlockCapabilityCache.create(CHEMICAL_HANDLER, level, pos, side);
        return () -> {
            final IChemicalHandler handler = cache.getCapability();
            return handler == null ? Optional.empty() : Optional.of(new HandlerPort(handler));
        };
    }

    @Override
    public boolean exists(final ResourceLocation chemical) {
        return MekanismAPI.CHEMICAL_REGISTRY.containsKey(chemical);
    }

    /*
     * Mekanism keeps its four kinds of chemical in one registry since 10.7, told apart by tags and by the pictures
     * each kind is drawn with. A slurry is one of its dirty or clean slurries, or a chemical another mod gives the
     * solid it was made from; an infusion (carbon, redstone, bio and the rest) and a pigment are no state of matter of
     * their own, so no line of the Core carries them; every other chemical is one of its gases, carried as its own
     * pressurised tubes carry it, rendered as a gas or not.
     */
    @Override
    public Optional<MatterState> stateOf(final ResourceLocation chemical) {
        final Optional<Holder.Reference<Chemical>> holder = MekanismAPI.CHEMICAL_REGISTRY.getHolder(chemical);
        if (holder.isEmpty()) {
            return Optional.empty();
        }
        final Holder.Reference<Chemical> found = holder.get();
        if (found.is(MekanismAPITags.Chemicals.DIRTY) || found.is(MekanismAPITags.Chemicals.CLEAN)
                || found.getData(IMekanismDataMapTypes.INSTANCE.chemicalSolidTag()) != null) {
            return Optional.of(MatterState.SLURRY);
        }
        if (INFUSIONS.stream().anyMatch(found::is) || noMatterPicture(found.value())) {
            return Optional.empty();
        }
        return Optional.of(MatterState.GAS);
    }

    @Override
    public Component displayName(final ResourceLocation chemical) {
        return lookup(chemical).getTextComponent();
    }

    @Override
    public Optional<IChemicalPort> itemPortFor(final ItemStack stack) {
        /*
         * Mekanism's item handlers are backed by the stack's own data components, so the port writes straight
         * into the stack it was made for.
         */
        final IChemicalHandler handler = stack.getCapability(ITEM_CHEMICAL_HANDLER);
        return handler == null ? Optional.empty() : Optional.of(new HandlerPort(handler));
    }

    @Override
    public Optional<ResourceLocation> chemicalOf(final ItemStack stack) {
        final IChemicalHandler handler = stack.getCapability(ITEM_CHEMICAL_HANDLER);
        if (handler == null) {
            return Optional.empty();
        }
        for (int tank = 0; tank < handler.getChemicalTanks(); tank++) {
            final ChemicalStack inTank = handler.getChemicalInTank(tank);
            if (!inTank.isEmpty()) {
                return Optional.ofNullable(HandlerPort.idOf(inTank.getChemical()));
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<ChemicalAmount> chemicalIngredient(final Object ingredient) {
        if (ingredient instanceof ChemicalStack stack && !stack.isEmpty()) {
            final ResourceLocation id = HandlerPort.idOf(stack.getChemical());
            return id == null ? Optional.empty() : Optional.of(new ChemicalAmount(id, stack.getAmount()));
        }
        return Optional.empty();
    }

    @Override
    public boolean perTickUsage(final Object recipe) {
        return recipe instanceof ItemStackChemicalToObjectRecipe<?> metered && metered.perTickUsage();
    }

    @Override
    public int tint(final ResourceLocation chemical) {
        // Forces full alpha on Mekanism's own tint, which carries none of its own: not a declared colour.
        return 0xFF << 24 | (lookup(chemical).getTint() & 0xFFFFFF);
    }

    private static Chemical lookup(final ResourceLocation chemical) {
        return MekanismAPI.CHEMICAL_REGISTRY.get(chemical);
    }

    /* Whether {@code chemical} is drawn as Mekanism draws its infusions and its pigments. */
    private static boolean noMatterPicture(final Chemical chemical) {
        final String picture = chemical.getIcon().getPath();
        return NO_MATTER_PICTURES.stream().anyMatch(picture::startsWith);
    }

    /** A chemical port over one handler; amounts are millibuckets on both sides. */
    private record HandlerPort(IChemicalHandler handler) implements IChemicalPort {

        private static Action action(final boolean simulate) {
            return simulate ? Action.SIMULATE : Action.EXECUTE;
        }

        @Override
        public long fill(final ResourceLocation chemical, final long amount, final boolean simulate) {
            if (amount <= 0 || !MekanismAPI.CHEMICAL_REGISTRY.containsKey(chemical)) {
                return 0L;
            }
            final ChemicalStack offered = new ChemicalStack(lookup(chemical).getAsHolder(), amount);
            final ChemicalStack remainder = handler.insertChemical(offered, action(simulate));
            return amount - remainder.getAmount();
        }

        @Override
        public long drain(final ResourceLocation chemical, final long amount, final boolean simulate) {
            if (amount <= 0 || !MekanismAPI.CHEMICAL_REGISTRY.containsKey(chemical)) {
                return 0L;
            }
            final ChemicalStack wanted = new ChemicalStack(lookup(chemical).getAsHolder(), amount);
            return handler.extractChemical(wanted, action(simulate)).getAmount();
        }

        @Override
        public long count(final ResourceLocation chemical) {
            long total = 0L;
            for (int tank = 0; tank < handler.getChemicalTanks(); tank++) {
                final ChemicalStack inTank = handler.getChemicalInTank(tank);
                if (!inTank.isEmpty() && chemical.equals(idOf(inTank.getChemical()))) {
                    total += inTank.getAmount();
                }
            }
            return total;
        }

        @Override
        public List<ResourceLocation> available() {
            final Set<ResourceLocation> ids = new LinkedHashSet<>();
            for (int tank = 0; tank < handler.getChemicalTanks(); tank++) {
                final ChemicalStack inTank = handler.getChemicalInTank(tank);
                if (!inTank.isEmpty()) {
                    final ResourceLocation id = idOf(inTank.getChemical());
                    if (id != null) {
                        ids.add(id);
                    }
                }
            }
            return new ArrayList<>(ids);
        }

        @Nullable
        private static ResourceLocation idOf(final Chemical chemical) {
            return MekanismAPI.CHEMICAL_REGISTRY.getKey(chemical);
        }
    }
}
