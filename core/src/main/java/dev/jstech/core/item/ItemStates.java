/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.item;

import com.mojang.serialization.Codec;
import dev.jstech.core.JsCore;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.content.ModContent;
import dev.jstech.core.energy.CoreEnergy;
import dev.jstech.core.energy.ItemEnergyStorage;
import dev.jstech.core.format.Unit;
import dev.jstech.core.format.UnitFormatter;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.templates.FluidHandlerItemStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;

/**
 * Items that hold something: the components the Core keeps it in, what each declared item holds, and the mode key.
 *
 * <p>An item says what it holds where it is declared, and the Core does the rest: it gives the item the game's
 * capability for each (energy, fluid, items), keeps each in a component that goes wherever the item goes and is saved
 * with it, makes the item stack alone, and tells a player what it holds in its tooltip:
 *
 * <pre>{@code
 * CONTENT.item("field_kit", Item::new).named("Field Kit")
 *         .modes(ItemMode.of("scan", SCAN), ItemMode.of("mark", MARK))
 *         .holdsEnergy(50_000L, 500, 500).holdsFluid(4_000).holdsItems(9)
 *         .register();
 * }</pre>
 *
 * <p>The mode key changes the mode of the item in the main hand, on the server, which tells the player the new one on
 * the action bar; with shift held it goes back one.
 */
