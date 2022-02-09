package com.stolbovoi.weka.cobid19;

import java.io.File;
import weka.core.Instances;
import weka.core.Utils;
import weka.core.converters.CSVLoader;

public class Main {

	public static void main(String[] args) throws Exception {

		System.out.println("Hello Weka!");
		System.out.println("Working Directory: " + System.getProperty("user.dir"));
		
		// Load 3 data set
		MyUtils u = new MyUtils();				
		CSVLoader loader = new CSVLoader();
		
		loader.setOptions(Utils.splitOptions("-D 1"));
		loader.setSource(new File("data/dpc-covid19-ita-andamento-nazionale.csv"));
		Instances dpc = loader.getDataSet();
		u.exploreAttribute(dpc);

		loader.setOptions(Utils.splitOptions("-D 3 -format yyyy-MM-dd"));
		loader.setSource(new File("data/country_vaccinations.csv"));
		Instances vac = loader.getDataSet();
		u.exploreAttribute(vac);

		loader.setOptions(Utils.splitOptions("-D 2 -format yyyy-MM-dd"));
		loader.setSource(new File("data/country_vaccinations_by_manufacturer.csv"));
		Instances mnf = loader.getDataSet();
		u.exploreAttribute(mnf);

		// Report Top Ten country by vaccine usage
		u.pivot(mnf);
		
		// Join instances vac + dpc
		// may be useful for future cross-dataset mining
		Instances join = u.join(vac, dpc);
		u.exploreAttribute(join);
		
		// Predict the spread of COVID-19 ahead of time
		u.predict(join);
		
		System.out.println("Bye-bye Weka!");
	}
}
