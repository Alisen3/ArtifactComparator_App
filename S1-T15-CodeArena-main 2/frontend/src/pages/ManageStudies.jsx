// Dosya Yolu: src/pages/ManageStudies.jsx
// (TAM VE GÜNCELLENMİŞ KOD - Yeni Tasarım)

import React, { useState, useEffect } from 'react';
import { api } from '../context/AuthContext';
import { Link } from 'react-router-dom';

// 1. Adım: Mevcut CSS'lerimizi import et
import './ResearcherDashboard.css'; // Kart stilleri için
import './Forms.css'; // Form (select, button) stilleri için

const ManageStudies = () => {
    // --- (LOGIC BÖLÜMÜ DEĞİŞMEDİ) ---
    const [studies, setStudies] = useState([]);
    const [quizzes, setQuizzes] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [message, setMessage] = useState(null);

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
                                    <h2>{study.title} (ID: {study.id})</h2>
                                </div>

                                <div className="card-content">
                                    <p>{study.description}</p>

                                    <hr style={{borderColor: '#444', margin: '1.5rem 0'}} />

                                    {/* --- Puan Görüntüleme --- */}
                                    {study.competencyQuiz ? (
                                        <div style={{marginBottom: '1.5rem'}}>
                                            <p style={{marginTop: 0}}>
                                                <strong>Assigned Quiz:</strong> {study.competencyQuiz.title}
                                            </p>
                                            <Link to={`/researcher-dashboard/study/${study.id}/submissions`}>
                                                <button
                                                    className="form-button form-button-secondary"
                                                    style={{backgroundColor: '#28a745', width: 'auto'}}
                                                >
                                                    View Submissions (Scores)
                                                </button>
                                            </Link>

                                            {/* (Adım 3) YENİ EKLENEN BUTON */}
                                            <Link to={`/researcher-dashboard/study/${study.id}/tasks`} style={{marginLeft: '1rem'}}>
                                                <button
                                                    className="form-button form-button-secondary"
                                                    style={{width: 'auto'}}
                                                >
                                                    Manage Tasks
                                                </button>
                                            </Link>
                                            {/* --- --- --- --- */}

                                        </div>
                                    ) : (
                                        <p><i>No quiz assigned to this study yet.</i></p>
                                    )}

                                    {/* --- Quiz Atama --- */}
                                    <label className="form-label" style={{marginTop: '1rem'}}>
                                        Assign / Change Competency Quiz:
                                    </label>
                                    <select
                                        className="form-select" // Yeni CSS class'ı
                                        defaultValue={study.competencyQuiz?.id || ""} // (value -> defaultValue)
                                        onChange={(e) => handleAssignQuiz(study.id, e.target.value)}
                                    >
                                        <option value="">-- Select a Quiz --</option>
                                        {quizzes.map(quiz => (
                                            <option key={quiz.id} value={quiz.id}>
                                                {quiz.title} (ID: {quiz.id})
                                            </option>
                                        ))}
                                    </select>
                                    <small style={{color: '#999', marginTop: '0.5rem', display: 'block'}}>
                                        Note: Selecting a quiz saves automatically.
                                    </small>
                                </div>
                            </div>
                        ))}
                    </div>
                )
            )}
        </div>
    );
};

export default ManageStudies;