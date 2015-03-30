CREATE TABLE vehicle.etl_delete
(
  RowId INT IDENTITY(1,1),
  container entityid,
  created DATETIME,
  modified DATETIME,

  id VARCHAR(9),
  name VARCHAR(100),
  TransformRun INT,
  rowversion rowversion,
  CONSTRAINT PK_etldelete PRIMARY KEY (rowid),
  CONSTRAINT AK_etldelete UNIQUE (container,id),
  CONSTRAINT FK_etldelete_container FOREIGN KEY (container) REFERENCES core.containers (entityid)
);

ALTER TABLE vehicle.etl_source ADD rowversion rowversion;

ALTER TABLE vehicle.transfer ADD CONSTRAINT PK_transfer PRIMARY KEY (rowid);