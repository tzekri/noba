import { Outlet } from "react-router-dom";
import { useAuth } from "../../auth";
import { StaffHeader, staffLinks } from "../../components/common";
import "../agent/agent.css";

export default function AdminLayout() {
  const { user } = useAuth();
  return (
    <>
      <StaffHeader links={staffLinks(user?.role)} />
      <main className="staff-main">
        <div className="container">
          <Outlet />
        </div>
      </main>
    </>
  );
}
