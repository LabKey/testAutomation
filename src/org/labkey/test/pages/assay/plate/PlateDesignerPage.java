package org.labkey.test.pages.assay.plate;

import org.jetbrains.annotations.NotNull;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility page class to create plate templates
 */
public class PlateDesignerPage extends LabKeyPage<PlateDesignerPage.ElementCache>
{
    public PlateDesignerPage(WebDriver driver)
    {
        super(driver);
    }

    public static PlateDesignerPage beginAt(WebDriverWrapper webDriverWrapper, PlateDesignerParams params)
    {
        return beginAt(webDriverWrapper, webDriverWrapper.getCurrentContainerPath(), params);
    }

    public static PlateDesignerPage beginAt(WebDriverWrapper webDriverWrapper, String containerPath, PlateDesignerParams params)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("plate", containerPath, "designer", params.toUrlParams()));
        return new PlateDesignerPage(webDriverWrapper.getDriver());
    }

    public void createWellGroup(String type, String name)
    {
        selectTypeTab(type);

        WebElement nameField = Locator.tagWithName("input", "wellGroupName")
                .withAttribute("data-type", type)
                .findElement(getDriver());
        setFormElement(nameField, name);
        fireEvent(nameField, SeleniumEvent.change);
        clickButton("Create", 0);
        waitForElement(Locator.tagContainingText("label", name));
    }

    public void selectTypeTab(String name)
    {
        Locator.tagWithClass("div", "gwt-Label").withText(name).waitForElement(getDriver(), WAIT_FOR_JAVASCRIPT).click();
    }

    public void selectWellsForWellgroup(String type, String wellGroup, String startLocation, String endLocation)
    {
        selectTypeTab(type);
        waitForElement(Locator.tagWithText("label", wellGroup));

        Locator start = Locator.css(".Cell-"+startLocation);
        Locator end = Locator.css(".Cell-"+endLocation);
        if (wellGroup != null & !"".equals(wellGroup))
        {
            if (!getText(Locator.css(".gwt-TabBarItem-selected")).equals(type))
            {
                Locator.css(".gwt-Label").withText(type).findElement(getDriver()).click();
                //want for switch
            }
            if (!isChecked(Locator.xpath("//input[@name='wellGroup' and following-sibling::label[text()='"+wellGroup+"']]")))
                click(Locator.xpath("//input[@name='wellGroup' and following-sibling::label[text()='"+wellGroup+"']]"));
            if (!getAttribute(start, "style").contains("rgb(255, 255, 255)"))
                click(start);
        }
        else
        {
            Locator.tagWithClass("*", "gwt-Label").withText(type).findElement(getDriver()).click();
            //select no group in order to clear area
        }
        WebElement fromEl = start.findElement(getDriver());
        WebElement toEl = end.findElement(getDriver());

        Actions builder = new Actions(getDriver());
        builder.clickAndHold(fromEl).moveToElement(toEl).release().build().perform();
    }

    public void setName(String name)
    {
        Locator nameField = Locator.id("templateName");
        waitForElement(nameField, WAIT_FOR_JAVASCRIPT);
        setFormElement(nameField, name);
        fireEvent(nameField, SeleniumEvent.change);
    }

    public void saveAndClose()
    {
        clickButton("Save & Close");
    }

    public void save()
    {
        clickButton("Save", 0);
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
    }

    public static class PlateDesignerParams
    {
        private final Integer rowCount;
        private final Integer colCount;
        private String assayType = "blank";
        private String templateType;

        public PlateDesignerParams(int rowCount, int colCount)
        {
            this.rowCount = rowCount;
            this.colCount = colCount;
        }

        public PlateDesignerParams setAssayType(@NotNull String assayType)
        {
            this.assayType = assayType;
            return this;
        }

        public PlateDesignerParams setTemplateType(@NotNull String templateType)
        {
            this.templateType = templateType;
            return this;
        }

        Map<String, String> toUrlParams()
        {
            Map<String, String> params = new HashMap<>();
            params.put("rowCount", rowCount.toString());
            params.put("colCount", colCount.toString());
            params.put("assayType", assayType);
            if (templateType != null)
            {
                params.put("templateType", templateType);
            }

            return params;
        }

        public String templateListOption()
        {
            StringBuilder sb = new StringBuilder();
            sb.append("new ")
                    .append(rowCount * colCount)
                    .append(" well (")
                    .append(rowCount)
                    .append("x")
                    .append(colCount)
                    .append(") ")
                    .append(assayType);
            if (templateType != null)
            {
                sb.append(" ").append(templateType);
            }
            sb.append(" template");
            return sb.toString();
        }

        public static PlateDesignerParams _96well()
        {
            return new PlateDesignerParams(8, 12);
        }

        public static PlateDesignerParams _384well()
        {
            return new PlateDesignerParams(16, 24);
        }
    }
}
