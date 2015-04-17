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

import java.io.IOException;
import java.util.Arrays;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.MemoryDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Setting;
import com.google.api.services.plus.Plus;
import com.google.api.services.plus.PlusScopes;
import com.google.api.services.plus.model.Person;
import com.google.api.services.plus.model.Person.Emails;

/*
 These links were useful while putting this together:
 
  https://developers.google.com/google-apps/calendar/instantiate
  https://code.google.com/p/google-api-java-client/source/browse/calendar-cmdline-sample/src/main/java/com/google/api/services/samples/calendar/cmdline/CalendarSample.java?repo=samples
  https://code.google.com/p/google-api-java-client/
  https://developers.google.com/api-client-library/java/apis/calendar/v3
  https://code.google.com/p/google-oauth-java-client/wiki/OAuth2
  http://javadoc.google-oauth-java-client.googlecode.com/hg/1.11.0-beta/index.html?com/google/api/client/extensions/servlet/auth/oauth2/AbstractAuthorizationCodeServlet.html
  https://developers.google.com/+/api/oauth#email
*/

/**
 * Handles all interactions with libraries for google plus and calendar
 * 
 * NOTE: Reference Google's docs for details about their libraries.
 */
public class CalendarUtility {
	private static final Logger logger = Logger.getLogger(CalendarUtility.class);
	private static final ResourceBundle resource = ResourceBundle.getBundle(CalendarUtility.class.getName());
	
	private static final String CALENDAR_NAME = "Chain of Learning";
	private static final String USER_EMAIL_SCOPE = "email"; // https://developers.google.com/+/api/oauth#email
			
	private static final DataStoreFactory DATA_STORE_FACTORY = new MemoryDataStoreFactory();
	private static final String APP_NAME = resource.getString("app.name");
	private static final String CLIENT_ID = resource.getString("app.client.id");
	private static final String CLIENT_SECRET = resource.getString("app.client.secret");
	private static final JacksonFactory JACKSON_FACTORY = JacksonFactory.getDefaultInstance();
	private static final NetHttpTransport HTTP_TRANSPORT = new NetHttpTransport();
	private static GoogleAuthorizationCodeFlow flow;
		
	/**
	 * Initializes configuration required by the google auth api and servlets
	 * 
	 * @return	configuration for google auth api and servlets
	 * @throws IOException
	 */
	static AuthorizationCodeFlow initializeFlow() throws IOException {
		if(flow==null) {		
		    flow = new GoogleAuthorizationCodeFlow.Builder(
		    			HTTP_TRANSPORT, JACKSON_FACTORY, CLIENT_ID, CLIENT_SECRET,
		    			Arrays.asList(CalendarScopes.CALENDAR,  PlusScopes.PLUS_ME, USER_EMAIL_SCOPE))
						    .setDataStoreFactory(DATA_STORE_FACTORY)
						    .setAccessType("offline")
						    // Seems that refreshing access without a prompt can result in no refresh token, and we should prompt anyway for our scenario.
						    // http://stackoverflow.com/questions/13777842/how-to-get-offline-token-and-refresh-token-and-auto-refresh-access-to-google-api
						    .setApprovalPrompt("force") 
						    .build();
		}
		
	    return flow;
	}
	
	/**
	 * Creates client to interact with a specific user's calendar	
	 * 
	 * @param credential	Credential provided by users's authorization
	 * @return	Calendar client for credential. Otherwise, returns null
	 * @throws IOException
	 */
	static Calendar createCalendarClient(Credential credential) throws IOException {
		return new Calendar.Builder(HTTP_TRANSPORT, JACKSON_FACTORY, credential)
							.setApplicationName(APP_NAME)
							.build();
	}
	
	/**
	 * Verifies the credential belongs to the email address we think
	 * 
	 * @param credential	Authorized user credential
	 * @param emailAddress	Email address to verify 
	 * @return	Boolean indicating if user credential belongs to email address
	 * @throws IOException
	 */
	static boolean verifyEmailAddress(Credential credential, String emailAddress) throws IOException {
		Plus client = new Plus.Builder(HTTP_TRANSPORT, JACKSON_FACTORY, credential)
							.setApplicationName(APP_NAME)
							.build();
		
		Person person = client.people().get("me").execute();
		
		if(person==null) {
			return false;
		}
		
		for(Emails email : person.getEmails()) {
			if(emailAddress.equalsIgnoreCase(email.getValue())) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Removes the given credential belonging to this user
	 * 
	 * @param emailAddress	Email address which the credential belongs
	 * @param credential	Credential authorized for the email address
	 * @throws IOException
	 */
	static void forgetUserCredential(String emailAddress, Credential credential) throws IOException {
		if(logger.isDebugEnabled()) {
			logger.info("DELETING CREDENTIAL FOR: " + emailAddress);
		}
		
		flow.getCredentialDataStore().delete(emailAddress);
	}
	
	/**
	 * Creates client to interact with a specific user's calendar	
	 * 
	 * @param userId	User id for the calendar	
	 * @return	Calendar client if user id is known. Otherwise, returns null
	 * @throws IOException
	 */
	static Calendar createCalendarClient(String userId) throws IOException {
		Credential credential = initializeFlow().loadCredential(userId);
		
		if(credential==null) {
			return null;
		}
		
		return createCalendarClient(credential);
	}
	
	/**
	 * Returns our specific calendar model associated with this user 
	 * 
	 * @param client	User's calendar client
	 * @return	Calendar used by this application if it exist. Otherwise, null is returned. 
	 * @throws IOException
	 */
	static com.google.api.services.calendar.model.Calendar getCalendar(Calendar client) throws IOException {
		com.google.api.services.calendar.model.Calendar calendar = null;
		
		// TODO - associate calendar id to user id. (Allows user to change name)
		CalendarList calendars = client.calendarList().list().execute();
		for(CalendarListEntry c : calendars.getItems()) {
			if(CALENDAR_NAME.equals(c.getSummary())) {
				if(logger.isDebugEnabled()) {
					logger.debug("FOUND CALENDAR: " + c.getId());
				}
				calendar = client.calendars().get(c.getId()).execute();
				break;
			}
		}
		
		return calendar;
	}
	
	/**
	 * Creates the secondary calendar our application will use
	 * 
	 * @param client	The user's client to use for creating the calendar
	 * @return	Model of the newly created secondary calendar
	 * @throws IOException
	 */
	static com.google.api.services.calendar.model.Calendar createCalendar(Calendar client) throws IOException {
		
		Setting tzSetting = client.settings().get("timezone").execute();
		String timezone = tzSetting.getValue();
		
		com.google.api.services.calendar.model.Calendar calendar =  
				new com.google.api.services.calendar.model.Calendar();
		calendar.setSummary(CALENDAR_NAME);
		
		if(logger.isDebugEnabled()) {
			logger.debug("CREATING CALENDAR: " + CALENDAR_NAME);
		}
		calendar.setTimeZone(timezone);
		calendar = client.calendars().insert(calendar).execute();
		
		return calendar;
	}
	
	
}
