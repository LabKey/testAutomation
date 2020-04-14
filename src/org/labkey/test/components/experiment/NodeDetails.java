package org.labkey.test.components.experiment;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.stream.Collectors;

public class NodeDetails extends WebDriverComponent<NodeDetails.ElementCache>
{
    final WebElement _el;
    final WebDriver _driver;
    final String _groupName;

    public NodeDetails(WebElement element, String groupName, WebDriver driver)
    {
        _el = element;
        _driver = driver;
        _groupName = groupName;
    }

    public String getGroupName()
    {
        if (_groupName!=null)
            return _groupName;
        else
            return elementCache().summary.getText();
    }

    public NodeDetailItem getItem(String itemName)
    {
        return  elementCache().item(itemName);
    }

    public List<NodeDetailItem> getItems()
    {
        return elementCache().items();
    }

    public List<String> getItemNames()
    {
        return elementCache().items().stream().map(NodeDetailItem::getName).collect(Collectors.toList());
    }

    @Override
    public WebElement getComponentElement()
    {
        return _el;
    }

    @Override
    public WebDriver getDriver()
    {
        return _driver;
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }


    protected class ElementCache extends Component<?>.ElementCache
    {
        WebElement summary = Locator.tagWithClass("summary", "lineage-name")
                .findElement(this);

        NodeDetailItem item(String itemName)
        {
            return new NodeDetailItem.NodeDetailItemFinder(getDriver()).withName(itemName)
                    .find(this);
        }

        List<NodeDetailItem> items()
        {
            return new NodeDetailItem.NodeDetailItemFinder(getDriver()).findAll(this);
        }
    }

    public static class NodeDetailsFinder extends WebDriverComponentFinder<NodeDetails, NodeDetailsFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tag("details")
                .withChild(Locator.tagWithClass("summary", "lineage-name"));
        private String _title = null;

        public NodeDetailsFinder(WebDriver driver)
        {
            super(driver);
        }

        public NodeDetailsFinder withTitle(String title)
        {
            _title = title;
            return this;
        }

        @Override
        protected NodeDetails construct(WebElement el, WebDriver driver)
        {
            return new NodeDetails(el, _title, driver);
        }

        @Override
        protected Locator locator()
        {
            if (_title != null)
                return _baseLocator.withChild(Locator.tag("summary").withChild(Locator.tagContainingText("h6", _title)));
            else
                return _baseLocator;
        }
    }
}
