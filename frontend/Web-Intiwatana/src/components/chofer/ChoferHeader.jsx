import { useState, useEffect } from "react";
import { Menu, LogOut, RefreshCw, Clock } from "lucide-react";
import { iniciales, fmtHora } from "./choferFormato";

export default function ChoferHeader({ nombre, sucursal, onAbrirMenu, onSalir, onRecargar, recargando, actualizado }) {
  const [ahora, setAhora] = useState(() => new Date());

  useEffect(() => {
    const t = setInterval(() => setAhora(new Date()), 30000);
    return () => clearInterval(t);
  }, []);

  return (
    <header className="cd-header">
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, minWidth: 0 }}>
        <button
          type="button"
          className="cd-btn cd-btn-icon cd-burger"
          onClick={onAbrirMenu}
          aria-label="Abrir menú lateral"
        >
          <Menu size={20} />
        </button>
        <div className="cd-header-titles">
          <h1 className="cd-header-title">Panel del Chofer</h1>
          <p className="cd-header-sub">
            Mis viajes, pasajeros y validación de boletos
            <span className="cd-user-rol cd-header-fecha" style={{ marginLeft: 8, textTransform: 'none' }}>
              {ahora.toLocaleDateString('es-PE', { weekday: 'long', day: '2-digit', month: 'long' })}
            </span>
          </p>
        </div>
      </div>

      <div className="cd-header-user">
        <div className="cd-reloj" title="Hora local del equipo">
          <Clock size={15} aria-hidden="true" />
          <span className="cd-reloj-hora">{fmtHora(ahora.toISOString())}</span>
        </div>
        <div className="cd-avatar" aria-hidden="true">{iniciales(nombre)}</div>
        <div className="cd-user-meta">
          <span className="cd-user-name">{nombre || 'Chofer'}</span>
          <span className="cd-user-rol">{sucursal || 'Chofer'}</span>
        </div>
        {onRecargar && (
          <button
            type="button"
            className="cd-btn cd-btn-outline cd-btn-sm"
            onClick={onRecargar}
            disabled={recargando}
            title={actualizado ? `Última actualización: ${fmtHora(actualizado.toISOString())}` : 'Actualizar viajes'}
          >
            <RefreshCw size={15} className={recargando ? 'cd-gira' : undefined} />
            <span className="cd-salir-texto">
              {actualizado ? `Actualizado ${fmtHora(actualizado.toISOString())}` : 'Actualizar'}
            </span>
          </button>
        )}
        <button type="button" className="cd-btn cd-btn-danger cd-btn-sm" onClick={onSalir}>
          <LogOut size={15} />
          <span className="cd-salir-texto">Salir</span>
        </button>
      </div>
    </header>
  );
}
