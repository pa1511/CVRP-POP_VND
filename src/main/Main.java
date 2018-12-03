package main;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import function.ID1Function;
import heuristic.VariableNeighborhoodDescent;
import heuristic.popvnd.ImmuneSystemVariableNeighborhoodDescent_V2;
import heuristic.popvnd.finalSolutionSelection.BestSolutionSelection;
import heuristic.popvnd.neighborAcceptanceTest.BetterThanBestInPopulation;
import heuristic.popvnd.neighborAcceptanceTest.NeighborAcceptanceTest;
import heuristic.popvnd.neighborhood.ChangeStationRoute_V2;
import heuristic.popvnd.neighborhood.INeighborhood_V2;
import heuristic.popvnd.neighborhood.MergeRoutes_V2;
import heuristic.popvnd.neighborhood.SwapStations_V2;
import heuristic.popvnd.neighborhoodMaintenance.BestMMaintenance;
import heuristic.popvnd.neighborhoodMaintenance.FirstMMaintenance;
import heuristic.popvnd.neighborhoodMaintenance.NeighborhoodMaintenance;
import heuristic.popvnd.neighborhoodMaintenance.RandomMMaintenance;
import heuristic.popvnd.populationSelection.BestMSelection;
import heuristic.popvnd.populationSelection.FirstMSelection;
import heuristic.popvnd.populationSelection.IPopulationSelection_V2;
import heuristic.popvnd.populationSelection.RandomMSelection;
import heuristic.routing.ImplicitLoopDemandRoute;
import heuristic.routing.cvrp.CVRPDescription;
import heuristic.routing.cvrp.DemandRoutesSolution;
import heuristic.routing.cvrp.neighborhood.ChangeStationRoute;
import heuristic.routing.cvrp.neighborhood.MergeRoutes;
import heuristic.routing.cvrp.neighborhood.SwapStations;
import optimization.decoder.IDecoder;
import optimization.decoder.PassThroughDecoder;
import optimization.fittnesEvaluator.FunctionValueFitnessEvaluator;
import optimization.fittnesEvaluator.IFitnessEvaluator;
import optimization.fittnesEvaluator.NegateFitnessEvaluator;
import optimization.solution.neighborhood.INeighborhood;
import optimization.solution.neighborhood.selection.INeighborSelection;
import optimization.solution.neighborhood.selection.SelectBestImprovingNeighbor;
import optimization.solution.neighborhood.selection.SelectFirstImprovingNeighbor;
import optimization.solution.neighborhood.selection.SelectRandomImprovingNeighbor;
import utilities.executor.ExecutorServiceProvider;

public class Main {

	private static final Algorithm ALGORITHM = Algorithm.POP_VND;
	private static final int THREAD_COUNT = 1;
	private static final boolean[] SHUFFLE_NEIGHBORHOODS_OPTIONS = new boolean[] {false,true};
	private static final Type[] TYPE_OPTIONS = Type.values();
	private static final int MIN_POPULATION_SIZE = 2;
	private static final int MAX_POPULATION_SIZE = 3; 
	private static final int REPETITION_COUNT = 5;
	private static final boolean SAVE_RESULT = true;
	//
	private static int POPULATION_SIZE;
	private static boolean SHUFFLE_NEIGHBORHOODS;
	private static Type TYPE;
	//
	private static ExecutorService executorService;
	
	
	public static void main(String[] args) throws IOException {
		
		PrintStream output = System.out;
		executorService = ExecutorServiceProvider.getNewExecutorService(THREAD_COUNT);

		try {
			for(boolean shuffle:SHUFFLE_NEIGHBORHOODS_OPTIONS) {
				SHUFFLE_NEIGHBORHOODS = shuffle;
				for(Type type:TYPE_OPTIONS) {
					TYPE = type;
					for(int i=0; i<REPETITION_COUNT; i++) {
						if(ALGORITHM.equals(Algorithm.POP_VND)) {
							for(int pop=MIN_POPULATION_SIZE; pop<=MAX_POPULATION_SIZE;pop++) {
								POPULATION_SIZE = pop;
									runAllInstances(output);
							}
						}
						else {
							runAllInstances(output);
						}
					}
				}
			}
		}
		finally {
			executorService.shutdown();
		}
		
	}

