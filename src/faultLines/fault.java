package faultLines;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.image.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import javax.imageio.*;

/**
 * Created as part of a class on concurrency
 * Uses multiple threads to generate a random terrain using the fault-line method.
 * To test this, the main method takes width, height, thread count, and fault count
 * arguments and generates a terrain with these parameters.
 * 
 * 
 * @author Ben
 *
 */
public class fault {

	public static void main(String[] args) {
		int w = (args.length > 0) ? Math.max(5, Integer.parseInt(args[0])) : 512;				// Width. Default 512, min 5
		int h = (args.length > 1) ? Math.max(5, Integer.parseInt(args[1])) : 512;				// Height. Default 512, min 5
		int t = (args.length > 2) ? Math.max(1, Integer.parseInt(args[2])) : 1;					// Threads. Default 1, min 1
		int k = (args.length > 3) ? Math.max(1, Integer.parseInt(args[3])) : 1000;				// Faults. Default 1000, min 1
		boolean openImage = (args.length > 4) ? Boolean.parseBoolean(args[4]) : false;			// Whether to open image automatically. Default false

		RandomTerrain terrain = new RandomTerrain(w, h, t, k);		// Generate terrain
		
		terrain.render("terrain.png", openImage);		// Render terrain as image

    }
    
    
    /**
     * Represents a 3-d terrain with randomly-generated heights
     * 
     * @author Ben
     *
     */
    private static class RandomTerrain {
    	public final int xSize;		// Size of x, y dimensions
    	public final int ySize;
    	private final List<Point> aPoints = new ArrayList<Point>();			// List of all points
    	private final List<Point> aBoundaryList = new ArrayList<Point>();		// List of boundary points to simplify uniform random selection
    	private final int aFaults;			// # of faults to use
    	private int aFaultCounter;			// for tracking how many faults have been used so far
    	private List<Thread> aGenerators = new ArrayList<Thread>();		// Fault generation threads. Size = t
    	
