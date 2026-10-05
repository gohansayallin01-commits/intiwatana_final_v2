import React, { useState, useEffect, useCallback } from "react";
import { useAuth } from "../context/useAuth";
import { Users, UserPlus, Trash2, CheckCircle2, XCircle } from "lucide-react";

const C = { verde: "#127369", gris: "#8AA6A3" };

export default function CrudUsuarios({ headers, showToast }) {
  const { session } = useAuth();
  const [usuarios, setUsuarios] = useState([]);
  const [roles, setRoles] = useState([]);
  const [sucursales, setSucursales] = useState([]);
  const [load, setLoad] = useState(true);
  const [modal, setModal] = useState(null); // null o "nuevo"
  const [form, setForm] = useState({});
  const [saving, setSaving] = useState(false);
  const [confEliminar, setConfEliminar] = useState(null);

  const cargarDatos = useCallback(async () => {
    setLoad(true);
    try {
      const [resUsr, resRol, resSuc] = await Promise.all([
        fetch("/api/v1/usuarios", { headers }),
        fetch("/api/v1/roles", { headers }),
        fetch("/api/v1/sucursales", { headers }),
      ]);
      if (resUsr.ok) setUsuarios(await resUsr.json());
      if (resRol.ok) setRoles(await resRol.json());
      if (resSuc.ok) setSucursales(await resSuc.json());
    } catch {
      showToast("⚠ Error cargando usuarios");
    } finally {
      setLoad(false);
    }
  }, [headers, showToast]);

  useEffect(() => {
    cargarDatos();
  }, [cargarDatos]);

  const abrirNuevo = () => {
    setForm({ username: "", password: "", nombres: "", apellidos: "", email: "", telefono: "", dniRuc: "", rolId: "", sucursalId: "" });
    setModal("nuevo");
  };

  const guardar = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      const payload = {
        ...form,
        rolId: parseInt(form.rolId),
        sucursalId: form.sucursalId ? parseInt(form.sucursalId) : null,
      };
      const res = await fetch("/api/v1/usuarios", {
        method: "POST",
        headers,
        body: JSON.stringify(payload),
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.mensaje || "Error al crear usuario");
      }
      showToast("✓ Usuario creado");
      setModal(null);
      cargarDatos();
    } catch (err) {
      showToast("⚠ " + err.message);
    } finally {
      setSaving(false);
    }
  };

  const eliminar = async (id) => {
    try {
      const res = await fetch(`/api/v1/usuarios/${id}`, { method: "DELETE", headers });
      if (!res.ok) throw new Error("No se pudo eliminar");
      setUsuarios(prev => prev.filter(u => u.id !== id));
      showToast("✓ Usuario eliminado");
    } catch (err) {
      showToast("⚠ " + err.message);
    } finally {
      setConfEliminar(null);
    }
  };

  const getRolBadgeColor = (rolName) => {
    switch(rolName) {
      case "ROLE_ADMIN": return "#e11d48"; // Rose
      case "ROLE_CHOFER": return "#2563eb"; // Blue
      case "ROLE_CAJERO": return "#f59e0b"; // Amber
      case "ROLE_VENDEDOR": return "#14b8a6"; // Teal
      case "ROLE_OPERADOR_ENCOMIENDAS": return "#a855f7"; // Purple
      default: return "#10b981"; // Green (Cliente)
    }
  };

  const formatRolName = (rolName) => {
    if (!rolName) return "—";
    return rolName.replace("ROLE_", "");
  };

  if (load) return <div style={{ padding: "2rem", color: C.verde, textAlign: "center" }}>Cargando usuarios...</div>;

  return (
    <>
      <div className="adm-topbar" style={{ animation: "fadeUp 0.3s ease" }}>
        <div>
          <h2 className="adm-title" style={{ display: "flex", alignItems: "center", gap: 10 }}>
            <Users size={28} /> Gestión de Personal
          </h2>
          <p className="adm-subtitle">{usuarios.length} usuarios registrados en el sistema</p>
        </div>
        <div style={{ display: "flex", gap: 8 }}>
          <button className="adm-btn-outline" onClick={cargarDatos}>↻ Actualizar</button>
          <button className="adm-btn-primary" onClick={abrirNuevo}><UserPlus size={18} /> Nuevo usuario</button>
        </div>
      </div>

      <div className="table-wrap">
        <table className="adm-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Usuario</th>
              <th>Nombre Completo</th>
              <th>Rol</th>
              <th>Sucursal</th>
              <th>Teléfono</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            {usuarios.length === 0 && (
              <tr><td colSpan="7" style={{ textAlign: "center", color: C.gris, padding: "2rem" }}>No hay usuarios registrados</td></tr>
            )}
            {usuarios.map(u => (
              <tr key={u.id} className="tr-hover">
                <td><span className="td-codigo">#{u.id}</span></td>
                <td style={{ fontWeight: 600, color: "#0d3330" }}>{u.username}</td>
                <td>{u.nombres} {u.apellidos}</td>
                <td>
                  <span style={{ 
                    background: `${getRolBadgeColor(u.rol)}20`, 
                    color: getRolBadgeColor(u.rol), 
                    padding: "4px 8px", 
                    borderRadius: 6, 
                    fontSize: 11, 
                    fontWeight: 700 
                  }}>
                    {formatRolName(u.rol)}
                  </span>
                </td>
                <td>{u.sucursalNombre || "—"}</td>
                <td>{u.telefono || "—"}</td>
                <td>
                  <div className="td-actions">
                    {u.username !== session.username && (
                      <button className="btn-delete" onClick={() => setConfEliminar(u)} title="Eliminar">
                        <Trash2 size={16} />
                      </button>
                    )}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Modal Nuevo Usuario */}
      {modal === "nuevo" && (
        <div className="modal-overlay" onClick={() => setModal(null)}>
          <div className="modal" onClick={(e) => e.stopPropagation()} style={{ maxWidth: 600 }}>
            <div className="modal-header">
              <div className="modal-badge"><UserPlus size={24} /></div>
              <div>
                <div className="modal-title">Nuevo Usuario</div>
                <div className="modal-subtitle">Crear cuenta para personal</div>
              </div>
              <button className="modal-close" onClick={() => setModal(null)}>✕</button>
            </div>
            
            <form className="modal-body" onSubmit={guardar}>
              <div className="form-grid">
                <div style={{ marginBottom: 12 }}>
                  <label style={{ display: "block", fontSize: 12, fontWeight: "bold", color: C.gris, marginBottom: 5 }}>Nombres *</label>
                  <input className="lf-input" value={form.nombres} onChange={e => setForm({...form, nombres: e.target.value})} required placeholder="Ej: Juan Carlos" />
                </div>
                <div style={{ marginBottom: 12 }}>
                  <label style={{ display: "block", fontSize: 12, fontWeight: "bold", color: C.gris, marginBottom: 5 }}>Apellidos *</label>
                  <input className="lf-input" value={form.apellidos} onChange={e => setForm({...form, apellidos: e.target.value})} required placeholder="Ej: Pérez" />
                </div>
                
                <div style={{ marginBottom: 12 }}>
                  <label style={{ display: "block", fontSize: 12, fontWeight: "bold", color: C.gris, marginBottom: 5 }}>Nombre de usuario (Login) *</label>
                  <input className="lf-input" value={form.username} onChange={e => setForm({...form, username: e.target.value})} required placeholder="jperez" />
                </div>
                <div style={{ marginBottom: 12 }}>
                  <label style={{ display: "block", fontSize: 12, fontWeight: "bold", color: C.gris, marginBottom: 5 }}>Contraseña *</label>
                  <input className="lf-input" type="password" value={form.password} onChange={e => setForm({...form, password: e.target.value})} required placeholder="Min 6 caracteres" minLength={6} />
                </div>

                <div style={{ marginBottom: 12 }}>
                  <label style={{ display: "block", fontSize: 12, fontWeight: "bold", color: C.gris, marginBottom: 5 }}>Rol *</label>
                  <select className="lf-select" value={form.rolId} onChange={e => setForm({...form, rolId: e.target.value})} required>
                    <option value="">Seleccionar rol...</option>
                    {roles.map(r => (
                      <option key={r.id} value={r.id}>{formatRolName(r.nombre)}</option>
                    ))}
                  </select>
                </div>
                <div style={{ marginBottom: 12 }}>
                  <label style={{ display: "block", fontSize: 12, fontWeight: "bold", color: C.gris, marginBottom: 5 }}>Sucursal Base</label>
                  <select className="lf-select" value={form.sucursalId} onChange={e => setForm({...form, sucursalId: e.target.value})}>
                    <option value="">Sin sucursal (Sede Central)</option>
                    {sucursales.map(s => (
                      <option key={s.id} value={s.id}>{s.nombre} — {s.ciudad}</option>
                    ))}
                  </select>
                </div>

                <div style={{ marginBottom: 12 }}>
                  <label style={{ display: "block", fontSize: 12, fontWeight: "bold", color: C.gris, marginBottom: 5 }}>DNI / RUC *</label>
                  <input className="lf-input" value={form.dniRuc} onChange={e => setForm({...form, dniRuc: e.target.value})} required placeholder="8 dígitos" />
                </div>
                <div style={{ marginBottom: 12 }}>
                  <label style={{ display: "block", fontSize: 12, fontWeight: "bold", color: C.gris, marginBottom: 5 }}>Teléfono</label>
                  <input className="lf-input" value={form.telefono} onChange={e => setForm({...form, telefono: e.target.value})} placeholder="Opcional" />
                </div>
              </div>
              <div style={{ marginTop: 20, display: "flex", gap: 10 }}>
                <button type="button" className="btn-cancelar" onClick={() => setModal(null)} style={{ flex: 1 }}>Cancelar</button>
                <button type="submit" className="btn-reservar" disabled={saving} style={{ flex: 1 }}>
                  {saving ? "Guardando..." : "Crear Usuario"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal Confirmación Eliminar */}
      {confEliminar && (
        <div className="modal-overlay" onClick={() => setConfEliminar(null)}>
          <div className="modal" onClick={e => e.stopPropagation()} style={{ maxWidth: 400, textAlign: "center", padding: "2rem" }}>
            <div style={{ color: "#e11d48", marginBottom: 15 }}><XCircle size={48} /></div>
            <h3 style={{ margin: "0 0 10px", color: "#0d3330" }}>¿Eliminar usuario?</h3>
            <p style={{ color: C.gris, fontSize: 14, margin: "0 0 20px" }}>
              Estás a punto de eliminar al usuario <strong>{confEliminar.username}</strong>. Esta acción no se puede deshacer.
            </p>
            <div style={{ display: "flex", gap: 10 }}>
              <button className="btn-cancelar" onClick={() => setConfEliminar(null)} style={{ flex: 1 }}>Cancelar</button>
              <button className="btn-reservar" onClick={() => eliminar(confEliminar.id)} style={{ flex: 1, background: "#e11d48", color: "white" }}>
                Sí, eliminar
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
