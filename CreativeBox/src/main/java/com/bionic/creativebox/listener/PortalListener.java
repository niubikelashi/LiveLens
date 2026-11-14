package com.bionic.creativebox.listener;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * 传送门与末影珍珠拦截：沙盒世界内取消所有跨维度传送（坑 #5）。
 */
public class PortalListener implements Listener {

    private final CreativeBoxPlugin plugin;

    public PortalListener(CreativeBoxPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        Player player = event.getPlayer();
        
        if (plugin.getWorldManager().getSandboxWorld() != null && 
                player.getWorld() == plugin.getWorldManager().getSandboxWorld()) {
            // 取消传送门（包括地狱门、末地门）
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityPortal(org.bukkit.event.entity.EntityPortalEvent event) {
        if (plugin.getWorldManager().getSandboxWorld() != null && 
                event.getEntity().getWorld() == plugin.getWorldManager().getSandboxWorld()) {
            // 拦截末影珍珠跨世界传送、矿车通过传送门等（坑 #5）
            if (event.getEntityType() == EntityType.ENDER_PEARL) return; // 同世界珍珠不拦
            event.setCancelled(true);
        }
    }
}
