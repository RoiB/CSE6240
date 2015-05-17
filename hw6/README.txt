RATINGS FILE DESCRIPTION
================================================================================

All ratings are contained in the file "ratings.csv" and are in the
following format:

UserID,MovieID,Rating,Timestamp

- UserIDs range between 1 and 1000
- MovieIDs range between 1 and 2069
- Ratings are made on a 5-star scale (whole-star ratings only)
- Timestamp is represented in seconds since the epoch as returned by time(2)

TO BE RATED FILE DESCRIPTION
================================================================================

All ratings are contained in the file "toBeRated.csv" and are in the
following format:

UserID,MovieID

- You are asked to predict the ratings for these cells
- output the corresponding predicted ratings (only the ratings each line) in a file named result.csv.