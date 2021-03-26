package org.labkey.test.components.ui.workflow;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.domain.DomainPanel;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class JobAssignSamplesPanel extends DomainPanel
{

    protected JobAssignSamplesPanel(WebElement element, WebDriver driver)
    {
        super(element, driver);
    }

    @Override
    protected JobAssignSamplesPanel getThis()
    {
        return this;
    }

    public WorkflowJobIncludedSamplesPanel showIncludedSamples()
    {
        if (isSearchMode())
            elementCache().modeSelectTab("Included Samples").findElement(this).click();
        WebDriverWrapper.waitFor(()-> !isSearchMode(), 2000);
        return new WorkflowJobIncludedSamplesPanel(elementCache().panel, getDriver());
    }

    public WorkflowJobSamplesSearchPanel showSearchForSamples()
    {
        if (!isSearchMode())
            elementCache().modeSelectTab("Search for Samples").findElement(this).click();
        WebDriverWrapper.waitFor(()-> isSearchMode(), 2000);


        return new WorkflowJobSamplesSearchPanel(elementCache().panel, getDriver());
    }


    private boolean isSearchMode()
    {
        return elementCache().modeSelectTab("Search for Samples").withAttribute("class", "active")
                .findOptionalElement(this).isPresent();
    }

    @Override
    protected ElementCache elementCache()
    {
       return new ElementCache();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }


    protected class ElementCache extends DomainPanel.ElementCache
    {
        Locator.XPathLocator modeSelectTab(String text)
        {
            return Locator.tagWithClass("li", "form-step-tab").withText(text);
        }
        public WebElement panel = panelBody;
    }

    public static class JobAssignSamplesPanelFinder extends BaseDomainPanelFinder<JobAssignSamplesPanel, JobAssignSamplesPanel.JobAssignSamplesPanelFinder>
    {
        public JobAssignSamplesPanelFinder(WebDriver driver)
        {
            super(driver);
        }

        @Override
        protected JobAssignSamplesPanel construct(WebElement el, WebDriver driver)
        {
            return new JobAssignSamplesPanel(el, driver);
        }
    }
}
