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

import org.bukkit.inventory.ItemStack;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.Worth;
import com.gmail.justisroot.broker.TransactionRecord;
import com.gmail.justisroot.broker.TransactionRecord.TransactionRecordBuilder;

/**
 * Essentials only handles sale prices
 */
public final class EssentialsXBroker extends ItemBroker {

	private Definitions def;

	public EssentialsXBroker() {
		super("com.earth2me.essentials.Essentials", "com.earth2me.essentials.Worth", "com.earth2me.essentials.IEssentials");
		if (!isAvailable()) return;
		def = new Definitions(this);
	}

	@Override
	public String getProvider() {
		return "Essentials";
	}

	@Override
	public boolean canBeBought(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item) {
		return false;
	}

	@Override
	public boolean canBeSold(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item) {
		return def.getPrice(item) != null;
	}

	@Override
	public Optional<BigDecimal> getBuyPrice(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item, int amount) {
		return Optional.empty();
	}

	@Override
	public Optional<BigDecimal> getSellPrice(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item, int amount) {
		BigDecimal w = def.getPrice(item);
		if (w == null) return Optional.empty();
		return Optional.of(w.multiply(BigDecimal.valueOf(amount)));
	}

	@Override
	public TransactionRecord<ItemStack> buy(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item, int amount) {
		return TransactionRecord.startPurchase(this, item, playerID, worldID).setVolume(amount).buildFailure(NO_PERMISSION);
	}

	@Override
	public TransactionRecord<ItemStack> sell(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item, int amount) {
		TransactionRecordBuilder<ItemStack> builder = TransactionRecord.startSale(this, item, playerID, worldID).setVolume(amount);
		if (!canBeSold(playerID, worldID, item)) return builder.buildFailure(NO_PERMISSION);
		Optional<BigDecimal> sellPrice = getSellPrice(playerID, worldID, item, amount);
		if (sellPrice.isEmpty()) return builder.buildFailure(NO_PERMISSION);
		return builder.setValue(sellPrice.get()).buildSuccess(null);
	}

	@Override
	public String getDisplayName(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item) {
		return def.ess.getItemDb().name(item);
	}

	@Override
	public boolean handlesPurchases(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item) {
		return false;
	}

	@Override
	public boolean handlesSales(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item) {
		return true;
	}

	private static class Definitions {

		private Essentials ess;
		private Worth worth;

		private Definitions(EssentialsXBroker broker) {
			ess = (Essentials) broker.plugin();
			worth = ess.getWorth();
		}

		private BigDecimal getPrice(ItemStack item) {
			return worth.getPrice(ess, item);
		}
	}
}
