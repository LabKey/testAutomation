package org.labkey.test.components.list;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.react.ReactSelect;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.html.RadioButton;
import org.labkey.test.pages.list.EditListDefinitionPage;
import org.openqa.selenium.WebElement;

public class AdvancedListSettingsDialog extends ModalDialog
{
    private final EditListDefinitionPage _page;

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
        elementCache().radio("Disable discussion links").check();
        return this;
    }

    public AdvancedListSettingsDialog allowOneDiscussionPerItem()
    {
        elementCache().radio("Allow one discussion per item").check();
        return this;
    }

    public AdvancedListSettingsDialog allowMultipleDiscussionsPerItem()
    {
        elementCache().radio("Allow multiple discussions per item").check();
        return this;
    }

    public AdvancedListSettingsDialog indexEntireListAsASingleDocument(boolean checked, String docTitle,
                                                                       SearchIncludeOptions includeOptions,
                                                                       SearchIndexOptions indexOptions)
    {
        String labelText = "Index entire list as a single document";
        new Checkbox(this, labelText).set(checked);
        if (checked)
        {
            WebElement expandContainer = elementCache().collapsibleFieldContainer(labelText)
                    .waitForElement(this, 2000);
            WebElement expandCollapsePane = elementCache().collapsibleField(labelText);
            expandPane(expandCollapsePane);
            Input.Input(Locator.id("entireListTitleTemplate"), getDriver()).find().set(docTitle);
            elementCache().radio(includeOptions.toString()).check();
            elementCache().radio(indexOptions.toString()).check();
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
        new Checkbox(this, labelText).set(checked);
        if (checked)
        {
            WebElement expandContainer = elementCache().collapsibleFieldContainer(labelText)
                    .waitForElement(this, 2000);
            WebElement expandCollapsePane = elementCache().collapsibleField(labelText);
            expandPane(expandCollapsePane);

            Input.Input(Locator.id("eachItemTitleTemplate"), getDriver()).find().set(docTitle);
            elementCache().radio(indexOptions.toString()).check();
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
        return Locator.tagWithClass("span", "fa-angle-down").existsIn(expandCollapsePane);
    }

    public AdvancedListSettingsDialog setIndexFileAttachments(boolean checked)
    {
        new Checkbox(this, "Index file attachments").set(checked);
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
            return Locator.tagWithText("label", labelText);
        }
        Locator.XPathLocator collapsibleFieldLoc(String checkboxLabelText)
        {
            return Locator.tagWithClass("div", "list__advanced-settings-modal__collapsible-field")
                    .withDescendant(checkBoxLoc(checkboxLabelText));
        }
        Locator.XPathLocator collapsibleFieldContainer(String checkboxLabelText)
        {
            return Locator.tag("div").withChild(checkBoxLoc(checkboxLabelText));
        }

        RadioButton radio(String labelText)
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
