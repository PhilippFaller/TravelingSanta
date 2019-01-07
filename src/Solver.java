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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

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
	
	public void gridSearchForGreedy(int step_size) {
		double best_dist = Integer.MAX_VALUE;
		int best_k = -1;
		List<Integer> best_path = null;
		int num_threads = 8;
		int steps_per_thread = cities.size() / (num_threads-1);
		ExecutorService ex = Executors.newFixedThreadPool(num_threads);
		List<Future<Tupel<Double, Integer>>> results = new LinkedList<>();
		for(int i = 0; i < num_threads; i++) {
			int start_idx =i;
			int stop_idx = i+steps_per_thread;
			results.add(ex.submit(() -> gridSearchWorker(start_idx, stop_idx, step_size)));
		}
		ex.shutdown();
		for(Future<Tupel<Double, Integer>> t : results) {
			try {
				Tupel<Double, Integer> tupel = t.get(1, TimeUnit.DAYS);
				double dist = tupel.fst;
				System.out.println(dist);
				if(dist < best_dist) {
					best_dist = dist;
					best_k = tupel.snd;
				}
			} catch (InterruptedException | ExecutionException | TimeoutException e) {
				e.printStackTrace();
			}
			
		}
		best_path = findGreedyPath(best_k);
		savePath(best_path, "GridGreedy.csv");
		System.out.println("Dist " + best_dist + " for k=" + best_k);
	}
	
	public Tupel<Double, Integer> gridSearchWorker(int start, int stop, int step_size) {
		double best_dist = Integer.MAX_VALUE;
		int best_k = -1;
		for(int k = start; k < stop; k+=step_size) {
			List<Integer>p = findGreedyPath(k);
			double dist = pathDist(p);
			if (dist < best_dist) {
				best_dist = dist;
				best_k = k;
			}
		}
		return new Tupel<Double, Integer>(best_dist, best_k);		
	}

	public List<Integer> findGreedyPath(int k) {
		KDTree<Integer> unvisited_cities = new KDTree<>(2);
		for (int i = 1; i < cities.size(); i++) {
			try {
				unvisited_cities.insert(new double[] { cities.getX(i), cities.getY(i) }, i);
			} catch (KeySizeException | KeyDuplicateException e) {
				e.printStackTrace();
			}
		}
		List<Integer> pathFromStart = new LinkedList<>();
		List<Integer> pathFromEnd = new ArrayList<>();
		pathFromStart.add(0);
		pathFromEnd.add(0);
		double[] curr_city_start = { cities.getX(0), cities.getY(0) };
		double[] curr_city_end = curr_city_start;
//		int k = unvisited_cities.size()/2;
		int j = 0;
		while (unvisited_cities.size() != 0) {
			
			if(unvisited_cities.size() == 0) break;
			if(j<k) {
				curr_city_start = addNearestCity(pathFromStart, curr_city_start, unvisited_cities);
			}
			else 
				curr_city_end = addNearestCity(pathFromEnd, curr_city_end, unvisited_cities);
			j++;
		}
		for(int i = pathFromEnd.size()-1; i>=0; i--) {
			pathFromStart.add(pathFromEnd.get(i));
		}
		return pathFromStart;
	}
	
	public double[] addNearestCity(List<Integer> path, double[] curr_city, KDTree<Integer> unvisited_cities) {
		try {
			int next_city = unvisited_cities.nearest(curr_city);
			path.add(next_city);
			curr_city[0] = cities.getX(next_city);
			curr_city[1] = cities.getY(next_city);
			unvisited_cities.delete(curr_city);
		} catch (KeySizeException | KeyMissingException e) {
			e.printStackTrace();
		}
		return curr_city;
	}

	public List<Integer> findSimulatedAnnealingPath(double alpha) {
		List<Integer> path = findGreedyPath(0);
		// List<Integer> path = findRandomPath();
		double curr_dist = pathDist(path);
		int path_len = path.size();
		int iterations = path_len;
		double temp = 1;
		int num_threads = 8;
		ExecutorService exe = Executors.newFixedThreadPool(num_threads);
		ReentrantLock lock = new ReentrantLock();
		for (int epoch = 0; epoch < iterations; epoch++) {
			if (epoch % 1000 == 0)
				System.out.println("Epoche " + epoch + "/" + (iterations - 1));
			temp *= alpha;
			int swaps_per_worker = (path_len - 3) / (num_threads - 1);
			List<Future<Double>> results = new ArrayList<>();
			for (int worker = 0, start = 1; worker < num_threads; worker++, start += swaps_per_worker) {
				int start_index = start;
				double current_dist = curr_dist;
				double current_temp = temp;
				results.add(exe.submit(() -> swap_worker(path, current_dist, start_index,
						Math.min(start_index + swaps_per_worker, path_len - 2), current_temp, lock)));
			}
			for (Future<Double> f : results) {
				try {
					curr_dist -= f.get();
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
				}
			}

		}
		try {
			exe.shutdown();
			exe.awaitTermination(1, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return path;
	}

	public double swap_worker(List<Integer> path, double old_dist, int start, int end, double temp,
			ReentrantLock lock) {
		double local_dist = old_dist;
		for (int i = start + 1; i < end - 1; i++) {
			local_dist = maybe_swap(path, local_dist, i, i + 1, temp);
		}
		try {
			lock.lock();
			local_dist = maybe_swap(path, local_dist, end - 1, end, temp);
		} finally {
			lock.unlock();
		}
		return old_dist - local_dist;

	}

	private double maybe_swap(List<Integer> path, double curr_dist, int fst, int snd, double temp) {
		double new_dist = calcDistAfterSwap(path, curr_dist, fst, snd);
		double random = Math.random();
		double rel_imporvement = (curr_dist - new_dist) / curr_dist;
		double treshold = Math.exp(-rel_imporvement / temp);
		if (new_dist < curr_dist || random < treshold) {
			int x = path.get(fst);
			path.set(fst, path.get(snd));
			path.set(snd, x);
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
		} else { // Not possible in current version
			// double dist_to_fst = cityDist(path.get(fst - 1), path.get(fst), fst);
			// double dist_from_fst = cityDist(path.get(fst), path.get(fst + 1), fst + 1);
			// double dist_to_snd = cityDist(path.get(snd - 1), path.get(snd), snd);
			// double dist_from_snd = cityDist(path.get(snd), path.get(snd + 1), snd + 1);
			// double new_dist = curr_dist - (dist_to_fst + dist_from_fst + dist_to_snd +
			// dist_from_snd);
			// dist_to_fst = cityDist(path.get(fst - 1), path.get(snd), fst);
			// dist_from_fst = cityDist(path.get(snd), path.get(fst + 1), fst + 1);
			// dist_to_snd = cityDist(path.get(snd - 1), path.get(fst), snd);
			// dist_from_snd = cityDist(path.get(fst), path.get(snd + 1), snd + 1);
			// new_dist += (dist_to_fst + dist_from_fst + dist_to_snd + dist_from_snd);
			// return new_dist;
			return -1;
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
		s.gridSearchForGreedy(1000);
		long t1 = System.currentTimeMillis();
//		System.out.println("Distance: " + s.pathDist(p));
		System.out.println("Time: " + (t1 - t0) / 1000);
//		s.savePath(p, "greedy2.csv");
	}

}
