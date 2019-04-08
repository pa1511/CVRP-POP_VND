package sa;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import utilities.PStrings;

public class ResultStats {
	
	private static enum ResultType{
		Time, Distance;
	}
	
	private static final ResultType RESULT_TYPE = ResultType.Distance; 
	private static final int devCount = 3;
		
	public static void main(String[] args) throws IOException {
		//used to order instance files
		Comparator<File> fileOrderComparator = (f1,f2)->{
			String[] data1 = f1.getName().split("-");
			String[] data2 = f2.getName().split("-");
			
			int n1 = Integer.parseInt(data1[1].replaceAll("n", "").trim());
			int n2 = Integer.parseInt(data2[1].replaceAll("n", "").trim());
			
			int k1 = Integer.parseInt(data1[2].split("\\.")[0].replaceAll(".vrp", "").replaceAll(".opt", "").replaceAll("k", "").trim());
			int k2 = Integer.parseInt(data2[2].split("\\.")[0].replaceAll(".vrp", "").replaceAll(".opt", "").replaceAll("k", "").trim());
			
			int res = Integer.compare(n1, n2);
			
			return res==0 ? Integer.compare(k1, k2) : res;
		};
		
		//Loading instance files
		List<File> instanceFiles = Files.list(Paths.get("data/")).map(Path::toFile).sorted(fileOrderComparator).collect(Collectors.toList());
		
		//For each instance
		for (File file : instanceFiles) {
			
			String instanceId = file.getName();
			if(instanceId.endsWith(".opt"))
				continue;

			String outFileName = "output/sa/"+instanceId.replace(".vrp", ".txt");

			//Loading all result files			
			List<Double> times = new ArrayList<>();
			List<Double> distances = new ArrayList<>();
			
			for (String line : Files.lines(Paths.get(outFileName)).collect(Collectors.toList())) {
				if(line.isEmpty())
					continue;
				
				String[] split = line.split(",");
				double time = Double.parseDouble(split[0]);
				double distance = Double.parseDouble(split[1]);
				
				times.add(new Double(time));
				distances.add(new Double(distance));
			}
			
			if(RESULT_TYPE==ResultType.Time) {
				//Time analysis
				double averageTime = times.stream().mapToDouble(Double::doubleValue).average().getAsDouble();
				double standardDeviationTime = getStandardDeviation(times, averageTime);
				
				double avgT = times.stream().mapToDouble(Double::doubleValue).filter(v->Math.abs(averageTime-v)<=devCount*standardDeviationTime).average().getAsDouble();
				System.out.print(PStrings.getRounded(4, avgT)+"\t");
			}
			else {
				//Distance analysis
				double averageDistance = distances.stream().mapToDouble(Double::doubleValue).average().getAsDouble();
				double standardDeviationDistance = getStandardDeviation(distances, averageDistance);
				
				double avgD = distances.stream().mapToDouble(Double::doubleValue).filter(v->Math.abs(averageDistance-v)<=devCount*standardDeviationDistance).average().getAsDouble();
				System.out.print(PStrings.getRounded(4, avgD)+"\t");
			}
		}
	}

	private static double getStandardDeviation(List<Double> values, double mean) {
		double standardDeviation = 0;
		for(Double time:values) {
			standardDeviation += Math.pow(time.doubleValue()-mean, 2);
		}
		standardDeviation/=values.size();
		standardDeviation = Math.sqrt(standardDeviation);
		return standardDeviation;
	}
}
