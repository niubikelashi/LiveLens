package com.bionic.creativebox.listener;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * 真正入口钩子：拦截所有跨世界传送。
 * 
 * 坑 #5: TeleportEvent 内再 teleport() 会死循环 → 用 isEntering/isExiting 标记挡住。
 */
public class TeleportListener implements Listener {

    private final CreativeBoxPlugin plugin;

    public TeleportListener(CreativeBoxPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        SandboxService svc = plugin.getSandboxService();

        // 防递归：如果已经在进入/退出流程中，跳过（坑 #5）
        if (svc.isEntering(player) || svc.isExiting(player)) return;

        World fromWorld = event.getFrom().getWorld();
        Location toLoc = event.getTo();
        
        // getTo() 可能为 null（坑 #5）
        if (toLoc == null) return;
        World toWorld = toLoc.getWorld();

        if (fromWorld == null || toWorld == null) return;

        String sandboxName = plugin.getConfig().getString("world.name", "sandbox");
        boolean fromSandbox = fromWorld.getName().equals(sandboxName);
        boolean toSandbox = toWorld.getName().equals(sandboxName);

        // 进入沙盒：from != sandbox && to == sandbox
        if (!fromSandbox && toSandbox) {
            event.setCancelled(true);   // cancel original teleport
            svc.enter(player);          // run enter flow
        }
        // 退出沙盒：from == sandbox && to != sandbox  
        else if (fromSandbox && !toSandbox) {
            event.setCancelled(true);
            svc.exit(player);           // run exit flow
        }
    }
}
