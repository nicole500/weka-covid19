package com.stolbovoi.weka.cobid19;


import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import weka.core.Instance;
import weka.core.Instances;

public class JoinDataset {
	
	public void  join (Instances vac, Instances dpc) throws ParseException, FileNotFoundException {

		PrintStream stdout = System.out;
		String foutName = "res/join.csv"; 
		PrintStream fileOut = new PrintStream(foutName);
		
		// Get valid date interval for vacc		
		String vac_first_date = null;
		String vac_last_date = null;

		int vac_count_instances = 0; 
		for(Instance inst: vac){
			String country = inst.toString(0);
			String date = inst.toString(2);

			if (country.contains("Italy")) {
				if (vac_first_date == null)
					vac_first_date = date;
				else
					vac_last_date = date;
				
				vac_count_instances++;
			}
		}

		// System.out.println("vac: " + vac_count_instances + " " + vac_first_date + " " +  vac_last_date);
		
		String dpc_first_date = null;
		String dpc_last_date = null;

		int dpc_count_instances = 0; 
		for(Instance inst: dpc){
			String date = inst.toString(0);

			if (dpc_first_date == null)
				dpc_first_date = date;
			else
				dpc_last_date = date;
			
			dpc_count_instances++;
		}

		
		dpc_first_date = dpc_first_date.substring(0,10);
		dpc_last_date = dpc_last_date.substring(0,10);
		
		// System.out.println("dpc: " + dpc_count_instances + " " + dpc_first_date + " " +  dpc_last_date);
		
		String first_date =  		(
				new SimpleDateFormat("yyyy-MM-dd").parse(dpc_first_date).after(  
				new SimpleDateFormat("yyyy-MM-dd").parse(vac_first_date))		
			) ? dpc_first_date : vac_first_date;

		String last_date =  		(
				new SimpleDateFormat("yyyy-MM-dd").parse(dpc_last_date).after(  
				new SimpleDateFormat("yyyy-MM-dd").parse(vac_last_date))		
			) ? vac_last_date : dpc_last_date;

		// System.out.println("first_date: " + first_date);
		// System.out.println("last_date: "  + last_date);


		
		// Check Instnces counter

		Date dt_first = new SimpleDateFormat("yyyy-MM-dd").parse(first_date);
		Date dt_last  = new SimpleDateFormat("yyyy-MM-dd").parse(last_date);
		
		vac_count_instances = 0; 
		for(Instance inst: vac){
			String country = inst.toString(0);
			Date date = new SimpleDateFormat("yyyy-MM-dd").parse(inst.toString(2));

			if (country.contains("Italy") &&
				date.after(dt_first) &&
				date.before(dt_last)) {
				vac_count_instances++;
				
				// System.out.println(inst.toString());
			}
		}

		// System.out.println("vac: " + vac_count_instances);
		
		dpc_count_instances = 0; 
		for(Instance inst: dpc){
			Date date = new SimpleDateFormat("yyyy-MM-dd").parse(inst.toString(0));

			if ( date.after(dt_first) &&
				date.before(dt_last)) {
				dpc_count_instances++;
			}
		}

		// System.out.println("dpc: " + dpc_count_instances);
		
		if (dpc_count_instances == vac_count_instances)
			System.out.println("We can do join!!!");
		/*	
		else
			throw new java.lang.Error("We can't do join!!! The number of instances is different");
		 */
		
		List<String> vacAttrNames = new ArrayList<String>();
		List<String> dpcAttrNames = new ArrayList<String>();  
		
		for(int i = 0; i < vac.numAttributes(); i++ ) {
			String name = vac.attribute(i).name();
			vacAttrNames.add(name);
		}

		for(int i = 0; i < dpc.numAttributes(); i++ ) {
			String name = dpc.attribute(i).name();
			dpcAttrNames.add(name);
		}

		int i = 0;
		int j = 0;
		
		System.setOut(fileOut);

		// Print header
		System.out.print(String.join(",", vacAttrNames));
		System.out.println("," + String.join(",", dpcAttrNames));

		for (i= 0; i < vac.size(); i ++) {

			Instance inst_vac = vac.get(i);
			Date date_vac = new SimpleDateFormat("yyyy-MM-dd").parse(inst_vac.toString(2));
			String country_vac = inst_vac.toString(0);

			Instance inst_dpc = dpc.get(j);
			Date date_dpc = new SimpleDateFormat("yyyy-MM-dd").parse(inst_dpc.toString(0));

			if ( date_vac.after(dt_first) &&
				 date_vac.before(dt_last) &&
				 country_vac.contains("Italy")) {

				while(date_dpc.before(date_vac) && date_dpc.before(dt_last)) {
					j++;
					inst_dpc = dpc.get(j);
					date_dpc = new SimpleDateFormat("yyyy-MM-dd").parse(inst_dpc.toString(0));
				}

				System.out.println(
						inst_vac.toString() + ","+ inst_dpc.toString());
				
			}
		}		

		System.setOut(stdout);
		System.out.println("See " + foutName);
	}	
}
