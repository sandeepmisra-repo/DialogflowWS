package service;

import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.security.cert.X509Certificate;
import java.util.Base64;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.container.httpserver.HttpServerFactory;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.net.httpserver.HttpServer;
import com.sun.xml.internal.ws.client.sei.ResponseBuilder;

import CrsCde.CODE.SQLite.DB.SQLiteDBManager;
import CrsCde.CODE.SQLite.DB.SQLiteOneDBManager;

/**
 *
 * @author Sandeep Kumar Misra
 */
@Path("/Service")
public class RestWSReqInterface {
	static Logger logger = LoggerFactory.getLogger(RestWSReqInterface.class);
	SQLiteDBManager _dbMgr;

	public RestWSReqInterface() {
		_dbMgr = SQLiteOneDBManager.This();
	}

	public static void StartService() throws Exception {
		String hostIP = AppPros.ReqInterfaceRestWSIP;
		Integer port = AppPros.ReqInterfaceRestWSPort;
		System.out.println("hostIp::" + hostIP + "::Port::" + port);

		ResourceConfig resCfg = new PackagesResourceConfig(RestWSReqInterface.class.getPackage().getName());
		URI uri = UriBuilder.fromUri("http://" + hostIP + "/").port(port).build();
		logger.info(uri.toString());
		HttpServer httpSrvr = HttpServerFactory.create(uri, resCfg);
		httpSrvr.start();
	}
	@POST
	@Path("/DialogBotTextWS")
	@Produces("application/json")
	public Response postTextRequest(@QueryParam("bunit") String bunit, @QueryParam("languagecode") String languageCode,
			@QueryParam("inputtext") String inputText, @QueryParam("session") String session) throws JSONException {
		String token = DB_Select(bunit);
		return BuildResponses(FetchTextInfo(token, languageCode, inputText, session).toString());
	}
	@POST
	@Path("/DialogAudioWS")
	@Produces("application/json")
	public Response postAudioRequest(@QueryParam("bunit") String bunit, @QueryParam("languagecode") String languageCode,
			@QueryParam("session") String session, @QueryParam("audio") String audio) throws JSONException {
		String token = DB_Select(bunit);
		return BuildResponses(FetchAudioInfo(token, languageCode, session, audio));
	}

	@POST
	@Path("/DialogBotWS")
	@Produces("application/json")
	public Response postOpAudioRequest(@QueryParam("bunit") String bunit,
			@QueryParam("languagecode") String languageCode, @QueryParam("session") String session,
			@QueryParam("inputaudio") String audio, @QueryParam("outputaudio") String outputAudio)
			throws JSONException {
		String token = DB_Select(bunit);
		return BuildResponses(FetchOutputAudioInfo(token, languageCode, session, audio, outputAudio));
	}

