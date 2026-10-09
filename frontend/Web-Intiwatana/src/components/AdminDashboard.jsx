import { useState, useEffect, useCallback, useRef, useMemo, Component } from "react";
import { useAuth } from "../context/useAuth";
import {
  LayoutDashboard, Building2, Map, Menu, Bus, CalendarDays, Users, Package,
  Route, LogOut, CheckCircle2, Ticket, Shield, Search, Plus, Pencil, Trash2,
  RefreshCw, X, ChevronLeft, ChevronRight, Navigation, Armchair, UserCircle,
  Filter, AlertCircle, Info, CreditCard, Zap, AlertTriangle, Scale,
  MapPin, Flag, Clock, ArrowRight, BarChart3, Download, TrendingUp, TrendingDown,
  LayoutTemplate, Smartphone, Upload, Wallet, CheckCircle, XCircle,
} from "lucide-react";
import LogoSVG from "./LogoSVG";
import CrudAsientos from "./CrudAsientos";
import ReportesPage from "./ReportesPage";
import PortalGestion from "./PortalGestion";
import { MapaAsientos } from "./PasajesPage";
import VentaPresencial from "./caja/VentaPresencial";
import BoletosCaja from "./caja/BoletosCaja";
import OperacionesCaja from "./caja/OperacionesCaja";
import CierreCaja from "./caja/CierreCaja";

class ErrorBoundary extends Component {
  constructor(props) { super(props); this.state = { hasError: false, error: null }; }
  static getDerivedStateFromError(error) { return { hasError: true, error }; }
  componentDidCatch(error, info) { console.error("AdminDashboard error:", error, info); }
  render() {
    if (this.state.hasError) return (
      <div style={{ padding:"2rem", textAlign:"center" }}>
        <AlertCircle size={40} color={C.rojo} style={{ marginBottom:12 }}/>
        <h3 style={{ color:C.rojo, marginBottom:8 }}>Error al cargar este módulo</h3>
        <p style={{ color:C.gris, fontSize:13, marginBottom:16 }}>{this.state.error?.message || "Error inesperado"}</p>
        <button onClick={() => this.setState({ hasError:false, error:null })} style={{ background:C.verde, color:"white", border:"none", borderRadius:8, padding:"0.6rem 1.4rem", cursor:"pointer", fontWeight:700 }}>
          <RefreshCw size={14} style={{ marginRight:6 }}/>Reintentar
        </button>
      </div>
    );
    return this.props.children;
  }
}

const C = {
  verde:"#127369", oscuro:"#0B3D38", amarillo:"#F5C518",
  medio:"#667875", gris:"#17312E", grisClaro:"#DDE7E4",
  fondo:"#F4F7F6", blanco:"#ffffff", rojo:"#C84C4C", naranja:"#D58B15",
};

function fmtFecha(iso) {
  if (!iso) return "—";
  try {
    const d = new Date(iso); if (isNaN(d.getTime())) return "—";
    return d.toLocaleString("es-PE",{day:"2-digit",month:"short",year:"numeric",hour:"2-digit",minute:"2-digit"});
  } catch { return "—"; }
}

function useCountUp(target, duration=900) {
  const [val, setVal] = useState(0);
  useEffect(() => {
    // El primer frame se agenda siempre, incluso si target es 0 o no viene:
    // así el estado nunca se actualiza de forma síncrona dentro del efecto y
    // además se cancela el frame si el valor cambia antes de terminar.
    let frame = 0;
    let start = null;
    const step = (ts) => {
      if (!start) start = ts;
      const p = Math.min((ts - start) / duration, 1);
      setVal(Math.floor(p * (target || 0)));
      if (p < 1) frame = requestAnimationFrame(step);
    };
    frame = requestAnimationFrame(step);
    return () => cancelAnimationFrame(frame);
  },[target,duration]);
  return val;
}
function AnimNum({ value }) { const n=useCountUp(value); return <>{n}</>; }

function Badge({ text }) {
  const map = {
    PROGRAMADO:{bg:"rgba(18,115,105,0.10)",c:"#0e5e56"},EN_CURSO:{bg:"rgba(245,197,24,0.22)",c:"#99780c"},
    FINALIZADO:{bg:"rgba(21,132,99,0.14)",c:"#158463"},CANCELADO:{bg:"rgba(200,76,76,0.12)",c:C.rojo},
    RETENIDO:{bg:"rgba(213,139,21,0.15)",c:"#a0650f"},PENDIENTE_PAGO:{bg:"rgba(213,139,21,0.15)",c:"#a0650f"},
    CONFIRMADO:{bg:"rgba(21,132,99,0.14)",c:"#158463"},VENCIDO:{bg:"rgba(200,76,76,0.12)",c:C.rojo},
    EMITIDO:{bg:"rgba(18,115,105,0.10)",c:"#0e5e56"},PAGADO:{bg:"rgba(21,132,99,0.14)",c:"#158463"},
    ACTIVO:{bg:"rgba(18,115,105,0.10)",c:"#0e5e56"},USADO:{bg:"rgba(21,132,99,0.14)",c:"#158463"},
    SOLICITUD_PENDIENTE:{bg:"rgba(213,139,21,0.15)",c:"#a96807"},
    RECIBIDO:{bg:"rgba(245,197,24,0.22)",c:"#99780c"},EN_TRANSITO:{bg:"rgba(18,115,105,0.10)",c:"#0e5e56"},
    ENTREGADO:{bg:"rgba(21,132,99,0.14)",c:"#158463"},NORMAL:{bg:"rgba(18,115,105,0.10)",c:"#0e5e56"},
    ECONOMICO:{bg:"rgba(18,115,105,0.10)",c:"#0e5e56"},SEMI_CAMA:{bg:"rgba(245,197,24,0.22)",c:"#99780c"},
    CAMA:{bg:"rgba(102,120,117,0.18)",c:"#43534f"},CAMA_SUITE:{bg:"rgba(148,103,189,0.15)",c:"#7b2d8b"},
    PERSONA:{bg:"rgba(18,115,105,0.10)",c:"#0e5e56"},EMPRESA:{bg:"rgba(245,197,24,0.22)",c:"#99780c"},
    ROLE_ADMIN:{bg:"rgba(200,76,76,0.12)",c:C.rojo},ROLE_CAJERO:{bg:"rgba(18,115,105,0.10)",c:"#0e5e56"},
    ROLE_VENDEDOR:{bg:"rgba(245,197,24,0.22)",c:"#99780c"},ROLE_OPERADOR_ENCOMIENDAS:{bg:"rgba(148,103,189,0.15)",c:"#7b2d8b"},
    ROLE_CHOFER:{bg:"rgba(245,197,24,0.22)",c:"#99780c"},ROLE_CLIENTE:{bg:"rgba(102,120,117,0.16)",c:"#43534f"},
    COMPLETO:{bg:"rgba(21,132,99,0.14)",c:"#158463"},INCOMPLETO:{bg:"rgba(200,76,76,0.12)",c:C.rojo},
  };
  const s = map[text] || {bg:"rgba(102,120,117,0.14)",c:"#43534f"};
  return <span className="adm-badge" style={{background:s.bg,color:s.c}}>
    <span>{text?.replace(/_/g," ") || "—"}</span>
  </span>;
}

function StatCard({ icon, valor, label, color, sub, onClick }) {
  const [hover, setHover] = useState(false);
  return (
    <div onClick={onClick} onMouseEnter={()=>setHover(true)} onMouseLeave={()=>setHover(false)}
      className="adm-stat" role={onClick?"button":undefined} tabIndex={onClick?0:undefined}
      onKeyDown={onClick?(e)=>{if(e.key==="Enter"||e.key===" "){e.preventDefault();onClick();}}:undefined}
      aria-label={onClick?`Abrir módulo ${label}`:undefined}
      style={{border:`1px solid ${hover?color+"55":"#DDE7E4"}`,boxShadow:hover?`0 12px 32px ${color}1c`:"0 1px 3px rgba(16,64,59,0.06)",background:hover?`linear-gradient(180deg,#FFFFFF,${color}08)`:C.blanco,cursor:onClick?"pointer":"default"}}>
      <div className="adm-stat-icon" style={{color,background:`${color}16`}}>{icon}</div>
      <div className="adm-stat-val"><AnimNum value={valor}/></div>
      <div className="adm-stat-label">{label}</div>
      {sub && <div className="adm-stat-sub">{sub}</div>}
    </div>
  );
}

function DonutChart({ data, size=130 }) {
  if (!data?.length) return null;
  const colores=[C.verde,C.amarillo,C.rojo,C.naranja,"#9b59b6","#3498db"];
  const total=data.reduce((s,d)=>s+(d.valor||0),0)||1;
  // Los arcos se acumulan con un bucle en el cuerpo del render en lugar de
  // reasignar una variable desde el callback de map().
  const arcos=[];
  let acum=0;
  for (let i=0;i<data.length;i++){
    const d=data[i];
    const pct=(d.valor||0)/total;
    const ini=acum; acum+=pct;
    if (pct<0.005) continue;
    const a1=ini*2*Math.PI-Math.PI/2, a2=acum*2*Math.PI-Math.PI/2;
    const R=size/2-10, r=R-22, cx=size/2, cy=size/2;
    const x1e=cx+R*Math.cos(a1), y1e=cy+R*Math.sin(a1);
    const x2e=cx+R*Math.cos(a2), y2e=cy+R*Math.sin(a2);
    const x1i=cx+r*Math.cos(a2), y1i=cy+r*Math.sin(a2);
    const x2i=cx+r*Math.cos(a1), y2i=cy+r*Math.sin(a1);
    const grande=pct>0.5?1:0;
    arcos.push(<path key={i} d={`M${x1e},${y1e} A${R},${R} 0 ${grande},1 ${x2e},${y2e} L${x1i},${y1i} A${r},${r} 0 ${grande},0 ${x2i},${y2i} Z`} fill={colores[i%colores.length]} opacity={0.88}/>);
  }
  return (
    <div style={{display:"flex",alignItems:"center",gap:16,flexWrap:"wrap"}}>
      <svg viewBox={`0 0 ${size} ${size}`} width={size} height={size}>{arcos}</svg>
      <div style={{display:"flex",flexDirection:"column",gap:5}}>
        {data.map((d,i)=>(<div key={i} style={{display:"flex",alignItems:"center",gap:7,fontSize:12}}><div style={{width:10,height:10,borderRadius:3,background:colores[i%colores.length],flexShrink:0}}/><span style={{color:C.gris}}>{d.nombre}</span><strong style={{color:C.oscuro,marginLeft:"auto",paddingLeft:8}}>{d.valor}</strong></div>))}
      </div>
    </div>
  );
}

function ProgressBar({ value, max, color }) {
  const pct = max>0?Math.round((value/max)*100):0;
  return (
    <div style={{display:"flex",alignItems:"center",gap:8}}>
      <div style={{flex:1,height:6,background:"#e8f0ee",borderRadius:99,overflow:"hidden"}}>
        <div style={{height:"100%",width:`${pct}%`,background:color,borderRadius:99,transition:"width 1s"}}/>
      </div>
      <span style={{fontSize:11,fontWeight:700,color:C.gris,minWidth:28}}>{pct}%</span>
    </div>
  );
}

