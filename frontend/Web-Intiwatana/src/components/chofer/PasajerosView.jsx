import { useCallback, useEffect, useState } from "react";
import { AlertCircle, RefreshCw, Users, Armchair } from "lucide-react";
import SelectorViaje from "./SelectorViaje";
import { obtenerPasajeros } from "./apiChofer";
import { claseEstadoBojeto, fmtFecha, fmtHora, fmtMoneda, ordenarViajes, rutaDe } from "./choferFormato";

/**
 * Pasajeros del viaje: relación real viaje → boleto → cliente.
 * Datos: GET /api/v1/boletos/viaje/{id} (Page<BoletoResponse>).
 */
export default function PasajerosView({ token, viajes }) {
  const ordenados = ordenarViajes(viajes);
  const [viajeId, setViajeId] = useState(() => (ordenados[0] ? String(ordenados[0].id) : ''));
  const [boletos, setBoletos] = useState([]);
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState('');

  const cargar = useCallback(async () => {
    if (!viajeId) {
      setBoletos([]);
      return;
    }
    setCargando(true);
    setError('');
    try {
      setBoletos(await obtenerPasajeros(token, viajeId));
    } catch (e) {
      setBoletos([]);
      setError(e.message || 'No se pudieron cargar los pasajeros.');
    } finally {
      setCargando(false);
    }
  }, [token, viajeId]);

  useEffect(() => {
    cargar();
  }, [cargar]);

  // Al entrar por URL directa los viajes aún no han llegado desde la API, por lo
  // que la selección inicial queda vacía: se elige el primer viaje propio.
  useEffect(() => {
    if (!viajeId && ordenados.length > 0) setViajeId(String(ordenados[0].id));
  }, [ordenados, viajeId]);

  const viaje = ordenados.find((v) => String(v.id) === String(viajeId));
  const { origen, destino } = viaje ? rutaDe(viaje) : { origen: '', destino: '' };

  if (ordenados.length === 0) {
    return (
      <div className="cd-card cd-vacio">
        <Users size={30} />
        <strong>No tienes viajes asignados</strong>
        <div>No hay viajes propios desde los cuales listar pasajeros.</div>
      </div>
    );
  }

  const usados = boletos.filter((b) => b.estado === 'USADO').length;

  return (
    <>
      <div className="cd-section-head">
        <h2 className="cd-section-title">Pasajeros del viaje</h2>
        <span className="cd-section-note">Relación real viaje → boleto → cliente</span>
      </div>

      <div className="cd-card" style={{ padding: 18, marginBottom: 18 }}>
        <div className="cd-form-grid">
          <SelectorViaje viajes={ordenados} valor={viajeId} onChange={setViajeId} />
          <div className="cd-campo">
            <span className="cd-label">Resumen del viaje</span>
            {viaje ? (
              <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap', alignItems: 'center' }}>
                <span className="cd-dato-valor">{origen} → {destino}</span>
                <span className="cd-section-note">{fmtFecha(viaje.fechaHoraSalida)} {fmtHora(viaje.fechaHoraSalida)}</span>
                <span className="cd-section-note">{boletos.length} boleto(s) · {usados} usado(s)</span>
              </div>
            ) : (
              <span className="cd-section-note">Selecciona un viaje</span>
            )}
          </div>
        </div>
      </div>

      {cargando && (
        <div className="cd-cargando">
          <span className="cd-spinner" />
          Cargando pasajeros...
        </div>
      )}

      {!cargando && error && (
        <div className="cd-card cd-vacio">
          <AlertCircle size={28} color="#dc2626" />
          <strong>No se pudieron cargar los pasajeros</strong>
          <div style={{ marginBottom: 12 }}>{error}</div>
          <button type="button" className="cd-btn cd-btn-primary" onClick={cargar}>
            <RefreshCw size={16} />
            Reintentar
          </button>
        </div>
      )}

      {!cargando && !error && boletos.length === 0 && (
        <div className="cd-card cd-vacio">
          <Armchair size={28} />
          <strong>Este viaje no tiene pasajeros</strong>
          <div>La API devolvió 0 boletos para el viaje #{viajeId}.</div>
        </div>
      )}

      {!cargando && !error && boletos.length > 0 && (
        <div className="cd-card cd-tabla-wrap">
          <table className="cd-tabla">
            <thead>
              <tr>
                <th>Boleto</th>
                <th>Pasajero</th>
                <th>DNI / RUC</th>
                <th>Asiento</th>
                <th>Tipo</th>
                <th>Pago</th>
                <th>Estado</th>
              </tr>
            </thead>
            <tbody>
              {boletos.map((b) => (
                <tr key={b.id}>
                  <td className="cd-num" data-label="Boleto">{b.numeroBoleto}</td>
                  <td data-label="Pasajero">{b.cliente?.nombreCompleto || '—'}</td>
                  <td className="cd-num" data-label="DNI / RUC">{b.cliente?.dniRuc || '—'}</td>
                  <td data-label="Asiento">{b.asiento?.numeroAsiento || '—'}</td>
                  <td data-label="Tipo">{b.asiento?.tipo || '—'}</td>
                  <td className="cd-num" data-label="Pago">{fmtMoneda(b.precioPagado)}</td>
                  <td data-label="Estado"><span className={claseEstadoBojeto(b.estado)}>{b.estado}</span></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}