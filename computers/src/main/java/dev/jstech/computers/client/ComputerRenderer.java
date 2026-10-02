/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.jstech.computers.ComputingLooks;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.block.IComputerCase;
import dev.jstech.computers.blockentity.AbstractSmallComputerBlockEntity;
import dev.jstech.computers.blockentity.ComputerHardwareLayout;
import dev.jstech.core.client.geo.LookGeoModel;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/**
 * Draws a small computer as its case: the tower of its age, or the one of three cases a later machine comes in, its
 * front toward whoever placed it and its left side on or off as the player left it. Its lamps stay dark: the case shows
 * the machine as it stands, switched off.
 *
 * <p>Inside it, each part the player installed is drawn by a model of its own, chosen by the item: the board on the
 * tray, and on the board's seats the processor, the memory, the cards and an M.2 drive; on the case's seats the supply
 * and the disks; and on the processor the cooler the case brings, unless the processor is a cartridge that brings its
 * own. A part's model has the point it plugs in by at its origin, so a part made for one age sits on the seat of
 * another age's board or case. The parts are drawn only when they can be seen: with the side off, or through the
 * glass or the mesh of a case that has some.
 *
 * @param <T> which of the small computers
 */
public final class ComputerRenderer<T extends AbstractSmallComputerBlockEntity> extends GeoBlockRenderer<T> {

    /** A case whose glass or mesh shows its inside carries this bone. */
    private static final String SEE_THROUGH = "see_through";
    private static final String SEAT = "seat_";
    private static final String CARTRIDGE = "cartridge";
    private static final String PSU = "psu_";
    private static final String PSU_TOP = "top";
    /** Under the Advanced cases' cover only that case's own supply is drawn: any other could not be seen. */
    private static final String PSU_COVER = "cover";
    private static final String[] PSU_FORMS = {PSU_TOP, "floor", PSU_COVER};
    private static final String LEAD = "lead_";
    private static final String DISK = "disk_";
    private static final String M2 = "m2";
    /** Half a supply's height, in the model's units: a supply turned over turns about its middle. */
    private static final float PSU_HALF_HEIGHT = 1.75f;
    private static final float UNITS = 16f;

    /**
     * Where each part's model and texture are, by the item that names it. The model itself is asked of GeckoLib's
     * cache on every frame, which a resource reload fills anew.
     */
    private static final Map<ResourceLocation, PartFiles> PART_FILES = new ConcurrentHashMap<>();

    public ComputerRenderer(final BlockEntityRendererProvider.Context context) {
        super(new LookGeoModel<>(ComputingLooks.COMPUTER));
    }

    @Override
    public void preRender(final PoseStack poseStack, final T computer, final BakedGeoModel model,
                          final MultiBufferSource bufferSource, final VertexConsumer buffer, final boolean isReRender,
                          final float partialTick, final int packedLight, final int packedOverlay, final int colour) {
        super.preRender(poseStack, computer, model, bufferSource, buffer, isReRender, partialTick, packedLight,
                packedOverlay, colour);
        DeviceLamps.show(model, ComputingLooks.COMPUTER_POWER_LAMP, false);
        DeviceLamps.show(model, ComputingLooks.COMPUTER_DISK_LAMP, false);
        DeviceLamps.show(model, ComputingLooks.COMPUTER_SIDE_PANEL, !computer.sidePanelOff());
    }

    @Override
    public void postRender(final PoseStack poseStack, final T computer, final BakedGeoModel model,
                           final MultiBufferSource bufferSource, final VertexConsumer buffer,
                           final boolean isReRender, final float partialTick, final int packedLight,
                           final int packedOverlay, final int colour) {
        super.postRender(poseStack, computer, model, bufferSource, buffer, isReRender, partialTick, packedLight,
                packedOverlay, colour);
        if (isReRender || !(computer.getBlockState().getBlock() instanceof IComputerCase chassis)
                || !computer.sidePanelOff() && model.getBone(SEE_THROUGH).isEmpty()) {
            return;
        }
        new Drawing(poseStack, computer, chassis, model, bufferSource, partialTick, packedLight, packedOverlay, colour)
                .parts();
    }

    /** The part an item is drawn as, or null for no item or an item with no model of its own. */
    private static @Nullable Part part(final @Nullable ResourceLocation item) {
        if (item == null) {
            return null;
        }
        final PartFiles files = PART_FILES.computeIfAbsent(item, PartFiles::of);
        final BakedGeoModel model = GeckoLibCache.getBakedModels().get(files.geo());
        return model == null || model.topLevelBones().size() != 1 ? null : new Part(model, files.texture());
    }

    private static Optional<GeoBone> seat(final @Nullable BakedGeoModel model, final String name) {
        return model == null ? Optional.empty() : model.getBone(SEAT + name);
    }

    /** Where a part's model and texture are. */
    private record PartFiles(ResourceLocation geo, ResourceLocation texture) {

        static PartFiles of(final ResourceLocation item) {
            final String namespace = item.getNamespace();
            return new PartFiles(
                    ResourceLocation.fromNamespaceAndPath(namespace, "geo/part/" + item.getPath() + ".geo.json"),
                    ResourceLocation.fromNamespaceAndPath(namespace, "textures/block/part/" + item.getPath() + ".png"));
        }
    }

