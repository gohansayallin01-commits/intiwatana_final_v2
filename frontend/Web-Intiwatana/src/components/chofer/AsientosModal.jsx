import { Fragment, useCallback, useEffect, useMemo, useRef, useState } from "react";
import { X, Armchair, AlertCircle } from "lucide-react";
import { obtenerAsientos } from "./apiChofer";
import { ESTADOS_ASIENTO, fmtFechaHora, rutaDe } from "./choferFormato";

/** Agrupa los asientos reales por piso y fila, ordenando por columna. */
function agrupar(asientos) {
  const pisos = new Map();
  for (const a of asientos) {
    const piso = a.piso ?? 1;
    if (!pisos.has(piso)) pisos.set(piso, new Map());
    const filas = pisos.get(piso);
    const fila = a.fila ?? 1;
    if (!filas.has(fila)) filas.set(fila, []);
    filas.get(fila).push(a);
  }
  return [...pisos.entries()]
    .sort((a, b) => a[0] - b[0])
    .map(([piso, filas]) => ({
      piso,
      filas: [...filas.entries()]
        .sort((a, b) => a[0] - b[0])
        .map(([fila, lista]) => ({
          fila,
          asientos: lista.sort((x, y) => (x.columna ?? 0) - (y.columna ?? 0)),
        })),
    }));
}

/**
 * Mapa de asientos del viaje.
 * Datos: GET /api/v1/viajes/{id}/asientos (estado y pasajero reales).
 */
export default function AsientosModal({ token, viaje, onClose }) {
  const [asientos, setAsientos] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');
  const cerrarRef = useRef(null);

  const cargar = useCallback(async () => {
    setCargando(true);
    setError('');
    try {
      setAsientos(await obtenerAsientos(token, viaje.id));
    } catch (e) {
      setError(e.message || 'No se pudieron cargar los asientos.');
    } finally {
      setCargando(false);
    }
  }, [token, viaje.id]);

  useEffect(() => {
    cargar();
  }, [cargar]);

  useEffect(() => {
    const onKey = (e) => {
      if (e.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [onClose]);

  // El foco entra al diálogo y el fondo no se desplaza mientras está abierto.
  useEffect(() => {
    cerrarRef.current?.focus();
    const previo = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = previo;
    };
  }, []);

  const grupos = useMemo(() => agrupar(asientos), [asientos]);
  const conteo = useMemo(() => {
    const c = { DISPONIBLE: 0, VENDIDO: 0, RESERVADO: 0, BLOQUEADO: 0 };
    for (const a of asientos) if (c[a.estado] !== undefined) c[a.estado] += 1;
    return c;
  }, [asientos]);

  const { origen, destino } = rutaDe(viaje);

  return (
    <div className="cd-modal-overlay" onClick={onClose}>
      <div
        className="cd-modal"
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
        aria-labelledby="cd-modal-asientos"
      >
        <div className="cd-modal-head">
          <div style={{ flex: 1, minWidth: 0 }}>
            <h2 className="cd-modal-title" id="cd-modal-asientos">Asientos · {origen} → {destino}</h2>
            <p className="cd-modal-sub">
              {viaje.bus?.placa ? `Placa ${viaje.bus.placa} · ` : ''}
              Salida {fmtFechaHora(viaje.fechaHoraSalida)} · Viaje #{viaje.id}
            </p>
          </div>
          <button ref={cerrarRef} type="button" className="cd-btn cd-btn-icon" onClick={onClose} aria-label="Cerrar mapa de asientos">
            <X size={18} />
          </button>
        </div>

        <div className="cd-modal-body">
          {cargando && (
            <div className="cd-cargando">
              <span className="cd-spinner" />
              Cargando asientos...
            </div>
          )}

          {!cargando && error && (
            <div className="cd-alerta cd-alerta-error">
              <AlertCircle size={18} />
              <div style={{ flex: 1 }}>
                <div>{error}</div>
                <button type="button" className="cd-btn cd-btn-outline cd-btn-sm" style={{ marginTop: 10 }} onClick={cargar}>
                  Reintentar
                </button>
              </div>
            </div>
          )}

          {!cargando && !error && asientos.length === 0 && (
            <div className="cd-vacio">
              <Armchair size={28} />
              <strong>Este viaje no tiene asientos generados</strong>
              <div>La API devolvió una lista vacía para el viaje #{viaje.id}.</div>
            </div>
          )}

          {!cargando && !error && asientos.length > 0 && (
            <>
              <div className="cd-bus" role="group" aria-label={`Mapa de ${asientos.length} asientos del bus`}>
                <div className="cd-bus-cabina">Cabina del conductor</div>
                {grupos.map((g) => (
                  <div key={g.piso}>
                    {grupos.length > 1 && <div className="cd-piso-label">Piso {g.piso}</div>}
                    {g.filas.map((f) => (
                      <div
                        className="cd-fila-asientos"
                        key={`${g.piso}-${f.fila}`}
                        role="group"
                        aria-label={`Fila ${f.fila}`}
                      >
                        {f.asientos.map((a, i) => {
                          const estado = a.estado || 'BLOQUEADO';
                          const detalle = `${a.numeroAsiento} · ${ESTADOS_ASIENTO[estado]?.label || estado}${a.pasajero ? ` · ${a.pasajero}` : ''}`;
                          return (
                            <Fragment key={a.id ?? `${f.fila}-${i}`}>
                              <div
                                className={`cd-asiento cd-asiento-${estado}`}
                                title={detalle}
                                aria-label={detalle}
                              >
                                <span className="cd-asiento-num">{a.numeroAsiento}</span>
                                {a.pasajero ? <span className="cd-asiento-pas">{a.pasajero}</span> : null}
                              </div>
                              {i === 1 && <div className="cd-pasillo" aria-hidden="true" />}
                            </Fragment>
                          );
                        })}
                      </div>
                    ))}
                  </div>
                ))}
              </div>

              <div className="cd-leyenda">
                {Object.entries(ESTADOS_ASIENTO).map(([clave, info]) => (
                  <span className="cd-leyenda-item" key={clave}>
                    <span
                      className="cd-leyenda-dot"
                      style={{ background: info.bg, borderColor: info.borde }}
                      aria-hidden="true"
                    />
                    {info.label} ({conteo[clave]})
                  </span>
                ))}
              </div>
            </>
          )}
        </div>

        <div className="cd-modal-foot">
          <button type="button" className="cd-btn cd-btn-outline" onClick={onClose}>
            Cerrar
          </button>
        </div>
      </div>
    </div>
  );
}