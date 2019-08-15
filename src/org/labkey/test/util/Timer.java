/*
 * Copyright (c) 2017-2019 LabKey Corporation
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

import java.time.Duration;
import java.time.Instant;

public class Timer
{
    private final Instant _startTime;
    private final Duration _timeout;
    private boolean _cancelled = false;

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

    public void cancel()
    {
        _cancelled = true;
    }

    public boolean isTimedOut()
    {
        return _cancelled || timeRemaining().isNegative();
    }
}
