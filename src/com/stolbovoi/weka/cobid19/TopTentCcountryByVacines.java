package com.stolbovoi.weka.cobid19;

import java.io.File;
import java.io.PrintStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.CSVLoader;

public class TopTentCcountryByVacines {
	
	public void prepare(Instances data ) throws Exception {
	
		// define output streams 
		PrintStream stdout = System.out;

		String foutNameMD = "res/" + data.relationName() + ".top.md"; 
		PrintStream fileOutMD = new PrintStream(foutNameMD);

		String foutNameCSV = "res/" + data.relationName() + ".top.csv"; 
		PrintStream fileOutCSV = new PrintStream(foutNameCSV);
		
		
		// Transform, clean: put all data in hash of hash like:
		/*
		{
			  "European Union": {
			    "Sinovac": "9.0",
			    "Sputnik V": "1845080.0",
			    "Oxford/AstraZeneca": "6.736043E7",
			    "Sinopharm/Beijing": "2260921.0",
			    "Moderna": "1.25638176E8",
			    "Pfizer/BioNTech": "5.47608975E8",
			    "Johnson&Johnson": "1.8273036E7"
			  },
			  "Cyprus": {
			    "Oxford/AstraZeneca": "254507.0",
			    "Moderna": "165449.0",
			    "Pfizer/BioNTech": "1114694.0",
			    "Johnson&Johnson": "27315.0"
			  },
			  ...
		}	  
		*/	  
		
		HashMap<String, HashMap<String, Double>> class_feature = 
				 new HashMap<String, HashMap<String, Double>>();
		HashMap<String, Double> val = null;
		 
		for(Instance inst: data){
			String location = inst.toString(0);
			String vaccine = inst.toString(2);
			Double total_vaccinations = inst.value(3);
			
			if (class_feature.get(location) != null )
				val = class_feature.get(location);
			else {
				val = new HashMap<String, Double>();
			}
			
			val.put(vaccine, Math.max(total_vaccinations,
				( class_feature.containsKey(location) && 
				  class_feature.get(location).containsKey(vaccine)) ?
				  class_feature.get(location).get(vaccine) : 0.0					
				));
			class_feature.put(location, val);		
		}

		
		// Transform: Totalize
		/*
		   {
			    "European Union": 762986627,
			    "Cyprus": 1561965,
			    "Czechia": 16650435,
			    "Portugal": 19555468,
			    "Iceland": 34168,
			    "South Korea": 110913950,
				...
			  }

			  {
			    "Sinovac": 57047519,
			    "Sputnik V": 3690160,
			    "Oxford/AstraZeneca": 169465616,
			    "Sinopharm/Beijing": 23795248,
			    "Moderna": 494821681,
			    "Pfizer/BioNTech": 1638903522,
			    "CanSino": 1050638,
			    "Johnson&Johnson": 56170112
			  }
		*/
		
		HashMap<String, Double> locationTot = new HashMap<String, Double>();
		HashMap<String, Double> vaccineTot  = new HashMap<String, Double>();
				
		for(String l : class_feature.keySet()) {
			for(String v : class_feature.get(l).keySet()) {
				
				Double d = class_feature.get(l).get(v);
				vaccineTot.put(v, (vaccineTot.containsKey(v)) ?
						(d + vaccineTot.get(v)) : d);

				locationTot.put(l, (locationTot.containsKey(l)) ?
						(d + locationTot.get(l)) : d);
			}
		} 

		// Sort DESC				
		Set<Entry<String, Double>> setVaccineTot = vaccineTot.entrySet(); 		
		Set<Entry<String, Double>> setLocationTot = locationTot.entrySet(); 		

		Map<Object, Object> mapVaccineTot = this.sortMapDesc(setVaccineTot);
		Map<Object, Object> mapLocationTot = this.sortMapDesc(setLocationTot);

		// Execute: Final print Top ten country by vaccines	CSV	
		System.setOut(fileOutCSV);
		System.out.print("Location");
		for(Object v : mapVaccineTot.keySet()) {
			System.out.print("," + v.toString().replace("'", "") );
		}
		System.out.println("");

		int i = 0;
		for(Object l : mapLocationTot.keySet()){ 

			System.out.printf("%s", l.toString().replace("'", ""));
			for(Object v : mapVaccineTot.keySet()) {
				double d = (double) (
						class_feature.get(l).containsKey(v) ?
						(class_feature.get(l).get(v) / 1000000) : 0);
			 	System.out.printf(",%d", (int) d);
			}
			
		 	System.out.printf("\n");
		 	if (! (++i < 10)) break;
		 }

		//The sane as befor, but in MD format	
		System.setOut(fileOutMD);
		System.out.print("|Location");
		String s = "|-";
		for(Object v : mapVaccineTot.keySet()) {
			System.out.print("|" + v.toString().replace("'", "").replace(" ", "&nbsp;") );
			s = s + "|-";
		}
		System.out.printf("\n%s\n", s);
		
		i = 0;
		for(Object l : mapLocationTot.keySet()){ 

			System.out.printf("|%s", l.toString().replace("'", ""));
			for(Object v : mapVaccineTot.keySet()) {
				double d = (double) (
						class_feature.get(l).containsKey(v) ?
						(class_feature.get(l).get(v) / 1000000) : 0);
			 	System.out.printf("|%d", (int) d);
			}
			
		 	System.out.printf("\n");
		 	if (! (++i < 10)) break;
		 }

		System.setOut(stdout);
		System.out.println("See " + foutNameMD);
		System.out.println("See " + foutNameCSV);

	}
	
	public static void main(String[] args) throws Exception{
		
		String path = "data/country_vaccinations_by_manufacturer.csv";
		System.out.println("Read dataset: " + path);
		CSVLoader loader = new CSVLoader();
		loader.setSource(new File(path));
		Instances data = loader.getDataSet();

		TopTentCcountryByVacines a = new TopTentCcountryByVacines();
		
		a.prepare(data);
	}

	private Map<Object, Object> sortMapDesc(Set<Entry<String, Double>> set) throws Exception{
	
		return( 
			set.stream()
		    .sorted(Entry.comparingByValue(Comparator.reverseOrder()))
		    .collect(Collectors.toMap(Entry::getKey, Entry::getValue,
		      (e1, e2) -> e1, LinkedHashMap::new))
			 );		
	}	
}
