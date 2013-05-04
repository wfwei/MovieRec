/**
 * 
 */
package baidu.entity;

/**
 * @author muti
 * 
 */
public class Record {
	private int userId;
	private int movieId;
	private double score;

	/**
	 * <p>
	 * Creates an instance of this class.
	 * </p>
	 */
	public Record(int userId, int movieId, double score) {
		super();
		this.userId = userId;
		this.movieId = movieId;
		this.score = score;
	}

	/**
	 * <p>
	 * Gets the namesake instance field.
	 * </p>
	 * 
	 * @return the userId
	 */
	public int getUserId() {
		return userId;
	}

	/**
	 * <p>
	 * Sets the namesake instance field.
	 * </p>
	 * 
	 * @param userId
	 *            the userId to set
	 */
	public void setUserId(int userId) {
		this.userId = userId;
	}

	/**
	 * <p>
	 * Gets the namesake instance field.
	 * </p>
	 * 
	 * @return the movieId
	 */
	public int getMovieId() {
		return movieId;
	}

	/**
	 * <p>
	 * Sets the namesake instance field.
	 * </p>
	 * 
	 * @param movieId
	 *            the movieId to set
	 */
	public void setMovieId(int movieId) {
		this.movieId = movieId;
	}

	/**
	 * <p>
	 * Gets the namesake instance field.
	 * </p>
	 * 
	 * @return the score
	 */
	public double getScore() {
		return score;
	}

	/**
	 * <p>
	 * Sets the namesake instance field.
	 * </p>
	 * 
	 * @param score
	 *            the score to set
	 */
	public void setScore(double score) {
		this.score = score;
	}

	public String toString() {
		return new StringBuilder().append(userId).append("\t").append(movieId)
				.append("\t").append(score).append("\n").toString();
	}
}
