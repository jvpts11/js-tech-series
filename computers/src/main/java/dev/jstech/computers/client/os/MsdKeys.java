/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.os.edit.TtyLook;
import dev.jstech.computers.program.cli.msd.MsdScreen;
import dev.jstech.computers.program.cli.msd.MsdState;
import dev.jstech.computers.program.cli.msd.MsdView;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import java.util.List;
import org.lwjgl.glfw.GLFW;

/**
 * The Vintage systems' diagnostics on a terminal they have taken whole: the keyboard and mouse half of it.
 *
 * <p>Nothing of the machine is held here. A key changes where the screen stands, the machine is asked for that screen,
 * and what comes back is what is shown; a port enabled or disabled is enabled or disabled on the machine, and the
 * screen that comes back says so.
 *
 * <p>The keys are that program's: arrows move between the buttons, Enter opens one, a button's letter opens it
 * straight away, and F3 leaves.
 */
public final class MsdKeys implements TtyEditor.IKeys {

    /** Where the screen stands, which is the whole of what this side of it remembers. */
    private MsdState state = MsdState.OPENING;

    /** How many ports the dialog showed when it was last drawn, read off the screen itself. */
    private int ports;

    /** The button that opened the dialog, which is picked again when the dialog is put away. */
    private int opener = MsdScreen.LPT_BUTTON;

    /** A plain glass with one line that talks, which is all this screen draws round itself. */
    private static final TtyLook LOOK =
            new TtyLook("", Text.EMPTY, Text.EMPTY, TtyLook.Status.LINE, List.of(), List.of(), false);

    @Override
    public TtyLook look(final TtyEditor editor) {
        return LOOK;
    }

    @Override
    public String status(final TtyEditor editor) {
        return this.state.ports() ? GameText.resolve(MsdTuiTexts.PORTS.with("E", "D"))
                : GameText.resolve(MsdTuiTexts.MAIN.with("L", "C", "F3"));
    }

    @Override
    public void opened(final TtyEditor editor, final boolean existed) {
        if (!existed) {
            editor.say(MsdTuiTexts.NO_MACHINE.with(Text.literal("msd")));
            return;
        }
        caretOnThePicked(editor);
    }

    /** The glass is another size, so the screen is asked for again at the size it now has. */
    @Override
    public void resized(final TtyEditor editor, final int columns, final int rows) {
        show(editor, this.state.on(columns, rows));
    }

    @Override
    public boolean typed(final TtyEditor editor, final char c) {
        switch (Character.toLowerCase(c)) {
            case 'l' -> {
                if (!this.state.ports()) {
                    openPorts(editor, MsdScreen.LPT_BUTTON);
                }
            }
            case 'c' -> {
                if (!this.state.ports()) {
                    openPorts(editor, MsdScreen.COM_BUTTON);
                }
            }
            case 'e' -> {
                if (this.state.ports()) {
                    act(editor, MsdView.ENABLE);
                }
            }
            case 'd' -> {
                if (this.state.ports()) {
                    act(editor, MsdView.DISABLE);
                }
            }
            case 'o' -> {
                if (this.state.ports()) {
                    closePorts(editor);
                }
            }
            default -> {
                // Any other letter is not one of this program's.
            }
        }
        return true;
    }

    @Override
    public boolean key(final TtyEditor editor, final int key, final int modifiers) {
        if (key == GLFW.GLFW_KEY_F3) {
            editor.quit();
            return true;
        }
        if (this.state.ports()) {
            return portsKey(editor, key);
        }
        final int picked = this.state.picked();
        switch (key) {
            case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_RIGHT -> show(editor, this.state.picking(picked ^ 1));
            case GLFW.GLFW_KEY_UP -> show(editor, this.state.picking(Math.max(picked % 2, picked - 2)));
            case GLFW.GLFW_KEY_DOWN -> show(editor, this.state.picking(Math.min(picked + 2,
                    MsdScreen.BUTTONS.length - 2 + picked % 2)));
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> openPorts(editor, picked);
            case GLFW.GLFW_KEY_ESCAPE -> editor.quit();
            default -> {
                return true;
            }
        }
        return true;
    }

