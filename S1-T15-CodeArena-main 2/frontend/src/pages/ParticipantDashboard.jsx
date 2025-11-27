// File: src/pages/ParticipantDashboard.jsx
import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { api } from '../context/AuthContext';
import { Link } from 'react-router-dom';

// Reuse existing researcher styles for consistency
import './ResearcherDashboard.css';

const ParticipantDashboard = () => {
    const { user } = useAuth();
    const [tasks, setTasks] = useState([]);
    const [studies, setStudies] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        const fetchTasks = async () => {
            try {
                setLoading(true);
                setError(null);
                
                // 1. Fetch tasks (Response now includes 'blinded' boolean)
                const response = await api.get('/api/tasks/my-tasks');
                const taskData = response.data;
                setTasks(taskData);

                // 2. Extract unique studies for the "Quizzes" section
                const studyMap = new Map();
                taskData.forEach(task => {
                    // Only show study in quiz list if task is not completed yet
                    // This logic assumes 1 task per study for simplicity, 
                    // but handles multiple by deduplicating via Map.
                    if (!studyMap.has(task.studyId) && task.status !== 'COMPLETED') {
                        studyMap.set(task.studyId, {
                            id: task.studyId,
                            title: task.studyTitle
                        });
                    }
                });
                setStudies(Array.from(studyMap.values()));

            } catch (err) {
                console.error("Tasks loading error:", err);
                setError("Your tasks could not be loaded.");
            } finally {
                setLoading(false);
            }
        };
        fetchTasks();
    }, []);

    return (
        <div className="researcher-dashboard">
            <div className="dashboard-header">
                <h1>Participant Dashboard</h1>
                <p style={{marginTop: '-1.5rem', color: '#ccc'}}>Welcome, {user.name}!</p>
            </div>

            <div className="dashboard-grid">

                {/* --- Section 1: Competency Quizzes --- */}
                <div className="dashboard-card full-width">
                    <div className="card-header">
                        <h2>Competency Quizzes</h2>
                    </div>
                    <div className="card-content">
                        {loading && <p>Loading...</p>}
                        {!loading && !error && (
                            studies.length === 0 ? (
                                <p>No pending quizzes found.</p>
                            ) : (
                                <table className="artifact-list-table">
                                    <thead>
                                    <tr>
                                        <th>Study Title</th>
                                        <th>Action</th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    {studies.map(study => (
                                        <tr key={study.id}>
                                            <td>{study.title}</td>
                                            <td>
                                                <Link to={`/participant-dashboard/quiz/${study.id}`}>
                                                    <button className="upload-form" style={{width: 'auto', padding: '0.5rem 1rem'}}>
                                                        Take Quiz
                                                    </button>
                                                </Link>
                                            </td>
                                        </tr>
                                    ))}
                                    </tbody>
                                </table>
                            )
                        )}
                    </div>
                </div>

                {/* --- Section 2: Comparison Tasks --- */}
                <div className="dashboard-card full-width">
                    <div className="card-header">
                        <h2>My Comparison Tasks</h2>
                    </div>
                    <div className="card-content">
                        {loading && <p>Loading tasks...</p>}
                        {error && <p style={{ color: 'red' }}>{error}</p>}
                        
                        {!loading && !error && (
                            tasks.length === 0 ? (
                                <p>You have no comparison tasks assigned.</p>
                            ) : (
                                <table className="artifact-list-table">
                                    <thead>
                                    <tr>
                                        <th>Study</th>
                                        <th>Mode</th>
                                        <th>Status</th>
                                        <th>Artifact A</th>
                                        <th>Artifact B</th>
                                        <th>Action</th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    {tasks.map(task => {
                                        // 1. Determine Target URL based on blinded mode
                                        const evalLink = task.blinded 
                                            ? `/evaluation-blinded/${task.taskId}` 
                                            : `/evaluation/${task.taskId}`;
                                        
                                        // 2. Determine Button Style & Text
                                        let buttonText = 'Start Task';
                                        let buttonStyle = { width: 'auto', padding: '0.5rem 1rem' };

                                        if (task.status === 'IN_PROGRESS') {
                                            buttonText = 'Continue';
                                        } else if (task.status === 'COMPLETED') {
                                            buttonText = 'View';
                                            buttonStyle.backgroundColor = '#444'; // Grey for View mode
                                            buttonStyle.color = '#ccc';
                                        }

                                        return (
                                            <tr key={task.taskId}>
                                                <td>{task.studyTitle}</td>
                                                <td>
                                                    {task.blinded ? (
                                                        <span style={{color: '#8be28b'}}>Blinded</span>
                                                    ) : (
                                                        <span style={{color: '#aaa'}}>Standard</span>
                                                    )}
                                                </td>
                                                <td>
                                                    <span className={
                                                        task.status === 'COMPLETED' ? 'status-completed' : 
                                                        task.status === 'IN_PROGRESS' ? 'status-in-progress' : 
                                                        'status-pending'
                                                    }>
                                                        {task.status}
                                                    </span>
                                                </td>
                                                <td>{task.artifactA.fileName}</td>
                                                <td>{task.artifactB.fileName}</td>
                                                <td>
                                                    <Link to={evalLink}>
                                                        <button className="upload-form" style={buttonStyle}>
                                                            {buttonText}
                                                        </button>
                                                    </Link>
                                                </td>
                                            </tr>
                                        );
                                    })}
                                    </tbody>
                                </table>
                            )
                        )}
                    </div>
                </div>

            </div>
        </div>
    );
};

export default ParticipantDashboard;
