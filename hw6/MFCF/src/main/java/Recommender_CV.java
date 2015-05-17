import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.regex.Pattern;

class Pair {
	int rating;
	int partition;
	Pair(int rating, int partition) {
		this.rating = rating;
		this.partition = partition;
	}
}

public class Recommender_CV {

	private String ratingsFilePath;
	private int numberOfAttributes;
	private double learningRate;
	private double regularizationFactor;
	private int numberOfFolds;
	
	public void run(String[] arguments) {
		this.parseArguments(arguments);
		this.crossValidation();
	}
	
	private void crossValidation() {
		// read ratings data
		// assign each rating a partition
		Map<Integer, Map<Integer, Pair>> ratings = new HashMap<Integer, Map<Integer,Pair>>();
		try (BufferedReader br = new BufferedReader(new FileReader(new File(ratingsFilePath)))) {
			String line = null;
			while ((line=br.readLine()) != null) {
				String[] item = line.split(Pattern.quote(","));
				if (item.length != 4) { continue; }
				int userId = Integer.parseInt(item[0]);
				int movieId = Integer.parseInt(item[1]);
				int rating = Integer.parseInt(item[2]);
				int partition = getRandomPartition();
				
				Map<Integer, Pair> oneUser = ratings.get(userId);
				if (oneUser == null) {
					oneUser = new HashMap<>();
					ratings.put(userId, oneUser);
					oneUser.put(movieId, new Pair(rating, partition));
				} else {
					oneUser.put(movieId, new Pair(rating, partition));
				}
			}
		} catch (IOException e) {
			showErrorAndExit(e, 1);
		}
		
		// create temporary files
		for (int i = 0;i != numberOfFolds;i++) {
			try (
					BufferedWriter bwRatings = new BufferedWriter(new FileWriter(new File("ratings_"+i)));
					BufferedWriter bwToBeRated = new BufferedWriter(new FileWriter(new File("to_be_rated_"+i)));
					BufferedWriter bwGroundTruth = new BufferedWriter(new FileWriter(new File("ground_truth_"+i)));
					) {
				for (Entry<Integer, Map<Integer, Pair>> userEntry : ratings.entrySet()) {
					for (Entry<Integer, Pair> oneItem : userEntry.getValue().entrySet()) {
						int userId = userEntry.getKey();
						int movieId = oneItem.getKey();
						int rating = oneItem.getValue().rating;
						int partition = oneItem.getValue().partition;
						if (partition == i) {
							bwToBeRated.write(""+userId+","+movieId+"\n");
							bwGroundTruth.write(rating+"\n");
						} else {
							bwRatings.write(""+userId+","+movieId+","+rating+","+"0\n");
						}
					}
				}
			} catch (IOException e) {
				showErrorAndExit(e, 1);
			}
		}
		
		// cross validation
		double totalError = 0;
		Recommender recommender = new Recommender();
		for (int i = 0;i != numberOfFolds;i++) {
			String[] arguments = {"ratings_"+i, "to_be_rated_"+i, ""+numberOfAttributes, ""+learningRate, ""+regularizationFactor};
			recommender.run(arguments);
			// compare error
			try (
					BufferedReader brGroundTruth = new BufferedReader(new FileReader(new File("ground_truth_"+i)));
					BufferedReader brResult = new BufferedReader(new FileReader(new File("results.csv")))
					) {
				String groundTruthLine = null;
				String resultLine = null;
				int count = 0;
				double currentError = 0;
				while ((groundTruthLine=brGroundTruth.readLine()) != null) {
					resultLine=brResult.readLine();
					count++;
					currentError += Math.pow(Double.parseDouble(groundTruthLine)-Double.parseDouble(resultLine), 2);
				}
				//System.out.println(Math.sqrt(currentError/count));
				totalError += Math.sqrt(currentError/count);
			} catch (IOException e) {
				showErrorAndExit(e, 1);
			}
		}
		System.out.println(totalError / numberOfFolds);
		
		// delete temporary files
		for (int i = 0;i != numberOfFolds;i++) {
			new File("ratings_"+i).delete();
			new File("to_be_rated_i"+i).delete();
			new File("ground_truth_i"+i).delete();
		}
	}
	
	private int getRandomPartition() {
		Random random = new Random();
		return random.nextInt(numberOfFolds);
	}
	
	private void parseArguments(String[] arguments) {
		if (arguments.length != 5) {
			this.showHelpAndExit(1);
		}
		
		ratingsFilePath = arguments[0];
		numberOfAttributes = Integer.parseInt(arguments[1]);
		learningRate = Double.parseDouble(arguments[2]);
		regularizationFactor = Double.parseDouble(arguments[3]);
		numberOfFolds = Integer.parseInt(arguments[4]);
	}
	
	public static void main(String[] args) {
		new Recommender_CV().run(args);
	}

	private void showHelpAndExit(int exitCode) {
		System.out.println("Arguments:");
		System.out.println("1. ratings file");
		System.out.println("2. number of attributes");
		System.out.println("3. learning rate");
		System.out.println("4. regulariztion factor");
		System.out.println("5. number of folds");
		System.exit(exitCode);
	}
	
	private void showErrorAndExit(Exception e, int exitCode) {
		e.printStackTrace();
		System.exit(exitCode);
	}
	
	
}
