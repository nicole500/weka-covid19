package com.stolbovoi.weka.cobid19;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import weka.classifiers.Classifier;
import weka.classifiers.evaluation.NumericPrediction;
import weka.classifiers.functions.GaussianProcesses;
import weka.classifiers.functions.LinearRegression;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.classifiers.functions.SMOreg;
import weka.classifiers.timeseries.WekaForecaster;
import weka.core.AttributeStats;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVLoader;
import weka.experiment.Stats;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.AddExpression;
import weka.filters.unsupervised.attribute.Copy;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.attribute.ReplaceMissingWithUserConstant;
import weka.filters.unsupervised.attribute.TimeSeriesDelta;

public class MyUtils {

	public void exploreAttribute(Instances data) throws FileNotFoundException {

		System.out.println("\n\n**********************************");
		System.out.println("Data set name: " + data.relationName());
		System.out.println("Data set size: " + data.numInstances());

		PrintStream stdout = System.out;
		String foutName = "res/" + data.relationName() + ".attribute.md";
		PrintStream fileOut = new PrintStream(foutName);

		System.setOut(fileOut);

		System.out.println("Data set attributes:\n");
		System.out.println(
				"|" + "name" + "|" + "type" + "|" + "num_values" + "|" + "distinct_count" + "|" + "total_count" + "|"
						+ "missing_count" + "|" + "int_count" + "|" + "min" + "|" + "max" + "\n|-|-|-|-|-|-|-|-|-");

		for (int i = 0; i < data.numAttributes(); i++) {
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

			System.out.println("|" + name + "|" + type + "|" + num_values + "|" + distinct_count + "|" + total_count
					+ "|" + missing_count + "|" + int_count + "|" + min + "|" + max);
		}

		System.setOut(stdout);
		System.out.println("See " + foutName);

	}

	/*
	 * Report Top Ten country by vaccine usage
	 */
	public void pivot(Instances data) throws Exception {

		System.out.println("\n\n**********************************");
		System.out.println("pivot");

		// define output streams
		PrintStream stdout = System.out;

		String foutNameMD = "res/" + data.relationName() + ".top.md";
		PrintStream fileOutMD = new PrintStream(foutNameMD);

		String foutNameCSV = "res/" + data.relationName() + ".top.csv";
		PrintStream fileOutCSV = new PrintStream(foutNameCSV);

		// Transform, clean: put all data in hash of hash like:
		/*
		 * { "European Union": { "Sinovac": "9.0", "Sputnik V": "1845080.0",
		 * "Oxford/AstraZeneca": "6.736043E7", "Sinopharm/Beijing": "2260921.0",
		 * "Moderna": "1.25638176E8", "Pfizer/BioNTech": "5.47608975E8",
		 * "Johnson&Johnson": "1.8273036E7" }, "Cyprus": { "Oxford/AstraZeneca":
		 * "254507.0", "Moderna": "165449.0", "Pfizer/BioNTech": "1114694.0",
		 * "Johnson&Johnson": "27315.0" }, ... }
		 */

		HashMap<String, HashMap<String, Double>> class_feature = new HashMap<String, HashMap<String, Double>>();
		HashMap<String, Double> val = null;

		for (Instance inst : data) {
			String location = inst.toString(0);
			String vaccine = inst.toString(2);
			Double total_vaccinations = inst.value(3);

			if (class_feature.get(location) != null)
				val = class_feature.get(location);
			else {
				val = new HashMap<String, Double>();
			}

			val.put(vaccine,
					Math.max(total_vaccinations,
							(class_feature.containsKey(location) && class_feature.get(location).containsKey(vaccine))
									? class_feature.get(location).get(vaccine)
									: 0.0));
			class_feature.put(location, val);
		}

		// Transform: Totalize
		/*
		 * { "European Union": 762986627, "Cyprus": 1561965, "Czechia": 16650435,
		 * "Portugal": 19555468, "Iceland": 34168, "South Korea": 110913950, ... }
		 * 
		 * { "Sinovac": 57047519, "Sputnik V": 3690160, "Oxford/AstraZeneca": 169465616,
		 * "Sinopharm/Beijing": 23795248, "Moderna": 494821681, "Pfizer/BioNTech":
		 * 1638903522, "CanSino": 1050638, "Johnson&Johnson": 56170112 }
		 */

		HashMap<String, Double> locationTot = new HashMap<String, Double>();
		HashMap<String, Double> vaccineTot = new HashMap<String, Double>();

		for (String l : class_feature.keySet()) {
			for (String v : class_feature.get(l).keySet()) {

				Double d = class_feature.get(l).get(v);
				vaccineTot.put(v, (vaccineTot.containsKey(v)) ? (d + vaccineTot.get(v)) : d);

				locationTot.put(l, (locationTot.containsKey(l)) ? (d + locationTot.get(l)) : d);
			}
		}

		// Sort DESC
		Set<Entry<String, Double>> setVaccineTot = vaccineTot.entrySet();
		Set<Entry<String, Double>> setLocationTot = locationTot.entrySet();

		Map<Object, Object> mapVaccineTot = this.sortMapDesc(setVaccineTot);
		Map<Object, Object> mapLocationTot = this.sortMapDesc(setLocationTot);

		// Execute: Final print Top ten country by vaccines CSV
		System.setOut(fileOutCSV);
		System.out.print("Location");
		for (Object v : mapVaccineTot.keySet()) {
			System.out.print("," + v.toString().replace("'", ""));
		}
		System.out.println("");

		int i = 0;
		for (Object l : mapLocationTot.keySet()) {

			System.out.printf("%s", l.toString().replace("'", ""));
			for (Object v : mapVaccineTot.keySet()) {
				double d = (double) (class_feature.get(l).containsKey(v) ? (class_feature.get(l).get(v) / 1000000) : 0);
				System.out.printf(",%d", (int) d);
			}

			System.out.printf("\n");
			if (!(++i < 10))
				break;
		}

		// The sane as befor, but in MD format
		System.setOut(fileOutMD);
		System.out.print("|Location");
		String s = "|-";
		for (Object v : mapVaccineTot.keySet()) {
			System.out.print("|" + v.toString().replace("'", "").replace(" ", "&nbsp;"));
			s = s + "|-";
		}
		System.out.printf("\n%s\n", s);

		i = 0;
		for (Object l : mapLocationTot.keySet()) {

			System.out.printf("|%s", l.toString().replace("'", ""));
			for (Object v : mapVaccineTot.keySet()) {
				double d = (double) (class_feature.get(l).containsKey(v) ? (class_feature.get(l).get(v) / 1000000) : 0);
				System.out.printf("|%d", (int) d);
			}

			System.out.printf("\n");
			if (!(++i < 10))
				break;
		}

		System.setOut(stdout);
		System.out.println("See " + foutNameMD);
		System.out.println("See " + foutNameCSV);

	}

