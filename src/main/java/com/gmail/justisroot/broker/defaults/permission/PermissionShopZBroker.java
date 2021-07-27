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
package com.gmail.justisroot.broker.defaults.permission;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.permissions.Permission;

import com.gmail.justisroot.broker.TransactionRecord;

/**
 * Waiting on response from author before implementing
 */
public final class PermissionShopZBroker extends PermissionBroker {

	public PermissionShopZBroker() {
		super("nl.tabuu.permissionshopz.PermissionShopZ", "nl.tabuu.permissionshopz.data.Perk");
	}

	@Override
	public String getProvider() {
		return "PermissionShopZ";
	}

	@Override
	public boolean canBeBought(Optional<UUID> playerID, Optional<UUID> worldID, Permission permission) {
		return false;
	}

	@Override
	public boolean canBeSold(Optional<UUID> playerID, Optional<UUID> worldID, Permission permission) {
		return false;
	}

	@Override
	public Optional<BigDecimal> getBuyPrice(Optional<UUID> playerID, Optional<UUID> worldID, Permission permission, int amount) {
		return Optional.empty();
	}

	@Override
	public Optional<BigDecimal> getSellPrice(Optional<UUID> playerID, Optional<UUID> worldID, Permission permission, int amount) {
		return Optional.empty();
	}

	@Override
	public TransactionRecord<Permission> buy(Optional<UUID> playerID, Optional<UUID> worldID, Permission permission, int amount) {
		return TransactionRecord.startPurchase(this, permission, playerID, worldID).setVolume(amount).buildFailure(NO_PERMISSION);
	}

	@Override
	public TransactionRecord<Permission> sell(Optional<UUID> playerID, Optional<UUID> worldID, Permission permission, int amount) {
		return TransactionRecord.startPurchase(this, permission, playerID, worldID).setVolume(amount).buildFailure(NO_PERMISSION);
	}

	@Override
	public String getDisplayName(Optional<UUID> playerID, Optional<UUID> worldID, Permission permission) {
		return permission.getName();
	}

	@Override
	public boolean handlesPurchases(Optional<UUID> playerID, Optional<UUID> worldID, Permission permission) {
		return false;
	}

	@Override
	public boolean handlesSales(Optional<UUID> playerID, Optional<UUID> worldID, Permission permission) {
		return false;
	}

}
