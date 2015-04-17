/*
* LearningStudio Eventing Sample Application - Chain-of-Learning 
* 
* Need Help or Have Questions? 
* Please use the PDN Developer Community at https://community.pdn.pearson.com
*
* @category   LearningStudio Eventing Sample Application - Chain-of-Learning 
* @author     Wes Williams <wes.williams@pearson.com>
* @author     Pearson Developer Services Team <apisupport@pearson.com>
* @copyright  2015 Pearson Education Inc.
* @license    http://www.apache.org/licenses/LICENSE-2.0  Apache 2.0
* @version    1.0
* 
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
*     http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
* 
* Portions of this work are reproduced from work created and 
* shared by Google and used according to the terms described in 
* the License. Google is not otherwise affiliated with the 
* development of this work.
*/

package com.pearson.pdn.demos.chainoflearning;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.TimeZone;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.crypto.engines.AESFastEngine;
import org.bouncycastle.crypto.macs.CMac;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.util.encoders.Hex;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Utility to handle all Eventing interactions
 */
public class EventingUtility {
	private static final Logger logger = Logger.getLogger(EventingUtility.class);
	private static final ResourceBundle resource = ResourceBundle.getBundle(EventingUtility.class.getName());

	private static final String BASE_URL = resource.getString("eventing.server.url");
	private static final String PRINCIPAL_ID = resource.getString("eventing.principal.id");
	private static final String PRINCIPAL_KEY = resource.getString("eventing.principal.key");
	private static final String CALLBACK_URL = resource.getString("eventing.subscribe.message.callback.url");
	private static final String TAGS = getTagString();

	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
	static {
		DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
	}
	
	private static HttpClient client;
	
	static void init() {
		if(client==null) {
			PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
			cm.setDefaultMaxPerRoute(50);
			cm.setMaxTotal(100);
			client = HttpClientBuilder.create().setConnectionManager(cm).build();
		}
	}

	/**
	 * Finds an existing subscription to this callback url
	 * 
	 * @return	Subscription Id
	 * @throws Exception	Thrown when operation fails
	 */
	static String findSubscription() throws Exception {
		// get current time in UTC for auth token
		String strTime = DATE_FORMAT.format(new Date());
		// use the principal id as the payload for auth token
		String authData = PRINCIPAL_ID;
		// generate the token using the default pipe delimiter
		String authToken = generateAuthToken(strTime, "|", PRINCIPAL_ID, PRINCIPAL_KEY, authData);

		// do a GET on /v1/subscriptions/principal/{principalId}
		HttpGet get = new HttpGet(BASE_URL + "/v1/subscriptions/principal/" + PRINCIPAL_ID);
		// use the auth token in the Authorization header
		get.setHeader("Authorization", authToken);
		HttpResponse response = client.execute(get);

		String responseBody = null;
		HttpEntity responseEntity = response.getEntity();
		if (responseEntity != null) {
			responseBody = new String(EntityUtils.toString(responseEntity));
		}

		// expect a 200 status if the call was executed successfully
		int responseCode = response.getStatusLine().getStatusCode();
		if(responseCode!=200) {
			throw new RuntimeException("Unexpected response code: "+responseCode);
		}

		if(logger.isDebugEnabled()) {
			logger.debug("FOUND SUBS: " +responseBody);
		}
		// {"subscriptions":[
		//     {"subscription":{"id":"","principal_id":"","callback_url":"",
		//      "wsdl_uri":"","queue_name":"","date_created":"","date_cancelled":"",
		//      "tags":[{"tag":{"id":"","type":"","value":""}}]
		//     }
		//   ]
		// }
		
		// search through the existing subscriptions to find subscription id with our callback url
		JsonParser jsonParser = new JsonParser();
		JsonElement json = jsonParser.parse(responseBody);
		JsonArray subscriptions = json.getAsJsonObject().get("subscriptions").getAsJsonArray();
		for(int i=0; i<subscriptions.size();i++) {
			JsonObject subscription = subscriptions.get(i).getAsJsonObject().get("subscription").getAsJsonObject();
			if(CALLBACK_URL.equals(subscription.get("callback_url").getAsString())) {
				return subscription.get("id").getAsString();
			}
		}
		
		return null;
	}
	
