/*
 * Copyright (c) 2017 LabKey Corporation
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
SELECT
  2 AS project,
  'account 2' AS account, -- base table column
  'inves 2' AS inves, -- base table column
  'inves2 2' AS inves2, -- base table column
  'outside_email 2' AS outside_email, -- extensible table column
  'outside_inst 2' AS outside_inst, -- extensible table column
  'outside_phone 2' AS outside_phone, -- extensible table column
  CAST('01/03/1995' as TIMESTAMP) as modified
