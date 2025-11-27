import React, { useState } from 'react';
import './StudyCard.css';

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

const StudyCard = ({ study, onClick, showSettingsMenu = false, onDelete }) => {
    const [isMenuOpen, setIsMenuOpen] = useState(false);

    const handleCardClick = () => {
        if (onClick) {
            onClick(study.id);
        }
    };

    const handleSettingsClick = (event) => {
        event.stopPropagation();
        setIsMenuOpen((prev) => !prev);
    };

    const handleDeleteClick = (event) => {
        event.stopPropagation();
        setIsMenuOpen(false);
        if (onDelete) {
            onDelete(study.id);
        } else {
            alert(`Delete ${study.title} (Mock functionality)`);
        }
    };

    return (
        <div 
            className="study-card"
            onClick={handleCardClick}
        >
            <div className="study-card-header">
                <div className="study-card-header-info">
                    <h2 className="study-card-title">{study.title}</h2>
                    {study.researcherName && (
                        <p className="study-card-researcher">
                            Created by {study.researcherName}
                        </p>
                    )}
                </div>
                {showSettingsMenu && (
                    <div className="study-card-menu-wrapper">
                        <button
                            type="button"
                            className="study-card-settings-btn"
                            onClick={handleSettingsClick}
                            aria-haspopup="true"
                            aria-expanded={isMenuOpen}
                            aria-label="Open study actions"
                        >
                            &#8942;
                        </button>
                        {isMenuOpen && (
                            <div className="study-card-menu" role="menu">
                                <button
                                    type="button"
                                    className="study-card-menu-item"
                                    role="menuitem"
                                    onClick={handleDeleteClick}
                                >
                                    Delete
                                </button>
                            </div>
                        )}
                    </div>
                )}
            </div>
            <div className="study-card-content">
                <div className="study-card-left">
                    <PieChart percentage={study.completionPercentage} />
                </div>
                <div className="study-card-right">
                    <div className="study-stat">
                        <span className="study-stat-label">Tasks</span>
                        <span className="study-stat-value">{study.taskCount}</span>
                    </div>
                    <div className="study-stat">
                        <span className="study-stat-label">Participants</span>
                        <span className="study-stat-value">{study.participantCount}</span>
                    </div>
                    <div className="study-stat">
                        <span className="study-stat-label">Completion</span>
                        <span className="study-stat-value">{study.completionPercentage}%</span>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default StudyCard;

