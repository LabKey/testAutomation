package org.labkey.test.pages.admin;

import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.html.OptionSelect;
import org.labkey.test.components.html.Table;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.openqa.selenium.NotFoundException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Wraps `AdminController.ExternalSourcesAction`
 */
public class ExternalSourcesPage extends LabKeyPage<ExternalSourcesPage.ElementCache>
{
    public ExternalSourcesPage(WebDriver driver)
    {
        super(driver);
    }

    public static ExternalSourcesPage beginAt(WebDriverWrapper webDriverWrapper)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("admin", "externalSources"));
        return new ExternalSourcesPage(webDriverWrapper.getDriver());
    }

    @LogMethod
    public ExternalSourcesPage ensureHost(Directive directive, String host)
    {
        if (!getExistingHosts().getOrDefault(directive, Collections.emptySet()).contains(host))
        {
            return addHost(directive, host);
        }
        else
        {
            log("Host for CSP " + directive.getDirectiveId() + " already registered: " + host);
            return this;
        }
    }

    @LogMethod (quiet = true)
    public ExternalSourcesPage addHost(@LoggedParam Directive directive, @LoggedParam String host)
    {
        elementCache().directiveSelect.selectOption(directive);
        elementCache().hostInput.set(host);

        clickAndWait(elementCache().addButton);
        clearCache();
        assertNoLabKeyErrors();
        return this;
    }

    @LogMethod (quiet = true)
    public List<String> addHostExpectingError(@LoggedParam Directive directive, @LoggedParam String host)
    {
        elementCache().directiveSelect.selectOption(directive);
        elementCache().hostInput.set(host);

        clickAndWait(elementCache().addButton);
        clearCache();

        return getTexts(Locators.labkeyError.findElements(elementCache()));
    }

    public ExternalSourcesPage editExistingSource(Directive directive, String host, String newHost)
    {
        getExistingSourceRow(directive, host).getHostInput().set(newHost);
        return this;
    }

    public ExternalSourcesPage deleteExistingSource(Directive directive, String host, String newHost)
    {
        getExistingSourceRow(directive, host).clickDelete();
        return this;
    }

    private ExistingSourceRow getExistingSourceRow(Directive directive, String host)
    {
        return elementCache().getExistingSourceRows().stream()
            .filter(row -> row.getDirective().equals(directive) && row.getHost().equalsIgnoreCase(host))
            .findFirst().orElseThrow(() -> new NotFoundException("Host " + host + " does not exist for directive " + directive));
    }

    @LogMethod (quiet = true)
    public ExternalSourcesPage saveChanges()
    {
        clickAndWait(elementCache().saveButton);
        clearCache();

        assertNoLabKeyErrors();
        return this;
    }

    @LogMethod (quiet = true)
    public List<String> saveChangesExpectingError()
    {
        clickAndWait(elementCache().saveButton);
        clearCache();

        return getTexts(Locators.labkeyError.findElements(elementCache()));
    }

    public Map<Directive, Set<String>> getExistingHosts()
    {
        Map<Directive, Set<String>> existingHosts = new HashMap<>();

        for (ExistingSourceRow row : elementCache().getExistingSourceRows())
        {
            Directive directive = row.getDirective();
            String hostInput = row.getHost();
            existingHosts.computeIfAbsent(directive, d -> new HashSet<>()).add(hostInput);
        }

        return existingHosts;
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage<ElementCache>.ElementCache
    {
        final WebElement addHostForm = Locator.name("addNewHost").findWhenNeeded(this);
        final OptionSelect<Directive> directiveSelect = OptionSelect.finder(Locator.id("newDirective"), Directive.class).findWhenNeeded(addHostForm);
        final Input hostInput = Input.Input(Locator.id("newHostTextField"), getDriver()).findWhenNeeded(addHostForm);
        final WebElement addButton = Locator.lkButton("Add").findWhenNeeded(addHostForm);

        final WebElement existingValuesForm = Locator.name("existingValues").findWhenNeeded(this);
        final Table existingValuesTable = new Table(getDriver(), Locator.byClass("labkey-data-region-legacy").findWhenNeeded(existingValuesForm), 0);
        private List<ExistingSourceRow> existingSourceRows;

        protected List<ExistingSourceRow> getExistingSourceRows()
        {
            if (existingSourceRows == null)
            {
                List<WebElement> rows = existingValuesTable.getRows();
                List<ExistingSourceRow> temp = new ArrayList<>();
                for (int i = 0; i < rows.size(); i++)
                {
                    WebElement row = rows.get(i);
                    temp.add(new ExistingSourceRow(row, i));
                }
                existingSourceRows = Collections.unmodifiableList(temp);
            }
            return existingSourceRows;
        }

        final WebElement saveButton = Locator.lkButton("Save").findWhenNeeded(existingValuesForm);
    }

    protected class ExistingSourceRow
    {
        private final WebElement directiveInput;
        private final Input hostInput;
        private final WebElement deleteButton;
        private final int rowIndex;

        private Directive directive;

        ExistingSourceRow(WebElement row, int rowIndex)
        {
            directiveInput = Locator.tag("input").withAttributeContaining("name", "directive").findWhenNeeded(row);
            hostInput = Input.Input(Locator.tag("input").withAttributeContaining("name", "host"), getDriver()).findWhenNeeded(row);
            deleteButton = Locator.lkButton("Delete").findWhenNeeded(row);
            this.rowIndex = rowIndex;
        }

        Directive getDirective()
        {
            if (directive == null)
            {
                directive = Directive.valueOf(directiveInput.getDomAttribute("data-directive"));
            }
            return directive;
        }

        public Input getHostInput()
        {
            return hostInput;
        }

        public String getHost()
        {
            return hostInput.get();
        }

        public void clickDelete()
        {
            clickAndWait(deleteButton);
            clearCache();
        }
    }

    public enum Directive implements OptionSelect.SelectOption
    {
        Connection("connect-src"),
        Font("font-src"),
        Frame("frame-src"),
        Image("image-src"),
        Style("style-src"),
        ;

        private final String directiveId;

        Directive(String directiveId)
        {
            this.directiveId = directiveId;
        }

        public String getDirectiveId()
        {
            return directiveId;
        }

        @Override
        public String getValue()
        {
            return name();
        }

        public static Directive fromId(String directiveId)
        {
            return Stream.of(values()).filter(d -> d.getDirectiveId().equals(directiveId)).findAny()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown CSP directive: " + directiveId));
        }
    }
}
