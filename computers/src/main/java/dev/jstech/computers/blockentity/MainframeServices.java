/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.engine.EngineDef;
import dev.jstech.computers.engine.INetworkEngine;
import dev.jstech.computers.engine.NetworkEngines;
import dev.jstech.computers.program.IqlJobAgent;
import dev.jstech.computers.program.Programs;
import dev.jstech.computers.program.iql.IqlCatalog;
import dev.jstech.computers.program.iql.IqlDefinition;
import dev.jstech.computers.program.iql.IqlSavedObject;
import dev.jstech.core.text.LongText;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * The software a Mainframe has installed on it, and what that software holds.
 *
 * <p>Each piece is a thing a player installs, starts, stops and takes off again rather than a property of the
 * hardware: the Network Operations Engines, of which one at a time plans the network's work (a new Mainframe ships
 * with the Midsoft IQL Server installed and running, in the version of its age); the Automation Engine, which fires
 * the saved jobs; and the Mirror, which is where the packages players write are published so every Linux machine on
 * the network can install them.
 *
 * <p>They are together because they behave alike. Each is on or off, each only serves while the machine it is
 * on has power, each is written down with the machine, and all of them go when the disk is formatted, because
 * software on a formatted disk is what they are.
 */
final class MainframeServices {

    /** How many packages one network's Mirror will hold, so a shelf cannot grow without end. */
    static final int SHELF_MAX = 64;

    private final MainframeBlockEntity mainframe;

    /** The engines installed, by the package that installs each, with the version each was installed in. */
    private final Map<ResourceLocation, String> engines = new LinkedHashMap<>();

    /** The engine that plans the network's work, one of those installed; {@code null} leaves the network without. */
    @Nullable
    private ResourceLocation activeEngine;

    /** Whether the active engine is started; a stopped one plans nothing, as if there were none. */
    private boolean engineRunning = true;

    /** A replacement of the engine under way, or {@code null}; while one is, the network has no engine. */
    @Nullable
    private EngineReplacement replacement;

    /** The game time the running engine came up at, or {@code -1} while none runs. */
    private long engineStartedAt = -1L;

    /** The game time of the last tick, so a Mainframe that was off or unloaded starts its engine's clock again. */
    private long lastTickAt = -1L;

    /** The day, in game days, that {@link #plansToday} counts for. */
    private long plansDay = -1L;

    /** How many requests the engine planned on {@link #plansDay}. */
    private int plansToday;

    /** What each engine keeps on this Mainframe of its own, by the package that installs it. */
    private final Map<ResourceLocation, CompoundTag> engineData = new LinkedHashMap<>();

    /**
     * The saved views, procedures and jobs. It is kept whether or not the Engine is installed, because
     * taking the Engine off is not the same as throwing away what somebody wrote with it.
     */
    private final IqlCatalog catalog = new IqlCatalog();

    private final IqlJobAgent jobAgent = new IqlJobAgent();

    /** Jobs the player paused from the Processes tab (lowercased names); a paused job never fires. */
    private final Set<String> pausedJobs = new HashSet<>();

    /**
     * The packages players on this network have published, by name.
     *
     * <p>They live with the Mainframe, not with the machine that built them: that is what a Mirror is for.
     * Each is the whole package as text, so what a player installs is exactly what the player who published
     * it could read on their own screen.
     */
    private final Map<String, String> shelved = new LinkedHashMap<>();

    private boolean automationEngineInstalled;
    private boolean mirrorInstalled;

    /** Each service by the program that installs it, the engines aside: those are looked up as they are asked for. */
    private final Map<ResourceLocation, IMainframeService> byProgram = Map.of(
            Programs.AUTOMATION_ENGINE, new Service(this::automationEngineInstalled, this::automationEngineActive,
                    this::installAutomationEngine, this::uninstallAutomationEngine),
            Programs.MIRROR, new Service(this::mirrorInstalled, this::mirrorActive,
                    this::installMirror, this::uninstallMirror));