// ── Shared UI components ──────────────────────────────
export function PageHeader({ title, sub, action }) {
  return (
    <div className="adm-topbar">
      <div className="adm-head">
        <span className="adm-head-rule" aria-hidden="true"/>
        <div><h2 className="adm-page-title">{title}</h2>{sub&&<p className="adm-page-sub">{sub}</p>}</div>
      </div>
      {action && <div className="adm-head-actions">{action}</div>}
    </div>
  );
}
function SearchBar({ value, onChange, placeholder }) {
  return (
    <div className="adm-search-row" style={{display:"flex",alignItems:"center",gap:8}}>
      <Search size={15} color={C.medio} style={{position:"absolute",marginLeft:12,pointerEvents:"none"}}/>
      <input className="adm-search" value={value} onChange={e=>onChange(e.target.value)} placeholder={placeholder||"Buscar..."} style={{paddingLeft:36}}/>
    </div>
  );
}
function TableWrap({ load, empty, children }) {
  if (load) return <div className="adm-table-wrap"><div className="skeleton-rows"><div className="skeleton-head"/>{[1,2,3,4].map(i=><div key={i} className="skeleton-row"/>)}</div></div>;
  if (empty) return <div className="adm-table-wrap"><div className="td-empty"><span className="td-empty-icon"><Search size={22}/></span><strong>{empty}</strong><span className="td-empty-hint">Este listado se actualiza con la información registrada en el sistema.</span></div></div>;
  return <div className="adm-table-wrap">{children}</div>;
}
function Field({ label, children, style }) {
  return <div className="lf-field" style={style}><label>{label}</label>{children}</div>;
}
function Actions({ onEdit, onDel, extra }) {
  return (
    <div className="td-actions">
      {onEdit && <button className="btn-edit" onClick={onEdit} title="Editar" aria-label="Editar registro"><Pencil size={13}/></button>}
      {onDel && <button className="btn-delete" onClick={onDel} title="Eliminar" aria-label="Eliminar registro"><Trash2 size={13}/></button>}
      {extra}
    </div>
  );
}
function ConfirmModal({ onConfirmar, onCancelar }) {
  return (
    <div className="modal-overlay" onClick={onCancelar} role="presentation">
      <div className="modal confirm-modal" role="dialog" aria-modal="true" aria-label="Confirmar eliminación" onClick={e=>e.stopPropagation()}>
        <span className="confirm-icon"><Trash2 size={26}/></span>
        <h3 className="confirm-title">¿Confirmar eliminación?</h3>
        <p className="confirm-text">Esta acción no se puede deshacer.</p>
        <div className="confirm-actions">
          <button className="btn-cancelar" onClick={onCancelar}>Cancelar</button>
          <button className="btn-delete-confirm" onClick={onConfirmar}><Trash2 size={14} style={{marginRight:6}}/>Eliminar</button>
        </div>
      </div>
    </div>
  );
}
function ModalForm({ titulo, icon, onClose, onSubmit, saving, children }) {
  return (
    <div className="modal-overlay" onClick={onClose} role="presentation">
      <div className="modal" role="dialog" aria-modal="true" aria-label={titulo} style={{maxWidth:560,width:"95%"}} onClick={e=>e.stopPropagation()}>
        <div className="modal-header">
          <div className="modal-badge">{icon||<Plus size={20}/>}</div>
          <div><div className="modal-title">{titulo}</div></div>
          <button className="modal-close" onClick={onClose} aria-label="Cerrar"><X size={20}/></button>
        </div>
        <div className="modal-body">
          <form onSubmit={onSubmit}>
            {children}
            <div style={{display:"flex",gap:10,marginTop:18}}>
              <button type="button" className="btn-cancelar" onClick={onClose} style={{flex:1}}>Cancelar</button>
              <button type="submit" className="btn-reservar" style={{flex:2}} disabled={saving}>
                {saving?<span className="spinner"/>:<span><CheckCircle2 size={15} style={{verticalAlign:"middle",marginRight:4}}/>Guardar</span>}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}


// ═══════════════════════════════════════════════════════
// DASHBOARD HOME
// ═══════════════════════════════════════════════════════
function DashboardHome({ headers, onNav, isCajero=false }) {
  const [data, setData] = useState({ rutas:0,viajes:0,buses:0,clientes:0,encomiendas:0,sucursales:0,pagos:0,pagosPend:0,reservasPend:0,viajeStats:{},viajesOrdenados:[],programados:0,enCurso:0,finalizados:0,ocupProm:0,proximasSalidas:[],reservasRecientes:[],encomiendasRecientes:[],pagosRecientes:[] });
  const [load, setLoad] = useState(true);
  const [paginaViajes, setPaginaViajes] = useState(0);
  const VIAJES_POR_PAG = 8;

  const hdrsRef = useRef(headers);
  useEffect(() => { hdrsRef.current = headers; });

  useEffect(() => {
    const toArr = d => {
      if (!d) return [];
      if (Array.isArray(d)) return d;
      if (Array.isArray(d.content)) return d.content;
      if (Array.isArray(d.data)) return d.data;
      return [];
    };
    const get = (url) => fetch(url, { headers: hdrsRef.current })
      .then(r => { if (!r.ok) { console.warn(`GET ${url} →`, r.status); return null; } return r.json(); })
      .catch(e => { console.warn(`GET ${url}:`, e.message); return null; });

    const baseCalls = [
      get("/api/v1/rutas"),
      get("/api/v1/viajes"),
      get("/api/v1/buses"),
      get("/api/v1/clientes?page=0&size=1000"),
      get("/api/v1/encomiendas"),
      get("/api/v1/sucursales"),
    ];
    // Indicadores reales de caja: pagos registrados y reservas web pendientes
    // de cobro. Ambos roles consultan estos endpoints (cajero en su operación
    // diaria; administrador en el resumen general). No se fabrican cifras.
    const extraCalls = [
      get("/api/v1/pagos"),
      get("/api/v1/ventas/reservas?estado=RETENIDO&estado=PENDIENTE_PAGO"),
    ];

    Promise.all([...baseCalls, ...extraCalls]).then(([rutas, viajes, buses, clientesResp, encomiendas, sucursales, pagos, reservas]) => {
      const vArr = toArr(viajes);
      const cArr = toArr(clientesResp);
      const pArr = toArr(pagos);
      const rArr = toArr(reservas);
      const encArr = toArr(encomiendas);
      const vStats = vArr.reduce((acc,v) => { acc[v.estado]=(acc[v.estado]||0)+1; return acc; }, {});
      const vProg = vArr.filter(v => v.estado === "PROGRAMADO");
      const occ = vProg.length > 0
        ? Math.round(vProg.reduce((s,v) => {
            const t = v.totalAsientos||40, lib = v.asientosDisponibles??t;
            return s + ((t-lib)/t*100);
          }, 0) / vProg.length)
        : 0;
      const ahora = Date.now();
      const proximasSalidas = [...vProg]
        .filter(v => v.fechaHoraSalida && new Date(v.fechaHoraSalida).getTime() >= ahora)
        .sort((a,b) => new Date(a.fechaHoraSalida)-new Date(b.fechaHoraSalida))
        .slice(0,5);
      setData({
        rutas: toArr(rutas).length,
        viajes: vArr.length,
        buses: toArr(buses).length,
        clientes: cArr.length,
        encomiendas: encArr.length,
        sucursales: toArr(sucursales).length,
        pagos: pArr.length,
        pagosPend: pArr.filter(p => (p.estado||"").toUpperCase().includes("PENDIENTE")).length,
        reservasPend: rArr.length,
        viajeStats: vStats,
        programados: vStats["PROGRAMADO"]||0,
        enCurso: vStats["EN_CURSO"]||0,
        finalizados: vStats["FINALIZADO"]||0,
        ocupProm: occ,
        viajesOrdenados: [...vArr].sort((a,b) => new Date(b.fechaHoraSalida)-new Date(a.fechaHoraSalida)),
        proximasSalidas,
        reservasRecientes: [...rArr].sort((a,b) => new Date(b.expiraEn||b.createdAt||0)-new Date(a.expiraEn||a.createdAt||0)).slice(0,5),
        encomiendasRecientes: [...encArr].sort((a,b) => new Date(b.createdAt||0)-new Date(a.createdAt||0)).slice(0,5),
        pagosRecientes: [...pArr].sort((a,b) => new Date(b.fechaPago||0)-new Date(a.fechaPago||0)).slice(0,5),
      });
      setLoad(false);
    }).catch(e => { console.error("Dashboard error:", e); setLoad(false); });
  }, [isCajero]); // isCajero es constante por sesión: la carga ocurre al montar

  const donutViajes=Object.entries(data.viajeStats||{}).map(([k,v])=>({nombre:k.replace(/_/g," "),valor:v}));
  const totalPags=Math.ceil((data.viajesOrdenados?.length||0)/VIAJES_POR_PAG);
  const viajesPagina=(data.viajesOrdenados||[]).slice(paginaViajes*VIAJES_POR_PAG,(paginaViajes+1)*VIAJES_POR_PAG);

  if (load) return <div className="adm-loading"><div className="pulse-logo"><Bus size={44}/></div><p>Cargando datos del sistema...</p></div>;

  const fechaHoy = new Date().toLocaleDateString("es-PE",{weekday:"long",day:"2-digit",month:"long",year:"numeric"});
  const rolTitulo = isCajero ? "Caja y ventas" : "Administración central";

  return (
    <div className="adm-dash">
      {/* Banner institucional (ilustración SVG propia; sin fotografía externa) */}
      <section className="adm-banner" aria-label="Panel de administración INTIWATANA">
        <svg className="adm-banner-art" viewBox="0 0 560 190" preserveAspectRatio="xMidYMid slice" aria-hidden="true" focusable="false">
          <defs>
            <linearGradient id="bgrad" x1="0" y1="0" x2="1" y2="1">
              <stop offset="0" stopColor="#012A26"/>
              <stop offset="0.55" stopColor="#0B3D38"/>
              <stop offset="1" stopColor="#127369"/>
            </linearGradient>
          </defs>
          <rect width="560" height="190" fill="url(#bgrad)"/>
          <circle cx="480" cy="44" r="27" fill="#F5C518" opacity="0.92"/>
          <circle cx="480" cy="44" r="20" fill="#FFD935" opacity="0.55"/>
          <path d="M0 158 L72 104 L122 134 L188 86 L252 122 L322 74 L392 108 L470 64 L560 100 L560 190 L0 190 Z" fill="#0E4A42"/>
          <path d="M0 176 L84 132 L154 160 L244 120 L334 154 L424 112 L504 146 L560 122 L560 190 L0 190 Z" fill="#091F1C"/>
          <g transform="translate(352,80)">
            <rect x="0" y="34" width="120" height="48" rx="10" fill="#104A43"/>
            <path d="M6 30 h52 v26 h-52 z" fill="#F5C518"/>
            <rect x="12" y="40" width="40" height="18" rx="5" fill="#0B3D38"/>
            <rect x="8" y="34" width="6" height="10" rx="2" fill="#C99B00"/>
            <rect x="50" y="34" width="6" height="10" rx="2" fill="#C99B00"/>
            <circle cx="22" cy="86" r="6" fill="#F4F7F6"/>
            <circle cx="98" cy="86" r="6" fill="#F4F7F6"/>
            <circle cx="22" cy="86" r="2.6" fill="#0B3D38"/>
            <circle cx="98" cy="86" r="2.6" fill="#0B3D38"/>
          </g>
          <path d="M40 96 q60 -26 130 -10 q54 12 96 -4" stroke="#1A8A7E" strokeWidth="3" fill="none" opacity="0.65" strokeLinecap="round"/>
          <path d="M64 124 l16 -18 12 22 18 -26 16 30" stroke="#F5C518" strokeWidth="2.5" fill="none" opacity="0.35" strokeLinecap="round"/>
        </svg>
        <div className="adm-banner-content">
          <span className="adm-banner-eyebrow">Empresa de Transportes S.R.L. · Gestión integrada</span>
          <h1 className="adm-banner-title">Panel Administrador</h1>
          <p className="adm-banner-sub">{rolTitulo} · Operación, clientes, reservas y encomiendas en un solo lugar.</p>
        </div>
        <div className="adm-banner-side">
          <span className="adm-banner-date">{fechaHoy}</span>
          <span className="adm-banner-chip"><CheckCircle2 size={13}/> Sistema operativo</span>
        </div>
      </section>

      {/* KPIs reales */}
      {isCajero ? (
        <div className="adm-stat-grid">
          <StatCard icon={<CalendarDays size={22}/>} valor={data.programados} label="Viajes programados" color={C.verde} onClick={()=>onNav("viajes")}/>
          <StatCard icon={<Users size={22}/>} valor={data.clientes} label="Clientes registrados" color={C.naranja} onClick={()=>onNav("clientes")}/>
          <StatCard icon={<CreditCard size={22}/>} valor={data.pagos} label="Pagos registrados" color={C.verde} onClick={()=>onNav("pagos")}/>
          <StatCard icon={<Ticket size={22}/>} valor={data.reservasPend} label="Reservas por cobrar" color={C.amarillo} onClick={()=>onNav("reservas")}/>
          <StatCard icon={<Package size={22}/>} valor={data.encomiendas} label="Encomiendas" color={C.rojo} onClick={()=>onNav("encomiendas")}/>
        </div>
      ) : (
        <div className="adm-stat-grid">
          <StatCard icon={<Route size={22}/>} valor={data.rutas} label="Rutas activas" color={C.verde} onClick={()=>onNav("rutas")}/>
          <StatCard icon={<CalendarDays size={22}/>} valor={data.viajes} label="Total viajes" color={C.amarillo} onClick={()=>onNav("viajes")}/>
          <StatCard icon={<Bus size={22}/>} valor={data.buses} label="Flota activa" color={C.verde} onClick={()=>onNav("buses")}/>
          <StatCard icon={<Users size={22}/>} valor={data.clientes} label="Clientes" color={C.naranja} onClick={()=>onNav("clientes")}/>
          <StatCard icon={<Package size={22}/>} valor={data.encomiendas} label="Encomiendas" color={C.rojo} onClick={()=>onNav("encomiendas")}/>
          <StatCard icon={<Building2 size={22}/>} valor={data.sucursales} label="Sucursales" color={C.gris} onClick={()=>onNav("sucursales")}/>
        </div>
      )}

      <div className="adm-dash-row2">
        <div className="card-panel">
          <div className="panel-title" style={{display:"flex",alignItems:"center",gap:7}}><Armchair size={15} color={C.verde}/> Ocupación promedio (viajes programados)</div>
          <div className="adm-ocup-val">{data.ocupProm}%</div>
          <ProgressBar value={data.ocupProm} max={100} color={data.ocupProm>80?C.rojo:data.ocupProm>50?C.naranja:C.verde}/>
          <div className="adm-mini-grid">
            {[{l:"Programados",v:data.programados,c:C.verde},{l:"En curso",v:data.enCurso,c:"#99780c"},{l:"Finalizados",v:data.finalizados,c:C.gris}].map(s=>(
              <div key={s.l} className="adm-mini-dato">
                <div className="adm-mini-val" style={{color:s.c}}>{s.v}</div>
                <div className="adm-mini-label">{s.l}</div>
              </div>
            ))}
          </div>
        </div>
        <div className="card-panel">
          <div className="panel-title" style={{display:"flex",alignItems:"center",gap:7}}><CalendarDays size={15} color={C.verde}/> Estado de viajes</div>
          {donutViajes.length>0?<DonutChart data={donutViajes} size={130}/>:<p className="adm-vacio-mini">Sin viajes registrados</p>}
        </div>
      </div>

      <div className="adm-dash-row2">
        <div className="card-panel">
          <div className="adm-card-head">
            <div className="panel-title" style={{margin:0}}><Navigation size={15} color={C.verde}/> Próximas salidas</div>
            <button className="adm-link" onClick={()=>onNav("viajes")}>Ver viajes <ArrowRight size={12}/></button>
          </div>
          {data.proximasSalidas?.length===0?(
            <p className="adm-vacio-mini">Sin salidas programadas próximas.</p>
          ):(
            <div className="adm-lista">
              {data.proximasSalidas.map(v=>(
                <div key={v.id} className="adm-lista-fila">
                  <div className="adm-lista-icono" style={{background:C.verde+"16",color:C.verde}}><CalendarDays size={16}/></div>
                  <div className="adm-lista-main">
                    <div className="adm-lista-titulo">{v.ruta?.origen?.ciudad||"—"} <ArrowRight size={11} style={{verticalAlign:"-2px",color:C.medio}}/> {v.ruta?.destino?.ciudad||"—"}</div>
                    <div className="adm-lista-sub">{fmtFecha(v.fechaHoraSalida)} · <span className="td-codigo">{v.bus?.placa||"—"}</span></div>
                  </div>
                  <div className="adm-lista-side">
                    <div className="adm-precio">S/ {v.precioAdulto||"—"}</div>
                    <Badge text={v.estado}/>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="card-panel">
          <div className="adm-card-head">
            <div className="panel-title" style={{margin:0}}><Ticket size={15} color="#C99B00"/> Reservas por confirmar</div>
            <button className="adm-link" onClick={()=>onNav("reservas")}>Gestionar <ArrowRight size={12}/></button>
          </div>
          {data.reservasRecientes?.length===0?(
            <p className="adm-vacio-mini">No hay reservas pendientes de pago.</p>
          ):(
            <div className="adm-lista">
              {data.reservasRecientes.map(r=>(
                <div key={r.referencia} className="adm-lista-fila">
                  <div className="adm-lista-icono" style={{background:"rgba(245,197,24,0.18)",color:"#99780c"}}><Ticket size={16}/></div>
                  <div className="adm-lista-main">
                    <div className="adm-lista-titulo" style={{fontFamily:"monospace",fontSize:12.5}}>{r.referencia}</div>
                    <div className="adm-lista-sub">{r.compradorNombre||"—"}</div>
                  </div>
                  <div className="adm-lista-side">
                    <div className="adm-precio">S/ {Number(r.total||0).toFixed(2)}</div>
                    <Badge text={r.estado}/>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      <div className="adm-dash-row2">
        <div className="card-panel">
          <div className="adm-card-head">
            <div className="panel-title" style={{margin:0}}><Package size={15} color={C.verde}/> Últimas encomiendas</div>
            <button className="adm-link" onClick={()=>onNav("encomiendas")}>Ver todas <ArrowRight size={12}/></button>
          </div>
          {data.encomiendasRecientes?.length===0?(
            <p className="adm-vacio-mini">Sin encomiendas registradas.</p>
          ):(
            <div className="adm-lista">
              {data.encomiendasRecientes.map(e=>(
                <div key={e.id} className="adm-lista-fila">
                  <div className="adm-lista-icono" style={{background:C.rojo+"14",color:C.rojo}}><Package size={16}/></div>
                  <div className="adm-lista-main">
                    <div className="adm-lista-titulo" style={{fontFamily:"monospace",fontSize:12.5}}>{e.numeroGuia||e.referenciaSolicitud||`#${e.id}`}</div>
                    <div className="adm-lista-sub">{e.sucursalOrigen?.ciudad||"—"} → {e.sucursalDestino?.ciudad||"—"}</div>
                  </div>
                  <div className="adm-lista-side">
                    {e.costo?<div className="adm-precio">S/ {e.costo}</div>:null}
                    <Badge text={e.estado}/>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {!isCajero && (
          <div className="card-panel">
            <div className="adm-card-head">
              <div className="panel-title" style={{margin:0}}><CreditCard size={15} color={C.naranja}/> Pagos recientes</div>
              <button className="adm-link" onClick={()=>onNav("pagos")}>Ver pagos <ArrowRight size={12}/></button>
            </div>
            {data.pagosRecientes?.length===0?(
              <p className="adm-vacio-mini">Sin pagos registrados.</p>
            ):(
              <div className="adm-lista">
                {data.pagosRecientes.map(p=>(
                  <div key={p.id} className="adm-lista-fila">
                    <div className="adm-lista-icono" style={{background:C.naranja+"16",color:C.naranja}}><CreditCard size={16}/></div>
                    <div className="adm-lista-main">
                      <div className="adm-lista-titulo">{p.cliente||"Cliente Varios"}</div>
                      <div className="adm-lista-sub">{fmtFecha(p.fechaPago)}</div>
                    </div>
                    <div className="adm-lista-side">
                      <div className="adm-precio">S/ {Number(p.monto||0).toFixed(2)}</div>
                      <Badge text={p.estado}/>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </div>

      {/* Historial de viajes paginado */}
      <div className="card-panel">
        <div className="adm-card-head">
          <div className="panel-title" style={{margin:0}}><Clock size={15} color={C.verde}/> Historial de viajes</div>
          <div style={{display:"flex",alignItems:"center",gap:8,flexWrap:"wrap"}}>
            <span className="adm-count">{data.viajesOrdenados?.length||0} viajes · pág. {paginaViajes+1}/{Math.max(totalPags,1)}</span>
            <button className="adm-page-btn" aria-label="Página anterior" disabled={paginaViajes===0} onClick={()=>setPaginaViajes(p=>p-1)}><ChevronLeft size={14}/></button>
            <button className="adm-page-btn" aria-label="Página siguiente" disabled={paginaViajes>=totalPags-1} onClick={()=>setPaginaViajes(p=>p+1)}><ChevronRight size={14}/></button>
          </div>
        </div>
        {viajesPagina.length===0?(<p className="adm-vacio-mini">Sin viajes registrados.</p>):(
          <div style={{overflowX:"auto"}}>
            <table className="adm-table tbl-dash-viajes">
              <thead><tr><th>Ruta</th><th>Bus</th><th>Salida</th><th>Precio</th><th>Estado</th></tr></thead>
              <tbody>
                {viajesPagina.map(v=>(
                  <tr key={v.id} className="tr-hover">
                    <td><strong>{v.ruta?.origen?.ciudad||"—"}</strong><span style={{color:C.medio}}> → </span><strong>{v.ruta?.destino?.ciudad||"—"}</strong></td>
                    <td><span className="td-codigo">{v.bus?.placa||"—"}</span></td>
                    <td style={{fontSize:12}}>{fmtFecha(v.fechaHoraSalida)}</td>
                    <td><strong style={{color:C.verde}}>S/ {v.precioAdulto||"—"}</strong></td>
                    <td><Badge text={v.estado}/></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        <div className="adm-paginacion">
          {Array.from({length:totalPags},(_,i)=>(
            <button key={i} aria-label={`Ir a la página ${i+1}`} aria-current={paginaViajes===i ? "page" : undefined} onClick={()=>setPaginaViajes(i)}
              className={paginaViajes===i?"adm-page-num active":"adm-page-num"}>{i+1}</button>
          ))}
        </div>
      </div>
    </div>
  );
}


// ═══════════════════════════════════════════════════════
// CRUD: Sucursales
// ═══════════════════════════════════════════════════════
function CrudSucursales({ headers, showToast }) {
  const [items,setItems]=useState([]);const [load,setLoad]=useState(true);const [buscar,setBuscar]=useState("");
  const [modal,setModal]=useState(null);const [form,setForm]=useState({});const [conf,setConf]=useState(null);const [saving,setSaving]=useState(false);

  const cargar=useCallback(async()=>{setLoad(true);try{const r=await fetch("/api/v1/sucursales",{headers});const d=r.ok?await r.json():[];setItems(Array.isArray(d)?d:[]);}catch{showToast("Error al cargar sucursales");}finally{setLoad(false);};},[headers,showToast]);
  useEffect(()=>{cargar();},[cargar]);

  const abrir=(s=null)=>{setForm(s?{id:s.id,nombre:s.nombre||"",codigo:s.codigo||"",ciudad:s.ciudad||"",provincia:s.provincia||"",departamento:s.departamento||"",direccion:s.direccion||"",telefono:s.telefono||"",esTerminal:s.esTerminal||false}:{nombre:"",codigo:"",ciudad:"",provincia:"",departamento:"",direccion:"",telefono:"",esTerminal:false});setModal(s||"nuevo");};

  const guardar=async(e)=>{e.preventDefault();if(!form.nombre||!form.ciudad)return showToast("Nombre y ciudad son obligatorios");setSaving(true);try{const isEdit=!!form.id;const res=await fetch(isEdit?`/api/v1/sucursales/${form.id}`:"/api/v1/sucursales",{method:isEdit?"PUT":"POST",headers,body:JSON.stringify({nombre:form.nombre,codigo:form.codigo,ciudad:form.ciudad,provincia:form.provincia,departamento:form.departamento,direccion:form.direccion,telefono:form.telefono,esTerminal:form.esTerminal})});if(!res.ok){const err=await res.json().catch(()=>({}));throw new Error(err.mensaje||"Error");}const data=await res.json();setItems(prev=>isEdit?prev.map(x=>x.id===data.id?data:x):[...prev,data]);showToast(isEdit?"Sucursal actualizada":"Sucursal creada");setModal(null);}catch(err){showToast(err.message);}finally{setSaving(false);};};

  const eliminar=async(id)=>{try{await fetch(`/api/v1/sucursales/${id}`,{method:"DELETE",headers});setItems(prev=>prev.filter(x=>x.id!==id));showToast("Sucursal eliminada");}catch{showToast("Error al eliminar");}finally{setConf(null);};};

  const fil=items.filter(s=>`${s.nombre} ${s.ciudad} ${s.codigo}`.toLowerCase().includes(buscar.toLowerCase()));

  return (<>
    <PageHeader title="Gestión de Sucursales" sub={`${items.length} sucursales`} action={<button className="adm-btn-primary" onClick={()=>abrir()}><Plus size={15} style={{marginRight:6}}/>Nueva sucursal</button>}/>
    <SearchBar value={buscar} onChange={setBuscar} placeholder="Buscar por nombre, ciudad o código..."/>
    <TableWrap load={load} empty={!fil.length&&"Sin sucursales registradas"}>
      <table className="adm-table tbl-sucursales">
        <thead><tr><th>Nombre</th><th>Código</th><th>Ciudad</th><th>Departamento</th><th>Teléfono</th><th>Terminal</th><th>Acciones</th></tr></thead>
        <tbody>
          {fil.map(s=>(
            <tr key={s.id} className="tr-hover">
              <td><strong>{s.nombre}</strong></td>
              <td><span className="td-codigo">{s.codigo||"—"}</span></td>
              <td>{s.ciudad||"—"}</td>
              <td style={{fontSize:12,color:C.medio}}>{s.departamento||"—"}</td>
              <td>{s.telefono||"—"}</td>
              <td><Badge text={s.esTerminal?"SÍ":"NO"}/></td>
              <td><Actions onEdit={()=>abrir(s)} onDel={()=>setConf(s.id)}/></td>
            </tr>
          ))}
        </tbody>
      </table>
    </TableWrap>
    {modal&&(<ModalForm titulo={modal==="nuevo"?"Nueva sucursal":"Editar sucursal"} icon={<Building2 size={20}/>} onClose={()=>setModal(null)} onSubmit={guardar} saving={saving}>
      <div className="form-grid">
        <Field label="Nombre *"><input value={form.nombre||""} onChange={e=>setForm(f=>({...f,nombre:e.target.value}))} required/></Field>
        <Field label="Código"><input value={form.codigo||""} onChange={e=>setForm(f=>({...f,codigo:e.target.value}))} placeholder="AYA-01"/></Field>
        <Field label="Ciudad *"><input value={form.ciudad||""} onChange={e=>setForm(f=>({...f,ciudad:e.target.value}))} required/></Field>
        <Field label="Provincia"><input value={form.provincia||""} onChange={e=>setForm(f=>({...f,provincia:e.target.value}))}/></Field>
        <Field label="Departamento"><input value={form.departamento||""} onChange={e=>setForm(f=>({...f,departamento:e.target.value}))}/></Field>
        <Field label="Teléfono"><input value={form.telefono||""} onChange={e=>setForm(f=>({...f,telefono:e.target.value}))}/></Field>
        <Field label="Dirección" style={{gridColumn:"1/-1"}}><input value={form.direccion||""} onChange={e=>setForm(f=>({...f,direccion:e.target.value}))}/></Field>
        <Field label="¿Es terminal principal?" style={{gridColumn:"1/-1"}}>
          <label style={{display:"flex",alignItems:"center",gap:8,cursor:"pointer"}}>
            <input type="checkbox" checked={!!form.esTerminal} onChange={e=>setForm(f=>({...f,esTerminal:e.target.checked}))} style={{width:16,height:16,accentColor:C.verde}}/>
            <span style={{fontSize:13}}>Marcar como terminal principal</span>
          </label>
        </Field>
      </div>
    </ModalForm>)}
    {conf&&<ConfirmModal onConfirmar={()=>eliminar(conf)} onCancelar={()=>setConf(null)}/>}
  </>);
}

// ═══════════════════════════════════════════════════════
// CRUD: Rutas
// ═══════════════════════════════════════════════════════
function CrudRutas({ headers, showToast }) {
  const [items,setItems]=useState([]);const [sucursales,setSucursales]=useState([]);const [load,setLoad]=useState(true);const [buscar,setBuscar]=useState("");
  const [modal,setModal]=useState(null);const [form,setForm]=useState({});const [conf,setConf]=useState(null);const [saving,setSaving]=useState(false);

  const cargar=useCallback(async()=>{setLoad(true);try{const[r1,r2]=await Promise.all([fetch("/api/v1/rutas",{headers}),fetch("/api/v1/sucursales",{headers})]);setItems(r1.ok?await r1.json():[]);setSucursales(r2.ok?await r2.json():[]);}catch{showToast("Error al cargar rutas");}finally{setLoad(false);};},[headers,showToast]);
  useEffect(()=>{cargar();},[cargar]);

  const abrir=(r=null)=>{setForm(r?{id:r.id,codigo:r.codigo||"",origenId:r.origen?.id?.toString()||"",destinoId:r.destino?.id?.toString()||"",distanciaKm:r.distanciaKm?.toString()||"",duracionHorasEstimada:r.duracionHorasEstimada?.toString()||"",precioBase:r.precioBase?.toString()||""}:{codigo:"",origenId:"",destinoId:"",distanciaKm:"",duracionHorasEstimada:"",precioBase:""});setModal(r||"nuevo");};

  const guardar=async(e)=>{e.preventDefault();if(!form.origenId||!form.destinoId)return showToast("Origen y destino obligatorios");setSaving(true);try{const isEdit=!!form.id;const res=await fetch(isEdit?`/api/v1/rutas/${form.id}`:"/api/v1/rutas",{method:isEdit?"PUT":"POST",headers,body:JSON.stringify({codigo:form.codigo,origenId:parseInt(form.origenId),destinoId:parseInt(form.destinoId),distanciaKm:form.distanciaKm?parseFloat(form.distanciaKm):null,duracionHorasEstimada:form.duracionHorasEstimada?parseFloat(form.duracionHorasEstimada):null,precioBase:form.precioBase?parseFloat(form.precioBase):null})});if(!res.ok){const err=await res.json().catch(()=>({}));throw new Error(err.mensaje||"Error");}const data=await res.json();setItems(prev=>isEdit?prev.map(x=>x.id===data.id?data:x):[...prev,data]);showToast(isEdit?"Ruta actualizada":"Ruta creada");setModal(null);}catch(err){showToast(err.message);}finally{setSaving(false);};};

  const eliminar=async(id)=>{try{await fetch(`/api/v1/rutas/${id}`,{method:"DELETE",headers});setItems(prev=>prev.filter(x=>x.id!==id));showToast("Ruta eliminada");}catch{showToast("Error");}finally{setConf(null);};};

  const fil=items.filter(r=>`${r.codigo||""} ${r.origen?.ciudad||""} ${r.destino?.ciudad||""}`.toLowerCase().includes(buscar.toLowerCase()));

  return (<>
    <PageHeader title="Gestión de Rutas" sub={`${items.length} rutas`} action={<button className="adm-btn-primary" onClick={()=>abrir()}><Plus size={15} style={{marginRight:6}}/>Nueva ruta</button>}/>
    <SearchBar value={buscar} onChange={setBuscar} placeholder="Buscar por código u origen/destino..."/>
    <TableWrap load={load} empty={!fil.length&&"Sin rutas registradas"}>
      <table className="adm-table tbl-rutas">
        <thead><tr><th>Código</th><th>Ruta</th><th>Distancia</th><th>Duración est.</th><th>Precio base</th><th>Acciones</th></tr></thead>
        <tbody>
          {fil.map(r=>(
            <tr key={r.id} className="tr-hover">
              <td><span className="td-codigo">{r.codigo||"—"}</span></td>
              <td><div className="td-lugar"><span className="td-ciudad" style={{display:"flex",alignItems:"center",gap:5}}><MapPin size={12} color={C.verde}/>{r.origen?.ciudad||"—"} <ArrowRight size={12} color={C.medio}/> {r.destino?.ciudad||"—"}</span></div></td>
              <td>{r.distanciaKm?`${r.distanciaKm} km`:"—"}</td>
              <td>{r.duracionHorasEstimada?`${r.duracionHorasEstimada} h`:"—"}</td>
              <td><strong style={{color:C.verde}}>{r.precioBase?`S/ ${r.precioBase}`:"—"}</strong></td>
              <td><Actions onEdit={()=>abrir(r)} onDel={()=>setConf(r.id)}/></td>
            </tr>
          ))}
        </tbody>
      </table>
    </TableWrap>
    {modal&&(<ModalForm titulo={modal==="nuevo"?"Nueva ruta":"Editar ruta"} icon={<Route size={20}/>} onClose={()=>setModal(null)} onSubmit={guardar} saving={saving}>
      <div className="form-grid">
        <Field label="Código"><input value={form.codigo||""} onChange={e=>setForm(f=>({...f,codigo:e.target.value}))} placeholder="HUA-VIL-01"/></Field>
        <Field label="Precio base (S/)"><input type="number" step="0.01" value={form.precioBase||""} onChange={e=>setForm(f=>({...f,precioBase:e.target.value}))} placeholder="0.00"/></Field>
        <Field label="Origen *">
          <select className="lf-select" value={form.origenId||""} onChange={e=>setForm(f=>({...f,origenId:e.target.value}))} required>
            <option value="">Seleccionar...</option>
            {sucursales.map(s=><option key={s.id} value={s.id}>{s.ciudad} — {s.nombre}</option>)}
          </select>
        </Field>
        <Field label="Destino *">
          <select className="lf-select" value={form.destinoId||""} onChange={e=>setForm(f=>({...f,destinoId:e.target.value}))} required>
            <option value="">Seleccionar...</option>
            {sucursales.filter(s=>s.id!==Number(form.origenId)).map(s=><option key={s.id} value={s.id}>{s.ciudad} — {s.nombre}</option>)}
          </select>
        </Field>
        <Field label="Distancia (km)"><input type="number" step="0.1" value={form.distanciaKm||""} onChange={e=>setForm(f=>({...f,distanciaKm:e.target.value}))}/></Field>
        <Field label="Duración estimada (h)"><input type="number" step="0.5" value={form.duracionHorasEstimada||""} onChange={e=>setForm(f=>({...f,duracionHorasEstimada:e.target.value}))}/></Field>
      </div>
    </ModalForm>)}
    {conf&&<ConfirmModal onConfirmar={()=>eliminar(conf)} onCancelar={()=>setConf(null)}/>}
  </>);
}

// ═══════════════════════════════════════════════════════
// CRUD: Buses
// ═══════════════════════════════════════════════════════
function repartirFilas(total, columnas=4) {
  const filas=[]; let restante=Math.max(0,Number(total)||0);
  while(restante>0){ const f=Math.min(columnas,restante); filas.push(f); restante-=f; }
  if(!filas.length) filas.push(0);
  return filas;
}
// Devuelve la configuracion inicial (editable) del bus: la guardada si existe,
// o una distribucion coherente con su capacidad declarada.
function configInicialEditable(bus) {
  const cfg=bus?.configuracionAsientos;
  if(cfg?.pisos?.length){
    return cfg.pisos.map((p,idx)=>({
      piso:p.piso??idx+1,
      filas:(Array.isArray(p.asientosPorFila)?p.asientosPorFila:[]).map(String),
    }));
  }
  const cap=Number(bus?.capacidadAsientos)||0;
  const nPisos=Math.min(2,Math.max(1,Number(bus?.numPisos)||1));
  const out=[]; let restante=cap;
  for(let p=1;p<=nPisos;p++){
    const asignados=(p===nPisos)?restante:Math.floor(cap/nPisos);
    restante-=asignados;
    out.push({ piso:p, filas:repartirFilas(asignados).map(String) });
  }
  return out;
}

function ConfigurarAsientosModal({ bus, headers, onClose, showToast, onSaved }) {
  const [pisos,setPisos]=useState(()=>configInicialEditable(bus));
  const [pasillo,setPasillo]=useState(()=>{
    const p=bus?.configuracionAsientos?.pasillo;
    return Number.isFinite(Number(p))?Number(p):2;
  });
  const [plantilla,setPlantilla]=useState(()=>bus?.configuracionAsientos?.plantilla||"PERSONALIZADO");
  const [plantillas,setPlantillas]=useState([]);
  const [saving,setSaving]=useState(false);

  useEffect(()=>{
    let activo=true;
    fetch("/api/v1/buses/plantillas",{headers})
      .then(r=>r.ok?r.json():[])
      .then(d=>{ if(activo) setPlantillas(Array.isArray(d)?d:[]); })
      .catch(()=>{});
    return ()=>{ activo=false; };
  },[headers]);

  const capacidad=Number(bus?.capacidadAsientos)||0;
  const parse=(s)=>{ const n=Number.parseInt(String(s).trim(),10); return Number.isInteger(n)?n:NaN; };
  const total=pisos.reduce((s,p)=>s+p.filas.reduce((a,v)=>{const n=parse(v);return a+(Number.isFinite(n)&&n>0?n:0);},0),0);
  const columnas=Math.max(1,...pisos.flatMap(p=>p.filas.map(v=>{const n=parse(v);return Number.isFinite(n)?n:0;})));
  const filasValidas=pisos.length>=1 && pisos.every(p=>p.filas.length>0 && p.filas.every(v=>{const n=parse(v);return Number.isFinite(n)&&n>=1&&n<=6;}));
  const completo=filasValidas && total===capacidad;

  const aplicarPlantilla=(codigo)=>{
    setPlantilla(codigo);
    if(codigo==="PERSONALIZADO"){ setPisos(configInicialEditable(bus)); setPasillo(Number(bus?.configuracionAsientos?.pasillo)||2); return; }
    const t=plantillas.find(x=>x.codigo===codigo);
    if(!t) return;
    setPasillo(Number(t.pasillo)||0);
    setPisos([{ piso:1, filas:(t.asientosPorFila||[]).map(String) }]);
  };
  const regenerar=()=>{ setPlantilla("PERSONALIZADO"); setPisos(configInicialEditable(bus)); };
  const setFilaValor=(pi,fi,valor)=>setPisos(prev=>prev.map((p,i)=>i===pi?{...p,filas:p.filas.map((v,j)=>j===fi?valor:v)}:p));
  const addFila=(pi)=>setPisos(prev=>prev.map((p,i)=>i===pi?{...p,filas:[...p.filas,String(columnas||4)]}:p));
  const delFila=(pi,fi)=>setPisos(prev=>prev.map((p,i)=>i===pi?{...p,filas:p.filas.filter((_,j)=>j!==fi)}:p));
  const cambiarPisos=(n)=>{
    const objetivo=Math.max(1,Math.min(2,n));
    if(objetivo===pisos.length) return;
    setPisos(objetivo>pisos.length?[...pisos,{piso:pisos.length+1,filas:[]}]:pisos.slice(0,objetivo));
  };

  const guardar=async(e)=>{
    e.preventDefault();
    if(!filasValidas){ showToast("Cada fila debe tener entre 1 y 6 asientos"); return; }
    if(total!==capacidad){ showToast(`La configuración tiene ${total} asiento(s) y la capacidad es ${capacidad}`); return; }
    setSaving(true);
    try{
      const body={ plantilla, pasillo:Number(pasillo)||0, pisos:pisos.map((p,i)=>({ piso:p.piso??i+1, asientosPorFila:p.filas.map(v=>parse(v)) })) };
      const res=await fetch(`/api/v1/buses/${bus.id}/configuracion-asientos`,{method:"PUT",headers,body:JSON.stringify(body)});
      if(!res.ok){ const err=await res.json().catch(()=>({})); throw new Error(err.mensaje||"No se pudo guardar la configuración"); }
      const data=await res.json();
      showToast("Configuración de asientos guardada");
      onSaved&&onSaved(data);
      onClose();
    }catch(err){ showToast(err.message); } finally { setSaving(false); }
  };

  return (
    <div className="modal-overlay" onClick={onClose} role="presentation">
      <div className="modal" role="dialog" aria-modal="true" aria-label="Configurar asientos" style={{maxWidth:780,width:"96%"}} onClick={e=>e.stopPropagation()}>
        <div className="modal-header">
          <div className="modal-badge"><Armchair size={20}/></div>
          <div>
            <div className="modal-title">Configurar asientos</div>
            <div className="modal-subtitle">{bus.placa} · capacidad declarada {capacidad} asiento(s)</div>
          </div>
          <button className="modal-close" onClick={onClose} aria-label="Cerrar"><X size={20}/></button>
        </div>
        <div className="modal-body">
          <form onSubmit={guardar}>
            <div className="form-grid">
              <Field label="Plantilla base">
                <select className="lf-select" value={plantilla} onChange={e=>aplicarPlantilla(e.target.value)}>
                  <option value="PERSONALIZADO">Personalizado</option>
                  {plantillas.map(t=><option key={t.codigo} value={t.codigo}>{t.nombre} ({t.capacidad})</option>)}
                </select>
              </Field>
              <Field label="N° de pisos">
                <select className="lf-select" value={pisos.length} onChange={e=>cambiarPisos(Number(e.target.value))}>
                  <option value={1}>1 piso</option>
                  <option value={2}>2 pisos</option>
                </select>
              </Field>
              <Field label="Pasillo (columnas antes del pasillo)">
                <input type="number" min="0" max="6" value={pasillo} onChange={e=>setPasillo(e.target.value)}/>
              </Field>
              <Field label="Ajuste rápido">
                <button type="button" className="btn-cancelar" style={{minHeight:38}} onClick={regenerar}>Generar por capacidad</button>
              </Field>
            </div>

            <div style={{display:"flex",flexWrap:"wrap",gap:16,marginTop:16}}>
              <div style={{flex:"1 1 300px",minWidth:260}}>
                {pisos.map((p,pi)=>(
                  <div key={pi} style={{background:"#F8FBFA",border:`1px solid ${C.verde}22`,borderRadius:12,padding:"10px 12px",marginBottom:10}}>
                    <div style={{display:"flex",alignItems:"center",justifyContent:"space-between",marginBottom:8}}>
                      <strong style={{fontSize:12,color:C.oscuro}}>Piso {p.piso??pi+1}</strong>
                      <span style={{fontSize:11,color:C.medio}}>{p.filas.reduce((a,v)=>{const n=parse(v);return a+(Number.isFinite(n)&&n>0?n:0);},0)} asientos</span>
                    </div>
                    {p.filas.map((v,fi)=>(
                      <div key={fi} style={{display:"flex",alignItems:"center",gap:8,marginBottom:5}}>
                        <span style={{width:24,fontSize:11,color:C.medio,textAlign:"right"}}>F{fi+1}</span>
                        <input type="number" min="1" max="6" value={v} style={{width:72}} onChange={e=>setFilaValor(pi,fi,e.target.value)}/>
                        <button type="button" className="btn-delete" title="Quitar fila" aria-label="Quitar fila" onClick={()=>delFila(pi,fi)}><Trash2 size={13}/></button>
                      </div>
                    ))}
                    <button type="button" className="btn-cancelar" style={{minHeight:34,marginTop:4}} onClick={()=>addFila(pi)}><Plus size={13} style={{marginRight:4}}/>Agregar fila</button>
                  </div>
                ))}
              </div>

              <div style={{flex:"1 1 260px",minWidth:240}}>
                <div style={{fontSize:11,fontWeight:800,color:C.medio,marginBottom:6,textTransform:"uppercase",letterSpacing:"0.05em"}}>Vista previa</div>
                <div style={{background:"#F8FBFA",border:`1px solid ${C.verde}22`,borderRadius:12,padding:"12px 10px",maxHeight:300,overflowY:"auto"}}>
                  {pisos.map((p,pi)=>(
                    <div key={pi} style={{marginBottom:12}}>
                      {pisos.length>1&&<div style={{fontSize:10,fontWeight:800,color:C.medio,textAlign:"center",marginBottom:6}}>Piso {p.piso??pi+1}</div>}
                      {p.filas.map((v,fi)=>{
                        const n=parse(v);
                        const cant=Number.isFinite(n)&&n>0?Math.min(n,6):0;
                        return (
                          <div key={fi} style={{display:"flex",alignItems:"center",justifyContent:"center",gap:3,marginBottom:4}}>
                            <span style={{width:14,fontSize:9,color:C.medio,textAlign:"right"}}>{fi+1}</span>
                            {Array.from({length:cant}).flatMap((_,ci)=>{
                              const col=ci+1;
                              const cells=[<span key={`s${ci}`} style={{width:20,height:24,borderRadius:5,background:"#fff",border:`1.5px solid ${C.verde}`,display:"inline-block"}}/>];
                              if(pasillo>0&&col===Number(pasillo)) cells.push(<span key={`g${ci}`} style={{width:10,display:"inline-block"}}/>);
                              return cells;
                            })}
                          </div>
                        );
                      })}
                    </div>
                  ))}
                </div>
                <div style={{marginTop:10,display:"flex",alignItems:"center",gap:8,flexWrap:"wrap"}}>
                  <Badge text={completo?"COMPLETO":"INCOMPLETO"}/>
                  <span style={{fontSize:12,color:C.gris}}>
                    Configurados: <strong style={{color:completo?C.verde:C.rojo}}>{total}</strong> / {capacidad}
                  </span>
                </div>
                {!completo&&(
                  <div style={{marginTop:8,fontSize:11,color:C.naranja,display:"flex",alignItems:"flex-start",gap:6}}>
                    <AlertTriangle size={13} style={{flexShrink:0,marginTop:1}}/>
                    <span>La cantidad de asientos debe coincidir con la capacidad declarada para guardar.</span>
                  </div>
                )}
              </div>
            </div>

            <div style={{display:"flex",gap:10,marginTop:18}}>
              <button type="button" className="btn-cancelar" onClick={onClose} style={{flex:1}}>Cancelar</button>
              <button type="submit" className="btn-reservar" style={{flex:2}} disabled={saving||!completo}>
                {saving?<span className="spinner"/>:<span><CheckCircle2 size={15} style={{verticalAlign:"middle",marginRight:4}}/>Guardar configuración</span>}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}

function CrudBuses({ headers, showToast }) {
  const [items,setItems]=useState([]);const [load,setLoad]=useState(true);const [buscar,setBuscar]=useState("");
  const [modal,setModal]=useState(null);const [form,setForm]=useState({});const [conf,setConf]=useState(null);const [saving,setSaving]=useState(false);
  const [configBus,setConfigBus]=useState(null);

  const cargar=useCallback(async()=>{setLoad(true);try{const r=await fetch("/api/v1/buses",{headers});const d=r.ok?await r.json():[];setItems(Array.isArray(d)?d:[]);}catch{showToast("Error al cargar buses");}finally{setLoad(false);};},[headers,showToast]);
  useEffect(()=>{cargar();},[cargar]);

  const abrir=(b=null)=>{setForm(b?{id:b.id,placa:b.placa||"",marca:b.marca||"",modelo:b.modelo||"",tipo:b.tipo||"ECONOMICO",capacidadAsientos:b.capacidadAsientos?.toString()||"",numPisos:b.numPisos?.toString()||"1",anioFabricacion:b.anioFabricacion?.toString()||"",observaciones:b.observaciones||""}:{placa:"",marca:"",modelo:"",tipo:"ECONOMICO",capacidadAsientos:"",numPisos:"1",anioFabricacion:"",observaciones:""});setModal(b||"nuevo");};

  const guardar=async(e)=>{e.preventDefault();if(!form.placa||!form.marca||!form.capacidadAsientos)return showToast("Placa, marca y capacidad son obligatorios");setSaving(true);try{const isEdit=!!form.id;const res=await fetch(isEdit?`/api/v1/buses/${form.id}`:"/api/v1/buses",{method:isEdit?"PUT":"POST",headers,body:JSON.stringify({placa:form.placa.toUpperCase(),marca:form.marca,modelo:form.modelo,tipo:form.tipo,capacidadAsientos:parseInt(form.capacidadAsientos),numPisos:parseInt(form.numPisos)||1,anioFabricacion:form.anioFabricacion?parseInt(form.anioFabricacion):null,observaciones:form.observaciones||null})});if(!res.ok){const err=await res.json().catch(()=>({}));throw new Error(err.mensaje||"Error");}const data=await res.json();setItems(prev=>isEdit?prev.map(x=>x.id===data.id?data:x):[...prev,data]);showToast(isEdit?"Bus actualizado":"Bus creado");setModal(null);}catch(err){showToast(err.message);}finally{setSaving(false);};};

  const eliminar=async(id)=>{try{await fetch(`/api/v1/buses/${id}`,{method:"DELETE",headers});setItems(prev=>prev.filter(x=>x.id!==id));showToast("Bus eliminado");}catch{showToast("Error");}finally{setConf(null);};};

  const onConfigSaved=(data)=>setItems(prev=>prev.map(x=>x.id===data.id?data:x));

  const fil=items.filter(b=>`${b.placa} ${b.marca} ${b.modelo||""}`.toLowerCase().includes(buscar.toLowerCase()));

  return (<>
    <PageHeader title="Gestión de Autobuses" sub={`${items.length} buses`} action={<button className="adm-btn-primary" onClick={()=>abrir()}><Plus size={15} style={{marginRight:6}}/>Nuevo bus</button>}/>
    <SearchBar value={buscar} onChange={setBuscar} placeholder="Buscar por placa, marca o modelo..."/>
    <TableWrap load={load} empty={!fil.length&&"Sin buses registrados"}>
      <table className="adm-table tbl-buses">
        <thead><tr><th>Placa</th><th>Marca / Modelo</th><th>Tipo</th><th>Capacidad</th><th>Asientos config.</th><th>Estado</th><th>Pisos</th><th>Año</th><th>Acciones</th></tr></thead>
        <tbody>
          {fil.map(b=>(
            <tr key={b.id} className="tr-hover">
              <td><span className="td-codigo">{b.placa}</span></td>
              <td><strong>{b.marca}</strong>{b.modelo&&<span style={{color:C.medio,fontSize:12}}> {b.modelo}</span>}</td>
              <td><Badge text={b.tipo}/></td>
              <td style={{textAlign:"center"}}><strong>{b.capacidadAsientos}</strong></td>
              <td style={{textAlign:"center"}}>{b.asientosConfigurados??"—"}</td>
              <td><Badge text={b.estadoConfiguracion||"INCOMPLETO"}/></td>
              <td style={{textAlign:"center"}}>{b.numPisos||1}</td>
              <td>{b.anioFabricacion||"—"}</td>
              <td><Actions onEdit={()=>abrir(b)} onDel={()=>setConf(b.id)} extra={<button className="btn-edit" onClick={()=>setConfigBus(b)} title="Configurar asientos" aria-label="Configurar asientos"><Armchair size={13}/></button>}/></td>
            </tr>
          ))}
        </tbody>
      </table>
    </TableWrap>
    {configBus&&<ConfigurarAsientosModal bus={configBus} headers={headers} showToast={showToast} onClose={()=>setConfigBus(null)} onSaved={onConfigSaved}/>}
    {modal&&(<ModalForm titulo={modal==="nuevo"?"Nuevo bus":"Editar bus"} icon={<Bus size={20}/>} onClose={()=>setModal(null)} onSubmit={guardar} saving={saving}>
      <div className="form-grid">
        <Field label="Placa *"><input value={form.placa||""} onChange={e=>setForm(f=>({...f,placa:e.target.value.toUpperCase()}))} required placeholder="ABC-123"/></Field>
        <Field label="Marca *"><input value={form.marca||""} onChange={e=>setForm(f=>({...f,marca:e.target.value}))} required/></Field>
        <Field label="Modelo"><input value={form.modelo||""} onChange={e=>setForm(f=>({...f,modelo:e.target.value}))}/></Field>
        <Field label="Tipo">
          <select className="lf-select" value={form.tipo||"ECONOMICO"} onChange={e=>setForm(f=>({...f,tipo:e.target.value}))}>
            <option value="ECONOMICO">Económico</option>
            <option value="SEMI_CAMA">Semi-cama</option>
            <option value="CAMA">Cama</option>
            <option value="CAMA_SUITE">Cama Suite</option>
          </select>
        </Field>
        <Field label="Capacidad (asientos) *"><input type="number" min="1" value={form.capacidadAsientos||""} onChange={e=>setForm(f=>({...f,capacidadAsientos:e.target.value}))} required/></Field>
        <Field label="N° de pisos"><input type="number" min="1" max="2" value={form.numPisos||1} onChange={e=>setForm(f=>({...f,numPisos:e.target.value}))}/></Field>
        <Field label="Año de fabricación"><input type="number" min="1990" max={new Date().getFullYear()} value={form.anioFabricacion||""} onChange={e=>setForm(f=>({...f,anioFabricacion:e.target.value}))}/></Field>
        <Field label="Observaciones" style={{gridColumn:"1/-1"}}><input value={form.observaciones||""} onChange={e=>setForm(f=>({...f,observaciones:e.target.value}))}/></Field>
      </div>
    </ModalForm>)}
    {conf&&<ConfirmModal onConfirmar={()=>eliminar(conf)} onCancelar={()=>setConf(null)}/>}
  </>);
}


// ═══════════════════════════════════════════════════════
// Mapa gráfico de asientos (para CrudViajes)
// ═══════════════════════════════════════════════════════
function MapaAsientosGrafico({ asientos, loading }) {
  if (loading) return <div style={{padding:"1rem",color:C.medio,fontSize:13,textAlign:"center"}}>Cargando mapa...</div>;
  if (!asientos.length) return <div style={{padding:"1rem",color:C.medio,fontSize:13,textAlign:"center"}}>Sin asientos registrados para este viaje.</div>;

  const disponibles=asientos.filter(a=>a.estado==="DISPONIBLE").length;
  const vendidos=asientos.filter(a=>a.estado==="VENDIDO").length;
  const reservados=asientos.filter(a=>a.estado==="RESERVADO").length;
  const maxFila=Math.max(...asientos.map(a=>a.fila||1),1);

  const colorEstado=(estado)=>{
    if(estado==="DISPONIBLE")return{bg:C.verde+"22",border:C.verde,c:C.verde};
    if(estado==="VENDIDO")return{bg:C.rojo+"22",border:C.rojo,c:C.rojo};
    if(estado==="RESERVADO")return{bg:C.naranja+"22",border:C.naranja,c:C.naranja};
    return{bg:"#f0f4f3",border:"#d4dbd9",c:C.gris};
  };

  return (
    <div>
      {/* Leyenda */}
      <div style={{display:"flex",gap:16,marginBottom:14,flexWrap:"wrap",padding:"10px 14px",background:"#f8faf9",borderRadius:10}}>
        {[
          {color:C.verde,label:"Disponible",val:disponibles},
          {color:C.rojo,label:"Vendido",val:vendidos},
          {color:C.naranja,label:"Reservado",val:reservados},
        ].map(s=>(
          <div key={s.label} style={{display:"flex",alignItems:"center",gap:6}}>
            <div style={{width:14,height:14,borderRadius:4,background:s.color+"33",border:`2px solid ${s.color}`}}/>
            <span style={{fontSize:12,color:C.gris}}><strong style={{color:s.color}}>{s.val}</strong> {s.label}</span>
          </div>
        ))}
        <div style={{marginLeft:"auto",fontSize:12,color:C.gris}}>Total: <strong>{asientos.length}</strong></div>
      </div>

      {/* Bus visual */}
      <div style={{background:"white",borderRadius:12,padding:"14px 10px",border:`1.5px solid ${C.verde}22`,overflowX:"auto"}}>
        <div style={{display:"flex",justifyContent:"center",marginBottom:10}}>
          <div style={{display:"flex",alignItems:"center",gap:6,padding:"4px 16px",background:C.oscuro,borderRadius:20,fontSize:11,color:"white",fontWeight:700}}>
            <Navigation size={12}/> FRENTE DEL BUS
          </div>
        </div>
        <div style={{display:"flex",flexDirection:"column",gap:5,alignItems:"center"}}>
          {Array.from({length:maxFila},(_,fi)=>{
            const fila=fi+1;
            const asientosFila=asientos.filter(a=>a.fila===fila);
            return (
              <div key={fila} style={{display:"flex",gap:4,alignItems:"center"}}>
                <span style={{fontSize:10,color:C.medio,width:18,textAlign:"right",flexShrink:0}}>{fila}</span>
                {[1,2,null,3,4].map((col,idx)=>{
                  if(col===null)return<div key="pas" style={{width:16}}/>;
                  const a=asientosFila.find(x=>x.columna===col);
                  if(!a)return<div key={idx} style={{width:36,height:36}}/>;
                  const s=colorEstado(a.estado);
                  const tooltip = `Asiento ${a.numeroAsiento}\nEstado: ${a.estado}${a.pasajero ? `\nPasajero: ${a.pasajero}` : ""}${a.tipo ? `\nTipo: ${a.tipo}` : ""}`;
                  return(
                    <div key={a.id} title={tooltip}
                      style={{
                        width:36,height:36,borderRadius:8,background:s.bg,border:`2px solid ${s.border}`,
                        display:"flex",flexDirection:"column",alignItems:"center",justifyContent:"center",
                        fontSize:10,fontWeight:700,color:s.c,cursor:"default",transition:"all 0.15s",
                        position:"relative"
                      }}>
                      {a.numeroAsiento}
                      {a.pasajero && <UserCircle size={10} style={{marginTop:-2}}/>}
                    </div>
                  );
                })}
              </div>
            );
          })}
        </div>
        <div style={{display:"flex",justifyContent:"center",marginTop:10}}>
          <div style={{display:"flex",alignItems:"center",gap:6,padding:"4px 16px",background:"#f0f4f3",borderRadius:20,fontSize:11,color:C.gris}}>
            PARTE TRASERA
          </div>
        </div>
      </div>

      {/* Lista de pasajeros (Resumen) */}
      {asientos.some(a => a.pasajero) && (
        <div style={{marginTop:20,borderTop:`1px solid ${C.verde}15`,paddingTop:15}}>
          <div style={{fontSize:12,fontWeight:700,color:C.oscuro,marginBottom:10,display:"flex",alignItems:"center",gap:6}}>
            <Users size={14} color={C.verde}/> Listado de Pasajeros
          </div>
          <div style={{display:"grid",gridTemplateColumns:"repeat(auto-fill, minmax(180px, 1fr))",gap:8}}>
            {asientos.filter(a => a.pasajero).sort((a,b) => a.numeroAsiento.localeCompare(b.numeroAsiento)).map(a => (
              <div key={a.id} style={{display:"flex",alignItems:"center",gap:8,padding:"6px 10px",background:C.blanco,border:`1px solid ${C.verde}22`,borderRadius:8}}>
                <div style={{width:24,height:24,borderRadius:6,background:C.verde,color:"white",fontSize:10,fontWeight:800,display:"flex",alignItems:"center",justifyContent:"center",flexShrink:0}}>
                  {a.numeroAsiento}
                </div>
                <div style={{overflow:"hidden"}}>
                  <div style={{fontSize:11,fontWeight:700,color:C.oscuro,whiteSpace:"nowrap",overflow:"hidden",textOverflow:"ellipsis"}}>{a.pasajero}</div>
                  <div style={{fontSize:9,color:C.medio,textTransform:"uppercase"}}>{a.tipo || "NORMAL"}</div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

// ═══════════════════════════════════════════════════════
// CRUD: Viajes — con asignación de chofer y mapa asientos
// ═══════════════════════════════════════════════════════
function CrudViajes({ headers, showToast, readOnly=false }) {
  const [items,setItems]=useState([]);const [rutas,setRutas]=useState([]);const [buses,setBuses]=useState([]);const [choferes,setChoferes]=useState([]);
  const [load,setLoad]=useState(true);const [modal,setModal]=useState(null);const [form,setForm]=useState({});
  const [conf,setConf]=useState(null);const [saving,setSaving]=useState(false);
  const [filtroEstado,setFiltroEstado]=useState("");
  const [filtroOrigen,setFiltroOrigen]=useState("");
  const [filtroDestino,setFiltroDestino]=useState("");
  const [sucursales,setSucursales]=useState([]);
  const [modalAsientos,setModalAsientos]=useState(null);
  const [asientosModal,setAsientosModal]=useState([]);const [loadAsientos,setLoadAsientos]=useState(false);
  const [cancelarViaje, setCancelarViaje] = useState(null);

  const cargar=useCallback(async()=>{
    setLoad(true);
    try{
      // Modo lectura (CAJERO): sólo consulta viajes y sucursales (públicas).
      // No se invocan /usuarios, /buses ni /rutas: son para administración y
      // evitan peticiones 403 sin función real en caja.
      if(readOnly){
        const[v,s]=await Promise.all([
          fetch("/api/v1/viajes",{headers}),
          fetch("/api/v1/sucursales",{headers}),
        ]);
        const dv=v.ok?await v.json():[];const ds=s.ok?await s.json():[];
        setItems(Array.isArray(dv)?dv:[]);setSucursales(Array.isArray(ds)?ds:[]);
        return;
      }
      const[r1,r2,r3,r4,r5]=await Promise.all([
        fetch("/api/v1/viajes",{headers}),
        fetch("/api/v1/rutas",{headers}),
        fetch("/api/v1/buses",{headers}),
        fetch("/api/v1/usuarios",{headers}),
        fetch("/api/v1/sucursales",{headers}),
      ]);
      const d1=r1.ok?await r1.json():[];const d2=r2.ok?await r2.json():[];
      const d3=r3.ok?await r3.json():[];const d4=r4.ok?await r4.json():[];
      const d5=r5.ok?await r5.json():[];
      setItems(Array.isArray(d1)?d1:[]);setRutas(Array.isArray(d2)?d2:[]);setBuses(Array.isArray(d3)?d3:[]);
      setSucursales(Array.isArray(d5)?d5:[]);
      // Filtrar solo choferes
      const usrs=Array.isArray(d4)?d4:[];
      setChoferes(usrs.filter(u=>u.rol==="ROLE_CHOFER"||u.rol?.includes("CHOFER")));
    }catch{showToast("Error al cargar viajes");}
    finally{setLoad(false);}
  },[headers,showToast,readOnly]);

  useEffect(()=>{cargar();},[cargar]);

  const abrirAsientos=async(viajeId)=>{
    setModalAsientos(viajeId);setLoadAsientos(true);setAsientosModal([]);
    try{const r=await fetch(`/api/v1/viajes/${viajeId}/asientos`,{headers});const d=r.ok?await r.json():[];setAsientosModal(Array.isArray(d)?d:[]);}
    catch{showToast("Error al cargar asientos");}
    finally{setLoadAsientos(false);}
  };

  const abrir=(v=null)=>{
    setForm(v?{
      id:v.id,rutaId:v.ruta?.id?.toString()||"",busId:v.bus?.id?.toString()||"",
      choferId:v.chofer?.id?.toString()||"",
      fechaHoraSalida:v.fechaHoraSalida?v.fechaHoraSalida.slice(0,16):"",
      fechaHoraLlegadaEstimada:v.fechaHoraLlegadaEstimada?v.fechaHoraLlegadaEstimada.slice(0,16):"",
      precioAdulto:v.precioAdulto?.toString()||"",precioNino:v.precioNino?.toString()||"",estado:v.estado||"PROGRAMADO",
    }:{rutaId:"",busId:"",choferId:"",fechaHoraSalida:"",fechaHoraLlegadaEstimada:"",precioAdulto:"",precioNino:"",estado:"PROGRAMADO"});
    setModal(v||"nuevo");
  };

  const guardar=async(e)=>{e.preventDefault();if(!form.rutaId||!form.busId||!form.fechaHoraSalida)return showToast("Ruta, bus y fecha son obligatorios");if(!form.precioAdulto||parseFloat(form.precioAdulto)<=0)return showToast("Precio adulto obligatorio");setSaving(true);
    try{const isEdit=!!form.id;const res=await fetch(isEdit?`/api/v1/viajes/${form.id}`:"/api/v1/viajes",{method:isEdit?"PUT":"POST",headers,body:JSON.stringify({rutaId:parseInt(form.rutaId),busId:parseInt(form.busId),choferId:form.choferId?parseInt(form.choferId):null,fechaHoraSalida:form.fechaHoraSalida,fechaHoraLlegadaEstimada:form.fechaHoraLlegadaEstimada||null,precioAdulto:parseFloat(form.precioAdulto),precioNino:form.precioNino?parseFloat(form.precioNino):null,estado:form.estado})});
    if(!res.ok){const err=await res.json().catch(()=>({}));throw new Error(err.mensaje||"Error");}
    const data=await res.json();setItems(prev=>isEdit?prev.map(x=>x.id===data.id?{...x,...data}:x):[...prev,data]);showToast(isEdit?"Viaje actualizado":"Viaje creado");setModal(null);}
    catch(err){showToast(err.message);}finally{setSaving(false);}
  };

  const eliminar=async(id)=>{try{await fetch(`/api/v1/viajes/${id}`,{method:"DELETE",headers});setItems(prev=>prev.filter(x=>x.id!==id));showToast("Viaje eliminado");}catch{showToast("Error");}finally{setConf(null);};};

  const ejecutarCancelacion = async () => {
    if(!cancelarViaje) return;
    setSaving(true);
    try {
      const res = await fetch(`/api/v1/viajes/${cancelarViaje.id}/estado?estado=CANCELADO`, { method: "PATCH", headers });
      if(res.ok) {
        showToast("Viaje cancelado correctamente");
        setCancelarViaje(null);
        await cargar();
      } else {
        const err = await res.json().catch(()=>({}));
        throw new Error(err.mensaje || "Error al cancelar");
      }
    } catch(e) {
      showToast(e.message);
    } finally {
      setSaving(false);
    }
  };

  const fil = items.filter(v => {
    const matchEstado = !filtroEstado || v.estado === filtroEstado;
    const matchOrigen = !filtroOrigen || v.ruta?.origen?.id === parseInt(filtroOrigen);
    const matchDestino = !filtroDestino || v.ruta?.destino?.id === parseInt(filtroDestino);
    return matchEstado && matchOrigen && matchDestino;
  });
  const prog=items.filter(v=>v.estado==="PROGRAMADO").length;
  const enCurso=items.filter(v=>v.estado==="EN_CURSO").length;

  return (<>
    <PageHeader title="Gestión de Viajes" sub={readOnly?`${items.length} viajes · consulta y disponibilidad de asientos`:`${items.length} viajes totales`} action={!readOnly&&<button className="adm-btn-primary" onClick={()=>abrir()}><Plus size={15} style={{marginRight:6}}/>Nuevo viaje</button>}/>

    {/* Mini stats + filtro */}
    <div style={{display:"flex",gap:10,marginBottom:14,flexWrap:"wrap",alignItems:"center"}}>
      {[{label:"Programados",val:prog,color:C.verde},{label:"En curso",val:enCurso,color:C.amarillo},{label:"Total",val:items.length,color:C.gris}].map(s=>(
        <div key={s.label} style={{background:C.blanco,border:`1.5px solid ${s.color}33`,borderRadius:10,padding:"8px 16px",display:"flex",alignItems:"center",gap:8}}>
          <span style={{fontSize:20,fontWeight:900,color:s.color}}>{s.val}</span>
          <span style={{fontSize:12,color:C.gris}}>{s.label}</span>
        </div>
      ))}
      <div style={{marginLeft:"auto",display:"flex",alignItems:"center",gap:8}}>
        <Filter size={14} color={C.medio}/>
        <select className="lf-select" value={filtroOrigen} onChange={e=>setFiltroOrigen(e.target.value)} style={{maxWidth:160,padding:"8px 12px"}}>
          <option value="">Cualquier origen</option>
          {sucursales.map(s=><option key={s.id} value={s.id}>{s.ciudad}</option>)}
        </select>
        <select className="lf-select" value={filtroDestino} onChange={e=>setFiltroDestino(e.target.value)} style={{maxWidth:160,padding:"8px 12px"}}>
          <option value="">Cualquier destino</option>
          {sucursales.map(s=><option key={s.id} value={s.id}>{s.ciudad}</option>)}
        </select>
        <select className="lf-select" value={filtroEstado} onChange={e=>setFiltroEstado(e.target.value)} style={{maxWidth:160,padding:"8px 12px"}}>
          <option value="">Todos los estados</option>
          <option value="PROGRAMADO">Programado</option>
          <option value="EN_CURSO">En curso</option>
          <option value="FINALIZADO">Finalizado</option>
          <option value="CANCELADO">Cancelado</option>
        </select>
      </div>
    </div>

    <TableWrap load={load} empty={!fil.length&&"Sin viajes registrados"}>
      <table className="adm-table tbl-viajes">
        <thead><tr><th>Ruta</th><th>Bus</th><th>Chofer asignado</th><th>Salida</th><th>Precio adulto</th><th>Asientos</th><th>Estado</th><th>Acciones</th></tr></thead>
        <tbody>
          {fil.map(v=>{
            const libres=v.asientosDisponibles??"-";const total=v.totalAsientos??"-";
            const pctOcup=total&&libres!=="-"?Math.round((1-libres/total)*100):0;
            const colorAsientos=libres>5?C.verde:libres>0?C.naranja:C.rojo;
            return(
              <tr key={v.id} className="tr-hover">
                <td><div style={{display:"flex",alignItems:"center",gap:5,fontWeight:700}}><MapPin size={12} color={C.verde}/>{v.ruta?.origen?.ciudad||"—"} <ArrowRight size={11} color={C.medio}/> {v.ruta?.destino?.ciudad||"—"}</div><span style={{fontSize:11,color:C.medio}}>{v.ruta?.codigo}</span></td>
                <td><span className="td-codigo">{v.bus?.placa||"—"}</span><div style={{fontSize:11,color:C.medio}}>{v.bus?.marca}</div></td>
                <td>
                  {v.chofer ? (
                    <div style={{display:"flex",alignItems:"center",gap:6}}>
                      <div style={{width:26,height:26,borderRadius:"50%",background:C.verde+"22",display:"flex",alignItems:"center",justifyContent:"center"}}>
                        <UserCircle size={16} color={C.verde}/>
                      </div>
                      <div>
                        <div style={{fontSize:12,fontWeight:700,color:C.oscuro}}>{v.chofer.nombreCompleto||v.chofer.nombres||v.choferNombre||"—"}</div>
                        <div style={{fontSize:10,color:C.medio}}>{v.chofer.username}</div>
                      </div>
                    </div>
                  ):<span style={{fontSize:12,color:C.grisClaro,fontStyle:"italic"}}>Sin asignar</span>}
                </td>
                <td style={{fontSize:12}}>{fmtFecha(v.fechaHoraSalida)}</td>
                <td><strong style={{color:C.verde}}>S/ {v.precioAdulto||"—"}</strong></td>
                <td>
                  <div style={{display:"flex",alignItems:"center",gap:6}}>
                    <Armchair size={13} color={colorAsientos}/>
                    <span style={{fontSize:12,fontWeight:700,color:colorAsientos}}>{libres}/{total}</span>
                  </div>
                  {total&&total!=="-"&&<div style={{width:60,height:4,background:"#e8f0ee",borderRadius:99,overflow:"hidden",marginTop:3}}><div style={{width:`${pctOcup}%`,height:"100%",background:colorAsientos,transition:"width 0.4s"}}/></div>}
                </td>
                <td><Badge text={v.estado}/></td>
                <td>
                  <div style={{display:"flex",flexDirection:"column",gap:6}}>
                    {!readOnly && (
                      <div style={{display:"flex",gap:4}}>
                        <button className="btn-edit" onClick={()=>abrir(v)} title="Editar" aria-label={`Editar viaje ${v.ruta?.codigo||v.id}`}><Pencil size={12}/></button>
                        {v.estado === "PROGRAMADO" && (
                          <button className="btn-delete" onClick={() => setCancelarViaje(v)} title="Cancelar Viaje" aria-label={`Cancelar viaje ${v.ruta?.codigo||v.id}`} style={{background:C.rojo+"22",color:C.rojo}}><X size={12}/></button>
                        )}
                        <button className="btn-delete" onClick={()=>setConf(v.id)} title="Eliminar" aria-label={`Eliminar viaje ${v.ruta?.codigo||v.id}`}><Trash2 size={12}/></button>
                      </div>
                    )}
                    <button onClick={()=>abrirAsientos(v.id)} aria-label={`Ver asientos del viaje ${v.ruta?.codigo||v.id}`} style={{background:`${C.amarillo}22`,border:`1px solid ${C.amarillo}66`,color:"#9a7a00",borderRadius:9,padding:"7px 10px",minHeight:36,fontSize:11,cursor:"pointer",fontFamily:"inherit",fontWeight:700,display:"flex",alignItems:"center",justifyContent:"center",gap:4}}>
                      <Armchair size={11}/> Asientos
                    </button>
                  </div>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </TableWrap>

    {/* Modal Mapa de Asientos */}
    {modalAsientos&&(
      <div className="modal-overlay" onClick={()=>setModalAsientos(null)}>
        <div className="modal" style={{maxWidth:620,width:"95%"}} onClick={e=>e.stopPropagation()}>
          <div className="modal-header">
            <div className="modal-badge"><Armchair size={20}/></div>
            <div><div className="modal-title">Mapa de Asientos</div><div className="modal-subtitle">Vista gráfica del viaje seleccionado</div></div>
            <button className="modal-close" onClick={()=>setModalAsientos(null)}><X size={20}/></button>
          </div>
          <div className="modal-body"><MapaAsientosGrafico asientos={asientosModal} loading={loadAsientos}/></div>
        </div>
      </div>
    )}

    {/* Modal Crear/Editar Viaje (sólo ADMIN) */}
    {modal&&!readOnly&&(<ModalForm titulo={modal==="nuevo"?"Nuevo viaje":"Editar viaje"} icon={<CalendarDays size={20}/>} onClose={()=>setModal(null)} onSubmit={guardar} saving={saving}>
      <div className="form-grid">
        <Field label="Ruta *">
          <select className="lf-select" value={form.rutaId||""} onChange={e=>setForm(f=>({...f,rutaId:e.target.value}))} required>
            <option value="">Seleccionar...</option>
            {rutas.map(r=><option key={r.id} value={r.id}>{r.codigo||`Ruta ${r.id}`} — {r.origen?.ciudad} → {r.destino?.ciudad}</option>)}
          </select>
        </Field>
        <Field label="Bus *">
          <select className="lf-select" value={form.busId||""} onChange={e=>setForm(f=>({...f,busId:e.target.value}))} required>
            <option value="">Seleccionar...</option>
            {buses.map(b=><option key={b.id} value={b.id} disabled={b.estadoConfiguracion==="INCOMPLETO"}>{b.placa} — {b.marca} ({b.capacidadAsientos} asientos){b.estadoConfiguracion==="INCOMPLETO"?" · MAPA INCOMPLETO":""}</option>)}
          </select>
        </Field>
        <Field label="Chofer asignado" style={{gridColumn:"1/-1"}}>
          <select className="lf-select" value={form.choferId||""} onChange={e=>setForm(f=>({...f,choferId:e.target.value}))}>
            <option value="">Sin chofer asignado</option>
            {choferes.map(c=><option key={c.id} value={c.id}>{c.nombres} {c.apellidos} ({c.username})</option>)}
          </select>
          {choferes.length===0&&<div style={{fontSize:11,color:C.naranja,marginTop:4,display:"flex",alignItems:"center",gap:4}}><AlertTriangle size={11}/>No hay choferes registrados. Crea uno en Personal.</div>}
        </Field>
        <Field label="Fecha y hora salida *" style={{gridColumn:"1/-1"}}>
          <input type="datetime-local" value={form.fechaHoraSalida||""} onChange={e=>setForm(f=>({...f,fechaHoraSalida:e.target.value}))} required/>
        </Field>
        <Field label="Fecha y hora llegada estimada" style={{gridColumn:"1/-1"}}>
          <input type="datetime-local" value={form.fechaHoraLlegadaEstimada||""} onChange={e=>setForm(f=>({...f,fechaHoraLlegadaEstimada:e.target.value}))}/>
        </Field>
        <Field label="Precio adulto (S/) *">
          <input type="number" step="0.01" value={form.precioAdulto||""} onChange={e=>setForm(f=>({...f,precioAdulto:e.target.value}))} required placeholder="0.00"/>
        </Field>
        <Field label="Precio niño (S/)">
          <input type="number" step="0.01" value={form.precioNino||""} onChange={e=>setForm(f=>({...f,precioNino:e.target.value}))} placeholder="0.00"/>
        </Field>
        <Field label="Estado" style={{gridColumn:"1/-1"}}>
          {(()=>{
            const trans={PROGRAMADO:["PROGRAMADO","EN_CURSO","CANCELADO"],EN_CURSO:["EN_CURSO","FINALIZADO","CANCELADO"],FINALIZADO:["FINALIZADO"],CANCELADO:["CANCELADO"]};
            const orig=(typeof modal==="object"&&modal?.estado)?modal.estado:"PROGRAMADO";
            const opts=trans[orig]||["PROGRAMADO","EN_CURSO","FINALIZADO","CANCELADO"];
            return(<select className="lf-select" value={form.estado||"PROGRAMADO"} onChange={e=>setForm(f=>({...f,estado:e.target.value}))}>
              {opts.map(op=><option key={op} value={op}>{op.replace("_"," ")}</option>)}
            </select>);
          })()}
        </Field>
      </div>
    </ModalForm>)}
    {conf&&<ConfirmModal onConfirmar={()=>eliminar(conf)} onCancelar={()=>setConf(null)}/>}

    {cancelarViaje && (
      <div className="modal-overlay" onClick={() => setCancelarViaje(null)}>
        <div className="modal confirm-modal" style={{ maxWidth: 420 }} onClick={e => e.stopPropagation()}>
          <div className="modal-header">
            <div className="modal-badge" style={{ background: C.rojo+"22", color: C.rojo }}><AlertTriangle size={22}/></div>
            <div>
              <div className="modal-title">Cancelar Viaje Completo</div>
              <div className="modal-subtitle">Esta acción es irreversible</div>
            </div>
            <button className="modal-close" onClick={() => setCancelarViaje(null)}><X size={20}/></button>
          </div>
          <div className="modal-body">
            <div style={{ background: "#fff5f5", border: `1px solid ${C.rojo}22`, borderRadius: 12, padding: 14, marginBottom: 16 }}>
              <div style={{ fontSize: 13, color: C.rojo, fontWeight: 700, marginBottom: 4 }}>ATENCIÓN:</div>
              <div style={{ fontSize: 12, color: C.gris, lineHeight: 1.5 }}>
                Está por cancelar el viaje <strong>{cancelarViaje.ruta?.origen?.ciudad} → {cancelarViaje.ruta?.destino?.ciudad}</strong> programado para el <strong>{fmtFecha(cancelarViaje.fechaHoraSalida)}</strong>.
                <br/><br/>
                • Todos los asientos reservados se liberarán.<br/>
                • El estado del viaje cambiará a <strong>CANCELADO</strong>.
              </div>
            </div>
            <div style={{ display: "flex", gap: 10, marginTop: 20 }}>
              <button className="btn-cancelar" onClick={() => setCancelarViaje(null)} style={{ flex: 1 }}>Volver</button>
              <button className="btn-delete-confirm" onClick={ejecutarCancelacion} disabled={saving} style={{ flex: 1.5, justifyContent: "center" }}>
                {saving ? <span className="spinner" /> : "Confirmar Cancelación"}
              </button>
            </div>
          </div>
        </div>
      </div>
    )}
  </>);
}


// ═══════════════════════════════════════════════════════
// CRUD: Clientes
// ═══════════════════════════════════════════════════════
function CrudClientes({ headers, showToast, puedeEliminar=true }) {
  const [items,setItems]=useState([]);const [load,setLoad]=useState(true);const [buscar,setBuscar]=useState("");
  const [modal,setModal]=useState(null);const [form,setForm]=useState({});const [conf,setConf]=useState(null);const [saving,setSaving]=useState(false);
  const [modalHistorial, setModalHistorial] = useState(null);
  const [historial, setHistorial] = useState({ boletos: [], encomiendas: [], load: false });

  const cargar=useCallback(async()=>{setLoad(true);try{const r=await fetch("/api/v1/clientes",{headers});const d=r.ok?await r.json():{};const arr=Array.isArray(d.content)?d.content:Array.isArray(d)?d:[];setItems(arr);}catch{showToast("Error al cargar clientes");setItems([]);}finally{setLoad(false);};},[headers,showToast]);
  useEffect(()=>{cargar();},[cargar]);

  const abrir=(c=null)=>{setForm(c?{id:c.id,nombres:c.nombres||"",apellidos:c.apellidos||"",dniRuc:c.dniRuc||"",email:c.email||"",telefono:c.telefono||"",tipoCliente:c.tipoCliente||"PERSONA"}:{nombres:"",apellidos:"",dniRuc:"",email:"",telefono:"",tipoCliente:"PERSONA"});setModal(c||"nuevo");};
  const guardar=async(e)=>{e.preventDefault();if(!form.nombres||!form.apellidos||!form.dniRuc)return showToast("Nombres, apellidos y DNI son obligatorios");setSaving(true);try{const isEdit=!!form.id;const res=await fetch(isEdit?`/api/v1/clientes/${form.id}`:"/api/v1/clientes",{method:isEdit?"PUT":"POST",headers,body:JSON.stringify({nombres:form.nombres.trim(),apellidos:form.apellidos.trim(),dniRuc:form.dniRuc.trim(),email:form.email?.trim()||null,telefono:form.telefono?.trim()||null,tipoCliente:form.tipoCliente||"PERSONA"})});if(!res.ok){const err=await res.json().catch(()=>({}));throw new Error(err.mensaje||"Error");}const data=await res.json();setItems(prev=>isEdit?prev.map(x=>x.id===data.id?data:x):[...prev,data]);showToast(isEdit?"Cliente actualizado":"Cliente creado");setModal(null);}catch(err){showToast(err.message);}finally{setSaving(false);};};
  const eliminar=async(id)=>{try{await fetch(`/api/v1/clientes/${id}`,{method:"DELETE",headers});setItems(prev=>prev.filter(x=>x.id!==id));showToast("Cliente eliminado");}catch{showToast("Error");}finally{setConf(null);};};

  const verHistorial = async (cliente) => {
    setModalHistorial(cliente);
    setHistorial({ boletos: [], encomiendas: [], load: true });
    try {
      const [r1, r2] = await Promise.all([
        fetch(`/api/v1/boletos/cliente/${cliente.id}`, { headers }),
        fetch(`/api/v1/encomiendas/remitente/${cliente.id}`, { headers })
      ]);
      const d1 = r1.ok ? await r1.json() : { content: [] };
      const d2 = r2.ok ? await r2.json() : { content: [] };
      setHistorial({
        boletos: Array.isArray(d1.content) ? d1.content : Array.isArray(d1) ? d1 : [],
        encomiendas: Array.isArray(d2.content) ? d2.content : Array.isArray(d2) ? d2 : [],
        load: false
      });
    } catch {
      showToast("Error al cargar historial");
      setHistorial(prev => ({ ...prev, load: false }));
    }
  };

  const fil=items.filter(c=>`${c.nombres} ${c.apellidos} ${c.dniRuc}`.toLowerCase().includes(buscar.toLowerCase()));

  return (<>
    <PageHeader title="Gestión de Clientes" sub={`${items.length} clientes`} action={<button className="adm-btn-primary" onClick={()=>abrir()}><Plus size={15} style={{marginRight:6}}/>Nuevo cliente</button>}/>
    <SearchBar value={buscar} onChange={setBuscar} placeholder="Buscar por nombre o DNI..."/>
    <TableWrap load={load} empty={!fil.length&&"Sin clientes registrados"}>
      <table className="adm-table tbl-clientes">
        <thead><tr><th>Nombre completo</th><th>DNI/RUC</th><th>Email</th><th>Teléfono</th><th>Tipo</th><th>Acciones</th></tr></thead>
        <tbody>
          {fil.map(c=>(
            <tr key={c.id} className="tr-hover">
              <td><strong>{[c.nombres,c.apellidos].filter(Boolean).join(" ")}</strong></td>
              <td><span className="td-codigo">{c.dniRuc}</span></td>
              <td style={{fontSize:12}}>{c.email||"—"}</td>
              <td>{c.telefono||"—"}</td>
              <td><Badge text={c.tipoCliente||"PERSONA"}/></td>
              <td>
                <Actions onEdit={()=>abrir(c)} onDel={puedeEliminar?()=>setConf(c.id):undefined} extra={
                  <button className="btn-edit" onClick={()=>verHistorial(c)} title="Ver Historial" aria-label={`Ver historial de ${[c.nombres,c.apellidos].filter(Boolean).join(" ")}`} style={{background:C.amarillo+"22",color:"#9a7a00",marginLeft:4}}>
                    <Clock size={13}/>
                  </button>
                }/>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </TableWrap>
    {modal&&(<ModalForm titulo={modal==="nuevo"?"Nuevo cliente":"Editar cliente"} icon={<Users size={20}/>} onClose={()=>setModal(null)} onSubmit={guardar} saving={saving}>
      <div className="form-grid">
        <Field label="Nombres *"><input value={form.nombres||""} onChange={e=>setForm(f=>({...f,nombres:e.target.value}))} required/></Field>
        <Field label="Apellidos *"><input value={form.apellidos||""} onChange={e=>setForm(f=>({...f,apellidos:e.target.value}))} required/></Field>
        <Field label="DNI / RUC *"><input value={form.dniRuc||""} onChange={e=>setForm(f=>({...f,dniRuc:e.target.value}))} required/></Field>
        <Field label="Tipo">
          <select className="lf-select" value={form.tipoCliente||"PERSONA"} onChange={e=>setForm(f=>({...f,tipoCliente:e.target.value}))}>
            <option value="PERSONA">PERSONA</option><option value="EMPRESA">EMPRESA</option>
          </select>
        </Field>
        <Field label="Email"><input type="email" value={form.email||""} onChange={e=>setForm(f=>({...f,email:e.target.value}))}/></Field>
        <Field label="Teléfono"><input value={form.telefono||""} onChange={e=>setForm(f=>({...f,telefono:e.target.value}))}/></Field>
      </div>
    </ModalForm>)}
    {conf&&<ConfirmModal onConfirmar={()=>eliminar(conf)} onCancelar={()=>setConf(null)}/>}

    {modalHistorial && (
      <div className="modal-overlay" onClick={() => setModalHistorial(null)}>
        <div className="modal" style={{ maxWidth: 800, width: "95%" }} onClick={e => e.stopPropagation()}>
          <div className="modal-header">
            <div className="modal-badge"><Clock size={20} /></div>
            <div>
              <div className="modal-title">Historial del Cliente</div>
              <div className="modal-subtitle">{modalHistorial.nombres} {modalHistorial.apellidos} — {modalHistorial.dniRuc}</div>
            </div>
            <button className="modal-close" onClick={() => setModalHistorial(null)}><X size={20} /></button>
          </div>
          <div className="modal-body" style={{ maxHeight: "70vh", overflowY: "auto" }}>
            {historial.load ? (
              <div style={{ textAlign: "center", padding: "2rem" }}><span className="spinner" style={{ borderColor: C.verde, borderTopColor: "transparent" }} /></div>
            ) : (
              <div style={{ display: "flex", flexDirection: "column", gap: 24 }}>
                <section>
                  <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 12, borderBottom: `1.5px solid ${C.verde}22`, paddingBottom: 8 }}>
                    <Ticket size={18} color={C.verde} />
                    <h4 style={{ margin: 0, color: C.oscuro }}>Historial de Viajes (Boletos)</h4>
                  </div>
                  {historial.boletos.length === 0 ? (
                    <div style={{ padding: "20px", textAlign: "center", color: C.medio, background: "#f8faf9", borderRadius: 10 }}>No registra viajes comprados.</div>
                  ) : (
                    <div className="adm-table-wrap">
                      <table className="adm-table tbl-clientes-boleto">
                        <thead>
                          <tr><th>Fecha</th><th>Ruta</th><th>Bus</th><th>Asiento</th><th>Precio</th><th>Estado</th></tr>
                        </thead>
                        <tbody>
                          {historial.boletos.map(b => (
                            <tr key={b.id}>
                              <td>{fmtFecha(b.createdAt)}</td>
                              <td><strong>{b.viaje?.ruta?.origen?.ciudad}</strong> → <strong>{b.viaje?.ruta?.destino?.ciudad}</strong></td>
                              <td><span className="td-codigo">{b.viaje?.bus?.placa}</span></td>
                              <td>#{b.numeroAsiento}</td>
                              <td><strong style={{ color: C.verde }}>S/ {b.precioFinal?.toFixed(2)}</strong></td>
                              <td><Badge text={b.estado} /></td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                </section>
                <section>
                  <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 12, borderBottom: `1.5px solid ${C.naranja}22`, paddingBottom: 8 }}>
                    <Package size={18} color={C.naranja} />
                    <h4 style={{ margin: 0, color: C.oscuro }}>Historial de Encomiendas Enviadas</h4>
                  </div>
                  {historial.encomiendas.length === 0 ? (
                    <div style={{ padding: "20px", textAlign: "center", color: C.medio, background: "#fff9f4", borderRadius: 10 }}>No registra envíos de encomiendas.</div>
                  ) : (
                    <div className="adm-table-wrap">
                      <table className="adm-table tbl-clientes-enc">
                        <thead>
                          <tr><th>Guía</th><th>Fecha</th><th>Destinatario</th><th>Origen/Destino</th><th>Peso</th><th>Costo</th><th>Estado</th></tr>
                        </thead>
                        <tbody>
                          {historial.encomiendas.map(e => (
                            <tr key={e.id}>
                              <td><span className="td-codigo">{e.numeroGuia}</span></td>
                              <td>{fmtFecha(e.createdAt)}</td>
                              <td>{e.destinatario?.nombres} {e.destinatario?.apellidos}</td>
                              <td style={{ fontSize: 11 }}>{e.sucursalOrigen?.ciudad} → {e.sucursalDestino?.ciudad}</td>
                              <td>{e.pesoKg} kg</td>
                              <td><strong style={{ color: C.verde }}>S/ {e.costo?.toFixed(2)}</strong></td>
                              <td><Badge text={e.estado} /></td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                </section>
              </div>
            )}
          </div>
        </div>
      </div>
    )}
  </>);
}

// ═══════════════════════════════════════════════════════
// CRUD: Encomiendas
// ═══════════════════════════════════════════════════════
const ENC_ESTADOS=["SOLICITUD_PENDIENTE","RECIBIDO","EN_ALMACEN","CARGADO_EN_BUS","EN_TRANSITO","LLEGADO_A_DESTINO","EN_DESTINO","LISTO_ENTREGA","ENTREGADO","DEVUELTO","PERDIDO"];
const ENC_TRANSICIONES={
  SOLICITUD_PENDIENTE:["RECIBIDO"],
  RECIBIDO:["EN_ALMACEN","CARGADO_EN_BUS","EN_TRANSITO"],
  EN_ALMACEN:["CARGADO_EN_BUS","EN_TRANSITO"],
  CARGADO_EN_BUS:["EN_TRANSITO"],
  EN_TRANSITO:["EN_DESTINO","LLEGADO_A_DESTINO"],
  EN_DESTINO:["LISTO_ENTREGA","ENTREGADO"],
  LLEGADO_A_DESTINO:["LISTO_ENTREGA","ENTREGADO"],
  LISTO_ENTREGA:["ENTREGADO","DEVUELTO"],
  ENTREGADO:[],DEVUELTO:[],PERDIDO:[],
};
// Métodos de pago admitidos en encomiendas. El pago es PRESENCIAL e inmediato:
// se excluye CONTRA_ENTREGA por regla del módulo.
const ENC_METODOS=["EFECTIVO","YAPE","PLIN","TARJETA_CREDITO","TARJETA_DEBITO","TRANSFERENCIA"];
const TIPOS_CLIENTE=[{v:"PERSONA",l:"Persona natural"},{v:"EMPRESA",l:"Empresa / RUC"}];
const encNombre=(e)=>e?.nombreCompleto||`${e?.nombres||""} ${e?.apellidos||""}`.trim()||`#${e?.id||""}`;

// ═══════════════════════════════════════════════════════
// Selector de clientes con búsqueda y alta en línea
// Reutiliza /api/v1/clientes (ClienteService): si el documento ya existe se
// reutiliza el mismo cliente; nunca se crean duplicados.
// ═══════════════════════════════════════════════════════
export function SelectorCliente({ headers, label, value, onChange, showToast, requerido=true }) {
  const [q,setQ]=useState("");
  const [res,setRes]=useState([]);
  const [buscando,setBuscando]=useState(false);
  const [nuevo,setNuevo]=useState(false);
  const [f,setF]=useState({dniRuc:"",nombres:"",apellidos:"",razonSocial:"",telefono:"",email:"",direccion:"",distrito:"",ciudad:"",tipoCliente:"PERSONA"});
  const [guardando,setGuardando]=useState(false);

  const buscar=useCallback(async()=>{
    setBuscando(true);
    try{
      const res=await fetch(`/api/v1/clientes?q=${encodeURIComponent(q)}&page=0&size=8`,{headers});
      const d=res.ok?await res.json():{};
      setRes(Array.isArray(d?.content)?d.content:[]);
    }catch{showToast("Error al buscar clientes");}
    finally{setBuscando(false);}
  },[q,headers,showToast]);

  const crear=async(e)=>{
    e.preventDefault();
    if(!f.dniRuc.trim()||!f.telefono.trim()) return showToast("Documento y teléfono son obligatorios");
    setGuardando(true);
    try{
      const res=await fetch("/api/v1/clientes",{method:"POST",headers,body:JSON.stringify({
        dniRuc:f.dniRuc.trim(),
        nombres:(f.nombres||"").trim()||null,
        apellidos:(f.apellidos||"").trim()||null,
        razonSocial:(f.razonSocial||"").trim()||null,
        telefono:f.telefono.trim(),
        email:(f.email||"").trim()||null,
        direccion:(f.direccion||"").trim()||null,
        distrito:(f.distrito||"").trim()||null,
        ciudad:(f.ciudad||"").trim()||null,
        tipoCliente:f.tipoCliente,
      })});
      if(!res.ok){const err=await res.json().catch(()=>({}));throw new Error(err.mensaje||"No se pudo registrar el cliente");}
      const c=await res.json();
      onChange(c);setNuevo(false);setF({dniRuc:"",nombres:"",apellidos:"",razonSocial:"",telefono:"",email:"",direccion:"",distrito:"",ciudad:"",tipoCliente:"PERSONA"});
      showToast("Cliente registrado y seleccionado");
    }catch(err){showToast(err.message);}
    finally{setGuardando(false);}
  };

  if(value){
    return (
      <Field label={label}>
        <div style={{display:"flex",gap:8,alignItems:"center",background:"rgba(18,115,105,0.08)",border:"1px solid rgba(18,115,105,0.3)",borderRadius:8,padding:"9px 11px",minHeight:44}}>
          <div style={{flex:1,minWidth:0}}>
            <div style={{fontSize:13,fontWeight:700,color:C.oscuro,overflow:"hidden",textOverflow:"ellipsis",whiteSpace:"nowrap"}}>{encNombre(value)}</div>
            <div style={{fontSize:11,color:C.medio}}>{value.dniRuc||"sin doc."} · {value.telefono||"sin tel."}</div>
          </div>
          <button type="button" className="btn-cancelar" onClick={()=>onChange(null)} style={{padding:"4px 10px",minHeight:32,fontSize:11}}>Cambiar</button>
        </div>
      </Field>
    );
  }

  return (
    <Field label={label}>
      <div style={{display:"flex",gap:8}}>
        <input value={q} onChange={e=>setQ(e.target.value)} onKeyDown={e=>{if(e.key==="Enter"){e.preventDefault();buscar();}}}
          placeholder="Documento, nombre o teléfono" aria-label={`Buscar ${label||"cliente"}`} style={{flex:1}}/>
        <button type="button" className="btn-cancelar" onClick={buscar} style={{minHeight:44,whiteSpace:"nowrap"}}>
          {buscando?"Buscando...":"Buscar"}
        </button>
      </div>
      {!nuevo?(
        <>
          {res.length>0&&(
            <div style={{marginTop:8,maxHeight:150,overflowY:"auto",border:"1px solid rgba(18,115,105,0.2)",borderRadius:8}}>
              {res.map(c=>(
                <button key={c.id} type="button" onClick={()=>{onChange(c);setRes([]);setQ("");}}
                  style={{display:"block",width:"100%",textAlign:"left",background:"transparent",border:"none",borderBottom:"1px solid rgba(18,115,105,0.1)",padding:"8px 11px",cursor:"pointer",fontFamily:"inherit"}}>
                  <div style={{fontSize:13,fontWeight:700,color:C.oscuro}}>{encNombre(c)}</div>
                  <div style={{fontSize:11,color:C.medio}}>{c.dniRuc||"—"} · {c.telefono||"—"}</div>
                </button>
              ))}
            </div>
          )}
          <button type="button" onClick={()=>setNuevo(true)}
            style={{marginTop:8,width:"100%",minHeight:44,background:"rgba(245,197,24,0.1)",border:"1px solid rgba(245,197,24,0.45)",borderRadius:8,color:C.naranja,fontWeight:700,fontSize:12,cursor:"pointer",fontFamily:"inherit",display:"flex",alignItems:"center",justifyContent:"center",gap:6}}>
            <Plus size={14}/> Registrar cliente nuevo
          </button>
          {requerido&&<p style={{fontSize:11,color:C.medio,marginTop:6}}>Busca por documento, nombre o teléfono. Si el cliente ya existe, selecciónalo para reutilizarlo.</p>}
        </>
      ):(
        <form onSubmit={crear} style={{marginTop:10,padding:12,background:"rgba(245,197,24,0.06)",border:"1px solid rgba(245,197,24,0.3)",borderRadius:10}}>
          <p style={{fontSize:11,color:C.naranja,marginBottom:10,lineHeight:1.4}}>
            Alta de cliente. Si el documento ya está registrado, el sistema reutilizará el cliente existente.
          </p>
          <div className="form-grid">
            <Field label="DNI / RUC *"><input value={f.dniRuc} onChange={e=>setF(x=>({...x,dniRuc:e.target.value}))} maxLength={11} required placeholder="8 dígitos o 11"/></Field>
            <Field label="Teléfono *"><input value={f.telefono} onChange={e=>setF(x=>({...x,telefono:e.target.value}))} maxLength={20} required placeholder="999 888 777"/></Field>
            <Field label="Nombres"><input value={f.nombres} onChange={e=>setF(x=>({...x,nombres:e.target.value}))} maxLength={150}/></Field>
            <Field label="Apellidos"><input value={f.apellidos} onChange={e=>setF(x=>({...x,apellidos:e.target.value}))} maxLength={150}/></Field>
            <Field label="Razón social"><input value={f.razonSocial} onChange={e=>setF(x=>({...x,razonSocial:e.target.value}))} maxLength={250}/></Field>
            <Field label="Tipo de cliente *">
              <select className="lf-select" value={f.tipoCliente} onChange={e=>setF(x=>({...x,tipoCliente:e.target.value}))}>
                {TIPOS_CLIENTE.map(t=><option key={t.v} value={t.v}>{t.l}</option>)}
              </select>
            </Field>
            <Field label="Email"><input type="email" value={f.email} onChange={e=>setF(x=>({...x,email:e.target.value}))} maxLength={150}/></Field>
            <Field label="Dirección"><input value={f.direccion} onChange={e=>setF(x=>({...x,direccion:e.target.value}))} maxLength={300}/></Field>
          </div>
          <div style={{display:"flex",gap:8,marginTop:10}}>
            <button type="button" className="btn-cancelar" onClick={()=>setNuevo(false)} style={{flex:1}}>Cancelar</button>
            <button type="submit" className="btn-reservar" style={{flex:2}} disabled={guardando}>
              {guardando?<span className="spinner"/>:<span>Registrar y usar</span>}
            </button>
          </div>
        </form>
      )}
    </Field>
  );
}

// Bloque de datos oficiales: peso, medidas, tarifa y método de pago.
// Lo usan tanto el registro presencial como la confirmación de una SOL.
// El volumen es solo vista previa: el backend lo calcula y lo valida.
function BloqueDatosOficiales({ conf, setConf }) {
  const l=Number(conf.largoCm),a=Number(conf.anchoCm),h=Number(conf.altoCm);
  const volumen=(l>0&&a>0&&h>0)?`${Number(((l*a*h)/1e6).toFixed(4))} m3 (calculado por el backend)`:"Ingresa largo, ancho y alto";
  return (
    <div className="form-grid">
      <Field label="Peso real (kg) *"><input type="number" min="0.001" step="0.001" value={conf.pesoKg} onChange={e=>setConf(c=>({...c,pesoKg:e.target.value}))} required/></Field>
      <Field label="Valor declarado (S/)"><input type="number" min="0" step="0.01" value={conf.valorDeclarado} onChange={e=>setConf(c=>({...c,valorDeclarado:e.target.value}))} placeholder="Opcional"/></Field>
      <Field label="Largo real (cm) *"><input type="number" min="0.1" step="0.1" value={conf.largoCm} onChange={e=>setConf(c=>({...c,largoCm:e.target.value}))} required/></Field>
      <Field label="Ancho real (cm) *"><input type="number" min="0.1" step="0.1" value={conf.anchoCm} onChange={e=>setConf(c=>({...c,anchoCm:e.target.value}))} required/></Field>
      <Field label="Alto real (cm) *"><input type="number" min="0.1" step="0.1" value={conf.altoCm} onChange={e=>setConf(c=>({...c,altoCm:e.target.value}))} required/></Field>
      <Field label="Volumen">
        <input readOnly value={volumen} style={{background:"rgba(18,115,105,0.08)",fontWeight:700}}/>
      </Field>
      <Field label="Tarifa oficial (S/) *">
        <input type="number" min="0.01" step="0.01" value={conf.tarifaOficial} onChange={e=>setConf(c=>({...c,tarifaOficial:e.target.value}))} required placeholder="Monto cobrado en agencia"/>
      </Field>
      <Field label="Método de pago recibido *">
        <select className="lf-select" value={conf.metodoPago} onChange={e=>setConf(c=>({...c,metodoPago:e.target.value}))} required>
          {ENC_METODOS.map(m=><option key={m} value={m}>{m.replace(/_/g," ")}</option>)}
        </select>
      </Field>
      <Field label="Observación" style={{gridColumn:"1/-1"}}>
        <input value={conf.observaciones} onChange={e=>setConf(c=>({...c,observaciones:e.target.value}))} maxLength={300} placeholder="Opcional"/>
      </Field>
    </div>
  );
}

const CONF_VACIO={pesoKg:"",largoCm:"",anchoCm:"",altoCm:"",valorDeclarado:"",tarifaOficial:"",metodoPago:"EFECTIVO",observaciones:""};

/**
 * Clave de idempotencia del registro presencial: se genera una sola vez por
 * formulario abierto y se envía en cada intento, de modo que un doble clic, un
 * reintento o una doble pestaña no creen una segunda encomienda con otra guía y
 * otro pago. El backend la persiste en encomiendas.clave_idempotencia.
 */
const generarClaveIdempotencia=()=>{
  const aleatorio=(globalThis.crypto?.randomUUID?.()||`${Date.now()}-${Math.random().toString(36).slice(2)}`);
  return `PRES-${aleatorio}`.slice(0,60);
};

function CrudEncomiendas({ headers, showToast }) {
  const [items,setItems]=useState([]);const [sucursales,setSucursales]=useState([]);const [solicitudes,setSolicitudes]=useState([]);
  const [load,setLoad]=useState(true);const [buscar,setBuscar]=useState("");const [filtroEstado,setFiltroEstado]=useState("");
  const [modal,setModal]=useState(null);const [detalle,setDetalle]=useState(null);
  const [nuevoEst,setNuevoEst]=useState("");const [obsEst,setObsEst]=useState("");const [saving,setSaving]=useState(false);
  // Filtros del panel de solicitudes pendientes
  const [qSol,setQSol]=useState("");const [solOrigen,setSolOrigen]=useState("");const [solDestino,setSolDestino]=useState("");
  const [solDesde,setSolDesde]=useState("");const [solHasta,setSolHasta]=useState("");
  // Modal de registro: "presencial" | "recuperar"
  const [modo,setModo]=useState(null);
  const [form,setForm]=useState({remitente:null,destinatario:null,sucursalOrigenId:"",sucursalDestinoId:"",descripcionContenido:"",tipoEnvio:"NORMAL",cuidadoEspecial:"",viajeId:"",claveIdempotencia:null});
  const [conf,setConf]=useState(CONF_VACIO);
  const [refSol,setRefSol]=useState("");const [sol,setSol]=useState(null);

  const cargar=useCallback(async()=>{
    setLoad(true);
    try{
      const [rEnc,rSuc]=await Promise.all([
        fetch("/api/v1/encomiendas",{headers}),
        fetch("/api/v1/sucursales",{headers}),
      ]);
      const dEnc=rEnc.ok?await rEnc.json():[];
      const dSuc=rSuc.ok?await rSuc.json():[];
      setItems(Array.isArray(dEnc)?dEnc:Array.isArray(dEnc?.content)?dEnc.content:[]);
      setSucursales(Array.isArray(dSuc)?dSuc:Array.isArray(dSuc?.content)?dSuc.content:[]);
    }catch{showToast("Error al cargar encomiendas");}
    finally{setLoad(false);}
  },[headers,showToast]);

  const cargarSolicitudes=useCallback(async()=>{
    try{
      const p=new URLSearchParams({page:"0",size:"50"});
      if(qSol.trim())p.set("q",qSol.trim());
      if(solOrigen)p.set("sucursalOrigenId",solOrigen);
      if(solDestino)p.set("sucursalDestinoId",solDestino);
      if(solDesde)p.set("desde",solDesde);
      if(solHasta)p.set("hasta",solHasta);
      const res=await fetch(`/api/v1/encomiendas/solicitudes-pendientes?${p}`,{headers});
      const d=res.ok?await res.json():{};
      setSolicitudes(Array.isArray(d?.content)?d.content:[]);
    }catch{showToast("Error al cargar solicitudes pendientes");}
  },[headers,showToast,qSol,solOrigen,solDestino,solDesde,solHasta]);

  useEffect(()=>{cargar();},[cargar]);
  useEffect(()=>{cargarSolicitudes();},[cargarSolicitudes]);

  // ── Apertura de modales ────────────────────────────────────────────────
  const abrirPresencial=()=>{
    setForm({remitente:null,destinatario:null,sucursalOrigenId:"",sucursalDestinoId:"",descripcionContenido:"",tipoEnvio:"NORMAL",cuidadoEspecial:"",viajeId:"",claveIdempotencia:generarClaveIdempotencia()});
    setConf(CONF_VACIO);setSol(null);setRefSol("");setModo("presencial");
  };
  const abrirRecuperar=(s)=>{
    setModo("recuperar");
    if(s){
      setSol(s);setRefSol(s.referenciaSolicitud||"");
      setForm(f=>({...f,sucursalOrigenId:s.sucursalOrigen?.id||"",sucursalDestinoId:s.sucursalDestino?.id||"",descripcionContenido:s.descripcionContenido||""}));
    }else{setSol(null);setRefSol("");}
    setConf(CONF_VACIO);
  };
  const abrirEstado=(enc)=>{setDetalle(null);setModal(enc);setNuevoEst("");setObsEst("");};

  // ── Recuperar una solicitud por referencia SOL ─────────────────────────
  const buscarSol=async()=>{
    if(!refSol.trim())return showToast("Ingresa la referencia SOL (SOL-AAAA-NNNNNN)");
    setSaving(true);
    try{
      const res=await fetch(`/api/v1/encomiendas/solicitudes-pendientes/${encodeURIComponent(refSol.trim())}`,{headers});
      if(!res.ok){const err=await res.json().catch(()=>({}));throw new Error(err.mensaje||"No se encontró la solicitud pendiente");}
      abrirRecuperar(await res.json());
      showToast("Solicitud cargada. Completa los datos oficiales.");
    }catch(err){showToast(err.message);}
    finally{setSaving(false);}
  };

  // ── Camino A: registro presencial ──────────────────────────────────────
  const guardar=async(e)=>{
    e.preventDefault();
    if(saving)return;
    if(!form.remitente||!form.destinatario) return showToast("Selecciona o registra el remitente y el destinatario");
    if(!form.sucursalOrigenId||!form.sucursalDestinoId) return showToast("Selecciona origen y destino");
    if(!form.descripcionContenido.trim()) return showToast("Describe el contenido del paquete");
    if(!(Number(conf.pesoKg)>0)) return showToast("Ingresa el peso real del paquete");
    if(!(Number(conf.largoCm)>0)||!(Number(conf.anchoCm)>0)||!(Number(conf.altoCm)>0)) return showToast("Ingresa largo, ancho y alto del paquete");
    if(!(Number(conf.tarifaOficial)>0)) return showToast("Ingresa la tarifa oficial");
    setSaving(true);
    try{
      const payload={
        remitenteId:Number(form.remitente.id),
        destinatarioId:Number(form.destinatario.id),
        sucursalOrigenId:Number(form.sucursalOrigenId),
        sucursalDestinoId:Number(form.sucursalDestinoId),
        descripcionContenido:form.descripcionContenido.trim(),
        pesoKg:Number(conf.pesoKg),
        largoCm:Number(conf.largoCm),
        anchoCm:Number(conf.anchoCm),
        altoCm:Number(conf.altoCm),
        valorDeclarado:conf.valorDeclarado?Number(conf.valorDeclarado):null,
        tarifaOficial:Number(conf.tarifaOficial),
        metodoPago:conf.metodoPago,
        tipoEnvio:form.tipoEnvio||null,
        cuidadoEspecial:form.cuidadoEspecial?.trim()||null,
        observaciones:conf.observaciones||null,
        viajeId:form.viajeId?Number(form.viajeId):null,
        claveIdempotencia:form.claveIdempotencia||null,
      };
      const res=await fetch("/api/v1/encomiendas",{method:"POST",headers,body:JSON.stringify(payload)});
      if(!res.ok){const err=await res.json().catch(()=>({}));throw new Error(err.mensaje||"Error al registrar la encomienda");}
      const enc=await res.json();
      await Promise.all([cargar(),cargarSolicitudes()]);
      showToast(`Encomienda registrada con la guía ${enc.numeroGuia}`);
      setModo(null);
    }catch(err){showToast(err.message);}finally{setSaving(false);}
  };

  // ── Camino B: confirmación de recepción de una SOL ─────────────────────
  const confirmarSol=async(e)=>{
    e.preventDefault();
    if(saving)return;
    if(!sol) return showToast("Selecciona la solicitud a confirmar");
    if(!(Number(conf.pesoKg)>0)) return showToast("Ingresa el peso real del paquete");
    if(!(Number(conf.largoCm)>0)||!(Number(conf.anchoCm)>0)||!(Number(conf.altoCm)>0)) return showToast("Ingresa largo, ancho y alto del paquete");
    if(!(Number(conf.tarifaOficial)>0)) return showToast("Ingresa la tarifa oficial");
    setSaving(true);
    try{
      const payload={
        pesoKg:Number(conf.pesoKg),
        largoCm:Number(conf.largoCm),
        anchoCm:Number(conf.anchoCm),
        altoCm:Number(conf.altoCm),
        valorDeclarado:conf.valorDeclarado?Number(conf.valorDeclarado):null,
        tarifaOficial:Number(conf.tarifaOficial),
        metodoPago:conf.metodoPago,
        observaciones:conf.observaciones||null,
      };
      const res=await fetch(`/api/v1/encomiendas/${sol.id}/confirmar-recepcion`,{method:"POST",headers,body:JSON.stringify(payload)});
      if(!res.ok){const err=await res.json().catch(()=>({}));throw new Error(err.mensaje||err.message||"Error al confirmar la recepción");}
      const enc=await res.json();
      await Promise.all([cargar(),cargarSolicitudes()]);
      showToast(`Recepción confirmada. Guía oficial: ${enc.numeroGuia}`);
      setModo(null);
    }catch(err){showToast(err.message);}finally{setSaving(false);}
  };

  const cambiarEstado=async(e)=>{
    e.preventDefault();if(!nuevoEst)return;
    setSaving(true);
    try{
      const res=await fetch(`/api/v1/encomiendas/${modal.id}/estado`,{method:"PATCH",headers,body:JSON.stringify({nuevoEstado:nuevoEst,observacion:obsEst||`Cambio a ${nuevoEst}`})});
      if(!res.ok){const err=await res.json().catch(()=>({}));throw new Error(err.mensaje||"Error al actualizar el estado");}
      await cargar();showToast("Estado actualizado");setModal(null);
    }catch(err){showToast(err.message);}finally{setSaving(false);}
  };

  const fil=items.filter(e=>{
    if(filtroEstado&&e.estado!==filtroEstado) return false;
    const txt=`${e.numeroGuia||""} ${e.referenciaSolicitud||""} ${e.remitente?.nombreCompleto||""} ${e.destinatarioNombre||""}`.toLowerCase();
    return txt.includes(buscar.toLowerCase());
  });

  const total=items.length;
  const enTransito=items.filter(e=>["CARGADO_EN_BUS","EN_TRANSITO","LLEGADO_A_DESTINO","EN_DESTINO"].includes(e.estado)).length;
  const entregadas=items.filter(e=>e.estado==="ENTREGADO").length;
  const pendientes=items.filter(e=>["RECIBIDO","EN_ALMACEN","LISTO_ENTREGA"].includes(e.estado)).length;

  return (<>
    <PageHeader title="Gestión de Encomiendas" sub={`${items.length} encomiendas registradas`} action={
      <div style={{display:"flex",gap:8,flexWrap:"wrap"}}>
        <button className="adm-btn-primary" onClick={abrirPresencial}><Plus size={15} style={{marginRight:6}}/>REGISTRAR ENCOMIENDA PRESENCIAL</button>
        <button className="adm-btn-secondary" onClick={()=>abrirRecuperar(null)}><Search size={15} style={{marginRight:6}}/>RECUPERAR SOLICITUD WEB</button>
      </div>
    }/>

    <div style={{display:"grid",gridTemplateColumns:"repeat(auto-fit,minmax(180px,1fr))",gap:14,marginBottom:18}}>
      <StatCard icon={<Package size={22}/>} valor={total} label="Total registradas" color={C.verde}/>
      <StatCard icon={<Clock size={22}/>} valor={solicitudes.length} label="Solicitudes web pendientes" color={C.naranja}/>
      <StatCard icon={<Navigation size={22}/>} valor={enTransito} label="En tránsito / destino" color="#1d4ed8"/>
      <StatCard icon={<CheckCircle2 size={22}/>} valor={entregadas} label="Entregadas" color="#15803d"/>
      <StatCard icon={<Clock size={22}/>} valor={pendientes} label="Pendientes de entrega" color={C.naranja}/>
    </div>

    {/* ═══ SOLICITUDES WEB PENDIENTES ═══ */}
    <section aria-label="Solicitudes web pendientes" style={{background:"rgba(245,197,24,0.05)",border:"1px solid rgba(245,197,24,0.3)",borderRadius:14,padding:16,marginBottom:22}}>
      <h3 style={{margin:"0 0 4px",fontSize:14,fontWeight:800,color:C.oscuro,letterSpacing:"0.04em"}}>SOLICITUDES WEB PENDIENTES</h3>
      <p style={{margin:"0 0 12px",fontSize:12,color:C.medio,lineHeight:1.5}}>
        Solicitudes recibidas por el portal que aún no tienen recepción, peso, medidas, tarifa, pago ni guía oficial.
        Los datos personales se muestran protegidos.
      </p>
      <div style={{display:"flex",gap:10,flexWrap:"wrap",alignItems:"flex-end",marginBottom:12}}>
        <div style={{flex:"2 1 220px"}}>
          <label style={{fontSize:11,fontWeight:700,color:C.medio,display:"block",marginBottom:4}}>Buscar</label>
          <input value={qSol} onChange={e=>setQSol(e.target.value)} onKeyDown={e=>{if(e.key==="Enter")cargarSolicitudes();}}
            placeholder="SOL-2026-000001, nombre, documento o teléfono" aria-label="Buscar solicitudes pendientes"/>
        </div>
        <div style={{flex:"1 1 150px"}}>
          <label style={{fontSize:11,fontWeight:700,color:C.medio,display:"block",marginBottom:4}}>Origen</label>
          <select className="lf-select" value={solOrigen} onChange={e=>setSolOrigen(e.target.value)} aria-label="Filtrar por origen">
            <option value="">Todos</option>
            {sucursales.map(s=><option key={s.id} value={s.id}>{s.nombre}</option>)}
          </select>
        </div>
        <div style={{flex:"1 1 150px"}}>
          <label style={{fontSize:11,fontWeight:700,color:C.medio,display:"block",marginBottom:4}}>Destino</label>
          <select className="lf-select" value={solDestino} onChange={e=>setSolDestino(e.target.value)} aria-label="Filtrar por destino">
            <option value="">Todos</option>
            {sucursales.map(s=><option key={s.id} value={s.id}>{s.nombre}</option>)}
          </select>
        </div>
        <div style={{flex:"1 1 130px"}}>
          <label style={{fontSize:11,fontWeight:700,color:C.medio,display:"block",marginBottom:4}}>Desde</label>
          <input type="date" value={solDesde} onChange={e=>setSolDesde(e.target.value)} aria-label="Fecha desde"/>
        </div>
        <div style={{flex:"1 1 130px"}}>
          <label style={{fontSize:11,fontWeight:700,color:C.medio,display:"block",marginBottom:4}}>Hasta</label>
          <input type="date" value={solHasta} onChange={e=>setSolHasta(e.target.value)} aria-label="Fecha hasta"/>
        </div>
        <button className="adm-btn-primary" onClick={cargarSolicitudes} style={{minHeight:44}}><Search size={15} style={{marginRight:6}}/>Buscar</button>
      </div>

      <TableWrap load={false} empty={!solicitudes.length&&"No hay solicitudes web pendientes con esos filtros"}>
        <table className="adm-table tbl-sol">
          <thead><tr><th>Referencia</th><th>Remitente</th><th>Destinatario</th><th>Ruta</th><th>Recibida</th><th>Acciones</th></tr></thead>
          <tbody>
            {solicitudes.map(s=>(
              <tr key={s.id} className="tr-hover">
                <td><span className="td-codigo" style={{color:C.naranja}}>{s.referenciaSolicitud||`#${s.id}`}</span></td>
                <td style={{fontSize:12}}>
                  <div style={{fontWeight:700,color:C.oscuro}}>{s.remitenteNombre||"—"}</div>
                  <div style={{fontSize:10,color:C.medio}}>{s.remitenteDocumento||"doc. protegido"} · {s.remitenteTelefono||"tel. protegido"}</div>
                </td>
                <td style={{fontSize:12}}>
                  <div>{s.destinatarioNombre||"—"}</div>
                  <div style={{fontSize:10,color:C.medio}}>{s.destinatarioTelefono||"tel. protegido"}</div>
                </td>
                <td style={{fontSize:11}}>{s.sucursalOrigen?.nombre||"—"} → {s.sucursalDestino?.nombre||"—"}</td>
                <td style={{fontSize:11,color:C.medio}}>{fmtFecha(s.createdAt)}<div style={{fontSize:10}}>{s.antiguedad||""}</div></td>
                <td>
                  <div className="td-actions">
                    <button className="btn-edit" onClick={()=>abrirRecuperar(s)} title="Confirmar recepción y registrar pago" aria-label={`Confirmar recepción de ${s.referenciaSolicitud}`}><CheckCircle2 size={13}/></button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </TableWrap>
    </section>

    {/* ═══ ENCOMIENDAS OFICIALES ═══ */}
    <div style={{display:"flex",gap:10,flexWrap:"wrap",alignItems:"center",marginBottom:8}}>
      <div style={{flex:"1 1 260px"}}><SearchBar value={buscar} onChange={setBuscar} placeholder="Buscar por guía, referencia, remitente o destinatario..."/></div>
      <label style={{display:"flex",alignItems:"center",gap:7,fontSize:12,fontWeight:700,color:C.medio}}>
        <Filter size={14}/> Estado
        <select className="lf-select" style={{minWidth:170}} value={filtroEstado} onChange={e=>setFiltroEstado(e.target.value)} aria-label="Filtrar por estado">
          <option value="">Todos</option>
          {ENC_ESTADOS.map(s=><option key={s} value={s}>{s.replace(/_/g," ")}</option>)}
        </select>
      </label>
    </div>

    <TableWrap load={load} empty={!fil.length&&"Sin encomiendas para los filtros seleccionados"}>
      <table className="adm-table tbl-encomiendas">
        <thead><tr><th>Guía</th><th>Remitente</th><th>Destinatario</th><th>Peso</th><th>Tarifa</th><th>Estado</th><th>Registro</th><th>Acciones</th></tr></thead>
        <tbody>
          {fil.map(enc=>(
            <tr key={enc.id} className="tr-hover">
              <td>
                {enc.numeroGuia
                  ?<span className="td-codigo">{enc.numeroGuia}</span>
                  :<span className="td-codigo" style={{color:C.naranja}}>{enc.referenciaSolicitud||`#${enc.id}`}</span>}
              </td>
              <td style={{fontSize:12}}>{enc.remitente?encNombre(enc.remitente):"—"}</td>
              <td style={{fontSize:12}}>{enc.destinatarioNombre||(enc.destinatario?encNombre(enc.destinatario):"—")}</td>
              <td>{enc.pesoKg?`${enc.pesoKg} kg`:"—"}</td>
              <td><strong style={{color:enc.estado==="SOLICITUD_PENDIENTE"?C.naranja:C.verde}}>{enc.estado==="SOLICITUD_PENDIENTE"?"Sin tarifa":(enc.costo?`S/ ${enc.costo}`:"—")}</strong></td>
              <td><Badge text={enc.estado}/></td>
              <td style={{fontSize:11,color:C.medio}}>{fmtFecha(enc.createdAt)}</td>
              <td>
                <div className="td-actions">
                  <button className="btn-edit" onClick={()=>setDetalle(enc)} title="Ver detalle" aria-label={`Ver detalle ${enc.numeroGuia||enc.referenciaSolicitud}`}><Info size={13}/></button>
                  {enc.estado==="SOLICITUD_PENDIENTE"
                    ?<button className="btn-edit" onClick={()=>abrirRecuperar({id:enc.id,referenciaSolicitud:enc.referenciaSolicitud,sucursalOrigen:enc.sucursalOrigen,sucursalDestino:enc.sucursalDestino,descripcionContenido:enc.descripcionContenido,remitenteNombre:enc.remitente?encNombre(enc.remitente):null,destinatarioNombre:enc.destinatarioNombre,createdAt:enc.createdAt})} title="Confirmar recepción" aria-label={`Confirmar recepción ${enc.referenciaSolicitud}`}><CheckCircle2 size={13}/></button>
                    :<button className="btn-edit" onClick={()=>abrirEstado(enc)} title="Cambiar estado" aria-label={`Cambiar estado ${enc.numeroGuia}`}><ArrowRight size={13}/></button>}
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </TableWrap>

    {/* ═══ MODAL: REGISTRO PRESENCIAL / RECUPERAR SOLICITUD ═══ */}
    {modo&&(
      <div className="modal-overlay">
        <div className="modal" style={{maxWidth:720,width:"95%"}} role="dialog" aria-modal="true" aria-label="Registrar encomienda">
          <div className="modal-header">
            <div className="modal-badge"><Package size={20}/></div>
            <div>
              <div className="modal-title">{modo==="presencial"?"Registrar encomienda presencial":"Confirmar recepción de solicitud"}</div>
              <div className="modal-subtitle">{modo==="presencial"?"El remitente está en ventanilla":"La solicitud web pasa a encomienda oficial al confirmar"}</div>
            </div>
            <button className="modal-close" onClick={()=>setModo(null)} aria-label="Cerrar"><X size={20}/></button>
          </div>
          <div className="modal-body">
            <div style={{display:"flex",gap:8,marginBottom:16}} role="tablist" aria-label="Origen del registro">
              <button type="button" role="tab" aria-selected={modo==="presencial"} onClick={()=>setModo("presencial")}
                style={{flex:1,minHeight:44,borderRadius:9,fontWeight:700,fontSize:12,cursor:"pointer",fontFamily:"inherit",
                  background:modo==="presencial"?C.verde:"rgba(18,115,105,0.08)",
                  color:modo==="presencial"?"#fff":C.medio,border:`1px solid ${modo==="presencial"?C.verde:"rgba(18,115,105,0.25)"}`}}>
                REGISTRO PRESENCIAL
              </button>
              <button type="button" role="tab" aria-selected={modo==="recuperar"} onClick={()=>{if(!sol)abrirRecuperar(null);}}
                style={{flex:1,minHeight:44,borderRadius:9,fontWeight:700,fontSize:12,cursor:"pointer",fontFamily:"inherit",
                  background:modo==="recuperar"?C.verde:"rgba(18,115,105,0.08)",
                  color:modo==="recuperar"?"#fff":C.medio,border:`1px solid ${modo==="recuperar"?C.verde:"rgba(18,115,105,0.25)"}`}}>
                RECUPERAR SOLICITUD WEB
              </button>
            </div>

            {modo==="presencial"&&(
              <form onSubmit={guardar}>
                <div className="form-grid">
                  <SelectorCliente headers={headers} label="Remitente *" value={form.remitente} onChange={c=>setForm(f=>({...f,remitente:c}))} showToast={showToast}/>
                  <SelectorCliente headers={headers} label="Destinatario *" value={form.destinatario} onChange={c=>setForm(f=>({...f,destinatario:c}))} showToast={showToast}/>
                  <Field label="Sucursal origen *">
                    <select className="lf-select" value={form.sucursalOrigenId} onChange={e=>setForm(f=>({...f,sucursalOrigenId:e.target.value}))} required>
                      <option value="">Seleccionar sucursal</option>
                      {sucursales.map(s=><option key={s.id} value={s.id}>{s.nombre} · {s.ciudad}</option>)}
                    </select>
                  </Field>
                  <Field label="Sucursal destino *">
                    <select className="lf-select" value={form.sucursalDestinoId} onChange={e=>setForm(f=>({...f,sucursalDestinoId:e.target.value}))} required>
                      <option value="">Seleccionar sucursal</option>
                      {sucursales.filter(s=>String(s.id)!==String(form.sucursalOrigenId)).map(s=><option key={s.id} value={s.id}>{s.nombre} · {s.ciudad}</option>)}
                    </select>
                  </Field>
                  <Field label="Descripción del contenido *" style={{gridColumn:"1/-1"}}>
                    <input value={form.descripcionContenido} onChange={e=>setForm(f=>({...f,descripcionContenido:e.target.value}))} maxLength={300} required placeholder="Ej: Ropa, documentos, artefacto..."/>
                  </Field>
                  <Field label="Tipo de envío">
                    <select className="lf-select" value={form.tipoEnvio} onChange={e=>setForm(f=>({...f,tipoEnvio:e.target.value}))}>
                      <option value="NORMAL">Normal</option>
                      <option value="EXPRESS">Express</option>
                      <option value="FRAGIL">Frágil</option>
                    </select>
                  </Field>
                  <Field label="Cuidado especial">
                    <input value={form.cuidadoEspecial} onChange={e=>setForm(f=>({...f,cuidadoEspecial:e.target.value}))} maxLength={120} placeholder="Ej: Frágil, no doblar"/>
                  </Field>
                </div>
                <h4 style={{margin:"18px 0 10px",fontSize:12,color:C.oscuro,textTransform:"uppercase",letterSpacing:"0.06em"}}>Datos oficiales y pago en agencia</h4>
                <BloqueDatosOficiales conf={conf} setConf={setConf}/>
                <p style={{fontSize:11,color:C.medio,marginTop:10,lineHeight:1.5}}>
                  No existe tarifario automático: el importe indicado es el que cobra la agencia y queda auditado con tu usuario y la fecha.
                </p>
                <div style={{display:"flex",gap:10,marginTop:16}}>
                  <button type="button" className="btn-cancelar" onClick={()=>setModo(null)} style={{flex:1}}>Cancelar</button>
                  <button type="submit" className="btn-reservar" style={{flex:2}} disabled={saving}>
                    {saving?<span className="spinner"/>:<span>REGISTRAR ENCOMIENDA Y COBRAR</span>}
                  </button>
                </div>
              </form>
            )}

            {modo==="recuperar"&&(
              <form onSubmit={confirmarSol}>
                {!sol?(
                  <div style={{background:"rgba(18,115,105,0.06)",border:"1px solid rgba(18,115,105,0.2)",borderRadius:10,padding:14,marginBottom:14}}>
                    <p style={{fontSize:12,color:C.medio,marginBottom:10,lineHeight:1.5}}>
                      Ingresa la referencia SOL que te entregó el cliente, o selecciónala desde el panel de solicitudes pendientes.
                    </p>
                    <div style={{display:"flex",gap:8}}>
                      <input value={refSol} onChange={e=>setRefSol(e.target.value.toUpperCase())} placeholder="SOL-2026-000001" aria-label="Referencia de solicitud" style={{flex:1}}/>
                      <button type="button" className="btn-cancelar" onClick={buscarSol} disabled={saving} style={{minHeight:44,whiteSpace:"nowrap"}}>
                        {saving?"Buscando...":"Buscar"}
                      </button>
                    </div>
                  </div>
                ):(
                  <>
                    <div style={{background:"rgba(245,197,24,0.08)",border:"1px solid rgba(245,197,24,0.3)",borderRadius:10,padding:14,marginBottom:14}}>
                      <div style={{display:"flex",justifyContent:"space-between",alignItems:"center",gap:10,flexWrap:"wrap",marginBottom:10}}>
                        <div>
                          <div style={{fontSize:11,color:C.medio,letterSpacing:"0.06em"}}>SOLICITUD</div>
                          <div style={{fontSize:19,fontWeight:800,color:C.naranja}}>{sol.referenciaSolicitud}</div>
                        </div>
                        <Badge text="SOLICITUD_PENDIENTE"/>
                      </div>
                      <div className="track-info-grid">
                        {[
                          ["Remitente", `${sol.remitenteNombre||"—"} (${sol.remitenteDocumento||"doc. protegido"})`],
                          ["Teléfono remitente", sol.remitenteTelefono||"protegido"],
                          ["Destinatario", sol.destinatarioNombre||"—"],
                          ["Teléfono destinatario", sol.destinatarioTelefono||"protegido"],
                          ["Origen", sol.sucursalOrigen?`${sol.sucursalOrigen.nombre} · ${sol.sucursalOrigen.ciudad}`:"—"],
                          ["Destino", sol.sucursalDestino?`${sol.sucursalDestino.nombre} · ${sol.sucursalDestino.ciudad}`:"—"],
                          ["Contenido", sol.descripcionContenido||"—"],
                          ["Solicitada", `${fmtFecha(sol.createdAt)}${sol.antiguedad?` · ${sol.antiguedad}`:""}`],
                        ].map(([lbl,val])=>(
                          <div className="track-info-item" key={lbl}>
                            <span className="track-info-label">{lbl}</span>
                            <span className="track-info-value">{val}</span>
                          </div>
                        ))}
                      </div>
                    </div>
                    <h4 style={{margin:"0 0 10px",fontSize:12,color:C.oscuro,textTransform:"uppercase",letterSpacing:"0.06em"}}>Datos oficiales y pago en agencia</h4>
                    <BloqueDatosOficiales conf={conf} setConf={setConf}/>
                    <p style={{fontSize:11,color:C.medio,marginTop:10,lineHeight:1.5}}>
                      Al confirmar se emite la guía oficial, se registra un único pago y queda auditado el usuario y la fecha.
                    </p>
                    <div style={{display:"flex",gap:10,marginTop:16}}>
                      <button type="button" className="btn-cancelar" onClick={()=>{setSol(null);setRefSol("");}} style={{flex:1}}>Cambiar solicitud</button>
                      <button type="submit" className="btn-reservar" style={{flex:2}} disabled={saving}>
                        {saving?<span className="spinner"/>:<span>CONFIRMAR RECEPCIÓN Y REGISTRAR PAGO</span>}
                      </button>
                    </div>
                  </>
                )}
              </form>
            )}
          </div>
        </div>
      </div>
    )}

    {modal&&modal!=="nuevo"&&(
      <div className="modal-overlay">
        <div className="modal" style={{maxWidth:440,width:"95%"}} role="dialog" aria-modal="true" aria-label="Cambiar estado de encomienda">
          <div className="modal-header">
            <div className="modal-badge"><Package size={20}/></div>
            <div><div className="modal-title">Cambiar estado</div></div>
            <button className="modal-close" onClick={()=>setModal(null)} aria-label="Cerrar"><X size={20}/></button>
          </div>
          <div className="modal-body">
            <p style={{fontSize:12,color:C.medio,marginBottom:12}}>Guía: <strong>{modal.numeroGuia||`#${modal.id}`}</strong> · Estado actual: <Badge text={modal.estado}/></p>
            {(ENC_TRANSICIONES[modal.estado]||[]).length===0?(
              <div style={{background:"rgba(18,115,105,0.06)",border:"1px solid rgba(18,115,105,0.15)",borderRadius:10,padding:14,fontSize:13,color:C.gris}}>
                Esta encomienda está en un estado final y no admite más transiciones.
              </div>
            ):(
              <form onSubmit={cambiarEstado}>
                <Field label="Nuevo estado">
                  <select className="lf-select" value={nuevoEst} onChange={e=>setNuevoEst(e.target.value)} required aria-label="Nuevo estado">
                    <option value="">Seleccionar estado</option>
                    {ENC_TRANSICIONES[modal.estado].map(s=><option key={s} value={s}>{s.replace(/_/g," ")}</option>)}
                  </select>
                </Field>
                <Field label="Observación" style={{marginTop:12}}>
                  <input value={obsEst} onChange={e=>setObsEst(e.target.value)} maxLength={300} placeholder="Opcional"/>
                </Field>
                <div style={{display:"flex",gap:10,marginTop:16}}>
                  <button type="button" className="btn-cancelar" onClick={()=>setModal(null)} style={{flex:1}}>Cancelar</button>
                  <button type="submit" className="btn-reservar" style={{flex:2}} disabled={saving||!nuevoEst}>
                    {saving?<span className="spinner"/>:<span>Actualizar estado</span>}
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>
      </div>
    )}

    {detalle&&(
      <div className="modal-overlay">
        <div className="modal" style={{maxWidth:640,width:"95%"}} role="dialog" aria-modal="true" aria-label="Detalle de encomienda">
          <div className="modal-header">
            <div className="modal-badge"><Package size={20}/></div>
            <div><div className="modal-title">Detalle de encomienda</div><div className="modal-subtitle">{detalle.numeroGuia||`#${detalle.id}`}</div></div>
            <button className="modal-close" onClick={()=>setDetalle(null)} aria-label="Cerrar"><X size={20}/></button>
          </div>
          <div className="modal-body">
            <div style={{marginBottom:14}}><Badge text={detalle.estado}/></div>
            <div className="track-info-grid">
              {[
                ["Remitente", detalle.remitente?encNombre(detalle.remitente):"—"],
                ["Destinatario", detalle.destinatarioNombre||"—"],
                ["Origen", detalle.sucursalOrigen?`${detalle.sucursalOrigen.nombre} · ${detalle.sucursalOrigen.ciudad}`:"—"],
                ["Destino", detalle.sucursalDestino?`${detalle.sucursalDestino.nombre} · ${detalle.sucursalDestino.ciudad}`:"—"],
                ["Contenido", detalle.descripcionContenido||"—"],
                ["Peso", detalle.pesoKg?`${detalle.pesoKg} kg`:"—"],
                ["Costo", detalle.estado==="SOLICITUD_PENDIENTE"?"Pendiente de recepción":(detalle.costo?`S/ ${detalle.costo}`:"—")],
                ["Método de pago", detalle.estado==="SOLICITUD_PENDIENTE"?"Se define en agencia":((detalle.metodoPago||"—").replace(/_/g," "))],
                ["Volumen", detalle.volumenM3?`${detalle.volumenM3} m3`:"—"],
                ["Registrado", fmtFecha(detalle.createdAt)],
              ].map(([lbl,val])=>(
                <div className="track-info-item" key={lbl}>
                  <span className="track-info-label">{lbl}</span>
                  <span className="track-info-value">{val}</span>
                </div>
              ))}
            </div>
            <h4 style={{margin:"18px 0 10px",color:C.oscuro,fontSize:13,textTransform:"uppercase",letterSpacing:"0.06em"}}>Historial de movimientos</h4>
            {(detalle.movimientos||[]).length===0?(
              <p style={{fontSize:13,color:C.medio}}>Sin movimientos registrados.</p>
            ):(
              <div style={{display:"flex",flexDirection:"column",gap:8}}>
                {detalle.movimientos.map((m,i)=>(
                  <div key={m.id||i} style={{display:"flex",gap:10,alignItems:"flex-start",background:"rgba(18,115,105,0.04)",border:"1px solid rgba(18,115,105,0.1)",borderRadius:8,padding:"9px 12px"}}>
                    <CheckCircle2 size={15} color={C.verde} style={{marginTop:2,flexShrink:0}}/>
                    <div style={{flex:1}}>
                      <div style={{fontSize:13,fontWeight:700,color:C.oscuro}}>{(m.estadoNuevo||"").replace(/_/g," ")}</div>
                      <div style={{fontSize:11,color:C.medio}}>{fmtFecha(m.fechaHora)} · {m.sucursalActual||"En tránsito"}</div>
                      {m.observacion&&<div style={{fontSize:12,color:C.gris,marginTop:2}}>{m.observacion}</div>}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    )}
  </>);
}


// ═══════════════════════════════════════════════════════
// CRUD: Personal (Usuarios del sistema)
// ═══════════════════════════════════════════════════════
function CrudPersonal({ headers, showToast }) {
  const [items,setItems]=useState([]);const [roles,setRoles]=useState([]);const [sucursales,setSucursales]=useState([]);
  const [load,setLoad]=useState(true);const [buscar,setBuscar]=useState("");const [filtroRol,setFiltroRol]=useState("");
  const [modal,setModal]=useState(null);const [form,setForm]=useState({});const [conf,setConf]=useState(null);const [saving,setSaving]=useState(false);

  const cargar=useCallback(async()=>{setLoad(true);try{const[r1,r2,r3]=await Promise.all([fetch("/api/v1/usuarios",{headers}),fetch("/api/v1/roles",{headers}),fetch("/api/v1/sucursales",{headers})]);setItems(r1.ok?await r1.json():[]);setRoles(r2.ok?await r2.json():[]);setSucursales(r3.ok?await r3.json():[]);}catch{showToast("Error al cargar personal");}finally{setLoad(false);};},[headers,showToast]);
  useEffect(()=>{cargar();},[cargar]);

  const abrir=(u=null)=>{setForm(u?{id:u.id,username:u.username||"",nombres:u.nombres||"",apellidos:u.apellidos||"",email:u.email||"",telefono:u.telefono||"",dniRuc:u.dniRuc||"",rolId:u.rolId?.toString()||"",sucursalId:u.sucursalId?.toString()||"",password:""}:{username:"",nombres:"",apellidos:"",email:"",telefono:"",dniRuc:"",rolId:"",sucursalId:"",password:""});setModal(u||"nuevo");};

  const guardar=async(e)=>{e.preventDefault();if(!form.username||!form.nombres||!form.apellidos)return showToast("Usuario, nombres y apellidos son obligatorios");if(!form.id&&!form.password)return showToast("La contraseña es obligatoria para nuevos usuarios");setSaving(true);
    try{const isEdit=!!form.id;const payload={username:form.username,nombres:form.nombres,apellidos:form.apellidos,email:form.email||null,telefono:form.telefono||null,dniRuc:form.dniRuc||null,rolId:form.rolId?parseInt(form.rolId):undefined,sucursalId:form.sucursalId?parseInt(form.sucursalId):null};
    if(!isEdit&&form.password)payload.password=form.password;
    const res=await fetch(isEdit?`/api/v1/usuarios/${form.id}`:"/api/v1/usuarios",{method:isEdit?"PUT":"POST",headers,body:JSON.stringify(payload)});
    if(!res.ok){const err=await res.json().catch(()=>({}));throw new Error(err.mensaje||err.message||"Error");}
    const data=await res.json();setItems(prev=>isEdit?prev.map(x=>x.id===data.id?data:x):[...prev,data]);showToast(isEdit?"Personal actualizado":"Personal creado");setModal(null);}
    catch(err){showToast(err.message);}finally{setSaving(false);}
  };

  const eliminar=async(id)=>{try{await fetch(`/api/v1/usuarios/${id}`,{method:"DELETE",headers});setItems(prev=>prev.filter(x=>x.id!==id));showToast("Usuario eliminado");}catch{showToast("Error");}finally{setConf(null);};};

  // Filtros combinados
  const fil=items.filter(u=>{
    const matchText=`${u.username} ${u.nombres} ${u.apellidos} ${u.email||""}`.toLowerCase().includes(buscar.toLowerCase());
    const matchRol=!filtroRol||u.rol===filtroRol||(u.rolNombre&&u.rolNombre===filtroRol);
    return matchText&&matchRol;
  });

  // Agrupar por rol para mostrar stats
  const rolStats={ROLE_ADMIN:0,ROLE_CAJERO:0,ROLE_VENDEDOR:0,ROLE_OPERADOR_ENCOMIENDAS:0,ROLE_CHOFER:0,ROLE_CLIENTE:0};
  items.forEach(u=>{if(rolStats[u.rol]!==undefined)rolStats[u.rol]++;});

  const ROLES_LABEL={ROLE_ADMIN:"Admin",ROLE_CAJERO:"Cajero",ROLE_VENDEDOR:"Vendedor",ROLE_OPERADOR_ENCOMIENDAS:"Operador enc.",ROLE_CHOFER:"Chofer",ROLE_CLIENTE:"Cliente"};

  return (<>
    <PageHeader title="Gestión de Personal" sub={`${items.length} usuarios registrados`} action={<button className="adm-btn-primary" onClick={()=>abrir()}><Plus size={15} style={{marginRight:6}}/>Nuevo usuario</button>}/>

    {/* Stats por rol */}
    <div style={{display:"flex",gap:10,marginBottom:14,flexWrap:"wrap"}}>
      {Object.entries(rolStats).map(([rol,cnt])=>(
        <button key={rol} onClick={()=>setFiltroRol(filtroRol===rol?"":rol)}
          style={{background:filtroRol===rol?C.verde+"22":C.blanco,border:`1.5px solid ${filtroRol===rol?C.verde:"#e2edeb"}`,borderRadius:10,padding:"8px 14px",cursor:"pointer",display:"flex",alignItems:"center",gap:8,fontFamily:"inherit",transition:"all 0.18s"}}>
          <span style={{fontSize:18,fontWeight:900,color:C.verde}}>{cnt}</span>
          <span style={{fontSize:12,color:C.gris}}>{ROLES_LABEL[rol]||rol}</span>
        </button>
      ))}
      <button onClick={()=>setFiltroRol("")} style={{marginLeft:"auto",background:"transparent",border:"none",color:C.medio,cursor:"pointer",fontSize:12,fontFamily:"inherit",display:"flex",alignItems:"center",gap:4}}>
        <X size={13}/>Limpiar filtros
      </button>
    </div>

    {/* Búsqueda + filtro rol */}
    <div style={{display:"flex",gap:10,marginBottom:14,flexWrap:"wrap"}}>
      <div style={{flex:1,minWidth:200,position:"relative"}}>
        <Search size={14} color={C.medio} style={{position:"absolute",left:10,top:"50%",transform:"translateY(-50%)",pointerEvents:"none"}}/>
        <input className="adm-search" value={buscar} onChange={e=>setBuscar(e.target.value)} placeholder="Buscar por nombre, usuario o email..." style={{paddingLeft:32,width:"100%",boxSizing:"border-box"}}/>
      </div>
      <div style={{display:"flex",alignItems:"center",gap:6}}>
        <Filter size={14} color={C.medio}/>
        <select className="lf-select" value={filtroRol} onChange={e=>setFiltroRol(e.target.value)} style={{maxWidth:180,padding:"8px 12px"}}>
          <option value="">Todos los roles</option>
          {Object.entries(ROLES_LABEL).map(([v,l])=><option key={v} value={v}>{l}</option>)}
        </select>
      </div>
    </div>

    <TableWrap load={load} empty={!fil.length&&"Sin usuarios registrados"}>
      <table className="adm-table tbl-personal">
        <thead><tr><th>Usuario</th><th>Nombre completo</th><th>Rol</th><th>Sucursal</th><th>Teléfono</th><th>Email</th><th>Acciones</th></tr></thead>
        <tbody>
          {fil.map(u=>(
            <tr key={u.id} className="tr-hover">
              <td>
                <div style={{display:"flex",alignItems:"center",gap:8}}>
                  <div style={{width:30,height:30,borderRadius:"50%",background:`linear-gradient(135deg,${C.verde},${C.oscuro})`,display:"flex",alignItems:"center",justifyContent:"center",flexShrink:0}}>
                    <span style={{color:"white",fontSize:11,fontWeight:800}}>{(u.username||"U").slice(0,2).toUpperCase()}</span>
                  </div>
                  <span className="td-codigo">{u.username}</span>
                </div>
              </td>
              <td><strong>{[u.nombres,u.apellidos].filter(Boolean).join(" ")||"—"}</strong></td>
              <td><Badge text={u.rol||u.rolNombre}/></td>
              <td style={{fontSize:12}}>{u.sucursalNombre||"—"}</td>
              <td style={{fontSize:12}}>{u.telefono||"—"}</td>
              <td style={{fontSize:12}}>{u.email||"—"}</td>
              <td><Actions onEdit={()=>abrir(u)} onDel={()=>setConf(u.id)}/></td>
            </tr>
          ))}
        </tbody>
      </table>
    </TableWrap>

    {modal&&(<ModalForm titulo={modal==="nuevo"?"Nuevo usuario":"Editar usuario"} icon={<UserCircle size={20}/>} onClose={()=>setModal(null)} onSubmit={guardar} saving={saving}>
      <div className="form-grid">
        <Field label="Username *"><input value={form.username||""} onChange={e=>setForm(f=>({...f,username:e.target.value}))} required disabled={!!form.id} placeholder="nombre_usuario"/></Field>
        {!form.id&&<Field label="Contraseña *"><input type="password" value={form.password||""} onChange={e=>setForm(f=>({...f,password:e.target.value}))} required placeholder="Mín. 6 caracteres" minLength={6}/></Field>}
        <Field label="Nombres *"><input value={form.nombres||""} onChange={e=>setForm(f=>({...f,nombres:e.target.value}))} required/></Field>
        <Field label="Apellidos *"><input value={form.apellidos||""} onChange={e=>setForm(f=>({...f,apellidos:e.target.value}))} required/></Field>
        <Field label="Rol *">
          <select className="lf-select" value={form.rolId||""} onChange={e=>setForm(f=>({...f,rolId:e.target.value}))} required>
            <option value="">Seleccionar rol...</option>
            {roles.map(r=><option key={r.id} value={r.id}>{r.nombre?.replace("ROLE_","")} — {r.descripcion||""}</option>)}
          </select>
        </Field>
        <Field label="Sucursal">
          <select className="lf-select" value={form.sucursalId||""} onChange={e=>setForm(f=>({...f,sucursalId:e.target.value}))}>
            <option value="">Sin sucursal</option>
            {sucursales.map(s=><option key={s.id} value={s.id}>{s.nombre} — {s.ciudad}</option>)}
          </select>
        </Field>
        <Field label="DNI / RUC"><input value={form.dniRuc||""} onChange={e=>setForm(f=>({...f,dniRuc:e.target.value}))}/></Field>
        <Field label="Teléfono"><input value={form.telefono||""} onChange={e=>setForm(f=>({...f,telefono:e.target.value}))}/></Field>
        <Field label="Email" style={{gridColumn:"1/-1"}}><input type="email" value={form.email||""} onChange={e=>setForm(f=>({...f,email:e.target.value}))}/></Field>
      </div>
    </ModalForm>)}
    {conf&&<ConfirmModal onConfirmar={()=>eliminar(conf)} onCancelar={()=>setConf(null)}/>}
  </>);
}

// ═══════════════════════════════════════════════════════
// CRUD: Pagos
// ═══════════════════════════════════════════════════════
function CrudPagos({ headers, showToast, puedeEliminar=true }) {
  const [items, setItems] = useState([]);
  const [load, setLoad] = useState(true);
  const [buscar, setBuscar] = useState("");
  const [modal, setModal] = useState(null);
  const [form, setForm] = useState({});
  const [conf, setConf] = useState(null);
  const [saving, setSaving] = useState(false);
  const [boleta, setBoleta] = useState(null); // Para el modal de impresión

  const cargar = useCallback(async () => {
    setLoad(true);
    try {
      const r = await fetch("/api/v1/pagos", { headers });
      const d = r.ok ? await r.json() : [];
      setItems(Array.isArray(d) ? d : []);
    } catch {
      showToast("Error al cargar pagos");
    } finally {
      setLoad(false);
    }
  }, [headers, showToast]);

  useEffect(() => { cargar(); }, [cargar]);

  const abrir = (p = null) => {
    setForm(p ? {
      id: p.id,
      monto: p.monto?.toString() || "",
      metodo: p.metodo || "EFECTIVO",
      referencia: p.referencia || "",
      observacion: p.observacion || "",
      estado: p.estado || "COMPLETADO"
    } : { monto: "", metodo: "EFECTIVO", referencia: "", observacion: "", estado: "COMPLETADO" });
    setModal(p || "nuevo");
  };

  const guardar = async (e) => {
    e.preventDefault();
    if (!form.monto || parseFloat(form.monto) <= 0) return showToast("Monto obligatorio y mayor a 0");
    setSaving(true);
    try {
      const isEdit = !!form.id;
      const res = await fetch(isEdit ? `/api/v1/pagos/${form.id}` : "/api/v1/pagos", {
        method: isEdit ? "PUT" : "POST",
        headers,
        body: JSON.stringify({
          monto: parseFloat(form.monto),
          metodo: form.metodo,
          referencia: form.referencia,
          observacion: form.observacion,
          estado: form.estado
        })
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.mensaje || "Error al guardar pago");
      }
      const data = await res.json();
      setItems(prev => isEdit ? prev.map(x => x.id === data.id ? data : x) : [data, ...prev]);
      showToast(isEdit ? "Pago actualizado" : "Pago registrado");
      setModal(null);
    } catch (err) {
      showToast(err.message);
    } finally {
      setSaving(false);
    }
  };

  const eliminar = async (id) => {
    try {
      const res = await fetch(`/api/v1/pagos/${id}`, { method: "DELETE", headers });
      if (!res.ok) throw new Error("No se pudo eliminar");
      setItems(prev => prev.filter(x => x.id !== id));
      showToast("Pago eliminado");
    } catch (err) {
      showToast(err.message);
    } finally {
      setConf(null);
    }
  };

  const imprimir = () => {
    window.print();
  };

  const fil = items.filter(p =>
    `${p.referencia || ""} ${p.metodo || ""} ${p.cliente || ""} ${p.referenciaEntidad || ""}`.toLowerCase().includes(buscar.toLowerCase())
  );

  return (
    <>
      <PageHeader
        title="Gestión de Pagos"
        sub={`${items.length} pagos registrados`}
        action={<button className="adm-btn-primary" onClick={() => abrir()}><Plus size={15} style={{ marginRight: 6 }} />Registrar Pago</button>}
      />
      <SearchBar value={buscar} onChange={setBuscar} placeholder="Buscar por cliente, referencia o entidad..." />
      <TableWrap load={load} empty={!fil.length && "Sin pagos registrados"}>
        <table className="adm-table tbl-pagos">
          <thead>
            <tr>
              <th>ID</th>
              <th>Fecha</th>
              <th>Cliente / Concepto</th>
              <th>Método</th>
              <th>Monto</th>
              <th>Referencia</th>
              <th>Estado</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            {fil.map(p => (
              <tr key={p.id} className="tr-hover">
                <td><span className="td-codigo">#{p.id}</span></td>
                <td>{fmtFecha(p.fechaPago)}</td>
                <td>
                  <div style={{ display: "flex", flexDirection: "column" }}>
                    <strong>{p.cliente || "Cliente Varios"}</strong>
                    <span style={{ fontSize: 10, color: C.medio }}>{p.referenciaEntidad || "Pago Manual"}</span>
                  </div>
                </td>
                <td><Badge text={p.metodo} /></td>
                <td><strong style={{ color: C.verde }}>S/ {p.monto?.toFixed(2)}</strong></td>
                <td><span style={{ fontSize: 12, color: C.gris }}>{p.referencia || "—"}</span></td>
                <td><Badge text={p.estado} /></td>
                <td>
                  <div className="td-actions">
                    <button className="btn-edit" onClick={() => abrir(p)} title="Editar" aria-label={`Editar pago #${p.id}`}><Pencil size={13}/></button>
                    <button className="btn-edit" onClick={() => setBoleta(p)} title="Ver Boleta" aria-label={`Ver comprobante del pago #${p.id}`} style={{ background: C.amarillo + "22", color: "#9a7a00" }}><Ticket size={13}/></button>
                    {puedeEliminar && <button className="btn-delete" onClick={() => setConf(p.id)} title="Eliminar" aria-label={`Eliminar pago #${p.id}`}><Trash2 size={13}/></button>}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </TableWrap>

      {modal && (
        <ModalForm
          titulo={modal === "nuevo" ? "Registrar Pago" : "Editar Pago"}
          icon={<CreditCard size={20} />}
          onClose={() => setModal(null)}
          onSubmit={guardar}
          saving={saving}
        >
          <div className="form-grid">
            <Field label="Monto (S/) *">
              <input type="number" step="0.01" value={form.monto || ""} onChange={e => setForm(f => ({ ...f, monto: e.target.value }))} required placeholder="0.00" />
            </Field>
            <Field label="Método *">
              <select className="lf-select" value={form.metodo || "EFECTIVO"} onChange={e => setForm(f => ({ ...f, metodo: e.target.value }))} required>
                <option value="EFECTIVO">Efectivo</option>
                <option value="YAPE">Yape</option>
                <option value="PLIN">Plin</option>
                <option value="TARJETA_CREDITO">Tarjeta de Crédito</option>
                <option value="TARJETA_DEBITO">Tarjeta de Débito</option>
                <option value="TRANSFERENCIA">Transferencia</option>
              </select>
            </Field>
            <Field label="Referencia" style={{ gridColumn: "1/-1" }}>
              <input value={form.referencia || ""} onChange={e => setForm(f => ({ ...f, referencia: e.target.value }))} placeholder="Número de operación, etc." />
            </Field>
            <Field label="Observación" style={{ gridColumn: "1/-1" }}>
              <input value={form.observacion || ""} onChange={e => setForm(f => ({ ...f, observacion: e.target.value }))} placeholder="Notas adicionales..." />
            </Field>
            <Field label="Estado" style={{ gridColumn: "1/-1" }}>
              <select className="lf-select" value={form.estado || "COMPLETADO"} onChange={e => setForm(f => ({ ...f, estado: e.target.value }))} required>
                <option value="COMPLETADO">Completado</option>
                <option value="PENDIENTE">Pendiente</option>
                <option value="RECHAZADO">Rechazado</option>
                <option value="REEMBOLSADO">Reembolsado</option>
              </select>
            </Field>
          </div>
        </ModalForm>
      )}

      {boleta && (
        <div className="modal-overlay no-print-bg" onClick={() => setBoleta(null)}>
          <div className="modal boleta-modal" onClick={e => e.stopPropagation()} style={{ maxWidth: 450, padding: 0 }}>
            <div className="no-print" style={{ padding: "10px 20px", borderBottom: "1px solid #eee", display: "flex", justifyContent: "space-between", alignItems: "center" }}>
              <span style={{ fontWeight: 700, color: C.oscuro }}>Vista Previa de Boleta</span>
              <div style={{ display: "flex", gap: 8 }}>
                <button onClick={imprimir} className="adm-btn-primary" style={{ padding: "6px 12px", fontSize: 12 }}><Zap size={14} style={{ marginRight: 5 }} />Imprimir / PDF</button>
                <button onClick={() => setBoleta(null)} style={{ background: "none", border: "none", cursor: "pointer", color: C.medio }}><X size={20} /></button>
              </div>
            </div>
            
            {/* BOLETA IMPRIMIBLE */}
            <div className="printable-receipt" style={{ padding: "40px", background: "#fff", color: "#000", fontFamily: "'Courier New', Courier, monospace" }}>
              <div style={{ textAlign: "center", marginBottom: 25 }}>
                <div style={{ fontSize: 22, fontWeight: "bold", letterSpacing: 2 }}>INTIWATANA S.R.L.</div>
                <div style={{ fontSize: 12 }}>R.U.C. 20450678901</div>
                <div style={{ fontSize: 11 }}>Av. El Sol 123, Cusco - Perú</div>
                <div style={{ fontSize: 11 }}>Telf: (084) 234567</div>
              </div>

              <div style={{ borderTop: "1px dashed #000", borderBottom: "1px dashed #000", padding: "10px 0", marginBottom: 20, textAlign: "center" }}>
                <div style={{ fontWeight: "bold" }}>BOLETA DE VENTA ELECTRÓNICA</div>
                <div style={{ fontSize: 14 }}>B001 - {boleta.id.toString().padStart(8, "0")}</div>
              </div>

              <div style={{ fontSize: 13, display: "flex", flexDirection: "column", gap: 6, marginBottom: 20 }}>
                <div style={{ display: "flex", justifyContent: "space-between" }}><span>FECHA EMISIÓN:</span> <span>{new Date(boleta.fechaPago).toLocaleDateString()}</span></div>
                <div style={{ display: "flex", justifyContent: "space-between" }}><span>HORA EMISIÓN:</span> <span>{new Date(boleta.fechaPago).toLocaleTimeString()}</span></div>
                <div style={{ display: "flex", justifyContent: "space-between" }}><span>CLIENTE:</span> <span>{boleta.cliente || "Venta Directa"}</span></div>
                <div style={{ display: "flex", justifyContent: "space-between" }}><span>MÉTODO PAGO:</span> <span>{boleta.metodo}</span></div>
                {boleta.referencia && <div style={{ display: "flex", justifyContent: "space-between" }}><span>REF/OPER:</span> <span>{boleta.referencia}</span></div>}
              </div>

              <div style={{ borderBottom: "1px solid #000", paddingBottom: 5, marginBottom: 10, fontWeight: "bold", fontSize: 13, display: "flex" }}>
                <span style={{ flex: 1 }}>DESCRIPCIÓN</span>
                <span style={{ width: 80, textAlign: "right" }}>TOTAL</span>
              </div>

              <div style={{ fontSize: 13, display: "flex", marginBottom: 20 }}>
                <span style={{ flex: 1 }}>{boleta.referenciaEntidad || "Servicio de Transporte / Encomienda"}</span>
                <span style={{ width: 80, textAlign: "right" }}>{boleta.monto?.toFixed(2)}</span>
              </div>

              <div style={{ borderTop: "2px solid #000", paddingTop: 10, textAlign: "right" }}>
                <div style={{ fontSize: 16, fontWeight: "bold" }}>TOTAL: S/ {boleta.monto?.toFixed(2)}</div>
              </div>

              <div style={{ marginTop: 40, textAlign: "center", fontSize: 11 }}>
                <div>Gracias por su preferencia</div>
                <div style={{ marginTop: 10, fontStyle: "italic" }}>Representación impresa de la Boleta de Venta Electrónica. Consulte su comprobante en: www.intiwatana.com.pe</div>
              </div>
            </div>
          </div>
          <style>{`
            @media print {
              body * { visibility: hidden; }
              .printable-receipt, .printable-receipt * { visibility: visible; }
              .printable-receipt { position: absolute; left: 0; top: 0; width: 100%; margin: 0; padding: 20px; border: none; }
              .no-print { display: none !important; }
              .no-print-bg { background: none !important; backdrop-filter: none !important; }
            }
          `}</style>
        </div>
      )}

      {conf && <ConfirmModal onConfirmar={() => eliminar(conf)} onCancelar={() => setConf(null)} />}
    </>
  );
}


// ═══════════════════════════════════════════════════════
// MENU por rol
// ═══════════════════════════════════════════════════════
// ═══════════════════════════════════════════════════════
// RESERVAS / VENTAS PRESENCIALES (retenciones web pendientes)
// ═══════════════════════════════════════════════════════
function ReservasVentas({ headers, showToast }) {
  const [reservas, setReservas] = useState([]);
  const [load, setLoad] = useState(false);
  const [accion, setAccion] = useState(null);
  const [form, setForm] = useState({ metodoPago:"EFECTIVO", referenciaPago:"", documento:"", observaciones:"" });
  const [boletos, setBoletos] = useState(null);

  const cargar = useCallback(async () => {
    setLoad(true);
    try {
      const r = await fetch("/api/v1/ventas/reservas?estado=RETENIDO&estado=PENDIENTE_PAGO", { headers });
      if (!r.ok) throw new Error();
      const d = await r.json();
      setReservas(Array.isArray(d) ? d : []);
    } catch { showToast("Error al cargar reservas"); }
    finally { setLoad(false); }
  }, [headers, showToast]);

  useEffect(() => { cargar(); }, [cargar]);

  const abrirAccion = (referencia) => {
    setBoletos(null);
    setForm({ metodoPago:"EFECTIVO", referenciaPago:"", documento:"", observaciones:"" });
    setAccion(accion === referencia ? null : referencia);
  };

  const confirmar = async (referencia) => {
    try {
      const r = await fetch(`/api/v1/ventas/reservas/${encodeURIComponent(referencia)}/confirmar`, {
        method:"POST", headers,
        body: JSON.stringify({
          metodoPago: form.metodoPago,
          referenciaPago: form.referenciaPago || null,
          observaciones: form.observaciones || null,
          documento: form.documento || null,
        }),
      });
      if (!r.ok) { const d = await r.json().catch(()=>({})); throw new Error(d.mensaje || "No se pudo confirmar el pago"); }
      setBoletos(await r.json());
      showToast("Pago confirmado. Boletos emitidos.");
      cargar();
    } catch (e) { showToast(e.message || "No se pudo confirmar"); }
  };

  const liberar = async (referencia) => {
    try {
      const r = await fetch(`/api/v1/ventas/reservas/${encodeURIComponent(referencia)}/liberar?motivo=${encodeURIComponent("Liberada desde caja")}`, { method:"POST", headers });
      if (!r.ok) { const d = await r.json().catch(()=>({})); throw new Error(d.mensaje || "No se pudo liberar"); }
      showToast("Reserva liberada");
      setAccion(null); cargar();
    } catch (e) { showToast(e.message || "No se pudo liberar"); }
  };

  const abrirPdf = (numeroBoleto) => {
    if (!form.documento) { showToast("Ingresa el documento del comprador para descargar el boleto"); return; }
    window.open(`/api/v1/ventas/boleto/${encodeURIComponent(numeroBoleto)}/pdf?documento=${encodeURIComponent(form.documento)}`, "_blank", "noopener,noreferrer");
  };

  const badgeEstado = (estado) => <Badge text={estado}/>;

  const inputS = { width:"100%", background:"white", border:`1.5px solid ${C.grisClaro}55`, borderRadius:8, padding:"8px 11px", color:C.oscuro, fontSize:13, fontFamily:"inherit", boxSizing:"border-box" };

  return (
    <div>
      <PageHeader title="Reservas pendientes de pago" sub="Retenciones web y ventas por confirmar en caja" action={
        <button onClick={cargar} disabled={load} className="adm-btn-secondary">
          <RefreshCw size={14} className={load?"spin-icon":undefined}/> Actualizar
        </button>
      }/>

      <TableWrap load={load} empty={!reservas.length&&"No hay reservas pendientes de pago"}>
        <table className="adm-table tbl-reservas">
          <thead>
            <tr><th>Referencia</th><th>Comprador</th><th>Asientos</th><th>Total</th><th>Vence</th><th>Estado</th><th>Acciones</th></tr>
          </thead>
          <tbody>
            {reservas.map((r) => (
              <tr key={r.referencia} className="tr-hover">
                <td><span className="td-codigo" style={{color:C.verde}}>{r.referencia}</span></td>
                <td style={{fontWeight:600}}>{r.compradorNombre || "—"}</td>
                <td style={{fontSize:12,color:C.medio}}>{r.asientos?.map((a)=>`N°${a.numeroAsiento}`).join(", ") || "—"}</td>
                <td><strong style={{color:C.verde}}>S/ {Number(r.total||0).toFixed(2)}</strong></td>
                <td style={{fontSize:12,color:C.medio}}>{fmtFecha(r.expiraEn)}</td>
                <td>{badgeEstado(r.estado)}</td>
                <td>
                  <button onClick={() => abrirAccion(r.referencia)} className="btn-edit" style={{background:accion===r.referencia?"rgba(213,139,21,0.16)":"rgba(18,115,105,0.09)",color:accion===r.referencia?"#a0650f":"#0e5e56",minHeight:38,gap:6}}>
                    <CheckCircle2 size={13}/> {accion===r.referencia?"Ocultar":"Gestionar"}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </TableWrap>

      {accion && (
        <div style={{ marginTop:16, background:C.blanco, border:`1.5px solid ${C.verde}55`, borderRadius:14, padding:"1.2rem" }}>
          <div style={{ fontWeight:700, color:C.oscuro, marginBottom:12, display:"flex", alignItems:"center", gap:8 }}><CreditCard size={17} color={C.verde}/> Registrar pago — {accion}</div>
          <div style={{ display:"grid", gridTemplateColumns:"repeat(auto-fit, minmax(190px, 1fr))", gap:12, marginBottom:12 }}>
            <div>
              <label style={{ fontSize:12, color:C.gris, display:"block", marginBottom:4 }}>Método de pago *</label>
              <select value={form.metodoPago} onChange={(e)=>setForm((p)=>({...p,metodoPago:e.target.value}))} style={inputS}>
                {["EFECTIVO","YAPE","PLIN","TRANSFERENCIA","TARJETA_CREDITO","TARJETA_DEBITO"].map((m)=><option key={m} value={m}>{m}</option>)}
              </select>
            </div>
            <div>
              <label style={{ fontSize:12, color:C.gris, display:"block", marginBottom:4 }}>N° operación</label>
              <input value={form.referenciaPago} onChange={(e)=>setForm((p)=>({...p,referenciaPago:e.target.value}))} style={inputS}/>
            </div>
            <div>
              <label style={{ fontSize:12, color:C.gris, display:"block", marginBottom:4 }}>Documento del comprador</label>
              <input value={form.documento} onChange={(e)=>setForm((p)=>({...p,documento:e.target.value}))} placeholder="Para descargar el boleto" style={inputS}/>
            </div>
            <div>
              <label style={{ fontSize:12, color:C.gris, display:"block", marginBottom:4 }}>Observaciones</label>
              <input value={form.observaciones} onChange={(e)=>setForm((p)=>({...p,observaciones:e.target.value}))} style={inputS}/>
            </div>
          </div>
          <div style={{ display:"flex", gap:10, flexWrap:"wrap" }}>
            <button onClick={()=>confirmar(accion)} style={{ background:C.verde, color:"white", border:"none", borderRadius:8, padding:"9px 16px", cursor:"pointer", fontWeight:700, display:"flex", alignItems:"center", gap:6 }}>
              <CheckCircle2 size={15}/> Confirmar pago y emitir boleto
            </button>
            <button onClick={()=>liberar(accion)} style={{ background:"transparent", color:C.rojo, border:`1.5px solid ${C.rojo}66`, borderRadius:8, padding:"9px 16px", cursor:"pointer", fontWeight:700, display:"flex", alignItems:"center", gap:6 }}>
              <X size={15}/> Liberar reserva
            </button>
            <button onClick={()=>setAccion(null)} style={{ background:"transparent", color:C.gris, border:`1.5px solid ${C.grisClaro}55`, borderRadius:8, padding:"9px 16px", cursor:"pointer", fontWeight:700 }}>Cerrar</button>
          </div>

          {boletos && boletos.length > 0 && (
            <div style={{ marginTop:16, background:`${C.verde}0f`, border:`1px solid ${C.verde}44`, borderRadius:10, padding:"12px 14px" }}>
              <div style={{ fontWeight:700, color:C.verde, marginBottom:8, display:"flex", alignItems:"center", gap:6 }}><CheckCircle2 size={15}/> Boletos emitidos</div>
              {boletos.map((b)=>(
                <div key={b.numeroBoleto} style={{ display:"flex", justifyContent:"space-between", alignItems:"center", gap:10, padding:"6px 0", borderTop:`1px solid ${C.grisClaro}22` }}>
                  <span style={{ fontFamily:"monospace", color:C.oscuro, fontSize:12.5 }}>{b.numeroBoleto} · Asiento N°{b.asiento?.numeroAsiento}</span>
                  <button onClick={()=>abrirPdf(b.numeroBoleto)} style={{ background:C.amarillo, color:C.oscuro, border:"none", borderRadius:7, padding:"5px 11px", cursor:"pointer", fontWeight:800, fontSize:12, display:"flex", alignItems:"center", gap:5 }}>
                    <Download size={13}/> PDF
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}

const MENU_ADMIN = [
  { group:"OPERACIÓN", items:[
    { key:"dashboard",   icon:<LayoutDashboard size={19}/>, label:"Panel" },
    { key:"viajes",      icon:<CalendarDays size={19}/>,    label:"Viajes" },
    { key:"reservas",    icon:<Ticket size={19}/>,          label:"Reservas" },
    { key:"pagos",       icon:<CreditCard size={19}/>,      label:"Pagos" },
  ]},
  { group:"FLUJO DE PAGOS", items:[
    { key:"flujoVentas", icon:<Smartphone size={19}/>,      label:"Flujo de pagos" },
  ]},
  { group:"CONFIGURACIÓN", items:[
    { key:"sucursales",  icon:<Building2 size={19}/>,       label:"Sucursales" },
    { key:"rutas",       icon:<Route size={19}/>,           label:"Rutas" },
    { key:"buses",       icon:<Bus size={19}/>,             label:"Autobuses" },
    { key:"personal",    icon:<Shield size={19}/>,          label:"Personal" },
  ]},
  { group:"CLIENTES Y SERVICIOS", items:[
    { key:"clientes",    icon:<Users size={19}/>,           label:"Clientes" },
    { key:"encomiendas", icon:<Package size={19}/>,         label:"Encomiendas" },
  ]},
  { group:"GESTIÓN", items:[
    { key:"reportes",    icon:<BarChart3 size={19}/>,       label:"Reportes" },
    { key:"portal",      icon:<LayoutTemplate size={19}/>,  label:"Gestión del Portal" },
  ]},
];

const MENU_CAJERO = [
  { group:"OPERACIÓN DE CAJA", items:[
    { key:"dashboard",   icon:<LayoutDashboard size={19}/>, label:"Inicio" },
    { key:"nuevaVenta",  icon:<Ticket size={19}/>,          label:"Nueva venta" },
    { key:"bandejaYape", icon:<Smartphone size={19}/>,      label:"Bandeja Yape" },
    { key:"reservas",    icon:<Ticket size={19}/>,          label:"Reservas pendientes" },
    { key:"pagos",       icon:<CreditCard size={19}/>,      label:"Pagos" },
    { key:"boletos",     icon:<Ticket size={19}/>,          label:"Boletos" },
  ]},
  { group:"CLIENTES Y SERVICIOS", items:[
    { key:"clientes",    icon:<Users size={19}/>,           label:"Clientes" },
    { key:"encomiendas", icon:<Package size={19}/>,         label:"Encomiendas" },
  ]},
  { group:"CONTROL", items:[
    { key:"operaciones", icon:<BarChart3 size={19}/>,       label:"Operaciones" },
    { key:"cierre",      icon:<CheckCircle2 size={19}/>,    label:"Resumen de Caja" },
    { key:"cierreViaje", icon:<CalendarDays size={19}/>,    label:"Cierre por viaje" },
  ]},
];

// ═══════════════════════════════════════════════════════
// LAYOUT PRINCIPAL
// ═══════════════════════════════════════════════════════
export default function AdminDashboard() {
  const { session, logout } = useAuth();
  const [seccion, setSeccion] = useState("dashboard");
  const [toast, setToast] = useState(null);
  const [sideCollapsed, setSideCollapsed] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);
  const [isMobile, setIsMobile] = useState(() => typeof window !== "undefined" && window.innerWidth <= 1024);
  const toastTimer = useRef(null);

  const headers = useMemo(() => ({ "Content-Type":"application/json", Authorization:`Bearer ${session.token}` }), [session.token]);

  const isAdmin = session.rol === "ROLE_ADMIN";
  const MENU = isAdmin ? MENU_ADMIN : MENU_CAJERO;
  const menuItems = useMemo(() => MENU.flatMap(g => g.items), [MENU]);

  // Debe ser estable: los módulos hijos dependen de showToast dentro de useCallback
  // y disparan su carga con useEffect([cargar]). Si esta función cambiara de
  // identidad en cada render, un fallo de red re-renderizaría el padre, lo que
  // volvería a disparar la carga y entraría en un bucle infinito de peticiones.
  const showToast = useCallback((msg) => {
    if (toastTimer.current) clearTimeout(toastTimer.current);
    setToast(msg);
    toastTimer.current = setTimeout(() => setToast(null), 3500);
  }, []);

  // Limpia el temporizador al desmontar para no actualizar estado sin componente.
  useEffect(() => () => {
    if (toastTimer.current) clearTimeout(toastTimer.current);
  }, []);

  const iniciales = (session.nombreCompleto||session.username||"AD").split(" ").map(w=>w[0]).join("").slice(0,2).toUpperCase();
  const rolLabel = { ROLE_ADMIN:"Administrador", ROLE_CAJERO:"Cajero", ROLE_VENDEDOR:"Vendedor", ROLE_OPERADOR_ENCOMIENDAS:"Operador de Encomiendas" }[session.rol] || session.rol;

  // Accesibilidad / responsive: Escape cierra el menú móvil y el ancho de
  // ventana decide si el sidebar es fijo (escritorio) o deslizante (móvil).
  useEffect(() => {
    const onKey = (e) => { if (e.key === "Escape") setMobileOpen(false); };
    const onResize = () => setIsMobile(window.innerWidth <= 1024);
    window.addEventListener("keydown", onKey);
    window.addEventListener("resize", onResize);
    return () => { window.removeEventListener("keydown", onKey); window.removeEventListener("resize", onResize); };
  }, []);

  // En móvil el sidebar nunca se colapsa: se muestra completo al abrirse.
  const collapsed = sideCollapsed && !isMobile;

  const sectionSub = {
    dashboard: isAdmin ? "Resumen general del sistema" : "Resumen operativo de caja",
    nuevaVenta: "Venta presencial (viaje → asiento → pasajero → pago → confirmación → boleto)",
    boletos: "Gestión de boletos emitidos",
    operaciones: "Operaciones del cajero",
    cierre: "Resumen operativo de caja derivado de movimientos confirmados",
    sucursales: "Puntos de atención y terminales",
    rutas: "Rutas y tarifas base",
    buses: "Flota de autobuses",
    viajes: isAdmin ? "Programación de viajes" : "Consulta de viajes y disponibilidad de asientos",
    clientes: "Registro y consulta de clientes",
    pagos: "Registro y control de pagos",
    reservas: "Reservas pendientes de confirmación",
    encomiendas: "Recepción y seguimiento de encomiendas",
    reportes: "Reportes del sistema",
    portal: "Contenido del portal público",
    personal: "Cuentas y roles del personal",
  }[seccion] || "Sistema Web INTIWATANA S.R.L.";

  const irA = (key) => { setSeccion(key); setMobileOpen(false); };

  return (
    <div className="adm-shell" style={{display:"flex",height:"100vh",overflow:"hidden",background:C.fondo,fontFamily:"'Source Sans 3', Arial, sans-serif"}}>

      {/* SIDEBAR */}
      <aside className={`adm-sidebar${collapsed?" collapsed":""}${mobileOpen?" open":""}`} aria-label="Navegación principal">
        {/* Logo */}
        <div className="adm-brand">
          <LogoSVG width={36} height={36}/>
          {!collapsed&&<div className="adm-brand-txt">
            <div className="adm-brand-name">INTIWATANA</div>
            <div className="adm-brand-sub">{isAdmin?"Administración":"Caja y ventas"}</div>
          </div>}
          <button className="adm-close-mobile" onClick={()=>setMobileOpen(false)} aria-label="Cerrar menú"><X size={18}/></button>
        </div>

        {/* Nav agrupada */}
        <nav id="adm-sidebar-nav" aria-label="Módulos del panel">
          {MENU.map((grupo)=>(
            <div key={grupo.group} className="adm-nav-group">
              {!collapsed&&<div className="adm-nav-group-title">{grupo.group}</div>}
              {grupo.items.map(({key,icon,label})=>(
                <button key={key} className={`adm-nav-item${seccion===key?" active":""}`} onClick={()=>irA(key)} aria-current={seccion===key?"page":undefined} title={collapsed?label:undefined}>
                  <span className="adm-nav-icon" aria-hidden="true">{icon}</span>
                  {!collapsed&&<span className="adm-nav-label">{label}</span>}
                </button>
              ))}
            </div>
          ))}
        </nav>

        {/* Usuario */}
        <div className="adm-sidebar-foot">
          <div className={`adm-session${collapsed?" only-icon":""}`}>
            <div className="adm-avatar">{iniciales}</div>
            {!collapsed&&<div className="adm-session-txt">
              <div className="adm-session-name">{session.nombreCompleto?.split(" ").slice(0,2).join(" ")||session.username}</div>
              <div className="adm-session-rol">{rolLabel}</div>
            </div>}
            {!collapsed&&<button className="adm-logout-side" onClick={logout} title="Cerrar sesión" aria-label="Cerrar sesión"><LogOut size={16}/></button>}
          </div>
        </div>
      </aside>

      {/* Fondo oscuro del menú móvil */}
      {mobileOpen&&isMobile&&<div className="adm-backdrop" onClick={()=>setMobileOpen(false)} aria-hidden="true"/>}

      {/* CONTENIDO */}
      <div className="adm-main">
        {/* Topbar ejecutiva */}
        <header className="adm-header">
          <div className="adm-header-left">
            <button className="adm-hamburger" onClick={()=>setMobileOpen(true)} aria-label="Abrir menú de navegación" aria-expanded={mobileOpen} aria-controls="adm-sidebar-nav">
              <Menu size={20}/>
            </button>
            <button className="adm-collapse" onClick={()=>setSideCollapsed(s=>!s)} aria-label={sideCollapsed?"Expandir menú lateral":"Contraer menú lateral"} title={sideCollapsed?"Expandir menú":"Contraer menú"}>
              {sideCollapsed?<ChevronRight size={18}/>:<ChevronLeft size={18}/>}
            </button>
            <div className="adm-crumb" aria-live="polite">
              <div className="adm-crumb-title">{menuItems.find(m=>m.key===seccion)?.label||"Panel"}</div>
              <div className="adm-sub">{sectionSub[seccion]}</div>
            </div>
          </div>
          <div className="adm-header-right">
            <div className="adm-date-chip" aria-label="Fecha actual">{new Date().toLocaleDateString("es-PE",{weekday:"short",day:"2-digit",month:"long"})}</div>
            <div className="adm-userchip" aria-label={`Sesión de ${session.nombreCompleto||session.username}`}>
              <div className="adm-avatar small">{iniciales}</div>
              <div className="adm-userchip-txt">
                <div className="adm-userchip-name">{session.nombreCompleto?.split(" ").slice(0,2).join(" ")||session.username}</div>
                <div className="adm-userchip-rol">{rolLabel}</div>
              </div>
            </div>
            <button className="adm-logout" onClick={logout} aria-label="Cerrar sesión"><LogOut size={15}/> <span className="adm-logout-txt">Salir</span></button>
          </div>
        </header>

        {/* Área principal */}
        <main className="adm-content">
          {seccion==="dashboard"   && <DashboardHome    headers={headers} onNav={irA} isCajero={!isAdmin}/>}
          {seccion==="sucursales"  && <ErrorBoundary key="suc"><CrudSucursales  headers={headers} showToast={showToast}/></ErrorBoundary>}
          {seccion==="rutas"       && <ErrorBoundary key="rut"><CrudRutas       headers={headers} showToast={showToast}/></ErrorBoundary>}
          {seccion==="buses"       && <ErrorBoundary key="bus"><CrudBuses       headers={headers} showToast={showToast}/></ErrorBoundary>}
          {seccion==="viajes"      && <ErrorBoundary key="via"><CrudViajes      headers={headers} showToast={showToast} readOnly={!isAdmin}/></ErrorBoundary>}
          {seccion==="clientes"    && <ErrorBoundary key="cli"><CrudClientes    headers={headers} showToast={showToast} puedeEliminar={isAdmin}/></ErrorBoundary>}
          {seccion==="pagos"       && <ErrorBoundary key="pag"><CrudPagos       headers={headers} showToast={showToast} puedeEliminar={isAdmin}/></ErrorBoundary>}
          {seccion==="nuevaVenta" && !isAdmin && <ErrorBoundary key="nv"><VentaPresencial headers={headers} showToast={showToast} irA={irA}/></ErrorBoundary>}
          {seccion==="boletos"     && !isAdmin && <ErrorBoundary key="bol"><BoletosCaja    headers={headers} showToast={showToast}/></ErrorBoundary>}
          {seccion==="operaciones" && !isAdmin && <ErrorBoundary key="op"><OperacionesCaja headers={headers} showToast={showToast}/></ErrorBoundary>}
          {seccion==="cierre"      && !isAdmin && <ErrorBoundary key="cierre"><CierreCaja     headers={headers} showToast={showToast}/></ErrorBoundary>}
          {seccion==="reservas"    && <ErrorBoundary key="rsv"><ReservasVentas  headers={headers} showToast={showToast}/></ErrorBoundary>}
          {seccion==="flujoVentas" && isAdmin && <ErrorBoundary key="flujo"><CrudFlujoVentas headers={headers} showToast={showToast}/></ErrorBoundary>}
          {seccion==="bandejaYape" && !isAdmin && <ErrorBoundary key="by"><BandejaPagosYape headers={headers} showToast={showToast}/></ErrorBoundary>}
          {seccion==="cierreViaje" && !isAdmin && <ErrorBoundary key="cv"><ConsultaCierreViaje headers={headers} showToast={showToast}/></ErrorBoundary>}
          {seccion==="encomiendas" && <ErrorBoundary key="enc"><CrudEncomiendas headers={headers} showToast={showToast}/></ErrorBoundary>}
        {seccion==="reportes"     && <ErrorBoundary key="rep"><ReportesPage   headers={headers} showToast={showToast}/></ErrorBoundary>}
          {seccion==="portal"      && isAdmin && <ErrorBoundary key="por"><PortalGestion  headers={headers} showToast={showToast}/></ErrorBoundary>}
          {seccion==="personal"    && isAdmin && <ErrorBoundary key="per"><CrudPersonal    headers={headers} showToast={showToast}/></ErrorBoundary>}
        </main>
      </div>

      {/* TOAST */}
      {toast&&(
        <div className={`adm-toast${toast.startsWith("Error")||toast.startsWith("⚠")?" error":""}`} role="status" aria-live="polite">
          {toast.startsWith("Error")||toast.startsWith("⚠")?<AlertCircle size={16}/>:<CheckCircle2 size={16}/>}
          <div>{toast}</div>
        </div>
      )}

      <style>{`
        @import url('https://fonts.googleapis.com/css2?family=Playfair+Display:wght@700;800&family=Source+Sans+3:wght@400;600;700&display=swap');

        /* ── T O K E N S  (§7) ─────────────────────────────── */
        .adm-shell{
          --verde:#127369; --oscuro:#0B3D38; --amarillo:#F5C518; --gold:#C99B00;
          --verde2:#104A43; --verde3:#1A8A7E; --fondo:#F4F7F6; --blanco:#FFFFFF;
          --tinta:#17312E; --medio:#667875; --borde:#DDE7E4;
          --ok:#158463; --warn:#D58B15; --rojo:#C84C4C; --info:#397CBF;
          --radio:14px; --radio-sm:10px;
          --lapiz:0 1px 3px rgba(16,64,59,0.07);
          --sombra:0 10px 30px rgba(16,64,59,0.10);
          font-family:'Source Sans 3',Arial,sans-serif;
        }

        @keyframes fadeUp  {from{opacity:0;transform:translateY(14px)}to{opacity:1;transform:none}}
        @keyframes fadeIn  {from{opacity:0}to{opacity:1}}
        @keyframes slideUp {from{opacity:0;transform:translateY(16px)}to{opacity:1;transform:none}}
        @keyframes modalIn {from{opacity:0;transform:scale(0.96) translateY(10px)}to{opacity:1;transform:none}}
        @keyframes spin    {to{transform:rotate(360deg)}}
        @keyframes pulse   {0%,100%{transform:scale(1)}50%{transform:scale(1.12)}}
        @keyframes skeletonShimmer{0%{background-position:-400px 0}100%{background-position:400px 0}}

        .pulse-logo{animation:pulse 1.4s ease infinite;display:inline-block;color:var(--verde);}
        .spin-icon{animation:spin 1s linear infinite;}

        /* ── Loading / skeleton / spinner ──────────────────── */
        .adm-loading{display:flex;flex-direction:column;align-items:center;justify-content:center;gap:12px;padding:6rem 0;color:var(--medio);font-size:14px;}
        .skeleton-rows{display:flex;flex-direction:column;gap:10px;padding:1rem 1.2rem;}
        .skeleton-head{height:40px;border-radius:10px;background:linear-gradient(90deg,#EDF4F2 25%,#F8FBFA 50%,#EDF4F2 75%);background-size:800px 100%;animation:skeletonShimmer 1.4s infinite linear;margin-bottom:4px;}
        .skeleton-row{height:40px;border-radius:10px;background:linear-gradient(90deg,#EDF4F2 25%,#F8FBFA 50%,#EDF4F2 75%);background-size:800px 100%;animation:skeletonShimmer 1.4s infinite linear;}
        .spinner{display:inline-block;width:16px;height:16px;border:2.5px solid rgba(255,255,255,0.35);border-top-color:#fff;border-radius:50%;animation:spin 0.7s linear infinite;}

        /* ── Cabecera de módulo ────────────────────────────── */
        .adm-topbar{display:flex;align-items:flex-end;justify-content:space-between;margin-bottom:20px;gap:1rem;flex-wrap:wrap;}
        .adm-head{display:flex;align-items:flex-start;gap:14px;min-width:0;}
        .adm-head-rule{width:5px;align-self:stretch;border-radius:99px;background:linear-gradient(180deg,var(--amarillo),var(--gold));flex-shrink:0;min-height:44px;}
        .adm-page-title{font-family:'Playfair Display',serif;font-size:1.7rem;font-weight:800;color:var(--oscuro);margin:0;letter-spacing:-0.01em;line-height:1.1;}
        .adm-page-sub{font-size:13.5px;color:var(--medio);margin:5px 0 0;}
        .adm-head-actions{display:flex;gap:10px;flex-wrap:wrap;}

        /* ── Botones ───────────────────────────────────────── */
        .adm-btn-primary{background:linear-gradient(180deg,var(--verde),#0f5e56);color:#fff;border:none;border-radius:12px;padding:11px 20px;min-height:44px;font-size:13.5px;font-weight:700;font-family:inherit;cursor:pointer;transition:all 0.18s;display:inline-flex;align-items:center;justify-content:center;box-shadow:0 4px 12px rgba(18,115,105,0.28);}
        .adm-btn-primary:hover{background:linear-gradient(180deg,var(--oscuro),#0E4A42);transform:translateY(-1px);}
        .adm-btn-secondary{background:#fff;color:var(--oscuro);border:1.5px solid var(--verde2);border-radius:12px;padding:11px 20px;min-height:44px;font-size:13px;font-weight:700;font-family:inherit;cursor:pointer;transition:all 0.18s;display:inline-flex;align-items:center;justify-content:center;gap:6px;}
        .adm-btn-secondary:hover{background:rgba(18,115,105,0.08);border-color:var(--verde);}

        /* ── Búsqueda / filtros ────────────────────────────── */
        .adm-search-row{margin-bottom:16px;position:relative;max-width:420px;}
        .adm-search{border:1.5px solid var(--borde);border-radius:12px;padding:11px 14px;min-height:44px;font-size:13.5px;font-family:inherit;color:var(--tinta);background:#fff;outline:none;width:100%;box-sizing:border-box;transition:border 0.18s,box-shadow 0.18s;}
        .adm-search:focus{border-color:var(--gold);box-shadow:0 0 0 3px rgba(245,197,24,0.22);}

        /* ── Tablas empresariales ──────────────────────────── */
        .adm-table-wrap{background:#fff;border-radius:var(--radio);border:1.5px solid var(--borde);box-shadow:var(--lapiz);overflow:hidden;margin-bottom:18px;}
        .adm-table-wrap .adm-table{border:0;border-radius:0;}
        .adm-table{width:100%;border-collapse:collapse;font-size:13.5px;}
        .adm-table th{background:#F6FAF9;padding:11px 16px;text-align:left;font-size:11px;font-weight:800;letter-spacing:0.09em;text-transform:uppercase;color:var(--medio);border-bottom:1.5px solid var(--borde);white-space:nowrap;}
        .adm-table td{padding:12px 16px;border-bottom:1px solid #EEF4F3;color:var(--tinta);vertical-align:middle;}
        .adm-table tr:last-child td{border-bottom:none;}
        .tr-hover:hover td{background:#F8FBFA;transition:background 0.12s;}
        .td-empty{text-align:center;color:var(--medio);padding:3.5rem 1.5rem;font-size:14px;display:flex;flex-direction:column;align-items:center;gap:8px;}
        .td-empty-icon{width:56px;height:56px;border-radius:18px;background:var(--fondo);border:1.5px solid var(--borde);display:flex;align-items:center;justify-content:center;color:var(--medio);margin-bottom:4px;}
        .td-empty-hint{font-size:12.5px;color:var(--medio);opacity:0.8;max-width:360px;}
        .td-codigo{font-family:Consolas,monospace;font-size:11.5px;background:rgba(18,115,105,0.09);color:#0e5e56;border-radius:7px;padding:3px 9px;font-weight:700;white-space:nowrap;}
        .td-lugar{display:flex;flex-direction:column;gap:2px;}
        .td-ciudad{font-weight:700;font-size:13.5px;color:var(--tinta);}
        .td-actions{display:flex;gap:8px;flex-wrap:wrap;}

        .btn-edit{background:rgba(18,115,105,0.09);color:#0e5e56;border:none;border-radius:10px;min-width:40px;min-height:40px;padding:8px 11px;font-size:12px;font-weight:700;font-family:inherit;cursor:pointer;transition:all 0.15s;display:inline-flex;align-items:center;justify-content:center;}
        .btn-edit:hover{background:rgba(18,115,105,0.18);}
        .btn-delete{background:rgba(200,76,76,0.09);color:var(--rojo);border:none;border-radius:10px;min-width:40px;min-height:40px;padding:8px 11px;font-size:12px;font-weight:700;font-family:inherit;cursor:pointer;transition:all 0.15s;display:inline-flex;align-items:center;justify-content:center;}
        .btn-delete:hover{background:rgba(200,76,76,0.18);}
        .btn-delete-confirm{background:var(--rojo);color:#fff;border:none;border-radius:11px;padding:11px 22px;font-size:14px;font-weight:700;font-family:inherit;cursor:pointer;transition:all 0.15s;display:inline-flex;align-items:center;gap:6px;justify-content:center;min-height:44px;}
        .btn-delete-confirm:hover{background:#a53f3f;}
        .btn-cancelar{background:#fff;color:var(--tinta);border:1.5px solid var(--borde);border-radius:11px;padding:11px 18px;min-height:44px;font-size:14px;font-weight:700;font-family:inherit;cursor:pointer;transition:all 0.15s;}
        .btn-cancelar:hover{background:var(--fondo);}
        .btn-reservar{background:linear-gradient(180deg,var(--verde),#0f5e56);color:#fff;border:none;border-radius:11px;padding:11px 22px;min-height:44px;font-size:14px;font-weight:700;font-family:inherit;cursor:pointer;flex:1;transition:all 0.18s;display:flex;align-items:center;justify-content:center;gap:8px;box-shadow:0 4px 12px rgba(18,115,105,0.24);}
        .btn-reservar:hover:not(:disabled){background:linear-gradient(180deg,var(--oscuro),#0E4A42);}
        .btn-reservar:disabled{opacity:0.6;cursor:not-allowed;}

        /* ── Badges ────────────────────────────────────────── */
        .adm-badge{display:inline-flex;align-items:center;border-radius:99px;padding:4px 12px;font-size:11.5px;font-weight:700;letter-spacing:0.03em;white-space:nowrap;border:1px solid rgba(23,49,46,0.05);}

        /* ── KPI / StatCard ────────────────────────────────── */
        .adm-stat{background:#fff;border-radius:var(--radio);padding:18px 18px 16px;position:relative;overflow:hidden;transition:border 0.22s,box-shadow 0.22s,transform 0.22s;display:flex;flex-direction:column;}
        .adm-stat:hover{transform:translateY(-3px);}
        .adm-stat-icon{width:42px;height:42px;border-radius:12px;display:flex;align-items:center;justify-content:center;margin-bottom:12px;}
        .adm-stat-val{font-size:32px;font-weight:900;color:var(--oscuro);font-family:'Playfair Display',serif;line-height:1;}
        .adm-stat-label{font-size:11.5px;font-weight:700;color:var(--medio);letter-spacing:0.05em;text-transform:uppercase;margin-top:6px;}
        .adm-stat-sub{font-size:11.5px;color:var(--medio);margin-top:2px;}

        /* ── Formularios ───────────────────────────────────── */
        .form-grid{display:grid;grid-template-columns:1fr 1fr;gap:0 1.2rem;}
        .lf-field{margin-bottom:1.05rem;}
        .lf-field label{display:block;font-size:12.5px;font-weight:700;color:var(--tinta);margin-bottom:6px;}
        .lf-field input,.lf-select{width:100%;border:1.5px solid var(--borde);border-radius:10px;padding:11px 13px;min-height:44px;font-size:14px;font-family:inherit;color:var(--tinta);background:#fff;outline:none;box-sizing:border-box;transition:border 0.18s,box-shadow 0.18s;}
        .lf-field input:focus,.lf-select:focus{border-color:var(--gold);box-shadow:0 0 0 3px rgba(245,197,24,0.22);}
        .lf-field input:disabled{background:#F5F7F6;color:var(--medio);cursor:not-allowed;}

        /* ── Modales premium ───────────────────────────────── */
        .modal-overlay{position:fixed;inset:0;background:rgba(9,31,28,0.55);display:flex;align-items:center;justify-content:center;z-index:9998;backdrop-filter:blur(3px);animation:fadeIn 0.2s ease;}
        .modal{background:#fff;border-radius:18px;max-width:520px;width:95%;max-height:90vh;overflow-y:auto;box-shadow:0 26px 70px rgba(9,31,28,0.35);animation:modalIn 0.24s cubic-bezier(.4,0,.2,1);}
        .modal-header{display:flex;align-items:center;gap:14px;padding:20px 24px;border-bottom:1px solid var(--borde);}
        .modal-badge{width:46px;height:46px;border-radius:12px;background:rgba(18,115,105,0.1);display:flex;align-items:center;justify-content:center;flex-shrink:0;color:var(--verde);}
        .modal-title{font-weight:800;font-size:16.5px;color:var(--oscuro);margin:0;}
        .modal-subtitle{font-size:12.5px;color:var(--medio);margin:4px 0 0;}
        .modal-close{background:rgba(102,120,117,0.1);border:none;color:var(--medio);width:34px;height:34px;border-radius:9px;cursor:pointer;margin-left:auto;line-height:1;transition:all 0.15s;display:flex;align-items:center;justify-content:center;flex-shrink:0;}
        .modal-close:hover{background:rgba(200,76,76,0.14);color:var(--rojo);}
        .modal-body{padding:20px 24px;}

        .confirm-modal{max-width:380px;width:92%;text-align:center;padding:30px 24px 26px;}
        .confirm-icon{width:72px;height:72px;border-radius:50%;background:rgba(200,76,76,0.12);color:var(--rojo);display:flex;align-items:center;justify-content:center;margin:0 auto 16px;}
        .confirm-title{font-size:18px;font-weight:800;color:var(--oscuro);margin:0 0 6px;}
        .confirm-text{font-size:13.5px;color:var(--medio);margin:0 0 22px;}
        .confirm-actions{display:flex;gap:10px;justify-content:center;}

        /* ── Tarjetas / paneles ────────────────────────────── */
        .card-panel{background:#fff;border-radius:var(--radio);padding:18px 20px;box-shadow:var(--lapiz);border:1.5px solid var(--borde);margin-bottom:18px;}
        .panel-title{font-weight:800;font-size:13.5px;color:var(--oscuro);margin-bottom:14px;display:flex;align-items:center;gap:7px;}
        .adm-card-head{display:flex;align-items:center;justify-content:space-between;margin-bottom:14px;flex-wrap:wrap;gap:8px;}
        .adm-link{background:transparent;border:none;color:var(--verde);font-size:12.5px;font-weight:700;font-family:inherit;cursor:pointer;display:inline-flex;align-items:center;gap:4px;padding:4px;}
        .adm-link:hover{color:var(--oscuro);}
        .adm-vacio-mini{color:var(--medio);font-size:13px;padding:10px 0;margin:0;}
        .adm-count{font-size:12px;color:var(--medio);font-weight:600;}

        /* ── Dashboards ────────────────────────────────────── */
        .adm-dash>*{animation:fadeUp 0.3s ease;}
        .adm-banner{position:relative;border-radius:20px;overflow:hidden;background:linear-gradient(120deg,#012A26,#0B3D38 55%,#127369);color:#fff;display:flex;align-items:center;justify-content:space-between;gap:18px;padding:30px 34px;margin-bottom:22px;box-shadow:0 14px 40px rgba(9,31,28,0.35);min-height:168px;border:1px solid rgba(200,153,0,0.35);}
        .adm-banner-art{position:absolute;inset:0;width:100%;height:100%;opacity:1;}
        .adm-banner-content{position:relative;z-index:1;max-width:520px;}
        .adm-banner-eyebrow{display:inline-flex;align-items:center;gap:6px;font-size:11px;font-weight:800;letter-spacing:0.16em;text-transform:uppercase;color:#FFD935;background:rgba(245,197,24,0.12);border:1px solid rgba(245,197,24,0.4);padding:5px 12px;border-radius:99px;margin-bottom:12px;}
        .adm-banner-title{font-family:'Playfair Display',serif;font-size:2.1rem;font-weight:800;margin:0 0 6px;color:#fff;letter-spacing:-0.01em;}
        .adm-banner-sub{font-size:14px;color:#C9DED8;margin:0;line-height:1.45;}
        .adm-banner-side{position:relative;z-index:1;display:flex;flex-direction:column;align-items:flex-end;gap:10px;flex-shrink:0;}
        .adm-banner-date{font-size:13px;color:#EAF3F0;font-weight:600;}
        .adm-banner-chip{display:inline-flex;align-items:center;gap:6px;font-size:11.5px;font-weight:700;color:#DFF0EB;background:rgba(255,255,255,0.1);border:1px solid rgba(255,255,255,0.18);padding:6px 12px;border-radius:99px;}
        .adm-stat-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(190px,1fr));gap:14px;margin-bottom:20px;}
        .adm-dash-row2{display:grid;grid-template-columns:1fr 1fr;gap:18px;align-items:start;}
        .adm-ocup-val{font-size:40px;font-weight:900;color:var(--verde);font-family:'Playfair Display',serif;margin-bottom:10px;line-height:1;}
        .adm-mini-grid{display:grid;grid-template-columns:1fr 1fr 1fr;gap:8px;margin-top:14px;}
        .adm-mini-dato{text-align:center;background:#F8FAF9;border-radius:10px;padding:10px 4px;border:1px solid var(--borde);}
        .adm-mini-val{font-size:22px;font-weight:900;line-height:1;}
        .adm-mini-label{font-size:10.5px;color:var(--medio);margin-top:3px;font-weight:700;text-transform:uppercase;letter-spacing:0.04em;}
        .adm-lista{display:flex;flex-direction:column;gap:2px;}
        .adm-lista-fila{display:flex;align-items:center;gap:12px;padding:9px 6px;border-bottom:1px dashed var(--borde);}
        .adm-lista-fila:last-child{border-bottom:none;}
        .adm-lista-icono{width:38px;height:38px;border-radius:11px;display:flex;align-items:center;justify-content:center;flex-shrink:0;}
        .adm-lista-main{flex:1;min-width:0;}
        .adm-lista-titulo{font-weight:700;font-size:13.5px;color:var(--tinta);white-space:nowrap;overflow:hidden;text-overflow:ellipsis;}
        .adm-lista-sub{font-size:11.5px;color:var(--medio);margin-top:2px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;}
        .adm-lista-side{display:flex;flex-direction:column;align-items:flex-end;gap:4px;flex-shrink:0;}
        .adm-precio{font-size:13px;font-weight:800;color:var(--oscuro);}

        .adm-page-btn{width:30px;height:30px;border-radius:8px;border:1.5px solid var(--borde);background:#fff;color:var(--medio);cursor:pointer;display:flex;align-items:center;justify-content:center;transition:all 0.15s;}
        .adm-page-btn:disabled{opacity:0.45;cursor:default;}
        .adm-page-btn:not(:disabled):hover{background:var(--verde);color:#fff;border-color:var(--verde);}
        .adm-paginacion{display:flex;gap:5px;margin-top:12px;justify-content:center;flex-wrap:wrap;}
        .adm-page-num{width:30px;height:30px;border-radius:8px;border:1.5px solid var(--borde);background:#fff;color:var(--medio);cursor:pointer;font-weight:700;font-size:12.5px;font-family:inherit;transition:all 0.15s;}
        .adm-page-num:hover{background:var(--fondo);}
        .adm-page-num.active{background:var(--verde);color:#fff;border-color:var(--verde);box-shadow:0 4px 10px rgba(18,115,105,0.3);}

        /* ── Layout del panel ──────────────────────────────── */
        .adm-shell{display:flex;height:100vh;overflow:hidden;background:var(--fondo);color:var(--tinta);}
        .adm-sidebar{width:260px;display:flex;flex-direction:column;height:100vh;flex-shrink:0;overflow:hidden;transition:width 0.24s cubic-bezier(.4,0,.2,1);background:linear-gradient(180deg,#0B3D38 0%,#0E4A42 40%,#0B3D38 100%);}
        .adm-sidebar.collapsed{width:72px;}
        .adm-brand{display:flex;align-items:center;gap:11px;padding:18px 16px 15px;border-bottom:1px solid rgba(255,255,255,0.08);min-height:66px;}
        .adm-brand-txt{animation:fadeIn 0.2s ease;overflow:hidden;}
        .adm-brand-name{font-family:'Playfair Display',serif;color:var(--amarillo);font-size:15px;font-weight:800;letter-spacing:0.05em;line-height:1;}
        .adm-brand-sub{color:#8FB0AB;font-size:9.5px;letter-spacing:0.14em;text-transform:uppercase;margin-top:3px;}
        .adm-close-mobile{display:none;margin-left:auto;background:transparent;border:none;color:#B7CFCB;cursor:pointer;padding:6px;border-radius:8px;}
        .adm-nav-group{padding:0 10px;}
        .adm-nav-group-title{font-size:9.5px;font-weight:800;letter-spacing:0.16em;color:#6E938D;text-transform:uppercase;padding:16px 14px 6px;}
        .adm-nav-group + .adm-nav-group-title{margin-top:6px;}
        .adm-nav-item{display:flex;align-items:center;gap:12px;width:100%;border:none;background:transparent;color:#B7CFCB;border-radius:11px;padding:10.5px 14px;font-size:14px;font-weight:600;font-family:inherit;text-align:left;cursor:pointer;transition:background 0.18s,color 0.18s;margin-bottom:2px;position:relative;}
        .adm-nav-item:hover{background:rgba(255,255,255,0.06);color:#E8F4F1;}
        .adm-nav-item.active{background:var(--verde2);color:#fff;box-shadow:inset 3px 0 0 var(--amarillo);}
        .adm-nav-icon{flex-shrink:0;display:flex;align-items:center;justify-content:center;}
        .adm-nav-label{animation:fadeIn 0.15s ease;white-space:nowrap;}
        .adm-sidebar.collapsed .adm-nav-group{padding:0 10px;}
        .adm-sidebar.collapsed .adm-nav-group-title{visibility:hidden;height:14px;position:relative;padding:9px 0 0;}
        .adm-sidebar.collapsed .adm-nav-group-title::after{content:"";visibility:visible;position:absolute;left:4px;right:4px;top:14px;height:1px;background:rgba(255,255,255,0.08);}
        .adm-sidebar.collapsed .adm-nav-item{justify-content:center;padding:11px 0;}
        .adm-sidebar.collapsed .adm-nav-label{display:none;}
        .adm-sidebar-foot{padding:12px 12px 16px;border-top:1px solid rgba(255,255,255,0.08);margin-top:auto;}
        .adm-session{display:flex;align-items:center;gap:10px;}
        .adm-session.only-icon{justify-content:center;}
        .adm-avatar{width:36px;height:36px;border-radius:50%;background:linear-gradient(135deg,var(--amarillo),var(--gold));color:#0B3D38;font-size:12px;font-weight:800;display:flex;align-items:center;justify-content:center;flex-shrink:0;}
        .adm-avatar.small{width:32px;height:32px;font-size:11px;}
        .adm-session-txt{flex:1;overflow:hidden;animation:fadeIn 0.15s ease;}
        .adm-session-name{color:#fff;font-size:12.5px;font-weight:700;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;}
        .adm-session-rol{color:#8FB0AB;font-size:10.5px;}
        .adm-logout-side{background:rgba(200,76,76,0.14);border:1px solid rgba(200,76,76,0.32);border-radius:9px;color:#F2A7A0;width:34px;height:34px;cursor:pointer;display:flex;align-items:center;justify-content:center;transition:all 0.15s;flex-shrink:0;}
        .adm-logout-side:hover{background:rgba(200,76,76,0.28);}

        .adm-main{flex:1;display:flex;flex-direction:column;overflow:hidden;min-width:0;}
        .adm-content{flex:1;overflow-y:auto;padding:24px 28px;scrollbar-width:thin;}

        /* ── Topbar ejecutiva ──────────────────────────────── */
        .adm-header{background:rgba(255,255,255,0.96);border-bottom:1px solid var(--borde);padding:0 26px;height:64px;display:flex;align-items:center;justify-content:space-between;flex-shrink:0;box-shadow:0 1px 6px rgba(16,64,59,0.04);gap:12px;position:relative;z-index:5;}
        .adm-header-left{display:flex;align-items:center;gap:10px;min-width:0;}
        .adm-crumb{min-width:0;}
        .adm-crumb-title{font-weight:800;font-size:16.5px;color:var(--oscuro);white-space:nowrap;overflow:hidden;text-overflow:ellipsis;line-height:1.2;}
        .adm-sub{font-size:11.5px;color:var(--medio);white-space:nowrap;overflow:hidden;text-overflow:ellipsis;}
        .adm-header-right{display:flex;align-items:center;gap:14px;flex-shrink:0;}
        .adm-date-chip{font-size:12.5px;font-weight:700;color:var(--medio);background:var(--fondo);border:1px solid var(--borde);border-radius:99px;padding:8px 14px;white-space:nowrap;}
        .adm-userchip{display:flex;align-items:center;gap:9px;padding:5px 12px 5px 5px;border:1.5px solid var(--borde);border-radius:99px;background:#fff;}
        .adm-userchip-txt{overflow:hidden;}
        .adm-userchip-name{font-size:12.5px;font-weight:800;color:var(--tinta);white-space:nowrap;overflow:hidden;text-overflow:ellipsis;line-height:1.2;}
        .adm-userchip-rol{font-size:10.5px;color:var(--medio);}
        .adm-logout{background:rgba(200,76,76,0.08);color:var(--rojo);border:1px solid rgba(200,76,76,0.25);border-radius:11px;padding:9px 15px;min-height:40px;font-size:12.5px;font-weight:700;cursor:pointer;font-family:inherit;transition:all 0.15s;display:flex;align-items:center;gap:7px;}
        .adm-logout:hover{background:rgba(200,76,76,0.16);}

        .adm-collapse,.adm-hamburger{background:transparent;border:1px solid var(--borde);color:var(--medio);cursor:pointer;padding:8px;border-radius:10px;display:flex;align-items:center;justify-content:center;transition:all 0.15s;}
        .adm-collapse:hover,.adm-hamburger:hover{background:var(--fondo);color:var(--oscuro);}
        .adm-hamburger{display:none;}
        .adm-backdrop{display:none;}

        /* ── Toast ─────────────────────────────────────────── */
        .adm-toast{position:fixed;bottom:24px;right:24px;background:var(--oscuro);color:#fff;padding:13px 20px;border-radius:12px;font-size:14px;font-weight:600;box-shadow:0 10px 34px rgba(9,31,28,0.4);z-index:9999;animation:slideUp 0.28s cubic-bezier(.4,0,.2,1);display:flex;align-items:center;gap:10px;font-family:inherit;max-width:380px;border:1px solid rgba(245,197,24,0.25);}
        .adm-toast.error{background:var(--rojo);border-color:rgba(255,255,255,0.2);}

        /* ── Foco visible accesible ────────────────────────── */
        .adm-shell button:focus-visible,
        .adm-shell input:focus-visible,
        .adm-shell select:focus-visible,
        .adm-shell a:focus-visible,
        .adm-shell [tabindex]:focus-visible{outline:3px solid rgba(245,197,24,0.8);outline-offset:2px;border-radius:8px;}

        /* ── Responsive tables → cards (móvil) ─────────────── */
        @media (max-width:920px){
          .adm-dash-row2{grid-template-columns:1fr;}
        }
        @media (max-width:768px){
          .adm-content{padding:16px;}
          .adm-banner{padding:24px 20px;min-height:auto;align-items:flex-start;}
          .adm-banner-title{font-size:1.6rem;}
          .adm-banner-side{display:none;}
          .adm-banner-art{opacity:0.55;}
          .adm-form-grid2 .form-grid{grid-template-columns:1fr;}
          .adm-table-wrap{overflow:hidden;box-shadow:none;border:none;background:transparent;}
          .adm-table, .adm-table tbody, .adm-table tr, .adm-table td{box-sizing:border-box;}
          .adm-table thead{display:none;}
          .adm-table tr{display:block;border:1.5px solid var(--borde);border-radius:14px;margin-bottom:12px;overflow:hidden;background:#fff;box-shadow:var(--lapiz);}
          .adm-table tbody{display:block;}
          .adm-table td{display:flex;justify-content:space-between;align-items:center;gap:14px;border:none;border-bottom:1px dashed var(--borde);padding:10px 14px;text-align:left;}
          .adm-table td:last-child{border-bottom:none;}
          .adm-table td::before{content:attr(data-label);flex-shrink:0;font-size:10px;font-weight:800;letter-spacing:0.09em;text-transform:uppercase;color:var(--medio);min-width:92px;}
          .adm-paginacion{justify-content:flex-start;}
          .tbl-sucursales td:nth-child(1)::before{content:"Nombre";}
          .tbl-sucursales td:nth-child(2)::before{content:"Código";}
          .tbl-sucursales td:nth-child(3)::before{content:"Ciudad";}
          .tbl-sucursales td:nth-child(4)::before{content:"Departamento";}
          .tbl-sucursales td:nth-child(5)::before{content:"Teléfono";}
          .tbl-sucursales td:nth-child(6)::before{content:"Terminal";}
          .tbl-sucursales td:nth-child(7)::before{content:"Acciones";}
          .tbl-rutas td:nth-child(1)::before{content:"Código";}
          .tbl-rutas td:nth-child(2)::before{content:"Ruta";}
          .tbl-rutas td:nth-child(3)::before{content:"Distancia";}
          .tbl-rutas td:nth-child(4)::before{content:"Duración est.";}
          .tbl-rutas td:nth-child(5)::before{content:"Precio base";}
          .tbl-rutas td:nth-child(6)::before{content:"Acciones";}
          .tbl-buses td:nth-child(1)::before{content:"Placa";}
          .tbl-buses td:nth-child(2)::before{content:"Marca/Modelo";}
          .tbl-buses td:nth-child(3)::before{content:"Tipo";}
          .tbl-buses td:nth-child(4)::before{content:"Capacidad";}
          .tbl-buses td:nth-child(5)::before{content:"Asientos config.";}
          .tbl-buses td:nth-child(6)::before{content:"Estado";}
          .tbl-buses td:nth-child(7)::before{content:"Pisos";}
          .tbl-buses td:nth-child(8)::before{content:"Año";}
          .tbl-buses td:nth-child(9)::before{content:"Acciones";}
          .tbl-viajes td:nth-child(1)::before{content:"Ruta";}
          .tbl-viajes td:nth-child(2)::before{content:"Bus";}
          .tbl-viajes td:nth-child(3)::before{content:"Chofer";}
          .tbl-viajes td:nth-child(4)::before{content:"Salida";}
          .tbl-viajes td:nth-child(5)::before{content:"Precio";}
          .tbl-viajes td:nth-child(6)::before{content:"Asientos";}
          .tbl-viajes td:nth-child(7)::before{content:"Estado";}
          .tbl-viajes td:nth-child(8)::before{content:"Acciones";}
          .tbl-clientes td:nth-child(1)::before{content:"Nombre";}
          .tbl-clientes td:nth-child(2)::before{content:"DNI/RUC";}
          .tbl-clientes td:nth-child(3)::before{content:"Email";}
          .tbl-clientes td:nth-child(4)::before{content:"Teléfono";}
          .tbl-clientes td:nth-child(5)::before{content:"Tipo";}
          .tbl-clientes td:nth-child(6)::before{content:"Acciones";}
          .tbl-clientes-boleto td:nth-child(1)::before{content:"Fecha";}
          .tbl-clientes-boleto td:nth-child(2)::before{content:"Ruta";}
          .tbl-clientes-boleto td:nth-child(3)::before{content:"Bus";}
          .tbl-clientes-boleto td:nth-child(4)::before{content:"Asiento";}
          .tbl-clientes-boleto td:nth-child(5)::before{content:"Precio";}
          .tbl-clientes-boleto td:nth-child(6)::before{content:"Estado";}
          .tbl-clientes-enc td:nth-child(1)::before{content:"Guía";}
          .tbl-clientes-enc td:nth-child(2)::before{content:"Fecha";}
          .tbl-clientes-enc td:nth-child(3)::before{content:"Destinatario";}
          .tbl-clientes-enc td:nth-child(4)::before{content:"Origen/Destino";}
          .tbl-clientes-enc td:nth-child(5)::before{content:"Peso";}
          .tbl-clientes-enc td:nth-child(6)::before{content:"Costo";}
          .tbl-clientes-enc td:nth-child(7)::before{content:"Estado";}
          .tbl-sol td:nth-child(1)::before{content:"Referencia";}
          .tbl-sol td:nth-child(2)::before{content:"Remitente";}
          .tbl-sol td:nth-child(3)::before{content:"Destinatario";}
          .tbl-sol td:nth-child(4)::before{content:"Ruta";}
          .tbl-sol td:nth-child(5)::before{content:"Recibida";}
          .tbl-sol td:nth-child(6)::before{content:"Acciones";}
          .tbl-encomiendas td:nth-child(1)::before{content:"Guía";}
          .tbl-encomiendas td:nth-child(2)::before{content:"Remitente";}
          .tbl-encomiendas td:nth-child(3)::before{content:"Destinatario";}
          .tbl-encomiendas td:nth-child(4)::before{content:"Peso";}
          .tbl-encomiendas td:nth-child(5)::before{content:"Tarifa";}
          .tbl-encomiendas td:nth-child(6)::before{content:"Estado";}
          .tbl-encomiendas td:nth-child(7)::before{content:"Registro";}
          .tbl-encomiendas td:nth-child(8)::before{content:"Acciones";}
          .tbl-personal td:nth-child(1)::before{content:"Usuario";}
          .tbl-personal td:nth-child(2)::before{content:"Nombre";}
          .tbl-personal td:nth-child(3)::before{content:"Rol";}
          .tbl-personal td:nth-child(4)::before{content:"Sucursal";}
          .tbl-personal td:nth-child(5)::before{content:"Teléfono";}
          .tbl-personal td:nth-child(6)::before{content:"Email";}
          .tbl-personal td:nth-child(7)::before{content:"Acciones";}
          .tbl-pagos td:nth-child(1)::before{content:"ID";}
          .tbl-pagos td:nth-child(2)::before{content:"Fecha";}
          .tbl-pagos td:nth-child(3)::before{content:"Cliente";}
          .tbl-pagos td:nth-child(4)::before{content:"Método";}
          .tbl-pagos td:nth-child(5)::before{content:"Monto";}
          .tbl-pagos td:nth-child(6)::before{content:"Referencia";}
          .tbl-pagos td:nth-child(7)::before{content:"Estado";}
          .tbl-pagos td:nth-child(8)::before{content:"Acciones";}
          .tbl-reservas td:nth-child(1)::before{content:"Referencia";}
          .tbl-reservas td:nth-child(2)::before{content:"Comprador";}
          .tbl-reservas td:nth-child(3)::before{content:"Asientos";}
          .tbl-reservas td:nth-child(4)::before{content:"Total";}
          .tbl-reservas td:nth-child(5)::before{content:"Vence";}
          .tbl-reservas td:nth-child(6)::before{content:"Estado";}
          .tbl-reservas td:nth-child(7)::before{content:"Acciones";}
          .tbl-dash-viajes td:nth-child(1)::before{content:"Ruta";}
          .tbl-dash-viajes td:nth-child(2)::before{content:"Bus";}
          .tbl-dash-viajes td:nth-child(3)::before{content:"Salida";}
          .tbl-dash-viajes td:nth-child(4)::before{content:"Precio";}
          .tbl-dash-viajes td:nth-child(5)::before{content:"Estado";}
          .adm-shell input,
          .adm-shell select{font-size:16px !important;}
        }

        @media (max-width:1024px){
          .adm-sidebar{position:fixed;top:0;left:0;z-index:1001;width:278px;box-shadow:0 0 46px rgba(0,0,0,0.35);transform:translateX(-100%);transition:transform 0.24s cubic-bezier(.4,0,.2,1);}
          .adm-sidebar.collapsed{width:278px;}
          .adm-sidebar.open{transform:translateX(0);}
          .adm-sidebar.collapsed .adm-nav-label{display:block;}
          .adm-sidebar.collapsed .adm-nav-item{justify-content:flex-start;padding:10.5px 14px;}
          .adm-sidebar.collapsed .adm-nav-group-title{visibility:visible;height:auto;position:static;}
          .adm-sidebar.collapsed .adm-nav-group-title::after{display:none;}
          .adm-sidebar.collapsed .adm-brand-txt,.adm-sidebar.collapsed .adm-session-txt{display:block;}
          .adm-sidebar.collapsed .adm-session{justify-content:flex-start;}
          .adm-collapse{display:none !important;}
          .adm-hamburger{display:flex !important;}
          .adm-close-mobile{display:flex !important;}
          .adm-backdrop{display:block;position:fixed;inset:0;background:rgba(4,26,24,0.55);z-index:1000;backdrop-filter:blur(2px);}
          .adm-header{padding:0 16px !important;}
          .adm-content{padding:18px 16px !important;}
        }
        @media (max-width:640px){
          .adm-content{padding:13px !important;}
          .adm-date-chip{display:none;}
          .adm-userchip-txt{display:none;}
          .adm-logout{padding:9px 11px;}
          .adm-logout-txt{display:none;}
          .adm-userchip{padding:5px;}
          .adm-sub{max-width:160px;}
          .adm-stat-grid{grid-template-columns:repeat(auto-fill,minmax(150px,1fr));}
          .form-grid{grid-template-columns:1fr;}
          .adm-banner-title{font-size:1.35rem;}
          .adm-banner{padding:20px 18px;}
        }
        @media (max-width:380px){
          .adm-stat-grid{grid-template-columns:1fr 1fr;}
          .adm-stat{padding:14px;}
          .adm-stat-val{font-size:26px;}
          .adm-page-title{font-size:1.35rem;}
        }

        @media (prefers-reduced-motion: reduce){
          .adm-shell *{animation-duration:0.001ms !important;transition-duration:0.001ms !important;scroll-behavior:auto !important;}
        }

        ::-webkit-scrollbar{width:6px;height:6px;}
        ::-webkit-scrollbar-track{background:transparent;}
        ::-webkit-scrollbar-thumb{background:#B9CFCB;border-radius:99px;}
        ::-webkit-scrollbar-thumb:hover{background:#93B1AC;}
      `}</style>
    </div>
  );
}

// ── ADMIN: configuración de tiempos de reserva/pago y QR Yape ────────────────
const LABEL_TIEMPOS = {
  RESERVA_HOLD_MINUTOS:   "Tiempo de reserva (retenida)",
  RESERVA_PENDIENTE_MIN:  "Tiempo para pagar (pendiente)",
  VENTA_WEB_CIERRE_MIN:   "Antelación de cierre de venta web",
  VENTA_YAPE_TOLERANCIA_MIN: "Tolerancia Yape (tiempo de espera en caja)",
};

function HistorialModal({ tipo, rows, onCerrar }) {
  const esYape = tipo === "yape";
  return (
    <div className="modal-overlay" onClick={onCerrar}>
      <div className="modal" style={{ maxWidth: 660 }} onClick={(e)=>e.stopPropagation()}>
        <div className="modal-header"><div className="modal-title">Historial de cambios</div><button className="modal-close" onClick={onCerrar} aria-label="Cerrar"><X size={18}/></button></div>
        <div className="adm-table-wrap" style={{ maxHeight: 420, overflow: "auto" }}>
          <table className="adm-table">
            <thead><tr>
              <th>{esYape ? "Receptor" : "Parámetro"}</th>
              <th>Valor anterior</th>
              <th>Valor nuevo</th>
              <th>Motivo</th>
              <th>Por / fecha</th>
            </tr></thead>
            <tbody>
              {rows.length ? rows.map((h,i)=>(
                <tr key={i}>
                  <td>{esYape ? (h.campo || "configuración") : (h.parametro || "—")}</td>
                  <td style={{fontSize:12}}>{h.valorAnterior || "—"}</td>
                  <td style={{fontSize:12, color:C.verde, fontWeight:700}}>{h.valorNuevo || "—"}</td>
                  <td style={{fontSize:12}}>{h.motivo || "—"}</td>
                  <td style={{fontSize:11}}>{(h.actualizadoPor||"")+(h.rol?` (${h.rol})`:"")}<br/>{fmtFecha(h.fecha)}</td>
                </tr>
              )):(
                <tr><td colSpan={5} className="td-empty">Aún no hay cambios registrados</td></tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

function CrudFlujoVentas({ headers, showToast }) {
  const [tiempos, setTiempos] = useState([]);
  const [edits, setEdits] = useState({});
  const [motivo, setMotivo] = useState("");
  const [yape, setYape] = useState(null);
  const [sucursales, setSucursales] = useState([]);
  const [tap, setTap] = useState("tiempos");
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState({ receptorNombre:"", numeroVisible:"", sucursalId:"", sucursalNombre:"", descripcion:"", instrucciones:"", observaciones:"" });
  const [activo, setActivo] = useState(true);
  const [imagen, setImagen] = useState(null);
  const [imgUrl, setImgUrl] = useState(null);
  const [hist, setHist] = useState(null);

  const cargar = useCallback(() => {
    const get = (u) => fetch(u,{headers}).then(r=>r.ok?r.json():null).catch(()=>null);
    (async () => {
      const [ts, y, ss] = await Promise.all([
        get("/api/v1/ventas/configurar/tiempos"),
        get("/api/v1/ventas/configurar/yape"),
        get("/api/v1/sucursales"),
      ]);
      setTiempos(Array.isArray(ts)?ts:[]);
      if (y) {
        setYape(y);
        setActivo(!!y.activo);
        setForm({ receptorNombre:y.receptorNombre||"", numeroVisible:y.numeroVisible||"", sucursalId:y.sucursalId||"", sucursalNombre:y.sucursalNombre||"", descripcion:y.descripcion||"", instrucciones:y.instrucciones||"", observaciones:y.observaciones||"" });
      }
      setSucursales(Array.isArray(ss)?ss:[]);
    })();
  }, [headers]);

  useEffect(()=>{ cargar(); },[cargar]);

  const guardarTiempos = async () => {
    if (!motivo.trim()) { showToast("Indica un motivo para guardar los parámetros"); return; }
    setSaving(true);
    try {
      for (const t of tiempos) {
        const v = parseInt(edits[t.parametro] ?? t.valor, 10);
        if (Number.isNaN(v) || v===t.valor) continue;
        if (v < t.rangoMin || v > t.rangoMax) { showToast(`${LABEL_TIEMPOS[t.parametro]||t.parametro}: valor fuera de rango (${t.rangoMin}–${t.rangoMax})`); setSaving(false); return; }
        const r = await fetch("/api/v1/ventas/configurar/tiempos",{method:"PUT",headers,body:JSON.stringify({parametro:t.parametro, valor:v, motivo:motivo.trim()})});
        if (!r.ok) { const e = await r.json().catch(()=>null); showToast(e?.message || `No se pudo guardar ${t.parametro}`); setSaving(false); return; }
      }
      setEdits({}); setMotivo("");
      const r = await fetch("/api/v1/ventas/configurar/tiempos",{headers});
      setTiempos(r.ok?await r.json():[]);
      showToast("Parámetros de reserva/pago guardados");
    } catch { showToast("Error al guardar los parámetros"); }
    finally { setSaving(false); }
  };

  const guardarYape = async (e) => {
    e.preventDefault();
    if (!form.receptorNombre?.trim() || !form.numeroVisible?.trim()) { showToast("Nombre del receptor y número son obligatorios"); return; }
    setSaving(true);
    try {
      const fd = new FormData();
      const datos = {
        receptorNombre: form.receptorNombre.trim(),
        numeroVisible: form.numeroVisible.trim(),
        sucursalId: form.sucursalId ? Number(form.sucursalId) : null,
        sucursalNombre: form.sucursalNombre?.trim() || (sucursales.find(s=>String(s.id)===String(form.sucursalId))?.nombre || null),
        descripcion: form.descripcion?.trim() || null,
        instrucciones: form.instrucciones?.trim() || null,
        observaciones: form.observaciones?.trim() || null,
      };
      fd.append("datos", new Blob([JSON.stringify(datos)],{type:"application/json"}));
      if (imagen) fd.append("imagen", imagen);
      const r = await fetch("/api/v1/ventas/configurar/yape",{method:"POST",headers:{Authorization:headers.Authorization,"X-Requested-With":"XMLHttpRequest"},body:fd});
      if (!r.ok) { const e = await r.json().catch(()=>null); showToast(e?.message || "No se pudo guardar la configuración Yape"); return; }
      const ok = await r.json();
      setYape(ok); setActivo(!!ok.activo);
      setImagen(null); setImgUrl(null);
      showToast("Configuración Yape guardada");
    } catch { showToast("Error al guardar la configuración Yape"); }
    finally { setSaving(false); }
  };

  const verHist = async (tipo) => {
    const u = tipo==="yape" ? "/api/v1/ventas/configurar/yape/historial" : "/api/v1/ventas/configurar/tiempos/historial";
    const r = await fetch(u,{headers});
    setHist({ tipo, rows: r.ok?await r.json():[] });
  };

  return (
    <>
      <PageHeader title="Flujo de pagos" sub="Configuración de la reserva temporal, el pago en caja y el QR oficial Yape"
        action={<button className="adm-btn" style={{padding:"0.55rem 1rem"}} onClick={()=>verHist(tap)}><Clock size={15}/> Historial</button>}/>
      <div style={{display:"flex",gap:8,marginBottom:14}}>
        {[{k:"tiempos",l:"Parámetros de reserva/pago"},{k:"yape",l:"QR oficial Yape"}].map(t=>(
          <button key={t.k} onClick={()=>setTap(t.k)} style={{padding:"0.5rem 1.1rem",borderRadius:10,border:`1.5px solid ${tap===t.k?C.verde:C.grisClaro}`,background:tap===t.k?C.verde:"#fff",color:tap===t.k?"#fff":C.gris,fontWeight:700,fontSize:13,cursor:"pointer"}}>
            {t.l}
          </button>
        ))}
      </div>

      {tap==="tiempos" ? (
        <div className="adm-card">
          <div style={{display:"flex",alignItems:"center",gap:8,marginBottom:12}}>
            <Clock size={16} color={C.verde}/><strong style={{color:C.oscuro}}>Duraciones de la reserva y el pago</strong>
          </div>
          <div className="adm-table-wrap">
            <table className="adm-table">
              <thead><tr><th>Parámetro</th><th>Valor</th><th>Rango permitido</th><th>Última actualización</th></tr></thead>
              <tbody>
                {tiempos.map(t=>(
                  <tr key={t.parametro}>
                    <td><strong>{LABEL_TIEMPOS[t.parametro]||t.parametro}</strong><div style={{fontSize:11,color:C.medio}}>{t.parametro}</div></td>
                    <td>
                      <input type="number" min={t.rangoMin} max={t.rangoMax} value={edits[t.parametro] ?? t.valor}
                        onChange={ev=>setEdits(ed=>({...ed,[t.parametro]:ev.target.value}))}
                        style={{width:90,padding:"0.45rem 0.6rem",border:`1.5px solid ${edits[t.parametro]!=null?C.amarillo:C.grisClaro}`,borderRadius:8,fontWeight:700}}/>
                      <span style={{marginLeft:6,color:C.medio,fontSize:12}}>{t.unidad||"min"}</span>
                    </td>
                    <td style={{fontSize:12,color:C.medio}}>{t.rangoMin} – {t.rangoMax}</td>
                    <td style={{fontSize:11.5}}>
                      {t.actualizadoPor ? <>{t.actualizadoPor}<div style={{color:C.medio}}>{fmtFecha(t.actualizadoEn)}</div><div style={{color:C.medio}}>{t.motivo}</div></> : <span style={{color:C.medio}}>Sin cambios (valor por defecto)</span>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div style={{marginTop:14,display:"flex",gap:10,flexWrap:"wrap",alignItems:"flex-end"}}>
            <div style={{flex:1,minWidth:260}}>
              <Field label="Motivo del cambio (obligatorio)">
                <input value={motivo} onChange={e=>setMotivo(e.target.value)} placeholder="Ej.: aumentar el tiempo de la reserva para clientes web"/>
              </Field>
            </div>
            <button className="adm-btn" disabled={saving} onClick={guardarTiempos}>
              <RefreshCw size={14}/> {saving?"Guardando...":"Guardar cambios"}
            </button>
          </div>
        </div>
      ) : (
        <div className="adm-card">
          <div style={{display:"flex",alignItems:"center",gap:8,marginBottom:12,flexWrap:"wrap"}}>
            <Smartphone size={16} color={C.verde}/><strong style={{color:C.oscuro}}>QR oficial Yape que ven los pasajeros</strong>
            <span style={{marginLeft:"auto",display:"flex",alignItems:"center",gap:6,fontSize:12.5,color:C.medio}}>
              <button onClick={()=>setActivo(a=>!a)} style={{position:"relative",width:40,height:22,borderRadius:99,border:"none",cursor:"pointer",background:activo?C.verde:C.grisClaro}}>
                <span style={{position:"absolute",top:2,left:activo?20:2,width:18,height:18,borderRadius:"50%",background:"#fff",transition:"left .15s"}}/>
              </button>
              QR {activo?"visible para los pasajeros":"oculto"}
            </span>
            {yape && yape.vigenteDesde && <span style={{fontSize:11.5,color:C.medio}}>vigente desde {fmtFecha(yape.vigenteDesde)}{yape.vigenteHasta?` hasta ${fmtFecha(yape.vigenteHasta)}`:""}</span>}
          </div>
          <div style={{display:"flex",gap:18,flexWrap:"wrap"}}>
            <div style={{flex:1,minWidth:270,display:"grid",gap:10}}>
              <Field label="Nombre del receptor (titular de la subcuenta Yape) *"><input value={form.receptorNombre} onChange={e=>setForm(f=>({...f,receptorNombre:e.target.value}))} placeholder="Ej.: Intiwatana Viajes S.A.C."/></Field>
              <Field label="Número Yape (se muestra enmascarado) *"><input value={form.numeroVisible} onChange={e=>setForm(f=>({...f,numeroVisible:e.target.value}))} placeholder="Ej.: 912 345 678"/></Field>
              <Field label="Sucursal o caja asociada"><select value={form.sucursalId} onChange={e=>setForm(f=>({...f,sucursalId:e.target.value}))}>
                <option value="">— Sin asignar —</option>
                {sucursales.map(s=>(<option key={s.id} value={s.id}>{s.nombre} ({s.ciudad})</option>))}
              </select></Field>
              <Field label="Nombre de sucursal personalizado"><input value={form.sucursalNombre} onChange={e=>setForm(f=>({...f,sucursalNombre:e.target.value}))} placeholder="Ej.: Agencia Lima Norte"/></Field>
            </div>
            <div style={{flex:1,minWidth:270,display:"grid",gap:10}}>
              <Field label="Descripción corta"><input value={form.descripcion} onChange={e=>setForm(f=>({...f,descripcion:e.target.value}))} placeholder="Ej.: Cuenta principal para pagos de boletos"/></Field>
              <Field label="Instrucciones para el pasajero"><textarea rows={2} value={form.instrucciones} onChange={e=>setForm(f=>({...f,instrucciones:e.target.value}))} placeholder="Ej.: Abre Yape, escanea el QR y confirma el monto exacto."/></Field>
              <Field label="Observaciones internas"><textarea rows={2} value={form.observaciones} onChange={e=>setForm(f=>({...f,observaciones:e.target.value}))} placeholder="Uso interno del administrador"/></Field>
            </div>
          </div>
          <div style={{display:"flex",gap:14,marginTop:14,flexWrap:"wrap",alignItems:"center"}}>
            <label style={{display:"flex",alignItems:"center",gap:10,cursor:"pointer"}}>
              <input type="file" accept="image/*" style={{display:"none"}} onChange={e=>{ const f=e.target.files?.[0]; setImagen(f||null); setImgUrl(f?URL.createObjectURL(f):null); }}/>
              <span style={{display:"flex",alignItems:"center",gap:8,background:"#F2F7F6",border:`1.5px dashed ${C.verde}`,borderRadius:10,padding:"0.55rem 1rem",fontSize:13,color:C.verde,fontWeight:700}}><Upload size={15}/> {imagen?"Reemplazar imagen":(imgUrl||yape?.imagenUrl?"Cambiar imagen del QR":"Subir imagen del QR")}</span>
            </label>
            {(imgUrl || yape?.imagenUrl) && <img src={imgUrl||yape?.imagenUrl} alt="QR Yape" style={{height:66,borderRadius:10,border:"1px solid #DCE8E5",background:"#fff"}}/>}
            <button className="adm-btn" style={{marginLeft:"auto"}} disabled={saving} onClick={guardarYape}>
              <RefreshCw size={14}/> {saving?"Guardando...":"Guardar configuración Yape"}
            </button>
          </div>
          {!activo && <div style={{marginTop:10,fontSize:12,color:C.naranja}}>El QR está oculto; los pasajeros no podrán declarar pagos Yape hasta reactivarlo.</div>}
        </div>
      )}
      {hist && <HistorialModal tipo={hist.tipo} rows={hist.rows} onCerrar={()=>setHist(null)}/>}
    </>
  );
}

// ── CAJERO: bandeja de pagos Yape declarados ─────────────────────────────────
function BandejaPagosYape({ headers, showToast }) {
  const [tab, setTab] = useState("pendientes");
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [rechazo, setRechazo] = useState(null);
  const [motivo, setMotivo] = useState("");
  const [saving, setSaving] = useState(false);
  const [aprobado, setAprobado] = useState(null);

  const cargar = useCallback(async () => {
    setLoading(true);
    try {
      const u = tab==="pendientes" ? "/api/v1/ventas/pago-yape/pendientes" : "/api/v1/ventas/pago-yape/hoy";
      const r = await fetch(u,{headers});
      setRows(r.ok?await r.json():[]);
    } catch { setRows([]); }
    finally { setLoading(false); }
  }, [headers, tab]);

  useEffect(()=>{ cargar(); },[cargar]);

  const aprobar = async (id) => {
    setSaving(true);
    try {
      const r = await fetch(`/api/v1/ventas/pago-yape/${id}/aprobar`,{method:"POST",headers});
      if (!r.ok) { const e = await r.json().catch(()=>null); showToast(e?.message || "No se pudo aprobar el pago"); return; }
      const d = await r.json();
      setAprobado({ fila: rows.find(x=>x.id===id), boletos: d.boletos || [] });
      cargar();
    } catch { showToast("Error al aprobar el pago"); }
    finally { setSaving(false); }
  };

  const confirmarRechazo = async () => {
    if (!motivo.trim()) { showToast("Indica el motivo del rechazo"); return; }
    setSaving(true);
    try {
      const r = await fetch(`/api/v1/ventas/pago-yape/${rechazo}/rechazar`,{method:"POST",headers,body:JSON.stringify({motivo:motivo.trim()})});
      if (!r.ok) { const e = await r.json().catch(()=>null); showToast(e?.message || "No se pudo rechazar la declaración"); return; }
      showToast("Declaración rechazada"); setRechazo(null); setMotivo(""); cargar();
    } catch { showToast("Error al rechazar la declaración"); }
    finally { setSaving(false); }
  };

  return (
    <>
      <PageHeader title="Bandeja Yape" sub="Pagos declarados por los pasajeros; la verificación es exclusiva de caja"
        action={<button className="adm-btn" style={{padding:"0.55rem 1rem"}} onClick={cargar}><RefreshCw size={15}/> Actualizar</button>}/>
      <div style={{display:"flex",gap:8,marginBottom:14}}>
        {[{k:"pendientes",l:"Por verificar"},{k:"hoy",l:"Verificados hoy"}].map(t=>(
          <button key={t.k} onClick={()=>setTab(t.k)} style={{padding:"0.5rem 1.1rem",borderRadius:10,border:`1.5px solid ${tab===t.k?C.verde:C.grisClaro}`,background:tab===t.k?C.verde:"#fff",color:tab===t.k?"#fff":C.gris,fontWeight:700,fontSize:13,cursor:"pointer"}}>
            {t.l}
          </button>
        ))}
      </div>
      <div className="adm-card">
        <TableWrap load={loading} empty={!rows.length ? "No hay pagos Yape en esta vista" : ""}>
          <div className="adm-table-wrap">
            <table className="adm-table">
              <thead><tr>
                <th>Referencia / pasajero</th><th>Viaje</th><th>Asientos</th><th>Total esperado</th>
                <th>Operación</th><th>Declarado</th><th>Comprobante</th><th>Acciones</th>
              </tr></thead>
              <tbody>
                {rows.map(p=>(
                  <tr key={p.id}>
                    <td><strong>{p.referencia}</strong><div style={{fontSize:11.5,color:C.medio}}>{p.pasajero} · {p.documento}</div></td>
                    <td style={{fontSize:12}}>{p.viaje}<div style={{color:C.medio}}>{p.busPlaca} · {fmtFecha(p.fechaHoraSalida)}</div></td>
                    <td style={{fontSize:12}}>{p.asientos}</td>
                    <td><strong style={{color:C.verde}}>S/ {Number(p.totalEsperado||0).toFixed(2)}</strong>{Number(p.importeDeclarado) !== Number(p.totalEsperado) && <div style={{fontSize:11,color:C.rojo}}>declaró S/ {Number(p.importeDeclarado||0).toFixed(2)}</div>}</td>
                    <td><span style={{fontSize:11.5,fontFamily:"Consolas,monospace"}}>{p.operacion}</span>{p.cuentaPagada&&<div style={{fontSize:11,color:C.medio}}>{p.cuentaPagada}</div>}</td>
                    <td style={{fontSize:11.5}}>{fmtFecha(p.declaradoEn)}{p.estado==="PENDIENTE_VERIFICACION" && p.segundosRestante!=null && p.segundosRestante>=0 && <div style={{color:C.naranja}}>{Math.ceil(p.segundosRestante/60)} min de vigencia</div>}</td>
                    <td>{p.comprobanteUrl ? <a href={p.comprobanteUrl} target="_blank" rel="noopener noreferrer" className="btn-edit" title="Ver comprobante"><Search size={14}/></a> : <span style={{fontSize:11,color:C.medio}}>sin adjunto</span>}</td>
                    <td style={{whiteSpace:"nowrap"}}>
                      {tab==="pendientes" ? (
                        <>
                          <button style={{display:"inline-flex",alignItems:"center",gap:5,padding:"0.35rem 0.7rem",borderRadius:8,border:"1px solid rgba(34,197,94,.5)",background:"rgba(34,197,94,.12)",color:"#15803d",fontWeight:700,fontSize:12,cursor:"pointer"}} onClick={()=>aprobar(p.id)} disabled={saving} title="Verificar la transferencia y aprobar"><CheckCircle2 size={15}/> Aprobar</button>
                          <button style={{display:"inline-flex",alignItems:"center",gap:5,padding:"0.35rem 0.7rem",borderRadius:8,border:"1px solid rgba(192,57,43,.5)",background:"rgba(192,57,43,.1)",color:"#c0392b",fontWeight:700,fontSize:12,cursor:"pointer",marginLeft:6}} onClick={()=>setRechazo(p.id)} disabled={saving} title="Rechazar; el pasajero podrá re-declarar"><XCircle size={15}/> Rechazar</button>
                        </>
                      ):(
                        p.estado==="APROBADO" ? <span className="badge-confirmado" style={{fontSize:11}}>Aprobado</span> :
                        <span className="badge-cancelado" style={{fontSize:11}}>{p.motivoRechazo ? "Rechazado" : p.estado}</span>
                      )}
                      {p.estado==="RECHAZADO" && p.motivoRechazo && <div style={{fontSize:10.5,color:C.rojo,maxWidth:180}}>{p.motivoRechazo}</div>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </TableWrap>
      </div>
      {rechazo && (
        <div className="modal-overlay" onClick={()=>setRechazo(null)}>
          <div className="modal" style={{maxWidth:440}} onClick={(e)=>e.stopPropagation()}>
            <div className="modal-header"><div className="modal-title">Rechazar declaración Yape</div><button className="modal-close" onClick={()=>setRechazo(null)} aria-label="Cerrar"><X size={18}/></button></div>
            <p style={{fontSize:13,color:C.gris,margin:"0 0 12px"}}>El pasajero verá el motivo y podrá re-declarar con un nuevo N° de operación.</p>
            <Field label="Motivo del rechazo *"><textarea rows={2} value={motivo} onChange={e=>setMotivo(e.target.value)} placeholder="Ej.: operación ya utilizada o importe no coincide"/></Field>
            <div style={{display:"flex",gap:8,marginTop:14}}>
              <button className="btn-cancelar" style={{flex:1}} onClick={()=>setRechazo(null)}>Cancelar</button>
              <button className="btn-delete-confirm" style={{flex:1}} disabled={saving} onClick={confirmarRechazo}><XCircle size={14}/> Rechazar</button>
            </div>
          </div>
        </div>
      )}
      {aprobado && (
        <div className="modal-overlay" onClick={()=>setAprobado(null)}>
          <div className="modal" style={{maxWidth:440}} onClick={(e)=>e.stopPropagation()}>
            <div className="modal-header"><div className="modal-title"><CheckCircle2 size={18} color={C.verde}/> Pago aprobado</div><button className="modal-close" onClick={()=>setAprobado(null)} aria-label="Cerrar"><X size={18}/></button></div>
            <p style={{fontSize:13,color:C.gris}}>La reserva {aprobado.fila?.referencia} fue cobrada y su viaje emitió los boletos:</p>
            <ul style={{margin:"10px 0 4px",paddingLeft:18,fontSize:13,color:C.oscuro}}>
              {(aprobado.boletos||[]).map((b,i)=>(<li key={i}>{b.numeroBoleto||b.numero||b} · S/ {Number(b.total||b.monto||0).toFixed(2)}</li>))}
            </ul>
            <button className="btn-reservar" style={{width:"100%",marginTop:10}} onClick={()=>setAprobado(null)}>Entendido</button>
          </div>
        </div>
      )}
    </>
  );
}

// ── CAJERO: cierre operativo por viaje (READ-ONLY) ──────────────────────────
function ConsultaCierreViaje({ headers, showToast }) {
  const [viajes, setViajes] = useState([]);
  const [viajeId, setViajeId] = useState("");
  const [cierre, setCierre] = useState(null);
  const [loading, setLoading] = useState(false);

  useEffect(()=>{
    fetch("/api/v1/viajes",{headers}).then(r=>r.ok?r.json():[]).then(d=>setViajes(Array.isArray(d)?d:[])).catch(()=>setViajes([]));
  },[headers]);

  const consultar = async () => {
    if (!viajeId) { showToast("Selecciona un viaje"); return; }
    setLoading(true); setCierre(null);
    try {
      const r = await fetch(`/api/v1/ventas/cierre/viaje/${viajeId}`,{headers});
      if (!r.ok) { const e = await r.json().catch(()=>null); showToast(e?.message || "No se pudo generar el cierre del viaje"); return; }
      setCierre(await r.json());
    } catch { showToast("Error al consultar el cierre del viaje"); }
    finally { setLoading(false); }
  };

  const fila = (l,v)=>(<tr><td style={{color:C.medio}}>{l}</td><td style={{textAlign:"right",fontWeight:700,color:C.oscuro}}>{v}</td></tr>);

  return (
    <>
      <PageHeader title="Cierre por viaje" sub="Resumen operativo derivado de los boletos, reservas y pagos del viaje (solo lectura)"/>
      <div className="adm-card">
        <div style={{display:"flex",gap:10,alignItems:"flex-end",flexWrap:"wrap"}}>
          <div style={{flex:1,minWidth:280}}>
            <Field label="Viaje"><select value={viajeId} onChange={e=>setViajeId(e.target.value)}>
              <option value="">— Selecciona un viaje —</option>
              {viajes.filter(v=>["PROGRAMADO","EN_CURSO","FINALIZADO"].includes(v.estado)).map(v=>(
                <option key={v.id} value={v.id}>{v.ruta?.origen?.ciudad||v.origen} → {v.ruta?.destino?.ciudad||v.destino} · {fmtFecha(v.fechaHoraSalida)} · {v.bus?.placa||v.busPlaca||""}</option>
              ))}
            </select></Field>
          </div>
          <button className="adm-btn" disabled={loading} onClick={consultar}><Navigation size={14}/> {loading?"Consultando...":"Consultar cierre"}</button>
        </div>
        {cierre && (
          <div style={{marginTop:16}}>
            <div style={{display:"flex",justifyContent:"space-between",alignItems:"center",flexWrap:"wrap",gap:8,marginBottom:12}}>
              <strong style={{color:C.oscuro}}>{cierre.origen} → {cierre.destino}</strong>
              <span style={{fontSize:12,color:C.medio}}>{cierre.busPlaca} · {fmtFecha(cierre.fechaHoraSalida)} · generado {fmtFecha(cierre.generadoEn)}</span>
            </div>
            {cierre.deficitario && (
              <div style={{display:"flex",gap:8,alignItems:"center",background:"#FBF8EE",border:"1px solid rgba(213,139,21,.4)",borderRadius:10,padding:"0.6rem 0.9rem",marginBottom:12,fontSize:12.5,color:C.naranja}}>
                <AlertTriangle size={16}/> Hay declaraciones Yape sin verificar o reservas sin cobrar: los pendientes NO se suman a lo cobrado. Verifica la bandeja Yape y cobra las reservas pendientes antes de cerrar.
              </div>
            )}
            <div style={{display:"grid",gridTemplateColumns:"repeat(auto-fit,minmax(150px,1fr))",gap:10,marginBottom:12}}>
              <div className="adm-stat"><div className="adm-stat-icon" style={{color:C.verde,background:"#EAF4F1"}}><Armchair size={17}/></div><div className="adm-stat-val">{cierre.asientosVendidos||0}<span style={{fontSize:12,color:C.medio,fontWeight:600}}>/{cierre.capacidadAsientos}</span></div><div className="adm-stat-label">Asientos vendidos</div></div>
              <div className="adm-stat"><div className="adm-stat-icon" style={{color:C.verde,background:"#EAF4F1"}}><Ticket size={17}/></div><div className="adm-stat-val">{cierre.boletosActivos||0}</div><div className="adm-stat-label">Boletos activos</div></div>
              <div className="adm-stat"><div className="adm-stat-icon" style={{color:C.verde,background:"#EAF4F1"}}><Users size={17}/></div><div className="adm-stat-val">{cierre.pasajerosConfirmados||0}</div><div className="adm-stat-label">Pasajeros confirmados</div></div>
              <div className="adm-stat" style={{borderColor:cierre.deficitario?C.amarillo:"var(--iwr-borde, #DCE8E5)"}}><div className="adm-stat-icon" style={{color:C.naranja,background:"#FBF2DC"}}><Wallet size={17}/></div><div className="adm-stat-val" style={{color:C.verde}}>S/ {Number(cierre.totalCobrado||0).toFixed(2)}</div><div className="adm-stat-label">Total cobrado</div></div>
            </div>
            <div className="adm-table-wrap">
              <table className="adm-table" style={{maxWidth:520}}>
                <tbody>
                  {fila("Efectivo", `S/ ${Number(cierre.totalEfectivo||0).toFixed(2)}`)}
                  {fila("Yape verificado", `S/ ${Number(cierre.totalYape||0).toFixed(2)}`)}
                  {fila("Otros métodos", `S/ ${Number(cierre.totalOtrosMetodos||0).toFixed(2)}`)}
                  {fila("Total cobrado", `S/ ${Number(cierre.totalCobrado||0).toFixed(2)}`)}
                  {fila("Reservas pendientes de pago", cierre.reservasPendientes||0)}
                  {fila("Reservas retenidas (sin crear)", cierre.reservasRetenidas||0)}
                  {fila("Reservas vencidas", cierre.reservasVencidas||0)}
                  {fila("Reservas liberadas", cierre.reservasLiberadas||0)}
                  {fila("Declaraciones Yape sin verificar", cierre.declaracionesPendientes||0)}
                  {fila("Declaraciones rechazadas", cierre.declaracionesRechazadas||0)}
                  {fila("Total pendiente de pago", `S/ ${Number(cierre.totalPendientePago||0).toFixed(2)}`)}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </div>
    </>
  );
}
