/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.recipeview;

import dev.jstech.core.client.machine.PictureFace;
import dev.jstech.core.client.machine.StandardFace;
import dev.jstech.core.machine.Byproduct;
import dev.jstech.core.machine.FacePicture;
import dev.jstech.core.machine.MachineScreenLayout;
import dev.jstech.core.machine.MachineScreenTexts;
import dev.jstech.core.machine.MachineTexts;
import dev.jstech.core.machine.MatterTanks;
import dev.jstech.core.machine.ProcessingMachineBlockEntity;
import dev.jstech.core.machine.ProcessingRecipe;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.core.recipeview.RecipeView;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import org.jetbrains.annotations.ApiStatus;

/**
 * The machines' own faces the recipe viewers lay pages on. A kind of processing whose machine has a face drawn as a
 * picture ({@link PictureFace}) has each recipe it holds laid in that face's ports and sight glasses and drawn on the
 * face at work, its loop playing, as the machine draws itself, rather than on a viewer's plain page. A mod registers
 * each kind's face from its client setup; a recipe the face cannot hold, or one that needs knowledge or conditions its
 * face has no place to say, keeps the plain page.
 */
public final class RecipeFaces {

    /* Each kind's faces, by the kind's id, in the order they were registered; and each face, by its frames. */
    private static final Map<ResourceLocation, List<Shown>> BY_KIND = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, RecipeFace> BY_FRAMES = new ConcurrentHashMap<>();
    /* How full the sight glasses of a page are drawn, and how far up its scale the meter stands. */
    private static final double FULL = 0.6;
    private static final int TICKS_A_SECOND = 20;
    /* How far round a sight glass its tooltip reaches: over its brackets. */
    private static final int GLASS_REACH_X = 3;
    private static final int GLASS_REACH_Y = 4;

    private RecipeFaces() {
    }

    /** Lays the recipes of the kind {@code kind} it holds on {@code face}, from a mod's client setup. */
    public static void register(final ResourceLocation kind, final RecipeFace face) {
        register(kind, face, recipe -> true);
    }

    /**
     * Lays the recipes of the kind {@code kind} that {@code shows} picks, and that it holds, on {@code face}, before
     * any face registered after it: a machine drawn one way for some of its work and another for the rest.
     */
    public static void register(final ResourceLocation kind, final RecipeFace face,
                                final Predicate<ProcessingRecipe> shows) {
        Objects.requireNonNull(face, "face");
        BY_KIND.computeIfAbsent(Objects.requireNonNull(kind, "kind"), each -> new CopyOnWriteArrayList<>())
                .add(new Shown(face, Objects.requireNonNull(shows, "shows")));
        BY_FRAMES.put(face.frames(), face);
    }

    /** The kinds that have a face, in no order. */
    public static Set<ResourceLocation> kinds() {
        return Set.copyOf(BY_KIND.keySet());
    }

    /** The face {@code recipe} of the kind {@code kind} is laid on: the first registered that picks and holds it. */
    public static Optional<RecipeFace> of(final ResourceLocation kind, final ProcessingRecipe recipe) {
        return BY_KIND.getOrDefault(kind, List.of()).stream().filter(shown -> shown.shows().test(recipe)
                && shown.face().holds(recipe)).map(Shown::face).findFirst();
    }

    /** How wide a page laid on a face is: the face itself. */
    public static int width() {
        return MachineScreenLayout.FACE_WIDTH;
    }

    /** How tall a page laid on a face is. */
    public static int height() {
        return MachineScreenLayout.FACE_HEIGHT;
    }

