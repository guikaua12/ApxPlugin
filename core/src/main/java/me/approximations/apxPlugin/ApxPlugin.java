package me.approximations.apxPlugin;

import com.google.common.base.Stopwatch;
import lombok.Getter;
import me.approximations.apxPlugin.di.manager.DependencyManager;
import me.approximations.apxPlugin.listener.manager.ListenerManager;
import me.approximations.apxPlugin.persistence.jpa.config.PersistenceConfig;
import me.approximations.apxPlugin.persistence.jpa.config.PersistenceUnitConfig;
import me.approximations.apxPlugin.persistence.jpa.config.discovery.PersistenceConfigDiscovery;
import me.approximations.apxPlugin.persistence.jpa.config.impl.HikariPersistenceUnitConfig;
import me.approximations.apxPlugin.persistence.jpa.proxy.handler.SharedSessionMethodHandler;
import me.approximations.apxPlugin.persistence.jpa.repository.impl.SimpleJpaRepository;
import me.approximations.apxPlugin.persistence.jpa.service.register.ServicesRegister;
import me.approximations.apxPlugin.placeholder.manager.PlaceholderManager;
import me.approximations.apxPlugin.placeholder.register.PlaceholderRegister;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.hibernate.Session;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class ApxPlugin extends JavaPlugin {
    private final List<Runnable> disableEntries = new ArrayList<>();

    @Getter
    private static ApxPlugin instance;

    @Getter
    private Reflections reflections;
    @Getter
    private DependencyManager dependencyManager;
    @Getter
    private ListenerManager listenerManager;
    private EntityManagerFactory entityManagerFactory;
    @Getter
    private final PlaceholderManager placeholderManager = new PlaceholderManager();

    @Override
    public void onLoad() {
        this.reflections = new Reflections(getClass().getPackage().getName(), new SubTypesScanner(), new TypeAnnotationsScanner());
        this.dependencyManager = new DependencyManager(reflections);
        onPluginLoad();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void onEnable() {
        final Stopwatch stopwatch = Stopwatch.createStarted();
        instance = this;

        try {
            Logger.getLogger("org.hibernate").setLevel(Level.OFF);
            dependencyManager.registerDependencies();

            dependencyManager.registerDependency(Plugin.class, this);
            dependencyManager.registerDependency(JavaPlugin.class, this);
            dependencyManager.registerDependency(ApxPlugin.class, this);
            dependencyManager.registerDependency(getClass(), this);

            final Optional<PersistenceConfig> persistenceConfigOptional = new PersistenceConfigDiscovery(reflections).discovery();
            persistenceConfigOptional.ifPresent(config -> {
                dependencyManager.injectDependencies(config);

                final List<Class<?>> jpaEntities = getJpaEntities();
                final PersistenceUnitConfig persistenceUnitConfig = new HikariPersistenceUnitConfig(config, jpaEntities);

                final HibernatePersistenceProvider provider = new HibernatePersistenceProvider();

                entityManagerFactory = provider.createContainerEntityManagerFactory(
                        persistenceUnitConfig,
                        persistenceUnitConfig.getProperties()
                );

                final List<Class<? extends SimpleJpaRepository>> repositories = getRepositories();

                for (final Class<? extends SimpleJpaRepository> repositoryClass : repositories) {
                    final Type[] types = ((ParameterizedType) repositoryClass.getGenericSuperclass()).getActualTypeArguments();

                    try {
                        final Class<?> entityClass = Class.forName(types[0].getTypeName());

                        final Session sharedSessionProxy = SharedSessionMethodHandler.createProxy(entityManagerFactory);

                        final Constructor<? extends SimpleJpaRepository> repositoryConstructor = repositoryClass.getConstructor(Session.class, Class.class);

                        if (repositoryConstructor == null) {
                            throw new RuntimeException(String.format("Repository constructor not found with params (%s, %s).", EntityManager.class.getName(), entityClass.getName()));
                        }

                        final SimpleJpaRepository repository = repositoryConstructor.newInstance(sharedSessionProxy, entityClass);

                        dependencyManager.registerDependency(repositoryClass, repository);
                    } catch (ClassNotFoundException | InvocationTargetException | InstantiationException |
                             NoSuchMethodException | IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
            });

            new ServicesRegister(reflections, entityManagerFactory, dependencyManager).register();
            new PlaceholderRegister(this, reflections, dependencyManager, placeholderManager).register();


            this.listenerManager = new ListenerManager(this, reflections, dependencyManager);

            dependencyManager.injectDependencies();
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

    private List<Class<?>> getJpaEntities() {
        final Set<Class<?>> entities = reflections.getTypesAnnotatedWith(Entity.class);
        return new ArrayList<>(entities);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private List<Class<? extends SimpleJpaRepository>> getRepositories() {
        final Set<Class<? extends SimpleJpaRepository>> repositories = reflections.getSubTypesOf(SimpleJpaRepository.class);

        return new ArrayList<>(repositories);
    }

    protected void onPluginLoad() {
    }

    protected abstract void onPluginEnable();

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