    /**
     * A part's model, whose one root bone names the kind of part: {@code board}, {@code cpu} or {@code cartridge},
     * {@code ram}, {@code card}, {@code psu_<form>}, {@code disk_<form>}, {@code cooler}.
     */
    private record Part(BakedGeoModel model, ResourceLocation texture) {

        String kind() {
            return model.topLevelBones().getFirst().getName();
        }
    }

    /** One computer's parts drawn in one frame. */
    private final class Drawing {

        private final PoseStack poseStack;
        private final T computer;
        private final IComputerCase chassis;
        private final BakedGeoModel caseModel;
        private final MultiBufferSource bufferSource;
        private final float partialTick;
        private final int packedLight;
        private final int packedOverlay;
        private final int colour;

        Drawing(final PoseStack poseStack, final T computer, final IComputerCase chassis, final BakedGeoModel caseModel,
                final MultiBufferSource bufferSource, final float partialTick, final int packedLight,
                final int packedOverlay, final int colour) {
            this.poseStack = poseStack;
            this.computer = computer;
            this.chassis = chassis;
            this.caseModel = caseModel;
            this.bufferSource = bufferSource;
            this.partialTick = partialTick;
            this.packedLight = packedLight;
            this.packedOverlay = packedOverlay;
            this.colour = colour;
        }

        void parts() {
            final ComputerHardwareLayout layout = computer.hardwareLayout();
            final Part board = part(computer.installedPart(layout.motherboardSlot()));
            final BakedGeoModel boardModel = board == null ? null : board.model();
            if (board != null) {
                draw(board, Optional.empty(), false);
                final Part cpu = part(computer.installedPart(layout.cpuStart()));
                final Optional<GeoBone> socket = seat(boardModel, "cpu");
                if (cpu != null && socket.isPresent()) {
                    draw(cpu, socket, false);
                    if (!CARTRIDGE.equals(cpu.kind())) {
                        final Part cooler = part(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
                                "cooler_" + chassis.caseStyle().caseName(chassis.chassisEra())));
                        if (cooler != null) {
                            draw(cooler, Optional.empty(), false);
                        }
                    }
                }
                for (int k = 0; k < layout.ramCount(); k++) {
                    drawOn(part(computer.installedPart(layout.ramStart() + k)), seat(boardModel, "ram_" + k));
                }
                for (int k = 0; k < layout.pcieCount(); k++) {
                    drawOn(part(computer.installedPart(layout.pcieStart() + k)), seat(boardModel, "card_" + k));
                }
            }
            supply(part(computer.installedPart(layout.psuSlot())));
            int onBoard = 0;
            int inBays = 0;
            for (int k = 0; k < layout.diskCount(); k++) {
                final Part disk = part(computer.installedPart(layout.diskStart() + k));
                if (disk == null || !disk.kind().startsWith(DISK)) {
                    continue;
                }
                final String form = disk.kind().substring(DISK.length());
                if (M2.equals(form)) {
                    drawOn(disk, seat(boardModel, form + "_" + onBoard++));
                } else {
                    drawOn(disk, seat(caseModel, form + "_" + inBays++));
                }
            }
        }

        /**
         * The supply, on the case's seat for it. One made for a case that holds its supply the other way up is
         * turned over; one from another age loses its leads, which run to a connector its board does not have there.
         */
        private void supply(final @Nullable Part psu) {
            if (psu == null || !psu.kind().startsWith(PSU)) {
                return;
            }
            final String form = psu.kind().substring(PSU.length());
            for (final String held : PSU_FORMS) {
                final Optional<GeoBone> seat = seat(caseModel, "psu_" + held);
                if (seat.isEmpty()) {
                    continue;
                }
                if (PSU_COVER.equals(held) && !PSU_COVER.equals(form)) {
                    return;
                }
                final String lead = LEAD + chassis.chassisEra().serializedName();
                for (final GeoBone child : psu.model().topLevelBones().getFirst().getChildBones()) {
                    if (child.getName().startsWith(LEAD)) {
                        child.setHidden(!child.getName().equals(lead));
                    }
                }
                draw(psu, seat, PSU_TOP.equals(held) != PSU_TOP.equals(form));
                return;
            }
        }

        private void drawOn(final @Nullable Part part, final Optional<GeoBone> seat) {
            if (part != null && seat.isPresent()) {
                draw(part, seat, false);
            }
        }

        private void draw(final Part part, final Optional<GeoBone> seat, final boolean turnedOver) {
            poseStack.pushPose();
            seat.ifPresent(at -> poseStack.translate(at.getPivotX() / UNITS, at.getPivotY() / UNITS,
                    at.getPivotZ() / UNITS));
            if (turnedOver) {
                poseStack.translate(0, PSU_HALF_HEIGHT / UNITS, 0);
                poseStack.scale(1, -1, 1);
                poseStack.translate(0, -PSU_HALF_HEIGHT / UNITS, 0);
            }
            final RenderType type = getGeoModel().getRenderType(computer, part.texture());
            final VertexConsumer buffer = bufferSource.getBuffer(type);
            for (final GeoBone bone : part.model().topLevelBones()) {
                renderRecursively(poseStack, computer, bone, type, bufferSource, buffer, true, partialTick,
                        packedLight, packedOverlay, colour);
            }
            poseStack.popPose();
        }
    }
}
