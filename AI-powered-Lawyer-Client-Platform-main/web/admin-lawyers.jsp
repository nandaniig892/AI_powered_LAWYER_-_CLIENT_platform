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
        .content-header { background: #fff; padding: 1.5rem 2rem; border-radius: 12px; margin-bottom: 1.5rem; box-shadow: 0 2px 8px rgba(0,0,0,0.05); }
        .content-header h1 { margin: 0 0 0.4rem; color: #111827; }
        .content-header p { margin: 0; color: #6b7280; }
        .list { display: grid; gap: 1rem; }
        .card { background: #fff; border-radius: 12px; padding: 1rem; box-shadow: 0 2px 8px rgba(0,0,0,0.05); border: 1px solid #E5E7EB; }
        .row { display: flex; justify-content: space-between; gap: 1rem; flex-wrap: wrap; }
        .name { font-weight: 700; color: #111827; }
        .meta { color: #6b7280; font-size: 0.9rem; margin-top: 0.35rem; }
        .tags { display: flex; gap: 0.5rem; margin-top: 0.5rem; flex-wrap: wrap; }
        .tag { padding: 0.25rem 0.6rem; border-radius: 999px; font-size: 0.75rem; font-weight: 600; }
        .verified { background: #dcfce7; color: #166534; }
        .not-verified { background: #fee2e2; color: #991b1b; }
        .active { background: #dbeafe; color: #1d4ed8; }
        .suspended { background: #fef3c7; color: #92400e; }
        .actions { display: flex; gap: 0.5rem; flex-wrap: wrap; }
        .btn { border: none; border-radius: 8px; padding: 0.55rem 0.8rem; color: #fff; cursor: pointer; font-weight: 600; }
        .btn-verify { background: #10b981; }
        .btn-unverify { background: #6b7280; }
        .btn-suspend { background: #ef4444; }
        .btn-activate { background: #2563eb; }
    </style>
</head>
<body>
    <div class="content-header">
        <h1>Lawyer Management</h1>
        <p>Approve, suspend, reactivate, or remove verification.</p>
    </div>

    <div id="list" class="list"></div>

    <script>
        function actionBtn(label, cls, onclick) {
            return '<button class="btn ' + cls + '" onclick="' + onclick + '">' + label + '</button>';
        }

        function act(targetUserId, action) {
            fetch('AdminLawyerActionServlet', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: 'targetUserId=' + encodeURIComponent(targetUserId) + '&action=' + encodeURIComponent(action)
            })
            .then(function(r) { return r.json(); })
            .then(function(data) {
                if (!data.success) {
                    alert(data.message || 'Action failed');
                    return;
                }
                loadLawyers();
            })
            .catch(function() { alert('Action failed'); });
        }

        function renderLawyers(items) {
            var html = '';
            items.forEach(function(lawyer) {
                var verifyTag = lawyer.isVerified
                    ? '<span class="tag verified">Verified</span>'
                    : '<span class="tag not-verified">Not verified</span>';
                var activeTag = lawyer.isActive
                    ? '<span class="tag active">Active</span>'
                    : '<span class="tag suspended">Suspended</span>';

                var actions = '';
                if (!lawyer.isVerified) {
                    actions += actionBtn('Verify', 'btn-verify', 'act(' + lawyer.userId + ',\'verify\')');
                } else {
                    actions += actionBtn('Unverify', 'btn-unverify', 'act(' + lawyer.userId + ',\'unverify\')');
                }
                if (lawyer.isActive) {
                    actions += actionBtn('Suspend', 'btn-suspend', 'act(' + lawyer.userId + ',\'suspend\')');
                } else {
                    actions += actionBtn('Activate', 'btn-activate', 'act(' + lawyer.userId + ',\'activate\')');
                }

                html +=
                    '<div class="card">' +
                        '<div class="row">' +
                            '<div>' +
                                '<div class="name">Adv. ' + lawyer.firstName + ' ' + lawyer.lastName + '</div>' +
                                '<div class="meta">' + lawyer.email + ' | ' + lawyer.phone + '</div>' +
                                '<div class="meta">Bar: ' + lawyer.barNumber + ' | ' + lawyer.specialization + ' | ' + lawyer.city + '</div>' +
                                '<div class="tags">' + verifyTag + activeTag + '</div>' +
                            '</div>' +
                            '<div class="actions">' + actions + '</div>' +
                        '</div>' +
                    '</div>';
            });
            document.getElementById('list').innerHTML = html || '<div class="card">No lawyers found.</div>';
        }

        function loadLawyers() {
            fetch('GetAdminLawyersServlet')
                .then(function(r) { return r.json(); })
                .then(renderLawyers)
                .catch(function() {
                    document.getElementById('list').innerHTML = '<div class="card">Failed to load lawyers.</div>';
                });
        }

        loadLawyers();
    </script>
</body>
</html>
