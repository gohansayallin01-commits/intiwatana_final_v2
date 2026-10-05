import { useCallback, useEffect, useState } from "react";
import { Navigate, Route, Routes, useNavigate } from "react-router-dom";
import { useAuth } from "../context/useAuth";
import ChoferSidebar from "./chofer/ChoferSidebar";
import ChoferHeader from "./chofer/ChoferHeader";
import MisViajesView from "./chofer/MisViajesView";
import PasajerosView from "./chofer/PasajerosView";
import QrView from "./chofer/QrView";
import PerfilView from "./chofer/PerfilView";
import { ApiError, cambiarEstadoViaje, obtenerMisViajes } from "./chofer/apiChofer";

/**
 * Panel del Chofer.
 *
 * Datos: GET /api/v1/viajes/mis-viajes (el backend devuelve únicamente los
 * viajes del chofer autenticado). Acciones de estado: PATCH
 * /api/v1/viajes/{id}/estado, respetando las transiciones del backend.
 */
export default function ChoferDashboard() {
  const { session, logout } = useAuth();
  const navigate = useNavigate();

  const [viajes, setViajes] = useState([]);
  const [total, setTotal] = useState(0);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');
  const [menuAbierto, setMenuAbierto] = useState(false);
  const [toast, setToast] = useState(null);
  const [actualizado, setActualizado] = useState(null);

  const token = session?.token;

  const cargarViajes = useCallback(async () => {
    if (!token) return;
    setCargando(true);
    setError('');
    try {
      const data = await obtenerMisViajes(token);
      setViajes(data.viajes);
      setTotal(data.total);
      setActualizado(new Date());
    } catch (e) {
      setViajes([]);
      setTotal(0);
      // 401/403 = sesión inválida o sin permisos: se cierra la sesión y se
      // redirige al login en lugar de dejar un panel inservible.
      if (e instanceof ApiError && (e.status === 401 || e.status === 403)) {
        logout();
        navigate('/login', { replace: true });
        return;
      }
      setError(e.message || 'No se pudieron cargar tus viajes.');
    } finally {
      setCargando(false);
    }
  }, [token, logout, navigate]);

  useEffect(() => {
    cargarViajes();
  }, [cargarViajes]);

  useEffect(() => {
    if (!menuAbierto) return undefined;
    const onKey = (e) => {
      if (e.key === 'Escape') setMenuAbierto(false);
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [menuAbierto]);

  useEffect(() => {
    if (!toast) return undefined;
    const t = setTimeout(() => setToast(null), 4000);
    return () => clearTimeout(t);
  }, [toast]);

  const cambiarEstado = useCallback(
    async (viaje, nuevoEstado) => {
      try {
        await cambiarEstadoViaje(token, viaje.id, nuevoEstado);
        setToast({ tipo: 'ok', texto: `Viaje #${viaje.id}: estado actualizado a ${nuevoEstado}.` });
        await cargarViajes();
      } catch (e) {
        setToast({ tipo: 'error', texto: e.message || 'No se pudo actualizar el estado del viaje.' });
      }
    },
    [token, cargarViajes]
  );

  const salir = useCallback(() => {
    logout();
    navigate('/login', { replace: true });
  }, [logout, navigate]);

  if (!token) return <Navigate to="/login" replace />;

  const nombre = session?.nombreCompleto || session?.username || '';

  return (
    <div className="cd-shell">
      <ChoferSidebar
        abierto={menuAbierto}
        onCerrar={() => setMenuAbierto(false)}
        onSalir={salir}
        nombre={nombre}
        sucursal={session?.sucursalNombre}
      />
      {menuAbierto && (
        <button
          type="button"
          className="cd-backdrop"
          aria-label="Cerrar menú"
          onClick={() => setMenuAbierto(false)}
        />
      )}

      <div className="cd-main">
        <ChoferHeader
          nombre={nombre}
          sucursal={session?.sucursalNombre}
          onAbrirMenu={() => setMenuAbierto(true)}
          onSalir={salir}
          onRecargar={cargarViajes}
          recargando={cargando}
          actualizado={actualizado}
        />

        <main className="cd-content" aria-busy={cargando}>
          {/* Región viva: anuncia la carga y los cambios de estado del viaje,
              que de otro modo solo serían visibles. */}
          <p className="cd-sr-only" role="status" aria-live="polite">
            {cargando
              ? 'Cargando viajes asignados…'
              : error
                ? `No se pudieron cargar los viajes: ${error}`
                : `${total} viaje${total === 1 ? '' : 's'} asignado${total === 1 ? '' : 's'} cargado${total === 1 ? '' : 's'}.`}
          </p>
          <Routes>
            <Route
              index
              element={
                <MisViajesView
                  token={token}
                  viajes={viajes}
                  total={total}
                  cargando={cargando}
                  error={error}
                  onRecargar={cargarViajes}
                  onCambiarEstado={cambiarEstado}
                />
              }
            />
            <Route path="pasajeros" element={<PasajerosView token={token} viajes={viajes} />} />
            <Route path="qr" element={<QrView token={token} viajes={viajes} />} />
            <Route path="perfil" element={<PerfilView session={session} onSalir={salir} />} />
            <Route path="*" element={<Navigate to="/chofer" replace />} />
          </Routes>
        </main>
      </div>

      {toast && (
        <div className={`cd-toast cd-toast-${toast.tipo === 'ok' ? 'ok' : 'error'}`} role="status">
          {toast.texto}
        </div>
      )}
    </div>
  );
}