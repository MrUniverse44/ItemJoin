package me.RockinChaos.itemjoin.item;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;

import com.vk2gpz.tokenenchant.api.TokenEnchantAPI;

import me.RockinChaos.itemjoin.handlers.ItemHandler;
import me.RockinChaos.itemjoin.utils.ServerUtils;
import me.RockinChaos.itemjoin.utils.StringUtils;
import me.RockinChaos.itemjoin.utils.api.DependAPI;

public class ItemStatistics {
	
	private Map < ItemMap, Map <String, Integer> > itemEnchants = new HashMap < ItemMap, Map <String, Integer> > ();
	
   /**
	* Sets the ItemStatistics information for the specific player.
	* 
	* @param player - The player being referenced.
	* @param items - The ItemMaps to be modified.
	*/
	public ItemStatistics(final Player player, final List <ItemMap> items) {
		for (final ItemMap itemMap : items) {
			this.setEnchantments(player, itemMap);
		}
	}
	
   /**
	* Sets the Custom Enchants to the Custom Item, 
	* adding the specified enchantments to the item.
	* 
	* Translates any placeholders setting the proper
	* enchant name and enchant level.
	* 
	* @param player - The player being referenced.
	* @param itemMap - The ItemMap being modified.
	*/
	private void setEnchantments(final Player player, final ItemMap itemMap) {
		if (itemMap.getNodeLocation().getString(".enchantment") != null) {
			final String enchantlist = itemMap.getNodeLocation().getString(".enchantment").replace(" ", "");
			final String[] enchantments = enchantlist.split(",");
			final Map < String, Integer > listEnchants = new HashMap < String, Integer > ();
			for (final String enchantment: enchantments) {
				final String[] parts = enchantment.split(":");
				final String name = StringUtils.translateLayout(parts[0], player).toUpperCase();
				final String levelPart = StringUtils.translateLayout(parts[1], player);
				final Enchantment enchantName = ItemHandler.getEnchantByName(name);
				int level = 1;
				if (StringUtils.containsIgnoreCase(enchantment, ":")) {
					try {
						level = Integer.parseInt(levelPart);
					} catch (NumberFormatException e) {
						ServerUtils.logSevere("{ItemStatistics} An error occurred in the config, " + levelPart + " is not a number and a number was expected!");
						ServerUtils.logWarn("{ItemStatistics} Enchantment: " + name + " will now be enchanted by level 1.");
						ServerUtils.sendDebugTrace(e);
					}
				}
				if (level > 0 && enchantName != null) {
					listEnchants.put(name, level);
				} else if (level > 0 && enchantName == null && DependAPI.getDepends(false).tokenEnchantEnabled() && TokenEnchantAPI.getInstance().getEnchantment(name) != null) {
					listEnchants.put(name, level);
				} else if (level > 0 && enchantName == null && !DependAPI.getDepends(false).tokenEnchantEnabled()) {
					ServerUtils.logSevere("{ItemStatistics} An error occurred in the config, " + name + " is not a proper enchant name!");
					ServerUtils.logWarn("{ItemStatistics} Please see: https://hub.spigotmc.org/javadocs/spigot/org/bukkit/enchantments/Enchantment.html for a list of correct enchantment names.");
				}
			}
			this.itemEnchants.put(itemMap, listEnchants);
		}
	}
	
   /**
	* Gets the properly translated enchantments for the ItemMap.
	* 
	* @param itemMap - The ItemMap being referenced.
	*/
	public Map <String, Integer> getEnchantments(final ItemMap itemMap) {
		return itemEnchants.get(itemMap);
	}
}