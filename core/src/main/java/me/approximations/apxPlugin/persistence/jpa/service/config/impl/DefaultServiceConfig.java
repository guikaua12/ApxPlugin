package me.approximations.apxPlugin.persistence.jpa.service.config.impl;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import me.approximations.apxPlugin.persistence.jpa.service.config.ServiceConfig;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DefaultServiceConfig implements ServiceConfig {
    public static final DefaultServiceConfig INSTANCE = new DefaultServiceConfig();
    public static final ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("ApxPlugin-Service-Thread-%d").build());

    @Override
    public ExecutorService getExecutorService() {
        return EXECUTOR_SERVICE;
    }
}
