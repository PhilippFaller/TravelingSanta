import java.io.BufferedReader;
import java.io.BufferedWriter;
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
			List<Integer> primes = new LinkedList<>();
			primes.add(2);
			for (int i = 3; i <= max_number; i += 2) {
				if (notDivisableBy(primes, i)) {
					primes.add(i);
				}
			}
			this.primes = new HashSet<>();
			for (int p : primes) {
				this.primes.add(p);
			}
		}
	}

	private boolean notDivisableBy(List<Integer> divisors, int n) {
		for (int d : divisors) {
			if (n % d == 0) {
				return false;
			} else if (d >= Math.sqrt(n)) {
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
			curr_city = next_city;
		}
		return dist;
	}

	public List<Integer> findRandomPath() {
		List<Integer> path = new ArrayList<Integer>();
		for (int i = 1; i < cities.size(); i++) {
			path.add(i);
		}
		Collections.shuffle(path);
		path.add(0, 0);
		path.add(0);
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
		List<Integer> path = new ArrayList<>();
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

	public List<Integer> findSimulatedAnnealingPath() {
		List<Integer> path = findGreedyPath();
		double curr_dist = pathDist(path);
		int path_len = path.size();
		int iterations = path_len * 2;
		for (int epoch = 0; epoch < iterations; epoch++) {
			double temp = (iterations - epoch) / (double) iterations;
			// for (int i = 2; i < path_len-1; i++) {
			// curr_dist = maybe_swap(path, curr_dist, i-1, i, temp);
			// }
			int fst_idx = (int) (Math.random() * (path_len - 2)) + 1;
			int snd_idx = (int) (Math.random() * (path_len - 2)) + 1;
			curr_dist = maybe_swap(path, curr_dist, Math.min(fst_idx, snd_idx), Math.max(fst_idx, snd_idx), temp);
		}
		return path;
	}

	private double maybe_swap(List<Integer> path, double curr_dist, int fst, int snd, double temp) {
		double new_dist = calcDistAfterSwap(path, curr_dist, fst, snd);
		double random = Math.random();
		double treshold = Math.exp(-(curr_dist - new_dist) / (curr_dist * temp));
		if (new_dist < curr_dist) {
			int x = path.get(fst);
			path.set(fst, path.get(snd));
			path.set(snd, x);
			double pd = pathDist(path);
			if (new_dist != pd) {
				System.out.println(fst + " " + snd);
				System.out.println(pd + " " + new_dist);
			}
			curr_dist = new_dist;
		}
		return curr_dist;
	}

	private double calcDistAfterSwap(List<Integer> path, double curr_dist, int fst, int snd) {
		if (fst == snd) {
			return curr_dist;
		}
		if ((snd - fst) == 1) {
			double dist_to_fst = cityDist(path.get(fst - 1), path.get(fst), fst);
			double dist_between = cityDist(path.get(fst), path.get(snd), snd);
			double dist_from_snd = cityDist(path.get(snd), path.get(snd - 1), snd + 1);
			double new_dist = curr_dist - (dist_to_fst + dist_between + dist_from_snd);
			dist_to_fst = cityDist(path.get(fst - 1), path.get(snd), fst);
			dist_between = cityDist(path.get(snd), path.get(fst), snd);
			dist_from_snd = cityDist(path.get(fst), path.get(snd + 1), snd + 1);
			new_dist += dist_to_fst + dist_between + dist_from_snd;
			return new_dist;
		} else {
			double dist_to_fst = cityDist(path.get(fst - 1), path.get(fst), fst);
			double dist_from_fst = cityDist(path.get(fst), path.get(fst + 1), fst + 1);
			double dist_to_snd = cityDist(path.get(snd - 1), path.get(snd), snd);
			double dist_from_snd = cityDist(path.get(snd), path.get(snd + 1), snd + 1);
			double new_dist = curr_dist - (dist_to_fst + dist_from_fst + dist_to_snd + dist_from_snd);
			dist_to_fst = cityDist(path.get(fst - 1), path.get(snd), fst);
			dist_from_fst = cityDist(path.get(snd), path.get(fst + 1), fst + 1);
			dist_to_snd = cityDist(path.get(snd - 1), path.get(fst), snd);
			dist_from_snd = cityDist(path.get(fst), path.get(snd + 1), snd + 1);
			new_dist += (dist_to_fst + dist_from_fst + dist_to_snd + dist_from_snd);
			return new_dist;
		}
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
		long t0 = System.currentTimeMillis();
		Solver s = new Solver();
		List<Integer> p = s.findSimulatedAnnealingPath();
		long t1 = System.currentTimeMillis();
		System.out.println("Distance: " + s.pathDist(p));
		System.out.println("Time: " + (t1 - t0) / 1000);
		s.savePath(p, "sim_anneal.csv");
	}

}
