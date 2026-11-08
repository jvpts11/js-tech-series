/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jstech.computers.ComputingLooks;
import dev.jstech.computers.block.IComputerCase;
import dev.jstech.computers.blockentity.AbstractSmallComputerBlockEntity;
import dev.jstech.computers.blockentity.CaseFront;
import dev.jstech.computers.blockentity.ComputerHardwareLayout;
import dev.jstech.computers.hardware.CaseCooling;
import dev.jstech.computers.hardware.CoolerSpec;
import dev.jstech.computers.os.media.DiscTray;
import dev.jstech.computers.registry.HardwareRegistry;
import dev.jstech.core.client.model.kit.IKitDriver;
import dev.jstech.core.client.model.kit.IKitExtras;
import dev.jstech.core.client.model.kit.KitBlockEntityRenderer;
import dev.jstech.core.client.model.kit.KitInstance;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.model.ModelAnimator;
import dev.jstech.core.model.ModelBone;
import dev.jstech.core.model.ModelData;
import dev.jstech.core.model.ModelPose;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Draws a small computer as its case: the tower of its age, or the one of three cases a later machine comes in, its
 * front toward whoever placed it and its left side on or off as the player left it. The power lamp is lit while the
 * machine runs and the disk lamp blinks while it works its disk; each bay shows the part in it, or its cover, and a
 * drive's lamp blinks while the machine reads its disc, the hot-swap bay's while it works its disk; the fans of the
 * parts in it turn while the machine runs.
 *
 * <p>Inside it, each part the player installed is drawn by a model of its own ({@link PartModels}): the board on the
 * tray, and on the board's seats the processor, the memory, the cards and an M.2 drive; on the case's seats the supply,
 * the disks and the case fans; and over each processor the cooler fitted on it, unless the processor is a cartridge
 * that brings its own (computers/docs/design/temperature.md). A fan sits the way the case's seat for it names: against
 * the back, under the roof or behind the front; a water cooler's radiator under the roof or behind the front, as the
 * pair of seats it takes lies. A case with a light lights the fans and coolers in it. The parts are drawn only when
 * they can be seen: with the side off, or through the glass or the mesh of a case that has some.
 */
