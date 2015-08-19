package RAYTRACER;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.media.opengl.*;
import javax.media.opengl.glu.*;
import javax.media.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.*;
import javax.swing.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import javax.swing.event.*;

public class RayTracer implements GLEventListener, ActionListener, ChangeListener {

	public static void main(String[] args) {
		new RayTracer();
	}

	//Initial size of window, init canvas and graphical units
	private int INITIAL_WIDTH = 600;
	private int INITIAL_HEIGHT = 600;
	private JButton quit_button; 
	private GLCanvas canvas;
	private GL2 gl;
	private GLU glu;
	//N = Rows, M = Cols of bitmap used to display final rendering
	private int M = 400; 
	private int N = 400;
	//Initial position of viewer in regards to the scene
	double viewer_position[] = { 0, 200, -200 };
	//The pixel grid which lies in the plane x = 800.
	//(grid_x, grid_y, grid_z) is the upper left corner
	double grid_x = 800;
	double grid_y = 0; 
	double grid_z = 0;
	//Width and Height of the pixel grid
	int grid_width = 400; 
	int grid_height = 400;
	//Lists used to track the shapes and light sources within the scene
	ArrayList<Shape> shapes;
	ArrayList<Light> lights;
	//Background color is displayed if the ray has no intersections
	NVector background_color;
	//Used to display rendering
	private byte buffer[];

	public RayTracer() {
		//Initialize arrays, bg color
		shapes = new ArrayList<Shape>();
		lights = new ArrayList<Light>();
		background_color = new NVector(25, 25, 25);
		//Add two spheres and a plane to the scene
		Sphere s = new Sphere(500, 200, -220, 15, 255, 100, 100, 0.4, 0.8, 0.1, 0.0);
		shapes.add(s);
		Sphere s2 = new Sphere(450, 170, -220, 30, 100, 100, 100, 0.4, 0.8, 0.1, 0.5);
		shapes.add(s2);
		NVector normal = new NVector(-0.2, 0, 1);
		Plane p = new Plane(700, 200, -200, normal, 50, 50, 250, 0.8, 0.8, 0.3, 0.0);
		shapes.add(p);
		//Add two light sources to the scene
		double[] light1_pos = { 150, 150, 0 };
		double[] light1_col = { 25, 25, 25 };
		Light l1 = new Light(light1_pos, light1_col);
		lights.add(l1);
		double[] light2_pos = { 200, 200, -200 };
		double[] light2_col = { 15, 15, 15 };
		Light l2 = new Light(light2_pos, light2_col);
		lights.add(l2);

		//Set up the canvas and UI
		GLProfile glp = GLProfile.getDefault();
		GLCapabilities caps = new GLCapabilities(glp);
		canvas = new GLCanvas(caps);
		canvas.addGLEventListener(this);

		JFrame frame = new JFrame("RAYTRACER");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(INITIAL_WIDTH, INITIAL_HEIGHT);
		frame.setLayout(new BorderLayout());
		frame.setVisible(true);

		JPanel north = new JPanel(new FlowLayout());

		quit_button = new JButton("Quit");
		quit_button.addActionListener(this);
		north.add(quit_button);

		JPanel center = new JPanel(new GridLayout(1, 1));
		center.add(canvas);

		frame.add(north, BorderLayout.NORTH);
		frame.add(center, BorderLayout.CENTER);

		FPSAnimator animator = new FPSAnimator(canvas, 60);
		animator.start();
	}

	///RAY TRACER METHODS///

	//Params: int i, int j represent the pixel at (i,j)
	//Returns: NRay going from the viewer through pixel (i, j)
	public NRay makeRay(int i, int j) {
		double direction[] = new double[3];
		double pixel[] = new double[3];
		pixel[0] = grid_x;
		pixel[1] = grid_y + (grid_width * j) / M;
		pixel[2] = grid_z - (grid_height * i) / N;
		for (int k = 0; k < 3; k++) {
			direction[k] = pixel[k] - viewer_position[k];
		}
		return new NRay(direction, viewer_position);
	}

