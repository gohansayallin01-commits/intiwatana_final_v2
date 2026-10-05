import { UserCircle2, ShieldCheck, Info, LogOut } from "lucide-react";
import { iniciales } from "./choferFormato";

/**
 * Perfil del chofer — solo lectura.
 *
 * El backend no expone un endpoint de perfil para ROLE_CHOFER
 * (GET /api/v1/usuarios/{id} responde 403) ni permite editar usuarios al rol.
 * Por eso se muestran únicamente los datos reales devueltos por
 * POST /api/v1/auth/login, sin inventar ni editar campos.
 */
export default function PerfilView({ session, onSalir }) {
  const datos = [
    { label: 'Usuario', valor: session?.username || '—' },
    { label: 'Nombre completo', valor: session?.nombreCompleto || '—' },
    { label: 'Rol', valor: session?.rol || '—' },
    { label: 'Sucursal', valor: session?.sucursalNombre || 'Sin sucursal asignada' },
    { label: 'ID de usuario', valor: session?.usuarioId ?? '—' },
    { label: 'ID de sucursal', valor: session?.sucursalId ?? '—' },
  ];

  return (
    <>
      <div className="cd-section-head">
        <h2 className="cd-section-title">Mi perfil</h2>
        <span className="cd-section-note">Datos del inicio de sesión</span>
      </div>

      <div className="cd-card" style={{ padding: 20, marginBottom: 18 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 16, marginBottom: 18 }}>
          <div className="cd-avatar" style={{ width: 56, height: 56, fontSize: 19 }} aria-hidden="true">
            {iniciales(session?.nombreCompleto || session?.username)}
          </div>
          <div style={{ minWidth: 0 }}>
            <div style={{ fontSize: 19, fontWeight: 800 }}>{session?.nombreCompleto || session?.username}</div>
            <div className="cd-section-note">{session?.rol} · {session?.sucursalNombre || 'Sin sucursal'}</div>
          </div>
        </div>

        <div className="cd-datos-lista">
          {datos.map((d) => (
            <div className="cd-dato-item" key={d.label}>
              <div className="cd-label">{d.label}</div>
              <div className="cd-dato-valor">{d.valor}</div>
            </div>
          ))}
        </div>
      </div>

      <div className="cd-alerta cd-alerta-info" style={{ marginBottom: 14 }}>
        <ShieldCheck size={18} />
        <span>
          Esta información es de solo lectura. El backend no expone un endpoint de perfil para el rol CHOFER ni permite
          que el chofer modifique sus datos; los cambios los realiza un administrador.
        </span>
      </div>

      <div className="cd-alerta cd-alerta-neutra" style={{ marginBottom: 18 }}>
        <Info size={18} />
        <span>
          Funciones disponibles para tu rol: Mis Viajes, Pasajeros (por viaje), Validar QR y Salir. Los reportes
          gerenciales son exclusivos del administrador.
        </span>
      </div>

      <div className="cd-section-head">
        <h2 className="cd-section-title">Sesión</h2>
      </div>
      <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
        <button type="button" className="cd-btn cd-btn-danger" onClick={onSalir}>
          <LogOut size={16} />
          Cerrar sesión
        </button>
        <span className="cd-section-note" style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}>
          <UserCircle2 size={15} />
          Al salir se elimina el token JWT almacenado y se vuelve al login público.
        </span>
      </div>
    </>
  );
}