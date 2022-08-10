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
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.jetbrains.annotations.NotNull;
import org.labkey.remoteapi.collections.CaseInsensitiveHashMap;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.ExtraSiteWrapper;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.TestProperties;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.core.ProjectMenu;
import org.openqa.selenium.Alert;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.labkey.test.WebTestHelper.getBaseURL;
import static org.labkey.test.WebTestHelper.stripContextPath;

public class Crawler
{
    private static final MultiValuedMap<ControllerActionId, String> _parametersInjected = new HashSetValuedHashMap<>();
    private static final Set<ControllerActionId> _actionsVisited = new HashSet<>();
    private static final Set<ControllerActionId> _actionsWithErrors = new HashSet<>();
    private static final Set<String> _urlsChecked = new HashSet<>();
    private static final Map<String, CrawlStats> _crawlStats = new LinkedHashMap<>();

    // All parameters seen by the crawler. Used to randomly attempt injection against parameters not found in UI
    private static final LinkedHashMap<String,String> _dictionary = new LinkedHashMap<>();

    static
    {
        Arrays.asList("rowid", "name", "userId", "query.sort", "query.rowid~eq", "query.name~contains", "returnUrl")
            .forEach(s -> _dictionary.put(s,""));
    }

    private final List<ControllerActionId> _excludedActions;
    private final List<ControllerActionId> _terminalActions;
    private final List<ControllerActionId> _actionsExcludedFromInjection;
    private final List<ControllerActionId> _actionsMayLinkTo404;
    private final List<Function<UrlToCheck, Boolean>> _specialCrawlExclusions;
    private final Collection<String> _adminControllers;
    private final Collection<String> _forbiddenWords;
    private final boolean _prioritizeAdminPages;
    private final ArrayList<UrlToCheck> _startingUrls = new ArrayList<>();
    private final Duration _maxCrawlTime;
    private final BaseWebDriverTest _test;
    private final List<String> _warnings = new ArrayList<>();
    private final boolean _injectionCheckEnabled;
    private final Set<String> _projects = Collections.newSetFromMap(new CaseInsensitiveHashMap<>());
    private final Set<String> _urlsVisited = new HashSet<>();

    private int _remainingAttemptsToGetProjectLinks = 4;
    private int _maxDepth = 4;

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
        _specialCrawlExclusions = getSpecialCrawlExclusions();
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
            new ControllerActionId("admin", "actions"), // Gets hit often in normal testing
            new ControllerActionId("admin", "addTab"),
            new ControllerActionId("admin", "credits"), // Gets checked by BasicTest
            new ControllerActionId("admin", "deleteFolder"),
            new ControllerActionId("admin", "doCheck"),
            new ControllerActionId("admin", "dumpHeap"),
            new ControllerActionId("admin", "mapNetworkDrive"), // 404 on non-Windows
            new ControllerActionId("admin", "memTracker"),
            new ControllerActionId("admin", "queryStackTraces"),
            new ControllerActionId("admin", "resetErrorMark"),
            new ControllerActionId("admin", "resetQueryStatistics"),
            new ControllerActionId("admin", "shortURLAdmin"),
            new ControllerActionId("admin", "showAllErrors"),
            new ControllerActionId("admin", "showErrorsSinceMark"), // Gets hit often in normal testing
            new ControllerActionId("admin", "showPrimaryLog"), // Can take very long to load
            new ControllerActionId("admin-sql", "saveReorderedScript"),
            new ControllerActionId("assay", "assayDetailRedirect"),
            new ControllerActionId("dumbster", "begin"),
            new ControllerActionId("filetransfer", "auth"), // redirects to external site
            new ControllerActionId("genotyping", "analyze"),    // Crawler doesn't like NotFoundException that the test generates
            new ControllerActionId("login", "createToken"),
            new ControllerActionId("login", "logout"),
            new ControllerActionId("login", "setAuthenticationParameter"),
            new ControllerActionId("login", "setPassword"),
            new ControllerActionId("login", "verifyToken"), // returns XML, which WDW.waitForPageToLoad can't handle
            new ControllerActionId("ms2", "pepSearch"), // TODO: Issue 36995: Check for SQL injection in StatementWrapper is not precise enough
            new ControllerActionId("ms2", "showList"),
            new ControllerActionId("ms2", "showParamsFile"),
            // Tested directly in XTandemTest
            new ControllerActionId("ms2", "doProteinSearch"),
            new ControllerActionId("nlp", "runPipeline"),
            new ControllerActionId("pipeline-analysis", "analyze"), // Doesn't navigate
            new ControllerActionId("project", "togglePageAdminMode"),
            new ControllerActionId("query", "printRows"), // Data region print button. 404s on "TargetedMS Runs" grid
            new ControllerActionId("reports", "streamFile"),
            new ControllerActionId("study", "manageStudyProperties"), // Intermittently triggers form dirty alert