	//raytrace intersects a given ray with all of the shapes in the scene
	//Params: NRay, ArrayList<Shape>, int level, double weight (level and weight are used in calculations for the specular lighting)
	//Returns: NVector representing the color (Red, Green, Blue) of the pixel that NRay R passes through
	NVector raytrace(NRay r, ArrayList<Shape> shapes, int level, double weight) {
		//Get the intersection between the NRay R and the shapes in the scene
		Intersection i = intersect(r, shapes);
		//If no intersection:
		if (i == null)
			//When level == 0, we are not calculating specular so return the background color
			if (level == 0)
				return background_color;
		//If we are calculating specular light on an object, return black if the reflected ray has no intersection
			else
				return new NVector(0, 0, 0);
		//If there is an intersection with an object:
		else {
			//Create an array containing all the shapes in the scene excluding the shape which NRay r intersected
			//We do this so that we can intersect shadow and reflection rays without worrying about intersecting with ourself
			Object clone = shapes.clone();
			ArrayList<Shape> other_shapes = (ArrayList<Shape>) clone;
			other_shapes.remove(i.shape);
			//Get the point of intersection and use that to calculate surface normal
			double[] intersection_point = i.intersection_point;
			NVector surface_normal = i.shape.findNormal(intersection_point);

			//Start with the specified surface color relative to the shape
			double[] surface_color = { i.shape.red, i.shape.green, i.shape.blue };

			//Amount of ambient light the surface reflects
			double ka = i.shape.ambient_level;
			//Amount of diffused light the surface reflects
			double kd = i.shape.diffuse_level;
			//Specularity of surface (reflection)
			double ks = i.shape.specular_level;

			//AMBIENT CALCULATION
			//Note: ambient light is always applied
			double[] pixel_color = new double[3];
			for (int n = 0; n < 3; n++) {
				pixel_color[n] = ka * surface_color[n];
			}

			//Step through each of the light sources
			for (int n = 0; n < lights.size(); n++) {
				Light l = lights.get(n);

				double[] light_direction = new double[3];
				for (int a = 0; a < 3; a++) {
					light_direction[a] = l.light_position[a] - intersection_point[a];
				}

				//Create a ray starting from the point of intersection and going towards the light source
				NRay shadow_ray = new NRay(light_direction, intersection_point);
				//If there is no intersection between shape and light source, then we are not in shadow so apply diffuse calculation
				if (!shadowIntersection(shadow_ray, other_shapes)) {
					NVector light_vector = new NVector(light_direction[0],light_direction[1], light_direction[2]);
					light_vector.unit();
					//DIFFUSE CALCULATION
					double diffuse_factor = Math.max(0.0, light_vector.dot(surface_normal));
					for (int a = 0; a < 3; a++) {
						pixel_color[a] += (kd * diffuse_factor * surface_color[a])
							+ (kd * diffuse_factor * l.light_color[a]);
					}
				}
			}

			//SPECULAR CALCULATION
			//Get the direction of the reflection ray using the incident ray's direction and the surface normal
			NVector incident = new NVector(r.direction[0], r.direction[1], r.direction[2]);
			double normal_scale = ((surface_normal.dot(incident))*2)/(surface_normal.mag()*surface_normal.mag());
			NVector reflect = surface_normal.scale(normal_scale);
			reflect = incident.sub(reflect);
			double[] reflect_array = {reflect.x, reflect.y, reflect.z};
			//Create a new ray going in the reflection direction, starting from the point of intersection
			NRay reflection_ray = new NRay(reflect_array, intersection_point);
			//Level is the max amount of times we will recursively measure reflections
			//If the weight of the reflected light (the degree to which the light affects the surface color) is too small, then we don't care about it anymore
			//Otherwise add the specular ray's color to the overall color
			if(level < 10 && weight*ks > 0.1) {
				NVector specular_color = raytrace(reflection_ray, other_shapes, level + 1, weight*ks);
				pixel_color[0] += ks*specular_color.x;
				pixel_color[1] += ks*specular_color.y;
				pixel_color[2] += ks*specular_color.z;
			}

			//After all calculations, return the final color for this pixel
			return new NVector(pixel_color[0], pixel_color[1], pixel_color[2]);
		}
	}

