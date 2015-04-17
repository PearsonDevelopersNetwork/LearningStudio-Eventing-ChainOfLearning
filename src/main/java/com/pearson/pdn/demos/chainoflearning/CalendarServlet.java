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
import com.google.api.client.extensions.servlet.auth.oauth2.AbstractAuthorizationCodeServlet;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;

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
 * Handles auth with google using a servlet they provide as a quickstart
 * 
 * NOTE: Reference Google's docs for details about their libraries.
 */
public class CalendarServlet extends AbstractAuthorizationCodeServlet {
	  
  	  @Override
	  protected void doGet(HttpServletRequest req, HttpServletResponse res)
	      throws IOException, ServletException {
		 
  		String email = (String) req.getSession().getAttribute("email");
  		// save the email address to short-circuit getUserId later
  		req.setAttribute("email", email);
  		// Will only get here if someone is attempting to re-register.
  		// The credential exist in storage, but the user might still have unauthorized the app
  		// forget the user in storage
  		CalendarUtility.forgetUserCredential(email, this.getCredential());
  		// then call this servlet again to force auth again
  		req.getRequestDispatcher("/oauth2").forward(req, res);
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
		  // account for a forced re-register from doGet
		  if(req.getAttribute("email")!=null) {
			  return (String) req.getAttribute("email");
		  }
		  
		  // return user ID
		  String email = req.getParameter("e");
		  String verifyCode = req.getParameter("v");
		  
		  if( email !=null && verifyCode != null) {
			  // TODO - do this better. auth will not be in the session
			  HttpSession session = req.getSession();
			  if(session!=null) {
				  String auth = (String) session.getAttribute("auth");
				  
				  String authMatch = Base64.encodeBase64String((email+":"+verifyCode).getBytes());
				  if(auth.equals(authMatch)) {
					  session.removeAttribute("auth");
					  session.setAttribute("email", email);
					  return email;
				  }
			  }
		  }  
		  
		  return null;
	  }
	}
