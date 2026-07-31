/*
 * Copyright (c) 2017-2026 LabKey Corporation
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

import org.apache.commons.lang3.time.StopWatch;
import org.jetbrains.annotations.Contract;
import org.openqa.selenium.TimeoutException;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.function.Supplier;

import static org.labkey.test.WebDriverWrapper.sleep;

public class Timer
{
    private final StopWatch _stopWatch;
    private final Duration _timeout;
    private boolean _cancelled = false;

    public Timer(Duration timeout)
    {
        _timeout = timeout;
        _stopWatch = StopWatch.createStarted();
    }

    public Timer()
    {
        this(Duration.ZERO);
    }

    public LocalDateTime getStartTime()
    {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(_stopWatch.getStartTime()), ZoneId.systemDefault());
    }

    public Duration elapsed()
    {
        return Duration.ofMillis(_stopWatch.getTime());
    }

    public Duration timeRemaining()
    {
        return _timeout.minus(elapsed());
    }

    public void stop()
    {
        if (!_stopWatch.isStopped())
        {
            _stopWatch.stop();
            cancel();
        }
    }

    public void cancel()
    {
        _cancelled = true;
    }

    public boolean isTimedOut()
    {
        return _cancelled || timeRemaining().isNegative();
    }

    /**
     * Wait for Supplier to return non-null non-false value. Supplier is guaranteed to run at least once.
     * @return final result of Supplier.get()
     */
    @Contract(pure = true)
    public <T> T waitFor(Supplier<T> checker)
    {
        T result;
        do
        {
            result = checker.get();
            if (isSuccessResult(result))
                break;
            sleep(100);
        } while (!isTimedOut());

        return result;
    }

    public <T> T waitFor(Supplier<T> checker, Supplier<String> failMessageSupplier)
    {
        T result = waitFor(checker);
        if (!isSuccessResult(result))
        {
            throw new TimeoutException(failMessageSupplier.get() + TestLogger.formatElapsedTime(_timeout.toMillis()));
        }
        return result;
    }

    public <T> T waitFor(Supplier<T> checker, String failMessage)
    {
        return waitFor(checker, () -> failMessage);
    }

    private static <T> boolean isSuccessResult(T result)
    {
        return result != null && !Boolean.FALSE.equals(result);
    }
}
