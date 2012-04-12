ALTER TABLE data ADD INDEX data (type, value(32));
ALTER TABLE poll ADD INDEX poll (type, value);
