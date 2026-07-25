import { NavLink, Navigate, Route, Routes } from "react-router-dom";
import HomePage from "./pages/HomePage";
import BooksPage from "./pages/BooksPage";
import UsersPage from "./pages/UsersPage";
import LoansPage from "./pages/LoansPage";
import "./App.css";

function App() {
  return (
    <div className="app">
      <header className="navbar">
        <div className="brand">Library Admin</div>

        <nav className="nav-links">
          <NavLink to="/app/home">Home</NavLink>
          <NavLink to="/app/books">Books</NavLink>
          <NavLink to="/app/users">Users</NavLink>
          <NavLink to="/app/loans">Loans</NavLink>
        </nav>
      </header>

      <main className="main-content">
        <Routes>
          <Route path="/" element={<Navigate to="/app/home" replace />} />

          <Route path="/app/home" element={<HomePage />} />
          <Route path="/app/books" element={<BooksPage />} />
          <Route path="/app/users" element={<UsersPage />} />
          <Route path="/app/loans" element={<LoansPage />} />

          <Route path="*" element={<Navigate to="/app/home" replace />} />
        </Routes>
      </main>
    </div>
  );
}

export default App;
