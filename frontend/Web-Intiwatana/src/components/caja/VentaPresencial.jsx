import { useState, useEffect, useCallback, useMemo } from "react";
import {
  Ticket, RefreshCw, MapPin, Bus, Clock, Armchair, CreditCard,
  CheckCircle2, Download, Printer, ArrowLeft, ArrowRight, Loader2,
  Banknote, AlertTriangle, RotateCcw, Search,
} from "lucide-react";
import { PageHeader, SelectorCliente } from "../AdminDashboard";
import { MapaAsientos } from "../PasajesPage";

const C = {
  verde: "#127369", oscuro: "#0B3D38", amarillo: "#F5C518",
  medio: "#667875", gris: "#17312E", grisClaro: "#DDE7E4",
  fondo: "#F4F7F6", blanco: "#ffffff", rojo: "#C84C4C", naranja: "#D58B15",
};

function fmtFecha(iso) {
  if (!iso) return "—";
  try {
    const d = new Date(iso);
    if (isNaN(d.getTime())) return "—";
    return d.toLocaleString("es-PE", { weekday: "short", day: "2-digit", month: "short", hour: "2-digit", minute: "2-digit" });
  } catch { return "—"; }
}

function nuevaClave() {
  try { return crypto.randomUUID(); } catch { return `venta-${Date.now()}-${Math.random().toString(36).slice(2)}`; }
}

const PASOS = ["Viaje", "Asientos", "Pasajero", "Cobro", "Boleto"];

