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
package org.labkey.test.pages.flow.reports;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.Input;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.ExtHelper;
import org.labkey.test.util.Maps;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.Arrays;
import java.util.List;

import static org.labkey.test.components.html.Input.Input;

public abstract class ReportEditorPage<Page extends ReportEditorPage> extends LabKeyPage<ReportEditorPage.ElementCache>
{
    public ReportEditorPage(WebDriver driver)
    {
        super(driver);
    }

    protected static void beginCreate(WebDriverWrapper driver, String containerPath, String reportType)
    {
        driver.beginAt(WebTestHelper.buildURL("flow-reports", containerPath, "create", Maps.of("reportType", reportType)));
    }

    protected static void beginEdit(WebDriverWrapper driver, String containerPath, String reportType, String reportId)
    {
        driver.beginAt(WebTestHelper.buildURL("flow-reports", containerPath, "create", Maps.of("reportType", reportType, "reportId", reportId)));
    }

    public Page setName(String name)
    {
        elementCache().name.set(name);
        return (Page) this;
    }

    public Page setDescription(String description)
    {
        elementCache().description.set(description);
        return (Page) this;
    }

    public Page setSubset(String subset)
    {
        elementCache().subsetPickerTrigger.click();
        selectFromStatisticPicker(subset);
        return (Page) this;
    }

    public Page addKeywordFilter(String property, String value)
    {
        elementCache().addKeyworkFilter.click();
        final List<WebElement> filterRows = elementCache().findFilterRows(FilterType.Keyword);
        WebElement newFilter = filterRows.get(filterRows.size() - 1);
        final WebElement propertyCombo = Locator.tag("input").attributeEndsWith("name", ".property").parent().findElement(newFilter);
        final Input valueInput = Input(Locator.tag("input").attributeEndsWith("name", ".value"), getDriver()).find(newFilter);
        _extHelper.selectComboBoxItem(propertyCombo, property);
        valueInput.set(value);
        return (Page) this;
    }

    public Page addSampleFilter(String property, String value)
    {
        elementCache().addSampleFilter.click();
        final List<WebElement> filterRows = elementCache().findFilterRows(FilterType.SampleProperty);
        WebElement newFilter = filterRows.get(filterRows.size() - 1);
        final WebElement propertyCombo = Locator.tag("input").attributeEndsWith("name", ".property").parent().findElement(newFilter);
        final Input valueInput = Input(Locator.tag("input").attributeEndsWith("name", ".value"), getDriver()).find(newFilter);
        _extHelper.selectComboBoxItem(propertyCombo, property);
        valueInput.set(value);
        return (Page) this;
    }

    public Page addStatisticFilter(String subset, String stat, String operator, String value)
    {
        elementCache().addStatisticFilter.click();
        final List<WebElement> filterRows = elementCache().findFilterRows(FilterType.Statistic);
        WebElement newFilter = filterRows.get(filterRows.size() - 1);
        final Locator.XPathLocator subsetInput = Locator.tag("input").attributeEndsWith("name", ".property_subset");
        final WebElement statCombo = Locator.tag("input").attributeEndsWith("name", ".property_stat").parent().findElement(newFilter);
        final WebElement opCombo = Locator.tag("input").attributeEndsWith("name", ".op").parent().findElement(newFilter);
        final Input valueInput = Input(Locator.tag("input").attributeEndsWith("name", ".value"), getDriver()).find(newFilter);
        subsetInput.followingSibling("img").withClass("x-form-trigger").findElement(newFilter).click();
        selectFromStatisticPicker(subset);
        fireEvent(subsetInput, SeleniumEvent.blur);
        _extHelper.selectComboBoxItem(statCombo, stat);
        _extHelper.selectComboBoxItem(opCombo, operator);
        valueInput.set(value);
        return (Page) this;
    }

