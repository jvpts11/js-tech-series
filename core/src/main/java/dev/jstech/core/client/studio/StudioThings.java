/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.studio;

import dev.jstech.core.cable.CableEntry;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.EntityEntry;
import dev.jstech.core.content.FluidEntry;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.content.ModContent;
import dev.jstech.core.content.PartEntry;
import dev.jstech.core.machine.MachineBlockEntity;
import dev.jstech.core.progression.Knowledge;
import dev.jstech.core.progression.Knowledges;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.ModList;

/**
 * The things of every mod on the Core and every content pack, read from what each declared: its blocks, a block whose
 * block entity is a machine's among the machines; its items, but for a block's own item, a fluid's bucket and a cable's
 * or a part's item, which stand for their block, fluid, cable or part; its fluids, entities, cables and parts; and the
 * research its mod declared.
 */
public final class StudioThings {

    /** What a mod that is not loaded as a mod, a content pack, is called by. */
    private static final String UNKNOWN = "";

    private StudioThings() {
    }

    /** Every thing, mod by mod in the order the mods declared their content, each mod's kind by kind. */
    public static List<StudioThing> all() {
        final Map<String, List<StudioThing>> byMod = new LinkedHashMap<>();
        for (final ModContent content : ModContent.all()) {
            byMod.put(content.modid(), of(content));
        }
        for (final Knowledge knowledge : Knowledges.all()) {
            final String modid = knowledge.id().getNamespace();
            byMod.computeIfAbsent(modid, id -> new ArrayList<>()).add(new StudioThing(modid, ThingKind.RESEARCH,
                    knowledge.id(), knowledge.name().key(), new ItemStack(Items.KNOWLEDGE_BOOK), null, null, null,
                    null, null));
        }
        final List<StudioThing> out = new ArrayList<>();
        byMod.values().forEach(things -> things.stream().sorted((a, b) -> a.kind().compareTo(b.kind()))
                .forEach(out::add));
        return Collections.unmodifiableList(out);
    }

    /** What a mod is called: its name as it is loaded, or, for a content pack, none. */
    public static String modName(final String modid) {
        return ModList.get().getModContainerById(modid).map(mod -> mod.getModInfo().getDisplayName())
                .orElse(UNKNOWN);
    }

    private static List<StudioThing> of(final ModContent content) {
        final String modid = content.modid();
        final List<StudioThing> things = new ArrayList<>();
        final Set<Item> standing = Collections.newSetFromMap(new IdentityHashMap<>());
        for (final BlockEntry<?> entry : content.declaredBlocks()) {
            final Block block = entry.get();
            final Item item = entry.hasItem() ? entry.item() : null;
            if (item != null) {
                standing.add(item);
            }
            things.add(new StudioThing(modid, isMachine(block) ? ThingKind.MACHINES : ThingKind.BLOCKS, entry.getId(),
                    block.getDescriptionId(), item == null ? ItemStack.EMPTY : new ItemStack(item), entry, block, item,
                    null, null));
        }
        for (final FluidEntry fluid : content.declaredFluids()) {
            final Item bucket = fluid.bucket().map(ItemEntry::get).orElse(null);
            if (bucket != null) {
                standing.add(bucket);
            }
            things.add(new StudioThing(modid, ThingKind.FLUIDS, fluid.id(), fluid.type().getDescriptionId(),
                    bucket == null ? ItemStack.EMPTY : new ItemStack(bucket), null,
                    fluid.block().map(BlockEntry::get).orElse(null), bucket, fluid.get(), null));
        }
        for (final CableEntry cable : content.declaredCables()) {
            standing.add(cable.asItem());
            things.add(part(modid, cable.id(), cable.asItem()));
        }
        for (final PartEntry<?> part : content.declaredParts()) {
            standing.add(part.item().get());
            things.add(part(modid, part.id(), part.item().get()));
        }
        for (final ItemEntry<?> entry : content.declaredItems()) {
            final Item item = entry.get();
            if (!standing.contains(item)) {
                things.add(new StudioThing(modid, ThingKind.ITEMS, entry.getId(), item.getDescriptionId(),
                        new ItemStack(item), null, null, item, null, null));
            }
        }
        for (final EntityEntry<?> entry : content.declaredEntities()) {
            final EntityType<?> type = entry.get();
            final SpawnEggItem egg = SpawnEggItem.byId(type);
            things.add(new StudioThing(modid, ThingKind.ENTITIES, entry.getId(), type.getDescriptionId(),
                    egg == null ? ItemStack.EMPTY : new ItemStack(egg), null, null, null, null, type));
        }
        return things;
    }

    private static StudioThing part(final String modid, final ResourceLocation id, final Item item) {
        return new StudioThing(modid, ThingKind.PARTS, id, item.getDescriptionId(), new ItemStack(item), null, null,
                item, null, null);
    }

    private static boolean isMachine(final Block block) {
        return MadeBlockEntities.of(block).filter(MachineBlockEntity.class::isInstance).isPresent();
    }
}
