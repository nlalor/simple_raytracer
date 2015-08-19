package RAYTRACER;

//Represents a ray, which has a direction and an origin
public class NRay {

	public double[] direction;
	public double[] origin;

	public NRay(double[] d, double[] o) {
		direction = d;
		origin = o;
	}

	//Calculates a point on the ray given a t value
	//Params: double t, starts from ray origin and scales by t in the direction of ray
	public double[] pointOnRay(double t) {
		double[] point = new double[3];
		for(int i = 0; i < 3; i++) {
			point[i] = origin[i] + t * direction[i];
		}
		return point;
	}
}
