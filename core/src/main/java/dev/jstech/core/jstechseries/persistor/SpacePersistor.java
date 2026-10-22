/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.persistor;

import dev.jstech.core.JsCore;
import dev.jstech.core.call.MenuCall;
import dev.jstech.core.call.MenuCalls;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.Device;
import dev.jstech.core.content.DeviceBlock;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.content.IBlockModel;
import dev.jstech.core.jstechseries.JsTechTab;
import dev.jstech.core.jstechseries.SharedUses;
import dev.jstech.core.machine.MachineBlockEntity;
import dev.jstech.core.machine.MachineMenu;
import dev.jstech.core.registry.CoreItems;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * The Space Persistor, a block of J's Core's shared content: the series' chunk loader, which keeps the area its player
 * sets loaded and working for any mod, a quarry far away, a station, a starship left in orbit. Its area is a shape
 * round it with a radius, and chunks clicked on its map; how far it reaches is limited only by its owner's number and
 * the server's budget. Whether it costs energy, and how much, is the server's setting ({@link PersistorSettings}).
 */
@TextHolder
public final class SpacePersistor {

    public static final TextKey PLATE = TextKey.of("jscore.persistor.plate", "SPACE PERSISTOR");
    public static final TextKey THIS = TextKey.of("jscore.persistor.this", "THIS");
    public static final TextKey YOURS = TextKey.of("jscore.persistor.yours", "YOURS");
    public static final TextKey SERVER = TextKey.of("jscore.persistor.server", "SERVER");
    public static final TextKey POWER = TextKey.of("jscore.persistor.power", "POWER");
    public static final TextKey POWER_OFF = TextKey.of("jscore.persistor.power_off", "OFF");
    public static final TextKey SHAPE = TextKey.of("jscore.persistor.shape", "SHAPE");
    public static final TextKey RADIUS = TextKey.of("jscore.persistor.radius", "RADIUS");
    public static final TextKey SIZE = TextKey.of("jscore.persistor.size", "%s x %s");
    public static final TextKey ON = TextKey.of("jscore.persistor.on", "ON");
    public static final TextKey OFF = TextKey.of("jscore.persistor.off", "OFF");
    public static final TextKey KEEPING = TextKey.of("jscore.persistor.keeping", "KEEPING");
    public static final TextKey FULL = TextKey.of("jscore.persistor.full", "FULL");
    public static final TextKey NORTH = TextKey.of("jscore.persistor.north", "N");
    public static final TextKey ACROSS = TextKey.of("jscore.persistor.across", "%s x %s CHUNKS");
    public static final TextKey CHUNK = TextKey.of("jscore.persistor.chunk", "Chunk %s, %s");
    public static final TextKey CLICK_KEEP = TextKey.of("jscore.persistor.click_keep", "Click: keep it loaded");
    public static final TextKey CLICK_LET_GO = TextKey.of("jscore.persistor.click_let_go", "Click: let it go");
    public static final TextKey MORE_FIT = TextKey.of("jscore.persistor.more_fit", "%s more fit in your %s");
    public static final TextKey NONE_FIT = TextKey.of("jscore.persistor.none_fit", "Your %s are all kept");
    public static final TextKey OWN_CHUNK = TextKey.of("jscore.persistor.own_chunk",
            "Its own chunk, always kept");
    public static final TextKey OTHERS = TextKey.of("jscore.persistor.others", "Kept loaded by another owner");
    public static final TextKey CLOSER = TextKey.of("jscore.persistor.closer", "Closer");
    public static final TextKey FURTHER = TextKey.of("jscore.persistor.further", "Further");
    public static final TextKey RENAME = TextKey.of("jscore.persistor.rename", "Click to name it");
    public static final TextKey SQUARE = TextKey.of("jscore.persistor.shape.square", "Square");
    public static final TextKey CIRCLE = TextKey.of("jscore.persistor.shape.circle", "Circle");
    public static final TextKey LINE = TextKey.of("jscore.persistor.shape.line", "Line, across the way it faces");
    public static final TextKey FREE = TextKey.of("jscore.persistor.shape.free", "Free: only the chunks clicked");
    /** Why it keeps its chunks, for the operator who finds them kept. */
    public static final TextKey REASON = TextKey.of("jscore.persistor.reason",
            "A Space Persistor keeps the area round it loaded");

