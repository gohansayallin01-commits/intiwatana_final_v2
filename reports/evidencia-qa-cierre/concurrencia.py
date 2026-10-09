import threading, json, urllib.request, urllib.error

BASE = "http://localhost:8081"

def retencion(asiento):
    body = {
        "viajeId": 2, "asientoIds": [asiento],
        "comprador": {"nombres": "Concurrente", "apellidos": "Test Uno",
                      "documento": "70000001", "telefono": "900000001"}
    }
    req = urllib.request.Request(BASE + "/api/v1/ventas/retencion",
        data=json.dumps(body).encode(), headers={"Content-Type": "application/json"}, method="POST")
    try:
        with urllib.request.urlopen(req, timeout=15) as r:
            return r.status, r.read().decode()[:300]
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode()[:300]

results = []
def worker(n):
    results.append((n,) + retencion(42))  # asiento 1B

t1 = threading.Thread(target=worker, args=(1,))
t2 = threading.Thread(target=worker, args=(2,))
t1.start(); t2.start()
t1.join(); t2.join()
for r in sorted(results):
    print("HILO", r[0], "->", r[1], r[2][:200])