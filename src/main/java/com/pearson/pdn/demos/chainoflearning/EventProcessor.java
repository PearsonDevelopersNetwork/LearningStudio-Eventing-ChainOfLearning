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
* 
*/

package com.pearson.pdn.demos.chainoflearning;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.apache.log4j.Logger;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Process the event payload in a separate thread to prevent delaying the ack to Eventing.
 * This operation should finish quickly, but it's dependent on 3rd party APIs and the network.
 */
public class EventProcessor implements Runnable {
	private static final Logger logger = Logger.getLogger(EventProcessor.class);
	private String payload;
	
	public EventProcessor(String payload) {
		this.payload = payload;
	}
	
	public void run() {
		try {
			// parse the payload	
			JsonParser jsonParser = new JsonParser();
			JsonObject json = jsonParser.parse(payload).getAsJsonObject();
			
			// extract the user info for this event
			JsonObject actor = json.get("actor").getAsJsonObject();
			String emailAddress = actor.get("emailAddress").getAsString();
			
			if(logger.isDebugEnabled()) {
				logger.debug("EVENT FOR USER: "+emailAddress);
			}
			
			// find the handle to the user's calendar
			com.google.api.services.calendar.Calendar client = CalendarUtility.createCalendarClient(emailAddress);
	
			// verify a handle to the user's calendar exist
			if (client == null) {
				if(logger.isDebugEnabled()) {
					logger.debug("USER NOT REGISTERED: " + emailAddress);
				}
				return; // quit if calendar not connected for this user
			} 
	
			// extract when and what event happened
			String eventDateValue = json.get("eventDate").getAsString();
			String eventTypeValue = json.get("eventType").getAsString();
			
			if(logger.isDebugEnabled()) {
				logger.debug("EVENT DETAILS: " + eventDateValue + " - " + eventTypeValue);
			}
			
			// extract the details of the course for this event
			JsonObject contexts = json.get("contexts").getAsJsonObject();
			JsonObject course = contexts.get("course").getAsJsonObject();
			String courseTitle = course.get("title").getAsString();
			String displayCourseCode = course.get("displayCourseCode").getAsString();
			final String eventTitle = displayCourseCode + " - " + courseTitle;
			
			// get the calendar which the event will be recorded
			Calendar calendar = null;
			try {
				calendar = CalendarUtility.getCalendar(client);
				
				if(calendar==null) {
					// create calendar if it's missing
					calendar = CalendarUtility.createCalendar(client);
				}
			}
			catch(GoogleJsonResponseException ex) {
				if(ex.getDetails().getCode()==401) { // user revoked access
					if(logger.isDebugEnabled()) {
						logger.debug("CALENDAR ACCESS DENIED FOR USER: " + emailAddress);
					}
					CalendarUtility.forgetUserCredential(emailAddress, null);
				}
				else { // maybe the api is down
					logger.warn("UNABLE TO ACCESS CALENDAR FOR USER: " + emailAddress, ex);
				}
				return; // quit if calendar not available for this user
			}
			catch(IOException ex) { // maybe the api is down
				logger.warn("UNABLE TO ACCESS CALENDAR FOR USER: " + emailAddress, ex);
				return; // quit if calendar not available for this user
			}
			
			if(logger.isDebugEnabled()) {
				logger.debug("CALENDAR DETAILS: "+calendar);
			}
			
			// convert the event date to a date type
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
			dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
			Date today = dateFormat.parse(eventDateValue);
			
			// convert time zone to match the calendar
			TimeZone timeZone = TimeZone.getTimeZone(calendar.getTimeZone());
			dateFormat.setTimeZone(timeZone);
			eventDateValue = dateFormat.format(today).substring(0,16); // date and time for description
			
			// remove the time from the event date
			// we will be working with all day events on the calendar
			dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			dateFormat.setTimeZone(timeZone);
			today = dateFormat.parse(dateFormat.format(today));
									
			// set search bounds for existing event
			java.util.Calendar cal = java.util.Calendar.getInstance();
			cal.setTimeInMillis(today.getTime());
			cal.setTimeZone(timeZone);
			cal.add(java.util.Calendar.DATE, 1);
			Date tomorrow = cal.getTime();
			cal.add(java.util.Calendar.DATE, -2);
			Date yesterday = cal.getTime();
			// convert search bounds to google date types
			DateTime timeMin = new DateTime(yesterday);  // lower bounds of end time (inclusive)
			// NOTE: Calendar API and UI are different on timeMax, so tomorrow in API is correct to end today in UI
			DateTime timeMax = new DateTime(tomorrow);  // upper bounds of start time (exclusive)
			
			// search for events that occurred yesterday or today
			Event eventChain = null;
			Events events = client.events().list(calendar.getId())
											.setTimeMax(timeMax) 
											.setTimeMin(timeMin)
											.execute();
			// look through results for one matching the current event
			List<Event> eventItems = events.getItems();
			for(Event event : eventItems) {
				if(eventTitle.equalsIgnoreCase(event.getSummary())) {
					eventChain = event;
					break;
				}
			}
	
			// prepare to record this event
			
			// create a start time - only applicable to new events
			EventDateTime startDateTime = new EventDateTime();
			startDateTime.setDate(new DateTime(dateFormat.format(today))); // Library Note: use string format for all day event
			startDateTime.setTimeZone(timeZone.getID());
			// create an end time
			EventDateTime endDateTime = new EventDateTime();
			endDateTime.setDate(new DateTime(dateFormat.format(tomorrow))); // Library Note: use string format for all day event
			endDateTime.setTimeZone(timeZone.getID());
			
			// google calendar event colors have numeric ids
			// this hack sets colors based on last # of courseId
			// this isn't perfect, but it's a start...
			String colorId = course.get("id").getAsString();
			colorId = colorId.substring(colorId.length()-1);
			colorId = String.valueOf(Integer.parseInt(colorId)+1); // 1-11
			
			// create or modify based on event existence
			if(eventChain==null) {	// create event when missing	
				eventChain = new Event();
				eventChain.setSummary(eventTitle);
				eventChain.setDescription(eventDateValue + " - " + eventTypeValue + "\n");
				eventChain.setStart(startDateTime);
				eventChain.setEnd(endDateTime);
				eventChain.setColorId(colorId);
				eventChain = client.events().insert(calendar.getId(), eventChain).execute();
				if(logger.isDebugEnabled()) {
					logger.debug("NEW EVENT CREATED: " + eventChain.getId());
				}
			}
			else { // update it when existing
				eventChain.setDescription(eventChain.getDescription() + eventDateValue + " - " + eventTypeValue + "\n");
				eventChain.setEnd(endDateTime);
				eventChain = client.events().update(calendar.getId(), eventChain.getId(), eventChain).execute();
				if(logger.isDebugEnabled()) {
					logger.debug("EXISTING EVENT UPDATED: " + eventChain.getId());
				}
			}
		}
		catch(Throwable t) {
			// The event is lost, because we already responded with a 200
			logger.warn("EVENT PROCESSING FAILURE: " + t.getMessage(),t);
		}
	}
}