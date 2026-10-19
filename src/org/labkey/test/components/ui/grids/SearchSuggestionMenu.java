/*
 * Copyright (c) 2026 LabKey Corporation
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
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Wraps the suggestion menu of 'labkey-ui-components' <code>public/QueryModel/search/SearchSuggestInput.tsx</code>.
 * Clicking a suggestion doesn't wait for the grid; {@link GridBar} wraps it in the wait that suits the suggestion kind.
 */
public class SearchSuggestionMenu extends WebDriverComponent<SearchSuggestionMenu.ElementCache>
{
    private final WebElement _input;
    private final WebElement _listbox;
    private final WebDriver _driver;

    protected SearchSuggestionMenu(WebElement input, WebDriver driver)
    {
        _input = input;
        _driver = driver;
        // The button bar renders a hidden second copy of the input for narrow layouts, so follow the input's own listbox id
        _listbox = Locator.id(input.getDomAttribute("aria-controls")).refindWhenNeeded(driver);
    }

    @Override
    public WebElement getComponentElement()
    {
        return _listbox;
    }

    @Override
    protected WebDriver getDriver()
    {
        return _driver;
    }

    public boolean isOpen()
    {
        return "true".equals(_input.getDomAttribute("aria-expanded"));
    }

    /**
     * Wait for the menu to open and finish loading. Options are keyed by position and replaced when suggestions
     * arrive, so an option clicked while loading can turn into a different suggestion.
     */
    public SearchSuggestionMenu waitForSuggestions()
    {
        WebDriverWrapper.waitFor(() -> isOpen()
                        && !Locators.loading.existsIn(_listbox)
                        && Locators.option.existsIn(_listbox),
                "Search suggestions did not finish loading.", WebDriverWrapper.WAIT_FOR_JAVASCRIPT);
        return this;
    }

    public List<Suggestion> getSuggestions()
    {
        waitForSuggestions();
        return Locators.option.findElements(_listbox).stream().map(Suggestion::fromElement).toList();
    }

    public List<String> getLabels()
    {
        return getSuggestions().stream().map(Suggestion::label).toList();
    }

    public Optional<Suggestion> getHighlighted()
    {
        return getSuggestions().stream().filter(Suggestion::highlighted).findFirst();
    }

    public boolean hasSuggestion(SuggestionKind kind, String label)
    {
        return getSuggestions().stream().anyMatch(s -> s.kind() == kind && s.label().equals(label));
    }

    /**
     * @return the bolded part of the suggestion's label, or an empty string if nothing is bolded
     */
    public String getBoldedText(SuggestionKind kind, String label)
    {
        WebElement option = findOption(kind, label::equals);
        return Locator.tag("strong").findOptionalElement(option).map(WebElement::getText).orElse("");
    }

    public void clickSuggestion(SuggestionKind kind, String label)
    {
        clickSuggestion(kind, label::equals);
    }

    public void clickSuggestion(SuggestionKind kind, Predicate<String> label)
    {
        findOption(kind, label).click();
    }

    /**
     * Click the only suggestion of the given kind, e.g. {@link SuggestionKind#SEARCH}.
     */
    public void clickSuggestion(SuggestionKind kind)
    {
        clickSuggestion(kind, label -> true);
    }

    public SearchSuggestionMenu highlightNext()
    {
        _input.sendKeys(Keys.ARROW_DOWN);
        return this;
    }

    public SearchSuggestionMenu highlightPrevious()
    {
        _input.sendKeys(Keys.ARROW_UP);
        return this;
    }

    /**
     * Press Enter in the input. With nothing highlighted, it applies the top suggestion, waiting for loading to finish.
     */
    public void pressEnter()
    {
        _input.sendKeys(Keys.ENTER);
    }

    /**
     * Close the menu, leaving the typed text in the input.
     */
    public void close()
    {
        if (isOpen())
        {
            _input.sendKeys(Keys.ESCAPE);
            WebDriverWrapper.waitFor(() -> !isOpen(), "Search suggestion menu did not close.", 1_000);
        }
    }

    private WebElement findOption(SuggestionKind kind, Predicate<String> label)
    {
        waitForSuggestions();
        List<WebElement> options = Locators.option.withAttribute("data-kind", kind.getKind()).findElements(_listbox);
        return options.stream()
                .filter(option -> label.test(Locators.label.findElement(option).getText()))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException(String.format(
                        "No '%s' suggestion matched. Suggestions: %s", kind.getKind(), getSuggestions())));
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
    }

    protected static abstract class Locators
    {
        static final Locator.XPathLocator option = Locator.tagWithAttribute("li", "role", "option");
        static final Locator.XPathLocator label = Locator.tagWithClass("span", "grid-panel__search-suggestion-label");
        static final Locator.XPathLocator loading = Locator.tagWithClass("li", "grid-panel__search-suggestion-loading");
    }

    /**
     * Matches the <code>data-kind</code> of a suggestion: a filter applies directly, a compose opens the filter
     * modal with a draft filter, and a search applies the Q filter ("Search all fields").
     */
    public enum SuggestionKind
    {
        FILTER("filter"),
        COMPOSE("compose"),
        SEARCH("search");

        private final String _kind;

        SuggestionKind(String kind)
        {
            _kind = kind;
        }

        public String getKind()
        {
            return _kind;
        }

        public static SuggestionKind fromKind(String kind)
        {
            return Arrays.stream(values())
                    .filter(value -> value._kind.equals(kind))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Unknown suggestion kind: " + kind));
        }
    }

    /**
     * @param source the server's suggestion source, e.g. <code>identity</code>, <code>partialValue</code> or <code>fallback</code>
     */
    public record Suggestion(SuggestionKind kind, String source, String label, boolean highlighted)
    {
        private static Suggestion fromElement(WebElement option)
        {
            return new Suggestion(
                    SuggestionKind.fromKind(option.getDomAttribute("data-kind")),
                    option.getDomAttribute("data-source"),
                    Locators.label.findElement(option).getText(),
                    "true".equals(option.getDomAttribute("aria-selected")));
        }
    }
}
