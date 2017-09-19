select
2 as project,
TIMESTAMPADD(SQL_TSI_MINUTE, 1, now()) as modified,
1 as rowversion