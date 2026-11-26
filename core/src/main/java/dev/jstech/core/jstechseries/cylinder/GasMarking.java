/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.cylinder;

import dev.jstech.core.JsCore;
import dev.jstech.core.item.ItemGasTank;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.Materials;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.matter.IMatterHandler;
import dev.jstech.core.matter.MatterCapabilities;
import io.netty.buffer.ByteBuf;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.ApiStatus;

/**
 * Marking an empty Gas Cylinder for a gas: used in the air, it opens the list of every gas of the Core, made yet or
 * not, and the gas picked marks the cylinder in that hand, on the server. A gas had no name in a pattern until a
 * machine had made it into a cylinder.
 */
public final class GasMarking {

    /* The version of the payload of a gas picked. */
    private static final String NETWORK_VERSION = "1";
    /* What opens the list on a player's game, handed over by the Core's client; nothing on a server. */
    private static Consumer<InteractionHand> opener = hand -> { };

    private GasMarking() {
    }

    /** Hands the mod's bus the payload of a gas picked, once, from the Core's constructor. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterPayloadHandlersEvent.class, event -> event.registrar(NETWORK_VERSION)
                .playToServer(MarkPayload.TYPE, MarkPayload.STREAM_CODEC, GasMarking::onMark));
    }

    /** Every gas of the Core, made yet or not: each material that is a gas at room temperature, by its id. */
    public static List<Material> gases() {
        return Materials.all().stream().filter(material -> material.roomState() == MatterState.GAS)
                .sorted(Comparator.comparing(material -> material.id().toString())).toList();
    }

    /** Asks the server to mark the cylinder in {@code hand} for {@code gas}; from a player's game. */
    public static void mark(final InteractionHand hand, final Material gas) {
        PacketDistributor.sendToServer(new MarkPayload(hand == InteractionHand.MAIN_HAND, gas.id()));
    }

    /** Hands over what opens the list on a player's game, once, from the Core's client. */
    @ApiStatus.Internal
    public static void opener(final Consumer<InteractionHand> opens) {
        opener = opens;
    }

    /* Opens the list for the cylinder in {@code hand}, on a player's game. */
    static void open(final InteractionHand hand) {
        opener.accept(hand);
    }

    /* Runs on the server: marks the cylinder in the hand, while it is still an empty cylinder and the gas a gas. */
    private static void onMark(final MarkPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            final ItemStack held = context.player().getItemInHand(payload.mainHand() ? InteractionHand.MAIN_HAND
                    : InteractionHand.OFF_HAND);
            final IMatterHandler tank = held.getCapability(MatterCapabilities.GAS_ITEM);
            final Optional<Material> gas = Materials.byId(payload.gas());
            if (held.getItem() instanceof GasCylinderItem && tank != null && tank.stack(0).isEmpty()
                    && gas.isPresent()) {
                ItemGasTank.mark(held, gas);
            }
        });
    }

    /**
     * A player's game to the server: mark the cylinder in that hand for that gas.
     *
     * @param mainHand whether the cylinder is in the main hand, else the off hand
     * @param gas      the gas's material id
     */
    public record MarkPayload(boolean mainHand, ResourceLocation gas) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<MarkPayload> TYPE =
                new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "mark_gas"));
        public static final StreamCodec<ByteBuf, MarkPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, MarkPayload::mainHand, ResourceLocation.STREAM_CODEC, MarkPayload::gas,
                MarkPayload::new);

        @Override
        public CustomPacketPayload.Type<MarkPayload> type() {
            return TYPE;
        }
    }
}
