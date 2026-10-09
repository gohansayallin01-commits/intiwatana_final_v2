import { useState, useEffect, useMemo, useRef } from "react";
import {
  Save, Upload, Send, Archive, History, RefreshCw, Eye, AlertCircle,
  Image as ImageIcon, Trash2, Plus, RotateCcw, Monitor, Tablet, Smartphone, X,
  ChevronDown, Search, ClipboardCheck, GitCompare, ArrowUp, ArrowDown,
  Ticket, Package, MapPin, Shield, Clock, User, Bus, Route, Heart, Star,
  Check as CheckIcon, Info, Phone, Mail,
} from "lucide-react";
import "./portal-gestion.css";

const C = {
  verde: "#127369", oscuro: "#0B3D38", amarillo: "#F5C518",
  medio: "#667875", gris: "#17312E", rojo: "#C84C4C", naranja: "#D58B15",
};

const SECCIONES = [
  { key: "HERO", label: "Hero", desc: "Encabezado principal del Portal Público con imagen y textos de bienvenida." },
  { key: "DESTINOS", label: "Destinos", desc: "Ciudades y rutas destacadas que se muestran en la página." },
  { key: "BENEFICIOS", label: "Beneficios", desc: "Ventajas y servicios que diferencian a la empresa." },
  { key: "NOSOTROS", label: "Nosotros", desc: "Presentación institucional e historia de la empresa." },
  { key: "CONTACTO", label: "Contacto", desc: "Teléfonos, correo, dirección y redes de atención." },
  { key: "FOOTER", label: "Footer", desc: "Pie de página con identidad, enlaces y aviso legal." },
  { key: "LOGIN", label: "Login", desc: "Pantalla de acceso: imagen principal, textos y servicios." },
];

const BLOQUES = {
  HERO: [
    { id: "imagen", t: "Imagen de fondo", s: "Imagen principal y créditos." },
    { id: "textos", t: "Textos", s: "Título y subtítulo del encabezado." },
    { id: "elementos", t: "Elementos", s: "Buscador de bus y recursos." },
    { id: "revision", t: "Revisión", s: "Resumen del contenido de esta sección." },
  ],
  DESTINOS: [
    { id: "lista", t: "Lista de destinos", s: "Orden, visibilidad e imágenes." },
    { id: "revision", t: "Revisión", s: "Resumen del contenido de esta sección." },
  ],
  BENEFICIOS: [
    { id: "lista", t: "Lista de beneficios", s: "Textos, iconos y visibilidad." },
    { id: "revision", t: "Revisión", s: "Resumen del contenido de esta sección." },
  ],
  NOSOTROS: [
    { id: "presentacion", t: "Presentación", s: "Título y descripción institucional." },
    { id: "imagen", t: "Imagen", s: "Imagen y créditos." },
    { id: "revision", t: "Revisión", s: "Resumen del contenido de esta sección." },
  ],
  CONTACTO: [
    { id: "datos", t: "Datos de contacto", s: "Teléfono, correo, dirección y horario." },
    { id: "redes", t: "Redes sociales", s: "Enlaces a redes oficiales." },
    { id: "visibilidad", t: "Visibilidad", s: "Mostrar u ocultar la sección." },
    { id: "revision", t: "Revisión", s: "Resumen del contenido de esta sección." },
  ],
  FOOTER: [
    { id: "identidad", t: "Identidad", s: "Descripción de la empresa." },
    { id: "enlaces", t: "Enlaces", s: "Accesos del pie de página." },
    { id: "legal", t: "Legal y créditos", s: "Aviso legal y créditos fotográficos." },
    { id: "revision", t: "Revisión", s: "Resumen del contenido de esta sección." },
  ],
  LOGIN: [
    { id: "imagen", t: "Imagen principal", s: "Imagen, encuadre y créditos." },
    { id: "textos", t: "Textos", s: "Título institucional y frases." },
    { id: "servicios", t: "Servicios mostrados", s: "Tarjetas de servicios del Login." },
    { id: "revision", t: "Revisión", s: "Resumen del contenido de esta sección." },
  ],
};

const ICONOS = ["ticket", "package", "mappin", "shield", "clock", "user", "bus", "route", "heart", "star", "check", "info", "phone", "mail"];
const ICONO_MAP = { ticket: Ticket, package: Package, mappin: MapPin, shield: Shield, clock: Clock, user: User, bus: Bus, route: Route, heart: Heart, star: Star, check: CheckIcon, info: Info, phone: Phone, mail: Mail };

const LABEL_CAMPO = {
  HERO: { titulo: "Título", subtitulo: "Subtítulo", imagenFondoUrl: "Imagen de fondo", imagenBusUrl: "Imagen del bus", imagenAlt: "Texto alternativo", autor: "Autor", fuente: "Fuente", licencia: "Licencia", atribucion: "Atribución", mostrarBus: "Mostrar buscador" },
  DESTINOS: { destinos: "Destinos destacados" },
  BENEFICIOS: { beneficios: "Beneficios" },
  NOSOTROS: { titulo: "Título", descripcion: "Descripción", imagenUrl: "Imagen", imagenAlt: "Texto alternativo", autor: "Autor", fuente: "Fuente", licencia: "Licencia" },
  CONTACTO: { telefono: "Teléfono", correo: "Correo", direccion: "Dirección", horario: "Horario", visible: "Mostrar sección", redes: "Redes sociales" },
  FOOTER: { descripcion: "Descripción", infoLegal: "Información legal", creditosFotograficos: "Créditos fotográficos", enlaces: "Enlaces" },
  LOGIN: { tituloInstitucional: "Título institucional", frasePrincipal: "Frase principal", textoComplementario: "Texto complementario", imagenUrl: "Imagen de escritorio", imagenMobileUrl: "Imagen móvil", imagenAlt: "Texto alternativo", autor: "Autor", fuente: "Fuente", licencia: "Licencia", atribucion: "Atribución", focoX: "Posición horizontal", focoY: "Posición vertical", overlay: "Oscurecimiento de la imagen", serviciosVisible: "Mostrar servicios", servicios: "Servicios" },
};

const CREDITOS_SOPORTADOS = {
  HERO: ["imagenAlt", "autor", "fuente", "licencia", "atribucion"],
  NOSOTROS: ["imagenAlt", "autor", "fuente", "licencia"],
  LOGIN: ["imagenAlt", "autor", "fuente", "licencia", "atribucion"],
};

// ── Utilidades ─────────────────────────────────────────────────────────────

function tienePendientes(info) {
  if (!info || info.estado === "ARCHIVADO") return false;
  return JSON.stringify(info.borrador ?? null) !== JSON.stringify(info.publicado ?? null);
}

function estadoVisible(info) {
  if (!info) return { txt: "BORRADOR", cls: "pg-badge-borrador" };
  if (info.estado === "ARCHIVADO") return { txt: "ARCHIVADO", cls: "pg-badge-archivado" };
  if (tienePendientes(info)) return { txt: "CAMBIOS SIN PUBLICAR", cls: "pg-badge-pendiente" };
  if (info.estado === "PUBLICADO") return { txt: "PUBLICADO", cls: "pg-badge-publicado" };
  return { txt: "BORRADOR", cls: "pg-badge-borrador" };
}

function fmtFecha(iso) {
  if (!iso) return "—";
  try { return new Date(iso).toLocaleString("es-PE"); } catch { return "—"; }
}

