package service;

import java.util.Map;
import java.util.TimerTask;

import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskScheduler extends TimerTask{
	public Logger logger = LoggerFactory.getLogger(TaskScheduler.class);
	public Map<String, JSONArray> hmValue;
	
	public TaskScheduler(Map<String, JSONArray> hmValue) {
		this.hmValue = hmValue;
	}
	@Override
	public void run() {
		new GetAccessToken().GetHashMap(hmValue);
	}		
}

