package com.ece670.PowerManager;

import java.util.ArrayList;
import java.util.List;

public class appInfo {
	String name;
	String totalPower;
	String currentPower;
	public appInfo(String name, List<String> powerList){
		this.name = name;
		currentPower = powerList.get(0);
		totalPower = powerList.get(1);
	}

	public void updatePower(List<String> powerList){
		currentPower = powerList.get(0);
		totalPower = powerList.get(1);
	}
	public String getName(){
		return name;
	}
	
	public String getTotalPower(){
		return totalPower;
	}
	
	public String getCurrentPower(){
		return currentPower;
	}
}
