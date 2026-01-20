ALTER TABLE p_popups
    ADD COLUMN reservation_open_at timestamp;

ALTER TABLE p_popups
    ADD COLUMN address_road text;

ALTER TABLE p_popups
    ADD COLUMN address_detail text;
