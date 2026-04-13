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
            border-left: 4px solid #C9A227;
            transition: all 0.3s;
        }
        
        .case-card:hover {
            box-shadow: 0 8px 20px rgba(0,0,0,0.1);
            transform: translateY(-2px);
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
        
        .urgency-badge {
            padding: 0.25rem 0.75rem;
            border-radius: 20px;
            font-size: 0.85rem;
            font-weight: 600;
        }
        
        .urgency-normal {
            background: #e0e7ff;
            color: #3730a3;
        }
        
        .urgency-urgent {
            background: #fef3c7;
            color: #92400e;
        }
        
        .urgency-very-urgent {
            background: #fee2e2;
            color: #991b1b;
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
            flex-shrink: 0;
        }
        
        .client-details {
            flex: 1;
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
        
        .case-details {
            margin-bottom: 1rem;
        }
        
        .detail-row {
            display: flex;
            align-items: center;
            gap: 0.75rem;
            padding: 0.5rem 0;
            color: #6b7280;
            font-size: 0.9rem;
        }
        
        .detail-row i {
            color: #C9A227;
            width: 20px;
        }
        
        .detail-row strong {
            color: #111827;
        }
        
        .case-description {
            background: #F8FAFC;
            padding: 1rem;
            border-radius: 8px;
            color: #6b7280;
            font-size: 0.9rem;
            line-height: 1.6;
            margin-bottom: 1rem;
        }
        
        .case-actions {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 0.75rem;
        }
        
        .btn {
            padding: 0.75rem;
            border-radius: 8px;
            font-weight: 600;
            cursor: pointer;
            border: none;
            font-size: 0.9rem;
            transition: all 0.3s;
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 0.5rem;
        }
        
        .btn-download {
            background: #F8FAFC;
            color: #111827;
        }
        
        .btn-download:hover {
            background: #E5E7EB;
        }
        
        .btn-download:disabled {
            background: #F8FAFC;
            color: #9ca3af;
            cursor: not-allowed;
        }
        
        .btn-accept {
            background: #10b981;
            color: white;
        }
        
        .btn-accept:hover {
            background: #059669;
        }
        
        .btn-reject {
            background: #ef4444;
            color: white;
        }
        
        .btn-reject:hover {
            background: #dc2626;
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
        
        @media (max-width: 768px) {
            .cases-grid { grid-template-columns: 1fr; }
            .case-actions { grid-template-columns: 1fr; }
        }
    </style>
</head>
<body>
    <div class="content-header">
        <h1>New Case Requests</h1>
        <p>Review and respond to client case submissions</p>
    </div>

    <div id="loadingState" style="display: none; text-align: center; padding: 3rem;">
        <i class="fas fa-spinner fa-spin" style="font-size: 3rem; color: #C9A227;"></i>
        <p style="margin-top: 1rem; color: #6b7280;">Loading cases...</p>
    </div>

    <div id="casesGrid" class="cases-grid"></div>

    <div id="emptyState" class="empty-state" style="display: none;">
        <i class="fas fa-inbox"></i>
        <h3>No New Cases</h3>
        <p>You don't have any new case requests at the moment.</p>
        <p style="margin-top: 1rem;">New cases will appear here when clients submit requests.</p>
    </div>

        <script>
        window.onload = function() {
            loadNewCases();
        };

        function loadNewCases() {
            console.log('Loading new cases...');
            document.getElementById('loadingState').style.display = 'block';
            document.getElementById('casesGrid').style.display = 'none';
            document.getElementById('emptyState').style.display = 'none';

            fetch('GetNewCasesServlet')
                .then(function(response) {
                    console.log('Response status:', response.status);
                    return response.json();
                })
                .then(function(data) {
                    console.log('Cases loaded:', data.length);
                    document.getElementById('loadingState').style.display = 'none';
                    
                    if (data.length === 0) {
                        document.getElementById('emptyState').style.display = 'block';
                        return;
                    }
                    
                    displayCases(data);
                    document.getElementById('casesGrid').style.display = 'grid';
                })
                .catch(function(error) {
                    console.error('Error loading cases:', error);
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
                card.id = 'case-' + c.caseId;
                
                var urgencyClass = 'urgency-normal';
                var urgencyText = c.urgency || 'Normal';
                if (urgencyText.toLowerCase().indexOf('very') >= 0) {
                    urgencyClass = 'urgency-very-urgent';
                } else if (urgencyText.toLowerCase().indexOf('urgent') >= 0) {
                    urgencyClass = 'urgency-urgent';
                }
                
                var initials = c.clientFirstName.charAt(0) + c.clientLastName.charAt(0);
                
                var downloadBtn = c.documentPath ? 
                    '<button class="btn btn-download" onclick="downloadDocument(\'' + c.documentPath + '\')">' +
                    '<i class="fas fa-download"></i> Download Document</button>' :
                    '<button class="btn btn-download" disabled>' +
                    '<i class="fas fa-file"></i> No Document</button>';
                
                card.innerHTML =
                    '<div class="case-header">' +
                        '<div>' +
                            '<div class="case-title">' + c.title + '</div>' +
                            '<div class="case-id">Case ID: #' + c.caseId + '</div>' +
                        '</div>' +
                        '<div class="urgency-badge ' + urgencyClass + '">' + urgencyText + '</div>' +
                    '</div>' +
                    
                    '<div class="client-info">' +
                        '<div class="client-avatar">' + initials + '</div>' +
                        '<div class="client-details">' +
                            '<div class="client-name">' + c.clientFirstName + ' ' + c.clientLastName + '</div>' +
                            '<div class="client-contact">' +
                                '<i class="fas fa-envelope"></i> ' + c.clientEmail + ' | ' +
                                '<i class="fas fa-phone"></i> ' + c.clientPhone +
                            '</div>' +
                        '</div>' +
                    '</div>' +
                    
                    '<div class="case-details">' +
                        '<div class="detail-row">' +
                            '<i class="fas fa-gavel"></i>' +
                            '<span><strong>Type:</strong> ' + c.type + '</span>' +
                        '</div>' +
                        '<div class="detail-row">' +
                            '<i class="fas fa-map-marker-alt"></i>' +
                            '<span><strong>Location:</strong> ' + c.city + '</span>' +
                        '</div>' +
                        '<div class="detail-row">' +
                            '<i class="fas fa-money-bill-wave"></i>' +
                            '<span><strong>Budget:</strong> ' + formatMoney(c.budget) + '</span>' +
                        '</div>' +
                        '<div class="detail-row">' +
                            '<i class="fas fa-calendar"></i>' +
                            '<span><strong>Submitted:</strong> ' + c.createdAt + '</span>' +
                        '</div>' +
                    '</div>' +
                    
                    '<div class="case-description">' +
                        '<strong>Description:</strong><br>' +
                        c.description +
                    '</div>' +
                    
                    '<div class="case-actions">' +
                        downloadBtn +
                        '<button class="btn btn-accept" id="accept-btn-' + c.caseId + '" onclick="acceptCase(' + c.caseId + ')">' +
                            '<i class="fas fa-check"></i> Accept Case' +
                        '</button>' +
                    '</div>';
                
                grid.appendChild(card);
            });
        }

        function formatMoney(value) {
            if (!value) return 'Not specified';
            return value.replace(/Rs\./gi, '₹').replace(/Rs/gi, '₹').replace(/₹\s+/g, '₹');
        }

        function downloadDocument(path) {
            if (!path) {
                alert('No document available');
                return;
            }
            window.open(path, '_blank');
        }

        function acceptCase(caseId) {
            if (!confirm('Are you sure you want to accept this case?')) {
                return;
            }
            
            var btn = document.getElementById('accept-btn-' + caseId);
            btn.disabled = true;
            btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Processing...';
            
            fetch('AcceptCaseServlet?caseId=' + caseId, {
                method: 'POST'
            })
            .then(function(response) { return response.json(); })
            .then(function(data) {
                if (data.success) {
                    btn.style.background = '#10b981';
                    btn.innerHTML = '<i class="fas fa-check-circle"></i> Accepted!';
                    
                    setTimeout(function() {
                        var card = document.getElementById('case-' + caseId);
                        card.style.transition = 'all 0.5s ease';
                        card.style.opacity = '0';
                        card.style.transform = 'scale(0.9)';
                        
                        setTimeout(function() {
                            card.remove();
                            
                            var grid = document.getElementById('casesGrid');
                            if (grid.children.length === 0) {
                                document.getElementById('emptyState').style.display = 'block';
                                grid.style.display = 'none';
                            }
                        }, 500);
                    }, 1500);
                    
                    showNotification('Case accepted successfully! Check Active Cases.', 'success');
                } else {
                    btn.disabled = false;
                    btn.innerHTML = '<i class="fas fa-check"></i> Accept Case';
                    showNotification('Error: ' + data.message, 'error');
                }
            })
            .catch(function(error) {
                console.error('Error:', error);
                btn.disabled = false;
                btn.innerHTML = '<i class="fas fa-check"></i> Accept Case';
                showNotification('Error accepting case', 'error');
            });
        }

        function showNotification(message, type) {
            var notification = document.createElement('div');
            notification.style.cssText = 'position: fixed; top: 20px; right: 20px; padding: 1rem 1.5rem; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.2); z-index: 9999; font-weight: 600;';
            notification.style.background = type === 'success' ? '#10b981' : '#ef4444';
            notification.style.color = 'white';
            notification.innerHTML = message;
            
            document.body.appendChild(notification);
            
            setTimeout(function() {
                notification.style.transition = 'opacity 0.3s ease';
                notification.style.opacity = '0';
                setTimeout(function() {
                    document.body.removeChild(notification);
                }, 300);
            }, 3000);
        }
    </script>

</body>
</html>
