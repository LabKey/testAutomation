/*
 * Copyright (c) 2008-2019 LabKey Corporation
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

package org.labkey.test;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.test.selenium.LazyWebElement;
import org.labkey.test.selenium.ReclickingWebElement;
import org.labkey.test.selenium.RefindingWebElement;
import org.labkey.test.util.TestLogger;
import org.labkey.test.util.selenium.WebDriverUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.InvalidSelectorException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.NotFoundException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WrapsDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.FluentWait;

import java.text.DecimalFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public abstract class Locator extends By
{
    private final String _loc;
    protected final Integer _index;
    protected final String _contains;
    protected final String _text;
    private String _description;

    // XPATH fragments
    public static final String HIDDEN = "ancestor-or-self::*[" +
            "contains(@style,'display: none') or " +
            "contains(@style,'visibility: hidden') or " +
            "contains(@class, 'x-hide-display') or " +
            "contains(@class, 'x4-hide-offsets') or " +
            "contains(@class, 'x-hide-offsets')] or " +
            "(@type = 'hidden')";
    public static final String NOT_HIDDEN = "not(" + HIDDEN + ")";
    public static final String DISABLED = "ancestor-or-self::*[contains(@class, 'disabled')]";
    public static final String ENABLED = "not(" + DISABLED + ")";
    public static final String NBSP = "\u00A0";

    protected Locator(String loc)
    {
        this(loc, null, null, null);
    }

    private Locator(String loc, Integer index, String contains, String text)
    {
        _loc = loc;
        _index = index;
        _contains = contains;
        _text = text == null ? null : text.trim();
    }

    protected static class WrappedLocator extends Locator
    {
        private final Locator wrappedLocator;

        protected WrappedLocator(Locator wrappedLocator)
        {
            super(wrappedLocator.getLoc());
            this.wrappedLocator = wrappedLocator;
        }

        @Override
        public Locator containing(String contains)
        {
            return wrappedLocator.containing(contains);
        }

        @Override
        public Locator withText(String text)
        {
            return wrappedLocator.withText(text);
        }

        @Override
        public Locator index(Integer index)
        {
            return wrappedLocator.index(index);
        }

        @Override
        public String toString()
        {
            return wrappedLocator.toString();
        }

        @Override
        public String getLoggableDescription()
        {
            return wrappedLocator.getLoggableDescription();
        }

        @Override
        protected By getBy()
        {
            return wrappedLocator.getBy();
        }
    }

    protected static class ImmutableLocator extends WrappedLocator
    {
        protected ImmutableLocator(Locator wrappedLocator)
        {
            super(wrappedLocator);
        }

        @Override
        public Locator containing(String contains)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public Locator withText(String text)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public Locator index(Integer index)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public Locator describedAs(String description)
        {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Wait for a single element located by any one of the provided Locators
     * Note: Not redundant with {@link #waitForElements(FluentWait, Locator...)}.
     * This will short-circuit as soon as a single element is found
     * @return The first element found
     */
    public static WebElement waitForAnyElement(FluentWait<? extends SearchContext> wait, final Locator... locators)
    {
        return wait.until(new Function<SearchContext, WebElement>()
        {
            @Override
            public WebElement apply(SearchContext context)
            {
                return findAnyElementOrNull(context, locators);
            }

            @Override
            public String toString()
            {
                List<String> locDescriptions = new ArrayList<>();
                Arrays.stream(locators).forEach(loc -> locDescriptions.add(loc.getLoggableDescription()));
                SearchContext searchContext = extractInputFromFluentWait(wait);
                return String.join("\n--OR--\n", locDescriptions) + (searchContext instanceof WebDriver ? "" : "\nIN: " + searchContext.toString());
            }
        });
    }

    /**
     * Wait for elements located by any one of the provided Locators
     * @return All elements matching any of the provided Locators
     */
    public static List<WebElement> waitForElements(FluentWait<? extends SearchContext> wait, final Locator... locators)
    {
        return wait.until(new Function<SearchContext, List<WebElement>>()
        {
            @Override
            public List<WebElement> apply(SearchContext context)
            {
                List<WebElement> els = findElements(context, locators);
                if (els.size() > 0)
                    return els;
                else
                    return null;
            }

            @Override
            public String toString()
            {
                List<String> locDescriptions = new ArrayList<>();
                Arrays.stream(locators).forEach(loc -> locDescriptions.add(loc.getLoggableDescription()));
                SearchContext searchContext = extractInputFromFluentWait(wait);
                return String.join("\n--OR--\n", locDescriptions) + (searchContext instanceof WebDriver ? "" : "\nIN: " + searchContext.toString());
            }
        });

    }

    public static List<WebElement> findElements(SearchContext context, final Locator... locators)
    {
        List<WebElement> els = new ArrayList<>();
        for (Locator loc : locators)
        {
            els.addAll(loc.findElements(context));
        }
        return els;
    }

    public static WebElement findAnyElement(String description, SearchContext context, final Locator... locators)
    {
        WebElement el = findAnyElementOrNull(context, locators);
        if (el == null)
            throw new NoSuchElementException(description + " not found");
        return el;
    }

    @Nullable
    public static WebElement findAnyElementOrNull(SearchContext context, final Locator... locators)
    {
        List<WebElement> els;
        for (Locator loc : locators)
        {
            els = loc.findElements(context);
            if (!els.isEmpty())
                return els.get(0);
        }
        return null;
    }

    public abstract Locator containing(String contains);

    public abstract Locator withText(String text);

    /**
     * Locate the nth element matched by the selector. Can only find a single element.
     * @param index zero-based index of desired element
     * @return Locator for the element
     */
    public abstract Locator index(Integer index);

    public final Locator immutable()
    {
        return new ImmutableLocator(this);
    }

    public abstract String toString();

    protected abstract By getBy();

    public Locator describedAs(String description)
    {
        _description = description;
        return this;
    }

    public String getLoc()
    {
        return _loc;
    }

    public String getLoggableDescription()
    {
        return (_description == null ? "" : _description + "\n") +
            toString() +
            (_index == null ? "" : "\nIndex: " + _index) +
            (_contains == null ? "" : "\nContaining: " + _contains) +
            (_text == null ? "" : "\nWith Text: " + _text);
    }

    protected final String getFindDescription(SearchContext searchContext)
    {
        return getLoggableDescription() + (searchContext instanceof WebDriver ? "" : "\nwithin: " + searchContext.toString());
    }

    protected final String getWaitDescription(FluentWait<? extends SearchContext> wait)
    {
        final SearchContext searchContext = extractInputFromFluentWait(wait);
        return getFindDescription(searchContext);
    }

    protected static <T> T extractInputFromFluentWait(FluentWait<T> wait)
    {
        MutableObject<T> wrappedContext = new MutableObject<>();
        wait.until(input -> {
            wrappedContext.setValue(input);
            return true;
        });
        return wrappedContext.getValue();
    }

    public LazyWebElement<?> findWhenNeeded(SearchContext context)
    {
        return new LazyWebElement<>(this, context);
    }

    public RefindingWebElement refindWhenNeeded(SearchContext context)
    {
        return new RefindingWebElement(this, context);
    }

    @Override
    public WebElement findElement(SearchContext context)
    {
        Optional<WebElement> optionalElement = findOptionalElement(context);
        return optionalElement.orElseThrow(() ->
                new NoSuchElementException("Unable to find element: " + getFindDescription(context)));
    }

    public WebElement findElementOrNull(SearchContext context)
    {
        return findOptionalElement(context).orElse(null);
    }

    public Optional<WebElement> findOptionalElement(SearchContext context)
    {
        List<WebElement> elements = findElements(context);
        if (elements.isEmpty())
            return Optional.empty();
        return Optional.of(elements.get(0));
    }

    @Override
    public List<WebElement> findElements(SearchContext context)
    {
        List<WebElement> elements = context.findElements(this.getBy());
        boolean matchText = _text != null;
        boolean matchContains = _contains != null && !_contains.isEmpty();
        int index = 0;
        if (matchText || matchContains)
        {
            if (elements.size() > 10)
                TestLogger.log(String.format("WARNING: Consider using XPath to find element(s) with text content to avoid time-consuming calls to WebElement.getText().\n" +
                        "Found %d WebElements with this Locator: %s", elements.size(), getLoggableDescription()));

            Iterator<WebElement> it = elements.iterator();
            WebElement el;
            while (it.hasNext())
            {
                el = it.next();
                String text;
                try
                {
                    text = el.getText().trim();
                    if (matchText && !text.equals(_text) || matchContains && !text.contains(_contains))
                    {
                        it.remove();
                    }
                    else
                    {
                        if (Integer.valueOf(index).equals(_index))
                            return Collections.singletonList(el); // Return as soon as we find the desired element
                        index++;
                    }
                }
                catch (StaleElementReferenceException ex)
                {
                    it.remove();
                }
            }
        }

        if (_index == null)
            return decorateWebElements(elements);
        else
        {
            if (elements.size() > _index)
                return decorateWebElements(Collections.singletonList(elements.get(_index)));
            return Collections.emptyList();
        }
    }

    public boolean existsIn(SearchContext context)
    {
        return findElementOrNull(context) != null;
    }

    protected final List<WebElement> decorateWebElements(List<WebElement> elements)
    {
        List<WebElement> decoratedElements = new ArrayList<>(elements.size());
        elements.forEach(el -> decoratedElements.add(decorateWebElement(el)));
        return decoratedElements;
    }

    protected WebElement decorateWebElement(WebElement toBeDecorated)
    {
        return new ReclickingWebElement(toBeDecorated);
    }

    public List<WebElement> waitForElements(final SearchContext context, final int msTimeout)
    {
        waitForElement(context, msTimeout);
        return findElements(context);
    }

    public List<WebElement> waitForElements(FluentWait<? extends SearchContext> wait)
    {
        try
        {
            return wait.until(new Function<SearchContext, List<WebElement>>()
            {
                @Override
                public List<WebElement> apply(SearchContext context)
                {
                    List<WebElement> elements = findElements(context);
                    return elements.isEmpty() ? null : elements;
                }

                @Override
                public String toString()
                {
                    return getWaitDescription(wait);
                }
            });
        }
        catch (TimeoutException notFound)
        {
            throw new NoSuchElementException(notFound.getMessage(), notFound);
        }
    }

    public WebElement waitForElement(final SearchContext context, final int msTimeout)
    {
        FluentWait<SearchContext> wait = new FluentWait<>(context).withTimeout(Duration.ofMillis(msTimeout));

        return waitForElement(wait);
    }

    public WebElement waitForElement(FluentWait<? extends SearchContext> wait)
    {
        try
        {
            return wait.ignoring(NoSuchElementException.class).until(new Function<SearchContext, WebElement>()
            {
                @Override
                public WebElement apply(SearchContext context)
                {
                    return findElement(context);
                }

                @Override
                public String toString()
                {
                    return getWaitDescription(wait);
                }
            });
        }
        catch (TimeoutException notFound)
        {
            throw new NoSuchElementException(notFound.getMessage(), notFound);
        }
    }

    public void waitForElementToDisappear(final SearchContext context, final int msTimeout)
    {
        FluentWait<SearchContext> wait = new FluentWait<>(context).withTimeout(Duration.ofMillis(msTimeout));

        waitForElementToDisappear(wait);
    }

    public void waitForElementToDisappear(FluentWait<? extends SearchContext> wait)
    {
        wait.ignoring(NotFoundException.class).until(new Function<SearchContext, Boolean>()
        {
            @Override
            public Boolean apply(SearchContext context)
            {
                return findElements(context).size() == 0;
            }

            @Override
            public String toString()
            {
                return "element to disappear: " + getWaitDescription(wait);
            }
        });
    }

    public static IdLocator id(String id)
    {
        return new IdLocator(id);
    }

    public static XPathLocator name(String name)
    {
        return tag("*").withAttribute("name", name);
    }

    public static CssLocator css(String selector)
    {
        return new CssLocator(selector);
    }

    public static XPathLocator xpath(String xpathExpr)
    {
        return new XPathLocator(xpathExpr);
    }

    public static XPathLocator tag(String tag)
    {
        return new XPathCSSLocator(tag);
    }

    public static XPathLocator tagWithName(String tag, String name)
    {
        return tagWithAttribute(tag, "name", name);
    }

    public static XPathLocator tagWithId(String tag, String id)
    {
        return tagWithAttribute(tag, "id", id);
    }

    public static XPathLocator tagWithAttribute(String tag, String attrName)
    {
        return tag(tag).withAttribute(attrName);
    }

    public static XPathLocator tagWithAttribute(String tag, String attrName, String attrVal)
    {
        return tag(tag).withAttribute(attrName, attrVal);
    }

    public static XPathLocator tagWithAttributeContaining(String tag, String attrName, String attrVal)
    {
        return tag(tag).withAttributeContaining(attrName, attrVal);
    }

    public static XPathLocator tagWithAttributeIgnoreCase(String tag, String attrName, String attrVal)
    {
        return tag(tag).withAttributeIgnoreCase(attrName, attrVal);
    }

    public static XPathLocator tagWithClass(String tag, String cssClass)
    {
        return tag(tag).withClass(cssClass);
    }

    public static XPathLocator tagWithClassContaining(String tag, String partialCssClass)
    {
        return tag(tag).withAttributeContaining("class", partialCssClass);
    }

    public static XPathLocator tagWithNameContaining(String tag, String partialName)
    {
        return tag(tag).withAttributeContaining("name",partialName);
    }

    public static XPathLocator byClass(String cssClass)
    {
        return tag("*").withClass(cssClass);
    }

    public static XPathLocator tagWithText(String tag, String text)
    {
        return tag(tag).withText(text);
    }

    /**
     * Non-standard 'contains' implementation, but changing to tag().containing() breaks too much stuff
     */
    public static XPathLocator tagContainingText(String tag, String text)
    {
        return tag(tag).withPredicate("contains(text(), " + xq(text) + ")");
    }

    public static XPathLocator linkWithImage(String image)
    {
        return tag("a").withChild(tag("img").withAttributeContaining("src", image));
    }

    public static XPathLocator gwtButton(String text)
    {
        return tag("a").withClass("gwt-Anchor").withText(text);
    }

    public static XPathLocator button(String text)
    {
        return tag("button").notHidden().withPredicate("not(contains(@class, 'tab'))").withText(text);
    }

    public static XPathLocator buttonContainingText(String text)
    {
        return tag("button").notHidden().withPredicate("not(contains(@class, 'tab'))").containing(text);
    }

    public static XPathLocator lkButton(String text)
    {
        return lkButton().withText(text);
    }

    /**
     * org.labkey.api.util.Button
     */
    public static XPathLocator lkButton()
    {
        return tagWithClass("a", "labkey-button").notHidden();
    }

    public static XPathLocator enabledLkButton()
    {
        return lkButton().withoutClass("labkey-disabled-button");
    }

    public static XPathLocator lkLabel(String text)
    {
        return tag("td").withClass("labkey-form-label").withText(text).notHidden();
    }

    public static XPathLocator navTreeExpander(String nodeText)
    {
        return tag("tr").withClass("labkey-nav-tree-row").withText(nodeText).append("/td").withClass("labkey-nav-tree-node").append("/a");
    }

    public static XPathLocator extButton(String text)
    {
        return  tagWithClass("button", "x-btn-text").notHidden().withText(text);
    }

    public static XPathLocator extButtonEnabled(String text)
    {
        return tag("table").withClass("x-btn").withoutClass("x-item-disabled").append("//button").withClass("x-btn-text").withText(text);
    }

    public static XPathLocator extButtonContainingText(String text)
    {
        return tag("button").withClass("x-btn-text").containing(text);
    }

    public static XPathLocator lkButtonDisabled(String text)
    {
        return tag("span").withPredicate("normalize-space(@class)='labkey-disabled-button' or normalize-space(@class)='labkey-disabled-menu-button'").withText(text);
    }

    public static XPathLocator lkButtonContainingText(String text)
    {
        return lkButton().containing(text);
    }

    public static XPathLocator linkWithText(String text)
    {
        return new LinkLocator(text);
    }

    public static XPathLocator linkContainingText(String text)
    {
        return tag("a").containing(text);
    }

    public static XPathLocator menuItem(String text)
    {
        return tag("a").child(tag("span").withAttributeContaining("class", "menu-item-text").withText(text)).notHidden();
    }

    public static XPathLocator menuBarItem(String text)
    {
        return tagWithClass("div", "navbar-header")
                .childTag("ul")
                .childTag("li").withClass("dropdown")
                .childTag("a").withText(text);
    }

    public static XPathLocator linkWithTitle(String title)
    {
        return tag("a").withAttribute("title", title);
    }

    public static XPathLocator linkWithHref(String url)
    {
        return tag("a").withAttributeContaining("href", url);
    }

    public static XPathLocator linkWithId(String id)
    {
        return tag("a").withAttributeContaining("id", id);
    }

    public static XPathLocator linkWithSpan(String text)
    {
        return tag("a").append(tag("span").containing(text));
    }

    public static XPathLocator bodyLinkContainingText(String text)
    {
        return Locators.bodyPanel().append(linkContainingText(text));
    }

    public static XPathLocator input(String name)
    {
        return tagWithName("input", name);
    }

    public static XPathLocator inputByNameContaining(String partialName)
    {
        return tag("input").withAttribute("type", "text").withAttributeContaining("name", partialName);
    }

    public static XPathLocator inputByIdContaining(String partialName)
    {
        return tag("input").withAttribute("type", "text").withAttributeContaining("id", partialName);
    }

    public static XPathLocator inputById(String id)
    {
        return tagWithId("input", id);
    }

    public static XPathLocator textarea(String name)
    {
        return tagWithName("textarea", name);
    }

    public static XPathLocator textAreaByNameContaining(String partialName)
    {
        return tag("textarea").withAttributeContaining("name", partialName);
    }

    public static XPathLocator checkboxByTitle(String title)
    {
        return checkbox().withAttribute("title", title);
    }

    public static XPathLocator radioButtonByName(String name)
    {
        return radioButton().withAttribute("name", name);
    }

    public static XPathLocator checkboxByLabel(String labelText, boolean labelBeforeBox)
    {
        XPathLocator label = tagWithText("label", labelText);
        if (labelBeforeBox)
            return label.followingSibling("input").withAttribute("type", "checkbox");
        else
            return label.precedingSibling("input").withAttribute("type", "checkbox");
    }

    public static XPathLocator checkboxByName(String name)
    {
        return checkbox().withAttribute("name", name);
    }

    public static XPathLocator radioButtonById(String id)
    {
        return radioButton().withAttribute("id", id);
    }

    public static XPathLocator checkboxById(String id)
    {
        return checkbox().withAttribute("id", id);
    }

    public static CssLocator checkedRadioInGroup(String groupName)
    {
        return css("input:checked[name=" + groupName + "][type=radio]");
    }

    public static XPathLocator radioButtonByNameAndValue(String name, String value)
    {
        return radioButtonByName(name).withAttribute("value", value);
    }

    public static XPathLocator checkboxByNameAndValue(String name, String value)
    {
        return checkboxByName(name).withAttribute("value", value);
    }

    public static XPathLocator checkbox()
    {
        return tag("input").withAttribute("type", "checkbox");
    }

    public static XPathLocator ehrCheckbox()
    {
        return tag("input").withAttribute("type", "button");
    }

    public static XPathLocator ehrCheckboxIdContaining(String partialId)
    {
        return ehrCheckbox().withAttributeContaining("id", partialId);
    }

    public static XPathLocator ehrCheckboxWithLabel(String label)
    {
        return ehrCheckbox().followingSibling("label").withText(label);
    }

    public static XPathLocator radioButton()
    {
        return tag("input").withAttribute("type", "radio");
    }

    public static XPathLocator imageMapLinkByTitle(String imageMapName, String title)
    {
        return tag("map").withAttribute("name", imageMapName).child(tag("area").withAttribute("title", title));
    }

    public static XPathLocator lookupLink(String schemaName, String queryName, String pkName)
    {
        String linkText = schemaName + "." + queryName + "." + (null != pkName ? pkName : "");
        return tagWithClass("span", "labkey-link").containing(linkText);
    }

    public static XPathLocator gwtTextBoxByLabel(String label)
    {
        return tagWithClass("input", "gwt-TextBox").withPredicate(xpath("../preceding-sibling::td").withText(label));
    }

    public static XPathLocator gwtListBoxByLabel(String label)
    {
        return tagWithClass("select", "gwt-ListBox").withPredicate(xpath("../preceding-sibling::label").withText(label));
    }

    public static XPathLocator gwtCheckBoxOnImportGridByColLabel(String label)
    {
        return tagWithAttribute("input", "type", "checkbox").withPredicate(xpath("../../following-sibling::td/span").containing(label));
    }

    public static Locator permissionRendered()
    {
        return Locators.pageSignal("policyRendered");
    }

    public static XPathLocator permissionButton(String groupName, String role)
    {
        // Supports permission types from a variety of modules.
        return tag("div").withClass("rolepanel").withDescendant(tag("h3").withText(role)).append(tag("a").withClass("x4-btn").withDescendant(tag("span").startsWith(groupName)));
    }

    public static XPathLocator closePermissionButton(String groupName, String role)
    {
        // Supports permission types from a variety of modules.
        return permissionButton(groupName, role).append(tag("span").withClass("closeicon"));
    }

    public static XPathLocator permissionsTreeNode(String folderName)
    {
        return tagWithClass("a", "x-tree-node-anchor").child("span[text()='" + folderName + "' or text()='" + folderName + "*']");
    }

    @Deprecated
    public static XPathLocator divByInnerText(String text)
    {
        return xpath("//div[.='" + text + "']");
    }

    public static IdLocator folderTab(String text)
    {
        if ("+".equals(text))
            return id("addTab");
        else
            return id(text.replace(" ", "") + "Tab");
    }

    public static XPathLocator paginationText(int firstRow, int lastRow, int maxRows)
    {
        DecimalFormat numFormat = new DecimalFormat("#,###");

        String paginationText = String.format("%s - %s of %s", numFormat.format(firstRow), numFormat.format(lastRow), numFormat.format(maxRows));
        return paginationText().child(Locator.tagWithText("a", paginationText));
    }

    /**
     * Pagination text for dataregion with one page of data
     */
    public static XPathLocator paginationText(int rowCount)
    {
        return paginationText(1, rowCount, rowCount);
    }

    public static XPathLocator paginationText()
    {
        return tagWithClassContaining("div", "paging-widget");
    }

    public static XPathLocator pageHeader(String headerText)
    {
        return Locators.bodyTitle(headerText);
    }

    /**
     * Quote text to be used as literal string in xpath expression
     *     Direct port from attibuteValue function in selenium IDE locatorBuilders.js
     * @param value to be quoted
     * @return value with either ' or " around it or assembled from parts
     */
    public static String xq(String value)
    {
        if (!value.contains("'")) {
            return "'" + value + "'";
        } else if (!value.contains("\"")) {
            return '"' + value + '"';
        } else {
            StringBuilder result = new StringBuilder("concat(");
            while (true) {
                int apos = value.indexOf("'");
                int quot = value.indexOf('"');
                if (apos < 0) {
                    result.append("'").append(value).append("'");
                    break;
                } else if (quot < 0) {
                    result.append('"').append(value).append('"');
                    break;
                } else if (quot < apos) {
                    String part = value.substring(0, apos);
                    result.append("'").append(part).append("'");
                    value = value.substring(part.length());
                } else {
                    String part = value.substring(0, quot);
                    result.append('"').append(part).append('"');
                    value = value.substring(part.length());
                }
                result.append(',');
            }
            result.append(')');
            return result.toString();
        }
    }

    public static String cq(String value)
    {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static class XPathCSSLocator extends XPathLocator
    {
        private final XPathLocator _xLoc;
        private final CssLocator _cssLoc;

        protected XPathCSSLocator(String tag)
        {
            this(new XPathLocator("//" + tag), new CssLocator(tag));
        }

        protected XPathCSSLocator(XPathLocator xLoc, CssLocator cssLoc)
        {
            super(xLoc.getLoc());
            _xLoc = xLoc;
            _cssLoc = cssLoc;
        }

        protected XPathCSSLocator(XPathCSSLocator copy)
        {
            this(copy._xLoc, copy._cssLoc);
        }

        @Override
        public XPathLocator containing(String contains)
        {
            return _xLoc.containing(contains);
        }

        @Override
        public XPathLocator withText(String text)
        {
            return _xLoc.withText(text);
        }

        @Override
        public XPathLocator index(Integer index)
        {
            if (index == 0)
                return this;
            return _xLoc.index(index);
        }

        @Override
        public XPathLocator withClass(String cssClass)
        {
            return new XPathCSSLocator(
                    _xLoc.withClass(cssClass),
                    _cssLoc.withClass(cssClass));
        }

        @Override
        public XPathLocator withClasses(String... cssClasses)
        {
            return new XPathCSSLocator(
                    _xLoc.withClasses(cssClasses),
                    _cssLoc.withClasses(cssClasses));
        }

        @Override
        public XPathLocator withoutClass(String cssClass)
        {
            return new XPathCSSLocator(
                    _xLoc.withoutClass(cssClass),
                    _cssLoc.withoutClass(cssClass));
        }

        @Override
        public XPathLocator withAttribute(String attribute, String text)
        {
            return new XPathCSSLocator(
                    _xLoc.withAttribute(attribute, text),
                    _cssLoc.withAttribute(attribute, text));
        }

        @Override
        public XPathLocator withAttribute(String attribute)
        {
            return new XPathCSSLocator(
                    _xLoc.withAttribute(attribute),
                    _cssLoc.withAttribute(attribute));
        }

        @Override
        public XPathLocator withAttributeContaining(String attribute, String text)
        {
            return new XPathCSSLocator(
                    _xLoc.withAttributeContaining(attribute, text),
                    _cssLoc.withAttributeContaining(attribute, text));
        }

        @Override
        public XPathLocator attributeStartsWith(String attribute, String text)
        {
            return new XPathCSSLocator(
                    _xLoc.attributeStartsWith(attribute, text),
                    _cssLoc.attributeStartsWith(attribute, text));
        }

        @Override
        public XPathLocator attributeEndsWith(String attribute, String text)
        {
            return new XPathCSSLocator(
                    _xLoc.attributeEndsWith(attribute, text),
                    _cssLoc.attributeEndsWith(attribute, text));
        }

        @Override
        public XPathLocator withoutAttribute(String attribute)
        {
            return new XPathCSSLocator(
                    _xLoc.withoutAttribute(attribute),
                    _cssLoc.withoutAttribute(attribute));
        }

        @Override
        public XPathLocator followingSibling(String tag)
        {
            return new XPathCSSLocator(
                    _xLoc.followingSibling(tag),
                    _cssLoc.followingSibling(tag));
        }

        @Override
        public XPathLocator child(XPathLocator childLocator)
        {
            XPathLocator xLoc = _xLoc.child(childLocator);
            if (!(childLocator instanceof XPathCSSLocator))
                return xLoc;

            return new XPathCSSLocator(
                    xLoc,
                    _cssLoc.child(((XPathCSSLocator)childLocator)._cssLoc));
        }

        @Override
        public XPathLocator last()
        {
            return new XPathCSSLocator(
                    _xLoc.last(),
                    _cssLoc.lastOfType());
        }

        @Override
        public XPathLocator append(XPathLocator child)
        {
            XPathLocator xLoc = _xLoc.append(child);
            if (!(child instanceof XPathCSSLocator))
                return xLoc;

            return new XPathCSSLocator(
                    xLoc,
                    _cssLoc.append(((XPathCSSLocator)child)._cssLoc));
        }

        public CssLocator append(CssLocator child)
        {
            return _cssLoc.append(child);
        }

        public XPathLocator getXPathLoc()
        {
            if (_xLoc instanceof XPathCSSLocator)
                return ((XPathCSSLocator)_xLoc).getXPathLoc();
            else
                return _xLoc;
        }

        public CssLocator getCssLoc()
        {
            return _cssLoc;
        }

        @Override
        public String getLoc()
        {
            return _xLoc.getLoc();
        }

        @Override
        public String toString()
        {
            return _cssLoc.toString();
        }

        @Override
        protected By getBy()
        {
            return _cssLoc.getBy();
        }

        @Override
        protected By getRelativeBy()
        {
            return getBy();
        }
    }

    public static class XPathLocator extends Locator
    {
        protected XPathLocator(String loc)
        {
            super(loc);
        }

        @Override
        public XPathLocator containing(String contains)
        {
            if (contains != null && !contains.isEmpty())
                return this.withPredicate("contains(normalize-space(), "+xq(contains)+")");
            else
                return this;
        }

        public XPathLocator containingIgnoreCase(String contains)
        {
            if (contains != null && !contains.isEmpty())
                return this.withPredicate("contains(translate(normalize-space(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), "+xq(contains.toLowerCase())+")");
            else
                return this;
        }

        public XPathLocator notContaining(String contains)
        {
            return this.withPredicate("not(contains(normalize-space(), "+xq(contains)+"))");
        }

        public XPathLocator notContainingIgnoreCase(String contains)
        {
            if (contains != null && !contains.isEmpty())
                return this.withPredicate("not(contains(translate(normalize-space(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), "+xq(contains.toLowerCase())+"))");
            else
                return this;
        }

        @Override
        public XPathLocator withText(String text)
        {
            return this.withPredicate("normalize-space()="+xq(text));
        }

        public XPathLocator withText()
        {
            return this.withPredicate("string-length() > 0");
        }

        public XPathLocator withoutText(String text)
        {
            return this.withPredicate("not(normalize-space()=" + xq(text) + ")");
        }

        public XPathLocator withoutText()
        {
            return this.withPredicate("string-length() == 0");
        }

        public XPathLocator withTextMatching(String regex)
        {
            return this.withPredicate("matches(normalize-space(), " + xq(regex) + ")");
        }

        public XPathLocator startsWith(String text)
        {
            return this.withPredicate("starts-with(normalize-space(), "+xq(text)+")");
        }

        public XPathLocator startsWithIgnoreCase(String text)
        {
            return this.withPredicate("starts-with(translate(normalize-space(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), "+xq(text.toLowerCase())+")");
        }

        @Override
        public XPathLocator index(Integer index)
        {
            return new XPathLocator("("+getLoc()+")["+(index+1)+"]");
        }

        @Override
        protected By getBy()
        {
            return By.xpath(getLoc());
        }

        public XPathLocator parent()
        {
            return new XPathLocator(getLoc() + "/..");
        }

        /**
         * Only select parent if it is tag
         * @param tag of parent
         * @return xpath locator
         */
        public XPathLocator parent(String tag)
        {
            return new XPathLocator(getLoc() + "/parent::" + tag);
        }

        public XPathLocator child(String str)
        {
            return new XPathLocator(getLoc() + "/" + str);
        }

        public XPathLocator descendant(String str)
        {
            return new XPathLocator(getLoc() + "//" + str);
        }

        public XPathLocator descendant(XPathLocator descendantLocator)
        {
            return this.descendant(stripLeadingSlashes(descendantLocator.getLoc()));
        }

        public XPathLocator childTag(String tag)
        {
            return this.child(tag(tag));
        }

        public XPathLocator child(XPathLocator childLocator)
        {
            return this.child(stripLeadingSlashes(childLocator.getLoc()));
        }

        public XPathLocator withChild(XPathLocator childLocator)
        {
            return this.withPredicate(stripLeadingSlashes(childLocator.getLoc()));
        }

        private String stripLeadingSlashes(String xpath)
        {
            String strippedXPath = xpath;

            while(strippedXPath.startsWith("/"))
            {
                strippedXPath = strippedXPath.substring(1);
            }

            return strippedXPath;
        }

        public XPathLocator last()
        {
            return withPredicate("last()");
        }

        public XPathLocator append(String clause)
        {
            return new XPathLocator(getLoc() + clause);
        }

        public XPathLocator append(XPathLocator child)
        {
            return append(child.getLoc());
        }

        public XPathLocator hidden()
        {
            return this.withPredicate(HIDDEN);
        }

        public XPathLocator notHidden()
        {
            return this.withPredicate(NOT_HIDDEN);
        }

        public XPathLocator enabled()
        {
            return this.withPredicate(ENABLED);
        }

        public XPathLocator withDescendant(XPathLocator descendant)
        {
            return this.withPredicate(descendant.getLoc());
        }

        public XPathLocator withPredicate(XPathLocator descendant)
        {
            return this.withPredicate(descendant.getLoc());
        }

        public XPathLocator withPredicate(String predicate)
        {
            return this.append("[" + getRelativeXPath(predicate) + "]");
        }

        public XPathLocator withoutPredicate(String predicate)
        {
            return this.append("[not(" + getRelativeXPath(predicate) + ")]");
        }

        public XPathLocator withoutPredicate(XPathLocator predicate)
        {
            return this.append("[not(" + getRelativeXPath(predicate.toXpath()) + ")]");
        }

        public XPathLocator attributeStartsWith(String attribute, String text)
        {
            return this.withPredicate(String.format("starts-with(@%s, %s)", attribute, xq(text)));
        }

        public XPathLocator attributeEndsWith(String attribute, String substring)
        {
            return this.endsWith("@" + attribute, substring);
        }

        public XPathLocator withLabel(String labelText)
        {
            return withAttributeMatchingOtherElementAttribute("id", Locator.tagWithText("label", labelText), "for");
        }

        public XPathLocator withLabelContaining(String labelText)
        {
            return withAttributeMatchingOtherElementAttribute("id", Locator.tagContainingText("label", labelText), "for");
        }

        public XPathLocator withAttributeMatchingOtherElementAttribute(String attribute, XPathLocator otherElement, String otherAttribute)
        {
            return this.withPredicate(String.format("@%s = %s/@%s", attribute, otherElement.toXpath(), otherAttribute));
        }

        public XPathLocator endsWith(String substring)
        {
            return this.endsWith("normalize-space()", substring);
        }

        private XPathLocator endsWith(String expression, String substring)
        {
            return this.withPredicate(String.format("substring(%s, string-length(%s) - %d) = %s", expression, expression, substring.length() - 1, xq(substring))); // XPath 1.0 doesn't support ends-with()
        }

        public XPathLocator withClass(String cssClass)
        {
            cssClass = Locator.normalizeCssClass(cssClass);
            return this.withPredicate("contains(concat(' ',normalize-space(@class),' '), " + xq(" " + cssClass + " ") + ")");
        }

        public XPathLocator withoutClass(String cssClass)
        {
            cssClass = Locator.normalizeCssClass(cssClass);
            return this.withoutPredicate("contains(concat(' ',normalize-space(@class),' '), " + xq(" " + cssClass + " ") + ")");
        }

        public XPathLocator withClasses(String... cssClasses)
        {
            XPathLocator result = this;
            for (String cssClass : cssClasses)
            {
                result = result.withClass(cssClass);
            }
            return result;
        }

        public XPathLocator withAttribute(String attrName, String attrVal)
        {
            return this.withPredicate("@" + attrName + "=" + xq(attrVal));
        }

        public XPathLocator withAttribute(String attrName)
        {
            return this.withPredicate("@" + attrName);
        }

        public XPathLocator withAttributeContaining(String attrName, String partialAttrVal)
        {
            return this.withPredicate("contains(@" + attrName + ", " + xq(partialAttrVal) + ")");
        }

        public XPathLocator withAttributeIgnoreCase(String attrName, String attrVal)
        {
            return this.withPredicate(
                    String.format("translate(@%s, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')=%s",
                            attrName, xq(attrVal.toLowerCase())));
        }

        public XPathLocator withoutAttribute(String attrName, String attrVal)
        {
            return this.withoutPredicate("@" + attrName + "=" + xq(attrVal));
        }

        public XPathLocator withoutAttribute(String attrName)
        {
            return this.withoutPredicate("@" + attrName);
        }

        public XPathLocator withoutAttributeContaining(String attrName, String partialAttrVal)
        {
            return this.withoutPredicate("contains(@" + attrName + ", " + xq(partialAttrVal) + ")");
        }

        public XPathLocator followingSibling(String tag)
        {
            return this.append("/following-sibling::" + tag);
        }

        public XPathLocator precedingSibling(String tag)
        {
            return this.append("/preceding-sibling::" + tag);
        }

        public XPathLocator position(int pos)
        {
            return this.withPredicate(String.valueOf(pos));
        }

        //Experimental. Might not work quite right within non-global SearchContext
        public static XPathLocator union(XPathLocator... locators)
        {
            if (locators.length < 1)
                throw new IllegalArgumentException("Specify at least one locator");
            if (locators.length == 1)
                return locators[0];
            List<String> xpaths = new ArrayList<>(locators.length);
            for (XPathLocator locator : locators)
            {
                xpaths.add(locator.getRelativeXPath());
            }
            return new XPathLocator("((" + String.join(")|(", xpaths) + "))");
        }

        public String toString()
        {
            return "xpath="+toXpath();
        }

        public String toXpath()
        {
            return getLoc();
        }

        @Override
        public List<WebElement> findElements(SearchContext context)
        {
            if (!(context instanceof WebDriver || context instanceof WrapsDriver) || context instanceof WebElement)
                return decorateWebElements(context.findElements(getRelativeBy()));
            else
                return decorateWebElements(context.findElements(this.getBy()));
        }

        protected By getRelativeBy()
        {
            return By.xpath(getRelativeXPath());
        }

        private String getRelativeXPath()
        {
            return getRelativeXPath(getLoc());
        }

        private String getRelativeXPath(String xpath)
        {
            if (xpath.startsWith("//") || xpath.startsWith("(//"))
                xpath = xpath.replaceFirst("//", ".//");
            return xpath;
        }
    }

    public static class IdLocator extends XPathCSSLocator
    {
        private final String _id;

        protected IdLocator(String id)
        {
            super(Locator.xpath("//*").withAttribute("id", id), CssLocator.forId(id));
            _id = id.contains(" ") ? null : id;
            if (_id == null)
            {
                TestLogger.warn(String.format("Element has an invalid ID: '%s'", id));
            }
        }

        @Override
        protected By getBy()
        {
            return _id == null ? super.getBy() : By.id(_id);
        }

        public String toString()
        {
            return _id == null ? super.toString() : "id=" + _id;
        }
    }

    public static class CssLocator extends Locator
    {
        protected CssLocator(String loc)
        {
            super(loc);
        }

        private CssLocator(String loc, Integer index, String contains, String text)
        {
            super(loc, index, contains, text);
        }

        protected static CssLocator forId(String id)
        {
            String selector = id.contains(" ") ? "[id=" + cq(id) + "]" : "#" + id;
            return new CssLocator(selector);
        }

        public static Locator union(CssLocator... locators)
        {
            if (locators.length == 0)
                throw new IllegalArgumentException("Specify one or more locators to union");

            for (Locator loc : locators)
            {
                if (loc._contains != null || loc._text != null || loc._index != null)
                    throw new IllegalArgumentException("Only able to union raw CSS selectors");
            }

            StringBuilder unionedLocators = new StringBuilder();
            unionedLocators.append(locators[0].getLoc());
            for (int i = 1; i < locators.length; i++)
            {
                unionedLocators.append(", ");
                unionedLocators.append(locators[i].getLoc());
            }

            return new WrappedLocator(new CssLocator(unionedLocators.toString()));
        }

        @Override
        public Locator containing(String contains)
        {
            if (_text != null && _text.length() > 0 || _contains != null && _contains.length() > 0)
                throw new IllegalStateException("Text content already been specified for this Locator");

            return new CssLocator(getLoc(), _index, contains, _text);
        }

        @Override
        public Locator withText(String text)
        {
            if (_text != null && _text.length() > 0 || _contains != null && _contains.length() > 0)
                throw new IllegalStateException("Text content already been specified for this Locator");

            return new CssLocator(getLoc(), _index, _contains, text);
        }

        /**
         * Locate the nth element matched by the selector. Can only find a single element.
         * @param index zero-based index of desired element
         * @return Locator to find the nth instance of the base selector
         */
        @Override
        public Locator index(Integer index)
        {
            if (_index != null && _index != 0)
                throw new IllegalArgumentException("An index has already been specified for this Locator");

            return new CssLocator(getLoc(), index, _contains, _text);
        }

        public CssLocator append(final String clause)
        {
            if (clause.isEmpty())
                return this;
            List<String> elementSeparators = Arrays.asList(" ", ",", ">", "+", "~");
            if (StringUtils.endsWith(getLoc(), "*") && !elementSeparators.contains(clause.substring(clause.length())))
                return new CssLocator(getLoc().substring(0, getLoc().length() - 1) + clause); // *.class becomes .class
            else
                return new CssLocator(getLoc() + clause);
        }

        public CssLocator append(CssLocator clause)
        {
            return append(" " + clause.getLoc());
        }

        public CssLocator withAttribute(String attribute, String text)
        {
            return append("[" + attribute + "=" + cq(text) + "]");
        }

        public CssLocator withAttribute(String attribute)
        {
            return append("[" + attribute + "]");
        }

        public CssLocator withoutAttribute(String attribute)
        {
            return append(":not([" + attribute + "])");
        }

        public CssLocator withAttributeContaining(String attribute, String text)
        {
            return append("[" + attribute + "*=" + cq(text) + "]");
        }

        public CssLocator attributeStartsWith(String attribute, String text)
        {
            return append("[" + attribute + "^=" + cq(text) + "]");
        }

        public CssLocator attributeEndsWith(String attribute, String text)
        {
            return append("[" + attribute + "$=" + cq(text) + "]");
        }

        public CssLocator followingSibling(String tag)
        {
            return append(" ~ " + tag);
        }

        public CssLocator withClass(String cssClass)
        {
            cssClass = Locator.normalizeCssClass(cssClass);
            return withClasses(cssClass.split(" +"));
        }

        public CssLocator withClasses(String... cssClasses)
        {
            return append("." + String.join(".", cssClasses));
        }

        public CssLocator withoutClass(String cssClass)
        {
            cssClass = Locator.normalizeCssClass(cssClass);
            return append(":not(." + cssClass + ")");
        }

        public CssLocator child(CssLocator childLocator)
        {
            return child(childLocator.getLoc());
        }

        public CssLocator child(String childSelector)
        {
            return append(" > " + childSelector);
        }

        public CssLocator lastChild()
        {
            return append(":last-child");
        }

        public CssLocator lastOfType()
        {
            return append(":last-of-type");
        }

        @Override
        public String toString()
        {
            return "css=" + getLoc();
        }

        @Override
        protected By getBy()
        {
            if (getLoc().contains(":contains("))
                throw new IllegalArgumentException("CSS3 does not support the ':contains' pseudo-class: '" + getLoc() + "'");
            return By.cssSelector(getLoc());
        }
    }

    public static class LinkLocator extends XPathLocator
    {
        private String _linkText;

        public LinkLocator(@NotNull String linkText)
        {
            super(tag("a").withText(linkText).toXpath());
            _linkText = linkText;
        }

        @Override
        public List<WebElement> findElements(SearchContext context)
        {
            List<WebElement> elements;
            try
            {
                String w3CLinkText = getW3CLinkText(context);
                if (w3CLinkText.equals(_linkText))
                    elements = super.findElements(context);
                else
                    return new LinkLocator(w3CLinkText).findElements(context);
            }
            catch (InvalidSelectorException retry)
            {
                // By.linkText doesn't allow all possible link texts. e.g. "[All]"
                return new XPathLocator(getLoc()).findElements(context);
            }
            if (elements.size() == 0 && !_linkText.equals(_linkText.toUpperCase()))
                return (new LinkLocator(_linkText.toUpperCase())).findElements(context);
            else
                return elements;
        }

        /**
         * Geckodriver follows spec more closely than some tests expect. Need to trim whitespace to find links by text.
         * <a href='https://www.w3.org/TR/webdriver1/#link-text'>WebDriver Spec</a>
         * @return Link text appropriate to the current WebDriver instance (if able to be determined)
         */
        private String getW3CLinkText(SearchContext context)
        {
            // We need to do this every time because many Locators are defined statically
            WebDriver webDriver = WebDriverUtils.extractWrappedDriver(context);
            if (webDriver instanceof FirefoxDriver)
            {
                return _linkText.replaceAll(NBSP, " ").trim();
            }
            return _linkText;
        }

        @Override
        public String toString()
        {
            return "link=" + _linkText;
        }

        @Override
        protected By getBy()
        {
            return By.linkText(_linkText);
        }
    }

    private static String normalizeCssClass(String cssClass)
    {
        return StringUtils.strip(cssClass, " .");
    }
}
