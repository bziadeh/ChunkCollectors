package com.cloth.objects;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by Brennan on 1/5/2020.
 */
public class CollectorInventory {

    private Inventory inventory;

    private Map<Material, ItemData> itemData;

    public CollectorInventory() {
        itemData = new HashMap<>();
    }

    public CollectorInventory(CollectorInventory inventory) {
        itemData = new HashMap<>();

        Inventory defaultInventory = inventory.get();

        this.inventory = Bukkit.createInventory(null, defaultInventory.getSize(), defaultInventory.getTitle());

        for(int i = 0; i < this.inventory.getSize(); i++) {
            this.inventory.setItem(i, defaultInventory.getItem(i));
        }

        inventory.getCollectedMaterials().forEach(material -> {
            itemData.put(material, inventory.getItemData(material));
        });
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public Inventory get() {
        return inventory;
    }

    public void set(Material material, ItemData itemData) {
        this.itemData.put(material, itemData);
    }

    public boolean isCollecting(Material material) {
        return itemData.containsKey(material);
    }

    public double getPrice(Material material) {
        return itemData.get(material).getPrice();
    }

    public Set<Material> getCollectedMaterials() {
        return itemData.keySet();
    }

    public int getSlot(Material material) {
        return itemData.get(material).getSlot();
    }

    public String getName(Material material) {
        return itemData.get(material).getName();
    }

    public ItemData getItemData(Material material) {
        return itemData.get(material);
    }
}
