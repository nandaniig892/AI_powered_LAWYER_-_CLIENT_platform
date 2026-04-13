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
        .list { display: grid; gap: 1rem; }
        .card { background: #fff; border-radius: 12px; padding: 1rem; box-shadow: 0 2px 8px rgba(0,0,0,0.05); border: 1px solid #E5E7EB; }
        .top { display: flex; justify-content: space-between; flex-wrap: wrap; gap: 0.75rem; }
        .title { color: #111827; font-weight: 700; }
        .meta { color: #6b7280; font-size: 0.9rem; margin-top: 0.3rem; }
        .desc { margin-top: 0.8rem; color: #374151; line-height: 1.5; }
        .controls { margin-top: 1rem; display: flex; gap: 0.5rem; flex-wrap: wrap; }
        .status { padding: 0.25rem 0.7rem; border-radius: 999px; font-size: 0.75rem; font-weight: 700; background: #fee2e2; color: #991b1b; text-transform: uppercase; }
        .btn { border: none; border-radius: 8px; padding: 0.5rem 0.8rem; font-weight: 600; color: #fff; cursor: pointer; }
        .btn-review { background: #2563eb; }
        .btn-resolve { background: #10b981; }
        .btn-reject { background: #ef4444; }
    </style>
</head>
<body>
    <div class="content-header">
        <h1 style="margin:0 0 0.4rem;color:#111827;">Complaints</h1>
        <p style="margin:0;color:#6b7280;">Track, review, and resolve user complaints.</p>
    </div>
    <div id="list" class="list"></div>

    <script>
        function updateStatus(id, status) {
            var note = '';
            if (status === 'resolved' || status === 'rejected') {
                note = prompt('Add resolution note (optional):', '') || '';
            }
            fetch('UpdateComplaintStatusServlet', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: 'complaintId=' + encodeURIComponent(id) + '&status=' + encodeURIComponent(status) + '&resolutionNote=' + encodeURIComponent(note)
            })
            .then(function(r) { return r.json(); })
            .then(function(data) {
                if (!data.success) {
                    alert(data.message || 'Failed to update');
                    return;
                }
                loadComplaints();
            })
            .catch(function() { alert('Failed to update'); });
        }

        function render(items) {
            var html = '';
            items.forEach(function(c) {
                html +=
                    '<div class="card">' +
                        '<div class="top">' +
                            '<div>' +
                                '<div class="title">Complaint #' + c.complaintId + ' | Case #' + c.caseId + '</div>' +
                                '<div class="meta">By: ' + c.complainant + ' | Against: ' + c.against + '</div>' +
                            '</div>' +
                            '<div class="status">' + c.status + '</div>' +
                        '</div>' +
                        '<div class="desc">' + c.description + '</div>' +
                        (c.resolutionNote ? '<div class="meta">Resolution: ' + c.resolutionNote + '</div>' : '') +
                        '<div class="controls">' +
                            '<button class="btn btn-review" onclick="updateStatus(' + c.complaintId + ',\'in_review\')">Mark In Review</button>' +
                            '<button class="btn btn-resolve" onclick="updateStatus(' + c.complaintId + ',\'resolved\')">Resolve</button>' +
                            '<button class="btn btn-reject" onclick="updateStatus(' + c.complaintId + ',\'rejected\')">Reject</button>' +
                        '</div>' +
                    '</div>';
            });
            document.getElementById('list').innerHTML = html || '<div class="card">No complaints found.</div>';
        }

        function loadComplaints() {
            fetch('GetAdminComplaintsServlet')
                .then(function(r) { return r.json(); })
                .then(render)
                .catch(function() {
                    document.getElementById('list').innerHTML = '<div class="card">Failed to load complaints.</div>';
                });
        }

        loadComplaints();
    </script>
</body>
</html>
