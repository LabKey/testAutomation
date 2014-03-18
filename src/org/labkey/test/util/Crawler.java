/*
 * Copyright (c) 2007-2013 LabKey Corporation
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

package org.labkey.test.util;

import com.google.common.base.Function;
import com.thoughtworks.selenium.SeleniumException;
import org.apache.commons.lang3.StringUtils;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;
import org.openqa.selenium.UnhandledAlertException;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import static org.junit.Assert.*;

/**
 * User: brittp
 * Date: Jan 13, 2007
 * Time: 5:11:16 PM
 */
public class Crawler
{
    private final List<ControllerActionId> _excludedActions;
    private final Collection<String> _forbiddenWords;

    // Replacements to make in HTML source before looking for "forbidden" words.
    private static Map<String, String> _sourceReplacements = new HashMap<>();

    static
    {
        // Keys must be all lowercase
        _sourceReplacements.put("http://help.labkey.org/wiki/home/cpas/documentation/", "");   // Allow forbidden word "cpas" in help links
    }

    private static Set<ControllerActionId> _actionsVisited = new HashSet<>();
    private static Set<String> _urlsChecked = new HashSet<>();
    public static ActionProfiler _actionProfiler = new ActionProfiler();
    private UrlToCheck _urlToCheck;
    private int _crawlTime = 0;
    private int _maxDepth = 3;
    private ArrayList<UrlToCheck> _urlsToCheck = new ArrayList<>();

    private int _maxCrawlTime;
    private static final int DEFAULT_CRAWL_TIME = 90000;

    private static Map<String, CrawlStats> _crawlStats = new LinkedHashMap<>();
    private BaseWebDriverTest _test;

    public Crawler(BaseWebDriverTest test)
    {
        this(test, DEFAULT_CRAWL_TIME);
    }

    public Crawler(BaseWebDriverTest test, int crawlTime)
    {
        _test = test;
        _maxCrawlTime = crawlTime;
        _forbiddenWords = getForbiddenWords();
        _excludedActions = getExcludedActions();
    }

    protected Set<String> getForbiddenWords()
    {
        return new HashSet<>();
    }

    protected List<ControllerActionId> getExcludedActions()
    {
        List<ControllerActionId> list = new ArrayList<>();
        Collections.addAll(list, new ControllerActionId("admin", "resetErrorMark"),
            new ControllerActionId("admin", "dbChecker"),
            new ControllerActionId("admin", "runSystemMaintenance"),
            new ControllerActionId("admin", "deleteFolder"),
            new ControllerActionId("admin", "defineWebThemes"),
            new ControllerActionId("admin", "memTracker"),
            new ControllerActionId("admin", "setAdminMode"),
            new ControllerActionId("admin", "dumpHeap"),
            new ControllerActionId("admin", "addTab"),
            new ControllerActionId("admin", "actions"), // Gets hit often in normal testing
            new ControllerActionId("admin", "credits"), // Gets checked by BasicTest
            new ControllerActionId("admin", "showErrorsSinceMark"), // Gets hit often in normal testing
            new ControllerActionId("admin", "resetQueryStatistics"),
            new ControllerActionId("assay", "assayDetailRedirect"),
            new ControllerActionId("core", "downloadFileLink"),
            new ControllerActionId("experiment", "showFile"),
            new ControllerActionId("flow-editscript", "download"),
            new ControllerActionId("flow-run", "download"),
            new ControllerActionId("flow-well", "download"),
            new ControllerActionId("genotyping", "analyze"),    // Crawler doesn't like NotFoundException that the test generates
            new ControllerActionId("issues", "download"),
            new ControllerActionId("login", "logout"),
            new ControllerActionId("login", "enable"),      // TODO: These should be post actions (product issue)
            new ControllerActionId("login", "disable"),      // TODO: These should be post actions (product issue)
            new ControllerActionId("login", "setPassword"),
            new ControllerActionId("ms2", "showParamsFile"),
            // Tested directly in XTandemTest
            new ControllerActionId("ms2", "showPeptide"),
            new ControllerActionId("ms2", "showProtein"), // TODO: 16617: MS1Test imports don't match provided FASTA file
            new ControllerActionId("pipeline-status", "showList"), // Is likely to contain 404 links
            new ControllerActionId("pipeline-status", "providerAction"), // Re-triggers previously expected errors
            new ControllerActionId("project", "deleteWebPart"),
            new ControllerActionId("project", "moveWebPart"),
            new ControllerActionId("query", "printRows"),
            new ControllerActionId("query", "exportRowsExcel"),
            new ControllerActionId("query", "excelWebQueryDefinition"),
            new ControllerActionId("reports", "downloadInputData"),
            new ControllerActionId("reports", "streamFile"),
            new ControllerActionId("reports", "download"),
            new ControllerActionId("security", "resetPassword"),
            new ControllerActionId("study", "confirmDeleteVisit"),
            new ControllerActionId("study", "template"),
            new ControllerActionId("study", "downloadTsv"),
            new ControllerActionId("study", "deleteDatasetReport"),
            new ControllerActionId("study", "deleteDataset"),
            new ControllerActionId("study", "importStudyFromPipeline"),
            new ControllerActionId("study", "protocolDocumentDownload"),
            new ControllerActionId("study-reports", "deleteReports"),
            new ControllerActionId("study-reports", "deleteReport"),
            new ControllerActionId("study-reports", "deleteCustomQuery"),
            new ControllerActionId("study-samples", "downloadSpecimenList"),
            new ControllerActionId("study-samples", "emailLabSpecimenLists"),
            new ControllerActionId("study-samples", "getSpecimenExcel"),
            new ControllerActionId("study-samples", "download"),
            new ControllerActionId("targetedms", "downloadChromLibrary"),
            new ControllerActionId("nabassay", "downloadDatafile"),
            new ControllerActionId("NAb", "download"),           // TODO: I think this controller is gone
            new ControllerActionId("wiki", "download"),

            // Actions from external modules
            new ControllerActionId("targetedms", "downloadDocument"),

            // Script injection exclusions
            new ControllerActionId("idri", "formulationDetails"), // TODO: 16735: Idri.FormulationDetailsAction: NullPointerException on bad 'rowId'
            new ControllerActionId("study-designer", "designer")); // TODO: 16768: study-designer.DesignerAction: IllegalArgumentException on bad 'panel'

        return list;
    }

