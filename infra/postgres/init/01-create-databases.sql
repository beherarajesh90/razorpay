-- One database per service. Runs once, on first start of the postgres volume.
CREATE DATABASE merchant_db;
CREATE DATABASE payment_db;
CREATE DATABASE vault_db;
CREATE DATABASE operations_db;
