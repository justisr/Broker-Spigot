/*
 * BrokerAPI Copyright 2020 Justis Root
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without
 * limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *
 */
package com.gmail.justisroot.broker.defaults.itemstack;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.gmail.justisroot.broker.TransactionRecord;
import com.gmail.justisroot.broker.TransactionRecord.TransactionRecordBuilder;

import me.sat7.dynamicshop.DynaShopAPI;
import me.sat7.dynamicshop.utilities.ShopUtil;
import me.sat7.dynamicshop.utilities.SoundUtil;

/**
 * Adjusts prices through DynamicShop with every purchase and sale.<br>
 * Works as expected only when items aren't duplicated across shops.<br>
 * Respects tax, shop balance and stock. Ignores delivery fee and shop hours since those depend on transaction method.
 */
public final class SSDynamicShopBroker extends ItemBroker {

	public SSDynamicShopBroker() {
		super("me.sat7.dynamicshop.DynaShopAPI", "me.sat7.dynamicshop.utilities.ShopUtil", "me.sat7.dynamicshop.utilities.SoundUtil");
	}

	@Override
	public String getProvider() {
		return "DynamicShop";
	}

	private static final Map<ItemStack, String> getShopItems() {
		Map<ItemStack, String> items = new HashMap<>();
		for (String shop : DynaShopAPI.getShops()) {
			for (ItemStack stack : DynaShopAPI.getShopItems(shop))
				items.put(stack, shop);
		}
		return items;
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
		String shop = getShopItems().get(item);
		if (shop == null) return Optional.empty();
		int index = ShopUtil.findItemFromShop(shop, item);
		if (index < 0) return Optional.empty();
		double value = buyPrice(shop, index, amount);
		if (value <= 0) return Optional.empty();
		int stock = stock(shop, item, index);
		if (stock > 0 && stock <= amount) return Optional.empty();
		return Optional.of(new BigDecimal(value));
	}

	@Override
	public Optional<BigDecimal> getSellPrice(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item, int amount) {
		String shop = getShopItems().get(item);
		if (shop == null) return Optional.empty();
		int index = ShopUtil.findItemFromShop(shop, item);
		if (index < 0) return Optional.empty();
		double value = sellPrice(shop, index, amount);
		if (value <= 0) return Optional.empty();
		int stock = stock(shop, item, index);
		if (stock > 0 && stock <= amount) return Optional.empty();
		return Optional.of(new BigDecimal(value));
	}

	@Override
	public TransactionRecord<ItemStack> buy(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item, int amount) {
		TransactionRecordBuilder<ItemStack> record = TransactionRecord.startPurchase(this, item, playerID, worldID).setVolume(amount);
		String shop = getShopItems().get(item);
		int index = ShopUtil.findItemFromShop(shop, item);
		if (shop == null) return record.buildFailure(NO_PERMISSION);
		double value = buyPrice(shop, index, amount);
		if (value <= 0) return record.buildFailure(NO_PERMISSION);
		int stock = stock(shop, item, index);
		if (stock > 0 && stock <= amount) return record.buildFailure(NO_PERMISSION);
		return record.setValue(new BigDecimal(value)).buildSuccess(() -> {
			buy(item, amount, shop, index, stock, value);
			if (playerID.isEmpty()) return;
			Player player = Bukkit.getPlayer(playerID.get());
			if (player != null) SoundUtil.playerSoundEffect(player, "buy");
		});
	}

	@Override
	public TransactionRecord<ItemStack> sell(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item, int amount) {
		TransactionRecordBuilder<ItemStack> record = TransactionRecord.startSale(this, item, playerID, worldID).setVolume(amount);
		String shop = getShopItems().get(item);
		int index = ShopUtil.findItemFromShop(shop, item);
		if (shop == null) return record.buildFailure(NO_PERMISSION);
		double value = sellPrice(shop, index, amount);
		if (value <= 0) return record.buildFailure(NO_PERMISSION);
		int stock = stock(shop, item, index);
		if (stock > 0 && stock <= amount) return record.buildFailure(NO_PERMISSION);
		return record.setValue(new BigDecimal(value)).buildSuccess(() -> {
			sell(item, amount, shop, index, stock, value);
			if (playerID.isEmpty()) return;
			Player player = Bukkit.getPlayer(playerID.get());
			if (player != null) SoundUtil.playerSoundEffect(player, "sell");
		});
	}

	private static void buy(ItemStack item, int amount, String shop, int index, int stock, double price) {
		if (stock > 0) ShopUtil.ccShop.get().set(shop + "." + index + ".stock", stock - amount);
		if (shopHasBalance(shop)) ShopUtil.addShopBalance(shop, price);
		ShopUtil.ccShop.save();
	}

	private static void sell(ItemStack item, int amount, String shop, int index, int stock, double price) {
		if (stock > 0) ShopUtil.ccShop.get().set(shop + "." + index + ".stock", stock + amount);
		if (shopHasBalance(shop)) ShopUtil.addShopBalance(shop, -price);
		ShopUtil.ccShop.save();
	}

	private static boolean shopHasBalance(String shop) {
		return ShopUtil.ccShop.get().contains(shop + ".Options.Balance");
	}

	private static int stock(String shopName, ItemStack item, int index) {
		return ShopUtil.ccShop.get().getInt(shopName + "." + index + ".stock");
	}

	private static double buyPrice(String shop, int index, int amount) {
		return getPrice(shop, index, amount, false);
	}

	private static double sellPrice(String shop, int index, int amount) {
		return getPrice(shop, index, amount, true);
	}

	// Improved performance compared to what's available through DynamicShop's API
	private static double getPrice(String shop, int index, int amount, boolean sell) {
		double value = ShopUtil.ccShop.get().getDouble(shop + "." + index + ".value");
		if (sell && ShopUtil.ccShop.get().contains(shop + "." + index + ".value2"))
			value = ShopUtil.ccShop.get().getDouble(shop + "." + index + ".value2");
		double min = ShopUtil.ccShop.get().getDouble(shop + "." + index + ".valueMin");
		double max = ShopUtil.ccShop.get().getDouble(shop + "." + index + ".valueMax");
		double median = ShopUtil.ccShop.get().getInt(shop + "." + index + ".median");
		double stock = ShopUtil.ccShop.get().getInt(shop + "." + index + ".stock");
		double price = 0;
		for (int i = 0; i < amount; i++) {
			if (max != 0 && price > max) {
				price += max;
			} else if (min != 0 && price < min) {
				price += min;
			} else if (median > 0 && stock > 1) {
				price += median / stock * value;
			} else {
				price += value;
			}
			if (sell) stock++;
			else stock--;
		}
		if (sell && !ShopUtil.ccShop.get().contains(shop + "." + index + ".value2"))
			price -= price / 100D * DynaShopAPI.getTaxRate(shop);
		return Math.round(price * 100D) / 100D;
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
