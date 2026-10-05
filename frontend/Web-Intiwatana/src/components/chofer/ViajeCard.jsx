import { useState } from "react";
import { MapPin, Clock, Bus, Armchair, Play, Flag, LayoutGrid } from "lucide-react";
import { claseEstadoViaje, ESTADOS_VIAJE, fmtFecha, fmtHora, rutaDe } from "./choferFormato";

/**
 * Tarjeta de un viaje real.
 * Las acciones se derivan del estado real del viaje y de las transiciones que
 * el backend acepta (PROGRAMADO→EN_CURSO, EN_CURSO→FINALIZADO).
 */
export default function ViajeCard({ viaje, esActual, onIniciar, onFinalizar, onVerAsientos, enAccion }) {
  const [confirmando, setConfirmando] = useState(false);
  const { origen, destino } = rutaDe(viaje);
  const estado = viaje.estado;
  const disponibles = Number(viaje.asientosDisponibles) || 0;
  const total = Number(viaje.totalAsientos) || 0;
  const ocupados = Math.max(total - disponibles, 0);
  const pctOcupado = total > 0 ? Math.min(100, Math.round((ocupados / total) * 100)) : 0;
  const estadoInfo = ESTADOS_VIAJE[estado];

  return (
    <article className={`cd-card cd-viaje${esActual ? ' is-actual' : ''}`}>
      <div className="cd-viaje-head">
        <div className="cd-ruta">
          <div className="cd-ruta-ciudad">
            {origen} <span className="cd-ruta-arrow">→</span> {destino}
          </div>
          <div className="cd-ruta-meta">
            {fmtFecha(viaje.fechaHoraSalida)} · {fmtHora(viaje.fechaHoraSalida)}
            {viaje.ruta?.duracionHorasEstimada ? ` · ${viaje.ruta.duracionHorasEstimada} h aprox.` : ''}
          </div>
        </div>
        <span className={claseEstadoViaje(estado)}>{estadoInfo?.label || estado}</span>
      </div>

      <div className="cd-ruta-strip" aria-hidden="true">
        <span className="cd-ruta-punto" />
        <span className="cd-ruta-tramo" />
        <span className="cd-ruta-bus">
          <Bus size={16} />
        </span>
        <span className="cd-ruta-tramo" />
        <span className="cd-ruta-punto cd-ruta-punto-fin" />
      </div>

      <div className="cd-viaje-datos">
        <div className="cd-dato">
          <Bus size={15} />
          <span className="cd-dato-valor">{viaje.bus?.placa || 'Sin placa'}</span>
        </div>
        <div className="cd-dato">
          <Clock size={15} />
          <span className="cd-dato-valor">
            {viaje.fechaHoraLlegadaEstimada
              ? `Lleg. ${fmtHora(viaje.fechaHoraLlegadaEstimada)}`
              : 'Sin llegada estimada'}
          </span>
        </div>
        <div className="cd-dato">
          <MapPin size={15} />
          <span className="cd-dato-valor">{viaje.ruta?.codigo || `Viaje #${viaje.id}`}</span>
        </div>
        <div className="cd-dato">
          <Armchair size={15} />
          <span className="cd-dato-valor">{disponibles} / {total} libres</span>
        </div>
      </div>

      <div className="cd-ocupacion">
        <div className="cd-ocupacion-barra">
          <div
            className={`cd-ocupacion-fill${pctOcupado >= 90 ? ' is-lleno' : pctOcupado >= 60 ? ' is-alerta' : ''}`}
            style={{ width: `${pctOcupado}%` }}
          />
        </div>
        <span className="cd-section-note">{pctOcupado}% ocupado</span>
      </div>

      {confirmando && (
        <div className="cd-alerta cd-alerta-neutra" style={{ padding: '10px 12px' }}>
          <span style={{ flex: 1 }}>
            {estado === 'PROGRAMADO'
              ? '¿Iniciar el viaje? El estado pasará a EN CURSO.'
              : '¿Finalizar el viaje? El estado pasará a FINALIZADO.'}
          </span>
          <button type="button" className="cd-btn cd-btn-primary cd-btn-sm" onClick={() => setConfirmando(false)}>
            Cancelar
          </button>
          <button
            type="button"
            className="cd-btn cd-btn-primary cd-btn-sm"
            onClick={() => {
              setConfirmando(false);
              if (estado === 'PROGRAMADO') onIniciar(viaje);
              else onFinalizar(viaje);
            }}
          >
            Confirmar
          </button>
        </div>
      )}

      <div className="cd-viaje-acciones">
        {estado === 'PROGRAMADO' && (
          <button
            type="button"
            className="cd-btn cd-btn-primary"
            disabled={enAccion}
            onClick={() => setConfirmando(true)}
          >
            <Play size={16} />
            Iniciar viaje
          </button>
        )}
        {estado === 'EN_CURSO' && (
          <button
            type="button"
            className="cd-btn cd-btn-alerta"
            disabled={enAccion}
            onClick={() => setConfirmando(true)}
          >
            <Flag size={16} />
            Finalizar viaje
          </button>
        )}
        <button type="button" className="cd-btn cd-btn-outline" onClick={() => onVerAsientos(viaje)}>
          <LayoutGrid size={16} />
          Ver asientos
        </button>
      </div>
    </article>
  );
}