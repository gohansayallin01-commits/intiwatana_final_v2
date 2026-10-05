/**
 * Helpers de presentación del Panel del Chofer.
 * Todo se deriva de los datos que devuelve la API; no hay valores inventados.
 */

const FECHA_LARGA = { weekday: 'short', day: '2-digit', month: 'short', year: 'numeric' };
const FECHA_CORTA = { weekday: 'short', day: '2-digit', month: 'short' };
const HORA = { hour: '2-digit', minute: '2-digit', hour12: true };

const MESES = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'set', 'oct', 'nov', 'dic'];

/** Convierte el ISO de LocalDateTime del backend a Date (se interpreta en hora local). */
export function aFecha(iso) {
  if (!iso) return null;
  const d = new Date(iso);
  return Number.isNaN(d.getTime()) ? null : d;
}

export function fmtFecha(iso) {
  const d = aFecha(iso);
  if (!d) return '—';
  return d.toLocaleDateString('es-PE', FECHA_CORTA).replace(/\./g, '');
}

export function fmtFechaLarga(iso) {
  const d = aFecha(iso);
  if (!d) return 'Fecha no registrada';
  return d.toLocaleDateString('es-PE', FECHA_LARGA).replace(/\./g, '');
}

export function fmtHora(iso) {
  const d = aFecha(iso);
  if (!d) return '—';
  const crudo = d.toLocaleTimeString('es-PE', HORA).replace(/\./g, '').trim();
  // es-PE devuelve "08:00 a. m." → "08:00 A.M." (normaliza el separador)
  return crudo.replace(/^(\d{1,2}:\d{2})\s*([ap])\s*m$/i, (_, hora, mer) => `${hora} ${mer.toUpperCase()}.M.`);
}

export function fmtFechaHora(iso) {
  const d = aFecha(iso);
  if (!d) return '—';
  return `${d.getDate()} ${MESES[d.getMonth()]} ${d.getFullYear()} · ${fmtHora(iso)}`;
}

export function fmtMoneda(valor) {
  if (valor === null || valor === undefined || valor === '') return '—';
  const n = Number(valor);
  if (Number.isNaN(n)) return String(valor);
  return `S/ ${n.toFixed(2)}`;
}

/** Origen y destino desde el objeto ruta que devuelve la API. */
export function rutaDe(viaje) {
  const origen = viaje?.ruta?.origen?.ciudad || viaje?.ruta?.origen?.nombre;
  const destino = viaje?.ruta?.destino?.ciudad || viaje?.ruta?.destino?.nombre;
  return { origen: origen || 'Origen no definido', destino: destino || 'Destino no definido' };
}

/** Etiquetas y color de estado de viaje (valores reales de EstadoViaje). */
export const ESTADOS_VIAJE = {
  PROGRAMADO: { label: 'Programado' },
  EN_CURSO: { label: 'En curso' },
  FINALIZADO: { label: 'Finalizado' },
  CANCELADO: { label: 'Cancelado' },
};

export const ESTADOS_ASIENTO = {
  DISPONIBLE: { label: 'Disponible', bg: '#ffffff', borde: '#cfe0d8' },
  VENDIDO: { label: 'Vendido', bg: '#00a859', borde: '#00844a' },
  RESERVADO: { label: 'Reservado', bg: '#fef3c7', borde: '#fcd34d' },
  BLOQUEADO: { label: 'Bloqueado', bg: '#64748b', borde: '#475569' },
};

export const ESTADOS_BOLETO = {
  ACTIVO: { label: 'Activo' },
  USADO: { label: 'Usado' },
  CANCELADO: { label: 'Cancelado' },
  REEMBOLSADO: { label: 'Reembolsado' },
};

export function claseEstadoViaje(estado) {
  return ESTADOS_VIAJE[estado] ? `cd-badge cd-badge-${estado}` : 'cd-badge cd-badge-neutro';
}

export function claseEstadoBojeto(estado) {
  return ESTADOS_BOLETO[estado] ? `cd-badge cd-badge-${estado}` : 'cd-badge cd-badge-neutro';
}

/** Orden operativo: en curso, luego programados por fecha, luego historial. */
export function ordenarViajes(viajes) {
  const prioridad = { EN_CURSO: 0, PROGRAMADO: 1 };
  return [...viajes].sort((a, b) => {
    const pa = prioridad[a.estado] ?? 2;
    const pb = prioridad[b.estado] ?? 2;
    if (pa !== pb) return pa - pb;
    if (pa === 1) return new Date(a.fechaHoraSalida || 0) - new Date(b.fechaHoraSalida || 0);
    return new Date(b.fechaHoraSalida || 0) - new Date(a.fechaHoraSalida || 0);
  });
}

/** Métricas calculadas sobre los viajes reales recibidos. */
export function resumenViajes(viajes) {
  const cuenta = { total: viajes.length, programados: 0, enCurso: 0, finalizados: 0, cancelados: 0 };
  for (const v of viajes) {
    if (v.estado === 'PROGRAMADO') cuenta.programados += 1;
    else if (v.estado === 'EN_CURSO') cuenta.enCurso += 1;
    else if (v.estado === 'FINALIZADO') cuenta.finalizados += 1;
    else if (v.estado === 'CANCELADO') cuenta.cancelados += 1;
  }
  return cuenta;
}

export function iniciales(nombre) {
  const partes = String(nombre || '').trim().split(/\s+/).filter(Boolean);
  if (!partes.length) return 'CH';
  return partes.slice(0, 2).map((p) => p[0]).join('').toUpperCase();
}