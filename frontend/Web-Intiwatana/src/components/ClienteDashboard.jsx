import { useState, useEffect, useRef, useCallback, useMemo } from "react";
import { useSearchParams } from "react-router-dom";
import { useAuth } from "../context/useAuth";
import LogoSVG from "./LogoSVG";
import { Ticket, Package, UserCircle, LogOut } from "lucide-react";

const C = {
  oscuro:  "#0f3d38",
  verde:   "#127369",
  medio:   "#6fa89e",
  amarillo:"#f5c518",
  fondo:   "#f0f4f3",
  blanco:  "#ffffff",
  borde:   "rgba(18,115,105,0.12)",
};

const ESTADO_BOLETO = {
  ACTIVO:      { label: "Activo",      cls: "badge-confirmado" },
  USADO:       { label: "Usado",       cls: "badge-entregado"  },
  CANCELADO:   { label: "Cancelado",   cls: "badge-cancelado"  },
  REEMBOLSADO: { label: "Reembolsado", cls: "badge-devuelto"   },
};

const ESTADO_ENCOMIENDA = {
  SOLICITUD_PENDIENTE:{ label: "Solicitud pendiente", cls: "badge-pendiente" },
  RECIBIDO:      { label: "Recibido",      cls: "badge-recibido"  },
  EN_ALMACEN:    { label: "En almacén",    cls: "badge-pendiente" },
  CARGADO_EN_BUS:{ label: "Cargado en bus",cls: "badge-transito"  },
  EN_TRANSITO:   { label: "En tránsito",   cls: "badge-transito"  },
  LLEGADO_A_DESTINO:{ label: "Llegó a destino", cls: "badge-destino" },
  EN_DESTINO:    { label: "En destino",    cls: "badge-destino"   },
  LISTO_ENTREGA: { label: "Listo entrega", cls: "badge-transito"  },
  ENTREGADO:     { label: "Entregado",     cls: "badge-entregado" },
  DEVUELTO:      { label: "Devuelto",      cls: "badge-devuelto"  },
  PERDIDO:       { label: "Perdido",       cls: "badge-cancelado" },
};

const ESTADO_VIAJE = {
  PROGRAMADO: { label: "Programado", cls: "badge-pendiente" },
  EN_CURSO:   { label: "En curso",   cls: "badge-transito"  },
  FINALIZADO: { label: "Finalizado", cls: "badge-entregado" },
  CANCELADO:  { label: "Cancelado",  cls: "badge-cancelado" },
};

function EstadoBadge({ estado, mapa }) {
  const cfg = (mapa && mapa[estado]) || { label: estado || "—", cls: "badge-pendiente" };
  return <span className={`estado-badge ${cfg.cls}`}>{cfg.label}</span>;
}

function fmtFecha(iso) {
  if (!iso) return "—";
  return new Date(iso).toLocaleString("es-PE", {
    day: "2-digit", month: "short", year: "numeric",
    hour: "2-digit", minute: "2-digit",
  });
}

function puedeCancelar(fechaHoraSalida) {
  if (!fechaHoraSalida) return false;
  return new Date(fechaHoraSalida).getTime() - Date.now() > 30 * 60 * 1000;
}

function Toast({ msg, tipo }) {
  if (!msg) return null;
  return (
    <div style={{
      position: "fixed", bottom: 24, right: 24, zIndex: 9999,
      background: tipo === "error" ? "#dc2626" : C.verde,
      color: "#fff", borderRadius: 10, padding: "12px 20px",
      fontSize: 14, fontWeight: 600, boxShadow: "0 4px 20px rgba(0,0,0,0.2)",
    }}>
      {msg}
    </div>
  );
}

