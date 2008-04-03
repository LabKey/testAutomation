package org.labkey.test.httpunit;

import com.meterware.httpunit.*;
import junit.framework.Assert;
import net.sourceforge.jwebunit.HttpUnitDialog;
import net.sourceforge.jwebunit.WebTestCase;
import org.apache.commons.lang.time.FastDateFormat;
import org.labkey.test.Runner;
import org.labkey.test.WebTestHelper;
import org.labkey.test.util.PasswordUtil;
import static org.labkey.test.WebTestHelper.*;
import org.labkey.test.Cleanable;
import org.labkey.test.WebTest;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * User: brittp
 * Date: Nov 15, 2005
 * Time: 1:25:46 PM
 */
public abstract class BaseHttpUnitWebTest extends WebTestCase implements Cleanable, WebTest
{
    private WebTestHelper _helper = new WebTestHelper();
    private String _lastPageTitle = null;
    private URL _lastPageURL = null;
    private String _lastPageText = null;
    private List<String> _createdProjects = new ArrayList<String>();
    private List<FolderIdentifier> _createdFolders = new ArrayList<FolderIdentifier>();
    private Stack<String> _locationStack = new Stack<String>();
    private static final int MAX_SERVER_STARTUP_WAIT_SECONDS = 60;

    protected interface RefreshChecker
    {
        boolean checkCondition();
        void refreshPage();
    }

    public BaseHttpUnitWebTest()
    {
        getTestContext().setBaseUrl(getBaseURL());
        // force initialization of _cpasRoot, just to get any warning messages into the logs at the same time
        // as those from _webPort and _contextPath.
        getLabKeyRoot();
    }


    // Return the directory of the module whose functionality this class tests, or "none" if multiple/all modules are tested
    abstract public String getAssociatedModuleDirectory();

    public static String getBaseURL()
    {
        return WebTestHelper.getBaseURL();
    }

    public static String getTargetServer()
    {
        return WebTestHelper.getTargetServer();
    }


    public static String getWebPort()
    {
        return WebTestHelper.getWebPort();
    }

    public static String getContextPath()
    {
        return WebTestHelper.getContextPath();
    }

    public void log(String message)
    {
        System.out.println(message);
    }

    public String getLabKeyRoot()
    {
        return WebTestHelper.getLabKeyRoot();
    }

    private void waitForStartup() throws Exception
    {
        int secondsWaited = 0;
        boolean hitFirstPage = false;
        log("Verifying that server has started...");
        while (!hitFirstPage)
        {
            try
            {
                beginAt("/login/logout.view");
                hitFirstPage = true;
            }
            catch (Exception e)
            {
                if (e.toString().indexOf("404") >= 0 &&  secondsWaited < MAX_SERVER_STARTUP_WAIT_SECONDS)
                {
                    log("Server is not ready.  Waiting " + (MAX_SERVER_STARTUP_WAIT_SECONDS -
                            secondsWaited) + " more seconds...");
                    sleep(1000);
                    secondsWaited++;
                }
                else
                    throw e;
            }
        }
        log("Server is running.");
    }

    protected void signIn()
    {
        try
        {
            PasswordUtil.ensureCredentials();
        }
        catch (IOException e)
        {
            fail("Unable to ensure credentials: " + e.getMessage());
        }
        log("Signing in");
        beginAt("/login/logout.view");
        checkForUpgrade();
        beginAt("/login/login.view");
        assertTitleEquals("Sign In");
        assertFormPresent("login");
        setFormElement("email", PasswordUtil.getUsername());
        setFormElement("password", PasswordUtil.getPassword());
        submit("SUBMIT");

        if (isTextPresent("Type in your email address and password"))
            fail("Could not log in with the saved credentials.  Please verify that the test user exists on this installation or reset the credentials using 'ant setPassword'");
        assertTextPresent("Sign&nbsp;out");
        assertTextPresent("My&nbsp;Account");

        ensureAdminMode();
    }

    protected void ensureAdminMode()
    {
        //Now switch to admin mode if available
        if (isLinkPresentWithText("Enable Admin"))
        {
            clickLinkWithText("Enable Admin");
        }
    }


    private void checkForUpgrade()
    {
        // check to see if we're the first user:
        if (isTextPresent("You are the first user"))
        {
            setFormElement("email", PasswordUtil.getUsername());
            submit("register");
        }

        if (isTextPresent("Type in a new password twice"))
        {
            setFormElement("password", PasswordUtil.getPassword());
            setFormElement("password2", PasswordUtil.getPassword());
            submit("set");
        }

        if (isTitleEqual("Sign In"))
        {
            // if the logout page takes us to the sign-in page, then we may have a schema update to do:
            assertFormPresent("login");
            setFormElement("email", PasswordUtil.getUsername());
            setFormElement("password", PasswordUtil.getPassword());
            submit("SUBMIT");
            if (isNavButtonPresent("Express Install"))
                clickNavButton("Express Install");
            if (isNavButtonPresent("Express Upgrade"))
                clickNavButton("Express Upgrade");
            while (isLinkPresentWithText("Click Here"))
            {
                // Give the scripts a chance to run before pounding the server again
                try
                {
                    Thread.sleep(2000);
                }
                catch (InterruptedException e)
                {
                }

                clickLinkWithText("Click Here");
            }
            if (isNavButtonPresent("Next"))
            {
                clickNavButton("Next");
                // Save the default site config properties
                setWorkingForm("preferences");
                clickNavButton("Save");
            }
            else
            {
                clickLinkWithText("Home");
            }
        }
    }

    protected void signOut()
    {
        log("Signing out");
        beginAt("/login/logout.view");
        assertLinkPresentWithText("Sign In");
    }

    protected void populateLastPageInfo()
    {
        _lastPageTitle = getLastPageTitle();
        _lastPageURL = getLastPageURL();
        _lastPageText = getLastPageText();
    }

    public String getLastPageTitle()
    {
        if (_lastPageTitle == null)
        {
            if (getDialog().getResponse().isHTML())
                return getDialog().getResponsePageTitle();
            else
                return "[no title: content type is not html]";
        }
        return _lastPageTitle;
    }

    public String getResponseText()
    {
        return getDialog().getResponseText();
    }
    
