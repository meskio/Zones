ZoneGuard
==================
  
ZoneGuard is a "find my phone" type Android app with a geofencing twist. All devices
running Android 2.2 and higher with Google Play Services installed are supported.
  
Find My Phone
-------------
While most find my phone type apps require the user to access a web application,
ZoneGuard is controlled entirely by commands within SMS (text) messages. SMS commands
are authenticated by a user defined password following the command, the below
commands are currently supported:  
  
**start location updates** - Subscribe the sender of this message to GPS location updates.  
  
**stop location updates** - Unsubscribe the sender of this message from GPS location
updates.  
  
**audio alarm** - Sound a loud alarm for the length of time defined in settings.  
  
**battery level** - Reply with an SMS describing the devices batery level.  
  
**super lock** - Lock the device screen and prevent it from turning on, effectively
disabling shutdown, airplane mode and silent mode.  
  
**super unlock** - Allow the device screen to turn on again.  
  
**clear call history** - Delete the history of all outgoing, incomming and missed calls.  
  
**clear contacts** - Delete all contacts, note that contacts may be re-synced if contact
syncing is enabled.  

**factory reset** - Perform a full factory reset, all user data and applications will be
lost!  
  
Geofencing
----------
The geofencing aspect of ZoneGuard starts with allowing the user to draw zones
(polygons) on a map. The user can then assign actions to these zones and decide
whether they should be run upon enter or exit of the zone. The below actions are
currently supported:  
  
**Audio alarm** - Sound a loud alarm for the length of time defined in settings.  
  
**SMS alert** - Send an alert to the phone number defined in settings.  
  
**Email alert** - Send an alert to the email address defined in settings.  
  
**Super lock** - Lock the device screen and prevent it from turning on, effectively
disabling shutdown, airplane mode and silent mode.  
  
**Super unlock** - Allow the device screen to turn on again.  
  
**Clear call history** - Delete the history of all outgoing, incomming and missed calls.  
  
**Clear contacts** - Delete all contacts, note that contacts may be re-synced if contact
syncing is enabled.  

**Factory reset** - Perform a full factory reset, all user data and applications will be
lost!  
  
DOs and DON'Ts
--------------
DO set a screenlock password on your device.  
DO setup full disk encryption on your device.  
DO draw zones around police stations and factory reset on enter.  
**DO NOT** use the factory reset SMS command or action unless you're serious.  

Building
--------
1) Clone the repo into a working directory.  
  
2) Import as an Android project in your IDE of choice.  
  
3) Download ActionBarSherlock and add it as a library project dependancy for ZoneGuard.  
  
4) Download Google Play Services and add it as a library project dependancy for ZoneGuard.  
  
5) Replace the Google Maps API key inside AndroidManifest.xml by following [this tutorial](https://developers.google.com/maps/documentation/android/start#the_google_maps_api_key)

6) Build & run!

License
-------

Licensed under the GPLv3: http://www.gnu.org/licenses/gpl-3.0.html