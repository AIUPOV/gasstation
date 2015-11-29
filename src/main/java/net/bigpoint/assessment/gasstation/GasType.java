package net.bigpoint.assessment.gasstation;

import java.util.Random;

public enum GasType {

	REGULAR, SUPER, DIESEL;

	public static GasType getRandomType() {
		Random random = new Random();
		return values()[random.nextInt(values().length)];
	}

}
