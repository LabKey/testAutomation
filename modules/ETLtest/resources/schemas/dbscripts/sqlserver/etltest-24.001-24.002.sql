ALTER PROCEDURE etltest.etlTest
    @transformRunId int,
    @containerId entityid = NULL OUTPUT,
    @rowsInserted int = 0 OUTPUT,
    @rowsDeleted int = 0 OUTPUT,
    @rowsModified int = 0 OUTPUT,
    @returnMsg varchar(100) = 'default message' OUTPUT,
    @debug varchar(1000) = '',
    @filterRunId int = null,
    @filterStartTimeStamp datetime = null OUTPUT,
    @filterEndTimeStamp datetime = null OUTPUT,
    @testMode int,
    @testInOutParam varchar(10) = null OUTPUT,
    @runCount int = 1 OUTPUT,
    @previousFilterRunId int = -1 OUTPUT,
    @previousFilterStartTimeStamp datetime = null OUTPUT,
    @previousFilterEndTimeStamp datetime = null OUTPUT
    AS
BEGIN

/*
	Test modes
	1	normal operation
	2	return code > 0
	3	raise error
	4	input/output parameter persistence
	5	override of persisted input/output parameter
	6	Run filter strategy, require filterRunId. Test persistence.
    7   Modified since filter strategy, no source, require filterStartTimeStamp & filterEndTimeStamp,
		populated from output of previous run
    8	Modified since filter strategy with source, require filterStartTimeStamp & filterEndTimeStamp
		populated from the filter strategy IncrementalStartTime & IncrementalEndTime
    9	Sleep for 2 minutes before finishing
*/

IF @testMode IS NULL
BEGIN
	SET @returnMsg = 'No testMode set'
	RETURN 1
END

IF @runCount IS NULL
    SET @runCount = 1;
ELSE
    SET @runCount = @runCount + 1;

IF @testMode = 1
BEGIN
	print 'Test print statement logging'
	SET @rowsInserted = 1
	SET @rowsDeleted = 2
	SET @rowsModified = 4
	SET @returnMsg = 'Test returnMsg logging'
	RETURN 0
END

IF @testMode = 2 RETURN 1

IF @testMode = 3
BEGIN
	SET @returnMsg = 'Intentional SQL Exception From Inside Proc'
	RAISERROR(@returnMsg, 11, 1)
END

IF @testMode = 4 AND @testInOutParam != 'after' AND @runCount > 1
BEGIN
	SET @returnMsg = 'Expected value "after" for @testInOutParam on run count = ' + convert(varchar, @runCount) + ', but was ' + @testInOutParam
	RETURN 1
END

IF @testMode = 5 AND @testInOutParam != 'before' AND @runCount > 1
BEGIN
	SET @returnMsg = 'Expected value "before" for @testInOutParam on run count = ' + convert(varchar, @runCount) + ', but was ' + @testInOutParam
	RETURN 1
END

IF @testMode = 6
BEGIN
	IF @filterRunId IS NULL
BEGIN
		SET @returnMsg = 'Required @filterRunId value not supplied'
		RETURN 1
END
	IF @runCount > 1 AND (@previousFilterRunId IS NULL OR @previousFilterRunId >= @filterRunId)
BEGIN
		SET @returnMsg = 'Required @filterRunId was not persisted from previous run.'
		RETURN 1
END
	SET @previousFilterRunId = @filterRunId
END

IF @testMode = 7
BEGIN
    IF @runCount > 1 AND (@filterStartTimeStamp IS NULL AND @filterEndTimeStamp IS NULL)
BEGIN
      SET @returnMsg = 'Required filterStartTimeStamp or filterEndTimeStamp were not persisted from previous run.';
RETURN 1;
END;
    SET @filterStartTimeStamp = CURRENT_TIMESTAMP;
    SET @filterEndTimeStamp = CURRENT_TIMESTAMP;
END;

IF @testMode = 8

BEGIN
    IF @runCount > 1 AND ((@previousFilterStartTimeStamp IS NULL AND @previousFilterEndTimeStamp IS NULL)
                         OR (@filterStartTimeStamp IS NULL AND @filterEndTimeStamp IS NULL))
BEGIN
      SET @returnMsg = 'Required filterStartTimeStamp or filterEndTimeStamp were not persisted from previous run.';
RETURN 1;
END;
    SET @previousFilterStartTimeStamp = coalesce(@filterStartTimeStamp, CURRENT_TIMESTAMP);
    SET @previousFilterEndTimeStamp = coalesce(@filterEndTimeStamp, CURRENT_TIMESTAMP);
END;

IF @testMode = 9
BEGIN
    -- Sleep for 30 seconds
    WAITFOR DELAY '00:00:30'
    RETURN 1;
END;

-- set value for persistence tests
IF @testInOutParam IS NOT NULL AND @testInOutParam != '' SET @testInOutParam = 'after'

RETURN 0

END
GO
