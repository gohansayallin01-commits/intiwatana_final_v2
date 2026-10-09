export default async function run(page, ui) {
  // Ir a la pagina de reserva
  await page.goto('http://localhost:5180/');
  await page.waitForTimeout(1500);
  // click RESERVAR PASAJE
  const snap = await ui.snapshot();
  const m = snap.match(/@(e\d+) (?:button|link) "RESERVAR PASAJE"/i);
  let clicked = false;
  if (m) { await ui.click(m[1]); clicked = true; }
  else {
    // probar por texto
    const el = page.getByText('RESERVAR PASAJE', { exact: false }).first();
    if (await el.count()) { await el.click(); clicked = true; }
  }
  await page.waitForTimeout(2000);
  const full = await ui.snapshot({ full: true });
  return { clicked, url: page.url(), full: full.slice(0, 3000) };
}