    MainframeServices(final MainframeBlockEntity mainframe) {
        this.mainframe = mainframe;
        shipFactoryEngine();
    }

    /** The service that program installs, or {@code null} when it is not one a Mainframe runs. */
    @Nullable
    IMainframeService service(final ResourceLocation programId) {
        if (NetworkEngines.isEngine(programId)) {
            return new Service(() -> engines.containsKey(programId),
                    () -> programId.equals(activeEngine) && runningEngine() != null,
                    () -> installEngine(programId), () -> uninstallEngine(programId));
        }
        return byProgram.get(programId);
    }

    /** The engines installed, by the package that installs each, with the version each was installed in. */
    Map<ResourceLocation, String> installedEngines() {
        return Collections.unmodifiableMap(engines);
    }

    /** What the engine installed by {@code program} keeps of its own, made empty the first time it is asked for. */
    CompoundTag engineData(final ResourceLocation program) {
        return engineData.computeIfAbsent(program, id -> new CompoundTag());
    }

    /** The engine chosen to plan the network's work, started or not; {@code null} when none is. */
    @Nullable
    ResourceLocation activeEngine() {
        return activeEngine;
    }

    boolean engineRunning() {
        return engineRunning;
    }

    /**
     * The engine planning the network's work now: chosen, started, not being replaced, and on a Mainframe that is
     * running.
     */
    @Nullable
    INetworkEngine runningEngine() {
        if (activeEngine == null || !engineRunning || replacement != null || !mainframe.isRunning()) {
            return null;
        }
        return NetworkEngines.get(activeEngine);
    }

    /** The replacement of the engine under way, or {@code null} when there is none. */
    @Nullable
    EngineReplacement replacement() {
        return replacement;
    }

    /**
     * Begins replacing the network's engine with {@code target}, which has to be installed; false when it is not,
     * when it is the engine running already, when a replacement is under way, or when the Mainframe is off. The
     * network has no engine until the replacement is over.
     */
    boolean replaceEngine(final ResourceLocation target, final long now, final int inFlight, final int itemTypes) {
        if (replacement != null || !engines.containsKey(target) || !mainframe.isRunning()
                || target.equals(activeEngine) && engineRunning) {
            return false;
        }
        replacement = EngineReplacement.begin(activeEngine, target, now, inFlight, itemTypes);
        mainframe.setChanged();
        return true;
    }

    /** How long the running engine has been up at {@code now}, in ticks; none when no engine runs. */
    long engineUpTicks(final long now) {
        return engineStartedAt < 0 || runningEngine() == null ? 0L : Math.max(0L, now - engineStartedAt);
    }

    /** Counts one request the engine planned, on game day {@code day}. */
    void notePlanned(final long day) {
        if (day != plansDay) {
            plansDay = day;
            plansToday = 0;
        }
        plansToday++;
        mainframe.setChanged();
    }

    /** How many requests the engine planned on game day {@code day}. */
    int plansToday(final long day) {
        return day == plansDay ? plansToday : 0;
    }

    /**
     * Installs an engine in the version made for this Mainframe's age, and makes it the active one when the network
     * has none; false when it is installed already, is no engine, or was not made for a Mainframe of this age.
     */
    boolean installEngine(final ResourceLocation program) {
        final EngineDef def = NetworkEngines.def(program);
        final String version = def == null ? null : def.versionFor(mainframe.mainframeEra());
        if (version == null || engines.containsKey(program)) {
            return false;
        }
        engines.put(program, version);
        if (activeEngine == null) {
            activeEngine = program;
            engineRunning = true;
        }
        mainframe.setChanged();
        return true;
    }

    /** Takes an engine off; when it was the active one, the network is left without. False when not installed. */
    boolean uninstallEngine(final ResourceLocation program) {
        if (engines.remove(program) == null) {
            return false;
        }
        if (program.equals(activeEngine)) {
            activeEngine = null;
        }
        /*
         * Taking off the engine a replacement was bringing up ends the replacement; the engine it was replacing was
         * stopped on the way, and stays chosen and stopped until somebody starts it again.
         */
        if (replacement != null && program.equals(replacement.to())) {
            replacement = null;
            engineRunning = false;
        }
        mainframe.setChanged();
        return true;
    }

