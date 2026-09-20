import { Navigate, Route, Routes } from "react-router-dom";

export default function AppRouter() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/players" replace />} />

      <Route path="/players" element={<div>Players</div>} />

      <Route path="*" element={<Navigate to="/players" replace />} />
    </Routes>
  );
}