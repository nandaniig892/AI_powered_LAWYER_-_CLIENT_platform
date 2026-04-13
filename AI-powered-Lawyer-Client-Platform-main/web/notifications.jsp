<%@ page session="true" %>
<%
    if (session.getAttribute("userId") == null) {
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
        .head { display: flex; justify-content: space-between; align-items: center; background: #fff; border-radius: 12px; padding: 1.2rem 1.5rem; margin-bottom: 1rem; box-shadow: 0 2px 8px rgba(0,0,0,0.05); }
        .head h1 { margin: 0; color: #111827; }
        .btn { border: none; border-radius: 8px; padding: 0.55rem 0.9rem; font-weight: 600; cursor: pointer; background: #C9A227; color: #fff; }
        .list { display: grid; gap: 0.8rem; }
        .item { background: #fff; border-radius: 12px; padding: 1rem; border: 1px solid #E5E7EB; box-shadow: 0 2px 8px rgba(0,0,0,0.05); }
        .item.unread { border-left: 4px solid #C9A227; }
        .title { font-weight: 700; color: #111827; margin-bottom: 0.3rem; }
        .message { color: #374151; line-height: 1.5; }
        .meta { color: #6b7280; font-size: 0.85rem; margin-top: 0.5rem; }
    </style>
</head>
<body>
    <div class="head">
        <h1>Notifications</h1>
        <button class="btn" onclick="markAllRead()">Mark all as read</button>
    </div>
    <div id="list" class="list"></div>

    <script>
        function render(items) {
            var html = '';
            items.forEach(function(item) {
                html +=
                    '<div class="item ' + (item.isRead ? '' : 'unread') + '">' +
                        '<div class="title">' + item.title + '</div>' +
                        '<div class="message">' + item.message + '</div>' +
                        '<div class="meta">' + item.type + ' | ' + item.createdAt + '</div>' +
                    '</div>';
            });
            document.getElementById('list').innerHTML = html || '<div class="item">No notifications yet.</div>';
        }

        function loadNotifications() {
            fetch('GetNotificationsServlet')
                .then(function(r) { return r.json(); })
                .then(function(data) {
                    if (!data.success) {
                        document.getElementById('list').innerHTML = '<div class="item">Failed to load notifications.</div>';
                        return;
                    }
                    render(data.items || []);
                })
                .catch(function() {
                    document.getElementById('list').innerHTML = '<div class="item">Failed to load notifications.</div>';
                });
        }

        function markAllRead() {
            fetch('MarkNotificationsReadServlet', { method: 'POST' })
                .then(function() { loadNotifications(); })
                .catch(function() {});
        }

        loadNotifications();
    </script>
</body>
</html>
