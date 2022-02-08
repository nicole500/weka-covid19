package com.stolbovoi.weka.cobid19;

import java.io.File;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import weka.classifiers.evaluation.NumericPrediction;
import weka.classifiers.functions.GaussianProcesses;
import weka.classifiers.timeseries.WekaForecaster;
import weka.core.Instances;
import weka.core.converters.CSVLoader;

public class Main {

	public static void main(String[] args) throws Exception {

		System.out.println("Hello Weka!");
		System.out.println("Working Directory: " + System.getProperty("user.dir"));
		
		/*
		 * Inspiration: 
		 * https://www.programcreek.com/java-api-examples/?api=weka.core.converters.CSVLoader
		 * https://github.com/shuchengc/weka-example/blob/master/Attributes.java
		 */
		
		/*
		 * 1. Load 3 data set
		 */

		List<String> path = Arrays.asList(
			"data/dpc-covid19-ita-andamento-nazionale.csv", // 1 data
		 	"data/country_vaccinations.csv", 				// 3 data
			"data/country_vaccinations_by_manufacturer.csv"	// 2 data
			);
		
		HashMap<String, Instances> instances = new HashMap<String, Instances>();				
		Attributes attr = new Attributes();				

		CSVLoader loader = new CSVLoader();

		for (String p : path) {
			// System.out.println(p);
			String n = Paths.get(p).getFileName().toString();
			
			if (n.contains("country_vaccinations_by_manufacturer.csv")) {
				String [] op = {"-D", "2","-format", "yyyy-MM-dd"};
				loader.setOptions(op);
			}

			if (n.contains("dpc-covid19-ita-andamento-nazionale.csv")) {
				String [] op = {"-D", "1"};
				loader.setOptions(op);
			}

			if (n.contains("country_vaccinations.csv")) {
				String [] op = {"-D", "3","-format", "yyyy-MM-dd"};
				loader.setOptions(op);
			}
			
			loader.setSource(new File(p));
			instances.put(n, loader.getDataSet());
			// System.out.println("The instance: " + instances.get(n));
			attr.explore(instances.get(n));		
		}
		
		
		TopTentCcountryByVacines top = new TopTentCcountryByVacines();		
		top.prepare(instances.get("country_vaccinations_by_manufacturer.csv"));
		

		JoinDataset jd = new JoinDataset();
		jd.join(
			instances.get("country_vaccinations.csv"),
			instances.get("dpc-covid19-ita-andamento-nazionale.csv")
			);
		
		
	}

}
