import java.io.*;
import java.util.*;

public class Utils {

    private static Random rng = new Random();
	
    // we support 2 layouts:
    //
    // Whataburger-style rows (7 columns):
    // 0 id
    // 1 address
    // 2 city
    // 3 state
    // 4 zip
    // 5 latitude
    // 6 longitude
    //
    // Starbucks-style rows (8 columns):
    // 0 id
    // 1 street
    // 2 extra
    // 3 city
    // 4 state
    // 5 zip
    // 6 latitude
    // 7 longitude
    //
    // if the CSV has a header row we skip first line

    public static ArrayList<Store> readStoresCSV(String filename) throws Exception {
        ArrayList<Store> stores = new ArrayList<>();

        BufferedReader br = new BufferedReader(new FileReader(filename));
        String line;

        // skip header
        br.readLine();

        while ((line = br.readLine()) != null) {
            if (line.trim().length() == 0) continue;

            String[] parts = line.split(",");

            String id;
            String street;
            String extra = "";
            String city;
            String state;
            String zip;
            double lat;
            double lon;

            if (parts.length == 7) {
                // whataburger style (or starbucks row that ended up collapsed to 7 cols in the file)
                id     = parts[0].trim();
                street = parts[1].trim();
                city   = parts[2].trim();
                state  = parts[3].trim();
                zip    = parts[4].trim();
                lat    = Double.parseDouble(parts[5].trim());
                lon    = Double.parseDouble(parts[6].trim());

            } else if (parts.length == 8) {
                // starbucks style (clean 8 cols)
                id     = parts[0].trim();
                street = parts[1].trim();
                extra  = parts[2].trim();
                city   = parts[3].trim();
                state  = parts[4].trim();
                zip    = parts[5].trim();
                lat    = Double.parseDouble(parts[6].trim());
                lon    = Double.parseDouble(parts[7].trim());

            } else {
                // unexpected format, skip row
                continue;
            }

            // build finalAddress
            String finalAddress;
            if (parts.length == 8) {
                // starbucks style where street + extra are separate columns
                if (extra != null && extra.length() > 0) {
                    finalAddress = street + ", " + extra;
                } else {
                    finalAddress = street;
                }
            } else {
                // whataburger style OR already-combined starbucks style
                finalAddress = street;
            }

            // normalize " - " -> ", " unless it's the John Peace Library one (cause that one is annoying)
            if (finalAddress.contains(" - ")) {
                if (!finalAddress.contains("John Peace Library")) {
                    finalAddress = finalAddress.replace(" - ", ", ");
                }
            }

            Store s = new Store(id, finalAddress, city, state, zip, lat, lon);
            stores.add(s);
        }

        br.close();
        return stores;
    }

    // read Queries.csv
    public static ArrayList<Query> readQueriesCSV(String filename) throws Exception {
        ArrayList<Query> list = new ArrayList<>();

        BufferedReader br = new BufferedReader(new FileReader(filename));
        String line;

        // skip header
        br.readLine();

        while ((line = br.readLine()) != null) {
            if (line.trim().isEmpty()) continue;

            String[] parts = line.split(",");
            double qLat = Double.parseDouble(parts[0].trim());
            double qLon = Double.parseDouble(parts[1].trim());
            int k       = Integer.parseInt(parts[2].trim());

            list.add(new Query(qLat, qLon, k));
        }

        br.close();
        return list;
    }

    // compute distance for every store to this query point
    public static void updateAllDistances(ArrayList<Store> stores, Query q) {
        for (Store s : stores) {
            s.computeDistance(q.qLat, q.qLon);
        }
    }

    // swap helper
    private static void swap(ArrayList<Store> arr, int i, int j) {
        if (i == j) return;
        Store tmp = arr.get(i);
        arr.set(i, arr.get(j));
        arr.set(j, tmp);
    }

    // randomized partition on distance (puts pivot in final spot, everything <= pivot.dist on the left)
    private static int randPartition(ArrayList<Store> arr, int left, int right) {
        int pivotIndex = left + rng.nextInt(right - left + 1);

        swap(arr, pivotIndex, right);

        double pivotVal = arr.get(right).distance;
        int storeIndex = left;

        for (int i = left; i < right; i++) {
            if (arr.get(i).distance <= pivotVal) {
                swap(arr, i, storeIndex);
                storeIndex++;
            }
        }

        swap(arr, storeIndex, right);
        return storeIndex;
    }

    // randSelect: expected O(n)
    // returns the store that is kIndex-th smallest distance (0-based)
    public static Store randSelect(ArrayList<Store> arr, int left, int right, int kIndex) {
        if (left == right) {
            return arr.get(left);
        }

        int pivotIndex = randPartition(arr, left, right);

        if (kIndex == pivotIndex) {
            return arr.get(pivotIndex);
        } else if (kIndex < pivotIndex) {
            return randSelect(arr, left, pivotIndex - 1, kIndex);
        } else {
            return randSelect(arr, pivotIndex + 1, right, kIndex);
        }
    }

    // quickSort for final pretty print ordering
    public static void quickSort(ArrayList<Store> arr, int left, int right) {
        if (left >= right) return;

        int pivotIndex = randPartition(arr, left, right);
        quickSort(arr, left, pivotIndex - 1);
        quickSort(arr, pivotIndex + 1, right);
    }

    // collect all stores with distance <= kthDist (keeps ties, we will only print first k lines anyway)
    public static ArrayList<Store> collectWithin(ArrayList<Store> allStores, double kthDist) {
        ArrayList<Store> res = new ArrayList<>();
        for (Store s : allStores) {
            if (s.distance <= kthDist) {
                res.add(s);
            }
        }
        return res;
    }

	// sorts by distance and alphabetically
    public static void sortByDistanceThenPrintable(ArrayList<Store> arr) {
	    Collections.sort(arr, new Comparator<Store>() {
		    @Override
		    public int compare(Store a, Store b) {
                    // compare by distance first
                    if (a.distance < b.distance) return -1;
            	    if (a.distance > b.distance) return 1;

            	    // get the address strings only (skip "Store #" part)
            	    String sa = a.address;
            	    String sb = b.address;

            	    // remove leading numbers and spaces from each address
            	    sa = sa.replaceFirst("^\\d+\\s*", "");
            	    sb = sb.replaceFirst("^\\d+\\s*", "");

            	    // compare alphabetically ignoring case
            	    int cmp = sa.compareToIgnoreCase(sb);
            	    if (cmp != 0) return cmp;

            	    // fallback: if addresses are identical, compare full toString (for city/state/zip)
            	    return a.toString().compareTo(b.toString());
		    }
	   });
     }


    public static void printQueryAnswer(Query q, ArrayList<Store> sortedSubset) {
        System.out.println("The " + q.k + " closest stores to (" + q.qLat + ", " + q.qLon + "):");

        java.text.DecimalFormat df = new java.text.DecimalFormat("0.00");

        for (int i = 0; i < sortedSubset.size() && i < q.k; i++) {
            Store s = sortedSubset.get(i);
            System.out.println(s.toString() + " - " + df.format(s.distance) + " miles.");
        }

        System.out.println();
    }
}

