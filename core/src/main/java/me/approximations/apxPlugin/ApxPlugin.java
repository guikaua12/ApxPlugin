package me.approximations.apxPlugin;

import com.google.common.base.Stopwatch;
import lombok.Getter;
import me.approximations.apxPlugin.dependencyInjection.manager.DependencyManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public abstract class ApxPlugin extends JavaPlugin {
    private final List<Runnable> disableEntries = new ArrayList<>();

    @Getter
    private Reflections reflections;
    @Getter
    private DependencyManager dependencyManager;

    @Override
    public void onEnable() {
        final Stopwatch stopwatch = Stopwatch.createStarted();

        try {
            this.reflections = new Reflections(getClass().getPackage().getName(), new SubTypesScanner(), new TypeAnnotationsScanner());

            dependencyManager = new DependencyManager(reflections);

            onPluginEnable();

            getLogger().info(String.format("Plugin enabled successfully! (%s)", stopwatch.stop()));
        } catch (Throwable t) {
            getLogger().log(Level.SEVERE, "An error happened while enabling.");

            t.printStackTrace();
        } finally {
            if (stopwatch.isRunning()) {
                stopwatch.stop();
            }
        }
    }

    protected void onPluginEnable() {
    }

    @Override
    public void onDisable() {
        for (Runnable runnable : disableEntries) {
            runnable.run();
        }
    }

    public void addDisableEntry(Runnable runnable) {
        disableEntries.add(runnable);
    }
}