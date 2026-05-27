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
package org.labkey.test.components.ui.grids;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.ui.files.AttachmentCard;
import org.labkey.test.components.ui.files.ImageFileViewDialog;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.util.List;
import java.util.Optional;


/**
 * This wraps DetailPanelHeader.tsx
 */
public class DetailDataPanel extends WebDriverComponent<DetailDataPanel.ElementCache>
{
    private final WebElement _el;
    private final WebDriver _driver;
    private String _title;

    protected DetailDataPanel(WebElement element, WebDriver driver)
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

    public String getTitle()
    {
        if (_title == null)
            _title = elementCache().headingText.getText();
        return _title;
    }

    /**
     * When the logged-in user does not have edit permissions, the edit button will not be present
     */
    public boolean isEditable()
    {
        return elementCache().editButton().isPresent();
    }

    /**
     * If this is an aliquot sample clicking the edit button will only allow the user to edit the description property.
     * No other fields are editable.
     *
     * @return A {@link DetailTableEdit}
     */
    public DetailTableEdit clickEdit()
    {
        String title = getTitle();
        if (!isEditable())
            throw new IllegalStateException("The current pane (with title ["+title+"]) is not editable");

        elementCache().editButton().get().click();

        return new DetailTableEdit.DetailTableEditFinder(getDriver())
                .withTitle("Editing " + title).waitFor();
    }

    /**
     * Get the table that has the details fields (i.e. column data) for this item. If this is an aliquot sample this
     * table will have the fields from the parent sample.
     *
     * @return A {@link DetailTable}.
     */
    public DetailTable getTable()
    {
        List<DetailTable> tables = elementCache().detailTables();

        // As of release 21.05 the table with the column values for the sample is the last table in the panel.
        return  tables.getLast();
    }

    /**
     * If this is an aliquot sample there will be multiple tables in the panel, the first table will contain the aliquot
     * information (under the 'Aliquot Data' header). This will return that table.
     *
     * @return A {@link DetailTable}.
     */
    public DetailTable getAliquotTable()
    {
        List<DetailTable> tables = elementCache().detailTables();

        if(tables.size() == 1)
            throw new NoSuchElementException("There does not appear to be an 'Aliquot' table in this panel.");

        // As of release 21.05 if this sample is an aliquot, then the table with the aliquot information is the first table in the panel.
        return tables.getFirst();
    }

    /**
     * If this a sub-aliquot sample there will be multiple tables in the "Original Sample Details".
     * The first contains just the "Original Sample" field.
     * The second contains the rest oof the original sample metadata.
     *
     * @return A {@link DetailTable}.
     */
    public DetailTable getSubAliquotRootSampleFieldTable()
    {
        List<DetailTable> tables = elementCache().detailTables();

        if(tables.size() != 2)
            throw new NoSuchElementException("This does not appear to be a sub-aliquot.");

        return tables.getFirst();
    }

    public AttachmentCard getFileField(String fieldLabel)
    {
        return elementCache().fileField(fieldLabel);
    }

    public String getFileName(String fieldLabel)
    {
        return getFileField(fieldLabel).getFileName();
    }

    public boolean isFileFieldBlank(String fieldLabel)
    {
        return getFileField(fieldLabel) == null;
    }

    public File downloadFileField(String fieldLabel)
    {
        return getFileField(fieldLabel).clickDownload();
    }

    public ImageFileViewDialog viewImgFile(String fieldLabel)
    {
        return getFileField(fieldLabel).viewImgFile();
    }

    public File clickNonImgFile(String fieldLabel)
    {
        return getFileField(fieldLabel).clickOnNonImgFile();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        final Locator.XPathLocator editHeadingLocator = Locator.tagWithClass("span", "detail__edit--heading");
        final Locator.XPathLocator headingLocator = editHeadingLocator.parent();
        final WebElement editHeading = editHeadingLocator.findWhenNeeded(this);
        final WebElement headingText = headingLocator.childTag("span").findWhenNeeded(this);

        public Optional<WebElement> editButton()
        {
            return Locator.byClass("detail__edit-button")
                    .findOptionalElement(editHeading);
        }

        // If this panel is for an aliquot sample there will be more than one table present.
        final List<DetailTable> detailTables()
        {
            return new DetailTable.DetailTableFinder(getDriver()).findAll(this);
        }

        public WebElement findValueEl(String fieldLabel)
        {
            return Locator.tagWithAttribute("td", "data-caption", fieldLabel).waitForElement(this, 4_000);
        }

        public AttachmentCard fileField(String fieldLabel)
        {
            return new AttachmentCard.FileAttachmentCardFinder(getDriver()).findOrNull(findValueEl(fieldLabel));
        }
    }

    public static class DetailDataPanelFinder extends WebDriverComponentFinder<DetailDataPanel, DetailDataPanelFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClass("div", "panel")
                .withDescendant(Locator.tagWithClass("table", "detail-component--table__fixed"));
        private final Locator.XPathLocator _baseLocatorAsTooltip = Locator.tagWithClass("div", "header-details-hover");
        private String _title = null;
        private boolean _asTooltip = false;

        public DetailDataPanelFinder(WebDriver driver)
        {
            super(driver);
        }

        public DetailDataPanelFinder withTitle(String title)
        {
            _title = title;
            return this;
        }

        public DetailDataPanelFinder asTooltip()
        {
            _asTooltip = true;
            return this;
        }

        @Override
        protected DetailDataPanel construct(WebElement el, WebDriver driver)
        {
            return new DetailDataPanel(el, driver);
        }

        @Override
        protected Locator locator()
        {
            if (_asTooltip)
                return _baseLocatorAsTooltip;
            else if (_title != null)
                return _baseLocator.withChild(Locator.byClass("panel-heading")
                        .withText().containing(_title));
            else
                return _baseLocator;
        }
    }
}