    /**
     * Lays {@code recipe} on {@code face}, which {@linkplain RecipeFace#holds holds} it: its inputs and then what it
     * needs but keeps in the face's input ports, in the order of their slots; its outputs and byproducts in its output
     * ports; its fluids in the sight glasses of the tanks that take and give them; its matter in those of the tanks of
     * its state where the face has them; and on its LCD that it runs, and for how long.
     */
    public static void lay(final RecipeFace face, final ProcessingRecipe recipe, final RecipeView.Builder page) {
        final Placed placed = face.placed(recipe).orElseThrow(() -> new IllegalArgumentException("the face "
                + face.frames() + " has no room for " + recipe));
        int at = 0;
        for (final SizedIngredient input : recipe.inputs()) {
            final FacePicture.Port port = placed.inputs().get(at++);
            page.input(port.x(), port.y(), List.of(input.getItems()));
        }
        for (final SizedIngredient catalyst : recipe.catalysts()) {
            final FacePicture.Port port = placed.inputs().get(at++);
            page.catalyst(port.x(), port.y(), List.of(catalyst.getItems())).tooltip(ProcessingRecipeViews.catalyst());
        }
        at = 0;
        for (final ItemStack output : recipe.outputs()) {
            final FacePicture.Port port = placed.outputs().get(at++);
            page.output(port.x(), port.y(), output);
        }
        for (final Byproduct byproduct : recipe.byproducts()) {
            final FacePicture.Port port = placed.outputs().get(at++);
            ProcessingRecipeViews.byproduct(page, port.x(), port.y(), byproduct);
        }
        for (int k = 0; k < recipe.fluidInputs().size(); k++) {
            final SizedFluidIngredient fluid = recipe.fluidInputs().get(k);
            fluid(page, placed.fluidInputs().get(k), List.of(fluid.getFluids()), fluid.amount(), true);
        }
        for (int k = 0; k < recipe.fluidOutputs().size(); k++) {
            final FluidStack fluid = recipe.fluidOutputs().get(k);
            fluid(page, placed.fluidOutputs().get(k), List.of(fluid), fluid.getAmount(), false);
        }
        final Map<Integer, MatterStack> matter = new HashMap<>();
        for (int k = 0; k < recipe.matterInputs().size(); k++) {
            matter.put(placed.matterInputs().get(k).tank(), recipe.matterInputs().get(k));
        }
        for (int k = 0; k < recipe.matterOutputs().size(); k++) {
            matter.put(placed.matterOutputs().get(k).tank(), recipe.matterOutputs().get(k));
        }
        // Its words are said as the page is laid, which a viewer does each time it shows it.
        final Map<String, String> said = new HashMap<>();
        said.put(face.name(), face.say().apply(recipe, face.name()));
        for (final FacePicture.Word word : face.picture().words()) {
            said.put(word.text(), face.say().apply(recipe, word.text()));
        }
        page.face(new RecipeView.Face(face.frames(), List.of(lcd(GameText.resolve(MachineScreenTexts.LCD_WORKING)),
                lcd(GameText.resolve(MachineTexts.RECIPE_TIME.with(Text.literal(seconds(recipe.ticks())))))), said,
                matter));
    }

    /**
     * Draws the face {@code page} is laid on, its top left at ({@code x}, {@code y}), if it is laid on one: the machine
     * at work, its loop playing by the clock, its plates, its RUN lamp lit and its switch on, its LCD saying what the
     * page says, its sight glasses of matter filled with each matter's colour and those of fluids empty under the
     * fluids the viewer draws in them, each glass named as what it holds is.
     */
    public static void draw(final GuiGraphics g, final RecipeView page, final int x, final int y) {
        final Optional<RecipeView.Face> laid = page.face();
        final Optional<RecipeFace> face = laid.map(each -> BY_FRAMES.get(each.id()));
        if (face.isEmpty()) {
            return;
        }
        final FacePicture picture = face.get().picture();
        final List<Optional<PictureFace.Fill>> fills = new ArrayList<>();
        for (final FacePicture.Glass glass : picture.glasses()) {
            final MatterStack held = laid.get().matter().get(glass.tank());
            if (held != null) {
                fills.add(Optional.of(new PictureFace.Fill(0xFF << 24 | held.material().colour(), FULL,
                        StandardFace.written(held.material()))));
            } else {
                fills.add(page.slots().stream().filter(slot -> slot.holdsFluid() && slot.x() == frameX(glass)
                        && slot.y() == frameY(glass)).findFirst().map(slot -> new PictureFace.Fill(0, 0,
                        StandardFace.written(slot.fluids().getFirst()))));
            }
        }
        final Map<String, String> said = laid.get().said();
        final List<String> lcd = laid.get().lcd().stream().map(Component::getString).toList();
        PictureFace.drawStill(g, Minecraft.getInstance().font, x, y, face.get().frames(), picture,
                new PictureFace.Still(said.getOrDefault(face.get().name(), face.get().name()),
                        face.get().rating().get(), lcd, FULL, fills), written -> said.getOrDefault(written, written));
    }

