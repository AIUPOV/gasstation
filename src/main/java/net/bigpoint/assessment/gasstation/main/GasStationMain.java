package net.bigpoint.assessment.gasstation.main;

import java.text.DecimalFormat;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import net.bigpoint.assessment.gasstation.GasPump;
import net.bigpoint.assessment.gasstation.GasType;
import net.bigpoint.assessment.gasstation.exceptions.GasTooExpensiveException;
import net.bigpoint.assessment.gasstation.exceptions.NotEnoughGasException;
import net.bigpoint.assessment.gasstation.realisation.MyGasStation;
/**
 * Class, which imitates behavior of gas station clients
 *
 */
public class GasStationMain {

	static MyGasStation gasStation = new MyGasStation();
	static int COUNT_OF_THREADS = 10;

	static double DISEL_PRICE = 1.11;
	static double SUPER_PRICE = 1.25;
	static double REGULAR_PRICE = 1.31;

	static int DISEL_AMOUNT = 50;
	static int SUPER_AMOUNT = 50;
	static int REGULAR_AMOUNT = 50;

	static int RANGE_MIN_AMOUNT = 20;
	static int RANGE_MAX_AMOUNT = 30;

	static double RANGE_MIN_PRICE = 1.05;
	static double RANGE_MAX_PRICE = 1.15;

	static DecimalFormat df = new DecimalFormat("#.##");

	public static Runnable createThread(final String clientName) {
		return new Runnable() {
			public void run() {
				try {
					Random r = new Random();

					GasType type = GasType.getRandomType();
					int randomAmount = r.nextInt((RANGE_MAX_AMOUNT - RANGE_MIN_AMOUNT) + 1) + RANGE_MIN_AMOUNT;
					double randomPrice = RANGE_MIN_PRICE + (RANGE_MAX_PRICE - RANGE_MIN_PRICE) * r.nextDouble();

					// Try to Gas buy
					double paid = gasStation.buyGas(type, randomAmount, randomPrice);

					System.out.println("Client: " + clientName.toString());
					System.out.println("Paid: " + df.format(paid) + "ˆ");
					System.out.println("Gas buying: type=" + type + "; amount=" + randomAmount + "; desired price="
							+ df.format(randomPrice) + ";\n");

				} catch (NotEnoughGasException e) {
					System.out.println("Count of exeption then no gas in station: "
							+ gasStation.getNumberOfCancellationsNoGas() + ";  " + clientName + "\n");
				} catch (GasTooExpensiveException e) {
					System.out.println("Count of exeption then gas too expensive: "
							+ gasStation.getNumberOfCancellationsTooExpensive()+ ";  " + clientName + "\n");
				}
			}

		};
	}

	public static void main(String[] args) throws InterruptedException {
		// Initial Data
		// Price
		gasStation.setPrice(GasType.DIESEL, DISEL_PRICE);
		gasStation.setPrice(GasType.REGULAR, REGULAR_PRICE);
		gasStation.setPrice(GasType.SUPER, SUPER_PRICE);
		// Pumps
		gasStation.addGasPump(new GasPump(GasType.DIESEL, DISEL_AMOUNT));
		gasStation.addGasPump(new GasPump(GasType.DIESEL, DISEL_AMOUNT));
		gasStation.addGasPump(new GasPump(GasType.DIESEL, DISEL_AMOUNT));
		gasStation.addGasPump(new GasPump(GasType.REGULAR, REGULAR_AMOUNT));
		gasStation.addGasPump(new GasPump(GasType.REGULAR, REGULAR_AMOUNT));
		gasStation.addGasPump(new GasPump(GasType.SUPER, SUPER_AMOUNT));
		gasStation.addGasPump(new GasPump(GasType.SUPER, SUPER_AMOUNT));
		gasStation.addGasPump(new GasPump(GasType.SUPER, SUPER_AMOUNT));

		ExecutorService es = Executors.newCachedThreadPool();
		for (int i = 0; i < COUNT_OF_THREADS; i++) {
			String client = "car " + i;
			es.submit(createThread(client));
		}
		es.shutdown();
		es.awaitTermination(1, TimeUnit.HOURS);

		System.out.println("END:");
		System.out.println("Revenue: " + df.format(gasStation.getRevenue()) + "ˆ");
		System.out.println("No Gas count: " + gasStation.getNumberOfCancellationsNoGas());
		System.out.println("Too Expansive count: " + gasStation.getNumberOfCancellationsTooExpensive() + "\n");
	}

}
