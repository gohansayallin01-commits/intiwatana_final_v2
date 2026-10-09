-- =============================================================================
-- INTIWATANA - Migracion V7: seccion editorial LOGIN en la gestion del portal
-- =============================================================================
-- La seccion LOGIN permite administrar la imagen principal y el contenido
-- visual del Login Premium desde el Panel Administrador (ROLE_ADMIN).
--
-- Solo se amplia la restriccion CHECK de 'contenidos_portal.seccion' para
-- admitir el valor 'LOGIN'. No se elimina ni modifica ninguna fila existente.
-- =============================================================================

ALTER TABLE public.contenidos_portal DROP CONSTRAINT IF EXISTS contenidos_portal_seccion_check;

ALTER TABLE public.contenidos_portal ADD CONSTRAINT contenidos_portal_seccion_check CHECK (
    ((seccion)::text = ANY ((ARRAY[
        'HERO'::character varying,
        'DESTINOS'::character varying,
        'BENEFICIOS'::character varying,
        'NOSOTROS'::character varying,
        'CONTACTO'::character varying,
        'FOOTER'::character varying,
        'LOGIN'::character varying
    ])::text[]))
);