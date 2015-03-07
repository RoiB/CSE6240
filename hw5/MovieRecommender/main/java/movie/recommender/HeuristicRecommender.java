package movie.recommender;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import movie.configuration.MagicNumbers;
import movie.util.Pair;

public class HeuristicRecommender {
	public void validate(String inputFilePath, String usersFilePath, String moviesFilePath) {
		
		
		// read data and assign each value a partition
		Map<Integer, Map<Integer, Pair<Integer, Integer>>> ratings = new HashMap<>();
		File input = new File(inputFilePath);
		try (Scanner sc = new Scanner(input)) {
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				if (line.length() < 4) { continue; }
				Scanner lineScanner = new Scanner(line);
				lineScanner.useDelimiter(",");
				int userId = lineScanner.nextInt();
				int movieId = lineScanner.nextInt();
				int rating = lineScanner.nextInt();
				lineScanner.close();
				int partitionNumber = MagicNumbers.getRandomPartition();
				
				Map<Integer, Pair<Integer, Integer>> temp = ratings.get(userId);
				if (temp == null) {
					temp = new HashMap<>();
					temp.put(movieId, new Pair<Integer, Integer>(rating, partitionNumber));
					ratings.put(userId, temp);
				} else {
					temp.put(movieId, new Pair<Integer, Integer>(rating, partitionNumber));
				}
			}			
		} catch (FileNotFoundException e) {
			System.err.println(e);
			System.exit(1);
		}
		
		// prepare files
		for (int p = 0;p < MagicNumbers.NUMBER_OF_PARTITIONS;p++) {
			try (
					BufferedWriter trainingSetWriter = new BufferedWriter(new FileWriter(new File("training_set_"+p)));
					BufferedWriter testSetWriter = new BufferedWriter(new FileWriter(new File("test_set_"+p)));
					BufferedWriter groundTruthWriter= new BufferedWriter(new FileWriter(new File("ground_truth_"+p)));
					) {
				for (Entry<Integer, Map<Integer, Pair<Integer, Integer>>> user : ratings.entrySet()) {
					for (Entry<Integer, Pair<Integer, Integer>> movie : user.getValue().entrySet()) {
						if (movie.getValue().getSecond() == p) {
							testSetWriter.write(""+user.getKey()+","+movie.getKey()+"\n");
							groundTruthWriter.write(movie.getValue().getFirst()+"\n");
						} else {
							trainingSetWriter.write(""+user.getKey()+","+movie.getKey()+","+movie.getValue().getFirst()+",0\n");
						}
					}
				}
			} catch (IOException e) {
				System.out.println(e);
				System.exit(1);
			}
		}
		
