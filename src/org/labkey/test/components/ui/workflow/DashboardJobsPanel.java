package org.labkey.test.components.ui.workflow;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.react.ReactSelect;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.labkey.test.WebDriverWrapper.waitFor;

public class DashboardJobsPanel extends WebDriverComponent<DashboardJobsPanel.ElementCache>
{
    private WebDriver _driver;
    final WebElement _componentElement;

    public DashboardJobsPanel(WebElement element, WebDriver driver)
    {
        _driver = driver;
        _componentElement = element;
    }

    @Override
    public WebElement getComponentElement()
    {
        return _componentElement;
    }

    @Override
    protected WebDriver getDriver()
    {
        return _driver;
    }

    public boolean isPanelLoaded()
    {
        try
        {
            return !Locator.tagContainingText("span", "Loading").findElement(this).isDisplayed();
        }
        catch(NoSuchElementException nse)
        {
            return true;
        }
    }

    public void clickStartNewJob()
    {
        elementCache().startNewJobLink.click();
    }

    public void clickJobsHome()
    {
        elementCache().jobsHomeLink.click();
    }

    public DashboardJobsPanel clickYourQueueTab()
    {
        elementCache().yourQueueTab.click();

        waitFor(()->{
            return elementCache().mineWorkflowPanel.getAttribute("aria-hidden").toLowerCase().equals("false");
        }, 1_000);

        return this;
    }

    public DashboardJobsPanel clickActiveJobsTab()
    {
        elementCache().activeJobsTab.click();

        waitFor(()->{
            return elementCache().activeWorkflowPanel.getAttribute("aria-hidden").toLowerCase().equals("false");
        }, 1_000);

        return this;
    }

    public void clickYourQueueJob(String jobTitle)
    {
        if(!isYourQueueTabSelected())
            clickYourQueueTab();

        clickJobNameInQueue(jobTitle, elementCache().mineWorkflowPanel);
    }

    public void clickActiveQueueJob(String jobTitle)
    {
        if(!isActiveJobsTabSelected())
            clickActiveJobsTab();

        clickJobNameInQueue(jobTitle, elementCache().activeWorkflowPanel);
    }

    private void clickJobNameInQueue(String jobTitle, WebElement panel)
    {
        waitFor(()-> Locator.linkWithText(jobTitle).findElement(panel).isDisplayed(), 5_000);

        Locator.linkWithText(jobTitle).findElement(panel).click();
    }

    public List<String> getYourJobsList()
    {
        if(!isYourQueueTabSelected())
            clickYourQueueTab();

        return getJobsInQueue(elementCache().mineWorkflowPanel);
    }

    public List<String> getActiveJobsList()
    {
        if(!isActiveJobsTabSelected())
            clickActiveJobsTab();

        return getJobsInQueue(elementCache().activeWorkflowPanel);
    }

    private List<String> getJobsInQueue(WebElement panel)
    {

        // TODO need to get a better wait condition.
        getWrapper().sleep(500);

        List<WebElement> links = Locator.tag("a").findElements(panel);

        List<String> jobsInQueue = new ArrayList<>();
        for(WebElement jobLink : links)
        {
            jobsInQueue.add(jobLink.getText());
        }

        return jobsInQueue;

    }

    public DashboardJobsPanel setYourQueuePriorityLevel(String priority)
    {

        if(!isYourQueueTabSelected())
            clickYourQueueTab();

        elementCache().priorityFilter(elementCache().mineWorkflowPanel).select(priority);
        return this;
    }

    public DashboardJobsPanel setActiveJobsPriorityLevel(String priority)
    {

        if(!isActiveJobsTabSelected())
            clickActiveJobsTab();

        elementCache().priorityFilter(elementCache().activeWorkflowPanel).select(priority);
        return this;
    }

    public String getYourQueuePanelText()
    {
        return getPanelText(elementCache().mineWorkflowPanel);
    }

    public String getActiveJobsPanelText()
    {
        return getPanelText(elementCache().activeWorkflowPanel);
    }


    private String getPanelText(WebElement panel)
    {
        return panel.getText();
    }

    public Map<PanelField, String> getYourQueueJobInfo(String jobName)
    {

        if(!isYourQueueTabSelected())
            clickYourQueueTab();

        return getJobInfo(jobName, elementCache().mineWorkflowPanel);

    }

