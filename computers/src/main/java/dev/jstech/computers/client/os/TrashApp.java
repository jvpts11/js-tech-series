/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.gui.TrashItem;
import dev.jstech.computers.gui.layout.TrashLayout;
import dev.jstech.computers.operation.payload.RequestTrashPayload;
import dev.jstech.computers.operation.payload.TrashActionPayload;
import dev.jstech.computers.operation.payload.TrashListingPayload;
import dev.jstech.computers.os.PanelStyle;
import dev.jstech.computers.os.fs.FileOpeners;
import dev.jstech.computers.os.fs.TrashKind;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * The trash of a desktop, opened in the look of that desktop's own file manager: Frames' Recycle Bin, the Trash of
 * the Linux desktops, or CDE's Trash Can.
 *
 * <p>The window keeps what the machine last said is in the trash and what the player has selected, and does what is
 * asked of it: putting things back, deleting them for good, or emptying the whole trash. Anything deleted for good is
 * asked about first, since it cannot be taken back. How it looks and which words it uses is its look's business.
 */
public final class TrashApp implements IDesktopApp {

    private final BlockPos host;
    private final PanelStyle style;
    private final ITrashLook look;

    /** What the machine last said is in the trash, in the order it went in, and in the order it is shown. */
    private List<TrashItem> arrived = List.of();
    private List<TrashItem> items = List.of();
    private final Set<String> selected = new LinkedHashSet<>();
    private int scroll;
    private Order order = Order.AS_DELETED;
    private OsSkin skin = OsSkin.fallback();

    /** The orders a window can show the trash in. */
    enum Order {
        /** The order things went in, which is how every trash lists them to begin with. */
        AS_DELETED,
        BY_NAME,
        /** The largest first. */
        BY_SIZE
    }

    /** Every trash window that is up, so what the machine says reaches the ones showing it. */
    private static final List<TrashApp> OPEN = new ArrayList<>();

    public TrashApp(final BlockPos host, final PanelStyle style) {
        this.host = host;
        this.style = style;
        this.look = switch (kindOf(style)) {
            case RECYCLER -> new TrashFramesLook(this);
            case FREEDESKTOP -> new TrashLinuxLook(this);
            case CDE -> new TrashCdeLook(this);
        };
        OPEN.add(this);
        ask();
    }

    /** The habit a desktop drawn in that style keeps its trash by. */
    public static TrashKind kindOf(final PanelStyle style) {
        return TrashKind.of(style.unixLike(), style == PanelStyle.CDE);
    }

    /** Hands what a machine says is in its trash to every window showing that machine's. */
    public static void accept(final TrashListingPayload payload) {
        for (final TrashApp app : List.copyOf(OPEN)) {
            if (app.host.equals(payload.hostPos())) {
                app.take(payload.entries());
            }
        }
    }

    /** Has every trash window ask again, which a change to the disk calls for. */
    public static void refreshAll() {
        for (final TrashApp app : List.copyOf(OPEN)) {
            app.ask();
        }
    }

    /** Says every trash window is gone, which is what a desktop closing means for the ones it held. */
    static void forgetAll() {
        OPEN.clear();
    }

    /** The names the window shows, in the order it shows them. */
    public List<String> shownNames() {
        final List<String> out = new ArrayList<>();
        for (final TrashItem item : this.items) {
            out.add(item.name());
        }
        return out;
    }

    /** Where the thing of that name is on screen, or null when it is not shown. */
    @Nullable
    public int[] itemPoint(final String name) {
        for (int i = 0; i < this.items.size(); i++) {
            if (this.items.get(i).name().equals(name)) {
                return this.look.itemCentre(i);
            }
        }
        return null;
    }

    /** Where the control that says that is, or null when this look has none of that name. */
    @Nullable
    public int[] controlPoint(final String label) {
        return this.look.controlPoint(label);
    }

    /** What the menu that is up offers, or nothing while none is. */
    public List<String> menuLabels() {
        return this.look.menuOpen() ? this.look.menuLabels() : List.of();
    }

    @Nullable
    public int[] menuPoint(final String label) {
        return this.look.menuPoint(label);
    }