    public String getLastPageText()
    {
        return _lastPageText != null ? _lastPageText : getDialog().getResponseText();
    }

    public URL getLastPageURL()
    {
        return _lastPageURL != null ? _lastPageURL : getDialog().getResponse().getURL();
    }


    public WebResponse changeRedirectTarget (WebClient wc, WebResponse resp, String searchTerm, String replaceTerm)
    {
        WebResponse newResp = null;
        WebRequest wr = null;
        String newTarget;
        String target;
        try {
            target = resp.getHeaderField( "Location" );
            if (null == target || target.compareTo(searchTerm) <0)
                return resp;

            newTarget = target.replaceAll(searchTerm, replaceTerm);

            wr = new GetMethodWebRequest(newTarget);
            if (resp.getHeaderField( "Referer") != null)
                    wr.setHeaderField("Referrer", resp.getHeaderField("Referer")) ;

            discardStream(resp);
            newResp = wc.getResponse(wr);

        } catch (IOException e) {
            fail("Error in changeRedirect: " + e.getMessage());
        }
        catch (SAXException e) {
            fail("Error in changeRedirect: " + e.getMessage());
        }
        return newResp;
    }

    protected void discardStream(WebResponse wr)
    {
        InputStream is = null;
        long cTot = 0;
        long cSkip = 1024 * 1024;
        try {
            is = wr.getInputStream();

            while ((cSkip = is.skip(cSkip)) >0)
                  cTot += cSkip;

            log(cTot + " bytes read in discardStream ");

        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        finally
        {
            try {
                is.close();
            } catch (IOException e) {     }
        }

        return;
    }

    public void resetErrors()
    {
        beginAt("/admin/resetErrorMark.view");
    }

    public void testSteps() throws Exception
    {
        try
        {
            Runner.setCurrentWebTest(this);
            log("\n\n=============== Starting " + getClass().getSimpleName() + Runner.getProgress() + " =================");
            waitForStartup();
            signIn();
            if (getTargetServer().equals(DEFAULT_TARGET_SERVER))
                resetErrors();

            try
            {
                log("Pre-cleaning " + getClass().getSimpleName());
                doCleanup();
            }
            catch (Throwable t)
            {
                // fall through
            }

            if (getTargetServer().equals(DEFAULT_TARGET_SERVER))
                checkLeaks();
            
            doTestSteps();

//            if (enableLinkCheck())
//            {
//                Crawler crawler = new Crawler(this);
//                crawler.crawlAllLinks();
//            }

            try
            {
                if (!skipCleanup())
                    doCleanup();
                else
                    log("Skipping test cleanup as requested.");
            }
            catch (Throwable t)
            {
                //log("WARNING: an exception occurred while cleaning up: " + t.getMessage());
                // fall through
                fail("WARNING: an exception occurred while cleaning up: " + t.getMessage());
            }

            if (getTargetServer().equals(DEFAULT_TARGET_SERVER))
            {
                checkErrors();
                checkLeaks();
            }
            try
            {
                signOut();
            }
            catch (Throwable t)
            {
                // fall through
            }
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
            log("=============== Completed " + getClass().getSimpleName() + Runner.getProgress() + " =================");
        }
    }

    private boolean skipCleanup()
    {
        return "false".equals(System.getProperty("clean"));
    }

    private boolean enableLinkCheck()
    {
        return "true".equals(System.getProperty("linkCheck"));
    }

    public boolean skipLeakCheck()
    {
        return "false".equals(System.getProperty("memCheck"));
    }

    private void checkLeaks()
    {
        if (skipLeakCheck())
            return;
        
        log("Starting memory leak check...");
        int leakCount = MAX_LEAK_LIMIT + 1;
        for (int attempt = 0; attempt < GC_ATTEMPT_LIMIT && leakCount > WebTestHelper.MAX_LEAK_LIMIT; attempt++)
        {
            if (attempt > 0)
            {
                log("Found " + leakCount + " in-use objects; rerunning GC.  ("
                        + (GC_ATTEMPT_LIMIT - attempt) + " attempt(s) remaining.)");
            }
            beginAt("/admin/memTracker.view?gc=true&clearCaches=true");
            if (!isTextPresent("In-Use Objects"))
                fail("Asserts must be enabled to track memory leaks; please add -ea to your server VM params and restart.");
            leakCount = getImageWithAltTextCount("expand/collapse");
        }

        if (leakCount > MAX_LEAK_LIMIT)
            fail(leakCount + " in-use objects exceeds allowed limit of " + MAX_LEAK_LIMIT + ".");
        else
            log("Found " + leakCount + " in-use objects.  This is within the expected number of " + MAX_LEAK_LIMIT + ".");
    }


    public void checkErrors()
    {
        beginAt("/admin/showErrorsSinceMark.view");

        String text = getLastPageText();
        assertEquals("Errors occurred", "", text);
        log("No new errors found.");
    }

    public int getResponseCode()
    {
        return getDialog().getResponse().getResponseCode();
    }
    
    protected abstract void doTestSteps();

    public void cleanup() throws Exception
    {
        log("========= Cleaning up " + getClass().getSimpleName() + " =========");
        // explicitly go back to the site, just in case we're on a 404 or crash page:
        beginAt("");
        signIn();
        doCleanup();

        beginAt("");

        // The following checks verify that the test deleted all projects and folders that it created.
        for (FolderIdentifier folder : _createdFolders)
            assertLinkNotPresentWithText(folder.getFolderName());

        for (String projectName : _createdProjects)
            assertLinkNotPresentWithText(projectName);

        log("========= " + getClass().getSimpleName() + " cleanup complete =========");
    }

    protected abstract void doCleanup() throws Exception;

    /**
     * beginAt assumes that all URLs are relative to the base url.  In our case, the base URL
     * is
     * @param relativeURL
     */
    public void beginAt(String relativeURL)
    {
        if (relativeURL.indexOf(getContextPath() + "/") == 0)
            relativeURL = relativeURL.substring(getContextPath().length() + 1);
        if (relativeURL.length() == 0)
            log("Navigating to root");
        else
            log("Navigating to " + relativeURL);
        super.beginAt(relativeURL);
    }

    protected void clickTab(String tabname)
    {
        log("Selecting tab " + tabname);
        assertLinkPresent(getTabLinkId(tabname));
        clickLink(getTabLinkId(tabname));
    }

    protected void clickImageWithAltText(String altText)
    {
        log("Clicking first image with alt text " + altText );
        try
        {
            WebImage[] images = getDialog().getResponse().getImages();
            for (WebImage image : images)
            {
                if (image.getAltText().equals(altText))
                {
                    WebLink link = image.getLink();
                    link.click();
                    return;
                }
            }
        }
        catch (SAXException e)
        {
            fail("SAXException: " + e.getMessage());
        }
        catch (IOException e)
        {
            fail("IOException: " + e.getMessage());
        }
        fail("Unable to find image with altText " + altText);

    }

    protected void assertTabPresent(String tabText)
    {
        assertLinkPresent(getTabLinkId(tabText));
    }

    protected void assertTabNotPresent(String tabText)
    {
        assertLinkNotPresent(getTabLinkId(tabText));
    }

    public void clickNavButton(String buttonText)
    {
        String imgName = buildNavButtonImagePath(buttonText);
        if (isLinkPresentWithImage(imgName))
            clickLinkWithImage(imgName);
        else
            clickButtonWithImgSrc(imgName);
    }

    public void clickNavButton(String buttonText, String style)
    {
        String imgName = buildNavButtonImagePath(buttonText, style);
        if (isLinkPresentWithImage(imgName))
            clickLinkWithImage(imgName);
        else
            clickButtonWithImgSrc(imgName);
    }

    protected boolean isNavButtonPresent(String buttonText)
    {
        String imgName = buildNavButtonImagePath(buttonText);
        return isLinkPresentWithImage(imgName);
    }

    protected void assertNavButtonPresent(String buttonText)
    {
        String imgName = buildNavButtonImagePath(buttonText);
        //assertLinkPresentWithImage(imgName);
        assertTrue(isLinkPresentWithImage(imgName));
    }

    protected void assertNavButtonNotPresent(String buttonText)
    {
        String imgName = buildNavButtonImagePath(buttonText);
        assertLinkNotPresentWithImage(imgName);
    }

    protected void assertFormElementNotEmpty(String formElementName)
    {
        assertFormElementPresent(formElementName);
        String value = getDialog().getFormParameterValue(formElementName);
        assertFalse("Value of form element " + formElementName + " is empty.", value == null || value.length() == 0);
    }

    protected void assertFormElementValuePresent(String formElementName, String desiredValue)
    {
        assertTrue("Form element with name " + formElementName + " was not found with value " + desiredValue,
                isFormElementValuePresent(formElementName, desiredValue));
    }

    protected void assertFormElementValueNotPresent(String formElementName, String desiredValue)
    {
        assertFalse("Form element with name " + formElementName + " was found with value " + desiredValue,
                isFormElementValuePresent(formElementName, desiredValue));
    }

    private boolean isFormElementValuePresent(String formElementName, String desiredValue)
    {
        assertFormElementPresent(formElementName);
        String[] values = getDialog().getForm().getParameterValues(formElementName);
        for (String foundValue : values)
        {
            if (foundValue.equals(desiredValue))
                return true;
        }
        return false;
    }

    public String toString()
    {
        StringBuilder value = new StringBuilder(super.toString());
        if (getDialog() != null)
        {
            value.append("\nCurrent Page Title = ").append(getLastPageTitle());
            value.append("\nCurrent Page URL = ").append(getLastPageURL().toString()).append("\n");
        }
        return value.toString();
    }

    public File dumpHtml(File dir)
    {
        HttpUnitDialog dialog = getDialog();
        if (dialog == null)
            return null;
        FileWriter writer = null;
        File file;
        try
        {
            FastDateFormat dateFormat = FastDateFormat.getInstance("yyyyMMddHHmm");
            file = new File(dir, dateFormat.format(new Date()) + getClass().getSimpleName() + ".html");
            writer = new FileWriter(file);
            writer.write(getLastPageText());
            return file;
        }
        catch (IOException e)
        {
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
                }
        }
    }

