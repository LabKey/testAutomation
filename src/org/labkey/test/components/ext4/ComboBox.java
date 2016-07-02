package org.labkey.test.components.ext4;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebDriverWrapperImpl;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.util.Ext4Helper.TextMatchTechnique;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

import static org.labkey.test.util.Ext4Helper.Locators.comboListItem;
import static org.labkey.test.util.Ext4Helper.TextMatchTechnique.EXACT;
import static org.labkey.test.util.Ext4Helper.getCssPrefix;

public class ComboBox extends WebDriverComponent<ComboBox.ElementCache>
{
    WebElement _formItem;
    WebDriverWrapper _driverWrapper;

    private ComboBox(WebElement formItem, WebDriver driver)
    {
        _formItem = formItem;
        _driverWrapper = new WebDriverWrapperImpl(driver);
    }

    @Override
    public WebElement getComponentElement()
    {
        return _formItem;
    }

    @Override
    protected WebDriver getDriver()
    {
        return _driverWrapper.getDriver();
    }

    public WebDriverWrapper getDriverWrapper()
    {
        return _driverWrapper;
    }

    @LogMethod(quiet = true)
    public void selectComboBoxItem(@LoggedParam String... selections)
    {
        selectComboBoxItem(EXACT, selections);
    }

    @LogMethod(quiet = true)
    public void selectComboBoxItem(TextMatchTechnique matchTechnique, @LoggedParam String... selections)
    {
        openComboList();

        try
        {
            for (String selection : selections)
            {
                selectItemFromOpenComboList(selection, matchTechnique);
            }
        }
        catch (StaleElementReferenceException retry) // Combo-box might still be loading previous selection (no good way to detect)
        {
            for (String selection : selections)
            {
                selectItemFromOpenComboList(selection, matchTechnique);
            }
        }

        closeComboList();
    }

    public void openComboList()
    {
        elementCache().arrowTrigger.click();

        try
        {
            WebDriverWrapper.waitFor(() -> getComponentElement().getAttribute("class").contains("pickerfield-open"), 1000);
        }
        catch (TimeoutException retry)
        {
            elementCache().arrowTrigger.click(); // try again if combo-box doesn't open
        }

        getDriverWrapper().waitForElement(comboListItem());
    }

    public void selectItemFromOpenComboList(String itemText, TextMatchTechnique matchTechnique, boolean clickAt)
    {
        selectItemFromOpenComboList(getDriverWrapper(), itemText, matchTechnique, clickAt);
    }

    public static void selectItemFromOpenComboList(WebDriverWrapper driverWrapper, String itemText, TextMatchTechnique matchTechnique, boolean clickAt)
    {
        Locator.XPathLocator listItem = comboListItem();

        switch (matchTechnique)
        {
            case EXACT:
                listItem = listItem.withText(itemText);
                break;
            case CONTAINS:
                listItem = listItem.containing(itemText);
                break;
            case STARTS_WITH:
                listItem = listItem.startsWith(itemText);
                break;
            case REGEX:
                listItem = listItem.withTextMatching(itemText);
                break;
        }

        WebElement element = listItem.waitForElement(driverWrapper.getDriver(), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        boolean elementAlreadySelected = element.getAttribute("class").contains("selected");
        if (!isOpenComboBoxMultiSelect(driverWrapper.getDriver()) || !elementAlreadySelected)
        {
            driverWrapper.scrollIntoView(element); // Workaround: Auto-scrolling in chrome isn't working well
            if(clickAt)
                driverWrapper.clickAt(listItem, 0, 0, 0);
            else
                driverWrapper.click(listItem);
        }
    }

    public void selectItemFromOpenComboList(String itemText, TextMatchTechnique matchTechnique)
    {
        selectItemFromOpenComboList(itemText, matchTechnique,false);
    }

    public static void selectItemFromOpenComboList(WebDriverWrapper driverWrapper, String itemText, TextMatchTechnique matchTechnique)
    {
        selectItemFromOpenComboList(driverWrapper, itemText, matchTechnique,false);
    }

    private boolean isOpenComboBoxMultiSelect()
    {
        return isOpenComboBoxMultiSelect(getDriver());
    }

    private static boolean isOpenComboBoxMultiSelect(WebDriver driver)
    {
        return !comboListItem().append("/span").withClass(getCssPrefix() + "combo-checker").findElements(driver).isEmpty();
    }

    public void closeComboList()
    {
        // close combo manually if it is a multi-select combo-box
        if (isOpenComboBoxMultiSelect())
            elementCache().arrowTrigger.click();

        // menu should disappear
        getDriverWrapper().waitForElementToDisappear(comboListItem());
    }

    public void clearComboBox()
    {
        openComboList();

        try
        {
            for (WebElement element : comboListItem().findElements(getDriver()))
            {
                boolean elementAlreadySelected = element.getAttribute("class").contains("selected");
                if (isOpenComboBoxMultiSelect() && elementAlreadySelected)
                    element.click();
            }
        }
        catch (StaleElementReferenceException retry) // Combo-box might still be loading previous selection (no good way to detect)
        {
            for (WebElement element : comboListItem().findElements(getDriver()))
            {
                boolean elementAlreadySelected = element.getAttribute("class").contains("selected");
                if (isOpenComboBoxMultiSelect() && elementAlreadySelected)
                    element.click();
            }
        }

        closeComboList();
    }

    @LogMethod(quiet=true)
    public List<String> getComboBoxOptions()
    {
        openComboList();
        return getDriverWrapper().getTexts(comboListItem().findElements(getDriver()));
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component.ElementCache
    {
        protected WebElement arrowTrigger = Locator.xpath("//div[contains(@class,'arrow')]").findWhenNeeded(this);
    }

    public static ComboBoxFinder ComboBox(WebDriver driver)
    {
        return new ComboBoxFinder(driver);
    }

    public static class ComboBoxFinder extends FormItemFinder<ComboBox, ComboBoxFinder>
    {
        WebDriver _driver;

        public ComboBoxFinder(WebDriver driver)
        {
            super();
            _driver = driver;
        }

        @Override
        protected Locator.XPathLocator itemLoc()
        {
            return Locator.tagWithClass("td", getCssPrefix() + "form-item-body").attributeStartsWith("id", "combobox");
        }

        @Override
        protected ComboBox construct(WebElement el)
        {
            return new ComboBox(el, _driver);
        }
    }
}
