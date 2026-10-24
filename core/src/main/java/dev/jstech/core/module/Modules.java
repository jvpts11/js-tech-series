/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.module;

import dev.jstech.core.JsCore;
import dev.jstech.core.data.DeclaredData;
import dev.jstech.core.energy.ItemEnergyStorage;
import dev.jstech.core.item.ItemState;
import dev.jstech.core.item.ItemStates;
import dev.jstech.core.registry.CoreItems;
import dev.jstech.core.worn.WornItems;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.enchanting.GetEnchantmentLevelEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

/**
 * The kit of modules in worn items and in tools: an item with sockets (a host) takes modules, each into a socket of
 * its kind, and each module on does what it does, spending the energy the host holds; out of energy, it does nothing
 * until the host is charged. Hosts and modules are declared by their items' ids, in code by their mods or in data
 * packs under {@code data/<namespace>/jstech/module_hosts/} and {@code data/<namespace>/jstech/modules/}, a file
 * winning over a declaration. J's Industrial's exo-suits and J's Space's suits are hosts of this kit, and so are tools
 * like a multitool whose modules give it reach, an area, a vein or silk touch.
 *
 * <p>A worn host is worn anywhere things are worn ({@link WornItems}): its armour slot, or a slot of Curios or
 * Accessories; the modules in it work for a player each second, spending their energy each second. A tool host's
 * modules work while it is held in the main hand, and spend their energy each use (each block broken, each shot).
 */
public final class Modules {

    /** The modules in a worn item. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<InstalledModules>> INSTALLED =
            CoreItems.CONTENT.component("modules", InstalledModules.CODEC, InstalledModules.STREAM_CODEC);
    static final String NAMESPACE = JsCore.MODID;
    private static final DeclaredData<ModuleDef> MODULES = new DeclaredData<>(id("modules"), "jstech/modules",
            ModuleDef.CODEC);
    private static final DeclaredData<ModuleHostDef> HOSTS = new DeclaredData<>(id("module_hosts"),
            "jstech/module_hosts", ModuleHostDef.CODEC);
    /* What every wearer's modules changed of their attributes the second before, to take back what stops. */
    private static final Map<LivingEntity, Map<ResourceLocation, Holder<Attribute>>> CHANGED = new WeakHashMap<>();
    /* Modules work once a second. */
    private static final int EVERY = 20;
    /* What an item that holds no energy has to give. */
    private static final IEnergyStorage NO_ENERGY = new EnergyStorage(0);

    private Modules() {
    }

    /** Hands the game's bus the modules' work for players and what they make of tools, once, from the Core. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(PlayerTickEvent.Post.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player && player.tickCount % EVERY == 0) {
                work(player);
            }
        });
        NeoForge.EVENT_BUS.addListener(ItemAttributeModifierEvent.class, Modules::onToolAttributes);
        NeoForge.EVENT_BUS.addListener(GetEnchantmentLevelEvent.class, Modules::onToolEnchantments);
    }

    /**
     * What the modules working in {@code tool}, a host whose modules work while held, make of it: those on, in a
     * socket of their kind, whose energy a use takes from it, counted in socket order while its energy lasts. Nothing
     * for any other item. Its level, or none where there is none to ask by, says whose data is read.
     */
    public static ModuleTool tool(final @Nullable Level level, final ItemStack tool) {
        final ModuleTool made = new ModuleTool();
        working(level, tool, (socket, module) -> {
            made.socket("socket_" + socket);
            module.effects().forEach(effect -> effect.shape(made));
        });
        return made;
    }

    /** Spends from {@code tool} what a use takes of each module working in it. */
    public static void spendForUse(final @Nullable Level level, final ItemStack tool) {
        final long[] total = new long[1];
        working(level, tool, (socket, module) -> total[0] += module.energy());
        if (total[0] > 0) {
            energyOf(tool).extractEnergy((int) Math.min(Integer.MAX_VALUE, total[0]), false);
        }
    }

    /** Declares the item {@code item} a module, as {@code def} says while no data pack says otherwise. */
    public static ModuleDef declare(final ResourceLocation item, final ModuleDef def) {
        return MODULES.declare(item, def);
    }

    /** Declares the item {@code item} a host of modules, as {@code def} says while no data pack says otherwise. */
    public static ModuleHostDef declareHost(final ResourceLocation item, final ModuleHostDef def) {
        return HOSTS.declare(item, def);
    }