function fmtPeso(bytes) {
  const n = Number(bytes);
  if (!n || n <= 0) return "—";
  if (n < 1024) return `${n} B`;
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(0)} KB`;
  return `${(n / 1024 / 1024).toFixed(1)} MB`;
}

function valorTexto(v) {
  if (v === null || v === undefined || v === "") return "—";
  if (typeof v === "boolean") return v ? "Sí" : "No";
  if (Array.isArray(v)) return `${v.length} elemento${v.length === 1 ? "" : "s"}`;
  if (typeof v === "object") return JSON.stringify(v);
  return String(v);
}

function diffSeccion(seccion, actual, nuevo) {
  const labels = LABEL_CAMPO[seccion] || {};
  const a = actual || {};
  const n = nuevo || {};
  const claves = Array.from(new Set([...Object.keys(a), ...Object.keys(n)]));
  const out = [];
  for (const k of claves) {
    if (JSON.stringify(a[k]) === JSON.stringify(n[k])) continue;
    out.push({ label: labels[k] || k, antes: valorTexto(a[k]), despues: valorTexto(n[k]) });
  }
  return out;
}

function resumenSeccion(seccion, contenido) {
  const labels = LABEL_CAMPO[seccion] || {};
  const rows = [];
  for (const [k, v] of Object.entries(contenido || {})) {
    if (v === "" || v === null || v === undefined) continue;
    rows.push({ label: labels[k] || k, value: valorTexto(v) });
  }
  return rows;
}

// ── Primitivas de formulario ───────────────────────────────────────────────

function useDialogo(onCerrar) {
  const ref = useRef(null);
  useEffect(() => {
    const previo = document.activeElement;
    if (ref.current) ref.current.focus();
    const h = (e) => { if (e.key === "Escape") onCerrar(); };
    document.addEventListener("keydown", h);
    return () => {
      document.removeEventListener("keydown", h);
      if (previo && typeof previo.focus === "function") previo.focus();
    };
  }, [onCerrar]);
  return ref;
}

function Campo({ label, value, onChange, textarea, maxLength, placeholder, type = "text" }) {
  return (
    <div className="pg-field">
      <label className="pg-label">{label}</label>
      {textarea ? (
        <textarea className="pg-textarea" maxLength={maxLength} placeholder={placeholder} value={value ?? ""} onChange={(e) => onChange(e.target.value)} />
      ) : (
        <input className="pg-input" type={type} maxLength={maxLength} placeholder={placeholder} value={value ?? ""} onChange={(e) => onChange(e.target.value)} />
      )}
      {maxLength ? <p className="pg-hint">Máximo {maxLength} caracteres.</p> : null}
    </div>
  );
}

function Check({ label, value, onChange }) {
  return (
    <label className="pg-check">
      <input type="checkbox" checked={!!value} onChange={(e) => onChange(e.target.checked)} />
      {label}
    </label>
  );
}

function IconPicker({ value, onChange }) {
  return (
    <div className="pg-field">
      <label className="pg-label">Icono</label>
      <div className="pg-icon-grid">
        {ICONOS.map((ic) => {
          const I = ICONO_MAP[ic];
          return (
            <button type="button" key={ic} className="pg-icon-btn" aria-pressed={value === ic} aria-label={`Icono ${ic}`} title={ic} onClick={() => onChange(ic)}>
              <I size={18} />
            </button>
          );
        })}
      </div>
    </div>
  );
}

function PortalStatusBadge({ info, extra }) {
  const b = estadoVisible(info);
  return (
    <>
      <span className={`pg-badge ${b.cls}`}>{b.txt}</span>
      {extra ? <span className="pg-badge pg-badge-pendiente">CAMPOS SIN GUARDAR</span> : null}
    </>
  );
}

// ── Áreas ──────────────────────────────────────────────────────────────────

function PortalSectionNavigation({ secciones, activa, onChange }) {
  return (
    <nav className="pg-nav" aria-label="Secciones del portal">
      <div className="pg-nav-title">Secciones</div>
      <ul className="pg-nav-list">
        {SECCIONES.map((s) => {
          const info = secciones.find((x) => x.seccion === s.key);
          return (
            <li key={s.key}>
              <button type="button" className="pg-nav-item" aria-current={activa === s.key ? "true" : "false"} onClick={() => onChange(s.key)}>
                <span className="pg-nav-label">{s.label}</span>
                {tienePendientes(info) && <span className="pg-dot" title="Cambios sin publicar" aria-hidden="true" />}
              </button>
            </li>
          );
        })}
      </ul>
      <div className="pg-nav-select-wrap">
        <label className="pg-label" htmlFor="pg-seccion-select">Sección</label>
        <select id="pg-seccion-select" className="pg-nav-select" value={activa} onChange={(e) => onChange(e.target.value)}>
          {SECCIONES.map((s) => {
            const info = secciones.find((x) => x.seccion === s.key);
            return <option key={s.key} value={s.key}>{s.label}{tienePendientes(info) ? " · cambios sin publicar" : ""}</option>;
          })}
        </select>
      </div>
    </nav>
  );
}

function PortalEditorAccordion({ id, title, sub, open, onToggle, children }) {
  return (
    <div className="pg-acc-item" data-open={open}>
      <button type="button" className="pg-acc-head" aria-expanded={open} aria-controls={`pg-acc-${id}`} onClick={onToggle}>
        <span className="pg-acc-titles">
          <span className="pg-acc-t">{title}</span>
          {sub ? <span className="pg-acc-s">{sub}</span> : null}
        </span>
        <ChevronDown className="pg-chev" size={17} aria-hidden="true" />
      </button>
      {open ? <div className="pg-acc-body" id={`pg-acc-${id}`} role="region">{children}</div> : null}
    </div>
  );
}

function PortalImagePicker({ imagenes, storage, imgForm, setImgForm, subiendo, onSubir, onSeleccionar, onCerrar, seccion }) {
  const [buscar, setBuscar] = useState("");
  const [filtro, setFiltro] = useState("todas");
  const ref = useDialogo(onCerrar);

  const filtradas = (imagenes || []).filter((im) => {
    const q = buscar.trim().toLowerCase();
    const okQ = !q || `${im.textoAlt || ""} ${im.autor || ""} ${im.fuente || ""}`.toLowerCase().includes(q);
    const okF = filtro === "todas" || (im.mime || "").toLowerCase().includes(filtro);
    return okQ && okF;
  });

  const formatos = [
    ["todas", "Todos"],
    ["jpeg", "JPEG"],
    ["png", "PNG"],
    ["webp", "WebP"],
  ];

  return (
    <div className="pg-modal" role="presentation" onClick={onCerrar}>
      <div className="pg-modal-card pg-modal-wide" role="dialog" aria-modal="true" aria-label="Biblioteca de imágenes" ref={ref} tabIndex={-1} onClick={(e) => e.stopPropagation()}>
        <div className="pg-modal-head">
          <div>
            <h3>Biblioteca de imágenes</h3>
            <p>Imágenes validadas para el Portal. Formatos admitidos: WebP, JPEG y PNG.</p>
          </div>
          <button type="button" className="pg-icon-close" aria-label="Cerrar" onClick={onCerrar}><X size={20} /></button>
        </div>
        <div className="pg-modal-body">
          <div className="pg-picker-search">
            <input className="pg-input" placeholder="Buscar por descripción, autor o fuente…" value={buscar} onChange={(e) => setBuscar(e.target.value)} aria-label="Buscar imágenes" />
            <select className="pg-select" value={filtro} onChange={(e) => setFiltro(e.target.value)} aria-label="Filtrar por formato" style={{ width: 120 }}>
              {formatos.map(([v, l]) => <option key={v} value={v}>{l}</option>)}
            </select>
          </div>

          {!storage.disponible && (
            <p className="pg-hint" style={{ color: C.naranja, fontWeight: 700 }}>
              La subida de imágenes está deshabilitada: el almacenamiento no es persistente.
            </p>
          )}

          <div className="pg-img-grid" style={{ marginBottom: 16 }}>
            {filtradas.map((im) => (
              <div key={im.id} className="pg-img-card">
                <img src={im.miniaturaUrl || im.url} alt={im.textoAlt || "Imagen del portal"} loading="lazy" />
                <div className="pg-img-info">
                  <span className="pg-img-alt" title={im.textoAlt || "Sin descripción"}>{im.textoAlt || "Sin descripción"}</span>
                  <span>{im.ancho && im.alto ? `${im.ancho}×${im.alto} · ${fmtPeso(im.pesoBytes)}` : fmtPeso(im.pesoBytes)}</span>
                  <span>Autor: {im.autor || "—"}</span>
                  <span>Licencia: {im.licencia || "—"}</span>
                  <span className={im.autorizacionComercial ? "pg-aut-ok" : "pg-aut-no"}>
                    {im.autorizacionComercial ? "Autorización comercial" : "Sin autorización comercial"}
                  </span>
                  <button type="button" className="pg-btn pg-btn-primary pg-btn-sm pg-btn-block" style={{ marginTop: 6 }} onClick={() => onSeleccionar(im)}>
                    <CheckIcon size={14} /> Usar imagen
                  </button>
                </div>
              </div>
            ))}
            {filtradas.length === 0 && <div className="pg-empty" style={{ gridColumn: "1 / -1" }}>No se encontraron imágenes con ese criterio.</div>}
          </div>

          <div style={{ borderTop: `1px solid var(--pg-borde)`, paddingTop: 14 }}>
            <div className="pg-preview-title" style={{ marginBottom: 10 }}><Upload size={16} /> Subir nueva imagen</div>
            <form onSubmit={onSubir}>
              <div className="pg-field">
                <label className="pg-label">Archivo</label>
                <input className="pg-input" type="file" accept="image/png,image/jpeg,image/webp" disabled={!storage.disponible}
                  onChange={(e) => setImgForm((f) => ({ ...f, archivo: e.target.files?.[0] || null }))} />
                <p className="pg-hint">La imagen se valida por contenido real. Etiqueta la imagen que usarás en la sección {seccion}.</p>
              </div>
              <div className="pg-grid2">
                <Campo label="Descripción (alt)" value={imgForm.textoAlt} maxLength={200} onChange={(v) => setImgForm((f) => ({ ...f, textoAlt: v }))} />
                <Campo label="Autor" value={imgForm.autor} maxLength={150} onChange={(v) => setImgForm((f) => ({ ...f, autor: v }))} />
                <Campo label="Fuente" value={imgForm.fuente} maxLength={200} onChange={(v) => setImgForm((f) => ({ ...f, fuente: v }))} />
                <Campo label="Licencia" value={imgForm.licencia} maxLength={100} onChange={(v) => setImgForm((f) => ({ ...f, licencia: v }))} />
                <Campo label="URL de origen (opcional)" value={imgForm.urlOrigen} maxLength={500} onChange={(v) => setImgForm((f) => ({ ...f, urlOrigen: v }))} />
              </div>
              <Check label="Declaro autorización comercial de uso" value={imgForm.autorizacionComercial} onChange={(v) => setImgForm((f) => ({ ...f, autorizacionComercial: v }))} />
              <button type="submit" className="pg-btn pg-btn-primary" disabled={subiendo || !storage.disponible}>
                <Upload size={15} /> {subiendo ? "Validando…" : "Subir imagen"}
              </button>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
}

function PortalPreviewPanel({ activa, form, preview, setPreview }) {
  const devices = [
    { k: "desktop", label: "Escritorio", I: Monitor },
    { k: "tablet", label: "Tablet", I: Tablet },
    { k: "mobile", label: "Móvil", I: Smartphone },
  ];
  return (
    <aside className="pg-preview" aria-label="Vista previa del borrador">
      <div className="pg-preview-head">
        <div className="pg-preview-title"><Eye size={16} /> Vista previa</div>
        <div className="pg-seg" role="group" aria-label="Tamaño de vista previa">
          {devices.map((d) => (
            <button type="button" key={d.k} aria-pressed={preview === d.k} title={d.label} aria-label={d.label} onClick={() => setPreview(d.k)}>
              <d.I size={15} />
            </button>
          ))}
        </div>
      </div>
      <p className="pg-preview-note">Borrador · no es la versión publicada</p>
      <div className="pg-stage">
        <div className="pg-frame" data-device={preview}>
          <PreviewPortal seccion={activa} form={form} />
        </div>
      </div>
    </aside>
  );
}

function PortalPublishSummary({ onCerrar, onConfirmar, seccionLabel, version, cambios, primeraPublicacion, ocupado }) {
  const ref = useDialogo(onCerrar);
  return (
    <div className="pg-modal" role="presentation" onClick={onCerrar}>
      <div className="pg-modal-card" role="dialog" aria-modal="true" aria-label="Revisión antes de publicar" ref={ref} tabIndex={-1} onClick={(e) => e.stopPropagation()}>
        <div className="pg-modal-head">
          <div>
            <h3>Revisión antes de publicar · {seccionLabel}</h3>
            <p>{primeraPublicacion ? "Será la primera versión publicada de esta sección." : `Nueva versión: ${(version || 0) + 1}. La versión actual es ${version || 0}.`}</p>
          </div>
          <button type="button" className="pg-icon-close" aria-label="Cerrar" onClick={onCerrar}><X size={20} /></button>
        </div>
        <div className="pg-modal-body">
          {cambios.length === 0 ? (
            <div className="pg-empty">No se detectaron cambios respecto a la versión publicada.</div>
          ) : (
            <>
              <p className="pg-help" style={{ fontWeight: 700, color: "var(--pg-gris)" }}>Se aplicarán {cambios.length} cambio{cambios.length === 1 ? "" : "s"} en el Portal Público:</p>
              {cambios.map((c, i) => (
                <div className="pg-diff" key={i}>
                  <div className="pg-diff-row">
                    <span className="pg-diff-label">{c.label}</span>
                    <span className="pg-diff-val">Antes: {c.antes} → <span className="pg-diff-new">Ahora: {c.despues}</span></span>
                  </div>
                </div>
              ))}
            </>
          )}
          <p className="pg-hint" style={{ marginTop: 12 }}>Al confirmar, los visitantes verán esta versión de inmediato.</p>
        </div>
        <div className="pg-modal-foot">
          <button type="button" className="pg-btn pg-btn-ghost" onClick={onCerrar}>Volver a editar</button>
          <button type="button" className="pg-btn pg-btn-dark" disabled={ocupado || cambios.length === 0} onClick={onConfirmar}>
            <Send size={15} /> {ocupado ? "Publicando…" : "Publicar cambios"}
          </button>
        </div>
      </div>
    </div>
  );
}

function PortalVersionHistory({ historial, publicado, seccion, onRestaurar, onCerrar, ocupado }) {
  const [expandido, setExpandido] = useState({});
  const [comparando, setComparando] = useState({});
  const ref = useDialogo(onCerrar);

  return (
    <div className="pg-modal" role="presentation" onClick={onCerrar}>
      <div className="pg-modal-card pg-modal-wide" role="dialog" aria-modal="true" aria-label="Historial de versiones" ref={ref} tabIndex={-1} onClick={(e) => e.stopPropagation()}>
        <div className="pg-modal-head">
          <div>
            <h3>Historial de versiones · {seccion}</h3>
            <p>Versiones más recientes primero. Restaurar aplica el contenido como borrador o versión publicada según su estado.</p>
          </div>
          <button type="button" className="pg-icon-close" aria-label="Cerrar" onClick={onCerrar}><X size={20} /></button>
        </div>
        <div className="pg-modal-body">
          {(!historial || historial.length === 0) && <div className="pg-empty">Sin versiones registradas para esta sección.</div>}
          {(historial || []).map((v) => {
            const b = estadoVisible({ estado: v.estado, borrador: v.contenido, publicado: v.estado === "PUBLICADO" ? v.contenido : publicado });
            const abiertoCont = !!expandido[v.id];
            const comp = comparando[v.id];
            const rows = comp ? diffSeccion(seccion, publicado, v.contenido) : resumenSeccion(seccion, v.contenido);
            return (
              <div className="pg-version" key={v.id}>
                <div className="pg-version-top">
                  <div>
                    <strong style={{ color: C.oscuro, fontSize: 13 }}>Versión {v.version}</strong>{" "}
                    <span className={`pg-badge ${b.cls}`}>{v.estado || "—"}</span>{" "}
                    <span style={{ fontSize: 12, color: C.medio }}>· {v.accion || "—"}</span>
                  </div>
                  <div className="pg-inline">
                    <button type="button" className="pg-btn pg-btn-ghost pg-btn-sm" onClick={() => setExpandido((s) => ({ ...s, [v.id]: !s[v.id] }))}>
                      <Eye size={13} /> {abiertoCont ? "Ocultar" : "Ver"}
                    </button>
                    <button type="button" className="pg-btn pg-btn-ghost pg-btn-sm" onClick={() => setComparando((s) => ({ ...s, [v.id]: !s[v.id] }))}>
                      <GitCompare size={13} /> {comp ? "Ver resumen" : "Comparar"}
                    </button>
                    <button type="button" className="pg-btn pg-btn-primary pg-btn-sm" disabled={ocupado} onClick={() => onRestaurar(v)}>
                      <RotateCcw size={13} /> Restaurar
                    </button>
                  </div>
                </div>
                <div className="pg-version-meta">
                  {v.username || "—"} ({v.rol || "—"}) · {fmtFecha(v.fechaHora)}{v.observacion ? ` · ${v.observacion}` : ""}
                </div>
                {abiertoCont && (
                  <div className="pg-version-content">
                    {comp ? <p className="pg-hint">Cambios de esta versión respecto a lo publicado actualmente:</p> : <p className="pg-hint">Contenido de esta versión:</p>}
                    {rows.length === 0 ? <div className="pg-empty">Sin diferencias respecto a lo publicado.</div> : rows.map((r, i) => (
                      <div className="pg-summary-row" key={i}>
                        <span className="k">{r.label}</span>
                        <span className="v">{comp ? `${r.antes} → ${r.despues}` : r.value}</span>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            );
          })}
        </div>
        <div className="pg-modal-foot">
          <button type="button" className="pg-btn pg-btn-ghost" onClick={onCerrar}>Cerrar</button>
        </div>
      </div>
    </div>
  );
}

function ConfirmDialog({ titulo, mensaje, confirmLabel, onConfirm, onCancel, ocupado, danger }) {
  const ref = useDialogo(onCancel);
  return (
    <div className="pg-modal" role="presentation" onClick={onCancel}>
      <div className="pg-modal-card" role="dialog" aria-modal="true" aria-label={titulo} ref={ref} tabIndex={-1} onClick={(e) => e.stopPropagation()} style={{ width: "min(460px,100%)" }}>
        <div className="pg-modal-head">
          <h3>{titulo}</h3>
        </div>
        <div className="pg-modal-body">
          <p style={{ fontSize: 13, color: "var(--pg-gris)", margin: 0, lineHeight: 1.5 }}>{mensaje}</p>
        </div>
        <div className="pg-modal-foot">
          <button type="button" className="pg-btn pg-btn-ghost" onClick={onCancel}>Cancelar</button>
          <button type="button" className={`pg-btn ${danger ? "pg-btn-danger" : "pg-btn-dark"}`} disabled={ocupado} onClick={onConfirm}>
            {confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}

// ── Componente principal ───────────────────────────────────────────────────

export default function PortalGestion({ headers, showToast }) {
  const [secciones, setSecciones] = useState([]);
  const [activa, setActiva] = useState("HERO");
  const [form, setForm] = useState({});
  const [cargando, setCargando] = useState(true);
  const [ocupado, setOcupado] = useState(false);
  const [preview, setPreview] = useState("desktop");
  const [storage, setStorage] = useState({ disponible: false, tipo: "none", persistente: false });
  const [imagenes, setImagenes] = useState([]);
  const [subiendo, setSubiendo] = useState(false);
  const [imgForm, setImgForm] = useState({ textoAlt: "", autor: "", fuente: "", licencia: "", urlOrigen: "", autorizacionComercial: false, archivo: null });
  const [abiertos, setAbiertos] = useState({});
  const [picker, setPicker] = useState(null);
  const [revisando, setRevisando] = useState(false);
  const [verHistorial, setVerHistorial] = useState(false);
  const [historial, setHistorial] = useState([]);
  const [confirm, setConfirm] = useState(null);

  const seccionActual = useMemo(() => secciones.find((s) => s.seccion === activa) || null, [secciones, activa]);
  const borradorServidor = seccionActual?.borrador ?? null;
  const publicado = seccionActual?.publicado ?? null;

  const hayCambiosSinGuardar = useMemo(
    () => JSON.stringify(form) !== JSON.stringify(borradorServidor),
    [form, borradorServidor],
  );
  const hayCambiosSinPublicar = useMemo(
    () => !!seccionActual && seccionActual.estado !== "ARCHIVADO" && JSON.stringify(borradorServidor) !== JSON.stringify(publicado),
    [seccionActual, borradorServidor, publicado],
  );

  const cargar = async () => {
    setCargando(true);
    try {
      const [rSec, rImg, rAlm] = await Promise.all([
        fetch("/api/v1/portal/admin/secciones", { headers }),
        fetch("/api/v1/portal/admin/imagenes", { headers }),
        fetch("/api/v1/portal/admin/almacenamiento", { headers }),
      ]);
      const sec = rSec.ok ? await rSec.json() : null;
      const img = rImg.ok ? await rImg.json() : null;
      const alm = rAlm.ok ? await rAlm.json() : null;
      setSecciones(sec?.data || []);
      setImagenes(img?.data || []);
      if (alm?.data) setStorage({ persistente: false, ...alm.data });
    } catch {
      showToast("No se pudo cargar la gestión del portal. Verifica tu conexión e inténtalo de nuevo.");
    } finally {
      setCargando(false);
    }
  };

  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => { cargar(); }, []);

  useEffect(() => {
    const s = secciones.find((x) => x.seccion === activa);
    setForm(s?.borrador ? structuredClone(s.borrador) : {});
    setAbiertos({ [BLOQUES[activa]?.[0]?.id || "revision"]: true });
  }, [activa, secciones]);

  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }));
  const setArr = (k, idx, field, v) => setForm((f) => {
    const arr = [...(f[k] || [])];
    arr[idx] = { ...arr[idx], [field]: v };
    return { ...f, [k]: arr };
  });
  const addItem = (k, item) => setForm((f) => ({ ...f, [k]: [...(f[k] || []), item] }));
  const delItem = (k, idx) => setForm((f) => ({ ...f, [k]: (f[k] || []).filter((_, i) => i !== idx) }));
  const mover = (k, idx, dir) => setForm((f) => {
    const arr = [...(f[k] || [])];
    const j = idx + dir;
    if (j < 0 || j >= arr.length) return f;
    [arr[idx], arr[j]] = [arr[j], arr[idx]];
    return { ...f, [k]: arr.map((it, i) => ({ ...it, orden: i })) };
  });
  const toggle = (id) => setAbiertos((o) => ({ ...o, [id]: !o[id] }));

  const guardar = async () => {
    setOcupado(true);
    try {
      const res = await fetch(`/api/v1/portal/admin/secciones/${activa}/borrador`, {
        method: "PUT", headers, body: JSON.stringify({ contenido: form }),
      });
      const data = await res.json().catch(() => ({}));
      if (!res.ok) throw new Error(data.mensaje || "No se pudo guardar el borrador");
      showToast("BORRADOR GUARDADO. Los visitantes continúan viendo la versión publicada.");
      await cargar();
    } catch (e) { showToast(`No se pudo guardar: ${e.message}`); }
    finally { setOcupado(false); }
  };

  const accion = async (ruta, mensaje) => {
    setOcupado(true);
    try {
      const res = await fetch(`/api/v1/portal/admin/secciones/${activa}/${ruta}`, {
        method: "POST", headers, body: JSON.stringify({}),
      });
      const data = await res.json().catch(() => ({}));
      if (!res.ok) throw new Error(data.mensaje || "La acción no está permitida");
      showToast(mensaje);
      await cargar();
    } catch (e) { showToast(`No se pudo completar: ${e.message}`); }
    finally { setOcupado(false); }
  };

  const publicar = async () => {
    const nueva = (seccionActual?.version || 0) + 1;
    await accion("publicar", `PUBLICACIÓN COMPLETADA. El Portal Público utiliza ahora la versión ${nueva}.`);
    setRevisando(false);
  };

  const abrirHistorial = async () => {
    setVerHistorial(true);
    setHistorial([]);
    try {
      const res = await fetch(`/api/v1/portal/admin/secciones/${activa}/historial`, { headers });
      const data = res.ok ? await res.json() : null;
      setHistorial(data?.data || []);
    } catch { showToast("No se pudo cargar el historial."); }
  };

  const restaurarVersion = async (v) => {
    setConfirm({
      titulo: "Restaurar versión",
      mensaje: `Se restaurará la versión ${v.version} de la sección ${activa}. El contenido se aplicará como borrador (o versión publicada si la versión estaba publicada). Esta acción queda registrada en el historial.`,
      confirmLabel: "Restaurar versión",
      onConfirm: async () => {
        await accion(`restaurar?versionId=${v.id}`, "VERSIÓN RESTAURADA. Revísala y publica si corresponde.");
        setVerHistorial(false);
        setConfirm(null);
      },
    });
  };

  const abrirPicker = (target) => setPicker(target);

  const aplicarImagen = (im) => {
    if (!picker) return;
    setForm((f) => {
      const nf = { ...f };
      if (picker.lista) {
        const arr = [...(nf[picker.lista] || [])];
        arr[picker.idx] = { ...arr[picker.idx], [picker.campo]: im.url };
        if (!arr[picker.idx].imagenAlt && im.textoAlt) arr[picker.idx].imagenAlt = im.textoAlt;
        nf[picker.lista] = arr;
      } else {
        nf[picker.campo] = im.url;
        const cr = CREDITOS_SOPORTADOS[activa] || [];
        if (cr.includes("imagenAlt")) nf.imagenAlt = im.textoAlt || nf.imagenAlt || "";
        if (cr.includes("autor")) nf.autor = im.autor || "";
        if (cr.includes("fuente")) nf.fuente = im.fuente || "";
        if (cr.includes("licencia")) nf.licencia = im.licencia || "";
      }
      return nf;
    });
    setPicker(null);
    showToast("Imagen seleccionada. Guarda el borrador para conservarla.");
  };

  const subirImagen = async (e) => {
    e.preventDefault();
    if (!imgForm.archivo) return showToast("Selecciona un archivo de imagen antes de subir.");
    setSubiendo(true);
    try {
      const fd = new FormData();
      fd.append("archivo", imgForm.archivo);
      fd.append("seccion", activa);
      fd.append("textoAlt", imgForm.textoAlt);
      fd.append("autor", imgForm.autor);
      fd.append("fuente", imgForm.fuente);
      fd.append("licencia", imgForm.licencia);
      if (imgForm.urlOrigen) fd.append("urlOrigen", imgForm.urlOrigen);
      fd.append("autorizacionComercial", imgForm.autorizacionComercial ? "true" : "false");
      const res = await fetch("/api/v1/portal/admin/imagenes", {
        method: "POST", headers: { Authorization: headers.Authorization }, body: fd,
      });
      const data = await res.json().catch(() => ({}));
      if (!res.ok) throw new Error(data.mensaje || "No se pudo subir la imagen");
      showToast("Imagen validada y registrada correctamente.");
      setImgForm({ textoAlt: "", autor: "", fuente: "", licencia: "", urlOrigen: "", autorizacionComercial: false, archivo: null });
      await cargar();
    } catch (err) { showToast(`No se pudo subir la imagen: ${err.message}`); }
    finally { setSubiendo(false); }
  };

  const cambiarSeccion = (k) => { setActiva(k); setPicker(null); };

  // ── Bloques del editor ───────────────────────────────────────────────────

  const BotonImagen = ({ etiqueta, valor, campo, lista, idx, permiteCredito }) => (
    <div className="pg-field">
      <label className="pg-label">{etiqueta}</label>
      <div className="pg-inline" style={{ marginBottom: 6 }}>
        <span style={{ fontSize: 12, color: valor ? C.gris : C.medio, flex: 1, minWidth: 0, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
          {valor || "Sin imagen seleccionada"}
        </span>
        <button type="button" className="pg-btn pg-btn-ghost pg-btn-sm" onClick={() => abrirPicker({ campo, lista, idx })}>
          <ImageIcon size={14} /> {valor ? "Cambiar imagen" : "Elegir imagen"}
        </button>
        {valor ? (
          <button type="button" className="pg-btn pg-btn-danger pg-btn-sm" onClick={() => {
            if (lista) setArr(lista, idx, campo, "");
            else set(campo, "");
          }}>Quitar</button>
        ) : null}
      </div>
      {!permiteCredito ? <p className="pg-hint">Puedes escribir una ruta interna (por ejemplo /assets/…) o elegir de la biblioteca.</p> : null}
    </div>
  );

  const CampoFoco = ({ eje, campo, valor, etiquetas }) => {
    const v = valor ?? 50;
    const preset = v <= 33 ? 0 : v <= 66 ? 1 : 2;
    return (
      <div className="pg-field">
        <label className="pg-label">{eje}</label>
        <div className="pg-seg" role="group" aria-label={eje} style={{ marginBottom: 6 }}>
          {etiquetas.map(([txt, num], i) => (
            <button type="button" key={txt} aria-pressed={preset === i} onClick={() => set(campo, num)}>{txt}</button>
          ))}
        </div>
        <div className="pg-inline">
          <input type="number" min={0} max={100} className="pg-input" style={{ width: 96 }} value={valor ?? ""} onChange={(e) => set(campo, e.target.value === "" ? null : Number(e.target.value))} aria-label={`${eje} exacto (0-100)`} />
          <span className="pg-hint" style={{ margin: 0 }}>Valor exacto 0-100 (personalizado)</span>
        </div>
      </div>
    );
  };

  const ListaDestinos = () => {
    const arr = form.destinos || [];
    return (
      <div>
        {arr.map((d, i) => (
          <div className="pg-item" key={i}>
            <div className="pg-item-head">
              <strong>Destino #{i + 1}</strong>
              <div className="pg-inline">
                <button type="button" className="pg-btn pg-btn-ghost pg-btn-sm" aria-label="Subir" disabled={i === 0} onClick={() => mover("destinos", i, -1)}><ArrowUp size={13} /></button>
                <button type="button" className="pg-btn pg-btn-ghost pg-btn-sm" aria-label="Bajar" disabled={i === arr.length - 1} onClick={() => mover("destinos", i, 1)}><ArrowDown size={13} /></button>
                <button type="button" className="pg-btn pg-btn-danger pg-btn-sm" aria-label="Eliminar destino" onClick={() => delItem("destinos", i)}><Trash2 size={13} /></button>
              </div>
            </div>
            <Campo label="Título" value={d.titulo} maxLength={80} onChange={(v) => setArr("destinos", i, "titulo", v)} />
            <Campo label="Texto breve" value={d.texto} maxLength={240} onChange={(v) => setArr("destinos", i, "texto", v)} />
            <BotonImagen etiqueta="Imagen" valor={d.imagenUrl} campo="imagenUrl" lista="destinos" idx={i} />
            <Campo label="Texto alternativo (alt)" value={d.imagenAlt} maxLength={200} onChange={(v) => setArr("destinos", i, "imagenAlt", v)} />
            <Check label="Visible en el Portal" value={d.visible} onChange={(v) => setArr("destinos", i, "visible", v)} />
          </div>
        ))}
        {arr.length === 0 && <div className="pg-empty">Aún no hay destinos. Añade el primero.</div>}
        <button type="button" className="pg-btn pg-btn-ghost" disabled={arr.length >= 12} onClick={() => addItem("destinos", { titulo: "", texto: "", imagenUrl: "", imagenAlt: "", orden: arr.length, visible: true })}>
          <Plus size={14} /> Añadir destino
        </button>
        {arr.length >= 12 ? <p className="pg-hint">Máximo 12 destinos destacados.</p> : null}
      </div>
    );
  };

  const ListaBeneficios = () => {
    const arr = form.beneficios || [];
    return (
      <div>
        {arr.map((b, i) => (
          <div className="pg-item" key={i}>
            <div className="pg-item-head">
              <strong>Beneficio #{i + 1}</strong>
              <div className="pg-inline">
                <button type="button" className="pg-btn pg-btn-ghost pg-btn-sm" aria-label="Subir" disabled={i === 0} onClick={() => mover("beneficios", i, -1)}><ArrowUp size={13} /></button>
                <button type="button" className="pg-btn pg-btn-ghost pg-btn-sm" aria-label="Bajar" disabled={i === arr.length - 1} onClick={() => mover("beneficios", i, 1)}><ArrowDown size={13} /></button>
                <button type="button" className="pg-btn pg-btn-danger pg-btn-sm" aria-label="Eliminar beneficio" onClick={() => delItem("beneficios", i)}><Trash2 size={13} /></button>
              </div>
            </div>
            <Campo label="Título" value={b.titulo} maxLength={80} onChange={(v) => setArr("beneficios", i, "titulo", v)} />
            <Campo label="Descripción" value={b.descripcion} maxLength={240} onChange={(v) => setArr("beneficios", i, "descripcion", v)} />
            <IconPicker value={b.icono} onChange={(ic) => setArr("beneficios", i, "icono", ic)} />
            <Check label="Visible en el Portal" value={b.visible} onChange={(v) => setArr("beneficios", i, "visible", v)} />
          </div>
        ))}
        {arr.length === 0 && <div className="pg-empty">Aún no hay beneficios. Añade el primero.</div>}
        <button type="button" className="pg-btn pg-btn-ghost" disabled={arr.length >= 12} onClick={() => addItem("beneficios", { titulo: "", descripcion: "", icono: "ticket", orden: arr.length, visible: true })}>
          <Plus size={14} /> Añadir beneficio
        </button>
      </div>
    );
  };

  const ListaServicios = () => {
    const arr = form.servicios || [];
    return (
      <div>
        <Check label="Mostrar servicios en el Login" value={form.serviciosVisible} onChange={(v) => set("serviciosVisible", v)} />
        {arr.map((s, i) => (
          <div className="pg-item" key={i}>
            <div className="pg-item-head">
              <strong>Servicio #{i + 1}</strong>
              <button type="button" className="pg-btn pg-btn-danger pg-btn-sm" aria-label="Eliminar servicio" onClick={() => delItem("servicios", i)}><Trash2 size={13} /></button>
            </div>
            <Campo label="Título" value={s.titulo} maxLength={60} onChange={(v) => setArr("servicios", i, "titulo", v)} />
            <Campo label="Subtítulo" value={s.subtitulo} maxLength={120} onChange={(v) => setArr("servicios", i, "subtitulo", v)} />
            <IconPicker value={s.icono} onChange={(ic) => setArr("servicios", i, "icono", ic)} />
            <Check label="Visible" value={s.visible} onChange={(v) => setArr("servicios", i, "visible", v)} />
          </div>
        ))}
        {arr.length === 0 && <div className="pg-empty">Aún no hay servicios. Añade el primero.</div>}
        <button type="button" className="pg-btn pg-btn-ghost" disabled={arr.length >= 8} onClick={() => addItem("servicios", { titulo: "", subtitulo: "", icono: "ticket", visible: true })}>
          <Plus size={14} /> Añadir servicio
        </button>
      </div>
    );
  };

  const ListaRedes = () => {
    const arr = form.redes || [];
    return (
      <div>
        {arr.map((r, i) => (
          <div className="pg-item-row" key={i}>
            <input className="pg-input" placeholder="Nombre" maxLength={40} value={r.nombre || ""} onChange={(e) => setArr("redes", i, "nombre", e.target.value)} aria-label="Nombre de la red" />
            <input className="pg-input" placeholder="URL" maxLength={500} value={r.url || ""} onChange={(e) => setArr("redes", i, "url", e.target.value)} aria-label="URL de la red" />
            <button type="button" className="pg-btn pg-btn-danger pg-btn-sm" aria-label="Eliminar red" onClick={() => delItem("redes", i)}><Trash2 size={13} /></button>
          </div>
        ))}
        {arr.length === 0 && <div className="pg-empty">Sin redes sociales registradas.</div>}
        <button type="button" className="pg-btn pg-btn-ghost" disabled={arr.length >= 10} onClick={() => addItem("redes", { nombre: "", url: "" })}><Plus size={14} /> Añadir red</button>
      </div>
    );
  };

  const ListaEnlaces = () => {
    const arr = form.enlaces || [];
    return (
      <div>
        {arr.map((e, i) => (
          <div className="pg-item-row" key={i}>
            <input className="pg-input" placeholder="Texto" maxLength={60} value={e.texto || ""} onChange={(ev) => setArr("enlaces", i, "texto", ev.target.value)} aria-label="Texto del enlace" />
            <input className="pg-input" placeholder="URL" maxLength={500} value={e.url || ""} onChange={(ev) => setArr("enlaces", i, "url", ev.target.value)} aria-label="URL del enlace" />
            <button type="button" className="pg-btn pg-btn-danger pg-btn-sm" aria-label="Eliminar enlace" onClick={() => delItem("enlaces", i)}><Trash2 size={13} /></button>
          </div>
        ))}
        {arr.length === 0 && <div className="pg-empty">Sin enlaces registrados.</div>}
        <button type="button" className="pg-btn pg-btn-ghost" disabled={arr.length >= 20} onClick={() => addItem("enlaces", { texto: "", url: "" })}><Plus size={14} /> Añadir enlace</button>
      </div>
    );
  };

  const ResumenBloque = () => {
    const rows = resumenSeccion(activa, form);
    if (rows.length === 0) return <p className="pg-hint">Sin contenido cargado en esta sección.</p>;
    return (
      <div>
        {rows.map((r, i) => (
          <div className="pg-summary-row" key={i}>
            <span className="k">{r.label}</span>
            <span className="v">{r.value}</span>
          </div>
        ))}
      </div>
    );
  };

  const renderBloque = (id) => {
    switch (`${activa}:${id}`) {
      case "HERO:imagen":
        return (<>
          <BotonImagen etiqueta="Imagen de fondo" valor={form.imagenFondoUrl} campo="imagenFondoUrl" permiteCredito />
          <Campo label="Texto alternativo (alt)" value={form.imagenAlt} maxLength={200} onChange={(v) => set("imagenAlt", v)} />
          <Campo label="Autor" value={form.autor} maxLength={150} onChange={(v) => set("autor", v)} />
          <Campo label="Fuente" value={form.fuente} maxLength={200} onChange={(v) => set("fuente", v)} />
          <Campo label="Licencia" value={form.licencia} maxLength={100} onChange={(v) => set("licencia", v)} />
          <Campo label="Atribución" value={form.atribucion} maxLength={300} onChange={(v) => set("atribucion", v)} />
        </>);
      case "HERO:textos":
        return (<>
          <Campo label="Título" value={form.titulo} maxLength={80} onChange={(v) => set("titulo", v)} />
          <Campo label="Subtítulo" textarea value={form.subtitulo} maxLength={160} onChange={(v) => set("subtitulo", v)} />
        </>);
      case "HERO:elementos":
        return (<>
          <Check label="Mostrar buscador de bus en el Hero" value={form.mostrarBus} onChange={(v) => set("mostrarBus", v)} />
          <BotonImagen etiqueta="Imagen del bus (opcional)" valor={form.imagenBusUrl} campo="imagenBusUrl" permiteCredito />
        </>);
      case "DESTINOS:lista":
        return <ListaDestinos />;
      case "BENEFICIOS:lista":
        return <ListaBeneficios />;
      case "NOSOTROS:presentacion":
        return (<>
          <Campo label="Título" value={form.titulo} maxLength={80} onChange={(v) => set("titulo", v)} />
          <Campo label="Descripción" textarea value={form.descripcion} maxLength={1200} onChange={(v) => set("descripcion", v)} />
        </>);
      case "NOSOTROS:imagen":
        return (<>
          <BotonImagen etiqueta="Imagen" valor={form.imagenUrl} campo="imagenUrl" permiteCredito />
          <Campo label="Texto alternativo (alt)" value={form.imagenAlt} maxLength={200} onChange={(v) => set("imagenAlt", v)} />
          <Campo label="Autor" value={form.autor} maxLength={150} onChange={(v) => set("autor", v)} />
          <Campo label="Fuente" value={form.fuente} maxLength={200} onChange={(v) => set("fuente", v)} />
          <Campo label="Licencia" value={form.licencia} maxLength={100} onChange={(v) => set("licencia", v)} />
        </>);
      case "CONTACTO:datos":
        return (<>
          <div className="pg-grid2">
            <Campo label="Teléfono" value={form.telefono} maxLength={30} onChange={(v) => set("telefono", v)} />
            <Campo label="Correo" type="email" value={form.correo} maxLength={120} onChange={(v) => set("correo", v)} />
          </div>
          <Campo label="Dirección" value={form.direccion} maxLength={200} onChange={(v) => set("direccion", v)} />
          <Campo label="Horario" value={form.horario} maxLength={200} onChange={(v) => set("horario", v)} />
        </>);
      case "CONTACTO:redes":
        return <ListaRedes />;
      case "CONTACTO:visibilidad":
        return <Check label="Mostrar la sección de contacto en el Portal" value={form.visible} onChange={(v) => set("visible", v)} />;
      case "FOOTER:identidad":
        return <Campo label="Descripción de la empresa" textarea value={form.descripcion} maxLength={400} onChange={(v) => set("descripcion", v)} />;
      case "FOOTER:enlaces":
        return <ListaEnlaces />;
      case "FOOTER:legal":
        return (<>
          <Campo label="Información legal" textarea value={form.infoLegal} maxLength={1000} onChange={(v) => set("infoLegal", v)} />
          <Campo label="Créditos fotográficos" textarea value={form.creditosFotograficos} maxLength={1000} onChange={(v) => set("creditosFotograficos", v)} />
        </>);
      case "LOGIN:imagen":
        return (<>
          <BotonImagen etiqueta="Imagen principal (escritorio)" valor={form.imagenUrl} campo="imagenUrl" permiteCredito />
          <BotonImagen etiqueta="Imagen para móvil (opcional)" valor={form.imagenMobileUrl} campo="imagenMobileUrl" />
          <Campo label="Texto alternativo (alt)" value={form.imagenAlt} maxLength={200} onChange={(v) => set("imagenAlt", v)} />
          <div className="pg-grid2">
            <CampoFoco eje="Posición horizontal" campo="focoX" valor={form.focoX} etiquetas={[["Izquierda", 20], ["Centro", 50], ["Derecha", 80]]} />
            <CampoFoco eje="Posición vertical" campo="focoY" valor={form.focoY} etiquetas={[["Superior", 20], ["Centro", 50], ["Inferior", 80]]} />
          </div>
          <div className="pg-field">
            <label className="pg-label" htmlFor="pg-overlay">Oscurecimiento de la imagen: <span className="pg-slider-val">{Math.round((form.overlay ?? 0) * 100)}%</span></label>
            <input id="pg-overlay" className="pg-slider" type="range" min={0} max={100} value={Math.round((form.overlay ?? 0) * 100)} onChange={(e) => set("overlay", Number(e.target.value) / 100)} />
            <p className="pg-hint">Mejora el contraste del texto sobre la imagen. 0% = sin oscurecer.</p>
          </div>
          <Campo label="Autor" value={form.autor} maxLength={150} onChange={(v) => set("autor", v)} />
          <Campo label="Fuente" value={form.fuente} maxLength={200} onChange={(v) => set("fuente", v)} />
          <Campo label="Licencia" value={form.licencia} maxLength={100} onChange={(v) => set("licencia", v)} />
          <Campo label="Atribución" value={form.atribucion} maxLength={300} onChange={(v) => set("atribucion", v)} />
        </>);
      case "LOGIN:textos":
        return (<>
          <Campo label="Título institucional" value={form.tituloInstitucional} maxLength={80} onChange={(v) => set("tituloInstitucional", v)} />
          <Campo label="Frase principal" textarea value={form.frasePrincipal} maxLength={200} onChange={(v) => set("frasePrincipal", v)} />
          <Campo label="Texto complementario" textarea value={form.textoComplementario} maxLength={500} onChange={(v) => set("textoComplementario", v)} />
        </>);
      case "LOGIN:servicios":
        return <ListaServicios />;
      default:
        return <ResumenBloque />;
    }
  };

  if (cargando) {
    return <div className="pg-loading"><RefreshCw size={20} className="pg-spin" /> Cargando gestión del portal…</div>;
  }

  const estado = estadoVisible(seccionActual);
  const meta = SECCIONES.find((s) => s.key === activa);
  const cambios = hayCambiosSinPublicar ? diffSeccion(activa, publicado, borradorServidor) : [];
  const primeraPublicacion = !publicado;

  return (
    <div className="pg">
      <header className="pg-head">
        <div>
          <h2><ClipboardCheck size={20} /> Gestión del Portal</h2>
          <p>Administra el contenido editorial del Portal Público por secciones. Los cambios se guardan como borrador y solo llegan a los visitantes cuando publicas.</p>
        </div>
        <div className="pg-head-badges">
          <div className="pg-head-kpi">Sección activa<strong>{meta?.label || activa}</strong></div>
          <div className="pg-head-kpi">Estado<strong>{estado.txt}</strong></div>
          <div className="pg-head-kpi">Versión publicada<strong>{seccionActual?.version ?? 0}</strong></div>
        </div>
      </header>

      {!storage.disponible && (
        <div className="pg-banner" role="alert">
          <AlertCircle size={18} color={C.naranja} />
          <div>
            <strong>Almacenamiento no persistente.</strong> La subida de imágenes está deshabilitada (tipo actual: {storage.tipo}).
            El contenido editorial puede administrarse, pero no debe declararse listo para producción hasta configurar un almacenamiento persistente.
          </div>
        </div>
      )}
      {storage.disponible && storage.persistente === false && (
        <div className="pg-banner" role="alert">
          <AlertCircle size={18} color={C.naranja} />
          <div>
            <strong>Almacenamiento no persistente (tipo actual: {storage.tipo}).</strong> Las imágenes se guardan en el sistema de archivos
            local y pueden perderse al reiniciar o volver a desplegar. No debe declararse listo para producción hasta configurar un
            almacenamiento persistente.
          </div>
        </div>
      )}

      <div className="pg-layout">
        <PortalSectionNavigation secciones={secciones} activa={activa} onChange={cambiarSeccion} />

        <section className="pg-editor" aria-label={`Editor de la sección ${meta?.label || activa}`}>
          <div className="pg-section-head">
            <h3>{meta?.label || activa} <span className={`pg-badge ${estado.cls}`}>{estado.txt}</span>{hayCambiosSinGuardar ? <span className="pg-badge pg-badge-pendiente">CAMPOS SIN GUARDAR</span> : null}</h3>
            <p className="pg-help">{meta?.desc}</p>
            <div className="pg-meta">
              <span>Versión publicada: <b>{seccionActual?.version ?? 0}</b></span>
              <span>Última publicación: <b>{fmtFecha(seccionActual?.publicadoEn)}</b></span>
              <span>Actualizado por: <b>{seccionActual?.actualizadoPor || "—"}</b></span>
              <span>Cambios pendientes: <b>{hayCambiosSinPublicar ? "Sí" : "No"}</b></span>
            </div>
          </div>

          <div className="pg-acc">
            {(BLOQUES[activa] || []).map((b) => (
              <PortalEditorAccordion key={b.id} id={`${activa}-${b.id}`} title={b.t} sub={b.s} open={!!abiertos[b.id]} onToggle={() => toggle(b.id)}>
                {renderBloque(b.id)}
              </PortalEditorAccordion>
            ))}
          </div>
        </section>

        <PortalPreviewPanel activa={activa} form={form} preview={preview} setPreview={setPreview} />
      </div>

      <div className="pg-actions" role="group" aria-label="Acciones de la sección">
        <div className="pg-actions-status">
          {hayCambiosSinGuardar
            ? <span><span className="pg-strong">Cambios sin guardar</span> · guarda el borrador para conservarlos</span>
            : hayCambiosSinPublicar
              ? <span><span className="pg-strong">Cambios sin publicar</span> · los visitantes ven la versión publicada</span>
              : <span><span className="pg-strong">Sin cambios pendientes</span>{seccionActual?.publicadoEn ? ` · publicado ${fmtFecha(seccionActual.publicadoEn)}` : ""}</span>}
        </div>
        <div className="pg-actions-main">
          <button type="button" className="pg-btn pg-btn-primary" disabled={ocupado || !hayCambiosSinGuardar} onClick={guardar}><Save size={15} /> Guardar borrador</button>
          <button type="button" className="pg-btn pg-btn-ghost" disabled={ocupado || !hayCambiosSinPublicar || hayCambiosSinGuardar} onClick={() => setRevisando(true)}><Eye size={15} /> Revisar cambios</button>
          <button type="button" className="pg-btn pg-btn-dark" disabled={ocupado || !hayCambiosSinPublicar || hayCambiosSinGuardar} onClick={() => setRevisando(true)}><Send size={15} /> Publicar</button>
        </div>
        <div className="pg-actions-second">
          <button type="button" className="pg-btn pg-btn-ghost pg-btn-sm" onClick={abrirHistorial}><History size={14} /> Historial</button>
          <button type="button" className="pg-btn pg-btn-danger pg-btn-sm" disabled={ocupado || seccionActual?.estado === "ARCHIVADO"} onClick={() => setConfirm({
            titulo: "Archivar sección",
            mensaje: `La sección ${meta?.label || activa} dejará de mostrarse en el Portal Público. Podrás restaurarla desde el historial. Esta acción queda registrada.`,
            confirmLabel: "Archivar sección",
            onConfirm: async () => { await accion("archivar", "SECCIÓN ARCHIVADA. Dejará de mostrarse en el Portal Público."); setConfirm(null); },
          })}><Archive size={14} /> Archivar</button>
        </div>
      </div>

      {picker && (
        <PortalImagePicker
          imagenes={imagenes} storage={storage}
          imgForm={imgForm} setImgForm={setImgForm} subiendo={subiendo} onSubir={subirImagen}
          onSeleccionar={aplicarImagen} onCerrar={() => setPicker(null)} seccion={meta?.label || activa}
        />
      )}

      {revisando && (
        <PortalPublishSummary
          onCerrar={() => setRevisando(false)} onConfirmar={publicar}
          seccionLabel={meta?.label || activa} version={seccionActual?.version || 0} cambios={cambios}
          primeraPublicacion={primeraPublicacion} ocupado={ocupado}
        />
      )}

      {verHistorial && (
        <PortalVersionHistory
          historial={historial} publicado={publicado} seccion={meta?.label || activa}
          onRestaurar={restaurarVersion} onCerrar={() => setVerHistorial(false)} ocupado={ocupado}
        />
      )}

      {confirm && (
        <ConfirmDialog
          titulo={confirm.titulo} mensaje={confirm.mensaje} confirmLabel={confirm.confirmLabel}
          onConfirm={() => confirm.onConfirm()} onCancel={() => setConfirm(null)} ocupado={ocupado} danger
        />
      )}
    </div>
  );
}

// ── Vista previa interna ───────────────────────────────────────────────────

function PreviewPortal({ seccion, form }) {
  if (seccion === "HERO") {
    const img = form.imagenFondoUrl;
    return (
      <div className="pg-pv-hero">
        {img ? <img src={img} alt={form.imagenAlt || "Imagen del Hero"} /> : null}
        <div className="pg-pv-overlay" />
        <div className="pg-pv-inner">
          <div className="pg-pv-h">{form.titulo || "Título del Hero"}</div>
          <div className="pg-pv-sub">{form.subtitulo || "Subtítulo de bienvenida"}</div>
          {form.mostrarBus ? <div className="pg-pv-chip" style={{ marginTop: 8, color: "#fff", borderColor: "rgba(255,255,255,.5)" }}>Buscador de bus</div> : null}
        </div>
      </div>
    );
  }

  if (seccion === "DESTINOS") {
    const arr = (form.destinos || []).filter((d) => d.visible !== false);
    return (
      <div className="pg-pv-pad">
        <div style={{ fontWeight: 800, marginBottom: 8, color: C.oscuro }}>Destinos</div>
        {arr.length === 0 ? <div className="pg-empty">Sin destinos visibles.</div> : (
          <div className="pg-pv-grid">
            {arr.map((d, i) => (
              <div className="pg-pv-card" key={i}>
                {d.imagenUrl ? <img src={d.imagenUrl} alt={d.imagenAlt || d.titulo || "Destino"} /> : null}
                <b>{d.titulo || "—"}</b>
                {d.texto ? <span style={{ color: C.medio }}>{d.texto}</span> : null}
              </div>
            ))}
          </div>
        )}
      </div>
    );
  }

  if (seccion === "BENEFICIOS") {
    const arr = (form.beneficios || []).filter((b) => b.visible !== false);
    return (
      <div className="pg-pv-pad">
        {arr.length === 0 ? <div className="pg-empty">Sin beneficios visibles.</div> : arr.map((b, i) => {
          const I = ICONO_MAP[b.icono] || Info;
          return (
            <div className="pg-pv-line" key={i} style={{ display: "flex", gap: 9, alignItems: "flex-start" }}>
              <span style={{ color: C.verde, flexShrink: 0 }}><I size={16} /></span>
              <span>
                <b>{b.titulo || "—"}</b>
                {b.descripcion ? <span>{b.descripcion}</span> : null}
              </span>
            </div>
          );
        })}
      </div>
    );
  }

  if (seccion === "NOSOTROS") {
    return (
      <div className="pg-pv-pad">
        {form.imagenUrl ? <img src={form.imagenUrl} alt={form.imagenAlt || "Nosotros"} style={{ width: "100%", height: 110, objectFit: "cover", borderRadius: 8, marginBottom: 8 }} /> : null}
        <div style={{ fontWeight: 800, marginBottom: 6, color: C.oscuro }}>{form.titulo || "Nosotros"}</div>
        <div style={{ fontSize: 11.5, whiteSpace: "pre-line", color: C.medio, lineHeight: 1.5 }}>{form.descripcion || "Descripción de la empresa."}</div>
      </div>
    );
  }

  if (seccion === "CONTACTO") {
    return (
      <div className="pg-pv-pad">
        <div style={{ fontWeight: 800, marginBottom: 8, color: C.oscuro }}>
          Contacto {form.visible === false ? <span style={{ color: C.rojo, fontSize: 10 }}>(oculto en el Portal)</span> : null}
        </div>
        <div className="pg-summary-row"><span className="k">Teléfono</span><span className="v">{form.telefono || "—"}</span></div>
        <div className="pg-summary-row"><span className="k">Correo</span><span className="v">{form.correo || "—"}</span></div>
        <div className="pg-summary-row"><span className="k">Dirección</span><span className="v">{form.direccion || "—"}</span></div>
        <div className="pg-summary-row"><span className="k">Horario</span><span className="v">{form.horario || "—"}</span></div>
        {(form.redes || []).length > 0 ? <div className="pg-pv-chips" style={{ marginTop: 8 }}>{form.redes.map((r, i) => <span className="pg-pv-chip" key={i}>{r.nombre || "Red"}</span>)}</div> : null}
      </div>
    );
  }

  if (seccion === "FOOTER") {
    return (
      <div className="pg-pv-pad" style={{ background: C.oscuro, color: "#fff", borderRadius: 8 }}>
        <div style={{ fontWeight: 800, marginBottom: 6 }}>INTIWATANA</div>
        <div style={{ fontSize: 11.5, opacity: 0.85 }}>{form.descripcion || "Descripción de la empresa"}</div>
        <div style={{ marginTop: 8, display: "flex", flexWrap: "wrap", gap: 6 }}>
          {(form.enlaces || []).map((e, i) => <span className="pg-pv-chip" key={i} style={{ color: "#fff", borderColor: "rgba(255,255,255,.4)" }}>{e.texto || "enlace"}</span>)}
        </div>
        <div style={{ fontSize: 10, opacity: 0.65, marginTop: 10 }}>{form.infoLegal}</div>
        {form.creditosFotograficos ? <div style={{ fontSize: 9.5, opacity: 0.55, marginTop: 4 }}>{form.creditosFotograficos}</div> : null}
      </div>
    );
  }

  if (seccion === "LOGIN") {
    const img = form.imagenUrl || form.imagenMobileUrl;
    const alpha = form.overlay ?? 0.55;
    const pos = `${form.focoX ?? 50}% ${form.focoY ?? 35}%`;
    return (
      <div style={{ position: "relative", minHeight: 260, borderRadius: 8, overflow: "hidden", background: C.oscuro, color: "#fff", display: "flex", flexDirection: "column", padding: 16, gap: 8 }}>
        {img ? <img src={img} alt={form.imagenAlt || "Imagen del Login"} style={{ position: "absolute", inset: 0, width: "100%", height: "100%", objectFit: "cover", objectPosition: pos }} /> : null}
        <div style={{ position: "absolute", inset: 0, background: `rgba(3, 40, 33, ${alpha})` }} />
        <div style={{ position: "relative", zIndex: 1, display: "flex", flexDirection: "column", gap: 8, height: "100%" }}>
          <strong style={{ fontSize: 15, letterSpacing: "0.06em" }}>{form.tituloInstitucional || "INTIWATANA"}</strong>
          <div style={{ fontSize: 11, opacity: 0.85, whiteSpace: "pre-line" }}>{form.frasePrincipal || "MÁS QUE UN DESTINO,\nES TU HISTORIA"}</div>
          <div style={{ fontSize: 10, opacity: 0.78 }}>{form.textoComplementario || "Reservas, pasajes y encomiendas en un solo lugar."}</div>
          {form.serviciosVisible !== false ? (
            <div style={{ display: "flex", flexDirection: "column", gap: 6, marginTop: 4 }}>
              {(form.servicios || []).filter((s) => s.visible !== false).slice(0, 4).map((s, i) => {
                const I = ICONO_MAP[s.icono] || Info;
                return (
                  <div key={i} style={{ display: "flex", gap: 7, alignItems: "center", fontSize: 10.5, opacity: 0.92 }}>
                    <I size={13} /> <span><b>{s.titulo}</b>{s.subtitulo ? ` · ${s.subtitulo}` : ""}</span>
                  </div>
                );
              })}
            </div>
          ) : null}
          {!img ? <div style={{ fontSize: 9.5, opacity: 0.6, marginTop: "auto" }}>Sin imagen: se usará el respaldo institucional del Login.</div> : null}
        </div>
      </div>
    );
  }

  return null;
}
