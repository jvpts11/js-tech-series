/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import dev.jstech.core.connect.RelativeFace;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.inventory.FaceMode;
import dev.jstech.core.network.payload.ClientPayloads;
import dev.jstech.core.network.payload.IPayloadGate;
import dev.jstech.core.registry.CoreItems;
import dev.jstech.core.team.Ownership;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * A wrench on a machine, any wrench of any mod, every item of the common tag {@code c:tools/wrench}: a click turns
 * what the face clicked does with the resource the wrench shows, going round in, out, both and neither; sneaking, it
 * takes the machine away whole, its contents, upgrades, settings and the work under way going with it in its item.
 * Sneaking and turning the mouse wheel shows the next resource of the machine aimed at. Holding a wrench, the player
 * sees each face of the machine aimed at in the colour of what it does.
 */
@TextHolder
public final class MachineWrench {

    /** The resource a wrench shows and turns, kept on the wrench itself, so any mod's wrench carries it. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MachineResource>> RESOURCE =
            CoreItems.CONTENT.component("wrench_resource", StableCodecs.byName(MachineResource.class),
                    ByteBufCodecs.fromCodec(StableCodecs.byName(MachineResource.class)));
    /** Marks a machine's item that holds the machine taken away whole, for its tooltip to say so. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Unit>> TAKEN_WHOLE =
            CoreItems.CONTENT.component("taken_whole", Unit.CODEC, StreamCodec.unit(Unit.INSTANCE));
    public static final TextKey FACE_SET = TextKey.of("jscore.machine.wrench.face_set", "%s, %s face: %s");
    public static final TextKey SHOWS = TextKey.of("jscore.machine.wrench.shows", "The wrench shows %s");
    public static final TextKey TAKEN = TextKey.of("jscore.machine.wrench.taken",
            "Taken whole: its contents, upgrades and settings go with it");
    private static final String NETWORK_VERSION = "1";
    /* How far from the machine a player may be to change what the wrench shows of it. */
    private static final double REACH = 8.0;

    private MachineWrench() {
    }

    /** Declares the wrench's component, from the Core's constructor, while the Core's content is declared. */
    public static void declare() {
        // Loading the class declares it.
    }

    /** Hands the mod's bus and the game's what this answers, once, from the Core's constructor. */
    public static void listen(final IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(PlayerInteractEvent.RightClickBlock.class, MachineWrench::onRightClick);
        modBus.addListener(RegisterPayloadHandlersEvent.class, event -> ClientPayloads.accept(
                event.registrar(NETWORK_VERSION), WrenchResourcePayload.TYPE, WrenchResourcePayload.STREAM_CODEC,
                IPayloadGate.near(WrenchResourcePayload::pos, REACH),
                (payload, player, level) -> {
                    final ItemStack held = player.getMainHandItem();
                    if (isWrench(held) && level.getBlockEntity(payload.pos()) instanceof MachineBlockEntity machine) {
                        step(held, machine, payload.up());
                        player.displayClientMessage(GameText.component(SHOWS.with(shown(held, machine).text())),
                                true);
                    }
                }));
    }

    /** Whether {@code stack} is a wrench, of any mod. */
    public static boolean isWrench(final ItemStack stack) {
        return stack.is(Tags.Items.TOOLS_WRENCH);
    }

    /**
     * The resource {@code wrench} shows of {@code machine}: the one it was turned to, when the machine has it, and the
     * machine's first otherwise.
     */
    public static MachineResource shown(final ItemStack wrench, final MachineBlockEntity machine) {
        final List<MachineResource> resources = machine.sides().resources();
        final MachineResource kept = wrench.get(RESOURCE.get());
        if (kept != null && resources.contains(kept)) {
            return kept;
        }
        return resources.isEmpty() ? MachineResource.ITEMS : resources.getFirst();
    }

    /** Turns {@code wrench} to the next resource of {@code machine}, or the one before for {@code up} false. */
    public static void step(final ItemStack wrench, final MachineBlockEntity machine, final boolean up) {
        final List<MachineResource> resources = machine.sides().resources();
        if (resources.isEmpty()) {
            return;
        }
        final int at = resources.indexOf(shown(wrench, machine));
        final int next = Math.floorMod(at + (up ? 1 : -1), resources.size());
        wrench.set(RESOURCE.get(), resources.get(next));
    }

    /**
     * Turns what {@code worldFace} of {@code machine} does with the resource {@code wrench} shows, to the next mode;
     * what it does now, or nothing when {@code player} may not set the machine up.
     */
    public static FaceMode turn(final ServerPlayer player, final MachineBlockEntity machine, final Direction worldFace,
                                final ItemStack wrench) {
        final MachineResource resource = shown(wrench, machine);
        final RelativeFace face = RelativeFace.of(RelativeFace.facingOf(machine.getBlockState())
                .orElse(Direction.NORTH), worldFace);
        if (!machine.maySetUp(player)) {
            return machine.sides().mode(resource, face);
        }
        final List<FaceMode> round = FaceModes.round();
        final FaceMode next = round.get((round.indexOf(machine.sides().mode(resource, face)) + 1) % round.size());
        machine.sides().set(resource, face, next);
        return next;
    }

    /**
     * Takes {@code machine} away whole into its item, given to {@code player}: its contents, upgrades, fuse, battery,
     * settings and the work under way. False, leaving it, when the player may not break it.
     */
    public static boolean takeWhole(final ServerPlayer player, final MachineBlockEntity machine) {
        if (!machine.mayBreak(player) || machine.getLevel() == null) {
            return false;
        }
        final ItemStack item = new ItemStack(machine.getBlockState().getBlock());
        if (item.isEmpty()) {
            return false;
        }
        BlockItem.setBlockEntityData(item, machine.getType(), machine.saveWithoutMetadata(
                machine.getLevel().registryAccess()));
        item.set(TAKEN_WHOLE.get(), Unit.INSTANCE);
        final BlockPos pos = machine.getBlockPos();
        machine.fields().takenWhole();
        machine.getLevel().removeBlock(pos, false);
        player.getInventory().placeItemBackInInventory(item);
        return true;
    }

    /* A wrench on a machine does the wrench's work instead of the machine's, whoever made the wrench. */
    private static void onRightClick(final PlayerInteractEvent.RightClickBlock event) {
        final ItemStack held = event.getItemStack();
        if (event.getHand() != InteractionHand.MAIN_HAND || !isWrench(held)
                || !(event.getLevel().getBlockEntity(event.getPos()) instanceof MachineBlockEntity machine)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide()));
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.isShiftKeyDown()) {
            if (!takeWhole(player, machine)) {
                deny(player, machine);
            }
            return;
        }
        if (!machine.maySetUp(player)) {
            deny(player, machine);
            return;
        }
        final Direction face = event.getFace() == null ? Direction.UP : event.getFace();
        final FaceMode now = turn(player, machine, face, held);
        final RelativeFace relative = RelativeFace.of(RelativeFace.facingOf(machine.getBlockState())
                .orElse(Direction.NORTH), face);
        player.displayClientMessage(GameText.component(FACE_SET.with(shown(held, machine).text(),
                MachineFaceNames.of(relative).text(), MachineFaceNames.of(now).text())), true);
    }

    private static void deny(final ServerPlayer player, final MachineBlockEntity machine) {
        final Ownership owned = machine.ownership();
        if (owned != null) {
            player.displayClientMessage(GameText.component(owned.denial(player.server)), true);
        }
    }
}
