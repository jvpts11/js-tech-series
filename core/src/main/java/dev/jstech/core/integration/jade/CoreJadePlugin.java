/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration.jade;

import dev.jstech.core.JsCore;
import dev.jstech.core.look.IDescribed;
import dev.jstech.core.look.LookTexts;
import dev.jstech.core.team.IOwned;
import dev.jstech.core.team.Ownership;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextTags;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * The Core's bridge to Jade, which finds it by its annotation and only when Jade is installed: a block entity that
 * {@link IDescribed describes itself}, or that {@link IOwned is owned}, has its lines written by the server and shown
 * in Jade's tooltip under the block's name. A mod whose block entities say what they do the Core's way writes nothing
 * for Jade of its own.
 */
@WailaPlugin(JsCore.MODID)
public final class CoreJadePlugin implements IWailaPlugin {

    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "described");
    private static final String LINES = "jscore_lines";
    private static final Lines PROVIDER = new Lines();

    @Override
    public void register(final IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(PROVIDER, BlockEntity.class);
    }

    @Override
    public void registerClient(final IWailaClientRegistration registration) {
        registration.registerBlockComponent(PROVIDER, Block.class);
    }

    /** The lines: written on the server, where the block entity is asked, and read into the tooltip on the client. */
    private static final class Lines implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {

        @Override
        public void appendServerData(final CompoundTag data, final BlockAccessor accessor) {
            final List<Text> lines = new ArrayList<>();
            final BlockEntity entity = accessor.getBlockEntity();
            if (entity instanceof IDescribed described) {
                described.describe(lines, accessor.showDetails());
            }
            if (entity instanceof IOwned owned && owned.ownership() != null
                    && accessor.getLevel().getServer() != null) {
                final Ownership ownership = owned.ownership();
                lines.add(LookTexts.OWNER.with(ownership.ownerName(accessor.getLevel().getServer())));
            }
            if (!lines.isEmpty()) {
                data.put(LINES, TextTags.writeAll(lines));
            }
        }

        @Override
        public boolean shouldRequestData(final BlockAccessor accessor) {
            return accessor.getBlockEntity() instanceof IDescribed || accessor.getBlockEntity() instanceof IOwned;
        }

        @Override
        public void appendTooltip(final ITooltip tooltip, final BlockAccessor accessor, final IPluginConfig config) {
            final CompoundTag data = accessor.getServerData();
            if (data.contains(LINES, Tag.TAG_LIST)) {
                for (final Text line : TextTags.readAll(data.getList(LINES, Tag.TAG_COMPOUND))) {
                    tooltip.add(GameText.component(line));
                }
            }
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }
}
