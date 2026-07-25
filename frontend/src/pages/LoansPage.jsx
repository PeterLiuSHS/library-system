import { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import {
  borrowBook,
  fetchAdminLoans,
  fetchLoansByUser,
  returnBook,
} from "../features/loans/loansSlice";

function LoansPage() {
  const dispatch = useDispatch();
  const loans = useSelector((state) => state.loans.items);
  const status = useSelector((state) => state.loans.status);
  const error = useSelector((state) => state.loans.error);

  const adminLoans = useSelector((state) => state.loans.adminItems);
  const adminStatus = useSelector((state) => state.loans.adminStatus);
  const adminError = useSelector((state) => state.loans.adminError);
  const adminFilter = useSelector((state) => state.loans.adminFilter);

  const [userId, setUserId] = useState("");
  const [borrowData, setBorrowData] = useState({
    userId: "",
    bookId: "",
    days: "15",
  });

  useEffect(() => {
    if (adminStatus === "idle") {
      dispatch(fetchAdminLoans("active"));
    }
  }, [adminStatus, dispatch]);

  function handleAdminFilterChange(filter) {
    dispatch(fetchAdminLoans(filter));
  }

  function handleSearch(event) {
    event.preventDefault();
    dispatch(fetchLoansByUser(userId));
  }

  function handleBorrowChange(event) {
    const { name, value } = event.target;

    setBorrowData({
      ...borrowData,
      [name]: value,
    });
  }

  async function handleBorrowSubmit(event) {
    event.preventDefault();

    try {
      await dispatch(
        borrowBook({
          userId: Number(borrowData.userId),
          bookId: Number(borrowData.bookId),
          days: Number(borrowData.days),
        }),
      ).unwrap();

      dispatch(fetchLoansByUser(borrowData.userId));
      dispatch(fetchAdminActiveLoans(adminFilter));

      setUserId(borrowData.userId);

      setBorrowData({
        userId: "",
        bookId: "",
        days: "15",
      });
    } catch (error) {}
  }

  async function handleReturn(userId, bookId) {
    try {
      await dispatch(returnBook({ userId, bookId })).unwrap();
      dispatch(fetchLoansByUser(userId));
      dispatch(fetchAdminLoans(adminFilter));
    } catch (error) {
      // The rejected action stores the error in Redux.
    }
  }

  return (
    <section>
      <h2>Loans</h2>
      <p>Create loan records, return books, and view loan history.</p>

      <form className="form-card" onSubmit={handleBorrowSubmit}>
        <h3>Borrow Book</h3>

        <input
          name="userId"
          placeholder="User ID"
          value={borrowData.userId}
          onChange={handleBorrowChange}
          required
          type="number"
        />

        <input
          name="bookId"
          placeholder="Book ID"
          value={borrowData.bookId}
          onChange={handleBorrowChange}
          required
          type="number"
        />

        <input
          name="days"
          placeholder="Days"
          value={borrowData.days}
          onChange={handleBorrowChange}
          required
          type="number"
        />

        <button type="submit">Borrow</button>
      </form>

      <form className="form-card" onSubmit={handleSearch}>
        <h3>Find Loans by User</h3>

        <input
          placeholder="User ID"
          value={userId}
          onChange={(event) => setUserId(event.target.value)}
          required
          type="number"
        />

        <button type="submit">Search Loans</button>
      </form>

      {status === "loading" && <p>Loading loans...</p>}

      {status === "failed" && <p className="error-message">{error}</p>}

      {status === "succeeded" && (
        <table className="data-table">
          <thead>
            <tr>
              <th>Loan ID</th>
              <th>User ID</th>
              <th>Book ID</th>
              <th>Borrow Date</th>
              <th>Due Date</th>
              <th>Return Date</th>
              <th>Action</th>
            </tr>
          </thead>

          <tbody>
            {loans.map((loan) => (
              <tr key={loan.id}>
                <td>{loan.id}</td>
                <td>{loan.userId}</td>
                <td>{loan.bookId}</td>
                <td>{loan.loanDate}</td>
                <td>{loan.dueDate}</td>
                <td>{loan.returnDate || "Not returned"}</td>
                <td>
                  {!loan.returnDate && (
                    <button
                      onClick={() => handleReturn(loan.userId, loan.bookId)}
                    >
                      Return
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      <div className="loans-section">
        <h3>Loan Records</h3>
        <p>View all, active, or returned loan records.</p>

        <div className="filter-buttons">
          <button
            type="button"
            className={adminFilter === "all" ? "active-filter" : ""}
            onClick={() => handleAdminFilterChange("all")}
          >
            All Loans
          </button>

          <button
            type="button"
            className={adminFilter === "active" ? "active-filter" : ""}
            onClick={() => handleAdminFilterChange("active")}
          >
            Active Loans
          </button>

          <button
            type="button"
            className={adminFilter === "history" ? "active-filter" : ""}
            onClick={() => handleAdminFilterChange("history")}
          >
            Returned Loans
          </button>
        </div>

        {adminStatus === "loading" && <p>Loading loan records...</p>}

        {adminStatus === "failed" && (
          <p className="error-message">{adminError}</p>
        )}

        {adminStatus === "succeeded" && adminLoans.length === 0 && (
          <p>No loan records were found.</p>
        )}

        {adminStatus === "succeeded" && adminLoans.length > 0 && (
          <table className="data-table">
            <thead>
              <tr>
                <th>Loan ID</th>
                <th>User ID</th>
                <th>Book ID</th>
                <th>Borrow Date</th>
                <th>Due Date</th>
                <th>Return Date</th>
                <th>Status</th>
              </tr>
            </thead>

            <tbody>
              {adminLoans.map((loan) => (
                <tr key={loan.id}>
                  <td>{loan.id}</td>
                  <td>{loan.userId}</td>
                  <td>{loan.bookId}</td>
                  <td>{loan.loanDate}</td>
                  <td>{loan.dueDate}</td>
                  <td>{loan.returnDate || "Not returned"}</td>
                  <td>{loan.returnDate ? "Returned" : "Active"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </section>
  );
}

export default LoansPage;
