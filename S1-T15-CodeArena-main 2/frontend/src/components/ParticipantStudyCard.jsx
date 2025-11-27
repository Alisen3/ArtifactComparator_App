import React from 'react';
import '../pages/dashboards/ResearcherDashboard.css';
import './ParticipantStudyCard.css';

// Get color based on completion percentage
const getCompletionColor = (percentage) => {
    if (percentage >= 70) {
        return '#4CAF50'; // Green for high completion
    } else if (percentage >= 40) {
        return '#FF9800'; // Orange for medium completion
    } else {
        return '#f44336'; // Red for low completion
    }
};

// Pie chart component
const PieChart = ({ percentage }) => {
    const radius = 50;
    const circumference = 2 * Math.PI * radius;
    const offset = circumference - (percentage / 100) * circumference;
    const chartColor = getCompletionColor(percentage);
    
    return (
        <div className="pie-chart-container">
            <svg width="120" height="120" className="pie-chart">
                <circle
                    cx="60"
                    cy="60"
                    r={radius}
                    fill="none"
                    stroke="#444"
                    strokeWidth="12"
                />
                <circle
                    cx="60"
                    cy="60"
                    r={radius}
                    fill="none"
                    stroke={chartColor}
                    strokeWidth="12"
                    strokeDasharray={circumference}
                    strokeDashoffset={offset}
                    strokeLinecap="round"
                    transform="rotate(-90 60 60)"
                />
            </svg>
            <div className="pie-chart-label">
                <span className="pie-chart-percentage" style={{ color: chartColor }}>
                    {percentage}%
                </span>
            </div>
        </div>
    );
};

const ParticipantStudyCard = ({ study, onEvaluate }) => {
    const completionPercentage = study.totalTasks > 0 
        ? Math.round((study.completedTasks / study.totalTasks) * 100) 
        : 0;
    
    const hasStarted = study.completedTasks > 0;
    const buttonText = hasStarted ? 'Continue Evaluating' : 'Start Evaluating';

    return (
        <div className="participant-study-card">
            <h2 className="participant-study-card-title">{study.title}</h2>
            <div className="participant-study-card-content">
                <div className="participant-study-card-left">
                    <PieChart percentage={completionPercentage} />
                </div>
                <div className="participant-study-card-right">
                    <div className="participant-study-info">
                        <div className="participant-study-stat">
                            <span className="participant-study-stat-label">Tasks</span>
                            <span className="participant-study-stat-value">
                                {study.completedTasks}/{study.totalTasks}
                            </span>
                        </div>
                        <div className="participant-study-stat">
                            <span className="participant-study-stat-label">Due Date</span>
                            <span className="participant-study-stat-value">{study.deadline}</span>
                        </div>
                    </div>
                    <button 
                        className="participant-evaluate-button"
                        onClick={() => onEvaluate && onEvaluate(study.id)}
                    >
                        {buttonText}
                    </button>
                </div>
            </div>
        </div>
    );
};

export default ParticipantStudyCard;

