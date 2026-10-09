// App.jsx
import { useState, useEffect, useRef, useMemo } from "react";
import { Routes, Route, useNavigate, useSearchParams, Navigate } from "react-router-dom";
import { Landmark, Trees, MountainSnow, Waves, Building2, Coffee, Sun, Mountain, MapPin, Clock, ArrowRight, Search, Calendar, Bus, Briefcase, ChevronRight, Shield, Star, Navigation, FileText, User, Navigation2, Package, CheckCircle2, AlertTriangle, Undo2, Factory, Mailbox, Phone, Ticket, Menu, X } from 'lucide-react';
import "./App.css";
import { AuthProvider } from "./context/AuthContext";
import { useAuth } from "./context/useAuth";
import Login, { RegisterForm, LeftPanel } from "./components/Login";
import AdminDashboard from "./components/AdminDashboard";
import ClienteDashboard from "./components/ClienteDashboard";
import ChoferDashboard from "./components/ChoferDashboard";
import PasajesPage from "./components/PasajesPage";
import LogoSVG from "./components/LogoSVG";

import { DESTINOS } from "./data/destinos";

// ── Modal horarios ─────────────────────────────────────
function ModalHorarios({ destino, onClose, onReservar }) {
  if (!destino) return null;
  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <div className="modal-badge">{destino.EmojiIcon && <destino.EmojiIcon />}</div>
          <div>
            <div className="modal-title">{destino.nombre}</div>
            <div className="modal-subtitle">{destino.region} · Valle andino</div>
          </div>
          <button className="modal-close" onClick={onClose}>✕</button>
        </div>
        <div className="modal-body">
          <div className="modal-info-row">
            <div className="modal-chip"><Clock size={16} /> Duración: <strong>{destino.duracion}</strong></div>
            <div className="modal-chip"><Navigation size={16} /> Distancia: <strong>{destino.distancia}</strong></div>
          </div>
          <hr className="modal-divider" />
          <div className="modal-section-label">Horarios de salida — Ayacucho → {destino.nombre}</div>
          <div className="horarios-grid">
            {destino.horarios.map((h, i) => (
              <div className="horario-pill" key={i}>
                <span className="hora">{h.hora}</span>
                <span className="hora-tipo">{h.tipo}</span>
                <span className="hora-precio">{h.precio}</span>
              </div>
            ))}
          </div>
          <hr className="modal-divider" />
          <div className="modal-section-label">Terminales</div>
          <div className="terminales-list">
            {destino.terminales.map((t, i) => (
              <div className="terminal-row" key={i}>
                <div className="terminal-icon">{t.tipo === "ORIGEN" ? <Bus size={18} /> : <MapPin size={18} />}</div>
                <div className="terminal-info">
                  <div className="terminal-nombre">{t.nombre}</div>
                  <div className="terminal-dir">{t.dir}</div>
                  <div className="terminal-horario">{t.horario}</div>
                </div>
                <span className="terminal-badge">{t.tipo}</span>
              </div>
            ))}
          </div>
          <button
            className="btn-reservar"
            onClick={() => { onClose(); onReservar(destino); }}
          >
            <Ticket size={20} /> Reservar pasaje en línea
          </button>
          <p className="modal-nota">* Horarios y precios referenciales. Confirmar disponibilidad en terminal.</p>
        </div>
      </div>
    </div>
  );
}

