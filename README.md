# Hackabetes Artificial Pancreas Project
**This code is highly experimental, all suggestions by the app must be reviewed**

Native Android Artificial Pancreas App with OpenAPS.org algorithm

* Most recent apk: https://drive.google.com/open?id=0BxE8lMx4AjLieEpYSWU4NkpNbnc
* App Summary: https://sites.google.com/a/n-omer.co.uk/hackabetes/home/happ-overview
* Project blog: http://www.hypodiabetic.co.uk
* Gitter chat group: https://gitter.im/timomer/HAPP

![](https://github.com/timomer/HAPP/blob/master/HAPP_Front_Page.png)
![](https://github.com/timomer/HAPP/blob/master/HAPP_WF.png)



Features:
* Native Open Loop system (requires user to review and enter data into pump)
* Capture your Bolus and Carb Treatments
* Bolus Wizard utilising IOB, COB and CGM data
* Ability to switch between OpenAPS.org Dev and Stable algorithms
* Notifications for Android and Android Wear
* Visuals showing Carb and IOB over time and Basal vs Temp Basal adjustments
* Integration with Nightscout via NSClient and Pump Driver Apps for full Closed Loop operation

App can be used to inform the user at a user set time interval of suggested Temp Basal calculated by the OpenAPS.org algorithm, this can then be manually set on the users pump.
This allows a Diabetic with any pump to experiment with the OpenAPS algorithm.

Requirements:
* Android 5.0+
* xDrip App for CGM Data (use this xDrip build for integration with xDrip WF https://dl.dropboxusercontent.com/u/17867795/xdripbroadcast/xdripBroadcast21.apk)
* Android Wear for viewing current Basal & acknowledging Temp Basal Suggestions (optional but highly recommended)
* NSClient for Nightscout uploads (optional)
* Pump Driver app for your pump (optional)

###Repos used
I am utilising code from the following repos and porting them over to this app.
* OpenAPS oref0 - https://github.com/timomer/oref0
* NightWatch - https://github.com/timomer/NightWatch
* Nightscout - https://github.com/timomer/cgm-remote-monitor
* xDrip with HAPP WF Support - https://github.com/StephenBlackWasAlreadyTaken/xDrip-Experimental/tree/status_happ

###Thanks to
The community for all their code and advice to make this possible.
Special thanks to StephenBlackWasAlreadyTaken, jasoncalabrese, AdrianLxM, Lorelai, MilosKozak and scottleibrand.

#WeAreNotWaiting
