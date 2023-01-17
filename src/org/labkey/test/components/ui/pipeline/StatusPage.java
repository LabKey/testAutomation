package org.labkey.test.components.ui.pipeline;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatusPage extends LabKeyPage<LabKeyPage<?>.ElementCache>
{

    public StatusPage(WebDriverWrapper driver)
    {
        super(driver);
    }

    @Override
    protected void waitForPage()
    {
        waitFor(()-> {
                    try
                    {
                        return elementCache().pageHeader().isDisplayed() &&
                                elementCache().statusDetailPanel().isDisplayed() &&
                                elementCache().statusLogPanel().isDisplayed();
                    }
                    catch(NoSuchElementException | StaleElementReferenceException nse)
                    {
                        return false;
                    }
                },
                "The 'Pipeline Status' page did not load in time.",
                2_500);
    }

    public String getPageHeader()
    {
        return elementCache().pageHeader().getText();
    }

    private Map<String, String> internal_getDetailInfo()
    {
        // It may take a moment for the details to show up.
        waitFor(()->elementCache().statusDetailPanel().isDisplayed(), "No details are visible.", 500);

        List<WebElement> tableRows = Locator.tagWithClass("tr", "pipeline-job-status-detail-row")
                .findElements(elementCache().statusDetailPanel());

        Map<String, String> details = new HashMap<>();

        for(WebElement tr : tableRows)
        {
            List<WebElement> tds = Locator.tag("td").findElements(tr);
            String detailId = tds.get(0).getText().trim().replace(":", "");
            details.put(detailId, tds.get(1).getText());
        }

        return details;
    }

    public Map<String, String> getDetailInfo()
    {
        // Trying to protect against elements going away because the page refreshed.
        // If the caller was unlucky and hit this during a refresh, catching the exception and trying again should
        // recover from that.
        try
        {
            return internal_getDetailInfo();
        }
        catch (NoSuchElementException | StaleElementReferenceException exception)
        {
            log("Ouch! Hit a page refresh while getting log detail info.");
            return internal_getDetailInfo();
        }
    }

    private String internal_getLog()
    {
        // Wait a moment to make sure the log is loaded.
        waitFor(()->elementCache().statusLogPanel().isDisplayed(), "No log is visible.", 500);

        return Locator.tag("table")
                .findElement(elementCache().statusLogPanel()).getText();
    }

    public String getLog()
    {
        // Trying to protect against elements going away because the page refreshed.
        // If the caller was unlucky and hit this during a refresh, catching the exception and trying again should
        // recover from that.
        try
        {
            return internal_getLog();
        }
        catch (NoSuchElementException | StaleElementReferenceException exception)
        {
            log("Ouch! Hit a page refresh while getting log info.");
            return internal_getLog();
        }
    }

    public ImportsPage goToImportsPage()
    {
        elementCache().importsPageLink().click();
        return new ImportsPage(this);
    }

    @Override
    protected ElementCache elementCache()
    {
        return (ElementCache) super.elementCache();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage<?>.ElementCache
    {

        final WebElement pageHeader()
        {
            return Locator.tagWithClass("div", "page-header")
                    .child(Locator.tagWithClass("h2", "no-margin-top"))
                    .findWhenNeeded(this);
        }

        final WebElement statusDetailPanel()
        {
            return Locator.tagWithClass("div", "pipeline-job-status-detail")
                    .child(Locator.tagWithClass("div", "panel-body"))
                    .refindWhenNeeded(this);
        }

        final WebElement statusLogPanel()
        {
            return Locator.tagWithClass("div", "pipeline-job-status-log")
                    .child(Locator.tagWithClass("div", "panel-body"))
                    .refindWhenNeeded(this);
        }

        final WebElement importsPageLink()
        {
            return Locator.tagWithClass("div", "parent-nav")
                    .child(Locator.linkWithHref("#/pipeline"))
                    .findWhenNeeded(this);
        }

    }

    public enum StatusInfo
    {
        CREATED("Created"),
        STATUS("Status"),
        INFO("Info");

        private String value;

        StatusInfo(String value)
        {
            this.value = value;
        }

        public String getValue()
        {
            return value;
        }

    }

}
