// Dosya Yolu: src/pages/Register.jsx
// (TAM VE GÜNCELLENMİŞ KOD)

import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom'; // Yönlendirme ve Link için
import { useAuth } from '../context/AuthContext'; // Global state için
import { api } from '../context/AuthContext'; // API istekleri için (axios yerine)

// 1. Adım: Login sayfasıyla aynı CSS'i ve logoyu import et
import './Login.css';
import CodeArenaLogo from '../assets/CodeArenaLogo.png';

const Register = () => {
    // --- (Form state'leri değişmedi) ---
    const [name, setName] = useState('');
    const [email, setEmail] = useState(''); // Ekstra alan
    const [password, setPassword] = useState('');
    const [error, setError] = useState(null); // Hata mesajı için

    // --- (Hook'lar değişmedi) ---
    const navigate = useNavigate();
    const { login } = useAuth(); // AuthContext'ten login fonksiyonunu al

    // --- 2. Adım: handleSubmit (API çağrısı ve Hata Mesajları) güncellendi ---
    const handleSubmit = async (e) => {
        e.preventDefault();
        setError(null);

        try {
            // 'axios.post' yerine 'api.post' kullan
            const response = await api.post('/register', {
                name: name,
                email: email,
                password: password
            });

            // Backend bir AuthResponse döndürür
            const userData = response.data;
            login(userData); // Kullanıcıyı otomatik olarak giriş yap (token'ı kaydet)

            // Kayıt olan kullanıcı her zaman PARTICIPANT'tır
            // Bu yüzden onu Participant Dashboard'una yönlendir
            navigate('/participant-dashboard');

        } catch (err) {
            // Hata mesajlarını İngilizce yap
            console.error('Registration failed:', err);
            if (err.response && err.response.status === 409) {
                // Backend'den gelen spesifik hata mesajını kullan (örn: "Email already exists")
                setError(err.response.data.error || 'Username or email is already in use.');
            } else {
                setError('An error occurred during registration. Please try again.');
            }
        }
    };
    // --- (Logic Sonu) ---


    // --- 3. Adım: JSX (Görünüm) tamamen 'Login.css' kullanacak şekilde yenilendi ---
    return (
        <div className="login-page-container">

            {/* 1. Sol Sütun (Logo ve Metin) */}
            <div className="login-left-column">
                <img
                    src={CodeArenaLogo}
                    alt="CodeArena Logo"
                    className="login-logo"
                />
                <h1>Create your Account</h1>
                <p>Join the platform to evaluate artifacts side by side.</p>
            </div>

            {/* 2. Sağ Sütun (Form) */}
            <div className="login-right-column">
                <div className="login-form-container">
                    <h2>Register</h2>

                    <form onSubmit={handleSubmit}>
                        <div>
                            <label htmlFor="name">Username</label>
                            <input
                                type="text"
                                id="name"
                                value={name}
                                onChange={(e) => setName(e.target.value)}
                                placeholder="Enter your username"
                                required
                            />
                        </div>

                        {/* --- YENİ EKLENEN ALAN (EMAIL) --- */}
                        <div>
                            <label htmlFor="email">Email</label>
                            <input
                                type="email"
                                id="email"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                placeholder="Enter your email address"
                                required
                            />
                        </div>
                        {/* --- --- --- --- --- --- --- */}

                        <div>
                            <label htmlFor="password">Password</label>
                            <input
                                type="password"
                                id="password"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                placeholder="Enter your password"
                                required
                            />
                        </div>

                        {/* Hata mesajını göster */}
                        {error && <p className="login-error-message">{error}</p>}

                        <button type="submit" className="login-button">
                            Create Account
                        </button>

                        {/* "Forgot password" yerine "Log in" linki */}
                        <Link to="/login" className="login-forgot-password">
                            Already have an account? Log in
                        </Link>
                    </form>
                </div>
            </div>
        </div>
    );
};

export default Register;