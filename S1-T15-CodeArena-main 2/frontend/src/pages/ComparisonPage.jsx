// Dosya Yolu: src/pages/ComparisonPage.jsx
// (YENİ DOSYA)

import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { api } from '../context/AuthContext';

// Gerekli stilleri import et
import './Forms.css';
import './ComparisonPage.css';

// Artefakt içeriğini getiren yardımcı fonksiyon
const fetchArtifactContent = async (artifactId) => {
    try {
        // Not: Response tipinin 'text' olmasını bekliyoruz, 'json' değil
        const response = await api.get(`/api/artifacts/${artifactId}`, {
            responseType: 'text'
        });
        return response.data;
    } catch (err) {
        console.error(`Error fetching artifact ${artifactId}:`, err);
        return `Error: Could not load artifact content. (${err.message})`;
    }
};

const ComparisonPage = () => {
    const { taskId } = useParams();
    const navigate = useNavigate();

    // Data State
    const [task, setTask] = useState(null);
    const [contentA, setContentA] = useState("Loading artifact...");
    const [contentB, setContentB] = useState("Loading artifact...");

    // Form State
    const [annotations, setAnnotations] = useState("");
    const [ratingA, setRatingA] = useState(3); // Varsayılan 3 puan
    const [ratingB, setRatingB] = useState(3);

    // UI State
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [submitting, setSubmitting] = useState(false);
    const [message, setMessage] = useState(null);

    // 1. Görev detaylarını (DTO) çek
    useEffect(() => {
        const fetchTaskDetails = async () => {
            setLoading(true);
            setError(null);
            try {
                // Backend'de düzelttiğimiz endpoint'i (DTO dönen) çağır:
                const response = await api.get(`/api/tasks/${taskId}`);
                const taskData = response.data; // ComparisonTaskDetailDTO
                setTask(taskData);

                // Eğer görev 'COMPLETED' değilse, mevcut notları yükle (kaydet/devam et)
                if (taskData.status !== 'COMPLETED') {
                    setAnnotations(taskData.annotations || "");
                    setRatingA(taskData.ratingA || 3);
                    setRatingB(taskData.ratingB || 3);
                }

                // 2. Görev detayları gelince, artefakt içeriklerini çek
                fetchArtifactContent(taskData.artifactA.id).then(setContentA);
                fetchArtifactContent(taskData.artifactB.id).then(setContentB);

            } catch (err) {
                console.error("Error loading task details:", err);
                setError(err.response?.data?.message || err.message || "Could not load task.");
            } finally {
                setLoading(false);
            }
        };

        fetchTaskDetails();
    }, [taskId]);

    // 3. Görevi tamamlama (Submit)
    const handleSubmit = async (e) => {
        e.preventDefault();
        setSubmitting(true);
        setMessage(null);

        const submissionData = {
            annotations: annotations,
            ratingA: parseInt(ratingA),
            ratingB: parseInt(ratingB)
        };

        try {
            // POST /api/tasks/{taskId}/complete
            await api.post(`/api/tasks/${taskId}/complete`, submissionData);

            setMessage({ type: 'success', text: 'Task completed successfully! Redirecting...' });
            setTimeout(() => {
                navigate('/participant-dashboard'); // Dashboard'a geri dön
            }, 2000);

        } catch (err) {
            console.error("Error submitting task:", err);
            setMessage({ type: 'error', text: err.response?.data?.message || 'Could not submit task.' });
            setSubmitting(false);
        }
    };

    const messageClass = message ? `form-message ${message.type}` : 'form-message';


    // --- RENDER BÖLÜMÜ ---
    if (loading) {
        return <div className="dashboard-card"><p>Loading comparison task...</p></div>;
    }

    if (error) {
        return <div className="form-message error">{error}</div>;
    }

    // Görev zaten tamamlanmışsa
    if (task.status === 'COMPLETED') {
        return (
            <div className="form-container">
                <div className="form-message info">
                    You have already completed this task.
                    <br /><br />
                    <button
                        className="form-button form-button-secondary"
                        onClick={() => navigate('/participant-dashboard')}
                    >
                        Back to Dashboard
                    </button>
                </div>
            </div>
        );
    }

    // Ana Karşılaştırma Arayüzü
    return (
        <div className="comparison-page-container">

            {/* 1. Başlık */}
            <div className="comparison-header">
                <h1>{task.studyTitle}</h1>
                <p>Task ID: {task.taskId} | Comparing '{task.artifactA.fileName}' vs '{task.artifactB.fileName}'</p>
            </div>

            {/* 2. Yan Yana Sütunlar */}
            <div className="comparison-body">
                {/* Sütun A */}
                <div className="artifact-viewer">
                    <div className="artifact-header">{task.artifactA.fileName}</div>
                    <pre className="artifact-content">{contentA}</pre>
                </div>
                {/* Sütun B */}
                <div className="artifact-viewer">
                    <div className="artifact-header">{task.artifactB.fileName}</div>
                    <pre className="artifact-content">{contentB}</pre>
                </div>
            </div>

            {/* 3. Puanlama ve Yorum Formu */}
            <form className="submission-form-container" onSubmit={handleSubmit}>

                {/* Form Sol Taraf (Yorumlar) */}
                <div className="form-group">
                    <label className="form-label">Annotations / Comments:</label>
                    <textarea
                        className="form-textarea"
                        style={{minHeight: '150px'}}
                        placeholder="Provide your overall comparison comments here..."
                        value={annotations}
                        onChange={(e) => setAnnotations(e.target.value)}
                    />
                </div>

                {/* Form Sağ Taraf (Puanlama ve Buton) */}
                <div>
                    <div className="form-group form-group-rating">
                        <label className="form-label">Ratings (1 = Very Poor, 5 = Very Good):</label>

                        <div className="rating-item">
                            <label htmlFor="ratingA" className="form-label">
                                {task.artifactA.fileName}: <strong>{ratingA}</strong>/5
                            </label>
                            <input
                                type="range" id="ratingA" min="1" max="5"
                                value={ratingA}
                                onChange={(e) => setRatingA(e.target.value)}
                                style={{flex: 1, marginLeft: '1rem'}}
                            />
                        </div>

                        <div className="rating-item">
                            <label htmlFor="ratingB" className="form-label">
                                {task.artifactB.fileName}: <strong>{ratingB}</strong>/5
                            </label>
                            <input
                                type="range" id="ratingB" min="1" max="5"
                                value={ratingB}
                                onChange={(e) => setRatingB(e.target.value)}
                                style={{flex: 1, marginLeft: '1rem'}}
                            />
                        </div>
                    </div>

                    <button type="submit" className="form-button form-button-submit" disabled={submitting}>
                        {submitting ? "Submitting..." : "Complete & Submit Task"}
                    </button>

                    {message && <p className={messageClass} style={{marginTop: '1rem'}}>{message.text}</p>}
                </div>

            </form>
        </div>
    );
};

export default ComparisonPage;