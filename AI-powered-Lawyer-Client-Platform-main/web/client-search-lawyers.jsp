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
        .card {
            background: white;
            padding: 2rem;
            border-radius: 12px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.05);
            margin-bottom: 1.5rem;
        }
        .card h3 { color: #111827; margin-bottom: 1rem; }
        .filter-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 1.5rem;
        }
        .filter-item label { display: block; color: #111827; font-weight: 500; margin-bottom: 0.5rem; font-size: 0.9rem; }
        .filter-control {
            width: 100%;
            padding: 0.75rem;
            border: 2px solid #E5E7EB;
            border-radius: 8px;
            font-size: 0.95rem;
            font-family: 'Inter', sans-serif;
        }
        .filter-control:focus { outline: none; border-color: #C9A227; }
        .btn-filter, .btn-reset {
            padding: 0.75rem 1.5rem;
            border-radius: 8px;
            font-weight: 600;
            cursor: pointer;
            border: none;
            font-size: 0.95rem;
        }
        .btn-filter { background: #C9A227; color: white; flex: 1; }
        .btn-filter:hover { background: #A9861F; }
        .btn-reset { background: #F8FAFC; color: #111827; }
        .lawyers-grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
            gap: 1.5rem;
            margin-top: 2rem;
        }
        .lawyer-card {
            background: white;
            border-radius: 12px;
            padding: 1.5rem;
            box-shadow: 0 2px 8px rgba(0,0,0,0.05);
            border: 2px solid transparent;
            transition: all 0.3s;
        }
        .lawyer-card:hover { transform: translateY(-5px); box-shadow: 0 8px 20px rgba(0,0,0,0.1); border-color: #C9A227; }
        .lawyer-header { display: flex; gap: 1rem; margin-bottom: 1rem; padding-bottom: 1rem; border-bottom: 2px solid #F8FAFC; }
        .lawyer-avatar {
            width: 70px; height: 70px; border-radius: 50%;
            background: linear-gradient(135deg, #0B1F3A 0%, #0B1F3A 100%);
            color: white; display: flex; align-items: center; justify-content: center;
            font-size: 1.8rem; font-weight: 700; flex-shrink: 0;
        }
        .lawyer-name { font-size: 1.2rem; font-weight: 700; color: #111827; margin-bottom: 0.25rem; }
        .lawyer-specialization { color: #C9A227; font-weight: 600; font-size: 0.9rem; margin-bottom: 0.5rem; }
        .lawyer-rating { color: #f59e0b; font-size: 0.9rem; }
        .detail-item { display: flex; gap: 0.75rem; padding: 0.5rem 0; color: #6b7280; font-size: 0.9rem; }
        .detail-item i { color: #C9A227; width: 18px; }
        .lawyer-bio { color: #6b7280; font-size: 0.9rem; line-height: 1.5; margin: 1rem 0; }
        .lawyer-footer { margin-top: 1rem; }
        .btn-contact {
            width: 100%;
            padding: 0.75rem;
            border-radius: 8px;
            font-weight: 600;
            cursor: pointer;
            border: none;
            font-size: 0.9rem;
            background: #C9A227;
            color: white;
            transition: all 0.3s;
        }
        .btn-contact:hover { background: #A9861F; }
        .verified-badge { background: #dcfce7; color: #166534; padding: 0.25rem 0.5rem; border-radius: 4px; font-size: 0.75rem; margin-left: 0.5rem; font-weight: 600; }
        
        /* Contact Modal */
        .modal {
            display: none;
            position: fixed;
            z-index: 9999;
            left: 0;
            top: 0;
            width: 100%;
            height: 100%;
            background: rgba(0,0,0,0.5);
            justify-content: center;
            align-items: center;
        }
        .modal.active { display: flex; }
        .modal-content {
            background: white;
            border-radius: 12px;
            padding: 2rem;
            max-width: 500px;
            width: 90%;
            position: relative;
            box-shadow: 0 10px 40px rgba(0,0,0,0.3);
        }
        .modal-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 1.5rem;
            padding-bottom: 1rem;
            border-bottom: 2px solid #F8FAFC;
        }
        .modal-header h2 {
            color: #111827;
            font-size: 1.5rem;
            margin: 0;
        }
        .close-btn {
            background: none;
            border: none;
            font-size: 1.5rem;
            color: #6b7280;
            cursor: pointer;
            width: 30px;
            height: 30px;
            display: flex;
            align-items: center;
            justify-content: center;
        }
        .close-btn:hover { color: #111827; }
        .contact-info {
            margin: 1.5rem 0;
        }
        .contact-item {
            display: flex;
            align-items: center;
            gap: 1rem;
            padding: 1rem;
            background: #F8FAFC;
            border-radius: 8px;
            margin-bottom: 1rem;
        }
        .contact-icon {
            width: 45px;
            height: 45px;
            background: linear-gradient(135deg, #0B1F3A 0%, #0B1F3A 100%);
            color: white;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 1.2rem;
            flex-shrink: 0;
        }
        .contact-details {
            flex: 1;
        }
        .contact-label {
            font-size: 0.85rem;
            color: #6b7280;
            margin-bottom: 0.25rem;
        }
        .contact-value {
            font-size: 1rem;
            color: #111827;
            font-weight: 600;
        }
        .copy-btn {
            padding: 0.5rem 1rem;
            background: #C9A227;
            color: white;
            border: none;
            border-radius: 6px;
            cursor: pointer;
            font-size: 0.85rem;
            font-weight: 600;
            transition: all 0.3s;
        }
        .copy-btn:hover {
            background: #A9861F;
        }
        .modal-lawyer-info {
            text-align: center;
            margin-bottom: 1.5rem;
        }
        .modal-lawyer-avatar {
            width: 80px;
            height: 80px;
            background: linear-gradient(135deg, #0B1F3A 0%, #0B1F3A 100%);
            color: white;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 2rem;
            font-weight: 700;
            margin: 0 auto 1rem;
        }
        .modal-lawyer-name {
            font-size: 1.3rem;
            font-weight: 700;
            color: #111827;
            margin-bottom: 0.25rem;
        }
        .modal-lawyer-spec {
            color: #C9A227;
            font-weight: 600;
        }
    </style>
</head>
<body>
    <div class="content-header">
        <h1>Search Lawyers</h1>
        <p>Find the perfect lawyer for your case</p>
    </div>

    <div class="card">
        <h3>Filter Lawyers</h3>
        <form id="filterForm" style="margin-top: 1.5rem;">
            <div class="filter-grid">
                <div class="filter-item">
                    <label>Specialization</label>
                    <select id="filterSpecialization" class="filter-control">
                        <option value="">All Specializations</option>
                        <option value="Criminal Law">Criminal Law</option>
                        <option value="Civil Law">Civil Law</option>
                        <option value="Family Law">Family Law</option>
                        <option value="Corporate Law">Corporate Law</option>
                        <option value="Property Law">Property Law</option>
                        <option value="Tax Law">Tax Law</option>
                        <option value="Labour Law">Labour Law</option>
                        <option value="Intellectual Property">Intellectual Property</option>
                        <option value="Consumer Law">Consumer Law</option>
                    </select>
                </div>
                <div class="filter-item">
                    <label>Experience</label>
                    <select id="filterExperience" class="filter-control">
                        <option value="">Any Experience</option>
                        <option value="0-2 years">0-2 years</option>
                        <option value="3-5 years">3-5 years</option>
                        <option value="6-10 years">6-10 years</option>
                        <option value="10+ years">10+ years</option>
                    </select>
                </div>
                <div class="filter-item">
                    <label>Price Range</label>
                    <select id="filterPrice" class="filter-control">
                        <option value="">Any Price</option>
                        <option value="₹500-1000/hour">₹500-1000/hour</option>
                        <option value="₹1000-2000/hour">₹1000-2000/hour</option>
                        <option value="₹2000-5000/hour">₹2000-5000/hour</option>
                        <option value="₹5000+/hour">₹5000+/hour</option>
                    </select>
                </div>
                <div class="filter-item">
                    <label>State/City</label>
                    <input type="text" id="filterLocation" class="filter-control" placeholder="Enter location">
                </div>
            </div>
            <div style="margin-top: 1.5rem; display: flex; gap: 1rem;">
                <button type="button" onclick="searchLawyers()" class="btn-filter">Search Lawyers</button>
                <button type="button" onclick="resetFilters()" class="btn-reset">Reset Filters</button>
            </div>
        </form>
    </div>

    <div id="loadingState" style="display: none; text-align: center; padding: 3rem;">
        <i class="fas fa-spinner fa-spin" style="font-size: 3rem; color: #C9A227;"></i>
        <p style="margin-top: 1rem; color: #6b7280;">Loading lawyers...</p>
    </div>

    <div id="lawyersGrid" class="lawyers-grid"></div>

    <div id="emptyState" style="display: none; text-align: center; padding: 3rem;">
        <i class="fas fa-user-slash" style="font-size: 4rem; color: #E5E7EB;"></i>
        <h3 style="margin-top: 1rem; color: #111827;">No Lawyers Found</h3>
        <p style="color: #6b7280;">Try adjusting your filters or check if lawyers are registered.</p>
    </div>

    <!-- Contact Modal -->
    <div id="contactModal" class="modal">
        <div class="modal-content">
            <div class="modal-header">
                <h2>Contact Lawyer</h2>
                <button class="close-btn" onclick="closeContactModal()">&times;</button>
            </div>
            
            <div class="modal-lawyer-info">
                <div class="modal-lawyer-avatar" id="modalAvatar"></div>
                <div class="modal-lawyer-name" id="modalName"></div>
                <div class="modal-lawyer-spec" id="modalSpec"></div>
            </div>
            
            <div class="contact-info">
                <div class="contact-item">
                    <div class="contact-icon">
                        <i class="fas fa-envelope"></i>
                    </div>
                    <div class="contact-details">
                        <div class="contact-label">Email Address</div>
                        <div class="contact-value" id="modalEmail"></div>
                    </div>
                    <button class="copy-btn" onclick="copyToClipboard('email')">
                        <i class="fas fa-copy"></i> Copy
                    </button>
                </div>
                
                <div class="contact-item">
                    <div class="contact-icon">
                        <i class="fas fa-phone"></i>
                    </div>
                    <div class="contact-details">
                        <div class="contact-label">Phone Number</div>
                        <div class="contact-value" id="modalPhone"></div>
                    </div>
                    <button class="copy-btn" onclick="copyToClipboard('phone')">
                        <i class="fas fa-copy"></i> Copy
                    </button>
                </div>
            </div>
        </div>
    </div>

    <script>
        var currentLawyerContact = { email: '', phone: '' };

        function formatHourlyRate(rate) {
            if (!rate) return '';
            return rate.replace(/Rs\./gi, '₹').replace(/Rs/gi, '₹').replace(/₹\s+/g, '₹');
        }

        function formatRatingText(lawyer) {
            var reviewCount = Number(lawyer.reviewCount || 0);
            if (reviewCount === 0 || lawyer.avgRating === null || lawyer.avgRating === undefined) {
                return 'Rating: New (No reviews yet)';
            }
            return 'Rating: ' + Number(lawyer.avgRating).toFixed(1) + '/5 (' + reviewCount + ' reviews)';
        }

        window.onload = function() { 
            console.log('Page loaded');
            searchLawyers(); 
        };

        function searchLawyers() {
            console.log('Searching lawyers...');
            
            var params = new URLSearchParams();
            var spec = document.getElementById('filterSpecialization').value;
            var exp = document.getElementById('filterExperience').value;
            var price = document.getElementById('filterPrice').value;
            var loc = document.getElementById('filterLocation').value;
            
            if (spec) params.append('specialization', spec);
            if (exp) params.append('experience', exp);
            if (price) params.append('priceRange', price);
            if (loc) params.append('location', loc);

            document.getElementById('loadingState').style.display = 'block';
            document.getElementById('lawyersGrid').style.display = 'none';
            document.getElementById('emptyState').style.display = 'none';

            fetch('SearchLawyersServlet?' + params.toString())
                .then(function(response) {
                    console.log('Response status:', response.status);
                    return response.json();
                })
                .then(function(data) {
                    console.log('Got lawyers:', data.length);
                    document.getElementById('loadingState').style.display = 'none';
                    
                    var grid = document.getElementById('lawyersGrid');
                    grid.innerHTML = '';
                    
                    if (data.length === 0) {
                        document.getElementById('emptyState').style.display = 'block';
                        return;
                    }
                    
                    document.getElementById('lawyersGrid').style.display = 'grid';
                    
                    data.forEach(function(lawyer) {
                        var card = document.createElement('div');
                        card.className = 'lawyer-card';
                        
                        var initials = lawyer.firstName.charAt(0) + lawyer.lastName.charAt(0);
                        var verified = lawyer.isVerified ? '<span class="verified-badge">Verified</span>' : '';
                        var bio = lawyer.bio && lawyer.bio.length > 0 ? '<div class="lawyer-bio">' + lawyer.bio.substring(0, 120) + '...</div>' : '';
                        
                        card.innerHTML = 
                            '<div class="lawyer-header">' +
                                '<div class="lawyer-avatar">' + initials + '</div>' +
                                '<div style="flex: 1;">' +
                                    '<div class="lawyer-name">Adv. ' + lawyer.firstName + ' ' + lawyer.lastName + ' ' + verified + '</div>' +
                                    '<div class="lawyer-specialization">' + lawyer.specialization + '</div>' +
                                    '<div class="lawyer-rating">' + formatRatingText(lawyer) + '</div>' +
                                '</div>' +
                            '</div>' +
                            '<div>' +
                                '<div class="detail-item"><i class="fas fa-briefcase"></i><span>' + lawyer.experience + ' experience</span></div>' +
                                '<div class="detail-item"><i class="fas fa-map-marker-alt"></i><span>' + lawyer.city + ', ' + lawyer.state + '</span></div>' +
                                '<div class="detail-item"><i class="fas fa-money-bill"></i><span>' + formatHourlyRate(lawyer.hourlyRate) + '</span></div>' +
                                '<div class="detail-item"><i class="fas fa-id-card"></i><span>Bar: ' + lawyer.barNumber + '</span></div>' +
                            '</div>' +
                            bio +
                            '<div class="lawyer-footer">' +
                                '<button class="btn-contact" onclick=\'showContactModal(' + JSON.stringify(lawyer) + ')\'>' +
                                    '<i class="fas fa-phone"></i> Contact Lawyer' +
                                '</button>' +
                            '</div>';
                        
                        grid.appendChild(card);
                    });
                })
                .catch(function(error) {
                    console.error('Error:', error);
                    document.getElementById('loadingState').style.display = 'none';
                    document.getElementById('emptyState').style.display = 'block';
                });
        }

        function showContactModal(lawyer) {
            currentLawyerContact.email = lawyer.email;
            currentLawyerContact.phone = lawyer.phone;
            
            var initials = lawyer.firstName.charAt(0) + lawyer.lastName.charAt(0);
            
            document.getElementById('modalAvatar').textContent = initials;
            document.getElementById('modalName').textContent = 'Adv. ' + lawyer.firstName + ' ' + lawyer.lastName;
            document.getElementById('modalSpec').textContent = lawyer.specialization;
            document.getElementById('modalEmail').textContent = lawyer.email;
            document.getElementById('modalPhone').textContent = lawyer.phone;
            
            document.getElementById('contactModal').classList.add('active');
            document.body.style.overflow = 'hidden';
        }

        function closeContactModal() {
            document.getElementById('contactModal').classList.remove('active');
            document.body.style.overflow = 'auto';
        }

        function copyToClipboard(type) {
            var text = type === 'email' ? currentLawyerContact.email : currentLawyerContact.phone;
            
            navigator.clipboard.writeText(text).then(function() {
                var btn = event.target.closest('.copy-btn');
                var originalText = btn.innerHTML;
                btn.innerHTML = '<i class="fas fa-check"></i> Copied!';
                btn.style.background = '#10b981';
                
                setTimeout(function() {
                    btn.innerHTML = originalText;
                    btn.style.background = '#C9A227';
                }, 2000);
            }).catch(function(err) {
                alert('Failed to copy: ' + text);
            });
        }

        function resetFilters() {
            document.getElementById('filterForm').reset();
            searchLawyers();
        }

        // Close modal when clicking outside
        window.onclick = function(event) {
            var modal = document.getElementById('contactModal');
            if (event.target === modal) {
                closeContactModal();
            }
        }
    </script>
</body>
</html>
