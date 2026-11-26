/**
 * Spend Tap -> Google Sheets yearly export.
 *
 * Setup (about 2 minutes, in YOUR Google account):
 * 1. Create or open the Google Sheet you want the yearly data in.
 * 2. Extensions > Apps Script. Delete the sample code and paste this whole file.
 * 3. Change SECRET below to any long random phrase. Put the same phrase in the app.
 * 4. Deploy > New deployment > type "Web app".
 *      Execute as: Me
 *      Who has access: Anyone      (the SECRET is what protects it)
 * 5. Authorise when asked, copy the Web app URL (ends in /exec), paste it in the app.
 *
 * Each year gets its own tab ("2026"). Re-exporting a year overwrites that tab only.
 */
const SECRET = 'change-me-to-a-long-random-phrase';

const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

function doPost(e) {
  try {
    const body = JSON.parse(e.postData.contents);
    if (body.token !== SECRET) return reply_({ ok: false, error: 'Wrong secret. It must match the script.' });

    const r = body.report;
    const ss = SpreadsheetApp.getActiveSpreadsheet();
    const name = String(r.year);
    const sheet = ss.getSheetByName(name) || ss.insertSheet(name);
    sheet.clearContents();

    const header = ['Month'].concat(r.categories, ['Total spend', 'Savings / SIP', 'Spend + savings']);
    const rows = MONTHS.map(function (m, i) {
      const spend = r.spendTotals[i];
      return [m].concat(r.months[i], [spend, r.savings[i], spend + r.savings[i]]);
    });

    const totals = ['Total'];
    for (let c = 1; c < header.length; c++) {
      let sum = 0;
      rows.forEach(function (row) { sum += row[c]; });
      totals.push(sum);
    }

    const values = [header].concat(rows, [totals]);
    sheet.getRange(1, 1, values.length, header.length).setValues(values);
    sheet.getRange(1, 1, 1, header.length).setFontWeight('bold');
    sheet.getRange(values.length, 1, 1, header.length).setFontWeight('bold');
    sheet.setFrozenRows(1);
    sheet.getRange(2, 2, values.length - 1, header.length - 1).setNumberFormat('#,##0.00');

    return reply_({ ok: true });
  } catch (err) {
    return reply_({ ok: false, error: String(err) });
  }
}

function reply_(obj) {
  return ContentService.createTextOutput(JSON.stringify(obj)).setMimeType(ContentService.MimeType.JSON);
}
