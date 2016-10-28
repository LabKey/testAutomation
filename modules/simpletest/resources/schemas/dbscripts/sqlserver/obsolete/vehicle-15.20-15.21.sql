/*
 * Copyright (c) 2013-2016 LabKey Corporation
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
EXEC core.fn_dropifexists 'etlTest', 'vehicle', 'PROCEDURE', NULL;
GO
CREATE PROCEDURE vehicle.etlTest
	@transformRunId int,
	@containerId entityId = NULL OUTPUT,
	@rowsInserted int = 0 OUTPUT,
	@rowsDeleted int = 0 OUTPUT,
	@rowsModified int = 0 OUTPUT,
	@returnMsg varchar(100) = 'default message' OUTPUT,
	@debug varchar(1000) = '',
	@filterRunId int = null,
	@filterStartTimeStamp datetime = null,
	@filterEndTimeStamp datetime = null,
	@testMode int,
	@testInOutParam varchar(10) = null OUTPUT,
	@runCount int = 1 OUTPUT,
	@previousFilterRunId int = null OUTPUT,
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
	6	Run filter strategy, require @filterRunId. Test persistence.
	7	Modified since filter strategy, require @filterStartTimeStamp & @filterEndTimeStamp. Test persistence.

*/

IF @testMode IS NULL
BEGIN
	SET @returnMsg = 'No testMode set'
	RETURN 1
END

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
	IF @runCount > 1 AND (@previousFilterRunId IS NULL OR @previousFilterRunId <= @filterRunId)
	BEGIN
		SET @returnMsg = 'Required @filterRunId was not persisted from previous run.'
		RETURN 1
	END
	SET @previousFilterRunId = @filterRunId
END

IF @testMode = 7
BEGIN
	IF @runCount > 1 AND (@previousFilterStartTimeStamp IS NULL OR @previousFilterEndTimeStamp IS NULL
							OR @previousFilterStartTimeStamp <= @filterStartTimeStamp OR @previousFilterEndTimeStamp <= @filterEndTimeStamp)
	BEGIN
		SET @returnMsg = 'Required @filterStartTimeStamp or @filterEndTimeStamp were not persisted from previous run.'
		RETURN 1
	END
	SET @previousFilterStartTimeStamp = @filterStartTimeStamp
	SET @previousFilterEndTimeStamp = @filterEndTimeStamp
END

-- set value for persistence tests
IF @testInOutParam IS NOT NULL SET @testInOutParam = 'after'

RETURN 0

END
GO
