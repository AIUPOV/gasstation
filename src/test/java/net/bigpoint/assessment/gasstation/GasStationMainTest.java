package net.bigpoint.assessment.gasstation;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import net.bigpoint.assessment.gasstation.exceptions.GasTooExpensiveException;
import net.bigpoint.assessment.gasstation.exceptions.NotEnoughGasException;
import net.bigpoint.assessment.gasstation.realisation.MyGasStation;

public class GasStationMainTest {
	
	static MyGasStation station;
	
	static double DISEL_PRICE = 1.11;
	static double REGULAR_PRICE = 1.25;
	static double SUPER_PRICE = 1.31;
	
	@Before
	public void configure() {
		// Initial Data
		station = new MyGasStation();
		// Prices
		station.setPrice(GasType.DIESEL, DISEL_PRICE);
		station.setPrice(GasType.REGULAR, REGULAR_PRICE);
		station.setPrice(GasType.SUPER, SUPER_PRICE);
		// Pumps		
		station.addGasPump(new GasPump(GasType.DIESEL, 100));
		station.addGasPump(new GasPump(GasType.SUPER, 150));
		station.addGasPump(new GasPump(GasType.REGULAR, 100));
		station.addGasPump(new GasPump(GasType.SUPER, 150));
	}
	
	public static Runnable createThread(final String clientName, final GasType type, final int amount, final double price) {
		return new Runnable() {
			public void run() {
				try {
					station.buyGas(type, amount, price);
				} catch (NotEnoughGasException e) {
				} catch (GasTooExpensiveException e) {
				}
			}
		};
	}
	
	@Test(expected=NotEnoughGasException.class)
	public void testByMoreThenPossibleGasType() throws NotEnoughGasException, GasTooExpensiveException{
		station.buyGas(GasType.DIESEL, 10000, DISEL_PRICE);
	}
	
	@Test
	public void testByPossibleCountGasType() throws NotEnoughGasException, GasTooExpensiveException{
		double price = station.buyGas(GasType.DIESEL, 100, DISEL_PRICE);
		assertEquals(111.0, price, 0.01);
	}
	
	@Test(expected=GasTooExpensiveException.class)
	public void testBuyTooExpensiveGas() throws NotEnoughGasException, GasTooExpensiveException{
		station.buyGas(GasType.DIESEL, 100, DISEL_PRICE - 0.50);
	}
		
	@Test(expected=RuntimeException.class)
	public void testGetUnknownGasPrice(){
		final MyGasStation station = new MyGasStation();
		
		station.setPrice(GasType.DIESEL, DISEL_PRICE);
		station.setPrice(GasType.REGULAR, REGULAR_PRICE);
		
		station.getPrice(GasType.SUPER);
	}
	
	@Test
	public void testGetGasPrice(){
		double price = station.getPrice(GasType.REGULAR);
		assertEquals(REGULAR_PRICE, price, 0.01);
	}
	
	@Test
	public void testModelingSituationAllGood() throws InterruptedException{	
		ExecutorService es = Executors.newCachedThreadPool();
		
		es.submit(createThread("Disel client", GasType.DIESEL, 100, DISEL_PRICE));
		es.submit(createThread("Super client1", GasType.SUPER, 100, SUPER_PRICE));
		es.submit(createThread("Regular client", GasType.REGULAR, 100, REGULAR_PRICE));
		es.submit(createThread("Super client2", GasType.SUPER, 50, SUPER_PRICE));
		es.submit(createThread("Super client3", GasType.SUPER, 50, SUPER_PRICE));
		
		es.shutdown();
		es.awaitTermination(1, TimeUnit.HOURS);
		
		double revenue = station.getRevenue();
		int noGasCounter = station.getNumberOfCancellationsNoGas();
		int gasTooExpensiveCounter = station.getNumberOfCancellationsTooExpensive();
		
		assertEquals(498.0, revenue, 0.01);
		assertEquals(0, noGasCounter);
		assertEquals(0, gasTooExpensiveCounter);
	}
	
	@Test
	public void testModelingNotGoodSituation() throws InterruptedException{	
		ExecutorService es = Executors.newCachedThreadPool();
		
		es.submit(createThread("Disel client", GasType.DIESEL, 100, DISEL_PRICE));
		es.submit(createThread("Super client1", GasType.SUPER, 100, SUPER_PRICE));
		es.submit(createThread("Regular client", GasType.REGULAR, 150, REGULAR_PRICE));
		es.submit(createThread("Super client2", GasType.SUPER, 50, SUPER_PRICE));
		es.submit(createThread("Super client3", GasType.SUPER, 50, 1.30));		
		
		es.shutdown();
		es.awaitTermination(1, TimeUnit.HOURS);
		
		int noGasCounter = station.getNumberOfCancellationsNoGas();
		int gasTooExpensiveCounter = station.getNumberOfCancellationsTooExpensive();

		double revenue = station.getRevenue();
		assertEquals(307.5, revenue, 0.01);
		assertEquals(1, noGasCounter);
		assertEquals(1, gasTooExpensiveCounter);
	}
	
}
