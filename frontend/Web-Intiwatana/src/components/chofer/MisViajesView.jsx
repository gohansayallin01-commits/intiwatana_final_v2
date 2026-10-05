import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { AlertCircle, Flag, RefreshCw, Users, Info, LayoutGrid, QrCode } from "lucide-react";
import ChoferStats from "./ChoferStats";
import ViajeCard from "./ViajeCard";
import AsientosModal from "./AsientosModal";
import { ordenarViajes } from "./choferFormato";

/**
 * Vista principal: métricas reales + viaje en curso + tarjetas de viaje.
 * Los datos provienen de GET /api/v1/viajes/mis-viajes.
 */
export default function MisViajesView({
  token,
  viajes,
  total,
  cargando,
  error,
  onRecargar,
  onCambiarEstado,
}) {
  const navigate = useNavigate();
  const [asientosDe, setAsientosDe] = useState(null);
  const [procesando, setProcesando] = useState(null);

  const ordenados = ordenarViajes(viajes);
  const enCurso = ordenados.filter((v) => v.estado === 'EN_CURSO');
  const programados = ordenados.filter((v) => v.estado === 'PROGRAMADO');
  const historial = ordenados.filter((v) => v.estado !== 'EN_CURSO' && v.estado !== 'PROGRAMADO');
  const viajeAcciones = enCurso[0] || programados[0] || ordenados[0] || null;

  const cambiar = async (viaje, nuevoEstado) => {
    setProcesando(viaje.id);
    try {
      await onCambiarEstado(viaje, nuevoEstado);
    } finally {
      setProcesando(null);
    }
  };

  if (cargando) {
    return (
      <div className="cd-cargando">
        <span className="cd-spinner" />
        Cargando viajes...
      </div>
    );
  }

  if (error) {
    return (
      <div className="cd-card cd-vacio">
        <AlertCircle size={30} color="#dc2626" />
        <strong>No se pudieron cargar tus viajes</strong>
        <div style={{ marginBottom: 14 }}>{error}</div>
        <button type="button" className="cd-btn cd-btn-primary" onClick={onRecargar}>
          <RefreshCw size={16} />
          Reintentar
        </button>
      </div>
    );
  }

  return (
    <>
      <ChoferStats viajes={viajes} />

      {total > viajes.length && (
        <div className="cd-alerta cd-alerta-neutra" style={{ marginBottom: 20 }}>
          <Info size={18} />
          <span>
            La API devolvió {viajes.length} de {total} viajes asignados. Actualiza para volver a consultar.
          </span>
        </div>
      )}

      <section className="cd-section">
        <div className="cd-section-head">
          <h2 className="cd-section-title">Viaje en curso</h2>
          {enCurso.length > 0 && <span className="cd-section-note">{enCurso.length} activo(s)</span>}
        </div>
        {enCurso.length > 0 ? (
          <div className="cd-grid-viajes">
            {enCurso.map((v) => (
              <ViajeCard
                key={v.id}
                viaje={v}
                esActual
                enAccion={procesando === v.id}
                onIniciar={(x) => cambiar(x, 'EN_CURSO')}
                onFinalizar={(x) => cambiar(x, 'FINALIZADO')}
                onVerAsientos={setAsientosDe}
              />
            ))}
          </div>
        ) : (
          <div className="cd-card cd-vacio cd-vacio-actual">
            <Flag size={30} />
            <strong>No tienes un viaje activo en este momento.</strong>
            <div>Tus viajes asignados aparecen más abajo con la acción disponible según su estado.</div>
            <button
              type="button"
              className="cd-btn cd-btn-outline"
              style={{ marginTop: 14 }}
              onClick={() => {
                const destino = document.getElementById('cd-programados') || document.getElementById('cd-historial');
                destino?.scrollIntoView({ behavior: 'smooth', block: 'start' });
              }}
            >
              Ver mis viajes
            </button>
          </div>
        )}
      </section>

      {viajes.length > 0 && (
        <section aria-label="Acciones rápidas">
          <div className="cd-section-head">
            <h2 className="cd-section-title">Acciones rápidas</h2>
          </div>
          <div className="cd-accesos">
            <button type="button" className="cd-acceso" onClick={onRecargar}>
              <span className="cd-acceso-icon" style={{ background: '#e8edf7', color: '#1d4ed8' }} aria-hidden="true">
                <RefreshCw size={20} />
              </span>
              <span className="cd-acceso-meta">
                <span className="cd-acceso-titulo">Actualizar</span>
                <span className="cd-acceso-sub">Volver a consultar tus viajes</span>
              </span>
            </button>
            <button
              type="button"
              className="cd-acceso"
              disabled={!viajeAcciones}
              onClick={() => viajeAcciones && setAsientosDe(viajeAcciones)}
            >
              <span className="cd-acceso-icon" style={{ background: '#e6f6ee', color: '#00844a' }} aria-hidden="true">
                <LayoutGrid size={20} />
              </span>
              <span className="cd-acceso-meta">
                <span className="cd-acceso-titulo">Ver asientos</span>
                <span className="cd-acceso-sub">Mapa real del viaje</span>
              </span>
            </button>
            <button type="button" className="cd-acceso" onClick={() => navigate('/chofer/pasajeros')}>
              <span className="cd-acceso-icon" style={{ background: '#fff4e0', color: '#a35c00' }} aria-hidden="true">
                <Users size={20} />
              </span>
              <span className="cd-acceso-meta">
                <span className="cd-acceso-titulo">Pasajeros</span>
                <span className="cd-acceso-sub">Boletos por viaje</span>
              </span>
            </button>
            <button type="button" className="cd-acceso" onClick={() => navigate('/chofer/qr')}>
              <span className="cd-acceso-icon" style={{ background: '#e8f0ff', color: '#1d63ed' }} aria-hidden="true">
                <QrCode size={20} />
              </span>
              <span className="cd-acceso-meta">
                <span className="cd-acceso-titulo">Validar QR</span>
                <span className="cd-acceso-sub">Escanear boleto</span>
              </span>
            </button>
          </div>
        </section>
      )}

      {programados.length > 0 && (
        <section className="cd-section" id="cd-programados">
          <div className="cd-section-head">
            <h2 className="cd-section-title">Próximos viajes</h2>
            <span className="cd-section-note">{programados.length} programado(s)</span>
          </div>
          <div className="cd-grid-viajes">
            {programados.map((v) => (
              <ViajeCard
                key={v.id}
                viaje={v}
                enAccion={procesando === v.id}
                onIniciar={(x) => cambiar(x, 'EN_CURSO')}
                onFinalizar={(x) => cambiar(x, 'FINALIZADO')}
                onVerAsientos={setAsientosDe}
              />
            ))}
          </div>
        </section>
      )}

      {historial.length > 0 && (
        <section className="cd-section" id="cd-historial">
          <div className="cd-section-head">
            <h2 className="cd-section-title">Historial</h2>
            <span className="cd-section-note">{historial.length} viaje(s) finalizados o cancelados</span>
          </div>
          <div className="cd-grid-viajes">
            {historial.map((v) => (
              <ViajeCard
                key={v.id}
                viaje={v}
                enAccion={procesando === v.id}
                onIniciar={(x) => cambiar(x, 'EN_CURSO')}
                onFinalizar={(x) => cambiar(x, 'FINALIZADO')}
                onVerAsientos={setAsientosDe}
              />
            ))}
          </div>
        </section>
      )}

      {viajes.length === 0 && (
        <div className="cd-card cd-vacio">
          <Users size={30} />
          <strong>No tienes viajes asignados</strong>
          <div>
            Cuando un administrador te asigne un viaje aparecerá aquí con su estado, sus asientos disponibles y las
            acciones disponibles.
          </div>
          <button type="button" className="cd-btn cd-btn-outline" style={{ marginTop: 14 }} onClick={onRecargar}>
            <RefreshCw size={16} />
            Actualizar
          </button>
        </div>
      )}

      {enCurso.length > 1 && (
        <div className="cd-alerta cd-alerta-neutra" style={{ marginTop: 6 }}>
          <Flag size={18} />
          <span>Tienes {enCurso.length} viajes en curso. El sistema permite varios viajes EN_CURSO a la vez.</span>
        </div>
      )}

      {asientosDe && (
        <AsientosModal token={token} viaje={asientosDe} onClose={() => setAsientosDe(null)} />
      )}
    </>
  );
}