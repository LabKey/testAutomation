package org.labkey.test.components.experiment;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.stream.Collectors;

public class NodeDetailGroup extends WebDriverComponent<NodeDetailGroup.ElementCache>
{
    final WebElement _el;
    final WebDriver _driver;
    final String _groupName;

    public NodeDetailGroup(WebElement element, String groupName, WebDriver driver)
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

    public NodeDetail getItem(String itemName)
    {
        return  elementCache().items().stream().filter(a-> a.getName().equals(itemName))
                .findFirst().orElseThrow();
    }

    public NodeDetail getItemByTitle(String title)
    {
        return elementCache().item(title);
    }

    public List<NodeDetail> getItems()
    {
        return elementCache().items();
    }

    public List<String> getItemNames()
    {
        return elementCache().items().stream().map(NodeDetail::getName).collect(Collectors.toList());
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

        List<NodeDetail> items()
        {
            return new NodeDetail.NodeDetailItemFinder(getDriver()).findAll(this);
        }

        NodeDetail item(String title)
        {
            return new NodeDetail.NodeDetailItemFinder(getDriver()).withTitle(title).find(this);
        }
    }

    public static class NodeDetailsFinder extends WebDriverComponentFinder<NodeDetailGroup, NodeDetailsFinder>
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
        protected NodeDetailGroup construct(WebElement el, WebDriver driver)
        {
            return new NodeDetailGroup(el, _title, driver);
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
