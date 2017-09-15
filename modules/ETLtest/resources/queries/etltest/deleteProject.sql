select
2 as project,
TIMESTAMPADD(SQL_TSI_MINUTE, 4, now()) as modified,
1 as rowversion