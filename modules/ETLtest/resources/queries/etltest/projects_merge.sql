/*
 * Copyright (c) 2017-2019 LabKey Corporation
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
  1 AS project,
  'account 1' AS account, -- empty base value -> non-empty base value
  NULL AS inves, -- non-empty base value -> empty base value
  12 AS inves2, -- non-empty base value -> different non-empty base value
  'outside_email 1' AS outside_email, -- empty extensible value -> non-empty extensible value
  NULL AS outside_inst, -- non-empty extensible value -> empty extensible value
  'outside_phone 1 changed' AS outside_phone, -- non-empty extensible value -> different non-empty extensible value
  CAST('01/02/1995' as TIMESTAMP) as modified

UNION SELECT
  2 AS project,
  'account 2' AS account, -- base table column
  20 AS inves, -- base table column
  21 AS inves2, -- base table column
  'outside_email 2' AS outside_email, -- extensible table column
  'outside_inst 2' AS outside_inst, -- extensible table column
  'outside_phone 2' AS outside_phone, -- extensible table column
  CAST('01/02/1995' as TIMESTAMP) as modified
