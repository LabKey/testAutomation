package org.labkey.test.components.ui.audit;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventDetailsPanel extends WebDriverComponent<EventDetailsPanel.ElementCache>
{
    private final WebDriver driver;
    private final WebElement editingDiv;

    public EventDetailsPanel(final WebElement element, final WebDriver driver)
    {
        this.driver = driver;
        editingDiv = element;
    }

    @Override
    public WebElement getComponentElement()
    {
        return editingDiv;
    }

    @Override
    protected WebDriver getDriver()
    {
        return driver;
    }

    public boolean isPanelLoaded()
    {
        try
        {
            if (Locator.tagWithText("div", "No event selected").findElement(this).isDisplayed())
                return false;
            else
                return true;
        }
        catch (NoSuchElementException nse)
        {
            return true;
        }
    }

    public String getPanelText()
    {
        return getComponentElement().getText();
    }

    public String getEventDescription()
    {
        if(isPanelLoaded())
        {
            return elementCache().eventDescription().getText();
        }
        else
        {
            return "";
        }
    }

    public Map<String, String> getMessageGridMap()
    {
        Map<String, String> gridData = new HashMap<>();

        if(isPanelLoaded())
        {
            List<WebElement> gridRows = Locator.tag("tr").findElements(elementCache().messageGrid());

            for (WebElement gridRow : gridRows)
            {
                List<WebElement> tds = Locator.tag("td").findElements(gridRow);

                if (!tds.isEmpty())
                {
                    gridData.put(tds.get(0).getText(), tds.get(1).getText());
                }

            }
        }

        return gridData;
    }

    public Map<String, Map<String, String>> getAuditDetailsMap()
    {
        Map<String, Map<String, String>> auditDetails = new HashMap<>();

        String xpath = "//div[contains(@class,'margin-bottom')][not(contains(@class,'display-light'))]";
        List<WebElement> auditDetailRowElements = Locator.xpath(xpath).findElements(this);

        for (WebElement rowElem : auditDetailRowElements)
        {
            String label = Locator.tagWithClassContaining("span", "audit-detail-row-label")
                    .findElement(rowElem).getText();

            // Clean up the label.
            label = label.trim();
            if(label.contains(":"))
                label = label.substring(0, label.indexOf(':'));

            Map<String, String> values = new HashMap<>();

            try
            {
                String oldValue = Locator.tagWithClassContaining("span", "old-audit-value")
                        .findElement(rowElem).getText();
                values.put("old_value", oldValue);
            }
            catch (NoSuchElementException nse)
            {
                // Do nothing. If there is no old value this is a registration event.
            }

            try
            {
                String newValue = Locator.tagWithClassContaining("span", "new-audit-value")
                        .findElement(rowElem).getText().trim();
                values.put("new_value", newValue);

            }
            catch (NoSuchElementException nse)
            {
                // Do nothing. The modified field will not have a new value if the update happens within a minute of
                // the last event.
            }

            auditDetails.put(label, values);
        }

        return auditDetails;
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {

        // Need to make all of the elements in this panel functions. Nothing will be there if no event is selected,
        // and some elements will disappear based on the event selected.
        final WebElement eventDescription()
        {
            return Locator.tagWithClassContaining("div", "display-light")
                    .childTag("div").findElement(this);
        }

        // Make this a function to protect against a sample that doesn't have a timeline causing a catastrophic failure.
        final WebElement messageGrid()
        {
            return Locator.tagWithClass("div", "table-responsive").childTag("table").findElement(this);
        }
    }

    public static class EventDetailsPanelFinder extends WebDriverComponentFinder<EventDetailsPanel, EventDetailsPanel.EventDetailsPanelFinder>
    {
        Locator _locator = Locator.xpath( "//div[contains(@class,'panel-heading')][text()='Event Details']/following-sibling::div[@class='panel-body']");
        public EventDetailsPanelFinder(WebDriver driver)
        {
            super(driver);
        }

        @Override
        protected EventDetailsPanel construct(WebElement element, WebDriver driver)
        {
            return new EventDetailsPanel(element, driver);
        }

        public EventDetailsPanelFinder withPanelTitle(String panelTitle)
        {
            _locator = Locator.xpath("\"//div[contains(@class,'panel-heading')][text()='"+panelTitle+"']/following-sibling::div[@class='panel-body']\"");
            return this;
        }

        @Override
        protected Locator locator()
        {

            return _locator;
        }
    }

}
