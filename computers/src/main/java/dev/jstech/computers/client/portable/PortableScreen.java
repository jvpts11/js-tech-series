/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.portable;

import dev.jstech.computers.operation.payload.portable.PortableViewPayload;
import dev.jstech.computers.portable.PortableBoot;
import dev.jstech.computers.portable.PortableItem;
import dev.jstech.computers.portable.PortableModel;
import dev.jstech.computers.portable.Portables;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * A portable held up (computers/docs/design/wireless.md): raised to the middle of the view over the blurred world,
 * its own face framing its screen, at the size the K0.10 pages gave its screen, smaller only where the window has no
 * room for it. Its screen is pressed with the mouse, its buttons round the screen too; a slide or a swipe lets its
 * lock go, a pull from the top brings its alerts, the right button goes back. The game goes on behind it, and it goes
 * down when the device leaves the hand.
 */
public final class PortableScreen extends Screen {

    private final InteractionHand hand;
    private final PortableModel model;
    private final PortableFace face;
    private final IMobileLook look;
    private int left;
    private int top;
    private float scale = 1.0F;
    private List<MobileCanvas.Press> presses = List.of();
    /* Where the button went down, in the screen's own pixels; NaN while it is up. */
    private double pressX = Double.NaN;
    private double pressY = Double.NaN;

    /* The room kept round the face, in the GUI's pixels. */
    private static final int MARGIN = 8;
    /* A press that moves less than this is a tap, more a slide, a swipe or a pull. */
    private static final double TAP_SLOP = 4.0;
    /* How far a slide or a swipe goes, of the screen's width or height, before the lock lets go. */
    private static final float LETS_GO = 0.85F;
    private static final float SLIDE_SPAN = 0.7F;
    private static final float SWIPE_SPAN = 0.35F;
    private static final float PULL = 0.2F;
    private static final int EDGE = 16;

