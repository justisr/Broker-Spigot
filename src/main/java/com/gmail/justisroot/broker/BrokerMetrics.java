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
package com.gmail.justisroot.broker;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bstats.charts.CustomChart;
import org.bstats.charts.DrilldownPie;
import org.bstats.charts.SingleLineChart;

abstract class BrokerMetrics {

	private BrokerMetrics() { }

	private static final Set<CustomChart> CHARTS = new HashSet<>();

	static {
		CHARTS.add(new DrilldownPie("broker_implementations", () -> {
			Map<String, Map<String, Integer>> data = new HashMap<>();
			for (Entry<Class<?>, SimilarBrokers<?>> entry : BrokerAPI.current().brokerMap().entrySet()) {
				Map<String, Integer> meta = new HashMap<>();
				Iterator<?> iterator = entry.getValue().iterator();
				while (iterator.hasNext()) meta.put(((PrioritizedBroker<?, ?>) iterator.next()).get().getProvider(), 1);
				data.put(entry.getKey().getSimpleName(), meta);
			}
			return data;
		}));
		CHARTS.add(new SingleLineChart("broker_count", () -> {
			int count = 0;
			for (Entry<Class<?>, SimilarBrokers<?>> entry : BrokerAPI.current().brokerMap().entrySet()) {
				Iterator<?> iterator = entry.getValue().iterator();
				while (iterator.hasNext()) {
					iterator.next();
					count++;
				}
			}
			return count;
		}));
	}

	static final Set<CustomChart> getCharts() {
		return CHARTS;
	}

}
