package net.bigpoint.assessment.gasstation.realisation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import net.bigpoint.assessment.gasstation.GasPump;
import net.bigpoint.assessment.gasstation.GasStation;
import net.bigpoint.assessment.gasstation.GasType;
import net.bigpoint.assessment.gasstation.exceptions.GasTooExpensiveException;
import net.bigpoint.assessment.gasstation.exceptions.NotEnoughGasException;

public class MyGasStation extends Thread implements GasStation {

	// All GasPumps by Type
	private Map<GasType, Collection<GasPump>> pumpsMap = new HashMap<GasType, Collection<GasPump>>();

	private Map<GasType, Double> prices = Collections.synchronizedMap(new HashMap<GasType, Double>());
	private volatile Double revenue = 0.00;
	private AtomicInteger salesCounter = new AtomicInteger(0);
	private AtomicInteger noGasCounter = new AtomicInteger(0);
	private AtomicInteger tooExpensiveCounter = new AtomicInteger(0);

	public void addGasPump(GasPump pump) {
		synchronized (pumpsMap) {
			if (pumpsMap.get(pump.getGasType()) == null) {
				Collection<GasPump> list = Collections.synchronizedCollection(new ArrayList<GasPump>());
				list.add(pump);
				pumpsMap.put(pump.getGasType(), list);
			} else {
				Collection<GasPump> list = pumpsMap.get(pump.getGasType());
				list.add(pump);
				pumpsMap.put(pump.getGasType(), list);
			}
		}
	}

	public Collection<GasPump> getGasPumps() {
		List<GasPump> allPumps = new ArrayList<GasPump>();
		List<Collection<GasPump>> list = new ArrayList<Collection<GasPump>>(pumpsMap.values());

		for (Collection<GasPump> pumps : list) {
			allPumps.addAll(pumps);
		}
		return allPumps;
	}

	public double buyGas(GasType type, double amountInLiters, double maxPricePerLiter)
			throws NotEnoughGasException, GasTooExpensiveException {
		double price = getPrice(type);

		if (price >= maxPricePerLiter) {
			if (pumpsMap.containsKey(type)) {
				// select suitable pump, and remove it from available pumps collection
				GasPump selectedPump = selectPump(type, amountInLiters);

				// pump gas and write statistical data
				selectedPump.pumpGas(amountInLiters);
				double resultPrice = getRevenueAndUpdateStats(amountInLiters, price);

				// return pump to available pumps collection, notify awaiting threads
				Collection<GasPump> pumps = pumpsMap.get(type);
				synchronized (pumps) {
					pumps.add(selectedPump);
					pumpsMap.get(type).notifyAll();
				}

				return resultPrice;
			} else{
				System.err.println("Unknown gas pump type");
				return 0;
			}
		} else {
			tooExpensiveCounter.getAndIncrement();
			throw new GasTooExpensiveException();
		}
	}

	private GasPump selectPump(GasType type, double amountInLiters) throws NotEnoughGasException {
		Collection<GasPump> pumps = pumpsMap.get(type);
		GasPump selectedPump = null;
		synchronized (pumps) {
			// if there are no free pumps, wait
			while (pumps.isEmpty()) {
				try {
					pumps.wait();
				} catch (InterruptedException ex) {
					ex.printStackTrace();
				}
			}
			// chose pump with enough amount of gas
			for (GasPump pump : pumps) {
				if (pump.getRemainingAmount() >= amountInLiters) {
					selectedPump = pump;
					break;
				}
			}
			if (selectedPump != null) {
				pumps.remove(selectedPump);
			} else {
				noGasCounter.getAndIncrement();
				throw new NotEnoughGasException();
			}
		}
		return selectedPump;
	}

	public double getRevenue() {
		synchronized (revenue) {
			return revenue;
		}
	}

	private void increaseRevenue(Double rev) {
		synchronized (revenue) {
			revenue += rev;
		}
	}

	private double getRevenueAndUpdateStats(double amountInLiters, double price) {
		double rev = amountInLiters * price;
		increaseRevenue(rev);
		salesCounter.getAndIncrement();
		return rev;
	}

	public int getNumberOfSales() {
		return salesCounter.get();
	}

	public int getNumberOfCancellationsNoGas() {
		return noGasCounter.get();
	}

	public int getNumberOfCancellationsTooExpensive() {
		return tooExpensiveCounter.get();
	}

	public double getPrice(GasType type) {
		return prices.get(type);
	}

	public void setPrice(GasType type, double price) {
		synchronized (prices) {
			prices.put(type, price);
		}
	}
}