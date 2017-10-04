select
project,
shortname,
dpt_code,
TIMESTAMPADD(SQL_TSI_MINUTE, -1, now()) as modified,
from lists.project1