            // Disable crawler for single-page apps until we make `beginAt` work with them
            new ControllerActionId("biologics", "app"),
            new ControllerActionId("cds", "app"),
            new ControllerActionId("samplemanager", "app"),

            // Actions that error from Admin->GoToModule->MoreModules when module is not enabled
            new ControllerActionId("biologics", "begin"),
            new ControllerActionId("datafinder", "begin"),
            new ControllerActionId("ehr_compliancedb", "requirementDetails"),
            new ControllerActionId("hdrl", "begin"),
            new ControllerActionId("onprc_billingpublic", "begin"),
            new ControllerActionId("reagent", "begin")
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
        controllers.add("crawlerTest");
        controllers.add("editableModule");
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

    public List<Function<UrlToCheck, Boolean>> getSpecialCrawlExclusions()
    {
        final List<Function<UrlToCheck, Boolean>> urlVisitableChecks = new ArrayList<>();

        // Don't crawl pipeline status if it will redirect.
        final ControllerActionId pipelineStatusAction = new ControllerActionId("pipeline-status", "details");
        urlVisitableChecks.add(url -> pipelineStatusAction.equals(url.getActionId()) && url.getRelativeURL().contains("redirect=1"));

        return urlVisitableChecks;
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

    public Set<String> getUrlsVisited()
    {
        return new HashSet<>(_urlsVisited);
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

    public static class CrawlStats
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
                    final String relativeURL = urlText.substring(relativeURLStart);
                    if (!relativeURL.isBlank())
                    {
                        _relativeURL = relativeURL;
                        _actionId = new ControllerActionId(_relativeURL);
                    }
                    else
                    {
                        _relativeURL = null;
                        _actionId = null;
                    }
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

            for (Function<UrlToCheck, Boolean> check : _specialCrawlExclusions)
            {
                // Exclude particular URLs based on other conditions
                if (check.apply(this))
                {
                    return false;
                }
            }

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
        @NotNull private final String _controller;
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
        int maxDepth = 0;
        final Timer crawlTimer = new Timer(_maxCrawlTime);
        PriorityQueue<UrlToCheck> urlsToCheck = new PriorityQueue<>(Comparator.comparingDouble(u -> u.priority));
        urlsToCheck.addAll(_startingUrls);

        // Loop through links in list until its empty or time runs out
        while (!urlsToCheck.isEmpty() && !crawlTimer.isTimedOut())
        {
            UrlToCheck urlToCheck = urlsToCheck.poll();
            if (urlToCheck != null && urlToCheck.isVisitableURL())
            {
                maxDepth = Math.max(urlToCheck.getDepth(), maxDepth);
                urlsToCheck.addAll(crawlLink(urlToCheck));
                linkCount++;
            }
        }

        return new CrawlStats(maxDepth, linkCount, _actionsVisited.size(), crawlTimer.elapsed(), _warnings);
    }

    @LogMethod
    public void validatePage(@LoggedParam String url)
    {
        crawlLink(new UrlToCheck(null, url, 0));
    }

    /**
     * Open the specified URL in the current browser
     * @param relativeUrl URL to navigate to
     * @return 'true' if opening the URL navigated
     */
    private boolean beginAt(String relativeUrl)
    {
        _urlsVisited.add(relativeUrl);

        // Escape brackets to prevent 400 errors
        relativeUrl = relativeUrl
                .replace("[", "%5B")
                .replace("]", "%5D")
                .replace("{", "%7B")
                .replace("}", "%7D");
        if (relativeUrl.startsWith(getBaseURL()))
            relativeUrl = relativeUrl.substring(getBaseURL().length());
        relativeUrl = stripContextPath(relativeUrl);
        String logMessage = "";
        Mutable<File[]> downloadedFiles = new MutableObject<>();

        try
        {
            String messagePrefix = "Navigating to ";
            if (relativeUrl.length() == 0)
            {
                logMessage = messagePrefix + "root";
            }
            else
            {
                logMessage = messagePrefix + relativeUrl;
                if (relativeUrl.charAt(0) != '/')
                {
                    relativeUrl = "/" + relativeUrl;
                }
            }

            final String fullURL = WebTestHelper.getBaseURL() + relativeUrl;

            Mutable<Boolean> navigated = new MutableObject<>(true);
            final File downloadDir = BaseWebDriverTest.getDownloadDir();
            final File[] existingDownloads = downloadDir.listFiles();

            long elapsedTime = _test.doAndMaybeWaitForPageToLoad(WebDriverWrapper.WAIT_FOR_PAGE, () -> {
                final WebElement mightGoStale = Locators.documentRoot.findElement(_test.getDriver());
                ExpectedCondition<Boolean> stalenessOf = ExpectedConditions.stalenessOf(mightGoStale);
                // 'getDriver().navigate().to(fullURL)' assumes navigation and fails for file downloads
                _test.executeScript("document.location = arguments[0]", fullURL);
                //noinspection ResultOfMethodCallIgnored
                if (!WebDriverWrapper.waitFor(() -> {
                    boolean stale;
                    try
                    {
                        stale = stalenessOf.apply(null);
                    }
                    catch (NullPointerException npe)
                    {
                        // Staleness check throws NPE sometimes when there's an alert present
                        _test.executeScript("return;"); // Try to trigger 'UnhandledAlertException'
                        return false;
                    }
                    if (stale)
                    {
                        // Wait for page to load when element goes stale
                        return true; // Stop waiting
                    }
                    else if (downloadDir.isDirectory()) // Don't check for download if dir doesn't exist
                    {
                        File[] filesArray = WebDriverWrapper.getNewFiles(0, downloadDir, existingDownloads);
                        downloadedFiles.setValue(filesArray);
                        if (downloadedFiles.getValue().length > 0)
                        {
                            navigated.setValue(false); // Don't wait for page load when a download occurs
                            return true; // Stop waiting
                        }
                    }
                    return false; // No navigation or download detected. Continue waiting.
                }, WebDriverWrapper.WAIT_FOR_PAGE))
                {
                    TestLogger.warn("URL didn't trigger a download or navigation: " + fullURL);
                }
                return navigated.getValue();
            });

            if (!navigated.getValue())
            {
                logMessage = logMessage.replace(messagePrefix, "Downloading from ");
            }

            logMessage += TestLogger.formatElapsedTime(elapsedTime);

            return navigated.getValue();
        }
        finally
        {
            TestLogger.info(logMessage); // log after navigation to
            if (downloadedFiles.getValue() != null)
            {
                Arrays.stream(downloadedFiles.getValue()).forEach(file -> {
                    TestLogger.info("  \u2517" + file.getName()); // Log downloaded files
                    FileUtils.deleteQuietly(file); // Clean up crawled downloads
                });
            }
        }
    }

    private List<UrlToCheck> crawlLink(final UrlToCheck urlToCheck)
    {
        String relativeURL = urlToCheck.getRelativeURL();
        ControllerActionId actionId = new ControllerActionId(relativeURL);
        URL actualUrl; // URL might redirect
        boolean navigated = true; // URL might download
        List<UrlToCheck> newUrlsToCheck = new ArrayList<>();

        // Keep track of where crawler has been
        _actionsVisited.add(actionId);
        _urlsChecked.add(stripQueryParams(relativeURL));
        URL origin = urlToCheck.getOrigin();
        int depth = urlToCheck.getDepth();
        String originMessage = (origin != null ? "\nOriginating page: " + origin : "") +
                "\nTarget Page: " + relativeURL;

        try
        {
            try
            {
                navigated = beginAt(relativeURL);
            }
            catch (UnhandledAlertException alert)
            {
                if (isRealFailure(alert))
                    throw alert;
            }

            if (navigated) // These checks were already performed if navigation didn't occur
            {
                actualUrl = _test.getURL();
                if (!actualUrl.toString().endsWith(relativeURL))
                {
                    originMessage = originMessage + "\nRedirected to: " + actualUrl;
                }

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
                        catch (WebDriverException ignore)
                        {
                        } // Hiccup with the project menu, try again next time.
                    }

                    if (code == 200 && _test.getDriver().getTitle().isEmpty())
                        _warnings.add("Action does not specify title: " + actionId);

                    if (depth >= 0 && !_terminalActions.contains(actionId)) // Negative depth indicates a one-off check
                    {
                        List<Pair<String, Map<String, String>>> linksWithAttributes = _test.getLinkAddresses();
                        for (Pair<String, Map<String, String>> linkWithAttributes : linksWithAttributes)
                        {
                            String href = linkWithAttributes.getLeft();
                            if (href.contains("://") && !href.startsWith(WebTestHelper.getBaseURL())) // Remote URL
                            {
                                Map<String, String> attributes = linkWithAttributes.getRight();
                                String target = StringUtils.trimToEmpty(attributes.get("target"));
                                List<String> rel = Arrays.asList(StringUtils.trimToEmpty(attributes.get("rel")).split(" +"));
                                if (target.equals("_blank"))
                                {
                                    // Issue 40708: Create automated tests to look for anchor tags with link to an outside server
                                    MatcherAssert.assertThat(String.format("Bad 'rel' attribute for link to %s. On Page: %s", href, actualUrl),
                                            rel, CoreMatchers.hasItems("noopener", "noreferrer"));
                                }
                            }
                        }
                        List<String> linkAddresses = linksWithAttributes.stream().map(Pair::getLeft).collect(Collectors.toList());
                        List<String> formAddresses = _test.getFormAddresses();
                        linkAddresses.addAll(formAddresses);

                        for (String url : linkAddresses)
                        {
                            try
                            {
                                UrlToCheck candidateUrl = new UrlToCheck(actualUrl, url, depth + 1);
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
                                    throw new AssertionError("Unable to parse link: \"" + url + "\". " + originMessage, badUrl);
                                }
                            }
                        }
                    }
                }
            }
            else
            {
                // Did not navigate. Test download URL for injection
                actualUrl = new URL(WebTestHelper.getBaseURL() + relativeURL);
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
            else if (rethrow instanceof TimeoutException)
                throw new RuntimeException(relativeURL + " failed to render. " + originMessage +
                        " It may be a file download which is unsupported by the crawler", rethrow); // Improve error reporting for downloads, see issue 42661
            else
                throw new RuntimeException("Crawler threw " + rethrow.getClass().getSimpleName() + ".\n" +
                    "Target page: " + relativeURL + "\n" +
                    originMessage, rethrow);
        }
        catch (MalformedURLException e)
        {
            throw new RuntimeException(e);
        }

