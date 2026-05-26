/*
 * Copyright (c) 2020-2026 LabKey Corporation
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
package org.labkey.test.components.ui.lineage;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

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

    private void showAllItems()
    {
        Locator.tag("li").child(Locator.byClass("lineage-link"))
                .findOptionalElement(this)
                .ifPresent(moreLink ->
                {
                    if (moreLink.getText().contains("more"))
                    {
                        moreLink.click();
                        getWrapper().shortWait().until(ExpectedConditions.textToBePresentInElement(moreLink, "less"));
                    }
                });
    }

    public NodeDetail getItem(String itemName)
    {
        showAllItems();
        return elementCache().itemNamed(itemName);
    }

    public NodeDetail getItemByTitle(String title)
    {
        showAllItems();
        return elementCache().itemWithTitle(title);
    }

    public List<NodeDetail> getItems()
    {
        showAllItems();
        return elementCache().items();
    }

    public List<String> getItemNames()
    {
        return getItems().stream().map(NodeDetail::getName).collect(Collectors.toList());
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

        NodeDetail itemNamed(String name)
        {
            return new NodeDetail.NodeDetailItemFinder(getDriver()).withName(name).find(this);
        }

        NodeDetail itemWithTitle(String title)
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
                return _baseLocator.withChild(Locator.tag("summary").withChild(Locator.tagContainingText("div", _title)));
            else
                return _baseLocator;
        }
    }
}
