Two Java files are in the folder:
1. Recommender.java	for recommendation
2. Recommender_CV.java	for cross validation, using Recommender.java

To run:
1. compile
javac Recommender.java
javac Recommender_CV.java
2. run
java Recommender ratings.csv toBeRated.csv 1 0.001 0.05
java Recommender_CV ratings.csv 1 0.001 0.05 10

results will be in results.csv file
   