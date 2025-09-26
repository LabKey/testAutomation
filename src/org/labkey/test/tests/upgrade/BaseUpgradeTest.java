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

import java.util.Arrays;
import java.util.List;

public abstract class BaseUpgradeTest extends BaseWebDriverTest
{

    protected static final boolean isUpgradeSetupPhase = TestProperties.getBooleanProperty("webtest.upgradeSetup", true);
    protected static final double previousVersion = TestProperties.getDoubleProperty("webtest.upgradePreviousVersion", 0.0);

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

    protected @interface EariestVersion {
        double value();
    }

    protected @interface LatestVersion {
        double value();
    }

    private static class UpgradeVersionCheck implements TestRule
    {

        @Override
        public @NotNull Statement apply(Statement base, Description description)
        {
            EariestVersion eariestVersion = description.getAnnotation(EariestVersion.class);
            LatestVersion latestVersion = description.getAnnotation(LatestVersion.class);
            if (isUpgradeSetupPhase || previousVersion <= 0)
            {
                return base; // Run the test normally
            }

            return new Statement()
            {
                @Override
                public void evaluate() throws Throwable
                {
                    Assume.assumeTrue("Test doesn't support upgrading from version: " + previousVersion,
                        (eariestVersion == null || eariestVersion.value() <= previousVersion) &&
                            (latestVersion == null || previousVersion <= latestVersion.value())
                    );
                    base.evaluate();
                }
            };
        }
    }

}
