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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.AuthorizationCodeResponseUrl;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.servlet.auth.oauth2.AbstractAuthorizationCodeCallbackServlet;
import com.google.api.client.http.GenericUrl;
import com.google.api.services.calendar.model.Calendar;

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
 * Handles auth with google using a callback servlet they provide as a quickstart
 * 
 * NOTE: Reference Google's docs for details about their libraries.
 */
public class CalendarCallbackServlet extends AbstractAuthorizationCodeCallbackServlet {

	  @Override
	  protected void onSuccess(HttpServletRequest req, HttpServletResponse res, Credential credential)
	      throws ServletException, IOException {

		  	Calendar calendar = null;
		  	String emailAddress = getUserId(req);
		  
		    if(CalendarUtility.verifyEmailAddress(credential, emailAddress)) {	    
				com.google.api.services.calendar.Calendar client = CalendarUtility.createCalendarClient(credential);
				calendar = CalendarUtility.getCalendar(client);
				
				if(calendar==null) {
					 calendar = CalendarUtility.createCalendar(client);
				}
		    }
		    else {
		    	CalendarUtility.forgetUserCredential(emailAddress, credential);
		    }
			
			if(calendar==null) {
				res.sendRedirect(req.getContextPath()+"/failure.jsp");
			}
			else {
				req.getSession().setAttribute("cal", calendar.getId());
				res.sendRedirect(req.getContextPath()+"/success.jsp");
			}
	  }

	  @Override
	  protected void onError(
	      HttpServletRequest req, HttpServletResponse resp, AuthorizationCodeResponseUrl errorResponse)
	      throws ServletException, IOException {
	    // handle error
		  resp.sendRedirect(req.getContextPath()+"/failure.jsp");
	  }

	  @Override
	  protected String getRedirectUri(HttpServletRequest req) throws ServletException, IOException {
	    GenericUrl url = new GenericUrl(req.getRequestURL().toString());
	    url.setRawPath(req.getContextPath()+"/oauth2callback");
	    return url.build();
	  }

	  @Override
	  protected AuthorizationCodeFlow initializeFlow() throws IOException {
		  return CalendarUtility.initializeFlow();
	  }

	  @Override
	  protected String getUserId(HttpServletRequest req) throws ServletException, IOException {
		  // return user ID
		  String email = null;
		  HttpSession session = req.getSession();
		  if(session!=null) {
			  email = (String) session.getAttribute("email");
		  }
		  
		  return email;
	  }
	}