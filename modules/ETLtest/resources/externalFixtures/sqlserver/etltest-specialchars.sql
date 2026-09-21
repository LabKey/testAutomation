/*
 * Copyright (c) 2026 LabKey Corporation
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

-- SQL Server fixture for the DataIntegration StoredProcedureStep. Apply by hand to an external SQL Server
-- database; see ../README.md. Not a module dbscript: it is deliberately outside schemas/dbscripts/ so the
-- module upgrade scanner never runs it against the primary database.

-- Creates a schema and stored procedure whose names contain special characters (spaces, exclamation point, and an
-- embedded double-quote in the procedure name). These exercise the identifier quoting/escaping that SqlDialect
-- applies when building the CALL statement for the StoredProcedureStep. Driven by ETLs/SProcSpecialCharacters.xml.

-- Use double-quote delimited identifiers (with the interior quote doubled as "") rather than [bracket] identifiers.
-- LabKey's SqlScanner, which splits scripts into statements, does not understand bracket quoting and would misread a
-- double-quote inside [ ... ] as the start of a string literal; it does correctly handle a doubled "" as an escaped
-- quote inside a "-delimited identifier. QUOTED_IDENTIFIER must be ON for "..." to be treated as an identifier.
SET QUOTED_IDENTIFIER ON;
GO

CREATE SCHEMA "etl test!schema";
GO

CREATE PROCEDURE "etl test!schema"."etl""test proc!"
    @transformRunId int,
    @rowsInserted int = 0 OUTPUT,
    @rowsDeleted int = 0 OUTPUT,
    @rowsModified int = 0 OUTPUT,
    @returnMsg varchar(100) = 'default message' OUTPUT
AS
BEGIN
    SET @rowsInserted = 1
    SET @rowsDeleted = 0
    SET @rowsModified = 0
    SET @returnMsg = 'Special characters proc ran'
    RETURN 0
END
GO