	/**
	 * Creates a subscription for this callback url
	 * 
	 * @return	ID of the created subscription
	 * @throws Exception	Thrown when operation fails
	 */
	static String subscribe() throws Exception {
		// get current time in UTC for auth token
		String dateTime = DATE_FORMAT.format(new Date());
		// Include all non-empty values in payload for auth token
		StringBuilder authDataBuilder = new StringBuilder();
		authDataBuilder.append(CALLBACK_URL);
		authDataBuilder.append(TAGS);

		// generate the token using the default pipe delimiter
		String authToken = generateAuthToken(dateTime, "|", PRINCIPAL_ID, PRINCIPAL_KEY, authDataBuilder.toString());

		List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("CALLBACK-URL", CALLBACK_URL));
		nvps.add(new BasicNameValuePair("WSDL-URI", "")); // N/A
		nvps.add(new BasicNameValuePair("TAGS", TAGS));
		// the following parameters were declared in TAGS if needed
		nvps.add(new BasicNameValuePair("CLIENT", ""));
		nvps.add(new BasicNameValuePair("CLIENT-STRING", ""));
		nvps.add(new BasicNameValuePair("MESSAGE-TYPE", ""));
		nvps.add(new BasicNameValuePair("SYSTEM", ""));
		nvps.add(new BasicNameValuePair("SUB-SYSTEM", ""));
		
		// do a POST on /v1/subscription
		HttpPost post = new HttpPost(BASE_URL + "/v1/subscription");
		// use the auth token in the Authorization header
		post.setHeader("Authorization", authToken);
		// include all parameters for subscription
		post.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
		HttpResponse response = client.execute(post);

		String responseBody = null;
		HttpEntity responseEntity = response.getEntity();
		if (responseEntity != null) {
			responseBody = new String(EntityUtils.toString(responseEntity));
		}

		// expect a 200 status if the call was executed successfully
		int responseCode = response.getStatusLine().getStatusCode();
		if(responseCode!=200) {
			throw new Exception("Unexpected response code of "+responseCode+" and body: " + responseBody + "");
		}

		if(logger.isDebugEnabled()) {
			logger.debug("NEW SUB: " +responseBody);
		}
		// {"subscription":{
		//    "id":"","principal_id":"","callback_url":"","wsdl_uri":"","queue_name":"",
		//    "date_created":"","date_cancelled":"",
		//    "tags":[{"tag":{"id":"","type":"","value":"" }}]
		//   }
		// }
		
		// find the subscription id
		JsonParser jsonParser = new JsonParser();
		JsonElement json = jsonParser.parse(responseBody);
		JsonObject subscription = json.getAsJsonObject().get("subscription").getAsJsonObject();
		
		if(subscription==null) {
			throw new Exception("Unable to find subscription id in response body: "+ responseBody);
		}
		