	private static void runAllInstances(PrintStream output) throws IOException, FileNotFoundException {
		for (String name : Files.list(Paths.get("data/")).map(p -> p.toFile().getName().replaceAll(".vrp", ""))
				.filter(n -> !n.endsWith(".opt")).collect(Collectors.toList())) {
			runInstance(output, name);
		}
	}

	private static void runInstance(PrintStream output, String name) throws IOException, FileNotFoundException {
		String inFileName = "data/"+name+".vrp";
		output.println(inFileName);
		
		// Get problem description
		CVRPDescription description = CVRPDescription.getDescription(inFileName);

		//Start measuring time
		long startTime = System.nanoTime();
		
		DemandRoutesSolution routes = getInitialSolution(description);
		switch (ALGORITHM) {
			case POP_VND:
				routes = optimizeISVND(routes,description);
				break;
				
			case VND:
				routes = optimizeVND(routes,description);
				break;
				
			default:
				break;
		}
		
		//Stop measuring time
		long endTime = System.nanoTime();

		// Solution presentation
		double length = routes.getLength();
		double time = (endTime-startTime)*1e-6;

		presentSolution(output, routes, time, routes.length);
		
		//Store solution file
		if(SAVE_RESULT) {
			String outFileName = "output/"+ALGORITHM.toString().toLowerCase()+"/";
			if(ALGORITHM.equals(Algorithm.POP_VND)) {
				outFileName+=Integer.toString(POPULATION_SIZE)+"/";
			}
			outFileName+="shuff_"+SHUFFLE_NEIGHBORHOODS+"/";
			outFileName+="type_"+TYPE+"/";
			if(ALGORITHM.equals(Algorithm.POP_VND)) {
				outFileName+="thread_"+THREAD_COUNT+"/";
			}
			outFileName+=name+".txt";
			File outputFile = new File(outFileName);
			if(!outputFile.exists()) {
				outputFile.getParentFile().mkdirs();
				outputFile.createNewFile();
			}
			String line = time+","+length+"\n";
			Files.write(outputFile.toPath(), line.getBytes(), StandardOpenOption.APPEND);
		}
	}

	private static DemandRoutesSolution getInitialSolution(CVRPDescription description) {
		DemandRoutesSolution solution = new DemandRoutesSolution();
		
		for(int i=1; i<description.dimension;i++) {
			ImplicitLoopDemandRoute route = new ImplicitLoopDemandRoute(description.distance, description.demand);
			route.add(i);
			solution.add(route);
		}
		
		
		return solution;
	}


	private static DemandRoutesSolution optimizeVND(DemandRoutesSolution routes, CVRPDescription description) {
		DemandRoutesSolution startSolution = routes;
		
		// Decoded
		IDecoder<DemandRoutesSolution, DemandRoutesSolution> decoder = new PassThroughDecoder<>();
		
		// Evaluator
		IFitnessEvaluator<DemandRoutesSolution> evaluator = new NegateFitnessEvaluator<>(new FunctionValueFitnessEvaluator<>());
		
		//Function
		ID1Function<DemandRoutesSolution> function = (s)->s.length;

		//Neighborhood Selection
		INeighborSelection<DemandRoutesSolution> neighborhoodSelection = null;
		switch (TYPE) {
			case BEST:
				neighborhoodSelection = new SelectBestImprovingNeighbor<>();
				break;
			case FIRST:
				neighborhoodSelection = new SelectFirstImprovingNeighbor<>();
				break;
			case RANDOM:
				neighborhoodSelection = new SelectRandomImprovingNeighbor<>();
				break;
			default:
				break;
		}
				
		//Neighborhoods
		Function<DemandRoutesSolution, Predicate<DemandRoutesSolution>> acceptanceTest = s -> n -> n.length<s.length;
		
		INeighborhood<DemandRoutesSolution> neighborhood1 = new MergeRoutes(description,acceptanceTest);
		
		INeighborhood<DemandRoutesSolution> neighborhood2 = new ChangeStationRoute(description,acceptanceTest);

		INeighborhood<DemandRoutesSolution> neighborhood3 = new SwapStations(description,acceptanceTest);

		List<INeighborhood<DemandRoutesSolution>> neghborhoods = Arrays.asList(
				neighborhood1, neighborhood2,neighborhood3 
		);		
		
		//Optimization algorithm
		VariableNeighborhoodDescent<DemandRoutesSolution, DemandRoutesSolution> optimization = new VariableNeighborhoodDescent<>(
				startSolution, decoder, neghborhoods, SHUFFLE_NEIGHBORHOODS, evaluator, neighborhoodSelection, function);

		DemandRoutesSolution solution = optimization.run();
		
		//remove empty routes
		solution.removeEmptyRoutes();
		
		return solution;
	}
	
