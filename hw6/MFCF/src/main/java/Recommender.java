import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.regex.Pattern;

/**
 * Recommender
 * 
 * @author Ke Wang
 *
 */
public class Recommender {

	public static double EPSILON = 0.000001;
	public static int NUMBER_OF_ITERATIONS = 10000;
	public static double DEFAULT_RATING = 3.5;
	
	private String ratingsFilePath;
	private String toBeRatedFilePath;
	private String resultFilePath;
	private int numberOfAttributes;
	private double learningRate;
	private double regularizationFactor;
	
	private Map<Integer, double[]> userProfiles;
	private Map<Integer, double[]> movieFeatures;
	private Map<Integer, Double> averageMovieRating;
	
	public static double GET_SMALL_RANDOM_VALUE() {
		Random random = new Random();
		return random.nextDouble();
	}
	
	
	/**
	 * 
	 * @param args ratings.csv toBeRated.csv 5 0.001 0.1
	 */
	public static void main(String[] args) {
		new Recommender().run(args);
	}
	
	public void run(String[] arguments) {
		this.parseArguments(arguments);
		this.matrixFactorization();
		this.makeRecommendations();
	}
	
	private void matrixFactorization() {
		
		Map<Integer, Map<Integer, Integer>> userRating = new HashMap<>();
		Map<Integer, Map<Integer, Integer>> movieRating = new HashMap<>();
		
		// read ratings file
		File ratingsFile = new File(this.ratingsFilePath);
		try (BufferedReader br = new BufferedReader(new FileReader(ratingsFile))) {
			String line = null;
			while ((line = br.readLine()) != null) {
				String[] item = line.split(Pattern.quote(","));
				int userId = Integer.parseInt(item[0]);
				int movieId = Integer.parseInt(item[1]);
				int rating = Integer.parseInt(item[2]);
				
				// add to userRating
				Map<Integer, Integer> ratingByAUser = userRating.get(userId);
				if (ratingByAUser == null) {
					ratingByAUser = new HashMap<>();
					ratingByAUser.put(movieId, rating);
					userRating.put(userId, ratingByAUser);
				} else {
					ratingByAUser.put(movieId, rating);
				}
				
				// add to movieRating
				Map<Integer, Integer> ratingByAMovie = movieRating.get(movieId);
				if (ratingByAMovie == null) {
					ratingByAMovie = new HashMap<>();
					ratingByAMovie.put(userId, rating);
					movieRating.put(movieId, ratingByAMovie);
				} else {
					ratingByAMovie.put(userId, rating);
				}
			}
		} catch (IOException e) {
			this.showErrorAndExit(e, 1);
		}
		
		// calculate average movie rating
		averageMovieRating = new HashMap<>();
		for (Entry<Integer, Map<Integer, Integer>> entry : movieRating.entrySet()) {
			int count = entry.getValue().size();
			int totalScore = entry.getValue().values().stream().mapToInt(e->e.intValue()).sum();
			averageMovieRating.put(entry.getKey(), (double)(totalScore) / count);
		}

		// create user profiles and movie attributes
		// initialize by random small values
		userProfiles = new HashMap<>();
		movieFeatures = new HashMap<>();
		for (Entry<Integer, Map<Integer, Integer>> userEntry : userRating.entrySet()) {
			double[] profile = new double[numberOfAttributes];
			for (int i = 0;i != numberOfAttributes;i++) {
				profile[i] = GET_SMALL_RANDOM_VALUE();
			}
			userProfiles.put(userEntry.getKey(), profile);
		}
		for (Entry<Integer, Map<Integer, Integer>> movieEntry : movieRating.entrySet()) {
			double[] feature = new double[numberOfAttributes];
			for (int i = 0;i != numberOfAttributes;i++) {
				feature[i] = GET_SMALL_RANDOM_VALUE();
			}
			movieFeatures.put(movieEntry.getKey(), feature);
		}
		
		// stochastic gradient descent
		for (int iter = 0;iter != NUMBER_OF_ITERATIONS;iter++) {
			for (Entry<Integer, Map<Integer, Integer>> userEntry : userRating.entrySet()) {
				for (Entry<Integer, Integer> anItem : userEntry.getValue().entrySet()) {
					int userId = userEntry.getKey();
					int movieId = anItem.getKey();
					int rating = anItem.getValue();

					double prediction = 0;
					for (int i = 0;i != numberOfAttributes;i++) {
						prediction += userProfiles.get(userId)[i]*movieFeatures.get(movieId)[i];
					}
					double error = ((double)(rating)) - prediction;
					
					for (int i = 0;i != numberOfAttributes;i++) {
						userProfiles.get(userId)[i] = (1-learningRate*regularizationFactor)*userProfiles.get(userId)[i]
								+ learningRate*error*movieFeatures.get(movieId)[i];
						movieFeatures.get(movieId)[i] = (1-learningRate*regularizationFactor)*movieFeatures.get(movieId)[i]
								+ learningRate*error*userProfiles.get(userId)[i];
					}
					
				}
			}
		}
	}
	
	private void makeRecommendations() {
		List<Double> prediction = new ArrayList<>();
		try(BufferedReader br = new BufferedReader(new FileReader(new File(toBeRatedFilePath)))) {
			String line = null;
			while ((line=br.readLine()) != null) {
				String[] item = line.split(Pattern.quote(","));
				if (item.length != 2) { continue; }
				int toRateUserId;
				int toRateMovieId;
				try {
					toRateUserId = Integer.parseInt(item[0]);
					toRateMovieId = Integer.parseInt(item[1]);
				} catch (NumberFormatException e) {
					continue;
				}
				
				double[] userProfile = userProfiles.get(toRateUserId);
				double[] movieFeature = movieFeatures.get(toRateMovieId);
				if (userProfile != null && movieFeature != null) {
					double currentPrediction = 0;
					for (int i = 0;i != numberOfAttributes;i++) {
						currentPrediction += userProfile[i]*movieFeature[i];
					}
					prediction.add(currentPrediction);
				} else if (userProfile == null) {
					prediction.add(averageMovieRating.get(toRateMovieId));
				} else if (movieFeature == null) {
					prediction.add(DEFAULT_RATING);
				} else {
				}
				
			}
		} catch (IOException e) {
			this.showErrorAndExit(e, 1);
		}
		
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File("results.csv")))) {
			for (Double d : prediction) {
				bw.write(""+d+"\n");
			}
		} catch (IOException e) {
			this.showErrorAndExit(e, 1);
		}
		
	}
	
	private void parseArguments(String[] arguments) {
		if (arguments.length != 5) {
			this.showHelpAndExit(1);
		}
		
		ratingsFilePath = arguments[0];
		toBeRatedFilePath = arguments[1];
		numberOfAttributes = Integer.parseInt(arguments[2]);
		learningRate = Double.parseDouble(arguments[3]);
		regularizationFactor = Double.parseDouble(arguments[4]);
		
	}
	
	private void showHelpAndExit(int exitCode) {
		System.out.println("Arguments:");
		System.out.println("1. ratings file");
		System.out.println("2. to be rated file");
		//System.out.println("3. result file");
		System.out.println("3. number of attributes");
		System.out.println("4. learning rate");
		System.out.println("5. regulariztion factor");
		System.exit(exitCode);
	}
	
	private void showErrorAndExit(Exception e, int exitCode) {
		e.printStackTrace();
		System.exit(exitCode);
	}
	
}