@EventBusSubscriber(modid = JsCore.MODID)
public final class ItemStates {

    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, JsCore.MODID);
    private static final String NETWORK_VERSION = "1";

    /** The id of the mode an item is in; an item that has none set is in its first. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> MODE =
            COMPONENTS.registerComponentType("item_mode", builder -> builder.persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8));
    /** The fluid an item holds. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SimpleFluidContent>> FLUID =
            COMPONENTS.registerComponentType("fluid", builder -> builder.persistent(SimpleFluidContent.CODEC)
                    .networkSynchronized(SimpleFluidContent.STREAM_CODEC));
    /** The stacks an item holds. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>> INVENTORY =
            COMPONENTS.registerComponentType("inventory", builder -> builder.persistent(ItemContainerContents.CODEC)
                    .networkSynchronized(ItemContainerContents.STREAM_CODEC).cacheEncoding());

    /* What each declared item holds, worked out the first time it is asked for, once every item is registered. */
    private static volatile @Nullable Map<Item, ItemState> byItem;

    private ItemStates() {
    }

    /** Hands the components to the Core's event bus. */
    public static void register(final IEventBus modEventBus) {
        COMPONENTS.register(modEventBus);
    }

    /** What {@code item} was declared to hold; {@link ItemState#NOTHING} for an item declared with nothing. */
    public static ItemState of(final Item item) {
        Map<Item, ItemState> known = byItem;
        if (known == null) {
            known = new IdentityHashMap<>();
            for (final ModContent content : ModContent.all()) {
                for (final ItemEntry<?> entry : content.declaredItems()) {
                    if (!entry.state().isNothing()) {
                        known.put(entry.get(), entry.state());
                    }
                }
            }
            byItem = known;
        }
        return known.getOrDefault(item, ItemState.NOTHING);
    }

    /** What the item of {@code stack} was declared to hold. */
    public static ItemState of(final ItemStack stack) {
        return of(stack.getItem());
    }

    /** The mode {@code stack} is in; empty for an item with no modes. */
    public static Optional<ItemMode> mode(final ItemStack stack) {
        final List<ItemMode> modes = of(stack).modes();
        if (modes.isEmpty()) {
            return Optional.empty();
        }
        final String saved = stack.get(MODE.get());
        for (final ItemMode mode : modes) {
            if (mode.id().equals(saved)) {
                return Optional.of(mode);
            }
        }
        return Optional.of(modes.get(0));
    }

    /** Puts {@code stack} in {@code mode}; false when it is not one of the item's modes. */
    public static boolean setMode(final ItemStack stack, final ItemMode mode) {
        if (!of(stack).modes().contains(mode)) {
            return false;
        }
        stack.set(MODE.get(), mode.id());
        return true;
    }

    /** Moves {@code stack} on to its next mode, or back to the one before; empty for an item with no modes. */
    public static Optional<ItemMode> cycleMode(final ItemStack stack, final boolean backwards) {
        final List<ItemMode> modes = of(stack).modes();
        final Optional<ItemMode> now = mode(stack);
        if (now.isEmpty()) {
            return Optional.empty();
        }
        final int step = backwards ? modes.size() - 1 : 1;
        final ItemMode next = modes.get((modes.indexOf(now.get()) + step) % modes.size());
        stack.set(MODE.get(), next.id());
        return Optional.of(next);
    }

    /** What the mode key does on the server: moves the item in the player's main hand on and says its new mode. */
    public static Optional<ItemMode> cycleHeld(final ServerPlayer player, final boolean backwards) {
        final Optional<ItemMode> next = cycleMode(player.getMainHandItem(), backwards);
        next.ifPresent(mode -> player.displayClientMessage(GameText.component(ItemTexts.MODE.with(mode.name())),
                true));
        return next;
    }

    /** What a player reads of what {@code stack} holds, a line for each thing it holds. */
    public static List<Text> describe(final ItemStack stack) {
        final ItemState state = of(stack);
        final List<Text> lines = new ArrayList<>();
        mode(stack).ifPresent(mode -> lines.add(ItemTexts.MODE.with(mode.name())));
        if (state.holdsEnergy()) {
            final UnitFormatter format = UnitFormatter.forCurrentLocale();
            final long stored = new ItemEnergyStorage(stack, state.energyCapacity(), state.energyIn(),
                    state.energyOut()).stored();
            lines.add(ItemTexts.ENERGY.with(format.full(stored, Unit.FE),
                    format.full(state.energyCapacity(), Unit.FE)));
        }
        if (state.holdsFluid()) {
            final SimpleFluidContent fluid = stack.getOrDefault(FLUID.get(), SimpleFluidContent.EMPTY);
            lines.add(fluid.isEmpty() ? ItemTexts.FLUID_EMPTY.with(state.fluidCapacity())
                    : ItemTexts.FLUID.with(GameText.of(fluid.copy().getHoverName()), fluid.getAmount(),
                            state.fluidCapacity()));
        }
        if (state.holdsItems()) {
            final long used = stack.getOrDefault(INVENTORY.get(), ItemContainerContents.EMPTY).nonEmptyStream()
                    .count();
            lines.add(ItemTexts.ITEMS.with(used, state.slots()));
        }
        return lines;
    }

    /** Gives {@code item} the game's capability for each thing {@code state} says it holds. */
    public static void registerCapabilities(final RegisterCapabilitiesEvent event, final ItemLike item,
                                            final ItemState state) {
        if (state.holdsEnergy()) {
            CoreEnergy.holds(event, item, state.energyCapacity(), state.energyIn(), state.energyOut());
        }
        if (state.holdsFluid()) {
            event.registerItem(Capabilities.FluidHandler.ITEM,
                    (stack, context) -> new FluidHandlerItemStack(FLUID, stack, state.fluidCapacity()), item);
        }
        if (state.holdsItems()) {
            event.registerItem(Capabilities.ItemHandler.ITEM,
                    (stack, context) -> new ItemInventory(stack, state.slots()), item);
        }
    }

    @SubscribeEvent
    public static void onRegisterPayloads(final RegisterPayloadHandlersEvent event) {
        event.registrar(NETWORK_VERSION).playToServer(ItemModePayload.TYPE, ItemModePayload.STREAM_CODEC,
                ItemStates::onItemMode);
    }

    /* Runs on the server: the payload is only ever sent to it. */
    private static void onItemMode(final ItemModePayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                cycleHeld(player, payload.backwards());
            }
        });
    }
}
