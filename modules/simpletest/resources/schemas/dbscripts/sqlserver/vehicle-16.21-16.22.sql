-- Undo change to this table from the 16.20-16.21 script. Make the merge test use etl_target2 instead
DROP TABLE vehicle.etl_target;
CREATE TABLE vehicle.etl_target(
  RowId INT IDENTITY(1,1),
  container entityid,
  created DATETIME,
  modified DATETIME,
  id VARCHAR(9),
  name VARCHAR(100),
  diTransformRunId INT,

  CONSTRAINT PK_etltarget PRIMARY KEY (rowid),
  CONSTRAINT AK_etltarget UNIQUE (container,id),
  CONSTRAINT FK_etltarget_container FOREIGN KEY (container) REFERENCES core.containers (entityid)
);

TRUNCATE TABLE vehicle.etl_target2;
ALTER TABLE vehicle.etl_target2 DROP CONSTRAINT PK_etltarget2;
ALTER TABLE vehicle.etl_target2 DROP CONSTRAINT AK_etltarget2;
ALTER TABLE vehicle.etl_target2 ALTER COLUMN container entityid NOT NULL;
GO
ALTER TABLE vehicle.etl_target2 ADD CONSTRAINT AK_etltarget2 UNIQUE (Container, id);
ALTER TABLE vehicle.etl_target2 ADD CONSTRAINT PK_etltarget2 PRIMARY KEY (RowId, container);

TRUNCATE TABLE vehicle.etl_180column_target;
ALTER TABLE vehicle.etl_180column_target DROP CONSTRAINT PK_etl_180column_target;
ALTER TABLE vehicle.etl_180column_target ALTER COLUMN container entityid NOT NULL;
GO
ALTER TABLE vehicle.etl_180column_target ADD CONSTRAINT PK_etl_180column_target PRIMARY KEY (RowId, container);

