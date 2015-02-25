package movie.recommender;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

import movie.configuration.MagicNumbers;
import movie.util.Pair;

public class UserBasedRecommender {
	
	public void validate(String similarityMethod, String inputFilePath) {
		// read data, assign each rating a number from 1 to number of partitions
		Map<Integer, Map<Integer, Pair<Integer, Integer>>> map = new TreeMap<Integer, Map<Integer,Pair<Integer,Integer>>>();
		File dataFile = new File(inputFilePath);
		try (Scanner sc = new Scanner(dataFile)) {
			
		} catch (FileNotFoundException e) {
			System.err.println(e);
			System.exit(1);
		}
	}
	
	public void predict(String similarityMethod, 
			String inputFilePath,
			String toBeRatedFilePath,
			String outputFilePath) {
		
		// read data from input file
		File ratingFile = new File(inputFilePath);
		TreeMap<Integer, List<Pair<Integer, Integer>>> map = new TreeMap<>();
		try (Scanner sc = new Scanner(ratingFile)) {
			while (sc.hasNextLine()) {
				Scanner l = new Scanner(sc.nextLine());
				l.useDelimiter(",");
				Integer userId = l.nextInt();
				Integer movieId = l.nextInt();
				Integer rating = l.nextInt();
				l.close();
				List<Pair<Integer, Integer>> listOfRatings = map.get(userId);
				if (listOfRatings != null) {
					listOfRatings.add(new Pair<Integer, Integer>(movieId, rating));
				} else {
					List<Pair<Integer, Integer>> newList = new ArrayList<Pair<Integer,Integer>>();
					newList.add(new Pair<Integer, Integer>(movieId, rating));
					map.put(userId, newList);
				}
			}
		} catch (FileNotFoundException e) {
			System.err.println(e);
			System.exit(1);
		}
		// for each user, sort by movie id
		for (Integer i : map.keySet()) {
			List<Pair<Integer, Integer>> list = map.get(i);
			Collections.sort(list, (e1,e2)->{
				if (e1.getFirst() > e2.getFirst()) {
					return -1;
				} else {
					return 1;
				}
			});
		}
		//System.out.println("Rating file reading complete");
		
		// compute average rating for each user
		Map<Integer, Double> averageRating = new TreeMap<Integer, Double>();
		for (Integer i : map.keySet()) {
			List<Pair<Integer, Integer>> list = map.get(i);
			//System.out.println(list.size());
			int totalRating = 0;
			for (Pair<Integer, Integer> p : list) {
				totalRating += p.getSecond();
			}
			averageRating.put(i, ((double)totalRating)/list.size());
		}
		
		// read toBeRatedFile and make prediction one by one
		List<Double> prediction = new ArrayList<Double>();
		File toBeRatedFile = new File(toBeRatedFilePath);
		try (Scanner sc = new Scanner(toBeRatedFile)) {
			while (sc.hasNextLine()) {
				String line = sc.nextLine();
				int index = line.indexOf(',');
				Integer userId = Integer.parseInt(line.substring(0, index));
				Integer movideId = Integer.parseInt(line.substring(index+1));
				List<Pair<Integer, Integer>> currentUserRatings = map.get(userId);
				
				// make prediction for userId and movieId based on similarity calculation method
				List<Pair<Integer, Double>> userSimilarity = new ArrayList<Pair<Integer, Double>>();
				switch (similarityMethod) {
				case "Jaccard":
					for (Integer i : map.keySet()) {
						if (i.intValue() == userId) {
							continue;
						}
						boolean flag = false;
						List<Pair<Integer, Integer>> oneOtherUserRatings = map.get(i);
						for (Pair<Integer, Integer> p : oneOtherUserRatings) {
							if (p.getFirst().intValue() == movideId) {
								flag = true;
								break;
							}
							if (p.getFirst() > movideId) {
								break;
							}
						}
						if (flag == false) {
							continue;
						}
						// this current user has rated 'movieId'
						int totalRatedMovies = 0;
						int sameRatedMovies = 0;
						int index1 = 0;
						int index2 = 0;
						while (index1 < currentUserRatings.size() && index2 < oneOtherUserRatings.size()) {
							if (currentUserRatings.get(index1).getFirst() == oneOtherUserRatings.get(index2).getFirst()) {
								sameRatedMovies++;
								index1++;
								index2++;
							} else if (currentUserRatings.get(index1).getFirst() > oneOtherUserRatings.get(index2).getFirst()) {
								index2++;
							} else {
								index1++;
							}
						}
						totalRatedMovies = currentUserRatings.size()+oneOtherUserRatings.size()-sameRatedMovies;
						userSimilarity.add(new Pair<Integer, Double>(i, ((double)sameRatedMovies)/totalRatedMovies));
					}
					break;
				case "Person":
					break;
				case "cosine":
					break;
				default:
					System.err.println("method type unknown");
					System.exit(1);
				}
				// use 'userSimilarity' to make prediction
				Collections.sort(userSimilarity, (e1, e2)->{
					if (e1.getSecond() > e2.getSecond()) {
						return -1;
					} else {
						return 1;
					}
				});
				int topNToChoose = MagicNumbers.LEAST_NUMBER_OF_NEIGHBORS; // choose how many as neighbors?
				if (userSimilarity.size() * MagicNumbers.PERCENTAGE_OF_NEIGHBORS > topNToChoose) {
					topNToChoose = (int)(userSimilarity.size() * MagicNumbers.PERCENTAGE_OF_NEIGHBORS);
				}
				if (userSimilarity.size() < topNToChoose) {
					topNToChoose = userSimilarity.size()/2;
				}
				if (topNToChoose == 0) {
					prediction.add(averageRating.get(userId));
					continue;
				}
				double similarity = 0;
				double normalization = 0;
				for (int ii = 0;ii < topNToChoose;ii++) {
					Pair<Integer, Double> current = userSimilarity.get(ii);
					normalization += current.getSecond();
					double currentAverage = averageRating.get(current.getFirst());
					List<Pair<Integer, Integer>> currentRatings = map.get(current.getFirst());
					for (Pair<Integer, Integer> p : currentRatings) {
						if (p.getFirst().intValue() == movideId) {
							similarity += current.getSecond()*((double)p.getSecond()-currentAverage);
							break;
						}
					}
				}
				prediction.add(averageRating.get(userId)+similarity/normalization);
			}
		} catch (FileNotFoundException e) {
			System.err.println(e);
			System.exit(1);
		}
		// output the result from 'prediction'
		File outputFile = new File(outputFilePath);
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile))) {
			for (Double i : prediction) {
				bw.write(i+"\n");
			}
		} catch (IOException e) {
			System.err.println(e);
			System.exit(1);
		}
		
	}
	
}
