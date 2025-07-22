import './App.css'
import {AuthProvider} from "./context/AuthContext.tsx";
import { Dashboard } from "./pages/Dashboard.tsx";
import { ProtectedRoute} from "./components/common/ProtectecRoute.tsx";


function App() {

  return (
      <AuthProvider>
        <ProtectedRoute>
          <Dashboard />
        </ProtectedRoute>
      </AuthProvider>
  );
}

export default App
