<%@ page session="true" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <link rel="stylesheet" href="styles.css">
    <style>
        body { margin: 0; padding: 20px; background: #F8FAFC; }
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
        }
        .stat-icon {
            width: 50px;
            height: 50px;
            background: linear-gradient(135deg, #0B1F3A 0%, #0B1F3A 100%);
            color: white;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            margin: 0 auto 1rem;
            font-size: 1.5rem;
        }
        .stat-value { font-size: 2rem; font-weight: 700; color: #111827; margin-bottom: 0.5rem; }
        .stat-label { color: #6b7280; font-size: 0.9rem; }
    </style>
</head>
<body>
    <div class="content-header">
        <h1>Welcome Back, <%= session.getAttribute("firstName") %>!</h1>
        <p>Here's what's happening with your legal matters today.</p>
    </div>

    <div class="stats-grid">
        <div class="stat-card">
            <div class="stat-icon"><i class="fas fa-briefcase"></i></div>
            <div class="stat-value">0</div>
            <div class="stat-label">Active Cases</div>
        </div>
        <div class="stat-card">
            <div class="stat-icon"><i class="fas fa-users"></i></div>
            <div class="stat-value">0</div>
            <div class="stat-label">Lawyer Connections</div>
        </div>
        <div class="stat-card">
            <div class="stat-icon"><i class="fas fa-calendar-check"></i></div>
            <div class="stat-value">0</div>
            <div class="stat-label">Appointments</div>
        </div>
        <div class="stat-card">
            <div class="stat-icon"><i class="fas fa-file-alt"></i></div>
            <div class="stat-value">0</div>
            <div class="stat-label">Documents</div>
        </div>
    </div>

    <div class="card">
        <h3>Quick Actions</h3>
        <p>Get started by uploading your case or searching for the perfect lawyer to handle your legal matter.</p>
    </div>
</body>
</html>
