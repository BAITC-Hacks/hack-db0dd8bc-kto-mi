import http from 'node:http';
import {readFile} from 'node:fs/promises';
import {fileURLToPath} from 'node:url';

// Development-only static server: proxy the existing backend without changing it.
const port = Number(process.env.QADAM_FRONTEND_PORT || 8082);
const backend = process.env.QADAM_BACKEND_URL || 'http://127.0.0.1:8080';
const staticRoot = new URL('../../main/resources/static/', import.meta.url);
const files = new Map([
  ['/', ['index.html', 'text/html; charset=utf-8']],
  ['/index.html', ['index.html', 'text/html; charset=utf-8']],
  ['/app.js', ['app.js', 'text/javascript; charset=utf-8']],
  ['/lesson-utils.mjs', ['lesson-utils.mjs', 'text/javascript; charset=utf-8']],
  ['/styles.css', ['styles.css', 'text/css; charset=utf-8']],
  ['/favicon.svg', ['favicon.svg', 'image/svg+xml']],
]);

http.createServer(async (request, response) => {
  const path = new URL(request.url, 'http://localhost').pathname;
  try {
    if (path.startsWith('/api/')) {
      const chunks = [];
      for await (const chunk of request) chunks.push(chunk);
      const body = Buffer.concat(chunks);
      const upstream = await fetch(new URL(request.url, backend), {
        method: request.method,
        headers: body.length ? {'Content-Type': 'application/json'} : {},
        body: body.length ? body : undefined,
      });
      response.writeHead(upstream.status, {
        'Content-Type': upstream.headers.get('content-type') || 'application/json',
        'Cache-Control': 'no-store',
      });
      response.end(Buffer.from(await upstream.arrayBuffer()));
      return;
    }
    const file = files.get(path);
    if (!file) { response.writeHead(404); response.end('Not found'); return; }
    const contents = await readFile(fileURLToPath(new URL(file[0], staticRoot)));
    response.writeHead(200, {'Content-Type': file[1], 'Cache-Control':'no-store'});
    response.end(contents);
  } catch {
    response.writeHead(502, {'Content-Type':'application/json; charset=utf-8'});
    response.end(JSON.stringify({message:'Не удалось связаться с сервером Qadam. Проверьте, что бэкенд запущен.'}));
  }
}).listen(port, '127.0.0.1', () => {
  console.log(`Qadam frontend: http://127.0.0.1:${port}`);
  console.log(`Existing backend: ${backend}`);
});
