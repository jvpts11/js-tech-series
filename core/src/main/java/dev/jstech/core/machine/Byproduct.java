/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/**
 * What a recipe makes besides its outputs: a grinder's stone dust, a separator's trace of gold. It comes one of two
 * ways. By chance, {@code "chance": 0.1}, it comes one recipe in ten on average, as luck has it. By a share,
 * {@code "share": 0.25}, the machine counts ({@link Tally}): a quarter of it each recipe, so it comes every fourth
 * recipe exactly, and a share above one is more than one each time. A machine starts a recipe only with room for its
 * byproducts too, so it never ends one it cannot finish.
 *
 * <pre>{@code
 * "byproducts": [{"item": {"id": "jscore:sulfur_dust"}, "share": 0.25},
 *                {"item": {"id": "minecraft:cobblestone"}, "chance": 0.1}]
 * }</pre>
 *
 * @param stack  what it makes
 * @param amount its chance, from above none to always; or its share of a recipe, above none, for a counted one
 * @param draw   how it comes
 */
public record Byproduct(ItemStack stack, float amount, Draw draw) {

    /** The most a counted byproduct makes in one recipe: a stack. */
    public static final float MOST_SHARE = 64.0F;
    public static final Codec<Byproduct> CODEC = Written.CODEC.flatXmap(Byproduct::of,
            byproduct -> DataResult.success(byproduct.written()));
    public static final StreamCodec<RegistryFriendlyByteBuf, Byproduct> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC, Byproduct::stack,
            ByteBufCodecs.FLOAT, Byproduct::amount,
            ByteBufCodecs.BOOL, byproduct -> byproduct.draw() == Draw.SHARE,
            (stack, amount, counted) -> new Byproduct(stack, amount, counted ? Draw.SHARE : Draw.CHANCE));

    public Byproduct {
        Objects.requireNonNull(stack, "stack");
        Objects.requireNonNull(draw, "draw");
        if (draw == Draw.CHANCE && !(amount > 0.0F && amount <= 1.0F)) {
            throw new IllegalArgumentException("a byproduct comes now and then or always, not at " + amount);
        }
        if (draw == Draw.SHARE && !(amount > 0.0F && amount <= MOST_SHARE)) {
            throw new IllegalArgumentException("a counted byproduct comes a share of each recipe above none and up"
                    + " to a stack, not " + amount);
        }
        stack = stack.copy();
    }

    /** A byproduct that comes by chance, {@code chance} of the recipes on average. */
    public Byproduct(final ItemStack stack, final float chance) {
        this(stack, chance, Draw.CHANCE);
    }

    /** A byproduct the machine counts, {@code share} of it each recipe. */
    public static Byproduct counted(final ItemStack stack, final float share) {
        return new Byproduct(stack, share, Draw.SHARE);
    }

    /** Its chance of coming, for one that comes by chance; a certainty for a counted one, which always comes due. */
    public float chance() {
        return this.draw == Draw.CHANCE ? this.amount : 1.0F;
    }

    /** Whether it comes this time, for one that comes by chance. */
    public boolean comes(final RandomSource random) {
        return this.amount >= 1.0F || random.nextFloat() < this.amount;
    }

    /** What its machine's count of it is kept under: its item, so two recipes of one byproduct count it together. */
    public String countedAs() {
        return this.stack.getItemHolder().getRegisteredName();
    }

    /* A byproduct as its file writes it, with a chance or a share and never both. */
    private static DataResult<Byproduct> of(final Written written) {
        if (written.chance().isPresent() == written.share().isPresent()) {
            return DataResult.error(() -> "a byproduct comes by a chance or by a share, and by one of them only");
        }
        return DataResult.success(written.chance().map(chance -> new Byproduct(written.stack(), chance, Draw.CHANCE))
                .orElseGet(() -> new Byproduct(written.stack(), written.share().orElseThrow(), Draw.SHARE)));
    }

    private Written written() {
        return new Written(this.stack, this.draw == Draw.CHANCE ? Optional.of(this.amount) : Optional.empty(),
                this.draw == Draw.SHARE ? Optional.of(this.amount) : Optional.empty());
    }

    /* How a byproduct is written in its recipe's file. */
    private record Written(ItemStack stack, Optional<Float> chance, Optional<Float> share) {

        static final Codec<Written> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ItemStack.CODEC.fieldOf("item").forGetter(Written::stack),
                Codec.floatRange(Float.MIN_NORMAL, 1.0F).optionalFieldOf("chance").forGetter(Written::chance),
                Codec.floatRange(Float.MIN_NORMAL, MOST_SHARE).optionalFieldOf("share").forGetter(Written::share)
        ).apply(instance, Written::new));
    }

    /** How a byproduct comes. */
    public enum Draw {
        /** By luck, its chance of the recipes on average. */
        CHANCE,
        /** By the machine's count, its share of each recipe exactly. */
        SHARE
    }
}
