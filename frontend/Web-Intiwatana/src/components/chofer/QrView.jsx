import { useCallback, useEffect, useState } from "react";
import { QrCode, ScanLine, CheckCircle2, AlertCircle, RefreshCw, Info } from "lucide-react";
import SelectorViaje from "./SelectorViaje";
import { obtenerPasajeros, validarQr } from "./apiChofer";
import { claseEstadoBojeto, fmtFecha, fmtFechaHora, fmtHora, ordenarViajes, rutaDe } from "./choferFormato";

/** Título de presentación según el mensaje real devuelto por el backend. */
function clasificarError(mensaje) {
  const m = String(mensaje || '').toLowerCase();
  if (/usad|utilizad/.test(m)) return 'Boleto ya utilizado';
  if (/cancelad|anulad|reembolsad/.test(m)) return 'Boleto cancelado';
  if (/no est[aá] asignad|ajeno|no pertenece|no corresponde/.test(m)) return 'Viaje no asignado';
  if (/conectar|servidor|red|conexi[oó]n/.test(m)) return 'Sin conexión con el servidor';
  if (/inv[aá]lid|no encontr|no existe|no reconocid/.test(m)) return 'Código no reconocido';
  return 'Validación rechazada';
}

/**
 * Validación de QR del boleto.
 * El código QR que genera el backend (BoletoServiceImpl) contiene el
 * numeroBoleto, que es exactamente el parámetro que espera
 * POST /api/v1/boletos/escanear?codigoQr=. No se simula ningún resultado:
 * cada validación es una llamada real y el boleto queda USADO en la BD.
 */
