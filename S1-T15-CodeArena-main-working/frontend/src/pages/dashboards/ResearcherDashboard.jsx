import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import './ResearcherDashboard.css';
import StudyCard from '../../components/StudyCard';

const ResearcherDashboard = () => {
    const navigate = useNavigate();
    
    // Mock data for studies (researcher's own studies)
    const [studies] = useState([
        {
            id: 1,
            title: 'Code Quality Evaluation Study',
            participantCount: 25,
            taskCount: 85,
            completionPercentage: 72
        },
        {
            id: 2,
            title: 'API Design Comparison',
            participantCount: 18,
            taskCount: 52,
            completionPercentage: 89
        },
        {
            id: 3,
            title: 'Documentation Quality Assessment',
            participantCount: 32,
            taskCount: 120,
            completionPercentage: 45
        },
        {
            id: 4,
            title: 'Performance Benchmark Study',
            participantCount: 15,
            taskCount: 60,
            completionPercentage: 95
        }
    ]);

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
        </div>
    );
};

export default ResearcherDashboard;
