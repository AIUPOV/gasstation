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
	
	// All busy's GasPumps by Type
	private Map<GasType, Collection<GasPump>> busyPumpsMap = new HashMap<GasType, Collection<GasPump>>();

	private Map<GasType, Double> prices = Collections.synchronizedMap(new HashMap<GasType, Double>());

	private volatile Double revenue = 0.00;
	private AtomicInteger salesCounter = new AtomicInteger(0);
	private AtomicInteger noGasCounter = new AtomicInteger(0);
	private AtomicInteger tooExpensiveCounter = new AtomicInteger(0);

	public void addGasPump(GasPump pump) {
		synchronized (pumpsMap) {
			if (pumpsMap.get(pump.getGasType()) == null) {
				Collection<GasPump> list = new ArrayList<GasPump>();
				list.add(pump);
				pumpsMap.put(pump.getGasType(), list);
				busyPumpsMap.put(pump.getGasType(), new ArrayList<GasPump>());
			} else {
				Collection<GasPump> list = pumpsMap.get(pump.getGasType());
				list.add(pump);
				pumpsMap.put(pump.getGasType(), list);
			}
		}
	}

	public synchronized Collection<GasPump> getGasPumps() {
		List<GasPump> allPumps = new ArrayList<GasPump>();
		allPumps.addAll(getPumps(pumpsMap));
		allPumps.addAll(getPumps(busyPumpsMap));
		return allPumps;
	}
	
	private Collection<GasPump> getPumps(Map<GasType, Collection<GasPump>> pumpsMap){
		List<GasPump> allPumps = new ArrayList<GasPump>();
		List<Collection<GasPump>> pumpsLists = new ArrayList<Collection<GasPump>>(pumpsMap.values());
		for (Collection<GasPump> pumps : pumpsLists) {
			allPumps.addAll(pumps);
		}
		return allPumps;
	}

	public double buyGas(GasType type, double amountInLiters, double maxPricePerLiter)
			throws NotEnoughGasException, GasTooExpensiveException {
		double price = getPrice(type);	
		if (price <= maxPricePerLiter) {
			if (pumpsMap.containsKey(type)) {
				// select suitable pump, and remove it from available pumps collection
				Collection<GasPump> pumps = pumpsMap.get(type);
				Collection<GasPump> busyPumps = busyPumpsMap.get(type);
				GasPump selectedPump = selectPump(pumps, busyPumps, type, amountInLiters);

				// pump gas and write statistical data
				selectedPump.pumpGas(amountInLiters);
				
				double resultPrice;
				synchronized(this){
					resultPrice = getRevenueAndUpdateStats(amountInLiters, price);
					movePump(selectedPump, busyPumps, pumps);
					notifyAll();
				}
				return resultPrice;
			} else {
				throw new NotEnoughGasException();
			}
		} else {
			tooExpensiveCounter.getAndIncrement();
			throw new GasTooExpensiveException();
		}
	}
	
	private synchronized void movePump(GasPump pump, Collection<GasPump> from, Collection<GasPump> to){
		from.remove(pump);
		to.add(pump);
	}

	private synchronized GasPump selectPump(Collection<GasPump> pumps, Collection<GasPump> busyPumps, GasType type, double amountInLiters) throws NotEnoughGasException {		
		GasPump selectedPump = findPumpWithEnoughGas(pumps, amountInLiters);
		if(selectedPump != null){
			movePump(selectedPump, pumps, busyPumps);
			return selectedPump;
		}
		
		selectedPump = findPumpWithEnoughGas(busyPumps, amountInLiters);
		
		if(selectedPump == null){
			noGasCounter.getAndIncrement();
			throw new NotEnoughGasException();
		}

		while(!pumps.contains(selectedPump)){
			try {
				wait();
			}catch(InterruptedException ex){
				ex.printStackTrace();
			}					
		}
		movePump(selectedPump, pumps, busyPumps);
		return selectedPump;
	}

	private synchronized GasPump findPumpWithEnoughGas(Collection<GasPump> pumps, double amount) {
		for(GasPump pump : pumps){
			double pumpAmount = pump.getRemainingAmount();
			if(pumpAmount >= amount){
				return pump;
			}
		}
		return null;
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
		Double price = prices.get(type);
		if (price == null) {
			throw new RuntimeException("Price for gas type " + type + " is unknown");
		}
		return prices.get(type);
	}

	public void setPrice(GasType type, double price) {
		synchronized (prices) {
			prices.put(type, price);
		}
	}
}