public final class ComputerRenderer implements IKitDriver<AbstractSmallComputerBlockEntity>,
        IKitExtras<AbstractSmallComputerBlockEntity> {

    /** A case whose glass or mesh shows its inside carries this bone. */
    private static final String SEE_THROUGH = "see_through";
    private static final String SEAT_CPU = "seat_cpu";
    private static final String LEAD = "lead_";
    private static final String DISK = "disk_";
    private static final String M2 = "m2";
    private static final String FAN = "fan";
    /** How many numbered seats of one kind a name is kept for; a seat past them is named when it is drawn. */
    private static final int NAMED = 16;
    /** What a bay's model can show, as its bones name it: drive_<bay>_<part>. */
    private static final String[] BAY_PARTS = {"cd", "dvd", "bd", "hotswap", "hotswap_empty"};
    private static final String[][] DRIVE_BONES = bones("drive_", BAY_PARTS);
    /** The parts with an activity lamp, a bone of its own under the part's: lamp_<bay>_<part>. */
    private static final String HOT_SWAP = "hotswap";
    private static final String[] LAMP_PARTS = {"cd", "dvd", "bd", HOT_SWAP};
    private static final String[][] LAMP_BONES = bones("lamp_", LAMP_PARTS);
    /** The controllers each bay's tray and caddy play on. */
    private static final String[] TRAYS = DeviceLamps.numbered("tray_", CaseFront.BAYS);
    private static final String[] CADDIES = DeviceLamps.numbered("caddy_", CaseFront.BAYS);
    /* A two-socket board's later processors' seats: seat_cpu_1 on, the first being seat_cpu. */
    private static final String[] CPU_SEATS = DeviceLamps.numbered("seat_cpu_", NAMED);
    private static final String[] RAM_SEATS = DeviceLamps.numbered("seat_ram_", NAMED);
    private static final String[] CARD_SEATS = DeviceLamps.numbered("seat_card_", NAMED);
    /** One of the ways a part sits, as a case's fan seat names it. */
    private static final String WAY = "way_";
    private static final String WAY_TOP = "way_top";
    private static final String WAY_FRONT = "way_front";
    /** A case's fan seat, numbered as its cooling numbers them and named for the way a fan sits on it. */
    private static final String SEAT_FAN = "seat_fan_";
    /** The mark of the light a case gives the fans and coolers in it: lit_<light>. */
    private static final String LIT = "lit_";

    /* The seats of each form of disk, on the board or in the case: seat_<form>_<n>. */
    private static final Map<String, String[]> DISK_SEATS = new HashMap<>();
    private static final Map<Era, String> LEADS = new HashMap<>();
    private static final ModelFacts<CaseSeats> CASES = new ModelFacts<>(CaseSeats::of);

    private ComputerRenderer() {
    }

    /** The renderer of one of the small computers. */
    public static <T extends AbstractSmallComputerBlockEntity> BlockEntityRenderer<T> create(
            final BlockEntityRendererProvider.Context context) {
        final ComputerRenderer drawn = new ComputerRenderer();
        return KitBlockEntityRenderer.<T>look(ComputingLooks.COMPUTER).driver(drawn).extras(drawn).build();
    }

    /* Each bay's tray and caddy ride with what goes in and out; the fans are the parts', turned as they are drawn. */
    @Override
    public void drive(final AbstractSmallComputerBlockEntity computer, final ModelAnimator animator,
                      final double seconds) {
        final CaseFront front = computer.front();
        final Level level = computer.getLevel();
        for (int bay = 0; bay < CaseFront.BAYS; bay++) {
            DeviceClips.play(animator, TRAYS[bay], front.tray(bay).playing(front.trayClips(bay), level), seconds);
            DeviceClips.play(animator, CADDIES[bay], caddy(front, bay, level), seconds);
        }
    }

    @Override
    public void move(final AbstractSmallComputerBlockEntity computer, final ModelPose pose, final double seconds) {
        final boolean running = computer.visualRunning();
        final boolean blink = DeviceLamps.blinkLit(computer.getLevel());
        DeviceLamps.show(pose, ComputingLooks.COMPUTER_POWER_LAMP, running);
        DeviceLamps.show(pose, ComputingLooks.COMPUTER_DISK_LAMP, running && computer.visualDiskBusy() && blink);
        DeviceLamps.show(pose, ComputingLooks.COMPUTER_SIDE_PANEL, !computer.sidePanelOff());
        // A bay's model carries every part it takes, each hidden but the one installed; an empty bay shows its cover.
        final CaseFront front = computer.front();
        for (int bay = 0; bay < CaseFront.BAYS; bay++) {
            final String shown = front.shown(bay);
            for (int part = 0; part < BAY_PARTS.length; part++) {
                DeviceLamps.show(pose, DRIVE_BONES[bay][part], BAY_PARTS[part].equals(shown));
            }
            // Its lamp: a drive's blinks while the machine reads its disc, the hot-swap bay's while it works the disk.
            final boolean working = HOT_SWAP.equals(shown) ? front.diskWorked(bay) : front.reading(bay);
            for (int part = 0; part < LAMP_PARTS.length; part++) {
                DeviceLamps.show(pose, LAMP_BONES[bay][part], LAMP_PARTS[part].equals(shown) && working && blink);
            }
        }
    }

    @Override
    public boolean movesBones() {
        return true;
    }

    @Override
    public void draw(final AbstractSmallComputerBlockEntity computer, final KitInstance instance,
                     final PoseStack poses, final MultiBufferSource buffers, final int light, final int overlay,
                     final float partialTick) {
        CaseFrontMedia.draw(computer, instance, poses, buffers, light, overlay);
        if (!(computer.getBlockState().getBlock() instanceof IComputerCase chassis)
                || !computer.sidePanelOff() && instance.pose().index(SEE_THROUGH) < 0) {
            return;
        }
        new Drawing(computer, chassis, instance, new PartModels.Drawer(computer, computer.visualRunning(), poses,
                buffers, light, overlay, partialTick)).parts();
    }

    /* The bones of each part in each bay, named once rather than every frame: <prefix><bay>_<part>. */
    private static String[][] bones(final String prefix, final String[] parts) {
        final String[][] out = new String[CaseFront.BAYS][parts.length];
        for (int bay = 0; bay < CaseFront.BAYS; bay++) {
            for (int part = 0; part < parts.length; part++) {
                out[bay][part] = prefix + bay + "_" + parts[part];
            }
        }
        return out;
    }

    /* The hot-swap bay's caddy: riding in as its disk goes in, riding out as the disk comes out, else at rest. */
    private static @Nullable String caddy(final CaseFront front, final int bay, @Nullable final Level level) {
        final DiscTray.Clips clips = front.caddyClips(bay);
        if (clips == null || !front.caddyMoving(bay, level)) {
            return null;
        }
        return front.caddyWentIn(bay) ? clips.closing() : clips.opening();
    }

    /**
     * What a case's model offers the parts the player fits, worked out once from its bones: the bone of each fan seat
     * by its number (none for a number it has no seat for) and the way a fan sits on it, and the light the case gives
     * its fans and coolers, or none.
     */
    private record CaseSeats(String[] fanSeats, String[] fanWays, @Nullable String light) {

        static CaseSeats of(final ModelData model) {
            final Map<Integer, String> seats = new HashMap<>();
            String light = null;
            int most = 0;
            for (final ModelBone bone : model.bones()) {
                final String name = bone.name();
                // seat_fan_<n>_<way>: its number, then the way.
                final int cut = name.startsWith(SEAT_FAN) ? name.indexOf('_', SEAT_FAN.length()) : -1;
                if (cut > SEAT_FAN.length()) {
                    final int seat = Integer.parseInt(name.substring(SEAT_FAN.length(), cut));
                    seats.put(seat, name);
                    most = Math.max(most, seat + 1);
                } else if (name.startsWith(LIT)) {
                    light = name.substring(LIT.length());
                }
            }
            final String[] fanSeats = new String[most];
            final String[] fanWays = new String[most];
            for (final Map.Entry<Integer, String> seat : seats.entrySet()) {
                final String name = seat.getValue();
                fanSeats[seat.getKey()] = name;
                fanWays[seat.getKey()] = WAY + name.substring(name.indexOf('_', SEAT_FAN.length()) + 1);
            }
            return new CaseSeats(fanSeats, fanWays, light);
        }
    }

    /** One computer's parts drawn in one frame. */
    private static final class Drawing {

        private final AbstractSmallComputerBlockEntity computer;
        private final IComputerCase chassis;
        private final KitInstance caseInstance;
        private final PartModels.Drawer drawer;
        /** The case's fan seats and its light. */
        private final CaseSeats caseSeats;

        Drawing(final AbstractSmallComputerBlockEntity computer, final IComputerCase chassis,
                final KitInstance caseInstance, final PartModels.Drawer drawer) {
            this.computer = computer;
            this.chassis = chassis;
            this.caseInstance = caseInstance;
            this.drawer = drawer;
            this.caseSeats = CASES.of(caseInstance.model().data());
        }

        void parts() {
            final ComputerHardwareLayout layout = computer.hardwareLayout();
            final List<CoolerSpec> coolers = coolers(layout);
            final int boardSlot = layout.motherboardSlot();
            final PartModels.Part board = PartModels.part(computer.installedPart(boardSlot));
            KitInstance boardInstance = null;
            if (board != null) {
                boardInstance = drawer.draw(PartModels.Drawer.key(boardSlot), board, null, null, false, null, null);
                // Each processor on its socket, a two-socket board's second on the second, and its cooler on it.
                for (int k = 0; k < layout.cpuCount(); k++) {
                    processor(layout, k, boardInstance, coolers);
                }
                for (int k = 0; k < layout.ramCount(); k++) {
                    drawer.slot(layout.ramSlot(k), computer.installedPart(layout.ramSlot(k)), boardInstance,
                            PartModels.nth(RAM_SEATS, "seat_ram_", k));
                }
                for (int k = 0; k < layout.pcieCount(); k++) {
                    final int at = layout.pcieStart() + k;
                    drawer.slot(at, computer.installedPart(at), boardInstance,
                            PartModels.nth(CARD_SEATS, "seat_card_", k));
                }
            }
            drawer.supply(layout.psuSlot(), computer.installedPart(layout.psuSlot()), caseInstance,
                    LEADS.computeIfAbsent(chassis.chassisEra(), era -> LEAD + HardwareEra.withParts(era).path()));
            disks(layout, boardInstance);
            fans(layout, coolers);
        }

        /*
         * Processor {@code k} on its socket, and on it the cooler fitted over it: a water cooler's radiator the way the
         * pair of seats it takes lies.
         */
        private void processor(final ComputerHardwareLayout layout, final int k, final KitInstance board,
                               final List<CoolerSpec> coolers) {
            final String seat = k == 0 ? SEAT_CPU : PartModels.nth(CPU_SEATS, "seat_cpu_", k);
            final boolean cooled = k < layout.coolerCount();
            final CaseCooling.Side side = cooled ? computer.computer().cooling().radiatorSide(coolers, k) : null;
            drawer.processor(layout.cpuSlot(k), computer.installedPart(layout.cpuSlot(k)), layout.coolerStart() + k,
                    cooled ? computer.installedPart(layout.coolerStart() + k) : null, board, seat,
                    side == CaseCooling.Side.TOP ? WAY_TOP : side == CaseCooling.Side.FRONT ? WAY_FRONT : null,
                    caseSeats.light());
        }

        /* Each disk on its seat: an M.2 drive on the board's, the others on the case's of their form, in order. */
        private void disks(final ComputerHardwareLayout layout, @Nullable final KitInstance board) {
            int onBoard = 0;
            int inBays = 0;
            for (int k = 0; k < layout.diskCount(); k++) {
                final int at = layout.diskStart() + k;
                final PartModels.Part disk = PartModels.part(computer.installedPart(at));
                if (disk == null || disk.kind() == null || !disk.kind().startsWith(DISK)) {
                    continue;
                }
                final String form = disk.kind().substring(DISK.length());
                if (M2.equals(form)) {
                    if (board != null) {
                        drawer.draw(PartModels.Drawer.key(at), disk, board, diskSeat(form, onBoard), false, null,
                                null);
                    }
                    onBoard++;
                } else {
                    drawer.draw(PartModels.Drawer.key(at), disk, caseInstance, diskSeat(form, inBays++), false, null,
                            null);
                }
            }
        }

        /* The case fans the player fitted, each on its seat the way the seat names; none where a radiator sits. */
        private void fans(final ComputerHardwareLayout layout, final List<CoolerSpec> coolers) {
            final String[] seats = caseSeats.fanSeats();
            final Set<Integer> radiators = computer.computer().cooling().radiatorSeats(coolers);
            for (int n = 0; n < Math.min(seats.length, layout.fanCount()); n++) {
                final int at = layout.fanStart() + n;
                final PartModels.Part fan = seats[n] == null || radiators.contains(n) ? null
                        : PartModels.lit(computer.installedPart(at), caseSeats.light());
                if (fan != null && FAN.equals(fan.kind())) {
                    drawer.draw(PartModels.Drawer.key(at), fan, caseInstance, seats[n], false, null,
                            caseSeats.fanWays()[n]);
                }
            }
        }

        /* The coolers over the processors, as the players who see the machine know its parts; null for none. */
        private List<CoolerSpec> coolers(final ComputerHardwareLayout layout) {
            final List<CoolerSpec> out = new ArrayList<>(layout.coolerCount());
            for (int k = 0; k < layout.coolerCount(); k++) {
                final ResourceLocation item = computer.installedPart(layout.coolerStart() + k);
                final HardwareRegistry.Part part = item == null ? null
                        : HardwareRegistry.part(BuiltInRegistries.ITEM.get(item));
                out.add(part != null && part.spec() instanceof CoolerSpec cooler ? cooler : null);
            }
            return out;
        }

        private String diskSeat(final String form, final int k) {
            return PartModels.nth(DISK_SEATS.computeIfAbsent(form,
                    named -> DeviceLamps.numbered("seat_" + named + "_", NAMED)), "seat_" + form + "_", k);
        }
    }
}
