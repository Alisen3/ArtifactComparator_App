// Dosya Yolu: src/pages/TakeQuiz.jsx
// (TAM VE GÜNCELLENMİŞ KOD - Yeni Tasarım)

import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { api } from '../context/AuthContext';

// 1. Adım: Yeni CSS dosyalarını import et
import './Forms.css';
import './dashboards/ResearcherDashboard.css'; // Kart stilleri için

const TakeQuiz = () => {
    const { studyId } = useParams();
    const navigate = useNavigate();

    // --- (LOGIC BÖLÜMÜ DEĞİŞMEDİ) ---
    const [quiz, setQuiz] = useState(null);
    const [answers, setAnswers] = useState({});
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [submitting, setSubmitting] = useState(false);
    const [result, setResult] = useState(null);
    const [timeLeft, setTimeLeft] = useState(null);

    useEffect(() => {
        const fetchQuiz = async () => {
            try {
                setLoading(true);
                const response = await api.get(`/api/studies/${studyId}/quiz`);
                setQuiz(response.data);
                if (response.data.durationInMinutes && response.data.durationInMinutes > 0) {
                    setTimeLeft(response.data.durationInMinutes * 60);
                }
            } catch (err) {
                console.error("Quiz loading error:", err);
                setError(err.response?.data?.message || err.message || "Could not load quiz.");
            } finally {
                setLoading(false);
            }
        };
        fetchQuiz();
    }, [studyId]);

    useEffect(() => {
        if (timeLeft === null || timeLeft <= 0) {
            if (timeLeft === 0) {
                handleSubmit(true); // Süre doldu
            }
            return;
        }
        const timerId = setInterval(() => {
            setTimeLeft(prevTime => prevTime - 1);
        }, 1000);
        return () => clearInterval(timerId);
    }, [timeLeft]);

    const handleAnswerChange = (questionId, questionType, value) => {
        setAnswers(prev => ({
            ...prev,
            [questionId]: {
                selectedOptionId: questionType === 'MULTIPLE_CHOICE' ? value : null,
                answerText: questionType === 'SHORT_ANSWER' ? value : null
            }
        }));
    };

    const handleSubmit = async (isTimeUp = false) => {
        if (submitting) return;

        if (isTimeUp) {
            alert("Time is up! Your answers are being submitted automatically.");
        } else {
            if (!window.confirm("Are you sure you want to submit this quiz? This action cannot be undone.")) {
                return;
            }
        }

        setSubmitting(true);
        setError(null);

        const submissionData = {
            answers: Object.keys(answers).map(qId => ({
                questionId: qId,
                selectedOptionId: answers[qId].selectedOptionId,
                answerText: answers[qId].answerText
            }))
        };

        try {
            const response = await api.post(`/api/studies/${studyId}/quiz/submit`, submissionData);
            setResult(response.data); // { message, score, correctAnswers, totalQuestions }
        } catch (err) {
            console.error("Quiz submission error:", err);
            setError(err.response?.data?.message || err.message || "Could not submit quiz.");
            setSubmitting(false);
        }
    };
    // --- (LOGIC BÖLÜMÜ SONU) ---


    // --- 2. Adım: (VIEW / JSX) TAMAMEN YENİLENDİ ---

    // 2a. Yükleniyor...
    if (loading) {
        return (
            <div className="dashboard-card">
                <div className="card-content">Loading Quiz...</div>
            </div>
        );
    }

    // 2b. Hata Ekranı
    if (error && !result) {
        return (
            <div className="form-container">
                <div className="form-message error">
                    Error: {error}
                    <br /><br />
                    <Link to="/participant-dashboard" className="form-button form-button-secondary" style={{textDecoration: 'none'}}>
                        Back to Dashboard
                    </Link>
                </div>
            </div>
        );
    }

    // 2c. Sonuç Ekranı (Quiz tamamlandı) (Success.png benzeri)
    if (result) {
        return (
            <div className="form-container" style={{textAlign: 'center'}}>
                <div className="form-header">
                    <h2>Quiz Completed!</h2>
                </div>
                <strong>{result.message}</strong>
                <h3>Your Score: {result.score.toFixed(2)}%</h3>
                <p>({result.correctAnswers} out of {result.totalQuestions} gradable questions correct)</p>
                {error && <p className="form-message error" style={{marginTop: '1rem'}}>Submission Error: {error}</p>}

                <Link
                    to="/participant-dashboard"
                    className="form-button form-button-submit"
                    style={{textDecoration: 'none', marginTop: '2rem'}}
                >
                    Go back to Dashboard
                </Link>
            </div>
        );
    }

    // 2d. Quiz Ekranı (Ana ekran)
    if (!quiz) return <div>Quiz not found.</div>;

    const formatTime = (seconds) => {
        if (seconds === null) return "Unlimited";
        const mins = Math.floor(seconds / 60);
        const secs = seconds % 60;
        return `${mins}:${secs < 10 ? '0' : ''}${secs}`;
    };

    return (
        <div className="form-container">

            {/* Başlık ve Zamanlayıcı */}
            <div className="form-header" style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
                <div>
                    <h2>{quiz.title}</h2>
                    <p style={{marginTop: '-1.5rem', color: '#ccc'}}>{quiz.description}</p>
                </div>
                {quiz.durationInMinutes > 0 && (
                    <div style={{
                        color: timeLeft <= 60 ? '#f44336' : 'white',
                        border: `1px solid ${timeLeft <= 60 ? '#f44336' : '#555'}`,
                        padding: '0.5rem 1rem',
                        borderRadius: '8px',
                        backgroundColor: '#1a1a1a'
                    }}>
                        <strong>Time Left: {formatTime(timeLeft)}</strong>
                    </div>
                )}
            </div>

            {/* Sorular */}
            <form onSubmit={(e) => { e.preventDefault(); handleSubmit(false); }}>
                {quiz.questions.map((q, qIndex) => (
                    <fieldset key={q.id} className="form-fieldset" style={{border: '1px solid #555'}}>
                        <legend className="form-legend">Question {qIndex + 1}</legend>
                        <p className="form-label" style={{fontSize: '1.1em', color: 'white'}}>{q.questionText}</p>

                        <div className="form-group">
                            {q.questionType === 'MULTIPLE_CHOICE' && (
                                <div>
                                    {q.options.map(opt => (
                                        <div key={opt.id} className="option-item">
                                            <input
                                                type="radio"
                                                id={`q${q.id}_opt${opt.id}`}
                                                name={`question_${q.id}`}
                                                value={opt.id}
                                                checked={answers[q.id]?.selectedOptionId === opt.id}
                                                onChange={() => handleAnswerChange(q.id, 'MULTIPLE_CHOICE', opt.id)}
                                                required
                                            />
                                            <label htmlFor={`q${q.id}_opt${opt.id}`} style={{color: 'white', flex: 1}}>{opt.optionText}</label>
                                        </div>
                                    ))}
                                </div>
                            )}

                            {q.questionType === 'SHORT_ANSWER' && (
                                <textarea
                                    className="form-textarea"
                                    value={answers[q.id]?.answerText || ""}
                                    onChange={(e) => handleAnswerChange(q.id, 'SHORT_ANSWER', e.target.value)}
                                    placeholder="Type your answer here..."
                                    required
                                />
                            )}
                        </div>
                    </fieldset>
                ))}

                <button type="submit" disabled={submitting} className="form-button form-button-submit" style={{fontSize: '1.2em'}}>
                    {submitting ? 'Submitting...' : "Finish & Submit Quiz"}
                </button>
            </form>
        </div>
    );
};

export default TakeQuiz;