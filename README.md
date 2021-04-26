# Dynamic-Image-Switcher

Dynamically switch your dashboard image based on weather conditions, weather alerts, time of day, day of week, holidays, location mode, and more.

For instance, set your dashboard background to a rainy landscape or an umbrella when it's raining, to a sunny meadow when it's clear skies, to a tornado when there's a tornado warning, a Christmas tree on Christmas, a birthday cake on your birthday, a baseball on game day, a full moon when there's a full moon, etc.

**Supported Image Types**
JPG
SVG
PNG

**Install Instructions**
1. Install Dynamic Image Switcher app and Dynamic Image URL Device driver, either manually or via Hubitat Package Manager
2. Enable OAth if installed manually
3. Configure different URLs with different images
4. Configure rules in the app to set the display image path to different ones of the configured URLs under different conditions
5. Point your dashboard background to the local URL in the Dynamic Image Switcher app. Your different images will dynamically load from this single URL under the different conditions you established in the app.

**Local Only**: Note this only works locally. Hubitat does not currently support rendering of images at a cloud endpoint for remote access.

Enjoy!