        if (urlToCheck.isInjectableURL() && _injectionCheckEnabled)
        {
            TestLogger.increaseIndent();
            try
            {
                testInjection(actualUrl);
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
            if (origin == null // Ignore 404s from the initial set of links
                    || _actionsMayLinkTo404.contains(new ControllerActionId(origin.toString())))
            {
                return true;
            }
            else if (urlToCheck.isFromForm()) // Forms may 404 with bad input
            {
                return doesUrlExist(urlToCheck); // Ignore if action exists, forms may 404 with bad input
            }
        }

        if (code == HttpStatus.SC_METHOD_NOT_ALLOWED) // 405
        {
            return urlToCheck.isFromForm() // Expect most forms to reject GETs
                    || isAdminSpiderAction(origin); // Similarly, spider page lists many POST-only actions
        }

        return false;
    }

    private boolean doesUrlExist(UrlToCheck urlToCheck)
    {
        final String url = stripQueryParams(urlToCheck.getRelativeURL());

        // Check whether 404 was due to a missing action
        HttpContext context = WebTestHelper.getBasicHttpContext();
        HttpResponse response = null;

        try (CloseableHttpClient httpClient = (CloseableHttpClient)WebTestHelper.getHttpClient())
        {
            // LabKey actions don't support HEAD requests. Only a non-existent action will 404
            var method = new HttpHead(url);
            response = httpClient.execute(method, context);
            // If url 404s, then the action doesn't exist
            return response.getStatusLine().getStatusCode() != HttpStatus.SC_NOT_FOUND;
        }
        catch (IOException | IllegalArgumentException e)
        {
            TestLogger.warn("Unable to verify existence of action: " + urlToCheck.getActionId(), e);
            return true;
        }
        finally
        {
            if (null != response)
                EntityUtils.consumeQuietly(response.getEntity());
        }
    }

