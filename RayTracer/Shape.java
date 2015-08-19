package RAYTRACER;

import java.util.ArrayList;

//Define the abstract class of Shape
public abstract class Shape {
	//All shapes will have a specified location (x,y,z), surface color (red, green, blue), and co-efficients for lighting calculations
	double x, y, z;
	double red, green, blue;
	double ambient_level, diffuse_level, specular_level, refraction_level;

	//All shapes are required to have an intersection method as well as a findNormal method
	abstract Intersection intersect(NRay r);

	abstract NVector findNormal(double[] surface_point);
}

//Plane is a type of shape
class Plane extends Shape {

	//Normal will be specified at construction
	NVector plane_normal;

	//Planes are defined as a single point on the plane, and the normal of the plane
	public Plane(double _x, double _y, double _z, NVector normal, double _red,
			double _green, double _blue, double ka, double kd, double ks, double kr) {
		x = _x;
		y = _y;
		z = _z;
		plane_normal = normal;
		red = _red;
		green = _green;
		blue = _blue;
		ambient_level = ka;
		diffuse_level = kd;
		specular_level = ks;
		refraction_level = kr;
	}

	public Intersection intersect(NRay r) {
		//Get the direction of the specified ray
		NVector direction = new NVector(r.direction[0], r.direction[1],
				r.direction[2]);

		//If the dot product between ray direction and plane normal is 0, then the two are parallel and they will never intersect
		if (plane_normal.dot(direction) == 0) {
			return null;
		} 
		//Otherwise, there must be an intersection eventually	
		else {
			//Get the vector from the ray's origin to the point on the plane
			NVector origin_to_plane = new NVector((x - r.origin[0]),
					(y - r.origin[1]), (z - r.origin[2]));
			//Calculate the t value at which the ray intersects the plane
			double normal_dot_point = plane_normal.dot(origin_to_plane);
			double normal_dot_offset = plane_normal.dot(direction);
			double t_value = normal_dot_point / normal_dot_offset;
			//Bounds check the t value to make sure the intersection is behind us
			if (t_value >= 0 && t_value <= 1) {
				ArrayList<Double> t_values = new ArrayList<Double>();
				t_values.add(t_value);
				//Create and return the intersection
				Intersection i = new Intersection(r.pointOnRay(t_value), this,
						t_values);
				return i;
			}
			else {
				return null;
			}
		}
	}

	//Simply return the specified normal
	public NVector findNormal(double[] surface_point) {
		return plane_normal;
	}
}

//Sphere is a type of shape
class Sphere extends Shape {

	//All spheres have a radius provided during construction
	double radius;

	public Sphere(double _x, double _y, double _z, double r, double _red,
			double _green, double _blue, double ka, double kd, double ks, double kr) {
		x = _x;
		y = _y;
		z = _z;
		radius = r;
		red = _red;
		green = _green;
		blue = _blue;
		ambient_level = ka;
		diffuse_level = kd;
		specular_level = ks;
		refraction_level = kr;
	}

	public Intersection intersect(NRay r) {
		//Preparing for quadratic equation in which the roots are the t values for intersection points between ray and sphere
		//almost always a ray will enter at one point and exit at another, unless it is tangential to the sphere
		double a = (r.direction[0] * r.direction[0])
			+ (r.direction[1] * r.direction[1])
			+ (r.direction[2] * r.direction[2]);
		double b = 2 * r.direction[0] * (r.origin[0] - x) + 2 * r.direction[1]
			* (r.origin[1] - y) + 2 * r.direction[2] * (r.origin[2] - z);
		double c = (x * x)
			+ (y * y)
			+ (z * z)
			+ (r.origin[0] * r.origin[0])
			+ (r.origin[1] * r.origin[1])
			+ (r.origin[2] * r.origin[2])
			+ (-2 * (x * r.origin[0] + y * r.origin[1] + z * r.origin[2]) - radius
					* radius);

		double discriminant = b * b - 4 * a * c;

		ArrayList<Double> t_values = new ArrayList<Double>();

		//If discriminant is negative, the ray never touches the sphere 
		if (discriminant < 0)
			return null;
		else {
		//Otherwise get the roots of the quadratic equation
			double disc_sqrt = Math.sqrt(discriminant);
			double t0 = (-b - disc_sqrt) / (2 * a);
			double t1 = (-b + disc_sqrt) / (2 * a);
			//If the larger t value is negative then the sphere is behind us
			if (t1 < 0) {
				return null;
			}
			//If the smaller t value is negative then we only need the larger (negative t values are not valid)
			if (t0 < 0) {
				t_values.add(t1);
				return new Intersection(r.pointOnRay(t1), this, t_values);
			} 
			//If both t values are valid then add them to the intersection (we may need both for purposes of refraction later on)
			else {
				t_values.add(t0);
				t_values.add(t1);
				return new Intersection(r.pointOnRay(t0), this, t_values);
			}
		}
	}

	//The surface normal of a sphere is the vector from the center of the sphere to the point on the surface
	public NVector findNormal(double[] surface_point) {
		NVector normal = new NVector((surface_point[0] - x) / radius,
				(surface_point[1] - y) / radius, (surface_point[2] - z)
				/ radius);
		return normal;
	}
}
