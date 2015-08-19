package RAYTRACER;

import java.util.ArrayList;

//Represents intersection between a ray and the objects of the scene
public class Intersection {

	public double[] intersection_point;
	public Shape shape;
	public ArrayList<Double> t_values;

	public Intersection(double[] i, Shape s, ArrayList<Double> t) {
		intersection_point = i;
		shape = s;
		t_values = t;
	}

}
