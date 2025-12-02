import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { api } from '../context/AuthContext';
import { useStudyPermissions } from '../hooks/useStudyPermissions';
import './dashboards/ResearcherDashboard.css';
import './Forms.css';

const roleOptions = ['OWNER', 'EDITOR', 'REVIEWER', 'VIEWER'];

const StudyCollaborators = () => {
    const { studyId } = useParams();
    const { data: permissions, loading: permissionsLoading, error: permissionsError } = useStudyPermissions(studyId);

    const [collaborators, setCollaborators] = useState([]);
    const [researchers, setResearchers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [message, setMessage] = useState(null);
    const [formState, setFormState] = useState({
        userId: '',
        email: '',
        role: 'VIEWER'
    });

    const canInvite = permissions?.canInvite;
    const selectableRoles = permissions?.role === 'EDITOR'
        ? ['REVIEWER', 'VIEWER']
        : roleOptions;

    const showMessage = (type, text) => {
        setMessage({ type, text });
        setTimeout(() => setMessage(null), 4000);
    };

    const fetchCollaborators = async () => {
        setLoading(true);
        try {
            const response = await api.get(`/api/studies/${studyId}/collaborators`);
            setCollaborators(response.data);
        } catch (err) {
            showMessage('error', err.response?.data?.message || 'Collaborators could not be loaded.');
        } finally {
            setLoading(false);
        }
    };

    const fetchResearchers = async () => {
        try {
            const response = await api.get('/api/users/researchers');
            setResearchers(response.data);
        } catch (err) {
            showMessage('error', 'Researchers could not be loaded.');
        }
    };

    useEffect(() => {
        fetchResearchers();
    }, []);

    useEffect(() => {
        if (permissionsLoading) {
            return;
        }
        if (permissions?.canView) {
            fetchCollaborators();
        } else if (permissions && !permissions.canView) {
            setCollaborators([]);
        }
    // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [permissions, permissionsLoading]);

    const handleAddCollaborator = async (e) => {
        e.preventDefault();
        if (!canInvite) {
            showMessage('error', 'You do not have permission to invite collaborators.');
            return;
        }

        if (!formState.userId && !formState.email) {
            showMessage('error', 'Please select a researcher or enter an email.');
            return;
        }

        try {
            await api.post(`/api/studies/${studyId}/collaborators`, {
                userId: formState.userId || null,
                email: formState.email || null,
                role: formState.role
            });
            showMessage('success', 'Collaborator added successfully.');
            setFormState({ userId: '', email: '', role: formState.role });
            fetchCollaborators();
        } catch (err) {
            showMessage('error', err.response?.data?.message || 'Unable to add collaborator.');
        }
    };

    const handleRoleChange = async (collaboratorId, nextRole) => {
        if (!canInvite) {
            showMessage('error', 'You do not have permission to manage collaborators.');
            return;
        }

        try {
            await api.patch(`/api/studies/${studyId}/collaborators/${collaboratorId}`, {
                role: nextRole
            });
            fetchCollaborators();
            showMessage('success', 'Role updated.');
        } catch (err) {
            showMessage('error', err.response?.data?.message || 'Unable to change role.');
        }
    };

    const handleRemove = async (collaboratorId) => {
        if (!canInvite) {
            showMessage('error', 'You do not have permission to manage collaborators.');
            return;
        }
        if (!window.confirm('Remove this collaborator?')) {
            return;
        }
        try {
            await api.delete(`/api/studies/${studyId}/collaborators/${collaboratorId}`);
            fetchCollaborators();
            showMessage('success', 'Collaborator removed.');
        } catch (err) {
            showMessage('error', err.response?.data?.message || 'Unable to remove collaborator.');
        }
    };

    const renderCollaborators = () => {
        if (permissionsLoading || loading) {
            return <p>Loading collaborators...</p>;
        }
        if (!permissions?.canView) {
            return <p style={{ color: '#bbb' }}>You only have read-only access to this study.</p>;
        }
        if (collaborators.length === 0) {
            return <p>No collaborators have been added yet.</p>;
        }
        return (
            <table className="artifact-list-table">
                <thead>
                    <tr>
                        <th>Name</th>
                        <th>Email</th>
                        <th>Role</th>
                        <th>Added</th>
                        {canInvite && <th>Actions</th>}
                    </tr>
                </thead>
                <tbody>
                {collaborators.map(collab => (
                    <tr key={collab.id}>
                        <td>{collab.name} (ID: {collab.userId})</td>
                        <td>{collab.email}</td>
                        <td>
                            {canInvite ? (
                                <select
                                    className="form-select"
                                    value={collab.role}
                                    onChange={(e) => handleRoleChange(collab.id, e.target.value)}
                                >
                                    {selectableRoles.map(role => (
                                        <option key={role} value={role}>{role}</option>
                                    ))}
                                    {!selectableRoles.includes(collab.role) && (
                                        <option value={collab.role}>{collab.role}</option>
                                    )}
                                </select>
                            ) : (
                                collab.role
                            )}
                        </td>
                        <td>{new Date(collab.addedAt).toLocaleString()}</td>
                        {canInvite && (
                            <td>
                                <button
                                    className="form-button form-button-secondary"
                                    type="button"
                                    style={{ width: 'auto', padding: '0.25rem 0.75rem' }}
                                    onClick={() => handleRemove(collab.id)}
                                >
                                    Remove
                                </button>
                            </td>
                        )}
                    </tr>
                ))}
                </tbody>
            </table>
        );
    };

    return (
        <div className="researcher-dashboard">
            <div className="dashboard-header">
                <h1>Study Collaborators</h1>
                <p style={{ color: '#ccc', marginTop: '-1rem' }}>Study ID: {studyId}</p>
            </div>

            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '1rem' }}>
                <Link to="/researcher-dashboard/manage-studies">
                    <button className="form-button form-button-secondary" style={{ width: 'auto' }}>
                        ← Back to Studies
                    </button>
                </Link>
                <Link to={`/researcher-dashboard/study/${studyId}`}>
                    <button className="form-button form-button-secondary" style={{ width: 'auto' }}>
                        View Study Stats →
                    </button>
                </Link>
            </div>

            {message && (
                <div className={`form-message ${message.type}`}>
                    {message.text}
                </div>
            )}
            {permissionsError && (
                <div className="form-message error">{permissionsError}</div>
            )}

            {canInvite && (
                <div className="dashboard-card" style={{ marginBottom: '1.5rem' }}>
                    <div className="card-header">
                        <h2>Invite Collaborator</h2>
                    </div>
                    <div className="card-content">
                        <form onSubmit={handleAddCollaborator}>
                            <div className="form-group">
                                <label className="form-label">Select Researcher</label>
                                <select
                                    className="form-select"
                                    value={formState.userId}
                                    onChange={(e) => setFormState(prev => ({
                                        ...prev,
                                        userId: e.target.value,
                                        email: ''
                                    }))}
                                >
                                    <option value="">-- Choose Researcher --</option>
                                    {researchers.map(user => (
                                        <option key={user.id} value={user.id}>
                                            {user.name} ({user.email})
                                        </option>
                                    ))}
                                </select>
                            </div>

                            <div className="form-group">
                                <label className="form-label">Or invite by email</label>
                                <input
                                    type="email"
                                    className="form-input"
                                    value={formState.email}
                                    placeholder="researcher@example.com"
                                    onChange={(e) => setFormState(prev => ({
                                        ...prev,
                                        email: e.target.value,
                                        userId: ''
                                    }))}
                                />
                            </div>

                            <div className="form-group">
                                <label className="form-label">Role</label>
                                <select
                                    className="form-select"
                                    value={formState.role}
                                    onChange={(e) => setFormState(prev => ({ ...prev, role: e.target.value }))}
                                >
                                    {selectableRoles.map(role => (
                                        <option key={role} value={role}>{role}</option>
                                    ))}
                                </select>
                                {permissions?.role === 'EDITOR' && (
                                    <small style={{ color: '#aaa' }}>
                                        Editors can grant Reviewer or Viewer access only.
                                    </small>
                                )}
                            </div>

                            <button type="submit" className="form-button form-button-submit">
                                Invite Collaborator
                            </button>
                        </form>
                    </div>
                </div>
            )}

            <div className="dashboard-card">
                <div className="card-header">
                    <h2>Current Collaborators ({collaborators.length})</h2>
                </div>
                <div className="card-content">
                    {renderCollaborators()}
                </div>
            </div>
        </div>
    );
};

export default StudyCollaborators;


