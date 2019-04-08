package utility;

import java.io.PrintStream;

import heuristic.routing.cvrp.DemandRoutesSolution;

public class Utilities {
	

	public static void presentSolution(PrintStream output, DemandRoutesSolution routes,double time, double length) {
		//
		output.println(routes);
		output.println("Time: " + time+" ms");
		output.println("Length: " + length);
		output.println();
	}


}
