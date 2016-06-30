package org.labkey.test.pages.issues;

import com.google.common.collect.ImmutableList;
import org.labkey.test.Locator;
import org.labkey.test.components.labkey.FormItem;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.labkey.test.components.labkey.ReadOnlyFormItem.ReadOnlyFormItem;

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

    public FormItem getCustomField(String label)
    {
        return elementCache().getCustomFormItem(label);
    }

    public String openedDate()
    {
        return elementCache().openedDate.getValue().toString();
    }

    public String closedDate()
    {
        return elementCache().closedDate.getValue().toString();
    }

    public String changedDate()
    {
        return elementCache().changedDate.getValue().toString();
    }

    public String resolvedDate()
    {
        return elementCache().resolvedDate.getValue().toString();
    }

    public List<IssueComment> getComments()
    {
        return elementCache().getComments();
    }

    protected abstract EC newElementCache();

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        protected FormItem<String> status = ReadOnlyFormItem(getDriver()).withLabel("Status").findWhenNeeded();
        protected FormItem<String> assignedTo = ReadOnlyFormItem(getDriver()).withLabel("Assigned To").findWhenNeeded();
        protected FormItem<String> priority = ReadOnlyFormItem(getDriver()).withLabel("Pri").findWhenNeeded();
        protected FormItem<String> resolution = ReadOnlyFormItem(getDriver()).withLabel("Resolution").findWhenNeeded();
        protected FormItem<String> duplicate = ReadOnlyFormItem(getDriver()).withLabel("Duplicate").findWhenNeeded();
        protected FormItem<String> related = ReadOnlyFormItem(getDriver()).withLabel("Related").findWhenNeeded();
        protected FormItem<String> notifyList = ReadOnlyFormItem(getDriver()).withLabel("Notify").findWhenNeeded();
        protected FormItem<String> openedDate = ReadOnlyFormItem(getDriver()).withLabel("Opened").findWhenNeeded();
        protected FormItem<String> changedDate = ReadOnlyFormItem(getDriver()).withLabel("Changed").findWhenNeeded();
        protected FormItem<String> resolvedDate = ReadOnlyFormItem(getDriver()).withLabel("Resolved").findWhenNeeded();
        protected FormItem<String> closedDate = ReadOnlyFormItem(getDriver()).withLabel("Closed").findWhenNeeded();

        private Map<String, FormItem> customFormItems = new TreeMap<>();
        protected FormItem getCustomFormItem(String label)
        {
            if (!customFormItems.containsKey(label))
                customFormItems.put(label, ReadOnlyFormItem(getDriver()).withLabel(label).findWhenNeeded());
            return customFormItems.get(label);
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