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

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.gmail.justisroot.broker.BrokerInfo;

public abstract class BrokerEvent extends Event {

	private final BrokerInfo info;

	BrokerEvent(BrokerInfo info) {
		this.info = info;
	}

	/**
	 * Get the name of the provider of this Broker's implementation.
	 *
	 * @return the name of this Broker's provider
	 */
	public String provider() {
		return info.provider();
	}

	/**
	 * Get the unique identification string for the Broker implementation involved in this event
	 * @return the string id for the involved Broker
	 */
	public String getBrokerId() {
		return info.id();
	}

	/**
	 * Get the class of the object type that the Broker involved in this event handles
	 * @return the type associated with the involved Broker
	 */
	public Class<?> getBrokerType() {
		return info.type();
	}

	private static final HandlerList HANDLERS = new HandlerList();

	@Override
	public HandlerList getHandlers() {
		return HANDLERS;
	}

	public static HandlerList getHandlerList() {
		return HANDLERS;
	}

}
