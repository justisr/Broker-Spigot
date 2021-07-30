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
package com.gmail.justisroot.broker;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bstats.bukkit.Metrics;
import org.bstats.charts.CustomChart;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.gmail.justisroot.broker.commands.BrokerCommands;
import com.gmail.justisroot.broker.defaults.AbstractBroker;
import com.gmail.justisroot.broker.defaults.itemstack.ClipAutoSellBroker;
import com.gmail.justisroot.broker.defaults.itemstack.ConjurateShopBroker;
import com.gmail.justisroot.broker.defaults.itemstack.EssentialsXBroker;
import com.gmail.justisroot.broker.defaults.itemstack.GUIShopBroker;
import com.gmail.justisroot.broker.defaults.itemstack.GUIShopSpawnersBroker;
import com.gmail.justisroot.broker.defaults.itemstack.OneStopShopBroker;
import com.gmail.justisroot.broker.defaults.itemstack.SSDynamicShopBroker;
import com.gmail.justisroot.broker.defaults.itemstack.ShopGUIPlusBroker;
import com.gmail.justisroot.broker.defaults.permission.BuyPermissionsBroker;
import com.gmail.justisroot.broker.events.BrokerRegistrationEvent;
import com.gmail.justisroot.broker.events.BrokerUnregistrationEvent;
import com.gmail.justisroot.broker.events.TransactionEvent;
import com.gmail.justisroot.broker.events.TransactionPreProcessEvent;
import com.google.common.collect.Sets;

public final class SpigotInitializer extends JavaPlugin implements Listener {

	private final BrokerAPI api = new BrokerAPI(new Config(this.getDataFolder()));

	private final Map<Plugin, AbstractBroker<?>> defaults = new HashMap<>();

	@Override
	public void onEnable() {
		registerEvents();
		registerCommands();
		registerCharts(new Metrics(this, 10492));
		this.getServer().getPluginManager().registerEvents(this, this);
		Bukkit.getScheduler().runTaskLater(this, () -> registerDefaultBrokers(), 1);
	}

	@EventHandler
	public void onPluginDisable(PluginDisableEvent e) {
		if (defaults.containsKey(e.getPlugin())) unregisterDefault(defaults.get(e.getPlugin()));
	}

	/**
	 * Reloads all of the registered Brokers and their configuration settings.
	 */
	public void reload() {
		for (AbstractBroker<?> broker : defaults.values())
			api.unregister(broker);
		defaults.clear();
		api.reload();
		registerDefaultBrokers();
	}

	/**
	 * Get the available brokers, with their IDs mapped to their provider
	 * @return a map of available brokers providers and their brokers
	 */
	public Map<String, Set<String>> available() {
		Map<String, Set<String>> available = new HashMap<>();
		for (PrioritizedBroker<?, ?> i : api.brokers()) {
			if (available.containsKey(i.get().getProvider())) {
				available.get(i.get().getProvider()).add(i.get().getId());
			} else available.put(i.get().getProvider(), Sets.newHashSet(i.get().getId()));
		}
		return available;
	}

	private final void registerEvents() {
		PluginManager pm = Bukkit.getPluginManager();
		api.eventService().setRegistrationHandler(info -> pm.callEvent(new BrokerRegistrationEvent(info)));
		api.eventService().setUnregistrationHandler(info -> pm.callEvent(new BrokerUnregistrationEvent(info)));
		api.eventService().setTransactionHandler((info, record) -> pm.callEvent(new TransactionEvent(info, record)));
		api.eventService().setPreProcessTransactionHandler((info, record) -> {
			TransactionPreProcessEvent event = new TransactionPreProcessEvent(info, record);
			pm.callEvent(event);
			return event.isCancelled();
		});
	}

	private final void registerCommands() {
		new BrokerCommands(this);
	}

	private final void registerCharts(Metrics metrics) {
		for (CustomChart chart : BrokerMetrics.getCharts())
			metrics.addCustomChart(chart);
	}

	private final void unregisterDefault(AbstractBroker<?> broker) {
		api.unregister(broker);
		defaults.remove(broker.plugin());
	}

	/*
	 * Plugins where prices are assigned to arbitrary shop/package names or gui slots, rather than to a specific item/permission/etc are difficult to support If you have a plugin you'd like supported, consider contacting
	 * the author, asking that they register their own Broker implementation within their plugin
	 */
	private final void registerDefaultBrokers() {
		// Register default org.bukkit.inventory.ItemStack Brokers
		register(new EssentialsXBroker()); // https://essentialsx.net/downloads.html
		register(new ShopGUIPlusBroker()); // https://www.spigotmc.org/resources/6515/
		register(new GUIShopBroker()); // https://www.mc-market.org/resources/581/
		register(new ConjurateShopBroker()); // https://www.spigotmc.org/resources/8185/
		register(new SSDynamicShopBroker()); // https://www.spigotmc.org/resources/65603/
		register(new GUIShopSpawnersBroker()); // https://www.spigotmc.org/resources/69279/
		register(new ClipAutoSellBroker()); // https://wiki.helpch.at/clips-plugins/autosell
		register(new OneStopShopBroker()); // https://www.spigotmc.org/resources/76640/

		// Register default org.bukkit.permissions.Permission Brokers
		register(new BuyPermissionsBroker()); // https://www.spigotmc.org/resources/52557/

		// Register other default Brokers

	}

	/**
	 * It will always be preferred to maintain Broker implementations within the projects they belongs to.<br>
	 * If you are a plugin author and would like to be supported, just implement the Broker interface within your own project and use {@link BrokerAPI#register(Broker)} to register it on enable and
	 * {@link BrokerAPI#unregister(Broker)} on disable.
	 *
	 * @param broker the Broker implementation to register, if available
	 */
	private final void register(AbstractBroker<?> broker) {
		if (broker.isAvailable() && broker.plugin() != null && api.register(broker)) defaults.put(broker.plugin(), broker);
	}

}