    /**
     * What the face {@code page} is laid on says where the viewer says nothing: over each sight glass of matter, how
     * much of which matter it takes or gives. The viewer says what its slots hold.
     */
    public static List<Note> notes(final RecipeView page) {
        final Optional<RecipeView.Face> laid = page.face();
        final Optional<RecipeFace> face = laid.map(each -> BY_FRAMES.get(each.id()));
        if (face.isEmpty()) {
            return List.of();
        }
        final List<Tank> tanks = face.get().tanks();
        final List<Note> notes = new ArrayList<>();
        for (final FacePicture.Glass glass : face.get().picture().glasses()) {
            final MatterStack held = laid.get().matter().get(glass.tank());
            if (held != null) {
                final TextKey says = tanks.get(glass.tank()).taking() ? MachineTexts.RECIPE_MATTER_IN
                        : MachineTexts.RECIPE_MATTER_OUT;
                notes.add(new Note(glass.x() - GLASS_REACH_X, glass.y() - GLASS_REACH_Y, glass.w() + 2 * GLASS_REACH_X,
                        glass.h() + 2 * GLASS_REACH_Y, List.of(GameText.component(says.with(Text.literal(
                        String.valueOf(held.amount())), held.material().name())))));
            }
        }
        return notes;
    }

    /**
     * Lays {@code amount} millibuckets of any of {@code fluids} in {@code glass} of a face, taken or given as
     * {@code taking} says, drawn as full as a working machine's glasses stand: for a page a mod lays on a machine's
     * face itself, as {@link #lay} lays a recipe's fluids.
     */
    public static void fluid(final RecipeView.Builder page, final FacePicture.Glass glass,
                             final List<FluidStack> fluids, final int amount, final boolean taking) {
        if (taking) {
            page.fluidInput(frameX(glass), frameY(glass), fluids, capacity(amount));
        } else {
            page.fluidOutput(frameX(glass), frameY(glass), fluids.getFirst(), capacity(amount));
        }
        page.sized(shownWidth(glass), shownHeight(glass));
    }

    /**
     * Whether the tank {@code tank} of the machine whose face {@code page} is laid on takes what it holds rather than
     * gives it: what a page's matter is to its recipe. None for a page on no face, or a tank the face has not.
     */
    @ApiStatus.Internal
    public static Optional<Boolean> taking(final RecipeView page, final int tank) {
        return page.face().map(each -> BY_FRAMES.get(each.id())).filter(face -> tank >= 0
                && tank < face.tanks().size()).map(face -> face.tanks().get(tank).taking());
    }

    /* How much of a fluid fills its glass, drawn as full as a working machine's glasses stand. */
    private static int capacity(final int amount) {
        return (int) Math.ceil(amount / FULL);
    }

    /* A glass's frame, a pixel round what it shows: a slim glass shows its inside, a bracketed one all its glass. */
    private static int frameX(final FacePicture.Glass glass) {
        return glass.slim() ? glass.x() : glass.x() - 1;
    }

    private static int frameY(final FacePicture.Glass glass) {
        return glass.slim() ? glass.y() : glass.y() - 1;
    }

    private static int shownWidth(final FacePicture.Glass glass) {
        return glass.slim() ? glass.w() - 2 : glass.w();
    }

    private static int shownHeight(final FacePicture.Glass glass) {
        return glass.slim() ? glass.h() - 2 : glass.h();
    }

    /* A recipe's time in seconds, to a tenth where it is not whole. */
    private static String seconds(final int ticks) {
        return ticks % TICKS_A_SECOND == 0 ? Integer.toString(ticks / TICKS_A_SECOND)
                : String.format(Locale.ROOT, "%.1f", ticks / (double) TICKS_A_SECOND);
    }

