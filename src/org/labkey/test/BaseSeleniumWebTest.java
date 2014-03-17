/*
 * Copyright (c) 2007-2014 LabKey Corporation
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
 */

package org.labkey.test;

import com.thoughtworks.selenium.DefaultSelenium;
import com.thoughtworks.selenium.SeleniumException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.http.HttpException;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.ContainerFilter;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.test.util.*;
import org.labkey.test.util.ext4cmp.Ext4CmpRef;
import org.labkey.test.util.ext4cmp.Ext4FieldRef;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.wc.SVNStatusClient;
import org.tmatesoft.svn.core.wc.SVNStatusType;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

import static org.labkey.test.WebTestHelper.*;
import static org.labkey.test.TestProperties.*;

import static org.junit.Assert.*;

/**
 * @deprecated Use {@link BaseWebDriverTest}
 */
@Deprecated
public abstract class BaseSeleniumWebTest implements Cleanable, WebTest
{
    @Deprecated protected DefaultSeleniumWrapper selenium;
    private static final int DEFAULT_SELENIUM_PORT = 4444;
    private static final String DEFAULT_SELENIUM_SERVER = "localhost";
    private String _lastPageTitle = null;
    private URL _lastPageURL = null;
    private String _lastPageText = null;
    private Stack<String> _locationStack = new Stack<>();
    private String _savedLocation = null;
    private Stack<String> _impersonationStack = new Stack<>();
    private List<FolderIdentifier> _createdFolders = new ArrayList<>();
    protected boolean _testFailed = true;
    protected boolean _testTimeout = false;
    public final static int WAIT_FOR_PAGE = 60000;
    public int defaultWaitForPage = WAIT_FOR_PAGE;
    public final static int WAIT_FOR_JAVASCRIPT = 20000;
    public int longWaitForPage = defaultWaitForPage * 5;
    private boolean _fileUploadAvailable;
    protected long _startTime;

    public AbstractContainerHelper _containerHelper = new APIContainerHelper(this);
    public ExtHelper _extHelper = new ExtHelper(this);
    public Ext4Helper _ext4Helper = new Ext4Helper(this);
    public FileBrowserHelper _fileBrowserHelper = new FileBrowserHelper(this);
    public CustomizeViewsHelper _customizeViewsHelper = new CustomizeViewsHelper(this);
    public StudyHelper _studyHelper = new StudyHelper(this);
    public ListHelper _listHelper = new ListHelper(this);
    public AbstractUserHelper _userHelper = new APIUserHelper(this);
    public AbstractAssayHelper _assayHelper = new APIAssayHelper(this);

    private static final int MAX_SERVER_STARTUP_WAIT_SECONDS = 60;
    protected static final int MAX_WAIT_SECONDS = 10 * 60;

    public static final String TRICKY_CHARACTERS = "><&/%\\' \"1äöü";
    public static final String TRICKY_CHARACTERS_NO_QUOTES = "></% 1äöü";
    public static final String TRICKY_CHARACTERS_FOR_PROJECT_NAMES = "\u2603~!@$&()_+{}-=[],.#äöü";

    public static final String INJECT_CHARS_1 = "\"'>--><script>alert('8(');</script>;P";
    public static final String INJECT_CHARS_2 = "\"'>--><img src=xss onerror=alert(\"8(\")>\u2639";

    public final static String FIREFOX_BROWSER = "*firefox";
    private final static String FIREFOX_UPLOAD_BROWSER = "*chrome";
    public final static String IE_BROWSER = "*iexploreproxy";
    //protected final static String IE_UPLOAD_BROWSER = "*iehta";

    /** Have we already done a memory leak and error check in this test harness VM instance? */
    protected static boolean _checkedLeaksAndErrors = false;
    private static final String ACTION_SUMMARY_TABLE_NAME = "actions";


    protected static final String PERMISSION_ERROR = "User does not have permission to perform this operation";


    public BaseSeleniumWebTest()
    {

    }

    public static int getSeleniumServerPort()
    {
        String portString = System.getProperty("selenium.server.port", "" + DEFAULT_SELENIUM_PORT);
        return Integer.parseInt(portString);
    }

    public static String getSeleniumServer()
    {
        return System.getProperty("selenium.server", DEFAULT_SELENIUM_SERVER);
    }

    @Deprecated
    public DefaultSeleniumWrapper getWrapper()
    {
        return selenium;
    }

    public static String getLabKeyRoot()
    {
        return WebTestHelper.getLabKeyRoot();
    }

    public static String getSampledataPath()
    {
        File path = new File(getLabKeyRoot(), "sampledata");
        return path.toString();
    }

    /**
     *
     * @return
     */
    public static File getApiScriptFolder()
    {
        return new File(getLabKeyRoot(), "server/test/data/api");
    }

    public static String getContextPath()
    {
        return WebTestHelper.getContextPath();
    }

    protected abstract @Nullable String getProjectName();

    @Before
    public void setUp() throws Exception
    {
        // Make sure LabKey server is responding.
        // The Selenium constructor will hang if it is not.
        // java.net.SocketTimeoutException is the likely failure.
        WebTestHelper.getHttpGetResponse(WebTestHelper.getBaseURL());

        selenium = new DefaultSeleniumWrapper();
        selenium.start();
        selenium.setTimeout(Integer.toString(defaultWaitForPage));
        //Now inject our standard javascript functions...
        InputStream inputStream = BaseSeleniumWebTest.class.getResourceAsStream("seleniumHelpers.js");
        String script = getStreamContentsAsString(inputStream);
        System.out.println("Loading scripts from seleniumHelpers.js");
        System.out.println(selenium.getEval(script));

        if (isScriptCheckEnabled())
            beginJsErrorChecker();
    }

    public void beginJsErrorChecker()
    {
        selenium.getEval("selenium.doBeginJsErrorChecker();");
    }

    public void endJsErrorChecker()
    {
        selenium.getEval("selenium.doEndJsErrorChecker();");
    }

    private static int _jsErrorPauseCount = 0;
    public void pauseJsErrorChecker()
    {
        selenium.getEval("selenium.pauseJsErrorChecker();");
        _jsErrorPauseCount++;
    }

    public void resumeJsErrorChecker()
    {
        if (--_jsErrorPauseCount < 1)
            selenium.getEval("selenium.resumeJsErrorChecker();");
    }

    /**
     * Override if using file upload features in the test. Returning true will attempt to use
     * a version of the browser that allows file upload fields to be set. Defaults to false.
     * Use isFileUploadAvailable to see if request worked.
     */
    protected boolean isFileUploadTest()
    {
        return false;
    }

    public boolean isFileUploadAvailable()
    {
        return true;
    }

    protected boolean isPipelineToolsTest()
    {
        return false;
    }

    /**
     * Set pipeline tools directory to the default location if the current location does not exist.
      */
    @LogMethod
    private void fixPipelineToolsDirectory()
    {
        log("Ensuring pipeline tools directory points to the right place");
        goToHome();
        goToSiteSettings();
        File currentToolsDirectory = new File(getFormElement("pipelineToolsDirectory"));
        if(!currentToolsDirectory.exists())
        {
            log("Pipeline tools directory does not exist: " + currentToolsDirectory);
            File defaultToolsDirectory = new File(getLabKeyRoot() + "/build/deploy/bin");
            log("Setting to default tools directory" + defaultToolsDirectory.toString());
            setFormElement("pipelineToolsDirectory", defaultToolsDirectory.toString());
            clickButton("Save");
        }
    }

    public String getBrowser()
    {
        String seleniumBrowser = System.getProperty("selenium.browser", FIREFOX_BROWSER);
        if (seleniumBrowser.equals("*best"))
            seleniumBrowser = FIREFOX_BROWSER;

        String browserPath = System.getProperty("selenium.browser.path", "");
        if (browserPath.length() > 0)
            browserPath = " " + browserPath;

        //File upload is "experimental" in selenium, so only use it when
        //necessary
        if (isFileUploadTest())
        {
            // IE is currently unable to do a file upload
            if (seleniumBrowser.startsWith(IE_BROWSER))
            {
                log("Warning: Internet Explorer cannot do file uploads!");
                //browser = IE_UPLOAD_BROWSER;
                _fileUploadAvailable = false;
            }
            else if (seleniumBrowser.startsWith(FIREFOX_BROWSER))
            {
                seleniumBrowser = FIREFOX_UPLOAD_BROWSER;
                _fileUploadAvailable = true;
            }
        }
        return seleniumBrowser + browserPath;
    }

