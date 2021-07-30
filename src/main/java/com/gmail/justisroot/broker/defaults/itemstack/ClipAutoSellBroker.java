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
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.gmail.justisroot.broker.TransactionRecord;
import com.gmail.justisroot.broker.TransactionRecord.TransactionRecordBuilder;

import me.clip.autosell.AutoSell;
import me.clip.autosell.SellHandler;
import me.clip.autosell.objects.Shop;

/**
 * Clip's AutoSell, as the name might suggest, only handles sales.
 */
public final class ClipAutoSellBroker extends ItemBroker {

	private AutoSell plugin;

	public ClipAutoSellBroker() {
		super("me.clip.autosell.AutoSell", "me.clip.autosell.SellHandler", "me.clip.autosell.objects.Shop");
		if (!isAvailable()) return;
		plugin = (AutoSell) plugin();
	}

	@Override
	public String getProvider() {
		return "AutoSell";
	}

	@Override
	public boolean canBeBought(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item) {
		return false;
	}

	@Override
	public boolean canBeSold(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item) {
		return getSellPrice(playerID, worldID, item, 1).isPresent();
	}

	@Override
	public Optional<BigDecimal> getBuyPrice(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item, int amount) {
		return Optional.empty();
	}

	@Override
	@SuppressWarnings("deprecation")
	public Optional<BigDecimal> getSellPrice(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item, int amount) {
		if (playerID.isEmpty()) return Optional.empty();
		Player player = Bukkit.getPlayer(playerID.get());
		if (player == null) return Optional.empty();
		Shop shop = SellHandler.getShop(player);
		if (shop == null && plugin.getOptions().sellAllFallbackToPermShop())
			shop = SellHandler.getPermShop(player);
		if (shop == null) return Optional.empty();
		for (Entry<ItemStack, Double> compare : shop.getPrices().entrySet()) {
			if (compare.getValue() <= 0) continue;
			ItemStack converted = compare.getKey().clone();
			Material convertedMat = Bukkit.getUnsafe().fromLegacy(converted.getType());
			if (convertedMat != converted.getType()) converted.setType(convertedMat);
			if (item.equals(converted)) return Optional.of(new BigDecimal(compare.getValue() * amount));
		}
		return Optional.empty();
	}

	@Override
	public TransactionRecord<ItemStack> buy(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item, int amount) {
		return TransactionRecord.startPurchase(this, item, playerID, worldID).setVolume(amount).buildFailure(NO_PERMISSION);
	}

	@Override
	public TransactionRecord<ItemStack> sell(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item, int amount) {
		TransactionRecordBuilder<ItemStack> builder = TransactionRecord.startSale(this, item, playerID, worldID).setVolume(amount);
		Optional<BigDecimal> value = getSellPrice(playerID, worldID, item, amount);
		if (value.isEmpty()) return builder.buildFailure(NO_PERMISSION);
		return builder.setValue(value.get()).buildSuccess(null);
	}

	@Override
	public String getDisplayName(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item) {
		return displayName(item);
	}

	@Override
	public boolean handlesPurchases(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item) {
		return false;
	}

	@Override
	public boolean handlesSales(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item) {
		return true;
	}

}