	private Map<Object, Object> sortMapDesc(Set<Entry<String, Double>> set) throws Exception {

		return (set.stream().sorted(Entry.comparingByValue(Comparator.reverseOrder()))
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new)));
	}

	/*
	 * Join instances vac + dpc may be useful for future cross-dataset mining
	 */
	public Instances join(Instances vac, Instances dpc) throws Exception {

		System.out.println("\n\n**********************************");
		System.out.println("join");

		PrintStream stdout = System.out;
		String foutName = "res/join.csv";
		PrintStream fileOut = new PrintStream(foutName);

		// Get valid date interval for vacc
		String vac_first_date = null;
		String vac_last_date = null;

		int vac_count_instances = 0;
		for (Instance inst : vac) {
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

		// System.out.println("vac: " + vac_count_instances + " " + vac_first_date + " "
		// + vac_last_date);

		String dpc_first_date = null;
		String dpc_last_date = null;

		int dpc_count_instances = 0;
		for (Instance inst : dpc) {
			String date = inst.toString(0);

			if (dpc_first_date == null)
				dpc_first_date = date;
			else
				dpc_last_date = date;

			dpc_count_instances++;
		}

		dpc_first_date = dpc_first_date.substring(0, 10);
		dpc_last_date = dpc_last_date.substring(0, 10);

		// System.out.println("dpc: " + dpc_count_instances + " " + dpc_first_date + " "
		// + dpc_last_date);

		String first_date = (new SimpleDateFormat("yyyy-MM-dd").parse(dpc_first_date)
				.after(new SimpleDateFormat("yyyy-MM-dd").parse(vac_first_date))) ? dpc_first_date : vac_first_date;

		String last_date = (new SimpleDateFormat("yyyy-MM-dd").parse(dpc_last_date)
				.after(new SimpleDateFormat("yyyy-MM-dd").parse(vac_last_date))) ? vac_last_date : dpc_last_date;

		// System.out.println("first_date: " + first_date);
		// System.out.println("last_date: " + last_date);

		// Check Instnces counter

		Date dt_first = new SimpleDateFormat("yyyy-MM-dd").parse(first_date);
		Date dt_last = new SimpleDateFormat("yyyy-MM-dd").parse(last_date);

		vac_count_instances = 0;
		for (Instance inst : vac) {
			String country = inst.toString(0);
			Date date = new SimpleDateFormat("yyyy-MM-dd").parse(inst.toString(2));

			if (country.contains("Italy") && date.after(dt_first) && date.before(dt_last)) {
				vac_count_instances++;

				// System.out.println(inst.toString());
			}
		}

		// System.out.println("vac: " + vac_count_instances);

		dpc_count_instances = 0;
		for (Instance inst : dpc) {
			Date date = new SimpleDateFormat("yyyy-MM-dd").parse(inst.toString(0));

			if (date.after(dt_first) && date.before(dt_last)) {
				dpc_count_instances++;
			}
		}

		// System.out.println("dpc: " + dpc_count_instances);

		if (dpc_count_instances == vac_count_instances)
			System.out.println("We can do join!!!");
		/*
		 * else throw new
		 * java.lang.Error("We can't do join!!! The number of instances is different");
		 */

		List<String> vacAttrNames = new ArrayList<String>();
		List<String> dpcAttrNames = new ArrayList<String>();

		for (int i = 0; i < vac.numAttributes(); i++) {
			String name = vac.attribute(i).name();
			vacAttrNames.add(name);
		}

		for (int i = 0; i < dpc.numAttributes(); i++) {
			String name = dpc.attribute(i).name();
			dpcAttrNames.add(name);
		}

		int i = 0;
		int j = 0;

		System.setOut(fileOut);

		// Print header
		System.out.print(String.join(",", vacAttrNames));
		System.out.println("," + String.join(",", dpcAttrNames));

		for (i = 0; i < vac.size(); i++) {

			Instance inst_vac = vac.get(i);
			Date date_vac = new SimpleDateFormat("yyyy-MM-dd").parse(inst_vac.toString(2));
			String country_vac = inst_vac.toString(0);

			Instance inst_dpc = dpc.get(j);
			Date date_dpc = new SimpleDateFormat("yyyy-MM-dd").parse(inst_dpc.toString(0));

			if (date_vac.after(dt_first) && date_vac.before(dt_last) && country_vac.contains("Italy")) {

				while (date_dpc.before(date_vac) && date_dpc.before(dt_last)) {
					j++;
					inst_dpc = dpc.get(j);
					date_dpc = new SimpleDateFormat("yyyy-MM-dd").parse(inst_dpc.toString(0));
				}

				System.out.println(inst_vac.toString() + "," + inst_dpc.toString());

			}
		}

		System.setOut(stdout);
		System.out.println("See " + foutName);

		CSVLoader loader = new CSVLoader();
		loader.setOptions(Utils.splitOptions("-D 3 -format yyyy-MM-dd"));
		loader.setSource(new File(foutName));
		System.out.println("Data set load: " + foutName);
		return (loader.getDataSet());
	}

	/*
	 * Predict the spread of COVID-19 ahead of time
	 */
	public void predict(Instances dataset) throws Exception {

		System.out.println("\n\n**********************************");
		System.out.println("predict");

		// https://waikato.github.io/weka-wiki/formats_and_processing/remove_attributes/
		/*
		 * 1 country 2 iso_code 3 date 4 total_vaccinations 5 people_vaccinated 6
		 * people_fully_vaccinated 7 daily_vaccinations_raw 8 daily_vaccinations 9
		 * total_vaccinations_per_hundred 10 people_vaccinated_per_hundred 11
		 * people_fully_vaccinated_per_hundred 12 daily_vaccinations_per_million 13
		 * vaccines 14 source_name 15 source_website
		 * 
		 * 16 data 17 stato 18 ricoverati_con_sintomi 19 terapia_intensiva 20
		 * totale_ospedalizzati 21 isolamento_domiciliare 22 totale_positivi 23
		 * variazione_totale_positivi 24 nuovi_positivi 25 dimessi_guariti 26 deceduti
		 * 27 casi_da_sospetto_diagnostico 28 casi_da_screening 29 totale_casi 30
		 * tamponi 31 casi_testati 32 note 33 ingressi_terapia_intensiva 34 note_test 35
		 * note_casi 36 totale_positivi_test_molecolare 37
		 * totale_positivi_test_antigenico_rapido 38 tamponi_test_molecolare 39
		 * tamponi_test_antigenico_rapido
		 */

		String foutNameCostMD = "./res/cost.md";
		String foutNameJoinARFF = "./res/join.arff";
		String foutNameFrorecstCSV = "./res/forecast.csv";

		// Remove unused attributes
		Remove removeFilter = new Remove();
		removeFilter.setAttributeIndices("1-2,13-17,27-28,32,34-35");
		removeFilter.setInputFormat(dataset);
		dataset = Filter.useFilter(dataset, removeFilter);

		// Replace Missing Data
		ReplaceMissingWithUserConstant missingFilter = new ReplaceMissingWithUserConstant();
		missingFilter.setAttributes("2-27");
		missingFilter.setInputFormat(dataset);
		dataset = Filter.useFilter(dataset, missingFilter);

		// Create Copy Tamponi attribute
		Copy copyFilter = new Copy();
		copyFilter.setAttributeIndices("21");
		copyFilter.setInputFormat(dataset);
		dataset = Filter.useFilter(dataset, copyFilter);

		// Calculate delta Tamponi Delta
		TimeSeriesDelta deltaFilter = new TimeSeriesDelta();
		deltaFilter.setAttributeIndices("28");
		deltaFilter.setFillWithMissing(false);
		deltaFilter.setInputFormat(dataset);
		dataset = Filter.useFilter(dataset, deltaFilter);

		// Create new attribute as:
		// tasso_positivita = 100 * nuovi_positivi / Tamponi delta
		AddExpression exprFilter = new AddExpression();
		exprFilter.setExpression("a17*100/a28");
		exprFilter.setName("tasso_positivita");
		exprFilter.setInputFormat(dataset);
		dataset = Filter.useFilter(dataset, exprFilter);

		// Remove unused attributes
		removeFilter = new Remove();
		removeFilter.setAttributeIndices("2-28");
		removeFilter.setInputFormat(dataset);
		dataset = Filter.useFilter(dataset, removeFilter);

		// Save instance
		ArffSaver saver = new ArffSaver();
		saver.setInstances(dataset);
		saver.setFile(new File(foutNameJoinARFF));
		saver.writeBatch();

		/********************************************************/
		// https://forums.pentaho.com/threads/220977-TimeSeries-Forecasting-Weka-Java-API/
		// https://tech.forums.softwareag.com/t/timeseries-forecasting-weka-java-api/208229

		WekaForecaster forecaster = new WekaForecaster();
		forecaster.setFieldsToForecast("tasso_positivita");

		ArrayList<Classifier> classifiers = new ArrayList<>();
		classifiers.add(new GaussianProcesses());
		classifiers.add(new MultilayerPerceptron());
		classifiers.add(new LinearRegression());
		classifiers.add(new SMOreg());

		ArrayList<Predict> predict = new ArrayList<Predict>();
		PrintStream stream = new PrintStream(foutNameFrorecstCSV);

		for (Classifier function : classifiers) {
			Predict p = new Predict();

			long start = System.currentTimeMillis();

			ArrayList<Double> predictDouble = new ArrayList<Double>();
			ArrayList<String> predictDate = new ArrayList<String>();

			forecaster.setBaseForecaster(function);
			forecaster.getTSLagMaker().setTimeStampField("date");
			forecaster.getTSLagMaker().setMinLag(7);
			forecaster.getTSLagMaker().setMaxLag(7);
			forecaster.getTSLagMaker().setAdjustForTrends(true);
			forecaster.getTSLagMaker().setAddMonthOfYear(true);
			forecaster.getTSLagMaker().setAddQuarterOfYear(true);

			List<List<NumericPrediction>> forecast = null;

			// forecaster.buildForecaster(dataset, stream);
			forecaster.buildForecaster(dataset);
			forecaster.primeForecaster(dataset);
			forecaster.setCalculateConfIntervalsForForecasts(1);
			// forecast = forecaster.forecast(100, stream);
			forecast = forecaster.forecast(100);

			long finish = System.currentTimeMillis();

			String algorithm_name[] = forecaster.getAlgorithmName().split("\\s");

			double dMarker = forecaster.getTSLagMaker().getCurrentTimeStampValue();
			Date dt = new Date((long) dMarker);
			LocalDate predMarker = LocalDate.ofInstant(dt.toInstant(), ZoneId.systemDefault());

			predMarker = predMarker.plusDays(1 - forecast.size());
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
		}

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
			stream.print(predict.get(0).getDate().get(i) + ",");

			for (int j = 0; j < predict.size(); j++) {
				stream.print("," + predict.get(j).getPredict().get(i));
			}
			stream.println();
		}

		stream.close();

		// Cost algorithm
		stream = new PrintStream(foutNameCostMD);
		stream.println("|algorithm_name|elapsed(ms)\n|-|-");
		for (int j = 0; j < predict.size(); j++) {
			stream.println("|" + predict.get(j).getName() + "|" + predict.get(j).getElapsed());
		}
		stream.close();

		System.out.println("See " + foutNameJoinARFF);
		System.out.println("See " + foutNameFrorecstCSV);
		System.out.println("See " + foutNameCostMD);

	}

}