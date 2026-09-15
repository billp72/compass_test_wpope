# run server
cd compass_wpope_backend <br />
.\gradlew.bat run

# run client
cd compass_wpope_frontend <br />
npm.cmd run dev


## Scoring criteria
# Presumably, the goal of the user is to find the most recent listings that are closest to their budget.
(results may vary depending on other filter selections like location)

# Opinionated version
* First, I would have the user enter a budget.
* Next, take the results (after filtering or searching is accomplished) and sort them by recency where the top 10 results each get a 10 (the highest score) and the next set of 10 get a 9 and so on until listings are too old and receive zero 
* Finally, evaluate the list again by comparing the home price to the budget, where every 10k above the budget deducts another 1 from the score
So even if a listing is recent, it might score low.

# User-governed version
So not to presume too much, I would probably allow the user to determine which is more important - the age of the listing or their budget.
If budget is the priority, the user could elect to score based on budget.
If age is the priority, we would score listings by age (similar to the above)
