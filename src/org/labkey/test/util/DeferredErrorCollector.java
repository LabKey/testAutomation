package org.labkey.test.util;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.labkey.test.BaseWebDriverTest;

import java.util.ArrayList;
import java.util.List;

/**
 * Allows tests to record non-fatal errors without failing the test immediately.
 * Once all such checks have been made (likely at the end of a test), you should invoke {@link #recordResults()}, which
 * will throw an {@link AssertionError} if any errors were recorded.
 */
public class DeferredErrorCollector
{
    private int screenShotCount = 0;
    private int errorMark = 0;
    private final ArtifactCollector artifactCollector;
    private final List<String> allErrorMessages = new ArrayList<>();

    /**
     * @param artifactCollector An {@link ArtifactCollector} to be used for screenshots
     */
    public DeferredErrorCollector(ArtifactCollector artifactCollector)
    {
        this.artifactCollector = artifactCollector;
    }

    /**
     * @param test A {@link BaseWebDriverTest} from which will provide an {@link ArtifactCollector}
     */
    public DeferredErrorCollector(@NotNull BaseWebDriverTest test)
    {
        this(test.getArtifactCollector());
    }

    /**
     * Create temporary error collector that allows `AssertionError`s to propagate normally.
     * @return Wrapped error collector. Intended to be used only once.
     */
    public DeferredErrorCollector fatal()
    {
        return new FatalErrorCollector(this);
    }

    /**
     * Create temporary error collector that takes a screenshot for any recorded errors.
     * @param screenshotName A string to identify screenshots; Will be included in screenshot filenames.
     * @return Wrapped error collector. Intended to be used only once.
     */
    public DeferredErrorCollector withScreenshot(@NotNull String screenshotName)
    {
        return new DeferredErrorCollectorWithScreenshot(this, screenshotName);
    }

    /**
     * Check if any errors have been recorded.
     * @return Return true if an error has been recorded.
     */
    public boolean hasErrorBeenRecorded()
    {
        return !allErrorMessages.isEmpty();
    }

    /**
     * Get the count of recorded errors.
     * @return Number of times errors have been recorded.
     */
    public int getErrorCount()
    {
        return allErrorMessages.size();
    }

    /**
     * Remember current error count. Used by {@link #errorsSinceMark()}
     */
    public void setErrorMark()
    {
        errorMark = getErrorCount();
    }

    /**
     * Check the number of errors that have been recorded since last time {@link #setErrorMark()} was called.
     * @return Current error count minus the error count set by {@link #setErrorMark()}.
     */
    public int errorsSinceMark()
    {
        return getErrorCount() - errorMark;
    }

    /**
     * Record any {@link AssertionError}s thrown by the provided {@link Runnable}.
     * @param wrappedAssertion {@link Runnable} that might throw an {@link AssertionError}
     */
    public void wrapAssertion(Runnable wrappedAssertion)
    {
        try
        {
            wrappedAssertion.run();
        }
        catch (AssertionError err)
        {
            recordError(err.getMessage());
        }
    }

    /**
     * Record an error if the two objects are not equal.
     * Wraps {@link Assert#assertEquals(String, Object, Object)}
     * @param message Message to show if check fails.
     * @param expected Expected value.
     * @param actual Actual value.
     */
    public void verifyEqual(String message, Object expected, Object actual)
    {
        wrapAssertion(() -> Assert.assertEquals(message, expected, actual));
    }

    /**
     * Record an error if the two objects are equal.
     * Wraps {@link Assert#assertNotEquals(String, Object, Object)}
     * @param message Message to show if check fails.
     * @param unexpected Unexpected value.
     * @param actual Actual value.
     */
    public void verifyNotEqual(String message, Object unexpected, Object actual)
    {
        wrapAssertion(() -> Assert.assertNotEquals(message, unexpected, actual));
    }

    /**
     * Record an error if the condition is 'false'.
     * Wraps {@link Assert#assertTrue(String, boolean)}
     * @param message Message to show if check fails.
     * @param condition Conditional check (ex: element.isDisplayed())
     */
    public void verifyTrue(String message, boolean condition)
    {
        wrapAssertion(() -> Assert.assertTrue(message, condition));
    }

    /**
     * Record an error if the condition is 'true'.
     * Wraps {@link Assert#assertFalse(String, boolean)}
     * @param message Message to show if the conditional test is false.
     * @param condition Conditional check (ex: element.isDisplayed())
     */
    public void verifyFalse(String message, boolean condition)
    {
        wrapAssertion(() -> Assert.assertFalse(message, condition));
    }

    /**
     * Record an error if the object is 'null'
     * Wraps {@link Assert#assertNull(String, Object)}
     * @param message Message to show if check fails.
     * @param object Object to check
     */
    public void verifyNull(String message, Object object)
    {
        wrapAssertion(() -> Assert.assertNull(message, object));
    }

    /**
     * Record an error message.
     * Wraps {@link Assert#fail()}
     * @param message Message to record.
     */
    public void error(String message)
    {
        wrapAssertion(() -> Assert.fail(message));
    }

