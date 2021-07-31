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
package com.gmail.justisroot.broker.defaults;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import com.gmail.justisroot.broker.Broker;

public abstract class AbstractBroker<T> implements Broker<T> {

	private final String id;
	private Plugin plugin;
	private boolean available;

	protected static final String NO_PERMISSION = "Not permissible";

	protected AbstractBroker(String required, String... packages) {
		try {
			// Validate required packages exist
			Class.forName(required);
			for (String p : packages)
				Class.forName(p);

			available = true;
		} catch (Exception e) {
			available = false;
		}

		// Set the ID of this Broker to the class name of the farthest implemented descendant
		Class<?> clazz = null;
		StackTraceElement[] trace = Thread.currentThread().getStackTrace();
		for (int i = 1; i < trace.length; i++) {
			try {
				Class<?> checkClazz = Class.forName(trace[i].getClassName());
				if (checkClazz.isInstance(this)) clazz = checkClazz;
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		this.id = clazz.getName().substring(clazz.getName().lastIndexOf('.') + 1);
	}

	public Plugin plugin() {
		if (plugin == null) plugin = Bukkit.getPluginManager().getPlugin(getProvider());
		return plugin;
	}

	protected void setUnavailable() {
		available = false;
	}

	/**
	 * Always check if your required library packages are available <b>before</b> attempting to access them
	 * @return true if the provided packages are available and the providing plugin is enabled, false otherwise
	 */
	public boolean isAvailable() {
		return available && plugin() != null && plugin.isEnabled();
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public byte getPriority() {
		return -100;
	}

}
