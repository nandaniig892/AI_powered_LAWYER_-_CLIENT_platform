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
        .stats-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 1.5rem;
            margin-bottom: 2rem;
        }
        .stat-card {
            background: white;
            padding: 1.5rem;
            border-radius: 12px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.05);
            text-align: center;
            position: relative;
            overflow: hidden;
            transition: transform 0.3s;
        }
        .stat-card:hover {
            transform: translateY(-5px);
            box-shadow: 0 8px 20px rgba(0,0,0,0.1);
        }
        .stat-card::before {
            content: '';
            position: absolute;
            top: 0;
            left: 0;
            right: 0;
            height: 4px;
            background: linear-gradient(135deg, #0B1F3A 0%, #C9A227 100%);
        }
        .stat-icon {
            width: 50px;
            height: 50px;
            background: linear-gradient(135deg, #0B1F3A 0%, #C9A227 100%);
            color: white;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            margin: 0 auto 1rem;
            font-size: 1.5rem;
        }
        .stat-value { 
            font-size: 2rem; 
            font-weight: 700; 
            color: #111827; 
            margin-bottom: 0.5rem;
            min-height: 48px;
            display: flex;
            align-items: center;
            justify-content: center;
        }
        .stat-label { color: #6b7280; font-size: 0.9rem; }
        .stat-loading {
            display: inline-block;
            width: 30px;
            height: 30px;
            border: 3px solid #f3f4f6;
            border-top: 3px solid #C9A227;
            border-radius: 50%;
            animation: spin 1s linear infinite;
        }
        @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
        }
        .card {
            background: white;
            padding: 2rem;
            border-radius: 12px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.05);
            margin-bottom: 1.5rem;
        }
        .card h3 { color: #111827; margin-bottom: 1rem; }
        .card p { color: #6b7280; line-height: 1.6; }
        .quick-actions {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 1rem;
            margin-top: 1.5rem;
        }
        .action-btn {
            padding: 1rem;
            background: #F8FAFC;
            border: 2px solid #E5E7EB;
            border-radius: 8px;
            text-align: center;
            cursor: pointer;
            transition: all 0.3s;
            text-decoration: none;
            color: #111827;
            font-weight: 600;
        }
        .action-btn:hover {
            background: #C9A227;
            color: white;
            border-color: #C9A227;
            transform: translateY(-2px);
        }
        .action-btn i {
            display: block;
            font-size: 2rem;
            margin-bottom: 0.5rem;
        }
    </style>
</head>
<body>
    <div class="content-header">
        <h1>Welcome Back, Adv. <%= session.getAttribute("firstName") %>!</h1>
        <p>Here's an overview of your legal practice today.</p>
    </div>

    <div class="stats-grid">
        <div class="stat-card">
            <div class="stat-icon"><i class="fas fa-folder-open"></i></div>
            <div class="stat-value" id="newCases">
                <div class="stat-loading"></div>
            </div>
            <div class="stat-label">New Case Requests</div>
        </div>
        <div class="stat-card">
            <div class="stat-icon"><i class="fas fa-briefcase"></i></div>
            <div class="stat-value" id="activeCases">
                <div class="stat-loading"></div>
            </div>
            <div class="stat-label">Active Cases</div>
        </div>
        <div class="stat-card">
            <div class="stat-icon"><i class="fas fa-users"></i></div>
            <div class="stat-value" id="totalClients">
                <div class="stat-loading"></div>
            </div>
            <div class="stat-label">Total Clients</div>
        </div>
        <div class="stat-card">
            <div class="stat-icon"><i class="fas fa-star"></i></div>
            <div class="stat-value" id="avgRating">
                <div class="stat-loading"></div>
            </div>
            <div class="stat-label">Average Rating</div>
        </div>
    </div>

    <div class="card">
        <h3>Quick Actions</h3>
        <div class="quick-actions">
            <a href="#" onclick="parent.document.querySelectorAll('.nav-item')[1].click(); return false;" class="action-btn">
                <i class="fas fa-folder-plus"></i>
                View New Cases
            </a>
            <a href="#" onclick="parent.document.querySelectorAll('.nav-item')[2].click(); return false;" class="action-btn">
                <i class="fas fa-briefcase"></i>
                Manage Active Cases
            </a>
            <a href="#" onclick="parent.document.querySelectorAll('.nav-item')[5].click(); return false;" class="action-btn">
                <i class="fas fa-user-edit"></i>
                Update Profile
            </a>
        </div>
    </div>

    <div class="card">
        <h3>Professional Tips</h3>
        <p><strong>Complete your profile:</strong> Add your bio, education, and specializations to increase visibility.</p>
        <p style="margin-top: 1rem;"><strong>Respond quickly:</strong> Fast response times to new cases increase your acceptance rate.</p>
        <p style="margin-top: 1rem;"><strong>Keep cases updated:</strong> Regular updates help maintain client trust and satisfaction.</p>
    </div>

    <script>
        window.onload = function() {
            loadStats();
        };

        function loadStats() {
            fetch('GetLawyerStatsServlet')
                .then(function(response) {
                    return response.json();
                })
                .then(function(data) {
                    console.log('Stats loaded:', data);
                    
                    document.getElementById('newCases').innerHTML = data.newCases;
                    document.getElementById('activeCases').innerHTML = data.activeCases;
                    document.getElementById('totalClients').innerHTML = data.totalClients;
                    document.getElementById('avgRating').innerHTML = data.avgRating.toFixed(1);
                    
                    // Add animation
                    animateNumber('newCases', 0, data.newCases, 1000);
                    animateNumber('activeCases', 0, data.activeCases, 1000);
                    animateNumber('totalClients', 0, data.totalClients, 1000);
                })
                .catch(function(error) {
                    console.error('Error loading stats:', error);
                    document.getElementById('newCases').innerHTML = '0';
                    document.getElementById('activeCases').innerHTML = '0';
                    document.getElementById('totalClients').innerHTML = '0';
                    document.getElementById('avgRating').innerHTML = '0.0';
                });
        }

        function animateNumber(elementId, start, end, duration) {
            var element = document.getElementById(elementId);
            var range = end - start;
            var increment = range / (duration / 16);
            var current = start;
            
            var timer = setInterval(function() {
                current += increment;
                if ((increment > 0 && current >= end) || (increment < 0 && current <= end)) {
                    current = end;
                    clearInterval(timer);
                }
                element.innerHTML = Math.round(current);
            }, 16);
        }
    </script>
</body>
</html>
