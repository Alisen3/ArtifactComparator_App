// Dosya Yolu: src/pages/ViewSubmissions.jsx
// (TAM VE GÜNCELLENMİŞ KOD - Yeni Tasarım)

import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { api } from '../context/AuthContext';

// 1. Adım: Mevcut CSS'lerimizi import et
import './ResearcherDashboard.css'; // Kart ve Tablo stilleri için
import './Forms.css'; // Buton stilleri için

const ViewSubmissions = () => {
    const { studyId } = useParams();
    const [submissions, setSubmissions] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [studyTitle, setStudyTitle] = useState(''); // (Bunu da çekebiliriz, şimdilik ID kalsın)

    // --- (LOGIC BÖLÜMÜ DEĞİŞMEDİ, SADECE DİL DEĞİŞTİ) ---
    useEffect(() => {
        const fetchSubmissions = async () => {
            try {
                setLoading(true);
                setError(null);
                const response = await api.get(`/api/studies/${studyId}/quiz/submissions`);
                setSubmissions(response.data); // SubmissionSummaryDTO[]
                setStudyTitle(`Study ID: ${studyId}`);
            } catch (err) {
                console.error("Error loading submissions:", err);
                setError(err.response?.data?.message || err.message || "Submissions could not be loaded.");
            } finally {
                setLoading(false);
            }
        };

        fetchSubmissions();
    }, [studyId]);
    // --- (LOGIC BÖLÜMÜ SONU) ---


    // 2. Adım: Tüm JSX'i yeni tasarımla değiştir
    return (
        <div className="researcher-dashboard"> {/* Ana stil class'ını yeniden kullan */}

            <div className="dashboard-header" style={{marginBottom: '1rem'}}>
                <h1>Quiz Submissions</h1>
                <p style={{marginTop: '-1.5rem', color: '#ccc'}}>{studyTitle}</p>
            </div>

            <p>
                <Link to="/researcher-dashboard/manage-studies">
                    <button className="form-button form-button-secondary" style={{width: 'auto'}}>
                        &larr; Back to Study List
                    </button>
                </Link>
            </p>

            {/* Sayfayı tam genişlikte bir kart içine al */}
            <div className="dashboard-card full-width">
                <div className="card-header">
                    <h2>Submission Scores</h2>
                </div>

                <div className="card-content">
                    {loading && <p>Loading submissions...</p>}
                    {error && <p style={{ color: 'red' }}>{error}</p>}
                    {!loading && !error && (
                        submissions.length === 0 ? (
                            <p>No completed submissions found for this study's quiz yet.</p>
                        ) : (
                            // 'ResearcherDashboard.css' içinden tablo stilini kullan
                            <table className="artifact-list-table">
                                <thead>
                                <tr>
                                    <th>Submission ID</th>
                                    <th>Participant Name</th>
                                    <th>Score</th>
                                    <th>Submitted At</th>
                                </tr>
                                </thead>
                                <tbody>
                                {submissions.map(sub => (
                                    <tr key={sub.submissionId}>
                                        <td>{sub.submissionId}</td>
                                        <td>{sub.participantName} (ID: {sub.participantId})</td>
                                        <td>{sub.score.toFixed(2)}%</td>
                                        <td>{new Date(sub.submittedAt).toLocaleString()}</td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        )
                    )}
                </div> {/* .card-content sonu */}
            </div> {/* .dashboard-card sonu */}
        </div> /* .researcher-dashboard sonu */
    );
};

export default ViewSubmissions;