CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER destinations_set_updated_at
BEFORE UPDATE ON destinations
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER deliveries_set_updated_at
BEFORE UPDATE ON deliveries
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();
