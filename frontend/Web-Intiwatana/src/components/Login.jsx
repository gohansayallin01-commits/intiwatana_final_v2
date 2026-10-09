import { useState, useEffect } from "react";
import { useNavigate, useSearchParams, Link } from "react-router-dom";
import { useAuth } from "../context/useAuth";
import { esSesionVencida, limpiarSesionVencida } from "../context/sesionVencida";
import LogoSVG from "./LogoSVG";
import {
  User, Lock, AlertCircle, ArrowRight, CheckCircle, Eye, EyeOff,
  Ticket, Bus, Package, MapPin,
} from "lucide-react";

const ICONOS_SERVICIO = { ticket: Ticket, bus: Bus, package: Package, mappin: MapPin };

const LOGIN_FALLBACK = {
  tituloInstitucional: "INTIWATANA",
  frasePrincipal: "MÁS QUE UN DESTINO,\nES TU HISTORIA",
  textoComplementario:
    "Reservas, pasajes y encomiendas en un solo lugar. Viaja con seguridad, comodidad y confianza.",
  imagenUrl: "",
  imagenMobileUrl: "",
  imagenAlt: "Imagen principal del Login de Transportes INTIWATANA",
  focoX: 50,
  focoY: 35,
  overlay: 0.55,
  serviciosVisible: true,
  servicios: [
    { icono: "ticket", titulo: "Pasajes y reservas", subtitulo: "Compra, elige y reserva tu asiento en línea." },
    { icono: "bus", titulo: "Selección de asientos", subtitulo: "Elige tu asiento y viaja con comodidad." },
    { icono: "package", titulo: "Encomiendas y tracking", subtitulo: "Registra tu envío y sigue tu guía." },
    { icono: "mappin", titulo: "Destinos y horarios", subtitulo: "Horarios, rutas y destinos con atención cercana." },
  ],
};

