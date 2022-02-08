package com.stolbovoi.weka.cobid19;

import java.io.File;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import weka.classifiers.Classifier;
import weka.classifiers.evaluation.NumericPrediction;
import weka.classifiers.functions.GaussianProcesses;
import weka.classifiers.functions.LinearRegression;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.classifiers.functions.SMOreg;
import weka.classifiers.timeseries.WekaForecaster;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.AddExpression;
import weka.filters.unsupervised.attribute.Copy;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.attribute.ReplaceMissingWithUserConstant;
import weka.filters.unsupervised.attribute.TimeSeriesDelta;

public class CleanDataset {

	public void clean(Instances dataset) throws Exception {

		// https://waikato.github.io/weka-wiki/formats_and_processing/remove_attributes/
		/*
		    1 country
			2 iso_code
			3 date
			4 total_vaccinations
			5 people_vaccinated
			6 people_fully_vaccinated
			7 daily_vaccinations_raw
			8 daily_vaccinations
			9 total_vaccinations_per_hundred
			10 people_vaccinated_per_hundred
			11 people_fully_vaccinated_per_hundred
			12 daily_vaccinations_per_million
			13 vaccines
			14 source_name
			15 source_website
			
			16 data
			17 stato
			18 ricoverati_con_sintomi
			19 terapia_intensiva
			20 totale_ospedalizzati
			21 isolamento_domiciliare
			22 totale_positivi
			23 variazione_totale_positivi
			24 nuovi_positivi
			25 dimessi_guariti
			26 deceduti
			27 casi_da_sospetto_diagnostico
			28 casi_da_screening
			29 totale_casi
			30 tamponi
			31 casi_testati
			32 note
			33 ingressi_terapia_intensiva
			34 note_test
			35 note_casi
			36 totale_positivi_test_molecolare
			37 totale_positivi_test_antigenico_rapido
			38 tamponi_test_molecolare
			39 tamponi_test_antigenico_rapido
		 */
		

		// Remove unused attributes
		Remove removeFilter = new Remove();
		removeFilter.setAttributeIndices("1-2,13-17,27-28,32,34-35");
		removeFilter.setInputFormat(dataset);
		dataset = Filter.useFilter(dataset, removeFilter);		

		System.out.println("****** step 1");

		// Replace Missing Data
		ReplaceMissingWithUserConstant missingFilter = new ReplaceMissingWithUserConstant();
		missingFilter.setAttributes("2-27");
		missingFilter.setInputFormat(dataset);
		dataset = Filter.useFilter(dataset, missingFilter);

		System.out.println("****** step 2");

		// Create Copy Tamponi attribute
		Copy copyFilter = new Copy();
		copyFilter.setAttributeIndices("21");
		copyFilter.setInputFormat(dataset);
		dataset = Filter.useFilter(dataset, copyFilter);		
		
		System.out.println("****** step 3");

		// Calculate delta Tamponi Delta
		TimeSeriesDelta deltaFilter = new TimeSeriesDelta();
		deltaFilter.setAttributeIndices("28");
		deltaFilter.setFillWithMissing(false);
		deltaFilter.setInputFormat(dataset);
		dataset = Filter.useFilter(dataset, deltaFilter);				
				
		System.out.println("****** step 4");

		// Create new attribute as tasso_positivita = 100 * nuovi_positivi / Tamponi delta
		AddExpression exprFilter = new AddExpression();
		exprFilter.setExpression("a17*100/a28");
		exprFilter.setName("tasso_positivita");
		exprFilter.setInputFormat(dataset);
		dataset = Filter.useFilter(dataset, exprFilter);				

		System.out.println("****** step 5");

		// Remove unused attributes
		removeFilter = new Remove();
		removeFilter.setAttributeIndices("2-28");
		removeFilter.setInputFormat(dataset);
		dataset = Filter.useFilter(dataset, removeFilter);		

		// Save instance
		ArffSaver saver = new ArffSaver();
		saver.setInstances(dataset);
		saver.setFile(new File("./res/join.arff"));
		saver.writeBatch();
		 
		/********************************************************/
		// https://forums.pentaho.com/threads/220977-TimeSeries-Forecasting-Weka-Java-API/
		// https://tech.forums.softwareag.com/t/timeseries-forecasting-weka-java-api/208229
						
		WekaForecaster forecaster = new WekaForecaster();
	    forecaster.setFieldsToForecast("tasso_positivita");
		// forecaster.setBaseForecaster(new GaussianProcesses());
	    

	    ArrayList<Classifier> classifiers = new ArrayList<>();
	    classifiers.add(new GaussianProcesses());	    
	    classifiers.add(new MultilayerPerceptron());	    
	    classifiers.add(new LinearRegression());	    
	    classifiers.add(new SMOreg());	    
	    
	    
	   //  ArrayList<ArrayList<Double>> predict = new ArrayList<ArrayList<Double>>();
	    // ArrayList<Object> predict = new ArrayList<Object>();
	   //  ArrayList<String> predictNames = new ArrayList<String>();
	    // ArrayList<String> predictDate = new ArrayList<String>();
	    
	    
	    ArrayList<Predict> predict = new ArrayList<Predict>();
	  	    
	    for (Classifier function: classifiers) {
	    	Predict p = new Predict();
	    	
	    	long start = System.currentTimeMillis();
	    	
		    ArrayList<Double> predictDouble = new ArrayList<Double>();	    	
		    ArrayList<String> predictDate   = new ArrayList<String>();	    	
	    	
	    	// Classifier f = new MultilayerPerceptron();
			forecaster.setBaseForecaster(function);
			// forecaster.setBaseForecaster(new MultilayerPerceptron());
			// forecaster.setBaseForecaster(new LinearRegression());
			// forecaster.setBaseForecaster(new SMOreg());
			forecaster.getTSLagMaker().setTimeStampField("date");
			forecaster.getTSLagMaker().setMinLag(7);
			forecaster.getTSLagMaker().setMaxLag(7);
			forecaster.getTSLagMaker().setAdjustForTrends(true);
			forecaster.getTSLagMaker().setAddMonthOfYear(true);
			forecaster.getTSLagMaker().setAddQuarterOfYear(true);
	
			PrintStream stream = null;
			List<List<NumericPrediction>> forecast = null;
			stream = new PrintStream("./res/forecast.csv");
	
			forecaster.buildForecaster(dataset, stream);		
			forecaster.primeForecaster(dataset);		
			forecaster.setCalculateConfIntervalsForForecasts(1);
			forecast = forecaster.forecast(100, stream);
			
			long finish = System.currentTimeMillis();
			
			String algorithm_name[] = forecaster.getAlgorithmName().split("\\s");
			
			double dMarker = forecaster.getTSLagMaker().getCurrentTimeStampValue();
			Date dt = new Date((long) dMarker);
			LocalDate predMarker =LocalDate.ofInstant(dt.toInstant(), ZoneId.systemDefault());
			
			predMarker = predMarker.plusDays( 1 - forecast.size());		
			for (int i = 0; i < forecast.size(); i++) {
	
				List<NumericPrediction> predsAtStep = forecast.get(i);
				NumericPrediction predForTarget = predsAtStep.get(0);

				if (!predictDate.contains(predMarker.plusDays(i).toString())) {
					predictDate.add(predMarker.plusDays(i).toString());
				}
				
				predictDouble.add(predForTarget.predicted());
			}

			p.setElapsed(finish - start);
			p.setName(algorithm_name[0]);
			p.setPredict(predictDouble);
			p.setDate(predictDate);
			predict.add(p);
			stream.close();
	    }
	    
		
	    PrintStream stream = new PrintStream("./res/forecast.csv");
  
	    // Print result
	    // header
        stream.print("data,tasso_positivita"); 		
	    for (int j = 0; j < predict.size(); j++) { 		      
	          stream.print(",tasso_positivita_" + predict.get(j).getName()); 		
	    }   		
        stream.println(); 					

        // dataset
        for (int j = 0; j < dataset.size(); j++) {
			stream.print(dataset.get(j));
		    for (int i = 0; i < predict.size(); i++) { 		      
		          stream.print(","); 		
		    }   		
          stream.println(); 					
		}
	    

        // prediction
	    for (int i = 0; i < predict.get(0).getDate().size(); i++) {
			stream.print(predict.get(0).getDate().get(i)+ ",");
						
		    for (int j = 0; j < predict.size(); j++) {
		        stream.print("," + predict.get(j).getPredict().get(i)); 		
		    }   		
	        stream.println(); 					
	    }
	    
	    
	    // Cost algorithm
	    System.out.println("algorithm_name,elapsed(ms)");
	    for (int j = 0; j < predict.size(); j++) {
	        System.out.println(predict.get(j).getName() + "," + predict.get(j).getElapsed()); 		
	    }   		

	}
	
	public static void main(String[] args) throws Exception{
		
		String path = "res/join.csv";
		System.out.println("Read dataset: " + path);
		CSVLoader loader = new CSVLoader();

		String [] op = {"-D", "3","-format", "yyyy-MM-dd"};
		loader.setOptions(op);
		
		loader.setSource(new File(path));
		Instances data = loader.getDataSet();

		CleanDataset a = new CleanDataset();
		
		a.clean(data);
	}
	
	
}
