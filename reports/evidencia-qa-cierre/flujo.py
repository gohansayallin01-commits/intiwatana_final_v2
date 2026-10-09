import urllib.request, urllib.error, json, sys

BASE = "http://localhost:8081"

def call(method, path, body=None, token=None, raw=False):
    url = BASE + path
    data = None
    headers = {}
    if body is not None:
        data = json.dumps(body).encode("utf-8")
        headers["Content-Type"] = "application/json"
    if token:
        headers["Authorization"] = "Bearer " + token
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=15) as r:
            content = r.read()
            if raw:
                return r.status, content, dict(r.headers)
            try:
                return r.status, json.loads(content.decode("utf-8")), dict(r.headers)
            except Exception:
                return r.status, content, dict(r.headers)
    except urllib.error.HTTPError as e:
        content = e.read()
        return e.code, content, dict(e.headers)

def jprint(label, obj):
    print(label, json.dumps(obj, ensure_ascii=True)[:800])

# 1. retencion
body = {
    "viajeId": 2,
    "asientoIds": [41],
    "comprador": {
        "nombres": "Juan Carlos",
        "apellidos": "Perez Quispe",
        "documento": "71234567",
        "telefono": "987654321",
        "email": "juan.perez@example.com"
    }
}
st, data, _ = call("POST", "/api/v1/ventas/retencion", body)
print("== POST /retencion ->", st)
jprint("retencion:", data)
if st not in (200, 201):
    sys.exit("FALLO retencion")
ref = data["referencia"]
print("REF=" + ref)
print("EXPIRA_EN=" + str(data.get("expiraEn")))
print("ESTADO=" + str(data.get("estado")))

# 2. reservar pendiente pago
st2, data2, _ = call("POST", "/api/v1/ventas/retencion/" + ref + "/reservar?metodoPago=EFECTIVO")
print("== POST /reservar ->", st2)
jprint("reserva:", data2)
if st2 != 200:
    sys.exit("FALLO reservar")
print("ESTADO2=" + str(data2.get("estado")))
print("EXPIRA2=" + str(data2.get("expiraEn")))
print("REF2=" + str(data2.get("referencia")))
print("TOTAL2=" + str(data2.get("total")))
print("ASIENTOS2=" + json.dumps(data2.get("asientos")))
print("VALEMBARQUE=" + str(data2.get("validoParaEmbarque")))

# 3. constancia pdf
st3, content3, headers3 = call("GET", "/api/v1/ventas/retencion/" + ref + "/constancia.pdf?documento=71234567", raw=True)
print("== GET constancia.pdf ->", st3, "ctype=", headers3.get("Content-Type"), "len=", len(content3) if isinstance(content3, bytes) else "?")
open(r"C:\Users\USER\Downloads\intiwatana_final_v2\reports\evidencia-qa-cierre\constancia_" + ref + ".pdf", "wb").write(content3)

# 4. recuperar reserva (mis reservas)
st4, data4, _ = call("GET", "/api/v1/ventas/retencion/" + ref + "?documento=71234567")
print("== GET /retencion/{ref} ->", st4)
jprint("recuperada:", data4)

print("FLOW1_OK ref=" + ref)