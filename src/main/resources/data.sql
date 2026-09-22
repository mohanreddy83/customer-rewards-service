INSERT INTO customer (customer_id, name) VALUES (1, 'Alice Johnson');
INSERT INTO customer (customer_id, name) VALUES (2, 'Bob Smith');
INSERT INTO customer (customer_id, name) VALUES (3, 'Carol Williams');
INSERT INTO customer (customer_id, name) VALUES (4, 'David Brown');
INSERT INTO customer (customer_id, name) VALUES (5, 'Eve Davis');

-- Alice: May purchase falls outside the default June-August window.
INSERT INTO purchase_transaction (customer_id, amount, transaction_date, description) VALUES (1, 300.00, '2026-05-20', 'Furniture (outside default period)');
INSERT INTO purchase_transaction (customer_id, amount, transaction_date, description) VALUES (1, 120.00, '2026-06-05', 'Clothing');
INSERT INTO purchase_transaction (customer_id, amount, transaction_date, description) VALUES (1,  75.50, '2026-06-18', 'Groceries');
INSERT INTO purchase_transaction (customer_id, amount, transaction_date, description) VALUES (1, 200.00, '2026-07-02', 'Electronics');
INSERT INTO purchase_transaction (customer_id, amount, transaction_date, description) VALUES (1,  49.99, '2026-07-21', 'Books (below $50, earns nothing)');
INSERT INTO purchase_transaction (customer_id, amount, transaction_date, description) VALUES (1, 100.00, '2026-08-09', 'Shoes (exactly $100)');
INSERT INTO purchase_transaction (customer_id, amount, transaction_date, description) VALUES (1, 130.00, '2026-08-25', 'Home decor');

-- Bob
INSERT INTO purchase_transaction (customer_id, amount, transaction_date, description) VALUES (2,  50.00, '2026-06-10', 'Pharmacy (exactly $50, earns nothing)');
INSERT INTO purchase_transaction (customer_id, amount, transaction_date, description) VALUES (2, 101.00, '2026-06-28', 'Sporting goods');
INSERT INTO purchase_transaction (customer_id, amount, transaction_date, description) VALUES (2, 250.00, '2026-07-15', 'Appliance');
INSERT INTO purchase_transaction (customer_id, amount, transaction_date, description) VALUES (2,  60.25, '2026-08-03', 'Garden supplies');
INSERT INTO purchase_transaction (customer_id, amount, transaction_date, description) VALUES (2,  90.00, '2026-08-30', 'Toys');

-- Carol
INSERT INTO purchase_transaction (customer_id, amount, transaction_date, description) VALUES (3, 500.00, '2026-06-14', 'Laptop');
INSERT INTO purchase_transaction (customer_id, amount, transaction_date, description) VALUES (3,  20.00, '2026-07-08', 'Stationery');
INSERT INTO purchase_transaction (customer_id, amount, transaction_date, description) VALUES (3,  55.00, '2026-07-29', 'Cosmetics');
INSERT INTO purchase_transaction (customer_id, amount, transaction_date, description) VALUES (3, 110.00, '2026-08-12', 'Headphones');

-- David: no purchases in June.
INSERT INTO purchase_transaction (customer_id, amount, transaction_date, description) VALUES (4,  99.99, '2026-07-04', 'Kitchenware');
INSERT INTO purchase_transaction (customer_id, amount, transaction_date, description) VALUES (4, 100.01, '2026-07-19', 'Tools');
INSERT INTO purchase_transaction (customer_id, amount, transaction_date, description) VALUES (4, 175.00, '2026-08-21', 'Bicycle accessories');

-- Eve: only a small purchase, so every month is zero.
INSERT INTO purchase_transaction (customer_id, amount, transaction_date, description) VALUES (5,  45.00, '2026-08-15', 'Snacks');
