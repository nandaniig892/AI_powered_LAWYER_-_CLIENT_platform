<%@ page session="true" %>
<%
    String firstName = (String) session.getAttribute("firstName");
    String lastName = (String) session.getAttribute("lastName");
    String email = (String) session.getAttribute("email");
    String userType = (String) session.getAttribute("userType");

    if (firstName == null || !"admin".equals(userType)) {
        response.sendRedirect("login.html");
        return;
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Admin Dashboard - LegalConnect</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <link rel="stylesheet" href="styles.css">
</head>
<body>
    <div class="dashboard-container">
        <aside class="sidebar">
            <div class="sidebar-header">
                <div class="logo">
                    <i class="fas fa-balance-scale"></i>
                    <span>LegalConnect</span>
                </div>

                <div class="user-info">
                    <div class="user-avatar">
                        <%= firstName.charAt(0) %><%= lastName.charAt(0) %>
                    </div>
                    <div class="user-name">Admin: <%= firstName %> <%= lastName %></div>
                    <div class="user-email"><%= email %></div>
                </div>
            </div>

            <nav class="sidebar-nav">
                <a class="nav-item active" onclick="loadPage('admin-home.jsp')">
                    <i class="fas fa-chart-pie"></i>
                    <span>Overview</span>
                </a>
                <a class="nav-item" onclick="loadPage('admin-lawyers.jsp')">
                    <i class="fas fa-user-shield"></i>
                    <span>Lawyer Management</span>
                </a>
                <a class="nav-item" onclick="loadPage('admin-complaints.jsp')">
                    <i class="fas fa-flag"></i>
                    <span>Complaints</span>
                </a>
                <a class="nav-item" onclick="loadPage('admin-logs.jsp')">
                    <i class="fas fa-clipboard-list"></i>
                    <span>Audit Logs</span>
                </a>
                <a class="nav-item logout" onclick="showLogoutModal()">
                    <i class="fas fa-sign-out-alt"></i>
                    <span>Logout</span>
                </a>
            </nav>
        </aside>

        <main class="main-content">
            <iframe id="contentFrame" class="content-frame" src="admin-home.jsp"></iframe>
        </main>
    </div>

    <div id="logoutModal" style="display: none; position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.5); z-index: 9999; justify-content: center; align-items: center;">
        <div style="background: white; border-radius: 12px; padding: 2rem; max-width: 400px; width: 90%; text-align: center; box-shadow: 0 10px 40px rgba(0,0,0,0.3);">
            <h2 style="color: #111827; margin-bottom: 0.5rem; font-size: 1.5rem;">Logout Confirmation</h2>
            <p style="color: #6b7280; margin-bottom: 2rem;">Are you sure you want to logout?</p>
            <div style="display: flex; gap: 1rem;">
                <button onclick="closeLogoutModal()" style="flex: 1; padding: 0.75rem; background: #F8FAFC; color: #111827; border: none; border-radius: 8px; font-weight: 600; cursor: pointer;">
                    Cancel
                </button>
                <button onclick="confirmLogout()" style="flex: 1; padding: 0.75rem; background: #ef4444; color: white; border: none; border-radius: 8px; font-weight: 600; cursor: pointer;">
                    Logout
                </button>
            </div>
        </div>
    </div>

    <script>
        function loadPage(page) {
            document.getElementById('contentFrame').src = page;
            document.querySelectorAll('.nav-item').forEach(function(item) {
                item.classList.remove('active');
            });
            if (event && event.currentTarget) {
                event.currentTarget.classList.add('active');
            }
        }

        function showLogoutModal() {
            document.getElementById('logoutModal').style.display = 'flex';
        }

        function closeLogoutModal() {
            document.getElementById('logoutModal').style.display = 'none';
        }

        function confirmLogout() {
            window.location.href = 'LogoutServlet';
        }
    </script>
</body>
</html>
