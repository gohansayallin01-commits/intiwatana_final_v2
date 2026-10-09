import urllib.request, json
ref = open('ref_expira.txt').read().strip()

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

s, t = call('POST', '/api/v1/auth/login', body={'username': 'cajero1', 'password': 'cajero123'})
print('LOGIN:', s, t[:120])
tok = json.loads(t)['token']
s, t = call('POST', '/api/v1/ventas/reservas/' + ref + '/confirmar', token=tok,
            body={'metodoPago': 'EFECTIVO'})
print('CONFIRMAR VENCIDA:', s, t[:250])