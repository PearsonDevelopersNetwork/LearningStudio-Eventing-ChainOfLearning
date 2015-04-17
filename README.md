# Chain-of-Learning

## App Overview

This sample application demonstrates [LearningStudio's Eventing capabilities](http://developer.pearson.com/learningstudio/about) by enabling LearningStudio users to self monitor their course activity on their Google calendar. Users opt-in by registering the Google email address associated with their LearningStudio account. A user's activity in each course will create an all-day event that can be extended to the next day through continued course activity. Consecutive daily activity forms a chain that continues to grow until participation stops. Students gain accountability and motivation from this visual reminder of their persistence in course participation. Teachers gain a reminder of their involvement in each class they manage.

This application's intent is to highlight the potential of combining [LearningStudio's Eventing capabilities](http://developer.pearson.com/learningstudio/about) with third party APIs. The LearningStudio APIs could be used to further enrich such an application. Best practices for subscription management and callback processing for Eventing can be found in the source code. 

### Scope of Functionality 

This sample app is intended for demonstration purposes, so we made it easy for you to test drive it. It doesn't require you to setup a database or have an email server. In fact, it uses an in-memory store for persistence and never actually sends an email. You would not be able to deploy this in a production setting without making modifications. The good news is you can experience this application's potential quickly. Continue reading to learn how!

## Prerequisites

### Build Environment 

  * Apache Maven should be installed and configured.
  * Java 6 or greater is required.

### Server Environment 

  * This application assumes you're running an application server (i.e. Tomcat). 
  * This application requires Java 6 or greater.

## Installation

### Application Configuration

#### LearningStudio Eventing Setup

  1. [Get credentials](http://developer.pearson.com/learningstudio/set-1) for Eventing
  
#### Google App Setup

  1. Create an application in the Google Developer Console
  2. Enable the Calendar and Google+ API
  3. Create a Client ID for this web application
  4. Set the redirect url to https://yourserver.com/chain-of-learning/oauth2callback
 
#### Application Setup

**src/main/resources/com/pearson/pdn/demos/chainoflearning/CalendarUtility.properties**

~~~~~~~~~~~~~~
app.name={Name of the Google App}
app.client.id={Google Client ID}
app.client.secret={Google Client Secret}
~~~~~~~~~~~~~~

**src/main/resources/com/pearson/pdn/demos/chainoflearning/EventingUtility.properties**

~~~~~~~~~~~~~~
eventing.principal.id={LearningStudio Eventing ID}
eventing.principal.key= {LearningStudio Eventing Secret}
eventing.server.url={LearningStudio Eventing Server}
eventing.subscribe.message.callback.url={Application Callback URL}
eventing.subscribe.tag#.key={Name for Subscription Filter}
eventing.subscribe.tag#.value= {Value for Subscription Filter}
~~~~~~~~~~~~~~

*Callback URL:* https://yourserver.com/chain-of-learning/eventing

*Common Tag Names:* MessageType, CourseId 

 
### Server Deployment

#### Build

Run `mvn clean package` to compile the application and assemble a war file.

#### Server 

Simply copy the `target/chain-of-learning.war` file to your server. You should be able to access this application from an address like: 

http://yourserver.com/chain-of-learning

## License

Copyright (c) 2015 Pearson Education Inc.
Created by Pearson Developer Services

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Portions of this work are reproduced from work created and 
shared by Google and used according to the terms described in 
the License. Google is not otherwise affiliated with the 
development of this work.
