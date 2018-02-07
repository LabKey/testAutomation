/*
 * Copyright (c) 2007-2017 LabKey Corporation
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

import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.apache.commons.lang3.StringUtils;
import org.labkey.remoteapi.collections.CaseInsensitiveHashMap;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.ExtraSiteWrapper;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.api.ProjectMenu;
import org.openqa.selenium.Alert;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebDriverException;

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
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class Crawler
{
    private final List<ControllerActionId> _excludedActions;
    private final List<ControllerActionId> _actionsExcludedFromInjection;
    private final Collection<String> _adminControllers;
    private final Collection<String> _forbiddenWords;

    // Replacements to make in HTML source before looking for "forbidden" words.
    private static Map<String, String> _sourceReplacements = new CaseInsensitiveHashMap<>();

    private static Set<ControllerActionId> _actionsVisited = new HashSet<>();
    private static Set<ControllerActionId> _actionsWithErrors = new HashSet<>();
    private static Set<String> _urlsChecked = new HashSet<>();
    public static ActionProfiler _actionProfiler = new ActionProfiler();
    private boolean _needToGetProjectMenuLinks = true;
    private int _finalDepth = 0;
    private int _crawlTime = 0;
    private int _maxDepth = 3;
    private ArrayList<UrlToCheck> _urlsToCheck = new ArrayList<>();

    private int _maxCrawlTime;
    private static final int DEFAULT_CRAWL_TIME = 90000;

    private static Map<String, CrawlStats> _crawlStats = new LinkedHashMap<>();
    private BaseWebDriverTest _test;
    private boolean _injectionCheckEnabled = false;
    private Collection<String> _projects;

    public Crawler(BaseWebDriverTest test)
    {
        this(test, DEFAULT_CRAWL_TIME);
    }

    public Crawler(BaseWebDriverTest test, int crawlTime)
    {
        this(test, test.getContainerHelper().getCreatedProjects(), crawlTime);
    }

    public Crawler(BaseWebDriverTest test, Collection<String> projects, int crawlTime)
    {
        _test = test;
        _projects = projects;
        _maxCrawlTime = crawlTime;
        _adminControllers = Collections.unmodifiableCollection(getAdminControllers());
        _forbiddenWords = getForbiddenWords();
        _excludedActions = getDefaultExcludedActions();
        _actionsExcludedFromInjection = getExcludedActionsFromInjection();
        for (String project : projects)
        {
            addProject(project);
        }
        _urlsToCheck.add(new UrlToCheck(null, "/project/begin.view?", 0));
    }

    protected Set<String> getForbiddenWords()
    {
        return new HashSet<>();
    }

    protected List<ControllerActionId> getDefaultExcludedActions()
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
            new ControllerActionId("admin", "queryStackTraces"),
            new ControllerActionId("admin", "shortURLAdmin"),
            new ControllerActionId("admin-sql", "saveReorderedScript"),
            new ControllerActionId("announcements", "download"),
            new ControllerActionId("assay", "assayDetailRedirect"),
            new ControllerActionId("assay", "designer"), // assay designer prompts to save design when navigating away
            new ControllerActionId("assay", "template"),
            new ControllerActionId("core", "downloadFileLink"),
            new ControllerActionId("dumbster", "begin"),
            new ControllerActionId("experiment", "showFile"),
            new ControllerActionId("flow-compensation", "download"),
            new ControllerActionId("flow-editscript", "download"),
            new ControllerActionId("flow-run", "download"),
            new ControllerActionId("flow-well", "download"),
            new ControllerActionId("genotyping", "analyze"),    // Crawler doesn't like NotFoundException that the test generates
            new ControllerActionId("issues", "download"),
            new ControllerActionId("list", "download"),
            new ControllerActionId("login", "logout"),
            new ControllerActionId("login", "enable"),      // TODO: These should be post actions (product issue)
            new ControllerActionId("login", "disable"),      // TODO: These should be post actions (product issue)
            new ControllerActionId("login", "setAuthenticationParameter"),
            new ControllerActionId("login", "setPassword"),
            new ControllerActionId("microarray", "designer"), // assay designer prompts to save design when navigating away
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
            new ControllerActionId("search", "search"), // Tests need to wait for indexer manually
            new ControllerActionId("security", "resetPassword"),
            new ControllerActionId("study", "confirmDeleteVisit"),
            new ControllerActionId("study", "template"),
            new ControllerActionId("study", "downloadTsv"),
            new ControllerActionId("study", "deleteDatasetReport"),
            new ControllerActionId("study", "deleteDataset"),
            new ControllerActionId("study", "importStudyFromPipeline"),
            new ControllerActionId("study", "manageStudyProperties"), // Intermittently triggers form dirty alert
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
            new ControllerActionId("wiki", "download"),
            new ControllerActionId("harvest", "sickSafeTime"),
            new ControllerActionId("harvest", "formatInvoice"),
            new ControllerActionId("targetedms", "downloadDocument"),

                // Actions that error from Admin->GoToModule->MoreModules when module is not enabled
                new ControllerActionId("nlp", "begin"),
                new ControllerActionId("biologics", "begin"),
                new ControllerActionId("reagent", "begin"),
                new ControllerActionId("su2c", "begin"),
                new ControllerActionId("trialshare", "begin"),
                new ControllerActionId("ehr_compliancedb", "requirementDetails"),
                new ControllerActionId("onprc_billingpublic", "begin"),
                new ControllerActionId("hdrl", "begin")
        );

        return list;
    }

    protected List<ControllerActionId> getExcludedActionsFromInjection()
    {
        List<ControllerActionId> list = new ArrayList<>();
        Collections.addAll(list,
                new ControllerActionId("experiment", "showRunGraphDetail"),
                new ControllerActionId("flow", "query"),
                new ControllerActionId("flow-attribute", "createAlias"),
                new ControllerActionId("flow-attribute", "details"),
                new ControllerActionId("flow-attribute", "edit"),
                new ControllerActionId("flow-attribute", "summary"),
                new ControllerActionId("flow-run", "showRuns")
        );

        return list;
    }

    protected Map<ControllerActionId, List<String>> getExcludedParametersFromInjection()
    {
        Map<ControllerActionId, List<String>> map = new HashMap<>();
        map.put(new ControllerActionId("assay", "assayResults"), Collections.singletonList("Data.Run/RowId")); // TODO: 23321: Bad rowId input for assay details triggers server errors
        map.put(new ControllerActionId("assay", "assayRuns"), Collections.singletonList("Data.Batch/RowId")); // TODO: 23321: Bad rowId input for assay details triggers server errors
        map.put(new ControllerActionId("study-designer", "designer"), Collections.singletonList("panel")); // TODO: 16768: study-designer.DesignerAction: IllegalArgumentException on bad 'panel'
        map.put(new ControllerActionId("study-samples", "samples"), Collections.singletonList("AtRepository")); // TODO: 21337: study-samples.SamplesAction: SQLGenerationException from un-parseable URL parameters

        return map;
    }

    public void addExcludedActions(Collection<ControllerActionId> action)
    {
        _excludedActions.addAll(action);
    }

    public void addProject(String project)
    {
        _urlsToCheck.add(new UrlToCheck(null, WebTestHelper.buildRelativeUrl("project", project, "start"), 0));
    }

    public void setInjectionCheckEnabled(boolean enabled)
    {
        _injectionCheckEnabled = enabled;
    }

    protected Collection<String> getAdminControllers()
    {
        Set<String> adminControllers = Collections.newSetFromMap(new CaseInsensitiveMap<>());
        adminControllers.addAll(Arrays.asList("login", "admin", "security", "user"));
        return adminControllers;
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

    private static String stripHash(String url)
    {
        int paramIdx = url.indexOf('#');
        if (paramIdx > 0)
            url = url.substring(0, paramIdx);
        return url;
    }

    private class UrlToCheck
    {
        // Keep track of urls to check for breadth first crawl
        private final URL _origin;
        private final String _urlText;
        private final String _relativeURL;
        private final ControllerActionId _actionId;
        private int _depth;

        public UrlToCheck(URL origin, String urlText, int depth)
        {
            _origin = origin;
            _urlText = urlText;
            _depth = depth;

            // Make sure it is a link to inside the page
            if (urlText.startsWith("http://") ||
                    urlText.startsWith("https://") ||
                    urlText.startsWith("javascript:") ||
                    urlText.startsWith("ftp://"))
            {
                if (!urlText.contains(WebTestHelper.getBaseURL()) || urlText.equals(WebTestHelper.getBaseURL()))
                {
                    _relativeURL = null;
                    _actionId = null;
                }
                else
                {
                    int relativeURLStart = urlText.lastIndexOf(WebTestHelper.getBaseURL()) + WebTestHelper.getBaseURL().length();
                    _relativeURL = urlText.substring(relativeURLStart);
                    _actionId = new ControllerActionId(_relativeURL);
                }
            }
            else
            {
                // Make sure it is correctly formatted
                if (urlText.startsWith("/"))
                    _relativeURL = urlText;
                else if (urlText.startsWith("#"))
                    _relativeURL = stripHash(origin.toString()) + urlText;
                else if (urlText.startsWith("?"))
                    _relativeURL = stripQueryParams(origin.toString()) + urlText;
                else
                    _relativeURL = getURLBase(origin) + urlText;

                _actionId = new ControllerActionId(_relativeURL);
            }
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

        public String getRelativeURL()
        {
            return _relativeURL;
        }

        public ControllerActionId getActionId()
        {
            return _actionId;
        }

        public boolean underCreatedProject()
        {
            String folder = StringUtils.strip(getActionId().getFolder(), "/");
            StringTokenizer st = new StringTokenizer(folder, "/");

            if (!st.hasMoreElements())
                return false;

            String currentProject = EscapeUtil.decode(st.nextToken());
            if (StringUtils.isEmpty(currentProject))
                return false;

            return _projects.contains(currentProject);
        }

        public boolean isVisitableURL()
        {
            if (getRelativeURL() == null)
                return false;

            String strippedRelativeURL = stripQueryParams(getRelativeURL());

            // never go to the exactly same URL (minus query params) twice:
            if (_urlsChecked.contains(strippedRelativeURL))
                return false;

            if (getRelativeURL().contains("export=")) //Study report export uses same URL for export. But don't mark visited yet
                return false;

            if (getRelativeURL().contains("mailto:")) //Don't crawl mailto: links
                return false;

            _urlsChecked.add(strippedRelativeURL);

            // after navigating past the first N levels of links, we'll only try "new" actions:
            if (getDepth() >= getMaxDepth() && _actionsVisited.contains(getActionId()))
                return false;

            // Don't let a single bad action fail multiple tests
            if (_actionsWithErrors.contains(getActionId()))
                return false;

            // never visit explicitly excluded actions:
            if (_excludedActions.contains(getActionId()))
                return false;

            //skip any _webdav or fake urls
            if (getActionId().getController().equalsIgnoreCase("_webdav") || getActionId().getController().equalsIgnoreCase("fake"))
                return false;

            // skip export actions.
            if (getActionId().getAction().toLowerCase().contains("export"))
                return false;

            // skip expanding and collapsing paths -- no HTML returned
            if (getActionId().getAction().equals("expandCollapse"))
                return false;

            // in addition to test projects, we'll crawl all admin functionality as well
            // (otherwise this never gets covered).
            if (_adminControllers.contains(getActionId().getController()) && !"/home".equals(getActionId().getFolder()))
                return true;

            // always visit all links under projects created by the tests:
            return underCreatedProject();
        }

        private boolean isInjectableURL()
        {
            return !_actionsExcludedFromInjection.contains(getActionId());
        }
    }

    public static class ControllerActionId
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
            rootRelativeURL = WebTestHelper.stripContextPath(rootRelativeURL);

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
                int dash = _action.lastIndexOf("-");
                _controller = _action.substring(0,dash);
                _action = _action.substring(dash+1);
            }
            else
            {
                /* controller/folders/ */
                int postControllerSlashIdx = rootRelativeURL.indexOf('/');
                if (-1 == postControllerSlashIdx)
                    throw new IllegalArgumentException("Unable to parse folder out of relative URL: \"" + rootRelativeURL + "\"");
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
        public String toString()
        {
            return _controller + "-" + _action;
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

    @LogMethod
    public void crawlAllLinks()
    {
        // quick unit-test
        {
            ControllerActionId oldAction = new ControllerActionId("/controller/project/folder/action.view");
            assertEquals("controller", oldAction.getController());
            assertEquals("project/folder", oldAction.getFolder());
            assertEquals("action", oldAction.getAction());
            ControllerActionId containerRelativeAction = new ControllerActionId("/project/folder/controller-action.view");
            assertEquals("controller", containerRelativeAction.getController());
            assertEquals("project/folder", containerRelativeAction.getFolder());
            assertEquals("action", containerRelativeAction.getAction());
            assertEquals(oldAction, containerRelativeAction);
            ControllerActionId subControllerOldAction = new ControllerActionId("/controller-sub/folder/action.view");
            assertEquals("controller-sub", subControllerOldAction.getController());
            assertEquals("folder", subControllerOldAction.getFolder());
            assertEquals("action", subControllerOldAction.getAction());
            ControllerActionId subControllerNewAction = new ControllerActionId("/folder/controller-sub-action.view");
            assertEquals("controller-sub", subControllerNewAction.getController());
            assertEquals("folder", subControllerNewAction.getFolder());
            assertEquals("action", subControllerNewAction.getAction());
            assertEquals(subControllerOldAction, subControllerNewAction);
            ControllerActionId webdavAction = new ControllerActionId("/_webdav/fred");
            assertEquals("_webdav", webdavAction.getController());
        }

        TestLogger.log("Starting crawl...");

        // Breadth first search
        int newPages = crawl();
        saveCrawlStats(_test, _finalDepth, newPages, _actionsVisited.size(), _crawlTime);

        TestLogger.log("Crawl complete. " + newPages + " pages visited, " + _actionsVisited.size() + " unique actions tested by all tests.");
    }

    private int crawl()
    {
        // Breadth first crawl
        long startTime = System.currentTimeMillis();
        int linkCount = 0;

        // Loop through links in list until its empty or time runs out
        while ((!_urlsToCheck.isEmpty()) && (_crawlTime < _maxCrawlTime))
        {
            UrlToCheck urlToCheck = _urlsToCheck.remove(0);

            if (urlToCheck.isVisitableURL())
            {
                crawlLink(urlToCheck);
                linkCount++;
            }
            _crawlTime = (int) (System.currentTimeMillis() - startTime);
        }

        return linkCount;
    }

    private void crawlLink(UrlToCheck urlToCheck)
    {
        _finalDepth = Math.max(urlToCheck.getDepth(), _finalDepth);
        String relativeURL = urlToCheck.getRelativeURL();
        ControllerActionId actionId = new ControllerActionId(relativeURL);

        // Keep track of where crawler has been
        _actionsVisited.add(actionId);
        URL origin = urlToCheck.getOrigin();
        int depth = urlToCheck.getDepth();

        try
        {
            long loadTime = _test.beginAt(relativeURL);
            _actionProfiler.updateActionProfile(relativeURL, loadTime);

            URL currentPageUrl = _test.getURL();

            // Find all the links at the site
            if (_needToGetProjectMenuLinks && depth == 1 && _test.isElementPresent(ProjectMenu.Locators.menuProjectNav))
            {
                _needToGetProjectMenuLinks = false;
                _test.projectMenu().open();
            }
            String[] linkAddresses = _test.getLinkAddresses();
            for (String url : linkAddresses)
                _urlsToCheck.add(new UrlToCheck(currentPageUrl, url, depth + 1));

            checkForForbiddenWords(relativeURL);

            // Check that there was no error
            int code = _test.getResponseCode();
            if (code >= 400)
                fail(relativeURL + "\nproduced response code " + code + ".\nOriginating page: " + origin.toString());
            List<String> serverError = _test.getTexts(Locator.css("table.server-error").findElements(_test.getDriver()));
            if (!serverError.isEmpty())
            {
                String[] errorLines = serverError.get(0).split("\n");
                fail(relativeURL + "\nproduced error: \"" + errorLines[0] + "\".\nOriginating page: " + origin.toString());
            }

            if (urlToCheck.isInjectableURL() && _injectionCheckEnabled)
                testInjection(currentPageUrl);
        }
        catch (RuntimeException | AssertionError rethrow)
        {
            _actionsWithErrors.add(actionId);
            // Collect origin page snapshot for failure and rethrow original failure

            if (origin != null)
            {
                TestLogger.log("Crawl failure: collecting origin page info.");
                try (ExtraSiteWrapper originBrowser = new ExtraSiteWrapper(WebDriverWrapper.BrowserType.FIREFOX, BaseWebDriverTest.getDownloadDir()))
                {
                    originBrowser.simpleSignIn();
                    originBrowser.beginAt(origin.toString());
                    ArtifactCollector collector = new ArtifactCollector(BaseWebDriverTest.getCurrentTest(), originBrowser);
                    collector.dumpPageSnapshot("crawler", "crawlOrigin");
                }
            }

            if (rethrow instanceof AssertionError)
                throw rethrow;
            else
                throw new RuntimeException(relativeURL + "\nTriggered an exception." + (origin != null ? "\nOriginating page: " + origin.toString() : ""), rethrow);
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
                if (responseText.contains(word.toLowerCase()))
                {
                    fail("Illegal use of forbidden word '" + word + "'> " + relativeURL);
                }
            }
        }
    }

    public static final String injectedAlert = "8(";
    public static final String maliciousScript = "<script>alert(\"" + injectedAlert + "\");</script>";
    public static final String injectString = "\"'>--></script>" + maliciousScript;

    public static <F, T> T tryInject(BaseWebDriverTest test, Function<F, T> f, F arg)
    {
        String msg = null;
        try
        {
            return f.apply(arg);
        }
        catch (UnhandledAlertException ex)
        {
            String alertText = ex.getAlertText();
            if (alertText == null)
                alertText = test.cancelAlert();

            if (alertText.startsWith(injectedAlert))
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
        catch (WebDriverException ex)
        {
            String html = test.getHtmlSource();
            if (html.contains(maliciousScript))
                msg = "page contains injected script";

            Alert alert;
            while (null != (alert = test.getAlertIfPresent()))
            {
                if (alert.getText().startsWith(injectedAlert))
                    msg = " malicious script executed";
                alert.dismiss();
            }
            test.switchToMainWindow();

            // see StatementWrapper.java
            if (html.contains("SQL injection test failed"))
                msg = "SQL injection detected";

            if (msg != null)
            {
                String url = test.getCurrentRelativeURL();
                fail(msg + "\n" + url);
            }

            throw ex;
        }
        catch (RuntimeException re)
        {
            // ignore javascript errors (HTTPUnit has a poor engine) and non-HTML download links:
            if (!re.getMessage().contains("ScriptException") && !re.getClass().getSimpleName().equals("NotHTMLException"))
                throw re;
        }

        return null;
    }

    private void testInjection(URL start)
    {
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
                _test.executeScript("return;"); // Trigger UnhandledAlertException
                return null;
            }
        };

        ControllerActionId actionId = new ControllerActionId(base);
        List<String> excludedParams = getExcludedParametersFromInjection().get(actionId);
        String[] parts = StringUtils.split(query,'&');
        for (int i=0 ; i<parts.length ; i++)
        {
            if (excludedParams != null && excludedParams.contains(parts[i].split("=")[0]))
                continue;
            String save = parts[i];
            parts[i] = save + ( save.contains("=") ? "" : "=") + injectString;
            String queryMalicious = StringUtils.join(parts, '&');
            parts[i] = save;

            String urlMalicious = base + "?" + queryMalicious;
            tryInject(_test, urlTester, urlMalicious);
        }
    }
}