    /* A line of the LCD, in its capitals. */
    private static Component lcd(final String words) {
        return Component.literal(words.toUpperCase(Locale.ROOT));
    }

    /**
     * A kind's face.
     *
     * @param frames  the picture's frames, as the machine's own screen draws them
     * @param picture where on the face its ports, glasses, plates, meter, LCD and switch stand
     * @param layout  which of the machine's slots take and which give, and its tanks of fluids and of matter
     * @param name    what its name plate writes, before {@code say} says it
     * @param rating  what its class plate writes, in the player's language
     * @param say     what a word written on the picture says on a recipe's page: its translation, or what the recipe
     *                makes of it
     */
    public record RecipeFace(ResourceLocation frames, FacePicture picture, ProcessingMachineBlockEntity.Layout layout,
                             String name, Supplier<String> rating, BiFunction<ProcessingRecipe, String, String> say) {

        public RecipeFace {
            Objects.requireNonNull(frames, "frames");
            Objects.requireNonNull(picture, "picture");
            Objects.requireNonNull(layout, "layout");
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(rating, "rating");
            Objects.requireNonNull(say, "say");
        }

        /**
         * Whether the face holds {@code recipe}: a port for each of its items and a sight glass for each of its fluids
         * and its matter, and no knowledge nor conditions it would have to say beside the face.
         */
        public boolean holds(final ProcessingRecipe recipe) {
            return placed(recipe).isPresent();
        }

        /* Where each of what {@code recipe} takes and makes stands on the face, if the face has room for it all. */
        private Optional<Placed> placed(final ProcessingRecipe recipe) {
            final List<FacePicture.Port> inputs = ports(true);
            final List<FacePicture.Port> outputs = ports(false);
            if (recipe.knowledge().isPresent() || !recipe.conditions().any()
                    || recipe.inputs().size() + recipe.catalysts().size() > inputs.size()
                    || recipe.outputs().size() + recipe.byproducts().size() > outputs.size()) {
                return Optional.empty();
            }
            final List<FacePicture.Glass> free = new ArrayList<>(this.picture.glasses().stream()
                    .sorted(Comparator.comparingInt(FacePicture.Glass::tank)).toList());
            final Optional<List<FacePicture.Glass>> fluidInputs = take(free, Collections.nCopies(
                    recipe.fluidInputs().size(), fluid(true)), fluid(true));
            final Optional<List<FacePicture.Glass>> fluidOutputs = take(free, Collections.nCopies(
                    recipe.fluidOutputs().size(), fluid(false)), fluid(false));
            final Optional<List<FacePicture.Glass>> matterInputs = take(free, recipe.matterInputs().stream()
                    .map(stack -> matter(true, stack)).toList(), anyMatter(true));
            final Optional<List<FacePicture.Glass>> matterOutputs = take(free, recipe.matterOutputs().stream()
                    .map(stack -> matter(false, stack)).toList(), anyMatter(false));
            if (fluidInputs.isEmpty() || fluidOutputs.isEmpty() || matterInputs.isEmpty() || matterOutputs.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new Placed(inputs, outputs, fluidInputs.get(), fluidOutputs.get(), matterInputs.get(),
                    matterOutputs.get()));
        }

        /* Its ports of the slots that take, or of those that give, in the order of their slots. */
        private List<FacePicture.Port> ports(final boolean taking) {
            return this.picture.ports().stream().filter(port -> taking ? this.layout.isInput(port.slot())
                    : this.layout.isOutput(port.slot())).sorted(Comparator.comparingInt(FacePicture.Port::slot))
                    .toList();
        }

        /*
         * Its machine's tanks, in the order a face's glasses count them: those of fluids taking and then giving, and
         * those of matter, each state's in the order of the states, taking and then giving.
         */
        private List<Tank> tanks() {
            final List<Tank> out = new ArrayList<>();
            out.addAll(Collections.nCopies(this.layout.inputTanks(), new Tank(true, Optional.empty())));
            out.addAll(Collections.nCopies(this.layout.outputTanks(), new Tank(false, Optional.empty())));
            for (final MatterState state : MatterState.values()) {
                final List<MatterTanks.MatterSpec> specs = this.layout.matter().stream()
                        .filter(spec -> spec.state() == state).toList();
                out.addAll(Collections.nCopies(specs.stream().mapToInt(MatterTanks.MatterSpec::inputs).sum(),
                        new Tank(true, Optional.of(state))));
                out.addAll(Collections.nCopies(specs.stream().mapToInt(MatterTanks.MatterSpec::outputs).sum(),
                        new Tank(false, Optional.of(state))));
            }
            return out;
        }

        /*
         * A glass of {@code free} for each of {@code wanted}: the first whose tank it fits, else the first whose tank
         * {@code also} fits, each taken from {@code free}; none when one of them finds no glass.
         */
        private Optional<List<FacePicture.Glass>> take(final List<FacePicture.Glass> free,
                                                       final List<Predicate<Tank>> wanted,
                                                       final Predicate<Tank> also) {
            final List<Tank> tanks = tanks();
            final List<FacePicture.Glass> out = new ArrayList<>();
            for (final Predicate<Tank> fits : wanted) {
                final Optional<FacePicture.Glass> glass = first(free, tanks, fits).or(() -> first(free, tanks, also));
                if (glass.isEmpty()) {
                    return Optional.empty();
                }
                free.remove(glass.get());
                out.add(glass.get());
            }
            return Optional.of(out);
        }

        /* The first of {@code free} whose tank {@code fits}. */
        private static Optional<FacePicture.Glass> first(final List<FacePicture.Glass> free, final List<Tank> tanks,
                                                         final Predicate<Tank> fits) {
            return free.stream().filter(glass -> glass.tank() >= 0 && glass.tank() < tanks.size()
                    && fits.test(tanks.get(glass.tank()))).findFirst();
        }

        /* A tank of fluids that takes, or that gives. */
        private static Predicate<Tank> fluid(final boolean taking) {
            return tank -> tank.taking() == taking && tank.state().isEmpty();
        }

        /* A tank of matter that takes, or that gives, of the state {@code stack}'s material is in at room. */
        private static Predicate<Tank> matter(final boolean taking, final MatterStack stack) {
            return tank -> tank.taking() == taking && tank.state().equals(Optional.of(stack.material().roomState()));
        }

        /* A tank of matter of any state that takes, or that gives. */
        private static Predicate<Tank> anyMatter(final boolean taking) {
            return tank -> tank.taking() == taking && tank.state().isPresent();
        }
    }

