-- Customer is optional on a card token; the tokenize API allows it to be omitted.
ALTER TABLE card_token ALTER COLUMN customer DROP NOT NULL;
