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