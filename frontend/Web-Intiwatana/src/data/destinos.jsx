// Catálogo de destinos, rutas, horarios y terminales.
// Vive en su propio módulo para que App.jsx solo exporte componentes
// y el Fast Refresh de Vite funcione correctamente.
import { Landmark, Trees, MountainSnow, Waves, Building2, Coffee, Sun, Mountain } from "lucide-react";

// ── DATA REAL ─────────────────────────────────────────
export const DESTINOS = [
  {
    nombre: "Vilcas Huaman",
    imagen: "/assets/img/rutas/vilcas.jpg",
    EmojiIcon: () => <Landmark size={24} />, color: "#8AA6A3", region: "Ayacucho",
    duracion: "Local", distancia: "–",
    horarios: [
      { hora: "06:00", tipo: "Diurno",   precio: "S/ 10–15" },
      { hora: "09:00", tipo: "Diurno",   precio: "S/ 10–15" },
      { hora: "14:00", tipo: "Tarde",    precio: "S/ 10–15" },
    ],
    terminales: [
      { nombre: "Terminal del Sur Madre Covadonga", dir: "Av. Cusco 362, Ayacucho 05003", horario: "Abierto 24 horas",   tipo: "ORIGEN"  },
      { nombre: "Agencia de Vilcas Huaman",         dir: "Jr. Lima 350, Ayacucho",        horario: "6:00 AM – 10:00 PM", tipo: "DESTINO" },
    ],
  },
  {
    nombre: "Accomarca",
    imagen: "/assets/img/rutas/accomarca.jpeg",
    EmojiIcon: () => <Trees size={24} />, color: "#127369", region: "Ayacucho",
    duracion: "~8h 30min", distancia: "~340 km",
    horarios: [
      { hora: "06:00", tipo: "Diurno",   precio: "S/ 35–45" },
      { hora: "08:00", tipo: "Diurno",   precio: "S/ 35–45" },
      { hora: "14:00", tipo: "Tarde",    precio: "S/ 40–50" },
      { hora: "21:00", tipo: "Nocturno", precio: "S/ 45–55" },
    ],
    terminales: [
      { nombre: "Terminal del Sur Madre Covadonga", dir: "Av. Cusco 362, Ayacucho 05003", horario: "Abierto 24 horas",    tipo: "ORIGEN"  },
      { nombre: "Agencia de Accomarca",             dir: "Av. Pachacútec 378, Abancay",   horario: "Lun–Dom: 8AM – 10PM", tipo: "DESTINO" },
    ],
  },
  {
    nombre: "Huarcas",
    imagen: "/assets/img/rutas/huarcas.jpeg",
    EmojiIcon: () => <MountainSnow size={24} />, color: "#4C5958", region: "Ayacucho",
    duracion: "~14h", distancia: "~570 km",
    horarios: [
      { hora: "07:00", tipo: "Diurno",   precio: "S/ 60–80" },
      { hora: "17:00", tipo: "Tarde",    precio: "S/ 65–85" },
      { hora: "20:00", tipo: "Nocturno", precio: "S/ 70–90" },
    ],
    terminales: [
      { nombre: "Terminal del Sur Madre Covadonga", dir: "Av. Cusco 362, Ayacucho 05003",        horario: "Abierto 24 horas", tipo: "ORIGEN"  },
      { nombre: "Agencia de Huarcas",               dir: "Av. Andrés Avelino Cáceres, Arequipa", horario: "Abierto 24 horas", tipo: "DESTINO" },
    ],
  },
  {
    nombre: "Pongococha",
    imagen: "/assets/img/rutas/pongococha.jpeg",
    EmojiIcon: () => <Waves size={24} />, color: "#10403B", region: "Ayacucho",
    duracion: "~16h", distancia: "~650 km",
    horarios: [
      { hora: "18:00", tipo: "Nocturno", precio: "S/ 75–95" },
      { hora: "20:00", tipo: "Nocturno", precio: "S/ 75–95" },
    ],
    terminales: [
      { nombre: "Terminal del Sur Madre Covadonga", dir: "Av. Cusco 362, Ayacucho 05003",  horario: "Abierto 24 horas",  tipo: "ORIGEN"  },
      { nombre: "Agencia de Pongococha",            dir: "Av. Lima s/n, Camaná, Arequipa", horario: "8:00 AM – 9:00 PM", tipo: "DESTINO" },
    ],
  },
  {
    nombre: "Vischongo",
    imagen: "/assets/img/rutas/vischongo.jpeg",
    EmojiIcon: () => <Building2 size={24} />, color: "#4C5958", region: "Ayacucho",
    duracion: "~2h", distancia: "~80 km",
    horarios: [
      { hora: "08:00", tipo: "Diurno",   precio: "S/ 10–15" },
      { hora: "15:00", tipo: "Tarde",    precio: "S/ 10–15" },
      { hora: "19:00", tipo: "Nocturno", precio: "S/ 10–15" },
    ],
    terminales: [
      { nombre: "Terminal del Sur Madre Covadonga", dir: "Av. Cusco 362, Ayacucho 05003", horario: "Abierto 24 horas", tipo: "ORIGEN"  },
      { nombre: "Agencia de Vischongo",             dir: "Vischongo, Cangallo, Ayacucho", horario: "Abierto 24 horas", tipo: "DESTINO" },
    ],
  },
  {
    nombre: "Andabamba",
    imagen: "/assets/img/rutas/andabamba.jpeg",
    EmojiIcon: () => <Coffee size={24} />, color: "#127369", region: "Ayacucho",
    duracion: "~12h", distancia: "~500 km",
    horarios: [
      { hora: "07:00", tipo: "Diurno",   precio: "S/ 55–75" },
      { hora: "19:00", tipo: "Nocturno", precio: "S/ 60–80" },
    ],
    terminales: [
      { nombre: "Terminal del Sur Madre Covadonga", dir: "Av. Cusco 362, Ayacucho 05003", horario: "Abierto 24 horas", tipo: "ORIGEN"  },
      { nombre: "Agencia de Andabamba",             dir: "Av. Velasco Astete s/n, Cusco", horario: "Abierto 24 horas", tipo: "DESTINO" },
    ],
  },
  {
    nombre: "Manallasacc",
    imagen: "/assets/img/rutas/manallasacc.jpeg",
    EmojiIcon: () => <Sun size={24} />, color: "#8AA6A3", region: "Ayacucho",
    duracion: "~8h", distancia: "~390 km",
    horarios: [
      { hora: "06:00", tipo: "Diurno",   precio: "S/ 40–55" },
      { hora: "20:00", tipo: "Nocturno", precio: "S/ 45–60" },
    ],
    terminales: [
      { nombre: "Terminal del Sur Madre Covadonga", dir: "Av. Cusco 362, Ayacucho 05003",    horario: "Abierto 24 horas",   tipo: "ORIGEN"  },
      { nombre: "Agencia de Manallasacc",           dir: "Prolongación Lambayeque 180, Ica", horario: "6:00 AM – 10:00 PM", tipo: "DESTINO" },
    ],
  },
  {
    nombre: "Chiribamba",
    imagen: "/assets/img/rutas/chiribamba.jpeg",
    EmojiIcon: () => <Mountain size={24} />, color: "#10403B", region: "Ayacucho",
    duracion: "~5h", distancia: "~195 km",
    horarios: [
      { hora: "06:00", tipo: "Diurno",   precio: "S/ 25–35" },
      { hora: "10:00", tipo: "Diurno",   precio: "S/ 25–35" },
      { hora: "14:00", tipo: "Tarde",    precio: "S/ 30–40" },
      { hora: "18:00", tipo: "Nocturno", precio: "S/ 30–40" },
    ],
    terminales: [
      { nombre: "Terminal del Sur Madre Covadonga", dir: "Av. Cusco 362, Ayacucho 05003",   horario: "Abierto 24 horas",  tipo: "ORIGEN"  },
      { nombre: "Agencia de Chiribamba",            dir: "Av. Martinelli s/n, Andahuaylas", horario: "7:00 AM – 9:00 PM", tipo: "DESTINO" },
    ],
  },
];