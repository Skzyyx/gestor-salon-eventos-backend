import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { ThemeProvider } from './context/ThemeContext';
import { Layout } from './components/Layout';
import Dashboard from './pages/Dashboard';
import Login from './pages/Login';
import Configuracion from './pages/Configuracion';
import TurnosPage from './pages/TurnosPage';
import ServiciosPage from './pages/ServiciosPage';
import PaquetesPage from './pages/PaquetesPage';
import ClientesPage from './pages/ClientesPage';
import PersonalPage from './pages/PersonalPage';
import PlataformaPage from './pages/PlataformaPage';
import UnauthorizedPage from './pages/UnauthorizedPage';

// Guard general: si no hay usuario logueado, lo manda al login.
const ProtectedRoute = ({ children }: { children: React.ReactNode }) => {
  const { user } = useAuth();
  if (!user) {
    return <Navigate to="/login" replace />;
  }
  return children;
};

// Guard de PLATAFORMA: además de estar logueado, exige rol SUPERADMIN.
// Si un ADMIN normal intenta entrar a /plataforma, lo regresamos a su inicio.
const SuperAdminRoute = ({ children }: { children: React.ReactNode }) => {
  const { user } = useAuth();
  if (!user) {
    return <Navigate to="/login" replace />;
  }
  if (user.rol !== 'SUPERADMIN') {
    return <Navigate to="/" replace />;
  }
  return children;
};

// Decide qué mostrar en la raíz "/" según el rol:
// - El SUPERADMIN no tiene negocio, así que el Dashboard normal le reventaría
//   (intentaría cargar /negocios/me). Lo enviamos directo a su panel de plataforma.
// - El ADMIN ve su Dashboard de siempre.
const InicioSegunRol = () => {
  const { user } = useAuth();
  if (user?.rol === 'SUPERADMIN') {
    return <Navigate to="/plataforma" replace />;
  }
  return <Dashboard />;
};

function App() {
  return (
    <ThemeProvider>
      <AuthProvider>
        <Router>
          <Routes>
            {/* RUTA PÚBLICA */}
            <Route path="/login" element={<Login />} />
            <Route path="/restringido" element={<UnauthorizedPage />} />

            {/* RUTAS PROTEGIDAS (comparten el Layout con sidebar) */}
            <Route path="/" element={
              <ProtectedRoute>
                <Layout />
              </ProtectedRoute>
            }>
              {/* La raíz decide a dónde ir según el rol */}
              <Route index element={<InicioSegunRol />} />

              {/* Rutas de NEGOCIO (las usa el ADMIN) */}
              <Route path="turnos" element={<TurnosPage />} />
              <Route path="servicios" element={<ServiciosPage />} />
              <Route path="paquetes" element={<PaquetesPage />} />
              <Route path="clientes" element={<ClientesPage />} />
              <Route path="personal" element={<PersonalPage />} />
              <Route path="configuracion" element={<Configuracion />} />

              {/* Ruta de PLATAFORMA (solo SUPERADMIN, doblemente protegida) */}
              <Route path="plataforma" element={
                <SuperAdminRoute>
                  <PlataformaPage />
                </SuperAdminRoute>
              } />
            </Route>

            {/* Cualquier ruta desconocida regresa al inicio */}
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </Router>
      </AuthProvider>
    </ThemeProvider>
  );
}

export default App;
