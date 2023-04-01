/*
 * ItemJoin
 * Copyright (C) CraftationGaming <https://www.craftationgaming.com/>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package me.RockinChaos.itemjoin.item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.scheduler.BukkitRunnable;

import me.RockinChaos.itemjoin.ItemJoin;
import me.RockinChaos.itemjoin.item.ItemCommand.Executor;
import me.RockinChaos.core.handlers.ItemHandler;
import me.RockinChaos.core.handlers.PlayerHandler;
import me.RockinChaos.core.utils.SchedulerUtils;
import me.RockinChaos.core.utils.ServerUtils;
import me.RockinChaos.core.utils.StringUtils;
import me.RockinChaos.itemjoin.utils.api.GuardAPI;
import me.RockinChaos.itemjoin.utils.sql.DataObject;
import me.RockinChaos.itemjoin.utils.sql.DataObject.Table;

public class ItemUtilities {
  	private List < ItemMap > items = new ArrayList < ItemMap >();
  	private List < ItemMap > craftingItems = new ArrayList < ItemMap >();
  	private List < ItemMap > protectItems = new ArrayList < ItemMap >();
	private HashMap <Integer, Integer> failCount = new HashMap <Integer, Integer> ();
	private HashMap <String, ItemStatistics> itemStats = new HashMap <String, ItemStatistics> ();
	
	private static ItemUtilities utilities;

   /**
    * Checks if the specified itemflag and ItemStack is allowed.
    * 
    * @param player - The Player that is being allowed.
    * @param item - The ItemStack to check if it is allowed to perform the itemflag.
    * @param itemflag - The itemflag to be checked if it is allowed.
    */
	public boolean isAllowed(final Player player, final ItemStack item, final String itemflag) {
		if (player == null || item == null) { return true; }
		ItemMap itemMap = this.getItemMap(item);
		if (itemMap != null && itemMap.isAllowedItem(player, item, itemflag)) {
			return false;
		}
		return true;
	}
	
   /**
    * Finds the matching ItemMap from the list of provided ItemMaps,
    * that has the exact same ItemStack defined.
    * 
    * @param itemStack - The ItemStack to have its ItemMap located.
    */
	public ItemMap getItemMap(final ItemStack itemStack) {
		for (ItemMap itemMap : this.getItems()) {
			if (itemMap.isSimilar(null, itemStack)) {
				return itemMap;
			}
		}
		return null;
	}
	
   /**
    * Finds the matching ItemMap from the list of provided ItemMaps,
    * that has the exact same config node name defined.
    * 
    * @param configName - The config node name of the ItemMap.
    */
	public ItemMap getItemMap(final String configName) {
		for (ItemMap itemMap : this.getItems()) {
			if (configName != null && itemMap.getConfigName().equalsIgnoreCase(configName)) {
				return itemMap;
			}
		}
		return null;
	}
	
   /**
    * Finds the matching ItemMap from the list of provided ItemMaps,
    * that has the exact same slot defined.
    * 
    * @param slot - The slot that the ItemMap should have defined.
    * @param items - The list of ItemMap to be checked.
    * @return The ItemMap matching the specified slot.
    */
	public ItemMap getItemMap(final String slot, final List < ItemMap > items) {
		final List<String> configMap = new ArrayList<String>();
		for (final ItemMap item: items) { configMap.add(item.getConfigName()); } {
			Collections.sort(configMap, new Comparator<String>() {
			    @Override
			    public int compare(String s1, String s2) {
			        return s1.compareToIgnoreCase(s2);
			    }
			});
		} {
			ItemMap itemMap = null;
			boolean located = false;
			for (final String configName: configMap) {
				if (located) { break; }
				for (final ItemMap item: items) { 
					if (located) { break; }
					if (item.getConfigName().equalsIgnoreCase(configName) && item.getUISlot().equalsIgnoreCase(slot)) {
						itemMap = item;
						located = true;
					}
				}
			}
			return itemMap;
		}
	}
	
   /**
    * Closes the ItemAnimations for ALL Online Players.
    * 
    */
	public void closeAnimations() {
		PlayerHandler.forOnlinePlayers(player -> { this.closeAnimations(player); });
	}
	
   /**
    * Deletes the Toggle Commands registered for the server.
    * 
    */
	public void delToggleCommands() {
		for (ItemMap item : this.getItems()) {
			item.delToggleCommands();
		}
	}
	
   /**
    * Closes the ItemAnimations for the specified Player.
    * 
    * @param player - The Player that is having their item animations closed.
    */
	public void closeAnimations(final Player player) {
		for (ItemMap item : this.getItems()) {
			if (item.isAnimated() && item.getAnimationHandler().get(player) != null
					|| item.isDynamic() && item.getAnimationHandler().get(player) != null) {
				item.getAnimationHandler().get(player).closeAnimation(player);
				item.removeFromAnimationHandler(player);
			}
		}
	}
	
   /**
    * Updates the ItemMap List to fill the Player placeholders for ALL Online Players.
    * 
    */
	public void updateItems() {
		PlayerHandler.forOnlinePlayers(player -> { this.updateItems(player, true); });
	}
	
   /**
    * Updates the ItemMap List to fill the Player placeholders.
    * 
    * @param player - The Player that is having their items updated.
    * @param refresh - If the ItemAnimation should be restarted.
    */
	private void updateItems(final Player player, final boolean refresh) {
		for (ItemMap item: this.getItems()) {
			item.updateItem(player);
			if (refresh) {
				item.setAnimations(player);
			}
		}
	}
	
   /**
    * Checks if the Player is waiting Authentication from a plugin such as AuthMe. 
    * Then sets the item after Authentication is complete.
    * 
    * @param player - The Player that is being authenticated.
    * @param world - The World that the player is from.
    * @param type - The TriggerType that is being performed.
    * @param newMode - The GameMode of the Player.
    * @param region - The region the Player is in.
    */
	public void setAuthenticating(final Player player, final World world, TriggerType type, final GameMode newMode, final String region) {
		if (ItemJoin.getCore().getDependencies().authMeEnabled()) {
			new BukkitRunnable() {
				@Override
				public void run() {
					try { 
						final fr.xephi.authme.api.v3.AuthMeApi authMe = fr.xephi.authme.api.v3.AuthMeApi.getInstance();
						if (fr.xephi.authme.api.v3.AuthMeApi.getInstance().isAuthenticated(player) 
						|| (authMe.getPlugin().getConfig().getString("settings.registration.force") != null 
					    && !authMe.getPlugin().getConfig().getBoolean("settings.registration.force"))) {
							setItems(player, world, type, newMode, region);
							this.cancel();
						} else if (!player.isOnline()) {
							this.cancel();
						}
					} catch (NoClassDefFoundError e) {
						ServerUtils.logSevere("{ItemMap} You are using an outdated version of AuthMe, custom items will not be given after login.");
						this.cancel();
					}
				}
			}.runTaskTimer(ItemJoin.getCore().getPlugin(), 0, 20);
		} else {
			this.setItems(player, world, type, newMode, region);
		}
	}
	
   /**
    * Sets the items to be given.
    * 
    * @param player - The Player that is having their items set.
    * @param world - The World that the player is from.
    * @param type - The TriggerType that is being performed.
    * @param newMode - The GameMode of the Player.
    * @param region - The region the Player is in.
    */
	public void setItems(final Player player, final World world, final TriggerType type, final GameMode newMode, final String region) {
		this.setStatistics(player);
		this.safeSet(player, world, type, region);
		if (this.getItemDelay() != 0 && type != TriggerType.LIMIT_SWITCH && type != TriggerType.REGION_ENTER && type != TriggerType.REGION_LEAVE) {
			SchedulerUtils.runLater(this.getItemDelay(), () -> {
				ItemData.getInfo().restoreCraftItems(player, type); {
					this.handleItems(player, world, type, newMode, region);
				}
			});
		} else {
			ItemData.getInfo().restoreCraftItems(player, type); {
				this.handleItems(player, world, type, newMode, region);
			}
		}
	}
	
   /**
    * Handles the item give and removal provided the trigger type.
    * 
    * @param player - The Player that is having their items handled.
    * @param type - The TriggerType that is being performed.
    * @param newMode - The GameMode of the Player.
    * @param region - The region the Player is in.
    */
	private void handleItems(final Player player, World world, final TriggerType type, final GameMode gameMode, final String region) {
		ItemMap probable = null;
		for (Object itemMap : ItemJoin.getCore().getChances().getItems().keySet()) {
			if (((ItemMap)itemMap).hasItem(player, true)) {
				probable = (ItemMap)itemMap;
			}
		}
		if (probable == null) { probable = (ItemMap) ItemJoin.getCore().getChances().getRandom(player); }
		final int session = StringUtils.getRandom(1, 100000);
		if (type.equals(TriggerType.WORLD_SWITCH) || type.equals(TriggerType.JOIN)) { world = player.getWorld(); }
		for (ItemMap item : this.getItems()) { 
			item.setAnimations(player);
			if (((((type.equals(TriggerType.JOIN) && item.isGiveOnJoin())
			  || (type.equals(TriggerType.TELEPORT) && item.isGiveOnTeleport()) 
			  || (type.equals(TriggerType.RESPAWN) && (item.isGiveOnRespawn() || item.isDeathKeepable()))
			  || (type.equals(TriggerType.WORLD_SWITCH) && item.isGiveOnWorldSwitch())
			  || (type.equals(TriggerType.LIMIT_SWITCH) && item.isUseOnLimitSwitch() && (region.equalsIgnoreCase("IJ_WORLD") || item.inRegion(region) || item.getEnabledRegions() == null || item.getEnabledRegions().isEmpty())))
		      || ((((type.equals(TriggerType.REGION_ENTER) && (item.isGiveOnRegionEnter() || item.isGiveOnRegionAccess()))) 
			  || (type.equals(TriggerType.REGION_LEAVE) && (item.isGiveOnRegionLeave() || item.isGiveOnRegionEgress()))) && item.inRegion(region))))
			   && item.inWorld(world) && item.isLimitMode(gameMode) && ((probable != null && item.getConfigName().equals(probable.getConfigName())) || item.getProbability() == -1) 
			   && item.conditionMet(player, "trigger-conditions", false) && ItemData.getInfo().isEnabled(player, item.getConfigName()) && item.hasPermission(player, world) 
			   && this.isObtainable(player, item, session, type)) {
				item.giveTo(player); 
			} else if (((type.equals(TriggerType.LIMIT_SWITCH) && item.isUseOnLimitSwitch() && !item.isLimitMode(gameMode)) || (((type.equals(TriggerType.REGION_LEAVE) && (item.isGiveOnRegionAccess() 
					|| !item.inRegion(region))) || (type.equals(TriggerType.REGION_ENTER) && (item.isGiveOnRegionEgress() || !item.inRegion(region)))))) && item.hasItem(player, false)) {
				item.removeFrom(player);
			} else if (item.isAutoRemove() && (!item.inWorld(world) || !item.isLimitMode(gameMode)) && item.hasItem(player, true)) {
				item.removeFrom(player);
			}
		}
		this.sendFailCount(player, session);
		PlayerHandler.updateInventory(player, 15L);
	}
	
   /**
    * Sets the statistical information for the player for the listed ItemMap.
    * 
    * @param player - The Player that is having their items set.
    */
	private void setStatistics(final Player player) {
		if (this.itemStats != null && this.itemStats.containsKey(PlayerHandler.getPlayerID(player))) {
			this.itemStats.remove(PlayerHandler.getPlayerID(player));
		}
		this.itemStats.put(PlayerHandler.getPlayerID(player), new ItemStatistics(player, this.getItems()));
	}
	
   /**
    * Safely checks and sets the items for their corresponding TriggerType.
    * 
    * @param player - The Player that is having their items set.
    * @param type - The TriggerType that is being performed.
    */
	private void safeSet(final Player player, final World world, final TriggerType type, final String region) {
		if (StringUtils.splitIgnoreCase(ItemData.getInfo().getHotbarTriggers(), type.name, ",")) { PlayerHandler.setHotbarSlot(player, ItemData.getInfo().getHotbarSlot()); }
		if (type.equals(TriggerType.REGION_LEAVE)) { GuardAPI.pasteReturnItems(player, region); }
		if (type.equals(TriggerType.WORLD_SWITCH)) { this.pasteReturnItems(type, player, world.getName()); }
		if (type.equals(TriggerType.REGION_ENTER)) { this.clearEvent(type, player, "", region); }
		if (type.equals(TriggerType.QUIT)) { this.clearEvent(type, player, world.getName(), ""); }
		if (this.getClearDelay() != 0) {
			SchedulerUtils.runLater(this.getClearDelay(), () -> {
				if (type.equals(TriggerType.JOIN) || type.equals(TriggerType.WORLD_SWITCH) || type.equals(TriggerType.TELEPORT)) {
					this.clearEvent(type, player, player.getWorld().getName(), "");
				}
				this.triggerCommands(player, type);
			});
		} else {
			if (type.equals(TriggerType.JOIN) || type.equals(TriggerType.WORLD_SWITCH) || type.equals(TriggerType.TELEPORT) || type.equals(TriggerType.QUIT)) {
				this.clearEvent(type, player, player.getWorld().getName(), "");
			}
			this.triggerCommands(player, type);
		}
	}
	
   /**
    * Sets the Players items to be cleared upon performing the specified event.
    * 
    * @param type - The trigger type.
    * @param player - The Player performing the event.
    * @param world - The world the Player is in.
    * @param event - The event/trigger being performed.
    * @param region - The region the Player is in (if any).
    */
	private void clearEvent(final TriggerType type, final Player player, final String world, final String region) {
		String clearEvent = ItemJoin.getCore().getConfig("config.yml").getString("Clear-Items." + type.name);
		if (clearEvent != null && ((region != null && !region.isEmpty() && StringUtils.containsLocation(region, clearEvent.replace(" ", ""))) || StringUtils.containsLocation(world, clearEvent.replace(" ", "")))) {
			if ((ItemJoin.getCore().getConfig("config.yml").getString("Clear-Items.Options") != null && player != null 
				&& (StringUtils.containsIgnoreCase(ItemJoin.getCore().getConfig("config.yml").getString("Clear-Items.Options"), "PROTECT_OP") && player.isOp())
				|| (StringUtils.containsIgnoreCase(ItemJoin.getCore().getConfig("config.yml").getString("Clear-Items.Options"), "PROTECT_CREATIVE") && PlayerHandler.isCreativeMode(player)))) {
			} else {
				String clearType = ItemJoin.getCore().getConfig("config.yml").getString("Clear-Items.Type");
				if (clearType != null && (clearType.equalsIgnoreCase("ALL") || clearType.equalsIgnoreCase("GLOBAL"))) {
					this.clearItems(type, player, region, type.name, 0);
				} else if (clearType != null && clearType.equalsIgnoreCase("VANILLA")) {
					this.clearItems(type, player, region, type.name, 1);
				} else if (clearType != null && clearType.equalsIgnoreCase("ITEMJOIN")) {
					this.clearItems(type, player, region, type.name, 2);
				} else if (clearType != null) {
					ServerUtils.logSevere("{ItemMap} " + clearType + " for Clear-Items in the config.yml is not a valid option.");
				}
			}
		}
	}
	
   /**
    * Clears the items from the Player Inventory.
    * 
    * @param type - The trigger type.
    * @param player - The Player performing the event.
    * @param event - The event/trigger being performed.
    * @param region - The region the Player is in (if any).
    * @param clearAll - If ALL items are expected to be cleared from the Player Inventory.
    */
	private void clearItems(final TriggerType type, final Player player, final String event, final String region, final int clearType) {
		this.protectItems = this.getProtectItems();
		PlayerInventory inventory = player.getInventory();
		Inventory craftView = player.getOpenInventory().getTopInventory();
		GuardAPI.saveReturnItems(player, event, region, craftView, inventory, 0);
		this.saveReturnItems(type, player, player.getWorld().getName(), craftView, inventory, 0);
		for (int i = 0; i < (!this.protectItems.isEmpty() ? this.protectItems.size() : 1); i++) {
			if (this.canClear(inventory.getHelmet(), "Helmet", i, clearType)) {
				inventory.setHelmet(new ItemStack(Material.AIR));
			} if (this.canClear(inventory.getChestplate(), "Chestplate", i, clearType)) {
				inventory.setChestplate(new ItemStack(Material.AIR));
			} if (this.canClear(inventory.getLeggings(), "Leggings", i, clearType)) {
				inventory.setLeggings(new ItemStack(Material.AIR));
			} if (this.canClear(inventory.getBoots(), "Boots", i, clearType)) {
				inventory.setBoots(new ItemStack(Material.AIR));
			} if (ServerUtils.hasSpecificUpdate("1_9") && this.canClear(inventory.getItemInOffHand(), "OffHand", i, clearType)) {
				PlayerHandler.setOffHandItem(player, new ItemStack(Material.AIR));
			} if (PlayerHandler.isCraftingInv(player.getOpenInventory())) {
				for (int k = 0; k < player.getOpenInventory().getTopInventory().getContents().length; k++) {
					if (this.canClear(player.getOpenInventory().getTopInventory().getItem(k), "CRAFTING[" + k + "]", i, clearType)) {
						craftView.setItem(k, new ItemStack(Material.AIR));
					}
				}
			}
			for (int f = 0; f < inventory.getSize(); f++) {
				if (this.canClear(inventory.getItem(f), Integer.toString(f), i, clearType)) {
					inventory.setItem(f, new ItemStack(Material.AIR));
				}
			}
		}
	}
	
   /**
    * Checks if the ItemStack is allowed to be cleared.
    * 
    * @param item - The ItemStack being checked.
    * @param slot - The slot the ItemStack is in.
    * @param i - The position in the cycle for protected items.
    * @param clearAll - If ALL items are expected to be cleared from the Player Inventory.
    * @return If the ItemStack is allowed.
    */
	public boolean canClear(final ItemStack item, final String slot, final int i, final int clearType) {
		return item != null && !this.isBlacklisted(slot, item) && !this.isProtected(i, item) && 
			   (clearType == 0 || (clearType == 1 && !ItemHandler.containsNBTData(item, ItemData.getInfo().getNBTList())) || (clearType == 2 && ItemHandler.containsNBTData(item, ItemData.getInfo().getNBTList())));
	}
	
   /**
    * Checks if the ItemStack is blacklisted from being cleared.
    * 
    * @param item - The ItemStack being checked.
    * @param slot - The slot the ItemStack is in.
    * @return If the ItemStack is blacklisted.
    */
	private boolean isBlacklisted(String slot, ItemStack item) {
		String[] blacklist = null;
		String list = ItemJoin.getCore().getConfig("config.yml").getString("Clear-Items.Blacklist");
		if (list != null && list.contains("{") && list.contains("}")) { blacklist = list.split(","); }
		try {
			if (blacklist != null) {
				for (String value: blacklist) {
					String valType = (StringUtils.containsIgnoreCase(value, "{id") ? "id" : (StringUtils.containsIgnoreCase(value, "{slot") ? "slot" : (StringUtils.containsIgnoreCase(value, "{name") ? "name" : "")));
					String inputResult = org.apache.commons.lang.StringUtils.substringBetween(value, "{" + valType + ":", "}");
					if (valType.equalsIgnoreCase("id") && item.getType() == ItemHandler.getMaterial(inputResult.trim(), null)) {
						return true;
					} else if (valType.equalsIgnoreCase("slot") && slot.trim().equalsIgnoreCase(inputResult.trim())) {
						return true;
					} else if (valType.equalsIgnoreCase("name") && item.hasItemMeta() && item.getItemMeta().hasDisplayName()
						&& ChatColor.stripColor(item.getItemMeta().getDisplayName()).trim().equalsIgnoreCase(inputResult.trim())) {
						return true;
					}
				}
			}
		} catch (Exception e) {
			ServerUtils.logSevere("{ItemMap} It looks like the Blacklist section is missing quotations or apostrohes.");
			ServerUtils.logSevere("{ItemMap} Include quotations or apostrophes at the beginning and the end or this error will persist.");
			ServerUtils.logSevere("{ItemMap} The blacklist should look like '{id:DIAMOND}, {slot:0}' or \"{id:DIAMOND}, {slot:0}\".");
		}
		return false;
	}
	
   /**
    * Checks if the ItemMap is obtainable by the Player.
    * 
    * @param player - The Player that is trying to obtain the ItemMap.
    * @param itemMap - The ItemMap being checked.
    * @param session - The current set items session.
    * @param gamemode - The current GameMode of the Player.
    * @return If the ItemMap is Obtainable.
    */
	public boolean isObtainable(final Player player, final ItemMap itemMap, final int session, final TriggerType type) {
		if (!itemMap.hasItem(player, false) || itemMap.isAlwaysGive()) {
			DataObject firstJoin = (itemMap.isOnlyFirstLife() && type.equals(TriggerType.RESPAWN) || itemMap.isOnlyFirstJoin() ? (DataObject) ItemJoin.getCore().getSQL().getData(new DataObject(Table.FIRST_JOIN, PlayerHandler.getPlayerID(player), "", itemMap.getConfigName())) : null);
			DataObject firstWorld = itemMap.isOnlyFirstWorld() ? (DataObject) ItemJoin.getCore().getSQL().getData(new DataObject(Table.FIRST_WORLD, PlayerHandler.getPlayerID(player), player.getWorld().getName(), itemMap.getConfigName())) : null;
			DataObject ipLimit = itemMap.isIpLimted() ? (DataObject) ItemJoin.getCore().getSQL().getData(new DataObject(Table.IP_LIMITS, PlayerHandler.getPlayerID(player), player.getWorld().getName(), itemMap.getConfigName(), player.getAddress().getHostString())) : null;
			if ((firstJoin == null || (itemMap.isOnlyFirstLife() && type.equals(TriggerType.RESPAWN))) && firstWorld == null 
					&& (ipLimit == null || (ipLimit != null && ipLimit.getPlayerId().equalsIgnoreCase(PlayerHandler.getPlayerID(player)))) && this.canOverwrite(player, itemMap)) {
				return true;
			} else if (firstJoin == null && firstWorld == null && ipLimit == null) {
				if (session != 0 && this.failCount.get(session) != null) {
					this.failCount.put(session, this.failCount.get(session) + 1);
				} else if (session != 0) { this.failCount.put(session, 1); }
				ServerUtils.logDebug("{ItemMap} " + player.getName() + " has failed to receive item: " + itemMap.getConfigName() + ".");
				return false;
			} else { 
				if (firstJoin != null) { 
					ServerUtils.logDebug("{ItemMap} " + player.getName() + " has already received first-join " + itemMap.getConfigName() + ", they can no longer recieve this."); 
					return false;
				} else if (firstWorld != null) { 
					ServerUtils.logDebug("{ItemMap} " + player.getName() + " has already received first-world " + itemMap.getConfigName() + ", they can no longer recieve this in " + player.getWorld().getName() + "."); 
					return false;
				} else if (ipLimit != null && !ipLimit.getPlayerId().equalsIgnoreCase(PlayerHandler.getPlayerID(player))) { 
					ServerUtils.logDebug("{ItemMap} " + player.getName() + " has already received ip-limited " + itemMap.getConfigName() + ", they will only recieve this on their dedicated ip.");
					return false;
				}
			}
		}
		ServerUtils.logDebug("{ItemMap} " + player.getName() + " already has item: " + itemMap.getConfigName() + ".");
		return false;
	}
	
   /**
    * Checks if the ItemMap can overwrite itself.
    * 
    * @param player - The Player that is trying to overwrite the ItemMap.
    * @param itemMap - The ItemMap being checked.
    * @return If the ItemMap is Overwritable.
    */
	public boolean canOverwrite(final Player player, final ItemMap itemMap) {
		try {
			if ((itemMap.isCraftingItem() && StringUtils.getSlotConversion(itemMap.getSlot()) == 0) || this.isOverwritable(player, itemMap) || (itemMap.isDropFull() || ((itemMap.isGiveNext() || itemMap.isMoveNext()) && player.getInventory().firstEmpty() != -1))) { return true; }
		} catch (Exception e) { ServerUtils.sendDebugTrace(e); }
		return false;
	}
	
   /**
    * Gets if the ItemMap can overwrite itself.
    * 
    * @param player - The Player that is trying to overwrite the ItemMap.
    * @param itemMap - The ItemMap being checked.
    * @return If the ItemMap is Overwritable.
    */
	private boolean isOverwritable(final Player player, final ItemMap itemMap) {
		try {
			String overWrite = ItemJoin.getCore().getConfig("items.yml").getString("items-Overwrite");
			if (itemMap.isOverwritable() || (((overWrite == null || (overWrite != null && StringUtils.containsLocation(player.getWorld().getName(), overWrite.replace(" ", ""))))) 
					|| (ItemJoin.getCore().getConfig("items.yml").getBoolean("items-Overwrite")))) {
				return true; 
			} else if (CustomSlot.ARBITRARY.isSlot(itemMap.getSlot()) && player.getInventory().firstEmpty() == -1) {
				return false;
			} else if (CustomSlot.HELMET.isSlot(itemMap.getSlot()) && player.getInventory().getHelmet() != null) {
				return false;
			} else if (CustomSlot.CHESTPLATE.isSlot(itemMap.getSlot()) && player.getInventory().getChestplate() != null) {
				return false;
			} else if (CustomSlot.LEGGINGS.isSlot(itemMap.getSlot()) && player.getInventory().getLeggings() != null) {
				return false;
			} else if (CustomSlot.BOOTS.isSlot(itemMap.getSlot()) && player.getInventory().getBoots() != null) {
				return false;
			} else if (ServerUtils.hasSpecificUpdate("1_9") && CustomSlot.OFFHAND.isSlot(itemMap.getSlot())) {
				if (player.getInventory().getItemInOffHand().getType() != Material.AIR) {
					return false;
				}
			} else if (StringUtils.isInt(itemMap.getSlot()) && player.getInventory().getItem(Integer.parseInt(itemMap.getSlot())) != null) {
				return false;
			}
		} catch (Exception e) { ServerUtils.sendDebugTrace(e); }
		return true;
	}
	
   /**
    * Sends the number of items that the Player failed to Obtain.
    * 
    * @param player - The Player that has failed to be given some items.
    * @param session - The current set items session.
    */
	public void sendFailCount(final Player player, final int session) {
		SchedulerUtils.run(() -> {
			if (this.failCount.get(session) != null && this.failCount.get(session) != 0) {
				String overWrite = ItemJoin.getCore().getConfig("items.yml").getString("items-Overwrite");
				if ((overWrite != null && StringUtils.containsLocation(player.getWorld().getName(), overWrite.replace(" ", "")))) {
					String[] placeHolders = ItemJoin.getCore().getLang().newString(); placeHolders[7] = this.failCount.get(session).toString();
					ItemJoin.getCore().getLang().sendLangMessage("general.failedInventory", player, placeHolders);
				} else {
					String[] placeHolders = ItemJoin.getCore().getLang().newString(); placeHolders[7] = this.failCount.get(session).toString();
					ItemJoin.getCore().getLang().sendLangMessage("general.failedOverwrite", player, placeHolders);
				}
				this.failCount.remove(session);
			}
		});
	}
	
   /**
    * Saves the current items in the Player Inventory to be returned later.
    * 
    * @param type - The trigger executed.
    * @param player - The Player that had their items saved.
    * @param world - The world to be checked.
    * @param type - The clear type that is being executed.
    * @param craftView - The players current CraftView.
    * @param inventory - The players current Inventory.
    * @param clearAll - If ALL items are being cleared.
    */
	public void saveReturnItems(final TriggerType type, final Player player, final String world, final Inventory craftView, final PlayerInventory inventory, final int clearType) {
		boolean doReturn = StringUtils.splitIgnoreCase(ItemJoin.getCore().getConfig("config.yml").getString("Clear-Items.Options"), "RETURN_SWITCH", ",");
		List < ItemMap > protectItems = ItemUtilities.getUtilities().getProtectItems();
		if (type == TriggerType.WORLD_SWITCH && doReturn) {
			Inventory saveInventory = Bukkit.createInventory(null, 54);
			for (int i = 0; i <= 47; i++) {
				for (int k = 0; k < (!protectItems.isEmpty() ? protectItems.size() : 1); k++) {
					if (i <= 41 && inventory.getSize() >= i && ItemUtilities.getUtilities().canClear(inventory.getItem(i), String.valueOf(i), k, clearType)) {
						saveInventory.setItem(i, inventory.getItem(i).clone());
					} else if (i >= 42 && ItemUtilities.getUtilities().canClear(craftView.getItem(i - 42), "CRAFTING[" + (i - 42) + "]", k, clearType) && PlayerHandler.isCraftingInv(player.getOpenInventory())) {
						saveInventory.setItem(i, craftView.getItem(i - 42).clone());
					}
				}
			}
			ItemJoin.getCore().getSQL().saveData(new DataObject(Table.RETURN_SWITCH_ITEMS, PlayerHandler.getPlayerID(player), world, ItemHandler.serializeInventory(saveInventory)));
		}
	}
	
   /**
    * Returns the previously removed Region Items to the Player.
    * 
    * @param type - The trigger executed.
    * @param player - The Player that had their items returned.
    * @param world - The world to be checked.
    */
	public void pasteReturnItems(final TriggerType type, final Player player, final String world) {
		if (type == TriggerType.WORLD_SWITCH && StringUtils.splitIgnoreCase(ItemJoin.getCore().getConfig("config.yml").getString("Clear-Items.Options").replace(" ", ""), "RETURN_SWITCH", ",")) {
			DataObject dataObject = (DataObject) ItemJoin.getCore().getSQL().getData(new DataObject(Table.RETURN_SWITCH_ITEMS, PlayerHandler.getPlayerID(player), world, ""));
			Inventory inventory = (dataObject != null ? ItemHandler.deserializeInventory(dataObject.getInventory64().replace(world + ".", "")) : null);
			for (int i = 47; i >= 0; i--) {
				if (inventory != null && inventory.getItem(i) != null && inventory.getItem(i).getType() != Material.AIR) {
					if (i <= 41) {
						player.getInventory().setItem(i, inventory.getItem(i).clone());
					} else if (i >= 42 && PlayerHandler.isCraftingInv(player.getOpenInventory())) {
						player.getOpenInventory().getTopInventory().setItem(i - 42, inventory.getItem(i).clone());
						PlayerHandler.updateInventory(player, 1L);
					}
				}
				ItemJoin.getCore().getSQL().removeData(new DataObject(Table.RETURN_SWITCH_ITEMS, PlayerHandler.getPlayerID(player), world, ""));
			}
		}
	}
	
   /**
    * Sets the ItemMap to the Player Inventory Slots.
    * 
    * @param player - The Player to have the ItemMap set to their Inventory slot(s).
    * @param itemMap - The ItemMap to be given to the Player.
    * @param size - The expected stack size of the item.
    */
	public void setInvSlots(final Player player, final ItemMap itemMap, final int size) {
		SchedulerUtils.run(() -> {
			boolean isGiven = false;
			ItemStack existingItem = ItemHandler.getItem(player, itemMap.getSlot());
			ItemStack item = itemMap.getItem(player).clone();
			this.shiftItem(player, itemMap);
			int nextSlot = this.nextItem(player, itemMap);
			boolean overWrite = itemMap.isOverwritable() || ItemJoin.getCore().getConfig("items.yml").getBoolean("items-Overwrite");
			if (size > 1) { item.setAmount(size); }
			if ((size > 1 || itemMap.isAlwaysGive()) && !overWrite && existingItem != null) {
				isGiven = true;
				player.getInventory().addItem(item);
			} else if (nextSlot != 0) {
				isGiven = true;
				player.getInventory().setItem(nextSlot, item);
			} else if (player.getInventory().firstEmpty() != -1 || overWrite) {
				if (itemMap.getSlot().contains("%")) {
					String slot = StringUtils.translateLayout(itemMap.getSlot(), player);
					if (StringUtils.isInt(slot)) {
						isGiven = true;
						player.getInventory().setItem(Integer.parseInt(slot), item);
					}
				} else {
					isGiven = true;
					player.getInventory().setItem(Integer.parseInt(itemMap.getSlot()), item);
				}
			} else if (itemMap.isDropFull()) { 
				isGiven = true;
				player.getWorld().dropItem(player.getLocation(), item);
			}
			if (isGiven) {
				ServerUtils.logDebug("{ItemMap} " + player.getName() + " has been given the item " + itemMap.getConfigName() + " in the world [" + player.getWorld().getName() + "].");
				DataObject ipLimit = (DataObject) ItemJoin.getCore().getSQL().getData(new DataObject(Table.IP_LIMITS, PlayerHandler.getPlayerID(player), player.getWorld().getName(), itemMap.getConfigName(), player.getAddress().getHostString()));
				if ((itemMap.isOnlyFirstJoin() || itemMap.isOnlyFirstLife())) { ItemJoin.getCore().getSQL().saveData(new DataObject(Table.FIRST_JOIN, PlayerHandler.getPlayerID(player), "", itemMap.getConfigName())); }
				if (itemMap.isOnlyFirstWorld()) { ItemJoin.getCore().getSQL().saveData(new DataObject(Table.FIRST_WORLD, PlayerHandler.getPlayerID(player), player.getWorld().getName(), itemMap.getConfigName())); }
				if (itemMap.isIpLimted() && ipLimit == null) { ItemJoin.getCore().getSQL().saveData(new DataObject(Table.IP_LIMITS, PlayerHandler.getPlayerID(player), player.getWorld().getName(), itemMap.getConfigName(), player.getAddress().getHostString())); }
			}
		});
	}
	
   /**
    * Sets the ItemMap to the Player Inventory Custom Slots.
    * 
    * @param player - The Player to have the ItemMap set to their Inventory slot(s).
    * @param itemMap - The ItemMap to be given to the Player.
    * @param size - The expected stack size of the item.
    */
	public void setCustomSlots(final Player player, final ItemMap itemMap, final int size) {
		SchedulerUtils.run(() -> {
			boolean isGiven = false;
			int craftSlot = StringUtils.getSlotConversion(itemMap.getSlot());
			ItemStack existingItem = ItemHandler.getItem(player, itemMap.getSlot());
			ItemStack item = itemMap.getItem(player).clone();
			this.shiftItem(player, itemMap);
			int nextSlot = this.nextItem(player, itemMap);
			boolean overWrite = itemMap.isOverwritable() || ItemJoin.getCore().getConfig("items.yml").getBoolean("items-Overwrite");
			if (size > 1) { item.setAmount(size); }
			if ((size > 1 || itemMap.isAlwaysGive()) && !overWrite && existingItem != null) {
				isGiven = true;
				player.getInventory().addItem(item);
			} else if (nextSlot != 0) {
				isGiven = true;
				player.getInventory().setItem(nextSlot, item);
			} else if (CustomSlot.ARBITRARY.isSlot(itemMap.getSlot()) && player.getInventory().firstEmpty() != -1) {
				isGiven = true;
				player.getInventory().addItem(item);
			} else if (CustomSlot.HELMET.isSlot(itemMap.getSlot()) && (existingItem == null || overWrite)) {
				isGiven = true;
				player.getEquipment().setHelmet(item);
			} else if (CustomSlot.CHESTPLATE.isSlot(itemMap.getSlot()) && (existingItem == null || overWrite)) {
				isGiven = true;
				player.getEquipment().setChestplate(item);
			} else if (CustomSlot.LEGGINGS.isSlot(itemMap.getSlot()) && (existingItem == null || overWrite)) {
				isGiven = true;
				player.getEquipment().setLeggings(item);
			} else if (CustomSlot.BOOTS.isSlot(itemMap.getSlot()) && (existingItem == null || overWrite)) {
				isGiven = true;
				player.getEquipment().setBoots(item);
			} else if (ServerUtils.hasSpecificUpdate("1_9") && CustomSlot.OFFHAND.isSlot(itemMap.getSlot()) && (existingItem == null || overWrite)) {
				isGiven = true;
				PlayerHandler.setOffHandItem(player, item);
			} else if (craftSlot != -1 && (existingItem == null || overWrite || craftSlot == 0)) {
				isGiven = true;
				SchedulerUtils.runLater(6L, () -> {
					this.setCraftingSlots(player, item, craftSlot, 240);
				});
			} else if (itemMap.isDropFull()) {
				isGiven = true;
				player.getWorld().dropItem(player.getLocation(), item);
			}
			if (isGiven) {
				ServerUtils.logDebug("{ItemMap} " + player.getName() + " has been given the item " + itemMap.getConfigName() + " in the world [" + player.getWorld().getName() + "].");
				DataObject ipLimit = (DataObject) ItemJoin.getCore().getSQL().getData(new DataObject(Table.IP_LIMITS, PlayerHandler.getPlayerID(player), player.getWorld().getName(), itemMap.getConfigName(), player.getAddress().getHostString()));
				if ((itemMap.isOnlyFirstJoin() || itemMap.isOnlyFirstLife())) { ItemJoin.getCore().getSQL().saveData(new DataObject(Table.FIRST_JOIN, PlayerHandler.getPlayerID(player), "", itemMap.getConfigName())); }
				if (itemMap.isOnlyFirstWorld()) { ItemJoin.getCore().getSQL().saveData(new DataObject(Table.FIRST_WORLD, PlayerHandler.getPlayerID(player), player.getWorld().getName(), itemMap.getConfigName())); }
				if (itemMap.isIpLimted() && ipLimit == null) { ItemJoin.getCore().getSQL().saveData(new DataObject(Table.IP_LIMITS, PlayerHandler.getPlayerID(player), player.getWorld().getName(), itemMap.getConfigName(), player.getAddress().getHostString())); }
			}
		});
	}
	
   /**
    * Sets the ItemStack to the Player Inventory Crafting Slots.
    * 
    * @param player - The Player to have the ItemStack set to their Crafting slot(s).
    * @param itemStack - The ItemStack to be given to the Player.
    * @param craftSlot - The designated slot for the Crafting Item.
    * @param attempts - The number of attempts to give the Crafting Slot item before failing.
    */
	private void setCraftingSlots(final Player player, final ItemStack itemStack, final int craftSlot, int attempts) {
		if (attempts != 0) {
			if (craftSlot == 0) {
				SchedulerUtils.runLater(2L, () -> {
					if (PlayerHandler.isCraftingInv(player.getOpenInventory())) {
						player.getOpenInventory().getTopInventory().setItem(craftSlot, itemStack);
						PlayerHandler.updateInventory(player, 1L);
					} else { 
						SchedulerUtils.runLater(20L, () -> this.setCraftingSlots(player, itemStack, craftSlot, (attempts - 1))); 
					}
				});
			} else if (PlayerHandler.isCraftingInv(player.getOpenInventory())) {
				if (player.getOpenInventory().getTopInventory().getItem(0) != null && !player.getOpenInventory().getTopInventory().getItem(0).getType().equals(Material.AIR)) {
					ItemHandler.returnCraftingItem(player, 0, player.getOpenInventory().getTopInventory().getItem(0).clone(), 0L);
				}
				player.getOpenInventory().getTopInventory().setItem(craftSlot, itemStack);
			} else { 		
				SchedulerUtils.runLater(20L, () -> this.setCraftingSlots(player, itemStack, craftSlot, (attempts - 1)));
			}
		}
	}
	
   /**
    * Moves any pre-exiting items in the players defined ItemMap slot to the next available slot.
    * 
    * @param player - The Player to have the ItemMap set to their Inventory slot(s).
    * @param itemMap - The ItemMap to be given to the Player.
    */
	public void shiftItem(final Player player, final ItemMap itemMap) {
		int i = 0; int k = 0;
		if (StringUtils.isInt(itemMap.getSlot())) { i = Integer.parseInt(itemMap.getSlot()); k = i; }
		else if (itemMap.getSlot().contains("%")) {
			String slot = StringUtils.translateLayout(itemMap.getSlot(), player);
			if (StringUtils.isInt(slot)) {
				i = Integer.parseInt(slot); k = i;
			}
		} 
		ItemStack existingItem = ItemHandler.getItem(player, itemMap.getSlot());
		if (itemMap.isMoveNext() && existingItem != null && player.getInventory().firstEmpty() != -1) {
			for (i = 0; i <= 35; i++) {
				if (player.getInventory().getItem(i) == null || player.getInventory().getItem(i).getType() == Material.AIR) {
					player.getInventory().setItem(i, existingItem);
					existingItem.setAmount(0);
					existingItem.setType(Material.AIR);
					existingItem.setData(new ItemStack(Material.AIR).getData());
					return;
				} else if (i == 35) {
					for (k = 0; k >= 0; k--) {
						if (player.getInventory().getItem(k) == null || player.getInventory().getItem(k).getType() == Material.AIR) {
							player.getInventory().setItem(k, existingItem);
							existingItem.setAmount(0);
							existingItem.setType(Material.AIR);
							existingItem.setData(new ItemStack(Material.AIR).getData());
							return;
						}
					}
				}
			}
		}
	}
	
   /**
    * Gets the next available inventory slot from the defined ItemMap slot,
    * only if the give-next itemflag is defined.
    * 
    * @param player - The Player to have the ItemMap set to their Inventory slot(s).
    * @param itemMap - The ItemMap to be given to the Player.
    * @return The next available inventory slot.
    */
	public int nextItem(final Player player, final ItemMap itemMap) {
		int i = 0; int k = 0;
		if (StringUtils.isInt(itemMap.getSlot())) { i = Integer.parseInt(itemMap.getSlot()); k = i; }
		else if (itemMap.getSlot().contains("%")) {
			String slot = StringUtils.translateLayout(itemMap.getSlot(), player);
			if (StringUtils.isInt(slot)) {
				i = Integer.parseInt(slot); k = i;
			}
		} 
		ItemStack existingItem = ItemHandler.getItem(player, itemMap.getSlot());
		if (itemMap.isGiveNext() && existingItem != null && player.getInventory().firstEmpty() != -1) {
			for (i = 0; i <= 35; i++) {
				if (player.getInventory().getItem(i) == null || player.getInventory().getItem(i).getType() == Material.AIR) {
					return i;
				} else if (i == 35) {
					for (k = 0; k >= 0; k--) {
						if (player.getInventory().getItem(k) == null || player.getInventory().getItem(k).getType() == Material.AIR) {
							return k;
						}
					}
				}
			}
		}
		return 0;
	}
	
   /**
    * Executes the Global Commands defined in the config.
    * 
    * @param player - The Player having the commands executed.
    */
	public void triggerCommands(final Player player, TriggerType triggerRef) {
		if ((ItemJoin.getCore().getConfig("config.yml").getString("Active-Commands.enabled-worlds") != null && ItemJoin.getCore().getConfig("config.yml").getStringList("Active-Commands.commands") != null) 
				&& (!ItemJoin.getCore().getConfig("config.yml").getString("Active-Commands.enabled-worlds").equalsIgnoreCase("DISABLED") || !ItemJoin.getCore().getConfig("config.yml").getString("Active-Commands.enabled-worlds").equalsIgnoreCase("FALSE"))
				&& ((StringUtils.containsIgnoreCase(ItemJoin.getCore().getConfig("config.yml").getString("Active-Commands.triggers"), TriggerType.JOIN.name) && triggerRef.equals(TriggerType.JOIN))
				|| (StringUtils.containsIgnoreCase(ItemJoin.getCore().getConfig("config.yml").getString("Active-Commands.triggers"), TriggerType.FIRST_JOIN.name) && triggerRef.equals(TriggerType.JOIN))
				|| (StringUtils.containsIgnoreCase(ItemJoin.getCore().getConfig("config.yml").getString("Active-Commands.triggers"), TriggerType.WORLD_SWITCH.name) && triggerRef.equals(TriggerType.WORLD_SWITCH))
				|| (StringUtils.containsIgnoreCase(ItemJoin.getCore().getConfig("config.yml").getString("Active-Commands.triggers"), TriggerType.RESPAWN.name) && triggerRef.equals(TriggerType.RESPAWN))
				|| (StringUtils.containsIgnoreCase(ItemJoin.getCore().getConfig("config.yml").getString("Active-Commands.triggers"), TriggerType.TELEPORT.name) && triggerRef.equals(TriggerType.TELEPORT)))) {
			String commandsWorlds = ItemJoin.getCore().getConfig("config.yml").getString("Active-Commands.enabled-worlds").replace(", ", ",");
			TriggerType trigger = triggerRef;
			if (StringUtils.containsIgnoreCase(ItemJoin.getCore().getConfig("config.yml").getString("Active-Commands.triggers"), TriggerType.FIRST_JOIN.name) && trigger.equals(TriggerType.JOIN)) { trigger = TriggerType.FIRST_JOIN; }
			if (commandsWorlds == null) { commandsWorlds = "DISABLED"; }
			String[] compareWorlds = commandsWorlds.split(",");
			for (String compareWorld: compareWorlds) {
				if (compareWorld.equalsIgnoreCase(player.getWorld().getName()) || compareWorld.equalsIgnoreCase("ALL") || compareWorld.equalsIgnoreCase("GLOBAL")) {
					ArrayList < String > commandArray = new ArrayList < String >();
					for (String cmd : ItemJoin.getCore().getConfig("config.yml").getStringList("Active-Commands.commands")) { commandArray.add(cmd); }
					List<String> commandList = this.getRandomMap(commandArray, player);
					for (String commands: commandList) {
						final HashMap<Executor, String> commandMap = this.getCommandMap(commands);
						for (Executor executor : commandMap.keySet()) {
							final String formatCommand = StringUtils.translateLayout(commandMap.get(executor), player);
							final DataObject dataObject = ((trigger.equals(TriggerType.FIRST_JOIN) || executor.equals(Executor.FIRSTJOIN)) ? (DataObject) ItemJoin.getCore().getSQL().getData(new DataObject(Table.FIRST_COMMANDS, PlayerHandler.getPlayerID(player), player.getWorld().getName(), formatCommand)): null);
							if (dataObject == null) {
								if (trigger.equals(TriggerType.FIRST_JOIN)) { ItemJoin.getCore().getSQL().saveData(new DataObject(Table.FIRST_COMMANDS, PlayerHandler.getPlayerID(player), player.getWorld().getName(), formatCommand)); }
								if (executor.equals(Executor.DEFAULT) || executor.equals(Executor.CONSOLE) || executor.equals(Executor.FIRSTJOIN)) {
									try {
										if (StringUtils.containsIgnoreCase(formatCommand, "[close]")) {
											PlayerHandler.safeInventoryClose(player);
										} else {
											ItemData.getInfo().setLoggable(player, "/" + formatCommand);
											Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), formatCommand);
										}
									} catch (Exception e) {
										ServerUtils.logSevere("{ItemUtilities} There was an error executing a command as console, if this continues report it to the developer.");
										ServerUtils.sendDebugTrace(e);
									}
								} else if (executor.equals(Executor.OP)) {
									try {
										if (StringUtils.containsIgnoreCase(formatCommand, "[close]")) {
											PlayerHandler.safeInventoryClose(player);
										} else {
											if (!player.isOp()) {
												try {
													player.setOp(true);
													ItemData.getInfo().setLoggable(player, "/" + formatCommand);
													player.chat("/" + formatCommand);
												} catch (Exception e) {
													ServerUtils.sendDebugTrace(e);
													player.setOp(false);
													ServerUtils.logSevere("{ItemUtilities} An critical error has occurred while setting " + player.getName() + " status on the OP list, to maintain server security they have been removed as an OP.");
												} finally { player.setOp(false); }
											} else {
												try {
													if (StringUtils.containsIgnoreCase(formatCommand, "[close]")) {
														PlayerHandler.safeInventoryClose(player);
													} else {
														ItemData.getInfo().setLoggable(player, "/" + formatCommand);
														player.chat("/" + formatCommand);
													}
												} catch (Exception e) {
													ServerUtils.logSevere("{ItemUtilities} There was an error executing an item's command as a player, if this continues report it to the developer.");
													ServerUtils.sendDebugTrace(e);
												}
											}
										}
									} catch (Exception e) {
										ServerUtils.logSevere("{ItemUtilities} There was an error executing an item's command as an op, if this continues report it to the developer.");
										ServerUtils.sendDebugTrace(e);
									}
								} else if (executor.equals(Executor.PLAYER)) {
									try {
										if (StringUtils.containsIgnoreCase(formatCommand, "[close]")) {
											PlayerHandler.safeInventoryClose(player);
										} else {
											ItemData.getInfo().setLoggable(player, "/" + formatCommand);
											player.chat("/" + formatCommand);
										}
									} catch (Exception e) {
										ServerUtils.logSevere("{ItemUtilities} There was an error executing an item's command as a player, if this continues report it to the developer.");
										ServerUtils.sendDebugTrace(e);
									}
								} else if (executor.equals(Executor.SERVERSWITCH)) {
									try { 
										ItemJoin.getCore().getBungee().SwitchServers(player, formatCommand); 
									} 
									catch (Exception e) {
										ServerUtils.logSevere("{ItemUtilities} There was an error executing an item's command to switch servers, if this continues report it to the developer.");
										ServerUtils.sendDebugTrace(e);
									}
								} else if (executor.equals(Executor.BUNGEE)) {
									try { 
										ItemJoin.getCore().getBungee().ExecuteCommand(player, formatCommand); 
									} 
									catch (Exception e) {
										ServerUtils.logSevere("{ItemUtilities} There was an error executing an item's command to BungeeCord, if this continues report it to the developer.");
										ServerUtils.sendDebugTrace(e);
									}
								} else if (executor.equals(Executor.MESSAGE)) {
									try { 
										String jsonMessage = ItemData.getInfo().getJSONMessage(formatCommand, "Active-Commands");
										Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(),"minecraft:tellraw " + player.getName() + " " + jsonMessage);
									} 
									catch (Exception e) {
										ServerUtils.logSevere("{ItemUtilities} There was an error executing an item's command to send a message, if this continues report it to the developer.");
										ServerUtils.sendDebugTrace(e);
									}
								}
							}
						}
					}
					break;
				}
			}
		}
	}
	
   /**
	* Gets the exact command line from the string input.
	* 
	* @param input - the raw command line input.
	* @return The exact command mapping.
	*/
	private HashMap<Executor, String> getCommandMap(String input) {
		input = input.trim();
		Executor type = Executor.DEFAULT;
		HashMap<Executor, String> commandMap = new HashMap<Executor, String>();
		if (input.startsWith("default:")) { input = input.substring(8); type = Executor.DEFAULT; } 
		else if (input.startsWith("console:")) { input = input.substring(8); type = Executor.CONSOLE; } 
		else if (input.startsWith("op:")) { input = input.substring(3); type = Executor.OP; } 
		else if (input.startsWith("player:")) { input = input.substring(7); type = Executor.PLAYER; } 
		else if (input.startsWith("server:")) { input = input.substring(7); type = Executor.SERVERSWITCH; } 
		else if (input.startsWith("bungee:")) { input = input.substring(7); type = Executor.BUNGEE; } 
		else if (input.startsWith("message:")) { input = input.substring(8); type = Executor.MESSAGE; } 
		else if (input.startsWith("first-join:")) { input = input.substring(11); type = Executor.FIRSTJOIN; }
		input = input.trim();
		input = StringUtils.colorFormat(input);
		commandMap.put(type, input);
		return commandMap;
	}
	
   /**
    * Randomly Selected a Command Entry for a Single Command.
    * 
    * @param commands - The commands to have an entry randomly selected.
    * @param player - The Player having their commands randomly selected.
    * @return The newly generated ArrayList.
    */
    private List < String > getRandomMap(final ArrayList < String > commands, final Player player) {
    	final String commandSequence = ItemJoin.getCore().getConfig("config.yml").getString("Active-Commands.commands-sequence");
    	if (commandSequence != null && commandSequence.replace(" ", "").equalsIgnoreCase("RANDOM_SINGLE")) {
	    	String dedicatedMap = (String) StringUtils.randomEntry(commands);
	    	if (dedicatedMap != null && dedicatedMap != null && player != null) {
	    		List<String> returnList = new ArrayList<String>();
	    		returnList.add(dedicatedMap);
	    		return returnList;
	    	}
    	}
    	return ItemJoin.getCore().getConfig("config.yml").getStringList("Active-Commands.commands");
    }
	
   /**
    * Gets the ItemDelay.
    * 
    * @return The items-Delay that is to be expected before the items are given.
    */
	private long getItemDelay() {
		if ((this.getClearDelay() / 20) >= (ItemJoin.getCore().getConfig("items.yml").getInt("items-Delay") / 2) && this.getClearDelay() != -1) { return this.getClearDelay() + 1; }
		return ItemJoin.getCore().getConfig("items.yml").getInt("items-Delay") * 10L;
	}
	
   /**
    * Gets the Delay before clearing the items.
    * 
    * @return The clear-Delay before the items are cleared.
    */
	public long getClearDelay() {
		if (!StringUtils.containsIgnoreCase(ItemJoin.getCore().getConfig("config.yml").getString("Clear-Items.Join"), "DISABLED") 
				&& !StringUtils.containsIgnoreCase(ItemJoin.getCore().getConfig("config.yml").getString("Clear-Items.Join"), "FALSE")
				|| !StringUtils.containsIgnoreCase(ItemJoin.getCore().getConfig("config.yml").getString("Clear-Items.Quit"), "DISABLED")
				&& !StringUtils.containsIgnoreCase(ItemJoin.getCore().getConfig("config.yml").getString("Clear-Items.Quit"), "FALSE")
				|| !StringUtils.containsIgnoreCase(ItemJoin.getCore().getConfig("config.yml").getString("Clear-Items.World-Switch"), "DISABLED")
				&& !StringUtils.containsIgnoreCase(ItemJoin.getCore().getConfig("config.yml").getString("Clear-Items.World-Switch"), "FALSE")
				|| !StringUtils.containsIgnoreCase(ItemJoin.getCore().getConfig("config.yml").getString("Clear-Items.Region-Enter"), "DISABLED")
				&& !StringUtils.containsIgnoreCase(ItemJoin.getCore().getConfig("config.yml").getString("Clear-Items.Region-Enter"), "FALSE")) {
				return ItemJoin.getCore().getConfig("config.yml").getInt("Clear-Items.Delay-Tick");
		}
		return -1;
	}
	
   /**
    * Checks the ItemStack to see if it is a protected item.
    * 
    * @param i - The position in the protectItems List when cycling.
    * @param item - The ItemStack being checked.
    * @return If the ItemStack is a protected ItemMap.
    */
	public boolean isProtected(final int i, final ItemStack item) {
			return !this.protectItems.isEmpty() && this.protectItems.get(i).isSimilar(null, item) && i == (this.protectItems.size() - 1);
	}
	
   /**
    * Gets the list of ItemMaps that should be protected from clearing.
    * 
    * @return The list of protected ItemMaps.
    */
	public List<ItemMap> getProtectItems() {
		List<ItemMap> protectItems = new ArrayList<ItemMap>();
		if (StringUtils.containsIgnoreCase(ItemJoin.getCore().getConfig("config.yml").getString("Clear-Items.Options"), "PROTECT")) {
			for (ItemMap item: this.getItems()) {
				if (item.isOnlyFirstJoin() || item.isOnlyFirstLife() || item.isOnlyFirstWorld()) {
					protectItems.add(item);
				}
			}
		}
		return protectItems;
	}
	
   /**
    * Clears any crafting recipes for the custom items.
    * 
    */
  	public void clearRecipes() {
  		final List < Recipe > backupRecipes = new ArrayList < Recipe > ();
  		final Iterator < Recipe > recipes = Bukkit.getServer().recipeIterator();
  		boolean itemRecipes = false;
  		while (recipes.hasNext()) {
  			final Recipe nextRecipe = recipes.next();
  			if (nextRecipe instanceof ShapedRecipe) {
  				final ShapedRecipe recipe = (ShapedRecipe) nextRecipe;
  				final ItemStack result = recipe.getResult();
  				boolean backupItem = true;
  				for (final ItemMap itemMap: this.getItems()) {
  					if (itemMap.getIngredients() != null && !itemMap.getIngredients().isEmpty()) {
  						itemRecipes = true;
  						if (itemMap.isSimilar(null, result) || recipe.getKey().getKey().contains(itemMap.getConfigName())) {
  							backupItem = false;
  						}
  					}
  				}
  				if (backupItem) {
	  				backupRecipes.add(recipe);
	  			}
  			}
  		}
  		if (itemRecipes) {
  			Bukkit.getServer().clearRecipes(); {
	  			for (final Recipe recipe: backupRecipes) {
	  				try {
	  					Bukkit.getServer().addRecipe(recipe);
	  				} catch (IllegalStateException e) { }
	  			}
  			}
  		}
  	}
	
   /**
    * Creates a duplicate items HashMap.
    * 
    * @return The duplicated items HashMap.
    */
	public List < ItemMap > copyItems() {
		List < ItemMap > itemsCopy = new ArrayList < ItemMap > ();
		for (ItemMap itemMap : this.items) { 
			itemsCopy.add(itemMap.clone());
		}
		return itemsCopy;
	}
	
   /**
    * Adds a new item to the items List.
    * 
    * @param itemMap - The ItemMap to be added to the items List.
    */
	public void addItem(final ItemMap itemMap) {
		this.items.add(itemMap);
	}
	
   /**
    * Adds a new item to the crafting items List.
    * 
    * @param itemMap - The crafting ItemMap to be added to the items List.
    */
	public void addCraftingItem(final ItemMap itemMap) {
		if (itemMap.isCraftingItem()) { 
			this.craftingItems.add(itemMap); 
		}
	}
	
   /**
    * Gets the current ItemMap List.
    * 
    * @return The current ItemMap list.
    */
	public List < ItemMap > getItems() {
		return this.items;
	}
	
   /**
    * Gets the current crafting ItemMap List.
    * 
    * @return The current crafting ItemMap list.
    */
	public List < ItemMap > getCraftingItems() {
		return this.craftingItems;
	}

   /**
    * Clears the existing ItemMaps for the items List.
    * 
    */
	public void clearItems() {
		this.clearRecipes(); {
			this.items = new ArrayList < ItemMap >();
			this.craftingItems = new ArrayList < ItemMap >();
		}
	}
	
   /**
    * Trigger types.
    * 
    */
	public enum CustomSlot {
		HELMET("Helmet"),
		CHESTPLATE("Chestplate"),
		LEGGINGS("Leggings"),
		BOOTS("Boots"),
		OFFHAND("Offhand"),
		CRAFTING("Crafting"),
		ARBITRARY("Arbitrary");
		private final String name;
		public boolean isSlot(String slot) { return this.name.equalsIgnoreCase(slot); }
		private CustomSlot(String name) { this.name = name; }
	}	
	
   /**
    * Trigger types.
    * 
    */
	public enum TriggerType {
		FIRST_JOIN("First-Join"),
		JOIN("Join"),
		QUIT("Quit"),
		RESPAWN("Respawn"),
		TELEPORT("Teleport"),
		WORLD_SWITCH("World-Switch"),
		LIMIT_SWITCH("Limit-Modes"),
		REGION_ENTER("Region-Enter"),
		REGION_LEAVE("Region-Leave"),
		DEFAULT("DEFAULT");
		private final String name;
		private TriggerType(String name) { this.name = name; }
	}	
	
   /**
    * Gets the instance of the ItemStatistics for the Player.
    * 
    * @param player - The player being referenced.
    * @return The ItemStatistics instance.
    */
    public ItemStatistics getStatistics(final Player player) { 
    	if (player != null && !this.itemStats.containsKey(PlayerHandler.getPlayerID(player))) {
    		this.itemStats.put(PlayerHandler.getPlayerID(player), new ItemStatistics(player, this.getItems()));
    	}
        return (player != null ? this.itemStats.get(PlayerHandler.getPlayerID(player)) : null); 
    } 
	
   /**
    * Gets the instance of the ItemUtilities.
    * 
    * @return The ItemUtilities instance.
    */
    public static ItemUtilities getUtilities() { 
        if (utilities == null) { utilities = new ItemUtilities(); }
        return utilities; 
    } 
}