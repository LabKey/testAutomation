package org.labkey.test.util;

import org.apache.commons.lang3.StringUtils;
import org.awaitility.core.ConditionTimeoutException;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.labkey.junit.LabKeyAssert;
import org.labkey.test.BaseWebDriverTest;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.awaitility.Awaitility.await;

/**
 * Allows tests to record non-fatal errors without failing the test immediately.
 * Once all such checks have been made (likely at the end of a test), you should invoke {@link #reportResults()}, which
 * will throw an {@link AssertionError} if any errors were recorded.
 */
public class DeferredErrorCollector
{
    private static final Set<Class<? extends Throwable>> defaultRecordableErrorTypes =
            Set.of(AssertionError.class, ConditionTimeoutException.class);

    private final ArtifactCollector artifactCollector;
    private final List<DeferredError> allErrors = new ArrayList<>();
    protected final Set<Class<? extends Throwable>> errorTypes = new HashSet<>();

    private int errorMark = 0;

    /**
     * For wrapper classes. Avoids resetting error types in wrapped collector.
     */
    protected DeferredErrorCollector()
    {
        artifactCollector = null;
    }

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
        errorTypes.addAll(defaultRecordableErrorTypes);
    }

    /**
     * Add an error type that should be recorded when caught by {@link #wrapAssertion(Runnable)}. May be called multiple
     * times to add more error types.
     * By default, {@link AssertionError} is the only error type recorded.
     * Reset instance to default error handling with {@link #resetErrorTypes()}
     *
     * @param errorType Additional error type to be recorded.
     */
    public void addRecordableErrorType(Class<? extends Throwable> errorType)
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
        return new DeferredErrorCollectorWrapper(this).fatal();
    }

    /**
     * Create temporary error collector that takes a screenshot for any recorded errors.
     *
     * @param screenshotName A string to identify screenshots; Will be included in screenshot filenames.
     * @return Wrapped error collector. Intended to be used only once.
     */
    public DeferredErrorCollector withScreenshot(@NotNull String screenshotName)
    {
        return new DeferredErrorCollectorWrapper(this).withScreenshot(screenshotName);
    }

    /**
     * Create temporary error collector that takes a screenshot for any recorded errors.
     *
     * @return Wrapped error collector. Intended to be used only once.
     */
    public final DeferredErrorCollector withScreenshot()
    {
        return withScreenshot("recordedError");
    }

    /**
     * Check if any errors have been recorded.
     *
     * @return Return true if an error has been recorded.
     */
    public boolean hasErrorBeenRecorded()
    {
        return !allErrors.isEmpty();
    }

    /**
     * Get the count of recorded errors.
     *
     * @return Number of times errors have been recorded.
     */
    public int getErrorCount()
    {
        return allErrors.size();
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
     * @return True if a screenshot was taken, false otherwise.
     * @see #takeScreenShot(String)
     */
    public boolean screenShotIfNewError(@NotNull String screenshotName)
    {
        if (errorsSinceMark() > 0)
        {
            final String s = artifactCollector.dumpPageSnapshot(screenshotName);
            allErrors.get(allErrors.size() - 1).setScreenshotName(s);
            artifactCollector.reportTestMetadata(s);
            setErrorMark();
            return true;
        }
        return false;
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
    public final boolean wrapAssertion(Runnable wrappedAssertion)
    {
        try
        {
            wrappedAssertion.run();
            return true;
        }
        catch (Throwable err)
        {
            if (isErrorDeferrable(err))
            {
                recordError(err);
                return false;
            }
            throw err; // Not a recordable error
        }
    }

    protected boolean isErrorDeferrable(Throwable err)
    {
        for (Class<? extends Throwable> errorType : errorTypes)
        {
            if (errorType.isAssignableFrom(err.getClass()))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Record an error if the provided assertion doesn't succeed within the specified timeout.
     *
     * @param timeout maximum duration to wait for assertion
     * @param wrappedAssertion {@link Runnable} that might throw an error that shouldn't fail a test immediately
     * @return <code>true</code> if the assertion succeeded within the specified timeout
     */
    public boolean awaiting(Duration timeout, Runnable wrappedAssertion)
    {
        return wrapAssertion(() ->
                await().atMost(timeout).ignoreExceptionsMatching(this::isErrorDeferrable)
                        .untilAsserted(wrappedAssertion::run));
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
     * Sort the collections before checking for equal. If not equal record an error message.
     *
     * @param message Message to show if check fails.
     * @param expected The expected collection of objects.
     * @param actual Actual collection of objects.
     * @see LabKeyAssert#assertEqualsSorted(String, Collection, Collection)
     * @return <code>true</code> if sorted collections are not equal
     */
    public final <T> boolean verifyEqualsSorted(String message, Collection<T> expected, Collection<T> actual)
    {
        return wrapAssertion(() -> LabKeyAssert.assertEqualsSorted(message, expected, actual));
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
     * @see MatcherAssert#assertThat(String, Object, Matcher)
     * @return <code>true</code> if the object satisfies the specified condition
     */
    public final <T> boolean verifyThat(String reason, T actual, Matcher<? super T> matcher)
    {
        return wrapAssertion(() -> MatcherAssert.assertThat(reason, actual, matcher));
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
     * Record an error, take a screen shot if requested, and records the call stack.
     * Allows subclasses to customize the error message.
     *
     * @param error Throwable to record
     */
    public void recordError(Throwable error)
    {
        final DeferredError deferredError = new DeferredError(error);
        allErrors.add(deferredError);

        TestLogger.error("\n*******************************\n" + error.getMessage() + "\n*******************************\n", error);
    }

    /**
     * Throws an {@link AssertionError} if any errors have been recorded. Should be called when done performing checks.
     */
    public void reportResults()
    {
        if (hasErrorBeenRecorded())
        {
            if (allErrors.get(allErrors.size() - 1).getScreenshotName() == null)
            {
                withScreenshot("fallback").error("No screeshot taken for last deferred error(s). " +
                        "This screenshot may be relevant to previous failures. " +
                        "Please update test to take appropriate screenshots.");
            }
            throw new DeferredAssertionError(getFailureMessage());
        }
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
        if (allErrors.size() > 1)
        {
            failureMessage
                    .append("Detected ")
                    .append(allErrors.size())
                    .append(" errors during test.")
                    .append(separator);
        }
        for (int i = 0; i < allErrors.size(); i++)
        {
            if (allErrors.size() > 1)
            {
                failureMessage
                        .append("[")
                        .append(i + 1)
                        .append("/")
                        .append(allErrors.size())
                        .append("] ");
            }
            failureMessage.append(allErrors.get(i).getErrorMessage().trim());
            failureMessage.append(separator);
        }
        return failureMessage.toString();
    }

    /**
     * Take a screen shot and HTML dump of the current page.
     * @deprecated Use {@link ArtifactCollector#dumpPageSnapshot(String)}
     *
     * @param screenshotName A string to identify screenshots; Will be included in screenshot filenames.
     * @return The name of the file used. Basically the screenshotName parameter with a counter added to the end.
     */
    @Deprecated (since = "22.2")
    public String takeScreenShot(@NotNull String screenshotName)
    {
        return artifactCollector.dumpPageSnapshot(screenshotName);
    }

    public static class DeferredAssertionError extends AssertionError
    {
        private DeferredAssertionError(Object detailMessage)
        {
            super(detailMessage);
        }
    }
}

/**
 * Wraps an existing error collector. Any errors will be recorded within the wrapped collector.
 */
class DeferredErrorCollectorWrapper extends DeferredErrorCollector
{
    private final DeferredErrorCollector wrappedCollector;

    private boolean fatal = false;
    private String screenshotName = null;

    protected DeferredErrorCollectorWrapper(DeferredErrorCollector collector)
    {
        wrappedCollector = collector;
    }

    @Override
    public DeferredErrorCollector fatal()
    {
        fatal = true;
        return this;
    }

    @Override
    public DeferredErrorCollector withScreenshot(@NotNull String screenshotName)
    {
        this.screenshotName = screenshotName;
        return this;
    }

    @Override
    public boolean hasErrorBeenRecorded()
    {
        return wrappedCollector.hasErrorBeenRecorded();
    }

    @Override
    public int getErrorCount()
    {
        return wrappedCollector.getErrorCount();
    }

    @Override
    public String takeScreenShot(@NotNull String screenshotName)
    {
        return wrappedCollector.takeScreenShot(screenshotName);
    }

    @Override
    public void resetErrorTypes()
    {
        wrappedCollector.resetErrorTypes();
    }

    @Override
    public void addRecordableErrorType(Class<? extends Throwable> errorType)
    {
        wrappedCollector.addRecordableErrorType(errorType);
    }

    @Override
    public void setErrorMark()
    {
        wrappedCollector.setErrorMark();
    }

    @Override
    public int errorsSinceMark()
    {
        return wrappedCollector.errorsSinceMark();
    }

    @Override
    public boolean screenShotIfNewError(@NotNull String screenshotName)
    {
        return wrappedCollector.screenShotIfNewError(screenshotName);
    }

    @Override
    protected boolean isErrorDeferrable(Throwable err)
    {
        return !fatal && wrappedCollector.isErrorDeferrable(err);
    }

    @Override
    public void recordError(Throwable error)
    {
        wrappedCollector.recordError(error);
        if (screenshotName != null)
        {
            screenShotIfNewError(screenshotName);
        }
    }

    @Override
    public void reportResults()
    {
        wrappedCollector.reportResults();
    }
}

class DeferredError
{
    private final Throwable error;
    private String screenshotName = null;

    public DeferredError(Throwable error)
    {
        this.error = error;
    }

    public String getErrorMessage()
    {
        StringBuilder messageForFailure = new StringBuilder();

        StackTraceElement[] cause = error.getStackTrace();

        // Don't repeat everything in the list of all error messages, only some parts of this detailed error.
        messageForFailure.append(error.getMessage());

        // Add screenshot info if present
        if (screenshotName != null)
        {
            messageForFailure
                    .append("\nScreen shot name: ")
                    .append(screenshotName)
                    .append("\n");
        }

        StringBuilder shallowCallStack = new StringBuilder();
        int shallowStackDepth = 0;

        shallowCallStack.append("\n");

        // Filter out library methods from stack trace
        for(StackTraceElement ste : cause)
        {
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
        messageForFailure.append(shallowCallStack);

        return messageForFailure.toString();
    }

    public String getScreenshotName()
    {
        return screenshotName;
    }

    public void setScreenshotName(String screenshotName)
    {
        this.screenshotName = screenshotName;
    }
}
