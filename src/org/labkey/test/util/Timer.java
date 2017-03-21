package org.labkey.test.util;

import java.time.Duration;
import java.time.Instant;

public class Timer
{
    private final Instant _startTime;
    private final Duration _timeout;

    public Timer(Duration timeout)
    {
        _timeout = timeout;
        _startTime = Instant.now();
    }

    public Duration elapsed()
    {
        return Duration.between(_startTime, Instant.now());
    }

    public Duration timeRemaining()
    {
        return _timeout.minus(elapsed());
    }

    public boolean isTimedOut()
    {
        return timeRemaining().isNegative();
    }
}