function ModalCancelar({ boleto, onConfirm, onClose, loading }) {
  const [motivo, setMotivo] = useState("");

  // Los hooks van antes de cualquier return temprano para respetar el orden.
  const fecha = boleto?.fecha_hora_salida || boleto?.viaje?.fechaHoraSalida;
  const faltan = () => (fecha ? new Date(fecha).getTime() - Date.now() : 0);
  const [msFaltantes, setMsFaltantes] = useState(faltan);
  const [fechaVista, setFechaVista] = useState(fecha);

  // Si el boleto cambia, el contador se recalcula en el propio render con el
  // patrón oficial de React, en vez de provocar un render extra desde un efecto.
  if (fechaVista !== fecha) {
    setFechaVista(fecha);
    setMsFaltantes(faltan());
  }

  // La cuenta regresiva baja cada segundo; el setState ocurre dentro del
  // temporizador, nunca de forma síncrona al montar el efecto.
  useEffect(() => {
    if (!fecha) return;
    const id = setInterval(
      () => setMsFaltantes(new Date(fecha).getTime() - Date.now()),
      1000
    );
    return () => clearInterval(id);
  }, [fecha]);

  if (!boleto) return null;
  const num     = boleto.numero_boleto || boleto.numeroBoleto;
  const origen  = boleto.ciudad_origen  || "—";
  const destino = boleto.ciudad_destino || "—";

  const esReembolsable = msFaltantes > 24 * 60 * 60 * 1000;
  const requiereJustif = !esReembolsable && msFaltantes > 30 * 60 * 1000;

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal cli-modal" onClick={e => e.stopPropagation()} style={{ maxWidth: 420 }}>
        <div className="modal-header">
          <div className="modal-badge" style={{ background: "rgba(220,38,38,0.1)", color: "#dc2626" }}>
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/>
            </svg>
          </div>
          <div>
            <div className="modal-title">Cancelar boleto</div>
            <div className="modal-subtitle">{origen} → {destino}</div>
          </div>
          <button className="modal-close" onClick={onClose}>✕</button>
        </div>
        <div className="modal-body">
          <p style={{ color: C.verde, fontSize: 14, marginBottom: "1rem", lineHeight: 1.6 }}>
            ¿Estás seguro de cancelar el boleto <strong style={{ fontFamily: "monospace" }}>{num}</strong>?
          </p>
          
          {esReembolsable ? (
            <div style={{ background: "#e1f5fe", border: "1px solid #03a9f466", borderRadius: 10, padding: "10px 14px", fontSize: 13, color: "#0277bd", marginBottom: "1rem", display:"flex", alignItems:"center", gap:8 }}>
              <div style={{fontSize:18}}>💰</div>
              <div><strong>¡Reembolso disponible!</strong> Al faltar más de 24h, el pago se marcará como reembolsado automáticamente.</div>
            </div>
          ) : (
            <div style={{ background: "#fff9e6", border: "1px solid rgba(245,197,24,0.4)", borderRadius: 10, padding: "10px 14px", fontSize: 13, color: "#7a6000", marginBottom: "1rem" }}>
              <strong>Nota:</strong> Faltan menos de 24h para la salida. <strong>Debe justificar</strong> su cancelación y no se garantiza el reembolso automático.
            </div>
          )}

          <div style={{ marginBottom: "1.5rem" }}>
            <label style={{ fontSize: 12, fontWeight: 700, color: C.gris, display: "block", marginBottom: 6 }}>
              Motivo / Justificación {requiereJustif && <span style={{color:C.rojo}}>*</span>}
            </label>
            <textarea 
              value={motivo} 
              onChange={e => setMotivo(e.target.value)}
              placeholder={esReembolsable ? "Opcional: ¿Por qué cancelas?" : "Obligatorio: Por qué cancelas a última hora..."}
              style={{ width: "100%", borderRadius: 8, border: `1.5px solid ${C.borde}`, padding: "10px", fontSize: 13, fontFamily: "inherit", resize: "none", height: 60 }}
            />
          </div>
          <div style={{ display: "flex", gap: 10, justifyContent: "flex-end" }}>
            <button onClick={onClose} disabled={loading} style={{ padding: "10px 20px", borderRadius: 8, border: `1.5px solid ${C.borde}`, background: "transparent", color: C.verde, cursor: "pointer", fontWeight: 600, fontSize: 14 }}>
              Volver
            </button>
            <button 
              onClick={() => onConfirm(motivo)} 
              disabled={loading || (requiereJustif && !motivo.trim())} 
              style={{ padding: "10px 22px", borderRadius: 8, border: "none", background: "#dc2626", color: "#fff", cursor: (loading || (requiereJustif && !motivo.trim())) ? "not-allowed" : "pointer", fontWeight: 700, fontSize: 14, opacity: (loading || (requiereJustif && !motivo.trim())) ? 0.7 : 1 }}
            >
              {loading ? "Cancelando..." : "Confirmar cancelación"}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

function TrackingModal({ encomienda, movimientos, loadingMov, error, onClose }) {
  if (!encomienda) return null;
  const PASOS = ["RECIBIDO","EN_ALMACEN","CARGADO_EN_BUS","EN_TRANSITO","LLEGADO_A_DESTINO","EN_DESTINO","LISTO_ENTREGA","ENTREGADO"];
  const idxActual = PASOS.indexOf(encomienda.estado);
  const numGuia  = encomienda.numero_guia || encomienda.numeroGuia;
  const refSol   = encomienda.referencia_solicitud || encomienda.referenciaSolicitud;
  const desc     = encomienda.descripcion_contenido || encomienda.descripcionContenido;
  const pesoKg   = encomienda.peso_kg ?? encomienda.pesoKg;
  const cOrigen  = encomienda.ciudad_origen || encomienda.sucursalOrigen?.ciudad;
  const cDestino = encomienda.ciudad_destino || encomienda.sucursalDestino?.ciudad;
  const sOrigen  = encomienda.sucursal_origen || encomienda.sucursalOrigen?.nombre;
  const sDestino = encomienda.sucursal_destino || encomienda.sucursalDestino?.nombre;
  const destNom  = encomienda.destinatarioNombre || [encomienda.destinatario_nombres||encomienda.destinatario?.nombres, encomienda.destinatario_apellidos||encomienda.destinatario?.apellidos].filter(Boolean).join(" ");
  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal cli-modal" onClick={e => e.stopPropagation()}>
        <div className="modal-header">
          <div className="modal-badge"><Package size={20}/></div>
          <div>
            <div className="modal-title">Guía {numGuia||refSol||`#${encomienda.id}`}</div>
            <div className="modal-subtitle">{desc}{pesoKg!=null?` · ${pesoKg} kg`:""}</div>
          </div>
          <button className="modal-close" onClick={onClose} aria-label="Cerrar">✕</button>
        </div>
        <div className="modal-body">
          <div style={{ textAlign:"center", marginBottom:"1.5rem" }}>
            <EstadoBadge estado={encomienda.estado} mapa={ESTADO_ENCOMIENDA}/>
          </div>
          <div className="track-progress">
            {PASOS.map((paso,i)=>{
              const done=i<=idxActual&&idxActual>=0; const actual=i===idxActual;
              const cfg=ESTADO_ENCOMIENDA[paso]||{};
              return (
                <div className={`track-step ${done?"done":""} ${actual?"current":""}`} key={paso}>
                  <div className="track-dot">{done?"✓":"○"}</div>
                  <div className="track-label">{cfg.label||paso}</div>
                  {i<PASOS.length-1&&<div className={`track-line ${done&&i<idxActual?"done":""}`}/>}
                </div>
              );
            })}
          </div>
          <hr className="modal-divider"/>
          <div className="track-info-grid">
            <div className="track-info-item"><span className="track-info-label">Origen</span><span className="track-info-value">{cOrigen||"—"}</span>{sOrigen&&<span style={{fontSize:"11px",color:C.medio}}>{sOrigen}</span>}</div>
            <div className="track-info-item"><span className="track-info-label">Destino</span><span className="track-info-value">{cDestino||"—"}</span>{sDestino&&<span style={{fontSize:"11px",color:C.medio}}>{sDestino}</span>}</div>
            <div className="track-info-item"><span className="track-info-label">Destinatario</span><span className="track-info-value">{destNom||"—"}</span></div>
            <div className="track-info-item"><span className="track-info-label">Tarifa</span><span className="track-info-value">{encomienda.costo?`S/ ${encomienda.costo}`:"Se define en agencia"}</span></div>
          </div>
          <hr className="modal-divider"/>
          <div className="modal-section-label">Historial de movimientos</div>
          {loadingMov?(<div className="cli-loading" style={{padding:"1rem"}}>Cargando...</div>):error?(<p style={{color:C.rojo,fontSize:"13px"}}>{error}</p>):movimientos.length===0?(<p style={{color:C.medio,fontSize:"13px"}}>Sin movimientos registrados aún.</p>):(
            <div className="timeline">
              {movimientos.map((m,i)=>{
                const estadoNuevo=m.estado_nuevo||m.estadoNuevo; const sucNombre=m.sucursal_nombre||m.sucursalNombre||m.sucursalActual?.nombre;
                const fechaHora=m.fecha_hora||m.fechaHora; const obs=m.observacion; const cfg=ESTADO_ENCOMIENDA[estadoNuevo]||{};
                return(<div className={`timeline-item ${i===0?"active":""}`} key={m.id||i}><div className="timeline-dot"/><div className="timeline-text"><strong>{cfg.label||estadoNuevo}</strong>{sucNombre&&<span> — {sucNombre}</span>}<br/><span style={{fontSize:"11px",color:C.verde}}>{fmtFecha(fechaHora)}</span>{obs&&<p style={{margin:"3px 0 0",fontSize:"12px",color:C.medio}}>{obs}</p>}</div></div>);
              })}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function HistorialBoletos({ boletos, loading, headers, onBoletosCambiados }) {
  const [modalCancelar, setModalCancelar] = useState(null);
  const [cancelando,    setCancelando]    = useState(false);
  const [toast,         setToast]         = useState(null);
  const toastTimer = useRef(null);

  const showToast = (msg, tipo="ok") => {
    if (toastTimer.current) clearTimeout(toastTimer.current);
    setToast({ msg, tipo });
    toastTimer.current = setTimeout(()=>setToast(null), 3500);
  };

  const confirmarCancelacion = async (motivo) => {
    if (!modalCancelar) return;
    setCancelando(true);
    try {
      const res = await fetch(`/api/v1/boletos/${modalCancelar.id}/cancelar?motivo=${encodeURIComponent(motivo)}`, { 
        method: "PATCH", // Cambiado a PATCH según el backend
        headers 
      });
      if (res.ok) {
        showToast("Boleto cancelado exitosamente");
        setModalCancelar(null);
        onBoletosCambiados();
      } else {
        const err = await res.json().catch(()=>({}));
        showToast(err.mensaje || err.message || "No se pudo cancelar el boleto", "error");
      }
    } catch { showToast("Error de conexión al cancelar", "error"); }
    finally { setCancelando(false); }
  };

  if (loading) return <div className="cli-loading">Cargando historial de viajes...</div>;
  if (!boletos.length) return (
    <div className="cli-empty">
      <Ticket size={40} color={C.medio} strokeWidth={1.5}/>
      <p>Aún no tienes boletos registrados</p>
      <small>Cuando compres un pasaje, aparecerá aquí</small>
    </div>
  );

  const ahora = Date.now();
  const proximos  = boletos.filter(b => {
    const f = new Date(b.fecha_hora_salida || b.viaje?.fechaHoraSalida).getTime();
    return (b.estado === "ACTIVO" || b.estado === "CONFIRMADO") && 
           (b.estado_viaje === "PROGRAMADO" || b.viaje?.estado === "PROGRAMADO") && 
           f > ahora;
  });
  const enCurso   = boletos.filter(b => {
    const f = new Date(b.fecha_hora_salida || b.viaje?.fechaHoraSalida).getTime();
    const esEstadoCurso = b.estado_viaje === "EN_CURSO" || b.viaje?.estado === "EN_CURSO";
    return (b.estado === "ACTIVO" || b.estado === "CONFIRMADO") && (esEstadoCurso || (f <= ahora && f + 4*3600000 > ahora));
  });
  const historial = boletos.filter(b => !proximos.find(p => p.id === b.id) && !enCurso.find(e => e.id === b.id));

  const renderCard = (b) => {
    const numeroBoleto  = b.numero_boleto  || b.numeroBoleto;
    const precioPagado  = b.precio_pagado  || b.precioPagado;
    const estado        = b.estado;
    const ciudadOrigen  = b.ciudad_origen  || b.viaje?.ruta?.origen?.ciudad;
    const ciudadDestino = b.ciudad_destino || b.viaje?.ruta?.destino?.ciudad;
    const fechaSalida   = b.fecha_hora_salida || b.viaje?.fechaHoraSalida;
    const numAsiento    = b.numero_asiento || b.asiento?.numeroAsiento;
    const estadoViaje   = b.estado_viaje   || b.viaje?.estado;
    const cancelable    = estado==="ACTIVO" && estadoViaje==="PROGRAMADO" && puedeCancelar(fechaSalida);

    return (
      <div className="cli-card" key={b.id}>
        <div className="cli-card-top">
          <div className="cli-card-ruta">
            <span className="cli-card-ciudad">{ciudadOrigen||"—"}</span>
            <span className="cli-card-arrow">→</span>
            <span className="cli-card-ciudad">{ciudadDestino||"—"}</span>
          </div>
          <EstadoBadge estado={estado} mapa={ESTADO_BOLETO}/>
        </div>
        <div className="cli-card-body">
          <div className="cli-card-row"><span>Salida</span><span>{fmtFecha(fechaSalida)}</span></div>
          <div className="cli-card-row"><span>Asiento</span><span>{numAsiento||"—"}</span></div>
          <div className="cli-card-row"><span>Precio pagado</span><span>S/ {precioPagado||"—"}</span></div>
          <div className="cli-card-row"><span>N° Boleto</span><span className="cli-mono">{numeroBoleto||"—"}</span></div>
          {estadoViaje&&(
            <div className="cli-card-row"><span>Estado del viaje</span><EstadoBadge estado={estadoViaje} mapa={ESTADO_VIAJE}/></div>
          )}
        </div>
        {cancelable && (
          <div style={{ padding: "0 1.2rem 1.2rem" }}>
            <button
              onClick={() => setModalCancelar(b)}
              style={{ width:"100%", padding:"9px 0", borderRadius:8, border:"1.5px solid rgba(220,38,38,0.35)", background:"rgba(220,38,38,0.06)", color:"#dc2626", cursor:"pointer", fontWeight:700, fontSize:13, fontFamily:"inherit", transition:"background 0.18s" }}
              onMouseEnter={e=>e.currentTarget.style.background="rgba(220,38,38,0.14)"}
              onMouseLeave={e=>e.currentTarget.style.background="rgba(220,38,38,0.06)"}
            >
              Cancelar boleto
            </button>
          </div>
        )}
      </div>
    );
  };

  return (
    <>
      <div style={{ display: "flex", flexDirection: "column", gap: "2.5rem" }}>
        {proximos.length > 0 && (
          <section>
            <div style={{ fontSize: 16, fontWeight: 800, color: C.oscuro, marginBottom: 14, display: "flex", alignItems:"center", gap:10 }}>
              <div style={{width:10,height:10,borderRadius:"50%",background:C.verde}}></div>
              VIAJES PRÓXIMOS
              <span style={{fontSize:11,background:C.verde+"15",color:C.verde,padding:"2px 8px",borderRadius:50}}>{proximos.length}</span>
            </div>
            <div className="cli-cards">{proximos.map(renderCard)}</div>
          </section>
        )}

        {enCurso.length > 0 && (
          <section>
            <div style={{ fontSize: 16, fontWeight: 800, color: C.oscuro, marginBottom: 14, display: "flex", alignItems:"center", gap:10 }}>
              <div style={{width:10,height:10,borderRadius:"50%",background:C.amarillo,animation:"pulse 1.5s infinite"}}></div>
              EN VIAJE AHORA
              <span style={{fontSize:11,background:C.amarillo+"25",color:"#9a7a00",padding:"2px 8px",borderRadius:50}}>{enCurso.length}</span>
            </div>
            <div className="cli-cards">{enCurso.map(renderCard)}</div>
          </section>
        )}

        {historial.length > 0 && (
          <section>
            <div style={{ fontSize: 16, fontWeight: 800, color: C.medio, marginBottom: 14, display: "flex", alignItems:"center", gap:10 }}>
              <div style={{width:10,height:10,borderRadius:"50%",background:C.medio}}></div>
              HISTORIAL Y FINALIZADOS
              <span style={{fontSize:11,background:C.medio+"15",color:C.medio,padding:"2px 8px",borderRadius:50}}>{historial.length}</span>
            </div>
            <div className="cli-cards">{historial.map(renderCard)}</div>
          </section>
        )}
      </div>

      <style>{`
        @keyframes pulse { 0% { transform: scale(0.9); opacity: 1; } 70% { transform: scale(1.2); opacity: 0.7; } 100% { transform: scale(0.9); opacity: 1; } }
      `}</style>

      <ModalCancelar boleto={modalCancelar} onConfirm={confirmarCancelacion} onClose={()=>!cancelando&&setModalCancelar(null)} loading={cancelando}/>
      {toast && <Toast msg={toast.msg} tipo={toast.tipo}/>}
    </>
  );
}

function PanelEncomiendas({ encomiendas, loading, headers }) {
  const [tracking,    setTracking]    = useState(null);
  const [movimientos, setMovimientos] = useState([]);
  const [loadingMov,  setLoadingMov]  = useState(false);
  const [errorMov,    setErrorMov]    = useState(null);
  const [buscar,      setBuscar]      = useState("");

  const filtradas = encomiendas.filter((e)=>{
    const guia=(e.numero_guia||e.numeroGuia||"").toLowerCase();
    const ref=(e.referencia_solicitud||e.referenciaSolicitud||"").toLowerCase();
    const desc=(e.descripcion_contenido||e.descripcionContenido||"").toLowerCase();
    const q=buscar.toLowerCase();
    return guia.includes(q)||ref.includes(q)||desc.includes(q);
  });

  const verTracking = async (enc) => {
    setTracking(enc); setMovimientos([]); setLoadingMov(true); setErrorMov(null);
    try {
      const res = await fetch(`/api/v1/encomiendas/${enc.id}/movimientos`,{headers});
      if (res.ok) { const data=await res.json(); setMovimientos(Array.isArray(data)?data:[]); }
      else { setErrorMov("No se pudieron cargar los movimientos de la encomienda."); }
    } catch { setErrorMov("No se pudo conectar con el servidor para ver el tracking."); }
    finally { setLoadingMov(false); }
  };

  if (loading) return <div className="cli-loading">Cargando encomiendas...</div>;
  return (
    <>
      <div className="cli-search-row">
        <input className="cli-search" placeholder="Buscar por N° de guía o contenido..." value={buscar} onChange={e=>setBuscar(e.target.value)}/>
      </div>
      {!filtradas.length?(
        <div className="cli-empty">
          <Package size={40} color={C.medio} strokeWidth={1.5}/>
          <p>No se encontraron encomiendas</p>
          <small>Tus envíos como remitente aparecerán aquí</small>
        </div>
      ):(
        <div className="cli-cards">
          {filtradas.map((enc)=>{
            const guia=enc.numero_guia||enc.numeroGuia;
            const ref=enc.referencia_solicitud||enc.referenciaSolicitud;
            const pendiente=enc.estado==="SOLICITUD_PENDIENTE";
            const desc=enc.descripcion_contenido||enc.descripcionContenido;
            const peso=enc.peso_kg??enc.pesoKg;
            const origen=enc.ciudad_origen||enc.sucursalOrigen?.ciudad;
            const destino=enc.ciudad_destino||enc.sucursalDestino?.ciudad;
            const destNom=enc.destinatarioNombre||[enc.destinatario_nombres||enc.destinatario?.nombres,enc.destinatario_apellidos||enc.destinatario?.apellidos].filter(Boolean).join(" ");
            const ultimaAct=enc.ultima_actualizacion||enc.ultimaActualizacion;
            return (
              <div className="cli-card" key={enc.id}>
                <div className="cli-card-top">
                  <span className="cli-mono">{guia||ref||`#${enc.id}`}</span>
                  <EstadoBadge estado={enc.estado} mapa={ESTADO_ENCOMIENDA}/>
                </div>
                {pendiente&&(
                  <div className="cli-card-row" style={{paddingTop:6}}>
                    <span style={{fontSize:"12px",color:"#b45309"}}>
                      Solicitud previa: aún no hay guía oficial. Presenta el paquete en la agencia.
                    </span>
                  </div>
                )}
                <div className="cli-card-body">
                  <div className="cli-card-row"><span>Contenido</span><span style={{textAlign:"right",maxWidth:"55%"}}>{desc||"—"}</span></div>
                  <div className="cli-card-row"><span>Peso</span><span>{peso!=null?`${peso} kg`:"Se mide en agencia"}</span></div>
                  <div className="cli-card-row"><span>Ruta</span><span>{origen||"—"} → {destino||"—"}</span></div>
                  <div className="cli-card-row"><span>Destinatario</span><span>{destNom||"—"}</span></div>
                  <div className="cli-card-row"><span>Tarifa</span><span>{pendiente?"Se define en agencia":`S/ ${enc.costo||"—"}`}</span></div>
                  {ultimaAct&&<div className="cli-card-row"><span>Última actualización</span><span style={{fontSize:"12px"}}>{fmtFecha(ultimaAct)}</span></div>}
                </div>
                {pendiente
                  ?<button className="cli-track-btn" disabled style={{opacity:.6,cursor:"not-allowed"}}>Pendiente de recepción en agencia</button>
                  :<button className="cli-track-btn" onClick={()=>verTracking(enc)}>Ver seguimiento</button>}
              </div>
            );
          })}
        </div>
      )}
      <TrackingModal encomienda={tracking} movimientos={movimientos} loadingMov={loadingMov} error={errorMov} onClose={()=>setTracking(null)}/>
    </>
  );
}

const MENU = [
  { key:"boletos",     icon:<Ticket size={19}/>,     label:"Mis viajes"  },
  { key:"encomiendas", icon:<Package size={19}/>,    label:"Encomiendas" },
  { key:"perfil",      icon:<UserCircle size={19}/>, label:"Mi perfil"   },
];

export default function ClienteDashboard() {
  const { session, logout } = useAuth();
  const [searchParams] = useSearchParams();
  const seccionParam = searchParams.get("seccion");
  const [seccion,     setSeccion]     = useState(seccionParam === "encomiendas" || seccionParam === "perfil" ? seccionParam : "boletos");
  const [boletos,     setBoletos]     = useState([]);
  const [encomiendas, setEncomiendas] = useState([]);
  const [loadBoletos, setLoadBoletos] = useState(true);
  const [loadEncom,   setLoadEncom]   = useState(false);
  const [perfil,      setPerfil]      = useState(null);

  const headers = useMemo(
    () => ({ "Content-Type":"application/json", Authorization:`Bearer ${session.token}` }),
    [session.token]
  );

  // Refresco manual, por ejemplo tras cancelar un boleto.
  const cargarBoletos = useCallback(() => {
    setLoadBoletos(true);
    fetch("/api/v1/boletos/mis-boletos",{headers})
      .then(r=>r.ok?r.json():[]).then(d=>setBoletos(Array.isArray(d)?d:[]))
      .catch(()=>setBoletos([])).finally(()=>setLoadBoletos(false));
  }, [headers]);

  const cargarEncomiendas = useCallback(() => {
    if (encomiendas.length) return;
    setLoadEncom(true);
    fetch("/api/v1/encomiendas/mis-encomiendas",{headers})
      .then(r=>r.ok?r.json():[]).then(d=>setEncomiendas(Array.isArray(d)?d:[]))
      .catch(()=>setEncomiendas([])).finally(()=>setLoadEncom(false));
  }, [encomiendas.length, headers]);

  // Carga inicial. Las peticiones se lanzan aquí y actualizan el estado solo
  // dentro de sus callbacks, y se ignoran si el componente ya se desmontó.
  useEffect(()=>{
    let vigente = true;
    fetch("/api/v1/clientes/mi-perfil",{headers})
      .then(r=>r.ok?r.json():null)
      .then(d=>{ if (vigente && d) setPerfil(d); })
      .catch(()=>{});
    fetch("/api/v1/boletos/mis-boletos",{headers})
      .then(r=>r.ok?r.json():[])
      .then(d=>{ if (vigente) setBoletos(Array.isArray(d)?d:[]); })
      .catch(()=>{ if (vigente) setBoletos([]); })
      .finally(()=>{ if (vigente) setLoadBoletos(false); });
    return ()=>{ vigente = false; };
  },[headers]);

  // Permite abrir directamente "Mis encomiendas" desde el portal (?seccion=encomiendas).
  useEffect(() => {
    if (seccion === "encomiendas") cargarEncomiendas();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [seccion]);

  const cambiarSeccion = (s) => { setSeccion(s); if(s==="encomiendas") cargarEncomiendas(); };
  const iniciales = (session.nombreCompleto||session.username||"CL").split(" ").map(w=>w[0]).join("").slice(0,2).toUpperCase();

  return (
    <div style={{ display:"flex", height:"100vh", overflow:"hidden", background:C.fondo, fontFamily:"'Source Sans 3', Arial, sans-serif" }}>

      {/* SIDEBAR */}
      <aside style={{ width:228, background:C.oscuro, display:"flex", flexDirection:"column", height:"100vh", flexShrink:0, overflow:"hidden" }}>
        <div style={{ padding:"18px 14px 14px", borderBottom:"1px solid rgba(255,255,255,0.06)", display:"flex", alignItems:"center", gap:10 }}>
          <LogoSVG width={34} height={34}/>
          <div>
            <div style={{ fontFamily:"'Playfair Display',serif", color:C.amarillo, fontSize:13, fontWeight:700, lineHeight:1 }}>INTIWATANA</div>
            <div style={{ color:C.medio, fontSize:10, letterSpacing:"0.08em", textTransform:"uppercase" }}>Mi cuenta</div>
          </div>
        </div>

        <nav style={{ flex:1, padding:"10px 8px", display:"flex", flexDirection:"column", gap:2, overflowY:"auto" }}>
          {MENU.map(({key,icon,label})=>(
            <button key={key} onClick={()=>cambiarSeccion(key)}
              style={{ width:"100%", background:seccion===key?"rgba(245,197,24,0.14)":"transparent", border:seccion===key?"1.5px solid rgba(245,197,24,0.28)":"1.5px solid transparent", borderRadius:10, color:seccion===key?C.amarillo:C.medio, padding:"10px 13px", cursor:"pointer", display:"flex", alignItems:"center", gap:10, fontSize:13, fontWeight:600, textAlign:"left", transition:"all 0.18s", fontFamily:"inherit" }}
              onMouseEnter={e=>{if(seccion!==key)e.currentTarget.style.background="rgba(255,255,255,0.05)";}}
              onMouseLeave={e=>{if(seccion!==key)e.currentTarget.style.background="transparent";}}>
              <span style={{flexShrink:0}}>{icon}</span>
              <span>{label}</span>
            </button>
          ))}
        </nav>

        <div style={{ padding:"10px 10px 14px", borderTop:"1px solid rgba(255,255,255,0.06)", display:"flex", alignItems:"center", gap:9 }}>
          <div style={{ width:32, height:32, borderRadius:"50%", background:`linear-gradient(135deg,${C.amarillo},#e8a800)`, color:C.oscuro, fontSize:12, fontWeight:800, display:"flex", alignItems:"center", justifyContent:"center", flexShrink:0 }}>{iniciales}</div>
          <div style={{ flex:1, overflow:"hidden" }}>
            <div style={{ fontSize:13, fontWeight:700, color:"#fff", whiteSpace:"nowrap", overflow:"hidden", textOverflow:"ellipsis" }}>{session.username}</div>
            <div style={{ fontSize:11, color:C.medio }}>Cliente</div>
          </div>
          <button onClick={logout} title="Cerrar sesión"
            style={{ background:"transparent", border:"1px solid rgba(255,255,255,0.15)", borderRadius:6, color:C.medio, width:30, height:30, cursor:"pointer", display:"flex", alignItems:"center", justifyContent:"center", transition:"all 0.2s", flexShrink:0 }}
            onMouseEnter={e=>{e.currentTarget.style.borderColor="#ff6b6b";e.currentTarget.style.color="#ff6b6b";}}
            onMouseLeave={e=>{e.currentTarget.style.borderColor="rgba(255,255,255,0.15)";e.currentTarget.style.color=C.medio;}}>
            <LogOut size={14}/>
          </button>
        </div>
      </aside>

      {/* MAIN */}
      <main style={{ flex:1, overflowY:"auto", padding:"2rem 2.5rem" }}>
        {seccion==="boletos"&&(
          <>
            <div className="admin-topbar">
              <div>
                <h1 className="admin-page-title">Mis viajes</h1>
                <p className="admin-page-sub">{boletos.length} boleto(s) en tu historial</p>
              </div>
            </div>
            <HistorialBoletos boletos={boletos} loading={loadBoletos} headers={headers} onBoletosCambiados={cargarBoletos}/>
          </>
        )}
        {seccion==="encomiendas"&&(
          <>
            <div className="admin-topbar">
              <div>
                <h1 className="admin-page-title">Mis encomiendas</h1>
                <p className="admin-page-sub">Seguimiento de tus envíos como remitente</p>
              </div>
            </div>
            <PanelEncomiendas encomiendas={encomiendas} loading={loadEncom} headers={headers}/>
          </>
        )}
        {seccion==="perfil"&&(
          <>
            <div className="admin-topbar">
              <div>
                <h1 className="admin-page-title">Mi perfil</h1>
                <p className="admin-page-sub">Información de tu cuenta</p>
              </div>
            </div>
            <div className="cli-perfil">
              <div style={{ width:64, height:64, borderRadius:"50%", background:`linear-gradient(135deg,${C.amarillo},#e8a800)`, color:C.oscuro, fontSize:22, fontWeight:800, display:"flex", alignItems:"center", justifyContent:"center", margin:"0 auto 1.5rem" }}>{iniciales}</div>
              <div className="cli-perfil-grid">
                {perfil?(
                  <>
                    <div className="cli-perfil-item"><span>Nombre completo</span><strong>{[perfil.nombres,perfil.apellidos].filter(Boolean).join(" ")||"—"}</strong></div>
                    <div className="cli-perfil-item"><span>DNI / RUC</span><strong className="cli-mono">{perfil.dniRuc||perfil.dni_ruc||"—"}</strong></div>
                    <div className="cli-perfil-item"><span>Correo</span><strong>{perfil.email||"—"}</strong></div>
                    <div className="cli-perfil-item"><span>Teléfono</span><strong>{perfil.telefono||"—"}</strong></div>
                    <div className="cli-perfil-item"><span>Usuario</span><strong>{session.username}</strong></div>
                    <div className="cli-perfil-item"><span>Tipo</span><strong>{perfil.tipoCliente||perfil.tipo_cliente||"PERSONA"}</strong></div>
                  </>
                ):<p style={{color:C.medio,padding:"1rem 0"}}>Cargando datos...</p>}
              </div>
              <button className="auth-btn" style={{marginTop:"1.5rem",maxWidth:"200px"}} onClick={logout}>Cerrar sesión</button>
            </div>
          </>
        )}
      </main>
    </div>
  );
}