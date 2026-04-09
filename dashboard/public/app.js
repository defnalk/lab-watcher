/* eslint-disable */
(() => {
  const conn = document.getElementById("conn");
  const tbody = document.getElementById("files-body");
  const cardTotal = document.getElementById("card-total");
  const cardRate = document.getElementById("card-rate");
  const cardLast = document.getElementById("card-last");

  const seen = new Set();
  let stats = { total: 0, pass: 0, fail: 0, error: 0, last: null };

  function refreshCards() {
    cardTotal.textContent = String(stats.total);
    const rate = stats.total > 0 ? Math.round((stats.pass / stats.total) * 100) : 0;
    cardRate.textContent = rate + "%";
    cardLast.textContent = stats.last ? new Date(stats.last).toLocaleString() : "–";
  }

  function statusBadge(status) {
    const cls = status === "PASS" ? "pass" : status === "FAIL" ? "fail" : "error";
    return `<span class="badge ${cls}">${status}</span>`;
  }

  function rowHtml(row) {
    return (
      `<tr data-id="${row.id}">` +
      `<td>${row.file_name}</td>` +
      `<td>${statusBadge(row.status)}</td>` +
      `<td>${row.row_count}</td>` +
      `<td>${row.column_count}</td>` +
      `<td>${row.error_count}</td>` +
      `<td>${row.warning_count}</td>` +
      `<td>${new Date(row.processed_at).toLocaleTimeString()}</td>` +
      `</tr>`
    );
  }

  function applyRow(row, flash) {
    if (seen.has(row.id)) return;
    seen.add(row.id);
    stats.total += 1;
    if (row.status === "PASS") stats.pass += 1;
    else if (row.status === "FAIL") stats.fail += 1;
    else stats.error += 1;
    stats.last = row.processed_at;

    if (tbody.querySelector("td.empty")) tbody.innerHTML = "";
    tbody.insertAdjacentHTML("afterbegin", rowHtml(row));
    if (flash) tbody.firstElementChild.classList.add("flash");
  }

  function applySnapshot(rows) {
    tbody.innerHTML = "";
    seen.clear();
    stats = { total: 0, pass: 0, fail: 0, error: 0, last: null };
    // Snapshot is newest-first; reverse so we apply oldest-first and the
    // newest ends up at the top.
    [...rows].reverse().forEach((r) => applyRow(r, false));
    refreshCards();
  }

  function connect() {
    const proto = location.protocol === "https:" ? "wss" : "ws";
    const ws = new WebSocket(`${proto}://${location.host}/ws`);
    ws.addEventListener("open", () => {
      conn.classList.remove("offline");
      conn.classList.add("online");
    });
    ws.addEventListener("close", () => {
      conn.classList.remove("online");
      conn.classList.add("offline");
      setTimeout(connect, 2000);
    });
    ws.addEventListener("message", (ev) => {
      const msg = JSON.parse(ev.data);
      if (msg.type === "snapshot") {
        applySnapshot(msg.data);
      } else if (msg.type === "new_file") {
        applyRow(msg.data, true);
        refreshCards();
      }
    });
  }

  connect();
})();