    public Page addFieldFilter(String property, String operator, String value)
    {
        elementCache().addFieldFilter.click();
        final List<WebElement> filterRows = elementCache().findFilterRows(FilterType.Field);
        WebElement newFilter = filterRows.get(filterRows.size() - 1);
        final Input propertyInput = Input(Locator.tag("input").attributeEndsWith("name", ".property"), getDriver()).find(newFilter);
        final WebElement opCombo = Locator.tag("input").attributeEndsWith("name", ".op").parent().findElement(newFilter);
        final Input valueInput = Input(Locator.tag("input").attributeEndsWith("name", ".value"), getDriver()).find(newFilter);
        propertyInput.set(property);
        _extHelper.selectComboBoxItem(opCombo, operator);
        valueInput.set(value);
        return (Page) this;
    }

    public Page setFolderFilter(String folder)
    {
        _extHelper.selectComboBoxItem(elementCache().analysisFolderCombo, folder);
        return (Page) this;
    }

    public Page setStartDateFilter(String startDate)
    {
        elementCache().startDateInput.set(startDate);
        return (Page) this;
    }

    public Page setEndDateFilter(String endDate)
    {
        elementCache().endDateInput.set(endDate);
        return (Page) this;
    }

    public LabKeyPage save()
    {
        clickAndWait(elementCache().saveButton);
        return new LabKeyPage(getDriver());
    }

    public LabKeyPage cancel()
    {
        clickAndWait(elementCache().cancelButton);
        return new LabKeyPage(getDriver());
    }

    public LabKeyPage delete()
    {
        clickAndWait(elementCache().deleteButton);
        return new LabKeyPage(getDriver());
    }

    private void selectFromStatisticPicker(String subset)
    {
        final WebElement window = ExtHelper.Locators.window("Statistic Picker").waitForElement(getDriver(), 10000);
        //x-tree-node-el
        final WebElement subsetEl = Locator.tagWithAttribute("div", "subset", subset).findElement(window);
        subsetEl.click();
        shortWait().until(ExpectedConditions.invisibilityOfAllElements(Arrays.asList(window)));
    }

    protected abstract Locator.XPathLocator getSubsetInput();

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        final Input name = Input(Locator.name("reportName"), getDriver()).findWhenNeeded(this);
        final Input description = Input(Locator.name("reportDescription"), getDriver()).findWhenNeeded(this);
        final WebElement subsetPickerTrigger = getSubsetInput().followingSibling("img").withClass("x-form-trigger").findWhenNeeded(this);
        final WebElement addKeyworkFilter = Locator.linkWithText("Add Keyword Filter").findWhenNeeded(this);
        final WebElement addSampleFilter = Locator.linkWithText("Add Sample Filter").findWhenNeeded(this);
        final WebElement addStatisticFilter = Locator.linkWithText("Add Statistic Filter").findWhenNeeded(this);
        final WebElement addFieldFilter = Locator.linkWithText("Add Field Filter").findWhenNeeded(this);
        final WebElement analysisFolderCombo = Locator.input("filter[5].value").parent().findWhenNeeded(this);
        final Input startDateInput = Input(Locator.input("filter[6].value"), getDriver()).findWhenNeeded(this);
        final Input endDateInput = Input(Locator.input("filter[7].value"), getDriver()).findWhenNeeded(this);
        final WebElement filtersFieldSet = Locator.id("filtersFieldSet").findWhenNeeded(this);

        protected List<WebElement> findFilterRows(FilterType label)
        {
            Locator.XPathLocator rowLoc = Locator.tagWithClass("div", "x-form-item").withChild(Locator.tag("label").withText(label + ":"));
            if (label == FilterType.Statistic)
                rowLoc = rowLoc.parent(); // Statistic filter is actually two rows; this will get both of them.
            return rowLoc.findElements(filtersFieldSet);
        }

        final WebElement saveButton = Locator.button("Save").findWhenNeeded(this);
        final WebElement cancelButton = Locator.button("Cancel").findWhenNeeded(this);
        final WebElement deleteButton = Locator.button("Delete").findWhenNeeded(this);

    }

    protected enum FilterType
    {
        Keyword,
        SampleProperty
                {
                    @Override
                    public String getLabel()
                    {
                        return "Sample Property";
                    }
                },
        Statistic,
        Field;

        public String getLabel()
        {
            return name();
        }
    }
}