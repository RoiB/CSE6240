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
	
	public final static int NUMBER_OF_USERS = 6040;
	public final static int NUMBER_OF_MOVIES = 3952;
	
	public final static double PERCENTAGE_OF_NEIGHBORS = 0.3;
	public final static int LEAST_NUMBER_OF_NEIGHBORS = 10;
	public final static int NUMBER_OF_PARTITIONS = 10;
	
	public final static float DEFAULT_MOVIE_RATING = 4;
	
	public final static int getRandomPartition() {
		Random rand = new Random();
		return rand.nextInt(NUMBER_OF_PARTITIONS);
	}
}
