import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import './ResearcherDashboard.css';
import './ParticipantDashboard.css';
import ParticipantStudyCard from '../../components/ParticipantStudyCard';

const ParticipantDashboard = () => {
    const navigate = useNavigate();
    // Mock data for assigned studies (current and upcoming)
    const [assignedStudies] = useState([
        {
            id: 1,
            title: 'Code Quality Evaluation Study',
            status: 'In Progress',
            enrolledDate: '2024-01-20',
            deadline: '2024-03-15',
            totalTasks: 10,
            completedTasks: 7,
            pendingTasks: 3,
            progress: 70
        },
        {
            id: 2,
            title: 'API Design Comparison',
            status: 'In Progress',
            enrolledDate: '2024-02-05',
            deadline: '2024-04-01',
            totalTasks: 8,
            completedTasks: 2,
            pendingTasks: 6,
            progress: 25
        },
        {
            id: 3,
            title: 'Documentation Quality Assessment',
            status: 'Upcoming',
            enrolledDate: '2024-02-20',
            deadline: '2024-05-01',
            totalTasks: 5,
            completedTasks: 0,
            pendingTasks: 5,
            progress: 0
        }
    ]);


    // Mock data for completed studies (history)
    const [completedStudies] = useState([
        {
            id: 4,
            title: 'Performance Benchmark Study',
            completedDate: '2024-01-10',
            totalTasks: 6,
            completedTasks: 6,
            evaluationsContributed: 12
        },
        {
            id: 5,
            title: 'Security Analysis Study',
            completedDate: '2023-12-15',
            totalTasks: 4,
            completedTasks: 4,
            evaluationsContributed: 8
        }
    ]);

    // Mock data for notifications
    const [notifications] = useState([
        { 
            id: 1, 
            message: 'Task T001 deadline approaching (3 days remaining)', 
            type: 'warning', 
            studyId: 1,
            taskId: 'T001'
        },
        { 
            id: 2, 
            message: 'Task T004 deadline approaching (5 days remaining)', 
            type: 'info', 
            studyId: 2,
            taskId: 'T004'
        },
        { 
            id: 3, 
            message: 'New study "Documentation Quality Assessment" assigned', 
            type: 'info', 
            studyId: 3
        }
    ]);



    return (
        <div className="researcher-dashboard">
            <div className="dashboard-header">
                <h1>Participant Dashboard</h1>
                <p className="dashboard-subtitle">Track your assignments and study progress</p>
            </div>

            <div className="dashboard-grid">
                {/* Deadlines & Notifications */}
                <div className="dashboard-card full-width">
                    <div className="card-header">
                        <h2>Deadlines & Notifications</h2>
                    </div>
                    <div className="card-content participant-notifications-section">
                        {notifications.length === 0 ? (
                            <p className="empty-state">No new notifications</p>
                        ) : (
                            <div className="notifications-list">
                                {notifications.map(notification => (
                                    <div
                                        key={notification.id}
                                        className={`notification-item ${notification.type}`}
                                        style={{cursor: notification.taskId ? 'pointer' : 'default'}}
                                        onClick={() => {
                                            if (notification.taskId) {
                                                // Navigate to task - using mock taskId for now
                                                navigate(`/participant-dashboard/task/${notification.taskId}`);
                                            }
                                        }}
                                    >
                                        <p className="notification-message">{notification.message}</p>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                </div>

                {/* Assigned Studies List */}
                <div className="dashboard-card full-width">
                    <div className="card-header">
                        <h2>Assigned Studies</h2>
                    </div>
                    <div className="card-content participant-studies-section">
                        <div className="studies-grid">
                            {assignedStudies.length === 0 ? (
                                <p className="empty-state" style={{textAlign: 'center', padding: '2rem', color: '#999'}}>
                                    No assigned studies yet. You will be notified when new studies are assigned to you.
                                </p>
                            ) : (
                                assignedStudies.map(study => (
                                    <ParticipantStudyCard 
                                        key={study.id} 
                                        study={study}
                                        onEvaluate={(studyId) => {
                                            // Navigate to first pending task for this study
                                            // In real app, this would fetch actual tasks
                                            const mockTaskId = `T${studyId}01`;
                                            navigate(`/participant-dashboard/task/${mockTaskId}`);
                                        }}
                                    />
                                ))
                            )}
                        </div>
                    </div>
                </div>

                {/* Study History */}
                <div className="dashboard-card full-width">
                    <div className="card-header">
                        <h2>Study History</h2>
                    </div>
                    <div className="card-content participant-history-section">
                        {completedStudies.length === 0 ? (
                            <p className="empty-state">No completed studies yet</p>
                        ) : (
                            <div className="participant-history-grid">
                                {completedStudies.map(study => (
                                    <div key={study.id} className="participant-history-card">
                                        <h3 className="history-card-title">{study.title}</h3>
                                        <div className="history-card-date">
                                            Completed on {study.completedDate}
                                        </div>
                                        <div className="history-card-stats">
                                            <div className="history-stat-item">
                                                <span className="history-stat-label">Tasks</span>
                                                <span className="history-stat-value">{study.completedTasks}/{study.totalTasks}</span>
                                            </div>
                                            <div className="history-stat-item">
                                                <span className="history-stat-label">Evaluations</span>
                                                <span className="history-stat-value">{study.evaluationsContributed}</span>
                                            </div>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default ParticipantDashboard;