    /**
     * Searches through all tables in the current page, looking for a cell at column position 'searchColumn'
     * that contains text 'searchString'.  If found, this method will return the element names of the
     * contents of the table cell in the same row, at column 'valueColumn'.  Element names are not ordered.
     *
     * @param captionString The value to search for in column 'searchColumn' of all tables.
     * @param captionColumn The column index to search for 'searchString'.
     * @param elementColumn The column index of the desired table cell.
     * @return An array of strings containing the names of all elements within the requested cell, if found.  Null
     *         is returned if the search string is not found in any tables at the specified index.
     */
    protected Set<String> getFormElementNameByTableCaption(String captionString, int captionColumn, int elementColumn)
    {
        try
        {
            WebTable[] tables = getDialog().getResponse().getTables();
            for (WebTable table : tables)
            {
                TableCell result = getFormElementByTableCaption(table, captionString, captionColumn, elementColumn);
                if (result != null)
                    return new HashSet<String>(Arrays.asList(result.getElementNames()));
            }
        }
        catch (SAXException e)
        {
            fail("Tables could not be parsed due to " + e.getLocalizedMessage());
            return null;
        }
        fail("Unable to find form element at column " + elementColumn + " of any table row with caption " +
                captionString + " at column " + captionColumn);
        return null;
    }

    protected TableCell getFormElementByTableCaption(WebTable table, String captionString, int captionColumn, int elementColumn)
    {
        if (table.getColumnCount() > Math.max(captionColumn, elementColumn))
        {
            for (int row = 0; row < table.getRowCount(); row++)
            {
                if (captionString.equals(table.getCellAsText(row, captionColumn)))
                {
                    TableCell cell = table.getTableCell(row, elementColumn);
                    String[] elementNames = cell.getElementNames();
                    if (elementNames != null && elementNames.length > 0)
                        return cell;
                }
            }
        }

        // didn't find it: move on to searching all nested tables:
        for (int row = 0; row < table.getRowCount(); row++)
        {
            for (int col = 0; col < table.getColumnCount(); col++)
            {
                TableCell cell = table.getTableCell(row, col);
                if (cell != null)
                {
                    WebTable[] subtables = cell.getTables();
                    for (WebTable subtable : subtables)
                    {
                        TableCell result = getFormElementByTableCaption(subtable, captionString, captionColumn, elementColumn);
                        if (result != null)
                            return result;
                    }
                }
            }
        }
        return null;
    }

