package org.labkey.test.components.list;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.glassLibrary.components.ReactSelect;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.html.RadioButton;
import org.labkey.test.pages.core.login.SvgCheckbox;
import org.labkey.test.pages.list.EditListDefinitionPage;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;

public class AdvancedListSettingsDialog extends ModalDialog
{
    private EditListDefinitionPage _page;

    public AdvancedListSettingsDialog(EditListDefinitionPage page)
    {
        super(new ModalDialogFinder(page.getDriver()).withTitle("Advanced List Settings"));
        _page = page;
    }

    public AdvancedListSettingsDialog setFieldUsedForDisplayTitle(String field)
    {
        ReactSelect.finder(getDriver()).waitFor(this).select(field);
        return this;
    }



    public AdvancedListSettingsDialog disableDiscussionLinks()
    {
        elementCache().radio(this, "Disable discussion links").check();
        return this;
    }

    public AdvancedListSettingsDialog allowOneDiscussionPerItem()
    {
        elementCache().radio(this, "Allow one discussion per item").check();
        return this;
    }

    public AdvancedListSettingsDialog allowMultipleDiscussionsPerItem()
    {
        elementCache().radio(this, "Allow multiple discussions per item").check();
        return this;
    }

    public AdvancedListSettingsDialog indexEntireListAsASingleDocument(boolean checked, String docTitle,
                                                                       SearchIncludeOptions includeOptions,
                                                                       SearchIndexOptions indexOptions)
    {
        String labelText = "Index entire list as a single document";
        elementCache().checkbox(labelText).set(checked);
        if (checked)
        {
            WebElement expandContainer = elementCache().collapsibleFieldContainer(labelText)
                    .waitForElement(this, 2000);
            WebElement expandCollapsePane = elementCache().collapsibleField(labelText);
            expandPane(expandCollapsePane);
            Input.Input(Locator.id("entireListTitleTemplate"), getDriver()).find().set(docTitle);
            elementCache().radio(expandContainer, includeOptions.toString()).check();
            elementCache().radio(expandContainer, indexOptions.toString()).check();
        }
        return this;
    }

    public AdvancedListSettingsDialog disableEntireListIndex()
    {
        return indexEntireListAsASingleDocument(false, null, null, null);
    }

    public AdvancedListSettingsDialog indexEachItemAsASeparateDocument(boolean checked, String docTitle, SearchIndexOptions indexOptions)
    {
        String labelText = "Index each item as a separate document";
        elementCache().checkbox(labelText).set(checked);
        if (checked)
        {
            WebElement expandContainer = elementCache().collapsibleFieldContainer(labelText)
                    .waitForElement(this, 2000);
            WebElement expandCollapsePane = elementCache().collapsibleField(labelText);
            expandPane(expandCollapsePane);

            Input.Input(Locator.id("eachItemTitleTemplate"), getDriver()).find().set(docTitle);
            elementCache().radio(expandContainer, indexOptions.toString()).check();
        }
        return this;
    }

    public AdvancedListSettingsDialog disableEachItemIndexing()
    {
        return indexEachItemAsASeparateDocument(false, null, null);
    }

    private void expandPane(WebElement expandCollapsePane)
    {
        if (!isPaneExpanded(expandCollapsePane))
        {
            expandCollapsePane.click();
            WebDriverWrapper.waitFor(()-> isPaneExpanded(expandCollapsePane),
                    "the pane did not expand in time", 1000);
        }
    }

    private boolean isPaneExpanded(WebElement expandCollapsePane)
    {
        return Locator.tagWithClass("svg", "fa-angle-down").existsIn(expandCollapsePane);
    }

    public AdvancedListSettingsDialog setIndexFileAttachments(boolean checked)
    {
        Locator loc = Locator.tagWithClass("span", "list__advanced-settings-model__index-checkbox")
                .withChild(Locator.tagWithText("span", "Index file attachments"))
                .child(Locator.tagWithClass("span", "list__properties__no-highlight"));
        SvgCheckbox checkbox = new SvgCheckbox(loc.waitForElement(this, 2000), getDriver());
        checkbox.set(checked);
        return this;
    }

    public EditListDefinitionPage clickApply()
    {
        dismiss("Apply");
        return _page;
    }

    public EditListDefinitionPage clickCancel()
    {
        dismiss("Cancel");
        return _page;
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    @Override
    protected ElementCache elementCache()
    {
        return  (ElementCache) super.elementCache();
    }

    protected class ElementCache extends ModalDialog.ElementCache
    {
        Locator.XPathLocator checkBoxLoc(String labelText)
        {
            return Locator.tagWithClass("span", "list__advanced-settings-modal__index-checkbox")
                    .withDescendant(Locator.tagWithText("span", labelText).withClass("list__clickable"))
                    .child(Locator.tagWithClass("span", "list__properties__no-highlight"));
        }
        Locator.XPathLocator collapsibleFieldLoc(String checkboxLabelText)
        {
            return Locator.tag("div").withChild(checkBoxLoc(checkboxLabelText))
                    .child(Locator.tagWithClass("span", "list__advanced-settings-model__collapsible-field"));
        }
        Locator.XPathLocator collapsibleFieldContainer(String checkboxLabelText)
        {
            return Locator.tag("div").withChild(checkBoxLoc(checkboxLabelText));
        }

        SvgCheckbox checkbox(String labelText)
        {
            return new SvgCheckbox(checkBoxLoc(labelText).waitForElement(this, 2000), getDriver());
        }

        RadioButton radio(SearchContext searchContext, String labelText)
        {
            Locator loc = Locator.tagWithClass("div", "radio")
                    .withChild(Locator.tagContainingText("label", labelText))
                    .descendant("input");
            return new RadioButton(loc.waitForElement(this, 2000));
        }

        WebElement collapsibleField(String checkboxLabelText)
        {
            return collapsibleFieldLoc(checkboxLabelText).waitForElement(this, 2000);
        }
    }

    public enum SearchIncludeOptions
    {
        MetadataAndData("Include both metadata and data" ),
        DataOnly("Include data only"),
        MetadataOnly("Include metadata only (name and description of list and fields)");

        private String _labelText;

        SearchIncludeOptions(String labelText)
        {
            _labelText = labelText;
        }

        public String toString()
        {
            return _labelText;
        }
    }

    public enum SearchIndexOptions
    {
        NonPhiText("Index all non-PHI text fields"),
        NonPhiFields("Index all non-PHI fields (text, number, date, and boolean)"),
        CustomTemplate("Index using custom template");

        private String _labelText;

        SearchIndexOptions(String labelText)
        {
            _labelText = labelText;
        }

        public String toString()
        {
            return _labelText;
        }
    }
}
