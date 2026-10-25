package com.flamekatana;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public final class KatanaListener implements Listener {

    private final FlameKatana plugin;

    public KatanaListener(FlameKatana plugin) {
        this.plugin = plugin;
    }

    /**
     * The F key (swap hands) fires this event.
     * Careful: getOffHandItem() is the item that is in the player's MAIN hand
     * right now and is about to be moved to the off hand. That is the katana
     * we want to check.
     */
    @EventHandler(ignoreCancelled = true)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        if (!plugin.katana().isKatana(event.getOffHandItem())) {
            return;
        }

        // Stop the sword from actually swapping to the off hand.
        event.setCancelled(true);

        Player player = event.getPlayer();
        if (!player.hasPermission("flamekatana.use")) {
            String msg = plugin.getConfig().getString("messages.no-permission", "You cannot use the Flame Katana.");
            player.sendActionBar(Component.text(msg, NamedTextColor.RED));
            return;
        }

        plugin.slash().cast(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.slash().clearCooldown(event.getPlayer().getUniqueId());
    }
}
