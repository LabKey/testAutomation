/*
 * Copyright (c) 2013 LabKey Corporation
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

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
-- =============================================
-- Author:		Ryan Standley
-- Create date: 10/24/2013
-- Description:	sp for ETL testing ModifiedSince filter strategy
-- =============================================
CREATE PROCEDURE vehicle.etlTestModifiedSince
	@containerId uniqueidentifier = NULL,
	@rowsInserted int = 0 OUTPUT,
	@rowsDeleted int = 0 OUTPUT,
	@rowsModified int = 0 OUTPUT,
	@returnMsg varchar(100) = 'default message' OUTPUT,
	@debug varchar(1000) = '',
	@desiredErrorOut int = 0,
	@transformRunId int,
	@created datetime,
	@modified datetime
AS
BEGIN
	SET NOCOUNT ON;
	SET IDENTITY_INSERT [vehicle].[etl_target] ON
	DECLARE @returnVal int = @desiredErrorOut
	DECLARE @RowId int, @id varchar(9), @name varchar(100), @TransformRun int
	DECLARE C CURSOR FOR SELECT rowid, name,id FROM vehicle.etl_source where TransformRun => @filterRunId ORDER BY ID
	OPEN C
	FETCH NEXT FROM C INTO @RowId, @name, @id
	WHILE @@FETCH_STATUS = 0
	BEGIN
		SET @rowsInserted = @rowsInserted + 1
		SET @created = GETDATE()
		SET @modified = GETDATE()
		IF NOT EXISTS(SELECT * FROM etl_target WHERE Id = @Id)
		BEGIN
		INSERT INTO [vehicle].[etl_target](RowId, container, id, name, diTransformRunId, created, modified) SELECT @RowId, @containerId, @id, @name, @transformRunId, @created, @modified
		END
		FETCH NEXT FROM C INTO @RowId, @name, @id
	END
	CLOSE C
	DEALLOCATE C
	IF(@desiredErrorOut > 10)
	BEGIN
	SET @returnMsg = 'error > 10, throwing exception'
	RAISERROR(@returnMsg, @desiredErrorOut, 1)
	END
	ELSE
	BEGIN
	RETURN @returnVal
	END
END
GO


/****** Object:  StoredProcedure [vehicle].[etlTestRunBased]    Script Date: 10/31/2013 11:08:19 AM ******/
SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO


-- =============================================
-- Author:		Ryan Standley
-- Create date: 10/24/2013
-- Description:	sp for ETL testing ModifiedSince filter strategy
-- =============================================
CREATE PROCEDURE [vehicle].[etlTestRunBased]
	@containerId uniqueidentifier = NULL,
	@rowsInserted int = 0 OUTPUT,
	@rowsDeleted int = 0 OUTPUT,
	@rowsModified int = 0 OUTPUT,
	@returnMsg varchar(100) = 'default message' OUTPUT,
	@debug varchar(1000) = '',
	@desiredErrorOut int = 0,
	@transformRunId int,
	@created datetime,
	@modified datetime,
	@filterRunId int
AS
BEGIN
    PRINT @filterRunId
	SET NOCOUNT ON;
	SET IDENTITY_INSERT [vehicle].[etl_target] ON
	DECLARE @returnVal int = @desiredErrorOut
	DECLARE @RowId int, @id varchar(9), @name varchar(100), @TransformRun int
	DECLARE C CURSOR FOR SELECT Rowid, Name,Id FROM vehicle.etl_source WHERE TransformRun = @filterRunId ORDER BY ID
	OPEN C
	FETCH NEXT FROM C INTO @RowId, @name, @id
	WHILE @@FETCH_STATUS = 0
	BEGIN
		SET @created = GETDATE()
		SET @modified = GETDATE()
		IF NOT EXISTS(SELECT * FROM etl_target WHERE id = @id)
		BEGIN
		SET @rowsInserted = @rowsInserted + 1
		INSERT INTO [vehicle].[etl_target](RowId, container, id, name, diTransformRunId, created, modified) SELECT @RowId, @containerId, @id, @name, @transformRunId, @created, @modified
		END
		FETCH NEXT FROM C INTO @RowId, @name, @id
	END
	CLOSE C
	DEALLOCATE C
	IF(@desiredErrorOut > 10)
	BEGIN
	SET @returnMsg = 'error > 10, throwing exception'
	RAISERROR(@returnMsg, @desiredErrorOut, 1)
	END
	ELSE
	BEGIN
	RETURN @returnVal
	END
END


GO

ALTER TABLE vehicle.[Transfer] Add [container] [dbo].[ENTITYID] NULL
GO

ALTER TABLE [vehicle].[Transfer]  WITH CHECK ADD  CONSTRAINT [FK_etltransfer_container] FOREIGN KEY([container])
REFERENCES [core].[Containers] ([EntityId])
GO

ALTER TABLE [vehicle].[Transfer] CHECK CONSTRAINT [FK_etltransfer_container]
GO

