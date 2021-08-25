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
package com.gmail.justisroot.broker.events;

import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;

public final class EventCreator {

	private EventCreator() { }

	public static final void registerEvents() {
		PluginManager pm = Bukkit.getPluginManager();
		BrokerEventService service = BrokerEventService.current();
		service.setRegistrationHandler(info -> pm.callEvent(new BrokerRegistrationEvent(info)));
		service.setUnregistrationHandler(info -> pm.callEvent(new BrokerUnregistrationEvent(info)));
		service.setPurchaseHandler((info, record) -> pm.callEvent(new PurchaseEvent(info, record)));
		service.setSaleHandler((info, record) -> pm.callEvent(new SaleEvent(info, record)));
		service.setSalePreProcessHandler((info, record) -> {
			SalePreProcessEvent event = new SalePreProcessEvent(info, record);
			pm.callEvent(event);
			return event.isCancelled();
		});
		service.setPurchasePreProcessHandler((info, record) -> {
			PurchasePreProcessEvent event = new PurchasePreProcessEvent(info, record);
			pm.callEvent(event);
			return event.isCancelled();
		});
	}

}
