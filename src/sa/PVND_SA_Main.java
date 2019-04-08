package sa;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Random;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

import common.StartSolution;
import heuristic.annealing.SimulatedAnnealing;
import heuristic.annealing.schedule.GeometricTempSchhedule;
import heuristic.annealing.schedule.ITempSchedule;
import heuristic.routing.cvrp.CVRPDescription;
import heuristic.routing.cvrp.DemandRoutesSolution;
import heuristic.routing.cvrp.neighborhoodGenerators.ChangeStationRouteNG;
import heuristic.routing.cvrp.neighborhoodGenerators.MergeRouteNG;
import heuristic.routing.cvrp.neighborhoodGenerators.SwapStationsNG;
import optimization.algorithm.IOptimizationAlgorithm;
import optimization.decoder.IDecoder;
import optimization.decoder.PassThroughDecoder;
import optimization.solution.neighborhood.CompositeNeighborhood;
import optimization.solution.neighborhood.RepeatNeighborhood;
import utilities.random.RNGProvider;
import utility.Utilities;

public class PVND_SA_Main {
	
	private static final boolean SAVE_RESULT = true;
	private static final int REPETITION_COUNT = 1;
	
	public static void main(String[] args) throws FileNotFoundException, IOException {
		for(int i=0; i<REPETITION_COUNT; i++) {
			for (String name : Files.list(Paths.get("data/")).map(p -> p.toFile().getName().replaceAll(".vrp", ""))
					.filter(n -> !n.endsWith(".opt")).collect(Collectors.toList())) {
				runInstance(name);
			}
		}
	}
	
	private static void runInstance(String name) throws IOException, FileNotFoundException {
		String inFileName = "data/"+name+".vrp";
		
		// Get problem description
		CVRPDescription description = CVRPDescription.getDescription(inFileName);

		
		//Start measuring time
		long startTime = System.nanoTime();
		
		DemandRoutesSolution routes = solve(description);
		routes.removeEmptyRoutes();
		
		//Stop measuring time
		long endTime = System.nanoTime();

		// Solution presentation
		double length = routes.getLength();
		double time = (endTime-startTime)*1e-6;

		//present results
		Utilities.presentSolution(System.out, routes, time, routes.length);

		//save result
		if(SAVE_RESULT) {
			String outFileName = "output/sa/"+name+".txt";
			File outputFile = new File(outFileName);
			if(!outputFile.exists()) {
				outputFile.getParentFile().mkdirs();
				outputFile.createNewFile();
			}
			String line = time+","+length+"\n";
			Files.write(outputFile.toPath(), line.getBytes(), StandardOpenOption.APPEND);
		}

	}

	private static DemandRoutesSolution solve(CVRPDescription description) {
		
		//Decoder
		IDecoder<DemandRoutesSolution, DemandRoutesSolution> decoder = new PassThroughDecoder<>();
		
		//Neighborhood
		Random random = RNGProvider.getRandom();		
		CompositeNeighborhood<DemandRoutesSolution> compositeNeighborhood = new CompositeNeighborhood<>(Arrays.asList(
				new SwapStationsNG(random, description)
				,new ChangeStationRouteNG(random, description)
				,new MergeRouteNG(description, random)
				));
		double repetitionChance = 0.05;
		RepeatNeighborhood<DemandRoutesSolution> neighborhood = new RepeatNeighborhood<>(repetitionChance ,
				compositeNeighborhood);
		
		//Start solution
		DemandRoutesSolution startSolution = StartSolution.getInitialSolution(description);

		
		//Function
		ToDoubleFunction<DemandRoutesSolution> function = drs -> drs.length;
		
		//Temperature schedule
		ITempSchedule tempSchedule = new GeometricTempSchhedule(10, 0.99, 1000, 1_000_000);
				
		//Optimization algorithm
		IOptimizationAlgorithm<DemandRoutesSolution> optimizationAlgorithm = 
				new SimulatedAnnealing<DemandRoutesSolution,DemandRoutesSolution>(decoder, neighborhood, startSolution, function, tempSchedule);
				
		
		return optimizationAlgorithm.run();
	}

	
}
