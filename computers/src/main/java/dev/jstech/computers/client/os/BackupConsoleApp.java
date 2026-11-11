/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.backup.BackupJob;
import dev.jstech.computers.backup.BackupPart;
import dev.jstech.computers.backup.BackupTexts;
import dev.jstech.computers.backup.LossCause;
import dev.jstech.computers.backup.TapeContents;
import dev.jstech.computers.backup.TapeDrive;
import dev.jstech.computers.backup.TapeFormat;
import dev.jstech.computers.hardware.DiskSpec;
import dev.jstech.computers.operation.payload.BackupActionPayload;
import dev.jstech.computers.operation.payload.BackupStatePayload;
import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.Checkbox;
import dev.jstech.core.client.gui.component.ColumnHeader;
import dev.jstech.core.client.gui.component.Dropdown;
import dev.jstech.core.client.gui.component.Label;
import dev.jstech.core.client.gui.component.ListView;
import dev.jstech.core.client.gui.component.Panel;
import dev.jstech.core.client.gui.component.Popup;
import dev.jstech.core.client.gui.component.ProgressBar;
import dev.jstech.core.client.gui.component.TabStrip;
import dev.jstech.core.client.gui.component.TextField;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.client.gui.component.UiComponent;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.time.GameCalendar;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * The Backup Console: the window of the network's Backup Service, in four tabs, as the Messenger is the window of its
 * service (computers/docs/design/programs.md). Jobs lists the service's jobs and runs them; Back up makes or changes
 * one; Restore lists the backups the network holds and puts one back, its items through the book of losses; Tapes
 * handles the tapes of a Tape Library. Every click is an action the service answers with the console's whole state.
 */
public final class BackupConsoleApp implements IDesktopApp {

    private final BlockPos host;
    private final BlockPos monitor;
    private OsSkin skin = OsSkin.fallback();
    @Nullable
    private BackupStatePayload state;
    private int tab;
    private long lastRequestMillis;
    private long noteUntil;
    private boolean closing;
    private int lastX;
    private int lastY;
    private int lastW;
    private int lastH;
    private int lastMouseX;
    private int lastMouseY;
    // What the Back up tab is filling in: a new job, or the job of the name it was opened on.
    private String editing = "";
    private int parts = DEFAULT_PARTS;
    private int whereKind;
    private int libraryIndex;
    private int driveIndex;
    private int whenKind;
    private int day;
    private int hour = DEFAULT_HOUR;
    // What Restore and Tapes look at.
    private UUID chosen = BackupActionPayload.NONE;
    private final Set<UUID> images = new HashSet<>();
    private BackupJob.Target shownLibrary = BackupJob.Target.NOWHERE;

    private final Panel root = new Panel();
    private final TabStrip tabs;
    private final Label intro;
    private final ColumnHeader jobColumns;
    private final ListView<BackupStatePayload.Job> jobList;
    private final Label noJobs;
    private final Button newJob;
    private final Button runNow;
    private final Button edit;
    private final Button delete;
    private final Label whatHeading;
    private final Label nameLabel;
    private final TextField nameField;
    private final Map<BackupPart, Checkbox> partBoxes = new EnumMap<>(BackupPart.class);
    private final Map<BackupPart, Label> partDetails = new EnumMap<>(BackupPart.class);
    private final Label itemsNote;
    private final Label whereHeading;
    private final Option toLibrary;
    private final Option toDrive;
    private final Button nextPlace;
    private final Label whenHeading;
    private final Option byHand;
    private final Option asJob;
    private final Dropdown dayChoice;
    private final Dropdown hourChoice;
    private final Button create;
    private final Button cancel;
    private final ColumnHeader backupColumns;
    private final ListView<BackupStatePayload.Held> backupList;
    private final Label noBackups;
    private final Label detailHeading;
    private final Label alsoLine;
    private final ListView<BackupStatePayload.Image> imageList;
    private final Label imageNote;
    private final ColumnHeader lossColumns;
    private final ListView<BackupStatePayload.Loss> lossList;
    private final Label onceNote;
    private final Label existNote;
    private final Label restoringLine;
    private final Label windLine;
    private final ProgressBar windBar;
    private final Button restore;
    private final Button close;
    private final ColumnHeader tapeColumns;
    private final ListView<BackupStatePayload.Tape> tapeList;
    private final Label noLibrary;
    private final Button label;
    private final Button erase;
    private final Button eject;
    private final Button nextLibrary;
    private final Popup labelPopup;
    private final Label labelHint;
    private final TextField labelField;
    private final Button labelApply;
    private final Button labelCancel;
    private final Popup erasePopup;
    private final Label eraseHint;
    private final Button eraseApply;
    private final Button eraseCancel;

    private static final TextKey[] TABS = {BackupConsoleTexts.JOBS_TAB, BackupConsoleTexts.BACK_UP_TAB,
            BackupConsoleTexts.RESTORE_TAB, BackupConsoleTexts.TAPES_TAB};
    /** A new job keeps what a nightly backup of a network usually does: everything but the system images. */
    private static final int DEFAULT_PARTS = BackupPart.mask(EnumSet.complementOf(EnumSet.of(BackupPart.IMAGES)));
    private static final int DEFAULT_HOUR = 2;
    private static final int JOBS = 0;
    private static final int BACK_UP = 1;
    private static final int RESTORE = 2;
    private static final int TAPES = 3;
    private static final int TAB_H = 13;
    private static final int STATUS_H = 11;
    private static final int ROW_H = 11;
    private static final int BTN_H = 12;
    private static final int PAD = 4;
    private static final int GAP = 6;
    private static final int LINE_H = 9;
    private static final int POPUP_W = 200;
    private static final int POPUP_H = 62;
    private static final int NIGHT_ENDS = 6;
    private static final int NIGHT_STARTS = 20;
    private static final int NOTE_MILLIS = 6000;
    private static final float SMALL = 0.75f;
    private static final float HEADING = 0.85f;
    /** Milliseconds between state requests, quicker while a drive winds so its bar moves. */
    private static final long REFRESH_MILLIS = 2000L;
    private static final long WINDING_MILLIS = 500L;
    private static final int TICKS_PER_SECOND = 20;
    /* What the columns are measured against: a long name, a clock of a late day, a size near the top, five digits. */
    private static final String WIDE_NAME = "WWWWWWWW";
    private static final long WIDEST_CLOCK = 999L * 24_000L + 20_000L;
    private static final long WIDEST_SIZE_MB = 999L * 1024L;
    private static final String DIGITS = "00000";

    private static BackupConsoleApp active;

