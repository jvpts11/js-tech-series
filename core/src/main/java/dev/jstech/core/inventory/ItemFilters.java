/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.inventory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.id.StableCodecs;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Item filters over the game's stacks: whether a stack passes, and how a filter is saved and sent, each rule by its
 * kind ({@code exact} with the item and its components, {@code fuzzy} with the item's id, {@code tag} with the tag's
 * id), in order.
 */
public final class ItemFilters {

    private static final Codec<Long> AMOUNT = Codec.LONG.validate(amount -> amount >= 0 ? DataResult.success(amount)
            : DataResult.error(() -> "a rule carries no less than nothing: " + amount));
    private static final MapCodec<ItemFilter.Exact> EXACT = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ItemStack.SINGLE_ITEM_CODEC.fieldOf("item").forGetter(ItemFilters::stackOf),
            AMOUNT.optionalFieldOf("amount", 0L).forGetter(ItemFilter.Exact::amount)
    ).apply(instance, (stack, amount) -> new ItemFilter.Exact(StackSubject.of(stack.copyWithCount(1)), amount)));
    private static final MapCodec<ItemFilter.Fuzzy> FUZZY = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("item").forGetter(rule -> ResourceLocation.parse(rule.item())),
            AMOUNT.optionalFieldOf("amount", 0L).forGetter(ItemFilter.Fuzzy::amount)
    ).apply(instance, (item, amount) -> new ItemFilter.Fuzzy(item.toString(), amount)));
    private static final MapCodec<ItemFilter.Tag> TAG = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("tag").forGetter(rule -> ResourceLocation.parse(rule.tag())),
            AMOUNT.optionalFieldOf("amount", 0L).forGetter(ItemFilter.Tag::amount)
    ).apply(instance, (tag, amount) -> new ItemFilter.Tag(tag.toString(), amount)));
    private static final Codec<ItemFilter.Rule> RULE = Codec.STRING.partialDispatch("kind",
            rule -> DataResult.success(kindOf(rule)), ItemFilters::codecOf);

    /** How a filter is saved and sent. */
    public static final Codec<ItemFilter> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            StableCodecs.byName(ItemFilter.Mode.class).fieldOf("mode").forGetter(ItemFilter::mode),
            RULE.listOf().optionalFieldOf("rules", List.of()).forGetter(ItemFilter::rules)
    ).apply(instance, ItemFilter::new));

    private ItemFilters() {
    }

    /** Whether {@code stack} passes {@code filter}. */
    public static boolean allows(final ItemFilter filter, final ItemStack stack) {
        return filter.allows(StackSubject.of(stack));
    }

    /** A rule naming exactly {@code stack}: its item with its components. */
    public static ItemFilter.Exact exactly(final ItemStack stack, final long amount) {
        return new ItemFilter.Exact(StackSubject.of(stack.copyWithCount(1)), amount);
    }

    private static String kindOf(final ItemFilter.Rule rule) {
        return switch (rule) {
            case ItemFilter.Exact exact -> "exact";
            case ItemFilter.Fuzzy fuzzy -> "fuzzy";
            case ItemFilter.Tag tag -> "tag";
        };
    }

    private static DataResult<MapCodec<? extends ItemFilter.Rule>> codecOf(final String kind) {
        return switch (kind) {
            case "exact" -> DataResult.success(EXACT);
            case "fuzzy" -> DataResult.success(FUZZY);
            case "tag" -> DataResult.success(TAG);
            default -> DataResult.error(() -> "no kind of filter rule is called " + kind);
        };
    }

    /* The stack an exact rule of the game names; nothing for a rule made from something else. */
    private static ItemStack stackOf(final ItemFilter.Exact rule) {
        return rule.pattern() instanceof StackSubject subject ? subject.stack() : ItemStack.EMPTY;
    }
}
