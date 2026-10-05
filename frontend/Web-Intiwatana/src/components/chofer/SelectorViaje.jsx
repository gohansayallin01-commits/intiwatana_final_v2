import { rutaDe, fmtFecha, fmtHora } from "./choferFormato";

/**
 * Selector de viaje para el chofer.
 * Las opciones provienen exclusivamente de GET /api/v1/viajes/mis-viajes,
 * es decir, solo viajes asignados al usuario autenticado.
 */
export default function SelectorViaje({ viajes, valor, onChange, etiqueta = 'Viaje', id = 'selector-viaje' }) {
  return (
    <div className="cd-campo" style={{ maxWidth: 460 }}>
      <label className="cd-label" htmlFor={id}>{etiqueta}</label>
      <select id={id} className="cd-select" value={valor ?? ''} onChange={(e) => onChange(e.target.value)}>
        {viajes.length === 0 && <option value="">Sin viajes asignados</option>}
        {viajes.map((v) => {
          const { origen, destino } = rutaDe(v);
          return (
            <option key={v.id} value={v.id}>
              #{v.id} · {origen} → {destino} · {fmtFecha(v.fechaHoraSalida)} {fmtHora(v.fechaHoraSalida)} · {v.estado}
            </option>
          );
        })}
      </select>
    </div>
  );
}