import { useState, useEffect, useCallback, useRef, Component } from "react";
import { createPortal } from "react-dom";

class ErrorBoundary extends Component {
  constructor(props) { super(props); this.state = { hasError: false, error: null }; }
  static getDerivedStateFromError(error) { return { hasError: true, error }; }
  componentDidCatch(error, info) { console.error("PasajesPage crash:", error, info); }
  render() {
    if (this.state.hasError) {
      return (
        <div style={{ minHeight: "100vh", background: "var(--fondo)", display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", gap: 16, padding: "2rem" }}>
          <div style={{ background: "rgba(220,38,38,0.1)", border: "1px solid rgba(220,38,38,0.3)", borderRadius: 16, padding: "2rem", maxWidth: 480, textAlign: "center" }}>
            <div style={{ fontSize: 40, marginBottom: 12 }}>⚠️</div>
            <div style={{ color: "white", fontWeight: 700, fontSize: 18, marginBottom: 8 }}>Ocurrió un error inesperado</div>
            <div style={{ color: "#fca5a5", fontSize: 13, marginBottom: 20 }}>{this.state.error?.message || "Error desconocido"}</div>
            <button onClick={() => { this.setState({ hasError: false, error: null }); window.location.reload(); }}
              style={{ background: "var(--amarillo)", color: "var(--verde-oscuro)", border: "none", borderRadius: 10, padding: "0.8rem 2rem", fontWeight: 700, cursor: "pointer", fontSize: 14 }}>
              Recargar página
            </button>
          </div>
        </div>
      );
    }
    return this.props.children;
  }
}
import { useNavigate, useSearchParams } from "react-router-dom";
import { useAuth } from "../context/useAuth";
import LogoSVG from "./LogoSVG";
import { DESTINOS } from "../data/destinos";
import {
  MapPin, Flag, Bus, ShoppingCart, User, Ticket, Package, X,
  Clock, CreditCard, Building2, Smartphone, AlertTriangle, CheckCircle,
  Search, Calendar, ChevronRight, Armchair, Loader2, Frown,
  ArrowRight, FileText, Zap, Info, Navigation,
  LogIn, LayoutDashboard, Check, Lock, Compass,
} from "lucide-react";

function fmtFecha(iso) {
  if (!iso) return "—";
  try {
    const d = new Date(iso);
    if (isNaN(d.getTime())) return "—";
    return d.toLocaleString("es-PE", {
      weekday: "short", day: "2-digit", month: "short",
      hour: "2-digit", minute: "2-digit",
    });
  } catch { return "—"; }
}

function getImagenDestino(nombreCiudad) {
  if (!nombreCiudad) return null;
  const lower = nombreCiudad.toLowerCase();
  const found = DESTINOS.find((d) =>
    d.nombre.toLowerCase().includes(lower) ||
    lower.includes(d.nombre.toLowerCase().split(" ")[0].toLowerCase())
  );
  return found?.imagen || null;
}

// Abre un documento PDF servido por el backend (Content-Disposition: attachment).
function abrirDocumento(url) {
  try { window.open(url, "_blank", "noopener,noreferrer"); } catch { /* popup bloqueado */ }
}

// Etiqueta legible para el estado de una reserva.
function etiquetaEstadoReserva(estado) {
  switch (estado) {
    case "RETENIDO": return { texto: "Retenido", color: "#fbbf24" };
    case "PENDIENTE_PAGO": return { texto: "Pendiente de pago", color: "#fbbf24" };
    case "CONFIRMADO": return { texto: "Confirmado", color: "#34d399" };
    case "VENCIDO": return { texto: "Vencido", color: "#fca5a5" };
    case "CANCELADO": return { texto: "Cancelado", color: "#fca5a5" };
    default: return { texto: estado || "—", color: "var(--verde-medio)" };
  }
}

const MODELO_METODOS = [
  { value: "EFECTIVO", label: "Efectivo en terminal", desc: "Paga al llegar", icon: <CreditCard size={18} /> },
  { value: "YAPE", label: "Yape", desc: "Coordina en caja", icon: <Smartphone size={18} /> },
  { value: "PLIN", label: "Plin", desc: "Coordina en caja", icon: <Smartphone size={18} /> },
  { value: "TRANSFERENCIA", label: "Transferencia", desc: "Coordina en caja", icon: <Building2 size={18} /> },
  { value: "TARJETA_CREDITO", label: "Tarjeta de crédito", desc: "Coordina en caja", icon: <CreditCard size={18} /> },
  { value: "TARJETA_DEBITO", label: "Tarjeta de débito", desc: "Coordina en caja", icon: <CreditCard size={18} /> },
];

// ── VISTA TOP-DOWN: Asiento desde arriba — igual a foto real Toyota Hiace ──
const TopDownSeatSVG = ({ numero, estado, esSelec, compact = false }) => {
  const isOcc = estado !== "DISPONIBLE";

  // Colores por estado
  let bg, hd, bd, tc;
  if (esSelec) {
    bg = "#7c3405"; hd = "#5c2803"; bd = "#f59e0b"; tc = "#fef3c7";
  } else if (isOcc) {
    bg = "#161f2e"; hd = "#0d1520"; bd = "#1e2d3d"; tc = "#253345";
  } else {
    bg = "#1e2d3d"; hd = "#111e2d"; bd = "#2d4462"; tc = "#d4e8ff";
  }

  if (compact) {
    return (
      <svg width="42" height="54" viewBox="0 0 42 54" fill="none" xmlns="http://www.w3.org/2000/svg">
        {/* Headrest */}
        <rect x="5" y="1" width="32" height="11" rx="4" fill={hd} stroke={bd} strokeWidth="1.5"/>
        <rect x="9" y="3" width="24" height="5" rx="2" fill="white" fillOpacity="0.07"/>
        {/* Seat Back */}
        <rect x="1" y="10" width="40" height="27" rx="6" fill={bg} stroke={bd} strokeWidth="1.5"/>
        <rect x="5" y="12" width="32" height="6" rx="2" fill="white" fillOpacity="0.07"/>
        <line x1="12" y1="11" x2="12" y2="36" stroke={bd} strokeWidth="0.7" strokeDasharray="2 3"/>
        <line x1="30" y1="11" x2="30" y2="36" stroke={bd} strokeWidth="0.7" strokeDasharray="2 3"/>
        {/* Cushion */}
        <rect x="1" y="35" width="40" height="15" rx="6" fill={bg} stroke={bd} strokeWidth="1.5"/>
        <rect x="5" y="37" width="32" height="5" rx="2" fill="white" fillOpacity="0.05"/>
        {/* Armrests */}
        <rect x="0" y="36" width="3" height="10" rx="1.5" fill={hd} stroke={bd} strokeWidth="1"/>
        <rect x="39" y="36" width="3" height="10" rx="1.5" fill={hd} stroke={bd} strokeWidth="1"/>
        {/* State icon */}
        {esSelec ? (
          <path d="M13 27 L18 33 L29 20" stroke={tc} strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"/>
        ) : isOcc ? (
          <>
            <rect x="14" y="23" width="14" height="10" rx="2" fill={tc}/>
            <path d="M16 23 V20 C16 17 26 17 26 20 V23" stroke={tc} strokeWidth="1.5" fill="none"/>
          </>
        ) : (
          <text x="21" y="30" textAnchor="middle" fill={tc} fontSize="14" fontWeight="800" fontFamily="system-ui,sans-serif">{numero}</text>
        )}
      </svg>
    );
  }

  return (
    <svg width="54" height="68" viewBox="0 0 54 68" fill="none" xmlns="http://www.w3.org/2000/svg">
      {/* Headrest */}
      <rect x="6" y="1" width="42" height="14" rx="5" fill={hd} stroke={bd} strokeWidth="1.5"/>
      <rect x="10" y="3" width="34" height="7" rx="2" fill="white" fillOpacity="0.08"/>
      {/* Seat Back */}
      <rect x="1" y="13" width="52" height="34" rx="7" fill={bg} stroke={bd} strokeWidth="1.5"/>
      <rect x="5" y="15" width="44" height="8" rx="3" fill="white" fillOpacity="0.08"/>
      <line x1="16" y1="14" x2="16" y2="46" stroke={bd} strokeWidth="0.8" strokeDasharray="2 3"/>
      <line x1="38" y1="14" x2="38" y2="46" stroke={bd} strokeWidth="0.8" strokeDasharray="2 3"/>
      {/* Seat Cushion */}
      <rect x="1" y="45" width="52" height="20" rx="7" fill={bg} stroke={bd} strokeWidth="1.5"/>
      <rect x="5" y="47" width="44" height="7" rx="2" fill="white" fillOpacity="0.06"/>
      {/* Armrests */}
      <rect x="0" y="46" width="3" height="14" rx="1.5" fill={hd} stroke={bd} strokeWidth="1"/>
      <rect x="51" y="46" width="3" height="14" rx="1.5" fill={hd} stroke={bd} strokeWidth="1"/>
      {/* State icon */}
      {esSelec ? (
        <path d="M18 39 L24 46 L36 30" stroke={tc} strokeWidth="3" strokeLinecap="round" strokeLinejoin="round"/>
      ) : isOcc ? (
        <>
          <rect x="19" y="28" width="16" height="12" rx="2" fill={tc}/>
          <path d="M21 28 V24 C21 20 33 20 33 24 V28" stroke={tc} strokeWidth="1.5" fill="none"/>
        </>
      ) : (
        <text x="27" y="41" textAnchor="middle" fill={tc} fontSize="19" fontWeight="800" fontFamily="system-ui,sans-serif">{numero}</text>
      )}
    </svg>
  );
};

// Asiento del chofer — estático, sin interacción
const DriverSeatSVG = () => (
  <svg width="54" height="68" viewBox="0 0 54 68" fill="none" xmlns="http://www.w3.org/2000/svg">
    <rect x="6" y="1" width="42" height="14" rx="5" fill="#0d1520" stroke="#1e2d3d" strokeWidth="1.5"/>
    <rect x="10" y="3" width="34" height="7" rx="2" fill="white" fillOpacity="0.04"/>
    <rect x="1" y="13" width="52" height="34" rx="7" fill="#111e2e" stroke="#1e2d3d" strokeWidth="1.5"/>
    <rect x="5" y="15" width="44" height="8" rx="3" fill="white" fillOpacity="0.04"/>
    <line x1="16" y1="14" x2="16" y2="46" stroke="#1e2d3d" strokeWidth="0.8" strokeDasharray="2 3"/>
    <line x1="38" y1="14" x2="38" y2="46" stroke="#1e2d3d" strokeWidth="0.8" strokeDasharray="2 3"/>
    <rect x="1" y="45" width="52" height="20" rx="7" fill="#111e2e" stroke="#1e2d3d" strokeWidth="1.5"/>
    <rect x="5" y="47" width="44" height="7" rx="2" fill="white" fillOpacity="0.04"/>
    <rect x="0" y="46" width="3" height="14" rx="1.5" fill="#0d1520" stroke="#1e2d3d" strokeWidth="1"/>
    <rect x="51" y="46" width="3" height="14" rx="1.5" fill="#0d1520" stroke="#1e2d3d" strokeWidth="1"/>
    <text x="27" y="33" textAnchor="middle" fill="#2d4059" fontSize="6.5" fontWeight="900" fontFamily="system-ui" letterSpacing="0.08em">CHOFER</text>
  </svg>
);

const AsientoBtn = ({ asiento, ocupado, esSelec, onSeleccionar, compact = false }) => {
  const [hovered, setHovered] = useState(false);
  const handleClick = (e) => {
    e.preventDefault(); e.stopPropagation();
    if (!ocupado) onSeleccionar(asiento);
  };
  return (
    <div style={{ position: "relative", display: "inline-block" }}>
      <button
        type="button"
        tabIndex={ocupado ? -1 : 0}
        aria-label={`Asiento ${asiento.numeroAsiento} - ${ocupado ? "Ocupado" : esSelec ? "Seleccionado" : "Disponible"}`}
        aria-pressed={esSelec}
        aria-disabled={ocupado}
        onMouseEnter={() => setHovered(true)}
        onMouseLeave={() => setHovered(false)}
        onClick={handleClick}
        style={{
          display: "inline-flex", background: "transparent", border: "none", padding: 0,
          minWidth: 44, minHeight: 44, justifyContent: "center", alignItems: "center",
          cursor: ocupado ? "not-allowed" : "pointer",
          transition: "transform 0.18s cubic-bezier(0.34,1.56,0.64,1), filter 0.2s",
          transform: esSelec ? "translateY(-5px) scale(1.07)" : "none",
          filter: esSelec ? "drop-shadow(0 0 10px rgba(245,158,11,0.8))" : "none",
          outline: "none", userSelect: "none",
        }}
      >
        <TopDownSeatSVG numero={asiento.numeroAsiento} estado={asiento.estado} esSelec={esSelec} compact={compact} />
      </button>
      {hovered && (
        <div style={{
          position: "absolute", bottom: "calc(100% + 8px)", left: "50%",
          transform: "translateX(-50%)",
          background: "#060e1c",
          border: `1.5px solid ${esSelec ? "#f59e0b" : ocupado ? "rgba(71,85,105,0.5)" : "rgba(45,100,195,0.6)"}`,
          borderRadius: 10, padding: "6px 12px", whiteSpace: "nowrap",
          pointerEvents: "none", zIndex: 300,
          boxShadow: "0 8px 30px rgba(0,0,0,0.9)", textAlign: "center",
        }}>
          <div style={{ color: "white", fontWeight: 700, fontSize: 11, marginBottom: 3 }}>
            Asiento N° {asiento.numeroAsiento}
          </div>
          <div style={{
            fontSize: 10, fontWeight: 700, padding: "2px 7px", borderRadius: 4, display: "inline-block",
            background: ocupado ? "rgba(220,38,38,0.15)" : esSelec ? "rgba(245,158,11,0.15)" : "rgba(37,99,195,0.15)",
            color: ocupado ? "#fca5a5" : esSelec ? "#fbbf24" : "#93c5fd",
          }}>
            {ocupado ? "🔒 Ocupado" : esSelec ? "✓ Seleccionado" : "● Disponible"}
          </div>
        </div>
      )}
    </div>
  );
};

function MapaAsientos({ asientos, seleccionados, onSeleccionar }) {
  const lista = Array.isArray(asientos) ? asientos : [];
  const sel = Array.isArray(seleccionados) ? seleccionados : [];

  if (!lista.length) return (
    <div style={{ textAlign: "center", padding: "2.5rem", color: "#4b6a8c", fontSize: 14 }}>
      Sin datos de asientos disponibles.
    </div>
  );

  const disponibles = lista.filter(a => a.estado === "DISPONIBLE");
  const ocupados   = lista.filter(a => a.estado !== "DISPONIBLE");

  // ── Ordenar asientos: por fila, luego columna, luego numeroAsiento
  // Esto garantiza compatibilidad con cualquier numeración del backend
  const sorted = [...lista].sort((a, b) => {
    const fa = a.fila || 0,        fb = b.fila || 0;
    const ca = a.columna || 0,     cb = b.columna || 0;
    const na = a.numeroAsiento || 0, nb = b.numeroAsiento || 0;
    if (fa !== fb) return fa - fb;
    if (ca !== cb) return ca - cb;
    return na - nb;
  });

  // Obtener asiento por posición (0-indexed)
  const get = (pos) => sorted[pos] || null;

  // Render de un asiento por posición
  const renderSeat = (pos, compact = false) => {
    const a = get(pos);
    if (!a) return <div key={`empty-p${pos}`} style={{ width: compact ? 42 : 54, height: compact ? 54 : 68, flexShrink: 0 }} />;
    const ocupado = a.estado !== "DISPONIBLE";
    const esSelec = sel.some(s => s.id === a.id);
    return (
      <AsientoBtn
        key={`seat-${a.id}`}
        asiento={a}
        ocupado={ocupado}
        esSelec={esSelec}
        onSeleccionar={onSeleccionar}
        compact={compact}
      />
    );
  };

  const limpiarSeleccion = () => sel.forEach(a => onSeleccionar(a));

  // Selección rápida por posición en el layout Hiace
  // Ventana: posiciones laterales — Pasillo: posiciones centrales — Fondo: banco trasero
  const selAuto = (posiciones) => {
    const found = posiciones
      .map(p => get(p))
      .filter(Boolean)
      .find(a => a.estado === "DISPONIBLE" && !sel.some(s => s.id === a.id));
    if (found) onSeleccionar(found);
  };

  return (
    <div className="mapa-asientos">
      {/* Leyenda */}
      <div className="mapa-leyenda">
        <div style={{ display:"flex", alignItems:"center", gap:6, fontSize:11, color:"#93c5fd", fontWeight:700 }}>
          <div style={{ width:9, height:9, borderRadius:"50%", background:"#2563eb", boxShadow:"0 0 7px rgba(37,99,235,0.8)" }} />
          Disponible ({disponibles.length})
        </div>
        <div style={{ display:"flex", alignItems:"center", gap:6, fontSize:11, color:"#fbbf24", fontWeight:700 }}>
          <div style={{ width:9, height:9, borderRadius:"50%", background:"#f59e0b", boxShadow:"0 0 7px rgba(245,158,11,0.8)" }} />
          Seleccionado ({sel.length})
        </div>
        <div style={{ display:"flex", alignItems:"center", gap:6, fontSize:11, color:"#334155", fontWeight:700 }}>
          <div style={{ width:9, height:9, borderRadius:"50%", background:"#1e2d3d" }} />
          Ocupado ({ocupados.length})
        </div>
      </div>

      {/* Botones de selección rápida (por posición en el layout) */}
      <div style={{ display:"flex", gap:6, justifyContent:"center", flexWrap:"wrap", marginBottom:14 }}>
        <button type="button" onClick={() => selAuto([1, 4, 7, 10, 11, 14])}
          style={{ background:"rgba(37,99,235,0.1)", border:"1px solid rgba(37,99,235,0.35)", borderRadius:20, color:"#93c5fd", padding:"3px 10px", fontSize:11, fontWeight:700, cursor:"pointer", display:"flex", alignItems:"center", gap:4 }}>
          🪟 Ventana
        </button>
        <button type="button" onClick={() => selAuto([0, 2, 3, 5, 6, 8, 9, 12, 13])}
          style={{ background:"rgba(37,99,235,0.1)", border:"1px solid rgba(37,99,235,0.35)", borderRadius:20, color:"#93c5fd", padding:"3px 10px", fontSize:11, fontWeight:700, cursor:"pointer", display:"flex", alignItems:"center", gap:4 }}>
          🚶 Pasillo
        </button>
        <button type="button" onClick={() => selAuto([11, 12, 13, 14])}
          style={{ background:"rgba(37,99,235,0.1)", border:"1px solid rgba(37,99,235,0.35)", borderRadius:20, color:"#93c5fd", padding:"3px 10px", fontSize:11, fontWeight:700, cursor:"pointer", display:"flex", alignItems:"center", gap:4 }}>
          🛋️ Fondo
        </button>
        {sel.length > 0 && (
          <button type="button" onClick={limpiarSeleccion}
            style={{ background:"rgba(220,38,38,0.1)", border:"1px solid rgba(220,38,38,0.3)", borderRadius:20, color:"#fca5a5", padding:"3px 10px", fontSize:11, fontWeight:700, cursor:"pointer" }}>
            ✕ Limpiar ({sel.length})
          </button>
        )}
      </div>

      {/* ═══ TOYOTA HIACE 15P — VISTA DESDE ARRIBA ═══
          Layout (posición → fila en combi):
          Pos 0-1  → Fila delantera (copiloto + asiento 2)
          Pos 2-4  → Fila 2 (3 asientos)
          Pos 5-7  → Fila 3 (2 izq + fold + 1 der)
          Pos 8-10 → Fila 4 (2 izq + fold + 1 der)
          Pos 11-14→ Banco trasero (4 asientos)
      */}
      <div className="hiace-body">
        <div className="hiace-window-strip left" />
        <div className="hiace-window-strip right" />

        <div className="hiace-interior">
          {/* ── TABLERO ── */}
          <div className="hiace-dashboard">
            <div className="hiace-steering-wheel" />
            <div style={{ display:"flex", flexDirection:"column", alignItems:"center", gap:3 }}>
              <div className="hiace-radio-unit" />
              <div className="hiace-vent-grille" />
            </div>
            <div className="hiace-dash-right" />
          </div>

          {/* ── FILA 1: CHOFER + pos[0] + pos[1] ── */}
          <div className="hiace-row">
            <div className="hiace-driver-seat"><DriverSeatSVG /></div>
            {renderSeat(0)}
            {renderSeat(1)}
          </div>

          <div className="hiace-cabin-divider" />

          {/* ── FILA 2: pos[2] + pos[3] + pos[4] + [DOBLEABLE pared] ── */}
          <div className="hiace-row">
            {renderSeat(2)}
            {renderSeat(3)}
            {renderSeat(4)}
            <div className="hiace-dobleable-label">DOBLEABLE</div>
          </div>

          {/* ── FILA 3: pos[5] + pos[6] + [DOB] + pos[7] ── */}
          <div className="hiace-row">
            {renderSeat(5)}
            {renderSeat(6)}
            <div className="hiace-fold-gap">DOBLEABLE</div>
            {renderSeat(7)}
          </div>

          {/* ── FILA 4: pos[8] + pos[9] + [DOB] + pos[10] ── */}
          <div className="hiace-row">
            {renderSeat(8)}
            {renderSeat(9)}
            <div className="hiace-fold-gap">DOBLEABLE</div>
            {renderSeat(10)}
          </div>

          {/* ── BANCO TRASERO: pos[11] + pos[12] + pos[13] + pos[14] ── */}
          <div className="hiace-row hiace-rear-bench">
            {renderSeat(11, true)}
            {renderSeat(12, true)}
            {renderSeat(13, true)}
            {renderSeat(14, true)}
          </div>
        </div>

        {/* Parachoques + luces traseras */}
        <div className="hiace-rear-bumper">
          <div className="hiace-taillight" />
          <div style={{ fontSize:7, color:"rgba(148,163,184,0.35)", fontWeight:900, letterSpacing:"0.1em" }}>INTIWATANA</div>
          <div className="hiace-taillight" />
        </div>
      </div>
    </div>
  );
}


function CarritoSidebar({ items, onRemove, onCheckout, open, onClose }) {

  const total = items.reduce((s, i) => s + i.precio, 0);
  return (
    <>
      <div onClick={onClose} style={{ position: "fixed", inset: 0, background: "rgba(0,0,0,0.55)", zIndex: 1100, opacity: open ? 1 : 0, pointerEvents: open ? "auto" : "none", transition: "opacity 0.3s" }} />
      <div style={{ position: "fixed", top: 0, right: 0, bottom: 0, width: 380, background: "linear-gradient(160deg, #10403B 0%, #0c2d29 100%)", borderLeft: "2px solid rgba(245,197,24,0.25)", zIndex: 1200, display: "flex", flexDirection: "column", transform: open ? "translateX(0)" : "translateX(100%)", transition: "transform 0.35s cubic-bezier(0.4,0,0.2,1)", boxShadow: open ? "-8px 0 40px rgba(0,0,0,0.5)" : "none" }}>
        <div style={{ padding: "1.2rem 1.4rem", borderBottom: "1px solid rgba(138,166,163,0.15)", display: "flex", alignItems: "center", justifyContent: "space-between" }}>

          <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
            <ShoppingCart size={22} color="var(--amarillo)" />
            <span style={{ fontFamily: "'Playfair Display', serif", fontSize: 18, color: "white", fontWeight: 700 }}>Mi Carrito</span>
            {items.length > 0 && <span style={{ background: "var(--amarillo)", color: "var(--verde-oscuro)", borderRadius: 50, fontSize: 11, fontWeight: 800, padding: "1px 7px", minWidth: 20, textAlign: "center" }}>{items.length}</span>}
          </div>
          <button onClick={onClose} style={{ background: "rgba(255,255,255,0.06)", border: "1px solid rgba(138,166,163,0.2)", borderRadius: 8, color: "var(--verde-medio)", cursor: "pointer", width: 32, height: 32, display: "flex", alignItems: "center", justifyContent: "center" }}><X size={16} /></button>
        </div>
        <div style={{ flex: 1, overflowY: "auto", padding: "1rem 1.4rem" }}>
          {items.length === 0 ? (
            <div style={{ textAlign: "center", padding: "3rem 1rem", color: "var(--verde-medio)" }}>
              <ShoppingCart size={48} style={{ marginBottom: 12, opacity: 0.4 }} />
              <p style={{ fontWeight: 600, marginBottom: 6 }}>Carrito vacío</p>
              <p style={{ fontSize: 13 }}>Agrega un pasaje para continuar con tu compra.</p>
            </div>
          ) : (
            <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
              {items.map((item) => (
                <div key={item.id} style={{ background: "rgba(18,115,105,0.12)", border: "1px solid rgba(18,115,105,0.25)", borderRadius: 12, padding: "0.9rem 1rem", display: "flex", alignItems: "flex-start", gap: 12 }}>
                  <div style={{ flexShrink: 0, marginTop: 2 }}><Ticket size={22} color="var(--amarillo)" /></div>
                  <div style={{ flex: 1 }}>
                    <div style={{ fontWeight: 700, color: "white", fontSize: 14, marginBottom: 2 }}>Pasaje</div>
                    <div style={{ color: "var(--verde-medio)", fontSize: 12, lineHeight: 1.5 }}>{item.descripcion}</div>
                    <div style={{ color: "var(--amarillo)", fontWeight: 800, fontSize: 15, marginTop: 4 }}>
                      S/ {item.precio.toFixed(2)}
                    </div>
                  </div>
                  <button onClick={() => onRemove(item.id)} style={{ background: "rgba(255,80,80,0.1)", border: "1px solid rgba(255,80,80,0.2)", borderRadius: 6, color: "#ff6b6b", cursor: "pointer", width: 28, height: 28, display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}><X size={14} /></button>
                </div>
              ))}
            </div>
          )}
        </div>
        {items.length > 0 && (
          <div style={{ padding: "1.2rem 1.4rem", borderTop: "1px solid rgba(138,166,163,0.15)" }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 14 }}>
              <span style={{ color: "var(--verde-medio)", fontSize: 14 }}>Total</span>
              <span style={{ fontWeight: 800, fontSize: 22, color: "var(--amarillo)" }}>S/ {total.toFixed(2)}</span>
            </div>
            <button onClick={onCheckout} style={{ width: "100%", background: "linear-gradient(135deg, var(--amarillo), #e6b800)", color: "var(--verde-oscuro)", border: "none", borderRadius: 12, padding: "0.9rem", fontSize: 15, fontWeight: 800, cursor: "pointer", fontFamily: "inherit", letterSpacing: "0.04em", display: "flex", alignItems: "center", justifyContent: "center", gap: 8, boxShadow: "0 4px 16px rgba(245,197,24,0.3)" }}>
              <CreditCard size={18} /> Proceder al pago
            </button>
          </div>
        )}
      </div>
    </>
  );
}

// ── FormEncomienda ────────────────────────────────────────────────────────────
// FIXED: árbol DOM estable en el preview de costo — sin montaje/desmontaje
//        condicional de nodos que causaba el error removeChild.
// ═══ FormEncomienda ══════════════════════════════════════════════════════════
// Solicitud PREVIA de encomienda. El cliente declara únicamente la ruta, las
// personas y la descripción: el peso, las dimensiones, la tarifa, el pago y la
// guía oficial los determina el personal autorizado al recibir el paquete.
const MENSAJE_SOLICITUD =
  "Esta es una solicitud previa. El peso, las dimensiones, la tarifa, el pago y la guía oficial serán confirmados por el personal autorizado cuando el paquete sea recibido físicamente en una agencia INTIWATANA.";

function FormEncomienda({ sucursales, session, onIrLogin }) {
  const navigate = useNavigate();
  const [origenId, setOrigenId] = useState("");
  const [destinoId, setDestinoId] = useState("");
  const [remitente, setRemitente] = useState("");
  const [telRemitente, setTelRemitente] = useState("");
  const [destinatario, setDestinatario] = useState("");
  const [telDestinatario, setTelDestinatario] = useState("");
  const [descripcion, setDescripcion] = useState("");
  const [tipoEnvio, setTipoEnvio] = useState("NORMAL");
  const [cuidado, setCuidado] = useState("");
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState("");
  const [solicitud, setSolicitud] = useState(null);

  const limpiarCampos = () => {
    setOrigenId(""); setDestinoId(""); setRemitente(""); setTelRemitente("");
    setDestinatario(""); setTelDestinatario(""); setDescripcion("");
    setTipoEnvio("NORMAL"); setCuidado("");
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    if (!session) { onIrLogin(); return; }
    if (session.rol !== "ROLE_CLIENTE") {
      setError("La solicitud web es para cuentas de cliente. Si eres personal autorizado, registra la encomienda desde el panel interno.");
      return;
    }
    if (!origenId || !destinoId || !remitente.trim() || !telRemitente.trim()
      || !destinatario.trim() || !telDestinatario.trim() || !descripcion.trim()) {
      setError("Completa todos los campos obligatorios.");
      return;
    }
    setEnviando(true);
    try {
      // Solo datos declarativos: sin peso, sin medidas, sin valor, sin tarifa,
      // sin método de pago y sin guía. El backend genera la referencia SOL.
      const headers = { "Content-Type": "application/json", Authorization: `Bearer ${session.token}` };
      const payload = {
        sucursalOrigenId: Number(origenId),
        sucursalDestinoId: Number(destinoId),
        remitenteNombre: remitente.trim(),
        remitenteTelefono: telRemitente.trim(),
        destinatarioNombre: destinatario.trim(),
        destinatarioTelefono: telDestinatario.trim(),
        descripcionContenido: descripcion.trim(),
        tipoEnvio,
        cuidadoEspecial: cuidado.trim() || null,
      };
      const res = await fetch("/api/v1/encomiendas/solicitar", { method: "POST", headers, body: JSON.stringify(payload) });
      if (!res.ok) {
        const d = await res.json().catch(() => ({}));
        throw new Error(d.mensaje || d.message || "No se pudo registrar la solicitud.");
      }
      const data = await res.json();
      setSolicitud({ ...data, _origen: origenId, _destino: destinoId, _remitente: remitente.trim(),
        _telRemitente: telRemitente.trim(), _destinatario: destinatario.trim(),
        _telDestinatario: telDestinatario.trim(), _descripcion: descripcion.trim() });
      limpiarCampos();
    } catch (err) {
      setError(err.message || "No se pudo registrar la solicitud.");
    } finally {
      setEnviando(false);
    }
  };

  const tipos = [
    { value: "NORMAL", label: "Normal", desc: "3–5 días", icon: <Package size={15} /> },
    { value: "EXPRESS", label: "Express", desc: "24 horas", icon: <Zap size={15} /> },
    { value: "FRAGIL", label: "Frágil", desc: "Cuidado especial", icon: <AlertTriangle size={15} /> },
  ];

  const inputStyle = { width: "100%", background: "rgba(255,255,255,0.06)", border: "1.5px solid rgba(138,166,163,0.3)", borderRadius: 8, padding: "10px 12px", color: "white", fontSize: 13, fontFamily: "inherit", boxSizing: "border-box", minHeight: 44 };
  const labelStyle = { color: "var(--verde-medio)", fontSize: 12, display: "flex", alignItems: "center", gap: 5, marginBottom: 5 };
  const infoRow = { display: "flex", justifyContent: "space-between", alignItems: "center", gap: 12, background: "rgba(255,255,255,0.05)", border: "1px solid rgba(138,166,163,0.2)", borderRadius: 8, padding: "9px 12px", fontSize: 13, color: "var(--verde-medio)" };

  // Pantalla de resultado: referencia SOL y estado pendiente. Nunca muestra guía.
  if (solicitud) {
    const nombreSucursal = (id) => sucursales.find((s) => String(s.id) === String(id))?.ciudad
      || sucursales.find((s) => String(s.id) === String(id))?.nombre || "—";
    return (
      <div style={{ background: "rgba(18,115,105,0.1)", border: "1px solid var(--verde)", borderRadius: 16, padding: "1.5rem" }}>
        <div style={{ background: "rgba(18,115,105,0.3)", border: "1px solid var(--verde)", borderRadius: 10, padding: "0.9rem 1.1rem", marginBottom: 6, color: "white", fontWeight: 800, fontSize: 16, letterSpacing: "0.03em", display: "flex", alignItems: "center", gap: 9 }}>
          <CheckCircle size={20} color="var(--amarillo)" /> SOLICITUD REGISTRADA
        </div>
        <div style={{ color: "var(--amarillo)", fontWeight: 700, fontSize: 13, marginBottom: 16, paddingLeft: 2 }}>
          PENDIENTE DE RECEPCIÓN EN AGENCIA
        </div>

        <div style={{ background: "rgba(245,197,24,0.1)", border: "1px solid var(--amarillo)", borderRadius: 10, padding: "0.9rem 1.1rem", marginBottom: 16 }}>
          <div style={{ fontSize: 11, color: "var(--verde-medio)", letterSpacing: "0.08em", textTransform: "uppercase", marginBottom: 4 }}>
            Referencia de solicitud
          </div>
          <div style={{ fontSize: 22, fontWeight: 800, color: "var(--amarillo)", fontFamily: "'Playfair Display', serif" }}>
            {solicitud.referenciaSolicitud}
          </div>
          <div style={{ fontSize: 11, color: "var(--verde-medio)", marginTop: 6 }}>
            Esta referencia NO es una guía de seguimiento. La guía oficial se emite al recibir el paquete.
          </div>
        </div>

        <div style={{ display: "grid", gap: 8, marginBottom: 16 }}>
          <div style={infoRow}><span>Origen</span><strong style={{ color: "white" }}>{nombreSucursal(solicitud._origen)}</strong></div>
          <div style={infoRow}><span>Destino</span><strong style={{ color: "white" }}>{nombreSucursal(solicitud._destino)}</strong></div>
          <div style={infoRow}><span>Remitente</span><strong style={{ color: "white" }}>{solicitud._remitente}</strong></div>
          <div style={infoRow}><span>Teléfono del remitente</span><strong style={{ color: "white" }}>{solicitud._telRemitente}</strong></div>
          <div style={infoRow}><span>Destinatario</span><strong style={{ color: "white" }}>{solicitud._destinatario}</strong></div>
          <div style={infoRow}><span>Teléfono del destinatario</span><strong style={{ color: "white" }}>{solicitud._telDestinatario}</strong></div>
          <div style={infoRow}><span>Descripción</span><strong style={{ color: "white" }}>{solicitud._descripcion}</strong></div>
          <div style={infoRow}>
            <span>Fecha de solicitud</span>
            <strong style={{ color: "white" }}>
              {solicitud.createdAt ? new Date(solicitud.createdAt).toLocaleString("es-PE") : "—"}
            </strong>
          </div>
          <div style={infoRow}>
            <span>Estado</span>
            <strong style={{ color: "var(--amarillo)" }}>PENDIENTE DE RECEPCIÓN EN AGENCIA</strong>
          </div>
        </div>

        <div style={{ background: "rgba(245,197,24,0.08)", border: "1px solid rgba(245,197,24,0.3)", borderRadius: 10, padding: "0.9rem 1rem", marginBottom: 16, display: "flex", gap: 8 }}>
          <Info size={16} color="var(--amarillo)" style={{ flexShrink: 0, marginTop: 2 }} />
          <span style={{ color: "var(--amarillo)", fontSize: 13, lineHeight: 1.5 }}>
            Presenta el paquete en la agencia de origen con tu referencia a mano. El personal
            autorizado verificará los datos, registrará el pago y te entregará la guía oficial.
          </span>
        </div>

        <div style={{ display: "flex", flexWrap: "wrap", gap: 10 }}>
          <button type="button" className="btn-reservar" style={{ flex: "1 1 180px", display: "flex", alignItems: "center", justifyContent: "center", gap: 8, minHeight: 44 }} onClick={() => navigate("/cliente?seccion=encomiendas")}>
            <Package size={16} /> Ver mis solicitudes
          </button>
          <button type="button" onClick={() => setSolicitud(null)} style={{ flex: "1 1 180px", background: "rgba(18,115,105,0.4)", border: "1px solid var(--verde)", borderRadius: 10, color: "white", fontWeight: 700, cursor: "pointer", fontFamily: "inherit", padding: "0.8rem", minHeight: 44 }}>Registrar otra solicitud</button>
          <button type="button" onClick={() => navigate("/")} style={{ flex: "1 1 180px", background: "transparent", border: "1px solid rgba(138,166,163,0.4)", borderRadius: 10, color: "var(--verde-medio)", fontWeight: 700, cursor: "pointer", fontFamily: "inherit", padding: "0.8rem", minHeight: 44 }}>Volver al portal</button>
        </div>
      </div>
    );
  }

  return (
    <div style={{ background: "rgba(18,115,105,0.08)", border: "1px solid rgba(18,115,105,0.2)", borderRadius: 16, padding: "1.5rem" }}>
      <div style={{ background: "rgba(245,197,24,0.08)", border: "1px solid rgba(245,197,24,0.3)", borderRadius: 10, padding: "0.8rem 1rem", marginBottom: 16, color: "var(--amarillo)", fontSize: 13, lineHeight: 1.5, display: "flex", gap: 8 }}>
        <Info size={16} style={{ flexShrink: 0, marginTop: 2 }} />
        {MENSAJE_SOLICITUD}
      </div>
      {error && (
        <div role="alert" style={{ background: "rgba(220,38,38,0.12)", border: "1px solid rgba(220,38,38,0.35)", borderRadius: 10, padding: "0.8rem 1rem", marginBottom: 16, color: "#fca5a5", fontSize: 13, display: "flex", alignItems: "center", gap: 8 }}>
          <AlertTriangle size={16} /> {error}
        </div>
      )}
      <form onSubmit={handleSubmit}>
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))", gap: 12, marginBottom: 14 }}>
          <div>
            <label htmlFor="enc-origen" style={labelStyle}><MapPin size={13} /> Ciudad de origen *</label>
            <select id="enc-origen" value={origenId} onChange={(e) => setOrigenId(e.target.value)} required style={inputStyle}>
              <option value="">Seleccionar</option>
              {sucursales.map((s) => <option key={s.id} value={s.id}>{s.ciudad}</option>)}
            </select>
          </div>
          <div>
            <label htmlFor="enc-destino" style={labelStyle}><Flag size={13} /> Ciudad destino *</label>
            <select id="enc-destino" value={destinoId} onChange={(e) => setDestinoId(e.target.value)} required style={inputStyle}>
              <option value="">Seleccionar</option>
              {sucursales.filter((s) => s.id !== Number(origenId)).map((s) => <option key={s.id} value={s.id}>{s.ciudad}</option>)}
            </select>
          </div>
        </div>

        <div style={{ marginBottom: 14 }}>
          <label style={labelStyle}><Package size={13} /> Tipo de envío *</label>
          <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
            {tipos.map((t) => (
              <label key={t.value} style={{ flex: "1 1 150px", background: tipoEnvio === t.value ? "rgba(18,115,105,0.3)" : "rgba(255,255,255,0.04)", border: `1.5px solid ${tipoEnvio === t.value ? "var(--verde)" : "rgba(138,166,163,0.2)"}`, borderRadius: 10, padding: "10px 8px", cursor: "pointer", textAlign: "center", transition: "all 0.18s", minHeight: 44, display: "flex", flexDirection: "column", justifyContent: "center" }}>
                <input type="radio" name="tipoEnvio" value={t.value} checked={tipoEnvio === t.value} onChange={() => setTipoEnvio(t.value)} style={{ display: "none" }} />
                <div style={{ fontSize: 13, fontWeight: 700, color: "white", display: "flex", alignItems: "center", justifyContent: "center", gap: 5 }}>{t.icon} {t.label}</div>
                <div style={{ fontSize: 11, color: "var(--verde-medio)", marginTop: 2 }}>{t.desc}</div>
              </label>
            ))}
          </div>
        </div>

        <fieldset style={{ border: "1px solid rgba(138,166,163,0.2)", borderRadius: 12, padding: "0.9rem 1rem", marginBottom: 14 }}>
          <legend style={{ fontSize: 12, color: "var(--verde-medio)", padding: "0 6px" }}>Remitente</legend>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))", gap: 12 }}>
            <div>
              <label htmlFor="enc-remitente" style={labelStyle}><User size={13} /> Nombre completo *</label>
              <input id="enc-remitente" value={remitente} onChange={(e) => setRemitente(e.target.value)} placeholder="Nombre de quien envía" type="text" required style={inputStyle} />
            </div>
            <div>
              <label htmlFor="enc-tel-remitente" style={labelStyle}><Smartphone size={13} /> Teléfono del remitente *</label>
              <input id="enc-tel-remitente" value={telRemitente} onChange={(e) => setTelRemitente(e.target.value)} placeholder="999 888 777" type="tel" required style={inputStyle} />
            </div>
          </div>
        </fieldset>

        <fieldset style={{ border: "1px solid rgba(138,166,163,0.2)", borderRadius: 12, padding: "0.9rem 1rem", marginBottom: 14 }}>
          <legend style={{ fontSize: 12, color: "var(--verde-medio)", padding: "0 6px" }}>Destinatario</legend>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))", gap: 12 }}>
            <div>
              <label htmlFor="enc-destinatario" style={labelStyle}><User size={13} /> Nombre completo *</label>
              <input id="enc-destinatario" value={destinatario} onChange={(e) => setDestinatario(e.target.value)} placeholder="Nombre de quien recibe" type="text" required style={inputStyle} />
            </div>
            <div>
              <label htmlFor="enc-tel-destinatario" style={labelStyle}><Smartphone size={13} /> Teléfono del destinatario *</label>
              <input id="enc-tel-destinatario" value={telDestinatario} onChange={(e) => setTelDestinatario(e.target.value)} placeholder="999 888 777" type="tel" required style={inputStyle} />
            </div>
          </div>
        </fieldset>

        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))", gap: 12, marginBottom: 14 }}>
          <div>
            <label htmlFor="enc-descripcion" style={labelStyle}><FileText size={13} /> Descripción del contenido *</label>
            <input id="enc-descripcion" value={descripcion} onChange={(e) => setDescripcion(e.target.value)} placeholder="Ej: Ropa, documentos, alimentos..." maxLength={300} required style={inputStyle} />
          </div>
          <div>
            <label htmlFor="enc-cuidado" style={labelStyle}><AlertTriangle size={13} /> Cuidado especial (opcional)</label>
            <input id="enc-cuidado" value={cuidado} onChange={(e) => setCuidado(e.target.value)} placeholder="Ej: Frágil, no doblar" maxLength={120} style={inputStyle} />
          </div>
        </div>

        <div role="note" style={{ background: "rgba(245,197,24,0.08)", border: "1px solid rgba(245,197,24,0.3)", borderRadius: 10, padding: "0.8rem 1rem", marginBottom: 14, color: "var(--amarillo)", fontSize: 13, lineHeight: 1.5, display: "flex", gap: 8 }}>
          <Info size={16} style={{ flexShrink: 0, marginTop: 2 }} />
          {MENSAJE_SOLICITUD}
        </div>

        <button
          type="submit"
          disabled={enviando}
          style={{
            width: "100%",
            background: "linear-gradient(135deg, var(--verde), #0f9183)",
            color: "white",
            border: "none",
            borderRadius: 10, padding: "0.9rem", fontSize: 14, fontWeight: 700,
            cursor: enviando ? "not-allowed" : "pointer",
            fontFamily: "inherit", letterSpacing: "0.04em",
            display: "flex", alignItems: "center", justifyContent: "center", gap: 8,
            transition: "all 0.2s",
            minHeight: 44,
            opacity: enviando ? 0.7 : 1,
          }}
        >
          <Package size={18} /> <span>{enviando ? "Registrando solicitud..." : "SOLICITAR ENVÍO"}</span> <ChevronRight size={16} />
        </button>
      </form>
    </div>
  );
}

