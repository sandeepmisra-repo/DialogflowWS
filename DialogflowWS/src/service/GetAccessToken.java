package service;

import java.io.FileInputStream;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.auth.oauth2.GoogleCredentials;

import CrsCde.CODE.SQLite.DB.SQLiteDBManager;
import CrsCde.CODE.SQLite.DB.SQLiteOneDBManager;

public class GetAccessToken {
	public Logger logger = LoggerFactory.getLogger(GetAccessToken.class);
	SQLiteDBManager _dbMgr;

	public GetAccessToken() {
		_dbMgr = SQLiteOneDBManager.This();
	}

	public void GetHashMap(Map<String, JSONArray> hmValue) {
		try {
			for (Map.Entry<String, JSONArray> entry : hmValue.entrySet()) {
				GetToken(entry);
			}

		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}
	}

	public void DB_tokenUpdate(String BUnit, String Token) {
		String token = " \"" + Token + "\" ";
		try {

			String data = "UPDATE `AccessToken` set\n" + "`Token`= \n" + "" + token + " where `BUnit`=" + BUnit + ";";
			_dbMgr.Execute(data);
			logger.debug("Token Update Query :: ",data);
			logger.info("Database Update Succesfully");

		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}
	}

	public void GetToken(Map.Entry<String, JSONArray> entry) {
		String token = "";
		try {
			JSONObject jsonObject = new JSONObject(AppPros.DialogflowDetails);
			String serviceAccPath = jsonObject.getJSONObject("BUnit").getJSONArray("B1").getJSONObject(0)
					.getString("ServiceAccountPath").toString();
			token = GoogleCredentials.fromStream(new FileInputStream(serviceAccPath))
					.createScoped("https://www.googleapis.com/auth/cloud-platform").refreshAccessToken()
					.getTokenValue();
			String debug = String.format("Token :: %s, Token Timeout :: %s",
					GoogleCredentials.fromStream(new FileInputStream(serviceAccPath))
							.createScoped("https://www.googleapis.com/auth/cloud-platform").refreshAccessToken()
							.getTokenValue(),
					GoogleCredentials.fromStream(new FileInputStream(serviceAccPath))
							.createScoped("https://www.googleapis.com/auth/cloud-platform").refreshAccessToken()
							.getExpirationTime());
			logger.debug(debug);
			
			
			DB_tokenUpdate(entry.getKey(), token);

		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}
	}

}

