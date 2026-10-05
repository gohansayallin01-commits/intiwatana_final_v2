import { NavLink } from "react-router-dom";
import { Bus, Users, QrCode, UserCircle2, LogOut, X } from "lucide-react";
import LogoSVG from "../LogoSVG";

/**
 * Opciones del menú del chofer.
 * Cada entrada corresponde a una función real del backend; las que no
 * existen (p. ej. reportes, reservados a ROLE_ADMIN) no se incluyen.
 */
const ITEMS = [
  { to: '/chofer', label: 'Mis Viajes', icon: Bus, end: true },
  { to: '/chofer/pasajeros', label: 'Pasajeros', icon: Users },
  { to: '/chofer/qr', label: 'Validar QR', icon: QrCode },
  { to: '/chofer/perfil', label: 'Perfil', icon: UserCircle2 },
];

export default function ChoferSidebar({ abierto, onCerrar, onSalir, nombre, sucursal }) {
  return (
    <aside className={`cd-sidebar${abierto ? ' is-open' : ''}`} aria-label="Menú del panel del chofer">
      <div className="cd-sidebar-head">
        <LogoSVG width={38} height={38} />
        <div>
          <div className="cd-brand-name">INTIWATANA</div>
          <div className="cd-brand-sub">Panel Chofer</div>
        </div>
        <button type="button" className="cd-nav-close" onClick={onCerrar} aria-label="Cerrar menú">
          <X size={18} />
        </button>
      </div>

      <nav className="cd-nav" aria-label="Secciones del panel del chofer">
        <div className="cd-nav-label">Operación</div>
        {ITEMS.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.end}
            onClick={onCerrar}
            className={({ isActive }) => `cd-nav-item${isActive ? ' is-active' : ''}`}
          >
            <item.icon size={18} />
            {item.label}
          </NavLink>
        ))}

        <div className="cd-nav-label" style={{ marginTop: 18 }}>Sesión</div>
        <button type="button" className="cd-nav-item" onClick={onSalir} style={{ width: '100%', border: 'none', background: 'transparent', textAlign: 'left' }}>
          <LogOut size={18} />
          Salir
        </button>
      </nav>

      <div className="cd-sidebar-foot">
        <strong>{nombre || 'Chofer'}</strong>
        {sucursal ? <div>{sucursal}</div> : null}
        <div style={{ marginTop: 6 }}>© {new Date().getFullYear()} INTIWATANA S.R.L.</div>
      </div>
    </aside>
  );
}