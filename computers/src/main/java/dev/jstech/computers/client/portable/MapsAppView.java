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
import dev.jstech.computers.operation.payload.portable.PortableMapPayload;
import dev.jstech.computers.portable.PortableLink;
import dev.jstech.core.client.gui.component.LandPicture;
import dev.jstech.core.client.gui.component.MapView;
import dev.jstech.core.client.map.ClientMarkers;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Maps (computers/docs/design/wireless.md), as the K0.11 apps page drew it on the qPhone X: J's Core's map of the land
 * round the player across the whole screen, north up and ruled every sixteen pixels, the reach of the network's access
 * points ringed, the access points, the Mainframe, the player and the Core's own markers, a scale, and a card saying
 * which access point reaches the device and how fast. The wheel brings the map nearer or takes it further.
 */
@PaletteHolder
final class MapsAppView implements IMobileAppView {

    private static final int ASK_EVERY = 100;
    private static final int GRID = 16;
    private static final float SCALE_PIXELS = 22.0F;
    private static final float LABEL_H = 7.0F;
    /* The land as the map draws it, one picture for every map open, made again as the player moves. */
    private static final LandPicture LAND = new LandPicture();
    /** Its colours, as the K0.11 apps page drew them: the grid, the reach, the markers, the card. */
    private static final Palette<Colours> COLOURS = Palettes.declare(JsComputers.MODID, "mobile/maps",
            new Colours(0x28FFFFFF, 0x8CFFFFFF, 0xFFFFFFFF, 0xFFFFD84A, 0xFF4AE0FF, 0xFF000000, 0xF0FAFAFC,
                    0xFF30A050, 0xFF8E8E93, 0xFF203018));

    @Override
    public int askEvery() {
        return ASK_EVERY;
    }

    @Override
    @Nullable
    public PortableActionPayload ask(final UUID device, final MobileAppState state) {
        return PortableActionPayload.of(device, Action.MAP);
    }

    @Override
    public void paint(final AppPage page) {
        final Colours k = COLOURS.get();
        final MobileCanvas c = page.c();
        final Minecraft game = Minecraft.getInstance();
        if (game.player == null) {
            return;
        }
        final int zoom = page.state().zoom();
        final int w = c.width();
        final int h = c.height();
        final BlockPos here = game.player.blockPosition();
        final ResourceLocation land = LAND.around(here, zoom, w, h, k.unloaded());
        if (land != null) {
            c.picture(land, w, h, 0, 0, w, h);
        }
        for (int x = GRID; x < w; x += GRID) {
            c.rect(x, 0, 0.5F, h, k.grid());
        }
        for (int y = GRID; y < h; y += GRID) {
            c.rect(0, y, w, 0.5F, k.grid());
        }
        final PortableMapPayload view = PortableApps.map(page.device());
        if (view != null) {
            for (final PortableMapPayload.Point point : view.points()) {
                c.ring(xOf(c, point.pos(), here, zoom), yOf(c, point.pos(), here, zoom),
                        (float) point.reach() / zoom, 1, k.reach());
            }
        }
        // The player first, then the network's, then the Core's: a name that would cover one before it is left off.
        final List<float[]> named = new ArrayList<>();
        mark(c, here, here, zoom, k.you(), MobileTexts.say(AppTexts.YOU), named);
        if (view != null) {
            view.mainframe().ifPresent(pos -> mark(c, pos, here, zoom, k.mainframe(),
                    MobileTexts.say(AppTexts.MAINFRAME), named));
            for (final PortableMapPayload.Point point : view.points()) {
                mark(c, point.pos(), here, zoom, k.point(), point.name(), named);
            }
        }
        for (final MapView.Marker marker : ClientMarkers.forMapView()) {
            mark(c, marker.pos(), here, zoom, marker.colour(), marker.name(), named);
        }
        c.text(MobileTexts.say(AppTexts.NORTH), w / 2.0F, page.top() + 3, k.ink(), MobileCanvas.SMALL, Align.CENTRE,
                false);
        card(page, k);
    }

    /** Lets the map's picture go, as the player leaves the world. */
    static void clear() {
        LAND.dispose();
    }

    /* The scale and the card at the foot: where the player is, the access point reaching them, its speed. */
    private static void card(final AppPage page, final Colours k) {
        final MobileCanvas c = page.c();
        final float w = c.width();
        final float bottom = page.bottom();
        c.rect(8, bottom - 46, SCALE_PIXELS, 1, k.ink());
        c.text(MobileTexts.say(AppTexts.METRES.with(Math.round(SCALE_PIXELS * page.state().zoom()))), 33,
                bottom - 49, k.ink(), MobileCanvas.SMALL);
        c.rounded(5, bottom - 38, w - 10, 34, 8, k.card(), null);
        c.text(MobileTexts.say(AppTexts.YOU_ARE_HERE), 11, bottom - 33, k.ink(), MobileCanvas.SMALL, Align.LEFT, true);
        final PortableLink link = page.frame().link();
        final String reach = MobileTexts.say(link == null ? AppTexts.OUT_OF_REACH.text()
                : AppTexts.IN_REACH_OF.with(link.accessPoint()));
        c.text(c.fit(reach, w - 22, MobileCanvas.SMALL), 11, bottom - 23, link == null ? k.dim() : k.good(),
                MobileCanvas.SMALL);
        if (link != null) {
            c.text(MobileTexts.say(AppTexts.ITEMS_PER_TICK.with(link.speed().itemsPerTick())), 11, bottom - 14,
                    k.dim(), MobileCanvas.SMALL);
        }
    }

    /*
     * A marker: a square in its colour, ringed black, its name under it unless that would cover a name already in
     * {@code named}, the boxes of the names drawn so far.
     */
    private static void mark(final MobileCanvas c, final BlockPos pos, final BlockPos here, final int zoom,
                             final int colour, final String name, final List<float[]> named) {
        final float x = xOf(c, pos, here, zoom);
        final float y = yOf(c, pos, here, zoom);
        if (x < 3 || y < 3 || x > c.width() - 3 || y > c.height() - 3) {
            return;
        }
        final Colours k = COLOURS.get();
        c.rect(x - 3, y - 3, 6, 6, k.ink());
        c.rect(x - 2, y - 2, 4, 4, colour);
        final String label = c.fit(name, 60, MobileCanvas.SMALL);
        final float half = c.width(label, MobileCanvas.SMALL) / 2;
        final float[] box = {x - half, y + 4, x + half, y + 4 + LABEL_H};
        for (final float[] other : named) {
            if (box[0] < other[2] && other[0] < box[2] && box[1] < other[3] && other[1] < box[3]) {
                return;
            }
        }
        named.add(box);
        c.text(label, x, y + 4, k.ink(), MobileCanvas.SMALL, Align.CENTRE, false);
    }

    private static float xOf(final MobileCanvas c, final BlockPos pos, final BlockPos here, final int zoom) {
        return c.width() / 2.0F + (float) (pos.getX() - here.getX()) / zoom;
    }

    private static float yOf(final MobileCanvas c, final BlockPos pos, final BlockPos here, final int zoom) {
        return c.height() / 2.0F + (float) (pos.getZ() - here.getZ()) / zoom;
    }

    /** The maps' colours, as the palette above names them. */
    private record Colours(int grid, int reach, int point, int mainframe, int you, int ink, int card, int good,
                           int dim, int unloaded) {
    }
}
