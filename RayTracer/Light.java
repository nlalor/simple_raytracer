package RAYTRACER;

//Light source for the ray tracer
public class Light {

	public double[] light_position;
	public double[] light_color;

	public Light(double[] p, double[] c) {
		light_position = p;
		light_color = c;
	}

}
