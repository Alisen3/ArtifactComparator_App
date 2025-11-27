// src/main.jsx
import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App.jsx'
import './index.css'
import { BrowserRouter } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext' // 1. Adım: Import et

ReactDOM.createRoot(document.getElementById('root')).render(
    <React.StrictMode>
        <BrowserRouter>
            <AuthProvider> {/* 2. Adım: <App />'i AuthProvider ile sarmala */}
                <App />
            </AuthProvider>
        </BrowserRouter>
    </React.StrictMode>,
)