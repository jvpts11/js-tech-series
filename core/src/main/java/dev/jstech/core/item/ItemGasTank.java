/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.Materials;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.matter.IMatterHandler;
import dev.jstech.core.matter.MatterStack;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * The gas tank of an item that holds a gas, as the Core's capability of gases offers it: one tank of one material at a
 * time, kept in the item's {@link ItemStates#GAS} component, so it goes wherever the item goes. A compressed air
 * line fills a pneumatic tool's, a station fills a suit's with oxygen.
 */
public final class ItemGasTank implements IMatterHandler {

    private final ItemStack stack;
    private final long capacity;

    /**
     * How a gas is kept in an item: its material's id and its amount. A material no longer in the game reads as an
     * error, so the item loads empty instead of failing to load.
     */
    static final Codec<MatterStack> CODEC = RecordCodecBuilder.<Held>create(held -> held.group(
            ResourceLocation.CODEC.fieldOf("material").forGetter(Held::material),
            Codec.LONG.fieldOf("amount").forGetter(Held::amount)).apply(held, Held::new))
            .comapFlatMap(ItemGasTank::read, stack -> new Held(stack.material().id(), stack.amount()));

    /** The tank of {@code stack}, which holds up to {@code capacity} millibuckets at the pressure of the open air. */
    public ItemGasTank(final ItemStack stack, final long capacity) {
        this.stack = stack;
        this.capacity = capacity;
    }

    @Override
    public MatterState state() {
        return MatterState.GAS;
    }

    @Override
    public int tanks() {
        return 1;
    }

    @Override
    public Optional<MatterStack> stack(final int tank) {
        return tank == 0 ? held() : Optional.empty();
    }

    @Override
    public long capacity(final int tank) {
        return tank == 0 ? this.capacity : 0L;
    }

    @Override
    public long insert(final MatterStack given, final boolean simulate) {
        final Optional<MatterStack> held = held();
        if (!given.material().takes(MatterState.GAS)
                || held.isPresent() && held.get().material() != given.material()) {
            return 0L;
        }
        final long now = held.map(MatterStack::amount).orElse(0L);
        final long taken = Math.max(0L, Math.min(this.capacity - now, given.amount()));
        if (taken > 0 && !simulate) {
            this.stack.set(ItemStates.GAS.get(), given.withAmount(now + taken));
        }
        return taken;
    }

    @Override
    public long extract(final Material material, final long amount, final boolean simulate) {
        final Optional<MatterStack> held = held();
        if (held.isEmpty() || held.get().material() != material || amount <= 0) {
            return 0L;
        }
        final long given = Math.min(amount, held.get().amount());
        if (!simulate) {
            if (given == held.get().amount()) {
                this.stack.remove(ItemStates.GAS.get());
            } else {
                this.stack.set(ItemStates.GAS.get(), held.get().withAmount(held.get().amount() - given));
            }
        }
        return given;
    }

    @Override
    public Optional<MatterStack> extractAny(final long amount, final boolean simulate) {
        final Optional<MatterStack> held = held();
        if (held.isEmpty()) {
            return Optional.empty();
        }
        final long given = extract(held.get().material(), amount, simulate);
        return given > 0 ? Optional.of(held.get().withAmount(given)) : Optional.empty();
    }

    /** What the tank holds, or none. */
    public Optional<MatterStack> held() {
        return Optional.ofNullable(this.stack.get(ItemStates.GAS.get()));
    }

    private static DataResult<MatterStack> read(final Held held) {
        final Optional<Material> material = Materials.byId(held.material());
        if (material.isEmpty()) {
            return DataResult.error(() -> "no material " + held.material());
        }
        if (held.amount() <= 0) {
            return DataResult.error(() -> "a gas kept in an item is some, not " + held.amount());
        }
        return DataResult.success(new MatterStack(material.get(), held.amount()));
    }

    /* A gas as it is written: its material's id and its amount. */
    private record Held(ResourceLocation material, long amount) {
    }
}
