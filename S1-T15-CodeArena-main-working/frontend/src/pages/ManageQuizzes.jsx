// Dosya Yolu: src/pages/ManageQuizzes.jsx
// (TAM VE GÜNCELLENMİŞ KOD - Yeni Tasarım)

import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../context/AuthContext';

// 1. Adım: Mevcut CSS'lerimizi import et
import './dashboards/ResearcherDashboard.css'; // Kart ve Tablo stilleri için
import './Forms.css'; // Buton stilleri için

const ManageQuizzes = () => {
    const [quizzes, setQuizzes] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        const fetchQuizzes = async () => {
            try {
                setLoading(true);
                setError(null);
                const response = await api.get('/api/quizzes/my-quizzes');
                setQuizzes(response.data);
            } catch (err) {
                console.error("Error loading quizzes:", err);
                setError("Quizzes could not be loaded.");
            } finally {
                setLoading(false);
            }
        };

        fetchQuizzes();
    }, []);

    // 2. Adım: Tüm JSX'i yeni tasarımla değiştir
    return (
        <div className="researcher-dashboard"> {/* Ana stil class'ını yeniden kullan */}

            {/* Sayfayı tam genişlikte bir kart içine al */}
            <div className="dashboard-card full-width">

                <div className="card-header">
                    <h2>Manage My Quizzes & Surveys</h2>
                    <Link to="/researcher-dashboard/create-quiz">
                        {/* 'Forms.css' içinden buton stilini kullan */}
                        <button
                            className="form-button form-button-secondary"
                            style={{width: 'auto'}}
                        >
                            + Create New Quiz
                        </button>
                    </Link>
                </div>

                <div className="card-content">
                    {loading && <p>Loading quizzes...</p>}
                    {error && <p style={{ color: 'red' }}>{error}</p>}
                    {!loading && !error && (
                        quizzes.length === 0 ? (
                            <p>You have not created any quizzes yet.</p>
                        ) : (
                            // 'ResearcherDashboard.css' içinden tablo stilini kullan
                            <table className="artifact-list-table">
                                <thead>
                                <tr>
                                    <th>ID</th>
                                    <th>Title</th>
                                    <th>Questions</th>
                                    <th>Duration (Mins)</th>
                                    <th>Created At</th>
                                </tr>
                                </thead>
                                <tbody>
                                {quizzes.map(quiz => (
                                    <tr key={quiz.id}>
                                        <td>{quiz.id}</td>
                                        <td>{quiz.title}</td>
                                        <td>{quiz.questionCount}</td>
                                        <td>{quiz.durationInMinutes || 'Unlimited'}</td>
                                        <td>{new Date(quiz.createdAt).toLocaleString()}</td>
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

export default ManageQuizzes;