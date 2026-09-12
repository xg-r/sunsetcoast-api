ALTER TABLE car_reservation_customers
    MODIFY COLUMN identity_encrypted VARBINARY(512) NOT NULL;