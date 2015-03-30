CREATE TABLE vehicle.etl_delete
(
  RowId SERIAL,
  container entityid,
  created TIMESTAMP,
  modified TIMESTAMP,

  id VARCHAR(9),
  name VARCHAR(100),
  TransformRun INT,
  rowversion SERIAL,
  CONSTRAINT PK_etldelete PRIMARY KEY (rowid),
  CONSTRAINT AK_etldelete UNIQUE (container,id),
  CONSTRAINT FK_etldelete_container FOREIGN KEY (container) REFERENCES core.containers (entityid)
);

ALTER TABLE vehicle.etl_source ADD COLUMN rowversion SERIAL;

ALTER TABLE vehicle.transfer ADD CONSTRAINT PK_transfer PRIMARY KEY (rowid);