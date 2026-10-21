/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.studio;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/**
 * A loot table rolled many times, as the studio's loot tester asks: a block's table as that block broken with the tool
 * asked, Fortune and Silk Touch put on it, any other table that needs only a place as a chest opened where the player
 * stands; answered with how the drops spread over the rolls.
 */
final class LootService implements IStudioService {

    static final String NAME = "loot";
    /** The most rolls asked at once. */
    static final int MOST_ROLLS = 10_000;
    private static final String BLOCKS = "blocks/";

    @Override
    public void answer(final ServerPlayer player, final CompoundTag args, final Consumer<CompoundTag> reply) {
        final ServerLevel level = player.serverLevel();
        final ResourceLocation id = ResourceLocation.parse(args.getString(StudioKeys.TABLE));
        final LootTable table = level.getServer().reloadableRegistries().getLootTable(
                ResourceKey.create(Registries.LOOT_TABLE, id));
        final CompoundTag out = new CompoundTag();
        if (table == LootTable.EMPTY) {
            out.putString(StudioKeys.PROBLEM, id.toString());
            reply.accept(out);
            return;
        }
        final LootParams params = params(level, player, id, tool(level, args));
        final LootTally tally = new LootTally();
        final int rolls = Math.clamp(args.getInt(StudioKeys.ROLLS), 1, MOST_ROLLS);
        for (int i = 0; i < rolls; i++) {
            final Map<String, Integer> drops = new HashMap<>();
            for (final ItemStack stack : table.getRandomItems(params)) {
                drops.merge(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack.getCount(),
                        Integer::sum);
            }
            tally.roll(drops);
        }
        final ListTag rows = new ListTag();
        for (final LootTally.Row row : tally.rows()) {
            final CompoundTag written = new CompoundTag();
            written.putString(StudioKeys.ID, row.item());
            written.putInt(StudioKeys.IN_ROLLS, row.inRolls());
            written.putDouble(StudioKeys.PER_ROLL, row.perRoll());
            written.putInt(StudioKeys.FEWEST, row.fewest());
            written.putInt(StudioKeys.MOST, row.most());
            rows.add(written);
        }
        out.putInt(StudioKeys.ROLLS, tally.rolls());
        out.put(StudioKeys.ROWS, rows);
        reply.accept(out);
    }

    /* The tool asked, with Fortune at its level and Silk Touch when asked. */
    private static ItemStack tool(final ServerLevel level, final CompoundTag args) {
        final ResourceLocation id = ResourceLocation.tryParse(args.getString(StudioKeys.TOOL));
        final ItemStack tool = id == null || !BuiltInRegistries.ITEM.containsKey(id) ? ItemStack.EMPTY
                : new ItemStack(BuiltInRegistries.ITEM.get(id));
        if (tool.isEmpty()) {
            return tool;
        }
        final HolderLookup.RegistryLookup<Enchantment> enchantments = level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT);
        final int fortune = args.getInt(StudioKeys.FORTUNE);
        if (fortune > 0) {
            tool.enchant(enchantments.getOrThrow(Enchantments.FORTUNE), fortune);
        }
        if (args.getBoolean(StudioKeys.SILK)) {
            tool.enchant(enchantments.getOrThrow(Enchantments.SILK_TOUCH), 1);
        }
        return tool;
    }

    /* A block's table as the block broken by the player; any other as a chest opened where they stand. */
    private static LootParams params(final ServerLevel level, final ServerPlayer player, final ResourceLocation table,
                                     final ItemStack tool) {
        final LootParams.Builder builder = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, player.position())
                .withOptionalParameter(LootContextParams.THIS_ENTITY, player);
        if (table.getPath().startsWith(BLOCKS)) {
            final Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(table.getNamespace(),
                    table.getPath().substring(BLOCKS.length())));
            return builder.withParameter(LootContextParams.BLOCK_STATE, block.defaultBlockState())
                    .withParameter(LootContextParams.TOOL, tool).create(LootContextParamSets.BLOCK);
        }
        return builder.create(LootContextParamSets.CHEST);
    }
}
