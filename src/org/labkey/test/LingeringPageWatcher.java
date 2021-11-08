package org.labkey.test;

import org.apache.commons.lang3.tuple.Pair;
import org.labkey.test.util.Crawler.ControllerActionId;
import org.labkey.test.util.TestLogger;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static java.util.Collections.reverseOrder;
import static java.util.Comparator.comparing;

class LingeringPageWatcher implements WebDriverWrapper.PageLoadListener
{
    private static final LingeringPageWatcher INSTANCE = new LingeringPageWatcher();

    private final Duration MAX_DURATION = Duration.ofSeconds(30);
    private final Set<ControllerActionId> ignoredActions = Set.of(new ControllerActionId("*", "app"));

    // Remember which pages we lingered on the longest
    private final Set<Pair<String, Duration>> slowUrls = new TreeSet<>(reverseOrder(comparing(Pair::getRight)));
    // Remember how many times we linger on particular actions
    private final Map<ControllerActionId, Integer> slowActions = new HashMap<>();

    private Instant lastNavigation = null;

    private LingeringPageWatcher()
    {
        // Private constructor for singleton
    }

    public static LingeringPageWatcher get()
    {
        return INSTANCE;
    }

    public void reset()
    {
        slowUrls.clear();
        slowActions.clear();
        lastNavigation = null;
    }

    public List<Pair<String, Duration>> getSlowUrls(int maxSize)
    {
        return slowUrls.stream()
                .limit(maxSize)
                .collect(Collectors.toList());
    }

    public List<Map.Entry<ControllerActionId, Integer>> getProblemActions(int maxSize)
    {
        return slowActions.entrySet().stream()
                .sorted(reverseOrder(Map.Entry.comparingByValue()))
                .limit(maxSize)
                .collect(Collectors.toList());
    }

    @Override
    public void beforePageLoad(WebDriverWrapper wrapper)
    {
        if (lastNavigation != null)
        {
            Duration lingeredOnPage = Duration.between(lastNavigation, Instant.now());
            if (lingeredOnPage.compareTo(MAX_DURATION) > 0)
            {
                final String relativeUrl = wrapper.getCurrentRelativeURL();
                final ControllerActionId actionId = new ControllerActionId(relativeUrl);
                if (!isIgnored(actionId))
                {
                    int previousCount = slowActions.getOrDefault(actionId, 0);
                    slowActions.put(actionId, previousCount + 1);
                    slowUrls.add(Pair.of(relativeUrl, lingeredOnPage));
                    TestLogger.warn("Test lingered on " + relativeUrl + " for " + lingeredOnPage);
                }
            }
        }
    }

    @Override
    public void afterPageLoad(WebDriverWrapper wrapper)
    {
        lastNavigation = Instant.now();
    }

    private boolean isIgnored(ControllerActionId actionId)
    {
        return ignoredActions.contains(actionId) ||
                ignoredActions.contains(new ControllerActionId(actionId.getController(), "*")) ||
                ignoredActions.contains(new ControllerActionId("*", actionId.getAction())) ||
                "home".equalsIgnoreCase(actionId.getFolder());
    }
}
