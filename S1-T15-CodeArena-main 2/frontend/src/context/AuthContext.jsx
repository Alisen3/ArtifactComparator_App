// src/context/AuthContext.jsx
import React, { createContext, useState, useContext, useEffect } from 'react';
import axios from 'axios'; // axios'u import ediyoruz

// --- Adım 1: Axios için bir API interceptor'ı (araya girici) oluşturma ---
// Bu, "api" adında özel bir axios örneği oluşturur
export const API_BASE_URL = 'http://localhost:8080';
export const api = axios.create({
    baseURL: API_BASE_URL // Tüm istekler bu adrese gidecek
});

// Bu "interceptor", 'api' ile yapılan HERhangi BİR istekten önce çalışır
api.interceptors.request.use(
    (config) => {
        // localStorage'dan token'ı al
        const token = localStorage.getItem('accessToken');
        if (token) {
            // Eğer token varsa, isteğin 'Authorization' başlığına ekle
            config.headers['Authorization'] = `Bearer ${token}`;
        }
        return config; // Değiştirilmiş config ile devam et
    },
    (error) => {
        // İstek hatası varsa reddet
        return Promise.reject(error);
    }
);
// --- --- --- --- --- --- --- --- --- --- --- --- --- ---

// 1. Context'in kendisini oluştur
const AuthContext = createContext();

// 2. Kolay hook
export const useAuth = () => {
    return useContext(AuthContext);
};

// 3. Provider component'i
export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(null);
    const [isAuthenticated, setIsAuthenticated] = useState(false);
    const [loading, setLoading] = useState(true); // Sayfa ilk yüklendiğinde token'ı kontrol et

    // Sayfa ilk yüklendiğinde (veya yenilendiğinde) SADECE BİR KEZ çalış
    useEffect(() => {
        const token = localStorage.getItem('accessToken');
        const userData = localStorage.getItem('user');

        if (token && userData) {
            // Eğer token ve kullanıcı bilgisi varsa, state'i bunlarla doldur
            setUser(JSON.parse(userData));
            setIsAuthenticated(true);
        }
        setLoading(false); // Yüklemeyi bitir
    }, []);

    // Giriş yapma fonksiyonu (Artık backend'den gelen AuthResponse'u alacak)
    const login = (authData) => {
        // authData = { accessToken, id, name, email, role }

        // Token'ı localStorage'a kaydet
        localStorage.setItem('accessToken', authData.accessToken);

        // Kullanıcı verisini (token hariç) ayır ve string olarak kaydet
        const userData = {
            id: authData.id,
            name: authData.name,
            email: authData.email,
            role: authData.role
        };
        localStorage.setItem('user', JSON.stringify(userData));

        // State'i güncelle
        setUser(userData);
        setIsAuthenticated(true);
    };

    // Çıkış yapma fonksiyonu
    const logout = () => {
        // localStorage'dan her şeyi temizle
        localStorage.removeItem('accessToken');
        localStorage.removeItem('user');

        // State'i sıfırla
        setUser(null);
        setIsAuthenticated(false);
    };

    const value = {
        user,
        isAuthenticated,
        loading, // Yükleme durumunu da dışarıya ver
        login,
        logout
    };

    // Eğer sayfa ilk yükleniyorsa (loading=true) hiçbir şey gösterme
    // Bu, korumalı rotaların state güncellenmeden tetiklenmesini engeller
    if (loading) {
        return <div>Uygulama Yükleniyor...</div>;
    }

    return (
        <AuthContext.Provider value={value}>
            {children}
        </AuthContext.Provider>
    );
};
