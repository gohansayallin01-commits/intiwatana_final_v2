import { useState, useEffect, useCallback } from "react";
import {
  Ticket, Search, Download, Printer, Eye, X, Loader2, RefreshCw,
  AlertTriangle, User, MapPin, Clock, Armchair, QrCode, RotateCcw,
} from "lucide-react";
import { PageHeader } from "../AdminDashboard";

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
    return d.toLocaleString("es-PE", { day: "2-digit", month: "short", year: "numeric", hour: "2-digit", minute: "2-digit" });
  } catch { return "—"; }
}

const ESTADO_COLOR = {
  ACTIVO: "#158463", USADO: "#397CBF", CANCELADO: "#C84C4C", ARREGLADO: "#D58B15",
};

export default function BoletosCaja({ headers, showToast }) {
  const [modo, setModo] = useState("numero");
  const [numero, setNumero] = useState("");
  const [docQ, setDocQ] = useState("");
  const [clientes, setClientes] = useState([]);
  const [buscando, setBuscando] = useState(false);
  const [viajes, setViajes] = useState([]);
  const [viajeId, setViajeId] = useState("");
  const [resultados, setResultados] = useState(null);
  const [load, setLoad] = useState(false);
  const [detalle, setDetalle] = useState(null);

  const cargarViajes = useCallback(async () => {
    try {
      const r = await fetch("/api/v1/viajes", { headers });
      const d = r.ok ? await r.json() : [];
      setViajes(Array.isArray(d) ? d : []);
    } catch { /* viajes opcionales */ }
  }, [headers]);

  useEffect(() => { cargarViajes(); }, [cargarViajes]);

  const inputS = { width: "100%", background: "white", border: `1.5px solid ${C.grisClaro}`, borderRadius: 8, padding: "9px 11px", color: C.oscuro, fontSize: 13, fontFamily: "inherit", boxSizing: "border-box" };

  const buscarPorNumero = async () => {
    const n = numero.trim();
    if (!n) { showToast("Ingresa el número de boleto"); return; }
    setLoad(true); setResultados(null);
    try {
      const r = await fetch(`/api/v1/boletos/numero/${encodeURIComponent(n)}`, { headers });
      const d = await r.json().catch(() => ({}));
      if (!r.ok) throw new Error(d.mensaje || d.message || "Boleto no encontrado");
      setResultados([d]);
    } catch (e) { showToast(e.message); setResultados([]); }
    finally { setLoad(false); }
  };

  const buscarClientes = async () => {
    const q = docQ.trim();
    if (!q) { showToast("Ingresa documento o nombre del pasajero"); return; }
    setBuscando(true);
    try {
      const r = await fetch(`/api/v1/clientes?q=${encodeURIComponent(q)}&page=0&size=8`, { headers });
      const d = r.ok ? await r.json() : {};
      setClientes(Array.isArray(d?.content) ? d.content : []);
      if (!d?.content?.length) showToast("Sin clientes que coincidan");
    } catch { showToast("Error al buscar clientes"); }
    finally { setBuscando(false); }
  };

  const buscarPorCliente = async (cliente) => {
    setLoad(true); setResultados(null); setClientes([]);
    try {
      const r = await fetch(`/api/v1/boletos/cliente/${cliente.id}?page=0&size=50`, { headers });
      const d = r.ok ? await r.json() : {};
      setResultados(Array.isArray(d?.content) ? d.content : []);
    } catch { showToast("Error al cargar boletos del pasajero"); setResultados([]); }
    finally { setLoad(false); }
  };

  const buscarPorViaje = async () => {
    if (!viajeId) { showToast("Selecciona un viaje"); return; }
    setLoad(true); setResultados(null);
    try {
      const r = await fetch(`/api/v1/boletos/viaje/${viajeId}?page=0&size=100`, { headers });
      const d = r.ok ? await r.json() : {};
      setResultados(Array.isArray(d?.content) ? d.content : []);
    } catch { showToast("Error al cargar boletos del viaje"); setResultados([]); }
    finally { setLoad(false); }
  };

  const abrirPdf = (b) => {
    const doc = b?.cliente?.dniRuc;
    if (!doc) { showToast("El boleto no tiene documento del comprador"); return; }
    window.open(`/api/v1/ventas/boleto/${encodeURIComponent(b.numeroBoleto)}/pdf?documento=${encodeURIComponent(doc)}`, "_blank", "noopener,noreferrer");
  };

  const reimprimir = (b) => {
    setDetalle(b);
    showToast("Reimpresión del boleto existente (no se emite uno nuevo)");
  };

  const tabs = [
    { k: "numero", label: "N° de boleto" },
    { k: "pasajero", label: "Pasajero / documento" },
    { k: "viaje", label: "Viaje" },
  ];

  return (
    <div>
      <style>{`@media print{body *{visibility:hidden!important}.caja-print,.caja-print *{visibility:visible!important}.caja-print{position:absolute;left:0;top:0;width:100%;padding:16px}}`}</style>

      <PageHeader title="Boletos" sub="Consulta, descarga y reimpresión de boletos existentes (no genera duplicados)" />

      <div style={{ background: C.blanco, border: `1.5px solid ${C.grisClaro}`, borderRadius: 14, padding: "1rem 1.1rem", marginBottom: 14 }}>
        <div style={{ display: "flex", gap: 8, marginBottom: 12, flexWrap: "wrap" }}>
          {tabs.map(t => (
            <button key={t.k} onClick={() => { setModo(t.k); setResultados(null); setClientes([]); }}
              className={modo === t.k ? "adm-btn-primary" : "adm-btn-secondary"}>{t.label}</button>
          ))}
        </div>

        {modo === "numero" && (
          <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
            <input value={numero} onChange={e => setNumero(e.target.value)} onKeyDown={e => e.key === "Enter" && buscarPorNumero()} placeholder="Ej: BOL-000123" style={{ ...inputS, flex: "1 1 240px" }} />
            <button className="adm-btn-primary" onClick={buscarPorNumero} disabled={load}><Search size={14} style={{ marginRight: 6 }} />Buscar</button>
          </div>
        )}

        {modo === "pasajero" && (
          <div>
            <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
              <input value={docQ} onChange={e => setDocQ(e.target.value)} onKeyDown={e => e.key === "Enter" && buscarClientes()} placeholder="Documento o nombre del pasajero" style={{ ...inputS, flex: "1 1 240px" }} />
              <button className="adm-btn-primary" onClick={buscarClientes} disabled={buscando}>{buscando ? <Loader2 size={14} className="spin-icon" style={{ marginRight: 6 }} /> : <Search size={14} style={{ marginRight: 6 }} />}Buscar pasajero</button>
            </div>
            {clientes.length > 0 && (
              <div style={{ marginTop: 10, display: "flex", flexDirection: "column", gap: 6 }}>
                {clientes.map(c => (
                  <button key={c.id} onClick={() => buscarPorCliente(c)} style={{ textAlign: "left", background: C.fondo, border: `1px solid ${C.grisClaro}`, borderRadius: 8, padding: "8px 11px", cursor: "pointer", display: "flex", alignItems: "center", gap: 8 }}>
                    <User size={14} color={C.verde} />
                    <span style={{ fontWeight: 700, color: C.oscuro }}>{c.nombreCompleto || c.razonSocial}</span>
                    <span style={{ color: C.medio, fontSize: 12 }}>{c.dniRuc}</span>
                  </button>
                ))}
              </div>
            )}
          </div>
        )}

        {modo === "viaje" && (
          <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
            <select value={viajeId} onChange={e => setViajeId(e.target.value)} style={{ ...inputS, flex: "1 1 240px" }}>
              <option value="">Selecciona un viaje…</option>
              {viajes.map(v => (
                <option key={v.id} value={v.id}>#{v.id} · {v.ruta?.origen?.ciudad} → {v.ruta?.destino?.ciudad} · {fmtFecha(v.fechaHoraSalida)}</option>
              ))}
            </select>
            <button className="adm-btn-primary" onClick={buscarPorViaje} disabled={load}><Search size={14} style={{ marginRight: 6 }} />Buscar</button>
          </div>
        )}
      </div>

      {load && <div style={{ textAlign: "center", padding: "1.5rem", color: C.medio }}><Loader2 size={22} className="spin-icon" /></div>}

      {!load && resultados !== null && (
        resultados.length === 0 ? (
          <div style={{ background: C.blanco, border: `1.5px solid ${C.grisClaro}`, borderRadius: 14, padding: "2rem", textAlign: "center", color: C.medio }}>
            <Ticket size={26} style={{ opacity: 0.5, marginBottom: 8 }} />
            <div style={{ fontWeight: 700 }}>Sin boletos para el criterio indicado</div>
          </div>
        ) : (
          <div className="adm-table-wrap">
            <table className="adm-table">
              <thead>
                <tr><th>N° Boleto</th><th>Pasajero</th><th>Viaje</th><th>Asiento</th><th>Fecha</th><th>Monto</th><th>Estado</th><th>Acciones</th></tr>
              </thead>
              <tbody>
                {resultados.map(b => (
                  <tr key={b.id || b.numeroBoleto} className="tr-hover">
                    <td><span className="td-codigo">{b.numeroBoleto}</span></td>
                    <td>
                      <div style={{ display: "flex", flexDirection: "column" }}>
                        <strong>{b.cliente?.nombreCompleto || "—"}</strong>
                        <span style={{ fontSize: 10, color: C.medio }}>{b.cliente?.dniRuc || ""}</span>
                      </div>
                    </td>
                    <td style={{ fontSize: 12 }}>{b.viaje?.ruta?.origen?.ciudad} → {b.viaje?.ruta?.destino?.ciudad}</td>
                    <td>N°{b.asiento?.numeroAsiento || "—"}</td>
                    <td style={{ fontSize: 12 }}>{fmtFecha(b.createdAt || b.viaje?.fechaHoraSalida)}</td>
                    <td><strong style={{ color: C.verde }}>S/ {Number(b.precioPagado ?? 0).toFixed(2)}</strong></td>
                    <td><span style={{ fontWeight: 700, color: ESTADO_COLOR[b.estado] || C.oscuro }}>{b.estado || "—"}</span></td>
                    <td>
                      <div className="td-actions">
                        <button className="btn-edit" title="Ver" aria-label="Ver boleto" onClick={() => setDetalle(b)}><Eye size={13} /></button>
                        <button className="btn-edit" title="Descargar PDF" aria-label="Descargar PDF" onClick={() => abrirPdf(b)}><Download size={13} /></button>
                        <button className="btn-edit" title="Reimprimir" aria-label="Reimprimir boleto" onClick={() => reimprimir(b)}><Printer size={13} /></button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )
      )}

      {detalle && (
        <div className="modal-overlay" onClick={() => setDetalle(null)} role="presentation">
          <div className="modal caja-print" role="dialog" aria-modal="true" aria-label="Detalle de boleto" style={{ maxWidth: 480, width: "95%" }} onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <div className="modal-badge"><Ticket size={20} /></div>
              <div><div className="modal-title">Boleto {detalle.numeroBoleto}</div><div className="modal-subtitle">Estado: {detalle.estado || "—"}</div></div>
              <button className="modal-close" onClick={() => setDetalle(null)} aria-label="Cerrar"><X size={18} /></button>
            </div>
            <div className="modal-body" style={{ display: "flex", flexDirection: "column", gap: 10 }}>
              <div style={{ display: "flex", flexDirection: "column", gap: 8, background: C.fondo, border: `1px solid ${C.grisClaro}`, borderRadius: 10, padding: "12px 14px", fontSize: 13, color: C.oscuro }}>
                <div style={{ display: "flex", gap: 8, alignItems: "center" }}><User size={14} color={C.verde} /><strong>{detalle.cliente?.nombreCompleto || "—"}</strong> · {detalle.cliente?.dniRuc || ""}</div>
                <div style={{ display: "flex", gap: 8, alignItems: "center" }}><MapPin size={14} color={C.verde} />{detalle.viaje?.ruta?.origen?.ciudad} → {detalle.viaje?.ruta?.destino?.ciudad}</div>
                <div style={{ display: "flex", gap: 8, alignItems: "center" }}><Clock size={14} color={C.verde} />{fmtFecha(detalle.viaje?.fechaHoraSalida)}</div>
                <div style={{ display: "flex", gap: 8, alignItems: "center" }}><Armchair size={14} color={C.verde} />Asiento N°{detalle.asiento?.numeroAsiento || "—"}</div>
              </div>
              <div style={{ textAlign: "center", background: "white", border: `1px dashed ${C.verde}`, borderRadius: 10, padding: "12px" }}>
                {detalle.qrImagenUrl ? (
                  <img src={detalle.qrImagenUrl} alt={`QR ${detalle.numeroBoleto}`} style={{ width: 140, height: 140, objectFit: "contain" }} />
                ) : (
                  <QrCode size={40} color={C.verde} />
                )}
                <div style={{ fontFamily: "monospace", fontWeight: 800, color: C.oscuro, fontSize: 12, marginTop: 6, wordBreak: "break-all" }}>{detalle.codigoQr || detalle.numeroBoleto}</div>
                <div style={{ fontSize: 11, color: C.medio, marginTop: 2 }}>Código QR de embarque</div>
              </div>
              <div style={{ display: "flex", justifyContent: "space-between", fontSize: 14 }}><span style={{ color: C.medio }}>Monto pagado</span><strong style={{ color: C.verde }}>S/ {Number(detalle.precioPagado ?? 0).toFixed(2)}</strong></div>
              <div style={{ display: "flex", gap: 10, flexWrap: "wrap" }}>
                <button className="adm-btn-primary" onClick={() => abrirPdf(detalle)}><Download size={14} style={{ marginRight: 6 }} />Descargar PDF</button>
                <button className="adm-btn-secondary" onClick={() => window.print()}><Printer size={14} style={{ marginRight: 6 }} />Imprimir</button>
                <button className="adm-btn-secondary" onClick={() => { abrirPdf(detalle); reimprimir(detalle); }}><RotateCcw size={14} style={{ marginRight: 6 }} />Reimprimir</button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
