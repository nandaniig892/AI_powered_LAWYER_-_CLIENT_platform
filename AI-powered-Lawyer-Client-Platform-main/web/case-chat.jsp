<%@ page session="true" %>
<%
    String userType = (String) session.getAttribute("userType");
    if (session.getAttribute("userId") == null || (!"client".equals(userType) && !"lawyer".equals(userType))) {
        response.sendRedirect("login.html");
        return;
    }
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    <style>
        body { margin: 0; padding: 20px; background: #F8FAFC; font-family: 'Inter', sans-serif; }
        .layout { display: grid; grid-template-columns: 320px 1fr; gap: 1rem; min-height: 80vh; }
        .panel { background: #fff; border-radius: 12px; border: 1px solid #E5E7EB; box-shadow: 0 2px 8px rgba(0,0,0,0.05); }
        .panel h2 { margin: 0; padding: 1rem; border-bottom: 1px solid #E5E7EB; color: #111827; font-size: 1.1rem; }
        .cases { max-height: 78vh; overflow-y: auto; }
        .case-item { padding: 0.9rem; border-bottom: 1px solid #F3F4F6; cursor: pointer; }
        .case-item:hover { background: #F8FAFC; }
        .case-item.active { background: #eff6ff; border-left: 4px solid #C9A227; padding-left: calc(0.9rem - 4px); }
        .case-title { color: #111827; font-weight: 600; }
        .case-meta { color: #6b7280; font-size: 0.85rem; margin-top: 0.25rem; }
        .badge { background: #ef4444; color: #fff; border-radius: 999px; font-size: 0.7rem; padding: 0.12rem 0.45rem; margin-left: 0.4rem; }
        .chat-wrap { display: flex; flex-direction: column; min-height: 80vh; }
        .chat-head { padding: 1rem; border-bottom: 1px solid #E5E7EB; color: #111827; font-weight: 600; }
        .msgs { flex: 1; padding: 1rem; overflow-y: auto; max-height: 60vh; }
        .msg { max-width: 75%; margin-bottom: 0.7rem; padding: 0.65rem 0.75rem; border-radius: 10px; }
        .mine { margin-left: auto; background: #dbeafe; color: #1e3a8a; }
        .other { background: #f3f4f6; color: #111827; }
        .meta { font-size: 0.75rem; opacity: 0.8; margin-top: 0.3rem; }
        .composer {
            border-top: 1px solid #E5E7EB;
            padding: 0.75rem;
            display: flex;
            gap: 0.5rem;
            align-items: center;
            flex-wrap: nowrap;
        }
        .composer input[type="text"] {
            flex: 1 1 auto;
            min-width: 0;
            width: auto;
            box-sizing: border-box;
            border: 1px solid #D1D5DB;
            border-radius: 8px;
            padding: 0.6rem;
            font-size: 0.95rem;
        }
        .btn {
            border: none;
            border-radius: 8px;
            padding: 0.6rem 0.8rem;
            cursor: pointer;
            font-weight: 600;
            flex: 0 0 auto;
            white-space: nowrap;
        }
        .btn-send { background: #C9A227; color: #fff; }
        .btn-file { background: #E5E7EB; color: #111827; }
        .hint { padding: 1rem; color: #6b7280; }
        @media (max-width: 900px) { .layout { grid-template-columns: 1fr; } .msgs { max-height: 45vh; } }
        @media (max-width: 560px) {
            .composer { flex-wrap: wrap; }
            .composer input[type="text"] { flex-basis: 100%; }
            .btn { flex: 1 1 48%; }
        }
    </style>
</head>
<body>
    <div class="layout">
        <div class="panel">
            <h2>Case Chats</h2>
            <div id="cases" class="cases"></div>
        </div>

        <div class="panel chat-wrap">
            <div id="chatHead" class="chat-head">Select a case to start chat</div>
            <div id="messages" class="msgs"></div>
            <form id="composer" class="composer" onsubmit="sendMessage(event)" style="display:none;">
                <input id="msgInput" type="text" placeholder="Type a message..." />
                <input id="msgFile" type="file" style="display:none;" />
                <button type="button" class="btn btn-file" onclick="document.getElementById('msgFile').click()">Attach</button>
                <button class="btn btn-send" type="submit">Send</button>
            </form>
            <div id="emptyHint" class="hint">No case selected.</div>
        </div>
    </div>

    <script>
        var selectedCaseId = null;
        var pollTimer = null;

        function loadCases() {
            fetch('GetCaseChatListServlet')
                .then(function(r) { return r.json(); })
                .then(function(items) {
                    var html = '';
                    items.forEach(function(item) {
                        var badge = item.unreadCount > 0 ? '<span class="badge">' + item.unreadCount + '</span>' : '';
                        html +=
                            '<div class="case-item ' + (selectedCaseId === item.caseId ? 'active' : '') + '" onclick="selectCase(' + item.caseId + ',\'' + encodeURIComponent(item.caseTitle) + '\',\'' + encodeURIComponent(item.otherParty) + '\')">' +
                                '<div class="case-title">#' + item.caseId + ' - ' + item.caseTitle + badge + '</div>' +
                                '<div class="case-meta">' + item.caseStatus + ' | ' + item.otherParty + '</div>' +
                            '</div>';
                    });
                    document.getElementById('cases').innerHTML = html || '<div class="hint">No chat-eligible cases yet.</div>';
                })
                .catch(function() {
                    document.getElementById('cases').innerHTML = '<div class="hint">Failed to load cases.</div>';
                });
        }

        function selectCase(caseId, titleEncoded, otherEncoded) {
            selectedCaseId = caseId;
            var title = decodeURIComponent(titleEncoded);
            var other = decodeURIComponent(otherEncoded);
            document.getElementById('chatHead').textContent = 'Case #' + caseId + ': ' + title + ' | ' + other;
            document.getElementById('composer').style.display = 'flex';
            document.getElementById('emptyHint').style.display = 'none';
            loadCases();
            loadMessages();

            if (pollTimer) {
                clearInterval(pollTimer);
            }
            pollTimer = setInterval(loadMessages, 6000);
        }

        function loadMessages() {
            if (!selectedCaseId) {
                return;
            }
            fetch('GetCaseMessagesServlet?caseId=' + selectedCaseId)
                .then(function(r) { return r.json(); })
                .then(function(data) {
                    if (!data.success) {
                        return;
                    }
                    var html = '';
                    (data.items || []).forEach(function(msg) {
                        var fileHtml = msg.filePath ? ('<div><a href="' + msg.filePath + '" target="_blank">Attachment</a></div>') : '';
                        html +=
                            '<div class="msg ' + (msg.isMine ? 'mine' : 'other') + '">' +
                                '<div>' + (msg.message || '') + '</div>' +
                                fileHtml +
                                '<div class="meta">' + msg.senderName + ' | ' + msg.createdAt + '</div>' +
                            '</div>';
                    });
                    var box = document.getElementById('messages');
                    box.innerHTML = html || '<div class="hint">No messages yet.</div>';
                    box.scrollTop = box.scrollHeight;
                    loadCases();
                })
                .catch(function() {});
        }

        function sendMessage(event) {
            event.preventDefault();
            if (!selectedCaseId) {
                return;
            }
            var text = document.getElementById('msgInput').value.trim();
            var fileInput = document.getElementById('msgFile');
            if (!text && fileInput.files.length === 0) {
                return;
            }

            var formData = new FormData();
            formData.append('caseId', selectedCaseId);
            formData.append('message', text);
            if (fileInput.files.length > 0) {
                formData.append('messageFile', fileInput.files[0]);
            }

            fetch('SendCaseMessageServlet', {
                method: 'POST',
                body: formData
            })
            .then(function(r) { return r.json(); })
            .then(function(data) {
                if (!data.success) {
                    alert(data.message || 'Failed to send');
                    return;
                }
                document.getElementById('msgInput').value = '';
                document.getElementById('msgFile').value = '';
                loadMessages();
            })
            .catch(function() { alert('Failed to send message'); });
        }

        loadCases();
    </script>
</body>
</html>
