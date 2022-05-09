package service;

import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;

import org.apache.log4j.PropertyConfigurator;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import CrsCde.CODE.Common.Consts.OSConst;
import CrsCde.CODE.Common.Utils.TypeUtil;
import CrsCde.CODE.SQLite.DB.SQLiteDBManager;
import CrsCde.CODE.SQLite.DB.SQLiteOneDBManager;

public class Main {
	static Logger logger = LoggerFactory.getLogger(Main.class);
	static SQLiteDBManager _dbMgr;

	public static void main(String[] args) {
		String propFileName = GetPropFileName(args);
		PropertyConfigurator.configure(propFileName);
		try {
			InitDb();
			InitAppConfig(propFileName);
//			StartRestServices();
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}

	}

	private static void InitDb() {
		try {
			logger.info("Init DBconfig.....");
			SQLiteOneDBManager.InitDBManager(null, 0, "DialogflowDB", null, null, 10, 10);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}
	} 

	private static void StartRestServices() {
		// TODO Auto-generated method stub
		JSONArray jsonarray = new JSONArray();
		HashMap<String, JSONArray> hmValue = new HashMap();
		DB_Handler dbHandle = new DB_Handler();
		try {
			dbHandle.DB_Create();
			for (int i = 0; i < Integer.parseInt(AppPros.TokenCount); i++) {
				int val = i + 1;
				JSONObject jsonObject = new JSONObject(AppPros.DialogflowDetails);
				jsonarray = jsonObject.getJSONObject("BUnit").getJSONArray("B" + val);
				hmValue.put("\"B" + String.valueOf(val) + "\"", jsonarray);
			}
			logger.info("Mapped Data :: " + hmValue);
			for (Map.Entry<String, JSONArray> entry : hmValue.entrySet()) {
				dbHandle.DB_Insert(entry.getKey().toString());
			}
			Timer timer = new Timer();
			TaskScheduler task = new TaskScheduler(hmValue);
			timer.scheduleAtFixedRate(task, 0, 3000000);
			RestWSReqInterface.StartService();

		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage(), e);
		}
	}

	/**
	 *
	 * @param args
	 * @return
	 */
	private static String GetPropFileName(String[] args) {
		if (args.length > 0) {
			return args[0];
		}

		if (OSConst.Type().equals(OSConst.OSType.Windows)) {
			return "C:\\Config\\TechMConfigs.properties";
		} else {
			return "/active/config/TechMConfigs.properties";
		}
	}

	private static void InitAppConfig(String filename) throws Exception {
		logger.info("Initializing AppConfig...");
		Properties props = new Properties();
		props.load(new FileInputStream(filename));
		Field[] allFields = AppPros.class.getDeclaredFields();

		for (Field field : allFields) {
			field.setAccessible(true);
			if (props.getProperty(field.getName()) != null) {
				field.set(null, TypeUtil.ValueOf(field.getType(), props.getProperty(field.getName())));
				logger.info(field.getName() + ": " + props.getProperty(field.getName()));
			}
		}
	}

}
