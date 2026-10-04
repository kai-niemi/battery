-- Params:
-- account_id1 - 1st account id (UUID)
-- account_id2 - 2nd account id (UUID)
-- amount - Transfer amount (decimal)
-- city - City name

with head as (
    insert into bank.transfer (city)
        values (:city)
        returning id),
     item1 as (
         insert into bank.transfer_item (transfer_id, account_id, city, amount, running_balance)
             values ((select id from head),
                     :account_id1,
                     :city,
                     :amount,
                     (select balance + :amount
                      from bank.account
                      where id = :account_id1))
             returning transfer_id),
     item2 as (
         insert into bank.transfer_item (transfer_id, account_id, city, amount, running_balance)
             values ((select id from head),
                     :account_id2,
                     :city,
                     -:amount,
                     (select balance - :amount
                      from bank.account
                      where id = :account_id2))
             returning transfer_id)
update bank.account
set balance = account.balance + dt.balance
from (select unnest(array [:amount, -:amount]) as balance,
             unnest(array [
                 :account_id1::uuid,
                 :account_id2::uuid
                 ]) as id) as dt
where account.id = dt.id
returning account.id, account.balance;