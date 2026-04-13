<%@ page session="true" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <link rel="stylesheet" href="styles.css">
    <style>
        body { margin: 0; padding: 20px; background: #F8FAFC; font-family: 'Inter', sans-serif; }
        .content-header {
            background: white;
            padding: 1.5rem 2rem;
            border-radius: 12px;
            margin-bottom: 2rem;
            box-shadow: 0 2px 8px rgba(0,0,0,0.05);
        }
        .content-header h1 { color: #111827; font-size: 1.75rem; margin-bottom: 0.5rem; }
        .content-header p { color: #6b7280; }
        
        .cases-grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
            gap: 1.5rem;
        }
        
        .case-card {
            background: white;
            border-radius: 12px;
            padding: 1.5rem;
            box-shadow: 0 2px 8px rgba(0,0,0,0.05);
            border-left: 4px solid #10b981;
        }
        
        .case-header {
            display: flex;
            justify-content: space-between;
            align-items: start;
            margin-bottom: 1rem;
            padding-bottom: 1rem;
            border-bottom: 2px solid #F8FAFC;
        }
        
        .case-title {
            font-size: 1.2rem;
            font-weight: 700;
            color: #111827;
            margin-bottom: 0.5rem;
        }
        
        .case-id {
            font-size: 0.85rem;
            color: #6b7280;
        }
        
        .status-badge {
            padding: 0.25rem 0.75rem;
            border-radius: 20px;
            font-size: 0.85rem;
            font-weight: 600;
            background: #dcfce7;
            color: #166534;
        }
        
        .client-info {
            display: flex;
            align-items: center;
            gap: 1rem;
            margin-bottom: 1rem;
            padding: 1rem;
            background: #F8FAFC;
            border-radius: 8px;
        }
        
        .client-avatar {
            width: 50px;
            height: 50px;
            border-radius: 50%;
            background: linear-gradient(135deg, #0B1F3A 0%, #0B1F3A 100%);
            color: white;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 1.2rem;
            font-weight: 700;
        }
        
        .client-name {
            font-weight: 600;
            color: #111827;
            margin-bottom: 0.25rem;
        }
        
        .client-contact {
            font-size: 0.85rem;
            color: #6b7280;
        }
        
        .detail-row {
            display: flex;
            gap: 0.75rem;
            padding: 0.5rem 0;
            color: #6b7280;
            font-size: 0.9rem;
        }
        
        .detail-row i {
            color: #10b981;
            width: 20px;
        }
        .status-control {
            margin-top: 1rem;
            padding-top: 1rem;
            border-top: 1px solid #E5E7EB;
        }
        .status-control label {
            display: block;
            font-size: 0.85rem;
            color: #6b7280;
            margin-bottom: 0.4rem;
            font-weight: 600;
        }
        .status-select, .status-note {
            width: 100%;
            border: 1px solid #D1D5DB;
            border-radius: 8px;
            padding: 0.55rem 0.65rem;
            font-family: 'Inter', sans-serif;
            margin-bottom: 0.5rem;
        }
        .status-note {
            min-height: 66px;
            resize: vertical;
        }
        .status-actions {
            display: flex;
            gap: 0.5rem;
            flex-wrap: wrap;
        }
        .btn-status, .btn-timeline {
            border: none;
            border-radius: 8px;
            padding: 0.55rem 0.75rem;
            color: white;
            font-weight: 600;
            cursor: pointer;
        }
        .btn-status { background: #2563eb; }
        .btn-timeline { background: #6b7280; }
        .timeline-list {
            margin-top: 0.75rem;
            background: #F8FAFC;
            border: 1px solid #E5E7EB;
            border-radius: 8px;
            padding: 0.65rem;
            display: none;
        }
        .timeline-item {
            font-size: 0.85rem;
            color: #374151;
            margin-bottom: 0.45rem;
        }
        
        .empty-state {
            text-align: center;
            padding: 4rem 2rem;
            background: white;
            border-radius: 12px;
        }
        .empty-state i { font-size: 4rem; color: #E5E7EB; margin-bottom: 1rem; }
        .empty-state h3 { color: #111827; margin-bottom: 0.5rem; }
        .empty-state p { color: #6b7280; }
    </style>
</head>
<body>
    <div class="content-header">
        <h1>Active Cases</h1>
        <p>Manage your ongoing client cases</p>
    </div>

    <div id="loadingState" style="display: none; text-align: center; padding: 3rem;">
        <i class="fas fa-spinner fa-spin" style="font-size: 3rem; color: #10b981;"></i>
        <p style="margin-top: 1rem; color: #6b7280;">Loading active cases...</p>
    </div>

    <div id="casesGrid" class="cases-grid"></div>

    <div id="emptyState" class="empty-state" style="display: none;">
        <i class="fas fa-briefcase"></i>
        <h3>No Active Cases</h3>
        <p>You don't have any active cases at the moment.</p>
        <p style="margin-top: 1rem;">Accepted cases will appear here.</p>
    </div>

    <script>
        window.onload = function() {
            loadActiveCases();
        };

        function loadActiveCases() {
            document.getElementById('loadingState').style.display = 'block';
            
            fetch('GetActiveCasesServlet')
                .then(function(response) { return response.json(); })
                .then(function(data) {
                    document.getElementById('loadingState').style.display = 'none';
                    
                    if (data.length === 0) {
                        document.getElementById('emptyState').style.display = 'block';
                        return;
                    }
                    
                    displayCases(data);
                    document.getElementById('casesGrid').style.display = 'grid';
                })
                .catch(function(error) {
                    console.error('Error:', error);
                    document.getElementById('loadingState').style.display = 'none';
                    document.getElementById('emptyState').style.display = 'block';
                });
        }

        function displayCases(cases) {
            var grid = document.getElementById('casesGrid');
            grid.innerHTML = '';

            cases.forEach(function(c) {
                var card = document.createElement('div');
                card.className = 'case-card';
                
                var initials = c.clientFirstName.charAt(0) + c.clientLastName.charAt(0);
                
                card.innerHTML =
                    '<div class="case-header">' +
                        '<div>' +
                            '<div class="case-title">' + c.title + '</div>' +
                            '<div class="case-id">Case ID: #' + c.caseId + '</div>' +
                        '</div>' +
                        '<div class="status-badge">' + formatStatus(c.status) + '</div>' +
                    '</div>' +
                    '<div class="client-info">' +
                        '<div class="client-avatar">' + initials + '</div>' +
                        '<div>' +
                            '<div class="client-name">' + c.clientFirstName + ' ' + c.clientLastName + '</div>' +
                            '<div class="client-contact">' + c.clientEmail + ' | ' + c.clientPhone + '</div>' +
                        '</div>' +
                    '</div>' +
                    '<div class="detail-row"><i class="fas fa-gavel"></i><span>' + c.type + '</span></div>' +
                    '<div class="detail-row"><i class="fas fa-map-marker-alt"></i><span>' + c.city + '</span></div>' +
                    '<div class="detail-row"><i class="fas fa-money-bill-wave"></i><span>' + formatMoney(c.budget) + '</span></div>' +
                    '<div class="status-control">' +
                        '<label>Update Case Status</label>' +
                        '<select class="status-select" id="status-' + c.caseId + '">' +
                            '<option value="active"' + (c.status === 'active' ? ' selected' : '') + '>Active</option>' +
                            '<option value="in_progress"' + (c.status === 'in_progress' ? ' selected' : '') + '>In Progress</option>' +
                            '<option value="resolved"' + (c.status === 'resolved' ? ' selected' : '') + '>Resolved</option>' +
                            '<option value="closed"' + (c.status === 'closed' ? ' selected' : '') + '>Closed</option>' +
                        '</select>' +
                        '<textarea class="status-note" id="note-' + c.caseId + '" placeholder="Add an optional timeline note"></textarea>' +
                        '<div class="status-actions">' +
                            '<button class="btn-status" onclick="updateStatus(' + c.caseId + ')">Save Status</button>' +
                            '<button class="btn-timeline" onclick="toggleTimeline(' + c.caseId + ')">View Timeline</button>' +
                        '</div>' +
                        '<div class="timeline-list" id="timeline-' + c.caseId + '"></div>' +
                    '</div>';
                
                grid.appendChild(card);
            });
        }

        function formatMoney(value) {
            if (!value) return 'Not specified';
            return value.replace(/Rs\./gi, '₹').replace(/Rs/gi, '₹').replace(/₹\s+/g, '₹');
        }

        function formatStatus(status) {
            if (!status) return 'Unknown';
            return status.replace('_', ' ').replace(/\b\w/g, function(ch) { return ch.toUpperCase(); });
        }

        function updateStatus(caseId) {
            var status = document.getElementById('status-' + caseId).value;
            var note = document.getElementById('note-' + caseId).value;

            fetch('UpdateCaseStatusServlet', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: 'caseId=' + encodeURIComponent(caseId) + '&status=' + encodeURIComponent(status) + '&note=' + encodeURIComponent(note)
            })
            .then(function(response) { return response.json(); })
            .then(function(data) {
                if (!data.success) {
                    alert(data.message || 'Failed to update status');
                    return;
                }
                alert('Case status updated');
                loadActiveCases();
            })
            .catch(function() { alert('Failed to update status'); });
        }

        function toggleTimeline(caseId) {
            var timeline = document.getElementById('timeline-' + caseId);
            if (timeline.style.display === 'block') {
                timeline.style.display = 'none';
                return;
            }
            timeline.style.display = 'block';
            timeline.textContent = 'Loading timeline...';

            fetch('GetCaseTimelineServlet?caseId=' + caseId)
                .then(function(response) { return response.json(); })
                .then(function(items) {
                    var html = '';
                    (items || []).forEach(function(item) {
                        var note = item.note ? (' | ' + item.note) : '';
                        html += '<div class="timeline-item"><strong>' + item.status + '</strong>' + note + ' <span style="color:#6b7280;">(' + item.actor + ' | ' + item.createdAt + ')</span></div>';
                    });
                    timeline.innerHTML = html || '<div class="timeline-item">No timeline entries yet.</div>';
                })
                .catch(function() {
                    timeline.innerHTML = '<div class="timeline-item">Failed to load timeline.</div>';
                });
        }
    </script>
</body>
</html>
