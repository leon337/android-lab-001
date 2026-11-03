-- ANDROID-LAB-DB-001: persistencia das anotacoes do laboratorio.
-- Executar exclusivamente no database android_lab_001 do projeto Neon android-lab-001.
-- Sem extensoes, segredos, IA externa ou dados pessoais.
CREATE TABLE IF NOT EXISTS public.notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content TEXT NOT NULL CHECK (char_length(btrim(content)) BETWEEN 1 AND 5000),
    suggested_summary TEXT,
    approved BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT approved_requires_summary
        CHECK (NOT approved OR NULLIF(btrim(suggested_summary), '') IS NOT NULL)
);

CREATE INDEX IF NOT EXISTS notes_created_at_idx
    ON public.notes (created_at DESC);

COMMENT ON TABLE public.notes IS
    'Android Lab 001: anotacoes do laboratorio, sem autenticacao publica e sem API exposta.';
