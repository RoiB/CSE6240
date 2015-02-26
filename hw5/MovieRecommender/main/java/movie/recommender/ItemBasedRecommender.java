package movie.recommender;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import movie.configuration.MagicNumbers;
import movie.util.Pair;

public class ItemBasedRecommender {

	public void validate(String similarityMethod, String inputFilePath) {
		
		// read data and assign each value a partition
		short[][] ratings = new short[MagicNumbers.NUMBER_OF_USERS][MagicNumbers.NUMBER_OF_MOVIES];
		short[][] partition = new short[MagicNumbers.NUMBER_OF_USERS][MagicNumbers.NUMBER_OF_MOVIES];
		File input = new File(inputFilePath);
		try (Scanner sc = new Scanner(input)) {
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				if (line.length() < 4) { continue; }
				Scanner lineScanner = new Scanner(line);
				lineScanner.useDelimiter(",");
				int userId = lineScanner.nextInt();
				int movieId = lineScanner.nextInt();
				short rating = lineScanner.nextShort();
				lineScanner.close();
				short partitionNumber = (short)MagicNumbers.getRandomPartition();
				ratings[userId-1][movieId-1] = rating;
				partition[userId-1][movieId-1] = partitionNumber;
			}			
		} catch (FileNotFoundException e) {
			System.err.println(e);
			System.exit(1);
		}
		System.out.println("Matrix construction complete");
		
		// prepare files
		for (int p = 0;p < MagicNumbers.NUMBER_OF_PARTITIONS;p++) {
			try (
					BufferedWriter trainingSetWriter = new BufferedWriter(new FileWriter(new File("training_set_"+p)));
					BufferedWriter testSetWriter = new BufferedWriter(new FileWriter(new File("test_set_"+p)));
					BufferedWriter groundTruthWriter= new BufferedWriter(new FileWriter(new File("ground_truth_"+p)));
					) {
				for (int i = 0;i < MagicNumbers.NUMBER_OF_USERS;i++) {
					for (int j = 0;j < MagicNumbers.NUMBER_OF_MOVIES;j++) {
						if (ratings[i][j] == 0) { continue; }
						if (partition[i][j] == p) {
							// test set
							testSetWriter.write(""+(i+1)+","+(j+1)+"\n");
							groundTruthWriter.write(ratings[i][j]+"\n");
						} else {
							// traning set
							trainingSetWriter.write(""+(i+1)+","+(j+1)+","+ratings[i][j]+",0\n");
						}
					}
				}
			} catch (IOException e) {
				System.out.println(e);
				System.exit(1);
			}
		}
		System.out.println("Files Preparation Complete");
		
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

