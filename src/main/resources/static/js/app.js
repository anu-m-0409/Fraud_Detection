(() => {
  'use strict';

  const $ = (id) => document.getElementById(id);
  const form = $('tx-form');
  const goBtn = $('go');
  const inr = new Intl.NumberFormat('en-IN', { maximumFractionDigits: 0 });
  const STAMP = { APPROVE: 'Approved', REVIEW: 'Held for review', DECLINE: 'Declined' };
  const COUNTRY = { IN: 'India', AE: 'UAE', SG: 'Singapore', GB: 'UK', US: 'USA', NG: 'Nigeria', RU: 'Russia', KP: 'North Korea' };

  let currentEvaluation = null;
  let thresholds = { review: 0.40, decline: 0.75 };

  const scenarios = [
    { title: 'Normal Weekly Grocery', note: 'Standard domestic cardholder activity',
      data: { userId: 'priya_sharma', amount: 1850, merchantCategory: 'grocery', country: 'IN',
              distanceFromHomeKm: 3, deviceId: 'priya-iphone', when: 'live' } },
    { title: 'Dubai Restaurant on Holiday', note: 'Foreign trip, but recognized personal phone',
      data: { userId: 'priya_sharma', amount: 3200, merchantCategory: 'restaurant', country: 'AE',
              distanceFromHomeKm: 2000, deviceId: 'priya-iphone', when: '13:00' } },
    { title: 'Spike: ₹46,000 Electronics', note: '20x usual average spend on same phone',
      data: { userId: 'priya_sharma', amount: 46000, merchantCategory: 'electronics', country: 'IN',
              distanceFromHomeKm: 6, deviceId: 'priya-iphone', when: 'live' } },
    { title: 'Nigeria 3 AM Gift Card Hijack', note: 'High velocity, foreign IP, unseen hardware',
      data: { userId: 'rohit_verma', amount: 28000, merchantCategory: 'gift_cards', country: 'NG',
              distanceFromHomeKm: 5200, deviceId: 'unknown-device-77', when: '03:10' } },
    { title: 'Automated Bot Burst Attack (7x)', note: 'Rapid velocity card testing sequence',
      burst: 7,
      data: { userId: 'arjun_mehta', amount: 4999, merchantCategory: 'electronics', country: 'IN',
              distanceFromHomeKm: 4, deviceId: 'arjun-pixel', when: 'live' } },
    { title: 'Sanctioned Territory (OFAC Block)', note: 'North Korea (KP) immediate compliance veto',
      data: { userId: 'rohit_verma', amount: 1200, merchantCategory: 'grocery', country: 'KP',
              distanceFromHomeKm: 6000, deviceId: 'rohit-redmi', when: 'live' } }
  ];

  const list = $('scenarios');
  scenarios.forEach((s) => {
    const li = document.createElement('li');
    const b = document.createElement('button');
    b.type = 'button';
    b.append(s.title);
    const small = document.createElement('small');
    small.textContent = s.note;
    b.append(small);
    b.addEventListener('click', () => {
      fill(s.data);
      submit(s.burst || 1);
    });
    li.append(b);
    list.append(li);
  });

  function fill(d) {
    Object.entries(d).forEach(([k, v]) => { if (form.elements[k]) form.elements[k].value = v; });
  }

  function readForm() {
    const f = form.elements;
    const payload = {
      userId: f.userId.value.trim(),
      amount: f.amount.value === '' ? null : Number(f.amount.value),
      merchantCategory: f.merchantCategory.value,
      country: f.country.value,
      deviceId: f.deviceId.value.trim(),
      distanceFromHomeKm: f.distanceFromHomeKm.value === '' ? 0 : Number(f.distanceFromHomeKm.value),
    };
    if (f.when.value !== 'live') {
      const day = new Date().toLocaleDateString('en-CA', { timeZone: 'Asia/Kolkata' });
      payload.timestamp = `${day}T${f.when.value}:00+05:30`;
    }
    return payload;
  }

  function problems(p) {
    const bad = [];
    if (!p.userId) bad.push('userId');
    if (!(p.amount > 0)) bad.push('amount');
    if (!p.deviceId) bad.push('deviceId');
    return bad;
  }

  async function submit(times = 1) {
    const payload = readForm();
    const bad = problems(payload);
    ['userId', 'amount', 'deviceId'].forEach((n) => form.elements[n].classList.toggle('bad', bad.includes(n)));
    const err = $('form-error');
    if (bad.length) {
      err.textContent = 'Please fill customer ID, amount (>0), and device identifier.';
      err.hidden = false;
      return;
    }
    err.hidden = true;

    goBtn.disabled = true;
    try {
      let last;
      for (let i = 0; i < times; i++) {
        last = await post(payload);
      }
      currentEvaluation = last;
      show(last);
      refreshSide();
    } catch (e) {
      err.textContent = e.message;
      err.hidden = false;
    } finally {
      goBtn.disabled = false;
    }
  }

  async function post(payload) {
    let res;
    try {
      res = await fetch('/api/evaluate', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });
    } catch (_) {
      throw new Error('Connection failed: Ensure Gatehouse backend is listening on port 8080.');
    }
    const body = await res.json().catch(() => ({}));
    if (!res.ok) {
      const detail = body.fields ? Object.values(body.fields).join('. ') : '';
      throw new Error((body.error || 'Evaluation failed') + (detail ? ': ' + detail : ''));
    }
    return body;
  }

  form.addEventListener('submit', (e) => { e.preventDefault(); submit(1); });

  function pct(x) { return Math.round(x * 100) + '%'; }

  function show(r) {
    $('empty').hidden = true;
    $('result').hidden = false;

    const stamp = $('r-stamp');
    stamp.className = 'stamp ' + r.decision;
    $('r-stamp-word').textContent = STAMP[r.decision];
    void stamp.offsetWidth;
    stamp.classList.add('land');

    $('r-meta').textContent = `${r.transactionId} • Scored in ${r.latencyMs.toFixed(1)} ms`;
    $('r-amount').textContent = '₹' + inr.format(r.amount);
    $('r-sub').textContent =
      `${r.userId} • ${r.merchantCategory.replace('_', ' ')} • ${COUNTRY[r.country] || r.country}`;

    $('z-ok').style.width = (thresholds.review * 100) + '%';
    $('z-review').style.width = ((thresholds.decline - thresholds.review) * 100) + '%';
    $('z-decline').style.width = ((1 - thresholds.decline) * 100) + '%';
    const tr = $('tick-review'), td = $('tick-decline');
    tr.style.left = (thresholds.review * 100) + '%';
    tr.textContent = 'Review: ' + pct(thresholds.review);
    td.style.left = (thresholds.decline * 100) + '%';
    td.textContent = 'Decline: ' + pct(thresholds.decline);

    const pin = $('pin');
    pin.style.left = '0%';
    void pin.offsetWidth;
    pin.style.left = (r.riskScore * 100) + '%';
    $('pin-label').textContent = pct(r.riskScore);

    $('r-score').textContent =
      `Final Aggregate Risk: ${pct(r.riskScore)} (Model Baseline: ${pct(r.modelScore)}` +
      (r.ruleHits.length ? `, adjusted by ${r.ruleHits.length} compliance rules).` : ').');

    const reasons = $('r-reasons');
    reasons.replaceChildren(...r.reasons.map((t) => {
      const el = document.createElement('li');
      el.textContent = t;
      return el;
    }));

    const rules = $('r-rules');
    if (!r.ruleHits.length) {
      const n = document.createElement('li');
      n.textContent = 'None — All deterministic policy boundaries clear';
      n.className = 'nothing';
      rules.replaceChildren(n);
    } else {
      rules.replaceChildren(...r.ruleHits.map((h) => {
        const item = document.createElement('li');
        if (h.hardBlock) item.className = 'hard';
        const s = document.createElement('strong');
        s.textContent = h.hardBlock ? h.code + ' [MANDATORY DECLINE]' : h.code;
        const p = document.createElement('span');
        p.textContent = h.description;
        item.append(s, p);
        return item;
      }));
    }

    renderShap(r.shapContributions);
    $('r-features').replaceChildren(...featureRows(r.features));
  }

  function renderShap(shap) {
    const container = $('shap-bars');
    container.innerHTML = '';
    if (!shap) return;
    const names = {
      amount_vs_usual: 'Spend Baseline Dev',
      merchant_risk: 'Merchant Category Risk',
      unseen_device: 'Hardware Anonymity',
      foreign_country: 'Cross-Border Origin',
      txns_last_hour: 'Velocity Burst',
      night_time: 'Nocturnal Activity',
      log_amount: 'Nominal Value',
      log_distance_km: 'Distance Offset'
    };
    Object.entries(shap).sort((a,b) => b[1] - a[1]).slice(0, 5).forEach(([k, v]) => {
      const row = document.createElement('div');
      row.className = 'shap-row';
      const label = document.createElement('span');
      label.textContent = names[k] || k;
      const track = document.createElement('div');
      track.className = 'shap-track';
      const fill = document.createElement('div');
      fill.className = 'shap-fill' + (v > 0.25 ? ' high' : '');
      fill.style.width = Math.min(100, Math.round(v * 160)) + '%';
      track.appendChild(fill);
      const val = document.createElement('span');
      val.style.fontFamily = 'var(--mono)';
      val.textContent = (v * 100).toFixed(0) + '%';
      row.append(label, track, val);
      container.appendChild(row);
    });
  }

  function featureRows(f) {
    const rows = [
      ['Gross Amount', '₹' + inr.format(Math.expm1(f.log_amount)), false],
      ['Compared with Normal Average', f.amount_vs_usual.toFixed(1) + 'x', f.amount_vs_usual >= 4],
      ['Authorizations Past 60 Mins', String(f.txns_last_hour), f.txns_last_hour >= 3],
      ['Night-Time Window (01:00-05:00)', f.night_time ? 'Yes' : 'No', !!f.night_time],
      ['International Territory', f.foreign_country ? 'Yes' : 'No', !!f.foreign_country],
      ['Unseen Device Fingerprint', f.unseen_device ? 'Yes' : 'No', !!f.unseen_device],
      ['Merchant Risk Index', Math.round(f.merchant_risk * 100) + ' / 100', f.merchant_risk >= 0.6],
      ['Distance from Baseline', inr.format(Math.expm1(f.log_distance_km)) + ' km', Math.expm1(f.log_distance_km) >= 500],
    ];
    return rows.map(([label, value, hot]) => {
      const row = document.createElement('li');
      if (hot) row.className = 'hot';
      const a = document.createElement('span'); a.textContent = label;
      const dots = document.createElement('i');
      const b = document.createElement('b'); b.textContent = value;
      row.append(a, dots, b);
      return row;
    });
  }

  // GenAI Modal Handlers
  $('btn-open-audit').addEventListener('click', async () => {
    if (!currentEvaluation) return;
    const modal = $('audit-modal');
    modal.hidden = false;
    $('m-narrative').textContent = 'Analyzing evaluation telemetry & synthesizing regulatory audit trail...';
    try {
      const res = await fetch('/api/audit/generate', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(currentEvaluation)
      });
      const data = await res.json();
      $('m-hash').textContent = data.auditTrailHash;
      $('m-txid').textContent = data.transactionId;
      $('m-badge').className = 'pill ' + data.riskLevel;
      $('m-badge').textContent = data.riskLevel;
      $('m-narrative').textContent = data.executiveSummary;

      const dList = $('m-drivers');
      dList.innerHTML = '';
      (data.primaryDrivers || []).forEach(d => {
        const li = document.createElement('li');
        li.textContent = d;
        dList.appendChild(li);
      });

      const vList = $('m-violations');
      vList.innerHTML = '';
      if (!data.complianceViolations || !data.complianceViolations.length) {
        const li = document.createElement('li');
        li.textContent = 'None: Clean record on OFAC/FATF and velocity gates.';
        vList.appendChild(li);
      } else {
        data.complianceViolations.forEach(v => {
          const li = document.createElement('li');
          li.textContent = v;
          vList.appendChild(li);
        });
      }
      $('m-action').textContent = data.recommendedAction;
    } catch (e) {
      $('m-narrative').textContent = 'Audit generation error: ' + e.message;
    }
  });

  $('btn-close-audit').addEventListener('click', () => { $('audit-modal').hidden = true; });
  $('audit-modal').addEventListener('click', (e) => { if (e.target === $('audit-modal')) $('audit-modal').hidden = true; });

  async function refreshSide() {
    try {
      const [stats, recent] = await Promise.all([
        fetch('/api/stats').then((r) => r.json()),
        fetch('/api/recent?limit=15').then((r) => r.json()),
      ]);
      thresholds = { review: stats.reviewThreshold, decline: stats.declineThreshold };
      $('t-total').textContent = stats.total;
      $('t-approved').textContent = stats.approved;
      $('t-review').textContent = stats.review;
      $('t-declined').textContent = stats.declined;
      $('t-latency').textContent = Number(stats.avgLatencyMs).toFixed(1) + ' ms';
      drawLedger(recent);
    } catch (_) {}
  }

  function drawLedger(rows) {
    const body = $('ledger-body');
    if (!rows.length) return;
    body.replaceChildren(...rows.map((r) => {
      const tr = document.createElement('tr');
      const cells = [
        new Date(r.evaluatedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' }),
        r.userId,
        '₹' + inr.format(r.amount),
        r.merchantCategory.replace('_', ' '),
        r.country,
      ];
      cells.forEach((c, i) => {
        const td = document.createElement('td');
        td.textContent = c;
        if (i === 2) td.className = 'num';
        tr.append(td);
      });
      const dec = document.createElement('td');
      const pill = document.createElement('span');
      pill.className = 'pill ' + r.decision;
      pill.textContent = r.decision;
      dec.append(pill);
      const risk = document.createElement('td');
      risk.className = 'num';
      risk.textContent = pct(r.riskScore);
      tr.append(dec, risk);
      return tr;
    }));
  }

  fill(scenarios[0].data);
  refreshSide();
})();
