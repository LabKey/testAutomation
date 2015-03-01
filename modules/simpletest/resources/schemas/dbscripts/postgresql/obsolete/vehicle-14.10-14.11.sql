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

CREATE OR REPLACE FUNCTION vehicle.etltestresultset(transformrunid integer, containerid entityid DEFAULT NULL::character varying, debug character varying DEFAULT ''::character varying, filterrunid integer DEFAULT NULL::integer, filterstarttimestamp timestamp without time zone DEFAULT NULL::timestamp without time zone, filterendtimestamp timestamp without time zone DEFAULT NULL::timestamp without time zone, previousfilterrunid integer DEFAULT (-1), previousfilterstarttimestamp timestamp without time zone DEFAULT NULL::timestamp without time zone, previousfilterendtimestamp timestamp without time zone DEFAULT NULL::timestamp without time zone, testmode integer DEFAULT (-1))
  RETURNS refcursor AS
$BODY$

DECLARE
      ref refcursor;                                                     -- Declare a cursor variable
    BEGIN
      OPEN ref FOR SELECT * FROM vehicle.etl_source where container = containerid;   -- Open a cursor
      RETURN ref;                                                       -- Return the cursor to the caller
    END;

$BODY$
  LANGUAGE plpgsql VOLATILE
  COST 100;
ALTER FUNCTION vehicle.etltestresultset(integer, entityid, character varying, integer, timestamp without time zone, timestamp without time zone, integer, timestamp without time zone, timestamp without time zone, integer)
  OWNER TO postgres;