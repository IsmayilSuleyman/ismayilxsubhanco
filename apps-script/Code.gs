// Apps Script web endpoint for the Shared Budget tracker.
//
// Bind this to the Google Sheet that powers the static site
// (Extensions -> Apps Script). Deploy as: New deployment ->
// Web app -> Execute as: Me, Who has access: Anyone.

var TIMEZONE = 'Asia/Baku';

var PEOPLE = ['Sübhan', 'İsmayıl', 'Shared'];

var EXPENSE_CATEGORIES = [
  '🥕 Groceries',
  '🍝 Dining out',
  '⚡ Utilities',
  '🚖 Transportation',
  '🖼 Household',
  '🤳 Subscriptions',
  '🎉 Entertainment',
  '🏥 Healthcare',
  '😀 Other'
];
var TOPUP_CATEGORY = '➕ Top up';

var MAX_NOTE_LENGTH = 200;

// Sheet header. ID column is required for update/delete.
var HEADER_ROW = [
  'Date',
  'Time',
  'Amount (AZN)',
  'Transaction by',
  'Transaction category',
  'Note',
  'ID'
];
var ID_HEADER = 'ID';

// Returns the sheet to write to (the first tab). Ensures the
// header row exists and contains an ID column.
function getTargetSheet_() {
  var ss = SpreadsheetApp.getActive();
  if (!ss) throw new Error('Script is not bound to a spreadsheet.');
  var sheets = ss.getSheets();
  if (sheets.length === 0) throw new Error('No tabs.');
  var sh = sheets[0];
  if (sh.getLastRow() === 0) {
    sh.appendRow(HEADER_ROW);
  } else {
    var lastCol = Math.max(1, sh.getLastColumn());
    var header = sh.getRange(1, 1, 1, lastCol).getValues()[0];
    if (header.indexOf(ID_HEADER) === -1) {
      sh.getRange(1, lastCol + 1).setValue(ID_HEADER);
    }
  }
  return sh;
}

function getHeader_(sh) {
  return sh.getRange(1, 1, 1, sh.getLastColumn()).getValues()[0];
}

function getIdColumn_(sh) {
  var header = getHeader_(sh);
  var idx = header.indexOf(ID_HEADER);
  if (idx === -1) {
    sh.getRange(1, header.length + 1).setValue(ID_HEADER);
    return header.length + 1;
  }
  return idx + 1;
}

function findRowById_(sh, id) {
  if (!id) return -1;
  var col = getIdColumn_(sh);
  var last = sh.getLastRow();
  if (last < 2) return -1;
  var values = sh.getRange(2, col, last - 1, 1).getValues();
  for (var i = 0; i < values.length; i++) {
    if (String(values[i][0]) === String(id)) return i + 2;
  }
  return -1;
}

function formatAmountStr_(amount, isTopUp) {
  return isTopUp ? ' ₼' + amount.toFixed(2) : '- ₼' + amount.toFixed(2);
}

function validatePayload_(body) {
  var amount = Number(body.amount);
  if (!isFinite(amount) || amount <= 0) {
    return { error: 'amount must be a positive number' };
  }
  var isTopUp = body.isTopUp === true;
  var who = body.transactionBy;
  if (PEOPLE.indexOf(who) === -1) {
    return { error: 'transactionBy must be Sübhan, İsmayıl, or Shared' };
  }
  var category = body.category;
  if (isTopUp) {
    category = TOPUP_CATEGORY;
    if (who === 'Shared') return { error: 'top up cannot be Shared' };
  } else {
    if (EXPENSE_CATEGORIES.indexOf(category) === -1) {
      return { error: 'unknown category: ' + category };
    }
  }
  var note = body.note ? String(body.note) : '';
  if (note.length > MAX_NOTE_LENGTH) note = note.substring(0, MAX_NOTE_LENGTH);
  return { amount: amount, isTopUp: isTopUp, who: who, category: category, note: note };
}

