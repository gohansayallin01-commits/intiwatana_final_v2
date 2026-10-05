import { useState, useEffect, useCallback, useMemo } from "react";
import {
  BarChart3, Download, RefreshCw, TrendingUp, Ticket, Package,
  Building2, Armchair, AlertCircle, CreditCard, CalendarDays, X,
} from "lucide-react";

const C = {
  verde: "#127369", oscuro: "#0d3330", amarillo: "#F5C518",
  medio: "#7a9e9b", gris: "#4C5958", grisClaro: "#8AA6A3",
  fondo: "#f0f4f3", blanco: "#ffffff", rojo: "#e05252", naranja: "#e67e22",
};

const SOLES = new Intl.NumberFormat("es-PE", { style: "currency", currency: "PEN", minimumFractionDigits: 2 });
const NUM = new Intl.NumberFormat("es-PE");

const dinero = (v) => SOLES.format(Number(v || 0));
const entero = (v) => NUM.format(Number(v || 0));
const pct = (v) => `${Number(v || 0).toFixed(2)}%`;

/** Formatea una fecha para el parámetro ISO que espera el backend. */
function aIso(d) {
  const p = (n) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}:00`;
}

/** 'YYYY-MM-DD' a partir de un input type="date". */
function aIsoFecha(v) {
  return v ? `${v}T00:00:00` : null;
}

function desdeHastaPreset(preset) {
  const ahora = new Date();
  const fin = new Date(ahora.getFullYear(), ahora.getMonth(), ahora.getDate(), 23, 59, 59);
  let inicio;
  if (preset === "hoy") {
    inicio = new Date(ahora.getFullYear(), ahora.getMonth(), ahora.getDate(), 0, 0, 0);
  } else if (preset === "7") {
    inicio = new Date(fin);
    inicio.setDate(inicio.getDate() - 6);
  } else if (preset === "mes") {
    inicio = new Date(ahora.getFullYear(), ahora.getMonth(), 1, 0, 0, 0);
  } else {
    inicio = new Date(fin);
    inicio.setDate(inicio.getDate() - 29);
  }
  return { desde: aIso(inicio), hasta: aIso(fin) };
}

const PRESETS = [
  { key: "hoy", label: "Hoy" },
  { key: "7", label: "7 días" },
  { key: "30", label: "30 días" },
  { key: "mes", label: "Mes actual" },
];

const PESTANAS = [
  { key: "resumen", label: "Resumen", icon: <BarChart3 size={15} /> },
  { key: "ingresos", label: "Ingresos", icon: <CreditCard size={15} /> },
  { key: "pasajes", label: "Pasajes y ocupación", icon: <Ticket size={15} /> },
  { key: "encomiendas", label: "Encomiendas", icon: <Package size={15} /> },
  { key: "sucursales", label: "Sucursales", icon: <Building2 size={15} /> },
];

// ── Piezas de presentación ────────────────────────────────────────────────

function Kpi({ icon, label, value, sub, color = C.verde }) {
  return (
    <div style={{
      background: C.blanco, border: "1px solid #e2edeb", borderRadius: 12, padding: "14px 16px",
      display: "flex", alignItems: "center", gap: 12, boxShadow: "0 1px 6px rgba(16,64,59,0.05)",
    }}>
      <div style={{
        width: 38, height: 38, borderRadius: 10, flexShrink: 0, display: "flex",
        alignItems: "center", justifyContent: "center", color, background: `${color}18`,
      }}>{icon}</div>
      <div style={{ minWidth: 0 }}>
        <div style={{ fontSize: 10, color: C.medio, textTransform: "uppercase", letterSpacing: "0.06em" }}>{label}</div>
        <div style={{ fontSize: 18, fontWeight: 800, color: C.oscuro, lineHeight: 1.25 }}>{value}</div>
        {sub && <div style={{ fontSize: 10.5, color: C.grisClaro }}>{sub}</div>}
      </div>
    </div>
  );
}

function Tabla({ columnas, filas, vacio = "Sin datos en el período seleccionado" }) {
  if (!filas || filas.length === 0) {
    return (
      <div style={{ padding: "28px 16px", textAlign: "center", color: C.medio, fontSize: 13 }}>
        {vacio}
      </div>
    );
  }
  return (
    <div style={{ overflowX: "auto" }}>
      <table style={{ width: "100%", borderCollapse: "collapse", fontSize: 12.5 }}>
        <thead>
          <tr>
            {columnas.map((c) => (
              <th key={c.key} style={{
                textAlign: c.align || "left", padding: "9px 12px", color: C.gris,
                fontSize: 10.5, textTransform: "uppercase", letterSpacing: "0.05em",
                borderBottom: "1.5px solid #e2edeb", whiteSpace: "nowrap", fontWeight: 700,
              }}>{c.label}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {filas.map((f, i) => (
            <tr key={f.__key ?? i} style={{ background: i % 2 ? "#fafcfc" : "transparent" }}>
              {columnas.map((c) => (
                <td key={c.key} style={{
                  textAlign: c.align || "left", padding: "9px 12px",
                  borderBottom: "1px solid #eef4f3", color: C.gris,
                  whiteSpace: "nowrap", fontWeight: c.fuerte ? 700 : 400,
                }}>{c.render ? c.render(f) : (f[c.key] ?? "—")}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function Barra({ serie }) {
  if (!serie || serie.length === 0) {
    return <div style={{ padding: "20px 0", textAlign: "center", color: C.medio, fontSize: 12.5 }}>Sin ingresos en el período</div>;
  }
  const max = Math.max(...serie.map((d) => Number(d.total || 0)), 1);
  return (
    <div style={{ display: "flex", alignItems: "flex-end", gap: 6, height: 170, padding: "8px 0" }}>
      {serie.map((d) => {
        const h = Math.max(3, (Number(d.total || 0) / max) * 150);
        return (
          <div key={d.fecha} style={{ flex: 1, display: "flex", flexDirection: "column", alignItems: "center", gap: 4, minWidth: 0 }}>
            <div style={{ fontSize: 9, color: C.grisClaro, whiteSpace: "nowrap" }}>{Number(d.total || 0) > 0 ? Number(d.total).toFixed(0) : ""}</div>
            <div
              title={`${d.fecha}: ${dinero(d.total)}`}
              style={{
                width: "100%", maxWidth: 40, height: h, borderRadius: "4px 4px 0 0",
                background: `linear-gradient(180deg, ${C.verde}, ${C.oscuro})`,
              }}
            />
            <div style={{ fontSize: 9, color: C.medio, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis", maxWidth: "100%" }}>
              {String(d.fecha).slice(5)}
            </div>
          </div>
        );
      })}
    </div>
  );
}

function BotonCsv({ onClick, children = "Descargar CSV", disabled }) {
  return (
    <button
      onClick={onClick}
      disabled={disabled}
      style={{
        display: "inline-flex", alignItems: "center", gap: 6, background: C.blanco, color: C.verde,
        border: "1.5px solid #cfe3e0", borderRadius: 8, padding: "7px 13px", fontSize: 12,
        fontWeight: 700, cursor: disabled ? "not-allowed" : "pointer", opacity: disabled ? 0.55 : 1,
        fontFamily: "inherit", transition: "all 0.15s",
      }}
      onMouseEnter={(e) => { if (!disabled) e.currentTarget.style.background = "#f0f8f7"; }}
      onMouseLeave={(e) => { if (!disabled) e.currentTarget.style.background = C.blanco; }}
    >
      <Download size={14} /> {children}
    </button>
  );
}

// ── Pantalla ──────────────────────────────────────────────────────────────

export default function ReportesPage({ headers, showToast }) {
  const inicial = useMemo(() => desdeHastaPreset("30"), []);
  const [preset, setPreset] = useState("30");
  const [desde, setDesde] = useState(inicial.desde);
  const [hasta, setHasta] = useState(inicial.hasta);
  const [pestana, setPestana] = useState("resumen");
  const [cargando, setCargando] = useState(true);
  const [datos, setDatos] = useState({
    resumen: null, ingresos: null, serie: [],
    pasajes: null, ocupacion: [], encomiendas: null, sucursales: [],
  });

  const query = `desde=${encodeURIComponent(desde)}&hasta=${encodeURIComponent(hasta)}`;

  const cargar = useCallback(async () => {
    setCargando(true);
    try {
      const pedir = async (ruta) => {
        const r = await fetch(`/api/v1/reportes${ruta}?${query}`, { headers });
        if (!r.ok) throw new Error(`Error ${r.status} al cargar ${ruta}`);
        return r.json();
      };
      const [resumen, ingresos, serie, pasajes, ocupacion, encomiendas, sucursales] = await Promise.all([
        pedir("/resumen"), pedir("/ingresos"), pedir("/ingresos/serie-diaria"),
        pedir("/pasajes"), pedir("/ocupacion"), pedir("/encomiendas"), pedir("/sucursales"),
      ]);
      setDatos({ resumen, ingresos, serie, pasajes, ocupacion, encomiendas, sucursales });
    } catch (e) {
      showToast(e.message || "Error al cargar los reportes");
    } finally {
      setCargando(false);
    }
  }, [headers, query, showToast]);

  useEffect(() => { cargar(); }, [cargar]);

  const aplicarPreset = (key) => {
    setPreset(key);
    const p = desdeHastaPreset(key);
    setDesde(p.desde);
    setHasta(p.hasta);
  };

  const personalized = (campo, valor) => {
    setPreset("custom");
    if (campo === "desde") setDesde(aIsoFecha(valor));
    else setHasta(aIsoFecha(valor) ? `${valor}T23:59:59` : null);
  };

  const descargarCsv = async (ruta) => {
    try {
      const r = await fetch(`/api/v1/reportes${ruta}?${query}`, { headers });
      if (!r.ok) throw new Error(`Error ${r.status} al generar el CSV`);
      const blob = await r.blob();
      const disp = r.headers.get("Content-Disposition") || "";
      const match = disp.match(/filename="?([^";]+)"?/);
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = match ? match[1] : "reporte.csv";
      document.body.appendChild(a);
      a.click();
      a.remove();
      URL.revokeObjectURL(url);
      showToast("Reporte descargado");
    } catch (e) {
      showToast(e.message || "Error al descargar el reporte");
    }
  };

  const r = datos.resumen;

  return (
    <div style={{ padding: 22, overflowY: "auto", height: "100%", background: C.fondo }}>
      {/* Período */}
      <div style={{
        background: C.blanco, border: "1px solid #e2edeb", borderRadius: 12, padding: 14,
        display: "flex", flexWrap: "wrap", gap: 12, alignItems: "center", marginBottom: 16,
      }}>
        <div style={{ display: "flex", alignItems: "center", gap: 7, color: C.oscuro, fontWeight: 700, fontSize: 13 }}>
          <CalendarDays size={16} /> Período
        </div>
        <div style={{ display: "flex", gap: 5, flexWrap: "wrap" }}>
          {PRESETS.map((p) => (
            <button
              key={p.key}
              onClick={() => aplicarPreset(p.key)}
              style={{
                background: preset === p.key ? C.verde : C.fondo, color: preset === p.key ? C.blanco : C.gris,
                border: "none", borderRadius: 7, padding: "6px 12px", fontSize: 12, fontWeight: 600,
                cursor: "pointer", fontFamily: "inherit",
              }}
            >{p.label}</button>
          ))}
        </div>
        <div style={{ display: "flex", gap: 8, alignItems: "center", marginLeft: "auto", flexWrap: "wrap" }}>
          <input
            type="date" value={desde?.slice(0, 10) || ""} onChange={(e) => personalized("desde", e.target.value)}
            style={{ padding: "6px 9px", border: "1px solid #cfe3e0", borderRadius: 7, fontSize: 12, fontFamily: "inherit", color: C.gris }}
          />
          <span style={{ color: C.medio, fontSize: 12 }}>a</span>
          <input
            type="date" value={hasta?.slice(0, 10) || ""} onChange={(e) => personalized("hasta", e.target.value)}
            style={{ padding: "6px 9px", border: "1px solid #cfe3e0", borderRadius: 7, fontSize: 12, fontFamily: "inherit", color: C.gris }}
          />
          <button
            onClick={cargar} disabled={cargando}
            title="Actualizar"
            style={{
              display: "flex", alignItems: "center", justifyContent: "center", width: 32, height: 32,
              background: C.verde, color: C.blanco, border: "none", borderRadius: 8, cursor: "pointer", opacity: cargando ? 0.6 : 1,
            }}
          ><RefreshCw size={15} style={{ animation: cargando ? "spin 1s linear infinite" : "none" }} /></button>
        </div>
      </div>

      {/* Indicadores */}
      {r && (
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(190px, 1fr))", gap: 12, marginBottom: 16 }}>
          <Kpi icon={<CreditCard size={19} />} label="Ingresos totales" value={dinero(r.ingresosTotales)} sub={`${entero(r.transacciones)} transacciones`} />
          <Kpi icon={<Ticket size={19} />} label="Boletos vendidos" value={entero(r.boletosVendidos)} sub={`${entero(r.viajes)} viajes`} color={C.naranja} />
          <Kpi icon={<TrendingUp size={19} />} label="Ocupación promedio" value={pct(r.ocupacionPromedio)} sub={`Ticket medio ${dinero(r.precioPromedioPasaje)}`} color={C.amarillo} />
          <Kpi icon={<Package size={19} />} label="Encomiendas" value={entero(r.encomiendas)} sub={`${dinero(r.ingresosEncomiendas)} · ${Number(r.pesoTotalKg || 0).toFixed(1)} kg`} color="#7a5c9e" />
        </div>
      )}

      {/* Pestañas */}
      <div style={{ display: "flex", gap: 4, borderBottom: "1.5px solid #e2edeb", marginBottom: 16, flexWrap: "wrap" }}>
        {PESTANAS.map((p) => (
          <button
            key={p.key}
            onClick={() => setPestana(p.key)}
            style={{
              display: "flex", alignItems: "center", gap: 6, background: "transparent", border: "none",
              borderBottom: pestana === p.key ? `2.5px solid ${C.verde}` : "2.5px solid transparent",
              color: pestana === p.key ? C.verde : C.medio, padding: "9px 14px", fontSize: 12.5,
              fontWeight: 700, cursor: "pointer", fontFamily: "inherit", marginBottom: -1.5,
            }}
          >{p.icon} {p.label}</button>
        ))}
      </div>

      {cargando && (
        <div style={{ textAlign: "center", padding: 40, color: C.medio, fontSize: 13 }}>
          Consultando los datos del período…
        </div>
      )}

      {!cargando && (
        <>
          {pestana === "resumen" && (
            <div style={{ display: "grid", gap: 14 }}>
              <Panel titulo="Ingresos por método de pago" accion={<BotonCsv onClick={() => descargarCsv("/ingresos/csv")} />}>
                <Tabla
                  vacio="No hay pagos completados en el período"
                  columnas={[
                    { key: "metodo", label: "Método" },
                    { key: "cantidad", label: "Transacciones", align: "right", render: (f) => entero(f.cantidad) },
                    { key: "total", label: "Importe", align: "right", fuerte: true, render: (f) => dinero(f.total) },
                  ]}
                  filas={datos.ingresos?.porMetodo || []}
                />
              </Panel>
              <Panel titulo="Ventas por ruta" accion={<BotonCsv onClick={() => descargarCsv("/pasajes/csv")} />}>
                <Tabla
                  vacio="No hay viajes en el período"
                  columnas={[
                    { key: "origen", label: "Origen" },
                    { key: "destino", label: "Destino" },
                    { key: "viajes", label: "Viajes", align: "right", render: (f) => entero(f.viajes) },
                    { key: "boletos", label: "Boletos", align: "right", render: (f) => entero(f.boletos) },
                    { key: "ingresos", label: "Ingresos", align: "right", fuerte: true, render: (f) => dinero(f.ingresos) },
                  ]}
                  filas={datos.pasajes?.porRuta || []}
                />
              </Panel>
            </div>
          )}

          {pestana === "ingresos" && (
            <div style={{ display: "grid", gap: 14 }}>
              <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))", gap: 12 }}>
                <Kpi icon={<CreditCard size={19} />} label="Total" value={dinero(datos.ingresos?.total)} />
                <Kpi icon={<Ticket size={19} />} label="Pasajes" value={dinero(datos.ingresos?.totalPasajes)} color={C.naranja} />
                <Kpi icon={<Package size={19} />} label="Encomiendas" value={dinero(datos.ingresos?.totalEncomiendas)} color="#7a5c9e" />
              </div>
              <Panel titulo="Ingresos por día" accion={<BotonCsv onClick={() => descargarCsv("/ingresos/serie-diaria/csv")} />}>
                <Barra serie={datos.serie} />
              </Panel>
              <Panel titulo="Detalle por método" accion={<BotonCsv onClick={() => descargarCsv("/ingresos/csv")} />}>
                <Tabla
                  columnas={[
                    { key: "metodo", label: "Método" },
                    { key: "cantidad", label: "Transacciones", align: "right", render: (f) => entero(f.cantidad) },
                    { key: "total", label: "Importe", align: "right", fuerte: true, render: (f) => dinero(f.total) },
                  ]}
                  filas={datos.ingresos?.porMetodo || []}
                />
              </Panel>
            </div>
          )}

          {pestana === "pasajes" && (
            <div style={{ display: "grid", gap: 14 }}>
              <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))", gap: 12 }}>
                <Kpi icon={<Ticket size={19} />} label="Boletos vendidos" value={entero(datos.pasajes?.boletosVendidos)} />
                <Kpi icon={<CalendarDays size={19} />} label="Viajes" value={entero(datos.pasajes?.viajes)} color={C.naranja} />
                <Kpi icon={<TrendingUp size={19} />} label="Ticket promedio" value={dinero(datos.pasajes?.precioPromedio)} color={C.amarillo} />
              </div>
              <Panel titulo="Ocupación por viaje" accion={<BotonCsv onClick={() => descargarCsv("/ocupacion/csv")} />}>
                <Tabla
                  vacio="No hay viajes en el período"
                  columnas={[
                    { key: "placa", label: "Placa" },
                    { key: "estado", label: "Estado" },
                    { key: "totalAsientos", label: "Asientos", align: "right", render: (f) => entero(f.totalAsientos) },
                    { key: "ocupados", label: "Ocupados", align: "right", render: (f) => entero(f.ocupados) },
                    { key: "disponibles", label: "Libres", align: "right", render: (f) => entero(f.disponibles) },
                    {
                      key: "ocupacionPorcentaje", label: "Ocupación", align: "right", fuerte: true,
                      render: (f) => (
                        <div style={{ display: "flex", alignItems: "center", gap: 8, justifyContent: "flex-end" }}>
                          <div style={{ width: 70, height: 6, background: "#e2edeb", borderRadius: 3, overflow: "hidden" }}>
                            <div style={{
                              width: `${Math.min(100, Number(f.ocupacionPorcentaje || 0))}%`,
                              height: "100%", background: Number(f.ocupacionPorcentaje) >= 80 ? C.rojo : C.verde,
                            }} />
                          </div>
                          <span style={{ minWidth: 52 }}>{pct(f.ocupacionPorcentaje)}</span>
                        </div>
                      ),
                    },
                    { key: "ingresos", label: "Ingresos", align: "right", render: (f) => dinero(f.ingresos) },
                  ]}
                  filas={datos.ocupacion || []}
                />
              </Panel>
              <Panel titulo="Ventas por ruta" accion={<BotonCsv onClick={() => descargarCsv("/pasajes/csv")} />}>
                <Tabla
                  columnas={[
                    { key: "origen", label: "Origen" },
                    { key: "destino", label: "Destino" },
                    { key: "viajes", label: "Viajes", align: "right", render: (f) => entero(f.viajes) },
                    { key: "boletos", label: "Boletos", align: "right", render: (f) => entero(f.boletos) },
                    { key: "ingresos", label: "Ingresos", align: "right", fuerte: true, render: (f) => dinero(f.ingresos) },
                  ]}
                  filas={datos.pasajes?.porRuta || []}
                />
              </Panel>
            </div>
          )}

          {pestana === "encomiendas" && (
            <div style={{ display: "grid", gap: 14 }}>
              <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))", gap: 12 }}>
                <Kpi icon={<Package size={19} />} label="Encomiendas" value={entero(datos.encomiendas?.total)} />
                <Kpi icon={<CreditCard size={19} />} label="Facturación" value={dinero(datos.encomiendas?.ingresos)} color="#7a5c9e" />
                <Kpi icon={<Armchair size={19} />} label="Peso total" value={`${Number(datos.encomiendas?.pesoTotalKg || 0).toFixed(2)} kg`} color={C.naranja} />
              </div>
              <Panel titulo="Por estado" accion={<BotonCsv onClick={() => descargarCsv("/encomiendas/por-estado/csv")} />}>
                <Tabla
                  vacio="No hay encomiendas en el período"
                  columnas={[
                    { key: "estado", label: "Estado" },
                    { key: "cantidad", label: "Cantidad", align: "right", render: (f) => entero(f.cantidad) },
                    { key: "ingresos", label: "Importe", align: "right", fuerte: true, render: (f) => dinero(f.ingresos) },
                  ]}
                  filas={datos.encomiendas?.porEstado || []}
                />
              </Panel>
              <Panel titulo="Por corredor" accion={<BotonCsv onClick={() => descargarCsv("/encomiendas/por-ruta/csv")} />}>
                <Tabla
                  vacio="No hay encomiendas en el período"
                  columnas={[
                    { key: "origen", label: "Origen" },
                    { key: "destino", label: "Destino" },
                    { key: "cantidad", label: "Cantidad", align: "right", render: (f) => entero(f.cantidad) },
                    { key: "pesoKg", label: "Peso (kg)", align: "right", render: (f) => Number(f.pesoKg || 0).toFixed(2) },
                    { key: "ingresos", label: "Ingresos", align: "right", fuerte: true, render: (f) => dinero(f.ingresos) },
                  ]}
                  filas={datos.encomiendas?.porRuta || []}
                />
              </Panel>
            </div>
          )}

          {pestana === "sucursales" && (
            <Panel titulo="Actividad por sucursal" accion={<BotonCsv onClick={() => descargarCsv("/sucursales/csv")} />}>
              <Tabla
                vacio="Ninguna sucursal tuvo actividad en el período"
                columnas={[
                  { key: "sucursal", label: "Sucursal", fuerte: true },
                  { key: "ciudad", label: "Ciudad" },
                  { key: "esTerminal", label: "Terminal", render: (f) => (f.esTerminal ? "Sí" : "No") },
                  { key: "viajesOrigen", label: "Viajes origen", align: "right", render: (f) => entero(f.viajesOrigen) },
                  { key: "viajesDestino", label: "Viajes destino", align: "right", render: (f) => entero(f.viajesDestino) },
                  { key: "boletosVendidos", label: "Boletos", align: "right", render: (f) => entero(f.boletosVendidos) },
                  { key: "encomiendasRegistradas", label: "Encomiendas", align: "right", render: (f) => entero(f.encomiendasRegistradas) },
                  { key: "ingresosTotales", label: "Ingresos totales", align: "right", fuerte: true, render: (f) => dinero(f.ingresosTotales) },
                ]}
                filas={datos.sucursales || []}
              />
            </Panel>
          )}
        </>
      )}

      <style>{`@keyframes spin{to{transform:rotate(360deg)}}`}</style>
    </div>
  );
}

function Panel({ titulo, accion, children }) {
  return (
    <div style={{ background: C.blanco, border: "1px solid #e2edeb", borderRadius: 12, overflow: "hidden", boxShadow: "0 1px 6px rgba(16,64,59,0.05)" }}>
      <div style={{
        display: "flex", alignItems: "center", justifyContent: "space-between", gap: 12,
        padding: "12px 16px", borderBottom: "1px solid #eef4f3",
      }}>
        <div style={{ display: "flex", alignItems: "center", gap: 8, color: C.oscuro, fontWeight: 700, fontSize: 13 }}>
          <BarChart3 size={15} color={C.verde} /> {titulo}
        </div>
        {accion}
      </div>
      {children}
    </div>
  );
}