    /**
     * What a face says over part of it, in pixels from the page's top left.
     *
     * @param x      its left
     * @param y      its top
     * @param width  how wide it reaches
     * @param height how tall it reaches
     * @param lines  what it says
     */
    public record Note(int x, int y, int width, int height, List<Component> lines) {

        public Note {
            lines = List.copyOf(lines);
        }

        /** Whether ({@code atX}, {@code atY}) is within it. */
        public boolean covers(final double atX, final double atY) {
            return MachineScreenLayout.on(atX, atY, this.x, this.y, this.width, this.height);
        }
    }

    /*
     * A tank of a machine: whether it takes or gives, and the state of its matter, none for a tank of fluids.
     */
    private record Tank(boolean taking, Optional<MatterState> state) {
    }

    /* A face of a kind, and the kind's recipes it shows of those it holds. */
    private record Shown(RecipeFace face, Predicate<ProcessingRecipe> shows) {
    }

    /*
     * Where a recipe stands on a face: the ports of its items in and its catalysts, of its items out and its
     * byproducts, and the glasses of its fluids and its matter in and out, each in the order the recipe lists them.
     */
    private record Placed(List<FacePicture.Port> inputs, List<FacePicture.Port> outputs,
                          List<FacePicture.Glass> fluidInputs, List<FacePicture.Glass> fluidOutputs,
                          List<FacePicture.Glass> matterInputs, List<FacePicture.Glass> matterOutputs) {
    }
}