	public Response BuildResponses(String result) {
		try {
			return Response.status(Response.Status.OK).header("Access-Control-Allow-Origin", "*")
					.header("Access-Control-Allow-Methods", "POST, GET, PUT, UPDATE, OPTIONS")
					.header("Access-Control-Allow-Headers", "Content-Type, Accept, X-Requested-With").entity(result)
					.build();
		} catch (Exception ex) {
			logger.info(ex.toString());
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).header("Access-Control-Allow-Origin", "*")
					.header("Access-Control-Allow-Methods", "POST, GET, PUT, UPDATE, OPTIONS")
					.header("Access-Control-Allow-Headers", "Content-Type, Accept, X-Requested-With")
					.entity(ex.toString()).build();
		}
	}

	public String DB_Select(String BUnit) {
		String result = "";
		try {
			logger.trace("BUnit :: " + BUnit);
			AccessToken accToken = _dbMgr.Find(AccessToken.class, "BUnit='" + BUnit + "'");
			result = accToken.getToken();
			logger.trace("Token :: "+result);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return result;
	}

	/**
	 * @method Text-Input
	 * @param token
	 * @param parameter
	 * @param inputText
	 * @param session
	 * @throws JSONException
	 * @Desc : This Method Use For Invoke the Google DialogFlow Rest API using Text
	 *       Input and Text Output.
	 * 
	 */

	public String FetchTextInfo(String token, String languageCode, String inputText, String session)
			throws JSONException {
		try {

			JSONObject obj = new JSONObject();
			JSONObject newObj = new JSONObject();
			logger.info("DialogFlow Text Bot Activated....");
			JSONObject data = postTextInfo(token, languageCode, inputText, session);
//			newObj.put("FulfilmentText", data.getJSONObject("Response").getJSONObject("queryResult").getString("fulfillmentText")
//					.toString());
			newObj.put("FulfilmentText", data.getJSONObject("Response").getJSONObject("queryResult").getJSONArray("fulfillmentMessages")
					.toString());
			newObj.put("InputText", data.getJSONObject("Response").getJSONObject("queryResult").getString("queryText")
					.toString());
			obj.put("ResponseCode", data.getString("ResponseCode"));
			obj.put("Response",newObj);
			return obj.toString();

		} catch (Exception ex) {
			logger.error(ex.toString(), ex);
			JSONObject jResp = new JSONObject();
			jResp.put("ResponseCode", "500");
			jResp.put("Response", "Error");
			return jResp.toString();
		}
	}
	public JSONObject postTextInfo(String token, String languageCode, String inputText, String session) {
		JSONObject responseData = new JSONObject();
		try {

			logger.trace(
					"----------------------------------------- SSL Certificate------------------------------------------------------------");
			// SSL Certificate
			TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					return null;
				}

				public void checkClientTrusted(X509Certificate[] certs, String authType) {
				}

				public void checkServerTrusted(X509Certificate[] certs, String authType) {
				}
			} };
			logger.trace(
					"---------------------------------------- Install the all-trusting trust manager-----------------------------------------");

			// Install the all-trusting trust manager
			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
			logger.trace(
					"---------------------------------------- Create all-trusting host name verifier-----------------------------------------");

			// Create all-trusting host name verifier
			HostnameVerifier allHostsValid = new HostnameVerifier() {
				public boolean verify(String hostname, SSLSession session) {
					return true;
				}
			};
			logger.trace(
					"---------------------------------------- Install the all-trusting host verifier-----------------------------------------");
			// Install the all-trusting host verifier-----29/9
			HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
			//
			logger.trace(
					"--------------------------------------------- SSL End Line-------------------------------------------------------------");

			String jsonObj = "";
			{
				if (languageCode.equals("1")) {
					jsonObj = "{\"queryInput\": {\"text\": {\"text\": \"" + inputText
							+ "\",\"language_code\": \"en-US\"}}}";
				}
				if (languageCode.equals("2")) {
					jsonObj = "{\"queryInput\": {\"text\": {\"text\": \"" + inputText
							+ "\",\"language_code\": \"fr-FR\"}}}";
				}
				if (languageCode.equals("3")) {
					jsonObj = "{\"queryInput\": {\"text\": {\"text\": \"" + inputText
							+ "\",\"language_code\": \"es-ES\"}}}";
				}
			}
			JSONObject jsonObject = new JSONObject(AppPros.DialogflowDetails);
			String ProjectID = jsonObject.getJSONObject("BUnit").getJSONArray("B1").getJSONObject(0)
					.getString("ProjectID").toString();
			String Version = jsonObject.getJSONObject("BUnit").getJSONArray("B1").getJSONObject(0).getString("Version")
					.toString();
			String weburl = "";
			if (Version.equals("beta")) {

				weburl = "https://dialogflow.googleapis.com/v2beta1/projects/" + ProjectID + "/agent/sessions/"
						+ session + ":detectIntent";
			}
			if (Version.equals("prod")) {
				weburl = "https://dialogflow.googleapis.com/v2/projects/" + ProjectID + "/agent/sessions/" + session
						+ ":detectIntent";
			}
			logger.trace("Google ASR URL :: " + weburl);
			java.net.URL url = new java.net.URL(weburl);
			java.net.HttpURLConnection connjava = (java.net.HttpURLConnection) url.openConnection();
			connjava.setRequestMethod("POST");
			connjava.setRequestProperty("Authorization", "Bearer " + token);
			connjava.setRequestProperty("Content-Type", "application/json");
			connjava.setDoInput(true);
			connjava.setDoOutput(true);
			connjava.setAllowUserInteraction(true);
			connjava.connect();
			java.io.DataOutputStream dos = new java.io.DataOutputStream(connjava.getOutputStream());
			dos.writeBytes(jsonObj.toString());
			dos.flush();
			dos.close();
			int respcode = connjava.getResponseCode();
			responseData.put("ResponseCode", Integer.toString(respcode));
			if (connjava.getResponseCode() != java.net.HttpURLConnection.HTTP_OK) {
				logger.info("********* Unable to connect to the URL *********" + connjava.getResponseMessage());
				responseData.put("Response", connjava.getResponseMessage());
			} else {
				logger.info("********* Connected *********\n");
				java.io.BufferedReader in = new java.io.BufferedReader(
						new java.io.InputStreamReader(connjava.getInputStream()));
				StringBuilder sbout = new StringBuilder();
				String line;
				while ((line = in.readLine()) != null) {
					sbout.append(line);
				}
				in.close();
				JSONObject newJson = new JSONObject(sbout.toString());
				responseData.put("Response", newJson);
				logger.trace("-->"+sbout.toString()+"<--");

			}
		}

		catch (Exception ex) {
			try {
				responseData.put("ResponseCode", Integer.toString(500));
				responseData.put("Response", ex.toString());
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			logger.error(ex.getMessage(), ex);
		}
		return responseData;
	}

	/**
	 * @method Text-Input
	 * @param token
	 * @param parameter
	 * @param session
	 * @param inputText
	 * @return JSONObject
	 * @throws JSONException
	 * 
	 */

	/**
	 * @method Audio-Input
	 * @param token
	 * @param parameter
	 * @param session
	 * @param inFile
	 * @return JSONObject
	 * @throws JSONException
	 * 
	 */

	public String FetchAudioInfo(String token, String languageCode, String session, String inFile)
			throws JSONException {
		try {

			JSONObject obj = new JSONObject();
			JSONObject newObj = new JSONObject();
			logger.info("Dialogflow Input Audio Activated....");
			JSONObject data = postInputAudioInfo(token, languageCode, session, inFile);
			obj.put("ResponseCode", data.getString("ResponseCode"));
			newObj.put("FulfilmentText", data.getJSONObject("Response").getJSONObject("queryResult").getString("fulfillmentText")
					.toString());
			newObj.put("InputText", data.getJSONObject("Response").getJSONObject("queryResult").getString("queryText")
					.toString());
			obj.put("Response",newObj);
			return obj.toString();

		} catch (Exception ex) {
			JSONObject jResp = new JSONObject();
			jResp.put("ResponseCode", "500");
			jResp.put("Response", "Error");
			return jResp.toString();
		}
	}

	public JSONObject postInputAudioInfo(String token, String languageCode, String session, String inFile) {
		JSONObject responseData = new JSONObject();
		try {

			logger.trace(
					"----------------------------------------- SSL Certificate------------------------------------------------------------");
			// SSL Certificate
			TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					return null;
				}

				public void checkClientTrusted(X509Certificate[] certs, String authType) {
				}

				public void checkServerTrusted(X509Certificate[] certs, String authType) {
				}
			} };
			logger.trace(
					"---------------------------------------- Install the all-trusting trust manager-----------------------------------------");

			// Install the all-trusting trust manager
			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
			logger.trace(
					"---------------------------------------- Create all-trusting host name verifier-----------------------------------------");

			// Create all-trusting host name verifier
			HostnameVerifier allHostsValid = new HostnameVerifier() {
				public boolean verify(String hostname, SSLSession session) {
					return true;
				}
			};
			logger.trace(
					"---------------------------------------- Install the all-trusting host verifier-----------------------------------------");
			// Install the all-trusting host verifier-----29/9
			HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
			//
			logger.trace(
					"--------------------------------------------- SSL End Line-------------------------------------------------------------");

			File inputFile = new File(inFile);
			String outFile = inputFile.getParent() + "/Voice.wav";
			File outputFile = new File(outFile);
			convertULawFileToWavTo128KBPS(inputFile, outputFile);
			String Audio = inputFile.getParent() + "/Voice.wav";
			File file = new File(inFile);
			String encode = Base64.getEncoder().encodeToString(Files.readAllBytes(file.toPath()));
			String jsonObj = "";
			{
				if (languageCode.equals("1")) {
					jsonObj = "{\"queryInput\": {\"audioConfig\": {\"languageCode\": \"en-US\"}},\"inputAudio\":\""
							+ encode + "\"}";
				}
				if (languageCode.equals("2")) {
					jsonObj = "{\"queryInput\": {\"audioConfig\": {\"languageCode\": \"fr-FR\"}},\"inputAudio\":\""
							+ encode + "\"}";
				}
				if (languageCode.equals("3")) {
					jsonObj = "{\"queryInput\": {\"audioConfig\": {\"languageCode\": \"es-ES\"}},\"inputAudio\":\""
							+ encode + "\"}";
				}
			}

			JSONObject jsonObject = new JSONObject(AppPros.DialogflowDetails);
			String ProjectID = jsonObject.getJSONObject("BUnit").getJSONArray("B1").getJSONObject(0)
					.getString("ProjectID").toString();
			String Version = jsonObject.getJSONObject("BUnit").getJSONArray("B1").getJSONObject(0).getString("Version")
					.toString();
			String weburl = "";
			if (Version.equals("beta")) {

				weburl = "https://dialogflow.googleapis.com/v2beta1/projects/" + ProjectID + "/agent/sessions/"
						+ session + ":detectIntent";
			}
			if (Version.equals("prod")) {
				weburl = "https://dialogflow.googleapis.com/v2/projects/" + ProjectID + "/agent/sessions/" + session
						+ ":detectIntent";
			}
			logger.trace("Google DialogFlow :: " + weburl);
			java.net.URL url = new java.net.URL(weburl);
			java.net.HttpURLConnection connjava = (java.net.HttpURLConnection) url.openConnection();
			connjava.setRequestMethod("POST");
			connjava.setRequestProperty("Authorization", "Bearer " + token);
			connjava.setRequestProperty("Content-Type", "application/json");
			connjava.setDoInput(true);
			connjava.setDoOutput(true);
			connjava.setAllowUserInteraction(true);
			java.io.DataOutputStream dos = new java.io.DataOutputStream(connjava.getOutputStream());
			dos.writeBytes(jsonObj.toString());
			dos.flush();
			dos.close();
			int respcode = connjava.getResponseCode();
			responseData.put("ResponseCode", Integer.toString(respcode));
			if (connjava.getResponseCode() != java.net.HttpURLConnection.HTTP_OK) {
				logger.info("********* Unable to connect to the URL *********" + connjava.getResponseMessage());
				responseData.put("Respose", connjava.getResponseMessage());
			} else {
				logger.info("********* Connected *********\n");
				java.io.BufferedReader in = new java.io.BufferedReader(
						new java.io.InputStreamReader(connjava.getInputStream()));
				StringBuilder sbout = new StringBuilder();
				String line;
				while ((line = in.readLine()) != null) {
					sbout.append(line);
				}
				in.close();
				JSONObject js = new JSONObject(sbout.toString());
				responseData.put("Response", js);
			}
		}

		catch (Exception ex) {
			try {
				responseData.put("ResponseCode", Integer.toString(500));
				responseData.put("Response", ex.toString());
			} catch (JSONException e) {
				e.printStackTrace();
			}
			logger.error(ex.getMessage(), ex);
		}
		return responseData;
	}

	/**
	 * @method Audio-Input
	 * @param token
	 * @param parameter
	 * @param session
	 * @param inFile
	 * @return JSONObject
	 * @throws JSONException
	 * 
	 */

	/**
	 * @method Audio-Output
	 * @param token
	 * @param parameter
	 * @param session
	 * @param inFile
	 * @param outFile
	 * @return JSONObject
	 * @throws JSONException
	 * 
	 */

	public String FetchOutputAudioInfo(String token, String languageCode, String session, String inFile, String outFile)
			throws JSONException {
		try {

			JSONObject obj = new JSONObject();
			logger.info("DialogFlow Output Audio Activated....");
			obj = getOutputAudioReply(token, languageCode, session, inFile, outFile);
			return obj.toString();

		} catch (Exception ex) {
			logger.error(ex.toString(), ex);
			JSONObject jResp = new JSONObject();
			jResp.put("ResponseCode", "500");
			jResp.put("Response", ex.toString());
			return jResp.toString();
		}
	}

	public JSONObject getOutputAudioReply(String token, String languageCode, String session, String inFile,
			String outFile) {
		logger.trace("Access Token : " + token);
		JSONObject obj = new JSONObject();
		JSONObject newObj = new JSONObject();
		try {
			JSONObject data = postOutAudioInfo(token, languageCode, session, inFile);
			obj.put("ResponseCode", data.getString("ResponseCode"));
			newObj.put("FulfilmentText", data.getJSONObject("Response").getJSONObject("queryResult").getString("fulfillmentText")
					.toString());
			newObj.put("InputText", data.getJSONObject("Response").getJSONObject("queryResult").getString("queryText")
					.toString());
			
			byte[] byt = Base64.getDecoder().decode(data.getJSONObject("Response").getString("outputAudio").toString());
			logger.trace(data.toString());
			try (OutputStream stream = new FileOutputStream(outFile + "\\BotReply.wav")) {
				stream.write(byt);
				logger.trace(stream.toString());
				stream.close();
				newObj.put("AudioFilePath", outFile + "\\BotReply.wav");
			} catch (Exception ex) {
				logger.error(ex.getMessage(), ex);
				obj.put("ResponseCode", "500");
				obj.put("Respose", ex.getMessage());
			}
			obj.put("Response",newObj);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			try {
				obj.put("ResponseCode", "500");
				obj.put("Respose", ex.getMessage());
				;
			} catch (JSONException e1) {
				logger.error(e1.getMessage(), e1);
			}

		}
		return obj;
	}

	public JSONObject postOutAudioInfo(String token, String languageCode, String session, String inFile) {
		JSONObject responseData = new JSONObject();
		try {

			logger.trace(
					"----------------------------------------- SSL Certificate------------------------------------------------------------");
			// SSL Certificate
			TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					return null;
				}

				public void checkClientTrusted(X509Certificate[] certs, String authType) {
				}

				public void checkServerTrusted(X509Certificate[] certs, String authType) {
				}
			} };
			logger.trace(
					"---------------------------------------- Install the all-trusting trust manager-----------------------------------------");

			// Install the all-trusting trust manager
			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
			logger.trace(
					"---------------------------------------- Create all-trusting host name verifier-----------------------------------------");

			// Create all-trusting host name verifier
			HostnameVerifier allHostsValid = new HostnameVerifier() {
				public boolean verify(String hostname, SSLSession session) {
					return true;
				}
			};
			logger.trace(
					"---------------------------------------- Install the all-trusting host verifier-----------------------------------------");
			// Install the all-trusting host verifier-----29/9
			HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
			//
			logger.trace(
					"--------------------------------------------- SSL End Line-------------------------------------------------------------");
			File inputFile = new File(inFile);
			String outFile = inputFile.getParent() + "/Voice.wav";
			File outputFile = new File(outFile);
			convertULawFileToWavTo128KBPS(inputFile, outputFile);
			String Audio = inputFile.getParent() + "/Voice.wav";
			File file = new File(inFile);
			String encode = Base64.getEncoder().encodeToString(Files.readAllBytes(file.toPath()));
			String jsonObj = "";
			{
				if (languageCode.equals("1")) {
					jsonObj = "{\"queryInput\": {\"audioConfig\": {\"languageCode\": \"en-US\"}},\"inputAudio\":\""
							+ encode
							+ "\",\"outputAudioConfig\": {\"audioEncoding\": \"OUTPUT_AUDIO_ENCODING_LINEAR_16\",\"sampleRateHertz\": \"8000\",\"synthesizeSpeechConfig\": {\"voice\": {\"ssmlGender\": \"SSML_VOICE_GENDER_FEMALE\"}}}}";
				}
				if (languageCode.equals("2")) {
					jsonObj = "{\"queryInput\": {\"audioConfig\": {\"languageCode\": \"fr-FR\"}},\"inputAudio\":\""
							+ encode
							+ "\",\"outputAudioConfig\": {\"audioEncoding\": \"OUTPUT_AUDIO_ENCODING_LINEAR_16\",\"sampleRateHertz\": \"8000\",\"synthesizeSpeechConfig\": {\"voice\": {\"ssmlGender\": \"SSML_VOICE_GENDER_FEMALE\"}}}}";
				}
				if (languageCode.equals("3")) {
					jsonObj = "{\"queryInput\": {\"audioConfig\": {\"languageCode\": \"es-ES\"}},\"inputAudio\":\""
							+ encode
							+ "\",\"outputAudioConfig\": {\"audioEncoding\": \"OUTPUT_AUDIO_ENCODING_LINEAR_16\",\"sampleRateHertz\": \"8000\",\"synthesizeSpeechConfig\": {\"voice\": {\"ssmlGender\": \"SSML_VOICE_GENDER_FEMALE\"}}}}";
				}
			}

			JSONObject jsonObject = new JSONObject(AppPros.DialogflowDetails);
			String ProjectID = jsonObject.getJSONObject("BUnit").getJSONArray("B1").getJSONObject(0)
					.getString("ProjectID").toString();
			String Version = jsonObject.getJSONObject("BUnit").getJSONArray("B1").getJSONObject(0).getString("Version")
					.toString();
			String weburl = "";
			if (Version.equals("beta")) {

				weburl = "https://dialogflow.googleapis.com/v2beta1/projects/" + ProjectID + "/agent/sessions/"
						+ session + ":detectIntent";
			}
			if (Version.equals("prod")) {
				weburl = "https://dialogflow.googleapis.com/v2/projects/" + ProjectID + "/agent/sessions/" + session
						+ ":detectIntent";
			}
			logger.trace("Google ASR URL :: " + weburl);
			java.net.URL url = new java.net.URL(weburl);
			java.net.HttpURLConnection connjava = (java.net.HttpURLConnection) url.openConnection();
			connjava.setRequestMethod("POST");
			connjava.setRequestProperty("Authorization", "Bearer " + token);
			connjava.setRequestProperty("Content-Type", "application/json");
			connjava.setDoInput(true);
			connjava.setDoOutput(true);
			connjava.setAllowUserInteraction(true);
			java.io.DataOutputStream dos = new java.io.DataOutputStream(connjava.getOutputStream());
			dos.writeBytes(jsonObj.toString());
			dos.flush();
			dos.close();
			int respcode = connjava.getResponseCode();
			responseData.put("ResponseCode", Integer.toString(respcode));
			if (connjava.getResponseCode() != java.net.HttpURLConnection.HTTP_OK) {
				logger.info("********* Unable to connect to the URL ********* " + connjava.getResponseMessage());
				responseData.put("Response", connjava.getResponseMessage());
			} else {
				logger.info("********* Connected *********\n");
				java.io.BufferedReader in = new java.io.BufferedReader(
						new java.io.InputStreamReader(connjava.getInputStream()));
				StringBuilder sbout = new StringBuilder();
				String line;
				while ((line = in.readLine()) != null) {
					sbout.append(line);
				}
				in.close();
				JSONObject newString = new JSONObject(sbout.toString());
				if (newString.length() == 0) {
					logger.info("Oops ..! Google can not convert the wav file");
					responseData.put("Response", "Error");

				} else {
					responseData.put("Response", newString);
				}
			}
		}

		catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}
		return responseData;
	}

	/**
	 * @method Audio-Output
	 * @param token
	 * @param parameter
	 * @param session
	 * @param inFile
	 * @param outFile
	 * @return JSONObject
	 * @throws JSONException
	 * 
	 */

	public void convertULawFileToWavTo256KBPS(File inputFileName, File OutFileName) {
		try {

			AudioInputStream sourceaudio = AudioSystem.getAudioInputStream(inputFileName);
			AudioFormat sourceFormat = sourceaudio.getFormat();
			AudioFormat targetformat = new AudioFormat(new AudioFormat.Encoding("PCM_SIGNED"), 16000, 16,
					sourceFormat.getChannels(), sourceFormat.getChannels() * 2, 16000, false);
			AudioFileFormat.Type targettype = AudioFileFormat.Type.WAVE;
			AudioInputStream targetaudiostream = AudioSystem.getAudioInputStream(targetformat, sourceaudio);
			AudioSystem.write(targetaudiostream, targettype, OutFileName);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	public void convertULawFileToWavTo128KBPS(File inputFileName, File OutFileName) {
		try {
			AudioInputStream sourceaudio = AudioSystem.getAudioInputStream(inputFileName);
			AudioFormat sourceFormat = sourceaudio.getFormat();
			AudioFormat targetformat = new AudioFormat(new AudioFormat.Encoding("PCM_SIGNED"), 8000, 16,
					sourceFormat.getChannels(), sourceFormat.getChannels() * 2, 8000, false);
			AudioFileFormat.Type targettype = AudioFileFormat.Type.WAVE;
			AudioInputStream targetaudiostream = AudioSystem.getAudioInputStream(targetformat, sourceaudio);
			AudioSystem.write(targetaudiostream, targettype, OutFileName);

		} catch (EOFException e) {
			logger.error(e.getMessage(), e);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

}
