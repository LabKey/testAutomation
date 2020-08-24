package org.labkey.test.util;

import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Matcher;
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
    private final List<Class<? extends Throwable>> errorTypes = new ArrayList<>();

    private FatalErrorCollector _fatalErrorCollector;

    /**
     * @param artifactCollector An {@link ArtifactCollector} to be used for screenshots
     */
    public DeferredErrorCollector(ArtifactCollector artifactCollector)
    {
        this.artifactCollector = artifactCollector;
        resetErrorTypes();
    }

    /**
     * @param test A {@link BaseWebDriverTest} from which will provide an {@link ArtifactCollector}
     */
    public DeferredErrorCollector(@NotNull BaseWebDriverTest test)
    {
        this(test.getArtifactCollector());
    }

    /**
     * Reset this error checker to only record {@link AssertionError}s
     */
    public void resetErrorTypes()
    {
        errorTypes.clear();
        errorTypes.add(AssertionError.class);
    }

    /**
     * Add an error type that should be recorded when caught by {@link #wrapAssertion(Runnable)}. May be called multiple
     * times to add more error types.
     * By default, {@link AssertionError} is the only error type recorded.
     * Reset instance to default error handling with {@link #resetErrorTypes()}
     *
     * @param errorType Additional error type to be recorded.
     */
    public void addRecordableErrorType(Class<? extends Exception> errorType)
    {
        errorTypes.add(errorType);
    }

    /**
     * Create temporary error collector that allows `AssertionError`s to propagate normally.
     *
     * @return Wrapped error collector. Intended to be used only once.
     */
    public DeferredErrorCollector fatal()
    {
        if (_fatalErrorCollector == null)
        {
            _fatalErrorCollector = new FatalErrorCollector();
        }
        return _fatalErrorCollector;
    }

    /**
     * Create temporary error collector that takes a screenshot for any recorded errors.
     *
     * @param screenshotName A string to identify screenshots; Will be included in screenshot filenames.
     * @return Wrapped error collector. Intended to be used only once.
     */
    public DeferredErrorCollector withScreenshot(@NotNull String screenshotName)
    {
        return new DeferredErrorCollectorWithScreenshot(screenshotName);
    }

    /**
     * Check if any errors have been recorded.
     *
     * @return Return true if an error has been recorded.
     */
    public boolean hasErrorBeenRecorded()
    {
        return !allErrorMessages.isEmpty();
    }

    /**
     * Get the count of recorded errors.
     *
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
     * Check the number of errors that have been recorded since the last time {@link #setErrorMark()} was called.
     *
     * @return Current error count minus the error count set by {@link #setErrorMark()}.
     */
    public int errorsSinceMark()
    {
        return getErrorCount() - errorMark;
    }

    /**
     * Take a screenshot if any errors have been recorded since the last time {@link #setErrorMark()} was called.
     * Then resets the error mark.
     *
     * @param screenshotName A string to identify screenshots; Will be included in screenshot filenames.
     * @see #takeScreenShot(String)
     */
    public void screenShotIfNewError(String screenshotName)
    {
        if (errorsSinceMark() > 0)
            takeScreenShot(screenshotName);
        setErrorMark();
    }

    /**
     * Record any errors thrown by the provided {@link Runnable} if they match any of the specified {@link Throwable}s.
     * Non-matching Throwables will be rethrown. By default, only {@link AssertionError}s will be recorded. Call
     * {@link #addRecordableErrorType(Class)} to specify additional error types that should be recorded.
     *
     * @param wrappedAssertion {@link Runnable} that might throw an error that shouldn't fail a test immediately
     * @return <code>true</code> if <code>wrappedAssertion</code> does not throw, <code>false</code> if an error was recorded,
     * otherwise rethrow exception thrown by <code>wrappedAssertion</code>
     */
    public boolean wrapAssertion(Runnable wrappedAssertion)
    {
        try
        {
            wrappedAssertion.run();
            return true;
        }
        catch (Exception | AssertionError err)
        {
            if (isErrorRecordable(err))
            {
                recordError(err.getMessage());
                return false;
            }
            throw err; // Not a recordable error
        }
    }

    protected boolean isErrorRecordable(Throwable err)
    {
        for (Class<? extends Throwable> errorType : errorTypes)
        {
            if (errorType.isInstance(err))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Record an error if the two objects are not equal.
     *
     * @param message Message to show if check fails.
     * @param expected Expected value.
     * @param actual Actual value.
     * @see Assert#assertEquals(String, Object, Object)
     * @return <code>true</code> if objects are equal
     */
    public final boolean verifyEquals(String message, Object expected, Object actual)
    {
        return wrapAssertion(() -> Assert.assertEquals(message, expected, actual));
    }

    /**
     * Record an error if the two objects are equal.
     *
     * @param message Message to show if check fails.
     * @param unexpected Unexpected value.
     * @param actual Actual value.
     * @see Assert#assertNotEquals(String, Object, Object)
     * @return <code>true</code> if objects are not equal
     */
    public final boolean verifyNotEquals(String message, Object unexpected, Object actual)
    {
        return wrapAssertion(() -> Assert.assertNotEquals(message, unexpected, actual));
    }

    /**
     * Record an error if the condition is <code>false</code>.
     *
     * @param message Message to show if check fails.
     * @param condition Conditional check (ex: element.isDisplayed())
     * @see Assert#assertTrue(String, boolean)
     * @return <code>true</code> if condition is true
     */
    public final boolean verifyTrue(String message, boolean condition)
    {
        return wrapAssertion(() -> Assert.assertTrue(message, condition));
    }

    /**
     * Record an error if the condition is <code>true</code>.
     *
     * @param message Message to show if the conditional test is false.
     * @param condition Conditional check (ex: element.isDisplayed())
     * @see Assert#assertFalse(String, boolean)
     * @return <code>true</code> if condition is false
     */
    public final boolean verifyFalse(String message, boolean condition)
    {
        return wrapAssertion(() -> Assert.assertFalse(message, condition));
    }

    /**
     * Record an error if the object is not <code>null</code>.
     *
     * @param message Message to show if check fails.
     * @param object Object to check
     * @see Assert#assertNull(String, Object)
     * @return <code>true</code> if object is null
     */
    public final boolean verifyNull(String message, Object object)
    {
        return wrapAssertion(() -> Assert.assertNull(message, object));
    }

    /**
     * Record an error if the object is <code>null</code>.
     *
     * @param message Message to show if check fails.
     * @param object Object to check
     * @see Assert#assertNotNull(String, Object)
     * @return <code>true</code> if object is not null
     */
    public final boolean verifyNotNull(String message, Object object)
    {
        return wrapAssertion(() -> Assert.assertNotNull(message, object));
    }

    /**
     * Record an error if the object doesn't satisfy the specified condition.
     *
     * @param reason additional information about the error
     * @param actual the computed value being compared
     * @param matcher an expression, built of {@link Matcher}s, specifying allowed
     * values
     * @see Assert#assertThat(String, Object, Matcher)
     * @return <code>true</code> if the object satisfies the specified condition
     */
    public final <T> boolean verifyThat(String reason, T actual, Matcher<? super T> matcher)
    {
        return wrapAssertion(() -> Assert.assertThat(reason, actual, matcher));
    }

    /**
     * Record an error message.
     *
     * @param message Message to record.
     * @see Assert#fail()
     * @return Always returns <code>false</code>
     */
    public final boolean error(String message)
    {
        return wrapAssertion(() -> Assert.fail(message));
    }

    /**
     * Log an error message, take a screen shot and record the call stack.
     *
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
     *
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
     *
     * @param screenshotName A string to identify screenshots; Will be included in screenshot filenames.
     * @return The name of the file used. Basically the screenshotName parameter with a counter added to the end.
     */
    public String takeScreenShot(String screenshotName)
    {
        String snapShotNumberedName = screenshotName + "_" + screenShotCount++;

        artifactCollector.dumpPageSnapshot(snapShotNumberedName, null);

        return snapShotNumberedName;
    }

    private abstract class DeferredErrorCollectorWrapper extends DeferredErrorCollector
    {
        protected DeferredErrorCollectorWrapper()
        {
            super((ArtifactCollector) null);
        }

        protected DeferredErrorCollector getWrappedErrorColloctor()
        {
            return DeferredErrorCollector.this;
        }

        @Override
        public final DeferredErrorCollector fatal()
        {
            return getWrappedErrorColloctor().fatal();
        }

        @Override
        public DeferredErrorCollector withScreenshot(@NotNull String screenshotName)
        {
            return new DeferredErrorCollectorWithScreenshot(screenshotName);
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
        public String takeScreenShot(String screenshotName)
        {
            return getWrappedErrorColloctor().takeScreenShot(screenshotName);
        }

        @Override
        public void resetErrorTypes()
        {
            getWrappedErrorColloctor().resetErrorTypes();
        }

        @Override
        public void addRecordableErrorType(Class<? extends Exception> errorType)
        {
            getWrappedErrorColloctor().addRecordableErrorType(errorType);
        }

        @Override
        public void setErrorMark()
        {
            getWrappedErrorColloctor().setErrorMark();
        }

        @Override
        public int errorsSinceMark()
        {
            return getWrappedErrorColloctor().errorsSinceMark();
        }

        @Override
        public void screenShotIfNewError(String screenshotName)
        {
            getWrappedErrorColloctor().screenShotIfNewError(screenshotName);
        }

        @Override
        protected boolean isErrorRecordable(Throwable err)
        {
            return getWrappedErrorColloctor().isErrorRecordable(err);
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

    private class FatalErrorCollector extends DeferredErrorCollectorWrapper
    {
        protected FatalErrorCollector()
        {
            super();
        }

        @Override
        public boolean wrapAssertion(Runnable wrappedAssertion)
        {
            wrappedAssertion.run();
            return true;
        }
    }

    private class DeferredErrorCollectorWithScreenshot extends DeferredErrorCollectorWrapper
    {
        private final String _screenshotName;

        protected DeferredErrorCollectorWithScreenshot(String screenshotName)
        {
            _screenshotName = screenshotName;
        }

        @Override
        protected void recordError(String errorMessage)
        {
            String shotName = super.takeScreenShot(_screenshotName);
            errorMessage = errorMessage + "\nScreen shot name: " + shotName + "\n";
            super.recordError(errorMessage);
        }
    }
}
