// Dosya Yolu: src/pages/Login.jsx
// (TAM VE GÜNCELLENMİŞ KOD)

import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { api } from '../context/AuthContext';
import './Login.css';

// Logoyu import et
import CodeArenaLogo from '../assets/CodeArenaLogo.png';

const Login = () => {
    // --- (LOGIC BÖLÜMÜ DEĞİŞMEDİ) ---
    const [name, setName] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState(null);
    const navigate = useNavigate();
    const { login } = useAuth();

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError(null);
        try {
            const response = await api.post('/login', {
                name: name,
                password: password
            });
            const authData = response.data;
            login(authData);
            if (authData.role === 'RESEARCHER') {
                navigate('/researcher-dashboard');
            } else if (authData.role === 'PARTICIPANT') {
                navigate('/participant-dashboard');
            } else {
                navigate('/');
            }
        } catch (err) {
            console.error('Login failed:', err);
            if (err.response && (err.response.status === 401 || err.response.status === 404)) {
                setError('Geçersiz kullanıcı adı veya şifre.');
            } else {
                setError('Giriş sırasında bir hata oluştu. Lütfen tekrar deneyin.');
            }
        }
    };
    // --- (LOGIC BÖLÜMÜ SONU) ---


    return (
        <div className="login-page-container">

            {/* 1. Sol Sütun (Grafik) */}
            <div className="login-left-column">

                <img
                    src={CodeArenaLogo}
                    alt="CodeArena Logo"
                    className="login-logo"
                />

                <h1>CodeArena: Artifact Evaluation</h1>
                <p>Evaluate artifacts side by side effortlessly.</p>
            </div>

            {/* 2. Sağ Sütun (Form) */}
            <div className="login-right-column">

                {/* --- İSTEĞİN ÜZERİNE "ARTIFACT DOCUMENT" YAZISI SİLİNDİ --- */}

                <div className="login-form-container">
                    <h2>Log in</h2>

                    <form onSubmit={handleSubmit}>
                        <div>
                            <label htmlFor="name">Username</label>
                            <input
                                type="text"
                                id="name"
                                value={name}
                                onChange={(e) => setName(e.target.value)}
                                placeholder="Kullanıcı adınızı girin"
                                required
                            />
                        </div>

                        <div>
                            <label htmlFor="password">Password</label>
                            <input
                                type="password"
                                id="password"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                placeholder="********"
                                required
                            />
                        </div>

                        {error && <p className="login-error-message">{error}</p>}

                        <button type="submit" className="login-button">
                            Log in
                        </button>

                        <a href="#" className="login-forgot-password">
                            Forgot your password?
                        </a>
                    </form>
                </div>
            </div>
        </div>
    );
};

export default Login;