		// cross validation
		double totalError = 0;
		double currentError = 0;
		for (int p = 0;p < MagicNumbers.NUMBER_OF_PARTITIONS;p++) {
			this.predict("training_set_"+p, usersFilePath, moviesFilePath, "test_set_"+p, "test_result_"+p);
			
			int count = 0;
			try (
					Scanner resultScanner = new Scanner(new File("test_result_"+p));
					Scanner groundTruthScanner = new Scanner(new File("ground_truth_"+p));
					) {
				while (groundTruthScanner.hasNextDouble()) {
					count++;
					double truth = groundTruthScanner.nextDouble();
					double prediction = resultScanner.nextDouble();
					currentError += Math.pow(prediction-truth,2);
				}
			} catch (FileNotFoundException e) {
				System.out.println(e);
				System.exit(1);
			}
			currentError = Math.sqrt(currentError/count);
			System.out.println(""+p+": "+currentError);
			totalError += currentError;
		}
		System.out.println("RMSE: "+totalError/MagicNumbers.NUMBER_OF_PARTITIONS);
		
	}

	public void predict(String ratingsFilePath,
			String usersFilePath, String itemsFilePath,
			String toBeRatedFilePath, String outputFilePath) {
		
		// read matrix
		Map<Integer, Map<Integer, Integer>> ratings = new HashMap<>();
		File ratingsFile = new File(ratingsFilePath);
		try (Scanner sc = new Scanner(ratingsFile)) {
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				if (line.length() < 4) { continue; }
				Scanner lineScanner = new Scanner(line);
				lineScanner.useDelimiter(",");
				int userId = lineScanner.nextInt()-1;
				int movieId = lineScanner.nextInt()-1;
				int rating = lineScanner.nextInt();
				
				Map<Integer, Integer> temp = ratings.get(userId);
				if (temp == null) {
					temp = new HashMap<>();
					temp.put(movieId, rating);
					ratings.put(userId, temp); 
				} else {
					temp.put(movieId, rating);
				}
				
				lineScanner.close();
			}
		} catch (FileNotFoundException e) {
			System.err.println(e);
			System.exit(1);
		}
		System.out.println("Matrix is loaded.");
		
		
		// read movies, build movieIndexId->setOfGenres map
		Map<Integer, Set<Integer>> movieGenres = new TreeMap<>();
		File moviesFile = new File(itemsFilePath);
		try (
				BufferedReader br = new BufferedReader(new FileReader(moviesFile));
				) {
			String line = null;
			while ((line=br.readLine()) != null) {
				int firstCommaIndex = line.indexOf(",");
				int lastCommaIndex = line.lastIndexOf(",");
				int movieid = Integer.parseInt(line.substring(0, firstCommaIndex));
				String categoryString = line.substring(lastCommaIndex+1);
				if (categoryString.startsWith(" ") || categoryString.startsWith("000")) { continue; }
				Set<Integer> categories = new HashSet<Integer>();
				for (String s : categoryString.split(Pattern.quote("|"))) {
					categories.add(MovieGenre.genreToId(s));
				}
				movieGenres.put(movieid-1, categories);
			}
		} catch (IOException e) {
			System.err.println(e);
			System.exit(1);
		}
		
		System.out.println("Movies Information Loaded.");
		
		// read user
		// for each user, build UserProfile
		Map<Integer, UserProfile> userProfiles = new HashMap<>();
		File usersFile = new File(usersFilePath);
		try (Scanner sc = new Scanner(usersFile)) {
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				if (line.length() < 4) { continue; }
				Scanner lineScanner = new Scanner(line);
				lineScanner.useDelimiter(",");
				int userId = lineScanner.nextInt()-1;
				boolean isMale = false;
				String gender = lineScanner.next();
				if (gender.equals("M")) {
					isMale = true;
				}
				int age = lineScanner.nextInt();
				int occupation = lineScanner.nextInt();
				lineScanner.close();
				
				// get average
				double[] totalPerGenre = new double[MovieGenre.NUMBER_OF_GENRES];
				int[] countPerGenre = new int[MovieGenre.NUMBER_OF_GENRES];
				double[] averagePerGenre = new double[MovieGenre.NUMBER_OF_GENRES];
				double total = 0;
				for (Entry<Integer, Integer> movie : ratings.get(userId).entrySet()) {
					total += movie.getValue();
					if (movieGenres.get(movie.getKey()) == null) { continue; }
					for (int genreId : movieGenres.get(movie.getKey())) {
						countPerGenre[genreId]++;
						totalPerGenre[genreId] += ratings.get(userId).get(movie.getKey());
					}
				}
				double average = total/ratings.get(userId).size();
				for (int ii = 0;ii < MovieGenre.NUMBER_OF_GENRES;ii++) {
					if (countPerGenre[ii] == 0) {
						averagePerGenre[ii] = average;
					} else {
						averagePerGenre[ii] = totalPerGenre[ii] / countPerGenre[ii];
					}
				}
				
				// build user profile
				UserProfile currentUserProfile = new UserProfile(isMale, age, occupation, average, averagePerGenre, ratings.get(userId).size());
				userProfiles.put(userId, currentUserProfile);
				//System.out.println(userId+" "+isMale+" "+occupation+" "+average+" "+averagePerGenre[0]);
			}
		} catch (FileNotFoundException e) {
			System.err.println(e);
			System.exit(1);
		}
		System.out.println("Users Information Loaded.");
		
		
		// buildUserSimilarityMatrix
		// user-based similarity * amplification
		Map<Integer, Map<Integer, Double>> similarity = new HashMap<>();
		for (Entry<Integer, UserProfile> userProfile1 : userProfiles.entrySet()) {
			for (Entry<Integer, UserProfile> userProfile2 : userProfiles.entrySet()) {
				if (userProfile1 == userProfile2) { continue; }
				double amplification = userProfile1.getValue().similarityAmplify(userProfile2.getValue());
				
				Map<Integer, Double> temp = similarity.get(userProfile1.getKey());
				if (temp == null) {
					temp = new HashMap<>();
					temp.put(userProfile2.getKey(), amplification);
					similarity.put(userProfile1.getKey(), temp);
				} else {
					temp.put(userProfile2.getKey(), amplification);
				}
				
				temp = similarity.get(userProfile2.getKey());
				if (temp == null) {
					temp = new HashMap<>();
					temp.put(userProfile1.getKey(), amplification);
					similarity.put(userProfile2.getKey(), temp);
				} else {
					temp.put(userProfile1.getKey(), amplification);
				}
			}
		}
		
		
		// read toBeRatedFile
		// make prediction one by one
		List<Double> prediction = new ArrayList<Double>();
		try (Scanner sc = new Scanner(new File(toBeRatedFilePath))) {
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				int index = line.indexOf(",");
				if (index == -1) { continue; }
				int toRateUser = Integer.parseInt(line.substring(0, index))-1;
				int toRateMovie = Integer.parseInt(line.substring(index+1))-1;
				
				List<Pair<Integer, Double>> possibleNeighbors = new ArrayList<>();
				if (similarity.get(toRateUser) == null) {
					prediction.add(userProfiles.get(toRateUser).getOverallAverage());
					continue;
				}
				for (Entry<Integer, Double> user : similarity.get(toRateUser).entrySet()) {
					if (user.getValue() <= MagicNumbers.LEAST_AMPLIFICATION) { continue; }
					if (ratings.get(user.getKey()).get(toRateMovie) == null) { continue; }
					possibleNeighbors.add(new Pair<Integer, Double>(user.getKey(), user.getValue()));
				}
				if (possibleNeighbors.size() == 0) {
					prediction.add(userProfiles.get(toRateUser).getOverallAverage());
					continue;
				}
				
				Collections.sort(possibleNeighbors, (e1, e2)->{
					if (e1.getSecond() > e2.getSecond()) {
						return -1;
					} else if (e1.getSecond() < e2.getSecond()) {
						return 1;
					} else {
						return 0;
					}
				});

 				// choose number of neighbors to predict
				int neighbors = (int)(possibleNeighbors.size() * MagicNumbers.PERCENTAGE_OF_NEIGHBORS)+1;
				if (neighbors < MagicNumbers.LEAST_NUMBER_OF_NEIGHBORS) {
					neighbors = Math.min(MagicNumbers.LEAST_NUMBER_OF_NEIGHBORS, possibleNeighbors.size());
				}
				
				// predict value
				double normalization = 0;
				double value = 0;
				for (int i = 0;i < neighbors;i++) {
					normalization += possibleNeighbors.get(i).getSecond();
					value += possibleNeighbors.get(i).getSecond()*
							(ratings.get(possibleNeighbors.get(i).getFirst()).get(toRateMovie)-
							userProfiles.get(possibleNeighbors.get(i).getFirst()).getOverallAverage());
				}
				//System.out.println(neighbors+" "+averageRating[toRateUserId]+" "+value+" "+normalization+" "+value/normalization);
				Double averageForToRateUser = userProfiles.get(toRateUser).getOverallAverage();
				if (averageForToRateUser == null) {
					prediction.add(MagicNumbers.DEFAULT_MOVIE_RATING + value/normalization);
				} else {
					prediction.add(averageForToRateUser.doubleValue() + value/normalization);
				}
 

			}
		} catch (FileNotFoundException e) {
			System.out.println(e);
			System.exit(1);
		}
		
		// output prediction
		File output = new File(outputFilePath);
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(output))) {
			for (double d : prediction) {
				bw.write(d+"\n");
			}
		} catch (IOException e) {
			System.err.println(e);
			System.exit(1);
		}
	}
}
