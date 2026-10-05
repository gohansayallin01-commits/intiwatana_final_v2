import React, { useState, useEffect, useCallback } from "react";
import { useAuth } from "../context/useAuth";
import { CheckCircle2, Armchair, AlertCircle, Wrench, X, Save } from "lucide-react";

export default function CrudAsientos({ viajeId, onClose, showToast }) {
  const { session } = useAuth();
  const [asientos, setAsientos] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [asientoSel, setAsientoSel] = useState(null);
  const [guardando, setGuardando] = useState(false);

  const cargarAsientos = useCallback(async () => {
    setCargando(true);
    try {
      const res = await fetch(`/api/v1/viajes/${viajeId}/asientos`, {
        headers: { Authorization: `Bearer ${session.token}` }
      });
      if (res.ok) {
        setAsientos(await res.json());
      }
    } catch {
      showToast("⚠ Error cargando asientos");
    } finally {
      setCargando(false);
    }
  }, [viajeId, session.token, showToast]);

  useEffect(() => {
    cargarAsientos();
  }, [cargarAsientos]);

  const getColorPorEstado = (estado) => {
    switch(estado) {
      case "DISPONIBLE": return "#10b981"; // Verde
      case "OCUPADO":
      case "VENDIDO": return "#ef4444"; // Rojo
      case "RESERVADO": return "#f59e0b"; // Naranja
      case "BLOQUEADO":
      case "EN_MANTENIMIENTO": return "#6b7280"; // Gris
      default: return "#9ca3af";
    }
  };

  const handleGuardar = async (e) => {
    e.preventDefault();
    if (!asientoSel) return;
    setGuardando(true);
    try {
      const res = await fetch(`/api/v1/asientos/${asientoSel.id}`, {
        method: "PATCH",
        headers: { 
          "Content-Type": "application/json",
          Authorization: `Bearer ${session.token}` 
        },
        body: JSON.stringify({
          estado: asientoSel.estado,
          tipo: asientoSel.tipo,
          precio: parseFloat(asientoSel.precio || 0)
        })
      });
      if (res.ok) {
        showToast("✓ Asiento actualizado");
        cargarAsientos();
        setAsientoSel(null);
      } else {
        const err = await res.json().catch(()=>({}));
        showToast("⚠ " + (err.mensaje || "Error actualizando asiento"));
      }
    } catch {
      showToast("⚠ Error actualizando asiento");
    } finally {
      setGuardando(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose} style={{ zIndex: 9999 }}>
      <div className="modal" style={{ maxWidth: 700 }} onClick={e => e.stopPropagation()}>
        <div className="modal-header">
          <div className="modal-badge"><Armchair size={24} /></div>
          <div>
            <div className="modal-title">Mapa de Asientos</div>
            <div className="modal-subtitle">Viaje #{viajeId}</div>
          </div>
          <button className="modal-close" onClick={onClose}><X size={24} /></button>
        </div>
        
        <div className="modal-body" style={{ display: "flex", gap: 20 }}>
          {/* Mapa Visual */}
          <div style={{ flex: 1, background: "#f4f9f8", padding: 20, borderRadius: 12, border: "1px solid #e2edeb", minHeight: 300 }}>
            {cargando ? (
              <div style={{ textAlign: "center", color: "#7a9e9b", marginTop: 100 }}>Cargando mapa...</div>
            ) : (
              <div style={{ display: "grid", gridTemplateColumns: "repeat(4, 1fr)", gap: 10 }}>
                {asientos.sort((a,b) => a.id - b.id).map(a => (
                  <button 
                    key={a.id} 
                    onClick={() => setAsientoSel({...a})}
                    style={{
                      background: getColorPorEstado(a.estado),
                      color: "white",
                      border: asientoSel?.id === a.id ? "3px solid #0d3330" : "none",
                      borderRadius: 8,
                      padding: "10px 0",
                      fontWeight: "bold",
                      cursor: "pointer",
                      gridColumn: a.columna === 3 ? "4" : a.columna === 4 ? "5" : "auto", // Espacio para pasillo si es necesario
                    }}>
                    {a.numeroAsiento}
                  </button>
                ))}
              </div>
            )}
            <div style={{ display: "flex", gap: 10, marginTop: 20, flexWrap: "wrap", fontSize: 12, color: "#4C5958", justifyContent: "center" }}>
              <div style={{ display: "flex", alignItems: "center", gap: 4 }}><div style={{ width:12, height:12, borderRadius:4, background:"#10b981" }}/> Disponible</div>
              <div style={{ display: "flex", alignItems: "center", gap: 4 }}><div style={{ width:12, height:12, borderRadius:4, background:"#f59e0b" }}/> Reservado</div>
              <div style={{ display: "flex", alignItems: "center", gap: 4 }}><div style={{ width:12, height:12, borderRadius:4, background:"#ef4444" }}/> Vendido</div>
              <div style={{ display: "flex", alignItems: "center", gap: 4 }}><div style={{ width:12, height:12, borderRadius:4, background:"#6b7280" }}/> Bloqueado</div>
            </div>
          </div>

          {/* Formulario Edición */}
          <div style={{ width: 250 }}>
            {asientoSel ? (
              <form onSubmit={handleGuardar} style={{ background: "white", padding: 15, borderRadius: 12, border: "1px solid #e2edeb" }}>
                <h4 style={{ margin: "0 0 15px", color: "#0d3330" }}>Asiento {asientoSel.numeroAsiento}</h4>
                <div style={{ marginBottom: 10 }}>
                  <label style={{ display: "block", fontSize: 11, fontWeight: "bold", color: "#7a9e9b", marginBottom: 5 }}>ESTADO</label>
                  <select 
                    style={{ width: "100%", padding: 8, borderRadius: 8, border: "1px solid #d4dbd9" }}
                    value={asientoSel.estado}
                    onChange={e => setAsientoSel({...asientoSel, estado: e.target.value})}
                  >
                    <option value="DISPONIBLE">Disponible</option>
                    <option value="RESERVADO">Reservado</option>
                    <option value="VENDIDO">Vendido (Ocupado)</option>
                    <option value="BLOQUEADO">Bloqueado (Mantenimiento)</option>
                  </select>
                </div>
                <div style={{ marginBottom: 10 }}>
                  <label style={{ display: "block", fontSize: 11, fontWeight: "bold", color: "#7a9e9b", marginBottom: 5 }}>TIPO</label>
                  <select 
                    style={{ width: "100%", padding: 8, borderRadius: 8, border: "1px solid #d4dbd9" }}
                    value={asientoSel.tipo}
                    onChange={e => setAsientoSel({...asientoSel, tipo: e.target.value})}
                  >
                    <option value="NORMAL">Normal</option>
                    <option value="VIP">VIP</option>
                    <option value="VENTANA">Ventana</option>
                    <option value="PASILLO">Pasillo</option>
                  </select>
                </div>
                <div style={{ marginBottom: 15 }}>
                  <label style={{ display: "block", fontSize: 11, fontWeight: "bold", color: "#7a9e9b", marginBottom: 5 }}>PRECIO PERSONALIZADO (Opcional)</label>
                  <input 
                    type="number" step="0.01"
                    style={{ width: "100%", padding: 8, borderRadius: 8, border: "1px solid #d4dbd9" }}
                    value={asientoSel.precio || ""}
                    placeholder="S/ 0.00"
                    onChange={e => setAsientoSel({...asientoSel, precio: e.target.value})}
                  />
                </div>
                <button type="submit" disabled={guardando} style={{ width: "100%", padding: 10, background: "#127369", color: "white", borderRadius: 8, border: "none", fontWeight: "bold", cursor: "pointer", display: "flex", alignItems: "center", justifyContent: "center", gap: 5 }}>
                  {guardando ? "Guardando..." : <><Save size={16} /> Guardar Cambios</>}
                </button>
              </form>
            ) : (
              <div style={{ height: "100%", display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", color: "#7a9e9b", textAlign: "center", padding: 20 }}>
                <Armchair size={40} style={{ opacity: 0.3, marginBottom: 10 }} />
                <p style={{ margin: 0, fontSize: 13 }}>Selecciona un asiento en el mapa para editarlo.</p>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
