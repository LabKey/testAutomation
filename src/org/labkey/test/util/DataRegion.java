/*
 * Copyright (c) 2017-2019 LabKey Corporation
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
package org.labkey.test.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import org.labkey.remoteapi.collections.CaseInsensitiveHashMap;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.ChartTypeDialog;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.html.BootstrapMenu;
import org.labkey.test.selenium.RefindingWebElement;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import static org.labkey.test.Locators.pageSignal;

public abstract class DataRegion extends WebDriverComponent<DataRegion.ElementCache> implements WebDriverWrapper.PageLoadListener
{
    public static final String UPDATE_SIGNAL = "dataRegionUpdate";
    public static final String PANEL_SHOW_SIGNAL = "dataRegionPanelShow";
    public static final String PANEL_HIDE_SIGNAL = "dataRegionPanelHide";
    protected static final int DEFAULT_WAIT_MS = 30000;

    private final WebDriverWrapper _webDriverWrapper;
    private final WebElement _el;

    // Set once when requested
    private String _regionName;

    // Cached items
    private String _tableId;

    // Settable
    private boolean _isAsync = false;
    private int _updateTimeout = DEFAULT_WAIT_MS;

    protected DataRegion(WebElement el, WebDriverWrapper driverWrapper)
    {
        _webDriverWrapper = driverWrapper;
        _el = el;

        if (_el instanceof RefindingWebElement)
        {
            ((RefindingWebElement) _el).
                    withRefindListener(element -> clearCache());
        }

        _webDriverWrapper.addPageLoadListener(this);
    }

    /**
     * @param regionName 'lk-region-name' of the table
     */
    protected DataRegion(String regionName, WebDriverWrapper driverWrapper)
    {
        this(Locators.form(regionName).refindWhenNeeded(driverWrapper.getDriver()).withTimeout(DEFAULT_WAIT_MS), driverWrapper);
        setRegionName(regionName);
    }

    @Override
    public WebElement getComponentElement()
    {
        return _el;
    }

    @Override
    public ElementCache elementCache()
    {
        getComponentElement().isEnabled(); // Trigger cache reset
        return super.elementCache();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    public DataRegionApi api()
    {
        return elementCache().dataRegionApi;
    }

    @Override
    public WebDriverWrapper getWrapper()
    {
        return _webDriverWrapper;
    }

    @Override
    public WebDriver getDriver()
    {
        return getWrapper().getDriver();
    }

    protected boolean isAsync()
    {
        return _isAsync;
    }

    protected void setRegionName(String regionName)
    {
        _regionName = regionName;
    }

    public void setAsync(boolean async)
    {
        _isAsync = async;
    }

    public int getUpdateTimeout()
    {
        return _updateTimeout;
    }

    public void setUpdateTimeout(int milliseconds)
    {
        _updateTimeout = milliseconds;
        if (_el instanceof RefindingWebElement)
        {
            ((RefindingWebElement) _el).withTimeout(milliseconds);
        }

    }

    @Override
    public void afterPageLoad()
    {
        clearCache();
    }

    protected void clearCache()
    {
        _tableId = null;
        clearElementCache();
    }

    /**
     * Triggered by clientapi/dom/DataRegion.js
     */
    protected String getUpdateSignal()
    {
        return UPDATE_SIGNAL + "-" + getDataRegionName();
    }

    public String doAndWaitForUpdate(Runnable run)
    {
        return getWrapper().doAndWaitForPageSignal(run, getUpdateSignal(), new WebDriverWait(getDriver(), Duration.ofMillis(getUpdateTimeout())));
    }

    public String getDataRegionName()
    {
        if (_regionName == null)
        {
            String regionName = StringUtils.trimToNull(getComponentElement().getAttribute("lk-region-form")); // new UI
            _regionName = regionName != null ? regionName : getComponentElement().getAttribute("lk-region-name"); // old UI
        }
        return _regionName;
    }

    protected String getTableId()
    {
        if (_tableId == null)
        {
            String id = _el.getAttribute("id");
            if (id.endsWith("-form"))
                _tableId = id.replace("-form", "");
            else
                _tableId = id;
        }
        return _tableId;
    }

    public List<WebElement> getHeaderButtons()
    {
        return elementCache().getAllHeaderButtons();
    }

    public WebElement getHeaderButton(String buttonText)
    {
        return elementCache().getHeaderButton(buttonText);
    }

    protected String replaceParameter(String param, String newValue)
    {
        URL url = getWrapper().getURL();
        String file = url.getFile();
        String encodedParam = EscapeUtil.encode(param);
        file = file.replaceAll("&" + Pattern.quote(encodedParam) + "=\\p{Alnum}+?", "");
        if (newValue != null)
            file += "&" + encodedParam + "=" + EscapeUtil.encode(newValue);

        try
        {
            url = new URL(url.getProtocol(), url.getHost(), url.getPort(), file);
        }
        catch (MalformedURLException mue)
        {
            throw new RuntimeException(mue);
        }
        return url.getFile();
    }

    @Deprecated
    public void clickHeaderButtonByText(String buttonText)
    {
        clickHeaderButton(buttonText);
    }

    public void clickHeaderButtonAndWait(String buttonText)
    {
        getWrapper().clickAndWait(elementCache().getHeaderButton(buttonText));
    }

    public void clickHeaderButton(String buttonText)
    {
        elementCache().getHeaderButton(buttonText).click();
    }

    public BootstrapMenu openHeaderMenu(String buttonText, String ... subMenuLabels)
    {
        BootstrapMenu headerMenu = elementCache().getHeaderMenu(buttonText);
        headerMenu.openMenuTo(subMenuLabels);
        return headerMenu;
    }

    public void clickHeaderMenu(String buttonText, String ... subMenuLabels)
    {
        clickHeaderMenu(buttonText, true, subMenuLabels);
    }

    public void clickHeaderMenu(String buttonText, boolean wait, String ... subMenuLabels)
    {
        elementCache().getHeaderMenu(buttonText).clickSubMenu(wait,  subMenuLabels);
    }

    public boolean hasHeaderMenu(String buttonText)
    {
        try
        {
            elementCache().getHeaderButton(buttonText);
            return true;
        }
        catch(NoSuchElementException e)
        {
            return false;
        }
    }

    public List<String> getHeaderMenuOptions(String buttonText)
    {
        BootstrapMenu menu = elementCache().getHeaderMenu(buttonText);
        menu.expand();
        return getWrapper().getTexts(menu.findVisibleMenuItems());
    }

    public void goToView(String... menuTexts)
    {
        getViewsMenu().clickSubMenu(true, menuTexts);
    }

    public void goToReport(String... menuTexts)
    {
        goToReport(true, menuTexts);
    }

    public void goToReport(boolean waitForRefresh, String... menuTexts)
    {
        clickReportMenu(waitForRefresh, menuTexts);
    }

    public void clickReportMenu(boolean waitForRefresh, String... menuTexts)
    {
        BootstrapMenu menu = getReportMenu();
        menu.clickSubMenu(waitForRefresh,  menuTexts);
    }

    public ChartTypeDialog createChart()
    {
        clickReportMenu(true, "Create Chart");
        return new ChartTypeDialog(getDriver());
    }

    @NotNull
    public BootstrapMenu getReportMenu()
    {
        return elementCache().getHeaderMenu("Charts / Reports");
    }

    @NotNull
    public BootstrapMenu getViewsMenu()
    {
        return elementCache().getHeaderMenu("Grid views");
    }

    protected static class Locators
    {
        public static Locator.XPathLocator form()
        {
            return Locator.tag("form").withAttribute("lk-region-form");
        }

        public static Locator.XPathLocator form(String regionName)
        {
            return Locator.tagWithAttribute("form", "lk-region-form", regionName);
        }
    }

    public class ElementCache extends Component.ElementCache
    {
        protected ElementCache()
        {
            getWrapper().waitForElement(pageSignal(getUpdateSignal()), getUpdateTimeout());
        }

        private final DataRegionApi dataRegionApi = new DataRegionApi();

        private final WebElement buttonBar = Locator.tagWithClass("*", "labkey-button-bar").findWhenNeeded(this);
        private final WebElement UX_ButtonBar = Locator.tagWithClass("div", "lk-region-bar").findWhenNeeded(this);

        public WebElement getButtonBar()
        {
            return UX_ButtonBar;
        }

        private List<WebElement> allHeaderButtons;
        private Map<String, WebElement> headerButtons;
        private Map<String, BootstrapMenu> headerMenus;

        protected List<WebElement> getAllHeaderButtons()
        {
            if (allHeaderButtons == null)
                allHeaderButtons = Collections.unmodifiableList(Locator.css("a.labkey-button, a.labkey-menu-button")
                        .findElements(UX_ButtonBar));
            return allHeaderButtons;
        }

        private final Map<String, String> alternateButtonTexts = Maps.of("Export", "Export / Sign Data");
        protected WebElement getHeaderButton(String text)
        {
            WebElement button = getHeaderButtonOrNull(text);
            if (button == null)
            {
                if (alternateButtonTexts.containsKey(text))
                    button = getHeaderButtonOrNull(alternateButtonTexts.get(text));
                if (button == null)
                    throw new NoSuchElementException("No header button: " + text);
            }
            return button;
        }

        protected WebElement getHeaderButtonOrNull(String text)
        {
            if (headerButtons == null)
                headerButtons = new CaseInsensitiveHashMap<>();

            if (!headerButtons.containsKey(text)) // This assumes buttons can't be added after data region is created
            {
                String title = "Grid Views".equals(text) ? "Grid views" : text;
                headerButtons.put(text, Locator.findAnyElementOrNull(
                        buttonBar,
                        Locator.lkButton().withAttribute("title", title),
                        Locator.lkButton(text),
                        Locator.tagWithAttribute("a", "data-original-title", title)));
            }
            return headerButtons.get(text);
        }

        protected BootstrapMenu getHeaderMenu(String text)
        {
            if (headerMenus == null)
                headerMenus = new TreeMap<>();

            if (!headerMenus.containsKey(text))
            {
                WebElement menuEl = Locator.findAnyElement(
                        "menu with data-original-title " + text,
                        buttonBar,
                        Locator.tagWithClassContaining("div", "lk-menu-drop")
                                .withChild(Locator.tagWithAttribute("a", "data-toggle", "dropdown").withText(text)),
                        Locator.tagWithClassContaining("div", "lk-menu-drop")
                                .withChild(Locator.tagWithAttribute("a", "data-original-title", text)),
                        Locator.tagWithClassContaining("div", "lk-menu-drop")
                                .withChild(Locator.tagWithAttribute("a", "title", text)));
                headerMenus.put(text, new BootstrapMenu(getDriver(), menuEl));
            }
            return headerMenus.get(text);
        }
    }

    private abstract class BaseDataRegionApi
    {
        final String regionJS = "LABKEY.DataRegions['" + getDataRegionName().replaceAll("'", "\\\\'") + "']";

        public void executeScript(String methodWithArgs, Object... args)
        {
            executeScript(methodWithArgs, null, args);
        }

        public <T> T executeScript(String methodWithArgs, Class<T> expectedResultType, Object... args)
        {
            return getWrapper().executeScript((expectedResultType != null ? "return " : "") + regionJS + "." + methodWithArgs, expectedResultType, args);
        }

        public void callMethod(String apiMethodName, Object... args)
        {
            StringBuilder argString = new StringBuilder();
            for (int i = 0; i < args.length; i++)
            {
                if (i > 0)
                    argString.append(", ");
                argString.append("arguments[").append(i).append("]");
            }
            String dataRegionMethod = apiMethodName + "(" + argString + ");";
            executeScript(dataRegionMethod, args);
        }
    }

    public class DataRegionApi extends BaseDataRegionApi
    {
        private final DataRegionApiExpectingRefresh refreshApi = new DataRegionApiExpectingRefresh();

        private DataRegionApi() { }

        public DataRegionApiExpectingRefresh expectingRefresh()
        {
            return refreshApi;
        }
    }

    public class DataRegionApiExpectingRefresh extends BaseDataRegionApi
    {
        private DataRegionApiExpectingRefresh() { }

        @Override
        public <T> T executeScript(String methodWithArgs, Class<T> expectedResultType, Object... args)
        {
            MutableObject<T> result = new MutableObject<>();
            doAndWaitForUpdate(() -> result.setValue(super.executeScript(methodWithArgs, expectedResultType, args)));
            return result.getValue();
        }
    }
}