    /**
     * A click on the glass: a button of the main screen is picked, and opened when it is one of the ports; in the
     * dialog a port is picked and its buttons are pressed.
     */
    @Override
    public boolean clicked(final TtyEditor editor, final int row, final int column) {
        if (!this.state.ports()) {
            final int button = MsdScreen.buttonAt(row, column, this.state.columns());
            if (button >= 0) {
                show(editor, this.state.picking(button));
                openPorts(editor, button);
            }
            return true;
        }
        if (row >= MsdScreen.PORTS_TOP && row < MsdScreen.PORTS_TOP + this.ports) {
            show(editor, this.state.picking(row - MsdScreen.PORTS_TOP));
        } else if (row == MsdScreen.dialogButtonsRow(this.ports)) {
            switch (MsdScreen.dialogButtonAt(column, this.state.columns())) {
                case 0 -> closePorts(editor);
                case 1 -> act(editor, MsdView.ENABLE);
                case 2 -> act(editor, MsdView.DISABLE);
                default -> {
                    // Between two buttons.
                }
            }
        }
        return true;
    }

    /** The keys of the ports dialog: arrows pick a port, Enter and Escape put the dialog away. */
    private boolean portsKey(final TtyEditor editor, final int key) {
        final int picked = this.state.picked();
        switch (key) {
            case GLFW.GLFW_KEY_UP -> show(editor, this.state.picking(Math.max(0, picked - 1)));
            case GLFW.GLFW_KEY_DOWN -> show(editor, this.state.picking(Math.min(Math.max(0, this.ports - 1),
                    picked + 1)));
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_ESCAPE -> closePorts(editor);
            default -> {
                return true;
            }
        }
        return true;
    }

    /** Opens the ports dialog from one of the two buttons that open it; the others show all they found already. */
    private void openPorts(final TtyEditor editor, final int button) {
        if (button != MsdScreen.LPT_BUTTON && button != MsdScreen.COM_BUTTON) {
            return;
        }
        this.opener = button;
        this.state = this.state.openingPorts(0).asking(button == MsdScreen.LPT_BUTTON ? MsdView.LPT : MsdView.COM);
        fetch(editor, this.state, true);
        this.state = this.state.done();
    }

    /** Puts the dialog away, the main screen back on the button that opened it. */
    private void closePorts(final TtyEditor editor) {
        show(editor, this.state.closingPorts(this.opener));
    }

    /**
     * Asks the machine to enable or disable the picked port's device, and holds the screen without the request, so
     * the next key does not ask for it again.
     */
    private void act(final TtyEditor editor, final String what) {
        fetch(editor, this.state.asking(what), false);
        this.state = this.state.done();
    }

    private void show(final TtyEditor editor, final MsdState wanted) {
        this.state = wanted;
        fetch(editor, wanted, false);
    }

    /**
     * Asks the machine for that screen, by name as a file is asked for, and shows what comes back. When the dialog
     * was opened on a kind of port, the machine picked the first such port, and the screen says which.
     */
    private void fetch(final TtyEditor editor, final MsdState wanted, final boolean readPicked) {
        editor.read(wanted.path().substring(MsdState.SCHEME.length()), (content, existed) -> {
            editor.document().setText(content);
            editor.toTheTop();
            final List<String> screen = List.of(content.split("\n", -1));
            this.ports = MsdScreen.portsSaid(screen);
            if (readPicked && this.state.ports()) {
                this.state = this.state.picking(MsdScreen.pickedSaid(screen));
            }
            caretOnThePicked(editor);
        });
    }

    /** Puts the caret on what is picked, so a terminal that draws one draws it in the right place. */
    private void caretOnThePicked(final TtyEditor editor) {
        if (this.state.ports()) {
            editor.document().setCursor(MsdScreen.PORTS_TOP + this.state.picked(), 0);
            return;
        }
        editor.document().setCursor(MsdScreen.GRID_TOP + this.state.picked() / 2 * MsdScreen.GRID_STEP, 0);
    }
}
