package org.loganshaw.mcdlink.util.managers;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.loganshaw.mcdlink.MCDLink;

public class ScheduleManager {
    MCDLink plugin;

    public ScheduleManager (MCDLink plugin) {
        this.plugin = plugin;
    }

    public BukkitTask Timeout(long delay, Runnable task) {
        return Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> new java.util.Timer().schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        task.run();
                    }
                },
                delay
        ));
    }
}
