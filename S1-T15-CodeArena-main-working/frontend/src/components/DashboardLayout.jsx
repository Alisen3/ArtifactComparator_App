// File Path: src/components/DashboardLayout.jsx
import React from 'react';
import { Outlet, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import './DashboardLayout.css';
import CodeArenaLogo from '../assets/CodeArenaLogo.png';

const DashboardLayout = () => {
    const { user, logout } = useAuth();
    const navigate = useNavigate();

    const handleLogout = () => {
        logout();
        navigate('/login');
    };

    const isResearcher = user.role === 'RESEARCHER';
    const isAdmin = user.role === 'ADMIN';
    const isParticipant = user.role === 'PARTICIPANT';

    return (
        <div className="dashboard-layout">

            {/* --- 1. Left Sidebar --- */}
            <aside className="sidebar">
                <div className="sidebar-header">
                    <img src={CodeArenaLogo} alt="CodeArena Logo" className="sidebar-logo" />
                    <h1>CodeArena</h1>
                </div>

                {/* --- 2. Navigation Links --- */}
                <nav className="sidebar-nav">
                    <ul className="sidebar-nav-list">
                        {isAdmin ? (
                            <>
                                <li>
                                    <NavLink to="/admin-dashboard" end className="sidebar-nav-link">
                                        Dashboard
                                    </NavLink>
                                </li>
                            </>
                        ) : isResearcher ? (
                            <>
                                <li>
                                    <NavLink to="/researcher-dashboard" end className="sidebar-nav-link">
                                        Dashboard
                                    </NavLink>
                                </li>
                                <li>
                                    <NavLink to="/researcher-dashboard/manage-studies" className="sidebar-nav-link">
                                        Manage Studies
                                    </NavLink>
                                </li>
                                <li>
                                    <NavLink to="/researcher-dashboard/manage-quizzes" className="sidebar-nav-link">
                                        Manage Quizzes
                                    </NavLink>
                                </li>
                                <li style={{marginTop:'1rem', marginBottom:'0.5rem', color:'#666', fontSize:'0.8rem', paddingLeft:'1rem', textTransform:'uppercase'}}>
                                    Assessments
                                </li>
                                <li>
                                    <NavLink to="/researcher-dashboard/create-quiz" className="sidebar-nav-link">
                                        Create Technical Quiz
                                    </NavLink>
                                </li>
                                <li>
                                    <NavLink to="/researcher-dashboard/create-questionnaire" className="sidebar-nav-link">
                                        Create Questionnaire
                                    </NavLink>
                                </li>
                                <li style={{marginTop:'1rem', marginBottom:'0.5rem', color:'#666', fontSize:'0.8rem', paddingLeft:'1rem', textTransform:'uppercase'}}>
                                    Artifacts
                                </li>
                                <li>
                                    <NavLink to="/researcher-dashboard/artifacts" className="sidebar-nav-link">
                                        Upload &amp; Organize
                                    </NavLink>
                                </li>
                            </>
                        ) : (
                            <>
                                <li>
                                    <NavLink to="/participant-dashboard" end className="sidebar-nav-link">
                                        Dashboard
                                    </NavLink>
                                </li>
                            </>
                        )}
                    </ul>
                </nav>

                {/* --- 3. Footer (Exit) --- */}
                <div className="sidebar-footer">
                    <button onClick={handleLogout} className="sidebar-logout-button">
                        Exit / Log out
                    </button>
                </div>
            </aside>

            {/* --- 4. Main Content --- */}
            <main className="dashboard-content">
                <Outlet />
            </main>
        </div>
    );
};

export default DashboardLayout;