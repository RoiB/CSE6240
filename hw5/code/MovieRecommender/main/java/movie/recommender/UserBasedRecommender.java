package movie.recommender;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;

import movie.configuration.MagicNumbers;
import movie.util.Pair;

public class UserBasedRecommender {

	public void validate(String similarityMethod, String inputFilePath) {
		
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
			this.predict(similarityMethod, "training_set_"+p, "test_set_"+p, "test_result_"+p);
			
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
	
	public void predict(String similarityMethod, String inputFilePath, String toBeRatedFilePath,
			String outputFilePath) {

		// read data
		Map<Integer, Map<Integer, Integer>> ratings = new HashMap<Integer, Map<Integer,Integer>>();
		File ratingsFile = new File(inputFilePath);
		try (Scanner sc = new Scanner(ratingsFile)) {
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				if (line.length() < 4) { continue; }
				Scanner lineScanner = new Scanner(line);
				lineScanner.useDelimiter(",");
				int userId = lineScanner.nextInt();
				int movieId = lineScanner.nextInt();
				int rating = lineScanner.nextInt();
				Map<Integer, Integer> temp = ratings.get(userId-1);
				if (temp == null) {
					temp = new HashMap<Integer, Integer>();
					temp.put(movieId-1, rating);
					ratings.put(userId-1, temp);
				} else {
					temp.put(movieId-1, rating);
				}
				lineScanner.close();
			}
		} catch (FileNotFoundException e) {
			System.err.println(e);
			System.exit(1);
		}
		System.out.println("Ratings file reading complete");
		
		
		// count average rating
		Map<Integer, Double> averageRating = new HashMap<Integer, Double>();
		for (Entry<Integer, Map<Integer, Integer>> entry : ratings.entrySet()) {
			int sumOfRatings = 0;
			for (Entry<Integer, Integer> e : entry.getValue().entrySet()) {
				sumOfRatings += e.getValue();
			}
			averageRating.put(entry.getKey(), ((double)sumOfRatings)/entry.getValue().size());
		}
		System.out.println("Average rating counting complete");
		
		// compute user similarity
		Map<Integer, Map<Integer, Double>> similarity = new HashMap<>();
		for (Entry<Integer, Map<Integer, Integer>> user1 : ratings.entrySet()) {
			for (Entry<Integer, Map<Integer, Integer>> user2 : ratings.entrySet()) {
				if (user1 == user2) { continue; }
				if (similarity.get(user1)!=null && similarity.get(user1).get(user2)!=null) { continue; }
				switch (similarityMethod) {
				case "Jaccard": {
					int numberOfMoviesRatedByUser1 = user1.getValue().size();
					int numberOfMoviesRatedByUser2 = user2.getValue().size();
					int numberOfMoviesRatedByBothUsers = 0;
					
					Set<Integer> moviesRatedByUser1 = new HashSet<Integer>(user1.getValue().keySet());
					Set<Integer> moviesRatedByUser2 = new HashSet<Integer>(user2.getValue().keySet());
					moviesRatedByUser1.retainAll(moviesRatedByUser2);
					numberOfMoviesRatedByBothUsers = moviesRatedByUser1.size();
					if (numberOfMoviesRatedByBothUsers == 0) { continue; }
					
					double jaccardSimilarity = (double)numberOfMoviesRatedByBothUsers / 
							(numberOfMoviesRatedByUser1+numberOfMoviesRatedByUser2-numberOfMoviesRatedByBothUsers);
					
					Map<Integer, Double> temp = similarity.get(user1.getKey());
					if (temp == null) {
						temp = new HashMap<>();
						temp.put(user2.getKey(), jaccardSimilarity);
						similarity.put(user1.getKey(), temp);
					} else {
						temp.put(user2.getKey(), jaccardSimilarity);
					}
					
					temp = similarity.get(user2.getKey());
					if (temp == null) {
						temp = new HashMap<>();
						temp.put(user1.getKey(), jaccardSimilarity);
						similarity.put(user2.getKey(), temp);
					} else {
						temp.put(user1.getKey(), jaccardSimilarity);
					}
					
					break;
				}
				case "Pearson": {
					double factor1 = 0;
					double factor2 = 0;
					double factor3 = 0;
					
					Set<Integer> moviesRatedByBothUsers = new HashSet<Integer>(user1.getValue().keySet());
					Set<Integer> moviesRatedByUser2 = new HashSet<Integer>(user2.getValue().keySet());
					moviesRatedByBothUsers.retainAll(moviesRatedByUser2);
					if (moviesRatedByBothUsers.size() == 0) { continue; }
					
					for (Integer movieId : moviesRatedByBothUsers) {
						int rating1 = ratings.get(user1.getKey()).get(movieId);
						int rating2 = ratings.get(user2.getKey()).get(movieId);
						factor1 += (rating1-averageRating.get(user1.getKey())+MagicNumbers.EPSILON)*(rating2-averageRating.get(user2.getKey())+MagicNumbers.EPSILON);
						factor2 += Math.pow(rating1-averageRating.get(user1.getKey())+MagicNumbers.EPSILON, 2);
						factor3 += Math.pow(rating2-averageRating.get(user2.getKey())+MagicNumbers.EPSILON, 2);
					}
					factor2 = Math.sqrt(factor2);
					factor3 = Math.sqrt(factor3);
					
					double cosineSimilarity = factor1/(factor2*factor3);
					
					Map<Integer, Double> temp = similarity.get(user1.getKey());
					if (temp == null) {
						temp = new HashMap<>();
						temp.put(user2.getKey(), cosineSimilarity);
						similarity.put(user1.getKey(), temp);
					} else {
						temp.put(user2.getKey(), cosineSimilarity);
					}
					
					temp = similarity.get(user2.getKey());
					if (temp == null) {
						temp = new HashMap<>();
						temp.put(user1.getKey(), cosineSimilarity);
						similarity.put(user2.getKey(), temp);
					} else {
						temp.put(user1.getKey(), cosineSimilarity);
					}
					
					break;
				}
				case "cosine": {
					double factor1 = 0;
					double factor2 = 0;
					double factor3 = 0;
					
					Set<Integer> moviesRatedByBothUsers = new HashSet<Integer>(user1.getValue().keySet());
					Set<Integer> moviesRatedByUser2 = new HashSet<Integer>(user2.getValue().keySet());
					moviesRatedByBothUsers.retainAll(moviesRatedByUser2);
					if (moviesRatedByBothUsers.size() == 0) { continue; }
					
					for (Integer movieId : moviesRatedByBothUsers) {
						int rating1 = ratings.get(user1.getKey()).get(movieId);
						int rating2 = ratings.get(user2.getKey()).get(movieId);
						factor1 += rating1*rating2;
						factor2 += Math.pow(rating1, 2);
						factor3 += Math.pow(rating2, 2);
					}
					factor2 = Math.sqrt(factor2);
					factor3 = Math.sqrt(factor3);
					
					double cosineSimilarity = factor1/(factor2*factor3);
					
					Map<Integer, Double> temp = similarity.get(user1.getKey());
					if (temp == null) {
						temp = new HashMap<>();
						temp.put(user2.getKey(), cosineSimilarity);
						similarity.put(user1.getKey(), temp);
					} else {
						temp.put(user2.getKey(), cosineSimilarity);
					}
					
					temp = similarity.get(user2.getKey());
					if (temp == null) {
						temp = new HashMap<>();
						temp.put(user1.getKey(), cosineSimilarity);
						similarity.put(user2.getKey(), temp);
					} else {
						temp.put(user1.getKey(), cosineSimilarity);
					}
					
					break;
				}
				default:
					break;
				}
			}
		}
		System.out.println("Similarity computation complete");
		
		// read toBeRated file
		List<Double> prediction = new ArrayList<>();
		File toBeRatedFile = new File(toBeRatedFilePath);
		try (Scanner sc = new Scanner(toBeRatedFile)) {
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				int index = line.indexOf(",");
				if (index == -1) { continue; }
				int toRateUser = Integer.parseInt(line.substring(0, index))-1;
				int toRateMovie = Integer.parseInt(line.substring(index+1))-1;
				
				// find users that are similar to toRateUser
				// and they have already rate toRateMovie
				List<Pair<Integer, Double>> similarUsers = new ArrayList<>();
				if (similarity.get(toRateUser) == null) {
					prediction.add(averageRating.get(toRateUser));
					continue;
				}
				for (Entry<Integer, Double> user : similarity.get(toRateUser).entrySet()) {
					if (user.getValue() <= 0) { continue; }
					if (ratings.get(user.getKey()).get(toRateMovie) == null) { continue; }
					similarUsers.add(new Pair<Integer, Double>(user.getKey(), user.getValue()));
				}
				if (similarUsers.size() == 0) {
					prediction.add(averageRating.get(toRateUser));
					continue;
				}
				
				// sort similarUsers by similarity
				Collections.sort(similarUsers, (e1, e2)->{
					if (e1.getSecond() > e2.getSecond()) {
						return -1;
					} else if (e1.getSecond() < e2.getSecond()) {
						return 1;
					} else {
						return 0;
					}
				});
				
				// choose number of neighbors to predict
				int neighbors = (int)(similarUsers.size() * MagicNumbers.PERCENTAGE_OF_NEIGHBORS)+1;
				if (neighbors < MagicNumbers.LEAST_NUMBER_OF_NEIGHBORS) {
					neighbors = Math.min(MagicNumbers.LEAST_NUMBER_OF_NEIGHBORS, similarUsers.size());
				}
				
				// predict value
				double normalization = 0;
				double value = 0;
				for (int i = 0;i < neighbors;i++) {
					normalization += similarUsers.get(i).getSecond();
					value += similarUsers.get(i).getSecond()*
							(ratings.get(similarUsers.get(i).getFirst()).get(toRateMovie)-
							averageRating.get(similarUsers.get(i).getFirst()));
				}
				//System.out.println(neighbors+" "+averageRating[toRateUserId]+" "+value+" "+normalization+" "+value/normalization);
				Double averageForToRateUser = averageRating.get(toRateUser);
				if (averageForToRateUser == null) {
					prediction.add(MagicNumbers.DEFAULT_MOVIE_RATING + value/normalization);
				} else {
					prediction.add(averageForToRateUser.doubleValue() + value/normalization);
				}
			}
		} catch (FileNotFoundException e) {
			System.err.println(e);
			System.exit(1);
		}
		System.out.println("Prediction complete");
		
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
		System.out.println("Output complete");
	}
	
}
