package org.labkey.test.pages.pipeline;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.collections.CaseInsensitiveHashSet;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.FileBrowserHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PipelineStatusTable;
import org.labkey.test.util.TextSearcher;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class PipelineStatusDetailsPage extends LabKeyPage<PipelineStatusDetailsPage.ElementCache>
{
    private static final Set<String> FINISHED_STATES = new CaseInsensitiveHashSet("COMPLETE", "ERROR", "CANCELLED");
    private static final int DEFAULT_PIPELINE_WAIT = BaseWebDriverTest.MAX_WAIT_SECONDS * 1000;

    public static PipelineStatusDetailsPage beginAt(WebDriverWrapper driver, int rowId)
    {
        driver.beginAt(WebTestHelper.buildURL("pipeline-status", driver.getCurrentContainerPath(), "details", Map.of("rowId", String.valueOf(rowId))));
        return new PipelineStatusDetailsPage(driver);
    }

    @Override
    protected void waitForPage()
    {
        shortWait().until(ExpectedConditions.visibilityOf(elementCache().statusText));
    }

    public PipelineStatusDetailsPage(WebDriver driver)
    {
        super(driver);
    }

    public PipelineStatusDetailsPage(WebDriverWrapper driver)
    {
        super(driver);
    }

    public String getErrorList()
    {
        return elementCache().errorList.getText();
    }

    public String getCreated()
    {
        return elementCache().created.getText();
    }

    public String getModified()
    {
        return elementCache().modified.getText();
    }

    public String getEmail()
    {
        return elementCache().email.getText();
    }

    public String getStatus()
    {
        String retVal = null;
        try
        {
            retVal = elementCache().statusText.getText();
        }
        catch (StaleElementReferenceException e)
        {
            return retVal;
        }
        return retVal;
    }

    public String getDescription()
    {
        return elementCache().description.getText();
    }

    public String getInfo()
    {
        return elementCache().info.getText();
    }

    public String getFilePath()
    {
        return elementCache().filePath.getText();
    }

    public List<WebElement> getFileLinks()
    {
        return elementCache().filesList.findElements(By.tagName("A"));
    }

    public List<WebElement> getRunLinks()
    {
        return elementCache().runsList.findElements(By.tagName("A"));
    }

    public String getLogText()
    {
        return elementCache().logData.getText();
    }

    @LogMethod
    public PipelineStatusDetailsPage assertStatus(String status)
    {
        assertEquals("Incorrect job status", status, getStatus());
        return this;
    }

    @LogMethod
    public PipelineStatusDetailsPage waitForStatus(String status)
    {
        return waitForStatus(status, DEFAULT_PIPELINE_WAIT);
    }

    @LogMethod
    public PipelineStatusDetailsPage waitForStatus(String status, int wait)
    {
        waitForFinish(wait);
        assertStatus(status);
        return this;
    }

    /**
     * Wait for job to finish: status is either COMPLETE, ERROR, or CANCELLED
     */
    public PipelineStatusDetailsPage waitForFinish()
    {
        return waitForFinish(DEFAULT_PIPELINE_WAIT);
    }

    /**
     * Wait for job to finish: status is either COMPLETE, ERROR, or CANCELLED
     */
    public PipelineStatusDetailsPage waitForFinish(int timoutMs)
    {
        waitFor(() -> FINISHED_STATES.contains(getStatus()), () -> "Pipeline job did not finish. Final state: " + getStatus(), timoutMs);
        return this;
    }

    /** Wait for CANCELLED job status */
    public PipelineStatusDetailsPage waitForCancelled()
    {
        waitForStatus("CANCELLED");
        return this;
    }

    /** Wait for COMPLETE job status */
    public PipelineStatusDetailsPage waitForComplete()
    {
        return waitForComplete(DEFAULT_PIPELINE_WAIT);
    }

    /** Wait for COMPLETE job status */
    @LogMethod
    public PipelineStatusDetailsPage waitForComplete(int wait)
    {
        // flow signals it would like to redirect when COMPLETED by using a URL parameter.
        boolean expectRedirect = getUrlParam("redirect") != null;
        if (expectRedirect)
            log("Expecting redirect when complete");

        waitForStatus("COMPLETE", wait);

        if (expectRedirect)
        {
            // The click does nothing, but we want to wait for the redirect before continuing
            log("Process done, clicking and waiting for redirect");
            clickAndWait(elementCache().statusText);
            log("Redirected on success as expected: " + getDriver().getCurrentUrl());
        }

        return this;
    }

    /** Wait for ERROR job status */
    public PipelineStatusDetailsPage waitForError()
    {
        return waitForError(List.of());
    }

    /** Wait for ERROR job status and expected errors in the log text. */
    public PipelineStatusDetailsPage waitForError(String expectedError)
    {
        return waitForError(List.of(expectedError));
    }

    /** Wait for ERROR job status and expected errors in the log text. */
    public PipelineStatusDetailsPage waitForError(List<String> expectedErrors)
    {
        waitForStatus("ERROR");
        if (!expectedErrors.isEmpty())
        {
            log("Checking for expected errors");
            assertLogTextContains(expectedErrors);
        }
        return this;
    }

    /**
     * Returns true if the job is in the queue waiting to run or is running.
     * See <code>{@link org.labkey.api.pipeline.PipelineJob.TaskStatus#isActive()}</code>
     */
    public boolean isActive()
    {
        return isWaiting() || !isFinished();
    }

    /**
     * Returns true if the job is COMPLETED, ERROR, or CANCELLED
     */
    public boolean isFinished()
    {
        String status = getStatus();
        return FINISHED_STATES.contains(status);
    }

    /**
     * Return true if the job is WAITING
     */
    public boolean isWaiting()
    {
        String status = getStatus();
        return "WAITING".equalsIgnoreCase(status) || status.toLowerCase().endsWith("waiting");
    }

    public boolean isErrored()
    {
        return "ERROR".equalsIgnoreCase(getStatus());
    }

    /** Return true if the log status file was found (the log data element is present.) */
    public PipelineStatusDetailsPage waitForLogPresent()
    {
        waitForElement(elementCache().logDataId);
        return this;
    }

    public boolean isShowingLogSummary()
    {
        String details = elementCache().showFullLogLink.getAttribute("data-details");
        assertNotNull("Expected 'data-details' attribute on 'show-full-log' link", details);
        return "false".equals(details);
    }

    @LogMethod
    public PipelineStatusDetailsPage showLogSummary()
    {
        if (!isShowingLogSummary())
            elementCache().showFullLogLink.click();
        waitForElement(Locator.linkWithText("Show full log file"));
        return this;
    }

    @LogMethod
    public PipelineStatusDetailsPage showLogDetails()
    {
        if (isShowingLogSummary())
            elementCache().showFullLogLink.click();
        waitForElement(Locator.linkWithText("Show summary"));
        return this;
    }

    public PipelineStatusDetailsPage waitForLogText(String logText)
    {
        waitForLogText(logText, defaultWaitForPage);
        log("Found log text: " + logText);
        return this;
    }

    @LogMethod
    public PipelineStatusDetailsPage waitForLogText(String logText, int wait)
    {
        // log text is updated on page without having to refresh
        waitFor(() -> elementCache().logData.getText().contains(logText), "Expected '" + logText + "' in log data", wait);
        log("Found log text: " + logText);
        return this;
    }

    public PipelineStatusDetailsPage assertLogTextContains(Collection<String> texts)
    {
        return assertLogTextContains(texts.toArray(new String[0]));
    }

    @LogMethod
    public PipelineStatusDetailsPage assertLogTextContains(String... texts)
    {
        assertTextPresent(new TextSearcher(getLogText()), texts);
        log("Found log text: " + StringUtils.join(texts, ", "));
        return this;
    }

    public PipelineStatusDetailsPage assertLogTextNotContains(Collection<String> texts)
    {
        return assertLogTextNotContains(texts.toArray(new String[0]));
    }

    @LogMethod
    public PipelineStatusDetailsPage assertLogTextNotContains(String... texts)
    {
        assertTextNotPresent(new TextSearcher(getLogText()), texts);
        log("Log text not present: " + StringUtils.join(texts, ", "));
        return this;
    }

    @LogMethod
    public PipelineStatusTable clickShowGrid()
    {
        clickAndWait(elementCache().showGridButton);
        return new PipelineStatusTable(getDriver());
    }

    @LogMethod
    public FileBrowserHelper clickBrowseFiles()
    {
        clickAndWait(elementCache().browseFilesButton);
        return new FileBrowserHelper(getDriver());
    }

    public List<WebElement> getSplitJobLinks()
    {
        return elementCache().splitJobsTableBody.findElements(By.tagName("A"));
    }

    public void forEachSplitJobLink(Consumer<PipelineStatusDetailsPage> action)
    {
        List<WebElement> splitJobLinks = getSplitJobLinks();
        int count = splitJobLinks.size();
        log("Found " + count + " split jobs");

        for (int i = 0; i < count; i++)
        {
            pushLocation();
            action.accept(clickSplitJobLink(i));
            popLocation();
            // NOTE: force refetching the split jobs links after navigating
            clearCache();
        }
    }

    public PipelineStatusDetailsPage clickSplitJobLink(int i)
    {
        WebElement link = getSplitJobLinks().get(i);
        log("Clicking link: " + link.getText());
        clickAndWait(link);
        return new PipelineStatusDetailsPage(getDriver());
    }


    public WebElement getParentJobLink()
    {
        return elementCache().parentJobTableBody.findElement(By.tagName("A"));
    }

    /**
     * Click the first run link.
     */
    @LogMethod
    public void clickRunLink()
    {
        List<WebElement> runLinks = getRunLinks();
        assertFalse("Expected at least one run link", runLinks.isEmpty());
        WebElement runLink = runLinks.get(0);
        log("clicking run link '" + runLink.getText() + "' with URL: " + runLink.getAttribute("href"));
    }

    /**
     * Click the run link with the given name.
     * @param runName
     */
    @LogMethod
    public void clickRunLink(@Nullable String runName)
    {
        List<WebElement> runLinks = getRunLinks();
        assertFalse("Expected at least one run link", runLinks.isEmpty());
        for (WebElement runLink : runLinks)
        {
            if (runName.equals(runLink.getText()))
            {
                log("clicking run link '" + runLink.getText() + "' with URL: " + runLink.getAttribute("href"));
                runLink.click();
            }
        }
    }

    /**
     * Data button will be hidden if job is not yet complete or there is no dataUrl for the job.
     * @return true if the data button is clicked
     */
    @LogMethod
    public boolean clickDataLink()
    {
        if (elementCache().showDataButton.isDisplayed())
        {
            elementCache().showDataButton.click();
            log("Clicked 'Data' link");
            return true;
        }

        return false;
    }

    @LogMethod
    public PipelineStatusDetailsPage clickRetry()
    {
        clickAndWait(elementCache().retryButton);
        return new PipelineStatusDetailsPage(getDriver());
    }

    @LogMethod
    public PipelineStatusDetailsPage clickCancel()
    {
        clickAndWait(elementCache().cancelButton);
        waitForLogText("Attempting to cancel");
        waitForCancelled();
        return this;
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        protected final WebElement errorList = Locator.id("error-list").findWhenNeeded(this);
        protected final WebElement created = Locator.id("created").findWhenNeeded(this);
        protected final WebElement modified = Locator.id("modified").findWhenNeeded(this);
        protected final WebElement email = Locator.id("email").findWhenNeeded(this);
        protected final WebElement statusSpinner = Locator.id("status-spinner").findWhenNeeded(this);
        protected final WebElement statusText = Locator.id("status-text").refindWhenNeeded(this);
        protected final WebElement info = Locator.id("info").findWhenNeeded(this);
        protected final WebElement description = Locator.id("description").findWhenNeeded(this);
        protected final WebElement filePath = Locator.id("file-path").findWhenNeeded(this);
        protected final WebElement filesList = Locator.id("files-list").findWhenNeeded(this);
        protected final WebElement parentJobTableBody = Locator.id("parent-job-table-body").findWhenNeeded(this);
        protected final WebElement splitJobsTableBody = Locator.id("split-jobs-table-body").findWhenNeeded(this);
        protected final WebElement runsList = Locator.id("runs-list").findWhenNeeded(this);

        protected final WebElement showGridButton = Locator.lkButton("Show Grid").findWhenNeeded(this);
        protected final WebElement showDataButton = Locator.id("show-data-btn").findWhenNeeded(this);
        protected final WebElement browseFilesButton = Locator.lkButton("Browse Files").findWhenNeeded(this);
        protected final WebElement cancelButton = Locator.id("cancel-btn").findWhenNeeded(this);
        protected final WebElement retryButton = Locator.id("retry-btn").findWhenNeeded(this);

        protected final WebElement showFullLogLink = Locator.id("show-full-log").findWhenNeeded(this);
        protected final WebElement logContainer = Locator.id("log-container").findWhenNeeded(this);
        protected final Locator logDataId = Locator.id("log-data");
        protected final WebElement logData = logDataId.findWhenNeeded(this);
    }
}
