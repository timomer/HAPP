# Hackabetes Artificial Pancreas Project
**This code is highly experimental, all suggestions by the app must be reviewed**

Native Android Artificial Pancreas App with OpenAPS.org algorithm

* Most recent [Master apk](https://drive.google.com/open?id=0BxE8lMx4AjLieEpYSWU4NkpNbnc)
* [App Summary](https://sites.google.com/a/n-omer.co.uk/hackabetes/home/happ-overview)
* [Project blog](http://www.hypodiabetic.co.uk)
* [Wiki Documentation](https://github.com/timomer/HAPP/wiki)
* [Bug Reports and Feature Requests](https://github.com/timomer/HAPP/issues)
* Support and Feedback [![Gitter](https://badges.gitter.im/timomer/HAPP.svg)](https://gitter.im/timomer/HAPP?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

![](https://github.com/timomer/HAPP/blob/master/images/HAPP_Front_Page.png)
![](https://github.com/timomer/HAPP/blob/master/images/HAPP_WF.png)



Features:
* Native Open Loop system (requires user to review and enter data into pump)
* Capture your Bolus and Carb Treatments
* Bolus Wizard utilising IOB, COB and CGM data
* Ability to switch between OpenAPS.org Dev and Stable algorithms
* Notifications for Android and Android Wear
* Visuals showing Carb and IOB over time and Basal vs Temp Basal adjustments
* Integration with Nightscout via NSClient and Pump Driver Apps for full Closed Loop operation

App can be used to inform the user at a user set time interval of suggested Temp Basal \ Extended Bolus calculated by the OpenAPS.org algorithm, this can then be manually set on the users pump.
This allows a Diabetic with any pump to experiment with the OpenAPS algorithm.

Requirements:
* Android 5.0+
* xDrip \ xDrip+, Nightscout for CGM Data
* Android Wear for viewing current Basal & acknowledging Temp Basal Suggestions (optional but highly recommended)
* xDrip Watch Face for displaying current Basal, IOB and COB on Android Wear (optional)
* NSClient for Nightscout uploads & CGM values (optional)
* Pump Driver app for your pump for closed Loop support (optional)

###Repos used
I am utilising code from the following repos and porting them over to this app.
* OpenAPS oref0 - https://github.com/timomer/oref0
* NightWatch - https://github.com/timomer/NightWatch
* Nightscout - https://github.com/timomer/cgm-remote-monitor
* xDrip - https://github.com/StephenBlackWasAlreadyTaken/xDrip-Experimental

###Thanks to
The community for all their code and advice to make this possible.
Special thanks to StephenBlackWasAlreadyTaken, jasoncalabrese, AdrianLxM, Lorelai, MilosKozak and scottleibrand.

#WeAreNotWaiting
