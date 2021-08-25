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

import com.Zrips.CMI.CMI;
import com.Zrips.CMI.Modules.Worth.WorthItem;
import com.gmail.justisroot.broker.record.PurchaseRecord;
import com.gmail.justisroot.broker.record.PurchaseRecord.PurchaseRecordBuilder;
import com.gmail.justisroot.broker.record.SaleRecord;
import com.gmail.justisroot.broker.record.SaleRecord.SaleRecordBuilder;

public class CMIBroker extends ItemBroker {

	public CMIBroker() {
		super("com.Zrips.CMI.CMI", "com.Zrips.CMI.Modules.Worth.WorthItem");
	}

	@Override
	public String getProvider() {
		return "CMI";
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
		WorthItem wi = CMI.getInstance().getWorthManager().getWorth(item);
		if (wi == null) return Optional.empty();
		double value = wi.getBuyPrice();
		if (value <= 0) return Optional.empty();
		return Optional.of(new BigDecimal(value * amount));
	}

	@Override
	public Optional<BigDecimal> getSellPrice(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item, int amount) {
		WorthItem wi = CMI.getInstance().getWorthManager().getWorth(item);
		if (wi == null) return Optional.empty();
		double value = wi.getSellPrice();
		if (value <= 0) return Optional.empty();
		return Optional.of(new BigDecimal(value * amount));
	}

	@Override
	public PurchaseRecord<ItemStack> buy(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item, int amount) {
		PurchaseRecordBuilder<ItemStack> record = PurchaseRecord.start(this, item, playerID, worldID).setVolume(amount);
		Optional<BigDecimal> value = getBuyPrice(playerID, worldID, item, amount);
		if (value.isEmpty()) return record.buildFailure(NO_PERMISSION);
		return record.setValue(value.get()).buildSuccess();
	}

	@Override
	public SaleRecord<ItemStack> sell(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item, int amount) {
		SaleRecordBuilder<ItemStack> record = SaleRecord.start(this, item, playerID, worldID).setVolume(amount);
		Optional<BigDecimal> value = getSellPrice(playerID, worldID, item, amount);
		if (value.isEmpty()) return record.buildFailure(NO_PERMISSION);
		return record.setValue(value.get()).buildSuccess();
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
