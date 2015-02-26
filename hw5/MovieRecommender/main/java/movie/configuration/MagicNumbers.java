package movie.configuration;

import java.util.Random;

/**
 * MagicNumbers class contains some 
 * magic numbers
 * 
 * @author Ke Wang
 *
 */
public class MagicNumbers {
	
	public final static double PERCENTAGE_OF_NEIGHBORS = 0.3;
	public final static int LEAST_NUMBER_OF_NEIGHBORS = 10;
	public final static int NUMBER_OF_PARTITIONS = 10;
	
	public final static int getRandomPartition() {
		Random rand = new Random();
		return rand.nextInt(NUMBER_OF_PARTITIONS);
	}
}