    protected WebLink getWebLinkByTableCaption(String caption, int captionCol, int elementCol, HTMLElementPredicate predicate, Object criteria)
    {
        try
        {
            WebTable[] tables = getDialog().getResponse().getTables();
            for (WebTable table : tables)
            {
                TableCell result = getFormElementByTableCaption(table, caption, captionCol, elementCol);
                if (result != null)
                {
                    return result.getFirstMatchingLink(predicate, criteria);
                }
            }
        }
        catch (SAXException e)
        {
            fail("Tables could not be parsed due to " + e.getLocalizedMessage());
        }
        return null;
    }

    protected void assertTableCellTextEquals(String tableSummaryOrId, int row, int col, String value)
    {
        assertTablePresent(tableSummaryOrId);
        String[][] sparseTableCellValues = getDialog().getSparseTableBySummaryOrId(tableSummaryOrId);
        if (sparseTableCellValues.length <= row)
            Assert.fail("Not enough rows in table " + tableSummaryOrId + " found " + sparseTableCellValues.length + " rows, needed " + row + 1);
        String[] rowStrings = sparseTableCellValues[row];
        if (rowStrings.length <= col)
            Assert.fail("Not enough cols in row " + row + " of table " + tableSummaryOrId + " found " + rowStrings.length + " cols, needed " + col + 1);

        assertTrue(rowStrings[col].equals(value));
    }

    protected String findPermissionsSelectName(String groupName)
    {
        Set<String> elements = getFormElementNameByTableCaption(groupName, 0, 1);
        assertTrue("Expected one (and only one) form element per row in permissions table", elements != null && elements.size() == 1);
        return (String)elements.toArray()[0];
    }
/*
    protected void clickButtonWithValue(String buttonValue)
    {
        log("Clicking button with value " + buttonValue);
        WebForm form = getDialog().getForm();
        if (form == null)
            throw new IllegalStateException("setWorkingForm must be called before trying to submit.");
        SubmitButton[] buttons = form.getSubmitButtons();
        for (SubmitButton button : buttons)
        {
            if (buttonValue.equals(button.getValue()))
            {
                try
                {
                    button.click();
                    logJavascriptAlerts();
                    return;
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e);
                }
                catch (SAXException e)
                {
                    throw new RuntimeException(e);
                }
            }
        }
        fail("Unable to find button with value " + buttonValue);
    }
*/
    private void clickButtonWithImgSrc(String imgSrc)
    {
        log("Clicking button with image source " + imgSrc);
        WebForm form = getDialog().getForm();
        if (form == null)
            throw new IllegalStateException("Test failure. The problem may be that setWorkingForm must be called before trying to submit, or it may be that you need to set your LabKey Server font size to Small.");
        SubmitButton[] buttons = form.getSubmitButtons();
        for (SubmitButton button : buttons)
        {
            if (button.isImageButton())
            {
                String src = button.getAttribute("src");
                String buttonNoSize = imgSrc;
                if (buttonNoSize.indexOf('?') > 0)
                    buttonNoSize = buttonNoSize.substring(0, buttonNoSize.indexOf('?'));
                if (src != null && src.indexOf(buttonNoSize) >= 0)
                {
                    try
                    {
                        button.click();
                        logJavascriptAlerts();
                        return;
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                        fail("Unable to click button with image source " + imgSrc + ": " + e.getMessage());
                    }
                    catch (SAXException e)
                    {
                        e.printStackTrace();
                        fail("Unable to click button with image source " + imgSrc + ": " + e.getMessage());
                    }
                }
            }
        }
        fail("Unable to find button with image source " + imgSrc + " in current working form.");
    }

    protected void logJavascriptAlerts()
    {
        while (getDialog().getWebClient().getNextAlert() != null)
            log("JavaScript Alert Ignored: " + getDialog().getWebClient().popNextAlert());
    }

    protected void createSubfolder(String project, String child, String[] tabsToAdd)
    {
        // create a child of the top-level project folder:
        createSubfolder(project, project, child, "None", tabsToAdd);
    }

    protected void createSubfolder(String project, String parent, String child, String folderType, String[] tabsToAdd)
    {
        if (isLinkPresentWithText(child))
            fail("Cannot create folder; A link with text " + child + " already exists.  " +
                    "This folder may already exist, or the name appears elsewhere in the UI.");
        assertLinkNotPresentWithText(child);
        log("Creating subfolder " + child + " under project " + parent);
        clickLinkWithText(project);
        clickLinkWithText("Manage Folders");
        // click last index, since this text appears in the nav tree
        clickLinkWithText(parent, countLinksWithText(parent) - 1);
        clickNavButton("Create Subfolder");
        setFormElement("name", child);
        setFormElement("folderType", folderType);
        submit();

        _createdFolders.add(new FolderIdentifier(project, child));
        if (!"None".equals(folderType))
        {
            if (null == tabsToAdd || tabsToAdd.length == 0)
                return;

            clickLinkWithText("Customize Folder");
        }
        // verify that we're on the customize tabs page, then submit:
        assertTextPresent("Customize folder /" + project);

        if (tabsToAdd != null)
        {
            for (String tabname : tabsToAdd)
                toggleCheckboxByTitle(tabname);
        }

        submit();

        if (tabsToAdd != null)
        {
            for (String tabname : tabsToAdd)
                assertTabPresent(tabname);
        }

        // verify that there's a link to our new folder:
        assertLinkPresentWithText(child);
    }

    protected void deleteFolder(String project, String folderName)
    {
        log("Deleting folder " + folderName + " under project " + project);
        clickLinkWithText("Home");
        clickLinkWithText(project);
        clickLinkWithText("Manage Folders");
        clickLink("managefolders/" + folderName);
        clickNavButton("Delete");
        // confirm delete:
        clickNavButton("Delete");
        // verify that we're not on an error page with a check for a project link:
        assertLinkPresentWithText(project);
        assertLinkNotPresentWithText(folderName);
    }