// ── ModalPago ─────────────────────────────────────────────────────────────────
// FIXED: botón "Confirmar y pagar" sin position alternante
function ModalPago({ viaje, asientosSeleccionados, itemsCarrito, onConfirmar, onCerrar, comprando }) {
  const [paso, setPaso] = useState(1);
  const [metodoPago, setMetodoPago] = useState("EFECTIVO");
  const [datosExtra, setDatosExtra] = useState({ operacion: "", nombre: "" });

  const isCarrito = itemsCarrito !== undefined;
  const precioUnit = viaje ? (viaje.precioAdulto || viaje.precioOficial || viaje.precio || 0) : 0;
  const totalCarrito = isCarrito ? itemsCarrito.reduce((s, i) => s + (parseFloat(i.precio) || 0), 0) : 0;
  const totalPasajes = !isCarrito ? (asientosSeleccionados?.length || 0) * (parseFloat(precioUnit) || 0) : 0;
  const total = isCarrito ? totalCarrito : totalPasajes;

  const metodos = [
    { value: "EFECTIVO", label: "Efectivo en terminal", desc: "Paga al momento", icon: <CreditCard size={20} /> },
    { value: "YAPE", label: "Yape", desc: "Pago móvil inmediato", icon: <Smartphone size={20} /> },
    { value: "PLIN", label: "Plin", desc: "Pago móvil inmediato", icon: <Smartphone size={20} /> },
    { value: "TARJETA_CREDITO", label: "Tarjeta de crédito", desc: "Visa, Mastercard", icon: <CreditCard size={20} /> },
    { value: "TARJETA_DEBITO", label: "Tarjeta de débito", desc: "Débito automático", icon: <CreditCard size={20} /> },
    { value: "TRANSFERENCIA", label: "Transferencia bancaria", desc: "BCP, Interbank, BBVA", icon: <Building2 size={20} /> },
  ];

  const necesitaDatos = ["YAPE", "PLIN", "TARJETA_CREDITO", "TARJETA_DEBITO", "TRANSFERENCIA"].includes(metodoPago);
  const inputStyle = { width: "100%", background: "rgba(255,255,255,0.06)", border: "1.5px solid rgba(138,166,163,0.3)", borderRadius: 8, padding: "10px 13px", color: "white", fontSize: 14, fontFamily: "inherit", boxSizing: "border-box" };

  return (
    <div className="modal-overlay" onClick={onCerrar}>
      <div className="modal pago-modal" onClick={(e) => e.stopPropagation()} style={{ maxWidth: 520 }}>
        <div className="modal-header">
          <div className="modal-badge">{isCarrito ? <ShoppingCart size={22} /> : <Ticket size={22} />}</div>
          <div>
            <div className="modal-title">{paso === 1 ? "Resumen del pedido" : "Método de pago"}</div>
            <div className="modal-subtitle">{isCarrito ? `${itemsCarrito.length} item(s) · Total S/ ${total.toFixed(2)}` : `${viaje?.ruta?.origen?.ciudad} → ${viaje?.ruta?.destino?.ciudad}`}</div>
          </div>
          <button className="modal-close" onClick={onCerrar}><X size={18} /></button>
        </div>
        <div className="modal-body">
          <div style={{ display: "flex", gap: 6, marginBottom: 20 }}>
            {["Resumen", "Pago"].map((label, i) => (
              <div key={i} style={{ flex: 1, textAlign: "center" }}>
                <div style={{ height: 4, borderRadius: 2, marginBottom: 4, background: paso > i + 1 || paso === i + 1 ? "var(--amarillo)" : "rgba(138,166,163,0.3)" }} />
                <span style={{ fontSize: 11, color: paso === i + 1 ? "var(--amarillo)" : "var(--verde-medio)" }}>{label}</span>
              </div>
            ))}
          </div>
          {paso === 1 && (
            <>
              {isCarrito ? (
                <div style={{ display: "flex", flexDirection: "column", gap: 8, marginBottom: 16, maxHeight: "50vh", overflowY: "auto" }}>
                  {itemsCarrito.map((item) => (
                    <div key={item.id} style={{ background: "rgba(18,115,105,0.12)", border: "1px solid rgba(18,115,105,0.25)", borderRadius: 10, padding: "0.8rem 1rem", display: "flex", alignItems: "center", gap: 10 }}>
                      <Ticket size={20} color="var(--amarillo)" />
                      <div style={{ flex: 1 }}>
                        <div style={{ fontWeight: 700, color: "white", fontSize: 13 }}>Pasaje</div>
                        <div style={{ color: "var(--verde-medio)", fontSize: 11 }}>{item.descripcion}</div>
                      </div>
                      <span style={{ color: "var(--amarillo)", fontWeight: 800 }}>S/ {item.precio.toFixed(2)}</span>
                    </div>
                  ))}
                </div>
              ) : (
                <div style={{ background: "rgba(18,115,105,0.12)", border: "1px solid rgba(18,115,105,0.25)", borderRadius: 12, padding: "1rem 1.2rem", marginBottom: "1.2rem" }}>
                  <div style={{ fontFamily: "'Playfair Display', serif", fontSize: 17, color: "white", fontWeight: 700, marginBottom: 8 }}>{viaje?.ruta?.origen?.ciudad} → {viaje?.ruta?.destino?.ciudad}</div>
                  <div style={{ color: "var(--verde-medio)", fontSize: 12, marginBottom: 10, display: "flex", alignItems: "center", gap: 5 }}><Clock size={12} /> {fmtFecha(viaje?.fechaHoraSalida)}</div>
                  <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 8, fontSize: 13 }}>
                    <div>
                      <div style={{ color: "var(--verde-medio)", fontSize: 11, display: "flex", alignItems: "center", gap: 4 }}><Armchair size={11} /> Asientos ({asientosSeleccionados?.length})</div>
                      <div style={{ color: "white", fontWeight: 600 }}>{asientosSeleccionados?.map((a) => `N°${a.numeroAsiento}`).join(", ")}</div>
                    </div>
                    <div>
                      <div style={{ color: "var(--verde-medio)", fontSize: 11, display: "flex", alignItems: "center", gap: 4 }}><Bus size={11} /> Bus</div>
                      <div style={{ color: "white", fontWeight: 600 }}>{viaje?.bus?.placa || "—"}</div>
                    </div>
                  </div>
                </div>
              )}
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", borderTop: "1px solid rgba(138,166,163,0.2)", paddingTop: 12, marginBottom: 16 }}>
                <span style={{ color: "var(--verde-medio)", fontSize: 14 }}>Total a pagar</span>
                <span style={{ fontSize: 24, fontWeight: 800, color: "var(--amarillo)" }}>S/ {total.toFixed(2)}</span>
              </div>
              <div style={{ display: "flex", gap: 10 }}>
                <button className="btn-cancelar" onClick={onCerrar} style={{ flex: 1 }}>Cancelar</button>
                <button className="btn-reservar" style={{ flex: 2, display: "flex", alignItems: "center", justifyContent: "center", gap: 8 }} onClick={() => setPaso(2)}>
                  Continuar al pago <ChevronRight size={16} />
                </button>
              </div>
            </>
          )}
          {paso === 2 && (
            <>
              <p style={{ color: "var(--verde-medio)", fontSize: 13, marginBottom: 14 }}>
                <>Selecciona cómo deseas pagar <strong style={{ color: "white" }}>S/ {total.toFixed(2)}</strong></>
              </p>
              <div style={{ display: "flex", flexDirection: "column", gap: 7, marginBottom: 14, maxHeight: "40vh", overflowY: "auto" }}>
                {metodos.map((m) => (
                  <label key={m.value} style={{ display: "flex", alignItems: "center", gap: 12, background: metodoPago === m.value ? "rgba(18,115,105,0.2)" : "rgba(255,255,255,0.04)", border: `1.5px solid ${metodoPago === m.value ? "var(--verde)" : "rgba(138,166,163,0.2)"}`, borderRadius: 10, padding: "10px 14px", cursor: "pointer", transition: "all 0.18s" }}>
                    <input type="radio" name="metodo" value={m.value} checked={metodoPago === m.value} onChange={() => setMetodoPago(m.value)} style={{ accentColor: "var(--verde)", width: 16, height: 16 }} />
                    <span style={{ color: "var(--amarillo)" }}>{m.icon}</span>
                    <div>
                      <div style={{ fontSize: 14, fontWeight: 700, color: "white" }}>{m.label}</div>
                      <div style={{ fontSize: 11, color: "var(--verde-medio)" }}>{m.desc}</div>
                    </div>
                  </label>
                ))}
              </div>
              {metodoPago === "EFECTIVO" && (
                <div style={{ background: "rgba(245,197,24,0.08)", border: "1px solid rgba(245,197,24,0.2)", borderRadius: 8, padding: "10px 14px", marginBottom: 14, fontSize: 13, color: "var(--amarillo)", display: "flex", alignItems: "center", gap: 8 }}>
                  <Info size={15} /> Presenta tu código de reserva en el terminal para pagar.
                </div>
              )}
              {(metodoPago === "YAPE" || metodoPago === "PLIN") && (
                <div style={{ marginBottom: 14 }}>
                  <div style={{ background: "rgba(18,115,105,0.1)", borderRadius: 8, padding: "10px 14px", marginBottom: 10, fontSize: 13, color: "var(--verde-medio)", display: "flex", alignItems: "center", gap: 8 }}>
                    <Smartphone size={15} color="var(--amarillo)" /> Envía <strong style={{ color: "var(--amarillo)" }}>S/ {total.toFixed(2)}</strong> al número <strong style={{ color: "white" }}>999-888-777</strong>
                  </div>
                  <label style={{ color: "var(--verde-medio)", fontSize: 12, display: "block", marginBottom: 5 }}>N° de operación *</label>
                  <input placeholder="Ej: 123456789" value={datosExtra.operacion} onChange={(e) => setDatosExtra((p) => ({ ...p, operacion: e.target.value }))} style={inputStyle} />
                </div>
              )}
              {metodoPago === "TRANSFERENCIA" && (
                <div style={{ marginBottom: 14 }}>
                  <div style={{ background: "rgba(18,115,105,0.1)", borderRadius: 8, padding: "10px 14px", marginBottom: 10, fontSize: 13, color: "var(--verde-medio)", display: "flex", gap: 8 }}>
                    <Building2 size={15} color="var(--amarillo)" style={{ flexShrink: 0, marginTop: 2 }} />
                    <div>Transfiere a la cuenta BCP: <strong style={{ color: "white" }}>123-456789-0-12</strong><br />A nombre de: <strong style={{ color: "white" }}>E.T. INTIWATANA S.R.L.</strong></div>
                  </div>
                  <label style={{ color: "var(--verde-medio)", fontSize: 12, display: "block", marginBottom: 5 }}>N° de operación *</label>
                  <input placeholder="Número de operación bancaria" value={datosExtra.operacion} onChange={(e) => setDatosExtra((p) => ({ ...p, operacion: e.target.value }))} style={inputStyle} />
                </div>
              )}
              <div style={{ display: "flex", gap: 10 }}>
                <button className="btn-cancelar" onClick={() => setPaso(1)} style={{ flex: 1 }}>Volver</button>
                {/* FIXED: sin position alternante — solo renderizado condicional simple */}
                <button
                  className="btn-reservar"
                  style={{
                    flex: 2,
                    opacity: (necesitaDatos && !datosExtra.operacion) || comprando ? 0.6 : 1,
                    display: "flex", alignItems: "center", justifyContent: "center", gap: 8,
                  }}
                  disabled={comprando || (necesitaDatos && !datosExtra.operacion)}
                  onClick={() => onConfirmar(metodoPago, datosExtra)}
                >
                  <span style={{ display: "flex", alignItems: "center", gap: 8 }}>
                    {comprando ? (
                      <>
                        <Loader2 size={16} className="spin-icon" />
                        <span>Procesando...</span>
                      </>
                    ) : (
                      <>
                        <CheckCircle size={16} />
                        <span>Confirmar y pagar</span>
                      </>
                    )}
                  </span>
                </button>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}

function ModalBoardingPass({ boletos, viaje, onCerrar }) {
  if (!boletos || !boletos.length) return null;
  const bList = Array.isArray(boletos) ? boletos : [boletos];
  return (
    <div style={{ position: "fixed", inset: 0, background: "rgba(0,0,0,0.88)", zIndex: 9999, overflowY: "auto", display: "flex", alignItems: "flex-start", justifyContent: "center", padding: "2rem 1rem" }}>
      <div style={{ width: "100%", maxWidth: 480, margin: "auto" }} onClick={(e) => e.stopPropagation()}>
        <div style={{ textAlign: "center", marginBottom: "1.5rem" }}>
          <div style={{ width: 64, height: 64, borderRadius: "50%", background: "linear-gradient(135deg, var(--verde), #0f9183)", display: "flex", alignItems: "center", justifyContent: "center", margin: "0 auto 12px", boxShadow: "0 0 0 8px rgba(18,115,105,0.2)" }}>
            <CheckCircle size={34} color="white" />
          </div>
          <h2 style={{ fontFamily: "'Playfair Display', serif", color: "white", fontSize: "1.6rem", margin: "0 0 4px" }}>¡Compra exitosa!</h2>
          <p style={{ color: "var(--verde-medio)", fontSize: 14, margin: 0 }}>Tu{bList.length > 1 ? "s boleto" : " boleto"}{bList.length > 1 ? "s han" : " ha"} sido reservado{bList.length > 1 ? "s" : ""} correctamente</p>
        </div>

        {bList.map((boleto, idx) => {
          const origen = boleto.viaje?.ruta?.origen?.ciudad || viaje?.ruta?.origen?.ciudad || "—";
          const destino = boleto.viaje?.ruta?.destino?.ciudad || viaje?.ruta?.destino?.ciudad || "—";
          const fechaSalida = fmtFecha(boleto.viaje?.fechaHoraSalida || viaje?.fechaHoraSalida);
          const bus = boleto.viaje?.bus?.placa || viaje?.bus?.placa || "—";
          const asiento = boleto.asiento?.numeroAsiento || "—";
          const precio = boleto.precioPagado ?? viaje?.precioAdulto ?? "—";
          const numeroBoleto = boleto.numeroBoleto || "—";
          const pasajero = boleto.cliente?.nombreCompleto || boleto.cliente?.nombre || "PASAJERO";

          return (
            <div key={idx} style={{ marginBottom: idx < bList.length - 1 ? 20 : 0 }}>
              <div style={{ background: "white", borderRadius: "20px 20px 0 0", overflow: "hidden", boxShadow: "0 20px 60px rgba(0,0,0,0.5)" }}>
                <div style={{ background: "linear-gradient(135deg, #10403B 0%, #127369 100%)", padding: "1.4rem 1.6rem", position: "relative", overflow: "hidden" }}>
                  <div style={{ position: "absolute", top: -20, right: -20, width: 120, height: 120, borderRadius: "50%", background: "rgba(245,197,24,0.08)" }} />
                  <div style={{ position: "absolute", bottom: -30, left: 60, width: 100, height: 100, borderRadius: "50%", background: "rgba(255,255,255,0.04)" }} />
                  <div style={{ position: "relative", zIndex: 1 }}>
                    <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 16 }}>
                      <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                        <Bus size={18} color="var(--amarillo)" />
                        <span style={{ color: "var(--amarillo)", fontWeight: 800, fontSize: 13, letterSpacing: "0.1em", textTransform: "uppercase" }}>INTIWATANA S.R.L.</span>
                      </div>
                      <span style={{ background: "rgba(245,197,24,0.15)", border: "1px solid rgba(245,197,24,0.4)", color: "var(--amarillo)", borderRadius: 20, padding: "3px 12px", fontSize: 11, fontWeight: 700 }}>CONFIRMADO</span>
                    </div>
                    <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                      <div style={{ flex: 1 }}>
                        <div style={{ color: "rgba(255,255,255,0.6)", fontSize: 11, textTransform: "uppercase", letterSpacing: "0.08em", marginBottom: 4 }}>Origen</div>
                        <div style={{ color: "white", fontFamily: "'Playfair Display', serif", fontSize: "1.6rem", fontWeight: 700, lineHeight: 1 }}>{origen}</div>
                      </div>
                      <div style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: 4, flexShrink: 0 }}>
                        <div style={{ width: 32, height: 32, borderRadius: "50%", background: "rgba(245,197,24,0.2)", border: "1.5px solid rgba(245,197,24,0.5)", display: "flex", alignItems: "center", justifyContent: "center" }}>
                          <ArrowRight size={16} color="var(--amarillo)" />
                        </div>
                      </div>
                      <div style={{ flex: 1, textAlign: "right" }}>
                        <div style={{ color: "rgba(255,255,255,0.6)", fontSize: 11, textTransform: "uppercase", letterSpacing: "0.08em", marginBottom: 4 }}>Destino</div>
                        <div style={{ color: "white", fontFamily: "'Playfair Display', serif", fontSize: "1.6rem", fontWeight: 700, lineHeight: 1 }}>{destino}</div>
                      </div>
                    </div>
                  </div>
                </div>
                <div style={{ position: "relative", height: 24, background: "white" }}>
                  <div style={{ position: "absolute", left: -12, top: "50%", transform: "translateY(-50%)", width: 24, height: 24, borderRadius: "50%", background: "rgba(0,0,0,0.88)" }} />
                  <div style={{ position: "absolute", right: -12, top: "50%", transform: "translateY(-50%)", width: 24, height: 24, borderRadius: "50%", background: "rgba(0,0,0,0.88)" }} />
                  <div style={{ position: "absolute", left: 24, right: 24, top: "50%", borderTop: "2px dashed #e5e7eb" }} />
                </div>
                <div style={{ background: "white", padding: "0.8rem 1.6rem 1.4rem" }}>
                  <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 16, marginBottom: 16 }}>
                    <div>
                      <div style={{ color: "#9ca3af", fontSize: 10, textTransform: "uppercase", letterSpacing: "0.1em", marginBottom: 3 }}>Pasajero</div>
                      <div style={{ color: "#111827", fontWeight: 700, fontSize: 15 }}>{pasajero}</div>
                    </div>
                    <div>
                      <div style={{ color: "#9ca3af", fontSize: 10, textTransform: "uppercase", letterSpacing: "0.1em", marginBottom: 3 }}>N° de Boleto</div>
                      <div style={{ color: "#127369", fontWeight: 700, fontSize: 12, fontFamily: "monospace", wordBreak: "break-all" }}>{numeroBoleto}</div>
                    </div>
                  </div>
                  <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr 1fr", gap: 12, padding: "12px 0", borderTop: "1px solid #f3f4f6", borderBottom: "1px solid #f3f4f6", marginBottom: 16 }}>
                    <div style={{ textAlign: "center" }}>
                      <div style={{ color: "#9ca3af", fontSize: 10, textTransform: "uppercase", letterSpacing: "0.08em", marginBottom: 4, display: "flex", alignItems: "center", justifyContent: "center", gap: 3 }}><Clock size={10} /> Salida</div>
                      <div style={{ color: "#111827", fontWeight: 600, fontSize: 12 }}>{fechaSalida}</div>
                    </div>
                    <div style={{ textAlign: "center", borderLeft: "1px solid #f3f4f6", borderRight: "1px solid #f3f4f6" }}>
                      <div style={{ color: "#9ca3af", fontSize: 10, textTransform: "uppercase", letterSpacing: "0.08em", marginBottom: 4, display: "flex", alignItems: "center", justifyContent: "center", gap: 3 }}><Bus size={10} /> Bus</div>
                      <div style={{ color: "#111827", fontWeight: 700, fontSize: 13 }}>{bus}</div>
                    </div>
                    <div style={{ textAlign: "center" }}>
                      <div style={{ color: "#9ca3af", fontSize: 10, textTransform: "uppercase", letterSpacing: "0.08em", marginBottom: 4, display: "flex", alignItems: "center", justifyContent: "center", gap: 3 }}><Armchair size={10} /> Asiento</div>
                      <div style={{ color: "#127369", fontWeight: 900, fontSize: 22, lineHeight: 1 }}>{asiento}</div>
                    </div>
                  </div>
                  <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
                    <div style={{ display: "flex", alignItems: "center", gap: 6, color: "#6b7280", fontSize: 12 }}>
                      <CreditCard size={14} /> Total pagado
                    </div>
                    <div style={{ fontWeight: 900, fontSize: 22, color: "#10403B" }}>S/ {typeof precio === "number" ? precio.toFixed(2) : precio}</div>
                  </div>
                </div>
              </div>
              <div style={{ background: "#f9fafb", borderRadius: "0 0 20px 20px", padding: "1rem 1.6rem", borderTop: "1px solid #e5e7eb", boxShadow: "0 20px 60px rgba(0,0,0,0.5)" }}>
                <div style={{ display: "flex", alignItems: "center", gap: 8, color: "#6b7280", fontSize: 12, marginBottom: 12 }}>
                  <Info size={14} color="#127369" />
                  Presenta este boleto en la terminal al momento de abordaje.
                </div>
                <button onClick={onCerrar} style={{ width: "100%", background: "linear-gradient(135deg, #10403B, #127369)", color: "white", border: "none", borderRadius: 12, padding: "0.85rem", fontSize: 14, fontWeight: 800, cursor: "pointer", fontFamily: "inherit", letterSpacing: "0.06em", textTransform: "uppercase" }}>
                  Cerrar
                </button>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

// ── TimerReserva ──────────────────────────────────────────────────────────────
// Cuenta regresiva basada SIEMPRE en el expiraEn que devuelve el backend.
function TimerReserva({ expiraEn, onExpirar }) {
  const calc = useCallback(() => {
    if (!expiraEn) return 0;
    return Math.max(0, Math.floor((new Date(expiraEn).getTime() - Date.now()) / 1000));
  }, [expiraEn]);
  const [restante, setRestante] = useState(calc);

  useEffect(() => { setRestante(calc()); }, [calc]);
  useEffect(() => {
    if (!expiraEn) return undefined;
    const t = setInterval(() => {
      setRestante((prev) => {
        const next = prev - 1;
        if (next <= 0) { clearInterval(t); onExpirar?.(); return 0; }
        return next;
      });
    }, 1000);
    return () => clearInterval(t);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [expiraEn]);

  const h = Math.floor(restante / 3600);
  const m = Math.floor((restante % 3600) / 60);
  const s = restante % 60;
  const urgente = restante <= 120;
  return (
    <span aria-live="polite" aria-label={`Tiempo restante ${h} horas ${m} minutos ${s} segundos`}
      style={{ fontVariantNumeric: "tabular-nums", fontWeight: 800, color: urgente ? "#fca5a5" : "var(--amarillo)" }}>
      {h > 0 ? `${String(h).padStart(2, "0")}:` : ""}{String(m).padStart(2, "0")}:{String(s).padStart(2, "0")}
    </span>
  );
}

// ── ModalReserva ──────────────────────────────────────────────────────────────
// Flujo invitado: datos del comprador → retención backend → reserva pendiente
// de pago → constancia PDF. NO emite boleto ni QR de embarque.
function ModalReserva({ viaje, asientosSeleccionados, session, onCerrar, onReservaCreada, onVencida }) {
  const [paso, setPaso] = useState(1);
  const [metodo, setMetodo] = useState("EFECTIVO");
  const [procesando, setProcesando] = useState(false);
  const [error, setError] = useState("");
  const [reserva, setReserva] = useState(null);
  const [expirada, setExpirada] = useState(false);
  const idemRef = useRef(`web-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`);
  const [form, setForm] = useState(() => {
    const partes = (session?.nombreCompleto || "").trim().split(/\s+/);
    return {
      nombres: session?.nombres || partes.slice(0, 1).join(" ") || "",
      apellidos: session?.apellidos || partes.slice(1).join(" ") || "",
      documento: session?.documento || "",
      telefono: session?.telefono || "",
      email: session?.email || "",
    };
  });

  const precioUnit = parseFloat(viaje?.precioAdulto ?? viaje?.precioOficial ?? viaje?.precio ?? 0);
  const total = (asientosSeleccionados?.length || 0) * precioUnit;
  const inputStyle = { width: "100%", background: "rgba(255,255,255,0.06)", border: "1.5px solid rgba(138,166,163,0.3)", borderRadius: 8, padding: "10px 13px", color: "white", fontSize: 14, fontFamily: "inherit", boxSizing: "border-box" };

  const enviar = async (e) => {
    e.preventDefault();
    setError("");
    if (!form.nombres.trim() || !form.apellidos.trim() || !form.documento.trim() || !form.telefono.trim()) {
      setError("Completa nombres, apellidos, documento y teléfono.");
      return;
    }
    setProcesando(true);
    try {
      const body = {
        viajeId: viaje.id,
        asientoIds: asientosSeleccionados.map((a) => a.id),
        idempotencyKey: idemRef.current,
        comprador: {
          nombres: form.nombres.trim(),
          apellidos: form.apellidos.trim(),
          documento: form.documento.trim(),
          telefono: form.telefono.trim(),
          email: form.email.trim() || null,
        },
        metodoPagoPreferido: metodo,
        observaciones: "Reserva web (invitado)",
      };
      const r1 = await fetch("/api/v1/ventas/retencion", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
      });
      if (!r1.ok) {
        const d = await r1.json().catch(() => ({}));
        throw new Error(d.mensaje || d.message || "No se pudieron retener los asientos seleccionados.");
      }
      let data = await r1.json();
      // Deja la reserva como PENDIENTE_PAGO para generar la constancia en caja.
      const r2 = await fetch(`/api/v1/ventas/retencion/${encodeURIComponent(data.referencia)}/reservar?metodoPago=${encodeURIComponent(metodo)}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
      });
      if (r2.ok) data = await r2.json();
      setReserva(data);
      setPaso(2);
      onReservaCreada?.(data);
    } catch (err) {
      setError(err.message || "No se pudo completar la reserva.");
      onVencida?.();
    } finally {
      setProcesando(false);
    }
  };

  const documentos = () => {
    if (!reserva) return;
    const doc = form.documento.trim();
    if (reserva.validoParaEmbarque && reserva.boletos?.length) {
      abrirDocumento(`/api/v1/ventas/boleto/${encodeURIComponent(reserva.boletos[0].numeroBoleto)}/pdf?documento=${encodeURIComponent(doc)}`);
    } else {
      abrirDocumento(`/api/v1/ventas/retencion/${encodeURIComponent(reserva.referencia)}/constancia.pdf?documento=${encodeURIComponent(doc)}`);
    }
  };

  return (
    <div className="modal-overlay" onClick={onCerrar}>
      <div className="modal pago-modal" style={{ maxWidth: 560 }} onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <div className="modal-badge">{paso === 1 ? <Armchair size={22} /> : <Ticket size={22} />}</div>
          <div>
            <div className="modal-title">{paso === 1 ? "Datos del comprador" : "Reserva registrada"}</div>
            <div className="modal-subtitle">{viaje?.ruta?.origen?.ciudad} → {viaje?.ruta?.destino?.ciudad}</div>
          </div>
          <button className="modal-close" onClick={onCerrar} aria-label="Cerrar"><X size={18} /></button>
        </div>
        <div className="modal-body">
          {error && (
            <div role="alert" style={{ background: "rgba(220,38,38,0.12)", border: "1px solid rgba(220,38,38,0.35)", borderRadius: 8, padding: "10px 14px", marginBottom: 14, fontSize: 13, color: "#fca5a5", display: "flex", alignItems: "center", gap: 8 }}>
              <AlertTriangle size={15} /> {error}
            </div>
          )}

          {paso === 1 && (
            <>
              <div style={{ background: "rgba(18,115,105,0.12)", border: "1px solid rgba(18,115,105,0.25)", borderRadius: 12, padding: "0.9rem 1.1rem", marginBottom: 14 }}>
                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 8, fontSize: 13 }}>
                  <div>
                    <div style={{ color: "var(--verde-medio)", fontSize: 11, display: "flex", alignItems: "center", gap: 4 }}><Armchair size={11} /> Asientos ({asientosSeleccionados?.length})</div>
                    <div style={{ color: "white", fontWeight: 700 }}>{asientosSeleccionados?.map((a) => `N°${a.numeroAsiento}`).join(", ")}</div>
                  </div>
                  <div>
                    <div style={{ color: "var(--verde-medio)", fontSize: 11, display: "flex", alignItems: "center", gap: 4 }}><Clock size={11} /> Salida</div>
                    <div style={{ color: "white", fontWeight: 700 }}>{fmtFecha(viaje?.fechaHoraSalida)}</div>
                  </div>
                </div>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginTop: 10, paddingTop: 10, borderTop: "1px solid rgba(138,166,163,0.2)" }}>
                  <span style={{ color: "var(--verde-medio)", fontSize: 13 }}>Total a pagar</span>
                  <span style={{ fontSize: 22, fontWeight: 800, color: "var(--amarillo)" }}>S/ {total.toFixed(2)}</span>
                </div>
              </div>

              <div style={{ background: "rgba(245,197,24,0.08)", border: "1px solid rgba(245,197,24,0.25)", borderRadius: 8, padding: "10px 14px", marginBottom: 14, fontSize: 12.5, color: "var(--amarillo)", display: "flex", alignItems: "center", gap: 8 }}>
                <Info size={15} /> Puedes reservar como <strong style={{ color: "white" }}>invitado</strong>. No necesitas crear una cuenta.
              </div>

              <form onSubmit={enviar}>
                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 }}>
                  <div>
                    <label htmlFor="rsv-nombres" style={{ color: "var(--verde-medio)", fontSize: 12, display: "block", marginBottom: 5 }}>Nombres *</label>
                    <input id="rsv-nombres" value={form.nombres} onChange={(e) => setForm((p) => ({ ...p, nombres: e.target.value }))} style={inputStyle} autoComplete="given-name" />
                  </div>
                  <div>
                    <label htmlFor="rsv-apellidos" style={{ color: "var(--verde-medio)", fontSize: 12, display: "block", marginBottom: 5 }}>Apellidos *</label>
                    <input id="rsv-apellidos" value={form.apellidos} onChange={(e) => setForm((p) => ({ ...p, apellidos: e.target.value }))} style={inputStyle} autoComplete="family-name" />
                  </div>
                  <div>
                    <label htmlFor="rsv-documento" style={{ color: "var(--verde-medio)", fontSize: 12, display: "block", marginBottom: 5 }}>DNI / RUC *</label>
                    <input id="rsv-documento" value={form.documento} onChange={(e) => setForm((p) => ({ ...p, documento: e.target.value }))} style={inputStyle} inputMode="numeric" />
                  </div>
                  <div>
                    <label htmlFor="rsv-telefono" style={{ color: "var(--verde-medio)", fontSize: 12, display: "block", marginBottom: 5 }}>Teléfono *</label>
                    <input id="rsv-telefono" value={form.telefono} onChange={(e) => setForm((p) => ({ ...p, telefono: e.target.value }))} style={inputStyle} inputMode="tel" />
                  </div>
                </div>
                <div style={{ marginTop: 12 }}>
                  <label htmlFor="rsv-email" style={{ color: "var(--verde-medio)", fontSize: 12, display: "block", marginBottom: 5 }}>Correo (opcional)</label>
                  <input id="rsv-email" type="email" value={form.email} onChange={(e) => setForm((p) => ({ ...p, email: e.target.value }))} style={inputStyle} autoComplete="email" />
                </div>

                <p style={{ color: "var(--verde-medio)", fontSize: 12, margin: "16px 0 8px" }}>Método de pago preferido</p>
                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 7, maxHeight: "30vh", overflowY: "auto" }}>
                  {MODELO_METODOS.map((m) => (
                    <label key={m.value} style={{ display: "flex", alignItems: "center", gap: 9, background: metodo === m.value ? "rgba(18,115,105,0.2)" : "rgba(255,255,255,0.04)", border: `1.5px solid ${metodo === m.value ? "var(--verde)" : "rgba(138,166,163,0.2)"}`, borderRadius: 10, padding: "9px 12px", cursor: "pointer" }}>
                      <input type="radio" name="metodo-reserva" value={m.value} checked={metodo === m.value} onChange={() => setMetodo(m.value)} style={{ accentColor: "var(--verde)", width: 15, height: 15 }} />
                      <span style={{ color: "var(--amarillo)" }}>{m.icon}</span>
                      <div>
                        <div style={{ fontSize: 12.5, fontWeight: 700, color: "white" }}>{m.label}</div>
                        <div style={{ fontSize: 10.5, color: "var(--verde-medio)" }}>{m.desc}</div>
                      </div>
                    </label>
                  ))}
                </div>

                <div style={{ display: "flex", gap: 10, marginTop: 18 }}>
                  <button type="button" className="btn-cancelar" onClick={onCerrar} style={{ flex: 1 }}>Cancelar</button>
                  <button type="submit" className="btn-reservar" disabled={procesando} style={{ flex: 2, display: "flex", alignItems: "center", justifyContent: "center", gap: 8, opacity: procesando ? 0.6 : 1 }}>
                    {procesando ? <><Loader2 size={16} className="spin-icon" /> Reteniendo asientos...</> : <><CheckCircle size={16} /> Continuar con la reserva</>}
                  </button>
                </div>
              </form>
            </>
          )}

          {paso === 2 && reserva && (
            <>
              <div style={{ background: "rgba(245,197,24,0.1)", border: "1px solid rgba(245,197,24,0.35)", borderRadius: 12, padding: "0.9rem 1.1rem", marginBottom: 14, display: "flex", alignItems: "center", gap: 10 }}>
                <AlertTriangle size={20} color="var(--amarillo)" />
                <div style={{ fontSize: 13, color: "var(--amarillo)", fontWeight: 700 }}>
                  Reserva pendiente de pago — NO VÁLIDA PARA EMBARQUE hasta confirmar el pago en caja.
                </div>
              </div>

              <div style={{ background: "rgba(18,115,105,0.12)", border: "1px solid rgba(18,115,105,0.25)", borderRadius: 12, padding: "1rem 1.2rem", marginBottom: 14 }}>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 10 }}>
                  <span style={{ color: "var(--verde-medio)", fontSize: 12 }}>Referencia de reserva</span>
                  {(() => { const e = etiquetaEstadoReserva(reserva.estado); return <span style={{ color: e.color, fontWeight: 800, fontSize: 12 }}>{e.texto}</span>; })()}
                </div>
                <div style={{ fontFamily: "monospace", fontSize: 18, fontWeight: 800, color: "var(--amarillo)", letterSpacing: "0.05em", marginBottom: 12, wordBreak: "break-all" }}>{reserva.referencia}</div>
                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 10, fontSize: 13 }}>
                  <div>
                    <div style={{ color: "var(--verde-medio)", fontSize: 11 }}>Asientos</div>
                    <div style={{ color: "white", fontWeight: 700 }}>{reserva.asientos?.map((a) => `N°${a.numeroAsiento}`).join(", ") || "—"}</div>
                  </div>
                  <div>
                    <div style={{ color: "var(--verde-medio)", fontSize: 11 }}>Salida</div>
                    <div style={{ color: "white", fontWeight: 700 }}>{fmtFecha(reserva.fechaHoraSalida)}</div>
                  </div>
                  <div>
                    <div style={{ color: "var(--verde-medio)", fontSize: 11 }}>Total</div>
                    <div style={{ color: "var(--amarillo)", fontWeight: 800 }}>S/ {Number(reserva.total || 0).toFixed(2)}</div>
                  </div>
                  <div>
                    <div style={{ color: "var(--verde-medio)", fontSize: 11 }}>Tiempo para pagar</div>
                    <div>{expirada ? <span style={{ color: "#fca5a5", fontWeight: 800 }}>Vencida</span> : <TimerReserva expiraEn={reserva.expiraEn} onExpirar={() => { setExpirada(true); onVencida?.(); }} />}</div>
                  </div>
                </div>
              </div>

              <div style={{ background: "rgba(255,255,255,0.04)", border: "1px solid rgba(138,166,163,0.2)", borderRadius: 10, padding: "10px 14px", fontSize: 12.5, color: "var(--verde-medio)", lineHeight: 1.5, marginBottom: 14 }}>
                <div style={{ display: "flex", gap: 8, marginBottom: 6 }}><Info size={15} color="var(--amarillo)" style={{ flexShrink: 0, marginTop: 1 }} /><span>Presenta tu referencia y DNI en cualquier agencia INTIWATANA para pagar. Recién entonces se emite el boleto con QR de embarque.</span></div>
                {reserva.mensaje && <div style={{ color: "var(--verde-medio)" }}>{reserva.mensaje}</div>}
              </div>

              <div style={{ display: "flex", gap: 10 }}>
                <button type="button" className="btn-reservar" style={{ flex: 2, display: "flex", alignItems: "center", justifyContent: "center", gap: 8, opacity: expirada ? 0.5 : 1 }} disabled={expirada} onClick={documentos}>
                  <FileText size={16} /> Descargar constancia (PDF)
                </button>
                <button type="button" className="btn-cancelar" onClick={onCerrar} style={{ flex: 1 }}>Cerrar</button>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}

// ── ModalRecuperar ────────────────────────────────────────────────────────────
// Recuperación de reservas/boletos por documento, sin cuenta.
function ModalRecuperar({ onCerrar }) {
  const [documento, setDocumento] = useState("");
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState("");
  const [data, setData] = useState(null);
  const inputStyle = { width: "100%", background: "rgba(255,255,255,0.06)", border: "1.5px solid rgba(138,166,163,0.3)", borderRadius: 8, padding: "10px 13px", color: "white", fontSize: 14, fontFamily: "inherit", boxSizing: "border-box" };

  const buscar = async (e) => {
    e.preventDefault();
    if (!documento.trim()) { setError("Ingresa tu DNI o RUC."); return; }
    setCargando(true); setError("");
    try {
      const res = await fetch(`/api/v1/ventas/recuperar?documento=${encodeURIComponent(documento.trim())}`);
      if (!res.ok) { const d = await res.json().catch(() => ({})); throw new Error(d.mensaje || d.message || "No se pudo consultar."); }
      setData(await res.json());
    } catch (err) { setError(err.message || "No se pudo consultar."); }
    finally { setCargando(false); }
  };

  const doc = documento.trim();

  // El endpoint /ventas/recuperar devuelve el boleto con un resumen del viaje
  // (fechas y precio) pero sin la ruta anidada, por lo que el itinerary se toma
  // de la reserva que lo emitio. Si no hay reserva asociada no se inventa texto.
  const rutaDeBoleto = (numero) => {
    const res = (data?.reservas || []).find((r) => (r.boletos || []).some((b) => b.numeroBoleto === numero));
    return res?.origen && res?.destino ? `${res.origen} → ${res.destino}` : null;
  };

  return (
    <div className="modal-overlay" onClick={onCerrar}>
      <div className="modal pago-modal" style={{ maxWidth: 560 }} onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <div className="modal-badge"><Search size={22} /></div>
          <div>
            <div className="modal-title">Consultar mis reservas</div>
            <div className="modal-subtitle">Recupera tus reservas y boletos con tu documento</div>
          </div>
          <button className="modal-close" onClick={onCerrar} aria-label="Cerrar"><X size={18} /></button>
        </div>
        <div className="modal-body">
          <form onSubmit={buscar} style={{ display: "flex", gap: 10, marginBottom: 16 }}>
            <input value={documento} onChange={(e) => setDocumento(e.target.value)} placeholder="DNI / RUC" style={inputStyle} inputMode="numeric" aria-label="Documento" />
            <button type="submit" className="btn-reservar" disabled={cargando} style={{ flexShrink: 0, padding: "0 18px", display: "flex", alignItems: "center", gap: 6 }}>
              {cargando ? <Loader2 size={15} className="spin-icon" /> : <Search size={15} />} Buscar
            </button>
          </form>
          {error && <div role="alert" style={{ color: "#fca5a5", fontSize: 13, marginBottom: 12 }}>{error}</div>}
          {data && (
            <>
              <h4 style={{ color: "white", fontSize: 13, marginBottom: 8 }}>Reservas ({data.reservas?.length || 0})</h4>
              {(!data.reservas || data.reservas.length === 0) ? (
                <p style={{ color: "var(--verde-medio)", fontSize: 12.5, marginBottom: 14 }}>No hay reservas con ese documento.</p>
              ) : (
                <div style={{ display: "flex", flexDirection: "column", gap: 8, marginBottom: 14 }}>
                  {data.reservas.map((r) => {
                    const e = etiquetaEstadoReserva(r.estado);
                    const puedeBoleto = r.validoParaEmbarque && r.boletos?.length;
                    const pdfUrl = puedeBoleto
                      ? `/api/v1/ventas/boleto/${encodeURIComponent(r.boletos[0].numeroBoleto)}/pdf?documento=${encodeURIComponent(doc)}`
                      : `/api/v1/ventas/retencion/${encodeURIComponent(r.referencia)}/constancia.pdf?documento=${encodeURIComponent(doc)}`;
                    return (
                      <div key={r.referencia} style={{ background: "rgba(255,255,255,0.04)", border: "1px solid rgba(138,166,163,0.2)", borderRadius: 10, padding: "10px 12px" }}>
                        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 4 }}>
                          <span style={{ fontFamily: "monospace", color: "var(--amarillo)", fontWeight: 700, fontSize: 13 }}>{r.referencia}</span>
                          <span style={{ color: e.color, fontWeight: 800, fontSize: 11 }}>{e.texto}</span>
                        </div>
                        <div style={{ color: "white", fontSize: 12.5 }}>{r.origen} → {r.destino} · {fmtFecha(r.fechaHoraSalida)}</div>
                        <div style={{ color: "var(--verde-medio)", fontSize: 11.5, marginTop: 2 }}>Asientos: {r.asientos?.map((a) => `N°${a.numeroAsiento}`).join(", ") || "—"} · S/ {Number(r.total || 0).toFixed(2)}</div>
                        {!r.validoParaEmbarque && (
                          <button type="button" onClick={() => abrirDocumento(pdfUrl)} style={{ marginTop: 8, background: "rgba(18,115,105,0.3)", border: "1px solid var(--verde)", borderRadius: 8, color: "white", fontWeight: 700, fontSize: 12, padding: "6px 12px", cursor: "pointer", display: "flex", alignItems: "center", gap: 6, fontFamily: "inherit" }}>
                            <FileText size={13} /> Constancia PDF
                          </button>
                        )}
                        {puedeBoleto && (
                          <button type="button" onClick={() => abrirDocumento(pdfUrl)} style={{ marginTop: 8, background: "linear-gradient(135deg, var(--verde), #0f9183)", border: "none", borderRadius: 8, color: "white", fontWeight: 700, fontSize: 12, padding: "6px 12px", cursor: "pointer", display: "flex", alignItems: "center", gap: 6, fontFamily: "inherit" }}>
                            <Ticket size={13} /> Boleto PDF
                          </button>
                        )}
                      </div>
                    );
                  })}
                </div>
              )}
              <h4 style={{ color: "white", fontSize: 13, marginBottom: 8 }}>Boletos ({data.boletos?.length || 0})</h4>
              {(!data.boletos || data.boletos.length === 0) ? (
                <p style={{ color: "var(--verde-medio)", fontSize: 12.5 }}>No hay boletos confirmados con ese documento.</p>
              ) : (
                <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
                  {data.boletos.map((b) => (
                    <div key={b.id || b.numeroBoleto} style={{ background: "rgba(18,115,105,0.12)", border: "1px solid rgba(18,115,105,0.3)", borderRadius: 10, padding: "10px 12px", display: "flex", justifyContent: "space-between", alignItems: "center", gap: 10 }}>
                      <div>
                        <div style={{ fontFamily: "monospace", color: "var(--amarillo)", fontWeight: 700, fontSize: 12.5 }}>{b.numeroBoleto}</div>
                        <div style={{ color: "var(--verde-medio)", fontSize: 11.5 }}>{rutaDeBoleto(b.numeroBoleto) ? `${rutaDeBoleto(b.numeroBoleto)} · ` : ""}Asiento N°{b.asiento?.numeroAsiento}</div>
                      </div>
                      <button type="button" onClick={() => abrirDocumento(`/api/v1/ventas/boleto/${encodeURIComponent(b.numeroBoleto)}/pdf?documento=${encodeURIComponent(doc)}`)} style={{ background: "var(--amarillo)", border: "none", borderRadius: 8, color: "var(--verde-oscuro)", fontWeight: 800, fontSize: 12, padding: "6px 12px", cursor: "pointer", display: "flex", alignItems: "center", gap: 6, fontFamily: "inherit", flexShrink: 0 }}>
                        <FileText size={13} /> PDF
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
}

function ViajeCard({ viaje, seleccionado, onSeleccionar }) {
  const origen = viaje?.ruta?.origen?.ciudad || "—";
  const destino = viaje?.ruta?.destino?.ciudad || "—";
  const imgDestino = getImagenDestino(destino);
  const asientosLibres = viaje.asientosDisponibles ?? "—";
  const totalAsientos = viaje.totalAsientos ?? "—";
  const pctOcupado = totalAsientos && asientosLibres !== "—" ? (1 - asientosLibres / totalAsientos) : 0;
  const colorAsientos = asientosLibres > 5 ? "var(--verde)" : asientosLibres > 0 ? "#e67e22" : "#e53e3e";

  return (
    <div style={{
      background: seleccionado ? "linear-gradient(135deg, rgba(18,115,105,0.25), rgba(16,64,59,0.35))" : "rgba(255,255,255,0.03)",
      border: `1.5px solid ${seleccionado ? "var(--verde)" : "rgba(138,166,163,0.15)"}`,
      borderRadius: 16, overflow: "hidden", transition: "all 0.25s",
      boxShadow: seleccionado ? "0 0 0 2px rgba(18,115,105,0.4), 0 8px 24px rgba(0,0,0,0.3)" : "0 2px 8px rgba(0,0,0,0.2)",
      display: "flex", flexDirection: "column",
    }}>
      {imgDestino ? (
        <div style={{ position: "relative", height: 110, overflow: "hidden" }}>
          <img src={imgDestino} alt={destino} style={{ width: "100%", height: "100%", objectFit: "cover", filter: "brightness(0.7)", transition: "all 0.3s" }} />
          <div style={{ position: "absolute", inset: 0, background: "linear-gradient(to top, rgba(16,64,59,0.95) 0%, rgba(16,64,59,0.2) 60%, transparent 100%)" }} />
          <div style={{ position: "absolute", top: 10, right: 10, background: "var(--amarillo)", color: "var(--verde-oscuro)", borderRadius: 20, padding: "4px 12px", fontWeight: 900, fontSize: 15, boxShadow: "0 2px 8px rgba(0,0,0,0.3)" }}>
            S/ {viaje.precioAdulto}
          </div>
          <div style={{ position: "absolute", bottom: 10, left: 14, display: "flex", alignItems: "center", gap: 6 }}>
            <span style={{ color: "white", fontFamily: "'Playfair Display', serif", fontWeight: 700, fontSize: 15, textShadow: "0 1px 4px rgba(0,0,0,0.8)" }}>{origen}</span>
            <ArrowRight size={13} color="var(--amarillo)" />
            <span style={{ color: "white", fontFamily: "'Playfair Display', serif", fontWeight: 700, fontSize: 15, textShadow: "0 1px 4px rgba(0,0,0,0.8)" }}>{destino}</span>
          </div>
        </div>
      ) : (
        <div style={{ background: seleccionado ? "rgba(18,115,105,0.3)" : "rgba(18,115,105,0.1)", padding: "0.8rem 1rem", display: "flex", alignItems: "center", justifyContent: "space-between", borderBottom: "1px solid rgba(138,166,163,0.1)" }}>
          <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
            <Navigation size={16} color="var(--amarillo)" />
            <span style={{ color: "white", fontFamily: "'Playfair Display', serif", fontWeight: 700, fontSize: 14 }}>{origen} → {destino}</span>
          </div>
          <span style={{ background: "var(--amarillo)", color: "var(--verde-oscuro)", borderRadius: 20, padding: "3px 10px", fontWeight: 900, fontSize: 14 }}>S/ {viaje.precioAdulto}</span>
        </div>
      )}
      <div style={{ padding: "0.9rem 1rem", flex: 1 }}>
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 10, marginBottom: 12 }}>
          <div style={{ display: "flex", alignItems: "flex-start", gap: 7 }}>
            <Clock size={14} color="var(--verde-medio)" style={{ marginTop: 2, flexShrink: 0 }} />
            <div>
              <div style={{ color: "var(--verde-medio)", fontSize: 10, textTransform: "uppercase", letterSpacing: "0.05em" }}>Salida</div>
              <div style={{ color: "white", fontWeight: 600, fontSize: 12 }}>{fmtFecha(viaje.fechaHoraSalida)}</div>
            </div>
          </div>
          <div style={{ display: "flex", alignItems: "flex-start", gap: 7 }}>
            <Bus size={14} color="var(--verde-medio)" style={{ marginTop: 2, flexShrink: 0 }} />
            <div>
              <div style={{ color: "var(--verde-medio)", fontSize: 10, textTransform: "uppercase", letterSpacing: "0.05em" }}>Bus</div>
              <div style={{ color: "white", fontWeight: 600, fontSize: 12 }}>{viaje.bus?.placa || "—"}</div>
              {viaje.bus?.tipo && <div style={{ color: "var(--verde-medio)", fontSize: 11 }}>{viaje.bus.tipo}</div>}
            </div>
          </div>
        </div>
        <div style={{ marginBottom: 12 }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 5 }}>
            <div style={{ display: "flex", alignItems: "center", gap: 5 }}>
              <Armchair size={13} color={colorAsientos} />
              <span style={{ color: "var(--verde-medio)", fontSize: 12 }}>Asientos disponibles</span>
            </div>
            <span style={{ color: colorAsientos, fontWeight: 700, fontSize: 13 }}>{asientosLibres} / {totalAsientos}</span>
          </div>
          <div style={{ height: 5, borderRadius: 3, background: "rgba(138,166,163,0.15)", overflow: "hidden" }}>
            <div style={{ height: "100%", borderRadius: 3, width: `${pctOcupado * 100}%`, background: colorAsientos, transition: "width 0.4s ease" }} />
          </div>
        </div>
        <button className="btn-reservar" style={{ width: "100%", padding: "9px 16px", fontSize: 13, display: "flex", gap: 7, alignItems: "center", justifyContent: "center", background: seleccionado ? "linear-gradient(135deg, var(--verde), #0f9183)" : "linear-gradient(135deg, var(--amarillo), #e6b800)", color: seleccionado ? "white" : "var(--verde-oscuro)" }} onClick={() => onSeleccionar(viaje)}>
          <Armchair size={15} /> {seleccionado ? "Cambiar asiento" : "Elegir asiento"}
        </button>
      </div>
    </div>
  );
}

export default function PasajesPageWrapper() {
  return <ErrorBoundary><PasajesPage /></ErrorBoundary>;
}

/** Destino de "Mi cuenta" según el rol. Antes se navegaba a /dashboard, ruta
 *  que no existe en App.jsx: el comodín '*' devolvía al inicio y perdía el
 *  contexto del panel. */
function rutaPanel(rol) {
  if (rol === "ROLE_ADMIN" || rol === "ROLE_CAJERO" || rol === "ROLE_VENDEDOR" || rol === "ROLE_OPERADOR_ENCOMIENDAS") return "/admin";
  if (rol === "ROLE_CHOFER") return "/chofer";
  return "/cliente";
}

function PasajesPage() {
  const { session } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const destinoParam = searchParams.get("destino") || "";
  const origenParam = searchParams.get("origen") || "";
  const fechaParam = searchParams.get("fecha") || "";
  const pasajerosParam = parseInt(searchParams.get("pasajeros"), 10);
  const tabParam = searchParams.get("tab");

  const headersRef = useRef(null);
  headersRef.current = session
    ? { "Content-Type": "application/json", Authorization: `Bearer ${session.token}` }
    : { "Content-Type": "application/json" };
  const headers = headersRef.current;

  const [sucursales, setSucursales] = useState([]);
  const [cargandoSucursales, setCargandoSucursales] = useState(true);
  const [errorSucursales, setErrorSucursales] = useState(false);
  const [origenId, setOrigenId] = useState("");
  const [destinoId, setDestinoId] = useState("");
  const [fecha, setFecha] = useState(fechaParam);
  const [pasajeros, setPasajeros] = useState(Number.isFinite(pasajerosParam) ? Math.min(Math.max(pasajerosParam, 1), 10) : 1);
  const [viajes, setViajes] = useState([]);
  const [buscando, setBuscando] = useState(false);
  const [buscado, setBuscado] = useState(false);
  const [viajeSelec, setViajeSelec] = useState(null);
  const [asientos, setAsientos] = useState([]);
  const [cargandoAsientos, setCargandoAsientos] = useState(false);
  const [asientosSel, setAsientosSel] = useState([]);
  const [modalMapa, setModalMapa] = useState(false);
  const [modalReserva, setModalReserva] = useState(false);
  const [modalRecuperar, setModalRecuperar] = useState(false);
  const [boletosOk, setBoletosOk] = useState(null);
  const [error, setError] = useState("");
  const [carrito, setCarrito] = useState([]);
  const [carritoAbierto, setCarritoAbierto] = useState(false);
  const [modalPagoCarrito, setModalPagoCarrito] = useState(false);
  const [procesandoCarrito, setProcesandoCarrito] = useState(false);
  const [pagoExitoso, setPagoExitoso] = useState(false);
  const [bannerExito, setBannerExito] = useState("¡Pago procesado con éxito! Revisa tu historial de compras.");
  const [tabActivo, setTabActivo] = useState(tabParam === "encomiendas" ? "encomiendas" : "pasajes");
  const timerPagoExitoso = useRef(null);
  const intencionRef = useRef(null);

  const cambiarTab = (key) => {
    setTabActivo(key);
    // Mantener la URL sincronizada para conservar la pestaña al iniciar sesión.
    navigate(key === "encomiendas" ? "/pasajes?tab=encomiendas" : "/pasajes", { replace: true });
  };

  useEffect(() => {
    setTabActivo(tabParam === "encomiendas" ? "encomiendas" : "pasajes");
  }, [tabParam]);

  // Evita actualizar estado tras desmontar el componente.
  useEffect(() => () => clearTimeout(timerPagoExitoso.current), []);

  const cargarSucursales = useCallback(async () => {
    setCargandoSucursales(true); setErrorSucursales(false);
    const urls = ["/api/v1/sucursales", "/api/v1/sucursales/terminales"];
    for (const url of urls) {
      try {
        const r = await fetch(url);
        if (!r.ok) { console.warn("GET", url, "→", r.status); continue; }
        const data = await r.json();
        const lista = Array.isArray(data) ? data
          : Array.isArray(data?.content) ? data.content : [];
        if (lista.length > 0) {
          const unicas = lista.filter((s, i, arr) => arr.findIndex(x => x.id === s.id) === i);
          setSucursales(unicas);
          setCargandoSucursales(false);
          return;
        }
        console.warn(url, "devolvió lista vacía");
      } catch (e) { console.warn("Error fetch", url, e.message); }
    }
    if (headersRef.current?.Authorization) {
      try {
        const r = await fetch("/api/v1/sucursales", { headers: headersRef.current });
        if (r.ok) {
          const data = await r.json();
          const lista = Array.isArray(data) ? data : Array.isArray(data?.content) ? data.content : [];
          if (lista.length > 0) { setSucursales(lista.filter((s, i, arr) => arr.findIndex(x => x.id === s.id) === i)); setCargandoSucursales(false); return; }
        }
      } catch (e) { console.warn("Error con token:", e.message); }
    }
    setErrorSucursales(true);
    setCargandoSucursales(false);
  }, []);

  useEffect(() => { cargarSucursales(); }, [cargarSucursales]);

  const buscarViajes = useCallback(async (oId, dId, fechaOverride) => {
    if (!oId || !dId) { setError("Selecciona origen y destino"); return; }
    if (String(oId) === String(dId)) { setError("El origen y destino no pueden ser iguales"); return; }
    const f = fechaOverride !== undefined ? fechaOverride : fecha;
    setError(""); setBuscando(true); setViajes([]); setViajeSelec(null); setAsientos([]); setAsientosSel([]);
    try {
      const paramsObj = { origenId: oId, destinoId: dId, page: 0, size: 20 };
      if (f) { paramsObj.desde = `${f}T00:00:00`; paramsObj.hasta = `${f}T23:59:59`; }
      const params = new URLSearchParams(paramsObj);
      const res = await fetch(`/api/v1/viajes/disponibles?${params}`);
      if (!res.ok) { const err = await res.json().catch(() => ({})); throw new Error(err.mensaje || err.message || `Error del servidor (${res.status})`); }
      const data = await res.json();
      const lista = Array.isArray(data) ? data : Array.isArray(data?.content) ? data.content : [];
      setViajes(lista); setBuscado(true);
    } catch (err) {
      setError(err.message || "Error al buscar viajes."); setViajes([]); setBuscado(true);
    } finally { setBuscando(false); }
  }, [fecha]);

  const buscar = (e) => { e.preventDefault(); buscarViajes(origenId, destinoId); };

  // Conserva el contexto del portal (ruta + búsqueda) al iniciar sesión.
  // Nunca se incluye el JWT ni datos sensibles en la URL.
  const irALogin = () => {
    try {
      // Guarda la intención (ruta, fecha, pasajeros y viaje si está elegido)
      // para restaurarla y revalidarla al volver autenticado.
      sessionStorage.setItem("iwt_intencion", JSON.stringify({
        origenId, destinoId, fecha, pasajeros,
        viajeId: viajeSelec?.id ?? null,
      }));
    } catch { /* almacenamiento no disponible */ }
    const returnTo = window.location.pathname + window.location.search;
    navigate(`/login?returnTo=${encodeURIComponent(returnTo)}`);
  };

  const hayIntencionPendiente = () => {
    try {
      const raw = sessionStorage.getItem("iwt_intencion");
      if (!raw) return false;
      const obj = JSON.parse(raw);
      return !!(obj && obj.viajeId);
    } catch { return false; }
  };

  const didAutoSearch = useRef(false);
  useEffect(() => {
    if (!destinoParam || !sucursales.length || didAutoSearch.current) return;
    if (hayIntencionPendiente()) return; // la restauración tras login tiene prioridad
    const lower = destinoParam.toLowerCase().replace(/%20/g, " ");
    const found = sucursales.find((s) =>
      s.ciudad?.toLowerCase().includes(lower) ||
      s.nombre?.toLowerCase().includes(lower) ||
      lower.includes(s.ciudad?.toLowerCase() || "____")
    );
    if (!found) return;
    const origenPref = origenParam
      ? sucursales.find((s) => String(s.id) === String(origenParam) || s.ciudad?.toLowerCase().includes(origenParam.toLowerCase()))
      : null;
    const ayacucho = sucursales.find((s) => s.ciudad?.toLowerCase().includes("ayacucho"));
    const primerOrigen = sucursales.find((s) => s.id !== found.id);
    const oId = origenPref && origenPref.id !== found.id
      ? String(origenPref.id)
      : ayacucho && ayacucho.id !== found.id
        ? String(ayacucho.id)
        : primerOrigen ? String(primerOrigen.id) : null;
    if (!oId) return;
    didAutoSearch.current = true;
    setOrigenId(oId);
    setDestinoId(String(found.id));
    buscarViajes(oId, String(found.id));
  }, [sucursales, destinoParam, origenParam, buscarViajes]);

  const seleccionarViaje = async (viaje) => {
    setViajeSelec(viaje); setAsientosSel([]); setCargandoAsientos(true); setModalMapa(true);
    try {
      const res = await fetch(`/api/v1/viajes/${viaje.id}/asientos`, { headers });
      const data = await res.json();
      setAsientos(Array.isArray(data) ? data : []);
    } catch { setAsientos([]); }
    finally { setCargandoAsientos(false); }
  };

  // Reconsulta la disponibilidad real del viaje (p. ej. tras vencer una retención).
  const refrescarAsientos = useCallback(async (viajeId) => {
    if (!viajeId) return;
    try {
      const res = await fetch(`/api/v1/viajes/${viajeId}/asientos`);
      if (res.ok) { const data = await res.json(); setAsientos(Array.isArray(data) ? data : []); }
    } catch { /* se reintentará al reabrir el mapa */ }
  }, []);

  const toggleAsiento = (asiento) => {
    const yaSeleccionado = asientosSel.find((a) => a.id === asiento.id);
    if (yaSeleccionado) {
      setAsientosSel((prev) => prev.filter((a) => a.id !== asiento.id));
      return;
    }
    if (asientosSel.length >= pasajeros) {
      setError(`Puedes seleccionar hasta ${pasajeros} asiento(s). Ajusta "Pasajeros" en la búsqueda si necesitas más.`);
      return;
    }
    setAsientosSel((prev) => [...prev, asiento]);
  };

  // Restaura la intención tras iniciar sesión: misma ruta/fecha y revalidación
  // del viaje seleccionado contra el backend.
  useEffect(() => {
    if (!session) return;
    let raw = null;
    try { raw = sessionStorage.getItem("iwt_intencion"); } catch { /* almacenamiento no disponible */ }
    if (!raw) return;
    try { sessionStorage.removeItem("iwt_intencion"); } catch { /* almacenamiento no disponible */ }
    let int = null;
    try { int = JSON.parse(raw); } catch { return; }
    if (!int?.viajeId || !int?.origenId || !int?.destinoId) return;
    didAutoSearch.current = true;
    setOrigenId(String(int.origenId));
    setDestinoId(String(int.destinoId));
    if (int.fecha) setFecha(int.fecha);
    if (int.pasajeros) setPasajeros(Math.min(Math.max(parseInt(int.pasajeros, 10) || 1, 1), 10));
    intencionRef.current = String(int.viajeId);
    buscarViajes(String(int.origenId), String(int.destinoId), int.fecha || "");
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [session]);

  // Reabre el viaje exacto una vez cargados los resultados; si ya no está
  // disponible, se informa y se conservan los viajes actualizados.
  useEffect(() => {
    const viajeId = intencionRef.current;
    if (!viajeId || !viajes.length) return;
    intencionRef.current = null;
    const viaje = viajes.find((v) => String(v.id) === String(viajeId));
    if (viaje) {
      seleccionarViaje(viaje);
    } else {
      setError("El viaje que habías seleccionado ya no está disponible. Te mostramos los viajes actualizados de la ruta.");
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [viajes]);

  const agregarPasajesAlCarrito = () => {
    if (!viajeSelec || !asientosSel.length) return;
    const precio = parseFloat(viajeSelec.precioAdulto || viajeSelec.precioOficial || viajeSelec.precio || 0);
    const nuevos = asientosSel.map((a) => ({ id: `pas-${Date.now()}-${a.id}`, tipo: "pasaje", descripcion: `${viajeSelec.ruta?.origen?.ciudad} → ${viajeSelec.ruta?.destino?.ciudad} · Asiento N°${a.numeroAsiento} · ${fmtFecha(viajeSelec.fechaHoraSalida)}`, precio, datos: { viajeId: viajeSelec.id, asientoId: a.id } }));
    setCarrito((prev) => [...prev, ...nuevos]);
    setCarritoAbierto(true); setModalMapa(false);
  };

  const confirmarPagoCarrito = async (metodoPago, datosExtra) => {
    if (!session) { irALogin(); return; }
    setProcesandoCarrito(true); setError("");
    const errores = [];
    try {
      for (const item of carrito.filter((i) => i.tipo === "pasaje")) {
        try {
          const body = { viajeId: item.datos.viajeId, asientoId: item.datos.asientoId, clienteId: session.clienteId || 0, metodoPago, referenciaPago: datosExtra.operacion || null, observaciones: `Carrito · ${metodoPago}` };
          const res = await fetch("/api/v1/boletos/comprar", { method: "POST", headers, body: JSON.stringify(body) });
          if (!res.ok) { const d = await res.json().catch(() => ({})); errores.push(d.mensaje || d.message || `Error en pasaje`); }
        } catch (e) { errores.push(e.message); }
      }
      if (errores.length) {
        // Antes, aunque todos los ítems fallaran, se vaciaba el carrito y se
        // mostraba el banner de "¡Compra exitosa!". Ahora, si hay errores, el
        // carrito se conserva íntegro y se informa qué ítems no se procesaron.
        setModalPagoCarrito(false);
        setError(`No se pudo completar la compra (${errores.length} de ${carrito.length} ítems fallaron): ${errores.join(" | ")}`);
        return;
      }
      setBannerExito("¡Pago procesado con éxito! Revisa tu historial de compras.");
      setCarrito([]); setModalPagoCarrito(false); setPagoExitoso(true);
      clearTimeout(timerPagoExitoso.current);
      timerPagoExitoso.current = setTimeout(() => setPagoExitoso(false), 6000);
    } catch (err) { setError(err.message || "Error inesperado."); }
    finally { setProcesandoCarrito(false); }
  };

  const getPrecioViaje = (viaje) => parseFloat(viaje?.precioAdulto ?? viaje?.precioOficial ?? viaje?.precio ?? 0);

  return (
    <div style={{ minHeight: "100vh", background: "var(--fondo)" }}>
      <nav className="nav">
        <div className="nav-logo" style={{ cursor: "pointer" }} onClick={() => navigate("/")}>
          <LogoSVG width={44} height={44} />
          <div className="nav-brand">INTIWATANA S.R.L.<span>Transportes</span></div>
        </div>
        <ul className="nav-links hidden md:flex items-center">
          <li><a onClick={() => navigate("/")} style={{ cursor: "pointer" }}>Inicio</a></li>
          <li><a onClick={() => cambiarTab("pasajes")} style={{ cursor: "pointer", color: tabActivo === "pasajes" ? "var(--amarillo)" : undefined, display: "flex", alignItems: "center", gap: 6 }}><Ticket size={15} /> Pasajes</a></li>
          <li><a onClick={() => cambiarTab("encomiendas")} style={{ cursor: "pointer", color: tabActivo === "encomiendas" ? "var(--amarillo)" : undefined, display: "flex", alignItems: "center", gap: 6 }}><Package size={15} /> Encomiendas</a></li>
        </ul>
        <div style={{ display: "flex", gap: 10, alignItems: "center" }}>
          <button onClick={() => setModalRecuperar(true)} style={{ background: "rgba(255,255,255,0.06)", border: "1.5px solid rgba(138,166,163,0.2)", borderRadius: 10, color: "var(--verde-medio)", cursor: "pointer", padding: "0.45rem 0.9rem", fontSize: 13, fontWeight: 700, display: "flex", alignItems: "center", gap: 6, transition: "all 0.2s" }}>
            <FileText size={16} /> Mis reservas
          </button>
          <button onClick={() => setCarritoAbierto(true)} style={{ position: "relative", background: carrito.length > 0 ? "rgba(245,197,24,0.15)" : "rgba(255,255,255,0.06)", border: carrito.length > 0 ? "1.5px solid rgba(245,197,24,0.5)" : "1.5px solid rgba(138,166,163,0.2)", borderRadius: 10, color: carrito.length > 0 ? "var(--amarillo)" : "var(--verde-medio)", cursor: "pointer", padding: "0.45rem 0.9rem", fontSize: 13, fontWeight: 700, display: "flex", alignItems: "center", gap: 6, transition: "all 0.2s" }}>
            <ShoppingCart size={16} /> Carrito
            {carrito.length > 0 && <span style={{ background: "var(--amarillo)", color: "var(--verde-oscuro)", borderRadius: 50, fontSize: 10, fontWeight: 900, padding: "1px 6px", marginLeft: 2 }}>{carrito.length}</span>}
          </button>
          {session ? (
            <button className="btn-admin-nav btn-registrarse" onClick={() => navigate(rutaPanel(session.rol))} style={{ display: "flex", alignItems: "center", gap: 6 }}>
              <LayoutDashboard size={15} /> {session.nombreCompleto?.split(" ")[0] || "Mi cuenta"}
            </button>
          ) : (
            <button className="btn-admin-nav btn-registrarse" onClick={() => irALogin()} style={{ display: "flex", alignItems: "center", gap: 6 }}>
              <LogIn size={15} /> Entrar
            </button>
          )}
        </div>
      </nav>

      {pagoExitoso && (
        <div style={{ background: "linear-gradient(135deg, rgba(18,115,105,0.98), rgba(16,64,59,0.99))", border: "1px solid rgba(245,197,24,0.5)", padding: "1rem 2rem", display: "flex", alignItems: "center", gap: 16, justifyContent: "center" }}>
          <CheckCircle size={20} color="var(--amarillo)" />
          <span style={{ color: "white", fontWeight: 700, fontSize: 15 }}>{bannerExito}</span>
          <button onClick={() => setPagoExitoso(false)} style={{ background: "transparent", border: "none", color: "var(--verde-medio)", cursor: "pointer" }}><X size={16} /></button>
        </div>
      )}

      {error && (
        <div style={{ background: "rgba(220,38,38,0.1)", border: "1px solid rgba(220,38,38,0.3)", padding: "0.8rem 2rem", display: "flex", alignItems: "center", gap: 12, justifyContent: "center" }}>
          <AlertTriangle size={18} color="#fca5a5" />
          <span style={{ color: "#fca5a5", fontSize: 14 }}>{error}</span>
          <button onClick={() => setError("")} style={{ background: "transparent", border: "none", color: "#fca5a5", cursor: "pointer" }}><X size={16} /></button>
        </div>
      )}

      <div style={{ background: "linear-gradient(135deg, var(--verde-oscuro) 0%, var(--verde) 100%)", padding: "2.5rem 2rem 3.5rem", textAlign: "center", position: "relative", overflow: "hidden" }}>
        <div style={{ display: "inline-flex", background: "rgba(0,0,0,0.25)", borderRadius: 50, padding: "4px", gap: 4, marginBottom: "1.5rem", position: "relative", zIndex: 1 }}>
          {[
            { key: "pasajes", label: "Comprar Pasaje", icon: <Ticket size={15} /> },
            { key: "encomiendas", label: "Solicitar envío", icon: <Package size={15} /> },
          ].map((tab) => (
            <button key={tab.key} onClick={() => cambiarTab(tab.key)} style={{ background: tabActivo === tab.key ? "var(--amarillo)" : "transparent", color: tabActivo === tab.key ? "var(--verde-oscuro)" : "rgba(255,255,255,0.75)", border: "none", borderRadius: 50, padding: "0.55rem 1.4rem", fontSize: 14, fontWeight: 700, cursor: "pointer", fontFamily: "inherit", transition: "all 0.25s", display: "flex", alignItems: "center", gap: 7 }}>
              {tab.icon} {tab.label}
            </button>
          ))}
        </div>

        {tabActivo === "pasajes" && (
          <>
            <h1 style={{ fontFamily: "'Playfair Display', serif", fontSize: "2.2rem", color: "white", marginBottom: "0.5rem", position: "relative", zIndex: 1 }}>Busca tu viaje</h1>
            <p style={{ color: "rgba(255,255,255,0.75)", marginBottom: "2rem", position: "relative", zIndex: 1 }}>Selecciona tu origen y destino para ver los viajes disponibles</p>
            {cargandoSucursales ? (
              <p style={{ color: "rgba(255,255,255,0.6)", fontSize: 14, display: "flex", alignItems: "center", justifyContent: "center", gap: 8 }}><span className="spin-icon"><Loader2 size={16} /></span> Cargando terminales...</p>
            ) : errorSucursales ? (
              <div style={{ color: "#fca5a5", fontSize: 14, display: "flex", alignItems: "center", justifyContent: "center", gap: 8 }}>
                <AlertTriangle size={16} /> No se pudieron cargar las sucursales.{" "}
                <button onClick={cargarSucursales} style={{ background: "none", border: "none", color: "var(--amarillo)", cursor: "pointer", fontWeight: 700 }}>Reintentar</button>
              </div>
            ) : (
              <form onSubmit={buscar} translate="no" style={{ background: "white", borderRadius: 60, padding: "0.5rem 0.5rem 0.5rem 1.2rem", display: "inline-flex", gap: "0.8rem", alignItems: "center", maxWidth: 660, width: "100%", boxShadow: "0 8px 32px rgba(16,64,59,0.4)", position: "relative", zIndex: 1 }}>
                <MapPin size={18} color="var(--verde-oscuro)" />
                <select value={origenId} onChange={(e) => setOrigenId(e.target.value)} required style={{ border: "none", outline: "none", flex: 1, fontSize: 14, color: "var(--verde-oscuro)", background: "transparent", padding: "0.5rem 0" }}>
                  <option value="">Origen</option>
                  {sucursales.map((s) => <option key={s.id} value={s.id}>{s.ciudad}</option>)}
                </select>
                <div style={{ width: 1, height: 28, background: "#ddd" }} />
                <Flag size={18} color="var(--verde-oscuro)" />
                <select value={destinoId} onChange={(e) => setDestinoId(e.target.value)} required style={{ border: "none", outline: "none", flex: 1, fontSize: 14, color: "var(--verde-oscuro)", background: "transparent", padding: "0.5rem 0" }}>
                  <option value="">Destino</option>
                  {sucursales.filter((s) => s.id !== Number(origenId)).map((s) => <option key={s.id} value={s.id}>{s.ciudad}</option>)}
                </select>
                <div style={{ width: 1, height: 28, background: "#ddd" }} />
                <Calendar size={18} color="var(--verde-oscuro)" />
                <input type="date" value={fecha} onChange={(e) => setFecha(e.target.value)} style={{ border: "none", outline: "none", flex: 1, fontSize: 14, color: "var(--verde-oscuro)", background: "transparent", padding: "0.5rem 0", fontFamily: "inherit" }} />
                {/* FIXED: sin position alternante en el spinner */}
                <button type="submit" className="btn-buscar" disabled={buscando}
                  style={{ borderRadius: 50, padding: "0.7rem 1.5rem", flexShrink: 0, minWidth: 100, display: "flex", alignItems: "center", justifyContent: "center", gap: 6 }}>
                  {buscando
                    ? <span className="spin-icon"><Loader2 size={16} /></span>
                    : <><Search size={16} /> Buscar</>
                  }
                </button>
              </form>
            )}
          </>
        )}

        {tabActivo === "encomiendas" && (
          <>
            <h1 style={{ fontFamily: "'Playfair Display', serif", fontSize: "2.2rem", color: "white", marginBottom: "0.5rem", position: "relative", zIndex: 1 }}>Solicita tu envío</h1>
            <p style={{ color: "rgba(255,255,255,0.75)", marginBottom: "0.5rem", position: "relative", zIndex: 1 }}>Registra tu solicitud; el personal de agencia confirma el peso y la tarifa oficial</p>
          </>
        )}
      </div>

      <div style={{ maxWidth: 1100, margin: "0 auto", padding: "2rem 1.5rem" }}>
        {tabActivo === "encomiendas" && (
          <div>
            {!session && (
              <div style={{ background: "rgba(245,197,24,0.08)", border: "1px solid rgba(245,197,24,0.3)", borderRadius: 12, padding: "1rem 1.4rem", marginBottom: "1.5rem", display: "flex", alignItems: "center", gap: 12 }}>
                <Info size={20} color="var(--amarillo)" />
                <div style={{ flex: 1, fontSize: 13, color: "var(--amarillo)" }}>
                  <strong>Inicia sesión</strong> para guardar tus encomiendas y hacer seguimiento.{" "}
                  <button onClick={() => irALogin()} style={{ background: "none", border: "none", color: "white", cursor: "pointer", fontWeight: 700, textDecoration: "underline" }}>Entrar aquí</button>
                </div>
              </div>
            )}
            <div style={{ display: "flex", alignItems: "center", gap: 12, marginBottom: "1.5rem" }}>
              <Package size={28} color="var(--amarillo)" />
              <div>
                <h2 style={{ fontFamily: "'Playfair Display', serif", color: "var(--blanco)", fontSize: "1.5rem", marginBottom: 2 }}>Solicitar envío de encomienda</h2>
                <p style={{ color: "var(--verde-medio)", fontSize: 13 }}>Completa el formulario; la tarifa final se confirma en agencia al recibir el paquete</p>
              </div>
            </div>
            <div style={{ display: "grid", gridTemplateColumns: "1fr 300px", gap: "1.5rem", alignItems: "start" }}>
              <FormEncomienda sucursales={sucursales} session={session} onIrLogin={irALogin} />
              <div style={{ background: "rgba(18,115,105,0.1)", border: "1px solid rgba(18,115,105,0.2)", borderRadius: 14, padding: "1.2rem" }}>
                <h3 style={{ color: "white", fontSize: 14, fontWeight: 700, marginBottom: 12, display: "flex", alignItems: "center", gap: 7 }}><Info size={16} color="var(--amarillo)" /> ¿Cómo funciona?</h3>
                {[
                  "Registra los datos iniciales de tu envío.",
                  "Presenta el paquete en una agencia INTIWATANA.",
                  "El peso, la tarifa y la guía oficial serán confirmados por el personal autorizado al recibir el paquete.",
                  "Realizas el pago en agencia y sigues tu envío con el número de guía.",
                ].map((paso, i) => (
                  <div key={i} style={{ display: "flex", gap: 10, padding: "8px 0", borderBottom: "1px solid rgba(138,166,163,0.15)" }}>
                    <span style={{ color: "var(--amarillo)", fontWeight: 800, fontSize: 13 }}>{i + 1}</span>
                    <span style={{ color: "white", fontSize: 12.5, lineHeight: 1.45 }}>{paso}</span>
                  </div>
                ))}
                <p style={{ color: "var(--verde-medio)", fontSize: 11, marginTop: 10, lineHeight: 1.4 }}>La tarifa final se confirma en agencia al recibir el paquete.</p>
              </div>
            </div>
          </div>
        )}

        {tabActivo === "pasajes" && (
          <>
            {buscando && (
              <div style={{ textAlign: "center", padding: "3rem 1rem", color: "var(--verde-medio)", fontSize: 15 }}>
                <span className="spin-icon"><Loader2 size={40} style={{ marginBottom: 16, opacity: 0.6 }} /></span>
                <p style={{ fontWeight: 600 }}>Buscando viajes disponibles...</p>
              </div>
            )}
            {buscado && !buscando && (
              <>
                <div style={{ display: "flex", alignItems: "center", gap: 12, marginBottom: 24 }}>
                  <Bus size={24} color="var(--amarillo)" />
                  <h2 style={{ fontFamily: "'Playfair Display', serif", color: "var(--blanco)", fontSize: "1.4rem", margin: 0 }}>
                    {viajes.length === 0 ? "Sin viajes para esta ruta" : `${viajes.length} viaje(s) disponible(s)`}
                  </h2>
                </div>
                {viajes.length > 0 && (
                  <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(280px, 1fr))", gap: "1.2rem" }}>
                    {viajes.map((v) => <ViajeCard key={v.id} viaje={v} seleccionado={viajeSelec?.id === v.id} onSeleccionar={seleccionarViaje} />)}
                  </div>
                )}
                {viajes.length === 0 && (
                  <div style={{ textAlign: "center", padding: "3rem 1rem", color: "var(--verde-medio)" }}>
                    <Frown size={48} style={{ marginBottom: 12, opacity: 0.5 }} />
                    <p style={{ fontWeight: 600, color: "white", marginBottom: 8 }}>No encontramos viajes disponibles</p>
                    <p style={{ fontSize: 13 }}>Prueba con otra fecha o diferente combinación de origen/destino.</p>
                  </div>
                )}
              </>
            )}
          </>
        )}
      </div>

      {modalMapa && viajeSelec && createPortal(
        <div className="modal-overlay" onClick={() => setModalMapa(false)}>
          <div className="modal" style={{ maxWidth: 800, width: "95%" }} onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div className="modal-badge"><Armchair size={22} /></div>
              <div>
                <div className="modal-title">Elige tus asientos</div>
                <div className="modal-subtitle">Combi 15 Pasajeros · {viajeSelec.ruta?.origen?.ciudad} → {viajeSelec.ruta?.destino?.ciudad} · Selecciona hasta {pasajeros} asiento(s) ({asientosSel.length}/{pasajeros})</div>
              </div>
              <button className="modal-close" onClick={() => setModalMapa(false)}><X size={18} /></button>
            </div>
            <div className="modal-body" style={{ background: "var(--fondo)" }}>
              {cargandoAsientos ? (
                <div style={{ textAlign: "center", padding: "2.5rem", color: "var(--verde-medio)", display: "flex", alignItems: "center", justifyContent: "center", gap: 10 }}>
                  <span className="spin-icon"><Loader2 size={20} /></span> Cargando mapa de asientos...
                </div>
              ) : (
                <div style={{ display: "grid", gridTemplateColumns: "minmax(300px, 1fr) 300px", gap: "1.5rem", alignItems: "start" }}>
                  <MapaAsientos asientos={asientos} seleccionados={asientosSel} onSeleccionar={toggleAsiento} />
                  <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
                    <div style={{ background: "rgba(18,115,105,0.12)", border: "1px solid rgba(18,115,105,0.25)", borderRadius: 12, padding: "1.2rem" }}>
                      <h3 style={{ color: "white", fontWeight: 600, marginBottom: 16, fontSize: 16, borderBottom: "1px solid rgba(138,166,163,0.2)", paddingBottom: 10 }}>Resumen de selección</h3>
                      {(() => {
                        const selList = asientosSel;
                        const precioUnit = getPrecioViaje(viajeSelec);
                        const totalSel = selList.length * precioUnit;
                        return selList.length === 0 ? (
                          <>
                            <p style={{ color: "var(--verde-medio)", fontSize: 13, fontStyle: "italic" }}>No has seleccionado ningún asiento aún.</p>
                            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginTop: 16, paddingTop: 12, borderTop: "1px solid rgba(138,166,163,0.2)" }}>
                              <span style={{ color: "var(--verde-medio)", fontSize: 13 }}>Total (0 asientos)</span>
                              <span style={{ color: "var(--amarillo)", fontWeight: 800, fontSize: 20 }}>S/ 0.00</span>
                            </div>
                          </>
                        ) : (
                          <>
                            <div style={{ display: "flex", flexDirection: "column", gap: 6, maxHeight: 160, overflowY: "auto" }}>
                              {selList.map((a) => (
                                <div key={a.id} style={{ display: "flex", justifyContent: "space-between", alignItems: "center", fontSize: 13 }}>
                                  <span style={{ color: "var(--verde-medio)", display: "flex", alignItems: "center", gap: 5 }}><Armchair size={13} /> Asiento {a.numeroAsiento}</span>
                                  <span style={{ color: "white", fontWeight: 700 }}>S/ {precioUnit.toFixed(2)}</span>
                                </div>
                              ))}
                            </div>
                            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginTop: 16, paddingTop: 12, borderTop: "1px solid rgba(138,166,163,0.2)" }}>
                              <span style={{ color: "var(--verde-medio)", fontSize: 13 }}>Total ({selList.length} asientos)</span>
                              <span style={{ color: "var(--amarillo)", fontWeight: 800, fontSize: 20 }}>S/ {totalSel.toFixed(2)}</span>
                            </div>
                          </>
                        );
                      })()}
                    </div>
                    {!session && (
                      <div style={{ background: "rgba(245,197,24,0.08)", border: "1px solid rgba(245,197,24,0.25)", borderRadius: 10, padding: "10px 14px", fontSize: 13, color: "var(--amarillo)", display: "flex", alignItems: "center", gap: 8 }}>
                        <Info size={15} /> Puedes reservar y pagar como <strong style={{ color: "white" }}>invitado</strong>, sin crear una cuenta.
                      </div>
                    )}
                    <button className="btn-reservar" style={{ width: "100%", padding: "0.9rem", display: "flex", justifyContent: "center", alignItems: "center", gap: 8, opacity: asientosSel.length === 0 ? 0.5 : 1 }} disabled={asientosSel.length === 0} onClick={() => setModalReserva(true)}>
                      <CreditCard size={17} /> Reservar y pagar
                    </button>
                    <button style={{ width: "100%", padding: "0.9rem", display: "flex", justifyContent: "center", alignItems: "center", gap: 8, background: "rgba(18,115,105,0.4)", border: "1px solid var(--verde)", borderRadius: 10, color: "white", fontWeight: 700, cursor: "pointer", fontFamily: "inherit", opacity: asientosSel.length === 0 ? 0.5 : 1 }} disabled={asientosSel.length === 0} onClick={agregarPasajesAlCarrito}>
                      <ShoppingCart size={17} /> Agregar al carrito
                    </button>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
        , document.body)}

      {createPortal(
        <CarritoSidebar items={carrito} onRemove={(id) => setCarrito((prev) => prev.filter((i) => i.id !== id))} onCheckout={() => { if (!session) { irALogin(); return; } setCarritoAbierto(false); setModalPagoCarrito(true); }} open={carritoAbierto} onClose={() => setCarritoAbierto(false)} />,
        document.body
      )}

      {modalReserva && viajeSelec && createPortal(
        <ModalReserva
          viaje={viajeSelec}
          asientosSeleccionados={asientosSel}
          session={session}
          onCerrar={() => { setModalReserva(false); setAsientosSel([]); }}
          onReservaCreada={(data) => {
            setAsientos((prev) => prev.map((a) => asientosSel.some((s) => s.id === a.id) ? { ...a, estado: "RESERVADO" } : a));
            setModalMapa(false);
            if (data?.validoParaEmbarque && data.boletos?.length) {
              setModalReserva(false);
              setAsientosSel([]);
              setBoletosOk(data.boletos);
            }
          }}
          onVencida={() => { setModalReserva(false); setAsientosSel([]); refrescarAsientos(viajeSelec?.id); }}
        />, document.body)}
      {modalRecuperar && createPortal(<ModalRecuperar onCerrar={() => setModalRecuperar(false)} />, document.body)}
      {modalPagoCarrito && createPortal(<ModalPago itemsCarrito={carrito} onConfirmar={confirmarPagoCarrito} onCerrar={() => setModalPagoCarrito(false)} comprando={procesandoCarrito} />, document.body)}
      {boletosOk && !modalReserva && !modalMapa && createPortal(<ModalBoardingPass boletos={boletosOk} viaje={viajeSelec} onCerrar={() => { setBoletosOk(null); }} />, document.body)}
    </div>
  );
} 
