/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.registry;

import com.mojang.serialization.Codec;
import dev.jstech.core.JsCore;
import dev.jstech.core.grid.LevelGrids;
import dev.jstech.core.heat.LevelHeat;
import dev.jstech.core.mechanical.LevelMechanics;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.persistence.CoreChunkData;
import dev.jstech.core.persistence.SaveLayout;
import dev.jstech.core.uuid.NetworkUuid;
import net.minecraft.core.UUIDUtil;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Supplier;

/**
 * Registers the mod's {@link AttachmentType}s.
 */
public final class CoreAttachments {

    private CoreAttachments() {
    }

    private static final Codec<NetworkUuid> NETWORK_UUID_CODEC =
            UUIDUtil.CODEC.xmap(NetworkUuid::new, NetworkUuid::value);

    private static final Codec<CoreChunkData> CHUNK_DATA_CODEC =
            NETWORK_UUID_CODEC.listOf().xmap(
                    list -> CoreChunkData.of(new LinkedHashSet<>(list)),
                    data -> List.copyOf(data.networks()));

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, JsCore.MODID);

    public static final Supplier<AttachmentType<NetworkSystem>> NETWORK_SYSTEM =
            ATTACHMENT_TYPES.register("network_system",
                    () -> AttachmentType.builder(NetworkSystem::new).build());

    /**
     * A dimension's grids of every kind but data, with the work that shares what each carries; never saved, since the
     * cables put themselves back as they load.
     */
    public static final Supplier<AttachmentType<LevelGrids>> GRIDS =
            ATTACHMENT_TYPES.register("grids", () -> AttachmentType.builder(LevelGrids::new).build());

    /**
     * A dimension's heat: the blocks that hold heat and are not in balance; never saved, since each block's heat is
     * saved with it and wakes as it loads.
     */
    public static final Supplier<AttachmentType<LevelHeat>> HEAT =
            ATTACHMENT_TYPES.register("heat", () -> AttachmentType.builder(LevelHeat::new).build());

    /**
     * A dimension's mechanical power: its networks of parts that turn; never saved, since each part's speed is saved
     * with it and its network is joined again as it loads.
     */
    public static final Supplier<AttachmentType<LevelMechanics>> MECHANICS =
            ATTACHMENT_TYPES.register("mechanics", () -> AttachmentType.builder(LevelMechanics::new).build());

    /** The layout a chunk's networks are saved in; a chunk saved before it had one holds the bare list. */
    public static final SaveLayout CHUNK_NETWORKS_LAYOUT = SaveLayout.of(JsCore.MODID + ":chunk_networks");

    public static final Supplier<AttachmentType<CoreChunkData>> CHUNK_NETWORKS =
            ATTACHMENT_TYPES.register("chunk_networks",
                    () -> AttachmentType.builder(CoreChunkData::empty)
                            .serialize(CHUNK_NETWORKS_LAYOUT.codec(CHUNK_DATA_CODEC))
                            .build());

    public static void register(final IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
