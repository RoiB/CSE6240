package movie.recommender;

/**
 * User profile represents a user
 * 
 * @author Ke Wang
 *
 */
public class UserProfile {
	
	private boolean isMale;
	private int age;
	private int occupation;
	private int totalRated;
	private double overallAverage;
	private double[] averagePerGenre;
	
	{
		averagePerGenre = new double[18];
	}
	
	public UserProfile(boolean isMale, int age, int occupation,
			double overallAverage, double[] averagePerGenre,
			int totalRated) {
		this.isMale = isMale;
		this.age = age;
		this.occupation = occupation;
		this.overallAverage = overallAverage;
		this.averagePerGenre = averagePerGenre;
		this.totalRated = totalRated;
	}
	
	public double similarityAmplify(UserProfile another) {
		double amplification = 1;
		if (this.isMale() == another.isMale()) {
			amplification *= 5;
		}
		if (this.getOccupation() == another.getOccupation()) {
			amplification *= 1.1;
		}
		amplification *= (5 - 0.05*Math.abs(this.getAge()-another.getAge()));
		for (int genre = 0; genre != MovieGenre.NUMBER_OF_GENRES;genre++) {
			double diff = Math.abs(this.getAveragePerGenre()[genre]-another.getAveragePerGenre()[genre]);
			if (diff >= 0.5) { 
				continue; 
			} else {
				amplification *= 1 + (0.5-diff)*0.1;
			}
			
		}
		return amplification;
	}

	public boolean isMale() {
		return isMale;
	}

	public int getAge() {
		return age;
	}

	public int getOccupation() {
		return occupation;
	}

	public int getTotalRated() {
		return this.totalRated;
	}
	
	public double getOverallAverage() {
		return overallAverage;
	}

	public double[] getAveragePerGenre() {
		return averagePerGenre;
	}
	
}
