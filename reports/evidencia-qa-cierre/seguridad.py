import urllib.request, json

def call(method, ep, token=None, body=None):
    data = json.dumps(body).encode() if body is not None else None
    h = {'Content-Type': 'application/json'}
    if token:
        h['Authorization'] = 'Bearer ' + token
    req = urllib.request.Request('http://localhost:8081' + ep, data=data, headers=h, method=method)
    try:
        with urllib.request.urlopen(req, timeout=10) as r:
            return r.status, r.read().decode()
    except Exception as e:
        return getattr(e, 'code', 'ERR'), (e.read().decode() if hasattr(e, 'read') else str(e))

def login(u, p):
    s, t = call('POST', '/api/v1/auth/login', body={'username': u, 'password': p})
    return json.loads(t)['token']

# nueva venta completa en asiento 42 para probar chofer no autorizado
s, t = call('POST', '/api/v1/ventas/retencion', body={
    'viajeId': 2, 'asientoIds': [42],
    'comprador': {'nombres': 'Seg', 'apellidos': 'Test', 'documento': '70000003', 'telefono': '900000003'}})
ref = json.loads(t)['referencia']
s, t = call('POST', '/api/v1/ventas/retencion/' + ref + '/reservar')
cajero = login('cajero1', 'cajero123')
s, t = call('POST', '/api/v1/ventas/reservas/' + ref + '/confirmar', token=cajero,
            body={'metodoPago': 'EFECTIVO'})
boleto = json.loads(t)[0]
num = boleto['numeroBoleto']
print('BOLETO CREADO:', num, 'QR:', boleto['codigoQr'][:40])

# chofer2 = login chofer2 (no asignado a viaje 2)
tok2 = login('chofer2', 'chofer123')
s, t = call('POST', '/api/v1/boletos/escanear?codigoQr=' + num, token=tok2)
print('CHOFER NO AUTORIZADO:', s, t[:200])

# chofer intentando listar reservas (rol no permitido) -> 403
s, t = call('GET', '/api/v1/ventas/reservas', token=tok2)
print('CHOFER -> /reservas:', s, t[:120])

# admin listando reservas -> 200
admin = login('admin', 'admin123')
s, t = call('GET', '/api/v1/ventas/reservas', token=admin)
print('ADMIN -> /reservas:', s, 'len=', len(json.loads(t)))

# PDF boleto (con QR)
s, t = call('GET', '/api/v1/ventas/boleto/' + num + '/pdf?documento=70000003')
print('BOLETO PDF status (texto crudo len):', s, 'bodylen=', len(t) if isinstance(t, str) else '?')

# token malformado
s, t = call('GET', '/api/v1/ventas/reservas', token='TOKEN.FALSO.XYZ')
print('TOKEN INVALIDO:', s, t[:120])
open('num_boleto_seg.txt', 'w').write(num)