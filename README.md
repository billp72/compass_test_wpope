
# Backend setup and run 
java -version (25 or higher) <br />
cd compass_wpope_backend <br />
Windows: .\gradlew.bat run <br />
MacOs: ./gradlew run

# Frontend setup and run
cd compass_wpope_frontend <br />
npm install <br />
npm.cmd run dev

# Scoring criteria
If the goal of a home-buyer is to find the most recent listings that are closest to their budget, the below method would deliver the most relevant listings.

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

# Dedup question.
During the interview, the interviewer asked me how I would prevent duplicate adresses and with different abbreviations etc 

On the train home, I thought about it.

* I think normalization when the data is entered would remove different abbreviations (e.g., ave vs. avenue)
* Once the data is normalized, you could use a lookup table to remove duplicates when the data is fetched
* Perhaps the best way is to normalize the addresses when they're input and, at 2AM on Saturday, run a batch process and do the checking then.
* if the data is scraped, simply run a batch process on the scrapped data or after it's entered at 2AM

# out-of-order search keyword network request

ExecutorService executor = Executors.newSingleThreadExecutor();
Future<Response> future = executor.submit(() ->
    httpClient.send(request, HttpResponse.BodyHandlers.ofString())
);

// When query changes, cancel the old future. This will start a new query and prevent stale queries from giving false results
future.cancel(true);

# problem of duplicate listings
this is a throny issue that I tried to solve with the least amount of overhead
* I created a method called Dedupe
* I passed the paginated data, the master data list, and the page size to it
* I normalized the address and removed all punctuation and things like: apartment, apt, ste, suite, etc
* I created a hash table, using the normilized addresses as keys, and checked for their existance
* If a dup existed, I exclude it from the subset
* I then use the master list to backfill (checking for dups there too) the missing listings based on the pageSize
* I returned the deduped list

Problems: If a listing on one page is a duplicate on the next, it will not remove it because the memory is wiped between page requests<br />
Possible solution: store duplicates in a database and check for them there, key 410pinestr10b|90210

the overhead would be tiny because I'm only brute-forcing (O(1)) paginated data which could be no more than 50 rows
