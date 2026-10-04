insert into bank.account (city, balance, name, allow_negative)
select 'stockholm',
       '100.00',
       (concat('user:', no::text)),
       0
from generate_series(1, 500) no;

insert into bank.account (city, balance, name, allow_negative)
select 'new york',
       '100.00',
       (concat('user:', no::text)),
       0
from generate_series(1, 500) no;

insert into bank.account (city, balance, name, allow_negative)
select 'london',
       '100.00',
       (concat('user:', no::text)),
       0
from generate_series(1, 500) no;
