select
project,
shortname,
department,
TIMESTAMPADD(SQL_TSI_MINUTE, 1, now()) as modified,
from lists.project2