		return subscription.get("id").getAsString();
	}
	
	/**
	 * Deletes the provided subscription
	 * 
	 * @param subscriptionId	ID of subscription to be deleted
	 * @throws Exception	Thrown when operation fails
	 */
	static void unsubscribe(String subscriptionId) throws Exception {
		// get current time in UTC for auth token
		String strTime = DATE_FORMAT.format(new Date());
		// use the principal id as the payload for auth token
		String authData = subscriptionId;
		// generate the token using the default pipe delimiter
		String authToken = generateAuthToken(strTime, "|", PRINCIPAL_ID, PRINCIPAL_KEY, authData);

		// do a DELETE on /v1/subscription/{subscriptionId}
		HttpDelete delete = new HttpDelete(BASE_URL + "/v1/subscription/" + subscriptionId);
		delete.setHeader("Authorization", authToken);
		HttpResponse response = client.execute(delete);
		
		// expect a 200 status if the call was executed successfully
		int responseCode = response.getStatusLine().getStatusCode();
		if(responseCode!=200) {
			throw new Exception("Unexpected repsponse code of " + responseCode);
		}
	}
	
	/**
	 * Verifies an auth token received during delivery is valid.
	 * The token would have been signed with our secret, so resign the
	 * original data to see if the token matches.
	 * 
	 * @param token			Auth token to be verify
	 * @param delimiter		Delimiter used in the auth token
	 * @param payload		The data received with the auth token
	 * @throws Exception	Thrown when verification fails
	 */
	static void verifyAuthToken(String token, String delimiter, String payload) throws Exception
	{
		String splitDelimiter = ("|".equals(delimiter)) ? "\\|" : delimiter;

		String[] tokenParts = token.split(splitDelimiter);

		if (tokenParts.length != 3) {
		    throw new Exception("Authorization token not 3 parts: "+ token);
		}

		String tokenPrincipal = tokenParts[0];
		String tokenTimestamp = tokenParts[1];
		String tokenMac = tokenParts[2];

		if (!PRINCIPAL_ID.equals(tokenPrincipal)) {
		    throw new Exception("Principal Mismatch: " + PRINCIPAL_ID + " != " + tokenPrincipal);
		}

		String generatedMac = generateAuthToken(tokenTimestamp, delimiter, 
												PRINCIPAL_ID, PRINCIPAL_KEY, payload);

		if (!generatedMac.equals(token)) {
		    throw new Exception("Generated MAC does not match input MAC " + tokenMac);
		}
	}
	
	/**
	 * Creates an auth token signed with principal's credentials
	 * 
	 * @param dateTime	utc time in yyyy-MM-dd'T'HH:mm:ssZ format 
	 * @param delimiter	Delimiter to between token parts
	 * @param principalId	Id portion of credentials
	 * @param key	Secret portion of credentials
	 * @param payload	Data being signed
	 * @return	Token value
	 * @throws UnsupportedEncodingException
	 */
	private static String generateAuthToken(String dateTime, String delimiter,
		    String principalId, String key, String payload) throws UnsupportedEncodingException
	{
		// prepare the data
		byte[] macInput = (dateTime+payload).getBytes("UTF-8");
		byte[] macOutput = new byte[16];
		// sign the data
		CMac macProvider = new CMac(new AESFastEngine(), 128);
		macProvider.init(new KeyParameter(key.getBytes()));
		macProvider.update(macInput, 0, macInput.length);
		macProvider.doFinal(macOutput, 0);
		// hex the signed data
		String macOutputHex = new String(Hex.encode(macOutput));
		// concatenate the token parts
		return principalId + delimiter + dateTime + delimiter + macOutputHex;
	}
	
	/**
	 * Reads tags from properties file and format correctly
	 * 
	 * @return	Tags in the correct format
	 */
	private static String getTagString() {
		Map<String,String> tags = new HashMap<String,String>();
		// loop through properties sequentially
		int tagCount=0;
		while(true) {
			tagCount++;
			String tagKey = null;
			try {
				tagKey = resource.getString("eventing.subscribe.message.tag"+tagCount+".key");
			}
			catch(MissingResourceException e) {
				break;
			}
			
			String tagValue = resource.getString("eventing.subscribe.message.tag"+tagCount+".value");	
			tags.put(tagKey, tagValue);
		}
		// separate the key and value with colons and the pairs with commas
		String delimiter="";
		StringBuilder tagsBuilder = new StringBuilder();
		for(String tagKey : tags.keySet()) {
			tagsBuilder.append(delimiter).append(tagKey).append(":").append(tags.get(tagKey));
			delimiter = ",";
		}
		
		return tagsBuilder.toString();
	}
}