    /**
     * Makes an installed engine the one that plans the network's work, and starts it; false when it is not installed
     * or is that one already, started, or while a replacement is under way. What the engine before it planned
     * carries on: an Operation belongs to the network once it is made, not to the engine that made it.
     */
    boolean activateEngine(final ResourceLocation program) {
        if (replacement != null || !engines.containsKey(program) || program.equals(activeEngine) && engineRunning) {
            return false;
        }
        activeEngine = program;
        engineRunning = true;
        mainframe.setChanged();
        return true;
    }

    /** Starts or stops the active engine; false when there is none, nothing to change, or a replacement is on. */
    boolean setEngineRunning(final boolean running) {
        if (replacement != null || activeEngine == null || engineRunning == running) {
            return false;
        }
        engineRunning = running;
        mainframe.setChanged();
        return true;
    }

    IqlCatalog catalog() {
        return catalog;
    }

    /**
     * Runs the jobs whose moment has come, brings up the engine a replacement was bringing up once its time is
     * over, and keeps the running engine's clock.
     */
    void tick(final ServerLevel level) {
        final long now = level.getGameTime();
        if (replacement != null && replacement.done(now)) {
            activeEngine = replacement.to();
            engineRunning = true;
            replacement = null;
            engineStartedAt = now;
            mainframe.setChanged();
        }
        // A gap since the last tick means the Mainframe was off or unloaded, and its engine was down with it.
        if (runningEngine() == null) {
            engineStartedAt = -1L;
        } else if (engineStartedAt < 0 || now - lastTickAt > 1) {
            engineStartedAt = now;
        }
        lastTickAt = now;
        jobAgent.tick(mainframe, level);
        // The engine that plans the network's work gets its turn too: one that holds states reacts here.
        final INetworkEngine engine = runningEngine();
        if (engine != null) {
            engine.tick(mainframe, level);
        }
    }

    boolean automationEngineInstalled() {
        return automationEngineInstalled;
    }

    /** Active when installed and the Mainframe is powered; lets the job agent fire, as the Engine does. */
    boolean automationEngineActive() {
        return automationEngineInstalled && mainframe.isRunning();
    }

    boolean installAutomationEngine() {
        if (automationEngineInstalled) {
            return false;
        }
        automationEngineInstalled = true;
        mainframe.setChanged();
        return true;
    }

    boolean uninstallAutomationEngine() {
        if (!automationEngineInstalled) {
            return false;
        }
        automationEngineInstalled = false;
        mainframe.setChanged();
        return true;
    }

    boolean mirrorInstalled() {
        return mirrorInstalled;
    }

    /** Whether the Mirror serves packages: installed and the Mainframe is running. */
    boolean mirrorActive() {
        return mirrorInstalled && mainframe.isRunning();
    }

    boolean installMirror() {
        if (mirrorInstalled) {
            return false;
        }
        mirrorInstalled = true;
        mainframe.setChanged();
        return true;
    }

    boolean uninstallMirror() {
        if (!mirrorInstalled) {
            return false;
        }
        mirrorInstalled = false;
        mainframe.setChanged();
        return true;
    }

    /** Everything on the shelf, by name. */
    Map<String, String> shelvedPackages() {
        return Map.copyOf(shelved);
    }

    /** One of them, or null. */
    @Nullable
    String shelvedPackage(final String name) {
        return shelved.get(name);
    }

    /**
     * Puts one on the shelf, replacing any build of it already there.
     *
     * <p>Replacing rather than refusing is deliberate: publishing again is how a player releases a fix, and
     * making them take the old one down first would only mean a moment when the network has none.
     */
    boolean shelve(final String name, final String text) {
        if (name == null || name.isBlank() || text == null || text.isBlank()) {
            return false;
        }
        if (!shelved.containsKey(name) && shelved.size() >= SHELF_MAX) {
            return false;
        }
        shelved.put(name, text);
        mainframe.setChanged();
        return true;
    }

