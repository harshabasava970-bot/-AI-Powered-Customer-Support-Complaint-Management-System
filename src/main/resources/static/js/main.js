// AI Analysis — live preview while typing the complaint description
(function () {
  const descEl = document.getElementById('description');
  const aiBox  = document.getElementById('ai-result-box');
  const loading = document.getElementById('ai-loading');

  if (!descEl || !aiBox) return;

  let debounceTimer;

  descEl.addEventListener('input', function () {
    clearTimeout(debounceTimer);
    const text = this.value.trim();
    if (text.length < 15) { aiBox.style.display = 'none'; return; }

    debounceTimer = setTimeout(function () {
      if (loading) loading.style.display = 'flex';

      fetch('/customer/complaints/analyze', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: 'text=' + encodeURIComponent(text)
      })
      .then(r => r.json())
      .then(data => {
        if (loading) loading.style.display = 'none';
        aiBox.style.display = 'block';

        const catEl  = document.getElementById('ai-category');
        const prioEl = document.getElementById('ai-priority');
        const confEl = document.getElementById('ai-confidence');
        const expEl  = document.getElementById('ai-explanation');

        if (catEl)  catEl.textContent  = data.predictedCategory || '—';
        if (prioEl) {
          prioEl.textContent = data.predictedPriority || '—';
          prioEl.className = 'ai-pred-value badge badge-' + (data.predictedPriority || '').toLowerCase();
        }
        if (confEl) confEl.textContent = data.confidenceScore != null
            ? 'Confidence: ' + Math.round(data.confidenceScore * 100) + '%'
            : '';
        if (expEl)  expEl.textContent  = data.explanation || '';

        // Auto-select category in dropdown if AI found one
        const catSelect = document.getElementById('categoryId');
        if (catSelect && data.predictedCategory) {
          Array.from(catSelect.options).forEach(opt => {
            if (opt.text.toLowerCase() === data.predictedCategory.toLowerCase()) {
              opt.selected = true;
            }
          });
        }

        // Auto-select priority radio/select
        const prioSelect = document.getElementById('priority');
        if (prioSelect && data.predictedPriority) {
          Array.from(prioSelect.options).forEach(opt => {
            if (opt.value === data.predictedPriority) opt.selected = true;
          });
        }
      })
      .catch(() => { if (loading) loading.style.display = 'none'; });
    }, 600);
  });
})();

// Sidebar mobile toggle
(function () {
  const toggle  = document.getElementById('sidebar-toggle');
  const sidebar = document.getElementById('sidebar');
  if (toggle && sidebar) {
    toggle.addEventListener('click', () => sidebar.classList.toggle('open'));
  }
})();

// Auto-dismiss alerts after 5 s
(function () {
  document.querySelectorAll('.alert[data-auto-dismiss]').forEach(el => {
    setTimeout(() => {
      el.style.transition = 'opacity .4s';
      el.style.opacity = '0';
      setTimeout(() => el.remove(), 400);
    }, 5000);
  });
})();

// Confirm dangerous actions
(function () {
  document.querySelectorAll('[data-confirm]').forEach(el => {
    el.addEventListener('click', function (e) {
      if (!confirm(this.dataset.confirm)) e.preventDefault();
    });
  });
})();