    protected List<String> getAdminControllers()
    {
        return Arrays.asList("login", "admin", "security", "user");
    }

    protected int getMaxDepth()
    {
        return _maxDepth;
    }

    protected int setMaxDepth(int maxDepth)
    {
        return _maxDepth = maxDepth;
    }

    private void saveCrawlStats(BaseWebDriverTest test, int maxDepth, int newPages, int uniqueActions, int crawlTestLength)
    {
        String testName = test.getClass().getSimpleName();
        _crawlStats.put(testName, new CrawlStats(maxDepth, newPages, uniqueActions, crawlTestLength));
    }

    public static Map<String, CrawlStats> getCrawlStats()
    {
        return _crawlStats;
    }

    public class CrawlStats
    {
        private final int _newPages;
        private final int _uniqueActions;
        private final int _crawlTestLength;
        private final int _maxDepth;

        public CrawlStats(int maxDepth, int newPages, int uniqueActions, int crawlTestLength)
        {
            _newPages = newPages;
            _uniqueActions = uniqueActions;
            _crawlTestLength = crawlTestLength;
            _maxDepth = maxDepth;
        }

        public int getMaxDepth()
        {
            return _maxDepth;
        }

        public int getNewPages()
        {
            return _newPages;
        }

        public int getUniqueActions()
        {
            return _uniqueActions;
        }

        public int getCrawlTestLength()
        {
            return _crawlTestLength;
        }
    }

    private static String getURLBase(URL currentPageURL)
    {
        String urlString = stripQueryParams(currentPageURL.getPath());
        int lastSlashIdx = urlString.lastIndexOf('/');
        if (lastSlashIdx > 0)
            urlString = urlString.substring(0, lastSlashIdx) + "/";
        return urlString;
    }

    private static String stripQueryParams(String url)
    {
        int paramIdx = url.indexOf('?');
        if (paramIdx > 0)
            url = url.substring(0, paramIdx);
        return url;
    }


    private class UrlToCheck
    {
        // Keep track of urls to check for breadth first crawl
        private URL _origin;
        private String _urlText;
        private int _depth;
		private boolean _testInjection;

		public UrlToCheck(URL origin, String urlText, int depth, boolean inject)
		{
			_origin = origin;
			_urlText = urlText;
			_depth = depth;
			_testInjection = inject;
		}

