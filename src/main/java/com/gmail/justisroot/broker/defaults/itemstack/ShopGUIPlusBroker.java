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

/**
 * Respects item permissions, sends configured commands, and plays buy/sell sounds on transaction complete.
 */
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
		return getBuyPrice(playerID, worldID, item, 1).isPresent();
	}

	@Override
	public boolean canBeSold(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item) {
		return getSellPrice(playerID, worldID, item, 1).isPresent();
	}

	@Override
	public Optional<BigDecimal> getBuyPrice(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item, int amount) {
		if (playerID.isEmpty()) return Optional.empty();
		Player player = Bukkit.getPlayer(playerID.get());
		if (player == null) return Optional.empty();
		double value = ShopGuiPlusApi.getItemStackPriceBuy(player, item);
		if (value <= 0) return Optional.empty();
		ShopItem shopItem = ShopGuiPlusApi.getItemStackShopItem(player, item);
		if (!shopItem.hasRequiredPermissions(player)) return Optional.empty();
		return Optional.of(new BigDecimal(value * amount));
	}

	@Override
	public Optional<BigDecimal> getSellPrice(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item, int amount) {
		if (playerID.isEmpty()) return Optional.empty();
		Player player = Bukkit.getPlayer(playerID.get());
		if (player == null) return Optional.empty();
		double value = ShopGuiPlusApi.getItemStackPriceSell(player, item);
		if (value <= 0) return Optional.empty();
		ShopItem shopItem = ShopGuiPlusApi.getItemStackShopItem(player, item);
		if (!shopItem.hasRequiredPermissions(player)) return Optional.empty();
		return Optional.of(new BigDecimal(value * amount));
	}

	@Override
	public TransactionRecord<ItemStack> buy(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item, int amount) {
		TransactionRecordBuilder<ItemStack> builder = TransactionRecord.startPurchase(this, item, playerID, worldID).setVolume(amount);
		Optional<BigDecimal> value = getBuyPrice(playerID, worldID, item, amount);
		if (value.isEmpty()) return builder.buildFailure(NO_PERMISSION);
		Player player = Bukkit.getPlayer(playerID.get());
		ShopItem shopItem = ShopGuiPlusApi.getItemStackShopItem(player, item);
		return builder.setValue(value.get()).buildSuccess(() -> {
			plugin.getSoundManager().playSound(player, SoundAction.BUY_ITEM);
			for (String command : shopItem.getCommandsOnBuyConsole()) sendCommand(Bukkit.getConsoleSender(), command, player, amount);
			for (String command : shopItem.getCommandsOnBuy()) sendCommand(player, command, player, amount);
		});
	}

	@Override
	public TransactionRecord<ItemStack> sell(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item, int amount) {
		TransactionRecordBuilder<ItemStack> builder = TransactionRecord.startSale(this, item, playerID, worldID).setVolume(amount);
		Optional<BigDecimal> value = getSellPrice(playerID, worldID, item, amount);
		if (value.isEmpty()) return builder.buildFailure(NO_PERMISSION);
		Player player = Bukkit.getPlayer(playerID.get());
		ShopItem shopItem = ShopGuiPlusApi.getItemStackShopItem(player, item);
		if (!shopItem.hasRequiredPermissions(player)) return builder.buildFailure(NO_PERMISSION);
		return builder.setValue(value.get()).buildSuccess(() -> {
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
		return displayName(item);
	}

	@Override
	public boolean handlesPurchases(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item) {
		return true;
	}

	@Override
	public boolean handlesSales(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item) {
		return true;
	}

}
