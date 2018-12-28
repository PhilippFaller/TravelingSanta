import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.wlu.cs.levy.CG.KDTree;
import edu.wlu.cs.levy.CG.KeyDuplicateException;
import edu.wlu.cs.levy.CG.KeyMissingException;
import edu.wlu.cs.levy.CG.KeySizeException;

public class Solver {
	private Set<Integer> primes = null;
	private CityList cities = null;

	public Solver() {
		cities = new CityList(read_csv("data/cities.csv"));
		initPrimes(cities.size());
	}

	private void initPrimes(int max_number) {
		if (primes == null) {
			primes = new HashSet<>();
			primes.add(2);
			for (int i = 3; i <= max_number; i += 2) {
				if (notDivisableByPrime(i)) {
					primes.add(i);
				}
			}
		}
	}

	private boolean notDivisableByPrime(int n) {
		for (int p : primes) {
			if (n % p == 0) {
				return false;
			}
			if (p > Math.sqrt(n)) {
				break;
			}
		}
		return true;
	}

	public double cityDist(int from_idx, int to_idx, int step) {
		double factor = step % 10 == 0 && !primes.contains(from_idx) ? 1.1 : 1;
		double x = cities.getX(from_idx) - cities.getX(to_idx);
		double y = cities.getY(from_idx) - cities.getY(to_idx);
		return Math.sqrt((x * x + y * y)) * factor;
	}

	public double cityDist(int from_idx, int to_idx) {
		return cityDist(from_idx, to_idx, 1);
	}

	public double pathDist(List<Integer> path) {
		double dist = 0;
		if (path.isEmpty())
			return dist;
		Iterator<Integer> it = path.iterator();
		int curr_city = it.next();
		for (int i = 1; i < path.size(); i++) {
			int next_city = it.next();
			dist += cityDist(curr_city, next_city, i);
		}
		return dist;
	}

	public List<Integer> findRandomPath() {
		List<Integer> path = new ArrayList<Integer>();
		for (int i = 0; i < cities.size(); i++) {
			path.add(i);
		}
		Collections.shuffle(path);
		return path;
	}

	public List<Integer> findGreedyPath() {
		KDTree<Integer> unvisited_cities = new KDTree<>(2);
		for (int i = 1; i < cities.size(); i++) {
			try {
				unvisited_cities.insert(new double[] { cities.getX(i), cities.getY(i) }, i);
			} catch (KeySizeException | KeyDuplicateException e) {
				e.printStackTrace();
			}
		}
		List<Integer> path = new LinkedList<>();
		path.add(0);
		double[] curr_city = { cities.getX(0), cities.getY(0) };
		while (unvisited_cities.size() != 0) {
			try {
				int next_city = unvisited_cities.nearest(curr_city);
				path.add(next_city);
				curr_city[0] = cities.getX(next_city);
				curr_city[1] = cities.getY(next_city);
				unvisited_cities.delete(curr_city);
			} catch (KeySizeException | KeyMissingException e) {
				e.printStackTrace();
			}
		}
		path.add(0);
		return path;
	}

	private int nearestCity(int city, Set<Integer> unvisited) {
		double min_dist = Double.POSITIVE_INFINITY;
		int min_city = city;
		for (int other_city : unvisited) {
			double dist = cityDist(city, other_city);
			if (dist <= min_dist) {
				min_dist = dist;
				min_city = other_city;
			}
		}
		return min_city;
	}

	public List<Double> read_csv(String location) {
		List<Double> cities = new ArrayList<>();
		String line = "";
		try (BufferedReader br = new BufferedReader(new FileReader(location))) {
			br.readLine(); // Drop header
			while ((line = br.readLine()) != null) {
				String[] row = line.split(",");
				cities.add(Double.parseDouble(row[1]));
				cities.add(Double.parseDouble(row[2]));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return cities;
	}

	public void savePath(List<Integer> path, String filename) {
		try (BufferedWriter wr = new BufferedWriter(new FileWriter(filename))) {
			wr.write("Path\n");
			for (int city : path) {
				wr.write(String.valueOf(city));
				wr.write("\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		Solver s = new Solver();
		List<Integer> p = s.findGreedyPath();
		System.out.println(s.pathDist(p));
		s.savePath(p, "greedy.csv");
	}

}
