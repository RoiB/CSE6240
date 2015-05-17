package movie.recommender;

/**
 * Action
 * Adventure
 * Animation
 * Children's
 * Comedy
 * Crime
 * Documentary
 * Drama
 * Fantasy
 * Film-Noir
 * Horror
 * Musical
 * Mystery
 * Romance
 * Sci-Fi
 * Thriller
 * War
 * Western
 * 
 * @author Ke Wang
 *
 */
public class MovieGenre {
	
	public final static int NUMBER_OF_GENRES = 18;
	
 	private static String[] GENRES = {
 			"Action",
			"Adventure",
			"Animation",
			"Children\'s",
			"Comedy",
			"Crime",
			"Documentary",
			"Drama",
			"Fantasy",
			"Film-Noir",
			"Horror",
			"Musical",
			"Mystery",
			"Romance",
			"Sci-Fi",
			"Thriller",
			"War",
			"Western"
			};
	
	public static int genreToId(String s) {
		switch (s) {
		case "Action":
			return 0;
		case "Adventure":
			return 1;
		case "Animation":
			return 2;
		case "Children\'s":
			return 3;
		case "Comedy":
			return 4;
		case "Crime":
			return 5;
		case "Documentary":
			return 6;
		case "Drama":
			return 7;
		case "Fantasy":
			return 8;
		case "Film-Noir":
			return 9;
		case "Horror":
			return 10;
		case "Musical":
			return 11;
		case "Mystery":
			return 12;
		case "Romance":
			return 13;
		case "Sci-Fi":
			return 14;
		case "Thriller":
			return 15;
		case "War":
			return 16;
		case "Western":
			return 17;
		default:
			return -1;
		}
	}
	
	public static String idToGenre(int id) {
		if (id < 0 || id >= NUMBER_OF_GENRES) {
			return null;
		} else {
			return GENRES[id];
		}
	}
	
}