	//==============================================================================================================================

	private static DemandRoutesSolution optimizeISVND(DemandRoutesSolution routes, CVRPDescription description) {
		
		//Comparator
		Comparator<DemandRoutesSolution> comparator = (r1,r2)-> Double.compare(r1.length, r2.length);

		//Neighborhood Selection
		IPopulationSelection_V2<DemandRoutesSolution> populationSelection = null;
		switch (TYPE) {

			case BEST:
				populationSelection = new BestMSelection<>(comparator);
				break;
			case FIRST:
				populationSelection = new FirstMSelection<>();
				break;
			case RANDOM:
				populationSelection = new RandomMSelection<>();
				break;
			default:
				break;
		}
		
		NeighborAcceptanceTest<DemandRoutesSolution> neighborAcceptanceTest = new BetterThanBestInPopulation<DemandRoutesSolution>(comparator);
			
		NeighborhoodMaintenance<DemandRoutesSolution> neighborhoodMaintenance = null;
		switch (TYPE) {
			case BEST:
				neighborhoodMaintenance = new BestMMaintenance<>(POPULATION_SIZE, comparator);
				break;
			case FIRST:
				neighborhoodMaintenance = new FirstMMaintenance<>(POPULATION_SIZE);
				break;
			case RANDOM:
				neighborhoodMaintenance = new RandomMMaintenance<>(POPULATION_SIZE);
				break;
			default:
				break;
		}
		
		//Neighborhoods
		INeighborhood_V2<DemandRoutesSolution> neighborhood1 = new MergeRoutes_V2(description, neighborAcceptanceTest, neighborhoodMaintenance);
		
		INeighborhood_V2<DemandRoutesSolution> neighborhood2 = new ChangeStationRoute_V2(description, neighborAcceptanceTest, neighborhoodMaintenance);

		INeighborhood_V2<DemandRoutesSolution> neighborhood3 = new SwapStations_V2(description, neighborAcceptanceTest, neighborhoodMaintenance);

		List<INeighborhood_V2<DemandRoutesSolution>> neghborhoods = Arrays.asList(
				neighborhood1,neighborhood2,neighborhood3
		);		
		
		ImmuneSystemVariableNeighborhoodDescent_V2<DemandRoutesSolution, DemandRoutesSolution> optimization = 
				new ImmuneSystemVariableNeighborhoodDescent_V2<>(POPULATION_SIZE, SHUFFLE_NEIGHBORHOODS, 
						()->routes, neghborhoods, populationSelection, new BestSolutionSelection<>(comparator), 
						executorService);

		DemandRoutesSolution solution = optimization.run();
		
		//remove empty routes
		solution.removeEmptyRoutes();
		
		return solution;
	}
	
	//==============================================================================================================================

	private static void presentSolution(PrintStream output, DemandRoutesSolution routes,double time, double length) {
		//
		output.println(routes);
		output.println("Time: " + time+" ms");
		output.println("Length: " + length);
		output.println();
	}

}
