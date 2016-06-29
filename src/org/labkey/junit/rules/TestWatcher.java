package org.labkey.junit.rules;

import java.util.ArrayList;
import java.util.List;

import org.junit.AssumptionViolatedException;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;

/**
 * Based off of {@link org.junit.rules.TestWatcher}
 * Modified to fail faster if {@link #starting(Description)} throws
 */
public abstract class TestWatcher implements TestRule
{
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                List<Throwable> errors = new ArrayList<Throwable>();

                try {
                    starting(description);
                    base.evaluate();
                    succeededQuietly(description, errors);
                } catch (@SuppressWarnings("deprecation") org.junit.internal.AssumptionViolatedException  e) {
                    errors.add(e);
                    skippedQuietly(e, description, errors);
                } catch (Throwable e) {
                    errors.add(e);
                    failedQuietly(e, description, errors);
                } finally {
                    finishedQuietly(description, errors);
                }

                MultipleFailureException.assertEmpty(errors);
            }
        };
    }

    private void succeededQuietly(Description description,
                                  List<Throwable> errors) {
        try {
            succeeded(description);
        } catch (Throwable e) {
            errors.add(e);
        }
    }

    private void failedQuietly(Throwable e, Description description,
                               List<Throwable> errors) {
        try {
            failed(e, description);
        } catch (Throwable e1) {
            errors.add(e1);
        }
    }

    @SuppressWarnings("deprecation")
    private void skippedQuietly(
            org.junit.internal.AssumptionViolatedException e, Description description,
            List<Throwable> errors) {
        try {
            if (e instanceof AssumptionViolatedException) {
                skipped((AssumptionViolatedException) e, description);
            } else {
                skipped(e, description);
            }
        } catch (Throwable e1) {
            errors.add(e1);
        }
    }

    private void finishedQuietly(Description description,
                                 List<Throwable> errors) {
        try {
            finished(description);
        } catch (Throwable e) {
            errors.add(e);
        }
    }

    /**
     * Invoked when a test succeeds
     */
    protected void succeeded(Description description) {
    }

    /**
     * Invoked when a test fails
     */
    protected void failed(Throwable e, Description description) {
    }

    /**
     * Invoked when a test is skipped due to a failed assumption.
     */
    @SuppressWarnings("deprecation")
    protected void skipped(AssumptionViolatedException e, Description description) {
        // For backwards compatibility with JUnit 4.11 and earlier, call the legacy version
        org.junit.internal.AssumptionViolatedException asInternalException = e;
        skipped(asInternalException, description);
    }

    /**
     * Invoked when a test is skipped due to a failed assumption.
     *
     * @deprecated use {@link #skipped(AssumptionViolatedException, Description)}
     */
    @Deprecated
    protected void skipped(
            org.junit.internal.AssumptionViolatedException e, Description description) {
    }

    /**
     * Invoked when a test is about to start
     */
    protected void starting(Description description) {
    }

    /**
     * Invoked when a test method finishes (whether passing or failing)
     */
    protected void finished(Description description) {
    }
}

