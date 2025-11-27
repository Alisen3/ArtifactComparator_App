import React from 'react';
import { Outlet, NavLink } from 'react-router-dom';
import './DashboardLayout.css';
import CodeArenaLogo from '../assets/CodeArenaLogo.png';

const TestDashboardLayout = () => {
    return (
        <div className="dashboard-layout">
            {/* --- 1. Sol Sidebar --- */}
            <aside className="sidebar">
                <div className="sidebar-header">
                    <img src={CodeArenaLogo} alt="CodeArena Logo" className="sidebar-logo" />
                    <h1>CodeArena</h1>
                </div>

                {/* --- 2. Navigasyon Linkleri --- */}
                <nav className="sidebar-nav">
                    <ul className="sidebar-nav-list">
                        <li>
                            <NavLink to="/test/dashboard/admin" className="sidebar-nav-link">
                                Admin Dashboard
                            </NavLink>
                        </li>
                        <li>
                            <NavLink to="/test/dashboard/researcher" className="sidebar-nav-link">
                                Researcher Dashboard
                            </NavLink>
                        </li>
                        <li>
                            <NavLink to="/test/dashboard/participant" className="sidebar-nav-link">
                                Participant Dashboard
                            </NavLink>
                        </li>
                    </ul>
                </nav>

                {/* --- 3. Footer (Çıkış Butonu) --- */}
                <div className="sidebar-footer">
                    <NavLink to="/login" className="sidebar-logout-button" style={{ textDecoration: 'none', display: 'block' }}>
                        Exit / Log out
                    </NavLink>
                </div>
            </aside>

            {/* --- 4. Ana İçerik --- */}
            <main className="dashboard-content">
                <Outlet />
            </main>
        </div>
    );
};

export default TestDashboardLayout;

