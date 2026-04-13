<%@ page session="true" %>
<%
    if (session.getAttribute("userId") == null || !"admin".equals(session.getAttribute("userType"))) {
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
        .content-header { background: #fff; padding: 1.5rem 2rem; border-radius: 12px; margin-bottom: 1.5rem; box-shadow: 0 2px 8px rgba(0,0,0,0.05); }
        .table { width: 100%; border-collapse: collapse; background: #fff; border-radius: 12px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.05); }
        .table th, .table td { padding: 0.85rem; border-bottom: 1px solid #E5E7EB; text-align: left; vertical-align: top; }
        .table th { background: #0B1F3A; color: #fff; font-weight: 600; }
        .table tr:last-child td { border-bottom: none; }
        .muted { color: #6b7280; font-size: 0.9rem; }
    </style>
</head>
<body>
    <div class="content-header">
        <h1 style="margin:0 0 0.4rem;color:#111827;">Audit Logs</h1>
        <p style="margin:0;color:#6b7280;">Review admin actions and moderation history.</p>
    </div>

    <table class="table">
        <thead>
            <tr>
                <th>Time</th>
                <th>Admin</th>
                <th>Action</th>
                <th>Target</th>
                <th>Details</th>
            </tr>
        </thead>
        <tbody id="rows">
            <tr><td colspan="5" class="muted">Loading...</td></tr>
        </tbody>
    </table>

    <script>
        function loadLogs() {
            fetch('GetAdminLogsServlet')
                .then(function(r) { return r.json(); })
                .then(function(items) {
                    var html = '';
                    items.forEach(function(log) {
                        html +=
                            '<tr>' +
                                '<td class="muted">' + log.createdAt + '</td>' +
                                '<td>' + (log.admin || '-') + '</td>' +
                                '<td>' + (log.action || '-') + '</td>' +
                                '<td>' + (log.target || '-') + '</td>' +
                                '<td>' + (log.details || '-') + '</td>' +
                            '</tr>';
                    });
                    document.getElementById('rows').innerHTML = html || '<tr><td colspan="5" class="muted">No logs found.</td></tr>';
                })
                .catch(function() {
                    document.getElementById('rows').innerHTML = '<tr><td colspan="5" class="muted">Failed to load logs.</td></tr>';
                });
        }
        loadLogs();
    </script>
</body>
</html>
