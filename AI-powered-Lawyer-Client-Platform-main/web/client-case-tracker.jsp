<%@ page session="true" %>
<%
    if (session.getAttribute("userId") == null || !"client".equals(session.getAttribute("userType"))) {
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
        .head { background: #fff; border-radius: 12px; padding: 1.3rem 1.6rem; margin-bottom: 1.2rem; box-shadow: 0 2px 8px rgba(0,0,0,0.05); }
        .head h1 { margin: 0 0 0.4rem; color: #111827; }
        .head p { margin: 0; color: #6b7280; }
        .list { display: grid; gap: 1rem; }
        .card { background: #fff; border: 1px solid #E5E7EB; border-radius: 12px; padding: 1rem; box-shadow: 0 2px 8px rgba(0,0,0,0.05); }
        .top { display: flex; justify-content: space-between; gap: 1rem; flex-wrap: wrap; }
        .title { color: #111827; font-weight: 700; }
        .meta { color: #6b7280; font-size: 0.9rem; margin-top: 0.35rem; }
        .status { text-transform: uppercase; font-size: 0.75rem; font-weight: 700; border-radius: 999px; padding: 0.25rem 0.6rem; background: #e0e7ff; color: #3730a3; }
        .timeline { margin-top: 0.9rem; padding-top: 0.9rem; border-top: 1px solid #E5E7EB; }
        .timeline-item { font-size: 0.88rem; color: #374151; margin-bottom: 0.45rem; }
        .actions { margin-top: 0.9rem; display: flex; flex-wrap: wrap; gap: 0.5rem; }
        .btn { border: none; border-radius: 8px; padding: 0.55rem 0.8rem; font-weight: 600; cursor: pointer; color: #fff; }
        .btn-review { background: #10b981; }
        .btn-complaint { background: #ef4444; }
        .btn-chat { background: #2563eb; }
    </style>
</head>
<body>
    <div class="head">
        <h1>Case Timeline & Tracking</h1>
        <p>Track status changes, rate lawyers, and raise complaints.</p>
    </div>

    <div id="list" class="list"></div>

    <script>
        function fetchTimeline(caseId, targetId) {
            fetch('GetCaseTimelineServlet?caseId=' + caseId)
                .then(function(r) { return r.json(); })
                .then(function(items) {
                    var html = '';
                    (items || []).forEach(function(t) {
                        var note = t.note ? (' | ' + t.note) : '';
                        html += '<div class="timeline-item"><strong>' + t.status + '</strong>' + note + ' <span style="color:#6b7280;">(' + t.actor + ' | ' + t.createdAt + ')</span></div>';
                    });
                    document.getElementById(targetId).innerHTML = html || '<div class="timeline-item">No timeline entries.</div>';
                })
                .catch(function() {
                    document.getElementById(targetId).innerHTML = '<div class="timeline-item">Failed to load timeline.</div>';
                });
        }

        function submitReview(caseId) {
            var rating = prompt('Rate your lawyer (1-5):', '5');
            if (!rating) return;
            var reviewText = prompt('Write review (optional):', '') || '';
            fetch('SubmitReviewServlet', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: 'caseId=' + encodeURIComponent(caseId) + '&rating=' + encodeURIComponent(rating) + '&reviewText=' + encodeURIComponent(reviewText)
            })
            .then(function(r) { return r.json(); })
            .then(function(data) {
                if (!data.success) {
                    alert(data.message || 'Failed to submit review');
                    return;
                }
                loadCases();
            })
            .catch(function() { alert('Failed to submit review'); });
        }

        function submitComplaint(caseId) {
            var description = prompt('Describe your complaint:', '');
            if (!description) return;
            fetch('SubmitComplaintServlet', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: 'caseId=' + encodeURIComponent(caseId) + '&description=' + encodeURIComponent(description)
            })
            .then(function(r) { return r.json(); })
            .then(function(data) {
                if (!data.success) {
                    alert(data.message || 'Failed to submit complaint');
                    return;
                }
                alert('Complaint submitted successfully.');
            })
            .catch(function() { alert('Failed to submit complaint'); });
        }

        function openChat() {
            if (parent && parent.loadPage) {
                parent.loadPage('case-chat.jsp');
            } else {
                window.location.href = 'case-chat.jsp';
            }
        }

        function loadCases() {
            fetch('GetClientCaseTrackerServlet')
                .then(function(r) { return r.json(); })
                .then(function(items) {
                    var html = '';
                    items.forEach(function(c, idx) {
                        var timelineId = 'tl_' + idx;
                        var reviewBtn = c.canReview
                            ? '<button class="btn btn-review" onclick="submitReview(' + c.caseId + ')">Submit Review</button>'
                            : '';
                        var complaintBtn = c.hasLawyer
                            ? '<button class="btn btn-complaint" onclick="submitComplaint(' + c.caseId + ')">Raise Complaint</button>'
                            : '';
                        html +=
                            '<div class="card">' +
                                '<div class="top">' +
                                    '<div>' +
                                        '<div class="title">#' + c.caseId + ' - ' + c.title + '</div>' +
                                        '<div class="meta">' + c.type + ' | ' + c.city + ' | Lawyer: ' + c.lawyerName + '</div>' +
                                    '</div>' +
                                    '<div class="status">' + c.status + '</div>' +
                                '</div>' +
                                '<div class="timeline" id="' + timelineId + '">Loading timeline...</div>' +
                                '<div class="actions">' +
                                    '<button class="btn btn-chat" onclick="openChat()">Open Chat</button>' +
                                    reviewBtn +
                                    complaintBtn +
                                '</div>' +
                            '</div>';
                    });
                    document.getElementById('list').innerHTML = html || '<div class="card">No cases found.</div>';
                    items.forEach(function(c, idx) { fetchTimeline(c.caseId, 'tl_' + idx); });
                })
                .catch(function() {
                    document.getElementById('list').innerHTML = '<div class="card">Failed to load cases.</div>';
                });
        }

        loadCases();
    </script>
</body>
</html>
