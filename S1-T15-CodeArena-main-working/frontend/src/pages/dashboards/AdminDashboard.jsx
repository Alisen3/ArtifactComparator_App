import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import './ResearcherDashboard.css';
import './AdminDashboard.css';
import StudyCard from '../../components/StudyCard';

const AdminDashboard = () => {
    const navigate = useNavigate();
    
    // Mock data for ALL studies (admin sees all)
    const [allStudies] = useState([
        {
            id: 1,
            title: 'Code Quality Evaluation Study',
            participantCount: 45,
            taskCount: 120,
            completionPercentage: 67,
            researcherName: 'Dr. Smith'
        },
        {
            id: 2,
            title: 'API Design Comparison',
            participantCount: 32,
            taskCount: 95,
            completionPercentage: 84,
            researcherName: 'Dr. Johnson'
        },
        {
            id: 3,
            title: 'Documentation Quality Assessment',
            participantCount: 28,
            taskCount: 78,
            completionPercentage: 43,
            researcherName: 'Dr. Williams'
        },
        {
            id: 4,
            title: 'Performance Benchmark Study',
            participantCount: 15,
            taskCount: 60,
            completionPercentage: 95,
            researcherName: 'Dr. Brown'
        },
        {
            id: 5,
            title: 'Security Analysis Study',
            participantCount: 38,
            taskCount: 110,
            completionPercentage: 58,
            researcherName: 'Dr. Davis'
        },
        {
            id: 6,
            title: 'User Experience Evaluation',
            participantCount: 22,
            taskCount: 75,
            completionPercentage: 76,
            researcherName: 'Dr. Miller'
        },
        {
            id: 7,
            title: 'Code Maintainability Study',
            participantCount: 30,
            taskCount: 88,
            completionPercentage: 62,
            researcherName: 'Dr. Wilson'
        },
        {
            id: 8,
            title: 'Algorithm Efficiency Comparison',
            participantCount: 19,
            taskCount: 55,
            completionPercentage: 91,
            researcherName: 'Dr. Moore'
        }
    ]);

    // Mock data for all users
    const [users, setUsers] = useState([
        { id: 1, name: 'John Doe', email: 'john.doe@example.com', role: 'RESEARCHER' },
        { id: 2, name: 'Jane Smith', email: 'jane.smith@example.com', role: 'RESEARCHER' },
        { id: 3, name: 'Bob Johnson', email: 'bob.johnson@example.com', role: 'PARTICIPANT' },
        { id: 4, name: 'Alice Brown', email: 'alice.brown@example.com', role: 'PARTICIPANT' },
        { id: 5, name: 'Charlie Wilson', email: 'charlie.wilson@example.com', role: 'PARTICIPANT' },
        { id: 6, name: 'Diana Lee', email: 'diana.lee@example.com', role: 'RESEARCHER' },
        { id: 7, name: 'Eve Martinez', email: 'eve.martinez@example.com', role: 'PARTICIPANT' },
        { id: 8, name: 'Frank Garcia', email: 'frank.garcia@example.com', role: 'PARTICIPANT' },
        { id: 9, name: 'Grace Kim', email: 'grace.kim@example.com', role: 'PARTICIPANT' },
        { id: 10, name: 'Henry Chen', email: 'henry.chen@example.com', role: 'RESEARCHER' },
        { id: 11, name: 'Ivy Taylor', email: 'ivy.taylor@example.com', role: 'PARTICIPANT' },
        { id: 12, name: 'Jack White', email: 'jack.white@example.com', role: 'PARTICIPANT' }
    ]);

    const handleStudyClick = (studyId) => {
        navigate(`/admin-dashboard/study/${studyId}`);
    };

    const handleDeleteStudy = (studyId) => {
        const study = allStudies.find((item) => item.id === studyId);
        const studyTitle = study ? study.title : `Study ${studyId}`;
        alert(`Delete ${studyTitle} (Mock functionality)`);
    };

    const handleRoleChange = (userId, newRole) => {
        setUsers(users.map(user => 
            user.id === userId ? { ...user, role: newRole } : user
        ));
    };

    return (
        <div className="researcher-dashboard">
            <div className="dashboard-header">
                <h1>Admin Dashboard</h1>
                <p className="dashboard-subtitle">System-wide overview and management</p>
            </div>

            {/* All Studies Section */}
            <div className="admin-section">
                <div className="admin-section-header">
                    <h2>All Studies ({allStudies.length})</h2>
                </div>
                <div className="admin-section-content">
                    <div className="studies-grid">
                        {allStudies.map(study => (
                            <StudyCard 
                                key={study.id} 
                                study={study}
                                onClick={() => handleStudyClick(study.id)}
                                showSettingsMenu
                                onDelete={handleDeleteStudy}
                            />
                        ))}
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
                                        onClick={() => alert(`Role updated to ${user.role} for ${user.name} (Mock functionality)`)}
                                    >
                                        Save
                                    </button>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default AdminDashboard;