export default function QrView({ token, viajes }) {
  const ordenados = ordenarViajes(viajes);
  const [codigo, setCodigo] = useState('');
  const [validando, setValidando] = useState(false);
  const [resultado, setResultado] = useState(null);
  const [error, setError] = useState('');

  const [viajeId, setViajeId] = useState(() => (ordenados[0] ? String(ordenados[0].id) : ''));
  const [boletos, setBoletos] = useState([]);
  const [cargandoLista, setCargandoLista] = useState(false);
  const [errorLista, setErrorLista] = useState('');

  const cargarLista = useCallback(async () => {
    if (!viajeId) {
      setBoletos([]);
      return;
    }
    setCargandoLista(true);
    setErrorLista('');
    try {
      setBoletos(await obtenerPasajeros(token, viajeId));
    } catch (e) {
      setBoletos([]);
      setErrorLista(e.message || 'No se pudieron cargar los boletos.');
    } finally {
      setCargandoLista(false);
    }
  }, [token, viajeId]);

  useEffect(() => {
    cargarLista();
  }, [cargarLista]);

  // Al entrar por URL directa los viajes aún no han llegado desde la API, por lo
  // que la selección inicial queda vacía: se elige el primer viaje propio.
  useEffect(() => {
    if (!viajeId && ordenados.length > 0) setViajeId(String(ordenados[0].id));
  }, [ordenados, viajeId]);

  const enviar = async (valorIngresado) => {
    const valor = String(valorIngresado || '').trim();
    if (!valor) {
      setError('Ingresa el código QR del boleto.');
      setResultado(null);
      return;
    }
    setValidando(true);
    setError('');
    setResultado(null);
    try {
      const boleto = await validarQr(token, valor);
      setResultado(boleto);
      setCodigo('');
      await cargarLista();
    } catch (err) {
      setError(err.message || 'No se pudo validar el código.');
    } finally {
      setValidando(false);
    }
  };

  if (ordenados.length === 0) {
    return (
      <div className="cd-card cd-vacio">
        <QrCode size={30} />
        <strong>No tienes viajes asignados</strong>
        <div>No hay viajes propios para validar boletos.</div>
      </div>
    );
  }

  const pendientes = boletos.filter((b) => b.estado === 'ACTIVO');

  return (
    <>
      <div className="cd-section-head">
        <h2 className="cd-section-title">Validar QR del boleto</h2>
        <span className="cd-section-note">POST /api/v1/boletos/escanear</span>
      </div>

      <div className="cd-card" style={{ padding: 18, marginBottom: 18 }}>
        <form onSubmit={(e) => { e.preventDefault(); enviar(codigo); }}>
          <div className="cd-form-grid" style={{ alignItems: 'end' }}>
            <div className="cd-campo">
              <label className="cd-label" htmlFor="codigo-qr">Código QR del boleto</label>
              <input
                id="codigo-qr"
                className="cd-input"
                placeholder="BOL-20261002-000032"
                value={codigo}
                onChange={(e) => setCodigo(e.target.value)}
                autoComplete="off"
                spellCheck="false"
              />
            </div>
            <button type="submit" className="cd-btn cd-btn-primary" disabled={validando}>
              <ScanLine size={16} />
              {validando ? 'Validando...' : 'Validar'}
            </button>
          </div>
          <div className="cd-alerta cd-alerta-neutra" style={{ marginTop: 12 }}>
            <Info size={16} />
            <span>
              El QR impreso en el boleto contiene su número (ej. <strong>BOL-20261002-000032</strong>). Se puede escribir
              a mano o copiar el número legible del boleto. El resultado y el cambio de estado a USADO provienen del
              backend.
            </span>
          </div>
        </form>
      </div>

      {error && (
        <div className="cd-resultado-error" style={{ marginBottom: 18 }} role="alert">
          <div className="cd-resultado-head">
            <AlertCircle size={18} color="#b91c1c" />
            {clasificarError(error)}
          </div>
          <div>{error}</div>
        </div>
      )}

      {resultado && (
        <div className="cd-resultado-ok" style={{ marginBottom: 18 }} role="status" aria-live="polite">
          <div className="cd-resultado-head">
            <CheckCircle2 size={18} color="#00844a" />
            Boleto validado
          </div>
          <div className="cd-datos-lista">
            <div className="cd-dato-item">
              <div className="cd-label">Pasajero</div>
              <div className="cd-dato-valor">{resultado.cliente?.nombreCompleto || '—'}</div>
            </div>
            <div className="cd-dato-item">
              <div className="cd-label">Boleto</div>
              <div className="cd-dato-valor">{resultado.numeroBoleto}</div>
            </div>
            <div className="cd-dato-item">
              <div className="cd-label">Asiento</div>
              <div className="cd-dato-valor">{resultado.asiento?.numeroAsiento || '—'}</div>
            </div>
            <div className="cd-dato-item">
              <div className="cd-label">DNI / RUC</div>
              <div className="cd-dato-valor">{resultado.cliente?.dniRuc || '—'}</div>
            </div>
            <div className="cd-dato-item">
              <div className="cd-label">Estado boleto</div>
              <div className="cd-dato-valor">
                <span className={claseEstadoBojeto(resultado.estado)}>{resultado.estado}</span>
              </div>
            </div>
            <div className="cd-dato-item">
              <div className="cd-label">Validado</div>
              <div className="cd-dato-valor">{fmtFechaHora(resultado.fechaHoraUso)}</div>
            </div>
          </div>
          <button type="button" className="cd-btn cd-btn-outline cd-btn-sm" style={{ marginTop: 12 }} onClick={() => setResultado(null)}>
            Validar otro boleto
          </button>
        </div>
      )}

      <div className="cd-section-head">
        <h2 className="cd-section-title">Boletos del viaje seleccionado</h2>
        <span className="cd-section-note">{pendientes.length} pendiente(s) de uso</span>
      </div>

      <div className="cd-card" style={{ padding: 18, marginBottom: 14 }}>
        <SelectorViaje viajes={ordenados} valor={viajeId} onChange={setViajeId} etiqueta="Viaje del chofer" />
      </div>

      {cargandoLista && (
        <div className="cd-cargando">
          <span className="cd-spinner" />
          Cargando boletos...
        </div>
      )}

      {!cargandoLista && errorLista && (
        <div className="cd-card cd-vacio">
          <AlertCircle size={28} color="#dc2626" />
          <strong>No se pudieron cargar los boletos</strong>
          <div style={{ marginBottom: 12 }}>{errorLista}</div>
          <button type="button" className="cd-btn cd-btn-primary" onClick={cargarLista}>
            <RefreshCw size={16} />
            Reintentar
          </button>
        </div>
      )}

      {!cargandoLista && !errorLista && boletos.length === 0 && (
        <div className="cd-card cd-vacio">
          <QrCode size={28} />
          <strong>Este viaje no tiene boletos</strong>
          <div>La API devolvió 0 boletos para el viaje #{viajeId}.</div>
        </div>
      )}

      {!cargandoLista && !errorLista && boletos.length > 0 && (
        <div className="cd-card cd-tabla-wrap">
          <table className="cd-tabla">
            <thead>
              <tr>
                <th>Código QR</th>
                <th>Pasajero</th>
                <th>Asiento</th>
                <th>Estado</th>
                <th style={{ textAlign: 'right' }}>Acción</th>
              </tr>
            </thead>
            <tbody>
              {boletos.map((b) => (
                <tr key={b.id}>
                  <td className="cd-num" data-label="Código QR">{b.codigoQr || b.numeroBoleto}</td>
                  <td data-label="Pasajero">{b.cliente?.nombreCompleto || '—'}</td>
                  <td data-label="Asiento">{b.asiento?.numeroAsiento || '—'}</td>
                  <td data-label="Estado"><span className={claseEstadoBojeto(b.estado)}>{b.estado}</span></td>
                  <td className="cd-td-accion" style={{ textAlign: 'right' }}>
                    {b.estado === 'ACTIVO' ? (
                      <button
                        type="button"
                        className="cd-btn cd-btn-azul cd-btn-sm"
                        disabled={validando}
                        onClick={() => enviar(b.codigoQr || b.numeroBoleto)}
                      >
                        <ScanLine size={14} />
                        Validar
                      </button>
                    ) : (
                      <span className="cd-section-note">—</span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {(() => {
        const v = ordenados.find((x) => String(x.id) === String(viajeId));
        if (!v) return null;
        const { origen, destino } = rutaDe(v);
        return (
          <p className="cd-section-note" style={{ marginTop: 12 }}>
            Viaje #{v.id} · {origen} → {destino} · {fmtFecha(v.fechaHoraSalida)} {fmtHora(v.fechaHoraSalida)}
          </p>
        );
      })()}
    </>
  );
}