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
	private double overallAverage;
	private double[] averagePerGenre;
	
	{
		averagePerGenre = new double[18];
	}
	
	public UserProfile(boolean isMale, int age, int occupation,
			double overallAverage, double[] averagePerGenre) {
		this.isMale = isMale;
		this.age = age;
		this.occupation = occupation;
		this.overallAverage = overallAverage;
		this.averagePerGenre = averagePerGenre;
	}
	
	public double similarityAmplify(UserProfile another) {
		double amplification = 1;
		
		
		return amplification;
	}
	
}
