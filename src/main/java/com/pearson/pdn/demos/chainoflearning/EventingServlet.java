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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

public class EventingServlet extends HttpServlet {
	private static final Logger logger = Logger.getLogger(EventingServlet.class);

	private static String subscriptionId = null;

	@Override
	public void init() throws ServletException {
		super.init();
 
		// Create a subscription at application startup if one doesn't already exist
		try {
			EventingUtility.init();
			
			subscriptionId = EventingUtility.findSubscription();
			
			if(subscriptionId==null) {
				
				subscriptionId = EventingUtility.subscribe();

				if (subscriptionId==null) {
					throw new RuntimeException("Failed to create a subscription");
				}
				
				logger.info("CREATED SUB: "+subscriptionId);
			}
			else {
				logger.info("FOUND SUB: "+subscriptionId);
			}
		} 
		catch (Throwable e) {
			logger.error("FAILED TO CREATE SUBSCRIPTION",e);
		}
		
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
		
		try {
			
			// extract parameters from eventing message delivery
			String messageType=req.getParameter("MESSAGE-TYPE");
			String messageId=req.getParameter("MESSAGE-ID");
			String payloadContentType=req.getParameter("PAYLOAD-CONTENT-TYPE");
			String payload=req.getParameter("PAYLOAD");
			// Deliveries include these instead of an Authorization header
			String authorization=req.getParameter("AUTHORIZATION");
			String authDelimiter=req.getParameter("AUTHORIZATION-DELIMITER");

			// validate the message was signed with this principal's credentials
			try {
				// concatenate original data in this order to validate
				String authData = messageId + messageType + payloadContentType + payload;
				EventingUtility.verifyAuthToken(authorization,authDelimiter, authData);
			}
			catch(Throwable e) {
				logger.error("VERIFICATION FAILED ON MESSAGE: "+messageId);
				return;
			}
			
			// eventing's SLA allows redelivery of the same message for 72 hours.
			// recognizing the MESSAGE-ID during that time is necessary to prevent side effects
			// TODO - check if this message-id has been processed. if so, abort. if not, record the message-id
			
			if(logger.isDebugEnabled()) {
				logger.debug("RECEIVED MESSAGE: "+payload);
			}
			
			// Begin event processing logic
			// NOTE: Eventing expects a response within 10 seconds.
			//       Persist the event if possible and process later.
			//       This example will just process asynchronously 
			new Thread(new EventProcessor(payload)).start();
		}
		catch(Throwable e) {
			// NOTE: Applications wanting the message re-delivered during an error condition should
			//       return with a non-200 status code. See the docs for a list of accepted error codes.
			//       This example application will just log the error and respond normally with a 200.
			logger.warn("CALLBACK FAILURE: " + e.getMessage(),e);
		}
	}
	
	public void destroy() {
		super.destroy();
		
		// delete the existing subscription at shutdown to prevent delivery failures while offline
		try {	
			if(subscriptionId!=null) {
				EventingUtility.unsubscribe(subscriptionId);
				logger.info("DELETED SUB: "+subscriptionId);
			}
		} 
		catch (Throwable e) {
			logger.warn("FAILED TO DELETE SUB: "+subscriptionId,e);
		}
	}	

}
