<%@ page session="true" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.sql.*" %>
<%
    Integer userId = (Integer) session.getAttribute("userId");
    if (userId == null) {
        response.sendRedirect("login.html");
        return;
    }
    
    // Fetch current user and lawyer data
    String firstName = "";
    String lastName = "";
    String email = "";
    String phone = "";
    String city = "";
    String barNumber = "";
    String stateLicensed = "";
    String yearsExperience = "";
    String primarySpecialization = "";
    String cityPractice = "";
    String hourlyRate = "";
    String bio = "";
    
    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    
    try {
        conn = DBConnectionUtil.getConnection();
        
        String sql = "SELECT u.first_name, u.last_name, u.email, u.phone_number, u.city, " +
                     "l.bar_number, l.state_licensed, l.years_experience, l.primary_specialization, " +
                     "l.city_practice, l.hourly_rate, l.bio " +
                     "FROM users u INNER JOIN lawyers l ON u.user_id = l.user_id " +
                     "WHERE u.user_id = ?";
        pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, userId);
        rs = pstmt.executeQuery();
        
        if (rs.next()) {
            firstName = rs.getString("first_name");
            lastName = rs.getString("last_name");
            email = rs.getString("email");
            phone = rs.getString("phone_number");
            city = rs.getString("city");
            barNumber = rs.getString("bar_number");
            stateLicensed = rs.getString("state_licensed");
            yearsExperience = rs.getString("years_experience");
            primarySpecialization = rs.getString("primary_specialization");
            cityPractice = rs.getString("city_practice");
            hourlyRate = rs.getString("hourly_rate");
            bio = rs.getString("bio");
        }

        if (hourlyRate != null) {
            hourlyRate = hourlyRate.replace("Rs.", "₹").replace("Rs", "₹").replace("₹ ", "₹");
        }
    } catch (Exception e) {
        e.printStackTrace();
    } finally {
        if (rs != null) rs.close();
        if (pstmt != null) pstmt.close();
        if (conn != null) conn.close();
    }
