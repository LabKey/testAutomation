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

package org.labkey.test.util;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
import org.labkey.remoteapi.collections.CaseInsensitiveHashMap;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.ExtraSiteWrapper;
import org.labkey.test.Locator;
import org.labkey.test.TestProperties;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.api.ProjectMenu;
import org.openqa.selenium.Alert;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebDriverException;

import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.time.Duration;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class Crawler
{
    private final List<ControllerActionId> _excludedActions;
    private final List<ControllerActionId> _terminalActions;
    private final List<ControllerActionId> _actionsExcludedFromInjection;
    private final List<ControllerActionId> _actionsMayLinkTo404;
    private final Collection<String> _adminControllers;
    private final Collection<String> _forbiddenWords;
    private final boolean _prioritizeAdminPages;

    // Replacements to make in HTML source before looking for "forbidden" words.
    private static Map<String, String> _sourceReplacements = new CaseInsensitiveHashMap<>();

    // All parameters seen by the crawler. Used to randomly attempt injection against parameters not found in UI
    private static LinkedHashMap<String,String> _dictionary = new LinkedHashMap<>();
    static
    {
        Arrays.asList("rowid", "name", "userId", "query.sort", "query.rowid~eq", "query.name~contains", "returnUrl")
                .forEach(s -> _dictionary.put(s,""));
    }

    private static MultiValuedMap<ControllerActionId, String> _parametersInjected = new HashSetValuedHashMap<>();
    private static Set<ControllerActionId> _actionsVisited = new HashSet<>();
    private static Set<ControllerActionId> _actionsWithErrors = new HashSet<>();
    private static Set<String> _urlsChecked = new HashSet<>();
    public static ActionProfiler _actionProfiler = new ActionProfiler();
    private int _remainingAttemptsToGetProjectLinks = 4;
    private int _maxDepth = 4;
    private final ArrayList<UrlToCheck> _startingUrls = new ArrayList<>();

    private final Duration _maxCrawlTime;

    private static Map<String, CrawlStats> _crawlStats = new LinkedHashMap<>();
    private BaseWebDriverTest _test;
    private final List<String> _warnings = new ArrayList<>();
    private final boolean _injectionCheckEnabled;
    private final Set<String> _projects = Collections.newSetFromMap(new CaseInsensitiveHashMap<>());

    public Crawler(BaseWebDriverTest test, Duration crawlTime)
    {
        this(test, test.getContainerHelper().getCreatedProjects(), crawlTime, false);
    }

    public Crawler(BaseWebDriverTest test, Duration crawlTime, boolean injectionTest)
    {
        this(test, test.getContainerHelper().getCreatedProjects(), crawlTime, injectionTest);
    }

    public Crawler(BaseWebDriverTest test, Collection<String> projects, Duration crawlTime, boolean injectionTest)
    {
        _test = test;
        _maxCrawlTime = crawlTime;
        _adminControllers = Collections.unmodifiableCollection(getAdminControllers());
        _forbiddenWords = getForbiddenWords();
        _excludedActions = getDefaultExcludedActions();
        _terminalActions = getDefaultTerminalActions();
        _actionsExcludedFromInjection = getExcludedActionsFromInjection();
        _actionsMayLinkTo404 = getAllowed404Sources();
        _injectionCheckEnabled = injectionTest;
        for (String project : projects)
        {
            addProject(project);
        }
        if (projects.isEmpty())
        {
            _startingUrls.add(new UrlToCheck(null, "/admin-showAdmin.view#links", 0));
            _startingUrls.add(new UrlToCheck(null, "/admin-spider.view", 2));
        }
        if (injectionTest)
        {
            test.getUrlsSeen().stream()
                .filter(url -> !StringUtils.isBlank(url))
                .map(url -> new UrlToCheck(null, url, 1))
                .filter(UrlToCheck::isVisitableURL)
                .filter(UrlToCheck::isInjectableURL)
                .forEach(_startingUrls::add);
        }
        _prioritizeAdminPages = projects.isEmpty();
    }

    protected Set<String> getForbiddenWords()
    {
        return new HashSet<>();
    }

    protected List<ControllerActionId> getDefaultExcludedActions()
    {
        List<ControllerActionId> list = new ArrayList<>();
        Collections.addAll(
            list,
            new ControllerActionId("admin", "resetErrorMark"),
            new ControllerActionId("admin", "doCheck"),
            new ControllerActionId("admin", "runSystemMaintenance"),
            new ControllerActionId("admin", "deleteFolder"),
            new ControllerActionId("admin", "memTracker"),
            new ControllerActionId("admin", "setAdminMode"),
            new ControllerActionId("admin", "dumpHeap"),
            new ControllerActionId("admin", "exportQueries"), // download action
            new ControllerActionId("admin", "getSchemaXmlDoc"), // download action
            new ControllerActionId("admin", "addTab"),
            new ControllerActionId("admin", "actions"), // Gets hit often in normal testing
            new ControllerActionId("admin", "credits"), // Gets checked by BasicTest
            new ControllerActionId("admin", "showErrorsSinceMark"), // Gets hit often in normal testing
            new ControllerActionId("admin", "resetQueryStatistics"),
            new ControllerActionId("admin", "queryStackTraces"),
            new ControllerActionId("admin", "mapNetworkDrive"), // 404 on non-Windows
            new ControllerActionId("admin", "shortURLAdmin"),
            new ControllerActionId("admin", "showAllErrors"),
            new ControllerActionId("admin", "showPrimaryLog"), // Can take very long to load
            new ControllerActionId("admin-sql", "saveReorderedScript"),
            new ControllerActionId("announcements", "download"),
            new ControllerActionId("assay", "assayDetailRedirect"),
            new ControllerActionId("assay", "downloadSampleQCData"),
            new ControllerActionId("assay", "template"),
            new ControllerActionId("cds", "exportTourDefinitions"), // Download action
            new ControllerActionId("cds", "permissionsReportExport"),
            new ControllerActionId("core", "downloadFileLink"), // Download action
            new ControllerActionId("dumbster", "begin"),
            new ControllerActionId("experiment", "exportProtocols"),
            new ControllerActionId("experiment", "exportRunFiles"),
            new ControllerActionId("experiment", "exportSampleType"),
            new ControllerActionId("experiment", "showFile"),
            new ControllerActionId("filetransfer", "auth"), // redirects to external site
            new ControllerActionId("flow-compensation", "download"),
            new ControllerActionId("flow-editscript", "download"),
            new ControllerActionId("flow-run", "download"),
            new ControllerActionId("flow-well", "download"),
            new ControllerActionId("genotyping", "analyze"),    // Crawler doesn't like NotFoundException that the test generates
            new ControllerActionId("harvest", "formatInvoice"),
            new ControllerActionId("harvest", "sickSafeTime"),
            new ControllerActionId("issues", "download"),
            new ControllerActionId("list", "download"),
            new ControllerActionId("login", "logout"),
            new ControllerActionId("login", "setAuthenticationParameter"),
            new ControllerActionId("login", "setPassword"),
            new ControllerActionId("login", "createToken"),
            new ControllerActionId("login", "verifyToken"), // returns XML, which WDW.waitForPageToLoad can't handle
            new ControllerActionId("luminex", "exportDefaultValues"), // download action
            new ControllerActionId("microarray", "designer"), // assay designer prompts to save design when navigating away
            new ControllerActionId("ms2", "pepSearch"), // TODO: 36995: Check for SQL injection in StatementWrapper is not precise enough
            new ControllerActionId("ms2", "showParamsFile"),
            new ControllerActionId("ms2", "showList"),
            // Tested directly in XTandemTest
            new ControllerActionId("ms2", "doProteinSearch"),
            new ControllerActionId("nabassay", "downloadDatafile"),
            new ControllerActionId("nlp", "runPipeline"),
            new ControllerActionId("pipeline-analysis", "analyze"), // Doesn't navigate
            new ControllerActionId("pipeline-status", "providerAction"), // Re-triggers previously expected errors
            new ControllerActionId("pipeline-status", "showFile"), // Download action
            new ControllerActionId("project", "togglePageAdminMode"),
            new ControllerActionId("query", "printRows"),
            new ControllerActionId("query", "exportExcelTemplate"), // Download action
            new ControllerActionId("query", "exportRowsExcel"),
            new ControllerActionId("query", "excelWebQueryDefinition"),
            new ControllerActionId("reports", "crosstabExport"), // Download action
            new ControllerActionId("reports", "downloadInputData"),
            new ControllerActionId("reports", "streamFile"),
            new ControllerActionId("reports", "download"),
            new ControllerActionId("search", "search"), // Tests need to wait for indexer manually
            new ControllerActionId("security", "groupExport"), // Download action
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
            new ControllerActionId("study-security", "exportSecurityPolicy"),
            new ControllerActionId("study-samples", "downloadSpecimenList"),
            new ControllerActionId("study-samples", "emailLabSpecimenLists"),
            new ControllerActionId("study-samples", "getSpecimenExcel"),
            new ControllerActionId("study-samples", "download"),
            new ControllerActionId("targetedms", "downloadChromLibrary"),
            new ControllerActionId("targetedms", "downloadDocument"),
            new ControllerActionId("test", "npe"),
            new ControllerActionId("wiki", "download"),

                // Disable crawler for single-page apps until we make `beginAt` work with them
                new ControllerActionId("biologics", "app"),
                new ControllerActionId("cds", "app"),
                new ControllerActionId("samplemanager", "app"),

                // Actions that error with no parameters. Generally linked from admin-spider.view
                new ControllerActionId("user", "changeEmail"), // NotFoundException from changeEmail.jsp

                // Actions that error from Admin->GoToModule->MoreModules when module is not enabled
                new ControllerActionId("nlp", "begin"),
                new ControllerActionId("biologics", "begin"),
                new ControllerActionId("reagent", "begin"),
                new ControllerActionId("su2c", "begin"),
                new ControllerActionId("trialshare", "begin"),
                new ControllerActionId("datafinder", "begin"),
                new ControllerActionId("ehr_compliancedb", "requirementDetails"),
                new ControllerActionId("onprc_billingpublic", "begin"),
                new ControllerActionId("hdrl", "begin")

        );

        for (String controller : getExcludedControllers())
        {
            list.add(new ControllerActionId(controller, "*"));
        }

        for (String actionName : getExcludedActionNames())
        {
            list.add(new ControllerActionId("*", actionName));
        }

        return list;
    }
    
    protected Set<String> getExcludedControllers()
    {
        Set<String> controllers = Collections.newSetFromMap(new CaseInsensitiveMap<>());

        // Don't crawl webdav
        controllers.add("_webdav");
        controllers.add("_webfiles");

        // Don't crawl test modules
        controllers.add("chartingapi");
        controllers.add("ETLtest");
        controllers.add("footerTest");
        controllers.add("linkedschematest");
        controllers.add("miniassay");
        controllers.add("pipelinetest");
        controllers.add("pipelinetest2");
        controllers.add("restrictedModule");
        controllers.add("scriptpad");
        controllers.add("simpletest");
        controllers.add("triggerTestModule");
        controllers.add("test");
        controllers.add("devtools");

        // Don't crawl fake links
        controllers.add("fake");

        return controllers;
    }

    protected List<ControllerActionId> getAllowed404Sources()
    {
        List<ControllerActionId> list = new ArrayList<>();
        Collections.addAll(list, spiderAction,
                new ControllerActionId("harvest", "begin"));

        return list;
    }

    protected Set<String> getExcludedActionNames()
    {
        Set<String> actionNames = Collections.newSetFromMap(new CaseInsensitiveMap<>());

        actionNames.add("export");
        actionNames.add("download");
        actionNames.add("expandCollapse");

        return actionNames;
    }

    // These actions are likely to contain bad links but should, themselves, be crawled and injection checked
    protected List<ControllerActionId> getDefaultTerminalActions()
    {
        List<ControllerActionId> list = new ArrayList<>();
        Collections.addAll(list,
                new ControllerActionId("admin", "caches"), // Just links to self and 404 pages
                new ControllerActionId("core", "styleGuide"), // Contains fake actions for style demonstration
                new ControllerActionId("pipeline-status", "showList") // Is likely to contain 404 links
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
        map.put(new ControllerActionId("study-designer", "designer"), Collections.singletonList("panel")); // TODO: 16768: study-designer.DesignerAction: IllegalArgumentException on bad 'panel'

        // Permanent exclusions
        map.put(new ControllerActionId("plate", "designer"), Arrays.asList("colCount", "rowCount")); // 37208: Plate designer dumps stack trace from bad URL parameters
        map.put(new ControllerActionId("reports", "runReport"), Arrays.asList(".lastFilter")); // Action triggers a POST, which logs an error. See `ViewServlet.requestActionURL`
        map.put(new ControllerActionId("study", "dataset"), Arrays.asList(".lastFilter")); // Action triggers a POST, which logs an error. See `ViewServlet.requestActionURL`

        return map;
    }

    public void addExcludedActions(Collection<ControllerActionId> action)
    {
        _excludedActions.addAll(action);
    }

    public void addProject(String project)
    {
        if (!_projects.contains(project))
        {
            _projects.add(project);
            _startingUrls.add(new UrlToCheck(null, WebTestHelper.buildRelativeUrl("project", project, "start"), 0));
            _startingUrls.add(new UrlToCheck(null, WebTestHelper.buildRelativeUrl("admin", project, "spider"), 2));
        }
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

    public static Map<String, CrawlStats> getCrawlStats()
    {
        return _crawlStats;
    }

    public class CrawlStats
    {
        private final int _newPages;
        private final int _uniqueActions;
        private final Duration _crawlTestLength;
        private final int _maxDepth;
        private final List<String> _warnings;

        public CrawlStats(int maxDepth, int newPages, int uniqueActions, Duration crawlTestLength, List<String> warnings)
        {
            _newPages = newPages;
            _uniqueActions = uniqueActions;
            _crawlTestLength = crawlTestLength;
            _maxDepth = maxDepth;
            _warnings = new ArrayList<>(warnings);
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

        public Duration getCrawlTestLength()
        {
            return _crawlTestLength;
        }

        public List<String> getWarnings()
        {
            return _warnings;
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
        public final float priority;

        // Keep track of urls to check for breadth first crawl
        private final URL _origin;
        private final String _urlText;
        private final String _relativeURL;
        private final ControllerActionId _actionId;
        private final int _depth;
        private boolean _isFromForm = false;

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
                if (!urlText.contains(WebTestHelper.getBaseURL()) || urlText.equals(WebTestHelper.getBaseURL()) || isLabKeyShortUrl(urlText))
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

            int p = _depth;
            if (underCreatedProject())
                p--;
            // demote admin controllers
            if (null != getActionId() && _adminControllers.contains(getActionId().getController()))
                p += (_prioritizeAdminPages ? -1 : 1);
            // demote root directory
            if (null != getActionId() && StringUtils.isBlank(StringUtils.strip(getActionId().getFolder(),"/")))
                p += (_prioritizeAdminPages ? -1 : 1);
            priority = p + random.nextFloat();
        }

        private boolean isLabKeyShortUrl(String urlText)
        {
            return urlText.startsWith(WebTestHelper.getBaseURL()) && urlText.endsWith(".url");
        }

        public boolean isFromForm()
        {
            return _isFromForm;
        }

        public void setFromForm(boolean fromForm)
        {
            _isFromForm = fromForm;
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
            if (null == getActionId())
                return false;

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

            if (getRelativeURL().contains("javascript:")) //Don't crawl javascript: links
                return false;

            // after navigating past the first N levels of links, we'll only try "new" actions:
            if (getDepth() >= getMaxDepth() && _actionsVisited.contains(getActionId()))
                return false;

            if (spiderAction.equals(getActionId()) && TestProperties.isPrimaryUserAppAdmin())
                return false; // SpiderAction is inaccessible to app admin

            // Don't let a single bad action fail multiple tests
            if (_actionsWithErrors.contains(getActionId()))
                return false;

            // never visit explicitly excluded actions:
            if (_excludedActions.contains(getActionId()))
                return false;

            //skip excluded controllers
            if (_excludedActions.contains(new ControllerActionId(getActionId().getController(), "*")))
                return false;

            // skip universally excluded actions.
            if (_excludedActions.contains(new ControllerActionId("*", getActionId().getAction())))
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
        @NotNull private String _controller;
        @NotNull private String _action = "";
        private String _folder;

        public ControllerActionId(@NotNull String controller, @NotNull String action)
        {
            _controller = controller;
            _action = action;
        }

        public ControllerActionId(@NotNull String rootRelativeURL)
        {
            rootRelativeURL = stripQueryParams(stripHash(rootRelativeURL));
            rootRelativeURL = WebTestHelper.stripContextPath(rootRelativeURL);

            if (rootRelativeURL.startsWith("_webdav/"))
            {
                _controller = "_webdav";
                _folder = rootRelativeURL.substring("_webdav/".length());
                return;
            }
            if (rootRelativeURL.startsWith("_webfiles/"))
            {
                _controller = "_webfiles";
                _folder = rootRelativeURL.substring("_webfiles/".length());
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
                /* folder/controller-action */
                int dash = _action.lastIndexOf("-");
                _controller = _action.substring(0,dash);
                _action = _action.substring(dash+1);
            }
            else
            {
                /* controller/folders/action */
                int postControllerSlashIdx = rootRelativeURL.indexOf('/');
                if (-1 == postControllerSlashIdx)
                    throw new IllegalArgumentException("Unable to parse folder out of relative URL: \"" + rootRelativeURL + "\"");
                _controller = rootRelativeURL.substring(0, postControllerSlashIdx);
                rootRelativeURL = rootRelativeURL.substring(postControllerSlashIdx+1);
            }
            _folder = StringUtils.strip(rootRelativeURL, "/");
            if (_folder.endsWith("/"))
                _folder = _folder.substring(0,_folder.length()-1);
        }

        @NotNull public String getAction()
        {
            return _action;
        }

        @NotNull public String getController()
        {
            return _controller;
        }

        /**
         * Folder is parsed out for convenience only. Is ignored for equality and hash calculations.
         * @return container path from parsed URL
         */
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
            return Objects.hash(_controller.toLowerCase(), _action.toLowerCase());
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ControllerActionId that = (ControllerActionId) o;
            return _controller.equalsIgnoreCase(that._controller) &&
                    _action.equalsIgnoreCase(that._action);
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

            ControllerActionId actionWithHash = new ControllerActionId("/project/folder/controller-app.view#/app/hash/path");
            assertEquals("controller", actionWithHash.getController());
            assertEquals("project/folder", actionWithHash.getFolder());
            assertEquals("app", actionWithHash.getAction());

            ControllerActionId webdavAction = new ControllerActionId("/_webdav/fred");
            assertEquals("_webdav", webdavAction.getController());
        }

        TestLogger.log("Starting crawl...");

        // Breadth first search
        CrawlStats crawlStats = crawl();
        _crawlStats.put(_test.getClass().getSimpleName(), crawlStats);

        TestLogger.log("Crawl complete. " + crawlStats.getNewPages() + " pages visited, " + _actionsVisited.size() + " unique actions tested by all tests.");

        _dictionary.keySet().forEach(TestLogger::debug);
        TestLogger.debug("Injected:");
        TestLogger.increaseIndent();
        for (ControllerActionId aid : _parametersInjected.keySet())
        {
            TestLogger.debug(aid.toString());
            TestLogger.increaseIndent();
            for (String param : _parametersInjected.get(aid))
            {
                TestLogger.debug(param);
            }
            TestLogger.decreaseIndent();
        }
        TestLogger.decreaseIndent();
    }

    private CrawlStats crawl()
    {
        // Breadth first crawl
        int linkCount = 0;
        int currentDepth = 0;
        int maxDepth = 0;
        final Timer crawlTimer = new Timer(_maxCrawlTime);
        PriorityQueue<UrlToCheck> urlsToCheck = new PriorityQueue<UrlToCheck>(new Comparator<UrlToCheck>()
        {
            @Override
            public int compare(UrlToCheck u1, UrlToCheck u2)
            {
                return Double.compare(u1.priority,u2.priority);
            }
        });
        urlsToCheck.addAll(_startingUrls);

        TestLogger.log("Crawl depth : " + currentDepth);
        TestLogger.increaseIndent();

        // Loop through links in list until its empty or time runs out
        while (!urlsToCheck.isEmpty() && !crawlTimer.isTimedOut())
        {
            UrlToCheck urlToCheck = urlsToCheck.poll();
            assert null != urlsToCheck;
            while (urlToCheck.getDepth() > currentDepth)
            {
                currentDepth++;
                TestLogger.log("Crawl depth : " + currentDepth);
                TestLogger.increaseIndent();
            }

            if (urlToCheck.isVisitableURL())
            {
                maxDepth = Math.max(currentDepth, maxDepth);
                urlsToCheck.addAll(crawlLink(urlToCheck));
                linkCount++;
            }
        }

        while (currentDepth > 0)
        {
            currentDepth--;
            TestLogger.decreaseIndent();
        }

        return new CrawlStats(maxDepth, linkCount, _actionsVisited.size(), crawlTimer.elapsed(), _warnings);
    }

    public void validatePage(String url)
    {
        crawlLink(new UrlToCheck(null, url, -1));
    }

    private List<UrlToCheck> crawlLink(final UrlToCheck urlToCheck)
    {
        String relativeURL = urlToCheck.getRelativeURL();
        ControllerActionId actionId = new ControllerActionId(relativeURL);
        URL currentPageUrl;
        List<UrlToCheck> newUrlsToCheck = new ArrayList<>();

        // Keep track of where crawler has been
        _actionsVisited.add(actionId);
        _urlsChecked.add(stripQueryParams(relativeURL));
        URL origin = urlToCheck.getOrigin();
        int depth = urlToCheck.getDepth();
        String originMessage = origin != null ? "\nOriginating page: " + origin.toString() : "";

        try
        {
            try
            {
                long loadTime = _test.beginAt(relativeURL
                        .replace("[", "%5B")
                        .replace("]", "%5D")
                        .replace("{", "%7B")
                        .replace("}", "%7D")); // Escape brackets to prevent 400 errors
                _actionProfiler.updateActionProfile(relativeURL, loadTime);
            }
            catch (UnhandledAlertException alert)
            {
                // Ignore GWT deferredjs loading issue when navigating away from designer pages
                if (!alert.getAlertText().contains("Script Tag Failure - no status available"))
                    throw alert;
            }

            currentPageUrl = _test.getURL();

            int code = _test.getResponseCode();

            checkForForbiddenWords(relativeURL);

            if (!isIgnoredError(code, urlToCheck, origin))
            {
                // Check that there was no error
                if (code >= 400)
                {
                    String message = relativeURL + "\nproduced response code " + code + originMessage;
                    if (code == 403 && TestProperties.isPrimaryUserAppAdmin())
                    {
                        // Crawling as app admin is likely to hit numerous 403s. Don't fail immediately.
                        _test.checker().wrapAssertion(() -> fail(message));
                    }
                    else
                    {
                        fail(message);
                    }

                }
                List<String> serverError = _test.getTexts(Locator.css("table.server-error").findElements(_test.getDriver()));
                if (!serverError.isEmpty())
                {
                    String[] errorLines = serverError.get(0).split("\n");
                    fail(relativeURL + "\nproduced error: \"" + errorLines[0] + "\"." + originMessage);
                }

                // Find all the links at the site
                if (_remainingAttemptsToGetProjectLinks > 0 && depth == 1 && _test.isElementPresent(ProjectMenu.Locators.menuProjectNav))
                {
                    _remainingAttemptsToGetProjectLinks--;
                    try
                    {
                        _test.projectMenu().open();
                        _remainingAttemptsToGetProjectLinks = 0; // Got em
                    }
                    catch (NoSuchElementException ignore) { } // Hiccup with the project menu. Don't worry about it
                }

                if (code == 200 && _test.getDriver().getTitle().isEmpty())
                    _warnings.add("Action does not specify title: " + actionId.toString());

                if (depth >= 0 && !_terminalActions.contains(actionId)) // Negative depth indicates a one-off check
                {
                    List<String> linkAddresses = _test.getLinkAddresses();
                    List<String> formAddresses = _test.getFormAddresses();
                    linkAddresses.addAll(formAddresses);

                    for (String url : linkAddresses)
                    {
                        try
                        {
                            UrlToCheck candidateUrl = new UrlToCheck(currentPageUrl, url, depth + 1);
                            if (candidateUrl.isVisitableURL())
                            {
                                candidateUrl.setFromForm(formAddresses.contains(url));
                                newUrlsToCheck.add(candidateUrl);
                            }
                        }
                        catch (IllegalArgumentException badUrl)
                        {
                            if (!formAddresses.contains(url)) // forms might have strange target action (e.g. '../formulations')
                            {
                                origin = null; // Don't grab screenshot for origin page
                                throw new AssertionError("Unable to parse link: " + url, badUrl);
                            }
                        }
                    }
                }
            }
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
                throw rethrow; // AssertionErrors already contain page and origin information.
            else
                throw new RuntimeException(relativeURL + "\nTriggered an exception." + originMessage, rethrow);
        }

        if (currentPageUrl != null && urlToCheck.isInjectableURL() && _injectionCheckEnabled)
        {
            TestLogger.increaseIndent();
            try
            {
                testInjection(currentPageUrl);
            }
            finally
            {
                TestLogger.decreaseIndent();
            }
        }

        return newUrlsToCheck;
    }

    private static final ControllerActionId spiderAction = new ControllerActionId("admin", "spider");
    public static boolean isAdminSpiderAction(URL url)
    {
        ControllerActionId originAction = new ControllerActionId(url.toString());

        return spiderAction.equals(originAction);
    }

    private boolean isIgnoredError(int code, UrlToCheck urlToCheck, URL origin)
    {
        if (code == HttpStatus.SC_NOT_FOUND) // 404
        {
            if (origin == null || _actionsMayLinkTo404.contains(new ControllerActionId(origin.toString())))
                return true; // Ignore 404s from the initial set of links
        }

        if (code == HttpStatus.SC_METHOD_NOT_ALLOWED) // 405
        {
            return urlToCheck.isFromForm() // Expect most forms to reject GETs
                    || isAdminSpiderAction(origin); // Similarly, spider page lists many POST-only actions
        }

        return false;
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
    public static final String maliciousScript = "alert(\"" + injectedAlert + "\")";
    public static final String injectString = "-->\">'>'\"</script><script>" + maliciousScript + ";</script>";
    public static final String injectString2 = "-->\">'>'\"</script><img src=\"xss\" onerror=\"" + maliciousScript + ">";

    public static void tryInject(BaseWebDriverTest test, Runnable r)
    {
        tryInject(test, arg -> r.run(), null);
    }

    public static <F> void tryInject(BaseWebDriverTest test, Consumer<F> f, F arg)
    {
        try
        {
            f.accept(arg);

            checkForSqlInjection(test);

            checkForServerError(test); // Don't wait for post-test error check to catch these
        }
        catch (UnhandledAlertException ex)
        {
            String alertText = ex.getAlertText();
            if (alertText == null)
                alertText = test.cancelAlert();

            checkForJavaScriptInjection(alertText);

            checkForSqlInjection(test);

            throw ex;
        }
        catch (WebDriverException ex)
        {
            Alert alert;
            while (null != (alert = test.getAlertIfPresent()))
            {
                checkForJavaScriptInjection(alert.getText());
                alert.dismiss();
            }

            checkForSqlInjection(test);

            throw ex;
        }
    }

    private static void checkForJavaScriptInjection(String alertText)
    {
        if (alertText.startsWith(injectedAlert))
            fail("Crawler: Malicious script executed");
    }

    private static void checkForSqlInjection(BaseWebDriverTest test)
    {
        String html = test.getHtmlSource();
        // see StatementWrapper.java
        if (html.contains("SQL injection test failed"))
        {
            String url = test.getCurrentRelativeURL();
            fail("Crawler: SQL injection detected" + "\n" + url);
        }
    }

    private static void checkForServerError(BaseWebDriverTest test)
    {
        int responseCode = test.getResponseCode();
        if (responseCode == 200 || responseCode >= 500)
        {
            if (test.isElementPresent(Locator.css("table.server-error")))
            {
                String url = test.getCurrentRelativeURL();
                fail("Crawler: Server error detected\n" + url);
            }
        }
    }

    private void testInjection(URL start)
    {
        String base = stripQueryParams(stripHash(start.toString()));
        String query = StringUtils.trimToEmpty(start.getQuery());
        int q = base.indexOf('?');
        if (q != -1)
            base = base.substring(0,q);

        if (query.startsWith("?"))
            query = query.substring(1);

        Consumer<String> urlTester = urlMalicious -> {
            _test.beginAt(urlMalicious);
            _test.executeScript("return;"); // Trigger UnhandledAlertException
        };

        List<Map.Entry<String,String>> params = Collections.unmodifiableList(queryStringToEntries(query));

        // add parameters to global dictionary
        params.forEach(entry -> _dictionary.put(entry.getKey(), entry.getValue()));

        ControllerActionId actionId = new ControllerActionId(base);

        List<String> excludedParams = getExcludedParametersFromInjection().getOrDefault(actionId, Collections.emptyList());

        // Attempt injection against random parameters from the list of those found so far
        // Don't include 'start' or 'begin' actions unless they already have some parameters
        if (!"start".equalsIgnoreCase(actionId.getAction()) || !"begin".equalsIgnoreCase(actionId.getAction()) || params.size() > 0)
        {
            params = addRandomParams(params, actionId);
        }
        if (!params.isEmpty())
        {
            TestLogger.log("Attempting script injection");
        }
        for (int i=0 ; i < params.size() ; i++)
        {
            String key = params.get(i).getKey();
            if (excludedParams.contains(key) || _parametersInjected.containsMapping(actionId, key))
                continue;
            _parametersInjected.put(actionId, key);
            List<Map.Entry<String,String>> injectParams = new ArrayList<>(params);
            injectParams.remove(i);
            String xss = (random.nextInt()%2)==0 ? injectString : injectString2;
            xss = (random.nextInt()%3)==0 ? URLEncoder.encode(xss) : xss;
            String paramMalicious = key + "=" + xss;
            String queryMalicious = paramMalicious + "&" + queryStringFromEntries(injectParams);
            String urlMalicious = base + "?" + queryMalicious;
            try
            {
                tryInject(_test, urlTester, urlMalicious);
            }
            catch (Exception ex)
            {
                throw new AssertionError("Non-injection error while attempting script injection on " + actionId.toString() + "\n" +
                        "param: " + paramMalicious + "\n" +
                        "URL: " + urlMalicious, ex);
            }
        }
        // TODO this blows up jquery document completed handling, which causes pageload to not fire and then timeout
        /// tryInject(_test, urlTester, base + "?" + query + "#" + injectString);
    }

    static Random random = new Random();

    List<Map.Entry<String,String>> addRandomParams(List<Map.Entry<String,String>> in, ControllerActionId actionId)
    {
        _dictionary.remove("_print"); // Print view causes Crawler to hang for some actions
        List<Map.Entry<String,String>> ret = new ArrayList<>(in);

        if (in.size() < 10)
        {
            List<String> additionalParams = new ArrayList<>(_dictionary.keySet());
            additionalParams.removeAll(_parametersInjected.get(actionId)); // Don't repeat injection attempts
            additionalParams.removeAll(in.stream().map(Map.Entry::getKey).collect(Collectors.toList())); // Don't add duplicate params
            Collections.shuffle(additionalParams);

            for (int i = 0; i < additionalParams.size() && ret.size() < 10; i++)
            {
                String key = additionalParams.get(i);
                ret.add(new AbstractMap.SimpleImmutableEntry<>(key, _dictionary.get(key)));
            }
        }
        return Collections.unmodifiableList(ret);
    }

    List<Map.Entry<String,String>> queryStringToEntries(String q)
    {
        if (StringUtils.isBlank(q))
            return Collections.emptyList();
        return Arrays.stream(StringUtils.split(q, '&'))
                .map(pair -> {
                    int i = pair.indexOf('=');
                    String k = i==-1 ? pair : pair.substring(0,i);
                    String v = i==-1 ? "" : pair.substring(i+1);
                    try
                    {
                        return new AbstractMap.SimpleImmutableEntry<>(URLDecoder.decode(k), URLDecoder.decode(v));
                    }
                    catch (IllegalArgumentException ex)
                    {
                        throw new IllegalArgumentException("Unable to decode URL parameter: " + pair, ex);
                    }
                })
                .collect(Collectors.toList());
    };

    String queryStringFromEntries(List<Map.Entry<String,String>> list)
    {
        StringBuilder sb = new StringBuilder();
        String and = "";
        list.forEach(e ->
        {
            if (sb.length()!=0)
                sb.append('&');
            sb.append(URLEncoder.encode(e.getKey()));
            sb.append('=');
            sb.append(null==e.getValue()?"":URLEncoder.encode(e.getValue()));
        }
        );
        return sb.toString();
    }
}