    /** Takes one off the shelf; false when it was not there. */
    boolean unshelve(final String name) {
        if (shelved.remove(name) == null) {
            return false;
        }
        mainframe.setChanged();
        return true;
    }

    boolean jobPaused(final String jobName) {
        return pausedJobs.contains(jobName.toLowerCase(Locale.ROOT));
    }

    /**
     * Pauses a job (a resumable end): the agent stops firing it until it is restarted. Only a job the catalog holds
     * is paused: the name comes from a player's screen, and a list of names nothing answers to would only grow.
     */
    void pauseJob(final String jobName) {
        if (catalog.contains(IqlDefinition.ObjectType.JOB, jobName)
                && pausedJobs.add(jobName.toLowerCase(Locale.ROOT))) {
            mainframe.setChanged();
        }
    }

    /** Restarts a job: resumes it if paused and re-arms its trigger so it reschedules from now. */
    void restartJob(final String jobName) {
        pausedJobs.remove(jobName.toLowerCase(Locale.ROOT));
        jobAgent.rearm(jobName);
        mainframe.setChanged();
    }


    /**
     * Takes every one off, the engines too, which is what formatting the disk they were on does.
     *
     * <p>What they held is not thrown away with them: the catalog, the shelf and what each engine kept are still
     * there if the same services are installed again, the way the files on a second disk would be.
     */
    void eraseInstalls() {
        byProgram.values().forEach(IMainframeService::uninstall);
        engines.clear();
        activeEngine = null;
        replacement = null;
        mainframe.setChanged();
    }

    void save(final CompoundTag tag) {
        final CompoundTag installed = new CompoundTag();
        engines.forEach((program, version) -> installed.putString(program.toString(), version));
        tag.put("Engines", installed);
        if (activeEngine != null) {
            tag.putString("ActiveEngine", activeEngine.toString());
        }
        tag.putBoolean("EngineRunning", engineRunning);
        if (replacement != null) {
            tag.put("EngineReplacement", replacement.save());
        }
        tag.putLong("EngineStartedAt", engineStartedAt);
        tag.putLong("EngineLastTick", lastTickAt);
        tag.putLong("PlansDay", plansDay);
        tag.putInt("PlansToday", plansToday);
        tag.putBoolean("AutomationEngineInstalled", automationEngineInstalled);
        tag.putBoolean("MirrorInstalled", mirrorInstalled);
        if (!shelved.isEmpty()) {
            final CompoundTag shelf = new CompoundTag();
            shelved.forEach(shelf::putString);
            tag.put("MirrorShelf", shelf);
        }
        if (!catalog.isEmpty()) {
            final ListTag objects = new ListTag();
            for (final IqlSavedObject object : catalog.all()) {
                final CompoundTag entry = new CompoundTag();
                entry.putByte("Type", (byte) object.type().id());
                entry.putString("Name", object.name());
                LongText.put(entry, "Body", object.body());
                entry.putByte("Trigger", (byte) object.triggerKind().id());
                entry.putString("Spec", object.triggerSpec());
                objects.add(entry);
            }
            tag.put("IqlCatalog", objects);
        }
        // A job dropped from the catalog since it was paused is left out, so the list never outgrows the jobs.
        pausedJobs.removeIf(name -> !catalog.contains(IqlDefinition.ObjectType.JOB, name));
        if (!pausedJobs.isEmpty()) {
            final ListTag paused = new ListTag();
            for (final String name : pausedJobs) {
                paused.add(StringTag.valueOf(name));
            }
            tag.put("PausedJobs", paused);
        }
        final CompoundTag kept = new CompoundTag();
        engineData.forEach((program, data) -> {
            if (!data.isEmpty()) {
                kept.put(program.toString(), data.copy());
            }
        });
        if (!kept.isEmpty()) {
            tag.put("EngineData", kept);
        }
    }

