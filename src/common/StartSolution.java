package common;

import heuristic.routing.ImplicitLoopDemandRoute;
import heuristic.routing.cvrp.CVRPDescription;
import heuristic.routing.cvrp.DemandRoutesSolution;

public class StartSolution {

	public static DemandRoutesSolution getInitialSolution(CVRPDescription description) {
		DemandRoutesSolution solution = new DemandRoutesSolution();
		
		for(int i=1; i<description.dimension;i++) {
			ImplicitLoopDemandRoute route = new ImplicitLoopDemandRoute(description.distance, description.demand);
			route.add(i);
			solution.add(route);
		}
		
		
		return solution;
	}

}
