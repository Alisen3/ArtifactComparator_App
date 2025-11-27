// Dosya Yolu: src/pages/ResearcherDashboard.jsx
// (DOĞRU YENİ KOD - KARTLI TASARIM)

import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { api } from '../context/AuthContext';
import { Link } from 'react-router-dom'; // Link'i import et

// Yeni CSS dosyasını import et (Bu dosyayı zaten oluşturmuştuk)
import './ResearcherDashboard.css';

const ResearcherDashboard = () => {
    const { user } = useAuth();

    // State'ler
    const [myArtifacts, setMyArtifacts] = useState([]);
    const [selectedFile, setSelectedFile] = useState(null);
    const [loading, setLoading] = useState(true);
    const [uploadMessage, setUploadMessage] = useState(null);
    const [listError, setListError] = useState(null);

    // Artefaktları çeken fonksiyon
    const fetchArtifacts = async () => {
        setLoading(true);
        setListError(null);
        try {
            const response = await api.get('/api/artifacts/my-artifacts');
            setMyArtifacts(response.data);
        } catch (err) {
            console.error("Error loading artifacts:", err);
            setListError("Your artifacts could not be loaded.");
        } finally {
            setLoading(false);
        }
    };

    // Component ilk yüklendiğinde listeyi çek
    useEffect(() => {
        fetchArtifacts();
    }, []);

    // Dosya seçme
    const handleFileChange = (e) => {
        setSelectedFile(e.target.files[0]);
        setUploadMessage(null);
    };

    // Dosya yükleme
    const handleUploadSubmit = async (e) => {
        e.preventDefault();
        if (!selectedFile) {
            setUploadMessage({ type: 'error', text: 'Please select a file first.' });
            return;
        }
        const formData = new FormData();
        formData.append('file', selectedFile);

        try {
            setUploadMessage({ type: 'info', text: 'Uploading file...' });
            const response = await api.post('/api/artifacts/upload', formData, {
                headers: { 'Content-Type': 'multipart/form-data' }
            });
            setUploadMessage({ type: 'success', text: response.data.message });
            setSelectedFile(null);
            // Formu temizle
            if (document.getElementById('file-input')) {
                document.getElementById('file-input').value = null;
            }
            fetchArtifacts(); // Listeyi yenile

        } catch (err) {
            console.error("File upload error:", err);
            setUploadMessage({ type: 'error', text: 'File could not be uploaded. (' + (err.response?.data?.error || err.message) + ')' });
        }
    };

    // Mesaj renkleri için dinamik class
    const messageClass = uploadMessage ? `upload-message ${uploadMessage.type}` : 'upload-message';


    return (
        <div className="researcher-dashboard">
            <div className="dashboard-header">
                <h1>Dashboard</h1>
                <p style={{marginTop: '-1.5rem', color: '#ccc'}}>Welcome, {user.name}!</p>
            </div>

            {/* Tasarımdaki (Home_Dashboard.png) kart ızgarası */}
            <div className="dashboard-grid">

                {/* --- İstatistik Kartları (Placeholder) --- */}
                <div className="dashboard-card">
                    <div className="card-header">
                        <h2>Active Studies</h2>
                    </div>
                    <div className="card-content stat-card-content">
                        5 {/* (Placeholder) */}
                        <p>Total studies you are running</p>
                    </div>
                </div>

                <div className="dashboard-card">
                    <div className="card-header">
                        <h2>Total Submissions</h2>
                    </div>
                    <div className="card-content stat-card-content">
                        128 {/* (Placeholder) */}
                        <p>Completed evaluations by participants</p>
                    </div>
                </div>

                {/* --- Artefakt Yükleme Kartı (Mevcut Fonksiyon) --- */}
                <div className="dashboard-card">
                    <div className="card-header">
                        <h2>Upload New Artifact</h2>
                    </div>
                    <div className="card-content">
                        <form onSubmit={handleUploadSubmit} className="upload-form">
                            <input
                                type="file"
                                id="file-input"
                                onChange={handleFileChange}
                            />
                            <button type="submit">Upload</button>
                        </form>
                        {uploadMessage && <p className={messageClass}>{uploadMessage.text}</p>}
                    </div>
                </div>

                {/* --- Artefakt Listesi Kartı (Mevcut Fonksiyon) --- */}
                <div className="dashboard-card full-width">
                    <div className="card-header">
                        <h2>My Uploaded Artifacts</h2>
                    </div>
                    <div className="card-content">
                        {loading && <p>Loading artifacts...</p>}
                        {listError && <p style={{ color: 'red' }}>{listError}</p>}
                        {!loading && !listError && (
                            myArtifacts.length === 0 ? (
                                <p>You have not uploaded any artifacts yet.</p>
                            ) : (
                                <table className="artifact-list-table">
                                    <thead>
                                    <tr>
                                        <th>ID</th>
                                        <th>File Name</th>
                                        <th>File Type</th>
                                        <th>Uploaded At</th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    {myArtifacts.map(artifact => (
                                        <tr key={artifact.id}>
                                            <td>{artifact.id}</td>
                                            <td>{artifact.fileName}</td>
                                            <td>{artifact.fileType}</td>
                                            <td>{new Date(artifact.uploadedAt).toLocaleString()}</td>
                                        </tr>
                                    ))}
                                    </tbody>
                                </table>
                            )
                        )}
                    </div>
                </div>

            </div> {/* .dashboard-grid sonu */}
        </div> /* .researcher-dashboard sonu */
    );
};

export default ResearcherDashboard;