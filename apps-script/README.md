# Apps Script web endpoint

Backs the Android app's "Add transaction" button. Appends a row to the
Google Sheet that powers the static site, in the exact format the site's
parser expects.

## Setup (one time)

1. Open the Google Sheet in a browser.
2. **Extensions → Apps Script** — opens the script editor bound to this sheet.
3. Replace the contents of `Code.gs` with the contents of this folder's
   [Code.gs](Code.gs).
4. If your sheet's tab is not named `Sheet1`, edit the `SHEET_NAME`
   constant at the top of `Code.gs` to match.
5. **Deploy → New deployment**
   - Type: **Web app**
   - Execute as: **Me**
   - Who has access: **Anyone**
6. Authorize the script when prompted (it needs access to the sheet).
7. Copy the deployment URL (`https://script.google.com/macros/s/.../exec`).
   This is `WEB_APP_URL` for the Android app.

## Updating later

Use **Manage deployments → pencil icon → Version: New version → Deploy**.
This keeps the same `/exec` URL.

Do **not** use "New deployment" each time — it issues a fresh URL and
the installed Android apps would silently 404 until rebuilt.

## Smoke tests

GET (browser):

```
https://script.google.com/macros/s/.../exec
```

Expect `{"ok":true,"ping":"pong"}`.

POST (Windows PowerShell):

```
curl.exe -L -X POST "<WEB_APP_URL>" ^
  -H "Content-Type: application/json" ^
  -d "{\"amount\":1.23,\"isTopUp\":false,\"transactionBy\":\"Sübhan\",\"category\":\"😀 Other\",\"note\":\"curl test\"}"
```

Expect `{"ok":true,"row":<n>}`. Open the sheet — the new row should
appear with `- ₼1.23`, today's date, current time in `H:mm`, and the
correct person/category/note.
