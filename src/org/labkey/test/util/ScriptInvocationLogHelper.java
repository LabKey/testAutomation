/*
 * Copyright (c) 2026 LabKey Corporation
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

import org.labkey.test.WebDriverWrapper;

import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Assertions against the org.labkey.api.reports.ScriptInvocationLog category, which records duration and a
 * caller-supplied label for every external script the server runs.
 */
public abstract class ScriptInvocationLogHelper
{
    private static final Pattern SCRIPT_COMPLETION = Pattern.compile(
            "script (?<outcome>done|failed) engine=\\S+ label=(?<label>.*?) durationMs=(?<durationMs>\\d+)(?<trailing>.*)");

    /**
     * Assert that a script logged the given outcome since the last {@link Log4jUtils#resetLogMark()} with a label
     * containing every fragment.
     */
    public static void assertScriptLogged(WebDriverWrapper test, String outcome, String... labelFragments) throws IOException
    {
        String log = Log4jUtils.getLogSinceMark(test);
        Matcher matcher = SCRIPT_COMPLETION.matcher(log);

        while (matcher.find())
        {
            String label = matcher.group("label");
            if (outcome.equals(matcher.group("outcome")) && Arrays.stream(labelFragments).allMatch(label::contains))
            {
                long durationMs = Long.parseLong(matcher.group("durationMs"));
                assertTrue("Logged durationMs doesn't look like an elapsed time: " + durationMs,
                        durationMs < 1_000_000L && durationMs > 0);
                assertEquals("'script " + outcome + "' line should end at the duration", "", matcher.group("trailing").trim());
                return;
            }
        }

        fail("No 'script " + outcome + "' line with a label containing " + Arrays.toString(labelFragments) + ". Log since mark:\n" + log);
    }
}