export function LeftPanel() {
  // Contenido editorial PUBLICADO de la sección LOGIN. Si no hay o falla, se
  // usa el respaldo local: una falla editorial NUNCA bloquea el Login.
  const [login, setLogin] = useState(null);
  // La imagen administrable solo se usa si es una fotografía real. Un
  // placeholder plano (imagen de un solo color) o una imagen rota cae al
  // respaldo institucional local, de modo que el Login nunca queda en verde plano.
  const [fotoValidada, setFotoValidada] = useState({ url: null, ok: false });
  useEffect(() => {
    let cancel = false;
    fetch("/api/v1/portal/publico")
      .then((r) => (r.ok ? r.json() : null))
      .then((d) => { if (!cancel && d?.data?.LOGIN) setLogin(d.data.LOGIN); })
      .catch(() => {});
    return () => { cancel = true; };
  }, []);

  const ed = login || LOGIN_FALLBACK;

  useEffect(() => {
    const url = ed.imagenUrl;
    if (!url) return;
    let cancel = false;
    const img = new Image();
    img.crossOrigin = "anonymous";
    img.onload = () => {
      if (cancel) return;
      try {
        const W = Math.max(1, Math.min(img.naturalWidth || 1, 80));
        const H = Math.max(1, Math.min(img.naturalHeight || 1, 80));
        const canvas = document.createElement("canvas");
        canvas.width = W; canvas.height = H;
        const ctx = canvas.getContext("2d", { willReadFrequently: true });
        ctx.drawImage(img, 0, 0, W, H);
        const d = ctx.getImageData(0, 0, W, H).data;
        // Un placeholder (rectángulo de un solo color, p. ej. verde plano) tiene
        // pocos colores distintos y uno dominante. Una fotografía real tiene
        // muchos colores repartidos.
        const buckets = new Map();
        for (let i = 0; i < d.length; i += 4) {
          const key = ((d[i] >> 4) << 8) | ((d[i + 1] >> 4) << 4) | (d[i + 2] >> 4);
          buckets.set(key, (buckets.get(key) || 0) + 1);
        }
        let dominante = 0;
        for (const v of buckets.values()) if (v > dominante) dominante = v;
        const total = W * H;
        setFotoValidada({ url, ok: buckets.size >= 24 && dominante / total <= 0.6 });
      } catch { setFotoValidada({ url, ok: false }); }
    };
    img.onerror = () => { if (!cancel) setFotoValidada({ url, ok: false }); };
    img.src = url;
    return () => { cancel = true; };
  }, [ed.imagenUrl]);

  const fotoOk = !!ed.imagenUrl && fotoValidada.url === ed.imagenUrl && fotoValidada.ok;
  const overlay = typeof ed.overlay === "number" ? ed.overlay : 0.55;
  const fx = typeof ed.focoX === "number" ? `${ed.focoX}%` : "50%";
  const fy = typeof ed.focoY === "number" ? `${ed.focoY}%` : "35%";
  const servicios = (Array.isArray(ed.servicios) ? ed.servicios : []).filter((s) => s.visible !== false);

  return (
    <div className="login-left">
      <div className="login-left-overlay" style={{ "--login-overlay": overlay }} />
      <div
        className="login-left-bg"
        data-image-slot="foto-oficial-bus"
        style={{
          "--login-foto": ed.imagenUrl && fotoOk ? `url(${ed.imagenUrl})` : undefined,
          "--login-foto-mobile": ed.imagenMobileUrl && fotoOk ? `url(${ed.imagenMobileUrl})` : undefined,
          backgroundPosition: `${fx} ${fy}`,
        }}
        role="img"
        aria-label={ed.imagenAlt || "Imagen principal del Login de Transportes INTIWATANA"}
      />
      <div className="login-left-pattern" />
      <div className="login-left-body">
        <div className="login-left-brand">
          <LogoSVG width={78} height={78} className="login-left-logo" style={{ marginBottom: "0.9rem" }} />
          <div className="login-left-title">{ed.tituloInstitucional || "INTIWATANA"}</div>
          <div className="login-left-subtitle">Transporte de pasajeros y encomiendas</div>
        </div>
        <div className="login-left-message">
          <p className="login-phrase">{ed.frasePrincipal || "MÁS QUE UN DESTINO,\nES TU HISTORIA"}</p>
          <p className="login-left-desc">
            {ed.textoComplementario || "Reservas, pasajes y encomiendas en un solo lugar. Viaja con seguridad, comodidad y confianza."}
          </p>
        </div>
        {ed.serviciosVisible !== false && servicios.length > 0 && (
          <ul className="login-services">
            {servicios.map((s, i) => {
              const Icono = ICONOS_SERVICIO[s.icono] || CheckCircle;
              return (
                <li key={i}>
                  <span className="lf-dot-circle">
                    <Icono size={15} aria-hidden="true" />
                  </span>
                  <div className="lf-content">
                    <span className="lf-title">{s.titulo}</span>
                    {s.subtitulo && <span className="lf-sub">{s.subtitulo}</span>}
                  </div>
                </li>
              );
            })}
          </ul>
        )}
      </div>
      <div className="login-left-foot">
        <div>Conectando destinos, uniendo personas</div>
        <div>© 2026 INTIWATANA S.R.L. Todos los derechos reservados.</div>
      </div>
    </div>
  );
}

