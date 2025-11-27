// src/components/ProtectedRoute.jsx
import React from 'react';
import { useAuth } from '../context/AuthContext';
import { Navigate, Outlet } from 'react-router-dom';

// Bu component, hangi role izin verildiğini 'allowedRole' prop'u ile alacak
const ProtectedRoute = ({ allowedRole }) => {
    const { isAuthenticated, user } = useAuth();

    // 1. Kullanıcı giriş yapmış mı?
    if (!isAuthenticated) {
        // Giriş yapmamışsa, /login sayfasına yönlendir
        return <Navigate to="/login" replace />;
    }

    // 2. Kullanıcı giriş yapmış, peki rolü uygun mu?
    if (user.role !== allowedRole) {
        // Rolü uygun değilse, (şimdilik) ana sayfaya yönlendir
        // (İleride bir "Yetkiniz Yok" sayfası yapılabilir)
        return <Navigate to="/" replace />;
    }

    // 3. Kullanıcı giriş yapmış VE rolü uygunsa:
    // İçerideki sayfayı (component'i) göster
    return <Outlet />;
};

export default ProtectedRoute;