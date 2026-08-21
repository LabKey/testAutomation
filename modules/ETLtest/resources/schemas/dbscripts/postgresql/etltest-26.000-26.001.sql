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

-- Create a schema and stored procedure whose names contain special characters (spaces, '!', and an embedded
-- double-quote in the procedure name). These exercise the identifier quoting/escaping that SqlDialect applies
-- when building the CALL statement for the DataIntegration StoredProcedureStep. The schema is registered with
-- the module via the matching "etl test!schema.xml" metadata file; it is created here because the module dbscript
-- filename convention only permits word-character schema names.

CREATE SCHEMA "etl test!schema";

CREATE FUNCTION "etl test!schema"."etl""test proc!"
  (IN transformrunid integer
    , INOUT rowsinserted integer DEFAULT 0
    , INOUT rowsdeleted integer DEFAULT 0
    , INOUT rowsmodified integer DEFAULT 0
    , INOUT returnmsg character varying DEFAULT 'default message'::character varying
    , OUT return_status integer)
  RETURNS record AS
$BODY$
BEGIN
  rowsInserted := 1;
  rowsDeleted := 0;
  rowsModified := 0;
  returnMsg := 'Special characters proc ran';
  return_status := 0;
  RETURN;
END;
$BODY$
LANGUAGE plpgsql;
