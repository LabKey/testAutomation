/*
 * Copyright (c) 2012-2014 LabKey Corporation
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

-- clean up old records
DELETE FROM vehicle.Vehicles;
DELETE FROM vehicle.Models;
DELETE FROM vehicle.Manufacturers;
DELETE FROM vehicle.Colors;
GO

-- add container constraint
ALTER TABLE vehicle.Vehicles
    ADD CONSTRAINT FK_Vehicles_Container FOREIGN KEY (Container) REFERENCES core.Containers (EntityId);