    protected void checkForForbiddenWords(String relativeURL)
    {
        if (!_forbiddenWords.isEmpty())
        {
            String responseText = _test.getResponseText().toLowerCase();

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
    public static final String maliciousScript = "alert('" + injectedAlert + "')";
    public static final String injectScriptBlock = "-->\">'>'\"<script>" + maliciousScript + "</script>";
    public static final String injectAttributeScript = "-->\">'>'\"</script><img src=\"xss\" onerror=\"" + maliciousScript + "\">";

    public static void tryInject(WebDriverWrapper test, Runnable r)
    {
        tryInject(test, arg -> {r.run(); return true;}, null);
    }

    /**
     * Apply the provided function and check for script injection.
     * @param test WebDriverWrapper to provide access to browser state
     * @param f function to apply. Should attempt to trigger script injection (usually by navigating).
     *          The return value indicates whether the page should be checked for errors.
     *          If it returns 'false', only 'UnhandledAlertException' and 'WebDriverException' will trigger validation
     * @param arg argument to pass in to the provided function. Usually a URL to allow funtion to be reused
     * @param <F> argument type to pass to function
     */
    public static <F> void tryInject(WebDriverWrapper test, Function<F, Boolean> f, F arg)
    {
        try
        {
            Boolean checkDestination = f.apply(arg);

            if (checkDestination)
            {
                checkForSqlInjection(test);

                checkForError(test); // Don't wait for post-test error check to catch these
            }
        }
        catch (UnhandledAlertException ex)
        {
            String alertText = ex.getAlertText();
            if (alertText == null)
                alertText = test.cancelAlert();
            else if (alertText.isBlank())
                alertText = ex.getMessage();

            checkForJavaScriptInjection(alertText);

            checkForSqlInjection(test);

            throw ex;
        }
        catch (Exception ex)
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
        if (alertText.contains(injectedAlert))
            fail("Crawler: Malicious script executed");
    }

    private static void checkForSqlInjection(WebDriverWrapper test)
    {
        String html = test.getHtmlSource();
        // see StatementWrapper.java
        if (html.contains("SQL injection test failed"))
        {
            String url = test.getCurrentRelativeURL();
            fail("Crawler: SQL injection detected" + "\n" + url);
        }
    }

    private static void checkForError(WebDriverWrapper test)
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
        else if (responseCode == 400 && !test.onLabKeyPage())
        {
            // 400 on a non-LabKey page probably means the crawler generated a bad URL
            fail("Crawler: Bad request: " + test.getDriver().getCurrentUrl());
        }
    }

    /** Ignore GWT alerts from designer pages */
    private boolean isRealFailure(Exception e)
    {
        return !(e instanceof UnhandledAlertException && (
                e.getMessage().contains("Script Tag Failure - no status available") || // alert when navigating away quickly
                e.getMessage().contains("Service_Proxy") // Alert from various GWT services (e.g. "from StudyDefinitionService_Proxy.getBlank")
        ));
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

        Function<String, Boolean> urlTester = urlMalicious -> {
            boolean navigated = beginAt(urlMalicious);
            if (navigated)
            {
                _test.executeScript("return;"); // Trigger UnhandledAlertException
            }
            return navigated;
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
            //noinspection SuspiciousListRemoveInLoop
            injectParams.remove(i);
            String xss = (random.nextInt()%2)==0 ? injectScriptBlock : injectAttributeScript;
            xss = URLEncoder.encode(xss, StandardCharsets.US_ASCII);
            String paramMalicious = key + "=" + xss;
            String queryMalicious = paramMalicious + "&" + queryStringFromEntries(injectParams);
            String urlMalicious = base + "?" + queryMalicious;
            try
            {
                tryInject(_test, urlTester, urlMalicious);
            }
            catch (Exception ex)
            {
                if (isRealFailure(ex))
                {
                    throw new AssertionError("Non-injection error while attempting script injection on " + actionId + "\n" +
                            "param: " + paramMalicious + "\n" +
                            "URL: " + urlMalicious, ex);
                }
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
            additionalParams.removeAll(in.stream().map(Map.Entry::getKey).toList()); // Don't add duplicate params
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
                        return new AbstractMap.SimpleImmutableEntry<>(URLDecoder.decode(k, StandardCharsets.UTF_8), URLDecoder.decode(v, StandardCharsets.UTF_8));
                    }
                    catch (IllegalArgumentException ex)
                    {
                        throw new IllegalArgumentException("Unable to decode URL parameter: " + pair, ex);
                    }
                })
                .collect(Collectors.toList());
    }

    String queryStringFromEntries(List<Map.Entry<String,String>> list)
    {
        StringBuilder sb = new StringBuilder();
        list.forEach(e ->
        {
            if (sb.length()!=0)
                sb.append('&');
            sb.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8));
            sb.append('=');
            sb.append(null==e.getValue()?"":URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
        }
        );
        return sb.toString();
    }
}
