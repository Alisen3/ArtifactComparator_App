// Dosya Yolu: src/pages/ManageStudyTasks.jsx
// (YENİ DOSYA)

import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { api } from '../context/AuthContext';

// Gerekli stilleri import et
import './ResearcherDashboard.css';
import './Forms.css';

const ManageStudyTasks = () => {
    const { studyId } = useParams();

    // 1. State'ler
    // Listeler için
    const [assignedTasks, setAssignedTasks] = useState([]);
    const [participants, setParticipants] = useState([]);
    const [artifacts, setArtifacts] = useState([]);
    const [studyTitle, setStudyTitle] = useState(''); // (Bonus)

    // Form için
    const [selectedParticipant, setSelectedParticipant] = useState('');
    const [selectedArtifactA, setSelectedArtifactA] = useState('');
    const [selectedArtifactB, setSelectedArtifactB] = useState('');

    // UI için
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [formMessage, setFormMessage] = useState(null);

    // 2. Veri Çekme (useEffect)
    const fetchData = async () => {
        setLoading(true);
        setError(null);
        try {
            // 3 API'yi aynı anda çek
            const [tasksRes, participantsRes, artifactsRes, studiesRes] = await Promise.all([
                api.get(`/api/studies/${studyId}/tasks`),        // (Adım 1'de oluşturulan API)
                api.get('/api/users/participants'),             // (Dropdown için - Zaten vardı)
                api.get('/api/artifacts/my-artifacts'),         // (Dropdown için - Zaten vardı)
                api.get('/api/studies/my-studies')              // (Çalışma başlığını bulmak için)
            ]);

            setAssignedTasks(tasksRes.data);
            setParticipants(participantsRes.data);
            setArtifacts(artifactsRes.data);

            // Çalışma başlığını bul
            const currentStudy = studiesRes.data.find(s => s.id == studyId);
            if (currentStudy) {
                setStudyTitle(currentStudy.title);
            }

        } catch (err) {
            console.error("Data loading error:", err);
            setError("Page data could not be loaded.");
        } finally {
            setLoading(false);
        }
    };

    // Sadece mevcut atanmış görevleri yenileyen fonksiyon
    const fetchTasksOnly = async () => {
        try {
            const tasksRes = await api.get(`/api/studies/${studyId}/tasks`);
            setAssignedTasks(tasksRes.data);
        } catch (err) {
            console.error("Tasks refresh error:", err);
            setFormMessage({ type: 'error', text: 'Could not refresh task list.' });
        }
    };

    // Sayfa yüklendiğinde verileri çek
    useEffect(() => {
        fetchData();
    }, [studyId]);

    // 3. Form Gönderme (handleSubmit)
    const handleSubmit = async (e) => {
        e.preventDefault();
        setFormMessage(null);

        if (!selectedParticipant || !selectedArtifactA || !selectedArtifactB) {
            setFormMessage({ type: 'error', text: 'Please select participant and both artifacts.' });
            return;
        }
        if (selectedArtifactA === selectedArtifactB) {
            setFormMessage({ type: 'error', text: 'Artifact A and Artifact B cannot be the same.' });
            return;
        }

        setFormMessage({ type: 'info', text: 'Assigning task...' });

        try {
            const requestBody = {
                participantId: Number(selectedParticipant),
                artifactAId: Number(selectedArtifactA),
                artifactBId: Number(selectedArtifactB)
            };
            console.log('Request body:', requestBody);
            console.log('Study ID:', studyId);

            // (Bu API zaten StudyController'da mevcuttu)
            const response = await api.post(`/api/studies/${studyId}/tasks`, requestBody);
            console.log('Success response:', response.data);

            setFormMessage({ type: 'success', text: 'Task assigned successfully!' });

            // Formu temizle
            setSelectedParticipant('');
            setSelectedArtifactA('');
            setSelectedArtifactB('');

            // Görev listesini yenile
            fetchTasksOnly();

        } catch (err) {
            console.error("Task assign error - Full error:", err);
            console.error("Error response:", err.response);
            console.error("Error data:", err.response?.data);
            console.error("Error status:", err.response?.status);

            const errorMessage = err.response?.data?.message || err.response?.data || err.message || 'Could not assign task.';
            setFormMessage({ type: 'error', text: `Error: ${errorMessage}` });
        }
    };

    const messageClass = formMessage ? `form-message ${formMessage.type}` : 'form-message';


    // 4. JSX (Görünüm)
    return (
        <div className="researcher-dashboard">

            <div className="dashboard-header" style={{marginBottom: '1rem'}}>
                <h1>Manage Tasks</h1>
                <p style={{marginTop: '-1.5rem', color: '#ccc'}}>
                    {loading ? `Study ID: ${studyId}` : `${studyTitle} (ID: ${studyId})`}
                </p>
            </div>

            <p>
                <Link to="/researcher-dashboard/manage-studies">
                    <button className="form-button form-button-secondary" style={{width: 'auto'}}>
                        &larr; Back to Study List
                    </button>
                </Link>
            </p>

            {error && <div className="form-message error">{error}</div>}

            {/* --- "YENİ GÖREV ATA" FORMU (Adım 2) --- */}
            <div className="dashboard-card" style={{marginBottom: '1.5rem'}}>
                <div className="card-header">
                    <h2>Assign New Task</h2>
                </div>
                <div className="card-content">
                    <form onSubmit={handleSubmit}>
                        {/* 1. Katılımcı Seçimi */}
                        <div className="form-group">
                            <label className="form-label">Participant:</label>
                            <select
                                className="form-select"
                                value={selectedParticipant}
                                onChange={(e) => setSelectedParticipant(e.target.value)}
                            >
                                <option value="">-- Select a Participant --</option>
                                {participants.map(p => (
                                    <option key={p.id} value={p.id}>{p.name} (ID: {p.id})</option>
                                ))}
                            </select>
                        </div>

                        {/* 2. Artefakt A Seçimi */}
                        <div className="form-group">
                            <label className="form-label">Artifact A:</label>
                            <select
                                className="form-select"
                                value={selectedArtifactA}
                                onChange={(e) => setSelectedArtifactA(e.target.value)}
                            >
                                <option value="">-- Select Artifact A --</option>
                                {artifacts.map(a => (
                                    <option key={a.id} value={a.id}>{a.fileName} (ID: {a.id})</option>
                                ))}
                            </select>
                        </div>

                        {/* 3. Artefakt B Seçimi */}
                        <div className="form-group">
                            <label className="form-label">Artifact B:</label>
                            <select
                                className="form-select"
                                value={selectedArtifactB}
                                onChange={(e) => setSelectedArtifactB(e.target.value)}
                            >
                                <option value="">-- Select Artifact B --</option>
                                {artifacts.map(a => (
                                    <option key={a.id} value={a.id}>{a.fileName} (ID: {a.id})</option>
                                ))}
                            </select>
                        </div>

                        <button type="submit" className="form-button form-button-submit">
                            Assign Task
                        </button>

                        {formMessage && <p className={messageClass} style={{marginTop: '1rem'}}>{formMessage.text}</p>}
                    </form>
                </div>
            </div>

            {/* --- "ATANMIŞ GÖREVLER" LİSTESİ (Adım 1 API'sini kullanır) --- */}
            <div className="dashboard-card full-width">
                <div className="card-header">
                    <h2>Assigned Tasks ({assignedTasks.length})</h2>
                </div>
                <div className="card-content">
                    {loading && <p>Loading tasks...</p>}
                    {!loading && (
                        assignedTasks.length === 0 ? (
                            <p>No tasks have been assigned to this study yet.</p>
                        ) : (
                            <table className="artifact-list-table">
                                <thead>
                                <tr>
                                    <th>Task ID</th>
                                    <th>Participant</th>
                                    <th>Status</th>
                                    <th>Artifact A</th>
                                    <th>Artifact B</th>
                                    <th>Assigned At</th>
                                </tr>
                                </thead>
                                <tbody>
                                {assignedTasks.map(task => (
                                    <tr key={task.taskId}>
                                        <td>{task.taskId}</td>
                                        <td>{task.participantName} (ID: {task.participantId})</td>
                                        <td>{task.status}</td>
                                        <td>{task.artifactAFileName}</td>
                                        <td>{task.artifactBFileName}</td>
                                        <td>{new Date(task.createdAt).toLocaleString()}</td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        )
                    )}
                </div>
            </div>

        </div>
    );
};

export default ManageStudyTasks;