    void load(final CompoundTag tag) {
        /*
         * A Mainframe saved before engines were software it holds has no such line; it keeps the engine it ships
         * with, the way a new one does.
         */
        if (tag.contains("Engines")) {
            engines.clear();
            final CompoundTag installed = tag.getCompound("Engines");
            for (final String program : installed.getAllKeys()) {
                final ResourceLocation id = ResourceLocation.tryParse(program);
                if (id != null) {
                    engines.put(id, installed.getString(program));
                }
            }
            final ResourceLocation active = ResourceLocation.tryParse(tag.getString("ActiveEngine"));
            activeEngine = active != null && engines.containsKey(active) ? active : null;
            engineRunning = tag.getBoolean("EngineRunning");
        }
        final EngineReplacement saved = tag.contains("EngineReplacement")
                ? EngineReplacement.load(tag.getCompound("EngineReplacement")) : null;
        replacement = saved != null && engines.containsKey(saved.to()) ? saved : null;
        engineStartedAt = tag.contains("EngineStartedAt") ? tag.getLong("EngineStartedAt") : -1L;
        lastTickAt = tag.contains("EngineLastTick") ? tag.getLong("EngineLastTick") : -1L;
        plansDay = tag.contains("PlansDay") ? tag.getLong("PlansDay") : -1L;
        plansToday = tag.getInt("PlansToday");
        automationEngineInstalled = tag.getBoolean("AutomationEngineInstalled");
        mirrorInstalled = tag.getBoolean("MirrorInstalled");
        engineData.clear();
        final CompoundTag kept = tag.getCompound("EngineData");
        for (final String program : kept.getAllKeys()) {
            final ResourceLocation id = ResourceLocation.tryParse(program);
            if (id != null) {
                engineData.put(id, kept.getCompound(program).copy());
            }
        }
        shelved.clear();
        final CompoundTag shelf = tag.getCompound("MirrorShelf");
        for (final String name : shelf.getAllKeys()) {
            shelved.put(name, shelf.getString(name));
        }
        catalog.clear();
        final ListTag objects = tag.getList("IqlCatalog", Tag.TAG_COMPOUND);
        for (int i = 0; i < objects.size(); i++) {
            final CompoundTag entry = objects.getCompound(i);
            catalog.put(new IqlSavedObject(
                    IqlDefinition.ObjectType.byId(entry.getByte("Type")),
                    entry.getString("Name"), LongText.get(entry, "Body"),
                    IqlDefinition.TriggerKind.byId(entry.getByte("Trigger")),
                    entry.getString("Spec")));
        }
        pausedJobs.clear();
        final ListTag paused = tag.getList("PausedJobs", Tag.TAG_STRING);
        for (int i = 0; i < paused.size(); i++) {
            pausedJobs.add(paused.getString(i));
        }
    }

    /**
     * What a Mainframe comes with, whatever its age and its system: the Midsoft IQL Server, in the version of its
     * age, installed, chosen and started, so a new network works the moment it is built.
     */
    private void shipFactoryEngine() {
        final EngineDef midsoft = NetworkEngines.MIDSOFT_IQL_SERVER;
        final String version = midsoft.versionFor(mainframe.mainframeEra());
        if (version != null) {
            engines.put(midsoft.program(), version);
            activeEngine = midsoft.program();
            engineRunning = true;
        }
    }

    /** One service's four answers, read off the flags this class keeps for it. */
    private record Service(BooleanSupplier isInstalled, BooleanSupplier isActive, BooleanSupplier installer,
                           BooleanSupplier uninstaller) implements IMainframeService {

        @Override
        public boolean installed() {
            return isInstalled.getAsBoolean();
        }

        @Override
        public boolean active() {
            return isActive.getAsBoolean();
        }

        @Override
        public boolean install() {
            return installer.getAsBoolean();
        }

        @Override
        public boolean uninstall() {
            return uninstaller.getAsBoolean();
        }
    }
}
