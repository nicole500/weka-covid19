package com.stolbovoi.weka.cobid19;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

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
			"data/dpc-covid19-ita-andamento-nazionale.csv", 
			"data/country_vaccinations.csv", 
			"data/country_vaccinations_by_manufacturer.csv"
			);
		
		HashMap<String, Instances> instances = new HashMap<String, Instances>();				
		Attributes attr = new Attributes();				
		CSVLoader loader = new CSVLoader();
		
		for (String p : path) {
			// System.out.println(p);
			String n = Paths.get(p).getFileName().toString();
			loader.setSource(new File(p));
			instances.put(n, loader.getDataSet());
			// System.out.println("The instance: " + instances.get(n));
			attr.explore(instances.get(n));		
		}
		
		
		TopTentCcountryByVacines top = new TopTentCcountryByVacines();		
		top.prepare(instances.get("country_vaccinations_by_manufacturer.csv"));
		
		
	}

}
