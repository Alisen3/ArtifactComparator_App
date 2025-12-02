import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import './ResearcherDashboard.css';
import './AdminDashboard.css';
import StudyCard from '../../components/StudyCard';
import { api } from '../../context/AuthContext';

const AdminDashboard = () => {
    const navigate = useNavigate();
    const [allStudies, setAllStudies] = useState([]);
    const [users, setUsers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    
    useEffect(() => {
        const fetchData = async () => {
            try {
                setLoading(true);
                // Fetch studies and users in parallel
                const [studiesResponse, usersResponse] = await Promise.all([
                    api.get('/api/studies/my-studies'),
                    api.get('/api/users')
                ]);
                
                const studiesData = studiesResponse.data || [];
                const usersData = usersResponse.data || [];
                
                // Filter out password from users
                const sanitizedUsers = usersData.map(user => ({
                    id: user.id,
                    name: user.name,
                    email: user.email,
                    role: user.role
                }));
                setUsers(sanitizedUsers);
                
                // Fetch tasks for each study to calculate stats and get creator info
                const studiesWithStats = await Promise.all(
                    studiesData.map(async (study) => {
                        try {
                            const [tasksResponse, collaboratorsResponse] = await Promise.all([
                                api.get(`/api/studies/${study.id}/tasks`).catch(() => ({ data: [] })),
                                api.get(`/api/studies/${study.id}/collaborators`).catch(() => ({ data: [] }))
                            ]);
                            
                            const tasks = tasksResponse.data || [];
                            const collaborators = collaboratorsResponse.data || [];
                            
                            // Find owner/creator from collaborators (owner has OWNER role)
                            const owner = collaborators.find(c => c.role === 'OWNER');
                            const researcherName = owner ? owner.collaboratorName : 'Unknown';
                            
                            // Calculate participant count (unique participants)
                            const participantIds = new Set(tasks.map(t => t.participantId));
                            const participantCount = participantIds.size;
                            
                            // Calculate task count
                            const taskCount = tasks.length;
                            
                            // Calculate completion percentage
                            const completedTasks = tasks.filter(t => t.status === 'COMPLETED').length;
                            const completionPercentage = taskCount > 0 
                                ? Math.round((completedTasks / taskCount) * 100) 
                                : 0;
                            
                            return {
                                id: study.id,
                                title: study.title,
                                participantCount,
                                taskCount,
                                completionPercentage,
                                researcherName
                            };
                        } catch (err) {
                            console.error(`Error fetching data for study ${study.id}:`, err);
                            return {
                                id: study.id,
                                title: study.title,
                                participantCount: 0,
                                taskCount: 0,
                                completionPercentage: 0,
                                researcherName: 'Unknown'
                            };
                        }
                    })
                );
                
                setAllStudies(studiesWithStats);
            } catch (err) {
                console.error('Error fetching data:', err);
                setError('Failed to load dashboard data. Please try again.');
            } finally {
                setLoading(false);
            }
        };
        
        fetchData();
    }, []);

    const handleStudyClick = (studyId) => {
        navigate(`/admin-dashboard/study/${studyId}`);
    };

    const handleDeleteStudy = (studyId) => {
        const study = allStudies.find((item) => item.id === studyId);
        const studyTitle = study ? study.title : `Study ${studyId}`;
        alert(`Delete ${studyTitle} (Mock functionality)`);
    };

    const handleRoleChange = async (userId, newRole) => {
        try {
            // Update user role via API
            // Note: This endpoint may need to be created in the backend
            await api.put(`/api/users/${userId}/role`, { role: newRole });
            
            // Update local state
            setUsers(users.map(user => 
                user.id === userId ? { ...user, role: newRole } : user
            ));
        } catch (err) {
            console.error('Error updating user role:', err);
            alert(`Failed to update user role: ${err.response?.data?.message || err.message}`);
        }
    };
    
    const handleSaveRole = async (userId) => {
        const user = users.find(u => u.id === userId);
        if (!user) return;
        
        try {
            // Try to update via PUT endpoint
            await api.put(`/api/users/${userId}/role`, { role: user.role });
            alert(`Role updated to ${user.role} for ${user.name}`);
        } catch (err) {
            // If endpoint doesn't exist, just show a message
            console.error('Error updating user role:', err);
            alert(`Role update functionality may require backend endpoint. Current role: ${user.role}`);
        }
    };

    return (
        <div className="researcher-dashboard">
            <div className="dashboard-header">
                <h1>Admin Dashboard</h1>
                <p className="dashboard-subtitle">System-wide overview and management</p>
            </div>

            {loading ? (
                <div className="admin-section">
                    <div className="admin-section-content">
                        <p style={{textAlign: 'center', color: '#999', padding: '2rem'}}>
                            Loading dashboard data...
                        </p>
                    </div>
                </div>
            ) : error ? (
                <div className="admin-section">
                    <div className="admin-section-content">
                        <p style={{textAlign: 'center', color: '#f44336', padding: '2rem'}}>
                            {error}
                        </p>
                    </div>
                </div>
            ) : (
                <>
                    {/* All Studies Section */}
                    <div className="admin-section">
                        <div className="admin-section-header">
                            <h2>All Studies ({allStudies.length})</h2>
                        </div>
                        <div className="admin-section-content">
                            <div className="studies-grid">
                                {allStudies.length === 0 ? (
                                    <p style={{textAlign: 'center', color: '#999', padding: '2rem'}}>
                                        No studies found.
                                    </p>
                                ) : (
                                    allStudies.map(study => (
                                        <StudyCard 
                                            key={study.id} 
                                            study={study}
                                            onClick={() => handleStudyClick(study.id)}
                                            showSettingsMenu
                                            onDelete={handleDeleteStudy}
                                        />
                                    ))
                                )}
                            </div>
                        </div>
                    </div>

            {/* User Management Section */}
            <div className="admin-section">
                <div className="admin-section-header">
                    <h2>User Management ({users.length} users)</h2>
                </div>
                <div className="admin-section-content">
                    <div className="users-table">
                        <div className="users-header-row">
                            <div className="user-col-name">Name</div>
                            <div className="user-col-email">Email</div>
                            <div className="user-col-role">Role</div>
                            <div className="user-col-actions">Actions</div>
                        </div>
                        {users.map(user => (
                            <div key={user.id} className="user-row">
                                <div className="user-col-name">
                                    <span className="user-name">{user.name}</span>
                                </div>
                                <div className="user-col-email">
                                    <span className="user-email">{user.email}</span>
                                </div>
                                <div className="user-col-role">
                                    <select 
                                        className="role-select"
                                        value={user.role}
                                        onChange={(e) => handleRoleChange(user.id, e.target.value)}
                                    >
                                        <option value="PARTICIPANT">Participant</option>
                                        <option value="RESEARCHER">Researcher</option>
                                        <option value="ADMIN">Admin</option>
                                    </select>
                                </div>
                                <div className="user-col-actions">
                                    <button 
                                        className="save-role-button"
                                        onClick={() => handleSaveRole(user.id)}
                                    >
                                        Save
                                    </button>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            </div>
                </>
            )}
        </div>
    );
};

export default AdminDashboard;