        public URL getOrigin()
        {
            return _origin;
        }

        public String getUrlText()
        {
            return _urlText;
        }

        public int getDepth()
        {
            return _depth;
        }

		public boolean testInjection()
		{
			return _testInjection;
		}
    }


    protected static class ControllerActionId
    {
        private String _controller;
        private String _action = "";
        private String _folder;

        public ControllerActionId(String controller, String action)
        {
            _controller = controller;
            _action = action;
        }

        public ControllerActionId(String rootRelativeURL)
        {
            rootRelativeURL = stripQueryParams(rootRelativeURL);
            rootRelativeURL = BaseWebDriverTest.stripContextPath(rootRelativeURL);

            if (rootRelativeURL.startsWith("_webdav/"))
            {
                _controller = "_webdav";
                _folder = rootRelativeURL.substring("_webdav/".length());
                return;
            }

            int actionIdx = rootRelativeURL.lastIndexOf('/');
            String action = rootRelativeURL.substring(actionIdx + 1);
            if (action.endsWith(".view") || action.endsWith(".api") || action.endsWith(".post"))
            {
                _action = action.substring(0,action.lastIndexOf("."));
                rootRelativeURL = rootRelativeURL.substring(0, actionIdx+1);
            }
            else
            {
                _action = "";
            }

            if (_action.contains("-"))
            {
                /* folders/ */
                int dash = _action.indexOf("-");
                _controller = _action.substring(0,dash);
                _action = _action.substring(dash+1);
            }
            else
            {
                /* controller/folders/ */
                int postControllerSlashIdx = rootRelativeURL.indexOf('/');
                if (-1 == postControllerSlashIdx)
                    fail("Expected to find a slash but didn't in \"" + rootRelativeURL + "\"");
                _controller = rootRelativeURL.substring(0, postControllerSlashIdx);
                rootRelativeURL = rootRelativeURL.substring(postControllerSlashIdx+1);
            }
            _folder = rootRelativeURL;
            if (_folder.endsWith("/"))
                _folder = _folder.substring(0,_folder.length()-1);
        }

        public String getAction()
        {
            return _action;
        }

        public String getController()
        {
            return _controller;
        }

        public String getFolder()
        {
            return _folder;
        }

        @Override
        public int hashCode()
        {
            return (null==_action?0:_action.hashCode()) ^ (null==_controller?0:_controller.hashCode());
        }

        @Override
        public boolean equals(Object obj)
        {
            return obj instanceof ControllerActionId &&
                   _action.equalsIgnoreCase(((ControllerActionId) obj).getAction()) &&
                   _controller.equalsIgnoreCase(((ControllerActionId) obj).getController());
        }
    }

    public static class ActionProfiler
    {
    	private Map<ControllerActionId, ActionProfile> _actionProfiles = new HashMap<>();

        public void updateActionProfile(String relativeUrl, long loadTime)
        {
            ControllerActionId actionId = new ControllerActionId(relativeUrl);
            if (_actionProfiles.containsKey(actionId))
                _actionProfiles.get(actionId).updateActionProfile(relativeUrl, loadTime);
            else
                _actionProfiles.put(actionId, new ActionProfile(relativeUrl, loadTime));
        }

    	private class ActionProfile
    	{
    		private long _invocations;
    		private long _longestLoad;
    		private long _totalTime;
    		private String _urlForLongest;
    		private ControllerActionId _actionId;

    		ActionProfile(String relativeURL, long loadTime)
    		{
    			_actionId = new ControllerActionId(relativeURL);
    			_invocations = 1;
    			_totalTime = _longestLoad = loadTime;
    			_urlForLongest = relativeURL;
    		}

            public void updateActionProfile(String relativeURL, long loadTime)
            {
                if (!_actionId.equals(new ControllerActionId(relativeURL)))
                    throw new IllegalArgumentException("Actions don't match");

                _invocations++;
                _totalTime += loadTime;

                if (loadTime > _longestLoad)
                {
                    _longestLoad = loadTime;
                    _urlForLongest = relativeURL;
                }
            }

            public ControllerActionId getActionId()
            {return _actionId;}

            public String getAction()
            {return _actionId.getAction();}

            public String getController()
            {return _actionId.getController();}

            public long getInvocations()
            {return _invocations;}

            public long getLongestLoad()
            {return _longestLoad;}

