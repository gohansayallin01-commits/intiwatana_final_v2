// Logo oficial de la empresa — imagen en public/assets/logo.png
export default function LogoSVG({ width = 52, height = 52, style = {}, className = "", alt = "Empresa de Transportes SRL Intiwatana" }) {
  const numericWidth = typeof width === "number" ? width : parseInt(width, 10) || 52;
  const borderRadius = numericWidth <= 36 ? "6px" : numericWidth <= 50 ? "8px" : "12px";
  const padding = numericWidth <= 36 ? "2px" : "3px";

  return (
    <div
      className={`company-logo-wrap ${className}`}
      style={{
        width: typeof width === "number" ? `${width}px` : width,
        height: typeof height === "number" ? `${height}px` : height,
        backgroundColor: "#ffffff",
        borderRadius: borderRadius,
        padding: padding,
        display: "inline-flex",
        alignItems: "center",
        justifyContent: "center",
        boxShadow: "0 2px 8px rgba(0,0,0,0.22)",
        overflow: "hidden",
        flexShrink: 0,
        boxSizing: "border-box",
        ...style,
      }}
    >
      <img
        src="/assets/logo.png"
        alt={alt}
        style={{
          width: "100%",
          height: "100%",
          objectFit: "contain",
          display: "block",
        }}
      />
    </div>
  );
}
