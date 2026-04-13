<%@ page session="true" %>
<%
    String firstName = (String) session.getAttribute("firstName");
    String lastName = (String) session.getAttribute("lastName");
    String email = (String) session.getAttribute("email");
    String userType = (String) session.getAttribute("userType");
    
    if (firstName == null || !"client".equals(userType)) {
        response.sendRedirect("login.html");
        return;
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Client Dashboard - LegalConnect</title>
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
                    <div class="user-name">Hello, <%= firstName %> <%= lastName %></div>
                    <div class="user-email"><%= email %></div>
                </div>
            </div>

            <nav class="sidebar-nav">
                <a class="nav-item active" onclick="loadPage('client-home.jsp')">
                    <i class="fas fa-home"></i>
                    <span>Dashboard Home</span>
                </a>
                <a class="nav-item" onclick="loadPage('client-search-lawyers.jsp')">
                    <i class="fas fa-search"></i>
                    <span>Search Lawyers</span>
                </a>
                <a class="nav-item" onclick="loadPage('client-upload-case.jsp')">
                    <i class="fas fa-upload"></i>
                    <span>Upload Case</span>
                </a>
                <a class="nav-item" onclick="loadPage('client-case-tracker.jsp')">
                    <i class="fas fa-list-check"></i>
                    <span>Case Tracker</span>
                </a>
                <a class="nav-item" onclick="loadPage('case-chat.jsp')">
                    <i class="fas fa-comments"></i>
                    <span>Case Chat</span>
                </a>
                <a class="nav-item" onclick="loadPage('notifications.jsp')">
                    <i class="fas fa-bell"></i>
                    <span class="nav-label">Notifications <span id="clientNotifBadge" class="nav-badge" style="display:none;">0</span></span>
                </a>
                <a class="nav-item" onclick="loadPage('client-profile.jsp')">
                    <i class="fas fa-user-edit"></i>
                    <span>Update Profile</span>
                </a>
                <a class="nav-item" onclick="loadPage('client-ai-support.jsp')">
                    <i class="fas fa-robot"></i>
                    <span>AI Support</span>
                </a>
                <a class="nav-item logout" onclick="showLogoutModal()">
                    <i class="fas fa-sign-out-alt"></i>
                    <span>Logout</span>
                </a>
            </nav>
        </aside>

        <main class="main-content">
            <iframe id="contentFrame" class="content-frame" src="client-home.jsp"></iframe>
        </main>
    </div>

    <!-- Logout Confirmation Modal -->
    <div id="logoutModal" style="display: none; position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.5); z-index: 9999; justify-content: center; align-items: center;">
        <div style="background: white; border-radius: 12px; padding: 2rem; max-width: 400px; width: 90%; text-align: center; box-shadow: 0 10px 40px rgba(0,0,0,0.3);">
            <div style="font-size: 3rem; color: #f59e0b; margin-bottom: 1rem;">
                <i class="fas fa-exclamation-triangle"></i>
            </div>
            <h2 style="color: #111827; margin-bottom: 0.5rem; font-size: 1.5rem;">Logout Confirmation</h2>
            <p style="color: #6b7280; margin-bottom: 2rem;">Are you sure you want to logout?</p>
            <div style="display: flex; gap: 1rem;">
                <button onclick="closeLogoutModal()" style="flex: 1; padding: 0.75rem; background: #F8FAFC; color: #111827; border: none; border-radius: 8px; font-weight: 600; cursor: pointer; font-size: 0.95rem; transition: all 0.3s;">
                    Cancel
                </button>
                <button onclick="confirmLogout()" style="flex: 1; padding: 0.75rem; background: #ef4444; color: white; border: none; border-radius: 8px; font-weight: 600; cursor: pointer; font-size: 0.95rem; transition: all 0.3s;">
                    <i class="fas fa-sign-out-alt"></i> Logout
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

        function refreshNotificationBadge() {
            fetch('GetNotificationsServlet')
                .then(function(response) { return response.json(); })
                .then(function(data) {
                    var badge = document.getElementById('clientNotifBadge');
                    if (!data.success || !data.unreadCount) {
                        badge.style.display = 'none';
                        return;
                    }
                    badge.textContent = data.unreadCount > 99 ? '99+' : data.unreadCount;
                    badge.style.display = 'inline-flex';
                })
                .catch(function() {});
        }

        refreshNotificationBadge();
        setInterval(refreshNotificationBadge, 12000);
    </script>
</body>
</html>