    public BackupConsoleApp(final BlockPos host, final BlockPos monitor) {
        this.host = host;
        this.monitor = monitor;
        final List<String> tabLabels = new ArrayList<>();
        for (final TextKey key : TABS) {
            tabLabels.add(GameText.resolve(key));
        }
        tabs = root.add(new TabStrip(tabLabels).fitToLabels(6).setOnSelect(this::selectTab));
        intro = root.add(new Label(this::introText, Label.Tone.DIM).setScale(SMALL));

        jobColumns = root.add(columns(BackupConsoleTexts.JOB_COLUMN, BackupConsoleTexts.WHAT_COLUMN,
                BackupConsoleTexts.WHERE_COLUMN, BackupConsoleTexts.WHEN_COLUMN, BackupConsoleTexts.LAST_RUN_COLUMN));
        jobList = root.add(new ListView<BackupStatePayload.Job>(this::jobs, ROW_H, this::renderJob)
                .setSelectable(true));
        noJobs = root.add(new Label(GameText.resolve(BackupConsoleTexts.NO_JOBS), Label.Tone.DIM).setScale(SMALL));
        newJob = root.add(new Button(GameText.resolve(BackupConsoleTexts.NEW_JOB), this::startNewJob));
        runNow = root.add(new Button(GameText.resolve(BackupConsoleTexts.RUN_NOW),
                () -> act(BackupActionPayload.RUN, selectedJobName())).setPrimary(true));
        edit = root.add(new Button(GameText.resolve(BackupConsoleTexts.EDIT), this::startEdit));
        delete = root.add(new Button(GameText.resolve(BackupConsoleTexts.DELETE),
                () -> act(BackupActionPayload.DELETE, selectedJobName())));

        whatHeading = root.add(new Label(GameText.resolve(BackupConsoleTexts.WHAT_HEADING)).setScale(HEADING));
        nameLabel = root.add(new Label(GameText.resolve(BackupConsoleTexts.NAME), Label.Tone.DIM).setScale(SMALL));
        nameField = root.add(new TextField(BackupJob.MOST_NAME));
        for (final BackupPart part : BackupPart.values()) {
            final int bit = BackupPart.mask(EnumSet.of(part));
            partBoxes.put(part, root.add(new Checkbox(() -> GameText.resolve(nameOf(part)),
                    () -> (parts & bit) != 0, () -> parts ^= bit).setLabelScale(SMALL)));
            partDetails.put(part, root.add(new Label(() -> partDetail(part), Label.Tone.DIM).setScale(SMALL)));
        }
        itemsNote = root.add(new Label(GameText.resolve(BackupConsoleTexts.ITEMS_NOTE), Label.Tone.DIM)
                .setScale(SMALL));
        whereHeading = root.add(new Label(GameText.resolve(BackupConsoleTexts.WHERE_HEADING)).setScale(HEADING));
        toLibrary = root.add(new Option(this::libraryOption, () -> whereKind == 0, () -> whereKind = 0));
        toDrive = root.add(new Option(this::driveOption, () -> whereKind == 1, () -> whereKind = 1));
        nextPlace = root.add(new Button(GameText.resolve(BackupConsoleTexts.NEXT), this::nextPlace)
                .setLabelScale(SMALL));
        whenHeading = root.add(new Label(GameText.resolve(BackupConsoleTexts.WHEN_HEADING)).setScale(HEADING));
        byHand = root.add(new Option(() -> GameText.resolve(BackupConsoleTexts.BY_HAND), () -> whenKind == 0,
                () -> whenKind = 0));
        asJob = root.add(new Option(() -> GameText.resolve(BackupConsoleTexts.AS_JOB), () -> whenKind == 1,
                () -> whenKind = 1));
        final List<Supplier<String>> days = new ArrayList<>();
        days.add(() -> GameText.resolve(hour >= NIGHT_STARTS || hour < NIGHT_ENDS ? BackupConsoleTexts.EVERY_NIGHT
                : BackupConsoleTexts.EVERY_DAY));
        for (final TextKey weekday : BackupConsoleTexts.WEEKDAYS) {
            days.add(() -> GameText.resolve(weekday));
        }
        dayChoice = root.add(new Dropdown(days, () -> day, chosenDay -> day = chosenDay, shown -> shown));
        final List<Supplier<String>> hours = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            final String at = clockText(h, 0);
            hours.add(() -> at);
        }
        hourChoice = root.add(new Dropdown(hours, () -> hour, chosenHour -> hour = chosenHour, shown -> shown));
        create = root.add(new Button(() -> GameText.resolve(editing.isEmpty() ? BackupConsoleTexts.CREATE
                : BackupConsoleTexts.SAVE), this::saveJob).setPrimary(true));
        cancel = root.add(new Button(GameText.resolve(BackupConsoleTexts.CANCEL), () -> selectTab(JOBS)));

        backupColumns = root.add(columns(BackupConsoleTexts.MADE_COLUMN, BackupConsoleTexts.JOB_COLUMN,
                BackupConsoleTexts.ON_COLUMN, BackupConsoleTexts.SIZE_COLUMN));
        backupList = root.add(new ListView<BackupStatePayload.Held>(this::backups, ROW_H, this::renderBackup)
                .setOnClick(this::backupClicked));
        noBackups = root.add(new Label(GameText.resolve(BackupConsoleTexts.NO_BACKUPS), Label.Tone.DIM)
                .setScale(SMALL));
        detailHeading = root.add(new Label(this::detailHeadingText).setScale(HEADING));
        alsoLine = root.add(new Label(this::alsoText, Label.Tone.DIM).setScale(SMALL));
        imageList = root.add(new ListView<BackupStatePayload.Image>(this::imagesOfChosen, ROW_H, this::renderImage)
                .setOnClick(this::imageClicked));
        imageNote = root.add(new Label(GameText.resolve(BackupConsoleTexts.IMAGE_NOTE), Label.Tone.DIM)
                .setScale(SMALL));
        lossColumns = root.add(columns(BackupConsoleTexts.ITEM_COLUMN, BackupConsoleTexts.LOST_COLUMN,
                BackupConsoleTexts.WHEN_COLUMN, BackupConsoleTexts.HOW_COLUMN, BackupConsoleTexts.IN_BACKUP_COLUMN,
                BackupConsoleTexts.BACK_COLUMN));
        lossList = root.add(new ListView<BackupStatePayload.Loss>(this::losses, ROW_H, this::renderLoss));
        onceNote = root.add(new Label(GameText.resolve(BackupConsoleTexts.ONCE_NOTE), Label.Tone.DIM)
                .setScale(SMALL));
        existNote = root.add(new Label(GameText.resolve(BackupConsoleTexts.STILL_EXIST_NOTE), Label.Tone.DIM)
                .setScale(SMALL));
        restoringLine = root.add(new Label(this::restoringText).setScale(SMALL));
        windLine = root.add(new Label(this::windText, Label.Tone.DIM).setScale(SMALL));
        windBar = root.add(new ProgressBar(() -> detail().permille() / 10));
        restore = root.add(new Button(GameText.resolve(BackupConsoleTexts.RESTORE), this::restoreChosen)
                .setPrimary(true));
        close = root.add(new Button(GameText.resolve(BackupConsoleTexts.CLOSE), () -> closing = true));

        tapeColumns = root.add(columns(BackupConsoleTexts.SLOT_COLUMN, BackupConsoleTexts.LABEL_COLUMN,
                BackupConsoleTexts.TAPE_COLUMN, BackupConsoleTexts.USED_COLUMN, BackupConsoleTexts.WRITTEN_COLUMN));
        tapeList = root.add(new ListView<BackupStatePayload.Tape>(this::tapesHeld, ROW_H, this::renderTape)
                .setSelectable(true));
        noLibrary = root.add(new Label(GameText.resolve(BackupConsoleTexts.NO_LIBRARY_HERE), Label.Tone.DIM)
                .setScale(SMALL));
        label = root.add(new Button(GameText.resolve(BackupConsoleTexts.LABEL), this::openLabel));
        erase = root.add(new Button(GameText.resolve(BackupConsoleTexts.ERASE), this::openErase));
        eject = root.add(new Button(GameText.resolve(BackupConsoleTexts.EJECT),
                () -> tapeAct(BackupActionPayload.EJECT, "")));
        nextLibrary = root.add(new Button(GameText.resolve(BackupConsoleTexts.NEXT_LIBRARY), this::nextLibrary));

        labelPopup = new TypedPopup(() -> GameText.resolve(BackupConsoleTexts.LABEL_TITLE.with(selectedTapeNumber())),
                this::applyLabel).setLayouter(this::layoutLabelPopup);
        labelHint = labelPopup.add(new Label(GameText.resolve(BackupConsoleTexts.LABEL_HINT), Label.Tone.DIM)
                .setScale(SMALL));
        labelField = labelPopup.add(new TextField(TapeContents.MOST_LABEL));
        labelApply = labelPopup.add(new Button(GameText.resolve(BackupConsoleTexts.APPLY), this::applyLabel)
                .setPrimary(true));
        labelCancel = labelPopup.add(new Button(GameText.resolve(BackupConsoleTexts.CANCEL), labelPopup::close));
        erasePopup = new TypedPopup(() -> GameText.resolve(BackupConsoleTexts.ERASE_TITLE.with(selectedTapeNumber())),
                this::applyErase).setLayouter(this::layoutErasePopup);
        eraseHint = erasePopup.add(new Label(() -> GameText.resolve(BackupConsoleTexts.ERASE_HINT.with(
                selectedTapeBackups())), Label.Tone.DIM).setScale(SMALL));
        eraseApply = erasePopup.add(new Button(GameText.resolve(BackupConsoleTexts.ERASE), this::applyErase)
                .setPrimary(true));
        eraseCancel = erasePopup.add(new Button(GameText.resolve(BackupConsoleTexts.CANCEL), erasePopup::close));

