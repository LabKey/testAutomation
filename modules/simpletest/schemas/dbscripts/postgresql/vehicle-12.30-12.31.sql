CREATE TABLE vehicle.emissiontest (
  rowid SERIAL,
  name varchar(100),
  container entityid,
  parentTest int,
  vehicleId int,
  result boolean,

  CONSTRAINT PK_emissiontest PRIMARY KEY (rowid),
  CONSTRAINT FK_emissiontest_container FOREIGN KEY (container) REFERENCES core.containers (entityid)
);
