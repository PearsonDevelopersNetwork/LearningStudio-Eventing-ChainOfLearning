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
		<title>Welcome</title>
		<link rel="stylesheet" href="css/main.css">
		<script>
			function showPopup() {
				var screenLeft=0;
				var screenTop=0;
			    if(typeof window.screenLeft !== 'undefined') {
			      screenLeft = window.screenLeft;
			      screenTop = window.screenTop;
			    } else if(typeof window.screenX !== 'undefined') {
			      screenLeft = window.screenX;
			      screenTop = window.screenY;
			    }
				var width = 500;
				var height = 520;
				var left = screenLeft + ((window.screen.width/2)-(width/2));
	  			var top = screenTop + ((window.screen.height/2)-(height/2));
				window.open('./register.jsp', '_blank', 'width='+width+',height='+height+',top='+top+',left='+left+',toolbar=no,location=no,menubar=no');
			}
		</script>
	</head>
	<body>
		<div id="landing-area">
			<a href="#" id="start-button" class="button" onclick="showPopup()">GET STARTED!</a>
 		</div>
	</body>
</html>