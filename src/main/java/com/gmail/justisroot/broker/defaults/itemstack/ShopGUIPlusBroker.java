/*
 * BrokerAPI Copyright 2020 Justis Root
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
package com.gmail.justisroot.broker.defaults.itemstack;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.gmail.justisroot.broker.TransactionRecord;
import com.gmail.justisroot.broker.TransactionRecord.TransactionRecordBuilder;

import net.brcdev.shopgui.ShopGuiPlugin;
import net.brcdev.shopgui.ShopGuiPlusApi;
import net.brcdev.shopgui.shop.ShopItem;
import net.brcdev.shopgui.sound.SoundAction;

public final class ShopGUIPlusBroker extends ItemBroker {

	private ShopGuiPlugin plugin;

	public ShopGUIPlusBroker() {
		super("net.brcdev.shopgui.ShopGuiPlusApi", "net.brcdev.shopgui.ShopGuiPlugin", "net.brcdev.shopgui.shop.ShopItem", "net.brcdev.shopgui.sound.SoundAction");
		if (!isAvailable()) return;
		plugin = (ShopGuiPlugin) plugin();
	}

	@Override
	public String getProvider() {
		return "ShopGUIPlus";
	}

	@Override
	public boolean canBeBought(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item) {
		if (!handlesPurchases(playerID, worldID, item)) return false;
		Player player = Bukkit.getPlayer(playerID.get());
		if (player == null) return false;
		if (ShopGuiPlusApi.getItemStackPriceBuy(player, item) > 0) return true;
		return false;
	}

	@Override
	public boolean canBeSold(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item) {
		if (!handlesPurchases(playerID, worldID, item)) return false;
		Player player = Bukkit.getPlayer(playerID.get());
		if (ShopGuiPlusApi.getItemStackPriceSell(player, item) > 0) return true;
		return false;
	}

	@Override
	public Optional<BigDecimal> getBuyPrice(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item, int amount) {
		if (playerID.isEmpty()) return Optional.empty();
		Player player = Bukkit.getPlayer(playerID.get());
		if (player == null) return Optional.empty();
		BigDecimal value = new BigDecimal(ShopGuiPlusApi.getItemStackPriceBuy(player, item));
		if (value.doubleValue() <= 0) return Optional.empty();
		return Optional.of(value.multiply(new BigDecimal(amount)));
	}

	@Override
	public Optional<BigDecimal> getSellPrice(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item, int amount) {
		if (playerID.isEmpty()) return Optional.empty();
		Player player = Bukkit.getPlayer(playerID.get());
		if (player == null) return Optional.empty();
		BigDecimal value = new BigDecimal(ShopGuiPlusApi.getItemStackPriceSell(player, item));
		if (value.doubleValue() <= 0) return Optional.empty();
		return Optional.of(value.multiply(new BigDecimal(amount)));
	}

	@Override
	public TransactionRecord<ItemStack> buy(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item, int amount) {
		TransactionRecordBuilder<ItemStack> builder = TransactionRecord.startPurchase(this, item, playerID, worldID).setVolume(amount);
		if (!canBeBought(playerID, worldID, item)) return builder.buildFailure(NO_PERMISSION);
		Optional<BigDecimal> buyPrice = getBuyPrice(playerID, worldID, item, amount);
		if (buyPrice.isEmpty()) return builder.buildFailure(NO_PERMISSION);
		Player player = Bukkit.getPlayer(playerID.get());
		ShopItem shopItem = ShopGuiPlusApi.getItemStackShopItem(player, item);
		if (!shopItem.hasRequiredPermissions(player)) return builder.buildFailure(NO_PERMISSION);
		return builder.setValue(buyPrice.get()).buildSuccess(() -> {
			plugin.getSoundManager().playSound(player, SoundAction.BUY_ITEM);
			for (String command : shopItem.getCommandsOnBuyConsole()) sendCommand(Bukkit.getConsoleSender(), command, player, amount);
			for (String command : shopItem.getCommandsOnBuy()) sendCommand(player, command, player, amount);
		});
	}

	@Override
	public TransactionRecord<ItemStack> sell(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item, int amount) {
		TransactionRecordBuilder<ItemStack> builder = TransactionRecord.startSale(this, item, playerID, worldID).setVolume(amount);
		if (!canBeBought(playerID, worldID, item)) return builder.buildFailure(NO_PERMISSION);
		Optional<BigDecimal> sellPrice = getSellPrice(playerID, worldID, item, amount);
		if (sellPrice.isEmpty()) return builder.buildFailure(NO_PERMISSION);
		Player player = Bukkit.getPlayer(playerID.get());
		ShopItem shopItem = ShopGuiPlusApi.getItemStackShopItem(player, item);
		if (!shopItem.hasRequiredPermissions(player)) return builder.buildFailure(NO_PERMISSION);
		return builder.setValue(sellPrice.get()).buildSuccess(() -> {
			plugin.getSoundManager().playSound(player, SoundAction.SELL_ITEM);
			for (String command : shopItem.getCommandsOnSellConsole()) sendCommand(Bukkit.getConsoleSender(), command, player, amount);
			for (String command : shopItem.getCommandsOnSell()) sendCommand(player, command, player, amount);
		});
	}

	private static void sendCommand(CommandSender sender, String command, Player player, double amount) {
		Bukkit.dispatchCommand(sender, command.replace("%PLAYER%", player.getName()).replace("%AMOUNT%", String.valueOf(amount)));
	}

	@Override
	public String getDisplayName(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item) {
		return WordUtils.capitalize(item.getType().toString().replace("_", " "));
	}

	@Override
	public boolean handlesPurchases(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item) {
		if (playerID.isEmpty()) return false;
		Player player = Bukkit.getPlayer(playerID.get());
		if (player == null) return false;
		if (ShopGuiPlusApi.getItemStackShopItem(player, item) != null) return true;
		return false;
	}

	@Override
	public boolean handlesSales(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item) {
		if (playerID.isEmpty()) return false;
		Player player = Bukkit.getPlayer(playerID.get());
		if (player == null) return false;
		if (ShopGuiPlusApi.getItemStackShopItem(player, item) != null) return true;
		return false;
	}

}
