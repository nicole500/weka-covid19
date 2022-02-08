package com.stolbovoi.weka.cobid19;

import java.util.ArrayList;

public class Predict {
	
	private String name;
	private long elapsed;
	private ArrayList<Double> predict;
	private ArrayList<String> date;
	
	public String getName() {
	    return name;
	}
	
	public void setName(String o) {
	    this.name = o;
	}
	
	public long getElapsed() {
	    return elapsed;
	}
	
	public void setElapsed(long o) {
	    this.elapsed = o;
	}
	
	public  ArrayList<Double> getPredict() {
	    return predict;
	}
	
	public void setPredict( ArrayList<Double> o) {
	    this.predict = o;
	}
	
	public  ArrayList<String> getDate() {
	    return date;
	}
	
	public void setDate( ArrayList<String> o) {
	    this.date = o;
	}
	
}