    	/**
    	 * Creates a new terrain with the given dimensions
    	 * @param pXSize x dimension
    	 * @param pYSize y dimension
    	 * @param pFaults The number of faults to use
    	 * @param pThreads The number of threads to use
    	 */
    	public RandomTerrain(int pXSize, int pYSize, int pThreads, int pFaults) {
    		xSize = pXSize;
    		ySize = pYSize;
    		aFaults = pFaults;
    		
    		for (int x = 0; x < xSize; x++) {		// Populate with all integral points in given range
    			for (int y = 0; y < ySize; y++) {
    				Point point = new Point(x, y);
    				aPoints.add(point);
    				
    				if (x == 0 || x == xSize - 1 || y == 0 || y == ySize - 1) {		// Also add to boundary list if on a boundary
    					aBoundaryList.add(point);
    					
    				}
    			}
    		}
    		
    		for (int i = 0; i < pThreads; i++) {			// Create generators (threads)
    			Thread generator = new Generator();
    			aGenerators.add(generator);
    			
    		}	
    		
    		generate();		// Generate terrain
    		
    	}
    	
    	
    	/**
    	 * Randomizes the terrain using the fault line method
    	 * @param 
    	 */
    	public void generate() {
    		aFaultCounter = aFaults;	// reset counter
    		double timeStart = System.currentTimeMillis();
    		
    		for (Thread generator : aGenerators) {		// start fault generators / threads
    			generator.start();
    			
    		}
    		
    		for (Thread generator : aGenerators) {		// Wait for fault generators to finish before returning
    			try {
					generator.join();
					
				} catch (InterruptedException e) {
					e.printStackTrace();
					
				}
    		}
    		
    		double time = System.currentTimeMillis() - timeStart;	// Time taken
    		
    		System.out.println("Time elapsed: " + time + " ms");	// Print time
    	}
    	
    	
    	/**
    	 * Decides whether more faults are needed (fault counter > 0) and decrement counter. 
    	 * Thread-safe.
    	 * @return true if more faults are needed, false otherwise
    	 */
    	synchronized private boolean moreFaults() {
    		return aFaultCounter-- > 0;		// Decrement counter after check
    		
    	}
    	
    	
    	/**
    	 * Finds a random point on the terrain
    	 * Used for generating fault line endpoints.
    	 * @return the random point
    	 */
    	private Point randomPoint() {
    		Random rng = ThreadLocalRandom.current();
    		return aPoints.get(rng.nextInt(aPoints.size()));
    		
    	}
    	
    	
    	/**
    	 * Finds the maximum height on this terrain
    	 * @return The max height
    	 */
    	public int maxHeight() {
    		int max = -1;
    		
    		for (Point point : aPoints) {		// Compare heights to find max
    			int height = point.getHeight();
    			if (height > max) {
    				max = height;

    			}
    		}
    		
    		return max;
    	}
    	
    	
    	/**
    	 * Finds the minimum height on this terrain
    	 * @return The min height
    	 */
    	public int minHeight() {
    		int min = -1;
    		    		
    		for (Point point : aPoints) {		// Compare heights to find min
    			int height = point.getHeight();
    			if (height < min || min < 0) {
    				min = height;

    			}
    		}
    		
    		return min;
    	}
    	
    	
    	/**
    	 * Renders and exports the terrain as a .png image at the given path
    	 * @param pPath The path to save the image at
    	 */
    	public void render(String pPath, boolean pOpen) {
    		BufferedImage outputimage = new BufferedImage(xSize, ySize, BufferedImage.TYPE_INT_ARGB);
    		
    		double minHeight = minHeight();		// min height and range of heights
    		double range = maxHeight() - minHeight;		

    		for (Point point : aPoints) {
    			int shade = (int) (((point.getHeight() - minHeight) / range) * 205);		// Convert % of range into value to add to rgb 
    			int rgb = new Color(50 + shade, 50 + shade, 255).getRGB();				// Get rgb value of shaded colour (shades of blue)

    			outputimage.setRGB(point.x , point.y, rgb);		// Colour corresponding pixel in image

    		}    		
    		
    		
    		File outputfile = new File(pPath); // Write the image
    		try {
    			ImageIO.write(outputimage, "png", outputfile);
    			System.out.println("Image saved: " + outputfile.getCanonicalPath());
    			
    		    Toolkit toolkit = Toolkit.getDefaultToolkit();		// Notifies once image is generated
    		    toolkit.beep();
    		    
    		    if (pOpen) {
    		    	Desktop.getDesktop().open(outputfile);				// Automatically open image if setting enabled
    			
    		    }
    		} catch (Exception e) {
    			e.printStackTrace();
    			
    		}
    	}
    	
    	
        /**
         * Represents a point in the terrain. 
         * Thread-safe.
         * 
         * @author Ben
         *
         */
        public class Point {
        	public final int x;
        	public final int y;
        	private int aHeight = 0;
        	
        	
        	/**
        	 * Creates a new point with the given coordinates
        	 * @param pX x-coordinate
        	 * @param pY y-coordinate
        	 */
        	public Point(int pX, int pY) {
        		x = pX;
        		y = pY;
        		
        	}
        	
        	
        	/**
        	 * Raise height by specified amount
        	 * @param pInc The increment by which to increase the height
        	 */
        	synchronized private void raise(int pInc) {
        		aHeight += pInc;
        		
        	}
        	
        	
        	/**
        	 * Get current height
        	 * @return Height
        	 */
        	synchronized public int getHeight() {
        		return aHeight;
        		
        	}
        	
        	
        	@Override
        	public String toString() {					// For testing purposes
        		return Integer.toString(getHeight());
        		
        	}
        	
        }
    	
    	
        /**
         * Used to generate random terrain using fault lines
         * 
         * @author Ben
         *
         */
        private class Generator extends Thread {
        	/**
        	 * Decides whether the point lies left of the fault line using provided inequality
        	 * @param pEnd1 First end of the fault line
        	 * @param pEnd2 Second end of the fault line
        	 * @param pPoint The point to compare
        	 * @return true if the point is left of the line, false otherwise
        	 */
        	private boolean left(Point pEnd1, Point pEnd2, Point pPoint) {
        		return (pEnd2.x - pEnd1.x) * (pPoint.y - pEnd1.y) > (pPoint.x - pEnd1.x) * (pEnd2.y - pEnd1.y);
        		
        	}
        	
        	
        	/**
        	 * Find all points left of the given fault line and raises them
        	 * @param pEnd1 First end of the fault line
        	 * @param pEnd2	Second end of the fault line
        	 */
        	private void raiseFault(Point pEnd1, Point pEnd2) {
        		Random rng = ThreadLocalRandom.current();
        		int adj = rng.nextInt(10) + 1;		// Height adjustment. Don't waste time adjusting by 0!
       		
    			for (Point point : aPoints) {
    				if (left(pEnd1, pEnd2, point)) {		// Point is on left side of line so raise it
    					point.raise(adj);;
    					
    				}
    			}  		
        	}
        	
        	
        	/**
        	 * Raises the terrain along a random fault
        	 */
        	private void generateFault() {
        		Point end1 = randomPoint();
        		Point end2;
        		do {
        			end2 = randomPoint();
        			
        		} while (end1 == end2);
        		
        		raiseFault(end1, end2);
        		
        	}
        	
        	
        	@Override
        	public void run() {
        		while (moreFaults()) {		// Continue generating faults until k faults have been generated (across all threads)
        			generateFault();
        			
        		}
        	}
        	
        }
        
    }
    
}
