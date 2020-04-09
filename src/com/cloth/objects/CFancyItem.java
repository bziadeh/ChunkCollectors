package com.cloth.objects;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.*;

/**
 * Created by Brennan on 9/30/2019.
 */

public class CFancyItem {

    private final Map<String, Object> placeholders;
    private final ItemStack item;
    private final ItemMeta meta;

    public CFancyItem(Material type) {
        placeholders = new HashMap<>();
        item = new ItemStack(type);
        meta = item.getItemMeta();
    }

    /**
     * Sets the displayname of the item. Automatically converts '&' to '§'
     *
     * @param name The name of the item being created.
     * @return A reference to this object.
     */
    public CFancyItem setDisplayname(String name) {
        meta.setDisplayName(name.replaceAll("&", "§"));
        return this;
    }

    /**
     * Sets the lore of the item. Automatically converts '&' to '§'
     *
     * @param lore The lore of the item being created.
     * @return A reference to this object.
     */
    public CFancyItem setLore(List<String> lore) {
        for(int i = lore.size() - 1; i >= 0; i--)
            lore.set(i, lore.get(i).replaceAll("&", "§"));
            meta.setLore(lore);
        return this;
    }

    /**
     * Adds an item flag to the item.
     *
     * @param itemFlag the flag being added.
     * @return A reference to this object.
     */
    public CFancyItem addItemFlag(ItemFlag itemFlag) {
        meta.addItemFlags(itemFlag);
        return this;
    }

    /**
     * Sets the amount of the item. Prevents the amount from
     * going over the minimum limit 1, and the maximum limit 64.
     *
     * @param amount The amount of the item being created.
     * @return A reference to this object.
     */
    public CFancyItem setAmount(int amount) {
        item.setAmount(amount < 0 ? 1 : amount > 64 ? 64 : amount);
        return this;
    }

    /**
     * Finalizes the creation process and returns the item.
     *
     * @return the ItemStack of the item that was created.
     */
    public ItemStack build() {
        convertPlaceholderValues();
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Converts the placeholder string for the item's displayname and lore.
     * This is especially useful if you are retrieving item information
     * from the configuration file.
     *
     * @param placeholder the placeholder being added (ex: %uses%)
     * @param replacement the replacement being added (ex: 6)
     */
    public CFancyItem addPlaceholder(String placeholder, Object replacement) {
        placeholders.put(placeholder, replacement);
        return this;
    }

    /**
     * Gives the ItemStack created to a Player. Automatically builds.
     *
     * @param player the Player being given the item.
     */
    public void give(Player player) {
        player.getInventory().addItem(build());
    }

    /**
     * Converts all placeholders to their appropriate corresponding value.
     * (Example: %uses% -> 6)
     */
    private void convertPlaceholderValues() {
        if(meta.getLore() == null)
            return;
        final List<String> beforeReplacement = meta.getLore();

        for(int i = beforeReplacement.size() - 1; i >= 0; i--) {
            String cValue = beforeReplacement.get(i);

            for(String pValue : placeholders.keySet()) {
                if (cValue.contains(pValue))
                    beforeReplacement.set(i, cValue.replaceAll(pValue, String.valueOf(placeholders.get(pValue))));
            }
        }
    }

    private final String user = "%%__USERNAME__%%";
    private final String version = "%%__VERSION__%%";
    private final String resource = "%%__RESOURCE__%%";
    private final String timestamp = "%%__TIMESTAMP__%%";
}

