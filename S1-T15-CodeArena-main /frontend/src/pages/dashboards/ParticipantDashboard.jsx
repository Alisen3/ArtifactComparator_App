import React, { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import './ResearcherDashboard.css';
import './ParticipantDashboard.css';
import ParticipantStudyCard from '../../components/ParticipantStudyCard';
import { api } from '../../context/AuthContext';

const normalizeInviteToken = (rawValue = '') => {
    const trimmed = rawValue.trim();
    if (!trimmed) {
        return '';
    }

    // Handle cases where users paste the entire invite URL
    try {
        const maybeUrl = new URL(trimmed);
        const fromQuery = maybeUrl.searchParams.get('token');
        if (fromQuery) {
            return fromQuery;
        }
    } catch (err) {
        // Not a full URL, fall through to manual parsing
    }

    const queryMatch = trimmed.match(/token=([^&]+)/i);
    if (queryMatch && queryMatch[1]) {
        return queryMatch[1];
    }

    return trimmed;
};

const ParticipantDashboard = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const [joinToken, setJoinToken] = useState('');
    const [joinMessage, setJoinMessage] = useState(null);
    const [joining, setJoining] = useState(false);
    const [assignedStudies, setAssignedStudies] = useState([]);
    const [completedStudies, setCompletedStudies] = useState([]);
    const [notifications, setNotifications] = useState([]);
    const [loading, setLoading] = useState(true);



    const handleJoinStudy = async (tokenOverride) => {
        const rawToken = tokenOverride || joinToken;
        const token = normalizeInviteToken(rawToken);
        if (!token.trim()) {
            setJoinMessage({ type: 'error', text: 'Enter an invite token to join a study.' });
            return;
        }
        // Reflect the normalized token in the input so users see what will be sent
        setJoinToken(token);
        setJoining(true);
        setJoinMessage(null);
        try {
            const response = await api.post(`/api/studies/invites/${token}/accept`);
            const pending = response.data?.pendingApproval;
            setJoinMessage({
                type: pending ? 'info' : 'success',
                text: response.data?.message || (pending ? 'Invite pending review.' : 'Invite accepted.')
            });
            if (!pending) {
                setJoinToken('');
            }
        } catch (err) {
            setJoinMessage({ type: 'error', text: err.response?.data?.message || 'Invite could not be accepted.' });
        } finally {
            setJoining(false);
        }
    };

    useEffect(() => {
        const fetchTasks = async () => {
            try {
                setLoading(true);
                const response = await api.get('/api/tasks/my-tasks');
                const tasks = response.data || [];
                
                // Group tasks by study
                const studyMap = new Map();
                
                tasks.forEach(task => {
                    const studyId = task.studyId;
                    if (!studyMap.has(studyId)) {
                        studyMap.set(studyId, {
                            id: studyId,
                            title: task.studyTitle,
                            tasks: [],
                            completedTasks: 0,
                            totalTasks: 0
                        });
                    }
                    const study = studyMap.get(studyId);
                    study.tasks.push(task);
                    study.totalTasks++;
                    if (task.status === 'COMPLETED') {
                        study.completedTasks++;
                    }
                });
                
                // Convert to arrays and separate active vs completed
                const allStudies = Array.from(studyMap.values());
                const active = allStudies.filter(s => s.completedTasks < s.totalTasks);
                const completed = allStudies.filter(s => s.completedTasks === s.totalTasks && s.totalTasks > 0);
                
                // Fetch study details to get deadlines
                const studyDetailsPromises = active.map(study => 
                    api.get(`/api/studies/${study.id}/permissions`).then(() => 
                        api.get('/api/studies/my-studies').then(res => 
                            res.data.find(s => s.id === study.id)
                        ).catch(() => null)
                    ).catch(() => null)
                );
                const studyDetails = await Promise.all(studyDetailsPromises);
                
                // Format assigned studies
                const formattedActive = active.map((study, idx) => {
                    const studyDetail = studyDetails[idx];
                    const deadline = studyDetail?.accessWindowEnd 
                        ? new Date(studyDetail.accessWindowEnd).toISOString().split('T')[0]
                        : null;
                    
                    return {
                        id: study.id,
                        title: study.title,
                        status: study.completedTasks > 0 ? 'In Progress' : 'Upcoming',
                        enrolledDate: study.tasks[0]?.createdAt ? new Date(study.tasks[0].createdAt).toISOString().split('T')[0] : new Date().toISOString().split('T')[0],
                        deadline: deadline,
                        totalTasks: study.totalTasks,
                        completedTasks: study.completedTasks,
                        pendingTasks: study.totalTasks - study.completedTasks,
                        progress: study.totalTasks > 0 ? Math.round((study.completedTasks / study.totalTasks) * 100) : 0
                    };
                });
                
                // Format completed studies
                const formattedCompleted = completed.map(study => ({
                    id: study.id,
                    title: study.title,
                    completedDate: study.tasks[0]?.completedAt ? new Date(study.tasks[0].completedAt).toISOString().split('T')[0] : new Date().toISOString().split('T')[0],
                    totalTasks: study.totalTasks,
                    completedTasks: study.completedTasks,
                    evaluationsContributed: study.completedTasks * 2 // Assuming 2 evaluations per task
                }));
                
                setAssignedStudies(formattedActive);
                setCompletedStudies(formattedCompleted);
                
                // Generate notifications from tasks with deadline awareness
                const studyDeadlines = new Map();
                studyDetails.forEach((detail, idx) => {
                    if (detail && active[idx]) {
                        studyDeadlines.set(active[idx].id, detail.accessWindowEnd);
                    }
                });
                
                const taskNotifications = tasks
                    .filter(t => t.status !== 'COMPLETED')
                    .map(task => {
                        const deadline = studyDeadlines.get(task.studyId);
                        const daysUntilDeadline = deadline 
                            ? Math.ceil((new Date(deadline) - new Date()) / (1000 * 60 * 60 * 24))
                            : null;
                        
                        let message = `Task ${task.taskId} for "${task.studyTitle}" is pending`;
                        let type = 'info';
                        
                        if (daysUntilDeadline !== null) {
                            if (daysUntilDeadline < 0) {
                                message = `Task ${task.taskId} for "${task.studyTitle}" deadline passed (${Math.abs(daysUntilDeadline)} days ago)`;
                                type = 'error';
                            } else if (daysUntilDeadline <= 3) {
                                message = `Task ${task.taskId} for "${task.studyTitle}" deadline approaching (${daysUntilDeadline} day${daysUntilDeadline !== 1 ? 's' : ''} remaining)`;
                                type = 'warning';
                            } else if (daysUntilDeadline <= 7) {
                                message = `Task ${task.taskId} for "${task.studyTitle}" deadline in ${daysUntilDeadline} days`;
                                type = 'info';
                            }
                        }
                        
                        return {
                            id: task.taskId,
                            message: message,
                            type: type,
                            studyId: task.studyId,
                            taskId: task.taskId,
                            daysUntilDeadline: daysUntilDeadline
                        };
                    })
                    .sort((a, b) => {
                        // Sort by urgency: errors first, then warnings, then by days until deadline
                        if (a.type === 'error' && b.type !== 'error') return -1;
                        if (b.type === 'error' && a.type !== 'error') return 1;
                        if (a.type === 'warning' && b.type !== 'warning') return -1;
                        if (b.type === 'warning' && a.type !== 'warning') return 1;
                        if (a.daysUntilDeadline !== null && b.daysUntilDeadline !== null) {
                            return a.daysUntilDeadline - b.daysUntilDeadline;
                        }
                        return 0;
                    })
                    .slice(0, 5); // Show top 5 most urgent
                
                setNotifications(taskNotifications);
            } catch (err) {
                console.error('Error fetching tasks:', err);
            } finally {
                setLoading(false);
            }
        };
        
        fetchTasks();
    }, []);
    
    useEffect(() => {
        const params = new URLSearchParams(location.search);
        const token = params.get('token');
        if (token) {
            setJoinToken(token);
            handleJoinStudy(token);
        }
    // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [location.search]);

    if (loading) {
        return (
            <div className="researcher-dashboard">
                <div className="dashboard-header">
                    <h1>Participant Dashboard</h1>
                    <p className="dashboard-subtitle">Track your assignments and study progress</p>
                </div>
                <div className="dashboard-card full-width">
                    <div className="card-content">
                        <p style={{textAlign: 'center', color: '#999', padding: '2rem'}}>
                            Loading your dashboard...
                        </p>
                    </div>
                </div>
            </div>
        );
    }
    
    return (
        <div className="researcher-dashboard">
            <div className="dashboard-header">
                <h1>Participant Dashboard</h1>
                <p className="dashboard-subtitle">Track your assignments and study progress</p>
            </div>

            <div className="dashboard-grid">
                <div className="dashboard-card full-width">
                    <div className="card-header">
                        <h2>Join with Invite</h2>
                    </div>
                    <div className="card-content">
                        <div className="form-grid">
                            <label className="form-label">Invite Token</label>
                            <input
                                className="form-input"
                                value={joinToken}
                                onChange={(e) => setJoinToken(e.target.value)}
                                placeholder="Paste invite token here"
                            />
                        </div>
                        <button
                            className="form-button form-button-submit"
                            style={{ width: 'auto', marginTop: '0.75rem' }}
                            onClick={() => handleJoinStudy()}
                            disabled={joining}
                        >
                            {joining ? 'Joining...' : 'Join Study'}
                        </button>
                        {joinMessage && (
                            <div className={`form-message ${joinMessage.type}`} style={{ marginTop: '0.75rem' }}>
                                {joinMessage.text}
                            </div>
                        )}
                    </div>
                </div>

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
                                            // Find first pending task for this study
                                            const study = assignedStudies.find(s => s.id === studyId);
                                            if (study && study.pendingTasks > 0) {
                                                // Fetch tasks to get actual task ID
                                                api.get('/api/tasks/my-tasks')
                                                    .then(response => {
                                                        const tasks = response.data || [];
                                                        const pendingTask = tasks.find(t => 
                                                            t.studyId === studyId && t.status !== 'COMPLETED'
                                                        );
                                                        if (pendingTask) {
                                                            navigate(`/participant-dashboard/task/${pendingTask.taskId}`);
                                                        }
                                                    })
                                                    .catch(err => {
                                                        console.error('Error fetching tasks:', err);
                                                    });
                                            }
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
