// Dosya Yolu: src/pages/Participants.jsx
import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../context/AuthContext';
import './dashboards/ResearcherDashboard.css';
import './Forms.css';

const Participants = () => {
    const navigate = useNavigate();
    const [participants, setParticipants] = useState([]);
    const [filteredParticipants, setFilteredParticipants] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [selectedParticipants, setSelectedParticipants] = useState(new Set());
    const [studies, setStudies] = useState([]);
    const [quizzes, setQuizzes] = useState([]);
    const [questionnaires, setQuestionnaires] = useState([]);
    
    // Filter state
    const [filters, setFilters] = useState({
        minQuizScores: {}, // { quizId: minScore }
        questionnaireAnswers: {}, // { questionId: answer }
        experienceLevel: '',
        minYearsOfExperience: '',
        skills: ''
    });

    // Fetch all data on component mount
    useEffect(() => {
        fetchAllData();
    }, []);

    // Apply filters when filters or participants change
    useEffect(() => {
        applyFilters();
    }, [filters, participants]);

    const fetchAllData = async () => {
        try {
            setLoading(true);
            setError(null);

            // Fetch participants with scores
            const participantsResponse = await api.get('/api/participants');
            setParticipants(participantsResponse.data);
            setFilteredParticipants(participantsResponse.data);

            // Fetch studies to get quiz/questionnaire IDs
            const studiesResponse = await api.get('/api/studies/my-studies');
            setStudies(studiesResponse.data);

            // Fetch quizzes
            const quizzesResponse = await api.get('/api/quizzes/my-quizzes');
            setQuizzes(quizzesResponse.data);

            // Extract questionnaires from studies (they're also quizzes)
            const allQuizzes = [...quizzesResponse.data];
            setQuestionnaires(allQuizzes);

        } catch (err) {
            console.error('Error fetching data:', err);
            setError(err.response?.data?.message || err.message || 'Failed to load participants.');
        } finally {
            setLoading(false);
        }
    };

    const applyFilters = async () => {
        try {
            // Build filter request
            const filterRequest = {
                minQuizScores: Object.keys(filters.minQuizScores).length > 0 
                    ? Object.fromEntries(
                        Object.entries(filters.minQuizScores)
                            .filter(([_, value]) => value !== '' && value != null)
                            .map(([key, value]) => [parseInt(key), parseFloat(value)])
                    )
                    : null,
                questionnaireAnswers: Object.keys(filters.questionnaireAnswers).length > 0
                    ? Object.fromEntries(
                        Object.entries(filters.questionnaireAnswers)
                            .filter(([_, value]) => value !== '' && value != null)
                            .map(([key, value]) => [parseInt(key), value])
                    )
                    : null,
                experienceLevel: filters.experienceLevel || null,
                minYearsOfExperience: filters.minYearsOfExperience 
                    ? parseInt(filters.minYearsOfExperience) 
                    : null,
                skills: filters.skills || null
            };

            // Remove null values
            Object.keys(filterRequest).forEach(key => {
                if (filterRequest[key] === null || 
                    (typeof filterRequest[key] === 'object' && Object.keys(filterRequest[key]).length === 0)) {
                    delete filterRequest[key];
                }
            });

            // If no filters, show all participants
            if (Object.keys(filterRequest).length === 0) {
                setFilteredParticipants(participants);
                return;
            }

            // Apply filters via API
            const response = await api.post('/api/participants/filter', filterRequest);
            setFilteredParticipants(response.data);
        } catch (err) {
            console.error('Error filtering participants:', err);
            setError(err.response?.data?.message || err.message || 'Failed to filter participants.');
        }
    };

    const handleFilterChange = (filterType, key, value) => {
        setFilters(prev => {
            const newFilters = { ...prev };
            if (filterType === 'minQuizScores') {
                newFilters.minQuizScores = { ...prev.minQuizScores, [key]: value };
            } else if (filterType === 'questionnaireAnswers') {
                newFilters.questionnaireAnswers = { ...prev.questionnaireAnswers, [key]: value };
            } else {
                newFilters[filterType] = value;
            }
            return newFilters;
        });
    };

    const removeFilter = (filterType, key) => {
        setFilters(prev => {
            const newFilters = { ...prev };
            if (filterType === 'minQuizScores') {
                const { [key]: removed, ...rest } = prev.minQuizScores;
                newFilters.minQuizScores = rest;
            } else if (filterType === 'questionnaireAnswers') {
                const { [key]: removed, ...rest } = prev.questionnaireAnswers;
                newFilters.questionnaireAnswers = rest;
            }
            return newFilters;
        });
    };

    const toggleParticipantSelection = (participantId) => {
        setSelectedParticipants(prev => {
            const newSet = new Set(prev);
            if (newSet.has(participantId)) {
                newSet.delete(participantId);
            } else {
                newSet.add(participantId);
            }
            return newSet;
        });
    };

    const selectAll = () => {
        setSelectedParticipants(new Set(filteredParticipants.map(p => p.id)));
    };

    const deselectAll = () => {
        setSelectedParticipants(new Set());
    };

    const handleInviteSelected = async (studyId) => {
        if (selectedParticipants.size === 0) {
            alert('Please select at least one participant.');
            return;
        }

        if (!studyId) {
            alert('Please select a study.');
            return;
        }

        try {
            // Get emails for selected participants
            const selectedEmails = filteredParticipants
                .filter(p => selectedParticipants.has(p.id))
                .map(p => p.email)
                .filter(email => email);

            // Send invites
            for (const email of selectedEmails) {
                try {
                    await api.post(`/api/studies/${studyId}/invites`, {
                        email: email,
                        expiresInHours: 72,
                        shareableLink: false
                    });
                } catch (err) {
                    console.error(`Failed to invite ${email}:`, err);
                }
            }

            alert(`Invites sent to ${selectedEmails.length} participant(s).`);
            setSelectedParticipants(new Set());
        } catch (err) {
            console.error('Error sending invites:', err);
            alert('Failed to send invites. Please try again.');
        }
    };

    // Get all unique question IDs from participants' questionnaire answers
    const getAllQuestionIds = () => {
        const questionIds = new Set();
        participants.forEach(participant => {
            Object.keys(participant.questionnaireAnswers || {}).forEach(qId => {
                questionIds.add(parseInt(qId));
            });
        });
        return Array.from(questionIds);
    };

    return (
        <div className="researcher-dashboard">
            <div className="dashboard-header">
                <h1>Participants</h1>
                <p className="dashboard-subtitle">Filter and invite participants to your studies</p>
            </div>

            {loading ? (
                <div className="dashboard-card full-width">
                    <div className="card-content">
                        <p style={{ textAlign: 'center', color: '#999', padding: '2rem' }}>
                            Loading participants...
                        </p>
                    </div>
                </div>
            ) : error ? (
                <div className="dashboard-card full-width">
                    <div className="card-content">
                        <p style={{ textAlign: 'center', color: '#f44336', padding: '2rem' }}>
                            {error}
                        </p>
                    </div>
                </div>
            ) : (
                <>
                    {/* Filters Section */}
                    <div className="dashboard-card full-width" style={{ marginBottom: '2rem' }}>
                        <div className="card-header">
                            <h2>Filters</h2>
                        </div>
                        <div className="card-content">
                            <div className="form-grid" style={{ gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                                {/* Quiz Score Filters */}
                                <div>
                                    <label className="form-label">Quiz Score Filters</label>
                                    {quizzes.map(quiz => (
                                        <div key={quiz.id} style={{ marginBottom: '0.5rem', display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                                            <input
                                                type="number"
                                                className="form-input"
                                                placeholder={`${quiz.title} min %`}
                                                min="0"
                                                max="100"
                                                value={filters.minQuizScores[quiz.id] || ''}
                                                onChange={(e) => handleFilterChange('minQuizScores', quiz.id, e.target.value)}
                                                style={{ flex: 1 }}
                                            />
                                            {filters.minQuizScores[quiz.id] && (
                                                <button
                                                    className="form-button-secondary"
                                                    onClick={() => removeFilter('minQuizScores', quiz.id)}
                                                    style={{ padding: '0.5rem', fontSize: '0.8rem' }}
                                                >
                                                    ×
                                                </button>
                                            )}
                                        </div>
                                    ))}
                                </div>

                                {/* Questionnaire Answer Filters */}
                                <div>
                                    <label className="form-label">Questionnaire Answer Filters</label>
                                    {getAllQuestionIds().map(questionId => (
                                        <div key={questionId} style={{ marginBottom: '0.5rem', display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                                            <input
                                                type="text"
                                                className="form-input"
                                                placeholder={`Question ${questionId} answer`}
                                                value={filters.questionnaireAnswers[questionId] || ''}
                                                onChange={(e) => handleFilterChange('questionnaireAnswers', questionId, e.target.value)}
                                                style={{ flex: 1 }}
                                            />
                                            {filters.questionnaireAnswers[questionId] && (
                                                <button
                                                    className="form-button-secondary"
                                                    onClick={() => removeFilter('questionnaireAnswers', questionId)}
                                                    style={{ padding: '0.5rem', fontSize: '0.8rem' }}
                                                >
                                                    ×
                                                </button>
                                            )}
                                        </div>
                                    ))}
                                </div>

                                {/* Experience Level Filter */}
                                <div>
                                    <label className="form-label">Experience Level</label>
                                    <input
                                        type="text"
                                        className="form-input"
                                        placeholder="e.g., Senior Developer"
                                        value={filters.experienceLevel}
                                        onChange={(e) => handleFilterChange('experienceLevel', null, e.target.value)}
                                    />
                                </div>

                                {/* Min Years of Experience */}
                                <div>
                                    <label className="form-label">Min Years of Experience</label>
                                    <input
                                        type="number"
                                        className="form-input"
                                        placeholder="e.g., 5"
                                        min="0"
                                        value={filters.minYearsOfExperience}
                                        onChange={(e) => handleFilterChange('minYearsOfExperience', null, e.target.value)}
                                    />
                                </div>

                                {/* Skills Filter */}
                                <div style={{ gridColumn: '1 / -1' }}>
                                    <label className="form-label">Skills (comma-separated)</label>
                                    <input
                                        type="text"
                                        className="form-input"
                                        placeholder="e.g., Java, Python, React"
                                        value={filters.skills}
                                        onChange={(e) => handleFilterChange('skills', null, e.target.value)}
                                    />
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* Participants List */}
                    <div className="dashboard-card full-width">
                        <div className="card-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                            <h2>Participants ({filteredParticipants.length})</h2>
                            <div style={{ display: 'flex', gap: '0.5rem' }}>
                                <button
                                    className="form-button-secondary"
                                    onClick={selectAll}
                                    style={{ padding: '0.5rem 1rem', fontSize: '0.9rem' }}
                                >
                                    Select All
                                </button>
                                <button
                                    className="form-button-secondary"
                                    onClick={deselectAll}
                                    style={{ padding: '0.5rem 1rem', fontSize: '0.9rem' }}
                                >
                                    Deselect All
                                </button>
                            </div>
                        </div>
                        <div className="card-content">
                            {filteredParticipants.length === 0 ? (
                                <p style={{ textAlign: 'center', color: '#999', padding: '2rem' }}>
                                    No participants match the filter criteria.
                                </p>
                            ) : (
                                <>
                                    <table className="artifact-list-table">
                                        <thead>
                                            <tr>
                                                <th style={{ width: '40px' }}>
                                                    <input
                                                        type="checkbox"
                                                        checked={selectedParticipants.size === filteredParticipants.length && filteredParticipants.length > 0}
                                                        onChange={(e) => e.target.checked ? selectAll() : deselectAll()}
                                                    />
                                                </th>
                                                <th>Name</th>
                                                <th>Email</th>
                                                <th>Skills</th>
                                                <th>Years of Experience</th>
                                                <th>Quiz Scores</th>
                                                <th>Questionnaire Answers</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {filteredParticipants.map(participant => (
                                                <tr key={participant.id}>
                                                    <td>
                                                        <input
                                                            type="checkbox"
                                                            checked={selectedParticipants.has(participant.id)}
                                                            onChange={() => toggleParticipantSelection(participant.id)}
                                                        />
                                                    </td>
                                                    <td>{participant.name}</td>
                                                    <td>{participant.email}</td>
                                                    <td>{participant.skills || 'N/A'}</td>
                                                    <td>{participant.yearsOfExperience ?? 'N/A'}</td>
                                                    <td>
                                                        {Object.keys(participant.quizScores || {}).length > 0 ? (
                                                            <ul style={{ margin: 0, paddingLeft: '1.2rem', fontSize: '0.9rem' }}>
                                                                {Object.entries(participant.quizScores).map(([quizId, score]) => {
                                                                    const quiz = quizzes.find(q => q.id === parseInt(quizId));
                                                                    return (
                                                                        <li key={quizId}>
                                                                            {quiz ? quiz.title : `Quiz ${quizId}`}: {score.toFixed(2)}%
                                                                        </li>
                                                                    );
                                                                })}
                                                            </ul>
                                                        ) : 'No scores'}
                                                    </td>
                                                    <td>
                                                        {Object.keys(participant.questionnaireAnswers || {}).length > 0 ? (
                                                            <ul style={{ margin: 0, paddingLeft: '1.2rem', fontSize: '0.9rem' }}>
                                                                {Object.entries(participant.questionnaireAnswers).map(([qId, answer]) => (
                                                                    <li key={qId}>
                                                                        Q{qId}: {answer}
                                                                    </li>
                                                                ))}
                                                            </ul>
                                                        ) : 'No answers'}
                                                    </td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>

                                    {/* Invite Selected Button */}
                                    {selectedParticipants.size > 0 && (
                                        <div style={{ marginTop: '1.5rem', padding: '1rem', backgroundColor: '#1a1a1a', borderRadius: '8px' }}>
                                            <label className="form-label">Invite Selected ({selectedParticipants.size}) to Study:</label>
                                            <div style={{ display: 'flex', gap: '1rem', alignItems: 'center' }}>
                                                <select
                                                    className="form-select"
                                                    style={{ flex: 1 }}
                                                    onChange={(e) => {
                                                        if (e.target.value) {
                                                            handleInviteSelected(parseInt(e.target.value));
                                                            e.target.value = '';
                                                        }
                                                    }}
                                                >
                                                    <option value="">Select a study...</option>
                                                    {studies.map(study => (
                                                        <option key={study.id} value={study.id}>
                                                            {study.title}
                                                        </option>
                                                    ))}
                                                </select>
                                            </div>
                                        </div>
                                    )}
                                </>
                            )}
                        </div>
                    </div>
                </>
            )}
        </div>
    );
};

export default Participants;

