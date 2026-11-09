/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.portable;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.client.portable.MobileCanvas.Align;
import dev.jstech.computers.operation.payload.portable.PortableActionPayload;
import dev.jstech.computers.operation.payload.portable.PortableActionPayload.Action;
import dev.jstech.computers.operation.payload.portable.PortableGalleryPayload;
import dev.jstech.computers.portable.MobileApp;
import dev.jstech.computers.portable.PortableModel;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * The camera (computers/docs/design/wireless.md), as the K0.11 apps page drew it on the qPhone 5: the resolution of
 * the device's era over the viewfinder, which shows what the player sees with the thirds ruled over it, the shutter
 * under it, and the last picture taken, which opens the gallery.
 */
@PaletteHolder
final class CameraAppView implements IMobileAppView {

    private static final int ASK_EVERY = 100;
    private static final float FOOT = 40.0F;
    /** Its colours, as the K0.11 apps page drew them: the camera's yellow, the thirds, the shutter. */
    private static final Palette<Colours> COLOURS = Palettes.declare(JsComputers.MODID, "mobile/camera",
            new Colours(0xFFFFCC00, 0xFFFFFFFF, 0xFF000000, 0x3CFFFFFF, 0xFF3A3A3C));

    @Override
    public int askEvery() {
        return ASK_EVERY;
    }

    /* The last picture's small copy comes with the gallery. */
    @Override
    @Nullable
    public PortableActionPayload ask(final UUID device, final MobileAppState state) {
        return PortableActionPayload.of(device, Action.GALLERY);
    }

    @Override
    public void paint(final AppPage page) {
        final Colours k = COLOURS.get();
        final MobileCanvas c = page.c();
        final PortableModel model = page.frame().model();
        final float w = c.width();
        final float top = page.top();
        final float bottom = page.bottom();
        c.rect(0, top, w, 14, k.black());
        c.text(MobileTexts.say(AppTexts.RESOLUTION.with(model.cameraWidth(), model.cameraHeight())), w / 2, top + 4,
                k.yellow(), MobileCanvas.SMALL, Align.CENTRE, false);
        final float viewTop = top + 14;
        final float viewH = bottom - FOOT - viewTop;
        viewfinder(c, viewTop, viewH);
        for (int n = 1; n < 3; n++) {
            c.rect(w * n / 3.0F, viewTop, 0.5F, viewH, k.thirds());
            c.rect(0, viewTop + viewH * n / 3.0F, w, 0.5F, k.thirds());
        }
        final String note = MobileTexts.say(page.state().cameraNote());
        if (!note.isEmpty()) {
            c.text(c.fit(note, w - 8, MobileCanvas.SMALL), w / 2, viewTop + viewH - 10, k.white(),
                    MobileCanvas.SMALL, Align.CENTRE, false);
        }
        c.rect(0, bottom - FOOT, w, FOOT, k.black());
        c.text(MobileTexts.say(AppTexts.PHOTO), w / 2, bottom - FOOT + 3, k.yellow(), MobileCanvas.SMALL,
                Align.CENTRE, false);
        final float shutterY = bottom - 17;
        c.disc(w / 2, shutterY, 11, k.white());
        c.ring(w / 2, shutterY, 9, 1, k.black());
        c.press(w / 2 - 11, shutterY - 11, 22, 22, MobileTap.run(() ->
                CameraShots.take(page.device(), model, page.state())));
        last(page, bottom);
    }

    /* The world as the camera sees it, the middle of the frame cut to the viewfinder's shape. */
    private static void viewfinder(final MobileCanvas c, final float top, final float h) {
        CameraShots.viewing();
        final int texW = CameraShots.width();
        final int texH = CameraShots.height();
        if (texW <= 0 || texH <= 0 || h <= 0) {
            return;
        }
        final float w = c.width();
        // The part of the frame of the viewfinder's own shape, about its middle.
        final int cutH = (int) Math.min(texH, texW * h / w);
        final int cutW = (int) (cutH * w / h);
        final int u = (texW - cutW) / 2;
        final int v = (texH - cutH) / 2;
        // A frame is stored from its foot up: read from the top of the cut downwards.
        c.picture(CameraShots.VIEWFINDER, texW, texH, u, texH - v, cutW, -cutH, 0, top, w, h);
    }

    /* The last picture taken, at the foot's left, opening the gallery. */
    private static void last(final AppPage page, final float bottom) {
        final Colours k = COLOURS.get();
        final MobileCanvas c = page.c();
        c.rounded(8, bottom - 25, 16, 16, 3, k.frame(), null);
        final PortableGalleryPayload gallery = PortableApps.gallery(page.device());
        if (gallery != null && !gallery.pictures().isEmpty()) {
            final PortableGalleryPayload.Picture picture = gallery.pictures().getLast();
            final ResourceLocation texture = GalleryTextures.of(page.device(), picture.number());
            if (texture != null) {
                c.picture(texture, GalleryTextures.WIDTH, GalleryTextures.HEIGHT, GalleryTextures.SQUARE_U, 0,
                        GalleryTextures.HEIGHT, GalleryTextures.HEIGHT, 9, bottom - 24, 14, 14);
            }
        }
        if (page.frame().installed().contains(MobileApp.GALLERY)) {
            c.press(8, bottom - 25, 16, 16, MobileTap.open(MobileApp.GALLERY));
        }
    }

    /** The camera's colours, as the palette above names them. */
    private record Colours(int yellow, int white, int black, int thirds, int frame) {
    }
}
