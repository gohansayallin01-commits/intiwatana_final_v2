/**
 * Capa de acceso a datos del Panel del Chofer.
 *
 * No contiene datos ficticios ni endpoints inventados: cada función llama a un
 * endpoint que ya existe en el backend y devuelve exactamente lo que la API
 * responde. Los mensajes de error se toman del cuerpo JSON que produce
 * GlobalExceptionHandler ({ mensaje, status, path }).
 */

import { marcarSesionVencida } from "../../context/sesionVencida";

export class ApiError extends Error {
  constructor(mensaje, status) {
    super(mensaje);
    this.name = 'ApiError';
    this.status = status;
  }
}

async function request(token, path, options = {}) {
  const { method = 'GET' } = options;
  let res;
  try {
    res = await fetch(path, {
      method,
      headers: {
        Accept: 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
    });
  } catch {
    throw new ApiError('No se pudo conectar con el servidor. Verifica que el servicio esté activo.', 0);
  }

  if (res.status === 204) return null;

  let cuerpo = null;
  try {
    cuerpo = await res.json();
  } catch {
    cuerpo = null;
  }

  if (!res.ok) {
    if (res.status === 401) {
      // La sesión fue revocada o su token expiró: se señaliza para que la
      // pantalla de login comunique el motivo exacto al redirigir.
      marcarSesionVencida();
    }
    const mensaje =
      cuerpo?.mensaje ||
      cuerpo?.message ||
      (res.status === 401
        ? 'Tu sesión expiró o no estás autenticado.'
        : res.status === 403
          ? 'No tienes permisos para esta acción.'
          : `La API respondió ${res.status}.`);
    throw new ApiError(mensaje, res.status);
  }

  return cuerpo;
}

/** Desenvuelve Page<T> de Spring Data o un array plano. */
function aLista(respuesta) {
  if (Array.isArray(respuesta)) return respuesta;
  if (Array.isArray(respuesta?.content)) return respuesta.content;
  return [];
}

/**
 * GET /api/v1/viajes/mis-viajes
 * El backend filtra por el chofer autenticado (obtenido del JWT).
 * @returns {{ viajes: object[], total: number }}
 */
export async function obtenerMisViajes(token) {
  const data = await request(token, '/api/v1/viajes/mis-viajes?size=200&sort=fechaHoraSalida,desc');
  const viajes = aLista(data);
  return { viajes, total: typeof data?.totalElements === 'number' ? data.totalElements : viajes.length };
}

/** GET /api/v1/viajes/{id}/asientos */
export async function obtenerAsientos(token, viajeId) {
  return aLista(await request(token, `/api/v1/viajes/${viajeId}/asientos`));
}

/**
 * PATCH /api/v1/viajes/{id}/estado
 * El backend valida la transición y que el viaje pertenezca al chofer.
 */
export async function cambiarEstadoViaje(token, viajeId, estado) {
  return request(token, `/api/v1/viajes/${viajeId}/estado?estado=${encodeURIComponent(estado)}`, {
    method: 'PATCH',
  });
}

/** GET /api/v1/boletos/viaje/{id} — pasajeros (boletos) del viaje */
export async function obtenerPasajeros(token, viajeId) {
  const data = await request(token, `/api/v1/boletos/viaje/${viajeId}?size=200`);
  return aLista(data);
}

/**
 * POST /api/v1/boletos/escanear?codigoQr=
 * El código QR del boleto es su numeroBoleto (ver BoletoServiceImpl).
 */
export async function validarQr(token, codigo) {
  return request(token, `/api/v1/boletos/escanear?codigoQr=${encodeURIComponent(codigo.trim())}`, {
    method: 'POST',
  });
}