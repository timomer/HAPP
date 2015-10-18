# Hackabetes Artificial Pancreas Project
**This code is highly experimental, all suggestions by the app must be reviewed**

Native Android implementation of the OpenAPS.org algorithm with additional functionality.

* Most recent apk: https://drive.google.com/uc?export=download&id=0BxE8lMx4AjLieDEwbHRLcTY2VGc
* App Summary: https://sites.google.com/a/n-omer.co.uk/hackabetes/home/happ-overview
* Project blog: http://www.hypodiabetic.co.uk
* Gitter chat group: https://gitter.im/timomer/HAPP

![](https://github.com/timomer/HAPP/blob/master/screenshot.png)
![](https://github.com/timomer/HAPP/blob/master/wear-screen.png)
![](https://github.com/timomer/HAPP/blob/master/wear-set-temp.png)

(Phone and Android Wear Notifications)

Current features:
* Open Loop system (requires user to review and enter data into pump)
* Capture your Bolus and Carb Treatments
* Bolus Wizard
* Suggested Temp Basal based on OpenAPS.org algorithm
* Notifications for Android and Android Wear when a new Temp Basal is suggested
* Visuals showing Carb and IOB over time and Basal vs Temp Basal adjustments

As of now the App can be used to inform the user at a user set time interval suggested Temp Basal as calculated by the OpenAPS.org algorithm, this can then be manually set on the users pump.
This allows a Diabetic with any pump to experiment with the OpenAPS algorithm.

Requirements:
* xDrip device and App for capturing blood sugar values
* Android 5.0

### What's next?
To integrate with the Roche Accu-Chek Combo pump. Any experience hacking Bluetooth? Please contact me.

### Why would I be interested?
As you can now play with the OpenAPS algorithm with any pump :)

### Common questions
* Q: Why have you not focused on the current Round Trip Android app?
* A: Round Trip was ported from the native Java implementation of OpenAPS to Android by a hired developer along with all Medtronic integration. I wanted a ground up native port of the OpenAPS system, by doing it myself I can learn as much as possible and be sure I am porting over only the items I require.
* Q: Why are you not reading Treatment Data from the pump
* A: I am building the system to support Roche pumps where Treatment data is captured on the Bluetooth enabled Blood Meter not on the Pump. This App is a complete replacement of this meter that will I hope one day link to the pump via Bluetooth.
* Q: Why do you not use the native OpenAPS Java files?
* A: I hear this is possible on Android, but believe this would be difficult to debug. One aim of this project is to provide an easy to debug version of determine_basal to help me understand the OpenAPS algorithm
* Q: Why have you done X and not Y in Android
* A: I am not a developer and have been only working on Android development for the last 2 months, if there is a better way to do something please do let me know

###To do
* Fix UI bug in line charts where lines appear to loop back on themselves
* Write up the core functions of the App
* Improve code comments
* Lots and lots of debugging
* Find a Bluetooth developer to help me hack the Roche Accu-Chek Combo pump

###Repos used
I am utilising code from the following repos and porting them over to this app.
* OpenAPS - https://github.com/timomer/openaps-js
* NightWatch - https://github.com/timomer/NightWatch
* Nightscout - https://github.com/timomer/cgm-remote-monitor

###Thanks to
The community for all their code and advice to make this possible. Special thanks to StephenBlackWasAlreadyTaken, jasoncalabrese and scottleibrand.

WeAreNotWaiting
