CREATE TABLE vehicle.emissiontest (
  rowid int identity(1,1),
  name varchar(100),
  container entityid,
  parentTest int,
  vehicleId int,
  result bit,

  CONSTRAINT PK_emissiontest PRIMARY KEY (rowid),
  CONSTRAINT FK_emissiontest_container FOREIGN KEY (container) REFERENCES core.containers (entityid)
);
