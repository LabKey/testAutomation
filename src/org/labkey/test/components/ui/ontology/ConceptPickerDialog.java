package org.labkey.test.components.ui.ontology;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.react.ReactSelect;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;

import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;

public class ConceptPickerDialog extends ModalDialog
{
    protected ConceptPickerDialog(WebElement element, WebDriver driver)
    {
        super(element, driver);
    }

    public ConceptPickerDialog(ModalDialogFinder finder)
    {
        super(finder);
    }

    @Override
    protected void waitForReady()
    {
        super.waitForReady();
        WebDriverWrapper.waitFor(() -> elementCache().ontologySelect.getComponentElement().isDisplayed() ||
                elementCache().searchBox.getComponentElement().isDisplayed(), "Concept picker didn't load", 5_000);
    }

    /**
     * uses the search bar to select an item in the ontology tree
     * @param conceptSearchExpression an expression close enough to the target node to hit
     * @param code  the intended concept code
     * @return the current dialog
     */
    public ConceptPickerDialog searchConcept(String conceptSearchExpression, String code)
    {
        elementCache().searchBox.selectItemWithCode(conceptSearchExpression, code);
        getWrapper().waitForElement(Locator.tagWithClass("span", "code").withText(code));
        return this;
    }

    /**
     * uses the information tabs to get the name of the selected concept
     * @return the contents of the 'title' element on the overview tab panel
     */
    public String getSelectedConcept()
    {
        return elementCache().infoTabs.getTitle();
    }

    /**
     * uses the information tabs to obtain the concept code
     * @return the contents of the 'code' span on the overview tab panel
     */
    public String getSelectedConceptCode()
    {
        return elementCache().infoTabs.getCode();
    }

    /**
     * uses the information tabs to obtain the parts of the current-selected path
     * @return contents of spans in the pathContainer
     */
    public List<String> getSelectedConceptPath()
    {
        return elementCache().infoTabs.getSelectedPath();
    }

    /**
     * uses the treePanel control to expand the nodes in order
     * @param pathToNode
     * @return the current dialog
     */
    public ConceptPickerDialog selectNodeFromPath(List<String> pathToNode)
    {
        elementCache().treePanel.openToPath(pathToNode);
        return this;
    }

    /**
     * This select is only shown if there are multiple ontologies available to choose from.
     * When it is shown, no ontology-specific controls (like the tree view, the search bar, or the
     * info tabs will be shown.
     * @return  Whether or not the ontology select is present
     */
    public boolean hasOntologySelect()
    {
        return elementCache().ontologySelect.getComponentElement().isDisplayed();
    }

    /**
     * When the select-ontology select appears, this sets it.  Once set, the select should disappear
     * and the rest of the controls should appear in the dialog
     * @param ontology The option to select from the ontology select
     * @return the current dialog
     */
    public ConceptPickerDialog selectOntology(String ontology)
    {
        WebDriverWrapper.waitFor(() -> elementCache().ontologySelect.getComponentElement().isDisplayed(),
                "the ontology select did not become present", WAIT_FOR_JAVASCRIPT);
        var select = elementCache().ontologySelect;
        var selectElement = select.getComponentElement();
        select.select(ontology);
        getWrapper().shortWait().until(ExpectedConditions.stalenessOf(selectElement));
        return this;
    }

    /**
     * gets the options in the ontology select
     * @return A list of string values from the options present in the select
     */
    public List<String> getAvailableOntologies()
    {
        WebDriverWrapper.waitFor(() -> elementCache().ontologySelect.getComponentElement().isDisplayed(),
                "the ontology select did not become present", WAIT_FOR_JAVASCRIPT);
        return elementCache().ontologySelect.getOptions();
    }

    public ConceptPickerDialog waitForActiveTreeNode()
    {
        elementCache().treePanel.waitForActiveNode();
        return this;
    }

    public void clickApply()
    {
        dismiss("Apply");
    }

    public void clickCancel()
    {
        dismiss("Cancel");
    }

    @Override
    protected ModalDialog.ElementCache newElementCache()
    {
        return new ElementCache();
    }

    @Override
    protected ElementCache elementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends ModalDialog.ElementCache
    {
        final OntologyTreeSearch searchBox = new OntologyTreeSearch.OntologyTreeSearchFinder(getDriver())
                .findWhenNeeded(this);

        final OntologyTreePanel treePanel = new OntologyTreePanel.OntologyTreePanelFinder(getDriver())
                .findWhenNeeded(this);

        final ConceptInfoTabs infoTabs = new ConceptInfoTabs.ConceptInfoTabsFinder(getDriver())
                .refindWhenNeeded(this);

        final ReactSelect ontologySelect = ReactSelect.finder(getDriver()).withId("ontology-select")
                .findWhenNeeded(this);
    }


}
