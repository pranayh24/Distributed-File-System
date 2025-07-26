import './App.css'
import {AuthProvider} from "./context/AuthContext.tsx";
import { ProtectedRoute} from "./components/common/ProtectecRoute.tsx";
import {Routes, Route, Navigate} from 'react-router-dom';
import { AuthPage } from './components/auth/AuthPage.tsx';
import FilesPage from "./pages/Files.tsx";
import TrashPage from "./pages/Trash.tsx";
import AccountPage from "./pages/Account.tsx";
import DashboardPage from "./pages/Dashboard.tsx";


function App() {

  return (
      <AuthProvider>
        <Routes>
            <Route path="/auth" element={<AuthPage />}/>
            <Route
                path="/*"
                element={
                    <ProtectedRoute>
                        <Routes>
                            <Route path="/dashboard" element={<DashboardPage />} />
                            <Route path="/files" element={<FilesPage />} />
                            <Route path="/trash" element={<TrashPage />} />
                            <Route path="/account" element={<AccountPage />} />
                            <Route path="*" element={<Navigate to="/dashboard" replace />} />
                        </Routes>
                    </ProtectedRoute>
                }
            />
        </Routes>
      </AuthProvider>
  );
}

export default App
