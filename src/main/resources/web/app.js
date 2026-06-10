/* FakeStore — shared front-end helpers (given, minimal). The cart lives in
   sessionStorage; the server only hears about it when you place an order. */

const CART_KEY = "fakestore.cart";

function getCart() {
  try { return JSON.parse(sessionStorage.getItem(CART_KEY)) || []; }
  catch { return []; }
}
function saveCart(cart) { sessionStorage.setItem(CART_KEY, JSON.stringify(cart)); updateBadge(); }
function clearCart() { sessionStorage.removeItem(CART_KEY); updateBadge(); }
function cartCount() { return getCart().reduce((n, l) => n + l.qty, 0); }

function addToCart(p, qty) {
  qty = Math.max(1, qty | 0);
  const cart = getCart();
  const found = cart.find(l => l.id === p.id);
  if (found) found.qty += qty;
  else cart.push({ id: p.id, title: p.title, price: p.price, qty });
  saveCart(cart);
}

function money(n) {
  return "$" + Number(n).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}
function esc(s) {
  return String(s).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;")
                  .replace(/"/g, "&quot;").replace(/'/g, "&#39;");
}

function updateBadge() {
  const el = document.getElementById("cart-count");
  if (el) el.textContent = cartCount();
}

/* ----- product thumbnail (no runtime image fetch) -------------------------
   The store renders a coloured CSS tile with the product's initials — so it
   needs NO network for images at all. If an instructor has run ImageDownloader,
   products get a same-origin "/images/<id>.jpg" path; only then do we use a real
   <img>. We never fetch a remote (off-site) URL from the browser. */
function thumbColor(id) { return `hsl(${(id * 47) % 360} 45% 42%)`; }
function initials(title) {
  const w = String(title).split(/\s+/).filter(Boolean);
  return ((w[0]?.[0] || "") + (w[1]?.[0] || "")).toUpperCase() || "?";
}
function thumb(p, cls) {
  const local = typeof p.image === "string" && p.image.startsWith("/");  // same-origin only
  if (local) return `<img class="${cls}" src="${esc(p.image)}" alt="" loading="lazy">`;
  return `<div class="${cls} thumb" style="background:${thumbColor(p.id)}">${esc(initials(p.title))}</div>`;
}

/* the shared top bar, dropped into <div id="bar"></div> on each page */
function renderBar() {
  const bar = document.getElementById("bar");
  if (!bar) return;
  bar.outerHTML = `
    <header class="bar">
      <a class="logo" href="index.html">Fake<span style="color:#f90">Store</span></a>
      <span class="grow"></span>
      <a href="index.html">Catalogue</a>
      <a href="register.html">Create account</a>
      <a href="checkout.html">Cart (<b id="cart-count">0</b>)</a>
    </header>`;
  updateBadge();
}
document.addEventListener("DOMContentLoaded", renderBar);