            public long getTotalTime()
            {return _totalTime;}
    	}

        public String toHtml()
        {
            StringBuilder sb = new StringBuilder();

            sb.append("<table cellspacing=0 cellpadding=3>\n");
            sb.append("<tr>");
            sb.append("<td>");
            sb.append("Controller");
            sb.append("</td><td style=\"padding-left:10;\">");
            sb.append("Action");
            sb.append("</td>");
            sb.append("<td>");
            sb.append("Slowest Instance");
            sb.append("</td>");
            sb.append("<td>");
            sb.append("Invocation Count (crawler)");
            sb.append("</td>");
            sb.append("<td>");
            sb.append("Total time");
            sb.append("</td>");
            sb.append("</tr>\n");

            for (ActionProfile action : _actionProfiles.values())
            {
                sb.append("<tr>");
                sb.append("<td valign=top align=right>").append(action.getController()).append("</td>");
                sb.append("<td style=\"padding-left:10;\">").append(action.getAction()).append("</td>");
                sb.append("<td style=\"padding-left:10;\">").append(action.getLongestLoad()).append("</td>");
                sb.append("<td style=\"padding-left:10;\">").append(action.getInvocations()).append("</td>");
                sb.append("<td style=\"padding-left:10;\">").append(action.getTotalTime()).append("</td>");
                sb.append("</tr>\n");
            }

            sb.append("</table>\n");

            return sb.toString();
        }

        public String toTsv()
        {
            StringBuilder sb = new StringBuilder();

            sb.append("Controller\tAction\tSlowest Instance\tInvocation Count (crawler)\tTotal time\n");

            for (ActionProfile action : _actionProfiles.values())
            {
                sb.append(action.getController()).append("\t");
                sb.append(action.getAction()).append("\t");
                sb.append(action.getLongestLoad()).append("\t");
                sb.append(action.getInvocations()).append("\t");
                sb.append(action.getTotalTime());
                sb.append("\n");
            }

            return sb.toString();
        }
    }

    private boolean isVisitableURL(String rootRelativeURL, int currentDepth)
    {
        ControllerActionId actionId = new ControllerActionId(rootRelativeURL);
        String strippedRelativeURL = stripQueryParams(rootRelativeURL);

        // never go to the exactly same URL (minus query params) twice:
        if (_urlsChecked.contains(strippedRelativeURL))
            return false;

        if (rootRelativeURL.contains("export=")) //Study report export uses same URL for export. But don't mark visited yet
            return false;

        if (rootRelativeURL.contains("mailto:")) //Don't crawl mailto: links
            return false;

        _urlsChecked.add(strippedRelativeURL);

        // after navigating past the first N levels of links, we'll only try "new" actions:
        if (currentDepth >= getMaxDepth() && _actionsVisited.contains(actionId))
            return false;

        // never visit explicity excluded actions:
        for (ControllerActionId excluded : _excludedActions)
        {
            if (actionId.equals(excluded))
                return false;
        }

        //skip any _webdav or fake urls
        if (actionId.getController().equalsIgnoreCase("_webdav") || actionId.getController().equalsIgnoreCase("fake"))
            return false;

        // skip export actions. 
        if (actionId.getAction().toLowerCase().contains("export"))
            return false;

        // skip expanding and collapsing paths -- no HTML returned
        if (actionId.getAction().equals("expandCollapse"))
            return false;

        // in addition to test projects, we'll crawl all admin functionality as well
        // (otherwise this never gets covered).
        for (String adminController : getAdminControllers())
        {
            if (actionId.getController().equalsIgnoreCase(adminController) && !"/home".equals(actionId.getFolder()))
                return true;
        }
		
        // always visit all links under projects created by the tests:
        return underCreatedProject(rootRelativeURL);
    }


    private boolean underCreatedProject(String relativeURL)
    {
        relativeURL = BaseWebDriverTest.stripContextPath(relativeURL);
        ControllerActionId cid = new ControllerActionId(relativeURL);
        String folder = StringUtils.strip(cid.getFolder(),"/");
        StringTokenizer st = new StringTokenizer(folder, "/");

        if (!st.hasMoreElements())
            return false;

        String currentProject = EscapeUtil.decode(st.nextToken());
        if (StringUtils.isEmpty(currentProject))
            return false;

        for (String createdProject : _test.getContainerHelper().getCreatedProjects())
        {
            if (currentProject.equals(createdProject))
                return true;
        }
        return false;
    }