        active = this;
        request();
    }

    /** Routes a state from the service to the open console. */
    public static void accept(final BackupStatePayload payload) {
        if (active == null) {
            return;
        }
        active.state = payload;
        if (!payload.service().note().isEmpty()) {
            active.noteUntil = Util.getMillis() + NOTE_MILLIS;
        }
        if (payload.tapes().library().library()) {
            active.shownLibrary = payload.tapes().library();
        }
        // A backup that went is no longer chosen, and neither are the images it held.
        if (payload.backups().stream().noneMatch(held -> held.id().equals(active.chosen))) {
            active.chosen = BackupActionPayload.NONE;
            active.images.clear();
        }
    }

    @Override
    public void onRestored() {
        active = this;
        request();
    }

    @Override
    public void onClosed() {
        if (active == this) {
            active = null;
        }
    }

    @Override
    public boolean wantsToClose() {
        return closing;
    }

    @Override
    public void applySkin(final OsSkin osSkin) {
        this.skin = osSkin;
    }

    @Override
    public String title() {
        return GameText.resolve(BackupConsoleTexts.TITLE);
    }

    // Wide enough for the five columns of the jobs and the two halves of the Back up tab.
    @Override
    public int defaultWidth() {
        return 360;
    }

    @Override
    public int defaultHeight() {
        return 226;
    }

    @Override
    public int minWidth() {
        return 340;
    }

    @Override
    public int minHeight() {
        return 210;
    }

    /** Until the service answers, the program can do nothing, which the pointer shows. */
    @Override
    public boolean waiting() {
        return state == null;
    }

    @Override
    public void renderContent(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                              final int height, final int mouseX, final int mouseY, final float partialTick) {
        lastX = x;
        lastY = y;
        lastW = width;
        lastH = height;
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        final long every = winding() ? WINDING_MILLIS : REFRESH_MILLIS;
        if (Util.getMillis() - lastRequestMillis >= every) {
            request();
        }
        g.fill(x, y, x + width, y + height, skin.windowBg());
        layout(font, x, y, width, height);
        root.render(g, new UiContext(skin, font, mouseX, mouseY, partialTick));
        renderStatusBar(g, font, x, y + height - STATUS_H, width);
    }

    @Override
    public boolean modalActive() {
        return labelPopup.isOpen() || erasePopup.isOpen();
    }

    @Override
    public void renderModal(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                            final int height, final int mouseX, final int mouseY) {
        final Popup open = openPopup();
        if (open != null) {
            open.renderIn(g, new UiContext(skin, font, mouseX, mouseY, 0f), x, y, width, height);
        }
    }

    @Override
    public void mouseClicked(final DesktopWindow window, final double mouseX, final double mouseY, final int button) {
        final Popup open = openPopup();
        if (open != null) {
            open.mouseClicked(mouseX, mouseY, button);
            return;
        }
        if (button == 0) {
            root.mouseClicked(mouseX, mouseY, button);
        }
    }

    @Override
    public void mouseDragged(final DesktopWindow window, final double mouseX, final double mouseY, final int button) {
        final Popup open = openPopup();
        if (open != null) {
            open.mouseDragged(mouseX, mouseY, button);
        } else {
            root.mouseDragged(mouseX, mouseY, button);
        }
    }

    @Override
    public void mouseReleased(final DesktopWindow window, final double mouseX, final double mouseY, final int button) {
        final Popup open = openPopup();
        if (open != null) {
            open.mouseReleased(mouseX, mouseY, button);
        } else {
            root.mouseReleased(mouseX, mouseY, button);
        }
    }

    @Override
    public boolean mouseScrolled(final double delta) {
        return openPopup() != null || root.mouseScrolled(lastMouseX, lastMouseY, delta);
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        final Popup open = openPopup();
        return open != null ? open.keyPressed(key, scanCode, modifiers) : root.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(final char c) {
        final Popup open = openPopup();
        return open != null ? open.charTyped(c) : root.charTyped(c);
    }

    @Override
    public void renderTooltip(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                              final int height, final int mouseX, final int mouseY) {
        final List<Component> lines = root.tooltip(mouseX, mouseY);
        if (!lines.isEmpty()) {
            g.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }

    // what the client tests read and click

    /** Whether the service has answered. */
    public boolean hasState() {
        return state != null;
    }

    /** The tab shown, from 0 for Jobs. */
    public int tab() {
        return tab;
    }

    /** The jobs listed, by name. */
    public List<String> jobNames() {
        final List<String> out = new ArrayList<>();
        for (final BackupStatePayload.Job job : jobs()) {
            out.add(job.name());
        }
        return out;
    }

    /** The backups listed, by the job that made each. */
    public List<String> backupJobs() {
        final List<String> out = new ArrayList<>();
        for (final BackupStatePayload.Held held : backups()) {
            out.add(held.job());
        }
        return out;
    }

    /** What the status bar says at the left. */
    public String statusLine() {
        return leftStatus();
    }

    /** The centre of the tab {@code index}, to click it. */
    public int[] tabCenter(final int index) {
        return tabs.tabCenter(index);
    }

    /** The centre of the button {@code which}: new, run, edit, delete, create, restore, label, erase or eject. */
    public int[] buttonCenter(final String which) {
        return switch (which) {
            case "new" -> newJob.center();
            case "run" -> runNow.center();
            case "edit" -> edit.center();
            case "delete" -> delete.center();
            case "create" -> create.center();
            case "restore" -> restore.center();
            case "label" -> label.center();
            case "erase" -> erase.center();
            default -> eject.center();
        };
    }

    /** The centre of row {@code index} of the list the tab shows. */
    public int[] rowCenter(final int index) {
        return switch (tab) {
            case JOBS -> jobList.rowCenter(index);
            case RESTORE -> backupList.rowCenter(index);
            default -> tapeList.rowCenter(index);
        };
    }

    /** Types {@code name} into the Back up tab's name field, as a player would. */
    public void typeName(final String name) {
        nameField.set(name);
    }

    // state readers

    private List<BackupStatePayload.Job> jobs() {
        return state == null ? List.of() : state.jobs();
    }

    private List<BackupStatePayload.Held> backups() {
        return state == null ? List.of() : state.backups();
    }

    private BackupStatePayload.Form form() {
        return state == null ? BackupStatePayload.Form.EMPTY : state.form();
    }

    private BackupStatePayload.Detail detail() {
        return state == null || !state.detail().id().equals(chosen) ? BackupStatePayload.Detail.NONE
                : state.detail();
    }

    private BackupStatePayload.Tapes tapes() {
        return state == null ? BackupStatePayload.Tapes.NONE : state.tapes();
    }

    private List<BackupStatePayload.Image> imagesOfChosen() {
        return detail().images();
    }

    private List<BackupStatePayload.Loss> losses() {
        return detail().losses();
    }

    private List<BackupStatePayload.Tape> tapesHeld() {
        final List<BackupStatePayload.Tape> out = new ArrayList<>();
        for (final BackupStatePayload.Tape tape : tapes().slots()) {
            if (tape.present()) {
                out.add(tape);
            }
        }
        return out;
    }

    @Nullable
    private BackupStatePayload.Job selectedJob() {
        final int index = jobList.selected();
        return index >= 0 && index < jobs().size() ? jobs().get(index) : null;
    }

    private String selectedJobName() {
        final BackupStatePayload.Job job = selectedJob();
        return job == null ? "" : job.name();
    }

    @Nullable
    private BackupStatePayload.Held chosenBackup() {
        for (final BackupStatePayload.Held held : backups()) {
            if (held.id().equals(chosen)) {
                return held;
            }
        }
        return null;
    }

    @Nullable
    private BackupStatePayload.Tape selectedTape() {
        final int index = tapeList.selected();
        final List<BackupStatePayload.Tape> held = tapesHeld();
        return index >= 0 && index < held.size() ? held.get(index) : null;
    }

    private int selectedTapeNumber() {
        final BackupStatePayload.Tape tape = selectedTape();
        return tape == null ? 0 : tape.slot() + 1;
    }

    private int selectedTapeBackups() {
        final BackupStatePayload.Tape tape = selectedTape();
        int count = 0;
        for (final BackupStatePayload.Held held : backups()) {
            if (tape != null && held.onTape() && held.tape() == tape.slot()
                    && held.library().equals(tapes().name())) {
                count++;
            }
        }
        return count;
    }

    private boolean winding() {
        return detail().windTape() >= 0 || tapes().drive().tape() >= 0;
    }

    // texts

    private String introText() {
        return switch (tab) {
            case JOBS -> GameText.resolve(BackupConsoleTexts.JOBS_INTRO);
            case TAPES -> tapesIntro();
            default -> "";
        };
    }

    private String tapesIntro() {
        final BackupStatePayload.Tapes tapes = tapes();
        if (!tapes.library().library()) {
            return "";
        }
        final BackupStatePayload.Drive drive = tapes.drive();
        final TextKey doing = switch (TapeDrive.State.byId(drive.state())) {
            case IDLE -> BackupConsoleTexts.IDLE;
            case WINDING -> BackupConsoleTexts.WINDING_STATE;
            case READING -> BackupConsoleTexts.READING;
            case WRITING -> BackupConsoleTexts.WRITING;
        };
        final String now = drive.tape() < 0 ? GameText.resolve(BackupConsoleTexts.DRIVE_IDLE)
                : GameText.resolve(BackupConsoleTexts.DRIVE_BUSY.with(drive.tape() + 1, GameText.resolve(doing)));
        return GameText.resolve(BackupConsoleTexts.TAPES_INTRO.with(rackRow(tapes.name()))) + " " + now;
    }

    /* What a part is called in the Back up tab's list. */
    private static TextKey nameOf(final BackupPart part) {
        return switch (part) {
            case ITEMS -> BackupConsoleTexts.ITEMS;
            case FILES -> BackupConsoleTexts.FILES;
            case SETTINGS -> BackupConsoleTexts.SETTINGS;
            case PATTERNS -> BackupConsoleTexts.PATTERNS;
            case SCRIPTS -> BackupConsoleTexts.SCRIPTS;
            case IMAGES -> BackupConsoleTexts.IMAGES;
        };
    }

    /* A part as a word in a line of them: items, files, settings. */
    private static TextKey wordOf(final BackupPart part) {
        return switch (part) {
            case ITEMS -> BackupConsoleTexts.PART_ITEMS;
            case FILES -> BackupConsoleTexts.PART_FILES;
            case SETTINGS -> BackupConsoleTexts.PART_SETTINGS;
            case PATTERNS -> BackupConsoleTexts.PART_PATTERNS;
            case SCRIPTS -> BackupConsoleTexts.PART_SCRIPTS;
            case IMAGES -> BackupConsoleTexts.PART_IMAGES;
        };
    }

    private String partDetail(final BackupPart part) {
        final BackupStatePayload.Form form = form();
        return switch (part) {
            case ITEMS -> GameText.resolve(BackupConsoleTexts.ITEMS_DETAIL.with(form.items(),
                    DiskSpec.sizeLabel(form.weight() * placeMbPerItem() / 1000L)));
            case FILES -> form.files().isEmpty() ? GameText.resolve(BackupConsoleTexts.NO_HOMES)
                    : String.join(", ", form.files());
            case SETTINGS, SCRIPTS -> GameText.resolve(BackupConsoleTexts.EVERY_COMPUTER);
            case PATTERNS -> GameText.resolve(BackupConsoleTexts.THE_NETWORKS);
            case IMAGES -> !BackupPart.of(parts).contains(BackupPart.IMAGES)
                    ? GameText.resolve(BackupConsoleTexts.NONE_CHOSEN) : String.join(", ", form.machines());
        };
    }

    private String libraryOption() {
        final List<BackupStatePayload.Place> libraries = form().libraries();
        if (libraries.isEmpty()) {
            return GameText.resolve(BackupConsoleTexts.NO_LIBRARY);
        }
        final BackupStatePayload.Place place = libraries.get(Math.min(libraryIndex, libraries.size() - 1));
        return GameText.resolve(BackupConsoleTexts.TO_LIBRARY.with(rackRow(place.name()),
                DiskSpec.sizeLabel(place.freeMb())));
    }

    private String driveOption() {
        final List<BackupStatePayload.Place> drives = form().drives();
        if (drives.isEmpty()) {
            return GameText.resolve(BackupConsoleTexts.NO_DRIVE);
        }
        final BackupStatePayload.Place place = drives.get(Math.min(driveIndex, drives.size() - 1));
        return GameText.resolve(BackupConsoleTexts.TO_DRIVE.with(GameText.resolve(BackupConsoleTexts.DRIVE_OF.with(
                place.name(), driveLetter(place.target().drive()))), DiskSpec.sizeLabel(place.freeMb())));
    }

    private String detailHeadingText() {
        final BackupStatePayload.Held held = chosenBackup();
        if (held == null) {
            return "";
        }
        return GameText.resolve(BackupPart.of(held.parts()).contains(BackupPart.ITEMS)
                ? BackupConsoleTexts.LOST_HEADING : BackupConsoleTexts.IN_BACKUP);
    }

    private String alsoText() {
        final BackupStatePayload.Held held = chosenBackup();
        if (held == null) {
            return "";
        }
        final List<String> words = new ArrayList<>();
        for (final BackupPart part : BackupPart.of(held.parts())) {
            if (part != BackupPart.ITEMS && part != BackupPart.IMAGES) {
                words.add(GameText.resolve(wordOf(part)));
            }
        }
        return words.isEmpty() ? "" : GameText.resolve(BackupConsoleTexts.ALSO.with(String.join(", ", words)));
    }

    private String restoringText() {
        final Map<String, Long> back = new LinkedHashMap<>();
        long total = 0L;
        for (final BackupStatePayload.Loss loss : losses()) {
            if (loss.back() > 0L) {
                back.merge(loss.key().displayName().getString(), loss.back(), Long::sum);
                total += loss.back();
            }
        }
        if (total == 0L) {
            return GameText.resolve(BackupConsoleTexts.NOTHING_LOST);
        }
        final List<String> each = new ArrayList<>();
        back.forEach((name, count) -> each.add(name + " " + count));
        return GameText.resolve(BackupConsoleTexts.RESTORING.with(total, String.join(", ", each)));
    }

    private String windText() {
        final BackupStatePayload.Detail detail = detail();
        if (detail.windTape() < 0) {
            return "";
        }
        return GameText.resolve(BackupConsoleTexts.WINDING.with(detail.windTape() + 1,
                Math.max(1L, Math.ceilDiv(detail.ticksLeft(), TICKS_PER_SECOND))));
    }

    private String leftStatus() {
        if (state == null) {
            return "";
        }
        if (Util.getMillis() < noteUntil && !state.service().note().isEmpty()) {
            return GameText.resolve(state.service().note());
        }
        if (!state.service().present()) {
            return GameText.resolve(BackupTexts.NO_SERVICE);
        }
        final String on = GameText.resolve(BackupConsoleTexts.SERVICE_ON.with(state.service().host()));
        return state.service().engine() ? on : on + "; " + GameText.resolve(BackupConsoleTexts.NO_ENGINE);
    }

    private String rightStatus() {
        final BackupStatePayload.Tapes tapes = tapes();
        if (!tapes.library().library()) {
            return "";
        }
        long used = 0L;
        for (final BackupStatePayload.Tape tape : tapesHeld()) {
            used += tape.usedMb();
        }
        return GameText.resolve(BackupConsoleTexts.LIBRARY_STATUS.with(rackRow(tapes.name()), tapesHeld().size(),
                DiskSpec.sizeLabel(used), DiskSpec.sizeLabel(tapes.capacityMb() * tapesHeld().size())));
    }

    private String whatText(final BackupStatePayload.Job job) {
        final Set<BackupPart> kept = BackupPart.of(job.parts());
        if (kept.equals(EnumSet.of(BackupPart.IMAGES))) {
            return GameText.resolve(BackupConsoleTexts.IMAGES_OF.with(String.join(", ", form().machines())));
        }
        final List<String> words = new ArrayList<>();
        for (final BackupPart part : kept) {
            words.add(GameText.resolve(wordOf(part)));
        }
        final String joined = String.join(", ", words);
        return joined.isEmpty() ? "" : Character.toUpperCase(joined.charAt(0)) + joined.substring(1);
    }

    private String whereText(final BackupStatePayload.Job job) {
        return job.target().library() ? GameText.resolve(BackupConsoleTexts.TAPE_LIBRARY)
                : GameText.resolve(BackupConsoleTexts.DRIVE_OF.with(job.where(), driveLetter(job.target().drive())));
    }

    private static String whenText(final BackupJob.Schedule when) {
        if (!when.timed()) {
            return GameText.resolve(BackupConsoleTexts.BY_HAND);
        }
        final TextKey days = when.kind() == BackupJob.Schedule.Kind.WEEKLY ? BackupConsoleTexts.WEEKDAYS[when.weekday()]
                : when.hour() >= NIGHT_STARTS || when.hour() < NIGHT_ENDS ? BackupConsoleTexts.EVERY_NIGHT
                : BackupConsoleTexts.EVERY_DAY;
        return GameText.resolve(BackupConsoleTexts.WHEN_AT.with(GameText.resolve(days), clockText(when.hour(), 0)));
    }

    private static String lastRunText(final BackupStatePayload.Job job) {
        if (job.lastRun() < 0L || job.result() == BackupJob.RunState.NEVER) {
            return GameText.resolve(BackupConsoleTexts.NEVER);
        }
        final TextKey how = switch (job.result()) {
            case DONE -> BackupConsoleTexts.DONE;
            case NO_ROOM -> BackupConsoleTexts.NO_ROOM;
            case NO_TARGET -> BackupConsoleTexts.NO_TARGET;
            case INTERRUPTED -> BackupConsoleTexts.INTERRUPTED;
            case RUNNING -> BackupConsoleTexts.RUNNING;
            case NEVER -> BackupConsoleTexts.NEVER;
        };
        return GameText.resolve(BackupConsoleTexts.LAST_RUN.with(dayClock(job.lastRun()), GameText.resolve(how)));
    }

    private static String howText(final BackupStatePayload.Loss loss) {
        final Text how = switch (LossCause.byId(loss.cause())) {
            case DEFRAG -> BackupConsoleTexts.HOW_DEFRAG.text();
            case DEAD_DISK -> BackupConsoleTexts.HOW_DEAD_DISK.with(loss.machine(), Math.max(1, loss.disk()));
            case RAID0 -> BackupConsoleTexts.HOW_RAID0.with(loss.machine());
            case DROP -> BackupConsoleTexts.HOW_DROP.with(loss.machine());
        };
        return GameText.resolve(how);
    }

    private static String dayClock(final long clock) {
        return GameText.resolve(BackupConsoleTexts.DAY_CLOCK.with(GameCalendar.day(clock) + 1,
                clockText(GameCalendar.hourOf(clock), GameCalendar.minuteOf(clock))));
    }

    private static String clockText(final int h, final int m) {
        return String.format(Locale.ROOT, "%02d:%02d", h, m);
    }

    /* A library's name as the network gives it, "2:5", in words: rack 2, 5U. */
    private static String rackRow(final String name) {
        final int colon = name.indexOf(':');
        return colon < 0 ? name : GameText.resolve(BackupConsoleTexts.RACK_ROW.with(name.substring(0, colon),
                name.substring(colon + 1)));
    }

    private static String driveLetter(final int drive) {
        return String.valueOf((char) ('C' + Math.max(0, Math.min(23, drive))));
    }

    private long placeMbPerItem() {
        final List<BackupStatePayload.Place> places = whereKind == 0 ? form().libraries() : form().drives();
        final int index = whereKind == 0 ? libraryIndex : driveIndex;
        return places.isEmpty() ? 1L : places.get(Math.min(index, places.size() - 1)).mbPerItem();
    }

    // actions

    private void request() {
        lastRequestMillis = Util.getMillis();
        PacketDistributor.sendToServer(BackupActionPayload.of(host, monitor, BackupActionPayload.LOOK, "",
                shownLibrary, chosen));
    }

    private void act(final int action, final String job) {
        if (!job.isEmpty()) {
            PacketDistributor.sendToServer(BackupActionPayload.of(host, monitor, action, job, shownLibrary, chosen));
            lastRequestMillis = Util.getMillis();
        }
    }

    private void tapeAct(final int action, final String text) {
        final BackupStatePayload.Tape tape = selectedTape();
        if (tape == null) {
            return;
        }
        PacketDistributor.sendToServer(new BackupActionPayload(host, monitor, action, "", "", 0, shownLibrary,
                BackupJob.Schedule.BY_HAND, chosen, List.of(), tape.slot(), text));
        lastRequestMillis = Util.getMillis();
    }

    private void selectTab(final int target) {
        tab = Math.max(0, Math.min(TABS.length - 1, target));
        tabs.setSelected(tab);
    }

    private void startNewJob() {
        editing = "";
        nameField.set("");
        parts = DEFAULT_PARTS;
        whereKind = form().libraries().isEmpty() && !form().drives().isEmpty() ? 1 : 0;
        libraryIndex = 0;
        driveIndex = 0;
        whenKind = 1;
        day = 0;
        hour = DEFAULT_HOUR;
        selectTab(BACK_UP);
    }

    private void startEdit() {
        final BackupStatePayload.Job job = selectedJob();
        if (job == null) {
            return;
        }
        editing = job.name();
        nameField.set(job.name());
        parts = job.parts();
        whereKind = job.target().library() ? 0 : 1;
        libraryIndex = Math.max(0, indexOf(form().libraries(), job.target()));
        driveIndex = Math.max(0, indexOf(form().drives(), job.target()));
        whenKind = job.when().timed() ? 1 : 0;
        day = job.when().kind() == BackupJob.Schedule.Kind.WEEKLY ? job.when().weekday() + 1 : 0;
        hour = job.when().hour();
        selectTab(BACK_UP);
    }

    private void saveJob() {
        final BackupJob.Target target = formTarget();
        if (target == null) {
            return;
        }
        final BackupJob.Schedule when = whenKind == 0 ? BackupJob.Schedule.BY_HAND
                : day == 0 ? new BackupJob.Schedule(BackupJob.Schedule.Kind.DAILY, hour, 0)
                : new BackupJob.Schedule(BackupJob.Schedule.Kind.WEEKLY, hour, day - 1);
        final String name = nameField.edit().strip();
        PacketDistributor.sendToServer(new BackupActionPayload(host, monitor, BackupActionPayload.SAVE,
                name.length() > BackupJob.MOST_NAME ? name.substring(0, BackupJob.MOST_NAME) : name, editing, parts,
                target, when, chosen, List.of(), -1, ""));
        lastRequestMillis = Util.getMillis();
        editing = "";
        selectTab(JOBS);
    }

    @Nullable
    private BackupJob.Target formTarget() {
        final List<BackupStatePayload.Place> places = whereKind == 0 ? form().libraries() : form().drives();
        final int index = whereKind == 0 ? libraryIndex : driveIndex;
        return places.isEmpty() ? null : places.get(Math.min(index, places.size() - 1)).target();
    }

    private void nextPlace() {
        if (whereKind == 0 && !form().libraries().isEmpty()) {
            libraryIndex = (libraryIndex + 1) % form().libraries().size();
        } else if (whereKind == 1 && !form().drives().isEmpty()) {
            driveIndex = (driveIndex + 1) % form().drives().size();
        }
    }

    private void backupClicked(final int index, final int button, final double mx, final double my) {
        if (button == 0 && index >= 0 && index < backups().size()) {
            chosen = backups().get(index).id();
            images.clear();
            request();
        }
    }

    private void imageClicked(final int index, final int button, final double mx, final double my) {
        final List<BackupStatePayload.Image> list = imagesOfChosen();
        if (button == 0 && index >= 0 && index < list.size() && !images.remove(list.get(index).node())) {
            images.add(list.get(index).node());
        }
    }

    private void restoreChosen() {
        final BackupStatePayload.Held held = chosenBackup();
        if (held == null) {
            return;
        }
        PacketDistributor.sendToServer(new BackupActionPayload(host, monitor, BackupActionPayload.RESTORE, "", "",
                held.parts(), shownLibrary, BackupJob.Schedule.BY_HAND, chosen, List.copyOf(images), -1, ""));
        lastRequestMillis = Util.getMillis();
    }

    private boolean restorable() {
        final BackupStatePayload.Held held = chosenBackup();
        if (held == null || detail().windTape() >= 0) {
            return false;
        }
        final Set<BackupPart> kept = BackupPart.of(held.parts());
        return !images.isEmpty() || kept.stream().anyMatch(part -> part != BackupPart.IMAGES);
    }

    private void nextLibrary() {
        final List<BackupStatePayload.Place> libraries = form().libraries();
        if (libraries.size() < 2) {
            return;
        }
        final int now = Math.max(0, indexOf(libraries, shownLibrary));
        shownLibrary = libraries.get((now + 1) % libraries.size()).target();
        tapeList.setSelected(-1);
        request();
    }

    private void openLabel() {
        final BackupStatePayload.Tape tape = selectedTape();
        if (tape == null) {
            return;
        }
        labelField.set(tape.label());
        labelPopup.open();
        labelPopup.placeIn(lastX, lastY, lastW, lastH);
        labelPopup.focus(labelField);
        labelField.selectAll();
    }

    private void applyLabel() {
        final String typed = labelField.edit().strip();
        tapeAct(BackupActionPayload.LABEL, typed.length() > TapeContents.MOST_LABEL
                ? typed.substring(0, TapeContents.MOST_LABEL) : typed);
        labelPopup.close();
    }

    private void openErase() {
        if (selectedTape() != null) {
            erasePopup.open();
            erasePopup.placeIn(lastX, lastY, lastW, lastH);
        }
    }

    private void applyErase() {
        tapeAct(BackupActionPayload.ERASE, "");
        erasePopup.close();
    }

    @Nullable
    private Popup openPopup() {
        return labelPopup.isOpen() ? labelPopup : erasePopup.isOpen() ? erasePopup : null;
    }

    private static int indexOf(final List<BackupStatePayload.Place> places, final BackupJob.Target target) {
        for (int i = 0; i < places.size(); i++) {
            if (places.get(i).target().equals(target)) {
                return i;
            }
        }
        return -1;
    }

    private static ColumnHeader columns(final TextKey... keys) {
        final List<String> labels = new ArrayList<>();
        for (final TextKey key : keys) {
            labels.add(GameText.resolve(key));
        }
        return new ColumnHeader(labels).setSortable(false);
    }

    // layout

    /** Places every component from the content rectangle and the state; hides what the tab has no use for. */
    private void layout(final Font font, final int x, final int y, final int width, final int height) {
        tabs.setBounds(x, y, width, TAB_H);
        tabs.setSelected(tab);
        final int top = y + TAB_H + 3;
        final int bottom = y + height - STATUS_H - PAD;
        final int px = x + PAD;
        final int pw = width - PAD * 2;
        final boolean ready = state != null && state.service().present();
        for (final UiComponent c : allBodies()) {
            c.setVisible(false);
        }
        intro.setVisible(ready && (tab == JOBS || tab == TAPES));
        intro.setBounds(px, top, pw, 8);
        if (!ready) {
            return;
        }
        switch (tab) {
            case JOBS -> layoutJobs(font, x, px, pw, top + 10, bottom, width);
            case BACK_UP -> layoutBackUp(font, px, pw, top, bottom);
            case RESTORE -> layoutRestore(font, x, px, pw, top, bottom, width);
            default -> layoutTapes(font, x, px, pw, top + 10, bottom, width);
        }
    }

    private List<UiComponent> allBodies() {
        final List<UiComponent> all = new ArrayList<>(List.of(jobColumns, jobList, noJobs, newJob, runNow, edit,
                delete, whatHeading, nameLabel, nameField, itemsNote, whereHeading, toLibrary, toDrive, nextPlace,
                whenHeading, byHand, asJob, dayChoice, hourChoice, create, cancel, backupColumns, backupList,
                noBackups, detailHeading, alsoLine, imageList, imageNote, lossColumns, lossList, onceNote, existNote,
                restoringLine, windLine, windBar, restore, close, tapeColumns, tapeList, noLibrary, label, erase,
                eject, nextLibrary));
        all.addAll(partBoxes.values());
        all.addAll(partDetails.values());
        return all;
    }

    private void layoutJobs(final Font font, final int x, final int px, final int pw, final int top,
                            final int bottom, final int width) {
        final int by = bottom - BTN_H;
        jobColumns.setVisible(true);
        jobColumns.setBounds(x, top, width, ROW_H);
        final int whatX = px + scaledWidth(font, WIDE_NAME) * 3 / 2 + GAP;
        final int runX = x + width - PAD - scaledWidth(font, GameText.resolve(BackupConsoleTexts.LAST_RUN.with(
                dayClock(WIDEST_CLOCK), GameText.resolve(BackupConsoleTexts.NO_TARGET))));
        final int whenX = runX - scaledWidth(font, GameText.resolve(BackupConsoleTexts.WHEN_AT.with(
                GameText.resolve(BackupConsoleTexts.WEDNESDAYS), clockText(0, 0)))) - GAP;
        final int whereX = whenX - scaledWidth(font, GameText.resolve(BackupConsoleTexts.DRIVE_OF.with(
                WIDE_NAME, driveLetter(0)))) - GAP;
        jobColumns.setColumnX(px, whatX, whereX, whenX, runX);
        jobList.setVisible(!jobs().isEmpty());
        jobList.setBounds(x, top + ROW_H, width, Math.max(ROW_H, (by - PAD - top - ROW_H) / ROW_H * ROW_H));
        noJobs.setVisible(jobs().isEmpty());
        noJobs.setBounds(px, top + ROW_H + 2, pw, 8);
        int bx = px;
        for (final Button button : List.of(newJob, runNow, edit, delete)) {
            final int bw = font.width(button.label()) + 12;
            button.setVisible(true);
            button.setBounds(bx, by, bw, BTN_H);
            bx += bw + PAD;
        }
        final boolean picked = selectedJob() != null;
        runNow.setEnabled(picked);
        edit.setEnabled(picked);
        delete.setEnabled(picked);
    }

    private void layoutBackUp(final Font font, final int px, final int pw, final int top, final int bottom) {
        whatHeading.setVisible(true);
        whatHeading.setBounds(px, top + 1, pw / 2, 8);
        final int fieldW = 90;
        nameField.setVisible(true);
        nameField.setBounds(px + pw - fieldW, top - 1, fieldW, ROW_H + 1);
        final int nameW = scaledWidth(font, GameText.resolve(BackupConsoleTexts.NAME));
        nameLabel.setVisible(true);
        nameLabel.setBounds(px + pw - fieldW - GAP - nameW, top + 2, nameW, 8);
        final int boxW = scaledWidth(font, GameText.resolve(BackupConsoleTexts.SCRIPTS)) + 18;
        int ry = top + 13;
        for (final BackupPart part : BackupPart.values()) {
            partBoxes.get(part).setVisible(true);
            partBoxes.get(part).setBounds(px, ry, boxW, ROW_H - 1);
            partDetails.get(part).setVisible(true);
            partDetails.get(part).setBounds(px + boxW + GAP, ry + 2, pw - boxW - GAP, 8);
            ry += ROW_H - 1;
        }
        itemsNote.setVisible(true);
        itemsNote.setBounds(px, ry + 2, pw, 8);
        ry += 14;
        final int rightX = px + pw * 3 / 5;
        final int leftW = rightX - px - GAP;
        whereHeading.setVisible(true);
        whereHeading.setBounds(px, ry, leftW, 8);
        whenHeading.setVisible(true);
        whenHeading.setBounds(rightX, ry, pw - (rightX - px), 8);
        ry += 11;
        toLibrary.setVisible(true);
        toLibrary.setBounds(px, ry, leftW, ROW_H);
        byHand.setVisible(true);
        byHand.setBounds(rightX, ry, pw - (rightX - px), ROW_H);
        ry += ROW_H + 1;
        toDrive.setVisible(true);
        toDrive.setBounds(px, ry, leftW, ROW_H);
        asJob.setVisible(true);
        asJob.setBounds(rightX, ry, pw - (rightX - px), ROW_H);
        ry += ROW_H + 2;
        final int places = whereKind == 0 ? form().libraries().size() : form().drives().size();
        nextPlace.setVisible(places > 1);
        nextPlace.setBounds(px + 13, ry, scaledWidth(font, GameText.resolve(BackupConsoleTexts.NEXT)) + 10, BTN_H);
        final int dayW = scaledWidth(font, GameText.resolve(BackupConsoleTexts.WEDNESDAYS)) + 22;
        dayChoice.setVisible(true);
        dayChoice.setEnabled(whenKind == 1);
        dayChoice.setBounds(rightX + 13, ry, dayW, BTN_H);
        hourChoice.setVisible(true);
        hourChoice.setEnabled(whenKind == 1);
        hourChoice.setBounds(rightX + 13 + dayW + PAD, ry, scaledWidth(font, clockText(0, 0)) + 22, BTN_H);
        final int by = bottom - BTN_H;
        final int cancelW = font.width(cancel.label()) + 14;
        final int createW = Math.max(font.width(GameText.resolve(BackupConsoleTexts.CREATE)),
                font.width(GameText.resolve(BackupConsoleTexts.SAVE))) + 14;
        cancel.setVisible(true);
        cancel.setBounds(px + pw - cancelW, by, cancelW, BTN_H);
        create.setVisible(true);
        create.setBounds(px + pw - cancelW - PAD - createW, by, createW, BTN_H);
        create.setEnabled(BackupJob.validName(nameField.edit().strip()) && parts != 0 && formTarget() != null);
    }

    private void layoutRestore(final Font font, final int x, final int px, final int pw, final int top,
                               final int bottom, final int width) {
        backupColumns.setVisible(true);
        backupColumns.setBounds(x, top, width, ROW_H);
        final int jobX = px + scaledWidth(font, dayClock(WIDEST_CLOCK)) + GAP * 2;
        final int onX = jobX + scaledWidth(font, WIDE_NAME) * 5 / 4 + GAP;
        final int sizeX = x + width - PAD - scaledWidth(font, DiskSpec.sizeLabel(WIDEST_SIZE_MB)) - GAP;
        backupColumns.setColumnX(px, jobX, onX, sizeX);
        final int listRows = chosenBackup() == null ? 8 : 3;
        backupList.setVisible(!backups().isEmpty());
        backupList.setBounds(x, top + ROW_H, width, ROW_H * listRows);
        noBackups.setVisible(backups().isEmpty());
        noBackups.setBounds(px, top + ROW_H + 2, pw, 8);
        final int by = bottom - BTN_H;
        final BackupStatePayload.Held held = chosenBackup();
        if (held != null) {
            final boolean items = BackupPart.of(held.parts()).contains(BackupPart.ITEMS);
            int ry = top + ROW_H * (listRows + 1) + 3;
            detailHeading.setVisible(true);
            detailHeading.setBounds(px, ry, pw, 8);
            ry += 10;
            alsoLine.setVisible(!alsoText().isEmpty());
            alsoLine.setBounds(px, ry, pw, 8);
            ry += alsoLine.visible() ? LINE_H : 0;
            if (!imagesOfChosen().isEmpty()) {
                final int rows = Math.min(imagesOfChosen().size(), items ? 1 : 3);
                imageList.setVisible(true);
                imageList.setBounds(x, ry, width, ROW_H * rows);
                ry += ROW_H * rows;
                imageNote.setVisible(!items);
                imageNote.setBounds(px, ry + 1, pw, 8);
                ry += imageNote.visible() ? LINE_H + 1 : 1;
            }
            if (items) {
                lossColumns.setVisible(true);
                lossColumns.setBounds(x, ry, width, ROW_H);
                final int lostX = px + scaledWidth(font, WIDE_NAME + WIDE_NAME) + GAP;
                final int whenX = lostX + scaledWidth(font, DIGITS) + GAP;
                final int howX = whenX + scaledWidth(font, dayClock(WIDEST_CLOCK)) + GAP;
                final int backX = x + width - PAD - scaledWidth(font, DIGITS);
                final int inX = backX - scaledWidth(font, GameText.resolve(BackupConsoleTexts.IN_BACKUP_COLUMN))
                        - GAP;
                lossColumns.setColumnX(px, lostX, whenX, howX, inX, backX);
                final int noteRoom = LINE_H * 3 + 2;
                final int rows = Math.max(1, (by - noteRoom - LINE_H - ry - ROW_H) / ROW_H);
                lossList.setVisible(true);
                lossList.setBounds(x, ry + ROW_H, width, ROW_H * rows);
                ry += ROW_H * (rows + 1) + 1;
                onceNote.setVisible(true);
                onceNote.setBounds(px, ry, pw, 8);
                existNote.setVisible(true);
                existNote.setBounds(px, ry + LINE_H, pw, 8);
                restoringLine.setVisible(true);
                restoringLine.setBounds(px, ry + LINE_H * 2 + 2, pw, 8);
            }
        }
        final int windW = pw * 3 / 5;
        final boolean wind = detail().windTape() >= 0;
        windLine.setVisible(wind);
        windLine.setBounds(px, by - 9, windW, 8);
        windBar.setVisible(wind);
        windBar.setBounds(px, by + 3, windW, 6);
        final int closeW = font.width(close.label()) + 14;
        final int restoreW = font.width(restore.label()) + 14;
        close.setVisible(true);
        close.setBounds(px + pw - closeW, by, closeW, BTN_H);
        restore.setVisible(held != null);
        restore.setBounds(px + pw - closeW - PAD - restoreW, by, restoreW, BTN_H);
        restore.setEnabled(restorable());
    }

    private void layoutTapes(final Font font, final int x, final int px, final int pw, final int top,
                             final int bottom, final int width) {
        final boolean any = tapes().library().library();
        noLibrary.setVisible(!any);
        noLibrary.setBounds(px, top, pw, 8);
        if (!any) {
            return;
        }
        tapeColumns.setVisible(true);
        tapeColumns.setBounds(x, top, width, ROW_H);
        final int labelX = px + scaledWidth(font, GameText.resolve(BackupConsoleTexts.SLOT_COLUMN)) + GAP * 2;
        final int tapeX = labelX + scaledWidth(font, WIDE_NAME) * 3 / 2 + GAP;
        final int usedX = tapeX + scaledWidth(font, GameText.resolve(BackupConsoleTexts.FORMAT.with(
                TapeFormat.REEL.label(), DiskSpec.sizeLabel(TapeFormat.REEL.capacityMb())))) + GAP;
        final int writtenX = usedX + scaledWidth(font, DiskSpec.sizeLabel(WIDEST_SIZE_MB)) + GAP;
        tapeColumns.setColumnX(px, labelX, tapeX, usedX, writtenX);
        final int by = bottom - BTN_H;
        tapeList.setVisible(true);
        tapeList.setBounds(x, top + ROW_H, width, Math.max(ROW_H, (by - PAD - top - ROW_H) / ROW_H * ROW_H));
        int bx = px;
        for (final Button button : List.of(label, erase, eject)) {
            final int bw = font.width(button.label()) + 12;
            button.setVisible(true);
            button.setEnabled(selectedTape() != null);
            button.setBounds(bx, by, bw, BTN_H);
            bx += bw + PAD;
        }
        final int nextW = font.width(nextLibrary.label()) + 12;
        nextLibrary.setVisible(tapes().count() > 1);
        nextLibrary.setBounds(px + pw - nextW, by, nextW, BTN_H);
    }

    private void layoutLabelPopup(final Popup p) {
        final int cy = p.contentTop();
        labelHint.setBounds(p.x() + PAD, cy + 1, p.width() - PAD * 2, 8);
        labelField.setBounds(p.x() + PAD, cy + 12, p.width() - PAD * 2, ROW_H + 2);
        final int bw = (p.width() - PAD * 2 - 2) / 2;
        final int by = p.bottom() - BTN_H - PAD;
        labelApply.setBounds(p.x() + PAD, by, bw, BTN_H);
        labelCancel.setBounds(p.x() + PAD + bw + 2, by, bw, BTN_H);
    }

    private void layoutErasePopup(final Popup p) {
        final int cy = p.contentTop();
        eraseHint.setBounds(p.x() + PAD, cy + 4, p.width() - PAD * 2, 8);
        final int bw = (p.width() - PAD * 2 - 2) / 2;
        final int by = p.bottom() - BTN_H - PAD;
        eraseApply.setBounds(p.x() + PAD, by, bw, BTN_H);
        eraseCancel.setBounds(p.x() + PAD + bw + 2, by, bw, BTN_H);
    }

    private static int scaledWidth(final Font font, final String text) {
        return Math.round(font.width(text) * SMALL);
    }

    // drawing

    private void renderStatusBar(final GuiGraphics g, final Font font, final int x, final int y, final int width) {
        skin.statusBar(g, x, y, width, STATUS_H);
        final String right = rightStatus();
        final int rightW = scaledWidth(font, right);
        final String left = Texts.clip(font, leftStatus(), (int) ((width - rightW - PAD * 3) / SMALL));
        Texts.scaled(g, font, left, x + PAD, y + 2, SMALL, skin.text());
        Texts.scaled(g, font, right, x + width - PAD - rightW, y + 2, SMALL, skin.dim());
    }

    private void renderJob(final GuiGraphics g, final UiContext ctx, final BackupStatePayload.Job job,
                           final int index, final int x, final int y, final int w, final int h,
                           final boolean hovered, final boolean selectedRow) {
        ctx.skin().listRow(g, x + 2, y, w - 4, h, hovered, selectedRow);
        cells(g, ctx, jobColumns, y, List.of(job.name(), whatText(job), whereText(job), whenText(job.when()),
                lastRunText(job)), x + w - PAD);
    }

    private void renderBackup(final GuiGraphics g, final UiContext ctx, final BackupStatePayload.Held held,
                              final int index, final int x, final int y, final int w, final int h,
                              final boolean hovered, final boolean selectedRow) {
        ctx.skin().listRow(g, x + 2, y, w - 4, h, hovered, held.id().equals(chosen));
        final String on = held.onTape() ? GameText.resolve(BackupConsoleTexts.ON_TAPE.with(held.tape() + 1))
                : GameText.resolve(BackupConsoleTexts.DRIVE_OF.with(held.machine(), driveLetter(held.drive())));
        cells(g, ctx, backupColumns, y, List.of(dayClock(held.clock()), held.job(), on,
                DiskSpec.sizeLabel(held.sizeMb())), x + w - PAD);
    }

    private void renderImage(final GuiGraphics g, final UiContext ctx, final BackupStatePayload.Image image,
                             final int index, final int x, final int y, final int w, final int h,
                             final boolean hovered, final boolean selectedRow) {
        ctx.skin().listRow(g, x + 2, y, w - 4, h, hovered, false);
        ctx.skin().checkbox(g, ctx.font(), x + PAD, y + 1, 8, images.contains(image.node()), hovered);
        Texts.scaled(g, ctx.font(), Texts.clip(ctx.font(), GameText.resolve(BackupConsoleTexts.IMAGE_ROW.with(
                image.machine(), image.system(), DiskSpec.sizeLabel(image.sizeMb()))),
                (int) ((w - PAD * 3 - 10) / SMALL)), x + PAD + 12, y + 2, SMALL, ctx.skin().text());
    }

    private void renderLoss(final GuiGraphics g, final UiContext ctx, final BackupStatePayload.Loss loss,
                            final int index, final int x, final int y, final int w, final int h,
                            final boolean hovered, final boolean selectedRow) {
        ctx.skin().listRow(g, x + 2, y, w - 4, h, hovered, false);
        cells(g, ctx, lossColumns, y, List.of(loss.key().displayName().getString(), Long.toString(loss.lost()),
                dayClock(loss.clock()), howText(loss), loss.inBackup() == 0L
                        ? GameText.resolve(BackupConsoleTexts.NONE) : Long.toString(loss.inBackup()),
                Long.toString(loss.back())), x + w - PAD);
    }

    private void renderTape(final GuiGraphics g, final UiContext ctx, final BackupStatePayload.Tape tape,
                            final int index, final int x, final int y, final int w, final int h,
                            final boolean hovered, final boolean selectedRow) {
        ctx.skin().listRow(g, x + 2, y, w - 4, h, hovered, selectedRow);
        final BackupStatePayload.Tapes tapes = tapes();
        cells(g, ctx, tapeColumns, y, List.of(Integer.toString(tape.slot() + 1),
                tape.label().isEmpty() ? GameText.resolve(BackupConsoleTexts.BLANK) : tape.label(),
                GameText.resolve(BackupConsoleTexts.FORMAT.with(tapes.format(), DiskSpec.sizeLabel(
                        tapes.capacityMb()))),
                tape.usedMb() == 0L ? "0" : DiskSpec.sizeLabel(tape.usedMb()),
                tape.lastClock() < 0L ? GameText.resolve(BackupConsoleTexts.NEVER) : dayClock(tape.lastClock())),
                x + w - PAD);
    }

    /* A row's cells at the small font, each clipped to the room before the next column. */
    private static void cells(final GuiGraphics g, final UiContext ctx, final ColumnHeader header, final int y,
                              final List<String> values, final int right) {
        final Font font = ctx.font();
        for (int i = 0; i < values.size(); i++) {
            final int cx = header.columnX(i);
            final int end = i + 1 < values.size() ? header.columnX(i + 1) - GAP / 2 : right;
            Texts.scaled(g, font, Texts.clip(font, values.get(i), (int) ((end - cx) / SMALL)), cx, y + 2, SMALL,
                    ctx.skin().text());
        }
    }

    /** One option of a choice, its button and its words at the small font, so a long place fits its column. */
    private static final class Option extends UiComponent {

        private final Supplier<String> label;
        private final BooleanSupplier chosen;
        private final Runnable onChoose;

        private static final int BUTTON = 8;

        Option(final Supplier<String> label, final BooleanSupplier chosen, final Runnable onChoose) {
            this.label = label;
            this.chosen = chosen;
            this.onChoose = onChoose;
        }

        @Override
        public void render(final GuiGraphics g, final UiContext ctx) {
            final boolean over = enabled() && ctx.over(x(), y(), width(), height());
            ctx.skin().radio(g, x(), y() + (height() - BUTTON) / 2, BUTTON, chosen.getAsBoolean(), over);
            final int fits = (int) ((width() - BUTTON - 4) / SMALL);
            ctx.skin().drawText(g, ctx.font(), Texts.trim(ctx.font(), label.get(), fits), x() + BUTTON + 4,
                    y() + 2, ctx.skin().text(), SMALL);
        }

        @Override
        public boolean mouseClicked(final double mx, final double my, final int button) {
            if (button != 0 || !contains(mx, my)) {
                return false;
            }
            onChoose.run();
            return true;
        }
    }

    /** A dialog with a title worked out when it opens, which Enter answers as its first button does. */
    private static final class TypedPopup extends Popup {

        private final Runnable onEnter;

        TypedPopup(final Supplier<String> title, final Runnable onEnter) {
            super(title, POPUP_W, POPUP_H);
            this.onEnter = onEnter;
        }

        @Override
        public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                onEnter.run();
                return true;
            }
            return super.keyPressed(key, scanCode, modifiers);
        }
    }
}
