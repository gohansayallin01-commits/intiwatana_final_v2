const FLAG = "iwt_sesion_vencida";

export function marcarSesionVencida() {
  try { sessionStorage.setItem(FLAG, "1"); } catch { /* sin sessionStorage */ }
}

export function esSesionVencida() {
  try { return sessionStorage.getItem(FLAG) === "1"; } catch { return false; }
}

export function limpiarSesionVencida() {
  try { sessionStorage.removeItem(FLAG); } catch { /* sin sessionStorage */ }
}