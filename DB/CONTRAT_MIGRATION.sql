ALTER TABLE contrat ADD COLUMN IF NOT EXISTS numero_contrat VARCHAR(50);
ALTER TABLE contrat ADD COLUMN IF NOT EXISTS lieu_signature VARCHAR(200);
ALTER TABLE contrat ADD COLUMN IF NOT EXISTS date_signature DATE;
CREATE UNIQUE INDEX IF NOT EXISTS idx_contrat_numero_unique ON contrat(numero_contrat);
