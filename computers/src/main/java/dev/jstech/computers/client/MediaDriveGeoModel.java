/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.os.media.MediaReaderBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * Picks the model of a drive: one per drive, since a floppy drive, a CD drive and a DVD drive are different
 * machines. The model and its atlas are named after the drive, and every drive shares one animation file.
 */
public final class MediaDriveGeoModel extends GeoModel<MediaReaderBlockEntity> {

    private static final ResourceLocation ANIMATIONS =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "animations/media_drive.animation.json");

    @Override
    public ResourceLocation getModelResource(final MediaReaderBlockEntity drive) {
        return ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
                "geo/" + drive.driveType().serializedName() + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(final MediaReaderBlockEntity drive) {
        return ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
                "textures/block/media_drive/" + drive.driveType().serializedName() + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(final MediaReaderBlockEntity drive) {
        return ANIMATIONS;
    }
}