    public Map<PanelField, String> getActiveJobsJobInfo(String jobName)
    {

        if(!isActiveJobsTabSelected())
            clickActiveJobsTab();

        return getJobInfo(jobName, elementCache().activeWorkflowPanel);

    }

    public enum PanelField
    {
        JOB_INFO_NAME,
        JOB_INFO_TASKS,
        JOB_INFO_SAMPLES,
        JOB_INFO_DUE_DATE
    }

    private Map<PanelField, String> getJobInfo(String sampleName, WebElement panel)
    {

        waitFor(()-> panel.isDisplayed(), 500);

        Map<PanelField, String> jobInfo = new HashMap<>();
        WebElement jobRow;

        // If there are no jobs return null.
        if(!doesActivePanelHaveJobs())
            return null;

        jobRow = Locator.linkWithText(sampleName).parent("div").parent("div").findElement(panel);

        // Wait for the two columns of data to be rendered.
        waitFor(()-> Locator.tag("div").findElements(jobRow).size() == 2, 1_000);

        // Now wait for the two spans, which have job info, to be rendered.
        waitFor(()-> Locator.tag("span").findElements(jobRow).size() == 2, 1_000);

        List<WebElement> columns = Locator.tag("div").findElements(jobRow);

        jobInfo.put(PanelField.JOB_INFO_NAME, Locator.tag("a").findElement(columns.get(0)).getText());
        jobInfo.put(PanelField.JOB_INFO_TASKS, Locator.tag("span").findElement(columns.get(0)).getText());

        jobInfo.put(PanelField.JOB_INFO_DUE_DATE, Locator.tag("span").findElement(columns.get(1)).getText());
        jobInfo.put(PanelField.JOB_INFO_SAMPLES, Locator.tagWithClassContaining("span", "display-light").findElement(columns.get(1)).getText());

        return jobInfo;
    }

    private boolean isYourQueueTabSelected()
    {
        return elementCache().yourQueueTab.getAttribute("aria-selected").toLowerCase().equals("true");
    }

    private boolean isActiveJobsTabSelected()
    {
        return elementCache().activeJobsTab.getAttribute("aria-selected").toLowerCase().equals("true");
    }

    private boolean doesActivePanelHaveJobs()
    {
        try
        {

            if (isYourQueueTabSelected())
            {
                return Locator.tagContainingText("div", "No jobs currently in your queue.").findElement(elementCache().mineWorkflowPanel).isDisplayed();
            }
            else
            {
                return Locator.tagWithClassContaining("div", "alert-warning").findElement(elementCache().mineWorkflowPanel).isDisplayed();
            }

        }
        catch(NoSuchElementException nse)
        {
            return true;
        }
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component.ElementCache
    {
        WebElement startNewJobLink = Locator.linkContainingText("Start a New Job").findWhenNeeded(this);
        WebElement jobsHomeLink = Locator.linkContainingText("Jobs Home").findWhenNeeded(this);
        WebElement yourQueueTab = Locator.linkContainingText("Your Queue").findWhenNeeded(this);
        WebElement activeJobsTab = Locator.linkContainingText("Active Jobs").findWhenNeeded(this);

        // These are the panels for each of the tabs.
        WebElement mineWorkflowPanel = Locator.tagWithId("div", "active-workflow-tabs-pane-mine").refindWhenNeeded(this);
        WebElement activeWorkflowPanel = Locator.tagWithId("div", "active-workflow-tabs-pane-active").refindWhenNeeded(this);

        ReactSelect priorityFilter(WebElement panel)
        {
            return ReactSelect.finder(getDriver()).findWhenNeeded(panel);
        }
    }

    public static class DashboardJobsPanelFinder extends WebDriverComponentFinder<DashboardJobsPanel, DashboardJobsPanelFinder>
    {

        private Locator _locator;

        public DashboardJobsPanelFinder(WebDriver driver)
        {
            super(driver);
            _locator = Locator.xpath("//div[contains(@class, 'section-panel--title-medium')][text()='Jobs List']/ancestor::div[@class='panel-body']");
        }

        @Override
        protected DashboardJobsPanel construct(WebElement element, WebDriver driver)
        {
            return new DashboardJobsPanel(element, driver);
        }

        @Override
        protected Locator locator()
        {
            return _locator;
        }
    }

}

