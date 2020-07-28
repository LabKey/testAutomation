package org.labkey.test.pages.pipeline;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.collections.CaseInsensitiveHashSet;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PipelineStatusTable;
import org.labkey.test.util.TextSearcher;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

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
    private final Set<String> FINISHED_STATES = new CaseInsensitiveHashSet("COMPLETE", "ERROR", "CANCELLED");

    public static PipelineStatusDetailsPage beginAt(WebDriverWrapper driver, int rowId)
    {
        driver.beginAt(WebTestHelper.buildURL("pipeline-status", driver.getCurrentContainerPath(), "details", Map.of("rowId", String.valueOf(rowId))));
        return new PipelineStatusDetailsPage(driver);
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
        return elementCache().statusText.getText();
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
        return waitForStatus(status, defaultWaitForPage);
    }

    @LogMethod
    public PipelineStatusDetailsPage waitForStatus(String status, int wait)
    {
        waitFor(() -> status.equals(getStatus()),
                "Expected status '" + status + "', but was '" + getStatus() + "'",
                wait);
        return this;
    }

    @LogMethod
    public PipelineStatusDetailsPage waitForStatus(Set<String> status, int wait)
    {
        waitFor(() -> status.contains(getStatus()),
                "Expected status to be one of '" + StringUtils.join(status, "', '") + "'",
                wait);
        return this;
    }

    /**
     * Wait for job to finish: status is either COMPLETE, ERROR, or CANCELLED
     */
    public PipelineStatusDetailsPage waitForFinish()
    {
        waitForStatus(FINISHED_STATES, defaultWaitForPage);
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
        return waitForComplete(defaultWaitForPage);
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
            clickAndWait(elementCache().statusText);
            log("Redirected on success as expected");
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
     * See <code>{@link PipelineJob.TaskStatus#isActive()}</code>
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

    public boolean isShowingLogSummary()
    {
        String details = elementCache().showFullLogLink.getAttribute("data-details");
        assertNotNull("Expected 'data-details' attribute on 'show-full-log' link", details);
        return "false".equals(details);
    }

    public PipelineStatusDetailsPage showLogSummary()
    {
        if (!isShowingLogSummary())
            elementCache().showFullLogLink.click();
        return this;
    }

    public PipelineStatusDetailsPage showLogDetails()
    {
        if (isShowingLogSummary())
            elementCache().showFullLogLink.click();
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

    @LogMethod
    public PipelineStatusTable clickShowGrid()
    {
        clickAndWait(elementCache().showGridButton);
        return new PipelineStatusTable(getDriver());
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
        elementCache().cancelButton.click();
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
        protected WebElement errorList = Locator.id("error-list").findWhenNeeded(this);
        protected WebElement created = Locator.id("created").findWhenNeeded(this);
        protected WebElement modified = Locator.id("modified").findWhenNeeded(this);
        protected WebElement email = Locator.id("email").findWhenNeeded(this);
        protected WebElement statusSpinner = Locator.id("status-spinner").findWhenNeeded(this);
        protected WebElement statusText = Locator.id("status-text").findWhenNeeded(this);
        protected WebElement info = Locator.id("info").findWhenNeeded(this);
        protected WebElement description = Locator.id("description").findWhenNeeded(this);
        protected WebElement filePath = Locator.id("file-path").findWhenNeeded(this);
        protected WebElement filesList = Locator.id("files-list").findWhenNeeded(this);
        protected WebElement parentJobTableBody = Locator.id("parent-job-table-body").findWhenNeeded(this);
        protected WebElement splitJobsTableBody = Locator.id("split-jobs-table-body").findWhenNeeded(this);
        protected WebElement runsList = Locator.id("runs-list").findWhenNeeded(this);

        protected WebElement showGridButton = Locator.lkButton("Show Grid").findWhenNeeded(this);
        protected WebElement showDataButton = Locator.id("show-data-btn").findWhenNeeded(this);
        protected WebElement browseFilesButton = Locator.lkButton("Browse Files").findWhenNeeded(this);
        protected WebElement cancelButton = Locator.id("cancel-btn").findWhenNeeded(this);
        protected WebElement retryButton = Locator.id("retry-btn").findWhenNeeded(this);

        protected WebElement showFullLogLink = Locator.id("show-full-log").findWhenNeeded(this);
        protected WebElement logData = Locator.id("log-data").findWhenNeeded(this);
    }
}
