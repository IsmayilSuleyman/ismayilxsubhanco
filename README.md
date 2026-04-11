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

## Deploy to Vercel
```
npm i -g vercel
vercel        # follow prompts
vercel --prod # subsequent deploys
```
No env vars, no build command. Output dir = project root.

You can also drag the folder onto https://vercel.com/new.

## How balances are computed
- **Top up** → adds to that person's balance.
- **Personal expense** (Sübhan / İsmayıl) → subtracts from that person.
- **Shared expense** → split 50/50.
- **Total** = Sübhan + İsmayıl balance.

## Updating the sheet
The sheet must be publicly viewable. The site refreshes on page load and on the refresh button — no caching.
"# ismayilxsubhanco." 
