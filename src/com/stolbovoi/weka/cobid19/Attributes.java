package com.stolbovoi.weka.cobid19;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

import weka.core.AttributeStats;
import weka.core.Instances;
import weka.core.converters.CSVLoader;
import weka.experiment.Stats;


public class Attributes {
	
	
	public void explore(Instances data ) throws FileNotFoundException {

		
		System.out.println("\n\n**********************************");
		System.out.println("Data set name: " + data.relationName());
		System.out.println("Data set size: " + data.numInstances());		

		PrintStream stdout = System.out;
		String foutName = "res/" + data.relationName() + ".attribute.md"; 
		PrintStream fileOut = new PrintStream(foutName);

		System.setOut(fileOut);

		System.out.println("Data set attributes:\n");
		System.out.println("|" + "name" 
				+ "|" + "type"
				+ "|" + "num_values"
				+ "|" + "distinct_count"
				+ "|" + "total_count"
				+ "|" + "missing_count"
				+ "|" + "int_count"
				+ "|" + "min"
				+ "|" + "max"
				+ "\n|-|-|-|-|-|-|-|-|-"
				);
	
		for(int i = 0; i < data.numAttributes(); i++ ) {
			String name = data.attribute(i).name();
			String type = null;
	
			if (data.attribute(i).isDate())
				type = "date";
			else if (data.attribute(i).isNominal())
				type = "nominal";
			else if (data.attribute(i).isNumeric())
				type = "numeric";
			else if (data.attribute(i).isString())
				type = "string";
			else
				type = "***unknown";
			
			int num_values = data.attribute(i).numValues();
			AttributeStats as = data.attributeStats(i);
			int distinct_count = as.distinctCount;
			int total_count = as.totalCount;
			int missing_count = as.missingCount;
			int int_count = as.intCount;
			double min = 0, max = 0;
			if (data.attribute(i).isNumeric()) {
				Stats s = as.numericStats;
				min = s.min;
				max = s.max;
			}
	
			System.out.println("|" + name 
					+ "|" + type
					+ "|" + num_values
					+ "|" + distinct_count
					+ "|" + total_count
					+ "|" + missing_count
					+ "|" + int_count
					+ "|" + min
					+ "|" + max
					);
		}

		System.setOut(stdout);
		System.out.println("See " + foutName);

	}
	
	public static void main(String[] args) throws Exception{
		
		String path = "data/dpc-covid19-ita-andamento-nazionale.csv";
		System.out.println("Read dataset: " + path);
		CSVLoader loader = new CSVLoader();
		loader.setSource(new File(path));
		Instances data = loader.getDataSet();

		Attributes a = new Attributes();
		
		a.explore(data);
	}
}