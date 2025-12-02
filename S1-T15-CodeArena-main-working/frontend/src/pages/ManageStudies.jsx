// Dosya Yolu: src/pages/ManageStudies.jsx
// (TAM VE GÜNCELLENMİŞ KOD - Yeni Tasarım)

import React, { useState, useEffect } from 'react';
import { api } from '../context/AuthContext';
import { Link } from 'react-router-dom';

// 1. Adım: Mevcut CSS'lerimizi import et
import './dashboards/ResearcherDashboard.css'; // Kart stilleri için
import './Forms.css'; // Form (select, button) stilleri için

const emptyStudyForm = {
    title: '',
    description: '',
    blinded: false,
    accessWindowStart: '',
    accessWindowEnd: ''
};

const toIsoString = (value) => value ? new Date(value).toISOString() : null;
const formatDateTimeLocal = (value) => value ? new Date(value).toISOString().slice(0, 16) : '';
const formatDisplayDate = (value) => value ? new Date(value).toLocaleString() : 'Not set';

const ManageStudies = () => {
    // --- (LOGIC BÖLÜMÜ DEĞİŞMEDİ) ---
    const [studies, setStudies] = useState([]);
    const [quizzes, setQuizzes] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [message, setMessage] = useState(null);
    const [newStudyForm, setNewStudyForm] = useState(emptyStudyForm);
    const [editingStudyId, setEditingStudyId] = useState(null);
    const [editForm, setEditForm] = useState(emptyStudyForm);
    const [publishLoadingId, setPublishLoadingId] = useState(null);
    const [expandedStudyId, setExpandedStudyId] = useState(null);

    const formatRole = (role) => {
        switch (role) {
            case 'OWNER':
                return 'Owner';
            case 'EDITOR':
                return 'Editor';
            case 'REVIEWER':
                return 'Reviewer';
            case 'VIEWER':
                return 'Viewer';
            default:
                return role || 'Viewer';
        }
    };

    const fetchData = async () => {
        setLoading(true);
        setError(null);
        try {
            const [studiesResponse, quizzesResponse] = await Promise.all([
                api.get('/api/studies/my-studies'),
                api.get('/api/quizzes/my-quizzes')
            ]);
            setStudies(studiesResponse.data);
            setQuizzes(quizzesResponse.data);
        } catch (err) {
            console.error("Data loading error:", err);
            setError("Studies or quizzes could not be loaded.");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchData();
    }, []);

    const handleAssignQuiz = async (studyId, quizId) => {
        if (!quizId) {
            setMessage({ type: 'error', text: 'Please select a quiz.' });
            return;
        }
        setMessage({ type: 'info', text: 'Assigning quiz...' });
        try {
            const response = await api.post(`/api/studies/${studyId}/assign-quiz`, {
                quizId: quizId
            });
            setMessage({ type: 'success', text: response.data.message });
            fetchData(); // Listeyi yenile
        } catch (err) {
            console.error("Quiz assign error:", err);
            setMessage({ type: 'error', text: 'Could not assign quiz. (' + (err.response?.data?.message || err.message) + ')' });
        }
    };
    const handleCreateStudy = async (e) => {
        e.preventDefault();
        setMessage(null);
        try {
            await api.post('/api/studies', {
                title: newStudyForm.title,
                description: newStudyForm.description,
                blinded: newStudyForm.blinded,
                accessWindowStart: toIsoString(newStudyForm.accessWindowStart),
                accessWindowEnd: toIsoString(newStudyForm.accessWindowEnd)
            });
            setMessage({ type: 'success', text: 'Study created successfully.' });
            setNewStudyForm(emptyStudyForm);
            fetchData();
        } catch (err) {
            const errors = err.response?.data?.errors;
            setMessage({ type: 'error', text: errors ? errors.join(' ') : (err.response?.data?.message || 'Could not create study.') });
        }
    };

    const openEditForm = (study) => {
        setEditingStudyId(study.id);
        setEditForm({
            title: study.title || '',
            description: study.description || '',
            blinded: study.blinded || false,
            accessWindowStart: formatDateTimeLocal(study.accessWindowStart),
            accessWindowEnd: formatDateTimeLocal(study.accessWindowEnd)
        });
    };

    const closeEditForm = () => {
        setEditingStudyId(null);
        setEditForm(emptyStudyForm);
    };

    const handleUpdateStudy = async (e) => {
        e.preventDefault();
        if (!editingStudyId) return;
        setMessage(null);
        try {
            await api.put(`/api/studies/${editingStudyId}`, {
                title: editForm.title,
                description: editForm.description,
                blinded: editForm.blinded,
                accessWindowStart: toIsoString(editForm.accessWindowStart),
                accessWindowEnd: toIsoString(editForm.accessWindowEnd)
            });
            setMessage({ type: 'success', text: 'Study updated.' });
            closeEditForm();
            fetchData();
        } catch (err) {
            setMessage({ type: 'error', text: err.response?.data?.message || 'Could not update study.' });
        }
    };

    const toggleExpanded = (studyId) => {
        setExpandedStudyId(prev => (prev === studyId ? null : studyId));
    };

    const handlePublish = async (studyId) => {
        setPublishLoadingId(studyId);
        setMessage(null);
        try {
            await api.post(`/api/studies/${studyId}/publish`);
            setMessage({ type: 'success', text: 'Study published successfully.' });
            fetchData();
        } catch (err) {
            const errors = err.response?.data?.errors;
            setMessage({ type: 'error', text: errors ? errors.join(' ') : (err.response?.data?.message || 'Publish failed.') });
        } finally {
            setPublishLoadingId(null);
        }
    };
    // --- (LOGIC BÖLÜMÜ SONU) ---


    // 2. Adım: Tüm JSX'i yeni tasarımla değiştir
    return (
        <div className="researcher-dashboard"> {/* Ana stil class'ını yeniden kullan */}

            <div className="dashboard-header">
                <h1>Manage Studies & Quiz Assignment</h1>
            </div>

            {/* Hata veya başarı mesajlarını en üste alalım */}
            {message && (
                <div
                    className={`form-message ${message.type}`}
                    style={{marginBottom: '1.5rem'}}
                >
                    {message.text}
                </div>
            )}

            <div className="dashboard-card" style={{ marginBottom: '1.5rem' }}>
                <div className="card-header">
                    <h2>Create New Study</h2>
                </div>
                <div className="card-content">
                    <form onSubmit={handleCreateStudy} className="form-grid">
                        <label className="form-label">Title</label>
                        <input
                            className="form-input"
                            value={newStudyForm.title}
                            onChange={(e) => setNewStudyForm(prev => ({ ...prev, title: e.target.value }))}
                            required
                        />

                        <label className="form-label">Description</label>
                        <textarea
                            className="form-textarea"
                            value={newStudyForm.description}
                            onChange={(e) => setNewStudyForm(prev => ({ ...prev, description: e.target.value }))}
                            required
                        />

                        <label className="form-label">Access Window Start</label>
                        <input
                            type="datetime-local"
                            className="form-input"
                            value={newStudyForm.accessWindowStart}
                            onChange={(e) => setNewStudyForm(prev => ({ ...prev, accessWindowStart: e.target.value }))}
                            required
                        />

                        <label className="form-label">Access Window End</label>
                        <input
                            type="datetime-local"
                            className="form-input"
                            value={newStudyForm.accessWindowEnd}
                            onChange={(e) => setNewStudyForm(prev => ({ ...prev, accessWindowEnd: e.target.value }))}
                            required
                        />

                        <label className="form-label">Blinded Study?</label>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                            <input
                                type="checkbox"
                                checked={newStudyForm.blinded}
                                onChange={(e) => setNewStudyForm(prev => ({ ...prev, blinded: e.target.checked }))}
                            />
                            <span>Hide artifact metadata from participants</span>
                        </div>

                        <button type="submit" className="form-button form-button-submit" style={{ marginTop: '1rem' }}>
                            Create Study
                        </button>
                    </form>
                </div>
            </div>

            {loading && <div className="dashboard-card"><p>Loading studies...</p></div>}
            {error && <div className="dashboard-card"><p style={{ color: 'red' }}>{error}</p></div>}

            {!loading && !error && (
                studies.length === 0 ? (
                    <div className="dashboard-card">
                        <p>You have not created any studies yet.</p>
                    </div>
                ) : (
                    // Her çalışmayı ayrı bir kart olarak göster
                    <div className="dashboard-grid" style={{gridTemplateColumns: '1fr'}}>
                        {studies.map(study => (
                            <div key={study.id} className="dashboard-card">

                                <div className="card-header" style={{marginBottom: '1rem'}}>
                                    <div style={{ display: 'flex', flexDirection: 'column', gap: '0.25rem' }}>
                                        <h2>{study.title} (ID: {study.id})</h2>
                                        <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                                            <span className={`badge status-badge status-${(study.status || '').toLowerCase()}`}>
                                                {study.status || 'UNKNOWN'}
                                            </span>
                                            <span className="badge badge-role">
                                                Role: {formatRole(study.currentRole)}
                                            </span>
                                            {study.hasUnpublishedChanges && (
                                                <span className="badge badge-warn">Unpublished changes</span>
                                            )}
                                        </div>
                                    </div>
                                    <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                                        {study.permissions?.canInvite && (
                                            <Link to={`/researcher-dashboard/study/${study.id}/collaborators`}>
                                                <button className="form-button form-button-secondary" style={{ width: 'auto' }}>
                                                    Manage Collaborators
                                                </button>
                                            </Link>
                                        )}
                                        <button
                                            className="form-button form-button-secondary"
                                            style={{ width: 'auto' }}
                                            onClick={() => toggleExpanded(study.id)}
                                        >
                                            {expandedStudyId === study.id ? 'Hide Details' : 'View Details'}
                                        </button>
                                    </div>
                                </div>

                                {expandedStudyId === study.id && (
                                <div className="card-content">
                                    {study.permissions?.canPublish && editingStudyId !== study.id && (
                                        <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap', marginBottom: '1rem' }}>
                                            <button
                                                className="form-button form-button-submit"
                                                style={{ width: 'auto' }}
                                                onClick={() => handlePublish(study.id)}
                                                disabled={publishLoadingId === study.id || (!study.hasUnpublishedChanges && study.status === 'PUBLISHED')}
                                            >
                                                {publishLoadingId === study.id ? 'Publishing...' : 'Publish Study'}
                                            </button>
                                            {!study.hasUnpublishedChanges && study.status === 'PUBLISHED' && (
                                                <span style={{ color: '#9fe870', fontSize: '0.85rem' }}>
                                                    All changes are already published.
                                                </span>
                                            )}
                                        </div>
                                    )}
                                    <p>{study.description}</p>
                                    <p style={{ marginTop: '0.25rem', color: '#bbb' }}>
                                    <strong>Access Window:</strong> {formatDisplayDate(study.accessWindowStart)} – {formatDisplayDate(study.accessWindowEnd)}
                                    </p>
                                    <p style={{ marginTop: '0.25rem', color: '#bbb' }}>
                                        <strong>Latest Published Version:</strong> {study.latestPublishedVersion ?? '—'} • <strong>Next Draft Version:</strong> {study.nextVersionNumber}
                                    </p>
                                    <p style={{ marginTop: '0.25rem', color: '#bbb' }}>
                                        <strong>Blinding:</strong> {study.blinded ? 'Enabled' : 'Disabled'}
                                    </p>
                                    {study.status !== 'PUBLISHED' && (
                                        <p style={{ color: '#f5c17c', marginTop: '0.25rem' }}>
                                            Participants cannot access this study until it is published.
                                        </p>
                                    )}

                                    <hr style={{borderColor: '#444', margin: '1.5rem 0'}} />

                                    {/* --- Puan Görüntüleme --- */}
                                    {study.competencyQuiz ? (
                                        <div style={{marginBottom: '1.5rem'}}>
                                            <p style={{marginTop: 0}}>
                                                <strong>Assigned Quiz:</strong> {study.competencyQuiz.title}
                                            </p>
                                            {study.permissions?.canExport && (
                                                <Link to={`/researcher-dashboard/study/${study.id}/submissions`}>
                                                    <button
                                                        className="form-button form-button-secondary"
                                                        style={{backgroundColor: '#28a745', width: 'auto'}}
                                                    >
                                                        View Submissions (Scores)
                                                    </button>
                                                </Link>
                                            )}
                                        </div>
                                    ) : (
                                        <p><i>No quiz assigned to this study yet.</i></p>
                                    )}

                                    {study.permissions?.canManageTasks && (
                                        <div style={{ marginBottom: '1rem' }}>
                                            <Link to={`/researcher-dashboard/study/${study.id}/tasks`}>
                                                <button
                                                    className="form-button form-button-secondary"
                                                    style={{width: 'auto'}}
                                                >
                                                    Manage Tasks
                                                </button>
                                            </Link>
                                        </div>
                                    )}

                                    {/* --- Quiz Atama --- */}
                                    <label className="form-label" style={{marginTop: '1rem'}}>
                                        Assign / Change Competency Quiz:
                                    </label>
                                    <select
                                        className="form-select" // Yeni CSS class'ı
                                        value={study.competencyQuiz?.id || ""} // controlled
                                        onChange={(e) => handleAssignQuiz(study.id, e.target.value)}
                                        disabled={!study.permissions?.canEditDraft}
                                    >
                                        <option value="">-- Select a Quiz --</option>
                                        {quizzes.map(quiz => (
                                            <option key={quiz.id} value={quiz.id}>
                                                {quiz.title} (ID: {quiz.id})
                                            </option>
                                        ))}
                                    </select>
                                    <small style={{color: '#999', marginTop: '0.5rem', display: 'block'}}>
                                        {study.permissions?.canEditDraft
                                            ? 'Note: Selecting a quiz saves automatically.'
                                            : 'You cannot modify quiz assignments with your current access.'}
                                    </small>
                                    {!study.permissions?.canManageTasks && (
                                        <p style={{ color: '#bbb', fontSize: '0.85rem', marginTop: '1rem' }}>
                                            You currently have read-only access to this study.
                                        </p>
                                    )}

                                    {study.permissions?.canEditDraft && editingStudyId !== study.id && (
                                        <button
                                            className="form-button form-button-secondary"
                                            style={{ width: 'auto', marginTop: '1rem' }}
                                            onClick={() => openEditForm(study)}
                                        >
                                            Edit Draft Details
                                        </button>
                                    )}

                                    {editingStudyId === study.id && (
                                        <div style={{ marginTop: '1.5rem' }}>
                                            <h3>Edit Study Details</h3>
                                            <form onSubmit={handleUpdateStudy} className="form-grid">
                                                <label className="form-label">Title</label>
                                                <input
                                                    className="form-input"
                                                    value={editForm.title}
                                                    onChange={(e) => setEditForm(prev => ({ ...prev, title: e.target.value }))}
                                                />

                                                <label className="form-label">Description</label>
                                                <textarea
                                                    className="form-textarea"
                                                    value={editForm.description}
                                                    onChange={(e) => setEditForm(prev => ({ ...prev, description: e.target.value }))}
                                                />

                                                <label className="form-label">Access Window Start</label>
                                                <input
                                                    type="datetime-local"
                                                    className="form-input"
                                                    value={editForm.accessWindowStart}
                                                    onChange={(e) => setEditForm(prev => ({ ...prev, accessWindowStart: e.target.value }))}
                                                />

                                                <label className="form-label">Access Window End</label>
                                                <input
                                                    type="datetime-local"
                                                    className="form-input"
                                                    value={editForm.accessWindowEnd}
                                                    onChange={(e) => setEditForm(prev => ({ ...prev, accessWindowEnd: e.target.value }))}
                                                />

                                                <label className="form-label">Blinded Study?</label>
                                                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                                                    <input
                                                        type="checkbox"
                                                        checked={editForm.blinded}
                                                        onChange={(e) => setEditForm(prev => ({ ...prev, blinded: e.target.checked }))}
                                                    />
                                                    <span>Hide artifact metadata from participants</span>
                                                </div>

                                                <div style={{ display: 'flex', gap: '0.5rem', marginTop: '1rem', flexWrap: 'wrap' }}>
                                                    <button type="submit" className="form-button form-button-submit">
                                                        Save Changes
                                                    </button>
                                                    <button type="button" className="form-button form-button-secondary" onClick={closeEditForm}>
                                                        Cancel
                                                    </button>
                                                    {study.permissions?.canPublish && (
                                                        <button
                                                            type="button"
                                                            className="form-button form-button-submit"
                                                            style={{ backgroundColor: '#1d6f43' }}
                                                            onClick={() => handlePublish(study.id)}
                                                            disabled={publishLoadingId === study.id || (!study.hasUnpublishedChanges && study.status === 'PUBLISHED')}
                                                        >
                                                            {publishLoadingId === study.id ? 'Publishing...' : 'Publish Draft'}
                                                        </button>
                                                    )}
                                                </div>
                                            </form>
                                        </div>
                                    )}
                                </div>
                                )}
                            </div>
                        ))}
                    </div>
                )
            )}
        </div>
    );
};

export default ManageStudies;