    @LogMethod
    public void crawlAllLinks(boolean inject)
    {
        // quick unit-test
        {
        ControllerActionId a = new ControllerActionId("/controller/folder/action.view");
        assertEquals("controller", a.getController());
        assertEquals("folder", a.getFolder());
        assertEquals("action", a.getAction());
        }
        {
        ControllerActionId b = new ControllerActionId("/folder/controller-action.view");
        assertEquals("controller", b.getController());
        assertEquals("folder", b.getFolder());
        assertEquals("action", b.getAction());
        }
        {
        ControllerActionId c = new ControllerActionId("/_webdav/fred");
        assertEquals("_webdav", c.getController());
        }

        _test.log("Starting crawl...");
        _test.beginAt(_test.getProjectUrl());

        // Breadth first search
        int newPages = crawl(inject);
        saveCrawlStats(_test, _urlToCheck.getDepth(), newPages, _actionsVisited.size(), _crawlTime);

        _test.log("Crawl complete. " + newPages + " pages visited, " + _actionsVisited.size() + " unique actions tested by all tests.");
    }


    private int crawl(boolean inject)
    {
        // Breadth first crawl
        long startTime = System.currentTimeMillis();
        int linkCount = 0;
		
        // Initialize list to links present at start page
        URL startPageURL = _test.getURL();
        String[] linkAddresses = _test.getLinkAddresses();
        for (String url : linkAddresses)
            _urlsToCheck.add(new UrlToCheck(startPageURL, url, 1, inject));

        // Loop through links in list until its empty or time runs out
        while ((!_urlsToCheck.isEmpty()) && (_crawlTime < _maxCrawlTime))
        {
            _urlToCheck = _urlsToCheck.remove(0);
            if(_urlToCheck.toString().contains("showMaterialSource"))
                System.out.println("pay attention");
            String urlText = _urlToCheck.getUrlText();
            // Make sure it is a link to inside the page
            if (urlText.startsWith("http://") ||
                    urlText.startsWith("https://") ||
                    urlText.startsWith("javascript:") ||
                    urlText.startsWith("ftp://"))
            {
                if (!urlText.contains(WebTestHelper.getBaseURL()) || urlText.equals(WebTestHelper.getBaseURL()))
                    continue;
                else
                {
                    int relativeURLStart = urlText.lastIndexOf(WebTestHelper.getBaseURL()) + WebTestHelper.getBaseURL().length();
                    urlText = urlText.substring(relativeURLStart);
                }
            }

            // Make sure it is correctly formatted
            String relativeURL;
            if (urlText.startsWith("/"))
                relativeURL = urlText;
            else if (urlText.startsWith("?"))
                relativeURL = stripQueryParams(_urlToCheck.getOrigin().toString()) + urlText;
            else
                relativeURL = getURLBase(_urlToCheck.getOrigin()) + urlText;

            // Check if it should be visited, if so, visit it
            int depth = _urlToCheck.getDepth();
            if (isVisitableURL(relativeURL, depth))
            {
                crawlLink(_urlToCheck, relativeURL);
                linkCount++;
            }
        }

        _crawlTime = (int) (System.currentTimeMillis() - startTime);
        return linkCount;
    }

    private void crawlLink(UrlToCheck urlToCheck, String relativeURL)
    {
        URL origin = null;
        // Helps breadth first crawl
        try
        {
            // Go to the site
            long loadTime = _test.beginAt(relativeURL);
            try{ _test.dismissAlerts(); } catch(SeleniumException ignore){}

            int depth = urlToCheck.getDepth();
            origin = urlToCheck.getOrigin();
            URL currentPageUrl = _test.getURL();

            // Find all the links at the site
            if (_test.isElementPresent(Locator.id("folderBar")))_test.hoverFolderBar();
            String[] linkAddresses = _test.getLinkAddresses();
            for (String url : linkAddresses)
                _urlsToCheck.add(new UrlToCheck(currentPageUrl, url, depth + 1, urlToCheck.testInjection()));

            // Keep track of where crawler has been
            _actionsVisited.add(new ControllerActionId(relativeURL));
            _actionProfiler.updateActionProfile(relativeURL, loadTime);

            checkForForbiddenWords(relativeURL);

            // Check that there was no error
            int code = _test.getResponseCode();
            if (code == 404 || code == 500)
                fail(relativeURL + " produced response code " + code + ".  Originating page: " + origin.toString());

			testInjection(urlToCheck, currentPageUrl);
        }
        catch (RuntimeException |
               AssertionError rethrow) {
            // Collect origin page snapshot for failure and rethrow original failure
            try
            {
                if (origin != null){
                    _test.log("Crawl failure: collecting origin page info.");
                    _test.pushLocation();
                    String originUrl = origin.toString();
                    int relativeURLStart = originUrl.lastIndexOf(WebTestHelper.getBaseURL()) + WebTestHelper.getBaseURL().length();
                    _test.beginAt(originUrl.substring(relativeURLStart));
                    _test.dumpPageSnapshot("crawlOrigin");
                    _test.popLocation();
                }
            }
            catch (Exception ignore) {}

            throw rethrow;
        }
    }

