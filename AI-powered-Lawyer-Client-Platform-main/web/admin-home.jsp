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
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <style>
        body { margin: 0; padding: 20px; background: #F8FAFC; font-family: 'Inter', sans-serif; }
        .content-header { background: #fff; padding: 1.5rem 2rem; border-radius: 12px; margin-bottom: 2rem; box-shadow: 0 2px 8px rgba(0,0,0,0.05); }
        .content-header h1 { margin: 0 0 0.5rem; color: #111827; }
        .content-header p { margin: 0; color: #6b7280; }
        .stats-grid { display: grid; grid-template-columns: repeat(auto-fit,minmax(220px,1fr)); gap: 1.25rem; }
        .stat-card { background: #fff; border-radius: 12px; padding: 1.25rem; box-shadow: 0 2px 8px rgba(0,0,0,0.05); }
        .stat-title { color: #6b7280; font-size: 0.9rem; margin-bottom: 0.5rem; }
        .stat-value { color: #111827; font-size: 2rem; font-weight: 700; }
        .tips { margin-top: 1.5rem; background: #fff; border-radius: 12px; padding: 1.25rem; box-shadow: 0 2px 8px rgba(0,0,0,0.05); }
        .tips h3 { margin-top: 0; color: #111827; }
        .tips p { color: #6b7280; margin: 0.5rem 0; }
    </style>
</head>
<body>
    <div class="content-header">
        <h1>Admin Overview</h1>
        <p>Moderation, complaints, and platform control at one place.</p>
    </div>

    <div class="stats-grid">
        <div class="stat-card">
            <div class="stat-title">Pending Lawyer Verifications</div>
            <div class="stat-value" id="pendingVerifications">0</div>
        </div>
        <div class="stat-card">
            <div class="stat-title">Open Complaints</div>
            <div class="stat-value" id="openComplaints">0</div>
        </div>
        <div class="stat-card">
            <div class="stat-title">Active Users</div>
            <div class="stat-value" id="activeUsers">0</div>
        </div>
        <div class="stat-card">
            <div class="stat-title">Active Cases</div>
            <div class="stat-value" id="activeCases">0</div>
        </div>
    </div>

    <div class="tips">
        <h3>Recommended actions</h3>
        <p>- Verify pending lawyers daily to keep case matching healthy.</p>
        <p>- Resolve open complaints quickly and keep resolution notes clear.</p>
        <p>- Review audit logs before and after major moderation actions.</p>
    </div>

    <script>
        function loadStats() {
            fetch('GetAdminStatsServlet')
                .then(function(response) { return response.json(); })
                .then(function(data) {
                    document.getElementById('pendingVerifications').textContent = data.pendingVerifications || 0;
                    document.getElementById('openComplaints').textContent = data.openComplaints || 0;
                    document.getElementById('activeUsers').textContent = data.activeUsers || 0;
                    document.getElementById('activeCases').textContent = data.activeCases || 0;
                })
                .catch(function() {});
        }
        loadStats();
    </script>
</body>
</html>