    static String getStreamContentsAsString(InputStream is) throws IOException
    {
        StringBuilder contents = new StringBuilder();
        BufferedReader input = null;

        try
        {
            input = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = input.readLine()) != null)
            {
                contents.append(line);
                contents.append("\n");
            }
        }
        finally
        {
            try
            {
                if (input != null) input.close();
            }
            catch (IOException e)
            {
                // Do nothing.
            }
        }
        return contents.toString();
    }

    public void copyFile(String original, String copy)
    {
        copyFile(new File(original), new File(copy));
    }

    public void copyFile(File original, File copy)
    {
        try
        {
            Files.createDirectories(Paths.get(copy.toURI()).getParent());
            Files.copy(Paths.get(original.toURI()), Paths.get(copy.toURI()),
                    StandardCopyOption.COPY_ATTRIBUTES,
                    StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }


    @After
    public void tearDown() throws Exception
    {
        if (selenium != null)
        {
            if (isScriptCheckEnabled())
            {
                dismissAlerts();
                endJsErrorChecker();
            }
            boolean skipTearDown = _testFailed && System.getProperty("close.on.fail", "true").equalsIgnoreCase("false");

            if (!skipTearDown || isTestRunningOnTeamCity())
            {
                //selenium.close(); // unnecessary since selenium.stop will close windows.
                selenium.stop();
            }
        }
    }

    public void log(String str)
    {
        str = str.replace(Locator.NOT_HIDDEN, "NOT_HIDDEN"); // This xpath fragment really clutters up the log
        TestLogger.log(str);
    }

    private static final Pattern LABKEY_ERROR_TITLE_PATTERN = Pattern.compile("\\d\\d\\d\\D.*Error.*", Pattern.CASE_INSENSITIVE);
    private static final Pattern TOMCAT_ERROR_PATTERN = Pattern.compile("HTTP Status\\s*(\\d\\d\\d)\\D");

    public int getResponseCode()
    {
        //We can't seem to get response codes via javascript, so we rely on default titles for error pages
        String title = selenium.getTitle();
        if (!title.toLowerCase().contains("error"))
            return 200;

        Matcher m = LABKEY_ERROR_TITLE_PATTERN.matcher(title);
        if (m.matches())
            return Integer.parseInt(title.substring(0, 3));

        //Now check the Tomcat page. This is going to be unreliable over time
        m = TOMCAT_ERROR_PATTERN.matcher(getResponseText());
        if (m.find())
            return Integer.parseInt(m.group(1));

        return 200;
    }

    public String getResponseText()
    {
        return selenium.getHtmlSource();
    }

    public URL getURL()
    {
        try
        {
            return new URL(selenium.getLocation());
        }
        catch (MalformedURLException x)
        {
            throw new RuntimeException("Bad location from selenium tester: " + selenium.getLocation(), x);
        }
    }

    public String[] getLinkAddresses()
    {
        String js = "selenium.getLinkAddresses();";
        String linkStr = selenium.getEval(js);
        String[] linkArray = linkStr.split("\\\\n");
        ArrayList<String> links = new ArrayList<>(linkArray.length);
        for (String link : linkArray)
        {
            if (link.contains("#"))
            {
                link = link.substring(0, link.indexOf("#"));
            }
            if (link != null && link.trim().length() > 0)
            {
                links.add(link);
            }
        }

        return links.toArray(new String[links.size()]);
    }


    public String getCurrentRelativeURL()
    {

        URL url = getURL();
        String urlString = selenium.getLocation();
        if ("80".equals(WebTestHelper.getWebPort()) && url.getAuthority().endsWith(":-1"))
        {
            int portIdx = urlString.indexOf(":-1");
            urlString = urlString.substring(0, portIdx) + urlString.substring(portIdx + (":-1".length()));
        }

        String baseURL = WebTestHelper.getBaseURL();
        assertTrue("Expected URL to begin with " + baseURL + ", but found " + urlString, urlString.indexOf(baseURL) == 0);
        return urlString.substring(baseURL.length());
    }

    public void pushLocation()
    {
        _locationStack.push(getCurrentRelativeURL());
    }

    public void popLocation()
    {
        popLocation(defaultWaitForPage);
    }

    public void popLocation(int millis)
    {
        String location = _locationStack.pop();
        assertNotNull("Cannot pop without a push.", location);
        beginAt(location, millis);
    }

    public void saveLocation()
    {
        _savedLocation = getCurrentRelativeURL();
    }

    public void recallLocation()
    {
        recallLocation(defaultWaitForPage);
    }

    public void recallLocation(int wait)
    {
        assertNotNull("Cannot recall without saving first.", _savedLocation);
        beginAt(_savedLocation, wait);
    }

    public void refresh()
    {
        refresh(defaultWaitForPage);
    }

    public void refresh(int millis)
    {
        selenium.refresh();
        waitForPageToLoad(millis);
    }

    public void goBack(int millis)
    {
        selenium.goBack();
        waitForPageToLoad(millis);
    }

    public void goBack()
    {
        goBack(defaultWaitForPage);
    }


    public void sleep(long ms)
    {
        try
        {
            Thread.sleep(ms);
        }
        catch (InterruptedException ignored)
        {
        }
    }

    @LogMethod
    public void signIn()
    {
        if ( isGuestModeTest() )
        {
            waitForStartup();
            log("Skipping sign in.  Test runs as guest.");
            beginAt("/login/logout.view");
            return;
        }

        try
        {
            PasswordUtil.ensureCredentials();
        }
        catch (IOException e)
        {
            fail("Unable to ensure credentials: " + e.getMessage());
        }
        waitForStartup();
        log("Signing in");
        //
        beginAt("/login/logout.view");
        checkForUpgrade();
        simpleSignIn();
        ensureAdminMode();
    }

    // Just sign in & verify -- don't check for startup, upgrade, admin mode, etc.
    public void simpleSignIn()
    {
        if ( isGuestModeTest() )
            return;
        ensureSignedOut();
        if (!isTitleEqual("Sign In"))
            beginAt("/login/login.view");

        // Sign in if browser isn't already signed in.  Otherwise, we'll be on the home page.
        if (isTitleEqual("Sign In"))
        {
            assertTitleEquals("Sign In");
            waitForElement(Locator.id("email"), defaultWaitForPage);
            assertElementPresent(Locator.tagWithName("form", "login"));
            setText("email", PasswordUtil.getUsername());
            setText("password", PasswordUtil.getPassword());
            clickAndWait(Locator.linkWithText("Sign In"));

            if (isTextPresent("Type in your email address and password"))
                fail("Could not log in with the saved credentials.  Please verify that the test user exists on this installation or reset the credentials using 'ant setPassword'");
        }

        assertSignOutAndMyAccountPresent();

        if (isElementPresent(Locator.tag("span").withClass("labkey-nav-page-header").withText("Startup Modules"))||
            isElementPresent(Locator.tag("span").withClass("labkey-nav-page-header").withText("Upgrade Modules")))
        {
            waitForElement(Locator.id("status-progress-bar").withText("Module startup complete"));
            clickButton("Next");
        }
    }

    public void assertSignOutAndMyAccountPresent()
    {
        click(Locator.id("userMenuPopupLink"));
        assertTextNotPresent("Sign In");
//        assertTextPresent("My Account");
    }

    // Just sign in & verify -- don't check for startup, upgrade, admin mode, etc.
    public void signIn(String email, String password, boolean failOnError)
    {
        if ( !isLinkPresentWithText("Sign In") )
            fail("You need to be logged out to log in.  Please log out to log in.");

        clickAndWait(Locator.linkWithText("Sign In"));

        attemptSignIn(email, password);

        if ( failOnError )
        {
            if ( isTextPresent("Type in your email address and password") )
                fail("Could not log in with the saved credentials.  Please verify that the test user exists on this installation or reset the credentials using 'ant setPassword'");

            assertSignOutAndMyAccountPresent();
        }
    }


    public void attemptSignIn(String email, String password)
    {
        clickAndWait(Locator.linkWithText("Sign In"));

        assertTitleEquals("Sign In");
        assertElementPresent(Locator.tagWithName("form", "login"));
        setText("email", email);
        setText("password", password);
        clickAndWait(Locator.linkWithText("Sign In"));
    }

    public void signInShouldFail(String email, String password, String... expectedMessages)
    {
        attemptSignIn(email, password);
        assertTitleEquals("Sign In");
        assertElementPresent(Locator.tagWithName("form", "login"));

        assertTextPresent(expectedMessages);
    }

    protected void setInitialPassword(String user, String password)
    {
        // Get setPassword URL from notification email.
        goToModule("Dumbster");
        clickAndWait(Locator.xpath("//table[@id='dataregion_EmailRecord']//td[text() = '" + user + "']/..//a[contains(@href, 'setPassword.view')]"));

        setFormElement("password", password);
        setFormElement("password2", password);

        clickButton("Set Password");
    }

    protected String getPasswordResetUrl(String username)
    {
        goToHome();
        goToModule("Dumbster");
        clickAndWait(Locator.xpath("//table[@id='dataregion_EmailRecord']//td[text() = '" + username + "']/..//a[contains(@href, 'setPassword.view')]"));

        return getCurrentRelativeURL();
    }

    protected void resetPassword(String resetUrl, String username, String newPassword)
    {
        if(resetUrl!=null)
            beginAt(resetUrl);

        assertTextPresent(username, "Choose a password you'll use to access this server", "six non-whitespace characters or more, cannot match email address");

        setFormElement("password", newPassword);
        setFormElement("password2", newPassword);

        clickButton("Set Password");

        if(!isElementPresent(Locator.id("userMenuPopupLink")))
        {
            clickButtonContainingText("Submit", defaultWaitForPage*3);
            clickButton("Done");

            signOut();
            signIn(username, newPassword, true);
        }
    }

    protected void changePassword(String oldPassword, String password)
    {
        goToMyAccount();
        clickButton("Change Password");

        setFormElement("oldPassword", oldPassword);
        setFormElement("password", password);
        setFormElement("password2", password);

        clickButton("Set Password");
    }


    /**
     * change user's e-mail from userEmail to newUserEmail from admin console
     */
    protected void changeUserEmail(String userEmail, String newUserEmail)
    {
        log("Attempting to change user email from " + userEmail + " to " + newUserEmail);


        goToSiteUsers();
        clickAndWait(Locator.linkContainingText(displayNameFromEmail(userEmail)));

        clickButton("Change Email");

        setFormElement("newEmail", newUserEmail);
        clickButton("Submit");
    }


    protected enum PasswordRule {Weak, Strong}
    protected enum PasswordExpiration {Never, FiveSeconds, ThreeMonths, SixMonths, OneYear}

    private PasswordRule oldStrength = null;
    private PasswordExpiration oldExpiration = null;
    protected void setDbLoginConfig(PasswordRule strength, PasswordExpiration expiration)
    {
        PasswordRule curStrength = null;
        PasswordExpiration curExpiration = null;

        pushLocation();

        beginAt("/login/configureDbLogin.view");


        if ( oldStrength == null || oldExpiration == null )
        {
            // Remember old login settings.
            curStrength = PasswordRule.valueOf(getText(Locator.xpath("//input[@name='strength' and @value='Weak']/.."))); // getAttribute broken on IE
            curExpiration = PasswordExpiration.valueOf(getFormElement("expiration"));
        }

        if ( strength != null && curStrength != strength)
        {
            if ( oldStrength == null ) oldStrength = curStrength;
            click(Locator.radioButtonByNameAndValue("strength", strength.toString()));
        }

        if ( expiration != null && curExpiration != expiration)
        {
            if ( oldExpiration == null ) oldExpiration = curExpiration;
            setFormElement("expiration", expiration.toString());
        }

        clickButton("Save");

        popLocation();
    }

    @LogMethod
    protected void resetDbLoginConfig()
    {
        if ( oldStrength != null || oldExpiration != null )
        {
            pushLocation();

            if (isElementPresent(Locator.id("userMenuPopupLink")))
            {
                click(Locator.id("userMenuPopupLink"));
                if (isTextPresent("Stop Impersonating"))
                {
                     stopImpersonating();
                }
            }

            beginAt("/login/configureDbLogin.view");

            if ( oldStrength != null ) click(Locator.radioButtonByNameAndValue("strength", oldStrength.toString()));
            if ( oldExpiration != null ) setFormElement("expiration", oldExpiration.toString());

            clickButton("Save");

            if ( oldStrength != null ) assertEquals("Unable to reset password strength.", oldStrength, PasswordRule.valueOf(getText(Locator.xpath("//input[@name='strength' and @value='Weak']/.."))));
            if ( oldExpiration != null ) assertEquals("Unable to reset password expiration.", oldExpiration, PasswordExpiration.valueOf(getFormElement("expiration")));

            // Back to default.
            oldStrength = null;
            oldExpiration = null;

            popLocation();
        }
    }

    protected void setSystemMaintenance(boolean enable)
    {
        // Not available in production mode
        if (isDevModeEnabled())
        {
            goToAdminConsole();
            clickAndWait(Locator.linkWithText("system maintenance"));

            if (enable)
                checkCheckbox("enableSystemMaintenance");
            else
                uncheckCheckbox("enableSystemMaintenance");

            clickButton("Save");
        }
    }

    public void ensureAdminMode()
    {
        //Now switch to admin mode if available
        //TODO:  this is causing all kinds of problems
//        if (!isElementPresent(Locator.id("leftmenupanel")) && !(isElementPresent(Locator.id("Admin ConsoleTab"))))
//            clickAdminMenuItem("Show Navigation Bar");
    }

    public void goToAdminConsole()
    {
        goToHome();
        clickAdminMenuItem("Site", "Admin Console");
    }

    public void goToSiteSettings()
    {
        goToAdminConsole();
        clickAndWait(Locator.linkWithText("site settings"));
    }

    public void goToAuditLog()
    {
        goToAdminConsole();
        clickAndWait(Locator.linkWithText("audit log"));
    }

    public void hideNavigationBar()
    {
        clickAndWait(Locator.xpath("//a/span[text() = 'Admin']"), 0);
        waitForElement(Locator.tagContainingText("span", "Navigation Bar"), 1000);
        if (isElementPresent(Locator.tagContainingText("span", "Hide Navigation Bar")))
            clickAndWait(Locator.tagContainingText("span", "Hide Navigation Bar"));
    }

    public void showNavigationBar()
    {
        clickAndWait(Locator.xpath("//a/span[text() = 'Admin']"), 0);
        waitForElement(Locator.tagContainingText("span", "Navigation Bar"), 1000);
        if (isElementPresent(Locator.tagContainingText("span", "Show Navigation Bar")))
            clickAndWait(Locator.tagContainingText("span", "Show Navigation Bar"));
    }

    /**
     * Allows test code to navigate to a Webpart Ext-based navigation menu.
     * @param webPartTitle title (not name) of webpart to be clicked.  Multiple web parts with the same title not supported.
     * @param items
     */
    public void clickWebpartMenuItem(String webPartTitle, String... items)
    {
        clickWebpartMenuItem(webPartTitle, true, items);
    }

    public void clickWebpartMenuItem(String webPartTitle, boolean wait, String... items)
    {
        _extHelper.clickExtMenuButton(wait, Locator.xpath("//img[@id='more-" + webPartTitle.toLowerCase() + "']"), items);
    }

    // Clicks admin menu items. Tests should use helpers to make admin menu changes less disruptive.
    protected void clickAdminMenuItem(String... items)
    {
        waitForElement(Locators.ADMIN_MENU);
        _ext4Helper.clickExt4MenuButton(true, Locators.ADMIN_MENU, false, items);
    }

    public void clickUserMenuItem(String... items)
    {
        clickUserMenuItem(true, false, items);
    }

    public void clickUserMenuItem(boolean wait, boolean onlyOpen, String... items)
    {
        waitForElement(Locators.USER_MENU);
        _ext4Helper.clickExt4MenuButton(true, Locators.USER_MENU, onlyOpen, items);
    }

    // Click on a module listed on the admin menu
    public void goToModule(String moduleName)
    {
        clickAdminMenuItem("Go To Module", "More Modules", moduleName);
    }

    public void goToSchemaBrowser()
    {
        goToModule("Query");
    }

    public void goToFolderManagement()
    {
        clickAdminMenuItem("Folder", "Management");
    }

    public void goToProjectSettings()
    {
        clickAdminMenuItem("Folder", "Project Settings");
    }

    public void goToSiteUsers()
    {
        clickAdminMenuItem("Site", "Site Users");
    }

    public void goToSiteGroups()
    {
        clickAdminMenuItem("Site", "Site Groups");
    }

    public void goToSiteDevelopers()
    {
        clickAdminMenuItem("Site", "Site Developers");
    }

    public void goToSiteAdmins()
    {
        clickAdminMenuItem("Site", "Site Admins");
    }

    public void goToManageViews()
    {
        clickAdminMenuItem("Manage Views");
        waitForElement(Locator.xpath("//*[starts-with(@id, 'dataviews-panel')]"));
    }

    public void goToManageStudy()
    {
        clickAdminMenuItem("Manage Study");
    }

    public void goToManageAssays()
    {
        clickAdminMenuItem("Manage Assays");
    }

    public void goToCreateProject()
    {
        clickAdminMenuItem("Site", "Create Project");
    }


    protected void createDefaultStudy()
    {
        clickButton("Create Study");
        clickButton("Create Study");
    }


    private void waitForStartup()
    {
        boolean hitFirstPage = false;
        log("Verifying that server has started...");
        long ms = System.currentTimeMillis();
        while (!hitFirstPage && ((System.currentTimeMillis() - ms)/1000) < MAX_SERVER_STARTUP_WAIT_SECONDS)
        {
            try
            {
                beginAt("/login/logout.view");

                if (getResponseCode() != 404)
                {
                    hitFirstPage = true;
                }
                else
                {
                    long elapsedMs = System.currentTimeMillis() - ms;
                    log("Server is not ready.  Waiting " + (MAX_SERVER_STARTUP_WAIT_SECONDS -
                            (elapsedMs / 1000)) + " more seconds...");
                }
            }
            catch (SeleniumException e)
            {
                // ignore timeouts that occur during startup; a poorly timed request
                // as the webapp is loading may hang forever, causing a timeout.
                log("Ignoring selenium exception: " + e.getMessage());
            }
            finally
            {
                if (!hitFirstPage)
                {
                    sleep(1000);
                }
            }

        }
        if (!hitFirstPage)
        {
            fail("Webapp failed to start up after " + MAX_SERVER_STARTUP_WAIT_SECONDS + " seconds.");
        }
        log("Server is running.");
    }

    private void checkForUpgrade()
    {
        boolean bootstrapped = false;

        // check to see if we're the first user:
        if (isTextPresent("Welcome! We see that this is your first time logging in."))
        {
            bootstrapped = true;
            assertTitleEquals("Account Setup");
            log("Need to bootstrap");
            verifyInitialUserRedirects();

            log("Testing bad email addresses");
            verifyInitialUserError("bogus@bogus@bogus", null, null, "Invalid email address: bogus@bogus@bogus");
            verifyInitialUserError(null, null, null, "Invalid email address: ");
            assertTextNotPresent("null");

            log("Testing bad passwords");
            String email = PasswordUtil.getUsername();
            verifyInitialUserError(email, null, null, "You must enter a password.");
            verifyInitialUserError(email, "LongEnough", null, "You must enter a password.");
            verifyInitialUserError(email, null, "LongEnough", "You must enter a password.");
            verifyInitialUserError(email, "short", "short", "Your password must be six non-whitespace characters or more.");
            verifyInitialUserError(email, email, email, "Your password must not match your email address.");
            verifyInitialUserError(email, "LongEnough", "ButDontMatch", "Your password entries didn't match.");

            log("Register the first user");
            pushLocation();
            assertTextPresent("Retype Password");
            verifyInitialUserError(email, PasswordUtil.getPassword(), PasswordUtil.getPassword(), null);

            log("Attempting to register another initial user");
            popLocation();
            // Make sure we got redirected to the module status page, since we already have a user
            assertTextNotPresent("Retype Password");
            assertTextPresent("Please wait, this page will automatically update with progress information");
            goToHome();
        }

        if (bootstrapped || isTitleEqual("Sign In"))
        {
            // if the logout page takes us to the sign-in page, then we may have a schema update to do:
            if (isTitleEqual("Sign In"))
                simpleSignIn();

            String upgradeText = "Please wait, this page will automatically update with progress information.";
            boolean performingUpgrade = isTextPresent(upgradeText);

            if (performingUpgrade)
            {
                verifyNoRedirect(upgradeText);

                // Check that sign out and sign in work properly during upgrade/install (once initial user is configured)
                signOut();
                simpleSignIn();

                int waitMs = 10 * 60 * 1000; // we'll wait at most ten minutes

                while (waitMs > 0 && (!(isNavButtonPresent("Next") || isLinkPresentWithText("Home"))))
                {
                    try
                    {
                        // Pound the server aggressively with requests for the home page to test synchronization
                        // in the sql script runner.
                        for (int i = 0; i < 5; i++)
                        {
                            goToHome();
                            Thread.sleep(200);
                            waitMs -= 200;
                        }
                        Thread.sleep(2000);
                        waitMs -= 2000;
                        if (isTextPresent("error occurred") || isTextPresent("failure occurred"))
                            fail("A startup failure occurred.");
                    }
                    catch (InterruptedException e)
                    {
                        log(e.getMessage());
                    }
                    catch (SeleniumException e)
                    {
                        // Do nothing -- this page will sometimes auto-navigate out from under selenium
                    }
                }

                if (waitMs <= 0)
                    fail("Script runner took more than 10 minutes to complete.");

                if (isNavButtonPresent("Next"))
                {
                    clickButton("Next");

                    // check for any additional upgrade pages inserted after module upgrade
                    if (isNavButtonPresent("Next"))
                        clickButton("Next");
                }

                if (isLinkPresentContainingText("Go directly to the server's Home page"))
                {
                    clickAndWait(Locator.linkContainingText("Go directly to the server's Home page"));
                }
            }

            // Will fail if left navbar is not enabled in Home project. TODO: allow this, see #xxxx
            clickAndWait(Locator.linkWithText("Home"));
        }
    }


    private void verifyInitialUserError(@Nullable String email, @Nullable String password1, @Nullable String password2, @Nullable String expectedText)
    {
        if (null != email)
            setFormElement("email", email);

        if (null != password1)
            setFormElement("password", password1);

        if (null != password2)
            setFormElement("password2", password2);

        clickAndWait(Locator.linkWithText("Next"));

        if (null != expectedText)
            assertTextPresent(expectedText);
    }


    private void verifyInitialUserRedirects()
    {
        String initialText = "Welcome! We see that this is your first time logging in.";

        // These requests should redirect to the initial user page
        beginAt("/login/resetPassword.view");
        assertTextPresent(initialText);
        beginAt("/admin/maintenance.view");
        assertTextPresent(initialText);
    }


    private void verifyNoRedirect(String upgradeText)
    {
        // These requests should NOT redirect to the upgrade page
        // Use a new window because the primary upgrade window seems to interfere with this test, #15853
        selenium.openWindow("", "noRedirect");
        selenium.selectWindow("noRedirect");
        beginAt("/login/resetPassword.view");
        assertTextNotPresent(upgradeText);
        beginAt("/admin/maintenance.view");
        assertTextNotPresent(upgradeText);
        selenium.close();
        selenium.selectWindow(null);
    }

    @LogMethod
    public void disableMaintenance()
    {
        if ( isGuestModeTest() )
            return;
        beginAt("/admin/customizeSite.view");
        click(Locator.radioButtonByNameAndValue("systemMaintenanceInterval", "never"));
        clickButton("Save");
    }

    private long smStart = 0;

    public void startSystemMaintenance()
    {
        startSystemMaintenance("Run all tasks");
    }

    public void startSystemMaintenance(String task)
    {
        goToAdminConsole();
        clickAndWait(Locator.linkWithText("system maintenance"));
        selenium.openWindow("", "systemMaintenance");
        click(Locator.linkWithText(task));
        smStart = System.currentTimeMillis();
    }


    public void pauseSearchCrawler()
    {
        goToAdminConsole();
        clickAndWait(Locator.linkWithText("full-text search"));
        if (isTextPresent("pause crawler"))
            clickButton("pause crawler");
    }



    public void unpauseSearchCrawler()
    {
        goToAdminConsole();
        clickAndWait(Locator.linkWithText("full-text search"));
        if (isTextPresent("unpause crawler"))
            clickButton("unpause crawler");
    }


    public void waitForSystemMaintenanceCompletion()
    {
        assertTrue("Must call startSystemMaintenance() before waiting for completion", smStart > 0);
        long elapsed = System.currentTimeMillis() - smStart;

        // Ensure that at least 5 seconds has passed since system maintenance was started
        if (elapsed < 5000)
        {
            log("Sleeping for " + (5000 - elapsed) + " ms");
            sleep(5000 - elapsed);
        }

        selenium.selectWindow("systemMaintenance");
        log("Waiting for system maintenance to complete");

        // Page updates automatically via AJAX... keep checking (up to 10 minutes) for system maintenance complete text
        waitFor(new Checker() {
            public boolean check()
            {
                return isTextPresent("System maintenance complete");
            }
        }, "System maintenance failed to complete in 10 minutes.", 10 * 60 * 1000);

        selenium.close();
        selenium.selectWindow(null);
    }

    //This is for the split test, I don't want to wait until that's complete to be able to check in other changes in this file
//    public void waitForSystemMaintenanceCompletion()
//    {
//        beginAt("/admin/showPrimaryLog.view?");
//        String log = selenium.getBodyText();
//
//        int startIndex =log.lastIndexOf("=== Starting SystemMaintenanceStartTest (1 of 1) ===");
//        if(!( startIndex > log.lastIndexOf("=== Completed SystemMaintenanceStartTest  ===")))
//        {
//            fail("Last SystemMaintenanceStartTest has not finished");
//        }
//
//
//       for(String msg : new String[] {"System maintenance complete", "Search Service Maintenance complete",
//               "Report Service Maintenance complete", "MS1 Data File Purge Task complete",
//               "Database maintenance complete", "Purge unused participants complete", "External schema reload complete"})
//       {
//            if(!( startIndex < log.lastIndexOf(msg)))
//            {
//                fail("\"" + msg + "\" did not appear" );
//            }
//       }
//        goBack();
//    }
//
//    protected void fail(String msg)
//    {
//        goBack(); //don't want to fail on the log file, it will create huge artifacts
//        fail(msg);
//    }

    public void populateLastPageInfo()
    {
        _lastPageTitle = getLastPageTitle();
        _lastPageURL = getLastPageURL();
        _lastPageText = getLastPageText();
    }

    public String getLastPageTitle()
    {
        if (_lastPageTitle == null)
        {
            if (null != selenium.getTitle())
                return selenium.getTitle();
            else
                return "[no title: content type is not html]";
        }
        return _lastPageTitle;
    }

    public String getLastPageText()
    {
        return _lastPageText != null ? _lastPageText : selenium.getHtmlSource();
    }

    public boolean isPageEmpty()
    {
        //IE and Firefox have different notions of empty.
        //IE returns html for all pages even empty text...
        String text = selenium.getHtmlSource();
        if (null == text || text.trim().length() == 0)
            return true;

        text = selenium.getBodyText();
        return null == text || text.trim().length() == 0;
    }

    public URL getLastPageURL()
    {
        try
        {
            return _lastPageURL != null ? _lastPageURL : new URL(selenium.getLocation());
        }
        catch (MalformedURLException x)
        {
            return null;
        }
    }

    public void resetErrors()
    {
        if ( isGuestModeTest() )
            return;
		if (getTargetServer().equals(DEFAULT_TARGET_SERVER))
        	beginAt("/admin/resetErrorMark.view");
    }

    /**
     * Override this method to skip running this test for a given configuration.
     * @return true to run the test, false to skip. Empty info should return false for overrides.
     */
    protected boolean isConfigurationSupported()
    {
        return true;
    }

    @Test//(timeout=1800000) // 30 minute default test timeout
    public void testSteps() throws Exception
    {
        try{ resumeJsErrorChecker(); }// Make sure js error checker didn't get stuck paused by a failure in the crawler.
        catch (Throwable t){/*ignore*/}
        try
        {
            log("\n\n=============== Starting " + getClass().getSimpleName() + Runner.getProgress() + " =================");

            _startTime = System.currentTimeMillis();

            logToServer("=== Starting " + getClass().getSimpleName() + Runner.getProgress() + " ===");
            signIn();
			resetErrors();

            if( isSystemMaintenanceDisabled() )
            {
                // Disable scheduled system maintenance to prevent timeouts during nightly tests.
                disableMaintenance();
            }


            if ( !isGuestModeTest() )
            {
                if (!isConfigurationSupported()) // skip this check if it returns true with no database info.
                {
                    log("** Skipping " + getClass().getSimpleName() + " test for unsupported configurarion");
                    _testFailed = false;
                    return;
                }
            }

            // Only do this as part of test startup if we haven't already checked. Since we do this as the last
            // step in the test, there's no reason to bother doing it again at the beginning of the next test
            if (!_checkedLeaksAndErrors && !"DRT".equals(System.getProperty("suite")))
            {
                checkLeaksAndErrors();
            }

            if (isPipelineToolsTest()) // Get DB back in a good state after failed pipeline tools test.
                fixPipelineToolsDirectory();

            log("Pre-cleaning " + getClass().getSimpleName());
            doCleanup(false);

            doTestSteps();

            //make sure you're signed in as admin, because this won't work otherwise
            ensureSignedInAsAdmin();

            checkQueries();

            checkViews();

            checkActionCoverage();

            checkLinks();

            if (!isTestCleanupSkipped())
            {
                goToHome();
                doCleanup(true);
            }
            else
            {
                log("Skipping test cleanup as requested.");
            }

            if (!"DRT".equals(System.getProperty("suite")) || Runner.isFinalTest())
            {
                checkLeaksAndErrors();
            }

            _testFailed = false;
        }
        catch (TestTimeoutException e)
        {
            _testTimeout = true;
            throw e;
        }
        finally
        {
            try
            {
                 populateLastPageInfo();
            }
            catch (Throwable t)
            {
                System.out.println("Unable to determine information about the last page: server not started or -Dlabkey.port incorrect?");
            }

            if (_testFailed)
            {
                try
                {
                    dumpPageSnapshot();
                    if (isTestRunningOnTeamCity())
                    {
                        dumpPipelineFiles(getLabKeyRoot() + "/sampledata");
                        dumpPipelineLogFiles(getLabKeyRoot() + "/build/deploy/files");
                    }
                    if (_testTimeout)
                        dumpThreads();
                }
                catch (Exception t)
                {
                    System.out.println("Unable to dump failure information");
                    t.printStackTrace();
                }
                finally
                {
                    try{
                        resetDbLoginConfig(); // Make sure to return DB config to its pre-test state.
                    }
                    catch(Throwable t){
                        log("Failed to reset DB long config after test failure");
                        dumpPageSnapshot("resetDbLogin");
                    }

                    try{
                        if (isPipelineToolsTest()) // Get DB back in a good state after failed pipeline tools test.
                            fixPipelineToolsDirectory();
                    }
                    catch(Throwable t){
                        // Assure that this failure is noticed
                        // Regression check: https://www.labkey.org/issues/home/Developer/issues/details.view?issueId=10732
                        log("**************************ERROR*******************************");
                        log("** SERIOUS ERROR: Failed to reset pipeline tools directory. **");
                        log("** Server may be in a bad state.                            **");
                        log("** Set tools directory manually or bootstrap to fix.        **");
                        log("**************************ERROR*******************************");
                        dumpPageSnapshot("fixPipelineToolsDir");
                    }
                }
            }

            logToServer("=== Completed " + getClass().getSimpleName() + Runner.getProgress() + " ===");

            log("=============== Completed " + getClass().getSimpleName() + Runner.getProgress() + " =================");
        }
    }

    @LogMethod
    public void ensureSignedInAsAdmin()
    {
        goToHome();
        if(isTextPresent("Admin"))
            return;

        if(isElementPresent(Locator.id("userMenuPopupLink")))
            signOut();

        signIn();

    }

    protected abstract void doTestSteps() throws Exception;

    protected abstract void doCleanup(boolean afterTest) throws TestTimeoutException;

    public void cleanup() throws Exception
    {
        boolean tearDown = false;
        try
        {
            log("========= Cleaning up " + getClass().getSimpleName() + " =========");
            if (selenium == null)
            {
                setUp();
                tearDown = true;
            }

            // explicitly go back to the site, just in case we're on a 404 or crash page:
            beginAt("");
            signIn();
            doCleanup(false);   // User requested cleanup... could be before or after tests have run (or some intermediate state). False generally means ignore errors.

            beginAt("");

            // The following checks verify that the test deleted all projects and folders that it created.
            for (FolderIdentifier folder : _createdFolders)
                assertLinkNotPresentWithText(folder.getFolderName());

            for (String projectName : _containerHelper.getCreatedProjects())
                assertLinkNotPresentWithText(projectName);
            log("========= " + getClass().getSimpleName() + " cleanup complete =========");
        }
        finally
        {
            if (tearDown)
                tearDown();
        }
    }

    public boolean isGuestModeTest()
    {
        return false;
    }

    protected boolean isQuickTest = false;

    protected boolean isQuickTest()
    {
        return isQuickTest;//"DRT".equals(System.getProperty("suite"));
    }

    protected void setIsQuickTest(boolean isQuickTest)
    {
        this.isQuickTest = isQuickTest;
    }

    @LogMethod
    public void checkLeaksAndErrors()
    {
        if ( isGuestModeTest() )
            return;
		checkErrors();
		checkLeaks();
        _checkedLeaksAndErrors = true;
    }

    public void checkLeaks()
    {
		if (!getTargetServer().equals(DEFAULT_TARGET_SERVER))
			return;
        if (isLeakCheckSkipped())
            return;
        if (isGuestModeTest())
            return;

        log("Starting memory leak check...");
        int leakCount = MAX_LEAK_LIMIT + 1;

        for (int attempt = 0; attempt < GC_ATTEMPT_LIMIT && leakCount > MAX_LEAK_LIMIT; attempt++)
        {
            if (attempt > 0)
            {
                log("Found " + leakCount + " in-use objects; rerunning GC. ("
                        + (GC_ATTEMPT_LIMIT - attempt) + " attempt(s) remaining.)");

                // If another thread (e.g., SearchService) is doing work then give it 10 seconds before trying again
                if (isTextPresent("Warning: active thread"))
                {
                    log("Pausing 10 seconds to wait for active thread");
                    sleep(10000);
                }
            }
            beginAt("/admin/memTracker.view?gc=1&clearCaches=1", 120000);
            if (!isTextPresent("In-Use Objects"))
                fail("Asserts must be enabled to track memory leaks; please add -ea to your server VM params and restart.");
            leakCount = getImageWithAltTextCount("expand/collapse");
        }

        if (leakCount > MAX_LEAK_LIMIT)
        {
            String leaks = selenium.getText(Locator.xpath("//table[@name = 'leaks']").toString());
            CRC32 crc = new CRC32();
            crc.update(leaks.getBytes());

            if (leakCRC != crc.getValue())
            {
                leakCRC = crc.getValue();
                dumpHeap();
                fail(leakCount + " in-use objects exceeds allowed limit of " + MAX_LEAK_LIMIT + ".");
            }

            log("Found " + leakCount + " in-use objects.  They appear to be from a previous test.");
        }
        else
            log("Found " + leakCount + " in-use objects.  This is within the expected number of " + MAX_LEAK_LIMIT + ".");
    }

    public void checkErrors()
    {
		if (!getTargetServer().equals(DEFAULT_TARGET_SERVER))
			return;
        if ( isGuestModeTest() )
            return;
        beginAt("/admin/showErrorsSinceMark.view");

       assertTrue("There were errors during the test run", isPageEmpty());
       log("No new errors found.");
       goToHome();         // Don't leave on an empty page
   }

    public void checkExpectedErrors(int count)
    {
        // Need to remember our location or the next test could start with a blank page
        pushLocation();
        beginAt("/admin/showErrorsSinceMark.view");

        //IE and Firefox have different notions of empty.
        //IE returns html for all pages even empty text...
        String text = selenium.getHtmlSource();
        if (null == text)
            text = "";
        text = text.trim();
        if ("".equals(text))
        {
            text = selenium.getText("//body");
            if (null == text)
                text = "";
            text = text.trim();
        }

        assertTrue("Expected " + count + " errors during this run", StringUtils.countMatches(text, "ERROR") == count);
        log("Found " + count + " expected errors.");

        // Clear the errors to prevent the test from failing.
        resetErrors();

        popLocation();
    }

    @LogMethod
    protected void checkQueries()
    {
        if (isQueryCheckSkipped())
            return;
        if(getProjectName() != null)
        {
            clickProject(getProjectName());
            if(!"Query Schema Browser".equals(selenium.getTitle()))
                goToSchemaBrowser();
            validateQueries(true);
//            validateLabAuditTrail();
        }
    }

    @LogMethod
    protected void checkViews()
    {
        if (isViewCheckSkipped())
            return;

        List<String> checked = new ArrayList<>();

        for (String projectName : _containerHelper.getCreatedProjects())
        {
            clickProject(projectName);

            doViewCheck(projectName);
            checked.add(projectName);
        }
        
        for (FolderIdentifier folderId : _createdFolders)
        {
            String project = folderId.getProjectName();
            String folder = folderId.getFolderName();
            if(!checked.contains(project))
            {
                clickProject(project);

                doViewCheck(project);
                checked.add(project);
            }
            if(!checked.contains(folder))
            {
                String currentProject = getText(Locator.id("folderBar"));
                if (!currentProject.equals(project))
                    clickProject(project);

                clickFolder(folder);

                doViewCheck(folder);
                checked.add(folder);
            }
        }
    }

    @LogMethod
    private void doViewCheck(@LoggedParam String folder)
    {
        try{
            goToManageViews();
        }
        catch (SeleniumException e)
        {
            log("No manage views option");
            return;
        }

        _extHelper.waitForLoadingMaskToDisappear(WAIT_FOR_JAVASCRIPT);
        Locator.XPathLocator view = Locator.xpath("//div[contains(@class, 'x-grid-group-body')]/div[contains(@class, 'x-grid3-row')]");
        int viewCount = getXpathCount(view);
        for (int i = 0; i < viewCount; i++)
        {
            Locator.XPathLocator thisView = view.index(i);
            waitForElement(thisView);
            String viewName = getText(thisView.append("//td[contains(@class, 'x-grid3-cell-first')]"));

            pushLocation();
            click(thisView);

            String schemaName = getText(Locator.xpath("//div[contains(@class, 'x-grid3-row-expanded')]//div[contains(@class, 'x-grid3-row-body')]//td[normalize-space()='schema name']/following-sibling::td"));
            String queryName = getText(Locator.xpath("//div[contains(@class, 'x-grid3-row-expanded')]//div[contains(@class, 'x-grid3-row-body')]//td[normalize-space()='query name']/following-sibling::td"));
            String viewString = viewName + " of " + schemaName + "." + queryName;
            log("Checking view: " + viewString);

            waitAndClick(Locator.linkWithText("view"));
            waitForText(viewName);
            popLocation();
            _extHelper.waitForLoadingMaskToDisappear(WAIT_FOR_JAVASCRIPT);
        }
    }

    public Connection getDefaultConnection()
    {
        return new Connection(getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());
    }

    protected SelectRowsResponse executeSelectRowCommand(String schemaName, String queryName)
    {
        return executeSelectRowCommand(schemaName, queryName, ContainerFilter.CurrentAndSubfolders, "/" + getProjectName(), null);
    }

    protected SelectRowsResponse executeSelectRowCommand(String schemaName, String queryName, ContainerFilter containerFilter, String path, @Nullable List<Filter> filters)
    {
        Connection cn = new Connection(getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());
        SelectRowsCommand selectCmd = new SelectRowsCommand(schemaName, queryName);
        selectCmd.setMaxRows(-1);
        selectCmd.setContainerFilter(containerFilter);
        selectCmd.setColumns(Arrays.asList("*"));
        if (filters != null)
            selectCmd.setFilters(filters);

        SelectRowsResponse selectResp = null;

//        selectCmd.setQueryName(subQuery);
        try
        {
            selectResp = selectCmd.execute(cn, path);
        }
        catch (Exception e)
        {
           fail(e.getMessage());
        }

        return selectResp;
    }

    @LogMethod
    private void checkActionCoverage()
    {
        if ( isGuestModeTest() )
            return;

        pushLocation();
        int rowCount, coveredActions, totalActions;
        Double actionCoveragePercent;
        String actionCoveragePercentString;
        beginAt("/admin/actions.view");

        rowCount = getTableRowCount(ACTION_SUMMARY_TABLE_NAME);
        if (getTableCellText(ACTION_SUMMARY_TABLE_NAME, rowCount - 1, 0).equals("Total"))
        {
            totalActions = Integer.parseInt(getTableCellText(ACTION_SUMMARY_TABLE_NAME, rowCount - 1, 1));
            coveredActions = Integer.parseInt(getTableCellText(ACTION_SUMMARY_TABLE_NAME, rowCount - 1, 2));
            actionCoveragePercentString = getTableCellText(ACTION_SUMMARY_TABLE_NAME, rowCount - 1, 3);
            actionCoveragePercent =  Double.parseDouble(actionCoveragePercentString.substring(0, actionCoveragePercentString.length() - 1 ));
            writeActionStatistics(totalActions, coveredActions, actionCoveragePercent);
        }

        // Download full action coverage table and add to TeamCity artifacts.
        beginAt("/admin/exportActions.view?asWebPage=true");
        publishArtifact(saveTsv(TestProperties.getDumpDir(), "ActionCoverage"));
        popLocation();
    }

    @LogMethod
    protected void checkLinks()
    {
        if (isLinkCheckEnabled())
        {
            pauseJsErrorChecker();
            Crawler crawler = new Crawler(this, Runner.getTestSet().getCrawlerTimeout());
            crawler.crawlAllLinks(isInjectCheckEnabled());
            resumeJsErrorChecker();
        }
    }

    private void writeActionStatistics(int totalActions, int coveredActions, Double actionCoveragePercent)
    {
        // TODO: Create static class for managing teamcity-info.xml file.
        FileWriter writer = null;
        try
        {
            File xmlFile = new File(getLabKeyRoot(), "teamcity-info.xml");
            xmlFile.createNewFile();
            writer = new FileWriter(xmlFile);

            writer.write("<build>\n");
            writer.write("\t<statisticValue key=\"totalActions\" value=\"" + totalActions + "\"/>\n");
            writer.write("\t<statisticValue key=\"coveredActions\" value=\"" + coveredActions + "\"/>\n");
            writer.write("\t<statisticValue key=\"actionCoveragePercent\" value=\"" + actionCoveragePercent + "\"/>\n");
            writer.write("</build>");
        }
        catch (IOException ignored) {}
        finally
        {
            if (writer != null)
                try
                {
                    writer.close();
                }
                catch (IOException ignored)
                {
                }
        }
    }

    protected File ensureDumpDir()
    {
        File dumpDir = new File(TestProperties.getDumpDir(), getClass().getSimpleName());
        if ( !dumpDir.exists() )
            dumpDir.mkdirs();

        return dumpDir;
    }

    public void dumpPageSnapshot()
    {
        dumpPageSnapshot(null);
    }

    public void dumpPageSnapshot(@Nullable String subdir)
    {
        try
        {
            File dumpDir = ensureDumpDir();
            if (subdir != null && subdir.length() > 0)
            {
                dumpDir = new File(dumpDir, subdir);
                if ( !dumpDir.exists() )
                    dumpDir.mkdirs();
            }
            FastDateFormat dateFormat = FastDateFormat.getInstance("yyyyMMddHHmm");
            String baseName = dateFormat.format(new Date()) + getClass().getSimpleName();

            publishArtifact(dumpFullScreen(dumpDir, baseName));
            publishArtifact(dumpScreen(dumpDir, baseName));
            publishArtifact(dumpHtml(dumpDir, baseName));
        }
        catch (Exception e)
        {
            log("Error executing dumpPageSnapshot()");
        }
    }

    public void dumpHeap()
    {
		if (!getTargetServer().equals(DEFAULT_TARGET_SERVER))
			return;
        if ( isGuestModeTest() )
            return;
        pushLocation();
        try
        {
            // Use dumpHeapAction rather that touching file so that we can get file name and publish artifact.
            beginAt("/admin/dumpHeap.view");
            File destDir = ensureDumpDir();
            String dumpMsg = selenium.getText("xpath=//td[@id='bodypanel']/div");
            String filename = dumpMsg.substring(dumpMsg.indexOf("HeapDump_"));
            File heapDump = new File(getLabKeyRoot() + "/build/deploy", filename);
            File destFile = new File(destDir, filename);

            if ( heapDump.renameTo(destFile) )
                publishArtifact(destFile);
            else
                log("Unable to move HeapDump file to test logs directory.");
        }
        catch (Exception e)
        {
            log("Error dumping heap: " + e.getMessage());
        }
        popLocation(); // go back to get screenshot if needed.
    }

    public void dumpThreads()
    {
		if (!getTargetServer().equals(DEFAULT_TARGET_SERVER))
			return;

        try
        {
            File threadDumpRequest = new File(getLabKeyRoot() + "/build/deploy", "threadDumpRequest");
            threadDumpRequest.setLastModified(System.currentTimeMillis()); // Touch file to trigger automatic thread dump.
        }
        catch (Exception e)
        {
            log("Error dumping threads: " + e.getMessage());
        }

        log("Threads dumped to standard labkey log file");
    }

    // Publish artifacts while the build is still in progress:
    // http://www.jetbrains.net/confluence/display/TCD4/Build+Script+Interaction+with+TeamCity#BuildScriptInteractionwithTeamCity-PublishingArtifactswhiletheBuildisStillinProgress
    public void publishArtifact(File file)
    {
        if (file != null && System.getProperty("teamcity.buildType.id") != null)
        {
            // relativize path to labkey project root
            String labkeyRoot = WebTestHelper.getLabKeyRoot();
            labkeyRoot = new File(labkeyRoot).getAbsolutePath();
            String strFile = file.getAbsolutePath();
            if (labkeyRoot != null && strFile.toLowerCase().startsWith(labkeyRoot.toLowerCase()))
            {
                String path = strFile.substring(labkeyRoot.length());
                if (path.startsWith(File.separator))
                    path = path.substring(1);
                System.out.println("##teamcity[publishArtifacts '" + path + "']");
            }
        }
    }

    public File dumpScreen(File dir, String baseName)
    {
        File screenFile = new File(dir, baseName + ".png");
        try
        {
            selenium.captureEntirePageScreenshot(screenFile.getAbsolutePath(), "");
            return screenFile;
        }
        catch (SeleniumException se)
        {
            // too bad.
            log("Failed to take screenshot using selenium.captureEntirePageScreenshot: " + se.getMessage());

            try
            {
                selenium.windowFocus();
                selenium.windowMaximize();
                selenium.captureScreenshot(screenFile.getAbsolutePath());
                return screenFile;
            }
            catch (SeleniumException se2)
            {
                // so sad.
                log("Failed to take screenshot using selenium.captureScreenshot: " + se2.getMessage());
            }
        }

        return null;
    }

    public void windowMaximize()
    {
        selenium.windowMaximize();
    }

    public File dumpFullScreen(File dir, String baseName)
    {
        File screenFile = new File(dir, baseName + "Fullscreen.png");

        try
        {
            // capture entire screen
            BufferedImage fullscreen = new Robot().createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
            ImageIO.write(fullscreen, "png", screenFile);

            return screenFile;
        }
        catch (Exception e)
        {
            log("Failed to take full screenshot: " + e.getMessage());
        }

        return null;
    }

    public File dumpHtml(File dir, String baseName)
    {
        if (getLastPageText() == null)
            return null;

        File htmlFile = new File(dir, baseName + ".html");
        try (FileWriter writer = new FileWriter(htmlFile))
        {
            writer.write(getLastPageText());
            return htmlFile;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public File saveTsv(File dir, String baseName)
    {
        return saveFile(dir, baseName + ".tsv");
    }

    public File saveFile(File dir, String fileName)
    {
        return saveFile(dir, fileName, selenium.getBodyText());
    }

    public File saveFile(File dir, String fileName, String contents)
    {
        FileWriter writer = null;
        try
        {
            File tsvFile = new File(dir, fileName);
            writer = new FileWriter(tsvFile);
            writer.write(contents);
            return tsvFile;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
        finally
        {
            if (writer != null)
                try
                {
                    writer.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
        }
    }

    public String getBaseURL()
    {
        return WebTestHelper.getBaseURL();
    }

    public String getProjectUrl()
    {
        return "/project/" + EscapeUtil.encode(getProjectName()) + "/begin.view?";
    }

    public static String stripContextPath(String url)
    {
        String root = WebTestHelper.getContextPath() + "/";
        int rootLoc = url.indexOf(root);
        int endOfAction = url.indexOf("?");
        if ((rootLoc != -1) && (endOfAction == -1 || rootLoc < endOfAction))
            url = url.substring(rootLoc + root.length());
        else if (url.indexOf("/") == 0)
            url = url.substring(1);
        return url;
    }

    public long beginAt(String relativeURL)
    {
        return beginAt(relativeURL, defaultWaitForPage);
    }

    public long beginAt(String relativeURL, int millis)
    {
        relativeURL = stripContextPath(relativeURL);
        String logMessage = "";

        try
        {
            if (relativeURL.length() == 0)
                logMessage = "Navigating to root";
            else
            {
                logMessage = "Navigating to " + relativeURL;
                if (relativeURL.charAt(0) != '/')
                {
                    relativeURL = "/" + relativeURL;
                }
            }
            pauseJsErrorChecker();

            long startTime = System.currentTimeMillis();
            selenium.open(getBaseURL() + relativeURL, millis);
            long elapsedTime = System.currentTimeMillis() - startTime;
            logMessage += " [" + elapsedTime + " ms]";

            resumeJsErrorChecker();

            return elapsedTime;
        }
        finally
        {
            log(logMessage);
        }
    }

    public String getContainerId(String url)
    {
        pushLocation();
        beginAt(url);
        String containerId = selenium.getEval("selenium.getContainerId()");
        popLocation();
        return containerId;
    }

    public String getConfirmationAndWait()
    {
        String confirmation = selenium.getConfirmation();
        waitForPageToLoad();
        return confirmation;
    }

    public void assertConfirmation(String msg)
    {
        assertEquals(msg, selenium.getConfirmation());
    }

    public void assertAlert(String msg)
    {
        assertEquals(msg, selenium.getAlert());
    }

    public void dismissAlerts()
    {
        boolean present = false;
        while (selenium != null && selenium.isAlertPresent())
            log("Found unexpected alert: " + selenium.getAlert());
    }

    public void logJavascriptAlerts()
    {
        while (selenium.isAlertPresent())
        {
            log("JavaScript Alert Ignored: " + selenium.getAlert());
        }
    }

	public boolean isAlertPresent()
	{
		return selenium.isAlertPresent();
	}

	public String getAlert()
	{
		return selenium.getAlert();
	}

    public void assertExtMsgBox(String title, String text)
    {
        String actual = _extHelper.getExtMsgBoxText(title);
        assertTrue("Expected Ext.Msg box text '" + text + "', actual '" + actual + "'", actual.contains(text));
    }

    public enum SeleniumEvent
    {blur,change,mousedown,mouseup,click,reset,select,submit,abort,error,load,mouseout,mouseover,unload,keyup,focus}

    public void fireEvent(Locator loc, SeleniumEvent event)
    {
        selenium.fireEvent(loc.toString(), event.toString());
    }

    public void startCreateGlobalPermissionsGroup(String groupName, boolean failIfAlreadyExists)
    {

        goToHome();
        goToSiteGroups();
        if(isElementPresent(Locator.tagWithText("div", groupName)))
        {
            if(failIfAlreadyExists)
                fail("Group already exists");
            else
                return;
        }

        Locator l = Locator.xpath("//input[contains(@name, 'sitegroupsname')]");
        waitForElement(l, defaultWaitForPage);

        setFormElement(l,groupName);
        clickButton("Create New Group", 0);
        _extHelper.waitForExtDialog(groupName + " Information");
    }

    public void createGlobalPermissionsGroup(String groupName, String... users)
    {
        createGlobalPermissionsGroup(groupName, true, users);
    }

    public void createGlobalPermissionsGroup(String groupName, boolean failIfAlreadyExists, String... users )
    {
        startCreateGlobalPermissionsGroup(groupName, failIfAlreadyExists);
        StringBuilder namesList = new StringBuilder();
        for(String member : users)
        {
            namesList.append(member).append("\n");
        }

        log("Adding\n" + namesList.toString() + " to group " + groupName + "...");
        waitAndClick(Locator.tagContainingText("a","manage group"));
        waitForPageToLoad();
        addUserToGroupFromGroupScreen(namesList.toString());
    }

    public void createPermissionsGroup(String groupName)
    {
        log("Creating permissions group " + groupName);
        if (!isElementPresent(Locator.permissionRendered()))
            enterPermissionsUI();
        waitForElement(Locator.permissionRendered(), WAIT_FOR_JAVASCRIPT);
        _ext4Helper.clickTabContainingText("Project Groups");
        setFormElement(Locator.xpath("//input[contains(@name, 'projectgroupsname')]"), groupName);
        clickButton("Create New Group", 0);
        sleep(500);
        waitForText("Group " + groupName);
        waitAndClick(Locator.xpath("//div[contains(@class, 'x4-tool')]//img[contains(@class, 'x4-tool-close')]"));
        waitForElement(Locator.xpath("//div[contains(@class, 'x4-grid-cell-inner') and text()='" + groupName + "']"), WAIT_FOR_JAVASCRIPT);
    }

    public void createPermissionsGroup(String groupName, String... memberNames)
    {
        createPermissionsGroup(groupName);
        clickManageGroup(groupName);

        StringBuilder namesList = new StringBuilder();
        for(String member : memberNames)
        {
            namesList.append(member).append("\n");
        }

        log("Adding\n" + namesList.toString() + " to group " + groupName + "...");
        addUserToGroupFromGroupScreen(namesList.toString());

        enterPermissionsUI();
    }

    public void openGroupPermissionsDisplay(String groupName)
    {
        _ext4Helper.clickTabContainingText("Project Groups");
        // warning Adminstrators can apper multiple times
        List<Ext4CmpRef> refs = _ext4Helper.componentQuery("grid", Ext4CmpRef.class);
        Ext4CmpRef ref = refs.get(0);
        int idx = Integer.parseInt(ref.eval("this.getStore().find(\"name\", \"" + groupName + "\")"));
        assertFalse("Unable to locate group: \"" + groupName + "\"", idx < 0);
        ref.eval("this.getSelectionModel().select(" + idx + ")");
    }

    public void clickManageGroup(String groupName)
    {
        openGroupPermissionsDisplay(groupName);
        waitAndClick(Locator.tagContainingText("a","manage group"));
        waitForPageToLoad();
    }

    public void createSubFolderFromTemplate(String project, String child, String template, String[] objectsToSkip)
    {
        createSubfolder(project, project, child, "Create From Template Folder", template, objectsToSkip, false);

    }

    public void createSubfolder(String project, String child, String[] tabsToAdd)
    {
        // create a child of the top-level project folder:
        createSubfolder(project, project, child, "None", tabsToAdd);
    }

    public void createSubfolder(String project, String parent, String child, String folderType, @Nullable String[] tabsToAdd)
    {
        createSubfolder(project, parent, child, folderType, tabsToAdd, false);
    }

    private  void startCreateFolder(String project, String parent, String child)
    {
        clickProject(project);
        if (!parent.equals(project))
        {
            clickFolder(parent);
        }
        hoverFolderBar();
        if (isElementPresent(Locator.id("folderBar_menu").append(Locator.linkWithText(child))))
            fail("Folder: " + child + " already exists in project: " + project);
        log("Creating subfolder " + child + " under " + parent);
        clickAndWait(Locator.xpath("//a[@title='New Subfolder']"));
        waitForElement(Locator.name("name"), WAIT_FOR_JAVASCRIPT);
        setFormElement(Locator.name("name"), child);
    }

    public void createSubfolder(String project, String parent, String child, String folderType, @Nullable String[] tabsToAdd, boolean inheritPermissions)
    {
        createSubfolder(project, parent, child, folderType, null, tabsToAdd, inheritPermissions);
    }

    public void createSubfolder(String project, String parent, String child, String folderType, @Nullable String templateFolder, String[] tabsToAdd, boolean inheritPermissions)
    {
        createSubfolder(project, parent, child, folderType, templateFolder, null, tabsToAdd, inheritPermissions);
    }
    /**
     *
     * @param project project in which to create new folder
     * @param parent immediate parent of the new folder (project, if it's a top level subfolder)
     * @param child name of folder to create
     * @param folderType type of folder (null for custom)
     * @param templateFolder if folderType = "create from Template Folder", this is the template folder used.  Otherwise, ignored
     * @param tabsToAdd module tabs to add iff foldertype=null,  or the copy related checkboxes iff foldertype=create from template
     * @param inheritPermissions should folder inherit permissions from parent?
     */
    @LogMethod
    public void createSubfolder(String project, String parent, String child, @Nullable String folderType, String templateFolder, @Nullable String[] templatePartsToUncheck, @Nullable String[] tabsToAdd, boolean inheritPermissions)
    {
        startCreateFolder(project, parent, child);
        if (null != folderType && !folderType.equals("None"))
        {
            click(Locator.xpath("//td[./label[text()='"+folderType+"']]/input[@type='button' and contains(@class, 'radio')]"));
            if(folderType.equals("Create From Template Folder"))
            {
                log("create from template");
                click(Locator.xpath("//td[./label[text()='"+folderType+"']]/input[@type='button' and contains(@class, 'radio')]"));
                _ext4Helper.waitForMaskToDisappear();
                _ext4Helper.selectComboBoxItem(Locator.xpath("//div").withClass("labkey-wizard-header").withText("Choose Template Folder:").append("/following-sibling::table[contains(@id, 'combobox')]"), templateFolder);
                _ext4Helper.checkCheckbox("Include Subfolders");
                if (templatePartsToUncheck != null)
                {
                    for(String part : templatePartsToUncheck)
                    {
                        click(Locator.xpath("//td[label[text()='" +  part + "']]/input"));
                    }
                }
            }
        }
        else {
            click(Locator.xpath("//td[./label[text()='Custom']]/input[@type='button' and contains(@class, 'radio')]"));


            if (tabsToAdd != null)
            {
                for (String tabname : tabsToAdd)
                    waitAndClick(Locator.xpath("//td[./label[text()='"+tabname+"']]/input[@type='button' and contains(@class, 'checkbox')]"));
            }
        }

        waitAndClick(Locator.ext4Button("Next"));
        _createdFolders.add(new FolderIdentifier(project, child));
        waitForPageToLoad();

        //second page of the wizard
        if (inheritPermissions)
        {
            //nothing needed, this is the default
        }
        else {
            waitAndClick(Locator.xpath("//td[./label[text()='My User Only']]/input"));
        }

        waitAndClick(Locator.ext4Button("Finish")); //Leave permissions where they are
        waitForPageToLoad();

        //unless we need addtional tabs, we end here.
        if (null == tabsToAdd || tabsToAdd.length == 0)
            return;

        if (null != folderType && !folderType.equals("None"))
        {
            goToFolderManagement();
            clickAndWait(Locator.linkWithText("Folder Type"));

            for (String tabname : tabsToAdd)
                checkCheckbox(Locator.checkboxByTitle(tabname));

            submit();
            if ("None".equals(folderType))
            {
                for (String tabname : tabsToAdd)
                    assertElementPresent(Locator.folderTab(tabname));
            }

            // verify that there's a link to our new folder:
            assertLinkPresentWithText(child);
        }
    }

    protected void deleteDir(File dir)
    {
        log("Deleting from filesystem: " + dir.toString());
        if (!dir.exists())
            return;

        try
        {
            FileUtils.deleteDirectory(dir);
            log("Deletion successful.");
        }
        catch (IOException e)
        {
            log("WARNING: Exception deleting directory -- " + e.getMessage());
        }
    }

    @LogMethod
    public void deleteFolder(String project, String folderName)
    {
        log("Deleting folder " + folderName + " under project " + project);
        clickProject(project);
        clickFolder(folderName);
        ensureAdminMode();
        goToFolderManagement();
        waitForElement(Ext4HelperWD.Locators.folderManagementTreeNode(folderName));
        clickButton("Delete");
        // confirm delete subfolders if present
        if(isTextPresent("This folder has subfolders."))
            clickButton("Delete All Folders");
        // confirm delete:
        clickButton("Delete");
        // verify that we're not on an error page with a check for a project link:
        assertLinkPresentWithText(project);
        assertLinkNotPresentWithText(folderName);
    }

    @LogMethod
    public void renameFolder(String project, String folderName, String newFolderName, boolean createAlias)
    {
        log("Renaming folder " + folderName + " under project " + project + " -> " + newFolderName);
        clickProject(project);
        clickFolder(folderName);
        ensureAdminMode();
        goToFolderManagement();
        waitForElement(Ext4HelperWD.Locators.folderManagementTreeNode(folderName));
        clickButton("Rename");
        setFormElement(Locator.name("name"), newFolderName);
        if (createAlias)
            checkCheckbox(Locator.name("addAlias"));
        else
            uncheckCheckbox(Locator.name("addAlias"));
        // confirm rename:
        clickButton("Rename");
        _createdFolders.remove(new WebTestHelper.FolderIdentifier(project, folderName));
        _createdFolders.add(new WebTestHelper.FolderIdentifier(project, newFolderName));
        assertElementPresent(Locator.currentProject().withText(project));
        hoverFolderBar();
        assertElementPresent(Locator.linkWithText(newFolderName));
        assertElementNotPresent(Locator.linkWithText(folderName));
    }

    @LogMethod
    public void moveFolder(String projectName, String folderName, String newParent, boolean createAlias)
    {
        log("Moving folder [" + folderName + "] under project [" + projectName + "] to [" + newParent + "]");
        clickProject(projectName);
        clickFolder(folderName);
        ensureAdminMode();
        goToFolderManagement();
        waitForElement(Ext4HelperWD.Locators.folderManagementTreeNode(folderName));
        clickButton("Move");
        if (createAlias)
            checkCheckbox(Locator.name("addAlias"));
        else
            uncheckCheckbox(Locator.name("addAlias"));
        // Select Target
        waitForElement(Locator.permissionsTreeNode(newParent), 10000);
        selectFolderTreeItem(newParent);
        // move:
        clickButton("Confirm Move");

        // verify that we're not on an error page with a check for folder link:
        assertElementPresent(Locator.currentProject().withText(projectName));
        hoverFolderBar();
        assertElementPresent(Locator.xpath("//li").withClass("clbl").withPredicate(Locator.xpath("a").withText(newParent)).append("/ul/li/a").withText(folderName));
        String newProject = getText(Locator.currentProject());
        _createdFolders.remove(new WebTestHelper.FolderIdentifier(projectName, folderName));
        _createdFolders.add(new WebTestHelper.FolderIdentifier(newProject, folderName));
    }

    public void hoverProjectBar()
    {
        waitForElement(Locator.id("projectBar"));
        selenium.getEval("selenium.browserbot.getCurrentWindow().HoverNavigation._project.show();"); // mouseOver doesn't work
        waitForElement(Locator.css("#projectBar_menu .project-nav"));
    }

    public void clickProject(String project)
    {
        clickProject(project, true);
    }

    public void clickProject(String project, boolean assertDestination)
    {
        hoverProjectBar();
        waitAndClickAndWait(Locator.linkWithText(project));
        if (assertDestination)
            waitForElement(Locator.id("folderBar").withText("Home".equals(project) ? "home" : project)); // home casing is incosistent
    }

    public void hoverFolderBar()
    {
        waitForElement(Locator.id("folderBar"));
        selenium.getEval("selenium.browserbot.getCurrentWindow().HoverNavigation._folder.show();"); // mouseOver doesn't work in Firefox
        waitForElement(Locator.css("#folderBar_menu .folder-nav"));
    }

    public void clickFolder(String folder)
    {
        hoverFolderBar();
        waitAndClickAndWait(Locator.linkWithText(folder));
        waitForElement(Locator.id("folderBar"));
    }

    /**
     * Delete specified project during test
     * @param project Project display name
     */
    public void deleteProject(String project, boolean failIfFail) throws TestTimeoutException
    {
        _containerHelper.deleteProject(project, failIfFail, 270000); // Wait for 270 seconds for project deletion
    }

    @LogMethod
    public void enableEmailRecorder()
    {
        try {
            getHttpGetResponse(WebTestHelper.getBaseURL() + "/dumbster/setRecordEmail.view?record=true", PasswordUtil.getUsername(), PasswordUtil.getPassword());}
        catch (IOException e) {
            fail("Failed to enable email recorder");}
        catch (HttpException e) {
            fail("Failed to enable email recorder");}
    }

    public void addWebPart(String webPartName)
    {
        Locator.XPathLocator selects = Locator.xpath("//form[contains(@action,'addWebPart.view')]//tr/td/select[@name='name']");

        for (int i = 0; i <= 1; i++)
        {
            Locator loc = selects.index(i);
            String[] labels = selenium.getSelectOptions(loc.toString());
            for (String label : labels)
            {
                if (label.equals(webPartName))
                {
                    selenium.select(loc.toString(), webPartName);
                    submit(Locator.xpath("//form[contains(@action,'addWebPart.view')]").index(i));
                    return;
                }
            }
        }

        throw new RuntimeException("Could not find webpart with name: " + webPartName);
    }

    public boolean isTitleEqual(String match)
    {
        return match.equals(selenium.getTitle());
    }

    public void assertTitleEquals(String match)
    {
        assertEquals("Wrong page title", match, selenium.getTitle());
    }

    public void assertTitleContains(String match)
    {
        String title = selenium.getTitle();
        assertTrue("Page title: '"+title+"' doesn't contain '"+match+"'", title.contains(match));
    }

    public void assertNoLabkeyErrors()
    {
        assertElementNotPresent(Locator.xpath("//div[@class='labkey-error']"));
        assertElementNotPresent(Locator.xpath("//font[@class='labkey-error']"));
    }

    public void assertLabkeyErrorPresent()
    {
        assertTrue("No errors found", isElementPresent(Locator.xpath("//div[@class='labkey-error']")) ||
            isElementPresent(Locator.xpath("//font[@class='labkey-error']")));

    }

    public boolean isTextPresent(String... texts)
    {
        if(texts==null)
            return true;

        for (String text : texts)
        {
            //Need to unencode here? Selenium turns &nbsp; into space???
            text = text.replace("&nbsp;", " ");
            try
            {
                if (!selenium.isTextPresent(text))
                    return false;
            }
            catch (SeleniumException ex)
            {
                return false;
            }
        }
        return true;
    }

    public String getText(Locator elementLocator)
    {
        return selenium.getText(elementLocator.toString());
    }

    /** Verifies that all the strings are present in the page */
    public void assertTextPresent(String... texts)
    {
        if(texts==null)
            return;

        for (String text : texts)
        {
            text = text.replace("&nbsp;", " ");
            assertTrue("Text '" + text + "' was not present", isTextPresent(text));
        }
    }

    /** Verifies that all the strings are present in the page */
    public void assertTextPresent(List<String> texts)
    {
        if(texts==null)
            return;

        for (String text : texts)
        {
            String _text = text.replace("&nbsp;", " ");
            assertTrue("Text '" + text + "' was not present", isTextPresent(_text));
        }
    }

    //takes the arguments used to set a filter and transforms them into the description in the grid view
    //then verifies that this description is present
    public void assertFilterTextPresent(String column, String type, String value)
    {
        String desc = type + value;
        if(type.contains("Equals One Of"))
        {
            desc = "IS ONE OF (" + value.replace(";", ", ") + "))";
        }
        else if(type.contains("Contains One Of"))
        {
            desc = "CONTAINS ONE OF (" + value.replace(";", ", ") + "))";
        }
        else if(type.contains("Does Not Equal Any Of"))
        {
            desc = "IS NOT ANY OF (" + value.replace(";", ", ") + "))";
        }
        else if(type.contains("Does Not Contain Any Of"))
        {
            desc = "DOES NOT CONTAIN ANY OF (" + value.replace(";", ", ") + "))";
        }
        else if(type.equals("Equals"))
        {
            desc = column +  " = " + value;
        }
        else if(type.contains("Start") || type.contains("Contain"))    //Starts With, Does Not Start With, Contains, Does not Contain
        {
            desc = column + " " + type.toUpperCase() + " " + value;
        }
        else if(type.equals("Does Not Equal"))
        {
            desc = column + " <> " + value;
        }
        else if(type.contains("Greater"))
        {
            desc = column + " >";
            if(type.contains("Equal To"))
                desc+="=";
            desc += " " + value;
        }
        else if(type.contains("Less"))
        {
            desc = column + " <";
            if(type.contains("Equal To"))
                desc+="=";
            desc += " " + value;
        }
        else if(type.contains("Blank"))
        {
            desc = "NULL";
        }

        assertTextPresent(desc);

    }

    public void assertTextPresent(String text, int amount)
    {
        assertTextPresent(text, amount, false);
    }

    public void assertTextPresent(String text, int amount, boolean browserDependent)
    {
        // IE doesn't getHtmlSource the same as Firefox, it replaces \t and \n with spaces, so skip if IE
        if (!getBrowser().equals(IE_BROWSER))
        {
            int count = countText(text);

            if (browserDependent)
            {
                if (count == 0)
                    log("Your browser is probably out of date");
                else
                    assertTrue("Text '" + text + "' was not present " + amount + " times.  It was present " + count + " times", count == amount);
            }
            else
                assertTrue("Text '" + text + "' was not present " + amount + " times.  It was present " + count + " times", count == amount);
        }
    }

    public int countText(String text)
    {
        text = text.replace("&nbsp;", " ");
        String html = selenium.getHtmlSource();
        // Strip all JavaScript tags; in particular, the selenium-injected javascript tag, which can foul up the expected occurrences
        String source = html.replaceAll("(?msi)<script type=\"text/javascript\">.*?</script>", "");
        int current_index = 0;
        int count = 0;

        while ((current_index = source.indexOf(text, current_index + 1)) != -1)
            count++;
        return count;
    }

    public void assertTextNotPresent(String... texts)
    {
        if(texts==null)
            return;

        for(String text : texts)
        {
            text = text.replace("&nbsp;", " ");
            assertFalse("Text '" + text + "' was present", isTextPresent(text));
        }
    }

    public String getTextInTable(String dataRegion, int row, int column)
    {
        String id = Locator.xq(dataRegion);
        return selenium.getText("//table[@id="+id+"]/tbody/tr["+row+"]/td["+column+"]");
    }

    public void assertTextAtPlaceInTable(String textToCheck, String dataRegion, int row, int column)
    {
       assertTrue(textToCheck+" is not at that place in the table", textToCheck.compareTo(getTextInTable(dataRegion, row, column))==0);
    }

    /**
     * Searches only the displayed text in the body of the page, not the HTML source.
     */
    public boolean isTextBefore(String text1, String text2)
    {
        String source = selenium.getBodyText();
        return (source.indexOf(text1) < source.indexOf(text2));
    }

    /**
     *
     * @param text
     * @return null = yes, present in this order
     * otherwise returns out of order string and explanation of error
     */
    public String isPresentInThisOrder(Object... text)
    {
        String source = selenium.getBodyText();
        int previousIndex = -1;
        String previousString = null;

        for (Object o : text)
        {
            String s = o.toString();
            int index = source.indexOf(s);

            if(index == -1)
                return s + " not found";
            if(index <= previousIndex)
                return s + " occured out of order";
            previousIndex = index;
            previousString = s;
        }
        return null;
    }
    // Searches only the displayed text in the body of the page, not the HTML source.
    public void assertTextPresentInThisOrder(Object... text)
    {
        String success = isPresentInThisOrder(text);
        assertTrue(success, success==null);
    }

    public void assertTextBefore(String text1, String text2)
    {
        assertTextPresentInThisOrder(text1, text2);
    }

    public void waitForPageToLoad(int millis)
    {
        if( selenium.isAlertPresent() )
            fail("ERROR: Unexpected alert.\n" + selenium.getAlert());
        else
        {
            _testTimeout = true;
            selenium.waitForPageToLoad(Integer.toString(millis));
            _testTimeout = false;
        }
    }

    public void waitForPageToLoad()
    {
        waitForPageToLoad(defaultWaitForPage);
    }


    public void waitForExtReady()
    {
        waitForElement(Locator.id("seleniumExtReady"), defaultWaitForPage);
    }

    public boolean doesElementAppear(Checker checker, int wait)
    {
        int time = 0;

        while ( time < wait )
        {
            try
            {
                if( checker.check() )
                    return true;
            }
            catch (Exception ignore) {} // Checker exceptions count as a false check
            sleep(100);
            time += 100;
        }

        if (!checker.check())
        {
            _testTimeout = true;
            return false;
        }

        return false;
    }

    public void waitFor(Checker checker, String failMessage, int wait)
    {
        if (!doesElementAppear(checker, wait))
            fail(failMessage + " ["+wait+"ms]");
    }

    /**
     * @deprecated Use {@link org.labkey.test.util.ExtHelper#waitForExt3MaskToDisappear(int)}
     */
    @Deprecated public void waitForExtMaskToDisappear()
    {
        waitForExtMaskToDisappear(WAIT_FOR_JAVASCRIPT);
    }

    /**
     * @deprecated Use {@link org.labkey.test.util.ExtHelper#waitForExt3MaskToDisappear(int)}
     */
    @Deprecated public void waitForExtMaskToDisappear(int wait)
    {
        _extHelper.waitForExt3MaskToDisappear(wait);
    }

    /**
     * @deprecated Use {@link org.labkey.test.util.ExtHelper#waitForExt3Mask(int)}
     */
    @Deprecated public void waitForExtMask()
    {
        waitForExtMask( WAIT_FOR_JAVASCRIPT );
    }

    /**
     * @deprecated Use {@link org.labkey.test.util.ExtHelper#waitForExt3Mask(int)}
     */
    @Deprecated public void waitForExtMask(int wait)
    {
        _extHelper.waitForExt3Mask(wait);
    }

    //like wait for ExtMask, but waits for a draggable mask (for example, the file rename mask)
    public void waitForDraggableMask()
    {
        waitForDraggableMask(WAIT_FOR_JAVASCRIPT);
    }

    //like wait for ExtMask, but waits for a draggable mask (for example, the file rename mask)
    public void waitForDraggableMask(int wait)
    {
        waitForElement(Locator.xpath("//div[contains(@class, 'x-window-draggable')]"), wait);

    }

    public void waitForAlert(String alertText, int wait)
    {
        waitFor(new Checker(){public boolean check(){return isAlertPresent();}}, "No alert appeared.", wait);
        assertAlert(alertText);
    }

    protected File getTestTempDir()
    {
        File buildDir = new File(getLabKeyRoot(), "build");
        return new File(buildDir, "testTemp");
    }

    /**
     * pre-condition: on Views and Scripting Configuration page
     * @return is a Perl enginge configured?
     */
    public boolean isPerlEngineConfigured()
    {
        waitForElement(Locator.xpath("//div[@id='enginesGrid']//td//div[.='js']"), WAIT_FOR_JAVASCRIPT);

        return isElementPresent(Locator.xpath("//div[@id='enginesGrid']//td//div[.='pl']"));
    }

    /**
     * pre-condition: on Views and Scripting Configuration page
     * @return is an R enginge configured?
     */
    public boolean isREngineConfigured()
    {
        // need to allow time for the server to return the engine list and the ext grid to render
        // wait for mozilla rhino (should be automatically included for all installations)
        waitForElement(Locator.xpath("//div[@id='enginesGrid']//td//div[.='js']"), WAIT_FOR_JAVASCRIPT);

        return isElementPresent(Locator.xpath("//div[@id='enginesGrid']//td//div[.='R,r']"));
    }

    @Deprecated
    /**
     * @deprecated Migrate to {@link org.labkey.test.BaseWebDriverTest} and use {@link org.labkey.test.BaseWebDriverTest#click(Locator)}
     */
    public void mouseClick(String locator)
    {
        selenium.mouseClick(locator);
    }

    protected void setSelectedFields(String containerPath, String schema, String query, String viewName, String[] fields)
    {
        pushLocation();
        beginAt("/query" + containerPath + "/internalNewView.view");
        setFormElement("ff_schemaName", schema);
        setFormElement("ff_queryName", query);
        if (viewName != null)
            setFormElement("ff_viewName", viewName);
        submit();
        StringBuilder strFields = new StringBuilder(fields[0]);
        for (int i = 1; i < fields.length; i ++)
        {
            strFields.append("&");
            strFields.append(fields[i]);
        }
        setFormElement("ff_columnList", strFields.toString());
        submit();
        popLocation();
    }

    protected void clickExportToText()
    {
        clickButton("Export", 0);
        _extHelper.clickSideTab("Text");
        clickButton("Export to Text");
    }

    /**
     * Use UI to export data region
     * note that Selenium/Firefox currently can't handle the dialogue that will pop up if you choose anything but script
     * @param tab   Excel, Text, or Script
     * @param type the specific radiobutton to choose
     */
    protected void exportDataRegion(String tab, String type)
    {
        clickButton("Export", 0);
        waitForText("Script");
        sleep(1500);
        _extHelper.clickSideTab(tab);
        if(type!=null)
        {
            click(Locator.xpath("//tr[td[contains(text()," +  Locator.xq(type) + ")]]/td/input"));
        }
        if(tab.equals("Script"))
        {
            clickButtonContainingText("Create Script", 0);
        }
        else
        {
            clickButtonContainingText("Export To " + tab, 0);
        }
    }

    protected void exportFolderAsZip()
    {
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Export"));
        checkRadioButton("location", 1);
        clickButton("Export");
    }

    public void setModuleProperties(List<ModulePropertyValue> values)
    {
        goToFolderManagement();
        log("setting module properties");
        clickAndWait(Locator.linkWithText("Module Properties"));
        waitForText("Save Changes");
        boolean changed = false;
        for (ModulePropertyValue value : values)
        {
            log("setting property: " + value.getPropertyName() + " for container: " + value.getContainerPath() + " to value: " + value.getValue());
            Map<String, String> map = new HashMap<>();
            map.put("moduleName", value.getModuleName());
            map.put("containerPath", value.getContainerPath());
            map.put("propName", value.getPropertyName());
            waitForText(value.getPropertyName()); //wait for the property name to appear
            String query = ComponentQuery.fromAttributes("field", map);
            Ext4FieldRef ref = _ext4Helper.queryOne(query, Ext4FieldRef.class);
            String val = ref.getValue();
            if(StringUtils.isEmpty(val) || !val.equals(value.getValue()))
            {
                changed = true;
                ref.setValue(value.getValue());
            }
        }
        if (changed)
        {
            clickButton("Save Changes", 0);
            waitForText("Properties saved");
            clickButton("OK", 0);
        }
        else
        {
            log("properties were already set, no changed needed");
        }
    }

    public interface Checker
    {
        public boolean check();
    }

    public void waitForExt4FolderTreeNode(String nodeText, int wait)
    {
        final Locator locator = Ext4HelperWD.Locators.folderManagementTreeNode(nodeText);
        String failMessage = "Ext 4 Tree Node with locator " + locator + " did not appear.";
        waitFor(new Checker()
        {
            public boolean check()
            {
                return isElementPresent(locator);
            }
        }, failMessage, wait);
    }

    public void waitForElement(final Locator locator)
    {
        waitForElement(locator, defaultWaitForPage);
    }

    /**
     *
     * @param locator Element to wait for
     * @param wait amount of time to wait for
     * @param failIfNotFound should fail if element is not found?  If not, will return false
     * @return
     */
    public boolean waitForElement(final Locator locator, int wait, boolean failIfNotFound)
    {

        String failMessage = "Element with locator " + locator + " did not appear.";
        Checker checker = new Checker()
        {
            public boolean check()
            {
                return isElementPresent(locator);
            }
        };

        if(!doesElementAppear(checker, wait))
            if(failIfNotFound)
                fail(failMessage);
            else
                return false;
        return true;
    }

    public void waitForElement(final Locator locator, int wait)
    {
        waitForElement(locator, wait, true);
//        String failMessage = "Element with locator " + locator + " did not appear.";
//        waitFor(new Checker()
//        {
//            public boolean check()
//            {
//                return isElementPresent(locator);
//            }
//        }, failMessage, wait);
    }

    public void waitForElementToDisappear(final Locator locator)
    {
        waitForElementToDisappear(locator, WAIT_FOR_JAVASCRIPT);
    }

    public void waitForElementToDisappear(final Locator locator, int wait)
    {
        String failMessage = "Element with locator " + locator + " was still present.";
//        if(locator.toString().contains("xt") && getBrowser().equals("*iexploreproxy"))
//        {
//            // IE can't detect some ext elements disappearing
//            sleep(10000);
//            return;
//        }
        waitFor(new Checker()
        {
            public boolean check()
            {
                return !isElementPresent(locator);
            }
        }, failMessage, wait);
    }

    public void waitForTextToDisappear(final String text)
    {
        waitForTextToDisappear(text, WAIT_FOR_JAVASCRIPT);
    }

    public void waitForTextToDisappear(final String text, int wait)
    {
        String failMessage = "Text: " + text + " was still present after [" + wait + "ms]";
//        if(getBrowser().equals("*iexploreproxy"))
//        {
//            // IE can't detect some ext elements disappearing
//            sleep(10000);
//            return;
//        }
        waitFor(new Checker()
        {
            public boolean check()
            {
                return !isTextPresent(text);
            }
        }, failMessage, wait);
    }

    public void waitForText(final String text)
    {
         waitForText(text, defaultWaitForPage);
    }

    public void waitForTextWithRefresh(String text, int wait)
    {
        for(int i=0; i<wait; i+=1000)
        {
            if(isTextPresent(text))
                return;
            else
                sleep(1000);
            refresh();
        }
        fail(text + " did not appear");
    }
    public void waitForText(final String text, int wait)
    {
        String failMessage = "'" + text + "' did not appear";
        waitFor(new Checker()
        {
            public boolean check()
            {
                return isTextPresent(text);
            }
        }, failMessage, wait);
    }

    public void waitForText(final String text, final int count, int wait)
    {
        final String failMessage = "'"+text+"' was not present "+count+" times.";
        waitFor(new Checker()
        {
            public boolean check()
            {
                int actualCount = countText(text);
                return actualCount == count;
            }
        }, failMessage, wait);
    }

    protected final String firstForm = "//td[@id='bodypanel']//form[1]";

    /**
     * @deprecated Use {@link #clickButton(String)}
     */
    @Deprecated public void submit()
    {
        selenium.submit(firstForm);
        waitForPageToLoad();
    }

    /**
     * @deprecated Use {@link #clickButton(String)}
     */
    @Deprecated public void submit(Locator formLocator)
    {
        selenium.submit(formLocator.toString());
        waitForPageToLoad();
    }

    /**
     * @deprecated Use {@link #clickButton(String)}
     */
    @Deprecated public void submit(String buttonName)
    {
        Locator l = findButton(buttonName);

        assertTrue("Button with name '" + buttonName + "' not found", null != l);

        selenium.click(l.toString());
        waitForPageToLoad();
    }

    public Locator findButton(String name)
    {
        // Note: we do not use inputs anymore, but instead links spans inside
        Locator l = Locator.tagWithName("a", name);
        if (isElementPresent(l))
            return l;

        return null;
    }

    public boolean isElementPresent(Locator loc)
    {
        try
        {
            return selenium.isElementPresent(loc.toString());
        }
        catch(SeleniumException e)
        {
            /*ignore permission denied errors in IE when page refreshes during this check*/
        }
        return false;
    }

    public void assertElementPresent(Locator loc)
    {
        assertTrue("Element '" + loc + "' is not present", isElementPresent(loc));
    }

    public void assertElementPresent(String message, Locator loc)
    {
        assertTrue(message, isElementPresent(loc));
    }

    public void assertElementPresent(Locator.XPathLocator loc, int amount)
    {
        assertEquals("Xpath '" + loc.getPath() + "' not present expected number of times.", amount, getXpathCount(loc));
    }

    public void assertElementContains(Locator loc, String text)
    {
        String elemText = selenium.getText(loc.toString());
        if(elemText == null)
            fail("The element at location " + loc.toString() + " contains no text! Expected '" + text + "'.");
        if(!elemText.contains(text))
            fail("The element at location '" + loc.toString() + "' contains '" + elemText + "'; expected '" + text + "'.");
    }

    public boolean elementContains(Locator loc, String text)
    {
        String elemText = selenium.getText(loc.toString());
        return (elemText != null && elemText.contains(text));
    }

    public void assertFormElementEquals(String elementName, String value)
    {
        assertFormElementEquals(new Locator.DeprecatedLocator(elementName), value);
    }

    public void waitForFormElementToEqual(final Locator locator, final String value)
    {
        String failMessage = "Field with locator " + locator + " did not equal " + value + ".";
        waitFor(new Checker()
        {
            public boolean check()
            {
                return value.equals(getFormElement(locator));
            }
        }, failMessage, WAIT_FOR_JAVASCRIPT);
    }

    public void waitForFormElementToNotEqual(final Locator locator, final String value)
    {
        String failMessage = "Field with locator " + locator + " did not equal " + value + ".";
        waitFor(new Checker()
        {
            public boolean check()
            {
                return !value.equals(getFormElement(locator));
            }
        }, failMessage, WAIT_FOR_JAVASCRIPT);
    }

    public String getFormElement(Locator loc)
    {
        return selenium.getValue(loc.toString());
    }

    /**
     * @deprecated Use {@link #getFormElement(Locator)}
     * @param elementName
     * @return
     */
    @Deprecated
    public String getFormElement(String elementName)
    {
        Locator loc = new Locator.DeprecatedLocator(elementName);
        return selenium.getValue(loc.toString());
    }

    public void assertFormElementEquals(Locator loc, String value)
    {
        assertEquals("Form element '" + loc + "' was not equal to '" + value + "'", value, selenium.getValue(loc.toString()));
    }

    public void assertFormElementNotEquals(Locator loc, String value)
    {
        assertNotSame("Form element '" + loc + "' was equal to '" + value + "'", value, selenium.getValue(loc.toString()));
    }

    public void assertOptionEquals(Locator loc, String value)
    {
        assertEquals("Option '" + loc + "' was not equal '" + value + "'", selenium.getSelectedLabel(loc.toString()), value);
    }

    public String getSelectedOptionText(Locator loc)
    {
        return selenium.getSelectedLabel(loc.toString());
    }

    public String getSelectedOptionValue(Locator loc)
    {
        return selenium.getSelectedValue(loc.toString());
    }

    public void assertElementNotPresent(String errorMsg, Locator loc)
    {
        assertFalse(errorMsg, isElementPresent(loc));
    }

    public void assertElementNotPresent(Locator loc)
    {
        assertElementNotPresent("Element was present in page: " + loc, loc);
    }

    public void assertElementNotVisible(Locator loc)
    {
        assertFalse("Element was visible in page: " + loc, selenium.isVisible(loc.toString()));
    }

    public void assertElementVisible(Locator loc)
    {
        assertTrue("Element was not visible in page: " + loc, selenium.isVisible(loc.toString()));
    }

    /**
     * @deprecated Use {@link #isElementPresent(Locator)}
     */
    @Deprecated public boolean isLinkPresentWithText(String text)
    {
        log("Checking for link with exact text '" + text + "'");
        return isElementPresent(Locator.linkWithText(text));
    }

    public boolean isLinkPresentWithTextCount(String text, int count)
    {
        log("Checking for " + count + " links with exact text '" + text + "'");
        return countLinksWithText(text) == count;
    }

    /**
     * @deprecated Use {@link #isElementPresent(Locator)}
     */
    @Deprecated public boolean isLinkPresentWithText(String text, int index)
    {
        return countLinksWithText(text) > index;
    }

    /**
     * @deprecated Use {@link #isElementPresent(Locator)}
     */
    @Deprecated public boolean isLinkPresentContainingText(String text)
    {
        log("Checking for link containing text '" + text + "'");
        return isElementPresent(Locator.linkContainingText(text));
    }

    /**
     * @deprecated Use {@link #assertElementPresent(Locator)}
     */
    @Deprecated public void assertLinkPresentContainingText(String text)
    {
        assertTrue("Could not find link containing text '" + text + "'", isLinkPresentContainingText(text));
    }

    /**
     * @deprecated Use {@link #assertElementPresent(Locator)}
     */
    @Deprecated public void assertLinkPresentWithText(String text)
    {
        assertTrue("Could not find link with text '" + text + "'", isLinkPresentWithText(text));
    }

    /**
     * @deprecated Use {@link #assertElementNotPresent(Locator)}
     */
    @Deprecated public void assertLinkNotPresentWithText(String text)
    {
        assertFalse("Found a link with text '" + text + "'", isLinkPresentWithText(text));
    }

    public void assertAtUserUserLacksPermissionPage()
    {
        assertTextPresent(PERMISSION_ERROR);
        assertTitleEquals("401: Error Page -- 401: User does not have permission to perform this operation");
    }

    /**
     * @deprecated Use {@link #isElementPresent(Locator)}
     */
    @Deprecated public boolean isLinkPresentWithTitle(String title)
    {
        log("Checking for link with exact title '" + title + "'");
        return isElementPresent(Locator.linkWithTitle(title));
    }

    /**
     * @deprecated Use {@link #assertElementPresent(Locator)}
     */
    @Deprecated public void assertLinkPresentWithTitle(String title)
    {
        assertTrue("Could not find link with title '" + title + "'", isLinkPresentWithTitle(title));
    }

    /**
     * @deprecated Use {@link #assertElementNotPresent(Locator)}
     */
    @Deprecated public void assertLinkNotPresentWithTitle(String title)
    {
        assertFalse("Found a link with title '" + title + "'", isLinkPresentWithTitle(title));
    }

    /**
     * @deprecated Use {@link #getXpathCount(org.labkey.test.Locator.XPathLocator)}
     */
    @Deprecated public int countLinksWithText(String text)
    {
        return selenium.getXpathCount("//a[text() = "+Locator.xq(text)+"]").intValue();
    }

    public void assertLinkPresentWithTextCount(String text, int count)
    {
        assertEquals("Link with text '" + text + "' was not present the expected number of times", count, countLinksWithText(text));
    }

    public void scrollIntoView(Locator loc)
    {
        String locId = getAttribute(loc, "id");
        selenium.getEval("selenium.browserbot.getCurrentWindow().document.getElementById('"+locId+"').scrollIntoView(true);");
    }

    public void click(Locator l)
    {
        clickAndWait(l, 0);
    }

    public void clickAt(Locator l, String coord)
    {
        selenium.clickAt(l.toString(), coord);
    }

    public void clickAndWait(Locator l)
    {
        clickAndWait(l, defaultWaitForPage);
    }

    public static final int WAIT_FOR_EXT_MASK_TO_DISSAPEAR = -1;
    public static final int WAIT_FOR_EXT_MASK_TO_APPEAR = -2;
    public void clickAndWait(Locator l, int millis)
    {
        selenium.click(l.toString());
        if (millis > 0)
            waitForPageToLoad(millis);
        else if(millis==WAIT_FOR_EXT_MASK_TO_APPEAR)
            waitForExtMask();
        else if(millis==WAIT_FOR_EXT_MASK_TO_DISSAPEAR)
            waitForExtMaskToDisappear();
    }

    public void doubleClick(Locator l)
    {
        doubleClickAndWait(l, 0);
    }

    public void doubleClickAndWait(Locator l, int millis)
    {
        selenium.doubleClick(l.toString());
        if (millis > 0)
            waitForPageToLoad(millis);

    }

    public void selectFolderTreeItem(String folderName)
    {
        click(Locator.permissionsTreeNode(folderName));
    }

    public void mouseOut(Locator l)
    {
        selenium.mouseOut(l.toString());
    }

    public void mouseOver(Locator l)
    {
        selenium.mouseOver(l.toString());
    }

    public void mouseDown(Locator l)
    {
        selenium.mouseDown(l.toString());
    }

    public void mouseUp(Locator l)
    {
        selenium.mouseUp(l.toString());
    }

    public int getElementIndex(Locator.XPathLocator l)
    {
        return selenium.getElementIndex(l.toString()).intValue();
    }

    public void dragAndDrop(Locator from, Locator to)
    {
        selenium.mouseOver(from.toString());
        selenium.mouseDownAt(from.toString(), "1,1");
        selenium.mouseMoveAt(to.toString(), "1,1");
        selenium.mouseOver(to.toString());
        selenium.mouseUpAt(to.toString(), "1,1");
    }

    public enum Position
    {top, bottom, middle}

    public void dragAndDrop(Locator from, Locator to, Position pos)
    {
        int y;
        if ( pos == Position.top )
            y = 1;
        else if ( pos == Position.bottom )
            y = selenium.getElementHeight(to.toString()).intValue() - 1;
        else // pos == Position.middle
            y = selenium.getElementHeight(to.toString()).intValue() / 2;

        selenium.mouseOver(from.toString());
        selenium.mouseDownAt(from.toString(), "1,1");
        selenium.mouseMoveAt(to.toString(), "1," + y);
        selenium.mouseOver(to.toString());
        selenium.mouseUpAt(to.toString(), "1," + y);
    }

    public void dragAndDrop(Locator el, int xOffset, int yOffset)
    {
        String x = "" + (1 + xOffset);
        String y = "" + (1 + yOffset);

        selenium.mouseOver(el.toString());
        selenium.mouseDownAt(el.toString(), "1,1");
        selenium.mouseMoveAt(el.toString(), x + "," + y);
        selenium.mouseUpAt(el.toString(), x + "," + y);
    }

    public void clickTab(String tabname)
    {
        log("Selecting tab " + tabname);
        clickAndWait(Locator.folderTab(tabname));
    }

    public void verifyTabSelected(String caption)
    {
        assertTrue("Tab not selected: " + caption, isElementPresent(Locator.xpath("//li[contains(@class, labkey-tab-active)]/a[text() = '"+caption+"']")));
    }

    public int getImageWithAltTextCount(String altText)
    {
        String js = "function countImagesWithAlt(txt) {var doc=selenium.browserbot.getCurrentWindow().document; var count = 0; for (var i = 0; i < doc.images.length; i++) {if (doc.images[i].alt == txt) count++;} return count}; ";
        js = js + "countImagesWithAlt('" + altText + "');";
        String count = selenium.getEval(js);
        return Integer.parseInt(count);
    }

    public String getTableCellText(String tableId, int row, int column)
    {
        return getText(Locator.xpath("//table[@id="+Locator.xq(tableId)+"]/tbody/tr["+(row+1)+"]/*[(name()='TH' or name()='TD' or name()='th' or name()='td') and position() = "+(column+1)+"]"));
    }

    public Locator getSimpleTableCell(Locator.XPathLocator table, int row, int column)
    {
        return Locator.xpath(table.toXpath() + "/tbody/tr["+(row+1)+"]/td[" + (column +1)  + "]");
    }

    public String getTableCellText(String tableName, int row, String columnTitle)
    {
        return getTableCellText(tableName, row, getColumnIndex(tableName, columnTitle));
    }

    public boolean isTableCellEqual(String tableName, int row, int column, String value)
    {
        return value.equals(getTableCellText(tableName, row, column));
    }

    public boolean isTableCellEqual(String tableName, int row, String columnTitle, String value)
    {
        return value.equals(getTableCellText(tableName, row, columnTitle));
    }

    public boolean areTableCellsEqual(String tableNameA, int rowA, int columnA, String tableNameB, int rowB, int columnB)
    {
        return getTableCellText(tableNameA, rowA, columnA).equals(getTableCellText(tableNameB, rowB, columnB));
    }

    public void assertTableCellTextEquals(String tableName, int row, int column, String value)
    {
        assertEquals(tableName + "." + String.valueOf(row) + "." + String.valueOf(column) + " != \"" + value + "\"", value, getTableCellText(tableName, row, column));
    }

    public void assertTableCellTextEquals(String tableName, int row, String columnTitle, String value)
    {
        assertTableCellTextEquals(tableName, row, getColumnIndex(tableName, columnTitle), value);
    }

    public void assertTableCellContains(String tableName, int row, int column, String... strs)
    {
        String cellText = getTableCellText(tableName, row, column);

        for (String str : strs)
        {
            assertTrue(tableName + "." + row + "." + column + " should contain \'" + str + "\' (actual value is " + cellText + ")", cellText.contains(str));
        }
    }

    public void assertTableCellContains(String tableName, int row, String columnTitle, String... strs)
    {
        assertTableCellContains(tableName, row, getColumnIndex(tableName, columnTitle), strs);
    }

    public void assertTableCellNotContains(String tableName, int row, int column, String... strs)
    {
        String cellText = getTableCellText(tableName, row, column);

        for (String str : strs)
        {
            assertFalse(tableName + "." + row + "." + column + " should not contain \'" + str + "\'", cellText.contains(str));
        }
    }

    public void assertTableCellNotContains(String tableName, int row, String columnTitle, String... strs)
    {
        assertTableCellNotContains(tableName, row, getColumnIndex(tableName, columnTitle), strs);
    }

    public void assertTableCellsEqual(String tableName, int rowA, int columnA, int rowB, int columnB)
    {
        assertTableCellsEqual(tableName, rowA, columnA, tableName, rowB, columnB);
    }

    public void assertTableCellsEqual(String tableName, int rowA, String columnTitleA, int rowB, String columnTitleB)
    {
        assertTableCellsEqual(tableName, rowA, getColumnIndex(tableName, columnTitleA), tableName, rowB, getColumnIndex(tableName, columnTitleB));
    }

    public void assertTableCellsEqual(String tableNameA, int rowA, String columnTitleA, String tableNameB, int rowB, String columnTitleB)
    {
        assertTableCellsEqual(tableNameA, rowA, getColumnIndex(tableNameA, columnTitleA), tableNameB, rowB, getColumnIndex(tableNameB, columnTitleB));
    }

    public void assertTableCellsEqual(String tableNameA, int rowA, int columnA, String tableNameB, int rowB, int columnB)
    {
        assertTrue("Table cells not equal: " + tableNameA + "." + String.valueOf(rowA) + "." + String.valueOf(columnA) + " & " + tableNameB + "." + String.valueOf(rowB) + "." + String.valueOf(columnB), areTableCellsEqual(tableNameA, rowA, columnA, tableNameB, rowB, columnB));
    }

    /*
    getColumnIndex works for standard labkey data grids
     */
    public int getColumnIndex(String tableName, String columnTitle)
    {
        int col = selenium.getXpathCount("//table[@id='"+tableName+"']/tbody/tr[contains(@id, 'dataregion_column_header_row') and not(contains(@id, 'spacer'))]/td[./div/.='"+columnTitle+"']/preceding-sibling::*").intValue();
        if(col == 0)
            fail("Column '" + columnTitle + "' not found in table '" + tableName + "'");

        return col;
    }

    // Specifies cell values in a TSV string -- values are separated by tabs, rows are separated by \n
    public void assertTableRowsEqual(String tableId, int startRow, String cellValuesTsv)
    {
        String[] lines = cellValuesTsv.split("\n");
        String[][] cellValues = new String[lines.length][];

        for (int row = 0; row < cellValues.length; row++)
            cellValues[row] = lines[row].split("\t");

        assertTableRowsEqual(tableId, startRow, cellValues);
    }

    public void assertTableRowsEqual(String tableId, int startRow, String[][] cellValues)
    {
        for (int row = 0; row < cellValues.length; row++)
            for (int col = 0; col < cellValues[row].length; col++)
                assertTableCellTextEquals(tableId, row + startRow, col, cellValues[row][col]);
    }

    // Returns the text contents of every "Status" cell in the pipeline StatusFiles grid
    public List<String> getPipelineStatusValues()
    {
        DataRegionTable status = new DataRegionTable("StatusFiles", this, true, false);
        return status.getColumnDataAsText("Status");
    }

    public void setPipelineRoot(String rootPath)
    {
        setPipelineRoot(rootPath, false);
    }

    private void dumpPipelineFiles(String path)
    {
        File dumpDir = ensureDumpDir();

        // moves all files under @path, created by the test, to the TeamCity publish directory
        ArrayList<File> files = listFilesRecursive(new File(path), new NonSVNFilter());
        for (File file : files)
        {
            if ( file.isFile() )
            {
                File dest = new File(dumpDir, file.getParent().substring(path.length()));
                if (!dest.exists())
                    dest.mkdirs();
                file.renameTo(new File(dest, file.getName()));
            }
        }
    }

    private void dumpPipelineLogFiles(String path)
    {
        File dumpDir = ensureDumpDir();

        // moves all .log files under @path, created by the test, to the TeamCity publish directory
        ArrayList<File> files = listFilesRecursive(new File(path), new NonSVNFilter());
        for (File file : files)
        {
            if ( file.isFile() && file.getName().endsWith(".log") )
            {
                File dest = new File(dumpDir, file.getParent().substring(path.length()));
                if (!dest.exists())
                    dest.mkdirs();
                file.renameTo(new File(dest, file.getName()));
            }
        }
    }

    private ArrayList<File> listFilesRecursive(File path, FilenameFilter filter)
    {
        File[] files = path.listFiles(filter);
        ArrayList<File> allFiles = new ArrayList<>();
        if (files != null)
        {
            for (File file : files)
            {
                if ( file.isDirectory() )
                    allFiles.addAll(listFilesRecursive(file, filter));
                else // file.isFile()
                    allFiles.add(file);
            }
        }
        return allFiles;
    }

    private class NonSVNFilter implements FilenameFilter
    {
        SVNStatusClient svn = new SVNStatusClient((ISVNAuthenticationManager)null, null);

        public NonSVNFilter() { }

        public boolean accept(File directory, String filename)
        {
            File file = new File(directory, filename);
            try
            {
                return (!file.isHidden() && file.isDirectory() ||
                        _startTime < file.lastModified() && svn.doStatus(file, false).getContentsStatus().equals(SVNStatusType.STATUS_UNVERSIONED));
            }
            catch (SVNException e)
            {
                return e.getMessage().contains("is not a working copy");
            }
        }
    }

    public void setPipelineRoot(String rootPath, boolean inherit)
    {
        log("Set pipeline to: " + rootPath);
        goToModule("Pipeline");
        clickButton("Setup");

        if (isLinkPresentWithText("override"))
        {
            if (inherit)
                clickAndWait(Locator.linkWithText("modify the setting for all folders"));
            else
                clickAndWait(Locator.linkWithText("override"));
        }
        clickRadioButtonById("pipeOptionProjectSpecified");
        setFormElement("pipeProjectRootPath", rootPath);

        submit();
        log("Finished setting pipeline to: " + rootPath);
    }

    // Returns true if any status value is "ERROR"
    public boolean hasError(List<String> statusValues)
    {
        return statusValues.contains("ERROR");
    }

    // Returns count of "COMPLETE"
    public int getCompleteCount(List<String> statusValues)
    {
        int complete = 0;

        for (String statusValue : statusValues)
            if ("COMPLETE".equals(statusValue) || "IMPORT FOLDER COMPLETE".equals(statusValue))
                complete++;

        return complete;
    }

    // Returns count of "COMPLETE" and "ERROR"
    public int getFinishedCount(List<String> statusValues)
    {
        int finsihed = 0;
        for (String statusValue : statusValues)
            if ("COMPLETE".equals(statusValue) || "ERROR".equals(statusValue) || "IMPORT FOLDER COMPLETE".equals(statusValue))
                finsihed++;
        return finsihed;
    }

    // Returns the value of all cells in the specified column
    public List<String> getTableColumnValues(String tableName, int column)
    {
        int rowCount = getTableRowCount(tableName);

        List<String> values = new ArrayList<>(rowCount);

        for (int i = 0; i < rowCount; i++)
        {
            try
            {
                values.add(getTableCellText(tableName, i, column));
            }
            catch(Exception ignore) {}
        }

        return values;
    }

    public void showNumberInTable(String shareValue)
    {
        clickButton("Page Size", 0);
        Locator l = Locator.id("Page Size:" + shareValue);
        waitAndClickAndWait(l);
    }

    /**get values for all specifed columns for all pages of the table
     * preconditions:  must be on start page of table
     * postconditions:  at start of table
     * @param tableName
     * @param columnNames
     * @return
     */
    protected  List<List<String>> getColumnValues(String tableName, String... columnNames)
    {
        boolean moreThanOnePage = isTextPresent("Next");
        if(moreThanOnePage)
        {
            showNumberInTable("All");
        }
        List<List<String>> columns = new ArrayList<>();
        for(int i=0; i<columnNames.length; i++)
        {
            columns.add(new ArrayList<String>());
        }

        DataRegionTable table = new DataRegionTable(tableName, this);
        for(int i=0; i<columnNames.length; i++)
        {
            columns.get(i).addAll(table.getColumnDataAsText(columnNames[i]));
        }

        if(moreThanOnePage)
        {
            showNumberInTable("100");
        }
        return columns;
    }

    // Returns the number of rows (both <tr> and <th>) in the specified table
    public int getTableRowCount(String tableName)
    {
        return selenium.getXpathCount("//table[@id=" + Locator.xq(tableName) + "]/thead/tr").intValue() + selenium.getXpathCount("//table[@id=" + Locator.xq(tableName) + "]/tbody/tr").intValue();
    }

    public int getTableColumnCount(String tableId)
    {
        return getXpathCount(Locator.xpath("//table[@id="+Locator.xq(tableId)+"]/colgroup/col"));
    }

    public boolean isButtonPresent(String text)
    {
        return (getButtonLocator(text) != null);
    }

    public void clickButtonByIndex(String text, int index)
    {
        clickButtonByIndex(text, index, defaultWaitForPage);
    }

    public void clickButtonByIndex(String text, int index, int wait)
    {
        Locator.XPathLocator buttonLocator = getButtonLocator(text, index);
        if (buttonLocator != null)
            clickAndWait(buttonLocator, wait);
        else
            fail("No button found with text \"" + text + "\" at index " + index);
    }

    public Locator.XPathLocator getButtonLocator(String text, int index)
    {
        // check for normal labkey nav button:
        Locator.XPathLocator locator = Locator.navButton(text, index);
        if (isElementPresent(locator))
            return locator;

        // check for normal labkey submit button:
        locator = Locator.navButton(text, index);
        if (isElementPresent(locator))
            return locator;

        // check for Ext button:
        locator = Locator.extButton(text, index);
        if (isElementPresent(locator))
            return locator;

        // check for Ext 4 button:
        locator = Locator.ext4Button(text).index(index);
        if (isElementPresent(locator))
            return locator;

        return null;
    }

    public Locator.XPathLocator getButtonLocator(String text)
    {
        Locator.XPathLocator locator;

        // check for normal labkey nav button:
        locator = Locator.navButton(text);
        if (isElementPresent(locator))
            return locator;

        // check for normal button:
        locator = Locator.button(text);
        if (isElementPresent(locator))
            return locator;

        // check for Ext button:
        locator = Locator.extButton(text);
        if (isElementPresent(locator))
            return locator;

        // check for Ext 4 button:
        locator = Locator.ext4Button(text);
        if (isElementPresent(locator))
            return locator;

        // check for GWT button:
        locator = Locator.gwtButton(text);
        if (isElementPresent(locator))
            return locator;

        return null;
    }

    protected Locator.XPathLocator getButtonLocatorContainingText(String text)
    {
        // check for normal button:
        Locator.XPathLocator locator = Locator.buttonContainingText(text);
        if (isElementPresent(locator))
            return locator;

        // check for normal labkey submit/nav button:
        locator = Locator.navButtonContainingText(text);
        if (isElementPresent(locator))
            return locator;

        // check for Ext3 button:
        locator = Locator.extButtonContainingText(text);
        if (isElementPresent(locator))
            return locator;

        // check for Ext4 button:
        locator = Locator.ext4ButtonContainingText(text);
        if (isElementPresent(locator))
            return locator;

        return null;
    }


    // waits for page to load after button is clicked
    // use clickButton(text, 0) to click a button and continure immediately
    public void clickButton(String text)
    {
        clickButton(text, defaultWaitForPage);
    }

    /**
     * click a button with text text and wait for text waitForText to appear
     * @param text
     * @param waitForText
     */
    public void clickButton(String text, String waitForText)
        {
            clickButton(text, 0);
            waitForText(waitForText);
        }


    public void clickButton(String text, int waitMillis)
    {
        Locator.XPathLocator buttonLocator = getButtonLocator(text);
        if (buttonLocator != null)
            clickAndWait(buttonLocator, waitMillis);
        else if(waitMillis==WAIT_FOR_EXT_MASK_TO_APPEAR)
            waitForExtMask();

        else if(waitMillis==WAIT_FOR_EXT_MASK_TO_DISSAPEAR)
            waitForExtMaskToDisappear();
        else
            fail("No button found with text \"" + text + "\"");
    }

    public void clickButtonContainingText(String text)
    {
        clickButtonContainingText(text, defaultWaitForPage);
    }

    public void clickButtonContainingText(String text, int waitMills)
    {
        Locator.XPathLocator buttonLocator = getButtonLocatorContainingText(text);
        if (buttonLocator != null)
            clickAndWait(buttonLocator, waitMills);
        else
            fail("No button found with text \"" + text + "\"");
    }

    public void clickButtonContainingText(String buttonText, String textShouldAppearAfterLoading)
    {
        clickButtonContainingText(buttonText, 0);
        waitForText(textShouldAppearAfterLoading, defaultWaitForPage);
    }

    /**
     *  wait for button to appear, click it, wait for page to load
     */
    public void waitAndClickButton(final String text)
    {
        waitAndClickButton(text, defaultWaitForPage);
    }

    public void waitAndClickButton(final String text, final int wait)
    {
        String failMessage = "Button with text '" + text + "' did not appear";
        waitFor(new Checker()
        {
            public boolean check()
            {
                return null != getButtonLocator(text);
            }
        }, failMessage, WAIT_FOR_JAVASCRIPT);
        clickButton(text, wait);
    }


    /**
     *  wait for element, click it, return immediately
     */
    public void waitAndClick(Locator l)
    {
        waitAndClick(WAIT_FOR_JAVASCRIPT, l, 0);
    }

    /**
     *  wait for element, click it, wait for page to load
     */
    public void waitAndClickAndWait(Locator l)
    {
        waitAndClick(WAIT_FOR_JAVASCRIPT, l, WAIT_FOR_PAGE);
    }

    /**
     *  wait for element, click it, wait for page to load
     */
    public void waitAndClick(int waitFor, Locator l, int waitForPageToLoad)
    {
        waitForElement(l, waitFor);
        clickAndWait(l, waitForPageToLoad);
    }

    /** @return target of link */
    public String getLinkHref(String linkText, String controller, String folderPath)
    {
        Locator link = Locator.linkWithText(linkText);
        String localAddress = getButtonHref(link);
        // IE puts the entire link in href, not just the local address
        if (localAddress.contains("/"))
        {
            int location = localAddress.lastIndexOf("/");
            if (location < localAddress.length() - 1)
                localAddress = localAddress.substring(location + 1);
        }
        return (getContextPath() + "/" + controller + folderPath + "/" + localAddress);
    }


    public String getButtonHref(Locator buttonLoc)
    {
        String address = getAttribute(buttonLoc, "href");
        // IE puts the entire link in href, not just the local address
        if (address.contains("/"))
        {
            int location = address.lastIndexOf("/");
            if (location < address.length() - 1)
                address = address.substring(location + 1);
        }
        return address;
    }

    /**
     * @deprecated Use {@link #setFormElement(Locator, String)}
     * @param elementName
     * @param text
     */
    @Deprecated
    public void setText(String elementName, String text)
    {
        if (elementName.toLowerCase().contains("password"))
            log("Setting text of " + elementName + " to ******");
        else
            log("Setting text of " + elementName + " to " + text);

        selenium.typeSilent(elementName, text);
    }

    /**
     * @deprecated Use {@link #setFormElement(Locator, String)}
     * @param element
     * @param text
     */
    @Deprecated
    public void setFormElement(String element, String text)
    {
        if (isElementPresent(Locator.id(element)))
        {
            log("DEPRECATED: Form element locator '" + element + "' is an id; use Locator.id");
            setFormElement(Locator.id(element), text, false);
        }
        else
        {
            log("DEPRECATED: Form element locator '" + element + "' is a name; use Locator.name");
            setFormElement(Locator.name(element), text, false);
        }
    }

    public void setFormElement(Locator element, String text, boolean suppressValueLogging)
    {
        selenium.type(element.toString(), text, suppressValueLogging);
        fireEvent(element, SeleniumEvent.keyup);
        // Element might disappear after keyup
        if(isElementPresent(element))
            fireEvent(element, SeleniumEvent.blur);
    }

    /**
     * @deprecated Use {@link #setFormElement(Locator, java.io.File)}
     * @param element
     * @param file
     */
    @Deprecated
    public void setFormElement(String element, File file)
    {
        assertTrue("Test must be declared as file upload by overriding isFileUploadTest().", isFileUploadAvailable());
        setFormElement(element, file.getAbsolutePath());
    }

    public void setFormElement(Locator loc, File file)
    {
        assertTrue("Test must be declared as file upload by overriding isFileUploadTest().", isFileUploadAvailable());
        setFormElement(loc, file.getAbsolutePath());
    }

    public void setFormElement(Locator element, String text)
    {
        setFormElement(element, text, false);
    }

    public void setFormElements(String tagName, String formElementName, String[] values)
    {
        for (int i = 0; i < values.length; i++)
        {
            //  (//td[@id='bodypanel']//form[1]//.[@name='inputs'])[2] BROKEN?
            //  //td[@id='bodypanel']//form[1]//descendant::textarea[@name='inputs'][1]
            setFormElement(Locator.xpath("//descendant::" + tagName + "[@name='" + formElementName + "'][" + (i+1) + "]"), values[i]);
        }
    }

    public void setSort(String regionName, String columnName, SortDirection direction)
    {
        setSort(regionName, columnName, direction, defaultWaitForPage);
    }
    //clear sort from a column
    public void clearSort(String regionName, String columnName)
    {
        clearSort(regionName, columnName, defaultWaitForPage);
    }

    public void clearSort(String regionName, String columnName, int wait)
    {
        log("Clearing sort in " + regionName + " for " + columnName);
        Locator header = Locator.id(EscapeUtil.filter(regionName + ":" + columnName + ":header"));
        waitForElement(header, WAIT_FOR_JAVASCRIPT);
        String id = EscapeUtil.filter(regionName + ":" + columnName + ":clear");
        if (runMenuItemHandler(id))
            waitForPageToLoad(wait);
    }

    public void setSort(String regionName, String columnName, SortDirection direction, int wait)
    {
        log("Setting sort in " + regionName + " for " + columnName + " to " + direction.toString());
        Locator header = Locator.id(EscapeUtil.filter(regionName + ":" + columnName + ":header"));
        waitForElement(header, WAIT_FOR_JAVASCRIPT);
        String id = EscapeUtil.filter(regionName + ":" + columnName + ":" + direction.toString().toLowerCase());
        if (runMenuItemHandler(id))
            waitForPageToLoad(wait);
    }

    public void setFilter(String regionName, String columnName, String filterType)
    {
        setUpFilter(regionName, columnName, filterType, null);
        clickButton("OK");
    }

    public void setFilter(String regionName, String columnName, String filterType, String filter)
    {
        setFilter(regionName, columnName, filterType, filter, WAIT_FOR_PAGE);
    }

    public void setFilter(String regionName, String columnName, String filterType, String filter, int waitMillis)
    {
        setUpFilter(regionName, columnName, filterType, filter);
        clickButton("OK", waitMillis);
    }

    public void setUpFilter(String regionName, String columnName, String filterType, String filter)
    {
        setUpFilter(regionName, columnName, filterType, filter, null, null);
    }

    public void setFilterAndWait(String regionName, String columnName, String filterType, String filter, int milliSeconds)
    {
        setUpFilter(regionName, columnName, filterType, filter);
        clickButton("OK", milliSeconds);
    }

    public void setUpFilter(String regionName, String columnName, String filter1Type, String filter1, String filter2Type, String filter2)
    {
        String log =    "Setting filter in " + regionName + " for " + columnName+" to " + filter1Type.toLowerCase() + (filter1!=null?" " + filter1:"");
        if(filter2Type!=null)
        {
            log+= " and " + filter2Type.toLowerCase() + (filter2!=null?" " + filter2:"");
        }
        log( log );
        String id = EscapeUtil.filter(regionName + ":" + columnName + ":filter");
        Locator header = Locator.id(EscapeUtil.filter(regionName + ":" + columnName + ":header"));
        waitForElement(header, WAIT_FOR_JAVASCRIPT);
        String columnLabel = getText(header);
        runMenuItemHandler(id);
        _extHelper.waitForExtDialog("Show Rows Where " + columnLabel + "...");
        waitForTextToDisappear("Loading...");

        if (isTextPresent("Choose Values"))
        {
            log("Switching to advanced filter UI");
            _extHelper.clickExtTab("Choose Filters");
            waitForElement(Locator.xpath("//span["+Locator.NOT_HIDDEN+" and text()='Filter Type:']"), WAIT_FOR_JAVASCRIPT);
        }
        _extHelper.selectComboBoxItem("Filter Type:", filter1Type); //Select combo box item.
        if(filter1 != null) setFormElement("value_1", filter1);
        if(filter2Type!=null)
        {
            _extHelper.selectComboBoxItem("and:", filter2Type); //Select combo box item.
            if(filter2 != null) setFormElement("value_2", filter2);
        }
    }

    public void setFilter(String regionName, String columnName, String filter1Type, String filter1, String filter2Type, String filter2)
    {
        setUpFilter(regionName, columnName, filter1Type, filter1, filter2Type, filter2);
        clickButton("OK");
    }

    public void setUpFacetedFilter(String regionName, String columnName, String... values)
    {
        String log = "Setting filter in " + regionName + " for " + columnName+" to one of: [";
        for(String v : values)
        {
            log += v + ", ";
        }
        log = log.substring(0, log.length() - 2) + "]";
        log(log);
        String id = EscapeUtil.filter(regionName + ":" + columnName + ":filter");
        Locator header = Locator.id(EscapeUtil.filter(regionName + ":" + columnName + ":header"));
        waitForElement(header, WAIT_FOR_JAVASCRIPT);
        String columnLabel = getText(header);
        runMenuItemHandler(id);
        _extHelper.waitForExtDialog("Show Rows Where " + columnLabel + "...");

        sleep(500);

        // Clear selections.
        assertEquals("Faceted filter tab should be selected.", "Choose Values", getText(Locator.css(".x-tab-strip-active")));
        if(!isElementPresent(Locator.xpath("//div[contains(@class, 'x-grid3-hd-checker-on')]")))
            click(Locator.linkWithText("[All]"));
        click(Locator.linkWithText("[All]"));

        if(values.length > 1)
        {
            for(String v : values)
            {
                mouseDown(Locator.xpath(_extHelper.getExtDialogXPath("Show Rows Where "+columnLabel+"...")+
                    "//div[contains(@class,'x-grid3-row') and .//span[text()='"+v+"']]//div[@class='x-grid3-row-checker']"));
            }
        }
        else if (values.length == 1)
        {
            mouseDown(Locator.xpath(_extHelper.getExtDialogXPath("Show Rows Where "+columnLabel+"...")+
                    "//div[contains(@class,'x-grid3-row')]//span[text()='"+values[0]+"']"));
        }
    }

    public void setFacetedFilter(String regionName, String columnName, String... values)
    {
        setUpFacetedFilter(regionName, columnName, values);
        clickButton("OK");
    }

    public void clearFilter(String regionName, String columnName)
    {
        clearFilter(regionName, columnName, WAIT_FOR_PAGE);
    }

    public void clearFilter(String regionName, String columnName, int waitForPageLoad)
    {
        log("Clearing filter in " + regionName + " for " + columnName);
        Locator header = Locator.id(EscapeUtil.filter(regionName + ":" + columnName + ":header"));
        waitForElement(header, WAIT_FOR_JAVASCRIPT);
        String id = EscapeUtil.filter(regionName + ":" + columnName + ":clear-filter");
        runMenuItemHandler(id);
        if(waitForPageLoad > 0)
            waitForPageToLoad(waitForPageLoad);
    }

    /**
     * @param columnName only used to find something to click on, as all the filters on all the columns will be cleared
     */
    public void clearAllFilters(String regionName, String columnName)
    {
        log("Clearing filter in " + regionName + " for " + columnName);
        Locator header = Locator.id(EscapeUtil.filter(regionName + ":" + columnName + ":header"));
        waitForElement(header, WAIT_FOR_JAVASCRIPT);
        String id = EscapeUtil.filter(regionName + ":" + columnName + ":filter");
        runMenuItemHandler(id);
        clickButton("CLEAR ALL FILTERS");
    }

    public String getPropertyXPath(String propertyHeading)
    {
        return "//td[contains(text(), '" + propertyHeading + "')]/../..";
    }

    public int getXpathCount(Locator.XPathLocator xpath)
    {
        return selenium.getXpathCount(xpath.getPath()).intValue();
    }

    /**
     *
     * @param feature  the enable link will have an id of the form "labkey-experimental-feature-[feature]
     */
    public void enableExperimentalFeature(String feature)
    {
        log("Attempting to enable feature: " + feature);
        goToAdminConsole();
        clickAndWait(Locator.linkWithText("experimental features"));

        String xpath = "//div[div[text()='Create Specimen Study']]/a";
        if(!isElementPresent(Locator.xpath(xpath)))
            fail("No such feature found");
        else
        {
            Locator link = Locator.xpath(xpath + "[text()='Enable']");
            if(isElementPresent(link))
            {
                click(link);
                log("Enable link found, enabling");
            }
            else
            {
                log("Link not found, presumed enabled");
            }
        }
    }

    /**
     * From the assay design page, add a field with the given name, label, and type
     * @param name
     * @param label
     * @param type
     */
    public void addRunField(String name, String label, ListHelper.ListColumnType type)
    {
        String xpath = ("//input[starts-with(@name, 'ff_name");
        int newFieldIndex = getXpathCount(Locator.xpath(xpath + "')]"));
        clickButtonByIndex("Add Field", 1, 0);
        _listHelper.setColumnName(newFieldIndex, name);
        _listHelper.setColumnLabel(newFieldIndex, label);
        _listHelper.setColumnType(newFieldIndex, type);
    }

    public void addRunField(String name, String label, int index, ListHelper.ListColumnType type)
    {
//        String xpath = ("//input[starts-with(@name, 'ff_name");
//        int newFieldIndex = getXpathCount(Locator.xpath(xpath + "')]"));
        clickButtonByIndex("Add Field", 1, 0);
        _listHelper.setColumnName(index, name);
        _listHelper.setColumnLabel(index, label);
        _listHelper.setColumnType(index, type);
    }
    // UNDONE: move usages to use ListHelper
    @Deprecated
    public void addField(String areaTitle, int index, String name, String label, ListHelper.ListColumnType type)
    {
        String prefix = getPropertyXPath(areaTitle);
        String addField = prefix + "//span" + Locator.navButton("Add Field").getPath();
        selenium.click(addField);
        waitForElement(Locator.xpath(prefix + "//input[@name='ff_name" + index + "']"), WAIT_FOR_JAVASCRIPT);
        _listHelper.setColumnName(prefix, index, name);
        _listHelper.setColumnLabel(prefix, index, label);
        _listHelper.setColumnType(this, prefix, index, type);
    }

    // UNDONE: move usages to use ListHelper
    @Deprecated
    public void addLookupField(String areaTitle, int index, String name, String label, ListHelper.LookupInfo type)
    {
        String prefix = areaTitle==null ? "" : getPropertyXPath(areaTitle);
        String addField = prefix + "//span" + Locator.navButton("Add Field").getPath();
        selenium.click(addField);
        waitForElement(Locator.xpath(prefix + "//input[@name='ff_name" + index + "']"), WAIT_FOR_JAVASCRIPT);
        _listHelper.setColumnName(prefix, index, name);
        _listHelper.setColumnLabel(prefix, index, label);
        _listHelper.setColumnType(prefix, index, type);
    }

    // UNDONE: move usages to ListHelper
    @Deprecated
    public void deleteField(String areaTitle, int index)
    {
        String prefix = getPropertyXPath(areaTitle);
        selenium.mouseClick(prefix + "//div[@id='partdelete_" + index + "']");

        // If domain hasn't been saved yet, the 'OK' prompt will not appear.
        Locator.XPathLocator buttonLocator = getButtonLocator("OK");
        // TODO: Be smarter about this.  Might miss the OK that should be there.
        if (buttonLocator != null)
        {
            // Confirm the deletion
            clickButton("OK", 0);
            waitForElement(Locator.xpath("//td/img[@id='partstatus_" + index + "' and contains(@src, 'deleted')]"), WAIT_FOR_JAVASCRIPT);
        }
    }

    /**
     * @deprecated Use {@link #setFormElement(Locator, String)}
     * @param elementName
     * @param text
     */
    @Deprecated
    public void setLongTextField(final String elementName, final String text)
    {
        setFormElement(Locator.name(elementName), text, true);

        waitFor(new Checker()
        {
            public boolean check()
            {
                return getFormElement(elementName).replace("\r", "").trim().equals(text.replace("\r", "").trim()); // Ignore carriage-returns, which are present in IE but absent in firefox
            }
        }, elementName + " was not set.", WAIT_FOR_JAVASCRIPT);
    }

    /**
     * @deprecated Use {@link #setFormElement(Locator, String)}
     */
    @Deprecated public void setLongTextField(Locator loc, String text)
    {
        setLongTextField(loc.toString(), text);
    }

    public boolean isNavButtonPresent(String buttonText)
    {
        return isButtonPresent(buttonText);
    }

    public boolean isMenuButtonPresent(String buttonText)
    {
        return isButtonPresent(buttonText);
    }

    public void assertNavButtonPresent(String buttonText)
    {
        assertTrue("Nav button '" + buttonText + "' was not present", isNavButtonPresent(buttonText));
    }

    public void assertNavButtonNotPresent(String buttonText)
    {
        assertFalse("Nav button '" + buttonText + "' was present", isNavButtonPresent(buttonText));
    }

    public void assertMenuButtonPresent(String buttonText)
    {
        assertTrue("Nav button '" + buttonText + "' was not present", isMenuButtonPresent(buttonText));
    }

    public void assertMenuButtonNotPresent(String buttonText)
    {
        assertFalse("Menu button '" + buttonText + "' was present", isMenuButtonPresent(buttonText));
    }

    /**
     * Executes an Ext.menu.Item's handler.
     */
    public boolean runMenuItemHandler(String id)
    {
        log("Invoking Ext menu item handler '" + id + "'");
        //selenium.getEval("selenium.browserbot.getCurrentWindow().Ext.getCmp('" + id + "').handler();");
        String result = selenium.getEval("selenium.clickExtComponent(" + EscapeUtil.jsString(EscapeUtil.filter(id)) + ");");
        return result != null && Boolean.parseBoolean(result);
    }

    /**
     * Clicks the labkey menu item and optional submenu labels (for cascading menus)
     */
    public void clickMenuButton(String menusLabel, String ... subMenusLabels)
    {
        _extHelper.clickMenuButton(true, menusLabel, subMenusLabels);
    }

    /**
     * Clicks the ext menu item and optional submenu labels's (for cascading menus)
     * Does not wait for page load.
     */
    public void clickMenuButtonAndContinue(String menusLabel, String ... subMenusLabels)
    {
        _extHelper.clickMenuButton(false, menusLabel, subMenusLabels);
    }

    public void dataRegionPageFirst(String dataRegionName)
    {
        log("Clicking page first on data region '" + dataRegionName + "'");
        clickDataRegionPageLink(dataRegionName, "First Page");
    }

    public void dataRegionPageLast(String dataRegionName)
    {
        log("Clicking page last on data region '" + dataRegionName + "'");
        clickDataRegionPageLink(dataRegionName, "Last Page");
    }

    public void dataRegionPageNext(String dataRegionName)
    {
        log("Clicking page next on data region '" + dataRegionName + "'");
        clickDataRegionPageLink(dataRegionName, "Next Page");
    }

    public void dataRegionPagePrev(String dataRegionName)
    {
        log("Clicking page previous on data region '" + dataRegionName + "'");
        clickDataRegionPageLink(dataRegionName, "Previous Page");
    }

    private void clickDataRegionPageLink(String dataRegionName, String title)
    {
        String id = Locator.xq("dataregion_header_" + dataRegionName);
        clickAndWait(Locator.xpath("//table[@id=" + id + "]//div/a[@title='" + title + "']"));
    }

    public int getDataRegionRowCount(String dataRegionName)
    {
        String id = Locator.xq("dataregion_" + dataRegionName);
        return selenium.getXpathCount("//table[@id=" + id + "]/tbody/tr[contains(@class, 'labkey-row') or contains(@class, 'labkey-alternate-row')]").intValue();
    }

    /** Sets selection state for rows of the data region on the current page. */
    public void checkAllOnPage(String dataRegionName)
    {
        String id = Locator.xq("dataregion_" + dataRegionName);
        Locator checkAllCheckbox = Locator.xpath("//table[@id=" + id + "]//input[@name='.toggle']");
        checkCheckbox(checkAllCheckbox);
    }

    /** Clears selection state for rows of the data region on the current page. */
    public void uncheckAllOnPage(String dataRegionName)
    {
        String id = Locator.xq("dataregion_" + dataRegionName);
        Locator checkAllCheckbox = Locator.xpath("//table[@id=" + id + "]//input[@name='.toggle']");
        checkCheckbox(checkAllCheckbox);
        uncheckCheckbox(checkAllCheckbox);
    }

    /** Sets selection state for single rows of the data region. */
    public void checkDataRegionCheckbox(String dataRegionName, String value)
    {
        String id = Locator.xq(dataRegionName);
        checkCheckbox(Locator.xpath("//form[@id=" + id + "]//input[@name='.select' and @value='" + value + "']"));
    }

    /** Sets selection state for single rows of the data region. */
    public void checkDataRegionCheckbox(String dataRegionName, int index)
    {
        String id = Locator.xq("dataregion_" + dataRegionName);
        Locator select = Locator.xpath("//table[@id=" + id + "]//input[@name='.select']").index(index);
        checkCheckbox(select);
    }

    /** Sets selection state for single rows of the data region. */
    public void uncheckDataRegionCheckbox(String dataRegionName, int index)
    {
        String id = Locator.xq("dataregion_" + dataRegionName);
        Locator select = Locator.xpath("//table[@id=" + id + "]//input[@name='.select']").index(index);
        uncheckCheckbox(select);
    }

    public void toggleCheckboxByTitle(String title)
    {
        log("Clicking checkbox with title " + title);
        Locator l = Locator.checkboxByTitle(title);
        click(l);
    }

    /**
     * @deprecated Use {@link #click(Locator)}
     */
    @Deprecated public void clickCheckbox(String name)
    {
        click(Locator.checkboxByName(name));
    }

    /**
     * @deprecated Use {@link #click(Locator)}
     */
    @Deprecated public void clickRadioButtonById(String id)
    {
        click(Locator.radioButtonById(id));
    }

    /**
     * @deprecated Use {@link #clickAndWait(Locator, int)}
     */
    @Deprecated public void clickRadioButtonById(String id, int millis)
    {
        clickAndWait(Locator.radioButtonById(id), millis);

    }

    /**
     * @deprecated Use {@link #click(Locator)}
     */
    @Deprecated public void clickCheckboxById(String id)
    {
        click(Locator.checkboxById(id));
    }

    /**
     * @deprecated Use {@link #checkCheckbox(Locator)}
     */
    @Deprecated public void checkRadioButton(String name, String value)
    {
        checkCheckbox(Locator.radioButtonByNameAndValue(name, value));
    }

    /**
     * @deprecated Use {@link #checkCheckbox(Locator)}
     */
    @Deprecated public void checkCheckbox(String name, String value)
    {
        checkCheckbox(Locator.checkboxByNameAndValue(name, value));
    }

    /**
     * @deprecated Use {@link #checkCheckbox(Locator)}
     */
    @Deprecated public void checkCheckbox(String name)
    {
        checkCheckbox(Locator.checkboxByName(name));
    }

    public void checkCheckboxByNameInDataRegion(String name)
    {
        checkCheckbox(Locator.xpath("//a[contains(text(), '" + name + "')]/../..//td/input"));
    }

    public void checkButtonByText(String text)
    {
        Locator l = Locator.xpath("//*[text()='" + text + "']/../input[contains(@type,'button')]");
        click(l);
    }

    public void checkRadioButton(Locator radioButtonLocator)
    {
        checkCheckbox(radioButtonLocator);
    }

    public void checkCheckbox(Locator checkBoxLocator)
    {
        log("Checking checkbox " + checkBoxLocator);
       //NOTE: We don't use selenium.check() because it doesn't fire click events.
        if (!isChecked(checkBoxLocator))
            click(checkBoxLocator);
        logJavascriptAlerts();
        assertTrue("Checking checkbox failed", isChecked(checkBoxLocator));
    }

    /**
     * @deprecated Use {@link #checkRadioButton(Locator)}
     */
    @Deprecated public void checkRadioButton(String name, int index)
    {
        checkCheckbox(Locator.radioButtonByName(name).index(index));
    }

    /**
     * @deprecated Use {@link #assertRadioButtonSelected(Locator)}
     */
    @Deprecated public void assertRadioButtonSelected(String name, String value)
    {
        assertRadioButtonSelected(Locator.radioButtonByNameAndValue(name, value));
    }

    public void assertRadioButtonSelected(Locator radioButtonLocator)
    {
        assertTrue("Radio Button is not selected at " + radioButtonLocator.toString(), isChecked(radioButtonLocator));
    }

    /**
     * @deprecated Use {@link #checkCheckbox(Locator)}
     */
    @Deprecated public void checkCheckbox(String name, int index)
    {
        checkCheckbox(Locator.checkboxByName(name).index(index));
    }

    /**
     * @deprecated Use {@link #uncheckCheckbox(Locator)}
     */
    @Deprecated public void uncheckCheckbox(String name)
    {
        uncheckCheckbox(Locator.checkboxByName(name));
    }

    /**
     * @deprecated Use {@link #uncheckCheckbox(Locator)}
     */
    @Deprecated public void uncheckCheckbox(String name, String value)
    {
        uncheckCheckbox(Locator.checkboxByNameAndValue(name, value));
    }

    /**
     * @deprecated Use {@link #uncheckCheckbox(Locator)}
     */
    @Deprecated public void uncheckCheckbox(String name, int index)
    {
        uncheckCheckbox(Locator.checkboxByName(name).index(index));
    }

    public void uncheckCheckbox(Locator checkBoxLocator)
    {
        log("Unchecking checkbox " + checkBoxLocator);
        //NOTE: We don't use selenium.uncheck() because it doesn't fire click events.
        if (isChecked(checkBoxLocator))
            click(checkBoxLocator);
        logJavascriptAlerts();
    }

    public void assertChecked(Locator checkBoxLocator)
    {
        assertTrue("Checkbox not checked at " + checkBoxLocator.toString(), isChecked(checkBoxLocator));
    }

    public void assertNotChecked(Locator checkBoxLocator)
    {
        assertFalse("Checkbox checked at " + checkBoxLocator.toString(), isChecked(checkBoxLocator));
    }

    public boolean isChecked(Locator checkBoxLocator)
    {
        return selenium.isChecked(checkBoxLocator.toString());
    }

    /**
     * @deprecated Use {@link #selectOptionByValue(Locator, String)}
     * @param selectId
     * @param value
     */
    @Deprecated
    public void selectOptionByValue(String selectId, String value)
    {
        selenium.select(selectId, "value=" + value);
    }

    public void selectOptionByValue(Locator loc, String value)
    {
        selectOptionByValue(loc.toString(), value);
    }

    /**
     * @deprecated Use {@link #selectOptionByText(Locator, String)}
     * @param selectId
     * @param text
     */
    @Deprecated
    public void selectOptionByText(String selectId, String text)
    {
        selenium.select(selectId, text);
    }

    public void selectOptionByText(Locator locator, String text)
    {
        selenium.select(locator.toString(), text);
    }

    public void addUrlParameter(String parameter)
    {
        if (!getCurrentRelativeURL().contains(parameter))
            if (getCurrentRelativeURL().contains("?"))
                beginAt(getCurrentRelativeURL().concat("&" + parameter));
            else
                beginAt(getCurrentRelativeURL().concat("?" + parameter));
    }

    String toRole(String perm)
    {
        String R = "security.roles.";
        if ("No Permissions".equals(perm))
            return R + "NoPermissionsRole";
        if ("Project Administrator".equals(perm))
            return R + "ProjectAdminRole";
        else if (!perm.contains("."))
            return R + perm + "Role";
        return perm;
    }

    public void assertNoPermission(String groupName, String permissionSetting)
    {
        waitForElement(Locator.permissionRendered(), WAIT_FOR_JAVASCRIPT);
        waitForElementToDisappear(Locator.permissionButton(groupName,permissionSetting), WAIT_FOR_JAVASCRIPT);
    }

    public void assertPermissionSetting(String groupName, String permissionSetting)
    {
        String role = toRole(permissionSetting);
        if ("security.roles.NoPermissionsRole".equals(role))
        {
            assertNoPermission(groupName,"Reader");
            assertNoPermission(groupName,"Editor");
            assertNoPermission(groupName,"Project Administrator");
            return;
        }
        log("Checking permission setting for group " + groupName + " equals " + role);
        waitForElement(Locator.permissionRendered(), WAIT_FOR_JAVASCRIPT);
        assertElementPresent(Locator.permissionButton(groupName,permissionSetting));
    }


    public void checkInheritedPermissions()
    {
        _ext4Helper.checkCheckbox("Inherit permissions from parent");
    }


    public void uncheckInheritedPermissions()
    {
        _ext4Helper.uncheckCheckbox("Inherit permissions from parent");
    }

    public void savePermissions()
    {
        clickButton("Save", 0);
    }

    @LogMethod
    public void setPermissions(String groupName, String permissionString)
    {
        _setPermissions(groupName, permissionString, "pGroup");
    }

    @LogMethod
    public void setSiteGroupPermissions(String groupName, String permissionString)
    {
        _setPermissions(groupName, permissionString, "pSite");
    }

    @LogMethod
    public void setUserPermissions(String userName, String permissionString)
    {
        log(new Date().toString());
        _setPermissions(userName, permissionString, "pUser");

        log(new Date().toString());
    }

    private void _setPermissions(String userOrGroupName, String permissionString, String className)
    {
        String role = toRole(permissionString);
        if ("org.labkey.api.security.roles.NoPermissionsRole".equals(role))
        {
            fail("call removePermission()");
        }
        else
        {
            log("Setting permissions for group " + userOrGroupName + " to " + role);

            if (!isElementPresent(Locator.permissionRendered()))
                enterPermissionsUI();
            _ext4Helper.clickTabContainingText("Permissions");

            waitForElement(Locator.permissionRendered(), WAIT_FOR_JAVASCRIPT);
            String group = userOrGroupName;
            if (className.equals("pSite"))
                group = "Site: " + group;
            click(Locator.xpath("//div[contains(@class, 'rolepanel')][.//h3[text()='" + permissionString + "']]//div[contains(@class, 'x4-form-trigger')]"));
            click(Locator.xpath("//div[contains(@class, 'x4-boundlist')]//li[contains(@class, '" + className + "') and starts-with(text(), '" + group + "')]"));
            waitForElement(Locator.permissionButton(userOrGroupName, permissionString));
            savePermissions();
            assertPermissionSetting(userOrGroupName, permissionString);
        }
    }


    public void removeSiteGroupPermission(String groupName, String permissionString)
    {
        _removePermission(groupName, permissionString, "pSite");
    }

    public void removePermission(String groupName, String permissionString)
    {
        _removePermission(groupName, permissionString, "pGroup");
    }


    public void _removePermission(String groupName, String permissionString, String className)
    {
        Locator close = Locator.closePermissionButton(groupName, permissionString);
        if (isElementPresent(close))
        {
            click(close);
            savePermissions();
            assertNoPermission(groupName, permissionString);
        }
    }

    protected void addUserToSiteGroup(String userName, String groupName)
    {
        goToHome();
        goToSiteGroups();
        Locator.XPathLocator groupLoc = Locator.tagWithText("div", groupName);
        waitForElement(groupLoc, defaultWaitForPage);
        mouseDown(groupLoc);
        clickAndWait(Locator.linkContainingText("manage group"));
        addUserToGroupFromGroupScreen(userName);
    }

    protected void addUserToGroupFromGroupScreen(String userName)
    {
        waitForElement(Locator.name("names"));
        setFormElement("names", userName);
        uncheckCheckbox("sendEmail");
        clickButton("Update Group Membership");
    }

    /**
     * Adds a new or existing user to an existing group within an existing project
     *
     * @param userName new or existing user name
     * @param projectName existing project name
     * @param groupName existing group within the project to which we should add the user
     */
    protected void addUserToProjGroup(String userName, String projectName, String groupName)
    {
        if (isElementPresent(Locator.permissionRendered()))
        {
            exitPermissionsUI();
            clickProject(projectName);
        }
        enterPermissionsUI();
        clickManageGroup(groupName);
        addUserToGroupFromGroupScreen(userName);
    } //addUserToProjGroup()

    public void enterPermissionsUI()
    {
        //if the following assert triggers, you were already in the permissions UI when this was called
        if (!isElementPresent(Locator.permissionRendered()))
        {
            clickAdminMenuItem("Folder", "Permissions");
            waitForElement(Locator.permissionRendered());
        }
    }

    public void exitPermissionsUI()
    {
        _ext4Helper.clickTabContainingText("Permissions");
        clickButton("Save and Finish");
    }

    public void impersonateGroup(String group, boolean isSiteGroup)
    {
        goToHome();
        clickUserMenuItem("Impersonate", "Group", (isSiteGroup ? "Site: " : "") + group);
    }

    public void impersonateRole(String role)
    {
        clickUserMenuItem("Impersonate", "Role", role);
    }

    public void stopImpersonatingRole()
    {
        clickUserMenuItem("Stop Impersonating");
        assertSignOutAndMyAccountPresent();
        goToHome();
    }

    public void stopImpersonatingGroup()
    {
        clickUserMenuItem("Stop Impersonating");
        assertSignOutAndMyAccountPresent();
        goToHome();
    }


    public void impersonate(String fakeUser)
    {
        _ext4Helper.clickExt4MenuButton(false, Locators.USER_MENU, false, "Impersonate", "User");
        waitForElement(Ext4HelperWD.Locators.window("Impersonate User"));
        _ext4Helper.selectComboBoxItem(Ext4HelperWD.Locators.formItemWithLabel("User:"), fakeUser + " (", true);
        clickAndWait(Ext4HelperWD.ext4WindowButton("Impersonate User", "Impersonate"));
        _impersonationStack.push(fakeUser);

        if (isElementPresent(Locator.navButton("Home")))
        {
            clickAndWait(Locator.navButton("Home"));
        }
    }


    public void stopImpersonating()
    {
        String fakeUser = _impersonationStack.pop();
        assertEquals(displayNameFromEmail(fakeUser), getDisplayName());
        clickUserMenuItem("Stop Impersonating");
        assertSignOutAndMyAccountPresent();
        goToHome();
        assertFalse(displayNameFromEmail(fakeUser).equals(getDisplayName()));
    }

    // assumes there are not collisions in the database causing unique numbers to be appended
    protected String displayNameFromEmail(String email)
    {
        String display = email.contains("@") ? email.substring(0,email.indexOf('@')) : email;
        display = display.replace('_', ' ');
        display = display.replace('.', ' ');
        return display.trim();
    }


    /** create a user with the specified permissions for the specified project
     *
     * @param userName
     * @param projectName
     * @param permissions
     */
    public void createUserWithPermissions(String userName, @Nullable String projectName, String permissions)
    {
        createUser(userName, null);

        if (projectName == null)
            goToProjectHome();
        else
            clickProject(projectName);

        setUserPermissions(userName, permissions);
    }

    public void createUser(String userName, String cloneUserName)
    {
        createUser(userName, cloneUserName, true);
    }

    public void createUser(String userName, @Nullable String cloneUserName, boolean verifySuccess)
    {
        if (cloneUserName == null)
        {
            _userHelper.createUser(userName, verifySuccess);
        }
        else
        {
            fail("cloneUserName support has been removed"); //not in use, so was not implemented in new user
            //helpers
        }
    }

    public void createUserAndNotify(String userName, String cloneUserName)
    {
        createUserAndNotify(userName, cloneUserName, true);
    }

    public void createUserAndNotify(String userName, String cloneUserName, boolean verifySuccess)
    {
        ensureAdminMode();
        goToSiteUsers();
        clickButton("Add Users");

        setFormElement("newUsers", userName);
        if (cloneUserName != null)
        {
            checkCheckbox("cloneUserCheck");
            setFormElement("cloneUser", cloneUserName);
        }
        clickButton("Add Users");

        if (verifySuccess)
            assertTrue("Failed to add user " + userName, isTextPresent(userName + " added as a new user to the system"));
    }

    public void createSiteDeveloper(String userEmail)
    {
        ensureAdminMode();
        goToSiteDevelopers();

        if (!isElementPresent(Locator.xpath("//input[@value='" + userEmail + "']")))
        {
            addUserToGroupFromGroupScreen(userEmail);
        }
    }

    public void deleteUser(String userEmail)
    {
        deleteUsers(true, userEmail);
    }

    public void deleteGroup(String groupName)
    {
        deleteGroup(groupName, false);
    }

    @LogMethod
    public void deleteGroup(String groupName, boolean failIfNotFound)
    {
        log("Attempting to delete group: " + groupName);
        if (selectGroup(groupName, failIfNotFound))
        {
            deleteAllUsersFromGroup();

            click(Locator.xpath("//td/a/span[text()='Delete Empty Group']"));
            waitForElementToDisappear(Locator.xpath("//div").withClass("groupPicker").append("//div").withClass("x4-grid-cell-first").withText(groupName), WAIT_FOR_JAVASCRIPT);
        }
    }

    private void deleteAllUsersFromGroup()
    {
        Locator.XPathLocator l = Locator.xpath("//td/a/span[text()='remove']");

        while(isElementPresent(l))
        {
            int i = getXpathCount(l) - 1;
            click(l);
            waitForElementToDisappear(l.index(i), WAIT_FOR_JAVASCRIPT);
        }
    }


    public void removeUserFromGroup(String groupName, String userName)
    {
         if(!isTextPresent("Group " + groupName))
             selectGroup(groupName);

        Locator l = Locator.xpath("//td[text()='" + userName +  "']/..//td/a/span[text()='remove']");
        click(l);
    }

    public void addUserToGroup(String groupName, String userName)
    {
         if(!isTextPresent("Group " + groupName))
             selectGroup(groupName);

        _ext4Helper.selectComboBoxItem(Locator.xpath("//table[contains(@id, 'labkey-principalcombo')]"), userName);
        waitForElement(Locator.css(".userinfo td:contains("+userName+")"));
        _extHelper.clickExtButton(groupName + " Information", "Done", 0);
        _extHelper.waitForExtDialogToDisappear(groupName + " Information");
        clickButton("Done");
    }

    public boolean selectGroup(String groupName)
    {
        return selectGroup(groupName, false);
    }

    public boolean selectGroup(String groupName, boolean failIfNotFound)
    {
        if(!isElementPresent(Locator.xpath("//li[contains(@class,'tab-strip-active')]//span[text()='Site Groups']")))
            goToSiteGroups();

        waitForElement(Locator.css(".groupPicker .x4-grid-body"), WAIT_FOR_JAVASCRIPT);
        if (isElementPresent(Locator.xpath("//div[text()='" + groupName + "']")))
        {
            waitForElement(Locator.xpath("//div[text()='" + groupName + "']/../.."), 1000);
            mouseDown(Locator.xpath("//div[text()='" + groupName + "']/../.."));
            _extHelper.waitForExtDialog(groupName + " Information");
            return true;
        }
        else if (failIfNotFound)
            fail("Group not found:" + groupName);

        return false;
    }

    @LogMethod
    public void deleteUsers(boolean failIfNotFound, String... userEmails)
    {
        int checked = 0;
        List<String> displayNames = new ArrayList<>();
        ensureAdminMode();
        goToSiteUsers();

        if(isLinkPresentWithText("include inactive users"))
            clickAndWait(Locator.linkWithText("include inactive users"));

        DataRegionTable usersTable = new DataRegionTable("Users", this, true, true);

        for(String userEmail : userEmails)
        {
            String userXPath = "//a[text()='details']/../../td[text()='" + userEmail + "']";
            int row = usersTable.getRow("Email", userEmail);

            boolean isPresent = row != -1;

            // If we didn't find the user and we have more than one page, then show all pages and try again
            if (!isPresent && isLinkPresentContainingText("Next") && isLinkPresentContainingText("Last"))
            {
                clickButton("Page Size", 0);
                clickAndWait(Locator.linkWithText("Show All"));
                row = usersTable.getRow("Email", userEmail);
                isPresent = row != -1;
            }

            if (failIfNotFound)
                assertTrue(userEmail + " was not present", isPresent);

            if (isPresent)
            {
                usersTable.checkCheckbox(row);
                checked++;
                displayNames.add(usersTable.getDataAsText(row, "Display Name"));
            }
        }

        if(checked > 0)
        {
            clickButton("Delete");
            assertTextPresent(displayNames);
            assertTextPresent("permanently delete");
            clickButton("Permanently Delete");
            assertTextNotPresent(userEmails);
        }
    }

    public void assertUserExists(String email)
    {
        log("asserting that user " + email + " exists...");
        ensureAdminMode();
        goToSiteUsers();
        assertTextPresent(email);
        log("user " + email + " exists.");
    }

    public boolean doesGroupExist(String groupName, String projectName)
    {
        ensureAdminMode();
        clickProject(projectName);
        enterPermissionsUI();
        _ext4Helper.clickTabContainingText("Project Groups");
        waitForText("Member Groups");
        List<Ext4CmpRef> refs = _ext4Helper.componentQuery("grid", Ext4CmpRef.class);
        Ext4CmpRef ref = refs.get(0);
        int idx = Integer.parseInt(ref.eval("this.getStore().find(\"name\", \"" + groupName + "\")"));
        exitPermissionsUI();
        return (idx >= 0);
    }

    public void assertGroupExists(String groupName, String projectName)
    {
        log("asserting that group " + groupName + " exists in project " + projectName + "...");
        if (!doesGroupExist(groupName, projectName))
            fail("group " + groupName + " does not exist in project " + projectName);
    }

    public void assertGroupDoesNotExist(String groupName, String projectName)
    {
        log("asserting that group " + groupName + " exists in project " + projectName + "...");
        if (doesGroupExist(groupName, projectName))
            fail("group " + groupName + " exists in project " + projectName);
    }

    public boolean isUserInGroup(String email, String groupName, String projectName)
    {
        if (doesGroupExist(groupName, projectName))
        {
            enterPermissionsUI();
            openGroupPermissionsDisplay(groupName);
            boolean ret = isElementPresent(Locator.xpath("//table[contains(@class,'userinfo')]//td[starts-with(text(), '" + email + "')]"));
//            click(Locator.xpath("//div[contains(@class, 'x4-window')]//button[./span[text()='Done']]"));
            click(Locator.ext4Button("Done"));
            exitPermissionsUI();
            return ret;
        }
        return false;
    }

    public void assertUserInGroup(String email, String groupName, String projectName)
    {
        log("asserting that user " + email + " is in group " + projectName + "/" + groupName + "...");
        if (!isUserInGroup(email, groupName, projectName))
            fail("user " + email + " was not in group " + projectName + "/" + groupName);
    }

    public void assertUserNotInGroup(String email, String groupName, String projectName)
    {
        log("asserting that user " + email + " is not in group " + projectName + "/" + groupName + "...");
        if (isUserInGroup(email, groupName, projectName))
            fail("user " + email + " was found in group " + projectName + "/" + groupName);
    }

    /**
     * Saves a wiki page that is currently being created or edited. Because
     * the wiki edit page now uses AJAX to save the page, use this function to
     * reliably save the page and wait for the browser to redirect to where it would
     * normally go next.
     */
    public void saveWikiPage()
    {
        String curUrl = selenium.getLocation();

        //get the redir parameter
        String redirUrl = getUrlParam(curUrl, "redirect", true);
        if(null == redirUrl || redirUrl.length() == 0)
        {
            String pageName = getUrlParam(curUrl, "name", true);
            if(null == pageName)
                pageName = selenium.getValue("wiki-input-name");
            assert null != pageName && pageName.length() > 0;
            int idxStart = curUrl.indexOf("/wiki/");
            int idxEnd = curUrl.indexOf("/editWiki.view?", idxStart);
            redirUrl = getContextPath() + curUrl.substring(idxStart, idxEnd) + "/page.view?name=" + pageName;
        }

        log("Saving wiki...");
        clickButton("Save", 0);
        log("Waiting for AJAX save return...");
        //waitForText("Saved.", 10000);
        waitFor(new WikiSaveChecker(), "Wiki page failed to save!", 10000);
        //sleep(100);
        log("Navigating to " + redirUrl);
        beginAt(redirUrl);
    }

    public String getUrlParam(String url, String paramName, boolean decode)
    {
        String paramStart = paramName + "=";
        int idxStart = url.indexOf(paramStart);
        if(idxStart > 0)
        {
            idxStart += paramStart.length();
            int idxEnd = url.indexOf("&", idxStart);
            if(idxEnd < 0)
                idxEnd = url.length();
            String ret = url.substring(idxStart, idxEnd);
            if(decode)
            {
                ret = ret.replace("+", "%20");
                try {ret = URLDecoder.decode(ret, "UTF-8");} catch(UnsupportedEncodingException ignored) {}
            }
            return ret.trim();
        }
        else
            return null;
    }

    public class WikiSaveChecker implements Checker
    {
        private Locator _locator = Locator.id("status");
        public boolean check()
        {
            return "Saved.".equals(getText(_locator));
        }
    }

    private long start = 0;

    protected void startTimer()
    {
        start = System.currentTimeMillis();
    }

    protected long elapsedSeconds()
    {
        return (System.currentTimeMillis() - start) / 1000;
    }

    /**
     * Creates a new wiki page, assuming that the [new page] link is available
     * somewhere on the current page. This link is typically displayed above
     * the Wiki table of contents, which is shown on collaboration portal pages,
     * the wiki module home page, as well as any wiki page.
     * @param format The format for the new page. Allowed values are "RADEOX" (for wiki),
     * "HTML", and "TEXT_WITH_LINKS". Note that these are the string names for the
     * WikiRendererType enum values.
     */
    public void createNewWikiPage(String format)
    {
        if(isLinkPresentWithText("new page"))
            clickAndWait(Locator.linkWithText("new page"));
        else if(isLinkPresentWithText("Create a new wiki page"))
            clickAndWait(Locator.linkWithText("Create a new wiki page"));
        else if(isLinkPresentWithText("add content"))
            clickAndWait(Locator.linkWithText("add content"));
        else if(isTextPresent("Pages"))
            clickWebpartMenuItem("Pages", "New");
        else
            fail("Could not find a link on the current page to create a new wiki page." +
                    " Ensure that you navigate to the wiki controller home page or an existing wiki page" +
                    " before calling this method.");

        convertWikiFormat(format);
    }

    //must already be on wiki page
    public void setWikiValuesAndSave(String name, String title, String body)
    {

        setFormElement("name", name);
        setFormElement("title", title);
        setWikiBody(body);
        clickButtonContainingText("Save & Close");
    }

    /**
     * Converts the current wiki page being edited to the specified format.
     * If the page is already in that format, it will no-op.
     * @param format The desired format ("RADEOX", "HTML", or "TEXT_WITH_LINKS")
     */
    public void convertWikiFormat(String format)
    {
        String curFormat = selenium.getEval("this.browserbot.getCurrentWindow()._wikiProps.rendererType");
        if(curFormat.equalsIgnoreCase(format))
            return;


        clickButton("Convert To...", 0);
        sleep(500);
        selectOptionByValue("wiki-input-window-change-format-to", format);
        clickButton("Convert", 0);
        sleep(500);
    }

    /**
     * Creates a new wiki page using HTML as the format. See {@link #createNewWikiPage(String)}
     * for more details.
     */
    public void createNewWikiPage()
    {
        createNewWikiPage("HTML");
    }

    /**
     * Sets the wiki page body, automatically switching to source view if necessary
     * @param body The body text to set
     */
    public void setWikiBody(String body)
    {
        switchWikiToSourceView();
        setLongTextField("body", body);
    }

    /**
     * Used by CohortTest and StudyCohortExportTest
     * Returns the data region for the the cohort table to enable setting
     * or verifying the enrolled status of the cohort
     */
    public DataRegionTable getCohortDataRegionTable()
    {
        clickTab("Manage");
        clickAndWait(Locator.linkWithText("Manage Cohorts"));
        return new DataRegionTable("Cohort", this, false);
    }

    /**
     * Used by CohortTest and StudyCohortExportTest
     * Verifies the enrolled status of a cohort
     */
    public void verifyCohortStatus(DataRegionTable table, String cohort, boolean  enrolled)
    {
        int row = getCohortRow(table, cohort);
        String s = table.getDataAsText(row, "Enrolled");
        assertTrue("Enrolled column should be " + String.valueOf(enrolled), (0 == s.compareToIgnoreCase(String.valueOf(enrolled))));
    }

    /**
     * Used by CohortTest and StudyCohortExportTest
     * Retrieves the row for the cohort matching the label passed in
     */
    public int getCohortRow(DataRegionTable cohortTable, String cohort)
    {
        int row;
        for (row = 0; row < cohortTable.getDataRowCount(); row++)
        {
            String s = cohortTable.getDataAsText(row, "Label");
            if (0 == s.compareToIgnoreCase(cohort))
            {
                break;
            }
        }
        return row;
    }

    /**
     * Used by CohortTest and StudyCohortExportTest
     * Changes the enrolled status of the passed in cohort name
     */
    public void changeCohortStatus(DataRegionTable cohortTable, String cohort, boolean enroll)
    {
        int row = getCohortRow(cohortTable, cohort);
        // if the row does not exist then most likely the cohort passed in is incorrect
        cohortTable.clickLink(row, 0);

        if (!enroll)
        {
            uncheckCheckbox("quf_enrolled");
        }
        else
        {
            checkCheckbox("quf_enrolled");
        }

        clickButton("Submit");
    }

    /**
     * Given a file name sets the wikiName page contents to a file in server/test/data/api
     * @param fileName file will be found in server/test/data/api
     * @param wikiName Name of the wiki where the source should be placed
     * @return The source found in the file.
     */
    public String setSourceFromFile(String fileName, String wikiName)
    {
        return setSource(getFileContents("server/test/data/api/" + fileName), wikiName);
    }

    private String setSource(String srcFragment, String wikiName)
    {
        if (!isTextPresent(wikiName))
        {
            fail("Could not find the Wiki '" + wikiName + "'. Please create the Wiki before attempting to set the source.");
        }
        clickWebpartMenuItem(wikiName, "Edit");

        setWikiBody(srcFragment);
        saveWikiPage();
        return srcFragment;
    }

    /**
     * Switches the wiki edit page to source view when the format type is HTML.
     */
    public void switchWikiToSourceView()
    {
        Locator sourceTab = Locator.xpath("//li[@id='wiki-tab-source']/a");
        if(null != sourceTab)
            click(sourceTab);
    }

    public void enableModule(String projectName, String moduleName)
    {
        ensureAdminMode();
        clickProject(projectName);
        enableModule(moduleName, true);
    }

    public void enableModule(String moduleName, boolean isProject)
    {
        enableModules(Collections.singletonList(moduleName), isProject);
    }

    public void enableModules(List<String> moduleNames, boolean isProject)
    {
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Folder Type"));
        for (String moduleName : moduleNames)
        {
            checkCheckbox(Locator.checkboxByTitle(moduleName));
        }
        clickButton("Update Folder");
    }

    public void disableModules(String... moduleNames)
    {
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Folder Type"));
        for (String moduleName : moduleNames)
        {
            uncheckCheckbox(Locator.checkboxByTitle(moduleName));
        }
        clickButton("Update Folder");
    }

    public void goToProjectHome()
    {
        clickProject(getProjectName());
    }

    public void goToHome()
    {
        beginAt("/project/home/begin.view");
    }

    /**
     * go to the project settings page of a project, or of the current project if argument=null
     * @param project project name, or null if current project
     */
    public void goToProjectSettings(String project)
    {
        if(!isElementPresent(Locator.id("projectBar")))
            goToHome();
        clickProject(project);
        goToProjectSettings();
    }

    public void goToAdmin()
    {
        beginAt("/admin/showAdmin.view");
    }

    public void goToMyAccount()
    {
        clickUserMenuItem("My Account");
    }

    protected void startImportStudyFromZip(File studyFile)
    {
        clickButton("Import Study");
        setFormElement(Locator.name("folderZip"), studyFile);
        clickButton("Import Study From Local Zip Archive");
        assertTextNotPresent("You must select a .study.zip file to import.");
    }

    protected void importStudyFromZip(File studyFile)
    {
        startImportStudyFromZip(studyFile);
        waitForPipelineJobsToComplete(1, "Study import", false);
    }


    protected void importFolderFromZip(File folderFile)
    {
        importFolderFromZip(folderFile, true, 1);
    }

    protected void importFolderFromZip(File folderFile, boolean validateQueries, int completedJobs)
    {
        importFolderFromZip(folderFile, validateQueries, completedJobs, false);
    }

    protected void importFolderFromZip(File folderFile, boolean validateQueries, int completedJobs, boolean expectError)
    {
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Import"));
        waitForElement(Locator.name("folderZip"));
        setFormElement(Locator.name("folderZip"), folderFile);
        if (!validateQueries)
            uncheckCheckbox(Locator.name("validateQueries"));
        clickButtonContainingText("Import Folder From Local Zip Archive");
        waitForText("Data Pipeline");
        waitForPipelineJobsToComplete(completedJobs, "Folder import", expectError);
    }

    protected void importFolderFromPipeline(String folderFile)
    {
        importFolderFromPipeline(folderFile, 1);
    }

    protected void importFolderFromPipeline(String folderFile, int completedJobsExpected)
    {
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Import"));
        clickButtonContainingText("Import Folder Using Pipeline");
        _fileBrowserHelper.importFile(folderFile, "Import Folder");
        waitForPipelineJobsToComplete(completedJobsExpected, "Folder import", false);
    }
    public String getFileContents(String rootRelativePath)
    {
        if (rootRelativePath.charAt(0) != '/')
            rootRelativePath = "/" + rootRelativePath;
        File file = new File(getLabKeyRoot() + rootRelativePath);
        return getFileContents(file);
    }

    public String getFileContents(File file)
    {
        try
        {
            return new String(Files.readAllBytes(Paths.get(file.toURI())));
        }
        catch (IOException fail)
        {
            fail(fail.getMessage());
            return null;
        }
    }

    public void signOut()
    {
        log("Signing out");
        beginAt("/login/logout.view");
        waitForElement(Locator.xpath("//a").withText("Sign\u00a0In")); // Will recognize link [BeginAction] or button [LoginAction]("Sign In");
    }

    /*
     * This assumes that you have added the "search" webpart to your project
     */
    public void searchFor(String projectName, String searchFor, int expectedResults, String titleName)
    {
        log("Searching Project : " + projectName + " for \"" + searchFor + "\".  Expecting to find : " + expectedResults + " results");
        clickProject(projectName);
        assertElementPresent(Locator.name("q"));
        setFormElement("query", searchFor);
        clickButton("Search");
        long wait = 0;
        while (wait < 5*defaultWaitForPage)
        {
            if ((titleName == null && isTextPresent("Found " + expectedResults + " result")) ||
                (titleName != null && isLinkPresentContainingText(titleName)))
                break;
            sleep(500);
            wait += 500;
            refresh();
        }
        if (titleName == null)
        {
            assertTextPresent("Found " + expectedResults + " result");
            log("found \"" + expectedResults + "\" result of " + searchFor);
        }
        else
        {
            clickAndWait(Locator.linkContainingText(titleName));
            assertTextPresent(searchFor);
        }
    }

    public void searchFor(String projectName, String searchFor, int expectedResults)
    {
        searchFor(projectName, searchFor, expectedResults, null);
    }


    public void assertAttributeEquals(Locator locator, String attributeName, String value)
    {
        String actual = getAttribute(locator, attributeName);
        assertEquals("Expected attribute '" + locator + "@" + attributeName + "' value to be '" + value + "', but was '" + actual + "' instead.", value, actual);
    }

    public void assertAttributeContains(Locator locator, String attributeName, String value)
    {
        String actual = getAttribute(locator, attributeName);
        assertTrue("Expected attribute '" + locator + "@" + attributeName + "' value to contain '" + value + "', but was '" + actual + "' instead.", actual != null && actual.contains(value));
    }

    public void assertAttributeNotContains(Locator locator, String attributeName, String value)
    {
        String actual = getAttribute(locator, attributeName);
        assertTrue("Expected attribute '" + locator + "@" + attributeName + "' value to not contain '" + value + "', but was '" + actual + "' instead.", actual != null && !actual.contains(value));
    }

    public String getAttribute(Locator locator, String attributeName)
    {
        return selenium.getAttribute(locator.toString() + "@" + attributeName);
    }

    public int getDefaultWaitForPage()
    {
        return defaultWaitForPage;
    }

    public void setDefaultWaitForPage(int defaultWaitForPage)
    {
        this.defaultWaitForPage = defaultWaitForPage;
    }

    // Return display name that's currently shown in the header
    public String getDisplayName()
    {
        return getText(Locator.id("userMenuPopupText"));
    }

	public String getHtmlSource()
	{
		return selenium.getHtmlSource();
	}

    public boolean isExtTreeNodeSelected(String nodeCaption)
    {
        Locator loc = Locator.xpath("//div[contains(./@class,'x-tree-selected')]/a/span[text()='" + nodeCaption + "']");
        return isElementPresent(loc);
    }

    public boolean isExtTreeNodeExpanded(String nodeCaption)
    {
        Locator loc = Locator.xpath("//div[contains(./@class,'x-tree-node-expanded')]/a/span[text()='" + nodeCaption + "']");
        return isElementPresent(loc);
    }

    // Helper methods for interacting with the query schema browser
    public void selectSchema(String schemaName)
    {
        String[] schemaParts = schemaName.split("\\.");
        if (isExtTreeNodeSelected(schemaParts[schemaParts.length - 1]))
            return;

        String schemaWithParents = "";
        String separator = "";
        for (String schemaPart : schemaParts)
        {
            schemaWithParents += separator + schemaPart;
            separator = ".";

            log("Selecting schema " + schemaWithParents + " in the schema browser...");
            Locator loc = Locator.schemaTreeNode(schemaPart);

            //first load of schemas might a few seconds
            waitForElement(loc, 30000);
            if (isExtTreeNodeExpanded(schemaPart))
                click(loc);
            else
            {
                selenium.doubleClick(loc.toString());
                sleep(1000);
                click(loc);
            }
            waitForElement(Locator.xpath("//div[contains(./@class,'x-tree-selected')]/a/span[text()='" + schemaPart + "']"), 1000);
            waitForText(schemaWithParents + " Schema");
        }
    }

    public void selectQuery(String schemaName, String queryName)
    {
        log("Selecting query " + schemaName + "." + queryName + " in the schema browser...");
        selectSchema(schemaName);
        Locator loc = Locator.queryTreeNode(schemaName, queryName);
        waitForElement(loc, WAIT_FOR_JAVASCRIPT);
        click(loc);
        waitForElement(Locator.linkWithText(schemaName + "." + queryName));
    }

    public void clickFkExpando(String schemaName, String queryName, String columnName)
    {
        String queryLabel = schemaName + "." + queryName;
        click(Locator.xpath("//div/a[text()='" + queryLabel + "']/../../../table/tbody/tr/td/img[(contains(@src, 'plus.gif') or contains(@src, 'minus.gif')) and ../../td[text()='" + columnName + "']]"));
    }

    public void viewQueryData(String schemaName, String queryName)
    {
        viewQueryData(schemaName, queryName, null);
    }

    public void viewQueryData(String schemaName, String queryName, String moduleName)
    {
        selectQuery(schemaName, queryName);
        Locator loc = Locator.xpath("//div[contains(@class, 'lk-qd-name')]/a[text()='" + schemaName + "." + queryName + "']");
        waitForElement(loc, WAIT_FOR_JAVASCRIPT);
        String href = getAttribute(loc, "href");
        if (moduleName != null) // 12474
            assertTextPresent("Defined in " + moduleName + " module");
        log("Navigating to " + href);
        beginAt(href);
    }

    public void editQueryProperties(String schemaName, String queryName)
    {
        selectQuery(schemaName, queryName);
        Locator loc = Locator.linkWithText("edit properties");
        waitForElement(loc, WAIT_FOR_JAVASCRIPT);
        clickAndWait(loc);
    }

    public void createNewQuery(String schemaName)
    {
        selectSchema(schemaName);
        String url = selenium.getEval("selenium.browserbot.getCurrentWindow()._browser.getCreateQueryUrl('" + schemaName + "')");
        if (null == url || url.length() == 0)
            fail("Could not get the URL for creating a new query in schema " + schemaName);
        beginAt(url);
    }


    protected void createQuery(String container, String name, String schemaName, String sql, String xml, boolean inheritable)
    {
        String queryURL = "query/" + container + "/begin.view?schemaName=" + schemaName;
        beginAt(queryURL);
        createNewQuery(schemaName);
        setFormElement("ff_newQueryName", name);
        clickButton("Create and Edit Source");
//        toggleSQLQueryEditor();
        setCodeEditorValue("queryText", sql);
//        setFormElement("queryText", sql);
        if (xml != null)
        {
            _extHelper.clickExtTab("XML Metadata");
            setCodeEditorValue("metadataText", xml);
//        toggleMetadataQueryEditor();
//        setFormElement("metadataText", xml);
        }
        clickButton("Save", 0);
        waitForText("Saved", WAIT_FOR_JAVASCRIPT);
        if (inheritable)
        {
            beginAt(queryURL);
            editQueryProperties("flow", name);
            selectOptionByValue("inheritable", "true");
            submit();
        }
    }

    public void validateQueries(boolean validateSubfolders)
    {
        _extHelper.clickExtButton("Validate Queries", 0);
        Locator locButton = Locator.xpath("//button[text()='Start Validation']");
        Locator locFinishMsg = Locator.xpath("//div[contains(@class, 'lk-vq-status-all-ok') or contains(@class, 'lk-vq-status-error')]");
        waitForElement(locButton, WAIT_FOR_JAVASCRIPT);
        if (validateSubfolders)
            checkCheckbox(Locator.id("lk-vq-subfolders"));
        checkCheckbox(Locator.id("lk-vq-systemqueries"));
        click(locButton);
        waitForElement(locFinishMsg, 120000);
        //test for success
        if (!isElementPresent(Locator.xpath("//div[contains(@class, 'lk-vq-status-all-ok')]")))
        {
            fail("Some queries did not pass validation. See error log for more details.");
        }
    }

    /**
     * Placeholder for WebDriver pressTab
     * @param l
     */
    public void pressTab(Locator l)
    {
        selenium.keyDown(l.toString(), "\\9"); // For Windows
        selenium.keyPress(l.toString(), "\\9"); // For Linux
        selenium.keyUp(l.toString(), "\\9");
    }

    /**
     * Placeholder for WebDriver pressEnter
     * @param l
     */
    public void pressEnter(Locator l)
    {
        selenium.keyDown(l.toString(), "\\13"); // For Windows
        selenium.keyPress(l.toString(), "\\13"); // For Linux
        selenium.keyUp(l.toString(), "\\13");
    }

    /**
     * Placeholder for WebDriver pressDownArrow
     * @param l
     */
    public void pressDownArrow(Locator l)
    {
        selenium.keyDown(l.toString(), "\\40"); // For Windows
        selenium.keyPress(l.toString(), "\\40"); // For Linux
        selenium.keyUp(l.toString(), "\\40");
    }

    public class DefaultSeleniumWrapper extends DefaultSelenium
    {
        DefaultSeleniumWrapper()
        {
            super("localhost", getSeleniumServerPort(), getBrowser(), WebTestHelper.getBaseURL());
        }

        private void log(String s)
        {
            BaseSeleniumWebTest.this.log("selenium - " + s);
        }

        @Override
        public void click(String locator)
        {
            log("Clicking on element: " + locator);
            super.click(locator);
        }

        @Override
        public void doubleClick(String locator)
        {
            log("Double clicking on element: " + locator);
            super.doubleClick(locator);
        }

        @Override
        public void clickAt(String locator, String coordString)
        {
            log("Clicking on element " + locator + " at location " + coordString);
            super.clickAt(locator, coordString);
        }

        public void clickAt(Locator l, String coord)
        {
            clickAt(l.toString().substring(6), coord);
        }

//        public void clickID(String id)
//        {
//        }

        @Override
        public void doubleClickAt(String locator, String coordString)
        {
            log("Double clicking on element " + locator + " at location " + coordString);
            super.doubleClickAt(locator, coordString);
        }

        @Override
        public void fireEvent(String locator, String eventName)
        {
            log("Firing event " + eventName + " on element: " + locator);
            super.fireEvent(locator, eventName);
        }

        @Override
        public void keyPress(String locator, String keySequence)
        {
            log("Pressing key sequence " + keySequence + " on element: " + locator);
            super.keyPress(locator, keySequence);
        }

        @Override
        public void keyDown(String locator, String keySequence)
        {
            log("Sending key down " + keySequence + " on element " + locator);
            super.keyDown(locator, keySequence);
        }

        @Override
        public void keyUp(String locator, String keySequence)
        {
            log("Sending key up " + keySequence + " on element " + locator);
            super.keyUp(locator, keySequence);
        }

        public void mouseClick(String locator)
        {
            log("MouseClick: " + locator);
            super.mouseOver(locator);
            super.mouseDown(locator);
            super.mouseUp(locator);
        }

        @Override
        public void mouseOver(String locator)
        {
            log("MouseOver: " + locator);
            super.mouseOver(locator);
        }

        @Override
        public void mouseOut(String locator)
        {
            log("MouseOut: " + locator);
            super.mouseOut(locator);
        }

        @Override
        public void mouseDown(String locator)
        {
            log("MouseDown: " + locator);
            super.mouseDown(locator);
        }

        @Override
        public void mouseDownAt(String locator, String coordString)
        {
            log("MouseDownAt " + coordString + " for element "+ locator);
            super.mouseDownAt(locator, coordString);
        }

        @Override
        public void mouseUp(String locator)
        {
            log("MouseUp: " + locator);
            super.mouseUp(locator);
        }

        public void mouseUp(Locator l)
        {
            mouseUp(l.toString());
        }

        @Override
        public void mouseUpAt(String locator, String coordString)
        {
            log("MouseUpAt " + coordString + " for element "+ locator);
            super.mouseUpAt(locator, coordString);
        }

        @Override
        public void mouseMove(String locator)
        {
            log("MouseMove: "+ locator);
            super.mouseMove(locator);
        }

        @Override
        public void mouseMoveAt(String locator, String coordString)
        {
            log("MouseMoveAt " + coordString + " for element "+ locator);
            super.mouseMoveAt(locator, coordString);
        }

        public void typeSilent(String locator, String value)
        {
            super.type(locator, value);
        }

        @Override
        public void type(String locator, String value)
        {
            type(locator, value, false);
        }

        public void type(String locator, String value, boolean suppressValueLogging)
        {
            log("Set value of element " + locator + " to "+ (suppressValueLogging ? "[logging suppressed]" : value));
            super.type(locator, value);
        }

        @Override
        public void check(String locator)
        {
            log("Check: " + locator);
            super.check(locator);
        }

        @Override
        public void uncheck(String locator)
        {
            log("Uncheck: " + locator);
            super.uncheck(locator);
        }

        @Override
        public void select(String selectLocator, String optionLocator)
        {
            log("Select " + optionLocator + " from element " + selectLocator);
            super.select(selectLocator, optionLocator);
        }

        @Override
        public void addSelection(String locator, String optionLocator)
        {
            log("Add Selection " + optionLocator + " from element " + locator);
            super.addSelection(locator, optionLocator);
        }

        @Override
        public void removeSelection(String locator, String optionLocator)
        {
            log("Remove Selection " + optionLocator + " from element " + locator);
            super.removeSelection(locator, optionLocator);
        }

        @Override
        public void submit(String formLocator)
        {
            log("Submit form " + formLocator);
            super.submit(formLocator);
        }

        @Override
        public void open(String url)
        {
            open(url, BaseSeleniumWebTest.this.defaultWaitForPage);
        }

        public void open(String url, int millis)
        {
            setTimeout("" + millis);
            _testTimeout = true;
            try
            {
                super.open(url);
            }
            catch (SeleniumException e)
            {
                // fall through if we get a 'livemark' exception, which occurs when running offline
                if (e.getMessage() == null || !e.getMessage().contains("Livemark Service"))
                    throw e;
            }
            // commandProcessor.doCommand("open", new String[] {url,"true"}); // Workaround for XHR errors. http://code.google.com/p/selenium/issues/detail?id=408
            _testTimeout = false;
        }

        @Override
        public void openWindow(String url, String windowID)
        {
            log("Open window " + windowID + " for url " + url);
            super.openWindow(url, windowID);
        }

        @Override
        public void selectWindow(String windowID)
        {
            log("Select window " + windowID);
            super.selectWindow(windowID);
        }

        @Override
        public void selectFrame(String locator)
        {
            log("Select frame " + locator);
            super.selectFrame(locator);
        }

        @Override
        public void waitForPopUp(String windowID, String timeout)
        {
            log("Waiting " + timeout + " ms for pop up " + windowID);
            super.waitForPopUp(windowID, timeout);
        }

        @Override
        public void goBack()
        {
            log("Go back");
            super.goBack();
        }

        @Override
        public void refresh()
        {
            log("Refresh ");
            super.refresh();
        }

        @Override
        public String getConfirmation()
        {
            return super.getConfirmation();
        }

        @Override
        public String getValue(String locator)
        {
            return super.getValue(locator);
        }

        @Override
        public void dragdrop(String locator, String movementsString)
        {
            log("dragdrop element " + locator + " movements: " + movementsString);
            super.dragdrop(locator, movementsString);
        }

        @Override
        public void dragAndDrop(String locator, String movementsString)
        {
            log("dragAndDrop element " + locator + " movements: " + movementsString);
            super.dragAndDrop(locator, movementsString);
        }

        @Override
        public void dragAndDropToObject(String locatorOfObjectToBeDragged, String locatorOfDragDestinationObject)
        {
            log("dragAndDrop element " + locatorOfObjectToBeDragged + " to element " + locatorOfDragDestinationObject);
            super.dragAndDropToObject(locatorOfObjectToBeDragged, locatorOfDragDestinationObject);
        }

        @Override
        public void setCursorPosition(String locator, String position)
        {
            log("Set cursor position for " + locator + " to " + position);
            super.setCursorPosition(locator, position);
        }

    }


    // This class makes it easier to start a specimen import early in a test and wait for completion later.
    public class SpecimenImporter
    {
        private final File _pipelineRoot;
        private final File[] _specimenArchives;
        private final File _tempDir;
        private final String _studyFolderName;
        private final int _completeJobsExpected;
        private final File[] _copiedArchives;
        private boolean _expectError = false;

        public SpecimenImporter(File pipelineRoot, File specimenArchive, File tempDir, String studyFolderName, int completeJobsExpected)
        {
            this(pipelineRoot, new File[] { specimenArchive }, tempDir, studyFolderName, completeJobsExpected);
        }

        public SpecimenImporter(File pipelineRoot, File[] specimenArchives, File tempDir, String studyFolderName, int completeJobsExpected)
        {
            _pipelineRoot = pipelineRoot;
            _specimenArchives = specimenArchives;
            _tempDir = tempDir;
            _studyFolderName = studyFolderName;
            _completeJobsExpected = completeJobsExpected;

            _copiedArchives = new File[_specimenArchives.length];
            for (int i = 0; i < _specimenArchives.length; i++)
            {
                File specimenArchive = _specimenArchives[i];
                String baseName = specimenArchive.getName();
                baseName = baseName.substring(0, baseName.length() - ".specimens".length());
                _copiedArchives[i] = new File(_tempDir, baseName + "_" + FastDateFormat.getInstance("MMddHHmmss").format(new Date()) + ".specimens");
            }
        }

        public void setExpectError(boolean expectError)
        {
            _expectError = expectError;
        }

        public void importAndWaitForComplete()
        {
            startImport();
            waitForComplete();
        }

        @LogMethod
        public void startImport()
        {
            log("Starting import of specimen archive(s):");
            for (File specimenArchive : _specimenArchives)
                log("  " + specimenArchive);

            // copy the file into its own directory
            for (int i = 0; i < _specimenArchives.length; i++)
            {
                File specimenArchive = _specimenArchives[i];
                copyFile(specimenArchive, _copiedArchives[i]);
            }

            clickFolder(_studyFolderName);

            int total = 0;
            while( !isLinkPresentWithText("Manage Files") && total < WAIT_FOR_PAGE)
            {
                // Loop in case test is outrunning the study creator
                sleep(250);
                total += 250;
                refresh();
            }

            clickAndWait(Locator.linkWithText("Manage Files"));
            waitAndClickButton("Process and Import Data");
            _fileBrowserHelper.waitForFileGridReady();

            // TempDir is somewhere underneath the pipeline root.  Determine each subdirectory we need to navigate to reach it.
            File testDir = _tempDir;
            List<String> dirNames = new ArrayList<>();

            while (!_pipelineRoot.equals(testDir))
            {
                dirNames.add(0, testDir.getName());
                testDir = testDir.getParentFile();
            }

            //Build folder path.
            String path = "/";
            for (String dir : dirNames)
                path += dir + "/";

            _fileBrowserHelper.selectFileBrowserItem(path);

            for (File copiedArchive : _copiedArchives)
                _fileBrowserHelper.clickFileBrowserFileCheckbox(copiedArchive.getName());
            _fileBrowserHelper.selectImportDataAction("Import Specimen Data");
            clickButton("Start Import");
        }

        @LogMethod
        public void waitForComplete()
        {
            log("Waiting for completion of specimen archives");

            clickFolder(_studyFolderName);
            clickAndWait(Locator.linkWithText("Manage Files"));

            waitForPipelineJobsToComplete(_completeJobsExpected, "specimen import", _expectError);

            for (File copiedArchive : _copiedArchives)
                if (!copiedArchive.delete())
                    fail("Couldn't delete copied specimen archive: " + copiedArchive.getAbsolutePath());
        }
    }


    // Wait until the pipeline UI shows the requested number of complete jobs.  Fail if any job status becomes "ERROR".
    @LogMethod
    public void waitForPipelineJobsToComplete(int completeJobsExpected, String description, boolean expectError)
    {
        log("Waiting for " + completeJobsExpected + " pipeline jobs to complete");
        List<String> statusValues = getPipelineStatusValues();

        // Short circuit in case we already have too many COMPLETE jobs
        assertTrue("Number of COMPLETE jobs already exceeds desired count", getCompleteCount(statusValues) <= completeJobsExpected);

        startTimer();

        while (getCompleteCount(statusValues) < completeJobsExpected && elapsedSeconds() < MAX_WAIT_SECONDS)
        {
            log("[" + StringUtils.join(statusValues,",") + "]");
            if (!expectError && hasError(statusValues))
                break;
            log("Waiting for " + description);
            sleep(1000);
            refresh();
            statusValues = getPipelineStatusValues();
        }

        if (!expectError)
            assertLinkNotPresentWithText("ERROR");  // Must be surrounded by an anchor tag.
        assertEquals("Did not find correct number of completed pipeline jobs.", completeJobsExpected, getCompleteCount(statusValues));
    }

    // wait until pipeline UI shows that all jobs have finished (either COMPLETE or ERROR status)
    @LogMethod
    protected void waitForPipelineJobsToFinish(int jobsExpected)
    {
        log("Waiting for " + jobsExpected + " pipeline jobs to finish");
        List<String> statusValues = getPipelineStatusValues();
        startTimer();
        while (getFinishedCount(statusValues) < jobsExpected && elapsedSeconds() < MAX_WAIT_SECONDS)
        {
            sleep(1000);
            refresh();
            statusValues = getPipelineStatusValues();
        }
        assertEquals("Did not find correct number of finished pipeline jobs.", jobsExpected, getFinishedCount(statusValues));
    }

    @LogMethod
    protected void waitForRunningPipelineJobs(int wait)
    {
        log("Waiting for running pipeline jobs list to be empty.");
        List<String> statusValues = getPipelineStatusValues();
        startTimer();
        while (statusValues.size() > 0 && elapsedSeconds() < wait)
        {
            log("[" + StringUtils.join(statusValues,",") + "]");
            log("Waiting for " + statusValues.size() + " jobs to complete...");
            sleep(1000);
            refresh();
            statusValues = getPipelineStatusValues();
        }

        assertTrue("Running pipeline jobs were found.  Timeout:" + wait, statusValues.size() == 0);
    }

    public void setCodeEditorValue(String id, String value)
    {
        _extHelper.setCodeEditorValue(id, value);
    }

    public void ensureSignedOut()
    {
        if(isElementPresent(Locator.id("userMenuPopupLink")))
            signOut();
    }

    protected void reloadStudyFromZip(File studyFile)
    {
        goToManageStudy();
        clickButton("Reload Study");
        setFormElement(Locator.name("folderZip"), studyFile);
        clickButton("Reload Study From Local Zip Archive");
        waitForPipelineJobsToComplete(2, "Study Reload", false);

    }

    public AbstractContainerHelper getContainerHelper()
    {
        return _containerHelper;
    }

    public void setContainerHelper(AbstractContainerHelper containerHelper)
    {
        _containerHelper = containerHelper;
    }


    //hopefully we'll come up with a better solution soon
    public void waitForSaveAssay()
    {
        sleep(5000);
    }


}
