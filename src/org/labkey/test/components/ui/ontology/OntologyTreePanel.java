/*
 * Copyright (c) 2021-2026 LabKey Corporation
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
package org.labkey.test.components.ui.ontology;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.react.BaseReactSelect;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class OntologyTreePanel extends WebDriverComponent<OntologyTreePanel.ElementCache>
{
    private final WebElement _el;
    private final WebDriver _driver;

    protected OntologyTreePanel(WebElement element, WebDriver driver)
    {
        _el = element;
        _driver = driver;
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

    public TreeNode getRootNode()
    {
        return elementCache().rootElement;
    }

    public TreeNode getVisibleActiveNode()
    {
        return new TreeNode.TreeNodeFinder(getDriver()).activeOnly().waitFor(this);
    }

    public List<TreeNode> getFilteringNodes()
    {
        return new TreeNode.TreeNodeFinder(getDriver()).withSelectedFilter().findAll(this);
    }

    public TreeNode openToPath(List<String> nodes)
    {
        var currentNode = getRootNode();
        assertThat(currentNode.getTitle(), is(nodes.getFirst()));

        for (int i=1; i < nodes.size(); i++)
        {
            currentNode = currentNode.getChild(nodes.get(i));
            currentNode.select();
        }
        return currentNode;
    }

    public OntologyTreePanel waitForActiveNode()
    {
        getWrapper().waitForElementToDisappear(elementCache().loadingSpinner);
        getWrapper().waitForElement(elementCache().activeNode);
        return this;
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        WebElement nodeContainer = Locator.tag("ul").parent().findWhenNeeded(this);
        Locator activeNode = Locator.tagWithClass("div", "filetree-node-active");
        Locator loadingSpinner = BaseReactSelect.Locators.loadingSpinner;
        TreeNode rootElement = new TreeNode.TreeNodeFinder(getDriver()).waitFor(nodeContainer);
    }

    public static class OntologyTreePanelFinder extends WebDriverComponentFinder<OntologyTreePanel, OntologyTreePanelFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClass("div", "filetree-container");

        public OntologyTreePanelFinder(WebDriver driver)
        {
            super(driver);
        }

        @Override
        protected OntologyTreePanel construct(WebElement el, WebDriver driver)
        {
            return new OntologyTreePanel(el, driver);
        }

        @Override
        protected Locator locator()
        {
            return _baseLocator;
        }
    }
}
