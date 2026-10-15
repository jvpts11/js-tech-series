/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.cable;

import dev.jstech.core.JsCore;
import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.cable.CableEnds;
import dev.jstech.core.cable.EndTexts;
import dev.jstech.core.cable.FlowSight;
import dev.jstech.core.cable.Wire;
import dev.jstech.core.cable.WireFlow;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.format.UnitFormatter;
import dev.jstech.core.grid.GridKind;
import dev.jstech.core.material.Materials;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import java.util.Optional;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * What flows along the wire a player looks at, beside the crosshair, while they see flows (a wrench in hand, goggles on
 * the head): what it carries, toward which end and how much a tick, and below, how the end they point at is set.
 */
@PaletteHolder
public final class FlowSightClient {

    /** The HUD layer of the reading beside the crosshair. */
    public static final ResourceLocation LAYER = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "flow_sight");
    /** The colours of the reading. */
    public static final Palette<Colours> PALETTE = Palettes.declare(JsCore.MODID, "flow_sight", new Colours(
            0xFF << 24 | 0xFFFFFF, 0xFF << 24 | 0xC8CED6, 0x96 << 24));

    private static final int FROM_CROSSHAIR = 10;
    private static final int PADDING = 4;
    private static final int LINE = 11;
    private static final float SMALL = 0.75F;
    /* How far past the player's reach a wire is still looked at, in blocks, as the wrench reaches it. */
    private static final double BEYOND_REACH = 1.0;

    private FlowSightClient() {
    }

    /** Hands the mod's bus the layer, once, from the Core's client. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterGuiLayersEvent.class, event -> event.registerAbove(VanillaGuiLayers.CROSSHAIR,
                LAYER, FlowSightClient::renderHud));
    }

    /** The name of what a wire of {@code grid} carries, by the id the wire gives it; empty for one not known. */
    public static Optional<Text> nameOf(final GridKind grid, final String what) {
        final ResourceLocation id = ResourceLocation.tryParse(what);
        if (id == null) {
            return Optional.empty();
        }
        if (grid == GridKind.ITEM) {
            final Item item = BuiltInRegistries.ITEM.get(id);
            return item == Items.AIR ? Optional.empty() : Optional.of(GameText.of(item.getDescription()));
        }
        final Fluid fluid = BuiltInRegistries.FLUID.get(id);
        if (fluid != Fluids.EMPTY) {
            return Optional.of(GameText.of(fluid.getFluidType().getDescription()));
        }
        return Materials.byId(id).map(material -> material.name());
    }

    private static void renderHud(final GuiGraphics g, final DeltaTracker delta) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null
                || minecraft.options.hideGui || !FlowSight.sees(minecraft.player)
                || !(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK
                || !(minecraft.level.getBlockEntity(hit.getBlockPos()) instanceof CableBlockEntity cable)) {
            return;
        }
        final Vec3 eye = minecraft.player.getEyePosition(delta.getGameTimeDeltaPartialTick(true));
        final Wire wire = cable.aim(eye, eye.add(minecraft.player.getLookAngle()
                .scale(minecraft.player.blockInteractionRange() + BEYOND_REACH))).wire();
        if (wire == null) {
            return;
        }
        final WireFlow flow = cable.flow(wire.slot());
        final Direction face = CableEnds.facing(hit.getBlockPos(), hit.getLocation(), hit.getDirection());
        final Optional<Text> name = flow.isEmpty() ? Optional.empty() : nameOf(wire.type().grid(), flow.what());
        final String first = name.map(what -> GameText.resolve(FlowSight.FLOW.with(what,
                EndTexts.face(flow.toward()).text(), amount(wire.type().grid(), flow.perTick()))))
                .orElseGet(() -> GameText.resolve(FlowSight.NOTHING));
        final String second = GameText.resolve(EndTexts.set(face, cable.end(wire.type(), face)));
        final int x = g.guiWidth() / 2 + FROM_CROSSHAIR;
        final int y = g.guiHeight() / 2 + FROM_CROSSHAIR / 2;
        final int width = Math.max(minecraft.font.width(first), (int) Math.ceil(minecraft.font.width(second)
                * SMALL)) + PADDING * 2;
        final Colours c = PALETTE.get();
        g.fill(x, y, x + width, y + LINE * 2 + PADDING, c.ground());
        Draw.text(g, minecraft.font, first, x + PADDING, y + PADDING / 2 + 1, c.white());
        Draw.textScaled(g, minecraft.font, second, x + PADDING, y + LINE + PADDING / 2 + 1, c.text(), SMALL);
    }

    /* How much a tick, in the unit of what a wire of {@code grid} carries. */
    private static Text amount(final GridKind grid, final long perTick) {
        final String number = UnitFormatter.forCurrentLocale().compact(perTick);
        return (grid == GridKind.ITEM ? FlowSight.ITEMS_PER_TICK : FlowSight.PER_TICK).with(number);
    }

    /**
     * The colours of the reading.
     *
     * @param white  what flows
     * @param text   how the end is set
     * @param ground the ground behind the reading
     */
    public record Colours(int white, int text, int ground) {
    }
}