    /** The Persistor. */
    public static final BlockEntry<DeviceBlock> BLOCK = CoreItems.CONTENT.block("space_persistor",
                    Device.of(() -> SpacePersistor.ENTITY.get()).ticks(MachineBlockEntity::serverTick)
                            .opensMenu(MachineMenu::new)::block).named("Space Persistor")
            .properties(properties -> properties.strength(3.0F, 6.0F).sound(SoundType.METAL)
                    .requiresCorrectToolForDrops())
            .look(IBlockLook.fixed(new IBlockModel.BottomTop("space_persistor", "block/space_persistor/side",
                    "block/space_persistor/bottom", "block/space_persistor/top", null))).item()
            .tag(BlockTags.MINEABLE_WITH_PICKAXE).tab(JsTechTab.BLOCKS_AND_TOOLS).register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SpacePersistorBlockEntity>> ENTITY =
            CoreItems.CONTENT.blockEntity("space_persistor", SpacePersistorBlockEntity::new, BLOCK);
    /** Its screen's shape keys: the shape picked, by its place among the keys. */
    public static final MenuCall<MachineMenu, Integer> SET_SHAPE = call("persistor_shape", ByteBufCodecs.VAR_INT,
            (persistor, index) -> persistor.setShape(PersistorShape.round().get(Math.floorMod(index,
                    PersistorShape.round().size()))));
    /** Its radius' thumbwheel. */
    public static final MenuCall<MachineMenu, Integer> SET_RADIUS = call("persistor_radius", ByteBufCodecs.VAR_INT,
            SpacePersistorBlockEntity::setRadius);
    /** A click on its map: the chunk's offset from its own, packed as {@link PersistorShapes#pack} packs it. */
    public static final MenuCall<MachineMenu, Long> CLICK = call("persistor_click", ByteBufCodecs.VAR_LONG,
            (persistor, packed) -> persistor.click(PersistorShapes.dx(packed), PersistorShapes.dz(packed)));
    /** Its name, as its plate is clicked and typed on. */
    public static final MenuCall<MachineMenu, String> RENAME_TO = call("persistor_name",
            ByteBufCodecs.stringUtf8(SpacePersistorBlockEntity.LONGEST_NAME * 4), SpacePersistorBlockEntity::rename);

    static {
        // Any mod may keep its machines working far away with it, so the Core itself uses it.
        SharedUses.declare(JsCore.MODID, BLOCK::get);
    }

    private SpacePersistor() {
    }

    /** Declares the content, from the Core's constructor, while the Core's content is declared. */
    public static void declare() {
        // Loading the class declares it.
    }

    /** The name of {@code shape} on its key's tooltip. */
    public static TextKey shapeName(final PersistorShape shape) {
        return switch (shape) {
            case SQUARE -> SQUARE;
            case CIRCLE -> CIRCLE;
            case LINE -> LINE;
            case FREE -> FREE;
        };
    }

    /* A call of the Persistor's screen, made only on a Persistor's menu and only by a player who may set it up. */
    private static <A> MenuCall<MachineMenu, A> call(final String id,
                                                     final StreamCodec<? super RegistryFriendlyByteBuf, A> codec,
                                                     final Change<A> change) {
        return MenuCalls.declare(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, id), MachineMenu.class, codec,
                (menu, player, argument) -> {
                    if (menu.machine() instanceof SpacePersistorBlockEntity persistor && persistor.maySetUp(player)) {
                        change.apply(persistor, argument);
                    }
                });
    }

    /* What a call changes of the Persistor. */
    @FunctionalInterface
    private interface Change<A> {

        void apply(SpacePersistorBlockEntity persistor, A argument);
    }
}