    /**
     * Log an error message, take a screen shot and record the call stack.
     * @param errorMessage Message to log.
     */
    protected void recordError(String errorMessage)
    {
        StringBuilder messageForLog = new StringBuilder();
        StringBuilder messageForFailure = new StringBuilder();

        StackTraceElement[] cause = Thread.currentThread().getStackTrace();

        messageForLog.append("\n*******************************\n");
        messageForLog.append("\n");
        messageForLog.append(errorMessage);
        messageForLog.append("\n");

        // Don't repeat everything in the list of all error messages, only some parts of this detailed error.
        messageForFailure.append(errorMessage);

        StringBuilder deepCallStack = new StringBuilder(); // Keep deep stack for possible DEBUG logging in the future
        StringBuilder shallowCallStack = new StringBuilder();
        int shallowStackDepth = 0;

        deepCallStack.append("\n");
        shallowCallStack.append("\n");

        for(StackTraceElement ste : cause)
        {
            deepCallStack.append(ste);
            deepCallStack.append("\n");

            if(ste.getClassName().toLowerCase().contains("org.labkey.test") && 
                    !ste.getClassName().startsWith(this.getClass().getName()) &&
                    shallowStackDepth < 6)
            {
                shallowCallStack.append("\t");
                shallowCallStack.append(ste);
                shallowCallStack.append("\n");
                shallowStackDepth++;
            }
        }
        
        // Record the call stack.
        messageForLog.append(shallowCallStack);
        messageForFailure.append(shallowCallStack);
        
        messageForLog.append("\n*******************************\n");
        
        allErrorMessages.add(messageForFailure.toString());

        TestLogger.log(messageForLog.toString());
    }

    /**
     * Throws an {@link AssertionError} if any errors have been recorded. Should be called when done performing checks.
     */
    public void recordResults()
    {
        if(hasErrorBeenRecorded())
            Assert.fail(getFailureMessage());
    }

    /**
     * Get the 'roll-up' of the errors messages that have been recorded.
     * @return String of all of the errors that have been recorded.
     */
    private String getFailureMessage()
    {
        final String separator = "\n" + StringUtils.repeat("=", 42) + "\n";
        StringBuilder failureMessage = new StringBuilder();
        if (allErrorMessages.size() > 1)
        {
            failureMessage
                    .append("Detected ")
                    .append(allErrorMessages.size())
                    .append(" errors during test.")
                    .append(separator);
        }
        for (int i = 0; i < allErrorMessages.size(); i++)
        {
            if (allErrorMessages.size() > 1)
            {
                failureMessage
                        .append("[")
                        .append(i + 1)
                        .append("/")
                        .append(allErrorMessages.size())
                        .append("] ");
            }
            failureMessage.append(allErrorMessages.get(i).trim());
            failureMessage.append(separator);
        }
        return failureMessage.toString();
    }

    /**
     * Take a screen shot and HTML dump of the current page.
     * See {@link ArtifactCollector#dumpPageSnapshot(String, String)} for details.
     * @param snapShotName A string to identify screenshots; Will be included in screenshot filenames.
     * @return The name of the file used. Basically the snapShotName parameter with a counter added to the end.
     */
    public String takeScreenShot(String snapShotName)
    {
        String _snapShotNumberedName = snapShotName + "_" + screenShotCount++;

        artifactCollector.dumpPageSnapshot(_snapShotNumberedName, null);

        return _snapShotNumberedName;
    }

    private static abstract class DeferredErrorCollectorWrapper extends DeferredErrorCollector
    {
        private final DeferredErrorCollector wrappedErrorColloctor;

        protected DeferredErrorCollectorWrapper(DeferredErrorCollector wrappedErrorColloctor)
        {
            super((ArtifactCollector) null);
            this.wrappedErrorColloctor = wrappedErrorColloctor;
        }

        protected DeferredErrorCollector getWrappedErrorColloctor()
        {
            return wrappedErrorColloctor;
        }

        @Override
        public DeferredErrorCollector fatal()
        {
            throw new IllegalArgumentException(); // Don't allow double-wrapping
        }

        @Override
        public DeferredErrorCollector withScreenshot(@NotNull String screenshotName)
        {
            throw new IllegalArgumentException(); // Don't allow double-wrapping
        }

        @Override
        public boolean hasErrorBeenRecorded()
        {
            return getWrappedErrorColloctor().hasErrorBeenRecorded();
        }

        @Override
        public int getErrorCount()
        {
            return getWrappedErrorColloctor().getErrorCount();
        }

        @Override
        public String takeScreenShot(String snapShotName)
        {
            return getWrappedErrorColloctor().takeScreenShot(snapShotName);
        }

        @Override
        protected void recordError(String errorMessage)
        {
            getWrappedErrorColloctor().recordError(errorMessage);
        }

        @Override
        public void recordResults()
        {
            getWrappedErrorColloctor().recordResults();
        }
    }

    private static class FatalErrorCollector extends DeferredErrorCollectorWrapper
    {
        public FatalErrorCollector(DeferredErrorCollector wrappedErrorColloctor)
        {
            super(wrappedErrorColloctor);
        }

        @Override
        public void wrapAssertion(Runnable wrappedAssertion)
        {
            wrappedAssertion.run();
        }
    }

    private static class DeferredErrorCollectorWithScreenshot extends DeferredErrorCollectorWrapper
    {
        private final String screenshotName;

        public DeferredErrorCollectorWithScreenshot(DeferredErrorCollector wrappedErrorColloctor, String screenshotName)
        {
            super(wrappedErrorColloctor);
            this.screenshotName = screenshotName;
        }

        @Override
        protected void recordError(String errorMessage)
        {
            String shotName = takeScreenShot(this.screenshotName);
            errorMessage = errorMessage + "\nScreen shot name: " + shotName + "\n";
            super.recordError(errorMessage);
        }
    }
}