%>
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
        
        .profile-container {
            max-width: 900px;
            margin: 0 auto;
        }
        
        .card {
            background: white;
            padding: 2rem;
            border-radius: 12px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.05);
            margin-bottom: 1.5rem;
        }
        .card h3 {
            color: #111827;
            margin-bottom: 1.5rem;
            font-size: 1.3rem;
            padding-bottom: 1rem;
            border-bottom: 2px solid #F8FAFC;
        }
        
        .form-group {
            margin-bottom: 1.5rem;
        }
        .form-group label {
            display: block;
            color: #111827;
            font-weight: 500;
            margin-bottom: 0.5rem;
            font-size: 0.95rem;
        }
        .required { color: #ef4444; }
        .form-control {
            width: 100%;
            padding: 0.75rem;
            border: 2px solid #E5E7EB;
            border-radius: 8px;
            font-size: 0.95rem;
            font-family: 'Inter', sans-serif;
            transition: border-color 0.3s;
        }
        .form-control:focus {
            outline: none;
            border-color: #C9A227;
        }
        .form-control:disabled {
            background: #F8FAFC;
            cursor: not-allowed;
        }
        textarea.form-control { min-height: 120px; resize: vertical; }
        
        .form-row {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 1rem;
        }
        
        .btn-section {
            display: flex;
            gap: 1rem;
            margin-top: 2rem;
        }
        .btn-update, .btn-cancel {
            flex: 1;
            padding: 0.875rem;
            border-radius: 8px;
            font-weight: 600;
            cursor: pointer;
            border: none;
            font-size: 1rem;
            transition: all 0.3s;
        }
        .btn-update {
            background: #C9A227;
            color: white;
        }
        .btn-update:hover {
            background: #A9861F;
            transform: translateY(-2px);
        }
        .btn-cancel {
            background: #F8FAFC;
            color: #111827;
        }
        .btn-cancel:hover {
            background: #E5E7EB;
        }
        
        .alert {
            padding: 1rem;
            border-radius: 8px;
            margin-bottom: 1.5rem;
            display: none;
        }
        .alert-success {
            background: #dcfce7;
            color: #166534;
            border: 1px solid #86efac;
        }
        .alert-error {
            background: #fee2e2;
            color: #991b1b;
            border: 1px solid #fca5a5;
        }
        .alert i {
            margin-right: 0.5rem;
        }
        
        .profile-avatar {
            width: 100px;
            height: 100px;
            background: linear-gradient(135deg, #0B1F3A 0%, #C9A227 100%);
            color: white;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 2.5rem;
            font-weight: 700;
            margin: 0 auto 1.5rem;
        }
        
        @media (max-width: 600px) {
            .form-row { grid-template-columns: 1fr; }
        }
    </style>
</head>
<body>
    <div class="profile-container">
        <div class="content-header">
            <h1>Lawyer Profile</h1>
            <p>Manage your professional information and credentials</p>
        </div>

        <div id="alertBox" class="alert"></div>

        <div class="card">
            <div class="profile-avatar">
                <%= firstName.charAt(0) %><%= lastName.charAt(0) %>
            </div>
            
            <h3>Personal Information</h3>
            
            <form id="profileForm" action="UpdateLawyerProfileServlet" method="POST">
                <div class="form-row">
                    <div class="form-group">
                        <label for="firstName">First Name <span class="required">*</span></label>
                        <input type="text" id="firstName" name="firstName" class="form-control" 
                               value="<%= firstName %>" required>
                    </div>
                    <div class="form-group">
                        <label for="lastName">Last Name <span class="required">*</span></label>
                        <input type="text" id="lastName" name="lastName" class="form-control" 
                               value="<%= lastName %>" required>
                    </div>
                </div>
                
                <div class="form-group">
                    <label for="email">Email Address</label>
                    <input type="email" id="email" name="email" class="form-control" 
                           value="<%= email %>" disabled>
                    <small style="color: #6b7280; font-size: 0.85rem;">Email cannot be changed</small>
                </div>
                
                <div class="form-row">
                    <div class="form-group">
                        <label for="phone">Phone Number <span class="required">*</span></label>
                        <input type="tel" id="phone" name="phone" class="form-control" 
                               value="<%= phone %>" required>
                    </div>
                    <div class="form-group">
                        <label for="city">City <span class="required">*</span></label>
                        <input type="text" id="city" name="city" class="form-control" 
                               value="<%= city %>" required>
                    </div>
                </div>
                
                <h3 style="margin-top: 2rem;">Professional Details</h3>
                
                <div class="form-row">
                    <div class="form-group">
                        <label for="barNumber">Bar Council Number <span class="required">*</span></label>
                        <input type="text" id="barNumber" name="barNumber" class="form-control" 
                               value="<%= barNumber %>" required>
                    </div>
                    <div class="form-group">
                        <label for="stateLicensed">State Licensed <span class="required">*</span></label>
                        <input type="text" id="stateLicensed" name="stateLicensed" class="form-control" 
                               value="<%= stateLicensed %>" required>
                    </div>
                </div>
                
                <div class="form-row">
                    <div class="form-group">
                        <label for="yearsExperience">Years of Experience <span class="required">*</span></label>
                        <select id="yearsExperience" name="yearsExperience" class="form-control" required>
                            <option value="">Select Experience</option>
                            <option value="0-2 years" <%= "0-2 years".equals(yearsExperience) ? "selected" : "" %>>0-2 years</option>
                            <option value="3-5 years" <%= "3-5 years".equals(yearsExperience) ? "selected" : "" %>>3-5 years</option>
                            <option value="6-10 years" <%= "6-10 years".equals(yearsExperience) ? "selected" : "" %>>6-10 years</option>
                            <option value="10+ years" <%= "10+ years".equals(yearsExperience) ? "selected" : "" %>>10+ years</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label for="primarySpecialization">Primary Specialization <span class="required">*</span></label>
                        <select id="primarySpecialization" name="primarySpecialization" class="form-control" required>
                            <option value="">Select Specialization</option>
                            <option value="Criminal Law" <%= "Criminal Law".equals(primarySpecialization) ? "selected" : "" %>>Criminal Law</option>
                            <option value="Civil Law" <%= "Civil Law".equals(primarySpecialization) ? "selected" : "" %>>Civil Law</option>
                            <option value="Family Law" <%= "Family Law".equals(primarySpecialization) ? "selected" : "" %>>Family Law</option>
                            <option value="Corporate Law" <%= "Corporate Law".equals(primarySpecialization) ? "selected" : "" %>>Corporate Law</option>
                            <option value="Property Law" <%= "Property Law".equals(primarySpecialization) ? "selected" : "" %>>Property Law</option>
                            <option value="Tax Law" <%= "Tax Law".equals(primarySpecialization) ? "selected" : "" %>>Tax Law</option>
                            <option value="Labour Law" <%= "Labour Law".equals(primarySpecialization) ? "selected" : "" %>>Labour Law</option>
                            <option value="Intellectual Property" <%= "Intellectual Property".equals(primarySpecialization) ? "selected" : "" %>>Intellectual Property</option>
                            <option value="Consumer Law" <%= "Consumer Law".equals(primarySpecialization) ? "selected" : "" %>>Consumer Law</option>
                        </select>
                    </div>
                </div>
                
                <div class="form-row">
                    <div class="form-group">
                        <label for="cityPractice">City of Practice <span class="required">*</span></label>
                        <input type="text" id="cityPractice" name="cityPractice" class="form-control" 
                               value="<%= cityPractice %>" required>
                    </div>
                    <div class="form-group">
                        <label for="hourlyRate">Hourly Rate <span class="required">*</span></label>
                        <select id="hourlyRate" name="hourlyRate" class="form-control" required>
                            <option value="">Select Rate</option>
                            <option value="₹500-1000/hour" <%= "₹500-1000/hour".equals(hourlyRate) ? "selected" : "" %>>₹500-1000/hour</option>
                            <option value="₹1000-2000/hour" <%= "₹1000-2000/hour".equals(hourlyRate) ? "selected" : "" %>>₹1000-2000/hour</option>
                            <option value="₹2000-5000/hour" <%= "₹2000-5000/hour".equals(hourlyRate) ? "selected" : "" %>>₹2000-5000/hour</option>
                            <option value="₹5000+/hour" <%= "₹5000+/hour".equals(hourlyRate) ? "selected" : "" %>>₹5000+/hour</option>
                        </select>
                    </div>
                </div>
                
                <div class="form-group">
                    <label for="bio">Professional Bio</label>
                    <textarea id="bio" name="bio" class="form-control" 
                              placeholder="Describe your legal expertise, achievements, and approach..."><%= bio != null ? bio : "" %></textarea>
                </div>
                
                <div class="btn-section">
                    <button type="button" class="btn-cancel" onclick="resetForm()">
                        <i class="fas fa-undo"></i> Reset
                    </button>
                    <button type="submit" class="btn-update">
                        <i class="fas fa-save"></i> Update Profile
                    </button>
                </div>
            </form>
        </div>
    </div>

    <script>
        window.onload = function() {
            var urlParams = new URLSearchParams(window.location.search);
            
            if (urlParams.get('success') === 'true') {
                showAlert('Profile updated successfully!', 'success');
                window.history.replaceState({}, document.title, window.location.pathname);
            } else if (urlParams.get('error') === 'failed') {
                showAlert('Update failed. Please try again.', 'error');
                window.history.replaceState({}, document.title, window.location.pathname);
            }
        };

        function showAlert(message, type) {
            var alertBox = document.getElementById('alertBox');
            alertBox.className = 'alert alert-' + type;
            alertBox.style.display = 'block';
            
            if (type === 'success') {
                alertBox.innerHTML = '<i class="fas fa-check-circle"></i>' + message;
            } else {
                alertBox.innerHTML = '<i class="fas fa-exclamation-circle"></i>' + message;
            }
            
            setTimeout(function() {
                alertBox.style.display = 'none';
            }, 5000);
        }

        function resetForm() {
            if (confirm('Are you sure you want to reset all changes?')) {
                document.getElementById('profileForm').reset();
            }
        }
    </script>
</body>
</html>
