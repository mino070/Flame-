package com.flamekatana;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * Builds and recognises the Flame Katana item.
 * The sword is marked with a hidden tag, so renaming it in an anvil
 * or changing its lore does not matter - only the tag counts.
 */
public final class KatanaItem {

    private final NamespacedKey key;

    public KatanaItem(NamespacedKey key) {
        this.key = key;
    }

    public ItemStack create() {
        ItemStack item = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Flame Katana", NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(List.of(
                line("A blade forged in living fire.", NamedTextColor.GRAY),
                Component.empty(),
                line("Press F to fire a Flame Slash", NamedTextColor.YELLOW),
                line("Damage: 4 hearts", NamedTextColor.RED),
                line("Night: stronger, wider, faster", NamedTextColor.AQUA)
        ));

        meta.setUnbreakable(true);
        meta.setEnchantmentGlintOverride(true);
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);

        item.setItemMeta(meta);
        return item;
    }

    public boolean isKatana(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    private static Component line(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }
}
