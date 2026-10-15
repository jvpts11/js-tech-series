/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration.jade;

import dev.jstech.core.JsCore;
import dev.jstech.core.heat.HeatReadings;
import dev.jstech.core.heat.IHeatHolder;
import dev.jstech.core.look.IDescribed;
import dev.jstech.core.look.LookTexts;
import dev.jstech.core.mechanical.IMechanicalHolder;
import dev.jstech.core.mechanical.IMechanicalPart;
import dev.jstech.core.mechanical.LevelMechanics;
import dev.jstech.core.mechanical.MechanicalPartBlockEntity;
import dev.jstech.core.mechanical.MechanicalReadings;
import dev.jstech.core.mechanical.ShaftNetwork;
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
import net.minecraft.server.level.ServerLevel;
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
 * {@link IDescribed describes itself}, that {@link IOwned is owned}, that {@link IHeatHolder holds heat} or that turns
 * with mechanical power has its lines written by the server and shown in Jade's tooltip under the block's name: its
 * temperature, its speed, a torque meter's torque, its network's power. A mod whose block entities say what they do
 * the Core's way writes nothing for Jade of its own.
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
            if (entity instanceof IHeatHolder holder && holder.heatBody() != null && holder.heatBody().known()) {
                lines.add(HeatReadings.line(holder.heatBody().temperature()));
            }
            final IMechanicalPart part = entity instanceof IMechanicalPart own ? own
                    : entity instanceof IMechanicalHolder holder ? holder.mechanicalPart() : null;
            if (part != null && accessor.getLevel() instanceof ServerLevel server) {
                turning(lines, server, entity, part);
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
            return accessor.getBlockEntity() instanceof IDescribed || accessor.getBlockEntity() instanceof IOwned
                    || accessor.getBlockEntity() instanceof IHeatHolder
                    || accessor.getBlockEntity() instanceof IMechanicalPart;
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

        /* How fast a part turns, the torque through a torque meter, and its network's power or that it jammed. */
        private static void turning(final List<Text> lines, final ServerLevel level, final BlockEntity entity,
                                    final IMechanicalPart part) {
            lines.add(MechanicalReadings.speedLine(part.rpm()));
            if (entity instanceof MechanicalPartBlockEntity meter && meter.measures()) {
                lines.add(MechanicalReadings.torqueLine(meter.torqueThrough()));
            }
            final ShaftNetwork network = LevelMechanics.of(level).networkAt(entity.getBlockPos());
            if (network != null) {
                lines.add(network.jammed() ? MechanicalReadings.JAMMED_LINE.with()
                        : MechanicalReadings.powerLine(network.power()));
            }
        }
    }
}
