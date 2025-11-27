// Dosya Yolu: src/pages/ResearcherDashboard.jsx
import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { api } from '../context/AuthContext';
import { Link } from 'react-router-dom';
import './ResearcherDashboard.css';

const ResearcherDashboard = () => {
    const { user } = useAuth();
    const [myArtifacts, setMyArtifacts] = useState([]);
    const [selectedFile, setSelectedFile] = useState(null);
    const [loading, setLoading] = useState(true);
    const [uploadMessage, setUploadMessage] = useState(null);
    const [listError, setListError] = useState(null);

    const fetchArtifacts = async () => {
        setLoading(true);
        setListError(null);
        try {
            const response = await api.get('/api/store-artifacts/my-artifacts'); // Yeni endpoint
            setMyArtifacts(response.data);
        } catch (err) {
            console.error("Error loading artifacts:", err);
            setListError("Your artifacts could not be loaded.");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchArtifacts();
    }, []);

    const handleFileChange = (e) => {
        setSelectedFile(e.target.files[0]);
        setUploadMessage(null);
    };

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
            // Yeni endpoint (store-artifacts)
            const response = await api.post('/api/store-artifacts/upload', formData, {
                headers: { 'Content-Type': 'multipart/form-data' }
            });
            setUploadMessage({ type: 'success', text: 'File uploaded successfully!' });
            setSelectedFile(null);
            if (document.getElementById('file-input')) {
                document.getElementById('file-input').value = null;
            }
            fetchArtifacts(); 

        } catch (err) {
            console.error("File upload error:", err);
            const isDup = err?.response?.status === 409;
            setUploadMessage({ 
                type: 'error', 
                text: isDup ? 'This file already exists (Duplicate).' : ('File could not be uploaded. (' + (err.response?.data?.error || err.message) + ')')
            });
        }
    };

    // --- YENİ: DOSYA AÇMA FONKSİYONU ---
    const handleDownload = async (artifact) => {
        try {
            const response = await api.get(`/api/store-artifacts/download/${artifact.id}`, {
                responseType: 'blob'
            });
            const url = window.URL.createObjectURL(new Blob([response.data], { type: artifact.mimeType }));
            const link = document.createElement('a');
            link.href = url;
            
            // Tarayıcıda açılabilirse yeni sekmede aç, yoksa indir
            if (artifact.mimeType === 'application/pdf' || artifact.mimeType.startsWith('image/') || artifact.mimeType.startsWith('text/')) {
                 link.target = "_blank";
            } else {
                 link.setAttribute('download', artifact.filename);
            }
            
            document.body.appendChild(link);
            link.click();
            link.parentNode.removeChild(link);
            setTimeout(() => window.URL.revokeObjectURL(url), 1000);
        } catch (err) {
            console.error("Download error:", err);
            alert("Could not open file.");
        }
    };

    const messageClass = uploadMessage ? `upload-message ${uploadMessage.type}` : 'upload-message';

    return (
        <div className="researcher-dashboard">
            <div className="dashboard-header">
                <h1>Dashboard</h1>
                <p style={{marginTop: '-1.5rem', color: '#ccc'}}>Welcome, {user.name}!</p>
            </div>

            <div className="dashboard-grid">
                <div className="dashboard-card">
                    <div className="card-header"><h2>Active Studies</h2></div>
                    <div className="card-content stat-card-content">5 <p>Total studies you are running</p></div>
                </div>

                <div className="dashboard-card">
                    <div className="card-header"><h2>Total Submissions</h2></div>
                    <div className="card-content stat-card-content">128 <p>Completed evaluations by participants</p></div>
                </div>

                <div className="dashboard-card">
                    <div className="card-header"><h2>Upload New Artifact</h2></div>
                    <div className="card-content">
                        <form onSubmit={handleUploadSubmit} className="upload-form">
                            <input type="file" id="file-input" onChange={handleFileChange} />
                            <button type="submit">Upload</button>
                        </form>
                        {uploadMessage && <p className={messageClass}>{uploadMessage.text}</p>}
                    </div>
                </div>

                <div className="dashboard-card full-width">
                    <div className="card-header"><h2>My Uploaded Artifacts</h2></div>
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
                                        <th>Type</th>
                                        <th>Version</th>
                                        <th>Uploaded At</th>
                                        <th>Action</th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    {myArtifacts.map(artifact => (
                                        <tr key={artifact.id}>
                                            <td>{artifact.id}</td>
                                            <td>{artifact.filename}</td>
                                            <td>{artifact.mimeType}</td>
                                            <td>v{artifact.versionNumber}</td>
                                            <td>{new Date(artifact.createdAt).toLocaleString()}</td>
                                            <td>
                                                <button 
                                                    className="form-button form-button-secondary" 
                                                    style={{padding: '0.4rem 0.8rem', fontSize: '0.8rem', width: 'auto'}}
                                                    onClick={() => handleDownload(artifact)}
                                                >
                                                    Open
                                                </button>
                                            </td>
                                        </tr>
                                    ))}
                                    </tbody>
                                </table>
                            )
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default ResearcherDashboard;