function rowDataFor_(sh, vals, idValue, existingRow) {
  var header = getHeader_(sh);
  var data = existingRow ? existingRow.slice() : [];
  while (data.length < header.length) data.push('');
  header.forEach(function (h, i) {
    switch (h) {
      case 'Date':
        if (!existingRow) data[i] = Utilities.formatDate(new Date(), TIMEZONE, 'yyyy-MM-dd');
        break;
      case 'Time':
        if (!existingRow) data[i] = Utilities.formatDate(new Date(), TIMEZONE, 'H:mm');
        break;
      case 'Amount (AZN)': data[i] = formatAmountStr_(vals.amount, vals.isTopUp); break;
      case 'Transaction by': data[i] = vals.who; break;
      case 'Transaction category': data[i] = vals.category; break;
      case 'Note': data[i] = vals.note; break;
      case 'ID': data[i] = idValue; break;
    }
  });
  return data;
}

function doGet(e) {
  try {
    var ss = SpreadsheetApp.getActive();
    var info = ss ? {
      bound: true,
      spreadsheet: ss.getName(),
      tabs: ss.getSheets().map(function (s) { return s.getName(); }),
      writeTab: ss.getSheets()[0].getName()
    } : { bound: false };
    return jsonOut({ ok: true, ping: 'pong', info: info });
  } catch (err) {
    return jsonOut({ ok: false, error: String(err && err.message ? err.message : err) });
  }
}

function doPost(e) {
  try {
    if (!e || !e.postData || !e.postData.contents) {
      return jsonOut({ ok: false, error: 'empty body' });
    }
    var body = JSON.parse(e.postData.contents);
    var action = (body.action || 'create').toLowerCase();
    if (action === 'delete') return handleDelete_(body);
    if (action === 'update') return handleUpdate_(body);
    return handleCreate_(body);
  } catch (err) {
    return jsonOut({ ok: false, error: String(err && err.message ? err.message : err) });
  }
}

function handleCreate_(body) {
  var v = validatePayload_(body);
  if (v.error) return jsonOut({ ok: false, error: v.error });

  var lock = LockService.getDocumentLock();
  lock.waitLock(10000);
  try {
    var sh = getTargetSheet_();
    var id = Utilities.getUuid();
    var data = rowDataFor_(sh, v, id, null);
    sh.appendRow(data);
    return jsonOut({ ok: true, row: sh.getLastRow(), id: id });
  } finally {
    lock.releaseLock();
  }
}

function handleUpdate_(body) {
  if (!body.id) return jsonOut({ ok: false, error: 'missing id' });
  var v = validatePayload_(body);
  if (v.error) return jsonOut({ ok: false, error: v.error });

  var lock = LockService.getDocumentLock();
  lock.waitLock(10000);
  try {
    var sh = getTargetSheet_();
    var rowIdx = findRowById_(sh, body.id);
    if (rowIdx === -1) return jsonOut({ ok: false, error: 'id not found' });
    var header = getHeader_(sh);
    var existing = sh.getRange(rowIdx, 1, 1, header.length).getValues()[0];
    var data = rowDataFor_(sh, v, body.id, existing);
    sh.getRange(rowIdx, 1, 1, data.length).setValues([data]);
    return jsonOut({ ok: true, row: rowIdx, id: body.id });
  } finally {
    lock.releaseLock();
  }
}

function handleDelete_(body) {
  if (!body.id) return jsonOut({ ok: false, error: 'missing id' });
  var lock = LockService.getDocumentLock();
  lock.waitLock(10000);
  try {
    var sh = getTargetSheet_();
    var rowIdx = findRowById_(sh, body.id);
    if (rowIdx === -1) return jsonOut({ ok: false, error: 'id not found' });
    sh.deleteRow(rowIdx);
    return jsonOut({ ok: true, deleted: body.id });
  } finally {
    lock.releaseLock();
  }
}

// Run once to grant Sheets permission, ensure the ID column
// exists, and backfill missing IDs on existing rows.
function setupAndBackfill() {
  var sh = getTargetSheet_();
  var idCol = getIdColumn_(sh);
  Logger.log('Spreadsheet: ' + SpreadsheetApp.getActive().getName());
  Logger.log('Write tab: ' + sh.getName());
  Logger.log('ID column: ' + idCol);
  var last = sh.getLastRow();
  if (last < 2) { Logger.log('No data rows.'); return; }
  var range = sh.getRange(2, idCol, last - 1, 1);
  var values = range.getValues();
  var added = 0;
  for (var i = 0; i < values.length; i++) {
    if (!values[i][0]) { values[i][0] = Utilities.getUuid(); added++; }
  }
  range.setValues(values);
  Logger.log('Backfilled ' + added + ' missing IDs.');
}

function jsonOut(obj) {
  return ContentService
    .createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}
