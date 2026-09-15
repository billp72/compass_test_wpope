# run server
cd compass_wpope_backend <br />
.\gradlew.bat run

# run client
cd compass_wpope_frontend <br />
npm.cmd run dev


## Scoring criteria
# Presumably, the goal of a home-buyer is to find the most recent listings that are closest to their budget.

# Opinionated version
* First, create an input field called Budget.
* Next, take the results (after filtering or searching is accomplished) and rank them by recency, where the 10 most recent get a 10 score (the highest score), and the next set of 10 get a 9, and so on to 0
* Next, evaluate the list again by comparing the home price to the budget, where every 10k above the budget deducts another 1 from the score
So even if a listing is recent, it might score low.
* Finally, create a column called Score on the UI, sort the list by rank on the server, and send it to the client-side

# User-governed version
So not to presume too much, I would probably allow the user to determine which is more important - the age of the listing or their budget.
If budget is the priority, the user could elect to score based on budget.
If age is the priority, we would score listings by age (similar to the above)

(results may vary depending on filter selections like location)
