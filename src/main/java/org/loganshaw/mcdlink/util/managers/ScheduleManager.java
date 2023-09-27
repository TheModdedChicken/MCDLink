package org.loganshaw.mcdlink.util.managers;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.loganshaw.mcdlink.MCDLink;

public class ScheduleManager {
    MCDLink plugin;
    BukkitScheduler scheduler;

    public ScheduleManager (MCDLink plugin) {
        this.plugin = plugin;
        this.scheduler = plugin.scheduler;
    }

    public BukkitTask SyncTask (Runnable task) {
        return this.scheduler.runTask(this.plugin, task);
    }

    public BukkitTask Timeout(long delay, Runnable task) {
        return this.scheduler.runTaskAsynchronously(this.plugin, () -> new java.util.Timer().schedule(
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
