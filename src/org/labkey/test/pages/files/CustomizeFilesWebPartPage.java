/*
 * Copyright (c) 2017-2026 LabKey Corporation
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
package org.labkey.test.pages.files;

import org.labkey.test.Locator;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.selenium.LazyWebElement;
import org.labkey.test.util.FileBrowserHelper;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import static org.labkey.test.Locators.pageSignal;
import static org.labkey.test.util.FileBrowserHelper.FILE_LIST_SIGNAL_NAME;
import static org.labkey.test.util.FileBrowserHelper.encodeFileNodeIdPart;

/**
 * Created by susanh on 9/20/17.
 */
public class CustomizeFilesWebPartPage extends LabKeyPage<CustomizeFilesWebPartPage.ElementCache>
{
    public CustomizeFilesWebPartPage(WebDriver driver)
    {
        super(driver);
        waitForElement(pageSignal(FILE_LIST_SIGNAL_NAME));
    }

    public String getTitle()
    {
        return getFormElement(elementCache().title);
    }

    public CustomizeFilesWebPartPage setTitle(String title)
    {
        setFormElement(elementCache().title, title);
        return this;
    }

    public String getFileRoot()
    {
        Locator.XPathLocator selectedNodeLoc = Locator.xpath("//tr").withClass("x4-grid-row-selected").append("/td/div/span");
        waitForElement(selectedNodeLoc);
        return getText(selectedNodeLoc);
    }

    public void setFileRoot(String... nodeParts)
    {
        selectFileRoot(true, nodeParts);
        submit();
    }

    public CustomizeFilesWebPartPage selectFileRoot(boolean nodeExist, String... nodeParts)
    {
        if (isExtTreeNodeSelected(nodeParts[nodeParts.length - 1]))
            return this;

        StringBuilder nodeId = new StringBuilder();
        StringBuilder nodeWithParents = new StringBuilder();
        String separator = "";
        for (String node : nodeParts)
        {
            nodeWithParents.append(separator).append(node);
            separator = "/";

            nodeId.append(encodeFileNodeIdPart(node));
            if (!node.isEmpty())
            {
                nodeId.append('/');
            }
            WebElement nodeEl = Locators.fileRootTreeNode(nodeId.toString()).findWhenNeeded(getDriver());

            if (!nodeExist)
            {
                sleep(1000);
                if (!nodeEl.isDisplayed())
                    return this;
            }

            shortWait().until(ExpectedConditions.elementToBeClickable(nodeEl));
            Locator.XPathLocator selectedNode = Locator.xpath("//tr").withClass("x4-grid-row-selected").append("/td/div/span").withText(node);

            if (nodeEl.getDomAttribute("class").contains("selected"))
                continue; // already selected

            log("Selecting node " + nodeWithParents + " ...");
            waitForElementToDisappear(Locator.xpath("//tbody[starts-with(@id, 'treeview')]/tr[not(starts-with(@id, 'treeview'))]"));
            // select/expand tree node
            try
            {
                scrollIntoView(nodeEl);
            }
            catch (StaleElementReferenceException log)
            {
                log(log.getMessage());
            }

            doAndWaitForPageSignal(nodeEl::click, FILE_LIST_SIGNAL_NAME);
            waitFor(() -> nodeEl.getDomAttribute("class").contains("selected"), "Node note selected: " + nodeWithParents, 2000);
        }

        if (!nodeExist)
            throw new AssertionError("Node is present in file root tree but shouldn't be!");

        return this;
    }

    public void submit()
    {
        clickAndWait(elementCache().submitButton);
        new FileBrowserHelper(this).waitForFileGridReady();
    }

    public void verifyFileRootNodePresent(String... nodeParts)
    {
        selectFileRoot(true, nodeParts);
    }

    public void verifyFileRootNodeNotPresent(String... nodeParts)
    {
        selectFileRoot(false, nodeParts);
    }

    @Override
    protected ElementCache elementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage<ElementCache>.ElementCache
    {
        protected WebElement title = new LazyWebElement<>(Locator.tagWithName("input", "title"), this);
        protected WebElement submitButton = Locator.lkButton("Submit").findWhenNeeded(this);
    }

    public static class Locators
    {
        public static Locator.XPathLocator fileRootTreeNode(String nodeId)
        {
            Locator.XPathLocator fBrowser = Locator.tagWithClass("div", "fbrowser");
            return fBrowser.append(Locator.tag("tr").withPredicate("starts-with(@id, 'treeview')").attributeEndsWith("data-recordid", nodeId));
        }
    }
}
