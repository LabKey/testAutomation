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