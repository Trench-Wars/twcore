CREATE TABLE alerts (
  id int not null default 0,
  name varchar(32) not null,
  date timestamp,
  primary key (id,name)
);