package com.exometric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import static net.minecraft.commands.Commands.literal;

public class ExoMetric implements ModInitializer {

    @Override
    public void onInitialize() {
        // Carrega configurações primeiro
        ConfigManager.load();
        
        // Start metric periodic collection
        MetricsCollector.start();
        
        // Start internal HTTP server
        StatsHttpServer.start();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            MetricsCollector.setServer(server);
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            MetricsCollector.stop();
            StatsHttpServer.stop();
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            MetricsCollector.onTick();
        });

        System.out.println("ExoMetric API initialized!");
    }
}
