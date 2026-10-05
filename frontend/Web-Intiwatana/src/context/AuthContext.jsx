import { useState } from "react";
import { AuthContext } from "./auth-context";

export function AuthProvider({ children }) {
  const [session, setSession] = useState(() => {
    // Recuperar sesión guardada al recargar página
    try {
      const saved = localStorage.getItem("iwt_session");
      return saved ? JSON.parse(saved) : null;
    } catch { return null; }
  });

  const login = (responseData) => {
    // Guardar toda la respuesta del backend
    const s = {
      token: responseData.token,
      username: responseData.username,
      rol: responseData.rol,
      usuarioId: responseData.usuarioId,
      nombreCompleto: responseData.nombreCompleto,
      sucursalId: responseData.sucursalId,
      sucursalNombre: responseData.sucursalNombre,
      clienteId: responseData.clienteId, // ✅ Agregado
    };
    setSession(s);
    localStorage.setItem("iwt_session", JSON.stringify(s));
    // El refreshToken que devuelve el backend NO se persiste a proposito: es una
    // credencial de larga duracion y localStorage es accesible desde cualquier
    // XSS. El endpoint /auth/refresh existe para clientes que necesiten renovar;
    // mientras esta SPA use el token de acceso directo, no se expone.
  };

  const logout = async () => {
    const token = session?.token;
    // Siempre se limpia la sesión local, incluso si la revocación falla: dejar al
    // usuario "dentro" de la interfaz porque el servidor no respondió sería peor
    // que un token que sigue vivo hasta expirar.
    setSession(null);
    localStorage.removeItem("iwt_session");

    if (!token) return;
    // Revoca el token en el servidor (H-06). Sin esto, cerrar sesión en el
    // navegador solo borraba la copia local y el JWT seguía siendo válido hasta
    // su expiración: un token copiado de localStorage seguía entrando al sistema.
    try {
      await fetch("/api/v1/auth/logout", {
        method: "POST",
        headers: { Authorization: `Bearer ${token}` },
      });
    } catch {
      // Red caída o servidor apagado: la sesión local ya está cerrada y no hay
      // nada que limpiar en el cliente. El token caducará solo.
    }
  };

  const actualizarFoto = (foto) => {
    setSession(prev => {
      const s = { ...prev, foto };
      localStorage.setItem("iwt_session", JSON.stringify(s));
      return s;
    });
  };

  return (
    <AuthContext.Provider value={{ session, login, logout, actualizarFoto }}>
      {children}
    </AuthContext.Provider>
  );
}
