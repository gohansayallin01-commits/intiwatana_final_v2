-- =============================================================================
-- INTIWATANA - Migracion V3: gestion del portal (contenido editorial)
-- =============================================================================
-- SOLO ESTRUCTURA. Crea el almacen de contenido editorial del Portal Publico
-- administrado desde el Panel Administrador (ROLE_ADMIN).
--
--   1. contenidos_portal         -> estado actual (borrador + publicado) por seccion
--   2. contenido_portal_version  -> historial versionado (nunca se borra)
--   3. imagenes_portal           -> metadatos de recursos subidos y validados
--
-- NO crea datos editoriales ficticios: los valores iniciales legitimos se
-- cargan como BORRADOR desde la aplicacion (ver ContenidoPortalServiceImpl),
-- reflejando EXACTAMENTE el contenido vigente del portal y sin cifras,
-- telefonos ni contactos inventados.
--
-- Ninguna tabla existente se altera de forma destructiva.
-- =============================================================================

SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET search_path TO public;

--
-- Name: contenidos_portal; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.contenidos_portal (
    id bigint NOT NULL,
    seccion character varying(30) NOT NULL,
    estado character varying(20) DEFAULT 'BORRADOR'::character varying NOT NULL,
    version integer DEFAULT 0 NOT NULL,
    contenido_borrador text,
    contenido_publicado text,
    autor character varying(120),
    actualizado_por character varying(120),
    publicado_en timestamp without time zone,
    activo boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    deleted_at timestamp without time zone,
    CONSTRAINT contenidos_portal_estado_check CHECK (((estado)::text = ANY ((ARRAY['BORRADOR'::character varying, 'PUBLICADO'::character varying, 'ARCHIVADO'::character varying])::text[]))),
    CONSTRAINT contenidos_portal_seccion_check CHECK (((seccion)::text = ANY ((ARRAY['HERO'::character varying, 'DESTINOS'::character varying, 'BENEFICIOS'::character varying, 'NOSOTROS'::character varying, 'CONTACTO'::character varying, 'FOOTER'::character varying])::text[])))
);


--
-- Name: contenidos_portal_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.contenidos_portal_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: contenidos_portal_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.contenidos_portal_id_seq OWNED BY public.contenidos_portal.id;


--
-- Name: contenido_portal_version; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.contenido_portal_version (
    id bigint NOT NULL,
    contenido_id bigint,
    seccion character varying(30) NOT NULL,
    version integer NOT NULL,
    estado character varying(20) NOT NULL,
    accion character varying(30) NOT NULL,
    contenido text NOT NULL,
    usuario_id bigint,
    username character varying(80),
    rol character varying(40),
    observacion character varying(500),
    fecha_hora timestamp without time zone DEFAULT now() NOT NULL,
    activo boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    deleted_at timestamp without time zone
);


--
-- Name: contenido_portal_version_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.contenido_portal_version_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: contenido_portal_version_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.contenido_portal_version_id_seq OWNED BY public.contenido_portal_version.id;


--
-- Name: imagenes_portal; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.imagenes_portal (
    id bigint NOT NULL,
    seccion character varying(30),
    nombre_interno character varying(120) NOT NULL,
    url character varying(500) NOT NULL,
    miniatura_url character varying(500),
    mime character varying(50) NOT NULL,
    peso_bytes bigint NOT NULL,
    ancho integer,
    alto integer,
    checksum character varying(64) NOT NULL,
    texto_alt character varying(300),
    autor character varying(150),
    fuente character varying(200),
    licencia character varying(100),
    url_origen character varying(500),
    autorizacion_comercial boolean DEFAULT false NOT NULL,
    subido_por character varying(80),
    activo boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone,
    deleted_at timestamp without time zone
);


--
-- Name: imagenes_portal_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.imagenes_portal_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: imagenes_portal_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.imagenes_portal_id_seq OWNED BY public.imagenes_portal.id;


--
-- Name: contenidos_portal; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contenidos_portal ALTER COLUMN id SET DEFAULT nextval('public.contenidos_portal_id_seq'::regclass);


--
-- Name: contenido_portal_version; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contenido_portal_version ALTER COLUMN id SET DEFAULT nextval('public.contenido_portal_version_id_seq'::regclass);


--
-- Name: imagenes_portal; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.imagenes_portal ALTER COLUMN id SET DEFAULT nextval('public.imagenes_portal_id_seq'::regclass);


--
-- Name: contenido_portal_version contenido_portal_version_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contenido_portal_version
    ADD CONSTRAINT contenido_portal_version_pkey PRIMARY KEY (id);


--
-- Name: contenidos_portal contenidos_portal_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contenidos_portal
    ADD CONSTRAINT contenidos_portal_pkey PRIMARY KEY (id);


--
-- Name: contenidos_portal contenidos_portal_seccion_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contenidos_portal
    ADD CONSTRAINT contenidos_portal_seccion_key UNIQUE (seccion);


--
-- Name: imagenes_portal imagenes_portal_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.imagenes_portal
    ADD CONSTRAINT imagenes_portal_pkey PRIMARY KEY (id);


--
-- Name: contenido_portal_version contenido_portal_version_contenido_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contenido_portal_version
    ADD CONSTRAINT contenido_portal_version_contenido_id_fkey FOREIGN KEY (contenido_id) REFERENCES public.contenidos_portal(id);


--
-- Name: idx_contenido_portal_version_seccion; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_contenido_portal_version_seccion ON public.contenido_portal_version USING btree (seccion, version DESC);


--
-- Name: idx_imagenes_portal_checksum; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_imagenes_portal_checksum ON public.imagenes_portal USING btree (checksum);