export function RegisterForm({ onVolver }) {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ nombres: "", apellidos: "", dniRuc: "", telefono: "", email: "", username: "", password: "", confirmar: "" });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [paso, setPaso] = useState(1);

  const handle = (e) => setForm((f) => ({ ...f, [e.target.name]: e.target.value }));

  const avanzar = (e) => {
    e.preventDefault();
    if (!form.nombres || !form.apellidos || !form.dniRuc || !form.email) { setError("Completa todos los campos obligatorios."); return; }
    setError(""); setPaso(2);
  };

  const submit = async (e) => {
    e.preventDefault(); e.stopPropagation(); setError("");
    if (form.password !== form.confirmar) { setError("Las contraseñas no coinciden."); return; }
    if (form.password.length < 6) { setError("La contraseña debe tener al menos 6 caracteres."); return; }
    setLoading(true);
    try {
      const res = await fetch("/api/v1/clientes/registro", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ nombres: form.nombres, apellidos: form.apellidos, dniRuc: form.dniRuc, telefono: form.telefono || null, email: form.email || null, username: form.username, password: form.password }) });
      if (!res.ok) { const d = await res.json().catch(() => ({})); throw new Error(d.errores?.join(", ") || d.mensaje || d.message || "Error al registrarse"); }
      const data = await res.json();
      login(data);
      navigate("/pasajes", { replace: true });
    } catch (err) { setError(err.message); setLoading(false); }
  };

  return (
    <div className="auth-panel-inner">
      <div className="auth-panel-header">
        <button type="button" className="auth-back-btn" onClick={paso === 2 ? () => setPaso(1) : onVolver}>← {paso === 2 ? "Anterior" : "Volver"}</button>
        <h3 className="auth-title">Crear cuenta</h3>
        <p className="auth-sub">Regístrate para comprar pasajes y rastrear encomiendas</p>
        <div style={{ display: "flex", gap: 8, marginTop: 12, justifyContent: "center" }}>
          {[1, 2].map((n) => (<div key={n} style={{ width: 32, height: 4, borderRadius: 2, background: paso >= n ? "var(--amarillo)" : "rgba(138,166,163,0.3)", transition: "background 0.3s" }} />))}
        </div>
        <p style={{ fontSize: 12, color: "var(--verde-medio)", marginTop: 6, textAlign: "center" }}>Paso {paso} de 2</p>
      </div>
      {error && <div className="auth-error" role="alert"><AlertCircle size={15} /> {error}</div>}
      {paso === 1 ? (
        <form onSubmit={avanzar} className="auth-form-scroll">
          <div className="auth-grid-2">
            <div className="auth-field"><label>Nombre *</label><input name="nombres" value={form.nombres} onChange={handle} placeholder="Juan" required /></div>
            <div className="auth-field"><label>Apellido *</label><input name="apellidos" value={form.apellidos} onChange={handle} placeholder="Pérez" required /></div>
            <div className="auth-field"><label>DNI / RUC *</label><input name="dniRuc" value={form.dniRuc} onChange={handle} placeholder="12345678" required minLength={7} maxLength={15} /></div>
            <div className="auth-field"><label>Teléfono</label><input name="telefono" value={form.telefono} onChange={handle} placeholder="9XX XXX XXX" /></div>
          </div>
          <div className="auth-field"><label>Correo electrónico *</label><input name="email" type="email" value={form.email} onChange={handle} placeholder="correo@ejemplo.com" required /></div>
          <button className="auth-btn" type="submit">Continuar <ArrowRight size={16} /></button>
        </form>
      ) : (
        <form onSubmit={submit} className="auth-form-scroll" aria-busy={loading}>
          <div className="auth-field"><label>Nombre de usuario *</label><input name="username" value={form.username} onChange={handle} placeholder="Elige un usuario único" required /></div>
          <div className="auth-grid-2">
            <div className="auth-field"><label>Contraseña *</label><input name="password" type="password" value={form.password} onChange={handle} placeholder="Mín. 6 caracteres" required minLength={6} /></div>
            <div className="auth-field"><label>Confirmar *</label><input name="confirmar" type="password" value={form.confirmar} onChange={handle} placeholder="Repite la contraseña" required /></div>
          </div>
          <div style={{ background: "rgba(18,115,105,0.1)", border: "1px solid rgba(18,115,105,0.2)", borderRadius: 8, padding: "10px 14px", marginBottom: 12, fontSize: 13, color: "var(--verde-medio)" }}>
            <strong style={{ color: "var(--blanco)", display: "block", marginBottom: 4 }}>Resumen de tu cuenta</strong>
            {form.nombres} {form.apellidos} · DNI: {form.dniRuc} · {form.email}
          </div>
          <button className="auth-btn" type="submit" disabled={loading}>
            {loading ? "Creando cuenta..." : <><CheckCircle size={16} /> Crear cuenta</>}
          </button>
          <p className="auth-footer-note">Al registrarte, aceptas nuestros términos de servicio.</p>
        </form>
      )}
    </div>
  );
}

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [form, setForm] = useState({ username: "", password: "" });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [showPassword, setShowPassword] = useState(false);

  useEffect(() => {
    if (esSesionVencida()) {
      setError("Tu sesión venció. Inicia sesión nuevamente.");
      limpiarSesionVencida();
    }
  }, []);

  const handle = (e) => setForm((f) => ({ ...f, [e.target.name]: e.target.value }));

  const submit = async (e) => {
    e.preventDefault();
    if (loading) return;
    setError(""); setLoading(true);
    try {
      const res = await fetch("/api/v1/auth/login", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(form) });
      if (!res.ok) {
        const data = await res.json().catch(() => ({}));
        let msg;
        if (res.status === 401) msg = "Usuario o contraseña incorrectos.";
        else if (res.status === 403) msg = "No tienes autorización para acceder.";
        else if (res.status === 500) msg = "Ocurrió un error interno del servidor. Inténtalo nuevamente.";
        else msg = data.mensaje || data.message || "Usuario o contraseña incorrectos.";
        throw new Error(msg);
      }
      const data = await res.json();
      login(data);
      // returnTo conserva el contexto del portal (p.ej. resultados de búsqueda).
      // Solo se aceptan rutas internas relativas: nunca URLs absolutas ni externas.
      const rawReturn = searchParams.get("returnTo");
      const returnTo = rawReturn && rawReturn.startsWith("/") && !rawReturn.startsWith("//") ? rawReturn : null;
      // Redirige según rol automáticamente
      if (data.rol === "ROLE_CLIENTE") navigate(returnTo || "/pasajes", { replace: true });
      else if (data.rol === "ROLE_CHOFER") navigate("/chofer", { replace: true });
      else navigate("/admin", { replace: true }); // ADMIN, CAJERO
    } catch (err) {
      const m = String(err?.message || "");
      if (m.includes("Failed to fetch") || m.includes("NetworkError") || m.includes("ERR_CONNECTION_REFUSED") || m.includes("ECONNREFUSED") || m.includes("fetch")) {
        setError("No pudimos conectar con el servicio. Inténtalo nuevamente.");
      } else {
        setError(m);
      }
      setForm((f) => ({ ...f, password: "" }));
    }
    finally { setLoading(false); }
  };

  return (
    <div className="login-page">
      <div className="login-wrap">
        <LeftPanel />
        <div className="login-right">
          <div className="login-box">
            <div className="login-box-head">
              <LogoSVG width={78} height={78} className="login-box-logo" alt="Logo de Transportes INTIWATANA" />
              <h3 className="auth-title">Bienvenido</h3>
              <p className="auth-sub">Ingresa tus credenciales para continuar</p>
            </div>

            {error && (
              <div className="auth-error" role="alert" aria-live="assertive">
                <AlertCircle size={15} aria-hidden="true" /> {error}
              </div>
            )}

            <form onSubmit={submit} className="auth-form" aria-busy={loading}>
              <div className="auth-field">
                <label htmlFor="login-usuario">USUARIO</label>
                <div className="auth-input-wrap">
                  <User size={16} className="auth-input-icon" aria-hidden="true" />
                  <input
                    id="login-usuario"
                    name="username"
                    value={form.username}
                    onChange={handle}
                    placeholder="tu_usuario"
                    autoComplete="username"
                    required
                    autoFocus
                  />
                </div>
              </div>
              <div className="auth-field">
                <label htmlFor="login-password">CONTRASEÑA</label>
                <div className="auth-input-wrap">
                  <Lock size={16} className="auth-input-icon" aria-hidden="true" />
                  <input
                    id="login-password"
                    name="password"
                    type={showPassword ? "text" : "password"}
                    value={form.password}
                    onChange={handle}
                    placeholder="••••••••"
                    autoComplete="current-password"
                    required
                  />
                  <button
                    type="button"
                    className="auth-eye-btn"
                    onClick={() => setShowPassword((v) => !v)}
                    aria-label={showPassword ? "Ocultar contraseña" : "Mostrar contraseña"}
                    aria-pressed={showPassword}
                    tabIndex={0}
                  >
                    {showPassword ? <EyeOff size={16} aria-hidden="true" /> : <Eye size={16} aria-hidden="true" />}
                  </button>
                </div>
              </div>
              <button className="auth-btn" type="submit" disabled={loading} aria-busy={loading}>
                {loading ? (
                  "VALIDANDO..."
                ) : (
                  <span className="auth-btn-inner">
                    INGRESAR <ArrowRight size={16} aria-hidden="true" />
                  </span>
                )}
              </button>
              <p className="login-validando-sr" role="status" aria-live="polite">
                {loading ? "Validando credenciales…" : ""}
              </p>
            </form>

            <div className="login-consultar">
              <Link to="/pasajes" className="login-consultar-btn">
                Consultar mis reservas
              </Link>
              <span className="login-consultar-desc">
                Consulta el estado de tu reserva y descarga tus documentos.
              </span>
            </div>

            <p className="auth-footer-note">
              ¿No tienes cuenta?{" "}
              <Link to="/registrar" className="auth-register-link">
                Regístrate gratis
              </Link>
            </p>
            <Link to="/" className="login-home-link" aria-label="Volver al inicio">
              Volver al inicio
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}