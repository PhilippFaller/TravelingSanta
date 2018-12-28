import java.util.Iterator;
import java.util.List;

public class CityList {

	private List<Double> cities;
	
	public CityList(List<Double> cities) {
		this.cities = cities;
	}
	
	public double getX(int i) {
		return cities.get(2*i);
	}
	
	public double getY(int i) {
		return cities.get(2*i+1);
	}
	
	public int size() {
		return cities.size() / 2;
	}

}