    /** Dolphin names the folder it is on before itself; the other desktops title the window with the place alone. */
    @Override
    public String title() {
        final String place = kindOf(this.style).title();
        return this.style == PanelStyle.KDE ? place + " - Dolphin" : place;
    }

    @Override
    public int defaultWidth() {
        return TrashLayout.DEFAULT_W;
    }

    @Override
    public int defaultHeight() {
        return TrashLayout.DEFAULT_H;
    }

    @Override
    public int minWidth() {
        return TrashLayout.MIN_W;
    }

    @Override
    public int minHeight() {
        return TrashLayout.MIN_H;
    }

    @Override
    public void applySkin(final OsSkin osSkin) {
        this.skin = osSkin;
    }

    @Override
    public void onRestored() {
        if (!OPEN.contains(this)) {
            OPEN.add(this);
        }
        ask();
    }

    @Override
    public void onClosed() {
        OPEN.remove(this);
    }

    @Override
    public void renderContent(final GuiGraphics g, final Font font, final int x, final int y, final int w,
                              final int h, final int mouseX, final int mouseY, final float partialTick) {
        this.look.render(g, font, x, y, w, h, mouseX, mouseY);
    }

    @Override
    public void mouseClicked(final DesktopWindow window, final double mouseX, final double mouseY,
                             final int button) {
        this.look.click(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(final double delta) {
        this.scroll = Math.max(0, Math.min(this.look.scrollLimit(), this.scroll + (delta > 0 ? -1 : 1)));
        return true;
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        if (key == GLFW.GLFW_KEY_DELETE && !this.selected.isEmpty()) {
            deleteSelected();
            return true;
        }
        return false;
    }

    /* What the looks read and do. */

    List<TrashItem> items() {
        return this.items;
    }

    PanelStyle style() {
        return this.style;
    }

    OsSkin skin() {
        return this.skin;
    }

    int scroll() {
        return this.scroll;
    }

    boolean isSelected(final TrashItem item) {
        return this.selected.contains(item.stored());
    }

    List<TrashItem> selection() {
        final List<TrashItem> out = new ArrayList<>();
        for (final TrashItem item : this.items) {
            if (isSelected(item)) {
                out.add(item);
            }
        }
        return out;
    }

    /** Picks an item, alone or, with {@code adding}, beside what is picked already, where it toggles. */
    void select(final TrashItem item, final boolean adding) {
        if (!adding) {
            this.selected.clear();
            this.selected.add(item.stored());
        } else if (!this.selected.remove(item.stored())) {
            this.selected.add(item.stored());
        }
    }

    void selectAll() {
        for (final TrashItem item : this.items) {
            this.selected.add(item.stored());
        }
    }

    void clearSelection() {
        this.selected.clear();
    }

    /** Shows the items in that order from now on. */
    void orderBy(final Order wanted) {
        this.order = wanted;
        this.items = ordered(this.arrived);
    }

    Order order() {
        return this.order;
    }

    /** Puts everything selected back where it came from. */
    void restoreSelected() {
        final List<String> stored = storedOf(selection());
        if (!stored.isEmpty()) {
            act(TrashActionPayload.Action.RESTORE, stored);
        }
    }

    /** Deletes everything selected for good, once the player has said yes to it. */
    void deleteSelected() {
        final List<TrashItem> chosen = selection();
        if (chosen.isEmpty()) {
            return;
        }
        final Text question = chosen.size() == 1 ? TrashTexts.DELETE_ONE.with(chosen.getFirst().name())
                : TrashTexts.DELETE_MANY.with(chosen.size());
        DesktopScreen.ask(GameText.resolve(TrashTexts.CONFIRM), GameText.resolve(question),
                () -> act(TrashActionPayload.Action.SHRED, storedOf(chosen)));
    }

    /** Deletes everything in the trash for good, once the player has said yes to it. */
    void emptyAll() {
        if (this.items.isEmpty()) {
            return;
        }
        final Text trash = kindOf(this.style).titleText();
        final Text question = this.items.size() == 1 ? TrashTexts.EMPTY_ONE.with(trash)
                : TrashTexts.EMPTY_ALL.with(this.items.size(), trash);
        DesktopScreen.ask(GameText.resolve(TrashTexts.CONFIRM), GameText.resolve(question),
                () -> act(TrashActionPayload.Action.EMPTY, List.of()));
    }

    /** Says what an item is: its name, where it came from and the room it takes. */
    void showProperties(final TrashItem item) {
        DesktopScreen.tell(GameText.resolve(TrashTexts.PROPERTIES_OF.with(item.name())),
                GameText.resolve(TrashTexts.PROPERTIES_BODY.with(item.name(), item.place(), item.size())));
    }

    /** Opens a file manager at a place of the sidebar and puts this window away, as leaving the trash does. */
    void openPlace(final boolean desktopFolder) {
        final DesktopScreen desktop = DesktopScreen.current();
        if (desktop != null) {
            desktop.openFolder(desktopFolder ? desktop.desktopDirectory() : desktop.homeDir());
            DesktopScreen.closeWindowFor(this);
        }
    }

    /** Closes the window, as its File menu's Close does. */
    void close() {
        DesktopScreen.closeWindowFor(this);
    }

    /** The picture of the program that opens an item, which is how the desktops showed a file. */
    ResourceLocation iconOf(final TrashItem item) {
        final String program = item.directory() ? "files"
                : FileOpeners.defaultFor(item.name(), DesktopScreen.installedProgramIds());
        return ResourceLocation.fromNamespaceAndPath("jsc", program.isEmpty() ? "generic" : program);
    }

    /** The picture the trash itself wears on this desktop, empty or full. */
    ResourceLocation trashIcon() {
        return ResourceLocation.fromNamespaceAndPath("jsc", this.items.isEmpty() ? "trash" : "trash_full");
    }

    /** The icon set of the desktop the window is on. */
    String iconSet() {
        final DesktopScreen desktop = DesktopScreen.current();
        return desktop == null ? this.skin.iconSet() : desktop.icons();
    }

    /** Writes words of the trash in the player's language, with the shadow that suits what they are written on. */
    static void write(final GuiGraphics g, final Font font, final Text text, final int x, final int y,
                      final int color, final int ground, final float scale) {
        write(g, font, GameText.resolve(text), x, y, color, ground, scale);
    }

    /** Writes text with the shadow that suits what it is written on, at {@code scale} of the font's size. */
    static void write(final GuiGraphics g, final Font font, final String text, final int x, final int y,
                      final int color, final int ground, final float scale) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1f);
        Draw.text(g, font, text, 0, 0, color, ground);
        g.pose().popPose();
    }

