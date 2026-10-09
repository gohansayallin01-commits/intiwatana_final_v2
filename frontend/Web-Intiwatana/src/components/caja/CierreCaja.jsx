import { useState, useEffect, useCallback, useMemo } from "react";
import {
  CheckCircle2, RefreshCw, Loader2, Banknote, Ticket, Receipt,
  MapPin, User, Clock, Printer, Eye, EyeOff, Info,
} from "lucide-react";
import { PageHeader } from "../AdminDashboard";
import { useAuth } from "../../context/useAuth";

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

function hoy() { return new Date().toISOString().slice(0, 10); }

export default function CierreCaja({ headers, showToast }) {
  const { session } = useAuth();
  const [desde, setDesde] = useState(hoy());
  const [hasta, setHasta] = useState(hoy());
  const [pagos, setPagos] = useState([]);
  const [boletos, setBoletos] = useState([]);
  const [reservas, setReservas] = useState([]);
  const [load, setLoad] = useState(true);
  const [verDetalle, setVerDetalle] = useState(true);

  const cargar = useCallback(async () => {
    setLoad(true);
    try {
      const [rp, rv, rr] = await Promise.all([
        fetch("/api/v1/pagos", { headers }),
        fetch("/api/v1/viajes", { headers }),
        fetch("/api/v1/ventas/reservas?estado=CONFIRMADO", { headers }),
      ]);
      setPagos(rp.ok ? await rp.json() : []);
      setReservas(rr.ok ? await rr.json() : []);
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
    } catch { showToast("Error al cargar datos de caja"); }
    finally { setLoad(false); }
  }, [headers, showToast]);

  useEffect(() => { cargar(); }, [cargar]);

  const enRango = useCallback((iso) => {
    if (!iso) return false;
    const t = new Date(iso).getTime();
    if (desde && t < new Date(`${desde}T00:00:00`).getTime()) return false;
    if (hasta && t > new Date(`${hasta}T23:59:59`).getTime()) return false;
    return true;
  }, [desde, hasta]);

  const pagosPeriodo = useMemo(() => (Array.isArray(pagos) ? pagos : []).filter(p => p.estado === "COMPLETADO" && enRango(p.fechaPago)), [pagos, enRango]);
  const boletosPeriodo = useMemo(() => (Array.isArray(boletos) ? boletos : []).filter(b => enRango(b.createdAt)), [boletos, enRango]);
  const reservasPeriodo = useMemo(() => (Array.isArray(reservas) ? reservas : []).filter(r => enRango(r.confirmadaEn || r.createdAt || r.fechaHoraSalida)), [reservas, enRango]);

  const efectivo = pagosPeriodo.filter(p => p.metodo === "EFECTIVO").reduce((s, p) => s + Number(p.monto || 0), 0);
  const ingreso = pagosPeriodo.reduce((s, p) => s + Number(p.monto || 0), 0);
  const ventasEfectivo = pagosPeriodo.filter(p => p.metodo === "EFECTIVO").length;
  const viajesIncluidos = new Set(boletosPeriodo.map(b => b.viaje?.id).filter(Boolean)).size;

  const kpi = (icon, label, value, color, help) => (
    <div style={{ background: C.blanco, border: `1.5px solid ${C.grisClaro}`, borderRadius: 12, padding: "13px 15px" }}>
      <div style={{ display: "flex", alignItems: "center", gap: 9, marginBottom: 6 }}>
        <span style={{ width: 34, height: 34, borderRadius: 10, background: `${color}1a`, color, display: "inline-flex", alignItems: "center", justifyContent: "center" }}>{icon}</span>
        <span style={{ fontSize: 11, color: C.medio, textTransform: "uppercase", letterSpacing: "0.06em" }}>{label}</span>
      </div>
      <div style={{ fontWeight: 800, color: C.oscuro, fontSize: 20 }}>{value}</div>
      {help && <div style={{ fontSize: 11, color: C.medio, marginTop: 2 }}>{help}</div>}
    </div>
  );

  return (
    <div>
      <style>{`@media print{body *{visibility:hidden!important}.caja-print,.caja-print *{visibility:visible!important}.caja-print{position:absolute;left:0;top:0;width:100%;padding:16px}}`}</style>

      <PageHeader title="Resumen de Caja" sub="Resumen operativo derivado de movimientos confirmados (no es un cierre contable)" action={
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
          <button className="adm-btn-secondary" onClick={() => setVerDetalle(v => !v)}>{verDetalle ? <EyeOff size={14} style={{ marginRight: 6 }} /> : <Eye size={14} style={{ marginRight: 6 }} />}{verDetalle ? "Ocultar detalle" : "Ver detalle"}</button>
          <button className="adm-btn-secondary" onClick={() => window.print()}>{Printer && <Printer size={14} style={{ marginRight: 6 }} />}Imprimir</button>
          <button className="adm-btn-secondary" onClick={cargar} disabled={load}><RefreshCw size={14} className={load ? "spin-icon" : undefined} style={{ marginRight: 6 }} />Actualizar</button>
        </div>
      } />

      <div className="caja-print">
        <div style={{ background: C.blanco, border: `1.5px solid ${C.grisClaro}`, borderRadius: 14, padding: "1rem 1.2rem", marginBottom: 14, display: "flex", flexWrap: "wrap", gap: 16 }}>
          <div style={{ display: "flex", alignItems: "center", gap: 8, color: C.oscuro }}><User size={15} color={C.verde} /><span style={{ fontSize: 12, color: C.medio }}>Cajero:</span><strong>{session?.nombreCompleto || session?.username || "—"}</strong></div>
          <div style={{ display: "flex", alignItems: "center", gap: 8, color: C.oscuro }}><MapPin size={15} color={C.verde} /><span style={{ fontSize: 12, color: C.medio }}>Sucursal:</span><strong>{session?.sucursalNombre || "—"}</strong></div>
          <div style={{ display: "flex", alignItems: "center", gap: 8, color: C.oscuro }}><Clock size={15} color={C.verde} /><span style={{ fontSize: 12, color: C.medio }}>Periodo:</span><strong>{desde || "—"} → {hasta || "—"}</strong></div>
        </div>

        <div style={{ display: "flex", gap: 8, flexWrap: "wrap", alignItems: "center", marginBottom: 14 }}>
          <label style={{ fontSize: 12, color: C.medio }}>Desde</label>
          <input type="date" value={desde} onChange={e => setDesde(e.target.value)} style={{ background: "white", border: `1.5px solid ${C.grisClaro}`, borderRadius: 8, padding: "8px 10px", fontSize: 12.5, color: C.oscuro }} />
          <label style={{ fontSize: 12, color: C.medio }}>Hasta</label>
          <input type="date" value={hasta} onChange={e => setHasta(e.target.value)} style={{ background: "white", border: `1.5px solid ${C.grisClaro}`, borderRadius: 8, padding: "8px 10px", fontSize: 12.5, color: C.oscuro }} />
        </div>

        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit,minmax(200px,1fr))", gap: 12, marginBottom: 14 }}>
          {kpi(<Banknote size={18} />, "Efectivo confirmado", `S/ ${efectivo.toFixed(2)}`, C.verde, "Pagos en efectivo COMPLETADO del periodo")}
          {kpi(<Receipt size={18} />, "Ingreso confirmado", `S/ ${ingreso.toFixed(2)}`, C.oscuro, "Todos los pagos COMPLETADO del periodo")}
          {kpi(<CheckCircle2 size={18} />, "N° operaciones", pagosPeriodo.length, C.naranja)}
          {kpi(<Banknote size={18} />, "Ventas presenciales", ventasEfectivo, C.verde)}
          {kpi(<Ticket size={18} />, "Reservas cobradas", reservasPeriodo.length, C.oscuro)}
          {kpi(<Ticket size={18} />, "Boletos emitidos", boletosPeriodo.length, C.verde)}
          {kpi(<MapPin size={18} />, "Viajes incluidos", viajesIncluidos, C.oscuro)}
        </div>

        <div style={{ background: `${C.medio}12`, border: `1px solid ${C.grisClaro}`, borderRadius: 10, padding: "9px 12px", marginBottom: 14, fontSize: 12.5, color: C.medio, display: "flex", gap: 8, alignItems: "flex-start" }}>
          <Info size={15} style={{ flexShrink: 0, marginTop: 1 }} />
          <span>Este resumen es derivado: <strong>no existe</strong> apertura, saldo inicial, efectivo contado, diferencia ni estado ABIERTA/CERRADA en el modelo actual. Los importes provienen únicamente de pagos confirmados y boletos emitidos. Excluye pendientes y operaciones no confirmadas. Solo cuenta con los datos disponibles para el rol autorizado.</span>
        </div>

        {verDetalle && (
          load ? <div style={{ textAlign: "center", padding: "2rem", color: C.medio }}><Loader2 size={22} className="spin-icon" /></div>
            : (
              <div className="adm-table-wrap">
                <table className="adm-table">
                  <thead>
                    <tr><th>Fecha</th><th>Pasajero / cliente</th><th>Concepto</th><th>Método</th><th>Importe</th><th>Estado</th></tr>
                  </thead>
                  <tbody>
                    {pagosPeriodo.length === 0
                      ? <tr><td colSpan={6} style={{ textAlign: "center", padding: "1.5rem", color: C.medio }}>Sin movimientos confirmados en el periodo</td></tr>
                      : pagosPeriodo.map(p => (
                        <tr key={p.id} className="tr-hover">
                          <td style={{ fontSize: 12 }}>{fmtFecha(p.fechaPago)}</td>
                          <td style={{ fontWeight: 600 }}>{p.cliente || "Cliente Varios"}</td>
                          <td style={{ fontSize: 12, color: C.medio }}>{p.referenciaEntidad || "Pago en caja"}</td>
                          <td style={{ fontWeight: 700, fontSize: 12 }}>{p.metodo}</td>
                          <td><strong style={{ color: C.verde }}>S/ {Number(p.monto || 0).toFixed(2)}</strong></td>
                          <td><span style={{ fontWeight: 700, color: "#158463" }}>{p.estado}</span></td>
                        </tr>
                      ))}
                  </tbody>
                </table>
              </div>
            )
        )}
      </div>
    </div>
  );
}
