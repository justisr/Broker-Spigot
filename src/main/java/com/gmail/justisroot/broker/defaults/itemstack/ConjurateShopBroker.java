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
import org.bukkit.inventory.ItemStack;

import com.gmail.justisroot.broker.TransactionRecord;
import com.gmail.justisroot.broker.TransactionRecord.TransactionRecordBuilder;

import conj.Shop.control.Manager;

/**
 * Due to Conjurate's Shop plugin primarily associating prices with slots rather than items, support is currently limited.<br>
 * Only sales will be facilitated by this Broker, and only using pricing kept in worth storage (intended for /sell hand).
 */
public final class ConjurateShopBroker extends ItemBroker {

	public ConjurateShopBroker() {
		super("conj.Shop.control.Manager");
	}

	@Override
	public String getProvider() {
		return "Shop";
	}

	@Override
	public boolean canBeBought(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item) {
		return false;
	}

	@Override
	public boolean canBeSold(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item) {
		return Manager.get().getWorth(item) > 0;
	}

	@Override
	public Optional<BigDecimal> getBuyPrice(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item, int amount) {
		return Optional.empty();
	}

	@Override
	public Optional<BigDecimal> getSellPrice(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item, int amount) {
		if (!canBeSold(playerID, worldID, item)) return Optional.empty();
		double value = Manager.get().getWorth(item) * amount;
		if (value <= 0) return Optional.empty();
		return Optional.of(new BigDecimal(value));
	}

	@Override
	public TransactionRecord<ItemStack> buy(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item, int amount) {
		return TransactionRecord.startPurchase(this, item, playerID, worldID).setVolume(amount).buildFailure(NO_PERMISSION);
	}

	@Override
	public TransactionRecord<ItemStack> sell(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item, int amount) {
		TransactionRecordBuilder<ItemStack> record = TransactionRecord.startPurchase(this, item, playerID, worldID).setVolume(amount);
		if (!canBeSold(playerID, worldID, item)) return record.buildFailure(NO_PERMISSION);
		Optional<BigDecimal> sellPrice = getSellPrice(playerID, worldID, item, amount);
		if (sellPrice.isEmpty()) return record.buildFailure(NO_PERMISSION);
		return record.setValue(sellPrice.get()).buildSuccess(null);
	}

	@Override
	public String getDisplayName(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item) {
		return WordUtils.capitalize(item.getType().toString().replace("_", " "));
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
