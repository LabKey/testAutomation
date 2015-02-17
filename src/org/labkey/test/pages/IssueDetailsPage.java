package org.labkey.test.pages;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IssueDetailsPage extends LabKeyPage
{
    Map<String, String> issueFields;
    List<IssueComment> issueComments;

    public IssueDetailsPage(BaseWebDriverTest test)
    {
        super(test);
    }

    public Map<String, String> getFields()
    {
        if (issueFields == null)
        {
            issueFields = new HashMap<>();
            List<WebElement> fields = Locators.field.findElements(_test.getDriver());

            for (WebElement field : fields)
            {
                String fieldName = field.findElement(By.cssSelector("td:nth-of-type(1)")).getText();
                String fieldValue = field.findElement(By.cssSelector("td:nth-of-type(2)")).getText();

                issueFields.put(fieldName, fieldValue);
            }
        }

        return issueFields;
    }

    public String getField(String fieldName)
    {
        return getFields().get(fieldName);
    }

    public List<IssueComment> getComments()
    {
        if (issueComments == null)
        {
            issueComments = new ArrayList<>();
        }

        return issueComments;
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
            {
                user = Locator.css(".comment-created-by").findElement(component).getText();
            }
            return user;
        }

        public String getTimestamp()
        {
            if (timestamp == null)
            {
                timestamp = Locator.css(".comment-created").findElement(component).getText();
            }
            return timestamp;
        }

        public String getComment()
        {
            if (comment == null)
            {
                comment = Locator.css(".labkey-wiki").findElement(component).getText();
            }
            return comment;
        }

        public Map<String, String> getFieldChanges()
        {
            if (fieldChanges == null)
            {
                fieldChanges = new HashMap<>();
                List<String> changes = _test.getTexts(Locator.css(".issues-Changes tr").findElements(component));

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

    public static class Locators extends LabKeyPage.Locators
    {
        public static Locator.XPathLocator fieldPanel = bodyPanel.append(Locator.tagWithClass("table", "issue-fields"));
        public static Locator.XPathLocator field = fieldPanel.append(Locator.tag("tr").withPredicate(Locator.xpath("./td").withClass("labkey-form-label")));
    }
}
