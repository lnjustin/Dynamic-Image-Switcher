/**
 *  Dynamic Image Switcher
 *
 *  Copyright 2021 Justin Leonard
 *  Copyright 2019 Dominick Meglio
 *
 *  Licensed Virtual the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Change History:
 *
 *    Date        Who            What
 *    ----        ---            ----
 */

import java.text.SimpleDateFormat
import groovy.transform.Field

definition(
    name: "Dynamic Image Switcher",
    namespace: "lnjustin",
    author: "Justin Leonard, Dominick Meglio",
    description: "Dynamically switches image based on date and weather",
    category: "My Apps",
    oauth: [displayName: "Dynamic Image Switcher", displayLink: ""],
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

mappings { 
    path("/DynamicImageSwitcher") { action: [ GET: "getImage"] }
}

@Field String checkMark = "https://raw.githubusercontent.com/lnjustin/App-Images/master/checkMark.svg"
@Field String xMark = "https://raw.githubusercontent.com/lnjustin/App-Images/master/xMark.svg"

preferences {
    page name: "mainPage", title: "", install: true, uninstall: true
	page name: "apiAccessPage", title: "Calendarific API Access", install: false, nextPage: "mainPage"
	page name: "ConfigureHolidays", title: "Configure Holidays", install: false, nextPage: "mainPage"
    page name: "DefineHolidays", title: "Define Holidays", install: false, nextPage: "ConfigureHolidays"
    page name: "ConfigureSchedule", title: "Configure Schedule", install: false, nextPage: "mainPage"
    page name: "ConfigureLocationModes", title: "Configure Location Modes", install: false, nextPage: "mainPage"
    page name: "ConfigureWeather", title: "Configure Weather", install: false, nextPage: "mainPage"
    page name: "DefineWeatherConditions", title: "Define Weather Conditions", install: false, nextPage: "ConfigureWeather"
    page name: "DefineWeatherAlerts", title: "Define Weather Alerts", install: false, nextPage: "ConfigureWeather"
    page name: "DefineSeasons", title: "Define Seasons", install: false, nextPage: "ConfigureWeather"
    page name: "ConfigureNighttime", title: "Configure Nighttime", description: "Configure image handling at night", install: false, nextPage: "mainPage"
}

def mainPage() {
    dynamicPage(name: "mainPage") {
        if(!state.accessToken){	
            //enable OAuth in the app settings or this call will fail
            logDebug("Creating new access token for Dynamic Image Switcher")
            createAccessToken()	
        }
        
        
        def localUri = getFullLocalApiServerUrl() + "/DynamicImageSwitcher?access_token=${state.accessToken}"
        
           section("") {
                def child = getChildDevice("DynamicImageSwitcherDevice${app.id}")
                if (child) {
                   paragraph("Dynamic Image Switcher outputs your image <strong><a href='${localUri}'>here</a></strong>")     
                }
                else paragraph "<b>After configuring, click Done to finish installation and create a Dynamic Image Switcher Device. Then return to the app to get the output URL.</b>"
           }
            section {
              //  header()
                paragraph getInterface("header", " Calendarific API")
                href(name: "apiAccessPage", title: getInterface("boldText", "Configure Calendarific API Access"), description: "API Access Required for Holiday Lookup", required: false, page: "apiAccessPage", image: (calAPIToken ? checkMark : xMark))
                paragraph getInterface("header", " Setup")
                href(name: "HolidayPage", title: getInterface("boldText", "Configure Holidays"), description: "Configure images for different holidays.", required: false, page: "ConfigureHolidays")
                href(name: "SchedulePage", title: getInterface("boldText", "Configure Schedule"), description: "Configure images according to a schedule.", required: false, page: "ConfigureSchedule")
                href(name: "ModePage", title: getInterface("boldText", "Configure Location Modes"), description: "Configure images for different location modes.", required: false, page: "ConfigureLocationModes")
                href(name: "WeatherPage", title: getInterface("boldText", "Configure Weather"), description: "Configure images for different weather conditions and weather alerts.", required: false, page: "ConfigureWeather")               
                href(name: "NightPage", title: getInterface("boldText", "Configure Nighttime"), description: "Configure image handling at nighttime.", required: false, page: "ConfigureNighttime")               
                input(name:"pathForDefault", type:"text", title: "Default Image", description: "Image Path", required: false, width: 12)
            }
            section (getInterface("header", " Settings")) {
                input(name:"multiImageRotateInterval", type: "number", title: "Rotate Interval (mins) if Multiple Images Active", required:false, default: 30)
 
			    input("debugOutput", "bool", title: "Enable debug logging?", defaultValue: true, displayDuringSetup: false, required: false)
		    }
            section("") {
                
                footer()
            }
    }
}

def getMultiImageRotateInterval() {
    return multiImageRotateInterval ? multiImageRotateInterval as Integer : 15
}

def footer() {
    paragraph getInterface("line", "") + '<div style="display: block;margin-left: auto;margin-right: auto;text-align:center">&copy; 2020 Justin Leonard.<br>'
}

def apiAccessPage() {
	dynamicPage(name: "apiAccessPage", title: "Connect to Calendarific API", nextPage:"mainPage", uninstall:false, install: false) {
		section("API Access"){
			paragraph "You can obtain a free API key going to <a href='https://calendarific.com'>https://calendarific.com</a>"
			input("calAPIToken", "text", title: "API Key", description: "API Key", required: true)
			input("calCountry", "enum", title: "Country", description: "Country", options: countryList, required: true)
		}
	}
	
}
                     
def ConfigureHolidays() {
    dynamicPage(name: "ConfigureHolidays", title: "Configure Holidays", nextPage:"mainPage", uninstall:false, install: false) {
          section("") {
              paragraph "Holiday Images are prioritized over all other images, except for weather alerts and, optionally, nighttime images."
              def isHolidayConfigured = calNational || calReligious || calObservances || customHolidayCount > 0
              
              href(name: "DefineHolidays", title: getInterface("boldText", "Define Holidays"), description: "Define what holidays to configure.", required: false, page: "DefineHolidays", image: (isHolidayConfigured ? checkMark : xMark))
                          
              if (isHolidayConfigured) {
                   input(name:"basePathForHolidayImages", type:"text", title: "Base Path for Holiday Images", description: "Optional prefix common to the path for all holiday images", required: false)                                          
              }
              if (calNational) DisplayImageInputs(calNational)
              if (calReligious) DisplayImageInputs(calReligious)
              if (calObservances) DisplayImageInputs(calObservances)
              
             if (customHolidayCount > 0)
                {
                    for (def i = 0; i < customHolidayCount; i++) {
                        input(name:"pathForCustomHoliday_${i}", type:"text", title: settings["customHolidayName${i}"], description: "Image Path for " + settings["customHolidayName${i}"], required: false)                        
                    }
                }
              else paragraph "No holidays defined for which to configure images."
          }
    }
}
    
def DisplayImageInputs(holidays) {
    for (holiday in holidays) {
        input(name:"pathForHoliday_${holiday}", type:"text", title: holiday, description: "Image Path for ${holiday}", required: false)
    }
          
}

def areHolidaysConfigured() {
    def holidaysConfigured = false
    if (calNational || calReligious || calObservances || customHolidayCount > 0) holidaysConfigured = true
    return holidaysConfigured
}

def DefineHolidays() {
	getHolidays()
    dynamicPage(name: "DefineHolidays", title: "Define Holidays", nextPage:"ConfigureHolidays", uninstall:false, install: false) {
        section("") {
			input(name:"calNational", type: "enum", title: "National Holidays", options:state.nationalHolidays, required:false, multiple:true)
			input(name:"calReligious", type: "enum", title: "Religious Holidays", options:state.religiousHolidays, required:false, multiple:true)
			input(name:"calObservances", type: "enum", title: "Observances", options:state.observanceHolidays, required:false, multiple:true)
        }
        
        section("Custom Holidays") {
            input("customHolidays", "bool", title: "Define custom holidays?",defaultValue: false, displayDuringSetup: true, submitOnChange: true)
            if (customHolidays)
            {
                input(name: "customHolidayCount", type: "number", range: "1..366", title: "How many custom holidays?", required: true, submitOnChange: true)
                if (customHolidayCount > 0)
                {
                    for (def i = 0; i < customHolidayCount; i++) {
                        input(name:"customHolidayName${i}", type:"text", title: "Holiday Name ${i+1}", required: true, width: 6)
                        input(name:"customHolidayMonth${i}", type:"enum", options:months, title: "Month ${i+1}", required: true, width: 3)
                        input(name:"customHolidayDay${i}", type:"enum", options:getNumDaysInMonth(settings["customHolidayMonth${i}"]), title: "Day ${i+1}", required: true, width: 3)
                    }
                }
            }
        }

    }
}

@Field daysOfWeekList = ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"]
@Field daysOfWeekMap = ["Sunday":1, "Monday":2, "Tuesday":3, "Wednesday":4, "Thursday":5, "Friday":6, "Saturday":7]
@Field months = ["JAN","FEB","MAR","APR","MAY","JUN","JUL","AUG","SEP","OCT","NOV","DEC"]
@Field monthsMap = [
    "JAN" : 1,
    "FEB" : 2,
    "MAR" : 3,
    "APR" : 4,
    "MAY" : 5,
    "JUN" : 6,
    "JUL" : 7,
    "AUG" : 8,
    "SEP" : 9,
    "OCT" : 10,
    "NOV" : 11,
    "DEC" : 12]
@Field days29 = [1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29]
@Field days30 = [1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30]    
@Field days31 = [1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31]

def isCustomHoliday(holiday) {
    def didFindCustomHoliday = false
    for (def i = 0; i < customHolidayCount; i++) {
        if (settings["customHolidayName${i}"] == holiday) {
            didFindCustomHoliday = true
            break
        }            
    }
    return didFindCustomHoliday
}
                     
def ConfigureSchedule() {
    dynamicPage(name: "ConfigureSchedule", title: "Configure Schedule", nextPage:"mainPage", uninstall:false, install: false) {
          section("") {
              paragraph "Scheduled images will be prioritized above location mode images and weather condition images, but not holiday images or weather alert images."
              input(name: "scheduleCount", type: "number", range: "1..366", title: "How many schedules?", required: true, submitOnChange: true)
                if (scheduleCount > 0)
                {
                    input(name:"basePathForScheduleImages", type:"text", title: "Base Path for Scheduled Images", description: "Optional prefix common to the path for all scheduled images", required: false)                                          
                    for (def i = 0; i < scheduleCount; i++) {
                        paragraph getInterface("line")
                        paragraph getInterface("header", "Schedule ${i+1}")
                        input(name:"scheduleName${i}", type:"text", title: "Schedule Name", required: true, width: 6)
                        input(name:"pathForSchedule_${i}", type:"text", title: "Image Path", required: true) 
                        input(name:"daysOfWeek${i}", type: "enum", title: "Days of the Week", description:"Select Days of Week for which to configure images", options:daysOfWeekList, required:true, multiple:true, submitOnChange: true, width: 3)
                        input(name:"startTime${i}", type: "time", title: "Start Time", description: "Input time to start displaying image on selected days", required: true, width: 3)
                        input(name:"stopTime${i}", type: "time", title: "Stop Time", description: "Input time to stop displaying image on selected days", required: true, width: 3)
                        input(name: "dateRestricted${i}", type: "bool", title: "Only between certain dates?", required: true, defaultValue: false, submitOnChange:true)
                        if (settings["dateRestricted${i}"]) {
                            input(name:"scheduleStartMonth${i}", type:"enum", options:months, title: "Start Month", required: true, width: 3)
                            input(name:"scheduleStartDay${i}", type:"enum", options:getNumDaysInMonth(settings["scheduleStartMonth${i}"]), title: "Start Date", required: true, width: 3)                            
                            input(name:"scheduleStopMonth${i}", type:"enum", options:months, title: "Stop Month", required: true, width: 3)
                            input(name:"scheduleStopDay${i}", type:"enum", options:getNumDaysInMonth(settings["scheduleStopMonth${i}"]), title: "Stop Date", required: true, width: 3)
                        }
                    }
                }
          }
    }    
}

def getNumDaysInMonth(month) {
    def days = days31
    if (month && month == "FEB") days = days29
    else if (month && (month == "APR" || month == "JUN" || month == "SEP" || month == "NOV")) days = days30
    return days        
}

def isScheduleConfigured() {
    def scheduleConfigured = false
    if (scheduleCount > 0) {
         scheduleConfigured = true            
    }
    return scheduleConfigured   
}

def anyScheduleActive() {
    def scheduleActive = false
    if (state.activeSchedules.size() > 0) {
        scheduleActive = true            
    }
    return scheduleActive
}

def getActiveSchedules() {
    def activeSchedules = []
    if (scheduleCount > 0) {
        for (def i = 0; i < scheduleCount; i++) {
            if (isScheduleActive(i)) activeSchedules.add(i)          
        }
    }
    return activeSchedules   
}

def setSchedules() {
    state.activeSchedules = getActiveSchedules()
}

def updateSchedules() {
    state.activeSchedules = getActiveSchedules()
    updateImage()
}

def getDayOfWeekExpression(daysOfWeek) {
    def expression = "*"
    def size = daysOfWeek.size()
    if (size > 0) {
        expression = ""
        for (def i = 0; i < size; i++) {
            expression += daysOfWeek[i]
            if (i < size - 1) expression += ","
        }
    }
    return expression
}

def getTimeExpression(time) {
    def expression = null
    if (time) {
        def dateTime = toDateTime(time)
        def hour = dateTime.format("H")
        def min = dateTime.format("mm")
        expression = min + " " + hour
    }
    return expression
}

def configureScheduleUpdates() {
    if (scheduleCount > 0) {
        for (def i = 0; i < scheduleCount; i++) {
            if (settings["daysOfWeek${i}"] && settings["startTime${i}"] && settings["stopTime${i}"]) {
                def daysOfWeek = getDayOfWeekExpression(settings["daysOfWeek${i}"])
            
                def startTime = getTimeExpression(settings["startTime${i}"])
                def startSchedule = "01 " + startTime + " ? * " + daysOfWeek
                schedule(startSchedule, updateSchedules, [overwrite: false]) 
            
                def stopTime = getTimeExpression(settings["stopTime${i}"])
                def stopSchedule = "01 " + stopTime + " ? * " + daysOfWeek
               schedule(stopSchedule, updateSchedules, [overwrite: false]) 
            }
            else log.warn "Schedule Number ${i} has incomplete configuration."
        }
    }
}

def isScheduleActive(i) {
    def scheduleActive = false
    
    if (settings["pathForSchedule_${i}"]) {
        def today = new Date()
        def dayOfWeek = today.format('EEEE') 
        if(settings["daysOfWeek${i}"].contains(dayOfWeek)) {    
            logDebug("Day of Week match in schedule ${i}")
            def startTime = settings["startTime${i}"]
            def stopTime = settings["stopTime${i}"]
            if (isWithinTime(startTime, stopTime)) {
                logDebug("Time match in schedule ${i}")
                if (settings["dateRestricted${i}"]) {
                    def startMonth = monthsMap[settings["scheduleStartMonth${i}"]] as Integer
                    def startDay = settings["scheduleStartDay${i}"] as Integer
                    def stopMonth = monthsMap[settings["scheduleStopMonth${i}"]] as Integer
                    def stopDay = settings["scheduleStopDay${i}"] as Integer
                    if (areWithinDates(startMonth, startDay, stopMonth, stopDay)) {
                        logDebug("Date match in schedule ${i}")
                        scheduleActive = true
                    }
                    else scheduleActive = false
                }
                else scheduleActive = true
            }
        }
    }
    return scheduleActive
}


def isWithinTime(startTime, stopTime) {
    def withinTime = false
    
    def windowStart = toDateTime(startTime)
    def windowEnd = toDateTime(stopTime)
    if (timeOfDayIsBetween(windowStart, windowEnd, new Date(), location.timeZone)) {
        withinTime = true
    }
    return withinTime
}

def areWithinDates(startMonth, startDay, stopMonth, stopDay) {
    def withinDates = false
    def today = timeToday(null, location.timeZone)
	def month = today.month+1
	def day = today.date
        
    if ((month == startMonth && day >= startDay) || month > startMonth)  {
        if ((month == stopMonth && day <= stopDay) || month < stopMonth) {
		     withinDates = true
        }
    }
    return withinDates
}
    
def ConfigureLocationModes() {
    dynamicPage(name: "ConfigureLocationModes", title: "Configure Location Modes", nextPage:"mainPage", uninstall:false, install: false) {
          section("") {
              input(name:"locationModes", type: "mode", title: "Location Modes", description:"Select Locations Modes for which to configure images",  required:false, multiple:true, submitOnChange: true)
              input(name:"prioritizeModeOrWeather", type: "enum", title: "Prioritize Location Mode or Weather Conditions?", options:["Location Mode", "Weather Conditions"], required:false, multiple:false)
              if (locationModes) {
                   input(name:"basePathForModeImages", type:"text", title: "Base Path for Location Mode Images", description: "Optional prefix common to the path for all location mode images", required: false)                                          
                    for (mode in locationModes) {
                        input(name:"pathForMode_${mode}", type:"text", title: mode, description: "Image Path", required: false)                        
                    }
                }
              else paragraph "No location modes defined for which to configure images."
          }
    }
}

def areSeasonsConfigured() {
    def areseasonsConfiugred = false
    if (settings["imagesPerSeason"] && settings["seasonCount"] > 0) areseasonsConfiugred = true
    return areseasonsConfiugred
}

def ConfigureWeather() {
    dynamicPage(name: "ConfigureWeather", title: getInterface("header", "Configure Weather"), nextPage:"mainPage", uninstall:false, install: false) {
          section("") {
              href(name: "DefineWeatherConditions", title: getInterface("boldText", "Define Weather Conditions"), description: "Define for what weather conditions to configure images.", required: false, page: "DefineWeatherConditions", image: (weatherConditions ? checkMark : xMark))
              href(name: "DefineWeatherAlerts", title: getInterface("boldText", "Define Weather Alerts"), description: "Define for what weather alerts to configure images.", required: false, page: "DefineWeatherAlerts", image: (weatherAlerts ? checkMark : xMark))
                       
              href(name: "DefineSeasons", title: getInterface("boldText", "Define Seasons"), description: "Define whether to configure weather images per season.", required: false, page: "DefineSeasons", image: (areSeasonsConfigured ? checkMark : xMark))
              input(name:"prioritizeModeOrWeather", type: "enum", title: "Prioritize Location Mode or Weather Conditions?", options:["Location Mode", "Weather Conditions"], required:true, multiple:false)
              input(name:"seperateNightWeather", type:"bool", title: "Define Separate Images for Weather at Night?", required: true, default: false, submitOnChange: true, width: 9)
              if (seperateNightWeather) {
                  state.previousNightHandling = nightHandling
                  app.updateSetting("nightHandling",[type:"enum",value:"Use Nighttime Weather Images"])
              }
              else {
                  if (state.previousNightHandling) app.updateSetting("nightHandling",[type:"enum",value:state.previousNightHandling])
              }
              if (weatherConditions)
                {          
                   input(name:"basePathForWeatherImages", type:"text", title: "Base Path for Weather Images", description: "Optional prefix common to the path for all weather images", required: false)                                          
                }
          }
        
        section ("") {
            
             if (weatherConditions)
                {
                    paragraph getInterface("header", "Configure Image Paths For Weather Conditions")
                   if (!areSeasonsConfigured() && !seperateNightWeather) {
                        for (condition in weatherConditions) {
                            def code = weatherMap[condition]
                            input(name:"pathForWeather_${code}", type:"text", title: condition, description: "Image Path", required: false)                        
                        }
                    }
                    else if (!areSeasonsConfigured() && seperateNightWeather) {
                        for (condition in weatherConditions) {
                            def code = weatherMap[condition]
                            input(name:"pathForWeather_${code}_day", type:"text", title: getInterface("boldText", "Day: ") + condition, description: "Image Path", required: false, width: 6)   
                            input(name:"pathForWeather_${code}_night", type:"text", title: getInterface("boldText", "Night: ") + condition, description: "Image Path", required: false, width: 6)   
                        }
                    }
                    else if (areSeasonsConfigured() && !seperateNightWeather) {
                        for (condition in weatherConditions) {
                            def code = weatherMap[condition]
                            for (i=1; i <= seasonCount; i++) {
                                def widthForSeasons = 12 / seasonCount
                                input(name:"pathForWeather_${code}_season${i}", type:"text", title: getInterface("boldText", settings["season${i}"] + ": ") + condition, description: "Image Path", required: false, width: widthForSeasons)   
                            }
                        }
                    }
                    else if (areSeasonsConfigured() && seperateNightWeather) {
                        paragraph getInterface("subHeader", "Daytime Weather")
                        for (condition in weatherConditions) {
                            def code = weatherMap[condition]
                            for (i=1; i <= seasonCount; i++) {
                                def widthForSeasons = 12 / seasonCount
                                input(name:"pathForWeather_${code}_season${i}_day", type:"text", title: getInterface("boldText", settings["season${i}"] + ": ") + condition, description: "Image Path", required: false, width: widthForSeasons)   
                            }
                        }
                        paragraph getInterface("subHeader", "Nighttime Weather")
                        for (condition in weatherConditions) {
                            def code = weatherMap[condition]
                            for (i=1; i <= seasonCount; i++) {
                                def widthForSeasons = 12 / seasonCount
                                input(name:"pathForWeather_${code}_season${i}_night", type:"text", title: getInterface("boldText", settings["season${i}"] + ": ") + condition, description: "Image Path", required: false, width: widthForSeasons)   
                            }
                        }
                    }
                }
            if (weatherAlerts) {
                paragraph getInterface("header", "Configure Image Paths For Weather Alerts")
                for (alert in weatherAlerts) {
                    input(name:"pathForAlert_${alert}", type:"text", title: alert, description: "Image Path", required: false)                        
                }
            }
            if (!weatherConditions && !weatherAlerts) {
                paragraph getInterface("header", "Configure Image Paths")
                paragraph "No weather defined for which to configure images."
            }
          }
    }
}

def DefineWeatherConditions() {
    dynamicPage(name: "DefineWeatherConditions", title: "Define Weather Conditions", nextPage:"ConfigureWeather", uninstall:false, install: false) {
        section("") {
            input name: "weatherDevice", type: "device.OpenWeatherMap-NWSAlertsWeatherDriver", title: "Open Weather Map Weather Device", submitOnChange: true, multiple: false, required: false
            if (weatherDevice) {
                 input(name:"weatherType", type: "enum", title: "Current Conditions or Forecasted Conditions?", options:["current", "forecasted"], required:true, multiple:false)
                input(name:"weatherConditions", type: "enum", title: "Weather Conditions for which to Configure Images", options:weatherList, required:true, multiple:true)
            }
        }

    }
}

def DefineWeatherAlerts() {
    dynamicPage(name: "DefineWeatherAlerts", title: "Define Weather Alerts", nextPage:"ConfigureWeather", uninstall:false, install: false) {
        section("") {
            paragraph "Weather Alert images are prioritized above all other images and in the order listed. For example, a Tornado Watch image will be prioritized over any holiday image and over any Severe Thunderstorm Watch image."
            input name: "NOAATileDevice", type: "device.NOAATile", title: "NOAA Tile from NOAA Weather Alerts App", submitOnChange: true, multiple: false, required: false
            if (NOAATileDevice) {
                input(name:"weatherAlerts", type: "enum", title: "Alerts for which to Configure Images", options:alertOptions, required:true, multiple:true)
             }
        }

    }
}

def DefineSeasons() {
    dynamicPage(name: "DefineSeasons", title: "Define Seasons", nextPage:"ConfigureWeather", uninstall:false, install: false) {
        section("") {
            input("imagesPerSeason", "bool", title: "Configure Images Per Season?",defaultValue: false, displayDuringSetup: true, submitOnChange: true)
            if (imagesPerSeason)
            {
                input(name: "seasonCount", type: "number", range: "1..4", title: "How many seasons?", required: true, submitOnChange: true)
                if (seasonCount > 0)
                {
                    for (def i = 1; i <= seasonCount; i++) {
                        input(name:"season${i}", type:"text", title: "Season Name ${i}", required: true, width: 4)
                        
                        input(name:"seasonStartMonth${i}", type:"enum", options:months, title: "Start Month ${i}", required: true, width: 2, submitOnChange:true)
                        def days = days31
                        if (settings["seasonStartMonth${i}"] && settings["seasonStartMonth${i}"] == "FEB") days = days29
                        else if (settings["seasonStartMonth${i}"] && (settings["seasonStartMonth${i}"] == "APR" || settings["seasonStartMonth${i}"] == "JUN" || settings["seasonStartMonth${i}"] == "SEP" || settings["seasonStartMonth${i}"] == "NOV")) days = days30
                        input(name:"seasonStartDay${i}", type:"enum", options:days, title: "Start Day ${i}", required: true, width: 2)
                        
                        input(name:"seasonEndMonth${i}", type:"enum", options:months, title: "End Month ${i}", required: true, width: 2, submitOnChange:true)
                        days = days31
                        if (settings["seasonEndMonth${i}"] && settings["seasonEndMonth${i}"] == "FEB") days = days29
                        else if (settings["seasonEndMonth${i}"] && (settings["seasonEndMonth${i}"] == "APR" || settings["seasonEndMonth${i}"] == "JUN" || settings["seasonEndMonth${i}"] == "SEP" || settings["seasonEndMonth${i}"] == "NOV")) days = days30
                        input(name:"seasonEndDay${i}", type:"enum", options:days, title: "End Day ${i}", required: true, width: 2)
                    }
                }
        }
        }
    }
}

def ConfigureNighttime() {
    dynamicPage(name: "ConfigureNighttime", title: "Configure Nighttime", nextPage:"mainPage", uninstall:false, install: false) {
          section("") {
              
              paragraph "Configure how nighttime impacts image handling. Nighttime is from sunset to sunrise. "
              input(name:"nightHandling", type: "enum", title: "At nighttime...", options:["Do Nothing Different", "Use Specific Image", "Use Images per Moon Phase", "Use Nighttime Weather Images"], default: "Do Nothing Different", required:false, multiple:false, submitOnChange: true)
              input("nightOverHoliday", "bool", title: "Prioritize Nighttime Images over Holiday images?",defaultValue: false, displayDuringSetup: true, submitOnChange: false)
              if (nightHandling == "Use Specific Image") {
                  input(name:"pathForNight", type:"text", title: "Nighttime Image", description: "Image Path", required: false, width: 12)   
              }
              else if (nightHandling == "Use Images per Moon Phase") {

                  input name: "moonDevice", type: "device.MoonPhase", title: "Moon Phase Device", submitOnChange: true, multiple: false, required: false
                  if (moonDevice) {
                      input(name:"basePathForMoonImages", type:"text", title: "Base Path for Moon Images", description: "Optional prefix common to the path for all moon images", required: false)                                          

                      input(name:"pathForPhase_NewMoon", type:"text", title: "New Moon", description: "Image Path", required: false, width: 6)   
                      input(name:"pathForPhase_WaxingCrescent", type:"text", title: "Waxing Crescent", description: "Image Path", required: false, width: 6) 
                      input(name:"pathForPhase_FirstQuarter", type:"text", title: "First Quarter", description: "Image Path", required: false, width: 6) 
                      input(name:"pathForPhase_WaxingGibbous", type:"text", title: "Waxing Gibbous", description: "Image Path", required: false, width: 6) 
                      input(name:"pathForPhase_FullMoon", type:"text", title: "Full Moon", description: "Image Path", required: false, width: 6) 
                      input(name:"pathForPhase_WaningGibbous", type:"text", title: "Waning Gibbous", description: "Image Path", required: false, width: 6) 
                      input(name:"pathForPhase_LastQuarter", type:"text", title: "Last Quarter", description: "Image Path", required: false, width: 6) 
                      input(name:"pathForPhase_WaningCrescent", type:"text", title: "Waning Crescent", description: "Image Path", required: false, width: 6)            
                  }
              }
          }
    }
}


def installed() {
	initialize()
}

def updated() {
    unschedule()
	unsubscribe()
    resetState()
	initialize()
}

def uninstalled() {
    deleteChild()
	logDebug "Uninstalled app"
}

def initialize() {
    if (areHolidaysConfigured()) {
	    getHolidays()
        setHoliday()
	    schedule("01 00 00 ? * *", updateHoliday)	    
    }
    if (isScheduleConfigured()) {
        setSchedules()
        configureScheduleUpdates()      
    }
    if (locationModes) {
        subscribe(location, "mode", handleModeChange) 
        state.mode = location.mode
    }
    if (weatherDevice) {
        logDebug("Found Weather Device: ${weatherDevice}")
        if (weatherType) {
            logDebug("Configuring images for weather, with type: ${weatherType}.")
            state.weatherCode = weatherType == "current" ? weatherDevice.currentValue("condition_code") : weatherDevice.currentValue("forecast_code")
            if (weatherType == "current") subscribe(weatherDevice, "condition_code", weatherConditionHandler)
            else if (weatherType == "forecasted") subscribe(weatherDevice, "forecast_code", weatherConditionHandler)
            if (imagesPerSeason && seasonCount > 0) {
                setSeason()
                schedule("01 00 00 ? * *", updateSeason)                
            }
        }
    }
            
    if (NOAATileDevice) {
        state.activeAlerts = NOAATileDevice.currentValue("Alerts").toLowerCase()
        subscribe(NOAATileDevice, "Alerts", weatherAlertHandler)
    }
            
    if (nightHandling && nightHandling != "Do Nothing Different") {
        subscribe(location, "sunrise", sunriseHandler)
        subscribe(location, "sunset", sunsetHandler)
        if (timeOfDayIsBetween(location.sunrise, location.sunset, new Date(),  location.timeZone)) {
            state.isNight = false
        }
        else {
            state.isNight = true
            if (nightHandling == "Use Images per Moon Phase") setMoonPhase()
        }
    }
    createChild()
    updateImage()
}

def resetState() {
    // clear all state except access token
    def token = state.accessToken  
    state.clear()
    state.accessToken = token
}

def createChild()
{
    def child = getChildDevice("DynamicImageSwitcherDevice${app.id}")
    if (!child) {
        String childNetworkID = "DynamicImageSwitcherDevice${app.id}"
        child = addChildDevice("lnjustin", "Dynamic Image URL Device", childNetworkID, [label:"Dynamic Image Switcher Device", isComponent:true, name:"Dynamic Image Switcher Device"])
        child.updateSetting("parentID", app.id)
    }
}

def deleteChild()
{
    deleteChildDevice("DynamicImageSwitcherDevice${app.id}")
}

def getURLFromChild() {
    def child = getChildDevice("DynamicImageSwitcherDevice${app.id}")
    if (child) {
        return child.getImageURL() 
    }
    log.error "No Child Device Found"
    return null
}

def getImage() {
	byte[] imageBytes = null
    String imageURL = ""
    
    imageURL = getURLFromChild()
    
    if (!imageURL) {
        log.error "No imageURL from which to get image. No image to be displayed."
        return
    }
    
    def params = [
        uri: imageURL,
        headers: ["Accept": "image/jpeg; image/svg+xml; image/png; image/gif"]
    ]
    try {
        httpGet(params) { resp ->
            if(resp?.data != null) {
                imageBytes = new byte[resp?.data.available()]
                resp?.data.read(imageBytes)
            }
            else {
                log.error "Null Response"
            }
        }
    } catch (exception) {
        log.error "Dynamic Image Switcher exception: ${exception.message}"
    }

    String extension = "";
    int i = imageURL.lastIndexOf('.');
    if (i > 0) {
        extension = imageURL.substring(i+1).toLowerCase()
    }
    
    def contentTypeString = null    
    if (extension == "svg") {
        contentTypeString = "image/svg+xml"
    }
    else if (extension == "jpg" || extension == "jpeg") {
        contentTypeString = "image/jpeg"
    }
    else if (extension == "png") {
         contentTypeString = "image/png"
    }
    else if (extension == "gif") {
         contentTypeString = "image/gif"
    }
    render contentType: contentTypeString, data: imageBytes, status: 200
}

def handleModeChange(evt) {
    state.mode = evt.value
    updateImage()
}

def weatherConditionHandler(evt) {
    state.weatherCode = evt.value
    updateImage()
}

def isConfiguredAlertActive() {
    def didFindAlert = false
    if (state.activeAlerts) {
        for (alert in weatherAlerts) {
            if (state.activeAlerts.contains(alert.toLowerCase())) {
                didFindAlert = true
            }
        }
    }
    return didFindAlert
}

def getHighestPriorityActiveAlert() {
    // get the highest priority alert that is active and for which there is an image configured
    def foundAlert = null
    for (alert in weatherAlerts) {
        // TO DO: check whether the for loop guarantees the order over which the collection is iterated
        if (state.activeAlerts.contains(alert.toLowerCase()) && settings["pathForAlert_${alert}"] != null) {
            foundAlert = alert
            break
        }
    }    
    if (foundAlert == null) log.error "Unexpected Failure to Find Alert"
    return foundAlert
}

def weatherAlertHandler(evt) {
    state.activeAlerts = evt.value.toLowerCase()
    updateImage()
}

def sunsetHandler(evt) {
 //   Date sunsetTime = evt.value
 //   schedule(sunsetTime, handleSunsetEvent)
    log.debug "In sunset handler"
    if (nightHandling && nightHandling == "Use Images per Moon Phase") setMoonPhase()
    state.isNight = true
    updateImage()  
}

def handleSunsetEvent() {
    if (nightHandling && nightHandling == "Use Images per Moon Phase") setMoonPhase()
    state.isNight = true
    updateImage()    
}

def sunriseHandler(evt) {
  //  Date sunriseTime = evt.value
  //  schedule(sunriseTime, handleSunriseEvent)
    log.debug "In sunrise handler"
    state.isNight = false
    updateImage()
}

def handleSunriseEvent() {
    state.isNight = false
    updateImage()    
}

def isConfiguredHoliday() {
    // returns true if today is a holiday and an image is configured for at least one holiday
    def isHoliday = false
    for (holiday in state.activeHolidays) {
        if (settings["pathForHoliday_${holiday}"]) isHoliday = true
    }
    for (holiday in state.activeCustomHolidays) {
        if (settings["pathForCustomHoliday_${holiday}"]) isHoliday = true
    }
    return isHoliday
}

def getNumActiveHolidays() {
    return state.activeHolidays.size() + state.activeCustomHolidays.size()
}

def getHolidayImage(holiday, custom=false) {
    def path = null
    if (custom) {
        path = basePathForHolidayImages ? basePathForHolidayImages + settings["pathForCustomHoliday_${holiday}"] : settings["pathForCustomHoliday_${holiday}"]
    }
    else {
        path = basePathForHolidayImages ? basePathForHolidayImages + settings["pathForHoliday_${holiday}"] : settings["pathForHoliday_${holiday}"]
    }
    return path
}

def getAllActiveHolidayImages(custom=false) {
    def paths = null
    if (custom) {
        for (holiday in state.activeCustomHolidays) {
            def path = basePathForHolidayImages ? basePathForHolidayImages + settings["pathForCustomHoliday_${holiday}"] : settings["pathForCustomHoliday_${holiday}"]
            paths.add(path)
        }
    }
    else {
        for (holiday in state.activeHolidays) {
            def path = basePathForHolidayImages ? basePathForHolidayImages + settings["pathForHoliday_${holiday}"] : settings["pathForHoliday_${holiday}"]
            paths.add(path)
        }
    }
    return paths    
}

def getAlertImage(alert) {
    return basePathForWeatherImages ? basePathForWeatherImages + settings["pathForAlert_${alert}"] : settings["pathForAlert_${alert}"]
}

def getModeImage() {
    return basePathForModeImages ? basePathForModeImages + settings["pathForMode_${state.mode}"] : settings["pathForMode_${state.mode}"]
}

def getScheduleImage(i) {
    return basePathForScheduleImages ? basePathForScheduleImages + settings["pathForSchedule_${i}"] : settings["pathForSchedule_${i}"]
}

def getAllActiveScheduleImages() {
    def paths = null
    for (schedule in state.activeSchedules) {
        def path = getScheduleImage(schedule)
        paths.add(path)
    }
    return paths
}

def isConfiguredMode() {
    def isMode = false
    for (mode in locationModes) {
        if (state.mode && state.mode == mode && settings["pathForMode_${mode}"]) isMode = true
    }
    return isMode
}

def isNightConfigured() {
    def isNightConfigured = false
    if (nightHandling && nightHandling != "Do Nothing Different") {
        if (nightHandling == "Use Specific Image" && pathForNight != null) isNightConfigured = true
        else if (nightHandling == "Use Images per Moon Phase" && state.moonPhase && settings["pathForPhase_" + state.moonPhase]) isNightConfigured = true
        else if (nightHandling == "Use Nighttime Weather Images") {
            if (getWeatherImage() != null) isNightConfigured = true     
        }
    }
    return isNightConfigured
}

def rotateImages(Map data) {
    if (state.rotatingImages && state.rotatingImages.size > 0) {
        def nextImageIndex = state.previousImageIndex + 1
        if (nextImageIndex > state.rotatingImages.size() - 1) nextImageIndex = 0
        state.image = state.rotatingImages[nextImageIndex]
        def intervalSetting = getMultiImageRotateInterval()
        def interval = intervalSetting*60
        runIn(interval, "rotateImages")
    }
}

def rotateActiveHolidayImages() {
    state.previousImageIndex = -1
    state.rotatingImages = []
    def holidayImages = getAllActiveHolidayImages(false)
    def customHolidayImages = getAllActiveHolidayImages(true)
    state.rotatingImages.addAll(holidayImages)
    state.rotatingImages.addAll(customHolidayImages)
    def intervalSetting = getMultiImageRotateInterval()
    def interval = intervalSetting*60
    runIn(interval, "rotateImages")    
}

def rotateActiveScheduleImages() {
    state.previousImageIndex = -1
    state.rotatingImages = []
    def scheduleImages = getAllActiveScheduleImages()
    state.rotatingImages.addAll(scheduleImages)
    def intervalSetting = getMultiImageRotateInterval()
    def interval = intervalSetting*60
    runIn(interval, "rotateImages")    
}
def updateImage() {
    def image = null
    unschedule("rotateImages")
    
    if (isConfiguredAlertActive()) {
        logDebug("Displaying image for alert")
        image = getAlertImage(getHighestPriorityActiveAlert())
    }
    else if (isConfiguredHoliday() && state.isNight && nightOverHoliday && isNightConfigured()) {
        logDebug("Displaying night image on holiday")
        // Would display holiday image, except that it's night and nighttime images are to be prioritized over holiday images --> display nighttime image
        image = getNightImage()
    }
    else if (isConfiguredHoliday() && (!state.isNight || !nightOverHoliday)) {
        logDebug("Displaying image for holiday")
        // It's a holiday, and either it's daytime or its nightime but holiday images are prioritized over nighttime images --> display holiday image
        def numActiveHolidays = getNumActiveHolidays()
        if (numActiveHolidays > 0) {
            if (state.activeHolidays.size() > 0) image = getHolidayImage(state.activeHolidays[0])
            else if (state.activeCustomHolidays.size() > 0) image = getHolidayImage(state.activeCustomHolidays[0], true)
        }
        if (numActiveHolidays > 1) {
            rotateActiveHolidayImages()
        }
    }
    else if (anyScheduleActive()) {
        if (state.activeSchedules.size() > 0) {
            image = getScheduleImage(state.activeSchedules[0])
        }
        if (state.activeSchedules.size() > 1) {
            rotateActiveScheduleImages()
        }
    }
    else if (state.isNight && isNightConfigured()) {
        logDebug("Displaying image for night")
        // nighttime and treating night differently --> display nighttime image
        image = getNightImage()        
    }
    else if (isConfiguredWeather() || isConfiguredMode()) {
        if (prioritizeModeOrWeather == "Location Mode") {
            logDebug("Displaying image for location mode")
            // display location mode image
            image = getModeImage()
        }
        else if (prioritizeModeOrWeather == "Weather Conditions") {
            logDebug("Displaying image for weather conditions")
            // display weather condition image
            image = getWeatherImage()
        }
    }

    if (!image) {
         log.warn "No image for state. Reverting to default image if available."
       if (pathForDefault) image = pathForDefault
        else log.warn "No default image available. Image set to null."
    }
    if (image) logDebug "Updating image to " + image
    state.image = image
    def child = getChildDevice("DynamicImageSwitcherDevice${app.id}")
    child.configureURL(image)
}
    
def getNightImage() {
    def retPath = null
    if (nightHandling == "Use Specific Image") {
        retPath = pathForNight
    }
    else if (nightHandling == "Use Images per Moon Phase") {
        retPath = getMoonImage()
    }
    else if (nightHandling == "Use Nighttime Weather Images") {
        retPath = getWeatherImage()
    }
    return retPath
}

def getMoonImage() {
    return basePathForMoonImages ? basePathForMoonImages + settings["pathForPhase_" + state.moonPhase] : settings["pathForPhase_" + state.moonPhase]
}

def isConfiguredWeather() {
    logDebug("Checking whether image configured for weather conditions")
    def isWeather = false
    def weatherCodes = translateConditionsToCodes(weatherConditions)
    logDebug("isConfiguredWeather: current weather code is: " + state.weatherCode + " Checking against weather codes: " + weatherCodes)
    for (code in weatherCodes) {
        if (state.weatherCode && (state.weatherCode == code || state.weatherCode == "nt_" + code)) {
            isWeather = true
        }
    }
    logDebug("isConfiguredWeather returning " + isWeather)
    return isWeather
}

def translateCodeToCondition(codeToTranslate) {
    def foundCondition = null
    weatherMap.each { cond, code ->
        if (codeToTranslate == code) foundCondition = cond
    }
    return foundCondition
}

def translateConditionsToCodes(conditions) {
    def codes = []
    for (condition in conditions) {
        if (weatherMap[condition]) codes.add(weatherMap[condition])
    }
    return codes
}
def getWeatherImage() {
    def weatherImage = null
    def weatherCodes = translateConditionsToCodes(weatherConditions)
    if (!areSeasonsConfigured() && !seperateNightWeather) {
        logDebug("No seasons configured. No separate nighttime weather configured.")
        for (code in weatherCodes) {
            if (state.weatherCode == code || state.weatherCode == "nt_" + code) {
                logDebug("Setting image to ${code} image")
                weatherImage = settings["pathForWeather_${code}"]                       
            }
        }
    }
    else if (!areSeasonsConfigured() && seperateNightWeather) {
        logDebug("No seasons configured. But separate nighttime weather configured.")
        for (code in weatherCodes) {
            if (!state.isNight) {
                if (state.weatherCode == code) {
                    logDebug("Setting image to ${code} daytime image")
                    weatherImage = settings["pathForWeather_${code}_day"]   
                }
            }
            else if (state.isNight) {
                if (state.weatherCode == "nt_" + code) {
                    logDebug("Setting image to ${code} nighttime image")
                    weatherImage = settings["pathForWeather_${code}_night"]
                }
           }
        }
    }
    else if (areSeasonsConfigured() && !seperateNightWeather) {
        logDebug("Seasons configured. No separate nighttime weather configured.")
        for (code in weatherCodes) {
            if (state.weatherCode == code || state.weatherCode == "nt_" + code) {
                logDebug("Setting image to ${code} image")
                weatherImage = settings["pathForWeather_${code}_season${state.season}"]
            }
        }
    }
    else if (areSeasonsConfigured() && seperateNightWeather) {
        logDebug("Seasons configured. And separate nighttime weather configured.")
        for (code in weatherCodes) {            
            if (!state.isNight) {
                if (state.weatherCode == code) {
                    logDebug("Setting image to ${code} daytime image")
                    weatherImage = settings["pathForWeather_${code}_season${state.season}_day"]   
                }
            }
            else if (state.isNight) {
                if (state.weatherCode == "nt_" + code) {
                    logDebug("Setting image to ${code} nighttime image")
                    weatherImage = settings["pathForWeather_${code}_season${state.season}_night"]
                }
            }
        }
    }
    if (weatherImage == null) return null
    
    return basePathForWeatherImages ? basePathForWeatherImages + weatherImage : weatherImage
}

def updateSeason() {
    setSeason()
    updateImage()
}

def setSeason() {
    // TO DO check to make sure user entered in valid, non-overlapping dates for seasons
    def today = timeToday(null, location.timeZone)
	def month = today.month+1
	def day = today.date
    def season = 0 
    
    for (def i = 1; i <= seasonCount; i++) {
        def seasonStartMonth = monthsMap[settings["seasonStartMonth${i}"]]
        def seasonStartDay = settings["seasonStartDay${i}"].toInteger()
        def seasonEndMonth = monthsMap[settings["seasonEndMonth${i}"]]
        def seasonEndDay = settings["seasonEndDay${i}"].toInteger()

        if (seasonEndMonth < seasonStartMonth) {
           // for example, season starts in December (month 12) but ends in February (month 2)
            if (month == seasonStartMonth && day >= seasonStartDay) {
                // for example, if the season starts November 21 and ends February 21, then in the season if month is November and date is after 21
                season = i
            }
            else if (month > seasonStartMonth) {
                // for example, if the season starts November 21 and ends February 21, then in the season if month is December
                season = i
            }
            else if (month < seasonStartMonth) {
                if (month == seasonEndMonth && day <= seasonEndDay) {
                    // for example, if the season starts December 21 and ends February 21, then in the season if the date is between February 1 and February 21
                    season = i
                }
                else if (month < seasonEndMonth) {
                    // for example, if the season starts December 21 and ends February 21, then in the season if the month is January
                    season = i
                }
            }
        }
        else if (seasonEndMonth > seasonStartMonth) {    
            if (month == seasonStartMonth && day >= seasonStartDay)  {
                season = i
            }
            else if (month > seasonStartMonth && month < seasonEndMonth) {
                season = i
            }
            else if (month == seasonEndMonth && day <= seasonEndDay) {
                season = i
            }
        }
        else if (seasonEndMonth == seasonStartMonth) {   
            log.warn "Season starts and ends in the same month?"
            if ((month == seasonStartMonth && day >= seasonStartDay) && (month == seasonEndMonth && day <= seasonEndDay))  {
                season = i
            }
        }
    }
    if (season == 0) log.error "No season found."
    state.season = season
}

def setMoonPhase() {
    
    moonDevice.getPhase()
    state.moonPhase = moonDevice.currentValue("moonPhase")
    
}

def setDayOfWeek() {
    def date = new Date()
    state.dayOfWeek = date.format("EEEE")
}

def updateDayOfWeek() {
    setDayOfWeek()
    updateImage()
}

def updateHoliday() {
    setHoliday()
    updateImage()
}

def setHoliday()
{
	def today = timeToday(null, location.timeZone)
	def year = today.year+1900
	def month = today.month+1
	def day = today.date
	
	// Refresh the holidays for this year
	if (month == 1 && day == 1)
		getHolidays()
	
    state.activeHolidays = [] 
    state.activeCustomHolidays = [] 
    
	setPreconfiguredHolidays(calNational, state.nationalHolidaysList, year, month, day)
	setPreconfiguredHolidays(calReligious, state.religiousHolidaysList, year, month, day)
	setPreconfiguredHolidays(calObservances, state.observanceHolidaysList, year, month, day)
	setCustomHolidays(year, month, day)
}

def setCustomHolidays(year, month, day)
{
	if (customHolidays && customHolidayCount > 0)
	{
		for (def i = 0; i < customHolidayCount; i++)
		{
            def holidayMonth = monthsMap[settings["customHolidayMonth${i}"]] as Integer
            def holidayDay = settings["customHolidayDay${i}"] as Integer
            if (month == holidayMonth && day == holidayDay)  {
                // ignore year for yearly recurrence
				state.activeCustomHolidays.add(i)
            }
		}
	}
}

def setPreconfiguredHolidays(holidayList, fulllist, year, month, day)
{
	for (holiday in holidayList)
	{
		def holidayDate = fulllist[holiday]
		if (holidayDate.year == year && holidayDate.month == month && holidayDate.day == day)
			state.activeHolidays.add(holiday)
	}

}

def extractHolidays(holidayList)
{
	def result = [:]
	
	for(holiday in holidayList)
	{
		result[holiday.name] = holiday.name
	}
	
	return result
}

def extractHolidayDetails(holidayList)
{
	def result = [:]
	
	for(holiday in holidayList)
	{
		result[holiday.name] = holiday.date.datetime
	}
	
	return result
}

def getHolidays()
{
	state.nationalHolidays = [:]
	state.religiousHolidays = [:]
	state.observanceHolidays = [:]
    
	state.nationalHolidaysList = [:]
	state.religiousHolidaysList = [:]
	state.observanceHolidaysList = [:]
    
    def result = sendApiRequest("national", "GET")
	if (result.status == 200) {
		state.nationalHolidays = extractHolidays(result.data.response.holidays)
		state.nationalHolidaysList = extractHolidayDetails(result.data.response.holidays)
	}
		
	result = sendApiRequest("religious", "GET")
	if (result.status == 200) {
		state.religiousHolidays = extractHolidays(result.data.response.holidays)
		state.religiousHolidaysList = extractHolidayDetails(result.data.response.holidays)
	}
		
	result = sendApiRequest("observance", "GET")
	if (result.status == 200) {
		state.observanceHolidays = extractHolidays(result.data.response.holidays)
		state.observanceHolidaysList = extractHolidayDetails(result.data.response.holidays)
	}
}

def sendApiRequest(type, method)
{
    def params = [
		uri: "https://calendarific.com",
        path: "/api/v2/holidays",
		contentType: "application/json",
		query: [
                api_key: calAPIToken,
				country: calCountry,
				year: timeToday(null, location.timeZone).year + 1900,
				type: type
            ],
		timeout: 300
	]

    if (body != null)
        params.body = body
    
    def result = null
    if (method == "GET") {
        httpGet(params) { resp ->
            result = resp
        }
    }
    else if (method == "POST") {
        httpPost(params) { resp ->
            result = resp
        }
    }
    return result
}


def logDebug(msg) {
    if (settings?.debugOutput) {
		log.debug msg
	}
}
    

def getInterface(type, txt="", link="") {
    switch(type) {
        case "line": 
            return "<hr style='background-color:#555555; height: 1px; border: 0;'></hr>"
            break
        case "header": 
            return "<div style='color:#ffffff;font-weight: bold;background-color:#555555;border: 1px solid;box-shadow: 2px 3px #A9A9A9'> ${txt}</div>"
            break
        case "error": 
            return "<div style='color:#ff0000;font-weight: bold;'>${txt}</div>"
            break
        case "note": 
            return "<div style='color:#333333;font-size: small;'>${txt}</div>"
            break
        case "subField":
            return "<div style='color:#000000;background-color:#ededed;'>${txt}</div>"
            break     
        case "subHeader": 
            return "<div style='color:#000000;font-weight: bold;background-color:#ededed;border: 1px solid;box-shadow: 2px 3px #A9A9A9'> ${txt}</div>"
            break
        case "subSection1Start": 
            return "<div style='color:#000000;background-color:#d4d4d4;border: 0px solid'>"
            break
        case "subSection2Start": 
            return "<div style='color:#000000;background-color:#e0e0e0;border: 0px solid'>"
            break
        case "subSectionEnd":
            return "</div>"
            break
        case "boldText":
            return "<b>${txt}</b>"
            break
        case "link":
            return '<a href="' + link + '" target="_blank" style="color:#51ade5">' + txt + '</a>'
            break
    }
} 

@Field alertOptions = [
    "Tornado Warning",
    "Earthquake Warning",
    "Flash Flood Warning",
    "Fire Warning",
    "Severe Thunderstorm Warning",
    "Blizzard Warning",
    "Ice Storm Warning",
    "Winter Storm Warning",
    "High Wind Warning",
    "Tornado Watch", 
    "Flood Watch",
    "Severe Thunderstorm Watch"
]

@Field weatherList = [ "Clear", "Partly Cloudy", "Mostly Cloudy", "Cloudy", "Rain", "Thunderstorm", "Flurries", "Snow", "Sleet"]

@Field weatherMap = [
    "Clear" : "clear",
    "Partly Cloudy" : "partlycloudy",
    "Mostly Cloudy" : "mostlycloudy",
    "Cloudy" : "cloudy",
    "Rain" : "rain",
    "Thunderstorm" : "chancetstorms",
    "Flurries" : "flurries",
    "Snow" : "snow",
    "Sleet" : "sleet"
    ]

@Field countryList = [
'us': 'United States',
'af': 'Afghanistan',
'al': 'Albania',
'dz': 'Algeria',
'as': 'American Samoa',
'ad': 'Andorra',
'ao': 'Angola',
'ai': 'Anguilla',
'ag': 'Antigua and Barbuda',
'ar': 'Argentina',
'am': 'Armenia',
'aw': 'Aruba',
'au': 'Australia',
'at': 'Austria',
'az': 'Azerbaijan',
'bh': 'Bahrain',
'bd': 'Bangladesh',
'bb': 'Barbados',
'by': 'Belarus',
'be': 'Belgium',
'bz': 'Belize',
'bj': 'Benin',
'bm': 'Bermuda',
'bt': 'Bhutan',
'bo': 'Bolivia',
'ba': 'Bosnia and Herzegovina',
'bw': 'Botswana',
'br': 'Brazil',
'vg': 'British Virgin Islands',
'bn': 'Brunei',
'bg': 'Bulgaria',
'bf': 'Burkina Faso',
'bi': 'Burundi',
'cv': 'Cabo Verde',
'kh': 'Cambodia',
'cm': 'Cameroon',
'ca': 'Canada',
'ky': 'Cayman Islands',
'cf': 'Central African Republic',
'td': 'Chad',
'cl': 'Chile',
'cn': 'China',
'co': 'Colombia',
'km': 'Comoros',
'cg': 'Congo',
'cd': 'Congo Democratic Republic',
'ck': 'Cook Islands',
'cr': 'Costa Rica',
'ci': 'Cote d\'Ivoire',
'hr': 'Croatia',
'cu': 'Cuba',
'cw': 'Curaçao',
'cy': 'Cyprus',
'cz': 'Czech Republic',
'dk': 'Denmark',
'dj': 'Djibouti',
'dm': 'Dominica',
'do': 'Dominican Republic',
'tl': 'East Timor',
'ec': 'Ecuador',
'eg': 'Egypt',
'sv': 'El Salvador',
'gq': 'Equatorial Guinea',
'er': 'Eritrea',
'ee': 'Estonia',
'et': 'Ethiopia',
'fk': 'Falkland Islands',
'fo': 'Faroe Islands',
'fj': 'Fiji',
'fi': 'Finland',
'fr': 'France',
'pf': 'French Polynesia',
'ga': 'Gabon',
'gm': 'Gambia',
'ge': 'Georgia',
'de': 'Germany',
'gh': 'Ghana',
'gi': 'Gibraltar',
'gr': 'Greece',
'gl': 'Greenland',
'gd': 'Grenada',
'gu': 'Guam',
'gt': 'Guatemala',
'gg': 'Guernsey',
'gn': 'Guinea',
'gw': 'Guinea-Bissau',
'gy': 'Guyana',
'ht': 'Haiti',
'va': 'Holy See (Vatican City)',
'hn': 'Honduras',
'hk': 'Hong Kong',
'hu': 'Hungary',
'is': 'Iceland',
'in': 'India',
'id': 'Indonesia',
'ir': 'Iran',
'iq': 'Iraq',
'ie': 'Ireland',
'im': 'Isle of Man',
'il': 'Israel',
'it': 'Italy',
'jm': 'Jamaica',
'jp': 'Japan',
'je': 'Jersey',
'jo': 'Jordan',
'kz': 'Kazakhstan',
'ke': 'Kenya',
'ki': 'Kiribati',
'xk': 'Kosovo',
'kw': 'Kuwait',
'kg': 'Kyrgyzstan',
'la': 'Laos',
'lv': 'Latvia',
'lb': 'Lebanon',
'ls': 'Lesotho',
'lr': 'Liberia',
'ly': 'Libya',
'li': 'Liechtenstein',
'lt': 'Lithuania',
'lu': 'Luxembourg',
'mo': 'Macau',
'mg': 'Madagascar',
'mw': 'Malawi',
'my': 'Malaysia',
'mv': 'Maldives',
'ml': 'Mali',
'mt': 'Malta',
'mh': 'Marshall Islands',
'mq': 'Martinique',
'mr': 'Mauritania',
'mu': 'Mauritius',
'yt': 'Mayotte',
'mx': 'Mexico',
'fm': 'Micronesia',
'md': 'Moldova',
'mc': 'Monaco',
'mn': 'Mongolia',
'me': 'Montenegro',
'ms': 'Montserrat',
'ma': 'Morocco',
'mz': 'Mozambique',
'mm': 'Myanmar',
'na': 'Namibia',
'nr': 'Nauru',
'np': 'Nepal',
'nl': 'Netherlands',
'nc': 'New Caledonia',
'nz': 'New Zealand',
'ni': 'Nicaragua',
'ne': 'Niger',
'ng': 'Nigeria',
'kp': 'North Korea',
'mk': 'North Macedonia',
'mp': 'Northern Mariana Islands',
'no': 'Norway',
'om': 'Oman',
'pk': 'Pakistan',
'pw': 'Palau',
'pa': 'Panama',
'pg': 'Papua New Guinea',
'py': 'Paraguay',
'pe': 'Peru',
'ph': 'Philippines',
'pl': 'Poland',
'pt': 'Portugal',
'pr': 'Puerto Rico',
'qa': 'Qatar',
're': 'Reunion',
'ro': 'Romania',
'ru': 'Russia',
'rw': 'Rwanda',
'sh': 'Saint Helena',
'kn': 'Saint Kitts and Nevis',
'lc': 'Saint Lucia',
'mf': 'Saint Martin',
'pm': 'Saint Pierre and Miquelon',
'vc': 'Saint Vincent and the Grenadines',
'ws': 'Samoa',
'sm': 'San Marino',
'st': 'Sao Tome and Principe',
'sa': 'Saudi Arabia',
'sn': 'Senegal',
'rs': 'Serbia',
'sc': 'Seychelles',
'sl': 'Sierra Leone',
'sg': 'Singapore',
'sx': 'Sint Maarten',
'sk': 'Slovakia',
'si': 'Slovenia',
'sb': 'Solomon Islands',
'so': 'Somalia',
'za': 'South Africa',
'kr': 'South Korea',
'ss': 'South Sudan',
'es': 'Spain',
'lk': 'Sri Lanka',
'bl': 'St. Barts',
'sd': 'Sudan',
'sr': 'Suriname',
'se': 'Sweden',
'ch': 'Switzerland',
'sy': 'Syria',
'tw': 'Taiwan',
'tj': 'Tajikistan',
'tz': 'Tanzania',
'th': 'Thailand',
'bs': 'The Bahamas',
'tg': 'Togo',
'to': 'Tonga',
'tt': 'Trinidad and Tobago',
'tn': 'Tunisia',
'tr': 'Turkey',
'tm': 'Turkmenistan',
'tc': 'Turks and Caicos Islands',
'tv': 'Tuvalu',
'vi': 'US Virgin Islands',
'ug': 'Uganda',
'ua': 'Ukraine',
'ae': 'United Arab Emirates',
'gb': 'United Kingdom',
'uy': 'Uruguay',
'uz': 'Uzbekistan',
'vu': 'Vanuatu',
've': 'Venezuela',
'vn': 'Vietnam',
'wf': 'Wallis and Futuna',
'ye': 'Yemen',
'zm': 'Zambia',
'zw': 'Zimbabwe',
'sz': 'eSwatini'
]
