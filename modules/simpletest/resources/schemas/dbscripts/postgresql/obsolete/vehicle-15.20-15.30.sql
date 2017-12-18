/*
 * Copyright (c) 2015-2016 LabKey Corporation
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
/* vehicle-15.20-15.21.sql */

CREATE OR REPLACE FUNCTION vehicle.etltest
(IN transformrunid integer
, IN containerid entityid DEFAULT NULL::character varying
, INOUT rowsinserted integer DEFAULT 0
, INOUT rowsdeleted integer DEFAULT 0
, INOUT rowsmodified integer DEFAULT 0
, INOUT returnmsg character varying DEFAULT 'default message'::character varying
, IN debug character varying DEFAULT ''::character varying
, IN filterrunid integer DEFAULT NULL::integer
, INOUT filterstarttimestamp timestamp without time zone DEFAULT NULL::timestamp without time zone
, INOUT filterendtimestamp timestamp without time zone DEFAULT NULL::timestamp without time zone
, INOUT previousfilterrunid integer DEFAULT (-1)
, INOUT previousfilterstarttimestamp timestamp without time zone DEFAULT NULL::timestamp without time zone
, INOUT previousfilterendtimestamp timestamp without time zone DEFAULT NULL::timestamp without time zone
--, INOUT procVersion decimal DEFAULT 0
, IN testmode integer DEFAULT (-1)
, INOUT testinoutparam character varying DEFAULT ''::character varying
, INOUT runcount integer DEFAULT 1
, OUT return_status integer)
  RETURNS record AS
$BODY$

/*
	Test modes
	1	normal operation
	2	return code > 0
	3	raise error
	4	input/output parameter persistence
	5	override of persisted input/output parameter
	6	Run filter strategy, require filterRunId. Test persistence.
  7 Modified since filter strategy, no source, require filterStartTimeStamp & filterEndTimeStamp,
		populated from output of previous run
  8	Modified since filter strategy with source, require filterStartTimeStamp & filterEndTimeStamp
		populated from the filter strategy IncrementalStartTime & IncrementalEndTime

*/
BEGIN

IF testMode IS NULL
THEN
  returnMsg := 'No testMode set';
  return_status := 1;
  RETURN;
END IF;

IF runCount IS NULL
THEN
	runCount := 1;
ELSE
	runCount := runCount + 1;
END IF;

IF testMode = 1
THEN
	RAISE NOTICE '%', 'Test print statement logging';
	rowsInserted := 1;
	rowsDeleted := 2;
	rowsModified := 4;
	returnMsg := 'Test returnMsg logging';
	return_status := 0;
	RETURN;
END IF;

IF testMode = 2 THEN return_status := 1; RETURN; END IF;

IF testMode = 3
THEN
	returnMsg := 'Intentional SQL Exception From Inside Proc';
	RAISE EXCEPTION '%', returnMsg;
END IF;

IF testMode = 4 AND testInOutParam != 'after' AND runCount > 1
THEN
	returnMsg := 'Expected value "after" for testInOutParam on run count = ' || runCount || ', but was ' || testInOutParam;
	return_status := 1;
	RETURN;
END IF;

IF testMode = 5 AND testInOutParam != 'before' AND runCount > 1
THEN
	returnMsg := 'Expected value "before" for testInOutParam on run count = ' || runCount || ', but was ' || testInOutParam;
	return_status := 1;
	RETURN;
END IF;

IF testMode = 6
THEN
	IF filterRunId IS NULL
	THEN
		returnMsg := 'Required filterRunId value not supplied';
		return_status := 1;
		RETURN;
	END IF;
	IF runCount > 1 AND (previousFilterRunId IS NULL OR previousFilterRunId >= filterRunId)
	THEN
		returnMsg := 'Required filterRunId was not persisted from previous run.';
		return_status := 1;
		RETURN;
	END IF;
	previousFilterRunId := filterRunId;
END IF;

IF testMode = 7
THEN
	IF runCount > 1 AND (filterStartTimeStamp IS NULL AND filterEndTimeStamp IS NULL)
	THEN
		returnMsg := 'Required filterStartTimeStamp or filterEndTimeStamp were not persisted from previous run.';
		return_status := 1;
		RETURN;
	END IF;
	filterStartTimeStamp := localtimestamp;
	filterEndTimeStamp := localtimestamp;
END IF;

IF testMode = 8
THEN
	IF runCount > 1 AND ((previousFilterStartTimeStamp IS NULL AND previousFilterEndTimeStamp IS NULL)
							OR (filterStartTimeStamp IS NULL AND filterEndTimeStamp IS NULL))
	THEN
		returnMsg := 'Required filterStartTimeStamp or filterEndTimeStamp were not persisted from previous run.';
		return_status := 1;
		RETURN;
	END IF;
	previousFilterStartTimeStamp := coalesce(filterStartTimeStamp, localtimestamp);
	previousFilterEndTimeStamp := coalesce(filterEndTimeStamp, localtimestamp);
END IF;


-- set value for persistence tests
IF testInOutParam != ''
THEN
	testInOutParam := 'after';
END IF;

return_status := 0;
RETURN;

END;
$BODY$
  LANGUAGE plpgsql;