	//Params: NRay, ArrayList<Shape>
	//Returns: Intersection object containing information about the ray's intersection with scene
	Intersection intersect(NRay r, ArrayList<Shape> objects) {
		//Create an array to store all intersections of NRay r (r could intersect multiple objects)
		ArrayList<Intersection> intersections = new ArrayList<Intersection>();
		//Step through shapes and call each shapes intersection methods with NRay r
		for (int i = 0; i < objects.size(); i++) {
			Shape shape = objects.get(i);
			intersections.add(shape.intersect(r));
		}
		//Need to find the first intersection
		double closest_t = Double.MAX_VALUE;
		Intersection closest_intersection = null;
		for (int n = 0; n < intersections.size(); n++) {
			Intersection i = intersections.get(n);
			if (i != null) {
				if (i.t_values != null) {
					if (i.t_values.get(0) < closest_t) {
						closest_t = i.t_values.get(0);
						closest_intersection = i;
					}
				}
			}
		}
		return closest_intersection;
	}

	//Params: NRay, ArrayList<Shape>
	//Returns: Boolean value, true if there is an intersection, false otherwise
	private boolean shadowIntersection(NRay r, ArrayList<Shape> objects) {
		for (int n = 0; n < objects.size(); n++) {
			Shape s = objects.get(n);
			Intersection shadow_intersection = s.intersect(r);
			if (shadow_intersection != null) {
				return true;
			}

		}
		return false;
	}

	public void makePicture() {
		gl.glClearColor(1, 1, 0, 1);
		gl.glPixelStorei(GL2.GL_PACK_ALIGNMENT, 1);
		gl.glPixelStorei(GL2.GL_UNPACK_ALIGNMENT, 1);
		gl.glPixelStorei(GL2.GL_UNPACK_SKIP_PIXELS, 0);
		gl.glPixelStorei(GL2.GL_UNPACK_SKIP_ROWS, 0);

		// The buffer has N rows and M columns
		// Have to copy pixels from row i in the image to row N-i-1 of the buffer since buffer is installed upside down
		buffer = new byte[N * M * 3];

		for (int i = 0; i < N; i++) {
			for (int j = 0; j < M; j++) {
				NRay r = makeRay(i, j);
				NVector color = raytrace(r, shapes, 0, 1);
				byte[] s = convertToByte(color);
				copy3(buffer, N - i - 1, j, s);
			}
		}
	}

	//Params: NVector color
	//Returns: byte[] representation of the specified color
	private byte[] convertToByte(NVector c) {
		byte[] result = new byte[3];
		//Color values are on a 0 - 255 scale, we want them to be on a 0 - 127 scale
		int red = (int) c.x / 2;
		int green = (int) c.y / 2;
		int blue = (int) c.z / 2;
		//Check the upper bounds of color values
		byte r = (red > 127) ? (byte) 127 : (byte) (red & 127);
		byte g = (green > 127) ? (byte) 127 : (byte) (green & 127);
		byte b = (blue > 127) ? (byte) 127 : (byte) (blue & 127);

		result[0] = r;
		result[1] = g;
		result[2] = b;
		//return byte[] of colors
		return result;
	}

	private void copy3(byte dest[], int row, int col, byte source[]) {
		int index = row * M * 3 + col * 3;
		dest[index] = source[0];
		dest[index + 1] = source[1];
		dest[index + 2] = source[2];
	}

	/// OpenGL Methods ///

	public void actionPerformed(ActionEvent event) {
		if (event.getSource() == quit_button)
			System.exit(0);
	}

	public void stateChanged(ChangeEvent e) {
	}

	public void display(GLAutoDrawable drawable) {
		update();
		render();
	}

	private void update() {
	}

	private void render() {
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT);
		gl.glRasterPos2i((INITIAL_WIDTH - M) / 2, (INITIAL_HEIGHT - N) / 2);
		gl.glDrawPixels(M, N, GL2.GL_RGB, GL2.GL_BYTE, ByteBuffer.wrap(buffer));
	}

	public void dispose(GLAutoDrawable drawable) {
	}

	public void init(GLAutoDrawable drawable) {
		gl = drawable.getGL().getGL2();
		glu = new GLU();
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		glu.gluOrtho2D(0f, INITIAL_WIDTH, 0f, INITIAL_HEIGHT);

		makePicture();
	}

	public void reshape(GLAutoDrawable drawable, int x, int y, int width,
			int height) {
		// this is called when the window is resized
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		glu.gluOrtho2D(0f, width, 0f, height);
	}
}
