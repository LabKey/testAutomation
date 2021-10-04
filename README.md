## LabKey Selenium WebDriver Test Library
This repository contains base test classes and helpers for LabKey's functional Selenium tests.

### Test Setup
1. Clone this into the `server` directory of your [LabKey](https://github.com/LabKey/server) enlistment. More details on setting up a development environment can be found on [labkey.org](https://www.labkey.org/Documentation/wiki-page.view?name=devMachine).

1. Install web browser and browser driver. In order to run the Selenium tests, you will need a web browser and the corresponding browser driver. You should install the newest driver compatible with your browser. Driver executable should be available on your system PATH. 

    - Firefox
        - [Geckodriver](https://github.com/mozilla/geckodriver/releases)
        - _Note: [Firefox ESR](https://www.mozilla.org/en-US/firefox/all/#product-desktop-esr) is recommended_
    - Google Chrome
        - [Chromedriver](https://sites.google.com/chromium.org/driver/)
        - _Note: Updates frequently and should be kept in sync with Chrome browser version_
    - Other Browsers
        - Edge, Safari, Opera, etc. are not currently supported.

1. Run `./gradlew :server:testAutomation:initProperties` to generate the `test.properties` file. \
    - _Note: If setting up a fresh development environment, this will run automatically with the root `ijConfigure` task_
    - Update `selenium.browser` to specify the browser you want to run against ("chrome" or "firefox")

1. Verify test setup
    1. Build LabKey and start server
    1. Run tests: `./gradlew :server:testAutomation:uiTests -Psuite=DRT`
