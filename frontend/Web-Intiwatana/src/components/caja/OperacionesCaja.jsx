import { useState, useEffect, useCallback, useMemo } from "react";
import {
  BarChart3, RefreshCw, Loader2, Banknote, Ticket, Receipt,
  AlertTriangle, Search, Info,
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

export default function OperacionesCaja({ headers, showToast }) {
  const [tab, setTab] = useState("pagos");
  const [pagos, setPagos] = useState([]);
  const [boletos, setBoletos] = useState([]);
  const [loadPagos, setLoadPagos] = useState(true);
  const [loadBoletos, setLoadBoletos] = useState(false);
  const [q, setQ] = useState("");
  const [desde, setDesde] = useState("");
  const [hasta, setHasta] = useState("");
  const [cargados, setCargados] = useState(false);

  const cargarPagos = useCallback(async () => {
    setLoadPagos(true);
    try {
      const r = await fetch("/api/v1/pagos", { headers });
      const d = r.ok ? await r.json() : [];
      setPagos(Array.isArray(d) ? d : []);
    } catch { showToast("Error al cargar pagos"); }
    finally { setLoadPagos(false); }
  }, [headers, showToast]);

  const cargarBoletos = useCallback(async () => {
    setLoadBoletos(true);
    try {
      const rv = await fetch("/api/v1/viajes", { headers });
      const viajes = rv.ok ? await rv.json() : [];
      const lista = Array.isArray(viajes) ? viajes : [];
      const resultados = await Promise.all(lista.map(async (v) => {
        try {
          const rb = await fetch(`/api/v1/boletos/viaje/${v.id}?page=0&size=100`, { headers });
          const d = rb.ok ? await rb.json() : {};
          return Array.isArray(d?.content) ? d.content : [];
        } catch { return []; }
      }));
      setBoletos(resultados.flat());
    } catch { showToast("Error al cargar boletos"); }
    finally { setLoadBoletos(false); }
  }, [headers, showToast]);

  useEffect(() => { cargarPagos(); }, [cargarPagos]);

  const cargarTodo = async () => {
    setCargados(true);
    await Promise.all([cargarPagos(), cargarBoletos()]);
  };

  const enRango = useCallback((iso) => {
    if (!iso) return true;
    const t = new Date(iso).getTime();
    if (desde && t < new Date(`${desde}T00:00:00`).getTime()) return false;
    if (hasta && t > new Date(`${hasta}T23:59:59`).getTime()) return false;
    return true;
  }, [desde, hasta]);

  const pagosF = useMemo(() => {
    const term = q.toLowerCase();
    return pagos.filter(p => enRango(p.fechaPago) &&
      `${p.cliente || ""} ${p.referencia || ""} ${p.referenciaEntidad || ""} ${p.metodo || ""}`.toLowerCase().includes(term)
    );
  }, [pagos, q, enRango]);

  const boletosF = useMemo(() => {
    const term = q.toLowerCase();
    return boletos.filter(b => enRango(b.createdAt) &&
      `${b.numeroBoleto || ""} ${b.cliente?.nombreCompleto || ""} ${b.cliente?.dniRuc || ""} ${b.viaje?.ruta?.origen?.ciudad || ""} ${b.viaje?.ruta?.destino?.ciudad || ""}`.toLowerCase().includes(term)
    );
  }, [boletos, q, enRango]);

  const efectivoConfirmado = useMemo(() =>
    pagos.filter(p => p.estado === "COMPLETADO" && p.metodo === "EFECTIVO").reduce((s, p) => s + Number(p.monto || 0), 0), [pagos]);
  const totalConfirmado = useMemo(() =>
    pagos.filter(p => p.estado === "COMPLETADO").reduce((s, p) => s + Number(p.monto || 0), 0), [pagos]);

  const kpi = (icon, label, value, color) => (
    <div style={{ background: C.blanco, border: `1.5px solid ${C.grisClaro}`, borderRadius: 12, padding: "12px 14px", display: "flex", alignItems: "center", gap: 10 }}>
      <span style={{ width: 36, height: 36, borderRadius: 10, background: `${color}1a`, color, display: "inline-flex", alignItems: "center", justifyContent: "center" }}>{icon}</span>
      <div>
        <div style={{ fontSize: 11, color: C.medio, textTransform: "uppercase", letterSpacing: "0.06em" }}>{label}</div>
        <div style={{ fontWeight: 800, color: C.oscuro, fontSize: 16 }}>{value}</div>
      </div>
    </div>
  );

  return (
    <div>
      <PageHeader title="Operaciones del cajero" sub="Movimientos reales de caja: pagos confirmados y boletos emitidos" action={
        <button className="adm-btn-secondary" onClick={cargarTodo} disabled={loadPagos || loadBoletos}>
          <RefreshCw size={14} className={loadPagos || loadBoletos ? "spin-icon" : undefined} style={{ marginRight: 6 }} />Actualizar
        </button>
      } />

      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit,minmax(190px,1fr))", gap: 12, marginBottom: 14 }}>
        {kpi(<Banknote size={18} />, "Efectivo confirmado", `S/ ${efectivoConfirmado.toFixed(2)}`, C.verde)}
        {kpi(<Receipt size={18} />, "Ingreso confirmado", `S/ ${totalConfirmado.toFixed(2)}`, C.oscuro)}
        {kpi(<BarChart3 size={18} />, "Pagos registrados", pagos.length, C.naranja)}
        {kpi(<Ticket size={18} />, "Boletos emitidos", cargados ? boletos.length : "—", C.verde)}
      </div>

      <div style={{ background: `${C.naranja}14`, border: `1px solid ${C.naranja}55`, borderRadius: 10, padding: "9px 12px", marginBottom: 14, fontSize: 12.5, color: "#8A5600", display: "flex", gap: 8, alignItems: "flex-start" }}>
        <Info size={15} style={{ flexShrink: 0, marginTop: 1 }} />
        <span>La API de pagos (<code>/api/v1/pagos</code>) no expone el cajero que registró cada movimiento, por lo que no se filtra por cajero para no atribuir datos falsos. Se muestran los movimientos reales a los que ROLE_CAJERO tiene acceso.</span>
      </div>

      <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center", marginBottom: 12 }}>
        <div style={{ display: "flex", gap: 8 }}>
          <button className={tab === "pagos" ? "adm-btn-primary" : "adm-btn-secondary"} onClick={() => setTab("pagos")}>Pagos</button>
          <button className={tab === "boletos" ? "adm-btn-primary" : "adm-btn-secondary"} onClick={() => { setTab("boletos"); if (!cargados && !loadBoletos) { setCargados(true); cargarBoletos(); } }}>Boletos emitidos</button>
        </div>
        <input value={q} onChange={e => setQ(e.target.value)} placeholder="Buscar pasajero, referencia…" style={{ flex: "1 1 200px", background: "white", border: `1.5px solid ${C.grisClaro}`, borderRadius: 8, padding: "9px 11px", fontSize: 13, color: C.oscuro }} />
        <input type="date" value={desde} onChange={e => setDesde(e.target.value)} style={{ background: "white", border: `1.5px solid ${C.grisClaro}`, borderRadius: 8, padding: "8px 10px", fontSize: 12.5, color: C.oscuro }} />
        <input type="date" value={hasta} onChange={e => setHasta(e.target.value)} style={{ background: "white", border: `1.5px solid ${C.grisClaro}`, borderRadius: 8, padding: "8px 10px", fontSize: 12.5, color: C.oscuro }} />
      </div>

      {tab === "pagos" && (
        loadPagos ? <div style={{ textAlign: "center", padding: "2rem", color: C.medio }}><Loader2 size={22} className="spin-icon" /></div>
          : pagosF.length === 0 ? <div style={{ textAlign: "center", padding: "2rem", color: C.medio }}><Search size={24} style={{ opacity: 0.5 }} /><div style={{ fontWeight: 700 }}>Sin pagos para el filtro</div></div>
            : (
              <div className="adm-table-wrap">
                <table className="adm-table">
                  <thead>
                    <tr><th>Fecha</th><th>Pasajero / cliente</th><th>Concepto</th><th>Referencia</th><th>Método</th><th>Importe</th><th>Estado</th></tr>
                  </thead>
                  <tbody>
                    {pagosF.map(p => (
                      <tr key={p.id} className="tr-hover">
                        <td style={{ fontSize: 12 }}>{fmtFecha(p.fechaPago)}</td>
                        <td style={{ fontWeight: 600 }}>{p.cliente || "Cliente Varios"}</td>
                        <td style={{ fontSize: 12, color: C.medio }}>{p.referenciaEntidad || "Pago en caja"}</td>
                        <td><span style={{ fontFamily: "monospace", fontSize: 12 }}>{p.referencia || "—"}</span></td>
                        <td><span style={{ fontWeight: 700, fontSize: 12 }}>{p.metodo}</span></td>
                        <td><strong style={{ color: p.estado === "COMPLETADO" ? C.verde : C.medio }}>S/ {Number(p.monto || 0).toFixed(2)}</strong></td>
                        <td><span style={{ fontWeight: 700, color: p.estado === "COMPLETADO" ? "#158463" : p.estado === "PENDIENTE" ? C.naranja : C.rojo }}>{p.estado}</span></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )
      )}

      {tab === "boletos" && (
        loadBoletos ? <div style={{ textAlign: "center", padding: "2rem", color: C.medio }}><Loader2 size={22} className="spin-icon" /></div>
          : boletosF.length === 0 ? <div style={{ textAlign: "center", padding: "2rem", color: C.medio }}><Ticket size={24} style={{ opacity: 0.5 }} /><div style={{ fontWeight: 700 }}>Sin boletos emitidos para el filtro</div></div>
            : (
              <div className="adm-table-wrap">
                <table className="adm-table">
                  <thead>
                    <tr><th>Fecha</th><th>N° Boleto</th><th>Pasajero</th><th>Viaje</th><th>Asiento</th><th>Importe</th><th>Estado</th></tr>
                  </thead>
                  <tbody>
                    {boletosF.map(b => (
                      <tr key={b.id || b.numeroBoleto} className="tr-hover">
                        <td style={{ fontSize: 12 }}>{fmtFecha(b.createdAt)}</td>
                        <td><span className="td-codigo">{b.numeroBoleto}</span></td>
                        <td style={{ fontWeight: 600 }}>{b.cliente?.nombreCompleto || "—"}</td>
                        <td style={{ fontSize: 12 }}>{b.viaje?.ruta?.origen?.ciudad} → {b.viaje?.ruta?.destino?.ciudad}</td>
                        <td>N°{b.asiento?.numeroAsiento || "—"}</td>
                        <td><strong style={{ color: C.verde }}>S/ {Number(b.precioPagado ?? 0).toFixed(2)}</strong></td>
                        <td><span style={{ fontWeight: 700, color: b.estado === "ACTIVO" ? "#158463" : C.medio }}>{b.estado}</span></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )
      )}
    </div>
  );
}
