# Shared Budget · Sübhan & İsmayıl

A minimal glassmorphism site that reads a public Google Sheet and shows the total balance, per-person balances, category insights, and recent transactions.

No backend, no build step — just three static files.

## Files
- `index.html` — markup
- `styles.css` — glass theme
- `app.js` — fetch, parse, compute, render

## Run locally
Either double-click `index.html`, or:
```
python -m http.server 8000
```
then open http://localhost:8000.

## Deploy to GitHub Pages (recommended)

GitHub Pages can't proxy the Google Sheet directly (CORS), so a GitHub Action
fetches the sheet on a schedule and commits `sheet.csv` to the repo. The site
then loads `./sheet.csv` from the same origin — no CORS, no third-party proxy.

1. Create a new GitHub repo (e.g. `ismayilxsubhan-co`).
2. From this folder:
   ```
   git init -b main
   git add .
   git commit -m "initial commit"
   git remote add origin https://github.com/<you>/<repo>.git
   git push -u origin main
   ```
3. On GitHub: **Settings → Pages → Build and deployment**
   - Source: **Deploy from a branch**
   - Branch: **main** / **/ (root)** → Save
4. **Settings → Actions → General → Workflow permissions** → enable
   **Read and write permissions** (so the workflow can commit `sheet.csv`).
5. **Actions → Refresh sheet.csv → Run workflow** to do the first refresh now,
   then it'll run automatically every 10 minutes.
6. Visit `https://<you>.github.io/<repo>/`.

### Deploy to Vercel (alternative)
```
npx vercel
```
Uses `vercel.json` to rewrite `/sheet.csv` to the published Google Sheet URL
server-side. No GitHub Action needed; the site is always live.

## How balances are computed
- **Top up** → adds to that person's balance.
- **Personal expense** (Sübhan / İsmayıl) → subtracts from that person.
- **Shared expense** → split 50/50.
- **Total** = Sübhan + İsmayıl balance.

## Updating the sheet
The sheet must be publicly viewable. The site refreshes on page load and on the refresh button — no caching.

