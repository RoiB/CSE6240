# How to run

Java SE 8 is required to run the program.
I have exported my code as a jar file.
Three kinds of arguments to run the program, corresponding to each type of recommender:

    java -jar MovieRecommender.jar user-based [similarity-calculator] [rating-file-path] [to-be-rated-file-path] [output-file-path] (validation)?
    java -jar MovieRecommender.jar item-based [similarity-calculator] [rating-file-path] [to-be-rated-file-path] [output-file-path] (validation)?
    java -jar MovieRecommender.jar heuristic [rating-file-path-1] [user-file-path-2] [item-file-path-3] [to-be-rated-file-path] [output-file-path] (validation)?

Note that three kinds of similarity-calculators are supported: Jaccard Pearson cosine 
“validation” is optional. If added, then the program will do cross validation.

for example:

    java -jar MovieRecommender.jar user-based Jaccard ratings.csv toBeRated.csv output.csv
will use a user-based method to recommend movies, the similarity between users are calculated by Jaccard similarity. Ratings will be read from ratings.csv. Movies to be rated will be read form toBeRated.csv. And output will be output.csv.
    
    java -jar MovieRecommender.jar item-based Jaccard ratings.csv toBeRated.csv output.csv validation
will use an item-based method to do the cross validation.


# Code organization

Code root is at MovieRecommender/main/java

package movie:
    MovieRecommender.java: main entry of the program, check arguments

package movie.util:
    Pair.java: represents a pair of objects

package movie.configuration:
    MagicNumbers.java: contains some public static magic numbers

package movie.recommender:
    UserProfile.java
    MovieGenre.java
    UserBasedRecommender.java: a user-based recommender
    ItemBasedRecommender.java: an item-based recommender
    HeuristicRecommender.java: a recommender based on user and item attributes