		// build matrix and read data
		short[][] ratings = new short[MagicNumbers.NUMBER_OF_USERS][MagicNumbers.NUMBER_OF_MOVIES];
		File ratingsFile = new File(inputFilePath);
		try (Scanner sc = new Scanner(ratingsFile)) {
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				if (line.length() < 4) { continue; }
				Scanner lineScanner = new Scanner(line);
				lineScanner.useDelimiter(",");
				int userId = lineScanner.nextInt();
				int movieId = lineScanner.nextInt();
				short rating = lineScanner.nextShort();
				ratings[userId-1][movieId-1] = rating;
				lineScanner.close();
			}
		} catch (FileNotFoundException e) {
			System.err.println(e);
			System.exit(1);
		}
		
		// count average rating
		float[] averageRatingPerMovie = new float[MagicNumbers.NUMBER_OF_MOVIES];
		short[] numberOfRatesPerMovie = new short[MagicNumbers.NUMBER_OF_MOVIES];
		for (int i = 0;i < MagicNumbers.NUMBER_OF_MOVIES;i++) {
			int numberOfUsersVotingCurrentMovie = 0;
			int sumOfRatings = 0;
			for (int j = 0;j < MagicNumbers.NUMBER_OF_USERS;j++) {
				if (ratings[j][i] != 0) {
					numberOfUsersVotingCurrentMovie++;
					sumOfRatings += ratings[j][i];
				}
			}
			numberOfRatesPerMovie[i] = (short)numberOfUsersVotingCurrentMovie;
			if (numberOfUsersVotingCurrentMovie == 0) {
				averageRatingPerMovie[i] = MagicNumbers.DEFAULT_MOVIE_RATING;
			} else {
				averageRatingPerMovie[i] = (float)sumOfRatings / numberOfUsersVotingCurrentMovie;
			}
		}
		float[] averageRatingPerUser = new float[MagicNumbers.NUMBER_OF_USERS];
		for (int i = 0;i < MagicNumbers.NUMBER_OF_USERS;i++) {
			int count = 0;
			int sumOfRatings = 0;
			for (int j = 0;j < MagicNumbers.NUMBER_OF_MOVIES;j++) {
				if (ratings[i][j] != 0) {
					count++;
					sumOfRatings += ratings[i][j];
				}
			}
			if (count == 0) {
				averageRatingPerUser[i] = MagicNumbers.DEFAULT_MOVIE_RATING;
			} else {
				averageRatingPerUser[i] = (float)sumOfRatings / count;
			}
		}
		
		// build item similarity matrix
		float[][] itemSimilarityMatrix = new float[MagicNumbers.NUMBER_OF_MOVIES][MagicNumbers.NUMBER_OF_MOVIES];
		switch (similarityMethod) {
		case "Jaccard":
			for (int movie1 = 0;movie1 < MagicNumbers.NUMBER_OF_MOVIES;movie1++) {
				for (int movie2 = 0;movie2 < MagicNumbers.NUMBER_OF_MOVIES;movie2++) {
					if (movie1 >= movie2) { continue; }
					int numberOfUsersRatingBothMovies = 0;
					for (int user = 0;user < MagicNumbers.NUMBER_OF_USERS;user++) {
						if (ratings[user][movie1] != 0 && ratings[user][movie2] != 0) {
							numberOfUsersRatingBothMovies++;
						}
					}
					if (numberOfUsersRatingBothMovies == 0) { continue; }
					int total = numberOfRatesPerMovie[movie1] + numberOfRatesPerMovie[movie2] - numberOfUsersRatingBothMovies;
					itemSimilarityMatrix[movie1][movie2] = (float)numberOfUsersRatingBothMovies / total;
					itemSimilarityMatrix[movie2][movie1] = itemSimilarityMatrix[movie1][movie2];
				}
			}
			break;
		case "Pearson":
			for (int movie1 = 0;movie1 < MagicNumbers.NUMBER_OF_MOVIES;movie1++) {
				for (int movie2 = 0;movie2 < MagicNumbers.NUMBER_OF_MOVIES;movie2++) {
					if (movie1 >= movie2) { continue; }
					int numberOfUsersRatingBothMovies = 0;
					double factor1 = 0;
					double factor2 = 0;
					double factor3 = 0;
					for (int user = 0;user < MagicNumbers.NUMBER_OF_USERS;user++) {
						if (ratings[user][movie1] != 0 && ratings[user][movie2] != 0) {
							numberOfUsersRatingBothMovies++;
							factor1 += (ratings[user][movie1]-averageRatingPerMovie[movie1])*(ratings[user][movie2]-averageRatingPerMovie[movie2]);
							factor2 += Math.pow(ratings[user][movie1]-averageRatingPerMovie[movie1],2);
							factor3 += Math.pow(ratings[user][movie2]-averageRatingPerMovie[movie2],2);
						}
					}
					if (numberOfUsersRatingBothMovies == 0) { continue; }
					factor2 = Math.sqrt(factor2);
					factor3 = Math.sqrt(factor3);
					if (factor1 == 0) {
						itemSimilarityMatrix[movie1][movie2] = 0.5f;
					} else {
						itemSimilarityMatrix[movie1][movie2] = (float)(factor1/(factor2*factor3));
						itemSimilarityMatrix[movie2][movie1] = itemSimilarityMatrix[movie1][movie2];
					}
				}
			}
			break;
		case "cosine":
			for (int movie1 = 0;movie1 < MagicNumbers.NUMBER_OF_MOVIES;movie1++) {
				for (int movie2 = 0;movie2 < MagicNumbers.NUMBER_OF_MOVIES;movie2++) {
					if (movie1 >= movie2) { continue; }
					int numberOfUsersRatingBothMovies = 0;
					double factor1 = 0;
					double factor2 = 0;
					double factor3 = 0;
					for (int user = 0;user < MagicNumbers.NUMBER_OF_USERS;user++) {
						if (ratings[user][movie1] != 0 && ratings[user][movie2] != 0) {
							numberOfUsersRatingBothMovies++;
							factor1 += (ratings[user][movie1])*(ratings[user][movie2]);
							factor2 += Math.pow(ratings[user][movie1],2);
							factor3 += Math.pow(ratings[user][movie2],2);
						}
					}
					if (numberOfUsersRatingBothMovies == 0) { continue; }
					factor2 = Math.sqrt(factor2);
					factor3 = Math.sqrt(factor3);
					if (factor1 == 0) {
						itemSimilarityMatrix[movie1][movie2] = 0.5f;
					} else {
						itemSimilarityMatrix[movie1][movie2] = (float)(factor1/(factor2*factor3));
						itemSimilarityMatrix[movie2][movie1] = itemSimilarityMatrix[movie1][movie2];
					}
				}
			}
			break;
		default:
			System.err.println("ERROR");
		}
		System.out.println("Item Similarity Matrix complete");
		
		// read toBeRated file
		// make prediction one by one
		List<Double> prediction = new ArrayList<>();
		File toBeRatedFile = new File(toBeRatedFilePath);
		try (Scanner sc = new Scanner(toBeRatedFile);) {
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				//System.out.println(line);
				int index = line.indexOf(",");
				if (index == -1) { continue; }
				int toRateUserId = Integer.parseInt(line.substring(0, index))-1;
				int toRateMovieId = Integer.parseInt(line.substring(index+1))-1;
				
				// calculate similarity between items
				List<Pair<Integer, Double>> itemSimilarity = new ArrayList<>();
				for (int movie = 0;movie < MagicNumbers.NUMBER_OF_MOVIES;movie++) {
					if (ratings[toRateUserId][movie] != 0) {
						itemSimilarity.add(new Pair<Integer, Double>(movie, (double)itemSimilarityMatrix[movie][toRateMovieId]));
					}
				}
				
				// sort 'userSimilarity' by similarity
				Collections.sort(itemSimilarity, (e1,e2)->{
					/*if (Math.abs(e1.getSecond()-e2.getSecond()) < 0.00001) {
						return 0;
					} else */if (e1.getSecond() > e2.getSecond()) {
						return -1;	
					} else if (e1.getSecond() < e2.getSecond()) {
						return 1;
					} else {
						return 0;
					}
				});
				
				
				// choose neighbors to predict
				int neighbors = itemSimilarity.size();
				if (neighbors == 0) { 
					prediction.add((double)averageRatingPerMovie[toRateMovieId]);
					continue;
				}
				neighbors = (int)(neighbors * MagicNumbers.PERCENTAGE_OF_NEIGHBORS)+1;
				if (neighbors > MagicNumbers.LEAST_NUMBER_OF_NEIGHBORS) {
					neighbors = MagicNumbers.LEAST_NUMBER_OF_NEIGHBORS;
				}
				double normalization = 0;
				double value = 0;
				for (int i = 0;i < neighbors;i++) {
					normalization += itemSimilarity.get(i).getSecond();
					value += itemSimilarity.get(i).getSecond()*(ratings[toRateUserId][itemSimilarity.get(i).getFirst()]-averageRatingPerMovie[itemSimilarity.get(i).getFirst()]);
				}
				//temp.write(""+neighbors+" "+averageRatingPerMovie[toRateMovieId]+" "+value+" "+normalization+" "+(value/normalization)+"\n");
				if (normalization == 0) {
					prediction.add((double)averageRatingPerMovie[toRateMovieId]);
				} else {
					prediction.add(averageRatingPerMovie[toRateMovieId]+value/normalization);
				}
			}
		} catch (Exception e) {
			System.err.println(e);
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
