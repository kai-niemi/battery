-- Params:
-- account_id1 - 1st account id (UUID)
-- account_id2 - 2nd account id (UUID)
-- amount - Transfer amount (decimal)
-- city - City name
insert into bank.transfer (city) values (:city) returning id as transfer_id;

insert into bank.transfer_item (transfer_id, account_id, city, amount, running_balance)
values (:transfer_id, :account_id1, :city, :amount,
        (select balance + :amount from bank.account where id = :account_id1));

insert into bank.transfer_item (transfer_id, account_id, city, amount, running_balance)
values (:transfer_id, :account_id2, :city, - :amount,
        (select balance - :amount from bank.account where id = :account_id2));

update bank.account set balance = balance + :amount where id = :account_id1;
update bank.account set balance = balance - :amount where id = :account_id2;
