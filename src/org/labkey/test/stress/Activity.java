package org.labkey.test.stress;

import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
import org.labkey.query.xml.TestCaseType;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Activity: A single user action in the app (e.g. viewing the SM dashboard). These will often trigger numerous API calls.
 */
public class Activity
{
    private final String _name;
    private final List<RequestParams> _requests;
    private final AtomicInteger debugCount = new AtomicInteger();

    Activity(String name, List<TestCaseType> requests)
    {
        _name = name;
        _requests = requests.stream().map(RequestParams::new).toList();
        verifyUniqueNames();
    }

    public String getName()
    {
        return _name;
    }

    public List<RequestParams> getRequests()
    {
        return _requests;
    }

    private void verifyUniqueNames()
    {
        Assertions.assertThat(_requests.stream().map(RequestParams::getName)).as("Request names").doesNotHaveDuplicates();
    }

    public class RequestParams
    {
        private final String _name;
        private final String _url;
        private final String _type;
        private final String _formData;

        private RequestParams(TestCaseType testCase)
        {
            _name = testCase.getName().trim();
            // Add dummy '_debug' parameter to differentiate similar requests in request logs
            String url = testCase.getUrl().trim();
            String debugParam = (url.contains("?") ? "&" : "?") + "_debug=" + debugCount.incrementAndGet();
            _url = url + debugParam;
            _type = testCase.getType().trim();
            _formData = StringUtils.trimToEmpty(testCase.getFormData());
        }

        public String getName()
        {
            return _name;
        }

        public String getUrl()
        {
            return _url;
        }

        public String getType()
        {
            return _type;
        }

        public String getFormData()
        {
            return _formData;
        }
    }
}