    private void ask() {
        PacketDistributor.sendToServer(new RequestTrashPayload(this.host));
    }

    private void act(final TrashActionPayload.Action action, final List<String> stored) {
        PacketDistributor.sendToServer(new TrashActionPayload(this.host, action,
                stored.size() <= TrashActionPayload.MAX_STORED ? stored
                        : stored.subList(0, TrashActionPayload.MAX_STORED)));
        this.selected.clear();
        // What was put back shows up where it went, and the trash's picture may change.
        FilesApps.diskChanged();
    }

    private void take(final List<TrashListingPayload.WireEntry> entries) {
        final boolean frames = kindOf(this.style) == TrashKind.RECYCLER;
        final List<TrashItem> built = new ArrayList<>();
        for (final TrashListingPayload.WireEntry entry : entries) {
            built.add(TrashItem.of(entry.stored(), entry.original(), entry.directory(), entry.weight(), frames));
        }
        this.arrived = List.copyOf(built);
        this.items = ordered(this.arrived);
        // A selection outlives a listing only as far as what it names is still there.
        this.selected.removeIf(stored -> this.items.stream().noneMatch(item -> item.stored().equals(stored)));
        this.scroll = Math.min(this.scroll, this.look.scrollLimit());
    }

    private List<TrashItem> ordered(final List<TrashItem> in) {
        final List<TrashItem> out = new ArrayList<>(in);
        switch (this.order) {
            case BY_NAME -> out.sort(Comparator.comparing(item -> item.name().toLowerCase(Locale.ROOT)));
            case BY_SIZE -> out.sort(Comparator.comparingLong(TrashItem::weight).reversed());
            case AS_DELETED -> { }
        }
        return List.copyOf(out);
    }

    private static List<String> storedOf(final List<TrashItem> items) {
        final List<String> out = new ArrayList<>();
        for (final TrashItem item : items) {
            out.add(item.stored());
        }
        return out;
    }
}
