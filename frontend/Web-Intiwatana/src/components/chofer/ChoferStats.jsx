import { Bus, Clock, Flag, CheckCircle2, XCircle, Armchair } from "lucide-react";
import { resumenViajes } from "./choferFormato";

/**
 * Métricas calculadas a partir de los viajes reales recibidos de la API.
 * Los textos secundarios solo aclaran el significado de cada contador; no
 * introducen cifras ni datos nuevos.
 */
export default function ChoferStats({ viajes }) {
  const r = resumenViajes(viajes);

  const items = [
    {
      label: 'Viajes asignados',
      hint: 'Total del backend',
      valor: r.total,
      icon: Bus,
      bg: '#e8edf7',
      color: '#1d4ed8',
      titulo: 'Total de viajes que el backend asignó a tu usuario',
    },
    {
      label: 'Programados',
      hint: 'Por iniciar',
      valor: r.programados,
      icon: Clock,
      bg: '#e6f6ee',
      color: '#00844a',
      titulo: 'Viajes que esperan ser iniciados',
    },
    {
      label: 'En curso',
      hint: 'En operación',
      valor: r.enCurso,
      icon: Flag,
      bg: '#fff4e0',
      color: '#a35c00',
      titulo: 'Viajes actualmente en marcha',
    },
    {
      label: 'Finalizados',
      hint: 'Completados',
      valor: r.finalizados,
      icon: CheckCircle2,
      bg: '#e8edf7',
      color: '#1d4ed8',
      titulo: 'Viajes completados',
    },
  ];

  if (r.cancelados > 0) {
    items.push({
      label: 'Cancelados',
      hint: 'No operados',
      valor: r.cancelados,
      icon: XCircle,
      bg: '#fdeaea',
      color: '#b91c1c',
      titulo: 'Viajes cancelados',
    });
  }

  if (r.total > 0) {
    items.push({
      label: 'Asientos libres',
      hint: 'Suma de tus viajes',
      valor: viajes.reduce((suma, v) => suma + (Number(v.asientosDisponibles) || 0), 0),
      icon: Armchair,
      bg: '#e8ecef',
      color: '#64748b',
      titulo: 'Suma de los asientos libres de tus viajes asignados',
    });
  }

  return (
    <div className="cd-stats" role="group" aria-label="Resumen de viajes asignados">
      {items.map((i) => (
        <div className="cd-stat" key={i.label} title={i.titulo} style={{ '--cd-stat-acento': i.color }}>
          <span className="cd-stat-icon" style={{ background: i.bg, color: i.color }} aria-hidden="true">
            <i.icon size={22} />
          </span>
          <div style={{ minWidth: 0 }}>
            <div className="cd-stat-valor">{i.valor}</div>
            <div className="cd-stat-label">{i.label}</div>
            <div className="cd-stat-hint">{i.hint}</div>
          </div>
        </div>
      ))}
    </div>
  );
}
