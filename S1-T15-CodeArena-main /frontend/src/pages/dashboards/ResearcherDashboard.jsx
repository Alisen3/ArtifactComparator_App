import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import './ResearcherDashboard.css';
import StudyCard from '../../components/StudyCard';
import { api } from '../../context/AuthContext';

const ResearcherDashboard = () => {
    const navigate = useNavigate();
    const [studies, setStudies] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    
    useEffect(() => {
        const fetchStudies = async () => {
            try {
                setLoading(true);
                const response = await api.get('/api/studies/my-studies');
                const studiesData = response.data;
                
                // Fetch tasks for each study to calculate stats
                const studiesWithStats = await Promise.all(
                    studiesData.map(async (study) => {
                        try {
                            const tasksResponse = await api.get(`/api/studies/${study.id}/tasks`);
                            const tasks = tasksResponse.data || [];
                            
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
                                completionPercentage
                            };
                        } catch (err) {
                            console.error(`Error fetching tasks for study ${study.id}:`, err);
                            return {
                                id: study.id,
                                title: study.title,
                                participantCount: 0,
                                taskCount: 0,
                                completionPercentage: 0
                            };
                        }
                    })
                );
                
                setStudies(studiesWithStats);
            } catch (err) {
                console.error('Error fetching studies:', err);
                setError('Failed to load studies. Please try again.');
            } finally {
                setLoading(false);
            }
        };
        
        fetchStudies();
    }, []);

    const handleCardClick = (studyId) => {
        navigate(`/researcher-dashboard/study/${studyId}`);
    };

    return (
        <div className="researcher-dashboard">
            <div className="dashboard-header">
                <h1>Researcher Dashboard</h1>
                <p className="dashboard-subtitle">Manage your studies and track participant progress</p>
            </div>

            {/* Quick Actions */}
            <div className="dashboard-actions" style={{marginBottom: '2rem', display: 'flex', gap: '1rem', flexWrap: 'wrap'}}>
                <button 
                    className="form-button form-button-primary"
                    onClick={() => navigate('/researcher-dashboard/manage-studies')}
                    style={{width: 'auto'}}
                >
                    + Create New Study
                </button>
                <button 
                    className="form-button form-button-secondary"
                    onClick={() => navigate('/researcher-dashboard/manage-quizzes')}
                    style={{width: 'auto'}}
                >
                    Manage Quizzes
                </button>
                <button 
                    className="form-button form-button-secondary"
                    onClick={() => navigate('/researcher-dashboard/artifacts')}
                    style={{width: 'auto'}}
                >
                    Upload Artifacts
                </button>
            </div>

            {loading ? (
                <div className="dashboard-card full-width">
                    <div className="card-content">
                        <p style={{textAlign: 'center', color: '#999', padding: '2rem'}}>
                            Loading studies...
                        </p>
                    </div>
                </div>
            ) : error ? (
                <div className="dashboard-card full-width">
                    <div className="card-content">
                        <p style={{textAlign: 'center', color: '#f44336', padding: '2rem'}}>
                            {error}
                        </p>
                    </div>
                </div>
            ) : (
                <div className="studies-grid">
                    {studies.length === 0 ? (
                        <div className="dashboard-card full-width">
                            <div className="card-content">
                                <p style={{textAlign: 'center', color: '#999', padding: '2rem'}}>
                                    No studies yet. Create your first study to get started.
                                </p>
                            </div>
                        </div>
                    ) : (
                        studies.map(study => (
                            <StudyCard 
                                key={study.id} 
                                study={study}
                                onClick={() => handleCardClick(study.id)}
                            />
                        ))
                    )}
                </div>
            )}
        </div>
    );
};

export default ResearcherDashboard;
