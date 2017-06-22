/*
 * Copyright (c) 2017 LabKey Corporation
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
package org.labkey.test.pages.core.admin.logger;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.components.html.Checkbox;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import java.util.ArrayList;
import java.util.List;

public class ManagerPage extends LabKeyPage<ManagerPage.ElementCache>
{
    public ManagerPage(WebDriver driver)
    {
        super(driver);
    }

    public static ManagerPage beginAt(WebDriverWrapper driver)
    {
        driver.beginAt(WebTestHelper.buildURL("logger", "manage"));
        return new ManagerPage(driver.getDriver());
    }

    public List<LoggerInfo> getLoggers()
    {
        List<LoggerInfo> loggers = new ArrayList<>();

        List<WebElement> rows = elementCache().loggerTable.findElements(By.xpath("//tr[contains(@class, 'logger-row')]"));
        for(WebElement row : rows)
        {
            LoggingLevel level = LoggingLevel.valueOf(row.getAttribute("data-level"));
            loggers.add(new LoggerInfo(row.getAttribute("data-name"), row.getAttribute("data-parent"), level));
        }

        return loggers;
    }

    public ManagerPage setLoggingLevel(String logger, LoggingLevel level)
    {
        WebElement row = elementCache().getLoggerRow(logger);

        scrollIntoView(row);

        row.findElement(By.xpath("./td[contains(@class, 'level-configured')]")).click();

        WebElement levelList = Locator.xpath("//input[@list='levelsList']").findElement(row);

        setFormElement(levelList, level.name());
        // Cause the blur event to happen which will set the value.
        fireEvent(levelList, SeleniumEvent.blur);

        return this;
    }

    public LoggingLevel getLoggingLevel(String logger)
    {
        return LoggingLevel.valueOf(elementCache().getLoggerRow(logger).getAttribute("data-level"));
    }

    public boolean isLoggerPresent(String logger)
    {
        try
        {
            return elementCache().getLoggerRow(logger).isDisplayed();
        }
        catch(NoSuchElementException nse)
        {
            return false;
        }
    }

    public ManagerPage clickRefresh()
    {
        elementCache().refresh.click();
        return this;
    }

    public ManagerPage clickReset()
    {
        elementCache().reset.click();
        return this;
    }

    public ManagerPage checkShowInherited()
    {
        elementCache().showInherited.check();
        return this;
    }

    public ManagerPage unCheckShowInherited()
    {
        elementCache().showInherited.uncheck();
        return this;
    }

    public ManagerPage setSearchText(String searchText)
    {
        setFormElement(elementCache().searchText, searchText);
        return this;
    }

    public String getSearchText()
    {
        return getFormElement(elementCache().searchText);
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        protected final WebElement searchText = Locator.tagWithId("input", "search").findWhenNeeded(this);

        protected final Checkbox showInherited = Checkbox.Checkbox(Locator.id("showInherited")).findWhenNeeded(this);

        protected final WebElement refresh = Locator.lkButton("Refresh").findWhenNeeded(this);

        protected final WebElement reset = Locator.lkButton("Reset").findWhenNeeded(this);

        protected final WebElement loggerTable = Locator.id("loggerTable").findWhenNeeded(this);

        protected  WebElement getLoggerRow(String logger)
        {
            try
            {
                return Locator.xpath("//tr[contains(@class, 'logger-row')][@data-name = '" + logger + "']").findElement(loggerTable);
            }
            catch(NoSuchElementException nse)
            {
                throw new NoSuchElementException("Could not find row for logger '" + logger + "'.", nse);
            }
        }
    }

    public class LoggerInfo
    {
        private String _name, _parent;
        LoggingLevel _level;

        public LoggerInfo(String name, String parent, LoggingLevel level)
        {
            _name = name;
            _parent = parent;
            _level = level;
        }

        public String getName()
        {
            return _name;
        }

        public String getParent()
        {
            return _parent;
        }

        public LoggingLevel getLevel()
        {
            return _level;
        }
    }

    public enum LoggingLevel
    {
        ALL,
        DEBUG,
        ERROR,
        FATAL,
        INFO,
        OFF,
        TRACE,
        WARN;
    }
}