    /**
     * Note that this requires that all subfolders already be deleted before deleting the project
     * @param project
     */
    protected void deleteProject(String project)
    {
        log("Deleting project " + project);
        clickLinkWithText(project);
        //Delete even if terms of use is required
        if (getDialog().hasFormParameterNamed("approvedTermsOfUse"))
        {
            checkCheckbox("approvedTermsOfUse");
            clickNavButton("Sign In");
        }

        clickLinkWithText("Manage Folders");
        clickNavButton("Delete");
        // confirm delete:
        clickNavButton("Delete");
        // verify that we're not on an error page with a check for a project link:
        assertLinkNotPresentWithText(project);
    }

    protected void toggleCheckboxByTitle(String title)
    {
        log("Toggling checkbox with title " + title);
        WebForm form = getDialog().getForm();
        if (form == null)
            throw new IllegalStateException("setWorkingForm must be called before trying to submit.");
        String[] paramNames = form.getParameterNames();
        if (paramNames != null)
        {
            for (String paramName : paramNames)
            {
                String[] paramTitles = form.getOptions(paramName);
                if (paramTitles != null)
                {
                    for (String paramTitle : paramTitles)
                    {
                        if (title.equals(paramTitle))
                        {
                            String[] possibleValues = form.getOptionValues(paramName);
                            String value = form.getParameterValue(paramName);
                            if (value == null)
                            {
                                if (possibleValues != null && possibleValues.length > 0)
                                    value = possibleValues[0];
                                else
                                    value = "on";
                                checkCheckbox(paramName, value);
                            }
                            else
                                uncheckCheckbox(paramName);
                            return;
                        }
                    }
                }
            }
        }
        fail("Unable to find checkbox with title " + title);
    }

    protected List<WebForm> getFormsByAction(String action)
    {
        List<WebForm> formList = new ArrayList<WebForm>();
        try
        {
            WebForm[] forms = getDialog().getResponse().getForms();
            for (WebForm form : forms)
            {
                if (action.equals(form.getAction()))
                    formList.add(form);
            }
        }
        catch (SAXException e)
        {
            throw new RuntimeException(e);
        }
        assertTrue("Unable to find form with action " + action, !formList.isEmpty());
        return formList;
    }

