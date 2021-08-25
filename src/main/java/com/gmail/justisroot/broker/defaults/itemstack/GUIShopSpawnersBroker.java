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
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang.WordUtils;
import org.bukkit.Material;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

import com.gmail.justisroot.broker.record.PurchaseRecord;
import com.gmail.justisroot.broker.record.PurchaseRecord.PurchaseRecordBuilder;
import com.gmail.justisroot.broker.record.SaleRecord;
import com.gmail.justisroot.broker.record.SaleRecord.SaleRecordBuilder;

import me.Darrionat.GUIShopSpawners.GuiShopSpawners;
import me.Darrionat.GUIShopSpawners.Maps;

/**
 * Only handles the transaction of spawners.<br>
 * Requires a reload to fetch new prices.
 */
public final class GUIShopSpawnersBroker extends ItemBroker {

	private GuiShopSpawners plugin;
	private Map<String, BigDecimal> buyPrices = new HashMap<>();
	private Map<String, BigDecimal> sellPrices = new HashMap<>();


	public GUIShopSpawnersBroker() {
		super("me.Darrionat.GUIShopSpawners.GuiShopSpawners", "me.Darrionat.GUIShopSpawners.Maps");
		if (!isAvailable()) return;
		plugin = (GuiShopSpawners) plugin();
		Collection<String> paths = new Maps().getMobMap().values();
		BigDecimal disabled = new BigDecimal(-1);
		for (String path : paths) {
			ConfigurationSection section = plugin.getConfig().getConfigurationSection(path);
			String key = path.toUpperCase(Locale.US);
			if (section == null) continue;
			if (section.getBoolean("Enabled")) {
				buyPrices.put(key, new BigDecimal(section.getDouble("Buy")));
				sellPrices.put(key, new BigDecimal(section.getDouble("Sell")));
			} else {
				buyPrices.put(key, disabled);
				sellPrices.put(key, disabled);
			}
		}
	}

	@Override
	public String getProvider() {
		return "GUIShopSpawners";
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
		if (item.getType() != Material.SPAWNER) return Optional.empty();
		Optional<String> type = spawnerType(item);
		if (type.isEmpty()) return Optional.empty();
		BigDecimal value = buyPrices.get(type.get());
		if (value == null || value.doubleValue() <= 0) return Optional.empty();
		return Optional.of(value);
	}

	@Override
	public Optional<BigDecimal> getSellPrice(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item, int amount) {
		if (item.getType() != Material.SPAWNER) return Optional.empty();
		Optional<String> type = spawnerType(item);
		if (type.isEmpty()) return Optional.empty();
		BigDecimal value = sellPrices.get(type.get());
		if (value == null || value.doubleValue() <= 0) return Optional.empty();
		return Optional.of(value);
	}

	@Override
	public PurchaseRecord<ItemStack> buy(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item, int amount) {
		PurchaseRecordBuilder<ItemStack> builder = PurchaseRecord.start(this, item, playerID, worldID).setVolume(amount);
		Optional<BigDecimal> value = getBuyPrice(playerID, worldID, item, amount);
		if (value.isEmpty()) return builder.buildFailure(NO_PERMISSION);
		return builder.setValue(value.get()).buildSuccess();
	}

	@Override
	public SaleRecord<ItemStack> sell(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item, int amount) {
		SaleRecordBuilder<ItemStack> builder = SaleRecord.start(this, item, playerID, worldID).setVolume(amount);
		Optional<BigDecimal> value = getSellPrice(playerID, worldID, item, amount);
		if (value.isEmpty()) return builder.buildFailure(NO_PERMISSION);
		return builder.setValue(value.get()).buildSuccess();
	}

	@Override
	public String getDisplayName(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item) {
		Optional<String> type = spawnerType(item);
		if (type.isEmpty()) return item.getType().toString();
		return WordUtils.capitalize(type.get().replace("_", " ") + "Spawner");
	}

	private static Optional<String> spawnerType(ItemStack stack) {
		ItemMeta itemMeta = stack.getItemMeta();
		if (!(itemMeta instanceof BlockStateMeta)) return Optional.empty();
		BlockStateMeta blockMeta = (BlockStateMeta) itemMeta;
		if (!(blockMeta.getBlockState() instanceof CreatureSpawner)) return Optional.empty();
		CreatureSpawner spawner = (CreatureSpawner) blockMeta.getBlockState();
		return Optional.of(spawner.getSpawnedType().toString());
	}

	@Override
	public boolean handlesPurchases(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item) {
		if (item.getType() != Material.SPAWNER) return false;
		Optional<String> type = spawnerType(item);
		if (type.isEmpty()) return false;
		return buyPrices.containsKey(type.get());
	}

	@Override
	public boolean handlesSales(Optional<UUID> playerID, Optional<UUID> worldID, ItemStack item) {
		if (item.getType() != Material.SPAWNER) return false;
		Optional<String> type = spawnerType(item);
		if (type.isEmpty()) return false;
		return sellPrices.containsKey(type.get());
	}

}