    protected void checkForForbiddenWords(String relativeURL)
    {
        if (!_forbiddenWords.isEmpty())
        {
            String responseText = _test.getResponseText().toLowerCase();

            for (Map.Entry<String, String> entry : _sourceReplacements.entrySet())
                responseText = responseText.replaceAll(entry.getKey(), entry.getValue());

            //loop through forbidden words#BLOCKED
            for (String word : _forbiddenWords)
            {
                if (responseText.indexOf(word.toLowerCase()) > 0)
                {
                    fail("Illegal use of forbidden word '" + word + "'> " + relativeURL);
                }
            }
        }
    }


	public static final String alertText = "8(";
	public static final String maliciousScript = "<script>alert(\"" + alertText + "\");</script>";
	public static final String injectString = "\"'>--></script>" + maliciousScript;

    public static <F, T> T tryInject(BaseWebDriverTest test, Function<F, T> f, F arg)
    {
        try
        {
            String msg = null;
            try
            {
                return f.apply(arg);
            }
            catch (SeleniumException se)
            {
                String html = test.getHtmlSource();
                if (html.contains(maliciousScript))
                    msg = "page contains injected script";

                while (test.isAlertPresent())
                {
                    if (test.getAlert().startsWith(alertText))
                        msg = " malicious script executed";
                }

                // see ConnectionWrapper.java
                if (html.contains("SQL injection test failed"))
                    msg = "SQL injection detected";

                if (msg != null)
                {
                    String url = test.getCurrentRelativeURL();
                    fail(msg + "\n" + url);
                }

                throw se;
            }
            catch (UnhandledAlertException ex)
            {
                if (test.getAlert().startsWith(alertText))
                    msg = " malicious script executed";

                String html = test.getHtmlSource();

                if (html.contains(maliciousScript))
                    msg = "page contains injected script";

                // see ConnectionWrapper.java
                if (html.contains("SQL injection test failed"))
                    msg = "SQL injection detected";

                if (msg != null)
                {
                    String url = test.getCurrentRelativeURL();
                    fail(msg + "\n" + url);
                }

                throw ex;
            }
        }
        catch (RuntimeException re)
        {
            // ignore javascript errors (HTTPUnit has a poor engine) and non-HTML download links:
            if (!re.getMessage().contains("ScriptException") && !re.getClass().getSimpleName().equals("NotHTMLException"))
                throw re;
        }

        return null;
    }
	
	private void testInjection(UrlToCheck urlToCheck, URL start)
	{
		if (!urlToCheck.testInjection())
			return;

		String base = start.toString();
		String query = start.getQuery();
		int q = base.indexOf('?');
		if (q != -1)
			base = base.substring(0,q);

		if (query == null)
			return;
		if (query.startsWith("?"))
			query = query.substring(1);
		if (query.length() == 0)
			return;

        Function<String, Void> urlTester = new Function<String, Void>() {
            @Override
            public Void apply(String urlMalicious)
            {
                _test.beginAt(urlMalicious);
                return null;
            }
        };

		String[] parts = StringUtils.split(query,'&');
		for (int i=0 ; i<parts.length ; i++)
		{
			String save = parts[i];
			parts[i] = save + ( save.indexOf('=') == -1 ? "=" : "") + injectString;
			String queryMalicious = StringUtils.join(parts, '&');
			parts[i] = save;

			String urlMalicious = base + "?" + queryMalicious;
            tryInject(_test, urlTester, urlMalicious);
		}
	}
}
