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
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;

import com.gmail.justisroot.broker.TransactionRecord;
import com.gmail.justisroot.broker.TransactionRecord.TransactionRecordBuilder;

import fr.maxlego08.shop.api.InventoryManager;
import fr.maxlego08.shop.api.button.buttons.ItemButton;

/**
 * Respects configured sounds on transaction. Does not currently run configured commands.
 */
public class ZShopBroker extends ItemBroker {

	private InventoryManager iv;

	public ZShopBroker() {
		super("fr.maxlego08.shop.api.InventoryManager", "fr.maxlego08.shop.api.button.buttons.ItemButton");
		if (!isAvailable()) return;
		RegisteredServiceProvider<InventoryManager> provider = Bukkit.getServicesManager().getRegistration(InventoryManager.class);
		if (provider != null) iv = provider.getProvider();
		else setUnavailable();
	}

	@Override
	public String getProvider() {
		return "zShop";
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
		Optional<ItemButton> button = iv.getItemButton(item);
		if (button.isEmpty()) return Optional.empty();
		if (!button.get().canBuy()) return Optional.empty();
		double value;
		if (playerID.isPresent() && Bukkit.getPlayer(playerID.get()) != null)
			value = button.get().getBuyPrice(Bukkit.getPlayer(playerID.get()));
		else value = button.get().getBuyPrice();
		if (value <= 0) return Optional.empty();
		return Optional.of(new BigDecimal(value * amount));
	}

	@Override
	public Optional<BigDecimal> getSellPrice(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item, int amount) {
		Optional<ItemButton> button = iv.getItemButton(item);
		if (button.isEmpty()) return Optional.empty();
		if (!button.get().canSell()) return Optional.empty();
		double value;
		if (playerID.isPresent() && Bukkit.getPlayer(playerID.get()) != null)
			value = button.get().getSellPrice(Bukkit.getPlayer(playerID.get()));
		else value = button.get().getSellPrice();
		if (value <= 0) return Optional.empty();
		return Optional.of(new BigDecimal(value * amount));
	}

	@Override
	public TransactionRecord<ItemStack> buy(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item, int amount) {
		TransactionRecordBuilder<ItemStack> builder = TransactionRecord.startPurchase(this, item, playerID, worldID).setVolume(amount);
		Optional<BigDecimal> value = getBuyPrice(playerID, worldID, item, amount);
		if (value.isEmpty()) return builder.buildFailure(NO_PERMISSION);
		return builder.setValue(value.get()).buildSuccess(() -> {
			Optional<ItemButton> button = iv.getItemButton(item);
			if (playerID.isEmpty() || Bukkit.getPlayer(playerID.get()) == null) return;
			button.get().playSound(Bukkit.getPlayer(playerID.get()));
		});
	}

	@Override
	public TransactionRecord<ItemStack> sell(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item, int amount) {
		TransactionRecordBuilder<ItemStack> builder = TransactionRecord.startSale(this, item, playerID, worldID).setVolume(amount);
		Optional<BigDecimal> value = getSellPrice(playerID, worldID, item, amount);
		if (value.isEmpty()) return builder.buildFailure(NO_PERMISSION);
		return builder.setValue(value.get()).buildSuccess(() -> {
			Optional<ItemButton> button = iv.getItemButton(item);
			if (playerID.isEmpty() || Bukkit.getPlayer(playerID.get()) == null) return;
			button.get().playSound(Bukkit.getPlayer(playerID.get()));
		});
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