    protected void addWebPart(String webpartName)
    {
        log("Adding web part " + webpartName + " to current page");
        List<WebForm> forms = getFormsByAction("addWebPart.view");
        for (WebForm form : forms)
        {
            String[] parts = form.getOptionValues("name");
            for (String part : parts)
            {
                if (webpartName.equals(part))
                {
                    form.setParameter("name", part);
                    try
                    {
                        form.submit();
                        return;
                    }
                    catch (IOException e)
                    {
                        throw new RuntimeException(e);
                    }
                    catch (SAXException e)
                    {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    protected void createProject(String projectName, String folderType)
    {
        ensureAdminMode();
        
        log("Creating project with name " + projectName);
        if (isLinkPresentWithText(projectName))
            fail("Cannot create project; A link with text " + projectName + " already exists.  " +
                    "This project may already exist, or its name appears elsewhere in the UI.");
        clickLinkWithText("Create Project");
        setFormElement("name", projectName);
        if(folderType != null){
            setFormElement("folderType", folderType);
        }
        submit();
        _createdProjects.add(projectName);
    }

    protected void createProject(String projectName){
        createProject(projectName, null);
    }

    protected void createPermissionsGroup(String groupName)
    {
        log("Creating permissions group " + groupName);
        assertTextPresent("Permissions for /");
        setFormElement("name", groupName);
        submit();
    }

    protected void assertPermissionSetting(String groupName, String permissionSetting)
    {
        setWorkingForm("updatePermissions");
        String permSelectName = findPermissionsSelectName(groupName);
        assertOptionEquals(permSelectName, permissionSetting);
    }

    protected void setPermissions(String groupName, String permissionString)
    {
        log("Setting permissions for group " + groupName + " to " + permissionString);
        setWorkingForm("updatePermissions");
        String selectName = findPermissionsSelectName(groupName);
        String[] options = getDialog().getForm().getOptions(selectName);
        String[] optionValues = getDialog().getForm().getOptionValues(selectName);
        String value = null;
        for (int i = 0; i < options.length && value == null; i++)
        {
            if (permissionString.equals(options[i]))
                value = optionValues[i];
        }
        assertNotNull(value, "Did not find permission with name '" + permissionString + "'");
        setFormElement(selectName, value);
        clickNavButton("Update");
        assertPermissionSetting(groupName, permissionString);
    }

    protected Map<String, String> getRequestGetParameters()
    {
        Map<String, String> params = new HashMap<String, String>();
        URL responseURL = getDialog().getResponse().getURL();
        String query = responseURL.getQuery();
        if (query != null && query.length() > 0)
        {
            String[] nameValuePairs = query.split("&");
            for (String pair : nameValuePairs)
            {
                int equalsIdx = pair.indexOf("=");
                if (equalsIdx > 0)
                    params.put(pair.substring(0, equalsIdx), pair.substring(equalsIdx + 1, pair.length()));
            }
        }
        return params;
    }


    public URL getURL() throws MalformedURLException
    {
        return getDialog().getResponse().getURL();
    }

    public String[] getLinkAddresses()
    {
        try
        {
            WebLink[] links = getDialog().getResponse().getLinks();
            String [] addresses = null == links ? new String[0] : new String[links.length];
            int i = 0;
            for (WebLink l : links)
                addresses[i++] = l.getURLString();

            return addresses;
        }
        catch (SAXException e)
        {
            throw new RuntimeException(e);
        }
    }


    protected List<MapArea> getAreasForImageMap(String imageMapName)
    {
        try
        {
            NodeList mapList = getDialog().getResponse().getDOM().getElementsByTagName("map");
            if (mapList != null)
            {
                for (int mapIndex = 0; mapIndex < mapList.getLength(); mapIndex++)
                {
                    Node mapNode = mapList.item(mapIndex);
                    NamedNodeMap mapAttribs = mapNode.getAttributes();
                    Node nameNode = mapAttribs.getNamedItem("name");
                    if (imageMapName.equals(nameNode.getTextContent()))
                    {
                        List<MapArea> areas = new ArrayList<MapArea>();

                        // get the area children
                        NodeList areaList = mapNode.getChildNodes();
                        for (int areaIndex = 0; areaIndex < areaList.getLength(); areaIndex++)
                        {
                            Node areaNode = areaList.item(areaIndex);
                            if ("area".equalsIgnoreCase(areaNode.getNodeName()))
                            {
                                NamedNodeMap areaAttribs = areaNode.getAttributes();
                                Node shapeAttrib = areaAttribs.getNamedItem("shape");
                                Node hrefAttrib = areaAttribs.getNamedItem("href");
                                Node titleAttrib = areaAttribs.getNamedItem("title");
                                Node altAttrib = areaAttribs.getNamedItem("alt");
                                Node coordsAttrib = areaAttribs.getNamedItem("coords");

                                MapArea area = new MapArea(shapeAttrib != null ? shapeAttrib.getTextContent() : "",
                                        hrefAttrib != null ? hrefAttrib.getTextContent() : "",
                                        titleAttrib != null ? titleAttrib.getTextContent() : "",
                                        altAttrib != null ? altAttrib.getTextContent() : "",
                                        coordsAttrib != null ? coordsAttrib.getTextContent() : "");

                                areas.add(area);
                            }
                        }
                        return areas;
                    }
                }
            }
        }
        catch (SAXException e)
        {
            // log, then fall through to return an empty list;
            System.err.println("ERROR: unable to parse ");
        }
        fail("Unable to find image map with name " + imageMapName);
        return null;
    }

    private boolean hasElementWithAttributeValue(String element, String attribute, String value)
    {
        try
        {
            NodeList fontTagList = getDialog().getResponse().getDOM().getElementsByTagName(element);
            if (fontTagList != null)
            {
                for (int fontTagIndex = 0; fontTagIndex < fontTagList.getLength(); fontTagIndex++)
                {
                    Node fontTagNode = fontTagList.item(fontTagIndex);
                    NamedNodeMap mapAttribs = fontTagNode.getAttributes();
                    Node nameNode = mapAttribs.getNamedItem(attribute);
                    if (nameNode != null && value.equals(nameNode.getTextContent()))
                        return true;
                }
            }
        }
        catch (SAXException e)
        {
            // log, then fall through to return an empty list;
            e.printStackTrace();
            fail("ERROR: unable to parse document: " + e.getException());
        }
        return false;
    }

    protected boolean hasError()
    {
        return hasElementWithAttributeValue("font", "class", "error");
    }

    protected void assertTitlePresentInImageMap(String mapname, String titleText)
    {
        List<MapArea> areas = getAreasForImageMap(mapname);
        assertNotNull("Unable to find image map with name " + mapname, areas);
        for (MapArea area : areas)
        {
            if (area.getTitle().indexOf(titleText) >= 0)
                return;
        }
        fail("Unable to find title text '" + titleText + "' within image map '" + mapname + "'.");
    }

    protected void clickImageMapLinkByTitle(String mapname, String areaTitle)
    {
        clickImageMapLinkByTitle(mapname, areaTitle, 0);
    }
    protected void clickImageMapLinkByTitle(String mapname, String areaTitle, int index)
    {
        log("Clicking link with title " + areaTitle + " in image map " + mapname);
        List<MapArea> areas = getAreasForImageMap(mapname);
        for (MapArea area : areas)
        {
            if (area.getTitle().compareTo(areaTitle) == 0)
            {
                if (index == 0)
                {
                    beginAt(area.getHref());
                    return;
                }
                else
                    index--;
            }
        }
        fail("Unable to find title text '" + areaTitle + "' within image map '" + mapname + "'.");
    }

    protected void refreshAndCheck(RefreshChecker checker, int secTimeout)
    {
        for (int i = 0; i < secTimeout; i++)
        {
            checker.refreshPage();
            if (checker.checkCondition())
                return;
            sleep(1000);
        }
        fail("Condition never met.");
    }

    protected String getCurrentRelativeURL()
    {
        URL url = getDialog().getResponse().getURL();
        String urlString = url.toString();
        if ("80".equals(_helper.getWebPort()) && url.getAuthority().endsWith(":-1"))
        {
            int portIdx = urlString.indexOf(":-1");
            urlString = urlString.substring(0, portIdx) + urlString.substring(portIdx + (":-1".length()));
        }

        String baseURL = getTestContext().getBaseUrl();
        assertTrue("Expected URL to begin with " + baseURL + ", but found " + urlString, urlString.indexOf(baseURL) == 0);
        return urlString.substring(baseURL.length());
    }

    protected void pushLocation()
    {
        _locationStack.push(getCurrentRelativeURL());
    }

    protected void popLocation()
    {
        String location = _locationStack.pop();
        assertNotNull("Cannot pop without a push.", location);
        beginAt(location);
    }

    protected void refresh()
    {
        log("Refreshing page.");
        beginAt(getCurrentRelativeURL());
    }

    protected void sleep(long ms)
    {
        try
        {
            Thread.sleep(ms);
        }
        catch (InterruptedException e)
        {
        }
    }

    protected boolean isTextPresent(String text)
    {
        return getDialog().isTextInResponse(text);
    }

    protected boolean isLinkPresentWithText(String linkText)
    {
        return getDialog().isLinkPresentWithText(linkText);
    }

    protected boolean isLinkPresentWithText(String linkText, int index)
    {
        return getDialog().isLinkPresentWithText(linkText, index);
    }

    protected boolean isLinkPresentWithImage(String imageFileName)
    {
        String and = imageFileName.indexOf('?') == -1 ? "?" : "&amp;"; 
        return getDialog().isLinkPresentWithImage(imageFileName) ||
                getDialog().isLinkPresentWithImage(imageFileName + and + DEFAULT_BUTTON_FONT_SIZE);
    }

    protected boolean isTitleEqual(String title)
    {
        return title.equals(getDialog().getResponsePageTitle());
    }

    public void setWorkingForm(String nameOrId)
    {
        log("Setting working form to " + nameOrId);
        super.setWorkingForm(nameOrId);
    }

    protected void checkCheckbox(String checkBoxName)
    {
        log("Checking checkbox " + checkBoxName);
        getDialog().getForm().setCheckbox(checkBoxName, true);
        logJavascriptAlerts();
    }

    protected void checkCheckbox(String checkBoxName, String value)
    {
        log("Checking checkbox " + checkBoxName + ", setting value to " + value);
        super.checkCheckbox(checkBoxName, value);
        logJavascriptAlerts();
    }

    protected void clickButton(String buttonId)
    {
        log("Clicking button " + buttonId);
        super.clickButton(buttonId);
        logJavascriptAlerts();
    }

    protected void clickLink(String linkId)
    {
        log("Clicking link " + linkId);
        super.clickLink(linkId);
        logJavascriptAlerts();
    }

    protected void clickLinkWithImage(String imageFileName)
    {
        log("Clicking link with image " + imageFileName);
        String and = imageFileName.indexOf('?') == -1 ? "?" : "&amp;";
        if (getDialog().isLinkPresentWithImage(imageFileName))
            super.clickLinkWithImage(imageFileName);
        else
            super.clickLinkWithImage(imageFileName + and + DEFAULT_BUTTON_FONT_SIZE);
        logJavascriptAlerts();
    }

    protected void clickLinkWithText(String linkText)
    {
        try
        {
            log("Clicking link with text " + linkText);
            super.clickLinkWithText(linkText);
        }
        finally
        {
            logJavascriptAlerts();
        }
    }

    protected void clickLinkWithText(String linkText, int index)
    {
        log("clicking link " + index + " with text " + linkText);
        super.clickLinkWithText(linkText, index);
        logJavascriptAlerts();
    }

    protected int countLinksWithText(String linkText)
    {
        int count = 0;
        try
        {
            HttpUnitDialog dialog = getDialog();
            WebLink[] links = dialog.getResponse().getLinks();
            for (WebLink link : links)
            {
                Node node = link.getDOMSubtree();
                if (HttpUnitDialog.nodeContainsText(node, linkText))
                    count++;
            }
        }
        catch (SAXException sxe)
        {
            throw new RuntimeException(sxe);
        }

        return count;
    }

    protected void clickLinkWithTextAfterText(String linkText, String labelText)
    {
        log("Clicking link with link text " +  linkText + " and label text " + labelText);
        super.clickLinkWithTextAfterText(linkText, labelText);
        logJavascriptAlerts();
    }

    public void selectOption(String selectName, String option)
    {
        log("Selecting option " + option + " from select " + selectName);
        super.selectOption(selectName, option);
        logJavascriptAlerts();
    }

    protected void selectOptionByValue(String selectName, String value)
    {
        log("Selecting option with value " + value + " from select " + selectName);
        getDialog().setFormParameter(selectName, value);
        logJavascriptAlerts();
    }

    protected void setFormElement(String formElementName, File fileToUpload)
    {
        formElementName = beforeSetFormElement(formElementName, fileToUpload.getPath());
        getDialog().getForm().setParameter(formElementName, new UploadFileSpec[] { new UploadFileSpec(fileToUpload) });
        logJavascriptAlerts();
    }

    private String beforeSetFormElement(String formElementName, String value)
    {
        if (!isFormElementPresent(formElementName) && isFormElementPresent(formElementName.toLowerCase()))
        {
            log("Can't find form elment with name " + formElementName + ", using lower-case version.");
            formElementName = formElementName.toLowerCase();
        }
        if (formElementName.toLowerCase().indexOf("password") >= 0)
            log("Setting form element " + formElementName + " to value **********");
        else
        {
            if (value.length() > 1000)
                log("Setting form element " + formElementName + " to value " + value.substring(0, 500) + "...");
            else
                log("Setting form element " + formElementName + " to value " + value);
        }
        return formElementName;
    }

    protected void checkCheckbox(String name, int index)
    {
        List<String> allValues = new ArrayList<String>();
        getInputValues(getDialog().getForm().getDOMSubtree(), allValues, name);
        checkCheckbox(name, allValues.get(index));
    }

    private void getInputValues(Node current, List<String> values, String inputName)
    {
        if (current != null)
        {
            if ("input".equalsIgnoreCase(current.getNodeName()))
            {
                Node nameNode = current.getAttributes().getNamedItem("name");
                if (nameNode != null && nameNode.getTextContent().equals(inputName))
                {
                    Node valueNode = current.getAttributes().getNamedItem("value");
                    values.add(valueNode.getTextContent());
                }
            }
            else
            {
                NodeList children = current.getChildNodes();
                for (int i = 0; i < children.getLength(); i++)
                    getInputValues(children.item(i), values, inputName);
            }
        }
    }

    protected void setFormElement(String formElementName, String value)
    {
        formElementName = beforeSetFormElement(formElementName, value);
        super.setFormElement(formElementName, value);
        logJavascriptAlerts();
    }

    protected void setFormElement(String formElementName, String[] values)
    {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.length; i++)
            builder.append(i > 0 ? ", " : "").append(values[i]);
        formElementName = beforeSetFormElement(formElementName, builder.toString());
        getDialog().getForm().setParameter(formElementName, values);
        logJavascriptAlerts();
    }

    protected boolean isFormElementPresent(String formElementName)
    {
        return getDialog().hasFormParameterNamed(formElementName);
    }

    protected void setFormElementWithLabel(String formElementLabel, String value)
    {
        log("Setting form element with label " + formElementLabel + " to value" + value);
        super.setFormElementWithLabel(formElementLabel, value);
        logJavascriptAlerts();
    }

    protected void submit()
    {
        log("Submitting form");
        super.submit();
        logJavascriptAlerts();
    }

    public void submit(String buttonName)
    {
        log("Submitting form with button " + buttonName);
        super.submit(buttonName);
        logJavascriptAlerts();
    }

    protected void uncheckCheckbox(String checkBoxName)
    {
        log("Unchecking checkbox " + checkBoxName);
        super.uncheckCheckbox(checkBoxName);
        logJavascriptAlerts();
    }

    protected void uncheckCheckbox(String checkBoxName, String value)
    {
        log("Unchecking checkbox " + checkBoxName + " with value " + value);
        super.uncheckCheckbox(checkBoxName, value);
        logJavascriptAlerts();
    }

    public void gotoWindow(String windowName)
    {
        log("Changing focus to window " + windowName + ".");
        super.gotoWindow(windowName);
        logJavascriptAlerts();
    }

    public void gotoFrame(String frameName)
    {
        log("Changing focus to frame " + frameName + ".");
        super.gotoFrame(frameName);
        logJavascriptAlerts();
    }

    public void gotoPage(String pageName)
    {
        log("Changing focus to page " + pageName + ".");
        super.gotoPage(pageName);
        logJavascriptAlerts();
    }

    protected void assertImagePresentWithSrc(String src)
    {
        assertImagePresentWithSrc(src, false);
    }

    protected void assertImagePresentWithSrc(String src, boolean substringMatch)
    {
        log("Checking for image with src " + src);
        try
        {
            WebImage[] images = getDialog().getResponse().getImages();
            for (WebImage image : images)
            {
                String source = image.getSource();
                if ((substringMatch && source.indexOf(src) >= 0) || source.equals(src))
                    return;
            }
            fail("Unable to find image with src " + src);
        }
        catch (SAXException e)
        {
            fail("Unable to find image due to SAXException: " + e.getMessage());
        }
    }

    protected void assertImagePresentWithName(String name)
    {
        log("Checking for image with name " + name);
        try
        {
            if (getDialog().getResponse().getImageWithName(name) == null)
                fail("Unable to find image with name " + name);
        }
        catch (SAXException e)
        {
            fail("Unable to find image due to SAXException: " + e.getMessage());
        }
    }

    protected void assertImagePresentWithAltText(String altText)
    {
        log("Checking for image with alt text " + altText);
        try
        {
            if (getDialog().getResponse().getImageWithAltText(altText) == null)
                fail("Unable to find image with alt text " + altText);
        }
        catch (SAXException e)
        {
            fail("Unable to find image due to SAXException: " + e.getMessage());
        }
    }

    protected void assertLinkPresentWithTextCount(String text, int count)
    {
        int actual = getLinkWithTextCount(text);
        assertTrue("Expected " + count + " links with text " + text + ", but found " + actual, actual == count);
    }


    protected int getLinkWithTextCount(String text)
    {
        int count = 0;
        try
        {
            WebLink[] links = getDialog().getResponse().getLinks();
            for (WebLink link : links)
            {
                if (link.getText().indexOf(text) >= 0)
                    count++;
            }
        }
        catch (SAXException e)
        {
            fail("SAXException: " + e.getMessage());
        }
        return count;
    }

    protected int getImageWithAltTextCount(String altText)
    {
        int count = 0;
        try
        {
            WebImage[] images = getDialog().getResponse().getImages();
            for (WebImage image : images)
            {
                if (image.getAltText().equals(altText))
                    count++;
            }
        }
        catch (SAXException e)
        {
            fail("SAXException: " + e.getMessage());
        }
        return count;
    }

    protected String getFileContents(String rootRelativePath)
    {
        if (rootRelativePath.charAt(0) != '/')
            rootRelativePath = "/" + rootRelativePath;
        File file = new File(getLabKeyRoot() + rootRelativePath);
        return getFileContents(file);
    }

    protected String getFileContents(File file)
    {
        FileInputStream fis = null;
        BufferedReader reader = null;
        try
        {
            fis = new FileInputStream(file);
            reader = new BufferedReader(new InputStreamReader(fis));
            StringBuilder content = new StringBuilder();
            int read;
            char[] buffer = new char[1024];
            while ((read = reader.read(buffer, 0, buffer.length)) > 0)
                content.append(buffer, 0, read);
            return content.toString();
        }
        catch (IOException e)
        {
            fail(e.getMessage());
            return null;
        }
        finally
        {
            if (reader != null) try { reader.close(); } catch (IOException e) {}
            if (fis != null) try { fis.close(); } catch (IOException e) {}
        }
    }

    protected void copyFile(File original, File copy)
    {
        InputStream fis = null;
        OutputStream fos = null;
        try
        {
            copy.getParentFile().mkdirs();
            fis = new BufferedInputStream(new FileInputStream(original));
            fos = new BufferedOutputStream(new FileOutputStream(copy));
            int read;
            byte[] buffer = new byte[1024];
            while ((read = fis.read(buffer, 0, buffer.length)) > 0)
                fos.write(buffer, 0, read);
        }
        catch (IOException e)
        {
            fail(e.getMessage());
        }
        finally
        {
            if (fis != null) try { fis.close(); } catch (IOException e) {}
            if (fos != null) try { fos.close(); } catch (IOException e) {}
        }
    }

    protected void deleteUser(String userEmail)
    {
        clickLinkWithText("Site Users");
        try
        {
            WebTable[] tables = getDialog().getResponse().getTables();
            for (WebTable table : tables)
            {
                TableCell result = getFormElementByTableCaption(table, userEmail, 3, 0);
                if (result != null)
                {
                    // look for the checkbox value..
                    Node child = result.getDOM().getFirstChild();
                    if (child != null && "input".equalsIgnoreCase(child.getNodeName()))
                    {
                        Node value = child.getAttributes().getNamedItem("value");
                        if (value != null)
                        {
                            String userId = value.getTextContent();
                            if (userId != null)
                            {
                                checkCheckbox(".select", userId);
                                clickNavButton("Delete");
                                break;
                            }
                        }
                    }
                }
            }
        }
        catch (SAXException e)
        {
            fail("Tables could not be parsed due to " + e.getLocalizedMessage());
        }
    }

    /*
     * This assumes that you have added the "search" webpart to your project
     */
    protected void searchFor(String projectName, String searchFor, String expectedResults, String titleName) {
        log("Searching Project : " + projectName + " for \"" + searchFor +"\".  Expecting to find : " + expectedResults + " results");
        clickLinkWithText(projectName);
        assertFormElementPresent("search");
        setFormElement("search", searchFor);
        clickNavButton("Search");
        if(Integer.parseInt(expectedResults) == 0) {
            assertTextPresent("found no result");
        } else {
            assertTextPresent("found " + expectedResults + " result");
        }

        log("found \"" + expectedResults + "\" result of " + searchFor);
        if(titleName != null) {
            clickLinkWithText(titleName);
            assertTextPresent(searchFor);
        }
    }

    protected void searchFor(String projectName, String searchFor, String expectedResults) {
        searchFor(projectName, searchFor, expectedResults, null);
    }


    public List<String> getCreatedProjects()
    {
        return Collections.unmodifiableList(_createdProjects);
    }
}
