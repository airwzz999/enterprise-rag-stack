import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App.tsx'
import './styles/global.css'
import './styles/layout.css'
import './styles/dashboard.css'
import './styles/forgot-password.css'
import './styles/pages.css'

ReactDOM.createRoot(document.getElementById('root')!).render(
  // StrictMode is temporarily disabled in development to avoid duplicate renders triggering duplicate API calls
  import.meta.env.DEV ? <App /> : <React.StrictMode><App /></React.StrictMode>
)
