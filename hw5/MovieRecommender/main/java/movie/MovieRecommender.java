package movie;

import movie.recommender.HeuristicRecommender;
import movie.recommender.ItemBasedRecommender;
import movie.recommender.UserBasedRecommender;

/**
 * Main entry of the movie recommender system
 * 
 * @author Ke Wang
 *
 */
public class MovieRecommender {

	private void help() {
		System.out.println("Usage:");
		System.out.println("1. java -jar MovieRecommender.jar user-based [similarity-calculator]"
				+ " [rating-file-path] [to-be-rated-file-path] [output-file-path] (validation)?");
		System.out.println("2. java -jar MovieRecommender.jar item-based [similarity-calculator]"
				+ " [rating-file-path] [to-be-rated-file-path] [output-file-path] (validation)?");
		System.out.println("3. java -jar MovieRecommender.jar heuristic"
				+ " [rating-file-path-1] [user-file-path-2] [item-file-path-3] [to-be-rated-file-path] [output-file-path] (validation)?");
		return;
	}
	
	/**
	 * Parse arguments
	 * 
	 * @param args
	 */
	public void run(String[] args) {
		if (args.length == 0) {
			help();
			System.exit(1);
		}
		String method = args[0];
		switch (method) {
		case "user-based":
			if (args.length != 5 && args.length != 6) {
				help();
				System.exit(1);
			}
			if (!args[1].equals("Jaccard")&&!args[1].equals("Pearson")&&!args[1].equals("cosine")) {
				help();
				System.exit(1);
			}
			UserBasedRecommender userRecommender = new UserBasedRecommender();
			
			if (args.length == 5) {
				userRecommender.predict(args[1], args[2], args[3], args[4]);
			} else if (args.length == 6) {
				userRecommender.validate(args[1], args[2]);
			}
			
			// multi-fold cross validation
			//userRecommender.validate(args[1], args[2]);
			// predict results
			//userRecommender.predict(args[1], args[2], args[3], args[4]);
			break;
		case "item-based":
			if (args.length != 5 && args.length != 6) {
				help();
				System.exit(1);
			}
			if (!args[1].equals("Jaccard")&&!args[1].equals("Pearson")&&!args[1].equals("cosine")) {
				help();
				System.exit(1);
			}
			ItemBasedRecommender itemRecommender = new ItemBasedRecommender();
			if (args.length == 5) {
				itemRecommender.predict(args[1], args[2], args[3], args[4]);
			} else {
				itemRecommender.validate(args[1], args[2]);
			}
			// multi-fold cross validation
			//itemRecommender.validate(args[1], args[2]);
			// predict results
			//itemRecommender.predict(args[1], args[2], args[3], args[4]);
			break;
		case "heuristic":
			if (args.length != 6 && args.length != 7) {
				help();
			}
			HeuristicRecommender heuristicRecommender = new HeuristicRecommender();
			if (args.length == 6) {
				heuristicRecommender.predict(args[1], args[2], args[3], args[4], args[5]);
			} else {
				heuristicRecommender.validate(args[1], args[2], args[3]);
			}
			// multi-fold cross validation
			//heuristicRecommender.validate(args[1], args[2], args[3]);
			// predict results
			//heuristicRecommender.predict(args[1], args[2], args[3], args[4], args[5]);
			break;
		default:
			help();
		}
	}
	
	public static void main(String[] args) {
		new MovieRecommender().run(args);
	}

}
