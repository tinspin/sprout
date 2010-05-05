drop table if exists link;
drop table if exists meta;
drop table if exists node;
drop table if exists data;
drop table if exists poll;

create table link (
	parent                  	BIGINT	NOT NULL	DEFAULT 0,
	child                   	BIGINT	NOT NULL	DEFAULT 0,
	type                    	INTEGER	NOT NULL	DEFAULT 0,
	date                    	BIGINT	NOT NULL	DEFAULT 0,
	primary key (parent,child)
) ENGINE = InnoDB;
create table meta (
	node                    	BIGINT	NOT NULL	DEFAULT 0,
	data                    	BIGINT	NOT NULL	DEFAULT 0,
	type                    	SMALLINT	NOT NULL	DEFAULT 0,
	date                    	BIGINT	NOT NULL	DEFAULT 0,
	primary key (node,data)
) ENGINE = InnoDB;
create table node (
	id                      	BIGINT	AUTO_INCREMENT,
	type                    	INTEGER	NOT NULL	DEFAULT 0,
	date                    	BIGINT	NOT NULL	DEFAULT 0,
	primary key (id)
) ENGINE = InnoDB;
create table data (
	id                      	BIGINT	AUTO_INCREMENT,
	value                   	BLOB,
	type                    	SMALLINT	NOT NULL	DEFAULT 0,
	date                    	BIGINT	NOT NULL	DEFAULT 0,
	primary key (id)
) ENGINE = InnoDB;
create table poll (
	node                    	BIGINT	NOT NULL	DEFAULT 0,
	value                   	DOUBLE PRECISION	NOT NULL	DEFAULT 0,
	type                    	SMALLINT	NOT NULL	DEFAULT 0,
	date                    	BIGINT	NOT NULL	DEFAULT 0,
	primary key (node)
) ENGINE = InnoDB;
