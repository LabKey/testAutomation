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
 * Base test class for tests that setup data and configure a server then verify the persistence or modification of those
 * data and configurations after upgrading to a newer version of LabKey.<br>
 * The {@code EariestVersion} and {@code LatestVersion} annotations can be used to skip particular tests when they are
 * not relevant to the version of LabKey being upgraded from (specified in the {@code webtest.upgradePreviousVersion}
 * system property).<br>
 * The setup steps will be skipped if the {@code webtest.upgradeSetup} system property is set to {@code false}.
 */
public abstract class BaseUpgradeTest extends BaseWebDriverTest
{

    protected static final boolean isUpgradeSetupPhase = TestProperties.getBooleanProperty("webtest.upgradeSetup", true);
    protected static final Version previousVersion = Optional.ofNullable(trimToNull(System.getProperty("webtest.upgradePreviousVersion")))
        .map(Version::new).orElse(null);

    @Override
    protected boolean skipCleanup(boolean afterTest)
    {
        return afterTest || !isUpgradeSetupPhase;
    }

    @BeforeClass
    public static void setupProject() throws Exception
    {
        BaseUpgradeTest currentTest = BaseWebDriverTest.getCurrentTest();

        if (isUpgradeSetupPhase)
        {
            currentTest.doSetup();
        }
        else
        {
            TestLogger.info("Skipping setup for %s. Verifying upgrade.". formatted(currentTest.getClass().getSimpleName()));
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
     * Annotates test methods that should only run when upgrading from particular LabKey versions, as specified in
     * {@code webtest.upgradePreviousVersion}.<br>
     * Specifies the earliest version of the test class that performed the required setup for the annotated method.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    protected @interface EariestVersion {
        String value();
    }

    /**
     * Annotates test methods that should only run when upgrading from particular LabKey versions, as specified in
     * {@code webtest.upgradePreviousVersion}.<br>
     * Specifies the latest version of the test class that performed the required setup for the annotated method.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    protected @interface LatestVersion {
        String value();
    }

    private static class UpgradeVersionCheck implements TestRule
    {

        @Override
        public @NotNull Statement apply(Statement base, Description description)
        {
            String eariestVersion = Optional.ofNullable(description.getAnnotation(EariestVersion.class))
                .map(EariestVersion::value).orElse(null);
            String latestVersion = Optional.ofNullable(description.getAnnotation(LatestVersion.class))
                .map(LatestVersion::value).orElse(null);

            if (isUpgradeSetupPhase || previousVersion == null || (eariestVersion == null && latestVersion == null))
            {
                return base; // Run the test normally
            }

            return new Statement()
            {
                @Override
                public void evaluate() throws Throwable
                {
                    Assume.assumeTrue("Test doesn't support upgrading from version: " + previousVersion,
                        VersionRange.versionRange(eariestVersion, latestVersion).contains(previousVersion)
                    );
                    base.evaluate();
                }
            };
        }
    }

}
