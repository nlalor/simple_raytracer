package RAYTRACER;

//Basic Vector class with standard operations supported
public class NVector {

    	public double x, y, z, a;
    	
    	public NVector(double _x, double _y, double _z) {
    	    x = _x;
    	    y = _y;
    	    z = _z;
    	    a = 1;
    	}
    	
    	public NVector(double _x, double _y, double _z, double _a) {
    	    x = _x;
    	    y = _y;
    	    z = _z;
    	    a = _a;
    	}
    	public NVector add(NVector v) {
    	    return new NVector(x + v.x, y + v.y, z + v.z);
    	}
 
    	public NVector sub(NVector v) {
    	    return new NVector(x - v.x, y - v.y, z - v.z);
    	}
    	
    	public NVector scale(double k) {
    	    return new NVector(k*x, k*y, k*z);
    	}
    	
    	public double dot(NVector other) {
    	    return (x * other.x) + (y * other.y) + (z * other.z);
    	}
    	
    	public void unit() {
    	    double mag = mag();
    	    x /= mag;
    	    y /= mag;
    	    z /= mag;
    	}
    	
    	public double mag() {
    	    return Math.sqrt(x*x + y*y + z*z);
    	}
    	
    	public double getX() {
    	    return x;
    	}
    	
        public double getY() {
            return y;
        }
        
        public double getZ() {
            return z;
        }
        
        public String toString() {
            return x + ", " + y + ", " + z;
        }
}
