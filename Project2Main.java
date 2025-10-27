import java.util.*;

public class Project2Main {

    public static void main(String[] args) {

        try {
            // pick which stores file to load
            // if you pass an argument, we use that csv
            // if you don't pass anything, default to WhataburgerData.csv
            String storesFile;
            if (args.length >= 1) {
                storesFile = args[0];
            } else {
                storesFile = "WhataburgerData.csv";
            }

            // queries file is the same no matter what
            String queriesFile = "Queries.csv";

            // load data
            ArrayList<Store> masterStores = Utils.readStoresCSV(storesFile);
            ArrayList<Query> queries = Utils.readQueriesCSV(queriesFile);

            // for every query in Queries.csv
            for (Query q : queries) {

                // 1. compute distance from this query point to every store
                Utils.updateAllDistances(masterStores, q);

                // 2. find the k-th closest store distance using randomized selection
                int kIndex = q.k - 1;
                if (kIndex < 0) kIndex = 0;
                if (kIndex >= masterStores.size()) {
                    kIndex = masterStores.size() - 1;
                }

                // copy because randSelect mutates / partitions the list
                ArrayList<Store> work = new ArrayList<>(masterStores);

                Store kthStore = Utils.randSelect(work, 0, work.size() - 1, kIndex);
                double kthDist = kthStore.distance;

                // 3. collect all stores whose distance <= kthDist (so we include ties)
		ArrayList<Store> closeEnough = Utils.collectWithin(masterStores, kthDist);

                // 4. sort that subset of stores by distance using randomized quicksort
		Utils.sortByDistanceThenPrintable(closeEnough);

                // 5. print result block for this query
                Utils.printQueryAnswer(q, closeEnough);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

