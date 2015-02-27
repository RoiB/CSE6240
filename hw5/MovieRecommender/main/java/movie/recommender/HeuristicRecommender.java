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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

import movie.configuration.MagicNumbers;
import movie.util.Pair;

public class HeuristicRecommender {
	public void validate(String inputFilePath, String usersFilePath, String moviesFilePath) {
		
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
		short[][] ratings = new short[MagicNumbers.NUMBER_OF_USERS][MagicNumbers.NUMBER_OF_MOVIES];
		File ratingsFile = new File(ratingsFilePath);
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
		UserProfile[] userProfiles = new UserProfile[MagicNumbers.NUMBER_OF_USERS];
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
				int count = 0;
				for (int movie = 0;movie < MagicNumbers.NUMBER_OF_MOVIES;movie++) {
					if (ratings[userId][movie] != 0) {
						count++;
						total += ratings[userId][movie];
						if (movieGenres.get(movie) == null) { continue; }
						for (int genreId : movieGenres.get(movie)) {
							countPerGenre[genreId]++;
							totalPerGenre[genreId] += ratings[userId][movie];
						}
					}
				}
				double average = total/count;
				for (int ii = 0;ii < MovieGenre.NUMBER_OF_GENRES;ii++) {
					if (countPerGenre[ii] == 0) {
						averagePerGenre[ii] = average;
					} else {
						averagePerGenre[ii] = totalPerGenre[ii] / countPerGenre[ii];
					}
				}
				
				// build user profile
				UserProfile currentUserProfile = new UserProfile(isMale, age, occupation, average, averagePerGenre, count);
				userProfiles[userId] = currentUserProfile;
				//System.out.println(userId+" "+isMale+" "+occupation+" "+average+" "+averagePerGenre[0]);
			}
		} catch (FileNotFoundException e) {
			System.err.println(e);
			System.exit(1);
		}
		System.out.println("Users Information Loaded.");
		
		// buildUserSimilarityMatrix
		// user-based similarity * amplification
		double[][] userSimilarityMatrix = new double[MagicNumbers.NUMBER_OF_USERS][MagicNumbers.NUMBER_OF_USERS];
		for (int user1 = 0;user1 != MagicNumbers.NUMBER_OF_USERS-1;user1++) {
			for (int user2 = user1+1;user2 != MagicNumbers.NUMBER_OF_USERS;user2++) {
				double amplification = userProfiles[user1].similarityAmplify(userProfiles[user2]);
				/*double factor1 = 0;
				double factor2 = 0;
				double factor3 = 0;
				int numberOfMoviesRatedByBothUsers = 0;
				for (int movie = 0;movie < MagicNumbers.NUMBER_OF_MOVIES;movie++) {
					if (ratings[user1][movie] != 0 && ratings[user2][movie] != 0) {
						numberOfMoviesRatedByBothUsers++;
						factor1 += (ratings[user1][movie]-userProfiles[user1].getOverallAverage())*
								(ratings[user2][movie]-userProfiles[user2].getOverallAverage());
						factor2 += Math.pow(ratings[user1][movie]-userProfiles[user1].getOverallAverage(),2);
						factor3 += Math.pow(ratings[user2][movie]-userProfiles[user2].getOverallAverage(),2);
					}
				}
				if (numberOfMoviesRatedByBothUsers == 0) { continue; }
				factor2 = Math.sqrt(factor2);
				factor3 = Math.sqrt(factor3);
				if (factor1 == 0) {
					userSimilarityMatrix[user1][user2] = 0.5*amplification;
					userSimilarityMatrix[user2][user1] = 0.5*amplification;
				} else {
					userSimilarityMatrix[user1][user2] = factor1/(factor2*factor3)*amplification;
					userSimilarityMatrix[user2][user1] = factor1/(factor2*factor3)*amplification;
				}*/
				userSimilarityMatrix[user1][user2] = amplification;
				userSimilarityMatrix[user2][user1] = amplification;
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
				int toRateUserId = Integer.parseInt(line.substring(0, index))-1;
				int toRateMovieId = Integer.parseInt(line.substring(index+1))-1;
				
				List<Pair<Integer, Double>> possibleNeighbors = new ArrayList<>();
				for (int user = 0;user < MagicNumbers.NUMBER_OF_USERS;user++) {
					if (ratings[user][toRateMovieId] != 0) {
						possibleNeighbors.add(new Pair<Integer, Double>(user,userSimilarityMatrix[user][toRateUserId]));
					}
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

				// choose neighbors
				int neighbors = possibleNeighbors.size();
				if (neighbors == 0) {
					prediction.add(userProfiles[toRateUserId].getOverallAverage());
					continue;
				}
				neighbors = (int)(neighbors * MagicNumbers.PERCENTAGE_OF_NEIGHBORS)+1;
				if (neighbors > MagicNumbers.LEAST_NUMBER_OF_NEIGHBORS) {
					neighbors = MagicNumbers.LEAST_NUMBER_OF_NEIGHBORS;
				}
				
				// calculate prediction
				double normalization = 0;
				double value = 0;
				for (int neighbor = 0;neighbor != neighbors;neighbor++) {
					normalization += possibleNeighbors.get(neighbor).getSecond();
					value += possibleNeighbors.get(neighbor).getSecond()*
							(ratings[possibleNeighbors.get(neighbor).getFirst()][toRateMovieId]-
									userProfiles[possibleNeighbors.get(neighbor).getFirst()].getOverallAverage());
				}
				if (normalization == 0) {
					prediction.add(userProfiles[toRateUserId].getOverallAverage());
				} else {
					prediction.add(userProfiles[toRateUserId].getOverallAverage()+value/normalization);
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
