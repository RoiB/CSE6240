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

public class UserBasedRecommender {

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
		
		// cros validation
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
		float[] averageRating = new float[MagicNumbers.NUMBER_OF_USERS];
		short[] numberOfMoviesRatedByEachUser = new short[MagicNumbers.NUMBER_OF_USERS];
		for (int i = 0;i < MagicNumbers.NUMBER_OF_USERS;i++) {
			int moviesRatedByCurrentUser = 0;
			int sumOfRatings = 0;
			for (int j = 0;j < MagicNumbers.NUMBER_OF_MOVIES;j++) {
				if (ratings[i][j] != 0) {
					moviesRatedByCurrentUser++;
					sumOfRatings += ratings[i][j];
				}
			}
			numberOfMoviesRatedByEachUser[i] = (short)moviesRatedByCurrentUser;
			if (moviesRatedByCurrentUser == 0) {
				averageRating[i] = MagicNumbers.DEFAULT_MOVIE_RATING;
			} else {
				averageRating[i] = (float)sumOfRatings / moviesRatedByCurrentUser;
			}
		}
		
		// read toBeRated file
		// make prediction one by one
		List<Double> prediction = new ArrayList<>();
		File toBeRatedFile = new File(toBeRatedFilePath);
		try (Scanner sc = new Scanner(toBeRatedFile)) {
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				int index = line.indexOf(",");
				if (index == -1) { continue; }
				int toRateUserId = Integer.parseInt(line.substring(0, index))-1;
				int toRateMovieId = Integer.parseInt(line.substring(index+1))-1;
				
				// calculate similarity between users
				List<Pair<Integer, Double>> userSimilarity = new ArrayList<>();
				switch (similarityMethod) {
				case "Jaccard":
					for (int i = 0;i < MagicNumbers.NUMBER_OF_USERS;i++) {
						if (i == toRateUserId) { continue; }
						if (ratings[i][toRateMovieId] == 0) { continue; }
						int numberOfMoviesRatedByBothUsers = 0;
						for (int j = 0;j < MagicNumbers.NUMBER_OF_MOVIES;j++) {
							if (ratings[i][j] != 0 && ratings[toRateUserId][j] != 0) {
								numberOfMoviesRatedByBothUsers++;
							}
						}
						if (numberOfMoviesRatedByBothUsers == 0) { continue; }
						int totalMoviesRatedByBothUsers = numberOfMoviesRatedByEachUser[i] + 
								numberOfMoviesRatedByEachUser[toRateUserId] - 
								numberOfMoviesRatedByBothUsers;
						userSimilarity.add(new Pair<Integer, Double>(i, (double)numberOfMoviesRatedByBothUsers / totalMoviesRatedByBothUsers));
					}
					break;
				case "Pearson":
					for (int i = 0;i < MagicNumbers.NUMBER_OF_USERS;i++) {
						if (i == toRateUserId) { continue; }
						if (ratings[i][toRateMovieId] == 0) { continue; }
						int numberOfMoviesRatedByBothUsers = 0;
						double factor1 = 0;
						double factor2 = 0;
						double factor3 = 0;
						for (int j = 0;j < MagicNumbers.NUMBER_OF_MOVIES;j++) {
							if (ratings[i][j] != 0 && ratings[toRateUserId][j] != 0) {
								numberOfMoviesRatedByBothUsers++;
								factor1 += (ratings[i][j]-averageRating[i])*(ratings[toRateUserId][j]-averageRating[toRateUserId]);
								factor2 += Math.pow(ratings[i][j]-averageRating[i],2);
								factor3 += Math.pow(ratings[toRateUserId][j]-averageRating[toRateUserId],2);
							}
						}
						factor2 = Math.sqrt(factor2);
						factor3 = Math.sqrt(factor3);
						if (numberOfMoviesRatedByBothUsers == 0) { continue; }
						if (factor1 == 0) {
							userSimilarity.add(new Pair<Integer, Double>(i, 0.9));
						} else {
							userSimilarity.add(new Pair<Integer, Double>(i, factor1/(factor2*factor3)));
						}
					}
					break;
				case "cosine":
					for (int i = 0;i < MagicNumbers.NUMBER_OF_USERS;i++) {
						if (i == toRateUserId) { continue; }
						if (ratings[i][toRateMovieId] == 0) { continue; }
						int numberOfMoviesRatedByBothUsers = 0;
						double factor1 = 0;
						double factor2 = 0;
						double factor3 = 0;
						for (int j = 0;j < MagicNumbers.NUMBER_OF_MOVIES;j++) {
							if (ratings[i][j] != 0 && ratings[toRateUserId][j] != 0) {
								numberOfMoviesRatedByBothUsers++;
								factor1 += (ratings[i][j])*(ratings[toRateUserId][j]);
								factor2 += Math.pow(ratings[i][j],2);
								factor3 += Math.pow(ratings[toRateUserId][j],2);
							}
						}
						factor2 = Math.sqrt(factor2);
						factor3 = Math.sqrt(factor3);
						if (numberOfMoviesRatedByBothUsers == 0) { continue; }
						if (factor1 == 0) {
							userSimilarity.add(new Pair<Integer, Double>(i, 0.9));
						} else {
							userSimilarity.add(new Pair<Integer, Double>(i, factor1/(factor2*factor3)));
						}

					}
					break;
				default:
					System.err.println("Error");
				}
				
				// sort 'userSimilarity' by similarity
				Collections.sort(userSimilarity, (e1,e2)->{
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
				int neighbors = userSimilarity.size();
				if (neighbors == 0) { 
					prediction.add((double)averageRating[toRateUserId]);
					continue;
				}
				neighbors = (int)(neighbors * MagicNumbers.PERCENTAGE_OF_NEIGHBORS)+1;
				if (neighbors > MagicNumbers.LEAST_NUMBER_OF_NEIGHBORS) {
					neighbors = MagicNumbers.LEAST_NUMBER_OF_NEIGHBORS;
				}
				double normalization = 0;
				double value = 0;
				for (int i = 0;i < neighbors;i++) {
					normalization += userSimilarity.get(i).getSecond();
					value += userSimilarity.get(i).getSecond()*(ratings[userSimilarity.get(i).getFirst()][toRateMovieId]-averageRating[userSimilarity.get(i).getFirst()]);
				}
				//System.out.println(neighbors+" "+averageRating[toRateUserId]+" "+value+" "+normalization+" "+value/normalization);
				prediction.add(averageRating[toRateUserId]+value/normalization);
			}
		} catch (FileNotFoundException e) {
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
