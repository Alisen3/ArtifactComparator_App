// src/pages/Welcome.jsx
import React from 'react';
import { useAuth } from '../context/AuthContext'; // Auth context'imizi import et

const Welcome = () => {
    const { user, logout } = useAuth(); // Global state'den 'user' ve 'logout'u al

    if (!user) {
        return <p>Giriş yapmadınız.</p>;
    }

    return (
        <div>
            <h2>Hoş Geldin, {user.name}!</h2>
            <p>Rolünüz: <strong>{user.role}</strong></p>
            <p>Email: {user.email}</p>
            <button onClick={logout}>Çıkış Yap (Logout)</button>
        </div>
    );
};

export default Welcome;