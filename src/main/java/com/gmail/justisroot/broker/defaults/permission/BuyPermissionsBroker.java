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
package com.gmail.justisroot.broker.defaults.permission;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.permissions.Permission;

import com.gmail.justisroot.broker.record.PurchaseRecord;
import com.gmail.justisroot.broker.record.PurchaseRecord.PurchaseRecordBuilder;
import com.gmail.justisroot.broker.record.SaleRecord;

import me.glaremasters.buypermissions.BuyPermissions;

public final class BuyPermissionsBroker extends PermissionBroker {

	private Map<String, BigDecimal> permissions = new HashMap<>();
	private Map<String, String> names = new HashMap<>();

	public BuyPermissionsBroker() {
		super("me.glaremasters.buypermissions.BuyPermissions");
		if (!isAvailable()) return;
		BuyPermissions plugin = (BuyPermissions) plugin();
		ConfigurationSection config = plugin.getConfig().getConfigurationSection("permissions.commands");
		for (String name : plugin.getConfig().getStringList("currently-selling")) {
			name = name.toLowerCase(Locale.US);
			String node = config.getString(name + ".perm");
			String cost = config.getString(name + ".cost");
			if (node == null || cost == null) continue;
			names.put(node, name);
			permissions.put(node, new BigDecimal(cost));
		}
	}

	@Override
	public String getProvider() {
		return "BuyPermissions";
	}

	@Override
	public boolean canBeBought(Optional<UUID> playerID, Optional<UUID> worldID, Permission permission) {
		return permissions.containsKey(permission.getName()) && permissions.get(permission.getName()).doubleValue() > 0;
	}

	@Override
	public boolean canBeSold(Optional<UUID> playerID, Optional<UUID> worldID, Permission permission) {
		return false;
	}

	@Override
	public Optional<BigDecimal> getBuyPrice(Optional<UUID> playerID, Optional<UUID> worldID, Permission permission, int amount) {
		return Optional.ofNullable(permissions.get(permission.getName()));
	}

	@Override
	public Optional<BigDecimal> getSellPrice(Optional<UUID> playerID, Optional<UUID> worldID, Permission permission, int amount) {
		return Optional.empty();
	}

	@Override
	public PurchaseRecord<Permission> buy(Optional<UUID> playerID, Optional<UUID> worldID, Permission permission, int amount) {
		PurchaseRecordBuilder<Permission> record = PurchaseRecord.start(this, permission, playerID, worldID).setVolume(amount);
		if (!canBeSold(playerID, worldID, permission)) return record.buildFailure(NO_PERMISSION);
		Optional<BigDecimal> buyPrice = getBuyPrice(playerID, worldID, permission, amount);
		if (buyPrice.isEmpty()) return record.buildFailure(NO_PERMISSION);
		return record.setValue(buyPrice.get()).buildSuccess(null);
	}

	@Override
	public SaleRecord<Permission> sell(Optional<UUID> playerID, Optional<UUID> worldID, Permission permission, int amount) {
		return SaleRecord.start(this, permission, playerID, worldID).setVolume(amount).buildFailure(NO_PERMISSION);
	}

	@Override
	public String getDisplayName(Optional<UUID> playerID, Optional<UUID> worldID, Permission permission) {
		String name = names.get(permission.getName());
		return name == null ? permission.getName() : name;
	}

	@Override
	public boolean handlesPurchases(Optional<UUID> playerID, Optional<UUID> worldID, Permission permission) {
		return permissions.containsKey(permission.getName());
	}

	@Override
	public boolean handlesSales(Optional<UUID> playerID, Optional<UUID> worldID, Permission permission) {
		return false;
	}

}
