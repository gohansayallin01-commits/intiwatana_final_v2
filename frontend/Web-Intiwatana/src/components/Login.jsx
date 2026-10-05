import { useState } from "react";
import { useNavigate, useSearchParams, Link } from "react-router-dom";
import { useAuth } from "../context/useAuth";
import LogoSVG from "./LogoSVG";
import { User, Lock, AlertCircle, ArrowRight, CheckCircle, Eye, EyeOff, Ticket, Armchair, Package, Bus } from "lucide-react";

export function LeftPanel() {
  return (
    <div className="login-left">
      <div className="login-left-overlay" />
      <div className="login-left-bg" data-image-slot="foto-oficial-bus" role="img" aria-label="Bus de Transportes INTIWATANA en carretera entre los Andes peruanos" />
      <div className="login-left-pattern" />
      <div className="login-left-body">
        <Link to="/" className="login-back-link" aria-label="Volver al inicio">
          <ArrowRight size={16} style={{ transform: "rotate(180deg)" }} /> Volver al inicio
        </Link>
        <div className="login-left-brand">
          <LogoSVG width={56} height={56} style={{ marginBottom: "1rem" }} />
          <div className="login-left-title">INTIWATANA</div>
          <div className="login-left-desc">
            Viaja seguro por los Andes peruanos. Reserva tu pasaje en línea.
          </div>
        </div>
        <ul className="login-features">
          <li>
            <span className="lf-dot-circle">
              <Ticket size={14} />
            </span>
            <div className="lf-content">
              <span className="lf-title">Reserva tu pasaje online</span>
              <span className="lf-sub">Rápido, seguro y desde cualquier lugar.</span>
            </div>
          </li>
          <li>
            <span className="lf-dot-circle">
              <Armchair size={14} />
            </span>
            <div className="lf-content">
              <span className="lf-title">Elige tu asiento</span>
              <span className="lf-sub">Viaja con comodidad.</span>
            </div>
          </li>
          <li>
            <span className="lf-dot-circle">
              <Package size={14} />
            </span>
            <div className="lf-content">
              <span className="lf-title">Rastrea tus encomiendas</span>
              <span className="lf-sub">Con tu número de guía.</span>
            </div>
          </li>
          <li>
            <span className="lf-dot-circle">
              <Bus size={14} />
            </span>
            <div className="lf-content">
              <span className="lf-title">Consulta tus viajes</span>
              <span className="lf-sub">Horarios, rutas y destinos.</span>
            </div>
          </li>
        </ul>
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
      {error && <div className="auth-error"><AlertCircle size={15}/> {error}</div>}
      {paso === 1 ? (
        <form onSubmit={avanzar} className="auth-form-scroll">
          <div className="auth-grid-2">
            <div className="auth-field"><label>Nombre *</label><input name="nombres" value={form.nombres} onChange={handle} placeholder="Juan" required /></div>
            <div className="auth-field"><label>Apellido *</label><input name="apellidos" value={form.apellidos} onChange={handle} placeholder="Pérez" required /></div>
            <div className="auth-field"><label>DNI / RUC *</label><input name="dniRuc" value={form.dniRuc} onChange={handle} placeholder="12345678" required minLength={7} maxLength={15} /></div>
            <div className="auth-field"><label>Teléfono</label><input name="telefono" value={form.telefono} onChange={handle} placeholder="9XX XXX XXX" /></div>
          </div>
          <div className="auth-field"><label>Correo electrónico *</label><input name="email" type="email" value={form.email} onChange={handle} placeholder="correo@ejemplo.com" required /></div>
          <button className="auth-btn" type="submit">Continuar <ArrowRight size={16}/></button>
        </form>
      ) : (
        <form onSubmit={submit} className="auth-form-scroll">
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
            {loading ? "Creando cuenta..." : <><CheckCircle size={16}/> Crear cuenta</>}
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
        setError("No se pudo conectar con el servidor. Verifica que el backend esté iniciado.");
      } else {
        setError(m);
      }
      setForm((f) => ({ ...f, password: "" }));
    }
    finally { setLoading(false); }
  };

  return (
    <div className="login-wrap">
      <LeftPanel />
      <div className="login-right">
        <div className="login-box">
          <div className="login-box-head">
            <LogoSVG width={56} height={56} className="login-box-logo" alt="Logo de Transportes INTIWATANA" />
            <h3 className="auth-title">Bienvenido</h3>
            <p className="auth-sub">Ingresa tus credenciales para continuar</p>
          </div>

          {error && (
            <div className="auth-error" role="alert">
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
                "Ingresando..."
              ) : (
                <span className="auth-btn-inner">
                  Ingresar <ArrowRight size={16} aria-hidden="true" />
                </span>
              )}
            </button>
          </form>

          <p className="auth-footer-note">
            ¿No tienes cuenta?{" "}
            <Link to="/registrar" className="auth-register-link">
              Regístrate gratis
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}
