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
				+ " [rating-file-path] [to-be-rated-file-path] [output-file-path]");
		System.out.println("2. java -jar MovieRecommender.jar item-based [similarity-calculator]"
				+ " [rating-file-path] [to-be-rated-file-path] [output-file-path]");
		System.out.println("3. java -jar MovieRecommender.jar heuristic"
				+ " [rating-file-path-1] [user-file-path-2] [item-file-path-3] [to-be-rated-file-path] [output-file-path]");
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
			if (args.length != 5) {
				help();
			}
			UserBasedRecommender userRecommender = new UserBasedRecommender();
			// multi-fold cross validation
			userRecommender.validate(args[1], args[2]);
			// predict results
			userRecommender.predict(args[1], args[2], args[3], args[4]);
			break;
		case "item-based":
			if (args.length != 5) {
				help();
			}
			ItemBasedRecommender itemRecommender = new ItemBasedRecommender();
			// multi-fold cross validation
			itemRecommender.validate(args[1], args[2]);
			// predict results
			//itemRecommender.predict(args[1], args[2], args[3], args[4]);
			break;
		case "heuristic":
			if (args.length != 6) {
				help();
			}
			HeuristicRecommender heuristicRecommender = new HeuristicRecommender();
			// multi-fold cross validation
			//heuristicRecommender.validate(args[1], args[2], args[3]);
			// predict results
			heuristicRecommender.predict(args[1], args[2], args[3], args[4], args[5]);
			break;
		default:
			help();
		}
	}
	
	public static void main(String[] args) {
		new MovieRecommender().run(args);
	}

}
