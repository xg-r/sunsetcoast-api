CREATE TABLE business_owners
(
    id                  bigint       not null auto_increment,
    name                varchar(255) not null,
    business_number     varchar(20)  not null,
    representative_name varchar(100),
    phone               varchar(30),
    email               varchar(255),
    address             text         not null,
    status              varchar(30)  not null,
    created_at          datetime     not null,
    updated_at          datetime,
    primary key (id),
    unique key uq_business_number (business_number)
);