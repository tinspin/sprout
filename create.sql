drop table if exists link_table;
drop table if exists node_table;
drop table if exists poll_table;
drop table if exists meta_table;
drop table if exists data_table;

create table link_table (
	link_parent                  	BIGINT,
	link_child                   	BIGINT,
	link_type                    	INT,
	link_date                    	BIGINT,
	constraint link_primary_key primary key (link_parent,link_child)
);
create table node_table (
	node_id                      	BIGSERIAL,
	node_type                    	INT,
	node_date                    	BIGINT,
	constraint node_primary_key primary key (node_id)
);
create table poll_table (
	poll_node                    	BIGINT,
	poll_value                   	DOUBLE PRECISION,
	poll_type                    	SMALLINT,
	poll_date                    	BIGINT,
	constraint poll_primary_key primary key (poll_node)
);
create table meta_table (
	meta_node                    	BIGINT,
	meta_data                    	BIGINT,
	meta_type                    	SMALLINT,
	meta_date                    	BIGINT,
	constraint meta_primary_key primary key (meta_node,meta_data)
);
create table data_table (
	data_id                      	BIGSERIAL,
	data_value                   	BYTEA,
	data_type                    	SMALLINT,
	data_date                    	BIGINT,
	constraint data_primary_key primary key (data_id)
);