    public PortableScreen(final InteractionHand hand, final PortableModel model) {
        super(Component.empty());
        this.hand = hand;
        this.model = model;
        this.face = PortableFace.of(model);
        this.look = IMobileLook.of(model.system());
        final UUID id = Portables.id(held());
        if (id != null) {
            final PortableViewPayload view = PortableViews.of(id);
            PortableViews.session(id).raised(view == null ? List.of() : view.alerts());
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Where the point {@code x} pixels across the device's screen is, across the window's GUI. */
    public double guiX(final double x) {
        return left + (face.screenX() + x) * scale;
    }

    /** Where the point {@code y} pixels down the device's screen is, down the window's GUI. */
    public double guiY(final double y) {
        return top + (face.screenY() + y) * scale;
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        final ItemStack stack = held();
        if (stack.isEmpty()) {
            return;
        }
        final MobileSession session = session(stack);
        final UUID id = Portables.id(stack);
        final PortableViewPayload view = id == null ? null : PortableViews.of(id);
        if (session != null && view != null) {
            session.alertsCame(view.alerts(), look instanceof FramesPocketLook);
        }
        g.pose().pushPose();
        g.pose().translate(left, top, 0.0F);
        g.pose().scale(scale, scale, 1.0F);
        g.pose().pushPose();
        g.pose().translate(face.screenX(), face.screenY(), 0.0F);
        presses = MobileDisplay.paint(g, stack, model, session, screenX(mouseX), screenY(mouseY));
        g.pose().popPose();
        g.blit(MobileArt.face(model), 0, 0, face.width(), face.height(), 0.0F, 0.0F, face.width() * 2,
                face.height() * 2, face.width() * 2, face.height() * 2);
        g.pose().popPose();
    }

    @Override
    public void tick() {
        if (held().isEmpty()) {
            onClose();
        }
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        final ItemStack stack = held();
        final MobileSession session = session(stack);
        if (session == null || !ready(stack)) {
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            session.tap(MobileTap.BACK, PacketDistributor::sendToServer);
            return true;
        }
        final MobileTap pressed = face.buttonAt((mouseX - left) / scale, (mouseY - top) / scale);
        if (pressed != null) {
            session.tap(pressed, PacketDistributor::sendToServer);
            return true;
        }
        pressX = screenX(mouseX);
        pressY = screenY(mouseY);
        return true;
    }

    @Override
    public boolean mouseDragged(final double mouseX, final double mouseY, final int button, final double dragX,
                                final double dragY) {
        final MobileSession session = session(held());
        if (session == null || Double.isNaN(pressX) || session.page() != MobilePage.LOCK) {
            return true;
        }
        switch (look.unlock()) {
            case SLIDE -> session.slideTo((float) ((screenX(mouseX) - pressX) / (model.screenWidth() * SLIDE_SPAN)));
            case SWIPE_UP -> session.slideTo((float) ((pressY - screenY(mouseY))
                    / (model.screenHeight() * SWIPE_SPAN)));
            case TAP -> {
            }
        }
        return true;
    }

    @Override
    public boolean mouseReleased(final double mouseX, final double mouseY, final int button) {
        final MobileSession session = session(held());
        if (session == null || Double.isNaN(pressX)) {
            return true;
        }
        final double dx = screenX(mouseX) - pressX;
        final double dy = screenY(mouseY) - pressY;
        if (Math.abs(dx) < TAP_SLOP && Math.abs(dy) < TAP_SLOP) {
            for (final MobileCanvas.Press press : presses) {
                if (press.has(pressX, pressY)) {
                    session.tap(press.tap(), PacketDistributor::sendToServer);
                    break;
                }
            }
        } else {
            gesture(session, dy);
        }
        session.slideTo(0.0F);
        pressX = Double.NaN;
        pressY = Double.NaN;
        return true;
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX,
                                 final double scrollY) {
        final MobileSession session = session(held());
        if (session != null && scrollY != 0) {
            session.scrollBy(scrollY > 0 ? -1 : 1);
        }
        return true;
    }

    @Override
    public boolean keyPressed(final int keyCode, final int scanCode, final int modifiers) {
        final MobileSession session = session(held());
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && session != null) {
            session.tap(MobileTap.BACK, PacketDistributor::sendToServer);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /* The face fitted to the window, at its own size where there is room and smaller where there is not. */
    @Override
    protected void init() {
        scale = Math.min(1.0F, Math.min((float) (width - 2 * MARGIN) / face.width(),
                (float) (height - 2 * MARGIN) / face.height()));
        left = Math.round((width - face.width() * scale) / 2.0F);
        top = Math.round((height - face.height() * scale) / 2.0F);
    }

    /* What a slide, a swipe or a pull ending {@code dy} below where it started does on the page. */
    private void gesture(final MobileSession session, final double dy) {
        final double tall = model.screenHeight();
        if (session.page() == MobilePage.LOCK) {
            if (session.slide() >= LETS_GO) {
                session.tap(MobileTap.UNLOCK, PacketDistributor::sendToServer);
            }
        } else if (look.pullsAlerts() && pressY < EDGE && dy > tall * PULL) {
            session.tap(MobileTap.ALERTS, PacketDistributor::sendToServer);
        } else if (session.page() == MobilePage.ALERTS && dy < -tall * PULL) {
            session.tap(MobileTap.BACK, PacketDistributor::sendToServer);
        } else if (pressY > tall - EDGE && dy < -tall * PULL) {
            session.tap(MobileTap.HOME, PacketDistributor::sendToServer);
        }
    }

    /* The device's system, when it has an id and is in the hand. */
    @Nullable
    private static MobileSession session(final ItemStack stack) {
        final UUID id = Portables.id(stack);
        return stack.isEmpty() || id == null ? null : PortableViews.session(id);
    }

    /* Whether the device has finished its start and shows its system. */
    private static boolean ready(final ItemStack stack) {
        final Minecraft game = Minecraft.getInstance();
        final long now = game.level == null ? 0L : game.level.getGameTime();
        return PortableBoot.stage(Portables.onSince(stack), now) == PortableBoot.Stage.READY;
    }

    /* The pointer across the screen, in the screen's own pixels. */
    private double screenX(final double mouseX) {
        return (mouseX - left) / scale - face.screenX();
    }

    private double screenY(final double mouseY) {
        return (mouseY - top) / scale - face.screenY();
    }

    /* The device of this model still in the hand it was held up from; empty once it is gone from there. */
    private ItemStack held() {
        final Minecraft game = Minecraft.getInstance();
        final ItemStack stack = game.player == null ? ItemStack.EMPTY : game.player.getItemInHand(hand);
        return stack.getItem() instanceof PortableItem item && item.model() == model ? stack : ItemStack.EMPTY;
    }
}
