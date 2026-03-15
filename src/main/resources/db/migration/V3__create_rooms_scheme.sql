CREATE TABLE rooms
(
    id                bigint       not null auto_increment,
    name              varchar(255) not null,
    type              varchar(30)  not null,
    status            varchar(30)  not null,
    business_owner_id bigint       not null,
    primary key (id),
    unique key uq_rooms_name (name)
);
