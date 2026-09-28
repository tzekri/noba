import { Navigate, Route, Routes } from "react-router-dom";
import { RequireRole } from "./auth";
import Landing from "./pages/Landing";
import Login from "./pages/Login";
import Register from "./pages/Register";
import TakeTicket from "./pages/client/TakeTicket";
import TicketTrack from "./pages/client/TicketTrack";
import Display from "./pages/Display";
import AgentDesk from "./pages/agent/AgentDesk";
import AdminLayout from "./pages/admin/AdminLayout";
import Branches from "./pages/admin/Branches";
import BranchDetail from "./pages/admin/BranchDetail";
import Staff from "./pages/admin/Staff";
import StatsPage from "./pages/admin/StatsPage";
import SuperOrgs from "./pages/super/SuperOrgs";

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Landing />} />
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />

      {/* Client (sans compte) */}
      <Route path="/q/:code" element={<TakeTicket />} />
      <Route path="/t/:token" element={<TicketTrack />} />

      {/* Écran TV de la salle d'attente */}
      <Route path="/display/:code" element={<Display />} />

      {/* Personnel */}
      <Route
        path="/agent"
        element={
          <RequireRole roles={["AGENT", "ORG_ADMIN"]}>
            <AgentDesk />
          </RequireRole>
        }
      />
      <Route
        path="/admin"
        element={
          <RequireRole roles={["ORG_ADMIN"]}>
            <AdminLayout />
          </RequireRole>
        }
      >
        <Route index element={<Branches />} />
        <Route path="branches/:id" element={<BranchDetail />} />
        <Route path="staff" element={<Staff />} />
        <Route path="stats" element={<StatsPage />} />
      </Route>
      <Route
        path="/super"
        element={
          <RequireRole roles={["SUPER_ADMIN"]}>
            <SuperOrgs />
          </RequireRole>
        }
      />

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
