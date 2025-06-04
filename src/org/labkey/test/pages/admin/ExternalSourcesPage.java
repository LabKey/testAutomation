package org.labkey.test.pages.admin;

import org.json.JSONObject;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.SimplePostCommand;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.html.OptionSelect;
import org.labkey.test.components.html.Table;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.TestLogger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Wraps `AdminController.ExternalSourceAction`
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

    public void deleteAllApi()
    {
        SimplePostCommand postCommand = new SimplePostCommand("admin", "externalSources");
        postCommand.setParameters(Map.of(
            "saveAll", true,
            "existingValues", ""));
        try
        {
            postCommand.execute(createDefaultConnection(), null);
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException(e);
        }
    }

    @LogMethod
    public ExternalSourcesPage ensureHost(Directive directive, String host)
    {
        if (!getExistingHosts().getOrDefault(directive, Collections.emptyList()).contains(host))
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
        return this;
    }

    public Map<Directive, List<String>> getExistingHosts()
    {
        Map<Directive, List<String>> existingHosts = new HashMap<>();

        for (Map.Entry<Directive, List<Input>> entry : getExistingHostInputs().entrySet())
        {
            existingHosts.put(entry.getKey(), entry.getValue().stream().map(Input::getValue).toList());
        }

        return existingHosts;
    }

    public Map<Directive, List<Input>> getExistingHostInputs()
    {
        List<WebElement> directiveColumn = elementCache().existingValuesTable.getColumnAsElement(1);
        List<WebElement> hostsColumn = elementCache().existingValuesTable.getColumnAsElement(2);

        Map<Directive, List<Input>> existingHosts = new HashMap<>();

        for (int i = 0; i < hostsColumn.size(); i++)
        {
            WebElement directiveInput = Locator.tag("input").findElement(directiveColumn.get(i));
            Directive directive = Directive.valueOf(directiveInput.getDomAttribute("data-directive"));
            Input hostInput = Input.Input(Locator.tag("input"), getDriver()).find(hostsColumn.get(i));
            existingHosts.computeIfAbsent(directive, d -> new ArrayList<>()).add(hostInput);
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
