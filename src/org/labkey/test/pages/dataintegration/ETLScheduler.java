/*
 * Copyright (c) 2015-2017 LabKey Corporation
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
package org.labkey.test.pages.dataintegration;

import org.apache.commons.lang3.NotImplementedException;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.Component;
import org.labkey.test.components.html.BootstrapMenu;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.selenium.LazyWebElement;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.LogMethod;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.HashMap;
import java.util.Map;

public class ETLScheduler extends LabKeyPage<ETLScheduler.Elements>
{
    public ETLScheduler(BaseWebDriverTest test)
    {
        super(test);
    }

    public static ETLScheduler beginAt(BaseWebDriverTest test)
    {
        return beginAt(test, test.getCurrentContainerPath());
    }

    public static ETLScheduler beginAt(BaseWebDriverTest test, String containerPath)
    {
        test.beginAt(WebTestHelper.buildURL("dataintegration", containerPath, "begin"));
        return new ETLScheduler(test);
    }

    public TransformRow transform(String transformId)
    {
        return elementCache().findTransformRow(transformId);
    }

    public LabKeyPage viewProcessedJobs()
    {
        _test.clickAndWait(elementCache().viewProcessedJobsButton);
        return new LabKeyPage(_test);
    }

    public int rowCount()
    {
        return Locator.xpath("//tr[@transformId]").findElements(getDriver()).size();
    }

    @Override
    protected Elements elementCache()
    {
        return new Elements();
    }

    protected class Elements extends LabKeyPage<?>.ElementCache
    {
        private Map<String, TransformRow> transformRows = new HashMap<>();

        protected TransformRow findTransformRow(String transformId)
        {
            if (!transformRows.containsKey(transformId))
                transformRows.put(transformId, new TransformRow(new LazyWebElement(Locator.css("tr[transformid=\"" + transformId + "\"]"), this)));
            return transformRows.get(transformId);
        }
        protected WebElement viewProcessedJobsButton = new LazyWebElement(Locator.lkButton("View Processed Jobs"), this);
    }

    public class TransformRow extends Component<TransformRow.RowElements>
    {
        WebElement componentElement;

        protected TransformRow(WebElement context)
        {
            this.componentElement = context;
        }

        @Override
        public WebElement getComponentElement()
        {
            return componentElement;
        }

        public String getName()
        {
            return elementCache().name.getText();
        }

        public String getSourceModule()
        {
            return elementCache().sourceModule.getText();
        }

        public boolean isEnabled()
        {
            return elementCache().enabledCheckbox.isSelected();
        }

        public TransformRow setEnabled(boolean enable)
        {
            _test.setCheckbox(elementCache().enabledCheckbox, enable);
            return this;
        }

//        public boolean isVerboseLoggingEnabled()
//        {
//            return elements().verboseLoggingCheckbox.isSelected();
//        }
//
//        public TransformRow setVerboseLoggingEnabled(boolean enable)
//        {
//            _test.setCheckbox(elements().verboseLoggingCheckbox, enable);
//            return this;
//        }

        public String getSchedule()
        {
            return elementCache().schedule.getText();
        }

        public boolean enabledIconDisplayed()
        {
            return elementCache().enabledIcon.isDisplayed();
        }
        public String getTooltipText()
        {
            if (elementCache().enabledIcon.isDisplayed())
            {
                return elementCache().enabledIcon.getAttribute("title");
            }

            return null;
        }
        public String getEnabledBy()
        {
            return elementCache().enabledBy.getText();
        }

        public String getLastStatus()
        {
            return elementCache().lastStatus.getText();
        }

        public LabKeyPage clickLastStatus()
        {
            _test.clickAndWait(elementCache().lastStatus.findElement(By.cssSelector("a")));

            return new LabKeyPage(_test);
        }

        public String getLastRun()
        {
            return elementCache().lastRun.getText();
        }

        public LabKeyPage clickLastRun()
        {
            _test.clickAndWait(elementCache().lastRun.findElement(By.cssSelector("a")));

            return new LabKeyPage(_test);
        }

        public String getLastChecked()
        {
            return elementCache().lastChecked.getText();
        }

        public LabKeyPage runNow()
        {
            _test.clickAndWait(elementCache().runNowButton);

            return new LabKeyPage(_test);
        }

        public LabKeyPage reset()
        {
            new BootstrapMenu(getDriver(), Locator.tagWithClassContaining("div", "lk-menu-drop")
                            .withChild(Locator.lkButton("Reset State...")).findElement(this))
                    .clickSubMenu(true, "Reset");

            return new LabKeyPage(_test);
        }

        @LogMethod(quiet = true)
        public Confirm truncateAndReset()
        {
            new BootstrapMenu(getDriver(), Locator.tagWithClassContaining("div", "lk-menu-drop")
                        .withChild(Locator.lkButton("Reset State...")).findElement(this))
                    .clickSubMenu(false, "Truncate and Reset");

            return new Confirm();
        }

        @Override
        protected RowElements newElementCache()
        {
            return new RowElements();
        }

        public class Confirm
        {
            public Confirm()
            {
                _test.waitForElement(Ext4Helper.Locators.window("Confirm"));
            }

            public Confirm assertMessageContains()
            {
                throw new NotImplementedException("");
            }

            public ETLScheduler confirmNo()
            {
                Locator.XPathLocator noButton = Ext4Helper.Locators.windowButton("Confirm", "No");
                _test.click(noButton);
                _test.waitForElementToDisappear(noButton);

                return ETLScheduler.this;
            }

            public ETLScheduler confirmYes()
            {
                _test.click(Ext4Helper.Locators.windowButton("Confirm", "Yes"));
                _test.waitForElement(Ext4Helper.Locators.window("Success"));
                _test.click(Ext4Helper.Locators.windowButton("Success", "OK"));
                _test.waitForElementToDisappear(Ext4Helper.Locators.window("Success"));

                return ETLScheduler.this;
            }
        }

        private class RowElements extends Component<?>.ElementCache
        {
            // Column numbers
            private final int NAME = 1;
            private final int SOURCE_MODULE = 2;
            private final int SCHEDULE = 3;
            private final int ENABLED = 4;
//            private final int VERBOSE_LOGGING = 5;
            private final int ENABLED_BY = 5;
            private final int LAST_STATUS = 6;
            private final int LAST_RUN = 7;
            private final int LAST_CHECKED = 8;

            WebElement name = new LazyWebElement(Locator.css("td:nth-of-type(" + NAME + ")"), this);
            WebElement sourceModule = new LazyWebElement(Locator.css("td:nth-of-type(" + SOURCE_MODULE + ")"), this);
            WebElement schedule = new LazyWebElement(Locator.css("td:nth-of-type(" + SCHEDULE + ")"), this);
            WebElement enabledCheckbox = new LazyWebElement(Locator.css("td:nth-of-type(" + ENABLED + ") input[type=checkbox]"), this);
            WebElement enabledIcon = new LazyWebElement(Locator.byClass("fa fa-info-circle"), this);
            WebElement lastStatus = new LazyWebElement(Locator.css("td:nth-of-type(" + LAST_STATUS + ")"), this);
            WebElement enabledBy = new LazyWebElement(Locator.css("td:nth-of-type(" + ENABLED_BY + ")"), this);
            WebElement lastRun = new LazyWebElement(Locator.css("td:nth-of-type(" + LAST_RUN + ")"), this);
            WebElement lastChecked = new LazyWebElement(Locator.css("td:nth-of-type(" + LAST_CHECKED + ")"), this);

            WebElement runNowButton = new LazyWebElement(Locator.lkButton("Run Now"), this);
            WebElement resetStateButton = new LazyWebElement(Locator.lkButton("Reset State..."), this);
        }
    }
}
