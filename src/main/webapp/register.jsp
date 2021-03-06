<!--

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

-->

<html>
	<head>
		<title>Register</title>
		<link rel="stylesheet" href="css/main.css">
		<script>
		 function validate() { return document.forms.register.email.value.match(/.+@gmail.com$/) ? true : false; }
		</script>
	</head>
	<body>
	 	<div id="popup-area">
	 		<h2>Register</h2>
	 		<div>Enter your school provided gmail address below to create a secondary google calendar for tracking all the actions you take in LearningStudio.</div>
	 		<br />
	 		<div>
	 			<form name="register" method="post" action="./register" onsubmit="return validate()">
	 				<input type="text" name="email" placeholder="you@gmail.com" size="30" />
	 				<input id="register-button" class="button" type="submit" value="Link to Calendar" />
	 			</form>
	 		</div>
 		</div>
	</body>
</html>