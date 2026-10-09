export default async function run(page, ui) {
  const log = [];
  await page.goto('http://localhost:5180/pasajes');
  await page.waitForTimeout(1500);

  const selects = page.locator('select');
  await selects.nth(0).selectOption({ label: 'Huamanga' });
  await selects.nth(1).selectOption({ label: 'Vilcas Huaman' });
  await page.waitForTimeout(400);

  await page.getByRole('button', { name: 'Buscar' }).click();
  await page.waitForTimeout(3000);

  await page.getByRole('button', { name: /Elegir asiento/i }).first().click();
  await page.waitForTimeout(3000);

  // Seleccionar un asiento disponible y confirmar que queda seleccionado
  const seat = page.locator('[aria-label*="Disponible"]').first();
  const seatLabel = await seat.getAttribute('aria-label');
  await seat.click();
  await page.waitForTimeout(1000);
  log.push('ASIENTO: ' + seatLabel);

  // Abrir modal de reserva
  await page.getByRole('button', { name: /Reservar y pagar/i }).click();
  await page.waitForSelector('#rsv-nombres', { timeout: 10000 });
  await page.waitForTimeout(600);

  await page.locator('#rsv-nombres').fill('Ana UI');
  await page.locator('#rsv-apellidos').fill('Prueba Visual');
  await page.locator('#rsv-documento').fill('70111223');
  await page.locator('#rsv-telefono').fill('988777666');

  await page.getByRole('button', { name: /Continuar con la reserva/i }).click();

  // Esperar el paso 2 (modal "Reserva registrada")
  await page.waitForSelector('text=Reserva registrada', { timeout: 15000 });
  await page.waitForTimeout(1500);

  const modalText = (await page.locator('.modal').innerText()).replace(/\s+/g, ' ');
  log.push('MODAL: ' + modalText);

  await page.screenshot({ path: 'reports/evidencia-qa-cierre/ui-reserva-registrada.png', fullPage: false });

  const refMatch = modalText.match(/RET-[A-Z0-9]+/i);
  return {
    hasReservaRegistrada: /Reserva registrada/i.test(modalText),
    referencia: refMatch ? refMatch[0] : null,
    hasNoValidaParaEmbarque: /NO VÁLIDA PARA EMBARQUE/i.test(modalText),
    hasDescargarConstancia: /Descargar constancia/i.test(modalText),
    hasPendienteDePago: /pendiente de pago/i.test(modalText),
    modalText
  };
}