// ── Página pública de seguimiento ──────────────────────
const TRACK_ESTADOS = {
  SOLICITUD_PENDIENTE:{ label:"Solicitud registrada", icon:<Package size={18}/>, color:"#b35c00", bg:"rgba(230,126,34,0.15)" },
  RECIBIDO:      { label:"Recibido",       icon:<Package size={18}/>, color:"#b8860b", bg:"rgba(245,197,24,0.15)" },
  EN_ALMACEN:    { label:"En almacén",     icon:<Factory size={18}/>, color:"#127369", bg:"rgba(18,115,105,0.15)" },
  CARGADO_EN_BUS:{ label:"Cargado en bus", icon:<Bus size={18}/>,     color:"#127369", bg:"rgba(18,115,105,0.15)" },
  EN_TRANSITO:   { label:"En tránsito",    icon:<Bus size={18}/>,     color:"#127369", bg:"rgba(18,115,105,0.15)" },
  LLEGADO_A_DESTINO:{ label:"Llegó a destino", icon:<MapPin size={18}/>, color:"#1d4ed8", bg:"rgba(59,130,246,0.15)" },
  EN_DESTINO:    { label:"En destino",     icon:<MapPin size={18}/>,  color:"#1d4ed8", bg:"rgba(59,130,246,0.15)" },
  LISTO_ENTREGA: { label:"Listo para entrega", icon:<Mailbox size={18}/>, color:"#127369", bg:"rgba(18,115,105,0.15)" },
  ENTREGADO:     { label:"Entregado",      icon:<CheckCircle2 size={18}/>, color:"#15803d", bg:"rgba(34,197,94,0.15)" },
  DEVUELTO:      { label:"Devuelto",       icon:<Undo2 size={18}/>,   color:"#7e22ce", bg:"rgba(168,85,247,0.15)" },
  PERDIDO:       { label:"Perdido",        icon:<AlertTriangle size={18}/>, color:"#dc2626", bg:"rgba(239,68,68,0.15)" },
};
const TRACK_PASOS = ["RECIBIDO","EN_ALMACEN","CARGADO_EN_BUS","EN_TRANSITO","LLEGADO_A_DESTINO","EN_DESTINO","LISTO_ENTREGA","ENTREGADO"];
const GUIA_REGEX = /^GUI-\d{8}-\d{6}$/;
function fmtTrackFecha(iso) {
  if (!iso) return "—";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return "—";
  return d.toLocaleString("es-PE", { dateStyle:"medium", timeStyle:"short" });
}

function SeguimientoPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const codigoParam = (searchParams.get("codigo") || "").trim().toUpperCase();
  const [codigo,    setCodigo]    = useState(codigoParam);
  const [resultado, setResultado] = useState(null);
  const [loading,   setLoading]   = useState(false);
  const [error,     setError]     = useState("");
  const autoConsultado = useRef(false);

  const consultar = async (e, codigoOverride) => {
    e?.preventDefault?.();
    const guia = (codigoOverride ?? codigo).trim().toUpperCase();
    if (!guia) { setError("Ingresa el número de guía."); setResultado(null); return; }
    if (!GUIA_REGEX.test(guia)) { setError("Revisa el número de guía ingresado."); setResultado(null); return; }
    setError(""); setResultado(null); setLoading(true);
    try {
      const res = await fetch(`/api/v1/encomiendas/tracking/${encodeURIComponent(guia)}`);
      if (res.ok) {
        setResultado(await res.json());
      } else if (res.status === 404) {
        setError("No encontramos una encomienda con el número de guía ingresado.");
      } else {
        setError("No se pudo consultar el estado. Intenta nuevamente.");
      }
    } catch {
      setError("No se pudo conectar con el servicio de seguimiento.");
    } finally {
      setLoading(false);
    }
  };

  // Si se llega desde el portal con ?codigo=..., consulta automáticamente.
  useEffect(() => {
    if (codigoParam && !autoConsultado.current) {
      autoConsultado.current = true;
      consultar(null, codigoParam);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [codigoParam]);

  const estado    = resultado?.estado || resultado?.ultimoEstado;
  const cfg       = TRACK_ESTADOS[estado] || { label: estado, icon:"•", color:"#8AA6A3", bg:"rgba(138,166,163,0.15)" };
  const idxActual = TRACK_PASOS.indexOf(estado);
  const movimientos = resultado?.movimientos || [];
  const ultimo    = movimientos.length ? movimientos[movimientos.length-1]?.fechaHora : resultado?.fechaRegistro;

  return (
    <div className="seg-wrap">
      <nav className="seg-nav">
        <div className="nav-logo" style={{ cursor:"pointer" }} onClick={() => navigate("/")}>
          <LogoSVG />
          <div className="nav-brand">Emp. de Transportes<span>INTIWATANA S.R.L.</span></div>
        </div>
        <button className="btn-admin-nav" onClick={() => navigate("/")}>Volver al inicio</button>
      </nav>

      <main className="seg-main">
        <header className="seg-header">
          <span className="seg-badge"><Package size={20}/></span>
          <h1>Seguimiento de encomiendas</h1>
          <p>Consulta en línea el estado y la ubicación actual de tu encomienda.</p>
        </header>

        <form className="seg-form" onSubmit={consultar}>
          <label htmlFor="guia-input">Número de guía</label>
          <div className="seg-form-row">
            <input id="guia-input" type="text" value={codigo} onChange={(e) => setCodigo(e.target.value)}
              placeholder="Ej. GUI-2026-001234" autoComplete="off" spellCheck="false"
              aria-invalid={!!error} aria-describedby={error ? "guia-error" : undefined}/>
            <button type="submit" className="btn-reservar" style={{ width:"auto", marginTop:0, padding:"0 1.4rem" }} disabled={loading}>
              {loading ? "Consultando..." : "Consultar estado"}
            </button>
          </div>
          <span className="seg-hint">Formato: GUI-AAAAMMDD-000001</span>
          {error && <p id="guia-error" role="alert" className="seg-error">{error}</p>}
        </form>

        {loading && <div className="seg-loading" role="status" aria-live="polite">Consultando estado de la encomienda...</div>}

        {resultado && !loading && (
          <section className="seg-result" aria-label="Resultado del seguimiento">
            <div className="seg-estado" style={{ background:cfg.bg, color:cfg.color }}>{cfg.icon} {cfg.label}</div>

            <div className="track-progress">
              {TRACK_PASOS.map((paso,i) => {
                const done = i <= idxActual && idxActual >= 0;
                const current = i === idxActual;
                const p = TRACK_ESTADOS[paso] || {};
                return (
                  <div className={`track-step ${done?"done":""} ${current?"current":""}`} key={paso}>
                    <div className="track-dot">{done ? (p.icon || "✓") : "○"}</div>
                    <div className="track-label">{p.label || paso.replace(/_/g," ")}</div>
                    {i < TRACK_PASOS.length-1 && <div className={`track-line ${done && i < idxActual ? "done" : ""}`}/>}
                  </div>
                );
              })}
            </div>

            <h2 className="seg-section-title">Datos generales</h2>
            <div className="track-info-grid">
              {[
                ["N° de guía",   resultado.numeroGuia || "—"],
                ["Contenido",    resultado.descripcionContenido || "—"],
                ["Peso",         resultado.pesoKg != null ? `${resultado.pesoKg} kg` : "—"],
                ["Origen",       resultado.sucursalOrigen  ? `${resultado.sucursalOrigen.nombre} · ${resultado.sucursalOrigen.ciudad}`   : "—"],
                ["Destino",      resultado.sucursalDestino ? `${resultado.sucursalDestino.nombre} · ${resultado.sucursalDestino.ciudad}` : "—"],
              ].map(([lbl,val]) => (
                <div className="track-info-item" key={lbl}>
                  <span className="track-info-label">{lbl}</span>
                  <span className="track-info-value">{val}</span>
                </div>
              ))}
            </div>

            <h2 className="seg-section-title">Historial de movimientos</h2>
            {movimientos.length === 0 ? (
              <p className="seg-empty">Aún no hay movimientos registrados para esta encomienda.</p>
            ) : (
              <ol className="seg-timeline">
                {movimientos.map((m,i) => {
                  const mc = TRACK_ESTADOS[m.estadoNuevo] || { label:(m.estadoNuevo || "").replace(/_/g," ") };
                  return (
                    <li key={i} className="seg-timeline-item">
                      <span className="seg-timeline-dot"/>
                      <div>
                        <div className="seg-timeline-estado">{mc.label}</div>
                        <div className="seg-timeline-meta">{fmtTrackFecha(m.fechaHora)} · {m.sucursalActual || "En tránsito"}</div>
                        {m.observacion && <div className="seg-timeline-obs">{m.observacion}</div>}
                      </div>
                    </li>
                  );
                })}
              </ol>
            )}

            <p className="seg-updated">Última actualización: {fmtTrackFecha(ultimo)}</p>
          </section>
        )}
      </main>
    </div>
  );
}

// ── Título del hero: preserva el realce de "INTIWATANA" aunque el texto cambie.
function TituloHero({ texto }) {
  const t = texto || "Viaja con INTIWATANA";
  const idx = t.toUpperCase().indexOf("INTIWATANA");
  if (idx < 0) return <>{t}</>;
  return <>{t.slice(0, idx)}<em>INTIWATANA</em>{t.slice(idx + 10)}</>;
}

const ICONO_PORTAL = {
  ticket: <Ticket size={22} />, package: <Package size={22} />, mappin: <MapPin size={22} />,
  shield: <Shield size={22} />, clock: <Clock size={22} />, user: <User size={22} />,
  bus: <Bus size={22} />, route: <Navigation size={22} />, heart: <Star size={22} />,
  star: <Star size={22} />, check: <CheckCircle2 size={22} />, info: <FileText size={22} />,
  phone: <Phone size={22} />, mail: <FileText size={22} />,
};

// ── Landing pública ────────────────────────────────────
function LandingPage({ onIrLogin }) {
  const [origen,        setOrigen]        = useState("Ayacucho");
  const [destinoSel,    setDestinoSel]    = useState(null);
  const [fecha,         setFecha]         = useState("");
  const [pasajeros,     setPasajeros]     = useState(1);
  const [modalDestino,  setModalDestino]  = useState(null);
  const [menuAbierto,   setMenuAbierto]   = useState(false);
  const [tabBusqueda,   setTabBusqueda]   = useState("pasajes");
  const [guia,          setGuia]          = useState("");
  const { session } = useAuth();
  const navigate = useNavigate();

  // Contenido editorial publicado. Si falla o no hay, se usan los valores de
  // respaldo locales: una falla editorial NUNCA rompe el portal ni sus flujos.
  const [portal, setPortal] = useState({});
  useEffect(() => {
    let cancel = false;
    fetch("/api/v1/portal/publico")
      .then((r) => (r.ok ? r.json() : null))
      .then((d) => { if (!cancel && d?.data) setPortal(d.data); })
      .catch(() => {});
    return () => { cancel = true; };
  }, []);

  const hero = portal.HERO || {};
  const destinosEd = Array.isArray(portal.DESTINOS?.destinos) ? portal.DESTINOS.destinos : null;
  const beneficiosEd = Array.isArray(portal.BENEFICIOS?.beneficios) ? portal.BENEFICIOS.beneficios : null;
  const nosotrosEd = portal.NOSOTROS || null;
  const footerEd = portal.FOOTER || null;
  const contactoEd = portal.CONTACTO || null;

  const destinosBase = useMemo(() => {
    if (!destinosEd) return DESTINOS;
    return DESTINOS.map((d, i) => {
      const e = destinosEd[i];
      if (!e) return d;
      return {
        ...d,
        _visible: e.visible,
        _titulo: e.titulo || d.nombre,
        _imagen: e.imagenUrl || d.imagen,
        _orden: typeof e.orden === "number" ? e.orden : i,
      };
    })
      .filter((d) => d._visible !== false)
      .sort((a, b) => (a._orden ?? 0) - (b._orden ?? 0));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [portal]);

  const beneficios = useMemo(() => {
    if (!beneficiosEd) {
      return [
        { icon: <Ticket size={22} />,  name: "Compra organizada",           desc: "Consulta rutas, horarios y asientos antes de viajar." },
        { icon: <Package size={22} />, name: "Encomiendas con seguimiento", desc: "Registra tu solicitud y sigue tu envío en línea." },
        { icon: <MapPin size={22} />,  name: "Atención en agencia",         desc: "El personal te atiende en terminal para cada servicio." },
        { icon: <Shield size={22} />,  name: "Información clara",           desc: "Tarifas y condiciones confirmadas por personal autorizado." },
      ];
    }
    return beneficiosEd
      .filter((b) => b.visible !== false)
      .map((b) => ({ icon: ICONO_PORTAL[b.icono] || <Star size={22} />, name: b.titulo, desc: b.descripcion }));
  }, [portal]); // eslint-disable-line react-hooks/exhaustive-deps

  const nombreDestino = (d) => d._titulo || d.nombre;
  const imagenDestino = (d) => d._imagen || d.imagen;

  const filtrados = destinoSel
    ? destinosBase.filter((d) => nombreDestino(d).toLowerCase().includes(destinoSel.toLowerCase()))
    : destinosBase;

  const esAdmin = session && (
    session.rol === "ROLE_ADMIN" ||
    session.rol === "ROLE_CAJERO" ||
    session.rol === "ROLE_VENDEDOR" ||
    session.rol === "ROLE_OPERADOR_ENCOMIENDAS" ||
    session.rol === "ROLE_CHOFER"
  );
  const esCliente = session?.rol === "ROLE_CLIENTE";

  const handleReservarDesdeModal = (destino) => {
    navigate(`/pasajes?destino=${encodeURIComponent(destino.nombre)}`);
  };

  const buscarPasajes = (e) => {
    e?.preventDefault?.();
    navigate(`/pasajes?origen=${encodeURIComponent(origen)}&destino=${encodeURIComponent(destinoSel || "")}&fecha=${fecha}&pasajeros=${pasajeros}`);
  };

  const consultarGuia = (e) => {
    e?.preventDefault?.();
    const codigo = guia.trim().toUpperCase();
    navigate(`/encomiendas/seguimiento${codigo ? `?codigo=${encodeURIComponent(codigo)}` : ""}`);
  };

  return (
    <div className="landing-root">
      {/* ── NAVBAR ── */}
      <nav className="nav">
        <div className="nav-logo" style={{ cursor:"pointer" }} onClick={() => navigate("/")}>
          <LogoSVG />
          <div className="nav-brand">
            Emp. de Transportes
            <span>INTIWATANA S.R.L.</span>
          </div>
        </div>

        <button
          className="nav-hamburguesa"
          aria-label={menuAbierto ? "Cerrar menú" : "Abrir menú"}
          aria-expanded={menuAbierto}
          aria-controls="menu-principal"
          onClick={() => setMenuAbierto((v) => !v)}
        >
          {menuAbierto ? <X size={22} /> : <Menu size={22} />}
        </button>

        <ul id="menu-principal" className={`nav-links ${menuAbierto ? "open" : ""}`}>
          <li><a href="#inicio" onClick={() => setMenuAbierto(false)}>Inicio</a></li>
          <li><a href="#destinos" onClick={() => setMenuAbierto(false)}>Destinos</a></li>
          <li><a href="#nosotros" onClick={() => setMenuAbierto(false)}>Nosotros</a></li>
          <li><a href="/pasajes?tab=encomiendas" onClick={(e) => { e.preventDefault(); setMenuAbierto(false); navigate("/pasajes?tab=encomiendas"); }}>Encomiendas</a></li>
          <li><a href="#contacto" onClick={() => setMenuAbierto(false)}>Contacto</a></li>
        </ul>

        <div className="nav-acciones">
          {/* Reservar / Mis reservas: nunca fuerza login */}
          <button
            className="btn-terminal"
            style={{ borderRadius:8, padding:"7px 14px", fontSize:13 }}
            onClick={() => { setMenuAbierto(false); navigate(esCliente ? "/cliente" : "/pasajes"); }}
          >
            <Ticket size={18} style={{ marginRight: 6 }} /> {esCliente ? "Mis reservas" : "Reservar pasaje"}
          </button>

          {session ? (
            /* Usuario logueado */
            <button
              className="nav-usuario-btn"
              onClick={() => navigate(esAdmin ? "/admin" : "/cliente")}
            >
              {session.foto ? (
                <img src={session.foto} alt="avatar" className="nav-avatar-img" />
              ) : (
                <div className="nav-avatar-iniciales">
                  {(session.username || "?").slice(0, 2).toUpperCase()}
                </div>
              )}
              <span className="nav-usuario-nombre">
                {esAdmin ? "Panel Admin" : session.username}
              </span>
            </button>
          ) : (
            /* Sin sesión */
            <>
              <button className="btn-admin-nav" onClick={onIrLogin}>
                Iniciar sesión
              </button>
              <button
                className="btn-admin-nav btn-registrarse"
                onClick={() => navigate("/registrar")}
              >
                Registrarse
              </button>
            </>
          )}
        </div>
      </nav>

      {/* ── HERO ── */}
      <section
        className="hero"
        id="inicio"
        style={hero.imagenFondoUrl ? { "--hero-image": `url("${hero.imagenFondoUrl}")` } : undefined}
      >
        <div
          className="hero-bg-pattern"
          role="img"
          aria-label={hero.imagenAlt || "Paisaje andino de la región de Ayacucho"}
        />
        <svg className="hero-mountain-svg" viewBox="0 0 1440 140" preserveAspectRatio="none" aria-hidden="true" focusable="false">
          <polygon
            points="0,140 0,100 200,20 400,80 600,10 800,70 1000,15 1200,60 1440,30 1440,140"
            fill="rgba(7,32,30,0.35)"
          />
        </svg>
        <div className="hero-content">
          {hero.titulo && hero.titulo !== "Viaja con INTIWATANA" ? (
            <h1><TituloHero texto={hero.titulo} /></h1>
          ) : (
            <h1>Viaja con<br /><em>INTIWATANA</em></h1>
          )}
          <p>{hero.subtitulo || "Conectamos destinos con seguridad, comodidad y atención cercana."}</p>
          {hero.mostrarBus !== false && (
          <div className="hero-search">
            <div className="hero-search-tabs" role="tablist" aria-label="Tipo de consulta">
              {[
                { key: "pasajes", label: "Pasajes" },
                { key: "encomiendas", label: "Encomiendas" },
                { key: "seguimiento", label: "Seguimiento" },
              ].map((t) => (
                <button
                  key={t.key}
                  type="button"
                  role="tab"
                  aria-selected={tabBusqueda === t.key}
                  className={`hero-search-tab ${tabBusqueda === t.key ? "active" : ""}`}
                  onClick={() => setTabBusqueda(t.key)}
                >
                  {t.label}
                </button>
              ))}
            </div>

            {tabBusqueda === "pasajes" && (
              <form className="search-box" onSubmit={buscarPasajes}>
                <select aria-label="Origen" value={origen} onChange={(e) => setOrigen(e.target.value)}>
                  <option value="Ayacucho">Ayacucho</option>
                  <option value="Abancay">Abancay</option>
                  <option value="Arequipa">Arequipa</option>
                </select>
                <div className="search-divider" />
                <select aria-label="Destino" value={destinoSel || ""} onChange={(e) => setDestinoSel(e.target.value || null)}>
                  <option value="">Todos los destinos</option>
                  {destinosBase.map((d) => (
                    <option key={d.nombre} value={d.nombre}>{nombreDestino(d)}</option>
                  ))}
                </select>
                <div className="search-divider" />
                <input
                  type="date"
                  aria-label="Fecha de ida"
                  value={fecha}
                  onChange={(e) => setFecha(e.target.value)}
                  title="Fecha de ida"
                  className="search-fecha"
                />
                <div className="search-divider" />
                <select
                  aria-label="Pasajeros"
                  className="search-pasajeros"
                  value={pasajeros}
                  onChange={(e) => setPasajeros(Number(e.target.value))}
                >
                  {[1, 2, 3, 4, 5, 6].map((n) => (
                    <option key={n} value={n}>{n} {n === 1 ? "pasajero" : "pasajeros"}</option>
                  ))}
                </select>
                <button type="submit" className="btn-buscar"><Search size={20} /> Buscar viajes</button>
              </form>
            )}

            {tabBusqueda === "encomiendas" && (
              <div className="search-box search-box-panel">
                <div className="search-panel-text">
                  <Package size={22} />
                  <div>
                    <strong>Envía o rastrea tus encomiendas</strong>
                    <span>Registro en línea con seguimiento en tiempo real.</span>
                  </div>
                </div>
                <button type="button" className="btn-buscar" onClick={() => navigate("/pasajes?tab=encomiendas")}>Solicitar envío</button>
              </div>
            )}

            {tabBusqueda === "seguimiento" && (
              <form className="search-box search-box-panel" onSubmit={consultarGuia}>
                <div className="search-panel-input">
                  <Package size={20} />
                  <input
                    type="text"
                    value={guia}
                    onChange={(e) => setGuia(e.target.value.toUpperCase())}
                    placeholder="N° de guía (Ej. GUI-2026-001234)"
                    aria-label="Número de guía"
                    autoComplete="off"
                    spellCheck="false"
                  />
                </div>
                <button type="submit" className="btn-buscar">Consultar</button>
              </form>
            )}
          </div>
          )}
        </div>
      </section>

      {/* ── BENEFICIOS (cualitativos, sin cifras no verificables) ── */}
      <div className="beneficios-bar">
        <h2>Confianza, cercanía y servicio</h2>
        <div className="beneficios-grid">
          {beneficios.map((b) => (
            <div className="beneficio" key={b.name}>
              <span className="beneficio-icon">{b.icon}</span>
              <span className="beneficio-name">{b.name}</span>
              <p>{b.desc}</p>
            </div>
          ))}
        </div>
      </div>

      {/* ── ACCESOS RÁPIDOS ── */}
      <div className="acceso-rapido">
        <button className="btn-acceso" onClick={() => navigate("/encomiendas/seguimiento")}>
          <span><Package size={20} /></span> Rastrear encomienda
        </button>
        <button className="btn-acceso" onClick={() => navigate("/pasajes")}>
          <span><Ticket size={20} /></span> Comprar pasaje
        </button>
        {!session && (
          <button className="btn-acceso" onClick={() => navigate("/registrar")}>
            <span><FileText size={20} /></span> Crear cuenta
          </button>
        )}
        <button
          className="btn-acceso"
          onClick={() => document.getElementById("contacto")?.scrollIntoView({ behavior:"smooth" })}
        >
          <span><Phone size={20} /></span> Contacto
        </button>
      </div>

      {/* ── DESTINOS ── */}
      <div className="section" id="destinos">
        <h2 className="section-title">Destinos</h2>
        <p className="section-subtitle">Elige tu destino y consulta los horarios disponibles</p>
        <div className="destinos-grid">
          {filtrados.map((d) => (
            <div className="destino-card" key={d.nombre}>
              <div className="destino-img-wrapper">
                <img
                  src={imagenDestino(d)}
                  alt={nombreDestino(d)}
                  className="destino-img"
                  onError={(e) => {
                    e.target.style.display = "none";
                    e.target.parentNode.style.background = `${d.color}33`;
                  }}
                />
                {d.EmojiIcon && <span className="destino-emoji-overlay"><d.EmojiIcon /></span>}
              </div>
              <div className="destino-body">
                <div className="destino-name">{nombreDestino(d)}</div>
                <button className="btn-terminal" onClick={() => setModalDestino(d)}>
                  Ver horarios
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* ── POR QUÉ VIAJAR ── */}
      <section className="porque" id="porque">
        <h2>Por qué viajar con INTIWATANA</h2>
        <p>
          Servicio de transporte interprovincial desde Ayacucho, con atención en agencia
          y acompañamiento en cada etapa del viaje.
        </p>
        <div className="valores">
          {[
            { icon: <Shield size={28} />,  name: "Compra segura",  desc: "Reserva tus pasajes dentro del portal oficial." },
            { icon: <Clock size={28} />,   name: "Horarios claros", desc: "Programación de salidas publicada por ruta." },
            { icon: <User size={28} />,    name: "Confort",        desc: "Elige tu asiento al momento de reservar." },
            { icon: <MapPin size={28} />,  name: "Cobertura",      desc: "Rutas hacia destinos de la región y provincias vecinas." },
            { icon: <Package size={28} />, name: "Encomiendas",    desc: "Seguimiento en línea por número de guía." },
          ].map((v) => (
            <div className="valor" key={v.name}>
              <span className="valor-icon">{v.icon}</span>
              <span className="valor-name">{v.name}</span>
              <p>{v.desc}</p>
            </div>
          ))}
        </div>
      </section>

      {/* ── NOSOTROS ── */}
      <section className="nosotros-info" id="nosotros">
        <h2>{nosotrosEd?.titulo || "Nosotros"}</h2>
        {(nosotrosEd?.descripcion ||
          "Empresa de Transportes INTIWATANA S.R.L. es una empresa ayacuchana de transporte " +
          "interprovincial de pasajeros y envío de encomiendas. Atendemos desde nuestro terminal " +
          "en Av. Cusco 362, Ayacucho, y contamos con agencias en las ciudades de destino.\n\n" +
          "Ofrecemos compra de pasajes en línea, consulta de horarios y seguimiento de encomiendas. " +
          "Las tarifas y condiciones se confirman con el personal autorizado en agencia.")
          .split("\n\n").map((par, i) => <p key={i}>{par}</p>)}
      </section>

      {/* ── FOOTER ── */}
      <footer id="contacto">
        <div className="footer-inner">
          <div>
            <div className="footer-logo">
              <LogoSVG width={36} height={36} />
              <strong>INTIWATANA S.R.L.</strong>
            </div>
            <p style={{ color:"var(--gris-verde)", fontSize:13, marginTop:6 }}>
              {footerEd?.descripcion || "Empresa de Transportes · Ayacucho, Perú"}
            </p>
            <p className="footer-contact">
              {contactoEd && contactoEd.visible !== false && (contactoEd.direccion || contactoEd.telefono)
                ? [contactoEd.direccion && `Terminal: ${contactoEd.direccion}`, contactoEd.telefono && `Tel: ${contactoEd.telefono}`]
                    .filter(Boolean).join(" · ")
                : "Terminal: Av. Cusco 362, Ayacucho"}
            </p>
          </div>
          <div className="footer-links">
            <h3 className="footer-heading">Servicios</h3>
            <ul>
              <li><a href="#destinos">Destinos y horarios</a></li>
              <li>
                <a href="#" onClick={(e) => { e.preventDefault(); navigate("/encomiendas/seguimiento"); }}>
                  Rastrear encomienda
                </a>
              </li>
              <li><a href="#nosotros">Sobre nosotros</a></li>
            </ul>
          </div>
          <div className="footer-links">
            <h3 className="footer-heading">Mi cuenta</h3>
            <ul>
              {session ? (
                <li>
                  <a href="#" onClick={(e) => { e.preventDefault(); navigate(esAdmin ? "/admin" : "/cliente"); }}>
                    {esAdmin ? "Panel Admin" : "Mi panel"}
                  </a>
                </li>
              ) : (
                <>
                  <li><a href="#" onClick={(e) => { e.preventDefault(); navigate("/registrar"); }}>Registrarse</a></li>
                  <li><a href="#" onClick={(e) => { e.preventDefault(); onIrLogin(); }}>Iniciar sesión</a></li>
                </>
              )}
            </ul>
          </div>
        </div>
        <div className="footer-bottom">
          © 2026 Empresa de Transportes INTIWATANA S.R.L. · Todos los derechos reservados
        </div>
      </footer>

      {/* ── MODALES ── */}
      <ModalHorarios destino={modalDestino} onClose={() => setModalDestino(null)} onReservar={handleReservarDesdeModal} />
    </div>
  );
}

// ── Protección de rutas ────────────────────────────────
function RutaProtegida({ children, roles }) {
  const { session } = useAuth();
  if (!session) return <Navigate to="/login" replace />;
  if (roles && !roles.includes(session.rol)) {
    if (session.rol === "ROLE_CLIENTE") return <Navigate to="/pasajes" replace />;
    if (session.rol === "ROLE_CHOFER") return <Navigate to="/chofer" replace />;
    if (["ROLE_ADMIN", "ROLE_CAJERO", "ROLE_VENDEDOR", "ROLE_OPERADOR_ENCOMIENDAS"].includes(session.rol)) return <Navigate to="/admin" replace />;
    return <Navigate to="/" replace />;
  }
  return children;
}

// ── Router interno ─────────────────────────────────────
function AppRouter() {
  const { session } = useAuth();
  const navigate = useNavigate();

  return (
    <Routes>
      {/* Landing pública */}
      <Route path="/" element={
        <LandingPage onIrLogin={() => navigate("/login")} />
      } />

      {/* Login */}
      <Route path="/login" element={
        session ? (
          session.rol === "ROLE_CLIENTE" ? <Navigate to="/pasajes" replace /> :
          session.rol === "ROLE_CHOFER"  ? <Navigate to="/chofer" replace /> :
          <Navigate to="/admin" replace />
        ) : <Login />
      } />

      {/* Registro */}
      <Route path="/registrar" element={
        session ? <Navigate to="/pasajes" replace /> : (
          <div className="login-page">
            <div className="login-wrap">
              <LeftPanel />
              <div className="login-right">
                <div className="login-box" style={{ maxWidth: 460 }}>
                  <RegisterForm onVolver={() => navigate("/login")} />
                </div>
              </div>
            </div>
          </div>
        )
      } />

      {/* Comprar pasajes */}
      <Route path="/pasajes" element={<PasajesPage />} />

      {/* Seguimiento público de encomiendas */}
      <Route path="/encomiendas/seguimiento" element={<SeguimientoPage />} />

      {/* Panel admin */}
      <Route path="/admin" element={
        <RutaProtegida roles={["ROLE_ADMIN", "ROLE_CAJERO", "ROLE_VENDEDOR", "ROLE_OPERADOR_ENCOMIENDAS"]}>
          <AdminDashboard />
        </RutaProtegida>
      } />

      {/* Panel cliente */}
      <Route path="/cliente" element={
        <RutaProtegida roles={["ROLE_CLIENTE"]}>
          <ClienteDashboard />
        </RutaProtegida>
      } />

      {/* Panel chofer (con sus vistas hijas: /chofer/pasajeros, /chofer/qr, /chofer/perfil) */}
      <Route path="/chofer/*" element={
        <RutaProtegida roles={["ROLE_CHOFER"]}>
          <ChoferDashboard />
        </RutaProtegida>
      } />

      {/* Ruta no encontrada */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <AppRouter />
    </AuthProvider>
  );
}