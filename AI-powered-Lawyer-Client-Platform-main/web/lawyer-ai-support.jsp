<%@ page session="true" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    String userType = (String) session.getAttribute("userType");
    if (session.getAttribute("userId") == null || !"lawyer".equals(userType)) {
        response.sendRedirect("login.html");
        return;
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>AI Support - Lawyer</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <style>
        *, *::before, *::after { box-sizing: border-box; }
        body { margin: 0; padding: 20px; background: #F8FAFC; font-family: 'Inter', sans-serif; color: #111827; }
        .content-header { background: #fff; border: 1px solid #E5E7EB; border-radius: 12px; padding: 1.2rem 1.4rem; margin-bottom: 1rem; }
        .content-header h1 { margin: 0 0 0.4rem; font-size: 1.65rem; }
        .content-header p { margin: 0; color: #6b7280; }
        .card { background: #fff; border: 1px solid #E5E7EB; border-radius: 12px; padding: 1rem; margin-bottom: 1rem; }
        .controls { display: grid; grid-template-columns: 2fr 1fr; gap: 0.75rem; align-items: end; }
        .field label { display: block; margin-bottom: 0.35rem; font-size: 0.9rem; font-weight: 600; color: #374151; }
        .field select, .field input, .chat-input {
            width: 100%; border: 1px solid #D1D5DB; border-radius: 8px; padding: 0.65rem 0.8rem; font-family: inherit; font-size: 0.95rem;
        }
        .field select:focus, .field input:focus, .chat-input:focus { outline: none; border-color: #C9A227; }
        .status { border: 1px solid #E5E7EB; border-radius: 8px; padding: 0.55rem 0.7rem; margin-bottom: 0.8rem; font-size: 0.9rem; }
        .status.ok { background: #ECFDF5; color: #065F46; border-color: #A7F3D0; }
        .status.warn { background: #FFFBEB; color: #92400E; border-color: #FCD34D; }
        .chat-box { border: 1px solid #E5E7EB; border-radius: 10px; background: #FAFBFC; min-height: 280px; max-height: 420px; overflow-y: auto; padding: 0.75rem; }
        .msg-row { display: flex; margin: 0.45rem 0; }
        .msg-row.user { justify-content: flex-end; }
        .msg-row.ai { justify-content: flex-start; }
        .msg {
            max-width: 82%; border-radius: 12px; padding: 0.7rem 0.8rem; white-space: pre-wrap; word-break: break-word; line-height: 1.4;
        }
        .msg.user { background: #0B1F3A; color: #fff; border-bottom-right-radius: 4px; }
        .msg.ai { background: #fff; color: #111827; border: 1px solid #E5E7EB; border-bottom-left-radius: 4px; }
        .quick-row { display: flex; gap: 0.5rem; flex-wrap: wrap; margin-top: 0.8rem; }
        .chip { border: 1px solid #E5E7EB; background: #fff; color: #111827; border-radius: 999px; padding: 0.4rem 0.7rem; font-size: 0.82rem; cursor: pointer; }
        .chip:hover { border-color: #C9A227; color: #A9861F; }
        .input-row { display: grid; grid-template-columns: 1fr auto; gap: 0.6rem; margin-top: 0.8rem; }
        .chat-input { min-height: 56px; resize: vertical; }
        .btn { background: #C9A227; color: #fff; border: none; border-radius: 8px; padding: 0.75rem 1rem; font-weight: 600; cursor: pointer; }
        .btn:hover { background: #A9861F; }
        .btn:disabled { opacity: 0.7; cursor: not-allowed; }
        .muted { color: #6B7280; font-size: 0.88rem; }
        .details-toggle { display: inline-flex; align-items: center; gap: 0.4rem; cursor: pointer; color: #0B1F3A; font-weight: 600; }
        .details-panel { display: none; margin-top: 0.8rem; border-top: 1px dashed #D1D5DB; padding-top: 0.8rem; }
        .list { margin: 0; padding-left: 1.1rem; }
        .list li { margin-bottom: 0.35rem; }
        @media (max-width: 900px) {
            .controls { grid-template-columns: 1fr; }
            .input-row { grid-template-columns: 1fr; }
        }
    </style>
</head>
<body>
    <div class="content-header">
        <h1><i class="fas fa-comments"></i> AI Drafting Support</h1>
        <p>Ask in plain language. You’ll get conversation-style drafting help from selected case details.</p>
    </div>

    <div class="card">
        <div id="modeStatus" class="status ok">Ready. Select an active case and start chatting.</div>
        <div class="controls">
            <div class="field">
                <label for="caseSelect">Active case</label>
                <select id="caseSelect"></select>
            </div>
            <details class="field">
                <summary class="muted">Use manual case ID (advanced)</summary>
                <input type="number" id="caseIdManual" placeholder="Example: 101" style="margin-top:0.45rem;">
            </details>
        </div>
    </div>

    <div class="card">
        <div id="chatBox" class="chat-box"></div>

        <div class="quick-row">
            <button class="chip" onclick="setPrompt('Summarize this case for first hearing preparation.')">Hearing summary</button>
            <button class="chip" onclick="setPrompt('List missing facts and evidence gaps in this case.')">Evidence gaps</button>
            <button class="chip" onclick="setPrompt('Give procedural next steps for this case.')">Procedural steps</button>
        </div>

        <div class="input-row">
            <textarea id="prompt" class="chat-input" placeholder="Type your request..."></textarea>
            <button id="sendBtn" class="btn" onclick="sendMessage()">Send</button>
        </div>
        <p class="muted" style="margin-top:0.55rem;">Tip: short instructions work best (example: “prepare checklist for tomorrow hearing”).</p>
    </div>

    <div class="card">
        <span class="details-toggle" onclick="toggleDetails()"><i class="fas fa-chevron-right" id="toggleIcon"></i> Show detailed analysis</span>
        <div id="detailsPanel" class="details-panel">
            <div id="analysisContent" class="muted">No analysis yet.</div>
        </div>
    </div>

    <script>
        function escapeHtml(text) {
            return String(text || '')
                .replace(/&/g, '&amp;')
                .replace(/</g, '&lt;')
                .replace(/>/g, '&gt;')
                .replace(/"/g, '&quot;')
                .replace(/'/g, '&#39;');
        }

        function setStatus(message, type) {
            var el = document.getElementById('modeStatus');
            el.className = 'status ' + (type === 'warn' ? 'warn' : 'ok');
            el.textContent = message;
        }

        function addMessage(role, text) {
            var box = document.getElementById('chatBox');
            var row = document.createElement('div');
            row.className = 'msg-row ' + (role === 'user' ? 'user' : 'ai');
            var bubble = document.createElement('div');
            bubble.className = 'msg ' + (role === 'user' ? 'user' : 'ai');
            bubble.textContent = text;
            row.appendChild(bubble);
            box.appendChild(row);
            box.scrollTop = box.scrollHeight;
        }

        function setPrompt(text) {
            document.getElementById('prompt').value = text;
            document.getElementById('prompt').focus();
        }

        function getSelectedCaseId() {
            var manual = document.getElementById('caseIdManual').value.trim();
            if (manual) return manual;
            return document.getElementById('caseSelect').value;
        }

        function buildFriendlyReply(analysis) {
            if (!analysis) return 'Sorry, I could not process that request.';
            var text = String(analysis.assistant_reply || '').trim();
            if (text) return text;
            return String(analysis.summary || 'I understood your request. Please add more details for a better response.');
        }

        function renderList(items) {
            if (!Array.isArray(items) || items.length === 0) return '<p class="muted">No items.</p>';
            return '<ul class="list">' + items.map(function (x) { return '<li>' + escapeHtml(x) + '</li>'; }).join('') + '</ul>';
        }

        function renderAnalysis(analysis) {
            var html = '';
            html += '<p><strong>Summary:</strong> ' + escapeHtml(analysis.summary || '') + '</p>';
            html += '<p><strong>Confidence:</strong> ' + escapeHtml(String(analysis.confidence || 0)) + '%</p>';
            html += '<p><strong>Applicable Rules</strong></p>' + renderList(analysis.applicable_rules || []);
            html += '<p><strong>Proof Required</strong></p>' + renderList(analysis.proof_required || []);
            if (analysis.insufficient_evidence) {
                html += '<p class="muted"><strong>Evidence Gap:</strong> ' + escapeHtml(analysis.insufficient_evidence_reason || '') + '</p>';
            }
            document.getElementById('analysisContent').innerHTML = html;
        }

        function toggleDetails() {
            var panel = document.getElementById('detailsPanel');
            var icon = document.getElementById('toggleIcon');
            var open = panel.style.display === 'block';
            panel.style.display = open ? 'none' : 'block';
            icon.className = open ? 'fas fa-chevron-right' : 'fas fa-chevron-down';
        }

        async function loadCases() {
            var select = document.getElementById('caseSelect');
            select.innerHTML = '<option value="">Loading active cases...</option>';
            try {
                var res = await fetch('GetActiveCasesServlet');
                var rows = await res.json();
                if (!Array.isArray(rows) || rows.length === 0) {
                    select.innerHTML = '<option value="">No active cases</option>';
                    addMessage('ai', 'No active case found right now. Once you accept a case, I can assist drafting.');
                    return;
                }
                var options = [];
                rows.forEach(function (r) {
                    options.push(
                        '<option value="' + r.caseId + '">#' + r.caseId + ' - ' +
                        escapeHtml(r.title || 'Untitled') + ' (' + escapeHtml(r.status || 'unknown') + ')</option>'
                    );
                });
                select.innerHTML = options.join('');
                addMessage('ai', 'Hi! I can help with hearing prep, issue framing, and evidence checklists for your selected case.');
            } catch (e) {
                select.innerHTML = '<option value="">Unable to load active cases</option>';
                addMessage('ai', 'Could not load active cases. Please refresh this page.');
            }
        }

        async function sendMessage() {
            var caseId = getSelectedCaseId();
            var promptEl = document.getElementById('prompt');
            var prompt = promptEl.value.trim();
            var btn = document.getElementById('sendBtn');

            if (!prompt) return;

            addMessage('user', prompt);
            promptEl.value = '';

            if (!caseId) {
                addMessage('ai', 'Please select an active case first.');
                return;
            }

            btn.disabled = true;
            btn.textContent = 'Sending...';
            setStatus('Thinking...', 'ok');

            try {
                var body = new URLSearchParams({ role: 'lawyer', caseId: String(caseId), prompt: prompt });
                var response = await fetch('AiSupportServlet', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' },
                    body: body.toString()
                });
                var data = await response.json();
                if (!response.ok || !data.ok) {
                    throw new Error(data.message || 'Request failed');
                }

                var analysis = data.analysis || {};
                renderAnalysis(analysis);
                addMessage('ai', buildFriendlyReply(analysis));

                if (data.mode === 'live') {
                    setStatus('Connected to live AI.', 'ok');
                } else if (data.mode === 'grounded-insufficient') {
                    setStatus('Need more case facts for stronger case-specific output.', 'warn');
                } else {
                    setStatus('Gemini is unavailable; using backup mode.', 'warn');
                }
            } catch (err) {
                setStatus('Message failed. Please try again.', 'warn');
                addMessage('ai', 'Sorry, I could not process that request right now.');
            } finally {
                btn.disabled = false;
                btn.textContent = 'Send';
            }
        }

        document.getElementById('prompt').addEventListener('keydown', function (e) {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                sendMessage();
            }
        });

        loadCases();
    </script>
</body>
</html>
