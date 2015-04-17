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
import javax.servlet.http.HttpSession;

import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;

/**
 * This servlet provides the the following functionality:
 *   Accepts an email address for the registration process.
 *   Should verify the domain matches what is expected. 
 *   Sends an email to verify the user's identity. 
 */
public class RegisterServlet extends HttpServlet {

	// Email matching rules. Substitute your pattern here.
	private final static String EMAIL_PATTERN = "^[a-zA-Z0-9_.-]+@gmail\\.com$";
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {

		String email = req.getParameter("email");
		if(email==null || !email.matches(EMAIL_PATTERN)) {
			res.sendRedirect(req.getContextPath()+"/register.jsp");
			return;
		}
		
		HttpSession session = req.getSession(true);
		
		// TODO - do this better. actually persist it...
		String verifyCode = Base64.encodeBase64String(String.valueOf(System.currentTimeMillis()).getBytes());
		session.setAttribute("auth",Base64.encodeBase64String((email+":"+verifyCode).getBytes()));
		
		
		// TODO - actually send an email
		String linkParams = "?e="+email+"&v="+verifyCode;
		String emailContent = "Hello " + email + ",<br /><br /><a id=\"email-link\" href=\"./oauth2"+linkParams+"\">Link</a> your calendar with ChainOfLearning. <br/ > <br /> Thanks!";
		req.setAttribute("emailContent", emailContent);
		
		req.setAttribute("email", email);
		req.getRequestDispatcher("/activation.jsp").forward(req, res);
	}
}
