/*
 * Copyright (c) 2014-2015 LabKey Corporation
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


--This script was erroneously numbered, and would only run on a bootstrapped system.  Problem is fixed by vehicle-15.00-15.01

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE PROCEDURE [vehicle].[etlTestResultSet]
	@transformRunId int,
	@containerId varchar(100) = NULL OUTPUT,
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
	@previousFilterRunId int = null OUTPUT,
	@previousFilterStartTimeStamp datetime = null OUTPUT,
	@previousFilterEndTimeStamp datetime = null OUTPUT
AS
BEGIN

IF @testMode = 9
BEGIN
	SELECT * FROM vehicle.etl_source WHERE container = @containerId
END

IF @testInOutParam IS NOT NULL SET @testInOutParam = 'after'

RETURN 0

END

GO