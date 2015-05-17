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
import java.util.Scanner;
import java.util.Set;
import java.util.Map.Entry;

import movie.configuration.MagicNumbers;
import movie.util.Pair;

public class ItemBasedRecommender {

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
		Map<Integer, Map<Integer, Integer>> ratings = new HashMap<Integer, Map<Integer,Integer>>(); // movie->(user, rating)
		File ratingsFile = new File(inputFilePath);
		try (Scanner sc = new Scanner(ratingsFile)) {
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				if (line.length() < 4) { continue; }
				Scanner lineScanner = new Scanner(line);
				lineScanner.useDelimiter(",");
				int userId = lineScanner.nextInt()-1;
				int movieId = lineScanner.nextInt()-1;
				int rating = lineScanner.nextInt();
				Map<Integer, Integer> temp = ratings.get(movieId);
				if (temp == null) {
					temp = new HashMap<Integer, Integer>();
					temp.put(userId, rating);
					ratings.put(movieId, temp);
				} else {
					temp.put(userId, rating);
				}
				lineScanner.close();
			}
		} catch (FileNotFoundException e) {
			System.err.println(e);
			System.exit(1);
		}
		System.out.println("Ratings file reading complete");
		
		
		// count average rating for each movie
		Map<Integer, Double> averageRating = new HashMap<Integer, Double>();
		for (Entry<Integer, Map<Integer, Integer>> entry : ratings.entrySet()) {
			int sumOfRatings = 0;
			for (Entry<Integer, Integer> e : entry.getValue().entrySet()) {
				sumOfRatings += e.getValue();
			}
			averageRating.put(entry.getKey(), ((double)sumOfRatings)/entry.getValue().size());
		}
		System.out.println("Average rating counting complete");
		
		// compute movie similarity
		Map<Integer, Map<Integer, Double>> similarity = new HashMap<>();
		for (Entry<Integer, Map<Integer, Integer>> movie1 : ratings.entrySet()) {
			for (Entry<Integer, Map<Integer, Integer>> movie2 : ratings.entrySet()) {
				if (movie1 == movie2) { continue; }
				if (similarity.get(movie1.getKey())!=null && similarity.get(movie1.getKey()).get(movie2.getKey())!=null) { continue; }
				switch (similarityMethod) {
				case "Jaccard": {
					int numberOfUsersRatingMovie1 = movie1.getValue().size();
					int numberOfUsersRatingMovie2 = movie2.getValue().size();
					int numberOfUsersRatingBothMovies = 0;
					
					Set<Integer> usersRatingMovie1 = new HashSet<Integer>(movie1.getValue().keySet());
					Set<Integer> usersRatingMovie2 = new HashSet<Integer>(movie2.getValue().keySet());
					usersRatingMovie1.retainAll(usersRatingMovie2);
					numberOfUsersRatingBothMovies = usersRatingMovie1.size();
					if (numberOfUsersRatingBothMovies == 0) { continue; }
					
					double jaccardSimilarity = (double)numberOfUsersRatingBothMovies / 
							(numberOfUsersRatingMovie1+numberOfUsersRatingMovie2-numberOfUsersRatingBothMovies);
					
					Map<Integer, Double> temp = similarity.get(movie1.getKey());
					if (temp == null) {
						temp = new HashMap<>();
						temp.put(movie2.getKey(), jaccardSimilarity);
						similarity.put(movie1.getKey(), temp);
					} else {
						temp.put(movie2.getKey(), jaccardSimilarity);
					}
					
					temp = similarity.get(movie2.getKey());
					if (temp == null) {
						temp = new HashMap<>();
						temp.put(movie1.getKey(), jaccardSimilarity);
						similarity.put(movie2.getKey(), temp);
					} else {
						temp.put(movie1.getKey(), jaccardSimilarity);
					}
					
					break;
				}
				case "Pearson": {
					double factor1 = 0;
					double factor2 = 0;
					double factor3 = 0;
					
					Set<Integer> usersRatingBothMovies = new HashSet<Integer>(movie1.getValue().keySet());
					Set<Integer> usersRatingMovie2 = new HashSet<Integer>(movie2.getValue().keySet());
					usersRatingBothMovies.retainAll(usersRatingMovie2);
					if (usersRatingBothMovies.size() == 0) { continue; }
					
					for (Integer userId : usersRatingBothMovies) {
						int rating1 = ratings.get(movie1.getKey()).get(userId);
						int rating2 = ratings.get(movie2.getKey()).get(userId);
						factor1 += (rating1-averageRating.get(movie1.getKey())+MagicNumbers.EPSILON)*(rating2-averageRating.get(movie2.getKey())+MagicNumbers.EPSILON);
						factor2 += Math.pow((rating1-averageRating.get(movie1.getKey())+MagicNumbers.EPSILON), 2);
						factor3 += Math.pow((rating1-averageRating.get(movie1.getKey())+MagicNumbers.EPSILON), 2);
					}
					factor2 = Math.sqrt(factor2);
					factor3 = Math.sqrt(factor3);
					
					double cosineSimilarity = factor1/(factor2*factor3);
					
					Map<Integer, Double> temp = similarity.get(movie1.getKey());
					if (temp == null) {
						temp = new HashMap<>();
						temp.put(movie2.getKey(), cosineSimilarity);
						similarity.put(movie1.getKey(), temp);
					} else {
						temp.put(movie2.getKey(), cosineSimilarity);
					}
					
					temp = similarity.get(movie2.getKey());
					if (temp == null) {
						temp = new HashMap<>();
						temp.put(movie1.getKey(), cosineSimilarity);
						similarity.put(movie2.getKey(), temp);
					} else {
						temp.put(movie1.getKey(), cosineSimilarity);
					}
					
					break;
				}
				case "cosine": {
					double factor1 = 0;
					double factor2 = 0;
					double factor3 = 0;
					
					Set<Integer> usersRatingBothMovies = new HashSet<Integer>(movie1.getValue().keySet());
					Set<Integer> usersRatingMovie2 = new HashSet<Integer>(movie2.getValue().keySet());
					usersRatingBothMovies.retainAll(usersRatingMovie2);
					if (usersRatingBothMovies.size() == 0) { continue; }
					
					for (Integer userId : usersRatingBothMovies) {
						int rating1 = ratings.get(movie1.getKey()).get(userId);
						int rating2 = ratings.get(movie2.getKey()).get(userId);
						factor1 += rating1*rating2;
						factor2 += Math.pow(rating1, 2);
						factor3 += Math.pow(rating2, 2);
					}
					factor2 = Math.sqrt(factor2);
					factor3 = Math.sqrt(factor3);
					
					double cosineSimilarity = factor1/(factor2*factor3);
					
					Map<Integer, Double> temp = similarity.get(movie1.getKey());
					if (temp == null) {
						temp = new HashMap<>();
						temp.put(movie2.getKey(), cosineSimilarity);
						similarity.put(movie1.getKey(), temp);
					} else {
						temp.put(movie2.getKey(), cosineSimilarity);
					}
					
					temp = similarity.get(movie2.getKey());
					if (temp == null) {
						temp = new HashMap<>();
						temp.put(movie1.getKey(), cosineSimilarity);
						similarity.put(movie2.getKey(), temp);
					} else {
						temp.put(movie1.getKey(), cosineSimilarity);
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
				
				// find movies that are similar to toRateMovie
				// and rated by toRateUser
				List<Pair<Integer, Double>> similarMovies = new ArrayList<>();
				if (similarity.get(toRateMovie) == null) {
					if (averageRating.get(toRateMovie) == null) {
						prediction.add(MagicNumbers.DEFAULT_MOVIE_RATING);
						//System.out.println(toRateMovie);
					} else {
						prediction.add(averageRating.get(toRateMovie));
					}
					continue;
				}
				for (Entry<Integer, Double> movie : similarity.get(toRateMovie).entrySet()) {
					if (movie.getValue() <= 0) { continue; }
					if (ratings.get(movie.getKey()).get(toRateUser) == null) { continue; }
					similarMovies.add(new Pair<Integer, Double>(movie.getKey(), movie.getValue()));
				}
				if (similarMovies.size() == 0) {
					if (averageRating.get(toRateMovie) == null) {
						prediction.add(MagicNumbers.DEFAULT_MOVIE_RATING);
						//System.out.println(toRateMovie);
					} else {
						prediction.add(averageRating.get(toRateMovie));
					}
					continue;
				}
				
				// sort similarUsers by similarity
				Collections.sort(similarMovies, (e1, e2)->{
					if (e1.getSecond() > e2.getSecond()) {
						return -1;
					} else if (e1.getSecond() < e2.getSecond()) {
						return 1;
					} else {
						return 0;
					}
				});
				
				// choose number of neighbors to predict
				int neighbors = (int)(similarMovies.size() * MagicNumbers.PERCENTAGE_OF_NEIGHBORS)+1;
				if (neighbors < MagicNumbers.LEAST_NUMBER_OF_NEIGHBORS) {
					neighbors = Math.min(MagicNumbers.LEAST_NUMBER_OF_NEIGHBORS, similarMovies.size());
				}
				
				// predict value
				double normalization = 0;
				double value = 0;
				for (int i = 0;i < neighbors;i++) {
					normalization += similarMovies.get(i).getSecond();
					value += similarMovies.get(i).getSecond()*
							(ratings.get(similarMovies.get(i).getFirst()).get(toRateUser)-
							averageRating.get(similarMovies.get(i).getFirst()));
				}
				//System.out.println(neighbors+" "+averageRating[toRateUserId]+" "+value+" "+normalization+" "+value/normalization);
				Double averageForToRateMovie = averageRating.get(toRateMovie);
				if (averageForToRateMovie == null) {
					prediction.add(MagicNumbers.DEFAULT_MOVIE_RATING + value/normalization);
				} else {
					prediction.add(averageForToRateMovie.doubleValue() + value/normalization);
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
			for (Double d : prediction) {
				bw.write(d+"\n");
			}
		} catch (IOException e) {
			System.err.println(e);
			System.exit(1);
		}
		System.out.println("Output complete");
		
	}
	
}
