package me.approximations.apxPlugin.testPlugin;

import com.j256.ormlite.support.ConnectionSource;
import me.approximations.apxPlugin.core.ApxPlugin;
import me.approximations.apxPlugin.core.di.Inject;

public class Main extends ApxPlugin {
    @Inject
    private ConnectionSource connectionSource;


    @Override
    protected void onPluginEnable() {
//        System.out.println(userRepository.findAll());

//        final BungeeChannel bungeeChannel = new BungeeChannel(this);
//        bungeeChannel.init();
//
//        bungeeChannel.subscribe("test", message -> {
//            System.out.println("[Subscriber] Received on channel `test` message: " + message.getBody());
//            message.respond("Response from any server!");
//        });

//        bungeeChannel.subscribe("test", message -> {
//            System.out.println("[Subscriber] Received on channel `test` message: " + message.getBody());
//            message.respond("Response from any server!");
//        });
    }

    public static ApxPlugin getPlugin() {
        return getPlugin(Main.class);
    }
}