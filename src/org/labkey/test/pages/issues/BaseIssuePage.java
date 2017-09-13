/*
 * Copyright (c) 2016-2017 LabKey Corporation
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
package org.labkey.test.pages.issues;

import com.google.common.collect.ImmutableList;
import org.labkey.remoteapi.collections.CaseInsensitiveHashMap;
import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.FormItem;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.html.OptionSelect;
import org.labkey.test.components.labkey.ReadOnlyFormItem;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.labkey.test.components.html.Input.Input;
import static org.labkey.test.components.html.OptionSelect.OptionSelect;
import static org.labkey.test.pages.issues.IssuesFormItemFinder.IssueFormItem;
import static org.labkey.test.pages.issues.IssuesReadOnlyFormItem.IssueReadOnlyFormItem;

public abstract class BaseIssuePage<EC extends BaseIssuePage.ElementCache> extends LabKeyPage<EC>
{
    protected BaseIssuePage(WebDriver driver)
    {
        super(driver);
    }

    public FormItem<String> status()
    {
        return elementCache().status;
    }

    public FormItem<String> assignedTo()
    {
        return elementCache().assignedTo;
    }

    public FormItem<String> priority()
    {
        return elementCache().priority;
    }

    public FormItem<String> related()
    {
        return elementCache().related;
    }

    public FormItem<String> resolution()
    {
        return elementCache().resolution;
    }

    public FormItem<String> duplicate()
    {
        return elementCache().duplicate;
    }

    public FormItem<String> notifyList()
    {
        return elementCache().notifyList;
    }

    public FormItem<Object> getCustomField(String label)
    {
        return elementCache().formItemWithLabel(label);
    }

    public String openedDate()
    {
        return (String) elementCache().openedDate.get();
    }

    public String closedDate()
    {
        return (String) elementCache().closedDate.get();
    }

    public String changedDate()
    {
        return (String) elementCache().changedDate.get();
    }

    public String resolvedDate()
    {
        return (String) elementCache().resolvedDate.get();
    }

    public List<IssueComment> getComments()
    {
        return elementCache().getComments();
    }

    public String getIssueId()
    {
        return WebTestHelper.parseUrlQuery(getURL()).get("issueId");
    }

    protected abstract EC newElementCache();

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        protected final Map<String, FormItem> formItems = new CaseInsensitiveHashMap<>();

        protected FormItem<String> status = formItemWithLabel("Status");
        protected FormItem<String> assignedTo = formItemWithLabel("Assigned To");
        protected FormItem<String> priority = formItemWithLabel("Pri");
        protected FormItem<String> resolution = formItemWithLabel("Resolution");
        protected FormItem<String> duplicate = formItemWithLabel("Duplicate");
        protected FormItem<String> related = formItemWithLabel("Related");
        protected FormItem<String> notifyList = formItemWithLabel("Notify");
        protected FormItem<String> openedDate = formItemWithLabel("Opened");
        protected FormItem<String> changedDate = formItemWithLabel("Changed");
        protected FormItem<String> resolvedDate = formItemWithLabel("Resolved");
        protected FormItem<String> closedDate = formItemWithLabel("Closed");

        private FormItem replaceIfNewer(String nameOrLabel, FormItem candidate)
        {
            String key = nameOrLabel.replaceAll("\\s", "");
            FormItem formItem = formItems.get(key);
            if (formItem == null || !(candidate.getClass().isAssignableFrom(formItem.getClass())))
                formItems.put(key, candidate); // Replace with more specific or different FormItem
            return formItems.get(key);
        }

        protected <T> FormItem<T> formItemWithLabel(String label)
        {
            return replaceIfNewer(label, IssueFormItem(getDriver()).withLabel(label).timeout(1000).findWhenNeeded(this));
        }

        protected <T> FormItem<T> formItemNamed(String name)
        {
            return replaceIfNewer(name, IssueFormItem(getDriver()).withName(name).timeout(1000).findWhenNeeded(this));
        }

        protected ReadOnlyFormItem readOnlyItem(String label)
        {
            return (ReadOnlyFormItem) replaceIfNewer(label, IssueReadOnlyFormItem().withLabel(label).timeout(1000).findWhenNeeded(this));
        }

        protected OptionSelect getSelect(String name)
        {
            FormItem formItem = replaceIfNewer(name, OptionSelect(fieldLocator(name)).timeout(1000).findWhenNeeded(this));
            return (OptionSelect) formItem;
        }

        protected Input getInput(String name)
        {
            FormItem formItem = replaceIfNewer(name, Input(fieldLocator(name), getDriver()).timeout(1000).findWhenNeeded(this));
            return (Input) formItem;
        }

        // Compensate for inconsistent name casing
        private Locator fieldLocator(String name)
        {
            return Locator.css(String.format("*[name=%s], *[name=%s]", name, name.toLowerCase()));
        }

        private List<IssueComment> issueComments;
        protected List<IssueComment> getComments()
        {
            if (issueComments == null)
            {
                List<WebElement> commentEls = Locator.css("div.currentIssue").findElements(this);
                issueComments = new ArrayList<>();
                for (WebElement commentEl : commentEls)
                {
                    issueComments.add(new IssueComment(commentEl));
                }
                issueComments = ImmutableList.copyOf(issueComments);
            }

            return issueComments;
        }
    }

    public class IssueComment
    {
        private final WebElement component;
        private String user;
        private String timestamp;
        private String comment;
        private Map<String, String> fieldChanges;

        IssueComment(WebElement component)
        {
            this.component = component;
        }

        public String getUser()
        {
            if (user == null)
                user = Locator.css(".comment-created-by").findElement(component).getText();
            return user;
        }

        public String getTimestamp()
        {
            if (timestamp == null)
                timestamp = Locator.css(".comment-created").findElement(component).getText();
            return timestamp;
        }

        public String getComment()
        {
            if (comment == null)
                comment = Locator.css(".labkey-wiki").findElement(component).getText();
            return comment;
        }

        public Map<String, String> getFieldChanges()
        {
            if (fieldChanges == null)
            {
                fieldChanges = new HashMap<>();
                List<String> changes = getTexts(Locator.css(".issues-Changes tr").findElements(component));

                for (String change : changes)
                {
                    Pattern pattern = Pattern.compile("(.+)\uc2bb(.*)");
                    Matcher matcher = pattern.matcher(change);

                    if (matcher.find())
                    {
                        String field = matcher.group(1).trim();
                        String value = matcher.group(2).trim();
                        fieldChanges.put(field, value);
                    }
                }
            }
            return fieldChanges;
        }
    }
}