export default function VentaPresencial({ headers, showToast, irA }) {
  const [paso, setPaso] = useState(1);
  const [viajes, setViajes] = useState([]);
  const [loadViajes, setLoadViajes] = useState(true);
  const [viaje, setViaje] = useState(null);
  const [asientos, setAsientos] = useState([]);
  const [loadAsientos, setLoadAsientos] = useState(false);
  const [seleccionados, setSeleccionados] = useState([]);
  const [cliente, setCliente] = useState(null);
  const [recibido, setRecibido] = useState("");
  const [observaciones, setObservaciones] = useState("");
  const [idemKey, setIdemKey] = useState(nuevaClave);
  const [enviando, setEnviando] = useState(false);
  const [boletos, setBoletos] = useState(null);

  const cargarViajes = useCallback(async () => {
    setLoadViajes(true);
    try {
      const r = await fetch("/api/v1/viajes", { headers });
      const d = r.ok ? await r.json() : [];
      setViajes(Array.isArray(d) ? d : []);
    } catch {
      showToast("Error al cargar viajes");
    } finally {
      setLoadViajes(false);
    }
  }, [headers, showToast]);

  useEffect(() => { cargarViajes(); }, [cargarViajes]);

  const viajesAptos = useMemo(() => {
    const ahora = Date.now();
    return viajes.filter(v =>
      v.estado === "PROGRAMADO" &&
      new Date(v.fechaHoraSalida).getTime() > ahora &&
      (v.asientosDisponibles ?? 0) > 0 &&
      v.bus?.estadoConfiguracion === "COMPLETO"
    ).sort((a, b) => new Date(a.fechaHoraSalida) - new Date(b.fechaHoraSalida));
  }, [viajes]);

  const elegirViaje = async (v) => {
    setViaje(v);
    setSeleccionados([]);
    setCliente(null);
    setPaso(2);
    setLoadAsientos(true);
    try {
      const r = await fetch(`/api/v1/viajes/${v.id}/asientos`, { headers });
      const d = r.ok ? await r.json() : [];
      setAsientos(Array.isArray(d) ? d : []);
    } catch {
      showToast("Error al cargar asientos");
      setAsientos([]);
    } finally {
      setLoadAsientos(false);
    }
  };

  const alternarAsiento = (a) => {
    if (a.estado !== "DISPONIBLE") return;
    setSeleccionados(prev => {
      const existe = prev.some(s => s.id === a.id);
      if (existe) return prev.filter(s => s.id !== a.id);
      if (prev.length >= 10) { showToast("Máximo 10 asientos por venta"); return prev; }
      return [...prev, a];
    });
  };

  const precioUnitario = Number(viaje?.precioAdulto ?? viaje?.precioOficial ?? 0);
  const total = seleccionados.length * precioUnitario;
  const recibidoNum = parseFloat(recibido);
  const vuelto = Number.isFinite(recibidoNum) ? recibidoNum - total : 0;
  const puedeCobrar = cliente && seleccionados.length > 0 && Number.isFinite(recibidoNum) && recibidoNum >= total && total > 0;

  const irAPasajero = () => {
    if (!seleccionados.length) { showToast("Selecciona al menos un asiento disponible"); return; }
    setPaso(3);
  };

  const irACobro = () => {
    if (!cliente) { showToast("Selecciona o registra el pasajero"); return; }
    setIdemKey(nuevaClave());
    setRecibido("");
    setPaso(4);
  };

  const cobrar = async () => {
    if (enviando) return;
    if (!puedeCobrar) { showToast("El efectivo recibido es insuficiente"); return; }
    setEnviando(true);
    try {
      const r = await fetch("/api/v1/ventas/presencial", {
        method: "POST",
        headers,
        body: JSON.stringify({
          viajeId: viaje.id,
          asientoIds: seleccionados.map(a => a.id),
          clienteId: cliente.id,
          metodoPago: "EFECTIVO",
          referenciaPago: null,
          observaciones: observaciones.trim() || null,
          idempotencyKey: idemKey,
        }),
      });
      const d = await r.json().catch(() => ({}));
      if (!r.ok) throw new Error(d.mensaje || d.message || "No se pudo registrar la venta");
      if (!Array.isArray(d) || d.length === 0) throw new Error("El backend no devolvió boletos");
      setBoletos(d);
      setPaso(5);
      showToast(`Venta registrada: ${d.length} boleto(s) emitido(s)`);
    } catch (e) {
      showToast(e.message || "No se pudo registrar la venta");
    } finally {
      setEnviando(false);
    }
  };

  const abrirPdf = (numeroBoleto) => {
    const doc = cliente?.dniRuc;
    if (!doc) { showToast("Falta el documento del comprador para el PDF"); return; }
    window.open(`/api/v1/ventas/boleto/${encodeURIComponent(numeroBoleto)}/pdf?documento=${encodeURIComponent(doc)}`, "_blank", "noopener,noreferrer");
  };

  const reiniciar = () => {
    setPaso(1);
    setViaje(null);
    setAsientos([]);
    setSeleccionados([]);
    setCliente(null);
    setRecibido("");
    setObservaciones("");
    setBoletos(null);
    setIdemKey(nuevaClave());
    cargarViajes();
  };

  const inputS = { width: "100%", background: "white", border: `1.5px solid ${C.grisClaro}`, borderRadius: 8, padding: "9px 11px", color: C.oscuro, fontSize: 13, fontFamily: "inherit", boxSizing: "border-box" };
  const card = { background: C.blanco, border: `1.5px solid ${C.grisClaro}`, borderRadius: 14, padding: "1.1rem 1.2rem" };

  const pasoActual = paso === 5 ? 5 : paso;

  return (
    <div>
      <style>{`@media print{body *{visibility:hidden!important}.caja-print,.caja-print *{visibility:visible!important}.caja-print{position:absolute;left:0;top:0;width:100%;padding:16px}}`}</style>

      <PageHeader
        title="Nueva venta"
        sub="Venta presencial en efectivo: viaje, asiento, pasajero y boleto con QR"
        action={<button className="adm-btn-secondary" onClick={reiniciar} disabled={enviando}><RotateCcw size={14} style={{ marginRight: 6 }} />Nueva venta</button>}
      />

      <div style={{ display: "flex", gap: 8, marginBottom: 16, flexWrap: "wrap" }}>
        {PASOS.map((label, i) => {
          const n = i + 1;
          const activo = n === pasoActual;
          const hecho = n < pasoActual;
          return (
            <div key={label} style={{ display: "flex", alignItems: "center", gap: 6, opacity: activo || hecho ? 1 : 0.55 }}>
              <span style={{ width: 22, height: 22, borderRadius: "50%", display: "inline-flex", alignItems: "center", justifyContent: "center", fontSize: 11, fontWeight: 800, background: activo ? C.verde : hecho ? C.amarillo : C.grisClaro, color: activo ? "white" : C.oscuro }}>{hecho ? "✓" : n}</span>
              <span style={{ fontSize: 12, fontWeight: activo ? 700 : 500, color: C.oscuro }}>{label}</span>
              {i < PASOS.length - 1 && <ArrowRight size={13} color={C.medio} />}
            </div>
          );
        })}
      </div>

      {paso === 1 && (
        <div style={card}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 12 }}>
            <strong style={{ color: C.oscuro, display: "flex", alignItems: "center", gap: 8 }}><Bus size={17} color={C.verde} /> Viajes disponibles para venta</strong>
            <button className="adm-btn-secondary" onClick={cargarViajes} disabled={loadViajes}><RefreshCw size={13} className={loadViajes ? "spin-icon" : undefined} style={{ marginRight: 6 }} />Actualizar</button>
          </div>
          {loadViajes ? (
            <div className="skeleton-rows"><div className="skeleton-head" />{[1, 2, 3].map(i => <div key={i} className="skeleton-row" />)}</div>
          ) : viajesAptos.length === 0 ? (
            <div style={{ textAlign: "center", padding: "2rem", color: C.medio }}>
              <Search size={26} style={{ marginBottom: 8, opacity: 0.5 }} />
              <div style={{ fontWeight: 700 }}>No hay viajes aptos para vender</div>
              <div style={{ fontSize: 12 }}>Solo se ofrecen viajes programados a futuro con vehículo COMPLETO y asientos libres.</div>
            </div>
          ) : (
            <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill,minmax(260px,1fr))", gap: 12 }}>
              {viajesAptos.map(v => (
                <div key={v.id} style={{ border: `1.5px solid ${C.grisClaro}`, borderRadius: 12, padding: "12px 13px", display: "flex", flexDirection: "column", gap: 6 }}>
                  <div style={{ display: "flex", alignItems: "center", gap: 7, fontWeight: 800, color: C.oscuro }}>
                    <MapPin size={15} color={C.verde} /> {v.ruta?.origen?.ciudad} → {v.ruta?.destino?.ciudad}
                  </div>
                  <div style={{ fontSize: 12, color: C.medio, display: "flex", alignItems: "center", gap: 6 }}><Clock size={12} /> {fmtFecha(v.fechaHoraSalida)}</div>
                  <div style={{ fontSize: 12, color: C.medio, display: "flex", alignItems: "center", gap: 6 }}><Bus size={12} /> {v.bus?.placa} · {v.bus?.tipo}</div>
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginTop: 4 }}>
                    <span style={{ fontSize: 12, color: C.medio, display: "flex", alignItems: "center", gap: 6 }}><Armchair size={12} /> {v.asientosDisponibles} libres</span>
                    <strong style={{ color: C.verde }}>S/ {Number(v.precioAdulto ?? 0).toFixed(2)}</strong>
                  </div>
                  <button className="adm-btn-primary" style={{ marginTop: 6, justifyContent: "center", display: "flex" }} onClick={() => elegirViaje(v)}>
                    Elegir viaje <ArrowRight size={14} style={{ marginLeft: 6 }} />
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {paso === 2 && viaje && (
        <div style={card}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: 8, marginBottom: 12 }}>
            <strong style={{ color: C.oscuro, display: "flex", alignItems: "center", gap: 8 }}><Armchair size={17} color={C.verde} /> Selecciona asientos · {viaje.ruta?.origen?.ciudad} → {viaje.ruta?.destino?.ciudad}</strong>
            <div style={{ display: "flex", gap: 8 }}>
              <button className="adm-btn-secondary" onClick={() => { setPaso(1); setSeleccionados([]); }}><ArrowLeft size={13} style={{ marginRight: 6 }} />Cambiar viaje</button>
              <button className="adm-btn-primary" onClick={irAPasajero} disabled={!seleccionados.length}>Continuar <ArrowRight size={14} style={{ marginLeft: 6 }} /></button>
            </div>
          </div>
          {loadAsientos ? (
            <div style={{ textAlign: "center", padding: "2rem", color: C.medio }}><Loader2 size={22} className="spin-icon" /></div>
          ) : (
            <MapaAsientos asientos={asientos} seleccionados={seleccionados} onSeleccionar={alternarAsiento} configuracion={viaje.bus?.configuracionAsientos} />
          )}
          <div style={{ marginTop: 12, display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: 8, borderTop: `1px solid ${C.grisClaro}`, paddingTop: 10 }}>
            <span style={{ fontSize: 13, color: C.oscuro }}><strong>{seleccionados.length}</strong> asiento(s): {seleccionados.map(a => `N°${a.numeroAsiento}`).join(", ") || "—"}</span>
            <strong style={{ color: C.verde }}>Total estimado S/ {total.toFixed(2)}</strong>
          </div>
        </div>
      )}

      {paso === 3 && viaje && (
        <div style={card}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: 8, marginBottom: 12 }}>
            <strong style={{ color: C.oscuro, display: "flex", alignItems: "center", gap: 8 }}><Ticket size={17} color={C.verde} /> Pasajero / comprador</strong>
            <div style={{ display: "flex", gap: 8 }}>
              <button className="adm-btn-secondary" onClick={() => setPaso(2)}><ArrowLeft size={13} style={{ marginRight: 6 }} />Asientos</button>
              <button className="adm-btn-primary" onClick={irACobro} disabled={!cliente}>Ir al cobro <ArrowRight size={14} style={{ marginLeft: 6 }} /></button>
            </div>
          </div>
          <SelectorCliente headers={headers} label="Pasajero / comprador" value={cliente} onChange={setCliente} showToast={showToast} />
          <div style={{ marginTop: 14, background: C.fondo, border: `1px solid ${C.grisClaro}`, borderRadius: 10, padding: "10px 12px", fontSize: 12.5, color: C.medio, display: "flex", flexWrap: "wrap", gap: 12 }}>
            <span><MapPin size={12} /> {viaje.ruta?.origen?.ciudad} → {viaje.ruta?.destino?.ciudad}</span>
            <span><Clock size={12} /> {fmtFecha(viaje.fechaHoraSalida)}</span>
            <span><Armchair size={12} /> {seleccionados.map(a => `N°${a.numeroAsiento}`).join(", ")}</span>
            <span><CreditCard size={12} /> Efectivo · <strong style={{ color: C.verde }}>S/ {total.toFixed(2)}</strong></span>
          </div>
        </div>
      )}

      {paso === 4 && viaje && cliente && (
        <div style={card}>
          <strong style={{ color: C.oscuro, display: "flex", alignItems: "center", gap: 8, marginBottom: 12 }}><Banknote size={17} color={C.verde} /> Cobro en efectivo</strong>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit,minmax(240px,1fr))", gap: 16 }}>
            <div>
              <div style={{ background: C.fondo, border: `1px solid ${C.grisClaro}`, borderRadius: 10, padding: "10px 12px", marginBottom: 12, fontSize: 12.5, color: C.oscuro, display: "flex", flexDirection: "column", gap: 5 }}>
                <div><strong>{cliente.nombreCompleto || cliente.razonSocial}</strong> · {cliente.dniRuc}</div>
                <div style={{ color: C.medio }}>{seleccionados.map(a => `N°${a.numeroAsiento}`).join(", ")} · {viaje.ruta?.origen?.ciudad} → {viaje.ruta?.destino?.ciudad}</div>
                <div style={{ color: C.medio }}>{fmtFecha(viaje.fechaHoraSalida)}</div>
              </div>
              <label style={{ fontSize: 12, color: C.medio, display: "block", marginBottom: 5 }}>Efectivo recibido (S/) *</label>
              <input type="number" min="0" step="0.10" value={recibido} onChange={e => setRecibido(e.target.value)} placeholder={`Mínimo S/ ${total.toFixed(2)}`} style={inputS} />
              <label style={{ fontSize: 12, color: C.medio, display: "block", margin: "10px 0 5px" }}>Observaciones (opcional)</label>
              <input value={observaciones} onChange={e => setObservaciones(e.target.value)} maxLength={300} placeholder="Ej: pasajero con equipaje" style={inputS} />
            </div>
            <div style={{ background: C.oscuro, borderRadius: 12, padding: "16px 18px", color: "white", display: "flex", flexDirection: "column", gap: 10 }}>
              <div style={{ display: "flex", justifyContent: "space-between", fontSize: 13 }}><span>Total a cobrar</span><strong>S/ {total.toFixed(2)}</strong></div>
              <div style={{ display: "flex", justifyContent: "space-between", fontSize: 13 }}><span>Efectivo recibido</span><strong>S/ {(Number.isFinite(recibidoNum) ? recibidoNum : 0).toFixed(2)}</strong></div>
              <div style={{ borderTop: "1px solid rgba(255,255,255,0.25)", paddingTop: 10, display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                <span style={{ fontSize: 13 }}>Vuelto</span>
                <strong style={{ fontSize: 22, color: C.amarillo }}>S/ {(Number.isFinite(recibidoNum) && vuelto > 0 ? vuelto : 0).toFixed(2)}</strong>
              </div>
              {Number.isFinite(recibidoNum) && recibidoNum < total && (
                <div style={{ background: "rgba(200,76,76,0.18)", border: "1px solid #C84C4C", borderRadius: 8, padding: "7px 10px", fontSize: 12, display: "flex", gap: 6, alignItems: "center" }}><AlertTriangle size={14} /> Efectivo insuficiente</div>
              )}
            </div>
          </div>
          <div style={{ display: "flex", gap: 10, marginTop: 16, flexWrap: "wrap" }}>
            <button className="adm-btn-secondary" onClick={() => setPaso(3)} disabled={enviando}><ArrowLeft size={13} style={{ marginRight: 6 }} />Pasajero</button>
            <button className="adm-btn-primary" onClick={cobrar} disabled={!puedeCobrar || enviando} style={{ display: "flex", alignItems: "center", gap: 8 }}>
              {enviando ? <><Loader2 size={15} className="spin-icon" /> Procesando...</> : <><CheckCircle2 size={15} /> Confirmar cobro y emitir boleto</>}
            </button>
          </div>
        </div>
      )}

      {paso === 5 && boletos && (
        <div style={card} className="caja-print">
          <div style={{ display: "flex", alignItems: "center", gap: 10, marginBottom: 14, color: "#158463", fontWeight: 800, fontSize: 16 }}>
            <CheckCircle2 size={22} color="#158463" /> Venta registrada · boletos emitidos
          </div>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill,minmax(280px,1fr))", gap: 12 }}>
            {boletos.map(b => (
              <div key={b.id || b.numeroBoleto} style={{ border: `2px dashed ${C.verde}`, borderRadius: 12, padding: "14px 15px", background: "#F6FBFA", display: "flex", flexDirection: "column", gap: 6 }}>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                  <span style={{ fontSize: 11, textTransform: "uppercase", letterSpacing: "0.08em", color: C.medio }}>Boleto</span>
                  <span style={{ fontFamily: "monospace", fontWeight: 800, color: C.oscuro }}>{b.numeroBoleto}</span>
                </div>
                <div style={{ fontSize: 13, color: C.oscuro }}><strong>{b.cliente?.nombreCompleto || cliente?.nombreCompleto}</strong></div>
                <div style={{ fontSize: 12, color: C.medio }}>{b.viaje?.ruta?.origen?.ciudad || viaje.ruta?.origen?.ciudad} → {b.viaje?.ruta?.destino?.ciudad || viaje.ruta?.destino?.ciudad} · Asiento N°{b.asiento?.numeroAsiento}</div>
                <div style={{ fontSize: 12, color: C.medio }}>{fmtFecha(b.viaje?.fechaHoraSalida || viaje.fechaHoraSalida)}</div>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginTop: 4 }}>
                  <span style={{ fontSize: 12, color: C.medio }}>Pagado</span>
                  <strong style={{ color: C.verde }}>S/ {Number(b.precioPagado ?? total / boletos.length).toFixed(2)}</strong>
                </div>
                <div style={{ marginTop: 6, background: "white", border: `1px solid ${C.grisClaro}`, borderRadius: 8, padding: "8px 10px", textAlign: "center" }}>
                  {b.qrImagenUrl ? (
                    <img src={b.qrImagenUrl} alt={`QR ${b.numeroBoleto}`} style={{ width: 120, height: 120, objectFit: "contain" }} />
                  ) : (
                    <div style={{ fontSize: 11, color: C.medio }}>Código QR de embarque</div>
                  )}
                  <div style={{ fontFamily: "monospace", fontWeight: 800, color: C.oscuro, fontSize: 12, marginTop: 4, wordBreak: "break-all" }}>{b.codigoQr || b.numeroBoleto}</div>
                </div>
                <div style={{ display: "flex", gap: 8, marginTop: 6 }}>
                  <button className="adm-btn-secondary" onClick={() => abrirPdf(b.numeroBoleto)}><Download size={13} style={{ marginRight: 5 }} />PDF</button>
                  <button className="adm-btn-secondary" onClick={() => window.print()}><Printer size={13} style={{ marginRight: 5 }} />Imprimir</button>
                </div>
              </div>
            ))}
          </div>
          <div style={{ marginTop: 16, display: "flex", gap: 10, flexWrap: "wrap" }}>
            <button className="adm-btn-primary" onClick={reiniciar}><RotateCcw size={14} style={{ marginRight: 6 }} />Iniciar otra venta</button>
            {irA && <button className="adm-btn-secondary" onClick={() => irA("boletos")}><Ticket size={14} style={{ marginRight: 6 }} />Ver en Boletos</button>}
            {irA && <button className="adm-btn-secondary" onClick={() => irA("operaciones")}><CreditCard size={14} style={{ marginRight: 6 }} />Ver en Operaciones</button>}
          </div>
        </div>
      )}
    </div>
  );
}
