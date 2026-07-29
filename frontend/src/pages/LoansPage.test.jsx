import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import {
  beforeEach,
  describe,
  expect,
  test,
  vi,
} from "vitest";
import { useDispatch, useSelector } from "react-redux";
import LoansPage from "./LoansPage";

const {
  mockBorrowBook,
  mockFetchAdminLoans,
  mockFetchLoansByUser,
  mockReturnBook,
} = vi.hoisted(() => ({
  mockBorrowBook: vi.fn(),
  mockFetchAdminLoans: vi.fn(),
  mockFetchLoansByUser: vi.fn(),
  mockReturnBook: vi.fn(),
}));

vi.mock("react-redux", () => ({
  useDispatch: vi.fn(),
  useSelector: vi.fn(),
}));

vi.mock("../features/loans/loansSlice", () => ({
  borrowBook: mockBorrowBook,
  fetchAdminLoans: mockFetchAdminLoans,
  fetchLoansByUser: mockFetchLoansByUser,
  returnBook: mockReturnBook,
}));

describe("LoansPage", () => {
  const mockDispatch = vi.fn();

  const activeLoan = {
    id: 1,
    userId: 10,
    bookId: 101,
    loanDate: "2026-07-01",
    dueDate: "2026-07-15",
    returnDate: null,
  };

  const returnedLoan = {
    id: 2,
    userId: 11,
    bookId: 102,
    loanDate: "2026-06-01",
    dueDate: "2026-06-15",
    returnDate: "2026-06-12",
  };

  const mockState = {
    loans: {
      items: [activeLoan, returnedLoan],
      status: "succeeded",
      error: null,

      adminItems: [activeLoan],
      adminStatus: "succeeded",
      adminError: null,
      adminFilter: "active",

      adminPage: 0,
      adminSize: 5,
      adminTotalPages: 1,
      adminTotalElements: 1,
    },
  };

  function setMockState(state) {
    useSelector.mockImplementation((selector) => selector(state));
  }

  beforeEach(() => {
    vi.clearAllMocks();

    useDispatch.mockReturnValue(mockDispatch);
    setMockState(mockState);
  });

  test("should fetch active loans when admin status is idle", () => {
    const idleState = {
      loans: {
        ...mockState.loans,
        adminItems: [],
        adminStatus: "idle",
        adminTotalElements: 0,
      },
    };

    const fetchAdminLoansAction = {
      type: "loans/fetchAdminLoans/pending",
    };

    setMockState(idleState);
    mockFetchAdminLoans.mockReturnValue(fetchAdminLoansAction);

    render(<LoansPage />);

    expect(mockFetchAdminLoans).toHaveBeenCalledWith({
      filter: "active",
      page: 0,
      size: 5,
    });

    expect(mockDispatch).toHaveBeenCalledWith(
      fetchAdminLoansAction,
    );
  });

  test("should search loans by user id", async () => {
    const user = userEvent.setup();

    const fetchLoansAction = {
      type: "loans/fetchLoansByUser/pending",
    };

    mockFetchLoansByUser.mockReturnValue(fetchLoansAction);

    render(<LoansPage />);

    const userIdInputs =
      screen.getAllByPlaceholderText("User ID");

    const searchUserIdInput = userIdInputs[1];

    await user.type(searchUserIdInput, "10");

    await user.click(
      screen.getByRole("button", {
        name: "Search Loans",
      }),
    );

    expect(searchUserIdInput).toHaveValue(10);

    expect(mockFetchLoansByUser).toHaveBeenCalledWith("10");

    expect(mockDispatch).toHaveBeenCalledWith(
      fetchLoansAction,
    );
  });

  test("should borrow a book using the entered values", async () => {
    const user = userEvent.setup();

    const borrowBookAction = {
      type: "loans/borrowBook/pending",
    };

    const fetchLoansAction = {
      type: "loans/fetchLoansByUser/pending",
    };

    const fetchAdminLoansAction = {
      type: "loans/fetchAdminLoans/pending",
    };

    const unwrap = vi.fn().mockResolvedValue({
      id: 3,
      userId: 12,
      bookId: 103,
    });

    mockBorrowBook.mockReturnValue(borrowBookAction);
    mockFetchLoansByUser.mockReturnValue(fetchLoansAction);
    mockFetchAdminLoans.mockReturnValue(
      fetchAdminLoansAction,
    );

    mockDispatch.mockImplementation((action) => {
      if (action === borrowBookAction) {
        return {
          unwrap,
        };
      }

      return action;
    });

    render(<LoansPage />);

    const userIdInputs =
      screen.getAllByPlaceholderText("User ID");

    const borrowUserIdInput = userIdInputs[0];
    const bookIdInput =
      screen.getByPlaceholderText("Book ID");
    const daysInput =
      screen.getByPlaceholderText("Days");

    await user.type(borrowUserIdInput, "12");
    await user.type(bookIdInput, "103");

    await user.clear(daysInput);
    await user.type(daysInput, "7");

    await user.click(
      screen.getByRole("button", {
        name: "Borrow",
      }),
    );

    expect(mockBorrowBook).toHaveBeenCalledWith({
      userId: 12,
      bookId: 103,
      days: 7,
    });

    expect(mockDispatch).toHaveBeenCalledWith(
      borrowBookAction,
    );

    expect(unwrap).toHaveBeenCalledTimes(1);
  });

  test("should refresh user loans and admin loans after borrowing", async () => {
    const user = userEvent.setup();

    const borrowBookAction = {
      type: "loans/borrowBook/pending",
    };

    const fetchLoansAction = {
      type: "loans/fetchLoansByUser/pending",
    };

    const fetchAdminLoansAction = {
      type: "loans/fetchAdminLoans/pending",
    };

    const unwrap = vi.fn().mockResolvedValue({
      id: 3,
      userId: 12,
      bookId: 103,
    });

    mockBorrowBook.mockReturnValue(borrowBookAction);
    mockFetchLoansByUser.mockReturnValue(fetchLoansAction);
    mockFetchAdminLoans.mockReturnValue(
      fetchAdminLoansAction,
    );

    mockDispatch.mockImplementation((action) => {
      if (action === borrowBookAction) {
        return {
          unwrap,
        };
      }

      return action;
    });

    render(<LoansPage />);

    const userIdInputs =
      screen.getAllByPlaceholderText("User ID");

    await user.type(userIdInputs[0], "12");

    await user.type(
      screen.getByPlaceholderText("Book ID"),
      "103",
    );

    await user.click(
      screen.getByRole("button", {
        name: "Borrow",
      }),
    );

    expect(mockFetchLoansByUser).toHaveBeenCalledWith("12");

    expect(mockFetchAdminLoans).toHaveBeenCalledWith({
      filter: "active",
      page: 0,
      size: 5,
    });

    expect(mockDispatch).toHaveBeenCalledWith(
      fetchLoansAction,
    );

    expect(mockDispatch).toHaveBeenCalledWith(
      fetchAdminLoansAction,
    );
  });
    test("should return an active loan", async () => {
    const user = userEvent.setup();

    const returnBookAction = {
      type: "loans/returnBook/pending",
    };

    const fetchLoansAction = {
      type: "loans/fetchLoansByUser/pending",
    };

    const fetchAdminLoansAction = {
      type: "loans/fetchAdminLoans/pending",
    };

    const unwrap = vi.fn().mockResolvedValue({
      ...activeLoan,
      returnDate: "2026-07-10",
    });

    mockReturnBook.mockReturnValue(returnBookAction);
    mockFetchLoansByUser.mockReturnValue(fetchLoansAction);
    mockFetchAdminLoans.mockReturnValue(
      fetchAdminLoansAction,
    );

    mockDispatch.mockImplementation((action) => {
      if (action === returnBookAction) {
        return {
          unwrap,
        };
      }

      return action;
    });

    render(<LoansPage />);

    await user.click(
      screen.getByRole("button", {
        name: "Return",
      }),
    );

    expect(mockReturnBook).toHaveBeenCalledWith({
      userId: 10,
      bookId: 101,
    });

    expect(mockDispatch).toHaveBeenCalledWith(
      returnBookAction,
    );

    expect(unwrap).toHaveBeenCalledTimes(1);

    expect(mockFetchLoansByUser).toHaveBeenCalledWith(10);

    expect(mockFetchAdminLoans).toHaveBeenCalledWith({
      filter: "active",
      page: 0,
      size: 5,
    });
  });

  test("should switch to returned loan records", async () => {
    const user = userEvent.setup();

    const fetchAdminLoansAction = {
      type: "loans/fetchAdminLoans/pending",
    };

    mockFetchAdminLoans.mockReturnValue(
      fetchAdminLoansAction,
    );

    render(<LoansPage />);

    await user.click(
      screen.getByRole("button", {
        name: "Returned Loans",
      }),
    );

    expect(mockFetchAdminLoans).toHaveBeenCalledWith({
      filter: "history",
      page: 0,
      size: 5,
    });

    expect(mockDispatch).toHaveBeenCalledWith(
      fetchAdminLoansAction,
    );
  });

  test("should fetch the next admin page", async () => {
    const user = userEvent.setup();

    const pagedState = {
      loans: {
        ...mockState.loans,
        adminPage: 0,
        adminTotalPages: 3,
        adminTotalElements: 11,
      },
    };

    const fetchAdminLoansAction = {
      type: "loans/fetchAdminLoans/pending",
    };

    setMockState(pagedState);

    mockFetchAdminLoans.mockReturnValue(
      fetchAdminLoansAction,
    );

    render(<LoansPage />);

    await user.click(
      screen.getByRole("button", {
        name: "Next",
      }),
    );

    expect(mockFetchAdminLoans).toHaveBeenCalledWith({
      filter: "active",
      page: 1,
      size: 5,
    });

    expect(mockDispatch).toHaveBeenCalledWith(
      fetchAdminLoansAction,
    );
  });

test("should display user loans and admin loan records", () => {
  render(<LoansPage />);

  expect(
    screen.getByRole("heading", {
      name: "Loans",
    }),
  ).toBeInTheDocument();

  expect(
    screen.getByText("Total records: 1"),
  ).toBeInTheDocument();

  expect(screen.getAllByText("101")).toHaveLength(2);

  expect(
    screen.getAllByText("2026-07-01"),
  ).toHaveLength(2);

  expect(
    screen.getAllByText("2026-07-15"),
  ).toHaveLength(2);

  expect(
    screen.getAllByText("Not returned"),
  ).toHaveLength(2);

  expect(screen.getByText("Active")).toBeInTheDocument();
});
});