/*
 * Copyright (c) 2025-2026 LabKey Corporation
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
package org.labkey.test.tests.upgrade;

import org.jetbrains.annotations.NotNull;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.TestProperties;
import org.labkey.test.util.TestLogger;
import org.labkey.test.util.Version;
import org.labkey.test.util.VersionRange;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.trimToNull;

/**
 * Base test class for tests that set up data and configure a server, then verify the persistence or modification of
 * those data and configurations after upgrading to a newer version of LabKey.<br>
 * The {@link EarliestVersion} and {@link LatestVersion} annotations can be used to skip particular tests when they are
 * not relevant to the version of LabKey being upgraded from (specified in the {@code webtest.upgradePreviousVersion}
 * system property).<br>
 * The setup steps will be skipped if the {@code webtest.upgradeSetup} system property is set to {@code false}.
 * <h2>Writing a new upgrade test</h2>
 * <ul>
 *     <li>Use a package name containing {@code upgrade}. Do all setup in {@link #doSetup()} rather than a
 *     {@code @BeforeClass}, and prefer APIs to the UI. {@code @Test} methods must be read-only or re-runnable, since
 *     cleanup is skipped after the upgrade.</li>
 *     <li><b>The setup phase runs the older branch's copy of the test</b>, so a test that exists only on the newer
 *     branch never gets setup and anything touching its project or users fails. Commit a matching copy to a feature
 *     branch on the preceding ESR release ({@code fb_coolUpgrade} plus {@code 26.3_fb_coolUpgrade}); TeamCity pairs
 *     them. Both copies do the full setup, allowing for API changes between the releases. The older one can
 *     do minimal validation and just needs one {@code @Test}. Names the newer copy looks up, such as the project and test
 *     users, must match exactly.</li>
 *     <li>Guard methods that depend on setup data with {@link EarliestVersion} naming the earliest release that
 *     carries a copy of the test, which is not necessarily the release the feature shipped in.</li>
 *     <li>One leg of the pipeline validates a build against itself, where nothing changed and {@link #setupVersion} is
 *     the running version. {@link #wasSetupBefore(String)} and {@link #wasSetupWithin(String, String)} adjust
 *     expectations for that leg; the annotations cannot express it.</li>
 * </ul>
 */
public abstract class BaseUpgradeTest extends BaseWebDriverTest
{

    protected static final boolean isUpgradeSetupPhase = TestProperties.getBooleanProperty("webtest.upgradeSetup", true);
    /** The version the setup phase ran, from {@code webtest.upgradePreviousVersion}; the running version if unset. */
    protected static final Version setupVersion = isUpgradeSetupPhase ? TestProperties.getProductVersion() :
        Optional.ofNullable(trimToNull(System.getProperty("webtest.upgradePreviousVersion"))).map(Version::new)
            .orElse(TestProperties.getProductVersion());

    @Override
    protected boolean skipCleanup(boolean afterTest)
    {
        return afterTest || !isUpgradeSetupPhase;
    }

    @BeforeClass
    public static void setupProject() throws Exception
    {
        BaseUpgradeTest currentTest = BaseWebDriverTest.getCurrentTest();

        Class<?> testClass = currentTest.getClass();
        String earliestVersion = Optional.ofNullable(testClass.getAnnotation(EarliestVersion.class))
                .map(EarliestVersion::value).orElse(null);
        String latestVersion = Optional.ofNullable(testClass.getAnnotation(LatestVersion.class))
                .map(LatestVersion::value).orElse(null);

        Assume.assumeTrue("Test class not valid when upgrading from version: " + setupVersion,
                VersionRange.versionRange(earliestVersion, latestVersion).contains(setupVersion)
        );

        if (isUpgradeSetupPhase)
        {
            currentTest.doSetup();
        }
        else
        {
            TestLogger.info("Skipping setup for %s. Verifying upgrade.". formatted(testClass.getSimpleName()));
        }
    }

    protected abstract void doSetup() throws Exception;

    @Rule
    public final TestRule upgradeVersionCheck = new UpgradeVersionCheck();

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }

    /**
     * Checks if the setup for the current test was performed in a version prior to the specified version.
     *
     * @param version The version to check against.
     * @return {@code true} if the setup version is earlier than the specified version.
     */
    protected boolean wasSetupBefore(String version)
    {
        return !wasSetupWithin(version, null);
    }

    /**
     * Checks if the setup for the current test was performed within the specified version range (inclusive).
     *
     * @param earliestVersion The earliest version in the range (inclusive).
     * @param latestVersion The latest version in the range (inclusive).
     * @return {@code true} if the setup version is within the specified range.
     */
    protected boolean wasSetupWithin(String earliestVersion, String latestVersion)
    {
        return VersionRange.versionRange(earliestVersion, latestVersion).contains(setupVersion);
    }

    /**
     * Annotates test methods that should only run when upgrading from particular LabKey versions, as specified in
     * {@code webtest.upgradePreviousVersion}.<br>
     * Specifies the earliest version of the test class that performed the required setup for the annotated method.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    protected @interface EarliestVersion
    {
        String value();
    }

    /**
     * Annotates test methods or classes that should only run when upgrading from particular LabKey versions, as
     * specified in {@code webtest.upgradePreviousVersion}.<br>
     * Specifies the latest version of the test class that performed the required setup for the annotated method or class.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    protected @interface LatestVersion {
        String value();
    }

    private static class UpgradeVersionCheck implements TestRule
    {

        @Override
        public @NotNull Statement apply(@NotNull Statement base, Description description)
        {
            String earliestVersion = Optional.ofNullable(description.getAnnotation(EarliestVersion.class))
                .map(EarliestVersion::value).orElse(null);
            String latestVersion = Optional.ofNullable(description.getAnnotation(LatestVersion.class))
                .map(LatestVersion::value).orElse(null);

            if (isUpgradeSetupPhase || setupVersion == null || (earliestVersion == null && latestVersion == null))
            {
                return base; // Run the test normally
            }

            return new Statement()
            {
                @Override
                public void evaluate() throws Throwable
                {
                    Assume.assumeTrue("Test not valid when upgrading from version: " + setupVersion,
                        VersionRange.versionRange(earliestVersion, latestVersion).contains(setupVersion)
                    );
                    base.evaluate();
                }
            };
        }
    }

}
