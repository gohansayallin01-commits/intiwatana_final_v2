import React, { useRef } from 'react';
import { Download, CheckCircle2, Ticket } from 'lucide-react';
import LogoSVG from './LogoSVG';

export default function BoletoVirtual({ boleto, viaje, asiento, onClose }) {
  const boletoRef = useRef(null);

  // Asegurar compatibilidad con nombres de BD (snake_case) o DTO (camelCase)
  const numBoleto = boleto?.numeroBoleto || boleto?.numero_boleto || "TKT-0000";
  const numAsiento = asiento?.numeroAsiento || boleto?.numero_asiento || "—";
  const pasajero = boleto?.pasajeroDni || boleto?.pasajero_dni || "Pasajero Principal";
  const costo = boleto?.precioPagado || boleto?.precio_pagado || viaje?.precioAdulto || "0.00";
  
  const origen = viaje?.ruta?.origen?.ciudad || boleto?.ciudad_origen || "Origen";
  const destino = viaje?.ruta?.destino?.ciudad || boleto?.ciudad_destino || "Destino";
  
  const fechaSalida = viaje?.fechaHoraSalida || boleto?.fecha_hora_salida;
  const fechaFmt = fechaSalida ? new Date(fechaSalida).toLocaleDateString("es-PE", { day: '2-digit', month: 'short', year: 'numeric' }) : "—";
  const horaFmt = fechaSalida ? new Date(fechaSalida).toLocaleTimeString("es-PE", { hour: '2-digit', minute: '2-digit' }) : "—";

  const placaBus = viaje?.bus?.placa || "—";
  const tipoBus = viaje?.bus?.tipo || "NORMAL";

  return (
    <div className="modal-overlay" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 9999, background: 'rgba(7, 32, 30, 0.85)', backdropFilter: 'blur(5px)' }} onClick={onClose}>
      <div 
        className="ticket-wrapper" 
        onClick={(e) => e.stopPropagation()}
        style={{ animation: "slideUp 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275)" }}
      >
        <style>{`
          .ticket-wrapper {
            width: 100%;
            max-width: 420px;
            display: flex;
            flex-direction: column;
            gap: 1.5rem;
          }
          .premium-ticket {
            background: #ffffff;
            border-radius: 20px;
            overflow: hidden;
            box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
            position: relative;
            filter: drop-shadow(0 0 15px rgba(245, 197, 24, 0.15));
          }
          .pt-header {
            background: #0d3330;
            color: #ffffff;
            padding: 1.5rem;
            display: flex;
            justify-content: space-between;
            align-items: center;
          }
          .pt-body {
            padding: 1.5rem;
            position: relative;
          }
          .pt-divider {
            position: relative;
            height: 2px;
            background: repeating-linear-gradient(90deg, #d4dbd9, #d4dbd9 6px, transparent 6px, transparent 12px);
            margin: 0;
          }
          /* Recortes laterales simulando ticket perforado */
          .pt-divider::before, .pt-divider::after {
            content: '';
            position: absolute;
            top: -12px;
            width: 24px;
            height: 24px;
            background: #0d3330; /* El color de fondo del overlay para que parezca un hueco */
            border-radius: 50%;
            box-shadow: inset 0px 4px 6px rgba(0,0,0,0.4);
          }
          .pt-divider::before { left: -12px; }
          .pt-divider::after { right: -12px; }
          
          .pt-footer {
            padding: 1.5rem;
            background: #f8faf9;
            text-align: center;
          }
          
          .pt-city {
            font-family: 'Playfair Display', serif;
            font-size: 2.2rem;
            font-weight: 800;
            color: #127369;
            line-height: 1;
          }
          .pt-city-label {
            font-size: 0.7rem;
            text-transform: uppercase;
            letter-spacing: 0.1em;
            color: #8AA6A3;
            font-weight: 700;
            margin-bottom: 4px;
          }
          
          .pt-info-grid {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 1.2rem;
            margin-top: 1.5rem;
          }
          .pt-info-block span {
            display: block;
          }
          .pt-label {
            font-size: 0.65rem;
            text-transform: uppercase;
            letter-spacing: 0.08em;
            color: #8AA6A3;
            font-weight: 700;
            margin-bottom: 2px;
          }
          .pt-value {
            font-size: 1rem;
            color: #0d3330;
            font-weight: 700;
          }
          .pt-value-large {
            font-size: 1.5rem;
            color: #F5C518;
            font-weight: 800;
            font-family: 'Playfair Display', serif;
          }
          .pt-barcode {
            height: 60px;
            background-image: repeating-linear-gradient(90deg, #0d3330 0, #0d3330 2px, transparent 0, transparent 4px, #0d3330 0, #0d3330 6px, transparent 0, transparent 8px, #0d3330 0, #0d3330 1px, transparent 0, transparent 5px);
            opacity: 0.8;
            margin-bottom: 0.5rem;
          }
        `}</style>

        {/* CONTENEDOR DEL TICKET */}
        <div className="premium-ticket" ref={boletoRef}>
          <div className="pt-header">
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <LogoSVG width={28} height={28} />
              <div>
                <div style={{ fontFamily: "'Playfair Display', serif", fontWeight: 700, color: '#F5C518', fontSize: '1rem', lineHeight: 1 }}>INTIWATANA</div>
                <div style={{ fontSize: '0.65rem', letterSpacing: '0.1em', opacity: 0.8 }}>BOARDING PASS</div>
              </div>
            </div>
            <div style={{ textAlign: 'right' }}>
              <div style={{ fontSize: '0.65rem', letterSpacing: '0.1em', opacity: 0.8 }}>CLASE</div>
              <div style={{ fontWeight: 700, color: '#10b981' }}>{tipoBus}</div>
            </div>
          </div>

          <div className="pt-body">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div style={{ flex: 1 }}>
                <div className="pt-city-label">Origen</div>
                <div className="pt-city">{origen.substring(0, 3).toUpperCase()}</div>
                <div style={{ fontSize: '0.85rem', color: '#4C5958', fontWeight: 600 }}>{origen}</div>
              </div>
              <div style={{ padding: '0 15px', color: '#127369', opacity: 0.4 }}>
                <Ticket size={32} />
              </div>
              <div style={{ flex: 1, textAlign: 'right' }}>
                <div className="pt-city-label">Destino</div>
                <div className="pt-city">{destino.substring(0, 3).toUpperCase()}</div>
                <div style={{ fontSize: '0.85rem', color: '#4C5958', fontWeight: 600 }}>{destino}</div>
              </div>
            </div>

            <div className="pt-info-grid">
              <div className="pt-info-block">
                <span className="pt-label">Pasajero</span>
                <span className="pt-value">{pasajero}</span>
              </div>
              <div className="pt-info-block">
                <span className="pt-label">Fecha</span>
                <span className="pt-value">{fechaFmt}</span>
              </div>
              <div className="pt-info-block">
                <span className="pt-label">Hora Salida</span>
                <span className="pt-value" style={{ color: '#e11d48' }}>{horaFmt}</span>
              </div>
              <div className="pt-info-block">
                <span className="pt-label">Asiento</span>
                <span className="pt-value-large">{numAsiento}</span>
              </div>
              <div className="pt-info-block">
                <span className="pt-label">Bus / Placa</span>
                <span className="pt-value">{placaBus}</span>
              </div>
              <div className="pt-info-block">
                <span className="pt-label">Monto Pagado</span>
                <span className="pt-value">S/ {costo}</span>
              </div>
            </div>
          </div>

          {/* Línea divisoria perforada */}
          <div className="pt-divider"></div>

          <div className="pt-footer">
            <div className="pt-barcode"></div>
            <span style={{ fontSize: '0.8rem', letterSpacing: '0.2em', color: '#4C5958', fontFamily: 'monospace', fontWeight: 700 }}>
              {numBoleto}
            </span>
          </div>
        </div>

        {/* BOTONES DE ACCIÓN (Fuera del ticket para no imprimirlos) */}
        <div style={{ display: 'flex', gap: '10px' }}>
          <button 
            onClick={onClose}
            style={{ flex: 1, padding: '12px', background: 'rgba(255,255,255,0.1)', color: 'white', border: '1px solid rgba(255,255,255,0.2)', borderRadius: '10px', fontWeight: 700, cursor: 'pointer' }}
          >
            Cerrar
          </button>
          <button 
            onClick={() => {
              // Lógica simple para imprimir o guardar
              window.print();
            }}
            style={{ flex: 2, padding: '12px', background: '#F5C518', color: '#0d3330', border: 'none', borderRadius: '10px', fontWeight: 800, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px' }}
          >
            <Download size={18} /> Guardar Ticket
          </button>
        </div>
      </div>
    </div>
  );
}