    /** What {@code stack} is as a module, on the side {@code level} is on, or none. */
    public static Optional<ModuleDef> module(final Level level, final ItemStack stack) {
        return stack.isEmpty() ? Optional.empty() : MODULES.get(level, BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    /** What {@code stack} is as a host of modules, on the side {@code level} is on, or none. */
    public static Optional<ModuleHostDef> host(final Level level, final ItemStack stack) {
        return stack.isEmpty() ? Optional.empty() : HOSTS.get(level, BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    /** The modules in {@code host}, socket by socket, as many sockets as it has; none for an item that takes none. */
    public static InstalledModules installed(final Level level, final ItemStack host) {
        final Optional<ModuleHostDef> def = host(level, host);
        if (def.isEmpty()) {
            return InstalledModules.NONE;
        }
        return host.getOrDefault(INSTALLED.get(), InstalledModules.NONE).sized(def.get().kinds().size());
    }

    /**
     * Puts one of {@code module} into {@code socket} of {@code host}, on, taking it from the stack; whether it went in.
     * It goes in only into an empty socket of its own kind, of a host with every trait it needs, and only while the
     * host holds fewer of it than it says one host takes.
     */
    public static boolean install(final Level level, final ItemStack host, final int socket, final ItemStack module) {
        final Optional<ModuleHostDef> def = host(level, host);
        final Optional<ModuleDef> fits = module(level, module);
        if (def.isEmpty() || fits.isEmpty()) {
            return false;
        }
        final List<String> kinds = def.get().kinds();
        final InstalledModules now = installed(level, host);
        if (socket < 0 || socket >= kinds.size() || !kinds.get(socket).equals(fits.get().kind())
                || !now.sockets().get(socket).module().isEmpty() || !def.get().has(fits.get().needs())) {
            return false;
        }
        if (fits.get().most() > 0) {
            int already = 0;
            for (final InstalledModules.Socketed held : now.sockets()) {
                already += ItemStack.isSameItemSameComponents(held.module(), module) ? 1 : 0;
            }
            if (already >= fits.get().most()) {
                return false;
            }
        }
        host.set(INSTALLED.get(), now.with(socket, module.copyWithCount(1), true));
        module.shrink(1);
        return true;
    }

    /** Takes the module out of {@code socket} of {@code host}; gives it, or an empty stack for an empty socket. */
    public static ItemStack remove(final Level level, final ItemStack host, final int socket) {
        final InstalledModules now = installed(level, host);
        if (socket < 0 || socket >= now.sockets().size()) {
            return ItemStack.EMPTY;
        }
        final ItemStack taken = now.sockets().get(socket).module().copy();
        host.set(INSTALLED.get(), now.with(socket, ItemStack.EMPTY, false));
        return taken;
    }

    /** Turns the module in {@code socket} of {@code host} on or off; whether there is one to turn. */
    public static boolean setOn(final Level level, final ItemStack host, final int socket, final boolean on) {
        final InstalledModules now = installed(level, host);
        if (socket < 0 || socket >= now.sockets().size() || now.sockets().get(socket).module().isEmpty()) {
            return false;
        }
        host.set(INSTALLED.get(), now.with(socket, now.sockets().get(socket).module(), on));
        return true;
    }

    /**
     * A second of work of the modules {@code wearer} wears: each on and in a socket of its kind spends its energy from
     * its host, when the host has it, and does what it does; what stopped is taken back.
     */
    public static void work(final LivingEntity wearer) {
        final Level level = wearer.level();
        final Map<ResourceLocation, Holder<Attribute>> now = new HashMap<>();
        final List<ItemStack> worn = WornItems.worn(wearer);
        for (int w = 0; w < worn.size(); w++) {
            final ItemStack host = worn.get(w);
            final Optional<ModuleHostDef> def = host(level, host);
            if (def.isEmpty() || def.get().held()) {
                continue;
            }
            final List<String> kinds = def.get().kinds();
            final List<InstalledModules.Socketed> sockets = installed(level, host).sockets();
            final IEnergyStorage energy = host.getCapability(Capabilities.EnergyStorage.ITEM);
            for (int s = 0; s < sockets.size(); s++) {
                final InstalledModules.Socketed socketed = sockets.get(s);
                final ModuleDef module = socketed.on() ? module(level, socketed.module()).orElse(null) : null;
                if (module != null && module.kind().equals(kinds.get(s)) && draw(energy, module.energy())) {
                    final ModuleWearing wearing = new ModuleWearing(wearer, "module_" + w + "_" + s, now);
                    module.effects().forEach(effect -> effect.apply(wearing));
                }
            }
        }
        final Map<ResourceLocation, Holder<Attribute>> before = CHANGED.put(wearer, now);
        if (before == null) {
            return;
        }
        for (final Map.Entry<ResourceLocation, Holder<Attribute>> change : before.entrySet()) {
            final AttributeInstance instance = wearer.getAttribute(change.getValue());
            if (!now.containsKey(change.getKey()) && instance != null) {
                instance.removeModifier(change.getKey());
            }
        }
    }

    private static void onToolAttributes(final ItemAttributeModifierEvent event) {
        final ItemStack stack = event.getItemStack();
        if (!heldHost(stack)) {
            return;
        }
        for (final ModuleTool.Change change : tool(null, stack).changes()) {
            event.addModifier(change.attribute(), change.modifier(), EquipmentSlotGroup.MAINHAND);
        }
    }

    private static void onToolEnchantments(final GetEnchantmentLevelEvent event) {
        final ItemStack stack = event.getStack();
        if (!heldHost(stack)) {
            return;
        }
        for (final Map.Entry<ResourceKey<Enchantment>, Integer> given : tool(null, stack).enchantments().entrySet()) {
            if (event.getTargetEnchant() == null || event.isTargetting(given.getKey())) {
                event.getHolder(given.getKey()).ifPresent(holder -> event.getEnchantments().upgrade(holder,
                        given.getValue()));
            }
        }
    }

    /* Whether {@code stack} is a host whose modules work while held, by whatever data is known: a quick look. */
    private static boolean heldHost(final ItemStack stack) {
        return !stack.isEmpty() && HOSTS.anywhere(BuiltInRegistries.ITEM.getKey(stack.getItem()))
                .filter(ModuleHostDef::held).isPresent();
    }

    /*
     * Tells {@code each} the modules working in {@code tool}, socket by socket, while the energy they take a use
     * lasts; nothing for an item that is no host working while held.
     */
    private static void working(final @Nullable Level level, final ItemStack tool,
                                final BiConsumer<Integer, ModuleDef> each) {
        if (tool.isEmpty()) {
            return;
        }
        final ResourceLocation id = BuiltInRegistries.ITEM.getKey(tool.getItem());
        final Optional<ModuleHostDef> def = level != null ? HOSTS.get(level, id) : HOSTS.anywhere(id);
        if (def.isEmpty() || !def.get().held()) {
            return;
        }
        final List<String> kinds = def.get().kinds();
        final List<InstalledModules.Socketed> sockets = tool.getOrDefault(INSTALLED.get(), InstalledModules.NONE)
                .sized(kinds.size()).sockets();
        long left = energyOf(tool).getEnergyStored();
        for (int s = 0; s < sockets.size(); s++) {
            final InstalledModules.Socketed socketed = sockets.get(s);
            if (!socketed.on() || socketed.module().isEmpty()) {
                continue;
            }
            final ResourceLocation moduleId = BuiltInRegistries.ITEM.getKey(socketed.module().getItem());
            final Optional<ModuleDef> module = level != null ? MODULES.get(level, moduleId)
                    : MODULES.anywhere(moduleId);
            if (module.isPresent() && module.get().kind().equals(kinds.get(s)) && module.get().energy() <= left) {
                left -= module.get().energy();
                each.accept(s, module.get());
            }
        }
    }

    /*
     * The energy {@code host} holds: the Core's own store of an item declared to hold energy, which spends at any rate
     * whatever it gives others, else the item's capability, else none.
     */
    private static IEnergyStorage energyOf(final ItemStack host) {
        final ItemState state = ItemStates.of(host);
        if (state.holdsEnergy()) {
            return new ItemEnergyStorage(host, state.energyCapacity(), 0, Integer.MAX_VALUE);
        }
        final IEnergyStorage given = host.getCapability(Capabilities.EnergyStorage.ITEM);
        return given != null ? given : NO_ENERGY;
    }

    /* Spends {@code joules} of {@code energy}, all or none; whether it was there. */
    private static boolean draw(final IEnergyStorage energy, final int joules) {
        if (joules <= 0) {
            return true;
        }
        if (energy == null || energy.extractEnergy(joules, true) < joules) {
            return false;
        }
        energy.extractEnergy(joules, false);
        return true;
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsCore.MODID, path);
    }
}
