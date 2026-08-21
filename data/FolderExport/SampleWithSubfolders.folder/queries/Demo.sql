/*
 * Copyright (c) 2020-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT Demographics.ParticipantId,
Demographics.date,
Demographics.StartDate,
Demographics.Height,
Demographics.Gender,
Demographics.Country,
Demographics."Group",
Demographics.Status,
Demographics.Comments
FROM Demographics
