package org.labkey.test.components.ui.ontology;

import org.labkey.test.Locator;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.react.ReactSelect;
import org.labkey.test.components.ui.ontology.OntologyTreeSearch;
import org.labkey.test.pages.ontology.BrowseConceptsPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;
import java.util.Optional;

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

    // optional ontology select, in case there are multiple ontologies

    /**
     * uses the search bar to select an item in the ontology tree
     * @param conceptSearchExpression an expression close enough to the target node to hit
     * @param code  the intended concept code
     * @return the current dialog
     */
    public ConceptPickerDialog searchConcept(String conceptSearchExpression, String code)
    {
        elementCache().searchBox.selectItemWithCode(conceptSearchExpression, code);
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

    public ConceptPickerDialog selectNodeFromPath(List<String> pathToNode)
    {
        elementCache().treePanel.openToPath(pathToNode);
        return this;
    }

    /**
     * This select is only shown if there are multiple ontologies available to choose from.
     * When it is shown, no ontology-specific controls (like the tree view, the search bar, or the
     * info tabs will be shown.
     * @return
     */
    public boolean hasOntologySelect()
    {
        return elementCache().selectOntologySelect().isPresent();
    }

    /**
     * When the select-ontology select appears, this sets it.  Once set,
     * @param ontology
     * @return
     */
    public ConceptPickerDialog selectOntology(String ontology)
    {
        getWrapper().waitFor(() -> elementCache().selectOntologySelect().isPresent(),
                "the ontology select did not become present", WAIT_FOR_JAVASCRIPT);
        var select = elementCache().selectOntologySelect().get();
        var selectElement = select.getComponentElement();
        select.select(ontology);
        getWrapper().shortWait().until(ExpectedConditions.stalenessOf(selectElement));
        return this;
    }

    public List<String> getAvailableOntologies()
    {
        getWrapper().waitFor(() -> elementCache().selectOntologySelect().isPresent(),
                "the ontology select did not become present", WAIT_FOR_JAVASCRIPT);
        var select = elementCache().selectOntologySelect().get();
        return select.getOptions();
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
                .findWhenNeeded(this);

        Optional<ReactSelect> selectOntologySelect()
        {
            return ReactSelect.finder(getDriver())
                    .withId("ontology-select").findOptional();
        }
    }


}
