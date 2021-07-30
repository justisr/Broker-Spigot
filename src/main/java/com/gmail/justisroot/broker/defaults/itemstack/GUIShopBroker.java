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

import com.gmail.justisroot.broker.TransactionRecord;
import com.gmail.justisroot.broker.TransactionRecord.TransactionRecordBuilder;
import com.pablo67340.guishop.api.GuiShopAPI;

/**
 * Respects GUIShop's dynamic pricing API.
 */
public final class GUIShopBroker extends ItemBroker {

	public GUIShopBroker() {
		super("com.pablo67340.guishop.api.GuiShopAPI");
	}

	@Override
	public String getProvider() {
		return "GUIShop";
	}

	@Override
	public boolean canBeBought(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item) {
		return GuiShopAPI.canBeBought(item);
	}

	@Override
	public boolean canBeSold(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item) {
		return GuiShopAPI.canBeSold(item);
	}

	@Override
	public Optional<BigDecimal> getBuyPrice(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item, int amount) {
		BigDecimal value = GuiShopAPI.getBuyPrice(item, amount);
		return Optional.ofNullable(value.doubleValue() > 0 ? value : null);
	}

	@Override
	public Optional<BigDecimal> getSellPrice(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item, int amount) {
		BigDecimal value = GuiShopAPI.getSellPrice(item, amount);
		return Optional.ofNullable(value.doubleValue() > 0 ? value : null);
	}

	@Override
	public TransactionRecord<ItemStack> buy(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item, int amount) {
		TransactionRecordBuilder<ItemStack> builder = TransactionRecord.startPurchase(this, item, playerID, worldID).setVolume(amount);
		if (!canBeBought(playerID, worldID, item)) return builder.buildFailure(NO_PERMISSION);
		Optional<BigDecimal> value = getBuyPrice(playerID, worldID, item, amount);
		if (value.isEmpty()) return builder.buildFailure(NO_PERMISSION);
		return builder.setValue(value.get()).buildSuccess(() -> GuiShopAPI.indicateBoughtItems(item, amount));
	}

	@Override
	public TransactionRecord<ItemStack> sell(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item, int amount) {
		TransactionRecordBuilder<ItemStack> builder = TransactionRecord.startSale(this, item, playerID, worldID).setVolume(amount);
		if (!canBeSold(playerID, worldID, item)) return builder.buildFailure(NO_PERMISSION);
		Optional<BigDecimal> value = getSellPrice(playerID, worldID, item, amount);
		if (value.isEmpty()) return builder.buildFailure(NO_PERMISSION);
		return builder.setValue(value.get()).buildSuccess(() -> GuiShopAPI.indicateSoldItems(item, amount));
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
