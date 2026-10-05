import { useContext } from "react";
import { AuthContext } from "./auth-context";

/**
 * Acceso a la sesión del usuario.
 *
 * En AuthContext.jsx queda importado y reexportado por compatibilidad, pero
 * conviene usar este módulo directamente para no arrastrar el provider.
 */
export function useAuth() {
  return useContext(AuthContext);
}
